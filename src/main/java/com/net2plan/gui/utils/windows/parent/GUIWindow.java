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
    private static JFrame window = new JFrame();

    public static void buildWindow(final JComponent topologyComponent, final String title)
    {
        window = new JFrame();

        window.setTitle(title);
        window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        window.setSize(600, 600);
        window.setLayout(new BorderLayout());
        window.setVisible(false);

        window.add(topologyComponent, BorderLayout.CENTER);

        URL iconURL = GUINet2Plan.class.getResource("/resources/gui/icon.png");
        ImageIcon icon = new ImageIcon(iconURL);
        window.setIconImage(icon.getImage());
    }

    public static void showWindow()
    {
        WindowUtils.setWindowRightSide(window);

        window.setVisible(true);
        window.requestFocusInWindow();
    }

    public static JFrame getWindow()
    {
        return window;
    }
}
