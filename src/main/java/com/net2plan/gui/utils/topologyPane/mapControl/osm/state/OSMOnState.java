package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.google.common.collect.Sets;
import com.net2plan.gui.utils.FileChooserConfirmOverwrite;
import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.topologyPane.mapControl.osm.OSMMapController;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants;
import com.net2plan.internal.plugins.ITopologyCanvas;
import com.net2plan.utils.ImageUtils;
import org.jxmapviewer.viewer.GeoPosition;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
class OSMOnState implements OSMState
{
    private final IVisualizationCallback callback;
    private final ITopologyCanvas canvas;
    private final OSMMapController mapController;

    OSMOnState(final IVisualizationCallback callback, final ITopologyCanvas canvas, final OSMMapController mapController)
    {
        this.callback = callback;
        this.canvas = canvas;
        this.mapController = mapController;
    }

    @Override
    public void panTo(final Point2D initialPoint, final Point2D currentPoint)
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
    public void addNode(final Point2D pos)
    {
        final GeoPosition geoPosition = OSMMapController.OSMMapUtils.convertPointToGeo(convertJungPointToMapSwing(canvas, pos));
        if (!OSMMapController.OSMMapUtils.isInsideBounds(geoPosition.getLongitude(), geoPosition.getLatitude()))
        {
            throw new OSMMapController.OSMMapException("The node is out of the map's bounds", "Problem while adding node");
        }

        final NetPlan netPlan = callback.getDesign();
        netPlan.addNode(geoPosition.getLongitude(), geoPosition.getLatitude(), "Node" + netPlan.getNumberOfNodes(), null);

        callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.NODE) , null , null);
        mapController.restartMapState(false);
    }

    @Override
    public void removeNode(final Node node)
    {
        node.remove();
        callback.updateVisualizationAfterChanges(Sets.newHashSet(Constants.NetworkElementType.NODE) , null , null);
        mapController.restartMapState(false);
    }

    @Override
    public void moveNode(final Node node, final Point2D positionInScreenPixels)
    {
        // Calculating JUNG Coordinates
        final Point2D jungPoint = canvas.getCanvasPointFromNetPlanPoint(positionInScreenPixels);
        final GeoPosition geoPosition = OSMMapController.OSMMapUtils.convertPointToGeo(convertJungPointToMapSwing(canvas, jungPoint));

        if (!OSMMapController.OSMMapUtils.isInsideBounds(geoPosition.getLongitude(), geoPosition.getLatitude()))
            return;

        // canvas.moveVertexToXYPosition(node, new Point2D.Double(jungPoint.getX(), -jungPoint.getY()));
    }

    @Override
    public void takeSnapshot()
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
        final double scale = canvas.getCurrentCanvasScale();

        return new Point2D.Double(jungPoint.getX() * scale, -jungPoint.getY() * scale);
    }
}
