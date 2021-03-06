/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.routing.filter.Filter;
import org.mule.api.service.Service;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.file.filters.FilenameRegexFilter;
import org.mule.transport.file.transformers.FileToByteArray;
import org.mule.transport.file.transformers.FileToString;
import org.mule.util.FileUtils;

import java.io.File;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FileNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "file-namespace-config.xml";
    }

    @Override
    protected void doTearDown() throws Exception
    {
        File workDir = new File(".mule");
        assertTrue(FileUtils.deleteTree(workDir));
        
        super.doTearDown();
    }

    @Test
    public void testConfig() throws Exception
    {
        FileConnector c = (FileConnector)muleContext.getRegistry().lookupConnector("fileConnector");
        assertNotNull(c);
        
        assertEquals(1234, c.getFileAge());
        assertEquals("abc", c.getMoveToDirectory());
        assertEquals("bcd", c.getMoveToPattern());
        assertEquals("cde", c.getOutputPattern());
        assertEquals(2345, c.getPollingFrequency());
        assertEquals(".mule/readFromDirectory", c.getReadFromDirectory());
        assertEquals(".mule/writeToDirectory", c.getWriteToDirectory());
        assertEquals(".mule/workDirectory", c.getWorkDirectory());
        assertEquals("#[function:uuid]", c.getWorkFileNamePattern());
        assertEquals(false, c.isAutoDelete());
        assertEquals(true, c.isOutputAppend());
        assertEquals(true, c.isSerialiseObjects());
        assertEquals(false, c.isStreaming());
        assertTrue(c.isRecursive());

        // Not implemented yet, see MULE-2671
        //assertNull(c.getComparator());
        //assertFalse(c.isReverseOrder());

        FilenameParser parser = c.getFilenameParser();
        assertTrue(parser.getClass().getName(), c.getFilenameParser() instanceof DummyFilenameParser);

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testThirdConnector() throws Exception
    {
        FileConnector c = (FileConnector)muleContext.getRegistry().lookupConnector("thirdConnector");
        assertNotNull(c);

        FilenameParser parser = c.getFilenameParser();
        assertTrue(parser.getClass().getName(), c.getFilenameParser() instanceof ExpressionFilenameParser);

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
        assertFalse(c.isRecursive());
    }

    @Test
    public void testTransformersOnEndpoints() throws Exception
    {
        Object transformer1 = muleContext.getEndpointFactory().getInboundEndpoint("ep1").getTransformers().get(0);
        assertNotNull(transformer1);
        assertEquals(FileToByteArray.class, transformer1.getClass());

        Object transformer2 = muleContext.getEndpointFactory().getInboundEndpoint("ep2").getTransformers().get(0);
        assertNotNull(transformer2);
        assertEquals(FileToString.class, transformer2.getClass());
    }
    
    @Test
    public void testFileFilter() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("Test");
        assertNotNull(service);
        List endpoints = ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints();
        assertEquals(1, endpoints.size());

        InboundEndpoint endpoint = (InboundEndpoint) endpoints.get(0);
        Filter filter = endpoint.getFilter();
        assertNotNull(filter);

        assertTrue(filter instanceof FilenameRegexFilter);
        final FilenameRegexFilter f = (FilenameRegexFilter) filter;
        assertEquals(false, f.isCaseSensitive());
        assertEquals("(^SemDirector_Report-\\d)(.*)(tab$)", f.getPattern());
    }
}
