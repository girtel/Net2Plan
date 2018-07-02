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

import java.awt.BasicStroke;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;
import java.util.List;
import java.util.SortedMap;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUILink;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.gui.utils.NetworkElementOrFr;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.InterLayerPropagationGraph;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

/**
 */
public class PickManager
{
	private final VisualizationState vs;
	private final GUINetworkDesign callback;
    private final List<PickStateInfo> timeLine;
    private int timelineCursor;
    private final int timelineMaxSize;

    public PickManager(GUINetworkDesign callback , final int timelineMaxSize)
    {
        this.callback = callback;
        this.vs = callback.getVisualizationState();
        this.timelineMaxSize = timelineMaxSize;
        this.timeLine = new ArrayList<>(timelineMaxSize + 1);
        this.timelineCursor = -1;
    }

    private void addElement (final PickStateInfo element)
    {
        if (this.timelineMaxSize <= 1) return;
        if (element == null) throw new RuntimeException("Cannot add a null element.");

        // Check pointer validity
        if (!timeLine.isEmpty())
        {
            if (!(timelineCursor >= 0 && timelineCursor < timeLine.size()))
                throw new RuntimeException("Timeline cursor has been misplaced.");
        } else
        {
            if (timelineCursor != -1)
                throw new RuntimeException("Timeline cursor has been misplaced.");
        }

        // Sanity check: remove duplicates
        cleanTimeLineDuty();

        // Clean duty can leave the timeline empty.
        if (!(timeLine.isEmpty() && timelineCursor == -1))
        {
            // Do not add the same element that is currently be clicked upon.
            if (element.equals(timeLine.get(timelineCursor))) return;

            // If the new element if different from what is stored, remove all the elements that were stored
            if (timelineCursor != (timeLine.size() - 1))
            {
                final int nextElementCursorIndex = timelineCursor + 1;

                final PickStateInfo nextTimelineElement = timeLine.get(nextElementCursorIndex);
                final PickStateInfo currentTimelineElement = timeLine.get(timelineCursor);

                if (!nextTimelineElement.equals(currentTimelineElement))
                    timeLine.subList(nextElementCursorIndex, timeLine.size()).clear();
            }
        }

        /* Add the elements at the end of the list */
        timeLine.add(element);
        timelineCursor++;

        // Remove the oldest pick if the list get too big.
        if (timeLine.size() > timelineMaxSize)
        {
            timeLine.remove(0);
            timelineCursor--;
        }
    }

    @SuppressWarnings("unchecked")
    private void cleanTimeLineDuty()
    {
        if (timeLine.size() > timelineMaxSize) throw new RuntimeException("Timeline is over its capacity.");

        final List<PickStateInfo> newTimeLine = new ArrayList<>(timeLine);
        for (int index = 0; index < timeLine.size(); index++)
        {
            final PickStateInfo o = timeLine.get(index);
            // Do not have duplicate elements next to each other
            if (index != timeLine.size() - 1)
            {
                if (o == timeLine.get(index + 1))
                {
                    newTimeLine.remove(o);
                    timelineCursor--;
                }
            }
        }
        this.timeLine.clear();
        this.timeLine.addAll(newTimeLine);
    }

    public Optional<PickStateInfo> getCurrentPick (NetPlan targetNp)
    {
    	if (timeLine.isEmpty()) return Optional.empty();
    	return timeLine.get(timelineCursor).getTranslationToGivenNetPlan(targetNp);
    }
    
    public Optional<PickStateInfo> getPickNavigationBackElement(NetPlan targetNp)
    {
        if (timeLine.isEmpty() || this.timelineMaxSize <= 1) return Optional.empty();
        if (timelineCursor == 0) return Optional.empty(); // End of the timeline, there is no more past.
    	do
    	{
            cleanTimeLineDuty();
            if (timeLine.isEmpty() || this.timelineMaxSize <= 1) return Optional.empty();
            if (timelineCursor == 0) return null; // End of the timeline, there is no more past.

            // Clean the timeline before giving anything
            final PickStateInfo info = timeLine.get(--timelineCursor);
            final Optional<PickStateInfo> pickInNewNp = info.getTranslationToGivenNetPlan(targetNp);
            if (pickInNewNp.isPresent()) return pickInNewNp;
    	} while (true);
    }

    public Optional<PickStateInfo> getPickNavigationForwardElement(NetPlan targetNp)
    {
        if (timeLine.isEmpty() || this.timelineMaxSize <= 1) return Optional.empty();
        if (timelineCursor == timeLine.size() - 1) return Optional.empty();
    	do
    	{
            cleanTimeLineDuty();
            if (timeLine.isEmpty() || this.timelineMaxSize <= 1) return Optional.empty();
            if (timelineCursor == timeLine.size() - 1) return Optional.empty();

            // Clean the timeline before giving anything
            final PickStateInfo info = timeLine.get(++timelineCursor);
            final Optional<PickStateInfo> pickInNewNp = info.getTranslationToGivenNetPlan(targetNp);
            if (pickInNewNp.isPresent()) return pickInNewNp;
    	} while (true);
    }

	public PickStateInfo createPickStateFromListNe (Collection<? extends NetworkElement> state) 
	{
		final List<Pair<NetworkElementOrFr,NetworkLayer>> newState = new ArrayList<> ();
		for (NetworkElement ne : state)
		{
			final NetworkElementOrFr nefr = new NetworkElementOrFr(ne);
			final NetworkLayer layer = nefr.getOrEstimateLayer();
			newState.add(Pair.of(nefr, layer));
		}
		return this.new PickStateInfo(newState);
	}

    public class PickStateInfo
    {
    	final private List<Pair<NetworkElementOrFr,NetworkLayer>> state;

		public PickStateInfo(NetworkElement ne , Optional<NetworkLayer> layer) 
		{
			final NetworkElementOrFr nefr = new NetworkElementOrFr(ne);
			if (!layer.isPresent()) layer = Optional.of(nefr.getOrEstimateLayer());
			this.state = Arrays.asList(Pair.of(nefr, layer.get()));
		}
		public PickStateInfo(Pair<Demand,Link> fr , Optional<NetworkLayer> layer)
		{
			final NetworkElementOrFr nefr = new NetworkElementOrFr(fr);
			if (!layer.isPresent()) layer = Optional.of(nefr.getOrEstimateLayer());
			this.state = Arrays.asList(Pair.of(nefr, layer.get()));
		}
		public PickStateInfo(List<Pair<NetworkElementOrFr, NetworkLayer>> state) 
		{
			this.state = state;
		}
		public List<Pair<NetworkElementOrFr, NetworkLayer>> getState() 
		{
			return state;
		}
		public List<NetworkElementOrFr> getStateOnlyNeFr() 
		{
			return state.stream().map(p->p.getFirst()).collect(Collectors.toList());
		}
		public Optional<Pair<NetworkElementType,NetworkLayer>> getElementTypeOfMainElement () 
		{
			if (state == null) return Optional.empty();
			if (state.isEmpty()) return Optional.empty();
			return Optional.of(Pair.of(state.get(0).getFirst().getElementType() , state.get(0).getSecond()));
		}
		public Optional<NetworkElementOrFr> getMainElement () 
		{
			if (state == null) return Optional.empty();
			if (state.isEmpty()) return Optional.empty();
			return Optional.of(state.get(0).getFirst());
		}
		
		public boolean isFullyTranslatableToGivenNetPlan (NetPlan targetNp)
		{
			return getTranslationToGivenNetPlan (targetNp).isPresent();
		}
		public Optional<PickStateInfo> getTranslationToGivenNetPlan (NetPlan targetNp)
		{
			final List<Pair<NetworkElementOrFr, NetworkLayer>> newState = new ArrayList<> ();
			for (Pair<NetworkElementOrFr,NetworkLayer> element : state)
			{
				final NetworkElementOrFr nefr = element.getFirst();
				final NetworkLayer layer = element.getSecond();
				final NetworkLayer layerNewNp = targetNp.getNetworkLayerFromId(layer.getId());
				if (layerNewNp == null) return Optional.empty();
				NetworkElementOrFr nefrNewNp = null;
				if (nefr.isNe ())
				{
					final NetworkElement ne = nefr.getNe(); if (ne == null) return Optional.empty();
					final NetworkElement neNewNp = targetNp.getNetworkElement(ne.getId());
					if (neNewNp == null) return Optional.empty();
					if (ne.getNeType() != neNewNp.getNeType()) return Optional.empty();
					nefrNewNp = new NetworkElementOrFr(neNewNp);
				}
				else if (nefr.isFr())
				{
					final Pair<Demand,Link> fr = nefr.getFr(); if (fr == null) return Optional.empty();
					final Demand d = fr.getFirst(); 
					final Link e = fr.getSecond(); 
					final Demand dNewNp = targetNp.getDemandFromId(d.getId());
					final Link eNewNp = targetNp.getLinkFromId(e.getId());
					if (dNewNp == null || eNewNp == null) return Optional.empty();
					if (!dNewNp.getLayer().equals(eNewNp.getLayer ())) return Optional.empty();
					nefrNewNp = new NetworkElementOrFr(Pair.of(dNewNp, eNewNp));
				}
				if (nefrNewNp == null) continue;
				newState.add(Pair.of(nefrNewNp, layerNewNp));
			}
			return Optional.of(new PickStateInfo(newState));
		}
	    public boolean isSomethingPicked () { return !isEmptyPick(); }
		public boolean isEmptyPick () { return state.isEmpty(); }
		
		public void applyVisualizationInCurrentDesign ()
		{
	        cleanPick();
	        final Optional<PickStateInfo> pickStateNewNp = getTranslationToGivenNetPlan (callback.getDesign());
	        if (!pickStateNewNp.isPresent()) return;

	        for (Pair<NetworkElementOrFr,NetworkLayer> thisNeFr : state)
			{
				final NetworkElementOrFr nefr = thisNeFr.getFirst();
				final NetworkLayer layerIfnodeResourceEtc = thisNeFr.getSecond();
				switch (nefr.getElementType())
				{
				case DEMAND: 
					applyVisualizationPickDemand(Arrays.asList((Demand) nefr.getNe()));
					break;
				case FORWARDING_RULE:
					applyVisualizationPickForwardingRule(Arrays.asList(nefr.getFr()));
					break;
				case LAYER:
					applyVisualizationPickLayer((NetworkLayer) nefr.getNe());
					break;
				case LINK:
					applyVisualizationPickLink(Arrays.asList((Link) nefr.getNe()));
					break;
				case MULTICAST_DEMAND:
					applyVisualizationPickMulticastDemand(Arrays.asList((MulticastDemand) nefr.getNe()));
					break;
				case MULTICAST_TREE:
					applyVisualizationPickMulticastTree(Arrays.asList((MulticastTree) nefr.getNe()));
					break;
				case NETWORK:
					break;
				case NODE:
					applyVisualizationPickNode(Arrays.asList((Node) nefr.getNe()) , layerIfnodeResourceEtc);
					break;
				case RESOURCE:
					applyVisualizationPickResource(Arrays.asList((Resource) nefr.getNe()) , layerIfnodeResourceEtc);
					break;
				case ROUTE:
					applyVisualizationPickRoute(Arrays.asList((Route) nefr.getNe()));
					break;
				case SRG:
					applyVisualizationPickSRG(Arrays.asList((SharedRiskGroup) nefr.getNe()));
					break;
				default:
					break;
				}
			}
		}
    }
    
    public void reset()
    {
        this.timeLine.clear();
        this.timelineCursor = -1;
        cleanPick();
    }

    private void applyVisualizationPickLayer(NetworkLayer pickedLayer)
    {
    }

    private void applyVisualizationPickDemand(List<Demand> pickedDemands)
    {
        Pair<SortedSet<Link>, SortedSet<Link>> thisLayerPropagation = null;
        for (Demand pickedDemand : pickedDemands)
        {
            final boolean isDemandLayerVisibleInTheCanvas = vs.isLayerVisibleInCanvas(pickedDemand.getLayer());
            final GUINode gnOrigin = vs.getCanvasAssociatedGUINode(pickedDemand.getIngressNode(), pickedDemand.getLayer());
            final GUINode gnDestination = vs.getCanvasAssociatedGUINode(pickedDemand.getEgressNode(), pickedDemand.getLayer());

            if (vs.isShowInCanvasThisLayerPropagation() && isDemandLayerVisibleInTheCanvas)
            {
                thisLayerPropagation = pickedDemand.getLinksNoDownPropagationPotentiallyCarryingTraffic();
                final SortedSet<Link> linksPrimary = thisLayerPropagation.getFirst();
                final SortedSet<Link> linksBackup = thisLayerPropagation.getSecond();
                final SortedSet<Link> linksPrimaryAndBackup = new TreeSet<> (Sets.intersection(linksPrimary, linksBackup));
                DrawUtils.drawCollateralLinks(vs, Sets.difference(linksPrimary, linksPrimaryAndBackup), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawCollateralLinks(vs, Sets.difference(linksBackup, linksPrimaryAndBackup), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
                DrawUtils.drawCollateralLinks(vs, linksPrimaryAndBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
            }

            if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
            {
                if (thisLayerPropagation == null)
                    thisLayerPropagation = pickedDemand.getLinksNoDownPropagationPotentiallyCarryingTraffic();
                final Pair<SortedSet<Demand>, SortedSet<Pair<MulticastDemand, Node>>> downLayerInfoPrimary = DrawUtils.getDownCoupling(thisLayerPropagation.getFirst());
                final Pair<SortedSet<Demand>, SortedSet<Pair<MulticastDemand, Node>>> downLayerInfoBackup = DrawUtils.getDownCoupling(thisLayerPropagation.getSecond());
                final InterLayerPropagationGraph ipgPrimary = new InterLayerPropagationGraph(downLayerInfoPrimary.getFirst(), null, downLayerInfoPrimary.getSecond(), false);
                final InterLayerPropagationGraph ipgBackup = new InterLayerPropagationGraph(downLayerInfoBackup.getFirst(), null, downLayerInfoBackup.getSecond(), false);
                final SortedSet<Link> linksPrimary = ipgPrimary.getLinksInGraph();
                final SortedSet<Link> linksBackup = ipgBackup.getLinksInGraph();
                final SortedSet<Link> linksPrimaryAndBackup = new TreeSet<> (Sets.intersection(linksPrimary, linksBackup));
                final SortedSet<Link> linksOnlyPrimary = new TreeSet<> (Sets.difference(linksPrimary, linksPrimaryAndBackup));
                final SortedSet<Link> linksOnlyBackup = new TreeSet<> (Sets.difference(linksBackup, linksPrimaryAndBackup));
                DrawUtils.drawCollateralLinks(vs, linksOnlyPrimary, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, linksOnlyPrimary, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawCollateralLinks(vs, linksOnlyBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, linksOnlyBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
                DrawUtils.drawCollateralLinks(vs, linksPrimaryAndBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, linksPrimaryAndBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
            }
            if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedDemand.isCoupled())
            {
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, new TreeSet<> (Arrays.asList(pickedDemand.getCoupledLink())), null, true);
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

    private void applyVisualizationPickSRG(List<SharedRiskGroup> pickedSRGs)
    {
        for (SharedRiskGroup pickedSRG : pickedSRGs)
        {
            final SortedSet<Link> allAffectedLinks = pickedSRG.getAffectedLinksAllLayers();
            SortedMap<Link, Triple<SortedMap<Demand, SortedSet<Link>>, SortedMap<Demand, SortedSet<Link>>, SortedMap<Pair<MulticastDemand, Node>, SortedSet<Link>>>> thisLayerPropInfo = new TreeMap<>();
            if (vs.isShowInCanvasThisLayerPropagation())
            {
                for (Link link : allAffectedLinks)
                {
                    thisLayerPropInfo.put(link, link.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink());
                    final SortedSet<Link> linksPrimary = thisLayerPropInfo.get(link).getFirst().values().stream().flatMap(set -> set.stream()).collect(Collectors.toCollection(TreeSet::new));
                    final SortedSet<Link> linksBackup = thisLayerPropInfo.get(link).getSecond().values().stream().flatMap(set -> set.stream()).collect(Collectors.toCollection(TreeSet::new));
                    final SortedSet<Link> linksMulticast = thisLayerPropInfo.get(link).getThird().values().stream().flatMap(set -> set.stream()).collect(Collectors.toCollection(TreeSet::new));
                    final SortedSet<Link> links = new TreeSet<>();
                    if (linksPrimary != null) links.addAll(linksPrimary);
                    if (linksBackup != null) links.addAll(linksBackup);
                    if (linksMulticast != null) links.addAll(linksMulticast);
                    DrawUtils.drawCollateralLinks(vs, links, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
                }
            }
            if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
            {
                final SortedSet<Link> affectedCoupledLinks = allAffectedLinks.stream().filter(e -> e.isCoupled()).collect(Collectors.toCollection(TreeSet::new));
                final Pair<SortedSet<Demand>, SortedSet<Pair<MulticastDemand, Node>>> couplingInfo = DrawUtils.getDownCoupling(affectedCoupledLinks);
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(couplingInfo.getFirst(), null, couplingInfo.getSecond(), false);
                final SortedSet<Link> lowerLayerLinks = ipg.getLinksInGraph();
                DrawUtils.drawCollateralLinks(vs, lowerLayerLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, lowerLayerLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
            }
            if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
            {
                final SortedSet<Demand> demandsPrimaryAndBackup = new TreeSet<>();
                final SortedSet<Pair<MulticastDemand, Node>> demandsMulticast = new TreeSet<>();
                for (Link link : allAffectedLinks)
                {
                    final Triple<SortedMap<Demand, SortedSet<Link>>, SortedMap<Demand, SortedSet<Link>>, SortedMap<Pair<MulticastDemand, Node>, SortedSet<Link>>> thisLinkInfo =
                            vs.isShowInCanvasThisLayerPropagation() ? thisLayerPropInfo.get(link) : link.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink();
                    demandsPrimaryAndBackup.addAll(Sets.union(thisLinkInfo.getFirst().keySet(), thisLinkInfo.getSecond().keySet()));
                    demandsMulticast.addAll(thisLinkInfo.getThird().keySet());
                }
                final SortedSet<Link> coupledUpperLinks = DrawUtils.getUpCoupling(demandsPrimaryAndBackup, demandsMulticast);
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

    private void applyVisualizationPickMulticastDemand(List<MulticastDemand> pickedDemands)
    {
        for (MulticastDemand pickedDemand : pickedDemands)
        {
            final boolean isDemandLayerVisibleInTheCanvas = vs.isLayerVisibleInCanvas(pickedDemand.getLayer());
            final GUINode gnOrigin = vs.getCanvasAssociatedGUINode(pickedDemand.getIngressNode(), pickedDemand.getLayer());
            SortedSet<Link> linksThisLayer = null;
            for (Node egressNode : pickedDemand.getEgressNodes())
            {
                final GUINode gnDestination = vs.getCanvasAssociatedGUINode(egressNode, pickedDemand.getLayer());
                if (vs.isShowInCanvasThisLayerPropagation() && isDemandLayerVisibleInTheCanvas)
                {
                    linksThisLayer = pickedDemand.getLinksNoDownPropagationPotentiallyCarryingTraffic(egressNode);
                    DrawUtils.drawCollateralLinks(vs, linksThisLayer, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
                {
                    if (linksThisLayer == null)
                        linksThisLayer = pickedDemand.getLinksNoDownPropagationPotentiallyCarryingTraffic(egressNode);
                    final Pair<SortedSet<Demand>, SortedSet<Pair<MulticastDemand, Node>>> downLayerInfo = DrawUtils.getDownCoupling(linksThisLayer);
                    final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downLayerInfo.getFirst(), null, downLayerInfo.getSecond(), false);
                    final SortedSet<Link> linksLowerLayers = ipg.getLinksInGraph();
                    DrawUtils.drawCollateralLinks(vs, linksLowerLayers, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                    DrawUtils.drawDownPropagationInterLayerLinks(vs, linksLowerLayers, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedDemand.isCoupled())
                {
                    final SortedSet<Link> upCoupledLink = DrawUtils.getUpCoupling(null, Collections.singleton(Pair.of(pickedDemand, egressNode)));
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

    private void applyVisualizationPickRoute(List<Route> pickedRoutes)
    {
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
                final Pair<SortedSet<Demand>, SortedSet<Pair<MulticastDemand, Node>>> downInfo = DrawUtils.getDownCoupling(pickedRoute.getSeqLinks());
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downInfo.getFirst(), null, downInfo.getSecond(), false);
                DrawUtils.drawCollateralLinks(vs, ipg.getLinksInGraph(), pickedRoute.isBackupRoute() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, ipg.getLinksInGraph(), pickedRoute.isBackupRoute() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedRoute.getDemand().isCoupled())
            {
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, new TreeSet<> (Arrays.asList(pickedRoute.getDemand().getCoupledLink())), null, true);
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

    private void applyVisualizationPickMulticastTree(List<MulticastTree> pickedTrees)
    {
        for (MulticastTree pickedTree : pickedTrees)
        {
            final boolean isTreeLayerVisibleInTheCanvas = vs.isLayerVisibleInCanvas(pickedTree.getLayer());
            final GUINode gnOrigin = vs.getCanvasAssociatedGUINode(pickedTree.getIngressNode(), pickedTree.getLayer());
            for (Node egressNode : pickedTree.getEgressNodesReached())
            {
                final GUINode gnDestination = vs.getCanvasAssociatedGUINode(egressNode, pickedTree.getLayer());
                if (vs.isShowInCanvasThisLayerPropagation() && isTreeLayerVisibleInTheCanvas)
                {
                    final List<Link> linksPrimary = pickedTree.getSeqLinksToEgressNode(egressNode);
                    DrawUtils.drawCollateralLinks(vs, linksPrimary, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
                {
                    final Pair<SortedSet<Demand>, SortedSet<Pair<MulticastDemand, Node>>> downInfo = DrawUtils.getDownCoupling(pickedTree.getSeqLinksToEgressNode(egressNode));
                    final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downInfo.getFirst(), null, downInfo.getSecond(), false);
                    DrawUtils.drawCollateralLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                    DrawUtils.drawDownPropagationInterLayerLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedTree.getMulticastDemand().isCoupled())
                {
                    final SortedSet<Link> upperCoupledLink = DrawUtils.getUpCoupling(null, Arrays.asList(Pair.of(pickedTree.getMulticastDemand(), egressNode)));
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

    private void applyVisualizationPickLink(List<Link> pickedLinks)
    {
        for (Link pickedLink : pickedLinks)
        {
            final boolean isLinkLayerVisibleInTheCanvas = vs.isLayerVisibleInCanvas(pickedLink.getLayer());
            Triple<SortedMap<Demand, SortedSet<Link>>, SortedMap<Demand, SortedSet<Link>>, SortedMap<Pair<MulticastDemand, Node>, SortedSet<Link>>> thisLayerTraversalInfo = null;
            if (vs.isShowInCanvasThisLayerPropagation() && isLinkLayerVisibleInTheCanvas)
            {
                thisLayerTraversalInfo = pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink();
                final SortedSet<Link> linksPrimary = thisLayerTraversalInfo.getFirst().values().stream().flatMap(set -> set.stream()).collect(Collectors.toCollection(TreeSet::new));
                final SortedSet<Link> linksBackup = thisLayerTraversalInfo.getSecond().values().stream().flatMap(set -> set.stream()).collect(Collectors.toCollection(TreeSet::new));
                final SortedSet<Link> linksMulticast = thisLayerTraversalInfo.getThird().values().stream().flatMap(set -> set.stream()).collect(Collectors.toCollection(TreeSet::new));
                DrawUtils.drawCollateralLinks(vs, Sets.union(Sets.union(linksPrimary, linksBackup), linksMulticast), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedLink.isCoupled())
            {
                final Pair<SortedSet<Demand>, SortedSet<Pair<MulticastDemand, Node>>> downLayerInfo = DrawUtils.getDownCoupling(Arrays.asList(pickedLink));
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downLayerInfo.getFirst(), null, downLayerInfo.getSecond(), false);
                DrawUtils.drawCollateralLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1))
            {
                if (thisLayerTraversalInfo == null)
                    thisLayerTraversalInfo = pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink();
                final SortedSet<Demand> demandsPrimaryAndBackup = new TreeSet<> (Sets.union(thisLayerTraversalInfo.getFirst().keySet(), thisLayerTraversalInfo.getSecond().keySet()));
                final SortedSet<Pair<MulticastDemand, Node>> mDemands = new TreeSet<> (thisLayerTraversalInfo.getThird().keySet());
                final SortedSet<Link> initialUpperLinks = DrawUtils.getUpCoupling(demandsPrimaryAndBackup, mDemands);
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, initialUpperLinks, null, true);
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

    private void applyVisualizationPickNode(List<Node> pickedNodes , NetworkLayer layer)
    {
    	if (layer == null) layer = callback.getDesign().getNetworkLayerDefault();
        for (Node pickedNode : pickedNodes)
        {
            for (GUINode gn : vs.getCanvasVerticallyStackedGUINodes(pickedNode))
            {
            	if (!gn.getLayer().equals(layer)) continue;
                gn.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_PICK);
                gn.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_PICK);
            }
            for (Link e : Sets.union(pickedNode.getOutgoingLinks(layer), pickedNode.getIncomingLinks(layer)))
            {
                final GUILink gl = vs.getCanvasAssociatedGUILink(e);
                gl.setShownSeparated(true);
                gl.setHasArrow(true);
            }
        }
    }

    private void applyVisualizationPickResource(List<Resource> pickedResources , NetworkLayer layer)
    {
    	if (layer == null) layer = callback.getDesign().getNetworkLayerDefault();
        for (Resource pickedResource : pickedResources)
        {
        	if (pickedResource.iAttachedToANode())
	            for (GUINode gn : vs.getCanvasVerticallyStackedGUINodes(pickedResource.getHostNode().get()))
	            {
	            	if (!gn.getLayer().equals(layer)) continue;
	                gn.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_RESOURCE);
	                gn.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_RESOURCE);
	            }
        }
    }

    private void applyVisualizationPickForwardingRule(List<Pair<Demand, Link>> pickedFRs)
    {
        for (Pair<Demand, Link> pickedFR : pickedFRs)
        {
            final boolean isFRLayerVisibleInTheCanvas = vs.isLayerVisibleInCanvas(pickedFR.getFirst().getLayer());
            final Demand pickedDemand = pickedFR.getFirst();
            final Link pickedLink = pickedFR.getSecond();
            if (vs.isShowInCanvasThisLayerPropagation() && isFRLayerVisibleInTheCanvas)
            {
                final Triple<SortedMap<Demand, SortedSet<Link>>, SortedMap<Demand, SortedSet<Link>>, SortedMap<Pair<MulticastDemand, Node>, SortedSet<Link>>> triple =
                        pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink();
                final SortedSet<Link> linksPrimary = triple.getFirst().get(pickedDemand);
                final SortedSet<Link> linksBackup = triple.getSecond().get(pickedDemand);
                DrawUtils.drawCollateralLinks(vs, Sets.union(linksPrimary == null ? new TreeSet<>() : linksPrimary, linksBackup == null ? new TreeSet<>() : linksBackup
                ), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasLowerLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedLink.isCoupled())
            {
                final Pair<SortedSet<Demand>, SortedSet<Pair<MulticastDemand, Node>>> downLayerInfo = DrawUtils.getDownCoupling(Arrays.asList(pickedLink));
                final InterLayerPropagationGraph ipgCausedByLink = new InterLayerPropagationGraph(downLayerInfo.getFirst(), null, downLayerInfo.getSecond(), false);
                final SortedSet<Link> frPropagationLinks = ipgCausedByLink.getLinksInGraph();
                DrawUtils.drawCollateralLinks(vs, frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                DrawUtils.drawDownPropagationInterLayerLinks(vs, frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (vs.isShowInCanvasUpperLayerPropagation() && (vs.getNetPlan().getNumberOfLayers() > 1) && pickedDemand.isCoupled())
            {
                final InterLayerPropagationGraph ipgCausedByDemand = new InterLayerPropagationGraph(null, new TreeSet<> (Arrays.asList(pickedDemand.getCoupledLink())), null, true);
                final SortedSet<Link> frPropagationLinks = ipgCausedByDemand.getLinksInGraph();
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

    private void cleanPick()
    {
    	final VisualizationState vs = callback.getVisualizationState();
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

        static Pair<SortedSet<Demand>, SortedSet<Pair<MulticastDemand, Node>>> getDownCoupling(Collection<Link> links)
        {
            final SortedSet<Demand> res_1 = new TreeSet<>();
            final SortedSet<Pair<MulticastDemand, Node>> res_2 = new TreeSet<>();
            for (Link link : links)
            {
                if (link.getCoupledDemand() != null) res_1.add(link.getCoupledDemand());
                else if (link.getCoupledMulticastDemand() != null)
                    res_2.add(Pair.of(link.getCoupledMulticastDemand(), link.getDestinationNode()));
            }
            return Pair.of(res_1, res_2);

        }

        static SortedSet<Link> getUpCoupling(Collection<Demand> demands, Collection<Pair<MulticastDemand, Node>> mDemands)
        {
            final SortedSet<Link> res = new TreeSet<>();
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

        static void drawDownPropagationInterLayerLinks(VisualizationState vs, SortedSet<Link> links, Paint color)
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

    public void pickElements (PickStateInfo pickState)
    {
    	final PickStateInfo stateThisNp = pickState.getTranslationToGivenNetPlan(callback.getDesign ()).orElse(null);
    	if (stateThisNp == null) return;
    	this.addElement(stateThisNp);
    	stateThisNp.applyVisualizationInCurrentDesign();
    }
    public void pickElements (NetworkElement e)
    {
    	final PickStateInfo ps = new PickStateInfo(e, Optional.empty());
    	pickElements(ps);
    }
    public void pickElements (NetworkElement e , NetworkLayer layer)
    {
    	final PickStateInfo ps = new PickStateInfo(e, layer == null? Optional.empty() : Optional.of(layer));
    	pickElements(ps);
    }
    public void pickElements (Pair<Demand,Link> e)
    {
    	final PickStateInfo ps = new PickStateInfo(e, Optional.empty());
    	pickElements(ps);
    }

}
