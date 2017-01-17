package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.mapControl.osm.OSMMapController;
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

    private final TopologyPanel topologyPanel;
    private final ITopologyCanvas canvas;
    private final IVisualizationCallback callback;

    private final OSMMapController mapController;

    OSMStateManager(final TopologyPanel topologyPanel, final ITopologyCanvas canvas, final IVisualizationCallback callback)
    {
        this.topologyPanel = topologyPanel;
        this.canvas = canvas;
        this.callback = callback;
        this.mapController = new OSMMapController();

        runningState = new OSMOnState(mapController);
        stoppedState = new OSMOffState(canvas);
        currentState = stoppedState;
    }

    public void setRunningState()
    {
        if (currentState == runningState) return;
        currentState = runningState;
        mapController.startMap(topologyPanel, canvas, callback);
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

    public void addNode(final IVisualizationCallback callback, final ITopologyCanvas canvas, final Point2D pos)
    {
        currentState.addNode(callback, canvas, pos);
    }

    public void removeNode(final IVisualizationCallback callback, final Node node)
    {
        currentState.removeNode(callback, node);
    }

    public void moveNode(final Node node, final Point2D pos)
    {
        currentState.moveNodeInVisualization(canvas, node, pos);
    }

    public boolean isMapActivated()
    {
        return currentState instanceof OSMOnState;
    }

    public void takeSnapshot(final ITopologyCanvas canvas)
    {
        currentState.takeSnapshot(canvas);
    }
}
