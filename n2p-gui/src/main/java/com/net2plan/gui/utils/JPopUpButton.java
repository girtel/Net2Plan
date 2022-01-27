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
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * Credits to user luca from stack overflow for his <a href="http://stackoverflow.com/questions/1692677/how-to-create-a-jbutton-with-a-menu">JMenuButton</a>.
 */
public class JPopUpButton extends JToggleButton
{
    private final JPopupMenu popup;

    public JPopUpButton(String name, JPopupMenu menu)
    {
        super(name);
        this.popup = menu;

        this.addActionListener(ev ->
        {
            JToggleButton b = JPopUpButton.this;
            if (b.isSelected())
            {
                popup.show(b, 0, b.getBounds().height);
            } else
            {
                popup.setVisible(false);
            }
        });

        popup.addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
            {
                JPopUpButton.this.setSelected(false);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });

    }
}
