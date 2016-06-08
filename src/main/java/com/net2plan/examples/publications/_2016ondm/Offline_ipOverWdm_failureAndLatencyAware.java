/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mari�o.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mari�o - initial API and implementation
 ******************************************************************************/


package com.net2plan.examples.publications._2016ondm;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import com.net2plan.examples.general.onlineSim.Online_evProc_ipOverWdm;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.libraries.IPUtils;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.utils.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Offline_ipOverWdm_failureAndLatencyAware implements IAlgorithm {

    private NetworkLayer wdmLayer;
    private NetworkLayer ipLayer;
    private double PRECISION_FACTOR;
    private DoubleMatrix1D h_d;
    private Random rng;

    private InputParameter setSRGsAsBidirectionalLinkFiber = new InputParameter("setSRGsAsBidirectionalLinkFiber", true, "Indicates whether the design configures bidirectional fiber SRGs");
    private InputParameter bidirectionalLightpaths = new InputParameter("bidirectionalLightpaths", true, "If rtue all lightpaths are bidirectional, and both directions follow the same route and have the same wavelengths");
    private InputParameter designShouldBeTolerantToSingleSRGFailure = new InputParameter("designShouldBeTolerantToSingleSRGFailure", true, "If true, the design should be tolerant to single SRG failures. If not, just be valid in the no-failure scenario");
    private InputParameter randomSeed = new InputParameter("randomSeed", (long) 1, "Seed for the random generator (-1 means random)");

    private InputParameter wdmNumWavelengthsPerFiber = new InputParameter("wdmNumWavelengthsPerFiber", (int) 40, "Set the number of wavelengths per link. If < 1, the number of wavelengths set in the input file is used.");
    private InputParameter wdmRwaType = new InputParameter("wdmRwaType", "#select# srg-disjointness-aware-route-first-fit alternate-routing load-sharing", "Criteria to decide the route of a connection among the available paths");
    //	private InputParameter wdmRwaType = new InputParameter ("wdmRwaType", "#select# srg-disjointness-aware-route-first-fit alternate-routing least-congested-routing load-sharing" , "Criteria to decide the route of a connection among the available paths");
    private InputParameter wdmKShortestPathType = new InputParameter("wdmKShortestPathType", "#select# hops km", "Criteria to compute the shortest path. Valid values: 'hops' or 'km'");
    private InputParameter wdmK = new InputParameter("wdmK", (int) 5, "Maximum number of admissible paths per demand", 1, Integer.MAX_VALUE);
    private InputParameter wdmMaxLightpathLengthInKm = new InputParameter("wdmMaxLightpathLengthInKm", (double) -1, "Lightpaths longer than this are considered not admissible. A non-positive number means this limit does not exist");
    private InputParameter wdmMaxLightpathNumHops = new InputParameter("wdmMaxLightpathNumHops", (int) -1, "A lightpath cannot have more than this number of hops. A non-positive number means this limit does not exist");
    private InputParameter wdmLayerIndex = new InputParameter("wdmLayerIndex", (int) 0, "Index of the WDM layer (-1 means default layer)");
    private InputParameter wdmProtectionTypeToNewRoutes = new InputParameter("wdmProtectionTypeToNewRoutes", "#select# none 1+1-link-disjoint 1+1-node-disjoint 1+1-srg-disjoint", "");
    private InputParameter wdmLineRatePerLightpath_Gbps = new InputParameter("wdmLineRatePerLightpath_Gbps", (double) 40, "All the lightpaths have this carried traffic by default, when a lightpath is added and this quantity is not specified in the add lightpath event", 0, false, Double.MAX_VALUE, true);
    private InputParameter ipLayerIndex = new InputParameter("ipLayerIndex", (int) 1, "Index of the layer containing IP network (-1 means default layer)");
    private InputParameter ipMaximumE2ELatencyMs = new InputParameter("ipMaximumE2ELatencyMs", (double) -1, "Maximum end-to-end latency of the traffic of an IP demand to consider it as lost traffic (a non-positive value means no limit)");

    private InputParameter ipOverWdmNetworkRecoveryType = new InputParameter("ipOverWdmNetworkRecoveryType", "#select# static-lps-OSPF-rerouting 1+1-lps-OSPF-rerouting lp-restoration-OSPF-rerouting", "The recovery type the network will apply. If static lps, the VT is overdimensioned to tolerate single SRG failures. In the 1+1 case, link disjoit backup lps are created. If lps are 1+1 protected or have lp restoration, the VT is dimensioned to carry all IP traffic in the no failure state.");

    private IEventProcessor ipOverWdmAlgorithm;

    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters) {
        /* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
        InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

        if (netPlan.getNumberOfLayers() == 1) {
            this.ipLayer = netPlan.addLayer("IP", "", "Gbps", "Gbps", null);
            this.wdmLayer = netPlan.getNetworkLayer((int) 0);
            wdmLayer.setName("WDM");
            ipLayer.setName("IP");
            for (Demand d : netPlan.getDemands(wdmLayer))
                netPlan.addDemand(d.getIngressNode(), d.getEgressNode(), d.getOfferedTraffic(), d.getAttributes(), ipLayer);
            netPlan.removeAllDemands(wdmLayer);
        } else if (netPlan.getNumberOfLayers() == 2) {
            this.wdmLayer = wdmLayerIndex.getInt() == -1 ? netPlan.getNetworkLayerDefault() : netPlan.getNetworkLayer(wdmLayerIndex.getInt());
            this.ipLayer = ipLayerIndex.getInt() == -1 ? netPlan.getNetworkLayerDefault() : netPlan.getNetworkLayer(ipLayerIndex.getInt());
        } else throw new Net2PlanException("Input design should have two layers: higher is IP, lower is WDM");
        if (ipLayer == wdmLayer) throw new Net2PlanException("IP and WDM layers cannot be the same!");

		/* Remove all routing information from all layers (routes and protection segments) and all links from IP layer */
        netPlan.removeAllUnicastRoutingInformation(ipLayer);
        netPlan.removeAllUnicastRoutingInformation(wdmLayer);
        netPlan.removeAllDemands(wdmLayer);
        netPlan.removeAllLinks(ipLayer);
        netPlan.setLinkCapacityUnitsName("Gbps", ipLayer);
        netPlan.setLinkCapacityUnitsName("Wavelengths", wdmLayer);
        netPlan.setDemandTrafficUnitsName("Gbps", ipLayer);
        netPlan.setDemandTrafficUnitsName("Gbps", wdmLayer);

        final int N = netPlan.getNumberOfNodes();
        final int E_f = netPlan.getNumberOfLinks(wdmLayer);
        final int D_ip = netPlan.getNumberOfDemands(ipLayer);
        this.h_d = netPlan.getVectorDemandOfferedTraffic(ipLayer);
        this.PRECISION_FACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));
        if (N == 0 || E_f == 0 || D_ip == 0)
            throw new Net2PlanException("This algorithm requires a complete topology with nodes, links (WDM layer) and demands (IP layer)");

		/* Read input parameters */
        final int W = wdmNumWavelengthsPerFiber.getInt();
        if (randomSeed.getLong() == -1) randomSeed.initialize((long) RandomUtils.random(0, Long.MAX_VALUE - 1));
        this.rng = new Random(randomSeed.getLong());
	
		/* If routing type is SRG, setSRGAsBidiritectionalLinks is false and NO SRG is defined, exit */
        if (!setSRGsAsBidirectionalLinkFiber.getBoolean() && netPlan.getSRGIds().isEmpty())
            throw new Net2PlanException("Some SRGs must be defined");

		/* Initialize multilayer design (links and demands in the input design are fibers in the lower layer and demands in the upper layer, respectively) */
        wdmLayer.setName("WDM");
        netPlan.setRoutingType(Constants.RoutingType.SOURCE_ROUTING, wdmLayer);
        ipLayer.setName("IP");
        netPlan.setRoutingType(Constants.RoutingType.HOP_BY_HOP_ROUTING, ipLayer);
        netPlan.setNetworkLayerDefault(wdmLayer);
        for (Link fiber : netPlan.getLinks(wdmLayer)) WDMUtils.setFiberNumWavelengths(fiber, W); /* Set the number of wavelengths per fiber */

		/* Configure SRGs */
        if (setSRGsAsBidirectionalLinkFiber.getBoolean()) {
            netPlan.removeAllSRGs();
            SRGUtils.configureSRGs(netPlan, 1, 1, SRGUtils.SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE, true, wdmLayer);
        }
		
		/* Compute lower bound for in/out nodes */
        DoubleMatrix2D ipTrafficMatrix = netPlan.getMatrixNode2NodeOfferedTraffic(ipLayer);
        ipTrafficMatrix.zMult(DoubleFactory1D.dense.make(N, 1.0), null);
        DoubleMatrix1D lowerBound_outgoing = ipTrafficMatrix.zMult(DoubleFactory1D.dense.make(N, 1.0), null).assign(DoubleFunctions.div(wdmLineRatePerLightpath_Gbps.getDouble())).assign(DoubleFunctions.ceil);
        DoubleMatrix1D lowerBound_incoming = ipTrafficMatrix.viewDice().zMult(DoubleFactory1D.dense.make(N, 1.0), null).assign(DoubleFunctions.div(wdmLineRatePerLightpath_Gbps.getDouble())).assign(DoubleFunctions.ceil);

        this.ipOverWdmAlgorithm = new Online_evProc_ipOverWdm();

        Map<String, String> ipOverWdmAlgorithmParameters = InputParameter.createMapFromInputParameters(new InputParameter[]
                {wdmNumWavelengthsPerFiber, wdmRwaType, wdmKShortestPathType, wdmK, wdmMaxLightpathLengthInKm,
                        wdmMaxLightpathNumHops, wdmProtectionTypeToNewRoutes, wdmLineRatePerLightpath_Gbps, ipOverWdmNetworkRecoveryType, ipMaximumE2ELatencyMs});
        ipOverWdmAlgorithmParameters.put("ipLayerIndex", "" + ipLayer.getIndex());
        ipOverWdmAlgorithmParameters.put("wdmLayerIndex", "" + wdmLayer.getIndex());
        ipOverWdmAlgorithmParameters.put("wdmRemovePreviousLightpaths", "true");
        ipOverWdmAlgorithmParameters.put("wdmRandomSeed", "" + rng.nextLong());

        this.ipOverWdmAlgorithm.initialize(netPlan, ipOverWdmAlgorithmParameters, null, net2planParameters);

        /**** END INITIALIZATION ***************/

        /******
         * First phase. Create lightpaths to carry all traffic in all failure states (single SRG failure)
         ******/
        do {
            Quadruple<DoubleMatrix1D, DoubleMatrix1D, DoubleMatrix1D, Boolean> p = evaluateSolution(netPlan);
            final DoubleMatrix1D worseCaseBlocked_d = p.getFirst();
            final DoubleMatrix1D worstCaseOversubscription_e = p.getSecond();
            final DoubleMatrix1D worstCaseLatencyMs_d = p.getThird();
            final boolean isValid = p.getFourth();
            if (isValid) break;

			/*Compute blocked traffic between Node pairs */
//			System.out.println ("Total blocked: " + worseCaseBlocked_d.zSum() + ", worseCaseBlocked_d: " + worseCaseBlocked_d);
//			System.out.println ("Total oversubscription " + worstCaseOversubscription_e.zSum() + ", worstCaseOversubscription_e: " + worstCaseOversubscription_e);
//			System.out.println ("worstCaseLatencyMs_d: " + worstCaseLatencyMs_d);
//			
            final int randomInitialDemandBlocking = rng.nextInt(D_ip);
            int ipBlockedDemandIndex = -1;
            for (int cont = 0; cont < D_ip; cont++)
                if (worseCaseBlocked_d.get((randomInitialDemandBlocking + cont) % D_ip) > PRECISION_FACTOR) {
                    ipBlockedDemandIndex = (randomInitialDemandBlocking + cont) % D_ip;
                    break;
                }
            final int randomInitialDemandLatency = rng.nextInt(D_ip);
            int ipTooLongDemandIndex = -1;
            if (ipMaximumE2ELatencyMs.getDouble() > 0)
                for (int cont = 0; cont < D_ip; cont++)
                    if (worstCaseLatencyMs_d.get((randomInitialDemandLatency + cont) % D_ip) > ipMaximumE2ELatencyMs.getDouble() + PRECISION_FACTOR) {
                        ipTooLongDemandIndex = (randomInitialDemandLatency + cont) % D_ip;
                        break;
                    }
            final int E_ip = (int) worstCaseOversubscription_e.size();
            final int initialLink = E_ip == 0 ? 0 : rng.nextInt(E_ip);
            int ipOversubscribedLinkIndex = -1;
            for (int cont = 0; cont < E_ip; cont++)
                if (worstCaseOversubscription_e.get((initialLink + cont) % E_ip) > PRECISION_FACTOR) {
                    ipOversubscribedLinkIndex = (initialLink + cont) % E_ip;
                    break;
                }

            Pair<Node, Node> nodePair;
            if (ipBlockedDemandIndex != -1) {
//				System.out.println ("Add lp because of demand blocking: ipBlockedDemandIndex: " + ipBlockedDemandIndex);
                nodePair = Pair.of(netPlan.getDemand(ipBlockedDemandIndex, ipLayer).getIngressNode(), netPlan.getDemand(ipBlockedDemandIndex, ipLayer).getEgressNode());
            } else if (ipTooLongDemandIndex != -1) {
//				System.out.println ("Add lp because of ipTooLongDemandIndex: " + ipTooLongDemandIndex);
                nodePair = Pair.of(netPlan.getDemand(ipTooLongDemandIndex, ipLayer).getIngressNode(), netPlan.getDemand(ipTooLongDemandIndex, ipLayer).getEgressNode());
            } else if (ipOversubscribedLinkIndex != -1) {
//				System.out.println ("Add lp because of ipOversubscribedLinkIndex: " + ipOversubscribedLinkIndex);
                nodePair = Pair.of(netPlan.getLink(ipOversubscribedLinkIndex, ipLayer).getOriginNode(), netPlan.getLink(ipOversubscribedLinkIndex, ipLayer).getDestinationNode());
            } else throw new RuntimeException("Bad");
				
			/* Add a lightpath between the given node pair */
            final Node nodeWithinReach = computeClosestNodeToDestinationWithinMaximumReach(netPlan, nodePair.getFirst(), nodePair.getSecond());
            if (nodeWithinReach == null)
                throw new Net2PlanException("Bad. Not enough network capacity to complete the design.");
            if (nodeWithinReach != nodePair.getSecond()) {
                System.out.println("Unreachable node, try a closer one");
                nodePair.setSecond(nodeWithinReach);
            }
            final Route lp = addLightpathOrLp11Pair(nodePair.getFirst(), nodePair.getSecond());
            if (lp == null) throw new Net2PlanException("Bad. Not enough network capacity to complete the design.");
            if (bidirectionalLightpaths.getBoolean())
                if (addLightpathOrLp11Pair(nodePair.getSecond(), nodePair.getFirst()) == null)
                    throw new Net2PlanException("Bad. Not enough network capacity to complete the design.");

//			System.out.println ("Added lp between nodes: " + newLightpath.getOriginNode() + ", " + newLightpath.getDestinationNode());
        }
        while (true);

        if (!netPlan.getLinksDownAllLayers().isEmpty() || !netPlan.getNodesDown().isEmpty())
            throw new RuntimeException("Bad");

        if (ipOverWdmNetworkRecoveryType.getString().equals("1+1-lps-OSPF-rerouting")) {
            for (Route lp : netPlan.getRoutes(wdmLayer)) {
                final List<Link> originalRoute = lp.getInitialSequenceOfLinks();
                final int[] originalSeqWavelengths = WDMUtils.getLightpathSeqWavelengthsInitialRoute(lp);
                if (originalRoute.equals(lp.getSeqLinksRealPath()) && Arrays.equals(originalSeqWavelengths, WDMUtils.getLightpathSeqWavelengths(lp)))
                    continue;
				/* the current route is not the same os the original. this means the route is carried throgh the protection segment */
                if (!(lp.getSeqLinksAndProtectionSegments().get(0) instanceof ProtectionSegment))
                    throw new RuntimeException("Bad");
//				System.out.println ("Reverted to original RWA route " + lp );
                lp.revertToInitialSequenceOfLinks();
                WDMUtils.setLightpathSeqWavelengths(lp, originalSeqWavelengths);
            }
        }
		
		/* Compute final ECMP routing according to IGP weights */
        IPUtils.setECMPForwardingRulesFromLinkWeights(netPlan, null, ipLayer);

		/* Check consistency of WDM layer */
        WDMUtils.checkConsistency(netPlan, true, wdmLayer);

//		System.out.println("Number of lightpaths with protection againts single SRG failure (before grooming): " + netPlan.getNumberOfLinks(ipLayer));
//		System.out.println("Lower Bound Lightpaths (incoming): " + lowerBound_incoming + " total = " + lowerBound_incoming.zSum());
//		System.out.println("Lower Bound Lightpaths (outgoing): " + lowerBound_outgoing + " total = " + lowerBound_outgoing.zSum());

        return "Ok. Number of lightpaths (multiply by two if 1+1): " + netPlan.getNumberOfLinks(ipLayer) + ", worse e2e latency: " + (netPlan.getVectorDemandWorseCasePropagationTimeInMs(ipLayer).getMaxLocation()[0]);
//		return "Ok. Number of lightpath: " + netPlan.getNumberOfLinks(ipLayer) + ", lower bound: " + Math.max(lowerBound_incoming.zSum() , lowerBound_outgoing.zSum()) + ", worse e2e latency: " + (netPlan.getVectorDemandWorseCasePropagationTimeInMs(ipLayer).getMaxLocation() [0]);
    }

    /**
     * This function selects randomly 2 nodes, then deletes all links (IP layer) and creates new ones trying to assure SRG disjointness among them
     *
     * @param solution
     */


    private Quadruple<DoubleMatrix1D, DoubleMatrix1D, DoubleMatrix1D, Boolean> evaluateSolution(NetPlan netPlan) {
        final int E_ip = netPlan.getNumberOfLinks(ipLayer);
        if (E_ip == 0)
            return Quadruple.of(h_d, DoubleFactory1D.dense.make(0), DoubleFactory1D.dense.make((int) h_d.size()), false);

        if (!netPlan.getLinksDownAllLayers().isEmpty()) throw new RuntimeException("Bad");

        DoubleMatrix1D linkWeights = DoubleFactory1D.dense.make(E_ip, 1);
        IPUtils.setECMPForwardingRulesFromLinkWeights(netPlan, linkWeights, ipLayer);
        DoubleMatrix1D worseOversubscribe_e = netPlan.getVectorLinkOversubscribedTraffic(ipLayer);
        DoubleMatrix1D worseDemandBlockedTraffic_d = netPlan.getVectorDemandBlockedTraffic(ipLayer);
        DoubleMatrix1D worseE2ELatency_d = netPlan.getVectorDemandWorseCasePropagationTimeInMs(ipLayer);
		
		/* Compute the worse carried traffic per lightpath in any failure state */
        if (designShouldBeTolerantToSingleSRGFailure.getBoolean()) {
//			Set<Node> previousNodesDown = new HashSet<Node> ();
//			Set<Link> previousLinksDown = new HashSet<Link> ();
//			NetPlan npCopy = netPlan.copy ();
            for (SharedRiskGroup srg : netPlan.getSRGs()) {
                if (!netPlan.getLinksDownAllLayers().isEmpty() || !netPlan.getNodesDown().isEmpty())
                    throw new RuntimeException("Bad");

//				NetPlan thisSRGNetPlan = netPlan.copy();
//				ipOverWdmAlgorithm.initialize(thisSRGNetPlan, ipOverWdmAlgorithmParameters, null, net2planParameters); // TOO SLOW!! (CPL computation in each iteration)

//				previousLinksDown.removeAll (srg.getLinks ()); // this now contains the links to set as up (before down, not now)
//				previousNodesDown.removeAll (srg.getNodes ()); // this now contains the nodes to set as up (before down, not now)
                SimEvent.NodesAndLinksChangeFailureState ev = new SimEvent.NodesAndLinksChangeFailureState(null, srg.getNodes(), null, srg.getLinks());
                ipOverWdmAlgorithm.processEvent(netPlan, new SimEvent(0, SimEvent.DestinationModule.EVENT_PROCESSOR, -1, ev));

                worseOversubscribe_e.assign(netPlan.getVectorLinkOversubscribedTraffic(ipLayer), DoubleFunctions.max);
                worseDemandBlockedTraffic_d.assign(netPlan.getVectorDemandBlockedTraffic(ipLayer), DoubleFunctions.max);
                worseE2ELatency_d.assign(netPlan.getVectorDemandWorseCasePropagationTimeInMs(ipLayer), DoubleFunctions.max);

                ev = new SimEvent.NodesAndLinksChangeFailureState(srg.getNodes(), null, srg.getLinks(), null);
                ipOverWdmAlgorithm.processEvent(netPlan, new SimEvent(0, SimEvent.DestinationModule.EVENT_PROCESSOR, -1, ev));
            }
//			/* Leave the n2p objec with all up again */
//			SimEvent.NodesAndLinksChangeFailureState ev = new SimEvent.NodesAndLinksChangeFailureState(previousNodesDown , null , previousLinksDown , null);
//			ipOverWdmAlgorithm.processEvent(netPlan , new SimEvent(0 , SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , ev));
            if (!netPlan.getLinksDownAllLayers().isEmpty() || !netPlan.getNodesDown().isEmpty())
                throw new RuntimeException("Bad");
        }
        if (!netPlan.getLinksDownAllLayers().isEmpty()) throw new RuntimeException("Bad");
        final boolean hasBlocking = worseDemandBlockedTraffic_d.getMaxLocation()[0] > PRECISION_FACTOR;
        final boolean hasOversubscription = worseOversubscribe_e.getMaxLocation()[0] > PRECISION_FACTOR;
        final boolean hasExcessiveLatency = (ipMaximumE2ELatencyMs.getDouble() > 0) && (worseE2ELatency_d.getMaxLocation()[0] > ipMaximumE2ELatencyMs.getDouble());
//		System.out.println ("hasBlocking: " + hasBlocking + ", hasOversubscription: " + hasOversubscription + ", hasExcessiveLatency: " + hasExcessiveLatency);
        final boolean validDesign = (!hasBlocking) && (!hasOversubscription) && (!hasExcessiveLatency);
        return Quadruple.of(worseDemandBlockedTraffic_d, worseOversubscribe_e, worseE2ELatency_d, validDesign);
    }


    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public List<Triple<String, String, String>> getParameters() {
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
        return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
    }

    private Route addLightpathOrLp11Pair(Node n1, Node n2) {
        WDMUtils.LightpathAdd ev = new WDMUtils.LightpathAdd(n1, n2, wdmLayer, wdmLineRatePerLightpath_Gbps.getDouble());
        ipOverWdmAlgorithm.processEvent(n1.getNetPlan(), new SimEvent(0, SimEvent.DestinationModule.EVENT_PROCESSOR, -1, ev));
        return ev.lpAddedToFillByProcessor;
    }

    private Node computeClosestNodeToDestinationWithinMaximumReach(NetPlan np, Node originNode, Node destinationNode) {
        final List<Link> seqFibers = GraphUtils.getShortestPath(np.getNodes(), np.getLinks(wdmLayer), originNode, destinationNode, null);
        if (seqFibers.isEmpty()) return null;
        if (wdmMaxLightpathLengthInKm.getDouble() < 0) return destinationNode;
        double accumLength = 0;
        Node res = originNode;
        for (Link e : seqFibers)
            if (accumLength + e.getLengthInKm() > wdmMaxLightpathLengthInKm.getDouble()) return res;
            else {
                accumLength += e.getLengthInKm();
                res = e.getDestinationNode();
            }
        return destinationNode;
    }
}
