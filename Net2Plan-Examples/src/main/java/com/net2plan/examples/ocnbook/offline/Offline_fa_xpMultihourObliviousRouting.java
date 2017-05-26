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
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Solves a multihour routing problem with oblivious routing (common routing in all the time intervals) using a flow-path formulation
 * @net2plan.description
 * @net2plan.keywords Flow assignment (FA), Flow-path formulation, JOM, Multihour optimization
 * @net2plan.ocnbooksections Section 4.6.8.1
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_fa_xpMultihourObliviousRouting implements IAlgorithm
{
	private InputParameter rootOfNameOfInputTrafficFiles = new InputParameter ("rootOfNameOfInputTrafficFiles", "multiHourObliviousRouting", "Root of the names of the traffic files. If the root is \"XXX\", the files are XXX_tm0.n2p, XXX_tm1.n2p, ...");
	private InputParameter rootOfNameOfOutputFiles = new InputParameter ("rootOfNameOfOutputFiles", "multiHourObliviousRouting", "Root of the names of the output files. One per input traffic file. If the root is \"XXX\", the files are XXX_res_tm0.n2p, XXX_res_tm1.n2p, ...");
	private InputParameter k = new InputParameter ("k", (int) 5 , "Maximum number of admissible paths per demand" , 1 , Integer.MAX_VALUE);
	private InputParameter shortestPathType = new InputParameter ("shortestPathType", "#select# hops km" , "Criteria to compute the shortest path. Valid values: 'hops' or 'km'");
	private InputParameter nonBifurcatedRouting = new InputParameter ("nonBifurcatedRouting", false , "True if the routing is constrained to be non-bifurcated");
	private InputParameter optimizationTarget = new InputParameter ("optimizationTarget", "#select# min-av-num-hops minimax-link-utilization maximin-link-idle-capacity" , "Type of optimization target. Choose among minimize the average number of hops, minimize the highest link utilization, maximize the lowest link idle capacity");
	private InputParameter maxLengthInKm = new InputParameter ("maxLengthInKm", (double) 2000 , "Paths longer than this are considered not admissible. A non-positive number means this limit does not exist");
	private InputParameter solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		if (solverName.getString ().equalsIgnoreCase("ipopt") && nonBifurcatedRouting.getBoolean()) throw new Net2PlanException ("With IPOPT solver, the routing cannot be constrained to be non-bifurcated");
		if (!shortestPathType.getString().equalsIgnoreCase("km") && !shortestPathType.getString().equalsIgnoreCase("hops"))
			throw new Net2PlanException("Wrong shortestPathType parameter");
		
		/* Initialize variables */
		final int N = netPlan.getNumberOfNodes ();
		final int E = netPlan.getNumberOfLinks ();
		final double PRECISION_FACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));
		if (E == 0) throw new Net2PlanException("This algorithm requires a topology with links");

		/* Remove all unicast routed traffic. Any multicast routed traffic is kept */
		netPlan.removeAllUnicastRoutingInformation();
		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
		netPlan.setTrafficMatrix(DoubleFactory2D.dense.make (N,N,1.0)); // just to create the demands

		/* Add all the k-shortest candidate routes to the netPlan object carrying no traffic */
		final DoubleMatrix1D linkCostVector = shortestPathType.getString().equalsIgnoreCase("hops")? DoubleFactory1D.dense.make (E , 1.0) : netPlan.getVectorLinkLengthInKm();
		netPlan.addRoutesFromCandidatePathList(netPlan.computeUnicastCandidatePathList(linkCostVector , k.getInt(), maxLengthInKm.getDouble(), -1, -1, -1, -1, -1 , null));
		final int P = netPlan.getNumberOfRoutes(); 

		/* Create the netPlan files, one per interval */
		ArrayList<NetPlan> netPlanFiles = new ArrayList<NetPlan> ();
		while (true)
		{
			try
			{ 
				DoubleMatrix2D thisIntervalTrafficMatrix = new NetPlan(new File (rootOfNameOfInputTrafficFiles.getString() + "_tm" + netPlanFiles.size () + ".n2p")).getMatrixNode2NodeOfferedTraffic(); 
				NetPlan netPlanToAdd = netPlan.copy ();
				for (Demand d : netPlanToAdd.getDemands()) d.setOfferedTraffic(thisIntervalTrafficMatrix.get (d.getIngressNode().getIndex() , d.getEgressNode().getIndex()));
				netPlanFiles.add (netPlanToAdd);
			} catch (Exception e) {  break; }
		}
		final int T = netPlanFiles.size();
		//System.out.println ("Number of traffic matrices loaded: " + netPlanFiles.size ());

		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();

		/* Set some input parameters to the problem */
		op.setInputParameter("u_e", netPlan.getVectorLinkSpareCapacity(), "row"); /* for each link, its unused capacity (the one not used by any mulitcast trees) */
		op.setInputParameter("A_dp", netPlan.getMatrixDemand2RouteAssignment()); /* 1 in position (d,p) if demand d is served by path p, 0 otherwise */ 
		op.setInputParameter("A_ep", netPlan.getMatrixLink2RouteAssignment()); /* 1 in position (e,p) if link e is traversed by path p, 0 otherwise */
		DoubleMatrix2D h_dt = DoubleFactory2D.dense.make (N*(N-1),T);
		DoubleMatrix2D h_pt = DoubleFactory2D.dense.make (P,T);
		for (int t = 0; t < T ; t ++) 
		{ 
			h_dt.viewColumn(t).assign (netPlanFiles.get(t).getVectorDemandOfferedTraffic());
			h_pt.viewColumn(t).assign (netPlanFiles.get(t).getVectorRouteOfferedTrafficOfAssociatedDemand());
		}
		op.setInputParameter("h_dt", h_dt); /* for each demand and time interval , its offered traffic */
		op.setInputParameter("h_pt", h_pt); /* for each path and time interval , the offered traffic of its demand */
		op.setInputParameter("onesT", DoubleFactory1D.dense.make (T,1.0) , "row"); /* a vector of ones of size T */

		/* Write the problem formulations */
		if (optimizationTarget.getString ().equals ("min-av-num-hops")) 
		{
			op.setInputParameter("l_p", netPlan.getVectorRouteNumberOfLinks() , "row"); /* for each path, the number of traversed links */
			op.addDecisionVariable("xx_p", nonBifurcatedRouting.getBoolean() , new int[] { 1 , P }, 0, 1); /* the FRACTION of traffic of demand d(p) that is carried by p (the same in all time intervals)  */
			op.setObjectiveFunction("minimize", "sum (diag (l_p .* xx_p) * h_pt)"); /* sum of the traffic in the links in all the time intervals, proportional to the average number of hops  */
			op.addConstraint("A_dp * xx_p' == 1"); /* for each demand, the 100% of the traffic is carried (summing the associated paths) */
			op.addConstraint("A_ep * (diag (xx_p) * h_pt) <= u_e' * onesT"); /* the traffic in each link cannot exceed its capacity in any time slot */
		}
		else if (optimizationTarget.getString ().equals ("minimax-link-utilization")) 
		{
			op.addDecisionVariable("xx_p", nonBifurcatedRouting.getBoolean() , new int[] { 1 , P }, 0, 1); /* the FRACTION of traffic of demand d(p) that is carried by p (the same in all time intervals)  */
			op.addDecisionVariable("rho", false, new int[] { 1, 1 }, 0, 1); /* worse case link utilization */
			op.setObjectiveFunction("minimize", "rho");
			op.addConstraint("A_dp * xx_p' == 1"); /* for each demand, the 100% of the traffic is carried (summing the associated paths) */
			op.addConstraint("A_ep * (diag (xx_p) * h_pt) <= rho * u_e' * onesT"); /* the traffic in each link cannot exceed its capacity. sets rho as the worse case utilization in any time slot */
		}
		else if (optimizationTarget.getString ().equals ("maximin-link-idle-capacity"))
		{
			op.addDecisionVariable("xx_p", nonBifurcatedRouting.getBoolean() , new int[] { 1 , P }, 0, 1); /* the FRACTION of traffic of demand d(p) that is carried by p (the same in all time intervals)  */
			op.addDecisionVariable("u", false, new int[] { 1, 1 }, 0, Double.MAX_VALUE); /* worse case link idle capacity */
			op.setObjectiveFunction("maximize", "u");
			op.addConstraint("A_dp * xx_p' == 1"); /* for each demand, the 100% of the traffic is carried (summing the associated paths) */
			op.addConstraint("A_ep * (diag (xx_p) * h_pt) <= -u + (u_e' * onesT)"); /* the traffic in each link cannot exceed its capacity. sets u as the worse case idle capacity in all time intervals */
		}
		else throw new Net2PlanException ("Unknown optimization target " + optimizationTarget.getString());

		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		
		/* Save the solution found in the netPlan object */
		final DoubleMatrix1D xx_p = DoubleFactory1D.sparse.make (op.getPrimalSolution("xx_p").to1DArray());
		for (int t = 0 ; t < T ; t ++)
		{
			NetPlan thisNp = netPlanFiles.get(t);
			final DoubleMatrix1D h_p = thisNp.getVectorRouteOfferedTrafficOfAssociatedDemand();
			final DoubleMatrix1D x_p = xx_p.copy().assign (h_p , DoubleFunctions.mult);
			thisNp.setVectorRouteCarriedTrafficAndOccupiedLinkCapacities(x_p , x_p);
			thisNp.removeAllRoutesUnused(PRECISION_FACTOR); // routes with zero traffic (or close to zero, with PRECISION_FACTOR tolerance)
			thisNp.saveToFile(new File (rootOfNameOfOutputFiles.getString() + "_res_tm" + netPlanFiles.size () + ".n2p"));
			if (t == 0) netPlan.assignFrom (thisNp);
			if (!thisNp.getLinksOversubscribed().isEmpty()) throw new RuntimeException ("Bad");
			if (thisNp.getVectorDemandBlockedTraffic().zSum() > PRECISION_FACTOR) throw new RuntimeException ("Bad: " + thisNp.getVectorDemandBlockedTraffic().zSum());
		}

		return "Ok!: The solution found is guaranteed to be optimal: " + op.solutionIsOptimal() + ". Number routes = " + netPlan.getNumberOfRoutes();
	}

	@Override
	public String getDescription()
	{
		return "Given a set of traffic matrices, corresponding to the offered network traffic at different times of the day, this algorithm solves a flow-path formulation for finding the oblivious routing (common routing to all the time intervals) that optimizes the selected optimization target.";
	}

	
	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
}
