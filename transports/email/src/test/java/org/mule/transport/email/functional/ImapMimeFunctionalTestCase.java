/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import org.junit.Test;

public class ImapMimeFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public ImapMimeFunctionalTestCase()
    {
        super(MIME_MESSAGE, "imap", "imap-mime-functional-test.xml");
    }

    @Test
    public void testRequest() throws Exception
    {
        doRequest();
    }

}
