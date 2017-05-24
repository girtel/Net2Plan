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
import com.net2plan.examples.ocnbook.offline.Offline_cba_wirelessCongControlTransmissionPowerAssignment;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.NetworkPerformanceMetrics;
import com.net2plan.libraries.WirelessUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 
 * This module implements a distributed dual-decomposition-based gradient algorithm, for a coordinated adjustment of the traffic to inject by each demand (congestion control), and the transmission power in each link of the underlying wireless network, to maximize the network utility enforcing a fair allocation of the resources.
 *
 * Ths event processor is adapted to permit observing the algorithm performances under user-defined conditions, 
 * including asynchronous distributed executions, where signaling can be affected by losses and/or delays, and/or measurement errors. 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec11_4_congControlAndBackpressureRouting_dualDecomp.m">{@code fig_sec11_4_congControlAndBackpressureRouting_dualDecomp.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * 
 * To simulate a network with this module, use the {@code Online_evGen_doNothing} generator.
 * 
 * @net2plan.keywords Bandwidth assignment (BA), Transmission power optimization, Wireless, Capacity assignment (CA), Distributed algorithm, Dual decomposition 
 * @net2plan.ocnbooksections Section 11.4
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
@SuppressWarnings("unchecked")
public class Online_evProc_congControlAndTransmissionPowerAssignmentDualDecomp extends IEventProcessor
{
	private Random rng;

	static final int MAC_SIGNALING_WAKEUPTOSENDMESSAGE = 200;
	static final int MAC_SIGNALING_RECEIVEDMESSAGE = 201;
	static final int MAC_UPDATE_WAKEUPTOUPDATE = 202;

	static final int CC_SIGNALING_WAKEUPTOSENDMESSAGE = 400;
	static final int CC_SIGNALING_RECEIVEDMESSAGE = 401;
	static final int CC_UPDATE_WAKEUPTOUPDATE = 402;

	
	private InputParameter mac_signaling_isSynchronous = new InputParameter ("mac_signaling_isSynchronous", false , "true if all the distributed agents involved wake up synchronously to send the signaling messages");
	private InputParameter mac_signaling_averageInterMessageTime = new InputParameter ("mac_signaling_averageInterMessageTime", 1.0 , "Average time between two signaling messages sent by an agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter mac_signaling_maxFluctuationInterMessageTime = new InputParameter ("mac_signaling_maxFluctuationInterMessageTime", 0.5 , "Max fluctuation in time between two signaling messages sent by an agent" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter mac_signaling_averageDelay = new InputParameter ("mac_signaling_averageDelay", 0.0 , "Average time between signaling message transmission by an agent and its reception by other or others" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter mac_signaling_maxFluctuationInDelay = new InputParameter ("mac_signaling_maxFluctuationInDelay", 0.0 , "Max fluctuation in time in the signaling delay, in absolute time values. The signaling delays are sampled from a uniform distribution within the given interval" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter mac_signaling_signalingLossProbability = new InputParameter ("mac_signaling_signalingLossProbability", 0.05 , "Probability that a signaling message transmitted is lost (not received by other or others involved agents)" , 0 , true , Double.MAX_VALUE , true);

	private InputParameter mac_update_isSynchronous = new InputParameter ("mac_update_isSynchronous", false , "true if all the distributed agents involved wake up synchronousely to update its state");
	private InputParameter mac_update_averageInterUpdateTime = new InputParameter ("mac_update_averageInterUpdateTime", 1.0 , "Average time between two updates of an agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter mac_update_maxFluctuationInterUpdateTime = new InputParameter ("mac_update_maxFluctuationInterUpdateTime", 0.5 , "Max fluctuation in time in the update interval of an agent, in absolute time values. The update intervals are sampled from a uniform distribution within the given interval" , 0 , true , Double.MAX_VALUE , true);

	private InputParameter mac_gradient_maxGradientAbsoluteNoise = new InputParameter ("mac_gradient_maxGradientAbsoluteNoise", 0.01 , "Max value of the added noise to the gradient coordinate in absolute values" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter mac_gradient_gammaStep = new InputParameter ("mac_gradient_gammaStep", 1.0 , "Gamma step in the gradient algorithm" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter mac_gradient_heavyBallBetaParameter = new InputParameter ("mac_gradient_heavyBallBetaParameter", 0.0 , "Beta parameter of heavy ball, between 0 and 1. Value 0 means no heavy ball" , 0 , true , 1.0 , true);
	private InputParameter mac_gradient_maxGradientCoordinateChange = new InputParameter ("mac_gradient_maxGradientCoordinateChange", 1000.0 , "Maximum change in an iteration of a gradient coordinate" , 0 , false , Double.MAX_VALUE , true);

	private InputParameter mac_pathLossExponent = new InputParameter ("mac_pathLossExponent", 3.0 , "Exponent in the model for propagation losses" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter mac_worseRiseOverThermal_nu = new InputParameter ("mac_worseRiseOverThermal_nu", 10.0 , "Worse case ratio between interference power and thermal power at any receiver. Used to set the common thermal power in the receivers" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter mac_interferenceAttenuationFactor_nu = new InputParameter ("mac_interferenceAttenuationFactor_nu", 1.0e6 , "The interference power received in natural units is divided by this to reduce its effect" , 1 , true , Double.MAX_VALUE , true);
	private InputParameter mac_maxTransmissionPower_logu = new InputParameter ("mac_maxTransmissionPower_logu", 3.0 , "The maximum link transmission power in logarithmic units (e.g. dBm)");
	private InputParameter mac_minTransmissionPower_logu = new InputParameter ("mac_minTransmissionPower_logu", 0.0 , "The minimum link transmission power in logarithmic units (e.g. dBm)");
	private InputParameter mac_simulation_maxNumberOfUpdateIntervals = new InputParameter ("mac_simulation_maxNumberOfUpdateIntervals", 2000.0 , "Maximum number of update intervals in average per agent" , 0 , false , Double.MAX_VALUE , true);

	private InputParameter simulation_randomSeed = new InputParameter ("simulation_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter simulation_outFileNameRoot = new InputParameter ("simulation_outFileNameRoot", "crossLayerCongControlPowerTransmissionAsignmentDualDecomp" , "Root of the file name to be used in the output files. If blank, no output");

	private InputParameter cc_signaling_isSynchronous = new InputParameter ("cc_signaling_isSynchronous", false , "true if all the distributed agents involved wake up synchronously to send the signaling messages");
	private InputParameter cc_signaling_averageInterMessageTime = new InputParameter ("cc_signaling_averageInterMessageTime", 10.0 , "Average time between two signaling messages sent by an agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter cc_signaling_maxFluctuationInterMessageTime = new InputParameter ("cc_signaling_maxFluctuationInterMessageTime", 5.0 , "Max fluctuation in time between two signaling messages sent by an agent" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_signaling_averageDelay = new InputParameter ("cc_signaling_averageDelay", 30.0 , "Average time between signaling message transmission by an agent and its reception by other or others" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_signaling_maxFluctuationInDelay = new InputParameter ("cc_signaling_maxFluctuationInDelay", 15.0 , "Max fluctuation in time in the signaling delay, in absolute time values. The signaling delays are sampled from a uniform distribution within the given interval" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_signaling_signalingLossProbability = new InputParameter ("cc_signaling_signalingLossProbability", 0.05 , "Probability that a signaling message transmitted is lost (not received by other or others involved agents)" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_update_isSynchronous = new InputParameter ("cc_update_isSynchronous", false , "true if all the distributed agents involved wake up synchronousely to update its state");
	private InputParameter cc_update_averageInterUpdateTime = new InputParameter ("cc_update_averageInterUpdateTime", 10.0 , "Average time between two updates of an agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter cc_update_maxFluctuationInterUpdateTime = new InputParameter ("cc_update_maxFluctuationInterUpdateTime", 5.0 , "Max fluctuation in time in the update interval of an agent, in absolute time values. The update intervals are sampled from a uniform distribution within the given interval" , 0 , true , Double.MAX_VALUE , true);

	private InputParameter cc_gradient_gammaStep = new InputParameter ("cc_gradient_gammaStep", 0.001 , "Gamma step in the gradient algorithm" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter cc_gradient_maxGradientAbsoluteNoise = new InputParameter ("cc_gradient_maxGradientAbsoluteNoise", 0.0 , "Max value of the added noise to the gradient coordinate in absolute values" , 0 , true , Double.MAX_VALUE , true);

	private InputParameter cc_minHd = new InputParameter ("cc_minHd", 0.1 , "Minimum traffic assigned to each demand" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_maxHd = new InputParameter ("cc_maxHd", 1.0E6 , "Maximum traffic assigned to each demand" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_fairnessFactor = new InputParameter ("cc_fairnessFactor", 1.0 , "Fairness factor in utility function of congestion control" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_initialLinkPrices = new InputParameter ("cc_initialLinkPrices", 1 , "Initial value of the link weights" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_simulation_maxNumberOfUpdateIntervals = new InputParameter ("cc_simulation_maxNumberOfUpdateIntervals", 2000.0 , "Maximum number of update intervals in average per agent" , 0 , false , Double.MAX_VALUE , true);

	private Map<String, String> algorithmParameters;
	private Map<String, String> net2planParameters;

	private DoubleMatrix1D cc_price_e;
	private DoubleMatrix2D cc_mostUpdatedLinkPriceKnownByDemand_de; 
	
	private DoubleMatrix2D mac_g_nu_ee;
	private DoubleMatrix1D mac_transmissionPower_logu_e; 
	private DoubleMatrix1D mac_previousTransmissionPower_logu_e; 
	private double mac_receptionThermalNoise_nu;
	private DoubleMatrix2D mac_mostUpdatedMe2ValueKnownByLink1_e1e2; 
			
	private TimeTrace traceOf_pi_e;
	private TimeTrace traceOf_u_e;
	private TimeTrace traceOf_y_e;
	private TimeTrace traceOf_p_e;
	private TimeTrace traceOf_h_d;
	private TimeTrace traceOf_objFunction;

	private NetPlan currentNetPlan;
	private NetPlan copyInitialNetPlan;
	private int E,N,D;
	
	@Override
	public String getDescription()
	{
		return "This module implements a distributed dual-decomposition-based gradient algorithm, for a coordinated adjustment of the traffic to inject by each demand (congestion control), and the transmission power in each link of the underlying wireless network, to maximize the network utility enforcing a fair allocation of the resources.";
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
		this.copyInitialNetPlan = currentNp.copy();
		this.algorithmParameters = algorithmParameters;
		this.net2planParameters = net2planParameters;
		this.N = this.currentNetPlan.getNumberOfNodes();
		this.E = this.currentNetPlan.getNumberOfLinks();
		if (currentNp.getNumberOfLayers() != 1) throw new Net2PlanException ("This algorithm works in single layer networks");
		if (E == 0) throw new Net2PlanException ("The input design should have links");

		this.rng = new Random (simulation_randomSeed.getLong ());
		
		/* INITIALIZE INFORMATION COMMON TO MAC AND CONGESTION CONTROL */
		this.cc_price_e = DoubleFactory1D.dense.make (E , cc_initialLinkPrices.getDouble());
		
		/* Remove all routes, and create one with the shortest path in km for each demand */
		this.currentNetPlan.removeAllDemands();
		currentNetPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
		for (Node n1 : this.currentNetPlan.getNodes())
			for (Node n2 : this.currentNetPlan.getNodes())
				if (n1 != n2)
					this.currentNetPlan.addDemand(n1, n2, cc_minHd.getDouble(), null);
		this.currentNetPlan.addRoutesFromCandidatePathList(currentNetPlan.computeUnicastCandidatePathList(currentNetPlan.getVectorLinkLengthInKm() , 1, -1, -1, -1, -1, -1, -1 , null));

		
		for (Route r : currentNetPlan.getRoutes ()) r.setCarriedTraffic(r.getDemand().getOfferedTraffic() , r.getDemand().getOfferedTraffic());
		this.D = this.currentNetPlan.getNumberOfDemands();
		
		/* Initialize the gains between links, normalizing them so that the maximum gain is one */
		this.mac_g_nu_ee = WirelessUtils.computeInterferenceMatrixNaturalUnits (currentNetPlan.getLinks () , mac_interferenceAttenuationFactor_nu.getDouble() , mac_pathLossExponent.getDouble());
		final double maxGainValue = mac_g_nu_ee.getMaxLocation() [0];
		mac_g_nu_ee.assign (DoubleFunctions.div(maxGainValue));
		
		/* Initialize the thermal noise at the receivers, to have a worse case ROT (rise over thermal) */
		double worseInterferenceReceivedAtMaxPower_nu = WirelessUtils.computeWorseReceiverInterferencePower_nu (mac_maxTransmissionPower_logu.getDouble()  , mac_g_nu_ee);

		/* Adjust the thermal noise in the receivers so that we have a given ROT */
		this.mac_receptionThermalNoise_nu = worseInterferenceReceivedAtMaxPower_nu / mac_worseRiseOverThermal_nu.getDouble();

		/* Initialize the transmission power variables */
		this.mac_transmissionPower_logu_e = DoubleFactory1D.dense.make (E , mac_minTransmissionPower_logu.getDouble());
		this.mac_previousTransmissionPower_logu_e = DoubleFactory1D.dense.make (E , mac_minTransmissionPower_logu.getDouble());
			
		/* Update the netplan object with the resulting capacities */
		for (Link e : currentNp.getLinks())
			e.setCapacity(Math.log(computeSINR_e (e)));

		this.mac_mostUpdatedMe2ValueKnownByLink1_e1e2 = DoubleFactory2D.dense.make (E,E);
		for (Link e1 : currentNp.getLinks())
		{
			for (Link otherLink : currentNp.getLinks())
				if (e1 != otherLink)
					mac_mostUpdatedMe2ValueKnownByLink1_e1e2.set(e1.getIndex () , otherLink.getIndex(), computeMeFactor_e (otherLink));
		}
		
		/* Initially all nodes receive a "wake up to transmit" event, aligned at time zero or y asynchr => randomly chosen */
		for (Link e : currentNp.getLinks())
		{
			final double signalingTime = (mac_signaling_isSynchronous.getBoolean())? mac_signaling_averageInterMessageTime.getDouble() : Math.max(0 , mac_signaling_averageInterMessageTime.getDouble() + mac_signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , MAC_SIGNALING_WAKEUPTOSENDMESSAGE , e));
			final double updateTime = (mac_update_isSynchronous.getBoolean())? mac_update_averageInterUpdateTime.getDouble() : Math.max(0 , mac_update_averageInterUpdateTime.getDouble() + mac_update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , MAC_UPDATE_WAKEUPTOUPDATE , e));
		}
		
		/* INITIALIZATION CONGESTION CONTROL LAYER */

		/* Initialize the information each demand knows of the prices of all the links */
		this.cc_mostUpdatedLinkPriceKnownByDemand_de = DoubleFactory2D.dense.make (D,E,cc_initialLinkPrices.getDouble());
		
		/* Compute the traffic each demand injects, set it in net2plan, and tell routing layer there was an update in this */
		for (Demand d : currentNetPlan.getDemands())
		{
			final double h_d = this.computeHdFromPrices(d);
			d.setOfferedTraffic(h_d);
			d.getRoutes ().iterator().next().setCarriedTraffic(h_d , h_d);
		}
		
		/* Initially all nodes receive a "wake up to transmit" event, aligned at time zero or y asynchr => randomly chosen */
		for (Link e : currentNetPlan.getLinks())
		{
			final double signalingTime = (cc_signaling_isSynchronous.getBoolean())? cc_signaling_averageInterMessageTime.getDouble() : Math.max(0 , cc_signaling_averageInterMessageTime.getDouble() + cc_signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , CC_SIGNALING_WAKEUPTOSENDMESSAGE , e));
		}
		for (Demand d : currentNetPlan.getDemands())
		{
			final double updateTime = (cc_update_isSynchronous.getBoolean())? cc_update_averageInterUpdateTime.getDouble() : Math.max(0 , cc_update_averageInterUpdateTime.getDouble() + cc_update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , CC_UPDATE_WAKEUPTOUPDATE , d));
		}

		/* INITIALIZE STATISTIC VARIABLES FOR BOTH LAYERS */
		this.traceOf_u_e = new TimeTrace  ();
		this.traceOf_y_e = new TimeTrace  ();
		this.traceOf_p_e = new TimeTrace ();
		this.traceOf_pi_e = new TimeTrace ();
		this.traceOf_h_d = new TimeTrace (); 
		this.traceOf_objFunction = new TimeTrace ();

		this.traceOf_u_e.add(0.0, this.currentNetPlan.getVectorLinkCapacity());
		this.traceOf_y_e.add(0.0, this.currentNetPlan.getVectorLinkCarriedTraffic());
		this.traceOf_p_e.add(0.0, this.mac_transmissionPower_logu_e.copy ());
		this.traceOf_pi_e.add(0.0, this.cc_price_e.copy ());
		this.traceOf_h_d.add(0.0, this.currentNetPlan.getVectorDemandCarriedTraffic());
		this.traceOf_objFunction.add(0.0 , NetworkPerformanceMetrics.alphaUtility(this.currentNetPlan.getVectorDemandOfferedTraffic() , cc_fairnessFactor.getDouble()));
		
	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		final double t = event.getEventTime();
		switch (event.getEventType())
		{
		case MAC_SIGNALING_RECEIVEDMESSAGE: // A node broadcasts signaling info to its 1 hop neighbors
		{
			final Pair<Link,Pair<Link,Double>> signalInfo = (Pair<Link,Pair<Link,Double>>) event.getEventObject();
			final Link eMe = signalInfo.getFirst();
			final Link otherLink = signalInfo.getSecond().getFirst();
			final double otherLink_me = signalInfo.getSecond().getSecond();
			mac_mostUpdatedMe2ValueKnownByLink1_e1e2.set (eMe.getIndex () , otherLink.getIndex () , otherLink_me);
			break;
		}
		
		case MAC_SIGNALING_WAKEUPTOSENDMESSAGE: // A node broadcasts signaling info to its 1 hop neighbors
		{
			final Link eMe = (Link) event.getEventObject();

			Pair<Link,Double> infoToSignal = Pair.of(eMe , this.computeMeFactor_e(eMe));
			if (rng.nextDouble() >= this.mac_signaling_signalingLossProbability.getDouble()) // the signaling may be lost => lost to all nodes
				for (Link otherLink : currentNetPlan.getLinks ())
					if (otherLink != eMe)
					{
						final double signalingReceptionTime = t + Math.max(0 , mac_signaling_averageDelay.getDouble() + mac_signaling_maxFluctuationInDelay.getDouble() * (rng.nextDouble() - 0.5));
						this.scheduleEvent(new SimEvent (signalingReceptionTime , SimEvent.DestinationModule.EVENT_PROCESSOR , MAC_SIGNALING_RECEIVEDMESSAGE , Pair.of(otherLink , infoToSignal)));
					}

			/* Re-schedule when to wake up again */
			final double signalingTime = mac_signaling_isSynchronous.getBoolean()? t + mac_signaling_averageInterMessageTime.getDouble() : Math.max(t , t + mac_signaling_averageInterMessageTime.getDouble() + mac_signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , MAC_SIGNALING_WAKEUPTOSENDMESSAGE , eMe));
			break;
		}

		case MAC_UPDATE_WAKEUPTOUPDATE: 
		{
			final Link eMe = (Link) event.getEventObject();	
			
			final double currentTransmissionPower_logu = this.mac_transmissionPower_logu_e.get(eMe.getIndex ());
			double gradientThisLink = computeGradient (eMe) + 2*mac_gradient_maxGradientAbsoluteNoise.getDouble()*(rng.nextDouble()-0.5);
			double nextTransmissionPower_logu = currentTransmissionPower_logu + this.mac_gradient_gammaStep.getDouble() * gradientThisLink;

			/* heavy ball */
			nextTransmissionPower_logu += this.mac_gradient_heavyBallBetaParameter.getDouble() * (currentTransmissionPower_logu - mac_previousTransmissionPower_logu_e.get(eMe.getIndex ()));
			/* projection */
			nextTransmissionPower_logu = GradientProjectionUtils.euclideanProjection_boxLike(nextTransmissionPower_logu, this.mac_minTransmissionPower_logu.getDouble(), this.mac_maxTransmissionPower_logu.getDouble());

			if (mac_gradient_maxGradientCoordinateChange.getDouble() > 0)
				nextTransmissionPower_logu = GradientProjectionUtils.scaleDown_maxAbsoluteCoordinateChange (currentTransmissionPower_logu , nextTransmissionPower_logu ,  mac_gradient_maxGradientCoordinateChange.getDouble());
			
			this.mac_previousTransmissionPower_logu_e.set(eMe.getIndex (), this.mac_transmissionPower_logu_e.get(eMe.getIndex ()));
			this.mac_transmissionPower_logu_e.set(eMe.getIndex (), nextTransmissionPower_logu);

			/* Send event next recomputing time */
			final double updateTime = mac_update_isSynchronous.getBoolean()? t + mac_update_averageInterUpdateTime.getDouble() : Math.max(t , t + mac_update_averageInterUpdateTime.getDouble() + mac_update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , MAC_UPDATE_WAKEUPTOUPDATE,  eMe));

			/* Update in currentNetPlan the capacity values */
			for (Link e : this.currentNetPlan.getLinks())
				e.setCapacity(Math.log(computeSINR_e (e)));

			this.traceOf_u_e.add(t, this.currentNetPlan.getVectorLinkCapacity());
			this.traceOf_y_e.add(t, this.currentNetPlan.getVectorLinkCarriedTraffic());
			this.traceOf_p_e.add(t, this.mac_transmissionPower_logu_e.copy ());
			this.traceOf_pi_e.add(t, this.cc_price_e.copy ());
			this.traceOf_h_d.add(t, this.currentNetPlan.getVectorDemandCarriedTraffic());
			this.traceOf_objFunction.add(t , NetworkPerformanceMetrics.alphaUtility(this.currentNetPlan.getVectorDemandOfferedTraffic() , cc_fairnessFactor.getDouble()));

			if (t > this.mac_simulation_maxNumberOfUpdateIntervals.getDouble() * this.mac_update_averageInterUpdateTime.getDouble()) { this.endSimulation (); }
			break;
		}

		case CC_SIGNALING_RECEIVEDMESSAGE: // A node receives from an out neighbor the q_nt for any destination
		{
			final Pair<Demand,Pair<Link,Double>> signalInfo = (Pair<Demand,Pair<Link,Double>>) event.getEventObject();
			final Demand d = signalInfo.getFirst();
			final Pair<Link,Double> receivedInfo_price_e = signalInfo.getSecond();
			this.cc_mostUpdatedLinkPriceKnownByDemand_de.set(d.getIndex() , receivedInfo_price_e.getFirst().getIndex () ,receivedInfo_price_e.getSecond());  
			break;
		}
		
		case CC_SIGNALING_WAKEUPTOSENDMESSAGE: // A node broadcasts signaling info to its 1 hop neighbors
		{
			final Link eMe = (Link) event.getEventObject();

			/* Update the new price with the gradient approach */
			final double u_e = eMe.getCapacity();
			final double y_e = eMe.getCarriedTraffic();
			final double old_pie = this.cc_price_e.get(eMe.getIndex());
			final double new_pie = Math.max(0, old_pie - this.cc_gradient_gammaStep.getDouble() * (u_e - y_e) + 2*cc_gradient_maxGradientAbsoluteNoise.getDouble()*(rng.nextDouble()-0.5));
			this.cc_price_e.set(eMe.getIndex(), new_pie);

			/* Create the info I will signal */
			Pair<Link,Double> infoToSignal = Pair.of(eMe ,  new_pie);

			/* Send the events of the signaling information messages to all the nodes */
			for (Route route : eMe.getTraversingRoutes())
			{
				if (rng.nextDouble() < this.cc_signaling_signalingLossProbability.getDouble()) continue; // the signaling may be lost => lost to all demands
				final Demand d = route.getDemand();
				final double signalingReceptionTime = t + Math.max(0 , cc_signaling_averageDelay.getDouble() + cc_signaling_maxFluctuationInDelay.getDouble() * (rng.nextDouble() - 0.5));
				this.scheduleEvent(new SimEvent (signalingReceptionTime , SimEvent.DestinationModule.EVENT_PROCESSOR , CC_SIGNALING_RECEIVEDMESSAGE , Pair.of(d , infoToSignal)));
			}

			/* Re-schedule when to wake up again */
			final double signalingTime = cc_signaling_isSynchronous.getBoolean()? t + cc_signaling_averageInterMessageTime.getDouble() : Math.max(t , t + cc_signaling_averageInterMessageTime.getDouble() + cc_signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , CC_SIGNALING_WAKEUPTOSENDMESSAGE , eMe));
			break;
		}

		case CC_UPDATE_WAKEUPTOUPDATE: // a node updates its p_n, p_e values, using most updated info available
		{
			final Demand dMe = (Demand) event.getEventObject();
			
			/* compute the new h_d and apply it */
			final double new_hd = computeHdFromPrices (dMe);
			dMe.setOfferedTraffic(new_hd);
			dMe.getRoutes ().iterator().next().setCarriedTraffic(new_hd , new_hd);

			final double updateTime = cc_update_isSynchronous.getBoolean()? t + cc_update_averageInterUpdateTime.getDouble() : Math.max(t , t + cc_update_averageInterUpdateTime.getDouble() + cc_update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , CC_UPDATE_WAKEUPTOUPDATE,  dMe));

			this.traceOf_objFunction.add(t, NetworkPerformanceMetrics.alphaUtility(this.currentNetPlan.getVectorDemandOfferedTraffic() , cc_fairnessFactor.getDouble()));

			if (t > this.cc_simulation_maxNumberOfUpdateIntervals.getDouble() * this.cc_update_averageInterUpdateTime.getDouble()) { this.endSimulation (); }
			break;
		}
		
		
		default: throw new RuntimeException ("Unexpected received event");
		}
		
		
	}

	public String finish (StringBuilder st , double simTime)
	{
		if (simulation_outFileNameRoot.getString().equals("")) return null;
		this.traceOf_objFunction.printToFile(new File (simulation_outFileNameRoot.getString() + "_objFunc.txt"));
		this.traceOf_u_e.printToFile(new File (simulation_outFileNameRoot.getString() + "_ue.txt"));
		this.traceOf_y_e.printToFile(new File (simulation_outFileNameRoot.getString() + "_ye.txt"));
		this.traceOf_p_e.printToFile(new File (simulation_outFileNameRoot.getString() + "_pe.txt"));
		this.traceOf_pi_e.printToFile(new File (simulation_outFileNameRoot.getString() + "_pie.txt"));
		this.traceOf_h_d.printToFile(new File (simulation_outFileNameRoot.getString() + "_hd.txt"));

		Map<String,String> param = new HashMap<String,String> (algorithmParameters);
		param.put("solverName", "ipopt");
		param.put("solverLibraryName", "");
		param.put("maxSolverTimeInSeconds", "-1");
		new Offline_cba_wirelessCongControlTransmissionPowerAssignment ().executeAlgorithm(copyInitialNetPlan , param , this.net2planParameters);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_objFunc.txt") , NetworkPerformanceMetrics.alphaUtility(copyInitialNetPlan.getVectorDemandOfferedTraffic() , cc_fairnessFactor.getDouble()));
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_ue.txt") , copyInitialNetPlan.getVectorLinkCapacity());
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_ye.txt") , copyInitialNetPlan.getVectorLinkCarriedTraffic());
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_hd.txt") , copyInitialNetPlan.getVectorDemandCarriedTraffic());
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_pe.txt") , copyInitialNetPlan.getVectorAttributeValues(copyInitialNetPlan.getLinks () , "p_e"));
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_pie.txt") , copyInitialNetPlan.getVectorAttributeValues(copyInitialNetPlan.getLinks () , "pi_e"));
		return null;
	}
	

	private double computeGradient (Link thisLink)
	{
		final DoubleMatrix1D infoIKnow_me = mac_mostUpdatedMe2ValueKnownByLink1_e1e2.viewRow (thisLink.getIndex ());
		
		double gradient = cc_price_e.get(thisLink.getIndex ()); 
		double accumFactor = 0;
		for (Link epp : this.currentNetPlan.getLinks())
			if (epp != thisLink)
				accumFactor += this.mac_g_nu_ee.get(thisLink.getIndex (),epp.getIndex ()) * infoIKnow_me.get(epp.getIndex ());
		accumFactor *= Math.exp(this.mac_transmissionPower_logu_e.get(thisLink.getIndex ()));
		gradient -= accumFactor;
		return gradient;
	}

	private double computeMeFactor_e (Link e)
	{
		final double snr_e = Math.exp(e.getCapacity());
		final double pi_e = this.cc_price_e.get(e.getIndex ());
		final double g_ee = this.mac_g_nu_ee.get(e.getIndex (),e.getIndex ());
		return  pi_e * snr_e / (Math.exp(this.mac_transmissionPower_logu_e.get(e.getIndex ())) * g_ee);
	}
	
	private double computeSINR_e (Link e)
	{
		final double receivedPower_nu = Math.exp(this.mac_transmissionPower_logu_e.get(e.getIndex())) * this.mac_g_nu_ee.get(e.getIndex (),e.getIndex ());
		double interferencePower_nu = this.mac_receptionThermalNoise_nu;
		for (Link eInt : this.currentNetPlan.getLinks ())
			if (eInt != e) interferencePower_nu += Math.exp(this.mac_transmissionPower_logu_e.get(eInt.getIndex())) * this.mac_g_nu_ee.get(eInt.getIndex (),e.getIndex ());
		return receivedPower_nu / interferencePower_nu;
	}

	private double computeHdFromPrices (Demand d)
	{
		DoubleMatrix1D infoIKnow_price_e = this.cc_mostUpdatedLinkPriceKnownByDemand_de.viewRow(d.getIndex());

		/* compute the demand price as weighted sum in the routes of route prices  */
		double demandWeightedSumLinkPrices = 0;
		double demandCarriedTraffic = 0; 
		for (Route r : d.getRoutes ())
		{
			final double h_r = r.getCarriedTraffic();
			demandCarriedTraffic += h_r;
			for (Link e : r.getSeqLinks())
				demandWeightedSumLinkPrices += h_r * infoIKnow_price_e.get(e.getIndex());
		}
		//if (Math.abs(demandCarriedTraffic - this.currentNetPlan.getDemandCarriedTraffic(dIdMe)) > 1E-3) throw new RuntimeException ("Not all the traffic is carried");
		demandWeightedSumLinkPrices /= demandCarriedTraffic;

		/* compute the new h_d */
		final double new_hd = Math.max(this.cc_minHd.getDouble() , Math.min(this.cc_maxHd.getDouble(), Math.pow(demandWeightedSumLinkPrices, -1/this.cc_fairnessFactor.getDouble())));

		return new_hd;
	}

}
