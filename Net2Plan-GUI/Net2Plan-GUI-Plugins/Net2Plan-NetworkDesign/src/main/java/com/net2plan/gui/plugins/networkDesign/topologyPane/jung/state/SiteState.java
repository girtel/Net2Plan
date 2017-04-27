package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.OSMController;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jorge San Emeterio
 * @date 21/04/17
 */
class SiteState extends ViewState
{
    private String siteName;

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
    public void zoomAll()
    {
        zoomSite();
    }

    public void zoomSite()
    {
        final VisualizationState visualizationState = callback.getVisualizationState();
        final NetPlan netPlan = callback.getDesign();

        // Finding site nodes
        final Set<GUINode> visibleGUINodes = new HashSet<>();

        final Set<Node> siteNodes = netPlan.getSiteNodes(siteName);

        for (Node siteNode : siteNodes)
            visibleGUINodes.addAll(visualizationState.getCanvasVerticallyStackedGUINodes(siteNode));

        zoomNodes(visibleGUINodes);
    }

    @Override
    public CanvasOption getState()
    {
        return CanvasOption.SiteState;
    }

    public void setSiteName(String siteName)
    {
        this.siteName = siteName;
    }
}
