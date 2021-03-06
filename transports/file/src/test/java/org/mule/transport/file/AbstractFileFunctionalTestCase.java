/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * We are careful here to access the file system in a generic way. This means setting
 * directories dynamically.
 */
public abstract class AbstractFileFunctionalTestCase extends FunctionalTestCase
{

    public static final String TEST_MESSAGE = "Test file contents";
    public static final String TARGET_FILE = "TARGET_FILE";

    private File tmpDir;

    @Override
    protected String getConfigResources()
    {
        return "file-functional-test.xml";
    }

    protected String fileToUrl(File file) throws MalformedURLException
    {
        return file.getAbsoluteFile().toURI().toURL().toString();
    }

    // annoying but necessary wait apparently due to OS caching?
    protected void waitForFileSystem() throws Exception
    {
        synchronized (this)
        {
            wait(1000);
        }
    }

    protected File initForRequest() throws Exception
    {
        createTempDirectory();
        File target = createAndPopulateTempFile();

        // define the readFromDirectory on the connector
        FileConnector connector = (FileConnector) muleContext.getRegistry().lookupConnector(
            "receiveConnector");
        connector.setReadFromDirectory(tmpDir.getAbsolutePath());
        logger.debug("Directory is " + connector.getReadFromDirectory());

        waitForFileSystem();
        return target;
    }

    private void createTempDirectory() throws Exception
    {
        tmpDir = File.createTempFile("mule-file-test-", "-dir");
        tmpDir.delete();
        tmpDir.mkdir();
    }

    private File createAndPopulateTempFile() throws Exception
    {
        File target = File.createTempFile("mule-file-test-", ".txt", tmpDir);

        Writer out = new FileWriter(target);
        out.write(TEST_MESSAGE);
        out.close();

        return target;
    }

    protected void checkReceivedMessage(MuleMessage message) throws Exception
    {
        assertNotNull(message);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayload() instanceof InputStream);

        InputStream fis = (InputStream) message.getPayload();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        IOUtils.copy(fis, byteOut);
        fis.close();
        String result = new String(byteOut.toByteArray());
        assertEquals(TEST_MESSAGE, result);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        FileUtils.deleteTree(tmpDir);
    }

}
