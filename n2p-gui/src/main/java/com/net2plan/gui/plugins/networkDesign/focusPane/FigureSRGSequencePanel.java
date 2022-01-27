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
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;

import java.awt.*;
import java.util.*;
import java.util.List;


/**
 * @author Jorge San Emeterio
 * @date 07-Feb-17
 */
public class FigureSRGSequencePanel extends FigureSequencePanel
{
    private final SharedRiskGroup riskGroup;
    private final List<String> generalMessage;

    private final Font headerFont, plainFont;
    private final int ICON_JUMP = 5;
    private final int LINE_JUMP = 1;

    private Dimension preferredSize;

    public FigureSRGSequencePanel(final GUINetworkDesign callback, final SharedRiskGroup srg, final String... titleMessage)
    {
        super(callback);
        this.riskGroup = srg;
        this.generalMessage = Arrays.asList(titleMessage);

        this.plainFont = new Font("Arial", Font.PLAIN, 10);
        this.headerFont = new Font("Arial", Font.BOLD, 12);

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

        int textRow = 0;
        int iconRow = 0;

        final int maxIconSize = 40;
        final int maxNumberOfTagsPerNodeNorResource = 1;

    	/* Initial messages */
        g2d.setFont(headerFont);
        final int fontHeightTitle = g2d.getFontMetrics().getHeight();
        for (String s : generalMessage)
        {
            g2d.drawString(s, maxIconSize, maxIconSize + (textRow) * fontHeightTitle);
            textRow = addLineJump(textRow);
        }

        // Draw node header
        textRow = addLineJump(textRow);
        g2d.drawString("Nodes", maxIconSize, maxIconSize + (fontHeightTitle * (textRow)));

        g2d.setFont(plainFont);
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final int regularInterlineSpacePixels = fontMetrics.getHeight();

        iconRow = textRow;
        iconRow = addLineJump(iconRow);
        int topCoordinateLineNodes = maxIconSize + (generalMessage.size() * fontHeightTitle) + (maxNumberOfTagsPerNodeNorResource * regularInterlineSpacePixels) * iconRow;
        Point initialDnTopLeftPosition = new Point(maxIconSize, topCoordinateLineNodes);
        int xSeparationDnCenters = maxIconSize * 3;

        int maxWidth = 0;
        int maxHeight = 0;

        // Drawing nodes
        int graphicsColumn = 0;
        for (Node node : riskGroup.getNodes())
        {
            final DrawNode drawNode = new DrawNode(node, callback.getDesign().getNetworkLayerDefault(), maxIconSize);
            drawnNodes.add(drawNode);
            final Dimension dimension = DrawNode.addNodeToGraphics(g2d, drawNode, new Point(initialDnTopLeftPosition.x + (xSeparationDnCenters * (graphicsColumn++)), initialDnTopLeftPosition.y), fontMetrics, regularInterlineSpacePixels, null);

            if (dimension.width > maxWidth) maxWidth = dimension.width + XYMARGIN;
            if (dimension.height > maxHeight) maxHeight = dimension.height + XYMARGIN;
        }

        // Draw layers

        // Building SRG layer
        final Set<NetworkLayer> layers = new HashSet<>();

        for (Link link : riskGroup.getLinksAllLayers())
        {
            layers.add(link.getLayer());
        }

        textRow = addIconJump(textRow);

        // Icons two rows below the text
        iconRow = addLineJump(textRow);
        iconRow = addLineJump(iconRow);

        // Drawing each layer
        // NOTE: Random order
        for (NetworkLayer layer : layers)
        {
            final Set<Link> links = riskGroup.getLinks(layer);

            // Drawing layer header
            g2d.setFont(headerFont);
            textRow = addLineJump(textRow);
            g2d.drawString("Links, layer " + layer.getName(), maxIconSize, maxIconSize + (textRow * fontHeightTitle));
            g2d.setFont(plainFont);

            // One row below the text
            iconRow = addLineJump(iconRow);
            int column = 0;

            final Iterator<Link> iterator = links.iterator();
            while (iterator.hasNext())
            {
                final Link link = iterator.next();
                final DrawNode ingressNode = new DrawNode(link.getOriginNode(), layer, maxIconSize);
                final DrawNode egressNode = new DrawNode(link.getDestinationNode(), layer, maxIconSize);

                topCoordinateLineNodes = maxIconSize + (generalMessage.size() * fontHeightTitle) + (maxNumberOfTagsPerNodeNorResource * regularInterlineSpacePixels) * iconRow;
                initialDnTopLeftPosition = new Point(maxIconSize, topCoordinateLineNodes);
                xSeparationDnCenters = maxIconSize * 3;

                final Point rightPos = new Point(initialDnTopLeftPosition.x + (xSeparationDnCenters * 2 * column), initialDnTopLeftPosition.y);
                final Point leftPos = new Point(initialDnTopLeftPosition.x + (xSeparationDnCenters) + (xSeparationDnCenters * 2 * column), initialDnTopLeftPosition.y);

                DrawNode.addNodeToGraphics(g2d, ingressNode, rightPos, fontMetrics, regularInterlineSpacePixels, null);
                DrawNode.addNodeToGraphics(g2d, egressNode, leftPos, fontMetrics, regularInterlineSpacePixels, null);

                drawnNodes.add(ingressNode);
                drawnNodes.add(egressNode);

                final DrawLine drawLine = new DrawLine(ingressNode, egressNode, link, ingressNode.posEast(), egressNode.posWest(), 0.0d);
                DrawLine.addLineToGraphics(g2d, drawLine, fontMetrics, regularInterlineSpacePixels);

                drawnLines.add(drawLine);

                if (!iterator.hasNext())
                {
                    // Create the space for the next layer.
                    iconRow = addIconJump(iconRow);
                    iconRow = addIconJump(iconRow);

                    if (maxWidth < leftPos.x) maxWidth = leftPos.x;
                    if (maxHeight < leftPos.y) maxHeight = leftPos.y;

                    break;
                }

                if (column < 1)
                {
                    // Next column
                    column++;
                } else
                {
                    // Next row
                    iconRow = addIconJump(iconRow);
                    iconRow = addIconJump(iconRow);
                    column = 0;
                }
            }

            textRow = removeIconJump(iconRow);
            iconRow = addLineJump(iconRow);
        }

        this.preferredSize = new Dimension(maxWidth + XYMARGIN, maxHeight + XYMARGIN);
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
