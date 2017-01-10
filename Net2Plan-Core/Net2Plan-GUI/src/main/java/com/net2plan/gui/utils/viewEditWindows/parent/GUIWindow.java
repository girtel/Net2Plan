package com.net2plan.gui.utils.viewEditWindows.parent;

import com.net2plan.gui.GUINet2Plan;
import com.net2plan.gui.utils.viewEditWindows.utils.WindowUtils;
import com.net2plan.interfaces.networkDesign.Net2PlanException;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * Created by Jorge San Emeterio on 06/10/2016.
 */
public abstract class GUIWindow extends JFrame
{
    private JComponent component = null;

    public void buildWindow(final JComponent topologyComponent)
    {
        this.component = topologyComponent;

        this.setTitle(this.getTitle());
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.setSize(600, 600);
        this.setLayout(new BorderLayout());
        this.setVisible(false);

        this.add(this.component, BorderLayout.CENTER);

        URL iconURL = GUINet2Plan.class.getResource("/resources/gui/icon.png");
        ImageIcon icon = new ImageIcon(iconURL);
        this.setIconImage(icon.getImage());
    }

    public void showWindow()
    {
        if (component != null)
        {
            WindowUtils.setWindowRightSide(this);

            this.setVisible(true);
            this.requestFocusInWindow();
        } else
        {
            throw new Net2PlanException("Nothing to show on the window.");
        }
    }

    public JComponent getComponent()
    {
        return component;
    }

    public abstract String getTitle();
}
