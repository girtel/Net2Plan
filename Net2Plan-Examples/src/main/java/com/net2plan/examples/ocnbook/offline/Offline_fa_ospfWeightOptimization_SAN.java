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
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Searches for the OSPF link weights that minimize a measure of congestion, using a simulated annealing (SAN) heuristic
 * 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec12_4_ospfWeightLocalSAN.m">{@code fig_sec12_4_ospfWeightSAN.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * @net2plan.description
 * @net2plan.keywords IP/OSPF, Flow assignment (FA), Simulated annealing (SAN)
 * @net2plan.ocnbooksections Section 12.4
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_fa_ospfWeightOptimization_SAN implements IAlgorithm
{
	private int numIterOuterLoop;

	private TimeTrace stat_objFunction;
	private TimeTrace stat_bestObjFunction;
	private TimeTrace stat_temperature;
	private OSPFHeuristicUtils ospfEngine;

	private InputParameter san_initializationType = new InputParameter ("san_initializationType", "#select# random" , "The type of initialization of the OSPF link weights");
	private InputParameter san_worseCaseObjFunctionWorsening = new InputParameter ("san_worseCaseObjFunctionWorsening", (double) 0.25 , "Objective function worsening in a transition considered as worse case for initial temperature computation" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter san_geometricTemperatureReductionFactor = new InputParameter ("san_geometricTemperatureReductionFactor", (double) 0.8 , "Geometric decrease factor of the temperature" , 0 , true , 1 , true);
	private InputParameter san_freezingProbabilityThreshold = new InputParameter ("san_freezingProbabilityThreshold", (double) 0.01 , "If the fraction of iterations with a jump, in the inner loop, is below this, the system is reheated (temperature to the initial value). If not, decreased geometrically" , 0 , true , 1 , true);
	private InputParameter san_initialAcceptanceProbability = new InputParameter ("san_initialAcceptanceProbability", (double) 0.5 , "Acceptance probability of the worst case transition: to compute the initial temperature" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter san_maxNumIterationsInnerLoop = new InputParameter ("san_maxNumIterationsInnerLoop", (int) 1000 , "Number of iterations of the inner loop (all with the same temperature)" , 1 , Integer.MAX_VALUE);
	private InputParameter san_maxNumIterationsOuterLoop = new InputParameter ("san_maxNumIterationsOuterLoop", (int) 50 , "Number of iterations of the outer loop" , 1 , Integer.MAX_VALUE);
	private InputParameter san_differenceInWeightToBeNeighbors = new InputParameter ("san_differenceInWeightToBeNeighbors", (int) 1 , "Two solutions where all the links have the same weight, but one link where the weight differs in the quantity given by this parameter, are considered neighbors");
	private InputParameter ospf_maxLinkWeight = new InputParameter ("ospf_maxLinkWeight", (int) 16 , "OSPF link weights are constrained to be integers between 1 and this parameter" , 1 , Integer.MAX_VALUE);
	private InputParameter ospf_weightOfMaxUtilizationInObjectiveFunction = new InputParameter ("ospf_weightOfMaxUtilizationInObjectiveFunction", (double) 0.9 , "Objective function is this factor multiplied by maximum link utilization, plus 1 minus this factor by average link utilization" , 0 , true , 1 , true);
	private InputParameter algorithm_randomSeed = new InputParameter ("algorithm_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter algorithm_outputFileNameRoot = new InputParameter ("algorithm_outputFileNameRoot", "ospfWeghtOptimization_san" , "Root of the file name to be used in the output files. If blank, no output");
	private InputParameter algorithm_maxExecutionTimeInSeconds = new InputParameter ("algorithm_maxExecutionTimeInSeconds", (double) 60 , "Algorithm maximum running time in seconds" , 0 , false , Double.MAX_VALUE , true);

	
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		/* Basic checks */
		final int N = netPlan.getNumberOfNodes();
		final int E = netPlan.getNumberOfLinks ();
		if (N == 0) throw new Net2PlanException("The input design must have nodes");
		netPlan.removeAllUnicastRoutingInformation();
		netPlan.setRoutingType (RoutingType.HOP_BY_HOP_ROUTING);

		final long algorithmInitialtime = System.nanoTime();
		final long algorithmEndtime = algorithmInitialtime + (long) (algorithm_maxExecutionTimeInSeconds.getDouble() * 1E9);
		
		/* Compute initial solution */ 
		Random rng = new Random (algorithm_randomSeed.getLong ());
		this.ospfEngine = new OSPFHeuristicUtils(netPlan, ospf_maxLinkWeight.getInt (), ospf_weightOfMaxUtilizationInObjectiveFunction.getDouble (), rng);
		this.stat_bestObjFunction = new TimeTrace ();
		this.stat_objFunction = new TimeTrace ();
		this.stat_temperature = new TimeTrace ();
		this.numIterOuterLoop = 0;

		Pair<DoubleMatrix1D,Double> p = ospfEngine.getInitialSolution (san_initializationType.getString ());
		DoubleMatrix1D currentSol = p.getFirst();
		double currentObjFunction = p.getSecond();
		
		/* Compute the initial and end temperatures, and the geometric temperature reduction factor */
		final double initialTemperature = -san_worseCaseObjFunctionWorsening.getDouble () / Math.log(san_initialAcceptanceProbability.getDouble ());

		//System.out.println("initialTemperature: " + initialTemperature + ", geometricReductionFactor: " + san_geometricTemperatureReductionFactor.getDouble ());
		
		double currentTemperature = initialTemperature;
		DoubleMatrix1D bestSol = currentSol.copy ();
		double bestObjFunction = currentObjFunction;

		stat_objFunction.add(0.0 , currentObjFunction);
		stat_bestObjFunction.add(0.0 , bestObjFunction);
		stat_temperature.add(0.0 , currentTemperature);
		while ((System.nanoTime() < algorithmEndtime) && (numIterOuterLoop < san_maxNumIterationsOuterLoop.getInt ()))
		{
			numIterOuterLoop ++;
			int numFrozenJumps = 0;
			for (int innerIt = 0 ; innerIt < san_maxNumIterationsInnerLoop.getInt () ; innerIt ++)
			{
				final Link neighborDifferentLink = netPlan.getLink (rng.nextInt(E));
				final double oldWeight = currentSol.get(neighborDifferentLink.getIndex ());
				double newWeight = 1;
				do 
				{ newWeight = oldWeight + rng.nextInt(2*san_differenceInWeightToBeNeighbors.getInt ()+1) - san_differenceInWeightToBeNeighbors.getInt (); } 
				while ((oldWeight == newWeight) || (newWeight < 1) || (newWeight > ospf_maxLinkWeight.getInt ()));

				currentSol.set(neighborDifferentLink.getIndex (), newWeight);
				final double neighborObjFunction = ospfEngine.computeObjectiveFunction(currentSol).getFirst();
				
				if ((neighborObjFunction < currentObjFunction) || (rng.nextDouble() < Math.exp(-(neighborObjFunction - currentObjFunction)/currentTemperature)))
				{
					/* If this jump does not change objFunction, consider it a frozen one */
					if (currentObjFunction == neighborObjFunction) numFrozenJumps ++;
					/* jump to the neighbor solution */
					currentObjFunction = neighborObjFunction;
					if (neighborObjFunction < bestObjFunction) { bestObjFunction = neighborObjFunction; bestSol = currentSol.copy (); /* System.out.println("Improving solution (temp: " + currentTemperature + ", objFunction: "+  bestObjFunction); */ }
				}
				else
				{
					currentSol.set(neighborDifferentLink.getIndex (), oldWeight);
					numFrozenJumps ++;
				}
				final double currentTimeInSecs = (System.nanoTime() - algorithmInitialtime) * 1e-9;
				stat_objFunction.add(currentTimeInSecs , currentObjFunction);
				stat_bestObjFunction.add(currentTimeInSecs , bestObjFunction);
				stat_temperature.add(currentTimeInSecs , currentTemperature);
			}
			if (((double) numFrozenJumps)/san_maxNumIterationsInnerLoop.getInt () > 1-san_freezingProbabilityThreshold.getDouble ())
				{ currentTemperature = initialTemperature; /* System.out.println("Reheat the system."); */ } // reheat the system  
			else
				{ currentTemperature *= san_geometricTemperatureReductionFactor.getDouble(); /* System.out.println("Decrease temperature."); */ } // decrease temperature
		}
		
		IPUtils.setECMPForwardingRulesFromLinkWeights(netPlan, bestSol);

		stat_bestObjFunction.printToFile(new File (algorithm_outputFileNameRoot.getString () + "_bestObjFunc.txt"));
		stat_objFunction.printToFile(new File (algorithm_outputFileNameRoot.getString () + "_objFunc.txt"));
		stat_temperature.printToFile(new File (algorithm_outputFileNameRoot.getString () + "_temp.txt"));
		System.out.println("Ok! ObjFunction: " + bestObjFunction );
		return "Ok! ObjFunction: " + bestObjFunction;
	}

	@Override
	public String getDescription()
	{
		return "Given a set of nodes, links and offered traffic, this algorithm assumes that the nodes are IP routers running the OSPF protocol (applying ECMP rules) for routing it. The algorithm searches for the set of link weights that optimize the routing. In particular, the target is minimizing a congestion metric computed as a function of both the worst-case link utilization and the average link utilization. The algorithm is based on applying a simulated annealign (SAN) heuristic approach.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

}