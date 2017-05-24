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

package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters;

import com.net2plan.gui.plugins.networkDesign.ElementSelection;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Jorge San Emeterio Villalain
 * @date 6/04/17
 */
public class TBFSelectionBasedTest
{
    private static NetPlan netPlan;
    private static NetworkLayer layer;
    private static ElementSelection selection;
    private static TBFSelectionBased filter;

    @BeforeClass
    public static void setUp()
    {
        netPlan = new NetPlan();
        layer = netPlan.getNetworkLayerDefault();

        final Node node1 = netPlan.addNode(0, 0, "Node 1", null);
        final Node node2 = netPlan.addNode(0, 0, "Node 2", null);
        final Node node3 = netPlan.addNode(0, 0, "Node 3", null);
        final Node node4 = netPlan.addNode(0, 0, "Node 4", null);

        netPlan.addLink(node1, node2, 0, 0, 1e3, null);
        netPlan.addLink(node2, node3, 0, 0, 1e3, null);
        netPlan.addLink(node3, node4, 0, 0, 1e3, null);
    }

    @Test
    public void filterNodeTest()
    {
        final List<Node> nodeList = new ArrayList<>();
        nodeList.add(netPlan.getNodeByName("Node 1"));
        nodeList.add(netPlan.getNodeByName("Node 2"));

        selection = new ElementSelection(Constants.NetworkElementType.NODE, nodeList);

        filter = new TBFSelectionBased(netPlan, selection);

        assertArrayEquals(nodeList.toArray(), filter.getVisibleNodes(layer).toArray());
        assertArrayEquals(netPlan.getLinks(layer).toArray(), filter.getVisibleLinks(layer).toArray());
    }

    @Test
    public void filterLinkTest()
    {
        final List<Link> linkList = new ArrayList<>();
        linkList.add(netPlan.getLink(0));

        selection = new ElementSelection(Constants.NetworkElementType.LINK, linkList);

        filter = new TBFSelectionBased(netPlan, selection);

        assertArrayEquals(linkList.toArray(), filter.getVisibleLinks(layer).toArray());
        assertArrayEquals(netPlan.getNodes().toArray(), filter.getVisibleNodes(layer).toArray());
    }

    @Test
    public void filterNoSelectionTest()
    {
        selection = new ElementSelection();

        filter = new TBFSelectionBased(netPlan, selection);

        assertArrayEquals(netPlan.getNodes().toArray(), filter.getVisibleNodes(layer).toArray());
        assertArrayEquals(netPlan.getLinks().toArray(), filter.getVisibleLinks(layer).toArray());
    }
}