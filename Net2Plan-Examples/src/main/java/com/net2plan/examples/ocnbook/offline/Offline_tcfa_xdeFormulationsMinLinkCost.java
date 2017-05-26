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
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * This algorithm gives access to several variants of full topology design problems. 
 * @net2plan.description 
 * @net2plan.keywords Topology assignment (TA), Capacity assignment (CA), Flow assignment (FA), Flow-link formulation, JOM
 * @net2plan.ocnbooksections Section 7.3
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_tcfa_xdeFormulationsMinLinkCost implements IAlgorithm
{
	private InputParameter topologyType = new InputParameter ("topologyType", "#select# arbitrary-mesh bidirectional-mesh unidirectional-ring bidirectional-ring bidirectional-tree", "The constraints on the link topology: arbitrary topology (no specific constraint), arbitrary bidirectional topology (link from i to j exists iff it exists from j to i), unidirectional ring, bidirectional ring, bidirectional tree");
	private InputParameter solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter fixedCostFactorPerKm = new InputParameter ("fixedCostFactorPerKm", (double) 1.0 , "Fixed cost factor per km of a link (the cost of a link of d km and 0 capacity is d times this quantity)" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter linkPropagationSpeedInKmPerSecond = new InputParameter ("linkPropagationSpeedInKmPerSecond", (double) 200000 , "The propagation speed in km per second of the deployed links" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter U_max = new InputParameter ("U_max", (double) -1 , "The maximum capacity a link can have. If non positive, there is not limit");
	private InputParameter variableCostFactorPerKmAndTrafficUnit = new InputParameter ("variableCostFactorPerKmAndTrafficUnit", (double) 1 , "Variable cost factor per km and traffic unit of a link (the cost of a link of 1 km and 1 unit of capacity, and 0 of fixed cost)" , 0 , true , Double.MAX_VALUE , true);
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		/* Basic checks */
		final int N = netPlan.getNumberOfNodes();
		final int D = netPlan.getNumberOfDemands();
		if (N == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology and a demand set");
		
		/* Initialize some variables */
		final double PRECISION_FACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));

		/* Create a full mesh of links in the netPlan object */
		netPlan.removeAllLinks();
		for (Node i : netPlan.getNodes ()) for (Node j : netPlan.getNodes ()) if (i.getId () > j.getId ()) netPlan.addLinkBidirectional(i, j, 1.0 , netPlan.getNodePairEuclideanDistance(i, j), linkPropagationSpeedInKmPerSecond.getDouble() , null);
					
		/* Compute the distances of the potential links */
		final int E_fm = netPlan.getNumberOfLinks();
		final DoubleMatrix1D d_e = netPlan.getVectorLinkLengthInKm();
		final DoubleMatrix1D h_d = netPlan.getVectorDemandOfferedTraffic();
		final DoubleMatrix2D Abid_ee = netPlan.getMatrixLink2LinkBidirectionalityMatrix ();

		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();

		/* Set some input parameters */
		op.setInputParameter("U_max", U_max.getDouble() > 0? U_max.getDouble() : h_d.zSum());
		op.setInputParameter("fc", fixedCostFactorPerKm.getDouble());
		op.setInputParameter("vc", variableCostFactorPerKmAndTrafficUnit.getDouble());
		op.setInputParameter("h_d", h_d, "row");
		op.setInputParameter("d_e", d_e, "row");
		op.setInputParameter("A_ne", netPlan.getMatrixNodeLinkIncidence());
		op.setInputParameter("A_nd", netPlan.getMatrixNodeDemandIncidence());

		/* Add the decision variables to the problem */
		op.addDecisionVariable("z_e", true, new int[] { 1, E_fm }, 0, 1); /* 1 if there is a link installed in the candidate link e */
		op.addDecisionVariable("u_e", false, new int[] { 1, E_fm }, 0, U_max.getDouble() <= 0? Double.MAX_VALUE : U_max.getDouble()); /* the capacity installed in the candidate  link e, if any */
		op.addDecisionVariable("xx_de", false, new int[] { D, E_fm }, 0, 1); /* fraction of the traffic of demand d, that is carried in the candidate link e */
		
		/* Sets the objective function */
		op.setObjectiveFunction("minimize", "sum(fc * d_e .* z_e) + sum (vc * d_e .* u_e)");

		/* Compute some matrices required for writing the constraints */
		op.addConstraint("A_ne * (xx_de') == A_nd"); /* the flow-conservation constraints (NxD constraints) */
		op.addConstraint("h_d * xx_de <= u_e"); /* the capacity constraints (E constraints) */
		op.addConstraint("u_e <= U_max * z_e"); /* limit to maximum link capacity */
		
		if (topologyType.getString().equalsIgnoreCase("arbitrary-mesh"))
		{
		}
		else if (topologyType.getString().equalsIgnoreCase("bidirectional-mesh"))
		{ 
			op.setInputParameter("Abid_ee", Abid_ee); 
			op.addConstraint("z_e == z_e * Abid_ee"); /* If a link exists, also the opposite */
		} 
		else if (topologyType.getString().equalsIgnoreCase("unidirectional-ring"))
		{
			op.setInputParameter("Aout_ne" , netPlan.getMatrixNodeLinkOutgoingIncidence());
			op.setInputParameter("Ain_ne" , netPlan.getMatrixNodeLinkIncomingIncidence());
			op.addConstraint("Aout_ne * z_e' == 1"); /* number of out links of a node is one */
			op.addConstraint("Ain_ne * z_e' == 1"); /* number of in links of a node is one */
		}
		else if (topologyType.getString().equalsIgnoreCase("bidirectional-ring"))
		{
			op.setInputParameter("Aout_ne" , netPlan.getMatrixNodeLinkOutgoingIncidence());
			op.setInputParameter("Ain_ne" , netPlan.getMatrixNodeLinkIncomingIncidence());
			op.setInputParameter("Abid_ee", Abid_ee); 
			op.addConstraint("z_e == z_e * Abid_ee"); /* If a link exists, also teh opposite */
			op.addConstraint("Aout_ne * z_e' == 2"); /* number of out links of a node is two */
			op.addConstraint("Ain_ne * z_e' == 2"); /* number of in links of a node is two */
		}
		else if (topologyType.getString().equalsIgnoreCase("bidirectional-tree"))
		{
			op.setInputParameter("Abid_ee", Abid_ee); 
			op.addConstraint("z_e == z_e * Abid_ee"); /* If a link exists, also teh opposite */
			op.addConstraint("sum(z_e) == " + (2*(N-1))); /* the number of links is characteristic of a tree */
			System.out.println ("num links: " + 2*(N-1));
		}
		else throw new Net2PlanException ("Unknown topology type " + topologyType.getString());
			
		/* Call the solver to solve the problem */
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		
		/* Retrieve the optimum solutions */
		final DoubleMatrix1D z_e = op.getPrimalSolution("z_e").view1D().copy ();
		final DoubleMatrix1D u_e = op.getPrimalSolution("u_e").view1D().copy ();
		final DoubleMatrix2D xx_de = op.getPrimalSolution("xx_de").view2D().copy ();

		/* Sets the routing, capacities and links. Remove unused links (no capacity) */
		netPlan.setRoutingFromDemandLinkCarriedTraffic(xx_de , true , true);
		netPlan.setVectorLinkCapacity(u_e);
		netPlan.removeAllLinksUnused (PRECISION_FACTOR);
		
		return "Ok! Num links: " + netPlan.getNumberOfLinks() + ", total capacity: " + u_e.zSum();
	}

	@Override
	public String getDescription()
	{
		return "This algorithm gives access to several variants of full topology design problems. Given a set of nodes and a set of unicast demands with known  offered traffic. The algorithm searches for (i) the set of links to deploy, (ii) the link capacities, and (iii) how the traffic is routed over the links. The objective function consists in minimizing the cost, given by the sum of the link costs. The cost of a link is the sum of a fixed cost (proportional to the link length) that applies if the link exists (whatever its capacity is), and a variable cost proportional to its capacity and distance. Different problem variants can be selected, with different constraints (e.g. constraint the topology to be bidirectional, to be a ring, a tree...). The solution is searched by solving several variants of flow-link formulations.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
}
