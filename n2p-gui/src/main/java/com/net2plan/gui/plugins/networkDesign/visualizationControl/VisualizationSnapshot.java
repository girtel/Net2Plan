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

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.utils.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Jorge San Emeterio on 8/03/17.
 */
public class VisualizationSnapshot
{
    private NetPlan netPlan;

    private List<GUILayer> layerList;
    private Map<NetworkLayer, GUILayer> aux_layerToGUI;

    public VisualizationSnapshot(NetPlan netPlan)
    {
        this.layerList = new ArrayList<>();
        this.aux_layerToGUI = new HashMap<>();
        this.resetSnapshot(netPlan);
    }

    private void resetSnapshot()
    {
        // Building GUILayers
        final List<NetworkLayer> networkLayers = netPlan.getNetworkLayers();

        layerList.clear();
        aux_layerToGUI.clear();
        for (NetworkLayer networkLayer : networkLayers)
        {
            GUILayer guiLayer = new GUILayer(networkLayer);
            layerList.add(guiLayer);
            aux_layerToGUI.put(networkLayer, guiLayer);
        }
    }

    public NetPlan getNetPlan()
    {
        return netPlan;
    }

    public void resetSnapshot(NetPlan netPlan)
    {
        this.netPlan = netPlan;
        resetSnapshot();
    }

    public Map<NetworkLayer, Integer> getMapCanvasLayerVisualizationOrder()
    {
        return layerList.stream().collect(Collectors.toMap(GUILayer::getAssociatedNetworkLayer, GUILayer::getLayerOrder));
    }

    public void setLayerVisualizationOrder(NetworkLayer layer, int order)
    {
        if (!aux_layerToGUI.containsKey(layer))
            throw new RuntimeException("Layer does not belong to current NetPlan...");
        aux_layerToGUI.get(layer).setLayerOrder(order);
    }

    public int getCanvasLayerVisualizationOrder(NetworkLayer layer)
    {
        return aux_layerToGUI.get(layer).getLayerOrder();
    }

    public Map<NetworkLayer, Boolean> getMapCanvasLayerVisibility()
    {
        return layerList.stream().collect(Collectors.toMap(GUILayer::getAssociatedNetworkLayer, GUILayer::isLayerVisible));
    }

    public void addLayerVisibility(NetworkLayer layer, boolean visibility)
    {
        if (!aux_layerToGUI.containsKey(layer))
            throw new RuntimeException("Layer does not belong to current NetPlan...");
        aux_layerToGUI.get(layer).setLayerVisibility(visibility);
    }

    public boolean getCanvasLayerVisibility(NetworkLayer layer)
    {
        return aux_layerToGUI.get(layer).isLayerVisible();
    }

    public Map<NetworkLayer, Boolean> getMapCanvasLinkVisibility()
    {
        return layerList.stream().collect(Collectors.toMap(GUILayer::getAssociatedNetworkLayer, GUILayer::isLinksVisible));
    }

    public boolean getCanvasLinkVisibility(NetworkLayer layer)
    {
        return aux_layerToGUI.get(layer).isLinksVisible();
    }

    public void addLinkVisibility(NetworkLayer layer, boolean visibility)
    {
        if (!aux_layerToGUI.containsKey(layer))
            throw new RuntimeException("Layer does not belong to current NetPlan...");
        aux_layerToGUI.get(layer).setLinksVisibility(visibility);
    }

    public VisualizationSnapshot copy()
    {
        final NetPlan npCopy = netPlan.copy();

        final VisualizationSnapshot snapshotCopy = new VisualizationSnapshot(npCopy);

        // Copy layer order
        for (Map.Entry<NetworkLayer, Integer> entry : getMapCanvasLayerVisualizationOrder().entrySet())
        {
            snapshotCopy.setLayerVisualizationOrder(npCopy.getNetworkLayer(entry.getKey().getIndex()), entry.getValue());
        }

        for (Map.Entry<NetworkLayer, Boolean> entry : getMapCanvasLayerVisibility().entrySet())
        {
            snapshotCopy.addLayerVisibility(npCopy.getNetworkLayer(entry.getKey().getIndex()), entry.getValue());
        }

        for (Map.Entry<NetworkLayer, Boolean> entry : getMapCanvasLinkVisibility().entrySet())
        {
            snapshotCopy.addLinkVisibility(npCopy.getNetworkLayer(entry.getKey().getIndex()), entry.getValue());
        }

        return snapshotCopy;
    }

    public Triple<NetPlan, Map<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> getSnapshotDefinition()
    {
        return Triple.unmodifiableOf(netPlan, getMapCanvasLayerVisualizationOrder(), getMapCanvasLayerVisibility());
    }
}
