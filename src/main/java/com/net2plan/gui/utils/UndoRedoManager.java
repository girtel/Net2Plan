package com.net2plan.gui.utils;

import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.utils.Triple;

/**
 * Manages the undo/redo information, tracking the current netPlan and the visualization state
 */
public class UndoRedoManager
{
    private List<TimelineState> pastInfoVsNewNp;
    private int pastInfoVsNewNpCursor;
    private int maxSizeUndoList;
    private final IVisualizationCallback callback;

    private TimelineState backupState;

    public UndoRedoManager(IVisualizationCallback callback, int maxSizeUndoList)
    {
        this.pastInfoVsNewNp = new ArrayList<>();
        this.pastInfoVsNewNpCursor = -1;
        this.callback = callback;
        this.maxSizeUndoList = maxSizeUndoList;
    }

    public void addNetPlanChange()
    {
        if (this.maxSizeUndoList <= 1) return; // nothing is stored since nothing will be retrieved
        if (callback.inOnlineSimulationMode()) return;

        final NetPlan npCopy = callback.getDesign().copy();

        final BidiMap<NetworkLayer, Integer> cp_mapLayer2VisualizationOrder = new DualHashBidiMap<>();
        for (NetworkLayer cpLayer : npCopy.getNetworkLayers())
        {
            cp_mapLayer2VisualizationOrder.put(cpLayer, callback.getVisualizationState().getCanvasVisualizationOrderNotRemovingNonVisible(callback.getDesign().getNetworkLayer(cpLayer.getIndex())));
        }

        final Map<NetworkLayer, Boolean> cp_layerVisibilityMap = new HashMap<>();
        for (NetworkLayer cpLayer : npCopy.getNetworkLayers())
        {
            cp_layerVisibilityMap.put(cpLayer, callback.getVisualizationState().isLayerVisibleInCanvas(callback.getDesign().getNetworkLayer(cpLayer.getIndex())));
        }

        // Removing all changes made after the one at the cursor
        if (pastInfoVsNewNpCursor != pastInfoVsNewNp.size() - 1)
        {
            pastInfoVsNewNp.subList(pastInfoVsNewNpCursor + 1, pastInfoVsNewNp.size()).clear();
        }

        pastInfoVsNewNp.add(new TimelineState(npCopy, cp_mapLayer2VisualizationOrder, cp_layerVisibilityMap));

        // Remove the older changes so that the list does not bloat.
        while (pastInfoVsNewNp.size() > maxSizeUndoList)
        {
            pastInfoVsNewNp.remove(0);
            pastInfoVsNewNpCursor--;
        }

        pastInfoVsNewNpCursor++;
    }

    /**
     * Returns the undo info in the navigation. Returns null if we are already in the first element. The NetPlan object returned is null if
     * there is no change respect to the current one
     *
     * @return see above
     */
    public Triple<NetPlan, BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> getNavigationBackElement()
    {
        if (pastInfoVsNewNp.isEmpty()) return null;
        if (this.maxSizeUndoList <= 1) return null; // nothing is stored since nothing will be retrieved
        if (callback.inOnlineSimulationMode()) return null;
        if (pastInfoVsNewNpCursor == 0) return null;
        this.pastInfoVsNewNpCursor--;
        final TimelineState currentState = pastInfoVsNewNp.get(this.pastInfoVsNewNpCursor);
        this.backupState = currentState;
        return currentState.getStateDefinition();
    }

    /**
     * Returns the forward info in the navigation. Returns null if we are already in the head. The NetPlan object returned is null if
     * there is no change respect to the current one
     *
     * @return see above
     */
    public Triple<NetPlan, BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> getNavigationForwardElement()
    {
        if (pastInfoVsNewNp.isEmpty()) return null;
        if (this.maxSizeUndoList <= 1) return null; // nothing is stored since nothing will be retrieved
        if (callback.inOnlineSimulationMode()) return null;
        if (pastInfoVsNewNpCursor == pastInfoVsNewNp.size() - 1) return null;
        this.pastInfoVsNewNpCursor++;
        final TimelineState currentState = pastInfoVsNewNp.get(this.pastInfoVsNewNpCursor);
        this.backupState = currentState;
        return currentState.getStateDefinition();
    }

    private class TimelineState
    {
        private final NetPlan netPlan;
        private final BidiMap<NetworkLayer, Integer> layerOrderMap;
        private final Map<NetworkLayer, Boolean> layerVisibilityMap;

        public TimelineState(final NetPlan netPlan, final BidiMap<NetworkLayer, Integer> layerOrderMap, final Map<NetworkLayer, Boolean> layerVisibilityMap)
        {
            this.netPlan = netPlan;
            this.layerOrderMap = layerOrderMap;
            this.layerVisibilityMap = layerVisibilityMap;
        }

        public Triple<NetPlan, BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> getStateDefinition()
        {
            return Triple.unmodifiableOf(netPlan, layerOrderMap, layerVisibilityMap);
        }
    }
}
