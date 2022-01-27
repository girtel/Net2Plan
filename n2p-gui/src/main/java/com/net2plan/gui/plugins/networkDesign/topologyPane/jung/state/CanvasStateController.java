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
package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.topologyPane.TopologyPanel;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.OSMController;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.ErrorHandling;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 17-Jan-17
 */
public class CanvasStateController
{
    private ICanvasState currentState;

    private final ViewState viewState;
    private final OSMState osmState;
    private final SiteState siteState;

    private final GUINetworkDesign callback;
    private final TopologyPanel topologyPanel;
    private final ITopologyCanvas canvas;

    private final OSMController mapController;

    public CanvasStateController(GUINetworkDesign callback, TopologyPanel topologyPanel, ITopologyCanvas canvas)
    {
        assert callback != null;
        assert topologyPanel != null;
        assert canvas != null;

        this.callback = callback;
        this.topologyPanel = topologyPanel;
        this.canvas = canvas;

        this.mapController = new OSMController(callback, topologyPanel, canvas);

        viewState = new ViewState(callback, canvas, mapController);
        osmState = new OSMState(callback, canvas, mapController);
        siteState = new SiteState(callback, canvas, mapController);

        currentState = viewState;
    }

    public void setState(CanvasOption state, Object... stateParameters)
    {
        assert state != null;

        // Change state
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
                    assert stateParameters != null;
                    assert stateParameters.length == 1;
                    assert stateParameters[0] instanceof Node;

                    final Node node = (Node) stateParameters[0];

                    siteState.setNode(node);
                    currentState = siteState;
                    break;
            }

            currentState.start();
            callback.updateVisualizationAfterCanvasState();

            // Changing background color
            canvas.getCanvasComponent().setBackground(currentState.getBackgroundColor());
        } catch (RuntimeException e)
        {
            e.printStackTrace();
            this.setState(viewState.getState());
        }
    }

    public CanvasOption getState()
    {
        return currentState.getState();
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
}
