/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.ClassUtils;
import org.mule.util.FilenameUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MuleServerTestCase extends AbstractMuleTestCase
{

    @Test
    public void testMuleServer() throws Exception
    {
        MuleServer muleServer = new MuleServer();
        assertEquals(ClassUtils.getResource("mule-config.xml", MuleServer.class).toString(),
            muleServer.getConfigurationResources());
        assertEquals(MuleServer.CLASSNAME_DEFAULT_CONFIG_BUILDER, MuleServer.getConfigBuilderClassName());
        muleServer.initialize();
    }

    @Test
    public void testMuleServerResource() throws Exception
    {
        MuleServer muleServer = new MuleServer("org/mule/test/spring/config1/test-xml-mule2-config.xml");
        assertEquals("org/mule/test/spring/config1/test-xml-mule2-config.xml", muleServer.getConfigurationResources());
        assertEquals(MuleServer.CLASSNAME_DEFAULT_CONFIG_BUILDER, MuleServer.getConfigBuilderClassName());
        muleServer.initialize();
    }

    @Test
    public void testMuleServerConfigArg() throws Exception
    {
        MuleServer muleServer = new MuleServer(new String[]{"-config",
            "org/mule/test/spring/config1/test-xml-mule2-config.xml"});
        assertEquals("org/mule/test/spring/config1/test-xml-mule2-config.xml", muleServer.getConfigurationResources());
        assertEquals(MuleServer.CLASSNAME_DEFAULT_CONFIG_BUILDER, MuleServer.getConfigBuilderClassName());
        muleServer.initialize();
    }

    @Test
    public void testMuleServerMultipleSpringConfigArgs() throws Exception
    {
        MuleServer muleServer = new MuleServer(new String[]{"-config",
            "mule-config.xml,org/mule/test/spring/config1/test-xml-mule2-config.xml"});
        assertEquals("mule-config.xml,org/mule/test/spring/config1/test-xml-mule2-config.xml",
            muleServer.getConfigurationResources());
        assertEquals(MuleServer.CLASSNAME_DEFAULT_CONFIG_BUILDER, MuleServer.getConfigBuilderClassName());
        muleServer.initialize();
    }

    @Test
    public void testMuleServerBuilerArg() throws Exception
    {
        MuleServer muleServer = new MuleServer(new String[]{"-builder",
            "org.mule.config.spring.SpringXmlConfigurationBuilder"});
        assertEquals(ClassUtils.getResource("mule-config.xml", MuleServer.class).toString(),
            muleServer.getConfigurationResources());
        assertEquals("org.mule.config.spring.SpringXmlConfigurationBuilder", MuleServer.getConfigBuilderClassName());
        muleServer.initialize();
    }

    @Test
    public void testMuleServerSpringBuilerArg() throws Exception
    {
        MuleServer muleServer = new MuleServer(new String[]{"-builder", "spring"});
        assertEquals(ClassUtils.getResource("mule-config.xml", MuleServer.class).toString(),
            muleServer.getConfigurationResources());
        assertEquals("org.mule.config.spring.SpringXmlConfigurationBuilder", MuleServer.getConfigBuilderClassName());
        muleServer.initialize();
    }
    
    @Test
    public void testMuleServerAppConfig() throws Exception
    {
        MuleServer muleServer = new MuleServer(new String[]{
            "-config",
            "mule-config.xml",
            "-appconfig",
            "org/mule/test/spring/config1/test-app-config.properties"});
        muleServer.initialize();
        final String workingDirectory = MuleServer.muleContext.getConfiguration().getWorkingDirectory();
        assertTrue(FilenameUtils.separatorsToUnix(workingDirectory).endsWith("/target/.appT"));
    }
}
