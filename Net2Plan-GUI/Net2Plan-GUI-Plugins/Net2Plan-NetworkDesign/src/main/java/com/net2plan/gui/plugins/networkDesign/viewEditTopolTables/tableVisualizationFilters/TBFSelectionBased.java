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

import com.net2plan.gui.plugins.networkDesign.ElementSelection;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class TBFSelectionBased extends ITableRowFilter
{
    public TBFSelectionBased( NetPlan netPlan,  ElementSelection elementSelection)
    {
        super(netPlan);

        // Selections can only be done at the active layer.
        final NetworkLayer layer = netPlan.getNetworkLayerDefault();

        vDemands.put(layer, netPlan.getDemands(layer));
        vLinks.put(layer, netPlan.getLinks(layer));
        vMDemands.put(layer, netPlan.getMulticastDemands(layer));
        vTrees.put(layer, netPlan.getMulticastTrees(layer));
        vNodes.put(layer, netPlan.getNodes());
        vResources.put(layer, netPlan.getResources());
        vSRGs.put(layer, netPlan.getSRGs());

        if (layer.isSourceRouting())
            vRoutes.put(layer, netPlan.getRoutes(layer));
        else
            vFRs.put(layer, new ArrayList<>(netPlan.getForwardingRules(layer).keySet()));

        if (elementSelection.isEmpty()) return;

        final NetworkElementType selectionType = elementSelection.getElementType();

        switch (selectionType)
        {
            case NODE:
                vNodes.put(layer, new ArrayList<>((List<Node>) elementSelection.getNetworkElements()));
                break;
            case LINK:
                vLinks.put(layer, new ArrayList<>((List<Link>) elementSelection.getNetworkElements()));
                break;
            case DEMAND:
                vDemands.put(layer, new ArrayList<>((List<Demand>) elementSelection.getNetworkElements()));
                break;
            case MULTICAST_DEMAND:
                vMDemands.put(layer, new ArrayList<>((List<MulticastDemand>) elementSelection.getNetworkElements()));
                break;
            case ROUTE:
                if (layer.isSourceRouting()) vRoutes.put(layer, new ArrayList<>((List<Route>) elementSelection.getNetworkElements()));
                break;
            case MULTICAST_TREE:
                vTrees.put(layer, new ArrayList<>((List<MulticastTree>) elementSelection.getNetworkElements()));
                break;
            case FORWARDING_RULE:
                if (!layer.isSourceRouting()) vFRs.put(layer, new ArrayList<>(elementSelection.getForwardingRules()));
                break;
            case RESOURCE:
                vResources.put(layer, new ArrayList<>((List<Resource>) elementSelection.getNetworkElements()));
                break;
            case SRG:
                vSRGs.put(layer, new ArrayList<>((List<SharedRiskGroup>) elementSelection.getNetworkElements()));
                break;
            default:
                break;
        }
    }

    public String getDescription()
    {
        if (!chainOfDescriptionsPreviousFiltersComposingThis.isEmpty())
            return "Multiple chained filters (" + (chainOfDescriptionsPreviousFiltersComposingThis.size() + 1) + ")";
        return "Selection-based filter";
    }
}
