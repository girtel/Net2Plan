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

import com.net2plan.gui.utils.topologyPane.VisualizationState.VisualizationLayer;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.utils.Pair;

import java.awt.*;

/**
 * Class representing a link.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class GUILink {
    private final GUINode originNode;
    private final GUINode destinationNode;
    private Link npLink;
    private final VisualizationLayer vl;
    private final VisualizationState vs;

    /* New variables */
    private boolean hasArrow;
//    private Stroke arrowStroke, arrowStrokeIfPicked, edgeStroke, edgeStrokeIfPicked;
    private Stroke arrowStroke, edgeStroke;
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
    	this.vs = originNode.getVisualizationLayer().getVisualizationState();
    	this.vl = (destinationNode.getVisualizationLayer() != originNode.getVisualizationLayer())? null : originNode.getVisualizationLayer();
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
        	if (Math.abs(originNode.getVisualizationLayer().getIndex() - destinationNode.getVisualizationLayer().getIndex()) != 1) throw new RuntimeException ("Bad");
        }
        this.hasArrow = true;
        this.arrowStroke = new BasicStroke(1);
//        this.arrowStrokeIfPicked = new BasicStroke(2);
        this.edgeDrawPaint = Color.BLACK;
//        this.edgeDrawPaintIfPicked = Color.BLUE;
        this.arrowDrawPaint = Color.BLACK;
//        this.arrowDrawPaintIfPicked = Color.BLUE;
        this.arrowFillPaint = Color.BLACK;
//        this.arrowFillPaintIfPicked = Color.BLUE;
        this.edgeStroke = new BasicStroke(3);
        this.shownSeparated = false;
//        this.edgeStrokeIfPicked = new BasicStroke(5);
//        this.userDefinedColorOverridesTheRest = null;
//        this.userDefinedEdgeStrokeOverridesTheRest = null;
        //PARA EL EDGE STROKE SI BACKUP: return new BasicStroke(vv.getPickedEdgeState().isPicked(i) ? 2 : 1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
    }

    public VisualizationLayer getVisualizationLayer () { return vl; }
    
    @Override
    public String toString() {
        return getLabel();
    }

    public boolean getHasArrow() {
        return this.hasArrow;
    }

    public boolean isVisible () 
    {
    	if (isIntraNodeLink()) return vs.isShowInterLayerLinks();
    	return vs.isVisible(this.npLink);
    }

    public void setHasArrow(boolean hasArrow) {
        this.hasArrow = hasArrow;
    }

    public Stroke getArrowStroke() {
        return arrowStroke;
    }

    public void setArrowStroke(Stroke arrowStroke) {
        this.arrowStroke = arrowStroke;
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

    public Stroke getEdgeStroke() {
        return edgeStroke;
    }

    public void setEdgeStroke(Stroke edgeStroke) {
        this.edgeStroke = edgeStroke;
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
        return npLink == null? "" : String.format("%.2f", npLink.getUtilization());
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
