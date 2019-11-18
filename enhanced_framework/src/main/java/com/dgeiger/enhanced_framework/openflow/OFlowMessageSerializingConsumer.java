package com.dgeiger.enhanced_framework.openflow;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class OFlowMessageSerializingConsumer implements Consumer<OFlowMessage> {

    private final Consumer<? super ByteBuffer> rawBytesConsumer;

    public OFlowMessageSerializingConsumer(Consumer<? super ByteBuffer> rawBytesConsumer) {
        this.rawBytesConsumer = rawBytesConsumer;
    }

    @Override
    public void accept(OFlowMessage message) {
        ByteBuf b = Unpooled.buffer();
        message.getMessage().writeTo(b);
        rawBytesConsumer.accept(b.nioBuffer());
        b.release();
    }

}
