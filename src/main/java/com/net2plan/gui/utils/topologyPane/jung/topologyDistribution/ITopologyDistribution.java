package com.net2plan.gui.utils.topologyPane.jung.topologyDistribution;

import com.net2plan.interfaces.networkDesign.Node;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;

/**
 * @author Jorge San Emeterio
 * @date 07-Dec-16
 */
public interface ITopologyDistribution
{
    Map<Node, Point2D> getNodeDistribution(final List<Node> nodes);
}
