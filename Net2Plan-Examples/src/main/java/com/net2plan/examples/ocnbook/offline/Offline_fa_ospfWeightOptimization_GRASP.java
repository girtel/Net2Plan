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
import com.net2plan.libraries.IPUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.TimeTrace;
import com.net2plan.utils.Triple;

import java.io.File;
import java.util.*;

/**
 * Searches for the OSPF link weights that minimize a measure of congestion, using a GRASP heuristic
 * 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec12_7_ospfWeightGRASP.m">{@code fig_sec12_7_ospfWeightGRASP.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * @net2plan.description
 * @net2plan.keywords IP/OSPF, Flow assignment (FA), GRASP
 * @net2plan.ocnbooksections Section 12.7
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_fa_ospfWeightOptimization_GRASP implements IAlgorithm
{
	private TimeTrace stat_objFunction;
	private OSPFHeuristicUtils ospfEngine;
	
	private InputParameter grasp_initializationType = new InputParameter ("grasp_initializationType", "#select# random" , "The type of initialization of the OSPF link weights");
	private InputParameter ospf_maxLinkWeight = new InputParameter ("ospf_maxLinkWeight", (int) 16 , "OSPF link weights are constrained to be integers between 1 and this parameter" , 1 , Integer.MAX_VALUE);
	private InputParameter grasp_differenceInWeightToBeNeighbors = new InputParameter ("grasp_differenceInWeightToBeNeighbors", (int) 1 , "Two solutions where all the links have the same weight, but one link where the weight differs in the quantity given by this parameter, are considered neighbors");
	private InputParameter algorithm_randomSeed = new InputParameter ("algorithm_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter algorithm_outputFileNameRoot = new InputParameter ("algorithm_outputFileNameRoot", "ospfWeghtOptimization_grasp" , "Root of the file name to be used in the output files. If blank, no output");
	private InputParameter ospf_weightOfMaxUtilizationInObjectiveFunction = new InputParameter ("ospf_weightOfMaxUtilizationInObjectiveFunction", (double) 0.9 , "Objective function is this factor multiplied by maximum link utilization, plus 1 minus this factor by average link utilization" , 0 , true , 1 , true);
	private InputParameter grasp_rclRandomnessFactor = new InputParameter ("grasp_rclRandomnessFactor", (double) 0.5 , "Factor to compute the Restricted Candidate List in the (RCL) in the greedy randomized part" , 0 , true , 1 , true);
	private InputParameter algorithm_maxExecutionTimeInSeconds = new InputParameter ("algorithm_maxExecutionTimeInSeconds", (double) 60 , "Algorithm maximum running time in seconds" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter grasp_maxNumIterations = new InputParameter ("grasp_maxNumIterations", (int) 50000 , "Maximum number of iterations" , 1 , Integer.MAX_VALUE);

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
		final long algorithmInitialtime = System.nanoTime();
		final long algorithmEndtime = algorithmInitialtime + (long) (algorithm_maxExecutionTimeInSeconds.getDouble() * 1E9);
		
		this.stat_objFunction = new TimeTrace ();
		
		DoubleMatrix1D bestSol = null;
		double bestObjFunction = Double.MAX_VALUE;

		int iterationCounter = 0;
		while ((System.nanoTime() < algorithmEndtime) && (iterationCounter < grasp_maxNumIterations.getInt ()))
		{
			iterationCounter ++;
			
			/* Try a greedy randomized solution */

			/* Get an initial solution */
			Pair<DoubleMatrix1D,Double> p = ospfEngine.getInitialSolution (grasp_initializationType.getString());
			DoubleMatrix1D currentSol = p.getFirst();
			double currentObjFunction = p.getSecond();
			
			/* Greedy approach for each link */
			List<Link> shuffledLinks = new ArrayList<Link> (netPlan.getLinks());
			Collections.shuffle(shuffledLinks , rng);
			for (Link e : shuffledLinks)
				currentObjFunction = greedyRandomizedWeightChoice (e , currentSol , ospf_maxLinkWeight.getInt () , grasp_rclRandomnessFactor.getDouble() , grasp_differenceInWeightToBeNeighbors.getInt () , ospf_weightOfMaxUtilizationInObjectiveFunction.getDouble() , netPlan , rng);

			final double greedySolutionCongestion = currentObjFunction; 
			
			/* Update the incumbent solution */
			if (currentObjFunction < bestObjFunction) { bestObjFunction = currentObjFunction; bestSol = currentSol.copy (); }

			/* Local search */
			Pair<Double,Integer> localSearchRes = ospfEngine.localSearch(currentSol, currentObjFunction, grasp_differenceInWeightToBeNeighbors.getInt (), true);
			currentObjFunction = localSearchRes.getFirst();

			/* Update the incumbent solution */
			if (currentObjFunction < bestObjFunction) { bestObjFunction = currentObjFunction; bestSol = currentSol.copy (); }
			
			stat_objFunction.add(iterationCounter, new double [] { greedySolutionCongestion , currentObjFunction } );
		}
		
		stat_objFunction.printToFile(new File (algorithm_outputFileNameRoot.getString () + "_D" + grasp_differenceInWeightToBeNeighbors.getInt () + "_cong.txt"));

		IPUtils.setECMPForwardingRulesFromLinkWeights(netPlan, bestSol);

		System.out.println("Ok! Best solution OF: " + bestObjFunction);
		return "Ok! Best OF: " + bestObjFunction;
	}

	@Override
	public String getDescription()
	{
		return "Given a set of nodes, links and offered traffic, this algorithm assumes that the nodes are IP routers running the OSPF protocol (applying ECMP rules) for routing it. The algorithm searches for the set of link weights that optimize the routing. In particular, the target is minimizing a congestion metric computed as a function of both the worst-case link utilization and the average link utilization. The algorithm is based on applying a GRASP heuristic approach (Greedy Randomzied Adaptive Search Procedure).";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	private double greedyRandomizedWeightChoice (Link e , DoubleMatrix1D currentSol , int maxLinkWeight , double rclRandomnessFactor , int differenceInWeightToBeNeighbors , double weightOfMaxUtilizationInObjectiveFunction , NetPlan np , Random rng)
	{
		Pair<double [],int []> pair = ospfEngine.computeSolutionsVaryingLinkWeight (e , currentSol , null , differenceInWeightToBeNeighbors);
		double [] objFunc_w = pair.getFirst();
		int [] wIds_w = pair.getSecond();
		
		/* Here if there is some greedy information to consider also */
		double maxObjFunc = -Double.MAX_VALUE;
		double minObjFunc = Double.MAX_VALUE;
		for (int cont = 0 ; cont < wIds_w.length ; cont ++)
		{
			final double greedyObjFunction = objFunc_w [cont];
			maxObjFunc = Math.max(maxObjFunc,greedyObjFunction);
			minObjFunc = Math.min(minObjFunc,greedyObjFunction);
		}
		
		/* Compute the obj function threshold to be a restricted candidate  */
		final double rclThresholdObjFunc = minObjFunc + (maxObjFunc - minObjFunc) * rclRandomnessFactor;
		ArrayList<Integer> rcl = new ArrayList<Integer> (maxLinkWeight);
		for (int cont = 0 ; cont < wIds_w.length ; cont ++)
		{
			final double greedyObjFunction = objFunc_w [cont];
			if (greedyObjFunction <= rclThresholdObjFunc) rcl.add (cont);
		}
		final int chosenIndexOfTheWeight = rcl.get(rng.nextInt(rcl.size()));
		final int chosenWeight = wIds_w [chosenIndexOfTheWeight];
		final double chosenObjFunc = objFunc_w [chosenIndexOfTheWeight];
		currentSol.set(e.getIndex (), (double) chosenWeight);
		return chosenObjFunc;
	}

}