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
import cern.jet.math.tdouble.DoubleFunctions;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * Solves several variants of unicast routing problems, with flow-path formulations
 * @net2plan.description
 * @net2plan.keywords JOM, Flow-path formulation, Flow assignment (FA)
 * @net2plan.ocnbooksections Section 4.2, Section 4.6.3
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_fa_xpFormulations implements IAlgorithm
{
	private InputParameter k = new InputParameter ("k", (int) 5 , "Maximum number of admissible paths per demand" , 1 , Integer.MAX_VALUE);
	private InputParameter shortestPathType = new InputParameter ("shortestPathType", "#select# hops km" , "Criteria to compute the shortest path. Valid values: 'hops' or 'km'");
	private InputParameter nonBifurcatedRouting = new InputParameter ("nonBifurcatedRouting", false , "True if the routing is constrained to be non-bifurcated");
	private InputParameter optimizationTarget = new InputParameter ("optimizationTarget", "#select# min-av-num-hops minimax-link-utilization maximin-link-idle-capacity min-av-network-delay min-av-network-blocking" , "Type of optimization target. Choose among minimize the average number of hops, minimize the highest link utilization, maximize the lowest link idle capacity, minimize the average end-to-end network delay including queueing (M/M/1 estimation) and propagation delays, and minimize the average network blocking assuming independent Erlang-B blocking in each link, load sharing model");
	private InputParameter maxLengthInKm = new InputParameter ("maxLengthInKm", (double) 2000 , "Paths longer than this are considered not admissible. A non-positive number means this limit does not exist");
	private InputParameter solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter binaryRatePerTrafficUnit_bps = new InputParameter ("binaryRatePerTrafficUnit_bps", (double) 1E6 , "Binary rate equivalent to one traffic unit (used only in average network delay minimization formulation)." , 0 , false , Double.MAX_VALUE , true);
	private InputParameter averagePacketLengthInBytes = new InputParameter ("averagePacketLengthInBytes", (double) 500 , "Average packet length in bytes (used only in average network delay minimization formulation)." , 0 , false , Double.MAX_VALUE , true);
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		if (solverName.getString ().equalsIgnoreCase("ipopt") && nonBifurcatedRouting.getBoolean()) throw new Net2PlanException ("With IPOPT solver, the routing cannot be constrained to be non-bifurcated");
		
		/* Initialize variables */
		final int E = netPlan.getNumberOfLinks();
		final int D = netPlan.getNumberOfDemands();
		final double PRECISION_FACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));
		if (E == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology with links and a demand set");

		/* Remove all unicast routed traffic. Any multicast routed traffic is kept */
		netPlan.removeAllUnicastRoutingInformation();
		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING);

		/* Add all the k-shortest candidate routes to the netPlan object carrying no traffic */
		final DoubleMatrix1D linkCostVector = shortestPathType.getString().equalsIgnoreCase("hops")? DoubleFactory1D.dense.make (E , 1.0) : netPlan.getVectorLinkLengthInKm();
		netPlan.addRoutesFromCandidatePathList(netPlan.computeUnicastCandidatePathList(linkCostVector , k.getInt(), maxLengthInKm.getDouble(), -1, -1, -1, -1, -1 , null));
		final int P = netPlan.getNumberOfRoutes(); 

		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();

		/* Set some input parameters to the problem */
		op.setInputParameter("u_e", netPlan.getVectorLinkSpareCapacity(), "row"); /* for each link, its unused capacity (the one not used by any mulitcast trees) */
		op.setInputParameter("A_dp", netPlan.getMatrixDemand2RouteAssignment()); /* 1 in position (d,p) if demand d is served by path p, 0 otherwise */ 
		op.setInputParameter("A_ep", netPlan.getMatrixLink2RouteAssignment()); /* 1 in position (e,p) if link e is traversed by path p, 0 otherwise */
		op.setInputParameter("h_d", netPlan.getVectorDemandOfferedTraffic(), "row"); /* for each demand, its offered traffic */
		op.setInputParameter("h_p", netPlan.getVectorRouteOfferedTrafficOfAssociatedDemand () , "row"); /* for each path, the offered traffic of its demand */

		/* Write the problem formulations */
		if (optimizationTarget.getString ().equals ("min-av-num-hops")) 
		{
			op.setInputParameter("l_p", netPlan.getVectorRouteNumberOfLinks() , "row"); /* for each path, the number of traversed links */
			op.addDecisionVariable("xx_p", nonBifurcatedRouting.getBoolean() , new int[] { 1, P }, 0, 1); /* the FRACTION of traffic of demand d(p) that is carried by p */

			op.setObjectiveFunction("minimize", "sum (l_p .* h_p .* xx_p)"); /* sum of the traffic in the links, proportional to the average number of hops  */
			op.addConstraint("A_dp * xx_p' == 1"); /* for each demand, the 100% of the traffic is carried (summing the associated paths) */
			op.addConstraint("A_ep * (h_p .* xx_p)' <= u_e'"); /* the traffic in each link cannot exceed its capacity  */
		}
		else if (optimizationTarget.getString ().equals ("minimax-link-utilization")) 
		{
			op.addDecisionVariable("xx_p", nonBifurcatedRouting.getBoolean(), new int[] { 1, P }, 0, 1); /* the FRACTION of traffic of demand d(p) that is carried by p */
			op.addDecisionVariable("rho", false, new int[] { 1, 1 }, 0, 1); /* worse case link utilization */
			op.setObjectiveFunction("minimize", "rho");
			op.addConstraint("A_dp * xx_p' == 1"); /* for each demand, the 100% of the traffic is carried (summing the associated paths) */
			op.addConstraint("A_ep * (h_p .* xx_p)' <= rho * u_e'"); /* the traffic in each link cannot exceed its capacity. sets rho as the worse case utilization */
		}
		else if (optimizationTarget.getString ().equals ("maximin-link-idle-capacity"))
		{
			op.addDecisionVariable("xx_p", nonBifurcatedRouting.getBoolean() , new int[] { 1, P }, 0, 1); /* the FRACTION of traffic of demand d(p) that is carried by p */
			op.addDecisionVariable("u", false, new int[] { 1, 1 }, 0, Double.MAX_VALUE); /* worse case link idle capacity */
			op.setObjectiveFunction("maximize", "u");
			op.addConstraint("A_dp * xx_p' == 1"); /* for each demand, the 100% of the traffic is carried (summing the associated paths) */
			op.addConstraint("A_ep * (h_p .* xx_p)' <= -u + u_e'"); /* the traffic in each link cannot exceed its capacity. sets u as the worse case idle capacity */
		}
		else if (optimizationTarget.getString ().equals ("min-av-network-delay"))
		{
			if (!solverName.getString ().equalsIgnoreCase("ipopt") || nonBifurcatedRouting.getBoolean()) throw new Net2PlanException ("This is a convex non linear model: please use IPOPT solver. The routing cannot be constrained to be non-bifurcated");
			op.setInputParameter("d_e_secs", netPlan.getVectorLinkPropagationDelayInMiliseconds().assign (DoubleFunctions.mult (0.001)) , "row");
			op.setInputParameter("L", averagePacketLengthInBytes.getDouble() * 8); /* average packet length in bits */
			op.setInputParameter("R", binaryRatePerTrafficUnit_bps.getDouble()); /* binary rate per traffic unit */
			op.addDecisionVariable("xx_p", false , new int[] { 1, P }, 0, 1); /* the FRACTION of traffic of demand d(p) that is carried by p */
			op.addDecisionVariable("y_e", false, new int[] { 1, E }, DoubleUtils.zeros(E), netPlan.getVectorLinkCapacity().toArray()); /* traffic in the links (already limited to the link capacity) */
			op.setObjectiveFunction("minimize", "sum( y_e .* (d_e_secs + (L./R) * (1 ./ (u_e - y_e)))  )");
			op.addConstraint("A_dp * xx_p' == 1"); /* for each demand, the 100% of the traffic is carried (summing the associated paths) */
			op.addConstraint("A_ep * (h_p .* xx_p)' == y_e'"); /* sets y_e as the traffic in link e */
		}
		else if (optimizationTarget.getString ().equals ("min-av-network-blocking"))
		{
			if (!solverName.getString ().equalsIgnoreCase("ipopt") || nonBifurcatedRouting.getBoolean()) throw new Net2PlanException ("This is a convex non linear model: please use IPOPT solver. The routing cannot be constrained to be non-bifurcated");
			op.addDecisionVariable("xx_p", false , new int[] { 1, P }, 0, 1); /* the FRACTION of traffic of demand d(p) that is carried by p */
			op.addDecisionVariable("y_e", false, new int[] { 1, E }, DoubleUtils.zeros(E), netPlan.getVectorLinkCapacity().toArray()); /* traffic in the links (already limited to the link capacity) */
			op.setObjectiveFunction("minimize", "sum(y_e .* erlangB(y_e, u_e))");
			op.addConstraint("A_dp * xx_p' == 1"); /* for each demand, the 100% of the traffic is carried (summing the associated paths) */
			op.addConstraint("A_ep * (h_p .* xx_p)' == y_e'"); /* sets y_e as the traffic in link e */
		}
		else throw new Net2PlanException ("Unknown optimization target " + optimizationTarget.getString());

		
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		
		/* Save the solution found in the netPlan object */
		final DoubleMatrix1D h_p = netPlan.getVectorRouteOfferedTrafficOfAssociatedDemand();
		final DoubleMatrix1D xx_p = DoubleFactory1D.dense.make (op.getPrimalSolution("xx_p").to1DArray());
		final DoubleMatrix1D x_p = xx_p.copy().assign (h_p , DoubleFunctions.mult);
		netPlan.setVectorRouteCarriedTrafficAndOccupiedLinkCapacities(x_p , x_p);
		netPlan.removeAllRoutesUnused(PRECISION_FACTOR); // routes with zero traffic (or close to zero, with PRECISION_FACTOR tolerance)

		return "Ok!: The solution found is guaranteed to be optimal: " + op.solutionIsOptimal() + ". Number routes = " + netPlan.getNumberOfRoutes();
	}

	@Override
	public String getDescription()
	{
		return "Given a network topology, the capacities in the links, and a set unicast traffic demands, this algorithm permits computing the optimum routing of the traffic (that is, the set of routes carrying the traffic of the demands) solving flow-path formulations, that start computing a set of admissible paths, and then solve the routing problem over them. Through a set of input parameters, the user can choose among different optimization targets and constraints.";
	}

	
	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
}
