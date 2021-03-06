/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.service.Service;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.mockobjects.dynamic.Mock;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class IdempotentMessageFilterTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testIdempotentReceiver() throws Exception
    {
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://Test1Provider?exchangePattern=one-way");
        Mock session = MuleTestUtils.getMockSession();
        Service service = getTestService();
        session.matchAndReturn("getFlowConstruct", service);


        IdempotentMessageFilter ir = new IdempotentMessageFilter();
        ir.setIdExpression("#[header:id]");
        ir.setFlowConstruct(service);
        ir.setThrowOnUnaccepted(false);

        MuleMessage okMessage = new DefaultMuleMessage("OK", muleContext);
        okMessage.setOutboundProperty("id", "1");
        MuleEvent event = new DefaultMuleEvent(okMessage, endpoint1, (MuleSession) session.proxy());

        // This one will process the event on the target endpoint
        event = ir.process(event);
        assertNotNull(event);

         // This will not process, because the ID is a duplicate
        okMessage = new DefaultMuleMessage("OK", muleContext);
        okMessage.setOutboundProperty("id", "1");
        event = new DefaultMuleEvent(okMessage, endpoint1, (MuleSession) session.proxy());
        event = ir.process(event);
        assertNull(event);
    }

}
