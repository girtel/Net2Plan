

package com.net2plan.gui.plugins.networkDesign.utils;

import java.awt.Color;
import java.awt.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.utils.DefaultTableCellHeaderRenderer;
import com.net2plan.gui.utils.LastRowAggregatedValue;
import com.net2plan.internal.Constants.NetworkElementType;

/**
 * Set of several cell renderers used into the GUI.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class CellRenderers extends com.net2plan.gui.utils.CellRenderers
{
    /**
     * Renderer that is able to determine if the row corresponds to the current state
     * or the initial state.
     *
     * @since 0.3.0
     */
    public abstract static class CurrentPlannedCellRenderer implements TableCellRenderer
    {
        private final TableCellRenderer cellRenderer;

        /**
         * Type of element (i.e. layers, nodes, links, and so on).
         *
         * @since 0.3.0
         */
        protected final NetworkElementType networkElementType;

        /**
         * Default constructor.
         *
         * @param cellRenderer       Reference to the original cell renderer
         * @param networkElementType Type of element (i.e. layers, nodes, links, and so on)
         * @since 0.3.0
         */
        public CurrentPlannedCellRenderer(TableCellRenderer cellRenderer, NetworkElementType networkElementType)
        {
            this.cellRenderer = cellRenderer;
            this.networkElementType = networkElementType;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            Component c = cellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value != null && value instanceof LastRowAggregatedValue)
            {
                c.setBackground(bgColorLastRow);
                c.setForeground(fgColorLastRow);
                return c;
            }

            if (!isSelected && row != -1 && column != -1)
            {
                row = table.convertRowIndexToModel(row);
                column = table.convertColumnIndexToModel(column);

                if (table.getModel().getValueAt(row, 0) == null)
                {
                    setPlannedState(c, table, row, column, isSelected);
                } else
                {
                    Object itemId = table.getModel().getValueAt(row, 0);
                    setCurrentState(c, table, itemId, row, column, isSelected);
                }
            }

            return c;
        }

        /**
         * Sets the cell properties for the current state.
         *
         * @param c                Component
         * @param table            Table
         * @param itemId           Item id (node identifier...)
         * @param rowIndexModel    Row index in model order
         * @param columnIndexModel Column index in model order
         * @param isSelected       Indicates whether or not the cell is selected
         * @since 0.2.0
         */
        public abstract void setCurrentState(Component c, JTable table, Object itemId, int rowIndexModel, int columnIndexModel, boolean isSelected);

        /**
         * Sets the cell properties for the planned state.
         *
         * @param c                Component
         * @param table            Table
         * @param rowIndexModel    Row index in model order
         * @param columnIndexModel Column index in model order
         * @param isSelected       Indicates whether or not the cell is selected
         * @since 0.2.0
         */
        public abstract void setPlannedState(Component c, JTable table, int rowIndexModel, int columnIndexModel, boolean isSelected);
    }

    public static class FixedTableHeaderRenderer extends DefaultTableCellHeaderRenderer
    {
        /**
         * Background color for Fixed Table Columns.
         *
         * @since 0.2.0
         */
        protected final static Color bgColorFixedTable = new Color(235, 235, 235);


        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value != null && value instanceof LastRowAggregatedValue)
            {
                c.setBackground(bgColorLastRow);
                c.setForeground(fgColorLastRow);
            } else
                c.setBackground(bgColorFixedTable);
            return c;
        }
    }
    
    public static class MainTableHeaderRenderer extends DefaultTableCellHeaderRenderer
    {
    	/**
    	 * Background color for Main Table Columns.
    	 *
    	 * @since 0.2.0
    	 */
    	protected final static Color bgColorMainTable = new Color(255, 255, 255);
    	protected final static Color bgBorderColorMainTable = new Color(230, 230, 230);
    	
    	
    	@Override
    	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    	{
    		Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    		if (value != null && value instanceof LastRowAggregatedValue)
    		{
    			c.setBackground(bgColorLastRow);
    			c.setForeground(fgColorLastRow);
    		} else
    			c.setBackground(bgColorMainTable);
    		
    		
    		((JComponent) c).setBorder(BorderFactory.createLineBorder(bgBorderColorMainTable));
    		return c;
    	}
    }

}
