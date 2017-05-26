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

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * Solves severals variants of routing problems in the form of destination-link formulations.
 * @net2plan.description
 * @net2plan.keywords JOM, Destination-link formulation, Flow assignment (FA)
 * @net2plan.ocnbooksections Section 4.4, Section 4.6.3
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_fa_xteFormulations implements IAlgorithm
{
	private InputParameter optimizationTarget = new InputParameter ("optimizationTarget", "#select# min-av-num-hops minimax-link-utilization maximin-link-idle-capacity min-av-network-delay min-av-network-blocking" , "Type of optimization target. Choose among minimize the average number of hops, minimize the highest link utilization, maximize the lowest link idle capacity, minimize the average end-to-end network delay including queueing (M/M/1 estimation) and propagation delays, and minimize the average network blocking assuming independent Erlang-B blocking in each link, load sharing model");
	private InputParameter solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter binaryRatePerTrafficUnit_bps = new InputParameter ("binaryRatePerTrafficUnit_bps", (double) 1E6 , "Binary rate equivalent to one traffic unit (used only in average network delay minimization formulation)." , 0 , false , Double.MAX_VALUE , true);
	private InputParameter averagePacketLengthInBytes = new InputParameter ("averagePacketLengthInBytes", (double) 500 , "Average packet length in bytes (used only in average network delay minimization formulation)." , 0 , false , Double.MAX_VALUE , true);
	private InputParameter nonBifurcatedRouting = new InputParameter ("nonBifurcatedRouting", false , "True if the routing is constrained to be non-bifurcated");

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		
		/* Initialize variables */
		final int N = netPlan.getNumberOfNodes ();
		final int E = netPlan.getNumberOfLinks();
		final int D = netPlan.getNumberOfDemands();
		if (E == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology with links and a demand set");

		/* Remove all routes in current netPlan object, and set routing type to SOURCE ROUTING */
		netPlan.removeAllUnicastRoutingInformation ();

		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();

		/* Set some input parameters to the problem */
		op.setInputParameter("u_e", netPlan.getVectorLinkSpareCapacity(), "row"); /* for each link, its unused capacity (the one not used by any mulitcast trees) */
		op.setInputParameter("A_ne", netPlan.getMatrixNodeLinkIncidence()); /* 1 in position (n,e) if link e starts in n, -1 if it ends in n, 0 otherwise */
		final DoubleMatrix1D egressTraffic_t = netPlan.getVectorNodeEgressUnicastTraffic();
		final DoubleMatrix2D trafficMatrixDiagonalNegative = netPlan.getMatrixNode2NodeOfferedTraffic();
		trafficMatrixDiagonalNegative.assign (DoubleFactory2D.sparse.diagonal(egressTraffic_t) , DoubleFunctions.minus);
		op.setInputParameter("TM", trafficMatrixDiagonalNegative);
		
		/* Write the problem formulations */
		if (optimizationTarget.getString ().equals ("min-av-num-hops")) 
		{
			op.addDecisionVariable("x_te", false , new int[] { N, E }, 0, Double.MAX_VALUE); /* the amount of traffic targeted to node t, that is carried by link e */
			if (nonBifurcatedRouting.getBoolean()) op.addDecisionVariable("f_te", true, new int [] {N,E} , 0 , 1);
			op.setObjectiveFunction("minimize", "sum (x_te)"); /* sum of the traffic in the links, proportional to the average number of hops  */
			op.addConstraint("A_ne * (x_te') == TM"); /* the flow-conservation constraints (NxD constraints) */
			op.addConstraint("sum(x_te,1) <= u_e"); /* the capacity constraints (E constraints) */
		}
		else if (optimizationTarget.getString ().equals ("minimax-link-utilization")) 
		{
			op.setInputParameter ("EPSILON" , getMinimumNonZeroTrafficOrCapacityValue (netPlan) / 1000);
			op.addDecisionVariable("x_te", false , new int[] { N, E }, 0, Double.MAX_VALUE); /* the amount of traffic targeted to node t, that is carried by link e */
			op.addDecisionVariable("rho", false, new int[] { 1, 1 }, 0, 1); /* worse case link utilization */
			if (nonBifurcatedRouting.getBoolean()) op.addDecisionVariable("f_te", true, new int [] {N,E} , 0 , 1);
			op.setObjectiveFunction("minimize", "rho + EPSILON * sum(x_te)"); // to avoid loops, we sum EPSILON by the traffic carried (EPSILON very small number)
			op.addConstraint("A_ne * (x_te') == TM"); /* the flow-conservation constraints (NxD constraints) */
			op.addConstraint("sum(x_te,1) <= rho * u_e"); /* the traffic in each link cannot exceed its capacity. sets rho as the worse case utilization */
		}
		else if (optimizationTarget.getString ().equals ("maximin-link-idle-capacity"))
		{
			op.setInputParameter ("EPSILON" , getMinimumNonZeroTrafficOrCapacityValue (netPlan) / 1000);
			op.addDecisionVariable("x_te", false , new int[] { N, E }, 0, Double.MAX_VALUE); /* the amount of traffic targeted to node t, that is carried by link e */
			op.addDecisionVariable("u", false, new int[] { 1, 1 }, 0, Double.MAX_VALUE); /* worse case link idle capacity */
			if (nonBifurcatedRouting.getBoolean()) op.addDecisionVariable("f_te", true, new int [] {N,E} , 0 , 1);
			op.setObjectiveFunction("maximize", "u - EPSILON * sum(x_te)"); // to avoid loops, we sum EPSILON by the traffic carried (EPSILON very small number)
			op.addConstraint("A_ne * (x_te') == TM"); /* the flow-conservation constraints (NxD constraints) */
			op.addConstraint("sum(x_te,1) <= -u + u_e"); /* the traffic in each link cannot exceed its capacity. sets u as the worse case idle capacity */
		}
		else if (optimizationTarget.getString ().equals ("min-av-network-delay"))
		{
			op.setInputParameter("d_e_secs", netPlan.getVectorLinkPropagationDelayInMiliseconds().assign (DoubleFunctions.mult (0.001)) , "row");
			op.setInputParameter("L", averagePacketLengthInBytes.getDouble() * 8); /* average packet length in bits */
			op.setInputParameter("R", binaryRatePerTrafficUnit_bps.getDouble()); /* binary rate per traffic unit */
			op.addDecisionVariable("x_te", false , new int[] { N, E }, 0, Double.MAX_VALUE); /* the amount of traffic targeted to node t, that is carried by link e */
			op.addDecisionVariable("y_e", false, new int[] { 1, E }, DoubleUtils.zeros(E), netPlan.getVectorLinkCapacity().toArray()); /* traffic in the links (already limited to the link capacity) */
			if (nonBifurcatedRouting.getBoolean()) op.addDecisionVariable("f_te", true, new int [] {N,E} , 0 , 1);
			op.setObjectiveFunction("minimize", "sum( y_e .* (d_e_secs + (L./R) * (1 ./ (u_e - y_e)))  )");
			op.addConstraint("A_ne * (x_te') == TM"); /* the flow-conservation constraints (NxD constraints) */
			op.addConstraint("sum(x_te,1) == y_e"); /* sets y_e as the total traffic in each link */
		}
		else if (optimizationTarget.getString ().equals ("min-av-network-blocking"))
		{
			op.addDecisionVariable("x_te", false , new int[] { N, E }, 0, Double.MAX_VALUE); /* the amount of traffic targeted to node t, that is carried by link e */
			op.addDecisionVariable("y_e", false, new int[] { 1, E }, DoubleUtils.zeros(E), netPlan.getVectorLinkCapacity().toArray()); /* traffic in the links (already limited to the link capacity) */
			if (nonBifurcatedRouting.getBoolean()) op.addDecisionVariable("f_te", true, new int [] {N,E} , 0 , 1);
			op.setObjectiveFunction("minimize", "sum(y_e .* erlangB(y_e, u_e))");
			op.addConstraint("A_ne * (x_te') == TM"); /* the flow-conservation constraints (NxD constraints) */
			op.addConstraint("sum(x_te,1) == y_e"); /* sets y_e as the total traffic in each link */
		}
		else throw new Net2PlanException ("Unknown optimization target " + optimizationTarget.getString());

		/* Constraints for non-bifurcated traffic */
		if (nonBifurcatedRouting.getBoolean())
		{
			DoubleMatrix2D maxNumOutLinksCarryingTraffic_nt = DoubleFactory2D.dense.make(N,N,1.0);
			for (int n = 0 ; n < N ; n ++) maxNumOutLinksCarryingTraffic_nt.set(n, n, 0);
			op.setInputParameter("U", netPlan.getVectorLinkCapacity().getMaxLocation() [0]);
			op.setInputParameter("Aout_ne", netPlan.getMatrixNodeLinkOutgoingIncidence());
			op.setInputParameter("outMax_nt", maxNumOutLinksCarryingTraffic_nt);
			op.addConstraint("x_te <= U * f_te"); /* f_te takes value 1 for non zero x_te */
			op.addConstraint ("Aout_ne * f_te' <= outMax_nt"); /* number of out links of node n carrying traffic to t is always below 1, and if n=t, it is zero */
		}

		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		
		/* Save the solution found in the netPlan object */
		final DoubleMatrix2D x_te = op.getPrimalSolution("x_te").view2D();
		netPlan.setRoutingFromDestinationLinkCarriedTraffic(x_te , true); // remove the cycles if any

		return "Ok!: The solution found is guaranteed to be optimal: " + op.solutionIsOptimal();
	}

	@Override
	public String getDescription()
	{
		return "Given a network topology, the capacities in the links, and a set unicast traffic demands, this algorithm permits computing the optimum destination-based routing of the traffic solving destination-link formulations (x_{te} variables). Recall that in destination-based routing, the nodes can only forward the traffic depending on its destination node, whatever its demand is (e.g. a node routes all the demands with the same egress node in the same form, whatever its ingress node is). Through a set of input parameters, the user can choose among different optimization targets and constraints.";
	}

	
	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	private double getMinimumNonZeroTrafficOrCapacityValue (NetPlan netPlan)
	{
		double res = Double.MAX_VALUE;
		for (Demand d : netPlan.getDemands ()) if (d.getOfferedTraffic() > 0) res = Math.min (res , d.getOfferedTraffic());
		for (Link e : netPlan.getLinks ()) if (e.getCapacity() > 0) res = Math.min (res , e.getCapacity());
		if (res == Double.MAX_VALUE) throw new Net2PlanException ("Too large offered traffics and link capacities");
		return res;
	}
}
