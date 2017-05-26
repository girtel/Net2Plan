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

package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Jorge San Emeterio Villalain
 * @date 10/04/17
 */
@RunWith(MockitoJUnitRunner.class)
public class AdvancedJTable_node_actions_Test
{
    @Mock
    private static GUINetworkDesign networkDesign;

    private static VisualizationState vs;

    private static NetPlan netPlan;
    private static List<Node> selection;

    @Before
    public void setUp()
    {
        netPlan = new NetPlan();

        final Node node1 = netPlan.addNode(0, 0, "Node 1", null);
        final Node node2 = netPlan.addNode(0, 0, "Node 2", null);

        selection = new ArrayList<>();
        selection.add(node1);
        selection.add(node2);

        vs = new VisualizationState(netPlan, null, null, 0);

        when(networkDesign.getDesign()).thenReturn(netPlan);
        when(networkDesign.getVisualizationState()).thenReturn(vs);
    }

    @Test
    public void showNodeTest()
    {
        for (Node node : selection)
            networkDesign.getVisualizationState().hideOnCanvas(node);

        final JMenuItem showSelection = new AdvancedJTable_node.MenuItem_ShowSelection(networkDesign, selection);
        showSelection.doClick();

        for (Node node : selection)
            assertFalse(networkDesign.getVisualizationState().isHiddenOnCanvas(node));
    }

    @Test
    public void hideNodeTest()
    {
        for (Node node : selection)
            networkDesign.getVisualizationState().showOnCanvas(node);

        final JMenuItem hideItem = new AdvancedJTable_node.MenuItem_HideSelection(networkDesign, selection);
        hideItem.doClick();

        for (Node node : selection)
            assertTrue(networkDesign.getVisualizationState().isHiddenOnCanvas(node));
    }

    @Test
    public void switchCoordinatesTest()
    {
        final List<Point2D> expected = new ArrayList<>();
        for (Node node : selection)
        {
            final Point2D point = node.getXYPositionMap();
            expected.add(new Point2D.Double(point.getY(), point.getX()));
        }

        final JMenuItem switchCoordinates = new AdvancedJTable_node.MenuItem_SwitchCoordinates(networkDesign, selection);
        switchCoordinates.doClick();

        final List<Point2D> result = new ArrayList<>();
        for (Node node : selection)
            result.add(node.getXYPositionMap());

        assertArrayEquals(expected.toArray(), result.toArray());
    }

    @Test
    public void removeNodesTest()
    {
        for (Node node : selection)
            assertNotNull(netPlan.getNodeFromId(node.getId()));

        final JMenuItem removeSelected = new AdvancedJTable_node.MenuItem_RemoveNodes(networkDesign, selection);
        removeSelected.doClick();

        for (Node node : selection)
            assertNull(netPlan.getNodeFromId(node.getId()));
    }

    @Test
    public void addNodeTest()
    {
        int numberOfNodes = netPlan.getNumberOfNodes();

        final JMenuItem addNode = new AdvancedJTable_node.MenuItem_AddNode(networkDesign);
        addNode.doClick();

        assertEquals(numberOfNodes + 1, netPlan.getNumberOfNodes());
    }
}