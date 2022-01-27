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

import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.gui.plugins.networkDesign.interfaces.patterns.IState;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
public interface ICanvasState extends IState
{
    CanvasOption getState();

    Color getBackgroundColor();

    void panTo(Point2D initialPoint, Point2D currentPoint);

    void zoomIn();

    void zoomOut();

    void zoomAll();
    
    void addNode(Point2D pos);

    void removeNode(Node node);

    void takeSnapshot();

    void updateNodesXYPosition();

    double getInterLayerDistance(int interLayerDistanceInPixels);

    Point2D getCanvasPoint(final Point2D pos);
}