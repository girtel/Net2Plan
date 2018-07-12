package com.net2plan.gui.utils;

/**
 * <p>This class extends {@link javax.swing.table.DefaultTableModel} to not allow to update any value in the table.</p>
 *
 * @author Cesar San-Nicolas-Martinez
 * @since 1.7
 */
public class NonEditableTableModel extends javax.swing.table.DefaultTableModel
{
    public NonEditableTableModel(String [] header)
    {
        super(header,0);
    }

    public NonEditableTableModel(Object [][] data, String [] header)
    {
        super(data, header);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return false;
    }
}
