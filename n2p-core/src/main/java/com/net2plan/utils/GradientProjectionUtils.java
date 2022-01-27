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


package com.net2plan.utils;


import cern.colt.function.tdouble.DoubleDoubleFunction;
import cern.colt.function.tdouble.DoubleFunction;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.utils.Constants.OrderingType;

import java.util.LinkedList;


/**
 * <p>Auxiliary static methods to solve some simple optimization problems (e.g. the projection of a vector in some sets defined with simple linear constraints) by means of relatively 
 * simple algorithms devised by applying the KKT conditions on the problem. Gradient algorithms can use these methods to solve some projections or regularizations.  </p>
 *
 * @author Pablo Pavon-Marino
 * @see <a name='jom'></a><a href='http://www.net2plan.com/jom'>Java Optimization Modeler (JOM) website</a>
 * @see <a name='jgrapht'></a><a href='http://jgrapht.org/'>JGraphT website</a>
 * @see <a name='jung'></a><a href='http://jung.sourceforge.net/'>Java Universal Network/Graph Framework (JUNG) website</a>
 */
public class GradientProjectionUtils 
{
	
	/** Using a simple algorithm, finds the euclidean projection of the input vector {@code x0_k}, to the set:
	 * {@code sum x_k = C, x_k >= 0}.
	 * @param inAndOut_x The input vector {@code x0_k}. The euclidean projection (output of the algorithm) is returned writing in this variable.
	 * @param vectorSum The {@code C} constant.
	 */
	public static void euclideanProjection_sumEquality (DoubleMatrix1D inAndOut_x , double vectorSum)
	{
		euclideanProjection_sumEquality (inAndOut_x , null , vectorSum);
	}

	/** Using a simple algorithm, finds the euclidean projection of the input vector {@code x0_k}, to the set:
	 * {@code sum x_k = C, x_k >= 0}.
	 * @param inAndOut_x The input vector {@code x0_k}. The euclidean projection (output of the algorithm) is returned writing in this variable.
	 * @param coordinatesToConsider The vector {@code x0_k} and {@code x_k} to consider is given by the input vector {@code inAndOut_x}, restricted to these coordinates. If {@code null} all the vector coordinates are selected.
	 * @param vectorSum The {@code C} constant.
	 */
	public static void euclideanProjection_sumEquality (DoubleMatrix1D inAndOut_x , int [] coordinatesToConsider , double vectorSum)
	{
		/* KKT say that 1. if two coordinates > 0 at the end => they shift their value in the same amount */
		/* KKT say that 2. if two coordinates > 0 at the end => the initially higher in traffic has finally more traffic also: preserves order */
		/* Order coordinates from initial higher to initial lower value. Try an increasing number of coordinates being > 0  */

		if (vectorSum == 0) { inAndOut_x.viewSelection (coordinatesToConsider).assign (0); return; } 

		DoubleMatrix1D trueInAndOut_x = inAndOut_x.viewSelection(coordinatesToConsider);
		
		final int K = (int) trueInAndOut_x.size ();
		double [] xArray_0 = trueInAndOut_x.toArray();
		final int [] orderedIndexes = DoubleUtils.sortIndexes(xArray_0, OrderingType.DESCENDING);
		
		double best_distance = Double.MAX_VALUE;
		int best_numberOfCoordGreaterZero = -1;
		double best_quantityToIncrease = -1;
		for (int numCoordsNonZero = 1 ; numCoordsNonZero <= K ; numCoordsNonZero ++)
		{
			double initialSum = 0; for (int cont = 0 ; cont < numCoordsNonZero ; cont ++) initialSum += xArray_0 [orderedIndexes [cont]];
			double quantityToIncreaseToAllNonZero = (vectorSum - initialSum) / numCoordsNonZero; 
			double lowerNonZeroCoordValue = xArray_0 [orderedIndexes [numCoordsNonZero-1]];
			if (lowerNonZeroCoordValue + quantityToIncreaseToAllNonZero < 0) continue; // this solution is not feasible, one more non-zero coordinate
			double euclideanDistance = numCoordsNonZero * Math.pow(quantityToIncreaseToAllNonZero,2);
			for (int cont = numCoordsNonZero ; cont < K ; cont ++)
				euclideanDistance += Math.pow(xArray_0 [orderedIndexes [cont]] , 2);
			if (euclideanDistance < best_distance) { best_distance = euclideanDistance; best_numberOfCoordGreaterZero = numCoordsNonZero; best_quantityToIncrease = quantityToIncreaseToAllNonZero; }
		}

		/* Last coordinates are zero */
		for (int cont = best_numberOfCoordGreaterZero ; cont < K ; cont ++)
			trueInAndOut_x.set(orderedIndexes [cont], 0.0);
		double sum = 0;
		for (int cont = 0; cont < best_numberOfCoordGreaterZero ; cont ++)
		{
			final int k = orderedIndexes [cont];
			final double currentValue = xArray_0 [k];
			trueInAndOut_x.set(k , currentValue + best_quantityToIncrease);
			sum += currentValue + best_quantityToIncrease;
		}

		if (Math.abs(vectorSum - sum) > 1E-3) throw new RuntimeException ("Bad");		
	}

	
	/** Using a simple algorithm, finds the euclidean projection of the input vector {@code x0_k}, to the set:
	 * {@code sum x_k <= C, x_k >= xMin}.
	 * @param inAndOut_x The input vector {@code x0_k}. The euclidean projection (output of the algorithm) is returned writing in this variable.
	 * @param xMin The {@code xMin} constant.
	 * @param vectorSum The {@code C} constant.
	 */
	public static void euclideanProjection_sumInequality (DoubleMatrix1D inAndOut_x , final double xMin , final double vectorSum)
	{
		euclideanProjection_sumInequality (inAndOut_x , null , xMin , vectorSum);
	}

	/** Using a simple algorithm, finds the euclidean projection of the input vector {@code x0_k}, to the set:
	 * {@code sum x_k <= C, x_k >= xMin}.
	 * @param inAndOut_x The input vector {@code x0_k}. The euclidean projection (output of the algorithm) is returned writing in this variable.
	 * @param coordinatesToConsider The vector {@code x0_k} and {@code x_k} to consider is given by the input vector {@code inAndOut_x}, restricted to these coordinates. If {@code null} all the vector coordinates are selected.
	 * @param xMin The {@code xMin} constant.
	 * @param vectorSum The {@code C} constant.
	 */
	public static void euclideanProjection_sumInequality (DoubleMatrix1D inAndOut_x , int [] coordinatesToConsider ,  final double xMin , final double vectorSum)
	{
		if (coordinatesToConsider != null) if (coordinatesToConsider.length == 0) return;
		DoubleMatrix1D trueInAndOut_x = inAndOut_x.viewSelection(coordinatesToConsider);
		final int K = (int) trueInAndOut_x.size ();
		/* any links with negative p_e, are set p_e=0, any klinks with p_e > 1000, set p_e=1000 (infinities) */
		trueInAndOut_x.assign(new DoubleFunction() { public double apply(double x) { if (x < xMin) return xMin; if (x > 1E3) return vectorSum*1E3; return x; } });
//		for (long k : keysToConsider) if (inAndOut_x.get(k) < xMin) inAndOut_x.put(k, xMin); else if (inAndOut_x.get(k) > 1E3) inAndOut_x.put(k, vectorSum * 1000.0);  

		//log.println("p_e 2:" + LongUtils.selectEntries(p_e, outLinks_n));
		int counter = 0;
		while (true)
		{
			counter ++; if (counter > trueInAndOut_x.size() + 1) throw new RuntimeException ("WTF!!");
			LinkedList<Integer> coordsNotInXmin = new LinkedList <Integer> ();
			double sumCurrentSolution = 0;
			double minValueGreaterThanXMin = Double.MAX_VALUE;
			for (int k = 0 ; k < K ; k ++) 
			{
				final double this_xk = trueInAndOut_x.get(k); 
				sumCurrentSolution += this_xk; 
				if (this_xk > xMin + 1E-3) { coordsNotInXmin.add (k); minValueGreaterThanXMin = (this_xk < minValueGreaterThanXMin)? this_xk : minValueGreaterThanXMin; }
			}
			/* If sums less than one => we have finished */
			if (sumCurrentSolution <= vectorSum + 1E-3) break;
			/* we have to reduce the same amount to all non-zero coordinates, until */
			final double excess = sumCurrentSolution - vectorSum;
			final double numberCoordinatesToReduce = coordsNotInXmin.size();
			final double amountToReduceEachCoordinate = Math.min (excess / numberCoordinatesToReduce , minValueGreaterThanXMin - xMin);
			if (amountToReduceEachCoordinate <= 0) throw new RuntimeException ("WTF");
			
			for (int k : coordsNotInXmin) { double new_xk = trueInAndOut_x.get(k) - amountToReduceEachCoordinate; if (Math.abs(new_xk - xMin) < 1E-3) new_xk = xMin; trueInAndOut_x.set(k , new_xk);  }
		}
	}

	/** Projects a value {@code x_0} into an interval [{@code xMin , xMax}].
	 * @param x_k The input value to project
	 * @param xMin The interval lower value
	 * @param xMax The interval upper value
	 * @return the projected value. Equivalent to {@code max(xMin, min (xMax,x_k))}
	 */
	public static double euclideanProjection_boxLike (double x_k , double xMin , double xMax)
	{
		return Math.min(xMax, Math.max(xMin, x_k));
	}

	/** Projects a value {@code x_0} into an interval [{@code xMin , inf}].
	 * @param x_k The input value to project
	 * @param xMin The interval lower value
	 * @return the projected value. Equivalent to {@code max(xMin, ,x_k)}
	 */
	public static double euclideanProjection_boxLikeLowerBound (double x_k , double xMin)
	{
		return Math.max(xMin, x_k);
	}

	/** Projects a value {@code x_0} into an interval [{@code inf , xMax}].
	 * @param x_k The input value to project
	 * @param xMax The interval upper value
	 * @return the projected value. Equivalent to {@code min(xMax, ,x_k)}
	 */
	public static double euclideanProjection_boxLikeUpperBound (double x_k , double xMax)
	{
		return Math.min(xMax, x_k);
	}
	

	/** If all the selected coordinates in {@code x_k} differ in absolute value less than {@code maxAbsDifference} respect to {@code x_0}, the vector {@code x_k} as it is, is returned. 
	 * If not, all the selected coordinates of {@code x_k} are scaled down by the value which makes the maximum value of {@code |x_k - x_0|} become {@code maxAbsDifference}.
	 * @param x0_k the {@code x0_k} vector
	 * @param inAndOut_x The vector {@code x_k}, used as input. The output is written also here
	 * @param coordinateKeysToConsider The vector {@code x0_k} and {@code x_k} to consider is given by the input vector {@code inAndOut_x}, restricted to these coordinates 
	 * @param maxAbsDifference The maximum absolute difference allowed in the selected coordinates of {@code x_k}, respect to {@code x_0}
	 */
	public static void scaleDown_maxAbsoluteCoordinateChange (DoubleMatrix1D x0_k , DoubleMatrix1D inAndOut_x , int [] coordinateKeysToConsider ,  double maxAbsDifference)
	{
		if (maxAbsDifference <= 0) return; 
		if (coordinateKeysToConsider != null) if (coordinateKeysToConsider.length == 0) return;
		
		DoubleMatrix1D trueInitial_x = x0_k.viewSelection(coordinateKeysToConsider);
		DoubleMatrix1D trueFinalInAndOut_x = inAndOut_x.viewSelection(coordinateKeysToConsider);
		final int K = (int) trueFinalInAndOut_x.size(); if (trueInitial_x.size () != K) throw new Net2PlanException ("Arrays must have the same size");
		double trueMaxAbsChange = 0; for (int k = 0 ; k < K ; k ++) trueMaxAbsChange = Math.max(Math.abs(trueInitial_x.get(k) - trueFinalInAndOut_x.get(k)), trueMaxAbsChange);
		if (trueMaxAbsChange > maxAbsDifference)
		{
			final double scalingFactor = maxAbsDifference / trueMaxAbsChange;
			for (int k = 0 ; k < K ; k ++)
			{
				final double oldValue = trueInitial_x.get(k);
				final double newValue = trueFinalInAndOut_x.get(k);
				trueFinalInAndOut_x.set(k, oldValue + (newValue-oldValue)*scalingFactor);
			}
		}
	}

	/** If {@code x} differs in absolute value less than {@code maxAbsDifference}, {@code x} as it is, is returned. 
	 * If not, {@code x} is scaled down by the value which makes the maximum value of {@code |x - x_0|} become {@code maxAbsDifference}.
	 * @param x0 the {@code x0} value
	 * @param x the {@code x} value
	 * @param maxAbsDifference the maximum absoulute value difference
	 * @return the scaled value
	 */
	public static double scaleDown_maxAbsoluteCoordinateChange (double x0 , double x ,  double maxAbsDifference)
	{
		if (maxAbsDifference <= 0) return x;
		final double absChange = Math.abs(x0 - x); 
		if (absChange <= maxAbsDifference) return x;
		final double scalingFactor = maxAbsDifference / absChange;
		return x0 + (x-x0)*scalingFactor;
	}

	
	/** Using a simple algorithm, finds the euclidean projection of the input vector {@code x0_k}, to the set:
	 * {@code sum x_k <= C, x_k >= xMin_k}.
	 * @param inAndOut_x The input vector {@code x0_k}. The euclidean projection (output of the algorithm) is returned writing in this variable.
	 * @param coordinatesToConsider The vector {@code x0_k} and {@code x_k} to consider is given by the input vector {@code inAndOut_x}, restricted to these coordinates. If {@code null} all the vector coordinates are selected.
	 * @param xMin_k The {@code xMin_k} vector of constant lower bounds.
	 * @param vectorSum The {@code C} constant.
	 */
	public static void euclideanProjection_sumInequality (DoubleMatrix1D inAndOut_x , int [] coordinatesToConsider ,  final DoubleMatrix1D xMin_k , final double vectorSum)
	{
		if (coordinatesToConsider != null) if (coordinatesToConsider.length == 0) return;
		DoubleMatrix1D trueInAndOut_x = inAndOut_x.viewSelection(coordinatesToConsider);
		DoubleMatrix1D trueXMin = xMin_k.viewSelection(coordinatesToConsider);
		final int K = (int) trueInAndOut_x.size ();
		/* any links with negative p_e, are set p_e=0, any klinks with p_e > 1000, set p_e=1000 (infinities) */
		trueInAndOut_x.assign(trueXMin , new DoubleDoubleFunction() { public double apply(double x,double y) { if (x < y) return y; if (x > 1E3) return vectorSum*1E3; return x; } });
		int counter = 0;
		while (true)
		{
			counter ++; if (counter > trueInAndOut_x.size() + 1) throw new RuntimeException ("WTF!!");
			LinkedList<Integer> coordsNotInXmin = new LinkedList <Integer> ();
			double sumCurrentSolution = 0;
			double minDistanceToXMin = Double.MAX_VALUE;
			for (int k = 0 ; k < K ; k ++) 
			{
				final double this_xk = trueInAndOut_x.get(k); 
				final double distanceToXMin = this_xk - trueXMin.get(k); 
				sumCurrentSolution += this_xk; 
				if (distanceToXMin > 1E-3) { coordsNotInXmin.add (k); minDistanceToXMin = (distanceToXMin < minDistanceToXMin)? distanceToXMin : minDistanceToXMin; }
			}
			/* If sums less than one => we have finished */
			if (sumCurrentSolution <= vectorSum + 1E-3) break;
			/* we have to reduce the same amount to all non-zero coordinates, until */
			final double excess = sumCurrentSolution - vectorSum;
			final double numberCoordinatesToReduce = coordsNotInXmin.size();
			final double amountToReduceEachCoordinate = Math.min (excess / numberCoordinatesToReduce , minDistanceToXMin);
			if (amountToReduceEachCoordinate <= 0) throw new RuntimeException ("WTF");
			
			for (int k : coordsNotInXmin) { double new_xk = trueInAndOut_x.get(k) - amountToReduceEachCoordinate; if (Math.abs(new_xk - trueXMin.get(k)) < 1E-3) new_xk = trueXMin.get(k); trueInAndOut_x.set(k , new_xk);  }
		}
	}

//	/* argmin_{sum x_k = vectorSum, x_k >= xMin_k} norm{initial_x - x}_2 . Initial */
//	/* Initial coordinates are taken from inAndOut_x ONLY THE KEYS IN coordinateKeysToConsider.
//	 * Projected coordinate values are stored in the same map */
//	public static void euclideanProjection_nonZeroSumInequality_JOM (Map<Long,Double> inAndOut_x , Set<Long> coordinateKeysToConsider , Map<Long,Double> xMin , double vectorSum)
//	{
//		/* Modify the map so that it is the pojection where all elements sum h_d, and are non-negative */
//		final Set<Long> keysToConsider = (coordinateKeysToConsider == null)? inAndOut_x.keySet() : coordinateKeysToConsider;
//		final int K = keysToConsider.size();
//		double [] xArray_0 = new double [K]; int counter = 0; for (long k : keysToConsider) xArray_0 [counter ++] = inAndOut_x.get(k);
//		double [] xMinArray = new double [K]; counter = 0; for (long k : keysToConsider) xMinArray [counter ++] = xMin.get(k);
//		
//		OptimizationProblem op = new OptimizationProblem();
//		op.addDecisionVariable("x_k", false , new int[] { 1 , K }, xMinArray , DoubleUtils.constantArray(K, vectorSum)); 
//		op.setInputParameter("x_k0", xArray_0 , "row");
//		op.setInputParameter("x_Min", xMinArray , "row");
//		op.setInputParameter("s", vectorSum);
//		
//		/* Set some input parameters */
//		op.setObjectiveFunction("minimize", "sum ((x_k - x_k0)^2)");
//		op.addConstraint("sum(x_k) <= s");
//		
//		/* Call the solver to solve the problem */
//		op.solve("ipopt");
//
//		/* If an optimal solution was not found, quit */
//		if (!op.solutionIsOptimal ()) throw new RuntimeException ("An optimal solution was not found");
//	
//		/* Retrieve the optimum solutions. Convert the bps into Erlangs */
//		double [] x_k_array = op.getPrimalSolution("x_k").to1DArray();
//
//		counter = 0; for (long k : keysToConsider) inAndOut_x.put(k , x_k_array [counter ++]);
//
//		double sum = 0; for (long k : keysToConsider) { sum += inAndOut_x.get(k); if (inAndOut_x.get(k) < xMin.get(k) - 1E-3) throw new RuntimeException ("Bad"); } 
//		if (Math.abs(sum - vectorSum) > 1E-3) throw new RuntimeException ("Bad");
//	}
////	public static void euclideanProjection_nonZeroSumInequality_JOM (Map<Long,Double> inAndOut_x , Map<Long,Double> xMin , double vectorSum)
////	{
////		euclideanProjection_nonZeroSumInequality_JOM (inAndOut_x , null , xMin , vectorSum);
////	}

	
	/** Using a simple algorithm, finds the vector {@code x} which minimizes {@code \sum_k a_k x_k + epsilon * x_l^2}, subject to: {@code x_k >= xMin_k, \sum_k x_k = C}. 
	 * @param a_k The input vector {@code a_k}.
	 * @param coordinatesToConsider The selected coordinates to consider in the vectors {@code a_k} and {@code xMin}. If {@code null}, all the vector coordinates are selected
	 * @param xMin_k The {@code xMin_k} vector of constant lower bounds. If {@code null}, a vector of zeros
	 * @param vectorSum The {@code C} constant.
	 * @param epsilon The {@code epsilon} value
	 * @return The with the solution of the problem
	 */
	public static DoubleMatrix1D regularizedProjection_sumEquality (DoubleMatrix1D a_k , int [] coordinatesToConsider , DoubleMatrix1D xMin_k , double vectorSum , double epsilon)
	{
		if (xMin_k == null) { xMin_k = DoubleFactory1D.dense.make ((int) a_k.size ()); }

		DoubleMatrix1D true_a_k = a_k.viewSelection (coordinatesToConsider);
		DoubleMatrix1D true_xMin = xMin_k.viewSelection (coordinatesToConsider);
		final int K = (int) true_a_k.size ();
		
		/* Sort the coordinates according to minimum a_k */
		double [] w_k_array = new double [K]; //long [] index2IdMap = new long [K]; 
		for (int k = 0 ; k < K ; k ++) { w_k_array [k] = -true_a_k.get(k)/(2*epsilon) - true_xMin.get(k); }
		final int [] sortIndexes_wk = DoubleUtils.sortIndexes(w_k_array, OrderingType.DESCENDING);

		/* If all the coordinates are at a minimum, the pending sum is the one to distribute among the coordinates */
		final double pendingSum = vectorSum - xMin_k.zSum ();
		if (pendingSum < 1E-3) throw new RuntimeException ("The problem has no solution");

		/* Check if the solution is all the coordinates to its minimum */
		if (Math.abs(pendingSum) < 1E-3) { DoubleMatrix1D x_k = DoubleFactory1D.dense.make ((int) a_k.size()); x_k.viewSelection (coordinatesToConsider).assign (true_xMin); return x_k; }

		/* One or more coordinates are not at its minimum. We keep the best */
		double bestSolutionCost = Double.MAX_VALUE;
		DoubleMatrix1D bestSolutionXk = DoubleFactory1D.dense.make ((int) a_k.size ());
		for (int numNonMinCoord = 1 ; numNonMinCoord < K ; numNonMinCoord ++)
		{
			/* Recompute the values of the new x_k */
			double accum = pendingSum; for (int cont = 0 ; cont < numNonMinCoord ; cont ++) accum -= w_k_array [sortIndexes_wk[cont]]; 
			final double piDividedBy2Epsilon = accum / numNonMinCoord;
			
			/* if smaller coordinate is still above zero, it is optimum */
			final double smallerNonZeroDelta = piDividedBy2Epsilon + w_k_array [sortIndexes_wk [numNonMinCoord-1]];
			if (smallerNonZeroDelta > -1e-5)
			{
				double objFunc = 0;
				double transmittedTraffic = 0;
				DoubleMatrix1D x_k = DoubleFactory1D.dense.make ((int) a_k.size());
				for (int cont = 0 ; cont < K ; cont ++)
				{
					final int coordInSolution = coordinatesToConsider == null? sortIndexes_wk[cont] : coordinatesToConsider [sortIndexes_wk[cont]];
					if (cont < numNonMinCoord)
						x_k.set(coordInSolution, xMin_k.get(coordInSolution) + piDividedBy2Epsilon  + w_k_array[sortIndexes_wk[cont]]); // 
						//x_k.set(coordInSolution, xMin.get(k) + piDividedBy2Epsilon  + w_k_array[sortIndexes_wk[cont]]); // changed, xMin.get(k) is xMin.get(cont)
					else
						x_k.set(coordInSolution, xMin_k.get(coordInSolution));
					objFunc += x_k.get(coordInSolution) * a_k.get(coordInSolution) + epsilon * Math.pow(x_k.get(coordInSolution) ,  2);
					transmittedTraffic += x_k.get(coordInSolution);
				}
				if (Math.abs(transmittedTraffic - vectorSum) > 1E-3) throw new RuntimeException("Bad");
				if (objFunc < bestSolutionCost) { bestSolutionCost = objFunc; bestSolutionXk = x_k; }
			}
		}
		if (bestSolutionCost == Double.MAX_VALUE) throw new RuntimeException ("No solution found: bad");
		return bestSolutionXk;
	}	

//	public static Map<Long,Double> regularizedProjection_sumEquality_JOM (Map<Long,Double> a_k , Set<Long> coordinateKeysToConsider , Map<Long,Double> xMin , double vectorSum , double epsilon)
//	{
//		/* Sort the coordinates according to minimum a_k */
//		final Set<Long> keysToConsider = (coordinateKeysToConsider == null)? a_k.keySet() : coordinateKeysToConsider;
//		final int K = keysToConsider.size();
//		if (xMin == null) { xMin = new HashMap<Long,Double> (); for (long k : keysToConsider) xMin.put(k, 0.0); }
//		double [] xMinArray = new double [K]; int counter = 0; for (long k : keysToConsider) xMinArray [counter ++] = xMin.get(k);
//		double [] akArray = new double [K]; counter = 0; for (long k : keysToConsider) akArray [counter ++] = a_k.get(k);
//		
//		OptimizationProblem op = new OptimizationProblem();
//		op.addDecisionVariable("x_k", false , new int[] { 1 , K }, xMinArray , DoubleUtils.constantArray(K, vectorSum)); 
//		op.setInputParameter("x_Min", xMinArray , "row");
//		op.setInputParameter("a_k", akArray , "row");
//		op.setInputParameter("s", vectorSum);
//		op.setInputParameter("epsilon", epsilon);
//		
//		/* Set some input parameters */
//		op.setObjectiveFunction("minimize", "a_k * x_k' + epsilon * x_k * x_k'");
//		op.addConstraint("sum(x_k) == s");
//		
//		/* Call the solver to solve the problem */
//		op.solve("ipopt");
//
//		/* If an optimal solution was not found, quit */
//		if (!op.solutionIsOptimal ()) throw new RuntimeException ("An optimal solution was not found");
//	
//		/* Retrieve the optimum solutions. Convert the bps into Erlangs */
//		double [] x_k_array = op.getPrimalSolution("x_k").to1DArray();
//		Map<Long,Double> x_k = new HashMap<Long,Double> ();
//		counter = 0; for (long k : keysToConsider) x_k.put(k ,  x_k_array [counter ++]);
//
//		/* Check solution */
//		for (long k : keysToConsider) if (x_k.get(k) < xMin.get(k) - 1E-3) throw new RuntimeException("Bad");
//		double sum = 0; for (long k : keysToConsider) sum += x_k.get(k);
//		if (Math.abs(sum - vectorSum) > 1E-3)  throw new RuntimeException("Bad");
//
//		return x_k;
//		
//	}
	
}
