package com.net2plan.gui.plugins.networkDesign.visualizationControl;

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Jorge San Emeterio on 8/03/17.
 */
public class VisualizationSnapshot
{
    private NetPlan netPlan;
    private BidiMap<NetworkLayer, Integer> mapCanvasLayerVisualizationOrder;
    private Map<NetworkLayer, Boolean> mapCanvasLayerVisibility;
    private Map<NetworkLayer, Boolean> mapCanvasLinkVisibility;

    public VisualizationSnapshot(NetPlan netPlan)
    {
        this.netPlan = netPlan;
        this.mapCanvasLayerVisualizationOrder = new DualHashBidiMap<>();
        this.mapCanvasLayerVisibility = netPlan.getNetworkLayers().stream().collect(Collectors.toMap(layer -> layer, layer -> true));
        this.mapCanvasLinkVisibility = netPlan.getNetworkLayers().stream().collect(Collectors.toMap(layer -> layer, layer -> true));
    }

    public NetPlan getNetPlan()
    {
        return netPlan;
    }

    public void setNetPlan(NetPlan netPlan)
    {
        this.netPlan = netPlan;
    }

    public BidiMap<NetworkLayer, Integer> getMapCanvasLayerVisualizationOrder()
    {
        return mapCanvasLayerVisualizationOrder;
    }

    public int getCanvasLayerVisualizationOrder(NetworkLayer layer)
    {
        final Integer res = mapCanvasLayerVisualizationOrder.get(layer);
        if  (res == null) throw new RuntimeException("Layer not found...");
        return res;
    }

    public Map<NetworkLayer, Boolean> getMapCanvasLayerVisibility()
    {
        return mapCanvasLayerVisibility;
    }

    public boolean getCanvasLayerVisibility(NetworkLayer layer)
    {
        final Boolean res = mapCanvasLayerVisibility.get(layer);
        if  (res == null) throw new RuntimeException("Layer not found...");
        return res;
    }

    public Map<NetworkLayer, Boolean> getMapCanvasLinkVisibility()
    {
        return mapCanvasLinkVisibility;
    }

    public boolean getCanvasLinkVisibility(NetworkLayer layer)
    {
        final Boolean res = mapCanvasLinkVisibility.get(layer);
        if  (res == null) throw new RuntimeException("Layer not found...");
        return res;
    }

    public VisualizationSnapshot copy()
    {
        return null;
    }
}
