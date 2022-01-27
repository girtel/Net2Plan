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

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.utils.Triple;

import java.util.LinkedList;
import java.util.Map;

/**
 * Manages the undo/redo information, tracking the current netPlan and the visualization state
 */
public class UndoRedoManager
{
    private final GUINetworkDesign callback;
    private LinkedList<VisualizationSnapshot> timeline;
    private int timelineCursor;
    private int listMaxSize;

    private VisualizationSnapshot backupState;

    public UndoRedoManager(GUINetworkDesign callback, int listMaxSize)
    {
        this.timeline = new LinkedList<>();
        this.timelineCursor = -1;
        this.callback = callback;
        this.listMaxSize = listMaxSize;
    }

    public void addNetPlanChange()
    {
        if (this.listMaxSize <= 1) return; // nothing is stored since nothing will be retrieved
        if (callback.inOnlineSimulationMode()) return;

        final VisualizationSnapshot snapshot = callback.getVisualizationState().getSnapshot().copy();

        // Removing all changes made after the one at the cursor
        if (timelineCursor != timeline.size() - 1)
        {
            timeline.subList(timelineCursor, timeline.size()).clear();

            // Adding a copy of the current state before it was modified
            timeline.add(backupState);
        }
        timeline.add(snapshot);

        // Remove the older changes so that the list does not bloat.
        while (timeline.size() > listMaxSize)
        {
            timeline.remove(0);
            timelineCursor--;
        }

        timelineCursor++;
    }

    /**
     * Returns the undo info in the navigation. Returns null if we are already in the first element. The NetPlan object returned is null if
     * there is no change respect to the current one
     *
     * @return see above
     */
    public Triple<NetPlan, Map<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> getNavigationBackElement()
    {
        if (!checkMovementValidity()) return null;
        if (timelineCursor == 0) return null;

        this.timelineCursor--;

        final VisualizationSnapshot currentState = timeline.get(this.timelineCursor);
        this.backupState = currentState.copy();

        return currentState.getSnapshotDefinition();
    }

    /**
     * Returns the forward info in the navigation. Returns null if we are already in the head. The NetPlan object returned is null if
     * there is no change respect to the current one
     *
     * @return see above
     */
    public Triple<NetPlan, Map<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> getNavigationForwardElement()
    {
        if (!checkMovementValidity()) return null;
        if (timelineCursor == timeline.size() - 1) return null;

        this.timelineCursor++;

        final VisualizationSnapshot currentState = timeline.get(this.timelineCursor);
        this.backupState = currentState.copy();

        return currentState.getSnapshotDefinition();
    }

    private boolean checkMovementValidity()
    {
        return !(timeline.isEmpty() || this.listMaxSize <= 1 || callback.inOnlineSimulationMode());
    }
}
