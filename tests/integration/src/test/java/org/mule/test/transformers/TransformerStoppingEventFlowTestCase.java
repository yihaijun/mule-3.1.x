/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.RequestContext;
import org.mule.transformer.AbstractTransformer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TransformerStoppingEventFlowTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/transformers/transformer-stopped-processing.xml";
    }

    @Test
    public void testNullReturnStopsFlow() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage msg = client.send("vm://in", TEST_MESSAGE, null);
        assertNotNull(msg);
        final String payload = msg.getPayloadAsString();
        assertNotNull(payload);
        assertEquals(TEST_MESSAGE, payload);
    }

    public static final class StopFlowTransformer extends AbstractTransformer
    {


        protected Object doTransform(Object src, String encoding) throws TransformerException
        {
            RequestContext.getEventContext().setStopFurtherProcessing(true);
            return src;
        }
    }
}
