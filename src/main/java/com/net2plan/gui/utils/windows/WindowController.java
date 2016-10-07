package com.net2plan.gui.utils.windows;

import com.net2plan.gui.utils.windows.parent.GUIWindow;

import javax.swing.*;

/**
 * Created by Jorge San Emeterio on 07/10/2016.
 */
public class WindowController
{
    private static GUIWindow topologyWindow;
    private static GUIWindow reportWindow;
    private static GUIWindow offlineWindow;
    private static GUIWindow onlineWindow;

    public static void buildTopologyWindow(final JComponent component)
    {
        topologyWindow = new GUIWindow()
        {
            @Override
            public String getTitle()
            {
                return "Net2Plan - Network state window";
            }
        };

        topologyWindow.buildWindow(component);
    }

    public static void showTopologyWindow()
    {
        if (topologyWindow != null)
        {
            topologyWindow.showWindow();
        }
    }

    public static void buildReportWindow(final JComponent component)
    {
        reportWindow = new GUIWindow()
        {
            @Override
            public String getTitle()
            {
                return "Net2Plan - Report window";
            }
        };

        reportWindow.buildWindow(component);
    }

    public static void showReportWindow()
    {
        if (reportWindow != null)
        {
            reportWindow.showWindow();
        }
    }

    public static void buildOfflineWindow(final JComponent component)
    {
        offlineWindow = new GUIWindow()
        {
            @Override
            public String getTitle()
            {
                return "Net2Plan - Offline design window";
            }
        };

        offlineWindow.buildWindow(component);
    }

    public static void showOfflineWindow()
    {
        if (offlineWindow != null)
        {
            offlineWindow.showWindow();
        }
    }

    public static void buildOnlineWindow(final JComponent component)
    {
        onlineWindow = new GUIWindow()
        {
            @Override
            public String getTitle()
            {
                return "Net2Plan - Online simulation window";
            }
        };

        onlineWindow.buildWindow(component);
    }

    public static void showOnlineWindow()
    {
        if (onlineWindow != null)
        {
            onlineWindow.showWindow();
        }
    }

}
