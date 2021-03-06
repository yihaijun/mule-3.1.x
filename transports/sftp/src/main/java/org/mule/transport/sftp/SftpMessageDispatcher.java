/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.sftp.notification.SftpNotifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * <code>SftpMessageDispatcher</code> dispatches files via sftp to a remote sftp
 * service. This dispatcher reads an InputStream, byte[], or String payload, which is
 * then streamed to the SFTP endpoint.
 */

public class SftpMessageDispatcher extends AbstractMessageDispatcher
{

    private SftpConnector connector;
    private SftpUtil sftpUtil = null;

    public SftpMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        connector = (SftpConnector) endpoint.getConnector();
        sftpUtil = new SftpUtil(endpoint);
    }

    // protected void doConnect() throws Exception
    // {
    // super.doConnect();
    //
    // SftpClient client = null;
    // if (sftpUtil.isUseTempDir()) {
    // if (logger.isDebugEnabled()) {
    // logger.debug("Initializing temp directory for endpoint " +
    // super.getEndpoint().getEndpointURI());
    // }
    //
    // try {
    // client = connector.createSftpClient(endpoint, null);
    //
    // sftpUtil.createSftpDirIfNotExists(client,
    // endpoint.getEndpointURI().getPath());
    // } finally {
    // if (client != null) {
    // // If the connection fails, the client will be null, otherwise disconnect.
    // connector.releaseClient(endpoint, client);
    // }
    // }
    // }
    // }

    protected void doDisconnect() throws Exception
    {
        // no op
    }

    // protected MuleMessage doReceive(long l)
    // {
    // throw new UnsupportedOperationException("doReceive");
    // }

    protected void doDispose()
    {
        // no op
    }

    protected void doDispatch(MuleEvent event) throws Exception
    {
        Object data = event.getMessage().getPayload();
        // this is outbound because the props are copied into the outbound scope
        // during processing
        String filename = (String) event.getMessage().findPropertyInAnyScope(SftpConnector.PROPERTY_FILENAME,
            null);
        // If no name specified, set filename according to output pattern specified
        // on
        // endpoint or connector

        if (filename == null)
        {
            MuleMessage message = event.getMessage();

            String outPattern = (String) endpoint.getProperty(SftpConnector.PROPERTY_OUTPUT_PATTERN);
            if (outPattern == null)
            {
                outPattern = (String) message.getProperty(SftpConnector.PROPERTY_OUTPUT_PATTERN,
                    connector.getOutputPattern());
            }
            filename = generateFilename(message, outPattern);
        }

        // byte[], String, or InputStream payloads supported.

        byte[] buf;
        InputStream inputStream;

        if (data instanceof byte[])
        {
            buf = (byte[]) data;
            inputStream = new ByteArrayInputStream(buf);
        }
        else if (data instanceof InputStream)
        {
            inputStream = (InputStream) data;
        }
        else if (data instanceof String)
        {
            inputStream = new ByteArrayInputStream(((String) data).getBytes());

        }
        else
        {
            throw new IllegalArgumentException(
                "Unexpected message type: java.io.InputStream, byte[], or String expected. Got "
                                + data.getClass().getName());
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Writing file to: " + endpoint.getEndpointURI() + " [" + filename + "]");

        }

        SftpClient client = null;
        boolean useTempDir = false;
        String transferFilename = null;

        try
        {
            String serviceName = (event.getFlowConstruct() == null)
                                                                   ? "UNKNOWN SERVICE"
                                                                   : event.getFlowConstruct().getName();
            SftpNotifier notifier = new SftpNotifier(connector, event.getMessage(), endpoint, serviceName);
            client = connector.createSftpClient(endpoint, notifier);
            String destDir = endpoint.getEndpointURI().getPath();

            if (logger.isDebugEnabled())
            {
                logger.debug("Connection setup successful, writing file.");
            }

            // Duplicate Handling
            filename = client.duplicateHandling(destDir, filename, sftpUtil.getDuplicateHandling());
            transferFilename = filename;

            useTempDir = sftpUtil.isUseTempDirOutbound();
            if (useTempDir)
            {
                // TODO move to a init-method like doConnect?
                // cd to tempDir and create it if it doesn't already exist
                sftpUtil.cwdToTempDirOnOutbound(client, destDir);

                // Add unique file-name (if configured) for use during transfer to
                // temp-dir
                boolean addUniqueSuffix = sftpUtil.isUseTempFileTimestampSuffix();
                if (addUniqueSuffix)
                {
                    transferFilename = sftpUtil.createUniqueSuffix(transferFilename);
                }
            }

            // send file over sftp
            client.storeFile(transferFilename, inputStream);

            if (useTempDir)
            {
                // Move the file to its final destination
                client.rename(transferFilename, destDir + "/" + filename);
            }

            logger.info("Successfully wrote file '" + filename + "' to " + endpoint.getEndpointURI());
        }
        catch (Exception e)
        {
            logger.error("Unexpected exception attempting to write file, message was: " + e.getMessage(), e);

            sftpUtil.setErrorOccurredOnInputStream(inputStream);

            if (useTempDir)
            {
                // Cleanup the remote temp dir from the not fullt completely
                // transferred file!
                String tempDir = sftpUtil.getTempDirOutbound();
                sftpUtil.cleanupTempDir(client, transferFilename, tempDir);
            }
            throw e;
        }
        finally
        {
            if (client != null)
            {
                // If the connection fails, the client will be null, otherwise
                // disconnect.
                connector.releaseClient(endpoint, client);
            }
            // else
            // {
            // logger.warn("Unexpected null SFTPClient instance - operation probably failed ...");
            // }

            inputStream.close();

        }

    }

    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        doDispatch(event);
        return event.getMessage();
    }

    // public Object getDelegateSession()
    // {
    // return null;
    // }

    private String generateFilename(MuleMessage message, String pattern)
    {
        if (pattern == null)
        {
            pattern = connector.getOutputPattern();
        }
        return connector.getFilenameParser().getFilename(message, pattern);
    }
}
