/*
 * ******************************************************************************
 *  * Copyright (c) 2017 Pablo Pavon-Marino.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the GNU Lesser Public License v3.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.gnu.org/licenses/lgpl.html
 *  *
 *  * Contributors:
 *  *     Pablo Pavon-Marino - Jose-Luis Izquierdo-Zaragoza, up to version 0.3.1
 *  *     Pablo Pavon-Marino - from version 0.4.0 onwards
 *  *     Pablo Pavon Marino - Jorge San Emeterio Villalain, from version 0.4.1 onwards
 *  *****************************************************************************
 */

package com.net2plan.launcher;

import org.apache.commons.cli.*;
import org.apache.commons.lang.ArrayUtils;

/**
 * Created by Jorge San Emeterio on 22/03/17.
 */
public class Net2PlanLauncher
{
    private static final Options OPTIONS;
    private static final OptionGroup GROUP_DIRECTION;

    static
    {
        OPTIONS = new Options();

        GROUP_DIRECTION = new OptionGroup();
        GROUP_DIRECTION.setRequired(true);

        final Option guiOption = new Option(null, "GUI", false, "Launch Net2Plan in GUI mode");
        GROUP_DIRECTION.addOption(guiOption);
        final Option cliOption = new Option(null, "CLI", false, "Launch Net2Plan in CLI mode");
        GROUP_DIRECTION.addOption(cliOption);

        OPTIONS.addOptionGroup(GROUP_DIRECTION);
    }

    public static void main(String[] args)
    {
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try
        {
            parser.parse(OPTIONS, args, true);

            if (GROUP_DIRECTION.getSelected().equals("CLI"))
            {
                CLILauncher.main((String[]) ArrayUtils.removeElement(args, "--CLI"));
            } else if (GROUP_DIRECTION.getSelected().equals("GUI"))
            {
                GUILauncher.main((String[]) ArrayUtils.removeElement(args, "--GUI"));
            } else
            {
                throw new ParseException("Unknown entry parameter...");
            }
        } catch (Exception e)
        {
            System.err.println(e.getMessage());
            formatter.printHelp("N2P-Launcher", OPTIONS);
        }
    }
}
