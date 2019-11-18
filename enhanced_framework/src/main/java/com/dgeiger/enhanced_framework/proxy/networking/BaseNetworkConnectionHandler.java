package com.dgeiger.enhanced_framework.proxy.networking;

import com.dgeiger.enhanced_framework.benchmarking.MessageTimestampRecorder;
import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import com.dgeiger.enhanced_framework.openflow.OFlowMessageParsingConsumer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public abstract class BaseNetworkConnectionHandler implements NetworkConnectionHandler {

    private MessageTimestampRecorder messageTimestampRecorder;

    final ByteBuffer receiveBufferDecrypted = ByteBuffer.allocate(16 * 1024);
    final ByteBuffer sendBufferDecrypted = ByteBuffer.allocate(1000 * 65536); // 1000 * max openflow message length

    SocketChannel socketChannel;
    SelectionKey key;
    Consumer<ByteBuffer> receivedDataCallback;
    private Consumer<Throwable> disconnectedCallback;

    private static final int BACKPRESSURE_LIMIT = 1024;
    private Set<NetworkConnectionHandler> backPressureables = new HashSet<>();
    private final boolean clientMode;

    Logger log;

    public BaseNetworkConnectionHandler(SocketChannel socketChannel, SelectionKey key,
                                        MessageTimestampRecorder messageTimestampRecorder, boolean clientMode){
        this.socketChannel = socketChannel;
        this.key = key;
        this.messageTimestampRecorder = messageTimestampRecorder;
        this.clientMode = clientMode;
        this.receivedDataCallback = data -> {};
        this.disconnectedCallback = throwable -> {};
    }

    public void setReceivedOFlowMessageCallback(Consumer<OFlowMessage> receivedDataCallback) {
        this.receivedDataCallback = new OFlowMessageParsingConsumer(receivedDataCallback,
                socketChannel.socket().getRemoteSocketAddress().toString(), messageTimestampRecorder, clientMode);
    }

    public void setDisconnectedCallback(Consumer<Throwable> disconnectedCallback){
        this.disconnectedCallback = disconnectedCallback;
    }

    public void closeConnection(Throwable t) {
        key.cancel();
        try {
            socketChannel.close();
        } catch (IOException ignored) {}
        finally {
            disconnectedCallback.accept(t);
        }
    }

    void write(ByteBuffer byteBufferToWrite) throws IOException {
        byteBufferToWrite.flip();
        if(messageTimestampRecorder != null)
            messageTimestampRecorder.saveOutTimeForExistingXids(clientMode);
        socketChannel.write(byteBufferToWrite);
        setRead(true);// When we write something, we may expect a response: unpressure so we can receive it
        if (byteBufferToWrite.remaining() == 0) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
        if (byteBufferToWrite.remaining() < BACKPRESSURE_LIMIT) {
            backPressureables.forEach(networkConnectionHandler -> networkConnectionHandler.setRead(true));
        }
        byteBufferToWrite.compact();
    }

    @Override
    public void send(ByteBuffer data, NetworkConnectionHandler producer) {
        synchronized (sendBufferDecrypted){
            if (sendBufferDecrypted.limit() > BACKPRESSURE_LIMIT && producer != null) {
                producer.setRead(false);
                backPressureables.add(producer);
            }
            if(data.limit() < sendBufferDecrypted.remaining()) sendBufferDecrypted.put(data);
            else log.warn("sendbuffer overflow");
        }

        key.interestOps(SelectionKey.OP_WRITE | key.interestOps());
        key.selector().wakeup();
    }

    @Override
    public void setRead(boolean read) {
        if(read) key.interestOps(key.interestOps() | SelectionKey.OP_READ);
        else key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
        key.selector().wakeup();
    }

}
