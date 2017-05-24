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
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.Node;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author Jorge San Emeterio
 * @date 06-Feb-17
 */
public class FigureMulticastDemandSequencePanel extends FigureSequencePanel
{
    private final List<String> generalMessage;
    private final MulticastDemand multicastDemand;
    private final BasicStroke lineStroke;

    private Dimension preferredDimension;

    public FigureMulticastDemandSequencePanel(final GUINetworkDesign callback, final MulticastDemand multicastDemand, final String... titleMessage)
    {
        super(callback);

        this.multicastDemand = multicastDemand;
        this.generalMessage = Arrays.asList(titleMessage);
        this.lineStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f);

        this.preferredDimension = null;
    }

    @Override
    public Dimension getPreferredSize()
    {
        return preferredDimension == null ? DEFAULT_DIMENSION : preferredDimension;
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        final Graphics2D g2d = (Graphics2D) graphics;
        g2d.setColor(Color.black);

        final int maxIconSize = 40;
        final int maxNumberOfTagsPerNodeNorResource = 1;

    	/* Initial messages */
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        final int fontHeightTitle = g2d.getFontMetrics().getHeight();
        for (int indexMessage = 0; indexMessage < generalMessage.size(); indexMessage++)
        {
            final String m = generalMessage.get(indexMessage);
            g2d.drawString(m, maxIconSize, maxIconSize + (indexMessage * fontHeightTitle));
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final int regularInterlineSpacePixels = fontMetrics.getHeight();

        final DrawNode ingressNode = new DrawNode(multicastDemand.getIngressNode(), multicastDemand.getLayer(), maxIconSize);

        final List<DrawNode> egressNodes = new ArrayList<>();
        for (Node node : multicastDemand.getEgressNodes())
        {
            final DrawNode egressNode = new DrawNode(node, multicastDemand.getLayer(), maxIconSize);
            egressNodes.add(egressNode);
        }

        final int topCoordinateLineNodes = maxIconSize + (generalMessage.size() * fontHeightTitle) + (maxNumberOfTagsPerNodeNorResource * regularInterlineSpacePixels);
        final Point initialDnTopLeftPosition = new Point(maxIconSize, topCoordinateLineNodes);
        final int xSeparationDnCenters = maxIconSize * 2;
        final int ySeparationDnCenters = maxIconSize * 3;

    	/* Initial dn */
        DrawNode.addNodeToGraphics(g2d, ingressNode, new Point(initialDnTopLeftPosition.x + ((xSeparationDnCenters * (egressNodes.size() / 2)) - xSeparationDnCenters/2), initialDnTopLeftPosition.y), fontMetrics, regularInterlineSpacePixels, null);
        drawnNodes.add(ingressNode);

        for (int i = 0; i < egressNodes.size(); i++)
        {
            final DrawNode egressNode = egressNodes.get(i);
            final Point nodePos = new Point(initialDnTopLeftPosition.x + (i * xSeparationDnCenters), initialDnTopLeftPosition.y + ySeparationDnCenters);
            final Dimension windowSize = DrawNode.addNodeToGraphics(g2d, egressNode, nodePos, fontMetrics, regularInterlineSpacePixels, null);
            final DrawLine link = new DrawLine(ingressNode, egressNode, ingressNode.posSouth(), egressNode.posNorth());
            DrawLine.addLineToGraphics(g2d, link, fontMetrics, regularInterlineSpacePixels, lineStroke);

            drawnNodes.add(egressNode);

            if (i == egressNodes.size() - 1)
            {
                this.preferredDimension = new Dimension(windowSize.width + XYMARGIN, windowSize.height + XYMARGIN);
            }
        }
    }
}
