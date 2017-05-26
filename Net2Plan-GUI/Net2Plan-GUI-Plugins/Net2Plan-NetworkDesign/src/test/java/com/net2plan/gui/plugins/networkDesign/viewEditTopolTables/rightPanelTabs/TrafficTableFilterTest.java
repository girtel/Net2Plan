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
package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.rightPanelTabs;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.NetPlan;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.swing.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Jorge San Emeterio
 * @date 17/05/17
 */
@RunWith(MockitoJUnitRunner.class)
public class TrafficTableFilterTest
{
    @Mock
    private static GUINetworkDesign callback;

    private static NetPlanViewTableComponent_trafficMatrix component;

    private static NetPlan netPlan;

    private static final String tag = "Test1";

    @Before
    public void setUp()
    {
        netPlan = new NetPlan();

        netPlan.addNode(0, 0, "Node 0", null);
        netPlan.addNode(0, 0, "Node 1", null);

        // Mock
        when(callback.getDesign()).thenReturn(netPlan);

        component = new NetPlanViewTableComponent_trafficMatrix(callback);
    }

    @Test
    public void filterNodeTagTest()
    {
        netPlan.getNodeByName("Node 0").addTag(tag);

        component.filterByNodeTag(tag);

        final JTable table = component.getTable();

        // Size
        assertThat(table.getRowCount()).isEqualTo(2);
        assertThat(table.getColumnCount()).isEqualTo(3);

        // Content
        assertThat(table.getValueAt(0, 0)).isEqualTo("Node 0");
        assertThat(table.getValueAt(1, 0)).isNotEqualTo("Node 1");

        assertThat(table.getColumnName(1)).isEqualTo("Node 0");
        assertThat(table.getColumnName(1)).isNotEqualTo("Node 1");
    }

    @Test
    public void filterDemandTagTest()
    {
        netPlan.addNode(0, 0, "Node 2", null);
        netPlan.addDemand(netPlan.getNodeByName("Node 2"), netPlan.getNodeByName("Node 1"), 100, null).addTag(tag);

        component.filterByDemandTag(tag);

        final JTable table = component.getTable();

        // Size
        assertThat(table.getRowCount()).isEqualTo(4);
        assertThat(table.getColumnCount()).isEqualTo(5);

        // Content
        assertThat(table.getValueAt(2, 2)).isEqualTo(100d);

        for (int i = 0; i < table.getRowCount() - 1; i++)
            for (int j = 1; j < table.getColumnCount() - 1; j++)
                if (i == 2 && j == 2) continue;
                else assertThat(table.getValueAt(i, j)).isEqualTo(0d);
    }

    @Test
    public void filterLinklessNodesTest()
    {
        netPlan.addNode(0, 0, "Node 2", null);

        netPlan.addLink(netPlan.getNodeByName("Node 0"), netPlan.getNodeByName("Node 1"), 10, 10, 10, null);

        component.filterLinklessNodes(true);

        final JTable table = component.getTable();

        // Size
        assertThat(table.getRowCount()).isEqualTo(3);
        assertThat(table.getColumnCount()).isEqualTo(4);

        // Content
        assertThat(table.getValueAt(0, 0)).isEqualTo("Node 0");
        assertThat(table.getValueAt(1, 0)).isEqualTo("Node 1");
        assertThat(table.getValueAt(2, 0)).isNotEqualTo("Node 2");

        assertThat(table.getColumnName(1)).isEqualTo("Node 0");
        assertThat(table.getColumnName(2)).isEqualTo("Node 1");
        assertThat(table.getColumnName(3)).isNotEqualTo("Node 2");
    }

    @Test
    public void showLinklessNodeTest()
    {
        netPlan.addNode(0, 0, "Node 2", null);

        netPlan.addLink(netPlan.getNodeByName("Node 0"), netPlan.getNodeByName("Node 1"), 10, 10, 10, null);

        component.filterLinklessNodes(true);

        final JTable table = component.getTable();

        component.filterLinklessNodes(false);

        // Size
        assertThat(table.getRowCount()).isEqualTo(4);
        assertThat(table.getColumnCount()).isEqualTo(5);

        // Content
        assertThat(table.getValueAt(0, 0)).isEqualTo("Node 0");
        assertThat(table.getValueAt(1, 0)).isEqualTo("Node 1");
        assertThat(table.getValueAt(2, 0)).isEqualTo("Node 2");

        assertThat(table.getColumnName(1)).isEqualTo("Node 0");
        assertThat(table.getColumnName(2)).isEqualTo("Node 1");
        assertThat(table.getColumnName(3)).isEqualTo("Node 2");
    }
}