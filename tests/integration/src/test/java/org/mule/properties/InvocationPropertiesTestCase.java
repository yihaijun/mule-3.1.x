/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.properties;

import static org.junit.Assert.assertEquals;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class InvocationPropertiesTestCase extends org.mule.tck.junit4.FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @After
    public void clearFlowAssertions()
    {
        FlowAssert.reset();
    }

    @Test
    public void setInvocationPropertyUsingAPIGetInFlow() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        message.setProperty("P1", "P1_VALUE", PropertyScope.INVOCATION);

        testFlow("GetInvocationPropertyInFlow", event);
    }

    @Test
    public void setInvocationPropertyInFlowGetUsingAPI() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        SimpleFlowConstruct flowA = (SimpleFlowConstruct) muleContext.getRegistry().lookupFlowConstruct(
            "SetInvocationPropertyInFlow");
        MuleEvent result = flowA.process(event);

        assertEquals("P1_VALUE", result.getMessage().getProperty("P1", PropertyScope.INVOCATION));
    }

    @Test
    public void overwritePropertyValueInFlow() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        message.setProperty("P1", "P1_VALUE", PropertyScope.INVOCATION);

        testFlow("OverwritePropertyValueInFlow", event);

        assertEquals("P1_VALUE_NEW", event.getMessage().getProperty("P1", PropertyScope.INVOCATION));
    }

    @Test
    public void propagationInSameFlow() throws Exception
    {
        testFlow("propagationInSameFlow");
    }

    @Test
    public void noPropagationInDifferentFlowVMRequestResponse() throws Exception
    {
        testFlow("noPropagationInDifferentFlowVMRequestResponse");
        FlowAssert.verify("noPropagationInDifferentFlowVMRequestResponse-2");
    }

    @Test
    public void noPropagationInDifferentFlowVMOneWay() throws Exception
    {
        testFlow("noPropagationInDifferentFlowVMOneWay");
        FlowAssert.verify("noPropagationInDifferentFlowVMOneWay-2");
    }

    @Test
    public void noPropagationInDifferentFlowHttp() throws Exception
    {
        testFlow("noPropagationInDifferentFlowHttp");
        FlowAssert.verify("noPropagationInDifferentFlowHttp-2");
    }

    @Test
    public void propagationThroughOneWayFlowSedaQueue() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);

        muleContext.getClient().dispatch("vm://AsyncFlow", message);

        FlowAssert.verify("AsyncFlow");
    }

    @Test
    public void propagationWithVMRequestResponseOutboundEndpointMidFlow() throws Exception
    {
        testFlow("VMRequestResponseEndpointFlowMidFlow");
    }

    @Test
    public void propagationWithHTTPRequestResponseOutboundEndpointMidFlow() throws Exception
    {
        testFlow("HTTPRequestResponseEndpointFlowMidFlow");
    }

    @Test
    public void propagationThroughFlowRefToFlow() throws Exception
    {
        testFlow("propagationThroughFlowRefToFlow");
        FlowAssert.verify("FlowRef-1");
        FlowAssert.verify("FlowRef-2");
        FlowAssert.verify("FlowRef-3");
    }

    @Test
    public void overwritePropertyValueInFlowViaFlowRef() throws Exception
    {
        testFlow("OverwriteInFlowRef");
    }

    @Test
    public void propagationThroughFlowRefToSubFlow() throws Exception
    {
        testFlow("propagationThroughFlowRefToSubFlow");
    }

    @Test
    public void overwritePropertyValueInSubFlowViaFlowRef() throws Exception
    {
        testFlow("OverwriteInSubFlowRef");
    }

    @Test
    public void propagationThroughAsyncElement() throws Exception
    {
        testFlow("propagationThroughAsyncElement");
    }

    @Test
    public void propertyAddedInAsyncElementNotAddedinFlow() throws Exception
    {
        testFlow("propertyAddedInAsyncElementNotAddedinFlow");
    }

    @Test
    public void propagationThroughWireTap() throws Exception
    {
        testFlow("propagationThroughWireTap");
    }

    @Test
    public void propertyAddedInWireTapNotAddedinFlow() throws Exception
    {
        testFlow("propertyAddedInWireTapNotAddedinFlow");
    }

    @Test
    public void propagationThroughEnricher() throws Exception
    {
        testFlow("propagationThroughEnricher");
    }

    @Test
    public void propertyAddedInEnricherNotAddedinFlow() throws Exception
    {
        testFlow("propertyAddedInEnricherNotAddedinFlow");
    }

    @Test
    public void propagateToRoutesInAll() throws Exception
    {
        testFlow("propagateToRoutesInAll");
    }

    @Test
    public void propagateThroughAllRouterWithResults() throws Exception
    {
        testFlow("propagateThroughAllRouterWithResults");
    }

    @Test
    public void propagateThroughAllRouterWithNoResults() throws Exception
    {
        testFlow("propagateThroughAllRouterWithNoResults");
    }

    @Test
    public void noPropagateBetweenRoutes() throws Exception
    {
        testFlow("noPropagateBetweenRoutes");
    }

    @Test
    public void noPropagateFromRouteToNextProcessorSingleRoute() throws Exception
    {
        testFlow("noPropagateFromRouteToNextProcessorSingleRoute");
    }

    @Test
    public void noPropagateFromRouteToNextProcessorMultipleRoutes() throws Exception
    {
        testFlow("noPropagateFromRouteToNextProcessorMultipleRoutes");
    }

    @Test
    public void noPropagateFromRouteToNextProcessorNoResult() throws Exception
    {
        testFlow("noPropagateFromRouteToNextProcessorNoResult");
    }

    @Test
    public void allAsync() throws Exception
    {
        testFlow("AllAsync");
    }

    @Test
    public void propogationOfPropertiesInMessageSplitWithSplitter() throws Exception
    {
        List<Fruit> fruitList = new ArrayList<Fruit>();
        fruitList.add(new Apple());
        fruitList.add(new Orange());
        fruitList.add(new Banana());
        testFlow("propogationOfPropertiesInMessageSplitWithSplitter", getTestEvent(fruitList));
    }

    @Test
    public void aggregationOfPropertiesFromMultipleMessageWithAggregator() throws Exception
    {
        List<Fruit> fruitList = new ArrayList<Fruit>();
        fruitList.add(new Apple());
        fruitList.add(new Orange());
        fruitList.add(new Banana());
        testFlow("aggregationOfPropertiesFromMultipleMessageWithAggregator", getTestEvent(fruitList));
        FlowAssert.verify("Split");
    }
    
    @Test
    public void defaultExceptionStrategy() throws Exception
    {
        testFlow("defaultExceptionStrategy");
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/properties/invocation-properties-config.xml";
    }

}
