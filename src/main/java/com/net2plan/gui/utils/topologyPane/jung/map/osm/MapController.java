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

    private static final String ATTRIB_LATITUDE = "lat";
    private static final String ATTRIB_LONGITUDE = "lon";

    static
    {
        mapViewer = new MapPanel();
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
        loadMap();

//        JOptionPane.showMessageDialog(null, "TODO: Click");
//
//        // 2nd step: Loading the snapshot
//        loadSnapshot();
    }

    private static void loadMap()
    {
        // JUNG Canvas
        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) canvas.getComponent();

        // Making some swing adjustments.
        final LayoutManager layout = new OverlayLayout(mapViewer);
        mapViewer.setLayout(layout);

        mapViewer.add(vv);

        topologyPanel.add(mapViewer, BorderLayout.CENTER);

        topologyPanel.validate();
        topologyPanel.repaint();

        final HashSet<GeoPosition> positionSet = new HashSet<>();

        // Moving the nodes
        for (Node node : callback.getDesign().getNodes())
        {
            // Getting coords from nodes attributes
            final double latitude = Double.parseDouble(node.getAttribute(ATTRIB_LATITUDE));
            final double longitude = Double.parseDouble(node.getAttribute(ATTRIB_LONGITUDE));

            final GeoPosition geoPosition = new GeoPosition(latitude, longitude);
            positionSet.add(geoPosition);

            // The position that the node really takes on the map. This is the point where the map and the nodes align.
            final Point2D realPosition = mapViewer.getTileFactory().geoToPixel(geoPosition, mapViewer.getZoom());
            callback.moveNode(node.getId(), new Point2D.Double(realPosition.getX(), -realPosition.getY()));
        }

        mapViewer.zoomToBestFit(positionSet, 0.6);
    }

    private static void loadSnapshot()
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
}
