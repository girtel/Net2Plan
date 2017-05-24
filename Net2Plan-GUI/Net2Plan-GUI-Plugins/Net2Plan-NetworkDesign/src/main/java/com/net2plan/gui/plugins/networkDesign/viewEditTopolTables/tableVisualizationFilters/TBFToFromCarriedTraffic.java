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
package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import java.util.*;
import java.util.stream.Collectors;

public class TBFToFromCarriedTraffic extends ITableRowFilter
{
    private final boolean onlyThisLayer;
    private final NetworkElement initialElement;
    private final Pair<Demand, Link> initialFR;
    private final NetworkLayer auxLayerInNodes;

    public TBFToFromCarriedTraffic(Demand demand, boolean onlyThisLayer)
    {
        super(demand.getNetPlan());

        this.initialElement = demand;
        this.onlyThisLayer = onlyThisLayer;
        this.initialFR = null;
        this.auxLayerInNodes = null;

        final Set<Link> linksAllLayers = new HashSet<>();
        final Set<Demand> demandsAllLayers = new HashSet<>();
        final Set<MulticastDemand> mDemandsAllLayers = new HashSet<>();

        demandsAllLayers.add(demand);

        final Pair<Set<Link>, Set<Link>> thisLayerPropagation = demand.getLinksThisLayerPotentiallyCarryingTraffic();
        linksAllLayers.addAll(thisLayerPropagation.getFirst());
        linksAllLayers.addAll(thisLayerPropagation.getSecond());
        if (!onlyThisLayer)
            updatePropagationDownWards(linksAllLayers, linksAllLayers, demandsAllLayers, mDemandsAllLayers);
        if (!onlyThisLayer)
            updatePropagationUpWards(Arrays.asList(demand), null, linksAllLayers, demandsAllLayers, mDemandsAllLayers);
        final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer ? Sets.difference(new HashSet<>(netPlan.getNetworkLayers()), Sets.newHashSet(demand.getLayer())) : new HashSet<>();
        updateAllButLinksDemandsMDemandsUsingExistingInfo(linksAllLayers, demandsAllLayers, mDemandsAllLayers, layersToKeepAllElements);
    }

    public TBFToFromCarriedTraffic(Link link, boolean onlyThisLayer)
    {
        super(link.getNetPlan());

        this.initialElement = link;
        this.onlyThisLayer = onlyThisLayer;
        this.initialFR = null;
        this.auxLayerInNodes = null;

        final Set<Link> linksAllLayers = new HashSet<>();
        final Set<Demand> demandsAllLayers = new HashSet<>();
        final Set<MulticastDemand> mDemandsAllLayers = new HashSet<>();

        linksAllLayers.add(link);

        Triple<Map<Demand, Set<Link>>, Map<Demand, Set<Link>>, Map<Pair<MulticastDemand, Node>, Set<Link>>> thisLayerTraversalInfo =
                link.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink();
        linksAllLayers.addAll(thisLayerTraversalInfo.getFirst().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet()));
        linksAllLayers.addAll(thisLayerTraversalInfo.getSecond().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet()));
        linksAllLayers.addAll(thisLayerTraversalInfo.getThird().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet()));
        demandsAllLayers.addAll(thisLayerTraversalInfo.getFirst().keySet());
        demandsAllLayers.addAll(thisLayerTraversalInfo.getSecond().keySet());
        mDemandsAllLayers.addAll(thisLayerTraversalInfo.getThird().keySet().stream().map(p -> p.getFirst()).collect(Collectors.toSet()));
        if (!onlyThisLayer)
            updatePropagationDownWards(Arrays.asList(link), linksAllLayers, demandsAllLayers, mDemandsAllLayers);
        if (!onlyThisLayer)
            updatePropagationUpWards(demandsAllLayers, mDemandsAllLayers, linksAllLayers, demandsAllLayers, mDemandsAllLayers);
        final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer ? Sets.difference(new HashSet<>(netPlan.getNetworkLayers()), Sets.newHashSet(link.getLayer())) : new HashSet<>();
        updateAllButLinksDemandsMDemandsUsingExistingInfo(linksAllLayers, demandsAllLayers, mDemandsAllLayers, layersToKeepAllElements);
    }

    public TBFToFromCarriedTraffic(MulticastTree tree, boolean onlyThisLayer)
    {
        super(tree.getNetPlan());

        this.initialElement = tree;
        this.onlyThisLayer = onlyThisLayer;
        this.initialFR = null;
        this.auxLayerInNodes = null;

        final Set<Link> linksAllLayers = new HashSet<>();
        final Set<Demand> demandsAllLayers = new HashSet<>();
        final Set<MulticastDemand> mDemandsAllLayers = new HashSet<>();

        mDemandsAllLayers.add(tree.getMulticastDemand());
        linksAllLayers.addAll(tree.getLinkSet());
        if (!onlyThisLayer)
            updatePropagationDownWards(linksAllLayers, linksAllLayers, demandsAllLayers, mDemandsAllLayers);
        if (!onlyThisLayer)
            updatePropagationUpWards(demandsAllLayers, mDemandsAllLayers, linksAllLayers, demandsAllLayers, mDemandsAllLayers);
        final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer ? Sets.difference(new HashSet<>(netPlan.getNetworkLayers()), Sets.newHashSet(tree.getLayer())) : new HashSet<>();
        updateAllButLinksDemandsMDemandsUsingExistingInfo(linksAllLayers, demandsAllLayers, mDemandsAllLayers, layersToKeepAllElements);
    }

    public TBFToFromCarriedTraffic(Route route, boolean onlyThisLayer)
    {
        super(route.getNetPlan());

        this.initialElement = route;
        this.onlyThisLayer = onlyThisLayer;
        this.initialFR = null;
        this.auxLayerInNodes = null;

        final Set<Link> linksAllLayers = new HashSet<>();
        final Set<Demand> demandsAllLayers = new HashSet<>();
        final Set<MulticastDemand> mDemandsAllLayers = new HashSet<>();

        demandsAllLayers.add(route.getDemand());
        linksAllLayers.addAll(route.getSeqLinks());

        if (!onlyThisLayer)
            updatePropagationDownWards(linksAllLayers, linksAllLayers, demandsAllLayers, mDemandsAllLayers);
        if (!onlyThisLayer)
            updatePropagationUpWards(demandsAllLayers, mDemandsAllLayers, linksAllLayers, demandsAllLayers, mDemandsAllLayers);
        final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer ? Sets.difference(new HashSet<>(netPlan.getNetworkLayers()), Sets.newHashSet(route.getLayer())) : new HashSet<>();
        updateAllButLinksDemandsMDemandsUsingExistingInfo(linksAllLayers, demandsAllLayers, mDemandsAllLayers, layersToKeepAllElements);
    }

    public TBFToFromCarriedTraffic(Resource resource, NetworkLayer layer, boolean onlyThisLayer)
    {
        super(resource.getNetPlan());

        this.initialElement = resource;
        this.onlyThisLayer = onlyThisLayer;
        this.initialFR = null;
        this.auxLayerInNodes = layer;

        final Set<Link> linksAllLayers = new HashSet<>();
        final Set<Demand> demandsAllLayers = new HashSet<>();
        final Set<MulticastDemand> mDemandsAllLayers = new HashSet<>();

        // if the resource is not used => does not appear anywhere
        for (Route r : resource.getTraversingRoutes())
        {
            if (r.getLayer() != layer) continue;
            linksAllLayers.addAll(r.getSeqLinks());
            demandsAllLayers.add(r.getDemand());
        }
        if (!onlyThisLayer)
            updatePropagationDownWards(linksAllLayers, linksAllLayers, demandsAllLayers, mDemandsAllLayers);
        if (!onlyThisLayer)
            updatePropagationUpWards(demandsAllLayers, mDemandsAllLayers, linksAllLayers, demandsAllLayers, mDemandsAllLayers);
        final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer ? Sets.difference(new HashSet<>(netPlan.getNetworkLayers()), Sets.newHashSet(layer)) : new HashSet<>();
        updateAllButLinksDemandsMDemandsUsingExistingInfo(linksAllLayers, demandsAllLayers, mDemandsAllLayers, layersToKeepAllElements);
        if (!this.vResources.get(layer).contains(resource))
            this.vResources.get(layer).add(resource); // add the resource if not there
    }

    public TBFToFromCarriedTraffic(Node node, NetworkLayer layer, boolean onlyThisLayer)
    {
        super(node.getNetPlan());

        this.initialElement = node;
        this.onlyThisLayer = onlyThisLayer;
        this.initialFR = null;
        this.auxLayerInNodes = layer;

        final Set<Link> linksAllLayers = new HashSet<>();
        final Set<Demand> demandsAllLayers = new HashSet<>();
        final Set<MulticastDemand> mDemandsAllLayers = new HashSet<>();

        demandsAllLayers.addAll(node.getIncomingDemands(layer));
        demandsAllLayers.addAll(node.getOutgoingDemands(layer));
        linksAllLayers.addAll(node.getIncomingLinks(layer));
        linksAllLayers.addAll(node.getOutgoingLinks(layer));
        mDemandsAllLayers.addAll(node.getIncomingMulticastDemands(layer));
        mDemandsAllLayers.addAll(node.getOutgoingMulticastDemands(layer));

        if (!onlyThisLayer)
            updatePropagationDownWards(linksAllLayers, linksAllLayers, demandsAllLayers, mDemandsAllLayers);
        if (!onlyThisLayer)
            updatePropagationUpWards(demandsAllLayers, mDemandsAllLayers, linksAllLayers, demandsAllLayers, mDemandsAllLayers);
        final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer ? Sets.difference(new HashSet<>(netPlan.getNetworkLayers()), Sets.newHashSet(layer)) : new HashSet<>();
        updateAllButLinksDemandsMDemandsUsingExistingInfo(linksAllLayers, demandsAllLayers, mDemandsAllLayers, layersToKeepAllElements);
    }

    public TBFToFromCarriedTraffic(SharedRiskGroup srg)
    {
        super(srg.getNetPlan());

        this.initialElement = srg;
        this.onlyThisLayer = false;
        this.initialFR = null;
        this.auxLayerInNodes = null;

        final Set<Link> linksAllLayers = new HashSet<>();
        final Set<Demand> demandsAllLayers = new HashSet<>();
        final Set<MulticastDemand> mDemandsAllLayers = new HashSet<>();

        for (Link affectedLink : srg.getAffectedLinksAllLayers())
        {
            final TBFToFromCarriedTraffic affectedThisLink = new TBFToFromCarriedTraffic(affectedLink, false);
            for (NetworkLayer layer : netPlan.getNetworkLayers())
            {
                linksAllLayers.addAll(affectedThisLink.getVisibleLinks(layer));
                demandsAllLayers.addAll(affectedThisLink.getVisibleDemands(layer));
                mDemandsAllLayers.addAll(affectedThisLink.getVisibleMulticastDemands(layer));
            }
        }
        final Set<NetworkLayer> layersToKeepAllElements = new HashSet<>();
        updateAllButLinksDemandsMDemandsUsingExistingInfo(linksAllLayers, demandsAllLayers, mDemandsAllLayers, layersToKeepAllElements);
    }

    public TBFToFromCarriedTraffic(Pair<Demand, Link> fr, boolean onlyThisLayer)
    {
        super(fr.getFirst().getNetPlan());

        this.initialElement = null;
        this.onlyThisLayer = onlyThisLayer;
        this.initialFR = fr;
        this.auxLayerInNodes = null;

        final Demand frDemand = fr.getFirst();
        final Link frLink = fr.getSecond();

        final Set<Link> linksAllLayers = new HashSet<>();
        final Set<Demand> demandsAllLayers = new HashSet<>();
        final Set<MulticastDemand> mDemandsAllLayers = new HashSet<>();

        demandsAllLayers.add(frDemand);
        linksAllLayers.add(frLink);

        if (!onlyThisLayer)
            updatePropagationDownWards(linksAllLayers, linksAllLayers, demandsAllLayers, mDemandsAllLayers);
        if (!onlyThisLayer)
            updatePropagationUpWards(demandsAllLayers, mDemandsAllLayers, linksAllLayers, demandsAllLayers, mDemandsAllLayers);

        final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer ? Sets.difference(new HashSet<>(netPlan.getNetworkLayers()), Sets.newHashSet(frLink.getLayer())) : new HashSet<>();
        updateAllButLinksDemandsMDemandsUsingExistingInfo(linksAllLayers, demandsAllLayers, mDemandsAllLayers, layersToKeepAllElements);
    }

    public TBFToFromCarriedTraffic(MulticastDemand demand, boolean onlyThisLayer)
    {
        super(demand.getNetPlan());

        this.initialElement = demand;
        this.onlyThisLayer = onlyThisLayer;
        this.initialFR = null;
        this.auxLayerInNodes = null;

        final Set<Link> linksAllLayers = new HashSet<>();
        final Set<Demand> demandsAllLayers = new HashSet<>();
        final Set<MulticastDemand> mDemandsAllLayers = new HashSet<>();

        mDemandsAllLayers.add(demand);

        final Set<Link> thisLayerPropagation = new HashSet<>();
        for (Node egressNode : demand.getEgressNodes())
            thisLayerPropagation.addAll(demand.getLinksThisLayerPotentiallyCarryingTraffic(egressNode));
        linksAllLayers.addAll(thisLayerPropagation);

        if (!onlyThisLayer)
            updatePropagationDownWards(linksAllLayers, linksAllLayers, demandsAllLayers, mDemandsAllLayers);
        if (!onlyThisLayer)
            updatePropagationUpWards(demandsAllLayers, mDemandsAllLayers, linksAllLayers, demandsAllLayers, mDemandsAllLayers);

        final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer ? Sets.difference(new HashSet<>(netPlan.getNetworkLayers()), Sets.newHashSet(demand.getLayer())) : new HashSet<>();
        updateAllButLinksDemandsMDemandsUsingExistingInfo(linksAllLayers, demandsAllLayers, mDemandsAllLayers, layersToKeepAllElements);
    }


    public String getDescription()
    {
        if (!chainOfDescriptionsPreviousFiltersComposingThis.isEmpty())
            return "Multiple chained filters (" + (chainOfDescriptionsPreviousFiltersComposingThis.size() + 1) + ")";

        StringBuffer st = new StringBuffer();
        if (initialFR != null)
        {
            st.append(onlyThisLayer ? "(Affecting to layer " + getLayerName(initialFR.getFirst().getLayer()) + ") " : "(Affecting to all layers). ");
            st.append("Elements associated to forwarding rule (" + initialFR.getFirst().getIndex() + "," + initialFR.getSecond().getIndex() + ") in layer " + getLayerName(initialFR.getFirst().getLayer()));
        } else
        {
            if (initialElement instanceof Demand)
            {
                final Demand e = (Demand) initialElement;
                st.append(onlyThisLayer ? "(Affecting to layer " + getLayerName(e.getLayer()) + ") " : "(Affecting to all layers). ");
                st.append("Elements associated to demand " + e.getIndex() + " in layer " + getLayerName(e.getLayer()));
            } else if (initialElement instanceof Link)
            {
                final Link e = (Link) initialElement;
                st.append(onlyThisLayer ? "(Affecting to layer " + getLayerName(e.getLayer()) + ") " : "(Affecting to all layers). ");
                st.append("Elements associated to link " + e.getIndex() + " in layer " + getLayerName(e.getLayer()));
            } else if (initialElement instanceof MulticastDemand)
            {
                final MulticastDemand e = (MulticastDemand) initialElement;
                st.append(onlyThisLayer ? "(Affecting to layer " + getLayerName(e.getLayer()) + ") " : "(Affecting to all layers). ");
                st.append("Elements associated to multicast demand " + e.getIndex() + " in layer " + getLayerName(e.getLayer()));
            } else if (initialElement instanceof MulticastTree)
            {
                final MulticastTree e = (MulticastTree) initialElement;
                st.append(onlyThisLayer ? "(Affecting to layer " + getLayerName(e.getLayer()) + ") " : "(Affecting to all layers). ");
                st.append("Elements associated to multicast tree " + e.getIndex() + " in layer " + getLayerName(e.getLayer()));
            } else if (initialElement instanceof Node)
            {
                final Node e = (Node) initialElement;
                st.append(onlyThisLayer ? "(Affecting to layer " + getLayerName(this.auxLayerInNodes) + ") " : "(Affecting to all layers). ");
                st.append("Elements associated to node " + getNodeName(e) + " in layer " + getLayerName(this.auxLayerInNodes));
            } else if (initialElement instanceof Resource)
            {
                final Resource e = (Resource) initialElement;
                st.append(onlyThisLayer ? "(Affecting to layer " + getLayerName(this.auxLayerInNodes) + ") " : "(Affecting to all layers). ");
                st.append("Elements associated to node " + getResourceName(e) + " in layer " + getLayerName(this.auxLayerInNodes));

            } else if (initialElement instanceof Route)
            {
                final Route e = (Route) initialElement;
                st.append(onlyThisLayer ? "(Affecting to layer " + getLayerName(e.getLayer()) + ") " : "(Affecting to all layers). ");
                st.append("Elements associated to route " + e.getIndex() + " in layer " + getLayerName(e.getLayer()));
            } else if (initialElement instanceof SharedRiskGroup)
            {
                final SharedRiskGroup e = (SharedRiskGroup) initialElement;
                st.append("Elements in all layers affected by failure in SRG " + e.getIndex());
            }
        }
        return st.toString();
    }


    private void updatePropagationDownWards(Collection<Link> linksToPropagateDown, Set<Link> linksAllLayersToUpdate, Set<Demand> demandsAllLayersToUpdate,
                                            Set<MulticastDemand> mDemandsAllLayersToUpdate)
    {
        if (netPlan.getNumberOfLayers() > 1)
        {
            final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downInfo = getDownCoupling(linksToPropagateDown);
            if (downInfo.getFirst().isEmpty() && downInfo.getSecond().isEmpty()) return;
            final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downInfo.getFirst(), null, downInfo.getSecond(), false);
            linksAllLayersToUpdate.addAll(ipg.getLinksInGraph());
            demandsAllLayersToUpdate.addAll(ipg.getDemandsInGraph());
            mDemandsAllLayersToUpdate.addAll(ipg.getMulticastDemandFlowsInGraph().stream().map(p -> p.getFirst()).collect(Collectors.toSet()));
        }
    }

    private void updatePropagationUpWards(Collection<Demand> demandsToPropagateUp, Collection<MulticastDemand> mDemandsToPropagateUp,
                                          Set<Link> linksAllLayersToUpdate, Set<Demand> demandsAllLayersToUpdate,
                                          Set<MulticastDemand> mDemandsAllLayersToUpdate)
    {
        if (netPlan.getNumberOfLayers() > 1)
        {
            final Set<Pair<MulticastDemand, Node>> mDemandsAllEgressNodes = new HashSet<Pair<MulticastDemand, Node>>();
            if (mDemandsToPropagateUp != null)
                for (MulticastDemand md : mDemandsToPropagateUp)
                    for (Node n : md.getEgressNodes())
                        mDemandsAllEgressNodes.add(Pair.of(md, n));
            final Set<Link> initialUpperLinks = getUpCoupling(demandsToPropagateUp, mDemandsAllEgressNodes);
            if (initialUpperLinks.isEmpty()) return;
            final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, initialUpperLinks, null, true);
            linksAllLayersToUpdate.addAll(ipg.getLinksInGraph());
            demandsAllLayersToUpdate.addAll(ipg.getDemandsInGraph());
            mDemandsAllLayersToUpdate.addAll(ipg.getMulticastDemandFlowsInGraph().stream().map(p -> p.getFirst()).collect(Collectors.toSet()));
        }
    }

    private static String getLayerName(NetworkLayer layer)
    {
        return layer.getName().equals("") ? "Layer " + layer.getIndex() : layer.getName();
    }

    private static String getNodeName(Node n)
    {
        return n.getName().equals("") ? "Node " + n.getIndex() : n.getName();
    }

    private static String getResourceName(Resource r)
    {
        return r.getName().equals("") ? "Resource " + r.getIndex() : r.getName();
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

    private void updateAllButLinksDemandsMDemandsUsingExistingInfo(Set<Link> linksAllLayers, Set<Demand> demandsAllLayers, Set<MulticastDemand> mDemandsAllLayers, Set<NetworkLayer> layersToKeepAllElements)
    {
        /* update the rest according to this */
        for (NetworkLayer layer : netPlan.getNetworkLayers())
        {
            final boolean isSourceRouting = layer.isSourceRouting();
            if (layersToKeepAllElements.contains(layer))
            {
				/* keep this layer unchanged */
                vDemands.put(layer, netPlan.getDemands(layer));
                if (!isSourceRouting) vFRs.put(layer, new ArrayList<>(netPlan.getForwardingRules(layer).keySet()));
                if (isSourceRouting) vRoutes.put(layer, netPlan.getRoutes(layer));
                vLinks.put(layer, netPlan.getLinks(layer));
                vMDemands.put(layer, netPlan.getMulticastDemands(layer));
                vTrees.put(layer, netPlan.getMulticastTrees(layer));
                vNodes.put(layer, netPlan.getNodes());
                vResources.put(layer, netPlan.getResources());
                vSRGs.put(layer, netPlan.getSRGs());
                continue;
            }
				
			/* Here if we filter out by this layer */
            final Set<Link> links = linksAllLayers.stream().filter(e -> e.getLayer().equals(layer)).collect(Collectors.toSet());
            final Set<Demand> demands = demandsAllLayers.stream().filter(e -> e.getLayer().equals(layer)).collect(Collectors.toSet());
            final Set<MulticastDemand> mDemands = mDemandsAllLayers.stream().filter(e -> e.getLayer().equals(layer)).collect(Collectors.toSet());

            final Set<Route> routes = new HashSet<>();
            final Set<MulticastTree> trees = new HashSet<>();
            final Set<Resource> resources = new HashSet<>();
            final Set<SharedRiskGroup> srgs = new HashSet<>();
            final Set<Pair<Demand, Link>> frs = new HashSet<>();
            final Set<Node> nodes = new HashSet<>();
			
			/* Update the FRs: all of the ones associated to the demands */
            if (!isSourceRouting)
                for (Demand d : demands) frs.addAll(d.getForwardingRules().keySet());

			/* The routes and the resources: all the ones of a demand in 
			 * the set that traverse a link in the set => for them the traversed resources */
            if (isSourceRouting)
                for (Link e : links)
                    for (Route r : e.getTraversingRoutes())
                        if (demands.contains(r.getDemand()))
                        {
                            routes.add(r);
                            resources.addAll(r.getSeqResourcesTraversed());
                        }
			
			/* The multicast trees: the ones in the multicast demands AND also traverse involved links */
            for (Link e : links)
                for (MulticastTree t : e.getTraversingTrees())
                    if (mDemands.contains(t.getMulticastDemand()))
                        trees.add(t);

			/* The nodes: the ones in any link */
            for (Link e : links)
            {
                nodes.add(e.getOriginNode());
                nodes.add(e.getDestinationNode());
            }
			
			/* The SRGs: the ones affecting any of the links in the set */
            srgs.addAll(SRGUtils.getAffectingSRGs(links));

            vDemands.put(layer, (List<Demand>) orderSet(demands));
            vFRs.put(layer, (List<Pair<Demand, Link>>) orderSetFR(frs));
            vLinks.put(layer, (List<Link>) orderSet(links));
            vMDemands.put(layer, (List<MulticastDemand>) orderSet(mDemands));
            vTrees.put(layer, (List<MulticastTree>) orderSet(trees));
            vNodes.put(layer, (List<Node>) orderSet(nodes));
            vResources.put(layer, (List<Resource>) orderSet(resources));
            vRoutes.put(layer, (List<Route>) orderSet(routes));
            vSRGs.put(layer, (List<SharedRiskGroup>) orderSet(srgs));
        }
    }
}
