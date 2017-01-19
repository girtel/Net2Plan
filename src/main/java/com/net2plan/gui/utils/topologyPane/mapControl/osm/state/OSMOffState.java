package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.net2plan.gui.utils.FileChooserConfirmOverwrite;
import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.VisualizationConstants;
import com.net2plan.gui.utils.topologyPane.VisualizationState;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants;
import com.net2plan.internal.plugins.ITopologyCanvas;
import com.net2plan.utils.ImageUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
class OSMOffState implements OSMState
{
    private final IVisualizationCallback callback;
    private final ITopologyCanvas canvas;

    @SuppressWarnings("unchecked")
    OSMOffState(final IVisualizationCallback callback, final ITopologyCanvas canvas)
    {
        this.callback = callback;
        this.canvas = canvas;
    }

    @Override
    public void panTo(Point2D initialPoint, Point2D currentPoint)
    {
        final Point2D q = canvas.getCanvasPointFromScreenPoint(initialPoint);
        final Point2D lvc = canvas.getCanvasPointFromScreenPoint(currentPoint);
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
        final Set<GUINode> visibleGUINodes = canvas.getVertices().stream().filter(vs::isVisible).collect(Collectors.toSet());
        if (visibleGUINodes.isEmpty()) return;

        // Returns the canvas transformer to its original state, so that Layout = View.
        canvas.resetTransformer();

        // Getting topology limits
        final List<Double> nodeXCoordJUNG = visibleGUINodes.stream()
                .map(node -> canvas.getCanvasPointFromNetPlanPoint(node.getAssociatedNetPlanNode().getXYPositionMap()).getX())
                .collect(Collectors.toList());
        final List<Double> nodeYCoordJUNG = visibleGUINodes.stream()
                .map(node -> canvas.getCanvasPointFromNetPlanPoint(node.getAssociatedNetPlanNode().getXYPositionMap()).getY())
                .collect(Collectors.toList());

        final double xmaxJungCoords = Collections.max(nodeXCoordJUNG);
        final double xminJungCoords = Collections.min(nodeXCoordJUNG);

        final double ymaxJungCoords = Collections.max(nodeYCoordJUNG);
        final double yminJungCoords = Collections.min(nodeYCoordJUNG);

        double PRECISION_FACTOR = 0.00001;

        Rectangle viewInLayoutUnits = canvas.getCurrentCanvasViewWindow();
        float ratio_h = Math.abs(xmaxJungCoords - xminJungCoords) < PRECISION_FACTOR ? 1 : (float) (viewInLayoutUnits.getWidth() / (xmaxJungCoords - xminJungCoords));
        float ratio_v = Math.abs(ymaxJungCoords - yminJungCoords) < PRECISION_FACTOR ? 1 : (float) (viewInLayoutUnits.getHeight() / (ymaxJungCoords - yminJungCoords));
        float ratio = (float) (0.8 * Math.min(ratio_h, ratio_v));
        canvas.zoom(canvas.getCanvasCenter(), ratio);

        Point2D topologyCenterJungCoord = new Point2D.Double((xminJungCoords + xmaxJungCoords) / 2, (yminJungCoords + ymaxJungCoords) / 2);
        Point2D windowCenterJungCoord = canvas.getCanvasPointFromScreenPoint(canvas.getCanvasCenter());
        double dx = (windowCenterJungCoord.getX() - topologyCenterJungCoord.getX());
        double dy = (windowCenterJungCoord.getY() - topologyCenterJungCoord.getY());

        canvas.moveCanvasTo(new Point2D.Double(dx, dy));
    }

    @Override
    public void addNode(Point2D pos)
    {
        callback.getDesign().addNode(pos.getX() , pos.getY() , "Node" + callback.getDesign().getNumberOfNodes(), null);
        callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.NODE) , null , null);
    }

    @Override
    public void removeNode(Node node)
    {
        node.remove();
        callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.NODE) ,null , null);
    }

    @Override
    public void moveNode(Node node, Point2D screenPoint)
    {
        final Point2D jungPoint = canvas.getCanvasPointFromNetPlanPoint(screenPoint);
        // canvas.moveVertexToXYPosition(node, new Point2D.Double(jungPoint.getX(), -jungPoint.getY()));
    }

    @Override
    public void takeSnapshot()
    {
        final JFileChooser fc = new FileChooserConfirmOverwrite();
        FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG files", "png");
        fc.setFileFilter(pngFilter);

        canvas.getCanvasComponent().setBackground(Color.WHITE);
        JComponent component = canvas.getCanvasComponent();
        BufferedImage bi = ImageUtils.trim(ImageUtils.takeSnapshot(component));
        canvas.getCanvasComponent().setBackground(new Color(212, 208, 200));

        int s = fc.showSaveDialog(null);
        if (s == JFileChooser.APPROVE_OPTION)
        {
            File f = fc.getSelectedFile();
            ImageUtils.writeImageToFile(f, bi, ImageUtils.ImageType.PNG);
        }
    }
}

