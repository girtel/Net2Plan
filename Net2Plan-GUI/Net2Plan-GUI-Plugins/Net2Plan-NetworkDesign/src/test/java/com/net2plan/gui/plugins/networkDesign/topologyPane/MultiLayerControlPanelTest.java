package com.net2plan.gui.plugins.networkDesign.topologyPane;

import com.net2plan.interfaces.networkDesign.NetPlan;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Jorge San Emeterio
 * @date 10/05/17
 */
public class MultiLayerControlPanelTest
{
    private static NetPlan netPlan;

    @BeforeClass
    public static void setUp()
    {
        netPlan = new NetPlan();

        netPlan.addLayer("Layer 2", "", "kbps", "kbps", null, null);
        netPlan.addLayer("Layer 3", "", "kbps", "kbps", null, null);
    }

    @Test
    public void buildTest()
    {
        MultiLayerControlPanel panel = new MultiLayerControlPanel(netPlan);
        final JComponent[][] table = panel.getTable();

        assertEquals(netPlan.getNumberOfLayers() + 1, table.length);

        // Is square
        int numCols = -1;
        for (int i = 0; i < table.length; i++)
        {
            if (numCols == -1)
            {
                numCols = table[i].length;
                continue;
            }

            assertEquals(numCols, table[i].length);
        }

        assertEquals(4, numCols);

        for (int i = 0; i < numCols; i++)
            assertTrue(table[0][i] instanceof JLabel);

        for (int i = 1; i < table.length; i++)
            for (int j = 0; j < 3; j++)
                assertTrue(table[i][j] instanceof JButton);

        for (int i = 1; i < table.length; i++)
            assertTrue(table[i][3] instanceof JToggleButton);
    }
}