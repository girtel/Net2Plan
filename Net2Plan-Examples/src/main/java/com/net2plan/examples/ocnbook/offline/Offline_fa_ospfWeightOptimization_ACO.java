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
 * Searches for the OSPF link weights that minimize a measure of congestion, using an ant-colony optimization (ACO) heuristic.
 * 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec12_8_ospfWeightACO.m">{@code fig_sec12_8_ospfWeightACO.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * @net2plan.description
 * @net2plan.keywords IP/OSPF, Flow assignment (FA), Ant Colony Optimization (ACO)
 * @net2plan.ocnbooksections Section 12.8
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_fa_ospfWeightOptimization_ACO implements IAlgorithm
{
	private TimeTrace stat_objFunction;
	private TimeTrace stat_pheromone_ew;
	private OSPFHeuristicUtils ospfEngine;

	private InputParameter aco_initializationType = new InputParameter ("aco_initializationType", "#select# random" , "The type of initialization of the OSPF link weights");
	private InputParameter ospf_maxLinkWeight = new InputParameter ("ospf_maxLinkWeight", (int) 16 , "OSPF link weights are constrained to be integers between 1 and this parameter" , 1 , Integer.MAX_VALUE);
	private InputParameter aco_differenceInWeightToBeNeighbors = new InputParameter ("aco_differenceInWeightToBeNeighbors", (int) 1 , "Two solutions where all the links have the same weight, but one link where the weight differs in the quantity given by this parameter, are considered neighbors");
	private InputParameter algorithm_randomSeed = new InputParameter ("algorithm_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter algorithm_outputFileNameRoot = new InputParameter ("algorithm_outputFileNameRoot", "ospfWeghtOptimization_aco" , "Root of the file name to be used in the output files. If blank, no output");
	private InputParameter ospf_weightOfMaxUtilizationInObjectiveFunction = new InputParameter ("ospf_weightOfMaxUtilizationInObjectiveFunction", (double) 0.9 , "Objective function is this factor multiplied by maximum link utilization, plus 1 minus this factor by average link utilization" , 0 , true , 1 , true);
	private InputParameter algorithm_maxExecutionTimeInSeconds = new InputParameter ("algorithm_maxExecutionTimeInSeconds", (double) 5 , "Algorithm maximum running time in seconds" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter aco_maxNumIterations = new InputParameter ("aco_maxNumIterations", (int) 100 , "Maximum number of iterations" , 1 , Integer.MAX_VALUE);
	private InputParameter aco_pheromoneEvaporationRate = new InputParameter ("aco_pheromoneEvaporationRate", (double) 0.5 , "Evaporation factor of the pheromones in the ACO model" , 0 , true , 1 , true);
	private InputParameter aco_factorImportanceOfPheromones = new InputParameter ("aco_factorImportanceOfPheromones", (double) 1 , "Importance of pheromones vs greedy step for the ants" , 0 , true , 1 , true);
	private InputParameter aco_numAnts = new InputParameter ("aco_numAnts", (int) 10 , "Number of ants in the system" , 1 , Integer.MAX_VALUE);
	private InputParameter aco_fractionOfEliteAntsContributingToPheromones = new InputParameter ("aco_fractionOfEliteAntsContributingToPheromones", (double) 0.5 , "Only the best fraction of the ants (round up) contributes to pheromone reinforcement" , 0 , true , 1 , true);
	
	
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
		this.stat_pheromone_ew = new TimeTrace ();
		DoubleMatrix1D bestSol = null;
		double bestObjFunction = Double.MAX_VALUE;

		/* Initialize all pheromones to zero */
		double [][] pheromones_ew = new double [E][ospf_maxLinkWeight.getInt ()]; for (int e = 0 ; e < E ; e ++) Arrays.fill (pheromones_ew [e] , 1.0); 

		int iterationCounter = 0;
		while ((System.nanoTime() < algorithmEndtime) && (iterationCounter < aco_maxNumIterations.getInt ()))
		{
			iterationCounter ++;
			
			/* To store each ant solution and objective function */
			double [] objFunc_a = new double [aco_numAnts.getInt ()];
			ArrayList<DoubleMatrix1D> sol_a = new ArrayList<DoubleMatrix1D> (aco_numAnts.getInt ()); 
			for (int contAnt = 0 ; contAnt < aco_numAnts.getInt () ; contAnt ++)
			{
				Pair<DoubleMatrix1D,Double> p = ospfEngine.getInitialSolution (aco_initializationType.getString ());
				DoubleMatrix1D currentSol = p.getFirst();

				/* Create a randomly shuffled sequence of link ids */
				List<Link> shuffledLinks = new ArrayList<Link> (netPlan.getLinks());
				Collections.shuffle(shuffledLinks , rng);

				/* The ant movements */
				for (Link e : shuffledLinks)
					antWeightChoice (e , currentSol , ospf_maxLinkWeight.getInt () , aco_differenceInWeightToBeNeighbors.getInt () , pheromones_ew [e.getIndex ()] , aco_factorImportanceOfPheromones.getDouble() , rng);

				double objFunc = ospfEngine.computeObjectiveFunction(currentSol).getFirst();
				objFunc_a [contAnt] = objFunc;
				sol_a.add(currentSol);
				
				/* Update the incumbent solution */
				if (objFunc < bestObjFunction) { bestObjFunction = objFunc; bestSol = currentSol.copy (); }
				
			}
			
			/* Truncate it in case we end the algorithm ahead of time */
			objFunc_a = Arrays.copyOf(objFunc_a, sol_a.size());

			/* Increase the pheromones */
			int [] orderedAntIndexes = DoubleUtils.sortIndexes(objFunc_a, OrderingType.ASCENDING);
			final int numEliteAnts = (int) Math.ceil(sol_a.size() * aco_fractionOfEliteAntsContributingToPheromones.getDouble());
			for (int cont = 0 ; cont < numEliteAnts ; cont ++)
			{
				final int a = orderedAntIndexes [cont];
				final double objFuncAnt = objFunc_a [a];
				for (Link e : netPlan.getLinks())
				{
					final int linkWeightAnt = (int) sol_a.get(a).get(e.getIndex ());
					pheromones_ew[e.getIndex ()][linkWeightAnt-1] += 1.0 / objFuncAnt;
				}
			}
			
			/* Evaporate pheromones */
			for (Link e : netPlan.getLinks())
			{
				double [] pheromone_w = pheromones_ew [e.getIndex ()]; 
				for (int w = 0; w < pheromone_w.length ; w ++) pheromone_w[w] *= 1 - aco_pheromoneEvaporationRate.getDouble(); 
			}
			
			/* Update the stats */
			//this.stat_objFunction.add(iterationCounter , Arrays.copyOf (objFunc_a , numAnts));
			this.stat_objFunction.add(iterationCounter , copyOf (objFunc_a , sol_a.size() , aco_numAnts.getInt ()));
			this.stat_pheromone_ew.add (iterationCounter , copyOf(pheromones_ew));
		}
		
		final String baseFileName = algorithm_outputFileNameRoot.getString () + "_a" + aco_numAnts.getInt () + "_rho" + ((int) (aco_pheromoneEvaporationRate.getDouble()*100)) + "_g" + ((int) (100*aco_factorImportanceOfPheromones.getDouble())); 
		stat_objFunction.printToFile(new File (baseFileName + "_objFunc.txt"));
		stat_pheromone_ew.printToFile(new File (baseFileName + "_pheromones_ew.txt"));

		IPUtils.setECMPForwardingRulesFromLinkWeights(netPlan, bestSol);

		System.out.println("Ok! Best solution OF: " + bestObjFunction);
		return "Ok! Best OF: " + bestObjFunction;
	}

	@Override
	public String getDescription()
	{
		return "Given a set of nodes, links and offered traffic, this algorithm assumes that the nodes are IP routers running the OSPF protocol (applying ECMP rules) for routing it. The algorithm searches for the set of link weights that optimize the routing. In particular, the target is minimizing a congestion metric computed as a function of both the worst-case link utilization and the average link utilization. The algorithm is based on applying an ant-colony optimization (ACO) heuristic approach.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	private void antWeightChoice (Link e , DoubleMatrix1D currentSol , int maxLinkWeight , int differenceInWeightToBeNeighbors , double [] pheromone_w , double factorImportanceOfPheromones , Random rng)
	{
		/* Sum the probabilities coming from the pheromones */
		double [] accumProbabilities = new double [pheromone_w.length];
		double accumProbabilitySum = 0;
		for (int w = 0 ; w < accumProbabilities.length ; w ++) { accumProbabilities [w] = pheromone_w [w] * factorImportanceOfPheromones; accumProbabilitySum += accumProbabilities [w]; } 
		
		if (factorImportanceOfPheromones != 1)
		{
			Pair<double [],int []> pair = ospfEngine.computeSolutionsVaryingLinkWeight (e , currentSol , null , differenceInWeightToBeNeighbors);
			final double [] objFunctions = pair.getFirst();
			final int [] weights = pair.getSecond();
			
			for (int cont = 0 ; cont < objFunctions.length ; cont ++)
			{
				final int w = weights [cont];
				final double greedyObjFunction = objFunctions [cont];
				final double importanceToSum = (1/greedyObjFunction) * (1-factorImportanceOfPheromones); 
				accumProbabilities [w-1] += importanceToSum;
				accumProbabilitySum += importanceToSum;
			}
		}
		currentSol.set(e.getIndex (), 1.0 + (double) sampleUniformDistribution (accumProbabilities , accumProbabilitySum , rng));
		return ;
	}

	/* Receives a vector with values proportional to the probabilities p[s] of each option s. Returns the index of the sample chosen. 
	 * Each sample s has a probability to be chosen proportional to p[s] */
	private int sampleUniformDistribution (double [] p , double totalSumProbabilities , Random r)
	{
		final double comp = r.nextDouble() * totalSumProbabilities;
		double accumValue = 0;
		
		double checkTotalSum = 0; for (int cont = 0 ; cont < p.length ; cont ++) checkTotalSum += p [cont];
		if (Math.abs(checkTotalSum - totalSumProbabilities) > 1E-3) throw new RuntimeException ("Bad"); 
		for (int cont = 0 ; cont < p.length ; cont ++)
			{ accumValue += p [cont]; if (accumValue >= comp) return cont; }
		return p.length-1;
	}

	private double [][] copyOf (double [][] x) 
	{
		double [][] res = new double [x.length][]; for (int c1 = 0 ; c1 < x.length ; c1 ++) res [c1] = Arrays.copyOf(x[c1], x[c1].length); return res;
	}
	private double [] copyOf (double [] x , int numValuesCopy , int finalSize) 
	{
		double [] res = new double [finalSize]; for (int c1 = 0 ; c1 < numValuesCopy ; c1 ++) res [c1] = x [c1]; return res;
	}
}