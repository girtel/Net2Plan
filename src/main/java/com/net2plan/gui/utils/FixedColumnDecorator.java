// PABLO: ForwardingRules table, appearing in hop-by-hop routing: does not have index/id columns.
// PABLO: Add option for a column "Always visible", which can have a tic (active option) or not
// These columns are moved to the right part of the fixed table, and cannot be hidden, cannot exchange order with non-fixed columns, but CAN exchange order with fixed columns
// Also, non-fixed columns can exchange order among them, but not with fixed columns
// Initially, two left most columns are set as always vissible, and all columns are shown
// PABLO: Index and id table are not special in any form, being special is just being the first columns (but in forwarding table, first columns are not index/id)
// PABLO: Hide all -> hide all the columns not fixed. If no column is fixed, then
// keep the one that is visible now at the leftmost column
//

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
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * <p>This class allows to hack a {@code JTable} with frozen columns.</p>
 * <p>
 * <p>Credits to "Meihta Dwiguna Saputra" and "Kurt Riede" for their codes about
 * frozen column in JTable (<a href='http://mdsaputra.wordpress.com/2011/02/08/fixed-column-jtable/'>Java
 * Swing Hack – Fixed Column Java Table</a> and
 * <a href='http://www.jroller.com/kriede/entry/swing_table_with_frozen_columns'>Swing
 * Table with frozen Columns</a>, respectively)
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class FixedColumnDecorator implements ChangeListener, PropertyChangeListener {
    private final static MouseAdapter FIXED_COLUMN_ADAPTER;
    private final JTable mainTable;
    private final JTable fixedTable;
    private final JScrollPane scrollPane;
    private int frozenColumns;


    private int numberOfColumns = 0;
    private final JPopupMenu showHideMenu, fixMenu;
    private final JMenu showMenu, hideMenu, checkBoxMenu;
    private final JMenuItem showAllItem, hideAllItem;
    private JCheckBox fixCheckBox, unfixCheckBox;
    private final TableModel model;
    private final ArrayList<TableColumn> hiddenColumns, shownColumns, fixedTableColumns;
    private final Map<String, Integer> indexForEachColumn, indexForEachHiddenColumn;
    private int columnIndexToHide;
    private ArrayList<JMenuItem> hiddenHeaderItems, shownHeaderItems;

    static {
        FIXED_COLUMN_ADAPTER = new FixedColumnMouseAdapter();
    }

    /**
     * Default constructor.
     *
     * @param scrollPaneOfMainTable Reference to the scrollpane containing the main table
     * @param frozenColumns         Number of columns to be fixed
     * @since 0.2.0
     */
    public FixedColumnDecorator(JScrollPane scrollPaneOfMainTable, int frozenColumns) {


        this.scrollPane = scrollPaneOfMainTable;
        this.frozenColumns = frozenColumns;

        mainTable = ((JTable) scrollPaneOfMainTable.getViewport().getView());
        mainTable.setAutoCreateColumnsFromModel(false);
        mainTable.addPropertyChangeListener(this);
        fixedTable = new JTableImpl();

        fixedTable.setAutoCreateColumnsFromModel(false);
        fixedTable.setModel(mainTable.getModel());

        TableColumnModel columnModel = mainTable.getColumnModel();
        for (int i = 0; i < frozenColumns; i++) {
            TableColumn column = columnModel.getColumn(0);
            fixedTable.getColumnModel().addColumn(column);
            columnModel.removeColumn(column);
        }

        fixedTable.setPreferredScrollableViewportSize(fixedTable.getPreferredSize());
        scrollPaneOfMainTable.setRowHeaderView(fixedTable);
        scrollPaneOfMainTable.setCorner(JScrollPane.UPPER_LEFT_CORNER, fixedTable.getTableHeader());

		/* Synchronize scrolling of fixed table header row table with the main table */
        scrollPaneOfMainTable.getRowHeader().addChangeListener(this);

        mainTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fixedTable.setSelectionModel(mainTable.getSelectionModel());
        fixedTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        mainTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        fixedTable.setRowSorter(mainTable.getRowSorter());
        mainTable.setUpdateSelectionOnSort(true);
        fixedTable.setUpdateSelectionOnSort(false);

        model = mainTable.getModel();
        hiddenColumns = new ArrayList<>();
        shownColumns = new ArrayList<>();
        fixedTableColumns = new ArrayList<>();
        numberOfColumns = mainTable.getColumnModel().getColumnCount();
        indexForEachColumn = new HashMap<>();
        indexForEachHiddenColumn = new HashMap<>();

        for (int i = 0; i < numberOfColumns; i++) {
            shownColumns.add(mainTable.getColumnModel().getColumn(i));
        }

        showHideMenu = new JPopupMenu();
        fixMenu = new JPopupMenu();
        showMenu = new JMenu("Show column");
        hideMenu = new JMenu("Hide column");
        checkBoxMenu = new JMenu("Always Visible");
        fixCheckBox = new JCheckBox("Always Visible",false);
        unfixCheckBox = new JCheckBox("",true);
        showAllItem = new JMenuItem("Show all columns");
        hideAllItem = new JMenuItem("Hide all columns");
        fixMenu.add(fixCheckBox);
        showHideMenu.add(showMenu);
        showHideMenu.add(hideMenu);
        showHideMenu.add(showAllItem);
        showHideMenu.add(hideAllItem);

        mainTable.getTableHeader().addMouseListener(new MouseAdapter(){

            @Override
            public void mouseReleased(MouseEvent ev){
                if(ev.isPopupTrigger()){
                    TableColumn clickedColumn = mainTable.getColumnModel().getColumn(mainTable.columnAtPoint(ev.getPoint()));
                    String clickedColumnName = clickedColumn.getHeaderValue().toString();
                    int clickedColumnIndex = indexForEachColumn.get(clickedColumnName);
                    fixMenu.show(ev.getComponent(),ev.getX(),ev.getY());
                    fixCheckBox.addItemListener(new ItemListener(){

                        @Override
                        public void itemStateChanged(ItemEvent e) {
                            if(fixCheckBox.isSelected() == true) {
                                shownColumns.remove(mainTable.getColumnModel().getColumn(clickedColumnIndex));
                                fromMainTableToFixedTable(clickedColumnIndex);
                                updateShowMenu();
                                updateHideMenu();
                                checkNewIndexes();
                                fixMenu.setVisible(false);
                                fixCheckBox.setSelected(false);
                            }

                        }
                    });


                }



            }
        });
        fixedTable.getTableHeader().addMouseListener(new MouseAdapter(){
            @Override
            public void mouseReleased(MouseEvent e) {


                //Checking if right button is clicked
                if (e.isPopupTrigger()) {
                    checkNewIndexes();
                    updateShowMenu();
                    updateHideMenu();
                    TableColumn clickedColumn = fixedTable.getColumnModel().getColumn(fixedTable.columnAtPoint(e.getPoint()));
                    int clickedColumnIndex = fixedTable.getColumnModel().getColumnIndex(clickedColumn.getIdentifier());
                    if(fixedTable.getColumnModel().getColumnCount() > 1) {
                        checkBoxMenu.add(unfixCheckBox);
                        checkBoxMenu.setVisible(true);
                        showHideMenu.add(checkBoxMenu);
                    }
                    else{
                        checkBoxMenu.setVisible(false);
                    }
                    showHideMenu.show(e.getComponent(),e.getX(),e.getY());
                    unfixCheckBox.addItemListener(new ItemListener() {

                            @Override
                            public void itemStateChanged(ItemEvent e) {
                                if(unfixCheckBox.isSelected() ==  false) {
                                    shownColumns.add(fixedTable.getColumnModel().getColumn(clickedColumnIndex));
                                    fromFixedTableToMainTable(clickedColumnIndex);
                                    updateShowMenu();
                                    updateHideMenu();
                                    checkNewIndexes();
                                    checkBoxMenu.removeAll();
                                    checkBoxMenu.setVisible(false);
                                    showHideMenu.setVisible(false);
                                    unfixCheckBox.setSelected(true);
                                }
                            }
                        });

                    for (int j = 0; j < hiddenColumns.size(); j++) {
                        JMenuItem currentItem = hiddenHeaderItems.get(j);
                        String currentColumnName = hiddenColumns.get(j).getHeaderValue().toString();
                        currentItem.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                TableColumn columnToShow = showColumn(currentColumnName, hiddenColumns, indexForEachHiddenColumn.get(currentColumnName));
                                hiddenColumns.remove(columnToShow);
                                checkNewIndexes();
                            }
                        });
                    }


                    for (int j = 0; j < indexForEachColumn.size(); j++) {
                        JMenuItem currentItem = shownHeaderItems.get(j);
                        int position = j;
                        currentItem.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                columnIndexToHide = indexForEachColumn.get(shownColumns.get(position).getHeaderValue().toString());
                                String hiddenColumnHeader = hideColumn(columnIndexToHide);
                                checkNewIndexes();
                            }
                        });
                    }
                }
            }

        });
        showAllItem.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {


                showAllColumns();
            }
        });
        hideAllItem.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {

                rescuingFixedColumnHeaders();
                rebuildFixedTable();
                hideAllColumns();


            }
        });
        mainTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {

            @Override
            public void columnAdded(TableColumnModelEvent e) {

            }

            @Override
            public void columnRemoved(TableColumnModelEvent e) {

            }

            @Override
            public void columnMoved(TableColumnModelEvent e) {

                checkNewIndexes();
            }

            @Override
            public void columnMarginChanged(ChangeEvent e) {

            }

            @Override
            public void columnSelectionChanged(ListSelectionEvent e) {

            }
        });


        for (MouseMotionListener listener : mainTable.getTableHeader().getMouseMotionListeners())
        {
            if(!(listener instanceof BasicTableHeaderUI.MouseInputHandler)){
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

        for (MouseListener listener : mainTable.getMouseListeners()) {
            if (fixedTableCurrentMouseListenerClass.contains(listener.getClass())) continue;
            fixedTable.addMouseListener(listener);
        }

        Set<Class> fixedTableCurrentKeyListenerClass = new LinkedHashSet<Class>();
        for (KeyListener listener : fixedTable.getKeyListeners())
            fixedTableCurrentKeyListenerClass.add(listener.getClass());

        for (KeyListener listener : mainTable.getKeyListeners()) {
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

    /**
     * Re-configures the menu to show hidden columns
     *
     * @param
     *
     */

    public void updateShowMenu(){
        showMenu.removeAll();
        hiddenHeaderItems = new ArrayList<>();
        for (int i = 0; i < hiddenColumns.size(); i++) {
            hiddenHeaderItems.add(new JMenuItem(hiddenColumns.get(i).getHeaderValue().toString()));
            showMenu.add(hiddenHeaderItems.get(i));

        }

    }

    /**
     * Re-configures the menu to hide shown columns
     *
     * @param
     *
     */

    public void updateHideMenu(){
        hideMenu.removeAll();
        shownHeaderItems = new ArrayList<>();
        for (int i = 0; i < shownColumns.size(); i++) {
            shownHeaderItems.add(new JMenuItem(shownColumns.get(i).getHeaderValue().toString()));
            hideMenu.add(shownHeaderItems.get(i));

        }

    }

    /**
     * When a column is moved into mainTable,
     * we have to know which are the new indexes and update indexForEachColumn
     *
     * @param
     *
     */

    public void checkNewIndexes() {
        indexForEachColumn.clear();
        numberOfColumns = mainTable.getColumnCount();
        for (int i = 0; i < numberOfColumns; i++) {
            indexForEachColumn.put(mainTable.getColumnName(i), i);
        }

    }
    /**
     * Show all columns which are hidden
     *
     * @param
     *
     */


    public void showAllColumns(){
        int columnIndex = 0;
        while(mainTable.getColumnModel().getColumnCount() < numberOfColumns + hiddenColumns.size()) {
            String currentColumnName = hiddenColumns.get(0).getHeaderValue().toString();
            String hiddenColumnName;
            columnIndex = indexForEachHiddenColumn.get(currentColumnName);
            for (TableColumn tc : hiddenColumns) {
                hiddenColumnName = tc.getHeaderValue().toString();
                if (currentColumnName.equals(hiddenColumnName)) {
                    mainTable.getColumnModel().addColumn(tc);
                    mainTable.getColumnModel().moveColumn(mainTable.getColumnCount() - 1, columnIndex);
                    shownColumns.add(tc);
                    hiddenColumns.remove(tc);
                    indexForEachHiddenColumn.remove(currentColumnName, columnIndex);
                }
            }

        }

        hiddenColumns.clear();
    }

    /**
     * Hide all columns which are shown
     *
     * @param
     *
     */

    public void hideAllColumns(){
        int counter = 0;
        while(mainTable.getColumnModel().getColumnCount() > 0){
            TableColumn columnToHide = mainTable.getColumnModel().getColumn(0);
            columnIndexToHide = indexForEachColumn.get(shownColumns.get(counter).getHeaderValue().toString());
            String hiddenColumnHeader = columnToHide.getHeaderValue().toString();
            hiddenColumns.add(columnToHide);
            indexForEachHiddenColumn.put(hiddenColumnHeader, counter);
            mainTable.getColumnModel().removeColumn(columnToHide);
            counter++;
        }
        checkNewIndexes();
        shownColumns.clear();
    }

    /**
     * Show one column which is hidden
     *
     * @param columnName Name of the column which we want to show
     * @param hiddenColumns List where the columns which are hidden are stored
     * @param columnIndex Index which the column had when it was shown
     *
     * @return The column to be shown
     */

    public TableColumn showColumn(String columnName, ArrayList<TableColumn> hiddenColumns, int columnIndex) {

        String hiddenColumnName;
        TableColumn columnToShow = null;
        for (TableColumn tc : hiddenColumns) {
            hiddenColumnName = tc.getHeaderValue().toString();
            if (columnName.equals(hiddenColumnName)) {
                mainTable.getColumnModel().addColumn(tc);
                mainTable.getColumnModel().moveColumn(mainTable.getColumnCount() - 1, columnIndex);
                shownColumns.add(tc);
                indexForEachHiddenColumn.remove(columnName, columnIndex);
                columnToShow = tc;
            }
        }
        return columnToShow;
    }

    /**
     * Hide one column which is shown
     *
     * @param columnIndex Index which the column has in the current Table
     *
     * @return The column to be hidden
     */
    public String hideColumn(int columnIndex) {

        TableColumn columnToHide = mainTable.getColumnModel().getColumn(columnIndex);
        String hiddenColumnHeader = columnToHide.getHeaderValue().toString();
        hiddenColumns.add(columnToHide);
        shownColumns.remove(columnToHide);
        indexForEachHiddenColumn.put(hiddenColumnHeader, columnIndex);
        mainTable.getColumnModel().removeColumn(columnToHide);
        return hiddenColumnHeader;


    }

    /**
     * Move one column from mainTable to fixedTable
     *

     * @param columnIndex Index which the column has in mainTable
     *
     */
    public void fromMainTableToFixedTable(int columnIndex){
        TableColumn columnToFix = mainTable.getColumnModel().getColumn(columnIndex);
        mainTable.getColumnModel().removeColumn(columnToFix);
        fixedTable.getColumnModel().addColumn(columnToFix);
    }

    /**
     * Move one column from fixedTable to mainTable
     *

     * @param columnIndex Index which the column has in fixedTable
     *
     */

    public void fromFixedTableToMainTable(int columnIndex){
        TableColumn columnToUnfix = fixedTable.getColumnModel().getColumn(columnIndex);
        fixedTable.getColumnModel().removeColumn(columnToUnfix);
        mainTable.getColumnModel().addColumn(columnToUnfix);
        mainTable.getColumnModel().moveColumn(mainTable.getColumnModel().getColumnCount() - 1,0);

    }

    public void rescuingFixedColumnHeaders(){
        fixedTableColumns.clear();
        fixedTable.setVisible(false);
        for(int i = 0;i<fixedTable.getColumnModel().getColumnCount();i++){
            fixedTableColumns.add(fixedTable.getColumnModel().getColumn(i));
            fixedTable.getColumnModel().removeColumn(fixedTableColumns.get(i));
        }
    }

    public void rebuildFixedTable(){
        fixedTable.removeAll();
        fixedTable.setVisible(true);
        for(int i = 0;i<fixedTableColumns.size();i++){
            fixedTable.getColumnModel().addColumn(fixedTableColumns.get(i));
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        switch (e.getPropertyName()) {
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
    public void stateChanged(ChangeEvent e) {
        /* keeping fixed table stays in sync with the main table when stateChanged */
        JViewport viewport = (JViewport) e.getSource();
        scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
    }

    private Action getAction(JComponent component, int keyCode, int modifiers) {
        final int condition = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);
        Object object = component.getInputMap(condition).get(keyStroke);
        if (object == null) {
            Container parent = component.getParent();
            return (parent instanceof JComponent) ? getAction((JComponent) parent, keyCode, modifiers) : null;
        } else {
            return mainTable.getActionMap().get(object);
        }
    }

    /**
     * Returns a reference to the fixed table of the decorator.
     *
     * @return Reference to the fixed table of the decorator
     * @since 0.2.0
     */
    public JTable getFixedTable() {
        return fixedTable;
    }

    /**
     * Returns the number of frozen columns.
     *
     * @return Number of frozen columns
     * @since 0.3.0
     */
    public final int getFrozenColumns() {
        return frozenColumns;
    }

    /**
     * Returns a reference to the main table of the decorator.
     *
     * @return Reference to the main table of the decorator
     * @since 0.2.0
     */
    public JTable getMainTable() {
        return mainTable;
    }

    private static int nextRow(JTable table) {
        int row = table.getSelectedRow() + 1;
        if (row == table.getRowCount()) row = 0;

        return row;
    }

    private static int previousRow(JTable table) {
        int row = table.getSelectedRow() - 1;
        if (row == -1) row = table.getRowCount() - 1;

        return row;
    }

    private void rearrangeColumns(final int frozenColumns) {
        TableColumnModel scrollColumnModel = mainTable.getColumnModel();
        TableColumnModel lockedColumnModel = fixedTable.getColumnModel();
        if (this.frozenColumns < frozenColumns) {
			/* move columns from scrollable to fixed table */
            for (int i = this.frozenColumns; i < frozenColumns; i++) {
                TableColumn column = scrollColumnModel.getColumn(0);
                lockedColumnModel.addColumn(column);
                scrollColumnModel.removeColumn(column);
            }

            fixedTable.setPreferredScrollableViewportSize(fixedTable.getPreferredSize());
        } else if (this.frozenColumns > frozenColumns) {
			/* move columns from fixed to scrollable table */
            for (int i = frozenColumns; i < this.frozenColumns; i++) {
                TableColumn column = lockedColumnModel.getColumn(lockedColumnModel.getColumnCount() - 1);
                scrollColumnModel.addColumn(column);
                scrollColumnModel.moveColumn(scrollColumnModel.getColumnCount() - 1, 0);
                lockedColumnModel.removeColumn(column);
            }

            fixedTable.setPreferredScrollableViewportSize(fixedTable.getPreferredSize());
        }
    }

    private void setAction(JComponent component, String name, Action action) {
        component.getActionMap().put(name, action);
    }

    /**
     * Re-configures the number of fixed columns.
     *
     * @param frozenColumns Number of columns to be fixed
     * @since 0.3.0
     */
    public final void setFrozenColumns(final int frozenColumns) {
        rearrangeColumns(frozenColumns);
        this.frozenColumns = frozenColumns;
    }

    private static class FixedColumnMouseAdapter extends MouseAdapter {
        private TableColumn column = null;
        private int columnWidth;
        private int pressedX;

        @Override
        public void mousePressed(MouseEvent e) {
            JTableHeader header = (JTableHeader) e.getComponent();
            TableColumnModel tcm = header.getColumnModel();
            int columnIndex = tcm.getColumnIndexAtX(e.getX());
            Cursor cursor = header.getCursor();

            if (columnIndex == tcm.getColumnCount() - 1 && cursor == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)) {
                column = tcm.getColumn(columnIndex);
                columnWidth = column.getWidth();
                pressedX = e.getX();
                header.addMouseMotionListener(this);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            column = null;

            JTableHeader header = (JTableHeader) e.getComponent();
            header.removeMouseMotionListener(this);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
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

    private static class JTableImpl extends JTable {
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getPreferredSize().height < getParent().getHeight();
        }
    }

    private class LockedTableSelectLastColumnCellAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == fixedTable) fixedTable.transferFocus();
            mainTable.changeSelection(mainTable.getSelectedRow(), mainTable.getColumnCount() - 1, false, false);
        }
    }

    private class LockedTableSelectNextColumnCellAction extends AbstractAction {
        private final Action fixedTableNextColumnCellAction;

        private LockedTableSelectNextColumnCellAction(Action fixedTableNextColumnCellAction) {
            this.fixedTableNextColumnCellAction = fixedTableNextColumnCellAction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (fixedTable.getSelectedColumn() == fixedTable.getColumnCount() - 1) {
                fixedTable.transferFocus();
                mainTable.changeSelection(fixedTable.getSelectedRow(), 0, false, false);
            } else {
                fixedTableNextColumnCellAction.actionPerformed(e);
            }
        }
    }

    private class LockedTableSelectPreviousColumnCellAction extends AbstractAction {
        private final Action fixedTablePrevColumnCellAction;

        private LockedTableSelectPreviousColumnCellAction(Action fixedTablePrevColumnCellAction) {
            this.fixedTablePrevColumnCellAction = fixedTablePrevColumnCellAction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (fixedTable.getSelectedColumn() == 0) {
                fixedTable.transferFocus();
                mainTable.changeSelection(previousRow(mainTable), mainTable.getColumnCount() - 1, false, false);
            } else {
                fixedTablePrevColumnCellAction.actionPerformed(e);
            }
        }
    }

    private class ScrollTableSelectFirstColumnCellAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == mainTable) mainTable.transferFocusBackward();
            fixedTable.changeSelection(fixedTable.getSelectedRow(), 0, false, false);
        }
    }

    private final class ScrollTableSelectNextColumnCellAction extends AbstractAction {
        private final Action mainTableNextColumnCellAction;

        private ScrollTableSelectNextColumnCellAction(Action mainTableNextColumnCellAction) {
            this.mainTableNextColumnCellAction = mainTableNextColumnCellAction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (mainTable.getSelectedColumn() == mainTable.getColumnCount() - 1) {
                mainTable.transferFocusBackward();
                fixedTable.changeSelection(nextRow(mainTable), 0, false, false);
            } else {
                mainTableNextColumnCellAction.actionPerformed(e);
            }
        }
    }

    private class ScrollTableSelectPreviousColumnCellAction extends AbstractAction {
        private final Action mainTablePrevColumnCellAction;

        private ScrollTableSelectPreviousColumnCellAction(Action mainTablePrevColumnCellAction) {
            this.mainTablePrevColumnCellAction = mainTablePrevColumnCellAction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (mainTable.getSelectedColumn() == 0) {
                mainTable.transferFocusBackward();
                fixedTable.changeSelection(mainTable.getSelectedRow(), fixedTable.getColumnCount() - 1, false, false);
            } else {
                mainTablePrevColumnCellAction.actionPerformed(e);
            }
        }
    }

    private class TableColumnWidthListener implements TableColumnModelListener {
        @Override
        public void columnMarginChanged(ChangeEvent e) {
            TableColumnModel tcm = (TableColumnModel) e.getSource();
            fixedTable.setPreferredScrollableViewportSize(new Dimension(tcm.getTotalColumnWidth(), fixedTable.getSize().height));
        }

        @Override
        public void columnMoved(TableColumnModelEvent e) {
        }

        @Override
        public void columnAdded(TableColumnModelEvent e) {
        }

        @Override
        public void columnRemoved(TableColumnModelEvent e) {
        }

        @Override
        public void columnSelectionChanged(ListSelectionEvent e) {
        }
    }
}
