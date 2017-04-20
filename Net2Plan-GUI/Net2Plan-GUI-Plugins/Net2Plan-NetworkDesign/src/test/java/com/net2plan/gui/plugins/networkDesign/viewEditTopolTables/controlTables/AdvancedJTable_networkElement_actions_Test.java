package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.ElementSelection;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters.TBFSelectionBased;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.util.Collections;

import static com.net2plan.internal.Constants.NetworkElementType;
import static org.junit.Assert.*;

/**
 * @author Jorge San Emeterio Villalain
 * @date 17/04/17
 */
public class AdvancedJTable_networkElement_actions_Test
{
    private static GUINetworkDesign networkDesign;

    private static NetPlan netPlan;

    private static Node node1, node2;

    @Before
    public void setUp()
    {
        networkDesign = new GUINetworkDesign();
        networkDesign.configure(new JPanel());

        netPlan = new NetPlan();

        node1 = netPlan.addNode(0, 0, "Node 1", null);
        node2 = netPlan.addNode(0, 0, "Node 2", null);

        networkDesign.setDesign(netPlan);
        networkDesign.updateVisualizationAfterNewTopology();

        final TBFSelectionBased filter = new TBFSelectionBased(netPlan, new ElementSelection(NetworkElementType.NODE, Collections.singletonList(node1)));

        networkDesign.getVisualizationState().cleanTableRowFilter();
        networkDesign.getVisualizationState().updateTableRowFilter(filter, ITableRowFilter.FilterCombinationType.INCLUDEIF_AND);
    }

    @Test
    public void hideFilteredTest()
    {
        for (Node node : new Node[]{node1, node2})
            assertFalse(networkDesign.getVisualizationState().isHiddenOnCanvas(node));

        final JMenuItem hideFilter = new AdvancedJTable_networkElement.MenuItem_HideFiltered(networkDesign, NetworkElementType.NODE);
        hideFilter.doClick();

        assertTrue(networkDesign.getVisualizationState().isHiddenOnCanvas(node2));
    }

    @Test
    public void removeFilteredTest()
    {
        assertNotNull(netPlan.getNodeByName(node1.getName()));
        assertNotNull(netPlan.getNodeByName(node2.getName()));

        final JMenuItem removeFilter = new AdvancedJTable_networkElement.MenuItem_RemovedFiltered(networkDesign, NetworkElementType.NODE);
        removeFilter.doClick();

        assertNotNull(netPlan.getNodeByName(node1.getName()));
        assertNull(netPlan.getNodeByName(node2.getName()));
    }
}
