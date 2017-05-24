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
package com.net2plan.examples.ocnbook.offline;


import cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.TimeTrace;
import com.net2plan.utils.Triple;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Searches for the OSPF link weights that minimize a measure of congestion, using a local-search heuristic
 * 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec12_3_ospfWeightLocalSearch.m">{@code fig_sec12_3_ospfWeightLocalSearch.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * @net2plan.description
 * @net2plan.keywords IP/OSPF, Flow assignment (FA), Local search (LS) heuristic
 * @net2plan.ocnbooksections Section 12.3
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_fa_ospfWeightOptimization_localSearch implements IAlgorithm
{
	private OSPFHeuristicUtils ospfEngine;
	private TimeTrace stat_objFunction;
	private TimeTrace stat_computationTime;
	private TimeTrace stat_numObjFuncEvaluations;

	private InputParameter algorithm_randomSeed = new InputParameter ("algorithm_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter algorithm_numSamples = new InputParameter ("algorithm_numSamples", (int) 100 , "Number of repetitions, returns the last, but saves in file all of them" , 1 , Integer.MAX_VALUE);
	private InputParameter algorithm_outputFileNameRoot = new InputParameter ("algorithm_outputFileNameRoot", "ospfWeghtOptimization_ls" , "Root of the file name to be used in the output files. If blank, no output");
	private InputParameter localSearch_initializationType = new InputParameter ("localSearch_initializationType", "#select# random" , "The type of initialization of the OSPF link weights");
	private InputParameter localSearch_type = new InputParameter ("localSearch_type", "#select# first-fit best-fit" , "The tpe of local search algorithm. First-fit, jumps to the first improving neighbor solution found, best-fit to the best improving neighbor");
	private InputParameter localSearch_differenceInWeightToBeNeighbors = new InputParameter ("localSearch_differenceInWeightToBeNeighbors", (int) 1 , "Two solutions where all the links have the same weight, but one link where the weight differs in the quantity given by this parameter, are considered neighbors");
	private InputParameter ospf_maxLinkWeight = new InputParameter ("ospf_maxLinkWeight", (int) 16 , "OSPF link weights are constrained to be integers between 1 and this parameter" , 1 , Integer.MAX_VALUE);
	private InputParameter ospf_weightOfMaxUtilizationInObjectiveFunction = new InputParameter ("ospf_weightOfMaxUtilizationInObjectiveFunction", (double) 0.9 , "Objective function is this factor multiplied by maximum link utilization, plus 1 minus this factor by average link utilization" , 0 , true , 1 , true);

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		/* Basic checks */
		final int N = netPlan.getNumberOfNodes();
		if (N == 0) throw new Net2PlanException("The input design must have nodes");
		netPlan.removeAllUnicastRoutingInformation();
		netPlan.setRoutingType (RoutingType.HOP_BY_HOP_ROUTING);
		
		
		/* Initialize some variables */
		final boolean isFirstFit = localSearch_type.getString ().equals ("first-fit");

		Random rng = new Random (algorithm_randomSeed.getLong ());
		this.ospfEngine = new OSPFHeuristicUtils(netPlan, ospf_maxLinkWeight.getInt (), ospf_weightOfMaxUtilizationInObjectiveFunction.getDouble(), rng);
		this.stat_objFunction = new TimeTrace  ();
		this.stat_computationTime = new TimeTrace  ();
		this.stat_numObjFuncEvaluations = new TimeTrace ();
		
		System.out.println("isFirstFit: " + isFirstFit);
		
		for (int it = 0 ; it < algorithm_numSamples.getInt () ; it ++)
		{
			final long initTime = System.nanoTime();
			Pair<DoubleMatrix1D,Double> p1 = ospfEngine.getInitialSolution (localSearch_initializationType.getString());
			DoubleMatrix1D currentSol = p1.getFirst();
			double currentObjFunction = p1.getSecond();
	
			final Pair<Double,Integer> p2 = ospfEngine.localSearch (currentSol , currentObjFunction , localSearch_differenceInWeightToBeNeighbors.getInt () , isFirstFit);
			final double bestObjFunc = p2.getFirst();
			final int numObjFunctionEvaluations = p2.getSecond();
			final double computationTimeIbnSeconds = (System.nanoTime() - initTime)*1e-9;
			stat_computationTime.add(it, computationTimeIbnSeconds);
			stat_objFunction.add(it, bestObjFunc);
			stat_numObjFuncEvaluations.add(it, numObjFunctionEvaluations);
		}
		
		stat_objFunction.printToFile(new File (algorithm_outputFileNameRoot.getString () + "_D" + localSearch_differenceInWeightToBeNeighbors.getInt () + ((isFirstFit)? "_FF_" : "_BF_") + "objFunc.txt"));
		stat_computationTime.printToFile(new File (algorithm_outputFileNameRoot.getString () + "_D" + localSearch_differenceInWeightToBeNeighbors.getInt () + ((isFirstFit)? "_FF_" : "_BF_") + "time.txt"));
		stat_numObjFuncEvaluations.printToFile(new File (algorithm_outputFileNameRoot.getString () + "_D" + localSearch_differenceInWeightToBeNeighbors.getInt () + ((isFirstFit)? "_FF_" : "_BF_") + "numEvalObjFunc.txt"));

		
		double minObjectiveFunction = Double.MAX_VALUE;
		for (Pair<Double,Object> element : stat_objFunction.getList())
			minObjectiveFunction = Math.min(minObjectiveFunction, (Double) element.getSecond());
		return "Ok! Min OF: " + minObjectiveFunction;

	}

	@Override
	public String getDescription()
	{
		return "Given a set of nodes, links and offered traffic, this algorithm assumes that the nodes are IP routers running the OSPF protocol (applying ECMP rules) for routing it. The algorithm searches for the set of link weights that optimize the routing. In particular, the target is minimizing a congestion metric computed as a function of both the worst-case link utilization and the average link utilization. The algorithm is based on applying a local-search heuristic approach.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
}