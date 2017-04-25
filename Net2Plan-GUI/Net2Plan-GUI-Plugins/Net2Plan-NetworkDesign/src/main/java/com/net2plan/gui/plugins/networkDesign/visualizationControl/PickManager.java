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

import static com.net2plan.internal.Constants.*;

/**
 * @author Jorge San Emeterio
 * @date 25/04/17
 */
class PickManager
{
    private final VisualizationState vs;

    private PickTimeLineManager pickTimeLineManager;

    private NetworkElementType pickedElementType;
    private List<? extends NetworkElement> pickedElement;
    private List<Pair<Demand, Link>> pickedForwardingRule;

    PickManager(VisualizationState vs)
    {
        this.vs = vs;

        this.pickTimeLineManager = new PickTimeLineManager();
        
        this.pickedElementType = null;
        this.pickedElement = null;
        this.pickedForwardingRule = null;
    }

    NetworkElementType getPickedElementType()
    {
        return pickedElementType;
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
        resetPickedState();
        this.pickedElementType = NetworkElementType.LAYER;
        this.pickedForwardingRule = null;
        this.pickedElement = Arrays.asList(pickedLayer);
        this.addElementToPickTimeline(pickedLayer);
    }

    void pickDemand(List<Demand> pickedDemands)
    {
        resetPickedState();
        this.pickedElementType = NetworkElementType.DEMAND;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedDemands;
        if (pickedDemands.size() == 1) this.addElementToPickTimeline(pickedDemands.get(0));

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
                drawColateralLinks(Sets.difference(linksPrimary, linksPrimaryAndBackup), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawColateralLinks(Sets.difference(linksBackup, linksPrimaryAndBackup), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
                drawColateralLinks(linksPrimaryAndBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
            }

            if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
            {
                if (thisLayerPropagation == null)
                    thisLayerPropagation = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic();
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfoPrimary = getDownCoupling(thisLayerPropagation.getFirst());
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfoBackup = getDownCoupling(thisLayerPropagation.getSecond());
                final InterLayerPropagationGraph ipgPrimary = new InterLayerPropagationGraph(downLayerInfoPrimary.getFirst(), null, downLayerInfoPrimary.getSecond(), false);
                final InterLayerPropagationGraph ipgBackup = new InterLayerPropagationGraph(downLayerInfoBackup.getFirst(), null, downLayerInfoBackup.getSecond(), false);
                final Set<Link> linksPrimary = ipgPrimary.getLinksInGraph();
                final Set<Link> linksBackup = ipgBackup.getLinksInGraph();
                final Set<Link> linksPrimaryAndBackup = Sets.intersection(linksPrimary, linksBackup);
                final Set<Link> linksOnlyPrimary = Sets.difference(linksPrimary, linksPrimaryAndBackup);
                final Set<Link> linksOnlyBackup = Sets.difference(linksBackup, linksPrimaryAndBackup);
                drawColateralLinks(linksOnlyPrimary, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(linksOnlyPrimary, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawColateralLinks(linksOnlyBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
                drawDownPropagationInterLayerLinks(linksOnlyBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
                drawColateralLinks(linksPrimaryAndBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
                drawDownPropagationInterLayerLinks(linksPrimaryAndBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
            }
            if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedDemand.isCoupled())
            {
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, Sets.newHashSet(pickedDemand.getCoupledLink()), null, true);
                drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
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
        resetPickedState();
        this.pickedElementType = NetworkElementType.SRG;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedSRGs;
        if (pickedSRGs.size() == 1) this.addElementToPickTimeline(pickedSRGs.get(0));

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
                    drawColateralLinks(links, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
                }
            }
            if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
            {
                final Set<Link> affectedCoupledLinks = allAffectedLinks.stream().filter(e -> e.isCoupled()).collect(Collectors.toSet());
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> couplingInfo = getDownCoupling(affectedCoupledLinks);
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(couplingInfo.getFirst(), null, couplingInfo.getSecond(), false);
                final Set<Link> lowerLayerLinks = ipg.getLinksInGraph();
                drawColateralLinks(lowerLayerLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
                drawDownPropagationInterLayerLinks(lowerLayerLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
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
                final Set<Link> coupledUpperLinks = getUpCoupling(demandsPrimaryAndBackup, demandsMulticast);
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, coupledUpperLinks, null, true);
                drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
                drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
            }
        /* Picked link the last, so overrides the rest */
            for (Link link : allAffectedLinks)
            {
                final GUILink gl = vs.getCanvasAssociatedGUILink(link);
                if (gl == null) continue;
                gl.setHasArrow(true);
                setCurrentDefaultEdgeStroke(gl, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
                final Paint color = link.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED;
                gl.setEdgeDrawPaint(color);
                gl.setShownSeparated(true);
            }
        }
    }

    void pickMulticastDemand(List<MulticastDemand> pickedDemands)
    {
        resetPickedState();
        this.pickedElementType = NetworkElementType.MULTICAST_DEMAND;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedDemands;
        if (pickedDemands.size() == 1) this.addElementToPickTimeline(pickedDemands.get(0));

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
                    drawColateralLinks(linksThisLayer, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
                {
                    if (linksThisLayer == null)
                        linksThisLayer = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic(egressNode);
                    final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfo = getDownCoupling(linksThisLayer);
                    final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downLayerInfo.getFirst(), null, downLayerInfo.getSecond(), false);
                    final Set<Link> linksLowerLayers = ipg.getLinksInGraph();
                    drawColateralLinks(linksLowerLayers, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                    drawDownPropagationInterLayerLinks(linksLowerLayers, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedDemand.isCoupled())
                {
                    final Set<Link> upCoupledLink = getUpCoupling(null, Collections.singleton(Pair.of(pickedDemand, egressNode)));
                    final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, upCoupledLink, null, true);
                    drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                    drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
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
        resetPickedState();
        this.pickedElementType = NetworkElementType.ROUTE;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedRoutes;
        if (pickedRoutes.size() == 1) this.addElementToPickTimeline(pickedRoutes.get(0));

        for (Route pickedRoute : pickedRoutes)
        {
            final boolean isRouteLayerVisibleInTheCanvas = vs.isLayerVisibleInCanvas(pickedRoute.getLayer());
            if (vs.isShowInCanvasThisLayerPropagation() && isRouteLayerVisibleInTheCanvas)
            {
                final List<Link> linksPrimary = pickedRoute.getSeqLinks();
                drawColateralLinks(linksPrimary, pickedRoute.isBackupRoute() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
            {
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downInfo = getDownCoupling(pickedRoute.getSeqLinks());
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downInfo.getFirst(), null, downInfo.getSecond(), false);
                drawColateralLinks(ipg.getLinksInGraph(), pickedRoute.isBackupRoute() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), pickedRoute.isBackupRoute() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedRoute.getDemand().isCoupled())
            {
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, Sets.newHashSet(pickedRoute.getDemand().getCoupledLink()), null, true);
                drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
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
        resetPickedState();
        this.pickedElementType = NetworkElementType.MULTICAST_TREE;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedTrees;
        if (pickedTrees.size() == 1) this.addElementToPickTimeline(pickedTrees.get(0));

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
                    drawColateralLinks(linksPrimary, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
                {
                    final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downInfo = getDownCoupling(pickedTree.getSeqLinksToEgressNode(egressNode));
                    final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downInfo.getFirst(), null, downInfo.getSecond(), false);
                    drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                    drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedTree.getMulticastDemand().isCoupled())
                {
                    final Set<Link> upperCoupledLink = getUpCoupling(null, Arrays.asList(Pair.of(pickedTree.getMulticastDemand(), egressNode)));
                    final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, upperCoupledLink, null, true);
                    drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                    drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
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
        resetPickedState();
        this.pickedElementType = NetworkElementType.LINK;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedLinks;
        if (pickedLinks.size() == 1) this.addElementToPickTimeline(pickedLinks.get(0));

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
                drawColateralLinks(Sets.union(Sets.union(linksPrimary, linksBackup), linksMulticast), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedLink.isCoupled())
            {
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfo = getDownCoupling(Arrays.asList(pickedLink));
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downLayerInfo.getFirst(), null, downLayerInfo.getSecond(), false);
                drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
            {
                if (thisLayerTraversalInfo == null)
                    thisLayerTraversalInfo = pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink();
                final Set<Demand> demandsPrimaryAndBackup = Sets.union(thisLayerTraversalInfo.getFirst().keySet(), thisLayerTraversalInfo.getSecond().keySet());
                final Set<Pair<MulticastDemand, Node>> mDemands = thisLayerTraversalInfo.getThird().keySet();
                final Set<Link> initialUpperLinks = getUpCoupling(demandsPrimaryAndBackup, mDemands);
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, Sets.newHashSet(initialUpperLinks), null, true);
                drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            /* Picked link the last, so overrides the rest */
            if (isLinkLayerVisibleInTheCanvas)
            {
                final GUILink gl = vs.getCanvasAssociatedGUILink(pickedLink);
                gl.setHasArrow(true);
                setCurrentDefaultEdgeStroke(gl, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
                final Paint color = pickedLink.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED;
                gl.setEdgeDrawPaint(color);
                gl.setShownSeparated(true);
            }
        }
    }

    void pickNode(List<Node> pickedNodes)
    {
        resetPickedState();
        this.pickedElementType = NetworkElementType.NODE;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedNodes;
        if (pickedNodes.size() == 1) this.addElementToPickTimeline(pickedNodes.get(0));

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
        resetPickedState();
        this.pickedElementType = NetworkElementType.RESOURCE;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedResources;
        if (pickedResources.size() == 1) this.addElementToPickTimeline(pickedResources.get(0));

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
        resetPickedState();
        this.pickedElementType = NetworkElementType.FORWARDING_RULE;
        this.pickedForwardingRule = pickedFRs;
        this.pickedElement = null;
        if (pickedFRs.size() == 1) this.addElementToPickTimeline(pickedFRs.get(0));

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
                drawColateralLinks(Sets.union(linksPrimary, linksBackup), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedLink.isCoupled())
            {
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfo = getDownCoupling(Arrays.asList(pickedLink));
                final InterLayerPropagationGraph ipgCausedByLink = new InterLayerPropagationGraph(downLayerInfo.getFirst(), null, downLayerInfo.getSecond(), false);
                final Set<Link> frPropagationLinks = ipgCausedByLink.getLinksInGraph();
                drawColateralLinks(frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedDemand.isCoupled())
            {
                final InterLayerPropagationGraph ipgCausedByDemand = new InterLayerPropagationGraph(null, Sets.newHashSet(pickedDemand.getCoupledLink()), null, true);
                final Set<Link> frPropagationLinks = ipgCausedByDemand.getLinksInGraph();
                drawColateralLinks(frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            /* Picked link the last, so overrides the rest */
            if (isFRLayerVisibleInTheCanvas)
            {
                final GUILink gl = vs.getCanvasAssociatedGUILink(pickedLink);
                gl.setHasArrow(true);
                setCurrentDefaultEdgeStroke(gl, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
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

    void resetPickedState()
    {
        this.pickedElementType = null;
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
            setCurrentDefaultEdgeStroke(e, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_ACTIVELAYER, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE);
            final boolean isDown = e.getAssociatedNetPlanLink().isDown();
            final Paint color = isDown ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR;
            e.setEdgeDrawPaint(color);
            e.setShownSeparated(isDown);
        }
        for (GUILink e : vs.getCanvasAllGUILinks(false, true))
        {
            e.setHasArrow(VisualizationConstants.DEFAULT_INTRANODEGUILINK_HASARROW);
            setCurrentDefaultEdgeStroke(e, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE);
            e.setEdgeDrawPaint(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR);
            e.setShownSeparated(false);
        }
    }

    private void drawColateralLinks(Collection<Link> links, Paint colorIfNotFailedLink)
    {
        for (Link link : links)
        {
            final GUILink glColateral = vs.getCanvasAssociatedGUILink(link);
            if (glColateral == null) continue;
            setCurrentDefaultEdgeStroke(glColateral, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALACTVELAYER, VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALNONACTIVELAYER);
            final Paint color = link.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : colorIfNotFailedLink;
            glColateral.setEdgeDrawPaint(color);
            glColateral.setShownSeparated(true);
            glColateral.setHasArrow(true);
        }
    }

    private static Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> getDownCoupling(Collection<Link> links)
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

    private static Set<Link> getUpCoupling(Collection<Demand> demands, Collection<Pair<MulticastDemand, Node>> mDemands)
    {
        final Set<Link> res = new HashSet<>();
        if (demands != null)
            for (Demand d : demands)
                if (d.isCoupled()) res.add(d.getCoupledLink());
        if (mDemands != null)
            for (Pair<MulticastDemand, Node> md : mDemands)
            {
                if (md.getFirst().isCoupled())
                    res.add(md.getFirst().getCoupledLinks().stream().filter(e -> e.getDestinationNode() == md.getSecond()).findFirst().get());
            }
        return res;
    }

    private void drawDownPropagationInterLayerLinks(Set<Link> links, Paint color)
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
                setCurrentDefaultEdgeStroke(interLayerLink, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED);
                interLayerLink.setEdgeDrawPaint(color);
                interLayerLink.setShownSeparated(false);
                interLayerLink.setHasArrow(true);
            }
            for (GUILink interLayerLink : vs.getCanvasIntraNodeGUILinkSequence(link.getDestinationNode(), lowerLayer, upperLayer))
            {
                setCurrentDefaultEdgeStroke(interLayerLink, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED, VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED);
                interLayerLink.setEdgeDrawPaint(color);
                interLayerLink.setShownSeparated(false);
                interLayerLink.setHasArrow(true);
            }
        }
    }

    private void setCurrentDefaultEdgeStroke(GUILink e, BasicStroke a, BasicStroke na)
    {
        e.setEdgeStroke(vs.resizedBasicStroke(a, vs.getLinkWidthFactor()), vs.resizedBasicStroke(na, vs.getLinkWidthFactor()));
    }

    private void addElementToPickTimeline(NetworkElement element)
    {
        pickTimeLineManager.addElement(vs.getNetPlan(), element);
    }

    private void addElementToPickTimeline(Pair<Demand, Link> element)
    {
        pickTimeLineManager.addElement(vs.getNetPlan(), element);
    }

    Object getPickNavigationBackElement()
    {
        return pickTimeLineManager.getPickNavigationBackElement();
    }

    Object getPickNavigationForwardElement()
    {
        return pickTimeLineManager.getPickNavigationForwardElement();
    }
}
