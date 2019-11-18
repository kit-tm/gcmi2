package com.dgeiger.enhanced_framework.apps.examples;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.apps.App;
import com.dgeiger.enhanced_framework.apps.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScenarioD1App implements App {

    private static final Logger log = LoggerFactory.getLogger(ScenarioD1App.class);

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

}
