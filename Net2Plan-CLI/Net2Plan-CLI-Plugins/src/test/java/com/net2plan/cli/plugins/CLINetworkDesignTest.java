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
package com.net2plan.cli.plugins;

import com.net2plan.utils.StringUtils;
import org.apache.commons.cli.ParseException;
import org.junit.Test;

/**
 * Created by Jorge San Emeterio on 22/03/17.
 */
public class CLINetworkDesignTest
{
    private final static CLINetworkDesign networkDesign = new CLINetworkDesign();

    @Test(expected = ParseException.class)
    public void launchNoOptionParam() throws ParseException
    {
        String[] args = StringUtils.arrayOf("--class-name", " ", "--output-file", "");
        networkDesign.executeFromCommandLine(args);
    }

    @Test(expected = ParseException.class)
    public void launchNoClassNameParam() throws ParseException
    {
        String[] args = StringUtils.arrayOf("--package-name", " ", "--output-file", "");
        networkDesign.executeFromCommandLine(args);
    }

    @Test(expected = ParseException.class)
    public void launchNoOutputFileParam() throws ParseException
    {
        String[] args = StringUtils.arrayOf("--package-name", " ", "--class-name", "");
        networkDesign.executeFromCommandLine(args);
    }

    @Test(expected = RuntimeException.class)
    public void launchBothOptionParam() throws ParseException
    {
        String[] args = StringUtils.arrayOf("--class-file", " ", "--package-name", " ", "--class-name", " ", "--output-file", "");
        networkDesign.executeFromCommandLine(args);
    }
}
