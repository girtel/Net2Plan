package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
public abstract class OSMState
{
    public abstract void panTo(Point2D initialPoint, Point2D currentPoint);
    public abstract void zoomIn();
    public abstract void zoomOut();
    public abstract void zoomAll();
}