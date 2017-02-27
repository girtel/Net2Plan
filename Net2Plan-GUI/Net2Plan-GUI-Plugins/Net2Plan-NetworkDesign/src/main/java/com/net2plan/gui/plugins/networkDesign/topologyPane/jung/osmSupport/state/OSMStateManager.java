package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.state;

import com.net2plan.gui.plugins.networkDesign.topologyPane.TopologyPanel;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.OSMController;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.Node;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 17-Jan-17
 */
public class OSMStateManager
{
    private OSMState currentState;
    private final OSMOnState runningState;
    private final OSMJUNGOffState stoppedState;

    private final GUINetworkDesign callback;
    private final TopologyPanel topologyPanel;
    private final ITopologyCanvas canvas;

    private final OSMController mapController;

    public OSMStateManager(final GUINetworkDesign callback, final TopologyPanel topologyPanel, final ITopologyCanvas canvas)
    {
        this.callback = callback;
        this.topologyPanel = topologyPanel;
        this.canvas = canvas;
        this.mapController = new OSMController();

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
        currentState.updateNodesXYPosition();
    }

    public double getCanvasInterlayerDistance(final int interLayerDistanceInPixels)
    {
        return currentState.getInterLayerDistance(interLayerDistanceInPixels);
    }

    public Point2D getCanvasCoordinateFromScreenPoint(final Point2D pos)
    {
        return currentState.getCanvasPoint(pos);
    }

    public boolean isMapActivated()
    {
        return currentState instanceof OSMOnState;
    }
}
