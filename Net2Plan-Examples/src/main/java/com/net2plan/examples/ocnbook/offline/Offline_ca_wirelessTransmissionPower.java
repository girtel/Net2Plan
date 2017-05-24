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

import cern.colt.matrix.tdouble.DoubleFactory2D;
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
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * Finds a fair allocation of the transmission power in a wireless network, solving a formulation.
 * @net2plan.description
 * @net2plan.keywords Capacity assignment (CA), Transmission power optimization, JOM, NUM, Wireless 
 * @net2plan.ocnbooksections Section 5.5
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_ca_wirelessTransmissionPower implements IAlgorithm
{

	private InputParameter solverName = new InputParameter ("solverName", "#select# ipopt", "The solver name to be used by JOM. ");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter alphaFairnessFactor = new InputParameter ("alphaFairnessFactor", (double) 2 , "Fairness alpha factor" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter pathLossExponent = new InputParameter ("pathLossExponent", (double) 2 , "Exponent in the the path loss model, computing attenuation for different distances to the transmitter" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter worseRiseOverThermal_nu = new InputParameter ("worseRiseOverThermal_nu", (double) 10 , "Worse case ratio between interference power and thermal power. Used to set the thermal power in the receivers" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter interferenceAttenuationFactor_nu = new InputParameter ("interferenceAttenuationFactor_nu", (double) 1E6 , "The interference power received in natural units is multiplied by this to reduce its effect" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter maxTransmissionPower_logu = new InputParameter ("maxTransmissionPower_logu", (double) 3 , "The maximum link transmission power in logarithmic units (e.g. dBm)");
	private InputParameter minTransmissionPower_logu = new InputParameter ("minTransmissionPower_logu", (double) 0 , "The minimum link transmission power in logarithmic units (e.g. dBm)");

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		final int E = netPlan.getNumberOfLinks();
		if (E == 0) throw new Net2PlanException ("The input topology has no links");
		
		/* Initialize the gains between links, normalizing them so that the maximum gain is one */
		DoubleMatrix2D mac_g_nu_ee = WirelessUtils.computeInterferenceMatrixNaturalUnits (netPlan.getLinks () , interferenceAttenuationFactor_nu.getDouble() , pathLossExponent.getDouble());
		//System.out.println("NOT normalized Gnu_ee: " + Gnu_ee);
		final double maxGainValue = mac_g_nu_ee.getMaxLocation() [0];
		mac_g_nu_ee.assign (DoubleFunctions.div(maxGainValue));
		//System.out.println("normalized mac_g_nu_ee: " + mac_g_nu_ee);
		
		/* Initialize the thermal noise at the receivers, to have a worse case ROT (rise over thermal) */
		double worseInterferenceReceivedAtMaxPower_nu = WirelessUtils.computeWorseReceiverInterferencePower_nu (maxTransmissionPower_logu.getDouble()  , mac_g_nu_ee);

		/* Adjust the thermal noise in the receivers so that we have a given ROT */
		final double mac_receptionThermalNoise_nu = worseInterferenceReceivedAtMaxPower_nu / worseRiseOverThermal_nu.getDouble();
		
		/* Make the formulation  */
		DoubleMatrix2D G_ee = DoubleFactory2D.sparse.make (E,E);
		DoubleMatrix2D G_eep = mac_g_nu_ee.copy ();
		for (int e = 0; e < E ; e ++) { G_ee.set(e,e,mac_g_nu_ee.get(e,e)); G_eep.set (e,e,0); }
		
		OptimizationProblem op = new OptimizationProblem();
		op.addDecisionVariable("p_e", false , new int[] { 1 , E }, minTransmissionPower_logu.getDouble() , maxTransmissionPower_logu.getDouble());
		op.setInputParameter("b", 1-alphaFairnessFactor.getDouble());
		op.setInputParameter("s2", mac_receptionThermalNoise_nu);
		op.setInputParameter("G_ee", G_ee);
		op.setInputParameter("G_eep", G_eep);
		
		/* For the objective function we use that e^(ln(x) = x */
		if (alphaFairnessFactor.getDouble() == 1)
			op.setObjectiveFunction("maximize", "sum ( ln ( ln (  exp(p_e)*G_ee ./ (s2 + exp(p_e)*G_eep)  ) ) )");
		else
			op.setObjectiveFunction("maximize", "(1/b) * sum (   ln (  exp(p_e)*G_ee ./ (s2 + exp(p_e)*G_eep)  )^b   )  ");
		
		/* Call the solver to solve the problem */
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () ,  "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (op.foundUnboundedSolution()) throw new Net2PlanException ("Found an unbounded solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
	
		/* Retrieve the optimum solutions. Convert the bps into Erlangs */
		final double optNetworkUtility = op.getOptimalCost(); // Warning: I saw in some tests, that IPOPT returned this value different to the optimum solution cost: I could not see if I did something wrong
		DoubleMatrix1D p_e = op.getPrimalSolution("p_e").view1D ();

		/* Update the netplan object with the resulting capacities */
		for (Link e : netPlan.getLinks())
		{
			final double sinr_e = WirelessUtils.computeSINRLink(e.getIndex () ,p_e , mac_g_nu_ee , mac_receptionThermalNoise_nu); 
			e.setCapacity(Math.log(sinr_e));
			e.setAttribute("p_e" , "" + p_e.get(e.getIndex ())); 
		}

		/* Check constraints */
		if (p_e.getMinLocation() [0] < minTransmissionPower_logu.getDouble() - 1E-3) throw new RuntimeException ("Bad");
		if (p_e.getMaxLocation() [0] > maxTransmissionPower_logu.getDouble() + 1E-3) throw new RuntimeException ("Bad");
		final boolean allMaxPower = (p_e.getMinLocation() [0] >= maxTransmissionPower_logu.getDouble() - 1E-3);

		System.out.println("Optimum solution: u_e: " + netPlan.getVectorLinkCapacity());
		System.out.println("Optimum solution: p_e: " + p_e);
		System.out.println("ALL AT THE MAX POWER: " + allMaxPower);	

		final double optimumNetUtilityFromCapacities = NetworkPerformanceMetrics.alphaUtility(netPlan.getVectorLinkCapacity() , alphaFairnessFactor.getDouble()); 
		return "Ok! Solution guaranteed to be optimal: " + op.solutionIsOptimal() + ", Optimal net utility: " + optimumNetUtilityFromCapacities + ", from JOM output: " + optNetworkUtility;
	}

	@Override
	public String getDescription()
	{
		return "This algorithm computes the optimum transmission power of each link in a wireless network subject to interferences. The link capacity is approximated as proportional to the logarithm of the signal-to-noise (including interferences) ratio. The target when optimizing the transmission power is enforcing a fair allocation of the resulting link capacities. Initially, the input network interference matrix is normalized so that the worst case rise-over-thermal (ROT) of any link matches a given value.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

}
