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


import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tint.IntFactory3D;
import cern.colt.matrix.tint.IntMatrix3D;
import cern.jet.random.tdouble.Poisson;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.TimeTrace;
import com.net2plan.utils.Triple;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

/** 
 * This module implements a distributed dual-gradient based algorithm for adapting the network routing to the one which minimizes the average number of hops, that results in a purely decentralized backpressure scheme.
 *
 * Ths event processor is adapted to permit observing the algorithm performances under user-defined conditions, 
 * including asynchronous distributed executions, where signaling can be affected by losses and/or delays, and/or measurement errors. 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec10_3_backpressureRoutingDual.m">{@code fig_sec10_3_backpressureRoutingDual.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * 
 * To simulate a network with this module, use the {@code Online_evGen_doNothing} generator.
 * 
 * @net2plan.keywords Flow assignment (FA), Distributed algorithm, Backpressure routing, Dual gradient algorithm
 * @net2plan.ocnbooksections Section 10.3
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
@SuppressWarnings("unchecked")
public class Online_evProc_backpressureRoutingDual extends IEventProcessor
{
	private static PrintStream getNulFile () { try { return new PrintStream (new FileOutputStream ("NUL") , false); } catch (Exception e) {e.printStackTrace(); throw new RuntimeException ("Not NUL file"); }   } 
	private PrintStream log = getNulFile (); //System.err;//new PrintStream (new FileOutputStream ("NUL") , true);

	private Random rng;
	private int N,E,D;
	
	private static final int SIGNALING_WAKEUPTOSENDMESSAGE = 300; // A NODE SIGNAL QUEUES TO NEIGHBOR NODES
	private static final int SIGNALING_RECEIVEDMESSAGE = 301;  // A NODE RECEIVED SIGNAL QUEUES TO NEIGHBOR NODES
	private static final int UPDATE_INITTIMESLOTFORSCHEDULINGPACKETS = 302; // ALL NODES SCHEDULE ONE PACKET PER LINK
	private static final int UPDATE_STATISTICTRACES = 303; // STAT UPDATE OF N2P

	private InputParameter signaling_isSynchronous = new InputParameter ("signaling_isSynchronous", false , "true if all the distributed agents involved wake up synchronously to send the signaling messages");
	private InputParameter signaling_averageInterMessageTime = new InputParameter ("signaling_averageInterMessageTime", 1.0 , "Average time between two signaling messages sent by an agent" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter signaling_maxFluctuationInterMessageTime = new InputParameter ("signaling_maxFluctuationInterMessageTime", 0.5 , "Max fluctuation in time between two signaling messages sent by an agent" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter signaling_averageDelay = new InputParameter ("signaling_averageDelay", 0.0 , "Average time between signaling message transmission by an agent and its reception by other or others" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter signaling_maxFluctuationInDelay = new InputParameter ("signaling_maxFluctuationInDelay", 0.0 , "Max fluctuation in time in the signaling delay, in absolute time values. The signaling delays are sampled from a uniform distribution within the given interval" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter signaling_signalingLossProbability = new InputParameter ("signaling_signalingLossProbability", 0.05 , "Probability that a signaling message transmitted is lost (not received by other or others involved agents)" , 0 , true , Double.MAX_VALUE , true);

	private InputParameter routing_fixedPacketDurationAndSchedulingInterval = new InputParameter ("routing_fixedPacketDurationAndSchedulingInterval", 1.0 , "Fixed slot size (synchronized all links)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter routing_numTrafficUnitsOfOnePacket = new InputParameter ("routing_numTrafficUnitsOfOnePacket", 1.0 , "One packet in one slot time is this traffic" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter routing_maxNumberOfN2PStatisticsIntervals = new InputParameter ("routing_maxNumberOfN2PStatisticsIntervals", (int) 50 , "Simulation length in number of scheduling intervals" , 1 , Integer.MAX_VALUE);
	private InputParameter routing_statNumSchedSlotBetweenN2PRecomputing = new InputParameter ("routing_statNumSchedSlotBetweenN2PRecomputing", (int) 100 , "Number of scheduling slots between two N2P update and storing a stat trace" , 1 , Integer.MAX_VALUE);
	private InputParameter routing_pressureDifferenceThreshold = new InputParameter ("routing_pressureDifferenceThreshold", 1.0 , "Pressure difference parameter (delta P in the book)" , 0 , false , Double.MAX_VALUE , true);

	private InputParameter gradient_gammaStep = new InputParameter ("gradient_gammaStep", 0.01 , "Gamma step in the gradient algorithm" , 0 , false , Double.MAX_VALUE , true);

	private InputParameter simulation_randomSeed = new InputParameter ("simulation_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter simulation_outFileNameRoot = new InputParameter ("simulation_outFileNameRoot", "backpressureRoutingDual" , "Root of the file name to be used in the output files. If blank, no output");

	private NetPlan currentNetPlan;

	private Map<List<Link>,Route> stat_mapSeqLinks2RouteId;
	private Map<Route,Integer> stat_mapRouteId2CarriedPacketsLastInterval;
	private int [] stat_accumNumGeneratedPackets_d;
	private int [] stat_accumNumReceivedPackets_d;
	private int [][] stat_accumNumQeueusPackets_nd;
	private double stat_totalOfferedTrafficConstant;
	
	private TimeTrace stat_traceOf_xp; // traces are taken from netPlan, when it is updated (stat_n2pRecomputingInterval)
	private TimeTrace stat_traceOf_objFunction;
	private TimeTrace stat_traceOf_ye;
	private TimeTrace stat_traceOf_queueSizes;

	private IntMatrix3D ctlMostUpdatedQndKnownByNodeN1_n1nd; // sparse for memory efficiency 
	private int [][] ctlNumPacketsQueue_nd;
	private Map<Pair<Node,Demand>,LinkedList<PacketInfo>> ctlQueue_nd; 

	private NetPlan optNetPlan;
	private double [][] optQueueSizes_nd;
	
	@Override
	public String getDescription()
	{
		return "This module implements a distributed dual-gradient based algorithm for adapting the network routing to the one which minimizes the average number of hops, that results in a purely decentralized backpressure scheme.";
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

		/* Update the indexes. The set of nodes, links and demands cannot change during the algorithm */
		this.N = this.currentNetPlan.getNumberOfNodes();
		this.E = this.currentNetPlan.getNumberOfLinks();
		this.D = this.currentNetPlan.getNumberOfDemands();
		if ((E == 0) || (D == 0)) throw new Net2PlanException ("The input design should have links and demands");
		
		/* Sets the initial routing, with all the traffic balanced equally in all the paths of the demand */
		this.stat_mapSeqLinks2RouteId = new HashMap<List<Link>,Route> ();
		this.stat_mapRouteId2CarriedPacketsLastInterval = new HashMap<Route,Integer> ();

		this.currentNetPlan.removeAllUnicastRoutingInformation();
		this.currentNetPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
		final Pair<NetPlan,double [][]> pOpt = computeOptimumSolution (false);
		final Pair<NetPlan,double [][]> pOpt01 = computeOptimumSolution (true);
		this.optNetPlan = pOpt.getFirst();
		this.optQueueSizes_nd = pOpt.getSecond();
		/* Add the routes in the optimum solution to the netPlan, with zero traffic. For having teh same IDs later */
		this.currentNetPlan.copyFrom(optNetPlan);
		for (Route r: currentNetPlan.getRoutes())
		{
			r.setCarriedTraffic(0.0 , 0.0);
			this.stat_mapSeqLinks2RouteId.put(r.getSeqLinks(),r);
			this.stat_mapRouteId2CarriedPacketsLastInterval.put(r, 0);
		}
		this.ctlQueue_nd = new HashMap<Pair<Node,Demand>,LinkedList<PacketInfo>> ();
		for (Node n: this.currentNetPlan.getNodes())
			for (Demand d: this.currentNetPlan.getDemands())
				{ ctlQueue_nd.put(Pair.of(n, d), new LinkedList<PacketInfo> ());  }

		this.ctlMostUpdatedQndKnownByNodeN1_n1nd = IntFactory3D.sparse.make(N, N, D); 
		this.ctlNumPacketsQueue_nd = new int [N][D];
		
		this.stat_accumNumGeneratedPackets_d = new int [D];
		this.stat_accumNumReceivedPackets_d = new int [D];
		this.stat_accumNumQeueusPackets_nd= new int [N][D]; 
		
		/* Initially all nodes receive a "wake up to transmit" event, aligned at time zero or y asynchr => randomly chosen */
		this.scheduleEvent(new SimEvent (routing_statNumSchedSlotBetweenN2PRecomputing.getInt() * routing_fixedPacketDurationAndSchedulingInterval.getDouble(), SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_STATISTICTRACES , -1));
		this.scheduleEvent(new SimEvent (routing_fixedPacketDurationAndSchedulingInterval.getDouble() , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_INITTIMESLOTFORSCHEDULINGPACKETS , -1));
		for (Node n : currentNetPlan.getNodes())
		{
			final double signalingTime = signaling_isSynchronous.getBoolean()? signaling_averageInterMessageTime.getDouble() : Math.max(0 , signaling_averageInterMessageTime.getDouble() + signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_WAKEUPTOSENDMESSAGE , n));
		}

		/* Intialize the traces */
		this.stat_totalOfferedTrafficConstant = this.currentNetPlan.getVectorDemandOfferedTraffic().zSum();
		this.stat_traceOf_xp = new TimeTrace ();
		this.stat_traceOf_ye = new TimeTrace ();
		this.stat_traceOf_queueSizes = new TimeTrace ();
		this.stat_traceOf_objFunction = new TimeTrace (); 
		this.stat_traceOf_xp.add(0.0 , netPlanRouteCarriedTrafficMap (this.currentNetPlan));
		this.stat_traceOf_ye.add(0.0, this.currentNetPlan.getVectorLinkCarriedTraffic());
		this.stat_traceOf_queueSizes.add(0.0, copyOf(this.ctlNumPacketsQueue_nd));
		this.stat_traceOf_objFunction.add(0.0, computeObjectiveFucntionFromNetPlan());
		
	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		final double time = event.getEventTime();
		switch (event.getEventType())
		{
		case UPDATE_STATISTICTRACES:
		{
			for (Route r : this.currentNetPlan.getRoutes())
			{
				final int numPacketsArrivedDestinationLastInterval = stat_mapRouteId2CarriedPacketsLastInterval.get(r);
				final double trafficArrivedDestinationLastInterval = numPacketsArrivedDestinationLastInterval * routing_numTrafficUnitsOfOnePacket.getDouble() / (routing_fixedPacketDurationAndSchedulingInterval.getDouble() * routing_statNumSchedSlotBetweenN2PRecomputing.getInt());
				r.setCarriedTraffic(trafficArrivedDestinationLastInterval , trafficArrivedDestinationLastInterval);

				/* Set zero in the counter of packets */
				stat_mapRouteId2CarriedPacketsLastInterval.put(r,0);
			}
			
			/* Update the traces */
			this.stat_traceOf_xp.add(time, netPlanRouteCarriedTrafficMap(this.currentNetPlan));
			this.stat_traceOf_ye.add(time, this.currentNetPlan.getVectorLinkCarriedTraffic());
			this.stat_traceOf_objFunction.add(time, computeObjectiveFucntionFromNetPlan());
			final double scaleFactorAccumNumQueuePacketsToAverageQueuedTraffic = this.routing_numTrafficUnitsOfOnePacket.getDouble() / this.routing_statNumSchedSlotBetweenN2PRecomputing.getInt();
			/* We store the average queue sizes in traffic units */
			this.stat_traceOf_queueSizes.add(time, scaledCopyOf(this.stat_accumNumQeueusPackets_nd , scaleFactorAccumNumQueuePacketsToAverageQueuedTraffic));
			this.stat_accumNumQeueusPackets_nd = new int [N][D]; // reset the count

			this.scheduleEvent(new SimEvent (time + routing_fixedPacketDurationAndSchedulingInterval.getDouble() * routing_statNumSchedSlotBetweenN2PRecomputing.getInt() , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_STATISTICTRACES , -1));

			break;
		}
		
		case SIGNALING_RECEIVEDMESSAGE: // A node receives from an out neighbor the q_nd for any demand
		{
			final Triple<Node,Node,int []> signalInfo = (Triple<Node,Node,int []>) event.getEventObject();
			final Node nMe = signalInfo.getFirst();
			final int index_nIdMe = nMe.getIndex ();
			final Node nNeighbor = signalInfo.getSecond();
			final int index_nNeighbor = nNeighbor.getIndex ();
			final int [] receivedInfo_q_d = signalInfo.getThird();
			for (int index_d = 0 ; index_d < D ; index_d ++)
				ctlMostUpdatedQndKnownByNodeN1_n1nd.set(index_nIdMe, index_nNeighbor, index_d, receivedInfo_q_d[index_d]);
			break;

		}
		
		case SIGNALING_WAKEUPTOSENDMESSAGE: // A node broadcasts signaling info to its 1 hop neighbors
		{
			final Node nMe = (Node) event.getEventObject();
			final int index_nMe = nMe.getIndex ();
			
			log.println("t=" + time + ": ROUTING_NODEWAKEUPTO_TRANSMITSIGNAL node " + nMe);

			/* Create the info I will signal */
			int [] infoToSignal_q_d = Arrays.copyOf(ctlNumPacketsQueue_nd [index_nMe], D);

			/* Send the events of the signaling information messages to incoming neighbors */
			for (Node n : nMe.getInNeighbors())
			{
				if (rng.nextDouble() >= this.signaling_signalingLossProbability.getDouble()) // the signaling may be lost => lost to all nodes
				{
					final double signalingReceptionTime = time + Math.max(0 , signaling_averageDelay.getDouble() + signaling_maxFluctuationInDelay.getDouble() * (rng.nextDouble() - 0.5));
					this.scheduleEvent(new SimEvent (signalingReceptionTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_RECEIVEDMESSAGE , Triple.of(n , nMe , infoToSignal_q_d)));
				}
			}
			
			/* Re-schedule when to wake up again */ 
			final double signalingTime = signaling_isSynchronous.getBoolean()? time + signaling_averageInterMessageTime.getDouble() : Math.max(time , time + signaling_averageInterMessageTime.getDouble() + signaling_maxFluctuationInterMessageTime.getDouble() * (rng.nextDouble() - 0.5));
			this.scheduleEvent(new SimEvent (signalingTime , SimEvent.DestinationModule.EVENT_PROCESSOR , SIGNALING_WAKEUPTOSENDMESSAGE , nMe));
			break;
		}

		case UPDATE_INITTIMESLOTFORSCHEDULINGPACKETS: // a node updates its p_n, p_e values, using most updated info available
		{
			log.println("t=" + time + ": UPDATE_INITTIMESLOTFORSCHEDULINGPACKETS");

			/* First, create traffic of the demands. A constant amount */
			for (Demand d : this.currentNetPlan.getDemands())
			{
				final double avgNumPackets_d = d.getOfferedTraffic() / routing_numTrafficUnitsOfOnePacket.getDouble();
				final int numPackets_d = Poisson.staticNextInt(avgNumPackets_d);
				final Node a_d = d.getIngressNode();
				LinkedList<PacketInfo> q = this.ctlQueue_nd.get(Pair.of(a_d, d));
				for (int cont = 0 ; cont < numPackets_d ; cont ++)
					q.add (new PacketInfo (d , time));
				this.ctlNumPacketsQueue_nd [a_d.getIndex ()][d.getIndex ()] += numPackets_d;
				this.stat_accumNumGeneratedPackets_d [d.getIndex ()] += numPackets_d;
			}

			/* Forwarding decisions for each link. Each link can transmit as many packets as its capacity divided by trafic per packet factor (floor), during this scheduling */
			int [][] numPacketsToForward_ed = new int [E][D];
			int [][] numPacketsToForward_nd = new int [N][D];
			for (int index_e = 0 ; index_e  < E ; index_e ++)
			{
				final Link e = currentNetPlan.getLink (index_e);
				final int maxNumPacketsToTransmit = (int) Math.floor(e.getCapacity() / this.routing_numTrafficUnitsOfOnePacket.getDouble());
				final Node a_e = e.getOriginNode();
				final Node b_e = e.getDestinationNode();
				final int index_ae = a_e.getIndex ();
				final int index_be = b_e.getIndex ();
				for (int cont = 0 ; cont < maxNumPacketsToTransmit ; cont ++)
				{
					/* Take the queue with higher */
					double highestBackpressure = -1;
					int indexDemandToTransmitPacket = -1;
					for (int index_d = 0 ; index_d < D ; index_d ++)
					{
						final Demand d = currentNetPlan.getDemand (index_d);
						final int thisQueueSize = ctlNumPacketsQueue_nd [index_ae][index_d];
						if (thisQueueSize != ctlQueue_nd.get(Pair.of(a_e, d)).size ()) throw new RuntimeException ("Bad"); 
						final int neighborQueueSize = ctlMostUpdatedQndKnownByNodeN1_n1nd.get(index_ae,index_be,index_d);
						final double backpressure = gradient_gammaStep.getDouble() * (thisQueueSize - numPacketsToForward_nd[index_ae][index_d] - neighborQueueSize);
						if (backpressure > highestBackpressure) { indexDemandToTransmitPacket = index_d; highestBackpressure = backpressure; } 
					}
					if (highestBackpressure <= routing_pressureDifferenceThreshold.getDouble()) break; // no more queues will be empty, not enough backpressures
					
					numPacketsToForward_ed[index_e][indexDemandToTransmitPacket] ++;
					numPacketsToForward_nd[index_ae][indexDemandToTransmitPacket] ++;
				}
			}
			/* Apply the forwarding decisions */
			for (int index_e = 0 ; index_e  < E ; index_e ++)
			{
				final Link e = currentNetPlan.getLink (index_e);
				final Node a_e = e.getOriginNode();
				final Node b_e = e.getDestinationNode();
				final int index_ae = a_e.getIndex ();
				final int index_be = b_e.getIndex ();
				for (int index_d = 0 ; index_d < D ; index_d ++)
				{
					final Demand d = currentNetPlan.getDemand (index_d);
					final int numPackets_ed = numPacketsToForward_ed [index_e][index_d];

					for (int cont = 0 ; cont < numPackets_ed ; cont ++)
					{
						PacketInfo p = this.ctlQueue_nd.get(Pair.of(a_e,d)).pop();
						ctlNumPacketsQueue_nd [index_ae][index_d] --;
						if (this.ctlQueue_nd.get(Pair.of(a_e,d)).size() != ctlNumPacketsQueue_nd [index_ae][index_d]) throw new RuntimeException ("Bad");
						p.seqLinks.add(e); // update the seq of links
						if (b_e == d.getEgressNode())
						{
							/* Packet arrived to destination: update the statistics */
							Route route = stat_mapSeqLinks2RouteId.get(p.seqLinks);
							
							/* If the route does not exist, create it */
							if (route == null) 
							{
								route = this.currentNetPlan.addRoute(d, 0.0 , 0.0 , p.seqLinks, null);
								stat_mapSeqLinks2RouteId.put(p.seqLinks , route);
								stat_mapRouteId2CarriedPacketsLastInterval.put(route, 1);
							}
							else
							{
								stat_mapRouteId2CarriedPacketsLastInterval.put(route, stat_mapRouteId2CarriedPacketsLastInterval.get(route) + 1);
								this.stat_accumNumReceivedPackets_d [index_d] ++;
							}
						}
						else
						{
							/* Not the end node => put it in the next queue */
							this.ctlQueue_nd.get(Pair.of(b_e,d)).addLast(p);
							ctlNumPacketsQueue_nd [index_be][index_d] ++;
						}
					}
				}
				
			}
			
			/* Update the statistics of accum number of packets in a queue */
			for (int index_n = 0 ; index_n < N ; index_n ++)
				for (int index_d = 0 ; index_d < D ; index_d ++)
					stat_accumNumQeueusPackets_nd [index_n][index_d] += this.ctlNumPacketsQueue_nd [index_n][index_d];
			

			this.scheduleEvent(new SimEvent (time + routing_fixedPacketDurationAndSchedulingInterval.getDouble() , SimEvent.DestinationModule.EVENT_PROCESSOR , UPDATE_INITTIMESLOTFORSCHEDULINGPACKETS , -1));
	
			if (time > this.routing_maxNumberOfN2PStatisticsIntervals.getInt() * routing_statNumSchedSlotBetweenN2PRecomputing.getInt() * this.routing_fixedPacketDurationAndSchedulingInterval.getDouble()) { this.endSimulation (); }
			break;
		}
			

		default: throw new RuntimeException ("Unexpected received event");
		}
		
		
	}

	public String finish (StringBuilder st , double simTime)
	{
		double [] avTrafficGenerated_d = new double [D];
		for (int index_d = 0; index_d < D ; index_d ++)
			avTrafficGenerated_d[index_d] = stat_accumNumGeneratedPackets_d [index_d] * routing_numTrafficUnitsOfOnePacket.getDouble() / simTime;

		/* If no output file, return */
		if (simulation_outFileNameRoot.getString().equals("")) return null;
		
		/* Compute optimum solution and cost */
		double optCost = optNetPlan.getVectorLinkCarriedTraffic().zSum() / stat_totalOfferedTrafficConstant;
		
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_objFunc.txt"), optCost);
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_xp.txt"), optNetPlan.getVectorRouteCarriedTraffic());
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_ye.txt"), optNetPlan.getVectorLinkCarriedTraffic());
		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString() + "_jom_qnd.txt"), optQueueSizes_nd);
		this.stat_traceOf_queueSizes.printToFile(new File (simulation_outFileNameRoot.getString() + "_qnd.txt"));
		this.stat_traceOf_objFunction.printToFile(new File (simulation_outFileNameRoot.getString() + "_objFunc.txt"));
		this.stat_traceOf_ye.printToFile(new File (simulation_outFileNameRoot.getString() + "_ye.txt"));
		for (Pair<Double,Object> sample : this.stat_traceOf_xp.getList())
		{
			Map<Long,Double> x_p = (Map<Long,Double>) sample.getSecond ();
			for (long r: this.currentNetPlan.getRouteIds())
				if (!x_p.containsKey(r)) x_p.put(r , 0.0); 
		}
		this.stat_traceOf_xp.printToFile(new File (simulation_outFileNameRoot.getString() + "_xp.txt"));
		return null;
	}
	
	private double computeObjectiveFucntionFromNetPlan ()
	{
		return this.currentNetPlan.getVectorLinkCarriedTraffic().zSum () / this.stat_totalOfferedTrafficConstant;
	}

	private Pair<NetPlan,double [][]> computeOptimumSolution (boolean xdeVariablesAsFractionsOfTraffic)
	{
		/* Modify the map so that it is the pojection where all elements sum h_d, and are non-negative */
		final int D = this.currentNetPlan.getNumberOfDemands();
		final int E = this.currentNetPlan.getNumberOfLinks();
		final DoubleMatrix1D h_d = this.currentNetPlan.getVectorDemandOfferedTraffic();
		final DoubleMatrix1D u_e = this.currentNetPlan.getVectorLinkCapacity();
				
		OptimizationProblem op = new OptimizationProblem();
		if (xdeVariablesAsFractionsOfTraffic)
			op.addDecisionVariable("x_de", false , new int[] { D , E }, 0 , 1);
		else
			op.addDecisionVariable("x_de", false , new int[] { D , E }, 0 , u_e.getMaxLocation() [0]);
		op.setInputParameter("h_d", h_d , "row");
		op.setInputParameter("u_e", u_e , "row");
		op.setInputParameter("deltaP", routing_pressureDifferenceThreshold.getDouble());
		
		/* Set some input parameters */
		if (xdeVariablesAsFractionsOfTraffic)
			op.setObjectiveFunction("minimize", "deltaP * sum (h_d * x_de)");
		else
			op.setObjectiveFunction("minimize", "deltaP * sum (x_de)");

		/* VECTORIAL FORM OF THE CONSTRAINTS  */
		op.setInputParameter("A_ne", this.currentNetPlan.getMatrixNodeLinkIncidence());
		op.setInputParameter("A_nd", this.currentNetPlan.getMatrixNodeDemandIncidence());
		if (xdeVariablesAsFractionsOfTraffic)
		{
			op.addConstraint("A_ne * (x_de') >= A_nd" , "flowConservationConstraints"); // the flow-conservation constraints (NxD constraints)
			op.addConstraint("h_d * x_de <= u_e"); // the capacity constraints (E constraints)
		}
		else
		{
			op.addConstraint("A_ne * (x_de') >= A_nd * diag(h_d)" , "flowConservationConstraints"); // the flow-conservation constraints (NxD constraints)
			op.addConstraint("sum(x_de,1) <= u_e"); // the capacity constraints (E constraints)
		}
		
		/* Call the solver to solve the problem */
		op.solve("cplex");

		/* If an optimal solution was not found, quit */
		if (!op.solutionIsOptimal ()) throw new RuntimeException ("An optimal solution was not found");
	
		/* Retrieve the optimum solutions. Convert the bps into Erlangs */
		final DoubleMatrix2D x_de_array = op.getPrimalSolution("x_de").view2D ();
		final double [][] q_nd_array = (double [][]) op.getMultipliersOfConstraint("flowConservationConstraints").toArray();
		
		/* Convert the x_de variables into a set of routes for each demand  */
		NetPlan np = this.currentNetPlan.copy();
		np.removeAllUnicastRoutingInformation();
		np.setRoutingFromDemandLinkCarriedTraffic(x_de_array , xdeVariablesAsFractionsOfTraffic , false);
		
		/* Check solution: all traffic is carried, no link oversubscribed */
		for (Demand d : np.getDemands()) if (d.getBlockedTraffic() > 1E-3) throw new RuntimeException ("d: " + d + ", hd: " +  d.getOfferedTraffic() + ", carried_d: " + d.getCarriedTraffic() + "... Bad");
		for (Link e : np.getLinks()) if (e.getCarriedTraffic() > e.getCapacity() + 1E-3) throw new RuntimeException ("Bad");

		/* Scale q_nd multipliers by 1/gamma factor => they become now the optimum queue sizes in the algorithm */
		double [][] optQueueSizes = new double [N][D];
		for (int i = 0 ; i < N ; i ++)
			for (int j = 0 ; j < D ; j ++)
				optQueueSizes [i][j] = q_nd_array [i][j] / this.gradient_gammaStep.getDouble();

		return Pair.of(np,optQueueSizes);
	}

	private Map<Long,Double> netPlanRouteCarriedTrafficMap (NetPlan np) { Map<Long,Double> res = new HashMap<Long,Double> (); for (Route r : np.getRoutes ()) res.put (r.getId () , r.getCarriedTraffic()); return res; }
	private class PacketInfo
	{
		final Demand d;
		final double packetCreationTime; 
		LinkedList<Link> seqLinks;
		PacketInfo (Demand d , double creationTime) { this.d = d; this.packetCreationTime = creationTime; this.seqLinks = new LinkedList<Link> (); }
	}
	private int [][] copyOf (int [][] a) { int [][] res = new int [a.length][]; for (int cont = 0 ; cont < a.length ; cont ++) res [cont] = Arrays.copyOf(a[cont], a[cont].length); return res; }
	private double [][] copyOf (double [][] a) { double [][] res = new double [a.length][]; for (int cont = 0 ; cont < a.length ; cont ++) res [cont] = Arrays.copyOf(a[cont], a[cont].length); return res; }
	private double [][] scaledCopyOf (int [][] a , double mult) { double [][] res = new double [a.length][a[0].length]; for (int cont = 0 ; cont < a.length ; cont ++) for (int cont2 = 0; cont2 < a[cont].length ; cont2 ++) res [cont][cont2] = a[cont][cont2] * mult; return res; }
}
