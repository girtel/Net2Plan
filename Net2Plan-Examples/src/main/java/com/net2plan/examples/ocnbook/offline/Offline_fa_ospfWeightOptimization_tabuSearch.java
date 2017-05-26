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
import com.net2plan.utils.Constants.OrderingType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.*;

import java.io.File;
import java.util.*;

/**
 * Searches for the OSPF link weights that minimize a measure of congestion, using a tabu search heuristic
 * 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec12_5_ospfWeightTabuSearch.m">{@code fig_sec12_5_ospfWeightTabuSearch.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * @net2plan.description
 * @net2plan.keywords IP/OSPF, Flow assignment (FA), Tabu search (TS)
 * @net2plan.ocnbooksections Section 12.5
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_fa_ospfWeightOptimization_tabuSearch implements IAlgorithm
{
	private int numIterations;
	private int numIterationsNonImprovingBestSolutionSinceLastRandomization;
	private TimeTrace stat_objFunction;
	private OSPFHeuristicUtils ospfEngine;
	private double [][] numberOccurrencies_ew;
	
	private InputParameter ts_initializationType = new InputParameter ("ts_initializationType", "#select# random" , "The type of initialization of the OSPF link weights");
	private InputParameter ospf_maxLinkWeight = new InputParameter ("ospf_maxLinkWeight", (int) 16 , "OSPF link weights are constrained to be integers between 1 and this parameter" , 1 , Integer.MAX_VALUE);
	private InputParameter algorithm_randomSeed = new InputParameter ("algorithm_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter algorithm_outputFileNameRoot = new InputParameter ("algorithm_outputFileNameRoot", "ospfWeghtOptimization_tabu" , "Root of the file name to be used in the output files. If blank, no output");
	private InputParameter ts_differenceInWeightToBeNeighbors = new InputParameter ("ts_differenceInWeightToBeNeighbors", (int) 1 , "Two solutions where all the links have the same weight, but one link where the weight differs in the quantity given by this parameter, are considered neighbors");
	private InputParameter ts_tabuListSizeAsFractionOfLinks = new InputParameter ("ts_tabuListSizeAsFractionOfLinks", (double) 0.5 , "Size of the tabe list, given as a fraction of the total number of links" , 0 , true , 1 , true);
	private InputParameter algorithm_maxExecutionTimeInSeconds = new InputParameter ("algorithm_maxExecutionTimeInSeconds", (double) 60 , "Algorithm maximum running time in seconds" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter ts_maxNumIterations = new InputParameter ("ts_maxNumIterations", (int) 50000 , "Maximum number of iterations" , 1 , Integer.MAX_VALUE);
	private InputParameter ts_maxNumIterationsNonImprovingIncumbentSolution = new InputParameter ("ts_maxNumIterationsNonImprovingIncumbentSolution", (int) 15 , "Num iterations non improving the incumbent solution, to restart the search in a randomized solution" , 1 , Integer.MAX_VALUE);
	private InputParameter ts_aspirationCriterion = new InputParameter ("ts_aspirationCriterion", true , "Apply aspiration criterion in tabu search");
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

		/* Initialize some variables */
		final long algorithmInitialtime = System.nanoTime();
		final long algorithmEndtime = algorithmInitialtime + (long) (algorithm_maxExecutionTimeInSeconds.getDouble() * 1E9);
		final int tabuListSize = (int) Math.ceil(E * ts_tabuListSizeAsFractionOfLinks.getDouble());
		if (tabuListSize >= E) throw new Net2PlanException ("The tabu list size is larger or equal than the number of links: all jumps would be tabu");

		Random rng = new Random (algorithm_randomSeed.getLong());
		this.ospfEngine = new OSPFHeuristicUtils(netPlan, ospf_maxLinkWeight.getInt (), ospf_weightOfMaxUtilizationInObjectiveFunction.getDouble(), rng);
		this.numberOccurrencies_ew = new double [E][ospf_maxLinkWeight.getInt ()];
		
		/* Tabu list info. Because of aspiration criterion, a link can appear more than once in the tabu list */
		Map<Link,Integer> numEntriesTabuList_e = new HashMap<Link,Integer> (); for (Link e : netPlan.getLinks()) numEntriesTabuList_e.put(e, 0); 
		LinkedList<Link> tabuList_e = new LinkedList<Link> ();
		
		Pair<DoubleMatrix1D,Double> p = ospfEngine.getInitialSolution (ts_initializationType.getString());
		DoubleMatrix1D currentSol = p.getFirst();
		double currentObjFunction = p.getSecond();

		DoubleMatrix1D bestSol = currentSol.copy ();
		double bestObjFunction = currentObjFunction;
		double bestObjFunctionSinceLastRandomization = currentObjFunction;
		
		this.numIterations= 0;
		this.numIterationsNonImprovingBestSolutionSinceLastRandomization = 0;
		this.stat_objFunction = new TimeTrace ();

		stat_objFunction.add(0.0, currentObjFunction);
		
		/* update the long-term memory*/
		for (int eIndex = 0 ; eIndex < E ; eIndex ++) this.numberOccurrencies_ew [eIndex][(int) (currentSol.get(eIndex)) - 1] ++;
		
		//System.out.println("Initial objFunction: " + bestObjFunction + ", tabu list tenure: " + tabuListSize);

		while ((System.nanoTime() < algorithmEndtime) && (numIterations < ts_maxNumIterations.getInt ()))
		{
			this.numIterations ++;
			Link bestNeighborLink = null;
			int bestNeighborWeight = -1;
			double bestNeighborObjFunction = Double.MAX_VALUE;
			
			/* Neighbors differing in one link */
			for (Link e1 : netPlan.getLinks())
			{
				final boolean isTabu = (numEntriesTabuList_e.get(e1) > 0);
				if (!ts_aspirationCriterion.getBoolean() && isTabu) continue;
				
				final int currentWeight1 = (int) currentSol.get(e1.getIndex ());
				for (int w1 = 1 ; w1 <= ospf_maxLinkWeight.getInt () ; w1 ++)
				{
					if (w1 == currentWeight1) continue;
					if (Math.abs(w1 - currentWeight1) > ts_differenceInWeightToBeNeighbors.getInt ()) continue;
					currentSol.set(e1.getIndex (), (double) w1);
					final double neighborObjFunction = ospfEngine.computeObjectiveFunction(currentSol).getFirst();
					currentSol.set(e1.getIndex (), (double) currentWeight1);
					
					/* Update best neighbor if (i) not tabu and improves best neighbor, (ii) tabu, but aspiration criterion is active and improves incumbent solution */
					if ( (!isTabu && (neighborObjFunction < bestNeighborObjFunction)) || (isTabu && ts_aspirationCriterion.getBoolean() && (neighborObjFunction < bestObjFunction)) ) 
					{
						// if (isTabu) System.out.println ("Aspiration criterion applied");
						bestNeighborLink = e1; bestNeighborWeight = w1; bestNeighborObjFunction = neighborObjFunction;
					}
				}
			}

			if (bestNeighborLink == null) { throw new RuntimeException ("This should not happen: it means a too large tabu list");  }
			
			/* Jump to the neighbor solution */
			currentSol.set(bestNeighborLink.getIndex (), (double) bestNeighborWeight); currentObjFunction = bestNeighborObjFunction;

			/* update tabu list */
			if (tabuList_e.size() == tabuListSize) { Link eToRemove = tabuList_e.removeFirst(); numEntriesTabuList_e.put(eToRemove, numEntriesTabuList_e.get(eToRemove) - 1); if (numEntriesTabuList_e.get(eToRemove) < 0) throw new RuntimeException ("Bad"); }
			tabuList_e.addLast(bestNeighborLink); numEntriesTabuList_e.put(bestNeighborLink , numEntriesTabuList_e.get(bestNeighborLink) + 1);

			/* update the incumbent solution */
			if (currentObjFunction < bestObjFunctionSinceLastRandomization)
			{
				this.numIterationsNonImprovingBestSolutionSinceLastRandomization = 0;
				bestObjFunctionSinceLastRandomization = currentObjFunction; 
				//System.out.println("Improving best objFunction since last randomization: " + currentObjFunction);
				if (currentObjFunction < bestObjFunction) { bestObjFunction = currentObjFunction; bestSol = currentSol.copy (); /* System.out.println("Improving best objFunction: " + bestObjFunction); */ }
			}
			else
				this.numIterationsNonImprovingBestSolutionSinceLastRandomization ++;
			
			/* update the long-term memory*/
			for (int eIndex = 0 ; eIndex < E ; eIndex ++) this.numberOccurrencies_ew [eIndex][(int) (currentSol.get(eIndex)) - 1] ++;

			/* Check if too many iterations without improving the incumbent solution */
			if (numIterationsNonImprovingBestSolutionSinceLastRandomization > ts_maxNumIterationsNonImprovingIncumbentSolution.getInt ())
			{
				// System.out.println("Randomization!!");
				
				/* Initialize tabu list */
				tabuList_e.clear(); for (Link e : netPlan.getLinks()) numEntriesTabuList_e.put(e, 0); 

				/* Make a diversification jump: incumbent solution, then each link with prob 0.5 randomizes its weight */
				currentSol = bestSol.copy();
				for (Link e : netPlan.getLinks())
				{
					if (rng.nextBoolean()) continue; 
					final int [] leastUtilizedWeightsMinus1 = DoubleUtils.sortIndexes(this.numberOccurrencies_ew [e.getIndex ()], OrderingType.ASCENDING);
					final int randomlyChosenWeight = leastUtilizedWeightsMinus1 [rng.nextInt(ospf_maxLinkWeight.getInt ()/2)] + 1;
					currentSol.set(e.getIndex (), (double) randomlyChosenWeight);
				}
				currentObjFunction = ospfEngine.computeObjectiveFunction(currentSol).getFirst();
				bestObjFunctionSinceLastRandomization = currentObjFunction;
				this.numIterationsNonImprovingBestSolutionSinceLastRandomization = 0;
			}
			
			final double currentTimeInSecs = (System.nanoTime() - algorithmInitialtime) * 1e-9;
			stat_objFunction.add(currentTimeInSecs , currentObjFunction);
		} 

		IPUtils.setECMPForwardingRulesFromLinkWeights(netPlan, bestSol);

		final String completeFileName = algorithm_outputFileNameRoot.getString () + "_0" + ((int) (10*ts_tabuListSizeAsFractionOfLinks.getDouble())) + ((ts_aspirationCriterion.getBoolean())? "_asp" : "_noasp"); 
		stat_objFunction.printToFile(new File (completeFileName + "_cong.txt"));

		System.out.println("Ok! ObjFunction: " + bestObjFunction + ", num it: " + numIterations + ", Best solution: " + bestSol);
		return "Ok! ObjFunction: " + bestObjFunction + ", num it: " + numIterations;
	}

	@Override
	public String getDescription()
	{
		return "Given a set of nodes, links and offered traffic, this algorithm assumes that the nodes are IP routers running the OSPF protocol (applying ECMP rules) for routing it. The algorithm searches for the set of link weights that optimize the routing. In particular, the target is minimizing a congestion metric computed as a function of both the worst-case link utilization and the average link utilization. The algorithm is based on applying a tabu search heuristic approach.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

}