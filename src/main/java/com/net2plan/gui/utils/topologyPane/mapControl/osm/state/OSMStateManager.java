package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.jung.JUNGCanvas;
import com.net2plan.gui.utils.topologyPane.mapControl.osm.OSMMapController;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.ITopologyCanvas;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
public class OSMStateManager
{
    private OSMState currentState;
    private final OSMRunningState runningState;
    private final OSMStoppedState stoppedState;

    private final TopologyPanel topologyPanel;
    private final ITopologyCanvas canvas;
    private final INetworkCallback callback;

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
        OSMMapController.startMap(topologyPanel, canvas, callback);
    }

    public void setStoppedState()
    {
        currentState = stoppedState;
        OSMMapController.cleanMap();
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

    public void addNode(final NetPlan netPlan, final String name, final Point2D pos)
    {
        currentState.addNode(topologyPanel, netPlan, name, pos);
    }

    public void modeNode(final Node node, final Point2D pos)
    {
        currentState.moveNode(callback, canvas, node, pos);
    }

    public OSMState getCurrentState()
    {
        return currentState;
    }

    public void takeSnapshot(final ITopologyCanvas canvas)
    {
        currentState.takeSnapshot(canvas);
    }
}
