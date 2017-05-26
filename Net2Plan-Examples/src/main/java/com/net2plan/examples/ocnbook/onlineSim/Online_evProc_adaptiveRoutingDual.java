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
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 
 * This module implements a distributed dual-gradient based algorithm, for iteratively adapting the network routing.
 *
 * Ths event processor is adapted to permit observing the algorithm performances under user-defined conditions, 
 * including asynchronous distributed executions, where signaling can be affected by losses and/or delays, and/or measurement errors. 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec10_2_adaptiveRoutingDual.m">{@code fig_sec10_2_adaptiveRoutingDual.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * 
 * To simulate a network with this module, use the {@code Online_evGen_doNothing} generator.
 * 
 * @net2plan.keywords Flow assignment (FA), Distributed algorithm, Dual gradient algorithm
 * @net2plan.ocnbooksections Section 10.2
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
@SuppressWarnings("unchecked")
public class Online_evProc_adaptiveRoutingDual extends IEventProcessor
{
	private static PrintStream getNulFile () { try { return new PrintStream (new FileOutputStream ("NUL") , false); } catch (Exception e) {e.printStackTrace(); throw new RuntimeException ("Not NUL file"); }   } 
	private PrintStream log = getNulFile (); //System.err;//new PrintStream (new FileOutputStream ("NUL") , true);

	private InputParameter signaling_isSynchronous = new InputParameter ("signaling_isSynchronous", false , "true if all the distributed agents involved wake up synchronously to send the signaling messages");
	private InputParameter signaling_averageInterMessageTime = new InputParameter ("signaling_averageInterMessageTime", 1.0 , "Average time between two signaling messages sent by an agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter signaling_maxFluctuationInterMessageTime = new InputParameter ("signaling_maxFluctuationInterMessageTime", 0.5 , "Max fluctuation in time between two signaling messages sent by an agent" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter signaling_averageDelay = new InputParameter ("signaling_averageDelay", 0.0 , "Average time between signaling message transmission by an agent and its reception by other or others" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter signaling_maxFluctuationInDelay = new InputParameter ("signaling_maxFluctuationInDelay", 0.0 , "Max fluctuation in time in the signaling delay, in absolute time values. The signaling delays are sampled from a uniform distribution within the given interval" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter signaling_signalingLossProbability = new InputParameter ("signaling_signalingLossProbability", 0.05 , "Probability that a signaling message transmitted is lost (not received by other or others involved agents)" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter update_isSynchronous = new InputParameter ("update_isSynchronous", false , "true if all the distributed agents involved wake up synchronousely to update its state");
	private InputParameter update_averageInterUpdateTime = new InputParameter ("update_averageInterUpdateTime", 1.0 , "Average time between two updates of an agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter update_maxFluctuationInterUpdateTime = new InputParameter ("update_maxFluctuationInterUpdateTime", 0.5 , "Max fluctuation in time in the update interval of an agent, in absolute time values. The update intervals are sampled from a uniform distribution within the given interval" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter simulation_maxNumberOfUpdateIntervals = new InputParameter ("simulation_maxNumberOfUpdateIntervals", 500.0 , "Maximum number of update intervals in average per agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter simulation_randomSeed = new InputParameter ("simulation_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter simulation_outFileNameRoot = new InputParameter ("simulation_outFileNameRoot", "adaptiveRoutingDual" , "Root of the file name to be used in the output files. If blank, no output");
	private InputParameter gradient_maxGradientAbsoluteNoise = new InputParameter ("gradient_maxGradientAbsoluteNoise", 0.0 , "Max value of the added noise to the gradient coordinate in absolute values" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter gradient_gammaStep = new InputParameter ("gradient_gammaStep", 0.001 , "Gamma step in the gradient algorithm" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter gradient_heavyBallBetaParameter = new InputParameter ("gradient_heavyBallBetaParameter", 0.0 , "Beta parameter of heavy ball, between 0 and 1. Value 0 means no heavy ball" , 0 , true , 1.0 , true);
	private InputParameter gradient_maxGradientCoordinateChange = new InputParameter ("gradient_maxGradientCoordinateChange", 1.0e10 , "Maximum change in an iteration of a gradient coordinate" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter gradient_regularizationEpsilon = new InputParameter ("gradient_regularizationEpsilon", 1.0e-3 , "Regularization factor in the objective function term to make it strictly convex" , 0 , true , Double.MAX_VALUE , true);

	private InputParameter control_maxNumberOfPathsPerDemand = new InputParameter ("control_maxNumberOfPathsPerDemand", (int) 5 , "Number of acceptable paths per demand. Take the k-shortest paths in number of hops" , 1 , Integer.MAX_VALUE);
	private InputParameter control_initialLinkWeights = new InputParameter ("control_initialLinkWeights", 0.5 , "Initial value of the link weights" , 0 , true , Double.MAX_VALUE , true);
	
	private Random rng;
	private int E , R , N , D;

	private static final int SIGNALING_WAKEUPTOSENDMESSAGE = 300;
	private static final int SIGNALING_RECEIVEDMESSAGE = 301;
	private static final int UPDATE_WAKEUPTOUPDATE = 302;

	private NetPlan currentNetPlan;
	private DoubleMatrix1D routing_price_e;
	
	private DoubleMatrix2D routing_mostUpdatedLinkPriceKnownByNode_ne; 
	private DoubleMatrix1D routing_previousLinkWeight_e;
	private int [][] control_routeIndexes_d;
	
	private TimeTrace stat_traceOf_xp;
	private TimeTrace stat_traceOf_pie;
	private TimeTrace stat_traceOf_ye;
	private TimeTrace stat_traceOf_objFunction;

	@Override
	public String getDescription()
	{
		return "This module implements a distributed dual-gradient based algorithm, for iteratively adapting the network routing.";
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

		if (currentNetPlan.getNumberOfLayers() != 1) throw new Net2PlanException ("This algorithm works in single layer networks");

		this.rng = new Random (simulation_randomSeed.getLong());
		this.currentNetPlan = currentNetPlan;
		this.E = currentNetPlan.getNumberOfLinks();
		this.N = currentNetPlan.getNumberOfNodes();
		this.D = currentNetPlan.getNumberOfDemands();
		
		/* Check all nodes have output links */
		for (Node n : currentNetPlan.getNodes()) if (n.getOutgoingLinks().isEmpty()) throw new Net2PlanException ("All nodes should have output links");

		/* Set the initial prices in the links to all zeros */
		this.routing_price_e = DoubleFactory1D.dense.make (E , control_initialLinkWeights.getDouble());
		this.routing_previousLinkWeight_e = routing_price_e.copy ();
		for (Link e : currentNetPlan.getLinks ()) e.setAttribute("pi_e" , "" + routing_price_e.get (e.getIndex ()));
		
		/* Initialize the information each demand knows of the prices of all the links */
		this.routing_mostUpdatedLinkPriceKnownByNode_ne = DoubleFactory2D.dense.make (N,E,control_initialLinkWeights.getDouble());
		
		/* Sets the initial routing, according to the prices, with all the traffic balanced equally in all the paths of the demand */
		currentNetPlan.removeAllUnicastRoutingInformation();
		currentNetPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
		this.currentNetPlan.addRoutesFromCandidatePathList(currentNetPlan.computeUnicastCandidatePathList(null , control_maxNumberOfPathsPerDemand.getInt(), -1, -1, -1, -1, -1, -1 , null));
		
		this.R = currentNetPlan.getNumberOfRoutes ();
		this.control_routeIndexes_d = new int [D][];
		for (Demand d : this.currentNetPlan.getDemands())
		{
			control_routeIndexes_d [d.getIndex ()] = new int [d.getRoutes ().size ()]; 
			int counter_r = 0; for (Route r : d.getRoutes ()) control_routeIndexes_d [d.getIndex ()][counter_r ++] = r.getIndex ();
		}

		
		for (Demand d : this.currentNetPlan.getDemands())
			updateDemandRoutingFromWeightsKnown (d);		

		/* Initially all nodes receive a "wake up to transmit" event, aligned at time zero or y asynchr => randomly chosen */
		for (Link e : currentNetPlan.getLinks())
		{
			final double signalingTime = (signaling_isSynchronous.getBoolean())? signaling_averageInterMessageTime.getDouble() : Math.max(0 , signaling_averageInterMessageTime.getDouble() + signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_WAKEUPTOSENDMESSAGE , e));
		}
		for (Node n : currentNetPlan.getNodes())
		{
			final double updateTime = (update_isSynchronous.getBoolean())? update_averageInterUpdateTime.getDouble() : Math.max(0 , update_averageInterUpdateTime.getDouble() + update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_WAKEUPTOUPDATE , n));
		}

		/* Intialize the traces */
		this.stat_traceOf_xp = new TimeTrace ();
		this.stat_traceOf_pie = new TimeTrace ();
		this.stat_traceOf_ye = new TimeTrace ();
		this.stat_traceOf_objFunction = new TimeTrace (); 
		this.stat_traceOf_xp.add(0.0, this.currentNetPlan.getVectorRouteCarriedTraffic());
		this.stat_traceOf_pie.add(0.0, this.routing_price_e.copy ());
		this.stat_traceOf_ye.add(0.0, this.currentNetPlan.getVectorLinkCarriedTraffic());
		this.stat_traceOf_objFunction.add(0.0, computeObjectiveFucntionFromNetPlan());

	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		final double t = event.getEventTime();
		switch (event.getEventType())
		{
		case SIGNALING_RECEIVEDMESSAGE: // A node receives a price information from a link
		{
			final Triple<Node,Link,Double> signalInfo = (Triple<Node,Link,Double>) event.getEventObject();
			final Node nMe = signalInfo.getFirst();
			final Link e = signalInfo.getSecond();
			final double link_price_e = signalInfo.getThird();
			this.routing_mostUpdatedLinkPriceKnownByNode_ne.set(nMe.getIndex () , e.getIndex () , link_price_e);
			break;
		}
		
		case SIGNALING_WAKEUPTOSENDMESSAGE: // A link updates its price (gradient), and signals it to all the nodes
		{
			final Link eMe = (Link) event.getEventObject();

			/* Update the gradient iteration */
			final double previous_pi_e = this.routing_previousLinkWeight_e.get(eMe.getIndex ());
			final double old_pi_e = this.routing_price_e.get(eMe.getIndex ());
			final double gradient_e = eMe.getCarriedTraffic() - eMe.getCapacity() +  2*gradient_maxGradientAbsoluteNoise.getDouble()*(this.rng.nextDouble()-0.5);
			final double new_pi_e_notProjected = old_pi_e + this.gradient_gammaStep.getDouble() * gradient_e + this.gradient_heavyBallBetaParameter.getDouble() * (old_pi_e - previous_pi_e);
			double new_pi_e_projected = Math.max(0, new_pi_e_notProjected);
			if (gradient_maxGradientCoordinateChange.getDouble() > 0)
				new_pi_e_projected = GradientProjectionUtils.scaleDown_maxAbsoluteCoordinateChange (old_pi_e , new_pi_e_projected ,  gradient_maxGradientCoordinateChange.getDouble());

			this.routing_previousLinkWeight_e.set(eMe.getIndex (), old_pi_e);
			this.routing_price_e.set(eMe.getIndex () , new_pi_e_projected);
			eMe.setAttribute("pi_e" , "" + routing_price_e.get (eMe.getIndex ()));

			
			/* Send the events of the signaling information messages to all the nodes */
			if (rng.nextDouble() >= this.signaling_signalingLossProbability.getDouble()) // the signaling may be lost => lost to all nodes
				for (Node n : currentNetPlan.getNodes ())
				{
					final double signalingReceptionTime = t + Math.max(0 , signaling_averageDelay.getDouble() + signaling_maxFluctuationInDelay.getDouble() * (rng.nextDouble() - 0.5));
					this.scheduleEvent(new SimEvent (signalingReceptionTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_RECEIVEDMESSAGE , Triple.of(n , eMe , routing_price_e.get(eMe.getIndex ()))));
				}
			
			/* Re-schedule when to wake up again */
			final double signalingTime = signaling_isSynchronous.getBoolean()? t + signaling_averageInterMessageTime.getDouble() : Math.max(t , t + signaling_averageInterMessageTime.getDouble() + signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_WAKEUPTOSENDMESSAGE , eMe));
			break;
			
		}
		
		case UPDATE_WAKEUPTOUPDATE: // a node updates its source routing using the most updated information it has
		{
			final Node nMe = (Node) event.getEventObject();	
			for (Demand d : nMe.getOutgoingDemands())
				updateDemandRoutingFromWeightsKnown (d);		

			final double updateTime = update_isSynchronous.getBoolean()? t + update_averageInterUpdateTime.getDouble() : Math.max(t , t + update_averageInterUpdateTime.getDouble() + update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_WAKEUPTOUPDATE,  nMe));

			this.stat_traceOf_xp.add(t, this.currentNetPlan.getVectorRouteCarriedTraffic());
			this.stat_traceOf_pie.add(t, this.routing_price_e.copy ());
			this.stat_traceOf_ye.add(t, this.currentNetPlan.getVectorLinkCarriedTraffic());
			this.stat_traceOf_objFunction.add(t, computeObjectiveFucntionFromNetPlan());

			if (t > this.simulation_maxNumberOfUpdateIntervals.getDouble() * this.update_averageInterUpdateTime.getDouble()) { this.endSimulation (); }
			
			break;
		}
			

		default: throw new RuntimeException ("Unexpected received event");
		}
		
		
	}

	public String finish (StringBuilder st , double simTime)
	{
		if (simulation_outFileNameRoot.getString().equals("")) return null;
		stat_traceOf_xp.printToFile(new File (simulation_outFileNameRoot.getString() + "_xp.txt"));
		stat_traceOf_pie.printToFile(new File (simulation_outFileNameRoot.getString() + "_pie.txt"));
		stat_traceOf_ye.printToFile(new File (simulation_outFileNameRoot.getString() + "_ye.txt"));
		stat_traceOf_objFunction.printToFile(new File (simulation_outFileNameRoot.getString() + "_objFunc.txt"));
		Quadruple<DoubleMatrix1D,DoubleMatrix1D,DoubleMatrix1D,Double> pair = computeOptimumSolution (true);
		DoubleMatrix1D x_p_opt = pair.getFirst();
		DoubleMatrix1D pi_e = pair.getSecond();  
		DoubleMatrix1D y_e = pair.getThird();  
		double optCost = pair.getFourth();  
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jomReg_objFunc.txt"), optCost);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jomReg_xp.txt"), x_p_opt);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jomReg_pie.txt"), pi_e);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jomReg_ye.txt"), y_e);
		return null;
	}
	
	private double computeObjectiveFucntionFromNetPlan ()
	{
		double objFunc = 0;
		for (Route r : this.currentNetPlan.getRoutes())
		{
			final double x_r = r.getCarriedTraffic();
			objFunc +=  x_r * r.getNumberOfHops() + this.gradient_regularizationEpsilon.getDouble() * Math.pow(x_r, 2);
		}
		return objFunc;
	}
	
	private Quadruple<DoubleMatrix1D,DoubleMatrix1D,DoubleMatrix1D,Double> computeOptimumSolution (boolean isRegularized)
	{
		/* Modify the map so that it is the pojection where all elements sum h_d, and are non-negative */
		final int P = this.currentNetPlan.getNumberOfRoutes();
		final int E = this.currentNetPlan.getNumberOfLinks();
		final DoubleMatrix1D h_d = this.currentNetPlan.getVectorDemandOfferedTraffic();
		final DoubleMatrix1D u_e = this.currentNetPlan.getVectorLinkCapacity();
		final DoubleMatrix1D h_p = this.currentNetPlan.getVectorRouteOfferedTrafficOfAssociatedDemand();
				
		OptimizationProblem op = new OptimizationProblem();
		op.addDecisionVariable("x_p", false , new int[] { 1 , P }, DoubleUtils.zeros(P) , h_p.toArray()); 
		op.addDecisionVariable("y_e", false , new int[] { 1 , E }, DoubleUtils.zeros(E) , u_e.toArray()); 
		op.setInputParameter("h_d", h_d , "row");
		op.setInputParameter("u_e", u_e , "row");
		op.setInputParameter("epsilon", this.gradient_regularizationEpsilon.getDouble());
		
		/* Set some input parameters */
		if (!isRegularized)
			op.setObjectiveFunction("minimize", "sum (y_e)");
		else
			op.setObjectiveFunction("minimize", "sum (y_e) + epsilon * x_p * x_p'");

		/* VECTORIAL FORM OF THE CONSTRAINTS  */
		op.setInputParameter("A_dp", currentNetPlan.getMatrixDemand2RouteAssignment());
		op.setInputParameter("A_ep", currentNetPlan.getMatrixLink2RouteAssignment());
		op.addConstraint("A_dp * x_p' == h_d'"); // for each demand, the 100% of the traffic is carried (summing the associated paths)
		op.addConstraint("A_ep * x_p' == y_e'"); // set the traffic in each link equal to y_e
		
		/* Call the solver to solve the problem */
		op.solve("ipopt");

		/* If an optimal solution was not found, quit */
		if (!op.solutionIsOptimal ()) throw new RuntimeException ("An optimal solution was not found");
	
		/* Retrieve the optimum solutions. Convert the bps into Erlangs */
		final DoubleMatrix1D x_p = op.getPrimalSolution("x_p").view1D();
		final DoubleMatrix1D y_e = op.getPrimalSolution("y_e").view1D();
		final DoubleMatrix1D pi_e = op.getMultiplierOfUpperBoundConstraintToPrimalVariables("y_e").view1D();
		
		/* Check the solution */
		for (Link e : this.currentNetPlan.getLinks())
		{
			double yThisLink = 0; for (Route r : e.getTraversingRoutes()) yThisLink += x_p.get(r.getIndex ());
			if (yThisLink > e.getCapacity() + 1E-3) throw new RuntimeException ("Bad");
		}
		for (Demand d : this.currentNetPlan.getDemands())
		{
			double trafDemand = 0; for (Route r : d.getRoutes()) trafDemand += x_p.get(r.getIndex ());
			if (Math.abs(trafDemand - d.getOfferedTraffic()) > 1E-3) throw new RuntimeException ("Bad");
		}
		
		/* Compute optimum cost */
		double objFunc = 0; for (Route r : this.currentNetPlan.getRoutes()) 
		{
			final double x_r = x_p.get(r.getIndex ()); 
			objFunc +=  x_r * r.getNumberOfHops();
			if (isRegularized) objFunc += this.gradient_regularizationEpsilon.getDouble() * Math.pow(x_r, 2);
		}
		return Quadruple.of(x_p,pi_e,y_e,objFunc);

	}
	
	
	private void updateDemandRoutingFromWeightsKnown (Demand d)
	{
		final Node a_d = d.getIngressNode();
		final double h_d = d.getOfferedTraffic();
		final DoubleMatrix1D weightsKnown_e = this.routing_mostUpdatedLinkPriceKnownByNode_ne.viewRow(a_d.getIndex ()); 
		DoubleMatrix1D a_k = DoubleFactory1D.dense.make (R);
		for (Route r : d.getRoutes())
		{
			double val = 0; 
			for (Link e : r.getSeqLinks()) val += weightsKnown_e.get(e.getIndex()) + 1; 
			a_k.set(r.getIndex (), val);
		}

		DoubleMatrix1D x_r = GradientProjectionUtils.regularizedProjection_sumEquality (a_k , control_routeIndexes_d [d.getIndex ()] , null , h_d , this.gradient_regularizationEpsilon.getDouble());
		for (Route r : d.getRoutes ())
			r.setCarriedTraffic(x_r.get(r.getIndex ()) ,  x_r.get(r.getIndex ()));
	}
	
}
