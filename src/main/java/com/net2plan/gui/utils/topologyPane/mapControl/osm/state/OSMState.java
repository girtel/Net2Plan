package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
public abstract class OSMState
{
    public abstract void panTo(Point2D initialPoint, Point2D currentPoint);
    public abstract void zoomIn();
    public abstract void zoomOut();
    public abstract void zoomAll();
    public abstract void addNode(TopologyPanel topologyPanel, NetPlan netPlan, String name, Point2D pos);
}