package com.net2plan.gui.utils.topologyPane.jung.map.osm;

import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.SwingUtils;
import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.jung.JUNGCanvas;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.ITopologyCanvas;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import org.jxmapviewer.viewer.*;
import sun.swing.SwingUtilities2;

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
        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) OSMMapController.canvas.getComponent();
        final MutableTransformer layoutTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);

        ((JUNGCanvas) canvas).zoom((float) (1 / layoutTransformer.getScale()));
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
