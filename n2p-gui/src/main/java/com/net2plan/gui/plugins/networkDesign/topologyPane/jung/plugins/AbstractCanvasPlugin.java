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


package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.plugins;

import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvasPlugin;
import edu.uci.ics.jung.visualization.control.AbstractGraphMousePlugin;

/**
 * Abstract class for JUNG plugins.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public abstract class AbstractCanvasPlugin extends AbstractGraphMousePlugin implements ITopologyCanvasPlugin
{
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

    /**
     * Checks a canvas has been assigned to the plugin.
     *
     * @since 0.2.3
     */
    public void checkCanvas() {
        if (canvas == null) throw new RuntimeException("No canvas attached!");
    }
}
