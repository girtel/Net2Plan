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
        when(callback.getVisualizationState()).thenReturn(vs);
        when(vs.isLayerVisibleInCanvas(any(NetworkLayer.class))).thenReturn(true);
        when(vs.getCanvasLayersInVisualizationOrder(true)).thenReturn(netPlan.getNetworkLayers());
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
            final JComponent component = table[i][2];
            if (matcher.matches(component))
            {
                final JButton button = (JButton) component;
                if (!button.isEnabled()) continue;

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

    @Test
    public void visibilityButtonTest()
    {
        // Mock visualization state
        doNothing().when(vs).setCanvasLayerVisibility(any(NetworkLayer.class), anyBoolean());

        MultiLayerControlPanel panel = new MultiLayerControlPanel(callback);
        GenericTypeMatcher<JToggleButton> matcher = new GenericTypeMatcher<JToggleButton>(JToggleButton.class)
        {
            @Override
            protected boolean isMatching(JToggleButton component)
            {
                return component.getName().equals(MultiLayerControlPanel.VISIBLE_COLUMN);
            }
        };

        final JComponent[][] table = panel.getTable();
        for (int i = 1; i < table.length; i++)
        {
            final JComponent component = table[i][3];

            if (matcher.matches(component))
            {
                final JToggleButton button = (JToggleButton) component;
                if (!button.isEnabled()) continue;

                button.doClick();

                final NetworkLayer layer = panel.getLayer(i);
                assertNotNull(layer);
                verify(callback.getVisualizationState()).setCanvasLayerVisibility(layer, false);

                assertFalse(button.isSelected());

                button.doClick();

                assertTrue(button.isSelected());
            } else
            {
                fail();
            }
        }
    }

    @Test
    public void moveButtonAvailabilityTest()
    {
        MultiLayerControlPanel panel = new MultiLayerControlPanel(callback);
        final JComponent[][] table = panel.getTable();

        assertFalse(table[1][0].isEnabled());
        assertFalse(table[table.length - 1][1].isEnabled());
    }

    @Test
    public void visibilityButtonAvailabilityTest()
    {
        MultiLayerControlPanel panel = new MultiLayerControlPanel(callback);
        final JComponent[][] table = panel.getTable();

        for (int i = 1; i < table.length; i++)
        {
            if (panel.getLayer(i) == netPlan.getNetworkLayerDefault())
            {
                assertFalse(table[i][2].isEnabled());
                assertFalse(table[i][3].isEnabled());
            }
        }
    }
}