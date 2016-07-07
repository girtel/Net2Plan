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


package com.net2plan.gui.utils;

import javax.swing.*;
import java.awt.*;

/**
 * <p>This class allows to see the full data when accessing to the popup of a
 * {@code JComboBox}, in case the {@code JComboBox} object is narrower
 * than its preferred width.</p>
 * <p>
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
