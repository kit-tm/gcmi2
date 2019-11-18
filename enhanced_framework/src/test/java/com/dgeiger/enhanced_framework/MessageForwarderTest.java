package com.dgeiger.enhanced_framework;

import com.dgeiger.enhanced_framework.apps.App;
import com.dgeiger.enhanced_framework.apps.Context;
import com.dgeiger.enhanced_framework.proxy.message_forwarding.ControllerMessageForwarder;
import com.dgeiger.enhanced_framework.proxy.message_forwarding.MessageForwardingTask;
import com.dgeiger.enhanced_framework.proxy.message_forwarding.SwitchMessageForwarder;
import com.dgeiger.enhanced_framework.openflow.OFlowMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MessageForwarderTest {

    private ArrayList<App> apps;

    private void buildApps(int number){
        apps = new ArrayList<>();
        for(int i = 0; i < number; i++) apps.add(spy(new App() {
            @Override
            public void receivedFromController(OFlowMessage message, Context context) {
                context.switchConnection().accept(message);
            }

            @Override
            public void receivedFromSwitch(OFlowMessage message, Context context) {
                context.controllerConnection().accept(message);
            }
        }));
    }

    @Test
    public void testCorrectUpstreamForwarding(){
        buildApps(3);
        OFlowMessage oFlowMessage = mock(OFlowMessage.class);

        Consumer<OFlowMessage> routingProxySendToDownstream = spy(Consumer.class);
        Consumer<OFlowMessage> routingProxySendToUpstream =spy(Consumer.class);
        Context context = new Context(routingProxySendToDownstream, routingProxySendToUpstream);

        SwitchMessageForwarder switchMessageForwarder = new SwitchMessageForwarder(apps, context);
        new MessageForwardingTask(switchMessageForwarder, oFlowMessage).run();

        InOrder inUpstreamOrder = inOrder(apps.get(0), apps.get(1), apps.get(2), routingProxySendToUpstream);
        inUpstreamOrder.verify(apps.get(0), times(1)).receivedFromSwitch(eq(oFlowMessage), any(Context.class));
        inUpstreamOrder.verify(apps.get(1), times(1)).receivedFromSwitch(eq(oFlowMessage), any(Context.class));
        inUpstreamOrder.verify(apps.get(2), times(1)).receivedFromSwitch(eq(oFlowMessage), any(Context.class));
        inUpstreamOrder.verify(routingProxySendToUpstream, times(1)).accept(oFlowMessage);
    }

    @Test
    public void testCorrectDownstreamForwarding(){
        buildApps(3);
        OFlowMessage oFlowMessage = mock(OFlowMessage.class);

        Consumer<OFlowMessage> routingProxySendToDownstream = spy(Consumer.class);
        Consumer<OFlowMessage> routingProxySendToUpstream =spy(Consumer.class);
        Context context = new Context(routingProxySendToDownstream, routingProxySendToUpstream);

        ControllerMessageForwarder controllerMessageForwarder = new ControllerMessageForwarder(apps, context);
        new MessageForwardingTask(controllerMessageForwarder, oFlowMessage).run();

        InOrder inUpstreamOrder = inOrder(apps.get(2), apps.get(1), apps.get(0), routingProxySendToDownstream);
        inUpstreamOrder.verify(apps.get(2), times(1)).receivedFromController(eq(oFlowMessage), any(Context.class));
        inUpstreamOrder.verify(apps.get(1), times(1)).receivedFromController(eq(oFlowMessage), any(Context.class));
        inUpstreamOrder.verify(apps.get(0), times(1)).receivedFromController(eq(oFlowMessage), any(Context.class));
        inUpstreamOrder.verify(routingProxySendToDownstream, times(1)).accept(oFlowMessage);
    }

    @Test
    public void testCorrectDownstreamForwardingWithOneApp(){
        buildApps(1);
        OFlowMessage oFlowMessage = mock(OFlowMessage.class);

        Consumer<OFlowMessage> routingProxySendToDownstream = spy(Consumer.class);
        Consumer<OFlowMessage> routingProxySendToUpstream =spy(Consumer.class);
        Context context = new Context(routingProxySendToDownstream, routingProxySendToUpstream);

        ControllerMessageForwarder controllerMessageForwarder = new ControllerMessageForwarder(apps, context);
        new MessageForwardingTask(controllerMessageForwarder, oFlowMessage).run();

        InOrder inUpstreamOrder = inOrder(apps.get(0), routingProxySendToDownstream);
        inUpstreamOrder.verify(apps.get(0), times(1)).receivedFromController(eq(oFlowMessage), any(Context.class));
        inUpstreamOrder.verify(routingProxySendToDownstream, times(1)).accept(oFlowMessage);
    }

    @Test
    public void testCorrectUpstreamForwardingWithOneApp(){
        buildApps(1);
        OFlowMessage oFlowMessage = mock(OFlowMessage.class);

        Consumer<OFlowMessage> routingProxySendToDownstream = spy(Consumer.class);
        Consumer<OFlowMessage> routingProxySendToUpstream =spy(Consumer.class);
        Context context = new Context(routingProxySendToDownstream, routingProxySendToUpstream);

        SwitchMessageForwarder switchMessageForwarder = new SwitchMessageForwarder(apps, context);
        new MessageForwardingTask(switchMessageForwarder, oFlowMessage).run();

        InOrder inUpstreamOrder = inOrder(apps.get(0), routingProxySendToUpstream);
        inUpstreamOrder.verify(apps.get(0), times(1)).receivedFromSwitch(eq(oFlowMessage), any(Context.class));
        inUpstreamOrder.verify(routingProxySendToUpstream, times(1)).accept(oFlowMessage);
    }

}
