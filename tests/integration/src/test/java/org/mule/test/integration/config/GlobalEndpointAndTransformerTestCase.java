/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.config;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/*
 * This test has been added due to MULE-610
 */
public class GlobalEndpointAndTransformerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/config/globalendpointandtransformer-mule-config.xml";
    }

    @Test
    public void testNormal() throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage msg=client.send("vm://in",new DefaultMuleMessage("HELLO!", muleContext));
        assertTrue(msg.getPayload() instanceof byte[]);
    }
}


