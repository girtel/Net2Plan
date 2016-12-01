package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.net2plan.gui.utils.topologyPane.mapControl.osm.OSMMapController;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
public class OSMRunningState extends OSMState
{
    @Override
    public void panTo(Point2D initialPoint, Point2D currentPoint)
    {
        final double dxPanelPixelCoord = (currentPoint.getX() - initialPoint.getX());
        final double dyPanelPixelCoord = (currentPoint.getY() - initialPoint.getY());

        OSMMapController.moveMap(-dxPanelPixelCoord, -dyPanelPixelCoord);
    }

    @Override
    public void zoomIn()
    {
        OSMMapController.zoomIn();
    }

    @Override
    public void zoomOut()
    {
        OSMMapController.zoomOut();
    }

    @Override
    public void zoomAll()
    {
        OSMMapController.centerMapToNodes();
    }
}
