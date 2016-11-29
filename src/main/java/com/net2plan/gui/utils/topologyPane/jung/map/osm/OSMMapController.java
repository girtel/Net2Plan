package com.net2plan.gui.utils.topologyPane.jung.map.osm;

import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.jung.JUNGCanvas;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.plugins.ITopologyCanvas;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import org.jxmapviewer.viewer.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
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

    private static final double zoomRatio = 0.6;

    private static boolean isMapActivated = false;
    private static Map<Node, GeoPosition> nodeToGeoPositionMap;

    static
    {
        mapViewer = new OSMMapPanel();
        nodeToGeoPositionMap = new HashMap<>();
    }

    // Non-instanciable
    private OSMMapController()
    {
    }

    public static void runMap(final TopologyPanel topologyPanel, final ITopologyCanvas canvas, final INetworkCallback callback)
    {
        // Checking if the nodes are valid for this operation.
        // They may not go outside the bounds: -180, 180: -90, 90
        for (Node node : callback.getDesign().getNodes())
        {
            final Point2D nodeXY = node.getXYPositionMap();

            final double x = nodeXY.getX();
            final double y = nodeXY.getY();

            if ((x > 180 || x < -180) || (y > 90 || y < -90))
            {
                final StringBuilder builder = new StringBuilder();
                builder.append("Node: " + node.getName() + " is out of the accepted bounds.\n");
                builder.append("All nodes must have their coordinates between the ranges: \n");
                builder.append("x = [-180, 180]\n");
                builder.append("y = [-90, 90]\n");

                throw new OSMMapException(builder.toString());
            }
        }

        OSMMapController.topologyPanel = topologyPanel;
        OSMMapController.canvas = canvas;
        OSMMapController.callback = callback;

        // If the map is already running, stop it before reloading it.
        if (isMapActivated())
        {
            setMapState(false);
        }

        // Activating maps on the canvas
        loadMapOntoTopologyPanel();

        // Making a relation between the map and the topology
        fitTopologyAndMap();

        setMapState(true);
    }

    public static void reloadMap()
    {
        // Calculating each node geoposition.
        buildNodeGeoPositionMap();

        final Point2D mapCenter = mapViewer.getCenter();

        // Moving nodes
        for (Map.Entry<Node, GeoPosition> entry : nodeToGeoPositionMap.entrySet())
        {
            final Node node = entry.getKey();
            final GeoPosition geoPosition = entry.getValue();

            // The position that the node really takes on the map.
            final Point2D realPosition = mapViewer.getTileFactory().geoToPixel(geoPosition, mapViewer.getZoom());
            ((JUNGCanvas) canvas).moveNode(node, new Point2D.Double(realPosition.getX(), realPosition.getY()));
        }

        // The map is now centered to the topology, we now center the topology to the map.
        ((JUNGCanvas) canvas).zoomCanvas();

        removeTopologyZoom();

        canvas.refresh();
        mapViewer.repaint();
    }

    private static void loadMapOntoTopologyPanel()
    {
        // Making some swing adjustments.
        // Canvas on top of the map panel.
        final LayoutManager layout = new OverlayLayout(mapViewer);
        mapViewer.setLayout(layout);

        topologyPanel.remove(canvas.getComponent());

        mapViewer.removeAll();
        mapViewer.add(canvas.getComponent());

        topologyPanel.add(mapViewer, BorderLayout.CENTER);

        mapViewer.validate();
        mapViewer.repaint();

        topologyPanel.validate();
        topologyPanel.repaint();
    }

    private static void fitTopologyAndMap()
    {
        // Calculating each node geoposition.
        buildNodeGeoPositionMap();

        // Calculating map center and zoom.
        mapViewer.zoomToBestFit(new HashSet<>(nodeToGeoPositionMap.values()), zoomRatio);

        // Moving nodes
        for (Map.Entry<Node, GeoPosition> entry : nodeToGeoPositionMap.entrySet())
        {
            final Node node = entry.getKey();
            final GeoPosition geoPosition = entry.getValue();

            // The position that the node really takes on the map.
            final Point2D realPosition = mapViewer.getTileFactory().geoToPixel(geoPosition, mapViewer.getZoom());
            ((JUNGCanvas) canvas).moveNode(node, new Point2D.Double(realPosition.getX(), realPosition.getY()));
        }

        // The map is now centered to the topology, we now center the topology to the map.
        topologyPanel.zoomAll();

        removeTopologyZoom();

        canvas.refresh();
        mapViewer.repaint();
    }

    private static void stopMap()
    {
        // First, remove any canvas from the top of the map viewer.
        mapViewer.removeAll();
        mapViewer.validate();
        mapViewer.repaint();

        // Then remove the map from the topology panel.
        topologyPanel.remove(mapViewer);

        // Repaint canvas on the topology panel
        topologyPanel.add(canvas.getComponent(), BorderLayout.CENTER);

        topologyPanel.validate();
        topologyPanel.repaint();
    }

    public static void centerMapToNodes()
    {
        if (isMapActivated())
        {
            mapViewer.zoomToBestFit(new HashSet<>(nodeToGeoPositionMap.values()), zoomRatio);

            reloadMap();
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    public static void moveMap(final double dx, final double dy)
    {
        if (isMapActivated())
        {
            final TileFactory tileFactory = mapViewer.getTileFactory();

            final Point2D mapCenter = mapViewer.getCenter();
            final Point2D newMapCenter = new Point2D.Double(mapCenter.getX() + dx, mapCenter.getY() + dy);

            mapViewer.setCenterPosition(tileFactory.pixelToGeo(newMapCenter, mapViewer.getZoom()));

            canvas.refresh();
            mapViewer.repaint();
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    public static void zoomIn()
    {
        if (isMapActivated())
        {
            final Point2D currentMapCenter = mapViewer.getCenter();
            final int currentZoom = mapViewer.getZoom();

            centerMapToNodes();

            mapViewer.setZoom(currentZoom - 1);

            reloadMap();

            final int newZoom = mapViewer.getZoom();

            ((JUNGCanvas) canvas).panTo(new Point2D.Double(currentMapCenter.getX() / ((newZoom - currentZoom) + 1), currentMapCenter.getY() / ((newZoom - currentZoom) + 1)), mapViewer.getCenter());
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    public static void zoomOut()
    {
        if (isMapActivated())
        {
            final Point2D currentMapCenter = mapViewer.getCenter();
            final int currentZoom = mapViewer.getZoom();

            centerMapToNodes();

            mapViewer.setZoom(currentZoom + 1);

            reloadMap();

            final int newZoom = mapViewer.getZoom();

            ((JUNGCanvas) canvas).panTo(new Point2D.Double(currentMapCenter.getX() / ((newZoom - currentZoom) + 1), currentMapCenter.getY() / ((newZoom - currentZoom) + 1)), mapViewer.getCenter());
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    private static void buildNodeGeoPositionMap()
    {
        for (Node node : callback.getDesign().getNodes())
        {
            final Point2D nodeXY = node.getXYPositionMap();

            final double latitude = nodeXY.getY();
            final double longitude = nodeXY.getX();

            final GeoPosition geoPosition = new GeoPosition(latitude, longitude);
            nodeToGeoPositionMap.put(node, geoPosition);
        }
    }

    private static void removeTopologyZoom()
    {
        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) OSMMapController.canvas.getComponent();
        final MutableTransformer layoutTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);

        // Removing zoom for a 1:1 scale.
        ((JUNGCanvas) canvas).zoom((float) (1 / layoutTransformer.getScale()));
    }

    public static boolean isMapActivated()
    {
        return isMapActivated;
    }

    public static void setMapState(final boolean state)
    {
        isMapActivated = state;

        if (!isMapActivated())
        {
            stopMap();
        }
    }

    private static class OSMMapException extends Net2PlanException
    {
        public OSMMapException(final String message)
        {
            ErrorHandling.showErrorDialog(message, "Could not display map");
        }
    }
}
