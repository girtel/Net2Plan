package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.rightPanelTabs;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import org.junit.Before;
import org.junit.BeforeClass;
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

    @BeforeClass
    public static void prepareTopology()
    {
        netPlan = new NetPlan();

        Node node0 = netPlan.addNode(0, 0, "Node 0", null);
        Node node1 = netPlan.addNode(0, 0, "Node 1", null);

        netPlan.addDemand(node0, node1, 100, null);
    }

    @Before
    public void setUp()
    {
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
    }
}