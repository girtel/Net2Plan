package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport.OSMController;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        final Set<Node> siteNodes = netPlan.getSiteNodes(siteName);
        final Set<GUINode> visibleGUINodes = new HashSet<>();

        for (Node siteNode : siteNodes)
            visibleGUINodes.addAll(visualizationState.getCanvasVerticallyStackedGUINodes(siteNode));

        zoomNodes(visibleGUINodes);
        frameSite(siteNodes);
    }

    private void frameSite(Set<Node> nodes)
    {
        List<Node> nodeList = new ArrayList<>(nodes);

        final Point2D referencePoint = canvas.getCanvasPointFromScreenPoint(canvas.getCanvasCenter());

        if (nodeList.size() == 1)
        {
            final Node node = nodeList.get(0);

            double prevDistance = Double.MAX_VALUE;
            double distance = calculateDistance(canvas.getCanvasPointFromNetPlanPoint(node.getXYPositionMap()), referencePoint);

            System.out.println(prevDistance / distance);

            int iter = 0;
            while (prevDistance / distance > 1.0001)
            {
                zoomIn();
                prevDistance = distance;
                distance = calculateDistance(canvas.getCanvasPointFromNetPlanPoint(node.getXYPositionMap()), referencePoint);

                System.out.println(prevDistance / distance);
                iter++;
            }

            System.out.println(iter);
        }
    }

    private double calculateDistance(Point2D point0, Point2D point1)
    {
        return Math.sqrt(Math.pow(point1.getX() - point0.getX(), 2) + Math.pow(point1.getY() - point0.getY(), 2));
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
