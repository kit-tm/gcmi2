package com.dgeiger.enhanced_framework.apps;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;

import java.util.function.Consumer;

public class Context {

    private Consumer<OFlowMessage> toDownstreamConsumer;
    private Consumer<OFlowMessage> toUpstreamConsumer;

    public Context(Consumer<OFlowMessage> toDownstreamConsumer, Consumer<OFlowMessage> toUpstreamConsumer){
        this.toDownstreamConsumer = toDownstreamConsumer;
        this.toUpstreamConsumer = toUpstreamConsumer;
    }

    public Consumer<OFlowMessage> controllerConnection(){
        return toUpstreamConsumer;
    }

    public Consumer<OFlowMessage> switchConnection(){
        return toDownstreamConsumer;
    }
}
