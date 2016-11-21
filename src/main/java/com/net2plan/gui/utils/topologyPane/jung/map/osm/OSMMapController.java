package com.net2plan.gui.utils.topologyPane.jung.map.osm;

import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.SwingUtils;
import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.jung.JUNGCanvas;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.ITopologyCanvas;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import org.jxmapviewer.viewer.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * Created by Jorge San Emeterio on 03/11/2016.
 */
public class OSMMapController
{
    private static final OSMMapPanel mapViewer;

    private static TopologyPanel topologyPanel;
    private static ITopologyCanvas canvas;
    private static INetworkCallback callback;

    private static final String ATTRIB_LATITUDE = "lat";
    private static final String ATTRIB_LONGITUDE = "lon";

    private static boolean isMapActivated;

    static
    {
        mapViewer = new OSMMapPanel();
        isMapActivated = false;
    }

    // Non-instanciable
    private OSMMapController()
    {
    }

    public static void runMap(final TopologyPanel topologyPanel, final ITopologyCanvas canvas, final INetworkCallback callback)
    {
        OSMMapController.topologyPanel = topologyPanel;
        OSMMapController.canvas = canvas;
        OSMMapController.callback = callback;

        loadMap();
    }

    private static void loadMap()
    {
        // Restart map original configuration
        // stopMap();

        // Activating maps on the canvas
        loadMapOntoTopologyPanel();

        // Calculating map position
        final HashSet<GeoPosition> positionSet = new HashSet<>();

        final HashSet<Waypoint> waypoints = new HashSet<>();

        for (Node node : callback.getDesign().getNodes())
        {
            // Getting coords from nodes attributes
            final double latitude = Double.parseDouble(node.getAttribute(ATTRIB_LATITUDE));
            final double longitude = Double.parseDouble(node.getAttribute(ATTRIB_LONGITUDE));

            final GeoPosition geoPosition = new GeoPosition(latitude, longitude);
            positionSet.add(geoPosition);

            final DefaultWaypoint waypoint = new DefaultWaypoint(geoPosition);
            waypoints.add(waypoint);

            // The position that the node really takes on the map. This is the point where the map and the nodes align.
            final Point2D realPosition = mapViewer.getTileFactory().geoToPixel(geoPosition, mapViewer.getZoom());
            callback.moveNode(node.getId(), new Point2D.Double(realPosition.getX(), -realPosition.getY()));
        }

        WaypointPainter<Waypoint> waypointPainter = new WaypointPainter<>();
        waypointPainter.setWaypoints(waypoints);

        mapViewer.setOverlayPainter(waypointPainter);
        mapViewer.zoomToBestFit(positionSet, 0.6);

        topologyPanel.zoomAll();
        fitTopologyToMap();

        isMapActivated = true;

        canvas.refresh();
        mapViewer.repaint();
    }

    public static void centerMapToNodes()
    {
        final HashSet<GeoPosition> positionSet = new HashSet<>();

        for (Node node : callback.getDesign().getNodes())
        {
            final double latitude = Double.parseDouble(node.getAttribute(ATTRIB_LATITUDE));
            final double longitude = Double.parseDouble(node.getAttribute(ATTRIB_LONGITUDE));

            final GeoPosition geoPosition = new GeoPosition(latitude, longitude);
            positionSet.add(geoPosition);
        }

        mapViewer.zoomToBestFit(positionSet, 0.6);

        canvas.refresh();
        mapViewer.repaint();
    }

    public static void fitTopologyToMap()
    {
        final List<Node> topologyNodes = callback.getDesign().getNodes();

        if (topologyNodes.size() > 1)
        {
            final Node firstNode = topologyNodes.get(0);
            final Node secondNode = topologyNodes.get(1);

            final Point2D firstNodeXYPositionMap = firstNode.getXYPositionMap();
            final Point2D secondNodeXYPositionMap = secondNode.getXYPositionMap();

            final double rOSM = Math.sqrt(Math.pow((secondNodeXYPositionMap.getX() - firstNodeXYPositionMap.getX()), 2) + Math.pow(secondNodeXYPositionMap.getY() + firstNodeXYPositionMap.getY(), 2));

//            System.out.println(swingXScaleRatio);
//            System.out.println(swingYScaleRatio);
//
//            System.out.println("....");

            final Point2D firstNodeXYPositionMapOnJUNG = ((JUNGCanvas) canvas).convertViewCoordinatesToRealCoordinates(firstNodeXYPositionMap);
            final Point2D secondNodeXYPositionMapOnJUNG = ((JUNGCanvas) canvas).convertViewCoordinatesToRealCoordinates(secondNodeXYPositionMap);

            final double rJUNG = Math.sqrt(Math.pow((secondNodeXYPositionMapOnJUNG.getX() - firstNodeXYPositionMapOnJUNG.getX()), 2) + Math.pow(secondNodeXYPositionMapOnJUNG.getY() + firstNodeXYPositionMapOnJUNG.getY(), 2));

            System.out.println(firstNodeXYPositionMapOnJUNG.getX() / secondNodeXYPositionMapOnJUNG.getX());
            System.out.println(firstNodeXYPositionMapOnJUNG.getY() / secondNodeXYPositionMapOnJUNG.getY());

            System.out.println("---");

            for (int i = 0; i < 15; i++)
            {
//                canvas.zoomOut();

                final Point2D firstNodeXYPositionMapOnJUNG2 = ((JUNGCanvas) canvas).convertViewCoordinatesToRealCoordinates(firstNodeXYPositionMap);
                final Point2D secondNodeXYPositionMapOnJUNG2 = ((JUNGCanvas) canvas).convertViewCoordinatesToRealCoordinates(secondNodeXYPositionMap);
//
//                System.out.println(firstNodeXYPositionMapOnJUNG2.getX() / secondNodeXYPositionMapOnJUNG2.getX());
//                System.out.println(firstNodeXYPositionMapOnJUNG2.getY() / secondNodeXYPositionMapOnJUNG2.getY());
//                System.out.println("---");

            }
        }
    }

    private static void loadMapOntoTopologyPanel()
    {
        // JUNG Canvas
        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) OSMMapController.canvas.getComponent();

        // Making some swing adjustments.
        // Canvas on top of the map panel.
        final LayoutManager layout = new OverlayLayout(mapViewer);
        mapViewer.setLayout(layout);

        mapViewer.add(vv);

        topologyPanel.add(mapViewer, BorderLayout.CENTER);
        topologyPanel.validate();
        topologyPanel.repaint();
    }

    private static void stopMap()
    {
        if (isMapActivated)
        {
            isMapActivated = false;
            mapViewer.removeAll();
            topologyPanel.remove(mapViewer);
        }

        topologyPanel.add(canvas.getComponent(), BorderLayout.CENTER);
        topologyPanel.validate();
        topologyPanel.repaint();
    }

    public static void moveMap(final double dx, final double dy)
    {
        final TileFactory tileFactory = mapViewer.getTileFactory();

        final Point2D mapCenter = mapViewer.getCenter();
        final Point2D newMapCenter = new Point2D.Double(mapCenter.getX() + dx, mapCenter.getY() + dy);

        mapViewer.setCenterPosition(tileFactory.pixelToGeo(newMapCenter, mapViewer.getZoom()));

        canvas.refresh();
        mapViewer.repaint();
    }

    public static boolean isMapActivated()
    {
        return isMapActivated;
    }

    public static void zoomIn()
    {
        final int zoom = mapViewer.getZoom();
        mapViewer.setZoom(zoom - 1);
    }

    public static void zoomOut()
    {
        final int zoom = mapViewer.getZoom();
        mapViewer.setZoom(zoom + 1);
    }
}
