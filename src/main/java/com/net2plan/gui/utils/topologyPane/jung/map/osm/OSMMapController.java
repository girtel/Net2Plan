package com.net2plan.gui.utils.topologyPane.jung.map.osm;

import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.jung.JUNGCanvas;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.ITopologyCanvas;
import com.net2plan.utils.Pair;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.viewer.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.*;

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

    static
    {
        mapViewer = new OSMMapPanel();
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

        // 1st step: Loading the map
        loadMap();

//        // 2nd step: Loading the snapshot
//        loadSnapshot();
    }

    private static void loadMap()
    {
        // Restart map original configuration
        // stopMap();
        // Activating maps on the canvas
        activateMap();

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
    }

    public static void loadSnapshot()
    {
        final int w = canvas.getComponent().getWidth();
        final int h = canvas.getComponent().getHeight();

        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) canvas.getComponent();
        mapViewer.remove(vv);

        // Getting snapshot
        final File file = mapViewer.saveMap(w, h);

        // Aligning the snapshot with the previous map
        final Rectangle viewInLayoutUnits = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(vv.getBounds()).getBounds();

        final Point2D mapCorner = new Point.Double(viewInLayoutUnits.getCenterX() - (w / 2), viewInLayoutUnits.getCenterY() - (h / 2));
        final Double mapCornerX = mapCorner.getX();
        final Double mapCornerY = mapCorner.getY();

        // Setting the photo as background
        ((JUNGCanvas) canvas).setBackgroundImage(file, mapCornerX.intValue(), mapCornerY.intValue());

        topologyPanel.remove(mapViewer);
        topologyPanel.add(vv, BorderLayout.CENTER);

        topologyPanel.validate();
        topologyPanel.repaint();
    }

    private static void activateMap()
    {
        ((JUNGCanvas) canvas).setMapActivated(true);

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
        if (((JUNGCanvas) canvas).isMapActivated())
        {
            ((JUNGCanvas) canvas).setMapActivated(false);
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

        final GeoPosition newMapGeo = tileFactory.pixelToGeo(newMapCenter, mapViewer.getZoom());

        for (Node node : callback.getDesign().getNodes())
        {
            final Point2D nodeXY = node.getXYPositionMap();
            final Point2D newNodeXY = new Point2D.Double(nodeXY.getX() - dx, nodeXY.getY() - dy);
        }
        mapViewer.setCenterPosition(newMapGeo);

        mapViewer.repaint();
    }
}
