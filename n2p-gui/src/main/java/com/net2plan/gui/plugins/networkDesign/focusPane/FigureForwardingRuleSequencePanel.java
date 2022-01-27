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

package com.net2plan.gui.plugins.networkDesign.focusPane;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Pair;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author Jorge San Emeterio Villalain
 * @date 4/04/17
 */
public class FigureForwardingRuleSequencePanel extends FigureSequencePanel
{
    private Pair<Demand, Link> forwardingRule;
    private NetworkLayer networkLayer;
    private List<String> generalMessage;
    private Dimension preferredSize;

    private final int ICON_JUMP = 5;
    private final int LINE_JUMP = 1;

    private final Font plainFont = new Font("Arial", Font.PLAIN, 10);
    private final Font headerFont = new Font("Arial", Font.BOLD, 12);

    private final BasicStroke lineStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f);

    private int textRow = 0;
    private int iconRow = 0;

    private final int maxIconSize = 40;
    private final int maxNumberOfTagsPerNodeNorResource = 1;

    public FigureForwardingRuleSequencePanel(GUINetworkDesign callback, Pair<Demand, Link> forwardingRule, NetworkLayer networkLayer, String... titleMessage)
    {
        super(callback);

        assert forwardingRule != null;
        assert networkLayer != null;

        this.forwardingRule = forwardingRule;
        this.networkLayer = networkLayer;
        this.generalMessage = Arrays.asList(titleMessage);
        this.preferredSize = null;
    }

    @Override
    public Dimension getPreferredSize()
    {
        return preferredSize == null ? DEFAULT_DIMENSION : preferredSize;
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        final Graphics2D g2d = (Graphics2D) graphics;
        g2d.setColor(Color.black);

        textRow = 0;
        iconRow = 0;

    	/* Initial messages */
        g2d.setFont(headerFont);
        final int fontHeightTitle = g2d.getFontMetrics().getHeight();
        for (String s : generalMessage)
        {
            g2d.drawString(s, maxIconSize, maxIconSize + (textRow) * fontHeightTitle);
            textRow = addLineJump(textRow);
        }

        // Drawing node
        drawNode(g2d);

        // Drawing demand
        drawDemand(g2d);

        // Drawing link
        drawLink(g2d);
    }

    private void drawNode(Graphics2D g2d)
    {
        g2d.setFont(headerFont);
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int regularInterlineSpacePixels = fontMetrics.getHeight();

        // Draw node header
        final Node originNode = forwardingRule.getSecond().getOriginNode();
        textRow = addLineJump(textRow);
        g2d.drawString("Node " + originNode.getIndex(), maxIconSize, maxIconSize + (regularInterlineSpacePixels * (textRow)));

        // Drawing node
        g2d.setFont(plainFont);
        fontMetrics = g2d.getFontMetrics();
        regularInterlineSpacePixels = fontMetrics.getHeight();

        iconRow = textRow;
        iconRow = addLineJump(iconRow);

        int topCoordinateLineNodes = maxIconSize + (generalMessage.size() * regularInterlineSpacePixels) + (maxNumberOfTagsPerNodeNorResource * regularInterlineSpacePixels) * iconRow;
        Point initialDnTopLeftPosition = new Point(maxIconSize, topCoordinateLineNodes);

        DrawNode dn = new DrawNode(originNode, networkLayer, maxIconSize);
        DrawNode.addNodeToGraphics(g2d, dn, initialDnTopLeftPosition, fontMetrics, regularInterlineSpacePixels, null);
        drawnNodes.add(dn);
    }

    private void drawDemand(Graphics2D g2d)
    {
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final int regularInterlineSpacePixels = fontMetrics.getHeight();

        final Demand demand = forwardingRule.getFirst();
        final Node demandIngress = demand.getIngressNode();
        final Node demandEgress = demand.getEgressNode();

        final DrawNode ingressNode = new DrawNode(demandIngress, demand.getLayer(), maxIconSize);
        final DrawNode egressNode = new DrawNode(demandEgress, demand.getLayer(), maxIconSize);

        textRow = addIconJump(iconRow);
        textRow = addLineJump(textRow);
        textRow = addLineJump(textRow);

        iconRow = addLineJump(textRow);

        int topCoordinateLineNodes = maxIconSize + (generalMessage.size() * regularInterlineSpacePixels) + (maxNumberOfTagsPerNodeNorResource * regularInterlineSpacePixels) * iconRow;
        Point initialDnTopLeftPosition = new Point(maxIconSize, topCoordinateLineNodes);
        int xSeparationDnCenters = maxIconSize * 3;

        g2d.setFont(headerFont);
        g2d.drawString("Demand " + demand.getIndex(), maxIconSize, maxIconSize + (regularInterlineSpacePixels * (textRow)));

        g2d.setFont(plainFont);

        DrawNode.addNodeToGraphics(g2d, ingressNode, initialDnTopLeftPosition, fontMetrics, regularInterlineSpacePixels, null);
        DrawNode.addNodeToGraphics(g2d, egressNode, new Point(initialDnTopLeftPosition.x + xSeparationDnCenters, initialDnTopLeftPosition.y), fontMetrics, regularInterlineSpacePixels, null);

        drawnNodes.add(ingressNode);
        drawnNodes.add(egressNode);

        final DrawLine demandDL = new DrawLine(ingressNode, egressNode, ingressNode.posEast(), egressNode.posWest());
        DrawLine.addLineToGraphics(g2d, demandDL, fontMetrics, regularInterlineSpacePixels, lineStroke);
    }

    private void drawLink(Graphics2D g2d)
    {
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final int regularInterlineSpacePixels = fontMetrics.getHeight();

        final Link link = forwardingRule.getSecond();
        final Node linkOrigin = link.getOriginNode();
        final Node linkDestination = link.getDestinationNode();

        final DrawNode originNode = new DrawNode(linkOrigin, link.getLayer(), maxIconSize);
        final DrawNode destinationNode = new DrawNode(linkDestination, link.getLayer(), maxIconSize);

        textRow = addIconJump(iconRow);
        textRow = addLineJump(textRow);
        textRow = addLineJump(textRow);

        iconRow = addLineJump(textRow);

        int topCoordinateLineNodes = maxIconSize + (generalMessage.size() * regularInterlineSpacePixels) + (maxNumberOfTagsPerNodeNorResource * regularInterlineSpacePixels) * iconRow;
        Point initialDnTopLeftPosition = new Point(maxIconSize, topCoordinateLineNodes);
        int xSeparationDnCenters = maxIconSize * 3;

        g2d.setFont(headerFont);
        g2d.drawString("Link " + link.getIndex(), maxIconSize, maxIconSize + (regularInterlineSpacePixels * (textRow)));

        g2d.setFont(plainFont);

        DrawNode.addNodeToGraphics(g2d, originNode, initialDnTopLeftPosition, fontMetrics, regularInterlineSpacePixels, null);
        final Dimension windowDimension = DrawNode.addNodeToGraphics(g2d, destinationNode, new Point(initialDnTopLeftPosition.x + xSeparationDnCenters, initialDnTopLeftPosition.y), fontMetrics, regularInterlineSpacePixels, null);

        drawnNodes.add(originNode);
        drawnNodes.add(destinationNode);

        final DrawLine linkDL = new DrawLine(originNode, destinationNode, link, originNode.posEast(), destinationNode.posWest(), link.getOccupiedCapacity());
        DrawLine.addLineToGraphics(g2d, linkDL, fontMetrics, regularInterlineSpacePixels);
        drawnLines.add(linkDL);
        preferredSize = new Dimension(windowDimension.width + XYMARGIN, windowDimension.height + XYMARGIN);
    }

    private int addIconJump(int graphicsRow)
    {
        return graphicsRow + ICON_JUMP;
    }

    private int addLineJump(int graphicsRow)
    {
        return graphicsRow + LINE_JUMP;
    }

    private int removeIconJump(int graphicsRow)
    {
        return graphicsRow - ICON_JUMP;
    }

    private int removeLineJump(int graphicsRow)
    {
        return graphicsRow - LINE_JUMP;
    }
}
