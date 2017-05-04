package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 25/04/17
 */
class CanvasStateMirror
{
    private final ICanvasState state;

    private final Point2D canvasCenter;
    private final double zoomLevel;

    CanvasStateMirror(ICanvasState state, Point2D mapCenter, double zoomLevel)
    {
        this.state = state;
        this.canvasCenter = mapCenter;
        this.zoomLevel = zoomLevel;
    }

    ICanvasState getState()
    {
        return state;
    }

    Point2D getCanvasCenter()
    {
        return canvasCenter;
    }

    double getZoomLevel()
    {
        return zoomLevel;
    }
}
