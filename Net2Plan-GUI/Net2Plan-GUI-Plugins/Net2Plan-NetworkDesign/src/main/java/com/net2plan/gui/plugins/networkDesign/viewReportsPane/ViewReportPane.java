/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/
package com.net2plan.gui.plugins.networkDesign.viewReportsPane;


import com.net2plan.gui.utils.ParameterValueDescriptionPanel;
import com.net2plan.gui.plugins.networkDesign.ReportBrowser;
import com.net2plan.gui.utils.RunnableSelector;
import com.net2plan.gui.plugins.networkDesign.ThreadExecutionController;
import com.net2plan.gui.utils.*;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IReport;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.SystemUtils;
import com.net2plan.internal.plugins.IGUIModule;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.Closeable;
import java.io.File;
import java.util.Map;

@SuppressWarnings("unchecked")
public class ViewReportPane extends JSplitPane implements ThreadExecutionController.IThreadExecutionHandler
{
	private final GUINetworkDesign mainWindow;
    private RunnableSelector reportSelector;
    private ThreadExecutionController reportController;
    private JTabbedPane reportContainer;
    private JButton closeAllReports;

	public ViewReportPane (GUINetworkDesign mainWindow , int newOrientation)
	{
		super (newOrientation);

		this.mainWindow = mainWindow;
	
        reportController = new ThreadExecutionController(this);

        File REPORTS_DIRECTORY = new File(IGUIModule.CURRENT_DIR + SystemUtils.getDirectorySeparator() + "workspace");
        REPORTS_DIRECTORY = REPORTS_DIRECTORY.isDirectory() ? REPORTS_DIRECTORY : IGUIModule.CURRENT_DIR;
        ParameterValueDescriptionPanel reportParameters = new ParameterValueDescriptionPanel();
        reportSelector = new RunnableSelector("Report", null, IReport.class, REPORTS_DIRECTORY, reportParameters);
        reportContainer = new JTabbedPane();

        final JPanel pnl_buttons = new JPanel(new WrapLayout());

        reportContainer.setVisible(false);

        reportContainer.addContainerListener(new ContainerListener() {
            @Override
            public void componentAdded(ContainerEvent e) {
                reportContainer.setVisible(true);
                setDividerLocation(0.5);

                for (Component component : pnl_buttons.getComponents())
                    if (component == closeAllReports)
                        return;

                pnl_buttons.add(closeAllReports);
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                if (reportContainer.getTabCount() == 0) {
                    reportContainer.setVisible(false);

                    for (Component component : pnl_buttons.getComponents())
                        if (component == closeAllReports)
                            pnl_buttons.remove(closeAllReports);
                }
            }
        });

        JButton btn_show = new JButton("Show");
        btn_show.setToolTipText("Show the report");
        btn_show.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reportController.execute();
            }
        });

        closeAllReports = new JButton("Close all");
        closeAllReports.setToolTipText("Close all reports");
        closeAllReports.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reportContainer.removeAll();
            }
        });

        reportContainer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int tabNumber = reportContainer.getUI().tabForCoordinate(reportContainer, e.getX(), e.getY());

                if (tabNumber >= 0) {
                    Rectangle rect = ((TabIcon) reportContainer.getIconAt(tabNumber)).getBounds();
                    if (rect.contains(e.getX(), e.getY())) reportContainer.removeTabAt(tabNumber);
                }
            }
        });

        pnl_buttons.add(btn_show);

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(reportSelector, BorderLayout.CENTER);
        pane.add(pnl_buttons, BorderLayout.SOUTH);
        setTopComponent(pane);

        setBottomComponent(reportContainer);
        addPropertyChangeListener(new ProportionalResizeJSplitPaneListener());
        setResizeWeight(0.5);
	}
	
	@Override
	public Object execute(ThreadExecutionController controller) 
	{
        Triple<File, String, Class> report = reportSelector.getRunnable();
        Map<String, String> reportParameters = reportSelector.getRunnableParameters();
        Map<String, String> net2planParameters = Configuration.getNet2PlanOptions();
        IReport instance = ClassLoaderUtils.getInstance(report.getFirst(), report.getSecond(), IReport.class , null);
        String title = null;
        try {
            title = instance.getTitle();
        } catch (UnsupportedOperationException ex) {
        }
        if (title == null) title = "Untitled";

        Pair<String, ? extends JPanel> aux = Pair.of(title, new ReportBrowser(instance.executeReport(mainWindow.getDesign().copy(), reportParameters, net2planParameters)));
        try {
            ((Closeable) instance.getClass().getClassLoader()).close();
        } catch (Throwable e) {
        }

        return aux;
	}
	@Override
	public void executionFinished(ThreadExecutionController controller, Object out) 
	{
        Pair<String, ? extends JPanel> aux = (Pair<String, ? extends JPanel>) out;
        reportContainer.addTab(aux.getFirst(), new TabIcon(TabIcon.IconType.TIMES_SIGN), aux.getSecond());
        reportContainer.setSelectedIndex(reportContainer.getTabCount() - 1);
	}
	@Override
	public void executionFailed(ThreadExecutionController controller) 
	{
        ErrorHandling.showErrorDialog("Error executing report");
	}

	public JTabbedPane getReportContainer () { return reportContainer; }
}
