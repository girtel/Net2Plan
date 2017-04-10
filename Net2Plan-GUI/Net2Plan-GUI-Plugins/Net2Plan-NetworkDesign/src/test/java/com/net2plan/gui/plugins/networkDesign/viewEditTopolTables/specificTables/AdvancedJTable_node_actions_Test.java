/*
 * ******************************************************************************
 *  * Copyright (c) 2017 Pablo Pavon-Marino.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the GNU Lesser Public License v3.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.gnu.org/licenses/lgpl.html
 *  *
 *  * Contributors:
 *  *     Pablo Pavon-Marino - Jose-Luis Izquierdo-Zaragoza, up to version 0.3.1
 *  *     Pablo Pavon-Marino - from version 0.4.0 onwards
 *  *     Pablo Pavon Marino - Jorge San Emeterio Villalain, from version 0.4.1 onwards
 *  *****************************************************************************
 */

package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.specificTables;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jorge San Emeterio Villalain
 * @date 10/04/17
 */
public class AdvancedJTable_node_actions_Test
{
    private static GUINetworkDesign networkDesign;

    private static NetPlan netPlan;
    private static List<Node> selection;

    @BeforeClass
    public static void setUp()
    {
        networkDesign = new GUINetworkDesign();
        networkDesign.configure(new JPanel());

        netPlan = new NetPlan();

        final Node node1 = netPlan.addNode(0, 0, "Node 1", null);
        final Node node2 = netPlan.addNode(0, 0, "Node 2", null);
        netPlan.addNode(0, 0, "Node 3", null);
        netPlan.addNode(0, 0, "Node 4", null);

        selection = new ArrayList<>();
        selection.add(node1);
        selection.add(node2);

        networkDesign.setCurrentNetPlanDoNotUpdateVisualization(netPlan);
        networkDesign.updateVisualizationAfterNewTopology();
    }

    @Test
    public void showNodeTest()
    {
        for (Node node : selection)
            networkDesign.getVisualizationState().hideOnCanvas(node);

        final JMenuItem showSelection = new AdvancedJTable_node.ShowSelectionMenuItem(networkDesign, selection);
        showSelection.doClick();

        for (Node node : selection)
            Assert.assertFalse(networkDesign.getVisualizationState().isHiddenOnCanvas(node));
    }

    @Test
    public void hideNodeTest()
    {
        for (Node node : selection)
            networkDesign.getVisualizationState().showOnCanvas(node);

        final JMenuItem hideItem = new AdvancedJTable_node.HideSelectionMenuItem(networkDesign, selection);
        hideItem.doClick();

        for (Node node : selection)
            Assert.assertTrue(networkDesign.getVisualizationState().isHiddenOnCanvas(node));
    }
}