package com.net2plan.gui.utils;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * @author Jorge San Emeterio
 * @date 01-Feb-17
 */
public class JPopUpButton extends JComponentButton
{
    private final JPopupMenu popup;

    public JPopUpButton(String name, JPopupMenu menu)
    {
        super(name, menu);
        this.popup = (JPopupMenu) innerComponent;

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

    @Override
    void doToggle()
    {
        popup.show(this, 0, this.getBounds().height);
    }
}
