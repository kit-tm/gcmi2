package com.dgeiger.enhanced_framework.proxy;

import java.net.InetAddress;

public class ProxyNetworkSettings {

    private int upstreamPort;
    private int downstreamPort;
    private boolean useSsl;
    private String upstreamIp;

    public ProxyNetworkSettings() {
        upstreamPort = 6653;
        downstreamPort = 9005;
        useSsl = false;
        upstreamIp = InetAddress.getLoopbackAddress().getHostAddress();
    }

    public int getUpstreamPort() {
        return upstreamPort;
    }

    public ProxyNetworkSettings setUpstreamPort(int upstreamPort) {
        this.upstreamPort = upstreamPort;
        return this;
    }

    public int getDownstreamPort() {
        return downstreamPort;
    }

    public ProxyNetworkSettings setDownstreamPort(int downstreamPort) {
        this.downstreamPort = downstreamPort;
        return this;
    }

    public boolean useSsl() {
        return useSsl;
    }

    public ProxyNetworkSettings setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
        return this;
    }

    public String getUpstreamIp() {
        return upstreamIp;
    }

    public ProxyNetworkSettings setUpstreamIp(String upstreamIp) {
        this.upstreamIp = upstreamIp;
        return this;
    }
}
