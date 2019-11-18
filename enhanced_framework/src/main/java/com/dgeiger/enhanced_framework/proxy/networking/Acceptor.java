package com.dgeiger.enhanced_framework.proxy.networking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

public class Acceptor implements NetworkEventHandler {

    private ServerSocketChannel serverSocketChannel;
    private Consumer<SocketChannel> onDownstreamConnectionEstablished;

    private static final Logger log = LoggerFactory.getLogger(Acceptor.class);

    public Acceptor(ServerSocketChannel serverSocketChannel, Consumer<SocketChannel> onDownstreamConnectionEstablished) {
        this.serverSocketChannel = serverSocketChannel;
        this.onDownstreamConnectionEstablished = onDownstreamConnectionEstablished;
    }

    public void handleEvent(){
        try {
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            onDownstreamConnectionEstablished.accept(socketChannel);
        } catch (IOException e) {
            log.error("failed to accept and configure a new client", e);
        }
    }
}
