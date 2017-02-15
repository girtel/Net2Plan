package com.net2plan.gui.utils.topologyPane.jung.osmSupport;

import java.awt.BorderLayout;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.*;

import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactory;

import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.ITopologyCanvas;

/**
 * @author Jorge San Emeterio
 * @date 03/11/2016
 */
public class OSMController
{
    private static OSMPanel mapViewer;

    private TopologyPanel topologyPanel;
    private ITopologyCanvas canvas;
    private IVisualizationCallback callback;

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
    public void startMap(final IVisualizationCallback callback, final TopologyPanel topologyPanel, final ITopologyCanvas canvas)
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
                canvas.stopOSMSupport();

                final String message = "Node: " + node.getName() + " is out of the accepted bounds.\n" +
                        "All nodes must have their coordinates between the ranges: \n" +
                        "x = [-180, 180]\n" +
                        "y = [-90, 90]\n";
                throw new OSMException(message);
            }
        }

        this.topologyPanel = topologyPanel;
        this.canvas = canvas;
        this.callback = callback;

        mapViewer = new OSMPanel();

        // Activating maps on the canvas
        loadMapOntoTopologyPanel();

        // Making the relation between the OSM map and the topology
        restartMap();
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

        final JPanel canvasPanel = topologyPanel.getCanvasPanel();

        canvasPanel.remove(canvas.getCanvasComponent());

        mapViewer.removeAll();
        mapViewer.add(canvas.getCanvasComponent());

        canvasPanel.add(mapViewer, BorderLayout.CENTER);

        mapViewer.validate();
        mapViewer.repaint();

        canvasPanel.validate();
        canvasPanel.repaint();

        topologyPanel.validate();
        topologyPanel.repaint();
    }

    /**
     * Restarts the map to its original state. The one that showed up when first starting the map support.
     */
    private void restartMap()
    {
        calculateMapPosition();
        alignTopologyToOSMMap();
    }

    /**
     * Recalculates the position of each node over the map, so that they are all positioned over their correspondent geo-positions.
     */
    public void refreshTopologyAlignment()
    {
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
        final Map<Node, GeoPosition> nodeToGeoPositionMap = netPlan.getNodes().stream().collect(Collectors.toMap(node->node, node -> new GeoPosition(node.getXYPositionMap().getY(), node.getXYPositionMap().getX())));

        final VisualizationState topologyVisualizationState = callback.getVisualizationState();

        // Getting interlayer distance for OSM coordinates
        canvas.updateInterLayerDistanceInNpCoordinates(topologyVisualizationState.getInterLayerSpaceInPixels());
        final double interLayerDistanceOSMCoordinates = canvas.getInterLayerDistanceInNpCoordinates();

        // Moving the nodes to the position dictated by their geoposition.
        for (Map.Entry<Node, GeoPosition> entry : nodeToGeoPositionMap.entrySet())
        {
            final Node node = entry.getKey();
            final GeoPosition geoPosition = entry.getValue();

            // The nodes' xy coordinates are not modified.
            final Point2D realPositionOSMCoordinates = mapViewer.getTileFactory().geoToPixel(geoPosition, mapViewer.getZoom());

            // Having in mind GUI Node stack
            final List<GUINode> guiNodes = topologyVisualizationState.getCanvasVerticallyStackedGUINodes(node);

            for (GUINode guiNode : guiNodes)
            {
                final double layerIndex = topologyVisualizationState.getCanvasVisualizationOrderRemovingNonVisible(guiNode.getLayer());

                // Moving GUI Nodes according to stack
                final Point2D guiNodePositionOSMCoordinates = new Point2D.Double(realPositionOSMCoordinates.getX(), realPositionOSMCoordinates.getY() - (layerIndex * interLayerDistanceOSMCoordinates)); // + = Downwards, - = Upwards
                canvas.moveVertexToXYPosition(guiNode, guiNodePositionOSMCoordinates);
            }
        }

        // Removing canvas zoom so that OSM coordinates and JUNG Coordinates align
        canvas.zoom(canvas.getCanvasCenter(), (float) (1 / canvas.getCurrentCanvasScale()));

        // Moving the canvas to the center of the map
        final Point2D q = mapViewer.getCenter();

        final Point2D aux = canvas.getCanvasPointFromNetPlanPoint(canvas.getCanvasCenter());
        final Point2D lvc = new Point2D.Double(aux.getX(), -aux.getY());

        final double dx = (lvc.getX() - q.getX());
        final double dy = (lvc.getY() - q.getY());

        canvas.moveCanvasTo(new Point2D.Double(dx, dy));

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
            canvas.zoom(canvas.getCanvasCenter(), (float) Math.pow(2, -zoomChange));
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

        final Point2D currentOSMCenterJUNG = canvas.getCanvasPointFromNetPlanPoint(new Point2D.Double(currentCenterX, currentCenterY));

        final double preCenterX = previousOSMViewportBounds.getCenterX();
        final double preCenterY = previousOSMViewportBounds.getCenterY();

        final Point2D previousOSMCenterJUNG = canvas.getCanvasPointFromNetPlanPoint(new Point2D.Double(preCenterX, preCenterY));

        final double dx = (currentOSMCenterJUNG.getX() - previousOSMCenterJUNG.getX());
        final double dy = (currentOSMCenterJUNG.getY() - previousOSMCenterJUNG.getY());

        if (dx != 0 || dy != 0)
        {
            canvas.moveCanvasTo(new Point2D.Double(-dx, dy));
        }

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

            final JPanel canvasPanel = topologyPanel.getCanvasPanel();
            // Then remove the OSM map from the topology panel.
            canvasPanel.remove(mapViewer);

            // Deleting the map component
            mapViewer = null;

            // Repaint canvas on the topology panel
            canvasPanel.add(canvas.getCanvasComponent(), BorderLayout.CENTER);

            // Reset nodes' original position
            canvas.updateAllVerticesXYPosition();
            canvas.zoomAll();

            canvasPanel.validate();
            canvasPanel.repaint();

            topologyPanel.validate();
            topologyPanel.repaint();
        }
    }

    /**
     * Restores the topology to its original state.
     */
    public void zoomAll()
    {
        if (canvas.isOSMRunning())
        {
            restartMap();
        } else
        {
            throw new OSMException("Map is currently deactivated");
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
        if (canvas.isOSMRunning())
        {
            final TileFactory tileFactory = mapViewer.getTileFactory();

            final Point2D mapCenter = mapViewer.getCenter();
            final Point2D newMapCenter = new Point2D.Double(mapCenter.getX() + dx, mapCenter.getY() + dy);

            mapViewer.setCenterPosition(tileFactory.pixelToGeo(newMapCenter, mapViewer.getZoom()));

            // Align the topology to the newly change OSM map.
            alignPanJUNGToOSMMap();
        } else
        {
            throw new OSMException("Map is currently deactivated");
        }
    }

    /**
     * Zooms the OSM map in and adapts the topology to its new state.
     */
    public void zoomIn()
    {
        if (canvas.isOSMRunning())
        {
            mapViewer.setZoom(mapViewer.getZoom() - 1);

            // Align the topology to the newly change OSM map.
            alignZoomJUNGToOSMMap();
        } else
        {
            throw new OSMException("Map is currently deactivated");
        }
    }

    /**
     * Zooms the OSM map out and adapts the topology to the new state.
     */
    public void zoomOut()
    {
        if (canvas.isOSMRunning())
        {
            mapViewer.setZoom(mapViewer.getZoom() + 1);

            // Align the topology to the newly change OSM map.
            alignZoomJUNGToOSMMap();
        } else
        {
            throw new OSMException("Map is currently deactivated");
        }
    }

    public JComponent getMapComponent()
    {
        return mapViewer;
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
