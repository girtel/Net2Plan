package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.net2plan.gui.utils.FileChooserConfirmOverwrite;
import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.VisualizationConstants;
import com.net2plan.gui.utils.topologyPane.VisualizationState;
import com.net2plan.gui.utils.topologyPane.jung.JUNGCanvas;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants;
import com.net2plan.internal.plugins.ITopologyCanvas;
import com.net2plan.utils.ImageUtils;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
class OSMOffState implements OSMState
{
    private final IVisualizationCallback callback;
    private final ITopologyCanvas canvas;
    private final VisualizationViewer<GUINode, GUILink> vv;

    @SuppressWarnings("unchecked")
    OSMOffState(final IVisualizationCallback callback, final ITopologyCanvas canvas)
    {
        this.callback = callback;
        this.canvas = canvas;

        this.vv = (VisualizationViewer<GUINode, GUILink>) canvas.getCanvasComponent();
    }

    @Override
    public void panTo(Point2D initialPoint, Point2D currentPoint)
    {
        final MutableTransformer layoutTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
        final Point2D q = layoutTransformer.inverseTransform(initialPoint);
        final Point2D lvc = layoutTransformer.inverseTransform(currentPoint);
        final double dxJungCoord = (lvc.getX() - q.getX());
        final double dyJungCoord = (lvc.getY() - q.getY());

        canvas.moveCanvasTo(new Point2D.Double(dxJungCoord, dyJungCoord));
    }

    @Override
    public void zoomIn()
    {
        canvas.zoom(canvas.getCanvasCenter(), VisualizationConstants.SCALE_IN);
    }

    @Override
    public void zoomOut()
    {
        canvas.zoom(canvas.getCanvasCenter(), VisualizationConstants.SCALE_OUT);
    }

    @Override
    public void zoomAll()
    {
        final VisualizationState vs = callback.getVisualizationState();
        final Set<GUINode> visibleGUINodes = canvas.getGraphVertices().stream().filter(vs::isVisible).collect(Collectors.toSet());
        if (visibleGUINodes.isEmpty()) return;

        double xmaxNpCoords = Double.NEGATIVE_INFINITY;
        double xminNpCoords = Double.POSITIVE_INFINITY;
        double ymaxNpCoords = Double.NEGATIVE_INFINITY;
        double yminNpCoords = Double.POSITIVE_INFINITY;
        double xmaxJungCoords = Double.NEGATIVE_INFINITY;
        double xminJungCoords = Double.POSITIVE_INFINITY;
        double ymaxJungCoords = Double.NEGATIVE_INFINITY;
        double yminJungCoords = Double.POSITIVE_INFINITY;
        for (GUINode node : visibleGUINodes)
        {
            Point2D nodeNpCoordinates = node.getAssociatedNetPlanNode().getXYPositionMap();
            Point2D nodeJungCoordinates = canvas.getNetPlanCoordinatesFromScreenPixelCoordinate(nodeNpCoordinates, Layer.VIEW);
            if (xmaxNpCoords < nodeNpCoordinates.getX()) xmaxNpCoords = nodeNpCoordinates.getX();
            if (xminNpCoords > nodeNpCoordinates.getX()) xminNpCoords = nodeNpCoordinates.getX();
            if (ymaxNpCoords < nodeNpCoordinates.getY()) ymaxNpCoords = nodeNpCoordinates.getY();
            if (yminNpCoords > nodeNpCoordinates.getY()) yminNpCoords = nodeNpCoordinates.getY();
            if (xmaxJungCoords < nodeJungCoordinates.getX()) xmaxJungCoords = nodeJungCoordinates.getX();
            if (xminJungCoords > nodeJungCoordinates.getX()) xminJungCoords = nodeJungCoordinates.getX();
            if (ymaxJungCoords < nodeJungCoordinates.getY()) ymaxJungCoords = nodeJungCoordinates.getY();
            if (yminJungCoords > nodeJungCoordinates.getY()) yminJungCoords = nodeJungCoordinates.getY();
        }

        double PRECISION_FACTOR = 0.00001;

        Rectangle viewInLayoutUnits = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(vv.getBounds()).getBounds();
        float ratio_h = Math.abs(xmaxJungCoords - xminJungCoords) < PRECISION_FACTOR ? 1 : (float) (viewInLayoutUnits.getWidth() / (xmaxJungCoords - xminJungCoords));
        float ratio_v = Math.abs(ymaxJungCoords - yminJungCoords) < PRECISION_FACTOR ? 1 : (float) (viewInLayoutUnits.getHeight() / (ymaxJungCoords - yminJungCoords));
        float ratio = (float) (0.8 * Math.min(ratio_h, ratio_v));
        canvas.zoom(canvas.getCanvasCenter(), ratio);

        Point2D topologyCenterJungCoord = new Point2D.Double((xminJungCoords + xmaxJungCoords) / 2, (yminJungCoords + ymaxJungCoords) / 2);
        Point2D windowCenterJungCoord = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(vv.getCenter());
        double dx = (windowCenterJungCoord.getX() - topologyCenterJungCoord.getX());
        double dy = (windowCenterJungCoord.getY() - topologyCenterJungCoord.getY());

        vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).translate(dx, dy);
    }

    @Override
    public void addNode(IVisualizationCallback callback, ITopologyCanvas topologyPanel, Point2D pos)
    {
        callback.getDesign().addNode(pos.getX() , pos.getY() , "Node" + callback.getDesign().getNumberOfNodes(), null);
        callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.NODE) , null , null);
    }

    @Override
    public void removeNode(IVisualizationCallback callback, Node node)
    {
        node.remove();
        callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.NODE) ,null , null);
    }

    @Override
    public void moveNodeInVisualization(ITopologyCanvas canvas, Node node, Point2D positionInScreenPixels)
    {
        final Point2D jungPoint = canvas.getNetPlanCoordinatesFromScreenPixelCoordinate(positionInScreenPixels, Layer.LAYOUT);
        // canvas.moveVertexToXYPosition(node, new Point2D.Double(jungPoint.getX(), -jungPoint.getY()));
    }

    @Override
    public void takeSnapshot(ITopologyCanvas canvas)
    {
        final JFileChooser fc = new FileChooserConfirmOverwrite();
        FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG files", "png");
        fc.setFileFilter(pngFilter);

        vv.setBackground(Color.WHITE);
        JComponent component = canvas.getCanvasComponent();
        BufferedImage bi = ImageUtils.trim(ImageUtils.takeSnapshot(component));
        vv.setBackground(new Color(212, 208, 200));

        int s = fc.showSaveDialog(null);
        if (s == JFileChooser.APPROVE_OPTION)
        {
            File f = fc.getSelectedFile();
            ImageUtils.writeImageToFile(f, bi, ImageUtils.ImageType.PNG);
        }
    }
}

