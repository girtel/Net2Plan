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
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.utils.*;
import com.net2plan.utils.Constants.RoutingType;

import java.io.File;
import java.util.*;

/**
 * Searches for the OSPF link weights that minimize a measure of congestion, using a greedy heuristic
 * @net2plan.description
 * @net2plan.keywords IP/OSPF, Flow assignment (FA), Greedy heuristic
 * @net2plan.ocnbooksections Section 12.6
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_fa_ospfWeightOptimization_greedy implements IAlgorithm
{
	private TimeTrace stat_objFunction;
	private OSPFHeuristicUtils ospfEngine;

	private InputParameter greedy_initializationType = new InputParameter ("greedy_initializationType", "#select# random" , "The type of initialization of the OSPF link weights");
	private InputParameter greedy_differenceInWeightToBeNeighbors = new InputParameter ("greedy_differenceInWeightToBeNeighbors", (int) 1 , "Two solutions where all the links have the same weight, but one link where the weight differs in the quantity given by this parameter, are considered neighbors");
	private InputParameter ospf_maxLinkWeight = new InputParameter ("ospf_maxLinkWeight", (int) 16 , "OSPF link weights are constrained to be integers between 1 and this parameter" , 1 , Integer.MAX_VALUE);
	private InputParameter algorithm_randomSeed = new InputParameter ("algorithm_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter algorithm_outputFileNameRoot = new InputParameter ("algorithm_outputFileNameRoot", "ospfWeghtOptimization_greedy" , "Root of the file name to be used in the output files. If blank, no output");
	private InputParameter algorithm_numSamples = new InputParameter ("algorithm_numSamples", (int) 100 , "Number of repetitions, returns the last, but prints in file all of them" , 1 , Integer.MAX_VALUE);
	private InputParameter ospf_weightOfMaxUtilizationInObjectiveFunction = new InputParameter ("ospf_weightOfMaxUtilizationInObjectiveFunction", (double) 0.9 , "Objective function is this factor multiplied by maximum link utilization, plus 1 minus this factor by average link utilization" , 0 , true , 1 , true);

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		/* Basic checks */
		final int N = netPlan.getNumberOfNodes();
		final int E = netPlan.getNumberOfLinks();
		if (N == 0) throw new Net2PlanException("The input design must have nodes");
		netPlan.removeAllUnicastRoutingInformation();
		netPlan.setRoutingType (RoutingType.HOP_BY_HOP_ROUTING);

		
		Random rng = new Random (algorithm_randomSeed.getLong());
		this.ospfEngine = new OSPFHeuristicUtils(netPlan, ospf_maxLinkWeight.getInt (), ospf_weightOfMaxUtilizationInObjectiveFunction.getDouble(), rng);
		
		this.stat_objFunction = new TimeTrace ();

		for (int it = 0 ; it < algorithm_numSamples.getInt () ; it ++)
		{
			Pair<DoubleMatrix1D,Double> p = ospfEngine.getInitialSolution (greedy_initializationType.getString());
			DoubleMatrix1D currentSol = p.getFirst();
			double currentObjFunction = p.getSecond();

			/* Create a randomly shuffled sequence of link ids */
			List<Link> shuffledLinks = new ArrayList<Link> (netPlan.getLinks());
			Collections.shuffle(shuffledLinks);

			for (Link e : shuffledLinks)
			{
				Pair<double [],int []> pair = ospfEngine.computeSolutionsVaryingLinkWeight (e , currentSol , null , greedy_differenceInWeightToBeNeighbors.getInt ());
				final int indexOfBestWeight = DoubleUtils.maxIndexes (pair.getFirst() , Constants.SearchType.FIRST) [0];
				final int bestWeight = pair.getSecond() [indexOfBestWeight];
				currentObjFunction = pair.getFirst() [indexOfBestWeight];
				currentSol.set(e.getIndex(), (double) bestWeight);
			} 
			stat_objFunction.add(it , currentObjFunction);
		}
		
		stat_objFunction.printToFile(new File (algorithm_outputFileNameRoot.getString () + "_D" + greedy_differenceInWeightToBeNeighbors.getInt () + "_cong.txt"));

		double minObjectiveFunction = Double.MAX_VALUE;
		for (Pair<Double,Object> element : stat_objFunction.getList()) minObjectiveFunction = Math.min(minObjectiveFunction, (Double) element.getSecond());
		return "Ok! Min congestion factor: " + minObjectiveFunction;
	}

	@Override
	public String getDescription()
	{
		return "Given a set of nodes, links and offered traffic, this algorithm assumes that the nodes are IP routers running the OSPF protocol (applying ECMP rules) for routing it. The algorithm searches for the set of link weights that optimize the routing. In particular, the target is minimizing a congestion metric computed as a function of both the worst-case link utilization and the average link utilization. The algorithm is based on applying a greedy heuristic approach.";	
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
}