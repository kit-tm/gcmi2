package com.dgeiger.enhanced_framework.openflow;

import com.dgeiger.enhanced_framework.benchmarking.MessageTimestampRecorder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFMessage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

public class OFlowMessageParsingConsumer implements Consumer<ByteBuffer> {

    private final Consumer<OFlowMessage> oflowMessageConsumer;
    private final String remoteAddress;
    private final MessageTimestampRecorder messageTimestampRecorder;
    private final boolean clientMode;

    public OFlowMessageParsingConsumer(Consumer<OFlowMessage> oflowMessageConsumer, String remoteAddress, MessageTimestampRecorder messageTimestampRecorder, boolean clientMode) {
        this.oflowMessageConsumer = oflowMessageConsumer;
        this.remoteAddress = remoteAddress;
        this.messageTimestampRecorder = messageTimestampRecorder;
        this.clientMode = clientMode;
    }

    @Override
    public void accept(ByteBuffer buffer) {
        buffer.order(ByteOrder.BIG_ENDIAN); // OpenFlow follows "most significant byte first"
        int limit = buffer.limit();
        int currentPosition = buffer.position();
        while (limit - currentPosition >= 4) { // OpenFlow message length is at bytes 3 and 4
            int length = Short.toUnsignedInt(buffer.getShort(currentPosition + 2));
            if (length < 8) {
                // invalid message, put remaining bytes in "damaged" message at the end of the list
                length = buffer.remaining();
            }
            if (currentPosition + length > limit) {
                break; // we didn't receive the whole message yet
            }
            byte[] message = new byte[length];
            buffer.get(message);

            try {
                ByteBuf messageBuffer = Unpooled.wrappedBuffer(message);
                OFMessage ofMessage = OFFactories.getGenericReader().readFrom(messageBuffer);
                messageBuffer.release();
                if(messageTimestampRecorder != null)
                    messageTimestampRecorder.saveInXidWithCurrentTime(!clientMode, ofMessage.getXid());
                oflowMessageConsumer.accept(new OFlowMessage(ofMessage, length, remoteAddress));
            } catch (OFParseError e) {
                throw new RuntimeException(e);
            }

            currentPosition = buffer.position();
        }
    }
}
