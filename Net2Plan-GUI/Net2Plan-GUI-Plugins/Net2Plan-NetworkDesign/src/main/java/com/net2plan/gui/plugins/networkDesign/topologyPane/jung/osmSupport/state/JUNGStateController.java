package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.state;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.topologyPane.TopologyPanel;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.OSMController;
import com.net2plan.interfaces.networkDesign.Node;

import javax.annotation.Nonnull;
import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 17-Jan-17
 */
public class JUNGStateController
{
    private IJUNGState currentState;
    private final ViewState viewState;
    private final OSMState osmState;
    private final SiteState siteState;

    private final GUINetworkDesign callback;
    private final TopologyPanel topologyPanel;
    private final ITopologyCanvas canvas;

    private final OSMController mapController;

    public enum JUNGState { ViewState, OSMState, SiteState };


    public JUNGStateController(final GUINetworkDesign callback, final TopologyPanel topologyPanel, final ITopologyCanvas canvas)
    {
        this.callback = callback;
        this.topologyPanel = topologyPanel;
        this.canvas = canvas;

        this.mapController = new OSMController();

        viewState = new ViewState(callback, canvas);
        osmState = new OSMState(callback, canvas, mapController);
        siteState = new SiteState();

        currentState = viewState;
    }

    public void setState( JUNGState state)
    {
        switch (state)
        {
            case ViewState:

                break;
            case OSMState:
                break;
            case SiteState:
                break;
        }
    }

    public void setRunningState()
    {
        if (currentState == osmState) return;
        currentState = osmState;
        mapController.startMap(callback, topologyPanel, canvas);
    }

    public void setStoppedState()
    {
        if (currentState == viewState) return;
        currentState = viewState;
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
        return currentState instanceof OSMState;
    }
}
