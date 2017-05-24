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
package com.net2plan.examples.ocnbook.onlineSim;


import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import com.net2plan.examples.ocnbook.offline.Offline_ca_wirelessCsmaWindowSize;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.NetworkPerformanceMetrics;
import com.net2plan.libraries.WirelessUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.TimeTrace;
import com.net2plan.utils.Triple;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 
 * This module implements a distributed dual-gradient based algorithm for adjusting the backoff windows sizes in a wireless network with a CSMA-mased MAC, to maximize the network utility enforcing a fair allocation of the resources.
 *
 * Ths event processor is adapted to permit observing the algorithm performances under user-defined conditions, 
 * including asynchronous distributed executions, where signaling can be affected by losses and/or delays, and/or measurement errors. 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec10_5_csmaBackoffOptimizationDual.m">{@code fig_sec10_5_csmaBackoffOptimizationDual.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * 
 * To simulate a network with this module, use the {@code Online_evGen_doNothing} generator.
 * 
 * @net2plan.keywords Wireless, CSMA, Capacity assignment (CA), Dual gradient algorithm
 * @net2plan.ocnbooksections Section 10.5
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Online_evProc_csmaBackoffOptimizationDual extends IEventProcessor 
{
	private InputParameter update_isSynchronous = new InputParameter ("update_isSynchronous", false , "true if all the distributed agents involved wake up synchronousely to update its state");
	private InputParameter update_averageInterUpdateTime = new InputParameter ("update_averageInterUpdateTime", 1.0 , "Average time between two updates of an agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter update_maxFluctuationInterUpdateTime = new InputParameter ("update_maxFluctuationInterUpdateTime", 0.5 , "Max fluctuation in time in the update interval of an agent, in absolute time values. The update intervals are sampled from a uniform distribution within the given interval" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter gradient_gammaStep = new InputParameter ("gradient_gammaStep", 5.0 , "Gamma step in the gradient algorithm" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter control_fairnessFactor = new InputParameter ("control_fairnessFactor", 1.0 , "Fairness factor in utility function of link capacity assignment" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter simulation_randomSeed = new InputParameter ("simulation_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter simulation_maxNumberOfUpdateIntervals = new InputParameter ("simulation_maxNumberOfUpdateIntervals", 1000.0 , "Maximum number of update intervals in average per agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter control_linkNominalCapacity = new InputParameter ("control_linkNominalCapacity", 1.0 , "Nominal rate of the links. If non positive, nominal rates are rates of the initial design");
	private InputParameter simulation_outFileNameRoot = new InputParameter ("simulation_outFileNameRoot", "csmaBackoffOptimizationDual" , "Root of the file name to be used in the output files. If blank, no output");
	
	private InputParameter control_betaFactor = new InputParameter ("control_betaFactor", 10.0 , "Factor weighting the network utility in the objective function" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter control_maxSeMeasurementRelativeNoise = new InputParameter ("control_maxSeMeasurementRelativeNoise", 0.1 , "Max relative fluctuation in gradient coordinate" , 0 , true , Double.MAX_VALUE , true);
	
	private Random rng;

	private static final int UPDATE_WAKEUPTOUPDATE = 202;

	private int E , N , M;
	
	private NetPlan currentNetPlan , copyInitialNetPlan;
	
	private DoubleMatrix2D A_em;
	private DoubleMatrix1D control_linkNominalCapacities_e;
	private DoubleMatrix1D control_r_e;
	private DoubleMatrix1D control_intendedU_e;
	private DoubleMatrix1D pi_m;

	private TimeTrace stat_traceOf_ue;
	private TimeTrace stat_traceOf_re;
	private TimeTrace stat_traceOf_objFun;
	private TimeTrace stat_traceOf_netUtilityWithoutBeta;
	private TimeTrace stat_traceOf_netUtilityWithBeta;
	
	@Override
	public String getDescription()
	{
		return "This module implements a distributed dual-gradient based algorithm for adjusting the backoff windows sizes in a wireless network with a CSMA-mased MAC, to maximize the network utility enforcing a fair allocation of the resources.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	@Override
	public void initialize(NetPlan currentNetPlan, Map<String, String> algorithmParameters, Map<String, String> simulationParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		this.currentNetPlan = currentNetPlan;
		this.copyInitialNetPlan = currentNetPlan.copy();
		
		if (currentNetPlan.getNumberOfLayers() != 1) throw new Net2PlanException ("This algorithm works in single layer networks");
		this.N = currentNetPlan.getNumberOfNodes ();
		this.E = currentNetPlan.getNumberOfLinks ();
		if (E == 0) throw new Net2PlanException ("The input design should have links");

		if (control_linkNominalCapacity.getDouble() > 0) currentNetPlan.setVectorLinkCapacity(DoubleFactory1D.dense.make (E , control_linkNominalCapacity.getDouble()));
		control_linkNominalCapacities_e = currentNetPlan.getVectorLinkCapacity();

		this.rng = new Random (simulation_randomSeed.getLong());

		this.E = this.currentNetPlan.getNumberOfLinks();
		this.N = this.currentNetPlan.getNumberOfNodes();
		this.A_em = WirelessUtils.computeSchedulingMatrix(currentNetPlan.getLinks ());
		this.M = A_em.columns();
		
		/* Take link nominal capacities */
		this.control_r_e = DoubleFactory1D.dense.make(E);
		this.control_intendedU_e = DoubleFactory1D.dense.make(E);
		for (Link e : currentNetPlan.getLinks ())
		{
			control_r_e.set(e.getIndex () , 1.0);
			e.setAttribute("r_e" , "" + 1.0); 
		}

		this.updateLinkCapacities();
		
		/* Initially all nodes receive a "wake up to transmit" event, aligned at time zero or y asynchr => randomly chosen */
		for (Link e : currentNetPlan.getLinks())
		{
			final double updateTime = (update_isSynchronous.getBoolean())? update_averageInterUpdateTime.getDouble() : Math.max(0 , update_averageInterUpdateTime.getDouble() + update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_WAKEUPTOUPDATE , e));
		}

		/* Intialize the traces */
		this.stat_traceOf_ue = new TimeTrace ();
		this.stat_traceOf_re = new TimeTrace ();
		this.stat_traceOf_objFun = new TimeTrace ();
		this.stat_traceOf_netUtilityWithoutBeta = new TimeTrace ();
		this.stat_traceOf_netUtilityWithBeta = new TimeTrace ();

		this.stat_traceOf_ue.add(0.0, currentNetPlan.getVectorLinkCapacity());
		this.stat_traceOf_re.add(0.0, control_r_e.copy ());
		Pair<Double,Double> objFunc = computeObjectiveFunction ();
		this.stat_traceOf_objFun.add(0.0, objFunc.getFirst());
		this.stat_traceOf_netUtilityWithBeta.add(0.0, objFunc.getSecond());
		this.stat_traceOf_netUtilityWithoutBeta.add(0.0, objFunc.getSecond() / control_betaFactor.getDouble());
	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		final double t = event.getEventTime();
		switch (event.getEventType())
		{
		case UPDATE_WAKEUPTOUPDATE: // a node updates its p_n, p_e values, using most updated info available
		{
			final Link eMe = (Link) event.getEventObject();	
			final int eIdMe_index = eMe.getIndex ();
			
			//final double gradientThisLink = this.currentNetPlan.getLinkCapacity(eIdMe_SN) - this.control_intendedU_e.get(eIdMe_index) + 2*mac_maxSeMeasurementRelativeNoise*(r.nextDouble()-0.5);  
			//final double gradientThisLink = (this.currentNetPlan.getLinkCapacity(eIdMe_SN) - this.control_intendedU_e.get(eIdMe_index)) * (1 + 2*mac_maxSeMeasurementRelativeNoise*(r.nextDouble()-0.5));  
			final double gradientThisLink = eMe.getCapacity() * (1 + 2*control_maxSeMeasurementRelativeNoise.getDouble()*(rng.nextDouble()-0.5)) - this.control_intendedU_e.get(eIdMe_index);
			final double newRe = Math.max(0.001 , control_r_e.get(eIdMe_index) - this.gradient_gammaStep.getDouble() * gradientThisLink);

			if (Double.isNaN(gradientThisLink))
			{
				System.out.println("Gradient this links: " + gradientThisLink);
				System.out.println("control_intendedU_e: " + control_intendedU_e);
				System.out.println("getLinkCapacity: " + this.currentNetPlan.getVectorLinkCapacity());
				throw new RuntimeException ("Bad");
			}
			
			this.control_r_e.set(eIdMe_index, newRe);
			eMe.setAttribute("r_e" , "" + control_r_e.get(eIdMe_index)); 
			this.updateLinkCapacities();
			
			final double updateTime = update_isSynchronous.getBoolean()? t + update_averageInterUpdateTime.getDouble() : Math.max(t , t + update_averageInterUpdateTime.getDouble() + update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_WAKEUPTOUPDATE,  eMe));

			this.stat_traceOf_ue.add(t, currentNetPlan.getVectorLinkCapacity());
			this.stat_traceOf_re.add(t, control_r_e.copy ());
			Pair<Double,Double> objFunc = computeObjectiveFunction ();
			this.stat_traceOf_objFun.add(t, objFunc.getFirst());
			this.stat_traceOf_netUtilityWithBeta.add(t, objFunc.getSecond());
			this.stat_traceOf_netUtilityWithoutBeta.add(t, objFunc.getSecond() / control_betaFactor.getDouble());
			
			if (t > this.simulation_maxNumberOfUpdateIntervals.getDouble() * this.update_averageInterUpdateTime.getDouble()) { this.endSimulation (); }
			
			break;
		}
			

		default: throw new RuntimeException ("Unexpected received event");
		}
	}

	public String finish (StringBuilder st , double simTime)
	{
		if (simulation_outFileNameRoot.getString().equals("")) return null;
		stat_traceOf_ue.printToFile(new File (simulation_outFileNameRoot.getString() + "_ue.txt"));
		stat_traceOf_re.printToFile(new File (simulation_outFileNameRoot.getString() + "_re.txt"));
		stat_traceOf_netUtilityWithoutBeta.printToFile(new File (simulation_outFileNameRoot.getString() + "_netUtilityWithoutBeta.txt"));
		stat_traceOf_netUtilityWithBeta.printToFile(new File (simulation_outFileNameRoot.getString() + "_netUtilityWithBeta.txt"));
		stat_traceOf_objFun.printToFile(new File (simulation_outFileNameRoot.getString() + "_objFunc.txt"));

		Map<String,String> par = new HashMap<String,String> ();
		par.put("solverName", "ipopt");
		par.put("solverLibraryName", "");
		par.put("maxSolverTimeInSeconds", "-1");
		par.put("alphaFairnessFactor", "" + this.control_fairnessFactor.getDouble());
		par.put("betaFactor", "" + this.control_betaFactor.getDouble());
		par.put("linkNominalCapacity", "" + this.control_linkNominalCapacity.getDouble());
		par.put("minimumSchedFractionTime", "0.00001");
		new Offline_ca_wirelessCsmaWindowSize ().executeAlgorithm(copyInitialNetPlan, par, null);
		final double optimumCSMAObjectiveFunction = Double.parseDouble(copyInitialNetPlan.getAttribute("optimumCSMAObjectiveFunction"));
		final double optimumCSMAUtilityByBeta = Double.parseDouble(copyInitialNetPlan.getAttribute("optimumCSMAUtilityByBeta"));
		final double optimumCSMAUtility = Double.parseDouble(copyInitialNetPlan.getAttribute("optimumCSMAUtility"));
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_ue.txt"), copyInitialNetPlan.getVectorLinkCapacity());
		DoubleMatrix1D optimum_re = DoubleFactory1D.dense.make (E); for (Link e : copyInitialNetPlan.getLinks ()) optimum_re.set (e.getIndex () , Double.parseDouble (e.getAttribute("r_e")));
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_re.txt"), optimum_re);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_netUtilityWithoutBeta.txt"), optimumCSMAUtility);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_netUtilityWithBeta.txt"), optimumCSMAUtilityByBeta);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_objFunc.txt"), optimumCSMAObjectiveFunction);
		return null;
	}
	

	/* Applies the TAs and recomputes the link capacities from it */
	private void updateLinkCapacities ()
	{
		this.pi_m = A_em.zMult(control_r_e, null , 1 , 0 , true); // for each m, the sum of the associated r_es
		final double maxRm = pi_m.getMaxLocation() [0];
		for (int m = 0 ; m < M ; m ++) this.pi_m.set(m , this.pi_m.get(m) - maxRm);
		this.pi_m.assign(DoubleFunctions.exp);
		this.pi_m.assign(DoubleFunctions.div(pi_m.zSum()));

		DoubleMatrix1D u_e = A_em.zMult(pi_m,null);
		for (Link e : currentNetPlan.getLinks ())
		{
			e.setCapacity(u_e.get(e.getIndex ()) * control_linkNominalCapacities_e.get(e.getIndex ()));
			final double intended_ue = Math.pow(control_betaFactor.getDouble() / control_r_e.get(e.getIndex ()) , 1/control_fairnessFactor.getDouble());
			this.control_intendedU_e.set(e.getIndex (), intended_ue);
		}
	}

	private Pair<Double,Double> computeObjectiveFunction ()
	{
		double objFunc = 0;
		for (int m = 0 ; m < M ; m ++)
			objFunc -= (pi_m.get(m) == 0)? 0 : pi_m.get(m) * Math.log(pi_m.get(m));
		double netUtilityByBeta = control_betaFactor.getDouble() * NetworkPerformanceMetrics.alphaUtility(currentNetPlan.getVectorLinkCapacity() , control_fairnessFactor.getDouble());
		if (!Double.isFinite(netUtilityByBeta))
			netUtilityByBeta = -Double.MAX_VALUE;
		objFunc += netUtilityByBeta;
		return Pair.of(objFunc, netUtilityByBeta);
	}

}
