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
