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
import java.util.List;

/**
 * @author Jorge San Emeterio
 * @date 20-Jan-17
 */
public final class MultiLayerControlPanel extends JPanel
{
    private final GUINetworkDesign callback;
    private final NetPlan netPlan;
    private JComponent[][] componentMatrix;

    private final int COLUMN_UP = 0;
    private final int COLUMN_DOWN = 1;
    private final int COLUMN_NAME = 2;
    private final int COLUMN_VISIBLE = 3;

    public MultiLayerControlPanel(GUINetworkDesign callback)
    {
        assert callback != null;

        this.callback = callback;
        this.netPlan = callback.getDesign();

        this.componentMatrix = new JComponent[netPlan.getNumberOfLayers() + 1][4];

        this.setLayout(new GridLayout(componentMatrix.length, componentMatrix[0].length));

        buildPanel();
    }

    private void buildPanel()
    {
        componentMatrix[0][COLUMN_UP] = new JLabel("Up");
        componentMatrix[0][COLUMN_DOWN] = new JLabel("Down");
        componentMatrix[0][COLUMN_NAME] = new JLabel("Name");
        componentMatrix[0][COLUMN_VISIBLE] = new JLabel("Visible");

        final List<NetworkLayer> networkLayers = netPlan.getNetworkLayers();
        int row = 1;
        for (NetworkLayer networkLayer : networkLayers)
        {
            componentMatrix[row][COLUMN_UP] = new UpButton(networkLayer);
            componentMatrix[row][COLUMN_DOWN] = new DownButton(networkLayer);
            componentMatrix[row][COLUMN_NAME] = new ActiveButton(networkLayer);
            componentMatrix[row][COLUMN_VISIBLE] = new VisibleButton(networkLayer);
            row++;
        }

        for (JComponent[] matrix : componentMatrix)
            for (JComponent component : matrix)
                this.add(component);
    }

    public void refreshTable ()
    {
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
