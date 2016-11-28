package com.net2plan.gui.utils.windows;

import com.net2plan.gui.utils.windows.parent.GUIWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Created by Jorge San Emeterio on 07/10/2016.
 */
public class WindowController
{
    private static GUIWindow controlWindow;
    private static GUIWindow reportWindow;
    private static GUIWindow offlineWindow;
    private static GUIWindow onlineWindow;

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

    public static void addTabToControlWindow(final String tabName, final JComponent component)
    {
        final JTabbedPane pane = (JTabbedPane) controlWindow.getComponent();

        pane.addTab(tabName, component);
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

        reportWindow.addWindowListener(new CloseWindowAdapter(WindowTab.getTabName(WindowTab.report), component));

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

        offlineWindow.addWindowListener(new CloseWindowAdapter(WindowTab.getTabName(WindowTab.offline), component));

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

        onlineWindow.addWindowListener(new CloseWindowAdapter(WindowTab.getTabName(WindowTab.online), component));

        onlineWindow.buildWindow(component);
    }

    public static void showOnlineWindow()
    {
        if (onlineWindow != null)
        {
            onlineWindow.showWindow();
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

    public enum WindowTab
    {
        control ("View/Edit network state"),
        offline ("Algorithm execution"),
        online ("Online simulation"),
        report ("View reports");

        private final String text;

        private WindowTab(final String text)
        {
            this.text = text;
        }

        public static WindowTab parseString(final String text)
        {
            switch (text)
            {
                case "View/Edit network state":
                    return control;
                case "Algorithm execution":
                    return offline;
                case "Online simulation":
                    return online;
                case "View reports":
                    return report;
            }

            return null;
        }

        public static String getTabName(final WindowTab tab)
        {
            switch (tab)
            {
                case control:
                    return "View/Edit network state";
                case offline:
                    return "Algorithm execution";
                case online:
                    return "Online simulation";
                case report:
                    return "View reports";
            }

            return null;
        }
    }
}
