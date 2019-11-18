package com.dgeiger.enhanced_framework.apps.examples;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.apps.Context;
import com.dgeiger.enhanced_framework.filtering.FilterApp;
import com.dgeiger.enhanced_framework.filtering.Filter;
import com.dgeiger.enhanced_framework.filtering.filters.staticfilter.StaticSizeFilter;
import com.dgeiger.enhanced_framework.filtering.filters.timebasedfilter.TimeBasedFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Only logs PACKET_IN messages if a minimum number of 100 messages smaller than 10 bytes has been received in the last 10s
 */
public class LogAndForwardTimeBasedFilterApp implements FilterApp {

    private ArrayList<Filter> filters;

    private static final Logger log = LoggerFactory.getLogger(LogAndForwardTimeBasedFilterApp.class);

    public LogAndForwardTimeBasedFilterApp(){
        filters = new ArrayList<>();
        Filter activationFilter = new StaticSizeFilter(0, 10);
        filters.add(new TimeBasedFilter(activationFilter, 10000, 100));
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
