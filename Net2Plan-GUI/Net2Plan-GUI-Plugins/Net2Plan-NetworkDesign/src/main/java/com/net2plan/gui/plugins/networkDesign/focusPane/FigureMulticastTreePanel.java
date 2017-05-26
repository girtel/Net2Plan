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
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.Node;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class FigureMulticastTreePanel extends FigureSequencePanel
{
    private MulticastTree tree;
    private List<String> generalMessage;

    private Dimension preferredDimension;

    public FigureMulticastTreePanel(GUINetworkDesign callback, MulticastTree tree, String titleMessage, double carriedTraffic)
    {
        super(callback);
        this.generalMessage = Arrays.asList(titleMessage, "Carried trafffic: " + String.format("%.2f ", carriedTraffic) + " " + callback.getDesign().getDemandTrafficUnitsName(tree.getLayer()));
        this.tree = tree;

        this.preferredDimension = null;
    }

    @Override
    public Dimension getPreferredSize()
    {
        return preferredDimension == null ? DEFAULT_DIMENSION : preferredDimension;
    }

    @Override
    protected void paintComponent(Graphics grphcs)
    {
        final Graphics2D g2d = (Graphics2D) grphcs;
        g2d.setColor(Color.black);

        final int maxHeightOrSizeIcon = 40;
        final int initialXTitle = 20;
        final int initialYTitle = 20;
        /* Initial messages */
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        final int fontHeightTitle = g2d.getFontMetrics().getHeight();
        for (int indexMessage = 0; indexMessage < generalMessage.size(); indexMessage++)
        {
            final String m = generalMessage.get(indexMessage);
            g2d.drawString(m, initialXTitle, initialYTitle + (indexMessage * fontHeightTitle));
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final int regularInterlineSpacePixels = fontMetrics.getHeight();
        final int maxNumberOfTagsPerNode = 4;
        final int maxNumberOfHops = tree.getTreeMaximumPathLengthInHops();
        final int initialTopYOfFirstLineOfNodes = initialYTitle + (generalMessage.size() * fontHeightTitle) + 20;
        final int spaceBetweenVerticalNodes = maxHeightOrSizeIcon + regularInterlineSpacePixels * maxNumberOfTagsPerNode + 20;
        final int xSeparationDnCenters = maxHeightOrSizeIcon * 4;

        this.drawnNodes = new ArrayList<>();
        this.drawnLines = new ArrayList<>();
        final Point dnOriginTopLeftPosition = new Point(initialXTitle, initialTopYOfFirstLineOfNodes);
        final DrawNode ingressDn = new DrawNode(tree.getIngressNode(), tree.getLayer(), maxHeightOrSizeIcon);
        this.drawnNodes.add(ingressDn);
        DrawNode.addNodeToGraphics(g2d, ingressDn, dnOriginTopLeftPosition, fontMetrics, regularInterlineSpacePixels, Color.GREEN);

        int maxWidth = 0;
        int maxHeight = 0;

        BidiMap<Node, DrawNode> nodesToDnMap = new DualHashBidiMap<>();
        nodesToDnMap.put(tree.getIngressNode(), ingressDn);
        for (int numHops = 1; numHops <= maxNumberOfHops; numHops++)
        {
            Set<Link> linksThisHopIndex = new HashSet<>();
            for (Node egressNode : tree.getEgressNodes())
            {
                final List<Link> seqLinks = tree.getSeqLinksToEgressNode(egressNode);
                if (numHops - 1 < seqLinks.size()) linksThisHopIndex.add(seqLinks.get(numHops - 1));
            }
            final Set<Node> destinationNodesThisHopIndex = linksThisHopIndex.stream().map(e -> e.getDestinationNode()).collect(Collectors.toSet());
            if (destinationNodesThisHopIndex.size() != linksThisHopIndex.size()) throw new RuntimeException();

            int yPositionTopDn = initialTopYOfFirstLineOfNodes;
            List<Link> sortedListUpToDown = linksThisHopIndex.stream().sorted(Comparator.comparingInt(e1 -> nodesToDnMap.get(e1.getOriginNode()).getPosTopLeftCornerToSetByPainter().y)).collect(Collectors.toList());
            for (Link e : sortedListUpToDown)
            {
                final Node n = e.getDestinationNode();
                /* Add the node */
                final Point dnTopLeftPosition = new Point(initialXTitle + numHops * xSeparationDnCenters, yPositionTopDn);
                final DrawNode dn = new DrawNode(tree.getIngressNode(), tree.getLayer(), maxHeightOrSizeIcon);
                this.drawnNodes.add(dn);
                DrawNode.addNodeToGraphics(g2d, dn, dnTopLeftPosition, fontMetrics, regularInterlineSpacePixels, tree.getEgressNodes().contains(n) ? Color.CYAN : null);
                nodesToDnMap.put(n, dn);
                yPositionTopDn += spaceBetweenVerticalNodes;

        		/* Add the link */
                final DrawNode dnOrigin = nodesToDnMap.get(e.getOriginNode());
                final DrawLine dlLink = new DrawLine(dnOrigin, dn, e, dnOrigin.posEast(), dn.posWest(), tree.getOccupiedLinkCapacity());
                DrawLine.addLineToGraphics(g2d, dlLink, fontMetrics, regularInterlineSpacePixels);
                drawnLines.add(dlLink);

                if (maxWidth < dnTopLeftPosition.x) maxWidth = dnTopLeftPosition.x;
                if (maxHeight < dnTopLeftPosition.y) maxHeight = dnTopLeftPosition.y;
            }
        }

        this.preferredDimension = new Dimension(maxWidth + XYMARGIN, maxHeight + XYMARGIN);
    }
}

