package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_forwardingRule;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_node;
import com.net2plan.interfaces.networkDesign.NetPlan;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;

/**
 * @author Jorge San Emeterio
 * @date 24/04/17
 */
public class AdvancedJTable_networkElementTest
{
    private static AdvancedJTable_networkElement table;

    private static GUINetworkDesign networkDesign;
    private static NetPlan netPlan;

    @Before
    public void setUp()
    {
        networkDesign = new GUINetworkDesign();
        networkDesign.configure(new JPanel());

        netPlan = new NetPlan();

        networkDesign.setDesign(netPlan);
        networkDesign.updateVisualizationAfterNewTopology();
    }

    @Test
    public void hasAttributesTest()
    {
        table = new AdvancedJTable_node(networkDesign);
        Assert.assertTrue(table.hasAttributes());

        table = new AdvancedJTable_forwardingRule(networkDesign);
        Assert.assertFalse(table.hasAttributes());
    }
}
