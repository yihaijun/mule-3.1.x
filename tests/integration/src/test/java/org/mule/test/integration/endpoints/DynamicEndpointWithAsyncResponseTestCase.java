/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.endpoints;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DynamicEndpointWithAsyncResponseTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/endpoints/dynamic-endpoint-with-async-response-config.xml";
    }

    @Test
    public void testDynamicEndpointWithAsyncResponse() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("hello", muleContext);
        message.setOutboundProperty("host", "localhost");
        message.setOutboundProperty("port", dynamicPort.getNumber());
        message.setOutboundProperty("path", "/TEST");

        DefaultLocalMuleClient client = new DefaultLocalMuleClient(muleContext);
        MuleMessage response = client.send("vm://vmProxy", message);
        assertEquals("hello Received", response.getPayloadAsString());

        response = client.request("vm://vmOut", 5000);
        assertNotNull(response);
    }
}
