package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.OSMController;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;

import java.util.ArrayList;
import java.util.List;

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
        final NetPlan netPlan = callback.getDesign();

        // Finding site nodes
        final List<Node> nodeList = new ArrayList<>(netPlan.getSiteNodes(siteName));
        callback.getVisualizationState().pickElement(nodeList);
        callback.updateVisualizationAfterPick();

        zoomNodes(nodeList);
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
