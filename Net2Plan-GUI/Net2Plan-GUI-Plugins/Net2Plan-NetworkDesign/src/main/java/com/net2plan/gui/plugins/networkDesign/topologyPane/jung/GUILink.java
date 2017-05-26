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


package com.net2plan.gui.plugins.networkDesign.topologyPane.jung;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.util.Set;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationConstants;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;


/**
 * Class representing a link.
 */
public class GUILink
{
    private final GUINode originNode;
    private final GUINode destinationNode;
    private final Link npLink;
    private final VisualizationState vs;

    /* New variables */
    private boolean hasArrow;
    private BasicStroke edgeStrokeIfActiveLayer;
    private BasicStroke edgeStrokeIfNotActiveLayer;
    private Paint edgeDrawPaint;
    private boolean shownSeparated;
//    private boolean overrideLinkColoringByUtilizationOrRunOut;
    
    
    /**
     * Default constructor.
     *
     * @param npLink              Link identifier
     * @param originNode      Origin node identifier
     * @param destinationNode Destination node identifier
     * @since 0.3.0
     */
    public GUILink(VisualizationState vs , Link npLink, GUINode originNode, GUINode destinationNode , BasicStroke edgeStrokeIfActiveLayer , BasicStroke edgeStrokeIfNotActiveLayer) 
    {
    	this.vs = vs;
        this.npLink = npLink;
        this.originNode = originNode;
        this.destinationNode = destinationNode;
//        this.overrideLinkColoringByUtilizationOrRunOut = false;
        if (npLink != null)
        {
        	if (originNode.getAssociatedNode() != npLink.getOriginNode()) throw new RuntimeException("The topology canvas must reflect the NetPlan object topology");
            if (destinationNode.getAssociatedNode() != npLink.getDestinationNode()) throw new RuntimeException("The topology canvas must reflect the NetPlan object topology");
        }
        this.edgeStrokeIfActiveLayer = edgeStrokeIfActiveLayer;
        this.edgeStrokeIfNotActiveLayer = edgeStrokeIfNotActiveLayer; 

        if (this.isIntraNodeLink())
        {
            this.hasArrow = VisualizationConstants.DEFAULT_INTRANODEGUILINK_HASARROW;
            this.edgeDrawPaint = VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR;
        }
        else
        {
            this.hasArrow = VisualizationConstants.DEFAULT_REGGUILINK_HASARROW;
            this.edgeDrawPaint = VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR;
        }
        this.shownSeparated = false;
    }

    @Override
    public String toString() {
        return getLabel();
    }

    public boolean getHasArrow() {
        return this.hasArrow;
    }

    public void setHasArrow(boolean hasArrow) {
        this.hasArrow = hasArrow;
    }

//    public void setOverrideLinkColoringByUtilizationOrRunOut (boolean override) { this.overrideLinkColoringByUtilizationOrRunOut = override; }
    
    public BasicStroke getStrokeIfActiveLayer() { return edgeStrokeIfActiveLayer; }

    public BasicStroke getStrokeIfNotActiveLayer() { return edgeStrokeIfNotActiveLayer; }
    
    public BasicStroke getStroke()
    {
    	if (npLink == null) return edgeStrokeIfNotActiveLayer; // interlayer link
        return npLink.getNetPlan().getNetworkLayerDefault() == npLink.getLayer()? edgeStrokeIfActiveLayer : edgeStrokeIfNotActiveLayer;
    }

    public Paint getEdgeDrawPaint()
    {
    	if (npLink == null) return edgeDrawPaint;
    	if (!npLink.isUp()) return Color.RED;
    	
    	/* Consider worst case color if not separated links */
    	final Set<Link> overlappingLinksToConsider = shownSeparated ? Sets.newHashSet(npLink) : 
    		npLink.getNetPlan().getNodePairLinks(npLink.getOriginNode(), npLink.getDestinationNode(), true);

    	/* In red if any overlapping link is down */
    	if (overlappingLinksToConsider.stream().anyMatch(ee->ee.isDown())) return Color.RED; 
    		
    	if (vs.getIsActiveLinkUtilizationColorThresholdList())
        {
    		if(!npLink.getLayer().isDefaultLayer()) return edgeDrawPaint;
    		final double worstUtilization = overlappingLinksToConsider.stream().mapToDouble(e->e.getUtilization()).max().orElse(0);
    		return vs.getLinkColorAccordingToUtilization(worstUtilization);
        }
    	else if (vs.getIsActiveLinkRunoutTimeColorThresholdList())
    	{
                 return edgeDrawPaint;   
    	} else
    		return edgeDrawPaint;
    }

    public void setEdgeDrawPaint(Paint drawPaint) {
        this.edgeDrawPaint = drawPaint;
    }

    public boolean isShownSeparated () { return shownSeparated; }

    public void setShownSeparated (boolean shownSeparated) { this.shownSeparated = shownSeparated; }

    public BasicStroke getEdgeStroke()
    {
    	if (isIntraNodeLink()) return edgeStrokeIfNotActiveLayer;
        return npLink.getNetPlan().getNetworkLayerDefault() == npLink.getLayer()? edgeStrokeIfActiveLayer : edgeStrokeIfNotActiveLayer;
    }

    public void setEdgeStroke(BasicStroke edgeStrokeIfActiveLayer, BasicStroke edgeStrokeIfNotActiveLayer)
    {
    	this.edgeStrokeIfActiveLayer = edgeStrokeIfActiveLayer;
    	this.edgeStrokeIfNotActiveLayer = edgeStrokeIfNotActiveLayer;
    }

    public String getToolTip() {
        StringBuilder temp = new StringBuilder();
        if (isIntraNodeLink())
        {
            temp.append("<html>");
            temp.append("<p>Internal link in the node, between two layers</p>");
            temp.append("<table border=\"0\">");
            temp.append("<tr><td>Origin layer:</td><td>" + getLayerName(originNode.getLayer()) + "</td></tr>");
            temp.append("<tr><td>Destination layer:</td><td>" + getLayerName(destinationNode.getLayer()) + "</td></tr>");
            temp.append("</table>");
            temp.append("</html>");
        }
        else
        {
            final NetPlan np = npLink.getNetPlan();
        	final NetworkLayer layer = npLink.getLayer();
    		final String capUnits = np.getLinkCapacityUnitsName(layer);
    		final String trafUnits = np.getDemandTrafficUnitsName(layer);
            temp.append("<html>");
            temp.append("<table border=\"0\">");
            temp.append("<tr><td colspan=\"2\"><strong>Link index " + npLink.getIndex() + " (id: " + npLink.getId() + ") - Layer " + getLayerName(layer) + "</strong></td></tr>");
            temp.append("<tr><td>Link carried traffic:</td><td>" + String.format("%.2f" , npLink.getCarriedTraffic()) + " " + trafUnits + "</td></tr>");
            temp.append("<tr><td>Link occupied capacity:</td><td>" + String.format("%.2f" , npLink.getOccupiedCapacity()) + " " + capUnits + "</td></tr>");
            temp.append("<tr><td>Link capacity:</td><td>" + String.format("%.2f" , npLink.getCapacity()) + " " + capUnits + "</td></tr>");
            temp.append("<tr><td>Link utilization:</td><td>" + String.format("%.2f" , npLink.getUtilization()) + "</td></tr>");
            temp.append("<tr><td>Destination layer:</td><td>" + getLayerName(destinationNode.getLayer()) + "</td></tr>");
            temp.append("<tr><td>Link length:</td><td>" + String.format("%.2f" , npLink.getLengthInKm()) + " km (" + String.format("%.2f" , npLink.getPropagationDelayInMs()) + " ms)" + "</td></tr>");
            temp.append("</table>");
            temp.append("</html>");
        }
        return temp.toString();
    }

    public boolean isIntraNodeLink () { return npLink == null; }
    
    /**
     * Returns the destination node of the link.
     *
     * @return Destination node
     * @since 0.2.0
     */
    public GUINode getDestinationNode() {
        return destinationNode;
    }

    /**
     * Returns the link identifier.
     *
     * @return Link identifier
     * @since 0.3.0
     */
    public Link getAssociatedNetPlanLink() {
        return npLink;
    }

    /**
     * Returns the link label.
     *
     * @return Link label
     * @since 0.2.0
     */
    public String getLabel() {
        return npLink == null? "IntraLink (" + getOriginNode() + "->" + getDestinationNode() +")" : String.format("%.2f", npLink.getUtilization());
    }

    /**
     * Returns the origin node of the link.
     *
     * @return Origin node
     * @since 0.2.0
     */
    public GUINode getOriginNode() {
        return originNode;
    }

	private String getNodeName (Node n) { return "Node " + n.getIndex() + " (" + (n.getName().length() == 0? "No name" : n.getName()) + ")"; }
	private String getResourceName (Resource e) { return "Resource " + e.getIndex() + " (" + (e.getName().length() == 0? "No name" : e.getName()) + "). Type: " + e.getType(); }
	private String getLayerName (NetworkLayer e) { return "Layer " + e.getIndex() + " (" + (e.getName().length() == 0? "No name" : e.getName()) + ")"; }

}
