package com.net2plan.gui.utils;

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


import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 */
public class DefaultTableCellHeaderRenderer extends DefaultTableCellRenderer
        implements UIResource {
    private boolean horizontalTextPositionSet;

    public DefaultTableCellHeaderRenderer() {
        setHorizontalAlignment(JLabel.CENTER);
    }

    public void setHorizontalTextPosition(int textPosition) {
        horizontalTextPositionSet = true;
        super.setHorizontalTextPosition(textPosition);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        Icon sortIcon = null;

        boolean isPaintingForPrint = false;

        if (table != null) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                Color fgColor = null;
                Color bgColor = null;
                if (hasFocus) {
                    fgColor = UIManager.getColor("TableHeader.focusCellForeground");
                    bgColor = UIManager.getColor("TableHeader.focusCellBackground");
                }
                if (fgColor == null) {
                    fgColor = header.getForeground();
                }
                if (bgColor == null) {
                    bgColor = header.getBackground();
                }
                setForeground(fgColor);
                setBackground(bgColor);

                setFont(header.getFont());

                isPaintingForPrint = header.isPaintingForPrint();
            }

            if (!isPaintingForPrint && table.getRowSorter() != null) {
                if (!horizontalTextPositionSet) {
                    // There is a row sorter, and the developer hasn't
                    // set a text position, change to leading.
                    setHorizontalTextPosition(JLabel.LEADING);
                }
                SortOrder sortOrder = getColumnSortOrder(table, column);
                if (sortOrder != null) {
                    switch(sortOrder) {
                        case ASCENDING:
                            sortIcon = UIManager.getIcon(
                                    "Table.ascendingSortIcon");
                            break;
                        case DESCENDING:
                            sortIcon = UIManager.getIcon(
                                    "Table.descendingSortIcon");
                            break;
                        case UNSORTED:
                            sortIcon = UIManager.getIcon(
                                    "Table.naturalSortIcon");
                            break;
                    }
                }
            }
        }

        setText(value == null ? "" : value.toString());
        setIcon(sortIcon);

        Border border = null;
        if (hasFocus) {
            border = UIManager.getBorder("TableHeader.focusCellBorder");
        }
        if (border == null) {
            border = UIManager.getBorder("TableHeader.cellBorder");
        }
        setBorder(border);

        return this;
    }

    public static SortOrder getColumnSortOrder(JTable table, int column) {
        SortOrder rv = null;
        if (table.getRowSorter() == null) {
            return rv;
        }
        java.util.List<? extends RowSorter.SortKey> sortKeys =
                table.getRowSorter().getSortKeys();
        if (sortKeys.size() > 0 && sortKeys.get(0).getColumn() ==
                table.convertColumnIndexToModel(column)) {
            rv = sortKeys.get(0).getSortOrder();
        }
        return rv;
    }
}
