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

import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.CommandLineParser;
import com.net2plan.internal.plugins.ICLIModule;
import com.net2plan.libraries.TrafficMatrixGenerationModels;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.*;

/**
 * Traffic matrix design tool (CLI mode).
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class CLITrafficDesign extends ICLIModule {
    private final static String TITLE = "Traffic matrix design";
    private final static Options OPTIONS;
    private final static Map<String, String> INCREMENTAL_PATTERNS, NORMALIZATION_PATTERNS, TRAFFIC_PATTERNS;

    static {
        INCREMENTAL_PATTERNS = new LinkedHashMap<String, String>();
        INCREMENTAL_PATTERNS.put("cagr", "New matrices with a compound annual growth rate");
        INCREMENTAL_PATTERNS.put("randomUniform", "Uniform random variations");
        INCREMENTAL_PATTERNS.put("randomGaussian", "Gaussian random variations");

        NORMALIZATION_PATTERNS = new LinkedHashMap<String, String>();
        NORMALIZATION_PATTERNS.put("total-normalization", "Total normalization");
        NORMALIZATION_PATTERNS.put("row-normalization", "Row normalization");
        NORMALIZATION_PATTERNS.put("column-normalization", "Column normalization");
        NORMALIZATION_PATTERNS.put("max-traffic-estimated-minHop", "Maximum possible traffic (upper-bound considering only min-hop shortest-path routing)");
        NORMALIZATION_PATTERNS.put("max-traffic-estimated-km", "Maximum possible traffic (upper-bound considering only shortest-path-in-km routing)");
        NORMALIZATION_PATTERNS.put("max-traffic-exact", "Maximum possible traffic (unconstrained routing using JOM)");

        TRAFFIC_PATTERNS = new LinkedHashMap<String, String>();
        TRAFFIC_PATTERNS.put("constant", "Constant");
        TRAFFIC_PATTERNS.put("gravity-model", "Gravity model");
        TRAFFIC_PATTERNS.put("uniform-random-10", "Uniform (0, 10)");
        TRAFFIC_PATTERNS.put("uniform-random-100", "Uniform (0, 100)");
        TRAFFIC_PATTERNS.put("uniform-random-bimodal-50-50", "50% Uniform (0, 100) & 50% Uniform(0, 10)");
        TRAFFIC_PATTERNS.put("uniform-random-bimodal-25-75", "25% Uniform (0, 100) & 75% Uniform(0, 10)");
        TRAFFIC_PATTERNS.put("population-distance-model", "Population-distance model");

        OPTIONS = new Options();

        Option inputFile = new Option(null, "input-file", true, "Input .n2p file including a topology/demand set");
        inputFile.setArgName("file");
        inputFile.setType(PatternOptionBuilder.FILE_VALUE);

        Option numNodes = new Option(null, "num-nodes", true, "Number of nodes");
        numNodes.setType(PatternOptionBuilder.NUMBER_VALUE);
        numNodes.setArgName("nodes");

        OptionGroup inputData = new OptionGroup();
        inputData.addOption(inputFile);
        inputData.addOption(numNodes);
        inputData.setRequired(true);
        OPTIONS.addOptionGroup(inputData);

        Option outputFile = new Option(null, "output-file", true, "Output .n2p file (multiple traffic matrix designs will be saved as a sequence of '-tmX.n2p', where X is the matrix index)");
        outputFile.setType(PatternOptionBuilder.FILE_VALUE);
        outputFile.setArgName("file");
        outputFile.setRequired(true);
        OPTIONS.addOption(outputFile);

        Option randomFactor = new Option(null, "random-factor", true, "Random factor (only for population-distance-model, default 0)");
        randomFactor.setType(PatternOptionBuilder.NUMBER_VALUE);
        randomFactor.setArgName("value");

        Option populationOffset = new Option(null, "population-offset", true, "Population offset (only for population-distance-model, default 0)");
        populationOffset.setType(PatternOptionBuilder.NUMBER_VALUE);
        populationOffset.setArgName("value");

        Option populationPower = new Option(null, "population-power", true, "Population power (only for population-distance-model, default 1)");
        populationPower.setType(PatternOptionBuilder.NUMBER_VALUE);
        populationPower.setArgName("value");

        Option distanceOffset = new Option(null, "distance-offset", true, "Distance offset (only for population-distance-model, default 0)");
        distanceOffset.setType(PatternOptionBuilder.NUMBER_VALUE);
        distanceOffset.setArgName("value");

        Option distancePower = new Option(null, "distance-power", true, "Distance power (only for population-distance-model, default 1)");
        distancePower.setType(PatternOptionBuilder.NUMBER_VALUE);
        distancePower.setArgName("value");

        Option normalizePopulation = new Option(null, "normalize-population", true, "Normalize to maximum population (only for population-distance-model, default true)");
        normalizePopulation.setType(PatternOptionBuilder.STRING_VALUE);
        normalizePopulation.setArgName("value");

        Option normalizeDistance = new Option(null, "normalize-distance", true, "Normalize to maximum distance (only for population-distance-model, default true)");
        normalizeDistance.setType(PatternOptionBuilder.STRING_VALUE);
        normalizeDistance.setArgName("value");

        OPTIONS.addOption(randomFactor);
        OPTIONS.addOption(populationOffset);
        OPTIONS.addOption(populationPower);
        OPTIONS.addOption(distanceOffset);
        OPTIONS.addOption(distancePower);
        OPTIONS.addOption(normalizePopulation);
        OPTIONS.addOption(normalizeDistance);

        Option numMatrices = new Option(null, "num-matrices", true, "Number of generated matrices (if no traffic pattern is specified it will be ignored, default 1)");
        numMatrices.setType(PatternOptionBuilder.NUMBER_VALUE);
        numMatrices.setArgName("matrices");
        OPTIONS.addOption(numMatrices);

        Option trafficPattern = new Option(null, "traffic-pattern", true, "Traffic pattern: " + StringUtils.join(StringUtils.toArray(TRAFFIC_PATTERNS.keySet()), ", "));
        trafficPattern.setType(PatternOptionBuilder.STRING_VALUE);
        trafficPattern.setArgName("patternName");
        OPTIONS.addOption(trafficPattern);

        Option levelMatrix = new Option(null, "level-matrix-file", true, "Input file with a 2D level matrix (only for population-distance-model)");
        levelMatrix.setType(PatternOptionBuilder.FILE_VALUE);
        levelMatrix.setArgName("file");
        OPTIONS.addOption(levelMatrix);

        Option normalizationPattern = new Option(null, "normalization-pattern", true, "Normalization pattern: " + StringUtils.join(StringUtils.toArray(NORMALIZATION_PATTERNS.keySet()), ", "));
        normalizationPattern.setType(PatternOptionBuilder.STRING_VALUE);
        normalizationPattern.setArgName("patternName");
        OPTIONS.addOption(normalizationPattern);

        Option incrementalPattern = new Option(null, "variation-pattern", true, "Variation pattern (generate new matrices from the input one): " + StringUtils.join(StringUtils.toArray(INCREMENTAL_PATTERNS.keySet()), ", "));
        incrementalPattern.setType(PatternOptionBuilder.STRING_VALUE);
        incrementalPattern.setArgName("patternName");
        OPTIONS.addOption(incrementalPattern);

        Option normalizationPatternFile = new Option(null, "normalization-pattern-file", true, "Input file representing the normalization pattern matrix (a single value for total-normalization, row vector for row normalization, and column vector for column normalization)");
        normalizationPatternFile.setType(PatternOptionBuilder.FILE_VALUE);
        normalizationPatternFile.setArgName("file");
        OPTIONS.addOption(normalizationPatternFile);

        Option gravityModelFile = new Option(null, "gravity-model-file", true, "Input file representing the Nx2 matrix for the gravity model, where N is the number of nodes (first column: ingress traffic to the corresponding node, second column: egress traffic to the corresponding node)");
        gravityModelFile.setType(PatternOptionBuilder.FILE_VALUE);
        gravityModelFile.setArgName("file");
        OPTIONS.addOption(gravityModelFile);
    }

    @Override
    public void executeFromCommandLine(String[] args) throws ParseException {
        long init = System.nanoTime();

        final CommandLineParser parser = new CommandLineParser();
        final CommandLine cli = parser.parse(OPTIONS, args);

        int numNodes;
        NetPlan netPlan;

        if (cli.hasOption("num-nodes") && cli.hasOption("input-file"))
            throw new ParseException("'num-nodes' and 'input-file' are mutually exclusive");

        if (cli.hasOption("num-nodes")) {
            numNodes = ((Number) cli.getParsedOptionValue("num-nodes")).intValue();
            if (numNodes < 2) throw new Net2PlanException("Traffic matrix requires at least 2 nodes");

            netPlan = new NetPlan();
            for (int n = 0; n < numNodes; n++) netPlan.addNode(0, 0, null, null);
        } else {
            netPlan = new NetPlan((File) cli.getParsedOptionValue("input-file"));
            numNodes = netPlan.getNumberOfNodes();
        }

        int numMatrices = 1;
        String trafficPattern = null;

        DoubleMatrix2D[] trafficMatrices;
        if (cli.hasOption("variation-pattern")) {
            if (!cli.hasOption("num-matrices"))
                throw new Net2PlanException("'num-matrices' parameters must be specified");

            numMatrices = ((Number) cli.getParsedOptionValue("num-matrices")).intValue();
            if (numMatrices < 1) throw new Net2PlanException("Number of traffic matrices must be positive");

            DoubleMatrix2D trafficMatrix = netPlan.getMatrixNode2NodeOfferedTraffic();
            List<DoubleMatrix2D> newMatrices;
            String variationPattern = (String) cli.getParsedOptionValue("variation-pattern");
            switch (variationPattern) {
                case "cagr": {
                    double cagr = ((Number) cli.getParsedOptionValue("variation-pattern-cagr")).doubleValue();
                    if (cagr <= 0) throw new Net2PlanException("Compound annual growth rate must be greater than zero");
                    newMatrices = TrafficMatrixGenerationModels.computeMatricesCAGR(trafficMatrix, cagr, numMatrices);

                    break;
                }

                case "randomGaussian": {
                    double cv = ((Number) cli.getParsedOptionValue("variation-pattern-cv")).doubleValue();
                    double maxRelativeVariation = ((Number) cli.getParsedOptionValue("variation-pattern-maxRelativeVariation")).doubleValue();
                    if (cv <= 0) throw new Net2PlanException("Coefficient of variation must be greater than zero");
                    if (maxRelativeVariation <= 0)
                        throw new Net2PlanException("Maximum relative variation must be greater than zero");
                    newMatrices = TrafficMatrixGenerationModels.computeMatricesRandomGaussianVariation(trafficMatrix, cv, maxRelativeVariation, numMatrices);

                    break;
                }

                case "randomUniform": {
                    double maxRelativeVariation = ((Number) cli.getParsedOptionValue("variation-pattern-maxRelativeVariation")).doubleValue();
                    if (maxRelativeVariation <= 0)
                        throw new Net2PlanException("Maximum relative variation must be greater than zero");
                    newMatrices = TrafficMatrixGenerationModels.computeMatricesRandomUniformVariation(trafficMatrix, maxRelativeVariation, numMatrices);

                    break;
                }

                default:
                    throw new RuntimeException("Bad - Unknown variation pattern '" + variationPattern + "'");
            }

            trafficMatrices = new DoubleMatrix2D[numMatrices];
            int i = 0;
            for (DoubleMatrix2D trafficMatrix1 : newMatrices)
                trafficMatrices[i++] = trafficMatrix1;
        } else {
            if (cli.hasOption("traffic-pattern")) {
                trafficPattern = cli.getOptionValue("traffic-pattern");
                if (!TRAFFIC_PATTERNS.containsKey(trafficPattern))
                    throw new Net2PlanException("Unknown traffic pattern");

                if (cli.hasOption("num-matrices")) {
                    numMatrices = ((Number) cli.getParsedOptionValue("num-matrices")).intValue();
                    if (numMatrices < 1) throw new Net2PlanException("Number of traffic matrices must be positive");
                }
            }

            trafficMatrices = new DoubleMatrix2D[numMatrices];
            if (trafficPattern != null) {
                switch (trafficPattern) {
                    case "uniform-random-10":
                        for (int tmId = 0; tmId < numMatrices; tmId++)
                            trafficMatrices[tmId] = TrafficMatrixGenerationModels.uniformRandom(numNodes, 0, 10);
                        break;

                    case "uniform-random-100":
                        for (int tmId = 0; tmId < numMatrices; tmId++)
                            trafficMatrices[tmId] = TrafficMatrixGenerationModels.uniformRandom(numNodes, 0, 100);
                        break;

                    case "uniform-random-bimodal-50-50":
                        for (int tmId = 0; tmId < numMatrices; tmId++)
                            trafficMatrices[tmId] = TrafficMatrixGenerationModels.bimodalUniformRandom(numNodes, 0.5, 0, 100, 0, 10);
                        break;

                    case "uniform-random-bimodal-25-75":
                        for (int tmId = 0; tmId < numMatrices; tmId++)
                            trafficMatrices[tmId] = TrafficMatrixGenerationModels.bimodalUniformRandom(numNodes, 0.25, 0, 100, 0, 10);
                        break;

                    case "population-distance-model":
                        double randomFactor = 0;
                        double populationOffset = 0;
                        double populationPower = 1;
                        double distanceOffset = 0;
                        double distancePower = 1;
                        boolean normalizePopulation = true;
                        boolean normalizeDistance = true;

                        if (cli.hasOption("random-factor"))
                            randomFactor = ((Number) cli.getParsedOptionValue("random-factor")).doubleValue();
                        if (cli.hasOption("population-offset"))
                            populationOffset = ((Number) cli.getParsedOptionValue("population-offset")).doubleValue();
                        if (cli.hasOption("population-power"))
                            populationPower = ((Number) cli.getParsedOptionValue("population-power")).doubleValue();
                        if (cli.hasOption("distance-offset"))
                            distanceOffset = ((Number) cli.getParsedOptionValue("distance-offset")).doubleValue();
                        if (cli.hasOption("distance-power"))
                            distancePower = ((Number) cli.getParsedOptionValue("distance-power")).doubleValue();
                        if (cli.hasOption("normalize-population"))
                            normalizePopulation = Boolean.parseBoolean(cli.getOptionValue("normalize-population"));
                        if (cli.hasOption("normalize-distance"))
                            normalizeDistance = Boolean.parseBoolean(cli.getOptionValue("normalize-distance"));

                        if (!cli.hasOption("level-matrix-file"))
                            throw new Net2PlanException("The level-matrix file is required");
                        DoubleMatrix2D levelMatrix = DoubleUtils.read2DMatrixFromFile((File) cli.getParsedOptionValue("level-matrix-file"));

                        DoubleMatrix2D distanceMatrix = netPlan.getMatrixNode2NodeEuclideanDistance();
                        double[] populationVector = netPlan.getVectorNodePopulation().toArray();
                        int[] levelVector = StringUtils.toIntArray(NetPlan.getAttributes(netPlan.getNodes(), "level").values(), 1);

                        for (int tmId = 0; tmId < numMatrices; tmId++)
                            trafficMatrices[tmId] = TrafficMatrixGenerationModels.populationDistanceModel(distanceMatrix, populationVector, levelVector, levelMatrix, randomFactor, populationOffset, populationPower, distanceOffset, distancePower, normalizePopulation, normalizeDistance);

                        break;

                    case "gravity-model":
                        if (cli.hasOption("gravity-model-file")) {
                            File gravityModelFile = (File) cli.getParsedOptionValue("gravity-model-file");
                            DoubleMatrix2D gravityModelMatrix = DoubleUtils.read2DMatrixFromFile(gravityModelFile);

                            if (gravityModelMatrix.rows() != numNodes || gravityModelMatrix.columns() != 2)
                                throw new Net2PlanException("'gravity-model-file' requires " + numNodes + " rows and two columns");

                            numMatrices = 1;
                            trafficMatrices[0] = TrafficMatrixGenerationModels.gravityModel(gravityModelMatrix.viewColumn(0).toArray(), gravityModelMatrix.viewColumn(1).toArray());
                        } else {
                            throw new Net2PlanException("Parameter 'gravity-model-file' should be specified");
                        }

                        break;

                    default:
                        throw new RuntimeException("Bad - Unknown traffic pattern '" + trafficPattern + "'");
                }
            } else {
                trafficMatrices[0] = netPlan.getMatrixNode2NodeOfferedTraffic();
            }

            if (cli.hasOption("normalization-pattern")) {
                String normalizationPattern = (String) cli.getParsedOptionValue("normalization-pattern");
                switch (normalizationPattern) {
                    case "total-normalization":
                    case "row-normalization":
                    case "column-normalization":
                        if (cli.hasOption("normalization-pattern-file")) {
                            double[] normalizationPatternVector;
                            int patternId;

                            File normalizationPatternFile = (File) cli.getParsedOptionValue("normalization-pattern-file");
                            DoubleMatrix2D normalizationPatternMatrix = DoubleUtils.read2DMatrixFromFile(normalizationPatternFile);
                            if (normalizationPatternMatrix.rows() == 1 && normalizationPatternMatrix.columns() == 1) {
                                patternId = 0;
                                normalizationPatternVector = new double[]{normalizationPatternMatrix.getQuick(0, 0)};
                            } else if (normalizationPatternMatrix.rows() == 1 && normalizationPatternMatrix.columns() > 1) {
                                patternId = 1;
                                normalizationPatternVector = normalizationPatternMatrix.viewRow(0).toArray();
                            } else if (normalizationPatternMatrix.rows() > 1 && normalizationPatternMatrix.columns() == 1) {
                                patternId = 2;
                                normalizationPatternVector = normalizationPatternMatrix.viewColumn(0).toArray();
                            } else {
                                throw new Net2PlanException("Bad normalization pattern - Neither a scalar (for total normalization), nor row vector (for row normalization) nor a column vector (for column normalization) was provided");
                            }

                            for (int tmId = 0; tmId < numMatrices; tmId++) {
                                switch (patternId) {
                                    case 0:
                                        trafficMatrices[tmId] = TrafficMatrixGenerationModels.normalizationPattern_totalTraffic(trafficMatrices[tmId], normalizationPatternVector[0]);
                                        break;

                                    case 1:
                                        trafficMatrices[tmId] = TrafficMatrixGenerationModels.normalizationPattern_incomingTraffic(trafficMatrices[tmId], normalizationPatternVector);
                                        break;

                                    case 2:
                                        trafficMatrices[tmId] = TrafficMatrixGenerationModels.normalizationPattern_outgoingTraffic(trafficMatrices[tmId], normalizationPatternVector);
                                        break;

                                    default:
                                        throw new RuntimeException("Bad");
                                }
                            }
                        } else {
                            throw new Net2PlanException("Parameter 'normalization-pattern-file' should be specified");
                        }

                        break;

                    case "max-traffic-estimated-minHop":
                        for (int tmId = 0; tmId < numMatrices; tmId++) {
                            netPlan.setTrafficMatrix(trafficMatrices[tmId]);
                            netPlan.setVectorDemandOfferedTraffic(TrafficMatrixGenerationModels.normalizeTraffic_networkCapacity(netPlan));
                            trafficMatrices[tmId] = netPlan.getMatrixNode2NodeOfferedTraffic();
                        }
                        break;

                    case "max-traffic-exact":
                        String solverName = Configuration.getOption("defaultILPSolver");
                        String solverLibraryName = Configuration.getDefaultSolverLibraryName(solverName);
//                        if (solverName.equalsIgnoreCase("glpk")) solverLibraryName = Configuration.getOption("glpkSolverLibraryName");
//                        else if (solverName.equalsIgnoreCase("ipopt")) solverLibraryName = Configuration.getOption("ipoptSolverLibraryName");
//                        else if (solverName.equalsIgnoreCase("cplex")) solverLibraryName = Configuration.getOption("cplexSolverLibraryName");
//                        else if (solverName.equalsIgnoreCase("xpress")) solverLibraryName = Configuration.getOption("xpressSolverLicenseFileName");
//
                        for (int tmId = 0; tmId < numMatrices; tmId++) {
                            netPlan.setTrafficMatrix(trafficMatrices[tmId]);
                            netPlan.setVectorDemandOfferedTraffic(TrafficMatrixGenerationModels.normalizeTraffic_linkCapacity_xde(netPlan, solverName, solverLibraryName));
                            trafficMatrices[tmId] = netPlan.getMatrixNode2NodeOfferedTraffic();
                        }
                        break;

                    default:
                        throw new RuntimeException("Bad - Unknown normalization pattern '" + normalizationPattern + "'");
                }
            }
        }

        List<NetPlan> outputDemandSets = new LinkedList<NetPlan>();
        for (int tmId = 0; tmId < numMatrices; tmId++) {
            NetPlan aux = new NetPlan();

            aux.setTrafficMatrix(trafficMatrices[tmId]);
            outputDemandSets.add(aux);

            trafficMatrices[tmId] = null;
        }

        File outputFile = (File) cli.getParsedOptionValue("output-file");

        if (outputDemandSets.size() == 1) {
            outputDemandSets.get(0).saveToFile(outputFile);
        } else {
            String templateFileName = outputFile.getAbsoluteFile().toString();
            if (templateFileName.endsWith(".n2p"))
                templateFileName = templateFileName.substring(0, templateFileName.lastIndexOf('.'));

            ListIterator<NetPlan> netPlanIt = outputDemandSets.listIterator();
            while (netPlanIt.hasNext())
                netPlanIt.next().saveToFile(new File(templateFileName + "_tm" + netPlanIt.nextIndex() + ".n2p"));
        }

        long end = System.nanoTime();

        System.out.println(String.format("%n%nTraffic matrix generation finished successfully in %f seconds", (end - init) / 1e9));
    }

    @Override
    public String getCommandLineHelp() {
        return "Assists users in the process of "
                + "generating and normalizing traffic matrices i.e. following "
                + "random models found in the literature";
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
        return "traffic-design";
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
        return Integer.MAX_VALUE - 1;
    }
}
