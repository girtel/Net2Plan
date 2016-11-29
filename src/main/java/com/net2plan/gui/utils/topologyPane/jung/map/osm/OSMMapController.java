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

    /**
     * Starts and runs the map to its original state.
     * This method should be executed when the map is not yet loaded.
     * @param topologyPanel The topology panel.
     * @param canvas The JUNG canvas.
     * @param callback The interface to the NetPlan.
     */
    public static void runMap(final TopologyPanel topologyPanel, final ITopologyCanvas canvas, final INetworkCallback callback)
    {
        // Checking if the nodes are valid for this operation.
        // They may not go outside the bounds: x: -180, 180: y: -90, 90
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

        // Making the relation between the map and the topology
        fitTopologyAndMap();

        setMapState(true);
    }

    /**
     * Sets the swing component structure.
     */
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

    /**
     * Creates the starting state of the map.
     * This state is the one where all nodes are seen and they all fit their corresponding position on the map.
     * This method should only be executed when the map is first run. From then on use {@link #centerMapToNodes()}
     */
    private static void fitTopologyAndMap()
    {
        // Calculating each node geoposition.
        buildNodeGeoPositionMap();

        // Calculating map center and zoom.
        mapViewer.zoomToBestFit(new HashSet<>(nodeToGeoPositionMap.values()), zoomRatio);

        // Moving the nodes to the position dictated by their geoposition.
        for (Map.Entry<Node, GeoPosition> entry : nodeToGeoPositionMap.entrySet())
        {
            final Node node = entry.getKey();
            final GeoPosition geoPosition = entry.getValue();

            // The nodes' xy coordinates are not modified.
            final Point2D realPosition = mapViewer.getTileFactory().geoToPixel(geoPosition, mapViewer.getZoom());
            ((JUNGCanvas) canvas).moveNode(node, realPosition);
        }

        // The map is now centered, now is time to fit the topology to the map.
        // Center the topology.
        topologyPanel.zoomAll();

        // Removing the zoom all scale, so that the relation between the JUNG Canvas and the SWING Canvas is 1:1.
        removeTopologyZoom();

        // As the topology is centered at the same point as the map, and the relation is 1:1 between their coordinates.
        // The nodes will be placed at the exact place as they are supposed to.

        // Refresh swing components.
        canvas.refresh();
        mapViewer.repaint();
    }

    /**
     * Similar to {@link #fitTopologyAndMap()} but only acts on the topology itself. This one does not change the map at all.
     */
    public static void refitMap()
    {
        // Moving the nodes to the positions given by their GeoPositions.
        for (Map.Entry<Node, GeoPosition> entry : nodeToGeoPositionMap.entrySet())
        {
            final Node node = entry.getKey();
            final GeoPosition geoPosition = entry.getValue();

            // The position that the node really takes on the map. This position depends on the zoom that the map currently has.
            final Point2D realPosition = mapViewer.getTileFactory().geoToPixel(geoPosition, mapViewer.getZoom());
            ((JUNGCanvas) canvas).moveNode(node, realPosition);
        }

        // The map is now centered to the topology, we now center the topology to the map.
        ((JUNGCanvas) canvas).zoomCanvas();
        removeTopologyZoom();

        canvas.refresh();
        mapViewer.repaint();
    }

    /**
     * Deactivates the map and males it dissapear from the topology.
     */
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

    /**
     * Restores the topology to its original state having in mind the current state of the map.
     */
    public static void centerMapToNodes()
    {
        if (isMapActivated())
        {
            // Restore the map to its original state.
            mapViewer.zoomToBestFit(new HashSet<>(nodeToGeoPositionMap.values()), zoomRatio);

            // Now that the map has been restored, lets do the same with the topology.
            refitMap();
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    /**
     * Moves the map center a given amount of pixels.
     * @param dx Moves map dx pixels over the X axis.
     * @param dy Moves map dy pixels over the Y axis.
     */
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

    /**
     * Zooms the map in and adapts the topology to its new state.
     */
    public static void zoomIn()
    {
        if (isMapActivated())
        {
            // Save current map state
            final Point2D beforeChangeMapCenter = mapViewer.getCenter();
            final int beforeChangeZoom = mapViewer.getZoom();

            // Return to the original state. We know that in this state the topology is always correct, so we use it as reference.
            centerMapToNodes();

            // As we have returned to the original state, decrease the zoom to the one we had previously save.
            mapViewer.setZoom(beforeChangeZoom - 1);

            // Resize the map so that the nodes take the new zoom.
            refitMap();

            // Take the zoom from the current state
            final int originalStateZoom = mapViewer.getZoom();

            // The map is currently centered at the original position, we must move it and the topology to the one the user set.
            // For the zoom in, the points in the map coordinates multiply by 2 with each point.
            // ((beforeChangeZoom - originalStateZoom) + 1) gets the multiplier for the map coordinates depending on the level of zoom using the original zoom as a reference.
            ((JUNGCanvas) canvas).panTo(new Point2D.Double(beforeChangeMapCenter.getX() * ((beforeChangeZoom - originalStateZoom) + 1), beforeChangeMapCenter.getY() * ((beforeChangeZoom - originalStateZoom) + 1)), mapViewer.getCenter());
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    /**
     * Zooms the map out and adapts the topology to the new state.
     */
    public static void zoomOut()
    {
        if (isMapActivated())
        {
            // Save current map state
            final Point2D beforeChangeMapCenter = mapViewer.getCenter();
            final int beforeChangeZoom = mapViewer.getZoom();

            // Return to the original state. We know that in this state the topology is always correct, so we use it as reference.
            centerMapToNodes();

            // As we have returned to the original state, decrease the zoom to the one we had previously save.
            mapViewer.setZoom(beforeChangeZoom + 1);

            // Resize the map so that the nodes take the new zoom.
            refitMap();

            // Take the zoom from the current state
            final int originalStateZoom = mapViewer.getZoom();

            // The map is currently centered at the original position, we must move it and the topology to the one the user set.
            // For the zoom in, the points in the map coordinates divided by 2 with each point.
            // ((beforeChangeZoom - originalStateZoom) + 1) gets the multiplier for the map coordinates depending on the level of zoom using the original zoom as a reference.
            ((JUNGCanvas) canvas).panTo(new Point2D.Double(beforeChangeMapCenter.getX() / ((originalStateZoom - beforeChangeZoom) + 1), beforeChangeMapCenter.getY() / ((originalStateZoom - beforeChangeZoom) + 1)), mapViewer.getCenter());
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    /**
     * Reads each node xy coordinates and creates the geoposition map.
     */
    private static void buildNodeGeoPositionMap()
    {
        // Read xy coordinates of each node as latitude and longitude coordinates.
        for (Node node : callback.getDesign().getNodes())
        {
            final Point2D nodeXY = node.getXYPositionMap();

            final double latitude = nodeXY.getY();
            final double longitude = nodeXY.getX();

            final GeoPosition geoPosition = new GeoPosition(latitude, longitude);
            nodeToGeoPositionMap.put(node, geoPosition);
        }
    }

    /**
     * Removes the canvas scale value so that the relation between the JUNG canvas and the map is 1:1
     */
    private static void removeTopologyZoom()
    {
        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) OSMMapController.canvas.getComponent();
        final MutableTransformer layoutTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);

        // Removing zoom for a 1:1 scale.
        ((JUNGCanvas) canvas).zoom((float) (1 / layoutTransformer.getScale()));
    }

    /**
     * Gets wether the map component is activated or not.
     * @return Map activation state.
     */
    public static boolean isMapActivated()
    {
        return isMapActivated;
    }

    /**
     * Activates or deactivates the map component.
     * @param state Map activation state.
     */
    public static void setMapState(final boolean state)
    {
        // Activates the map so that the JUNG Canvas changes its behaviour.
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
