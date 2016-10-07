package com.net2plan.gui.utils.windows.parent;

import com.net2plan.gui.GUINet2Plan;
import com.net2plan.gui.utils.windows.utils.WindowUtils;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * Created by Jorge San Emeterio on 06/10/2016.
 */
public abstract class GUIWindow
{
    private JFrame window = null;

    public void buildWindow(final JComponent topologyComponent)
    {
        window = new JFrame();

        window.setTitle(this.getTitle());
        window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        window.setSize(600, 600);
        window.setLayout(new BorderLayout());
        window.setVisible(false);

        window.add(topologyComponent, BorderLayout.CENTER);

        URL iconURL = GUINet2Plan.class.getResource("/resources/gui/icon.png");
        ImageIcon icon = new ImageIcon(iconURL);
        window.setIconImage(icon.getImage());
    }

    public void showWindow()
    {
        if (window != null)
        {
            WindowUtils.setWindowRightSide(window);

            window.setVisible(true);
            window.requestFocusInWindow();
        }
    }

    public abstract String getTitle();
}
