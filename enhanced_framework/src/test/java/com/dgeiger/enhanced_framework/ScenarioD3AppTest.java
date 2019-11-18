package com.dgeiger.enhanced_framework;

import com.dgeiger.enhanced_framework.apps.App;
import com.dgeiger.enhanced_framework.apps.Context;
import com.dgeiger.enhanced_framework.apps.examples.ScenarioD3App;
import com.dgeiger.enhanced_framework.filtering.FilterLayer;
import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;

import static org.mockito.Mockito.*;

public class ScenarioD3AppTest {

    private void testMatchesIp(String ip){
        ScenarioD3App scenarioD3App = spy(new ScenarioD3App());
        App scenariod3App = new FilterLayer(scenarioD3App, true, false);

        IPv4Address ipAddress = IPv4Address.of(ip);

        // treat last 8 bits as wild
        IPv4Address ipAddressMask = IPv4Address.of("255.255.255.0");

        OFFactory factory = OFFactories.getFactory(OFVersion.OF_10);
        Match.Builder matchBuilder = factory.buildMatch();

        Match match = matchBuilder
                .setExact(MatchField.IPV4_SRC, ipAddress)
                .setMasked(MatchField.IPV4_SRC, ipAddress, ipAddressMask)
                .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .build();

        OFlowMessage oFlowMessage = mock(OFlowMessage.class);
        when(oFlowMessage.getMatch()).thenReturn(match);

        Context context = mock(Context.class);
        when(context.controllerConnection()).thenReturn(OFlowMessage -> {});
        scenariod3App.receivedFromSwitch(oFlowMessage, context);
        verify(scenarioD3App, times(1)).receivedFromSwitch(eq(oFlowMessage), any(Context.class));
    }

    @Test
    public void testMatchFieldFilter(){
        testMatchesIp("192.1.0.0");
    }
}
