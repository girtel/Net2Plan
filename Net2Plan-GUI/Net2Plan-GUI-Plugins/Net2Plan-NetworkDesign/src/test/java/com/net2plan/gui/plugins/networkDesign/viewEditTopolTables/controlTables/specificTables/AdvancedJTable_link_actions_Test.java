package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Jorge San Emeterio Villalain
 * @date 17/04/17
 */
public class AdvancedJTable_link_actions_Test
{
    private static GUINetworkDesign networkDesign;
    private static List<Link> selection;

    private static NetPlan netPlan;

    @Before
    public void setUp()
    {
        networkDesign = new GUINetworkDesign();
        networkDesign.configure(new JPanel());

        netPlan = new NetPlan();

        final Node node1 = netPlan.addNode(0, 0, "Node 1", null);
        final Node node2 = netPlan.addNode(0, 0, "Node 2", null);

        final Link link = netPlan.addLink(node1, node2, 0, 0, 1e3, null);

        selection = new ArrayList<>();
        selection.add(link);

        networkDesign.setCurrentNetPlanDoNotUpdateVisualization(netPlan);
        networkDesign.updateVisualizationAfterNewTopology();
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
    public void decoupleLinksButton()
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
}
