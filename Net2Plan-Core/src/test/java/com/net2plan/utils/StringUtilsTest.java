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
