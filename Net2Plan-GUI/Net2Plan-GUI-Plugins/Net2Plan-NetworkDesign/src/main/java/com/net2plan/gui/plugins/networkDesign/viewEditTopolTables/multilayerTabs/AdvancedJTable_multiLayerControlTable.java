package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.multilayerTabs;

import com.google.common.collect.Lists;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.CellRenderers;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.ColumnHeaderToolTips;
import com.net2plan.gui.utils.ColumnsAutoSizer;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.internal.Constants;
import com.net2plan.utils.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * @author Jorge San Emeterio
 * @date 19-Jan-17
 */
public class AdvancedJTable_multiLayerControlTable extends AdvancedJTable
{
    private final GUINetworkDesign callback;

    private final DefaultTableModel tableModel;

    private static final int COLUMN_UP_DOWN = 0;
    private static final int COLUMN_INDEX = 1;
    private static final int COLUMN_NAME = 2;
    private static final int COLUMN_LAYER_VISIBILITY = 3;
    private static final int COLUMN_LAYER_LINK_VISIBILITY = 4;
    private static final int COLUMN_IS_ACTIVE = 5;

    private static final String[] tableHeader = StringUtils.arrayOf(
            "Move up/down",
            "Layer index",
            "Name",
            "Layer visibility",
            "Layer's link visibility",
            "Active layer"
    );

    private static final String[] tableTips = StringUtils.arrayOf(
            "Move the layer upwards/downwards",
            "Layer index",
            "Layer name",
            "Is layer visible?",
            "Are links in the layer visible?",
            "Is the active layer?"
    );

    public AdvancedJTable_multiLayerControlTable(final GUINetworkDesign callback)
    {
        super();

        this.callback = callback;

        this.tableModel = createTableModel();

        this.setModel(tableModel);
        this.setDefaultCellRenders();

        // Removing column reordering
        this.getTableHeader().setReorderingAllowed(false);

        // Removing column resizing
        this.getTableHeader().setResizingAllowed(false);

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

        final List<Object[]> allLayerData = new ArrayList<>();
        for (NetworkLayer networkLayer : Lists.reverse(visualizationState.getCanvasLayersInVisualizationOrder(true)))
        {
            final boolean isActiveLayer = callback.getDesign().getNetworkLayerDefault() == networkLayer;

            final Object[] layerData = new Object[tableHeader.length];
            layerData[COLUMN_UP_DOWN] = new Object();
            layerData[COLUMN_INDEX] = networkLayer.getIndex();
            layerData[COLUMN_NAME] = networkLayer.getName();
            layerData[COLUMN_LAYER_VISIBILITY] = isActiveLayer || visualizationState.isLayerVisibleInCanvas(networkLayer);
            layerData[COLUMN_LAYER_LINK_VISIBILITY] = visualizationState.isCanvasLayerLinksShown(networkLayer);
            layerData[COLUMN_IS_ACTIVE] = isActiveLayer;

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
                    case COLUMN_IS_ACTIVE:
                        final NetworkLayer selectedLayer = callback.getDesign().getNetworkLayer((int) this.getValueAt(rowIndex, COLUMN_INDEX));
                        return !(callback.getDesign().getNetworkLayerDefault() == selectedLayer);
                    default:
                        return true;
                }
            }

            @Override
            public void setValueAt(Object newValue, int row, int column)
            {
                final VisualizationState visualizationState = callback.getVisualizationState();

                final NetworkLayer selectedLayer = callback.getDesign().getNetworkLayer((int) this.getValueAt(row, COLUMN_INDEX));

                switch (column)
                {
                    case COLUMN_LAYER_VISIBILITY:
                        final boolean state = (boolean) newValue;
                        visualizationState.setCanvasLayerVisibility(selectedLayer, state);
                        break;
                    case COLUMN_LAYER_LINK_VISIBILITY:
                        visualizationState.setLayerLinksVisibilityInCanvas(selectedLayer, (boolean) newValue);
                        break;
                    case COLUMN_IS_ACTIVE:
                        callback.getDesign().setNetworkLayerDefault(selectedLayer);
                        visualizationState.setCanvasLayerVisibility(selectedLayer, true);
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

    public synchronized void updateTable()
    {
        if (callback.getDesign().getNumberOfLayers() > 0)
        {
            final List<Object[]> layerData = this.getAllData();

            // Setting up values
            this.tableModel.setDataVector(layerData.toArray(new Object[layerData.size()][tableHeader.length]), tableHeader);
        }

        // Calculating column width to new values
        ColumnsAutoSizer.sizeColumnsToFit(this);

        // Adding more space for the buttons column
        this.getColumnModel().getColumn(COLUMN_UP_DOWN).setPreferredWidth(100);

        this.revalidate();
        this.repaint();
    }

    private void setDefaultCellRenders()
    {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Float.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());

        // Using Object class for button cell
        setDefaultRenderer(Object.class, new ButtonRenderer());
        setDefaultEditor(Object.class, new ButtonEditor());
    }

    // <-----Custom cell----->

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
            return null;
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
        private final JButton btn_up, btn_down;

        private ButtonPanel()
        {
            this.setLayout(new GridLayout(1, 2));
            btn_up = new JButton("\u25B2");
            btn_down = new JButton("\u25BC");

            this.add(btn_up);
            this.add(btn_down);

            btn_up.addActionListener(this);
            btn_down.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            final VisualizationState vs = callback.getVisualizationState();

            final Object src = e.getSource();
            if (src == btn_up)
            {
                if (getSelectedRow() == 0) return;

                final NetPlan netPlan = callback.getDesign();

                final NetworkLayer selectedLayer = netPlan.getNetworkLayer((int) getValueAt(getSelectedRow(), COLUMN_INDEX));
                final NetworkLayer neighbourLayer = netPlan.getNetworkLayer((int) getValueAt(getSelectedRow() - 1, COLUMN_INDEX));

                final Map<NetworkLayer, Integer> layerOrderMapConsideringNonVisible = vs.getCanvasLayerOrderIndexMap(true);

                // Swap the selected layer with the one on top of it.
                this.swap(layerOrderMapConsideringNonVisible, selectedLayer, neighbourLayer);

                vs.setCanvasLayerVisibilityAndOrder(callback.getDesign(), layerOrderMapConsideringNonVisible, null);
            } else if (src == btn_down)
            {
                if (getSelectedRow() == getRowCount() - 1) return;

                final NetPlan netPlan = callback.getDesign();

                final NetworkLayer selectedLayer = netPlan.getNetworkLayer((int) getValueAt(getSelectedRow(), COLUMN_INDEX));
                final NetworkLayer neighbourLayer = netPlan.getNetworkLayer((int) getValueAt(getSelectedRow() + 1, COLUMN_INDEX));

                final Map<NetworkLayer, Integer> layerOrderMapConsideringNonVisible = vs.getCanvasLayerOrderIndexMap(true);

                // Swap the selected layer with the one on top of it.
                this.swap(layerOrderMapConsideringNonVisible, selectedLayer, neighbourLayer);

                vs.setCanvasLayerVisibilityAndOrder(callback.getDesign(), layerOrderMapConsideringNonVisible, null);
            }

            updateTable();
            callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.LAYER));
        }

        private <K, V> void swap(Map<K, V> map, K k1, K k2)
        {
            final V value1 = map.get(k1);
            final V value2 = map.get(k2);
            if ((value1 == null) || (value2 == null)) throw new RuntimeException();
            map.remove(k1);
            map.remove(k2);
            map.put(k1, value2);
            map.put(k2, value1);
        }
    }
}
