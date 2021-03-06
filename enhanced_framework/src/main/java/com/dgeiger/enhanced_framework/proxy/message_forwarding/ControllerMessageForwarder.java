package com.dgeiger.enhanced_framework.proxy.message_forwarding;

import com.dgeiger.enhanced_framework.apps.App;
import com.dgeiger.enhanced_framework.apps.Context;
import com.dgeiger.enhanced_framework.openflow.OFlowMessage;

import java.util.List;

public class ControllerMessageForwarder extends MessageForwarder {

    private final App firstApp;

    public ControllerMessageForwarder(List<App> apps, Context context) {
        super(apps, context);
        int firstAppId = apps.size() - 1;
        firstApp = apps.get(firstAppId);
        firstAppContext = new Context(getDownstreamCallback(firstAppId), getUpstreamCallback(firstAppId));
    }

    @Override
    void forwardMessage(OFlowMessage oFlowMessage) {
        firstApp.receivedFromController(oFlowMessage, firstAppContext);
    }

}
