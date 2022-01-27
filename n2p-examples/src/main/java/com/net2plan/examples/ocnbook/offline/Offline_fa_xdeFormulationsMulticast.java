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
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Solves several variants of multicast routing problems, with flow-link formulations
 * @net2plan.description
 * @net2plan.keywords Multicast, JOM, Flow-link formulation, Flow assignment (FA)
 * @net2plan.ocnbooksections Section 4.6.2
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_fa_xdeFormulationsMulticast implements IAlgorithm
{
	private InputParameter linkCostType = new InputParameter ("linkCostType", "#select# hops km" , "Criteria to compute the multicast tree cost. Valid values: 'hops' (all links cost one) or 'km' (link cost is its length in km)");
	private InputParameter optimizationTarget = new InputParameter ("optimizationTarget", "#select# min-consumed-bandwidth min-av-num-hops minimax-link-utilization maximin-link-idle-capacity" , "Type of optimization target. Choose among minimize the total traffic in the links, minimize the average number of hops from ingress to different egress nodes, minimize the highest link utilization, maximize the lowest link idle capacity");
	private InputParameter maxCopyCapability = new InputParameter ("maxCopyCapability", (int) -1 , "Maximum number of copies of the traffic a node can make (this is the maximum number of output links in a node of the same multicast tree). A non-positive value means no limit");
	private InputParameter maxE2ELengthInKm = new InputParameter ("maxE2ELengthInKm", (double) -1 , "The path from an origin to any destination in any multicast tree cannot be longer than this. A non-positive number means this limit does not exist");
	private InputParameter maxE2ENumHops = new InputParameter ("maxE2ENumHops", (int) -1 , "The path from an origin to any destination in any multicast tree cannot have more than this number of hops. A non-positive number means this limit does not exist");
	private InputParameter maxE2EPropDelayInMs = new InputParameter ("maxE2EPropDelayInMs", (double) -1 , "The path from an origin to any destination in any multicast tree cannot have more than this propagation delay in miliseconds. A non-positive number means this limit does not exist");
	private InputParameter solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		if (!linkCostType.getString().equalsIgnoreCase("km") && !linkCostType.getString().equalsIgnoreCase("hops"))
			throw new Net2PlanException("Wrong linkCostType parameter");
		
		/* Initialize variables */
		final int E = netPlan.getNumberOfLinks();
		final int N = netPlan.getNumberOfNodes();
		final int MD = netPlan.getNumberOfMulticastDemands();
		if (E == 0 || MD == 0) throw new Net2PlanException("This algorithm requires a topology with links and a multicast demand set");

		/* Remove all multicast routed traffic. Any unicast routed traffic is kept */
		netPlan.removeAllMulticastTrees();

		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();

		/* Set some input parameters to the problem */
		op.setInputParameter("u_e", netPlan.getVectorLinkSpareCapacity(), "row"); /* for each link, its unused capacity (the one not used by any mulitcast trees) */
		op.setInputParameter("A_nd", netPlan.getMatrixNodeMulticastDemandIncidence()); /* 1 if node n is ingress in multicast demand d, -1 if n is egress */ 
		final DoubleMatrix2D Aout_nd = netPlan.getMatrixNodeMulticastDemandOutgoingIncidence();
		final DoubleMatrix2D Ain_nd = netPlan.getMatrixNodeMulticastDemandIncomingIncidence();
		op.setInputParameter("Aout_nd", Aout_nd); /* 1 if node n is ingress in multicast demand d */ 
		op.setInputParameter("Ain_nd", Ain_nd); /* -1 if node n is egress in multicast demand d */ 
		op.setInputParameter("A_ne", netPlan.getMatrixNodeLinkIncidence()); /* 1 in position (n,e) if link e starts in n, -1 if it ends in n, 0 otherwise */
		op.setInputParameter("Aout_ne", netPlan.getMatrixNodeLinkOutgoingIncidence()); /* 1 in position (n,e) if link e starts in n */
		op.setInputParameter("Ain_ne", netPlan.getMatrixNodeLinkIncomingIncidence()); /* 1 in position (n,e) if link e ends in n */
		op.setInputParameter("h_d", netPlan.getVectorMulticastDemandOfferedTraffic(), "row"); /* for each multicast demand, its offered traffic */
		op.setInputParameter("lengthInKm_e", netPlan.getVectorLinkLengthInKm(), "row"); /* for each link, its length in km */
		op.setInputParameter("propDelay_e", netPlan.getVectorLinkPropagationDelayInMiliseconds(), "row"); /* for each link, its length in km */
		op.setInputParameter("onesE", DoubleFactory1D.dense.make (E,1.0) , "row"); /* for each link, a one */
		op.setInputParameter("K", maxCopyCapability.getInt() <= 0? N : maxCopyCapability.getInt ()); /* the maximum number of output links a node can copy the input traffic of a multicast tree (<= 0 means no limitation) */
		
		/* Add decision variables for the multicast demands */
		op.addDecisionVariable("xx_de", true , new int [] {MD , E} , 0 , 1); // 1 if link e is used my the multicast tree of this demand
		op.addDecisionVariable("xx_det", true , new int [] {MD , E , N} , 0 , 1); // 1 if link e is used my the multicast tree of this demand

		/* Write the problem objective function and constraints specific to this objective function */
		if (optimizationTarget.getString ().equals ("min-consumed-bandwidth")) 
		{
			op.setObjectiveFunction("minimize", "sum (h_d * xx_de)"); /* total traffic in the links */
			op.addConstraint("h_d * xx_de <= u_e"); /* the traffic in each link cannot exceed its capacity */
		}
		else if (optimizationTarget.getString ().equals ("min-av-num-hops")) 
		{
			op.setInputParameter ("EPSILON" , getMinimumNonZeroTrafficOrCapacityValue (netPlan) / 1000);
			op.setObjectiveFunction("minimize", "sum(diag(h_d) * xx_det) + EPSILON * sum (h_d * xx_de)"); /* proportional to the number of hops each packet makes */
			op.addConstraint("h_d * xx_de <= u_e"); /* the traffic in each link cannot exceed its capacity */
		}
		else if (optimizationTarget.getString ().equals ("minimax-link-utilization")) 
		{
			op.setInputParameter ("EPSILON" , getMinimumNonZeroTrafficOrCapacityValue (netPlan) / 1000);
			op.addDecisionVariable("rho", false, new int[] { 1, 1 }, 0, 1); /* worse case link utilization */
			op.setObjectiveFunction("minimize", "rho + EPSILON * sum (h_d * xx_de)"); // to avoid loops, we sum EPSILON by the traffic carried (EPSILON very small number)
			op.addConstraint("h_d * xx_de <= rho * u_e"); /* the traffic in each link cannot exceed its capacity. sets rho as the worse case utilization */
		}
		else if (optimizationTarget.getString ().equals ("maximin-link-idle-capacity"))
		{
			op.setInputParameter ("EPSILON" , getMinimumNonZeroTrafficOrCapacityValue (netPlan) / 1000);
			op.addDecisionVariable("u", false, new int[] { 1, 1 }, 0, Double.MAX_VALUE); /* worse case link idle capacity */
			op.setObjectiveFunction("maximize", "u - EPSILON * sum (h_d * xx_de)"); // to avoid loops, we sum EPSILON by the traffic carried (EPSILON very small number)
			op.addConstraint("h_d * xx_de <= -u + u_e"); /* the traffic in each link cannot exceed its capacity. sets u as the worse case idle capacity */
		}
		else throw new Net2PlanException ("Unknown optimization target " + optimizationTarget.getString());

//		DoubleMatrix3D I_eet = DoubleFactory3D.sparse.make (E,E,N); for (int t = 0; t < N ; t ++) I_eet.viewColumn (t).assign (DoubleFactory2D.sparse.identity(E));
//		op.setInputParameter("I_eet", new DoubleMatrixND (I_eet));
		DoubleMatrixND I_eet = new DoubleMatrixND (new int [] {E,E,N} , "sparse"); for (int t = 0 ; t < N ; t ++) for (int e = 0 ; e < E ; e ++) I_eet.set (new int [] { e,e,t} , 1.0);
		op.setInputParameter("I_eet", I_eet);
		
		DoubleMatrixND A_ndt = new DoubleMatrixND (new int [] { N , MD , N }  , "sparse"); 
		for (MulticastDemand d : netPlan.getMulticastDemands())
			for (Node t : d.getEgressNodes())
			{
				A_ndt.set (new int [] { d.getIngressNode().getIndex() , d.getIndex ()  , t.getIndex () } , 1.0);
				A_ndt.set (new int [] { t.getIndex () , d.getIndex () , t.getIndex () } , -1.0);
			}
		op.setInputParameter("A_ndt", A_ndt);
		op.setInputParameter("onesN" , DoubleFactory1D.dense.make (N,1.0)  , "row");
		
		/* Add constraints for the multicast demands */
		op.addConstraint ("Ain_ne * xx_de' >= Ain_nd"); // a destination node receives at least one input link
		op.addConstraint ("Ain_ne * xx_de' <= 1 - Aout_nd"); // source nodes receive 0 links, destination nodes at most one (then just one)
		op.addConstraint ("Aout_ne * xx_de' <= K * (Aout_nd + (Ain_ne * xx_de'))"); // at most K out links from ingress node and from intermediate nodes if they have one input link
		op.addConstraint ("xx_det  <= xx_de * I_eet"); // a link belongs to a path only if it is in the tree
		op.addConstraint ("A_ne * permute(xx_det , [2 ; 1 ; 3]) == A_ndt"); // flow conservation constraint for each path in the tree

		final boolean maxE2ELengthConstraintApplies = (maxE2ELengthInKm.getDouble() > 0) && (maxE2ELengthInKm.getDouble() < netPlan.getVectorLinkLengthInKm().zSum());
		final boolean maxE2ENumHopsConstraintApplies = (maxE2ENumHops.getInt() > 0) && (maxE2ENumHops.getInt() < E);
		final boolean maxE2EPropDelayConstraintApplies = (maxE2EPropDelayInMs.getDouble() > 0) && (maxE2EPropDelayInMs.getDouble() < netPlan.getVectorLinkPropagationDelayInMiliseconds().zSum());
		if (maxE2ELengthConstraintApplies) op.addConstraint ("lengthInKm_e * permute(xx_det,[2;1;3]) <= " + maxE2ELengthInKm.getDouble()); // maximum length in km from ingress node to any demand egress node is limited
		if (maxE2ENumHopsConstraintApplies) op.addConstraint ("sum(xx_det,2) <= " + maxE2ENumHops.getInt()); // maximum number of hops from ingress node to any demand egress node is limited
		if (maxE2EPropDelayConstraintApplies) op.addConstraint ("propDelay_e * permute(xx_det,[2;1;3])  <= " + maxE2EPropDelayInMs.getDouble()); // maximum propagation delay in ms from ingress node to any demand egress node is limited

		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		
		/* Save the solution found in the netPlan object */
		final DoubleMatrix2D xx_de = op.getPrimalSolution("xx_de").view2D();
		/* Check */
		final DoubleMatrixND xx_det = op.getPrimalSolution("xx_det");
		for (MulticastDemand d : netPlan.getMulticastDemands())
			for (Link e : netPlan.getLinks())
				for (Node n : netPlan.getNodes())
					if (xx_de.get(d.getIndex () , e.getIndex ()) == 0)
						if (xx_det.get(new int [] {d.getIndex() , e.getIndex () , n.getIndex() } ) != 0) throw new RuntimeException ("Bad: x_de: " + (xx_de.get(d.getIndex () , e.getIndex ())) + ", xx_det: " + (xx_det.get(new int [] {d.getIndex() , e.getIndex () , n.getIndex() } )) );
		
		for (MulticastDemand d : netPlan.getMulticastDemands())
		{
			Set<Link> linkSet = new HashSet<Link> (); for (int e = 0 ; e < E ; e ++) if (xx_de.get(d.getIndex() , e) != 0) { linkSet.add (netPlan.getLink (e)); }
			netPlan.addMulticastTree(d , d.getOfferedTraffic() , d.getOfferedTraffic() , linkSet , null);
		}
		

		
		
		return "Ok!: The solution found is guaranteed to be optimal: " + op.solutionIsOptimal() + ", number of multicast trees created: " + netPlan.getNumberOfMulticastTrees();
	}

	@Override
	public String getDescription()
	{
		return "Given a network topology, the capacities in the links, and a set multicast traffic demands, this algorithm permits computing the optimum multicast routing of the traffic (that is, the set ofm multicast trees carrying the traffic of the multicast demand) solving flow-link formulations. Through a set of input parameters, the user can choose among different optimization targets and constraints.";
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
