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
package com.net2plan.gui.plugins.networkDesign.visualizationControl;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUILink;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jorge San Emeterio
 * @date 25/04/17
 */
class PickManager
{
    private final VisualizationState vs;

    private final PickTimeLineManager pickTimeLineManager;

    private List<? extends NetworkElement> pickedElement;
    private List<Pair<Demand, Link>> pickedForwardingRule;

    PickManager(VisualizationState vs)
    {
        this.vs = vs;

        this.pickTimeLineManager = new PickTimeLineManager();
    }

    void reset()
    {
        this.pickedElement = null;
        this.pickedForwardingRule = null;
    }

    List<NetworkElement> getPickedNetworkElements()
    {
        return pickedElement == null ? new ArrayList<>() : Collections.unmodifiableList(pickedElement);
    }


    List<Pair<Demand, Link>> getPickedForwardingRules()
    {
        return pickedForwardingRule == null ? new ArrayList<>() : Collections.unmodifiableList(pickedForwardingRule);
    }

    void pickLayer(NetworkLayer pickedLayer)
    {
        cleanPick();
        this.pickedForwardingRule = null;
        this.pickedElement = Arrays.asList(pickedLayer);
        this.pickTimeLineManager.addElement(vs.getNetPlan(), pickedLayer);
    }

    void pickDemand(List<Demand> pickedDemands)
    {
        cleanPick();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedDemands;
        if (pickedDemands.size() == 1) this.pickTimeLineManager.addElement(vs.getNetPlan(), pickedDemands.get(0));

        Pair<Set<Link>, Set<Link>> thisLayerPropagation = null;
        for (Demand pickedDemand : pickedDemands)
        {
            final boolean isDemandLayerVisibleInTheCanvas = vs.isLayerVisibleInCanvas(pickedDemand.getLayer());
            final GUINode gnOrigin = vs.getCanvasAssociatedGUINode(pickedDemand.getIngressNode(), pickedDemand.getLayer());
            final GUINode gnDestination = vs.getCanvasAssociatedGUINode(pickedDemand.getEgressNode(), pickedDemand.getLayer());

            if (vs.isShowInCanvasThisLayerPropagation() && isDemandLayerVisibleInTheCanvas)
            {
                thisLayerPropagation = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic();
                final Set<Link> linksPrimary = thisLayerPropagation.getFirst();
                final Set<Link> linksBackup = thisLayerPropagation.getSecond();
                final Set<Link> linksPrimaryAndBackup = Sets.intersection(linksPrimary, linksBackup);
                DrawUtils.drawCollateralLinks(vs, Sets.difference(linksPrimary, linksPrimaryAndBackup), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawCollateralLinks(vs, Sets.difference(linksBackup, linksPrimaryAndBackup), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
                DrawUtils.drawCollateralLinks(vs, linksPrimaryAndBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
            }

            if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
            {
                if (thisLayerPropagation == null)
                    thisLayerPropagation = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic();
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfoPrimary = DrawUtils.getDownCoupling(thisLayerPropagation.getFirst());
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfoBackup = DrawUtils.getDownCoupling(thisLayerPropagation.getSecond());
                final InterLayerPropagationGraph ipgPrimary = new InterLayerPropagationGraph(downLayerInfoPrimary.getFirst(), null, downLayerInfoPrimary.getSecond(), false);
                final InterLayerPropagationGraph ipgBackup = new InterLayerPropagationGraph(downLayerInfoBackup.getFirst(), null, downLayerInfoBackup.getSecond(), false);
                final Set<Link> linksPrimary = ipgPrimary.getLinksInGraph();
                final Set<Link> linksBackup = ipgBackup.getLinksInGraph();
                final Set<Link> linksPrimaryAndBackup = Sets.intersection(linksPrimary, linksBackup);
                final Set<Link> linksOnlyPrimary = Sets.difference(linksPrimary, linksPrimaryAndBackup);
                final Set<Link> linksOnlyBackup = Sets.difference(linksBackup, linksPrimaryAndBackup);
                DrawUtils.drawCollateralLinks(vs, linksOnlyPrimary, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, linksOnlyPrimary, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawCollateralLinks(vs, linksOnlyBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, linksOnlyBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
                DrawUtils.drawCollateralLinks(vs, linksPrimaryAndBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, linksPrimaryAndBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
            }
            if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedDemand.isCoupled())
            {
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, Sets.newHashSet(pickedDemand.getCoupledLink()), null, true);
                DrawUtils.drawCollateralLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }

        /* Picked link the last, so overrides the rest */
            if (isDemandLayerVisibleInTheCanvas)
            {
                gnOrigin.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gnDestination.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
            }
        }
    }

    void pickSRG(List<SharedRiskGroup> pickedSRGs)
    {
        cleanPick();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedSRGs;
        if (pickedSRGs.size() == 1) this.pickTimeLineManager.addElement(vs.getNetPlan(), pickedSRGs.get(0));

        for (SharedRiskGroup pickedSRG : pickedSRGs)
        {
            final Set<Link> allAffectedLinks = pickedSRG.getAffectedLinksAllLayers();
            Map<Link, Triple<Map<Demand, Set<Link>>, Map<Demand, Set<Link>>, Map<Pair<MulticastDemand, Node>, Set<Link>>>> thisLayerPropInfo = new HashMap<>();
            if (vs.isShowInCanvasThisLayerPropagation())
            {
                for (Link link : allAffectedLinks)
                {
                    thisLayerPropInfo.put(link, link.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink());
                    final Set<Link> linksPrimary = thisLayerPropInfo.get(link).getFirst().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
                    final Set<Link> linksBackup = thisLayerPropInfo.get(link).getSecond().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
                    final Set<Link> linksMulticast = thisLayerPropInfo.get(link).getThird().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
                    final Set<Link> links = new HashSet<>();
                    if (linksPrimary != null) links.addAll(linksPrimary);
                    if (linksBackup != null) links.addAll(linksBackup);
                    if (linksMulticast != null) links.addAll(linksMulticast);
                    DrawUtils.drawCollateralLinks(vs, links, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
                }
            }
            if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
            {
                final Set<Link> affectedCoupledLinks = allAffectedLinks.stream().filter(e -> e.isCoupled()).collect(Collectors.toSet());
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> couplingInfo = DrawUtils.getDownCoupling(affectedCoupledLinks);
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(couplingInfo.getFirst(), null, couplingInfo.getSecond(), false);
                final Set<Link> lowerLayerLinks = ipg.getLinksInGraph();
                DrawUtils.drawCollateralLinks(vs, lowerLayerLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, lowerLayerLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
            }
            if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
            {
                final Set<Demand> demandsPrimaryAndBackup = new HashSet<>();
                final Set<Pair<MulticastDemand, Node>> demandsMulticast = new HashSet<>();
                for (Link link : allAffectedLinks)
                {
                    final Triple<Map<Demand, Set<Link>>, Map<Demand, Set<Link>>, Map<Pair<MulticastDemand, Node>, Set<Link>>> thisLinkInfo =
                            vs.isShowInCanvasThisLayerPropagation() ? thisLayerPropInfo.get(link) : link.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink();
                    demandsPrimaryAndBackup.addAll(Sets.union(thisLinkInfo.getFirst().keySet(), thisLinkInfo.getSecond().keySet()));
                    demandsMulticast.addAll(thisLinkInfo.getThird().keySet());
                }
                final Set<Link> coupledUpperLinks = DrawUtils.getUpCoupling(demandsPrimaryAndBackup, demandsMulticast);
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, coupledUpperLinks, null, true);
                DrawUtils.drawCollateralLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
            }

            /* Picked link the last, so overrides the rest */
            for (Link link : allAffectedLinks)
            {
                final GUILink gl = vs.getCanvasAssociatedGUILink(link);
                if (gl == null) continue;
                gl.setHasArrow(true);
                DrawUtils.setCurrentDefaultEdgeStroke(vs, gl, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
                final Paint color = link.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED;
                gl.setEdgeDrawPaint(color);
                gl.setShownSeparated(true);
            }

            for (Node node : pickedSRG.getNodes())
            {
                for (GUINode gn : vs.getCanvasVerticallyStackedGUINodes(node))
                {
                    gn.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_FAILED);
                    gn.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_FAILED);
                }
            }
        }
    }

    void pickMulticastDemand(List<MulticastDemand> pickedDemands)
    {
        cleanPick();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedDemands;
        if (pickedDemands.size() == 1) this.pickTimeLineManager.addElement(vs.getNetPlan(), pickedDemands.get(0));

        for (MulticastDemand pickedDemand : pickedDemands)
        {
            final boolean isDemandLayerVisibleInTheCanvas = vs.isLayerVisibleInCanvas(pickedDemand.getLayer());
            final GUINode gnOrigin = vs.getCanvasAssociatedGUINode(pickedDemand.getIngressNode(), pickedDemand.getLayer());
            Set<Link> linksThisLayer = null;
            for (Node egressNode : pickedDemand.getEgressNodes())
            {
                final GUINode gnDestination = vs.getCanvasAssociatedGUINode(egressNode, pickedDemand.getLayer());
                if (vs.isShowInCanvasThisLayerPropagation() && isDemandLayerVisibleInTheCanvas)
                {
                    linksThisLayer = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic(egressNode);
                    DrawUtils.drawCollateralLinks(vs, linksThisLayer, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
                {
                    if (linksThisLayer == null)
                        linksThisLayer = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic(egressNode);
                    final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfo = DrawUtils.getDownCoupling(linksThisLayer);
                    final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downLayerInfo.getFirst(), null, downLayerInfo.getSecond(), false);
                    final Set<Link> linksLowerLayers = ipg.getLinksInGraph();
                    DrawUtils.drawCollateralLinks(vs, linksLowerLayers, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                    DrawUtils.drawDownPropagationInterLayerLinks(vs, linksLowerLayers, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedDemand.isCoupled())
                {
                    final Set<Link> upCoupledLink = DrawUtils.getUpCoupling(null, Collections.singleton(Pair.of(pickedDemand, egressNode)));
                    final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, upCoupledLink, null, true);
                    DrawUtils.drawCollateralLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                    DrawUtils.drawDownPropagationInterLayerLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                /* Picked link the last, so overrides the rest */
                if (isDemandLayerVisibleInTheCanvas)
                {
                    gnDestination.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                    gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                }
            }
            /* Picked link the last, so overrides the rest */
            if (isDemandLayerVisibleInTheCanvas)
            {
                gnOrigin.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
            }
        }
    }

    void pickRoute(List<Route> pickedRoutes)
    {
        cleanPick();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedRoutes;
        if (pickedRoutes.size() == 1) this.pickTimeLineManager.addElement(vs.getNetPlan(), pickedRoutes.get(0));

        for (Route pickedRoute : pickedRoutes)
        {
            final boolean isRouteLayerVisibleInTheCanvas = vs.isLayerVisibleInCanvas(pickedRoute.getLayer());
            if (vs.isShowInCanvasThisLayerPropagation() && isRouteLayerVisibleInTheCanvas)
            {
                final List<Link> linksPrimary = pickedRoute.getSeqLinks();
                DrawUtils.drawCollateralLinks(vs, linksPrimary, pickedRoute.isBackupRoute() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
            {
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downInfo = DrawUtils.getDownCoupling(pickedRoute.getSeqLinks());
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downInfo.getFirst(), null, downInfo.getSecond(), false);
                DrawUtils.drawCollateralLinks(vs, ipg.getLinksInGraph(), pickedRoute.isBackupRoute() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, ipg.getLinksInGraph(), pickedRoute.isBackupRoute() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedRoute.getDemand().isCoupled())
            {
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, Sets.newHashSet(pickedRoute.getDemand().getCoupledLink()), null, true);
                DrawUtils.drawCollateralLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            /* Picked link the last, so overrides the rest */
            if (isRouteLayerVisibleInTheCanvas)
            {
                final GUINode gnOrigin = vs.getCanvasAssociatedGUINode(pickedRoute.getIngressNode(), pickedRoute.getLayer());
                final GUINode gnDestination = vs.getCanvasAssociatedGUINode(pickedRoute.getEgressNode(), pickedRoute.getLayer());
                gnOrigin.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gnDestination.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
            }
        }
    }

    void pickMulticastTree(List<MulticastTree> pickedTrees)
    {
        cleanPick();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedTrees;
        if (pickedTrees.size() == 1) this.pickTimeLineManager.addElement(vs.getNetPlan(), pickedTrees.get(0));

        for (MulticastTree pickedTree : pickedTrees)
        {
            final boolean isTreeLayerVisibleInTheCanvas = vs.isLayerVisibleInCanvas(pickedTree.getLayer());
            final GUINode gnOrigin = vs.getCanvasAssociatedGUINode(pickedTree.getIngressNode(), pickedTree.getLayer());
            for (Node egressNode : pickedTree.getEgressNodes())
            {
                final GUINode gnDestination = vs.getCanvasAssociatedGUINode(egressNode, pickedTree.getLayer());
                if (vs.isShowInCanvasThisLayerPropagation() && isTreeLayerVisibleInTheCanvas)
                {
                    final List<Link> linksPrimary = pickedTree.getSeqLinksToEgressNode(egressNode);
                    DrawUtils.drawCollateralLinks(vs, linksPrimary, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
                {
                    final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downInfo = DrawUtils.getDownCoupling(pickedTree.getSeqLinksToEgressNode(egressNode));
                    final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downInfo.getFirst(), null, downInfo.getSecond(), false);
                    DrawUtils.drawCollateralLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                    DrawUtils.drawDownPropagationInterLayerLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedTree.getMulticastDemand().isCoupled())
                {
                    final Set<Link> upperCoupledLink = DrawUtils.getUpCoupling(null, Arrays.asList(Pair.of(pickedTree.getMulticastDemand(), egressNode)));
                    final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, upperCoupledLink, null, true);
                    DrawUtils.drawCollateralLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                    DrawUtils.drawDownPropagationInterLayerLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (isTreeLayerVisibleInTheCanvas)
                {
                    gnDestination.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                    gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                }
            }
            /* Picked link the last, so overrides the rest */
            if (isTreeLayerVisibleInTheCanvas)
            {
                gnOrigin.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
            }
        }
    }

    void pickLink(List<Link> pickedLinks)
    {
        cleanPick();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedLinks;
        if (pickedLinks.size() == 1) this.pickTimeLineManager.addElement(vs.getNetPlan(), pickedLinks.get(0));

        for (Link pickedLink : pickedLinks)
        {
            final boolean isLinkLayerVisibleInTheCanvas = vs.isLayerVisibleInCanvas(pickedLink.getLayer());
            Triple<Map<Demand, Set<Link>>, Map<Demand, Set<Link>>, Map<Pair<MulticastDemand, Node>, Set<Link>>> thisLayerTraversalInfo = null;
            if (vs.isShowInCanvasThisLayerPropagation() && isLinkLayerVisibleInTheCanvas)
            {
                thisLayerTraversalInfo = pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink();
                final Set<Link> linksPrimary = thisLayerTraversalInfo.getFirst().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
                final Set<Link> linksBackup = thisLayerTraversalInfo.getSecond().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
                final Set<Link> linksMulticast = thisLayerTraversalInfo.getThird().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
                DrawUtils.drawCollateralLinks(vs, Sets.union(Sets.union(linksPrimary, linksBackup), linksMulticast), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedLink.isCoupled())
            {
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfo = DrawUtils.getDownCoupling(Arrays.asList(pickedLink));
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downLayerInfo.getFirst(), null, downLayerInfo.getSecond(), false);
                DrawUtils.drawCollateralLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
            {
                if (thisLayerTraversalInfo == null)
                    thisLayerTraversalInfo = pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink();
                final Set<Demand> demandsPrimaryAndBackup = Sets.union(thisLayerTraversalInfo.getFirst().keySet(), thisLayerTraversalInfo.getSecond().keySet());
                final Set<Pair<MulticastDemand, Node>> mDemands = thisLayerTraversalInfo.getThird().keySet();
                final Set<Link> initialUpperLinks = DrawUtils.getUpCoupling(demandsPrimaryAndBackup, mDemands);
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, Sets.newHashSet(initialUpperLinks), null, true);
                DrawUtils.drawCollateralLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            /* Picked link the last, so overrides the rest */
            if (isLinkLayerVisibleInTheCanvas)
            {
                final GUILink gl = vs.getCanvasAssociatedGUILink(pickedLink);
                gl.setHasArrow(true);
                DrawUtils.setCurrentDefaultEdgeStroke(vs, gl, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
                final Paint color = pickedLink.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED;
                gl.setEdgeDrawPaint(color);
                gl.setShownSeparated(true);
            }
        }
    }

    void pickNode(List<Node> pickedNodes)
    {
        cleanPick();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedNodes;
        if (pickedNodes.size() == 1) this.pickTimeLineManager.addElement(vs.getNetPlan(), pickedNodes.get(0));

        for (Node pickedNode : pickedNodes)
        {
            for (GUINode gn : vs.getCanvasVerticallyStackedGUINodes(pickedNode))
            {
                gn.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_PICK);
                gn.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_PICK);
            }
            for (Link e : Sets.union(pickedNode.getOutgoingLinks(vs.getNetPlan().getNetworkLayerDefault()), pickedNode.getIncomingLinks(vs.getNetPlan().getNetworkLayerDefault())))
            {
                final GUILink gl = vs.getCanvasAssociatedGUILink(e);
                gl.setShownSeparated(true);
                gl.setHasArrow(true);
            }
        }
    }

    void pickResource(List<Resource> pickedResources)
    {
        cleanPick();
        this.pickedForwardingRule = null;
        this.pickedElement = pickedResources;
        if (pickedResources.size() == 1) this.pickTimeLineManager.addElement(vs.getNetPlan(), pickedResources.get(0));

        for (Resource pickedResource : pickedResources)
        {
            for (GUINode gn : vs.getCanvasVerticallyStackedGUINodes(pickedResource.getHostNode()))
            {
                gn.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_RESOURCE);
                gn.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_RESOURCE);
            }
        }
    }

    void pickForwardingRule(List<Pair<Demand, Link>> pickedFRs)
    {
        cleanPick();
        this.pickedForwardingRule = pickedFRs;
        this.pickedElement = null;
        if (pickedFRs.size() == 1) this.pickTimeLineManager.addElement(vs.getNetPlan(), pickedFRs.get(0));

        for (Pair<Demand, Link> pickedFR : pickedFRs)
        {
            final boolean isFRLayerVisibleInTheCanvas = vs.isLayerVisibleInCanvas(pickedFR.getFirst().getLayer());
            final Demand pickedDemand = pickedFR.getFirst();
            final Link pickedLink = pickedFR.getSecond();
            if (vs.isShowInCanvasThisLayerPropagation() && isFRLayerVisibleInTheCanvas)
            {
                final Triple<Map<Demand, Set<Link>>, Map<Demand, Set<Link>>, Map<Pair<MulticastDemand, Node>, Set<Link>>> triple =
                        pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink();
                final Set<Link> linksPrimary = triple.getFirst().get(pickedDemand);
                final Set<Link> linksBackup = triple.getSecond().get(pickedDemand);
                DrawUtils.drawCollateralLinks(vs, Sets.union(linksPrimary == null ? new HashSet<>() : linksPrimary, linksBackup == null ? new HashSet<>() : linksBackup
                ), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedLink.isCoupled())
            {
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfo = DrawUtils.getDownCoupling(Arrays.asList(pickedLink));
                final InterLayerPropagationGraph ipgCausedByLink = new InterLayerPropagationGraph(downLayerInfo.getFirst(), null, downLayerInfo.getSecond(), false);
                final Set<Link> frPropagationLinks = ipgCausedByLink.getLinksInGraph();
                DrawUtils.drawCollateralLinks(vs, frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedDemand.isCoupled())
            {
                final InterLayerPropagationGraph ipgCausedByDemand = new InterLayerPropagationGraph(null, Sets.newHashSet(pickedDemand.getCoupledLink()), null, true);
                final Set<Link> frPropagationLinks = ipgCausedByDemand.getLinksInGraph();
                DrawUtils.drawCollateralLinks(vs, frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            /* Picked link the last, so overrides the rest */
            if (isFRLayerVisibleInTheCanvas)
            {
                final GUILink gl = vs.getCanvasAssociatedGUILink(pickedLink);
                gl.setHasArrow(true);
                DrawUtils.setCurrentDefaultEdgeStroke(vs, gl, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
                final Paint color = pickedLink.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED;
                gl.setEdgeDrawPaint(color);
                gl.setShownSeparated(true);
                gl.getOriginNode().setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gl.getOriginNode().setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gl.getDestinationNode().setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                gl.getDestinationNode().setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
            }
        }


    }

    Object getPickNavigationBackElement()
    {
        return pickTimeLineManager.getPickNavigationBackElement();
    }

    Object getPickNavigationForwardElement()
    {
        return pickTimeLineManager.getPickNavigationForwardElement();
    }

    void cleanPick()
    {
        this.pickedElement = null;
        this.pickedForwardingRule = null;

        for (GUINode n : vs.getCanvasAllGUINodes())
        {
            n.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR);
            n.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR);
        }
        for (GUILink e : vs.getCanvasAllGUILinks(true, false))
        {
            e.setHasArrow(VisualizationConstants.DEFAULT_REGGUILINK_HASARROW);
            DrawUtils.setCurrentDefaultEdgeStroke(vs, e, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_ACTIVELAYER, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE);
            final boolean isDown = e.getAssociatedNetPlanLink().isDown();
            final Paint color = isDown ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR;
            e.setEdgeDrawPaint(color);
            e.setShownSeparated(isDown);
        }
        for (GUILink e : vs.getCanvasAllGUILinks(false, true))
        {
            e.setHasArrow(VisualizationConstants.DEFAULT_INTRANODEGUILINK_HASARROW);
            DrawUtils.setCurrentDefaultEdgeStroke(vs, e, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE);
            e.setEdgeDrawPaint(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR);
            e.setShownSeparated(false);
        }
    }

    private static class DrawUtils
    {
        static void drawCollateralLinks(VisualizationState vs, Collection<Link> links, Paint colorIfNotFailedLink)
        {
            for (Link link : links)
            {
                final GUILink glCollateral = vs.getCanvasAssociatedGUILink(link);
                if (glCollateral == null) continue;
                setCurrentDefaultEdgeStroke(vs, glCollateral, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALACTVELAYER, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALNONACTIVELAYER);
                final Paint color = link.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : colorIfNotFailedLink;
                glCollateral.setEdgeDrawPaint(color);
                glCollateral.setShownSeparated(true);
                glCollateral.setHasArrow(true);
            }
        }

        static Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> getDownCoupling(Collection<Link> links)
        {
            final Set<Demand> res_1 = new HashSet<>();
            final Set<Pair<MulticastDemand, Node>> res_2 = new HashSet<>();
            for (Link link : links)
            {
                if (link.getCoupledDemand() != null) res_1.add(link.getCoupledDemand());
                else if (link.getCoupledMulticastDemand() != null)
                    res_2.add(Pair.of(link.getCoupledMulticastDemand(), link.getDestinationNode()));
            }
            return Pair.of(res_1, res_2);

        }

        static Set<Link> getUpCoupling(Collection<Demand> demands, Collection<Pair<MulticastDemand, Node>> mDemands)
        {
            final Set<Link> res = new HashSet<>();
            if (demands != null)
                for (Demand d : demands)
                    if (d.isCoupled()) res.add(d.getCoupledLink());

            if (mDemands != null)
            {
                for (Pair<MulticastDemand, Node> md : mDemands)
                {
                    if (md.getFirst().isCoupled())
                    {
                        for (Link link : md.getFirst().getCoupledLinks())
                        {
                            if (link.getDestinationNode() == md.getSecond())
                            {
                                res.add(link);
                                break;
                            }
                        }
                    }
                }
            }
            return res;
        }

        static void drawDownPropagationInterLayerLinks(VisualizationState vs, Set<Link> links, Paint color)
        {
            for (Link link : links)
            {
                final GUILink gl = vs.getCanvasAssociatedGUILink(link);
                if (gl == null) continue;
                if (!link.isCoupled()) continue;
                final boolean isCoupledToDemand = link.getCoupledDemand() != null;
                final NetworkLayer upperLayer = link.getLayer();
                final NetworkLayer lowerLayer = isCoupledToDemand ? link.getCoupledDemand().getLayer() : link.getCoupledMulticastDemand().getLayer();
                if (!vs.isLayerVisibleInCanvas(lowerLayer)) continue;
                for (GUILink interLayerLink : vs.getCanvasIntraNodeGUILinkSequence(link.getOriginNode(), upperLayer, lowerLayer))
                {
                    setCurrentDefaultEdgeStroke(vs, interLayerLink, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED);
                    interLayerLink.setEdgeDrawPaint(color);
                    interLayerLink.setShownSeparated(false);
                    interLayerLink.setHasArrow(true);
                }
                for (GUILink interLayerLink : vs.getCanvasIntraNodeGUILinkSequence(link.getDestinationNode(), lowerLayer, upperLayer))
                {
                    setCurrentDefaultEdgeStroke(vs, interLayerLink, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED);
                    interLayerLink.setEdgeDrawPaint(color);
                    interLayerLink.setShownSeparated(false);
                    interLayerLink.setHasArrow(true);
                }
            }
        }

        static void setCurrentDefaultEdgeStroke(VisualizationState vs, GUILink e, BasicStroke a, BasicStroke na)
        {
            e.setEdgeStroke(VisualizationUtils.resizedBasicStroke(a, vs.getLinkWidthFactor()), VisualizationUtils.resizedBasicStroke(na, vs.getLinkWidthFactor()));
        }
    }
}
