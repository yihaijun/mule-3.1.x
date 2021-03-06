/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.mock;

import org.mule.api.MuleMessage;

import com.mockobjects.constraint.Constraint;

/**
 * TODO
 */
public class PayloadConstraint implements Constraint
{
    private Object object;

    public PayloadConstraint(Object object)
    {
        this.object = object;
    }

    public boolean eval(Object o)
    {
        return ((MuleMessage) o).getPayload().equals(object);
    }
}
