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
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class FigureNodeSequencePanel extends FigureSequencePanel
{
    private DrawNode dn;
    private List<String> generalMessage;
    private Node node;
    private NetworkLayer layer;
    private Dimension preferredSize;

    public FigureNodeSequencePanel(GUINetworkDesign callback, Node node, NetworkLayer layer, String... titleMessage)
    {
        super(callback);
        this.layer = layer;
        this.node = node;
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

        int currentXYStartOfText = 40;
        final int maxHeightOrSizeIcon = 60;
        final int maxNumberOfTagsPerNodeNorResource = 1;

    	/* Initial messages */
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        final int fontHeightTitle = g2d.getFontMetrics().getHeight();
        for (int indexMessage = 0; indexMessage < generalMessage.size(); indexMessage++)
        {
            final String m = generalMessage.get(indexMessage);
            g2d.drawString(m, currentXYStartOfText, currentXYStartOfText);
            currentXYStartOfText += fontHeightTitle;
        }

        currentXYStartOfText += 15; // separation main message to first info nodes

        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final int regularInterlineSpacePixels = fontMetrics.getHeight();

        final int topCoordinateLineNodes = currentXYStartOfText + (maxNumberOfTagsPerNodeNorResource * regularInterlineSpacePixels);
        final Point initialDnTopLeftPosition = new Point(maxHeightOrSizeIcon, topCoordinateLineNodes);

    	/* Initial dn */
        dn = new DrawNode(node, layer, maxHeightOrSizeIcon);
        final Dimension windowDimension = DrawNode.addNodeToGraphics(g2d , dn , initialDnTopLeftPosition , fontMetrics , regularInterlineSpacePixels , null);
        drawnNodes.add(dn);
        preferredSize = new Dimension (windowDimension.width + XYMARGIN , windowDimension.height + XYMARGIN);
    }
}

