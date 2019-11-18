package com.dgeiger.enhanced_framework.apps;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;

public interface App {

    void receivedFromController(OFlowMessage message, Context context);
    void receivedFromSwitch(OFlowMessage message, Context context);

}
