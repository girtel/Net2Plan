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


package com.net2plan.gui.utils.topology;

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

    /* New variables */
    private Color normalColor, colorIfPicked;
    private boolean showLabel;
    private boolean isVisible;
    private boolean hasArrow;
    private Stroke arrowStroke, arrowStrokeIfPicked, edgeStroke, edgeStrokeIfPicked;
    private Paint arrowDrawPaint, arrowDrawPaintIfPicked, arrowFillPaint, arrowFillPaintIfPicked, edgeDrawPaint, edgeDrawPaintIfPicked;
    private Color userDefinedColorOverridesTheRest;
    private Stroke userDefinedEdgeStrokeOverridesTheRest;

    /**
     * Default constructor.
     *
     * @param id              Link identifier
     * @param originNode      Origin node identifier
     * @param destinationNode Destination node identifier
     * @since 0.3.0
     */
    public GUILink(Link npLink, GUINode originNode, GUINode destinationNode) {
        this.npLink = npLink;
        this.originNode = originNode;
        this.destinationNode = destinationNode;
        if (originNode.getAssociatedNetPlanNode() != npLink.getOriginNode())
            throw new RuntimeException("The topology canvas must reflect the NetPlan object topology");
        if (destinationNode.getAssociatedNetPlanNode() != npLink.getDestinationNode())
            throw new RuntimeException("The topology canvas must reflect the NetPlan object topology");

        this.isVisible = true;
        this.hasArrow = true;
        this.arrowStroke = new BasicStroke(1);
        this.arrowStrokeIfPicked = new BasicStroke(2);
        this.edgeDrawPaint = Color.BLACK;
        this.edgeDrawPaintIfPicked = Color.BLUE;
        this.arrowDrawPaint = Color.BLACK;
        this.arrowDrawPaintIfPicked = Color.BLUE;
        this.arrowFillPaint = Color.BLACK;
        this.arrowFillPaintIfPicked = Color.BLUE;
        this.edgeStroke = new BasicStroke(3);
        this.edgeStrokeIfPicked = new BasicStroke(5);
        this.userDefinedColorOverridesTheRest = null;
        this.userDefinedEdgeStrokeOverridesTheRest = null;
        //PARA EL EDGE STROKE SI BACKUP: return new BasicStroke(vv.getPickedEdgeState().isPicked(i) ? 2 : 1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
    }

    @Override
    public String toString() {
        return getLabel();
    }

    public boolean isVisible() {
        return this.isVisible;
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public boolean getHasArrow() {
        return this.hasArrow;
    }

    public void setHasArrow(boolean hasArrow) {
        this.hasArrow = hasArrow;
    }

    public Pair<Stroke, Stroke> getArrowStroke() {
        return Pair.of(arrowStroke, arrowStrokeIfPicked);
    }

    public void setArrowStroke(Stroke arrowStroke, Stroke arrowStrokeIfPicked) {
        this.arrowStroke = arrowStroke;
        this.arrowStrokeIfPicked = arrowStrokeIfPicked;
    }

    public Pair<Paint, Paint> getEdgeDrawPaint() {
        return Pair.of(npLink.isUp() ? edgeDrawPaint : Color.RED, npLink.isUp() ? edgeDrawPaintIfPicked : Color.RED);
    }

    public void setEdgeDrawPaint(Paint drawPaint, Paint drawPaintIfPicked) {
        this.edgeDrawPaint = drawPaint;
        this.edgeDrawPaintIfPicked = drawPaintIfPicked;
    }

    public Pair<Paint, Paint> getArrowDrawPaint() {
        return Pair.of(npLink.isUp() ? arrowDrawPaint : Color.RED, npLink.isUp() ? arrowDrawPaintIfPicked : Color.RED);
    }

    public void setArrowDrawPaint(Paint drawPaint, Paint drawPaintIfPicked) {
        this.arrowDrawPaint = drawPaint;
        this.arrowDrawPaintIfPicked = drawPaintIfPicked;
    }

    public Pair<Paint, Paint> getArrowFillPaint() {
        return Pair.of(npLink.isUp() ? arrowFillPaint : Color.RED, npLink.isUp() ? arrowFillPaintIfPicked : Color.RED);
    }

    public void setArrowFillPaint(Paint fillPaint, Paint fillPaintIfPicked) {
        this.arrowFillPaint = fillPaint;
        this.arrowFillPaintIfPicked = fillPaintIfPicked;
    }

    public Pair<Stroke, Stroke> getEdgeStroke() {
        return Pair.of(edgeStroke, edgeStrokeIfPicked);
    }

    public void setEdgeStroke(Stroke edgeStroke, Stroke edgeStrokeIfPicked) {
        this.edgeStroke = edgeStroke;
        this.edgeStrokeIfPicked = edgeStrokeIfPicked;
    }

    public Color getUserDefinedColorOverridesTheRest() {
        return userDefinedColorOverridesTheRest;
    }

    public void setUserDefinedColorOverridesTheRest(Color c) {
        this.userDefinedColorOverridesTheRest = c;
    }

    public Stroke getUserDefinedStrokeOverridesTheRest() {
        return userDefinedEdgeStrokeOverridesTheRest;
    }

    public void setUserDefinedStrokeOverridesTheRest(Stroke c) {
        this.userDefinedEdgeStrokeOverridesTheRest = c;
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
        return String.format("%.2f", npLink.getUtilizationIncludingProtectionSegments());
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
