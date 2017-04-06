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

        assertArrayEquals(filter.getVisibleNodes(layer).toArray(), nodeList.toArray());
        assertArrayEquals(filter.getVisibleLinks(layer).toArray(), netPlan.getLinks(layer).toArray());
    }

    @Test
    public void filterLinkTest()
    {
        final List<Link> linkList = new ArrayList<>();
        linkList.add(netPlan.getLink(0));

        selection = new ElementSelection(Constants.NetworkElementType.LINK, linkList);

        filter = new TBFSelectionBased(netPlan, selection);

        assertArrayEquals(filter.getVisibleLinks(layer).toArray(), linkList.toArray());
        assertArrayEquals(filter.getVisibleNodes(layer).toArray(), netPlan.getNodes().toArray());
    }

    @Test
    public void filterNoSelectionTest()
    {
        selection = new ElementSelection();

        filter = new TBFSelectionBased(netPlan, selection);

        assertArrayEquals(filter.getVisibleNodes(layer).toArray(), netPlan.getNodes().toArray());
        assertArrayEquals(filter.getVisibleLinks(layer).toArray(), netPlan.getLinks().toArray());
    }
}