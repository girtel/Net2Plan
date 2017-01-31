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
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Pair;

public class TBFToFromCarriedTraffic extends ITableRowFilter
{
	private final boolean upWards , downWards , sameLayer;
	private final NetworkElement initialElement;
	private final Pair<Demand,Link> initialFR;
	
	public TBFToFromCarriedTraffic (Demand demand , boolean downWards , boolean upWards , boolean sameLayer)
	{
		super (demand.getNetPlan());
		
		this.initialElement = demand;
		this.upWards = upWards;
		this.downWards = downWards;
		this.sameLayer = sameLayer;
		this.initialFR = null;

		final Set<Link> linksAllLayers = new HashSet<> ();
		final Set<Demand> demandsAllLayers = new HashSet<> ();
		final Set<MulticastDemand> mDemandsAllLayers = new HashSet<> ();

		demandsAllLayers.add(demand);
		
		Pair<Set<Link>,Set<Link>> thisLayerPropagation = null;
		if (sameLayer)
		{
    		thisLayerPropagation = demand.getLinksThisLayerPotentiallyCarryingTraffic(false);
    		linksAllLayers.addAll(thisLayerPropagation.getFirst());
    		linksAllLayers.addAll(thisLayerPropagation.getSecond());
		}
		if (downWards && (netPlan.getNumberOfLayers() > 1))
		{
			if (thisLayerPropagation == null) thisLayerPropagation = demand.getLinksThisLayerPotentiallyCarryingTraffic(false);
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
		if (upWards && (netPlan.getNumberOfLayers() > 1) && demand.isCoupled())
		{
			final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (null , Sets.newHashSet(demand.getCoupledLink()) , null , true , false);
			final Set<Link> links = ipg.getLinksInGraph();
			final Set<Demand> demands = ipg.getDemandsInGraph();
			final Set<MulticastDemand> mDemands = ipg.getMulticastDemandFlowsInGraph().stream().map(e->e.getFirst()).collect(Collectors.toSet());
			linksAllLayers.addAll(links);
			demandsAllLayers.addAll(demands);
			mDemandsAllLayers.addAll(mDemands);
		}

		updateAllButLinksDemandsMDemandsUsingExistingInfo (linksAllLayers , demandsAllLayers , mDemandsAllLayers);
	}

    
	public String getDescription () 
	{ 
		StringBuffer st = new StringBuffer();
		st.append("Elements associated to demand " + initialElement.getIndex() + "(");
		List<String> messages = new LinkedList<> (); 
		if (sameLayer) messages.add("this layer");
		if (upWards) messages.add("from upper layers");
		if (downWards) messages.add("to lower layers");
		st.append(StringUtils.join(messages , ",") + ")");
		return st.toString();
	} 

}
