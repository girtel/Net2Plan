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


package com.net2plan.cli;

import com.jom.JOMException;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.internal.CommandLineParser;
import com.net2plan.internal.Constants.UserInterface;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.SystemUtils;
import com.net2plan.internal.Version;
import com.net2plan.internal.plugins.ICLIModule;
import com.net2plan.internal.plugins.Plugin;
import com.net2plan.internal.plugins.PluginSystem;
import com.net2plan.utils.StringUtils;
import org.apache.commons.cli.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main class for the command-line user interface (CLI).
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
@SuppressWarnings("unchecked")
public class CLINet2Plan {
    private final static int LINE_WIDTH = 80;
    private final Map<String, Class<? extends ICLIModule>> modes = new LinkedHashMap<String, Class<? extends ICLIModule>>();
    private final Options options = new Options();

    /**
     * Default constructor.
     *
     * @param args Command-line arguments
     */
    public CLINet2Plan(String args[]) {
        try {
            SystemUtils.configureEnvironment(CLINet2Plan.class, UserInterface.CLI);

            for (Class<? extends Plugin> plugin : PluginSystem.getPlugins(ICLIModule.class)) {
                try {
                    ICLIModule instance = ((Class<? extends ICLIModule>) plugin).newInstance();
                    modes.put(instance.getModeName(), instance.getClass());
                } catch (NoClassDefFoundError e) {
                    e.printStackTrace();
                    throw new Net2PlanException("Class " + e.getMessage() + " cannot be found. A dependence for " + plugin.getSimpleName() + " is missing?");
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            Option helpOption = new Option("help", true, "Show the complete help information. 'modeName' is optional");
            helpOption.setArgName("modeName");
            helpOption.setOptionalArg(true);

            Option modeOption = new Option("mode", true, "Mode: " + StringUtils.join(StringUtils.toArray(modes.keySet()), ", "));
            modeOption.setArgName("modeName");
            modeOption.setOptionalArg(true);

            OptionGroup group = new OptionGroup();
            group.addOption(modeOption);
            group.addOption(helpOption);

            options.addOptionGroup(group);

            CommandLineParser parser = new CommandLineParser();
            CommandLine cli = parser.parse(options, args);

            if (cli.hasOption("help")) {
                String mode = cli.getOptionValue("help");
                System.out.println(mode == null ? getCompleteHelp() : getModeHelp(mode));
            } else if (!cli.hasOption("mode")) {
                System.out.println(getMainHelp());
            } else {
                String mode = cli.getOptionValue("mode");

                if (modes.containsKey(mode)) {
                    ICLIModule modeInstance = modes.get(mode).newInstance();

                    try {
                        modeInstance.executeFromCommandLine(args);
                    } catch (Net2PlanException | JOMException ex) {
                        if (ErrorHandling.isDebugEnabled()) ErrorHandling.printStackTrace(ex);

                        System.out.println("Execution stopped");
                        System.out.println();
                        System.out.println(ex.getMessage());
                    } catch (ParseException ex) {
                        System.out.println("Bad syntax: " + ex.getMessage());
                        System.out.println();
                        System.out.println(getModeHelp(mode));
                    } catch (Throwable ex) {
                        Throwable ex1 = ErrorHandling.getInternalThrowable(ex);
                        if (ex1 instanceof Net2PlanException || ex1 instanceof JOMException) {
                            if (ErrorHandling.isDebugEnabled()) ErrorHandling.printStackTrace(ex);

                            System.out.println("Execution stopped");
                            System.out.println();
                            System.out.println(ex1.getMessage());
                        } else if (ex1 instanceof ParseException) {
                            System.out.println("Bad syntax: " + ex1.getMessage());
                            System.out.println();
                            System.out.println(getModeHelp(mode));
                        } else {
                            System.out.println("Execution stopped. An unexpected error happened");
                            System.out.println();
                            ErrorHandling.printStackTrace(ex1);
                        }
                    }
                } else {
                    throw new IllegalModeException("Bad mode - " + mode);
                }
            }
        } catch (IllegalModeException e) {
            System.out.println(e.getMessage());
            System.out.println();
            System.out.println(getMainHelp());
        } catch (ParseException e) {
            System.out.println("Bad syntax: " + e.getMessage());
            System.out.println();
            System.out.println(getMainHelp());
        } catch (Net2PlanException e) {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.printStackTrace(e);
            System.out.println(e.getMessage());
        } catch (Throwable e) {
            ErrorHandling.printStackTrace(e);
        }
    }

    private String getModeHelp(String mode) {
        if (!modes.containsKey(mode)) throw new IllegalModeException("Bad mode - " + mode);

        try {
            ICLIModule modeInstance = modes.get(mode).newInstance();
            Options modeOptions = modeInstance.getCommandLineOptions();
            String modeHelp = modeInstance.getCommandLineHelp();

            StringWriter sw = new StringWriter();
            try (PrintWriter w = new PrintWriter(sw)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printWrapped(w, LINE_WIDTH, "Mode: " + mode);
                formatter.printWrapped(w, LINE_WIDTH, "");
                formatter.printWrapped(w, LINE_WIDTH, modeHelp);
                formatter.printWrapped(w, LINE_WIDTH, "");
                formatter.printHelp(w, LINE_WIDTH, "java -jar Net2Plan-cli.jar --mode " + mode, null, modeOptions, 0, 1, null, true);

                w.flush();
            }

            return (sw.toString());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private String getMainHelp() {
        final StringBuilder help = new StringBuilder();

        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter w = new PrintWriter(sw);
        formatter.printWrapped(w, LINE_WIDTH, "Net2Plan " + new Version().toString() + " Command-Line Interface");
        formatter.printWrapped(w, LINE_WIDTH, "");
        if (modes.isEmpty()) {
            formatter.printWrapped(w, LINE_WIDTH, "No CLI tool is available");
        } else {
            formatter.printHelp(w, LINE_WIDTH, "java -jar Net2Plan-cli.jar", null, options, 0, 1, null, true);
            formatter.printWrapped(w, LINE_WIDTH, "");
            formatter.printWrapped(w, LINE_WIDTH, "Select 'help' to show this information, or 'mode' to execute a specific tool. Optionally, if 'help' is accompanied of a mode name, the help information for this mode is shown");
        }
        w.flush();
        help.append(sw.toString());

        return help.toString();
    }

    private String getCompleteHelp() {
        final StringBuilder help = new StringBuilder();
        final String lineSeparator = StringUtils.getLineSeparator();

        help.append(getMainHelp());

        for (String mode : modes.keySet()) {
            help.append(lineSeparator);
            help.append(lineSeparator);
            help.append(getModeHelp(mode));
        }

        return help.toString();
    }

    /**
     * Entry point for the command-line interface.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        new CLINet2Plan(args);
    }

    private static class IllegalModeException extends RuntimeException {
        public IllegalModeException(String message) {
            super(message);
        }
    }
}
