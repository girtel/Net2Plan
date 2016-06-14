/*******************************************************************************
 * Copyright (c) 2016 Pablo Pavon Mari�o.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 ******************************************************************************/


package com.net2plan.examples.general.onlineSim;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tint.IntFactory1D;
import cern.jet.math.tdouble.DoubleFunctions;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.*;

import java.util.*;

/**
 * Implements the reactions of a WDM network carrying lightpaths in a fixed grid of wavelengths
 * <p>
 * This algorithm implements the reactions of a WDM network carrying lightpaths in a fixed wavelength grid, to a the following events:
 * <ul>
 * <li>WDMUtils.LightpathAdd: Adds the corresponding lightpath to the network (the Route object and potentially a ProtectionSegment object if the lightpath is asked to be 1+1 protected), if enough resources exist for it. If the event includes a Demand object, the lightpath is associated to it. If not, a new demand is created.</li>
 * <li>WDMUtils.LightpathRemove: Removes the corresponding lightpath (including the Demand and Route objects, and potentially the 1+1 segment if any), releasing the resources.</li>
 * <li>SimEvent.NodesAndLinksChangeFailureState: Fails/repairs the indicated nodes and/or links, and reacts to such failures (the particular form depends on the network recovery options selected).</li>
 * <li>SimEvent.DemandModify: Modifies the offered traffic of a demand (a lightpath).</li>
 * <li>SimEvent.DemandRemove: Removes a demand, and all its associated lightpaths if any, releasing the resources.</li>
 * </ul>
 * <p>
 * This module can be used in conjunction with the {@code Online_evGen_wdm} generator for simulating the WDM layer.
 * <p>
 * See the technology conventions used in Net2Plan built-in algorithms and libraries to represent WDM networks.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @net2plan.keywords WDM, Network recovery: protection, Network recovery: restoration
 * @net2plan.inputParameters
 */
public class Online_evProc_wdm extends IEventProcessor {
    private InputParameter wdmNumWavelengthsPerFiber = new InputParameter("wdmNumWavelengthsPerFiber", (int) 40, "Set the number of wavelengths per link. If < 1, the number of wavelengths set in the input file is used.");
    private InputParameter wdmRwaType = new InputParameter("wdmRwaType", "#select# srg-disjointness-aware-route-first-fit alternate-routing least-congested-routing load-sharing", "Criteria to decide the route of a connection among the available paths");
    private InputParameter wdmKShortestPathType = new InputParameter("wdmKShortestPathType", "#select# hops km", "Criteria to compute the shortest path. Valid values: 'hops' or 'km'");
    private InputParameter wdmK = new InputParameter("wdmK", (int) 5, "Maximum number of admissible paths per demand in the candidate list computation", 1, Integer.MAX_VALUE);
    private InputParameter wdmRandomSeed = new InputParameter("wdmRandomSeed", (long) 1, "Seed for the random generator (-1 means random)");
    private InputParameter wdmMaxLightpathLengthInKm = new InputParameter("wdmMaxLightpathLengthInKm", (double) -1, "Lightpaths longer than this are considered not admissible. A non-positive number means this limit does not exist");
    private InputParameter wdmMaxLightpathNumHops = new InputParameter("wdmMaxLightpathNumHops", (int) -1, "A lightpath cannot have more than this number of hops. A non-positive number means this limit does not exist");
    private InputParameter wdmLayerIndex = new InputParameter("wdmLayerIndex", (int) 0, "Index of the WDM layer (-1 means default layer)");
    private InputParameter wdmRecoveryType = new InputParameter("wdmRecoveryType", "#select# protection restoration none", "None, nothing is done, so affected routes fail. Restoration, affected routes are visited sequentially, and we try to reroute them in the available capacity; in protection, affected routes are rerouted using the protection segments.");
    private InputParameter wdmRemovePreviousLightpaths = new InputParameter("wdmRemovePreviousLightpaths", false, "If true, previous lightpaths are removed from the system during initialization.");
    private InputParameter wdmProtectionTypeToNewRoutes = new InputParameter("wdmProtectionTypeToNewRoutes", "#select# none 1+1-link-disjoint 1+1-node-disjoint 1+1-srg-disjoint", "New lightpaths are not protected, or are protected by a 1+1 link disjoint, or a node disjoint or a SRG disjoint lightpath");
    private InputParameter wdmLineRatePerLightpath_Gbps = new InputParameter("wdmLineRatePerLightpath_Gbps", (double) 40, "All the lightpaths have this carried traffic by default, when a lightpath is added and this quantity is not specified in the add lightpath event", 0, false, Double.MAX_VALUE, true);

    private NetworkLayer wdmLayer;
    private Map<Route, Pair<RWA, RWA>> wdmRouteOriginalRwa;
    private NetPlan cplWdm;
    private DoubleMatrix2D wavelengthFiberOccupancy;
    private DoubleMatrix2D cplA_rs, cplA_pss;

    private boolean newRoutesHave11Protection;
    private boolean isRestorationRecovery, isProtectionRecovery, isShortestPathNumHops;
    private boolean isAlternateRouting, isLeastCongestedRouting, isLoadSharing, isSrgDisjointAwareLpRouting;
    private Random rng;
    private int E_wdm;
    private int protectionTypeCode;

    private double stat_trafficOfferedConnections, stat_trafficCarriedConnections;
    private double stat_trafficAttemptedToRecoverConnections, stat_trafficSuccessfullyRecoveredConnections;
    private long stat_numOfferedConnections, stat_numCarriedConnections;
    private long stat_numAttemptedToRecoverConnections, stat_numSuccessfullyRecoveredConnections;
    private double stat_transitoryInitTime;


    @Override
    public String getDescription() {
        return "Implements the reactions of a WDM network carrying lightpaths in a fixed grid of wavelengths";
    }

    @Override
    public List<Triple<String, String, String>> getParameters() {
        /* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
        return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
    }

    @Override
    public void initialize(NetPlan initialNetPlan, Map<String, String> algorithmParameters, Map<String, String> simulationParameters, Map<String, String> net2planParameters) {
        /* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
        InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
        if (!wdmKShortestPathType.getString().equalsIgnoreCase("km") && !wdmKShortestPathType.getString().equalsIgnoreCase("hops"))
            throw new Net2PlanException("Wrong 'shortestPathType' parameter");

        this.wdmLayer = wdmLayerIndex.getInt() == -1 ? initialNetPlan.getNetworkLayerDefault() : initialNetPlan.getNetworkLayer(wdmLayerIndex.getInt());
        this.isShortestPathNumHops = wdmKShortestPathType.getString().equalsIgnoreCase("hops");
        this.wdmRouteOriginalRwa = new HashMap<Route, Pair<RWA, RWA>>();
        this.isRestorationRecovery = wdmRecoveryType.getString().equalsIgnoreCase("restoration");
        this.isProtectionRecovery = wdmRecoveryType.getString().equalsIgnoreCase("protection");
        this.isAlternateRouting = wdmRwaType.getString().equalsIgnoreCase("alternate-routing");
        this.isLeastCongestedRouting = wdmRwaType.getString().equalsIgnoreCase("least-congested-routing");
        this.isSrgDisjointAwareLpRouting = wdmRwaType.getString().equalsIgnoreCase("srg-disjointness-aware-route-first-fit");
        ;
        this.isLoadSharing = wdmRwaType.getString().equalsIgnoreCase("load-sharing");
        this.newRoutesHave11Protection = !wdmProtectionTypeToNewRoutes.getString().equalsIgnoreCase("none");
        this.rng = new Random(wdmRandomSeed.getLong() == -1 ? (long) RandomUtils.random(0, Long.MAX_VALUE - 1) : wdmRandomSeed.getLong());
        if (!isProtectionRecovery && newRoutesHave11Protection)
            throw new Net2PlanException("In the input parameter you ask to assign protection paths to new connections, while the recovery type chosen does not use them");

        if (wdmRemovePreviousLightpaths.getBoolean()) {
            initialNetPlan.removeAllDemands(wdmLayer);
            initialNetPlan.removeAllMulticastDemands(wdmLayer);
        }
        initialNetPlan.setRoutingType(RoutingType.SOURCE_ROUTING, wdmLayer);
        if (wdmNumWavelengthsPerFiber.getInt() > 0)
            WDMUtils.setFibersNumWavelengths(initialNetPlan, wdmNumWavelengthsPerFiber.getInt(), wdmLayer);
		

		/* Compute the candidate path list */
        this.E_wdm = initialNetPlan.getNumberOfLinks(wdmLayer);
        final DoubleMatrix1D linkCostVector = isShortestPathNumHops ? DoubleFactory1D.dense.make(E_wdm, 1.0) : initialNetPlan.getVectorLinkLengthInKm(wdmLayer);
        this.cplWdm = initialNetPlan.copy();
        for (NetworkLayer cplLayer : new HashSet<NetworkLayer>(cplWdm.getNetworkLayers()))
            if (cplLayer.getId() != wdmLayer.getId()) cplWdm.removeNetworkLayer(cplLayer);
        cplWdm.removeAllDemands();
        for (Node n1 : cplWdm.getNodes())
            for (Node n2 : cplWdm.getNodes()) if (n1 != n2) cplWdm.addDemand(n1, n2, 0, null);
        final Map<Demand, List<List<Link>>> cplPaths = cplWdm.computeUnicastCandidatePathList(linkCostVector.toArray(), "K", Integer.toString(wdmK.getInt()), "maxLengthInKm", Double.toString(wdmMaxLightpathLengthInKm.getDouble() > 0 ? wdmMaxLightpathLengthInKm.getDouble() : Double.MAX_VALUE), "maxNumHops", Integer.toString(wdmMaxLightpathNumHops.getInt() > 0 ? wdmMaxLightpathNumHops.getInt() : Integer.MAX_VALUE));
        this.protectionTypeCode = wdmProtectionTypeToNewRoutes.getString().equals("1+1-srg-disjoint") ? 0 : wdmProtectionTypeToNewRoutes.getString().equals("1+1-node-disjoint") ? 1 : 2;
        if (newRoutesHave11Protection) {
//			System.out.println ("cplWdm.getLinksDownAllLayers(): " + cplWdm.getLinksDownAllLayers());
//			System.out.println ("cplWdm.getRoutes(): " + cplWdm.getRoutes());
//			System.out.println ("INITIALIZE WDM WITH 1+1: protectionTypeCode: " + protectionTypeCode + ", linkCostVector: " + linkCostVector);
            final Map<Demand, List<Pair<List<Link>, List<Link>>>> pathPairs = NetPlan.computeUnicastCandidate11PathList(cplPaths, linkCostVector, protectionTypeCode);
//			for (Demand d : pathPairs.keySet())
//				System.out.println ("Demand " + d + ", path pairs: " + pathPairs.get(d));
//			System.out.println ("PathPairs: " + pathPairs);
            this.cplWdm.addRoutesAndProtectionSegmentFromCandidate11PathList(pathPairs);
            for (Route r : cplWdm.getRoutes()) {
//				System.out.println ("Route: " + r + ", demand: " + r.getDemand () + ", SEQ LINKS: " + r.getSeqLinksRealPath() + ", backup: " + r.getPotentialBackupProtectionSegments().iterator().next().getSeqLinks()); 
                checkDisjointness(r.getSeqLinksRealPath(), r.getPotentialBackupProtectionSegments().iterator().next().getSeqLinks(), protectionTypeCode);
            }
        } else {
//			System.out.println ("INITIALIZE WDM WITHOUT 1+1");
            this.cplWdm.addRoutesFromCandidatePathList(cplPaths);
        }
//		for (Demand d : cplWdm.getDemands()) if (d.getRoutes().isEmpty()) throw new Net2PlanException ("Some demands in the candidate path list have no routes: increase 'k' (or maybe the topology is not connected)");
        this.cplA_rs = cplWdm.getMatrixRoute2SRGAffecting();
        this.cplA_pss = cplWdm.getMatrixProtectionSegment2SRGAffecting();

//		System.out.println ("******************* cplWDM: " + cplWdm);

        this.wavelengthFiberOccupancy = WDMUtils.getMatrixWavelength2FiberOccupancy(initialNetPlan, true, wdmLayer);
        initialNetPlan.setLinkCapacityUnitsName("Wavelengths", wdmLayer);

        for (Route r : initialNetPlan.getRoutes(wdmLayer)) {
            if (!r.getSeqLinksAndProtectionSegments().equals(r.getSeqLinksRealPath()))
                throw new Net2PlanException("Some of the initial routes are traversing protection segments");
            final int[] seqWavelengths = WDMUtils.getLightpathSeqWavelengths(r);
            for (int cont = 1; cont < seqWavelengths.length; cont++)
                if (seqWavelengths[cont] != seqWavelengths[0])
                    throw new Net2PlanException("Initial lightpaths must have route continuity");
            final RWA thisRouteRWA = new RWA(r.getSeqLinksRealPath(), seqWavelengths[0]);
            RWA segmentRWA = null;
            if (r.getPotentialBackupProtectionSegments().size() > 1)
                throw new Net2PlanException("The number of protection segments of a route cannot be higher than one");
            if ((r.getPotentialBackupProtectionSegments().size() == 1) && (!isProtectionRecovery))
                throw new Net2PlanException("Initial routes have protection segments, which would be unused since the recovery type is not protection");
            if (r.getPotentialBackupProtectionSegments().size() == 1) {
                final ProtectionSegment backupPath = r.getPotentialBackupProtectionSegments().iterator().next();
                final int[] seqWavelengths_backup = WDMUtils.getLightpathSeqWavelengths(backupPath);
                for (int cont = 1; cont < seqWavelengths_backup.length; cont++)
                    if (seqWavelengths_backup[cont] != seqWavelengths_backup[0])
                        throw new Net2PlanException("Initial lightpaths must have route continuity");
                segmentRWA = new RWA(backupPath.getSeqLinks(), seqWavelengths_backup[0]);
            }
            wdmRouteOriginalRwa.put(r, Pair.of(thisRouteRWA, segmentRWA));
        }

        this.finishTransitory(0);
        checkClashing(initialNetPlan);
    }

    @Override
    public void processEvent(NetPlan currentNetPlan, SimEvent event) {
        if (event.getEventObject() instanceof WDMUtils.LightpathAdd) {
            WDMUtils.LightpathAdd addLpEvent = (WDMUtils.LightpathAdd) event.getEventObject();
            if (addLpEvent.layer != this.wdmLayer)
                throw new Net2PlanException("Lightpaths cannot be added to layer " + addLpEvent.demand.getLayer() + ", and just to the WDM layer (" + wdmLayer + ")");
            final Node ingressNode = addLpEvent.ingressNode;
            final Node egressNode = addLpEvent.egressNode;
            final Demand cplDemand = this.cplWdm.getNodePairDemands(cplWdm.getNodeFromId(ingressNode.getId()), cplWdm.getNodeFromId(egressNode.getId()), false).iterator().next();
            final double lineRateThisLp_Gbps = addLpEvent.lineRateGbps > 0 ? addLpEvent.lineRateGbps : wdmLineRatePerLightpath_Gbps.getDouble();

//			System.out.println ("EVENT LightpathAddAsPrimary: " + addLpEvent + ", demand: " + addLpEvent.demand);
			/* update the offered traffic of the demand */
            this.stat_numOfferedConnections++;
            this.stat_trafficOfferedConnections += lineRateThisLp_Gbps;
			
			/* Computes one or two paths over the links (the second path would be a segment). You cannot use the already existing segments in these paths */
            if (newRoutesHave11Protection) {
				/* The RWA may be computed by me, or mandated by the event */
                Pair<RWA, RWA> rwa;
                if (addLpEvent.seqLinks_backup == null)
                    rwa = computeValid11PathPairNewRoute(cplDemand, currentNetPlan);
                else {
                    if (!WDMUtils.isNonConflictingRWAPair(addLpEvent.seqLinks_primary, addLpEvent.seqWavelengths_primary, addLpEvent.seqLinks_backup, addLpEvent.seqWavelengths_backup, wavelengthFiberOccupancy))
                        throw new Net2PlanException("A request was received to add a conflicting lightpath 1+1 pair");
                    rwa = Pair.of(new RWA(addLpEvent.seqLinks_primary, addLpEvent.seqWavelengths_primary[0]), new RWA(addLpEvent.seqLinks_backup, addLpEvent.seqWavelengths_backup[0]));
                }
//				System.out.println ("------ ADD LP: rwa: " + rwa);
                if (rwa != null) {
                    for (Link e : rwa.getFirst().path)
                        if (wavelengthFiberOccupancy.get(rwa.getFirst().w, e.getIndex()) != 0)
                            throw new RuntimeException("Bad");
                    for (Link e : rwa.getSecond().path)
                        if (wavelengthFiberOccupancy.get(rwa.getSecond().w, e.getIndex()) != 0)
                            throw new RuntimeException("Bad");
                    for (Link e : rwa.getFirst().path)
                        if (e.getNetPlan() != currentNetPlan) throw new RuntimeException("Bad");
                    for (Link e : rwa.getSecond().path)
                        if (e.getNetPlan() != currentNetPlan) throw new RuntimeException("Bad");

                    final Demand wdmLayerDemand = addLpEvent.demand == null ? currentNetPlan.addDemand(addLpEvent.ingressNode, addLpEvent.egressNode, lineRateThisLp_Gbps, null, wdmLayer) : addLpEvent.demand;
                    final Route wdmLayerRoute = WDMUtils.addLightpathAndUpdateOccupancy(wdmLayerDemand, rwa.getFirst().path, lineRateThisLp_Gbps, rwa.getFirst().w, wavelengthFiberOccupancy);
                    final ProtectionSegment wdmLayerSegment = WDMUtils.addLightpathAsProtectionSegmentAndUpdateOccupancy(rwa.getSecond().path, rwa.getSecond().w, wavelengthFiberOccupancy);
                    checkDisjointness(wdmLayerRoute.getSeqLinksRealPath(), wdmLayerSegment.getSeqLinks(), protectionTypeCode);
                    wdmLayerRoute.addProtectionSegment(wdmLayerSegment);
                    this.wdmRouteOriginalRwa.put(wdmLayerRoute, rwa);
                    this.stat_numCarriedConnections++;
                    this.stat_trafficCarriedConnections += lineRateThisLp_Gbps;
                    addLpEvent.lpAddedToFillByProcessor = wdmLayerRoute;
                    checkClashing(currentNetPlan);
                }
                checkClashing(currentNetPlan);
            } else {
				/* The RWA may be computed by me, or mandated by the event */
                RWA rwa;
                if (addLpEvent.seqLinks_primary == null)
                    rwa = computeValidPathNewRoute(cplDemand, currentNetPlan);
                else {
                    if (!WDMUtils.isNonConflictingRWA(addLpEvent.seqLinks_primary, addLpEvent.seqWavelengths_primary, wavelengthFiberOccupancy))
                        throw new Net2PlanException("A request was received to add a conflicting lightpath");
                    rwa = new RWA(addLpEvent.seqLinks_primary, addLpEvent.seqWavelengths_primary[0]);
                }
//				System.out.println ("------ ADD LP: rwa: " + rwa);
                if (rwa != null) {
                    final Demand wdmLayerDemand = addLpEvent.demand == null ? currentNetPlan.addDemand(addLpEvent.ingressNode, addLpEvent.egressNode, lineRateThisLp_Gbps, null, wdmLayer) : addLpEvent.demand;
                    final Route wdmLayerRoute = WDMUtils.addLightpathAndUpdateOccupancy(wdmLayerDemand, rwa.path, lineRateThisLp_Gbps, rwa.w, wavelengthFiberOccupancy);
                    this.wdmRouteOriginalRwa.put(wdmLayerRoute, Pair.of(rwa, (RWA) null));
                    this.stat_numCarriedConnections++;
                    this.stat_trafficCarriedConnections += lineRateThisLp_Gbps;
                    addLpEvent.lpAddedToFillByProcessor = wdmLayerRoute;
//					System.out.println ("Added lp: " + wdmLayerRoute);
                }
            }
            checkClashing(currentNetPlan);
        } else if (event.getEventObject() instanceof WDMUtils.LightpathRemove) {
            WDMUtils.LightpathRemove lpEvent = (WDMUtils.LightpathRemove) event.getEventObject();
            final Route lpToRemove = lpEvent.lp;
            final Pair<RWA, RWA> originalRwa = wdmRouteOriginalRwa.get(lpToRemove);
            if (originalRwa == null)
                return; // the ligtpath was already removed, because it was first accepted, but then blocked because of failures
//			System.out.println ("lpToRemvoe: " + lpToRemove + ", links: " + lpToRemove.getSeqLinksAndProtectionSegments() + ", w: " + Arrays.toString (WDMUtils.getLightpathSeqWavelengths(lpToRemove)));
//			System.out.println ("wavelengthFiberOccupancy: " + wavelengthFiberOccupancy);
            if (isRestorationRecovery) // restoration => the current route is the one to release
                removeRoute_restoration(lpToRemove, true);
            else
                removeRoute_protection(lpToRemove, originalRwa);
//			if (!WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,true,wdmLayer).equals(wavelengthFiberOccupancy)) 
//			{
//				System.out.println ("WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,wdmLayer): " + WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,true,wdmLayer));
//				System.out.println ("wavelengthFiberOccupancy: " + wavelengthFiberOccupancy);
//				throw new RuntimeException ("Bad");
//			}
            checkClashing(currentNetPlan);
//			WDMUtils.checkConsistency(currentNetPlan , true , wdmLayer);
        } else if (event.getEventObject() instanceof SimEvent.NodesAndLinksChangeFailureState) {
            SimEvent.NodesAndLinksChangeFailureState ev = (SimEvent.NodesAndLinksChangeFailureState) event.getEventObject();

//			System.out.println ("----------- Event NodesAndLinksChangeFailureState: links up" + ev.linksUp + ", links down: " + ev.linksDown);

            checkClashing(currentNetPlan);

			/* This automatically sets as up the routes affected by a repair in its current path, and sets as down the affected by a failure in its current path */
            Set<Route> routesFromDownToUp = currentNetPlan.getRoutesDown(wdmLayer);

            currentNetPlan.setLinksAndNodesFailureState(ev.linksToUp, ev.linksToDown, ev.nodesToUp, ev.nodesToDown);
            routesFromDownToUp.removeAll(currentNetPlan.getRoutesDown(wdmLayer));

//			if (!WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,true,wdmLayer).equals(wavelengthFiberOccupancy)) 
//			{
//				System.out.println ("WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,wdmLayer): " + WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,true,wdmLayer));
//				System.out.println ("wavelengthFiberOccupancy: " + wavelengthFiberOccupancy);
//				System.out.println ("1 minus 2: " + WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,true,wdmLayer).assign(wavelengthFiberOccupancy , DoubleFunctions.minus));
//				throw new RuntimeException ("Bad");
//			}

            checkClashing(currentNetPlan);

			/* If something failed, and I am supposed to use protection or restoration... */
            if (isProtectionRecovery || isRestorationRecovery) {
//				System.out.println ("currentNetPlan.getRoutesDown(wdmLayer): " + currentNetPlan.getRoutesDown(wdmLayer));
				
				/* Try to reroute the routes that are still failing */
                for (Route r : new HashSet<Route>(currentNetPlan.getRoutesDown(wdmLayer))) {
//					if (isRestorationRecovery)
//						WDMUtils.releaseResources(r.getSeqLinksRealPath() , WDMUtils.getLightpathSeqWavelengths(r) , wavelengthFiberOccupancy, null , null);

                    this.stat_numAttemptedToRecoverConnections++;
                    this.stat_trafficAttemptedToRecoverConnections += r.getCarriedTrafficInNoFailureState();
                    final Demand cplDemand = this.cplWdm.getNodePairDemands(cplWdm.getNodeFromId(r.getIngressNode().getId()), cplWdm.getNodeFromId(r.getEgressNode().getId()), false).iterator().next();
                    RWA rwa = isProtectionRecovery ? computeValidReroute_protection(r) : computeValidPathNewRoute(cplDemand, currentNetPlan);
//					System.out.println ("Route down: " + r + ", r.getSeqLinksAndProtectionSegments() " + r.getSeqLinksAndProtectionSegments() + ", r.getSeqLinksRealPath(): " + r.getSeqLinksRealPath() + ", r.wavelengths: " + Arrays.toString (WDMUtils.getLightpathSeqWavelengths(r)));
                    if (rwa != null) {
//						System.out.println ("Pair<List<Link>,Integer> rwa: " + rwa);
//						if (!(rwa.path.get(0) instanceof ProtectionSegment)) for (Link e: rwa.path) if (wavelengthFiberOccupancy.get(rwa.w , e.getIndex ()) != 0) { System.out.println (wavelengthFiberOccupancy); throw new RuntimeException ("Bad"); }
                        if (isRestorationRecovery) {
                            WDMUtils.releaseResources(r.getSeqLinksRealPath(), WDMUtils.getLightpathSeqWavelengths(r), wavelengthFiberOccupancy, null, null);
                            WDMUtils.allocateResources(rwa.path, IntFactory1D.dense.make(E_wdm, rwa.w).toArray(), wavelengthFiberOccupancy, null, null);
                        }
                        r.setSeqLinksAndProtectionSegments(rwa.path);
                        WDMUtils.setLightpathSeqWavelengths(r, rwa.w);

                        this.stat_numSuccessfullyRecoveredConnections++;
                        this.stat_trafficSuccessfullyRecoveredConnections += r.getCarriedTrafficInNoFailureState();
                        checkClashing(currentNetPlan);
                    }
//					else 
//					{
//						if (isRestorationRecovery) // restoration => the current route is the one to release
//							removeRoute_restoration(r , false);
//						else
//							removeRoute_protection(r , wdmRouteOriginalRwa.get(r));
////						if (!WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,true,wdmLayer).equals(wavelengthFiberOccupancy)) 
////						{
////							System.out.println ("WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,wdmLayer): " + WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,true,wdmLayer));
////							System.out.println ("wavelengthFiberOccupancy: " + wavelengthFiberOccupancy);
////							System.out.println ("1 minus 2: " + WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,true,wdmLayer).copy ().assign(wavelengthFiberOccupancy , DoubleFunctions.minus));
////							System.out.println ("Route : " + r + ", w: " + Arrays.toString (WDMUtils.getLightpathSeqWavelengths(r)) + ", seq links real: " + r.getSeqLinksRealPath() + ", r.get seq links and prot: " + r.getSeqLinksAndProtectionSegments() + ", links down: " + currentNetPlan.getLinksDownAllLayers());
////							throw new RuntimeException ("Bad");
////						}
//						checkClashing (currentNetPlan);
////						WDMUtils.checkConsistency(currentNetPlan , true , wdmLayer);
//					}
                }
            }
//			if (!WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,true,wdmLayer).equals(wavelengthFiberOccupancy)) 
//			{
//				System.out.println ("WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,wdmLayer): " + WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,true,wdmLayer));
//				System.out.println ("wavelengthFiberOccupancy: " + wavelengthFiberOccupancy);
//				System.out.println ("1 minus 2: " + WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,true,wdmLayer).copy ().assign(wavelengthFiberOccupancy , DoubleFunctions.minus));
//				throw new RuntimeException ("Bad");
//			}
            checkClashing(currentNetPlan);
//			WDMUtils.checkConsistency(currentNetPlan , true, wdmLayer);
        } else if (event.getEventObject() instanceof SimEvent.DemandModify) {
            SimEvent.DemandModify ev = (SimEvent.DemandModify) event.getEventObject();
            Demand d = ev.demand;
            if (ev.modificationIsRelativeToCurrentOfferedTraffic)
                d.setOfferedTraffic(d.getOfferedTraffic() + ev.offeredTraffic);
            else
                d.setOfferedTraffic(ev.offeredTraffic);
//			if (!WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,true,wdmLayer).equals(wavelengthFiberOccupancy)) 
//			{
//				System.out.println ("WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,wdmLayer): " + WDMUtils.getMatrixWavelength2FiberOccupancy(currentNetPlan,true,wdmLayer));
//				System.out.println ("wavelengthFiberOccupancy: " + wavelengthFiberOccupancy);
//				throw new RuntimeException ("Bad");
//			}
            checkClashing(currentNetPlan);
//			WDMUtils.checkConsistency(currentNetPlan , true,wdmLayer);
        } else if (event.getEventObject() instanceof SimEvent.DemandRemove) {
            SimEvent.DemandRemove ev = (SimEvent.DemandRemove) event.getEventObject();
            Demand d = ev.demand;
            for (Route lpToRemove : d.getRoutes()) {
                final Pair<RWA, RWA> originalRwa = wdmRouteOriginalRwa.get(lpToRemove);
                if (originalRwa == null)
                    return; // the ligtpath was already removed, because it was first accepted, but then blocked because of failures
                if (isRestorationRecovery) // restoration => the current route is the one to release
                    removeRoute_restoration(lpToRemove, true);
                else
                    removeRoute_protection(lpToRemove, originalRwa);
            }
            checkClashing(currentNetPlan);
            d.remove();
            checkClashing(currentNetPlan);
        } else throw new Net2PlanException("Unknown event type: " + event);
    }

    @Override
    public String finish(StringBuilder output, double simTime) {
        final double trafficBlockingOnConnectionSetup = stat_trafficOfferedConnections == 0 ? 0 : 1 - (stat_trafficCarriedConnections / stat_trafficOfferedConnections);
        final double connectionBlockingOnConnectionSetup = stat_numOfferedConnections == 0 ? 0.0 : 1 - (((double) stat_numCarriedConnections) / ((double) stat_numOfferedConnections));
        final double successProbabilityRecovery = stat_numAttemptedToRecoverConnections == 0 ? 0.0 : (((double) stat_numSuccessfullyRecoveredConnections) / ((double) stat_numAttemptedToRecoverConnections));
        final double successProbabilityTrafficRecovery = stat_trafficAttemptedToRecoverConnections == 0 ? 0.0 : (((double) stat_trafficSuccessfullyRecoveredConnections) / ((double) stat_trafficAttemptedToRecoverConnections));
        final double dataTime = simTime - stat_transitoryInitTime;
        if (dataTime <= 0) {
            output.append("<p>No time for data acquisition</p>");
            return "";
        }

        output.append(String.format("<p>Total traffic of offered connections: number connections %d, total time average traffic: %f</p>", stat_numOfferedConnections, stat_trafficOfferedConnections / dataTime));
        output.append(String.format("<p>Total traffic of carried connections: number connections %d, total time average traffic: %f</p>", stat_numCarriedConnections, stat_trafficCarriedConnections / dataTime));
        output.append(String.format("<p>Blocking at connection setup: Probability of blocking a connection: %f, Fraction of blocked traffic: %f</p>", connectionBlockingOnConnectionSetup, trafficBlockingOnConnectionSetup));
        output.append(String.format("<p>Number connections attempted to recover: %d (summing time average traffic: %f). </p>", stat_numAttemptedToRecoverConnections, stat_trafficAttemptedToRecoverConnections / dataTime));
        output.append(String.format("<p>Number connections successfully recovered: %d (summing time average traffic: %f). </p>", stat_numSuccessfullyRecoveredConnections, stat_trafficSuccessfullyRecoveredConnections / dataTime));
        output.append(String.format("<p>Probability of successfully recover a connection: %f. Proability weighted by the connection traffic: %f). </p>", successProbabilityRecovery, successProbabilityTrafficRecovery));
//		output.append (String.format("<p>Total traffic carried at current state: %f. </p>", -1);
        return "";
    }

    @Override
    public void finishTransitory(double simTime) {
        this.stat_trafficOfferedConnections = 0;
        this.stat_trafficCarriedConnections = 0;
        this.stat_trafficAttemptedToRecoverConnections = 0;
        this.stat_trafficSuccessfullyRecoveredConnections = 0;
        this.stat_numOfferedConnections = 0;
        this.stat_numCarriedConnections = 0;
        this.stat_numAttemptedToRecoverConnections = 0;
        this.stat_numSuccessfullyRecoveredConnections = 0;
        this.stat_transitoryInitTime = simTime;
    }

    /* down links or segments cannot be used */
    private RWA computeValidPathNewRoute(Demand cplDemand, NetPlan currentNetPlan) {
//		System.out.println ("computeValidPathNewRoute, demand: " + demand + " occupied: " + occupiedLinkCapacity);
		/* If load sharing */
        if (isLoadSharing) {
            final ArrayList<Route> paths = new ArrayList<Route>(cplDemand.getRoutes());
            final int randomChosenIndex = rng.nextInt(paths.size());
            final Route cplRoute = paths.get(randomChosenIndex);
            final List<Link> seqLinksThisNetPlan = cplToNetPlanLinks(cplRoute.getSeqLinksRealPath(), currentNetPlan);
            final Triple<Boolean, Integer, Double> rwaEval = computeValidWAFirstFit_path(seqLinksThisNetPlan);
            if (rwaEval.getFirst()) return new RWA(seqLinksThisNetPlan, rwaEval.getSecond());
            else return null;
        } else if (isAlternateRouting || isLeastCongestedRouting) {
			/* If alternate or LCR */
            RWA lcrSoFar = null;
            double lcrIdleCapacitySoFar = -Double.MAX_VALUE;
            for (Route cplRoute : cplDemand.getRoutes()) {
                final List<Link> seqLinks = cplToNetPlanLinks(cplRoute.getSeqLinksRealPath(), currentNetPlan);
                final Triple<Boolean, Integer, Double> rwaEval = computeValidWAFirstFit_path(seqLinks);
//				System.out.println ("Route: " + cplRoute + ", links : " + seqLinks + ", eval: " + rwaEval);
                if (!rwaEval.getFirst()) continue;
                if (isAlternateRouting) return new RWA(seqLinks, rwaEval.getSecond());
                if (rwaEval.getThird() > lcrIdleCapacitySoFar) {
                    lcrIdleCapacitySoFar = rwaEval.getThird();
                    lcrSoFar = new RWA(seqLinks, rwaEval.getSecond());
                }
            }
            return (isAlternateRouting || (lcrSoFar == null)) ? (RWA) null : lcrSoFar;
        } else if (isSrgDisjointAwareLpRouting) {
            final Node thisNpIngressNode = currentNetPlan.getNodeFromId(cplDemand.getIngressNode().getId());
            final Node thisNpEgressNode = currentNetPlan.getNodeFromId(cplDemand.getEgressNode().getId());
            RWA chosenRWA = null;
            Set<Route> existingRoutesBetweenPairOfNodes = currentNetPlan.getNodePairRoutes(thisNpIngressNode, thisNpEgressNode, false, wdmLayer);
            int cplChosenRoute_numSRGOverlappingRoutes = Integer.MAX_VALUE;
            for (Route cplCandidateRoute : cplDemand.getRoutes()) {
//				if (forbiddenCplRoutes.contains (cplCandidateRoute)) continue;
				/* Check that the route has an available wavlength */
                final List<Link> seqLinks = cplToNetPlanLinks(cplCandidateRoute.getSeqLinksRealPath(), currentNetPlan);
                final Triple<Boolean, Integer, Double> rwaEval = computeValidWAFirstFit_path(seqLinks);
                if (!rwaEval.getFirst()) continue;
				/* compute the number of SRG-route pairs this route overlaps with, respect to the already existing lightpaths  */
                final DoubleMatrix1D affectingSRGs = cplA_rs.viewRow(cplCandidateRoute.getIndex());
                int numOverlappingSRGs = 0;
                for (Route currentRoute : existingRoutesBetweenPairOfNodes)
                    for (SharedRiskGroup srg : currentRoute.getSRGs())
                        if (affectingSRGs.get(srg.getIndex()) == 1) numOverlappingSRGs++;
                if (numOverlappingSRGs < cplChosenRoute_numSRGOverlappingRoutes) {
                    cplChosenRoute_numSRGOverlappingRoutes = numOverlappingSRGs;
                    chosenRWA = new RWA(seqLinks, rwaEval.getSecond());
                    if (numOverlappingSRGs == 0) break; // no need to search more
                }
            }
            return chosenRWA;
        }
        throw new RuntimeException("Bad");
    }

    private Pair<RWA, RWA> computeValid11PathPairNewRoute(Demand cplDemand, NetPlan currentNetPlan) {
//		final List<Pair<List<Link>,List<Link>>> pathPairs = cpl11.get(demand);
		/* If load sharing */
        if (isLoadSharing) {
            final ArrayList<Route> primaryRoutes = new ArrayList<Route>(cplDemand.getRoutes());
            final int randomChosenIndex = rng.nextInt(primaryRoutes.size());
            final Route cplRoute = primaryRoutes.get(randomChosenIndex);
            final ProtectionSegment cplSegment = cplRoute.getPotentialBackupProtectionSegments().iterator().next();
            final List<Link> seqLinksPrimaryThisNetPlan = cplToNetPlanLinks(cplRoute.getSeqLinksRealPath(), currentNetPlan);
            final List<Link> seqLinksBackupThisNetPlan = cplToNetPlanLinks(cplSegment.getSeqLinks(), currentNetPlan);
            final Quadruple<Boolean, Integer, Integer, Double> rwaEval = computeValidWAFirstFit_pathPair(seqLinksPrimaryThisNetPlan, seqLinksBackupThisNetPlan);
            if (rwaEval.getFirst())
                return Pair.of(new RWA(seqLinksPrimaryThisNetPlan, rwaEval.getSecond()), new RWA(seqLinksBackupThisNetPlan, rwaEval.getThird()));
            else
                return null;
        } else if (isAlternateRouting || isLeastCongestedRouting) {
            Pair<RWA, RWA> lcrSoFar = null;
            double lcrIdleCapacitySoFar = -Double.MAX_VALUE;
            for (Route cplRoute : cplDemand.getRoutes()) {
                if (cplRoute.getPotentialBackupProtectionSegments().isEmpty()) throw new RuntimeException("Bad");
                final ProtectionSegment cplSegment = cplRoute.getPotentialBackupProtectionSegments().iterator().next();
                final List<Link> seqLinksPrimaryThisNetPlan = cplToNetPlanLinks(cplRoute.getSeqLinksRealPath(), currentNetPlan);
                final List<Link> seqLinksBackupThisNetPlan = cplToNetPlanLinks(cplSegment.getSeqLinks(), currentNetPlan);
                final Quadruple<Boolean, Integer, Integer, Double> rwaEval = computeValidWAFirstFit_pathPair(seqLinksPrimaryThisNetPlan, seqLinksBackupThisNetPlan);
//				System.out.println ("Route: " + cplRoute + ", links primary: " + seqLinksPrimaryThisNetPlan + ", backup: " + seqLinksBackupThisNetPlan +  ", eval: " + rwaEval);
                if (!rwaEval.getFirst()) continue;
                if (isAlternateRouting)
                    return Pair.of(new RWA(seqLinksPrimaryThisNetPlan, rwaEval.getSecond()), new RWA(seqLinksBackupThisNetPlan, rwaEval.getThird()));
                if (rwaEval.getFourth() > lcrIdleCapacitySoFar) {
                    lcrIdleCapacitySoFar = rwaEval.getFourth();
                    lcrSoFar = Pair.of(new RWA(seqLinksPrimaryThisNetPlan, rwaEval.getSecond()), new RWA(seqLinksBackupThisNetPlan, rwaEval.getThird()));
                }
            }
            return lcrSoFar; //if alternate, this is null also
        } else if (isSrgDisjointAwareLpRouting) {
            final Node thisNpIngressNode = currentNetPlan.getNodeFromId(cplDemand.getIngressNode().getId());
            final Node thisNpEgressNode = currentNetPlan.getNodeFromId(cplDemand.getEgressNode().getId());
            Pair<RWA, RWA> chosenRWA = null;
            Set<Route> existingRoutesBetweenPairOfNodes = currentNetPlan.getNodePairRoutes(thisNpIngressNode, thisNpEgressNode, false, wdmLayer);
            int cplChosenRoute_numSRGOverlappingRoutes = Integer.MAX_VALUE;
            for (Route cplCandidateRoute : cplDemand.getRoutes()) {
                if (cplCandidateRoute.getPotentialBackupProtectionSegments().isEmpty())
                    throw new RuntimeException("Bad");
                final ProtectionSegment cplSegment = cplCandidateRoute.getPotentialBackupProtectionSegments().iterator().next();
                final List<Link> seqLinksPrimaryThisNetPlan = cplToNetPlanLinks(cplCandidateRoute.getSeqLinksRealPath(), currentNetPlan);
                final List<Link> seqLinksBackupThisNetPlan = cplToNetPlanLinks(cplSegment.getSeqLinks(), currentNetPlan);
                final Quadruple<Boolean, Integer, Integer, Double> rwaEval = computeValidWAFirstFit_pathPair(seqLinksPrimaryThisNetPlan, seqLinksBackupThisNetPlan);
//				System.out.println ("Route: " + cplCandidateRoute + ", links primary: " + seqLinksPrimaryThisNetPlan + ", backup: " + seqLinksBackupThisNetPlan +  ", eval: " + rwaEval);
                if (!rwaEval.getFirst()) continue;
				/* compute the number of SRG-route pairs this route overlaps with, respect to the already existing lightpaths  */
                final DoubleMatrix1D affectingSRGs_primary = cplA_rs.viewRow(cplCandidateRoute.getIndex());
                final DoubleMatrix1D affectingSRGs_backup = cplA_pss.viewRow(cplSegment.getIndex());
                int numOverlappingSRGs = 0;
                for (Route currentRoute : existingRoutesBetweenPairOfNodes) {
                    for (SharedRiskGroup srg : currentRoute.getSRGs())
                        if (affectingSRGs_primary.get(srg.getIndex()) == 1) numOverlappingSRGs++;
                    final ProtectionSegment currentSegment = currentRoute.getPotentialBackupProtectionSegments().iterator().next();
                    for (SharedRiskGroup srg : currentSegment.getSRGs())
                        if (affectingSRGs_backup.get(srg.getIndex()) == 1) numOverlappingSRGs++;
                }
                if (numOverlappingSRGs < cplChosenRoute_numSRGOverlappingRoutes) {
                    cplChosenRoute_numSRGOverlappingRoutes = numOverlappingSRGs;
                    chosenRWA = Pair.of(new RWA(seqLinksPrimaryThisNetPlan, rwaEval.getSecond()), new RWA(seqLinksBackupThisNetPlan, rwaEval.getThird()));
                    if (numOverlappingSRGs == 0) break; // no need to search more
                }
            }
            return chosenRWA;
        }
        throw new RuntimeException("Bad");
    }

    /* down links or segments cannot be used */
    private RWA computeValidReroute_protection(Route routeToReroute) {
        final NetPlan np = routeToReroute.getNetPlan();
        final Pair<RWA, RWA> originalRWA = wdmRouteOriginalRwa.get(routeToReroute);
        final boolean primaryUp = np.isUp(originalRWA.getFirst().path);
        final boolean backupUp = originalRWA.getSecond() == null ? false : np.isUp(originalRWA.getSecond().path);
        if (primaryUp) {
            if (originalRWA.getFirst().path.contains(null)) throw new RuntimeException("Bad");
            return originalRWA.getFirst();
        } else if (backupUp) {
            List<Link> res = new LinkedList<Link>();
            res.add(routeToReroute.getPotentialBackupProtectionSegments().iterator().next());
            if (routeToReroute.getPotentialBackupProtectionSegments().iterator().next() == null)
                throw new RuntimeException("Bad");
            if (res.contains(null)) throw new RuntimeException("Bad");
            return new RWA(res, originalRWA.getSecond().w);
        } else return null;
    }


    private List<Link> cplToNetPlanLinks(List<Link> cplLinkList, NetPlan netPlan) {
        List<Link> list = new LinkedList<>();
        for (Link cplLink : cplLinkList) list.add(netPlan.getLinkFromId(cplLink.getId()));
        return list;
    }

    private Triple<Boolean, Integer, Double> computeValidWAFirstFit_path(List<Link> seqLinks) {
        if (seqLinks.isEmpty()) throw new RuntimeException("Bad");
        if (seqLinks.get(0).getOriginNode().isDown()) return Triple.of(false, -1, -1.0);
        double worseCaseSpareCapacity = Double.MAX_VALUE;
        for (Link e : seqLinks) {
            worseCaseSpareCapacity = Math.min(worseCaseSpareCapacity, e.getCapacity() - e.getOccupiedCapacityIncludingProtectionSegments());
            if (e.getDestinationNode().isDown() || e.isDown()) return Triple.of(false, -1, -1.0);
        }
        int[] seqWavelengths = WDMUtils.WA_firstFit(seqLinks, wavelengthFiberOccupancy);
        if (seqWavelengths.length != 0)
            return Triple.of(true, seqWavelengths[0], worseCaseSpareCapacity);
        else return Triple.of(false, -1, -1.0);
    }

    private Quadruple<Boolean, Integer, Integer, Double> computeValidWAFirstFit_pathPair(List<Link> seqLinks_1, List<Link> seqLinks_2) {
        if (seqLinks_1.isEmpty() || seqLinks_2.isEmpty()) throw new RuntimeException("Bad");
        if (seqLinks_1.get(0).getOriginNode().isDown()) return Quadruple.of(false, -1, -1, -1.0);
        if (seqLinks_2.get(0).getOriginNode().isDown()) return Quadruple.of(false, -1, -1, -1.0);
        double worseCaseSpareCapacity = Double.MAX_VALUE;
        for (Link e : seqLinks_1) {
            worseCaseSpareCapacity = Math.min(worseCaseSpareCapacity, e.getCapacity() - e.getOccupiedCapacityIncludingProtectionSegments());
            if (e.getDestinationNode().isDown() || e.isDown()) return Quadruple.of(false, -1, -1, -1.0);
        }
        for (Link e : seqLinks_2) {
            worseCaseSpareCapacity = Math.min(worseCaseSpareCapacity, e.getCapacity() - e.getOccupiedCapacityIncludingProtectionSegments());
            if (e.getDestinationNode().isDown() || e.isDown()) return Quadruple.of(false, -1, -1, -1.0);
        }
        Pair<int[], int[]> seqWavelengths = WDMUtils.WA_firstFitTwoRoutes(seqLinks_1, seqLinks_2, wavelengthFiberOccupancy);
        if (seqWavelengths != null)
            return Quadruple.of(true, seqWavelengths.getFirst()[0], seqWavelengths.getSecond()[0], worseCaseSpareCapacity);
        else return Quadruple.of(false, -1, -1, -1.0);
    }

    private class RWA {
        public final List<Link> path;
        public final int w;

        public RWA(List<Link> path, int w) {
            this.path = new LinkedList<Link>(path);
            this.w = w;
        }

        public String toString() {
            return "Links: " + path + ", w: " + w;
        }
    }

    private void removeRoute_restoration(Route lpToRemove, boolean releaseWDMResources) {
        if (releaseWDMResources)
            WDMUtils.releaseResources(lpToRemove.getSeqLinksRealPath(), WDMUtils.getLightpathSeqWavelengths(lpToRemove), wavelengthFiberOccupancy, null, null);
        lpToRemove.remove();
        this.wdmRouteOriginalRwa.remove(lpToRemove);
    }

    private void removeRoute_protection(Route lpToRemove, Pair<RWA, RWA> originalRwa) {
		/* Release WDM resources */
        WDMUtils.releaseResources(originalRwa.getFirst().path, IntFactory1D.dense.make(E_wdm, originalRwa.getFirst().w).toArray(), wavelengthFiberOccupancy, null, null);
        if (originalRwa.getSecond() != null)
            WDMUtils.releaseResources(originalRwa.getSecond().path, IntFactory1D.dense.make(E_wdm, originalRwa.getSecond().w).toArray(), wavelengthFiberOccupancy, null, null);
		/* Release NetPlan resources */
        lpToRemove.remove();
        for (ProtectionSegment s : lpToRemove.getPotentialBackupProtectionSegments()) s.remove();
        this.wdmRouteOriginalRwa.remove(lpToRemove);
    }

    private void checkClashing(NetPlan np) {
        DoubleMatrix2D mat = DoubleFactory2D.dense.make(wavelengthFiberOccupancy.rows(), E_wdm);
        for (Route r : np.getRoutes(wdmLayer)) {


            if (newRoutesHave11Protection) {
                Pair<RWA, RWA> origRouting = wdmRouteOriginalRwa.get(r);
//				System.out.println ("Route " + r + ", origRouting: " + origRouting);
                for (Link e : origRouting.getFirst().path) {
                    if (mat.get(origRouting.getFirst().w, e.getIndex()) != 0) throw new RuntimeException("Bad");
                    mat.set(origRouting.getFirst().w, e.getIndex(), 1);
                }
                if (origRouting.getSecond() != null) {
                    if (!r.getSeqLinksRealPath().equals(origRouting.getFirst().path) && !r.getPotentialBackupProtectionSegments().iterator().next().getSeqLinks().equals(origRouting.getSecond().path))
                        throw new RuntimeException("Bad");
                    for (Link e : origRouting.getSecond().path) {
                        if (mat.get(origRouting.getSecond().w, e.getIndex()) != 0) throw new RuntimeException("Bad");
                        mat.set(origRouting.getSecond().w, e.getIndex(), 1);
                    }
                } else if (!r.getSeqLinksRealPath().equals(origRouting.getFirst().path))
                    throw new RuntimeException("Bad");

            } else {
                final int[] wavelengths = WDMUtils.getLightpathSeqWavelengths(r);
                int counter = 0;
                for (Link e : r.getSeqLinksRealPath()) {
                    final int w = wavelengths[counter++];
                    if (mat.get(w, e.getIndex()) != 0) throw new RuntimeException("Bad");
                    mat.set(w, e.getIndex(), 1);
                }
            }
        }
        if (!mat.equals(wavelengthFiberOccupancy)) {
            System.out.println("mat: " + mat);
            System.out.println("wavelengthFiberOccupancy: " + wavelengthFiberOccupancy);
            System.out.println("1 minus 2: " + mat.copy().assign(wavelengthFiberOccupancy, DoubleFunctions.minus));
            throw new RuntimeException("Bad");
        }

    }

    private void checkDisjointness(List<Link> p1, List<Link> p2, int disjointnessType) {
        if ((p1 == null) || (p2 == null)) throw new RuntimeException("Bad");
        if (disjointnessType == 0) // SRG disjoint
        {
            Set<SharedRiskGroup> srg1 = new HashSet<SharedRiskGroup>();
            for (Link e : p1) srg1.addAll(e.getSRGs());
            for (Link e : p2)
                for (SharedRiskGroup srg : e.getSRGs()) if (srg1.contains(srg)) throw new RuntimeException("Bad");
        } else if (disjointnessType == 1) // link and node
        {
            Set<NetworkElement> resourcesP1 = new HashSet<NetworkElement>();
            boolean firstLink = true;
            for (Link e : p1) {
                resourcesP1.add(e);
                if (firstLink) firstLink = false;
                else resourcesP1.add(e.getOriginNode());
            }
            for (Link e : p2) if (resourcesP1.contains(e)) throw new RuntimeException("Bad");
        } else if (disjointnessType == 2) // link
        {
            Set<Link> linksP1 = new HashSet<Link>(p1);
            for (Link e : p2) if (linksP1.contains(e)) throw new RuntimeException("Bad: p1: " + p1 + ", p2: " + p2);
        } else throw new RuntimeException("Bad");

    }
}
