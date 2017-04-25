package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.state;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 25/04/17
 */
class CanvasStateMirror
{
    private final CanvasState state;

    private final Point2D canvasCenter;
    private final double zoomLevel;

    CanvasStateMirror(CanvasState state, Point2D mapCenter, double zoomLevel)
    {
        this.state = state;
        this.canvasCenter = mapCenter;
        this.zoomLevel = zoomLevel;
    }

    CanvasState getState()
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
