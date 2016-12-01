package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.mapControl.osm.OSMMapController;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.ITopologyCanvas;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
public class OSMRunningState extends OSMState
{
    @Override
    public void panTo(Point2D initialPoint, Point2D currentPoint)
    {
        final double dxPanelPixelCoord = (currentPoint.getX() - initialPoint.getX());
        final double dyPanelPixelCoord = (currentPoint.getY() - initialPoint.getY());

        OSMMapController.moveMap(-dxPanelPixelCoord, -dyPanelPixelCoord);
    }

    @Override
    public void zoomIn()
    {
        OSMMapController.zoomIn();
    }

    @Override
    public void zoomOut()
    {
        OSMMapController.zoomOut();
    }

    @Override
    public void zoomAll()
    {
        OSMMapController.restoreMap();
    }

    @Override
    public void addNode(TopologyPanel topologyPanel, NetPlan netPlan, String name, Point2D pos)
    {
        final GeoPosition geoPosition = OSMMapController.convertPointToGeo(new Point2D.Double(pos.getX(), -pos.getY()));
        final Node node = netPlan.addNode(geoPosition.getLongitude(), geoPosition.getLatitude(), name, null);

        topologyPanel.getCanvas().addNode(node);
        topologyPanel.getCanvas().refresh();

        OSMMapController.restoreMap();
    }

    @Override
    public void moveNode(INetworkCallback callback, ITopologyCanvas canvas, Node node, Point2D pos)
    {
//        final Point2D jungPoint = canvas.convertViewCoordinatesToRealCoordinates(pos);
//        final GeoPosition geoPosition = OSMMapController.convertPointToGeo(pos);
//
//        callback.moveNode(node.getId(), new Point2D.Double(geoPosition.getLongitude(), geoPosition.getLatitude()));
//        canvas.moveNodeToXYPosition(node, new Point2D.Double(jungPoint.getX(), -jungPoint.getY()));
    }
}
