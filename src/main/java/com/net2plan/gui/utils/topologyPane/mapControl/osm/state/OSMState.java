package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.net2plan.gui.utils.IVisualizationControllerCallback;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.ITopologyCanvas;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
public interface OSMState
{
    void panTo(Point2D initialPoint, Point2D currentPoint);

    void zoomIn();

    void zoomOut();

    void zoomAll();
    
    Point2D.Double translateNodeBaseCoordinatesIntoNetPlanCoordinates (ITopologyCanvas canvas, Point2D pos);

    void moveNodeInVisualization(ITopologyCanvas canvas, Node node, Point2D pos);

    void takeSnapshot(ITopologyCanvas canvas);
}