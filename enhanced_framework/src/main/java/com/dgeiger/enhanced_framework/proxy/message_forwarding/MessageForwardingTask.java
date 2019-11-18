package com.dgeiger.enhanced_framework.proxy.message_forwarding;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;

public class MessageForwardingTask implements Runnable{

    private MessageForwarder messageForwarder;
    private OFlowMessage oFlowMessage;

    public MessageForwardingTask(MessageForwarder messageForwarder, OFlowMessage oFlowMessage) {
        this.messageForwarder = messageForwarder;
        this.oFlowMessage = oFlowMessage;
    }

    @Override
    public void run() {
        messageForwarder.forwardMessage(oFlowMessage);
    }
}
