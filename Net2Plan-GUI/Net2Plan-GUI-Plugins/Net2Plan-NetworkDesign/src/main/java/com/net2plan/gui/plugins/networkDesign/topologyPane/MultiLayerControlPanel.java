package com.net2plan.gui.plugins.networkDesign.topologyPane;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.internal.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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


        for (NetworkLayer networkLayer : networkLayers)
        {
            // Up button
            componentMatrix[row][0] = new UpButton(networkLayer);
            componentMatrix[row][1] = new DownButton(networkLayer);
            componentMatrix[row][2] = new ActiveButton(networkLayer);
            componentMatrix[row][3] = new VisibleButton(networkLayer);
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
        return null;
    }

    JComponent[][] getTable()
    {
        return componentMatrix;
    }

    private class UpButton extends JButton implements ActionListener
    {
        private final NetworkLayer layer;

        public UpButton(NetworkLayer layer)
        {
            this.layer = layer;
            this.setText("\u25B2");
            this.setName("UpButton");
            this.setFocusable(false);
            this.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
        }
    }

    private class DownButton extends JButton implements ActionListener
    {
        private final NetworkLayer layer;

        public DownButton(NetworkLayer layer)
        {
            this.layer = layer;
            this.setText("\u25BC");
            this.setName("DownButton");
            this.setFocusable(false);
            this.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            //TODO
        }
    }

    private class ActiveButton extends JButton implements ActionListener
    {
        private final NetworkLayer layer;

        public ActiveButton(NetworkLayer layer)
        {
            this.layer = layer;
            this.setFocusable(false);
            this.setName("ActiveButton");
            this.setText(layer.getName());
            this.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            callback.getDesign().setNetworkLayerDefault(layer);
            callback.getVisualizationState().setCanvasLayerVisibility(layer, true);

            refreshTable();
            callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.LAYER));
        }
    }

    private class VisibleButton extends JToggleButton implements ActionListener
    {
        private final NetworkLayer layer;

        public VisibleButton(NetworkLayer layer)
        {
            this.layer = layer;
            this.setFocusable(false);
            this.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            //TODO
        }
    }
}
