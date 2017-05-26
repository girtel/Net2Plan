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

import javax.swing.table.DefaultTableModel;

/**
 * <p>This class extends {@code DefaultTableModel} so that computes the
 * classes of its columns from the values that they contain.</p>
 * <p>Credits to Simon White for his <a href='http://www.catalysoft.com/articles/ClassAwareTableModel.html'>A TableModel with Class</a></p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ClassAwareTableModel extends DefaultTableModel {
    /**
     * Default constructor.
     *
     * @since 0.2.0
     */
    public ClassAwareTableModel() {
        super();
    }

    /**
     * Constructs a {@code ClassAwareTableModel} and initializes the table with some data.
     *
     * @param data        Table data
     * @param columnNames Column names
     * @since 0.2.0
     */
    public ClassAwareTableModel(Object[][] data, Object[] columnNames) {
        super(data, columnNames);
    }

    @Override
    public Class getColumnClass(int col) {
        if (getRowCount() == 0) return Object.class;

        Object aux = getValueAt(0, col);
        return aux == null ? Object.class : aux.getClass();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
    
    /* Called to set a value in a cell without going through thje overriden method setValueAt */
    public void setAtValueSuper (Object aValue, int rowIndex, int columnIndex)
    {
    	super.setValueAt(aValue , rowIndex , columnIndex);
    }
}
