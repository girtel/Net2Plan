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
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.EventObject;

/**
 * <p>Cell editor that allows to trigger some action when clicked on the "..." button.</p>
 * <p>Credits to Santhosh Kumar for his <a href='http://jroller.com/santhosh/entry/add_button_to_any_tablecelleditor'>Add [...] button to any TableCellEditor</a>.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public abstract class ActionTableCellEditor implements TableCellEditor, ActionListener {
    private final static FocusAdapter FOCUS_LISTENER;
    private final TableCellEditor editor;
    private final JButton customEditorButton = new JButton("...");
    private JTable table;
    private int row, column;

    static {
        FOCUS_LISTENER = new FocusAdapterImpl();
    }

    /**
     * Default constructor.
     *
     * @param editor Cell editor
     * @since 0.2.0
     */
    public ActionTableCellEditor(TableCellEditor editor) {
        this.editor = editor;
        customEditorButton.addActionListener(this);

		/* ui-tweaking */
        customEditorButton.setFocusable(false);
        customEditorButton.setFocusPainted(false);
        customEditorButton.setMargin(new Insets(0, 0, 0, 0));
    }

    @Override
    public final void actionPerformed(ActionEvent e) {
        cancelCellEditing();
        editCell(table, row, column);
    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {
        editor.addCellEditorListener(l);
    }

    @Override
    public void cancelCellEditing() {
        editor.cancelCellEditing();
    }

    @Override
    public Object getCellEditorValue() {
        return editor.getCellEditorValue();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(editor.getTableCellEditorComponent(table, value, isSelected, row, column));
        panel.add(customEditorButton, BorderLayout.EAST);
        this.table = table;
        this.row = row;
        this.column = column;

        panel.addFocusListener(FOCUS_LISTENER);

        return panel;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return editor.isCellEditable(anEvent);
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
        editor.removeCellEditorListener(l);
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return editor.shouldSelectCell(anEvent);
    }

    @Override
    public boolean stopCellEditing() {
        return editor.stopCellEditing();
    }

    /**
     * Action handler when "..." button is clicked.
     *
     * @param table  Reference to the table
     * @param row    Row index (in view order)
     * @param column Column index (in view order)
     * @since 0.2.0
     */
    protected abstract void editCell(JTable table, int row, int column);

    private static class FocusAdapterImpl extends FocusAdapter {
        @Override
        public void focusGained(FocusEvent e) {
            JPanel panel = (JPanel) e.getSource();
            panel.getComponent(0).requestFocus();
            panel.removeFocusListener(this);
        }
    }
}
