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
package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.OSMController;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jorge San Emeterio
 * @date 21/04/17
 */
class SiteState extends ViewState
{
    private Node siteNode;

    SiteState(GUINetworkDesign callback, ITopologyCanvas canvas, OSMController mapController)
    {
        super(callback, canvas, mapController);
    }

    @Override
    public void start()
    {
        updateNodesXYPosition();
        zoomSite();
    }

    @Override
    public void stop()
    {
        callback.resetPickedStateAndUpdateView();
        callback.getVisualizationState().pickElement(siteNode);
        callback.updateVisualizationAfterPick();
    }

    @Override
    public void zoomAll()
    {
        zoomSite();
    }

    public void zoomSite()
    {
        final NetPlan netPlan = callback.getDesign();

        // Finding site nodes
        final List<Node> nodeList = new ArrayList<>(netPlan.getSiteNodes(siteNode.getSiteName()));
        callback.getVisualizationState().pickElement(nodeList);
        callback.updateVisualizationAfterPick();

        zoomNodes(nodeList);
    }

    @Override
    public CanvasOption getState()
    {
        return CanvasOption.SiteState;
    }

    @Override
    public Color getBackgroundColor()
    {
        return UIManager.getColor("Panel.background").brighter();
    }

    public void setNode(Node node)
    {
        assert node != null;
        this.siteNode = node;
    }
}
