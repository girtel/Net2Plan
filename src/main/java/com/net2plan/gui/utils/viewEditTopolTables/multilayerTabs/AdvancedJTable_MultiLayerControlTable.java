package com.net2plan.gui.utils.viewEditTopolTables.multilayerTabs;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.CellRenderers;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.ColumnHeaderToolTips;
import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.topologyPane.VisualizationState;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.internal.Constants;
import com.net2plan.utils.StringUtils;

/**
 * @author Jorge San Emeterio
 * @date 19-Jan-17
 */
public class AdvancedJTable_MultiLayerControlTable extends AdvancedJTable
{
    private final IVisualizationCallback callback;

    private final DefaultTableModel tableModel;

    private static final int COLUMN_UP_DOWN = 0;
    private static final int COLUMN_INDEX = 1;
    private static final int COLUMN_NAME = 2;
    private static final int COLUMN_LAYER_VISIBILITY = 3;
    private static final int COLUMN_LAYER_LINK_VISIBILITY = 4;
    private static final int COLUMN_IS_DEFAULT = 5;

    private static final String[] tableHeader = StringUtils.arrayOf(
            "Move up/down",
            "Layer index",
            "Name",
            "Layer visibility",
            "Layer's link visibility",
            "Layer default"
    );

    private static final String[] tableTips = StringUtils.arrayOf(
            "Move the layer upwards/downwards",
            "Layer index",
            "Layer name",
            "Is layer visible?",
            "Are links in the layer visible?",
            "Is the active layer?"
    );

    public AdvancedJTable_MultiLayerControlTable(final IVisualizationCallback callback)
    {
        super();

        this.callback = callback;

        this.tableModel = createTableModel();

        this.setModel(tableModel);
        this.setDefaultCellRenders();

        // Configure tips
        ColumnHeaderToolTips tips = new ColumnHeaderToolTips();
        for (int c = 0; c < tableHeader.length; c++)
        {
            TableColumn col = getColumnModel().getColumn(c);
            tips.setToolTip(col, tableTips[c]);
        }
        this.getTableHeader().addMouseMotionListener(tips);

        this.updateTable();
    }

    public List<Object[]> getAllData()
    {
        final VisualizationState visualizationState = callback.getVisualizationState();

        final LinkedList<Object[]> allLayerData = new LinkedList<>();
        for (NetworkLayer networkLayer : visualizationState.getLayersInVisualizationOrder(true))
        {
            final boolean isActiveLayer = isDefaultLayer(networkLayer);

            final Object[] layerData = new Object[tableHeader.length];
            layerData[COLUMN_UP_DOWN] = "";
            layerData[COLUMN_INDEX] = networkLayer.getIndex();
            layerData[COLUMN_NAME] = networkLayer.getName();
            layerData[COLUMN_LAYER_VISIBILITY] = isActiveLayer || visualizationState.isLayerVisible(networkLayer);
            layerData[COLUMN_LAYER_LINK_VISIBILITY] = visualizationState.isLayerLinksShown(networkLayer);
            layerData[COLUMN_IS_DEFAULT] = isActiveLayer;

            allLayerData.add(layerData);
        }

        return allLayerData;
    }

    private DefaultTableModel createTableModel()
    {
        return new ClassAwareTableModel(new Object[1][tableHeader.length], tableHeader)
        {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {

                switch (columnIndex)
                {
                    case COLUMN_INDEX:
                    case COLUMN_NAME:
                        return false;
                    case COLUMN_LAYER_VISIBILITY:
                    case COLUMN_IS_DEFAULT:
                        final NetworkLayer selectedLayer = callback.getDesign().getNetworkLayer((int) this.getValueAt(rowIndex, COLUMN_INDEX));
                        return !(isDefaultLayer(selectedLayer));
                    default:
                        return true;
                }
            }

            @Override
            public void setValueAt(Object newValue, int row, int column)
            {
//                final Object oldValue = getValueAt(row, column);

                final VisualizationState visualizationState = callback.getVisualizationState();

                final NetworkLayer selectedLayer = callback.getDesign().getNetworkLayer((int) this.getValueAt(row, COLUMN_INDEX));

                switch (column)
                {
                    case COLUMN_LAYER_VISIBILITY:
                        visualizationState.setLayerVisibility(selectedLayer, (Boolean) newValue);
                        break;
                    case COLUMN_LAYER_LINK_VISIBILITY:
                        visualizationState.setLayerLinksVisibility(selectedLayer, (Boolean) newValue);
                        break;
                    case COLUMN_IS_DEFAULT:
                        callback.getDesign().setNetworkLayerDefault(selectedLayer);
                        visualizationState.setLayerVisibility(selectedLayer, true);
                        break;
                    default:
                        break;
                }

                updateTable();
                callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.LAYER));

                super.setValueAt(newValue, row, column);
            }
        };
    }

    private synchronized void updateTable()
    {
        if (callback.getDesign().getNumberOfLayers() > 0)
        {
            final List<Object[]> layerData = this.getAllData();

            // Setting up values
            this.tableModel.setDataVector(layerData.toArray(new Object[layerData.size()][tableHeader.length]), tableHeader);
        }

        // NOTE: Why do we have to update this every time?
        // Adding buttons to the Move Up/Down Colummn
        final TableColumn moveUpDownColumn = this.getColumn(tableHeader[COLUMN_UP_DOWN]);
        moveUpDownColumn.setCellRenderer(new ButtonRenderer());
        moveUpDownColumn.setCellEditor(new ButtonEditor());

        this.revalidate();
        this.repaint();
    }

    private void setDefaultCellRenders()
    {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());
    }

    private boolean isDefaultLayer(final NetworkLayer layer)
    {
        return callback.getDesign().getNetworkLayerDefault() == layer;
    }

    /**
     * Credits to user "MadProgrammer" from stack overflow for his <a href="http://stackoverflow.com/questions/17565169/unable-to-add-two-buttons-in-a-single-cell-in-a-jtable">ButtonEditor</a>.
     */
    private class ButtonEditor extends AbstractCellEditor implements TableCellEditor
    {
        private ButtonPanel buttonPanel;

        private ButtonEditor()
        {
            buttonPanel = new ButtonPanel();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
        {
            if (isSelected)
            {
                buttonPanel.setBackground(table.getSelectionBackground());
            } else
            {
                buttonPanel.setBackground(table.getBackground());
            }
            return buttonPanel;
        }

        @Override
        public Object getCellEditorValue()
        {
            return "";
        }

        @Override
        public boolean isCellEditable(EventObject e)
        {
            return true;
        }
    }

    private class ButtonRenderer extends JButton implements TableCellRenderer
    {
        private ButtonPanel panel;

        private ButtonRenderer()
        {
            panel = new ButtonPanel();
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column)
        {
            if (isSelected)
            {
                panel.setBackground(table.getSelectionBackground());
            } else
            {
                panel.setBackground(table.getBackground());
            }
            return panel;
        }
    }

    private class ButtonPanel extends JPanel implements ActionListener
    {
        private final JButton upButton, downButton;

        private ButtonPanel()
        {
            this.setLayout(new GridLayout(1, 2));
            upButton = new JButton("\u2191");
            downButton = new JButton("\u2193");

            this.add(upButton);
            this.add(downButton);

            upButton.addActionListener(this);
            downButton.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            final VisualizationState vs = callback.getVisualizationState();

            final Object src = e.getSource();
            if (src == upButton)
            {
                final NetPlan netPlan = callback.getDesign();

                final NetworkLayer selectedLayer = netPlan.getNetworkLayer((int) getValueAt(getSelectedRow(), COLUMN_INDEX));
                final NetworkLayer neighbourLayer = netPlan.getNetworkLayer((int) getValueAt(getSelectedRow() - 1, COLUMN_INDEX));

                final Map<NetworkLayer, Integer> layerOrderMap = vs.getLayerOrderIndexMap(true);

                if (layerOrderMap.get(selectedLayer) == 0) return;

                // Swap the selected layer with the one on top of it.
                this.swap(layerOrderMap, selectedLayer, neighbourLayer);

                vs.updateLayerVisualizationState(callback.getDesign(), layerOrderMap);
            } else if (src == downButton)
            {
                final NetPlan netPlan = callback.getDesign();

                final NetworkLayer selectedLayer = netPlan.getNetworkLayer((int) getValueAt(getSelectedRow(), COLUMN_INDEX));
                final NetworkLayer neighbourLayer = netPlan.getNetworkLayer((int) getValueAt(getSelectedRow() + 1, COLUMN_INDEX));

                final Map<NetworkLayer, Integer> layerOrderMap = vs.getLayerOrderIndexMap(true);

                if (layerOrderMap.get(selectedLayer) == layerOrderMap.size() - 1) return;

                // Swap the selected layer with the one on top of it.
                this.swap(layerOrderMap, selectedLayer, neighbourLayer);

                vs.updateLayerVisualizationState(callback.getDesign(), layerOrderMap);
            }

            updateTable();
            callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.LAYER));
        }

        /**
         * Credits to user "Laurence Gonsalves" from stack overflow for his <a href="http://stackoverflow.com/questions/4698143/how-to-swap-two-keys-in-a-map">swap map function</a>.
         */
        private <K, V> void swap(Map<K, V> map, K k1, K k2)
        {
            if (map.containsKey(k1))
            {
                if (map.containsKey(k2))
                {
                    map.put(k1, map.put(k2, map.get(k1)));
                } else
                {
                    map.put(k2, map.remove(k1));
                }
            } else if (map.containsKey(k2))
            {
                map.put(k1, map.remove(k2));
            }
        }
    }
}
