package com.dgeiger.enhanced_framework.proxy.networking;

import com.dgeiger.enhanced_framework.benchmarking.MessageTimestampRecorder;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Handler extends BaseNetworkConnectionHandler {

    public Handler(SocketChannel socketChannel, SelectionKey key, MessageTimestampRecorder messageTimestampRecorder,
                   boolean clientMode) {
        super(socketChannel, key, messageTimestampRecorder, clientMode);
        log = LoggerFactory.getLogger(Handler.class);
    }

    public void handleEvent(){
        try {
            if (key.isWritable()) {
                synchronized (sendBufferDecrypted) {
                    write(sendBufferDecrypted);
                }
            }
            if (key.isReadable()) read();
        }catch (Exception e){
            closeConnection(e);
        }
    }

    private void read() {
        int read = 0;
        try {
            read = socketChannel.read(receiveBufferDecrypted);
            if(messageTimestampRecorder != null)
                messageTimestampRecorder.saveInTime(!clientMode);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (read == -1) {
            closeConnection(null);
        } else if (read > 0) {
            receiveBufferDecrypted.flip();
            receivedDataCallback.accept(receiveBufferDecrypted);
            receiveBufferDecrypted.compact();
        }
    }
}
