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
import com.net2plan.examples.ocnbook.offline.Offline_cba_congControLinkBwSplitTwolQoS;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 
 * This module implements a distributed primal-decomposition-based gradient algorithm, for a coordinated adjustment of the congestion control of two types of demands (with different utility functions), and the fraction of each link capacity to grant to the traffic of each type, to maximize the network utility enforcing a fair allocation of the resources.
 *
 * Ths event processor is adapted to permit observing the algorithm performances under user-defined conditions, 
 * including asynchronous distributed executions, where signaling can be affected by losses and/or delays, and/or measurement errors. 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec11_3_congControlAndQoSLinkCap_primalDecomp.m">{@code fig_sec11_3_congControlAndQoSLinkCap_primalDecomp.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * 
 * To simulate a network with this module, use the {@code Online_evGen_doNothing} generator.
 * 
 * @net2plan.keywords Bandwidth assignment (BA), Capacity assignment (CA), Distributed algorithm, Primal decomposition 
 * @net2plan.ocnbooksections Section 11.3
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
@SuppressWarnings("unchecked")
public class Online_evProc_congControlAndQoSTwoClassesPrimalDecomp extends IEventProcessor
{
	private double PRECISIONFACTOR;
	private Random rng;

	private int N,E,D1,D2,D;
	private Map<String,String> algorithmParameters , net2PlanParameters;
	
	private static final int MAC_UPDATE_WAKEUPTOUPDATE = 202;
	private static final int CC_SIGNALING_WAKEUPTOSENDMESSAGE = 400;
	private static final int CC_SIGNALING_RECEIVEDMESSAGE = 401;
	private static final int CC_UPDATE_WAKEUPTOUPDATE = 402;

	private InputParameter mac_update_isSynchronous = new InputParameter ("mac_update_isSynchronous", false , "true if all the distributed agents involved wake up synchronousely to update its state");
	private InputParameter mac_update_averageInterUpdateTime = new InputParameter ("mac_update_averageInterUpdateTime", 50.0 , "Average time between two updates of an agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter mac_update_maxFluctuationInterUpdateTime = new InputParameter ("mac_update_maxFluctuationInterUpdateTime", 10.0 , "Max fluctuation in time in the update interval of an agent, in absolute time values. The update intervals are sampled from a uniform distribution within the given interval" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter mac_gradient_gammaStep = new InputParameter ("mac_gradient_gammaStep", 5.0 , "Gamma step in the gradient algorithm" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter mac_gradient_maxGradientCoordinateChange = new InputParameter ("mac_gradient_maxGradientCoordinateChange", 1000.0 , "Maximum change in an iteration of a gradient coordinate" , 0 , false , Double.MAX_VALUE , true);

	private InputParameter mac_simulation_maxNumberOfUpdateIntervals = new InputParameter ("mac_simulation_maxNumberOfUpdateIntervals", 600.0 , "Maximum number of update intervals in average per agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter simulation_randomSeed = new InputParameter ("simulation_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter simulation_outFileNameRoot = new InputParameter ("simulation_outFileNameRoot", "crossLayerCongControlTwoQoSPrimalDecomp" , "Root of the file name to be used in the output files. If blank, no output");
	
	private InputParameter cc_signaling_isSynchronous = new InputParameter ("cc_signaling_isSynchronous", false , "true if all the distributed agents involved wake up synchronously to send the signaling messages");
	private InputParameter cc_signaling_averageInterMessageTime = new InputParameter ("cc_signaling_averageInterMessageTime", 1.0 , "Average time between two signaling messages sent by an agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter cc_signaling_maxFluctuationInterMessageTime = new InputParameter ("cc_signaling_maxFluctuationInterMessageTime", 0.5 , "Max fluctuation in time between two signaling messages sent by an agent" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_signaling_averageDelay = new InputParameter ("cc_signaling_averageDelay", 3.0 , "Average time between signaling message transmission by an agent and its reception by other or others" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_signaling_maxFluctuationInDelay = new InputParameter ("cc_signaling_maxFluctuationInDelay", 0.5 , "Max fluctuation in time in the signaling delay, in absolute time values. The signaling delays are sampled from a uniform distribution within the given interval" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_signaling_signalingLossProbability = new InputParameter ("cc_signaling_signalingLossProbability", 0.05 , "Probability that a signaling message transmitted is lost (not received by other or others involved agents)" , 0 , true , Double.MAX_VALUE , true);

	private InputParameter cc_update_isSynchronous = new InputParameter ("cc_update_isSynchronous", false , "true if all the distributed agents involved wake up synchronousely to update its state");
	private InputParameter cc_update_averageInterUpdateTime = new InputParameter ("cc_update_averageInterUpdateTime", 1.0 , "Average time between two updates of an agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter cc_update_maxFluctuationInterUpdateTime = new InputParameter ("cc_update_maxFluctuationInterUpdateTime", 0.5 , "Max fluctuation in time in the update interval of an agent, in absolute time values. The update intervals are sampled from a uniform distribution within the given interval" , 0 , true , Double.MAX_VALUE , true);

	private InputParameter cc_gradient_gammaStep = new InputParameter ("cc_gradient_gammaStep", 0.001 , "Gamma step in the gradient algorithm" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter cc_gradient_maxGradientAbsoluteNoise = new InputParameter ("cc_gradient_maxGradientAbsoluteNoise", 0.0 , "Max value of the added noise to the gradient coordinate in absolute values" , 0 , true , Double.MAX_VALUE , true);

	private InputParameter cc_simulation_maxNumberOfUpdateIntervals = new InputParameter ("cc_simulation_maxNumberOfUpdateIntervals", 600.0 , "Maximum number of update intervals in average per agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter cc_control_minHd = new InputParameter ("cc_control_minHd", 0.1 , "Minimum traffic assigned to each demand" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_control_maxHd = new InputParameter ("cc_control_maxHd", 1e6 , "Maximum traffic assigned to each demand" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_control_initialiLinkPrices = new InputParameter ("cc_control_initialiLinkPrices", 1.0 , "Link prices in the algorithm initialization" , 0 , true , Double.MAX_VALUE , true);

	private InputParameter cc_control_fairnessFactor_1 = new InputParameter ("cc_control_fairnessFactor_1", 1.0 , "Fairness factor in utility function of congestion control for demands of class 1" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_control_fairnessFactor_2 = new InputParameter ("cc_control_fairnessFactor_2", 1.0 , "Fairness factor in utility function of congestion control for demands of class 2" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_control_weightFairness_1 = new InputParameter ("cc_control_weightFairness_1", 1.0 , "Weight factor in utility function demands type 1" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter cc_control_weightFairness_2 = new InputParameter ("cc_control_weightFairness_2", 2.0 , "Weight factor in utility function demands type 2" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter mac_minCapacity_1 = new InputParameter ("mac_minCapacity_1", 0.1 , "Minimum capacity in each link, allocated to traffic of type 1" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter mac_minCapacity_2 = new InputParameter ("mac_minCapacity_2", 0.1 , "Minimum capacity in each link, allocated to traffic of type 2" , 0 , true , Double.MAX_VALUE , true);

	private DoubleMatrix1D demandType;
	private DoubleMatrix1D congControl_price1_e;
	private DoubleMatrix1D congControl_price2_e;
	private DoubleMatrix2D control_mostUpdatedLinkPriceKnownDemand_de;
	private DoubleMatrix1D mac_u1; 
	private DoubleMatrix1D mac_u2; 
	
	private TimeTrace traceOf_pi1_e;
	private TimeTrace traceOf_pi2_e;
	private TimeTrace traceOf_u1_e;
	private TimeTrace traceOf_u2_e;
	private TimeTrace traceOf_y1_e;
	private TimeTrace traceOf_y2_e;
	private TimeTrace traceOf_h_d1;
	private TimeTrace traceOf_h_d2;
	private TimeTrace traceOf_objFunction;

	private NetPlan currentNetPlan , copyInitialNetPlan;
	
	
	@Override
	public String getDescription()
	{
		return "This module implements a distributed primal-decomposition-based gradient algorithm, for a coordinated adjustment of the congestion control of two types of demands (with different utility functions), and the fraction of each link capacity to grant to the traffic of each type, to maximize the network utility enforcing a fair allocation of the resources.";
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
		this.algorithmParameters = algorithmParameters;
		this.net2PlanParameters = net2planParameters;
		if (currentNetPlan.getNumberOfLayers() != 1) throw new Net2PlanException ("This algorithm works in single layer networks");

		/* INITIALIZE VARIABLES COMMON FOR BOTH LAYERS */
		this.copyInitialNetPlan = currentNp.copy();
		if (currentNetPlan.getVectorLinkCapacity().getMaxLocation() [0]< mac_minCapacity_1.getDouble() + mac_minCapacity_2.getDouble()) throw new Net2PlanException ("Minimum link capacities for each class are too high for the available link capacities");
		
		this.PRECISIONFACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));
		
		/* READ INPUT PARAMETERS BOTH LAYERS */
		this.rng = new Random (simulation_randomSeed.getLong());
		this.N = this.currentNetPlan.getNumberOfNodes();
		this.E = this.currentNetPlan.getNumberOfLinks();
		if (E == 0) throw new Net2PlanException ("The input design should have links");
		this.D1 = N*(N-1);
		this.D2 = N*(N-1);
		this.D = D1+D2;
		
		/* Initialize the demands and routers per demand */
		this.currentNetPlan.removeAllDemands();
		this.currentNetPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
		this.demandType = DoubleFactory1D.dense.make (D1+D2);
		for (Node n1 : this.currentNetPlan.getNodes())
			for (Node n2 : this.currentNetPlan.getNodes())
				if (n1 != n2) 
				{ 
					final Demand d1 = this.currentNetPlan.addDemand(n1, n2, 0.0, null); d1.setAttribute("type" , "1"); demandType.set(d1.getIndex (), 1); 
					final Demand d2 = this.currentNetPlan.addDemand(n1, n2, 0.0, null); d1.setAttribute("type" , "2"); demandType.set(d2.getIndex (), 2); 
				}
		
		/* Remove all routes, and create one with the shortest path in km for each demand */
		currentNetPlan.removeAllUnicastRoutingInformation();
		currentNetPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
		this.currentNetPlan.addRoutesFromCandidatePathList(currentNetPlan.computeUnicastCandidatePathList(currentNetPlan.getVectorLinkLengthInKm() , 1, -1, -1, -1, -1, -1, -1 , null));

		
		for (Route r : currentNp.getRoutes ()) r.setCarriedTraffic(cc_control_minHd.getDouble() , cc_control_minHd.getDouble());
		
		/* INITIALIZE control information  */
		this.congControl_price1_e = DoubleFactory1D.dense.make (E , cc_control_initialiLinkPrices.getDouble());
		this.congControl_price2_e = DoubleFactory1D.dense.make (E , cc_control_initialiLinkPrices.getDouble());
		
		/* Initialize the information each demand knows of the prices of all the links */
		this.control_mostUpdatedLinkPriceKnownDemand_de = DoubleFactory2D.dense.make (D,E,cc_control_initialiLinkPrices.getDouble());

		this.mac_u1 = DoubleFactory1D.dense.make (E , mac_minCapacity_1.getDouble());
		this.mac_u2 = currentNetPlan.getVectorLinkCapacity().copy ().assign(DoubleFunctions.minus (mac_minCapacity_1.getDouble()));
		
		/* Each demand adjusts its rate to the pi_e */
		/* Compute the traffic each demand injects, set it in net2plan, and tell routing layer there was an update in this */
		for (Demand d : currentNetPlan.getDemands())
		{
			final double h_d = this.computeHdFromPrices(d);
			d.setOfferedTraffic(h_d);
			d.getRoutes().iterator().next ().setCarriedTraffic(h_d , h_d);
		}
		
		/* Initially all nodes receive a "wake up to transmit" event, aligned at time zero or y asynchr => randomly chosen */
		for (Link e : currentNp.getLinks())
		{
			final double updateTime = (mac_update_isSynchronous.getBoolean())? mac_update_averageInterUpdateTime.getDouble() : Math.max(0 , mac_update_averageInterUpdateTime.getDouble() + mac_update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , MAC_UPDATE_WAKEUPTOUPDATE , e));
		}
		
		/* INITIALIZATION CONGESTION CONTROL LAYER */
		/* Initially all nodes receive a "wake up to transmit" event, aligned at time zero or y asynchr => randomly chosen */
		for (Link e : currentNp.getLinks())
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
		this.traceOf_pi1_e = new TimeTrace  ();
		this.traceOf_pi2_e = new TimeTrace  ();
		this.traceOf_u1_e = new TimeTrace ();
		this.traceOf_u2_e = new TimeTrace ();
		this.traceOf_y1_e = new TimeTrace (); 
		this.traceOf_y2_e = new TimeTrace (); 
		this.traceOf_h_d1 = new TimeTrace (); 
		this.traceOf_h_d2 = new TimeTrace (); 
		this.traceOf_objFunction = new TimeTrace ();

		
		this.traceOf_pi1_e.add(0.0, this.congControl_price1_e.copy ());
		this.traceOf_pi2_e.add(0.0, this.congControl_price2_e.copy ());
		this.traceOf_u1_e.add(0.0, this.mac_u1.copy ());
		this.traceOf_u2_e.add(0.0, this.mac_u2.copy ());
		final Triple<Double,Pair<DoubleMatrix1D,DoubleMatrix1D>,Pair<DoubleMatrix1D,DoubleMatrix1D>> objFunctionInfo = computeObjectiveFunctionFromNetPlan(this.currentNetPlan);
		this.traceOf_objFunction.add(0.0 , objFunctionInfo.getFirst());
		this.traceOf_h_d1.add(0.0, objFunctionInfo.getSecond().getFirst().copy ());
		this.traceOf_h_d2.add(0.0, objFunctionInfo.getSecond().getSecond().copy ());
		this.traceOf_y1_e.add(0.0, objFunctionInfo.getThird().getFirst().copy ());
		this.traceOf_y2_e.add(0.0, objFunctionInfo.getThird().getSecond().copy ());
	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		final double t = event.getEventTime();
		switch (event.getEventType())
		{
		case MAC_UPDATE_WAKEUPTOUPDATE: 
		{
			final Link eMe = (Link) event.getEventObject();
			final double current_u1 = mac_u1.get(eMe.getIndex ());
			final double current_u2 = mac_u2.get(eMe.getIndex ());
			final double gradient_1 = congControl_price1_e.get(eMe.getIndex ());
			final double gradient_2 = congControl_price2_e.get(eMe.getIndex ());
			final double nextBeforeProjection_u1 = current_u1 + this.mac_gradient_gammaStep.getDouble() * gradient_1;
			final double nextBeforeProjection_u2 = current_u2 + this.mac_gradient_gammaStep.getDouble() * gradient_2;
			DoubleMatrix1D initialSolution = DoubleFactory1D.dense.make (new double [] { current_u1 , current_u2 });
			DoubleMatrix1D inOut = DoubleFactory1D.dense.make (new double [] { nextBeforeProjection_u1 , nextBeforeProjection_u2 });
			DoubleMatrix1D uMin = DoubleFactory1D.dense.make (new double [] { mac_minCapacity_1.getDouble() , mac_minCapacity_2.getDouble() });
			GradientProjectionUtils.euclideanProjection_sumInequality (inOut , null , uMin , eMe.getCapacity());
			if (mac_gradient_maxGradientCoordinateChange.getDouble() > 0)
				GradientProjectionUtils.scaleDown_maxAbsoluteCoordinateChange (initialSolution , inOut , null , mac_gradient_maxGradientCoordinateChange.getDouble());

			this.mac_u1.set(eMe.getIndex (), inOut.get(0));
			this.mac_u2.set(eMe.getIndex (), inOut.get(1));
			if (mac_u1.get(eMe.getIndex ()) + mac_u2.get(eMe.getIndex ()) > eMe.getCapacity() + PRECISIONFACTOR) throw new RuntimeException ("Bad");
			
			final double updateTime = mac_update_isSynchronous.getBoolean()? t + mac_update_averageInterUpdateTime.getDouble() : Math.max(t , t + mac_update_averageInterUpdateTime.getDouble() + mac_update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , MAC_UPDATE_WAKEUPTOUPDATE,  eMe));

			this.traceOf_u1_e.add(t, this.mac_u1.copy ());
			this.traceOf_u2_e.add(t, this.mac_u2.copy ());

			if (t > this.mac_simulation_maxNumberOfUpdateIntervals.getDouble() * this.mac_update_averageInterUpdateTime.getDouble()) { this.endSimulation (); }

			break;
		}

		case CC_SIGNALING_RECEIVEDMESSAGE: // A node receives from an out neighbor the q_nt for any destination
		{
			final Pair<Demand,Triple<Link,Double,Double>> signalInfo = (Pair<Demand,Triple<Link,Double,Double>>) event.getEventObject();
			final Demand d = signalInfo.getFirst();
			final int type = (int) demandType.get(d.getIndex ());
			final Link e = signalInfo.getSecond().getFirst();
			final double pi1 = signalInfo.getSecond().getSecond();
			final double pi2 = signalInfo.getSecond().getThird();
			this.control_mostUpdatedLinkPriceKnownDemand_de.set(d.getIndex () , e.getIndex () , (type == 1)? pi1 : pi2);  
			break;
		}
		
		case CC_SIGNALING_WAKEUPTOSENDMESSAGE: // A node broadcasts signaling info to its 1 hop neighbors
		{
			final Link eMe = (Link) event.getEventObject();

			/* Update the new price with the gradient approach */
			Triple<Double,Pair<DoubleMatrix1D,DoubleMatrix1D>,Pair<DoubleMatrix1D,DoubleMatrix1D>>  triple = computeObjectiveFunctionFromNetPlan (this.currentNetPlan);
			final double u1 = this.mac_u1.get(eMe.getIndex ());
			final double u2 = this.mac_u2.get(eMe.getIndex ());
			final double y1 = triple.getThird().getFirst().get(eMe.getIndex ());
			final double y2 = triple.getThird().getSecond().get(eMe.getIndex ());
			final double old_pie_1 = this.congControl_price1_e.get(eMe.getIndex ());
			final double old_pie_2 = this.congControl_price2_e.get(eMe.getIndex ());
			final double new_pie_1 = Math.max(0, old_pie_1 - this.cc_gradient_gammaStep.getDouble() * (u1 - y1) + 2*cc_gradient_maxGradientAbsoluteNoise.getDouble()*(rng.nextDouble()-0.5));
			final double new_pie_2 = Math.max(0, old_pie_2 - this.cc_gradient_gammaStep.getDouble() * (u2 - y2) + 2*cc_gradient_maxGradientAbsoluteNoise.getDouble()*(rng.nextDouble()-0.5));
			this.congControl_price1_e.set(eMe.getIndex (), new_pie_1);
			this.congControl_price2_e.set(eMe.getIndex (), new_pie_2);
			
			this.traceOf_pi1_e.add(t, this.congControl_price1_e.copy ());
			this.traceOf_pi2_e.add(t, this.congControl_price2_e.copy ());

			/* Create the info I will signal */
			Triple<Link,Double,Double> infoToSignal = Triple.of(eMe ,  new_pie_1 , new_pie_2);

			/* Send the events of the signaling information messages to all the nodes */
			for (Route route : eMe.getTraversingRoutes())
			{
				if (rng.nextDouble() < this.cc_signaling_signalingLossProbability.getDouble()) continue; // the signaling may be lost => lost to all demands
				final Demand d = route.getDemand ();
				final double signalingReceptionTime = t + Math.max(0 , cc_signaling_averageDelay.getDouble() + cc_signaling_maxFluctuationInDelay.getDouble() * (rng.nextDouble() - 0.5));
				this.scheduleEvent(new SimEvent (signalingReceptionTime , SimEvent.DestinationModule.EVENT_PROCESSOR , CC_SIGNALING_RECEIVEDMESSAGE , Pair.of(d , infoToSignal)));
			}
			
			/* Re-schedule when to wake up again */
			final double signalingTime = cc_signaling_isSynchronous.getBoolean()? t + cc_signaling_averageInterMessageTime.getDouble() : Math.max(t , t + cc_signaling_averageInterMessageTime.getDouble() + cc_signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , CC_SIGNALING_WAKEUPTOSENDMESSAGE , eMe));
			break;
		}

		case CC_UPDATE_WAKEUPTOUPDATE: 
		{
			final Demand dMe = (Demand) event.getEventObject();
			final double new_hd = computeHdFromPrices (dMe);
			
			dMe.setOfferedTraffic(new_hd);
			dMe.getRoutes ().iterator().next().setCarriedTraffic(new_hd , new_hd);
			
			final Triple<Double,Pair<DoubleMatrix1D,DoubleMatrix1D>,Pair<DoubleMatrix1D,DoubleMatrix1D>> objFunctionInfo = computeObjectiveFunctionFromNetPlan(this.currentNetPlan);
			this.traceOf_objFunction.add(t , objFunctionInfo.getFirst());
			this.traceOf_h_d1.add(t, objFunctionInfo.getSecond().getFirst().copy ());
			this.traceOf_h_d2.add(t, objFunctionInfo.getSecond().getSecond().copy ());
			this.traceOf_y1_e.add(t, objFunctionInfo.getThird().getFirst().copy ());
			this.traceOf_y2_e.add(t, objFunctionInfo.getThird().getSecond().copy ());

			final double updateTime = cc_update_isSynchronous.getBoolean()? t + cc_update_averageInterUpdateTime.getDouble() : Math.max(t , t + cc_update_averageInterUpdateTime.getDouble() + cc_update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , CC_UPDATE_WAKEUPTOUPDATE,  dMe));

			if (t > this.cc_simulation_maxNumberOfUpdateIntervals.getDouble() * this.cc_update_averageInterUpdateTime.getDouble()) { this.endSimulation (); }

			break;
		}
		
		
		default: throw new RuntimeException ("Unexpected received event");
		}
		
		
	}

	public String finish (StringBuilder st , double simTime)
	{
		if (simulation_outFileNameRoot.getString().equals("")) return null;
		traceOf_h_d1.printToFile(new File (simulation_outFileNameRoot.getString() + "_hd1.txt"));
		traceOf_h_d2.printToFile(new File (simulation_outFileNameRoot.getString() + "_hd2.txt"));
		traceOf_u1_e.printToFile(new File (simulation_outFileNameRoot.getString() + "_ue1.txt"));
		traceOf_u2_e.printToFile(new File (simulation_outFileNameRoot.getString() + "_ue2.txt"));
		traceOf_objFunction.printToFile(new File (simulation_outFileNameRoot.getString() + "_objFunc.txt"));

		Map<String,String> param = new HashMap<String,String> (algorithmParameters);
		param.put("solverName", "ipopt");
		param.put("solverLibraryName", "");
		param.put("maxSolverTimeInSeconds", "-1");
		new Offline_cba_congControLinkBwSplitTwolQoS().executeAlgorithm(copyInitialNetPlan , param , this.net2PlanParameters);
		DoubleMatrix1D jom_hd1 = DoubleFactory1D.dense.make (D1);
		DoubleMatrix1D jom_hd2 = DoubleFactory1D.dense.make (D2);
		DoubleMatrix1D jom_ue1 = DoubleFactory1D.dense.make (E);
		DoubleMatrix1D jom_ue2 = DoubleFactory1D.dense.make (E);
		int counter_1 = 0; int counter_2 = 0;
		for (Demand d : copyInitialNetPlan.getDemands ())
			if (d.getAttribute("type").equals ("1")) jom_hd1.set (counter_1 ++ , d.getOfferedTraffic()); else jom_hd2.set (counter_2 ++ , d.getOfferedTraffic());
		for (Link e : copyInitialNetPlan.getLinks ()) { jom_ue1.set (e.getIndex () , Double.parseDouble (e.getAttribute("u_1"))); jom_ue2.set (e.getIndex () , Double.parseDouble (e.getAttribute("u_2"))); }
		final double jomObjcFunc = Double.parseDouble (copyInitialNetPlan.getAttribute("netUtility"));
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_hd1.txt"), jom_hd1);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_hd2.txt"), jom_hd2);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_ue1.txt"), jom_ue1);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_ue2.txt"), jom_ue2);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_objFunc.txt"), jomObjcFunc);
		return null;
	}
	

	private Triple<Double,Pair<DoubleMatrix1D,DoubleMatrix1D>,Pair<DoubleMatrix1D,DoubleMatrix1D>>  computeObjectiveFunctionFromNetPlan (NetPlan np)
	{
		DoubleMatrix1D y1 = DoubleFactory1D.dense.make (E);
		DoubleMatrix1D y2 = DoubleFactory1D.dense.make (E);
		DoubleMatrix1D h1 = DoubleFactory1D.dense.make (D1);
		DoubleMatrix1D h2 = DoubleFactory1D.dense.make (D2);
		double obj = 0;
		int counter_d1 = 0; int counter_d2 = 0;
		for (Demand d : np.getDemands())
		{
			final double h_d = d.getCarriedTraffic();
			final int type = (int) this.demandType.get(d.getIndex());
			final List<Link> seqLinks = d.getRoutes ().iterator().next().getSeqLinks();
			for (Link e : seqLinks)
			{
				final int index_e = e.getIndex ();
				if (type == 1) y1.set(index_e , y1.get(index_e) + h_d); else y2.set(index_e , y2.get(index_e) + h_d); 
			}
			if (type == 1) 
			{ 
				h1.set (counter_d1 , h_d);
				obj += (this.cc_control_fairnessFactor_1.getDouble() == 1)? this.cc_control_weightFairness_1.getDouble() * Math.log(h_d) : this.cc_control_weightFairness_1.getDouble() *  Math.pow(h_d, 1-this.cc_control_fairnessFactor_1.getDouble()) / (1-this.cc_control_fairnessFactor_1.getDouble());
				counter_d1 ++; 
			} else 
			{ 
				h2.set (counter_d2 , h_d);
				obj += (this.cc_control_fairnessFactor_2.getDouble() == 1)? this.cc_control_weightFairness_2.getDouble() * Math.log(h_d) : this.cc_control_weightFairness_2.getDouble() *  Math.pow(h_d, 1-this.cc_control_fairnessFactor_2.getDouble()) / (1-this.cc_control_fairnessFactor_2.getDouble());
				counter_d2 ++; 
			} 
		}
		if ((counter_d1 != D1) || (counter_d2 != D2)) throw new RuntimeException ("Bad");
		return Triple.of(obj, Pair.of(h1, h2),Pair.of(y1, y2));
	}


	private double computeHdFromPrices (Demand d)
	{
		DoubleMatrix1D infoIKnow_price_e = this.control_mostUpdatedLinkPriceKnownDemand_de.viewRow(d.getIndex ());

		/* compute the demand price as weighted sum in the routes of route prices  */
		double demandWeightedSumLinkPrices = 0;
		double demandCarriedTraffic = 0; 
		for (Route r : d.getRoutes ())
		{
			final double h_r = r.getCarriedTraffic();
			demandCarriedTraffic += h_r;
			for (Link e : r.getSeqLinks())
				demandWeightedSumLinkPrices += h_r * infoIKnow_price_e.get(e.getIndex ());
		}
		//if (Math.abs(demandCarriedTraffic - this.currentNetPlan.getDemandCarriedTraffic(dIdMe)) > 1E-3) throw new RuntimeException ("Not all the traffic is carried");
		demandWeightedSumLinkPrices /= demandCarriedTraffic;

		/* compute the new h_d */
		final double alpha = (demandType.get(d.getIndex ()) == 1)? this.cc_control_fairnessFactor_1.getDouble() : this.cc_control_fairnessFactor_2.getDouble();
		final double weight = (demandType.get(d.getIndex ()) == 1)? this.cc_control_weightFairness_1.getDouble() : this.cc_control_weightFairness_2.getDouble();
		final double new_hd = Math.max(this.cc_control_minHd.getDouble() , Math.min(this.cc_control_maxHd.getDouble(), Math.pow(demandWeightedSumLinkPrices/weight, -1/alpha)));

		return new_hd;
	}

}
