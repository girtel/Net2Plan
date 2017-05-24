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

import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Jorge San Emeterio Villalain
 * @date 6/04/17
 */
public class TBFTagBasedTest
{
    private static NetPlan netPlan;
    private static TBFTagBased filter;

    private static Link link0, link1;
    private static Link link10, link11;

    private static NetworkLayer layer0, layer1;

    @BeforeClass
    public static void setUp()
    {
        netPlan = new NetPlan();
        layer0 = netPlan.getNetworkLayer("Layer 0");
        layer1 = netPlan.addLayer("Layer 1", "", "", "", null, null);

        final Node node1 = netPlan.addNode(0, 0, "Node 1", null);
        final Node node2 = netPlan.addNode(0, 0, "Node 2", null);
        final Node node3 = netPlan.addNode(0, 0, "Node 3", null);

        link0 = netPlan.addLink(node1, node2, 0, 0, 1e3, null);
        link1 = netPlan.addLink(node2, node3, 0, 0, 1e3, null);

        link0.addTag("Hello");
        link1.addTag("Hello");
        link1.addTag("World");

        netPlan.setNetworkLayerDefault(netPlan.getNetworkLayer("Layer 1"));

        final Node node10 = netPlan.addNode(0, 0, "Node 10", null);
        final Node node11 = netPlan.addNode(0, 0, "Node 11", null);

        link10 = netPlan.addLink(node10, node11, 0, 0, 1e3, null);
        link11 = netPlan.addLink(node11, node10, 0, 0, 1e3, null);

        link10.addTag("How");
        link10.addTag("You");
        link11.addTag("Hello");
        link11.addTag("How");

        netPlan.setNetworkLayerDefault(netPlan.getNetworkLayer("Layer 0"));
    }

    @Test
    public void layerFilterContainsTagTest()
    {
        filter = new TBFTagBased(netPlan, layer0, "World", "");

        final Link[] links = {link1};

        Assert.assertArrayEquals(links, filter.getVisibleLinks(layer0).toArray());
    }

    @Test
    public void layerFilterNotContainsTagTest()
    {
        filter = new TBFTagBased(netPlan, layer0, "", "Hello");

        final Link[] links = {};

        Assert.assertArrayEquals(links, filter.getVisibleLinks(layer0).toArray());
    }

    @Test
    public void layerFilterCombinationTest()
    {
        filter = new TBFTagBased(netPlan, netPlan.getNetworkLayer("Layer 0"), "Hello", "World");

        final Link[] links = {link0};

        Assert.assertArrayEquals(links, filter.getVisibleLinks(layer0).toArray());
    }

    @Test
    public void allLayersFilterContainsTagTest()
    {
        filter = new TBFTagBased(netPlan, null, "Hello", "");

        final Link[] links0 = {link0, link1};
        final Link[] links1 = {link11};

        Assert.assertArrayEquals(links0, filter.getVisibleLinks(layer0).toArray());
        Assert.assertArrayEquals(links1, filter.getVisibleLinks(layer1).toArray());
    }

    @Test
    public void allLayersFilterNotContainsTagTest()
    {
        filter = new TBFTagBased(netPlan, null, "", "Hello");

        final Link[] links0 = {};
        final Link[] links1 = {link10};

        Assert.assertArrayEquals(links0, filter.getVisibleLinks(layer0).toArray());
        Assert.assertArrayEquals(links1, filter.getVisibleLinks(layer1).toArray());
    }

    @Test
    public void allLayersFilterCombinationTest()
    {
        filter = new TBFTagBased(netPlan, null, "Hello", "World");

        final Link[] links0 = {link0};
        final Link[] links1 = {link11};

        Assert.assertArrayEquals(links0, filter.getVisibleLinks(layer0).toArray());
        Assert.assertArrayEquals(links1, filter.getVisibleLinks(layer1).toArray());
    }


}