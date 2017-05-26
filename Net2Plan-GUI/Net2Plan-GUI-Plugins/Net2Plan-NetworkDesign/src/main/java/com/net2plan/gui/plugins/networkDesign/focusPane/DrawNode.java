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
package com.net2plan.gui.plugins.networkDesign.focusPane;

import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationConstants;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.utils.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationConstants.DEFAULT_LAYERNAME2ICONURLMAP;

public class DrawNode
{
	private static final Stroke STROKE_ROUNDICONS = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);

	DrawNode (Node n , NetworkLayer layer , int maxHeightOrSizeIcon)
	{
    	Pair<ImageIcon,Shape> getIconAndShape = getNodeIcon(n , layer , maxHeightOrSizeIcon);
    	this.associatedElement = n;
    	this.icon = getIconAndShape.getFirst().getImage();
    	this.shapeIconToSetByPainter = getIconAndShape.getSecond();
    	this.labels = Arrays.asList(n.getName().equals("")? "Node " + n.getIndex() : n.getName());
    	this.urlsLabels = Arrays.asList("node" + n.getId());
	}
	DrawNode (Resource r , int maxHeightOrSizeIcon , double occupiedCapacity)
	{
    	Pair<ImageIcon,Shape> getIconAndShape = getResourceIcon(r , maxHeightOrSizeIcon);
    	this.associatedElement = r;
    	this.icon = getIconAndShape.getFirst().getImage();
    	this.shapeIconToSetByPainter = getIconAndShape.getSecond();
    	final String l1 = r.getName().equals("")? "Resource " + r.getIndex() : r.getName();
    	final String l2 = "Type: " + r.getType();
		final String l3 = "Total occup: " + String.format("%.2f" , r.getOccupiedCapacity()) +  " / " + String.format("%.2f" , r.getCapacity()) + " " + r.getCapacityMeasurementUnits();
		if (occupiedCapacity < 0)
		{
	    	this.labels = Arrays.asList(l1,l2,l3);
	    	this.urlsLabels = Arrays.asList("resource" + r.getId() , "" , "");
		}
		else
		{
			final String l4 = "This occup: " + String.format("%.2f" , occupiedCapacity) + " " + r.getCapacityMeasurementUnits();
	    	this.labels = Arrays.asList(l1,l2,l3,l4);
	    	this.urlsLabels = Arrays.asList("resource" + r.getId() , "" , "" , "");
		}
	}
	
	private Image icon;
	private Shape shapeIconToSetByPainter;
	private Point posTopLeftCornerToSetByPainter;
	private NetworkElement associatedElement;

	private List<String> labels;
	private List<String> urlsLabels;
	private List<Rectangle2D> shapesLabelsToCreateByPainter;

	Point posNorthSomeWest () { return new Point (posTopLeftCornerToSetByPainter.x + (icon.getWidth(null) / 2) - 5 , posTopLeftCornerToSetByPainter.y); }
	Point posNorthSomeEast () { return new Point (posTopLeftCornerToSetByPainter.x + (icon.getWidth(null) / 2) + 5 , posTopLeftCornerToSetByPainter.y); }
	Point posSouthSomeWest () { return new Point (posTopLeftCornerToSetByPainter.x  + (icon.getWidth(null) / 2) - 5, posTopLeftCornerToSetByPainter.y + icon.getWidth(null)); }
	Point posSouthSomeEast () { return new Point (posTopLeftCornerToSetByPainter.x + (icon.getWidth(null) / 2) + 5, posTopLeftCornerToSetByPainter.y + icon.getWidth(null)); }

	Point posWest () { return new Point (posTopLeftCornerToSetByPainter.x , posTopLeftCornerToSetByPainter.y + (icon.getHeight(null) / 2)); }
	Point posEast () { return new Point (posTopLeftCornerToSetByPainter.x + icon.getWidth(null) , posTopLeftCornerToSetByPainter.y + (icon.getHeight(null) / 2)); }
	Point posSouth () { return new Point (posTopLeftCornerToSetByPainter.x + (icon.getWidth(null) / 2) , posTopLeftCornerToSetByPainter.y + icon.getWidth(null)); }
	Point posNorth () { return new Point (posTopLeftCornerToSetByPainter.x + (icon.getWidth(null) / 2) , posTopLeftCornerToSetByPainter.y); }
	Point posCenter () { return new Point (posTopLeftCornerToSetByPainter.x + (icon.getWidth(null) / 2) , posTopLeftCornerToSetByPainter.y + (icon.getHeight(null) / 2)); }
	public String toString () { return "node: " + associatedElement; }

    private Pair<ImageIcon,Shape> getNodeIcon (Node n , NetworkLayer layer , int maxHeightOrSizeIcon)
    {
    	URL url = n.getUrlNodeIcon(layer);
    	if (url == null) url = layer.getDefaultNodeIconURL();
    	if (url == null) url = DEFAULT_LAYERNAME2ICONURLMAP.get(layer.getName());
    	Pair<ImageIcon,Shape> pair = VisualizationState.getIcon(url , maxHeightOrSizeIcon , VisualizationConstants.TRANSPARENTCOLOR);
    	if (pair.getFirst().getIconWidth() > maxHeightOrSizeIcon)
    	{
    		final int newHeight = maxHeightOrSizeIcon * (pair.getFirst().getIconWidth() / maxHeightOrSizeIcon);
        	pair = VisualizationState.getIcon(url , newHeight , VisualizationConstants.TRANSPARENTCOLOR);
    	}
    	return pair;
    }
    private Pair<ImageIcon,Shape> getResourceIcon (Resource r , int maxHeightOrSizeIcon)
    {
    	URL url = r.getUrlIcon();
    	if (url == null) url = VisualizationConstants.DEFAULT_RESPOURCETYPE2ICONURLMAP.get(r.getType());
    	Pair<ImageIcon,Shape> pair = VisualizationState.getIcon(url , maxHeightOrSizeIcon , VisualizationConstants.TRANSPARENTCOLOR);
    	if (pair.getFirst().getIconWidth() > maxHeightOrSizeIcon)
    	{
    		final int newHeight = maxHeightOrSizeIcon * (pair.getFirst().getIconWidth() / maxHeightOrSizeIcon);
        	pair = VisualizationState.getIcon(url , newHeight , VisualizationConstants.TRANSPARENTCOLOR);
    	}
    	return pair;
    }

    /**
     * @param g2d
     * @param dn
     * @param topLeftPosition
     * @param fontMetrics
     * @param interlineSpacePixels
	 * @return Dimension indicating the south-east point of the icon
     */
    static Dimension addNodeToGraphics (Graphics2D g2d , DrawNode dn ,
										Point topLeftPosition , FontMetrics fontMetrics , int interlineSpacePixels , Color rectangleColor)
    {
		/* create resource node,with URL  */
    	g2d.drawImage (dn.icon , topLeftPosition.x , topLeftPosition.y , null);
    	Point bottomRightPoint = new Point (topLeftPosition.x + dn.icon.getWidth(null) , topLeftPosition.y + dn.icon.getHeight(null));
    	
    	final Point nodeCenter = new Point (topLeftPosition.x + dn.icon.getWidth(null) / 2 , topLeftPosition.y + dn.icon.getHeight(null) / 2);
    	dn.shapeIconToSetByPainter = new Rectangle2D.Double(topLeftPosition.x , topLeftPosition.y , dn.icon.getWidth(null) , dn.icon.getHeight(null));
    	g2d.setStroke(STROKE_ROUNDICONS);
    	if (rectangleColor != null) g2d.setColor(rectangleColor);
    	g2d.setStroke(STROKE_ROUNDICONS);
    	g2d.draw (dn.shapeIconToSetByPainter);
    	if (rectangleColor != null) g2d.setColor(Color.black);
    	
    	dn.shapesLabelsToCreateByPainter = new ArrayList<> (dn.labels.size());
    	dn.posTopLeftCornerToSetByPainter = topLeftPosition;
    	for (int lineIndex = 0; lineIndex < dn.labels.size() ; lineIndex ++)
    	{
    		final String label = dn.labels.get(lineIndex);
    		boolean hasUrl = false; if (dn.urlsLabels != null) if (!dn.urlsLabels.get(lineIndex).equals("")) hasUrl = true;
    		final int xTopLeftCornerString = nodeCenter.x  - (fontMetrics.stringWidth(label)/2);
    		int yTopLeftCornerString;
    		if (dn.associatedElement instanceof Resource)
    			yTopLeftCornerString = topLeftPosition.y + dn.icon.getHeight(null) + interlineSpacePixels * (lineIndex + 1);
    		else if (dn.associatedElement instanceof Node)
    			yTopLeftCornerString = topLeftPosition.y  - 5 - (interlineSpacePixels * (dn.labels.size() - lineIndex - 1));
    		else throw new RuntimeException();
    			
    		final Color defaultColor = g2d.getColor();
    		if (hasUrl) g2d.setColor(Color.blue); 
        	g2d.drawString (label , xTopLeftCornerString, yTopLeftCornerString);
    		if (hasUrl) g2d.setColor(defaultColor); 
        	/* create rectangle of the text for links, draw it and store it */
        	final Rectangle2D shapeText = g2d.getFontMetrics().getStringBounds(label, g2d);
        	shapeText.setRect(xTopLeftCornerString , yTopLeftCornerString - g2d.getFontMetrics().getAscent(), shapeText.getWidth(), shapeText.getHeight());
        	dn.shapesLabelsToCreateByPainter.add (shapeText);
        	final int maxX = (int) Math.max(bottomRightPoint.x , xTopLeftCornerString + shapeText.getWidth());
        	final int maxY = (int) Math.max(bottomRightPoint.y , yTopLeftCornerString - g2d.getFontMetrics().getAscent() + shapeText.getHeight());
        	bottomRightPoint = new Point (maxX , maxY);
    	}
    	return new Dimension(bottomRightPoint.x, bottomRightPoint.y);
    }

	public Image getIcon()
	{
		return icon;
	}

	public Shape getShapeIconToSetByPainter()
	{
		return shapeIconToSetByPainter;
	}

	public Point getPosTopLeftCornerToSetByPainter()
	{
		return posTopLeftCornerToSetByPainter;
	}

	public NetworkElement getAssociatedElement()
	{
		return associatedElement;
	}

	public List<String> getLabels()
	{
		return labels;
	}

	public List<String> getUrlsLabels()
	{
		return urlsLabels;
	}

	public List<Rectangle2D> getShapesLabelsToCreateByPainter()
	{
		return shapesLabelsToCreateByPainter;
	}
}

