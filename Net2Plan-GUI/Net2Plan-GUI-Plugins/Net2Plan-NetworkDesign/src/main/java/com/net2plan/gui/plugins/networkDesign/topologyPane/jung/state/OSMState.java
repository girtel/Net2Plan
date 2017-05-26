/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/
package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.OSMController;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.OSMException;
import com.net2plan.gui.utils.FileChooserConfirmOverwrite;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants;
import com.net2plan.utils.ImageUtils;
import org.jxmapviewer.viewer.GeoPosition;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
class OSMState implements ICanvasState
{
    private final GUINetworkDesign callback;
    private final ITopologyCanvas canvas;
    private final OSMController mapController;

    OSMState(GUINetworkDesign callback, ITopologyCanvas canvas, OSMController mapController)
    {
        this.callback = callback;
        this.canvas = canvas;
        this.mapController = mapController;
    }

    @Override
    public void start()
    {
        mapController.startMap();
    }

    @Override
    public void stop()
    {
        mapController.cleanMap();
    }

    @Override
    public CanvasOption getState()
    {
        return CanvasOption.OSMState;
    }

    @Override
    public Color getBackgroundColor()
    {
        return new Color(0, 0, 0, 0);
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
    public void addNode(Point2D pos)
    {
        final double scale = canvas.getCurrentCanvasScale();
        final Point2D swingPoint = new Point2D.Double(pos.getX() * scale, -pos.getY() * scale);

        final GeoPosition geoPosition = OSMController.OSMMapUtils.convertPointToGeo(swingPoint);
        if (!OSMController.OSMMapUtils.isInsideBounds(geoPosition.getLongitude(), geoPosition.getLatitude()))
        {
            throw new OSMException("The node is out of the map's bounds", "Problem while adding node");
        }

        final NetPlan netPlan = callback.getDesign();
        netPlan.addNode(geoPosition.getLongitude(), geoPosition.getLatitude(), "Node" + netPlan.getNumberOfNodes(), null);
        callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
        callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.NODE));
        mapController.refreshTopologyAlignment();
        callback.addNetPlanChange();
    }

    @Override
    public void removeNode(Node node)
    {
        node.remove();
        callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
        callback.updateVisualizationAfterChanges(Sets.newHashSet(Constants.NetworkElementType.NODE));
        mapController.refreshTopologyAlignment();
        callback.addNetPlanChange();
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

    @Override
    public void updateNodesXYPosition()
    {
        mapController.refreshTopologyAlignment();
    }

    @Override
    public double getInterLayerDistance(int interLayerDistanceInPixels)
    {
        return interLayerDistanceInPixels;
    }

    @Override
    public Point2D getCanvasPoint(Point2D pos)
    {
        final Point2D jungPoint = canvas.getCanvasPointFromScreenPoint(pos);

        final GeoPosition geoPosition = OSMController.OSMMapUtils.convertPointToGeo(jungPoint);

        if (!OSMController.OSMMapUtils.isInsideBounds(geoPosition.getLongitude(), geoPosition.getLatitude()))
        {
            return null;
        }

        return new Point2D.Double(geoPosition.getLongitude(), geoPosition.getLatitude());
    }
}
