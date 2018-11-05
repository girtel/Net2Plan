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

package com.net2plan.examples.smartCity.utn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleMatrix1D;

/** This algorithm implements several methods for estimating the urban traffic in a city, according to two models: User Equilibrium (UE) and Stochastic User Equilibrium( SUE). 
 * The city is modeled as links (streets) between nodes (interconnections of streets). 
 * The models are solved by finding a numerical solution to the related convex formulations that model the equilibrium equations for 
 * the traffic in the roads.
 * @net2plan.description 
 * @net2plan.keywords SmartCity, JOM
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino, Victoria Bueno-Delgado, Pilar Jimenez-Gomez 
 */
public class Offline_urbanTransportationNetworkAssignmentAlgorithm implements IAlgorithm
{
	private InputParameter useExistingRoutes = new InputParameter ("useExistingRoutes", false , "If true, the input design should have routes assigned to the demands, and these will be used as the candidate routes for them.");
	private InputParameter alpha = new InputParameter ("alpha", (double) 1 , "Alpha parameter in the BPR arc model" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter beta = new InputParameter ("beta", (double) 4 , "Beta parameter in the BPR arc model" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter defaultC0PRTLinkParameter = new InputParameter ("defaultC0PRTLinkParameter", (double) 30 , "The default value of the C0 parameter in the model (minimum possible traversing time for the link), for those links which do not have the appropriate attribute already set." , 0 , false , Double.MAX_VALUE , true);
	private InputParameter numberOfRoutesPerDemand = new InputParameter ("numberOfRoutesPerDemand", (int) 10 , "Set the number of routes created per demand, if the user selects to override the routes of the input design. The shortest path routes are computed according to the V0PRT parameter." , 1 , Integer.MAX_VALUE);
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter optimizationModel = new InputParameter ("optimizationModel", "#select# User-Equilibrium(UE) Stochastic-User-Equilibrium(SUE) Blind-free-speed-only" , "The model used for predicting the user routes. User equilibrium (Wardrop), and its stochastic version. In the blind free speed, the cars take the shortest path taking as cost of a street the free speed, without considering the traffic in each street");
	private InputParameter sueModel_theta = new InputParameter ("sueModel_theta", (double) 0.4 , "(only in SUE model) Parameter to model the exactitude in the perception from the user of the possible paths. Low values mean bad perception, which means more chances to use longer routes" , 0 , true , Double.MAX_VALUE , true);

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Typically, you start reading the input parameters */
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		final int E = netPlan.getNumberOfLinks();
		double [] c0_a = NetPlan.getAttributeValues (netPlan.getLinks() , UtnConstants.ATTRNAME_C0A, defaultC0PRTLinkParameter.getDouble()).toArray();
		double [] Q_a = netPlan.getVectorLinkCapacity().toArray();
		
		/* Create the set of candidate paths, or use the existing ones */
		createCandidateRoutes (netPlan , c0_a);
		final int K = netPlan.getNumberOfRoutes();

		OptimizationProblem op = new OptimizationProblem ();
		
		op.setInputParameter("Q_a", Q_a , "row");
		op.setInputParameter("alpha", alpha.getDouble());
		op.setInputParameter("beta", beta.getDouble());
		op.setInputParameter("c0_a", c0_a , "row");
		op.setInputParameter("t_i", netPlan.getVectorDemandOfferedTraffic() , "row");
		op.setInputParameter("A_ik", netPlan.getMatrixDemand2RouteAssignment());
		op.setInputParameter("A_ki", netPlan.getMatrixDemand2RouteAssignment().viewDice());
		op.setInputParameter("A_ak", netPlan.getMatrixLink2RouteAssignment());
		
		
		double [] r_a = new double [E];
		for (Link e : netPlan.getLinks ())
			r_a [e.getIndex()] = c0_a [e.getIndex()] / ((beta.getDouble()+1) * Math.pow(e.getCapacity(),beta.getDouble()));
		op.setInputParameter("r_a", r_a , "row");

		
		op.addDecisionVariable("f_k", false, new int [] {1 , K} , 0 , Double.MAX_VALUE);
		op.addDecisionVariable("v_a", false, new int [] {1,E} , 0 , Double.MAX_VALUE);

		if (optimizationModel.getString().equals("User-Equilibrium(UE)"))
		{
			op.setObjectiveFunction("minimize", "c0_a * v_a' + alpha * sum (r_a .* v_a^(beta+1))");
		}
		else if (optimizationModel.getString().equals("Stochastic-User-Equilibrium(SUE)"))
		{
			op.addDecisionVariable("C_a", false, new int [] {1,E} , 0 , Double.MAX_VALUE);
			op.setInputParameter("theta", sueModel_theta.getDouble());
			op.setObjectiveFunction("minimize", " (theta * sum (t_i .* (exp(-theta * C_a * A_ak)*A_ki))) + (v_a * C_a') - (c0_a * v_a') - (alpha * sum (r_a .* v_a^(beta+1)))");
			op.addConstraint("C_a == c0_a.*(1+(alpha*((v_a./Q_a)^beta)))");
		}
		else if (optimizationModel.getString().equals("Blind-free-speed-only"))
		{
			op.setObjectiveFunction("minimize", "c0_a * v_a'");
		}
		
		op.addConstraint("A_ak * f_k' == v_a'");
		op.addConstraint("A_ik * f_k' == t_i'");
		
		op.solve("ipopt", "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");

		DoubleMatrix1D f_k = op.getPrimalSolution("f_k").view1D();
		DoubleMatrix1D v_a = op.getPrimalSolution("v_a").view1D();
		
		netPlan.setVectorRouteCarriedTrafficAndOccupiedLinkCapacities(f_k, f_k);
		
		System.out.println("** Optimum cost: " + op.getOptimalCost() + ", solution is optimal: " + op.solutionIsOptimal());
		
		double [] c_a = new double [E];
		for (Link e : netPlan.getLinks())
		{
			c_a [e.getIndex()] = UtnConstants.bprCaComputation (c0_a [e.getIndex()] , alpha.getDouble() , v_a.get(e.getIndex()) , e.getCapacity() , beta.getDouble());
			System.out.println ("Link " + e.getOriginNode().getIndex() + " -> " + e.getDestinationNode().getIndex() + ": " + c_a [e.getIndex()]);
		}
		
		for (Route r : netPlan.getRoutes())
		{
			double cost = 0;
			for (Link e : r.getSeqLinks()) cost += c_a [e.getIndex()];
			System.out.println ("Node " + r.getIngressNode().getIndex() + " -> " + r.getEgressNode().getIndex() + ": Cost: " + cost + ", traffic: " + r.getCarriedTraffic());
		}

		netPlan.setAttribute("UTNCost" , "" + op.getOptimalCost());
		
		netPlan.removeAllRoutesUnused(0.001); // routes carrying less than this traffic are removed
		
		return "Ok! Optimum cost: " + op.getOptimalCost() + ", Number of routes carrying traffic: " + netPlan.getNumberOfRoutes(); // this is the message that will be shown in the screen at the end of the algorithm
	}

	@Override
	public String getDescription()
	{
		return "This algorithm solves the assignment problem solving the user equilibrium relation, given a network and a set of OD demands";
	}

	
	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	private void createCandidateRoutes (NetPlan netPlan , double [] c0)
	{
		if (useExistingRoutes.getBoolean())
		{
			for (Demand d : netPlan.getDemands()) if (d.getRoutes().isEmpty()) throw new Net2PlanException ("There are no paths available for the demand " + d + ", from: " + d.getIngressNode() + " to " + d.getEgressNode());
		}
		else
		{
			netPlan.removeAllRoutes();
			Map<Link,Double> linkCostMap = new HashMap<Link,Double> (); for (Link e : netPlan.getLinks()) linkCostMap.put(e , c0 [e.getIndex()]); 
			for (Demand d : netPlan.getDemands())
			{
				List<List<Link>> shortestPaths = GraphUtils.getKLooplessShortestPaths(netPlan.getNodes (), netPlan.getLinks(), d.getIngressNode(), d.getEgressNode(), linkCostMap, numberOfRoutesPerDemand.getInt(), -1, -1, -1, -1, -1, -1);
				if (shortestPaths.isEmpty()) throw new Net2PlanException ("There are no paths available for the demand " + d + ", from: " + d.getIngressNode() + " to " + d.getEgressNode());
				for (List<Link> path : shortestPaths)
					netPlan.addRoute(d, 0, 0, path, null);
			}
		}
	}
}
