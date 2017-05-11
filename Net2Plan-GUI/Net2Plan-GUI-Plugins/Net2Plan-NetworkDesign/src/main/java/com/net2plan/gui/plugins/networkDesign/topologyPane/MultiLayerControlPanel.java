package com.net2plan.gui.plugins.networkDesign.topologyPane;

import com.net2plan.gui.plugins.GUINetworkDesign;
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
    private final NetPlan netPlan;
    private JComponent[][] componentMatrix;

    private final Map<Integer, NetworkLayer> rowIndexToLayerMap;

    public static final String UP_COLUMN = "Up";
    public static final String DOWN_COLUMN = "Down";
    public static final String ACTIVE_COLUMN = "Name";
    public static final String VISIBLE_COLUMN = "Visible";

    public MultiLayerControlPanel(GUINetworkDesign callback)
    {
        assert callback != null;

        this.callback = callback;
        this.netPlan = callback.getDesign();

        this.rowIndexToLayerMap = new HashMap<>();

        this.componentMatrix = new JComponent[netPlan.getNumberOfLayers() + 1][4];

        this.setLayout(new GridLayout(componentMatrix.length, componentMatrix[0].length));

        buildPanel();
    }

    private void buildPanel()
    {
        componentMatrix[0][0] = new JLabel(UP_COLUMN);
        componentMatrix[0][1] = new JLabel(DOWN_COLUMN);
        componentMatrix[0][2] = new JLabel(ACTIVE_COLUMN);
        componentMatrix[0][3] = new JLabel(VISIBLE_COLUMN);

        final List<NetworkLayer> networkLayers = netPlan.getNetworkLayers();
        int row = 1;

        // Each row
        for (NetworkLayer layer : networkLayers)
        {
            rowIndexToLayerMap.put(row, layer);
            // Up button
            final JButton upButton = new JButton();
            upButton.setText("\u25B2");
            upButton.setName(UP_COLUMN);
            upButton.setFocusable(false);
            upButton.addActionListener(e ->
            {
            });
            componentMatrix[row][0] = upButton;

            // Down button
            final JButton downButton = new JButton();
            downButton.setText("\u25BC");
            downButton.setName(DOWN_COLUMN);
            downButton.setFocusable(false);
            downButton.addActionListener(e ->
            {
            });
            componentMatrix[row][1] = downButton;

            // Active button
            final JButton activeButton = new JButton();
            activeButton.setText(layer.getName());
            activeButton.setName(ACTIVE_COLUMN);
            activeButton.setFocusable(false);
            activeButton.addActionListener(e ->
            {
                netPlan.setNetworkLayerDefault(layer);
                callback.getVisualizationState().setCanvasLayerVisibility(layer, true);

                refreshTable();
                callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.LAYER));
            });
            componentMatrix[row][2] = activeButton;

            // Visible button
            final JToggleButton visibleButton = new JToggleButton();
            // TODO: ICON
            visibleButton.setName(VISIBLE_COLUMN);
            visibleButton.setFocusable(false);
            visibleButton.addActionListener(e ->
            {
            });
            componentMatrix[row][3] = visibleButton;

            row++;
        }

        for (JComponent[] matrix : componentMatrix)
            for (JComponent component : matrix)
                this.add(component);
    }

    public void refreshTable ()
    {
    }

    NetworkLayer getLayer(int row)
    {
        return rowIndexToLayerMap.get(row);
    }

    JComponent[][] getTable()
    {
        return componentMatrix;
    }
}
