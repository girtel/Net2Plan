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


package com.net2plan.gui.utils.topologyPane;

import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.internal.plugins.ITopologyCanvas;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * This plugin allows to pan the graph along the canvas.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.1
 */
public class PanGraphPlugin extends MouseAdapter implements ITopologyCanvasPlugin {
    private final INetworkCallback callback;
    private final Cursor cursor;

    private ITopologyCanvas canvas;
    private Point down, initialPoint;
    private int modifiers;
    private Cursor originalCursor;

    /**
     * Default constructor.
     *
     * @param callback  Topology callback listening plugin events
     * @param modifiers Mouse event modifiers to activate this functionality
     * @since 0.3.1
     */
    public PanGraphPlugin(INetworkCallback callback, ITopologyCanvas canvas , int modifiers) {
        setModifiers(modifiers);
        this.callback = callback;
        this.canvas = canvas;

        originalCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        down = null;
        initialPoint = null;
    }

    @Override
    public boolean checkModifiers(MouseEvent e) {
        return e.getModifiers() == modifiers;
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
        if (down != null) {
            getCanvas().getInternalVisualizationController().setCursor(cursor);
            getCanvas().panTo(down, e.getPoint());
            down = e.getPoint();
            e.consume();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (checkModifiers(e)) {
            GUINode gn = canvas.getNode(e);
            GUILink gl = canvas.getLink(e);
            if (gn == null && gl == null) {
                down = e.getPoint();
                initialPoint = e.getPoint();
                canvas.getInternalVisualizationController().setCursor(cursor);
                e.consume();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (initialPoint != null && initialPoint.equals(e.getPoint()))
            callback.resetPickedStateAndUpdateView();

        down = null;
        initialPoint = null;
        getCanvas().getInternalVisualizationController().setCursor(originalCursor);
    }

    @Override
    public void setCanvas(ITopologyCanvas canvas) {
        this.canvas = canvas;
        originalCursor = this.canvas.getInternalVisualizationController().getCursor();
    }

    @Override
    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }
}
