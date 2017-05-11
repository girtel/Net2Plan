package com.net2plan.gui.plugins.networkDesign.topologyPane;

import com.google.common.annotations.VisibleForTesting;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.internal.Constants;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jorge San Emeterio
 * @date 20-Jan-17
 */
public final class MultiLayerControlPanel extends JPanel
{
    private final GUINetworkDesign callback;

    private JComponent[][] componentMatrix;

    private final Map<Integer, NetworkLayer> rowIndexToLayerMap;

    public static final String UP_COLUMN = "\u25B2";
    public static final String DOWN_COLUMN = "\u25BC";
    public static final String ACTIVE_COLUMN = "Name";
    public static final String VISIBLE_COLUMN = "Visible";

    public MultiLayerControlPanel(GUINetworkDesign callback)
    {
        assert callback != null;

        this.callback = callback;

        this.rowIndexToLayerMap = new HashMap<>();

        buildPanel();
    }

    private void buildPanel()
    {
        this.rowIndexToLayerMap.clear();
        this.componentMatrix = new JComponent[callback.getDesign().getNumberOfLayers() + 1][4];
        this.setLayout(new GridLayout(componentMatrix.length, componentMatrix[0].length));

        componentMatrix[0][0] = new JLabel(UP_COLUMN);
        componentMatrix[0][1] = new JLabel(DOWN_COLUMN);
        componentMatrix[0][2] = new JLabel(ACTIVE_COLUMN);
        componentMatrix[0][3] = new JLabel(VISIBLE_COLUMN);

        final NetPlan netPlan = callback.getDesign();

        final List<NetworkLayer> networkLayers = callback.getVisualizationState().getCanvasLayersInVisualizationOrder(true);

        int row = 1;
        // Each row
        for (NetworkLayer layer : networkLayers)
        {
            final int thisRow = row;

            rowIndexToLayerMap.put(thisRow, layer);
            // Up button
            final JButton upButton = new JButton();
            upButton.setText(UP_COLUMN);
            upButton.setName(UP_COLUMN);
            upButton.setFocusable(false);
            upButton.addActionListener(e ->
            {
                if (thisRow == 1) return;

                final VisualizationState vs = callback.getVisualizationState();
                final NetworkLayer neighbourLayer = rowIndexToLayerMap.get(thisRow - 1);

                final Map<NetworkLayer, Integer> layerOrderMapConsideringNonVisible = vs.getCanvasLayerOrderIndexMap(true);

                // Swap the selected layer with the one on top of it.
                this.swap(layerOrderMapConsideringNonVisible, layer, neighbourLayer);

                vs.setCanvasLayerVisibilityAndOrder(netPlan, layerOrderMapConsideringNonVisible, null);

                callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.LAYER));
            });
            componentMatrix[thisRow][0] = upButton;

            // Down button
            final JButton downButton = new JButton();
            downButton.setText(DOWN_COLUMN);
            downButton.setName(DOWN_COLUMN);
            downButton.setFocusable(false);
            downButton.addActionListener(e ->
            {
                if (thisRow == componentMatrix.length - 1) return;

                final VisualizationState vs = callback.getVisualizationState();
                final NetworkLayer neighbourLayer = rowIndexToLayerMap.get(thisRow + 1);

                final Map<NetworkLayer, Integer> layerOrderMapConsideringNonVisible = vs.getCanvasLayerOrderIndexMap(true);

                // Swap the selected layer with the one on top of it.
                this.swap(layerOrderMapConsideringNonVisible, layer, neighbourLayer);

                vs.setCanvasLayerVisibilityAndOrder(netPlan, layerOrderMapConsideringNonVisible, null);

                callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.LAYER));
            });
            componentMatrix[thisRow][1] = downButton;

            // Active button
            final JButton activeButton = new JButton();
            activeButton.setText(layer.getName());
            activeButton.setName(ACTIVE_COLUMN);
            activeButton.setFocusable(false);
            activeButton.addActionListener(e ->
            {
                netPlan.setNetworkLayerDefault(layer);
                callback.getVisualizationState().setCanvasLayerVisibility(layer, true);

                callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.LAYER));
            });
            componentMatrix[thisRow][2] = activeButton;

            // Visible button
            final JToggleButton visibleButton = new JToggleButton();
            visibleButton.setIcon(new ImageIcon(MultiLayerControlPanel.class.getResource("/resources/gui/eye.png")));
            visibleButton.setName(VISIBLE_COLUMN);
            visibleButton.setSelected(callback.getVisualizationState().isLayerVisibleInCanvas(layer));
            visibleButton.setFocusable(false);
            visibleButton.addActionListener(e ->
            {
                callback.getVisualizationState().setCanvasLayerVisibility(layer, visibleButton.isSelected());

                callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.LAYER));
            });
            componentMatrix[thisRow][3] = visibleButton;

            // Limiting buttons
            if (netPlan.getNumberOfLayers() == 1)
            {
                upButton.setEnabled(false);
                downButton.setEnabled(false);
            }

            if (thisRow == 1) upButton.setEnabled(false);
            if (thisRow == networkLayers.size()) downButton.setEnabled(false);

            if (layer == netPlan.getNetworkLayerDefault())
            {
                activeButton.setEnabled(false);
                visibleButton.setEnabled(false);
            }

            row++;
        }

        for (JComponent[] matrix : componentMatrix)
            for (JComponent component : matrix)
                this.add(component);
    }

    public void refreshTable ()
    {
        this.removeAll();

        this.componentMatrix = new JComponent[callback.getDesign().getNumberOfLayers() + 1][4];
        this.rowIndexToLayerMap.clear();
        this.setLayout(new GridLayout(componentMatrix.length, componentMatrix[0].length));

        this.buildPanel();

        this.validate();
        this.repaint();
    }

    @VisibleForTesting
    NetworkLayer getLayer(int row)
    {
        return rowIndexToLayerMap.get(row);
    }

    @VisibleForTesting
    JComponent[][] getTable()
    {
        return componentMatrix;
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
