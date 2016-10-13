package com.net2plan.gui.utils.topologyPane.jung.map;

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Pair;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Jorge San Emeterio on 13/10/2016.
 */
public class MapPanel
{
    private final JXMapViewer mapViewer;
    private final TileFactoryInfo info;
    private final DefaultTileFactory tileFactory;

    public MapPanel()
    {
        // Background image
        mapViewer = new JXMapViewer();

        // Create a TileFactoryInfo for OpenStreetMap
        info = new OSMTileFactoryInfo();
        tileFactory = new DefaultTileFactory(info);
        mapViewer.setTileFactory(tileFactory);

        // Use 8 threads in parallel to load the tiles
        tileFactory.setThreadPoolSize(8);

        // Set the focus
        final GeoPosition USA = new GeoPosition(39.50, -98.35);

        mapViewer.setZoom(15);
        mapViewer.setAddressLocation(USA);
    }

    public JComponent getMapComponent()
    {
        return mapViewer;
    }

    public Point2D.Double getMapCoords()
    {
        final GeoPosition centerPosition = mapViewer.getCenterPosition();

        return new Point2D.Double(centerPosition.getLatitude(), centerPosition.getLongitude());
    }

    public void setMapZoom(final int zoom)
    {
        mapViewer.setZoom(zoom);
    }


    public void centerMap(final NetPlan netPlan)
    {
        centerMap(netPlan.getNodes());
    }

    public void centerMap(final List<Node> nodes)
    {
        final Point2D.Double topologyCenter = getTopologyCenter(nodes);

        setMapCoords(topologyCenter.getY(), topologyCenter.getX());
    }

    public void setMapCoords(final double lat, final double lon)
    {
        final GeoPosition newLoc = new GeoPosition(lat, lon);

        mapViewer.setAddressLocation(newLoc);
    }

    public Point2D.Double getTopologyCenter(final List<Node> nodes)  {
        final List<Point2D> knots = nodes.stream().map(node -> node.getXYPositionMap()).collect(Collectors.toList());

        double centroidX = 0, centroidY = 0;

        for(Point2D knot : knots) {
            centroidX += knot.getX();
            centroidY += knot.getY();
        }
        return new Point2D.Double(centroidX / knots.size(), centroidY / knots.size());
    }
}
