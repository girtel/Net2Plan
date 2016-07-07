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

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Plugin for the popup menu of the canvas.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.1
 */
public class PopupMenuPlugin extends MouseAdapter implements ITopologyCanvasPlugin {
    private final INetworkCallback callback;
    private ITopologyCanvas canvas;

    /**
     * Default constructor.
     *
     * @param callback Reference to the class handling change events.
     * @since 0.3.1
     */
    public PopupMenuPlugin(INetworkCallback callback) {
        this.callback = callback;
    }

    @Override
    public boolean checkModifiers(MouseEvent e) {
        return e.isPopupTrigger();
    }

    @Override
    public ITopologyCanvas getCanvas() {
        return canvas;
    }

    @Override
    public int getModifiers() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (checkModifiers(e)) {
            final Point p = e.getPoint();
            final Point2D pp = getCanvas().convertViewCoordinatesToRealCoordinates(p);
            final long nodeId = getCanvas().getNode(e);
            final long linkId = getCanvas().getLink(e);

            List<JComponent> actions;
            if (nodeId != -1) {
                actions = callback.getNodeActions(nodeId, pp);
            } else if (linkId != -1) {
                actions = callback.getLinkActions(linkId, pp);
            } else {
                getCanvas().resetPickedAndUserDefinedColorState();
                actions = callback.getCanvasActions(pp);
            }

            if (actions == null || actions.isEmpty()) return;

            final JPopupMenu popup = new JPopupMenu();
            for (JComponent action : actions)
                popup.add(action);

            popup.show(getCanvas().getInternalComponent(), e.getX(), e.getY());
            e.consume();
        }
    }

    @Override
    public void setCanvas(ITopologyCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void setModifiers(int modifiers) {
        throw new UnsupportedOperationException("Not supported yet");
    }
}
