package com.dgeiger.enhanced_framework.apps.examples;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.apps.Context;
import com.dgeiger.enhanced_framework.filtering.FilterApp;
import com.dgeiger.enhanced_framework.filtering.Filter;
import com.dgeiger.enhanced_framework.filtering.filters.staticfilter.StaticMatchFilter;
import org.projectfloodlight.openflow.types.IPAddressWithMask;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;


public class ScenarioD3App implements FilterApp {

    private ArrayList<Filter> filters;

    private static final Logger log = LoggerFactory.getLogger(ScenarioD3App.class);

    public ScenarioD3App(Integer numberOfFilters){
        filters = new ArrayList<>();

        StaticMatchFilter matchFilter;
        for(String matchingAddress : generateMatchingAddresses(numberOfFilters)){
            matchFilter = new StaticMatchFilter();
            matchFilter.setSourceNetmask(IPAddressWithMask.of(
                    IPv4Address.of(matchingAddress),
                    IPv4Address.of("255.255.255.0")));
            filters.add(matchFilter);
        }
    }

    public ScenarioD3App(){
        this(1000);
    }

    private ArrayList<String> generateMatchingAddresses(int number){
        ArrayList<String> addresses = new ArrayList<>();

        for(int i = 0; i < number; i++){
            String address = ((i >> (0 * 8)) & 0x000000FF) +
                    "." +
                    ((i >> (1 * 8)) & 0x000000FF) +
                    "." +
                    ((i >> (2 * 8)) & 0x000000FF) +
                    "." +
                    ((i >> (3 * 8)) & 0x000000FF);
            addresses.add(address);
        }

        String last = addresses.get(addresses.size() - 1);

        return addresses;
    }

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
