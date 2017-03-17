package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.interfaces.networkDesign.Demand;
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

public class TBFTagBased extends ITableRowFilter
{
	private final NetworkLayer restrictToThisLayer;
	private final String tagContainsName , tagDoesNotContainName;
	private final NetPlan netPlan;
	
	public TBFTagBased (NetPlan netPlan , NetworkLayer restrictToThisLayer , String tagContainsName , String tagDoesNotContainName)
	{
		super (netPlan);
		
		this.restrictToThisLayer = restrictToThisLayer;
		this.tagContainsName = tagContainsName;
		this.tagDoesNotContainName = tagDoesNotContainName;
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
			vDemands.put(layer , (List<Demand>) filteredView(netPlan.getDemands(layer)));
			if (!isSourceRouting) vFRs.put(layer , new ArrayList<> (netPlan.getForwardingRules(layer).keySet()));
			if (isSourceRouting) vRoutes.put(layer , (List<Route>) filteredView(netPlan.getRoutes(layer)));
			vLinks.put(layer , (List<Link>) filteredView(netPlan.getLinks(layer)));
			vMDemands.put(layer , (List<MulticastDemand>) filteredView(netPlan.getMulticastDemands(layer)));
			vTrees.put(layer , (List<MulticastTree>) filteredView(netPlan.getMulticastTrees(layer)));
			vNodes.put(layer , (List<Node>) filteredView(netPlan.getNodes()));
			vResources.put(layer , (List<Resource>) filteredView(netPlan.getResources()));
			vSRGs.put(layer , (List<SharedRiskGroup>) filteredView(netPlan.getSRGs()));	
		}
	}

    
	public String getDescription () 
	{ 
		if (!chainOfDescriptionsPreviousFiltersComposingThis.isEmpty()) return "Multiple chained filters (" + (chainOfDescriptionsPreviousFiltersComposingThis.size() + 1) + ")";
		String msg = "";
		if (!this.tagContainsName.isEmpty())
		{
			msg +=  "Has tag that contains: " + this.tagContainsName;
			if (!this.tagDoesNotContainName.isEmpty()) msg += " AND has tag that does NOT contain " + this.tagDoesNotContainName;
		}
		else
			if (!this.tagDoesNotContainName.isEmpty())
				msg +=  "Has tag that does NOT contain: " + this.tagDoesNotContainName;
		return ((restrictToThisLayer!=null)? "(Affecting to layer " + getLayerName(restrictToThisLayer) + ") ": "") + msg;
	} 
	private static String getLayerName (NetworkLayer layer) { return layer.getName().equals("")? "Layer " + layer.getIndex() : layer.getName(); } 
	
	private List<? extends NetworkElement> filteredView (List<? extends NetworkElement> list)
	{
		List<NetworkElement> res = new ArrayList<> (list.size());
		for (NetworkElement e : list)
		{
			boolean containsOk = this.tagContainsName.isEmpty();
			boolean doesNotCountainOk = this.tagDoesNotContainName.isEmpty();
			for (String tag : e.getTags())
			{
				if (!this.tagContainsName.isEmpty())
					if (tag.contains(tagContainsName)) containsOk = true; 
				if (!this.tagDoesNotContainName.isEmpty())
					if (!tag.contains(tagDoesNotContainName)) doesNotCountainOk = true;
				if (containsOk && doesNotCountainOk) break;
			}
			if (containsOk && doesNotCountainOk) res.add(e);
		}
		return res;
	}

}
