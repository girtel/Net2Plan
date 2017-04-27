package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state;

import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.gui.plugins.networkDesign.interfaces.patterns.IState;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
public interface ICanvasState extends IState
{
    CanvasOption getState();

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