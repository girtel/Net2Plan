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

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.assertj.swing.core.GenericTypeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Jorge San Emeterio
 * @date 10/05/17
 */
@RunWith(JUnitParamsRunner.class)
public class MultiLayerControlPanelTest
{
    private static GUINetworkDesign callback = mock(GUINetworkDesign.class);
    private static VisualizationState vs = mock(VisualizationState.class);

    private static NetPlan netPlan;

    private static MultiLayerControlPanel panel;
    private static JComponent[][] table;

    static
    {
        // NetPlan
        netPlan = new NetPlan();

        netPlan.addLayer("Layer 2", "", "kbps", "kbps", null, null);
        netPlan.addLayer("Layer 3", "", "kbps", "kbps", null, null);

        // Mocks
        when(callback.getDesign()).thenReturn(netPlan);
        when(callback.getVisualizationState()).thenReturn(vs);
        when(vs.isLayerVisibleInCanvas(any(NetworkLayer.class))).thenReturn(true);
        when(vs.getCanvasLayersInVisualizationOrder(true)).thenReturn(netPlan.getNetworkLayers());

        // Panel: for parameters instance
        panel = new MultiLayerControlPanel(callback);
        table = panel.getTable();
    }

    @Before
    public void refreshPanel()
    {
        panel = new MultiLayerControlPanel(callback);
        table = panel.getTable();
    }

    @Test
    public void buildTest()
    {
        assertEquals(netPlan.getNumberOfLayers(), table.length);

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

        for (int i = 0; i < table.length; i++)
            for (int j = 0; j < 2; j++)
                assertTrue(table[i][j] instanceof JButton);

        for (int i = 0; i < table.length; i++)
            for (int j = 2; j < 4; j++)
                assertTrue(table[i][j] instanceof JToggleButton);

    }

    @Test
    public void columnOrderTest()
    {
        for (int i = 0; i < table.length; i++)
        {
            final JComponent[] row = table[i];

            assertEquals(((AbstractButton) row[0]).getText(), MultiLayerControlPanel.UP_COLUMN);
            assertEquals(((AbstractButton) row[1]).getText(), MultiLayerControlPanel.DOWN_COLUMN);
            assertEquals(((AbstractButton) row[2]).getText(), panel.getLayer(i).getName());
            assertNotNull(((AbstractButton) row[3]).getIcon());
        }
    }

    @Test
    public void rowAssociationTest()
    {
        for (int i = 0; i < table.length; i++)
            assertNotNull(panel.getLayer(i));
    }

    @Test
    @Parameters(named = "activeLayerButtons")
    public void activeLayerTest(int componentIndex, JComponent component)
    {
        // Mock visualization state
        doNothing().when(vs).setCanvasLayerVisibility(any(NetworkLayer.class), anyBoolean());

        final JToggleButton button = (JToggleButton) component;
        button.doClick();

        final NetworkLayer layer = panel.getLayer(componentIndex);
        assertNotNull(layer);

        verify(callback.getVisualizationState()).setCanvasLayerVisibility(layer, true);

        assertThat(layer.isDefaultLayer()).isTrue();
    }

    @NamedParameters("activeLayerButtons")
    private final Object[] getDefaultLayerButtons()
    {
        GenericTypeMatcher<JToggleButton> matcher = new GenericTypeMatcher<JToggleButton>(JToggleButton.class)
        {
            @Override
            protected boolean isMatching(JToggleButton component)
            {
                return component.getName().equals(MultiLayerControlPanel.ACTIVE_COLUMN);
            }
        };

        List<Object[]> componentList = new ArrayList<>();

        for (int i = 0; i < table.length; i++)
        {
            final JComponent component = table[i][2];
            if (matcher.matches(component)) componentList.add(new Object[]{i, component});
        }

        return componentList.toArray();
    }

    @Test
    @Parameters(named = "visibilityButtons")
    public void visibilityButtonTest(int componentIndex, JComponent component)
    {
        // Mock visualization state
        doNothing().when(vs).setCanvasLayerVisibility(any(NetworkLayer.class), anyBoolean());

        final JToggleButton button = (JToggleButton) component;
        button.doClick();

        final NetworkLayer layer = panel.getLayer(componentIndex);
        assertNotNull(layer);

        verify(callback.getVisualizationState()).setCanvasLayerVisibility(layer, button.isSelected());

        assertFalse(button.isSelected());

        button.doClick();

        assertTrue(button.isSelected());
    }

    @NamedParameters("visibilityButtons")
    private final Object[] getVisibilityButtons()
    {
        GenericTypeMatcher<JToggleButton> matcher = new GenericTypeMatcher<JToggleButton>(JToggleButton.class)
        {
            @Override
            protected boolean isMatching(JToggleButton component)
            {
                return component.getName().equals(MultiLayerControlPanel.VISIBLE_COLUMN);
            }
        };

        List<Object[]> componentList = new ArrayList<>();
        for (int i = 0; i < table.length; i++)
        {
            final JComponent component = table[i][3];
            if (matcher.matches(component))
                if (component.isEnabled())
                    componentList.add(new Object[]{i, component});

        }

        return componentList.toArray();
    }

    @Test
    public void moveButtonAvailabilityTest()
    {
        assertFalse(table[0][0].isEnabled());
        assertFalse(table[table.length - 1][1].isEnabled());
    }

    @Test
    public void visibilityButtonAvailabilityTest()
    {
        final int i = panel.getLayerIndex(netPlan.getNetworkLayerDefault());
        assertTrue(((JToggleButton) table[i][2]).isSelected());
        assertFalse(table[i][3].isEnabled());
    }
}