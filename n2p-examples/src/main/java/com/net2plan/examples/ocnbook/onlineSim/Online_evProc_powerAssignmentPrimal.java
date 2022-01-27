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
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import com.net2plan.examples.ocnbook.offline.Offline_ca_wirelessTransmissionPower;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.NetworkPerformanceMetrics;
import com.net2plan.libraries.WirelessUtils;
import com.net2plan.utils.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 
 * This module implements a distributed primal-gradient based algorithm for adjusting the transmission power of the links in a wireless network subject to interferences, to maximize the network utility enforcing a fair allocation of the resources.
 *
 * Ths event processor is adapted to permit observing the algorithm performances under user-defined conditions, 
 * including asynchronous distributed executions, where signaling can be affected by losses and/or delays, and/or measurement errors. 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec9_6_powerAssignmentPrimal.m">{@code fig_sec9_6_powerAssignmentPrimal.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * 
 * To simulate a network with this module, use the {@code Online_evGen_doNothing} generator.
 * 
 * @net2plan.keywords Transmission power optimization, Wireless, Distributed algorithm, Primal gradient algorithm, Capacity assignment (CA)
 * @net2plan.ocnbooksections Section 9.6
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
@SuppressWarnings("unchecked")
public class Online_evProc_powerAssignmentPrimal extends IEventProcessor
{
	private InputParameter signaling_isSynchronous = new InputParameter ("signaling_isSynchronous", false , "true if all the distributed agents involved wake up synchronously to send the signaling messages");
	private InputParameter signaling_averageInterMessageTime = new InputParameter ("signaling_averageInterMessageTime", 1.0 , "Average time between two signaling messages sent by an agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter signaling_maxFluctuationInterMessageTime = new InputParameter ("signaling_maxFluctuationInterMessageTime", 0.5 , "Max fluctuation in time between two signaling messages sent by an agent" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter signaling_averageDelay = new InputParameter ("signaling_averageDelay", 0.0 , "Average time between signaling message transmission by an agent and its reception by other or others" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter signaling_maxFluctuationInDelay = new InputParameter ("signaling_maxFluctuationInDelay", 0.0 , "Max fluctuation in time in the signaling delay, in absolute time values. The signaling delays are sampled from a uniform distribution within the given interval" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter signaling_signalingLossProbability = new InputParameter ("signaling_signalingLossProbability", 0.05 , "Probability that a signaling message transmitted is lost (not received by other or others involved agents)" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter update_isSynchronous = new InputParameter ("update_isSynchronous", false , "true if all the distributed agents involved wake up synchronousely to update its state");
	private InputParameter update_averageInterUpdateTime = new InputParameter ("update_averageInterUpdateTime", 1.0 , "Average time between two updates of an agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter update_maxFluctuationInterUpdateTime = new InputParameter ("update_maxFluctuationInterUpdateTime", 0.5 , "Max fluctuation in time in the update interval of an agent, in absolute time values. The update intervals are sampled from a uniform distribution within the given interval" , 0 , true , Double.MAX_VALUE , true);
	
	private InputParameter gradient_maxGradientAbsoluteNoise = new InputParameter ("gradient_maxGradientAbsoluteNoise", 0.01 , "Max value of the added noise to the gradient coordinate in absolute values" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter gradient_gammaStep = new InputParameter ("gradient_gammaStep", 10.0 , "Gamma step in the gradient algorithm" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter gradient_heavyBallBetaParameter = new InputParameter ("gradient_heavyBallBetaParameter", 0.0 , "Beta parameter of heavy ball, between 0 and 1. Value 0 means no heavy ball" , 0 , true , 1.0 , true);
	private InputParameter gradient_maxGradientCoordinateChange = new InputParameter ("gradient_maxGradientCoordinateChange", 1000.0 , "Maximum change in an iteration of a gradient coordinate" , 0 , false , Double.MAX_VALUE , true);

	private InputParameter simulation_randomSeed = new InputParameter ("simulation_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter simulation_outFileNameRoot = new InputParameter ("simulation_outFileNameRoot", "powerTransmissionAsignmentPrimal" , "Root of the file name to be used in the output files. If blank, no output");
	private InputParameter simulation_maxNumberOfUpdateIntervals = new InputParameter ("simulation_maxNumberOfUpdateIntervals", 100.0 , "Maximum number of update intervals in average per agent" , 0 , false , Double.MAX_VALUE , true);
	
	private InputParameter control_pathLossExponent = new InputParameter ("control_pathLossExponent", 3.0 , "Exponent in the model for propagation losses" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter control_worseRiseOverThermal_nu = new InputParameter ("control_worseRiseOverThermal_nu", 10.0 , "Worse case ratio between interference power and thermal power at any receiver. Used to set the common thermal power in the receivers" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter control_interferenceAttenuationFactor_nu = new InputParameter ("control_interferenceAttenuationFactor_nu", 1.0e6 , "The interference power received in natural units is divided by this to reduce its effect" , 1 , true , Double.MAX_VALUE , true);
	private InputParameter control_maxTransmissionPower_logu = new InputParameter ("control_maxTransmissionPower_logu", 3.0 , "The maximum link transmission power in logarithmic units (e.g. dBm)");
	private InputParameter control_minTransmissionPower_logu = new InputParameter ("control_minTransmissionPower_logu", 0.0 , "The minimum link transmission power in logarithmic units (e.g. dBm)");
	private InputParameter control_fairnessFactor = new InputParameter ("control_fairnessFactor", 2.0 , "Fairness factor in utility function of link capacities" , 0 , true , Double.MAX_VALUE , true);

	private static final int SIGNALING_WAKEUPTOSENDMESSAGE = 300;
	private static final int SIGNALING_RECEIVEDMESSAGE = 301;
	private static final int UPDATE_WAKEUPTOUPDATE = 302;

	private Random rng;
	private int E , N;
	private DoubleMatrix2D mac_g_nu_ee;
	private DoubleMatrix1D mac_transmissionPower_logu_e; 
	private DoubleMatrix1D mac_previousTransmissionPower_logu_e; 
	private double mac_receptionThermalNoise_nu;
	
	private DoubleMatrix2D control_mostUpdatedMe2ValueKnownByLink1_e1e2; 
			
	private TimeTrace stat_traceOf_u_e;
	private TimeTrace stat_traceOf_p_e;
	private TimeTrace stat_traceOf_objFunction;

	private NetPlan currentNetPlan , copyInitialNetPlan;
	
	@Override
	public String getDescription()
	{
		return "This module implements a distributed primal-gradient based algorithm for adjusting the transmission power of the links in a wireless network subject to interferences, to maximize the network utility enforcing a fair allocation of the resources.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	@Override
	public void initialize(NetPlan currentNp, Map<String, String> algorithmParameters, Map<String, String> simulationParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		this.currentNetPlan = currentNp;
		this.copyInitialNetPlan = currentNp.copy ();
		this.E = currentNp.getNumberOfLinks ();
		this.N = currentNp.getNumberOfNodes ();
		if (E == 0) throw new Net2PlanException ("The input design should have links");
		
		if (currentNp.getNumberOfLayers() != 1) throw new Net2PlanException ("This algorithm works in single layer networks");

		this.rng = new Random (simulation_randomSeed.getLong ());

		/* Initialize the gains between links, normalizing them so that the maximum gain is one */
		this.mac_g_nu_ee = WirelessUtils.computeInterferenceMatrixNaturalUnits (currentNetPlan.getLinks () , control_interferenceAttenuationFactor_nu.getDouble() , control_pathLossExponent.getDouble());
		//System.out.println("NOT normalized Gnu_ee: " + Gnu_ee);
		final double maxGainValue = mac_g_nu_ee.getMaxLocation() [0];
		mac_g_nu_ee.assign (DoubleFunctions.div(maxGainValue));
		//System.out.println("normalized mac_g_nu_ee: " + mac_g_nu_ee);
		
		/* Initialize the thermal noise at the receivers, to have a worse case ROT (rise over thermal) */
		double worseInterferenceReceivedAtMaxPower_nu = WirelessUtils.computeWorseReceiverInterferencePower_nu (control_maxTransmissionPower_logu.getDouble()  , mac_g_nu_ee);

		/* Adjust the thermal noise in the receivers so that we have a given ROT */
		this.mac_receptionThermalNoise_nu = worseInterferenceReceivedAtMaxPower_nu / control_worseRiseOverThermal_nu.getDouble();

		/* Initialize the transmission power variables */
		this.mac_transmissionPower_logu_e = DoubleFactory1D.dense.make (E , control_minTransmissionPower_logu.getDouble());
		this.mac_previousTransmissionPower_logu_e = DoubleFactory1D.dense.make (E , control_minTransmissionPower_logu.getDouble());
			
		/* Update the netplan object with the resulting capacities */
		for (Link e : currentNp.getLinks())
			e.setCapacity(Math.log(computeSINR_e (e)));

		this.control_mostUpdatedMe2ValueKnownByLink1_e1e2 = DoubleFactory2D.dense.make (E,E);
		for (Link e1 : currentNp.getLinks())
		{
			for (Link otherLink : currentNp.getLinks())
				if (e1 != otherLink)
					control_mostUpdatedMe2ValueKnownByLink1_e1e2.set(e1.getIndex () , otherLink.getIndex(), computeMeFactor_e (otherLink));
		}
		
		/* Initially all links receive a "wake up to transmit" event, aligned at time zero or y asynchr => randomly chosen */
		for (Link e : currentNp.getLinks())
		{
			final double signalingTime = (signaling_isSynchronous.getBoolean())? signaling_averageInterMessageTime.getDouble() : Math.max(0 , signaling_averageInterMessageTime.getDouble() + signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_WAKEUPTOSENDMESSAGE , e));
			final double updateTime = (update_isSynchronous.getBoolean())? update_averageInterUpdateTime.getDouble() : Math.max(0 , update_averageInterUpdateTime.getDouble() + update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_WAKEUPTOUPDATE , e));
		}

		/* Intialize the traces */
		this.stat_traceOf_u_e = new TimeTrace ();
		this.stat_traceOf_p_e = new TimeTrace ();
		this.stat_traceOf_objFunction = new TimeTrace ();
		this.stat_traceOf_u_e.add(0.0, this.currentNetPlan.getVectorLinkCapacity());
		this.stat_traceOf_p_e.add(0.0, this.mac_transmissionPower_logu_e.copy ());
		this.stat_traceOf_objFunction.add(0.0 , NetworkPerformanceMetrics.alphaUtility(currentNetPlan.getVectorLinkCapacity() , control_fairnessFactor.getDouble()));

		/* */
//		System.out.println("Initialize control_mostUpdatedMe2ValueKnownByLink1_e1e2: " + this.control_mostUpdatedMe2ValueKnownByLink1_e1e2);
//		System.out.println("mac_g_nu_ee: " + this.mac_g_nu_ee);
	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		final double t = event.getEventTime();
		switch (event.getEventType())
		{
		case SIGNALING_RECEIVEDMESSAGE: // a link receives the signaling informatio
		{
			final Pair<Link,Pair<Link,Double>> signalInfo = (Pair<Link,Pair<Link,Double>>) event.getEventObject();
			final Link eMe = signalInfo.getFirst();
			final Link otherLink = signalInfo.getSecond().getFirst();
			final double otherLink_me = signalInfo.getSecond().getSecond();
			control_mostUpdatedMe2ValueKnownByLink1_e1e2.set (eMe.getIndex () , otherLink.getIndex () , otherLink_me);
			break;
		}
		
		case SIGNALING_WAKEUPTOSENDMESSAGE: // A link sends signaling information
		{
			final Link eMe = (Link) event.getEventObject();

			Pair<Link,Double> infoToSignal = Pair.of(eMe , this.computeMeFactor_e(eMe));
			if (rng.nextDouble() >= this.signaling_signalingLossProbability.getDouble()) // the signaling may be lost => lost to all nodes
				for (Link otherLink : currentNetPlan.getLinks ())
					if (otherLink != eMe)
					{
						final double signalingReceptionTime = t + Math.max(0 , signaling_averageDelay.getDouble() + signaling_maxFluctuationInDelay.getDouble() * (rng.nextDouble() - 0.5));
						this.scheduleEvent(new SimEvent (signalingReceptionTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_RECEIVEDMESSAGE , Pair.of(otherLink , infoToSignal)));
					}

			/* Re-schedule when to wake up again */
			final double signalingTime = signaling_isSynchronous.getBoolean()? t + signaling_averageInterMessageTime.getDouble() : Math.max(t , t + signaling_averageInterMessageTime.getDouble() + signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_WAKEUPTOSENDMESSAGE , eMe));
			break;
		}

		case UPDATE_WAKEUPTOUPDATE: // a link updates its power 
		{
			final Link eMe = (Link) event.getEventObject();	
			
			final double currentTransmissionPower_logu = this.mac_transmissionPower_logu_e.get(eMe.getIndex ());
			double gradientThisLink = computeGradient (eMe) + 2*gradient_maxGradientAbsoluteNoise.getDouble()*(rng.nextDouble()-0.5);
			double nextTransmissionPower_logu = currentTransmissionPower_logu + this.gradient_gammaStep.getDouble() * gradientThisLink;
			
			/* heavy ball */
			nextTransmissionPower_logu += this.gradient_heavyBallBetaParameter.getDouble() * (currentTransmissionPower_logu - mac_previousTransmissionPower_logu_e.get(eMe.getIndex ()));
			/* projection */
			nextTransmissionPower_logu = GradientProjectionUtils.euclideanProjection_boxLike(nextTransmissionPower_logu, this.control_minTransmissionPower_logu.getDouble(), this.control_maxTransmissionPower_logu.getDouble());

			if (gradient_maxGradientCoordinateChange.getDouble() > 0)
				nextTransmissionPower_logu = GradientProjectionUtils.scaleDown_maxAbsoluteCoordinateChange (currentTransmissionPower_logu , nextTransmissionPower_logu ,  gradient_maxGradientCoordinateChange.getDouble());
			
			this.mac_previousTransmissionPower_logu_e.set(eMe.getIndex (), this.mac_transmissionPower_logu_e.get(eMe.getIndex ()));
			this.mac_transmissionPower_logu_e.set(eMe.getIndex (), nextTransmissionPower_logu);

			//System.out.println("Link " + eIdMe + ": mac_transmissionPower_logu_e values Before: " + currentTransmissionPower_logu + ", after: " + nextTransmissionPower_logu);

			/* Send event next recomputing time */
			final double updateTime = update_isSynchronous.getBoolean()? t + update_averageInterUpdateTime.getDouble() : Math.max(t , t + update_averageInterUpdateTime.getDouble() + update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_WAKEUPTOUPDATE,  eMe));

			/* Update in currentNetPlan the capacity values */
			for (Link e : this.currentNetPlan.getLinks())
				e.setCapacity(Math.log(computeSINR_e (e)));

			this.stat_traceOf_u_e.add(t, this.currentNetPlan.getVectorLinkCapacity());
			this.stat_traceOf_p_e.add(t, this.mac_transmissionPower_logu_e.copy ());
			this.stat_traceOf_objFunction.add(t , NetworkPerformanceMetrics.alphaUtility(currentNetPlan.getVectorLinkCapacity() , control_fairnessFactor.getDouble()));

			if (t > this.simulation_maxNumberOfUpdateIntervals.getDouble() * this.update_averageInterUpdateTime.getDouble()) { this.endSimulation (); }
			
			break;
		}

		default: throw new RuntimeException ("Unexpected received event");
		}
		
		
	}

	public String finish (StringBuilder st , double simTime)
	{
		if (simulation_outFileNameRoot.getString().equals("")) return null;
		stat_traceOf_u_e.printToFile(new File (simulation_outFileNameRoot.getString() + "_ue.txt"));
		stat_traceOf_objFunction.printToFile(new File (simulation_outFileNameRoot.getString() + "_objFunc.txt"));
		stat_traceOf_p_e.printToFile(new File (simulation_outFileNameRoot.getString() + "_pe.txt"));
		/* compute optimum solution */
		Map<String,String> param = new HashMap<String,String> ();
		param.put("solverName", "ipopt");
		param.put("solverLibraryName", "");
		param.put("maxSolverTimeInSeconds", "-1");
		param.put("alphaFairnessFactor", "" + this.control_fairnessFactor.getDouble());
		param.put("pathLossExponent", "" + this.control_pathLossExponent.getDouble());
		param.put("worseRiseOverThermal_nu", "" + this.control_worseRiseOverThermal_nu.getDouble());
		param.put("interferenceAttenuationFactor_nu", "" + this.control_interferenceAttenuationFactor_nu.getDouble());
		param.put("maxTransmissionPower_logu", "" + this.control_maxTransmissionPower_logu.getDouble());
		param.put("minTransmissionPower_logu", "" + this.control_minTransmissionPower_logu.getDouble());
		new Offline_ca_wirelessTransmissionPower ().executeAlgorithm(copyInitialNetPlan , param , null);
		final double optimumNetUtilityJOM = NetworkPerformanceMetrics.alphaUtility(copyInitialNetPlan.getVectorLinkCapacity() , control_fairnessFactor.getDouble());
		DoubleMatrix1D optimum_ue = copyInitialNetPlan.getVectorLinkCapacity();
		DoubleMatrix1D optimum_pe = DoubleFactory1D.dense.make (E);
		for (Link e : copyInitialNetPlan.getLinks ()) optimum_pe.set (e.getIndex () , Double.parseDouble (e.getAttribute("p_e")));
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_ue.txt"), optimum_ue);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_pe.txt"), optimum_pe);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_objFunc.txt"), optimumNetUtilityJOM);
		return null;
	}
	

	private double computeGradient (Link thisLink)
	{
		final double u_e = thisLink.getCapacity();
		final DoubleMatrix1D infoIKnow_me = control_mostUpdatedMe2ValueKnownByLink1_e1e2.viewRow(thisLink.getIndex ());
		
		double gradient = Math.pow(u_e, -this.control_fairnessFactor.getDouble());
		double accumFactor = 0;
		for (Link epp : this.currentNetPlan.getLinks())
			if (epp != thisLink)
				accumFactor += this.mac_g_nu_ee.get(thisLink.getIndex (),epp.getIndex ()) * infoIKnow_me.get(epp.getIndex ());
		accumFactor *= Math.exp(this.mac_transmissionPower_logu_e.get(thisLink.getIndex ()));
		//System.out.println("Gradient: positive factor: " + gradient + ", negative factor: " + accumFactor);
		gradient -= accumFactor;
		return gradient;
	}

	private double computeMeFactor_e (Link e)
	{
		final double u_e = e.getCapacity();
		final double snr_e = Math.exp(u_e);
		return  Math.pow(u_e,-this.control_fairnessFactor.getDouble()) * snr_e / (Math.exp(this.mac_transmissionPower_logu_e.get(e.getIndex ())) * this.mac_g_nu_ee.get(e.getIndex (),e.getIndex ()));
	}
	
	private double computeSINR_e (Link e)
	{
		final double receivedPower_nu = Math.exp(this.mac_transmissionPower_logu_e.get(e.getIndex ())) * this.mac_g_nu_ee.get(e.getIndex (),e.getIndex ());
		double interferencePower_nu = this.mac_receptionThermalNoise_nu;
		for (Link eInt : this.currentNetPlan.getLinks ())
			if (eInt != e) interferencePower_nu += Math.exp(this.mac_transmissionPower_logu_e.get(eInt.getIndex ())) * this.mac_g_nu_ee.get(eInt.getIndex (),e.getIndex ());
//		System.out.println ("SINR link " + e + ": " + receivedPower_nu / interferencePower_nu);
//		System.out.println ("receiver power link " + e + ": " + receivedPower_nu + ", total interf power: " + interferencePower_nu + "thermal noise: " + mac_receptionThermalNoise_nu);
		return receivedPower_nu / interferencePower_nu;
	}

}
