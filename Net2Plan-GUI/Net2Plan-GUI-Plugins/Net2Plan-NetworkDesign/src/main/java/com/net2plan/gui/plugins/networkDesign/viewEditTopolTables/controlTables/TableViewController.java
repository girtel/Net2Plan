package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_layer;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableStateFiles.TableState;
import com.net2plan.internal.Constants;
import com.net2plan.internal.ErrorHandling;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.xml.stream.XMLStreamException;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Cesar on 05/05/2017.
 */
public class TableViewController {


    AdvancedJTable_networkElement table;

    protected final JPopupMenu fixedTableMenu, mainTableMenu;
    protected final JMenu showMenu;
    protected final JMenuItem showAllItem, hideAllItem, resetItem, saveStateItem, loadStateItem;
    protected final ArrayList<TableColumn> hiddenColumns, shownColumns, removedColumns;
    protected ArrayList<String> hiddenColumnsNames, hiddenColumnsAux;
    protected final Map<String, Integer> indexForEachColumn, indexForEachHiddenColumn;
    protected final Map<Integer, String> mapToSaveState;
    protected final Map<String, Integer> mapToSaveWidths;
    protected JCheckBoxMenuItem lockColumn, unfixCheckBox, attributesItem, hideColumn;
    protected ArrayList<JMenuItem> hiddenHeaderItems;
    protected JTable mainTable, fixedTable;
    private boolean expandAttributes = false;

    public TableViewController(AdvancedJTable_networkElement table)
    {
        this.table = table;
        mainTable = table.getMainTable();
        fixedTable = table.getFixedTable();
        hiddenColumnsNames = new ArrayList<>();
        hiddenColumns = new ArrayList<>();
        shownColumns = new ArrayList<>();
        removedColumns = new ArrayList<>();
        indexForEachColumn = new HashMap<>();
        indexForEachHiddenColumn = new HashMap<>();
        mapToSaveState = new HashMap<>();
        mapToSaveWidths = new HashMap<>();

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

        if (table.hasAttributes())
        {
            this.table.getModel().addTableModelListener(e ->
            {
                int changedColumn = e.getColumn();
                int selectedRow = mainTable.getSelectedRow();
                Object value = null;
                if (changedColumn > table.getAttributesColumnIndex())
                {
                    for (String title : table.getAttributesColumnsHeaders())
                    {
                        if (table.getModel().getColumnName(changedColumn).equals("Att: " + title))
                        {
                            value = table.getModel().getValueAt(selectedRow, changedColumn);
                            if (value != null)
                                table.callback.getDesign().getNetworkElement((Long) table.getModel().getValueAt(selectedRow, 0)).setAttribute(title, (String) value);
                        }
                    }
                    table.callback.updateVisualizationJustTables();
                }
            });
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
                        final int col = table.getTableHeader().columnAtPoint(ev.getPoint());
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

    }

    protected void buildAttributeControls()
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

    /**
     * Re-configures the menu to show hidden columns
     */
    protected void updateShowMenu()
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
    protected void showAllColumns()
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

    protected void hideAllColumns()
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
    protected void showColumn(String columnName, int columnIndex, boolean move)
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
    protected void hideColumn(int columnIndex)
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
    protected void fromMainTableToFixedTable(int columnIndex)
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

    protected void fromFixedTableToMainTable(int columnIndex)
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

    protected void updateTables()
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
    protected void saveColumnsPositionsAndWidths()
    {
        mapToSaveState.clear();
        mapToSaveWidths.clear();
        TableColumn currentColumn = null;
        String currentColumnName = "";
        for (int i = 0; i < mainTable.getColumnModel().getColumnCount(); i++)
        {
            currentColumn = mainTable.getColumnModel().getColumn(i);
            currentColumnName = currentColumn.getHeaderValue().toString();
            mapToSaveState.put(i, currentColumnName);
            mapToSaveWidths.put(currentColumnName, currentColumn.getWidth());
            if(table.getNetworkElementType() == Constants.NetworkElementType.NODE)
            System.out.println("Columna "+currentColumnName+" tiene anchura "+currentColumn.getWidth());
        }
    }

    /**
     * Restore for each column its previous position
     *
     * @param
     */
    protected void restoreColumnsPositionsAndWidths()
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
        String currentColumnName = "";
        int columnWidth = 0;
        for (int j = 0; j < mapToSaveState.size(); j++)
        {
            currentColumnName = mapToSaveState.get(j);
            showColumn(currentColumnName, j, false);
        }

        for (Map.Entry<String, Integer> entry : mapToSaveWidths.entrySet())
        {
            currentColumnName = entry.getKey();
            columnWidth = entry.getValue();
            setWidth(currentColumnName, columnWidth);
        }

        checkNewIndexes();
    }

    /**
     * Loads a table state from a external file
     *
     * @param
     */
    protected void loadTableState()
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
    protected void saveTableState() throws XMLStreamException
    {
//        Map<NetworkElementType, AdvancedJTable_networkElement> currentTables = callback.getTables();
//        TableStateController.saveTableState(currentTables);
    }

    /**
     * Update columns positions from a table state
     *
     * @param state TableState where the table configuration is saved
     */
    protected void updateTableFromTableState(TableState state)
    {
        resetColumnsPositions();
        TableColumn fixedTableCol = null;
        while (fixedTable.getColumnModel().getColumnCount() > 0)
        {
            fixedTableCol = fixedTable.getColumnModel().getColumn(0);
            fixedTable.getColumnModel().removeColumn(fixedTableCol);
        }
        table.createDefaultColumnsFromModel();
        ArrayList<String> fixedTableColumns = state.getFixedTableColumns();
        ArrayList<String> mainTableColumns = state.getMainTableColumns();
        HashMap<String, Integer> hiddenColumnsMap = state.getHiddenTableColumns();
        boolean areAttributesExpanded = state.getExpandAttributes();

        String[] currentHeaders = table.getCurrentTableHeaders();
        ArrayList<String> currentHeadersList = new ArrayList<>();

        for (int i = 0; i < currentHeaders.length; i++)
        {
            currentHeadersList.add(currentHeaders[i]);
        }

        if (areAttributesExpanded && table.getAttributesColumnsHeaders().size() > 0)
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
    protected void resetColumnsPositions()
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
        for (int i = 0; i < table.getNumberOfDecoratorColumns(); i++)
        {
            tc = mainTable.getColumnModel().getColumn(0);
            mainTable.getColumnModel().removeColumn(tc);
            fixedTable.getColumnModel().addColumn(tc);
        }
        for (String s : table.getAttributesColumnsHeaders())
        {
            removeNewColumn("Att: " + s);
        }
        setAttributesCellExpanded(false);
        attributesItem.setSelected(false);
        updateTables();
        checkNewIndexes();
    }

    /**
     * When a column is moved into mainTable,
     * we have to know which are the new indexes and update indexForEachColumn
     */
    protected void checkNewIndexes()
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
    protected void removeNewColumn(String columnToRemoveName)
    {
        TableColumn columnToRemove = null;
        for (int j = 0; j < table.getColumnModel().getColumnCount(); j++)
        {
            if (columnToRemoveName.equals(table.getColumnModel().getColumn(j).getHeaderValue().toString()))
            {
                columnToRemove = table.getColumnModel().getColumn(j);
                mainTable.getColumnModel().removeColumn(table.getColumnModel().getColumn(j));
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
    protected void recoverRemovedColumn(String columnToRecoverName)
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
    protected void attributesInDifferentColumns()
    {
        saveColumnsPositionsAndWidths();
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
            if (table.getAttributesColumnsHeaders().size() > 0)
            {
                table.callback.updateVisualizationJustTables();
                table.createDefaultColumnsFromModel();
                table.setTips();
                removedColumns.clear();
                removeNewColumn("Attributes");
                updateTables();
                setAttributesCellExpanded(true);
                restoreColumnsPositionsAndWidths();
                for (String att : table.getAttributesColumnsHeaders())
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

    protected void attributesInOneColumn()
    {
        saveColumnsPositionsAndWidths();
        int attributesCounter = 0;
        String columnToCheck = null;
        for (String att : table.getAttributesColumnsHeaders())
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
        if (!(attributesCounter == table.getAttributesColumnsHeaders().size()))
        {
            JOptionPane.showMessageDialog(null, "All attributes columns must be unlocked and visible to contract them in one column");
            attributesItem.setSelected(true);
        } else
        {

            if (table.getAttributesColumnsHeaders().size() > 0)
            {

                table.callback.updateVisualizationJustTables();
                table.createDefaultColumnsFromModel();
                table.setTips();
                removedColumns.clear();
                for (String att : table.getAttributesColumnsHeaders())
                {
                    removeNewColumn("Att: " + att);
                }
                updateTables();
                setAttributesCellExpanded(false);
                restoreColumnsPositionsAndWidths();
                showColumn("Attributes", 0, false);
                checkNewIndexes();
            } else
            {
                attributesItem.setSelected(true);
            }
        }
    }

    public void setWidth(String columnName, int columnWidth)
    {
        TableColumn tc = null;
        for(int i = 0; i < table.getColumnModel().getColumnCount(); i++)
        {
            tc = table.getColumnModel().getColumn(i);
            if(tc.getHeaderValue().toString().equals(columnName))
            {
                tc.setPreferredWidth(columnWidth);
                if(table.getNetworkElementType() == Constants.NetworkElementType.NODE)
                System.out.println("A la columna "+tc.getHeaderValue().toString()+ " se le ha puesto el valor de anchura "+columnWidth);
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

    public void setAttributesCellExpanded(boolean flag)
    {
        expandAttributes = flag;
    }

}
