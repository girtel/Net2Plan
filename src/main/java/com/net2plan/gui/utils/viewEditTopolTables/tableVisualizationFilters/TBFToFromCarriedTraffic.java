package com.net2plan.gui.utils.viewEditTopolTables.tableVisualizationFilters;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;
import com.net2plan.gui.utils.viewEditTopolTables.ITableRowFilter;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.InterLayerPropagationGraph;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Pair;

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

		if (!onlyThisLayer && (netPlan.getNumberOfLayers() > 1))
		{
			final Pair<Set<Demand>,Set<Pair<MulticastDemand,Node>>> downLayerInfo = getDownCoupling(thisLayerPropagation.getFirst()); 
			final Pair<Set<Demand>,Set<Pair<MulticastDemand,Node>>> downLayerInfoBackup = getDownCoupling(thisLayerPropagation.getSecond()); 
			downLayerInfo.getFirst().addAll(downLayerInfoBackup.getFirst());
			downLayerInfo.getSecond().addAll(downLayerInfoBackup.getSecond());
			final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (downLayerInfo.getFirst() , null , downLayerInfo.getSecond() , false , false);
			final Set<Link> links = ipg.getLinksInGraph();
			final Set<Demand> demands = ipg.getDemandsInGraph();
			final Set<MulticastDemand> mDemands = ipg.getMulticastDemandFlowsInGraph().stream().map(e->e.getFirst()).collect(Collectors.toSet());
			linksAllLayers.addAll(links);
			demandsAllLayers.addAll(demands);
			mDemandsAllLayers.addAll(mDemands);
		}
		if (!onlyThisLayer && (netPlan.getNumberOfLayers() > 1) && demand.isCoupled())
		{
			final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (null , Sets.newHashSet(demand.getCoupledLink()) , null , true , false);
			final Set<Link> links = ipg.getLinksInGraph();
			final Set<Demand> demands = ipg.getDemandsInGraph();
			final Set<MulticastDemand> mDemands = ipg.getMulticastDemandFlowsInGraph().stream().map(e->e.getFirst()).collect(Collectors.toSet());
			linksAllLayers.addAll(links);
			demandsAllLayers.addAll(demands);
			mDemandsAllLayers.addAll(mDemands);
		}

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

	
	private static String getLayerName (NetworkLayer layer) { return layer.getName().equals("")? "Layer " + layer.getIndex() : layer.getName(); } 
}
