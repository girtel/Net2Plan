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

import com.net2plan.interfaces.networkDesign.Node;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Class representing a node.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class GUINode {
    private final Node npNode;

    /* New variables */
    private boolean visible;
    private Paint drawPaint, fillPaint, fillPaintIfPicked;
    private Font font;
    private Shape shape, shapeIfPicked;
    private double shapeSize;
    private Color userDefinedColorOverridesTheRest;

    /**
     * Constructor that allows to set a node label.
     *
     * @param id    Node identifier
     * @param pos   Node position
     * @param label Node label (may be null, default: "Node " + node identifier)
     * @since 0.3.0
     */
    public GUINode(Node npNode) {
        this.npNode = npNode;

		/* defaults */
        this.visible = true;
        this.drawPaint = java.awt.Color.BLACK;
        this.fillPaint = java.awt.Color.BLACK;
        this.fillPaintIfPicked = java.awt.Color.BLACK;
        this.font = new Font("Helvetica", Font.BOLD, 11);
        this.shapeSize = 30;
        this.shape = new Ellipse2D.Double(-1 * shapeSize / 2, -1 * shapeSize / 2, 1 * shapeSize, 1 * shapeSize);
        this.shapeIfPicked = new Ellipse2D.Double(-1.2 * shapeSize / 2, -1.2 * shapeSize / 2, 1.2 * shapeSize, 1.2 * shapeSize);
        this.userDefinedColorOverridesTheRest = null;
    }

    public Node getAssociatedNetPlanNode() {
        return npNode;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean isVisible) {
        this.visible = isVisible;
    }

    public double getShapeSize() {
        return shapeSize;
    }

    public void setShapeSize(double size) {
        this.shapeSize = size;
        this.shape = new Ellipse2D.Double(-1 * shapeSize / 2, -1 * shapeSize / 2, 1 * shapeSize, 1 * shapeSize);
        this.shapeIfPicked = new Ellipse2D.Double(-1.2 * shapeSize / 2, -1.2 * shapeSize / 2, 1.2 * shapeSize, 1.2 * shapeSize);
    }

    public Paint getDrawPaint() {
        return npNode.isUp() ? drawPaint : Color.RED;
    }

    public void setDrawPaint(Paint p) {
        this.drawPaint = p;
    }

    public Paint getFillPaint() {
        return fillPaint;
    }

    public void setFillPaint(Paint p) {
        this.fillPaint = p;
    }

    public Paint getFillPaintIfPicked() {
        return fillPaintIfPicked;
    }

    public void setFillPaintIfPicked(Paint p) {
        this.fillPaintIfPicked = p;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font f) {
        this.font = f;
    }

    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape f) {
        this.shape = f;
    }

    public Shape getShapeIfPicked() {
        return shapeIfPicked;
    }

    public void setShapeIfPicked(Shape f) {
        this.shapeIfPicked = f;
    }

    public Color getUserDefinedColorOverridesTheRest() {
        return userDefinedColorOverridesTheRest;
    }

    public void setUserDefinedColorOverridesTheRest(Color c) {
        this.userDefinedColorOverridesTheRest = c;
    }

    public boolean decreaseFontSize() {
        final int currentSize = font.getSize();
        if (currentSize == 1) return false;
        font = new Font("Helvetica", Font.BOLD, currentSize - 1);
        return true;
    }

    public void increaseFontSize() {
        font = new Font("Helvetica", Font.BOLD, font.getSize() + 1);
    }

    public String getToolTip() {
        StringBuilder temp = new StringBuilder();
        temp.append("<html>");
        temp.append("<table>");
        temp.append("<tr><td>Name:</td><td>" + getLabel() + "</td></tr>");
        temp.append("<tr><td>Index:</td><td>" + getAssociatedNetPlanNode().getIndex() + "</td></tr>");
        temp.append("<tr><td>Id:</td><td>" + getAssociatedNetPlanNode().getId() + "</td></tr>");
        temp.append("</table>");
        temp.append("</html>");
        return temp.toString();
    }

    @Override
    public String toString() {
        return getLabel();
    }

    /**
     * Returns the node label.
     *
     * @return Node label
     * @since 0.2.0
     */
    public String getLabel() {
        return npNode.getName();
    }

}
