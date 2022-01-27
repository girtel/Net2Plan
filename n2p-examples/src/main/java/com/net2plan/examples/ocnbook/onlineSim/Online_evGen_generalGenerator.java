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
import cern.jet.random.tdouble.Exponential;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.simulation.IEventGenerator;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.TrafficMatrixGenerationModels;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.RandomUtils;
import com.net2plan.utils.Triple;

import java.text.SimpleDateFormat;
import java.util.*;

/** 
 * Generates events to a technology-agnostic network, consisting of connection requests/releases and failures and repairs.
 * 
 * The events generated targeted to the event processor module (e.g. {@code Online_evProc_generalProcessor}) are: 
 * <ul>
 * <li>SimEvent.RouteAdd: To add a route to the network, associated to a given demand (the demand is seen as a source of connection requests).</li>
 * <li>SimEvent.RouteRemove: If the processor successfully creates a Route object, as a reaction to the RouteAdd event, then a RouteRemove event will be sent to the processor, to release the resources when the connection holding time ends. In the incremental model, route remove events are never sent</li>
 * <li>SimEvent.DemandModify: Sends this event to ask the processor to modify the offered traffic of a demand (recall that generators cannot modify the NetPlan object). The demand offered traffic is the average traffic of connection requests created. This generator changes it to be able to simulate fast and slow traffic fluctuations.</li>
 * <li>SimEvent.NodesAndLinksChangeFailureState: Sends these events to the processor, representing network failures and repairs to react to.</li>
 * </ul>
 * 
 * The average rate of connection requests sent can change along time using fast and/or slow fluctuations. Fast fluctuations are intended to reflect typical short time-scale
 *  traffic variations, while slow fluctuation are more suitable for representing multihour (slow) traffic fluctuations (e.g. night vs day traffic). 
 *  
 *  In the long-run simulations, connections have a finite holding time, so route add and route remove events can be sent. 
 *  With the incremental model, connections are never released, and the traffic only increases. This can be used e.g. in studies that search for the moment in 
 *  which the network needs an upgrade, since its capacity is exhausted.
 *  
 * @net2plan.keywords CAC (Connection-Admission-Control), Network recovery: protection, Network recovery: restoration
 * @net2plan.ocnbooksections Section 3.3.3, Exercise 3.7, Exercise 3.8
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Online_evGen_generalGenerator extends IEventGenerator
{
	private final static String DATE_FORMAT = "MM/dd/YY HH:mm:ss";
	
	private InputParameter _fail_failureModel = new InputParameter ("_fail_failureModel", "#select# perBidirectionalLinkBundle none SRGfromNetPlan perNode perLink perDirectionalLinkBundle" , "Failure model selection: SRGfromNetPlan, perNode, perLink, perDirectionalLinkBundle, perBidirectionalLinkBundle");
	private InputParameter _tfFast_fluctuationType = new InputParameter ("_tfFast_fluctuationType", "#select# none random-truncated-gaussian" , "");
	private InputParameter _trafficType = new InputParameter ("_trafficType", "#select# non-connection-based connection-based-longrun connection-based-incremental " , "");
	private InputParameter _tfSlow_fluctuationType = new InputParameter ("_tfSlow_fluctuationType", "#select# none time-zone-based" , "");
	private InputParameter cac_arrivalsPattern = new InputParameter ("cac_arrivalsPattern", "#select# deterministic random-exponential-arrivals-deterministic-duration random-exponential-arrivals-and-duration" , "");
	private InputParameter trafficLayerId = new InputParameter ("trafficLayerId", (long) -1 , "Layer containing traffic demands (-1 means default layer)");
	private InputParameter randomSeed = new InputParameter ("randomSeed", (long) 1 , "Seed for the random generator (-1 means random)");
	private InputParameter cac_avHoldingTimeHours = new InputParameter ("cac_avHoldingTimeHours", (double) 1 , "Default average connection duration (in seconds)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter cac_defaultConnectionSizeTrafficUnits = new InputParameter ("cac_defaultConnectionSizeTrafficUnits", (double) 1 , "Default requested traffic volume per connection" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter tfFast_timeBetweenDemandFluctuationsHours = new InputParameter ("tfFast_timeBetweenDemandFluctuationsHours", (double) 0.1 , "Average time between two changes of demand offered traffic in a demand (demands behave independently)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter tfFast_fluctuationCoefficientOfVariation = new InputParameter ("tfFast_fluctuationCoefficientOfVariation", (double) 1.0 , "Average time between two changes of demand offered traffic in a demand (demands behave independently)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter tfFast_maximumFluctuationRelativeFactor = new InputParameter ("tfFast_maximumFluctuationRelativeFactor", (double) 1.0 , "The fluctuation of a demand cannot exceed this percentage from the media" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter tfSlow_startDate = new InputParameter ("tfSlow_startDate", new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance().getTime()) , "Initial date and time of the simulation");
	private InputParameter tfSlow_timeBetweenDemandFluctuationsHours = new InputParameter ("tfSlow_timeBetweenDemandFluctuationsHours", (double) 1.0 , "Average time between two changes of demand offered traffic in a demand (demands behave independently)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter tfSlow_defaultTimezone = new InputParameter ("tfSlow_defaultTimezone", (int) 0 , "Default timezone with respect to UTC (in range [-12, 12])" , -12 , 12);
	private InputParameter fail_defaultMTTFInHours = new InputParameter ("fail_defaultMTTFInHours", (double) 10 , "Default value for Mean Time To Fail (hours) (unused when failureModel=SRGfromNetPlan)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter fail_defaultMTTRInHours = new InputParameter ("fail_defaultMTTRInHours", (double) 12 , "Default value for Mean Time To Repair (hours) (unused when failureModel=SRGfromNetPlan)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter fail_statisticalPattern = new InputParameter ("fail_statisticalPattern", "#select# exponential-iid" , "Type of failure and repair statistical pattern");

	/* demands and links do not change the number (maybe capacity, offered traffic...) */
	private Random rng;
	private DoubleMatrix1D cac_avHoldingTimeSeconds_d , cac_connectionSize_d;
	private DoubleMatrix1D currentTheoreticalOfferedTraffic_d; 
	private boolean cac_auxIATDeterministic , cac_auxIATExponential , cac_auxDurationDeterministic , cac_auxDurationExponential , cac_auxIncremental;
	private boolean isCac;
	private boolean tfFast_auxRandomGaussian;
	private DoubleMatrix1D initialOfferedTraffic_d; // the offered traffic is the sum of the two
	private DoubleMatrix1D slowChangingOfferedTraffic_d; // the offered traffic is the sum of the two
	private DoubleMatrix1D tfSlow_timeZones_n; 
	private Calendar tfSlow_calendar;
	private double tfSlow_simTimeOfLastCalendarUpdate;
	private boolean tfSlow_auxTimeZoneBased;
	private Set<Pair<SimEvent.RouteAdd,Double>> cacIncremental_potentiallyBlockedRouteRequests;
	
	private Set<SharedRiskGroup> fail_currentlyFailedSRGs;
	
	public Online_evGen_generalGenerator () { super (); }
	
	@Override
	public String getDescription()
	{
		return "Generates events to a technology-agnostic network, consisting of connection requests/releases and failures and repairs.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this , "com.net2plan.examples.ocnbook.onlineSim.Online_evGen_generalGenerator");
	}

	@Override
	public void initialize(NetPlan initialNetPlan, Map<String, String> algorithmParameters, Map<String, String> simulationParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this , "com.net2plan.examples.ocnbook.onlineSim.Online_evGen_generalGenerator" , algorithmParameters);

		NetworkLayer trafficLayer = trafficLayerId.getLong () == -1? initialNetPlan.getNetworkLayerDefault () : initialNetPlan.getNetworkLayerFromId(trafficLayerId.getLong ());
		if (trafficLayer == null) throw new Net2PlanException ("Unknown layer id");
		final int D = initialNetPlan.getNumberOfDemands(trafficLayer);
		final int N = initialNetPlan.getNumberOfNodes ();
		if (D == 0) throw new Net2PlanException("No demands were defined in the original design");

		if (randomSeed.getLong () == -1) randomSeed.initialize((long) RandomUtils.random(0, Long.MAX_VALUE - 1));
		this.rng = new Random(randomSeed.getLong ());
		this.initialOfferedTraffic_d = initialNetPlan.getVectorDemandOfferedTraffic(trafficLayer);
		this.currentTheoreticalOfferedTraffic_d = initialNetPlan.getVectorDemandOfferedTraffic(trafficLayer);
		this.isCac = (_trafficType.getString ().equalsIgnoreCase("connection-based-longrun") || _trafficType.getString ().equalsIgnoreCase("connection-based-incremental"));
		/* Initialize CAC if applicable */
		if (isCac)
		{
			this.cac_auxIATDeterministic = cac_arrivalsPattern.getString ().equalsIgnoreCase("deterministic");
			this.cac_auxIATExponential = cac_arrivalsPattern.getString ().equalsIgnoreCase("random-exponential-arrivals-deterministic-duration") || cac_arrivalsPattern.getString ().equalsIgnoreCase("random-exponential-arrivals-and-duration");
			this.cac_auxDurationDeterministic = cac_arrivalsPattern.getString ().equalsIgnoreCase("deterministic") || cac_arrivalsPattern.getString ().equalsIgnoreCase("random-exponential-arrivals-deterministic-duration");
			this.cac_auxDurationExponential = cac_arrivalsPattern.getString ().equalsIgnoreCase("random-exponential-arrivals-and-duration");
			this.cac_auxIncremental = _trafficType.getString ().equalsIgnoreCase("connection-based-incremental");
			this.cac_avHoldingTimeSeconds_d = DoubleFactory1D.dense.make (D , 0); 
			this.cac_connectionSize_d = DoubleFactory1D.dense.make (D , 0);
			this.cacIncremental_potentiallyBlockedRouteRequests = cac_auxIncremental? new HashSet<Pair<SimEvent.RouteAdd,Double>> () : null;
			for (Demand originalDemand : initialNetPlan.getDemands(trafficLayer))
			{
				final int d = originalDemand.getIndex();
				final double connectionSize = (originalDemand.getAttribute("connectionSize") != null)? Double.parseDouble(originalDemand.getAttribute("connectionSize")) : cac_defaultConnectionSizeTrafficUnits.getDouble();
				final double holdingTimeSeconds = (originalDemand.getAttribute("holdingTime") != null)? Double.parseDouble(originalDemand.getAttribute("holdingTime")) : cac_avHoldingTimeHours.getDouble() * 3600;
				final double avIATSeconds = connectionSize * holdingTimeSeconds / currentTheoreticalOfferedTraffic_d.get(d);
				final double nextInterArrivalTimeSeconds = cac_auxIATDeterministic? avIATSeconds : cac_auxIATExponential? Exponential.staticNextDouble(1/avIATSeconds) : -1;
				cac_avHoldingTimeSeconds_d.set (d,holdingTimeSeconds);
				cac_connectionSize_d.set (d,connectionSize);
				scheduleEvent(new SimEvent(nextInterArrivalTimeSeconds, SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateConnectionRequest(originalDemand)));
			}
		}
		
		/* Initialize fast changing traffic */
		this.tfFast_auxRandomGaussian = false;
		if (_tfFast_fluctuationType.getString ().equalsIgnoreCase("random-truncated-gaussian"))
		{
			this.tfFast_auxRandomGaussian = true;
			for (Demand originalDemand : initialNetPlan.getDemands(trafficLayer))
				scheduleEvent(new SimEvent(0, SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateDemandOfferedTrafficFastFluctuation(originalDemand)));
		}

		/* Initialize slow changing traffic */
		this.slowChangingOfferedTraffic_d = initialNetPlan.getVectorDemandOfferedTraffic(trafficLayer); // the offered traffic is the sum of the two
		if (_tfSlow_fluctuationType.getString ().equalsIgnoreCase("time-zone-based"))
		{
			this.tfSlow_auxTimeZoneBased = true;
			this.tfSlow_calendar = Calendar.getInstance(); 
			try { this.tfSlow_calendar.setTime(new SimpleDateFormat(DATE_FORMAT).parse(tfSlow_startDate.getString())); } catch (Exception e) { e.printStackTrace(); throw new Net2PlanException ("Error parsing the date"); }
			this.tfSlow_simTimeOfLastCalendarUpdate = 0;
			tfSlow_timeZones_n = DoubleFactory1D.dense.make (N , tfSlow_defaultTimezone.getInt());
			for(Node node : initialNetPlan.getNodes ())
			{
				if (node.getAttribute("timezone") == null) continue;
				double timezone = Double.parseDouble(node.getAttribute("timezone"));
				if (timezone < -12 || timezone > 12) throw new Net2PlanException(String.format("Timezone for node %d must be in range [-12, 12]", node.getIndex ()));
				tfSlow_timeZones_n.set(node.getIndex (), timezone);
			}
			for (Demand demand : initialNetPlan.getDemands(trafficLayer))
				scheduleEvent(new SimEvent(0.0, SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateDemandOfferedTrafficSlowFluctuation(demand)));
		}

		/* Initialize slow changing traffic */
		if (!_fail_failureModel.getString ().equalsIgnoreCase("none"))
		{
			this.fail_currentlyFailedSRGs = new HashSet<SharedRiskGroup> ();
			if (!fail_statisticalPattern.getString ().equalsIgnoreCase("exponential-iid")) throw new Net2PlanException ("Unknown failure statisitical pattern");
			switch (_fail_failureModel.getString ())
			{
				case "SRGfromNetPlan":
					break;
				case "perNode":
					SRGUtils.configureSRGs(initialNetPlan, fail_defaultMTTFInHours.getDouble(), fail_defaultMTTRInHours.getDouble(), SRGUtils.SharedRiskModel.PER_NODE, true);
					break;
				case "perLink":
					SRGUtils.configureSRGs(initialNetPlan, fail_defaultMTTFInHours.getDouble(), fail_defaultMTTRInHours.getDouble(), SRGUtils.SharedRiskModel.PER_LINK, true);
					break;
				case "perDirectionalLinkBundle":
					SRGUtils.configureSRGs(initialNetPlan, fail_defaultMTTFInHours.getDouble(), fail_defaultMTTRInHours.getDouble(), SRGUtils.SharedRiskModel.PER_DIRECTIONAL_LINK_BUNDLE, true);
					break;
				case "perBidirectionalLinkBundle":
					SRGUtils.configureSRGs(initialNetPlan, fail_defaultMTTFInHours.getDouble(), fail_defaultMTTRInHours.getDouble(), SRGUtils.SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE, true);
					break;
				default:
					throw new Net2PlanException("Failure model not valid. Please, check algorithm parameters description");
			}
			if (initialNetPlan.getNumberOfSRGs() == 0) throw new Net2PlanException("No SRGs were defined");
			for (SharedRiskGroup srg : initialNetPlan.getSRGs())
			{
				final double nextEvent = Exponential.staticNextDouble(1 / srg.getMeanTimeToFailInHours());
//				System.out.println ("nextEvent: " + nextEvent  +", srg.getMeanTimeToFailInHours(): " + srg.getMeanTimeToFailInHours());
				scheduleEvent(new SimEvent(nextEvent , SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateFailureSRG(srg)));
			}
		}


	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		final double simTime = event.getEventTime();
		Object eventObject = event.getEventObject();

		/* if a connection could not be setup, end simulation in the incremental simulation mode */
		if (this.cac_auxIncremental)
		{
			for (Pair<SimEvent.RouteAdd,Double> ev : new LinkedList<Pair<SimEvent.RouteAdd,Double>> (this.cacIncremental_potentiallyBlockedRouteRequests))
			{
				if (ev.getFirst().routeAddedToFillByProcessor != null) 
					cacIncremental_potentiallyBlockedRouteRequests.remove(ev);
				else if (ev.getSecond() < simTime) endSimulation(); // not assigned route, and it is in the past => end simulation in the incremental mode
			}
		}
		
		if (eventObject instanceof GenerateConnectionRequest)
		{
			final GenerateConnectionRequest connectionRequest = (GenerateConnectionRequest) eventObject;
			final Demand demand = connectionRequest.demand;
			final int d = demand.getIndex ();
			final double h_d = currentTheoreticalOfferedTraffic_d.get(d); // same traffic units as connection size
			final double avHoldingTimeSeconds = cac_avHoldingTimeSeconds_d.get(d);
			final double connectionSize = cac_connectionSize_d.get (d);
			final double avIATSeconds = connectionSize * avHoldingTimeSeconds / h_d;
			final double nextHoldingTimeSeconds = cac_auxDurationDeterministic? avHoldingTimeSeconds : cac_auxDurationExponential? Exponential.staticNextDouble(1/avHoldingTimeSeconds) : -1;
			final double nextInterArrivalTimeSeconds = cac_auxIATDeterministic? avIATSeconds : cac_auxIATExponential? Exponential.staticNextDouble(1/avIATSeconds) : -1;

			/* Events to the processor. RouteAdd, and if not incremental mode, route remove */
			SimEvent.RouteAdd routeInfo_add = new SimEvent.RouteAdd(demand , null , connectionSize , connectionSize);
			scheduleEvent(new SimEvent (simTime, SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , routeInfo_add));
			if (cac_auxIncremental)
				this.cacIncremental_potentiallyBlockedRouteRequests.add (Pair.of(routeInfo_add,simTime)); // to check later if it was blocked
			else
				scheduleEvent(new SimEvent(simTime + nextHoldingTimeSeconds, SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateConnectionRelease(routeInfo_add)));
			
			/* Event for me: new connection */
			scheduleEvent(new SimEvent(simTime + nextInterArrivalTimeSeconds, SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateConnectionRequest(demand)));
		}
		if (eventObject instanceof GenerateConnectionRelease)
		{
			final GenerateConnectionRelease releaseEvent = (GenerateConnectionRelease) eventObject;
//			SimEvent.DemandModify demandOfferedTrafficUpdate = new SimEvent.DemandModify(releaseEvent.routeAddEvent.demand , -releaseEvent.routeAddEvent.carriedTraffic , true);
//			scheduleEvent(new SimEvent (simTime, SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , demandOfferedTrafficUpdate));
			if (releaseEvent.routeAddEvent.routeAddedToFillByProcessor != null)
			{
				SimEvent.RouteRemove routeInfo_remove = new SimEvent.RouteRemove(releaseEvent.routeAddEvent.routeAddedToFillByProcessor);
				scheduleEvent(new SimEvent (simTime , SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , routeInfo_remove));
			}
		}
		else if (eventObject instanceof GenerateDemandOfferedTrafficFastFluctuation)
		{
			final GenerateDemandOfferedTrafficFastFluctuation trafficFluctuation = (GenerateDemandOfferedTrafficFastFluctuation) eventObject;
			final Demand demand = trafficFluctuation.demand;
			final int d = demand.getIndex ();
			final double slowChangingTrafficPart = slowChangingOfferedTraffic_d.get(d);
			if (tfFast_auxRandomGaussian)
			{
				double newFastTrafficVariation = rng.nextGaussian() * tfFast_fluctuationCoefficientOfVariation.getDouble() * slowChangingTrafficPart;
				newFastTrafficVariation = Math.max (newFastTrafficVariation , slowChangingTrafficPart * (1 - tfFast_maximumFluctuationRelativeFactor.getDouble()));
				newFastTrafficVariation = Math.min (newFastTrafficVariation , slowChangingTrafficPart * (1 + tfFast_maximumFluctuationRelativeFactor.getDouble()));
				currentTheoreticalOfferedTraffic_d.set (d , slowChangingTrafficPart + newFastTrafficVariation);
				if (!isCac) // inform the processor with a demand modified only if it is NOT cac. In CAC the sent events are the routes only, and the algorithms update the offered traffic according to it
				{
					SimEvent.DemandModify modifyEvent = new SimEvent.DemandModify(demand , Math.max (0 , slowChangingTrafficPart + newFastTrafficVariation) , false);
					scheduleEvent(new SimEvent (simTime, SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , modifyEvent));
				}
			}
			else if (_tfFast_fluctuationType.getString ().equalsIgnoreCase("none"))
			{
				throw new RuntimeException ("Bad");
			}
			else throw new Net2PlanException ("Unknow fast traffic fluctuation type: " + _tfFast_fluctuationType.getString ());
			/* Send event to me for the next fast change */
			scheduleEvent(new SimEvent(simTime + tfFast_timeBetweenDemandFluctuationsHours.getDouble()*3600 , SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateDemandOfferedTrafficFastFluctuation(demand)));
		}
		else if (eventObject instanceof GenerateDemandOfferedTrafficSlowFluctuation)
		{
			final GenerateDemandOfferedTrafficSlowFluctuation trafficFluctuation = (GenerateDemandOfferedTrafficSlowFluctuation) eventObject;
			final Demand demand = trafficFluctuation.demand;
			final int d = demand.getIndex ();
			final double currentSlowHd = slowChangingOfferedTraffic_d.get(d);
			final double currentHd = currentTheoreticalOfferedTraffic_d.get(d);
			if (tfSlow_auxTimeZoneBased)
			{
				/* Send event to processor with the demand change */
				tfSlow_calendar.add(Calendar.MILLISECOND, (int) ((simTime - tfSlow_simTimeOfLastCalendarUpdate) * 1000));
				final int hours = tfSlow_calendar.get(Calendar.HOUR_OF_DAY);
				final int minutes = tfSlow_calendar.get(Calendar.MINUTE);
				final int seconds = tfSlow_calendar.get(Calendar.SECOND);
				final int weekday = tfSlow_calendar.get(Calendar.DAY_OF_WEEK);
				final double UTC = hours + (double) minutes / 60 + (double) seconds / 3600;
				final double peakTrafficFactor = weekday == Calendar.SATURDAY || weekday == Calendar.SUNDAY ? 0.5 : 1;
				final double activityOriginNode = TrafficMatrixGenerationModels.activityFactor(UTC, tfSlow_timeZones_n.get(demand.getIngressNode().getIndex ()), 0.3, peakTrafficFactor);
				final double activityDestinationNode = TrafficMatrixGenerationModels.activityFactor(UTC, tfSlow_timeZones_n.get(demand.getEgressNode().getIndex ()), 0.3, peakTrafficFactor);
				final double activityFactorNodePair = Math.max (0 , (activityOriginNode + activityDestinationNode) / 2);
				final double newSlowFluctuationTraffic = initialOfferedTraffic_d.get(d) * activityFactorNodePair;
				final double currentFastFluctuationTraffic = currentHd - currentSlowHd;
				this.currentTheoreticalOfferedTraffic_d.set (d , newSlowFluctuationTraffic + currentFastFluctuationTraffic);
				this.slowChangingOfferedTraffic_d.set (d , newSlowFluctuationTraffic);
				if (!isCac) // inform the processor with a demand modified only if it is NOT cac. In CAC the sent events are the routes only, and the algorithms update the offered traffic according to it
				{
					SimEvent.DemandModify modifyEvent = new SimEvent.DemandModify(demand , Math.max (0 , newSlowFluctuationTraffic + currentFastFluctuationTraffic), false);
					scheduleEvent(new SimEvent (simTime, SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , modifyEvent));
				}
				tfSlow_simTimeOfLastCalendarUpdate = simTime;
			}
			else throw new Net2PlanException ("Unknow fast traffic fluctuation type: " + _tfFast_fluctuationType.getString ());
			/* Send event to me for the next fast change */
			scheduleEvent(new SimEvent(simTime + tfSlow_timeBetweenDemandFluctuationsHours.getDouble()*3600 , SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateDemandOfferedTrafficSlowFluctuation(demand)));
		}
		else if (eventObject instanceof GenerateFailureSRG)
		{
			final GenerateFailureSRG srgEvent = (GenerateFailureSRG) eventObject;
			final SharedRiskGroup srg = srgEvent.srg;
			
			/* Send event of appropriate failures to the processor (only links and nodes changing its state) */
			Set<Node> nodesUpToDown = new HashSet<Node> (currentNetPlan.getNodesUp()); nodesUpToDown.retainAll(srg.getNodes());
			Set<Link> linksUpToDown = new HashSet<Link> (currentNetPlan.getLinksUpAllLayers()); linksUpToDown.retainAll(srg.getLinksAllLayers());
			if (!nodesUpToDown.isEmpty() || !linksUpToDown.isEmpty())
			{
				SimEvent.NodesAndLinksChangeFailureState failEvent = new SimEvent.NodesAndLinksChangeFailureState (null , nodesUpToDown , null , linksUpToDown);
				scheduleEvent(new SimEvent(simTime , SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , failEvent));
			}
			/* Send repair event to myself */
			scheduleEvent(new SimEvent(simTime + Exponential.staticNextDouble(1 / srg.getMeanTimeToRepairInHours()) , SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateRepairSRG(srg)));			
			
			fail_currentlyFailedSRGs.add (srg);
		}
		else if (eventObject instanceof GenerateRepairSRG)
		{
			final GenerateRepairSRG srgEvent = (GenerateRepairSRG) eventObject;
			final SharedRiskGroup srg = srgEvent.srg;
			
			/* Send event of appropriate repairs to the processor (only links and nodes changing its state) */
			fail_currentlyFailedSRGs.remove (srg);
			Set<Node> nodesDownAfterRepair = new HashSet<Node> (); 
			Set<Link> linksDownAfterRepair = new HashSet<Link> (); 
			for (SharedRiskGroup srgStillFailed : fail_currentlyFailedSRGs)
			{
				nodesDownAfterRepair.addAll (srgStillFailed.getNodes());
				linksDownAfterRepair.addAll (srgStillFailed.getLinksAllLayers());
			}
			Set<Node> nodesDownToUp = new HashSet<Node> (currentNetPlan.getNodesDown()); nodesDownToUp.removeAll (nodesDownAfterRepair);
			Set<Link> linksDownToUp = new HashSet<Link> (currentNetPlan.getLinksDownAllLayers()); linksDownToUp.removeAll (linksDownAfterRepair);
			
			if (!nodesDownToUp.isEmpty() || !linksDownToUp.isEmpty())
			{
				SimEvent.NodesAndLinksChangeFailureState repairEvent = new SimEvent.NodesAndLinksChangeFailureState (nodesDownToUp , null , linksDownToUp , null);
				scheduleEvent(new SimEvent(simTime , SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , repairEvent));
			}
			/* Send repair event to myself */
			scheduleEvent(new SimEvent(simTime + Exponential.staticNextDouble(1 / srg.getMeanTimeToFailInHours()) , SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateFailureSRG(srg)));			
		}
	}

	
	private static class GenerateConnectionRequest
	{
		public final Demand demand;
		public GenerateConnectionRequest(Demand demand) { this.demand = demand; }
		@Override
		public String toString() { return "Generate connection request for demand " + demand.getId (); }
	}
	private static class GenerateConnectionRelease
	{
		public final SimEvent.RouteAdd routeAddEvent;
		public GenerateConnectionRelease(SimEvent.RouteAdd routeAddEvent) { this.routeAddEvent = routeAddEvent; }
		@Override
		public String toString() { return "Generate connection release for demand " + routeAddEvent.demand.getId (); }
	}
	private static class GenerateDemandOfferedTrafficFastFluctuation
	{
		public final Demand demand;
		public GenerateDemandOfferedTrafficFastFluctuation(Demand demand) { this.demand= demand; }
		@Override
		public String toString() { return "Generate fast fluctuation of offered traffic of demand " + demand.getId () ; }
	}
	private static class GenerateDemandOfferedTrafficSlowFluctuation
	{
		public final Demand demand;
		public GenerateDemandOfferedTrafficSlowFluctuation(Demand demand) { this.demand= demand; }
		@Override
		public String toString() { return "Generate slow fluctuation of offered traffic of demand " + demand.getId () ; }
	}
	private static class GenerateFailureSRG
	{
		public final SharedRiskGroup srg;
		public GenerateFailureSRG(SharedRiskGroup srg) { this.srg = srg; }
		@Override
		public String toString() { return "Generate failure in SRG " + srg.getId () ; }
	}
	private static class GenerateRepairSRG
	{
		public final SharedRiskGroup srg;
		public GenerateRepairSRG(SharedRiskGroup srg) { this.srg = srg; }
		@Override
		public String toString() { return "Generate repair event in SRG " + srg.getId () ; }
	}

}
