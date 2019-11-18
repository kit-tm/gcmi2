package com.dgeiger.enhanced_framework.apps.examples;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.apps.Context;
import com.dgeiger.enhanced_framework.filtering.FilterApp;
import com.dgeiger.enhanced_framework.filtering.Filter;
import com.dgeiger.enhanced_framework.filtering.filters.staticfilter.StaticSizeFilter;
import com.dgeiger.enhanced_framework.filtering.filters.staticfilter.StaticTypeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;


public class LogAndForwardFilterApp implements FilterApp {

    private ArrayList<Filter> filters;

    private static final Logger log = LoggerFactory.getLogger(LogAndForwardFilterApp.class);

    public LogAndForwardFilterApp(){
        filters = new ArrayList<>();
        filters.add(new StaticTypeFilter("PACKET_IN"));
        filters.add(new StaticSizeFilter(8, 100));
    }

    @Override
    public void receivedFromController(OFlowMessage message, Context context) {
        log.debug("controller -> switch: {}", message.getMessage());
        context.switchConnection().accept(message);
    }

    @Override
    public void receivedFromSwitch(OFlowMessage message, Context context) {
        log.debug("switch -> controller: {}", message.getMessage());
        context.controllerConnection().accept(message);
    }

    @Override
    public ArrayList<Filter> getFilters() {
        return filters;
    }
}
