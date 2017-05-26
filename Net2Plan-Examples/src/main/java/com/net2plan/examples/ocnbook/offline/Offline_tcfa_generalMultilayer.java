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
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * Solves a general multilayer optimization problem formulation. 
 * @net2plan.description
 * @net2plan.keywords Multilayer, Flow assignment (FA), Flow-link formulation, Destination-link formulation, Modular capacities
 * @net2plan.ocnbooksections Section 7.4
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_tcfa_generalMultilayer implements IAlgorithm
{
	private InputParameter solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter ciurcuitCapacityGbps = new InputParameter ("ciurcuitCapacityGbps", (double) 1.0 , "Capacity of a circuit in Gbps, the assumed units for upper layer traffic" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter capLowLayerLinksInNumCircuits = new InputParameter ("capLowLayerLinksInNumCircuits", (int) 100 , "The capacity of a lower layer link, measured as the maximum number of circuits that can traverse it." , 1 , Integer.MAX_VALUE);

	private NetworkLayer lowerLayer, upperLayer;
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		if (netPlan.getNumberOfLayers() > 2) throw new Net2PlanException ("The design must have one or two layers");

		/* Set a two layer network topology, maybe starting from a single layer design */
		if (netPlan.isSingleLayer())
		{
			this.lowerLayer = netPlan.getNetworkLayerDefault();
			this.upperLayer = netPlan.addLayer("UP LAYER" , "Upper layer of the design" , "Gbps" , "Gbps" , null , null);
			/* Save the demands in the upper layer, and remove them from the lower layer */
			for (Demand d : netPlan.getDemands (lowerLayer)) netPlan.addDemand(d.getIngressNode() , d.getEgressNode() , d.getOfferedTraffic() , null , upperLayer);
		}
		else
		{
			this.lowerLayer = netPlan.getNetworkLayer(0);
			this.upperLayer = netPlan.getNetworkLayer(1);
		}
		netPlan.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING, upperLayer);
		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING, lowerLayer);

		/* Initialize some variables */
		final double PRECISION_FACTOR = 0.05; //Double.parseDouble(net2planParameters.get("precisionFactor"));
		final int N = netPlan.getNumberOfNodes();
		final int D_up = netPlan.getNumberOfDemands (upperLayer);
		final int E_lo = netPlan.getNumberOfLinks (lowerLayer);
		if (N == 0 || D_up == 0) throw new Net2PlanException("This algorithm requires a topology and a demand set");

		netPlan.removeAllLinks(upperLayer);
		netPlan.removeAllDemands(lowerLayer);
		netPlan.setLinkCapacityUnitsName("Gbps" , lowerLayer);
		netPlan.setLinkCapacityUnitsName("Gbps" , upperLayer);
		netPlan.setDemandTrafficUnitsName("Gbps" , lowerLayer);
		netPlan.setDemandTrafficUnitsName("Gbps" , upperLayer);
		netPlan.setVectorLinkCapacity(DoubleFactory1D.dense.make (E_lo , capLowLayerLinksInNumCircuits.getInt() * ciurcuitCapacityGbps.getDouble()) , lowerLayer);
		
		/* Create a full mesh of upper layer links in the netPlan object (index "c" in the variables), coupled to the
		 * corresponding demand in the lower layer */
		for (Node i : netPlan.getNodes ()) for (Node j : netPlan.getNodes ()) if (i != j)
		{
			if ((i.getIndex() == 0) && (j.getIndex() == 1)) continue;
			final Link link = netPlan.addLink (i, j,0 , netPlan.getNodePairEuclideanDistance(i, j), 200000 , null , upperLayer);
			final Demand demand = netPlan.addDemand(i, j, 0, null, lowerLayer);
			if (link.getIndex() != demand.getIndex()) throw new RuntimeException ("Bad");
			link.coupleToLowerLayerDemand(demand);
		}
					
		/* Compute the distances of the potential links */
		final int E_ul = netPlan.getNumberOfLinks(upperLayer);
		final int E_ll = netPlan.getNumberOfLinks(lowerLayer);

		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();

		op.addDecisionVariable("x_tc", false , new int[] { N, E_ul }, 0, Double.MAX_VALUE); /* the amount of traffic targeted to node t, that is carried by link c (upper layer virtual link) */
		op.addDecisionVariable("z_c", true , new int[] { 1 , E_ul }, 0, Double.MAX_VALUE); /* number of circuits established between end nodes of virtual link c  */
		op.addDecisionVariable("x_ce", true , new int[] { E_ul , E_ll }, 0, Double.MAX_VALUE); /* number of circuits corresponding to virtual link c, that traverse low layer link e */

		/* Set some input parameters */
		op.setInputParameter("A_nc", netPlan.getMatrixNodeLinkIncidence(upperLayer)); /* 1 in position (n,e) if link e starts in n, -1 if it ends in n, 0 otherwise */
		final DoubleMatrix1D egressTraffic_t = netPlan.getVectorNodeEgressUnicastTraffic(upperLayer);
		final DoubleMatrix2D trafficMatrixDiagonalNegative = netPlan.getMatrixNode2NodeOfferedTraffic(upperLayer);
		trafficMatrixDiagonalNegative.assign (DoubleFactory2D.sparse.diagonal(egressTraffic_t) , DoubleFunctions.minus);
		op.setInputParameter("TM", trafficMatrixDiagonalNegative);
		op.setInputParameter("U_hi", ciurcuitCapacityGbps.getDouble());
		op.setInputParameter("U_lo", capLowLayerLinksInNumCircuits.getInt());
		op.setInputParameter("A_ne", netPlan.getMatrixNodeLinkIncidence(lowerLayer)); /* 1 in position (n,e) if link e starts in n, -1 if it ends in n, 0 otherwise */
		op.setInputParameter("h_d", netPlan.getVectorDemandOfferedTraffic(upperLayer) , "row");
		op.setInputParameter("EPSILON", 0.001 / (E_ul * netPlan.getVectorDemandOfferedTraffic(upperLayer).zSum()));
		
		/* Sets the objective function */
		op.setObjectiveFunction("minimize", "sum(z_c) + EPSILON * sum(x_tc) + EPSILON*sum(x_ce)");

		/* Compute some matrices required for writing the constraints */
		op.addConstraint("A_nc * (x_tc') == TM"); /* the flow-conservation constraints (NxE_hi constraints) */
		op.addConstraint("sum(x_tc,1) <= U_hi * z_c"); /* the capacity constraints (E_hi constraints) */
		op.addConstraint("A_ne * (x_ce') == A_nc * diag(z_c)"); /* the lower layer flow-conservation constraints (NxE_hi constraints) */
		op.addConstraint("sum(x_ce,1) <= U_lo"); /* the capacity constraints (E constraints) */

		/* Call the solver to solve the problem */
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		
		/* Retrieve the optimum solutions */
		final DoubleMatrix1D z_c = op.getPrimalSolution("z_c").view1D();
		final DoubleMatrix2D x_tc = op.getPrimalSolution("x_tc").view2D();
		final DoubleMatrix2D x_ce = op.getPrimalSolution("x_ce").view2D();

		/* Sets the routing in both layers, which also establish the capacity in the upper layer links */
		netPlan.setRoutingFromDemandLinkCarriedTraffic(x_ce.copy ().assign(DoubleFunctions.mult(ciurcuitCapacityGbps.getDouble())) , false , true , lowerLayer);
		netPlan.setRoutingFromDestinationLinkCarriedTraffic(x_tc , true , upperLayer); // remove the cycles if any

		for (Link eHi : netPlan.getLinks (upperLayer))
			if (Math.abs(eHi.getCapacity() - ciurcuitCapacityGbps.getDouble() * z_c.get(eHi.getIndex())) > 1e-3) throw new RuntimeException ("Bad");
		netPlan.removeAllLinksUnused (PRECISION_FACTOR , upperLayer);

		/* check */
		if (netPlan.getVectorDemandBlockedTraffic(upperLayer).zSum() > PRECISION_FACTOR) throw new RuntimeException ("Bad: " + netPlan.getVectorDemandBlockedTraffic(upperLayer));
		if (netPlan.getVectorDemandBlockedTraffic(lowerLayer).zSum() > PRECISION_FACTOR) throw new RuntimeException ("Bad");
		if (netPlan.getVectorLinkOversubscribedTraffic(upperLayer).zSum() > PRECISION_FACTOR) throw new RuntimeException ("Bad");
		if (netPlan.getVectorLinkOversubscribedTraffic(lowerLayer).zSum() > PRECISION_FACTOR) throw new RuntimeException ("Bad");
		
		return "Ok! Num circuits: " + Math.round(netPlan.getVectorLinkCapacity(upperLayer).zSum() / ciurcuitCapacityGbps.getDouble());
	}

	@Override
	public String getDescription()
	{
		return "Given a network composed of two layers, with traffic demands defined at the upper layer and links defined at the lower. The upper laye traffic is supposed to be routed through fixed capacity circuits. Each circuit, which is a direct link or the upper layer, is actually realized by a demand in the lower layer, that is routed through the lower layer links. Each lower layer link can host a given number of circuits traversing it. The algorithm searches for (i) the circuits to establish, (ii) the routing of the upper layer over the circuits, and (iii) the routing of the circuits over the lower layer links. The target is minimizing the number of circuits needed, a measure of the network cost. For finding the solution, the algorithm solves using JOM a formulation that combines a destination-link formulation in the upper layer routing, and a flow-link formulation in the lower layer. ";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
}
