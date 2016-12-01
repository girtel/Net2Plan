package com.net2plan.gui.utils.topologyPane.mapControl.osm;

import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.components.mapPanel.OSMMapPanel;
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

    // Previous osmMap state
    private static Rectangle previousOSMViewportBounds;
    private static int previousZoomLevel;

    private static boolean isMapActivated = false;

    static
    {
        mapViewer = new OSMMapPanel();
    }

    // Non-instanciable
    private OSMMapController()
    {
    }

    /**
     * Starts and runs the osmMap to its original state.
     * This method should be executed when the osmMap is not yet loaded.
     *
     * @param topologyPanel The topology panel.
     * @param canvas        The JUNG canvas.
     * @param callback      The interface to the NetPlan.
     */
    public static void startMap(final TopologyPanel topologyPanel, final ITopologyCanvas canvas, final INetworkCallback callback)
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

        // If the osmMap is already running, stop it before reloading it.
        if (isMapActivated())
        {
            setMapState(false);
        }

        // Activating maps on the canvas
        loadMapOntoTopologyPanel();

        // Making the relation between the osmMap and the topology
        restartMapState();

        setMapState(true);
    }

    /**
     * Sets the swing component structure.
     */
    private static void loadMapOntoTopologyPanel()
    {
        // Making some swing adjustments.
        // Canvas on top of the osmMap panel.
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
     * Creates the starting state of the osmMap.
     * This state is the one where all nodes are seen and they all fit their corresponding position on the osmMap.
     * This method should only be executed when the osmMap is first run. From then on use {@link #centerMapToNodes()}
     */
    private static void restartMapState()
    {
        // Canvas components.
        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) OSMMapController.canvas.getComponent();
        final MutableTransformer layoutTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);

        final Map<Node, GeoPosition> nodeToGeoPositionMap = new HashMap<>();
        // Read xy coordinates of each node as latitude and longitude coordinates.
        for (Node node : callback.getDesign().getNodes())
        {
            final Point2D nodeXY = node.getXYPositionMap();

            final double latitude = nodeXY.getY();
            final double longitude = nodeXY.getX();

            final GeoPosition geoPosition = new GeoPosition(latitude, longitude);
            nodeToGeoPositionMap.put(node, geoPosition);
        }

        // Calculating osmMap center and zoom.
        mapViewer.zoomToBestFit(new HashSet<>(nodeToGeoPositionMap.values()), zoomRatio);

        // Moving the nodes to the position dictated by their geoposition.
        for (Map.Entry<Node, GeoPosition> entry : nodeToGeoPositionMap.entrySet())
        {
            final Node node = entry.getKey();
            final GeoPosition geoPosition = entry.getValue();

            // The nodes' xy coordinates are not modified.
            final Point2D realPosition = mapViewer.getTileFactory().geoToPixel(geoPosition, mapViewer.getZoom());
            ((JUNGCanvas) canvas).moveNodeToXYPosition(node, realPosition);
        }

        // The osmMap is now centered, now is time to fit the topology to the osmMap.
        // Center the topology.
        ((JUNGCanvas) canvas).zoomCanvas();

        // Removing the zoom all scale, so that the relation between the JUNG Canvas and the SWING Canvas is 1:1.
        ((JUNGCanvas) canvas).zoom((float) (1 / layoutTransformer.getScale()));

        // As the topology is centered at the same point as the osmMap, and the relation is 1:1 between their coordinates.
        // The nodes will be placed at the exact place as they are supposed to.

        previousOSMViewportBounds = mapViewer.getViewportBounds();
        previousZoomLevel = mapViewer.getZoom();

        // Refresh swing components.
        canvas.refresh();
        mapViewer.repaint();
    }

    private static void alignZoomJUNGToOSMMap()
    {
        final double zoomChange = mapViewer.getZoom() - previousZoomLevel;

        if (zoomChange != 0)
        {
            ((JUNGCanvas) canvas).zoom((float) Math.pow(2, -zoomChange));
        }

        previousZoomLevel = mapViewer.getZoom();
        previousOSMViewportBounds = mapViewer.getViewportBounds();

        canvas.refresh();
        mapViewer.repaint();
    }

    private static void alignPanJUNGToOSMMap()
    {
        final Rectangle currentOSMViewportBounds = mapViewer.getViewportBounds();

        final double currentCenterX = currentOSMViewportBounds.getCenterX();
        final double currentCenterY = currentOSMViewportBounds.getCenterY();

        final Point2D currentOSMCenterJUNG = ((JUNGCanvas) canvas).convertViewCoordinatesToRealCoordinates(new Point2D.Double(currentCenterX, currentCenterY));

        final double preCenterX = previousOSMViewportBounds.getCenterX();
        final double preCenterY = previousOSMViewportBounds.getCenterY();

        final Point2D previousOSMCenterJUNG = ((JUNGCanvas) canvas).convertViewCoordinatesToRealCoordinates(new Point2D.Double(preCenterX, preCenterY));

        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) OSMMapController.canvas.getComponent();
        final MutableTransformer layoutTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);

        final double dx = (currentOSMCenterJUNG.getX() - previousOSMCenterJUNG.getX());
        final double dy = (currentOSMCenterJUNG.getY() - previousOSMCenterJUNG.getY());

        if (dx != 0 || dy != 0)
            layoutTransformer.translate(-dx, dy);

        previousZoomLevel = mapViewer.getZoom();
        previousOSMViewportBounds = currentOSMViewportBounds;

        canvas.refresh();
        mapViewer.repaint();
    }

    /**
     * Returns the swing component to the state they were before activating the osmMap.
     */
    private static void cleanMap()
    {
        // First, remove any canvas from the top of the osmMap viewer.
        mapViewer.removeAll();
        mapViewer.validate();
        mapViewer.repaint();

        // Then remove the osmMap from the topology panel.
        topologyPanel.remove(mapViewer);

        // Repaint canvas on the topology panel
        topologyPanel.add(canvas.getComponent(), BorderLayout.CENTER);

        topologyPanel.validate();
        topologyPanel.repaint();
    }

    /**
     * Restores the topology to its original state.
     */
    public static void centerMapToNodes()
    {
        if (isMapActivated())
        {
            restartMapState();
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    /**
     * Moves the osmMap center a given amount of pixels.
     *
     * @param dx Moves osmMap dx pixels over the X axis.
     * @param dy Moves osmMap dy pixels over the Y axis.
     */
    public static void moveMap(final double dx, final double dy)
    {
        if (isMapActivated())
        {
            final TileFactory tileFactory = mapViewer.getTileFactory();

            final Point2D mapCenter = mapViewer.getCenter();
            final Point2D newMapCenter = new Point2D.Double(mapCenter.getX() + dx, mapCenter.getY() + dy);

            mapViewer.setCenterPosition(tileFactory.pixelToGeo(newMapCenter, mapViewer.getZoom()));

            // Align the topology to the newly change osmMap.
            alignPanJUNGToOSMMap();

            canvas.refresh();
            mapViewer.repaint();
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    /**
     * Zooms the osmMap in and adapts the topology to its new state.
     */
    public static void zoomIn()
    {
        if (isMapActivated())
        {
            mapViewer.setZoom(mapViewer.getZoom() - 1);

            // Align the topology to the newly change osmMap.
            alignZoomJUNGToOSMMap();
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    /**
     * Zooms the osmMap out and adapts the topology to the new state.
     */
    public static void zoomOut()
    {
        if (isMapActivated())
        {
            mapViewer.setZoom(mapViewer.getZoom() + 1);

            // Align the topology to the newly change osmMap.
            alignZoomJUNGToOSMMap();
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    /**
     * Gets wether the osmMap component is activated or not.
     *
     * @return Map activation state.
     */
    public static boolean isMapActivated()
    {
        return isMapActivated;
    }

    public static void disableMapSupport()
    {
        if (isMapActivated())
        {
            setMapState(false);
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    /**
     * Activates or deactivates the osmMap component.
     *
     * @param state Map activation state.
     */
    private static void setMapState(final boolean state)
    {
        // Activates the osmMap so that the JUNG Canvas changes its behaviour.
        isMapActivated = state;

        if (!isMapActivated())
        {
            cleanMap();
        }
    }

    public static GeoPosition convertPointToGeo(final Point2D point)
    {
        return mapViewer.getTileFactory().pixelToGeo(point, mapViewer.getZoom());
    }

    private static class OSMMapException extends Net2PlanException
    {
        public OSMMapException(final String message)
        {
            ErrorHandling.showErrorDialog(message, "Could not display osmMap");
        }
    }
}
