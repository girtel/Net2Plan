package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.state;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.JUNGCanvas;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.OSMController;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationConstants;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.utils.FileChooserConfirmOverwrite;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants;
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
 * @date 19-Jan-17
 */
class ViewState implements ICanvasState
{
    protected final GUINetworkDesign callback;
    protected final JUNGCanvas canvas;
    protected final OSMController mapController;

    ViewState(GUINetworkDesign callback, ITopologyCanvas canvas, OSMController mapController)
    {
        assert canvas instanceof JUNGCanvas;

        this.callback = callback;
        this.canvas = (JUNGCanvas) canvas;
        this.mapController = mapController;
    }

    @Override
    public void start()
    {
        // Reset nodes' original position
        canvas.updateAllVerticesXYPosition();
        canvas.zoomAll();
    }

    @Override
    public void stop()
    {
    }

    @Override
    public CanvasState getState()
    {
        return CanvasState.ViewState;
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
        canvas.updateInterLayerDistanceInNpCoordinates(callback.getVisualizationState().getInterLayerSpaceInPixels());
        canvas.updateAllVerticesXYPosition();
    }

    @Override
    public void zoomOut()
    {
        canvas.zoom(canvas.getCanvasCenter(), VisualizationConstants.SCALE_OUT);
        canvas.updateInterLayerDistanceInNpCoordinates(callback.getVisualizationState().getInterLayerSpaceInPixels());
        canvas.updateAllVerticesXYPosition();
    }

    @Override
    public void zoomAll()
    {
        final VisualizationState vs = callback.getVisualizationState();
        final Set<GUINode> visibleGUINodes = canvas.getAllVertices().stream().filter(vs::isVisibleInCanvas).collect(Collectors.toSet());

        zoomNodes(visibleGUINodes);
    }

    protected void zoomNodes(Set<GUINode> nodes)
    {
        if (nodes.isEmpty()) return;

        // Returns the canvas transformer to its original state, so that Layout = View.
        canvas.resetTransformer();

        // Getting topology limits
        final List<Double> nodeXCoordJUNG = nodes.stream()
                .map(node -> canvas.getCanvasPointFromNetPlanPoint(node.getAssociatedNode().getXYPositionMap()).getX())
                .collect(Collectors.toList());
        final List<Double> nodeYCoordJUNG = nodes.stream()
                .map(node -> canvas.getCanvasPointFromNetPlanPoint(node.getAssociatedNode().getXYPositionMap()).getY())
                .collect(Collectors.toList());

        final double xmaxJungCoords = Collections.max(nodeXCoordJUNG);
        final double xminJungCoords = Collections.min(nodeXCoordJUNG);

        final double ymaxJungCoords = Collections.max(nodeYCoordJUNG);
        final double yminJungCoords = Collections.min(nodeYCoordJUNG);

        double PRECISION_FACTOR = 0.00001;

        Rectangle viewInLayoutUnits = canvas.getCurrentCanvasViewWindow();
        float ratio_h = Math.abs(xmaxJungCoords - xminJungCoords) < PRECISION_FACTOR ? 1 : (float) (viewInLayoutUnits.getWidth() / (xmaxJungCoords - xminJungCoords));
        float ratio_v = Math.abs(ymaxJungCoords - yminJungCoords) < PRECISION_FACTOR ? 1 : (float) (viewInLayoutUnits.getHeight() / (ymaxJungCoords - yminJungCoords));
        float ratio = (float) (0.6 * Math.min(ratio_h, ratio_v));
        canvas.zoom(canvas.getCanvasCenter(), ratio);

        Point2D topologyCenterJungCoord = new Point2D.Double((xminJungCoords + xmaxJungCoords) / 2, (yminJungCoords + ymaxJungCoords) / 2);
        Point2D windowCenterJungCoord = canvas.getCanvasPointFromScreenPoint(canvas.getCanvasCenter());
        double dx = (windowCenterJungCoord.getX() - topologyCenterJungCoord.getX());
        double dy = (windowCenterJungCoord.getY() - topologyCenterJungCoord.getY());

        canvas.moveCanvasTo(new Point2D.Double(dx, dy));
        canvas.updateInterLayerDistanceInNpCoordinates(callback.getVisualizationState().getInterLayerSpaceInPixels());
        canvas.updateAllVerticesXYPosition();
    }

    @Override
    public void addNode(Point2D pos)
    {
        callback.getDesign().addNode(pos.getX(), pos.getY(), "Node" + callback.getDesign().getNumberOfNodes(), null);
        callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
        callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.NODE));
        callback.addNetPlanChange();
    }

    @Override
    public void removeNode(Node node)
    {
        node.remove();
        callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
        callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.NODE));
        callback.addNetPlanChange();
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

    @Override
    public void updateNodesXYPosition()
    {
        for (GUINode vertex : canvas.getAllVertices())
        {
            canvas.getLayout().setLocation(vertex, canvas.getTransformer().transform(vertex));
        }
    }

    @Override
    public double getInterLayerDistance(int interLayerDistanceInPixels)
    {
        final double currentInterLayerDistanceInNpCoordinates;

        final Rectangle r = canvas.getCanvasComponent().getBounds();
        if (r.getHeight() == 0)
        {
            currentInterLayerDistanceInNpCoordinates = interLayerDistanceInPixels;
        } else
        {
            currentInterLayerDistanceInNpCoordinates = interLayerDistanceInPixels / canvas.getCurrentCanvasScale();
        }

        return currentInterLayerDistanceInNpCoordinates;
    }

    @Override
    public Point2D getCanvasPoint(Point2D pos)
    {
        return canvas.getCanvasPointFromNetPlanPoint(pos);
    }
}
