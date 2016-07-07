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


package com.net2plan.gui.utils.topology.jung;

import com.net2plan.gui.utils.topology.ITopologyCanvasPlugin;
import com.net2plan.internal.plugins.ITopologyCanvas;
import edu.uci.ics.jung.visualization.control.AbstractGraphMousePlugin;

/**
 * Abstract class for JUNG plugins.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public abstract class AbstractCanvasPlugin extends AbstractGraphMousePlugin implements ITopologyCanvasPlugin {
    /**
     * Reference to the topology canvas.
     *
     * @since 0.2.3
     */
    protected ITopologyCanvas canvas = null;

    /**
     * Default constructor.
     *
     * @param modifiers Mouse event modifiers to activate this functionality
     * @since 0.2.3
     */
    public AbstractCanvasPlugin(int modifiers) {
        super(modifiers);
    }

    @Override
    public ITopologyCanvas getCanvas() {
        checkCanvas();
        return canvas;
    }

    /**
     * Sets the canvas for this plugin.
     *
     * @param canvas Reference to the canvas
     * @since 0.2.3
     */
    @Override
    public void setCanvas(ITopologyCanvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Checks a canvas has been assigned to the plugin.
     *
     * @since 0.2.3
     */
    public void checkCanvas() {
        if (canvas == null) throw new RuntimeException("No canvas attached!");
    }
}
