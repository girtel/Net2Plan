package com.net2plan.gui.utils.viewEditWindows;

import com.net2plan.gui.utils.viewEditWindows.parent.GUIWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jorge San Emeterio on 07/10/2016.
 */
public class WindowController
{
    private static GUIWindow controlWindow;
    private static GUIWindow reportWindow;
    private static GUIWindow offlineWindow;
    private static GUIWindow onlineWindow;

    // WindowToTab.network must always be the first one.
    private static WindowToTab[] tabCorrectOrder = {WindowToTab.network, WindowToTab.offline, WindowToTab.online, WindowToTab.report};

    public static void buildControlWindow(final JComponent component)
    {
        // Control window != Network state tab.
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

    public static void addTabToControlWindow(final String newTabName, final JComponent newTabComponent)
    {
        final JTabbedPane tabPane = (JTabbedPane) controlWindow.getComponent();

        final Map<String, Component> toSortTabs = new HashMap<>();
        toSortTabs.put(newTabName, newTabComponent);

        for (int i = 0; i < tabPane.getTabCount(); i = 0)
        {
            toSortTabs.put(tabPane.getTitleAt(i), tabPane.getComponentAt(i));
            tabPane.remove(i);
        }

        for (int i = 0; i < tabCorrectOrder.length; i++)
        {
            final String tabName = WindowToTab.getTabName(tabCorrectOrder[i]);

            if (toSortTabs.containsKey(tabName))
            {
                final Component tabComponent = toSortTabs.get(tabName);

                tabPane.addTab(tabName, tabComponent);
            }
        }
    }

    public static void buildOfflineWindow(final JComponent component)
    {
        final String tabName = WindowToTab.getTabName(WindowToTab.offline);

        offlineWindow = new GUIWindow()
        {
            @Override
            public String getTitle()
            {
                return "Net2Plan - " + tabName;
            }
        };

        offlineWindow.addWindowListener(new CloseWindowAdapter(tabName, component));

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
        final String tabName = WindowToTab.getTabName(WindowToTab.online);

        onlineWindow = new GUIWindow()
        {
            @Override
            public String getTitle()
            {
                return "Net2Plan - " + tabName;
            }
        };

        onlineWindow.addWindowListener(new CloseWindowAdapter(tabName, component));

        onlineWindow.buildWindow(component);
    }

    public static void showOnlineWindow()
    {
        if (onlineWindow != null)
        {
            onlineWindow.showWindow();
        }
    }

    public static void buildReportWindow(final JComponent component)
    {
        final String tabName = WindowToTab.getTabName(WindowToTab.report);

        reportWindow = new GUIWindow()
        {
            @Override
            public String getTitle()
            {
                return "Net2Plan - " + tabName;
            }
        };

        reportWindow.addWindowListener(new CloseWindowAdapter(tabName, component));

        reportWindow.buildWindow(component);
    }

    public static void showReportWindow()
    {
        if (reportWindow != null)
        {
            reportWindow.showWindow();
        }
    }

    private static class CloseWindowAdapter extends WindowAdapter
    {
        private final String tabName;
        private final JComponent component;

        public CloseWindowAdapter(final String tabName, final JComponent component)
        {
            this.tabName = tabName;
            this.component = component;
        }

        @Override
        public void windowClosing(WindowEvent e)
        {
            addTabToControlWindow(tabName, component);
        }
    }

    public enum WindowToTab
    {
        network(WindowToTab.networkWindowName),
        offline(WindowToTab.offlineWindowName),
        online(WindowToTab.onlineWindowName),
        report(WindowToTab.reportWindowName);

        private final static String networkWindowName = "View/Edit network state";
        private final static String offlineWindowName = "Offline algorithms";
        private final static String onlineWindowName = "Online simulation";
        private final static String reportWindowName = "View reports";


        private final String text;

        WindowToTab(final String text)
        {
            this.text = text;
        }

        public static WindowToTab parseString(final String text)
        {
            switch (text)
            {
                case WindowToTab.networkWindowName:
                    return network;
                case WindowToTab.offlineWindowName:
                    return offline;
                case WindowToTab.onlineWindowName:
                    return online;
                case WindowToTab.reportWindowName:
                    return report;
            }

            return null;
        }

        public static String getTabName(final WindowToTab tab)
        {
            switch (tab)
            {
                case network:
                    return WindowToTab.networkWindowName;
                case offline:
                    return WindowToTab.offlineWindowName;
                case online:
                    return WindowToTab.onlineWindowName;
                case report:
                    return WindowToTab.reportWindowName;
            }

            return null;
        }
    }
}
