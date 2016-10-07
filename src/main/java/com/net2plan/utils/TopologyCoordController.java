package com.net2plan.utils;

import com.net2plan.interfaces.networkDesign.Node;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jorge San Emeterio on 26/09/2016.
 */
public final class TopologyCoordController
{
    final Map<Long, Point2D> topologyMap;

    public TopologyCoordController()
    {
        topologyMap = new HashMap<>();
    }

    public void addNodeLocation(final Node node)
    {
        addNodeLocation(node.getId(), node.getXYPositionMap());
    }

    public void addNodeLocation(final long nodeId, final Point2D location)
    {
        topologyMap.put(nodeId, location);
    }

    public Map<Long, Point2D> getNodeLocationMap()
    {
        return topologyMap;
    }

    public Point2D getNodeLocation(final Node node)
    {
        return getNodeLocation(node.getId());
    }

    public Point2D getNodeLocation(final long nodeId)
    {
        return topologyMap.get(nodeId);
    }
}
