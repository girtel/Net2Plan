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

import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetworkLayer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class DrawLine
{
    private static final Stroke STROKE_LINKSREGULAR = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
    private static final Stroke STROKE_LINKSNOURL = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);

    // TODO: These have to be private.
    private DrawNode from;
    private DrawNode to;
    private Polygon shapeLineToCreateByPainter;
    private Link associatedElement;
    private Point pFrom, pTo;
    private List<String> labels;
    private List<String> urlsLabels;
    private List<Rectangle2D> shapesLabelstoCreateByPainter;

    Point posCenter(Point posTopLeftCorner)
    {
        return new Point(posTopLeftCorner.x + (int) (shapeLineToCreateByPainter.getBounds2D().getWidth() / 2), posTopLeftCorner.y + (int) (shapeLineToCreateByPainter.getBounds2D().getHeight()));
    }

    public String toString()
    {
        return "line from: " + from + ", to: " + to + ", element: " + associatedElement;
    }

    DrawLine(DrawNode from, DrawNode to, Point pFrom, Point pTo)
    {
        this.from = from;
        this.to = to;
        this.labels = new ArrayList<>();
        this.urlsLabels = new ArrayList<>();
        this.associatedElement = null;
        this.pFrom = pFrom;
        this.pTo = pTo;
    }

    DrawLine(DrawNode from, DrawNode to, Link e, Point pFrom, Point pTo, double occupiedToShow)
    {
        final String capUnits = e.getNetPlan().getLinkCapacityUnitsName(e.getLayer());
        this.from = from;
        this.to = to;
        this.pFrom = pFrom;
        this.pTo = pTo;
        this.labels = new ArrayList<>();
        this.urlsLabels = new ArrayList<>();

        this.labels.addAll(Arrays.asList(
                "Link " + e.getIndex() ,
                String.format("%.1f" , e.getLengthInKm()) + " km (" + String.format("%.2f" , e.getPropagationDelayInMs()) + " ms)" ,
                "Occup: " + String.format("%.1f" , occupiedToShow) + " " + capUnits,
                "Total: " + String.format("%.1f" , e.getOccupiedCapacity()) + "/" + String.format("%.1f" , e.getCapacity()) + " " + capUnits ));
        this.urlsLabels.addAll(Arrays.asList("link" + e.getId(), "", "", ""));

        if (e.getCoupledDemand() != null)
        {
            labels.add("Coupled: Demand " + e.getCoupledDemand().getIndex() + ", " + getLayerName(e.getCoupledDemand().getLayer()));
            urlsLabels.add("demand" + e.getCoupledDemand().getId());
        } else if (e.getCoupledMulticastDemand() != null)
        {
            labels.add("Coupled: Multicast demand " + e.getCoupledMulticastDemand().getIndex() + ", " + getLayerName(e.getCoupledMulticastDemand().getLayer()));
            urlsLabels.add("multicastDemand" + e.getCoupledMulticastDemand().getId());
        }
        this.associatedElement = e;
    }

    static Point addLineToGraphics(Graphics2D g2d, DrawLine dl,
                                  FontMetrics fontMetrics, int interlineSpacePixels)
    {
        return addLineToGraphics(g2d, dl, fontMetrics, interlineSpacePixels, null);
    }

    static Point addLineToGraphics(Graphics2D g2d, DrawLine dl,
                                  FontMetrics fontMetrics, int interlineSpacePixels, Stroke stroke)
    {
        final int margin = 3;
        final int x1 = dl.pFrom.x;
        final int y1 = dl.pFrom.y;
        final int x2 = dl.pTo.x;
        final int y2 = dl.pTo.y;

        if (stroke == null)
        {
            if (dl.associatedElement instanceof Link)
                g2d.setStroke(STROKE_LINKSREGULAR);
            else
                g2d.setStroke(STROKE_LINKSNOURL);
        } else
        {
            g2d.setStroke(stroke);
        }
        g2d.drawLine(x1, y1, x2, y2);
        int xPoints[] = {x1 - margin, x1 + margin, x2 + margin, x2 - margin};
        int yPoints[] = {y1 + margin, y1 - margin, y2 - margin, y2 + margin};
        dl.shapeLineToCreateByPainter = new Polygon(xPoints, yPoints, yPoints.length);
        dl.shapesLabelstoCreateByPainter = new ArrayList<>(dl.labels.size());
    	Point bottomRightPoint = new Point (dl.shapeLineToCreateByPainter.getBounds().getLocation().x + dl.shapeLineToCreateByPainter.getBounds().width , dl.shapeLineToCreateByPainter.getBounds().getLocation().y + dl.shapeLineToCreateByPainter.getBounds().height);
        
        drawArrowHead(g2d, x1, y1, x2, y2);

        for (int lineIndex = 0; lineIndex < dl.labels.size(); lineIndex++)
        {
            final String label = dl.labels.get(lineIndex);
            boolean hasUrl = false;
            if (dl.urlsLabels != null) if (!dl.urlsLabels.get(lineIndex).equals("")) hasUrl = true;
            final int xTopLeftCorner = (x1 + x2) / 2 - (fontMetrics.stringWidth(label)) / 2;
            final int yTopLeftCorner = (y1 + y2) / 2 + 10 + interlineSpacePixels * (lineIndex + 1);
            final Color defaultColor = g2d.getColor();
            if (hasUrl) g2d.setColor(Color.blue);
            g2d.drawString(label, xTopLeftCorner, yTopLeftCorner);
            if (hasUrl) g2d.setColor(defaultColor);
            /* create rectangle of the text for links, draw it and store it */
            final Rectangle2D shapeText = fontMetrics.getStringBounds(label, g2d);
            shapeText.setRect(xTopLeftCorner, yTopLeftCorner - g2d.getFontMetrics().getAscent(), shapeText.getWidth(), shapeText.getHeight());
            dl.shapesLabelstoCreateByPainter.add(shapeText);
        	final int maxX = (int) Math.max(bottomRightPoint.x , xTopLeftCorner + shapeText.getWidth());
        	final int maxY = (int) Math.max(bottomRightPoint.y , yTopLeftCorner - g2d.getFontMetrics().getAscent() + shapeText.getHeight());
        	bottomRightPoint = new Point (maxX , maxY);
        }
        return bottomRightPoint;
    }

    private static void drawArrowHead(Graphics g1, int x1, int y1, int x2, int y2)
    {
        final int ARR_SIZE = 4;
        Graphics2D g = (Graphics2D) g1.create();

        double dx = x2 - x1, dy = y2 - y1;
        double angle = Math.atan2(dy, dx);
        int len = (int) Math.sqrt(dx * dx + dy * dy);
        AffineTransform at = AffineTransform.getTranslateInstance(x1, y1);
        at.concatenate(AffineTransform.getRotateInstance(angle));
        g.transform(at);

        // Draw horizontal arrow starting in (0, 0)
        g.drawLine(0, 0, len, 0);
        g.fillPolygon(new int[]{len, len - ARR_SIZE, len - ARR_SIZE, len},
                new int[]{0, -ARR_SIZE, ARR_SIZE, 0}, 4);
    }

    private String getLayerName(NetworkLayer layer)
    {
        return layer.getName().equals("") ? "Layer " + layer.getIndex() : layer.getName();
    }

    public DrawNode getOriginNode()
    {
        return from;
    }

    public DrawNode getDestinationNode()
    {
        return to;
    }

    public Polygon getShapeLineToCreateByPainter()
    {
        return shapeLineToCreateByPainter;
    }

    public Link getAssociatedElement()
    {
        return associatedElement;
    }

    public Point getOriginPoint()
    {
        return pFrom;
    }

    public Point getDestinationPoint()
    {
        return pTo;
    }

    public List<String> getLabels()
    {
        return labels;
    }

    public List<String> getUrlsLabels()
    {
        return urlsLabels;
    }

    public List<Rectangle2D> getShapesLabelstoCreateByPainter()
    {
        return shapesLabelstoCreateByPainter;
    }
}
