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


import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.libraries.IPUtils;
import com.net2plan.utils.Constants.OrderingType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Constants.SearchType;
import com.net2plan.utils.*;

import java.io.File;
import java.util.*;

/**
 * Searches for the OSPF link weights that minimize a measure of congestion, using an evolutionary algorithm (genetic algorithm) heuristic
 * 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec12_9_ospfWeightEA.m">{@code fig_sec12_9_ospfWeightEA.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * @net2plan.description
 * @net2plan.keywords IP/OSPF, Flow assignment (FA), Evolutionary algorithm (EA)
 * @net2plan.ocnbooksections Section 12.9
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_fa_ospfWeightOptimization_EA implements IAlgorithm
{
	private OSPFHeuristicUtils ospfEngine;
	private NetPlan netPlan;
	private List<DoubleMatrix1D> population;
	private double[] costs;

	private TimeTrace stat_objFunction;
	private TimeTrace stat_avEntropy;
	private int E;

	private InputParameter ospf_maxLinkWeight = new InputParameter ("ospf_maxLinkWeight", (int) 16 , "OSPF link weights are constrained to be integers between 1 and this parameter" , 1 , Integer.MAX_VALUE);
	private InputParameter algorithm_randomSeed = new InputParameter ("algorithm_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter algorithm_outputFileNameRoot = new InputParameter ("algorithm_outputFileNameRoot", "ospfWeghtOptimization_ea" , "Root of the file name to be used in the output files. If blank, no output");
	private InputParameter ospf_weightOfMaxUtilizationInObjectiveFunction = new InputParameter ("ospf_weightOfMaxUtilizationInObjectiveFunction", (double) 0.9 , "Objective function is this factor multiplied by maximum link utilization, plus 1 minus this factor by average link utilization" , 0 , true , 1 , true);
	private InputParameter algorithm_maxExecutionTimeInSeconds = new InputParameter ("algorithm_maxExecutionTimeInSeconds", (double) 300 , "Algorithm maximum running time in seconds" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter ea_maxNumIterations = new InputParameter ("ea_maxNumIterations", (int) 100 , "Maximum number of iterations" , 1 , Integer.MAX_VALUE);
	private InputParameter ea_populationSize = new InputParameter ("ea_populationSize", (int) 1000 , "Number of elements in the population" , 1 , Integer.MAX_VALUE);
	private InputParameter ea_offSpringSize = new InputParameter ("ea_offSpringSize", (int) 200 , "Number of childs in the offspring every generation" , 1 , Integer.MAX_VALUE);
	private InputParameter ea_fractionParentsChosenRandomly = new InputParameter ("ea_fractionParentsChosenRandomly", (double) 0.1 , "Fraction of the parents that are selected randomly for creating the offspring" , 0 , true , 1 , true);

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		/* Basic checks */
		final int N = netPlan.getNumberOfNodes();
		this.E = netPlan.getNumberOfLinks();
		if (N == 0) throw new Net2PlanException("The input design must have nodes");
		netPlan.removeAllUnicastRoutingInformation();
		netPlan.setRoutingType (RoutingType.HOP_BY_HOP_ROUTING);

		/* Initialize some variables */
		if (ea_offSpringSize.getInt () > ea_populationSize.getInt () / 2.0) throw new Net2PlanException("The offspring size cannot exceed the population size divided by two");

		
		Random rng = new Random (algorithm_randomSeed.getLong());
		this.netPlan = netPlan;
		this.E = netPlan.getNumberOfLinks();
		this.ospfEngine = new OSPFHeuristicUtils(netPlan, ospf_maxLinkWeight.getInt (), ospf_weightOfMaxUtilizationInObjectiveFunction.getDouble(), rng);
		final long algorithmInitialtime = System.nanoTime();
		final long algorithmEndtime = algorithmInitialtime + (long) (algorithm_maxExecutionTimeInSeconds.getDouble() * 1E9);
		
		this.stat_objFunction = new TimeTrace ();
		this.stat_avEntropy = new TimeTrace();
		
		DoubleMatrix1D bestSol = null;
		double bestObjFunction = Double.MAX_VALUE;

		/* Generate the initial population */
		generateInitialSolutions(ea_populationSize.getInt ());

		/* Update the best solution found so far */
		final int bestSolutionId = DoubleUtils.maxIndexes(costs, SearchType.FIRST)[0];
		bestObjFunction = costs[bestSolutionId];
		bestSol = population.get(bestSolutionId).copy ();
		System.out.println("Initial population. Best solution cost: " + bestObjFunction);

		/* Evolve: one iteration per generation */
		int iterationCounter = 0;
		while ((System.nanoTime() < algorithmEndtime) && (iterationCounter < ea_maxNumIterations.getInt ()))
		{
			LinkedList<Integer> parents = operator_parentSelection(ea_offSpringSize.getInt () , ea_fractionParentsChosenRandomly.getDouble () , rng);
			List<DoubleMatrix1D> offspring = operator_crossover(parents , rng);
			this.operator_mutate(offspring , rng , ospf_maxLinkWeight.getInt ());
			int indexBestSolution = this.operator_select(offspring);
			if (costs[indexBestSolution] < bestObjFunction)
			{
				bestObjFunction = costs[indexBestSolution];
				bestSol = this.population.get(indexBestSolution).copy ();
			}
			
			//System.out.println("Iteration: " + iterationCounter + ". Best solution cost: " + bestObjFunction);
			
			final double runningTimeSecs = (System.nanoTime() - algorithmInitialtime) * 1E-9;
			stat_objFunction.add (runningTimeSecs , costs);
			stat_avEntropy.add (runningTimeSecs , computeAverageEntropy ());
			iterationCounter++;
		}

		/* Save the best solution found into the netPlan object */
		IPUtils.setECMPForwardingRulesFromLinkWeights(netPlan, bestSol);

		stat_objFunction.printToFile(new File (algorithm_outputFileNameRoot.getString () + "_cong.txt"));
		stat_avEntropy.printToFile(new File (algorithm_outputFileNameRoot.getString () + "_entropy.txt"));
		
		System.out.println("Ok! Best solution OF: " + bestObjFunction + ", number generations: " + iterationCounter);
		return "Ok! Best solution OF: " + bestObjFunction + ", number generations: " + iterationCounter;
	}

	@Override
	public String getDescription()
	{
		return "Given a set of nodes, links and offered traffic, this algorithm assumes that the nodes are IP routers running the OSPF protocol (applying ECMP rules) for routing it. The algorithm searches for the set of link weights that optimize the routing. In particular, the target is minimizing a congestion metric computed as a function of both the worst-case link utilization and the average link utilization. The algorithm is based on applying an evolutionary algorithm (EA) or genetic algorithm heuristic approach.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
	
	private void generateInitialSolutions(int ea_populationSize)
	{
		population = new ArrayList<DoubleMatrix1D>(ea_populationSize);
		costs = new double[ea_populationSize];

		for (int cont = 0; cont < ea_populationSize; cont++)
		{
			Pair<DoubleMatrix1D,Double> p = ospfEngine.getInitialSolution ("random");
			population.add(p.getFirst());
			costs[cont] = p.getSecond();
		}
	}

	private LinkedList<Integer> operator_parentSelection(int ea_offspringSize , double ea_fractionChosenRandomly , Random rng)
	{
		final int numParentsToChoose = 2 * ea_offspringSize;
		final int numParentsChosenRandomly = (int) Math.floor(numParentsToChoose * ea_fractionChosenRandomly);
		final int numParentsChosenFromCost = numParentsToChoose - numParentsChosenRandomly;

		/* Initialize the list to be returned */
		LinkedList<Integer> chosenParents = new LinkedList<Integer>();

		int [] sortedPopulationIds = DoubleUtils.sortIndexes(costs, OrderingType.ASCENDING); 
		
		/* Choose the best solutions as parents: as many as numParentsChosenFromCost */
		for (int cont = 0; cont < numParentsChosenFromCost; cont++)
			chosenParents.add(sortedPopulationIds [cont]);

		/* The rest of the parents (numParentsChosenRandomly) are chosen randomly among all. Parents can be repeated */
		for (int cont = 0; cont < numParentsChosenRandomly; cont++)
			chosenParents.add(rng.nextInt(this.population.size()));

		return chosenParents;
	}

	private List<DoubleMatrix1D> operator_crossover(LinkedList<Integer> parents , Random rng)
	{
		/* The offspring to be returned */
		List<DoubleMatrix1D> offspring = new ArrayList<DoubleMatrix1D>(parents.size() / 2);

		/* Shuffle randomly the parent selection. Then, couples are formed randomly */
		Collections.shuffle(parents);

		/* Two consecutive parents are a couple */
		while (parents.size() >= 2)
		{
			final int firstParentId = parents.poll();
			final int secondParentId = parents.poll();

			final DoubleMatrix1D firstParent = population.get(firstParentId);
			final DoubleMatrix1D secondParent = population.get(secondParentId);

			/* The descendant chooses randomly for each link, the link weight from any of the two parents */
			final DoubleMatrix1D descendant = DoubleFactory1D.dense.make (E);
			for (Link link : netPlan.getLinks ()) descendant.set(link.getIndex (), rng.nextBoolean() ? firstParent.get(link.getIndex ()) : secondParent.get(link.getIndex ()));
			offspring.add(descendant);
		}
		
		return offspring;
	}

	private void operator_mutate(List<DoubleMatrix1D> offspring , Random rng , int maxLinkWeight)
	{
		for (DoubleMatrix1D solution : offspring)
		{
			final int e = rng.nextInt(netPlan.getNumberOfLinks());
			solution.set(e, 1.0 + rng.nextInt(maxLinkWeight));
		}
	}

	private int operator_select(List<DoubleMatrix1D> offspring)
	{
		/* Compute the costs of the offspring */
		final int ea_populationSize = costs.length;
		double [] combinedCosts = Arrays.copyOf(costs, ea_populationSize + offspring.size());
		population.addAll(offspring);
		int counter = ea_populationSize;
		for (DoubleMatrix1D descendant : offspring)
			combinedCosts[counter ++] = ospfEngine.computeObjectiveFunction(descendant).getFirst();

		int [] sortedPopulationIds = DoubleUtils.sortIndexes(combinedCosts, OrderingType.ASCENDING); 

		this.costs = new double [ea_populationSize];
		ArrayList<DoubleMatrix1D> newPopulation = new ArrayList<DoubleMatrix1D>(ea_populationSize);
		for (int cont = 0 ; cont < ea_populationSize ; cont ++)
		{
			final int indexInCombinedOffspringAndPopulation =  sortedPopulationIds[cont];
			costs [cont] = combinedCosts [indexInCombinedOffspringAndPopulation];
			newPopulation.add(population.get(indexInCombinedOffspringAndPopulation));
		}
		this.population = newPopulation;
		return 0; // return the best solution 
	}

	
	private double computeAverageEntropy ()
	{
		double freq_ew [][] = new double [E][ospf_maxLinkWeight.getInt ()]; // number of occurrencies
		double sum_e [] = new double [E]; // sum of occurrencies per link
		//private List<Map<Long, Double>> population;
		for (DoubleMatrix1D sol : population)
		{
			for (Link e : netPlan.getLinks ())
			{
				final int eIndex = e.getIndex ();
				final int w = (int) sol.get(eIndex) - 1;
				freq_ew [eIndex][w] ++;
				sum_e [eIndex] ++;
			}
		}
		final double convLogFactor = 1 / Math.log(2);
		double avEntropy = 0;
		for (int eIndex = 0 ; eIndex < E ; eIndex ++)
		{
			double entropy_e = 0;
			for (int w = 0 ; w < ospf_maxLinkWeight.getInt () ; w ++)
			{
				if (sum_e [eIndex] > 0) { final double val = freq_ew [eIndex][w] / sum_e [eIndex]; entropy_e -= (val == 0)? 0 : val * Math.log(val) * convLogFactor; }
			}
			avEntropy += entropy_e;
		}
		return avEntropy / E;
	}
}