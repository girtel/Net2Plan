package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters;

import java.util.ArrayList;
import java.util.List;

import com.net2plan.gui.plugins.networkDesign.ElementSelection;
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
import com.net2plan.internal.Constants.NetworkElementType;

public class TBFSelectionBased extends ITableRowFilter
{
    private final ElementSelection elementSelection;

    public TBFSelectionBased(NetPlan netPlan, ElementSelection elementSelection , boolean invertSelection)
    {
        super(netPlan);
        this.elementSelection = elementSelection;
        final NetworkLayer selectionLayer = elementSelection.getLayer();
        final NetworkElementType selectionType = elementSelection.getElementType();

        compute here the inverted selection if needed, so from here you operate with list of nbetwork elements / FRs
        
		/* update the rest according to this */
        for (NetworkLayer layer : netPlan.getNetworkLayers())
        {
        	final boolean isSourceRouting = layer.isSourceRouting();
            /* keep this layer unchanged */
        	if (selectionLayer == layer && selectionType == NetworkElementType.DEMAND)
        		vDemands.put(layer, new ArrayList<>((List<Demand>) elementSelection.getNetworkElements()));
        	else
        		vDemands.put(layer, netPlan.getDemands(layer));
            if (!isSourceRouting) 
            {
            	if (selectionLayer == layer && selectionType == NetworkElementType.FORWARDING_RULE)
                	vFRs.put(elementSelection.getLayer(), new ArrayList<>(elementSelection.getForwardingRules()));
            	else
            		vFRs.put(layer, new ArrayList<>(netPlan.getForwardingRules(layer).keySet()));
            }
            if (isSourceRouting)
            {
            	if (selectionLayer == layer && selectionType == NetworkElementType.ROUTE)
    				vRoutes.put(elementSelection.getLayer(), new ArrayList<>((List<Route>) elementSelection.getNetworkElements()));
            	else
                	vRoutes.put(layer, netPlan.getRoutes(layer));
            }
        	if (selectionLayer == layer && selectionType == NetworkElementType.LINK)
				vLinks.put(elementSelection.getLayer(), new ArrayList<>((List<Link>) elementSelection.getNetworkElements()));
        	else
                vLinks.put(layer, netPlan.getLinks(layer));
        	if (selectionLayer == layer && selectionType == NetworkElementType.MULTICAST_DEMAND)
				vMDemands.put(elementSelection.getLayer(), new ArrayList<>((List<MulticastDemand>) elementSelection.getNetworkElements()));
        	else
            	vMDemands.put(layer, netPlan.getMulticastDemands(layer));
        	if (selectionLayer == layer && selectionType == NetworkElementType.MULTICAST_TREE)
				vTrees.put(elementSelection.getLayer(), new ArrayList<>((List<MulticastTree>) elementSelection.getNetworkElements()));
        	else
        		vTrees.put(layer, netPlan.getMulticastTrees(layer));
        	if (selectionType == NetworkElementType.NODE)
	        	vNodes.put(layer, new ArrayList<>((List<Node>) elementSelection.getNetworkElements()));
        	else
                vNodes.put(layer, netPlan.getNodes());
        	if (selectionType == NetworkElementType.RESOURCE)
	        	vResources.put(layer, new ArrayList<>((List<Resource>) elementSelection.getNetworkElements()));
        	else
                vResources.put(layer, netPlan.getResources());
        	if (selectionType == NetworkElementType.SRG)
	        	vSRGs.put(layer, new ArrayList<>((List<SharedRiskGroup>) elementSelection.getNetworkElements()));
        	else
        		vSRGs.put(layer, netPlan.getSRGs());
        }
        
    }


    public String getDescription()
    {
        if (!chainOfDescriptionsPreviousFiltersComposingThis.isEmpty())
            return "Multiple chained filters (" + (chainOfDescriptionsPreviousFiltersComposingThis.size() + 1) + ")";
        return "Selection-based filter";
    }
}
