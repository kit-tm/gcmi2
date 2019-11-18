package com.dgeiger.enhanced_framework.filtering;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.apps.Context;
import com.dgeiger.enhanced_framework.apps.App;
import com.dgeiger.enhanced_framework.filtering.caching.Cache;
import com.dgeiger.enhanced_framework.filtering.caching.HashMapCache;
import com.dgeiger.enhanced_framework.filtering.caching.OFlowMessageField;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Underlying layer for all filter apps
 * Applies all filters and forwards non matching messages directly
 */
public class FilterLayer implements App {

    private FilterApp filterApp;
    private Cache upstreamCache;
    private Cache downstreamCache;
    private boolean matchModeAny;
    private Set<OFlowMessageField> relevantMessageFields;

    public FilterLayer(FilterApp filterApp, boolean matchModeAny, boolean useCache){
        this.filterApp = filterApp;
        this.matchModeAny = matchModeAny;
        if(useCache) {
            this.upstreamCache = new HashMapCache();
            this.downstreamCache = new HashMapCache();

            relevantMessageFields = filterApp.getFilters()
                    .stream()
                    .map(Filter::getRelevantMessageFields)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public void receivedFromController(OFlowMessage message, Context context) {
        message.setUpStreamDirection(false);

        if(matchAgainstCacheAndFilters(message, downstreamCache)){
            filterApp.receivedFromController(message, context);
        }else{
            context.switchConnection().accept(message);
        }
    }

    @Override
    public void receivedFromSwitch(OFlowMessage message, Context context) {
        message.setUpStreamDirection(true);

        if(matchAgainstCacheAndFilters(message, upstreamCache)){
            filterApp.receivedFromSwitch(message, context);
        }else{
            context.controllerConnection().accept(message);
        }
    }

    private boolean matchAgainstFilters(OFlowMessage message){
        if(matchModeAny){
            return filterApp.getFilters().stream().anyMatch(filter -> filter.matches(message));
        }else{
            return filterApp.getFilters().stream().allMatch(filter -> filter.matches(message));
        }
    }

    private String getFilterIdForMessage(OFlowMessage message){
        StringBuilder builder = new StringBuilder();
        if(relevantMessageFields.contains(OFlowMessageField.SIZE)) builder.append(message.getSize());
        builder.append("|");
        if(relevantMessageFields.contains(OFlowMessageField.SENDER)) builder.append(message.getSender());
        builder.append("|");
        if(relevantMessageFields.contains(OFlowMessageField.TYPE)) builder.append(message.getType().name());
        builder.append("|");
        if(relevantMessageFields.contains(OFlowMessageField.DIRECTION)) builder.append(message.isUpStreamDirection());
        builder.append("|");
        if(relevantMessageFields.contains(OFlowMessageField.XID)) builder.append(message.getXid());
        builder.append("|");
        if(relevantMessageFields.contains(OFlowMessageField.MATCH_FIELD) && message.getMatch() != null)
            builder.append(message.getMatch().toString());
        return builder.toString();
    }

    private boolean matchAgainstCacheAndFilters(OFlowMessage message, Cache cache){
        if (cache == null) return matchAgainstFilters(message);

        String messageFilterId = getFilterIdForMessage(message);

        Cache.FilterResult result = cache.getFilterResult(messageFilterId);
        if(result == Cache.FilterResult.match) return true;
        else if(result == Cache.FilterResult.no_match) return false;
        else{
            if(matchAgainstFilters(message)){
                cache.storeFilterResult(messageFilterId, Cache.FilterResult.match);
                return true;
            }else {
                cache.storeFilterResult(messageFilterId, Cache.FilterResult.no_match);
                return false;
            }
        }
    }

}
