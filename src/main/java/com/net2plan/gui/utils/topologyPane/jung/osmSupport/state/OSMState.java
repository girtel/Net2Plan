package com.net2plan.gui.utils.topologyPane.jung.osmSupport.state;

import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.interfaces.networkDesign.Node;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
interface OSMState
{
    void panTo(Point2D initialPoint, Point2D currentPoint);

    void zoomIn();

    void zoomOut();

    void zoomAll();
    
    void addNode(Point2D pos);

    void removeNode(Node node);

    void takeSnapshot();

    void updateNodesXYPosition();

    double getInterLayerDistance(int interLayerDistanceInPixels);
}