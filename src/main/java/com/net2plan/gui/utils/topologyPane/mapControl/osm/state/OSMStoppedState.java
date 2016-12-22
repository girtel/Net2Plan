package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.net2plan.gui.utils.FileChooserConfirmOverwrite;
import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.jung.JUNGCanvas;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.ITopologyCanvas;
import com.net2plan.utils.ImageUtils;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
public class OSMStoppedState implements OSMState
{
    private final JUNGCanvas canvas;
    private final VisualizationViewer<GUINode, GUILink> vv;

    public OSMStoppedState(final ITopologyCanvas canvas)
    {
        this.canvas = (JUNGCanvas) canvas;
        this.vv = (VisualizationViewer<GUINode, GUILink>) canvas.getComponent();
    }

    @Override
    public void panTo(Point2D initialPoint, Point2D currentPoint)
    {
        final MutableTransformer layoutTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
        final Point2D q = layoutTransformer.inverseTransform(initialPoint);
        final Point2D lvc = layoutTransformer.inverseTransform(currentPoint);
        final double dxJungCoord = (lvc.getX() - q.getX());
        final double dyJungCoord = (lvc.getY() - q.getY());

        layoutTransformer.translate(dxJungCoord, dyJungCoord);
    }

    @Override
    public void zoomIn()
    {
        canvas.zoomIn(vv.getCenter());
    }

    @Override
    public void zoomOut()
    {
        canvas.zoomOut(vv.getCenter());
    }

    @Override
    public void zoomAll()
    {
        canvas.frameTopology();
    }

    @Override
    public void addNode(TopologyPanel topologyPanel, NetPlan netPlan, String name, Point2D pos)
    {
        final Node node = netPlan.addNode(pos.getX(), pos.getY(), name, null);

        topologyPanel.getCanvas().addNode(node);
        topologyPanel.getCanvas().refresh();
    }

    @Override
    public void moveNode(INetworkCallback callback, ITopologyCanvas canvas, Node node, Point2D pos)
    {
        final Point2D jungPoint = canvas.convertViewCoordinatesToRealCoordinates(pos);

        callback.moveNode(node.getId(), pos);
        canvas.moveNodeToXYPosition(node, new Point2D.Double(jungPoint.getX(), -jungPoint.getY()));
    }

    @Override
    public void takeSnapshot(ITopologyCanvas canvas)
    {
        final JFileChooser fc = new FileChooserConfirmOverwrite();
        FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG files", "png");
        fc.setFileFilter(pngFilter);

        canvas.takeSnapshot_preConfigure();
        JComponent component = canvas.getInternalComponent();
        BufferedImage bi = ImageUtils.trim(ImageUtils.takeSnapshot(component));
        canvas.takeSnapshot_postConfigure();

        int s = fc.showSaveDialog(null);
        if (s == JFileChooser.APPROVE_OPTION)
        {
            File f = fc.getSelectedFile();
            ImageUtils.writeImageToFile(f, bi, ImageUtils.ImageType.PNG);
        }
    }
}

