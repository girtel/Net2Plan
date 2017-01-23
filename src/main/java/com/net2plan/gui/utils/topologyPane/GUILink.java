/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/


package com.net2plan.gui.utils.topologyPane;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;

import com.net2plan.interfaces.networkDesign.Link;

import static com.net2plan.gui.utils.topologyPane.VisualizationConstants.*;


/**
 * Class representing a link.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class GUILink 
{
    private final GUINode originNode;
    private final GUINode destinationNode;
    private Link npLink;
    private final VisualizationState vs;

    /* New variables */
    private boolean hasArrow;
//    private Stroke arrowStroke, arrowStrokeIfPicked, edgeStroke, edgeStrokeIfPicked;
    private Stroke arrowStrokeIfActiveLayer, edgeStrokeIfActiveLayer;
    private Stroke arrowStrokeIfNotActiveLayer, edgeStrokeIfNotActiveLayer;
    private Paint arrowDrawPaint, arrowFillPaint, edgeDrawPaint;
    private boolean shownSeparated;
//    private Paint arrowDrawPaint, arrowDrawPaintIfPicked, arrowFillPaint, arrowFillPaintIfPicked, edgeDrawPaint, edgeDrawPaintIfPicked;
//    private Color userDefinedColorOverridesTheRest;
//    private Stroke userDefinedEdgeStrokeOverridesTheRest;

    /**
     * Default constructor.
     *
     * @param npLink              Link identifier
     * @param originNode      Origin node identifier
     * @param destinationNode Destination node identifier
     * @since 0.3.0
     */
    public GUILink(Link npLink, GUINode originNode, GUINode destinationNode) 
    {
    	this.vs = originNode.getVisualizationState();
        this.npLink = npLink;
        this.originNode = originNode;
        this.destinationNode = destinationNode;
        if (npLink != null)
        {
        	if (originNode.getAssociatedNetPlanNode() != npLink.getOriginNode()) throw new RuntimeException("The topology canvas must reflect the NetPlan object topology");
            if (destinationNode.getAssociatedNetPlanNode() != npLink.getDestinationNode()) throw new RuntimeException("The topology canvas must reflect the NetPlan object topology");
        }
        else
        {
        	if (Math.abs(originNode.getVisualizationOrderRemovingNonVisibleLayers() - destinationNode.getVisualizationOrderRemovingNonVisibleLayers()) != 1) throw new RuntimeException ("Bad");
        }
//        this.hasArrow = true;
////        this.arrowStrokeIfPicked = new BasicStroke(2);
//        this.edgeDrawPaint = Color.BLACK;
////        this.edgeDrawPaintIfPicked = Color.BLUE;
//        this.arrowDrawPaint = Color.BLACK;
////        this.arrowDrawPaintIfPicked = Color.BLUE;
//        this.arrowFillPaint = Color.BLACK;
////        this.arrowFillPaintIfPicked = Color.BLUE;
        if (this.isIntraNodeLink())
        {
            this.hasArrow = DEFAULT_INTRANODEGUILINK_HASARROW;
            this.edgeDrawPaint = DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR;
            this.arrowDrawPaint = DEFAULT_INTRANODEGUILINK_ARROWDRAWCOLOR;
            this.arrowFillPaint = DEFAULT_INTRANODEGUILINK_ARROWFILLCOLOR;
            this.edgeStrokeIfActiveLayer = DEFAULT_INTRANODEGUILINK_EDGESTROKE;
            this.edgeStrokeIfNotActiveLayer = DEFAULT_INTRANODEGUILINK_EDGESTROKE;
            this.arrowStrokeIfActiveLayer = DEFAULT_INTRANODEGUILINK_ARROWSTROKE_ACTIVE;
            this.arrowStrokeIfNotActiveLayer = DEFAULT_INTRANODEGUILINK_ARROWSTROKE;
        }
        else
        {
            this.hasArrow = DEFAULT_REGGUILINK_HASARROW;
            this.edgeDrawPaint = DEFAULT_REGGUILINK_EDGEDRAWCOLOR;
            this.arrowDrawPaint = DEFAULT_REGGUILINK_ARROWDRAWCOLOR;
            this.arrowFillPaint = DEFAULT_REGGUILINK_ARROWFILLCOLOR;
            this.edgeStrokeIfActiveLayer = DEFAULT_REGGUILINK_EDGESTROKE_ACTIVELAYER;
            this.edgeStrokeIfNotActiveLayer = DEFAULT_REGGUILINK_EDGESTROKE;
            this.arrowStrokeIfActiveLayer = DEFAULT_REGGUILINK_ARROWSTROKE_ACTIVELAYER;
            this.arrowStrokeIfNotActiveLayer = DEFAULT_REGGUILINK_ARROWSTROKE;
        }
        
        this.shownSeparated = false;
//        this.edgeStrokeIfPicked = new BasicStroke(5);
//        this.userDefinedColorOverridesTheRest = null;
//        this.userDefinedEdgeStrokeOverridesTheRest = null;
        //PARA EL EDGE STROKE SI BACKUP: return new BasicStroke(vv.getPickedEdgeState().isPicked(i) ? 2 : 1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
    }

    @Override
    public String toString() {
        return getLabel();
    }

    public boolean getHasArrow() {
        return this.hasArrow;
    }

    public void setHasArrow(boolean hasArrow) {
        this.hasArrow = hasArrow;
    }

    public Stroke getArrowStroke() 
    {
        return npLink.getNetPlan().getNetworkLayerDefault() == npLink.getLayer()? arrowStrokeIfActiveLayer : arrowStrokeIfNotActiveLayer;
    }

    public void setArrowStroke(Stroke arrowStrokeIfActiveLayer , Stroke arrowStrokeIfNotActiveLayer) 
    {
    	this.arrowStrokeIfActiveLayer = arrowStrokeIfActiveLayer;
    	this.arrowStrokeIfNotActiveLayer = arrowStrokeIfNotActiveLayer;
    }

    public Paint getEdgeDrawPaint() 
    {
        return npLink == null? edgeDrawPaint : npLink.isUp() ? edgeDrawPaint : Color.RED;
    }

    public void setEdgeDrawPaint(Paint drawPaint) {
        this.edgeDrawPaint = drawPaint;
    }

    public boolean isShownSeparated () { return shownSeparated; }

    public void setShownSeparated (boolean shownSeparated) { this.shownSeparated = shownSeparated; }

    public Paint getArrowDrawPaint() 
    {
        return npLink == null? arrowDrawPaint : npLink.isUp() ? arrowDrawPaint : Color.RED;
    }

    public void setArrowDrawPaint(Paint drawPaint)
    {
        this.arrowDrawPaint = drawPaint;
    }

    public Paint getArrowFillPaint() 
    {
        return npLink == null? arrowFillPaint : npLink.isUp() ? arrowFillPaint : Color.RED;
    }

    public void setArrowFillPaint(Paint fillPaint) {
        this.arrowFillPaint = fillPaint;
    }

    public Stroke getEdgeStroke() 
    {
    	if (isIntraNodeLink()) return edgeStrokeIfNotActiveLayer;
        return npLink.getNetPlan().getNetworkLayerDefault() == npLink.getLayer()? edgeStrokeIfActiveLayer : edgeStrokeIfNotActiveLayer;
    }

    public void setEdgeStroke(Stroke edgeStrokeIfActiveLayer , Stroke edgeStrokeIfNotActiveLayer) 
    {
    	this.edgeStrokeIfActiveLayer = edgeStrokeIfActiveLayer;
    	this.edgeStrokeIfNotActiveLayer = edgeStrokeIfNotActiveLayer;
    }

    public String getToolTip() {
        StringBuilder temp = new StringBuilder();
        temp.append("<html>");
        temp.append("<table>");
        temp.append("<tr><td>Index:</td><td>" + getAssociatedNetPlanLink().getIndex() + "</td></tr>");
        temp.append("<tr><td>Id:</td><td>" + getAssociatedNetPlanLink().getId() + "</td></tr>");
        temp.append("<tr><td>Utilization:</td><td>" + getLabel() + "</td></tr>");
        temp.append("</table>");
        temp.append("</html>");
        return temp.toString();
    }

    public boolean isIntraNodeLink () { return npLink == null; }
    
    /**
     * Returns the destination node of the link.
     *
     * @return Destination node
     * @since 0.2.0
     */
    public GUINode getDestinationNode() {
        return destinationNode;
    }

    /**
     * Returns the link identifier.
     *
     * @return Link identifier
     * @since 0.3.0
     */
    public Link getAssociatedNetPlanLink() {
        return npLink;
    }

    /**
     * Returns the link label.
     *
     * @return Link label
     * @since 0.2.0
     */
    public String getLabel() {
        return npLink == null? "IntraLink (" + getOriginNode() + "->" + getDestinationNode() +")" : String.format("%.2f", npLink.getUtilization());
    }

    /**
     * Returns the origin node of the link.
     *
     * @return Origin node
     * @since 0.2.0
     */
    public GUINode getOriginNode() {
        return originNode;
    }
}
