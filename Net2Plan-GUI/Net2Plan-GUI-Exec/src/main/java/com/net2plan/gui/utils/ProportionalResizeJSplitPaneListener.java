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


package com.net2plan.gui.utils;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Listener that allows maintaining the divider's location constant
 * proportionally when the window it is resized.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ProportionalResizeJSplitPaneListener implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent changeEvent) {
        JSplitPane src = (JSplitPane) changeEvent.getSource();
        String propertyName = changeEvent.getPropertyName();
        if (propertyName.equals(JSplitPane.LAST_DIVIDER_LOCATION_PROPERTY)) {
            if (src.getOrientation() == JSplitPane.HORIZONTAL_SPLIT)
                src.setResizeWeight(src.getDividerLocation() / src.getSize().getWidth());
            else
                src.setResizeWeight(src.getDividerLocation() / src.getSize().getHeight());
        }
    }
}
