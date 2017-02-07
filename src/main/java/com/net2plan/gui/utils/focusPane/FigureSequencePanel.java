package com.net2plan.gui.utils.focusPane;

import com.net2plan.gui.utils.IVisualizationCallback;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jorge San Emeterio
 * @date 06-Feb-17
 */
public abstract class FigureSequencePanel extends JPanel
{
    protected final IVisualizationCallback callback;
    protected List<DrawNode> drawnNodes;
    protected List<DrawLine> drawnLines;

    public FigureSequencePanel(IVisualizationCallback callback)
    {
        this.callback = callback;
        this.drawnNodes = new ArrayList<>();
        this.drawnLines = new ArrayList<>();

        this.addMouseListener(new MouseAdapterFocusPanel());
        this.addMouseMotionListener(new MouseMotionFocusPanel());
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(600, 600);
    }

    @Override
    protected abstract void paintComponent(Graphics graphics);

    private class MouseMotionFocusPanel extends MouseMotionAdapter
    {
        @Override
        public void mouseMoved(MouseEvent e)
        {
            for (DrawNode drawnNode : drawnNodes)
            {
                if (drawnNode.shapeIconToSetByPainter.contains(e.getPoint()))
                {
                    FigureSequencePanel.this.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    return;
                }
            }

            FigureSequencePanel.this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private class MouseAdapterFocusPanel extends MouseAdapter
    {
        @Override
        public void mouseClicked(MouseEvent me)
        {
            super.mouseClicked(me);
            for (DrawNode dn : drawnNodes)
            {
                if (dn.shapeIconToSetByPainter.contains(me.getPoint()))
                    FocusPane.processMouseClickInternalLink("node" + dn.associatedElement.getId(), callback);
                for (int labelIndex = 0; labelIndex < dn.labels.size(); labelIndex++)
                    if (dn.shapesLabelsToCreateByPainter.get(labelIndex).contains(me.getPoint()))
                        FocusPane.processMouseClickInternalLink(dn.urlsLabels.get(labelIndex), callback);
            }
            for (DrawLine dl : drawnLines)
            {
                if (dl.getShapeLineToCreateByPainter().contains(me.getPoint()))
                    FocusPane.processMouseClickInternalLink("link" + dl.getAssociatedElement().getId(), callback);
                for (int labelIndex = 0; labelIndex < dl.getLabels().size(); labelIndex++)
                    if (dl.getShapesLabelstoCreateByPainter().get(labelIndex).contains(me.getPoint()))
                        FocusPane.processMouseClickInternalLink(dl.getUrlsLabels().get(labelIndex), callback);
            }
        }
    }
}
