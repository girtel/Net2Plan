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
package com.net2plan.gui.plugins.networkDesign.topologyPane;

import com.google.common.annotations.VisibleForTesting;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.internal.Constants;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.collections4.MapUtils;

import javax.swing.*;
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
    public static final String ACTIVE_COLUMN = "Layer";
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
        this.componentMatrix = new JComponent[callback.getDesign().getNumberOfLayers()][4];

        final MigLayout layout = new MigLayout(
                "insets 0, gap 0, wrap 4");

        this.setLayout(layout);

        final NetPlan netPlan = callback.getDesign();

        final List<NetworkLayer> networkLayers = callback.getVisualizationState().getCanvasLayersInVisualizationOrder(true);

        // Each row
        for (int i = 0; i < networkLayers.size(); i++)
        {
            final int thisRow = i;
            final NetworkLayer layer = networkLayers.get(i);

            rowIndexToLayerMap.put(thisRow, layer);
            // Up button
            final JButton upButton = new JButton();
            upButton.setText(UP_COLUMN);
            upButton.setName(UP_COLUMN);
            upButton.setFocusable(false);
            upButton.addActionListener(e ->
            {
                if (thisRow == 0) return;

                final VisualizationState vs = callback.getVisualizationState();
                final NetworkLayer neighbourLayer = rowIndexToLayerMap.get(thisRow - 1);

                final Map<NetworkLayer, Integer> layerOrderMapConsideringNonVisible = vs.getCanvasLayerOrderIndexMap(true);

                // Swap the selected layer with the one on top of it.
                this.swap(layerOrderMapConsideringNonVisible, layer, neighbourLayer);

                vs.setCanvasLayerVisibilityAndOrder(netPlan, layerOrderMapConsideringNonVisible, vs.getCanvasLayerVisibilityMap());

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

                vs.setCanvasLayerVisibilityAndOrder(netPlan, layerOrderMapConsideringNonVisible, vs.getCanvasLayerVisibilityMap());

                callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.LAYER));
            });
            componentMatrix[thisRow][1] = downButton;

            // Active button
            final JToggleButton activeButton = new JToggleButton();
            activeButton.setText(layer.getName());
            activeButton.setToolTipText(layer.getName());
            activeButton.setName(ACTIVE_COLUMN);
            activeButton.setFocusable(false);
            activeButton.setSelected(false);
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

            if (thisRow == 0) upButton.setEnabled(false);
            if (thisRow == networkLayers.size() - 1) downButton.setEnabled(false);

            if (layer == netPlan.getNetworkLayerDefault())
            {
                activeButton.setSelected(true);
                visibleButton.setEnabled(false);
            }
        }

        for (JComponent[] matrix : componentMatrix)
            for (JComponent component : matrix)
                this.add(component, "grow, wmax 75");
    }

    public void refreshTable ()
    {
        this.removeAll();

        this.buildPanel();
    }

    @VisibleForTesting
    NetworkLayer getLayer(int row)
    {
        return rowIndexToLayerMap.get(row);
    }

    @VisibleForTesting
    int getLayerIndex(NetworkLayer layer)
    {
        return MapUtils.invertMap(rowIndexToLayerMap).get(layer);
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
