/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MessagePropertyScopesTestCase extends FunctionalTestCase {

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/message-property-scopes-config.xml";
    }

    @Test
    public void testSessionProperty() throws Exception {

        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://in1", "Hello World", null);
        assertNotNull(response);
        String payload = response.getPayloadAsString();
        assertNotNull(payload);
        assertEquals("java.util.Date", payload);
    }

    /* Test fails
    @Test
    public void testInvocationProperty() throws Exception {
        
        MuleClient client = new MuleClient();
        MuleMessage response = client.send("vm://in2", "Hello World", null);
        // scope = "invocation" should not propagate the property on to the next service
        assertTrue(response.getPayload() instanceof NullPayload);
    }
    */
}
