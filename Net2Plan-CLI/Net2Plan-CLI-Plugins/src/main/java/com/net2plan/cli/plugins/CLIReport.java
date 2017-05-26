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

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IReport;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.CommandLineParser;
import com.net2plan.internal.plugins.ICLIModule;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.HTMLUtils;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Reporting tool (CLI mode).
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class CLIReport extends ICLIModule {
    private final static String TITLE = "Reporting tool";
    private final static Options OPTIONS;

    static {
        OPTIONS = new Options();

        Option inputFile = new Option(null, "input-file", true, "Input .n2p file");
        inputFile.setType(PatternOptionBuilder.FILE_VALUE);
        inputFile.setArgName("file");
        inputFile.setRequired(true);
        OPTIONS.addOption(inputFile);

        Option classFile = new Option(null, "class-file", true, ".class/.jar file containing the report");
        classFile.setType(PatternOptionBuilder.FILE_VALUE);
        classFile.setArgName("file");
        classFile.setRequired(true);
        OPTIONS.addOption(classFile);

        Option className = new Option(null, "class-name", true, "Class name of the report (package name could be omitted)");
        className.setType(PatternOptionBuilder.STRING_VALUE);
        className.setArgName("classname");
        className.setRequired(true);
        OPTIONS.addOption(className);

        Option outputFile = new Option(null, "output-file", true, "Output .html file (extra .png files could be saved)");
        outputFile.setType(PatternOptionBuilder.FILE_VALUE);
        outputFile.setArgName("file");
        outputFile.setRequired(true);
        OPTIONS.addOption(outputFile);

        Option reportParameters = new Option(null, "report-param", true, "(Optional) report parameters (use one of this for each parameter)");
        reportParameters.setArgName("property=value");
        reportParameters.setArgs(2);
        reportParameters.setValueSeparator('=');
        OPTIONS.addOption(reportParameters);
    }

    @Override
    public void executeFromCommandLine(String[] args) throws ParseException
    {
        CommandLineParser parser = new CommandLineParser();
        CommandLine cli = parser.parse(OPTIONS, args);

        File classFile = (File) cli.getParsedOptionValue("class-file");
        String className = (String) cli.getParsedOptionValue("class-name");

        File inputFile = (File) cli.getParsedOptionValue("input-file");
        NetPlan netPlan = new NetPlan(inputFile);

        File outputFile = (File) cli.getParsedOptionValue("output-file");

        IReport report = ClassLoaderUtils.getInstance(classFile, className, IReport.class , null);

        List<Triple<String, String, String>> defaultReportParameters = null;
        try {
            defaultReportParameters = report.getParameters();
        } catch (UnsupportedOperationException ex) {
        }

        Map<String, String> reportParameters = CommandLineParser.getParameters(defaultReportParameters, cli.getOptionProperties("report-param"));
        Map<String, String> net2planParameters = Configuration.getNet2PlanOptions();

        System.out.println("Net2Plan parameters");
        System.out.println("-----------------------------");
        System.out.println(StringUtils.mapToString(net2planParameters, "=", String.format("%n")));
        System.out.println();
        System.out.println("Report parameters");
        System.out.println("-----------------------------");
        System.out.println(reportParameters.isEmpty() ? "None" : StringUtils.mapToString(reportParameters, "=", String.format("%n")));
        System.out.println();
        System.out.println("Executing report...");
        System.out.println();

        long init = System.nanoTime();
        String html = report.executeReport(netPlan, reportParameters, net2planParameters);
        long end = System.nanoTime();

        HTMLUtils.saveToFile(outputFile, html);

        System.out.println(String.format("%n%nReport finished successfully in %f seconds", (end - init) / 1e9));
    }

    @Override
    public String getCommandLineHelp() {
        return "Permits the generation of built-in or "
                + "user-defined reports, from any network design";
    }

    @Override
    public Options getCommandLineOptions() {
        return OPTIONS;
    }

    @Override
    public String getDescription() {
        return getName();
    }

    @Override
    public String getModeName() {
        return "report";
    }

    @Override
    public String getName() {
        return TITLE + " (CLI)";
    }

    @Override
    public List<Triple<String, String, String>> getParameters() {
        return null;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE - 3;
    }
}
