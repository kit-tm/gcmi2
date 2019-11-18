package com.dgeiger.enhanced_framework.proxy.networking;

import java.nio.ByteBuffer;

public interface NetworkConnectionHandler extends NetworkEventHandler {
    void send(ByteBuffer data, NetworkConnectionHandler producer);
    void setRead(boolean read);
}
