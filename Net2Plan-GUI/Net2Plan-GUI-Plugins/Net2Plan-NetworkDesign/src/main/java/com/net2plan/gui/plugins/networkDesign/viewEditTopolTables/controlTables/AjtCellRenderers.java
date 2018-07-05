package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

import java.awt.Color;
import java.awt.Component;
import java.util.Collection;
import java.util.function.Function;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.utils.LastRowAggregatedValue;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.utils.Pair;

public class AjtCellRenderers
{
    public static final Color bgColorLastRow = new Color(200, 200, 200);
    public static final Color fgColorLastRow = new Color(0, 0, 0);
    public static final Color bgColorNonEditable = new Color(235, 235, 235);
    public static final Color bgColorEditable = new Color(250, 250, 250);

    
    private static Color computeBgColor (AdvancedJTable_abstractElement table, Object value, boolean isSelected, boolean hasFocus, int rowInModel, int columnInModel , AjtColumnInfo colInfo)
    {
        if (value instanceof LastRowAggregatedValue) return bgColorLastRow;
        if (isSelected) return table.getSelectionBackground();
        final Function<Object,Color> functionCommonColorRow = table.getColoringFunctionForTheFullRowIfNotSelected();
        final Function<Object,Color> functionThisCellParticularColor = colInfo.getGetSpecialBgColorIfNotSelected();
        if (functionCommonColorRow != null || functionThisCellParticularColor != null)
        {
        	final Object rowDefiningValue = table.getElementAtModelRowIndex (rowInModel);
        	if (functionCommonColorRow != null) 
        	{
        		final Color c = functionCommonColorRow.apply(rowDefiningValue);
        		if (c != null) return c;
        	}
        	if (functionThisCellParticularColor != null) 
        	{
        		final Color c = functionThisCellParticularColor.apply(rowDefiningValue);
        		if (c != null) return c;
        	}
        }
        if (table.getModel().isCellEditable(rowInModel, columnInModel))
        {
            if (colInfo.getValueShownIfNotAggregation().equals(Boolean.class)) 
                return (Color) UIManager.get("CheckBox.interiorBackground");
            else 
                return bgColorEditable;
        }
        return bgColorNonEditable;
    }
    

    
    /**
     * Renderer for cells containing boolean values.
     *
     * @since 0.2.0
     */
    public static class CheckBoxRenderer extends JCheckBox implements TableCellRenderer
    {
        private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
        private static final LastRowCellRenderer lastRowCellRender = new LastRowCellRenderer();
        private final AjtColumnInfo columnInfo;
        
        public CheckBoxRenderer(AjtColumnInfo columnInfo)
        {
            super();
            this.columnInfo = columnInfo;
            this.setHorizontalAlignment(JLabel.CENTER);
            this.setBorderPainted(false);
            this.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowInView, int columnInView)
        {
            final int rowInModel = table.convertRowIndexToModel(rowInView);
            final int columnInModel = table.convertColumnIndexToModel(columnInView);
            
            if (value == null) return this;
            if (value instanceof LastRowAggregatedValue)
                return lastRowCellRender.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowInView, columnInView);
            this.setSelected((boolean) value);
        	final AdvancedJTable_abstractElement absTable = columnInfo.getAjTable();
            this.setBackground(computeBgColor(absTable, value, isSelected, hasFocus, rowInModel, columnInModel , columnInfo));
            this.setBorder(hasFocus ? UIManager.getBorder("Table.focusCellHighlightBorder") : noFocusBorder);
            return this;
        }
    }
    /**
     * Renderer for cells containing boolean values.
     *
     * @since 0.2.0
     */
    public static class JLabelRenderer extends DefaultTableCellRenderer implements TableCellRenderer
    {
        private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
        private static final LastRowCellRenderer lastRowCellRender = new LastRowCellRenderer();
        private final AjtColumnInfo columnInfo;
        private final GUINetworkDesign callback;
        
        public JLabelRenderer(GUINetworkDesign callback , AjtColumnInfo columnInfo)
        {
            super();
            this.columnInfo = columnInfo;
            
            this.setHorizontalAlignment(JLabel.CENTER);
            this.setOpaque(true);
            this.callback = callback;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowInView, int columnInView)
        {
            final int rowInModel = table.convertRowIndexToModel(rowInView);
            final int columnInModel = table.convertColumnIndexToModel(columnInView);
            final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowInView, columnInView);
            
            if (value == null) ((JLabel) c).setText("undefined");

            if (value instanceof LastRowAggregatedValue)
                return lastRowCellRender.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowInView, columnInView);
            
        	final AdvancedJTable_abstractElement absTable = columnInfo.getAjTable();
            this.setBackground(computeBgColor(absTable, value, isSelected, hasFocus, rowInModel, columnInModel , columnInfo));
            
            if (value instanceof Pair) value = ((Pair<Number, ?>)value).getFirst().doubleValue();
            
            if (value instanceof NetworkElement)
            {
            	if (isSelected || this.getBackground() == Color.RED) c.setForeground(Color.WHITE);
            	else c.setForeground(Color.BLUE);
                ((JLabel) c).setText(((NetworkElement)value).getName());
            }
            else if (value instanceof Double)
            {
                final Double valDouble = (Double)value;
                final Double valDoubleToPrint;
                if (valDouble == Double.MAX_VALUE) valDoubleToPrint = Double.POSITIVE_INFINITY;
                else if (valDouble == -Double.MAX_VALUE) valDoubleToPrint = Double.NEGATIVE_INFINITY;
                else valDoubleToPrint = valDouble;
                ((JLabel) c).setText(columnInfo.getDecimalFormatForDouble().format(valDoubleToPrint));
                c.setForeground(Color.BLACK);
            }
            else if (value instanceof Collection)
            {
				final int listSize = ((Collection) value).size();
								
				if (listSize == 0)
				{
					((JLabel) c).setText(listSize + "");
					c.setForeground(Color.BLACK);
				}
				else
				{
					final Object firstElement = ((Collection) value).iterator().next();
					if (firstElement instanceof NetworkElement)
					{
						((JLabel) c).setText(listSize + "");
						if (isSelected || this.getBackground() == Color.RED) c.setForeground(Color.WHITE);
						else c.setForeground(Color.BLUE);

					}
					else c.setForeground(Color.BLACK);
				}
			}
            else
            {
                c.setForeground(Color.BLACK);
            }
            return c;
        }
    }

    
    /**
     * Renderer that shades in gray non-editable cells.
     *
     * @since 0.2.0
     */
    public static class LastRowCellRenderer extends DefaultTableCellRenderer
    {
        private LastRowCellRenderer ()
        {
            this.setHorizontalAlignment(JLabel.CENTER);
            this.setOpaque(true);
        }

        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowInView, int columnInView)
        {
            final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowInView, columnInView);
            c.setForeground(fgColorLastRow);
            c.setBackground(bgColorLastRow);
            return c;
//            if ((value != null) && (value instanceof LastRowAggregatedValue))
//            {
//                c.setForeground(fgColorLastRow);
//                c.setBackground(bgColorLastRow);
//                ((JLabel) c).setText(value.toString());
//                return c;
//            }
//
//            /*
//             * When you call setForeground() or setBackground(), the selected colors stay
//             * in effect until subsequent calls are made to those methods. Without care,
//             * you might inadvertently color cells that should not be colored. To prevent
//             * that from happening, you should always include calls to the aforementioned
//             * methods that set a cell's colors to their look and feel–specific defaults,
//             * if that cell is not being colored
//             */
//            c.setForeground(UIManager.getColor(isSelected ? "Table.selectionForeground" : "Table.foreground"));
//            c.setBackground(UIManager.getColor(isSelected ? "Table.selectionBackground" : "Table.background"));
//
//            if (!isSelected)
//            {
//                if (rowInView != -1 && columnInView != -1)
//                {
//                    final int rowInModel = table.convertRowIndexToModel(rowInView);
//                    final int columnInModel = table.convertColumnIndexToModel(columnInView);
//
//                    if (!table.getModel().isCellEditable(rowInModel, columnInModel))
//                    {
//                        c.setBackground(isSelected ? UIManager.getColor("Table.selectionBackground") : bgColorNonEditable);
//                    }
//                }
//            }
//
//            return c;
        }
    }
}
