package com.net2plan.gui.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Jorge San Emeterio
 * @date 01-Feb-17
 */
public class JDialogButton extends JComponentButton
{
    private final JDialog dialog;

    public JDialogButton(final String name, final JPanel panel)
    {
        super(name, panel);
        this.dialog = new JDialog();
        this.dialog.setLocation(0, this.getBounds().height);
        this.dialog.setLayout(new BorderLayout());
        this.dialog.add(panel, BorderLayout.CENTER);
        this.dialog.setUndecorated(true);
        this.dialog.pack();

        this.dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                JDialogButton.this.setSelected(false);
            }
        });
    }

    @Override
    void doToggle()
    {
        this.dialog.setVisible(true);
    }
}
