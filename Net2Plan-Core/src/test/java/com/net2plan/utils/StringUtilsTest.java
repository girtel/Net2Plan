/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/
package com.net2plan.utils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jorge San Emeterio on 17/03/17.
 */
public class StringUtilsTest
{
    private static List<String> testList;

    @BeforeClass
    public static void prepareTest()
    {
        testList = new ArrayList<>();
        testList.add("test1");
        testList.add("test2");
        testList.add("test3");
    }

    @Test
    public void testListToStringDefaultSeparator()
    {
        final String string = StringUtils.listToString(testList);

        Assert.assertEquals("test1, test2, test3", string);
    }

    @Test
    public void testListToStringSpecificSeparator()
    {
        final String string = StringUtils.listToString(testList, "; ");

        Assert.assertEquals("test1; test2; test3", string);
    }
}
