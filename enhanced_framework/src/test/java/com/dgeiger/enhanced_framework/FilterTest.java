package com.dgeiger.enhanced_framework;

import com.dgeiger.enhanced_framework.filtering.filters.monitoringbasedfilter.MonitoringControllerLatencyFilter;
import com.dgeiger.enhanced_framework.filtering.filters.staticfilter.StaticMatchFilter;
import com.dgeiger.enhanced_framework.filtering.filters.staticfilter.StaticSenderFilter;
import com.dgeiger.enhanced_framework.filtering.filters.staticfilter.StaticSizeFilter;
import com.dgeiger.enhanced_framework.filtering.filters.staticfilter.StaticTypeFilter;
import com.dgeiger.enhanced_framework.filtering.filters.timebasedfilter.TimeBasedFilter;
import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import org.junit.Before;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPAddressWithMask;
import org.projectfloodlight.openflow.types.IPv4Address;

import static org.junit.Assert.*;

public class FilterTest {

    private OFlowMessage message;
    private OFlowMessage largerMessage;
    private OFlowMessage upstreamMessage;
    private OFlowMessage downstreamMessage;
    private OFlowMessage flowModMessage;

    @Before
    public void setup(){
        OFFactory factory = OFFactories.getFactory(OFVersion.OF_10);
        OFPacketIn.Builder packetInBuilder = factory.buildPacketIn();

        OFFlowMod.Builder flowModBuilder = factory.buildFlowModify();
        Match.Builder matchBuilder = factory.buildMatch();

        IPv4Address ipAddress = IPv4Address.of("192.168.178.93");

        // treat last 8 bits as wild
        IPv4Address ipAddressMask = IPv4Address.of("255.255.255.0");

        Match match = matchBuilder
                .setExact(MatchField.IPV4_SRC, ipAddress)
                .setMasked(MatchField.IPV4_SRC, ipAddress, ipAddressMask)
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .build();

        OFMessage ofFlowModMessage = flowModBuilder
                .setXid(1)
                .setMatch(match)
                .build();

        OFMessage ofMessage = packetInBuilder
                .setXid(1)
                .setReason(OFPacketInReason.BSN_PACKET_OF_DEATH)
                .build();

        message = new OFlowMessage(ofMessage, 50, "127.0.0.1:9953");
        largerMessage = new OFlowMessage(ofMessage, 500, "127.0.0.1:9953");
        upstreamMessage = new OFlowMessage(ofMessage, 50, "127.0.0.1:9953");
        downstreamMessage = new OFlowMessage(ofMessage, 50, "127.0.0.1:9953");
        flowModMessage = new OFlowMessage(ofFlowModMessage, 50, "127.0.0.1:9953");

        upstreamMessage.setUpStreamDirection(true);
        downstreamMessage.setUpStreamDirection(false);
    }

    @Test
    public void testSenderFilter(){
        StaticSenderFilter matchingFilter = new StaticSenderFilter("127.0.0.1:9953");
        StaticSenderFilter notMatchingFilter = new StaticSenderFilter("127.0.0.2:9953");

        assertTrue(matchingFilter.matches(message));
        assertFalse(notMatchingFilter.matches(message));
    }

    @Test
    public void testSizeFilter() {
        StaticSizeFilter tooSmallFilter = new StaticSizeFilter(10, 20);
        StaticSizeFilter matchingFilter = new StaticSizeFilter(40, 60);
        StaticSizeFilter tooBigFilter = new StaticSizeFilter(60, 70);

        assertTrue(matchingFilter.matches(message));
        assertFalse(tooSmallFilter.matches(message));
        assertFalse(tooBigFilter.matches(message));
    }

    @Test
    public void testTypeFilter(){
        StaticTypeFilter notMatchingFilter = new StaticTypeFilter("ECHO_REQUEST");
        StaticTypeFilter matchingFilter = new StaticTypeFilter("PACKET_IN");

        assertTrue(matchingFilter.matches(message));
        assertFalse(notMatchingFilter.matches(message));
    }

    @Test
    public void testTimeBasedFilter(){
        StaticSizeFilter activationFilter = new StaticSizeFilter(400, 600);

        TimeBasedFilter timeBasedFilter = new TimeBasedFilter(activationFilter, 1000, 10);

        assertFalse(timeBasedFilter.matches(message));

        for(int i = 0; i < 9; i++){
            assertFalse(timeBasedFilter.matches(largerMessage));
        }

        assertTrue(timeBasedFilter.matches(largerMessage));
        assertTrue(timeBasedFilter.matches(message));

        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertFalse(timeBasedFilter.matches(largerMessage));
        assertFalse(timeBasedFilter.matches(message));
    }

    @Test
    public void testMonitoringControllerLatencyFilter(){
        MonitoringControllerLatencyFilter filter = new MonitoringControllerLatencyFilter(0, 100000);

        assertFalse(filter.matches(upstreamMessage));
        assertTrue(filter.matches(downstreamMessage));

        assertTrue(filter.matches(upstreamMessage));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse(filter.matches(downstreamMessage));
    }

    @Test
    public void testStaticMatchFilter(){
        StaticMatchFilter matchFilter = new StaticMatchFilter();

        matchFilter.setSourceNetmask(IPAddressWithMask.of(IPv4Address.of("123.123.123.93"), IPv4Address.of("0.0.0.0")));
        assertTrue(matchFilter.matches(flowModMessage));

        matchFilter.setSourceNetmask(IPAddressWithMask.of(IPv4Address.of("192.168.178.1"), IPv4Address.of("255.255.255.0")));
        assertTrue(matchFilter.matches(flowModMessage));


        matchFilter.setSourceNetmask(IPAddressWithMask.of(IPv4Address.of("111.111.111.2"), IPv4Address.of("255.255.255.0")));
        assertFalse(matchFilter.matches(flowModMessage));


        matchFilter.setSourceNetmask(IPAddressWithMask.of(IPv4Address.of("192.168.178.3"), IPv4Address.of("255.255.255.0")));
        assertTrue(matchFilter.matches(flowModMessage));


        matchFilter.setSourceNetmask(IPAddressWithMask.of(IPv4Address.of("192.168.180.3"), IPv4Address.of("255.255.0.0")));
        assertTrue(matchFilter.matches(flowModMessage));


        matchFilter.setSourceNetmask(IPAddressWithMask.of(IPv4Address.of("192.168.180.3"), IPv4Address.of("255.255.255.0")));
        assertFalse(matchFilter.matches(flowModMessage));

    }
}
