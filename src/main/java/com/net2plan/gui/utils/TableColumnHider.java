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


package com.net2plan.gui.utils;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.util.*;

/**
 * Class able to show/hide columns from a table.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @see http://stackoverflow.com/questions/6796673
 * @since 0.3.0
 */
public class TableColumnHider {
    private final TableColumnModel tcm;
    private final Set<String> hiddenColumns;
    private final Map<String, TableColumn> allColumns;

    /**
     * Default constructor.
     *
     * @param table Input table
     * @since 0.3.0
     */
    public TableColumnHider(JTable table) {
        tcm = table.getColumnModel();
        allColumns = new LinkedHashMap<String, TableColumn>();
        int numColumns = tcm.getColumnCount();
        for (int i = 1; i < numColumns; i++) {
            TableColumn tableColumn = tcm.getColumn(i);
            String columnIdentifier = tcm.getColumn(i).getIdentifier().toString();
            allColumns.put(columnIdentifier, tableColumn);
        }

        hiddenColumns = new LinkedHashSet<String>();
    }

    /**
     * Hide a column.
     *
     * @param columnName Name of the column to hide.
     * @since 0.3.0
     */
    public void hide(String columnName) {
        int index = tcm.getColumnIndex(columnName);
        TableColumn column = tcm.getColumn(index);
        hiddenColumns.add(columnName);
        tcm.removeColumn(column);
    }

    /**
     * Hide all columns but one.
     *
     * @param columnName Name of the only column to be shown
     * @since 0.3.0
     */
    public void maintainOnly(String columnName) {
        List<String> columnNames = new LinkedList<String>();
        int numColumns = tcm.getColumnCount();
        for (int i = 0; i < numColumns; i++) {
            String columnIdentifier = tcm.getColumn(i).getIdentifier().toString();
            if (!columnIdentifier.equals(columnName)) columnNames.add(columnIdentifier);
        }

        for (String columnName1 : columnNames)
            hide(columnName1);
    }

    /**
     * Show a column.
     *
     * @param columnName Name of the column to be shown
     * @since 0.3.0
     */
    public void show(String columnName) {
        hiddenColumns.remove(columnName);

        for (TableColumn tc : allColumns.values()) tcm.removeColumn(tc);
        for (TableColumn tc : allColumns.values()) {
            String columnIdentifier = tc.getIdentifier().toString();
            if (!columnIdentifier.equals(columnName)) tcm.addColumn(tc);
        }
    }

    /**
     * Show all columns.
     *
     * @since 0.3.0
     */
    public void showAll() {
        hiddenColumns.clear();

        for (TableColumn tc : allColumns.values()) tcm.removeColumn(tc);
        for (TableColumn tc : allColumns.values()) tcm.addColumn(tc);
    }
}
