package com.net2plan.cli.plugins.launcher;

import com.net2plan.cli.plugins.CLINetworkDesign;
import org.apache.commons.cli.ParseException;

/**
 * Created by Jorge San Emeterio on 20/03/17.
 */
public class CLIAlgorithmLauncher extends CLINetworkDesign
{
    public static void main(String[] args)
    {
        try
        {
            CLIAlgorithmLauncher algorithmLauncher = new CLIAlgorithmLauncher();
            algorithmLauncher.executeFromCommandLine(args);
        } catch (ParseException e)
        {
            e.printStackTrace();
        }
    }
}
