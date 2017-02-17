/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/


package com.net2plan.gui.plugins.networkDesign.topologyPane;

import com.net2plan.gui.utils.networkDesign.GUINode;
import com.net2plan.interfaces.ITopologyCanvas;
import com.net2plan.interfaces.ITopologyCanvasPlugin;
import com.net2plan.gui.plugins.GUINetworkDesign;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Plugin that enables to move nodes.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.1
 */
public class MoveNodePlugin extends MouseAdapter implements ITopologyCanvasPlugin
{
    private GUINetworkDesign callback;
    private ITopologyCanvas canvas;
    private GUINode startVertex;
    private int modifiers;

    /**
     * Default constructor. Default modifier is "left button" click.
     *
     * @param callback Topology callback listening plugin events
     * @since 0.3.1
     */
    public MoveNodePlugin(GUINetworkDesign callback , ITopologyCanvas canvas) {
        this(callback, canvas , MouseEvent.BUTTON1_MASK);
    }

    /**
     * Constructor that allows to set the modifiers.
     *
     * @param callback  Topology callback listening plugin events
     * @param modifiers Mouse event modifiers to activate this functionality
     * @since 0.3.1
     */
    public MoveNodePlugin(GUINetworkDesign callback, ITopologyCanvas canvas , int modifiers) {
        setModifiers(modifiers);
        this.callback = callback;
        this.canvas = canvas;

        startVertex = null;
    }

    @Override
    public boolean checkModifiers(MouseEvent e) {
        return e.getModifiers() == getModifiers();
    }

    @Override
    public int getModifiers() {
        return modifiers;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (startVertex != null) {
            final Point p = e.getPoint();

            callback.moveNodeTo(startVertex, p);

            e.consume();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (checkModifiers(e)) {
            GUINode node = canvas.getVertex(e);
            if (node != null) {
                callback.getVisualizationState().pickNode(node.getAssociatedNetPlanNode());
                callback.updateVisualizationAfterPick();
                startVertex = node;
                e.consume();
            } else {
                callback.resetPickedStateAndUpdateView();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        startVertex = null;
    }

    @Override
    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }
}
