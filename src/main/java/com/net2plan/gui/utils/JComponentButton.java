package com.net2plan.gui.utils;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Jorge San Emeterio
 * @date 01-Feb-17
 */
public abstract class JComponentButton extends JToggleButton
{
    protected final JComponent innerComponent;

    public JComponentButton(final String name, final JComponent component)
    {
        super(name);
        this.innerComponent = component;

        this.addActionListener(e ->
        {
            final JToggleButton toggleButton = JComponentButton.this;

            if (toggleButton.isSelected())
            {
                JComponentButton.this.doToggle();
            } else
            {
                innerComponent.setVisible(false);
            }
        });
    }

    abstract void doToggle();
}