package com.dgeiger.enhanced_framework.apps.examples;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.apps.Context;
import com.dgeiger.enhanced_framework.filtering.FilterApp;
import com.dgeiger.enhanced_framework.filtering.Filter;
import com.dgeiger.enhanced_framework.filtering.filters.monitoringbasedfilter.MonitoringControllerLatencyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Only logs PACKET_IN messages if the controller latency measured during the last 3s is not larger than 100ms
 */
public class LogAndForwardMonitoringBasedFilterApp implements FilterApp {

    private ArrayList<Filter> filters;

    private static final Logger log = LoggerFactory.getLogger(LogAndForwardMonitoringBasedFilterApp.class);

    public LogAndForwardMonitoringBasedFilterApp(){
        filters = new ArrayList<>();
        filters.add(new MonitoringControllerLatencyFilter(0, 100000));
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
