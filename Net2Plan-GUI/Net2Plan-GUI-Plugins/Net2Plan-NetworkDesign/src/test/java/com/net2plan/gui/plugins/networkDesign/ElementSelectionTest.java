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

package com.net2plan.gui.plugins.networkDesign;

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants.NetworkElementType;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Jorge San Emeterio Villalain
 * @date 28/03/17
 */
public class ElementSelectionTest
{
    private NetPlan netPlan;
    private List<NetworkElement> networkElements;
    private ElementSelection elementSelection;

    @Before
    public void setUp()
    {
        elementSelection = null;
        networkElements = new ArrayList<>();

        netPlan = new NetPlan();

        netPlan.addNode(0, 0, "Node 1", null);
        netPlan.addNode(0, 0, "Node 2", null);
        netPlan.addNode(0, 0, "Node 3", null);

        netPlan.addLink(netPlan.getNodeByName("Node 1"), netPlan.getNodeByName("Node 2"), 0, 0, 1e3, null);
        netPlan.addLink(netPlan.getNodeByName("Node 1"), netPlan.getNodeByName("Node 2"), 0, 0, 1e3, null);

        netPlan.addDemand(netPlan.getNodeByName("Node 1"), netPlan.getNodeByName("Node 2"), 0, null);
    }

    @Test
    public void invertTest()
    {
        final List<Node> selectedNodes = new ArrayList<>();
        selectedNodes.add(netPlan.getNodeByName("Node 1"));
        selectedNodes.add(netPlan.getNodeByName("Node 2"));
        elementSelection = new ElementSelection(NetworkElementType.NODE, selectedNodes);

        final ElementSelection invertedSelection = this.elementSelection.invertSelection();

        assertNotNull(invertedSelection);
        assertEquals(invertedSelection.getElementType(), NetworkElementType.NODE);

        final List<? extends NetworkElement> networkElements = invertedSelection.getNetworkElements();

        assertArrayEquals(new Node[] {netPlan.getNodeByName("Node 3")}, networkElements.toArray());
    }

    @Test
    public void invertEmptySelectionTest()
    {
        elementSelection = new ElementSelection();
        final ElementSelection invertSelection = this.elementSelection.invertSelection();

        assertNull(invertSelection);
    }

    @Test
    public void invertInvalidElementTypeTest()
    {
        elementSelection = new ElementSelection(NetworkElementType.LAYER, netPlan.getNetworkLayers());
        final ElementSelection invertSelection = this.elementSelection.invertSelection();

        assertNull(invertSelection);
    }

    @Test
    public void buildEmptySelection()
    {
        elementSelection = new ElementSelection();

        assertTrue(elementSelection.getNetworkElements().isEmpty());
        assertTrue(elementSelection.getForwardingRules().isEmpty());
    }
}