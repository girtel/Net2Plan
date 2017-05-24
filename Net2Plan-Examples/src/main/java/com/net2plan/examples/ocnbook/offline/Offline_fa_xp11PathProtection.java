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
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Solves several variants of unicast routing problems with 1+1 protection, with flow-path formulations
 * @net2plan.description
 * @net2plan.keywords JOM, Flow-path formulation, Flow assignment (FA), Network recovery: protection
 * @net2plan.ocnbooksections Section 4.2, Section 4.6.6 
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_fa_xp11PathProtection implements IAlgorithm
{
	private InputParameter k = new InputParameter ("k", (int) 5 , "Maximum number of admissible paths per demand" , 1 , Integer.MAX_VALUE);
	private InputParameter shortestPathType = new InputParameter ("shortestPathType", "#select# hops km" , "Criteria to compute the shortest path. Valid values: 'hops' or 'km'");
	private InputParameter maxLengthInKm = new InputParameter ("maxLengthInKm", (double) 2000 , "Paths longer than this are considered not admissible. A non-positive number means this limit does not exist");
	private InputParameter optimizationTarget = new InputParameter ("optimizationTarget", "#select# min-av-num-hops minimax-link-utilization maximin-link-idle-capacity" , "Type of optimization target. Choose among minimize the average number of hops, minimize the highest link utilization, maximize the lowest link idle capacity");
	private InputParameter solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter type11 = new InputParameter ("type11", "#select# linkDisjoint srgDisjoint" , "Type of 1+1 protection: 1+1 link disjoint (primary and backup paths have no link in common), and 1+1 srg disjoint (primary and backup paths have no SRG in common)");

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		if (!shortestPathType.getString().equalsIgnoreCase("km") && !shortestPathType.getString().equalsIgnoreCase("hops"))
			throw new Net2PlanException("Wrong shortestPathType parameter");
		if (type11.getString ().equalsIgnoreCase("srgDisjoint") && (netPlan.getNumberOfSRGs() == 0)) throw new Net2PlanException("No SRG was defined"); 

		/* Basic checks */
		final int N = netPlan.getNumberOfNodes();
		final int E = netPlan.getNumberOfLinks();
		final int D = netPlan.getNumberOfDemands();
		final int S = netPlan.getNumberOfSRGs();
		final double PRECISION_FACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));
		if (N == 0 || D == 0 || E == 0) throw new Net2PlanException("This algorithm requires a topology with links, and a demand set");

		/* Remove all unicast routed traffic. Any multicast routed traffic is kept */
		netPlan.removeAllUnicastRoutingInformation();
		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING);

		/* Add all the k-shortest candidate routes to the netPlan object carrying no traffic */
		final DoubleMatrix1D linkCostVector = shortestPathType.getString().equalsIgnoreCase("hops")? DoubleFactory1D.dense.make (E , 1.0) : netPlan.getVectorLinkLengthInKm();
		netPlan.addRoutesFromCandidatePathList(netPlan.computeUnicastCandidatePathList(linkCostVector , k.getInt(), maxLengthInKm.getDouble(), -1, -1, -1, -1, -1 , null));
		final int P = netPlan.getNumberOfRoutes(); 
		
		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();
		op.setInputParameter("u_e", netPlan.getVectorLinkSpareCapacity(), "row"); /* for each link, its unused capacity (the one not used by any mulitcast trees) */
		op.setInputParameter("A_ep", netPlan.getMatrixLink2RouteAssignment()); /* 1 in position (e,p) if link e is traversed by path p, 0 otherwise */
		op.setInputParameter("A_dp", netPlan.getMatrixDemand2RouteAssignment()); /* 1 in position (d,p) if demand d is served by path p, 0 otherwise */ 
		op.setInputParameter("h_d", netPlan.getVectorDemandOfferedTraffic(), "row"); /* for each demand, its offered traffic */
		op.setInputParameter("h_p", netPlan.getVectorRouteOfferedTrafficOfAssociatedDemand () , "row"); /* for each path, the offered traffic of its demand */

		if (type11.getString ().equalsIgnoreCase("linkDisjoint"))
			op.setInputParameter("A_ps", netPlan.getMatrixLink2RouteAssignment().viewDice ()); /* 1 in position (p,s) if link s is traversed by path p, 0 otherwise */
		else if (type11.getString ().equalsIgnoreCase("srgDisjoint"))
			op.setInputParameter("A_ps", netPlan.getMatrixRoute2SRGAffecting()); /* 1 in position (p,s) if srg s is affecting path p, 0 otherwise */
		
		/* Common decision variables */
		op.addDecisionVariable("xx_p", true , new int[] { 1, P }, 0, 1); /* 1 if primary path of demand d(p) that is carried by p */
		op.addDecisionVariable("bb_p", true , new int[] { 1, P }, 0, 1); /* 1 if backup path of demand d(p) that is carried by p */

		if (optimizationTarget.getString ().equals ("min-av-num-hops")) 
		{
			op.setInputParameter("l_p", netPlan.getVectorRouteNumberOfLinks() , "row"); /* for each path, the number of traversed links */
			op.setObjectiveFunction("minimize", "sum (l_p .* h_p .* (xx_p + bb_p))"); /* sum of the traffic in the links, proportional to the average number of hops  */
			op.addConstraint("A_ep * (h_p .* (xx_p + bb_p))' <= u_e'"); /* the traffic in each link cannot exceed its capacity  */
		}
		else if (optimizationTarget.getString ().equals ("minimax-link-utilization")) 
		{
			op.addDecisionVariable("rho", false, new int[] { 1, 1 }, 0, 1); /* worse case link utilization */
			op.setObjectiveFunction("minimize", "rho");
			op.addConstraint("A_ep * (h_p .* (xx_p + bb_p))' <= rho * u_e'"); /* the traffic in each link cannot exceed its capacity. sets rho as the worse case utilization */
		}
		else if (optimizationTarget.getString ().equals ("maximin-link-idle-capacity"))
		{
			op.addDecisionVariable("u", false, new int[] { 1, 1 }, 0, Double.MAX_VALUE); /* worse case link idle capacity */
			op.setObjectiveFunction("maximize", "u");
			op.addConstraint("A_ep * (h_p .* (xx_p + bb_p))' <= -u + u_e'"); /* the traffic in each link cannot exceed its capacity. sets u as the worse case idle capacity */
		}
		else throw new Net2PlanException ("Unknown optimization target " + optimizationTarget.getString());
		
		op.addConstraint("A_dp * xx_p' == 1"); /* for each demand, one primary path exists  */
		op.addConstraint("A_dp * bb_p' == 1"); /* for each demand, one backup path exists  */
		op.addConstraint("A_dp * diag(xx_p + bb_p) * A_ps  <= 1"); /* primary and backup are link or srg disjoint (depending on what is needed) */

		/* Call the solver to solve the problem */
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");

		/* Retrieve the optimum solutions */
		double [] xx_p = op.getPrimalSolution("xx_p").to1DArray();
		double [] bb_p = op.getPrimalSolution("bb_p").to1DArray();

		for (Demand d : netPlan.getDemands ())
		{
			Route primary = null; Route backup = null;
			for (Route r : d.getRoutes()) { if (xx_p [r.getIndex ()] == 1) primary = r; if (bb_p [r.getIndex ()] == 1) backup = r; }
			if (primary == null) throw new RuntimeException ("Bad");
			if (backup == null) throw new RuntimeException ("Bad");
			primary.setCarriedTraffic(d.getOfferedTraffic() , d.getOfferedTraffic());
			backup.setCarriedTraffic(0 , d.getOfferedTraffic());
			primary.addBackupRoute(backup);
		}

		netPlan.removeAllRoutesUnused(PRECISION_FACTOR); // routes with zero traffic (or close to zero, with PRECISION_FACTOR tolerance)
		
		checkSolution(netPlan, type11.getString ());

		return "Ok!";
	}

	@Override
	public String getDescription()
	{
		return "Given a network topology, the capacities in the links, and a set unicast traffic demands, this algorithm permits computing the optimum routing of the traffic with 1+1 protection to each route, solving a variations of the flow-path formulation. Initially, a set of admissible candidate paths are computed for each demand, which can be used as either primary or backup paths. Through a set of input parameters, the user can choose among different optimization targets and constraints.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
	
	private static void checkSolution(NetPlan netPlan, String type11)
	{
		if (!netPlan.getLinksOversubscribed().isEmpty()) throw new Net2PlanException("Bad - Some link is oversubscribed (constraint violated)");
		if (!netPlan.getDemandsBlocked().isEmpty()) throw new Net2PlanException("Bad - Some demand is blocked (constraint violated)");

		for (Route route : netPlan.getRoutesAreNotBackup())
		{
			if (route.getBackupRoutes().size () != 1) throw new RuntimeException("Bad");
			
			final Route backupRoute = route.getBackupRoutes().get(0);
			
			if (type11.equalsIgnoreCase("srgDisjoint"))
				if (!Collections.disjoint(route.getSRGs(), backupRoute.getSRGs()))
					throw new RuntimeException("Bad");
			else if (type11.equalsIgnoreCase("linkDisjoint"))
				if (!Collections.disjoint(route.getSeqLinks(), backupRoute.getSeqLinks()))
					throw new RuntimeException("Bad");
			else
				throw new RuntimeException ("Bad");
		}
	}
}
		
