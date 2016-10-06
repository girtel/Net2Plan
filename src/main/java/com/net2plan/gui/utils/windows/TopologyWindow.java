package com.net2plan.gui.utils.windows;

import com.net2plan.gui.utils.windows.parent.GUIWindow;

import javax.swing.*;

/**
 * Created by Jorge San Emeterio on 06/10/2016.
 */
public class TopologyWindow extends GUIWindow
{
    private static final String title = "Net2Plan - Topology window";

    public static void buildWindow(JComponent component)
    {
        buildWindow(component, title);
    }
}
