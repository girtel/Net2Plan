package com.net2plan.gui.utils.focusPane;

import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @author Jorge San Emeterio
 * @date 07-Feb-17
 */
public class FigureSRGSequencePanel extends FigureSequencePanel
{
    private final SharedRiskGroup riskGroup;
    private final List<String> generalMessage;

    private final Font headerFont, plainFont;

    public FigureSRGSequencePanel(final IVisualizationCallback callback, final SharedRiskGroup srg, final String... titleMessage)
    {
        super(callback);
        this.riskGroup = srg;
        this.generalMessage = Arrays.asList(titleMessage);
        this.plainFont = new Font("Arial", Font.PLAIN, 10);
        this.headerFont = new Font("Arial", Font.BOLD, 12);
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        int graphicsRow = 0;
        int graphicsColumn = 0;

        final Graphics2D g2d = (Graphics2D) graphics;
        g2d.setColor(Color.black);

        final int maxIconSize = 40;
        final int maxNumberOfTagsPerNodeNorResource = 1;

    	/* Initial messages */
        g2d.setFont(headerFont);
        final int fontHeightTitle = g2d.getFontMetrics().getHeight();
        for (String s : generalMessage)
        {
            g2d.drawString(s, maxIconSize, maxIconSize + ((graphicsRow++) * fontHeightTitle));
        }

        // Draw node header
        g2d.drawString("Nodes", maxIconSize, maxIconSize + (fontHeightTitle * (++graphicsRow)));

        g2d.setFont(plainFont);
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final int regularInterlineSpacePixels = fontMetrics.getHeight();

        int topCoordinateLineNodes = maxIconSize + (generalMessage.size() * fontHeightTitle) + (maxNumberOfTagsPerNodeNorResource * regularInterlineSpacePixels) * ++graphicsRow;
        Point initialDnTopLeftPosition = new Point(maxIconSize, topCoordinateLineNodes);
        int xSeparationDnCenters = maxIconSize * 3;

        // Drawing nodes
        graphicsColumn = 0;
        for (Node node : riskGroup.getNodes())
        {
            final DrawNode drawNode = new DrawNode(node, callback.getDesign().getNetworkLayerDefault(), maxIconSize);
            drawnNodes.add(drawNode);
            DrawNode.addNodeToGraphics(g2d, drawNode, new Point(initialDnTopLeftPosition.x + (xSeparationDnCenters * (graphicsColumn++)), initialDnTopLeftPosition.y), fontMetrics, regularInterlineSpacePixels, null);
        }

        // Draw layers

        // Building SRG layer
        final Set<NetworkLayer> layers = new HashSet<>();

        for (Link link : riskGroup.getLinksAllLayers())
        {
            layers.add(link.getLayer());
        }

        // Drawing each layer
        for (NetworkLayer layer : layers)
        {
            final Set<Link> links = riskGroup.getLinks(layer);

            // Drawing layer header
            g2d.setFont(headerFont);
            graphicsRow += 5;
            g2d.drawString("Links, layer " + layer.getName(), maxIconSize, maxIconSize + ((graphicsRow) * fontHeightTitle));

            g2d.setFont(plainFont);

            graphicsRow += 2;
            for (Link link : links)
            {
                final DrawNode ingressNode = new DrawNode(link.getOriginNode(), layer, maxIconSize);
                final DrawNode egressNode = new DrawNode(link.getDestinationNode(), layer, maxIconSize);

                topCoordinateLineNodes = maxIconSize + (generalMessage.size() * fontHeightTitle) + (maxNumberOfTagsPerNodeNorResource * regularInterlineSpacePixels) * graphicsRow;
                initialDnTopLeftPosition = new Point(maxIconSize, topCoordinateLineNodes);
                xSeparationDnCenters = maxIconSize * 3;

                DrawNode.addNodeToGraphics(g2d, ingressNode, new Point( new Point(initialDnTopLeftPosition.x, initialDnTopLeftPosition.y)), fontMetrics, regularInterlineSpacePixels, null);
                DrawNode.addNodeToGraphics(g2d, egressNode, new Point(initialDnTopLeftPosition.x + (xSeparationDnCenters), initialDnTopLeftPosition.y), fontMetrics, regularInterlineSpacePixels, null);

                drawnNodes.add(ingressNode);
                drawnNodes.add(egressNode);

                final DrawLine drawLine = new DrawLine(ingressNode, egressNode, ingressNode.posEast(), egressNode.posWest());
                DrawLine.addLineToGraphics(g2d, drawLine, fontMetrics, regularInterlineSpacePixels);

                drawnLines.add(drawLine);

                graphicsRow += 5;
            }
        }
    }
}
