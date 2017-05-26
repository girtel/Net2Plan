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

import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.interfaces.networkDesign.*;



import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unchecked")
public class TBFTagBased extends ITableRowFilter
{
    private final NetworkLayer restrictToThisLayer;
    private final String tagContainsName, tagDoesNotContainName;

    public TBFTagBased( NetPlan netPlan,  NetworkLayer restrictToThisLayer, String tagContainsName, String tagDoesNotContainName)
    {
        super(netPlan);

        this.restrictToThisLayer = restrictToThisLayer;
        this.tagContainsName = tagContainsName;
        this.tagDoesNotContainName = tagDoesNotContainName;

        if (restrictToThisLayer != null)
            if (restrictToThisLayer.getNetPlan() != this.netPlan) throw new RuntimeException();

		/* update the rest according to this */
        for (NetworkLayer layer : netPlan.getNetworkLayers())
        {
            final boolean isSourceRouting = layer.isSourceRouting();
            if ((restrictToThisLayer != null) && (restrictToThisLayer != layer))
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
            vDemands.put(layer, (List<Demand>) filteredView(netPlan.getDemands(layer)));
            if (!isSourceRouting) vFRs.put(layer, new ArrayList<>(netPlan.getForwardingRules(layer).keySet()));
            if (isSourceRouting) vRoutes.put(layer, (List<Route>) filteredView(netPlan.getRoutes(layer)));
            vLinks.put(layer, (List<Link>) filteredView(netPlan.getLinks(layer)));
            vMDemands.put(layer, (List<MulticastDemand>) filteredView(netPlan.getMulticastDemands(layer)));
            vTrees.put(layer, (List<MulticastTree>) filteredView(netPlan.getMulticastTrees(layer)));
            vNodes.put(layer, (List<Node>) filteredView(netPlan.getNodes()));
            vResources.put(layer, (List<Resource>) filteredView(netPlan.getResources()));
            vSRGs.put(layer, (List<SharedRiskGroup>) filteredView(netPlan.getSRGs()));
        }
    }


    public String getDescription()
    {
        if (!chainOfDescriptionsPreviousFiltersComposingThis.isEmpty())
            return "Multiple chained filters (" + (chainOfDescriptionsPreviousFiltersComposingThis.size() + 1) + ")";
        String msg = "";
        if (!this.tagContainsName.isEmpty())
        {
            msg += "Has tag that contains: " + this.tagContainsName;
            if (!this.tagDoesNotContainName.isEmpty())
                msg += " AND does NOT have tag containing " + this.tagDoesNotContainName;
        } else if (!this.tagDoesNotContainName.isEmpty())
            msg += "Does NOT have tag containing: " + this.tagDoesNotContainName;
        return ((restrictToThisLayer != null) ? "(Affecting to layer " + getLayerName(restrictToThisLayer) + ") " : "") + msg;
    }

    private static String getLayerName(NetworkLayer layer)
    {
        return layer.getName().equals("") ? "Layer " + layer.getIndex() : layer.getName();
    }

    private List<? extends NetworkElement> filteredView(List<? extends NetworkElement> list)
    {
        List<NetworkElement> res = new ArrayList<>(list.size());
        for (NetworkElement e : list)
        {
            final Set<String> tags = e.getTags();

            boolean containsOk = this.tagContainsName.isEmpty() || tags.contains(tagContainsName);
            boolean doesNotContainOk = this.tagDoesNotContainName.isEmpty() || !tags.contains(tagDoesNotContainName);
            if (containsOk && doesNotContainOk) res.add(e);
        }
        return res;
    }

}
