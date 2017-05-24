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
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Solves several variants of unicast routing problems with 1+1 protection, with flow-link formulations
 * @net2plan.description
 * @net2plan.keywords JOM, Flow-path formulation, Flow assignment (FA), Network recovery: protection
 * @net2plan.ocnbooksections Section 4.3, Section 4.6.6
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_fa_xde11PathProtection implements IAlgorithm
{
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

		/* Basic checks */
		final int N = netPlan.getNumberOfNodes();
		final int E = netPlan.getNumberOfLinks();
		final int D = netPlan.getNumberOfDemands();
		if (N == 0 || D == 0 || E == 0) throw new Net2PlanException("This algorithm requires a topology with links, and a demand set");

		/* Remove all unicast routed traffic. Any multicast routed traffic is kept */
		netPlan.removeAllUnicastRoutingInformation();
		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
		
		/* Initialize some variables */
		DoubleMatrix1D u_e = netPlan.getVectorLinkSpareCapacity(); // just the unused capacity (some capacity may be used by multicast traffic)
		DoubleMatrix1D h_d = netPlan.getVectorDemandOfferedTraffic();
		
		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();
		op.setInputParameter("u_e", u_e, "row");
		op.setInputParameter("h_d", h_d, "row");
		op.setInputParameter("A_ne", netPlan.getMatrixNodeLinkIncidence());
		op.setInputParameter("A_nd", netPlan.getMatrixNodeDemandIncidence());

		if (type11.getString ().equalsIgnoreCase("linkDisjoint"))
		{
			/* Add the decision variables to the problem */
			op.addDecisionVariable("x_de", true, new int[] { D, E }, 0, 1); /* 1 if the primary path of demand d, traverses link e */
			op.addDecisionVariable("xx_de", true, new int[] { D, E }, 0, 1);  /* 1 if the backup path of demand d, traverses link e */
			
			/* Sets the objective function */
			if (optimizationTarget.getString ().equalsIgnoreCase("min-av-num-hops"))
			{
				op.setObjectiveFunction("minimize", "sum (h_d * (x_de + xx_de))");
				op.addConstraint("h_d * (x_de + xx_de) <= u_e"); /* the capacity constraints summing primary and backup paths (E constraints) */
			}
			else if (optimizationTarget.getString ().equalsIgnoreCase("minimax-link-utilization"))
			{
				op.setInputParameter ("EPSILON" , 1e-4);
				op.addDecisionVariable("rho", false, new int[] { 1, 1 }, 0, Double.MAX_VALUE); /* idle capacity in the bottleneck link */
				op.setObjectiveFunction("minimize", "rho + EPSILON * sum(x_de + xx_de)");
				op.addConstraint("h_d * (x_de + xx_de) <= rho * u_e"); /* the capacity constraints summing primary and backup paths (E constraints), make that rho becomes the worse case link utilization */
			}
			else if (optimizationTarget.getString ().equalsIgnoreCase("maximin-link-idle-capacity"))
			{
				op.setInputParameter ("EPSILON" , 1e-4);
				op.addDecisionVariable("u", false, new int[] { 1, 1 }, 0, Double.MAX_VALUE); /* idle capacity in the bottleneck link */
				op.setObjectiveFunction("maximize", "u - EPSILON * sum(x_de + xx_de)");
				op.addConstraint("h_d * (x_de + xx_de) <= u_e - u"); /* the capacity constraints summing primary and backup paths (E constraints) */
			}
			else throw new Net2PlanException ("Unknown optimization target " + optimizationTarget.getString());
			
			/* Add constraints */
			op.addConstraint("A_ne * (x_de') == A_nd"); /* the flow-conservation constraints for the primary path (NxD constraints) */
			op.addConstraint("A_ne * (xx_de') == A_nd"); /* the flow-conservation constraints for the backup path (NxD constraints) */
			op.addConstraint("x_de + xx_de <= 1"); /* the primary and backup path are link disjoint (DxE constraints) */
		}	
		else if (type11.getString ().equalsIgnoreCase("srgDisjoint"))
		{
			final int S = netPlan.getNumberOfSRGs();
			if (S == 0) throw new Net2PlanException("No SRG was defined");
			
			op.setInputParameter("A_es", netPlan.getMatrixLink2SRGAssignment());
			op.setInputParameter("E", E);
			
			/* Add the decision variables to the problem */
			op.addDecisionVariable("x_de", true, new int[] { D, E }, 0, 1); /* 1 if the primary path of demand d, traverses link e */
			op.addDecisionVariable("xx_de", true, new int[] { D, E }, 0, 1);  /* 1 if the backup path of demand d, traverses link e */
			op.addDecisionVariable("u", false, new int[] { 1, 1 }, 0, Double.MAX_VALUE); /* idle capacity in the bottleneck link */
			op.addDecisionVariable("x_ds", true, new int[] { D, S }, 0, 1); /* 1 if demand d traverses SRG s in the primary  */
			op.addDecisionVariable("xx_ds", true, new int[] { D, S }, 0, 1); /* 1 if demand d traverses SRG s in the backup */
			
			/* Sets the objective function */
			if (optimizationTarget.getString ().equalsIgnoreCase("min-av-num-hops"))
			{
				op.setObjectiveFunction("minimize", "sum (h_d * (x_de + xx_de))");
				op.addConstraint("h_d * (x_de + xx_de) <= u_e"); /* the capacity constraints summing primary and backup paths (E constraints) */
			}
			else if (optimizationTarget.getString ().equalsIgnoreCase("minimax-link-utilization"))
			{
				op.setInputParameter ("EPSILON" , 1e-4);
				op.addDecisionVariable("rho", false, new int[] { 1, 1 }, 0, Double.MAX_VALUE); /* idle capacity in the bottleneck link */
				op.setObjectiveFunction("minimize", "rho + EPSILON * sum(x_de + xx_de)");
				op.addConstraint("h_d * (x_de + xx_de) <= rho * u_e"); /* the capacity constraints summing primary and backup paths (E constraints), make that rho becomes the worse case link utilization */
			}
			else if (optimizationTarget.getString ().equalsIgnoreCase("maximin-link-idle-capacity"))
			{
				op.setInputParameter ("EPSILON" , 1e-4);
				op.addDecisionVariable("u", false, new int[] { 1, 1 }, 0, Double.MAX_VALUE); /* idle capacity in the bottleneck link */
				op.setObjectiveFunction("maximize", "u - EPSILON * sum (x_de + xx_de)"); 
				op.addConstraint("h_d * (x_de + xx_de) <= u_e - u"); /* the capacity constraints summing primary and backup paths (E constraints) */
			}
			else throw new Net2PlanException ("Unknown optimization target " + optimizationTarget.getString());

			/* Add constraints */
			op.addConstraint("A_ne * (x_de') == A_nd"); /* the flow-conservation constraints for the primary path (NxD constraints) */
			op.addConstraint("A_ne * (xx_de') == A_nd"); /* the flow-conservation constraints for the backup path (NxD constraints) */
			op.addConstraint("h_d * (x_de + xx_de) <= u_e - u"); /* the capacity constraints summing primary and backup paths (E constraints) */
			op.addConstraint("E * x_ds >= x_de * A_es"); /* 1 if demand s traverses SRG s in the primary (DxS constraints) */
			op.addConstraint("E * xx_ds >= xx_de * A_es"); /* 1 if demand s traverses SRG s in the nackup (DxS constraints) */
			op.addConstraint("x_ds + xx_ds <= 1"); /* the primary and backup path are SRG disjoint (DxS constraints) */
		}
		else throw new Net2PlanException("Wrong type of 1:1 protection");
		
		/* Call the solver to solve the problem */
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");

		/* Retrieve the optimum solutions */
		DoubleMatrix2D x_de = op.getPrimalSolution("x_de").view2D();
		DoubleMatrix2D xx_de = op.getPrimalSolution("xx_de").view2D();

		/* Convert the x_de variables into a set of routes for each demand */
		List<Demand> primary_demands = new ArrayList<Demand>();
		List<List<Link>> primary_seqLinks = new ArrayList<List<Link>>();
		List<Double> primary_x_p = new ArrayList<Double>();
		GraphUtils.convert_xde2xp(netPlan.getNodes (), netPlan.getLinks () , netPlan.getDemands () , x_de, primary_demands, primary_x_p, primary_seqLinks);
		
		List<Demand> backup_demands = new ArrayList<Demand>();
		List<List<Link>> backup_seqLinks = new ArrayList<List<Link>>();
		List<Double> backup_x_p = new ArrayList<Double>();
		GraphUtils.convert_xde2xp(netPlan.getNodes (), netPlan.getLinks () , netPlan.getDemands () , xx_de, backup_demands, backup_x_p, backup_seqLinks);

		/* Update netPlan object adding the calculated routes */
		if (primary_demands.size() != D) throw new Net2PlanException("Unexpected error");
		if (backup_demands.size() != D) throw new Net2PlanException("Unexpected error");
		
		for (Demand demand : netPlan.getDemands())
		{
			/* for each demand, there is one primary and one backup path*/
			int primary_p = primary_demands.indexOf(demand);
			if (primary_p == -1) throw new RuntimeException("Unexpected error");

			int backup_p = backup_demands.indexOf(demand);
			if (backup_p == -1) throw new RuntimeException("Unexpected error");
			
			final Route route = netPlan.addRoute(demand , demand.getOfferedTraffic() , demand.getOfferedTraffic() , primary_seqLinks.get(primary_p), null); /* add the primary route (primary path) */
			final Route backupRoute = netPlan.addRoute (demand , 0.0 , demand.getOfferedTraffic() , backup_seqLinks.get(backup_p), null); /* add the protection segment (backup path) */
			route.addBackupRoute(backupRoute);
		}

		checkSolution(netPlan, type11.getString ());

		return "Ok!";
	}

	@Override
	public String getDescription()
	{
		return "Given a network topology, the capacities in the links, and a set unicast traffic demands, this algorithm permits computing the optimum routing of the traffic with 1+1 protection to each route, solving a variations of the flow-link formulation. Through a set of input parameters, the user can choose among different optimization targets and constraints.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
	
	/**
	 * Checks if solution is valid.
	 * 
	 * @param netPlan Network design
	 * @param type11 Type of 1:1 protection
	 * @since 1.1
	 */
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
		
