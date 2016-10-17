package com.net2plan.gui.utils.topologyPane.jung.map;

import com.net2plan.interfaces.networkDesign.Node;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.WaypointPainter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Jorge San Emeterio on 14/10/2016.
 */
public class NodeOverlayPanel extends MapPanel
{
    private final JToggleButton btn_overlay;
    private final List<Node> nodes;

    public NodeOverlayPanel(final List<Node> nodes)
    {
        super();

        final OverlayPainter overlay = new OverlayPainter();
        final JXMapViewer mainMap = this.getMainMap();

        this.nodes = nodes;
        this.btn_overlay = new JToggleButton("Show nodes");
        this.btn_overlay.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                if (e.getStateChange() == ItemEvent.SELECTED)
                {
                    mainMap.setOverlayPainter(overlay);
                } else if (e.getStateChange() == ItemEvent.DESELECTED)
                {
                    mainMap.setOverlayPainter(null);
                }
            }
        });

        mainMap.add(btn_overlay);
        btn_overlay.doClick();
    }

    @Override
    public File saveMap(final int width, final int height)
    {
        btn_overlay.setVisible(false);
        this.getMainMap().setOverlayPainter(null);
        return super.saveMap(width, height);
    }

    public class OverlayPainter extends WaypointPainter<DefaultWaypoint>
    {
        @Override
        protected void doPaint(Graphics2D g, JXMapViewer jxMapViewer, int width, int height)
        {
            final Color dotColor = new Color(255, 255, 0, 128);
            g.setColor(dotColor);

            // Calculating topology's center point
            final List<Point2D> knots = nodes.stream().map(node -> node.getXYPositionMap()).collect(Collectors.toList());

            double centroidX = 0, centroidY = 0;

            for (Point2D knot : knots)
            {
                centroidX += knot.getX();
                centroidY += knot.getY();
            }

            final Point2D.Double centerPoint = new Point2D.Double(centroidX / knots.size(), centroidY / knots.size());

            // Window's center
            final int centerX = NodeOverlayPanel.this.getWidth() / 2;

            for (Node node : nodes)
            {
                // Node's coords
                final Double x = node.getXYPositionMap().getX();
                final Double y = node.getXYPositionMap().getY();

                final int x2 = x.intValue();
                final int y2 = -y.intValue();

                // Distance between the topology center and the window center.
                final Double dx = centerPoint.getX() - centerX;

                // Centering nodes
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

            btn_overlay.setLocation(0, 0);
        }
    }
}
