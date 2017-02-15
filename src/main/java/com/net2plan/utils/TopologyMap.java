package com.net2plan.utils;

import com.net2plan.interfaces.networkDesign.Node;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jorge San Emeterio on 26/09/2016.
 */
public final class TopologyMap
{
    final Map<Long, Point2D> topologyMap;

    public TopologyMap()
    {
        topologyMap = new HashMap<>();
    }

    /**
     * Creates a new entry in the topology map with the ID and coordinates of the given node.
     * @param node Node
     */
    public void addNodeLocation(final Node node)
    {
        addNodeLocation(node.getId(), node.getXYPositionMap());
    }

    /**
     * Creates a new entry in the topology map with the node ID and the given position.
     * @param nodeId The node ID.
     * @param location XY coordinates.
     */
    public void addNodeLocation(final long nodeId, final Point2D location)
    {
        topologyMap.put(nodeId, location);
    }

    /**
     * Returns the map containing the coordinates for each node.
     * @return The mentioned map
     */
    public Map<Long, Point2D> getNodeLocationMap()
    {
        return topologyMap;
    }

    /**
     * Return the coordinates for a given node.
     * @param node The node
     * @return Node's coordinates
     */
    public Point2D getNodeLocation(final Node node)
    {
        return getNodeLocation(node.getId());
    }

    /**
     * Returns the coordinates for a given node by using its ID.
     * @param nodeId The node
     * @return Node's coordinates
     */
    public Point2D getNodeLocation(final long nodeId)
    {
        return topologyMap.get(nodeId);
    }
}
