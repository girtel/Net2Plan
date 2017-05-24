package com.net2plan.examples.general.onlineSim;
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


import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.jet.random.tdouble.Exponential;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.simulation.IEventGenerator;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.TrafficMatrixGenerationModels;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.utils.*;

import java.text.SimpleDateFormat;
import java.util.*;

/** 
 * Generates events for a WDM network carrying lightpaths in a fixed or flexi grid of wavelengths
 * 
 * <p>The design follows the assumptions described in {@link com.net2plan.libraries.WDMUtils WDMUtils} Net2Plan library</p>
 * <p>The events generated targeted to the event processor module (e.g. {@link com.net2plan.examples.general.onlineSim.Online_evProc_wdm Online_evProc_wdm}) are:</p> 
 * <ul>
 * <li>WDMUtils.LightpathAdd: Request to add a new lightpath to the network. In this case, the lightpath is associated to an existing demand, which generates lightpath requests. 
 * The line rates of the requested lightpaths are chosen among a user-defined set of possible line rates.</li>
 * <li>WDMUtils.LightpathRemove: Removes the corresponding lightpath, releasing the resources. This occurs if the generator is configured to generate on-demand lightpath connection requests, and the increemntal mode is NOT activated (in the incremental model, lightpaths are setup, and necever released, so traffic always increments).</li>
 * <li>SimEvent.DemandModify: Modifies the offered traffic of a demand (here a demand is a source that generates lightpath requests).</li>
 * <li>SimEvent.NodesAndLinksChangeFailureState: Sends these events to the processor, representing network failures and repairs to react to.</li>
 * </ul>
 * The average rate of lightpath requests sent can change along time using fast and/or slow fluctuations. Fast fluctuations are 
 * intended to reflect typical short time-scale traffic variations, while slow fluctuation are more suitable for representing 
 * multihour (slow) traffic fluctuations (e.g. night vs day traffic). 
 *  
 *  In the long-run simulations, lightpath requests and removal are sent. This is used to simulate the operation of a lightpath-on-demand service. 
 *  With the incremental model, lightpaths are never released, and the traffic only increases. This can be used e.g. in studies that search for the moment in 
 *  which the network needs an upgrade, since its capacity is exhausted.
 *  
 * See the technology conventions used in Net2Plan built-in algorithms and libraries to represent WDM networks. 
 * @net2plan.keywords WDM, Network recovery: protection, Network recovery: restoration, Multihour optimization
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class Online_evGen_wdm extends IEventGenerator
{
	private final static String DATE_FORMAT = "MM/dd/YY HH:mm:ss";
	
	private InputParameter _fail_failureModel = new InputParameter ("_fail_failureModel", "#select# perBidirectionalLinkBundle none SRGfromNetPlan perNode perLink perDirectionalLinkBundle" , "Failure model selection: SRGfromNetPlan, perNode, perLink, perDirectionalLinkBundle, perBidirectionalLinkBundle");
	private InputParameter _tfFast_fluctuationType = new InputParameter ("_tfFast_fluctuationType", "#select# none random-truncated-gaussian" , "");
	private InputParameter _trafficType = new InputParameter ("_trafficType", "#select# connection-based-longrun connection-based-incremental " , "");
	private InputParameter _tfSlow_fluctuationType = new InputParameter ("_tfSlow_fluctuationType", "#select# none time-zone-based" , "");
	private InputParameter cac_arrivalsPattern = new InputParameter ("cac_arrivalsPattern", "#select# deterministic random-exponential-arrivals-deterministic-duration random-exponential-arrivals-and-duration" , "");
	private InputParameter trafficLayerId = new InputParameter ("trafficLayerId", (long) -1 , "Layer containing traffic demands (-1 means default layer)");
	private InputParameter randomSeed = new InputParameter ("randomSeed", (long) 1 , "Seed for the random generator (-1 means random)");
	private InputParameter cac_avHoldingTimeHours = new InputParameter ("cac_avHoldingTimeHours", (double) 1 , "Default average connection duration (in seconds)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter tfFast_timeBetweenDemandFluctuationsHours = new InputParameter ("tfFast_timeBetweenDemandFluctuationsHours", (double) 0.1 , "Average time between two changes of demand offered traffic in a demand (demands behave independently)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter tfFast_fluctuationCoefficientOfVariation = new InputParameter ("tfFast_fluctuationCoefficientOfVariation", (double) 1.0 , "Average time between two changes of demand offered traffic in a demand (demands behave independently)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter tfFast_maximumFluctuationRelativeFactor = new InputParameter ("tfFast_maximumFluctuationRelativeFactor", (double) 1.0 , "The fluctuation of a demand cannot exceed this percentage from the media" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter tfSlow_startDate = new InputParameter ("tfSlow_startDate", new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance().getTime()) , "Initial date and time of the simulation");
	private InputParameter tfSlow_timeBetweenDemandFluctuationsHours = new InputParameter ("tfSlow_timeBetweenDemandFluctuationsHours", (double) 1.0 , "Average time between two changes of demand offered traffic in a demand (demands behave independently)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter tfSlow_defaultTimezone = new InputParameter ("tfSlow_defaultTimezone", (int) 0 , "Default timezone with respect to UTC (in range [-12, 12])" , -12 , 12);
	private InputParameter fail_defaultMTTFInHours = new InputParameter ("fail_defaultMTTFInHours", (double) 10 , "Default value for Mean Time To Fail (hours) (unused when failureModel=SRGfromNetPlan)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter fail_defaultMTTRInHours = new InputParameter ("fail_defaultMTTRInHours", (double) 12 , "Default value for Mean Time To Repair (hours) (unused when failureModel=SRGfromNetPlan)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter fail_statisticalPattern = new InputParameter ("fail_statisticalPattern", "#select# exponential-iid" , "Type of failure and repair statistical pattern");
	private InputParameter lineRatesPerLightpath_Gbps = new InputParameter ("lineRatesPerLightpath_Gbps", "40 0.5 ; 100 0.5" , "Pairs of the form line-rate-Gbps SPACE probability, where probability stands for the chances of requesting a lightpath of such rate. Pairs are separated among them by character \";\" ");

	/* demands and links do not change the number (maybe capacity, offered traffic...) */
	private Random rng;
	private DoubleMatrix1D cac_avHoldingTimeHours_d , cac_avConnectionSize_d;
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
	private double [] probabilitiesLineRates_t;
	private double [] accumProbabilitiesLineRates_t;
	private double [] lineRatesGbps_t;
	private Set<Pair<WDMUtils.LightpathAdd,Double>> cacIncremental_potentiallyBlockedRouteRequests;
	
	private Set<SharedRiskGroup> fail_currentlyFailedSRGs;
	
	@Override
	public String getDescription()
	{
		return "Generates events for a WDM network carrying lightpaths in a fixed grid of wavelengths";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	@Override
	public void initialize(NetPlan initialNetPlan, Map<String, String> algorithmParameters, Map<String, String> simulationParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		NetworkLayer trafficLayer = trafficLayerId.getLong () == -1? initialNetPlan.getNetworkLayerDefault () : initialNetPlan.getNetworkLayerFromId(trafficLayerId.getLong ());
		if (trafficLayer == null) throw new Net2PlanException ("Unknown layer id");
		final int D = initialNetPlan.getNumberOfDemands(trafficLayer);
		final int N = initialNetPlan.getNumberOfNodes ();
		if (D == 0) throw new Net2PlanException("No demands were defined in the original design");

		/* Initialize the transponders information: the line rates of the lightpaths that will be requested */
		final String [] lineRateInfo_t = StringUtils.split(lineRatesPerLightpath_Gbps.getString() , ";");
		final int T = lineRateInfo_t.length;
		if (T == 0) throw new Net2PlanException ("The number of line rates defined cannot be zero");
		this.accumProbabilitiesLineRates_t = new double [T];
		this.lineRatesGbps_t = new double [T];
		this.probabilitiesLineRates_t = new double [T];
		double sumProbabilities = 0;
		for (int t = 0; t < T ; t ++)
		{
			double [] vals = StringUtils.toDoubleArray(StringUtils.split(lineRateInfo_t [t]));
			this.lineRatesGbps_t [t] = vals [0]; if (lineRatesGbps_t [t] <= 0) throw new Net2PlanException ("The line rate of a lightpath must be positive");
			this.probabilitiesLineRates_t [t] = vals [1]; if (vals [1] < 0) throw new Net2PlanException ("Occurrence probablities of the line rates cannot be negative");
			sumProbabilities += vals [1];
		}
		if (sumProbabilities == 0) Arrays.fill(probabilitiesLineRates_t , 1.0/T);
		else for (int t = 0; t < T ; t ++) probabilitiesLineRates_t [t] /= sumProbabilities;
		for (int t = 0 ; t < T ; t ++) accumProbabilitiesLineRates_t [t] = t == 0? probabilitiesLineRates_t [0] : probabilitiesLineRates_t [t] + accumProbabilitiesLineRates_t [t-1];
		if (Math.abs(accumProbabilitiesLineRates_t [T-1] - 1) > 1e-3) throw new RuntimeException ("Bad");
		
		/* More initializations */
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
			this.cac_avHoldingTimeHours_d = DoubleFactory1D.dense.make (D , 0); 
			this.cac_avConnectionSize_d = DoubleFactory1D.dense.make (D , 0);
			this.cacIncremental_potentiallyBlockedRouteRequests = cac_auxIncremental? new HashSet<Pair<WDMUtils.LightpathAdd,Double>> () : null;
			for (Demand originalDemand : initialNetPlan.getDemands(trafficLayer))
			{
				final int d = originalDemand.getIndex();
				final double averageConnectionSize = DoubleUtils.scalarProduct(lineRatesGbps_t , probabilitiesLineRates_t);
				final double holdingTime = (originalDemand.getAttribute("holdingTime") != null)? Double.parseDouble(originalDemand.getAttribute("holdingTime")) : cac_avHoldingTimeHours.getDouble();
				final double avIATHours = averageConnectionSize * holdingTime / currentTheoreticalOfferedTraffic_d.get(d);
				final double nextInterArrivalTime = cac_auxIATDeterministic? avIATHours : cac_auxIATExponential? Exponential.staticNextDouble(1/avIATHours) : -1;
				cac_avHoldingTimeHours_d.set (d,holdingTime);
				cac_avConnectionSize_d.set (d,averageConnectionSize);
				scheduleEvent(new SimEvent(nextInterArrivalTime, SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateConnectionRequest(originalDemand)));
			}
		}
		
		/* Initialize fast changing traffic */
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
			for (Pair<WDMUtils.LightpathAdd,Double> ev : new LinkedList<Pair<WDMUtils.LightpathAdd,Double>> (this.cacIncremental_potentiallyBlockedRouteRequests))
			{
				if (ev.getFirst().lpAddedToFillByProcessor != null) 
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
			final double avHoldingTimeHours = cac_avHoldingTimeHours_d.get(d);
			final double connectionSize = cac_avConnectionSize_d.get (d);
			final double avIATHours = connectionSize * avHoldingTimeHours / h_d;
			final double nextHoldingTime = cac_auxDurationDeterministic? avHoldingTimeHours : cac_auxDurationExponential? Exponential.staticNextDouble(1/avHoldingTimeHours) : -1;
			final double nextInterArrivalTime = cac_auxIATDeterministic? avIATHours : cac_auxIATExponential? Exponential.staticNextDouble(1/avIATHours) : -1;
			final double lineRateThisLpGbps = randomPick (lineRatesGbps_t , probabilitiesLineRates_t);
			
			/* Events to the processor. RouteAdd, and if not incremental mode, route remove */
			WDMUtils.LightpathAdd routeInfo_add = new WDMUtils.LightpathAdd(demand , lineRateThisLpGbps);
			scheduleEvent(new SimEvent (simTime, SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , routeInfo_add));
			if (cac_auxIncremental)
				this.cacIncremental_potentiallyBlockedRouteRequests.add (Pair.of(routeInfo_add,simTime)); // to check later if it was blocked
			else
				scheduleEvent(new SimEvent(simTime + nextHoldingTime, SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateConnectionRelease(routeInfo_add)));
			
			/* Event for me: new connection */
			scheduleEvent(new SimEvent(simTime + nextInterArrivalTime, SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateConnectionRequest(demand)));
		}
		if (eventObject instanceof GenerateConnectionRelease)
		{
			final GenerateConnectionRelease releaseEvent = (GenerateConnectionRelease) eventObject;
//			SimEvent.DemandModify demandOfferedTrafficUpdate = new SimEvent.DemandModify(releaseEvent.routeAddEvent.demand , -releaseEvent.routeAddEvent.carriedTraffic , true);
//			scheduleEvent(new SimEvent (simTime, SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , demandOfferedTrafficUpdate));
			if (releaseEvent.routeAddEvent.lpAddedToFillByProcessor != null)
			{
				WDMUtils.LightpathRemove routeInfo_remove = new WDMUtils.LightpathRemove(releaseEvent.routeAddEvent.lpAddedToFillByProcessor);
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
			scheduleEvent(new SimEvent(simTime + tfFast_timeBetweenDemandFluctuationsHours.getDouble() , SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateDemandOfferedTrafficFastFluctuation(demand)));
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
				currentTheoreticalOfferedTraffic_d.set (d , newSlowFluctuationTraffic + currentFastFluctuationTraffic);
				if (!isCac) // inform the processor with a demand modified only if it is NOT cac. In CAC the sent events are the routes only, and the algorithms update the offered traffic according to it
				{
					SimEvent.DemandModify modifyEvent = new SimEvent.DemandModify(demand , Math.max (0 , newSlowFluctuationTraffic + currentFastFluctuationTraffic), false);
					scheduleEvent(new SimEvent (simTime, SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , modifyEvent));
				}
				tfSlow_simTimeOfLastCalendarUpdate = simTime;
			}
			else throw new Net2PlanException ("Unknow fast traffic fluctuation type: " + _tfFast_fluctuationType.getString ());
			/* Send event to me for the next fast change */
			scheduleEvent(new SimEvent(simTime + tfSlow_timeBetweenDemandFluctuationsHours.getDouble() , SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateDemandOfferedTrafficSlowFluctuation(demand)));
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

	private double randomPick (double [] vals , double [] accumProbs)
	{
		final double x = rng.nextDouble();
		for (int cont = 0 ; cont < vals.length-1 ; cont ++)
			if (accumProbs [cont] < x) return vals [cont];
		return vals [vals.length - 1];
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
		public final WDMUtils.LightpathAdd routeAddEvent;
		public GenerateConnectionRelease(WDMUtils.LightpathAdd routeAddEvent) { this.routeAddEvent = routeAddEvent; }
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
