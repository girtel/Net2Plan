/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/


package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.AttributeEditor;
import com.net2plan.gui.plugins.networkDesign.ElementSelection;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.gui.plugins.networkDesign.io.excel.ExcelWriter;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_forwardingRule;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_layer;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableStateFiles.TableState;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters.TBFSelectionBased;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters.TBFTagBased;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters.TBFToFromCarriedTraffic;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ColumnHeaderToolTips;
import com.net2plan.gui.utils.FixedColumnDecorator;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;


import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.xml.stream.XMLStreamException;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

import static com.net2plan.gui.plugins.networkDesign.ElementSelection.SelectionType;
import static com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter.FilterCombinationType;


/**
 * <p>Extended version of the {@code JTable} class. It presents the following
 * additional features:</p>
 * <ul>
 * <li>Reordering of table columns is not allowed</li>
 * <li>Auto-resize of columns is disabled</li>
 * <li>It allows to set per-cell editors</li>
 * <li>It allows to navigate the table with the cursor</li>
 * <li>It allows to configure if all cell contents are selected when editing or typing ('true' by default, using 'setSelectAllXXX()' methods to customize)</li>
 * </ul>
 * <p>Credits to Santhosh Kumar for his methods to solve partially visible cell
 * issues (<a href='http://www.jroller.com/santhosh/entry/partially_visible_tablecells'>Partially Visible TableCells</a>)</p>
 * <p>Credits to "Kah - The Developer" for his static method to set column widths
 * in proportion to each other (<a href='http://kahdev.wordpress.com/2011/10/30/java-specifying-the-column-widths-of-a-jtable-as-percentages/'>Specifying the column widths of a JTable as percentages</a>)
 * </p>
 * <p>Credits to Rob Camick for his 'select all' editing feature for {@code JTable}
 * (<a href='https://tips4java.wordpress.com/2008/10/20/table-select-all-editor/'>Table Select All Editor</a>)
 * </p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
@SuppressWarnings("unchecked")
public abstract class AdvancedJTable_networkElement extends AdvancedJTable
{
    protected final TableModel model;
    protected final GUINetworkDesign callback;
    protected final NetworkElementType networkElementType;

    protected final JTable mainTable;
    protected final JTable fixedTable;

    private final JPopupMenu fixedTableMenu, mainTableMenu;
    private final JMenu showMenu;
    private final JMenuItem showAllItem, hideAllItem, resetItem, saveStateItem, loadStateItem;
    private final ArrayList<TableColumn> hiddenColumns, shownColumns, removedColumns;
    private ArrayList<String> hiddenColumnsNames, hiddenColumnsAux;
    private final Map<String, Integer> indexForEachColumn, indexForEachHiddenColumn;
    private final Map<Integer, String> mapToSaveState;
    private JCheckBoxMenuItem lockColumn, unfixCheckBox, attributesItem, hideColumn;
    private ArrayList<JMenuItem> hiddenHeaderItems;

    private final JScrollPane scroll;
    private final FixedColumnDecorator decorator;

    private boolean expandAttributes = false;

    /**
     * Constructor that allows to set the table model.
     *
     * @param model              Table model
     * @param networkViewer      Network callback
     * @param networkElementType Network element type
     * @since 0.2.0
     */
    public AdvancedJTable_networkElement(TableModel model, final GUINetworkDesign networkViewer, NetworkElementType networkElementType)
    {
        super(model);
        this.model = model;
        this.callback = networkViewer;
        this.networkElementType = networkElementType;

		/* configure the tips */
        String[] columnTips = getTableTips();
        String[] columnHeader = getTableHeaders();
        ColumnHeaderToolTips tips = new ColumnHeaderToolTips();
        for (int c = 0; c < columnHeader.length; c++)
        {
            TableColumn col = getColumnModel().getColumn(c);
            tips.setToolTip(col, columnTips[c]);
        }
        getTableHeader().addMouseMotionListener(tips);

		/* add the popup menu listener (this) */
        addMouseListener(new PopupMenuMouseAdapter());

        addKeyboardActions();

        this.getTableHeader().setReorderingAllowed(true);

        scroll = new JScrollPane(this);
        this.decorator = new FixedColumnDecorator(scroll, getNumberOfDecoratorColumns());
        mainTable = decorator.getMainTable();
        fixedTable = decorator.getFixedTable();

        hiddenColumnsNames = new ArrayList<>();
        hiddenColumns = new ArrayList<>();
        shownColumns = new ArrayList<>();
        removedColumns = new ArrayList<>();
        indexForEachColumn = new HashMap<>();
        indexForEachHiddenColumn = new HashMap<>();
        mapToSaveState = new HashMap<>();

        for (int j = 0; j < mainTable.getColumnModel().getColumnCount(); j++)
        {
            shownColumns.add(mainTable.getColumnModel().getColumn(j));
        }

        fixedTableMenu = new JPopupMenu();
        mainTableMenu = new JPopupMenu();
        showMenu = new JMenu("Show column");
        lockColumn = new JCheckBoxMenuItem("Lock column", false);
        unfixCheckBox = new JCheckBoxMenuItem("Unlock column", true);
        showAllItem = new JMenuItem("Unhide all columns");
        hideColumn = new JCheckBoxMenuItem("Hide column", false);
        hideAllItem = new JMenuItem("Hide all columns");
        hideAllItem.setToolTipText("All columns will be hidden except for the first one.");
        attributesItem = new JCheckBoxMenuItem("Attributes in different columns", false);
        resetItem = new JMenuItem("Reset columns positions");
        loadStateItem = new JMenuItem("Load tables visualization profile");
        saveStateItem = new JMenuItem("Save tables visualization profile");

        if (hasAttributes())
        {
            this.getModel().addTableModelListener(e ->
            {
                int changedColumn = e.getColumn();
                int selectedRow = mainTable.getSelectedRow();
                Object value = null;
                if (changedColumn > getAttributesColumnIndex())
                {
                    for (String title : getAttributesColumnsHeaders())
                    {
                        if (getModel().getColumnName(changedColumn).equals("Att: " + title))
                        {
                            value = getModel().getValueAt(selectedRow, changedColumn);
                            if (value != null)
                                callback.getDesign().getNetworkElement((Long) getModel().getValueAt(selectedRow, 0)).setAttribute(title, (String) value);
                        }
                    }
                    callback.updateVisualizationJustTables();
                }
            });
        }

        if (!(this instanceof AdvancedJTable_layer))
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
                        mainTableMenu.repaint();
                        mainTableMenu.add(hideColumn);
                        mainTableMenu.add(lockColumn);

                        updateShowMenu();
                        checkNewIndexes();
                        TableColumn clickedColumn = mainTable.getColumnModel().getColumn(mainTable.columnAtPoint(ev.getPoint()));
                        String clickedColumnName = clickedColumn.getHeaderValue().toString();
                        int clickedColumnIndex = indexForEachColumn.get(clickedColumnName);
                        lockColumn.setEnabled(true);
                        hideColumn.setEnabled(true);
                        if (mainTable.getColumnModel().getColumnCount() <= 1)
                        {
                            lockColumn.setEnabled(false);
                            hideColumn.setEnabled(false);
                        }

                        // Individual column options
                        final int col = tableHeader.columnAtPoint(ev.getPoint());
                        final String columnName = mainTable.getColumnName(col);

                        switch (columnName)
                        {
                            case "Attributes":
                                mainTableMenu.add(new JPopupMenu.Separator());
                                mainTableMenu.add(attributesItem);
                                break;
                            default:
                                if (columnName.startsWith("Att:"))
                                {
                                    mainTableMenu.add(new JPopupMenu.Separator());
                                    mainTableMenu.add(attributesItem);
                                }
                                break;
                        }

                        mainTableMenu.show(ev.getComponent(), ev.getX(), ev.getY());
                        lockColumn.addItemListener(new ItemListener()
                        {

                            @Override
                            public void itemStateChanged(ItemEvent e)
                            {
                                if (lockColumn.isSelected() == true)
                                {
                                    shownColumns.remove(mainTable.getColumnModel().getColumn(clickedColumnIndex));
                                    fromMainTableToFixedTable(clickedColumnIndex);
                                    updateShowMenu();
                                    checkNewIndexes();
                                    mainTableMenu.setVisible(false);
                                    lockColumn.setSelected(false);
                                }

                            }
                        });
                        hideColumn.addItemListener(new ItemListener()
                        {
                            @Override
                            public void itemStateChanged(ItemEvent e)
                            {
                                if (hideColumn.isSelected())
                                {
                                    hideColumn(clickedColumnIndex);
                                    checkNewIndexes();
                                    hideColumn.setSelected(false);
                                }

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
                    //Checking if right button is clicked
                    if (SwingUtilities.isRightMouseButton(e))
                    {
                        // Build menu
                        fixedTableMenu.removeAll();
                        fixedTableMenu.repaint();
                        fixedTableMenu.add(unfixCheckBox);
                        fixedTableMenu.add(new JPopupMenu.Separator());
                        //fixedTableMenu.add(loadStateItem);
                        //fixedTableMenu.add(saveStateItem);
                        //fixedTableMenu.add(new JPopupMenu.Separator());
                        fixedTableMenu.add(resetItem);
                        fixedTableMenu.add(new JPopupMenu.Separator());
                        fixedTableMenu.add(showMenu);
                        fixedTableMenu.add(new JPopupMenu.Separator());
                        fixedTableMenu.add(showAllItem);
                        fixedTableMenu.add(hideAllItem);

                        checkNewIndexes();
                        updateShowMenu();
                        TableColumn clickedColumn = fixedTable.getColumnModel().getColumn(fixedTable.columnAtPoint(e.getPoint()));
                        int clickedColumnIndex = fixedTable.getColumnModel().getColumnIndex(clickedColumn.getIdentifier());
                        showMenu.setEnabled(true);
                        unfixCheckBox.setEnabled(true);
                        if (hiddenColumns.size() == 0)
                        {
                            showMenu.setEnabled(false);
                        }
                        if (fixedTable.getColumnModel().getColumnCount() <= 1)
                        {
                            unfixCheckBox.setEnabled(false);
                        }
                        fixedTableMenu.show(e.getComponent(), e.getX(), e.getY());
                        unfixCheckBox.addItemListener(new ItemListener()
                        {

                            @Override
                            public void itemStateChanged(ItemEvent e)
                            {
                                if (unfixCheckBox.isSelected() == false)
                                {
                                    shownColumns.add(fixedTable.getColumnModel().getColumn(clickedColumnIndex));
                                    fromFixedTableToMainTable(clickedColumnIndex);
                                    updateShowMenu();
                                    checkNewIndexes();
                                    fixedTableMenu.setVisible(false);
                                    unfixCheckBox.setSelected(true);
                                }
                            }
                        });

                        for (int j = 0; j < hiddenColumns.size(); j++)
                        {
                            JMenuItem currentItem = hiddenHeaderItems.get(j);
                            String currentColumnName = hiddenColumns.get(j).getHeaderValue().toString();
                            currentItem.addActionListener(new ActionListener()
                            {

                                @Override
                                public void actionPerformed(ActionEvent e)
                                {
                                    showColumn(currentColumnName, indexForEachHiddenColumn.get(currentColumnName), true);
                                    checkNewIndexes();
                                }
                            });
                        }
                    }
                }

            });

            this.buildAttributeControls();
        }

        checkNewIndexes();

        this.setRowSelectionAllowed(true);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    private void buildAttributeControls()
    {
        showAllItem.addActionListener(e ->
        {
            showAllColumns();
            checkNewIndexes();
        });
        hideAllItem.addActionListener(e ->
        {
            hideAllColumns();
            checkNewIndexes();
        });
        resetItem.addActionListener(e ->
        {
            resetColumnsPositions();
            checkNewIndexes();
        });
        loadStateItem.addActionListener(e -> loadTableState());
        saveStateItem.addActionListener(e ->
        {
            try
            {
                saveTableState();
            } catch (XMLStreamException ex)
            {
                ErrorHandling.showErrorDialog("Error");
                ex.printStackTrace();
            }
        });

        attributesItem.addItemListener(e ->
        {
            if (attributesItem.isSelected())
            {
                if (!isAttributeCellExpanded())
                {
                    attributesInDifferentColumns();
                }
            } else
            {
                if (isAttributeCellExpanded())
                {
                    attributesInOneColumn();
                }
            }
        });

        mainTable.getColumnModel().addColumnModelListener(new TableColumnModelListener()
        {
            @Override
            public void columnAdded(TableColumnModelEvent e)
            {
            }

            @Override
            public void columnRemoved(TableColumnModelEvent e)
            {
            }

            @Override
            public void columnMoved(TableColumnModelEvent e)
            {
                checkNewIndexes();
            }

            @Override
            public void columnMarginChanged(ChangeEvent e)
            {
            }

            @Override
            public void columnSelectionChanged(ListSelectionEvent e)
            {
            }
        });
    }

    private void addKeyboardActions()
    {
        // Key input
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK), "shiftUpArrow");
        this.getActionMap().put("shiftUpArrow", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                final AdvancedJTable_networkElement table = AdvancedJTable_networkElement.this;
                final int upRow = Ints.min(table.getSelectedRows());
                final int downRow = Ints.max(table.getSelectedRows());

                int row = upRow - 1;
                if (row < 0) return;
                table.setRowSelectionInterval(row, downRow);

                final ElementSelection selectedElements = getSelectedElements();
                if (selectedElements.isEmpty()) return;
                SwingUtilities.invokeLater(() -> showInCanvas(selectedElements));
            }
        });

        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "upArrow");
        this.getActionMap().put("upArrow", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final AdvancedJTable_networkElement table = AdvancedJTable_networkElement.this;
                final int upRow = Ints.min(table.getSelectedRows());

                int row = upRow - 1;
                if (row < 0) return;
                table.setRowSelectionInterval(row, row);

                final ElementSelection selectedElements = getSelectedElements();
                if (selectedElements.isEmpty()) return;
                SwingUtilities.invokeLater(() -> showInCanvas(selectedElements));
            }
        });

        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK), "shiftDownArrow");
        this.getActionMap().put("shiftDownArrow", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                final AdvancedJTable_networkElement table = AdvancedJTable_networkElement.this;
                final int upRow = Ints.min(table.getSelectedRows());
                final int downRow = Ints.max(table.getSelectedRows());

                int row = downRow + 1;
                if (hasAggregationRow() && row == (table.getRowCount() - 1)) return;
                table.setRowSelectionInterval(upRow, row);

                final ElementSelection selectedElements = getSelectedElements();
                if (selectedElements.isEmpty()) return;
                SwingUtilities.invokeLater(() -> showInCanvas(selectedElements));
            }
        });

        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "downArrow");
        this.getActionMap().put("downArrow", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final AdvancedJTable_networkElement table = AdvancedJTable_networkElement.this;
                final int downRow = Ints.max(table.getSelectedRows());

                int row = downRow + 1;

                if (hasAggregationRow() && row == table.getRowCount() - 1) return;
                table.setRowSelectionInterval(row, row);

                final ElementSelection selectedElements = getSelectedElements();
                if (selectedElements.isEmpty()) return;
                SwingUtilities.invokeLater(() -> showInCanvas(selectedElements));
            }
        });
    }

    /**
     * Re-configures the menu to show hidden columns
     */
    private void updateShowMenu()
    {
        showMenu.removeAll();
        hiddenHeaderItems = new ArrayList<>();
        for (int i = 0; i < hiddenColumns.size(); i++)
        {
            hiddenHeaderItems.add(new JMenuItem(hiddenColumns.get(i).getHeaderValue().toString()));
            showMenu.add(hiddenHeaderItems.get(i));
        }
    }

    /**
     * Show all columns which are hidden
     */
    private void showAllColumns()
    {
        while (hiddenColumnsNames.size() > 0)
        {
            String s = hiddenColumnsNames.get(hiddenColumnsNames.size() - 1);
            showColumn(s, indexForEachHiddenColumn.get(s), true);
        }
        checkNewIndexes();
        hiddenColumns.clear();
        hiddenColumnsNames.clear();
        shownColumns.clear();
        for (int i = 0; i < mainTable.getColumnModel().getColumnCount(); i++)
        {
            shownColumns.add(mainTable.getColumnModel().getColumn(i));
        }
    }

    /**
     * Hide all columns unless the first one of mainTable which are shown
     */

    private void hideAllColumns()
    {
        TableColumn columnToHide = null;
        String hiddenColumnHeader = null;
        while (mainTable.getColumnModel().getColumnCount() > 1)
        {
            columnToHide = mainTable.getColumnModel().getColumn(1);
            hiddenColumnHeader = columnToHide.getHeaderValue().toString();
            hiddenColumns.add(columnToHide);
            hiddenColumnsNames.add(columnToHide.getHeaderValue().toString());
            indexForEachHiddenColumn.put(hiddenColumnHeader, 1);
            mainTable.getColumnModel().removeColumn(columnToHide);
            shownColumns.remove(columnToHide);
        }
        checkNewIndexes();
    }

    /**
     * Show one column which is hidden
     *
     * @param columnName  Name of the column which we want to show
     * @param columnIndex Index which the column had when it was shown
     * @param move        true if column will be moved, false if column will be shown at the end
     * @return The column to be shown
     */
    private void showColumn(String columnName, int columnIndex, boolean move)
    {

        String hiddenColumnName;
        TableColumn columnToShow = null;
        for (TableColumn tc : hiddenColumns)
        {
            hiddenColumnName = tc.getHeaderValue().toString();
            if (columnName.equals(hiddenColumnName))
            {
                mainTable.getColumnModel().addColumn(tc);
                if (move)
                    mainTable.getColumnModel().moveColumn(mainTable.getColumnCount() - 1, columnIndex);
                shownColumns.add(tc);
                indexForEachHiddenColumn.remove(columnName, columnIndex);
                columnToShow = tc;
                break;
            }
        }
        hiddenColumns.remove(columnToShow);
        hiddenColumnsNames.clear();
        for (TableColumn tc : hiddenColumns)
        {
            hiddenColumnsNames.add(tc.getHeaderValue().toString());
        }
    }

    /**
     * Hide one column which is shown
     *
     * @param columnIndex Index which the column has in the current Table
     * @return The column to be hidden
     */
    private void hideColumn(int columnIndex)
    {
        TableColumn columnToHide = mainTable.getColumnModel().getColumn(columnIndex);
        String hiddenColumnHeader = columnToHide.getHeaderValue().toString();
        hiddenColumns.add(columnToHide);
        hiddenColumnsNames.add(hiddenColumnHeader);
        shownColumns.remove(columnToHide);
        indexForEachHiddenColumn.put(hiddenColumnHeader, columnIndex);
        mainTable.getColumnModel().removeColumn(columnToHide);
    }

    /**
     * Move one column from mainTable to fixedTable
     *
     * @param columnIndex Index which the column has in mainTable
     */
    private void fromMainTableToFixedTable(int columnIndex)
    {
        TableColumn columnToFix = mainTable.getColumnModel().getColumn(columnIndex);
        mainTable.getColumnModel().removeColumn(columnToFix);
        fixedTable.getColumnModel().addColumn(columnToFix);
    }

    /**
     * Move one column from fixedTable to mainTable
     *
     * @param columnIndex Index which the column has in fixedTable
     */

    private void fromFixedTableToMainTable(int columnIndex)
    {
        TableColumn columnToUnfix = fixedTable.getColumnModel().getColumn(columnIndex);
        fixedTable.getColumnModel().removeColumn(columnToUnfix);
        mainTable.getColumnModel().addColumn(columnToUnfix);
        mainTable.getColumnModel().moveColumn(mainTable.getColumnModel().getColumnCount() - 1, 0);

    }

    /**
     * When a new column is added, update the tables
     *
     * @param
     */

    private void updateTables()
    {
        String fixedTableColumn;
        String mainTableColumn;
        ArrayList<Integer> columnIndexesToRemove = new ArrayList<>();

        for (int i = 0; i < fixedTable.getColumnModel().getColumnCount(); i++)
        {
            fixedTableColumn = fixedTable.getColumnModel().getColumn(i).getHeaderValue().toString();
            for (int j = 0; j < mainTable.getColumnModel().getColumnCount(); j++)
            {
                mainTableColumn = mainTable.getColumnModel().getColumn(j).getHeaderValue().toString();
                if (mainTableColumn.equals(fixedTableColumn))
                {
                    columnIndexesToRemove.add(j);
                }
            }
        }
        int counter = 0;
        for (int i = 0; i < columnIndexesToRemove.size(); i++)
        {

            if (i > 0)
            {
                for (int j = 0; j < i; j++)
                {
                    if (columnIndexesToRemove.get(j) < columnIndexesToRemove.get(i))
                    {
                        counter++;
                    }
                }

            }
            mainTable.getColumnModel().removeColumn(mainTable.getColumnModel().getColumn(columnIndexesToRemove.get(i) - counter));
            counter = 0;
        }
        if (removedColumns.size() > 0)
        {
            TableColumn columnRemoved = null;
            String mainTableColumnToCheck = null;
            for (int j = 0; j < removedColumns.size(); j++)
            {
                columnRemoved = removedColumns.get(j);
                for (int k = 0; k < mainTable.getColumnModel().getColumnCount(); k++)
                {
                    mainTableColumnToCheck = mainTable.getColumnModel().getColumn(k).getHeaderValue().toString();
                    if (mainTableColumnToCheck.equals(columnRemoved.getHeaderValue().toString()))
                    {
                        mainTable.getColumnModel().removeColumn(mainTable.getColumnModel().getColumn(k));
                    }
                }
            }


        }
        if (hiddenColumns.size() > 0)
        {


            String hiddenColumnName = null;
            String mainTableColumnName = null;
            for (TableColumn tc : hiddenColumns)
            {
                hiddenColumnName = tc.getHeaderValue().toString();
                for (int k = 0; k < mainTable.getColumnModel().getColumnCount(); k++)
                {
                    mainTableColumnName = mainTable.getColumnModel().getColumn(k).getHeaderValue().toString();
                    if (hiddenColumnName.equals(mainTableColumnName))
                    {
                        mainTable.getColumnModel().removeColumn(mainTable.getColumnModel().getColumn(k));
                    }
                }
            }
        }

    }

    /**
     * Saves the current positions of the columns
     *
     * @param
     */
    private void saveColumnsPositions()
    {
        mapToSaveState.clear();
        String currentColumnName = "";
        for (int i = 0; i < mainTable.getColumnModel().getColumnCount(); i++)
        {
            currentColumnName = mainTable.getColumnModel().getColumn(i).getHeaderValue().toString();
            mapToSaveState.put(i, currentColumnName);
        }
    }

    /**
     * Restore for each column its previous position
     *
     * @param
     */
    private void restoreColumnsPositions()
    {
        TableColumn columnToHide;
        String hiddenColumnHeader;
        while (mainTable.getColumnModel().getColumnCount() > 0)
        {
            columnToHide = mainTable.getColumnModel().getColumn(0);
            hiddenColumnHeader = columnToHide.getHeaderValue().toString();
            hiddenColumns.add(columnToHide);
            hiddenColumnsNames.add(columnToHide.getHeaderValue().toString());
            indexForEachHiddenColumn.put(hiddenColumnHeader, 0);
            mainTable.getColumnModel().removeColumn(columnToHide);
            shownColumns.remove(columnToHide);
        }
        String currentColumnName;
        for (int j = 0; j < mapToSaveState.size(); j++)
        {
            currentColumnName = mapToSaveState.get(j);
            showColumn(currentColumnName, j, false);
        }

        checkNewIndexes();
    }

    /**
     * Loads a table state from a external file
     *
     * @param
     */
    private void loadTableState()
    {
//        Map<NetworkElementType, AdvancedJTable_networkElement> currentTables = callback.getTables();
//        HashMap<NetworkElementType, TableState> tStateMap = null;
//        try
//        {
//            tStateMap = TableStateController.loadTableState(currentTables);
//        } catch (XMLStreamException e)
//        {
//            e.printStackTrace();
//        }
//        for (Map.Entry<NetworkElementType, AdvancedJTable_networkElement> entry : currentTables.entrySet())
//        {
//            entry.getValue().updateTableFromTableState(tStateMap.get(entry.getValue().getNetworkElementType()));
//        }
//        JOptionPane.showMessageDialog(null, "Tables visualization profile successfully loaded!");
    }

    /**
     * Saves the current table state on a external file
     */
    private void saveTableState() throws XMLStreamException
    {
//        Map<NetworkElementType, AdvancedJTable_networkElement> currentTables = callback.getTables();
//        TableStateController.saveTableState(currentTables);
    }

    /**
     * Update columns positions from a table state
     *
     * @param state TableState where the table configuration is saved
     */
    private void updateTableFromTableState(TableState state)
    {
        resetColumnsPositions();
        TableColumn fixedTableCol = null;
        while (fixedTable.getColumnModel().getColumnCount() > 0)
        {
            fixedTableCol = fixedTable.getColumnModel().getColumn(0);
            fixedTable.getColumnModel().removeColumn(fixedTableCol);
        }
        createDefaultColumnsFromModel();
        ArrayList<String> fixedTableColumns = state.getFixedTableColumns();
        ArrayList<String> mainTableColumns = state.getMainTableColumns();
        HashMap<String, Integer> hiddenColumnsMap = state.getHiddenTableColumns();
        boolean areAttributesExpanded = state.getExpandAttributes();

        String[] currentHeaders = getCurrentTableHeaders();
        ArrayList<String> currentHeadersList = new ArrayList<>();

        for (int i = 0; i < currentHeaders.length; i++)
        {
            currentHeadersList.add(currentHeaders[i]);
        }

        if (areAttributesExpanded && getAttributesColumnsHeaders().size() > 0)
        {
            attributesInDifferentColumns();
            attributesItem.setSelected(true);
        }

        for (String col : fixedTableColumns)
        {
            if (!currentHeadersList.contains(col))
                continue;
            TableColumn mainTableCol = null;
            for (int i = 0; i < mainTable.getColumnModel().getColumnCount(); i++)
            {
                mainTableCol = mainTable.getColumnModel().getColumn(i);
                if (col.equals(mainTableCol.getHeaderValue().toString()))
                {
                    mainTable.getColumnModel().removeColumn(mainTableCol);
                    fixedTable.getColumnModel().addColumn(mainTableCol);
                    break;
                }
            }
        }

        while (mainTable.getColumnModel().getColumnCount() > 0)
        {
            hideColumn(0);
        }
        for (String col : mainTableColumns)
        {
            if (!currentHeadersList.contains(col))
                continue;
            showColumn(col, 0, false);
        }
        indexForEachHiddenColumn.clear();
        for (Map.Entry<String, Integer> entry : hiddenColumnsMap.entrySet())
        {
            if (!currentHeadersList.contains(entry.getKey()))
                continue;
            indexForEachHiddenColumn.put(entry.getKey(), entry.getValue());
        }

    }

    /**
     * Reset the column positions
     *
     * @param
     */
    private void resetColumnsPositions()
    {
        hiddenColumns.clear();
        removedColumns.clear();
        hiddenColumnsNames.clear();
        indexForEachHiddenColumn.clear();
        TableColumn tc = null;
        while (fixedTable.getColumnModel().getColumnCount() > 0)
        {
            tc = fixedTable.getColumnModel().getColumn(0);
            fixedTable.getColumnModel().removeColumn(tc);
        }
        mainTable.createDefaultColumnsFromModel();
        for (int i = 0; i < getNumberOfDecoratorColumns(); i++)
        {
            tc = mainTable.getColumnModel().getColumn(0);
            mainTable.getColumnModel().removeColumn(tc);
            fixedTable.getColumnModel().addColumn(tc);
        }
        for (String s : getAttributesColumnsHeaders())
        {
            removeNewColumn("Att: " + s);
        }
        expandAttributes = false;
        attributesItem.setSelected(false);
        updateTables();
        checkNewIndexes();
    }

    /**
     * When a column is moved into mainTable,
     * we have to know which are the new indexes and update indexForEachColumn
     */
    private void checkNewIndexes()
    {
        indexForEachColumn.clear();

        for (int i = 0; i < mainTable.getColumnModel().getColumnCount(); i++)
            indexForEachColumn.put(mainTable.getColumnModel().getColumn(i).getHeaderValue().toString(), i);

        for (int i = 0; i < fixedTable.getColumnModel().getColumnCount(); i++)
            indexForEachColumn.put(fixedTable.getColumnModel().getColumn(i).getHeaderValue().toString(), i);
    }

    /**
     * Remove a column from mainTable
     *
     * @param columnToRemoveName name of the column which is going to be removed
     */
    private void removeNewColumn(String columnToRemoveName)
    {
        TableColumn columnToRemove = null;
        for (int j = 0; j < getColumnModel().getColumnCount(); j++)
        {
            if (columnToRemoveName.equals(getColumnModel().getColumn(j).getHeaderValue().toString()))
            {
                columnToRemove = getColumnModel().getColumn(j);
                mainTable.getColumnModel().removeColumn(getColumnModel().getColumn(j));
                break;
            }
        }
        removedColumns.add(columnToRemove);
        TableColumn columnToRemoveFromShownColumns = null;
        for (TableColumn tc : shownColumns)
        {
            if (columnToRemoveName.equals(tc.getHeaderValue().toString()))
            {
                columnToRemoveFromShownColumns = tc;
            }
        }
        shownColumns.remove(columnToRemoveFromShownColumns);

    }

    /**
     * Recover a removed column and adds it in mainTable
     *
     * @param columnToRecoverName column of the column which is going to be recovered
     */
    private void recoverRemovedColumn(String columnToRecoverName)
    {
        TableColumn columnToRecover = null;
        for (TableColumn tc : removedColumns)
        {
            if (tc.getHeaderValue().toString().equals(columnToRecoverName))
            {
                columnToRecover = tc;
            }
        }
        removedColumns.remove(columnToRecover);
        shownColumns.add(columnToRecover);
        mainTable.getColumnModel().addColumn(columnToRecover);
    }

    /**
     * Expands attributes in different columns, one for each attribute
     */
    private void attributesInDifferentColumns()
    {
        saveColumnsPositions();
        boolean attributesColumnInMainTable = false;
        String currentColumnName = null;
        for (int i = 0; i < mainTable.getColumnModel().getColumnCount(); i++)
        {
            currentColumnName = mainTable.getColumnModel().getColumn(i).getHeaderValue().toString();
            if (currentColumnName.equals("Attributes"))
            {
                attributesColumnInMainTable = true;
                break;
            }
        }
        if (!attributesColumnInMainTable)
        {
            JOptionPane.showMessageDialog(null, "Attributes column must " +
                    "be unlocked and visible to expand attributes into different columns");
            attributesItem.setSelected(false);
        } else
        {
            if (getAttributesColumnsHeaders().size() > 0)
            {
                callback.updateVisualizationJustTables();
                createDefaultColumnsFromModel();
                removedColumns.clear();
                removeNewColumn("Attributes");
                updateTables();
                expandAttributes = true;
                restoreColumnsPositions();
                for (String att : getAttributesColumnsHeaders())
                {
                    showColumn("Att: " + att, 0, false);
                }
                checkNewIndexes();
            } else
            {
                attributesItem.setSelected(false);
            }
        }
    }

    /**
     * Contracts attributes in different columns, one for each attribute
     */

    private void attributesInOneColumn()
    {
        saveColumnsPositions();
        int attributesCounter = 0;
        String columnToCheck = null;
        for (String att : getAttributesColumnsHeaders())
        {
            for (int j = 0; j < mainTable.getColumnModel().getColumnCount(); j++)
            {
                columnToCheck = mainTable.getColumnModel().getColumn(j).getHeaderValue().toString();
                if (columnToCheck.equals("Att: " + att))
                {
                    attributesCounter++;
                    break;
                }
            }
        }
        if (!(attributesCounter == getAttributesColumnsHeaders().size()))
        {
            JOptionPane.showMessageDialog(null, "All attributes columns must be unlocked and visible to contract them in one column");
            attributesItem.setSelected(true);
        } else
        {

            if (getAttributesColumnsHeaders().size() > 0)
            {

                callback.updateVisualizationJustTables();
                createDefaultColumnsFromModel();
                removedColumns.clear();
                for (String att : getAttributesColumnsHeaders())
                {
                    removeNewColumn("Att: " + att);
                }
                updateTables();
                expandAttributes = false;
                restoreColumnsPositions();
                showColumn("Attributes", 0, false);
                checkNewIndexes();
            } else
            {
                attributesItem.setSelected(true);
            }
        }
    }

    public ArrayList<String> getMainTableColumns()
    {
        ArrayList<String> mainTableColumns = new ArrayList<>();
        for (int i = 0; i < mainTable.getColumnModel().getColumnCount(); i++)
        {
            mainTableColumns.add(mainTable.getColumnModel().getColumn(i).getHeaderValue().toString());
        }

        return mainTableColumns;
    }

    public ArrayList<String> getFixedTableColumns()
    {
        ArrayList<String> fixedTableColumns = new ArrayList<>();
        for (int i = 0; i < fixedTable.getColumnModel().getColumnCount(); i++)
        {
            fixedTableColumns.add(fixedTable.getColumnModel().getColumn(i).getHeaderValue().toString());
        }

        return fixedTableColumns;
    }

    public HashMap<String, Integer> getHiddenColumns()
    {
        HashMap<String, Integer> hiddenTableColumns = new HashMap<>();

        for (TableColumn hiddenColumn : hiddenColumns)
        {
            String col = hiddenColumn.getHeaderValue().toString();
            hiddenTableColumns.put(col, indexForEachHiddenColumn.get(col));
        }

        return hiddenTableColumns;
    }

    public boolean isAttributeCellExpanded()
    {
        return expandAttributes;
    }

    public JScrollPane getScroll()
    {
        return scroll;
    }

    public NetworkElementType getNetworkElementType()
    {
        return networkElementType;
    }

    public FixedColumnDecorator getDecorator()
    {
        return decorator;
    }

    public JTable getMainTable()
    {
        return mainTable;
    }

    public JTable getFixedTable()
    {
        return fixedTable;
    }

    public void updateView(NetPlan currentState)
    {
        this.setEnabled(false);

        this.saveColumnsPositions();

        if (currentState.getRoutingType() == RoutingType.SOURCE_ROUTING && networkElementType.equals(NetworkElementType.FORWARDING_RULE))
            return;
        if (currentState.getRoutingType() == RoutingType.HOP_BY_HOP_ROUTING && (networkElementType.equals(NetworkElementType.ROUTE)))
            return;

        final List<? extends SortKey> sortKeys = this.getRowSorter().getSortKeys();
        final String[] tableHeaders = getCurrentTableHeaders();

        if (!hasElements())
            ((DefaultTableModel) getModel()).setDataVector(new Object [1][tableHeaders.length], tableHeaders);

        if (hasElements())
        {
            ArrayList<String> attColumnsHeaders = getAttributesColumnsHeaders();

            List<Object[]> allData = getAllData(currentState, attColumnsHeaders);
            ((DefaultTableModel) getModel()).setDataVector(allData.toArray(new Object[allData.size()][tableHeaders.length]), tableHeaders);
            this.createDefaultColumnsFromModel();

            if (attColumnsHeaders != null && networkElementType != NetworkElementType.FORWARDING_RULE)
            {

                final String[] columnTips = getTableTips();
                final String[] columnHeader = getTableHeaders();

                // Tips
                final ColumnHeaderToolTips tips = new ColumnHeaderToolTips();

                for (int c = 0; c < columnHeader.length; c++)
                    tips.setToolTip(getColumnModel().getColumn(c), columnTips[c]);

                this.getTableHeader().addMouseMotionListener(tips);

                if (isAttributeCellExpanded())
                    removeNewColumn("Attributes");
                else if (attColumnsHeaders.size() > 0)
                    for (String att : attColumnsHeaders)
                        removeNewColumn("Att: " + att);

                updateTables();

                restoreColumnsPositions();

                hiddenColumnsAux = new ArrayList<>();
                if (isAttributeCellExpanded())
                {
                    for (TableColumn col : hiddenColumns)
                    {
                        hiddenColumnsAux.add(col.getHeaderValue().toString());
                    }
                    for (String hCol : hiddenColumnsAux)
                    {
                        for (String att : getAttributesColumnsHeaders())
                        {
                            if (hCol.equals("Att: " + att))
                            {
                                showColumn("Att: " + att, 0, false);
                                break;
                            }

                        }
                    }
                }
            }
            setColumnRowSorting();
        }

        this.getRowSorter().setSortKeys(sortKeys);

        setEnabled(true);
    }

    private class PopupMenuMouseAdapter extends MouseAdapter
    {
        @Override
        public void mouseClicked(final MouseEvent e)
        {
            try
            {
                final ElementSelection selection = getSelectedElements();

                if (SwingUtilities.isRightMouseButton(e))
                {
                    getPopup(selection).show(e.getComponent(), e.getX(), e.getY());
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e))
                {
                    if (selection.isEmpty())
                        callback.resetPickedStateAndUpdateView();
                    else
                        SwingUtilities.invokeLater(() -> showInCanvas(selection));
                }
            } catch (Exception ex)
            {
                ErrorHandling.showErrorDialog("The GUI has suffered a problem.\nPlease see the console for more information.", "Error");
                ex.printStackTrace();
            }
        }
    }

    protected final void addPopupMenuAttributeOptions(ElementSelection selection, JPopupMenu popup)
    {
        assert popup != null;
        assert selection != null;

        if (networkElementType == NetworkElementType.FORWARDING_RULE) return;

        final List<? extends NetworkElement> selectedElements = selection.getNetworkElements();

        if (!selectedElements.isEmpty())
        {
            popup.addSeparator();

            // Tags controls
            JMenuItem addTag = new JMenuItem("Add tag" + (networkElementType == NetworkElementType.LAYER ? "" : " to selected elements"));
            addTag.addActionListener(e1 ->
            {
                JTextField txt_name = new JTextField(20);

                JPanel pane = new JPanel();
                pane.add(new JLabel("Tag: "));
                pane.add(txt_name);

                while (true)
                {
                    int result = JOptionPane.showConfirmDialog(null, pane, "Please enter tag name", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return;
                    String tag;
                    try
                    {
                        if (txt_name.getText().isEmpty()) continue;

                        tag = txt_name.getText();

                        for (NetworkElement selectedElement : selectedElements)
                            selectedElement.addTag(tag);

                        callback.updateVisualizationJustTables();
                    } catch (Throwable ex)
                    {
                        ErrorHandling.addErrorOrException(ex, getClass());
                        ErrorHandling.showErrorDialog("Error adding/editing tag");
                    }
                    break;
                }
            });
            popup.add(addTag);

            JMenuItem removeTag = new JMenuItem("Remove tag" + (networkElementType == NetworkElementType.LAYER ? "" : " from selected elements"));

            removeTag.addActionListener(e1 ->
            {
                try
                {
                    final Set<String> tags = new HashSet<>();
                    for (NetworkElement selectedElement : selectedElements)
                    {
                        final Set<String> elementTags = selectedElement.getTags();
                        tags.addAll(elementTags);
                    }

                    String[] tagList = StringUtils.toArray(tags);

                    if (tagList.length == 0) throw new Exception("No tag to remove");

                    Object out = JOptionPane.showInputDialog(null, "Please, select a tag to remove", "Remove tag", JOptionPane.QUESTION_MESSAGE, null, tagList, tagList[0]);
                    if (out == null) return;

                    String tagToRemove = out.toString();

                    for (NetworkElement selectedElement : selectedElements)
                        selectedElement.removeTag(tagToRemove);

                    callback.updateVisualizationJustTables();

                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing tag");
                }
            });

            popup.add(removeTag);

            JMenuItem addAttribute = new JMenuItem("Add/Update attribute" + (networkElementType == NetworkElementType.LAYER ? "" : " to selected elements"));
            popup.add(new JPopupMenu.Separator());
            addAttribute.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    JTextField txt_key = new JTextField(20);
                    JTextField txt_value = new JTextField(20);

                    JPanel pane = new JPanel();
                    pane.add(new JLabel("Attribute: "));
                    pane.add(txt_key);
                    pane.add(Box.createHorizontalStrut(15));
                    pane.add(new JLabel("Value: "));
                    pane.add(txt_value);

                    while (true)
                    {
                        int result = JOptionPane.showConfirmDialog(null, pane, "Please enter an attribute name and its value", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (result != JOptionPane.OK_OPTION) return;
                        String attribute, value;
                        try
                        {
                            if (txt_key.getText().isEmpty())
                            {
                                ErrorHandling.showWarningDialog("Please, insert an attribute name.", "Message");
                                continue;
                            }

                            attribute = txt_key.getText();
                            value = txt_value.getText();

                            for (NetworkElement selectedElement : selectedElements)
                                selectedElement.setAttribute(attribute, value);

                            callback.updateVisualizationJustTables();
                        } catch (Throwable ex)
                        {
                            ErrorHandling.addErrorOrException(ex, getClass());
                            ErrorHandling.showErrorDialog("Error adding/editing attribute");
                        }
                        break;
                    }
                }
            });
            popup.add(addAttribute);

            JMenuItem removeAttribute = new JMenuItem("Remove attribute" + (networkElementType == NetworkElementType.LAYER ? "" : " from selected elements"));

            removeAttribute.addActionListener(e1 ->
            {
                try
                {
                    final Set<String> attributes = new HashSet<>();
                    for (NetworkElement selectedElement : selectedElements)
                    {
                        attributes.addAll(selectedElement.getAttributes().keySet());
                    }

                    String[] attributeList = StringUtils.toArray(attributes);

                    if (attributeList.length == 0) throw new Exception("No attribute to remove");

                    Object out = JOptionPane.showInputDialog(null, "Please, select an attribute to remove", "Remove attribute", JOptionPane.QUESTION_MESSAGE, null, attributeList, attributeList[0]);
                    if (out == null) return;

                    String attributeToRemove = out.toString();

                    for (NetworkElement selectedElement : selectedElements)
                        selectedElement.removeAttribute(attributeToRemove);

                    callback.updateVisualizationJustTables();
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing attribute");
                }
            });
            popup.add(removeAttribute);

            JMenuItem removeAttributes = new JMenuItem("Remove all attributes" + (networkElementType == NetworkElementType.LAYER ? "" : " from selected elements"));

            removeAttributes.addActionListener(e1 ->
            {
                try
                {
                    for (NetworkElement selectedElement : selectedElements)
                        selectedElement.removeAllAttributes();

                    if (isAttributeCellExpanded())
                    {
                        recoverRemovedColumn("Attributes");
                        expandAttributes = false;
                        attributesItem.setSelected(false);
                    }

                    callback.updateVisualizationJustTables();
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing attributes");
                }
            });

            popup.add(removeAttributes);
            popup.addSeparator();

            JMenuItem editAttributes = new JMenuItem("Edit attributes" + (networkElementType == NetworkElementType.LAYER ? "" : " from selected elements"));
            editAttributes.addActionListener(e1 ->
            {
                try
                {
                    JDialog dialog = new AttributeEditor(callback, selection);
                    dialog.setVisible(true);
                    callback.updateVisualizationJustTables();
                } catch (Throwable ex)
                {
                    ex.printStackTrace();
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying attributes");
                }
            });

            popup.add(editAttributes);
        }
    }

    protected static class ColumnComparator implements Comparator<Object>
    {
        private final boolean isDoubleWithParenthesis;
        private final RowSorter rs;

        public ColumnComparator(RowSorter rs, boolean isDoubleWithParenthesis)
        {
            this.rs = rs;
            this.isDoubleWithParenthesis = isDoubleWithParenthesis;
        }

        @Override
        public int compare(Object o1, Object o2)
        {

            if (o1 instanceof LastRowAggregatedValue)
            {
                final boolean ascending = ((List<? extends SortKey>) rs.getSortKeys()).get(0).getSortOrder() == SortOrder.ASCENDING;
                return ascending ? 1 : -1;
            }
            if (o2 instanceof LastRowAggregatedValue)
            {
                final boolean ascending = ((List<? extends SortKey>) rs.getSortKeys()).get(0).getSortOrder() == SortOrder.ASCENDING;
                return ascending ? -1 : 1;
            }
            if (o1 instanceof Boolean)
            {
                final Boolean oo1 = (Boolean) o1;
                final Boolean oo2 = (Boolean) o2;
                return oo1.compareTo(oo2);
            }
            if (o1 instanceof Number)
            {
                final Number oo1 = (Number) o1;
                final Number oo2 = (Number) o2;
                return new Double(oo1.doubleValue()).compareTo(new Double(oo2.doubleValue()));
            }
            String oo1 = (String) o1;
            String oo2 = (String) o2;
            if (!isDoubleWithParenthesis)
                return oo1.compareTo(oo2);

            int pos1 = oo1.indexOf(" (");
            if (pos1 != -1) oo1 = oo1.substring(0, pos1);

            int pos2 = oo2.indexOf(" (");
            if (pos2 != -1) oo2 = oo2.substring(0, pos2);

            try
            {
                Double d1, d2;
                d1 = Double.parseDouble(oo1);
                d2 = Double.parseDouble(oo2);
                return d1.compareTo(d2);
            } catch (Throwable e)
            {
                return oo1.compareTo(oo2);
            }
        }
    }

    public static class LastRowAggregatedValue
    {
        private String value;

        public LastRowAggregatedValue()
        {
            value = "---";
        }

        public LastRowAggregatedValue(int val)
        {
            value = "" + val;
        }

        public LastRowAggregatedValue(double val)
        {
            value = String.format("%.2f", val);
        }

        public LastRowAggregatedValue(String value)
        {
            this.value = value;
        }

        String getValue()
        {
            return value;
        }

        public String toString()
        {
            return value;
        }
    }

    /**
     * Gets the selected elements in this table.
     *
     * @return
     */
    private ElementSelection getSelectedElements()
    {
        final int[] rowIndexes = this.getSelectedRows();
        final NetPlan np = callback.getDesign();

        final List<NetworkElement> elementList = new ArrayList<>();
        final List<Pair<Demand, Link>> frList = new ArrayList<>();

        if (rowIndexes.length != 0)
        {
            final int maxValidRowIndex = model.getRowCount() - 1 - (hasAggregationRow() ? 1 : 0);
            final List<Integer> validRows = new ArrayList<Integer>();
            for (int a : rowIndexes) if ((a >= 0) && (a <= maxValidRowIndex)) validRows.add(a);

            if (networkElementType == NetworkElementType.FORWARDING_RULE)
            {
                for (int rowIndex : validRows)
                {
                    final String demandInfo = (String) ((DefaultTableModel) getModel()).getValueAt(rowIndex, AdvancedJTable_forwardingRule.COLUMN_DEMAND);
                    final String linkInfo = (String) ((DefaultTableModel) getModel()).getValueAt(rowIndex, AdvancedJTable_forwardingRule.COLUMN_OUTGOINGLINK);
                    final int demandIndex = Integer.parseInt(demandInfo.substring(0, demandInfo.indexOf("(")).trim());
                    final int linkIndex = Integer.parseInt(linkInfo.substring(0, linkInfo.indexOf("(")).trim());
                    frList.add(Pair.of(np.getDemand(demandIndex), np.getLink(linkIndex)));
                }
            } else
            {
                for (int rowIndex : validRows)
                {
                    final long id = (long) getModel().getValueAt(rowIndex, 0);
                    elementList.add(np.getNetworkElement(id));
                }
            }
        }

        // Parse into ElementSelection
        final Pair<List<NetworkElement>, List<Pair<Demand, Link>>> selection = Pair.of(elementList, frList);
        final boolean nothingSelected = selection.getFirst().isEmpty() && selection.getSecond().isEmpty();

        // Checking for selection type
        final ElementSelection elementHolder;

        if (!nothingSelected)
        {
            if (!selection.getFirst().isEmpty())
                elementHolder = new ElementSelection(NetworkElementType.getType(selection.getFirst()), selection.getFirst());
            else if (!selection.getSecond().isEmpty())
                elementHolder = new ElementSelection(selection.getSecond());
            else elementHolder = new ElementSelection();
        } else
        {
            elementHolder = new ElementSelection();
        }

        return elementHolder;
    }


    public boolean hasAggregationRow()
    {
        return !networkElementType.equals(NetworkElementType.LAYER) && !networkElementType.equals(NetworkElementType.NETWORK);
    }

    /* Dialog for filtering by tag */
    protected void dialogToFilterByTag(boolean onlyInActiveLayer, FilterCombinationType filterCombinationType)
    {
        JTextField txt_tagContains = new JTextField(30);
        JTextField txt_tagDoesNotContain = new JTextField(30);
        JPanel pane = new JPanel(new GridLayout(-1, 1));
        pane.add(new JLabel("Has tag (could be empty): "));
        pane.add(txt_tagContains);
        pane.add(new JLabel("AND does not have tag (could be empty): "));
        pane.add(txt_tagDoesNotContain);

        while (true)
        {
            int result = JOptionPane.showConfirmDialog(null, pane, "Filter elements by tag", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
            try
            {
                if (txt_tagContains.getText().isEmpty() && txt_tagDoesNotContain.getText().isEmpty())
                {
                    ErrorHandling.showErrorDialog("At least one input tag is required", "Invalid input");
                    continue;
                }

                final ITableRowFilter filter = new TBFTagBased(
                        callback.getDesign(), onlyInActiveLayer ? callback.getDesign().getNetworkLayerDefault() : null,
                        txt_tagContains.getText(), txt_tagDoesNotContain.getText());
                callback.getVisualizationState().updateTableRowFilter(filter, filterCombinationType);
                callback.updateVisualizationJustTables();
            } catch (Throwable ex)
            {
                ErrorHandling.addErrorOrException(ex, getClass());
                ErrorHandling.showErrorDialog("Error adding filter");
            }
            break;
        }
    }

    protected void addFilterOptions(ElementSelection selection, JPopupMenu popup)
    {
        final NetPlan netPlan = callback.getDesign();
        final VisualizationState vs = callback.getVisualizationState();
        final boolean isMultilayerDesign = netPlan.isMultilayer();

        for (boolean applyJustToThisLayer : isMultilayerDesign ? new boolean[]{true, false} : new boolean[]{true})
        {
            final JMenu submenuFilters;
            if (applyJustToThisLayer)
                submenuFilters = new JMenu("Filters: Apply to this layer");
            else
                submenuFilters = new JMenu("Filters: Apply to all layers");

            for (FilterCombinationType filterCombinationType : vs.getTableRowFilter() == null ? new FilterCombinationType[]{FilterCombinationType.INCLUDEIF_AND} : FilterCombinationType.values())
            {
                final JMenu filterCombinationSubMenu;
                switch (filterCombinationType)
                {
                    case INCLUDEIF_OR:
                        filterCombinationSubMenu = new JMenu("Add elements that...");
                        break;
                    case INCLUDEIF_AND:
                        filterCombinationSubMenu = new JMenu("Keep elements that...");
                        break;
                    default:
                        throw new RuntimeException();
                }

                final JMenuItem trafficBasedFilterMenu = new JMenuItem("Are affected by these " + networkElementType + "s");
                filterCombinationSubMenu.add(trafficBasedFilterMenu);
                trafficBasedFilterMenu.addActionListener(e1 ->
                {
                    if (selection.getSelectionType() == SelectionType.EMPTY) return;
                    TBFToFromCarriedTraffic filter = null;

                    if (selection.getSelectionType() == SelectionType.NETWORK_ELEMENT)
                    {
                        final List<? extends NetworkElement> selectedElements = selection.getNetworkElements();
                        final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
                        for (NetworkElement element : selectedElements)
                        {
                            final NetworkElementType type = NetworkElementType.getType(element);
                            if (type != null)
                            {

                                switch (type)
                                {
                                    case NODE:
                                        if (filter == null)
                                            filter = new TBFToFromCarriedTraffic((Node) element, layer, applyJustToThisLayer);
                                        else
                                            filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((Node) element, layer, applyJustToThisLayer));
                                        break;
                                    case LINK:
                                        if (filter == null)
                                            filter = new TBFToFromCarriedTraffic((Link) element, applyJustToThisLayer);
                                        else
                                            filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((Link) element, applyJustToThisLayer));
                                        break;
                                    case DEMAND:
                                        if (filter == null)
                                            filter = new TBFToFromCarriedTraffic((Demand) element, applyJustToThisLayer);
                                        else
                                            filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((Demand) element, applyJustToThisLayer));
                                        break;
                                    case MULTICAST_DEMAND:
                                        if (filter == null)
                                            filter = new TBFToFromCarriedTraffic((MulticastDemand) element, applyJustToThisLayer);
                                        else
                                            filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((MulticastDemand) element, applyJustToThisLayer));
                                        break;
                                    case ROUTE:
                                        if (filter == null)
                                            filter = new TBFToFromCarriedTraffic((Route) element, applyJustToThisLayer);
                                        else
                                            filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((Route) element, applyJustToThisLayer));
                                        break;
                                    case MULTICAST_TREE:
                                        if (filter == null)
                                            filter = new TBFToFromCarriedTraffic((MulticastTree) element, applyJustToThisLayer);
                                        else
                                            filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((MulticastTree) element, applyJustToThisLayer));
                                        break;
                                    case RESOURCE:
                                        if (filter == null)
                                            filter = new TBFToFromCarriedTraffic((Resource) element, layer, applyJustToThisLayer);
                                        else
                                            filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((Resource) element, layer, applyJustToThisLayer));
                                        break;
                                    case SRG:
                                        if (filter == null)
                                            filter = new TBFToFromCarriedTraffic((SharedRiskGroup) element);
                                        else
                                            filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((SharedRiskGroup) element));
                                        break;
                                    default:
                                        // TODO: Control exceptions on filters.
                                        throw new RuntimeException();
                                }
                            }
                        }
                    } else
                    {
                        final List<Pair<Demand, Link>> forwardingRules = selection.getForwardingRules();

                        for (Pair<Demand, Link> forwardingRule : forwardingRules)
                        {
                            if (filter == null)
                                filter = new TBFToFromCarriedTraffic(forwardingRule, applyJustToThisLayer);
                            else
                                filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic(forwardingRule, applyJustToThisLayer));
                        }
                    }

                    callback.getVisualizationState().updateTableRowFilter(filter, filterCombinationType);
                    callback.updateVisualizationJustTables();
                });
                final JMenuItem tagFilterMenu = new JMenuItem("Have tag...");
                filterCombinationSubMenu.add(tagFilterMenu);
                tagFilterMenu.addActionListener(e1 -> dialogToFilterByTag(applyJustToThisLayer, filterCombinationType));

                submenuFilters.add(filterCombinationSubMenu);
            }

            if (applyJustToThisLayer)
            {
                final JMenuItem submenuFilters_filterIn = new JMenuItem("Keep only selected elements in this table");
                submenuFilters_filterIn.addActionListener(e1 ->
                {
                    TBFSelectionBased filter = new TBFSelectionBased(callback.getDesign(), selection);
                    callback.getVisualizationState().updateTableRowFilter(filter, FilterCombinationType.INCLUDEIF_AND);
                    callback.updateVisualizationJustTables();
                });
                final JMenuItem submenuFilters_filterOut = new JMenuItem("Filter-out selected elements in this table");
                submenuFilters_filterOut.addActionListener(e1 ->
                {
                    final ElementSelection invertedSelection = selection.invertSelection();
                    if (invertedSelection == null)
                        throw new Net2PlanException("Could not invert selection for the given elements.");

                    TBFSelectionBased filter = new TBFSelectionBased(callback.getDesign(), invertedSelection);
                    callback.getVisualizationState().updateTableRowFilter(filter, FilterCombinationType.INCLUDEIF_AND);
                    callback.updateVisualizationJustTables();
                });

                submenuFilters.add(submenuFilters_filterIn);
                submenuFilters.add(submenuFilters_filterOut);
            }

            popup.add(submenuFilters);
        }

        if (networkElementType != NetworkElementType.FORWARDING_RULE && networkElementType != NetworkElementType.NETWORK && networkElementType != NetworkElementType.LAYER)
        {
            popup.addSeparator();
            popup.add(new MenuItem_RemovedFiltered(callback, networkElementType));

            if (networkElementType == NetworkElementType.NODE || networkElementType == NetworkElementType.LINK)
                popup.add(new MenuItem_HideFiltered(callback, networkElementType));
        }
    }

    public void writeTableToFile(File file)
    {
        ExcelWriter.writeToFile(file, this.getTabName(), buildData());
    }

    protected static void updateRowSum (Object [] data , double [] aggreg , int index , double val)
    {
    	data [index] = val; aggreg [index] += val;
    }
    protected static void updateRowMax (Object [] data , double [] aggreg , int index , double val)
    {
    	data [index] = val; aggreg [index] = Math.max(val, aggreg[index]);
    }

    private Object[][] buildData()
    {
        final int fixedColumnCount = fixedTable.getColumnCount();
        final int mainColumnCount = mainTable.getColumnCount();
        final int rowCount = this.hasElements() ? this.getRowCount() : 0;

        Object[][] data = new Object[rowCount + 1][fixedColumnCount + mainColumnCount];

        // Headers
        for (int i = 0; i < fixedColumnCount; i++)
            data[0][i] = fixedTable.getColumnName(i);

        for (int i = 0; i < mainColumnCount; i++)
            data[0][fixedColumnCount + i] = mainTable.getColumnName(i);

        // Values
        for (int i = 0; i < rowCount; i++)
        {
            for (int j = 0; j < fixedColumnCount; j++)
                data[i + 1][j] = fixedTable.getValueAt(i, j);

            for (int j = 0; j < mainColumnCount; j++)
                data[i + 1][fixedColumnCount + j] = mainTable.getValueAt(i, j);
        }

        return data;
    }

    public long getElementID(int row)
    {
        final String COLUMN_ID = "Unique identifier";

        for (int i = 0; i < mainTable.getColumnCount(); i++)
        {
            if (COLUMN_ID.equals(mainTable.getColumnName(i)))
            {
                final Object value = mainTable.getValueAt(row, i);
                if (!(value instanceof Long)) return -1;
                return (long) value;
            }
        }

        for (int i = 0; i < fixedTable.getColumnCount(); i++)
        {
            if (COLUMN_ID.equals(fixedTable.getColumnName(i)))
            {
                final Object value = fixedTable.getValueAt(row, i);
                if (!(value instanceof Long)) return -1;
                return (long) value;
            }
        }

        return -1;
    }

    public abstract List<Object[]> getAllData(NetPlan currentState, ArrayList<String> attributesTitles);

    public abstract String getTabName();

    public abstract String[] getTableHeaders();

    public abstract String[] getCurrentTableHeaders();

    public abstract String[] getTableTips();

    public abstract boolean hasElements();

    public abstract int getAttributesColumnIndex();

    public abstract int getNumberOfDecoratorColumns();

    public abstract ArrayList<String> getAttributesColumnsHeaders();

    protected abstract void setColumnRowSorting();

    protected abstract List<JComponent> getExtraAddOptions();

    protected abstract JMenuItem getAddOption();

    protected abstract List<JComponent> getForcedOptions(ElementSelection selection);

    protected abstract List<JComponent> getExtraOptions(ElementSelection selection);

    protected abstract JPopupMenu getPopup(ElementSelection selection);

    protected abstract void showInCanvas(ElementSelection selection);

    protected abstract boolean hasAttributes();

    static class MenuItem_RemovedFiltered extends JMenuItem
    {
        MenuItem_RemovedFiltered(GUINetworkDesign callback, NetworkElementType networkElementType)
        {
            final NetPlan netPlan = callback.getDesign();

            this.setText("Remove all filtered out " + networkElementType + "s");
            this.addActionListener(e1 ->
            {
                try
                {
                    final ITableRowFilter tableRowFilter = callback.getVisualizationState().getTableRowFilter();
                    if (tableRowFilter != null)
                    {
                        switch (networkElementType)
                        {
                            case NODE:
                                final List<Node> visibleNodes = tableRowFilter.getVisibleNodes(netPlan.getNetworkLayerDefault());
                                for (Node node : new ArrayList<>(netPlan.getNodes()))
                                    if (!visibleNodes.contains(node)) node.remove();
                                break;
                            case LINK:
                                final List<Link> visibleLinks = tableRowFilter.getVisibleLinks(netPlan.getNetworkLayerDefault());
                                for (Link link : new ArrayList<>(netPlan.getLinks()))
                                    if (!visibleLinks.contains(link)) link.remove();
                                break;
                            case DEMAND:
                                final List<Demand> visibleDemands = tableRowFilter.getVisibleDemands(netPlan.getNetworkLayerDefault());
                                for (Demand demand : new ArrayList<>(netPlan.getDemands()))
                                    if (!visibleDemands.contains(demand)) demand.remove();
                                break;
                            case MULTICAST_DEMAND:
                                final List<MulticastDemand> visibleMulticastDemands = tableRowFilter.getVisibleMulticastDemands(netPlan.getNetworkLayerDefault());
                                for (MulticastDemand multicastDemand : new ArrayList<>(netPlan.getMulticastDemands()))
                                    if (!visibleMulticastDemands.contains(multicastDemand))
                                        multicastDemand.remove();
                                break;
                            case ROUTE:
                                final List<Route> visibleRoutes = tableRowFilter.getVisibleRoutes(netPlan.getNetworkLayerDefault());
                                for (Route route : new ArrayList<>(netPlan.getRoutes()))
                                    if (!visibleRoutes.contains(route)) route.remove();
                                break;
                            case MULTICAST_TREE:
                                final List<MulticastTree> visibleMulticastTrees = tableRowFilter.getVisibleMulticastTrees(netPlan.getNetworkLayerDefault());
                                for (MulticastTree tree : new ArrayList<>(netPlan.getMulticastTrees()))
                                    if (!visibleMulticastTrees.contains(tree)) tree.remove();
                                break;
                            case RESOURCE:
                                final List<Resource> visibleResources = tableRowFilter.getVisibleResources(netPlan.getNetworkLayerDefault());
                                for (Resource resource : new ArrayList<>(netPlan.getResources()))
                                    if (!visibleResources.contains(resource)) resource.remove();
                                break;
                            case SRG:
                                final List<SharedRiskGroup> visibleSRGs = tableRowFilter.getVisibleSRGs(netPlan.getNetworkLayerDefault());
                                for (SharedRiskGroup sharedRiskGroup : new ArrayList<>(netPlan.getSRGs()))
                                    if (!visibleSRGs.contains(sharedRiskGroup)) sharedRiskGroup.remove();
                                break;
                            default:
                                // TODO: Error message?
                                return;
                        }
                    }
                    callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                    callback.updateVisualizationAfterChanges(Sets.newHashSet(networkElementType));
                    callback.addNetPlanChange();
                } catch (Throwable ex)
                {
                    ex.printStackTrace();
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to complete this action");
                }
            });
        }
    }

    static class MenuItem_HideFiltered extends JMenuItem
    {
        MenuItem_HideFiltered(GUINetworkDesign callback, NetworkElementType networkElementType)
        {
            final NetPlan netPlan = callback.getDesign();
            this.setText("Hide all filtered out " + networkElementType + "s");
            this.addActionListener(e1 ->
            {
                final ITableRowFilter tableRowFilter = callback.getVisualizationState().getTableRowFilter();
                if (tableRowFilter != null)
                {
                    switch (networkElementType)
                    {
                        case NODE:
                            final List<Node> visibleNodes = tableRowFilter.getVisibleNodes(netPlan.getNetworkLayerDefault());
                            for (Node node : netPlan.getNodes())
                                if (!visibleNodes.contains(node))
                                    callback.getVisualizationState().hideOnCanvas(node);
                            break;
                        case LINK:
                            final List<Link> visibleLinks = tableRowFilter.getVisibleLinks(netPlan.getNetworkLayerDefault());
                            for (Link link : netPlan.getLinks())
                                if (!visibleLinks.contains(link))
                                    callback.getVisualizationState().hideOnCanvas(link);
                            break;
                        default:
                            return;
                    }
                }

                callback.updateVisualizationAfterChanges(Sets.newHashSet(networkElementType));
                callback.addNetPlanChange();
            });
        }
    }
}
