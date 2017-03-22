package com.net2plan.cli.plugins.launcher;

import com.net2plan.cli.plugins.CLINetworkDesign;
import org.apache.commons.cli.ParseException;

/**
 * Launcher using the CLI version of Net2Plan that helps the debugging of algorithms.
 * This main class receives the same input parameters as the CLINetworkDesign tool.
 * Algorithm packages need to be module dependencies in order for the program to find them.
 */
public class CLINetworkDesignLauncher
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
