package com.dgeiger.enhanced_framework;

import com.dgeiger.enhanced_framework.apps.Context;
import com.dgeiger.enhanced_framework.apps.examples.PaylessApp;
import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFFlowStatsRequest;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PaylessAppTest {

    private PaylessApp paylessApp;
    private Consumer<OFlowMessage> controllerConnection;
    private Consumer<OFlowMessage> switchConnection;
    private Context context;
    private List<OFlowMessage> statsRequests;

    @Before
    public void setup(){
        controllerConnection = spy(new Consumer<OFlowMessage>() {
            @Override
            public void accept(OFlowMessage oFlowMessage) {}
        });

        switchConnection = spy(new Consumer<OFlowMessage>() {
            @Override
            public void accept(OFlowMessage oFlowMessage) {}
        });

        context = new Context(switchConnection, controllerConnection);

        statsRequests = new ArrayList<>();
        paylessApp = new PaylessApp();
        Match match = mock(Match.class);

        for(int i = 0; i < 3; i++){

            OFFlowStatsRequest message = mock(OFFlowStatsRequest.class);
            when(message.getStatsType()).thenReturn(OFStatsType.FLOW);
            when(message.getXid()).thenReturn((long) i);
            when(message.getMatch()).thenReturn(match);
            when(message.getVersion()).thenReturn(OFVersion.OF_10);
            statsRequests.add(new OFlowMessage(message, 0, ""));
        }
    }

    @Test
    public void testForwardsOnlyFirstMessage(){
        for (int i = 0; i < statsRequests.size(); i++)
            paylessApp.receivedFromController(statsRequests.get(i), context);

        verify(switchConnection, times(1)).accept(eq(statsRequests.get(0)));
    }

    @Test
    public void testAnswersCachedRequests(){
        for (int i = 0; i < statsRequests.size(); i++)
            paylessApp.receivedFromController(statsRequests.get(i), context);

        OFFlowStatsReply ofFlowStatsReply = mock(OFFlowStatsReply.class);
        when(ofFlowStatsReply.getXid()).thenReturn(0L);
        when(ofFlowStatsReply.getStatsType()).thenReturn(OFStatsType.FLOW);
        paylessApp.receivedFromSwitch(new OFlowMessage(ofFlowStatsReply, 0, ""), context);

        verify(controllerConnection, times(statsRequests.size())).accept(any());
    }
}
