package com.net2plan.gui.utils.focusPane;

import com.net2plan.gui.utils.IVisualizationCallback;
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

    public FigureSRGSequencePanel(final IVisualizationCallback callback, final SharedRiskGroup srg, final String... titleMessage)
    {
        super(callback);
        this.riskGroup = srg;
        this.generalMessage = Arrays.asList(titleMessage);
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
        int graphicsRow = 0;
        for (String s : generalMessage)
        {
            final String m = generalMessage.get(graphicsRow);
            g2d.drawString(m, maxIconSize, maxIconSize + ((graphicsRow++) * fontHeightTitle));
        }

        // Draw node header
        g2d.drawString("Nodes", maxIconSize, maxIconSize + (fontHeightTitle * (++graphicsRow)));

        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final int regularInterlineSpacePixels = fontMetrics.getHeight();

        final int topCoordinateLineNodes = maxIconSize + (generalMessage.size() * fontHeightTitle) + (maxNumberOfTagsPerNodeNorResource * regularInterlineSpacePixels) * ++graphicsRow;
        final Point initialDnTopLeftPosition = new Point(maxIconSize, topCoordinateLineNodes);
        final int xSeparationDnCenters = maxIconSize * 3;

        // Drawing nodes
        int column = 0;
        for (Node node : riskGroup.getNodes())
        {
            final DrawNode drawNode = new DrawNode(node, callback.getDesign().getNetworkLayerDefault(), maxIconSize);
            drawnNodes.add(drawNode);
            DrawNode.addNodeToGraphics(g2d, drawNode, new Point(initialDnTopLeftPosition.x + (xSeparationDnCenters * (column++)), initialDnTopLeftPosition.y), fontMetrics, regularInterlineSpacePixels);
        }

        // Draw layers
        riskGroup.get
    }
}
