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
package com.net2plan.gui.plugins.networkDesign.visualizationControl;

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

    public NetworkLayer getAssociatedNetworkLayer()
    {
        return associatedNetworkLayer;
    }
}
