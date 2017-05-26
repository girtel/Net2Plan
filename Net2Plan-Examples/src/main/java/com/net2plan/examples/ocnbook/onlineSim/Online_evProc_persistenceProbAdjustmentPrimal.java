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
import com.net2plan.examples.ocnbook.offline.Offline_ca_wirelessPersistenceProbability;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.NetworkPerformanceMetrics;
import com.net2plan.libraries.WirelessUtils;
import com.net2plan.utils.GradientProjectionUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.TimeTrace;
import com.net2plan.utils.Triple;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;

/** 
 * This module implements a distributed primal-gradient based algorithm for adjusting the link persistence probabilities in a wireless network with a ALOHA-type random-access based MAC, to maximize the network utility enforcing a fair allocation of the resources.
 *
 * Ths event processor is adapted to permit observing the algorithm performances under user-defined conditions, 
 * including asynchronous distributed executions, where signaling can be affected by losses and/or delays, and/or measurement errors. 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec9_5_persitenceProbabilityAdjustmentPrimal.m">{@code fig_sec9_5_persitenceProbabilityAdjustmentPrimal.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * 
 * To simulate a network with this module, use the {@code Online_evGen_doNothing} generator.
 * 
 * @net2plan.keywords Random-access MAC, Wireless, Distributed algorithm, Primal gradient algorithm, Capacity assignment (CA)
 * @net2plan.ocnbooksections Section 9.5
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
@SuppressWarnings("unchecked")
public class Online_evProc_persistenceProbAdjustmentPrimal extends IEventProcessor
{
	private static PrintStream getNulFile () { try { return new PrintStream (new FileOutputStream ("NUL") , false); } catch (Exception e) {e.printStackTrace(); throw new RuntimeException ("Not NUL file"); }   } 
	private PrintStream log = getNulFile (); //System.err;//new PrintStream (new FileOutputStream ("NUL") , true);

	private Random rng;
	
	private static final int SIGNALING_WAKEUPTOSENDMESSAGE = 300;
	private static final int SIGNALING_RECEIVEDMESSAGE = 301;
	private static final int UPDATE_WAKEUPTOUPDATE = 302;

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
	private InputParameter gradient_gammaStep = new InputParameter ("gradient_gammaStep", 0.00001 , "Gamma step in the gradient algorithm" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter gradient_maxGradientCoordinateChange = new InputParameter ("gradient_maxGradientCoordinateChange", 0.1 , "Maximum change in an iteration of a gradient coordinate" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter simulation_randomSeed = new InputParameter ("simulation_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter simulation_outFileNameRoot = new InputParameter ("simulation_outFileNameRoot", "persistenceProbAdjustmentPrimal" , "Root of the file name to be used in the output files. If blank, no output");
	private InputParameter simulation_maxNumberOfUpdateIntervals = new InputParameter ("simulation_maxNumberOfUpdateIntervals", 100.0 , "Maximum number of update intervals in average per agent" , 0 , false , Double.MAX_VALUE , true);

	private InputParameter control_maxUeRelativeNoise = new InputParameter ("control_maxUeRelativeNoise", 0 , "Maximum relative fluctuation in link capacity estimation, that reflects into gradient noise" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter control_simultaneousTxAndRxPossible = new InputParameter ("control_simultaneousTxAndRxPossible", false , "If false, a node cannot transmit and receive simultaneously. If true, it can. This affects the interference map calculation");
	private InputParameter control_linkNominalCapacity = new InputParameter ("control_linkNominalCapacity", 1.0 , "Nominal rate of the links. If non positive, nominal rates are rates of the initial design");
	private InputParameter control_minLinkPersistenceProb = new InputParameter ("control_minLinkPersistenceProb", 0.03 , "Minimum persistence probability of a link" , 0 , true , 1 , true);
	private InputParameter control_maxNodePersistenceProb = new InputParameter ("control_maxNodePersistenceProb", 0.99 , "Maximum persistence probability of a node (summing the persistence probabilities of its outoingl links)" , 0 , true , 1 , true);
	private InputParameter control_fairnessFactor = new InputParameter ("control_fairnessFactor", 2.0 , "Fairness factor in utility function of link capacities (>= 1 to be a convex program)" , 0 , true , Double.MAX_VALUE , true);

	private NetPlan currentNetPlan , copyInitialNetPlan;
	private int E , N;
		
	private DoubleMatrix1D control_linkNominalCapacities;
	private DoubleMatrix1D control_p_e;
	private DoubleMatrix1D control_q_n;
	private DoubleMatrix2D control_mostUpdatedQn2ValueNodeKnowByNode1_n1n2; // each node is assigned a map with known p_n values of one hop neighbors and two hop neighbors
	private DoubleMatrix2D control_mostUpdatedUeValueNodeKnowByNode_ne; // each node is assigned a map with known s_e values of one hop neighbors and two hop neighbors

	private Map<Link,Set<Node>> control_nodesInterfTo_e;
	private Map<Node,Set<Link>> control_linksInterfFrom_n;
	private Map<Node,int []> control_outLinksIndexes;
	
	private TimeTrace stat_traceOf_p_e;
	private TimeTrace stat_traceOf_u_e;
	private TimeTrace stat_traceOf_objFunction;

	
	@Override
	public String getDescription()
	{
		return "This module implements a distributed primal-gradient based algorithm for adjusting the link persistence probabilities in a wireless network with a ALOHA-type random-access based MAC, to maximize the network utility enforcing a fair allocation of the resources.";
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
		this.copyInitialNetPlan = currentNetPlan.copy ();
		if (currentNetPlan.getNumberOfLayers() != 1) throw new Net2PlanException ("This algorithm works in single layer networks");
		this.N = currentNetPlan.getNumberOfNodes ();
		this.E = currentNetPlan.getNumberOfLinks ();

		if (control_linkNominalCapacity.getDouble() > 0) currentNetPlan.setVectorLinkCapacity(DoubleFactory1D.dense.make (E , control_linkNominalCapacity.getDouble()));
		control_linkNominalCapacities = currentNetPlan.getVectorLinkCapacity();
		
		this.rng = new Random (simulation_randomSeed.getLong());

		/* Check all nodes have output links */
		for (Node n : currentNetPlan.getNodes()) if (n.getOutgoingLinks().isEmpty()) throw new Net2PlanException ("All nodes should have output links");
		for (Node n : currentNetPlan.getNodes()) if (control_minLinkPersistenceProb.getDouble() * n.getOutgoingLinks().size () > control_maxNodePersistenceProb.getDouble()) throw new Net2PlanException("Minimum persistence per link is too high or maximum persistence per node too small: the problem has no feasible solutions");

		Triple<DoubleMatrix2D, Map<Link,Set<Node>> , Map<Node,Set<Link>>> interfMap = WirelessUtils.computeInterferenceMap(currentNetPlan.getNodes () , currentNetPlan.getLinks () , control_simultaneousTxAndRxPossible.getBoolean());
		this.control_nodesInterfTo_e = interfMap.getSecond ();
		this.control_linksInterfFrom_n = interfMap.getThird();
		
		this.control_outLinksIndexes = new HashMap<Node,int []> ();
		for (Node n : currentNetPlan.getNodes ()) 
		{ 
			int [] indexes = new int [n.getOutgoingLinks().size ()]; 
			int counter = 0; for (Link e : n.getOutgoingLinks()) indexes [counter ++] = e.getIndex (); 
			control_outLinksIndexes.put (n , indexes);
		}
		
		
		/* Initialize the persistence probabilities: p_e to the minimum */
		this.control_q_n = DoubleFactory1D.dense.make (N);
		this.control_p_e = DoubleFactory1D.dense.make (E , control_minLinkPersistenceProb.getDouble());
		for (Node n : currentNetPlan.getNodes ()) 
			this.control_q_n.set(n.getIndex () , control_minLinkPersistenceProb.getDouble() *  n.getOutgoingLinks().size ());
		control_mostUpdatedUeValueNodeKnowByNode_ne = DoubleFactory2D.dense.make (N,E);
		control_mostUpdatedQn2ValueNodeKnowByNode1_n1n2 = DoubleFactory2D.dense.make (N,N);

		/* Initialize the most updated signaled values known of q_n: one and two hop neighbors */
		for (Node n1 : currentNetPlan.getNodes())
		{ 
			for (Node nNeighborOneHop : n1.getOutNeighbors())
			{
				control_mostUpdatedQn2ValueNodeKnowByNode1_n1n2.set (n1.getIndex () , nNeighborOneHop.getIndex () , control_q_n.get (nNeighborOneHop.getIndex ()));
				for (Node nNeighborTwoHops : nNeighborOneHop.getOutNeighbors())
					if (nNeighborTwoHops != n1) 
						control_mostUpdatedQn2ValueNodeKnowByNode1_n1n2.set (n1.getIndex () , nNeighborTwoHops.getIndex () , control_q_n.get (nNeighborTwoHops.getIndex ()));
			}
		}
		
		/* Set link capacities to its actual values */
		for (Link e : this.currentNetPlan.getLinks ()) 
		{
			/* compute link capacity */
			double u_e = this.control_linkNominalCapacities.get(e.getIndex ()) * this.control_p_e.get(e.getIndex ());
			for (Node n : this.control_nodesInterfTo_e.get(e))
				u_e *= Math.max(0 , 1 - this.control_q_n.get(n.getIndex ()));
			e.setAttribute("p_e" , "" + control_p_e.get(e.getIndex ())); 
			e.setCapacity(u_e); 
		}		

		/* Set the most updated values known in each node, from the true u_e values */
		DoubleMatrix1D noisyUeEstimation_e = DoubleFactory1D.dense.make (E);
		for (Link e : currentNetPlan.getLinks())
			noisyUeEstimation_e.set(e.getIndex (), e.getCapacity() * ( 1 + control_maxUeRelativeNoise.getDouble()*2*(rng.nextDouble()-0.5)));
		for (Node n : currentNetPlan.getNodes())
		{ 
			for (Link e : this.control_linksInterfFrom_n.get(n)) control_mostUpdatedUeValueNodeKnowByNode_ne.set (n.getIndex () , e.getIndex () , noisyUeEstimation_e.get(e.getIndex ()));
			for (Link e : n.getOutgoingLinks()) control_mostUpdatedUeValueNodeKnowByNode_ne.set (n.getIndex () , e.getIndex () , noisyUeEstimation_e.get(e.getIndex ()));
			for (Link e : n.getIncomingLinks()) control_mostUpdatedUeValueNodeKnowByNode_ne.set (n.getIndex () , e.getIndex () , noisyUeEstimation_e.get(e.getIndex ()));
		}
		
		/* Initially all nodes receive a "wake up to transmit" event, aligned at time zero or y asynchr => randomly chosen */
		for (Node n : currentNetPlan.getNodes())
		{
			final double signalingTime = (signaling_isSynchronous.getBoolean())? signaling_averageInterMessageTime.getDouble() : Math.max(0 , signaling_averageInterMessageTime.getDouble() + signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_WAKEUPTOSENDMESSAGE , n));
			final double updateTime = (update_isSynchronous.getBoolean())? update_averageInterUpdateTime.getDouble() : Math.max(0 , update_averageInterUpdateTime.getDouble() + update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_WAKEUPTOUPDATE , n));
		}

		/* Intialize the traces */
		this.stat_traceOf_p_e = new TimeTrace ();
		this.stat_traceOf_u_e = new TimeTrace ();
		this.stat_traceOf_objFunction = new TimeTrace ();
		this.stat_traceOf_p_e.add(0.0, control_p_e.copy());
		this.stat_traceOf_u_e.add(0.0, currentNetPlan.getVectorLinkCapacity());
		this.stat_traceOf_objFunction.add(0.0 , NetworkPerformanceMetrics.alphaUtility(currentNetPlan.getVectorLinkCapacity() , control_fairnessFactor.getDouble()));
	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		final double t = event.getEventTime();
		switch (event.getEventType())
		{
		case SIGNALING_RECEIVEDMESSAGE: // A node broadcasts signaling info to its 1 hop neighbors
		{
			final Triple<Node,Map<Node,Double>,Map<Link,Double>> signalInfo = (Triple<Node,Map<Node,Double>,Map<Link,Double>>) event.getEventObject();
			final Node nMe = signalInfo.getFirst();
			final Map<Node,Double> receivedInfo_qn = signalInfo.getSecond();
			final Map<Link,Double> receivedInfo_ue = signalInfo.getThird();
			/* Take the p_n values signaled except my own p_n (I prefer my own estimation of q_n, than something estimated by others) */
			for (Entry<Node,Double> nodeInfo : receivedInfo_qn.entrySet())
				if (nodeInfo.getKey() != nMe) control_mostUpdatedQn2ValueNodeKnowByNode1_n1n2.set (nMe.getIndex () , nodeInfo.getKey().getIndex () , nodeInfo.getValue());
			/* Take the u_e values signaled */
			for (Entry<Link,Double> linkInfo : receivedInfo_ue.entrySet())
				control_mostUpdatedUeValueNodeKnowByNode_ne.set (nMe.getIndex () , linkInfo.getKey().getIndex () , linkInfo.getValue());
			break;
		}
		
		case SIGNALING_WAKEUPTOSENDMESSAGE: // A node broadcasts signaling info to its 1 hop neighbors
		{
			final Node nMe = (Node) event.getEventObject();
			final DoubleMatrix1D infoIKnow_qn = control_mostUpdatedQn2ValueNodeKnowByNode1_n1n2.viewRow (nMe.getIndex ());
			infoIKnow_qn.set (nMe.getIndex () , control_q_n.get (nMe.getIndex ()));

			/* Create the info I will signal */
			Map<Node,Double> infoToSignal_qn = new HashMap<Node,Double> ();
			infoToSignal_qn.put(nMe, this.control_q_n.get (nMe.getIndex ()));
			for (Node inNeighbor : nMe.getInNeighbors()) 
				infoToSignal_qn.put(inNeighbor, infoIKnow_qn.get(inNeighbor.getIndex ()));
			Map<Link,Double> infoToSignal_ue = new HashMap<Link,Double> ();
			for (Link inLink : nMe.getIncomingLinks()) 
				infoToSignal_ue.put(inLink, inLink.getCapacity() * ( 1 + 2*this.control_maxUeRelativeNoise.getDouble()*(rng.nextDouble() - 0.5)  ) );

			/* Send the events of the signaling information messages to the neighbors */
			if (rng.nextDouble() >= this.signaling_signalingLossProbability.getDouble()) // the signaling may be lost => lost to all nodes
				for (Node outNeighbor : nMe.getOutNeighbors())
				{
					final double signalingReceptionTime = t + Math.max(0 , signaling_averageDelay.getDouble() + signaling_maxFluctuationInDelay.getDouble() * (rng.nextDouble() - 0.5));
					this.scheduleEvent(new SimEvent (signalingReceptionTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_RECEIVEDMESSAGE , Triple.of(outNeighbor, infoToSignal_qn , infoToSignal_ue)));
				}
			
			/* Re-schedule when to wake up again */
			final double signalingTime = signaling_isSynchronous.getBoolean()? t + signaling_averageInterMessageTime.getDouble() : Math.max(t , t + signaling_averageInterMessageTime.getDouble() + signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_WAKEUPTOSENDMESSAGE , nMe));
			break;
		}

		case UPDATE_WAKEUPTOUPDATE: // a node updates its p_n, p_e values, using most updated info available
		{
			final Node nMe = (Node) event.getEventObject();	
			
			final DoubleMatrix1D infoIKnow_qn = control_mostUpdatedQn2ValueNodeKnowByNode1_n1n2.viewRow (nMe.getIndex ());
			infoIKnow_qn.set (nMe.getIndex () , control_q_n.get (nMe.getIndex ()));
			DoubleMatrix1D infoIKnow_ue = control_mostUpdatedUeValueNodeKnowByNode_ne.viewRow (nMe.getIndex ());
			DoubleMatrix1D newMacValues_e = DoubleFactory1D.dense.make (E);
			for (int eIndex : control_outLinksIndexes.get (nMe))
			{
				final Link e = currentNetPlan.getLink (eIndex);
				final double current_pe = this.control_p_e.get(eIndex);
				final double gradientThisLink = computeGradient (e , this.control_fairnessFactor.getDouble() , current_pe , infoIKnow_qn , infoIKnow_ue) + 2*gradient_maxGradientAbsoluteNoise.getDouble()*(rng.nextDouble()-0.5);
				newMacValues_e.set(eIndex, current_pe + gradient_gammaStep.getDouble() * gradientThisLink);
			}
			GradientProjectionUtils.euclideanProjection_sumInequality(newMacValues_e , control_outLinksIndexes.get (nMe) , control_minLinkPersistenceProb.getDouble() , control_maxNodePersistenceProb.getDouble());
			
			if (gradient_maxGradientCoordinateChange.getDouble() > 0)
				GradientProjectionUtils.scaleDown_maxAbsoluteCoordinateChange (control_p_e , newMacValues_e , control_outLinksIndexes.get (nMe) ,  gradient_maxGradientCoordinateChange.getDouble());
			for (int eIndex : control_outLinksIndexes.get (nMe)) this.control_p_e.set (eIndex , newMacValues_e.get (eIndex));
			
			/* update p_n value applied by me */
			double thisNode_pn = 0; for (Link e : nMe.getOutgoingLinks()) thisNode_pn += this.control_p_e.get(e.getIndex ());
			if ((thisNode_pn <= 0) || (thisNode_pn > 1 + 1E-3)) throw new RuntimeException ("Bad");
			this.control_q_n.set(nMe.getIndex (), thisNode_pn);
			
			/* Send event next recomputing time */
			final double updateTime = update_isSynchronous.getBoolean()? t + update_averageInterUpdateTime.getDouble() : Math.max(t , t + update_averageInterUpdateTime.getDouble() + update_maxFluctuationInterUpdateTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (updateTime , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_WAKEUPTOUPDATE,  nMe));

			/* Set link capacities to its actual values, update n2p object */
			for (Link e : currentNetPlan.getLinks ())
			{ 
				/* compute link capacity */
				double u_e = this.control_linkNominalCapacities.get(e.getIndex ()) * this.control_p_e.get(e.getIndex ());
				for (Node n : this.control_nodesInterfTo_e.get(e))
					u_e *= Math.max(0 , 1 - this.control_q_n.get(n.getIndex ()));
				e.setAttribute("p_e" , "" + control_p_e.get(e.getIndex ())); 
				e.setCapacity(u_e); 
			}		
			for (Node n : currentNetPlan.getNodes ()) { n.setAttribute("q_n" , "" + control_q_n.get(n.getIndex ())); }		

			checkCapacitiesNetPlan ();
			
			this.stat_traceOf_p_e.add(t, control_p_e.copy ());
			this.stat_traceOf_u_e.add(t, this.currentNetPlan.getVectorLinkCapacity());
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
		param.put("alphaFairnessFactor", "" + this.control_fairnessFactor.getDouble());
		param.put("minLinkPersistenceProb", "" + this.control_minLinkPersistenceProb.getDouble());
		param.put("maxNodePersistenceProb", "" + this.control_maxNodePersistenceProb.getDouble());
		param.put("linkNominalCapacity", "" + this.control_linkNominalCapacity.getDouble());
		param.put("simultaneousTxAndRxPossible", "" + this.control_simultaneousTxAndRxPossible.getBoolean());
		param.put("solverName", "ipopt");
		param.put("solverLibraryName", "");
		param.put("maxSolverTimeInSeconds", "-1");
//		private InputParameter solverName = new InputParameter ("solverName", "#select# ipopt", "The solver name to be used by JOM. ");
//		private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", Configuration.getOption("glpkSolverLibraryName") , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
//		private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
		new Offline_ca_wirelessPersistenceProbability ().executeAlgorithm(copyInitialNetPlan , param , null);
		final double optimumNetUtilityJOM = NetworkPerformanceMetrics.alphaUtility(copyInitialNetPlan.getVectorLinkCapacity() , control_fairnessFactor.getDouble());
		DoubleMatrix1D optimum_ue = copyInitialNetPlan.getVectorLinkCapacity();
		DoubleMatrix1D optimum_pe = DoubleFactory1D.dense.make (E);
		for (Link e : copyInitialNetPlan.getLinks ()) optimum_pe.set (e.getIndex () , Double.parseDouble (e.getAttribute("p_e")));
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_ue.txt"), optimum_ue);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_pe.txt"), optimum_pe);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_objFunc.txt"), optimumNetUtilityJOM);
		return null;
	}
	
	private double computeGradient (Link thisLink , double fairnessFactor , double thisLink_pe , DoubleMatrix1D infoIKnow_p_n , DoubleMatrix1D infoIKnow_ue)
	{
		final Node a_e = thisLink.getOriginNode();
		if (thisLink_pe == 0) throw new RuntimeException ("Link " + thisLink + " has zero p_e");
		double gradient = Math.pow(infoIKnow_ue.get(thisLink.getIndex ()) , 1 - fairnessFactor) / thisLink_pe; 
		for (Link e : this.control_linksInterfFrom_n.get(a_e))
		{
			if (e.getOriginNode() == thisLink.getOriginNode()) throw new RuntimeException ("Bad");
			gradient -= Math.pow(infoIKnow_ue.get(e.getIndex ()),1-fairnessFactor) / (1 - infoIKnow_p_n.get(e.getOriginNode().getIndex ()));
		}
		if (Double.isNaN(gradient)) throw new RuntimeException ("Link " + thisLink + " gradient is Nan");
		if (Double.isInfinite(gradient)) throw new RuntimeException ("Link " + thisLink + " gradient is inifinte");
		return gradient;
	}

	private void checkCapacitiesNetPlan ()
	{
		double [] p_e = new double [E]; 
		for (Link e : currentNetPlan.getLinks ()) { p_e [e.getIndex ()] = Double.parseDouble (e.getAttribute("p_e")); if ((p_e [e.getIndex ()] < control_minLinkPersistenceProb.getDouble() - 1E-3) ||(p_e [e.getIndex ()] > 1E-3 + control_maxNodePersistenceProb.getDouble())) throw new RuntimeException ("Bad"); }
		double [] q_n = new double [N]; 
		for (Node n : currentNetPlan.getNodes ()) 
		{
			for (Link e : n.getOutgoingLinks()) q_n [n.getIndex ()] += p_e [e.getIndex ()];
			if (Math.abs (q_n [n.getIndex ()] - Double.parseDouble (n.getAttribute("q_n"))) > 1E-3) throw new RuntimeException ("Bad");
			if (q_n [n.getIndex ()] > control_maxNodePersistenceProb.getDouble() + 1E-3) throw new RuntimeException ("Bad");
		}
		for (Link e : currentNetPlan.getLinks ())
		{
			final double u_e = e.getCapacity();
			double supposedCapacity = control_linkNominalCapacities.get(e.getIndex ()) * control_p_e.get (e.getIndex ());
			for (Node n : control_nodesInterfTo_e.get(e)) supposedCapacity *= 1 - q_n [n.getIndex ()];
			if (Math.abs (u_e - supposedCapacity) > 1e-3) throw new RuntimeException ("Bad");
		}
	}
	
}
