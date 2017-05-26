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


package com.net2plan.gui;


import com.net2plan.gui.utils.ParameterValueDescriptionPanel;
import com.net2plan.gui.utils.SolverCheckPanel;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.plugins.Plugin;
import com.net2plan.internal.plugins.PluginSystem;
import com.net2plan.utils.SwingUtils;
import com.net2plan.utils.Triple;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * This class is a graphical inteface to edit Net2Plan-wide options.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public class GUIConfiguration extends JDialog implements ActionListener
{
    private final JButton btn_cancel, btn_save;
    private final JTabbedPane tabbedPane;

    /**
     * Default constructor.
     *
     * @since 0.2.3
     */
    public GUIConfiguration()
    {
        super();

        setTitle("Options");
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        JPanel buttonBar = new JPanel();

        btn_save = new JButton("Save");
        btn_save.setToolTipText("Save the current options in the .ini file");
        btn_save.addActionListener(this);

        btn_cancel = new JButton("Close");
        btn_cancel.setToolTipText("Close the dialog");
        btn_cancel.addActionListener(this);

        buttonBar.add(btn_save);
        buttonBar.add(btn_cancel);

        add(tabbedPane, BorderLayout.CENTER);
        add(buttonBar, BorderLayout.SOUTH);

        JPanel pane_generalOptions = new JPanel(new BorderLayout());
        tabbedPane.addTab("General options", pane_generalOptions);

        SolverCheckPanel checkSolversPanel = new SolverCheckPanel();
        tabbedPane.addTab("Check solvers", checkSolversPanel);

        ParameterValueDescriptionPanel generalParameterPanel = new ParameterValueDescriptionPanel();
        pane_generalOptions.add(generalParameterPanel, BorderLayout.CENTER);

        List<Triple<String, String, String>> net2planParameters = new LinkedList<Triple<String, String, String>>(Configuration.getNet2PlanParameters());
        Iterator<Triple<String, String, String>> it = net2planParameters.iterator();
        while (it.hasNext())
        {
            if (it.next().getFirst().equals("classpath"))
            {
                it.remove();
                break;
            }
        }

        tabbedPane.addChangeListener(changeEvent ->
        {
            final JTabbedPane tabPane = (JTabbedPane) changeEvent.getSource();

            if (tabPane.getSelectedComponent() == pane_generalOptions)
            {
                generalParameterPanel.setParameters(net2planParameters);
                generalParameterPanel.setParameterValues(Configuration.getNet2PlanOptions());
            }
        });

        generalParameterPanel.setParameters(net2planParameters);
        generalParameterPanel.setParameterValues(Configuration.getNet2PlanOptions());

        Set<Class<? extends Plugin>> pluginTypes = PluginSystem.getPluginTypes();
        for (Class<? extends Plugin> pluginType : pluginTypes)
        {
            for (Class<? extends Plugin> plugin : PluginSystem.getPlugins(pluginType))
            {
                try
                {
                    Plugin instance = plugin.newInstance();
                    String description;
                    String name;

                    name = instance.getName();

                    if (name == null || name.isEmpty()) continue;

                    description = instance.getDescription();

                    JPanel subTab = new JPanel(new BorderLayout());
                    if (description != null && !description.isEmpty())
                        subTab.add(new JLabel(description), BorderLayout.NORTH);

                    List<Triple<String, String, String>> parameters;
                    parameters = instance.getParameters();

                    System.out.println("Plugin name :" + name + ", descrption: " + description + ", parameters: " + parameters);

                    if (parameters.isEmpty()) continue;

                    ParameterValueDescriptionPanel parameterPanel = new ParameterValueDescriptionPanel();
                    parameterPanel.setParameters(parameters);
                    parameterPanel.setParameterValues(instance.getCurrentOptions());
                    subTab.add(parameterPanel, BorderLayout.CENTER);
                    tabbedPane.addTab(name, subTab);
                } catch (Throwable ignored)
                {
                }
            }
        }

        SwingUtils.configureCloseDialogOnEscape(this);
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == btn_save)
        {
            Map<String, String> newOptions = new LinkedHashMap<String, String>();
            int numTabs = tabbedPane.getTabCount();
            for (int tabId = 0; tabId < numTabs; tabId++)
            {
                try
                {
                    ParameterValueDescriptionPanel pane = (ParameterValueDescriptionPanel) ((BorderLayout) ((JPanel) tabbedPane.getComponentAt(tabId)).getLayout()).getLayoutComponent(BorderLayout.CENTER);
                    newOptions.putAll(pane.getParameters());
                } catch (ClassCastException ignored)
                {
                }
            }

            try
            {
                Configuration.setOptions(newOptions);
            } catch (Throwable e1)
            {
                ErrorHandling.addErrorOrException(e1, GUIConfiguration.class);
            }

            try
            {
                Configuration.saveOptions();
            } catch (Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Error saving options");
                return;
            }
        } else if (e.getSource() == btn_cancel)
        {
            dispose();
        }
    }
}
