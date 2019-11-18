package com.dgeiger.enhanced_framework.apps.examples;

import com.dgeiger.enhanced_framework.filtering.filters.staticfilter.StaticPercentageFilter;
import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.apps.Context;
import com.dgeiger.enhanced_framework.filtering.Filter;
import com.dgeiger.enhanced_framework.filtering.FilterApp;
import com.dgeiger.enhanced_framework.filtering.filters.staticfilter.StaticSenderFilter;
import com.dgeiger.enhanced_framework.filtering.filters.staticfilter.StaticSizeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class ScenarioD2App implements FilterApp {

    private ArrayList<Filter> filters;

    private static final Logger log = LoggerFactory.getLogger(ScenarioD2App.class);

    public ScenarioD2App(Integer matchingPercentage){
        filters = new ArrayList<>();

        filters.add(new StaticSizeFilter(8, 200));
        filters.add(new StaticSizeFilter(8, 200));
        filters.add(new StaticSenderFilter("127.0.0.1"));
        filters.add(new StaticSenderFilter("127.0.0.1"));
        filters.add(new StaticPercentageFilter(matchingPercentage));
    }

    public ScenarioD2App(){this(50);}

    @Override
    public void receivedFromController(OFlowMessage message, Context context) {
        log.info("controller -> switch: {}", message.getMessage());
        context.switchConnection().accept(message);
    }

    @Override
    public void receivedFromSwitch(OFlowMessage message, Context context) {
        log.info("switch -> controller: {}", message.getMessage());
        context.controllerConnection().accept(message);
    }

    @Override
    public ArrayList<Filter> getFilters() {
        return filters;
    }
}
