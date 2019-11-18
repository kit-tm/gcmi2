package com.dgeiger.enhanced_framework.apps.examples;

import com.dgeiger.enhanced_framework.apps.Context;
import com.dgeiger.enhanced_framework.filtering.Filter;
import com.dgeiger.enhanced_framework.filtering.FilterApp;
import com.dgeiger.enhanced_framework.filtering.filters.staticfilter.StaticTypeFilter;
import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * An adapted version of PayLess for the enhanced framework
 * https://github.com/kit-tm/gcmi/blob/master/examples/payless/src/main/java/com/github/sherter/jcon/examples/payless/Payless.java
 */
public class PaylessApp implements FilterApp {

    private static final Logger log = LoggerFactory.getLogger(PaylessApp.class);

    private ArrayList<Filter> filters;
    private Cache<Match, List<OFFlowStatsEntry>> stats;
    private Multimap<Match, OFFlowStatsRequest> pendingRequests = HashMultimap.create();
    private Map<Long, Match> pendingXids = new HashMap<>();
    private ByteBuf byteBuf;

    public PaylessApp(){
        StaticTypeFilter staticTypeFilter = new StaticTypeFilter("STATS_REQUEST");
        staticTypeFilter.addType("STATS_RESPONSE");

        filters = new ArrayList<>();
        filters.add(staticTypeFilter);

        stats = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .weakKeys()
                .maximumSize(10000)
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build();
        byteBuf = Unpooled.buffer();
    }

    @Override
    public ArrayList<Filter> getFilters() {
        return filters;
    }

    private int getSize(OFMessage message){
        message.writeTo(byteBuf);
        int size = byteBuf.readerIndex();
        byteBuf.clear();
        return size;
    }

    @Override
    public void receivedFromController(OFlowMessage message, Context context) {
        OFStatsRequest<?> request = (OFStatsRequest<?>) message.getMessage();
        OFStatsType type = request.getStatsType();
        if (type == OFStatsType.FLOW) {
            OFFlowStatsRequest flowReq = (OFFlowStatsRequest) request;

            List<OFFlowStatsEntry> previousStats = stats.getIfPresent(flowReq.getMatch());
            if (previousStats != null) {
                OFStatsReply response =
                        OFFactories.getFactory(request.getVersion())
                                .buildFlowStatsReply()
                                .setXid(request.getXid())
                                .setEntries(previousStats)
                                .build();
                context.controllerConnection().accept(new OFlowMessage(response, getSize(response), ""));
                log.info("payless -> controller: {}", response);
            } else {
                if (!pendingRequests.containsKey(flowReq.getMatch())) {
                    context.switchConnection().accept(message);
                    pendingXids.put(request.getXid(), flowReq.getMatch());
                }
                pendingRequests.put(flowReq.getMatch(), flowReq);
            }
        } else {
            // port stats or other
            log.info("controller: -> switch: {}", request);
            context.switchConnection().accept(message);
        }
    }

    @Override
    public void receivedFromSwitch(OFlowMessage message, Context context) {
        OFStatsReply reply = (OFStatsReply) message.getMessage();
        log.info("switch -> controller: {}", reply);
        if (reply.getStatsType() == OFStatsType.FLOW) {
            OFFlowStatsReply flowReply = (OFFlowStatsReply) reply;
            Match pendingRequestMatch = pendingXids.remove(flowReply.getXid());
            // answer all pending requests
            for (OFFlowStatsRequest request : pendingRequests.removeAll(pendingRequestMatch)) {
                OFStatsReply response =
                        OFFactories.getFactory(request.getVersion())
                                .buildFlowStatsReply()
                                .setXid(request.getXid())
                                .setEntries(flowReply.getEntries())
                                .build();

                context.controllerConnection().accept(new OFlowMessage(response, getSize(response), ""));
            }
            // put in cache for future requests
            stats.put(pendingRequestMatch, flowReply.getEntries());
        }else{
            context.controllerConnection().accept(message);
        }
    }
}
