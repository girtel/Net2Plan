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
package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_layer;
import com.net2plan.interfaces.networkDesign.Net2PlanException;


public class TableViewController
{
    private final AdvancedJTable_abstractElement table;
    private final GUINetworkDesign callback;

    private final JPopupMenu fixedTableMenu, mainTableMenu;
    private final JMenu showMenu, doubleFormatMenu;
    private final JMenuItem showAllItem, hideMenu;
    private final List<String> allColumnsHeaders;
    private final List<String> doubleColumnHeaders;
    private final JTable mainTable, fixedTable;
    private final Map<String, JMenuItem> hiddenHeaderItems;
    private final Map<Integer, JMenuItem> doubleFormatItems;
    private boolean expandAttributes = false;

    public TableViewController(GUINetworkDesign callback, AdvancedJTable_abstractElement table)
    {
        this.table = table;
        this.callback = callback;
        this.mainTable = table.getMainTable();
        this.fixedTable = table.getFixedTable();
        this.allColumnsHeaders = ((List<AjtColumnInfo>) table.getColumnsInfo(false)).stream().map(c->c.getHeader()).collect(Collectors.toList());
        this.doubleColumnHeaders = ((List<AjtColumnInfo>) table.getColumnsInfo(false)).stream().filter(c->c.getValueShownIfNotAggregation().equals(Double.class)).map(c->c.getHeader()).collect(Collectors.toList());
        this.hiddenHeaderItems = new HashMap<>();
        this.doubleFormatItems = new HashMap<>();

        this.fixedTableMenu = new JPopupMenu();
        this.mainTableMenu = new JPopupMenu();
        this.showMenu = new JMenu("Show column");
        this.showAllItem = new JMenuItem("Unhide all columns");
        this.hideMenu = new JMenuItem("Hide column");
        this.doubleFormatMenu = new JMenu("Double format");
        for (int i = 0; i <= 5; i++) 
    	{
        	doubleFormatItems.put(i, new JMenuItem(i + ""));
            doubleFormatMenu.add(doubleFormatItems.get(i));
        }

        if (!(table instanceof AdvancedJTable_layer))
        {
            mainTable.getTableHeader().addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseReleased(MouseEvent ev)
                {
                    if (SwingUtilities.isRightMouseButton(ev))
                    {
                        // Building menu
                        mainTableMenu.removeAll();
                        mainTableMenu.revalidate();
                        mainTableMenu.repaint();
                        mainTableMenu.add(hideMenu);

                        updateShowMenu();
                        TableColumn clickedColumn = mainTable.getColumnModel().getColumn(mainTable.columnAtPoint(ev.getPoint()));
                        String clickedColumnName = clickedColumn.getHeaderValue().toString();
                        
                        if (doubleColumnHeaders.contains(clickedColumnName))
                        {
                            mainTableMenu.add(doubleFormatMenu);
                            for (int i = 0; i <= 5; i++) 
                        	{
                            	final JMenuItem currentItem = doubleFormatItems.get(i);
                                currentItem.addActionListener(new ActionListener()
                                {
                                    @Override
                                    public void actionPerformed(ActionEvent e)
                                    {
                                    	final JMenuItem currentItem = (JMenuItem) e.getSource();

                                    	table.setDoubleFormat(clickedColumnName, Integer.parseInt(currentItem.getText()));
                                    	table.updateView();
                                    }
                                });
                            }
                        }

                        
                        mainTableMenu.show(ev.getComponent(), ev.getX(), ev.getY());


                        hideMenu.addActionListener(new ActionListener()
                        {
                            @Override
                            public void actionPerformed(ActionEvent e)
                            {
                            	table.setIsColumnHiddenByUser(clickedColumnName, true);
                            	hideMenu.setSelected(false);
                            	table.updateView();
                            }

                        });
                    }
                }
            });

            fixedTable.getTableHeader().addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseReleased(MouseEvent e)
                {
                    if (SwingUtilities.isRightMouseButton(e))
                    {
                        // Build menu
                        fixedTableMenu.removeAll();
                        fixedTableMenu.revalidate();
                        fixedTableMenu.repaint();
                        fixedTableMenu.add(showMenu);
                        fixedTableMenu.add(showAllItem);
                        fixedTableMenu.show(e.getComponent(), e.getX(), e.getY());

                        updateShowMenu();

                        showMenu.setEnabled(true);

                        final List<String> hiddenColumns = allColumnsHeaders.stream().filter(c->table.isColumnHiddenByUser(c)).collect(Collectors.toList());
                        if (hiddenColumns.size() == 0)
                        {
                            showMenu.setEnabled(false);
                        }


                        for (String ajColumnName : hiddenColumns)
                        {
                            JMenuItem currentItem = hiddenHeaderItems.get(ajColumnName);
                            currentItem.addActionListener(new ActionListener()
                            {

                                @Override
                                public void actionPerformed(ActionEvent e)
                                {
                                	table.setIsColumnHiddenByUser(ajColumnName, false);
                                	table.updateView();
                                }
                            });
                        }
                    }
                }

            });

            this.buildAttributeControls();
        }
    }

    private void buildAttributeControls()
    {
        showAllItem.addActionListener(e ->
        {
        	allColumnsHeaders.forEach(c->table.setIsColumnHiddenByUser(c, false));
            table.updateView();
        });
    }

    /**
     * Re-configures the menu to show hidden columns
     */
    private void updateShowMenu()
    {
        showMenu.removeAll();
        hiddenHeaderItems.clear();
        final List<String> hiddenColumns = allColumnsHeaders.stream().filter(c->table.isColumnHiddenByUser(c)).collect(Collectors.toList());

        for (String ajColumn : hiddenColumns) 
    	{
            hiddenHeaderItems.put(ajColumn, new JMenuItem(ajColumn));
            showMenu.add(hiddenHeaderItems.get(ajColumn));
        }
    }
}
