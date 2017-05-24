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
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Jorge San Emeterio Villalain
 * @date 17/04/17
 */
@RunWith(MockitoJUnitRunner.class)
public class AdvancedJTable_link_actionsTest
{
    private static final double DELTA = 1e-15;

    @Mock
    private static GUINetworkDesign networkDesign;

    private static VisualizationState vs;

    private static List<Link> selection;

    private static NetPlan netPlan;

    @Before
    public void setUp()
    {
        netPlan = new NetPlan();

        final Node node1 = netPlan.addNode(0, 0, "Node 1", null);
        final Node node2 = netPlan.addNode(0, 0, "Node 2", null);

        final Link link = netPlan.addLink(node1, node2, 0, 0, 1e3, null);

        selection = new ArrayList<>();
        selection.add(link);

        vs = new VisualizationState(netPlan, null, null, 0);

        when(networkDesign.getDesign()).thenReturn(netPlan);
        when(networkDesign.getVisualizationState()).thenReturn(vs);
    }

    @Test
    public void removeButtonTest()
    {
        for (Link link : selection)
            assertNotNull(netPlan.getLinkFromId(link.getId()));

        final JMenuItem removeButton = new AdvancedJTable_link.MenuItem_RemoveLinks(networkDesign, selection);
        removeButton.doClick();

        for (Link link : selection)
            assertNull(netPlan.getLinkFromId(link.getId()));
    }

    @Test
    public void hideButtonTest()
    {
        for (Link link : selection)
            assertFalse(networkDesign.getVisualizationState().isHiddenOnCanvas(link));

        final JMenuItem hideButton = new AdvancedJTable_link.MenuItem_HideLinks(networkDesign, selection);
        hideButton.doClick();

        for (Link link : selection)
            assertTrue(networkDesign.getVisualizationState().isHiddenOnCanvas(link));
    }

    @Test
    public void showButtonTest()
    {
        for (Link link : selection)
            networkDesign.getVisualizationState().hideOnCanvas(link);

        for (Link link : selection)
            assertTrue(networkDesign.getVisualizationState().isHiddenOnCanvas(link));

        final JMenuItem showButton = new AdvancedJTable_link.MenuItem_ShowLinks(networkDesign, selection);
        showButton.doClick();

        for (Link link : selection)
            assertFalse(networkDesign.getVisualizationState().isHiddenOnCanvas(link));
    }

    @Test
    public void decoupleLinksButtonTest()
    {
        final NetworkLayer networkLayer = netPlan.addLayer("Layer 1", "", "", "", null, null);

        for (Link link : selection)
            link.coupleToNewDemandCreated(networkLayer);

        for (Link link : selection)
            assertTrue(link.isCoupled());

        final JMenuItem decoupleButton = new AdvancedJTable_link.MenuItem_DecoupleLinks(networkDesign, selection);
        decoupleButton.doClick();

        for (Link link : selection)
            assertFalse(link.isCoupled());
    }

    @Test
    public void lengthToEuclideanTest()
    {
        final JMenuItem euclideanLength = new AdvancedJTable_link.MenuItem_LengthToEuclidean(networkDesign, selection);
        euclideanLength.doClick();

        for (Link link : selection)
        {
            final Node originNode = link.getOriginNode();
            final Node destinationNode = link.getDestinationNode();

            final double euclideanDistance = netPlan.getNodePairEuclideanDistance(originNode, destinationNode);

            assertEquals(euclideanDistance, link.getLengthInKm(), DELTA);
        }
    }

    @Test
    public void lengthToHaversineTest()
    {
        final JMenuItem haversineButton = new AdvancedJTable_link.MenuItem_LengthToHaversine(networkDesign, selection);
        haversineButton.doClick();

        for (Link link : selection)
        {
            final Node originNode = link.getOriginNode();
            final Node destinationNode = link.getDestinationNode();

            final double distance = netPlan.getNodePairHaversineDistanceInKm(originNode, destinationNode);

            assertEquals(distance, link.getLengthInKm(), DELTA);
        }
    }

    @Test
    public void generateMatrixTest()
    {
        netPlan.removeAllLinks();

        final JButton meshButton = new JButton();
        meshButton.addActionListener(new AdvancedJTable_link.FullMeshTopologyActionListener(networkDesign, true));
        meshButton.doClick();

        final List<Link> links = netPlan.getLinks();
        assertTrue(links.size() == 2);

        for (Link link : links)
            assertNotNull(link.getBidirectionalPair());

        final Node node1 = netPlan.getNodeByName("Node 1");
        final Node node2 = netPlan.getNodeByName("Node 2");

        final Set<Link> pairLinks = netPlan.getNodePairLinks(node1, node2, true);
        assertFalse(pairLinks.isEmpty());
        assertTrue(pairLinks.size() == 2);
    }
}
