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
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.libraries.IPUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Quadruple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

class OSPFHeuristicUtils 
{
	final NetPlan netPlan;
	final int maxLinkWeight;
	final double weightOfMaxUtilizationInObjectiveFunction;
	final Random rng;
	
	OSPFHeuristicUtils (NetPlan netPlan , int maxLinkWeight , double weightOfMaxUtilizationInObjectiveFunction , Random rng)
	{
		this.netPlan = netPlan;
		this.maxLinkWeight = maxLinkWeight;
		this.weightOfMaxUtilizationInObjectiveFunction = weightOfMaxUtilizationInObjectiveFunction;
		this.rng = rng;
	}
	
	Pair<DoubleMatrix1D,Double> getInitialSolution (String initializationType)
	{
		DoubleMatrix1D currentSol = DoubleFactory1D.dense.make (netPlan.getNumberOfLinks ());
		if (initializationType.equalsIgnoreCase("random"))
			for (Link e : netPlan.getLinks())
				currentSol.set(e.getIndex (), rng.nextInt(maxLinkWeight) + 1.0);
		else if (initializationType.equalsIgnoreCase("ones"))
			for (Link e : netPlan.getLinks())
				currentSol.set(e.getIndex (), 1.0);
		else
			throw new Net2PlanException ("Non recognized initialization type");
		double currentObjFunction = computeObjectiveFunction (currentSol).getFirst();
		return Pair.of(currentSol , currentObjFunction);
	}	

	Pair<Double,Integer> localSearch (DoubleMatrix1D currentSol , double currentObjFunction , int differenceInWeightToBeNeighbors , boolean isFirstFit)
	{
		List<Link> shuffledLinks = new ArrayList<Link> (netPlan.getLinks()); 
		Collections.shuffle(shuffledLinks ,rng);
		int numObjFunctionEvaluations = 0;
		do
		{
			Link bestNeighborLink = null;
			int bestNeighborWeight = -1;
			double bestNeighborObjFunction = currentObjFunction;

			/* Neighbors differing in one link */
			for (Link e1 : shuffledLinks)
			{
				final int currentWeight1 = (int) currentSol.get(e1.getIndex ());
				final int initialWeightNeighborhood = Math.max(1, currentWeight1 - differenceInWeightToBeNeighbors);
				final int endWeightNeighborhood = Math.min(maxLinkWeight, currentWeight1 + differenceInWeightToBeNeighbors);
				for (int w1 = initialWeightNeighborhood ; w1 <= endWeightNeighborhood ; w1 ++)
				{
					if (w1 == currentWeight1) continue;
					if (Math.abs(w1 - currentWeight1) > differenceInWeightToBeNeighbors) throw new RuntimeException ("Bad");
					currentSol.set(e1.getIndex (), (double) w1);
					Pair<Double,DoubleMatrix1D> neighborEvaluation = computeObjectiveFunction(currentSol); 
					currentSol.set(e1.getIndex (), (double) currentWeight1);
					final double neighborObjFunction = neighborEvaluation.getFirst();
					final DoubleMatrix1D y_e = neighborEvaluation.getSecond();
					numObjFunctionEvaluations ++;
					if (neighborObjFunction < bestNeighborObjFunction)
					{
						bestNeighborLink = e1; bestNeighborWeight = w1; bestNeighborObjFunction = neighborObjFunction;
						if (isFirstFit) break;
					}
					//if (y_e.get(e1) <= 0) { System.out.println("No need to increase link weight upper than " + w1); break;} // The link does not carry traffic => increasing the weight does not change the routing
					if (y_e.get(e1.getIndex ()) <= 0) break; // The link does not carry traffic => increasing the weight does not change the routing
				}
				if ((bestNeighborLink != null) && isFirstFit) break;
			}
			if (bestNeighborLink == null) break;
			currentSol.set(bestNeighborLink.getIndex (), (double) bestNeighborWeight);
			currentObjFunction = bestNeighborObjFunction;
		} while (true); // algorithm ends
		
		return Pair.of(currentObjFunction,numObjFunctionEvaluations);
	}

	Pair<double [],int []> computeSolutionsVaryingLinkWeight (Link e , DoubleMatrix1D currentSol , Pair<Double,DoubleMatrix1D> currentSolEvaluation , int differenceInWeightToBeNeighbors)
	{
		final int originalWeight = (int) currentSol.get(e.getIndex ());
		final int initialWeightNeighborhood = Math.max(1, originalWeight - differenceInWeightToBeNeighbors);
		final int endWeightNeighborhood = Math.min(maxLinkWeight, originalWeight + differenceInWeightToBeNeighbors);
		final int numWeightsNeighborhood = endWeightNeighborhood - initialWeightNeighborhood + 1;  
		
		int [] wIds = new int [numWeightsNeighborhood];
		double [] objFunc = new double [numWeightsNeighborhood];
		
		Pair<Double,DoubleMatrix1D> previousNeighborEvaluation= null; //(initialWeightNeighborhood == originalWeight)? currentSolEvaluation : null;
		Pair<Double,DoubleMatrix1D> neighborEvaluation = null;
		for (int cont = 0 ; cont < numWeightsNeighborhood ; cont ++)
		{
			final int w1 = initialWeightNeighborhood + cont;
			if (Math.abs(w1 - originalWeight) > differenceInWeightToBeNeighbors) throw new RuntimeException ("Bad");
			if  ((w1 == originalWeight) && (currentSolEvaluation != null))
				neighborEvaluation = currentSolEvaluation; 
			else
				neighborEvaluation = computeObjectiveFunctionOfNeighborSolution (currentSol, previousNeighborEvaluation , e , w1 , false);
			
			wIds [cont] = w1;
			objFunc [cont] = neighborEvaluation.getFirst();
			previousNeighborEvaluation = neighborEvaluation;
			
//			if (linkDoesNotCarryTraffic)
//				{ objFunc [cont] = objFunc [cont-1]; continue; }
//			if (w1 == originalWeight)
//				{ objFunc [cont] = currentObjFunction; continue; }
//			currentSol.put(e, (double) w1);
//			Pair<Double,Map<Long,Double>> pair = computeObjectiveFunction(currentSol);
//			objFunc [cont] = pair.getFirst();
//			linkDoesNotCarryTraffic = (pair.getSecond().get(e) <= 0);
		}
		currentSol.set(e.getIndex (), (double) originalWeight);
		return Pair.of(objFunc , wIds);
	}
	

	Pair<Double,DoubleMatrix1D> computeObjectiveFunctionOfNeighborSolution (DoubleMatrix1D initialSol , Pair<Double,DoubleMatrix1D> initialSolEvaluation , Link changingLink , int newWeight , boolean keepInitialSolutionUnchanged)
	{
		final double originalWeight = initialSol.get(changingLink.getIndex ());
		if ((originalWeight == newWeight) && (initialSolEvaluation != null)) return initialSolEvaluation;
		if ((newWeight > originalWeight) && (initialSolEvaluation != null)) 
			if (initialSolEvaluation.getSecond().get(changingLink.getIndex ()) <= 0) return initialSolEvaluation;
		initialSol.set(changingLink.getIndex () , (double) newWeight);
		Pair<Double,DoubleMatrix1D> eval = computeObjectiveFunction(initialSol);
		if (keepInitialSolutionUnchanged) initialSol.set(changingLink.getIndex () , originalWeight);
		return eval;
	}
	
	Pair<Double,DoubleMatrix1D> computeObjectiveFunction (DoubleMatrix1D sol)
	{
		Quadruple<DoubleMatrix2D, DoubleMatrix2D, DoubleMatrix1D, DoubleMatrix1D> q = IPUtils.computeCarriedTrafficFromIGPWeights(netPlan, sol);
		DoubleMatrix1D y_e = q.getFourth();
		double congestion = 0;
		double accumUtilization = 0;
		for (Link e : netPlan.getLinks ())
		{
			final double linkTraf = y_e.get(e.getIndex ());
			final double u_e = e.getCapacity();
			final double utilization = (linkTraf == 0)? 0 : (u_e == 0)? Double.MAX_VALUE : linkTraf / u_e;
			accumUtilization += utilization;
			congestion = Math.max(congestion, utilization);
		}
		final double objFunc = weightOfMaxUtilizationInObjectiveFunction * congestion + (1-weightOfMaxUtilizationInObjectiveFunction) * accumUtilization / netPlan.getNumberOfLinks();
		return Pair.of(objFunc, y_e);
	}
}
