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

import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author Jorge San Emeterio
 * @date 06-Feb-17
 */
public class FigureDemandSequencePanel extends FigureSequencePanel
{
    private final List<String> generalMessage;
    private final Demand demand;
    private final BasicStroke lineStroke;
    private Dimension preferredSize;

    public FigureDemandSequencePanel(final GUINetworkDesign callback, final Demand demand, final String... titleMessage)
    {
        super(callback);
        this.demand = demand;
        this.generalMessage = Arrays.asList(titleMessage);
        this.lineStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f);
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

        final DrawNode ingressNode = new DrawNode(demand.getIngressNode(), demand.getLayer(), maxIconSize);
        final DrawNode egressNode = new DrawNode(demand.getEgressNode(), demand.getLayer(), maxIconSize);

        final int topCoordinateLineNodes = maxIconSize + (generalMessage.size() * fontHeightTitle) + (maxNumberOfTagsPerNodeNorResource * regularInterlineSpacePixels);
        final Point initialDnTopLeftPosition = new Point(maxIconSize, topCoordinateLineNodes);
        final int xSeparationDnCenters = maxIconSize * 3;

    	/* Initial dn */
        DrawNode.addNodeToGraphics(g2d, ingressNode, initialDnTopLeftPosition, fontMetrics, regularInterlineSpacePixels, null);

        // The furthest node of the drawing.
        final Dimension windowDimension = DrawNode.addNodeToGraphics(g2d, egressNode, new Point(initialDnTopLeftPosition.x + xSeparationDnCenters, initialDnTopLeftPosition.y), fontMetrics, regularInterlineSpacePixels, null);

        drawnNodes.add(ingressNode);
        drawnNodes.add(egressNode);

        final DrawLine link = new DrawLine(ingressNode, egressNode, ingressNode.posEast(), egressNode.posWest());
        DrawLine.addLineToGraphics(g2d, link, fontMetrics, regularInterlineSpacePixels,lineStroke);
        preferredSize = new Dimension (windowDimension.width + XYMARGIN , windowDimension.height + XYMARGIN);
    }
}
