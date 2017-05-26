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

import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationConstants;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * Class representing a node.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class GUINode
{
    private final Node npNode;
    private final NetworkLayer layer;

    /* New variables */
    private Font font;
    private Paint borderPaint, fillPaint;
    private double iconHeightIfNotActive;


    /**
     * Constructor that allows to set a node label.
     *
     * @param npNode Node identifier
     * @since 0.3.0
     */
    public GUINode(Node npNode, NetworkLayer layer , double iconHeightIfNotActive)
    {
        this.layer = layer;
        this.npNode = npNode;

		/* defaults */
        this.borderPaint = VisualizationConstants.DEFAULT_GUINODE_COLOR;
        this.fillPaint = VisualizationConstants.DEFAULT_GUINODE_COLOR;
        this.font = new Font("Helvetica", Font.BOLD, 11);
        this.iconHeightIfNotActive = iconHeightIfNotActive;
    }

    public Node getAssociatedNode()
    {
        return npNode;
    }

    public NetworkLayer getLayer()
    {
        return layer;
    }

    public double getIconHeightInNotActiveLayer()
    {
        return iconHeightIfNotActive;
    }

    public void setIconHeightInNonActiveLayer(double sizeNonActiveLayer)
    {
        this.iconHeightIfNotActive = sizeNonActiveLayer;
    }

    public Paint getBorderPaint()
    {
        return npNode.isUp() ? borderPaint : Color.RED;
    }

    public void setBorderPaint(Paint p)
    {
        this.borderPaint = p;
    }

    public Paint getFillPaint()
    {
        return npNode.isUp() ? fillPaint : Color.RED;
    }

    public void setFillPaint(Paint p)
    {
        this.fillPaint = p;
    }

    public void setFont(Font f)
    {
        this.font = f;
    }

    public Font getFont()
    {
        return font;
    }

    public boolean decreaseFontSize()
    {
        final int currentSize = font.getSize();
        if (currentSize == 1) return false;
        font = new Font("Helvetica", Font.BOLD, currentSize - 1);
        return true;
    }

    public void increaseFontSize()
    {
        font = new Font("Helvetica", Font.BOLD, font.getSize() + 1);
    }

    public Icon getIcon()
    {
        URL url = npNode.getUrlNodeIcon(layer);
        if (url == null) url = layer.getDefaultNodeIconURL();
        if (url == null) url = VisualizationConstants.DEFAULT_LAYERNAME2ICONURLMAP.get(layer.getName());
        final int height = layer.isDefaultLayer() ? (int) (iconHeightIfNotActive * VisualizationConstants.INCREASENODESIZEFACTORACTIVE) : (int) iconHeightIfNotActive;
        final Color borderColor = getBorderPaint() == VisualizationConstants.DEFAULT_GUINODE_COLOR ? VisualizationConstants.TRANSPARENTCOLOR : (Color) getBorderPaint();
        final Icon icon = VisualizationState.getIcon(url, height, borderColor).getFirst();
        return icon;
    }

    public String getToolTip()
    {
        StringBuilder temp = new StringBuilder();
        final NetPlan np = npNode.getNetPlan();
        final String capUnits = np.getLinkCapacityUnitsName(layer);
        final String trafUnits = np.getDemandTrafficUnitsName(layer);
        final double inLinkOccup = npNode.getIncomingLinks(layer).stream().mapToDouble(e -> e.getOccupiedCapacity()).sum();
        final double outLinkOccup = npNode.getOutgoingLinks(layer).stream().mapToDouble(e -> e.getOccupiedCapacity()).sum();
        final double inLinkCapacity = npNode.getIncomingLinks(layer).stream().mapToDouble(e -> e.getCapacity()).sum();
        final double outLinkCapacity = npNode.getOutgoingLinks(layer).stream().mapToDouble(e -> e.getCapacity()).sum();
        final double inOfferedUnicast = npNode.getIncomingDemands(layer).stream().mapToDouble(e -> e.getOfferedTraffic()).sum();
        final double outOfferedUnicast = npNode.getOutgoingDemands(layer).stream().mapToDouble(e -> e.getOfferedTraffic()).sum();
        final double inOfferedMulticast = npNode.getIncomingMulticastDemands(layer).stream().mapToDouble(e -> e.getOfferedTraffic()).sum();
        final double outOfferedMulticast = npNode.getOutgoingMulticastDemands(layer).stream().mapToDouble(e -> e.getOfferedTraffic()).sum();
        temp.append("<html>");
        temp.append("<table border=\"0\">");
        temp.append("<tr><td colspan=\"2\"><strong>Node index " + npNode.getIndex() + " (id: " + npNode.getId() + ") - Layer " + getLayerName(layer) + "</strong></td></tr>");
        temp.append("<tr><td>Name:</td><td>" + npNode.getName() + "</td></tr>");
        temp.append("<tr><td>Total offered unicast traffic (in / out):</td>");
        temp.append("<td>" + String.format("%.2f", inOfferedUnicast) + "  / " + String.format("%.2f", outOfferedUnicast) + " " + trafUnits + "</td></tr>");
        temp.append("<tr><td>Total offered multicast traffic (in / out):</td>");
        temp.append("<td>" + String.format("%.2f", inOfferedMulticast) + "  / " + String.format("%.2f", outOfferedMulticast) + " " + trafUnits + "</td></tr>");
        temp.append("<tr><td>Total link capacities (in / out):</td>");
        temp.append("<td>" + String.format("%.2f", inLinkCapacity) + "  / " + String.format("%.2f", outLinkCapacity) + " " + capUnits + "</td></tr>");
        temp.append("<tr><td>Total link occupation (in / out):</td>");
        temp.append("<td>" + String.format("%.2f", inLinkOccup) + "  / " + String.format("%.2f", outLinkOccup) + " " + capUnits + "</td></tr>");
        for (Resource r : npNode.getResources())
        {
            temp.append("<tr><td>Resource " + getResourceName(r) + "</td>");
            temp.append("<td>Capacity (occupp. / avail.): " + String.format("%.2f", r.getOccupiedCapacity()) + "  / " + String.format("%.2f", r.getCapacity()) + " " + r.getCapacityMeasurementUnits() + "</td></tr>");
        }
        temp.append("</table>");
        temp.append("</html>");
        return temp.toString();
    }

    @Override
    public String toString()
    {
        return getLabel();
    }

    /**
     * Returns the node label.
     *
     * @return Node label
     * @since 0.2.0
     */
    public String getLabel()
    {
        return npNode.getName().equals("") ? "Node " + npNode.getIndex() : npNode.getName();
//        return npNode.getName() + " - L" + layer.getIndex() + ", VL" + getVisualizationOrderRemovingNonVisibleLayers();
    }

    private String getResourceName(Resource e)
    {
        return "Resource " + e.getIndex() + " (" + (e.getName().length() == 0 ? "No name" : e.getName()) + "). Type: " + e.getType();
    }

    private String getLayerName(NetworkLayer e)
    {
        return "Layer " + e.getIndex() + " (" + (e.getName().length() == 0 ? "No name" : e.getName()) + ")";
    }
}
