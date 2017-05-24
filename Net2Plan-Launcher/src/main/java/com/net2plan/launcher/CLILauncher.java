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

package com.net2plan.launcher;

import com.net2plan.cli.plugins.CLINetworkDesign;
import org.apache.commons.cli.ParseException;

/**
 * @author Jorge San Emeterio Villalain
 * @date 24/03/17
 */
public class CLILauncher
{
    public static void main(String[] args)
    {
        try
        {
            CLINetworkDesign networkDesign = new CLINetworkDesign();
            networkDesign.executeFromCommandLine(args);
        } catch (ParseException e)
        {
            e.printStackTrace();
        }
    }
}
