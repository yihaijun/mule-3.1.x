/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class GroovyBindingFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "groovy-binding-config.xml";
    }

    @Test
    public void testBindingCallout() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("client_request", "Important Message", null);
        MuleMessage response = client.request("client_response", 2000);
        assertNotNull(response);
        assertEquals("Important Message Received by Callout1 Received by Callout2", response.getPayloadAsString());
    }

}
