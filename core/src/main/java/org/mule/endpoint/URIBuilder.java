/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.util.ClassUtils;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

/**
 * This has the following logic:
 * - if an address is specified, it is used verbatim (except for parameters); this is consistent with the generic case
 * - otherwise, we construct from components, omitting things that aren't specified as much as possible
 * (use required attributes to guarantee entries)
 *
 * In addition, parameters are handled as follows:
 * - parameters can be given in the uri, the queryMap, or both
 * - queryMap values override uri values
 * - the order of parameters in the uri remains the same (even if values change)
 * - queryMap parameters are appended after uri parameters
 *
 * TODO - check that we have sufficient control via XML (what about empty strings?)
 *
 * Not called EndpointURIBuilder because of {@link org.mule.api.endpoint.EndpointURIBuilder}
 * 
 */
public class URIBuilder
{

    private static final String DOTS = ":";
    private static final String DOTS_SLASHES = DOTS + "//";
    private static final String QUERY = "?";
    private static final String AND = "&";
    private static final String EQUALS = "=";
    private static final String BACKSLASH = "\\";

    public static final String META = "meta";
    public static final String PROTOCOL = "protocol";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String HOST = "host";
    public static final String ADDRESS = "address";
    public static final String PORT = "port";
    public static final String PATH = "path";

    public static final String[] ALL_ATTRIBUTES =
            new String[]{META, PROTOCOL, USER, PASSWORD, HOST, ADDRESS, PORT, PATH};
    // combinations used in various endpoint parsers to validate required attributes
    public static final String[] PATH_ATTRIBUTES = new String[]{PATH};
    public static final String[] HOST_ATTRIBUTES = new String[]{HOST};
    public static final String[] SOCKET_ATTRIBUTES = new String[]{HOST, PORT};
    public static final String[] USERHOST_ATTRIBUTES = new String[]{USER, HOST};
    // this doesn't include address, since that is handled separately (and is exclusive with these)
    public static final String[] ALL_TRANSPORT_ATTRIBUTES = new String[]{USER, PASSWORD, HOST, PORT, PATH};

    private String address;
    private String meta;
    private String protocol;
    private String user;
    private String password;
    private String host;
    private Integer port;
    private String path;
    private Map queryMap;
    private MuleContext muleContext;

    private AtomicReference cache = new AtomicReference();

    public URIBuilder()
    {
        //default for spring. Must call setMulecontext().
    }

    public URIBuilder(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public URIBuilder(EndpointURI endpointURI)
    {
        this(endpointURI.getMuleContext());
        cache.set(endpointURI);
    }

    public URIBuilder(String address, MuleContext muleContext)
    {
        this(muleContext);
        // separate meta from address, if necessary
        int dots = address.indexOf(DOTS);
        int dotsSlashes = address.indexOf(DOTS_SLASHES);
        if (dots > -1 && dots < dotsSlashes)
        {
            this.meta = address.substring(0, dots);
            address = address.substring(dots+1);
        }
        this.address = address;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public void setUser(String user)
    {
        assertNotUsed();
        this.user = user;
    }

    public void setPassword(String password)
    {
        assertNotUsed();
        this.password = password;
    }

    public void setHost(String host)
    {
        assertNotUsed();
        this.host = host;
    }

    public void setAddress(String address)
    {
        assertNotUsed();
        this.address = address;
        assertAddressConsistent();
    }

    public void setPort(int port)
    {
        assertNotUsed();
        this.port = new Integer(port);
    }

    public void setProtocol(String protocol)
    {
        assertNotUsed();
        this.protocol = protocol;
        assertAddressConsistent();
    }

    public void setMeta(String meta)
    {
        assertNotUsed();
        this.meta = meta;
    }

    public void setPath(String path)
    {
        assertNotUsed();
        if (null != path)
        {
            if (path.indexOf(DOTS_SLASHES) > -1)
            {
                throw new IllegalArgumentException("Unusual syntax in path: '" + path + "' contains " + DOTS_SLASHES);
            }
            else if (path.contains(BACKSLASH))
            {
                // Windows syntax.  convert it to URI syntax
                try
                {
                    URI pathUri = new File(path).toURI();
                    path = pathUri.getPath();
                }
                catch (Exception ex)
                {
                    throw new IllegalArgumentException("Illegal syntax in path: " + path, ex);
                }
            }
        }
        this.path = path;
    }

    public void setQueryMap(Map queryMap)
    {
        assertNotUsed();
        this.queryMap = queryMap;
    }

    public EndpointURI getEndpoint()
    {
        if (null == cache.get())
        {
            try
            {
                EndpointURI endpointUri = new MuleEndpointURI(getConstructor(), getEncodedConstructor(), muleContext);
                cache.compareAndSet(null, endpointUri);
            }
            catch (EndpointException e)
            {
                throw (IllegalStateException)new IllegalStateException("Bad endpoint configuration").initCause(e);
            }
        }
        return (EndpointURI)cache.get();
    }

    /**
     * @return The String supplied to the delegate constructor
     */
    protected String getConstructor()
    {
        return URLDecoder.decode(getEncodedConstructor());
    }

    protected String getEncodedConstructor()
    {
        StringBuffer buffer = new StringBuffer();
        appendMeta(buffer);
        OrderedQueryParameters uriQueries = appendAddress(buffer);
        uriQueries.override(queryMap);
        buffer.append(uriQueries.toString());
        return buffer.toString();
    }

    private void appendMeta(StringBuffer buffer)
    {
        if (null != meta)
        {
            buffer.append(meta);
            buffer.append(DOTS);
        }
    }

    private OrderedQueryParameters appendAddress(StringBuffer buffer)
    {
        if (null != address)
        {
            int index = address.indexOf(QUERY);
            if (index > -1)
            {
                buffer.append(address.substring(0, index));
                return parseQueries(address.substring(index + 1));
            }
            else
            {
                buffer.append(address);
                return new OrderedQueryParameters();
            }
        }
        else
        {
            constructAddress(buffer);
            return new OrderedQueryParameters();
        }
    }

    private OrderedQueryParameters parseQueries(String queries)
    {
        OrderedQueryParameters map = new OrderedQueryParameters();
        StringTokenizer query = new StringTokenizer(queries, AND);
        while (query.hasMoreTokens())
        {
            StringTokenizer nameValue = new StringTokenizer(query.nextToken(), EQUALS);
            String name = nameValue.nextToken();
            String value = null;
            if (nameValue.hasMoreTokens())
            {
                value = nameValue.nextToken();
            }
            map.put(name, value);
        }
        return map;
    }

    private void constructAddress(StringBuffer buffer)
    {
        buffer.append(protocol);
        buffer.append(DOTS_SLASHES);
        boolean atStart = true;
        if (null != user)
        {
            buffer.append(user);
            if (null != password)
            {
                buffer.append(":");
                buffer.append(password);
            }
            buffer.append("@");
            atStart = false;
        }
        if (null != host)
        {
            buffer.append(host);
            if (null != port)
            {
                buffer.append(":");
                buffer.append(port);
            }
            atStart = false;
        }
        if (null != path)
        {
            if (! atStart)
            {
                buffer.append("/");
            }
            buffer.append(path);
        }
    }

    protected void assertNotUsed()
    {
        if (null != cache.get())
        {
            throw new IllegalStateException("Too late to set values - builder already used");
        }
    }

    protected void assertAddressConsistent()
    {
        if (null != meta)
        {
            if (null != address)
            {
                if (address.startsWith(meta + DOTS))
                {
                    throw new IllegalArgumentException("Meta-protocol '" + meta +
                            "' should not be specified in the address '" + address +
                            "' - it is implicit in the element namespace.");
                }
                if (null != protocol)
                {
                    assertProtocolConsistent();
                }
                else
                {
                    if (address.indexOf(DOTS_SLASHES) == -1)
                    {
                        throw new IllegalArgumentException("Address '" + address +
                                "' does not have a transport protocol prefix " +
                                "(omit the meta protocol prefix, '" + meta + DOTS +
                                "' - it is implicit in the element namespace).");
                    }
                }
            }
        }
        else
        {
            assertProtocolConsistent();
        }
    }

    protected void assertProtocolConsistent()
    {
        if (null != protocol && null != address && !address.startsWith(protocol + DOTS_SLASHES))
        {
            throw new IllegalArgumentException("Address '" + address + "' for protocol '" + protocol +
                    "' should start with " + protocol + DOTS_SLASHES);
        }
    }

    public String toString()
    {
        return getConstructor();
    }

    public boolean equals(Object other)
    {
        if (null == other || !getClass().equals(other.getClass())) return false;
        if (this == other) return true;

        URIBuilder builder = (URIBuilder) other;
        return equal(address, builder.address)
                && equal(meta, builder.meta)
                && equal(protocol, builder.protocol)
                && equal(user, builder.user)
                && equal(password, builder.password)
                && equal(host, builder.host)
                && equal(port, builder.port)
                && equal(path, builder.path)
                && equal(queryMap, builder.queryMap);
    }

    protected static boolean equal(Object a, Object b)
    {
        return ClassUtils.equal(a, b);
    }

    public int hashCode()
    {
        return ClassUtils.hash(new Object[]{address, meta, protocol, user, password, host, port, path, queryMap});
    }

    private static class OrderedQueryParameters
    {
        private List<String> names = new ArrayList<String>();
        private List<String> values = new ArrayList<String>();

        public void put(String name, String value)
        {
            names.add(name);
            values.add(value);
        }

        /**
         * Replace the first instance of the given parameter. This method does not make sense under the assumption that
         * a given parameter name can have multiple values, so here we simply preserve the existing semantics.
         * @param map A map off the name/value pairs to add/replace in the query string 
         */
        public void override(Map map)
        {
            if (null != map)
            {
                // order additional parameters
                Iterator mapNames = new TreeMap(map).keySet().iterator();
                while (mapNames.hasNext())
                {
                    String name = (String) mapNames.next();
                    String value = (String) map.get(name);
                    
                    int pos = names.indexOf(name);
                    if (pos >= 0)
                    {
                        // Found, so replace
                        values.set(pos, value);
                    }
                    else 
                    {       
                        // Append new value
                        names.add(name);
                        values.add(value);
                    }
                 }
            }
        }

        public String toString()
        {
            StringBuffer buffer = new StringBuffer();

            boolean first = true;

            for (int i = 0; i < names.size(); i++)
            {
                if (first)
                {
                    buffer.append(QUERY);
                    first = false;
                }
                else
                {
                    buffer.append(AND);
                }
                
                buffer.append(names.get(i));
                String value = values.get(i);
 
                if (null != value)
                {
                    buffer.append(EQUALS);
                    buffer.append(value);
                }
            }
            return buffer.toString();
        }
    }

}
