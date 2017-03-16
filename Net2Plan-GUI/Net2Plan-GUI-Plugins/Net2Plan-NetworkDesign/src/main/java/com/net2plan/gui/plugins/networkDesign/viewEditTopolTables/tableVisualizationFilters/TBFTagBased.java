package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters;

import java.util.ArrayList;
import java.util.List;

import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;

public class TBFTagBased extends ITableRowFilter
{
	private final NetworkLayer restrictToThisLayer;
	private final String tagName;
	private final NetPlan netPlan;
	
	public TBFTagBased (NetPlan netPlan , NetworkLayer restrictToThisLayer , String tagName)
	{
		super (netPlan);
		
		this.restrictToThisLayer = restrictToThisLayer;
		this.tagName = tagName;
		this.netPlan = netPlan;
		if (restrictToThisLayer != null)
			if (restrictToThisLayer.getNetPlan() != this.netPlan) throw new RuntimeException ();
		
		/* update the rest according to this */
		for (NetworkLayer layer : netPlan.getNetworkLayers())
		{
			final boolean isSourceRouting = layer.isSourceRouting();
			if ((restrictToThisLayer!= null) && (restrictToThisLayer != layer))
			{
				/* keep this layer unchanged */
				vDemands.put(layer , netPlan.getDemands(layer));
				if (!isSourceRouting) vFRs.put(layer , new ArrayList<> (netPlan.getForwardingRules(layer).keySet()));
				if (isSourceRouting) vRoutes.put(layer , netPlan.getRoutes(layer));
				vLinks.put(layer ,netPlan.getLinks(layer));
				vMDemands.put(layer , netPlan.getMulticastDemands(layer));
				vTrees.put(layer , netPlan.getMulticastTrees(layer));
				vNodes.put(layer , netPlan.getNodes());
				vResources.put(layer , netPlan.getResources());
				vSRGs.put(layer , netPlan.getSRGs());
				continue;
			}
				
			/* Here if we filter out by this layer */
			vDemands.put(layer , (List<Demand>) orderSet (netPlan.getTaggedDemands(tagName , layer)));
			if (!isSourceRouting) vFRs.put(layer , new ArrayList<> (netPlan.getForwardingRules(layer).keySet()));
			if (isSourceRouting) vRoutes.put(layer , (List<Route>) orderSet(netPlan.getTaggedRoutes(tagName , layer)));
			vLinks.put(layer , (List<Link>) orderSet(netPlan.getTaggedLinks(tagName , layer)));
			vMDemands.put(layer , (List<MulticastDemand>) orderSet(netPlan.getTaggedMulticastDemands(tagName , layer)));
			vTrees.put(layer , (List<MulticastTree>) orderSet(netPlan.getTaggedMulticastTrees(tagName , layer)));
			vNodes.put(layer , (List<Node>) orderSet(netPlan.getTaggedNodes(tagName)));
			vResources.put(layer , (List<Resource>) orderSet(netPlan.getTaggedResources(tagName)));
			vSRGs.put(layer , (List<SharedRiskGroup>) orderSet(netPlan.getTaggedSrgs(tagName)));	
		}
	}

    
	public String getDescription () 
	{ 
		if (!chainOfDescriptionsPreviousFiltersComposingThis.isEmpty()) return "Multiple chained filters (" + (chainOfDescriptionsPreviousFiltersComposingThis.size() + 1) + ")";
		return ((restrictToThisLayer!=null)? "(Affecting to layer " + getLayerName(restrictToThisLayer) + ") ": "") + "Tag: " + this.tagName;
	} 
	private static String getLayerName (NetworkLayer layer) { return layer.getName().equals("")? "Layer " + layer.getIndex() : layer.getName(); } 
}
