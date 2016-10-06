package com.net2plan.gui.utils.windows;

import com.net2plan.gui.utils.windows.parent.GUIWindow;

import javax.swing.*;

/**
 * Created by Jorge San Emeterio on 06/10/2016.
 */
public class TopologyWindow extends GUIWindow
{
    public TopologyWindow(final JComponent component)
    {
        super(component);
    }

    @Override
    public String getTitle()
    {
        return "Net2Plan - Topology window";
    }
}
