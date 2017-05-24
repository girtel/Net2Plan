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

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Allows to set tooltip texts to table column headers.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public class ColumnHeaderToolTips extends MouseMotionAdapter {
    private TableColumn curCol;
    private final Map<TableColumn, String> tips = new LinkedHashMap<TableColumn, String>();

    @Override
    public void mouseMoved(MouseEvent evt) {
        JTableHeader header = (JTableHeader) evt.getSource();
        JTable table = header.getTable();
        TableColumnModel colModel = table.getColumnModel();
        int vColIndex = colModel.getColumnIndexAtX(evt.getX());

        TableColumn col = null;
        if (vColIndex >= 0) {
            col = colModel.getColumn(vColIndex);
        }

        if (col != curCol) {
            header.setToolTipText((String) tips.get(col));
            curCol = col;
        }
    }

    /**
     * Sets the tooltip of a given column header.
     *
     * @param col     Table column
     * @param tooltip Text of the column header (null or empty means no tooltip)
     * @since 0.2.3
     */
    public void setToolTip(TableColumn col, String tooltip) {
        if (tooltip == null || tooltip.isEmpty()) tips.remove(col);
        else tips.put(col, tooltip);
    }
}
