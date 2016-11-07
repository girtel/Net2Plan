package com.net2plan.gui.utils.topologyPane.jung.map.osm;

import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.jung.JUNGCanvas;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.ITopologyCanvas;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import org.jxmapviewer.viewer.GeoPosition;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Jorge San Emeterio on 03/11/2016.
 */
public class MapController
{
    private static final MapPanel mapViewer;

    private static TopologyPanel topologyPanel;
    private static ITopologyCanvas canvas;
    private static INetworkCallback callback;

    private static Map<Long, Point2D> nodeMap;

    private static final String ATTRIB_LATITUDE = "lat";
    private static final String ATTRIB_LONGITUDE = "lon";

    static
    {
        mapViewer = new MapPanel();
        nodeMap = new HashMap<>();
    }

    // Non-instanciable
    private MapController()
    {
    }

    public static void runMap(final TopologyPanel topologyPanel, final ITopologyCanvas canvas, final INetworkCallback callback)
    {
        MapController.topologyPanel = topologyPanel;
        MapController.canvas = canvas;
        MapController.callback = callback;

        // 1st step: Loading the map
        // HACK: Why the hell does it need to do the same thing multiple times in order to work?
        for (int i = 0; i < 4; i++)
        {
            loadMap();
        }

        JOptionPane.showMessageDialog(null, "TODO: Click");

        // 2nd step: Loading the snapshot
        loadSnapshot();
    }

    private static void loadMap()
    {
        topologyPanel.zoomAll();

        // JUNG Canvas
        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) canvas.getComponent();

        // Getting viewport rectangle
        final Rectangle viewInLayoutUnits = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(vv.getBounds()).getBounds();

        // Viewport center point.
        final Point2D centerPoint = new Point.Double(viewInLayoutUnits.getCenterX(), viewInLayoutUnits.getCenterY());
        final GeoPosition position = new GeoPosition(-centerPoint.getY(), centerPoint.getX());

        mapViewer.setCenterPosition(position);

        final Set<GeoPosition> positionSet = new HashSet<>();

        for (Node node : callback.getDesign().getNodes())
        {
            // Transforming N2P coords to geo position.
            // final GeoPosition geoPosition = new GeoPosition(node.getXYPositionMap().getY(), node.getXYPositionMap().getX());

            // Getting coords from nodes attributes 
            final double latitude = Double.parseDouble(node.getAttribute(ATTRIB_LATITUDE));
            final double longitude = Double.parseDouble(node.getAttribute(ATTRIB_LONGITUDE));

            final GeoPosition geoPosition = new GeoPosition(latitude, longitude);

            positionSet.add(geoPosition);

            // The position that the node really takes on the map. This is the point where the map and the nodes align.
            final Point2D realPosition = mapViewer.getTileFactory().geoToPixel(geoPosition, mapViewer.getZoom());
            nodeMap.put(node.getId(), new Point2D.Double(realPosition.getX(), -realPosition.getY()));
            //callback.moveNode(node.getId(), realPosition);
        }

        mapViewer.zoomToBestFit(positionSet, 0.7);

        topologyPanel.remove(vv);
        mapViewer.setLayout(new BorderLayout());
        mapViewer.add(vv, BorderLayout.CENTER);
        topologyPanel.add(mapViewer, BorderLayout.CENTER);

        topologyPanel.validate();
        topologyPanel.repaint();
    }

    private static void loadSnapshot()
    {
        for (Node node : callback.getDesign().getNodes())
        {
            callback.moveNode(node.getId(), nodeMap.get(node.getId()));
        }

        topologyPanel.zoomAll();

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
}
