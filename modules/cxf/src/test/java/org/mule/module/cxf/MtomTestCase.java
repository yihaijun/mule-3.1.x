/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.mime.TestMtom;
import org.apache.cxf.mime.TestMtomService;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MtomTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "mtom-conf.xml";
    }

    @Test
    public void testEchoService() throws Exception
    {
        URL wsdl = getClass().getResource("/wsdl/mtom_xop.wsdl");
        assertNotNull(wsdl);

        CxfConfiguration clientConfig = new CxfConfiguration();
        clientConfig.setMuleContext(muleContext);
        clientConfig.initialise();
        BusFactory.setThreadDefaultBus(clientConfig.getCxfBus());

        TestMtomService svc = new TestMtomService(wsdl);

        TestMtom port = svc.getTestMtomPort();

        BindingProvider bp = ((BindingProvider) port);
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
            "http://localhost:" + dynamicPort.getNumber() + "/services/mtom");
        ((SOAPBinding) bp.getBinding()).setMTOMEnabled(true);
        // Client client = ClientProxy.getClient(port);
        // new LoggingFeature().initialize(client, null);

        File file = new File("src/test/resources/mtom-conf.xml");
        DataHandler dh = new DataHandler(new FileDataSource(file));

        Holder<String> name = new Holder<String>("test");
        Holder<DataHandler> info = new Holder<DataHandler>(dh);

        port.testXop(name, info);

        assertEquals("return detail + test", name.value);
        assertNotNull(info.value);

        InputStream input = info.value.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(input, bos);
        input.close();
    }

}

