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
import java.awt.*;

/**
 * <p>This class allows to see the full data when accessing to the popup of a
 * {@code JComboBox}, in case the {@code JComboBox} object is narrower
 * than its preferred width.</p>
 * <p>Credits to Santhosh Kumar for his workaround to narrow {@code JComboBox}
 * issues (<a href='http://www.jroller.com/santhosh/entry/make_jcombobox_popup_wide_enough'>Make JComboBox popup wide enough</a>)</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class WiderJComboBox extends JComboBox {
    private boolean layingOut = false;

    @Override
    public void doLayout() {
        try {
            layingOut = true;
            super.doLayout();
        } finally {
            layingOut = false;
        }
    }

    @Override
    public Dimension getSize() {
        Dimension dim = super.getSize();
        if (!layingOut) dim.width = Math.max(dim.width, getPreferredSize().width);
        return dim;
    }
}
