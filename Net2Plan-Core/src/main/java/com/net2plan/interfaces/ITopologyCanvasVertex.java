package com.net2plan.interfaces;

import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;

/**
 * @author Jorge San Emeterio
 * @date 17-Feb-17
 */
public interface ITopologyCanvasVertex
{
    Node getAssociatedNode();
    NetworkLayer getLayer();
}
