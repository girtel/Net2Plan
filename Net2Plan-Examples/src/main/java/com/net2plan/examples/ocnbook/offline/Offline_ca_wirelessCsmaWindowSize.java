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
import cern.jet.math.tdouble.DoubleFunctions;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.libraries.NetworkPerformanceMetrics;
import com.net2plan.libraries.WirelessUtils;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * Optimizes the backoff window size of the links in a wireless network based on a CSMA MAC, solving a formulation.
 * @net2plan.description
 * @net2plan.keywords Capacity assignment (CA), CSMA, JOM, Wireless, NUM
 * @net2plan.ocnbooksections Section 5.4.2
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_ca_wirelessCsmaWindowSize implements IAlgorithm
{

	private InputParameter solverName = new InputParameter ("solverName", "#select# ipopt", "The solver name to be used by JOM. ");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter alphaFairnessFactor = new InputParameter ("alphaFairnessFactor", (double) 2 , "Fairness alpha factor" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter betaFactor = new InputParameter ("betaFactor", (double) 1000 , "Factor weighting the network utility in the objective function" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter linkNominalCapacity = new InputParameter ("linkNominalCapacity", (double) 1 , "Nominal rate of the links. If non positive, nominal rates are THE rates of the initial design");
	private InputParameter minimumSchedFractionTime = new InputParameter ("minimumSchedFractionTime", (double) 0.00001 , "Minimum fraction time of any feasible schedule. To avoid numerical problems" , 0 , true , Double.MAX_VALUE , true);

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		final int E = netPlan.getNumberOfLinks();

		/* Take link nominal capacities */
		if (linkNominalCapacity.getDouble() > 0) netPlan.setVectorLinkCapacity(DoubleFactory1D.dense.make (E,  linkNominalCapacity.getDouble()));
		DoubleMatrix1D mac_linkNominalCapacities = netPlan.getVectorLinkCapacity();

		/* Make the formulation  */
		final DoubleMatrix2D A_em = WirelessUtils.computeSchedulingMatrix (netPlan.getLinks ());
		final int M = A_em.columns ();
		
		OptimizationProblem op = new OptimizationProblem();
		op.addDecisionVariable("pi_m", false , new int[] { 1 , M }, minimumSchedFractionTime.getDouble() , 1);
		op.addDecisionVariable("u_e", false , new int[] { 1 , E }, DoubleUtils.zeros(E) , mac_linkNominalCapacities.toArray()); 
		
		op.setInputParameter("A_em", A_em);
		op.setInputParameter("b", 1-alphaFairnessFactor.getDouble());
		op.setInputParameter("beta", betaFactor.getDouble());
		op.setInputParameter("nomU_e", mac_linkNominalCapacities , "row");
		op.setInputParameter("A_em", A_em);
		
		/* For the objective function we use that e^(ln(x) = x */
		if (alphaFairnessFactor.getDouble() == 1)
			op.setObjectiveFunction("maximize", "-pi_m * ln(pi_m') +  beta* sum(ln (u_e))     ");
		else
			op.setObjectiveFunction("maximize", "-pi_m * ln(pi_m') +  beta/b* sum(u_e ^ b)     ");

		op.addConstraint("sum(pi_m) == 1"); // relate link and node persistence prob
		op.addConstraint("u_e <= nomU_e .* (pi_m * A_em')" , "r_e"); // relate link and node persistence prob
		
		/* Call the solver to solve the problem */
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (op.foundUnboundedSolution()) throw new Net2PlanException ("Found an unbounded solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");

		/* Retrieve the optimum solutions */
		final DoubleMatrix1D pi_m = op.getPrimalSolution("pi_m").view1D();
		final DoubleMatrix1D u_e = op.getPrimalSolution("u_e").view1D();
		final DoubleMatrix1D r_e = op.getMultipliersOfConstraint("r_e").view1D().assign (DoubleFunctions.abs); // multipliers are positive

		/* Compute the link capacities according to the schedules. Check if it is equal to returned solution  */
		DoubleMatrix1D fromSched_ue = A_em.zMult(pi_m, null).assign (mac_linkNominalCapacities , DoubleFunctions.mult);
		final double maxErrorInLinkCapacity = fromSched_ue.size () > 0? fromSched_ue.copy ().assign (u_e , DoubleFunctions.minus).assign (DoubleFunctions.abs).getMaxLocation () [0] : 0;
		if (maxErrorInLinkCapacity > 1E-3) throw new RuntimeException ("Bad");

		/* Save solution */
		for (Link e : netPlan.getLinks ())
		{ 
			e.setAttribute("r_e" , "" + r_e.get(e.getIndex ()));
			e.setCapacity(u_e.get(e.getIndex ()));
		}		

		final double netUtilityByBeta = betaFactor.getDouble() * NetworkPerformanceMetrics.alphaUtility(u_e , alphaFairnessFactor.getDouble());
		double objFunc = netUtilityByBeta; for (int m = 0 ; m < M ; m ++) objFunc -= pi_m.get(m) * Math.log(pi_m.get(m));
		
		netPlan.setAttribute("optimumCSMAObjectiveFunction", "" + objFunc);
		netPlan.setAttribute("optimumCSMAUtilityByBeta", "" + netUtilityByBeta);
		netPlan.setAttribute("optimumCSMAUtility", "" + (netUtilityByBeta/betaFactor.getDouble()));

		return "Ok! Num Valid schedules: " + M  + ", Optimal Obj Func: " + objFunc + ", Objective function just considering network utility: " + netUtilityByBeta;
	}

	@Override
	public String getDescription()
	{
		return "This algorithm computes the optimum backoff window size for each link in a wireless network governed by the CSMA protocol, so that the resulting link capacities in the network globally optimize a fairness function (that is, link capacities are allocated in a fair form). The solution is found solving a problem formulation with JOM.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
	
}
