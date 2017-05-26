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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.libraries.WDMUtils.TransponderTypesInfo;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Quadruple;
import com.net2plan.utils.RandomUtils;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;

/** Implements the reactions of a WDM network carrying lightpaths in a fixed or flexi grid of wavelengths. 
 * 
 * <p>The design follows the assumptions described in {@link com.net2plan.libraries.WDMUtils WDMUtils} Net2Plan library</p>
 * <p>This algorithm implements the reactions of a WDM network carrying lightpaths, to the following events:</p> 
 * <ul>
 * <li>{@link com.net2plan.libraries.WDMUtils.LightpathAdd WDMUtils.LightpathAdd}: Adds the corresponding lightpath to the network 
 * (the Route object and potentially a ProtectionSegment object if the lightpath is asked to be 1+1 protected), 
 * if enough resources exist for it. If the event includes a Demand object, the lightpath is associated to it. 
 * If not, a new demand is created. The object establishes the line rate of the lightpath to establish. If such line rate is 
 * not present in any transponder type defined, an exception is raised (on valid transponders exist for such a lightpath).
 * If the event includes the RSA (also of the backup, if 1+1 protection is selected), the algorithm 
 * tries this RSA. If not, the user-selected strategy for the routing and spectrum assignment of the lightpath is applied.
 * Such strategy is tried first with the first transponder type (in order) with the appropriate line rate. If a valid RSA is not found 
 * it is repeated with the next. The lightpath request is blocked if under no transponder type of the appropriate line rate, a RSA 
 * is found (note that different transponders can have e.g. different optical reaches or number of occupied slots).</li>
 * <li>{@link com.net2plan.libraries.WDMUtils.LightpathModify WDMUtils.LightpathModify}: Modifies the carried traffic and/or the RSA of a lightpath.</li>
 * <li>{@link com.net2plan.libraries.WDMUtils.LightpathRemove WDMUtils.LightpathRemove}: Removes the corresponding lightpath (including the Demand and Route objects, and potentially the 1+1 segment if any), releasing the resources.</li>
 * <li>{@link com.net2plan.interfaces.simulation.SimEvent.NodesAndLinksChangeFailureState SimEvent.NodesAndLinksChangeFailureState}: Fails/repairs the indicated nodes and/or links, and reacts to such failures 
 * (the particular form depends on the network recovery options selected).</li>
 * <li>{@link com.net2plan.interfaces.simulation.SimEvent.DemandModify SimEvent.DemandModify}: Modifies the offered traffic of a demand.</li>
 * <li>{@link com.net2plan.interfaces.simulation.SimEvent.DemandRemove SimEvent.DemandRemove}: Removes a demand, and all its associated lightpaths if any, releasing the resources.</li>
 * </ul>
 * 
 * This module can be used in conjunction with the {@code Online_evGen_wdm} generator for creating the events to which 
 * this module reacts. 
 * 
 * See in {@link com.net2plan.libraries.WDMUtils} Javadoc the WDM technology conventions used in Net2Plan built-in algorithms and libraries to represent WDM networks. 
 * @net2plan.keywords WDM, Network recovery: protection, Network recovery: restoration
 * @net2plan.inputParameters 
 */
public class Online_evProc_wdm extends IEventProcessor
{
	private InputParameter wdmNumFrequencySlotsPerFiber = new InputParameter ("wdmNumFrequencySlotsPerFiber", (int) 40 , "Set the number of frequency slots per fiber. If < 1, the number of slots set in the input file is used.");
	private InputParameter wdmRwaType = new InputParameter ("wdmRwaType", "#select# srg-disjointness-aware-route-first-fit alternate-routing least-congested-routing load-sharing" , "Criteria to decide the route of a connection among the available paths");
	private InputParameter wdmK = new InputParameter ("wdmK", (int) 5 , "Maximum number of admissible paths per demand in the candidate list computation" , 1 , Integer.MAX_VALUE);
	private InputParameter wdmRandomSeed = new InputParameter ("wdmRandomSeed", (long) 1 , "Seed for the random generator (-1 means random)");
	private InputParameter wdmMaxLightpathNumHops = new InputParameter ("wdmMaxLightpathNumHops", (int) -1 , "A lightpath cannot have more than this number of hops. A non-positive number means this limit does not exist");
//	private InputParameter wdmRecoveryType = new InputParameter ("wdmRecoveryType", "#select# protection restoration none" , "None, nothing is done, so affected routes fail. Restoration, affected routes are visited sequentially, and we try to reroute them in the available capacity; in protection, affected routes are rerouted using the protection segments.");
	private InputParameter wdmRemovePreviousLightpaths = new InputParameter ("wdmRemovePreviousLightpaths", false  , "If true, previous lightpaths are removed from the system during initialization.");
	//private InputParameter wdmProtectionTypeToNewRoutes = new InputParameter ("wdmProtectionTypeToNewRoutes", "#select# none 1+1-link-disjoint 1+1-node-disjoint 1+1-srg-disjoint" , "New lightpaths are not protected, or are protected by a 1+1 link disjoint, or a node disjoint or a SRG disjoint lightpath");
	private InputParameter wdmDefaultAndNewRouteRevoveryType = new InputParameter ("wdmDefaultAndNewRouteRevoveryType", "#select# none restoration 1+1-link-disjoint 1+1-node-disjoint 1+1-srg-disjoint" , "New lightpaths are not protected, or are protected by a 1+1 link disjoint, or a node disjoint or a SRG disjoint lightpath");
	private InputParameter wdmTransponderTypesInfo = new InputParameter ("wdmTransponderTypesInfo", "10 1 1 9600 1" , "Transpoder types separated by \";\" . Each type is characterized by the space-separated values: (i) Line rate in Gbps, (ii) cost of the transponder, (iii) number of slots occupied in each traversed fiber, (iv) optical reach in km (a non-positive number means no reach limit), (v) cost of the optical signal regenerator (regenerators do NOT make wavelength conversion ; if negative, regeneration is not possible).");

	private NetworkLayer wdmLayer;
	//private Map<Route,Pair<WDMUtils.RSA,WDMUtils.RSA>> wdmRouteOriginalRwa;
	private Map<Pair<Node,Node>,List<List<Link>>> cplWdm;
	private Map<Pair<Node,Node>,List<Pair<List<Link>,List<Link>>>> cplWdm11;
	private DoubleMatrix2D wavelengthFiberOccupancy;
	private TransponderTypesInfo tpInfo;
	private Map<Route,Integer> transponderTypeOfNewLps;

//	private boolean newRoutesHave11Protection;
	private boolean isRestorationRecovery , isProtectionRecovery;
	private boolean isAlternateRouting , isLeastCongestedRouting , isLoadSharing , isSrgDisjointAwareLpRouting;
	private Random rng;
	private int protectionTypeCode;
	private Demand.IntendedRecoveryType defaultRecoveryType;

	private double stat_trafficOfferedConnections , stat_trafficCarriedConnections;
	private double stat_trafficAttemptedToRecoverConnections , stat_trafficSuccessfullyRecoveredConnections;
	private long stat_numOfferedConnections , stat_numCarriedConnections;
	private long stat_numAttemptedToRecoverConnections , stat_numSuccessfullyRecoveredConnections;
	private double stat_transitoryInitTime;
	public boolean DEBUG = false;
	
	@Override
	public String getDescription()
	{
		return "Implements the reactions of a WDM network carrying lightpaths in a fixed grid of wavelengths";
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
		
		this.wdmLayer = initialNetPlan.getNetworkLayer("WDM"); if (wdmLayer == null) throw new Net2PlanException ("WDM layer not found");
		this.isRestorationRecovery = wdmDefaultAndNewRouteRevoveryType.getString ().equalsIgnoreCase("restoration");
		this.isProtectionRecovery = wdmDefaultAndNewRouteRevoveryType.getString ().startsWith("1+1");
		this.isAlternateRouting = wdmRwaType.getString().equalsIgnoreCase("alternate-routing");
		this.isLeastCongestedRouting = wdmRwaType.getString().equalsIgnoreCase("least-congested-routing");
		this.isSrgDisjointAwareLpRouting = 	wdmRwaType.getString().equalsIgnoreCase("srg-disjointness-aware-route-first-fit");;
		this.isLoadSharing = wdmRwaType.getString().equalsIgnoreCase("load-sharing");
		this.rng = new Random(wdmRandomSeed.getLong () == -1? (long) RandomUtils.random(0, Long.MAX_VALUE - 1) : wdmRandomSeed.getLong ());
		
		this.defaultRecoveryType = isProtectionRecovery? Demand.IntendedRecoveryType.PROTECTION_REVERT : isRestorationRecovery? Demand.IntendedRecoveryType.RESTORATION : Demand.IntendedRecoveryType.NONE;

		if (wdmRemovePreviousLightpaths.getBoolean())
		{
			initialNetPlan.removeAllRoutes(wdmLayer);
			initialNetPlan.removeAllMulticastTrees(wdmLayer);
		}
		initialNetPlan.setRoutingType(RoutingType.SOURCE_ROUTING , wdmLayer);
		if (wdmNumFrequencySlotsPerFiber.getInt() > 0) WDMUtils.setFibersNumFrequencySlots(initialNetPlan , wdmNumFrequencySlotsPerFiber.getInt() , wdmLayer);
		
		/* Get the transponders information */
		this.tpInfo = new TransponderTypesInfo(wdmTransponderTypesInfo.getString());
		this.transponderTypeOfNewLps = new HashMap<Route,Integer> ();

		/* Create empty candidate path lists: they will be filled on demand */
		this.cplWdm = new HashMap<> ();
		this.protectionTypeCode = wdmDefaultAndNewRouteRevoveryType.getString ().equals("1+1-srg-disjoint") ? 0 : wdmDefaultAndNewRouteRevoveryType.getString ().equals("1+1-node-disjoint")? 1 : 2;
		this.cplWdm11 = isProtectionRecovery? new HashMap<> () : null; 
		
		this.wavelengthFiberOccupancy = WDMUtils.getNetworkSlotAndRegeneratorOcupancy(initialNetPlan, true , wdmLayer).getFirst();
		if (DEBUG) { checkWaveOccupEqualsNp(initialNetPlan); checkClashing (initialNetPlan); } 
		initialNetPlan.setLinkCapacityUnitsName("Frequency slots" , wdmLayer);

		this.finishTransitory(0);
		if (DEBUG) { checkWaveOccupEqualsNp(initialNetPlan); checkClashing (initialNetPlan); } 
	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		try {
			if (event.getEventObject () instanceof WDMUtils.LightpathAdd)
			{
				WDMUtils.LightpathAdd addLpEvent = (WDMUtils.LightpathAdd) event.getEventObject ();
				if (addLpEvent.layer != this.wdmLayer) throw new Net2PlanException ("Lightpaths cannot be added to layer " + addLpEvent.demand.getLayer() + ", and just to the WDM layer (" + wdmLayer + ")");
				final Node ingressNode = addLpEvent.ingressNode;
				final Node egressNode = addLpEvent.egressNode;
				final Pair<Node,Node> cplNodePair = Pair.of(ingressNode,egressNode);
				final double lineRateThisLp_Gbps = addLpEvent.lineRateGbps;
				if (!tpInfo.isValidLineRateForAtLeastOneType (lineRateThisLp_Gbps)) throw new Net2PlanException ("Requested to set up a lightpath of a line rate (" + lineRateThisLp_Gbps + ") for which I don't have transpoders");

				/* update the offered traffic of the demand */
				this.stat_numOfferedConnections ++;
				this.stat_trafficOfferedConnections += lineRateThisLp_Gbps;
				
				/* Computes one or two paths over the links (the second path would be a segment). You cannot use the already existing segments in these paths */
				if (isProtectionRecovery)
				{
					/* The RWA may be computed by me, or mandated by the event */
					Pair<WDMUtils.RSA,WDMUtils.RSA> rwa = null;
					int transponderTypeUsed = -1;
					if ((addLpEvent.primaryRSA != null) && (addLpEvent.backupRSA != null))
					{
						if (!WDMUtils.isAllocatableRSASet(wavelengthFiberOccupancy , addLpEvent.primaryRSA , addLpEvent.backupRSA))
							throw new Net2PlanException ("A request was received to add a conflicting lightpath 1+1 pair");
						rwa = Pair.of (addLpEvent.primaryRSA , addLpEvent.backupRSA);
					}
					else
					{
						/* Check among the transponder types (in order), the one in the same rate that fits (optical reach in range). */
						for (int t = 0; t < tpInfo.getNumTypes() ; t ++)
						{
							if (lineRateThisLp_Gbps != tpInfo.getLineRateGbps(t)) continue;
							rwa = computeValid11PathPairNewRoute(cplNodePair , currentNetPlan , tpInfo.getNumSlots(t) , tpInfo.getOpticalReachKm(t) , tpInfo.isOpticalRegenerationPossible(t));
							if (rwa != null) { transponderTypeUsed = t; break; }
						}
					}

					if (rwa != null)
					{
						if (!WDMUtils.isAllocatableRSASet(wavelengthFiberOccupancy , rwa.getFirst() , rwa.getSecond())) throw new RuntimeException ("Bad");
						if (!WDMUtils.isAllocatableRSASet(wavelengthFiberOccupancy , rwa.getSecond())) throw new RuntimeException ("Bad");
						for (Link e : rwa.getFirst ().seqLinks) if (e.getNetPlan() != currentNetPlan) throw new RuntimeException ("Bad. e.getNetPlan is null?: " + (e.getNetPlan() ==null) + ", currentNp is null: " + (currentNetPlan == null));
						for (Link e : rwa.getSecond ().seqLinks) if (e.getNetPlan() != currentNetPlan) throw new RuntimeException ("Bad");

						if (DEBUG) { checkWaveOccupEqualsNp(currentNetPlan); checkClashing (currentNetPlan); } 
						
						final Demand wdmLayerDemand = addLpEvent.demand == null? currentNetPlan.addDemand(addLpEvent.ingressNode, addLpEvent.egressNode, lineRateThisLp_Gbps , null, wdmLayer) : addLpEvent.demand;
						wdmLayerDemand.setIntendedRecoveryType(Demand.IntendedRecoveryType.PROTECTION_REVERT);
						final Route wdmLayerRoute = WDMUtils.addLightpath(wdmLayerDemand, rwa.getFirst(), lineRateThisLp_Gbps);
						WDMUtils.allocateResources(rwa.getFirst() , wavelengthFiberOccupancy , null);

						if (DEBUG) { checkWaveOccupEqualsNp(currentNetPlan); checkClashing (currentNetPlan); } 

						if (rwa.getFirst().seqLinks.equals(rwa.getSecond().seqLinks)) throw new RuntimeException ("Both 1+1 same route");
						final Route wdmLayerBackupRoute = WDMUtils.addLightpath(wdmLayerDemand, rwa.getSecond(), 0); // it is a backup, has no traffic carried then
						wdmLayerRoute.addBackupRoute(wdmLayerBackupRoute);
						WDMUtils.allocateResources(rwa.getSecond() , wavelengthFiberOccupancy , null);
						if (DEBUG) { checkWaveOccupEqualsNp(currentNetPlan); checkClashing (currentNetPlan); } 
						checkDisjointness(wdmLayerRoute.getSeqLinks() , rwa.getSecond().seqLinks , protectionTypeCode);
						this.transponderTypeOfNewLps.put(wdmLayerRoute , transponderTypeUsed);
						this.stat_numCarriedConnections ++;
						this.stat_trafficCarriedConnections += lineRateThisLp_Gbps;
						addLpEvent.lpAddedToFillByProcessor = wdmLayerRoute;
						if (DEBUG) { checkWaveOccupEqualsNp(currentNetPlan); checkClashing (currentNetPlan); } 
					}
					if (DEBUG) { checkWaveOccupEqualsNp(currentNetPlan); checkClashing (currentNetPlan); } 
				}
				else
				{
					/* The RWA may be computed by me, or mandated by the event */
					WDMUtils.RSA rwa = null;
					int transponderTypeUsed = -1;
					if (addLpEvent.primaryRSA != null)
					{
						if (!WDMUtils.isAllocatableRSASet(wavelengthFiberOccupancy , addLpEvent.primaryRSA))
							throw new Net2PlanException ("A request was received to add a conflicting lightpath");
						rwa = addLpEvent.primaryRSA; 
					}
					else
					{
						/* Check among the transponder types (in order), the one in the same rate that fits (optical reach in range). */
						for (int t = 0; t < tpInfo.getNumTypes() ; t ++)
						{
							if (lineRateThisLp_Gbps != tpInfo.getLineRateGbps(t)) continue;
							rwa = computeValidPathNewRoute(cplNodePair , currentNetPlan , tpInfo.getNumSlots(t) , tpInfo.getOpticalReachKm(t) , tpInfo.isOpticalRegenerationPossible(t));
							if (rwa != null) { transponderTypeUsed = t; break; }
						}
					}
					if (rwa != null)
					{
						final Demand wdmLayerDemand = addLpEvent.demand == null? currentNetPlan.addDemand(addLpEvent.ingressNode, addLpEvent.egressNode, lineRateThisLp_Gbps , null, wdmLayer) : addLpEvent.demand;
						wdmLayerDemand.setIntendedRecoveryType(isRestorationRecovery? Demand.IntendedRecoveryType.RESTORATION : Demand.IntendedRecoveryType.NONE);
						final Route wdmLayerRoute = WDMUtils.addLightpath(wdmLayerDemand, rwa , lineRateThisLp_Gbps);
						WDMUtils.allocateResources(rwa , wavelengthFiberOccupancy , null);
						this.transponderTypeOfNewLps.put(wdmLayerRoute , transponderTypeUsed);
						this.stat_numCarriedConnections ++;
						this.stat_trafficCarriedConnections += lineRateThisLp_Gbps;
						addLpEvent.lpAddedToFillByProcessor = wdmLayerRoute;
					}
				}
				if (DEBUG) { checkWaveOccupEqualsNp(currentNetPlan); checkClashing (currentNetPlan); } 
			}
			else if (event.getEventObject () instanceof WDMUtils.LightpathRemove)
			{
				WDMUtils.LightpathRemove lpEvent = (WDMUtils.LightpathRemove) event.getEventObject ();
				final Route lpToRemove = lpEvent.lp;
				WDMUtils.releaseResources(new WDMUtils.RSA(lpToRemove , false) , wavelengthFiberOccupancy, null);
				for (Route backupLp : new ArrayList<> (lpToRemove.getBackupRoutes()))
				{
					WDMUtils.releaseResources(new WDMUtils.RSA(backupLp , false), wavelengthFiberOccupancy, null);
					backupLp.remove();
					this.transponderTypeOfNewLps.remove(backupLp);
				}
				lpToRemove.remove (); 
				this.transponderTypeOfNewLps.remove(lpToRemove);
				if (DEBUG) { checkWaveOccupEqualsNp(currentNetPlan); checkClashing (currentNetPlan); } 
			} else if (event.getEventObject () instanceof SimEvent.NodesAndLinksChangeFailureState)
			{
				SimEvent.NodesAndLinksChangeFailureState ev = (SimEvent.NodesAndLinksChangeFailureState) event.getEventObject ();

				if (DEBUG) { checkWaveOccupEqualsNp(currentNetPlan); checkClashing (currentNetPlan); } 

				/* This automatically sets as up the routes affected by a repair in its current path, and sets as down the affected by a failure in its current path */
				Set<Route> routesFromDownToUp = new HashSet<> (currentNetPlan.getRoutesDown(wdmLayer));
				currentNetPlan.setLinksAndNodesFailureState(ev.linksToUp , ev.linksToDown , ev.nodesToUp , ev.nodesToDown);
				routesFromDownToUp.removeAll(currentNetPlan.getRoutesDown(wdmLayer));

				/* If primary GOES up in PROTECTION-REVERT => backup carried is set to zero in all backups */ 
				for (Route r : routesFromDownToUp)
				{
					final Demand wdmDemand = r.getDemand();
					final Demand.IntendedRecoveryType recoveryType = wdmDemand.getIntendedRecoveryType() == Demand.IntendedRecoveryType.NOTSPECIFIED? defaultRecoveryType : wdmDemand.getIntendedRecoveryType();
					if (recoveryType ==  Demand.IntendedRecoveryType.PROTECTION_REVERT)
					for (Route backup : r.getBackupRoutes())
						backup.setCarriedTraffic(0, null); // primary to up => carried in backup to zero
				}

				/* Now take down routes one by one, and see what to do with them (if something)  */ 
				for (Route r : new ArrayList<> (currentNetPlan.getRoutesDown(wdmLayer)))
				{
					final Demand wdmDemand = r.getDemand();
					final Demand.IntendedRecoveryType recovType = wdmDemand.getIntendedRecoveryType() == Demand.IntendedRecoveryType.NOTSPECIFIED? defaultRecoveryType : wdmDemand.getIntendedRecoveryType();

					if (recovType == Demand.IntendedRecoveryType.NONE) continue;

					if (recovType == Demand.IntendedRecoveryType.PROTECTION_REVERT)
					{
						/* If is 1+1 protection, do nothing with backup routes that are down */
						if (r.isBackupRoute()) continue;
						this.stat_numAttemptedToRecoverConnections ++;
						this.stat_trafficAttemptedToRecoverConnections += r.getCarriedTrafficInNoFailureState();
						/* The primary routes goes down => its backup has now carried traffic (if no failure) */
						if (r.hasBackupRoutes())
						{
							final Route backupRoute = r.getBackupRoutes().get(0);
							backupRoute.setCarriedTraffic(r.getCarriedTrafficInNoFailureState(), null);
							if (!backupRoute.isDown())
							{
								this.stat_numSuccessfullyRecoveredConnections ++; 
								this.stat_trafficSuccessfullyRecoveredConnections += r.getCarriedTrafficInNoFailureState(); 
							}
						}
					}
					else if (recovType == Demand.IntendedRecoveryType.RESTORATION)
					{
						this.stat_numAttemptedToRecoverConnections ++;
						this.stat_trafficAttemptedToRecoverConnections += r.getCarriedTrafficInNoFailureState();
						final Pair<Node,Node> cplNodePair= Pair.of(r.getIngressNode() , r.getEgressNode());
						final Integer transponderTypeLp = transponderTypeOfNewLps.get(r);
						final double maxOpticalReachKm = transponderTypeLp == null? Double.MAX_VALUE : tpInfo.getOpticalReachKm(transponderTypeLp);
						final boolean isSignalRegenerationPossible = transponderTypeLp == null? true : tpInfo.isOpticalRegenerationPossible(transponderTypeLp); 
						final int numSlots = transponderTypeLp == null? new WDMUtils.RSA(r,false).getNumSlots() : tpInfo.getNumSlots(transponderTypeLp);
						WDMUtils.RSA rwa = computeValidPathNewRoute (cplNodePair , currentNetPlan , numSlots , maxOpticalReachKm , isSignalRegenerationPossible);
						if (rwa != null)
						{ 
							WDMUtils.releaseResources(new WDMUtils.RSA (r , false) , wavelengthFiberOccupancy, null);
							WDMUtils.allocateResources(rwa , wavelengthFiberOccupancy , null);
							r.setSeqLinks(rwa.seqLinks);
							WDMUtils.setLightpathRSAAttributes(r , rwa , false);
							
							this.stat_numSuccessfullyRecoveredConnections ++; 
							this.stat_trafficSuccessfullyRecoveredConnections += r.getCarriedTrafficInNoFailureState(); 
						}
					}
				}
				if (DEBUG) { checkWaveOccupEqualsNp(currentNetPlan); checkClashing (currentNetPlan); } 
				
			} else if (event.getEventObject () instanceof SimEvent.DemandModify)
			{
				SimEvent.DemandModify ev = (SimEvent.DemandModify) event.getEventObject ();
				Demand d = ev.demand; 
				if (ev.modificationIsRelativeToCurrentOfferedTraffic) 
					d.setOfferedTraffic(d.getOfferedTraffic() + ev.offeredTraffic);
				else
					d.setOfferedTraffic(ev.offeredTraffic);
				if (DEBUG) { checkWaveOccupEqualsNp(currentNetPlan); checkClashing (currentNetPlan); } 
			} else if (event.getEventObject () instanceof SimEvent.DemandRemove)
			{
				SimEvent.DemandRemove ev = (SimEvent.DemandRemove) event.getEventObject ();
				Demand d = ev.demand;
				for (Route lpToRemove : new ArrayList<> (d.getRoutes()))
				{
					WDMUtils.releaseResources(new WDMUtils.RSA(lpToRemove , false), wavelengthFiberOccupancy, null);
					lpToRemove.remove();
					transponderTypeOfNewLps.remove(lpToRemove);
				}
				d.remove();
				if (DEBUG) { checkWaveOccupEqualsNp(currentNetPlan); checkClashing (currentNetPlan); } 
			}
			else if(event.getEventObject() instanceof WDMUtils.LightpathModify)
			{
				WDMUtils.LightpathModify modifyLpEvent = (WDMUtils.LightpathModify) event.getEventObject();
				final Route lpRoute = modifyLpEvent.lp;
				if (lpRoute != null) 
				{
					WDMUtils.RSA oldRSA = new WDMUtils.RSA(lpRoute , false);
					final Integer tpType = transponderTypeOfNewLps.get (lpRoute);
					if (modifyLpEvent.rsa == null) throw new Net2PlanException ("Modifying the lightpath line rate requires setting the RSA also");
					if (tpType == null) throw new Net2PlanException ("Cannot find the transponder type of the lightpath!");
					if ((modifyLpEvent.carriedTraffic != 0) && (modifyLpEvent.carriedTraffic != tpInfo.getLineRateGbps(tpType))) throw new Net2PlanException ("Cannot change the line rate of an existing lightpath");
					if ((modifyLpEvent.rsa.getNumSlots() != 0) && (modifyLpEvent.rsa.getNumSlots() != tpInfo.getNumSlots(tpType))) throw new Net2PlanException ("Cannot change the number of slots of an existing lightpath");
					if ((modifyLpEvent.rsa.getLengthInKm() > tpInfo.getOpticalReachKm(tpType)) && (!tpInfo.isOpticalRegenerationPossible(tpType))) throw new Net2PlanException ("Cannot modify the lightpath RSA in sucha way that the lightpath length exceeds the transponder maximum reach");
					
					lpRoute.setCarriedTraffic(modifyLpEvent.carriedTraffic, modifyLpEvent.rsa.getNumSlots());
					WDMUtils.releaseResources( oldRSA, wavelengthFiberOccupancy , null);
					WDMUtils.allocateResources(modifyLpEvent.rsa , wavelengthFiberOccupancy , null);
					lpRoute.setSeqLinks(modifyLpEvent.rsa.seqLinks);
					WDMUtils.setLightpathRSAAttributes(lpRoute , modifyLpEvent.rsa , false);
				}
			}
			else throw new Net2PlanException ("Unknown event type: " + event);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}	
	@Override
	public String finish(StringBuilder output, double simTime)
	{
		final double trafficBlockingOnConnectionSetup = stat_trafficOfferedConnections == 0? 0 : 1 - (stat_trafficCarriedConnections / stat_trafficOfferedConnections );
		final double connectionBlockingOnConnectionSetup = stat_numOfferedConnections == 0? 0.0 : 1 - (((double) stat_numCarriedConnections) / ((double) stat_numOfferedConnections));
		final double successProbabilityRecovery = stat_numAttemptedToRecoverConnections == 0? 0.0 : (((double) stat_numSuccessfullyRecoveredConnections) / ((double) stat_numAttemptedToRecoverConnections));
		final double successProbabilityTrafficRecovery = stat_trafficAttemptedToRecoverConnections == 0? 0.0 : (((double) stat_trafficSuccessfullyRecoveredConnections) / ((double) stat_trafficAttemptedToRecoverConnections));
		final double dataTime = simTime - stat_transitoryInitTime;
		if (dataTime <= 0) { output.append ("<p>No time for data acquisition</p>"); return ""; }

		output.append (String.format("<p>Total traffic of offered connections: number connections %d, total time average traffic: %f</p>", stat_numOfferedConnections, stat_trafficOfferedConnections / dataTime));
		output.append (String.format("<p>Total traffic of carried connections: number connections %d, total time average traffic: %f</p>", stat_numCarriedConnections, stat_trafficCarriedConnections / dataTime));
		output.append (String.format("<p>Blocking at connection setup: Probability of blocking a connection: %f, Fraction of blocked traffic: %f</p>", connectionBlockingOnConnectionSetup , trafficBlockingOnConnectionSetup));
		output.append (String.format("<p>Number connections attempted to recover: %d (summing time average traffic: %f). </p>", stat_numAttemptedToRecoverConnections, stat_trafficAttemptedToRecoverConnections / dataTime));
		output.append (String.format("<p>Number connections successfully recovered: %d (summing time average traffic: %f). </p>", stat_numSuccessfullyRecoveredConnections, stat_trafficSuccessfullyRecoveredConnections / dataTime));
		output.append (String.format("<p>Probability of successfully recover a connection: %f. Proability weighted by the connection traffic: %f). </p>", successProbabilityRecovery, successProbabilityTrafficRecovery));
//		output.append (String.format("<p>Total traffic carried at current state: %f. </p>", -1);
		return "";
	}

	@Override
	public void finishTransitory(double simTime)
	{
		this.stat_trafficOfferedConnections = 0;
		this.stat_trafficCarriedConnections = 0;
		this.stat_trafficAttemptedToRecoverConnections  = 0;
		this.stat_trafficSuccessfullyRecoveredConnections = 0;
		this.stat_numOfferedConnections = 0;
		this.stat_numCarriedConnections = 0;
		this.stat_numAttemptedToRecoverConnections = 0;
		this.stat_numSuccessfullyRecoveredConnections = 0;
		this.stat_transitoryInitTime = simTime;
	}
	

	/* down links or segments cannot be used */
	private WDMUtils.RSA computeValidPathNewRoute (Pair<Node,Node> cplNodePair , NetPlan currentNetPlan , int numContiguousSlots , double opticalReachKm , boolean isSignalRegenerationPossible)
	{
		/* If load sharing */
		final double maxLightpathLengthKm = isSignalRegenerationPossible? Double.MAX_VALUE : opticalReachKm;
		if (isLoadSharing)
		{
			final List<List<Link>> paths = getAndUpdateCplWdm(cplNodePair, currentNetPlan);
			final int randomChosenIndex = rng.nextInt(paths.size ());
			final List<Link> seqLinksThisNetPlan = paths.get(randomChosenIndex);
			final Triple<Boolean,Integer,Double> rwaEval = computeValidWAFirstFit_path (seqLinksThisNetPlan , numContiguousSlots , maxLightpathLengthKm);
			if (rwaEval.getFirst()) return new WDMUtils.RSA (seqLinksThisNetPlan, rwaEval.getSecond() , numContiguousSlots , WDMUtils.computeRegeneratorPositions(seqLinksThisNetPlan , maxLightpathLengthKm)); else return null;
		}
		else if (isAlternateRouting || isLeastCongestedRouting)
		{
			/* If alternate or LCR */
			WDMUtils.RSA lcrSoFar = null; double lcrIdleCapacitySoFar = -Double.MAX_VALUE; 
			for (List<Link> seqLinks : getAndUpdateCplWdm(cplNodePair, currentNetPlan))
			{
				final Triple<Boolean,Integer,Double> rwaEval = computeValidWAFirstFit_path (seqLinks , numContiguousSlots , maxLightpathLengthKm);
				if (!rwaEval.getFirst()) continue;
				if (isAlternateRouting) return new WDMUtils.RSA (seqLinks, rwaEval.getSecond(),numContiguousSlots , WDMUtils.computeRegeneratorPositions(seqLinks , maxLightpathLengthKm));
				if (rwaEval.getThird() > lcrIdleCapacitySoFar)
				{
					lcrIdleCapacitySoFar = rwaEval.getThird();
					lcrSoFar = new WDMUtils.RSA (seqLinks , rwaEval.getSecond());
				}
			}
			return (isAlternateRouting || (lcrSoFar == null))? (WDMUtils.RSA) null : lcrSoFar; 
		}
		else if (isSrgDisjointAwareLpRouting)
		{
			final Node thisNpIngressNode = cplNodePair.getFirst();
			final Node thisNpEgressNode = cplNodePair.getSecond();
			WDMUtils.RSA chosenRWA = null; 
			final Set<Route> existingPrimaryRoutesBetweenPairOfNodes = currentNetPlan.getNodePairRoutes(thisNpIngressNode, thisNpEgressNode , false , wdmLayer);
			int cplChosenRoute_numSRGOverlappingRoutes = Integer.MAX_VALUE;
			for(List<Link> seqLinks : getAndUpdateCplWdm(cplNodePair, currentNetPlan))
			{
				/* Check that the route has an available wavlength */
				final Triple<Boolean,Integer,Double> rwaEval = computeValidWAFirstFit_path (seqLinks , numContiguousSlots , maxLightpathLengthKm);
				if (!rwaEval.getFirst()) continue;
				/* compute the number of SRG-route pairs this route overlaps with, respect to the already existing lightpaths  */
				Set<SharedRiskGroup> affectingSRGs = SRGUtils.getAffectingSRGs(seqLinks);
				int numOverlappingSRGs = 0; 
				for (Route currentRoute : existingPrimaryRoutesBetweenPairOfNodes)
					for (SharedRiskGroup srg : currentRoute.getSRGs()) if (affectingSRGs.contains(srg)) numOverlappingSRGs ++;
				if (numOverlappingSRGs < cplChosenRoute_numSRGOverlappingRoutes) 
				{ 
					cplChosenRoute_numSRGOverlappingRoutes = numOverlappingSRGs; 
					chosenRWA = new WDMUtils.RSA (seqLinks , rwaEval.getSecond(),numContiguousSlots , WDMUtils.computeRegeneratorPositions(seqLinks, maxLightpathLengthKm));
					if (numOverlappingSRGs == 0) break; // no need to search more
				}
			}
			return chosenRWA;
		}
		throw new RuntimeException ("Bad");
	}

	private Pair<WDMUtils.RSA,WDMUtils.RSA> computeValid11PathPairNewRoute (Pair<Node,Node> cplNodePair , NetPlan currentNetPlan , int numContigousSlotsOccupies , double opticalReachKm , boolean isSignalRegenerationPossible)
	{
		final double maximumLengthOfLighptathKm = isSignalRegenerationPossible? Double.MAX_VALUE : opticalReachKm;
		
		/* If load sharing */
		if (isLoadSharing)
		{
			final List<Pair<List<Link>,List<Link>>> routePairs = getAndUpdateCplWdm11(cplNodePair, currentNetPlan);
			final int randomChosenIndex = rng.nextInt(routePairs.size());
			final List<Link> seqLinksPrimaryThisNetPlan = routePairs.get(randomChosenIndex).getFirst();
			final List<Link> seqLinksBackupThisNetPlan = routePairs.get(randomChosenIndex).getSecond();
			final Quadruple<Boolean,Integer,Integer,Double> rwaEval = computeValidWAFirstFit_pathPair (seqLinksPrimaryThisNetPlan , seqLinksBackupThisNetPlan , numContigousSlotsOccupies , maximumLengthOfLighptathKm);
			if (rwaEval.getFirst()) 
				return Pair.of(new WDMUtils.RSA (seqLinksPrimaryThisNetPlan, rwaEval.getSecond() , numContigousSlotsOccupies , WDMUtils.computeRegeneratorPositions(seqLinksPrimaryThisNetPlan, maximumLengthOfLighptathKm)) , new WDMUtils.RSA (seqLinksBackupThisNetPlan, rwaEval.getThird() , numContigousSlotsOccupies , WDMUtils.computeRegeneratorPositions(seqLinksBackupThisNetPlan , maximumLengthOfLighptathKm))); 
			else 
				return null;
		}
		else if (isAlternateRouting || isLeastCongestedRouting)
		{
			Pair<WDMUtils.RSA,WDMUtils.RSA> lcrSoFar = null; double lcrIdleCapacitySoFar= -Double.MAX_VALUE; 
			for (Pair<List<Link>,List<Link>> pathPair : getAndUpdateCplWdm11(cplNodePair, currentNetPlan))
			{
				final List<Link> seqLinksPrimaryThisNetPlan = pathPair.getFirst();
				final List<Link> seqLinksBackupThisNetPlan = pathPair.getSecond();
				final Quadruple<Boolean,Integer,Integer,Double> rwaEval = computeValidWAFirstFit_pathPair (seqLinksPrimaryThisNetPlan , seqLinksBackupThisNetPlan , numContigousSlotsOccupies , maximumLengthOfLighptathKm);
				if (!rwaEval.getFirst()) continue;
				if (isAlternateRouting) return Pair.of(new WDMUtils.RSA (seqLinksPrimaryThisNetPlan, rwaEval.getSecond() , numContigousSlotsOccupies , WDMUtils.computeRegeneratorPositions(seqLinksPrimaryThisNetPlan , maximumLengthOfLighptathKm)) , new WDMUtils.RSA (seqLinksBackupThisNetPlan, rwaEval.getThird() , numContigousSlotsOccupies , WDMUtils.computeRegeneratorPositions(seqLinksBackupThisNetPlan , maximumLengthOfLighptathKm)));
				if (rwaEval.getFourth() > lcrIdleCapacitySoFar)
				{
					lcrIdleCapacitySoFar = rwaEval.getFourth();
					lcrSoFar = Pair.of(new WDMUtils.RSA (seqLinksPrimaryThisNetPlan , rwaEval.getSecond()) , new WDMUtils.RSA (seqLinksBackupThisNetPlan , rwaEval.getThird()));
				}
			}
			return lcrSoFar; //if alternate, this is null also
		} else if (isSrgDisjointAwareLpRouting)
		{
			final Node thisNpIngressNode = cplNodePair.getFirst();
			final Node thisNpEgressNode = cplNodePair.getSecond();
			Pair<WDMUtils.RSA,WDMUtils.RSA> chosenRWA = null; 
			Set<Route> existingRoutesBetweenPairOfNodes = currentNetPlan.getNodePairRoutes(thisNpIngressNode, thisNpEgressNode , false , wdmLayer).stream().filter(e -> !e.isBackupRoute()).collect(Collectors.toSet());
			int cplChosenRoute_numSRGOverlappingRoutes = Integer.MAX_VALUE;
			for(Pair<List<Link>,List<Link>> pathPair : getAndUpdateCplWdm11(cplNodePair, currentNetPlan))
			{
				final List<Link> seqLinksPrimaryThisNetPlan = pathPair.getFirst();
				final List<Link> seqLinksBackupThisNetPlan = pathPair.getSecond();
				final Quadruple<Boolean,Integer,Integer,Double> rwaEval = computeValidWAFirstFit_pathPair (seqLinksPrimaryThisNetPlan , seqLinksBackupThisNetPlan , numContigousSlotsOccupies , maximumLengthOfLighptathKm);
				if (!rwaEval.getFirst()) continue;
				/* compute the number of SRG-route pairs this route overlaps with, respect to the already existing lightpaths  */
				final Set<SharedRiskGroup> affectingSRGs_primaryOrBackup = SRGUtils.getAffectingSRGs(seqLinksPrimaryThisNetPlan);
				affectingSRGs_primaryOrBackup.addAll(SRGUtils.getAffectingSRGs(seqLinksBackupThisNetPlan));
				int numOverlappingSRGs = 0; 
				for (Route currentRoute : existingRoutesBetweenPairOfNodes)
				{
					for (SharedRiskGroup srg : currentRoute.getSRGs()) if (affectingSRGs_primaryOrBackup.contains (srg)) numOverlappingSRGs ++;
					Set<SharedRiskGroup> affectingSRGs = currentRoute.hasBackupRoutes()? currentRoute.getBackupRoutes().get(0).getSRGs() : new HashSet<> ();
					for (SharedRiskGroup srg : affectingSRGs) if (affectingSRGs_primaryOrBackup.contains(srg)) numOverlappingSRGs ++;
				}
				if (numOverlappingSRGs < cplChosenRoute_numSRGOverlappingRoutes) 
				{ 
					cplChosenRoute_numSRGOverlappingRoutes = numOverlappingSRGs; 
					chosenRWA = Pair.of (new WDMUtils.RSA (seqLinksPrimaryThisNetPlan , rwaEval.getSecond() , 
							numContigousSlotsOccupies , 
							WDMUtils.computeRegeneratorPositions(seqLinksPrimaryThisNetPlan , maximumLengthOfLighptathKm)) , 
							new WDMUtils.RSA (seqLinksBackupThisNetPlan , rwaEval.getThird() , numContigousSlotsOccupies , 
									WDMUtils.computeRegeneratorPositions(seqLinksBackupThisNetPlan , maximumLengthOfLighptathKm)));
					if (numOverlappingSRGs == 0) break; // no need to search more
				}
			}
			return chosenRWA;
		}
		throw new RuntimeException ("Bad");
	}

	private Triple<Boolean,Integer,Double> computeValidWAFirstFit_path (List<Link> seqLinks , int numContiguousSlots , double maxOpticalReachKm)
	{
		if (seqLinks.isEmpty()) throw new RuntimeException ("Bad");
		if (seqLinks.get(0).getOriginNode().isDown()) return Triple.of(false,-1,-1.0);
		if (getLengthInKm(seqLinks) > maxOpticalReachKm) return Triple.of(false,-1,-1.0);
		double worseCaseSpareCapacity = Double.MAX_VALUE; 
		for (Link e : seqLinks) 
		{ 
			worseCaseSpareCapacity = Math.min(worseCaseSpareCapacity, e.getCapacity() - e.getOccupiedCapacity());
			if (e.getDestinationNode().isDown() || e.isDown()) return Triple.of(false,-1,-1.0);
		}
		int wavelength = WDMUtils.spectrumAssignment_firstFit(seqLinks, wavelengthFiberOccupancy , numContiguousSlots);
		if (wavelength != -1) 
			return Triple.of(true , wavelength , worseCaseSpareCapacity); 
		else return Triple.of(false,-1,-1.0);
	}
	
	private Quadruple<Boolean,Integer,Integer,Double> computeValidWAFirstFit_pathPair (List<Link> seqLinks_1 , List<Link> seqLinks_2 , int numSlots , double maxOpticalReachKm)
	{
		if (seqLinks_1.isEmpty() || seqLinks_2.isEmpty()) throw new RuntimeException ("Bad");
		if (seqLinks_1.get(0).getOriginNode().isDown()) return Quadruple.of(false,-1,-1,-1.0);
		if (seqLinks_2.get(0).getOriginNode().isDown()) return Quadruple.of(false,-1,-1,-1.0);
		if (getLengthInKm(seqLinks_1) > maxOpticalReachKm) return Quadruple.of(false,-1,-1,-1.0);
		if (getLengthInKm(seqLinks_2) > maxOpticalReachKm) return Quadruple.of(false,-1,-1,-1.0);
		double worseCaseSpareCapacity = Double.MAX_VALUE; 
		for (Link e : seqLinks_1) 
		{ 
			worseCaseSpareCapacity = Math.min(worseCaseSpareCapacity, e.getCapacity() - e.getOccupiedCapacity());
			if (e.getDestinationNode().isDown() || e.isDown()) return Quadruple.of(false,-1,-1,-1.0);
		}
		for (Link e : seqLinks_2) 
		{ 
			worseCaseSpareCapacity = Math.min(worseCaseSpareCapacity, e.getCapacity() - e.getOccupiedCapacity());
			if (e.getDestinationNode().isDown() || e.isDown()) return Quadruple.of(false,-1,-1,-1.0);
		}
		Pair<Integer,Integer> seqWavelengths = WDMUtils.spectrumAssignment_firstFitTwoRoutes(seqLinks_1, seqLinks_2, wavelengthFiberOccupancy,numSlots);
		if (seqWavelengths != null) 
		{
			if (!WDMUtils.isAllocatableRSASet(wavelengthFiberOccupancy , new WDMUtils.RSA(seqLinks_1 , seqWavelengths.getFirst() , numSlots)))
				throw new RuntimeException();
			if (!WDMUtils.isAllocatableRSASet(wavelengthFiberOccupancy , new WDMUtils.RSA(seqLinks_2 , seqWavelengths.getSecond() , numSlots)))
				throw new RuntimeException();
			return Quadruple.of(true , seqWavelengths.getFirst () , seqWavelengths.getSecond() , worseCaseSpareCapacity);
		}
		else return Quadruple.of(false,-1,-1,-1.0);
	}

	private void checkClashing (NetPlan np)
	{
		WDMUtils.checkResourceAllocationClashing(np, false, false, wdmLayer);
	}
	
	private static void checkDisjointness (List<Link> p1 , List<Link> p2 , int disjointnessType)
	{
		if ((p1 == null) || (p2 == null)) throw new RuntimeException ("Bad");
		if (disjointnessType == 0) // SRG disjoint
		{
			Set<SharedRiskGroup> srg1 = new HashSet<SharedRiskGroup> (); for (Link e : p1) srg1.addAll (e.getSRGs());
			for (Link e : p2) for (SharedRiskGroup srg : e.getSRGs()) if (srg1.contains(srg)) throw new RuntimeException ("Bad");
		}
		else if (disjointnessType == 1) // link and node
		{
			Set<NetworkElement> resourcesP1 = new HashSet<NetworkElement> (); boolean firstLink = true; 
			for (Link e : p1) { resourcesP1.add (e); if (firstLink) firstLink = false; else resourcesP1.add (e.getOriginNode()); }
			for (Link e : p2) if (resourcesP1.contains(e)) throw new RuntimeException ("Bad");
		}
		else if (disjointnessType == 2) // link 
		{
			Set<Link> linksP1 = new HashSet<Link> (p1); 
			for (Link e : p2) if (linksP1.contains (e)) throw new RuntimeException ("Bad: p1: " + p1 + ", p2: " + p2);
		}
		else throw new RuntimeException ("Bad");
			
	}
	
	private static double getLengthInKm (List<Link> p) { double res = 0; for (Link e : p) res += e.getLengthInKm(); return res; }
	private void checkWaveOccupEqualsNp (NetPlan currentNetPlan)
	{
		DoubleMatrix2D freqNow_se = WDMUtils.getNetworkSlotAndRegeneratorOcupancy(currentNetPlan, true , wdmLayer).getFirst();
		if (!freqNow_se.equals(wavelengthFiberOccupancy))
		{
			System.out.println(freqNow_se.assign(wavelengthFiberOccupancy , DoubleFunctions.minusMult(1.0)));
			throw new RuntimeException ();
		} 

	}
	
	private List<List<Link>> getAndUpdateCplWdm (Pair<Node,Node> pair , NetPlan np)
	{
		List<List<Link>> res = cplWdm.get(pair);
		if (res != null) return res;
		final Map<Link,Double> linkCostMap = new HashMap<> (); for (Link e : np.getLinks(wdmLayer)) linkCostMap.put(e, e.getLengthInKm());
        res = GraphUtils.getKLooplessShortestPaths(np.getNodes(), np.getLinks(wdmLayer), pair.getFirst(), 
        		pair.getSecond(), linkCostMap, wdmK.getInt(), tpInfo.getMaxOpticalReachKm(), wdmMaxLightpathNumHops.getInt(), -1, -1, -1, -1);
        if (res.isEmpty()) throw new Net2PlanException ("There is no path between nodes: " + pair.getFirst() + " -> " + pair.getSecond());
        cplWdm.put(pair ,  res);
        return res;
	}
	private List<Pair<List<Link>,List<Link>>> getAndUpdateCplWdm11 (Pair<Node,Node> pair , NetPlan np)
	{
		List<Pair<List<Link>,List<Link>>> res = cplWdm11.get(pair);
		if (res != null) return res;
		final List<List<Link>> basePathList = getAndUpdateCplWdm (pair , np);
		final Map<Pair<Node,Node> , List<List<Link>>> singleNodePairCpl = new HashMap<>();
		singleNodePairCpl.put(pair , basePathList); 
		final Map<Pair<Node, Node>, List<Pair<List<Link>, List<Link>>>> cpl11ThisPair = 
				NetPlan.computeUnicastCandidate11PathList(singleNodePairCpl , protectionTypeCode);
        if (cpl11ThisPair.isEmpty()) throw new Net2PlanException ("There is no 1+1 paths between nodes: " + pair.getFirst() + " -> " + pair.getSecond());
        cplWdm11.put(pair ,  cpl11ThisPair.get(pair));
        return cpl11ThisPair.get(pair);
	}
}
