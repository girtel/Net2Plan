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

import cern.colt.list.tdouble.DoubleArrayList;
import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.DoubleMatrix3D;
import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * Solves several variants of unicast routing problems with flow-link formulations, so that designs are fault tolerant to a set of failure states, using shared restoration
 * @net2plan.description
 * @net2plan.keywords JOM, Flow-link formulation, Flow assignment (FA), Network recovery: restoration
 * @net2plan.ocnbooksections Section 4.3, Section 4.6.7
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_fa_xdeSharedRestoration implements IAlgorithm
{
	private InputParameter optimizationTarget = new InputParameter ("optimizationTarget", "#select# min-av-num-hops minimax-link-utilization maximin-link-idle-capacity" , "Type of optimization target. Choose among minimize the average number of hops, minimize the highest link utilization, maximize the lowest link idle capacity");
	private InputParameter solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		if (netPlan.getNumberOfSRGs() == 0) throw new Net2PlanException("No SRG was defined"); 

		/* Basic checks */
		final int N = netPlan.getNumberOfNodes();
		final int E = netPlan.getNumberOfLinks();
		final int D = netPlan.getNumberOfDemands();
		final int S = netPlan.getNumberOfSRGs();
		if (N == 0 || D == 0 || E == 0) throw new Net2PlanException("This algorithm requires a topology with links, and a demand set");

		/* Remove all unicast routed traffic. Any multicast routed traffic is kept */
		netPlan.removeAllUnicastRoutingInformation();
		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
		
		/* Initialize some variables */
		DoubleMatrix1D u_e = netPlan.getVectorLinkSpareCapacity(); // just the unused capacity (some capacity may be used by multicast traffic)
		DoubleMatrix1D h_d = netPlan.getVectorDemandOfferedTraffic();
		
		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();
		op.setInputParameter("E", E);
		op.setInputParameter("u_e", u_e, "row");
		op.setInputParameter("h_d", h_d, "row");
		op.setInputParameter("A_ne", netPlan.getMatrixNodeLinkIncidence());
		op.setInputParameter("onesS", DoubleFactory1D.dense.make (S , 1.0) , "row");
		op.setInputParameter("A_nd", netPlan.getMatrixNodeDemandIncidence());
		op.setInputParameter("A_es", netPlan.getMatrixLink2SRGAssignment());
		DoubleMatrixND A_des = new DoubleMatrixND (new int []{D,E,S} , "sparse"); for (SharedRiskGroup srg : netPlan.getSRGs()) for (Link e : srg.getAffectedLinksAllLayers()) for (int d = 0; d < D ; d ++) A_des.set(new int [] {d,e.getIndex (),srg.getIndex()} , 1.0);
		DoubleMatrixND A_nds = new DoubleMatrixND (new int []{N,D,S} , "sparse"); for (Demand d : netPlan.getDemands()) for (int s = 0; s < S ; s ++) { A_nds.set(new int [] {d.getIngressNode().getIndex(),d.getIndex(),s}, 1.0); A_nds.set(new int [] {d.getEgressNode().getIndex(),d.getIndex(),s}, -1.0); }
		op.setInputParameter("A_des", A_des);
		op.setInputParameter("A_nds", A_nds);
		
		/* Add the decision variables to the problem */
		op.addDecisionVariable("xx_de", true, new int[] { D, E }, 0, 1); /* 1 if the primary path of demand d, traverses link e */
		op.addDecisionVariable("xx_des", true, new int[] { D, E , S }, 0, 1); /* 1 if the primary path of demand d, traverses link e */

		/* Sets the objective function */
		if (optimizationTarget.getString ().equalsIgnoreCase("min-av-num-hops"))
		{
			op.setObjectiveFunction("minimize", "sum (h_d * xx_de)");
			op.addConstraint("h_d * xx_de <= u_e"); /* the capacity constraints in the no-failure state */
			op.addConstraint("sum (diag(h_d) * xx_des , 1) <=  u_e' * onesS"); /* the capacity constraints summing primary and backup paths (E constraints), make that rho becomes the worse case link utilization */
		}
		else if (optimizationTarget.getString ().equalsIgnoreCase("minimax-link-utilization"))
		{
			op.setInputParameter ("EPSILON" , 1e-4);
			op.addDecisionVariable("rho", false, new int[] { 1, 1 }, 0, Double.MAX_VALUE); /* idle capacity in the bottleneck link in any failure state */
			op.setObjectiveFunction("minimize", "rho + EPSILON * sum(xx_de) + EPSILON * sum(xx_des) ");
			op.addConstraint("h_d * xx_de <= rho * u_e"); /* the capacity constraints in the no-failure state */
			op.addConstraint("sum (diag(h_d) * xx_des , 1) <=  rho * u_e' * onesS"); /* the capacity constraints summing primary and backup paths (E constraints), make that rho becomes the worse case link utilization */
		}
		else if (optimizationTarget.getString ().equalsIgnoreCase("maximin-link-idle-capacity"))
		{
			op.setInputParameter ("EPSILON" , 1e-4);
			op.addDecisionVariable("u", false, new int[] { 1, 1 }, 0, Double.MAX_VALUE); /* idle capacity in the bottleneck link */
			op.setObjectiveFunction("maximize", "u - EPSILON * sum(xx_de) - EPSILON * sum(xx_des)");
			op.addConstraint("h_d * xx_de <= -u + u_e"); /* the capacity constraints in the no-failure state */
			op.addConstraint("sum (diag(h_d) * xx_des , 1) <=  -u + u_e' * onesS"); /* the capacity constraints summing primary and backup paths (E constraints), make that rho becomes the worse case link utilization */
		}
		else throw new Net2PlanException ("Unknown optimization target " + optimizationTarget.getString());
		
		/* Add constraints */
		op.addConstraint("A_ne * xx_de' == A_nd"); /* flow conservation constraints for the non-failure state */
		op.addConstraint("A_ne * permute(xx_des,[2;1;3]) == A_nds"); /* flow conservation constraints in all the failure states */
		op.addConstraint("xx_des <= 1 - A_des"); /* failing links do not carry traffic */

		DoubleMatrixND I_sse = new DoubleMatrixND (new int [] {S,S,E}); 
		DoubleMatrixND I_ees = new DoubleMatrixND (new int [] {E,E,S}); 
		for (int e = 0 ; e < E ; e ++) for (int s = 0 ; s < S ; s ++) { I_sse.set (new int [] { s,s,e } ,1.0); I_ees.set (new int [] { e,e,s } ,1.0); }
		op.setInputParameter("I_sse" , I_sse);
		op.setInputParameter("I_ees" , I_ees);
		op.setInputParameter("onesE" , DoubleFactory1D.dense.make(E,1) , "row");

		/* If a demand does not traverse a changing link in failure state s, keep the same routing as in no-failure state */
		op.addConstraint("- permute((xx_de * A_es) * I_sse , [1;3;2]) <= xx_des - xx_de * I_ees "); 
		op.addConstraint("  permute((xx_de * A_es) * I_sse , [1;3;2]) >= xx_des - xx_de * I_ees "); 

		/* Call the solver to solve the problem */
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");

		/* Retrieve the optimum solutions */
		DoubleMatrix2D xx_de = op.getPrimalSolution("xx_de").view2D();
		DoubleMatrix3D xx_des = op.getPrimalSolution("xx_des").view3D("sparse");

		netPlan.setRoutingFromDemandLinkCarriedTraffic(xx_de , true , false);

		checkSolution(netPlan, xx_de , xx_des);

		return "Ok!";
	}

	@Override
	public String getDescription()
	{
		return "Given a network topology, the capacities in the links and the offered traffic, this algorithm obtains a link-disjoint or a SRG-disjoint (depending on user's choice) primary and backup path for the traffic of each demand, that minimizes the network congestion, with the constraint of non-bifurcated routing. For that it solves an ILP flow-link formulation (more complex in the SRG-disjoint case). In this algorithm, congestion is minimized by maximizing the idle link capacity in the bottleneck (the link with less idle capacity).";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
	
	private static void checkSolution(NetPlan netPlan, DoubleMatrix2D xx_de , DoubleMatrix3D xx_des)
	{
		if (!netPlan.getLinksOversubscribed().isEmpty()) throw new Net2PlanException("Bad - Some link is oversubscribed (constraint violated)");
		if (!netPlan.getDemandsBlocked().isEmpty()) throw new Net2PlanException("Bad - Some demand is blocked (constraint violated)");

		for (SharedRiskGroup srg : netPlan.getSRGs())
		{
			NetPlan npThis = netPlan.copy ();
			npThis.removeAllUnicastRoutingInformation();
			DoubleMatrix2D this_xxde = xx_des.viewColumn (srg.getIndex ()).copy ();
			for (Link e : srg.getAffectedLinksAllLayers()) if (this_xxde.viewColumn(e.getIndex ()).zSum () != 0) throw new Net2PlanException("Bad - some failing links carry traffic");
			npThis.setRoutingFromDemandLinkCarriedTraffic(this_xxde , true , false);
			if (!npThis.getLinksOversubscribed().isEmpty()) throw new Net2PlanException("Bad - Some link is oversubscribed (constraint violated) in a failure");
			if (!npThis.getDemandsBlocked().isEmpty()) throw new Net2PlanException("Bad - Some demand is blocked (constraint violated) in a failure");
			for (Demand d : netPlan.getDemands ())
			{
				IntArrayList es_noFailure = new IntArrayList (); xx_de.viewRow (d.getIndex ()).getNonZeros(es_noFailure , new DoubleArrayList ());
				boolean affectedByThisFailure = false; for (Link e : srg.getAffectedLinksAllLayers()) if (xx_de.get(d.getIndex() , e.getIndex ()) != 0) { affectedByThisFailure = true; break; }
				if (!affectedByThisFailure)
				{
					IntArrayList es_thisFailure = new IntArrayList (); this_xxde.viewRow (d.getIndex ()).getNonZeros(es_thisFailure , new DoubleArrayList ());
					if (!es_noFailure.equals(es_thisFailure)) throw new Net2PlanException("Routing cannot change when a demand is not affected by the failure");
				}
			}
		}
	}
}
		
