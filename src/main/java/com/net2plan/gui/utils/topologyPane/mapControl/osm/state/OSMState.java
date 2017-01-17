package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.ITopologyCanvas;

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
    
    void addNode(IVisualizationCallback callback, ITopologyCanvas canvas, Point2D pos);

    void removeNode(IVisualizationCallback callback, Node node);

    void moveNodeInVisualization(ITopologyCanvas canvas, Node node, Point2D pos);

    void takeSnapshot(ITopologyCanvas canvas);
}