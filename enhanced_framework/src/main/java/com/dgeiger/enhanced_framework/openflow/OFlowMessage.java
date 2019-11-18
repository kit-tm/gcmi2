package com.dgeiger.enhanced_framework.openflow;

import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.Match;

public class OFlowMessage {

    private OFMessage message;
    private int size;
    private String sender;
    private boolean upStreamDirection;

    public OFlowMessage(OFMessage message, int size, String sender){
        this.message = message;
        this.size = size;
        this.sender = sender;
    }

    public long getXid(){
        return message.getXid();
    }

    public OFType getType(){
        return message.getType();
    }

    public OFMessage getMessage() {
        return message;
    }

    public void setMessage(OFMessage message) {
        this.message = message;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public boolean isUpStreamDirection() {
        return upStreamDirection;
    }

    public Match getMatch(){
        if(message.getType() == OFType.FLOW_MOD){
            return ((OFFlowMod) message).getMatch();
        } else if(message.getType() == OFType.FLOW_REMOVED){
            return ((OFFlowRemoved) message).getMatch();
        }
        return null;
    }

    public void setUpStreamDirection(boolean upStreamDirection) {
        this.upStreamDirection = upStreamDirection;
    }

}
