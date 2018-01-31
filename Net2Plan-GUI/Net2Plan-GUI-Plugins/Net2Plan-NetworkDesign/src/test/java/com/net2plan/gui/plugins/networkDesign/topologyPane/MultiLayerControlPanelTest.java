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
import com.net2plan.utils.Pair;
import junitparams.JUnitParamsRunner;
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

	@Before
	public void setUp()
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
		{
			for (int j = 0; j < 2; j++)
			{
				assertTrue(table[i][j] instanceof JButton);
			}
		}

		for (int i = 0; i < table.length; i++)
		{
			for (int j = 2; j < 4; j++)
			{
				assertTrue(table[i][j] instanceof JToggleButton);
			}
		}

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
		{
			assertNotNull(panel.getLayer(i));
		}
	}

	@Test
	public void activeLayerTest()
	{
		// Mock visualization state
		doNothing().when(vs).setCanvasLayerVisibility(any(NetworkLayer.class), anyBoolean());

		List<Pair<Integer, JToggleButton>> toggleButtons = this.getDefaultLayerButtons();

		for (Pair<Integer, JToggleButton> pair : toggleButtons)
		{
			JToggleButton layerButton = pair.getSecond();

			layerButton.doClick();

			final NetworkLayer layer = panel.getLayer(pair.getFirst());
			assertNotNull(layer);

			verify(callback.getVisualizationState()).setCanvasLayerVisibility(layer, true);

			assertThat(layer.isDefaultLayer()).isTrue();
		}

	}

	private final List<Pair<Integer, JToggleButton>> getDefaultLayerButtons()
	{
		GenericTypeMatcher<JToggleButton> matcher = new GenericTypeMatcher<JToggleButton>(JToggleButton.class)
		{
			@Override
			protected boolean isMatching(JToggleButton component)
			{
				return component.getName().equals(MultiLayerControlPanel.ACTIVE_COLUMN);
			}
		};

		List<Pair<Integer, JToggleButton>> componentList = new ArrayList<>();

		for (int i = 0; i < table.length; i++)
		{
			final JToggleButton component = (JToggleButton) table[i][2];
			if (matcher.matches(component))
			{
				componentList.add(Pair.of(i, component));
			}
		}

		return componentList;
	}

	@Test
	public void visibilityButtonTest()
	{
		// Mock visualization state
		doNothing().when(vs).setCanvasLayerVisibility(any(NetworkLayer.class), anyBoolean());

		List<Pair<Integer, JToggleButton>> visibilityButtons = this.getVisibilityButtons();

		for (Pair<Integer, JToggleButton> pair : visibilityButtons)
		{
			JToggleButton visibilityButton = pair.getSecond();
			visibilityButton.doClick();

			final NetworkLayer layer = panel.getLayer(pair.getFirst());
			assertNotNull(layer);

			verify(callback.getVisualizationState()).setCanvasLayerVisibility(layer, visibilityButton.isSelected());

			assertFalse(visibilityButton.isSelected());

			visibilityButton.doClick();

			assertTrue(visibilityButton.isSelected());
		}
	}

	private List<Pair<Integer, JToggleButton>> getVisibilityButtons()
	{
		GenericTypeMatcher<JToggleButton> matcher = new GenericTypeMatcher<JToggleButton>(JToggleButton.class)
		{
			@Override
			protected boolean isMatching(JToggleButton component)
			{
				return component.getName().equals(MultiLayerControlPanel.VISIBLE_COLUMN);
			}
		};

		List<Pair<Integer, JToggleButton>> componentList = new ArrayList<>();
		for (int i = 0; i < table.length; i++)
		{
			final JToggleButton component = (JToggleButton) table[i][3];
			if (matcher.matches(component))
			{
				if (component.isEnabled())
				{
					componentList.add(Pair.of(i, component));
				}
			}
		}

		return componentList;
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