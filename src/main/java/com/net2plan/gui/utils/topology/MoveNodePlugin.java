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


package com.net2plan.gui.utils.topology;

import com.net2plan.internal.plugins.ITopologyCanvas;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Plugin that enables to move nodes.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.1
 */
public class MoveNodePlugin extends MouseAdapter implements ITopologyCanvasPlugin {
    private INetworkCallback callback;
    private ITopologyCanvas canvas;
    private long startVertex;
    private int modifiers;

    /**
     * Default constructor. Default modifier is "left button" click.
     *
     * @param callback Topology callback listening plugin events
     * @since 0.3.1
     */
    public MoveNodePlugin(INetworkCallback callback) {
        this(callback, MouseEvent.BUTTON1_MASK);
    }

    /**
     * Constructor that allows to set the modifiers.
     *
     * @param callback  Topology callback listening plugin events
     * @param modifiers Mouse event modifiers to activate this functionality
     * @since 0.3.1
     */
    public MoveNodePlugin(INetworkCallback callback, int modifiers) {
        setModifiers(modifiers);
        this.callback = callback;

        startVertex = -1;
    }

    @Override
    public boolean checkModifiers(MouseEvent e) {
        return e.getModifiers() == getModifiers();
    }

    @Override
    public ITopologyCanvas getCanvas() {
        return canvas;
    }

    @Override
    public int getModifiers() {
        return modifiers;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        System.out.println("mouseDragged: " + e + ", startVertex: " + startVertex);
        if (startVertex != -1) {
            Point p = e.getPoint();
            callback.moveNode(startVertex, getCanvas().convertViewCoordinatesToRealCoordinates(p));
            e.consume();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (checkModifiers(e)) {
            long nodeId = getCanvas().getNode(e);
            if (nodeId != -1) {
                callback.showNode(nodeId);
                startVertex = nodeId;
                e.consume();
            } else {
                callback.resetView();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        startVertex = -1;
    }

    @Override
    public void setCanvas(ITopologyCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }
}
