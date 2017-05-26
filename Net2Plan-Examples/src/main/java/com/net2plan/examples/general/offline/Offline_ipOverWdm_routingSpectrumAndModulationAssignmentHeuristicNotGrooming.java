package com.net2plan.examples.general.offline;
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


import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.utils.Constants.OrderingType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.*;

import java.util.*;

/**
 * Algorithm based on an heuristic solving the Routing, Spectrum, Modulation Assignment (RSMA) problem with regenerator placement, 
 * in flexi (elastic) or fixed grid optical WDM networks, with or without fault tolerance and/or latency requisites.
 * 
 * <p>The input design typically has two layers (IP and WDM layers), according to the typical conventions in {@link com.net2plan.libraries.WDMUtils WDMUtils}. 
 * If the design has one single layer, it is first converted into a two-layer design: WDM layer taking the links (fibers) with no demands, 
 * IP layer taking the traffic demands, without IP links. Any previous routes are removed.</p>
 * <p>The WDM layer is compatible with {@link com.net2plan.libraries.WDMUtils WDMUtils} Net2Plan 
 * library usual assumptions:</p>
 * <ul>
 * <li>Each network node is assumed to be an Optical Add/Drop Multiplexer WDM node</li>
 * <li>Each network link at WDM layer is assumed to be an optical fiber.</li>
 * <li>The spectrum in the fibers is assumed to be composed of a number of frequency slots. 
 * In fixed-grid network, each frequency slot would correspond to a wavelength channel. 
 * In flexi-grid networks, it is just a frequency slot, and lightpaths can occupy more than one. In any case, 
 * two lightpaths that use overlapping frequency slots cannot traverse the same fiber, since their signals would mix.</li>
 * <li>No traffic demands initially exist at the WDM layer. In this algorithms, each lightpath is associated to a WDM demand </li>
 * <li> Each traffic demand at the IP layer is a need to transmit an amount of Gbps between two nodes. 
 * A demand traffic can be carried using one or more lightpaths.</li>
 * </ul>
 * 
 * <p>Each lightpath (primary or backup) produced by the design is returned as a {@code Route} object.</p>
 * 
 * <p>Each lightpath starts and ends in a transponder. The user is able to define a set of available transpoder types, so 
 * the design can create lightpaths using any combination of them. The information user-defined per transponder is:</p>
 * <ul>
 * <li>Line rate in Gbps (typically 10, 40, 100 in fixed-grid cases, and other multiples in flexi-grid networks).</li> 
 * <li>Cost</li> 
 * <li>Number of frequency slots occupied (for a given line rate, this depends on the modulation the transponder uses)</li> 
 * <li>Optical reach in km: Maximum allowed length in km of the lightpaths with this transponder. 
 * Higher distances can be reached using signal regenerators.</li> 
 * <li>Cost of a regenerator for this transponder. Regenerators can be placed at intermdiate nodes of the lightpath route, 
 * regenerate its optical signal, and then permit extending its reach. We consider that regenerators cannot change the 
 * frequency slots occupied by the lightpath (that is, they are not capable of wavelength conversion)</li> 
 * </ul>
 * 
 * <p>We assume that all the fibers use the same wavelength grid, composed of a user-defined number of frequency slots. 
 * The user can also select a limit in the maximum propagation delay of a lightpath. </p>
 * <p>The output design consists in the set of lightpaths to establish, in the 1+1 case also with a 1+1 lightpath each. 
 * Each lightpath is characterized by the transponder type used (which sets its line rate and number of occupied slots in the 
 * traversed fibers), the particular set of contiguous frequency slots occupied, and the set of signal regeneration points (if any). 
 * This information is stored in the {@code Route} object using the regular methods in WDMUTils, 
 * and can be retrieved in the same form (e.g. by a report showing the WDM network information). 
 * If a feasible solution is not found (one where all the demands are satisfied with the given constraints), a message is shown.</p>.  
 * 
 * <h2>Failure tolerance</h2>
 * <p>The user can choose among three possibilities for designing the network:</p>
 * <ul>
 * <li>No failure tolerant: The lightpaths established should be enough to carry the traffic of all the demands when no failure 
 * occurs in the network, but any losses are accepted if the network suffers failures in links or nodes.</li>
 * <li>Tolerant to single-SRG (Shared Risk Group) failures with static lightpaths: All the traffic demands should be satisfied using 
 * lightpaths, so that under any single-SRG failure (SRGs are taken from the input design), the surviving lightpaths are enough 
 * to carry the 100% of the traffic. Note that lightpaths are static, in the sense that they are not rerouted when affected by a 
 * failure (they just also fail), and the design should just overprovision the number of lightpaths to establish with that in mind.</li>
 * <li>1+1 SRG-disjoint protection: This is another form to provide single-SRG failure tolerance. Each lightpath is backed up by 
 * a SRG-disjoint lightpath. The backup lightpath uses the same type of transponder 
 * as the primary (and thus the same line rate, an occupies the same number of slots), its path is SRG-disjoint, and the particular 
 * set of slots occupied can be different. </li>
 * </ul>
 * <h2>Use cases</h2>
 * <p>This algorithm is quite general, and fits a number of use cases designing WDM networks, for instance:</p>
 * <ul>
 * <li>Single line rate, fixed grid networks: Then, one single type of transponder will be available, which occupies one frequency slot</li>
 * <li>Mixed-Line Rate fixed-grid networks: In this case, several transponders can be available at different line rates and with different optical 
 * reaches. However, all of them occupy one slot (one wavelength channel)</li>
 * <li>Single line rate, flexi-grid networks using varying-modulation transponders: Several transponders are available (or the same 
 * transponder with varying configurations), all of them with the same line rate, but thanks to the different usable modulations 
 * they can have different optical reaches and/or number of occupied slots.</li>
 * <li>Multiple line rate, flexi-grid networks using Bandwidth Variable Transponders: Here, it is possible to use different transponders 
 * with different line rates, e.g. to reflect more sophisticated transponders which can have different configurations, varying its line rate, 
 * optical reach, and number of occupied slots.</li>
 * <li>...</li>
 * </ul>
 * <h2>Some details of the algorithm</h2>
 * <p>The algorithm is based on a heuristic. Initially, at most {@code k} paths are selected for each demand and transponder type. 
 * Then, in each iteration, the algorithm first orders the demands in descending order according to the traffic pending 
 * to be carried (if single-SRG failure tolerance is chosen, this is the average among all the 
 * states -no failure and single SRG failure). Then, all the transponder types and possible routes (or SRG-disjoint 1+1 pairs in the 
 * 1+1 case) are attempted for that demand, using a first-fit approach for the slots. If an RSA is found for more than one 
 * transponder and route, the one chosen is first, the one with best performance metric, and among them, the first transponder 
 * according to the order in which the user put it in the input parameter, and among them the shortest one in km . 
 * The performance metric used is the amount of extra traffic carried if the lightpath is established, divided by the lightpath cost, 
 * summing the transponder cost, and the cost of the signal regenerators if any.</p>
 * <p>The details of the algorithm will be provided in a publication currently under elaboration.</p>
 * 
 * @net2plan.keywords WDM
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
@SuppressWarnings("unchecked")
public class Offline_ipOverWdm_routingSpectrumAndModulationAssignmentHeuristicNotGrooming implements IAlgorithm
{
	private InputParameter k = new InputParameter ("k", (int) 5 , "Maximum number of admissible paths per input-output node pair" , 1 , Integer.MAX_VALUE);
	private InputParameter numFrequencySlotsPerFiber = new InputParameter ("numFrequencySlotsPerFiber", (int) 40 , "Number of wavelengths per link" , 1, Integer.MAX_VALUE);
	private InputParameter transponderTypesInfo = new InputParameter ("transponderTypesInfo", "10 1 1 9600 1" , "Transpoder types separated by \";\" . Each type is characterized by the space-separated values: (i) Line rate in Gbps, (ii) cost of the transponder, (iii) number of slots occupied in each traversed fiber, (iv) optical reach in km (a non-positive number means no reach limit), (v) cost of the optical signal regenerator (regenerators do NOT make wavelength conversion ; if negative, regeneration is not possible).");
	private InputParameter ipLayerIndex = new InputParameter ("ipLayerIndex", (int) 1 , "Index of the IP layer (-1 means default layer)");
	private InputParameter wdmLayerIndex = new InputParameter ("wdmLayerIndex", (int) 0 , "Index of the WDM layer (-1 means default layer)");
	private InputParameter networkRecoveryType = new InputParameter ("networkRecoveryType", "#select# not-fault-tolerant single-srg-tolerant-static-lp 1+1-srg-disjoint-lps" , "Establish if the design should be tolerant or not to single SRG failures (SRGs are as defined in the input NetPlan). First option is that the design should not be fault tolerant, the second means that failed lightpaths are not recovered, but an overprovisioned should be made so enough lightpaths survive to carry all the traffic in every failure. The third means that each lightpath is 1+1 protceted by a SRG-disjoint one, that uses the same transponder");
	private InputParameter maxPropagationDelayMs = new InputParameter ("maxPropagationDelayMs", (double) -1 , "Maximum allowed propagation time of a lighptath in miliseconds. If non-positive, no limit is assumed");
	
	private NetPlan netPlan;
	private Map<Pair<Node,Node>,List<List<Link>>> cpl;
	private Map<Pair<Node,Node>,List<Pair<List<Link>,List<Link>>>> cpl11;
	private NetworkLayer wdmLayer, ipLayer;
	private WDMUtils.TransponderTypesInfo tpInfo;
	private int N, Ewdm, Dip, S, T;
	private boolean singleSRGToleranceNot11Type;
	private DoubleMatrix2D frequencySlot2FiberOccupancy_se;
	private Demand.IntendedRecoveryType recoveryTypeNewLps;
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		this.netPlan = netPlan;
		
		/* Create a two-layer IP over WDM design if the input is single layer */
		if (netPlan.getNumberOfLayers() == 1)
		{
			this.wdmLayer = netPlan.getNetworkLayerDefault();
			this.netPlan.setDemandTrafficUnitsName("Gbps");
			this.netPlan.setLinkCapacityUnitsName("Frequency slots");
			this.ipLayer = netPlan.addLayer("IP" , "IP layer" , "Gbps" , "Gbps" , null , null);
			for (Demand wdmDemand : netPlan.getDemands(wdmLayer))
				netPlan.addDemand(wdmDemand.getIngressNode(), wdmDemand.getEgressNode() , wdmDemand.getOfferedTraffic() , wdmDemand.getAttributes() , ipLayer);
		}
		else
		{
			this.wdmLayer = wdmLayerIndex.getInt () == -1? netPlan.getNetworkLayerDefault() : netPlan.getNetworkLayer(wdmLayerIndex.getInt ());
			this.ipLayer = ipLayerIndex.getInt () == -1? netPlan.getNetworkLayerDefault() : netPlan.getNetworkLayer(ipLayerIndex.getInt ());
		}

		/* Basic checks */
		this.N = netPlan.getNumberOfNodes();
		this.Ewdm = netPlan.getNumberOfLinks(wdmLayer);
		this.Dip = netPlan.getNumberOfDemands(ipLayer);
		this.S = numFrequencySlotsPerFiber.getInt();
		if (N == 0 || Ewdm == 0 || Dip == 0) throw new Net2PlanException("This algorithm requires a topology with links and a demand set");
		this.singleSRGToleranceNot11Type = networkRecoveryType.getString().equals("single-srg-tolerant-static-lp");
		
		if (singleSRGToleranceNot11Type && (netPlan.getNumberOfSRGs() == 0)) throw new Net2PlanException ("No SRGs are defined, so there is no reason to use the single-SRG failure-tolerant design option");
		
		if (networkRecoveryType.getString().equals("not-fault-tolerant") || networkRecoveryType.getString().equals("single-srg-tolerant-static-lp"))
			recoveryTypeNewLps = Demand.IntendedRecoveryType.NONE;
		else if (networkRecoveryType.getString().equals("1+1-srg-disjoint-lps"))
			recoveryTypeNewLps = Demand.IntendedRecoveryType.PROTECTION_REVERT;
		else throw new Net2PlanException ("Wrong input parameters");
		
		/* Store transpoder info */
		WDMUtils.setFibersNumFrequencySlots(netPlan, S, wdmLayer);
		this.tpInfo = new WDMUtils.TransponderTypesInfo(transponderTypesInfo.getString());
		this.T = tpInfo.getNumTypes();

		/* Remove all routes in current netPlan object. Initialize link capacities and attributes, and demand offered traffic */
		/* WDM and IP layer are in source routing type */
		netPlan.removeAllLinks (ipLayer);
		netPlan.removeAllDemands (wdmLayer);
		netPlan.removeAllMulticastDemands(wdmLayer);
		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING , wdmLayer);
		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING , ipLayer);

		/* Initialize the slot occupancy */
		this.frequencySlot2FiberOccupancy_se = DoubleFactory2D.dense.make(S , Ewdm); 

		/* Compute the candidate path list of possible paths */
		this.cpl = netPlan.computeUnicastCandidatePathList(netPlan.getVectorLinkLengthInKm(wdmLayer) , k.getInt(), tpInfo.getMaxOpticalReachKm() , -1, maxPropagationDelayMs.getDouble(), -1, -1, -1 , null , wdmLayer);
		this.cpl11 = networkRecoveryType.getString().equals("1+1-srg-disjoint-lps")? NetPlan.computeUnicastCandidate11PathList(cpl,0) : null;
		
		/* Compute the CPL, adding the routes */
		/* 1+1 case: as many routes as 1+1 valid pairs (then, the same sequence of links can be in more than one Route).  */
		/* rest of the cases: each sequence of links appears at most once */
		Map<Link,Double> linkLengthMap = new HashMap<Link,Double> (); for (Link e : netPlan.getLinks(wdmLayer)) linkLengthMap.put(e , e.getLengthInKm());
		final int maximumNumberOfPaths = T*k.getInt()*Dip;
		List<Integer> transponderType_p = new ArrayList<Integer> (maximumNumberOfPaths);
		List<Double> cost_p = new ArrayList<Double> (maximumNumberOfPaths); 
		List<Double> lineRate_p = new ArrayList<Double> (maximumNumberOfPaths); 
		List<Integer> numSlots_p = new ArrayList<Integer> (maximumNumberOfPaths);
		List<List<Link>> seqLinks_p = new ArrayList<List<Link>> (maximumNumberOfPaths);
		List<List<Link>> seqLinks2_p = cpl11 == null? null : new ArrayList<List<Link>> (maximumNumberOfPaths);
		List<int []> regPositions_p = new ArrayList<int []> (maximumNumberOfPaths);
		List<int []> regPositions2_p = new ArrayList<int []> (maximumNumberOfPaths);
		List<Demand> ipDemand_p = new ArrayList<Demand> (maximumNumberOfPaths);
		Map<Demand,List<Integer>> ipDemand2WDMPathListMap = new HashMap<Demand,List<Integer>> (); 
		for (Demand ipDemand : netPlan.getDemands(ipLayer))
		{
			final Pair<Node,Node> nodePair = Pair.of(ipDemand.getIngressNode() , ipDemand.getEgressNode());
			boolean atLeastOnePathOrPathPair = false;
			List<Integer> pathListThisDemand = new LinkedList<Integer> ();
			ipDemand2WDMPathListMap.put(ipDemand , pathListThisDemand);
			for (int t = 0 ; t < T ; t ++)
			{
				final boolean isRegenerable = tpInfo.isOpticalRegenerationPossible(t);
				for (Object sp : cpl11 != null? cpl11.get(nodePair) : cpl.get(nodePair))
				{
					List<Link> firstPath = (cpl11 == null)? ((List<Link>) sp) : ((Pair<List<Link>,List<Link>>) sp).getFirst(); 
					List<Link> secondPath = (cpl11 == null)? null : ((Pair<List<Link>,List<Link>>) sp).getSecond (); 
					if (!isRegenerable && (getLengthInKm(firstPath) > tpInfo.getOpticalReachKm(t))) break;
					if (secondPath != null) if (!isRegenerable && (getLengthInKm(secondPath) > tpInfo.getOpticalReachKm(t))) break;

					final int [] regPositions1 = isRegenerable? WDMUtils.computeRegeneratorPositions(firstPath , tpInfo.getOpticalReachKm(t)) : new int [firstPath.size()];
					final int [] regPositions2 = cpl11 == null? null : isRegenerable? WDMUtils.computeRegeneratorPositions(secondPath , tpInfo.getOpticalReachKm(t)) : new int [secondPath.size()];
					final int numRegeneratorsNeeded = !isRegenerable? 0 : (int) IntUtils.sum(regPositions1) + (secondPath == null? 0 : (int) IntUtils.sum(regPositions2)) ;
					final double costOfLightpathOr11Pair = tpInfo.getCost(t) * (cpl11 != null? 2 : 1) + (tpInfo.getRegeneratorCost(t) * numRegeneratorsNeeded);

					final int pathIndex = cost_p.size();
					cost_p.add (costOfLightpathOr11Pair);
					transponderType_p.add (t);
					lineRate_p.add(tpInfo.getLineRateGbps(t));
					numSlots_p.add(tpInfo.getNumSlots(t));
					ipDemand_p.add(ipDemand);
					seqLinks_p.add(firstPath);
					regPositions_p.add(regPositions1);
					pathListThisDemand.add(pathIndex);
					if (cpl11 != null) { seqLinks2_p.add(secondPath); regPositions2_p.add(regPositions2); }
					atLeastOnePathOrPathPair = true;
				}
			}
			if (!atLeastOnePathOrPathPair) throw new Net2PlanException ("There are no possible routes (or 1+1 pairs) for a demand (" + ipDemand + "). The topology may be not connected enough, or the optical reach may be too small");
		}
		final int P = transponderType_p.size(); // one per potential sequence of links (or 1+1 pairs of sequences) and transponder

		/* Main algorithm loop. Take one demand at a time, in a HLDA loop (ordered by average blocked traffic). 
		 * In each demand, try all possible path-transponder pairs, and take the best according to the performance metric:
		 * avExtraTrafficCarried/transponderCost */
		boolean atLeastOneLpAdded = false;
		Set<Integer> ipDemandIndexesNotToTry = new HashSet<Integer> ();
		double totalCost = 0;
		do 
		{
			double [] b_d = getVectorIPDemandAverageAllStatesBlockedTraffic ();
			int [] ipDemandIndexes = DoubleUtils.sortIndexes(b_d , OrderingType.DESCENDING);
			atLeastOneLpAdded = false;
			for (int ipDemandIndex : ipDemandIndexes)
			{
				/* Not to try a demand if already fully satisfied or we tried and could not add a lp to it */
				if (ipDemandIndexesNotToTry.contains(ipDemandIndex)) continue;

				final Demand ipDemand = netPlan.getDemand(ipDemandIndex , ipLayer);

				/* If the demand is already fully satisfied, skip it */
				if (isIpDemandFullySatisfied(ipDemand)) { ipDemandIndexesNotToTry.add(ipDemandIndex); continue; } 
				
				/* Try all the possible routes and all the possible transpoder types. Take the solution with the best 
				 * performance metric (average extra carried traffic / transponder cost) */
				WDMUtils.RSA best_rsa = null;
				WDMUtils.RSA best_rsa2 = null;
				double best_performanceMetric = 0;
				int best_pathIndex = -1;
				for (int pathIndex : ipDemand2WDMPathListMap.get (ipDemand))
				{
					List<Link> firstPath = seqLinks_p.get(pathIndex);
					List<Link> secondPath = cpl11 == null? null : seqLinks2_p.get(pathIndex);
					Pair<Integer,Integer> slotIds = null;
					int slotId = -1;
					if (cpl11 == null)
						slotId = WDMUtils.spectrumAssignment_firstFit(firstPath , frequencySlot2FiberOccupancy_se , numSlots_p.get(pathIndex));
					else
						slotIds = WDMUtils.spectrumAssignment_firstFitTwoRoutes(firstPath, secondPath, frequencySlot2FiberOccupancy_se , numSlots_p.get(pathIndex));
					
					/* Check if the path (or 1+1 path pair) is not feasible */
					if (cpl11 == null) if (slotId == -1) continue;
					if (cpl11 != null) if (slotIds == null) continue;
					
					/* If the performance metric is better than existing, this is the best choice */
					final double extraCarriedTraffic = getAverageAllStatesExtraCarriedTrafficAfterPotentialAllocation (ipDemand , lineRate_p.get(pathIndex) , seqLinks_p.get(pathIndex));
					final double performanceIndicator = extraCarriedTraffic / cost_p.get(pathIndex); 
					if (performanceIndicator > best_performanceMetric)
					{
						best_performanceMetric = performanceIndicator;
						best_rsa = new WDMUtils.RSA(firstPath , cpl11 != null? slotIds.getFirst() : slotId , numSlots_p.get(pathIndex) , regPositions_p.get(pathIndex));
						best_rsa2 = cpl11 == null? null : new WDMUtils.RSA(secondPath , slotIds.getSecond() , numSlots_p.get(pathIndex) , regPositions2_p.get(pathIndex));
						best_pathIndex = pathIndex;
					}
				}

				
				/* No lp could be added to this demand, try with the next */
				if (best_pathIndex == -1) { ipDemandIndexesNotToTry.add(ipDemand.getIndex()); continue; }
				
				/* Add the lightpath to the design */
				atLeastOneLpAdded = true;
				totalCost += cost_p.get(best_pathIndex);
				final Demand newWDMDemand = netPlan.addDemand(best_rsa.ingressNode , best_rsa.egressNode , lineRate_p.get(best_pathIndex) , null , wdmLayer);
				newWDMDemand.setIntendedRecoveryType(recoveryTypeNewLps);
				final Route lp = WDMUtils.addLightpath(newWDMDemand , best_rsa , lineRate_p.get(best_pathIndex));
				final Link ipLink = newWDMDemand.coupleToNewLinkCreated(ipLayer);
				final double ipTrafficToCarry = Math.min(lineRate_p.get(best_pathIndex) , ipDemand.getBlockedTraffic());
				netPlan.addRoute(ipDemand , ipTrafficToCarry , ipTrafficToCarry , Arrays.asList(ipLink), null);
				WDMUtils.allocateResources(best_rsa , frequencySlot2FiberOccupancy_se , null);
				if (cpl11 != null)
				{
					final Route lpBackup = WDMUtils.addLightpath(newWDMDemand , best_rsa2 , 0);
					WDMUtils.allocateResources(best_rsa2 , frequencySlot2FiberOccupancy_se , null);
					lp.addBackupRoute(lpBackup);
				}
				break;
			}
			
		} while (atLeastOneLpAdded);
		
		WDMUtils.checkResourceAllocationClashing(netPlan,true,true,wdmLayer);

		String outMessage = "Total cost: " + totalCost + ". Num lps (not including 1+1 backup if any) " + netPlan.getNumberOfRoutes(wdmLayer);
		//System.out.println (outMessage);
		return "Ok! " + outMessage;
	}

	private static double getLengthInKm (Collection<Link> r) { double res = 0; for (Link e : r) res += e.getLengthInKm(); return res; }
	
	@Override
	public String getDescription()
	{
		return "Algorithm based on an ILP solving the Routing, Spectrum, Modulation Assignment (RSMA) problem with regenerator placement, in flexi (elastic) or fixed grid optical WDM networks, with or without fault tolerance, latency and/or lightpath bidirectionality requisites (see the Javadoc for details).";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
	
	/* A vector with the blocked traffic for each demand (in the single-SRG failure tolerance, is averaged for each state) */
	private double [] getVectorIPDemandAverageAllStatesBlockedTraffic ()
	{
		double [] res = new double [Dip];
		for (Demand ipDemand : netPlan.getDemands(ipLayer))
		{
			res [ipDemand.getIndex()] = ipDemand.getBlockedTraffic();
			if (singleSRGToleranceNot11Type)
			{
				for (SharedRiskGroup srg : netPlan.getSRGs())
				{
					Set<Route> affectedRoutes = srg.getAffectedRoutes(wdmLayer);
					double carriedTrafficThisFailure = 0; 
					for (Route ipRoute : ipDemand.getRoutes())
					{
						final Set<Route> wdmRoutes = ipRoute.getSeqLinks().get(0).getCoupledDemand().getRoutes();
						for (Route r : wdmRoutes)
							if (!affectedRoutes.contains(r)) carriedTrafficThisFailure += r.getCarriedTraffic();
					}
					res [ipDemand.getIndex()] += Math.max(0 , ipDemand.getOfferedTraffic() - carriedTrafficThisFailure);
				}
			}
			res [ipDemand.getIndex()] /= (singleSRGToleranceNot11Type? (netPlan.getNumberOfSRGs() + 1) : 1);
		}
		return res;
	}
	
	/* The average for all the states (no failure, and potentially one per SRG if single-SRG failure tolerance option is chosen), 
	 * of all the blocked traffic */
	private double getAverageAllStatesExtraCarriedTrafficAfterPotentialAllocation (Demand ipDemand , double lineRateGbps , List<Link> seqLinksIfSingleSRGToleranceIsNeeded)
	{
		double extraCarriedTraffic = Math.min(ipDemand.getBlockedTraffic() , lineRateGbps);
		if (singleSRGToleranceNot11Type)
		{
			for (SharedRiskGroup srg : netPlan.getSRGs())
			{
				if (srg.affectsAnyOf(seqLinksIfSingleSRGToleranceIsNeeded)) continue; // no extra carried traffic
				Set<Route> affectedRoutes = srg.getAffectedRoutes(wdmLayer);
				double carriedTrafficThisFailure = 0; 
				for (Route ipRoute : ipDemand.getRoutes())
				{
					Set<Route> wdmRoutes = ipRoute.getSeqLinks().get(0).getCoupledDemand().getRoutes();
					for (Route r : wdmRoutes)
						if (!affectedRoutes.contains(r)) carriedTrafficThisFailure += r.getCarriedTraffic();
				}
				extraCarriedTraffic += Math.min(lineRateGbps , Math.max(0 , ipDemand.getOfferedTraffic() - carriedTrafficThisFailure));
			}
		}
		
		return extraCarriedTraffic / (singleSRGToleranceNot11Type? (netPlan.getNumberOfSRGs() + 1) : 1);
	}

	/* True if the demand is fully satisfied (in the single-SRG failure case, also in each SRG) */
	private boolean isIpDemandFullySatisfied (Demand d)
	{
		if (d.getBlockedTraffic() > 1e-3) return false;
		if (singleSRGToleranceNot11Type)
		{
			for (SharedRiskGroup srg : netPlan.getSRGs())
			{
				Set<Route> affectedRoutes = srg.getAffectedRoutes(wdmLayer);
				double carriedTrafficThisFailure = 0;
				for (Route ipRoute : d.getRoutes())
				{
					final Set<Route> lps = ipRoute.getSeqLinks().get(0).getCoupledDemand().getRoutes();  
					for (Route wdmRoute : lps) if (!affectedRoutes.contains(wdmRoute)) carriedTrafficThisFailure += wdmRoute.getCarriedTraffic();
				}
				if (carriedTrafficThisFailure + 1e-3 < d.getOfferedTraffic()) return false;
			}
		}
		return true;
	}
	
}
