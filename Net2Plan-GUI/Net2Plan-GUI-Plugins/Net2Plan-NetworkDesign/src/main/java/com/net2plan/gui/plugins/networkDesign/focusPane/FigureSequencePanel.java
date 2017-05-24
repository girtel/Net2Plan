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
    protected final GUINetworkDesign callback;
    protected List<DrawNode> drawnNodes;
    protected List<DrawLine> drawnLines;

    protected final int DEFAULT_WIDTH = 600, DEFAULT_HEIGHT = 600;
    protected final Dimension DEFAULT_DIMENSION = new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    protected final int XYMARGIN = 100;

    public FigureSequencePanel(GUINetworkDesign callback)
    {
        this.callback = callback;
        this.drawnNodes = new ArrayList<>();
        this.drawnLines = new ArrayList<>();

        this.addMouseListener(new MouseAdapterFocusPanel());
        this.addMouseMotionListener(new MouseMotionFocusPanel());
    }

    @Override
    public abstract Dimension getPreferredSize();

    @Override
    protected abstract void paintComponent(Graphics graphics);

    private class MouseMotionFocusPanel extends MouseMotionAdapter
    {
        @Override
        public void mouseMoved(MouseEvent e)
        {
            for (DrawNode drawnNode : drawnNodes)
            {
                if (drawnNode.getShapeIconToSetByPainter().contains(e.getPoint()))
                {
                    FigureSequencePanel.this.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    return;
                }
            }

            for (DrawLine drawnLine : drawnLines)
            {
                if (drawnLine.getShapeLineToCreateByPainter().contains(e.getPoint()))
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
                if (dn.getShapeIconToSetByPainter().contains(me.getPoint()))
                    FocusPane.processMouseClickInternalLink("node" + dn.getAssociatedElement().getId(), callback);
                for (int labelIndex = 0; labelIndex < dn.getLabels().size(); labelIndex++)
                    if (dn.getShapesLabelsToCreateByPainter().get(labelIndex).contains(me.getPoint()))
                        FocusPane.processMouseClickInternalLink(dn.getUrlsLabels().get(labelIndex), callback);
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
