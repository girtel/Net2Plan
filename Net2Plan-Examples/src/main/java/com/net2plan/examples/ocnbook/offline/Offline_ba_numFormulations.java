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
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.List;
import java.util.Map;

/** 
 * Solves the congestion control problem using a NUM formulation.
 * @net2plan.description 
 * @net2plan.keywords Bandwidth assignment (BA), JOM, NUM, TCP
 * @net2plan.ocnbooksections Section 6.2, Section 6.3
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_ba_numFormulations implements IAlgorithm
{
	private InputParameter shortestPathType = new InputParameter ("shortestPathType", "#select# hops km" , "Criteria to compute the shortest path. Valid values: 'hops' or 'km'");
	private InputParameter alphaFairnessFactor = new InputParameter ("alphaFairnessFactor", (double) 2 , "Fairness alpha factor" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter utilityFunctionType = new InputParameter ("utilityFunctionType", "#select# alphaFairness TCP-Reno TCP-Vegas", "The type of utility function of each demand, in the NUM problem to solve: standard alpha-fairness (alpha parameter is given by alphaFairnessFactor), an utility function suitable for TCP-Reno NUM model (each demand is a TCP connection) and the same for TCP-Vegas");
	private InputParameter solverName = new InputParameter ("solverName", "#select# ipopt glpk xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		if (!shortestPathType.getString().equalsIgnoreCase("km") && !shortestPathType.getString().equalsIgnoreCase("hops"))
			throw new Net2PlanException("Wrong shortestPathType parameter");
		
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
		
		netPlan.addRoutesFromCandidatePathList(netPlan.computeUnicastCandidatePathList(linkCostVector , 1 , -1, -1, -1, -1, -1, -1, null)); // one route per demand, so P equals D
		final int P = netPlan.getNumberOfRoutes(); 

		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();

		/* Set some input parameters to the problem */
		op.setInputParameter("u_e", netPlan.getVectorLinkSpareCapacity(), "row"); /* for each link, its unused capacity (the one not used by any mulitcast trees) */
		op.setInputParameter("A_ep", netPlan.getMatrixLink2RouteAssignment()); /* 1 in position (e,p) if link e is traversed by path p, 0 otherwise */
		op.addDecisionVariable("h_p", false , new int[] { 1, P }, 0, Double.MAX_VALUE); /* the traffic carried by the demand in the macroscopic equilibrium */
		op.addConstraint("A_ep * h_p' <= u_e'" , "pi_e"); // the capacity constraints (E constraints)

		/* Write the problem formulations */
		if (utilityFunctionType.getString ().equals ("alphaFairness")) 
		{
			op.setInputParameter("oneMinusAlpha", 1-alphaFairnessFactor.getDouble());
			if (alphaFairnessFactor.getDouble() == 1)
		    op.setObjectiveFunction("maximize", "sum(ln(h_p))");
			else if (alphaFairnessFactor.getDouble() == 0)
			    op.setObjectiveFunction("maximize", "sum(h_p)");
			else
			    op.setObjectiveFunction("maximize", "(1/oneMinusAlpha) * sum(h_p ^ oneMinusAlpha)");
	
		}
		else if (utilityFunctionType.getString ().equals ("TCP-Reno"))
		{
			op.setInputParameter("rtt_p", netPlan.getVectorRoutePropagationDelayInMiliseconds().assign (DoubleFunctions.mult (2)) , "row"); /* round-trip-time in miliseconds for each connection */
			op.setObjectiveFunction("maximize", "sum (-1.5 / (rtt_p .* h_p))");
		}
		else if (utilityFunctionType.getString ().equals ("TCP-Vegas"))
		{
	    op.setObjectiveFunction("maximize", "sum(ln(h_p))");
		}
		else throw new Net2PlanException ("Unknown optimization target " + utilityFunctionType.getString());

		/* Call the solver to solve the problem */
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () ,  "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (op.foundUnboundedSolution()) throw new Net2PlanException ("Found an unbounded solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		
		/* Save the solution found in the netPlan object */
		final DoubleMatrix1D h_p = op.getPrimalSolution("h_p").view1D();
		netPlan.setVectorRouteCarriedTrafficAndOccupiedLinkCapacities(h_p , h_p);
		for (Demand d : netPlan.getDemands ()) d.setOfferedTraffic(d.getCarriedTraffic()); // make the offered traffic equal to the carried traffic

//		System.out.println ("Total carried traffic: " + netPlan.getVectorDemandOfferedTraffic().zSum());
//		System.out.println ("Network utility: " + op.getOptimalCost());
		
		return "Ok!: The solution found is guaranteed to be optimal: " + op.solutionIsOptimal() + ". Total carried traffic: " + netPlan.getVectorDemandOfferedTraffic().zSum() + ", Network utility: " + op.getOptimalCost();
	}

	@Override
	public String getDescription()
	{
		return "Given a network topology, the capacities in the links, and the set of unicast demands, this algorithm assigns the shortest path route to each demand, then optimizes the traffic to inject by each demand (that is, assigns bandwidth), with different optimization targets based on some form of fairness in resource allocation";
	}

	
	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
}
