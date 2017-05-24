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
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * <p>The ButtonColumn class provides a renderer and an editor that looks like a
 * {@code JButton}. The renderer and editor will then be used for a specified
 * column in the table. The TableModel will contain the String to be displayed on
 * the button.</p>
 * <p>The button can be invoked by a mouse click or by pressing the space bar
 * when the cell has focus. Optionally a mnemonic can be set to invoke the
 * button. When the button is invoked the provided Action is invoked. The
 * source of the Action will be the table. The action command will contain
 * the model row number of the button that was clicked.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 *
 * @see <a href="http://tips4java.wordpress.com/2009/07/12/table-button-column/"></a>
 * @since 0.2.3
 */
public class ButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener, MouseListener {
    private final JTable table;
    private final Action action;
    private int mnemonic;
    private final Border originalBorder;
    private Border focusBorder;
    private final JButton renderButton;
    private final JButton editButton;
    private Object editorValue;
    private boolean isButtonColumnEditor;

    /**
     * Create the ButtonColumn to be used as a renderer and editor. The
     * renderer and editor will automatically be installed on the TableColumn
     * of the specified column.
     *
     * @param table  the table containing the button renderer/editor
     * @param action the Action to be invoked when the button is invoked
     * @param column the column to which the button renderer/editor is added
     * @since 0.2.3
     */
    public ButtonColumn(JTable table, Action action, int column) {
        this.table = table;
        this.action = action;

        renderButton = new JButton();
        editButton = new JButton();
        editButton.setFocusPainted(false);
        editButton.addActionListener(this);
        originalBorder = editButton.getBorder();
        setFocusBorder(new LineBorder(Color.BLUE));

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(column).setCellRenderer(this);
        columnModel.getColumn(column).setCellEditor(this);
        table.addMouseListener(this);
    }

    /**
     * Get foreground color of the button when the cell has focus.
     *
     * @return the foreground color
     * @since 0.2.3
     */
    public Border getFocusBorder() {
        return focusBorder;
    }

    /**
     * The foreground color of the button when the cell has focus.
     *
     * @param focusBorder the foreground color
     * @since 0.2.3
     */
    public final void setFocusBorder(Border focusBorder) {
        this.focusBorder = focusBorder;
        editButton.setBorder(focusBorder);
    }

    /**
     * Returns the mnemonic.
     *
     * @return the mnemonic
     * @since 0.2.3
     */
    public int getMnemonic() {
        return mnemonic;
    }

    /**
     * The mnemonic to activate the button when the cell has focus.
     *
     * @param mnemonic the mnemonic
     * @since 0.2.3
     */
    public void setMnemonic(int mnemonic) {
        this.mnemonic = mnemonic;
        renderButton.setMnemonic(mnemonic);
        editButton.setMnemonic(mnemonic);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value == null) {
            editButton.setText("");
            editButton.setIcon(null);
        } else if (value instanceof Icon) {
            editButton.setText("");
            editButton.setIcon((Icon) value);
        } else {
            editButton.setText(value.toString());
            editButton.setIcon(null);
        }

        this.editorValue = value;
        return editButton;
    }

    @Override
    public Object getCellEditorValue() {
        return editorValue;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            renderButton.setForeground(table.getSelectionForeground());
            renderButton.setBackground(table.getSelectionBackground());
        } else {
            renderButton.setForeground(table.getForeground());
            renderButton.setBackground(UIManager.getColor("Button.background"));
        }

        if (hasFocus) {
            renderButton.setBorder(focusBorder);
        } else {
            renderButton.setBorder(originalBorder);
        }

        if (value == null) {
            renderButton.setText("");
            renderButton.setIcon(null);
        } else if (value instanceof Icon) {
            renderButton.setText("");
            renderButton.setIcon((Icon) value);
        } else {
            renderButton.setText(value.toString());
            renderButton.setIcon(null);
        }

        return renderButton;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int row = table.convertRowIndexToModel(table.getEditingRow());
        fireEditingStopped();

		/* Invoke the Action */
        ActionEvent event = new ActionEvent(table, ActionEvent.ACTION_PERFORMED, Integer.toString(row));
        action.actionPerformed(event);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (table.isEditing() && table.getCellEditor() == this) isButtonColumnEditor = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (isButtonColumnEditor && table.isEditing()) table.getCellEditor().stopCellEditing();
        isButtonColumnEditor = false;
    }
}
