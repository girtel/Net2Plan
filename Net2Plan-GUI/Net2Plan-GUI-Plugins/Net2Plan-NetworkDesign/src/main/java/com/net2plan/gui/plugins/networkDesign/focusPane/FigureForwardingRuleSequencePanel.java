/*
 * ******************************************************************************
 *  * Copyright (c) 2017 Pablo Pavon-Marino.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the GNU Lesser Public License v3.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.gnu.org/licenses/lgpl.html
 *  *
 *  * Contributors:
 *  *     Pablo Pavon-Marino - Jose-Luis Izquierdo-Zaragoza, up to version 0.3.1
 *  *     Pablo Pavon-Marino - from version 0.4.0 onwards
 *  *     Pablo Pavon Marino - Jorge San Emeterio Villalain, from version 0.4.1 onwards
 *  *****************************************************************************
 */

package com.net2plan.gui.plugins.networkDesign.focusPane;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
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

        DrawNode dn = new DrawNode(forwardingRule.getSecond().getOriginNode(), networkLayer, maxHeightOrSizeIcon);
        final Dimension windowDimension = DrawNode.addNodeToGraphics(g2d , dn , initialDnTopLeftPosition , fontMetrics , regularInterlineSpacePixels , null);
        drawnNodes.add(dn);
        preferredSize = new Dimension (windowDimension.width + XYMARGIN , windowDimension.height + XYMARGIN);
    }
}
