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

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;

/**
 * Class representing a node.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class GUINode 
{
    private final Node npNode;
    private final NetworkLayer layer;
    private final VisualizationState vs;

    /* New variables */
    private Font font;
    //private Paint drawPaint, fillPaint, fillPaintIfPicked;
    private Paint drawPaint, fillPaint;
//    private Shape shape, shapeIfPicked;
    private Shape shapeIfNotActive , shapeIfActive;
    private double shapeSizeIfNotActive;
//    private Color userDefinedColorOverridesTheRest;

    /**
     * Constructor that allows to set a node label.
     *
     * @param npNode    Node identifier
     * @since 0.3.0
     */
    public GUINode(Node npNode , NetworkLayer layer , VisualizationState vs)
    {
    	this.layer = layer;
        this.npNode = npNode;
        this.vs = vs;
        if (!vs.isLayerVisible(layer)) throw new RuntimeException ("Bad");

		/* defaults */
        this.drawPaint = java.awt.Color.BLACK;
        this.fillPaint = java.awt.Color.BLACK;
        this.font = new Font("Helvetica", Font.BOLD, 11);
        this.shapeSizeIfNotActive = 30;
        final double shapeSizeIfActive = shapeSizeIfNotActive * VisualizationConstants.INCREASENODESIZEFACTORACTIVE;
        this.shapeIfNotActive = adjustShapeToSize(VisualizationConstants.DEFAULT_GUINODE_SHAPE , shapeSizeIfNotActive , shapeSizeIfNotActive);
        this.shapeIfActive = adjustShapeToSize(VisualizationConstants.DEFAULT_GUINODE_SHAPE , shapeSizeIfActive , shapeSizeIfActive);
    }
    
    public NetworkLayer getLayer () { return layer; }

    public Node getAssociatedNetPlanNode() {
        return npNode;
    }

    public double getShapeInNotActiveLayerSize() 
    {
        return shapeSizeIfNotActive;
    }

    public void setShapeSizeInNonActiveLayer (double sizeNonActiveLayer) 
    {
        this.shapeSizeIfNotActive = sizeNonActiveLayer;
        final double shapeSizeIfActive = shapeSizeIfNotActive * VisualizationConstants.INCREASENODESIZEFACTORACTIVE;
        this.shapeIfNotActive = adjustShapeToSize(VisualizationConstants.DEFAULT_GUINODE_SHAPE , shapeSizeIfNotActive , shapeSizeIfNotActive);
        this.shapeIfActive = adjustShapeToSize(VisualizationConstants.DEFAULT_GUINODE_SHAPE , shapeSizeIfActive , shapeSizeIfActive);
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

    public void setFont(Font f) {
        this.font = f;
    }
    public Font getFont() {
        return font;
    }

    public Shape getShape() 
    {
        return npNode.getNetPlan().getNetworkLayerDefault() == layer? shapeIfActive : shapeIfNotActive;
    }

    public void setShape(Shape f) 
    {
        final double shapeSizeIfActive = shapeSizeIfNotActive * VisualizationConstants.INCREASENODESIZEFACTORACTIVE;
        this.shapeIfNotActive = adjustShapeToSize(VisualizationConstants.DEFAULT_GUINODE_SHAPE , shapeSizeIfNotActive , shapeSizeIfNotActive);
        this.shapeIfActive = adjustShapeToSize(VisualizationConstants.DEFAULT_GUINODE_SHAPE , shapeSizeIfActive , shapeSizeIfActive);
    }

    public boolean decreaseFontSize() 
    {
        final int currentSize = font.getSize();
        if (currentSize == 1) return false;
        font = new Font("Helvetica", Font.BOLD, currentSize - 1);
        return true;
    }

    public void increaseFontSize() 
    {
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
        return npNode.getName() + " - L" + layer.getIndex() + ", VL" + getVisualizationOrderRemovingNonVisibleLayers();
    }
    
    public VisualizationState getVisualizationState () { return vs; }

    public int getVisualizationOrderRemovingNonVisibleLayers () { return vs.getVisualizationOrderRemovingNonVisible(layer); }

    private static Shape adjustShapeToSize (Shape s , double size_x , double size_y)
    {
    	AffineTransform transf = new AffineTransform();
    	final Rectangle currentShapeBounds = s.getBounds();
    	transf.scale(size_x / currentShapeBounds.getWidth() , size_y / currentShapeBounds.getHeight());
    	return transf.createTransformedShape(s);
    }
}
