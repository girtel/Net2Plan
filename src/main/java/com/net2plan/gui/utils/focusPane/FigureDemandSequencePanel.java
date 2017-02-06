package com.net2plan.gui.utils.focusPane;

import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.interfaces.networkDesign.*;

import javax.swing.*;
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
    final BasicStroke lineStroke;

    public FigureDemandSequencePanel(final IVisualizationCallback callback, final Demand demand, final String titleMessage)
    {
        super(callback);
        this.demand = demand;
        this.generalMessage = Arrays.asList(titleMessage);
        this.lineStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f);
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        final Graphics2D g2d = (Graphics2D) graphics;
        g2d.setColor(Color.black);

        final int maxHeightOrSizeIcon = 40;
        final int maxNumberOfTagsPerNodeNorResource = 1;

    	/* Initial messages */
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        final int fontHeightTitle = g2d.getFontMetrics().getHeight();
        for (int indexMessage = 0; indexMessage < generalMessage.size(); indexMessage++)
        {
            final String m = generalMessage.get(indexMessage);
            g2d.drawString(m, maxHeightOrSizeIcon, maxHeightOrSizeIcon + (indexMessage * fontHeightTitle));
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final int regularInterlineSpacePixels = fontMetrics.getHeight();

        final DrawNode ingressNode = new DrawNode(demand.getIngressNode(), demand.getLayer(), maxHeightOrSizeIcon);
        final DrawNode egressNode = new DrawNode(demand.getEgressNode(), demand.getLayer(), maxHeightOrSizeIcon);

        final int topCoordinateLineNodes = maxHeightOrSizeIcon + (generalMessage.size() * fontHeightTitle) + (maxNumberOfTagsPerNodeNorResource * regularInterlineSpacePixels);
        final int topCoordinateLineResources = topCoordinateLineNodes + maxHeightOrSizeIcon * 4;
        final Point initialDnTopLeftPosition = new Point(maxHeightOrSizeIcon, topCoordinateLineNodes);
        final int xSeparationDnCenters = maxHeightOrSizeIcon * 3;

    	/* Initial dn */
        DrawNode.addNodeToGraphics(g2d, ingressNode, initialDnTopLeftPosition, fontMetrics, regularInterlineSpacePixels);
        DrawNode.addNodeToGraphics(g2d, egressNode, new Point(initialDnTopLeftPosition.x + xSeparationDnCenters, initialDnTopLeftPosition.y), fontMetrics, regularInterlineSpacePixels);

        drawnNodes.add(ingressNode);
        drawnNodes.add(egressNode);

        // NOTE: Resources?
        final DrawLine link = new DrawLine(ingressNode, egressNode);
        DrawLine.addLineToGraphics(g2d, link, fontMetrics, regularInterlineSpacePixels, lineStroke);

        drawnLines.add(link);
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(600, 600);
    }
}
