/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.scan;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.BloodOrange;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.OrangeInterface;
import org.mule.tck.testmodels.fruit.RedApple;
import org.mule.util.scan.annotations.Marker;
import org.mule.util.scan.annotations.Meta;
import org.mule.util.scan.annotations.NonMeta;
import org.mule.util.scan.annotations.SampleBeanWithAnnotations;
import org.mule.util.scan.annotations.SampleClassWithAnnotations;

import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClasspathScannerTestCase extends AbstractMuleTestCase
{

    //This is slow
    @Test
    public void testSearchInterfaceScanClasspathAndJars() throws Exception
    {
        ClasspathScanner s = new ClasspathScanner("org");
        Set<Class> set = s.scanFor(Fruit.class);

        assertTrue(set.contains(Apple.class));
        assertTrue(set.contains(RedApple.class));
        assertTrue(set.contains(BloodOrange.class));
        assertFalse(set.contains(OrangeInterface.class));
        assertTrue(set.contains(Orange.class));
        assertTrue(set.contains(Grape.class));
        assertTrue(set.contains(SeedlessGrape.class));
    }

    @Test
    public void testSearchInterfaceScanClasspathAndJarsWithInterfaceFlag() throws Exception
        {
            ClasspathScanner s = new ClasspathScanner("org/mule");
            Set<Class> set = s.scanFor(Fruit.class, ClasspathScanner.INCLUDE_INTERFACE);

            assertTrue(set.contains(Apple.class));
            assertTrue(set.contains(RedApple.class));
            assertTrue(set.contains(BloodOrange.class));
            assertTrue(set.contains(OrangeInterface.class));
            assertTrue(set.contains(Orange.class));
            assertTrue(set.contains(Grape.class));
            assertTrue(set.contains(SeedlessGrape.class));
        }

    //This will be a lot more efficient
    @Test
    public void testInterfaceScanClasspathAndJarsMultipleBasePaths() throws Exception
    {
        ClasspathScanner s = new ClasspathScanner("org/mule");
        Set<Class> set = s.scanFor(Fruit.class);

        assertTrue(set.contains(Apple.class));
        assertTrue(set.contains(RedApple.class));
        assertTrue(set.contains(BloodOrange.class));
        assertFalse(set.contains(OrangeInterface.class));
        assertTrue(set.contains(Orange.class));
        assertTrue(set.contains(Grape.class));
        assertTrue(set.contains(SeedlessGrape.class));
        assertTrue(set.contains(MadridOrange.class));
    }

    @Test
    public void testImplementationScanClasspathAndJarsMultipleBasePaths() throws Exception
    {
        ClasspathScanner s = new ClasspathScanner("org/mule");
        Set<Class> set = s.scanFor(Orange.class);

        assertFalse(set.contains(Apple.class));
        assertTrue(set.contains(BloodOrange.class));
        assertFalse(set.contains(OrangeInterface.class));
        assertFalse(set.contains(Orange.class));
        assertFalse(set.contains(Grape.class));
        assertTrue(set.contains(MadridOrange.class));
    }

    @Test
    public void testAnnotationMetaScanClasspathAndJarsMultipleBasePaths() throws Exception
    {
        ClasspathScanner s = new ClasspathScanner("org/mule/util");
        Set<Class> set = s.scanFor(Meta.class);

        assertEquals(3, set.size());
        assertTrue(set.contains(SampleClassWithAnnotations.class));
        assertTrue(set.contains(SampleBeanWithAnnotations.class));
        assertTrue(set.contains(SubscribeBean.class));
    }

    @Test
    public void testAnnotationScanClasspathAndJarsMultipleBasePaths() throws Exception
    {
        ClasspathScanner s = new ClasspathScanner("org/mule");
        Set<Class> set = s.scanFor(Marker.class);

        assertTrue(set.contains(SampleBeanWithAnnotations.class));
        assertTrue(set.contains(SubscribeBean.class));

        set = s.scanFor(NonMeta.class);
        //assertEquals(1, set.size());
        assertTrue(set.contains(SampleBeanWithAnnotations.class));
    }
}
