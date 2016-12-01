package com.net2plan.gui.utils.topologyPane.components;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MenuButton extends JToggleButton
{
    final JPopupMenu popup;

    public MenuButton(String name, JPopupMenu menu)
    {
        super(name);
        this.popup = menu;
        addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent ev)
            {
                JToggleButton b = MenuButton.this;
                if (b.isSelected())
                {
                    popup.show(b, 0, b.getBounds().height);
                } else
                {
                    popup.setVisible(false);
                }
            }
        });

        popup.addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
            {
                MenuButton.this.setSelected(false);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e)
            {
            }
        });
    }
}