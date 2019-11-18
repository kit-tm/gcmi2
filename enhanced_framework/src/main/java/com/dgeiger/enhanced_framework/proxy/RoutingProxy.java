package com.dgeiger.enhanced_framework.proxy;

import com.dgeiger.enhanced_framework.apps.App;
import com.dgeiger.enhanced_framework.apps.Context;
import com.dgeiger.enhanced_framework.benchmarking.MessageTimestampRecorder;
import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.openflow.OFlowMessageSerializingConsumer;
import com.dgeiger.enhanced_framework.proxy.message_forwarding.ControllerMessageForwarder;
import com.dgeiger.enhanced_framework.proxy.message_forwarding.SwitchConnectionMessageHandler;
import com.dgeiger.enhanced_framework.proxy.message_forwarding.SwitchMessageForwarder;
import com.dgeiger.enhanced_framework.proxy.networking.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class RoutingProxy {

    public enum SaveTimestampsSetting{UPSTREAM, DOWNSTREAM, ALL, NONE}

    private Selector selector;
    private ProxyNetworkSettings settings;

    private List<App> apps;
    private SaveTimestampsSetting saveTimestampsSetting;
    private MessageTimestampRecorder messageTimestampRecorder;

    private static final Logger log = LoggerFactory.getLogger(RoutingProxy.class);

    public RoutingProxy(ProxyNetworkSettings settings, List<App> apps, SaveTimestampsSetting saveTimestampsSetting){
        this.saveTimestampsSetting = saveTimestampsSetting;
        this.apps = apps;
        this.settings = settings;

        log.info("Starting new proxy: " + settings.getDownstreamPort() + " " + settings.getUpstreamPort());

        if(saveTimestampsSetting != RoutingProxy.SaveTimestampsSetting.NONE){
            this.messageTimestampRecorder = new MessageTimestampRecorder();
        }
    }

    private void loop(){
        while(!Thread.interrupted()){
            try {
                selector.select();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            Iterator iter = selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = (SelectionKey) iter.next();
                ((NetworkEventHandler) key.attachment()).handleEvent();
                iter.remove();
            }
        }
    }

    public SocketChannel establishUpstreamConnection() throws IOException {
        log.info("establishing upstream connection..");
        SocketChannel upstreamSocketChannel = selector.provider().openSocketChannel();
        upstreamSocketChannel.connect(new InetSocketAddress(settings.getUpstreamIp(), settings.getUpstreamPort()));

        upstreamSocketChannel.configureBlocking(false);
        return upstreamSocketChannel;
    }

    public void listen() {
        try {
            selector = Selector.open();
            ServerSocketChannel downstreamServerSocketChannel = selector.provider().openServerSocketChannel();
            downstreamServerSocketChannel.configureBlocking(false);
            downstreamServerSocketChannel.socket().bind(new InetSocketAddress(settings.getDownstreamPort()));

            log.info("waiting for switches to connect...");
            Acceptor downstreamAcceptor = new Acceptor(downstreamServerSocketChannel, this::onDownstreamConnectionEstablished);
            downstreamServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT, downstreamAcceptor);
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(this::loop).start();
    }

    private BaseNetworkConnectionHandler createHandler(SocketChannel socketChannel, boolean clientMode) throws ClosedChannelException {
        SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ);

        BaseNetworkConnectionHandler handler;
        if(settings.useSsl()){
            handler = new TlsHandler(socketChannel, key, messageTimestampRecorder, clientMode);
        }else {
            handler = new Handler(socketChannel, key, messageTimestampRecorder, clientMode);
        }

        key.attach(handler);

        return handler;
    }

    private void onDownstreamConnectionEstablished(SocketChannel downstreamChannel){
        try {
            final String switchRemoteAddress = downstreamChannel.getRemoteAddress().toString();
            log.info("Downstream connection established: " + switchRemoteAddress);

            final BaseNetworkConnectionHandler downstreamHandler = createHandler(downstreamChannel, false);

            SocketChannel upstreamSocketChannel = null;
            try {
                upstreamSocketChannel = establishUpstreamConnection();
            }catch (IOException e){
                log.error("Failed to establish upstream connection", e);
            }
            final BaseNetworkConnectionHandler upstreamHandler;
            if(upstreamSocketChannel != null) upstreamHandler = createHandler(upstreamSocketChannel, true);
            else upstreamHandler = null;

            final Context context = buildContextFromHandlers(upstreamHandler, downstreamHandler);
            final ControllerMessageForwarder controllerMessageForwarder = new ControllerMessageForwarder(apps, context);
            final SwitchMessageForwarder switchMessageForwarder = new SwitchMessageForwarder(apps, context);

            final SwitchConnectionMessageHandler switchConnectionDownstreamMessageHandler
                    = new SwitchConnectionMessageHandler(controllerMessageForwarder);
            final SwitchConnectionMessageHandler switchConnectionUpstreamMessageHandler
                    = new SwitchConnectionMessageHandler(switchMessageForwarder);

            // Switch -> Controller
            downstreamHandler.setReceivedOFlowMessageCallback(oFlowMessage -> {
                if (saveTimestampsSetting == SaveTimestampsSetting.UPSTREAM
                        || saveTimestampsSetting == SaveTimestampsSetting.ALL) {
                    messageTimestampRecorder.saveOutXid(oFlowMessage.getXid(), true);
                }

                switchConnectionUpstreamMessageHandler.processMessage(oFlowMessage);
            });

            downstreamHandler.setDisconnectedCallback(t -> log.error("Downstream connection closed: " + switchRemoteAddress, t));

            // Controller -> Switch
            if(upstreamHandler == null) return;
            upstreamHandler.setReceivedOFlowMessageCallback(oFlowMessage -> {
                if(saveTimestampsSetting == SaveTimestampsSetting.DOWNSTREAM
                        || saveTimestampsSetting == SaveTimestampsSetting.ALL){
                    messageTimestampRecorder.saveOutXid(oFlowMessage.getXid(), false);
                }

                switchConnectionDownstreamMessageHandler.processMessage(oFlowMessage);
            });

            upstreamHandler.setDisconnectedCallback(t -> log.error("Upstream connection closed", t));

            downstreamHandler.setDisconnectedCallback(t -> {
                log.error("Switch disconnected: " + switchRemoteAddress, t);
                upstreamHandler.closeConnection(null);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Context buildContextFromHandlers(NetworkConnectionHandler upstreamHandler, NetworkConnectionHandler downstreamHandler){
        OFlowMessageSerializingConsumer sendToDownstreamConsumer =
                new OFlowMessageSerializingConsumer(messageBytes -> downstreamHandler.send(messageBytes, upstreamHandler));

        Consumer<OFlowMessage> sendToUpstreamConsumer = new OFlowMessageSerializingConsumer(messageBytes -> {
            if(upstreamHandler != null){
                upstreamHandler.send(messageBytes, downstreamHandler);
            }
            else if(saveTimestampsSetting == SaveTimestampsSetting.UPSTREAM
                    || saveTimestampsSetting == SaveTimestampsSetting.ALL){
                messageTimestampRecorder.saveOutTimeForExistingXids(true);
            }
        });

        return new Context(sendToDownstreamConsumer, sendToUpstreamConsumer);
    }

}
