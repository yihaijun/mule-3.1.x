/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExpressionRecipientListAsyncTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/expression-recipient-list-async-test.xml";
    }

    @Test
    public void testRecipientList() throws Exception
    {
        String message = "test";
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>(3);
        props.put("recipient1", "vm://service1.queue");
        props.put("recipient2", "vm://service2.queue");
        props.put("recipient3", "vm://service3.queue");
        client.dispatch("vm://distributor.queue", message, props);

        List<Object> results = new ArrayList<Object>(3);

        MuleMessage result = client.request("vm://collector.queue", 5000);
        assertNotNull(result);
        results.add(result.getPayload());

        result = client.request("vm://collector.queue", 3000);
        assertNotNull(result);
        results.add(result.getPayload());

        result = client.request("vm://collector.queue", 3000);
        assertNotNull(result);
        results.add(result.getPayload());

        assertTrue(results.contains("test 1 Received"));
        assertTrue(results.contains("test 2 Received"));
        assertTrue(results.contains("test 3 Received"));
    }
}
