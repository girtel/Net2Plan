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
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * <p>This class allows to hack a {@code JTable} with frozen columns.</p>
 * <p>Credits to "Meihta Dwiguna Saputra" and "Kurt Riede" for their codes about
 * frozen column in JTable (<a href='http://mdsaputra.wordpress.com/2011/02/08/fixed-column-jtable/'>Java
 * Swing Hack – Fixed Column Java Table</a> and
 * <a href='http://www.jroller.com/kriede/entry/swing_table_with_frozen_columns'>Swing
 * Table with frozen Columns</a>, respectively)
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class FixedColumnDecorator implements ChangeListener, PropertyChangeListener
{
    private final static MouseAdapter FIXED_COLUMN_ADAPTER;

    static
    {
        FIXED_COLUMN_ADAPTER = new FixedColumnMouseAdapter();
    }

    private int frozenColumns;
    private final JTable mainTable;
    private final JTable fixedTable;
    private final JScrollPane scroll;

    /**
     * Default constructor.
     *
     * @param scroll        Reference to the main table ScrollPane
     * @param frozenColumns Number of columns to be fixed
     * @since 0.2.0
     */
    public FixedColumnDecorator(JScrollPane scroll, int frozenColumns)
    {
        this.scroll = scroll;
        this.frozenColumns = frozenColumns;
        mainTable = (JTable) scroll.getViewport().getView();
        mainTable.setAutoCreateColumnsFromModel(false);
        mainTable.addPropertyChangeListener(this);
        fixedTable = new JTableImpl();

        fixedTable.setAutoCreateColumnsFromModel(false);
        fixedTable.setModel(mainTable.getModel());

        TableColumnModel columnModel = mainTable.getColumnModel();
        for (int i = 0; i < frozenColumns; i++)
        {
            TableColumn column = columnModel.getColumn(0);
            fixedTable.getColumnModel().addColumn(column);
            columnModel.removeColumn(column);
        }

        fixedTable.setPreferredScrollableViewportSize(fixedTable.getPreferredSize());
        scroll.setRowHeaderView(fixedTable);
        scroll.setCorner(JScrollPane.UPPER_LEFT_CORNER, fixedTable.getTableHeader());

        scroll.getRowHeader().addChangeListener(this);

        mainTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fixedTable.setSelectionModel(mainTable.getSelectionModel());
        fixedTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        mainTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        fixedTable.setRowSorter(mainTable.getRowSorter());
        mainTable.setUpdateSelectionOnSort(true);
        fixedTable.setUpdateSelectionOnSort(false);


        for (MouseMotionListener listener : mainTable.getTableHeader().getMouseMotionListeners())
        {
            if (!(listener instanceof BasicTableHeaderUI.MouseInputHandler))
            {
                fixedTable.getTableHeader().addMouseMotionListener(listener);
            }
        }

        fixedTable.setDefaultRenderer(Boolean.class, mainTable.getDefaultRenderer(Boolean.class));
        fixedTable.setDefaultRenderer(Double.class, mainTable.getDefaultRenderer(Double.class));
        fixedTable.setDefaultRenderer(Object.class, mainTable.getDefaultRenderer(Object.class));
        fixedTable.setDefaultRenderer(Float.class, mainTable.getDefaultRenderer(Float.class));
        fixedTable.setDefaultRenderer(Long.class, mainTable.getDefaultRenderer(Long.class));
        fixedTable.setDefaultRenderer(Integer.class, mainTable.getDefaultRenderer(Integer.class));
        fixedTable.setDefaultRenderer(String.class, mainTable.getDefaultRenderer(String.class));

        Set<Class> fixedTableCurrentMouseListenerClass = new LinkedHashSet<Class>();
        for (MouseListener listener : fixedTable.getMouseListeners())
            fixedTableCurrentMouseListenerClass.add(listener.getClass());

        for (MouseListener listener : mainTable.getMouseListeners())
        {
            if (fixedTableCurrentMouseListenerClass.contains(listener.getClass())) continue;
            fixedTable.addMouseListener(listener);
        }

        Set<Class> fixedTableCurrentKeyListenerClass = new LinkedHashSet<Class>();
        for (KeyListener listener : fixedTable.getKeyListeners())
            fixedTableCurrentKeyListenerClass.add(listener.getClass());

        for (KeyListener listener : mainTable.getKeyListeners())
        {
            if (fixedTableCurrentKeyListenerClass.contains(listener.getClass())) continue;
            fixedTable.addKeyListener(listener);
        }

        fixedTable.getColumnModel().addColumnModelListener(new TableColumnWidthListener());

        MouseAdapter ma = FIXED_COLUMN_ADAPTER;
        fixedTable.getTableHeader().addMouseListener(ma);

        /* set a new action for the tab key */
        final Action fixedTableNextColumnCellAction = getAction(fixedTable, KeyEvent.VK_TAB, 0);
        final Action mainTableNextColumnCellAction = getAction(mainTable, KeyEvent.VK_TAB, 0);
        final Action fixedTablePrevColumnCellAction = getAction(fixedTable, KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK);
        final Action mainTablePrevColumnCellAction = getAction(mainTable, KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK);

        setAction(fixedTable, "selectNextColumn", new LockedTableSelectNextColumnCellAction(fixedTableNextColumnCellAction));
        setAction(mainTable, "selectNextColumn", new ScrollTableSelectNextColumnCellAction(mainTableNextColumnCellAction));
        setAction(fixedTable, "selectPreviousColumn", new LockedTableSelectPreviousColumnCellAction(fixedTablePrevColumnCellAction));
        setAction(mainTable, "selectPreviousColumn", new ScrollTableSelectPreviousColumnCellAction(mainTablePrevColumnCellAction));

        setAction(fixedTable, "selectNextColumnCell", new LockedTableSelectNextColumnCellAction(fixedTableNextColumnCellAction));
        setAction(mainTable, "selectNextColumnCell", new ScrollTableSelectNextColumnCellAction(mainTableNextColumnCellAction));
        setAction(fixedTable, "selectPreviousColumnCell", new LockedTableSelectPreviousColumnCellAction(fixedTablePrevColumnCellAction));
        setAction(mainTable, "selectPreviousColumnCell", new ScrollTableSelectPreviousColumnCellAction(mainTablePrevColumnCellAction));

        setAction(mainTable, "selectFirstColumn", new ScrollTableSelectFirstColumnCellAction());
        setAction(fixedTable, "selectLastColumn", new LockedTableSelectLastColumnCellAction());
    }

    private static int nextRow(JTable table)
    {
        int row = table.getSelectedRow() + 1;
        if (row == table.getRowCount()) row = 0;

        return row;
    }

    private static int previousRow(JTable table)
    {
        int row = table.getSelectedRow() - 1;
        if (row == -1) row = table.getRowCount() - 1;

        return row;
    }


    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        switch (e.getPropertyName())
        {
            case "model":
                fixedTable.setModel(mainTable.getModel());
                break;

            case "selectionModel":
                fixedTable.setSelectionModel(mainTable.getSelectionModel());
                break;

            default:
                break;
        }
    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
        /* keeping fixed table stays in sync with the main table when stateChanged */
        JViewport viewport = (JViewport) e.getSource();
        scroll.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
    }

    private Action getAction(JComponent component, int keyCode, int modifiers)
    {
        final int condition = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);
        Object object = component.getInputMap(condition).get(keyStroke);
        if (object == null)
        {
            Container parent = component.getParent();
            return (parent instanceof JComponent) ? getAction((JComponent) parent, keyCode, modifiers) : null;
        } else
        {
            return mainTable.getActionMap().get(object);
        }
    }

    /**
     * Returns a reference to the fixed table of the decorator.
     *
     * @return Reference to the fixed table of the decorator
     * @since 0.2.0
     */
    public JTable getFixedTable()
    {
        return fixedTable;
    }

    /**
     * Returns the number of frozen columns.
     *
     * @return Number of frozen columns
     * @since 0.3.0
     */
    public final int getFrozenColumns()
    {
        return fixedTable.getColumnCount();
    }

    /**
     * Re-configures the number of fixed columns.
     *
     * @param frozenColumns Number of columns to be fixed
     * @since 0.3.0
     */
    public final void setFrozenColumns(final int frozenColumns)
    {
        rearrangeColumns(frozenColumns);
        this.frozenColumns = frozenColumns;
    }

    /**
     * Returns a reference to the main table of the decorator.
     *
     * @return Reference to the main table of the decorator
     * @since 0.2.0
     */
    public JTable getMainTable()
    {
        return mainTable;
    }

    private void rearrangeColumns(final int frozenColumns)
    {
        TableColumnModel scrollColumnModel = mainTable.getColumnModel();
        TableColumnModel lockedColumnModel = fixedTable.getColumnModel();
        if (this.frozenColumns < frozenColumns)
        {
            /* move columns from scrollable to fixed table */
            for (int i = this.frozenColumns; i < frozenColumns; i++)
            {
                TableColumn column = scrollColumnModel.getColumn(0);
                lockedColumnModel.addColumn(column);
                scrollColumnModel.removeColumn(column);
            }

            fixedTable.setPreferredScrollableViewportSize(fixedTable.getPreferredSize());
        } else if (this.frozenColumns > frozenColumns)
        {
            /* move columns from fixed to scrollable table */
            for (int i = frozenColumns; i < this.frozenColumns; i++)
            {
                TableColumn column = lockedColumnModel.getColumn(lockedColumnModel.getColumnCount() - 1);
                scrollColumnModel.addColumn(column);
                scrollColumnModel.moveColumn(scrollColumnModel.getColumnCount() - 1, 0);
                lockedColumnModel.removeColumn(column);
            }

            fixedTable.setPreferredScrollableViewportSize(fixedTable.getPreferredSize());
        }
    }

    private void setAction(JComponent component, String name, Action action)
    {
        component.getActionMap().put(name, action);
    }

    private static class FixedColumnMouseAdapter extends MouseAdapter
    {
        private TableColumn column = null;
        private int columnWidth;
        private int pressedX;

        @Override
        public void mousePressed(MouseEvent e)
        {
            JTableHeader header = (JTableHeader) e.getComponent();
            TableColumnModel tcm = header.getColumnModel();
            int columnIndex = tcm.getColumnIndexAtX(e.getX());
            Cursor cursor = header.getCursor();

            if (columnIndex == tcm.getColumnCount() - 1 && cursor == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR))
            {
                column = tcm.getColumn(columnIndex);
                columnWidth = column.getWidth();
                pressedX = e.getX();
                header.addMouseMotionListener(this);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            column = null;

            JTableHeader header = (JTableHeader) e.getComponent();
            header.removeMouseMotionListener(this);
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (column == null) return;

            int width = columnWidth - pressedX + e.getX();
            column.setPreferredWidth(width);
            JTableHeader header = (JTableHeader) e.getComponent();
            JTable table = header.getTable();
            table.setPreferredScrollableViewportSize(table.getPreferredSize());

            Container parent = table.getParent().getParent();
            if (parent instanceof JScrollPane) ((JScrollPane) parent).revalidate();
        }
    }

    private static class JTableImpl extends JTable
    {
        @Override
        public boolean getScrollableTracksViewportHeight()
        {
            return getPreferredSize().height < getParent().getHeight();
        }


    }

    private class LockedTableSelectLastColumnCellAction extends AbstractAction
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (e.getSource() == fixedTable) fixedTable.transferFocus();
            mainTable.changeSelection(mainTable.getSelectedRow(), mainTable.getColumnCount() - 1, false, false);
        }
    }

    private class LockedTableSelectNextColumnCellAction extends AbstractAction
    {
        private final Action fixedTableNextColumnCellAction;

        private LockedTableSelectNextColumnCellAction(Action fixedTableNextColumnCellAction)
        {
            this.fixedTableNextColumnCellAction = fixedTableNextColumnCellAction;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (fixedTable.getSelectedColumn() == fixedTable.getColumnCount() - 1)
            {
                fixedTable.transferFocus();
                mainTable.changeSelection(fixedTable.getSelectedRow(), 0, false, false);
            } else
            {
                fixedTableNextColumnCellAction.actionPerformed(e);
            }
        }
    }

    private class LockedTableSelectPreviousColumnCellAction extends AbstractAction
    {
        private final Action fixedTablePrevColumnCellAction;

        private LockedTableSelectPreviousColumnCellAction(Action fixedTablePrevColumnCellAction)
        {
            this.fixedTablePrevColumnCellAction = fixedTablePrevColumnCellAction;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (fixedTable.getSelectedColumn() == 0)
            {
                fixedTable.transferFocus();
                mainTable.changeSelection(previousRow(mainTable), mainTable.getColumnCount() - 1, false, false);
            } else
            {
                fixedTablePrevColumnCellAction.actionPerformed(e);
            }
        }
    }

    private class ScrollTableSelectFirstColumnCellAction extends AbstractAction
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (e.getSource() == mainTable) mainTable.transferFocusBackward();
            fixedTable.changeSelection(fixedTable.getSelectedRow(), 0, false, false);
        }
    }

    private final class ScrollTableSelectNextColumnCellAction extends AbstractAction
    {
        private final Action mainTableNextColumnCellAction;

        private ScrollTableSelectNextColumnCellAction(Action mainTableNextColumnCellAction)
        {
            this.mainTableNextColumnCellAction = mainTableNextColumnCellAction;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (mainTable.getSelectedColumn() == mainTable.getColumnCount() - 1)
            {
                mainTable.transferFocusBackward();
                fixedTable.changeSelection(nextRow(mainTable), 0, false, false);
            } else
            {
                mainTableNextColumnCellAction.actionPerformed(e);
            }
        }
    }

    private class ScrollTableSelectPreviousColumnCellAction extends AbstractAction
    {
        private final Action mainTablePrevColumnCellAction;

        private ScrollTableSelectPreviousColumnCellAction(Action mainTablePrevColumnCellAction)
        {
            this.mainTablePrevColumnCellAction = mainTablePrevColumnCellAction;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (mainTable.getSelectedColumn() == 0)
            {
                mainTable.transferFocusBackward();
                fixedTable.changeSelection(mainTable.getSelectedRow(), fixedTable.getColumnCount() - 1, false, false);
            } else
            {
                mainTablePrevColumnCellAction.actionPerformed(e);
            }
        }
    }

    private class TableColumnWidthListener implements TableColumnModelListener
    {
        @Override
        public void columnMarginChanged(ChangeEvent e)
        {
            TableColumnModel tcm = (TableColumnModel) e.getSource();
            fixedTable.setPreferredScrollableViewportSize(new Dimension(tcm.getTotalColumnWidth(), fixedTable.getSize().height));
        }

        @Override
        public void columnMoved(TableColumnModelEvent e)
        {
        }

        @Override
        public void columnAdded(TableColumnModelEvent e)
        {
        }

        @Override
        public void columnRemoved(TableColumnModelEvent e)
        {
        }

        @Override
        public void columnSelectionChanged(ListSelectionEvent e)
        {
        }
    }

}
