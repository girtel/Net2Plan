package com.net2plan.gui.plugins.networkDesign.viewEditWindows;

import com.net2plan.gui.plugins.networkDesign.viewEditWindows.parent.GUIWindow;

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
    private static GUIWindow tableControlWindow;

    private static GUIWindow reportWindow;
    private static GUIWindow offlineWindow;
    private static GUIWindow onlineWindow;
    private static GUIWindow whatifWindow;

    // WindowToTab.network must always be the first one.
    private final static WindowToTab[] tabCorrectOrder = {WindowToTab.network, WindowToTab.offline, WindowToTab.online, WindowToTab.whatif , WindowToTab.report};

    public static void buildTableControlWindow(final JComponent component)
    {
        // Control window != Network state tab.
        tableControlWindow = new GUIWindow()
        {
            @Override
            public String getTitle()
            {
                return "Net2Plan - Design tables and control window";
            }
        };

        tableControlWindow.buildWindow(component);
    }

    public static void showTablesWindow(final boolean gainFocus)
    {
        if (tableControlWindow != null)
        {
            if (gainFocus)
            {
                tableControlWindow.showWindow();
            } else
            {
                tableControlWindow.setFocusableWindowState(false);
                tableControlWindow.showWindow();
                tableControlWindow.setFocusableWindowState(true);
            }
        }
    }

    private static void addTabToControlWindow(final String newTabName, final JComponent newTabComponent)
    {
        final JTabbedPane tabPane = (JTabbedPane) tableControlWindow.getComponent();

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

    public static void showOfflineWindow(final boolean gainFocus)
    {
        if (offlineWindow != null)
        {
            if (gainFocus)
            {
                offlineWindow.showWindow();
            } else
            {
                offlineWindow.setFocusableWindowState(false);
                offlineWindow.showWindow();
                offlineWindow.setFocusableWindowState(true);
            }
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

    public static void buildWhatifWindow(final JComponent component)
    {
        final String tabName = WindowToTab.getTabName(WindowToTab.whatif);

        whatifWindow = new GUIWindow()
        {
            @Override
            public String getTitle()
            {
                return "Net2Plan - " + tabName;
            }
        };

        whatifWindow.addWindowListener(new CloseWindowAdapter(tabName, component));

        whatifWindow.buildWindow(component);
    }

    public static void showOnlineWindow(final boolean gainFocus)
    {
        if (onlineWindow != null)
        {
            if (gainFocus)
            {
                onlineWindow.showWindow();
            } else
            {
                onlineWindow.setFocusableWindowState(false);
                onlineWindow.showWindow();
                onlineWindow.setFocusableWindowState(true);
            }
        }
    }

    public static void showWhatifWindow(final boolean gainFocus)
    {
        if (whatifWindow != null)
        {
            if (gainFocus)
            {
                whatifWindow.showWindow();
            } else
            {
                whatifWindow.setFocusableWindowState(false);
                whatifWindow.showWindow();
                whatifWindow.setFocusableWindowState(true);
            }
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

    public static void showReportWindow(final boolean gainFocus)
    {
        if (reportWindow != null)
        {
            if (gainFocus)
            {
                reportWindow.showWindow();
            } else
            {
                reportWindow.setFocusableWindowState(false);
                reportWindow.showWindow();
                reportWindow.setFocusableWindowState(true);
            }
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
        whatif(WindowToTab.whatifWindowName),
        report(WindowToTab.reportWindowName);

        private final static String networkWindowName = "View/Edit network state";
        private final static String offlineWindowName = "Offline algorithms";
        private final static String onlineWindowName = "Online simulation";
        private final static String whatifWindowName = "What-if analysis";
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
                case WindowToTab.whatifWindowName:
                    return whatif;
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
                case whatif:
                    return WindowToTab.whatifWindowName;
                case report:
                    return WindowToTab.reportWindowName;
            }

            return null;
        }
    }
}
