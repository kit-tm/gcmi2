package com.dgeiger.enhanced_framework.proxy.message_forwarding;

import com.dgeiger.enhanced_framework.apps.App;
import com.dgeiger.enhanced_framework.apps.Context;
import com.dgeiger.enhanced_framework.openflow.OFlowMessage;

import java.util.List;
import java.util.function.Consumer;

abstract class MessageForwarder {

    private final List<App> apps;
    private final Context context;
    Context firstAppContext;

    MessageForwarder(List<App> apps, Context context){
        this.apps = apps;
        this.context = context;
    }

    Consumer<OFlowMessage> getDownstreamCallback(int appId){
        if(appId == 0) return context.switchConnection();

        return message -> {
            apps.get(appId - 1).receivedFromController(message, new Context(getDownstreamCallback(appId - 1), getUpstreamCallback(appId - 1)));
        };
    }

    Consumer<OFlowMessage> getUpstreamCallback(int appId){
        if(appId == apps.size() - 1) return context.controllerConnection();

        return message -> {
            apps.get(appId + 1).receivedFromSwitch(message, new Context(getDownstreamCallback(appId + 1), getUpstreamCallback(appId + 1)));
        };
    }

    abstract void forwardMessage(OFlowMessage oFlowMessage);

}
