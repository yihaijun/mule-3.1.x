/*
 * $Id: XsltTransformer.java 20704 2010-12-14 15:56:00Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.xml.util.LocalURIResolver;
import org.mule.module.xml.util.XMLUtils;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import org.mule.api.transport.OutputHandler;

/**
 * <code>XsltTransformer</code> performs an XSLT transform on a DOM (or other XML-ish)
 * object.
 * <p/>
 * This transformer maintains a pool of {@link javax.xml.transform.Transformer} objects to speed up processing of concurrent requests.
 * The pool can be configured using {@link #setMaxIdleTransformers(int)}.
 * <p/>
 * Parameter can also be set as part of the transformation context and these can be mapped to conent in the current message using
 * property extractors or can be fixed values.
 * <p/>
 * <p/>
 * For example, the current event's message has a property named "myproperty", also you want to generate a uuid as a
 * parameter. To do this you can define context properties that can provide an expression to be evaluated on the current
 * message.
 * </p>
 * <p>
 * Example Configuration:
 * </p>
 * <p/>
 * <pre>
 *  &lt;mxml:xslt-transformer name=&quot;MyXsltTransformer&quot; xslFile=&quot;myXslFile.xsl&quot;&amp;gt
 *      &lt;context-property name=&quot;myParameter&quot; value=&quot;#[head:myproperty]&quot;/&amp;gt
 *      &lt;context-property name=&quot;myParameter2&quot; value=&quot;#[function:uuid]&quot;/&amp;gt
 *  &lt;/mxml:xslt-transformer&amp;gt
 * </pre>
 * <p/>
 * <p>
 * The 'header' expression pulls a header from the current message and 'function' can execute a set of arbitrary functions.
 * You can also pass in static values by ommitting the expression prefix '#['.
 * </p>
 * <p/>
 * In addition to being able to pass in an XSLT file you can also define templates inline. For example -
 * <p/>
 * <pre>
 *  &lt;mxml:xslt-transformer name=&quot;MyXsltTransformer&quot;&amp;gt
 *      &lt;mxml:xslt-text&amp;gt
 *          <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://test.com" version="2.0">
 * <p/>
 *                <xsl:param name="echo"/>
 * <p/>
 *               <xsl:template match="/">
 *                   <echo-value>
 *                       <xsl:value-of select="$echo"/>
 *                   </echo-value>
 *               </xsl:template>
 *           </xsl:stylesheet>
 *  &lt;/mxml:xslt-text&amp;gt
 * </pre>
 */

public class XsltTransformer extends AbstractXmlTransformer
{
    // keep at least 1 XSLT Transformer ready by default
    private static final int MIN_IDLE_TRANSFORMERS = 1;
    // keep max. 32 XSLT Transformers around by default
    private static final int MAX_IDLE_TRANSFORMERS = 100;
    // MAX_IDLE is also the total limit
    private static final int MAX_ACTIVE_TRANSFORMERS = MAX_IDLE_TRANSFORMERS;

    //Saxon shipped with Mule
    public static final String PREFERRED_TRANSFORMER_FACTORY = "net.sf.saxon.TransformerFactoryImpl";

    protected final GenericObjectPool transformerPool;

    /**
     * Default to Saxon
     */
    private volatile String xslTransformerFactoryClassName = PREFERRED_TRANSFORMER_FACTORY;
    private volatile String xslFile;
    private volatile String xslt;
    private volatile Map contextProperties;

    private URIResolver uriResolver;

    public XsltTransformer()
    {
        super();
        transformerPool = new GenericObjectPool(new PooledXsltTransformerFactory());
        transformerPool.setMinIdle(MIN_IDLE_TRANSFORMERS);
        transformerPool.setMaxIdle(MAX_IDLE_TRANSFORMERS);
        transformerPool.setMaxActive(MAX_ACTIVE_TRANSFORMERS);
        uriResolver = new LocalURIResolver();
        contextProperties = new HashMap();
    }

    public XsltTransformer(String xslFile)
    {
        this();
        this.setXslFile(xslFile);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        logger.debug("Initialising transformer: " + this);
        try
        {
            // Only load the file once at initialize time
            if (xslFile != null)
            {
                //this.xslt = IOUtils.getResourceAsString(xslFile, getClass());
                InputStream is = muleContext.getExecutionClassLoader().getResourceAsStream(xslFile);
                this.xslt = org.apache.commons.io.IOUtils.toString(is);
 
           }
            transformerPool.addObject();
            logger.debug("transformerPool--name,MaxActive,MinIdle,NumActive:" + name + "," + transformerPool.getMaxActive()
                    + "," + transformerPool.getMinIdle() + "," + transformerPool.getNumActive());
        }
        catch (Throwable te)
        {
            throw new InitialisationException(te, this);
        }
    }

    /**
     * Transform, using XSLT, a XML String to another String.
     *
     * @return The result in the type specified by the user
     */
    @Override
    public Object transformMessage(MuleMessage message, String encoding) throws TransformerException
    {
        Object src = message.getPayload();
        int len = 0;
        if (src instanceof javax.xml.transform.Source) {
        } else if (src instanceof byte[]) {
            len = ((byte[]) src).length;
        } else if (src instanceof InputStream) {
        } else if (src instanceof String) {
            len = src.toString().length();
        } else if (src instanceof org.dom4j.Document) {
            len = ((org.dom4j.Document) src).getText().length();
        } else if (src instanceof org.xml.sax.InputSource) {
        } else if (src instanceof XMLStreamReader) {
        } else if (src instanceof org.w3c.dom.Document || src instanceof org.w3c.dom.Element) {
            if (src instanceof org.w3c.dom.Document) {
                len = ((org.w3c.dom.Document) src).getTextContent().length();
            } else if (src instanceof org.w3c.dom.Element) {
                len = ((org.w3c.dom.Element) src).getTextContent().length();
            }
        } else if (src instanceof DelayedResult) {
        } else if (src instanceof OutputHandler) {
        } else {
            return null;
        }
        if (len >= 5 * 1024 * 1024 / 10) {
            logger.warn("Do not query so much records or such a big message package(" + (len / 1024) + "K>0.5m)");
        }
        try
        {
            Source sourceDoc = XMLUtils.toXmlSource(getXMLInputFactory(), isUseStaxSource(), src);
            if (sourceDoc == null)
            {
                return null;
            }

            ResultHolder holder = getResultHolder(returnType.getType());

            // If the users hasn't specified a class, lets return the same type they gave us
            if (holder == null)
            {
                holder = getResultHolder(src.getClass());
            }

            // If we still don't have a result type, lets fall back to using a DelayedResult
            // as it is the most efficient.
            if (holder == null || DelayedResult.class.equals(returnType.getType()))
            {
                return getDelayedResult(message, encoding, sourceDoc);
            }

            doTransform(message, encoding, sourceDoc, holder.getResult());

            return holder.getResultObject();
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected Object getDelayedResult(final MuleMessage message, final String encoding, final Source sourceDoc)
    {
        return new DelayedResult()
        {
            private String systemId;

            public void write(Result result) throws Exception
            {
                doTransform(message, encoding, sourceDoc, result);
            }

            public String getSystemId()
            {
                return systemId;
            }

            public void setSystemId(String systemId)
            {
                this.systemId = systemId;
            }
        };
    }

    protected void doTransform(MuleMessage message, String encoding, Source sourceDoc, Result result)
            throws Exception
    {
        DefaultErrorListener errorListener = new DefaultErrorListener(this);
        javax.xml.transform.Transformer transformer = null;

        try
        {
            transformer = (javax.xml.transform.Transformer) transformerPool.borrowObject();

            transformer.setErrorListener(errorListener);
            transformer.setOutputProperty(OutputKeys.ENCODING, encoding);

            // set transformation parameters
            if (contextProperties != null)
            {
                for (Iterator i = contextProperties.entrySet().iterator(); i.hasNext();)
                {
                    Map.Entry parameter = (Entry) i.next();
                    String key = (String) parameter.getKey();
                    transformer.setParameter(key, evaluateTransformParameter(key, parameter.getValue(), message));
                }
            }

            transformer.transform(sourceDoc, result);

            if (errorListener.isError())
            {
                throw errorListener.getException();
            }
        }
        finally
        {
            if (transformer != null)
            {
                // clear transformation parameters before returning transformer to the
                // pool
                transformer.clearParameters();

                transformerPool.returnObject(transformer);
            }
        }
    }

    /**
     * Returns the name of the currently configured javax.xml.transform.Transformer
     * factory class used to create XSLT Transformers.
     *
     * @return a TransformerFactory class name or <code>null</code> if none has been
     *         configured
     */
    public String getXslTransformerFactory()
    {
        return xslTransformerFactoryClassName;
    }

    /**
     * Configures the javax.xml.transform.Transformer factory class
     *
     * @param xslTransformerFactory the name of the TransformerFactory class to use
     */
    public void setXslTransformerFactory(String xslTransformerFactory)
    {
        this.xslTransformerFactoryClassName = xslTransformerFactory;
    }

    /**
     * @return Returns the xslFile.
     */
    public String getXslFile()
    {
        return xslFile;
    }

    /**
     * @param xslFile The xslFile to set.
     */
    public void setXslFile(String xslFile)
    {
        this.xslFile = xslFile;
    }

    public String getXslt()
    {
        return xslt;
    }

    public void setXslt(String xslt)
    {
        this.xslt = xslt;
    }

    public URIResolver getUriResolver()
    {
        return uriResolver;
    }

    public void setUriResolver(URIResolver uriResolver)
    {
        this.uriResolver = uriResolver;
    }

    /**
     * Returns the StreamSource corresponding to xslt (which should have been loaded
     * in {@link #initialise()}).
     * 
     * @return The StreamSource
     */
    protected StreamSource getStreamSource() throws InitialisationException
    {
        if (xslt == null)
        {
            throw new InitialisationException(CoreMessages.propertiesNotSet("xsl-file or xsl-text"), this);
        }
        else
        {
            return new StreamSource(new StringReader(xslt));
        }
    }

    protected class PooledXsltTransformerFactory extends BasePoolableObjectFactory
    {
        @Override
        public Object makeObject() throws Exception
        {
            StreamSource source = XsltTransformer.this.getStreamSource();
            String factoryClassName = XsltTransformer.this.getXslTransformerFactory();
            TransformerFactory factory;

            logger.debug("factory instanceof default:" + factoryClassName);
            if (PREFERRED_TRANSFORMER_FACTORY.equals(factoryClassName) && !ClassUtils.isClassOnPath(factoryClassName, getClass()))
            {
                logger.warn("Preferred Transfomer Factory " + PREFERRED_TRANSFORMER_FACTORY + " not on classpath and no default is set, defaulting to JDK");
                factoryClassName = null;
            }

            if (StringUtils.isNotEmpty(factoryClassName))
            {

                factory = (TransformerFactory) ClassUtils.instanciateClass(factoryClassName,
                        ClassUtils.NO_ARGS, this.getClass());
            }
            else
            {
                // fall back to JDK default
                try
                {
                    factory = TransformerFactory.newInstance();
                }
                catch (TransformerFactoryConfigurationError e)
                {
                    System.setProperty("javax.xml.transform.TransformerFactory", XMLUtils.TRANSFORMER_FACTORY_JDK5);
                    factory = TransformerFactory.newInstance();
                }
            }
//            if (System.getProperty("asip.transformer.xslt.caching","false").equalsIgnoreCase("true")) {
//                if (factory instanceof com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl) {
//                    System.setProperty("javax.xml.transform.TransformerFactory",
//                            "com.regaltec.asip.transformer.xslt.XalanCachingTransformerFactory");
//                    factory = TransformerFactory.newInstance();
//                } else if (factory instanceof net.sf.saxon.TransformerFactoryImpl) {
//                    System.setProperty("javax.xml.transform.TransformerFactory",
//                            "com.regaltec.asip.transformer.xslt.SaxonCachingTransformerFactory");
//                    factory = TransformerFactory.newInstance();
//                } else if (factory instanceof org.apache.xalan.processor.TransformerFactoryImpl) {
//                    System.setProperty("javax.xml.transform.TransformerFactory",
//                            "com.regaltec.asip.transformer.xslt.ApacheXalanProcessorCachingTransformerFactory");
//                    factory = TransformerFactory.newInstance();
//                }else{
//                }
//            }

            logger.debug("factory instanceof actual:" + factory.getClass().getName());
            factory.setURIResolver(getUriResolver());
            return factory.newTransformer(source);
        }
    }

    protected class DefaultErrorListener implements ErrorListener
    {
        private TransformerException e = null;
        private final Transformer trans;

        public DefaultErrorListener(Transformer trans)
        {
            this.trans = trans;
        }

        public TransformerException getException()
        {
            return e;
        }

        public boolean isError()
        {
            return e != null;
        }

        public void error(javax.xml.transform.TransformerException exception)
                throws javax.xml.transform.TransformerException
        {
            e = new TransformerException(trans, exception);
        }

        public void fatalError(javax.xml.transform.TransformerException exception)
                throws javax.xml.transform.TransformerException
        {
            e = new TransformerException(trans, exception);
        }

        public void warning(javax.xml.transform.TransformerException exception)
                throws javax.xml.transform.TransformerException
        {
            logger.warn(exception.getMessage());
        }
    }

    /**
     * @return The current maximum number of allowable active transformer objects in
     *         the pool
     */
    public int getMaxActiveTransformers()
    {
        return transformerPool.getMaxActive();
    }

    /**
     * Sets the the current maximum number of active transformer objects allowed in the
     * pool
     *
     * @param maxActiveTransformers New maximum size to set
     */
    public void setMaxActiveTransformers(int maxActiveTransformers)
    {
        transformerPool.setMaxActive(maxActiveTransformers);
    }

    /**
     * @return The current maximum number of allowable idle transformer objects in the
     *         pool
     */
    public int getMaxIdleTransformers()
    {
        return transformerPool.getMaxIdle();
    }

    /**
     * Sets the the current maximum number of idle transformer objects allowed in the pool
     *
     * @param maxIdleTransformers New maximum size to set
     */
    public void setMaxIdleTransformers(int maxIdleTransformers)
    {
        transformerPool.setMaxIdle(maxIdleTransformers);
    }

    /**
     * Gets the parameters to be used when applying the transformation
     *
     * @return a map of the parameter names and associated values
     * @see javax.xml.transform.Transformer#setParameter(java.lang.String,
     *      java.lang.Object)
     */
    public Map getContextProperties()
    {
        return contextProperties;
    }

    /**
     * Sets the parameters to be used when applying the transformation
     *
     * @param contextProperties a map of the parameter names and associated values
     * @see javax.xml.transform.Transformer#setParameter(java.lang.String,
     *      java.lang.Object)
     */
    public void setContextProperties(Map contextProperties)
    {
        this.contextProperties = contextProperties;
    }

    /**
     * Returns the value to be set for the parameter. This method is called for each
     * parameter before it is set on the transformer. The purpose of this method is to
     * allow dynamic parameters related to the event (usually message properties) to be
     * used. Any attribute of the current MuleEvent can be accessed using Property Extractors
     * such as JXpath, bean path or header retrieval.
     *
     * @param name  the name of the parameter. The name isn't used for this implementation but is exposed as a
     *              param for classes that may need it.
     * @param value the value of the paramter
     * @return the object to be set as the parameter value
     * @throws TransformerException
     */
    protected Object evaluateTransformParameter(String name, Object value, MuleMessage message) throws TransformerException
    {
        if (value instanceof String)
        {
            return muleContext.getExpressionManager().parse(value.toString(), message);
        }

        return value;
    }
}