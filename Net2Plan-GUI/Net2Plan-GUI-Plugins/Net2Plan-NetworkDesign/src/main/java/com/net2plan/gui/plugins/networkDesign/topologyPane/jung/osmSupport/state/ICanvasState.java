package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.state;

import com.net2plan.interfaces.networkDesign.Node;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
interface ICanvasState
{
    void start();

    void stop();

    CanvasState getState();

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