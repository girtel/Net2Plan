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

import cern.colt.matrix.tdouble.*;
import cern.jet.math.tdouble.DoubleFunctions;
import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Finds the multiperiod (e.g. subsequent years) routing and capacity acquisitions with a MILP formulation
 * @net2plan.description
 * @net2plan.keywords Capacity assignment (CA), Modular capacities, Flow assignment (FA), Multiperiod optimization, JOM
 * @net2plan.ocnbooksections Section 5.2.3
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_cfa_xpMultiperiodModularCapacities implements IAlgorithm
{
	private InputParameter rootOfNameOfInputTrafficFiles = new InputParameter ("rootOfNameOfInputTrafficFiles", "multiPeriodModularCapacities", "Root of the names of the traffic files. If the root is \"XXX\", the files are XXX_tm0.n2p, XXX_tm1.n2p, ...");
	private InputParameter rootOfNameOfOutputFiles = new InputParameter ("rootOfNameOfOutputFiles", "multiPeriodModularCapacities", "Root of the names of the output files. One per input traffic file. If the root is \"XXX\", the files are XXX_res_tm0.n2p, XXX_res_tm1.n2p, ...");
	private InputParameter k = new InputParameter ("k", (int) 5 , "Maximum number of admissible paths per demand" , 1 , Integer.MAX_VALUE);
	private InputParameter shortestPathType = new InputParameter ("shortestPathType", "#select# hops km" , "Criteria to compute the shortest path. Valid values: 'hops' or 'km'");
	private InputParameter nonBifurcatedRouting = new InputParameter ("nonBifurcatedRouting", false , "True if the routing is constrained to be non-bifurcated");
	private InputParameter maxLengthInKm = new InputParameter ("maxLengthInKm", (double) 2000 , "Paths longer than this are considered not admissible. A non-positive number means this limit does not exist");
	private InputParameter solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter costPerCapacityModuleType = new InputParameter ("costPerCapacityModuleType", "1 3 6", "The cost of each module of the given type");
	private InputParameter capacityOfEachCapacityModuleType = new InputParameter ("capacityOfEachCapacityModuleType", "10 40 100", "The capacity of each module of the given type");
	private InputParameter costReductionFactor = new InputParameter ("costReductionFactor", (double) 1 , "The cost of each element at period t is the cost at the previous period multiplied by this. Typically below one since things tend to decrease its price because of improvement in manufacturing" , 0 , true , Double.MAX_VALUE , true);

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		if (!shortestPathType.getString().equalsIgnoreCase("km") && !shortestPathType.getString().equalsIgnoreCase("hops"))
			throw new Net2PlanException("Wrong shortestPathType parameter");
		final DoubleMatrix1D u_k = DoubleFactory1D.dense.make (StringUtils.toDoubleArray(StringUtils.split(capacityOfEachCapacityModuleType.getString())));
		final DoubleMatrix1D c_k0 = DoubleFactory1D.dense.make (StringUtils.toDoubleArray(StringUtils.split(costPerCapacityModuleType.getString())));
		final int K = (int) u_k.size (); // number of types of capacity modules 
		if (K == 0) throw new Net2PlanException ("No capacity modules defined");
		if (c_k0.size() != K) throw new Net2PlanException ("The number of costs should be equal to the number of types of capacity modules");
		if (u_k.getMinLocation() [0] < 0) throw new Net2PlanException ("Capacities of the modules cannot be negative");
		if (c_k0.getMinLocation() [0] < 0) throw new Net2PlanException ("Costs of the modules cannot be negative");
		
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
		final DoubleMatrix1D linkCostVectorForCandidatePathList = shortestPathType.getString().equalsIgnoreCase("hops")? DoubleFactory1D.dense.make (E , 1.0) : netPlan.getVectorLinkLengthInKm();
		netPlan.addRoutesFromCandidatePathList(netPlan.computeUnicastCandidatePathList(linkCostVectorForCandidatePathList , k.getInt(), maxLengthInKm.getDouble(), -1, -1, -1, -1, -1 , null));
		
		final int P = netPlan.getNumberOfRoutes(); 
		
		/* Create the netPlan files, one per interval */
		ArrayList<NetPlan> netPlanFiles = new ArrayList<NetPlan> ();
		while (true)
		{
			try
			{ 
				DoubleMatrix2D thisIntervalTrafficMatrix = new NetPlan(new File (rootOfNameOfInputTrafficFiles.getString() + "_tm" + netPlanFiles.size () + ".n2p")).getMatrixNode2NodeOfferedTraffic(); 
				if (thisIntervalTrafficMatrix.rows () != N) throw new Net2PlanException ("The number of nodes in traffic matrix: " + rootOfNameOfInputTrafficFiles.getString() + "_tm" + netPlanFiles.size () + ".n2p (" + thisIntervalTrafficMatrix.rows() + ") is not correct (" + N + ")");
				NetPlan netPlanToAdd = netPlan.copy ();
				for (Demand d : netPlanToAdd.getDemands()) d.setOfferedTraffic(thisIntervalTrafficMatrix.get (d.getIngressNode().getIndex() , d.getEgressNode().getIndex()));
				netPlanFiles.add (netPlanToAdd);
			} catch (Exception e) { break; }
		}
		final int T = netPlanFiles.size();

		/* Compute the costs */
		final DoubleMatrix2D c_kt = DoubleFactory2D.dense.make (K,T);
		c_kt.viewColumn(0).assign (c_k0);
		for (int t = 1 ; t < T ; t ++)
			c_kt.viewColumn(t).assign (c_kt.viewColumn (t-1).copy ()).assign(DoubleFunctions.mult(costReductionFactor.getDouble()));

		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();

		/* Set some input parameters to the problem */
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
		op.setInputParameter("u_k", u_k , "row"); /* The capacity of each module of type k */
		op.setInputParameter("c_kt", c_kt); /* The cost of a module of type k, acquired to be used starting in interval t */
		op.setInputParameter("onesT", DoubleFactory1D.dense.make (T,1.0) , "row"); /* a vector of ones of size T */
		DoubleMatrix2D timeAccumulationMatrix = DoubleFactory2D.dense.make (T,T); for (int t1 = 0 ; t1 < T; t1 ++) for (int t2 = t1 ; t2 < T ; t2++) timeAccumulationMatrix.set(t1,t2,1.0); 
		op.setInputParameter("T_tt", timeAccumulationMatrix); /* 1 if column >= row: if the time of acquisition (row) is equal or higher than the time if observation (t2) */

		op.addDecisionVariable("xx_pt", nonBifurcatedRouting.getBoolean() , new int[] { P , T }, 0, 1); /* the FRACTION of traffic of demand d(p) that is carried by p in each time interval  */
		op.addDecisionVariable("a_ket", true , new int[] { K , E , T }, 0, 1); /* the number of elements of type k, acquired at time t, and placed at link e (in t and all intervals after t) */
		
		op.setObjectiveFunction("minimize", "sum (c_kt .* sum(a_ket,2)) "); /* sum of the cost of all the elements acquired, at the moment of acquisition */
		op.addConstraint("A_dp * xx_pt == 1"); /* for each demand, the 100% of the traffic is carried (summing the associated paths) in any time period */
		op.addConstraint("A_ep * (xx_pt .* h_pt) <= sum(u_k * a_ket,1) * T_tt"); /* the traffic in each link cannot exceed its capacity in any time period */

		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		
		/* Save the solution found in the netPlan object */
		final DoubleMatrix2D xx_pt = op.getPrimalSolution("xx_pt").view2D ();
		final DoubleMatrix3D a_ket = op.getPrimalSolution("a_ket").view3D("sparse");
		
		for (int t = 0 ; t < T ; t ++)
		{
			NetPlan thisNp = netPlanFiles.get(t);
			final DoubleMatrix1D h_p = thisNp.getVectorRouteOfferedTrafficOfAssociatedDemand();
			final DoubleMatrix1D x_p = xx_pt.viewColumn(t).copy().assign (h_p , DoubleFunctions.mult);
			//System.out.println ("h_p: " + h_p);
			thisNp.setVectorRouteCarriedTrafficAndOccupiedLinkCapacities(x_p , x_p);
			for (Link link : thisNp.getLinks ())
			{
				final int e = link.getIndex ();
				double linkCapacityAccumulatingPreviosModules = 0; for (int t1 = 0; t1 <= t ; t1 ++) for (int k = 0 ; k < K ; k ++) linkCapacityAccumulatingPreviosModules += u_k.get(k) * a_ket.get(k,e,t1);
				link.setCapacity(linkCapacityAccumulatingPreviosModules);
				for (int k = 0 ; k < K ; k ++) link.setAttribute ("numNewModulesType_" + k , "" + a_ket.get (k,e,t));
			}
			thisNp.removeAllRoutesUnused(PRECISION_FACTOR); // routes with zero traffic (or close to zero, with PRECISION_FACTOR tolerance)
			thisNp.saveToFile(new File (rootOfNameOfOutputFiles.getString() + "_res_tm" + netPlanFiles.size () + ".n2p"));
			if (t == 0) netPlan.assignFrom (thisNp);
			if (thisNp.getVectorLinkOversubscribedTraffic().zSum () > PRECISION_FACTOR) throw new RuntimeException ("Bad: " + thisNp.getVectorLinkOversubscribedTraffic().zSum ());
			if (thisNp.getVectorDemandBlockedTraffic().zSum() > PRECISION_FACTOR) throw new RuntimeException ("Bad: " + thisNp.getVectorDemandBlockedTraffic().zSum());
		}

		return "Ok!: The solution found is guaranteed to be optimal: " + op.solutionIsOptimal() + ". Total cost = " + op.parseExpression("sum (c_kt .* sum(a_ket,2))").evaluate("a_ket" , new DoubleMatrixND (a_ket));
	}

	@Override
	public String getDescription()
	{
		return "Given a network with a set of given nodes, and links, and a given a sequence of offered traffic matrices in the network, corresponding to the (typically increasing) traffic of successive periods (e.g. each for one year). The link capacities are constrained to be modular: selectable among a set of user-defined capacity modules. Each capacity module type is characterized by its capacity and its cost. We assume that the costs of the capacity modules decrease along time, according to a cost reduction factor. Then, the algorithm should find for each of the successive periods: (i) the routing of the traffic in each period, (ii) how many NEW modules of capacity are installed in each link. Once a capacity module is installed in a link, we assume that it is never moved. The optimization target is minimizing the total cost along all the periods. This algorithm optimizes the problem solving a flow-path formulation using JOM.";
	}

	
	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
}
