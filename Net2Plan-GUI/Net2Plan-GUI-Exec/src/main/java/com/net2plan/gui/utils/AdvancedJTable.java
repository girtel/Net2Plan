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

import com.net2plan.utils.Pair;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

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
 * </p>
 * <p>Credits to Santhosh Kumar for his methods to solve partially visible cell
 * issues (<a href='http://www.jroller.com/santhosh/entry/partially_visible_tablecells'>Partially Visible TableCells</a>)</p>
 * </p>
 * <p>Credits to "Kah - The Developer" for his static method to set column widths
 * in proportion to each other (<a href='http://kahdev.wordpress.com/2011/10/30/java-specifying-the-column-widths-of-a-jtable-as-percentages/'>Specifying the column widths of a JTable as percentages</a>)
 * </p>
 * <p>Credits to Rob Camick for his 'select all' editing feature for {@code JTable}
 * (<a href='https://tips4java.wordpress.com/2008/10/20/table-select-all-editor/'>Table Select All Editor</a>)
 * </p>
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class AdvancedJTable extends JTable {
    private boolean disableSetAutoResizeMode;
    private final Map<Pair<Integer, Integer>, TableCellEditor> cellEditorMap;
    private final Map<Pair<Integer, Integer>, TableCellRenderer> cellRendererMap;
    private final Map<Pair<Integer, Integer>, String> tooltipMap;

    private boolean isSelectAllForMouseEvent = true;
    private boolean isSelectAllForActionEvent = true;
    private boolean isSelectAllForKeyEvent = true;



    /**
     * Default constructor.
     *
     * @since 0.2.0
     */
    public AdvancedJTable() {
        super();


        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        addKeyListener(new TableCursorNavigation());
        cellEditorMap = new LinkedHashMap<Pair<Integer, Integer>, TableCellEditor>();
        cellRendererMap = new LinkedHashMap<Pair<Integer, Integer>, TableCellRenderer>();
        tooltipMap = new LinkedHashMap<Pair<Integer, Integer>, String>();

        disableSetAutoResizeMode = true;
        this.getTableHeader().setReorderingAllowed(false);
    }

    /**
     * Constructor that allows to set the table model.
     *
     * @param model Table model
     * @since 0.2.0
     */
    public AdvancedJTable(TableModel model) {
        this();

        setModel(model);
        this.getTableHeader().setReorderingAllowed(false);
    }

    @Override
    public void setModel(TableModel model){
        super.setModel(model);
    }

    @Override
    public void doLayout() {
        TableColumn resizingColumn = tableHeader == null ? null : tableHeader.getResizingColumn();

        if (resizingColumn == null) {
            /* Viewport size changed. May need to increase columns widths */
            super.doLayout();
        } else {
            /* Specific column resized. Reset preferred widths */
            TableColumnModel tcm = getColumnModel();

            for (int i = 0; i < tcm.getColumnCount(); i++) {
                TableColumn tc = tcm.getColumn(i);
                tc.setPreferredWidth(tc.getWidth());
            }

            disableSetAutoResizeMode = false;

			/* Columns don't fill the viewport, invoke default layout */
            if (tcm.getTotalColumnWidth() < getParent().getWidth()) {
                setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                super.doLayout();
            }

            setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            disableSetAutoResizeMode = true;
        }
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        boolean result = super.editCellAt(row, column, e);

        if (isSelectAllForMouseEvent || isSelectAllForActionEvent || isSelectAllForKeyEvent)
            selectAll(e);

        return result;
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        if (cellEditorMap.containsKey(Pair.of(row, column))) return cellEditorMap.get(Pair.of(row, column));
        else return super.getCellEditor(row, column);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) 
    {
        if (cellRendererMap.containsKey(Pair.of(row, column)))
        	return cellRendererMap.get(Pair.of(row, column));
        else 
        	return super.getCellRenderer(row, column);
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getPreferredSize().height < getParent().getHeight();
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getPreferredSize().width < getParent().getWidth();
    }

    @Override
    public Point getToolTipLocation(MouseEvent event) {
        int row = rowAtPoint(event.getPoint());
        if (row == -1) return null;

        int col = columnAtPoint(event.getPoint());
        if (col == -1) return null;

        boolean hasTooltip = getToolTipText() == null ? getToolTipText(event) != null : true;
        return hasTooltip ? getCellRect(row, col, false).getLocation() : null;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        Point p = event.getPoint();

        int hitColumnIndex = columnAtPoint(p);
        int hitRowIndex = rowAtPoint(p);
        if (hitRowIndex == -1 || hitColumnIndex == -1) return null;

        TableCellRenderer renderer = getCellRenderer(hitRowIndex, hitColumnIndex);
        Component component = prepareRenderer(renderer, hitRowIndex, hitColumnIndex);

        String tip = tooltipMap.get(Pair.of(hitRowIndex, hitColumnIndex));
        if (tip == null && component instanceof JComponent) {
            Rectangle cellRect = getCellRect(hitRowIndex, hitColumnIndex, false);
            if (cellRect.width >= component.getPreferredSize().width) return null;

            p.translate(-cellRect.x, -cellRect.y);
            MouseEvent newEvent = new MouseEvent(component, event.getID(), event.getWhen(), event.getModifiers(), p.x, p.y, event.getClickCount(), event.isPopupTrigger());
            tip = ((JComponent) component).getToolTipText(newEvent);
        }

        if (tip == null) tip = getToolTipText();

        if (tip == null) {
            Object value = getValueAt(hitRowIndex, hitColumnIndex);

            if (value != null) {
                String stringValue = value.toString();
                if (!stringValue.isEmpty()) tip = stringValue;
            }
        }

        if (tip != null && !tip.startsWith("<html>"))
            tip = "<html>" + tip.replaceAll("(\r\n|\n\r|\r|\n)", "<br />") + "</html>";

        return tip;
    }

    @Override
    public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {
        final Component prepareRenderer = super.prepareRenderer(renderer, row, column);
        final TableColumn tableColumn = getColumnModel().getColumn(column);

        if (column != -1 && tableColumn.getHeaderValue().toString().startsWith("Attrib"))
            tableColumn.setPreferredWidth(Math.max(prepareRenderer.getPreferredSize().width, tableColumn.getPreferredWidth()));

        return prepareRenderer;
    }

    @Override
    public final void setAutoResizeMode(int mode) {
        if (disableSetAutoResizeMode) throw new UnsupportedOperationException("Forbidden operation");
        super.setAutoResizeMode(mode);
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        super.tableChanged(e);

        if (e.getType() == TableModelEvent.UPDATE) return;

        int firstRow = e.getFirstRow();
        if (firstRow == TableModelEvent.HEADER_ROW) return;

        int lastRow = e.getLastRow();
        int numRows = lastRow - firstRow + 1;

        int column = e.getColumn();
        int firstColumn;
        int lastColumn;
        if (column == TableModelEvent.ALL_COLUMNS) {
            firstColumn = 0;
            lastColumn = getModel().getColumnCount() - 1;
        } else {
            firstColumn = column;
            lastColumn = column;
        }

        for (column = firstColumn; column <= lastColumn; column++) {
            for (int row = firstRow; row <= lastRow; row++) {
                Pair<Integer, Integer> key = Pair.of(row, column);
                if (cellEditorMap.containsKey(key)) cellEditorMap.remove(Pair.of(row, column));
                if (cellRendererMap.containsKey(key)) cellRendererMap.remove(Pair.of(row, column));
                if (tooltipMap.containsKey(key)) tooltipMap.remove(Pair.of(row, column));
            }

            Map<Pair<Integer, Integer>, TableCellEditor> newCellEditorEntries = new LinkedHashMap<Pair<Integer, Integer>, TableCellEditor>();
            for (Iterator<Map.Entry<Pair<Integer, Integer>, TableCellEditor>> it = cellEditorMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Pair<Integer, Integer>, TableCellEditor> entry = it.next();

                int row2 = entry.getKey().getFirst();
                int col2 = entry.getKey().getSecond();

                if (column == col2 && row2 > lastRow) {
                    newCellEditorEntries.put(Pair.of(row2 - numRows, col2), entry.getValue());
                    it.remove();
                }
            }

            cellEditorMap.putAll(newCellEditorEntries);

            Map<Pair<Integer, Integer>, TableCellRenderer> newCellRendererEntries = new LinkedHashMap<Pair<Integer, Integer>, TableCellRenderer>();
            for (Iterator<Map.Entry<Pair<Integer, Integer>, TableCellRenderer>> it = cellRendererMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Pair<Integer, Integer>, TableCellRenderer> entry = it.next();

                int row2 = entry.getKey().getFirst();
                int col2 = entry.getKey().getSecond();

                if (column == col2 && row2 > lastRow) {
                    newCellRendererEntries.put(Pair.of(row2 - numRows, col2), entry.getValue());
                    it.remove();
                }
            }

            cellRendererMap.putAll(newCellRendererEntries);
        }
    }

    /**
     * Sets the cell editor for a given cell (default Java behavior only allows to set per-column cell editor).
     *
     * @param row             Model row
     * @param column          Model column
     * @param tableCellEditor Model
     */
    public void setCellEditor(int row, int column, TableCellEditor tableCellEditor) {
        cellEditorMap.put(Pair.of(row, column), tableCellEditor);
    }

    /**
     * Sets the cell renderer for a given cell (default Java behavior only allows to set per-column cell editor).
     *
     * @param row               Model row
     * @param column            Model column
     * @param tableCellRenderer Model
     */
    public void setCellRenderer(int row, int column, TableCellRenderer tableCellRenderer) {
        cellRendererMap.put(Pair.of(row, column), tableCellRenderer);
    }

    /**
     * Sets the tooltip text for a given cell.
     *
     * @param row         Model row
     * @param column      Model column
     * @param tooltipText Text to be shown
     * @since 0.2.2
     */
    public void setToolTipText(int row, int column, String tooltipText) {
        tooltipMap.put(Pair.of(row, column), tooltipText);
    }

    /**
     * Sets the number of visible rows in a table.
     *
     * @param table Table
     * @param rows  Number of rows to show
     * @since 0.2.0
     */
    public static void setVisibleRowCount(JTable table, int rows) {
        int height = 0;
        for (int row = 0; row < rows; row++)
            height += table.getRowHeight(row);

        table.setPreferredScrollableViewportSize(new Dimension(table.getPreferredScrollableViewportSize().width, height));
    }

    /**
     * Sets the width of the columns as percentages.
     *
     * @param table       the {@code JTable} whose columns will be set
     * @param percentages the widths of the columns as percentages; note: this
     *                    method does NOT verify that all percentages add up to 100% and for
     *                    the columns to appear properly, it is recommended that the widths for
     *                    ALL columns be specified
     * @since 0.2.0
     */
    public static void setWidthAsPercentages(JTable table, double... percentages) {
        final double factor = table.getPreferredScrollableViewportSize().getWidth();
        TableColumnModel model = table.getColumnModel();
        for (int columnIndex = 0; columnIndex < percentages.length; columnIndex++) {
            TableColumn column = model.getColumn(columnIndex);
            column.setPreferredWidth((int) (percentages[columnIndex] * factor));
        }
    }

    /**
     * Select the text when editing on a text related cell is started.
     *
     * @param e {@code EventObject}
     * @since 0.3.1
     */
    private void selectAll(EventObject e) {
        final Component editor = getEditorComponent();

        if (editor == null || !(editor instanceof JTextComponent)) return;

        if (e == null) {
            ((JTextComponent) editor).selectAll();
            return;
        }

		/* Typing in the cell was used to activate the editor */
        if (e instanceof KeyEvent && isSelectAllForKeyEvent) {
            ((JTextComponent) editor).selectAll();
            return;
        }

		/*  F2 was used to activate the editor */
        if (e instanceof ActionEvent && isSelectAllForActionEvent) {
            ((JTextComponent) editor).selectAll();
            return;
        }

		/*
		 * A mouse click was used to activate the editor.
		 * Generally this is a double click and the second mouse click is
		 * passed to the editor which would remove the text selection unless
		 * we use the invokeLater().
		 */
        if (e instanceof MouseEvent && isSelectAllForMouseEvent) {
            SwingUtilities.invokeLater(new SelectAllRunnable(editor));
        }
    }

    /**
     * Sets the 'Select All' property for all event types.
     *
     * @param isSelectAllForEdit {@code true} if 'select all' must be apply, and {@code false} otherwise
     * @since 0.3.1
     */
    public void setSelectAllForEdit(boolean isSelectAllForEdit) {
        setSelectAllForActionEvent(isSelectAllForEdit);
        setSelectAllForKeyEvent(isSelectAllForEdit);
        setSelectAllForMouseEvent(isSelectAllForEdit);
    }

    /**
     * Sets the 'Select All' property when editing is invoked by the 'F2' key.
     *
     * @param isSelectAllForActionEvent {@code true} if 'select all' must be apply, and {@code false} otherwise
     * @since 0.3.1
     */
    public void setSelectAllForActionEvent(boolean isSelectAllForActionEvent) {
        this.isSelectAllForActionEvent = isSelectAllForActionEvent;
    }

    /**
     * Sets the 'Select All' property when editing is invoked by typing directly
     * into the cell.
     *
     * @param isSelectAllForKeyEvent {@code true} if 'select all' must be apply, and {@code false} otherwise
     * @since 0.3.1
     */
    public void setSelectAllForKeyEvent(boolean isSelectAllForKeyEvent) {
        this.isSelectAllForKeyEvent = isSelectAllForKeyEvent;
    }

    /**
     * Sets the 'Select All' property when editing is invoked by the mouse.
     *
     * @param isSelectAllForMouseEvent {@code true} if 'select all' must be apply, and {@code false} otherwise
     * @since 0.3.1
     */
    public void setSelectAllForMouseEvent(boolean isSelectAllForMouseEvent) {
        this.isSelectAllForMouseEvent = isSelectAllForMouseEvent;
    }


    private static class SelectAllRunnable implements Runnable {
        private final Component editor;

        public SelectAllRunnable(Component editor) {
            this.editor = editor;
        }

        @Override
        public void run() {
            ((JTextComponent) editor).selectAll();
        }
    }
}

