package com.net2plan.gui.plugins.networkDesign.visualizationControl;

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.utils.Triple;
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
        this.resetSnapshot();
    }

    public void resetSnapshot()
    {
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

    public void addLayerVisualizationOrder(NetworkLayer layer, int order)
    {
        if (!netPlan.getNetworkLayers().contains(layer))
            throw new RuntimeException("Layer does not belong to current NetPlan...");
        mapCanvasLayerVisualizationOrder.put(layer, order);
    }

    public int getCanvasLayerVisualizationOrder(NetworkLayer layer)
    {
        final Integer res = mapCanvasLayerVisualizationOrder.get(layer);
        if (res == null) throw new RuntimeException("Layer not found...");
        return res;
    }

    public Map<NetworkLayer, Boolean> getMapCanvasLayerVisibility()
    {
        return mapCanvasLayerVisibility;
    }

    public void addLayerVisibility(NetworkLayer layer, boolean visibility)
    {
        if (!netPlan.getNetworkLayers().contains(layer))
            throw new RuntimeException("Layer does not belong to current NetPlan...");
        mapCanvasLayerVisibility.put(layer, visibility);
    }

    public boolean getCanvasLayerVisibility(NetworkLayer layer)
    {
        final Boolean res = mapCanvasLayerVisibility.get(layer);
        if (res == null) throw new RuntimeException("Layer not found...");
        return res;
    }

    public Map<NetworkLayer, Boolean> getMapCanvasLinkVisibility()
    {
        return mapCanvasLinkVisibility;
    }

    public boolean getCanvasLinkVisibility(NetworkLayer layer)
    {
        final Boolean res = mapCanvasLinkVisibility.get(layer);
        if (res == null) throw new RuntimeException("Layer not found...");
        return res;
    }

    public void addLinkVisibility(NetworkLayer layer, boolean visibility)
    {
        if (!netPlan.getNetworkLayers().contains(layer))
            throw new RuntimeException("Layer does not belong to current NetPlan...");
        mapCanvasLinkVisibility.put(layer, visibility);
    }

    public VisualizationSnapshot copy()
    {
        final NetPlan npCopy = netPlan.copy();

        final VisualizationSnapshot snapshotCopy = new VisualizationSnapshot(npCopy);

        // Copy layer order
        for (Map.Entry<NetworkLayer, Integer> entry : mapCanvasLayerVisualizationOrder.entrySet())
        {
            snapshotCopy.addLayerVisualizationOrder(npCopy.getNetworkLayer(entry.getKey().getIndex()), entry.getValue());
        }

        for (Map.Entry<NetworkLayer, Boolean> entry : mapCanvasLayerVisibility.entrySet())
        {
            snapshotCopy.addLayerVisibility(npCopy.getNetworkLayer(entry.getKey().getIndex()), entry.getValue());
        }

        for (Map.Entry<NetworkLayer, Boolean> entry : mapCanvasLinkVisibility.entrySet())
        {
            snapshotCopy.addLinkVisibility(npCopy.getNetworkLayer(entry.getKey().getIndex()), entry.getValue());
        }

        return snapshotCopy;
    }

    public Triple<NetPlan, BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> getSnapshotDefinition()
    {
        return Triple.unmodifiableOf(netPlan, mapCanvasLayerVisualizationOrder, mapCanvasLayerVisibility);
    }
}
