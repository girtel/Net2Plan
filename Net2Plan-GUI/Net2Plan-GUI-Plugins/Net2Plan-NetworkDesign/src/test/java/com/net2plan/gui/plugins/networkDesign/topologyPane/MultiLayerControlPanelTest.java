package com.net2plan.gui.plugins.networkDesign.topologyPane;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import org.assertj.swing.core.GenericTypeMatcher;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.swing.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Jorge San Emeterio
 * @date 10/05/17
 */
@RunWith(MockitoJUnitRunner.class)
public class MultiLayerControlPanelTest
{
    @Mock
    private static GUINetworkDesign callback = mock(GUINetworkDesign.class);

    @Mock
    private static VisualizationState vs = mock(VisualizationState.class);

    private static NetPlan netPlan;

    @BeforeClass
    public static void setUp()
    {
        netPlan = new NetPlan();

        netPlan.addLayer("Layer 2", "", "kbps", "kbps", null, null);
        netPlan.addLayer("Layer 3", "", "kbps", "kbps", null, null);
    }

    @Before
    public void prepareMock()
    {
        when(callback.getDesign()).thenReturn(netPlan);
    }

    @Test
    public void buildTest()
    {
        MultiLayerControlPanel panel = new MultiLayerControlPanel(callback);
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

    @Test
    public void columnOrderTest()
    {
        MultiLayerControlPanel panel = new MultiLayerControlPanel(callback);
        final JComponent[][] table = panel.getTable();
        final JComponent[] labelRow = table[0];

        assertEquals(((JLabel) labelRow[0]).getText(), MultiLayerControlPanel.UP_COLUMN);
        assertEquals(((JLabel) labelRow[1]).getText(), MultiLayerControlPanel.DOWN_COLUMN);
        assertEquals(((JLabel) labelRow[2]).getText(), MultiLayerControlPanel.ACTIVE_COLUMN);
        assertEquals(((JLabel) labelRow[3]).getText(), MultiLayerControlPanel.VISIBLE_COLUMN);
    }

    @Test
    public void rowAssociationTest()
    {
        MultiLayerControlPanel panel = new MultiLayerControlPanel(callback);

        final int numRows = panel.getTable().length;

        for (int i = 0; i < numRows; i++)
        {
            if (i == 0) assertNull(panel.getLayer(i));
            else assertNotNull(panel.getLayer(i));
        }
    }

    @Test
    public void activeLayerTest()
    {
        // Mock visualization state
        when(callback.getVisualizationState()).thenReturn(vs);
        doNothing().when(vs).setCanvasLayerVisibility(any(NetworkLayer.class), anyBoolean());

        MultiLayerControlPanel panel = new MultiLayerControlPanel(callback);

        GenericTypeMatcher<JButton> matcher = new GenericTypeMatcher<JButton>(JButton.class)
        {
            @Override
            protected boolean isMatching(JButton component)
            {
                return component.getName().equals(MultiLayerControlPanel.ACTIVE_COLUMN);
            }
        };

        final JComponent[][] table = panel.getTable();
        for (int i = 1; i < table.length; i++)
        {
            if (matcher.matches(table[i][2]))
            {
                final JButton button = (JButton) table[i][2];
                button.doClick();

                final NetworkLayer layer = panel.getLayer(i);
                assertNotNull(layer);

                verify(callback.getVisualizationState()).setCanvasLayerVisibility(layer, true);

                assertTrue(layer.isDefaultLayer());
            } else
            {
                fail();
            }
        }
    }
}