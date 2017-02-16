package com.net2plan.gui.utils;

import java.util.*;

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
    private final IVisualizationCallback callback;
    private List<TimelineState> pastInfoVsNewNp;
    private int pastInfoVsNewNpCursor;
    private int maxSizeUndoList;

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

        final TimelineState state = createState(callback.getDesign());

        // Removing all changes made after the one at the cursor
        if (pastInfoVsNewNpCursor != pastInfoVsNewNp.size() - 1)
        {
            pastInfoVsNewNp.subList(pastInfoVsNewNpCursor, pastInfoVsNewNp.size()).clear();

            // Adding a copy of the current state before it was modified
            pastInfoVsNewNp.add(backupState);
        }
        pastInfoVsNewNp.add(state);

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
        if (!checkMovementValidity()) return null;
        if (pastInfoVsNewNpCursor == 0) return null;

        this.pastInfoVsNewNpCursor--;

        final TimelineState currentState = pastInfoVsNewNp.get(this.pastInfoVsNewNpCursor);
        this.backupState = createState(currentState.getStateDefinition().getFirst());

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
        if (!checkMovementValidity()) return null;
        if (pastInfoVsNewNpCursor == pastInfoVsNewNp.size() - 1) return null;

        this.pastInfoVsNewNpCursor++;

        final TimelineState currentState = pastInfoVsNewNp.get(this.pastInfoVsNewNpCursor);
        this.backupState = createState(currentState.getStateDefinition().getFirst());

        return currentState.getStateDefinition();
    }

    private boolean checkMovementValidity()
    {
        return !(pastInfoVsNewNp.isEmpty() || this.maxSizeUndoList <= 1 || callback.inOnlineSimulationMode());
    }

    /**
     * Creates a new timeline state from a given NetPlan moment.
     *
     * @param netPlan
     * @return
     */
    private TimelineState createState(final NetPlan netPlan)
    {
        final NetPlan npCopy = netPlan.copy();

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

        return new TimelineState(npCopy, cp_mapLayer2VisualizationOrder, cp_layerVisibilityMap);
    }

    private class TimelineState
    {
        private final NetPlan netPlan;
        private final BidiMap<NetworkLayer, Integer> layerOrderMap;
        private final Map<NetworkLayer, Boolean> layerVisibilityMap;

        private TimelineState(final NetPlan netPlan, final BidiMap<NetworkLayer, Integer> layerOrderMap, final Map<NetworkLayer, Boolean> layerVisibilityMap)
        {
            this.netPlan = netPlan;
            this.layerOrderMap = layerOrderMap;
            this.layerVisibilityMap = layerVisibilityMap;
        }

        private Triple<NetPlan, BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> getStateDefinition()
        {
            return Triple.unmodifiableOf(netPlan, layerOrderMap, layerVisibilityMap);
        }
    }
}
