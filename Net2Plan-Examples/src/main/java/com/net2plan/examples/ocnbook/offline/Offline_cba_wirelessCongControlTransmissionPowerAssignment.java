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
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.libraries.NetworkPerformanceMetrics;
import com.net2plan.libraries.WirelessUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * Jointly optimizes the demand injected traffic (congestion control) and link transmission powers ina wireless network, solving a NUM formulation. 
 * @net2plan.description
 * @net2plan.keywords Capacity assignment (CA), Transmission power optimization, JOM, NUM, Wireless 
 * @net2plan.ocnbooksections Section 11.5, Section 5.5, Section 6.2
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_cba_wirelessCongControlTransmissionPowerAssignment implements IAlgorithm
{
	private double PRECISIONFACTOR;

	private InputParameter solverName = new InputParameter ("solverName", "#select# ipopt", "The solver name to be used by JOM. ");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter mac_pathLossExponent = new InputParameter ("mac_pathLossExponent", 3.0 , "Exponent in the model for propagation losses" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter mac_worseRiseOverThermal_nu = new InputParameter ("mac_worseRiseOverThermal_nu", 10.0 , "Worse case ratio between interference power and thermal power at any receiver. Used to set the common thermal power in the receivers" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter mac_interferenceAttenuationFactor_nu = new InputParameter ("mac_interferenceAttenuationFactor_nu", 1.0e6 , "The interference power received in natural units is divided by this to reduce its effect" , 1 , true , Double.MAX_VALUE , true);
	private InputParameter mac_maxTransmissionPower_logu = new InputParameter ("mac_maxTransmissionPower_logu", 3.0 , "The maximum link transmission power in logarithmic units (e.g. dBm)");
	private InputParameter mac_minTransmissionPower_logu = new InputParameter ("mac_minTransmissionPower_logu", 0.0 , "The minimum link transmission power in logarithmic units (e.g. dBm)");
	private InputParameter cc_minHd = new InputParameter ("cc_minHd", 0.1 , "Minimum traffic assigned to each demand" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_maxHd = new InputParameter ("cc_maxHd", 1.0E6 , "Maximum traffic assigned to each demand" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_fairnessFactor = new InputParameter ("cc_fairnessFactor", 1.0 , "Fairness factor in utility function of congestion control" , 0 , true , Double.MAX_VALUE , true);

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		final int E = netPlan.getNumberOfLinks();
		if (E == 0) throw new Net2PlanException ("The input topology has no links");

		this.PRECISIONFACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));

		/* Remove all demands, then create a demand per input output node pair */
		netPlan.removeAllDemands();
		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
		for (Node n1 : netPlan.getNodes())
			for (Node n2 : netPlan.getNodes())
				if (n1 != n2)
					netPlan.addDemand(n1, n2, cc_minHd.getDouble(), null);
		netPlan.addRoutesFromCandidatePathList(netPlan.computeUnicastCandidatePathList(netPlan.getVectorLinkLengthInKm() , 1 , -1, -1, -1, -1, -1, -1, null)); // one route per demand, so P equals D
		
		
		/* Initialize the gains between links, normalizing them so that the maximum gain is one */
		DoubleMatrix2D mac_g_nu_ee = WirelessUtils.computeInterferenceMatrixNaturalUnits (netPlan.getLinks () , mac_interferenceAttenuationFactor_nu.getDouble() , mac_pathLossExponent.getDouble());
		final double maxGainValue = mac_g_nu_ee.getMaxLocation() [0];
		mac_g_nu_ee.assign (DoubleFunctions.div(maxGainValue));
		
		/* Initialize the thermal noise at the receivers, to have a worse case ROT (rise over thermal) */
		double worseInterferenceReceivedAtMaxPower_nu = WirelessUtils.computeWorseReceiverInterferencePower_nu (mac_maxTransmissionPower_logu.getDouble()  , mac_g_nu_ee);

		/* Adjust the thermal noise in the receivers so that we have a given ROT */
		final double mac_receptionThermalNoise_nu = worseInterferenceReceivedAtMaxPower_nu / mac_worseRiseOverThermal_nu.getDouble();

		/* Make the formulation  */
		final int D = netPlan.getNumberOfDemands();

		/* Make the formulation  */
		DoubleMatrix2D G_ee = DoubleFactory2D.sparse.make (E,E);
		DoubleMatrix2D G_eep = mac_g_nu_ee.copy ();
		for (int e = 0; e < E ; e ++) { G_ee.set(e,e,mac_g_nu_ee.get(e,e)); G_eep.set (e,e,0); }

		OptimizationProblem op = new OptimizationProblem();
		op.addDecisionVariable("p_e", false , new int[] { 1 , E }, mac_minTransmissionPower_logu.getDouble() , mac_maxTransmissionPower_logu.getDouble());
		op.addDecisionVariable("h_d", false , new int[] { 1 , D }, cc_minHd.getDouble() , cc_maxHd.getDouble());
		
		op.setInputParameter("b", 1-cc_fairnessFactor.getDouble());
		op.setInputParameter("s2", mac_receptionThermalNoise_nu);
		op.setInputParameter("G_ee", G_ee);
		op.setInputParameter("G_eep", G_eep);
		op.setInputParameter("R_de", netPlan.getMatrixDemand2LinkAssignment());
		
		/* For the objective function we use that e^(ln(x) = x */
		if (cc_fairnessFactor.getDouble() == 1)
			op.setObjectiveFunction("maximize", "sum ( ln ( h_d ))");
		else
			op.setObjectiveFunction("maximize", "(1/b) * sum (   h_d^b ) ");

		op.addConstraint("h_d * R_de <= ln (  exp(p_e)*G_ee ./ (s2 + exp(p_e)*G_eep) )" , "constr");
		
		/* Call the solver to solve the problem */
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () ,  "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (op.foundUnboundedSolution()) throw new Net2PlanException ("Found an unbounded solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
	
		/* Retrieve the optimum solutions. Convert the bps into Erlangs */
		final double optNetworkUtility = op.getOptimalCost(); // Warning: I saw in some tests, that IPOPT returned this value different to the optimum solution cost: I could not see if I did something wrong
		DoubleMatrix1D p_e_array = op.getPrimalSolution("p_e").view1D ();
		DoubleMatrix1D h_d_array = op.getPrimalSolution("h_d").view1D ();
		DoubleMatrix1D pi_e_array = op.getMultipliersOfConstraint("constr").view1D ();

		/* Update the netplan object with the resulting capacities */
		for (Link e : netPlan.getLinks())
		{
			final double sinr_e = computeSINR_e (e ,p_e_array , mac_g_nu_ee , mac_receptionThermalNoise_nu , netPlan); 
			e.setCapacity(Math.log(sinr_e));
			e.setAttribute("p_e" , "" + p_e_array.get(e.getIndex ()));
			e.setAttribute("pi_e" , "" + Math.abs (pi_e_array.get(e.getIndex ())));
		}
		/* Update the netplan object with the resulting demand offered and carried traffic */
		for (Demand d : netPlan.getDemands())
		{
			final double h_d = h_d_array.get (d.getIndex ());
			d.setOfferedTraffic(h_d);
			d.getRoutes().iterator().next().setCarriedTraffic(h_d , h_d);
		}

		/* Check constraints */
		for (Link e : netPlan.getLinks ()) 
		{ 
			final double p_e = Double.parseDouble(e.getAttribute("p_e"));
			if (p_e < mac_minTransmissionPower_logu.getDouble() - 1E-3) throw new RuntimeException ("Bad");
			if (p_e > mac_maxTransmissionPower_logu.getDouble() + 1E-3) throw new RuntimeException ("Bad");
			if (e.getCapacity() < e.getCarriedTraffic() - PRECISIONFACTOR) throw new RuntimeException ("Bad"); 
		} 

		boolean allMaxPower = true;
		for (Link e : netPlan.getLinks())
			if (Math.abs(mac_maxTransmissionPower_logu.getDouble() - Double.parseDouble(e.getAttribute("p_e"))) > 1E-3) allMaxPower = false;
		
		final double optimumNetUtilityFromCapacities = NetworkPerformanceMetrics.alphaUtility(netPlan.getVectorDemandOfferedTraffic() , cc_fairnessFactor.getDouble());
		System.out.println ("Ok! Optimal net utility: " + optimumNetUtilityFromCapacities + ", from JOM output: " + optNetworkUtility + ", all links at maximum power: " + allMaxPower);
		return "Ok! Optimal net utility: " + optimumNetUtilityFromCapacities + ", from JOM output: " + optNetworkUtility + ", all links at maximum power: " + allMaxPower;
	}

	@Override
	public String getDescription()
	{
		return "Given a wireless network where each link capacity is determined by the link SNR (signal to noise ratio - incliding interferences), and given a set of unicast traffic demands defined in it. This algorithm first computes one shortest path route per demand, which will carry the demand traffic. Then, jointly optimizes (i) the traffic to inject by each demand, and (ii) the transmission power in the wireless links, to optimize a fair allocation of the traffic among the sources (demands). The link capacity is approximated as proportional to the logarithm of the link SNR. Initially, the input network interference matrix is normalized so that the worst case rise-over-thermal (ROT) of any link matches a given value. The solution is found solving a problem formulation with JOM.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	
	private double computeSINR_e (Link e , DoubleMatrix1D p_e , DoubleMatrix2D mac_g_nu_ee , double mac_receptionThermalNoise_nu , NetPlan np)
	{
		final double receivedPower_nu = Math.exp(p_e.get(e.getIndex ())) * mac_g_nu_ee.get(e.getIndex (),e.getIndex ());
		double interferencePower_nu = mac_receptionThermalNoise_nu;
		for (Link eInt : np.getLinks ())
			if (eInt != e) interferencePower_nu += Math.exp(p_e.get(eInt.getIndex ())) * mac_g_nu_ee.get(eInt.getIndex (),e.getIndex ());
		return receivedPower_nu / interferencePower_nu;
	}

}
