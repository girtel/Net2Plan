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


package com.net2plan.gui.plugins.networkDesign;

import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.utils.StringUtils;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public final class ParamValueTable extends AdvancedJTable
{
    private String[] columnNames;

    /**
     * Default constructor.
     *
     * @since 0.2.0
     */
    public ParamValueTable() {
        this(null, null);
    }

    /**
     * Constructor which allows to indicate the table model.
     *
     * @param model Table model
     * @since 0.2.3
     */
    public ParamValueTable(DefaultTableModel model) {
        this(model, null);
    }

    /**
     * Constructor that allows to set the column names.
     *
     * @param columnNames Column name vector
     * @since 0.2.0
     */
    public ParamValueTable(String[] columnNames) {
        this(null, columnNames);
    }

    /**
     * Constructor that allows to set the table model and the column names.
     *
     * @param model       Table model
     * @param columnNames Column name vector
     * @since 0.2.3
     */
    public ParamValueTable(DefaultTableModel model, String[] columnNames) {
        super();

        if (columnNames == null) columnNames = StringUtils.arrayOf("Parameter", "Value");
        if (columnNames.length != 2)
            throw new RuntimeException("'columnNames' must have a length of 2 (or null to default values");

        this.columnNames = columnNames;
        if (model == null) model = new ClassAwareTableModel(new Object[1][2], columnNames);

        setModel(model);
        reset();
    }

    /**
     * Sets a given data in the table. Current data will be removed.
     *
     * @param data New data
     * @since 0.2.0
     */
    public void setData(Map data) {
        Object[][] objectData = new Object[data.size()][2];
        int i = 0;

        Iterator it = data.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry aux = (Map.Entry) it.next();

            objectData[i][0] = aux.getKey();
            objectData[i][1] = aux.getValue();
            i++;
        }

        setData(objectData);
    }

    private void setData(Object[][] data) {
        ((DefaultTableModel) getModel()).setDataVector(data, columnNames);
        setEnabled(true);
    }

    /**
     * Returns the data within the table.
     *
     * @return Table data
     * @since 0.2.3
     */
    public Map<String, String> getData() {
        Map<String, String> data = new LinkedHashMap<String, String>();

        if (isEnabled()) {
            TableModel model = getModel();
            int numRows = model.getRowCount();
            for (int rowId = 0; rowId < numRows; rowId++)
                data.put(model.getValueAt(rowId, 0).toString(), model.getValueAt(rowId, 1).toString());
        }

        return data;
    }

    /**
     * Resets the table.
     *
     * @since 0.2.0
     */
    public void reset() {
        ((DefaultTableModel) getModel()).setDataVector(new Object[1][2], columnNames);
        setEnabled(false);
    }
}
