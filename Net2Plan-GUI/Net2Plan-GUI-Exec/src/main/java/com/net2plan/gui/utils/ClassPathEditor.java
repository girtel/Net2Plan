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


package com.net2plan.gui.utils;

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.SystemUtils;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.SwingUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

/**
 * <p>Class implementing the class-path editor.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ClassPathEditor {
    private final static JFileChooser FILE_CHOOSER;
    private final static JDialog CLASSPATH_EDITOR;
    private final static DefaultTableModel MODEL;
    private final static AdvancedJTable TABLE;
    private final static JButton ADD_ITEM, REMOVE_SELECTED, REMOVE_ALL;

    static {
        FILE_CHOOSER = new JFileChooser();
        FILE_CHOOSER.setCurrentDirectory(SystemUtils.getCurrentDir());
        FILE_CHOOSER.setDialogTitle("Select a JAR file");
        FILE_CHOOSER.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FILE_CHOOSER.setAcceptAllFileFilterUsed(false);
        FILE_CHOOSER.setMultiSelectionEnabled(true);
        FILE_CHOOSER.setFileFilter(null);
        FileFilter filter = new FileNameExtensionFilter("JAR file", "jar");
        FILE_CHOOSER.addChoosableFileFilter(filter);

        CLASSPATH_EDITOR = new JDialog();
        CLASSPATH_EDITOR.setTitle("Classpath editor");
        SwingUtils.configureCloseDialogOnEscape(CLASSPATH_EDITOR);
        CLASSPATH_EDITOR.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        CLASSPATH_EDITOR.setSize(new Dimension(500, 300));
        CLASSPATH_EDITOR.setLocationRelativeTo(null);
        CLASSPATH_EDITOR.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        CLASSPATH_EDITOR.setLayout(new MigLayout("insets 0 0 0 0", "[grow]", "[][][grow][]"));

        MODEL = new ClassAwareTableModel();
        TABLE = new AdvancedJTable(MODEL);

        JPanel pane = new JPanel();

        ADD_ITEM = new JButton("Add file");
        ADD_ITEM.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (FILE_CHOOSER.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        for (File file : FILE_CHOOSER.getSelectedFiles()) {
                            String previousClasspath = Configuration.getOption("classpath");
                            String path = file.getCanonicalPath();
                            Configuration.setOption("classpath", previousClasspath.isEmpty() ? path : previousClasspath + ";" + path);
                            SystemUtils.addToClasspath(file);
                        }

                        Configuration.saveOptions();
                        refresh();
                    }
                } catch (Throwable ex) {
                    ErrorHandling.addErrorOrException(ex);
                    ErrorHandling.showErrorDialog("Error adding classpath items");
                }
            }

            ;
        });

        REMOVE_SELECTED = new JButton("Remove selected");
        REMOVE_SELECTED.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String currentClasspath = Configuration.getOption("classpath");
                try {
                    int row = TABLE.getSelectedRow();
                    if (row == -1) return;
                    row = TABLE.convertRowIndexToModel(row);

                    StringBuilder classpath = new StringBuilder();
                    int numRows = MODEL.getRowCount();
                    for (int i = 0; i < numRows; i++) {
                        if (i == row) continue;

                        if (classpath.length() > 0) classpath.append(";");
                        String path = (String) MODEL.getValueAt(i, 0);
                        path = path.replace(SystemUtils.getDirectorySeparator(), "\\");
                        classpath.append(path);
                    }

                    Configuration.setOption("classpath", classpath.toString());
                    Configuration.saveOptions();

                    MODEL.removeRow(row);
                    if (MODEL.getRowCount() == 0) resetTable();
                } catch (Throwable ex) {
                    Configuration.setOption("classpath", currentClasspath);
                    ErrorHandling.addErrorOrException(ex);
                    ErrorHandling.showErrorDialog("Error removing classpath items");
                }
            }
        });

        REMOVE_ALL = new JButton("Remove all");
        REMOVE_ALL.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Configuration.setOption("classpath", "");
                    Configuration.saveOptions();
                    while (MODEL.getRowCount() > 0)
                        MODEL.removeRow(0);

                    resetTable();
                } catch (Throwable ex) {
                    ErrorHandling.addErrorOrException(ex);
                    ErrorHandling.showErrorDialog("Error removing classpath items");
                }
            }
        });

        pane.add(ADD_ITEM);
        pane.add(REMOVE_SELECTED);
        pane.add(REMOVE_ALL);

        CLASSPATH_EDITOR.add(new JLabel("<html>Algorithms and reports made by users may require external Java libraries<br />not included within Net2Plan. Use this option to include them</html>", JLabel.CENTER), "grow, wrap");
        CLASSPATH_EDITOR.add(pane, "grow, wrap");
        CLASSPATH_EDITOR.add(new JScrollPane(TABLE), "grow, wrap");
        CLASSPATH_EDITOR.add(new JLabel("<html>In the current version it is recommended to restart Net2Plan after removing libraries<br /> since they are not actually unloaded from memory</html", JLabel.CENTER), "grow");

        resetTable();
    }

    private static void refresh() {
        resetTable();

        Set<URL> userClasspath = SystemUtils.getUserClasspath();
        if (!userClasspath.isEmpty()) {
            URI currentPath = SystemUtils.getCurrentDir().toURI();
            MODEL.removeRow(0);
            for (URL url : userClasspath) {
                String path;
                try {
                    path = url.toURI().toString();
                } catch (URISyntaxException ex) {
                    throw new RuntimeException(ex);
                }

                path = path.replace(currentPath.toString(), "");
                path = path.replace("/", SystemUtils.getDirectorySeparator());

                MODEL.addRow(new Object[]{path});
            }

            TABLE.setEnabled(true);
            REMOVE_SELECTED.setEnabled(true);
            REMOVE_ALL.setEnabled(true);
        }
    }

    private static void resetTable() {
        MODEL.setDataVector(new Object[1][], StringUtils.arrayOf("Path"));
        TABLE.setEnabled(false);

        REMOVE_SELECTED.setEnabled(false);
        REMOVE_ALL.setEnabled(false);
    }

    /**
     * Shows the classpath GUI.
     *
     * @since 0.2.0
     */
    public static void showGUI() {
        if (!CLASSPATH_EDITOR.isVisible()) {
            refresh();
            CLASSPATH_EDITOR.setVisible(true);
        }
    }
}
