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


package com.net2plan.gui.plugins.trafficDesign;


import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.NumberFormat;

/**
 * Set of several cell renderers used into the GUI.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
@SuppressWarnings("unchecked")
public class CellRenderers
{
    /**
     * Renderer for cells containing boolean values.
     *1
     * @since 0.2.0
     */
    public static final Color bgColorLastRow = new Color(200, 200, 200);
    public static final Color fgColorLastRow = new Color(0, 0, 0);

    /**
     * Renderer that shades in gray non-editable cells.
     *
     * @since 0.2.0
     */
    public static class NonEditableCellRenderer extends DefaultTableCellRenderer
    {
        /**
         * Background color for non-editable cell.
         *
         * @since 0.2.0
         */
        protected final static Color bgColorNonEditable = new Color(245, 245, 245);


        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            // TODO: if ((value != null) && (value instanceof AdvancedJTable_NetworkElement.LastRowAggregatedValue))
            if ((value != null))
            {
                c.setForeground(fgColorLastRow);
                c.setBackground(bgColorLastRow);
                return c;
            }

			/*
             * When you call setForeground() or setBackground(), the selected colors stay
			 * in effect until subsequent calls are made to those methods. Without care,
			 * you might inadvertently color cells that should not be colored. To prevent
			 * that from happening, you should always include calls to the aforementioned
			 * methods that set a cell's colors to their look and feel–specific defaults,
			 * if that cell is not being colored
			 */
            c.setForeground(UIManager.getColor(isSelected ? "Table.selectionForeground" : "Table.foreground"));
            c.setBackground(UIManager.getColor(isSelected ? "Table.selectionBackground" : "Table.background"));

            if (!isSelected)
            {
                if (row != -1 && column != -1)
                {
                    row = table.convertRowIndexToModel(row);
                    column = table.convertColumnIndexToModel(column);

                    if (!table.getModel().isCellEditable(row, column))
                    {
                        c.setBackground(isSelected ? UIManager.getColor("Table.selectionBackground") : bgColorNonEditable);
                    }
                }
            }

            return c;
        }
    }

    /**
     * Gives the current number format to cells, and fixes the
     * {@code setForeground()}/{@code setBackground()} issue.
     *
     * @since 0.2.0
     */
    public static class NumberCellRenderer extends NonEditableCellRenderer
    {
        private final NumberFormat nf;

        /**
         * Default constructor.
         *
         * @since 0.2.0
         */
        public NumberCellRenderer()
        {
            nf = NumberFormat.getInstance();
            nf.setGroupingUsed(false);
        }

        /**
         * Constructor that allows configuring the maximum number of decimal digits.
         *
         * @param maxNumDecimals Maximum number of decimal digits
         * @since 0.3.1
         */
        public NumberCellRenderer(int maxNumDecimals)
        {
            this();
            nf.setMaximumFractionDigits(maxNumDecimals);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            if (value instanceof Number)
            {
                double doubleValue = ((Number) value).doubleValue();
                if (doubleValue == Double.MAX_VALUE) doubleValue = Double.POSITIVE_INFINITY;
                else if (doubleValue == -Double.MAX_VALUE) doubleValue = Double.NEGATIVE_INFINITY;
                value = nf.format(Math.abs(doubleValue) < 1e-10 ? 0.0 : doubleValue);
                setHorizontalAlignment(SwingConstants.RIGHT);
            }

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            // TODO: if ((value != null) && (value instanceof AdvancedJTable_NetworkElement.LastRowAggregatedValue))
            if ((value != null))
            {
                c.setForeground(fgColorLastRow);
                c.setBackground(bgColorLastRow);
                return c;
            }
            return c;
        }
    }
}
