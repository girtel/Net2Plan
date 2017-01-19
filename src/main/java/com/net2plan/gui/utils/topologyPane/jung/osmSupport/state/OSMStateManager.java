package com.net2plan.gui.utils.topologyPane.jung.osmSupport.state;

import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.jung.osmSupport.OSMMapController;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.ITopologyCanvas;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 17-Jan-17
 */
public class OSMStateManager
{
    private OSMState currentState;
    private final OSMOnState runningState;
    private final OSMOffState stoppedState;

    private final IVisualizationCallback callback;
    private final TopologyPanel topologyPanel;
    private final ITopologyCanvas canvas;

    private final OSMMapController mapController;

    public OSMStateManager(final IVisualizationCallback callback, final TopologyPanel topologyPanel, final ITopologyCanvas canvas)
    {
        this.callback = callback;
        this.topologyPanel = topologyPanel;
        this.canvas = canvas;
        this.mapController = new OSMMapController();

        runningState = new OSMOnState(callback, canvas, mapController);
        // Using JUNG canvas off state.
        stoppedState = new OSMJUNGOffState(callback, canvas);

        currentState = stoppedState;
    }

    public void setRunningState()
    {
        if (currentState == runningState) return;
        currentState = runningState;
        mapController.startMap(callback, topologyPanel, canvas);
    }

    public void setStoppedState()
    {
        if (currentState == stoppedState) return;
        currentState = stoppedState;
        mapController.cleanMap();
    }

    public void panTo(final Point2D initialPoint, final Point2D currentPoint)
    {
        currentState.panTo(initialPoint, currentPoint);
    }

    public void zoomIn()
    {
        currentState.zoomIn();
    }

    public void zoomOut()
    {
        currentState.zoomOut();
    }

    public void zoomAll()
    {
        currentState.zoomAll();
    }

    public void addNode(final Point2D pos)
    {
        currentState.addNode(pos);
    }

    public void removeNode(final Node node)
    {
        currentState.removeNode(node);
    }

    public void takeSnapshot()
    {
        currentState.takeSnapshot();
    }

    public void updateNodesXYPosition()
    {
        currentState.updateNodeXYPosition();
    }

    public boolean isMapActivated()
    {
        return currentState instanceof OSMOnState;
    }

}
