/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.servlet.MuleReceiverServlet;
import org.mule.transport.servlet.jetty.util.EmbeddedJettyServer;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.Rule;
import org.junit.Test;

public class WsdlCallTestCase extends FunctionalTestCase
{

    @Rule
    public final DynamicPort jettyPort = new DynamicPort("jettyPort");

    @Rule
    public final DynamicPort httpPort = new DynamicPort("httpPort");

    private EmbeddedJettyServer httpServer;

    @Override
    protected String getConfigResources()
    {
        return "wsdl-conf.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        httpServer = new EmbeddedJettyServer(jettyPort.getNumber(), "/", "/services/*", new MuleReceiverServlet(), muleContext);
        httpServer.start();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if (httpServer != null && httpServer.isStarted())
        {
            httpServer.stop();
        }

        super.doTearDown();
    }

    @Test
    public void testRequestWsdlWithServlets() throws Exception
    {
        InputStream wsdlStream = new URL("http://localhost:" + jettyPort.getNumber()
            + "/services/mycomponent?wsdl").openStream();

        String location = "http://localhost:" + jettyPort.getNumber() + "/services/mycomponent";

        Document document = new SAXReader().read(wsdlStream);

        List nodes = document.selectNodes("//wsdl:definitions/wsdl:service");
        assertEquals("Callable", ((Element) nodes.get(0)).attribute("name").getStringValue());
        nodes = document.selectNodes("//wsdl:definitions/wsdl:service/wsdl:port/soap:address");
        assertEquals(location, ((Element) nodes.get(0)).attribute("location").getStringValue());
    }

    @Test
    public void testRequestWsdlWithHttp() throws Exception
    {
        String location = "http://localhost:" + httpPort.getNumber() + "/cxfService";
        InputStream wsdlStream = new URL(location + "?wsdl").openStream();

        Document document = new SAXReader().read(wsdlStream);
        List nodes = document.selectNodes("//wsdl:definitions/wsdl:service");
        assertEquals(((Element) nodes.get(0)).attribute("name").getStringValue(), "Callable");

        nodes = document.selectNodes("//wsdl:definitions/wsdl:service/wsdl:port/soap:address");
        assertEquals(location, ((Element) nodes.get(0)).attribute("location").getStringValue());
    }

}
