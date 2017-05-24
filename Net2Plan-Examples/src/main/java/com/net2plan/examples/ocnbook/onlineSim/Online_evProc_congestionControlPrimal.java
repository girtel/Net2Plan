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
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.NetworkPerformanceMetrics;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.*;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** 
 * This module implements a distributed primal-gradient based algorithm using a barrier function, for adapting the demand injected traffic (congestion control) in the network, to maximize the network utility enforcing a fair allocation of the resources.
 *
 * Ths event processor is adapted to permit observing the algorithm performances under user-defined conditions, 
 * including asynchronous distributed executions, where signaling can be affected by losses and/or delays, and/or measurement errors. 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec9_4_congestionControlPrimal.m">{@code fig_sec9_4_congestionControlPrimal.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * 
 * To simulate a network with this module, use the {@code Online_evGen_doNothing} generator.
 * 
 * @net2plan.keywords Bandwidth assignment (BA), Distributed algorithm, Primal gradient algorithm
 * @net2plan.ocnbooksections Section 9.4
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
@SuppressWarnings("unchecked")
public class Online_evProc_congestionControlPrimal extends IEventProcessor
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
	private InputParameter gradient_diagonalScaling = new InputParameter ("gradient_diagonalScaling", false , "Whether diagonal scaling is applied or not");
	private InputParameter gradient_maxGradientAbsoluteNoise = new InputParameter ("gradient_maxGradientAbsoluteNoise", 0.0 , "Max value of the added noise to the gradient coordinate in absolute values" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter gradient_gammaStep = new InputParameter ("gradient_gammaStep", 20.0 , "Gamma step in the gradient algorithm" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter gradient_heavyBallBetaParameter = new InputParameter ("gradient_heavyBallBetaParameter", 0.0 , "Beta parameter of heavy ball, between 0 and 1. Value 0 means no heavy ball" , 0 , true , 1.0 , true);
	private InputParameter gradient_maxGradientCoordinateChange = new InputParameter ("gradient_maxGradientCoordinateChange", 1.0 , "Maximum change in an iteration of a gradient coordinate" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter gradient_penaltyMethod = new InputParameter ("gradient_penaltyMethod", "#select# barrier exterior-penalty" , "Use a barrier (interior penalty) or an exterior penalty for enforcing link capacity constraints");
	private InputParameter gradient_exteriorPenaltyMuFactor = new InputParameter ("gradient_exteriorPenaltyMuFactor", 1000 , "Mu factor to apply in the exterior penalty method" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter gradient_interiorPenaltyEpsilonFactor = new InputParameter ("gradient_interiorPenaltyEpsilonFactor", 0.00001 , "Factor to multiply the delay penalization function" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter simulation_maxNumberOfUpdateIntervals = new InputParameter ("simulation_maxNumberOfUpdateIntervals", 800.0 , "Maximum number of update intervals in average per agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter simulation_randomSeed = new InputParameter ("simulation_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter simulation_outFileNameRoot = new InputParameter ("simulation_outFileNameRoot", "congestionControlPrimal" , "Root of the file name to be used in the output files. If blank, no output");

	private InputParameter control_minHd = new InputParameter ("control_minHd", 0.1 , "Minimum traffic assigned to each demand" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter control_maxHd = new InputParameter ("control_maxHd", 1.0E6 , "Maximum traffic assigned to each demand" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter control_fairnessFactor = new InputParameter ("control_fairnessFactor", 2.0 , "Fairness factor in utility function of congestion control" , 0 , true , Double.MAX_VALUE , true);
	
	private Random rng;
	
	private static final int SIGNALING_WAKEUPTOSENDMESSAGE = 300;
	private static final int SIGNALING_RECEIVEDMESSAGE = 301;
	private static final int UPDATE_WAKEUPTOUPDATE = 302;

	private NetPlan currentNetPlan;
	
	private double control_epsilonOrMuFactor;
	private DoubleMatrix1D control_priceFirstOrder_e;
	private DoubleMatrix1D control_priceSecondOrder_e;
	private DoubleMatrix1D control_previous_h_d;
	private DoubleMatrix2D control_mostUpdatedLinkFirstOrderPriceKnownDemand_de; 
	private DoubleMatrix2D control_mostUpdatedLinkSecondOrderPriceKnownDemand_de; 
	private boolean control_isBarrierMethod;
	private int D , E;
	
	private TimeTrace stat_traceOf_objFunction;
	private TimeTrace stat_traceOf_hd;
	private TimeTrace stat_traceOf_maxLinkTraffic;

	@Override
	public String getDescription()
	{
		return "This module implements a distributed primal-gradient based algorithm using a barrier function, for adapting the demand injected traffic (congestion control) in the network, to maximize the network utility enforcing a fair allocation of the resources.";
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


		this.control_isBarrierMethod = gradient_penaltyMethod.getString ().equals ("barrier");
		this.control_epsilonOrMuFactor = control_isBarrierMethod? gradient_interiorPenaltyEpsilonFactor.getDouble() : gradient_exteriorPenaltyMuFactor.getDouble();
		this.rng = new Random (simulation_randomSeed.getLong());
		this.D = currentNetPlan.getNumberOfDemands ();
		this.E = currentNetPlan.getNumberOfLinks ();
		if ((E == 0) || (D == 0)) throw new Net2PlanException ("The input design should have links and demands");
		
		/* Initially all demands offer exactly hdMin */
		control_previous_h_d = DoubleFactory1D.dense.make (D , control_minHd.getDouble());
		currentNetPlan.setVectorDemandOfferedTraffic(control_previous_h_d);
		for (Demand d : currentNetPlan.getDemands()) // carry the demand traffic
		{ 
			final Set<Route> routes = d.getRoutes(); 
			for (Route r : routes) r.setCarriedTraffic(d.getOfferedTraffic() / routes.size () , d.getOfferedTraffic() / routes.size ()); 
		}
		
		/* Set the initial prices in the links */
		this.control_priceFirstOrder_e = DoubleFactory1D.dense.make (E); for (Link e : this.currentNetPlan.getLinks ()) control_priceFirstOrder_e.set (e.getIndex () , computeFirstOrderPriceFromNetPlan(e));
		this.control_priceSecondOrder_e = DoubleFactory1D.dense.make (E); for (Link e : this.currentNetPlan.getLinks ()) control_priceSecondOrder_e.set (e.getIndex () , computeSecondOrderPriceFromNetPlan(e));
		
		/* Initialize the information each demand knows of the prices of all the links */
		this.control_mostUpdatedLinkFirstOrderPriceKnownDemand_de = DoubleFactory2D.dense.make (D,E);
		this.control_mostUpdatedLinkSecondOrderPriceKnownDemand_de = DoubleFactory2D.dense.make (D,E);
		for (Demand d : currentNetPlan.getDemands ())
		{
			control_mostUpdatedLinkFirstOrderPriceKnownDemand_de.viewRow (d.getIndex ()).assign (control_priceFirstOrder_e);
			control_mostUpdatedLinkSecondOrderPriceKnownDemand_de.viewRow (d.getIndex ()).assign (control_priceSecondOrder_e);
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
		this.stat_traceOf_objFunction = new TimeTrace (); 
		this.stat_traceOf_maxLinkTraffic = new TimeTrace ();
		this.stat_traceOf_hd.add(0 , this.currentNetPlan.getVectorDemandOfferedTraffic());
		this.stat_traceOf_objFunction.add(0 , NetworkPerformanceMetrics.alphaUtility(currentNetPlan.getVectorDemandOfferedTraffic() , control_fairnessFactor.getDouble()));
		this.stat_traceOf_maxLinkTraffic.add(0.0, this.currentNetPlan.getVectorLinkCarriedTraffic().getMaxLocation() [0]);
	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		final double t = event.getEventTime();
		switch (event.getEventType())
		{
		case SIGNALING_RECEIVEDMESSAGE: // A node receives from an out neighbor the q_nt for any destination
		{
			final Quadruple<Demand,Link,Double,Double> signalInfo = (Quadruple<Demand,Link,Double,Double>) event.getEventObject();
			final Demand dMe = signalInfo.getFirst();
			final Link e = signalInfo.getSecond ();
			control_mostUpdatedLinkFirstOrderPriceKnownDemand_de.set (dMe.getIndex () , e.getIndex () , signalInfo.getThird ());
			control_mostUpdatedLinkSecondOrderPriceKnownDemand_de.set (dMe.getIndex () , e.getIndex () , signalInfo.getFourth ());
			break;
		}
		
		case SIGNALING_WAKEUPTOSENDMESSAGE: // A node broadcasts signaling info to its 1 hop neighbors
		{
			final Link eMe = (Link) event.getEventObject();

			/* Send the events of the signaling information messages to all the nodes */
			if (rng.nextDouble() >= this.signaling_signalingLossProbability.getDouble()) // the signaling may be lost => lost to all demands
				for (Route route : eMe.getTraversingRoutes())
				{
					final Demand d = route.getDemand();
					Quadruple<Demand,Link,Double,Double> infoToSignal = Quadruple.of(d , eMe ,  this.computeFirstOrderPriceFromNetPlan(eMe) , computeSecondOrderPriceFromNetPlan(eMe));
					final double signalingReceptionTime = t + Math.max(0 , signaling_averageDelay.getDouble() + signaling_maxFluctuationInDelay.getDouble() * (rng.nextDouble() - 0.5));
					this.scheduleEvent(new SimEvent (signalingReceptionTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_RECEIVEDMESSAGE , infoToSignal));
				}
			
			/* Re-schedule when to wake up again */
			final double signalingTime = signaling_isSynchronous.getBoolean()? t + signaling_averageInterMessageTime.getDouble() : Math.max(t , t + signaling_averageInterMessageTime.getDouble() + signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_WAKEUPTOSENDMESSAGE , eMe));
			break;
		}

		case UPDATE_WAKEUPTOUPDATE: 
		{
			final Demand dMe = (Demand) event.getEventObject();
			
			DoubleMatrix1D infoIKnow_priceFirstOrder_e = this.control_mostUpdatedLinkFirstOrderPriceKnownDemand_de.viewRow (dMe.getIndex ());
			DoubleMatrix1D infoIKnow_priceSecondOrder_e = this.control_mostUpdatedLinkSecondOrderPriceKnownDemand_de.viewRow (dMe.getIndex ());

			/* compute the demand price as weighted sum in the routes of route prices  */
			double demandWeightedSumLinkPrices = 0;
			double demandWeightedSumSecondDerivativeLinkPrices = 0;
			double demandCarriedTraffic = 0; 
			for (Route r : dMe.getRoutes ())
			{
				final double h_r = r.getCarriedTraffic();
				demandCarriedTraffic += h_r;
				for (Link e : r.getSeqLinks())
				{
					demandWeightedSumLinkPrices += h_r * infoIKnow_priceFirstOrder_e.get(e.getIndex ());
					demandWeightedSumSecondDerivativeLinkPrices += h_r * infoIKnow_priceSecondOrder_e.get(e.getIndex ());
				}
			}
			if (Math.abs(demandCarriedTraffic - dMe.getOfferedTraffic()) > 1E-3) throw new RuntimeException ("Not all the traffic is carried. demandCarriedTraffic: " + demandCarriedTraffic);
			demandWeightedSumLinkPrices /= demandCarriedTraffic;
			demandWeightedSumSecondDerivativeLinkPrices /= demandCarriedTraffic;
			
			/* compute the new h_d */
			final double old_hd = dMe.getOfferedTraffic();
			final double gradient_d = Math.pow(old_hd, -this.control_fairnessFactor.getDouble()) - this.control_epsilonOrMuFactor * demandWeightedSumLinkPrices +  2*gradient_maxGradientAbsoluteNoise.getDouble()*(rng.nextDouble()-0.5);
			double seconDerivativehd2ForDiagonalScaling = (gradient_diagonalScaling.getBoolean())? Math.abs(-this.control_fairnessFactor.getDouble() * Math.pow(old_hd, -this.control_fairnessFactor.getDouble()-1)- this.control_epsilonOrMuFactor * demandWeightedSumSecondDerivativeLinkPrices) : 1;
			if (!Double.isFinite(seconDerivativehd2ForDiagonalScaling)) seconDerivativehd2ForDiagonalScaling = 1; 
			
			double coordinateChange =  this.gradient_gammaStep.getDouble() / seconDerivativehd2ForDiagonalScaling * gradient_d + this.gradient_heavyBallBetaParameter.getDouble() * (old_hd - this.control_previous_h_d.get(dMe.getIndex ())  );
			
			if (gradient_maxGradientCoordinateChange.getDouble() > 0) 
				coordinateChange = Math.signum(coordinateChange) * Math.min(gradient_maxGradientCoordinateChange.getDouble() , Math.abs(coordinateChange));  

			final double new_hd = GradientProjectionUtils.euclideanProjection_boxLike (old_hd + coordinateChange , control_minHd.getDouble() , control_maxHd.getDouble ());

			control_previous_h_d.set (dMe.getIndex (), dMe.getOfferedTraffic());
			dMe.setOfferedTraffic(new_hd); 
			final Set<Route> routes = dMe.getRoutes(); // carry the demand traffic
			for (Route r : routes) r.setCarriedTraffic(dMe.getOfferedTraffic() / routes.size () , dMe.getOfferedTraffic() / routes.size ());

			final double updateTime = update_isSynchronous.getBoolean()? t + update_averageInterUpdateTime.getDouble() : Math.max(t , t + update_averageInterUpdateTime.getDouble() + update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_WAKEUPTOUPDATE,  dMe));

			this.stat_traceOf_hd.add(t, this.currentNetPlan.getVectorDemandOfferedTraffic());
			this.stat_traceOf_objFunction.add(t, NetworkPerformanceMetrics.alphaUtility(currentNetPlan.getVectorDemandOfferedTraffic() , control_fairnessFactor.getDouble()));
			this.stat_traceOf_maxLinkTraffic.add(t, this.currentNetPlan.getVectorLinkCarriedTraffic().getMaxLocation() [0]);

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
		stat_traceOf_objFunction.printToFile(new File (simulation_outFileNameRoot.getString() + "_objFunc.txt"));
		stat_traceOf_maxLinkTraffic.printToFile(new File (simulation_outFileNameRoot.getString() + "_maxYe.txt"));
		Pair<DoubleMatrix1D,Double> optPair = computeOptimumSolution ();
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_hd.txt"), optPair.getFirst());
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_objFunc.txt"), optPair.getSecond());
		return null;
	}
	
			
	private Pair<DoubleMatrix1D,Double> computeOptimumSolution ()
	{
		OptimizationProblem op = new OptimizationProblem();

		/* Add the decision variables to the problem */
		op.addDecisionVariable("h_d", false, new int[] {1, D}, control_minHd.getDouble() , control_maxHd.getDouble());

		/* Set some input parameters */
		op.setInputParameter("u_e", currentNetPlan.getVectorLinkCapacity() , "row");
		op.setInputParameter("alpha", this.control_fairnessFactor.getDouble());
		op.setInputParameter("R_de", currentNetPlan.getMatrixDemand2LinkAssignment());

		/* Sets the objective function */
		if (control_fairnessFactor.getDouble() == 1)
		    op.setObjectiveFunction("maximize", "sum(ln(h_d))");
		else if (control_fairnessFactor.getDouble() == 0)
		    op.setObjectiveFunction("maximize", "sum(h_d)");
		else
		    op.setObjectiveFunction("maximize", "(1-alpha) * sum(h_d ^ (1-alpha))");

		op.addConstraint("h_d * R_de <= u_e"); // the capacity constraints (E constraints)

		/* Call the solver to solve the problem */
		op.solve("ipopt");

		/* If an optimal solution was not found, quit */
		if (!op.solutionIsOptimal()) throw new Net2PlanException("An optimal solution was not found");

		/* Retrieve the optimum solutions */
		DoubleMatrix1D h_d = op.getPrimalSolution("h_d").view1D ();
		return Pair.of(h_d ,NetworkPerformanceMetrics.alphaUtility(h_d , control_fairnessFactor.getDouble()));
	}
			
	
	
	/* Computes the price, and the price for diagonal scaling */
	private double computeFirstOrderPriceFromNetPlan (Link e)
	{
		final double y_e = e.getCarriedTraffic();
		final double u_e = e.getCapacity();
		if (u_e == 0) throw new RuntimeException ("Zero capacity in a link");
		double price = 0;
		if (control_isBarrierMethod)
		{
			if (y_e / u_e < 1)
				price = u_e / Math.pow(u_e - y_e , 2);
			else
				price = Double.MAX_VALUE; 
		}
		else
			price = Math.max(0, y_e - u_e);
		return price;
	}

	private double computeSecondOrderPriceFromNetPlan (Link e)
	{
		final double y_e = e.getCarriedTraffic();
		final double u_e = e.getCapacity();
		if (u_e == 0) throw new RuntimeException ("Zero capacity in a link");
		double priceDiagScaling = 0;
		if (control_isBarrierMethod)
		{
			if (y_e / u_e < 1)
				priceDiagScaling = 2 * u_e / Math.pow(u_e - y_e , 3);
			else
				priceDiagScaling = Double.MAX_VALUE; //2 * u_e / Math.pow(u_e - 0.999999 * u_e , 3);
		}
		else
			priceDiagScaling = (y_e <= u_e)? 0 : 2;
		return priceDiagScaling;
	}

	
}
