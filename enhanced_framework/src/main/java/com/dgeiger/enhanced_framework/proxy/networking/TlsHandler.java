package com.dgeiger.enhanced_framework.proxy.networking;

import com.dgeiger.enhanced_framework.benchmarking.MessageTimestampRecorder;
import com.dgeiger.enhanced_framework.proxy.RoutingProxy;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TlsHandler extends BaseNetworkConnectionHandler {

    private SSLEngine sslEngine;

    private final ByteBuffer receiveBufferEncrypted = ByteBuffer.allocate(4096);
    private final ByteBuffer sendBufferEncrypted = ByteBuffer.allocate(16709);

    private boolean clientMode;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public TlsHandler(SocketChannel socketChannel, SelectionKey key, MessageTimestampRecorder messageTimestampRecorder,
                      boolean clientMode) {
        super(socketChannel, key, messageTimestampRecorder, clientMode);
        this.clientMode = clientMode;
        log = LoggerFactory.getLogger(TlsHandler.class);

        setupSslContexts();

        try {
            sslEngine.beginHandshake();
            if(!clientMode) doHandshakeDownstream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupSslContexts(){
        String certificateName;
        if(clientMode) certificateName = "clienttruststore.jks";
        else certificateName = "certificate.jks";

        try {
            InputStream jks = TlsHandler.class.getClassLoader().getResourceAsStream(certificateName);

            // Create a KeyStore
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(jks, "changeit".toCharArray());

            // KeyManagerFactory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "changeit".toCharArray() );

            // Create a TrustManager that trusts the CAs in our KeyStore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            sslEngine = sslContext.createSSLEngine();
            sslEngine.setUseClientMode(clientMode);
            sslEngine.setEnabledProtocols(sslEngine.getSupportedProtocols());
            sslEngine.setEnabledCipherSuites(sslEngine.getSupportedCipherSuites());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void doHandshakeDownstream() throws IOException {
        log.debug("About to do handshake...");

        SSLEngineResult result;
        SSLEngineResult.HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();

        while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED && handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            switch (handshakeStatus) {
                case NEED_UNWRAP:
                    if (socketChannel.read(receiveBufferEncrypted) < 0) {
                        if (sslEngine.isInboundDone() && sslEngine.isOutboundDone()) {
                            log.error("handshake failed");
                            return;
                        }
                        try {
                            sslEngine.closeInbound();
                        } catch (SSLException e) {
                            log.error("This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.");
                        }
                        sslEngine.closeOutbound();
                        // After closeOutbound the engine will be set to WRAP state, in order to try to send a close message to the client.
                        handshakeStatus = sslEngine.getHandshakeStatus();
                        break;
                    }

                    receiveBufferEncrypted.flip();
                    try {
                        result = sslEngine.unwrap(receiveBufferEncrypted, receiveBufferDecrypted);
                        handshakeStatus = result.getHandshakeStatus();
                    } catch (SSLException sslException) {
                        sslException.printStackTrace();
                        log.error("A problem was encountered while processing the data that caused the SSLEngine to abort. Will try to properly close connection...");
                        sslEngine.closeOutbound();
                        break;
                    } finally {
                        receiveBufferEncrypted.compact();
                        receiveBufferDecrypted.clear();
                    }

                    switch (result.getStatus()) {
                        case OK:
                        case BUFFER_UNDERFLOW:
                            break;
                        case CLOSED:
                            if (sslEngine.isOutboundDone()) {
                                return;
                            } else {
                                sslEngine.closeOutbound();
                                handshakeStatus = sslEngine.getHandshakeStatus();
                                break;
                            }
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                    break;
                case NEED_WRAP:
                    sendBufferEncrypted.clear();
                    try {
                        sendBufferDecrypted.flip();
                        result = sslEngine.wrap(sendBufferDecrypted, sendBufferEncrypted);
                        sendBufferDecrypted.compact();
                        handshakeStatus = result.getHandshakeStatus();
                    } catch (SSLException sslException) {
                        sslException.printStackTrace();
                        log.error("A problem was encountered while processing the data that caused the SSLEngine to abort. Will try to properly close connection...");
                        sslEngine.closeOutbound();
                        handshakeStatus = sslEngine.getHandshakeStatus();
                        break;
                    }
                    switch (result.getStatus()) {
                        case OK :
                            sendBufferEncrypted.flip();
                            while (sendBufferEncrypted.hasRemaining()) {
                                socketChannel.write(sendBufferEncrypted);
                            }
                            sendBufferEncrypted.clear();
                            break;
                        case CLOSED:
                            try {
                                sendBufferEncrypted.flip();
                                while (sendBufferEncrypted.hasRemaining()) {
                                    socketChannel.write(sendBufferEncrypted);
                                }
                                // At this point the handshake status will probably be NEED_UNWRAP so we make sure that peerNetData is clear to read.
                                sendBufferEncrypted.clear();
                            } catch (Exception e) {
                                log.error("Failed to send server's CLOSE message due to socket channel's failure.");
                                handshakeStatus = sslEngine.getHandshakeStatus();
                            }
                            break;
                        default:
                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                    break;
                case NEED_TASK:
                    Runnable task;
                    while ((task = sslEngine.getDelegatedTask()) != null) {
                        executor.execute(task);
                    }
                    handshakeStatus = sslEngine.getHandshakeStatus();
                    break;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + handshakeStatus);
            }
        }
    }

    private void doHandshakeUpstream(){
        try{
            switch (sslEngine.getHandshakeStatus()) {
                case NEED_WRAP:
                    wrap();
                    write(sendBufferEncrypted);
                    break;

                case NEED_UNWRAP:
                    read();
                    unwrap();
                    break;

                case NEED_TASK:
                    Runnable runnable;
                    if ((runnable = sslEngine.getDelegatedTask()) != null) {
                        runnable.run();
                    }
                    break;
            }
        }catch (Exception e){
            closeConnection(e);
        }
    }

    private void wrap() {
        SSLEngineResult wrapResult;

        try {
            sendBufferDecrypted.flip();
            wrapResult = sslEngine.wrap(sendBufferDecrypted, sendBufferEncrypted);
        }catch (SSLException exc) {
            exc.printStackTrace();
            return;
        }finally {
            sendBufferDecrypted.compact();
        }

        if (wrapResult.getStatus() != SSLEngineResult.Status.OK) {
            throw new IllegalStateException("Failed to wrap: " + wrapResult.getStatus().name());
        }
    }

    private void read(){
        try {
            socketChannel.read(receiveBufferEncrypted);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void unwrap() {
        if(receiveBufferEncrypted.position() == 0) return;
        SSLEngineResult unwrapResult = null;

        try {
            receiveBufferEncrypted.flip();
            while(receiveBufferEncrypted.hasRemaining()) unwrapResult = sslEngine.unwrap(receiveBufferEncrypted, receiveBufferDecrypted);
        } catch (SSLException ex) {
            ex.printStackTrace();
            return;
        }finally {
            receiveBufferEncrypted.compact();
        }

        switch (unwrapResult.getStatus()) {
            case OK:
            case BUFFER_UNDERFLOW:
                if (receiveBufferDecrypted.position() > 0) {
                    receiveBufferDecrypted.flip();
                    receivedDataCallback.accept(receiveBufferDecrypted);
                    receiveBufferDecrypted.compact();
                }
                break;

            case CLOSED:
            case BUFFER_OVERFLOW:
                throw new IllegalStateException("failed to unwrap: " + unwrapResult.getStatus().name());
        }
    }

    @Override
    public void handleEvent() {
        try{
            if(clientMode && sslEngine.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.FINISHED
                    && sslEngine.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
                doHandshakeUpstream();

            if (key.isReadable()){
                read();
                unwrap();
            } else if (key.isWritable()) {
                wrap();
                write(sendBufferEncrypted);
            }
        }catch (Exception e){
            closeConnection(e);
        }
    }

}
