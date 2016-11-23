package com.net2plan.gui.utils.windows;

import com.net2plan.gui.utils.windows.parent.GUIWindow;

import javax.swing.*;

/**
 * Created by Jorge San Emeterio on 07/10/2016.
 */
public class WindowController
{
    private static GUIWindow controlWindow;

    public static void buildControlWindow(final JComponent component)
    {
        controlWindow = new GUIWindow()
        {
            @Override
            public String getTitle()
            {
                return "Net2Plan - Control window";
            }
        };

        controlWindow.buildWindow(component);
    }

    public static void showControlWindow()
    {
        if (controlWindow != null)
        {
            controlWindow.showWindow();
        }
    }
}
