package com.net2plan.gui.utils.focusPane;

import static com.net2plan.gui.utils.topologyPane.VisualizationConstants.DEFAULT_LAYERNAME2ICONURLMAP;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;

import com.net2plan.gui.utils.topologyPane.VisualizationConstants;
import com.net2plan.gui.utils.topologyPane.VisualizationState;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.utils.Pair;

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
	DrawNode (Resource r , int maxHeightOrSizeIcon , double capacity , double occupiedCapacity)
	{
    	Pair<ImageIcon,Shape> getIconAndShape = getResourceIcon(r , maxHeightOrSizeIcon);
    	this.associatedElement = r;
    	this.icon = getIconAndShape.getFirst().getImage();
    	this.shapeIconToSetByPainter = getIconAndShape.getSecond();
    	final String l1 = r.getName().equals("")? "Resource " + r.getIndex() : r.getName();
    	final String l2 = "Type: " + r.getType();
    	final String l3 = "Occup: " + String.format("%.2f" , occupiedCapacity) + "/" + String.format("%.2f" , capacity) + r.getCapacityMeasurementUnits();
    	this.labels = Arrays.asList(l1,l2,l3);
    	this.urlsLabels = Arrays.asList("resource" + r.getId() , "resource" + r.getId() , "resource" + r.getId());
	}
	
	Image icon;
	Shape shapeIconToSetByPainter;
	Point posTopLeftCornerToSetByPainter;
	NetworkElement associatedElement;

	List<String> labels;
	List<String> urlsLabels;
	List<Rectangle2D> shapesLabelsToCreateByPainter;

	Point posCenter () { return new Point (posTopLeftCornerToSetByPainter.x + (icon.getWidth(null) / 2) , posTopLeftCornerToSetByPainter.y + (icon.getHeight(null) / 2)); }
	public String toString () { return "node: " + associatedElement; } 

	public Image getIcon()
	{
		return icon;
	}

	public Shape getShape()
	{
		return shapeIconToSetByPainter;
	}

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
     */
    static void addNodeToGraphics (Graphics2D g2d , DrawNode dn , 
    		Point topLeftPosition , FontMetrics fontMetrics , int interlineSpacePixels)
    {
		/* create resource node,with URL  */
    	g2d.drawImage (dn.icon , topLeftPosition.x , topLeftPosition.y , null);
    	final Point nodeCenter = new Point (topLeftPosition.x + dn.icon.getWidth(null) / 2 , topLeftPosition.y + dn.icon.getHeight(null) / 2);
    	dn.shapeIconToSetByPainter = new Rectangle2D.Double(topLeftPosition.x , topLeftPosition.y , dn.icon.getWidth(null) , dn.icon.getHeight(null));
    	g2d.setStroke(STROKE_ROUNDICONS);
    	g2d.draw (dn.shapeIconToSetByPainter);
    	
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
    	}
    }

}

