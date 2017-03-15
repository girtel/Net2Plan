package com.net2plan.gui.plugins.networkDesign.topologyPane.jung;

import com.net2plan.interfaces.networkDesign.NetworkLayer;

/**
 * Created by Jorge San Emeterio on 15/03/17.
 */
public class GUILayer
{
    private final NetworkLayer associatedNetworkLayer;
    private boolean isLayerVisible;
    private boolean isLinksVisible;
    private int layerOrder;

    public GUILayer(final NetworkLayer layer)
    {
        this.associatedNetworkLayer = layer;
        this.isLayerVisible = true;
        this.isLinksVisible = true;
        this.layerOrder = layer.getIndex();
    }

    public GUILayer(final NetworkLayer layer, final int order, final boolean isVisible, final boolean linksVisible)
    {
        this.associatedNetworkLayer = layer;
        this.layerOrder = order;
        this.isLayerVisible = isVisible;
        this.isLinksVisible = linksVisible;
    }

    public boolean isLayerVisible()
    {
        return isLayerVisible;
    }

    public boolean isLinksVisible()
    {
        return isLinksVisible;
    }

    public int getLayerOrder()
    {
        return layerOrder;
    }

    public void setLayerVisibility(boolean layerVisible)
    {
        isLayerVisible = layerVisible;
    }

    public void setLinksVisibility(boolean linksVisible)
    {
        isLinksVisible = linksVisible;
    }

    public void setLayerOrder(int layerOrder)
    {
        this.layerOrder = layerOrder;
    }
}
