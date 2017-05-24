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


package com.net2plan.gui.plugins.networkDesign.interfaces;

import java.awt.event.MouseEvent;

/**
 * Interface to identify plugins for topology canvas.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public interface ITopologyCanvasPlugin {
    /**
     * Compares the set modifiers against those of the supplied event.
     *
     * @param e an event to compare to
     * @return whether the member modifers match the event modifiers
     * @since 0.2.3
     */
    boolean checkModifiers(MouseEvent e);

//    /**
//     * Returns the canvas associated to the plugin.
//     *
//     * @return Canvas
//     * @since 0.3.1
//     */
//    public ITopologyCanvas getCanvas();

    /**
     * Returns the mouse event modifiers that will activate this plugin.
     *
     * @return modifiers
     * @since 0.2.3
     */
    public int getModifiers();

//    /**
//     * Sets the canvas for this plugin.
//     *
//     * @param canvas Reference to the canvas
//     * @since 0.3.1
//     */
//    public void setCanvas(ITopologyCanvas canvas);

    /**
     * Sets the mouse event modifiers that will activate this plugin.
     *
     * @param modifiers Modifiers
     * @since 0.2.3
     */
    public void setModifiers(int modifiers);
}
