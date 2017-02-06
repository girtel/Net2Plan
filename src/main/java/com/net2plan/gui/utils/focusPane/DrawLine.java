package com.net2plan.gui.utils.focusPane;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.net2plan.interfaces.networkDesign.*;

class DrawLine
{
    private static final Stroke STROKE_LINKSREGULAR = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
    private static final Stroke STROKE_LINKSNOURL = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);

    public enum Orientation {VERTICAL, HORIZONTAL}

    DrawNode from, to;
    Polygon shapeLineToCreateByPainter;

    List<String> labels;
    List<String> urlsLabels;
    List<Rectangle2D> shapesLabelstoCreateByPainter;
    Link associatedElement;

    Point posCenter(Point posTopLeftCorner)
    {
        return new Point(posTopLeftCorner.x + (int) (shapeLineToCreateByPainter.getBounds2D().getWidth() / 2), posTopLeftCorner.y + (int) (shapeLineToCreateByPainter.getBounds2D().getHeight()));
    }

    public String toString()
    {
        return "line from: " + from + ", to: " + to + ", element: " + associatedElement;
    }

    DrawLine(DrawNode from, DrawNode to)
    {
        this.from = from;
        this.to = to;
        this.labels = new ArrayList<>();
        this.urlsLabels = new ArrayList<>();
        this.associatedElement = null;
    }

    DrawLine(DrawNode from, DrawNode to, Link e, double occupiedToShow)
    {
        final String capUnits = e.getNetPlan().getLinkCapacityUnitsName(e.getLayer());
        this.from = from;
        this.to = to;
        this.labels = Arrays.asList(
                "Link " + e.getIndex(),
                String.format("%.1f", e.getLengthInKm()) + " km (" + String.format("%.2f", e.getPropagationDelayInMs()) + " ms)",
                "Occup: " + String.format("%.1f", occupiedToShow) + " " + capUnits,
                "Total: " + String.format("%.1f", e.getOccupiedCapacity()) + "/" + String.format("%.1f", e.getCapacity()) + " " + capUnits);
        this.urlsLabels = Arrays.asList("link" + e.getId(), "", "", "");
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

    static void addLineToGraphics(Graphics2D g2d, DrawLine dl, FontMetrics fontMetrics, int interlineSpacePixels, Orientation orientation)
    {
        addLineToGraphics(g2d, dl, fontMetrics, interlineSpacePixels, orientation, null);
    }

    static void addLineToGraphics(Graphics2D g2d, DrawLine dl, FontMetrics fontMetrics, int interlineSpacePixels, Orientation orientation, Stroke stroke)
    {
        final int margin = 3;
        int x1 = 0, y1 = 0, x2 = 0, y2 = 0;
        if (((dl.from.associatedElement instanceof Node) && (dl.to.associatedElement instanceof Node)) ||
                ((dl.from.associatedElement instanceof Resource) && (dl.to.associatedElement instanceof Resource)))
        {
            if (orientation == Orientation.HORIZONTAL)
            {
            /* from node to node or from resource to resource => left to right  */
                x1 = dl.from.posCenter().x + dl.from.icon.getWidth(null) / 2;
                y1 = dl.from.posCenter().y;
                x2 = dl.to.posCenter().x - (dl.to.icon.getWidth(null) / 2);
                y2 = dl.to.posCenter().y;
            } else if (orientation == Orientation.VERTICAL)
            {
                x1 = dl.from.posCenter().x;
                y1 = dl.from.posCenter().y + dl.from.icon.getHeight(null) / 2;
                x2 = dl.to.posCenter().x;
                y2 = dl.to.posCenter().y - dl.from.icon.getHeight(null) / 2;
            }
        } else if ((dl.from.associatedElement instanceof Node) && (dl.to.associatedElement instanceof Resource))
        {
            if (orientation == Orientation.HORIZONTAL)
            {
            /* from node to resource (diagonal down) */
                x1 = dl.from.posCenter().x - 5;
                y1 = dl.from.posCenter().y + dl.from.icon.getHeight(null) / 2;
                x2 = dl.to.posCenter().x - 5;
                y2 = dl.to.posCenter().y - (dl.to.icon.getHeight(null) / 2);
            } else if (orientation == Orientation.VERTICAL)
            {
                x1 = 0;
                y1 = 0;
                x2 = 0;
                y2 = 0;
            }
        } else if ((dl.from.associatedElement instanceof Resource) && (dl.to.associatedElement instanceof Node))
        {
            if (orientation == Orientation.HORIZONTAL)
            {
            /* from resource to node (diagonal up) */
                x1 = dl.from.posCenter().x + 5;
                y1 = dl.from.posCenter().y - dl.from.icon.getHeight(null) / 2;
                x2 = dl.to.posCenter().x + 5;
                y2 = dl.to.posCenter().y + (dl.to.icon.getHeight(null) / 2);
            } else if (orientation == Orientation.VERTICAL)
            {
                x1 = 0;
                y1 = 0;
                x2 = 0;
                y2 = 0;
            }
        } else throw new RuntimeException();

        if (stroke == null)
        {
            if (dl.associatedElement != null)
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
        }
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


}
