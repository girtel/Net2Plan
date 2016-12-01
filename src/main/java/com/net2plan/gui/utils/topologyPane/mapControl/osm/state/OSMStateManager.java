package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.jung.JUNGCanvas;
import com.net2plan.gui.utils.topologyPane.mapControl.osm.OSMMapController;
import com.net2plan.internal.plugins.ITopologyCanvas;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
public class OSMStateManager
{
    private OSMState currentState;
    private OSMRunningState runningState;
    private OSMStoppedState stoppedState;

    final TopologyPanel topologyPanel;
    final ITopologyCanvas canvas;
    final INetworkCallback callback;

    public OSMStateManager(final TopologyPanel topologyPanel, final ITopologyCanvas canvas, final INetworkCallback callback)
    {
        this.topologyPanel = topologyPanel;
        this.canvas = canvas;
        this.callback = callback;

        runningState = new OSMRunningState();
        stoppedState = new OSMStoppedState(canvas);
        currentState = stoppedState;
    }

    public void setRunningState()
    {
        currentState = runningState;
        ((JUNGCanvas) canvas).removeScalingPlugin();
        OSMMapController.startMap(topologyPanel, canvas, callback);
    }

    public void setStoppedState()
    {
        currentState = stoppedState;
        ((JUNGCanvas) canvas).addScalingPlugin();
        OSMMapController.disableMapSupport();
    }

    public void panTo(Point2D initialPoint, Point2D currentPoint)
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
}
