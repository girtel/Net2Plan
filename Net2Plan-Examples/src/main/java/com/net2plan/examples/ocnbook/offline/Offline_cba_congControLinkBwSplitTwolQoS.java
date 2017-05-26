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
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * In a network with demands of two QoS, jointly optimizes the demand injected traffic and link capacity assigned to each solving a formulation.
 * @net2plan.description
 * @net2plan.keywords Bandwidth assignment (BA), Capacity assignment (CA), JOM, NUM 
 * @net2plan.ocnbooksections Section 11.3, Section 6.2
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_cba_congControLinkBwSplitTwolQoS implements IAlgorithm
{
	private double PRECISIONFACTOR;
	private InputParameter solverName = new InputParameter ("solverName", "#select# ipopt", "The solver name to be used by JOM. ");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter cc_control_minHd = new InputParameter ("cc_control_minHd", 0.1 , "Minimum traffic assigned to each demand" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_control_maxHd = new InputParameter ("cc_control_maxHd", 1e6 , "Maximum traffic assigned to each demand" , 0 , true , Double.MAX_VALUE , true);

	private InputParameter cc_control_fairnessFactor_1 = new InputParameter ("cc_control_fairnessFactor_1", 1.0 , "Fairness factor in utility function of congestion control for demands of class 1" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_control_fairnessFactor_2 = new InputParameter ("cc_control_fairnessFactor_2", 1.0 , "Fairness factor in utility function of congestion control for demands of class 2" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_control_weightFairness_1 = new InputParameter ("cc_control_weightFairness_1", 1.0 , "Weight factor in utility function demands type 1" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_control_weightFairness_2 = new InputParameter ("cc_control_weightFairness_2", 2.0 , "Weight factor in utility function demands type 2" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter mac_minCapacity_1 = new InputParameter ("mac_minCapacity_1", 0.1 , "Minimum capacity in each link, allocated to traffic of type 1" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter mac_minCapacity_2 = new InputParameter ("mac_minCapacity_2", 0.1 , "Minimum capacity in each link, allocated to traffic of type 2" , 0 , true , Double.MAX_VALUE , true);
	
	private int N,E,D;
	private DoubleMatrix1D demandType;

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		if (netPlan.getNumberOfLayers() != 1) throw new Net2PlanException ("This algorithm works in single layer networks");

		this.PRECISIONFACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));
		this.N = netPlan.getNumberOfNodes();
		this.E = netPlan.getNumberOfLinks();
		this.D = 2*N*(N-1);
		
		/* Remove all demands, then create a demand per input output node pair. One route for it  */
		netPlan.removeAllDemands();
		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
		List<Integer> arrayIndexesOfDemand1 = new LinkedList<Integer> ();
		List<Integer> arrayIndexesOfDemand2 = new LinkedList<Integer> ();
		this.demandType = DoubleFactory1D.dense.make (D);
		for (Node n1 : netPlan.getNodes())
			for (Node n2 : netPlan.getNodes())
				if (n1 != n2) 
				{ 
					final Demand d1 = netPlan.addDemand(n1, n2, 0.0, null); d1.setAttribute("type" , "1"); demandType.set(d1.getIndex (), 1); 
					final Demand d2 = netPlan.addDemand(n1, n2, 0.0, null); d2.setAttribute("type" , "2"); demandType.set(d2.getIndex (), 2); 
					arrayIndexesOfDemand1.add (d1.getIndex ());
					arrayIndexesOfDemand2.add (d2.getIndex ());
				}

		/* Remove all routes, and create one with the shortest path in km for each demand */
		netPlan.removeAllUnicastRoutingInformation();
		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
		netPlan.addRoutesFromCandidatePathList(netPlan.computeUnicastCandidatePathList(netPlan.getVectorLinkLengthInKm() , 1 , -1, -1, -1, -1, -1, -1, null)); // one route per demand, so P equals D

		
		
		/* Make the formulation  */
		final int E = netPlan.getNumberOfLinks();
		final int D = netPlan.getNumberOfDemands();
		final DoubleMatrix1D u_e = netPlan.getVectorLinkCapacity();
		OptimizationProblem op = new OptimizationProblem();
		
		op.setInputParameter("b1", 1-cc_control_fairnessFactor_1.getDouble());
		op.setInputParameter("b2", 1-cc_control_fairnessFactor_2.getDouble());
		op.setInputParameter("w1", cc_control_weightFairness_1.getDouble());
		op.setInputParameter("w2", cc_control_weightFairness_2.getDouble());
		op.setInputParameter("R_de", netPlan.getMatrixDemand2LinkAssignment());
		op.setInputParameter("u_e", u_e , "row");
		op.setInputParameter("D1", arrayIndexesOfDemand1 , "row");
		op.setInputParameter("D2", arrayIndexesOfDemand2 , "row");

		op.addDecisionVariable("h_d", false , new int[] { 1 , D }, cc_control_minHd.getDouble() , cc_control_maxHd.getDouble());
		op.addDecisionVariable("u_1", false , new int[] { 1 , E }, DoubleFactory1D.dense.make (E , mac_minCapacity_1.getDouble()) , u_e);
		op.addDecisionVariable("u_2", false , new int[] { 1 , E }, DoubleFactory1D.dense.make (E , mac_minCapacity_2.getDouble()) , u_e);

		String objFunc = "";
		if (cc_control_fairnessFactor_1.getDouble() == 1) 
			objFunc += "w1*sum(ln(h_d(D1)))";
		else
			objFunc += "(w1/b1) * sum (   h_d(D1)^b1 ) ";
		if (cc_control_fairnessFactor_2.getDouble() == 1) 
			objFunc += " + w2*sum(ln(h_d(D2)))";
		else
			objFunc += " + (w2/b2) * sum (   h_d(D2)^b2 ) ";

		op.setObjectiveFunction("maximize", objFunc);

		op.addConstraint("h_d(D1) * R_de(D1,all) <= u_1" , "pie_1");
		op.addConstraint("h_d(D2) * R_de(D2,all) <= u_2" , "pie_2");
		op.addConstraint("u_1 + u_2 <= u_e");

		/* Call the solver to solve the problem */
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (op.foundUnboundedSolution()) throw new Net2PlanException ("Found an unbounded solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
	
		/* Retrieve the optimum solutions. Convert the bps into Erlangs */
		DoubleMatrix1D h_d = op.getPrimalSolution("h_d").view1D ();
		DoubleMatrix1D u_1 = op.getPrimalSolution("u_1").view1D ();
		DoubleMatrix1D u_2 = op.getPrimalSolution("u_2").view1D ();
		DoubleMatrix1D pie_1 = op.getMultipliersOfConstraint("pie_1").assign(DoubleFunctions.abs).view1D ();
		DoubleMatrix1D pie_2 = op.getMultipliersOfConstraint("pie_2").assign(DoubleFunctions.abs).view1D ();

		/* Set the demand offered and carried traffic */
		for (Demand d : netPlan.getDemands ())
		{
			final double thisDemand_hd = h_d.get(d.getIndex ());
			d.setOfferedTraffic(thisDemand_hd);
			d.getRoutes().iterator().next().setCarriedTraffic(thisDemand_hd , thisDemand_hd);
		}
		/* Set the link capacity split and multipliers */
		for (Link e : netPlan.getLinks())
		{
			e.setAttribute("u_1", "" + u_1.get(e.getIndex ()));
			e.setAttribute("u_2", "" + u_2.get(e.getIndex ()));
			e.setAttribute("pie_1", "" + pie_1.get(e.getIndex ()));
			e.setAttribute("pie_2", "" + pie_2.get(e.getIndex ()));
		}
		
		/* Check link capacities */
		for (Link e : netPlan.getLinks())
		{
			double traf1  = 0 ; double traf2 = 0;
			for (Route r : e.getTraversingRoutes())
				if (demandType.get(r.getDemand().getIndex ()) == 1) traf1 += r.getCarriedTraffic(); else traf2 += r.getCarriedTraffic();  
			if (traf1 > u_1.get (e.getIndex ())  + PRECISIONFACTOR) throw new RuntimeException ("Bad");
			if (traf2 > u_2.get (e.getIndex ())  + PRECISIONFACTOR) throw new RuntimeException ("Bad");
			if (u_1.get(e.getIndex ())  + u_2.get(e.getIndex ()) > e.getCapacity() + PRECISIONFACTOR) throw new RuntimeException ("Bad");
		}

		/* Compute the network utility */
		double netUtil = 0;
		for (Demand d : netPlan.getDemands())
		{
			final double thisDemand_hd = h_d.get(d.getIndex ());
			if (d.getRoutes ().size() != 1) throw new RuntimeException ("Bad");
			if (Math.abs (d.getCarriedTraffic() - d.getOfferedTraffic()) > 1e-3) throw new RuntimeException ("Bad");
			if (demandType.get(d.getIndex ()) == 1)
				netUtil += cc_control_weightFairness_1.getDouble () * ((cc_control_fairnessFactor_1.getDouble () == 1)? Math.log(thisDemand_hd) : 1/(1-cc_control_fairnessFactor_1.getDouble ()) * Math.pow(thisDemand_hd, 1-cc_control_fairnessFactor_1.getDouble ()));
			else
				netUtil += cc_control_weightFairness_2.getDouble () * ((cc_control_fairnessFactor_2.getDouble () == 1)? Math.log(thisDemand_hd) : 1/(1-cc_control_fairnessFactor_2.getDouble ()) * Math.pow(thisDemand_hd, 1-cc_control_fairnessFactor_2.getDouble ()));
		}

		netPlan.setAttribute("netUtility" , "" + netUtil);
		
		return "Ok! Optimal net utility: " + netUtil;
	}

	@Override
	public String getDescription()
	{
		return "Given a network with a set of given nodes. The algorithm first creates two demands for each node pair, one of traffic type 1 and one of traffic type 2. The routing of each demand is known. A shortest-path route is assigned to each demand. We assume that, in order to enforce a strict QoS to the two types of traffic, the capacity in each link is split into two parts, one for each traffic type. Then, this algorithm will jointly optimize: (i) the traffic to inject by each demand, and (ii) the capacity in each link assigned to traffic of class 1 and class 2. The solution is found solving a problem formulation with JOM. The optimization target is finding a fair allocation using the NUM model, where the utility functions of demands of type 1 and type 2 are different.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
}
