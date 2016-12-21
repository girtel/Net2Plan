package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.net2plan.gui.utils.FileChooserConfirmOverwrite;
import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.jung.JUNGCanvas;
import com.net2plan.gui.utils.topologyPane.mapControl.osm.OSMMapController;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.ITopologyCanvas;
import com.net2plan.utils.ImageUtils;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import org.jxmapviewer.viewer.GeoPosition;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
public class OSMRunningState implements OSMState
{
    private final OSMMapController mapController;

    public OSMRunningState(final OSMMapController mapController)
    {
         this.mapController = mapController;
    }

    @Override
    public void panTo(Point2D initialPoint, Point2D currentPoint)
    {
        final double dxPanelPixelCoord = (currentPoint.getX() - initialPoint.getX());
        final double dyPanelPixelCoord = (currentPoint.getY() - initialPoint.getY());

        mapController.moveMap(-dxPanelPixelCoord, -dyPanelPixelCoord);
    }

    @Override
    public void zoomIn()
    {
        mapController.zoomIn();
    }

    @Override
    public void zoomOut()
    {
        mapController.zoomOut();
    }

    @Override
    public void zoomAll()
    {
        mapController.zoomAll();
    }

    @Override
    public void addNode(TopologyPanel topologyPanel, NetPlan netPlan, String name, Point2D pos)
    {
        final GeoPosition geoPosition = OSMMapController.OSMMapUtils.convertPointToGeo(convertJungPointToMapSwing(topologyPanel.getCanvas(), pos));

        if (!OSMMapController.OSMMapUtils.isInsideBounds(geoPosition.getLongitude(), geoPosition.getLatitude()))
        {
            throw new OSMMapController.OSMMapException("The node is out of the map's bounds", "Problem while adding node");
        }

        final Node node = netPlan.addNode(geoPosition.getLongitude(), geoPosition.getLatitude(), name, null);
        topologyPanel.getCanvas().addNode(node);

        mapController.restartMapState(false);
    }

    @Override
    public void moveNode(INetworkCallback callback, ITopologyCanvas canvas, Node node, Point2D pos)
    {
        // Calculating JUNG Coordinates
        final Point2D jungPoint = canvas.convertViewCoordinatesToRealCoordinates(pos);

        final GeoPosition geoPosition = OSMMapController.OSMMapUtils.convertPointToGeo(convertJungPointToMapSwing(canvas, jungPoint));

        if (!OSMMapController.OSMMapUtils.isInsideBounds(geoPosition.getLongitude(), geoPosition.getLatitude()))
        {
            return;
        }

        callback.moveNode(node.getId(), new Point2D.Double(geoPosition.getLongitude(), geoPosition.getLatitude()));
        canvas.moveNodeToXYPosition(node, new Point2D.Double(jungPoint.getX(), -jungPoint.getY()));
    }

    @Override
    public void takeSnapshot(ITopologyCanvas canvas)
    {
        final JFileChooser fc = new FileChooserConfirmOverwrite();
        FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG files", "png");
        fc.setFileFilter(pngFilter);

        JComponent component = mapController.getMapComponent();
        BufferedImage bi = ImageUtils.trim(ImageUtils.takeSnapshot(component));

        int s = fc.showSaveDialog(null);
        if (s == JFileChooser.APPROVE_OPTION)
        {
            File f = fc.getSelectedFile();
            ImageUtils.writeImageToFile(f, bi, ImageUtils.ImageType.PNG);
        }
    }

    private Point2D convertJungPointToMapSwing(final ITopologyCanvas canvas, final Point2D jungPoint)
    {
        // Compensating zoom, all movements must be done over a 1:1 ratio.
        final MutableTransformer layoutTransformer = ((JUNGCanvas) canvas).getTransformer();
        final double scale = layoutTransformer.getScale();

        return new Point2D.Double(jungPoint.getX() * scale, -jungPoint.getY() * scale);
    }
}
