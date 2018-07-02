package com.net2plan.gui.utils;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;



public class CellRenderers
{
    public static final Color bgColorLastRow = new Color(200, 200, 200);
    public static final Color fgColorLastRow = new Color(0, 0, 0);
    public static final Color bgColorNonEditable = new Color(235, 235, 235);

    /**
     * Renderer for cells containing boolean values.
     *
     * @since 0.2.0
     */
    public static class CheckBoxRenderer extends JCheckBox implements TableCellRenderer
    {
        private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
        private static final NonEditableCellRenderer nonEditableCellRender = new NonEditableCellRenderer();

        public CheckBoxRenderer()
        {
            super();
            this.setHorizontalAlignment(JLabel.CENTER);
            this.setBorderPainted(false);
            this.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            if (value == null) return this;
            if (value instanceof LastRowAggregatedValue)
            {
                return nonEditableCellRender.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }

            this.setSelected((boolean) value);

            if (isSelected)
                this.setBackground(table.getSelectionBackground());
            else
                this.setBackground(table.isCellEditable(row, column) ? (Color) UIManager.get("CheckBox.interiorBackground") : bgColorNonEditable);

            this.setBorder(hasFocus ? UIManager.getBorder("Table.focusCellHighlightBorder") : noFocusBorder);

            return this;
        }
    }

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
        protected final static Color bgColorNonEditable = new Color(235, 235, 235);


        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if ((value != null) && (value instanceof LastRowAggregatedValue))
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
        private final InfNumberFormat nf;

        /**
         * Default constructor.
         *
         * @since 0.2.0
         */
        public NumberCellRenderer()
        {
            nf = new InfNumberFormat("##.##");
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
                final Number number = (Number) value;

                final boolean isDecimalNumber = number.doubleValue() % 1 != 0;

                if (isDecimalNumber)
                {
                    final double doubleValue = number.doubleValue();
                    value = nf.format(Math.abs(doubleValue) < 1e-10 ? 0.0 : doubleValue);
                } else
                {
                    final int intValue = number.intValue();
                    value = nf.format(intValue);
                }

                setHorizontalAlignment(SwingConstants.RIGHT);
            }

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if ((value != null) && (value instanceof LastRowAggregatedValue))
            {
                c.setForeground(fgColorLastRow);
                c.setBackground(bgColorLastRow);
                return c;
            }
            return c;
        }
    }

    public static class LastRowAggregatingInfoCellRenderer extends DefaultTableCellRenderer
    {
        private final static Color bgColorNonEditable = new Color(230, 230, 230);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setForeground(UIManager.getColor("Table.foreground"));
            c.setBackground(bgColorNonEditable);
            return c;
        }
    }
    
    
    
}
