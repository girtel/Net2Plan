package com.net2plan.gui.plugins.networkDesign.interfaces;

import com.net2plan.interfaces.networkDesign.Link;

import java.awt.*;

/**
 * @author Jorge San Emeterio
 * @date 17-Feb-17
 */
public interface ITopologyCanvasEdge
{
    boolean getHasArrow();
    void setHasArrow(boolean hasArrow);

    Stroke getArrowStroke();
    void setArrowStroke(Stroke arrowStrokeIfActiveLayer , Stroke arrowStrokeIfNotActiveLayer);

    Paint getEdgeDrawPaint();
    void setEdgeDrawPaint(Paint drawPaint);

    Paint getArrowDrawPaint();
    void setArrowDrawPaint(Paint drawPaint);

    Paint getArrowFillPaint();
    void setArrowFillPaint(Paint fillPaint);

    Stroke getEdgeStroke();
    void setEdgeStroke(Stroke edgeStrokeIfActiveLayer, Stroke edgeStrokeIfNotActiveLayer);

    String getToolTip();
    String getLabel();

    Link getAssociatedNetPlanLink();
    ITopologyCanvasVertex getOriginNode();
    ITopologyCanvasVertex getDestinationNode();
}
