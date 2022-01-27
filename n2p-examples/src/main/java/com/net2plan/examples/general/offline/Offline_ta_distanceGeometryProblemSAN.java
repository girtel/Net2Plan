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



package com.net2plan.examples.general.offline;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

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
public class Offline_ta_distanceGeometryProblemSAN implements IAlgorithm
{
	private int numIterOuterLoop;

	private InputParameter distanceStep = new InputParameter ("distanceStep", (double) 0.25 , "The step to be used in moving the points" , 0 , false , Double.MAX_VALUE , true);
	
//	private InputParameter san_worseCaseObjFunctionWorsening = new InputParameter ("san_worseCaseObjFunctionWorsening", (double) 0.25 , "Objective function worsening in a transition considered as worse case for initial temperature computation" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter san_geometricTemperatureReductionFactor = new InputParameter ("san_geometricTemperatureReductionFactor", (double) 0.8 , "Geometric decrease factor of the temperature" , 0 , true , 1 , true);
	private InputParameter san_freezingProbabilityThreshold = new InputParameter ("san_freezingProbabilityThreshold", (double) 0.01 , "If the fraction of iterations with a jump, in the inner loop, is below this, the system is reheated (temperature to the initial value). If not, decreased geometrically" , 0 , true , 1 , true);
	private InputParameter san_initialAcceptanceProbability = new InputParameter ("san_initialAcceptanceProbability", (double) 0.5 , "Acceptance probability of the worst case transition: to compute the initial temperature" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter san_maxNumIterationsInnerLoop = new InputParameter ("san_maxNumIterationsInnerLoop", (int) 1000 , "Number of iterations of the inner loop (all with the same temperature)" , 1 , Integer.MAX_VALUE);
	private InputParameter algorithm_randomSeed = new InputParameter ("algorithm_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter algorithm_maxExecutionTimeInSeconds = new InputParameter ("algorithm_maxExecutionTimeInSeconds", (double) 5.0 , "Algorithm maximum running time in seconds" , 0 , false , Double.MAX_VALUE , true);
	
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		/* Basic checks */
		final int N = netPlan.getNumberOfNodes();
		final int E = netPlan.getNumberOfLinks ();
		if (N == 0 || E == 0) throw new Net2PlanException("The input design must have nodes and links");

		final long algorithmInitialtime = System.nanoTime();
		final long algorithmEndtime = algorithmInitialtime + (long) (algorithm_maxExecutionTimeInSeconds.getDouble() * 1E9);
		
		/* Compute initial solution */ 
		Random rng = new Random (algorithm_randomSeed.getLong ());
		this.numIterOuterLoop = 0;
//		final Function<NetPlan,Double> getCost = np -> np.getLinks ().stream ().mapToDouble (e-> np.getNodePairEuclideanDistance (e.getOriginNode () , e.getDestinationNode ()) , e.getLengthInKm ()).max ().orElese (0.0);
//		final Function<NetPlan,Double> getCost = np -> np.getLinks ().stream ().mapToDouble (e-> Math.pow (np.getNodePairEuclideanDistance (e.getOriginNode () , e.getDestinationNode ()) - e.getLengthInKm () , 2)).sum ();
		final Function<NetPlan,Double> avLinkLength = np -> np.getLinks ().stream ().mapToDouble (e-> e.getLengthInKm()).sum () / E;
		final Function<NetPlan,Double> sumAbsError = np -> np.getLinks ().stream ().mapToDouble (e-> Math.abs (np.getNodePairEuclideanDistance (e.getOriginNode () , e.getDestinationNode ()) - e.getLengthInKm ())).sum ();
		final Function<NetPlan,Double> getCost = sumAbsError;
		final int maxNodeDegree = netPlan.getNodes().stream().mapToInt (n->  Math.max(n.getOutgoingLinks().size() , n.getIncomingLinks().size())).max ().orElse (0);
		final double maxChangeInCostIfOneNodeMoves = distanceStep.getDouble() * Math.sqrt(2) * maxNodeDegree;
		
		/* Compute the initial and end temperatures, and the geometric temperature reduction factor */
		final double initialTemperature = -maxChangeInCostIfOneNodeMoves / Math.log(san_initialAcceptanceProbability.getDouble ());

		System.out.println("initialTemperature: " + initialTemperature + ", geometricReductionFactor: " + san_geometricTemperatureReductionFactor.getDouble ());
		
		double currentTemperature = initialTemperature;
		NetPlan bestSol = netPlan.copy ();
		double bestObjFunction = getCost.apply(bestSol);
		double currentObjFunction = bestObjFunction;
		while (System.nanoTime() < algorithmEndtime)
		{
			numIterOuterLoop ++;
			int numFrozenJumps = 0;
			for (int innerIt = 0 ; innerIt < san_maxNumIterationsInnerLoop.getInt () ; innerIt ++)
			{
				final Node neighborDifferentNode = netPlan.getNode (rng.nextInt(N));
				final Point2D oldPosition = new Point2D.Double(neighborDifferentNode.getXYPositionMap().getX() , neighborDifferentNode.getXYPositionMap().getY()); 
				final boolean moveRight = rng.nextBoolean();
				final boolean moveUp = rng.nextBoolean();
				final Point2D newPosition = new Point2D.Double(neighborDifferentNode.getXYPositionMap().getX() + (moveRight? distanceStep.getDouble() : -distanceStep.getDouble()) , 
						neighborDifferentNode.getXYPositionMap().getY() + (moveUp? distanceStep.getDouble() : -distanceStep.getDouble()));
				neighborDifferentNode.setXYPositionMap(newPosition);
				final double neighborObjFunction = getCost.apply(netPlan);
				
				if ((neighborObjFunction < currentObjFunction) || (rng.nextDouble() < Math.exp(-(neighborObjFunction - currentObjFunction)/currentTemperature)))
				{
					/* If this jump does not change objFunction, consider it a frozen one */
					if (currentObjFunction == neighborObjFunction) numFrozenJumps ++;
					/* jump to the neighbor solution */
					currentObjFunction = neighborObjFunction;
					if (neighborObjFunction < bestObjFunction) { bestObjFunction = neighborObjFunction; bestSol = netPlan.copy (); /* System.out.println("Improving solution (temp: " + currentTemperature + ", objFunction: "+  bestObjFunction); */ }
				}
				else
				{
					neighborDifferentNode.setXYPositionMap(oldPosition);
					numFrozenJumps ++;
				}
				final double currentTimeInSecs = (System.nanoTime() - algorithmInitialtime) * 1e-9;
			}
			if (((double) numFrozenJumps)/san_maxNumIterationsInnerLoop.getInt () > 1-san_freezingProbabilityThreshold.getDouble ())
				{ currentTemperature = initialTemperature;  System.out.println("Reheat the system.");  } // reheat the system  
			else
				{ currentTemperature *= san_geometricTemperatureReductionFactor.getDouble(); /* System.out.println("Decrease temperature."); */ } // decrease temperature
		}
		
		netPlan.assignFrom(bestSol);
		final String message = "Average per link absolute error :" + sumAbsError.apply(netPlan)/E + ", average link length: " + avLinkLength.apply(netPlan); 
		System.out.println("Ok! " + message);
		return "Ok! " + message;
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