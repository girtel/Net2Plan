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
package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables;


import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter.FilterCombinationType;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.*;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.rightPanelTabs.NetPlanViewTableComponent_layer;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.rightPanelTabs.NetPlanViewTableComponent_network;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.rightPanelTabs.NetPlanViewTableComponent_trafficMatrix;
import com.net2plan.gui.utils.FullScrollPaneLayout;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.SwingUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class ViewEditTopologyTablesPane extends JPanel
{
    private final GUINetworkDesign callback;
    private final JTabbedPane netPlanView;
    private final JTabbedPane demandTabbedPaneListAndMatrix;
    private NetPlanViewTableComponent_trafficMatrix trafficMatrixComponent;
    private final Map<Constants.NetworkElementType, AdvancedJTable_networkElement> netPlanViewTable;
    private final Map<Constants.NetworkElementType, JComponent> netPlanViewTableComponent;
    private final Map<Constants.NetworkElementType, JLabel> netPlanViewTableNumEntriesLabel;

    private final JMenuBar menuBar;
    private final JMenu exportMenu;

    public ViewEditTopologyTablesPane(GUINetworkDesign callback, LayoutManager layout)
    {
        super(layout);

        this.callback = callback;

        netPlanViewTable = new EnumMap<>(Constants.NetworkElementType.class);
        netPlanViewTableComponent = new EnumMap<>(Constants.NetworkElementType.class);
        netPlanViewTableNumEntriesLabel = new EnumMap<>(Constants.NetworkElementType.class);

        netPlanViewTable.put(Constants.NetworkElementType.NODE, new AdvancedJTable_node(callback));
        netPlanViewTable.put(Constants.NetworkElementType.LINK, new AdvancedJTable_link(callback));
        netPlanViewTable.put(Constants.NetworkElementType.DEMAND, new AdvancedJTable_demand(callback));
        netPlanViewTable.put(Constants.NetworkElementType.ROUTE, new AdvancedJTable_route(callback));
        netPlanViewTable.put(Constants.NetworkElementType.FORWARDING_RULE, new AdvancedJTable_forwardingRule(callback));
        netPlanViewTable.put(Constants.NetworkElementType.MULTICAST_DEMAND, new AdvancedJTable_multicastDemand(callback));
        netPlanViewTable.put(Constants.NetworkElementType.MULTICAST_TREE, new AdvancedJTable_multicastTree(callback));
        netPlanViewTable.put(Constants.NetworkElementType.SRG, new AdvancedJTable_srg(callback));
        netPlanViewTable.put(Constants.NetworkElementType.RESOURCE, new AdvancedJTable_resource(callback));
        netPlanViewTable.put(Constants.NetworkElementType.LAYER, new AdvancedJTable_layer(callback));

        netPlanViewTableNumEntriesLabel.put(NetworkElementType.NODE, new JLabel("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.LINK, new JLabel("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.DEMAND, new JLabel("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.ROUTE, new JLabel("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.FORWARDING_RULE, new JLabel("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.MULTICAST_DEMAND, new JLabel("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.MULTICAST_TREE, new JLabel("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.SRG, new JLabel("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.RESOURCE, new JLabel("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.LAYER, new JLabel("Number of entries: "));

        netPlanView = new JTabbedPane();
        demandTabbedPaneListAndMatrix = new JTabbedPane();
        this.trafficMatrixComponent = new NetPlanViewTableComponent_trafficMatrix(callback);
        for (NetworkElementType elementType : NetworkElementType.values())
        {
            if (elementType == NetworkElementType.NETWORK)
            {
                netPlanViewTableComponent.put(elementType, new NetPlanViewTableComponent_network(callback, (AdvancedJTable_layer) netPlanViewTable.get(NetworkElementType.LAYER)));
            } else if (elementType == NetworkElementType.LAYER)
            {
                netPlanViewTableComponent.put(elementType, new NetPlanViewTableComponent_layer(callback, (AdvancedJTable_layer) netPlanViewTable.get(NetworkElementType.LAYER)));
            } else
            {
                JScrollPane scrollPane = netPlanViewTable.get(elementType).getScrollPane();
                scrollPane.setLayout(new FullScrollPaneLayout());
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                final JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                JPanel labelsPanel = new JPanel();
                labelsPanel.setLayout(new BorderLayout());
                labelsPanel.setBackground(Color.YELLOW);
                labelsPanel.setForeground(Color.BLACK);
                labelsPanel.add(netPlanViewTableNumEntriesLabel.get(elementType), BorderLayout.CENTER);
                {
                    final JPanel buttonsPanel = new JPanel();
                    final JButton resetTableRowFilters = new JButton("Reset VFs");
                    buttonsPanel.add(resetTableRowFilters, BorderLayout.EAST);
                    resetTableRowFilters.addActionListener(e ->
                    {
                        callback.getVisualizationState().updateTableRowFilter(null, FilterCombinationType.INCLUDEIF_AND);
                        callback.updateVisualizationJustTables();
                        callback.resetPickedStateAndUpdateView();
                    });
                    buttonsPanel.setOpaque(false);
                    labelsPanel.add(buttonsPanel, BorderLayout.EAST);
                }

                labelsPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
                panel.add(labelsPanel, BorderLayout.NORTH);
                panel.add(scrollPane, BorderLayout.CENTER);
                netPlanViewTableComponent.put(elementType, panel);
                if (elementType == NetworkElementType.DEMAND)
                {
                    this.demandTabbedPaneListAndMatrix.addTab("List view", panel);
                    this.demandTabbedPaneListAndMatrix.addTab("Traffic matrix view", trafficMatrixComponent);
                }
            }
        }

        this.add(netPlanView, BorderLayout.CENTER);

        final JMenuItem writeToExcel = new JMenuItem("To excel");
        writeToExcel.addActionListener((ActionEvent ev) ->
        {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);

            FileFilter xlsFilter = new FileNameExtensionFilter("Excel 2003 file (*.xls)", "xls");
            FileFilter xlsxFilter = new FileNameExtensionFilter("Excel 2007 file (*.xlsx)", "xlsx");
            fileChooser.addChoosableFileFilter(xlsFilter);
            fileChooser.addChoosableFileFilter(xlsxFilter);

            final int res = fileChooser.showSaveDialog(null);

            if (res == JFileChooser.APPROVE_OPTION)
            {
                final File file = SwingUtils.getSelectedFileWithExtension(fileChooser);

                if (file.exists())
                {
                    int option = JOptionPane.showConfirmDialog(null, "File already exists.\nOverwrite?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);

                    if (option == JOptionPane.YES_OPTION)
                        file.delete();
                    else
                        return;
                }

                try
                {
                    final NetPlan netPlan = callback.getDesign();

                    for (AdvancedJTable_networkElement table : netPlanViewTable.values())
                    {
                        if (table instanceof AdvancedJTable_forwardingRule)
                            if (netPlan.getRoutingType() != RoutingType.HOP_BY_HOP_ROUTING)
                                continue;

                        if (table instanceof AdvancedJTable_route)
                            if (netPlan.getRoutingType() != RoutingType.SOURCE_ROUTING)
                                continue;

                        table.writeTableToFile(file);

                        if (table instanceof AdvancedJTable_demand)
                            trafficMatrixComponent.writeTableToFile(file);
                    }

                    ErrorHandling.showInformationDialog("Excel file successfully written", "Finished writing into file");
                } catch (Exception e)
                {
                    ErrorHandling.showErrorDialog("Error");
                    e.printStackTrace();
                }
            }
        });

        menuBar = new JMenuBar();

        exportMenu = new JMenu("Export tables...");
        exportMenu.add(writeToExcel);

        menuBar.add(exportMenu);

        this.add(menuBar, BorderLayout.SOUTH);
    }

    public Map<Constants.NetworkElementType, AdvancedJTable_networkElement> getNetPlanViewTable()
    {
        return netPlanViewTable;
    }

    public void updateView()
    {
        /* Load current network state */
        final NetPlan currentState = callback.getDesign();
        final NetworkLayer layer = currentState.getNetworkLayerDefault();
        if (ErrorHandling.isDebugEnabled()) currentState.checkCachesConsistency();

        final int selectedTabIndex = netPlanView.getSelectedIndex();
        netPlanView.removeAll();
        final int selectedListOrMatrixDemands = demandTabbedPaneListAndMatrix.getSelectedIndex();
        demandTabbedPaneListAndMatrix.removeAll();
        for (NetworkElementType elementType : NetworkElementType.values())
        {
            if (layer.isSourceRouting() && elementType == NetworkElementType.FORWARDING_RULE)
                continue;
            if (!layer.isSourceRouting() && (elementType == NetworkElementType.ROUTE))
                continue;
            if (elementType == NetworkElementType.DEMAND)
            {
                this.demandTabbedPaneListAndMatrix.addTab("List view", netPlanViewTableComponent.get(elementType));
                this.demandTabbedPaneListAndMatrix.addTab("Traffic matrix view", trafficMatrixComponent);
                netPlanView.addTab(netPlanViewTable.get(elementType).getTabName(), this.demandTabbedPaneListAndMatrix);
                continue;
            }
            netPlanView.addTab(elementType == NetworkElementType.NETWORK ? "Network" : netPlanViewTable.get(elementType).getTabName(), netPlanViewTableComponent.get(elementType));
        }
        demandTabbedPaneListAndMatrix.setSelectedIndex(selectedListOrMatrixDemands);
        if ((selectedTabIndex < netPlanView.getTabCount()) && (selectedTabIndex >= 0))
            netPlanView.setSelectedIndex(selectedTabIndex);

        if (ErrorHandling.isDebugEnabled()) currentState.checkCachesConsistency();

        /* update the required tables */
        for (Map.Entry<Constants.NetworkElementType, AdvancedJTable_networkElement> entry : netPlanViewTable.entrySet())
        {
            if (layer.isSourceRouting() && entry.getKey() == NetworkElementType.FORWARDING_RULE) continue;
            if (!layer.isSourceRouting() && (entry.getKey() == NetworkElementType.ROUTE)) continue;
            final AdvancedJTable_networkElement table = entry.getValue();
            table.updateView(currentState);
            final JLabel label = netPlanViewTableNumEntriesLabel.get(entry.getKey());
            if (label != null)
            {
                final int numEntries = table.getModel().getRowCount() - 1; // last columns is for the aggregation
                if (callback.getVisualizationState().getTableRowFilter() != null)
                    label.setText("Number of entries: " + numEntries + " / " + table.getModel().getRowCount() + ", FILTERED VIEW: " + callback.getVisualizationState().getTableRowFilter().getDescription());
                else
                    label.setText("Number of entries: " + numEntries);
            }
        }

        trafficMatrixComponent.updateNetPlanView();
        ((NetPlanViewTableComponent_layer) netPlanViewTableComponent.get(NetworkElementType.LAYER)).updateNetPlanView(currentState);
        ((NetPlanViewTableComponent_network) netPlanViewTableComponent.get(NetworkElementType.NETWORK)).updateNetPlanView(currentState);
    }


    /**
     * Shows the tab corresponding associated to a network element.
     *
     * @param type   Network element type
     * @param itemId Item identifier (if null, it will just show the tab)
     */
    public void selectItemTab(NetworkElementType type, Object itemId)
    {
        AdvancedJTable_networkElement table = netPlanViewTable.get(type);
        int tabIndex = netPlanView.getSelectedIndex();
        int col = 0;
        if (netPlanView.getTitleAt(tabIndex).equals(type == NetworkElementType.NETWORK ? "Network" : table.getTabName()))
        {
            col = table.getSelectedColumn();
            if (col == -1) col = 0;
        } else
        {
            netPlanView.setSelectedComponent(netPlanViewTableComponent.get(type));
        }

        if (itemId == null)
        {
            table.clearSelection();
            return;
        }

        TableModel model = table.getModel();
        final int numRows = model.getRowCount();
        for (int row = 0; row < numRows; row++)
        {
            Object obj = model.getValueAt(row, 0);
            if (obj == null) continue;

            if (type == NetworkElementType.FORWARDING_RULE)
            {
                obj = Pair.of(
                        Integer.parseInt(model.getValueAt(row, AdvancedJTable_forwardingRule.COLUMN_DEMAND).toString().split(" ")[0]),
                        Integer.parseInt(model.getValueAt(row, AdvancedJTable_forwardingRule.COLUMN_OUTGOINGLINK).toString().split(" ")[0]));
                if (!obj.equals(itemId)) continue;
            } else if ((long) obj != (long) itemId)
            {
                continue;
            }

            row = table.convertRowIndexToView(row);
            table.changeSelection(row, col, false, true);
            return;
        }

        throw new RuntimeException(type + " " + itemId + " does not exist");
    }

    public void selectItem(NetworkElementType type, Pair<Demand, Link> fr)
    {
        final AdvancedJTable_networkElement table = netPlanViewTable.get(type);
        final TableModel model = table.getModel();
        final int numRows = model.getRowCount();
        for (int row = 0; row < numRows; row++)
        {
            final Object demandInTable = model.getValueAt(row, AdvancedJTable_forwardingRule.COLUMN_DEMAND);
            final Object linkInTable = model.getValueAt(row, AdvancedJTable_forwardingRule.COLUMN_OUTGOINGLINK);
            if (demandInTable == null) continue;
            if (linkInTable == null) continue;
            final Pair<Integer, Integer> obj = Pair.of(
                    Integer.parseInt(demandInTable.toString().split(" ")[0]), Integer.parseInt(linkInTable.toString().split(" ")[0]));
            if (obj.equals(fr))
            {
                table.addRowSelectionInterval(table.convertRowIndexToModel(row), table.convertRowIndexToModel(row));
                return;
            }
        }
    }

    public void selectItems(NetworkElementType type, List<NetworkElement> elements)
    {
        final AdvancedJTable_networkElement table = netPlanViewTable.get(type);
        final Set<Long> idstoSelect = elements.stream().map(e -> e.getId()).collect(Collectors.toSet());

        final int numRows = table.getRowCount();
        int numSelected = 0;
        for (int row = 0; row < numRows; row++)
        {
            try
            {
                final long elementID = (long) table.getModel().getValueAt(row, 0);
                if (idstoSelect.contains(elementID))
                {
                    final int viewRow = table.convertRowIndexToView(row);
                    table.addRowSelectionInterval(viewRow, viewRow);
                    numSelected++;
                    if (numSelected == elements.size()) return;
                }
            } catch (ClassCastException e)
            {
                ErrorHandling.log("Tried to use aggregation row at: " + row);
            }
        }
    }

}
