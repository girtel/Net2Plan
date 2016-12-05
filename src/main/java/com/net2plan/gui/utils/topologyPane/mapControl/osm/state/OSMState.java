package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.net2plan.gui.utils.INetworkCallback;
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

    void addNode(TopologyPanel topologyPanel, NetPlan netPlan, String name, Point2D pos);

    void moveNode(INetworkCallback callback, ITopologyCanvas canvas, Node node, Point2D pos);

    void takeSnapshot(ITopologyCanvas canvas);
}