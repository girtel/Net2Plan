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
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.libraries.NetworkPerformanceMetrics;
import com.net2plan.libraries.WirelessUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * Optimizes the persistence probability of the links in a wireless network based on a random-access (ALOHA-type) MAC, solving a formulation.
 * @net2plan.description
 * @net2plan.keywords Capacity assignment (CA), Random-access MAC, JOM, Wireless, NUM
 * @net2plan.ocnbooksections Section 5.4.1
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_ca_wirelessPersistenceProbability implements IAlgorithm
{
	private InputParameter solverName = new InputParameter ("solverName", "#select# ipopt", "The solver name to be used by JOM. ");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter alphaFairnessFactor = new InputParameter ("alphaFairnessFactor", (double) 2 , "Fairness alpha factor (alpha factors below to one are not accepted since the problem may be nonconvex)" , 1 , true , Double.MAX_VALUE , true);
	private InputParameter minLinkPersistenceProb = new InputParameter ("minLinkPersistenceProb", (double) 0.01 , "Minimum persistence porbability of a link" , 0 , true , 1 , true);
	private InputParameter maxNodePersistenceProb = new InputParameter ("maxNodePersistenceProb", (double) 0.99 , "Maximum persistence probability of a node" , 0 , true , 1 , true);
	private InputParameter linkNominalCapacity = new InputParameter ("linkNominalCapacity", (double) 1 , "Nominal rate of the links. If non positive, nominal rates are rates of the initial design");
	private InputParameter simultaneousTxAndRxPossible = new InputParameter ("simultaneousTxAndRxPossible", false , "If false, a node cannot transmit and receive simultaneously. If true, it can. Affects the interference map");

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		final int E = netPlan.getNumberOfLinks();
		final int N = netPlan.getNumberOfNodes();

		/* Check input parameters */
		for (Node n : netPlan.getNodes()) if (minLinkPersistenceProb.getDouble() * n.getOutgoingLinks().size () > maxNodePersistenceProb.getDouble()) throw new Net2PlanException("Minimum persistence per link is too high or maximum persistence per node too small");

		/* Take link nominal capacities */
		if (linkNominalCapacity.getDouble() > 0) netPlan.setVectorLinkCapacity(DoubleFactory1D.dense.make (E,  linkNominalCapacity.getDouble()));

		DoubleMatrix1D mac_linkNominalCapacities = netPlan.getVectorLinkCapacity();
		DoubleMatrix2D nodeInterferesToLink_ne = WirelessUtils.computeInterferenceMap (netPlan.getNodes () , netPlan.getLinks () , simultaneousTxAndRxPossible.getBoolean()).getFirst();

		/* Make the formulation  */
		OptimizationProblem op = new OptimizationProblem();
		op.addDecisionVariable("p_e", false , new int[] { 1 , E }, minLinkPersistenceProb.getDouble() , maxNodePersistenceProb.getDouble());
		op.addDecisionVariable("q_n", false , new int[] { 1 , N }, minLinkPersistenceProb.getDouble() , maxNodePersistenceProb.getDouble()); 
		
		op.setInputParameter("Aout_ne", netPlan.getMatrixNodeLinkOutgoingIncidence());
		op.setInputParameter("interf_en", nodeInterferesToLink_ne.viewDice());
		op.setInputParameter("b", 1-alphaFairnessFactor.getDouble());
		op.setInputParameter("nomU_e", netPlan.getVectorLinkCapacity() , "row");
		op.setInitialSolution("p_e" , minLinkPersistenceProb.getDouble());
		DoubleMatrix1D initQn = DoubleFactory1D.dense.make (N); for (Node n : netPlan.getNodes ()) initQn.set (n.getIndex () , minLinkPersistenceProb.getDouble() * n.getOutgoingLinks().size ());
		op.setInitialSolution("q_n" , new DoubleMatrixND (new int [] { 1 , N} , initQn));
		
		/* For the objective function we use that e^(ln(x) = x */
		if (alphaFairnessFactor.getDouble() == 1)
			op.setObjectiveFunction("maximize", "sum (    ln(nomU_e') + ln(p_e') +   interf_en *ln(1-q_n)'     )");
		else
//			op.setObjectiveFunction("maximize", "(1/b) * sum (    exp(  b*ln(nomU_e') +   b*ln(p_e') +   b*interf_en *ln(1-q_n)'   )      )");
			op.setObjectiveFunction("maximize", "(1/b) * sum (    exp(  ln(nomU_e') +   ln(p_e') +   interf_en *ln(1-q_n)'   )^b      )");

		
		op.addConstraint("q_n' == Aout_ne * p_e'"); // relate link and node persistence prob
		
		/* Call the solver to solve the problem */
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");

		/* Retrieve the optimum solutions. Convert the bps into Erlangs */
		final double optNetworkUtility = op.getOptimalCost(); // Warning: I saw in some tests, that IPOPT returned this value different to the optimum solution cost: I could not see if I did something wrong
		final double [] p_e = op.getPrimalSolution("p_e").to1DArray();
		final double [] q_n = op.getPrimalSolution("q_n").to1DArray();

		/* Save solution */
		for (int e = 0 ; e < E ; e ++)
		{ 
			double u_e = mac_linkNominalCapacities.get(e) * p_e [e]; 
			for (int n =0 ; n < N ; n ++) if (nodeInterferesToLink_ne.get(n,e) == 1) u_e *= Math.max(0 , 1 - q_n [n]);
			netPlan.getLink (e).setAttribute("p_e" , "" + p_e [e]); 
			netPlan.getLink (e).setCapacity(u_e); 
		}		
		for (int n = 0; n < N ; n ++) { netPlan.getNode (n).setAttribute("q_n" , "" + q_n [n]); }		
		
		/* check constraints etc in the solution (sometimes IPOPT makes numerical errors) */
		checkSolution (netPlan);
		
		final double optimumNetUtilityFromCapacities = NetworkPerformanceMetrics.alphaUtility(netPlan.getVectorLinkCapacity() , alphaFairnessFactor.getDouble());

		return "Ok! Optimal net utility: " + optimumNetUtilityFromCapacities + ", from JOM output: " + optNetworkUtility;
	}

	@Override
	public String getDescription()
	{
		return "This algorithm computes the persistence probability of each link in a wireless network, that is operating using a random access MAC protocol (ALOHA-type), so that the resulting link capacities globally optimize a fairness function (that is, link capacities are allocated in a fair form). The solution is found solving a problem formulation with JOM.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	private void checkSolution (NetPlan netPlan)
	{
		for (Link e : netPlan.getLinks ()) 
		{ 
			final double pThisLink = Double.parseDouble(e.getAttribute("p_e"));
			if (pThisLink < minLinkPersistenceProb.getDouble() - 1E-3) throw new RuntimeException ("Bad");
		} 
		for (Node n : netPlan.getNodes ()) 
		{
			final double qThisNode = Double.parseDouble(n.getAttribute("q_n"));
			if (qThisNode < 0) throw new RuntimeException ("Bad");
			if (qThisNode > maxNodePersistenceProb.getDouble() +1E-3) throw new RuntimeException ("Bad");			
			double peAccum = 0;
			for (Link e : n.getOutgoingLinks()) peAccum += Double.parseDouble(e.getAttribute("p_e"));
			if (Math.abs(peAccum - qThisNode) > 1E-3) throw new RuntimeException ("Bad");
		}
	}
}
