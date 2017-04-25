package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.state;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.topologyPane.TopologyPanel;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.OSMController;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.state.observer.StateSubject;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.ErrorHandling;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 17-Jan-17
 */
public class CanvasStateController extends StateSubject
{
    private ICanvasState currentState;

    private final ViewState viewState;
    private final OSMState osmState;

    private final GUINetworkDesign callback;
    private final ITopologyCanvas canvas;

    private final OSMController mapController;

    public CanvasStateController(GUINetworkDesign callback, TopologyPanel topologyPanel, ITopologyCanvas canvas)
    {
        assert callback != null;
        assert topologyPanel != null;
        assert canvas != null;

        this.callback = callback;
        this.canvas = canvas;

        this.mapController = new OSMController(callback, topologyPanel, canvas);

        viewState = new ViewState(callback, canvas, mapController);
        osmState = new OSMState(callback, canvas, mapController);

        currentState = viewState;
    }

    @Override
    public void setState(CanvasState state, Object... stateParameters)
    {
        currentState.stop();

        try
        {
            switch (state)
            {
                case ViewState:
                    currentState = viewState;
                    break;
                case OSMState:
                    currentState = osmState;
                    break;
                case SiteState:
                    assert stateParameters.length == 1;
                    assert stateParameters[0] instanceof Node;

                    final Node node = (Node) stateParameters[0];

                    currentState = new SiteState(callback, canvas, mapController, node.getSiteName());
                    break;
            }

            currentState.start();

            notifyAllObservers();
        } catch (RuntimeException e)
        {
            ErrorHandling.showErrorDialog("Error");
            e.printStackTrace();
            this.setState(CanvasState.ViewState);
        }
    }

    @Override
    public CanvasState getState()
    {
        if (currentState instanceof ViewState)
            return CanvasState.ViewState;
        else if (currentState instanceof SiteState)
            return CanvasState.SiteState;
        else if (currentState instanceof OSMState)
            return CanvasState.OSMState;

        throw new RuntimeException();
    }

    // ** Back step controller **
    public void returnToPreviousState()
    {

    }

    // ** Mediator interface **
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

    public void addNode(Point2D pos)
    {
        currentState.addNode(pos);
    }

    public void removeNode(Node node)
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

    public double getCanvasInterlayerDistance(int interLayerDistanceInPixels)
    {
        return currentState.getInterLayerDistance(interLayerDistanceInPixels);
    }

    public Point2D getCanvasCoordinateFromScreenPoint(Point2D pos)
    {
        return currentState.getCanvasPoint(pos);
    }

    public boolean isMapActivated()
    {
        return currentState instanceof OSMState;
    }
}
