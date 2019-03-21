package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.treeView;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.focusPane.FocusPane;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.monitoring.MonitoringGraphPane;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class TreePanel extends JPanel
{
    private final JTabbedPane tabbedPane;

    private final FocusPane focusView;
    private final MonitoringGraphPane monitGraphView;

    private final TreeToolBar focusToolBar;
    private final TreeToolBar monitGraphToolBar;

    public TreePanel(GUINetworkDesign callback)
    {
        super();

        this.setLayout(new BorderLayout());

        this.focusView = new FocusPane(callback);
        this.monitGraphView = new MonitoringGraphPane(callback);

        this.focusToolBar = new TreeToolBar(this);
        this.monitGraphToolBar = new TreeToolBar(this);

        final JPanel focusPanel = new JPanel(new BorderLayout());
        focusPanel.add(new JScrollPane(focusView), BorderLayout.CENTER);
        focusPanel.add(focusToolBar, BorderLayout.NORTH);

    	final JPanel monitGraphPanel = new JPanel(new BorderLayout());
    	//monitGraphPanel.add(new JScrollPane(monitGraphView), BorderLayout.CENTER);
    	//monitGraphPanel.add(monitGraphToolBar, BorderLayout.NORTH);

        this.tabbedPane = new JTabbedPane();
        this.tabbedPane.addTab("Focus panel", focusPanel);
        this.tabbedPane.addTab("Monitoring/Forecast", monitGraphPanel);

        this.add(tabbedPane, BorderLayout.CENTER);
    }

    public void updateView()
    {
        this.focusView.updateView();
        //this.monitGraphView.updateView();
    }

    public void restoreView()
    {
        this.focusView.reset();
        //this.monitGraphView.resetView();

        this.updateView();

        this.focusToolBar.setState(false);
        this.monitGraphToolBar.setState(false);
    }
}
