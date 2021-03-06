/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JmsRedeliveryTestCase extends AbstractJmsRedeliveryTestCase
{

    private static final int MAX_REDELIVERY = 3;

    public JmsRedeliveryTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected int getMaxRedelivery()
    {
        return MAX_REDELIVERY;
    }

    @Test
    public void testRedelivery() throws Exception
    {
        client.dispatch(JMS_INPUT_QUEUE, TEST_MESSAGE, null);

        assertTrue(messageRedeliveryExceptionFired.await(timeout, TimeUnit.MILLISECONDS));
        assertEquals("MessageRedeliveredException never fired.", 0, messageRedeliveryExceptionFired.getCount());
        assertEquals("Wrong number of delivery attempts", MAX_REDELIVERY + 1, callback.getCallbackCount());

        assertMessageInDlq();
    }
}
