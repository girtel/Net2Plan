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
import java.util.*;

/** 
 * This module implements a distributed primal-gradient based algorithm, for iteratively adapting the network routing.
 *
 * Ths event processor is adapted to permit observing the algorithm performances under user-defined conditions, 
 * including asynchronous distributed executions, where signaling can be affected by losses and/or delays, and/or measurement errors. 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec9_3_adaptiveRoutingPrimal.m">{@code fig_sec9_3_adaptiveRoutingPrimal.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * 
 * To simulate a network with this module, use the {@code Online_evGen_doNothing} generator.
 * 
 * @net2plan.keywords Flow assignment (FA), Distributed algorithm, Primal gradient algorithm
 * @net2plan.ocnbooksections Section 9.3
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
@SuppressWarnings("unchecked")
public class Online_evProc_adaptiveRoutingPrimal extends IEventProcessor
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
	private InputParameter gradient_maxGradientAbsoluteNoise = new InputParameter ("gradient_maxGradientAbsoluteNoise", 0.0 , "Max value of the added noise to the gradient coordinate in absolute values" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter gradient_gammaStep = new InputParameter ("gradient_gammaStep", 1.0 , "Gamma step in the gradient algorithm" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter gradient_heavyBallBetaParameter = new InputParameter ("gradient_heavyBallBetaParameter", 0.0 , "Beta parameter of heavy ball, between 0 and 1. Value 0 means no heavy ball" , 0 , true , 1.0 , true);
	private InputParameter gradient_maxGradientCoordinateChange = new InputParameter ("gradient_maxGradientCoordinateChange", 1000.0 , "Maximum change in an iteration of a gradient coordinate" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter simulation_maxNumberOfUpdateIntervals = new InputParameter ("simulation_maxNumberOfUpdateIntervals", 100.0 , "Maximum number of update intervals in average per agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter simulation_randomSeed = new InputParameter ("simulation_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter simulation_outFileNameRoot = new InputParameter ("simulation_outFileNameRoot", "adaptiveRoutingPrimal" , "Root of the file name to be used in the output files. If blank, no output");
	private InputParameter control_maxNumberOfPathsPerDemand = new InputParameter ("control_maxNumberOfPathsPerDemand", (int) 5 , "Number of acceptable paths per demand. Take the k-shortest paths in number of hops" , 1 , Integer.MAX_VALUE);

	private static PrintStream getNulFile () { try { return new PrintStream (new FileOutputStream ("NUL") , false); } catch (Exception e) {e.printStackTrace(); throw new RuntimeException ("Not NUL file"); }   } 
	private PrintStream log = getNulFile (); //System.err;//new PrintStream (new FileOutputStream ("NUL") , true);

	private Random rng;

	private static final int SIGNALING_WAKEUPTOSENDMESSAGE = 300;
	private static final int SIGNALING_RECEIVEDMESSAGE = 301;
	private static final int UPDATE_WAKEUPTOUPDATE = 302;

	private NetPlan currentNetPlan;

	private DoubleMatrix1D control_price_e;
	private DoubleMatrix2D control_mostUpdated_price_e_knownByNode_n;
	private DoubleMatrix1D control_previousXp;
	private int [][] control_routeIndexes_d;
	private int E , R , N , D;
	
//	private Map<Node,DoubleMatrix1D> routing_mostUpdated_price_e_knownByNode_n; 
//	private Map<Long,Set<Long>> routing_outgoingDemands_n;
//	private Map<Long,Double> routing_previousXp;
	
	private TimeTrace stat_traceOf_xp;
	private TimeTrace stat_traceOf_objFunction;
	
	
	@Override
	public String getDescription()
	{
		return "This module implements a distributed primal-gradient based algorithm, for iteratively adapting the network routing.";
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

//		/* Compute the CPL */
//		this.cpl = new CandidatePathList (this.currentNetPlan , "K" , "" + routing_maxNumberOfPathsPerDemand);
		
		/* Sets the initial routing, with all the traffic balanced equally in all the paths of the demand */
		currentNetPlan.removeAllUnicastRoutingInformation();
		currentNetPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
		this.currentNetPlan.addRoutesFromCandidatePathList(currentNetPlan.computeUnicastCandidatePathList(null , control_maxNumberOfPathsPerDemand.getInt(), -1, -1, -1, -1, -1, -1 , null));

		this.R = currentNetPlan.getNumberOfRoutes ();
		this.control_previousXp = DoubleFactory1D.dense.make (currentNetPlan.getNumberOfRoutes());
		this.control_routeIndexes_d = new int [D][];
		for (Demand d : this.currentNetPlan.getDemands())
		{
			final double h_d = d.getOfferedTraffic();
			final Set<Route> routes = d.getRoutes();
			if (routes.isEmpty()) throw new Net2PlanException ("A demand has no paths assigned: not connected topology");
			control_routeIndexes_d [d.getIndex ()] = new int [routes.size ()]; 
			int counter_r = 0;
			for (Route r : routes)
			{
				control_routeIndexes_d [d.getIndex ()][counter_r ++] = r.getIndex ();
				r.setCarriedTraffic(h_d / routes.size() , h_d / routes.size());
				control_previousXp.set(r.getIndex(), h_d / routes.size());
				log.println("Initial route: " + r + " demand: " +d + ", h_d: " + h_d + ", h_r = " + r.getCarriedTraffic() + ", seqLinks: "+ r.getSeqLinks());
			}
		}

		/* Set the initial prices in the nodes */
		this.control_price_e = DoubleFactory1D.dense.make (E);
		for (Link e : currentNetPlan.getLinks ()) this.control_price_e.set (e.getIndex() , computeLinkPriceFromNetPlan (e));
		
		/* Initialize the information each demand knows of the prices of all the links */
		this.control_mostUpdated_price_e_knownByNode_n = DoubleFactory2D.dense.make (N , E);
		for (int n = 0; n < N ; n ++) control_mostUpdated_price_e_knownByNode_n.viewRow (n).assign (control_price_e);
		
		/* Initially all nodes receive a "wake up to transmit" event, aligned at time zero or y asynchr => randomly chosen */
		for (Node n : currentNetPlan.getNodes())
		{
			final double signalingTime = (signaling_isSynchronous.getBoolean())? signaling_averageInterMessageTime.getDouble() : Math.max(0 , signaling_averageInterMessageTime.getDouble() + signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_WAKEUPTOSENDMESSAGE , n));
			final double updateTime = (update_isSynchronous.getBoolean())? update_averageInterUpdateTime.getDouble() : Math.max(0 , update_averageInterUpdateTime.getDouble() + update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_WAKEUPTOUPDATE , n));
		}

		/* Intialize the traces */
		this.stat_traceOf_xp = new TimeTrace();
		this.stat_traceOf_objFunction = new TimeTrace (); 
		this.stat_traceOf_xp.add(0.0, this.currentNetPlan.getVectorRouteCarriedTraffic());
		this.stat_traceOf_objFunction.add(0.0, computeObjectiveFucntionFromNetPlan());

		/* */
		log.println("Initial rouetrs in this.currentNetPlan.getRouteCarriedTrafficMap(): " + this.currentNetPlan.getVectorRouteCarriedTraffic());
	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		final double t = event.getEventTime();
		switch (event.getEventType())
		{
		case SIGNALING_RECEIVEDMESSAGE: // A node receives from an out neighbor the q_nt for any destination
		{
			final Pair<Node,Map<Link,Double>> signalInfo = (Pair<Node,Map<Link,Double>>) event.getEventObject();
			final Node nMe = signalInfo.getFirst();
			final Map<Link,Double> receivedInfo_price_e = signalInfo.getSecond();
			for (Link e : receivedInfo_price_e.keySet())
				this.control_mostUpdated_price_e_knownByNode_n.set (nMe.getIndex () , e.getIndex() , receivedInfo_price_e.get(e));
			log.println("t=" + t + ": SIGNALING_RECEIVEDMESSAGE node " + nMe);
			break;
		}
		
		case SIGNALING_WAKEUPTOSENDMESSAGE: // A node broadcasts signaling info to its 1 hop neighbors
		{
			final Node nMe = (Node) event.getEventObject();
			log.println("t=" + t + ": ROUTING_NODEWAKEUPTO_TRANSMITSIGNAL node " + nMe);

			/* Create the info I will signal */
			Map<Link,Double> infoToSignal_price_e = new HashMap<Link,Double> ();
			for (Link e : nMe.getOutgoingLinks())
				infoToSignal_price_e.put (e , this.computeLinkPriceFromNetPlan(e));

			/* Send the events of the signaling information messages to all the nodes */
			if (rng.nextDouble() >= this.signaling_signalingLossProbability.getDouble()) // the signaling may be lost => lost to all nodes
				for (Node n : currentNetPlan.getNodes ())
				{
					final double signalingReceptionTime = t + Math.max(0 , signaling_averageDelay.getDouble() + signaling_maxFluctuationInDelay.getDouble() * (rng.nextDouble() - 0.5));
					this.scheduleEvent(new SimEvent (signalingReceptionTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_RECEIVEDMESSAGE , Pair.of(n , infoToSignal_price_e)));
				}
			
			/* Re-schedule when to wake up again */
			final double signalingTime = signaling_isSynchronous.getBoolean()? t + signaling_averageInterMessageTime.getDouble() : Math.max(t , t + signaling_averageInterMessageTime.getDouble() + signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_WAKEUPTOSENDMESSAGE , nMe));
			break;
		}

		case UPDATE_WAKEUPTOUPDATE: // a node updates its p_n, p_e values, using most updated info available
		{
			final Node nMe = (Node) event.getEventObject();	
			log.println("t=" + t + ": ROUTING_NODEWAKEUPTO_RECOMPUTE node " + nMe);
			//log.println("p_e values Before = " + mac_p_e);
			
			//if (1 ==1) break;
			DoubleMatrix1D x_p = this.currentNetPlan.getVectorRouteCarriedTraffic();
			DoubleMatrix1D infoIKnow_price_e = this.control_mostUpdated_price_e_knownByNode_n.viewRow (nMe.getIndex ());
			for (Demand d : nMe.getOutgoingDemands())
			{
				final double h_d = d.getOfferedTraffic();
				DoubleMatrix1D new_x_p = DoubleFactory1D.dense.make (R);
				for (int rIndex : control_routeIndexes_d [d.getIndex ()])
				{
					final Route r = currentNetPlan.getRoute (rIndex);
					final double old_xp = x_p.get(rIndex);
					double accumPrices = 0;
					for (Link e : r.getSeqLinks()) accumPrices += infoIKnow_price_e.get(e.getIndex());
					accumPrices += 2*gradient_maxGradientAbsoluteNoise.getDouble()*(this.rng.nextDouble()-0.5);
					new_x_p.set (r.getIndex () , old_xp - this.gradient_gammaStep.getDouble() * accumPrices + this.gradient_heavyBallBetaParameter.getDouble() * (r.getCarriedTraffic() - this.control_previousXp.get(r.getIndex ())  ));
				}

				GradientProjectionUtils.euclideanProjection_sumEquality (new_x_p , control_routeIndexes_d [d.getIndex ()] , h_d);

				if (gradient_maxGradientCoordinateChange.getDouble() > 0)
					GradientProjectionUtils.scaleDown_maxAbsoluteCoordinateChange (x_p , new_x_p , control_routeIndexes_d [d.getIndex ()] ,  gradient_maxGradientCoordinateChange.getDouble());

				for (int rIndex : control_routeIndexes_d [d.getIndex ()])
				{
					final double newCarriedTraffic = new_x_p.get (rIndex);
					final Route r = currentNetPlan.getRoute (rIndex);
					control_previousXp.set (rIndex , r.getCarriedTraffic());
					r.setCarriedTraffic(newCarriedTraffic , newCarriedTraffic);
				}
			}

			final double updateTime = update_isSynchronous.getBoolean()? t + update_averageInterUpdateTime.getDouble() : Math.max(t , t + update_averageInterUpdateTime.getDouble() + update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_WAKEUPTOUPDATE,  nMe));

			this.stat_traceOf_xp.add(t, currentNetPlan.getVectorRouteCarriedTraffic());
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
		stat_traceOf_objFunction.printToFile(new File (simulation_outFileNameRoot.getString() + "_objFunc.txt"));
		Pair<DoubleMatrix1D,Double> optPair = computeOptimumSolution ();
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_xp.txt"), optPair.getFirst());
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_objFunc.txt"), optPair.getSecond());
		return null;
	}
	
	private double computeLinkPriceFromNetPlan (Link e)
	{
		final double y_e = e.getCarriedTraffic();
		final double u_e = e.getCapacity();
		if (u_e == 0) throw new RuntimeException ("Zero capacity in a link means");
		final double gradient = (y_e / u_e > 0.99)? 1/Math.pow(u_e * 0.01,2) : 1/Math.pow(u_e - y_e,2); 
		return gradient;
	}

	
	
	private double computeObjectiveFucntionFromNetPlan ()
	{
		double objFunc = 0;
		for (Link e : this.currentNetPlan.getLinks())
		{
			final double y_e = e.getCarriedTraffic();
			final double u_e = e.getCapacity();
			if (y_e / u_e > 0.99)
				objFunc += 1/(0.01*u_e) + 1/Math.pow(u_e * 0.01,2) * (y_e - 0.99*u_e);
			else
				objFunc += 1/(u_e - y_e);
		}
		return objFunc;
	}
	
	private Pair<DoubleMatrix1D,Double> computeOptimumSolution ()
	{
		/* Modify the map so that it is the pojection where all elements sum h_d, and are non-negative */
		final int R = this.currentNetPlan.getNumberOfRoutes();
		final int E = this.currentNetPlan.getNumberOfLinks();
		final DoubleMatrix1D h_d = this.currentNetPlan.getVectorDemandOfferedTraffic();
		final DoubleMatrix1D u_e = this.currentNetPlan.getVectorLinkCapacity();
		final DoubleMatrix1D h_p = this.currentNetPlan.getVectorRouteOfferedTrafficOfAssociatedDemand();
				
		OptimizationProblem op = new OptimizationProblem();
		op.addDecisionVariable("x_p", false , new int[] { 1 , R }, DoubleUtils.zeros(R) , h_p.toArray());
		op.addDecisionVariable("y_e", false , new int[] { 1 , E }, DoubleUtils.zeros(E) , u_e.toArray()); 
		op.setInputParameter("h_d", h_d , "row");
		op.setInputParameter("h_p", h_p , "row");
		op.setInputParameter("u_e", u_e , "row");
		
		/* Set some input parameters */
		op.setObjectiveFunction("minimize", "sum (1./(u_e - y_e))");

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
		final double optCost = op.getOptimalCost();
		DoubleMatrix1D x_p = op.getPrimalSolution("x_p").view1D();
//		Map<Long,Double> x_p_map = new HashMap<Long,Double> ();
//		for (long p : cpl.getPathIds()) x_p_map.put(p, x_p_array [id2IndexPathMap.get(p)]); 

		/* Check the solution */
		for (Link e : this.currentNetPlan.getLinks())
		{
			double y_e = 0; for (Route r : e.getTraversingRoutes()) y_e += x_p.get (r.getIndex());
			if (y_e > e.getCapacity() + 1E-3) throw new RuntimeException ("Bad");
		}
		for (Demand d : this.currentNetPlan.getDemands())
		{
			double trafDemand = 0; for (Route r : d.getRoutes()) trafDemand += x_p.get(r.getIndex ());
			if (Math.abs(trafDemand - d.getOfferedTraffic()) > 1E-3) throw new RuntimeException ("Bad");
		}
		
		return Pair.of(x_p,optCost);
	}
	

	
}
