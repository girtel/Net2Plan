package com.net2plan.gui.utils;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Credits to user luca from stack overflow for his <a href="http://stackoverflow.com/questions/1692677/how-to-create-a-jbutton-with-a-menu">JMenuButton</a>.
 */
public class JMenuButton extends JToggleButton
{
    final JPopupMenu popup;

    public JMenuButton(String name, JPopupMenu menu)
    {
        super(name);
        this.popup = menu;
        addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent ev)
            {
                JToggleButton b = JMenuButton.this;
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
                JMenuButton.this.setSelected(false);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e)
            {
            }
        });
    }
}