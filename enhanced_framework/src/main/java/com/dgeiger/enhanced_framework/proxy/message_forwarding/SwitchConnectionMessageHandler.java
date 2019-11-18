package com.dgeiger.enhanced_framework.proxy.message_forwarding;

import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import java.util.concurrent.*;

public class SwitchConnectionMessageHandler {

    private MessageForwarder messageForwarder;
    //private ThreadPoolExecutor executorService;

    public SwitchConnectionMessageHandler(MessageForwarder messageForwarder) {
        this.messageForwarder = messageForwarder;
        //executorService = new ThreadPoolExecutor(1,1,1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        //executorService.prestartAllCoreThreads();
    }

    public void processMessage(OFlowMessage oFlowMessage){
        //while(executorService.getQueue().size() > 20000){};
        messageForwarder.forwardMessage(oFlowMessage);
        //this.executorService.execute(new MessageForwardingTask(messageForwarder, oFlowMessage));
    }
}

