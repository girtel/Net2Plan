package com.net2plan.gui.utils.viewEditTopolTables.tableVisualizationFilters;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.net2plan.gui.utils.viewEditTopolTables.ITableRowFilter;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.InterLayerPropagationGraph;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

public class TBFToFromCarriedTraffic extends ITableRowFilter
{
	private final boolean onlyThisLayer;
	private final NetworkElement initialElement;
	private final Pair<Demand,Link> initialFR;

	public TBFToFromCarriedTraffic (Demand demand , boolean onlyThisLayer)
	{
		super (demand.getNetPlan());
		
		this.initialElement = demand;
		this.onlyThisLayer = onlyThisLayer;
		this.initialFR = null;

		final Set<Link> linksAllLayers = new HashSet<> ();
		final Set<Demand> demandsAllLayers = new HashSet<> ();
		final Set<MulticastDemand> mDemandsAllLayers = new HashSet<> ();

		demandsAllLayers.add(demand);
		
		final Pair<Set<Link>,Set<Link>> thisLayerPropagation = demand.getLinksThisLayerPotentiallyCarryingTraffic(false);
		linksAllLayers.addAll(thisLayerPropagation.getFirst());
		linksAllLayers.addAll(thisLayerPropagation.getSecond());
		if (!onlyThisLayer) updatePropagationDownWards (linksAllLayers , linksAllLayers , demandsAllLayers , mDemandsAllLayers);
		if (!onlyThisLayer) updatePropagationUpWards (Arrays.asList(demand) , null , linksAllLayers , demandsAllLayers , mDemandsAllLayers);
		final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer? Sets.difference(new HashSet<>(netPlan.getNetworkLayers ()), Sets.newHashSet(demand.getLayer())): new HashSet<> ();
		updateAllButLinksDemandsMDemandsUsingExistingInfo (linksAllLayers , demandsAllLayers , mDemandsAllLayers , layersToKeepAllElements);
	}

	public TBFToFromCarriedTraffic (Link link , boolean onlyThisLayer)
	{
		super (link.getNetPlan());
		
		this.initialElement = link;
		this.onlyThisLayer = onlyThisLayer;
		this.initialFR = null;

		final Set<Link> linksAllLayers = new HashSet<> ();
		final Set<Demand> demandsAllLayers = new HashSet<> ();
		final Set<MulticastDemand> mDemandsAllLayers = new HashSet<> ();

		linksAllLayers.add(link);
		
		Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> thisLayerTraversalInfo = 
				link.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false);
		linksAllLayers.addAll(thisLayerTraversalInfo.getFirst().values().stream().flatMap(set->set.stream()).collect (Collectors.toSet()));
		linksAllLayers.addAll(thisLayerTraversalInfo.getSecond().values().stream().flatMap(set->set.stream()).collect (Collectors.toSet()));
		linksAllLayers.addAll(thisLayerTraversalInfo.getThird().values().stream().flatMap(set->set.stream()).collect (Collectors.toSet()));
		demandsAllLayers.addAll(thisLayerTraversalInfo.getFirst().keySet());
		demandsAllLayers.addAll(thisLayerTraversalInfo.getSecond().keySet());
		mDemandsAllLayers.addAll(thisLayerTraversalInfo.getThird().keySet().stream().map(p->p.getFirst()).collect(Collectors.toSet()));
		if (!onlyThisLayer) updatePropagationDownWards (Arrays.asList(link) , linksAllLayers , demandsAllLayers , mDemandsAllLayers);
		if (!onlyThisLayer) updatePropagationUpWards (demandsAllLayers , mDemandsAllLayers , linksAllLayers , demandsAllLayers , mDemandsAllLayers);
		final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer? Sets.difference(new HashSet<>(netPlan.getNetworkLayers ()), Sets.newHashSet(link.getLayer())): new HashSet<> ();
		updateAllButLinksDemandsMDemandsUsingExistingInfo (linksAllLayers , demandsAllLayers , mDemandsAllLayers , layersToKeepAllElements);
	}

	public TBFToFromCarriedTraffic (MulticastTree tree , boolean onlyThisLayer)
	{
		super (tree.getNetPlan());
		
		this.initialElement = tree;
		this.onlyThisLayer = onlyThisLayer;
		this.initialFR = null;

		final Set<Link> linksAllLayers = new HashSet<> ();
		final Set<Demand> demandsAllLayers = new HashSet<> ();
		final Set<MulticastDemand> mDemandsAllLayers = new HashSet<> ();

		mDemandsAllLayers.add(tree.getMulticastDemand());
		linksAllLayers.addAll(tree.getLinkSet());
		if (!onlyThisLayer) updatePropagationDownWards (linksAllLayers , linksAllLayers , demandsAllLayers , mDemandsAllLayers);
		if (!onlyThisLayer) updatePropagationUpWards (demandsAllLayers , mDemandsAllLayers , linksAllLayers , demandsAllLayers , mDemandsAllLayers);
		final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer? Sets.difference(new HashSet<>(netPlan.getNetworkLayers ()), Sets.newHashSet(tree.getLayer())): new HashSet<> ();
		updateAllButLinksDemandsMDemandsUsingExistingInfo (linksAllLayers , demandsAllLayers , mDemandsAllLayers , layersToKeepAllElements);
	}

	public TBFToFromCarriedTraffic (Route route , boolean onlyThisLayer)
	{
		super (route.getNetPlan());
		
		this.initialElement = route;
		this.onlyThisLayer = onlyThisLayer;
		this.initialFR = null;

		final Set<Link> linksAllLayers = new HashSet<> ();
		final Set<Demand> demandsAllLayers = new HashSet<> ();
		final Set<MulticastDemand> mDemandsAllLayers = new HashSet<> ();

		demandsAllLayers.add(route.getDemand());
		linksAllLayers.addAll(route.getSeqLinks());

		if (!onlyThisLayer) updatePropagationDownWards (linksAllLayers , linksAllLayers , demandsAllLayers , mDemandsAllLayers);
		if (!onlyThisLayer) updatePropagationUpWards (demandsAllLayers , mDemandsAllLayers , linksAllLayers , demandsAllLayers , mDemandsAllLayers);
		final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer? Sets.difference(new HashSet<>(netPlan.getNetworkLayers ()), Sets.newHashSet(route.getLayer())): new HashSet<> ();
		updateAllButLinksDemandsMDemandsUsingExistingInfo (linksAllLayers , demandsAllLayers , mDemandsAllLayers , layersToKeepAllElements);
	}

	public TBFToFromCarriedTraffic (Resource resource , NetworkLayer layer , boolean onlyThisLayer)
	{
		super (resource.getNetPlan());
		
		this.initialElement = resource;
		this.onlyThisLayer = onlyThisLayer;
		this.initialFR = null;

		final Set<Link> linksAllLayers = new HashSet<> ();
		final Set<Demand> demandsAllLayers = new HashSet<> ();
		final Set<MulticastDemand> mDemandsAllLayers = new HashSet<> ();

		for (Route r : resource.getTraversingRoutes())
		{
			if (r.getLayer() != layer) continue;
			linksAllLayers.addAll(r.getSeqLinks());
			demandsAllLayers.add(r.getDemand());
		}
		if (!onlyThisLayer) updatePropagationDownWards (linksAllLayers , linksAllLayers , demandsAllLayers , mDemandsAllLayers);
		if (!onlyThisLayer) updatePropagationUpWards (demandsAllLayers , mDemandsAllLayers , linksAllLayers , demandsAllLayers , mDemandsAllLayers);
		final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer? Sets.difference(new HashSet<>(netPlan.getNetworkLayers ()), Sets.newHashSet(layer)): new HashSet<> ();
		updateAllButLinksDemandsMDemandsUsingExistingInfo (linksAllLayers , demandsAllLayers , mDemandsAllLayers , layersToKeepAllElements);
	}

	public TBFToFromCarriedTraffic (Node node , NetworkLayer layer , boolean onlyThisLayer)
	{
		super (node.getNetPlan());
		
		this.initialElement = node;
		this.onlyThisLayer = onlyThisLayer;
		this.initialFR = null;

		final Set<Link> linksAllLayers = new HashSet<> ();
		final Set<Demand> demandsAllLayers = new HashSet<> ();
		final Set<MulticastDemand> mDemandsAllLayers = new HashSet<> ();

		demandsAllLayers.addAll(node.getIncomingDemands(layer));
		demandsAllLayers.addAll(node.getOutgoingDemands(layer));
		linksAllLayers.addAll(node.getIncomingLinks(layer));
		linksAllLayers.addAll(node.getOutgoingLinks(layer));
		mDemandsAllLayers.addAll (node.getIncomingMulticastDemands(layer));
		mDemandsAllLayers.addAll (node.getOutgoingMulticastDemands(layer));

		if (!onlyThisLayer) updatePropagationDownWards (linksAllLayers , linksAllLayers , demandsAllLayers , mDemandsAllLayers);
		if (!onlyThisLayer) updatePropagationUpWards (demandsAllLayers , mDemandsAllLayers , linksAllLayers , demandsAllLayers , mDemandsAllLayers);
		final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer? Sets.difference(new HashSet<>(netPlan.getNetworkLayers ()), Sets.newHashSet(layer)): new HashSet<> ();
		updateAllButLinksDemandsMDemandsUsingExistingInfo (linksAllLayers , demandsAllLayers , mDemandsAllLayers , layersToKeepAllElements);
	}

	public TBFToFromCarriedTraffic (SharedRiskGroup srg)
	{
		super (srg.getNetPlan());
		
		this.initialElement = srg;
		this.onlyThisLayer = false;
		this.initialFR = null;

		final Set<Link> linksAllLayers = new HashSet<> ();
		final Set<Demand> demandsAllLayers = new HashSet<> ();
		final Set<MulticastDemand> mDemandsAllLayers = new HashSet<> ();

		for (Link affectedLink : srg.getAffectedLinksAllLayers())
		{
			final TBFToFromCarriedTraffic affectedThisLink = new TBFToFromCarriedTraffic(affectedLink , false);
			for (NetworkLayer layer : netPlan.getNetworkLayers())
			{
				linksAllLayers.addAll(affectedThisLink.getVisibleLinks(layer));
				demandsAllLayers.addAll(affectedThisLink.getVisibleDemands(layer));
				mDemandsAllLayers.addAll(affectedThisLink.getVisibleMulticastDemands(layer));
			}
		}
	}

	public TBFToFromCarriedTraffic (Pair<Demand,Link> fr , boolean onlyThisLayer)
	{
		super (fr.getFirst().getNetPlan());
		
		this.initialElement = null;
		this.onlyThisLayer = onlyThisLayer;
		this.initialFR = fr;
		
		final Demand frDemand = fr.getFirst();
		final Link frLink = fr.getSecond();
		
		final Set<Link> linksAllLayers = new HashSet<> ();
		final Set<Demand> demandsAllLayers = new HashSet<> ();
		final Set<MulticastDemand> mDemandsAllLayers = new HashSet<> ();

		demandsAllLayers.add(frDemand);
		linksAllLayers.add(frLink);

		if (!onlyThisLayer) updatePropagationDownWards (linksAllLayers , linksAllLayers , demandsAllLayers , mDemandsAllLayers);
		if (!onlyThisLayer) updatePropagationUpWards (demandsAllLayers , mDemandsAllLayers , linksAllLayers , demandsAllLayers , mDemandsAllLayers);

		final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer? Sets.difference(new HashSet<>(netPlan.getNetworkLayers ()), Sets.newHashSet(frLink.getLayer())): new HashSet<> ();
		updateAllButLinksDemandsMDemandsUsingExistingInfo (linksAllLayers , demandsAllLayers , mDemandsAllLayers , layersToKeepAllElements);
	}

	public TBFToFromCarriedTraffic (MulticastDemand demand , boolean onlyThisLayer)
	{
		super (demand.getNetPlan());
		
		this.initialElement = demand;
		this.onlyThisLayer = onlyThisLayer;
		this.initialFR = null;

		final Set<Link> linksAllLayers = new HashSet<> ();
		final Set<Demand> demandsAllLayers = new HashSet<> ();
		final Set<MulticastDemand> mDemandsAllLayers = new HashSet<> ();

		mDemandsAllLayers.add(demand);

		final Set<Link> thisLayerPropagation = new HashSet<> ();
		for (Node egressNode : demand.getEgressNodes())
			thisLayerPropagation.addAll(demand.getLinksThisLayerPotentiallyCarryingTraffic(egressNode , false));
		linksAllLayers.addAll(thisLayerPropagation);

		if (!onlyThisLayer) updatePropagationDownWards (linksAllLayers , linksAllLayers , demandsAllLayers , mDemandsAllLayers);
		if (!onlyThisLayer) updatePropagationUpWards (demandsAllLayers , mDemandsAllLayers , linksAllLayers , demandsAllLayers , mDemandsAllLayers);

		final Set<NetworkLayer> layersToKeepAllElements = onlyThisLayer? Sets.difference(new HashSet<>(netPlan.getNetworkLayers ()), Sets.newHashSet(demand.getLayer())): new HashSet<> ();
		updateAllButLinksDemandsMDemandsUsingExistingInfo (linksAllLayers , demandsAllLayers , mDemandsAllLayers , layersToKeepAllElements);
	}

    
	public String getDescription () 
	{ 
		final Demand d = (Demand) initialElement;
		StringBuffer st = new StringBuffer();
		st.append(onlyThisLayer? "(Affecting to layer " +  getLayerName(d.getLayer()) + ") " : "(Affecting to all layers). ");
		st.append("Elements associated to demand " + initialElement.getIndex() + " in layer " + getLayerName (((Demand) initialElement).getLayer()));
		return st.toString();
	} 


	private void updatePropagationDownWards (Collection<Link> linksToPropagateDown , Set<Link> linksAllLayersToUpdate , Set<Demand> demandsAllLayersToUpdate , 
			Set<MulticastDemand> mDemandsAllLayersToUpdate)
	{
		if (netPlan.getNumberOfLayers() > 1)
		{
			final Pair<Set<Demand>,Set<Pair<MulticastDemand,Node>>> downInfo = getDownCoupling (linksToPropagateDown);
			if (downInfo.getFirst().isEmpty() && downInfo.getSecond().isEmpty()) return;
			final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (downInfo.getFirst() , null , downInfo.getSecond() , false , false);
			linksAllLayersToUpdate.addAll(ipg.getLinksInGraph());
			demandsAllLayersToUpdate.addAll(ipg.getDemandsInGraph());
			mDemandsAllLayersToUpdate.addAll(ipg.getMulticastDemandFlowsInGraph().stream().map(p->p.getFirst()).collect(Collectors.toSet()));
		}
	}
	private void updatePropagationUpWards (Collection<Demand> demandsToPropagateUp , Collection<MulticastDemand> mDemandsToPropagateUp , 
			Set<Link> linksAllLayersToUpdate , Set<Demand> demandsAllLayersToUpdate , 
			Set<MulticastDemand> mDemandsAllLayersToUpdate)
	{
		if (netPlan.getNumberOfLayers() > 1)
		{
    		final Set<Pair<MulticastDemand,Node>> mDemandsAllEgressNodes = new HashSet<Pair<MulticastDemand,Node>> ();
    		if (mDemandsToPropagateUp != null)
    			for (MulticastDemand md : mDemandsToPropagateUp)
    				for (Node n : md.getEgressNodes())
    					mDemandsAllEgressNodes.add(Pair.of(md , n));
    		final Set<Link> initialUpperLinks = getUpCoupling(demandsToPropagateUp , mDemandsAllEgressNodes);
    		if (initialUpperLinks.isEmpty()) return;
    		final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (null , initialUpperLinks , null , true , false);
			linksAllLayersToUpdate.addAll(ipg.getLinksInGraph());
			demandsAllLayersToUpdate.addAll(ipg.getDemandsInGraph());
			mDemandsAllLayersToUpdate.addAll(ipg.getMulticastDemandFlowsInGraph().stream().map(p->p.getFirst()).collect(Collectors.toSet()));
		}
	}
	
	private static String getLayerName (NetworkLayer layer) { return layer.getName().equals("")? "Layer " + layer.getIndex() : layer.getName(); } 
}
