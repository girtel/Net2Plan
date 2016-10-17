package com.net2plan.gui.utils.topologyPane.jung.map.utils;

import com.net2plan.interfaces.networkDesign.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Jorge San Emeterio on 14/10/2016.
 */
public class NodeOverlayPanel extends JPanel
{
    private List<Node> nodes;


    public NodeOverlayPanel(final List<Node> nodes)
    {
        super();

        this.nodes = nodes;

        this.setOpaque(false);
        this.setBackground(new Color(0, 0, 0, 0));

        this.setVisible(true);
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        // Topology's center point
        final List<Point2D> knots = nodes.stream().map(node -> node.getXYPositionMap()).collect(Collectors.toList());

        double centroidX = 0, centroidY = 0;

        for (Point2D knot : knots)
        {
            centroidX += knot.getX();
            centroidY += knot.getY();
        }

        final Point2D.Double centerPoint = new Point2D.Double(centroidX / knots.size(), centroidY / knots.size());

        final Color dotColor = new Color(255, 255, 0, 128);
        g.setColor(dotColor);

        // Window's center
        final int centerX = this.getWidth()/2;

        for (Node node : nodes)
        {
            final Double x = node.getXYPositionMap().getX();
            final Double y = node.getXYPositionMap().getY();

            final int x2 = x.intValue();
            final int y2 = -y.intValue();

            final Double dx = centerPoint.getX() - centerX;

            final int x3;
            if (dx >= 0)
            {
                x3 = x2 - dx.intValue();
            } else
            {
                x3 = x2 + dx.intValue();
            }

            g.fillOval(x3, y2, 25, 25);
        }
    }
}
