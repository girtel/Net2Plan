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

import static com.net2plan.internal.sim.SimKernel.runSimulation;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.simulation.IEventGenerator;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.internal.CommandLineParser;
import com.net2plan.internal.IExternal;
import com.net2plan.internal.SystemUtils;
import com.net2plan.internal.plugins.ICLIModule;
import com.net2plan.internal.sim.SimKernel;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.HTMLUtils;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

/**
 * Online simulation tool (CLI mode).
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class CLIOnlineSimulation extends ICLIModule {
    private final static String TITLE = "Online simulation";
    private final static Options OPTIONS;

    static {
        String generatorLabel = SimKernel.getEventGeneratorLabel();
        String processorLabel = SimKernel.getEventProcessorLabel();

        OPTIONS = new Options();

        Option inputFile = new Option(null, "input-file", true, ".n2p file containing an initial network design");
        inputFile.setType(PatternOptionBuilder.FILE_VALUE);
        inputFile.setArgName("file");
        OPTIONS.addOption(inputFile);

        Option outputFile = new Option(null, "output-file", true, "Output HTML file with the simulation report");
        outputFile.setType(PatternOptionBuilder.FILE_VALUE);
        outputFile.setArgName("file");
        outputFile.setRequired(true);
        OPTIONS.addOption(outputFile);

        Option simParameters = new Option(null, "sim-param", true, "Simulation parameters (use one of this for each parameter)");
        simParameters.setArgName("property=value");
        simParameters.setArgs(2);
        simParameters.setValueSeparator('=');
        OPTIONS.addOption(simParameters);

        Option generatorParameters = new Option(null, "generator-param", true, "(Optional) " + generatorLabel.toLowerCase(Locale.getDefault()) + " parameters (use one of this for each parameter)");
        generatorParameters.setArgName("property=value");
        generatorParameters.setArgs(2);
        generatorParameters.setValueSeparator('=');
        OPTIONS.addOption(generatorParameters);

        Option processorParameters = new Option(null, "processor-param", true, "(Optional) " + processorLabel.toLowerCase(Locale.getDefault()) + " parameters (use one of this for each parameter)");
        processorParameters.setArgName("property=value");
        processorParameters.setArgs(2);
        processorParameters.setValueSeparator('=');
        OPTIONS.addOption(processorParameters);

        Option eventGeneratorClassFile = new Option(null, "generator-class-file", true, ".class/.jar file containing the event generator");
        eventGeneratorClassFile.setType(PatternOptionBuilder.FILE_VALUE);
        eventGeneratorClassFile.setArgName("file");
        eventGeneratorClassFile.setRequired(true);
        OPTIONS.addOption(eventGeneratorClassFile);

        Option eventGeneratorClassName = new Option(null, "generator-class-name", true, "Class name of the event generator (package name could be omitted)");
        eventGeneratorClassName.setType(PatternOptionBuilder.STRING_VALUE);
        eventGeneratorClassName.setArgName("classname");
        eventGeneratorClassName.setRequired(true);
        OPTIONS.addOption(eventGeneratorClassName);

        Option eventProcessorClassFile = new Option(null, "processor-class-file", true, ".class/.jar file containing the event processor");
        eventProcessorClassFile.setType(PatternOptionBuilder.FILE_VALUE);
        eventProcessorClassFile.setArgName("file");
        eventProcessorClassFile.setRequired(true);
        OPTIONS.addOption(eventProcessorClassFile);

        Option eventProcessorClassName = new Option(null, "processor-class-name", true, "Class name of the event processor (package name could be omitted)");
        eventProcessorClassName.setType(PatternOptionBuilder.STRING_VALUE);
        eventProcessorClassName.setArgName("classname");
        eventProcessorClassName.setRequired(true);
        OPTIONS.addOption(eventProcessorClassName);
    }

    @Override
    public final void executeFromCommandLine(String[] args) throws ParseException
    {
        CommandLineParser parser = new CommandLineParser();
        CommandLine cli = parser.parse(OPTIONS, args);
        Map<String, String> net2planParameters = Configuration.getNet2PlanOptions();

		/* Load event generator and event processor objects */
        File generatorClassFile = (File) cli.getParsedOptionValue("generator-class-file");
        String generatorClassName = (String) cli.getParsedOptionValue("generator-class-name");
        File provisioningClassFile = (File) cli.getParsedOptionValue("processor-class-file");
        String provisioningClassName = (String) cli.getParsedOptionValue("processor-class-name");


        File classFileForClassLoader_generator;
        File classFileForClassLoader_processor;
        switch (SystemUtils.getExtension(generatorClassFile).toLowerCase(Locale.getDefault()))
        {
        case "jar": classFileForClassLoader_generator = generatorClassFile; break;
        case "class": classFileForClassLoader_generator = ClassLoaderUtils.getClasspathAndQualifiedNameFromClassFile(generatorClassFile).getFirst(); break;
        default: throw new Net2PlanException ("'file' is not a valid Java file (.jar or .class)");
        }
        switch (SystemUtils.getExtension(provisioningClassFile).toLowerCase(Locale.getDefault()))
        {
        case "jar": classFileForClassLoader_processor = provisioningClassFile; break;
        case "class": classFileForClassLoader_processor = ClassLoaderUtils.getClasspathAndQualifiedNameFromClassFile(provisioningClassFile).getFirst(); break;
        default: throw new Net2PlanException ("'file' is not a valid Java file (.jar or .class)");
        }
        
        URLClassLoader ucl = null;
        try 
        {
        	ucl = new URLClassLoader(new URL[] { classFileForClassLoader_generator.toURI().toURL() , classFileForClassLoader_processor.toURI().toURL() }, ClassLoader.getSystemClassLoader());
        } catch (Exception e) { throw new Net2PlanException ("Unable to create the URL for class loading. Wrong file name.");  }
        IExternal aux_eventGenerator = ClassLoaderUtils.getInstance(generatorClassFile, generatorClassName, IEventGenerator.class , ucl);
        IExternal aux_eventProcessor = ClassLoaderUtils.getInstance(provisioningClassFile, provisioningClassName, IEventProcessor.class , ucl);

		/* Read simulation, event generator and event processor parameters */
        Properties customSimulationParameters = cli.getOptionProperties("sim-param");
        Properties customEventGeneratorParameters = cli.getOptionProperties("generator-param");
        Properties customEventProcessorParameters = cli.getOptionProperties("processor-param");

		/* Read the input netPlan file */
        File inputFile = (File) cli.getParsedOptionValue("input-file");
        File outputFile = (File) cli.getParsedOptionValue("output-file");

		/* Initialize and run simulation */
        NetPlan aux_netPlan = new NetPlan(inputFile);
        String html = runSimulation(new SimKernel(), aux_netPlan, aux_eventGenerator, customEventGeneratorParameters, aux_eventProcessor, customEventProcessorParameters, customSimulationParameters, net2planParameters).getSecond();
        HTMLUtils.saveToFile(outputFile, html);
    }

    @Override
    public final String getCommandLineHelp() {
        String NEW_LINE = StringUtils.getLineSeparator();

        StringBuilder out = new StringBuilder();
        out.append("Simulates the network operation, where events appeared according to "
                + "a built-in or user-defined event generator. Targeted to evaluate the performance of "
                + "built-in or user-defined reaction algorithms on top of a discrete-event simulator");
        out.append(NEW_LINE);
        out.append("Simulation parameters:");
        out.append(NEW_LINE);

        List<Triple<String, String, String>> simParam = new SimKernel().getSimulationParameters();
        for (Triple<String, String, String> param : simParam)
            out.append(String.format("- %s: %s. Default: %s", param.getFirst(), param.getThird(), param.getSecond())).append(NEW_LINE);

        return out.toString();
    }

    @Override
    public final Options getCommandLineOptions() {
        return OPTIONS;
    }

    @Override
    public String getDescription() {
        return getName();
    }

    @Override
    public String getModeName() {
        return "online-sim";
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
        return Integer.MAX_VALUE - 2;
    }
}
