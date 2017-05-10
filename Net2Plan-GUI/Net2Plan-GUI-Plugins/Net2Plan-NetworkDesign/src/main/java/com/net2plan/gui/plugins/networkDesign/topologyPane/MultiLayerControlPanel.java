package com.net2plan.gui.plugins.networkDesign.topologyPane;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.NetPlan;

import javax.swing.*;
import java.awt.*;

/**
 * @author Jorge San Emeterio
 * @date 20-Jan-17
 */
public class MultiLayerControlPanel extends JPanel
{
    private final GUINetworkDesign callback;
    private final NetPlan netPlan;
    private final JComponent[][] componentMatrix;

    private final int NUMBER_OF_COLUMNS = 4;
    private final int COLUMN_UP = 0;
    private final int COLUMN_DOWN = 1;
    private final int COLUMN_NAME = 2;
    private final int COLUMN_VISIBLE = 3;

    public MultiLayerControlPanel(final GUINetworkDesign callback)
    {
        this.callback = callback;
        this.netPlan = callback.getDesign();

        this.componentMatrix = new AbstractButton[netPlan.getNumberOfLayers() + 1][NUMBER_OF_COLUMNS];
        this.setLayout(new BorderLayout());
    }

    private void buidPanel()
    {
        buildHeader();
    }

    private void buildHeader()
    {
        componentMatrix[0][COLUMN_UP];
    }

    public void refreshTable ()
    {

    }
}
