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
package es.upct.girtel.networks2014_IPoverWDMRestoration;


import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.libraries.IPUtils;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.utils.*;
import com.net2plan.utils.Constants.OrderingType;

import java.util.*;

/**
 * <p>This algorithm is used for dimensioning multilayer IP-over-WDM networks taking
 * into account different restoration policies under study. We assume that IP
 * routing is governed by OSPF, with all IP links (or lightpaths) having a
 * static IGP weight, given as a parameter. Equal-Cost Multi-Path (ECMP) rule
 * is applied according to the standard. The algorithm does not take into account
 * any delay constraint either in the IP layer or in the WDM layer. In addition,
 * we assume that ROADMs do not allow wavelength conversion, and signal regeneration
 * is not required.</p>
 * <p>The algorithm is divided into two stages. First, we use a heuristic, inspired
 * in the well-known heuristic logical topology design algorithm (HLDA), to solve
 * the VTD problem allocating enough lightpaths to carry all the offered traffic
 * to the network without grooming. Then, an optional second stage is able to
 * set up new lightpaths in such a way that the traffic can still be routed
 * across the network under different possible failure states.</p>
 * <p>When the second stage is disabled, we are considering the case that the optical
 * layer is able to reroute affected lightpaths over the surviving physical topology,
 * before triggering restoration mechanisms at the IP layer. Otherwise, we are
 * over-dimensioning the IP layer so that is able to recover from different
 * pre-defined failure states.</p>
 *
 * @author Jose-Luis Izquierdo-Zaragoza, Pablo Pavon-Marino, Maria-Victoria Bueno-Delgado
 * @version 1.1, May 2015
 */
public class TCFA_IPoverWDM_mixedRestoration implements IAlgorithm {
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters) {
        /* Basic checks */
        if (netPlan.getNumberOfLayers() != 2)
            throw new Net2PlanException("Input design should have two layers: higher is IP, lower is WDM");
        final NetworkLayer wdmLayer = netPlan.getNetworkLayer(0);
        final NetworkLayer ipLayer = netPlan.getNetworkLayer(1);

        final int N = netPlan.getNumberOfNodes();
        final int E_f = netPlan.getNumberOfLinks(wdmLayer);
        final int D_ip = netPlan.getNumberOfDemands(ipLayer);
        if (N == 0 || E_f == 0 || D_ip == 0)
            throw new Net2PlanException("This algorithm requires a physical topology (nodes and links) and demands");

		/* Read input parameters */
        final double fixedOSPFWeight = Double.parseDouble(algorithmParameters.get("fixedOSPFWeight"));
        final int W = Integer.parseInt(algorithmParameters.get("numWavelengthsPerFiber"));
        final double lineRatePerLightpath_Gbps = Double.parseDouble(algorithmParameters.get("lineRatePerLightpath_Gbps"));
        final boolean setSRGsAsBidirectionalLinkFiber = Boolean.parseBoolean(algorithmParameters.get("setSRGsAsBidirectionalLinkFiber"));
        final double defaultMTTR_hours = Double.parseDouble(algorithmParameters.get("defaultMTTR_hours"));
        final boolean dimensionForSingleSRGFailure = Boolean.parseBoolean(algorithmParameters.get("dimensionForSingleSRGFailure"));
        final int K = Integer.parseInt(algorithmParameters.get("K"));

        final double PRECISION_FACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));
		
		/* Initialize multilayer design (links and demands in the input design are fibers in the lower layer and demands in the upper layer, respectively) */
        wdmLayer.setName("WDM");
        netPlan.setRoutingType(Constants.RoutingType.SOURCE_ROUTING, wdmLayer);
        ipLayer.setName("IP");
        netPlan.setRoutingType(Constants.RoutingType.HOP_BY_HOP_ROUTING, ipLayer);
        netPlan.setNetworkLayerDefault(wdmLayer);
        for (Link fiber : netPlan.getLinks(wdmLayer))
            WDMUtils.setFiberNumFrequencySlots(fiber, W); /* Set the number of wavelengths per fiber */
        netPlan.removeAllLinks(ipLayer);
        netPlan.removeAllDemands(wdmLayer);
		
		/* Generate a candidate path list for the WDM layer (using an auxiliary traffic matrix) */
        NetPlan cpl = netPlan.copy();
        cpl.removeNetworkLayer(cpl.getNetworkLayerFromId(ipLayer.getId()));
        for (Node n1 : cpl.getNodes()) for (Node n2 : cpl.getNodes()) if (n1 != n2) cpl.addDemand(n1, n2, 0.0, null);
		cpl.addRoutesFromCandidatePathList(cpl.computeUnicastCandidatePathList(null , K, -1, -1, -1, -1, -1, -1 , null));
		
		/* Configure SRGs */
        if (setSRGsAsBidirectionalLinkFiber) {
            SRGUtils.configureSRGs(netPlan, -1, defaultMTTR_hours, SRGUtils.SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE, true, wdmLayer);
            for (SharedRiskGroup srg : netPlan.getSRGs()) {
                Link link = srg.getLinks(wdmLayer).iterator().next();
                double mtbf_years = 1000.0 / (link.getLengthInKm() * 1.9);
                double mtbf_hours = 365 * 24 * mtbf_years;
                double mttf_hours = mtbf_hours - srg.getMeanTimeToRepairInHours();
                if (mttf_hours < 0) throw new RuntimeException("Bad");
                srg.setMeanTimeToFailInHours(mttf_hours);
            }
        }

        final DoubleMatrix1D w_f = WDMUtils.getVectorFiberNumFrequencySlots(netPlan, wdmLayer);
        final DoubleMatrix2D wavelengthFiberOccupancy = WDMUtils.getNetworkSlotAndRegeneratorOcupancy(netPlan, true, wdmLayer).getFirst();
				
		/* First stage: full mesh of enough lightpaths to carry all in one hop. HLDA strategy to create the lightpaths */
        DoubleMatrix1D pendingCarriedTraffic_d = netPlan.getVectorDemandOfferedTraffic(ipLayer).copy();

        while (true) {
			/* Get the demand with the highest pending traffic */
            final int ipLayerDemandIndex = (int) pendingCarriedTraffic_d.getMaxLocation()[1]; //DoubleUtils.maxIndexes(pendingCarriedTraffic_d, Constants.SearchType.FIRST).iterator().next();
            final double pendingCarriedTraffic_thisDemand = pendingCarriedTraffic_d.get(ipLayerDemandIndex);
            if (pendingCarriedTraffic_thisDemand <= PRECISION_FACTOR) break;
            final Demand ipDemand = netPlan.getDemand(ipLayerDemandIndex, ipLayer);
            final Node ingressNode = ipDemand.getIngressNode();
            final Node egressNode = ipDemand.getEgressNode();
			
			/* Try to add a lightpath for this demand */
            Pair<Route, Integer> lp = addLightpath(netPlan, ingressNode, egressNode, w_f, cpl, wavelengthFiberOccupancy);
            if (lp == null) throw new Net2PlanException("Infeasible - Unable to carry all unprotected traffic");

			/* Allocate the lightpath */
            final List<Link> seqFibers = new LinkedList<Link>();
            for (Link cplLink : lp.getFirst().getSeqLinks())
                seqFibers.add(netPlan.getLinkFromId(cplLink.getId()));
            final int wavelengthId = lp.getSecond();
            final double trafficToCarry = Math.min(pendingCarriedTraffic_thisDemand, lineRatePerLightpath_Gbps);
            pendingCarriedTraffic_d.set(ipLayerDemandIndex, pendingCarriedTraffic_thisDemand - trafficToCarry);
			
			/* Add a demand and a route in the WDM layer */
            final Demand wdmLayerDemand = netPlan.addDemand(ingressNode, egressNode, lineRatePerLightpath_Gbps, null, wdmLayer);
            WDMUtils.RSA rsa = new WDMUtils.RSA (seqFibers, wavelengthId);
            final Route wdmLayerRoute = WDMUtils.addLightpath(wdmLayerDemand, rsa , lineRatePerLightpath_Gbps);
            WDMUtils.allocateResources(rsa , wavelengthFiberOccupancy , null);
			
			/* Add a link in the IP layer */
            final Link ipLayerLightpath = netPlan.addLink(ingressNode, egressNode, lineRatePerLightpath_Gbps, wdmLayerRoute.getLengthInKm(), wdmLayerRoute.getPropagationSpeedInKmPerSecond(), null, ipLayer);
            IPUtils.setLinkWeight(ipLayerLightpath, fixedOSPFWeight);

			/* Couple IP link with WDM demand */
            wdmLayerDemand.coupleToUpperLayerLink(ipLayerLightpath);
        }
		
		/* Compute ECMP routing according to IGP weights */
        IPUtils.setECMPForwardingRulesFromLinkWeights(netPlan, null, ipLayer);
		
		/* Check: No lightpath is over-subscribed */
        if (!netPlan.getLinksOversubscribed(ipLayer).isEmpty())
            throw new RuntimeException("Bad - Some link is over-subscribed");

        System.out.println("Number of lightpaths after first stage: " + netPlan.getNumberOfLinks(ipLayer));
		
		/* Second stage: IP OSPF restoration: add lighptaths if links over-subscribed under single SRG failure */
        if (dimensionForSingleSRGFailure) {
            boolean step2SuccessfullyCompleted = false;

            boolean atLeastOneLightpathAdded = true;
            while (atLeastOneLightpathAdded) {
                atLeastOneLightpathAdded = false;

                DoubleMatrix1D lpWeights = IPUtils.getLinkWeightVector(netPlan, ipLayer);
                DoubleMatrix1D worseCarriedTraffic_lp = DoubleFactory1D.dense.make(netPlan.getNumberOfLinks(ipLayer));
				
				/* Compute the worse carried traffic per lightpath in any failure state */
                for (SharedRiskGroup srg : netPlan.getSRGs()) {
                    NetPlan netPlan_thisFailure = netPlan.copy();
                    final NetworkLayer ipLayer_thisFailure = netPlan_thisFailure.getNetworkLayer(ipLayer.getIndex());
                    final SharedRiskGroup thisNp_srg = netPlan_thisFailure.getSRG(srg.getIndex());
                    thisNp_srg.setAsDown();
					
					/* Update IGP weights according to this failure state */
                    DoubleMatrix1D lpWeights_thisSRG = lpWeights.copy();
                    for (Link e : netPlan_thisFailure.getLinksDown(ipLayer_thisFailure))
                        lpWeights_thisSRG.set(e.getIndex(), Double.MAX_VALUE);
                    for (Link e : netPlan_thisFailure.getLinksWithZeroCapacity(ipLayer_thisFailure))
                        lpWeights_thisSRG.set(e.getIndex(), Double.MAX_VALUE);
						
					/* Compute the new routing and carried traffic per lightpath */
                    final Quadruple<DoubleMatrix2D, DoubleMatrix2D, DoubleMatrix1D, DoubleMatrix1D> failInfo = IPUtils.computeCarriedTrafficFromIGPWeights(netPlan_thisFailure, lpWeights_thisSRG, ipLayer_thisFailure);
                    final DoubleMatrix1D y_e_withoutFailingLinksVector = failInfo.getFourth();

					/* Update the worse traffic per lightpath in this routing */
                    for (Link ipLink : netPlan_thisFailure.getLinks()) {
                        double y_e_thisLink = y_e_withoutFailingLinksVector.get(ipLink.getIndex());
                        if (y_e_thisLink > 0 && lpWeights_thisSRG.get(ipLink.getIndex()) == Double.MAX_VALUE)
                            throw new Net2PlanException("Bad");
                        worseCarriedTraffic_lp.set(ipLink.getIndex(), Math.max(worseCarriedTraffic_lp.get(ipLink.getIndex()), y_e_thisLink));
                    }
                }
				
				/* Sort the worse carried traffic map in descending order */
                int[] lpIndexOrdered = DoubleUtils.sortIndexes(worseCarriedTraffic_lp.toArray(), OrderingType.DESCENDING);
				
				/* If the most-subscribed lightpath is not over-subscribed, we are done */
                if (worseCarriedTraffic_lp.get(lpIndexOrdered[0]) <= lineRatePerLightpath_Gbps + PRECISION_FACTOR) {
                    step2SuccessfullyCompleted = true;
                    break;
                }
				
				/* Loop over the lightpaths to add parallel lightpaths */
                Set<Pair<Node, Node>> nodePairsAlreadyTried = new LinkedHashSet<Pair<Node, Node>>();
                for (int lpIndex : lpIndexOrdered)//Map.Entry<Long, Double> entry : worseCarriedTraffic_lp.entrySet())
                {
                    final Link lightPath = netPlan.getLink(lpIndex, ipLayer);
                    final double worseCarriedTraffic_thisLightpath = worseCarriedTraffic_lp.get(lpIndex);

					/* If we don't need it, stop */
					/* Otherwise, try to allocate the lightpath */
                    if (worseCarriedTraffic_thisLightpath <= lineRatePerLightpath_Gbps + PRECISION_FACTOR) break;

					/* If we unsuccessfully tried to add a lightpath between the same node pair, skip it */
                    final Node originNode = lightPath.getOriginNode();
                    final Node destinationNode = lightPath.getDestinationNode();
                    Pair<Node, Node> nodePair_thisLightpath = Pair.of(originNode, destinationNode);
                    if (nodePairsAlreadyTried.contains(nodePair_thisLightpath)) continue;
					
					/* Try to add a parallel lightpath */
                    nodePairsAlreadyTried.add(nodePair_thisLightpath);
                    Pair<Route, Integer> lp = addLightpath(netPlan, originNode, destinationNode, w_f, cpl, wavelengthFiberOccupancy);
                    if (lp == null) continue;

                    final List<Link> seqFibers = new LinkedList<Link>();
                    for (Link cplLink : lp.getFirst().getSeqLinks())
                        seqFibers.add(netPlan.getLinkFromId(cplLink.getId()));
                    final int wavelengthId = lp.getSecond();
					
					/* Add a demand and a route in the WDM layer */
                    final Demand wdmLayerDemand = netPlan.addDemand(originNode, destinationNode, lineRatePerLightpath_Gbps, null, wdmLayer);
                    final WDMUtils.RSA rsa = new WDMUtils.RSA(seqFibers, wavelengthId);
                    final Route wdmLayerRoute = WDMUtils.addLightpath(wdmLayerDemand, rsa , lineRatePerLightpath_Gbps);
                    WDMUtils.allocateResources(rsa , wavelengthFiberOccupancy , null);
					
					/* Add a link in the IP layer */
                    final Link ipLayerLightpath = netPlan.addLink(originNode, destinationNode, lineRatePerLightpath_Gbps, wdmLayerRoute.getLengthInKm(), wdmLayerRoute.getPropagationSpeedInKmPerSecond(), null, ipLayer);
                    IPUtils.setLinkWeight(ipLayerLightpath, fixedOSPFWeight);

					/* Couple IP link with WDM demand */
                    wdmLayerDemand.coupleToUpperLayerLink(ipLayerLightpath);

                    atLeastOneLightpathAdded = true;
                    break;
                }
            }

            netPlan.setAttribute("step2SuccessfullyCompleted", Boolean.toString(step2SuccessfullyCompleted));
            System.out.println("Phase 2 successfully completed " + step2SuccessfullyCompleted);
        }

        System.out.println("Number of lightpaths after second stage: " + netPlan.getNumberOfLinks(ipLayer));
		
		/* Compute final ECMP routing according to IGP weights */
        IPUtils.setECMPForwardingRulesFromLinkWeights(netPlan, null, ipLayer);
		
		/* Check consistency of WDM layer */
        WDMUtils.checkResourceAllocationClashing(netPlan, true, false , wdmLayer);

        return "Ok";
    }

    @Override
    public String getDescription() {
        String NEW_LINE = StringUtils.getLineSeparator();
        StringBuilder aux = new StringBuilder();

        aux.append("This algorithm is used for dimensioning multilayer IP-over-WDM "
                + "networks taking into account different restoration policies under "
                + "study. We assume that IP routing is governed by OSPF, with all IP "
                + "links (or lightpaths) having a static IGP weight, given as a "
                + "parameter. Equal-Cost Multi-Path (ECMP) rule is applied according "
                + "to the standard. The algorithm does not take into account any "
                + "delay constraint either in the IP layer or in the WDM layer. In "
                + "addition, we assume that ROADMs do not allow wavelength conversion, "
                + "and signal regeneration is not required.");

        aux.append(NEW_LINE);

        aux.append("The algorithm is divided into two stages. First, we use a heuristic, "
                + "inspired in the well-known heuristic logical topology design algorithm "
                + "(HLDA), to solve the VTD problem allocating enough lightpaths to carry "
                + "all the offered traffic to the network without grooming. Then, an "
                + "optional second stage is able to set up new lightpaths in such a "
                + "way that the traffic can still be routed across the network under "
                + "different possible failure states.");

        aux.append(NEW_LINE);

        aux.append("When the second stage is disabled, we are considering the case that "
                + "the optical layer is able to reroute affected lightpaths over the "
                + "surviving physical topology, before triggering restoration mechanisms "
                + "at the IP layer. Otherwise, we are over-dimensioning the IP layer so "
                + "that is able to recover from different pre-defined failure states.");

        return aux.toString();
    }

    @Override
    public List<Triple<String, String, String>> getParameters() {
        List<Triple<String, String, String>> algorithmParameters = new LinkedList<Triple<String, String, String>>();
        algorithmParameters.add(Triple.of("lineRatePerLightpath_Gbps", "10", "All the IP links have this capacity"));
        algorithmParameters.add(Triple.of("fixedOSPFWeight", "1", "Fixed OSPF weight to assign to all the IP links"));
        algorithmParameters.add(Triple.of("numWavelengthsPerFiber", "40", "Number of wavelengths per link"));
        algorithmParameters.add(Triple.of("setSRGsAsBidirectionalLinkFiber", "#boolean# true", "Indicates whether the design configures bidirectional fiber SRGs"));
        algorithmParameters.add(Triple.of("defaultMTTR_hours", "12", "Default mean time to repair (in hours)"));
        algorithmParameters.add(Triple.of("dimensionForSingleSRGFailure", "#boolean# true", "Indicates whether IP layer is overdimensioned to consider single SRG failures"));
        algorithmParameters.add(Triple.of("K", "5", "Maximum number of admissible paths per demand"));
        return algorithmParameters;
    }

    private static Pair<Route, Integer> addLightpath(NetPlan netPlan, Node ingressNode, Node egressNode, DoubleMatrix1D w_f, NetPlan cpl, DoubleMatrix2D wavelengthFiberOccupancy) {
		/* Try to find a feasible physical path, with a free wavelength along it, among the set of candidate paths */
        final Demand cplDemand = cpl.getNodePairDemands(cpl.getNodeFromId(ingressNode.getId()), cpl.getNodeFromId(egressNode.getId()), false).iterator().next();
        for (Route cplRoute : cplDemand.getRoutes()) {
            List<Link> seqLinks = new LinkedList<Link>();
            for (Link cplLink : cplRoute.getSeqLinks()) seqLinks.add(netPlan.getLinkFromId(cplLink.getId()));
            final int wavelength = WDMUtils.spectrumAssignment_firstFit(seqLinks, wavelengthFiberOccupancy,1);
            if (wavelength != -1)
                return Pair.of(cplRoute, wavelength);
        }
		
		/* Otherwise, return null*/
        return null;
    }

}