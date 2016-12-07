package com.net2plan.gui.utils.topologyPane.jung.topologyDistribution;

import com.net2plan.interfaces.networkDesign.Node;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jorge San Emeterio
 * @date 07-Dec-16
 */
public class CircularDistribution implements ITopologyDistribution
{
    @Override
    public Map<Long, Point2D> getNodeDistribution(List<Node> nodes)
    {
        final Map<Long, Point2D> nodeDistribution = new HashMap<>();

        final int maxAng = 360;

        final int minWidth = 320;
        final int minHeight = 320;

        final int width = 30 * nodes.size();
        final int height = 30 * nodes.size();

        final int finalWidth = width < minWidth ? minWidth : width;
        final int finalHeight = height < minHeight ? minHeight : height;

        // Distribute nodes along a circle
        for (int i = 0; i < nodes.size(); i++)
        {
            final Node node = nodes.get(i);

            final int ang = (i + 1) * (maxAng / nodes.size());

            final int x = (int) (finalWidth * Math.cos(Math.toRadians(ang)));
            final int y = (int) (finalHeight * Math.sin(Math.toRadians(ang)));

            nodeDistribution.put(node.getId(), new Point2D.Double(x, y));
        }

        return nodeDistribution;
    }
}
