package com.net2plan.cli.plugins.launcher;

import com.net2plan.cli.plugins.CLINetworkDesign;

/**
 * Created by Jorge San Emeterio on 20/03/17.
 */
public class CLIAlgorithmLauncher
{
    public static void main(String[] args)
    {
        try
        {
            CLINetworkDesign networkDesign = new CLINetworkDesign();
            networkDesign.executeFromCommandLine(args);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
