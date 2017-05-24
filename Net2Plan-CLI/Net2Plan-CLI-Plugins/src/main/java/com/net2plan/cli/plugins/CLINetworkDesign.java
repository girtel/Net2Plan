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

import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.CommandLineParser;
import com.net2plan.internal.plugins.ICLIModule;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;
import org.apache.commons.cli.*;
import org.reflections.Reflections;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Offline network design tool (CLI mode).
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class CLINetworkDesign extends ICLIModule
{
    private final static String TITLE = "Offline network design";
    private final static Options OPTIONS;

    static
    {
        OPTIONS = new Options();

        Option inputFile = new Option(null, "input-file", true, "(Optional) .n2p file containing an initial network design");
        inputFile.setType(PatternOptionBuilder.FILE_VALUE);
        inputFile.setArgName("file");
        OPTIONS.addOption(inputFile);

        Option trafficFile = new Option(null, "traffic-file", true, "(Optional) .n2p file containing traffic demands");
        trafficFile.setType(PatternOptionBuilder.FILE_VALUE);
        trafficFile.setArgName("file");
        OPTIONS.addOption(trafficFile);

        Option trafficLayer = new Option(null, "traffic-layer", true, "(Optional) Identifier of the layer where demands from 'traffic-file' will be loaded");
        trafficLayer.setType(PatternOptionBuilder.NUMBER_VALUE);
        trafficLayer.setArgName("layer");
        OPTIONS.addOption(trafficLayer);

        OptionGroup group = new OptionGroup();
        group.setRequired(true);

        Option packageSearch = new Option(null, "package-name", true, "Search for algorithm under the given package in the application's class-path");
        packageSearch.setType(PatternOptionBuilder.STRING_VALUE);
        packageSearch.setArgName("package");

        Option classFile = new Option(null, "class-file", true, ".class/.jar file containing the algorithm");
        classFile.setType(PatternOptionBuilder.FILE_VALUE);
        classFile.setArgName("file");

        group.addOption(packageSearch);
        group.addOption(classFile);
        OPTIONS.addOptionGroup(group);

        Option className = new Option(null, "class-name", true, "Class name of the algorithm (package name could be omitted)");
        className.setType(PatternOptionBuilder.STRING_VALUE);
        className.setArgName("classname");
        className.setRequired(true);
        OPTIONS.addOption(className);

        Option outputFile = new Option(null, "output-file", true, ".n2p file where saving the resulting design");
        outputFile.setType(PatternOptionBuilder.FILE_VALUE);
        outputFile.setArgName("file");
        outputFile.setRequired(true);
        OPTIONS.addOption(outputFile);

        Option algorithmParameters = new Option(null, "alg-param", true, "(Optional) algorithm parameters (use one of this for each parameter)");
        algorithmParameters.setArgName("property=value");
        algorithmParameters.setArgs(2);
        algorithmParameters.setValueSeparator('=');
        OPTIONS.addOption(algorithmParameters);
    }

    @Override
    public void executeFromCommandLine(String[] args) throws ParseException
    {
        final CommandLineParser parser = new CommandLineParser();
        final CommandLine cli = parser.parse(OPTIONS, args);

        final File classFile = (File) cli.getParsedOptionValue("class-file");
        final String className = (String) cli.getParsedOptionValue("class-name");

        NetPlan netPlan;
        if (cli.hasOption("input-file"))
        {
            final File inputFile = (File) cli.getParsedOptionValue("input-file");
            netPlan = new NetPlan(inputFile);
        } else
        {
            netPlan = new NetPlan();
        }

        if (cli.hasOption("traffic-file"))
        {
            NetPlan demands = new NetPlan((File) cli.getParsedOptionValue("traffic-file"));

            NetworkLayer layer = (cli.hasOption("traffic-layer")) ? netPlan.getNetworkLayerFromId(((Number) cli.getParsedOptionValue("traffic-layer")).longValue()) : netPlan.getNetworkLayerDefault();

            netPlan.removeAllDemands(layer);
            for (Demand demand : demands.getDemands())
            {
                netPlan.addDemand(demand.getIngressNode(), demand.getEgressNode(), demand.getOfferedTraffic(), demand.getAttributes(), layer);
            }
            netPlan.removeAllMulticastDemands(layer);
            for (MulticastDemand demand : demands.getMulticastDemands())
            {
                netPlan.addMulticastDemand(demand.getIngressNode(), demand.getEgressNodes(), demand.getOfferedTraffic(), demand.getAttributes(), layer);
            }
        }

        File outputFile = (File) cli.getParsedOptionValue("output-file");

        IAlgorithm algorithm;
        if (!cli.hasOption("package-name"))
        {
            algorithm = ClassLoaderUtils.getInstance(classFile, className, IAlgorithm.class , null);
        } else
        {
            algorithm = findAlgorithm(className, cli.getOptionValue("package-name"));
        }

        List<Triple<String, String, String>> defaultAlgorithmParameters = algorithm.getParameters();
        Map<String, String> algorithmParameters = CommandLineParser.getParameters(defaultAlgorithmParameters, cli.getOptionProperties("alg-param"));
        Configuration.updateSolverLibraryNameParameter(algorithmParameters); // put default path to libraries if solverLibraryName is ""
        Map<String, String> net2planParameters = Configuration.getNet2PlanOptions();

        System.out.println("Net2Plan parameters");
        System.out.println("-----------------------------");
        System.out.println(StringUtils.mapToString(net2planParameters, "=", String.format("%n")));
        System.out.println();
        System.out.println("Algorithm parameters");
        System.out.println("-----------------------------");
        System.out.println(algorithmParameters.isEmpty() ? "None" : StringUtils.mapToString(algorithmParameters, "=", String.format("%n")));
        System.out.println();
        System.out.println("Executing algorithm...");
        System.out.println();

        long init = System.nanoTime();
        String out = algorithm.executeAlgorithm(netPlan, algorithmParameters, net2planParameters);
        netPlan.saveToFile(outputFile);
        long end = System.nanoTime();

        System.out.println(String.format("%n%nAlgorithm finished successfully in %f seconds%nOutput message: %s", (end - init) / 1e9, out));
    }

    @Override
    public String getCommandLineHelp()
    {
        return "Targeted to evaluate the network designs "
                + "generated by built-in or user-defined static planning "
                + "algorithms, deciding on aspects such as the network "
                + "topology, the traffic routing, link capacities, protection "
                + "routes and so on. Algorithms based on constrained optimization "
                + "formulations (i.e. ILPs) can be fast-prototyped using the "
                + "open-source Java Optimization Modeler library, to interface "
                + "to a number of external solvers such as GPLK, CPLEX or IPOPT";
    }

    @Override
    public Options getCommandLineOptions()
    {
        return OPTIONS;
    }

    @Override
    public String getDescription()
    {
        return getName();
    }

    @Override
    public String getModeName()
    {
        return "net-design";
    }

    @Override
    public String getName()
    {
        return TITLE + " (CLI)";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        return null;
    }

    @Override
    public int getPriority()
    {
        return Integer.MAX_VALUE;
    }

    private static IAlgorithm findAlgorithm(final String algorithmName, final String packageName)
    {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends IAlgorithm>> algorithms = reflections.getSubTypesOf(IAlgorithm.class);

        IAlgorithm algorithm = null;
        try
        {
            for (Class<?> algorithmClass : algorithms)
            {
                if (algorithmClass.getSimpleName().equals(algorithmName))
                {
                    final Class<?> classDefinition = Class.forName(algorithmClass.getName());
                    final Constructor<?> constructor = classDefinition.getConstructor();
                    final Object object = constructor.newInstance();

                    if (object instanceof IAlgorithm)
                    {
                        algorithm = (IAlgorithm) object;
                        break;
                    }
                }
            }

            if (algorithm == null) throw new Exception("Algorithm not found: " + algorithmName + " at " + packageName);

            return algorithm;
        } catch (Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }
}
