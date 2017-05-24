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
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.NetworkPerformanceMetrics;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.TimeTrace;
import com.net2plan.utils.Triple;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** 
 * This module implements a distributed dual-gradient based algorithm, for adapting the demand injected traffic (congestion control) in the network, to maximize the network utility enforcing a fair allocation of the resources.
 *
 * Ths event processor is adapted to permit observing the algorithm performances under user-defined conditions, 
 * including asynchronous distributed executions, where signaling can be affected by losses and/or delays, and/or measurement errors. 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec10_4_congestionControlDual.m">{@code fig_sec10_4_congestionControlDual.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * 
 * To simulate a network with this module, use the {@code Online_evGen_doNothing} generator.
 * 
 * @net2plan.keywords Bandwidth assignment (BA), Distributed algorithm, Dual gradient algorithm
 * @net2plan.ocnbooksections Section 10.4
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
@SuppressWarnings("unchecked")
public class Online_evProc_congestionControlDual extends IEventProcessor
{
	private Random rng;

	private InputParameter signaling_isSynchronous = new InputParameter ("signaling_isSynchronous", false , "true if all the distributed agents involved wake up synchronously to send the signaling messages");
	private InputParameter signaling_averageInterMessageTime = new InputParameter ("signaling_averageInterMessageTime", 1.0 , "Average time between two signaling messages sent by an agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter signaling_maxFluctuationInterMessageTime = new InputParameter ("signaling_maxFluctuationInterMessageTime", 0.5 , "Max fluctuation in time between two signaling messages sent by an agent" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter signaling_averageDelay = new InputParameter ("signaling_averageDelay", 0.0 , "Average time between signaling message transmission by an agent and its reception by other or others" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter signaling_maxFluctuationInDelay = new InputParameter ("signaling_maxFluctuationInDelay", 0.0 , "Max fluctuation in time in the signaling delay, in absolute time values. The signaling delays are sampled from a uniform distribution within the given interval" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter signaling_signalingLossProbability = new InputParameter ("signaling_signalingLossProbability", 0.05 , "Probability that a signaling message transmitted is lost (not received by other or others involved agents)" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter update_isSynchronous = new InputParameter ("update_isSynchronous", false , "true if all the distributed agents involved wake up synchronousely to update its state");
	private InputParameter update_averageInterUpdateTime = new InputParameter ("update_averageInterUpdateTime", 1.0 , "Average time between two updates of an agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter update_maxFluctuationInterUpdateTime = new InputParameter ("update_maxFluctuationInterUpdateTime", 0.5 , "Max fluctuation in time in the update interval of an agent, in absolute time values. The update intervals are sampled from a uniform distribution within the given interval" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter gradient_gammaStep = new InputParameter ("gradient_gammaStep", 0.0001 , "Gamma step in the gradient algorithm" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter gradient_maxGradientAbsoluteNoise = new InputParameter ("gradient_maxGradientAbsoluteNoise", 0.0 , "Max value of the added noise to the gradient coordinate in absolute values" , 0 , true , Double.MAX_VALUE , true);

	private InputParameter simulation_maxNumberOfUpdateIntervals = new InputParameter ("simulation_maxNumberOfUpdateIntervals", 700.0 , "Maximum number of update intervals in average per agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter simulation_randomSeed = new InputParameter ("simulation_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter simulation_outFileNameRoot = new InputParameter ("simulation_outFileNameRoot", "congestionControlDual" , "Root of the file name to be used in the output files. If blank, no output");

	private InputParameter control_minHd = new InputParameter ("control_minHd", 0.1 , "Minimum traffic assigned to each demand" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter control_maxHd = new InputParameter ("control_maxHd", 1.0E6 , "Maximum traffic assigned to each demand" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter control_fairnessFactor = new InputParameter ("control_fairnessFactor", 2.0 , "Fairness factor in utility function of congestion control" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter control_initialLinkPrices = new InputParameter ("control_initialLinkPrices", 1 , "Initial value of the link weights" , 0 , true , Double.MAX_VALUE , true);

	private static final int SIGNALING_WAKEUPTOSENDMESSAGE = 400;
	private static final int SIGNALING_RECEIVEDMESSAGE = 401;
	private static final int UPDATE_WAKEUPTOUPDATE = 402;

	private NetPlan currentNetPlan;
	private int N,E,D;
	private DoubleMatrix1D congControl_price_e;
	private DoubleMatrix2D control_mostUpdatedLinkPriceKnownByDemand_de; 
	
	private TimeTrace stat_traceOf_hd;
	private TimeTrace stat_traceOf_objFunction;
	private TimeTrace stat_traceOf_pie;
	private TimeTrace stat_traceOf_ye;

	@Override
	public String getDescription()
	{
		return "This module implements a distributed dual-gradient based algorithm, for adapting the demand injected traffic (congestion control) in the network, to maximize the network utility enforcing a fair allocation of the resources.";
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
		if (currentNetPlan.getNumberOfLayers() != 1) throw new Net2PlanException ("This algorithm works in single layer networks");

		/* Remove all routes, and create one with the shortest path in km for each demand */
		currentNetPlan.removeAllUnicastRoutingInformation();
		currentNetPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
		this.currentNetPlan.addRoutesFromCandidatePathList(currentNetPlan.computeUnicastCandidatePathList(currentNetPlan.getVectorLinkLengthInKm() , 1, -1, -1, -1, -1, -1, -1 , null));

		for (Route r : currentNetPlan.getRoutes ()) r.setCarriedTraffic(r.getDemand().getOfferedTraffic() , r.getDemand().getOfferedTraffic());
		
		this.rng = new Random (simulation_randomSeed.getLong());
		this.currentNetPlan = currentNetPlan;
		this.D = currentNetPlan.getNumberOfDemands ();
		this.E = currentNetPlan.getNumberOfLinks ();
		this.N = currentNetPlan.getNumberOfNodes ();

		/* Set the initial prices in the links: 1.0 */
		this.congControl_price_e = DoubleFactory1D.dense.make (E , control_initialLinkPrices.getDouble());
		
		/* Initialize the information each demand knows of the prices of all the links */
		this.control_mostUpdatedLinkPriceKnownByDemand_de = DoubleFactory2D.dense.make (D,E,control_initialLinkPrices.getDouble());
		
		/* Compute the traffic each demand injects, update the routes keeping the fraction */
		for (Demand d : currentNetPlan.getDemands())
		{
			final double new_hd = this.computeHdFromPrices(d);
			if (Double.isNaN(new_hd)) throw new RuntimeException ("Bad");
			final double old_hd = d.getOfferedTraffic();
			d.setOfferedTraffic(new_hd);
			final Set<Route> routes = d.getRoutes();
			final double increasingFactor = (old_hd == 0)?  Double.MAX_VALUE : new_hd/old_hd;
			for (Route r : routes)
			{
				final double old_hr = r.getCarriedTraffic();
				final double new_hr = (old_hd == 0)? new_hd / routes.size() : old_hr * increasingFactor;
				if (Double.isNaN(old_hr)) throw new RuntimeException ("Bad");
				if (Double.isNaN(new_hr)) throw new RuntimeException ("Bad");
				r.setCarriedTraffic(new_hr , new_hr);
			}
			if (Math.abs(d.getOfferedTraffic() - d.getCarriedTraffic()) > 1E-3) throw new RuntimeException ("Bad");
		}
		
		/* Initially all nodes receive a "wake up to transmit" event, aligned at time zero or y asynchr => randomly chosen */
		for (Link e : currentNetPlan.getLinks())
		{
			final double signalingTime = (signaling_isSynchronous.getBoolean())? signaling_averageInterMessageTime.getDouble() : Math.max(0 , signaling_averageInterMessageTime.getDouble() + signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_WAKEUPTOSENDMESSAGE , e));
		}
		for (Demand d : currentNetPlan.getDemands())
		{
			final double updateTime = (update_isSynchronous.getBoolean())? update_averageInterUpdateTime.getDouble() : Math.max(0 , update_averageInterUpdateTime.getDouble() + update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_WAKEUPTOUPDATE , d));
		}

		/* Intialize the traces */
		this.stat_traceOf_hd = new TimeTrace ();
		this.stat_traceOf_pie = new TimeTrace  ();
		this.stat_traceOf_ye = new TimeTrace  ();
		this.stat_traceOf_objFunction = new TimeTrace (); 

		this.stat_traceOf_hd.add(0.0, this.currentNetPlan.getVectorDemandOfferedTraffic());
		this.stat_traceOf_pie.add(0.0, this.congControl_price_e.copy ());
		this.stat_traceOf_ye.add(0.0, this.currentNetPlan.getVectorLinkCarriedTraffic());
		this.stat_traceOf_objFunction.add(0.0, NetworkPerformanceMetrics.alphaUtility(currentNetPlan.getVectorDemandOfferedTraffic() , control_fairnessFactor.getDouble()));

	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		final double t = event.getEventTime();
		switch (event.getEventType())
		{
		case SIGNALING_RECEIVEDMESSAGE: // A node receives from an out neighbor the q_nt for any destination
		{
			final Pair<Demand,Pair<Link,Double>> signalInfo = (Pair<Demand,Pair<Link,Double>>) event.getEventObject();
			final Demand d = signalInfo.getFirst();
			final Pair<Link,Double> receivedInfo_price_e = signalInfo.getSecond();
			this.control_mostUpdatedLinkPriceKnownByDemand_de.set(d.getIndex() , receivedInfo_price_e.getFirst().getIndex () ,receivedInfo_price_e.getSecond());  
			break;
		}
		
		case SIGNALING_WAKEUPTOSENDMESSAGE: 
		{
			final Link eMe = (Link) event.getEventObject();

			/* Update the new price with the gradient approach */
			final double u_e = eMe.getCapacity();
			final double y_e = eMe.getCarriedTraffic();
			final double old_pie = this.congControl_price_e.get(eMe.getIndex());
			final double new_pie = Math.max(0, old_pie - this.gradient_gammaStep.getDouble() * (u_e - y_e) + 2*gradient_maxGradientAbsoluteNoise.getDouble()*(rng.nextDouble()-0.5));
			this.congControl_price_e.set(eMe.getIndex(), new_pie);
			
			/* Create the info I will signal */
			Pair<Link,Double> infoToSignal = Pair.of(eMe ,  new_pie);

			/* Send the events of the signaling information messages to all the nodes */
			for (Route route : eMe.getTraversingRoutes())
			{
				if (rng.nextDouble() < this.signaling_signalingLossProbability.getDouble()) continue; // the signaling may be lost => lost to all demands
				final Demand d = route.getDemand();
				final double signalingReceptionTime = t + Math.max(0 , signaling_averageDelay.getDouble() + signaling_maxFluctuationInDelay.getDouble() * (rng.nextDouble() - 0.5));
				this.scheduleEvent(new SimEvent (signalingReceptionTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_RECEIVEDMESSAGE , Pair.of(d , infoToSignal)));
			}
			
			/* Re-schedule when to wake up again */
			final double signalingTime = signaling_isSynchronous.getBoolean()? t + signaling_averageInterMessageTime.getDouble() : Math.max(t , t + signaling_averageInterMessageTime.getDouble() + signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_WAKEUPTOSENDMESSAGE , eMe));
			break;
		}

		case UPDATE_WAKEUPTOUPDATE: // a node updates its p_n, p_e values, using most updated info available
		{
			final Demand dMe = (Demand) event.getEventObject();
			
			/* compute the new h_d and apply it */
			final double new_hd = computeHdFromPrices (dMe);
			final double old_hd = dMe.getCarriedTraffic();
			dMe.setOfferedTraffic(new_hd);
			final Set<Route> routes = dMe.getRoutes();
			final double increasingFactor = (old_hd == 0)?  Double.MAX_VALUE : new_hd/old_hd;
			for (Route r : routes)
			{
				final double old_hr = r.getCarriedTraffic();
				final double new_hr = (old_hd == 0)? new_hd / routes.size() : old_hr * increasingFactor;
				r.setCarriedTraffic(new_hr , new_hr);
			}
//			if (Math.abs(currentNetPlan.getDemandOfferedTraffic(dIdMe) - currentNetPlan.getDemandCarriedTraffic(dIdMe)) > 1E-3) throw new RuntimeException ("Bad");

			final double updateTime = update_isSynchronous.getBoolean()? t + update_averageInterUpdateTime.getDouble() : Math.max(t , t + update_averageInterUpdateTime.getDouble() + update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_WAKEUPTOUPDATE,  dMe));

			this.stat_traceOf_hd.add(t, this.currentNetPlan.getVectorDemandOfferedTraffic());
			this.stat_traceOf_pie.add(t, this.congControl_price_e.copy ());
			this.stat_traceOf_ye.add(t, this.currentNetPlan.getVectorLinkCarriedTraffic());
			this.stat_traceOf_objFunction.add(t, NetworkPerformanceMetrics.alphaUtility(currentNetPlan.getVectorDemandOfferedTraffic() , control_fairnessFactor.getDouble()));

			if (t > this.simulation_maxNumberOfUpdateIntervals.getDouble() * this.update_averageInterUpdateTime.getDouble()) { this.endSimulation (); }
			
			break;
		}
			

		default: throw new RuntimeException ("Unexpected received event");
		}
		
		
	}

	public String finish (StringBuilder st , double simTime)
	{
		if (simulation_outFileNameRoot.getString().equals("")) return null;
		stat_traceOf_hd.printToFile(new File (simulation_outFileNameRoot.getString() + "_hd.txt"));
		stat_traceOf_pie.printToFile(new File (simulation_outFileNameRoot.getString() + "_pie.txt"));
		stat_traceOf_ye.printToFile(new File (simulation_outFileNameRoot.getString() + "_ye.txt"));
		stat_traceOf_objFunction.printToFile(new File (simulation_outFileNameRoot.getString() + "_objFunc.txt"));
		Triple<DoubleMatrix1D,DoubleMatrix1D,Double> pair = computeOptimumSolution ();
		DoubleMatrix1D h_d_opt = pair.getFirst();
		DoubleMatrix1D pi_e = pair.getSecond();
		double optCost = pair.getThird();  
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_objFunc.txt"), optCost);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_hd.txt"), h_d_opt);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_pie.txt"), pi_e);
		return null;
	}
	
	private double computeHdFromPrices (Demand d)
	{
		DoubleMatrix1D infoIKnow_price_e = this.control_mostUpdatedLinkPriceKnownByDemand_de.viewRow (d.getIndex ());

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
		demandWeightedSumLinkPrices /= demandCarriedTraffic;

		/* compute the new h_d */
		final double new_hd = Math.max(this.control_minHd.getDouble() , Math.min(this.control_maxHd.getDouble(), Math.pow(demandWeightedSumLinkPrices, -1/this.control_fairnessFactor.getDouble())));
		return new_hd;
	}

	private Triple<DoubleMatrix1D,DoubleMatrix1D,Double> computeOptimumSolution ()
	{
		/* Modify the map so that it is the pojection where all elements sum h_d, and are non-negative */
		final int D = this.currentNetPlan.getNumberOfDemands();

		OptimizationProblem op = new OptimizationProblem();

		/* Add the decision variables to the problem */
		op.addDecisionVariable("h_d", false, new int[] {1, D}, control_minHd.getDouble(), control_maxHd.getDouble());

		/* Set some input parameters */
		op.setInputParameter("u_e", currentNetPlan.getVectorLinkCapacity() , "row");
		op.setInputParameter("alpha", control_fairnessFactor.getDouble());
		op.setInputParameter("R_de", currentNetPlan.getMatrixDemand2LinkAssignment());

		/* Sets the objective function */
		if (control_fairnessFactor.getDouble() == 1)
		    op.setObjectiveFunction("maximize", "sum(ln(h_d))");
		else if (control_fairnessFactor.getDouble() == 0)
		    op.setObjectiveFunction("maximize", "sum(h_d)");
		else
		    op.setObjectiveFunction("maximize", "(1-alpha) * sum(h_d ^ (1-alpha))");

		op.addConstraint("h_d * R_de <= u_e" , "pi_e"); // the capacity constraints (E constraints)

		/* Call the solver to solve the problem */
		op.solve("ipopt");

		/* If an optimal solution was not found, quit */
		if (!op.solutionIsOptimal()) throw new Net2PlanException("An optimal solution was not found");

		/* Retrieve the optimum solutions */
		DoubleMatrix1D h_d = op.getPrimalSolution("h_d").view1D ();
		DoubleMatrix1D pi_e = op.getMultipliersOfConstraint("pi_e").assign(DoubleFunctions.abs).view1D ();
		
		return Triple.of(h_d,pi_e,NetworkPerformanceMetrics.alphaUtility(currentNetPlan.getVectorDemandOfferedTraffic() , control_fairnessFactor.getDouble()));
	}
	
}
