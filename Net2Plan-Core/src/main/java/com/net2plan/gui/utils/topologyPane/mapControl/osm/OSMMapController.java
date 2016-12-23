package com.net2plan.gui.utils.topologyPane.mapControl.osm;

import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.components.mapPanel.OSMMapPanel;
import com.net2plan.gui.utils.topologyPane.jung.JUNGCanvas;
import com.net2plan.gui.utils.topologyPane.mapControl.osm.state.OSMMapStateBuilder;
import com.net2plan.gui.utils.topologyPane.mapControl.osm.state.OSMRunningState;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.plugins.ITopologyCanvas;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import org.jxmapviewer.viewer.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jorge San Emeterio
 * @date 03/11/2016
 */
public class OSMMapController
{
    private static OSMMapPanel mapViewer;

    private TopologyPanel topologyPanel;
    private ITopologyCanvas canvas;
    private INetworkCallback callback;

    // Previous OSM map state
    private Rectangle previousOSMViewportBounds;
    private int previousZoomLevel;

    /**
     * Starts and runs the OSM map to its original state.
     * This method should be executed when the OSM map is not yet loaded.
     *
     * @param topologyPanel The topology panel.
     * @param canvas        The JUNG canvas.
     * @param callback      The interface to the NetPlan.
     */
    public void startMap(final TopologyPanel topologyPanel, final ITopologyCanvas canvas, final INetworkCallback callback)
    {
        // Checking if the nodes are valid for this operation.
        // They may not go outside the bounds: x: -180, 180: y: -90, 90
        for (Node node : callback.getDesign().getNodes())
        {
            final Point2D nodeXY = node.getXYPositionMap();

            final double x = nodeXY.getX();
            final double y = nodeXY.getY();

            if (!OSMMapUtils.isInsideBounds(x, y))
            {
                String message = "Node: " + node.getName() + " is out of the accepted bounds.\n" +
                        "All nodes must have their coordinates between the ranges: \n" +
                        "x = [-180, 180]\n" +
                        "y = [-90, 90]\n";

                OSMMapStateBuilder.getSingleton().setStoppedState();

                throw new OSMMapException(message);
            }
        }

        this.topologyPanel = topologyPanel;
        this.canvas = canvas;
        this.callback = callback;

        mapViewer = new OSMMapPanel();

        // Activating maps on the canvas
        loadMapOntoTopologyPanel();

        // Making the relation between the OSM map and the topology
        restartMapState(true);
    }

    /**
     * Sets the swing component structure.
     */
    private void loadMapOntoTopologyPanel()
    {
        // Making some swing adjustments.
        // Canvas on top of the OSM map panel.
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
     * Creates the starting state of the OSM map.
     * This state is the one where all nodes are seen and they all fit their corresponding position on the OSM map.
     * This method should only be executed when the OSM map is first run. From then on use {@link #zoomAll()}
     */
    public void restartMapState(final boolean calculateMap)
    {
        if (calculateMap) calculateMapPosition();
        alignTopologyToOSMMap();
    }

    /**
     * Calculates the correct map center and zoom so that all nodes are visible.
     */
    private void calculateMapPosition()
    {
        final NetPlan netPlan = callback.getDesign();
        final double zoomRatio = 0.6;

        // Read xy coordinates of each node as latitude and longitude coordinates.
        final Map<Long, GeoPosition> nodeToGeoPositionMap = netPlan.getNodes().stream().collect(Collectors.toMap(Node::getId, node -> new GeoPosition(node.getXYPositionMap().getY(), node.getXYPositionMap().getX())));

        // Calculating OSM map center and zoom.
        mapViewer.zoomToBestFit(nodeToGeoPositionMap.isEmpty() ? Collections.singleton(mapViewer.getDefaultPosition()) : new HashSet<>(nodeToGeoPositionMap.values()), zoomRatio);
        if (netPlan.getNumberOfNodes() <= 1) mapViewer.setZoom(16); // So that the map is not too close to the node.
    }

    /**
     * Aligns the topology with the map so that the nodes are placed in their corresponding geoposition.
     * For every problem regarding the nodes not correctly aligning to the map, call this function.
     */
    private void alignTopologyToOSMMap()
    {
        final NetPlan netPlan = callback.getDesign();
        final Map<Long, GeoPosition> nodeToGeoPositionMap = netPlan.getNodes().stream().collect(Collectors.toMap(Node::getId, node -> new GeoPosition(node.getXYPositionMap().getY(), node.getXYPositionMap().getX())));

        // Moving the nodes to the position dictated by their geoposition.
        for (Map.Entry<Long, GeoPosition> entry : nodeToGeoPositionMap.entrySet())
        {
            final Node node = callback.getDesign().getNodeFromId(entry.getKey());
            final GeoPosition geoPosition = entry.getValue();

            // The nodes' xy coordinates are not modified.
            final Point2D realPosition = mapViewer.getTileFactory().geoToPixel(geoPosition, mapViewer.getZoom());
            canvas.moveNodeToXYPosition(node, realPosition);
        }

        @SuppressWarnings("unchecked")
        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) canvas.getComponent();
        final MutableTransformer layoutTransformer = ((JUNGCanvas) canvas).getTransformer();

        /* Rescale and pan JUNG layout so that it fits to OSM viewing */
        ((JUNGCanvas) canvas).zoom((float) (1 / layoutTransformer.getScale()));

        Point2D q = mapViewer.getCenter();
        Point2D lvc = layoutTransformer.inverseTransform(vv.getCenter());

        double dx = (lvc.getX() - q.getX());
        double dy = (lvc.getY() - q.getY());

        layoutTransformer.translate(dx, dy);

        previousOSMViewportBounds = mapViewer.getViewportBounds();
        previousZoomLevel = mapViewer.getZoom();

        // Refresh swing components.
        canvas.refresh();
        mapViewer.repaint();
    }

    /**
     * Aligns the topology to the map's zoom.
     */
    private void alignZoomJUNGToOSMMap()
    {
        final double zoomChange = mapViewer.getZoom() - previousZoomLevel;

        if (zoomChange != 0)
        {
            ((JUNGCanvas) canvas).zoom((float) Math.pow(2, -zoomChange));
        }

        previousZoomLevel = mapViewer.getZoom();
        previousOSMViewportBounds = mapViewer.getViewportBounds();

        alignTopologyToOSMMap();
    }

    /**
     * Aligns the topology to the map's pan.
     */
    private void alignPanJUNGToOSMMap()
    {
        final Rectangle currentOSMViewportBounds = mapViewer.getViewportBounds();

        final double currentCenterX = currentOSMViewportBounds.getCenterX();
        final double currentCenterY = currentOSMViewportBounds.getCenterY();

        final Point2D currentOSMCenterJUNG = canvas.convertViewCoordinatesToRealCoordinates(new Point2D.Double(currentCenterX, currentCenterY));

        final double preCenterX = previousOSMViewportBounds.getCenterX();
        final double preCenterY = previousOSMViewportBounds.getCenterY();

        final Point2D previousOSMCenterJUNG = canvas.convertViewCoordinatesToRealCoordinates(new Point2D.Double(preCenterX, preCenterY));

        final MutableTransformer layoutTransformer = ((JUNGCanvas) canvas).getTransformer();

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
     * Returns the swing component to the state they were before activating the OSM map.
     */
    public void cleanMap()
    {
        if (mapViewer != null)
        {
            // First, remove any canvas from the top of the OSM map viewer.
            mapViewer.removeAll();

            // Then remove the OSM map from the topology panel.
            topologyPanel.remove(mapViewer);

            // Deleting the map component
            mapViewer = null;

            // Repaint canvas on the topology panel
            topologyPanel.add(canvas.getComponent(), BorderLayout.CENTER);

            // Reset nodes' original position
            for (Node node : callback.getDesign().getNodes())
            {
                canvas.updateNodeXYPosition(node);
                canvas.zoomAll();
            }

            topologyPanel.validate();
            topologyPanel.repaint();
        }
    }

    /**
     * Restores the topology to its original state.
     */
    public void zoomAll()
    {
        if (isMapActivated())
        {
            restartMapState(true);
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    /**
     * Moves the OSM map center a given amount of pixels.
     *
     * @param dx Moves OSM map dx pixels over the X axis.
     * @param dy Moves OSM map dy pixels over the Y axis.
     */
    public void moveMap(final double dx, final double dy)
    {
        if (isMapActivated())
        {
            final TileFactory tileFactory = mapViewer.getTileFactory();

            final Point2D mapCenter = mapViewer.getCenter();
            final Point2D newMapCenter = new Point2D.Double(mapCenter.getX() + dx, mapCenter.getY() + dy);

            mapViewer.setCenterPosition(tileFactory.pixelToGeo(newMapCenter, mapViewer.getZoom()));

            // Align the topology to the newly change OSM map.
            alignPanJUNGToOSMMap();
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    /**
     * Zooms the OSM map in and adapts the topology to its new state.
     */
    public void zoomIn()
    {
        if (isMapActivated())
        {
            mapViewer.setZoom(mapViewer.getZoom() - 1);

            // Align the topology to the newly change OSM map.
            alignZoomJUNGToOSMMap();
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    /**
     * Zooms the OSM map out and adapts the topology to the new state.
     */
    public void zoomOut()
    {
        if (isMapActivated())
        {
            mapViewer.setZoom(mapViewer.getZoom() + 1);

            // Align the topology to the newly change OSM map.
            alignZoomJUNGToOSMMap();
        } else
        {
            throw new OSMMapException("Map is currently deactivated");
        }
    }

    /**
     * Gets whether the OSM map component is activated or not.
     *
     * @return Map activation state.
     */
    private boolean isMapActivated()
    {
        return OSMMapStateBuilder.getSingleton().getCurrentState() instanceof OSMRunningState;
    }

    public JComponent getMapComponent()
    {
        return mapViewer;
    }

    public static class OSMMapException extends Net2PlanException
    {
        public OSMMapException(final String message)
        {
            ErrorHandling.showErrorDialog(message, "Could not display OSM Map");
        }

        public OSMMapException(final String message, final String title)
        {
            ErrorHandling.showErrorDialog(message, title);
        }
    }

    public static class OSMMapUtils
    {
        public static GeoPosition convertPointToGeo(final Point2D point)
        {
            // Pixel to geo must be calculated at the zoom level where canvas and map align.
            // That zoom level is the one given by the restore map method.
            return mapViewer.getTileFactory().pixelToGeo(point, mapViewer.getZoom());
        }

        public static Point2D convertGeoToPoint(final GeoPosition geoPosition)
        {
            return mapViewer.getTileFactory().geoToPixel(geoPosition, mapViewer.getZoom());
        }

        public static boolean isInsideBounds(final double x, final double y)
        {
            return !((x > 180 || x < -180) || (y > 90 || y < -90));
        }
    }
}
