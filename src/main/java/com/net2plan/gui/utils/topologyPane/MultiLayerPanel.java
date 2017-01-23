package com.net2plan.gui.utils.topologyPane;

import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.viewEditTopolTables.multilayerTabs.AdvancedJTable_MultiLayerControlTable;

import javax.swing.*;
import java.awt.*;

/**
 * @author Jorge San Emeterio
 * @date 20-Jan-17
 */
public class MultiLayerPanel extends JPanel
{
    private final IVisualizationCallback callback;
    private final AdvancedJTable_MultiLayerControlTable multiLayerTable;

    public MultiLayerPanel(final IVisualizationCallback callback)
    {
        this.callback = callback;
        this.multiLayerTable = new AdvancedJTable_MultiLayerControlTable(callback);

        this.setLayout(new BorderLayout());

        fillPanel();
    }

    private void fillPanel()
    {
        this.add(new JScrollPane(multiLayerTable), BorderLayout.CENTER);
    }
}
