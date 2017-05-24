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
import com.net2plan.interfaces.networkDesign.Node;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.ComponentLookupScope;
import org.assertj.swing.core.Robot;
import org.assertj.swing.fixture.JPanelFixture;
import org.junit.AfterClass;
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
 * @date 19/05/17
 */
@RunWith(MockitoJUnitRunner.class)
public class TrafficTablePanelTest
{
    @Mock
    private static GUINetworkDesign callback;

    private static Robot robot;

    private static NetPlan netPlan;
    private static NetPlanViewTableComponent_trafficMatrix component;

    @Before
    public void setUp()
    {
        // NetPlan
        netPlan = new NetPlan();

        final Node node0 = netPlan.addNode(0, 0, "Node 0", null);
        final Node node1 = netPlan.addNode(0, 0, "Node 1", null);
        final Node node2 = netPlan.addNode(0, 0, "Node 2", null);

        netPlan.addDemand(node0, node1, 1, null);
        netPlan.addDemand(node0, node1, 2, null);
        netPlan.addDemand(node1, node0, 3, null);
        netPlan.addDemand(node0, node2, 4, null);

        // Mock
        when(callback.getDesign()).thenReturn(netPlan);

        component = new NetPlanViewTableComponent_trafficMatrix(callback);
    }

    @BeforeClass
    public static void buildRobot()
    {
        robot = BasicRobot.robotWithCurrentAwtHierarchy();
        robot.settings().componentLookupScope(ComponentLookupScope.ALL);
    }

    @AfterClass
    public static void killRobot()
    {
        robot.cleanUp();
    }

    @Test
    public void applyButtonStateTest()
    {
        final JPanelFixture panelFixture = new JPanelFixture(robot, component);

        final JButton normalizationApply = panelFixture.button("normalizationApply").target();
        final JComboBox normalizationWheel = panelFixture.comboBox("normalizationWheel").target();

        final JButton trafficModelApply = panelFixture.button("trafficModelApply").target();
        final JComboBox trafficModelWheel = panelFixture.comboBox("trafficModelWheel").target();

        for (int i = 0; i < normalizationWheel.getItemCount(); i++)
        {
            normalizationWheel.setSelectedIndex(i);
            assertThat(!normalizationApply.isEnabled()).isEqualTo(normalizationWheel.getSelectedIndex() == 0);
        }

        for (int i = 0; i < trafficModelWheel.getItemCount(); i++)
        {
            trafficModelWheel.setSelectedIndex(i);
            assertThat(!trafficModelApply.isEnabled()).isEqualTo(trafficModelWheel.getSelectedIndex() == 0);
        }
    }

    @Test
    public void getTrafficMatrixTest()
    {
        final double[][] trafficMatrix = component.getTrafficMatrix();

        // Is square
        for (int i = 0; i < trafficMatrix.length; i++)
            assertThat(trafficMatrix[i].length).isEqualTo(trafficMatrix.length);

        // Size
        assertThat(trafficMatrix.length).isEqualTo(netPlan.getNumberOfNodes());

        // Content
        for (int i = 0; i < trafficMatrix.length; i++)
        {
            for (int j = 0; j < trafficMatrix[i].length; j++)
            {
                assertThat(trafficMatrix[i][j]).isInstanceOf(Double.class);

                final double offTraffic = netPlan.getNodePairDemands(netPlan.getNode(i), netPlan.getNode(j), false)
                        .stream()
                        .mapToDouble(e -> e.getOfferedTraffic())
                        .sum();

                assertThat(trafficMatrix[i][j]).isEqualTo(offTraffic);
            }
        }
    }
}