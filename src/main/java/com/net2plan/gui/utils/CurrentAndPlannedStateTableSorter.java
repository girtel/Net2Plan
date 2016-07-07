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
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.Comparator;

/**
 * Table sorter that allows to consider 'initial netPlan state' as a common rowIndexModel
 * to the current one, so that ordering is related to the current one.
 *
 * @param <M> Model type
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class CurrentAndPlannedStateTableSorter<M extends TableModel> extends TableRowSorter<M> {
    /**
     * Default constructor.
     *
     * @param model Table model
     * @since 0.2.0
     */
    public CurrentAndPlannedStateTableSorter(M model) {
        super(model);
    }

    @Override
    public Comparator<?> getComparator(int column) {
        return new CurrentAndPlannedStateTableCellValueComparator(super.getComparator(column));
    }

    @Override
    public void setModel(M model) {
        super.setModel(model);
        setModelWrapper(new CurrentAndPlannedStateModelWrapper<M>(getModelWrapper()));
    }

    @Override
    protected boolean useToString(int column) {
        return false;
    }

    private class CurrentAndPlannedStateModelWrapper<M extends TableModel> extends DefaultRowSorter.ModelWrapper<M, Integer> {
        private final DefaultRowSorter.ModelWrapper<M, Integer> delegate;

        public CurrentAndPlannedStateModelWrapper(DefaultRowSorter.ModelWrapper<M, Integer> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int getColumnCount() {
            return delegate.getColumnCount();
        }

        @Override
        public Integer getIdentifier(int row) {
            return delegate.getIdentifier(row);
        }

        @Override
        public M getModel() {
            return delegate.getModel();
        }

        @Override
        public int getRowCount() {
            return delegate.getRowCount();
        }

        @Override
        public String getStringValueAt(int row, int column) {
            return delegate.getStringValueAt(row, column);
        }

        @Override
        public Object getValueAt(int row, int column) {
            return new CurrentAndPlannedStateTableCellValue(row, column, delegate.getValueAt(row, column));
        }
    }

    /**
     * Class encapsulating cell value and coordinates information.
     *
     * @since 0.2.0
     */
    public static class CurrentAndPlannedStateTableCellValue {
        /**
         * Column index in model order.
         *
         * @since 0.2.0
         */
        public int columnIndexModel;

        /**
         * Row index in model order.
         *
         * @since 0.2.0
         */
        public int rowIndexModel;

        /**
         * Cell value.
         *
         * @since 0.2.0
         */
        public Object value;

        /**
         * Default constructor.
         *
         * @param rowIndexModel    Row index in model order
         * @param columnIndexModel Column index in model order
         * @param value            Cell value
         * @since 0.2.0
         */
        public CurrentAndPlannedStateTableCellValue(int rowIndexModel, int columnIndexModel, Object value) {
            if (rowIndexModel < 0 && columnIndexModel < 0) throw new RuntimeException("Bad");

            this.rowIndexModel = rowIndexModel;
            this.columnIndexModel = columnIndexModel;
            this.value = value;
        }
    }

    private class CurrentAndPlannedStateTableCellValueComparator implements Comparator<CurrentAndPlannedStateTableCellValue> {
        private final Comparator delegate;

        public CurrentAndPlannedStateTableCellValueComparator(Comparator delegate) {
            if (delegate == null) throw new RuntimeException("Bad");
            this.delegate = delegate;
        }

        @Override
        public int compare(CurrentAndPlannedStateTableCellValue cell1, CurrentAndPlannedStateTableCellValue cell2) {
            Object value1 = cell1.value;
            Object value2 = cell2.value;
            int row1 = cell1.rowIndexModel;
            int row2 = cell2.rowIndexModel;

            if (getModel().getValueAt(row1, 0) == null)
                if (row1 > 0) value1 = getModel().getValueAt(row1 - 1, cell1.columnIndexModel);

            if (getModel().getValueAt(row2, 0) == null)
                if (row2 > 0) value2 = getModel().getValueAt(row2 - 1, cell2.columnIndexModel);

            return delegate.compare(value1, value2);
        }
    }
}
