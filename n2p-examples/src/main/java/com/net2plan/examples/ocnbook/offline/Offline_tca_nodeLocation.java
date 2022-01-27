package com.net2plan.examples.ocnbook.offline;

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


import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * Solves several variants of node location problem formlations.
 * @net2plan.description
 * @net2plan.keywords Topology assignment (TA), JOM
 * @net2plan.ocnbooksections Section 7.2
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_tca_nodeLocation implements IAlgorithm
{
	private InputParameter solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter linkCapacities = new InputParameter ("linkCapacities", (double) 100 , "The capacities to set in the links" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter linkPropagationSpeedInKmPerSecond = new InputParameter ("linkPropagationSpeedInKmPerSecond", (double) 200000 , "The propagation speed in km per second of the deployed links" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter coreNodeCost = new InputParameter ("coreNodeCost", (double) 1 , "Cost of one core node. Link cost is proportional to its distance, and normalized to the cost of the longest possible link is one" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter K_max = new InputParameter ("K_max", (int) 5 , "Maximum number of access nodes that can be connected to a core node" , (int) 1 , Integer.MAX_VALUE);
	private InputParameter maxNumCoreNodesPerSite = new InputParameter ("maxNumCoreNodesPerSite", (int) 1 , "Maximum number of core nodes that can be placed in a site." , (int) 1 , Integer.MAX_VALUE);
	private InputParameter maxNumCoreNodes = new InputParameter ("maxNumCoreNodes", (int) -1 , "Maximum number of core nodes in the network. If <= 0, there is no limit.");

	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		/* Basic checks */
		final int N = netPlan.getNumberOfNodes();
		if (N == 0) throw new Net2PlanException("The input design must have nodes");

		final DoubleMatrix2D linkDistanceMatrix_nn = netPlan.getMatrixNode2NodeEuclideanDistance();
		final double maxDistance = linkDistanceMatrix_nn.getMaxLocation() [0];
		final DoubleMatrix2D c_ij = linkDistanceMatrix_nn.copy ().assign (DoubleFunctions.div(maxDistance)); // normalize so the maximum possible link cost is one

		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();

		/* Set some input parameters */
		op.setInputParameter("c_ij", c_ij);
		op.setInputParameter("K_max", K_max.getInt ());
		op.setInputParameter("C", coreNodeCost.getDouble());
		
		/* Add the decision variables to the problem */
		op.addDecisionVariable("z_j", true, new int[] { 1, N }, 0, maxNumCoreNodesPerSite.getInt()); /* 1 if there is a node in this site */
		op.addDecisionVariable("e_ij", true, new int[] { N, N }, 0, 1); /* 1 if site i is connected to site j */

		/* Sets the objective function */
		op.setObjectiveFunction("minimize", "C * sum(z_j) + sum (e_ij .* c_ij)");

		/* Constraints */
		op.addConstraint("sum(e_ij , 2) == 1"); /* each site is connected to a core site */
		op.addConstraint("sum(e_ij , 1) <= K_max * z_j"); /* a site is connected to other, if the second is a core site */
		if (maxNumCoreNodes.getInt() > 0) op.addConstraint("sum(z_j) <= " + maxNumCoreNodes.getInt()); /* limit to the total number of core nodes in the network */

		/* Call the solver to solve the problem */
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		
		/* Retrieve the optimum solutions */
		DoubleMatrix1D  z_j = op.getPrimalSolution("z_j").view1D();
		DoubleMatrix2D e_ij = op.getPrimalSolution("e_ij").view2D();

		System.out.println("z_j: " + z_j);

		/* Remove all previous demands, links, protection segments, routes */
		netPlan.removeAllLinks();

		/* Save the new network links */
		for (int n = 0; n < N ; n ++)
			if (z_j.get(n) == 1) netPlan.getNode (n).setAttribute("hasCoreNode" , "true"); else netPlan.getNode (n).setAttribute("hasCoreNode" , "false");
		for (int i = 0; i < N ; i ++) for (int j = 0; j < N ; j ++)
			if (e_ij.get(i,j) == 1) 
			{
				if (z_j.get(j) == 0) throw new RuntimeException ("Bad");
				if (i != j) netPlan.addLink(netPlan.getNode(i), netPlan.getNode(j), linkCapacities.getDouble(),linkDistanceMatrix_nn.get(i,j), linkPropagationSpeedInKmPerSecond.getDouble() , null);
			}

		return "Ok! Number of core nodes: " + z_j.zSum();
	}

	@Override
	public String getDescription()
	{
		return "Given a set of access nodes, this algorithm computes the subset of access nodes which have a core node located next to it (in the same place), and the links access-to-core nodes, so that the network cost is minimized. This cost is given by a cost per core node (C) plus a cost per link, proportional to the link euclidean distance and normalized so that the maximum link cost is one. Access-core link capacities are fixed to the user-defined parameter linkCapacities. A core node cannot be connected to more than K_max access nodes, a user-defined parameter. This problem is modeled as an ILP and optimally solved using JOM.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
	
	
}

