package com.net2plan.gui.utils.viewEditTopolTables.multilayerTabs;

import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.CellRenderers;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.utils.StringUtils;

import javax.swing.table.TableModel;
import java.util.List;

/**
 * @author Jorge San Emeterio
 * @date 19-Jan-17
 */
public class AdvancedJTable_MultiLayerControlTable extends AdvancedJTable
{
    private final IVisualizationCallback callback;
    private final TableModel tableModel;

    private static final int COLUMN_DOWN = 0;
    private static final int COLUMN_UP = 1;
    private static final int COLUMN_INDEX = 2;
    private static final int COLUMN_NAME = 3;
    private static final int COLUMN_SHOW_LAYER = 4;
    private static final int COLUMN_SHOW_LINK = 5;
    private static final int COLUMN_DEFAULT = 6;

    private static final String[] tableHeader = StringUtils.arrayOf(
            "Move up",
            "Move down",
            "Index",
            "Name",
            "Layer visibility",
            "Layer's link visibility",
            "Layer default"
    );

    private static final String[] tableTips = StringUtils.arrayOf(
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    );

    public AdvancedJTable_MultiLayerControlTable(final IVisualizationCallback callback)
    {
        super();

        this.callback = callback;
        this.tableModel = createTableModel();

        this.setModel(tableModel);
    }

    public List<Object[]> getAllData(final NetPlan currentNetPlan)
    {
        return null;
    }

    private TableModel createTableModel()
    {
        final TableModel multiLayerTableModel = new ClassAwareTableModel(new Object[1][tableHeader.length], tableHeader)
        {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return false;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column)
            {
            }
        };

        return multiLayerTableModel;
    }

    private void setDefaultCellRenderers()
    {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());
    }
}
