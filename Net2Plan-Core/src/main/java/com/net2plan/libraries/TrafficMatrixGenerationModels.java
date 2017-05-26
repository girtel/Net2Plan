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

package com.net2plan.libraries;

import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.RandomUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * <p>Set of methods implementing different traffic generation models based on traffic matrices.</p>
 *
 * <p><b>Important</b>: In {@code Net2Plan} self-demands are not allowed, thus the diagonal of the traffic matrices must be always zero.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class TrafficMatrixGenerationModels
{
	private TrafficMatrixGenerationModels() { }
	
	/**
	 * Returns the activity factor of a node, given the current UTC hour and its timezone (see [1]).
	 *
	 * @param UTC Universal Time Coordinated hour in decimal format (e.g. 10:30h is represented as 10.5)
	 * @param timezoneOffset Timezone offset from UTC in range [-12, 12]
	 * @param minValueFactor Minimum value at low-traffic hour in range [0,1] (in ref. [1] is equal to 0.1)
	 * @param maxValueFactor Maximum value at peak hour in range [0,1] (in ref. [1] is equal to 1)
	 * @return Activity factor
	 * @see <a href="http://www3.informatik.uni-wuerzburg.de/TR/tr363.pdf">[1] J. Milbrandt, M. Menth, S. Kopf, "Adaptive Bandwidth Allocation: Impact of Traffic Demand Models for Wide Area Networks," University of Würzburg, Germany, Report No. 363, June 2005</a>
	 */
	public static double activityFactor(double UTC, double timezoneOffset, double minValueFactor, double maxValueFactor)
	{
		if (maxValueFactor < 0 || maxValueFactor > 1) throw new RuntimeException("'maxValueFactor' must be in range [0, 1]");
		if (minValueFactor < 0 || minValueFactor > 1) throw new RuntimeException("'minValueFactor' must be in range [0, 1]");
		if (minValueFactor > maxValueFactor) throw new RuntimeException("'minValueFactor' must be lower or equal than 'maxValueFactor'");

		double activity = minValueFactor;
		double localTime = (UTC + timezoneOffset + 24) % 24;

		if (localTime >= 6) activity = maxValueFactor - (maxValueFactor - minValueFactor) * Math.pow(Math.cos(Math.PI * (localTime - 6) / 18), 10);

		return activity;
	}

	/**
	 * Generates a traffic matrix using a bimodal uniform random distribution, 
	 * that is, a distribution in which a value is taken for a uniform random 
	 * distribution with probability <i>p</i>, and from the other one with 
	 * probability <i>1-p</i>.
	 *
	 * @param N Number of nodes
	 * @param percentageThreshold Mixture coefficient
	 * @param minValueClass1 Minimum traffic value for class 1
	 * @param maxValueClass1 Maximum traffic value for class 1
	 * @param minValueClass2 Minimum traffic value for class 2
	 * @param maxValueClass2 Maximum traffic value for class 2
	 * @return Traffic matrix
	 */
	public static DoubleMatrix2D bimodalUniformRandom(int N, double percentageThreshold, double minValueClass1, double maxValueClass1, double minValueClass2, double maxValueClass2)
	{
		DoubleMatrix2D trafficMatrix = DoubleFactory2D.dense.make(N, N);

		IntArrayList list = new IntArrayList();
		for (int i = 0; i < N; i++) list.add(i);
		list.trimToSize();

		for (int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
		{
			list.shuffle();

			for (int pos = 0; pos < N; pos++)
			{
				if (ingressNodeId == list.getQuick(pos)) continue;

				if ((double) pos / N < percentageThreshold)
				{
					trafficMatrix.setQuick(ingressNodeId, list.getQuick(pos), minValueClass1 + Math.random() * (maxValueClass1 - minValueClass1));
				}
				else
				{
					trafficMatrix.setQuick(ingressNodeId, list.getQuick(pos), minValueClass2 + Math.random() * (maxValueClass2 - minValueClass2));
				}
			}
		}

		return trafficMatrix;
	}

	/**
	 * Checks whether the input traffic matrix is valid: non-negative square matrix with zero diagonal.
	 * 
	 * @param trafficMatrix Traffic matrix
	 */
	public static void checkTrafficMatrix(DoubleMatrix2D trafficMatrix)
	{
		if (trafficMatrix == null) throw new NullPointerException("Traffic matrix cannot be null");

		int N = trafficMatrix.rows();
		if (trafficMatrix.columns() != N) throw new RuntimeException("Traffic matrix must be a square matrix");

		for (int rowId = 0; rowId < N; rowId++)
		{
			for (int columnId = 0; columnId < N; columnId++)
			{
				if (rowId == columnId && trafficMatrix.getQuick(rowId, columnId) != 0) throw new RuntimeException("Self-demands are not allowed");
				if (trafficMatrix.getQuick(rowId, columnId) < 0) throw new RuntimeException("Offered traffic from node " + rowId + " to node " + columnId + " must be greater or equal than zero");
			}
		}
	}
	
	/**
	 * <p>Computes a set of matrices from a seminal one, using a traffic forecast 
	 * based on the compound annual growth rate (CAGR) concept.</p>
	 * 
	 * <p>Traffic forecast for node pair <i>(i,j)</i> on year <i>k</i> from the 
	 * reference one is given by:</p>
	 * 
	 * <p><i>TM(i,j,k) = TM(i,j,0) * (1+CAGR<sup>k</sup>)</i></p>
	 * 
	 * @param trafficMatrix Seminal traffic matrix
	 * @param cagr Compound Annual Growth Rate (0.2 means an increase of 20% with respect to the previous year)
	 * @param numMatrices Number of matrices to generate
	 * @return New traffic matrices
	 */
	public static List<DoubleMatrix2D> computeMatricesCAGR(DoubleMatrix2D trafficMatrix, double cagr, int numMatrices)
	{
		checkTrafficMatrix(trafficMatrix);

		if (cagr <= 0) throw new Net2PlanException("Compound annual growth rate must be greater than zero");
		if (numMatrices < 1) throw new Net2PlanException("Number of matrices must be greater or equal than one");

		List<DoubleMatrix2D> newMatrices = new LinkedList<DoubleMatrix2D>();
		for (int matrixId = 0; matrixId < numMatrices; matrixId++)
		{
			double multiplicativeFactor = Math.pow(1 + cagr, matrixId + 1);
			newMatrices.add(trafficMatrix.copy ().assign(DoubleFunctions.mult(multiplicativeFactor)));
		}

		return newMatrices;
	}

	/**
	 * Computes a set of matrices from a seminal one, using a random Gaussian distribution.
	 * 
	 * @param trafficMatrix Seminal traffic matrix
	 * @param cv Coefficient of variation
	 * @param maxRelativeVariation Maximum relative variation from the mean value (0.2 means a maximum variation of +-20%)
	 * @param numMatrices Number of matrices to generate
	 * @return New traffic matrices
	 */
	public static List<DoubleMatrix2D> computeMatricesRandomGaussianVariation(DoubleMatrix2D trafficMatrix, double cv, double maxRelativeVariation, int numMatrices)
	{
		checkTrafficMatrix(trafficMatrix);

		if (cv <= 0) throw new Net2PlanException("Coefficient of variation must be greater than zero");
		if (maxRelativeVariation <= 0) throw new Net2PlanException("Maximum relative variation must be greater than zero");
		if (numMatrices < 1) throw new Net2PlanException("Number of matrices must be greater or equal than one");

		int N = trafficMatrix.rows();
		Random r = new Random();
		List<DoubleMatrix2D> newMatrices = new LinkedList<DoubleMatrix2D>();

		for (int matrixId = 0; matrixId < numMatrices; matrixId++)
		{
			DoubleMatrix2D newTrafficMatrix = DoubleFactory2D.dense.make(N, N);
			for (int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
			{
				for (int egressNodeId = 0; egressNodeId < N; egressNodeId++)
				{
					if (trafficMatrix.getQuick(ingressNodeId, egressNodeId) == 0) continue;

					double sigma = cv * trafficMatrix.getQuick(ingressNodeId, egressNodeId);
					
					double variationFromMeanValue = r.nextGaussian() * sigma;
					if (variationFromMeanValue > maxRelativeVariation) variationFromMeanValue = maxRelativeVariation;
					else if (variationFromMeanValue < -maxRelativeVariation) variationFromMeanValue = -maxRelativeVariation;

					newTrafficMatrix.setQuick(ingressNodeId, egressNodeId, trafficMatrix.getQuick(ingressNodeId, egressNodeId) * (1 + variationFromMeanValue));
					if (newTrafficMatrix.getQuick(ingressNodeId, egressNodeId) < 0) newTrafficMatrix.setQuick(ingressNodeId, egressNodeId, 0);
				}
			}

			newMatrices.add(newTrafficMatrix);
		}

		return newMatrices;
	}

	/**
	 * Computes a set of matrices from a seminal one, using a random uniform distribution.
	 * 
	 * @param trafficMatrix Seminal traffic matrix
	 * @param maxRelativeVariation Maximum relative variation from the mean value (0.2 means a maximum variation of +-20%)
	 * @param numMatrices Number of matrices to generate
	 * @return New traffic matrices
	 */
	public static List<DoubleMatrix2D> computeMatricesRandomUniformVariation(DoubleMatrix2D trafficMatrix, double maxRelativeVariation, int numMatrices)
	{
		checkTrafficMatrix(trafficMatrix);

		if (maxRelativeVariation <= 0) throw new Net2PlanException("Maximum relative variation must be greater than zero");
		if (numMatrices < 1) throw new Net2PlanException("Number of matrices must be greater or equal than one");

		int N = trafficMatrix.rows();
		Random r = new Random();
		List<DoubleMatrix2D> newMatrices = new LinkedList<DoubleMatrix2D>();

		for (int matrixId = 0; matrixId < numMatrices; matrixId++)
		{
			DoubleMatrix2D newTrafficMatrix = DoubleFactory2D.dense.make(N, N);
			for (int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
			{
				for (int egressNodeId = 0; egressNodeId < N; egressNodeId++)
				{
					if (trafficMatrix.getQuick(ingressNodeId, egressNodeId) == 0) continue;

					double variationFromMeanValue = RandomUtils.random(-maxRelativeVariation, maxRelativeVariation, r);

					newTrafficMatrix.setQuick(ingressNodeId, egressNodeId, trafficMatrix.getQuick(ingressNodeId, egressNodeId) * (1 + variationFromMeanValue));
					if (newTrafficMatrix.getQuick(ingressNodeId, egressNodeId) < 0) newTrafficMatrix.setQuick(ingressNodeId, egressNodeId, 0);
				}
			}

			newMatrices.add(newTrafficMatrix);
		}

		return newMatrices;
	}
	
	/**
	 * Generates a constant traffic matrix.
	 *
	 * @param N Number of nodes
	 * @param value Traffic value
	 * @return Traffic matrix
	 */
	public static DoubleMatrix2D constantTrafficMatrix(int N, double value)
	{
		if (value < 0) throw new Net2PlanException("Traffic value must be greater or equal than zero");
		
		DoubleMatrix2D trafficMatrix = DoubleFactory2D.dense.make(N, N, value);
		for (int n = 0; n < N; n++) trafficMatrix.setQuick(n, n, 0);

		return trafficMatrix;
	}

	/**
	 * <p>Generates a traffic matrix using a 'gravity model' (see [1]). This basic model predicts the demand between node <i>i</i> and <i>j</i> as:</p>
	 * <p><i>s<sub>ij</sub>=Ct<sub>e(i)</sub>t<sub>x(j)</sub></i></p>
	 * <p>where <i>C</i> is a normalization constant that makes the sum of estimated demands equal to the total traffic entering/leaving the network.</p>
	 * 
	 * @param ingressTrafficPerNode Ingress traffic per node
	 * @param egressTrafficPerNode Egress traffic per node
	 * @return Traffic matrix
	 * @see <a href="http://dl.acm.org/citation.cfm?id=1028807">[1] Anders Gunnar, Mikael Johansson, Thomas Telkamp, "Traffic Matrix Estimation on a Large IP Backbone – A Comparison on Real Data," in <i>Proceedings of the 4th ACM SIGCOMM Conference on Internet Measurement</i> (IMC'04), October 2004</a>
	 */
	public static DoubleMatrix2D gravityModel(double[] ingressTrafficPerNode, double[] egressTrafficPerNode)
	{
		int N = ingressTrafficPerNode.length;

		double totalIngressTraffic = DoubleUtils.sum(ingressTrafficPerNode);
		double totalEgressTraffic = DoubleUtils.sum(egressTrafficPerNode);
		double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
		if (!DoubleUtils.isEqualWithinRelativeTolerance(totalEgressTraffic, totalIngressTraffic, PRECISION_FACTOR)) throw new Net2PlanException(String.format("Total ingress traffic (%f) must be equal to the total egress traffic (%f)", totalIngressTraffic, totalEgressTraffic));
		if (totalIngressTraffic < PRECISION_FACTOR || totalEgressTraffic < PRECISION_FACTOR)
			ErrorHandling.showErrorDialog("Total ingress and egress traffic must be greater than zero", "Error applying gravity model");

		DoubleMatrix2D trafficMatrix = DoubleFactory2D.dense.make(N, N);
		for (int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
		{
			for (int egressNodeId = 0; egressNodeId < N; egressNodeId++)
			{
				if (ingressNodeId == egressNodeId) continue;

				trafficMatrix.setQuick(ingressNodeId, egressNodeId, ingressTrafficPerNode[ingressNodeId] * egressTrafficPerNode[egressNodeId] / totalEgressTraffic);
			}
		}
		
		return trafficMatrix;
	}

	/**
	 * Normalizes the input traffic matrix with respect to a given incoming traffic vector.
	 *
	 * @param trafficMatrix Input traffic matrix
	 * @param incomingTraffic Vector of incoming traffic to each node
	 * @return Traffic matrix normalized to the specified incoming traffic per node
	 */
	public static DoubleMatrix2D normalizationPattern_incomingTraffic(DoubleMatrix2D trafficMatrix, double[] incomingTraffic)
	{
		checkTrafficMatrix(trafficMatrix);

		int N = trafficMatrix.rows();
		if (incomingTraffic.length != N) throw new Net2PlanException("'incomingTraffic' size doesn't match the number of nodes");

		DoubleMatrix2D trafficMatrix_out = trafficMatrix.copy();
		for (int columnId = 0; columnId < N; columnId++)
		{
			DoubleMatrix1D column = trafficMatrix_out.viewColumn(columnId);
			double incomingTraffic_old = column.zSum();
			if (incomingTraffic_old == 0) continue;

			column.assign(DoubleFunctions.mult(incomingTraffic[columnId] / incomingTraffic_old));
		}

		return trafficMatrix_out;
	}
	
	/**
	 * Normalizes the input traffic matrix with respect to a given outgoing traffic vector.
	 *
	 * @param trafficMatrix Input traffic matrix
	 * @param outgoingTraffic Vector of outgoing traffic to each node
	 * @return Traffic matrix normalized to the specified outgoing traffic per node
	 */
	public static DoubleMatrix2D normalizationPattern_outgoingTraffic(DoubleMatrix2D trafficMatrix, double[] outgoingTraffic)
	{
		checkTrafficMatrix(trafficMatrix);

		int N = trafficMatrix.rows();
		if (outgoingTraffic.length != N) throw new Net2PlanException("'outgoingTraffic' size doesn't match the number of nodes");

		DoubleMatrix2D trafficMatrix_out = trafficMatrix.copy();
		for (int rowId = 0; rowId < N; rowId++)
		{
			DoubleMatrix1D row = trafficMatrix_out.viewRow(rowId);
			double outgoingTraffic_old = row.zSum();
			if (outgoingTraffic_old == 0) continue;

			row.assign(DoubleFunctions.mult(outgoingTraffic[rowId] / outgoingTraffic_old));
		}

		return trafficMatrix_out;
	}

	/**
	 * Normalizes the input traffic matrix so that the sum of all entries is equal to a given value.
	 *
	 * @param trafficMatrix Input traffic matrix
	 * @param totalTraffic Total traffic expected
	 * @return Traffic matrix normalized to the specified total traffic
	 */
	public static DoubleMatrix2D normalizationPattern_totalTraffic(DoubleMatrix2D trafficMatrix, double totalTraffic)
	{
		checkTrafficMatrix(trafficMatrix);

		double H = trafficMatrix.zSum();
		if (H == 0) return trafficMatrix.copy();

		double scaleFactor = totalTraffic / H;
		return trafficMatrix.copy().assign(DoubleFunctions.mult(scaleFactor));
	}

	/**
	 * <p>Normalizes the load of a traffic matrix in an effort to assess the merits
	 * of the algorithms for different traffic load conditions. The value
	 * {@code load} represents the average amount of traffic between two nodes,
	 * among all the entries, during the highest loaded time, measured in number of links.</p>
	 *
	 * <p>Consequently, a value of {@code load}=0.5 corresponds to the case
	 * when the (maximum) average traffic between two nodes equals 50% of a single link capacity.
	 * In constrast, a value of {@code load}=10 captures cases in which the
	 * (maximum) average traffic between two nodes fills on average 10 links.
	 *
	 * @param trafficMatrix Input traffic matrix
	 * @param linkCapacity Link capacity (in same units as {@code trafficMatrix})
	 * @param load Load factor (measured in number of links). The highest value of the resulting traffic matrix will be equal to {@code load}*{@code linkCapacity}
	 * @return Traffic matrix normalized to the specified link load factor
	 */
	public static DoubleMatrix2D normalizeToLinkLoad(DoubleMatrix2D trafficMatrix, double linkCapacity, double load)
	{
		checkTrafficMatrix(trafficMatrix);

		double maxValue = trafficMatrix.size () == 0? 0 : trafficMatrix.getMaxLocation()[0];
		if (maxValue == 0) return trafficMatrix.copy();

		double scaleFactor = linkCapacity * load / maxValue;
		return trafficMatrix.copy().assign(DoubleFunctions.mult(scaleFactor));
	}

	/**
	 * <p>Returns the maximum scaled version of the offered traffic vector that 
	 * can be carried by the network, provided that no link is oversubscribed. 
	 * It is assumed no protection capacity is reserved.</p>
	 * 
	 * <p><b>Important</b>: {@code JOM} library is required here.</p>
	 * 
	 * @param netPlan Network design with physical topology (nodes and links) and a set of demands
	 * @param solverName The solver name to be used by JOM
	 * @param solverLibraryName The solver library full or relative path, to be used by JOM. Leave blank to use JOM default
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Scaled version of the offered traffic vector
	 */
	public static DoubleMatrix1D normalizeTraffic_linkCapacity_xde(NetPlan netPlan, String solverName, String solverLibraryName , NetworkLayer ... optionalLayerParameter)
	{
		if (optionalLayerParameter.length >= 2) throw new Net2PlanException ("None or one layer parameter can be supplied");
		NetworkLayer layer = (optionalLayerParameter.length == 1)? optionalLayerParameter [0] : netPlan.getNetworkLayerDefault();
		int N = netPlan.getNumberOfNodes();
		int E = netPlan.getNumberOfLinks();
		int D = netPlan.getNumberOfDemands();
		if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("A physical topology (nodes and links) and a demand set are required");

		OptimizationProblem op = new OptimizationProblem();

		op.setInputParameter("u_e", netPlan.getVectorLinkCapacity(layer) , "row");
		op.setInputParameter("h_d", netPlan.getVectorDemandOfferedTraffic(layer) , "row");
		op.setInputParameter("A_ne", netPlan.getMatrixNodeLinkIncidence(layer));
		op.setInputParameter("A_nd", netPlan.getMatrixNodeDemandIncidence(layer));

		op.addDecisionVariable("x_de", false, new int[]	{ D, E }, 0, Double.MAX_VALUE);
		op.addDecisionVariable("alphaFactor", false, new int[] { 1, 1 }, 0, Double.MAX_VALUE);

		op.setObjectiveFunction("maximize", "alphaFactor");

		op.addConstraint("A_ne * x_de' == alphaFactor * A_nd");
		op.addConstraint("h_d * x_de <= u_e");

		op.solve(solverName, "solverLibraryName", solverLibraryName);

		if (!op.solutionIsOptimal()) throw new Net2PlanException("A solution cannot be found");

		double alphaFactor = op.getPrimalSolution("alphaFactor").toValue();

		DoubleMatrix1D newHd = netPlan.getVectorDemandOfferedTraffic(layer);
		newHd.assign(DoubleFunctions.mult(alphaFactor));
		return newHd;
	}

	/**
	 * <p>Returns the maximum scaled version of the offered traffic vector so 
	 * that the network capacity (summation of capacity of all links) is exhausted. </p>
	 * 
	 * @param netPlan Network design with physical topology (nodes and links) and a set of demands
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Scaled version of the offered traffic vector
	 */
	public static DoubleMatrix1D normalizeTraffic_networkCapacity(NetPlan netPlan , NetworkLayer ... optionalLayerParameter)
	{
		if (optionalLayerParameter.length >= 2) throw new Net2PlanException ("None or one layer parameter can be supplied");
		NetworkLayer layer = (optionalLayerParameter.length == 1)? optionalLayerParameter [0] : netPlan.getNetworkLayerDefault();
		int N = netPlan.getNumberOfNodes();
		int E = netPlan.getNumberOfLinks();
		int D = netPlan.getNumberOfDemands();
		if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("A physical topology (nodes and links) and a demand set are required");

		double minTrafficInTheLinks = 0;
		for (Demand d : netPlan.getDemands(layer))
		{
			List<Link> seqLinks = GraphUtils.getShortestPath(netPlan.getNodes () , netPlan.getLinks (), d.getIngressNode() , d.getEgressNode() , null);
			if (seqLinks == null) throw new Net2PlanException("Physical topology must be connected");
			minTrafficInTheLinks += d.getOfferedTraffic() * seqLinks.size();
		}
		
		double U_e = netPlan.getVectorLinkCapacity(layer).zSum();
		double alphaFactor = minTrafficInTheLinks == 0 ? 0 : U_e / minTrafficInTheLinks;
		DoubleMatrix1D newHd = netPlan.getVectorDemandOfferedTraffic(layer);
		newHd.assign(DoubleFunctions.mult(alphaFactor));
		return newHd;
	}
	
	/**
	 * <p>Generates a traffic matrix using the population-distance model.</p>
	 * 
	 * <p>Reference: {@code R. S. Cahn, <i>Wide area network design: concepts and tools for optimization</i>, Morgan Kaufmann Publishers Inc., 1998}</p>
	 *
	 * @param distanceMatrix Distance matrix, where cell (<i>i</i>, <i>j</i>) represents the distance from node <i>i</i> to node <i>j</i>
	 * @param populationVector Vector with <i>N</i> elements in which each element is the population of the corresponding node
	 * @param levelVector Vector with <i>N</i> elements in which each element is the level (i.e. type) of the corresponding node
	 * @param levelMatrix Level matrix
	 * @param randomFactor Random factor
	 * @param populationOffset Population offset
	 * @param populationPower Population power
	 * @param distanceOffset Distance offset
	 * @param distancePower Distance power
	 * @param normalizePopulationFactor Indicates whether population products must be normalized by the maximum population among all nodes
	 * @param normalizeDistanceFactor  Indicates whether node-pair distances must be normalized by the maximum distance among all node-pairs
	 * @return Traffic matrix
	 */
	public static DoubleMatrix2D populationDistanceModel(DoubleMatrix2D distanceMatrix, double[] populationVector, int[] levelVector, DoubleMatrix2D levelMatrix, double randomFactor, double populationOffset, double populationPower, double distanceOffset, double distancePower, boolean normalizePopulationFactor, boolean normalizeDistanceFactor)
	{
		int N = distanceMatrix.rows();

		DoubleMatrix2D trafficMatrix = DoubleFactory2D.dense.make(N, N);
		
		double dist_max = 1;
		double pop_max = 1.0;
		if (normalizePopulationFactor || normalizeDistanceFactor)
		{
			/* First, compute pop_max and dist_max */
			if (normalizePopulationFactor) pop_max = -1;
			if (normalizeDistanceFactor) dist_max = -1;
			
			for (int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
			{
				if (normalizePopulationFactor) pop_max = Math.max(pop_max, populationVector[ingressNodeId]);
				if (!normalizeDistanceFactor) continue;

				for (int egressNodeId = ingressNodeId + 1; egressNodeId < N; egressNodeId++)
				{
					dist_max = Math.max(dist_max, distanceMatrix.getQuick(ingressNodeId, egressNodeId));
				}
			}
		}

		if (pop_max == 0) throw new Net2PlanException("The maximum population is zero, so traffic matrix would have only zero entries");
		if (dist_max == 0) throw new Net2PlanException("The maximum distance between nodes is zero, so traffic matrix would have only zero entries");
		
		/* Then, compute the traffic matrix */
		for (int i = 0; i < N; i++)
		{
			for (int j = 0; j < N; j++)
			{
				if (i == j) continue;

				double distanceCoeff = Math.pow(distanceOffset + (distanceMatrix.getQuick(i, j) / dist_max), distancePower);
				if (distanceCoeff == 0) continue;
				else if (Double.isNaN(distanceCoeff)) distanceCoeff = 1;
				
				double populationCoeff = Math.pow(populationOffset + (populationVector[i] * populationVector[j] / Math.pow(pop_max, 2)), populationPower);
				if (populationCoeff == 0) continue;
				
				double levelCoeff = levelMatrix.getQuick(levelVector[i] - 1, levelVector[j] - 1);
				if (levelCoeff == 0) continue;
				
				double randomCoeff = 1 - randomFactor + 2 * randomFactor * Math.random();
				trafficMatrix.setQuick(i, j, levelCoeff * randomCoeff * populationCoeff / distanceCoeff);
			}
		}

		return trafficMatrix;
	}

	/**
	 * Symmetrizes the input traffic matrix setting each node-pair traffic value 
	 * equal to the average between the traffic in both directions.
	 * 
	 * @param trafficMatrix Traffic matrix
	 */
	public static void symmetrizeTrafficMatrix(DoubleMatrix2D trafficMatrix)
	{
		checkTrafficMatrix(trafficMatrix);
		int N = trafficMatrix.rows();

		for (int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
		{
			for (int egressNodeId = ingressNodeId + 1; egressNodeId < N; egressNodeId++)
			{
				double meanValue = (trafficMatrix.getQuick(ingressNodeId, egressNodeId) + trafficMatrix.getQuick(egressNodeId, ingressNodeId)) / 2.0;

				trafficMatrix.setQuick(ingressNodeId, egressNodeId, meanValue);
				trafficMatrix.setQuick(egressNodeId, ingressNodeId, meanValue);
			}
		}
	}

	/**
	 * Generates a traffic matrix using a uniform random distribution.
	 *
	 * @param N Number of nodes
	 * @param minValue Minimum traffic value
	 * @param maxValue Maximum traffic value
	 * @return Traffic matrix
	 */
	public static DoubleMatrix2D uniformRandom(int N, double minValue, double maxValue)
	{
		DoubleMatrix2D trafficMatrix = DoubleFactory2D.dense.random(N, N);
		trafficMatrix.assign(DoubleFunctions.chain(DoubleFunctions.plus(minValue), DoubleFunctions.mult(maxValue - minValue)));
		for (int n = 0; n < N; n++) trafficMatrix.setQuick(n, n, 0);

		return trafficMatrix;
	}
}
