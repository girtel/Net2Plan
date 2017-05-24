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
package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.ElementSelection;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_node;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters.TBFSelectionBased;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.Robot;
import org.assertj.swing.fixture.JPopupMenuFixture;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static com.net2plan.internal.Constants.NetworkElementType;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jorge San Emeterio Villalain
 * @date 17/04/17
 */
@RunWith(MockitoJUnitRunner.class)
public class AdvancedJTable_networkElement_actionsTest
{
    @Mock
    private static GUINetworkDesign networkDesign;

    @Mock
    private static VisualizationState vs;

    private static NetPlan netPlan;

    private static Robot robot;

    @BeforeClass
    public static void createRobot()
    {
        robot = BasicRobot.robotWithCurrentAwtHierarchy();
    }

    @AfterClass
    public static void killRobot()
    {
        robot.cleanUp();
    }

    @Before
    public void setUp()
    {
        netPlan = new NetPlan();

        netPlan.addNode(0, 0, "Node 1", null);
        netPlan.addNode(0, 0, "Node 2", null);

        when(networkDesign.getDesign()).thenReturn(netPlan);
        when(networkDesign.getVisualizationState()).thenReturn(vs);
    }

    @Test
    public void hideFilteredTest()
    {
        final AdvancedJTable_networkElement table = new AdvancedJTable_node(networkDesign);

        final Node node1 = netPlan.getNodeByName("Node 1");
        final Node node2 = netPlan.getNodeByName("Node 2");

        final ElementSelection selection = new ElementSelection(NetworkElementType.NODE, Collections.singletonList(node2));
        final JPopupMenuFixture popUpFixture = new JPopupMenuFixture(robot, table.getPopup(selection));
        final TBFSelectionBased filter = new TBFSelectionBased(netPlan, selection);

        when(vs.getTableRowFilter()).thenReturn(filter);

        popUpFixture.menuItem("hideFilteredItem").target().doClick();

        verify(vs).hideOnCanvas(node1);
    }

    @Test
    public void removeFilteredTest()
    {
        final AdvancedJTable_networkElement table = new AdvancedJTable_node(networkDesign);

        final Node node1 = netPlan.getNodeByName("Node 1");
        final Node node2 = netPlan.getNodeByName("Node 2");

        final ElementSelection selection = new ElementSelection(NetworkElementType.NODE, Collections.singletonList(node1));
        final JPopupMenuFixture popupMenuFixture = new JPopupMenuFixture(robot, table.getPopup(selection));
        final TBFSelectionBased filter = new TBFSelectionBased(netPlan, selection);

        when(vs.getTableRowFilter()).thenReturn(filter);

        assertNotNull(netPlan.getNodeByName(node1.getName()));
        assertNotNull(netPlan.getNodeByName(node2.getName()));

        popupMenuFixture.menuItem("removeFilteredItem").target().doClick();

        assertNotNull(netPlan.getNodeByName(node1.getName()));
        assertNull(netPlan.getNodeByName(node2.getName()));
    }
}
