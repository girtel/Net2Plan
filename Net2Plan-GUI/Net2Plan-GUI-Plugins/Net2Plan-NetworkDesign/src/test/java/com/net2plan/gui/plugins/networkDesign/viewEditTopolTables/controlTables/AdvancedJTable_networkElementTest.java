package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_forwardingRule;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;

/**
 * @author Jorge San Emeterio
 * @date 24/04/17
 */
@RunWith(MockitoJUnitRunner.class)
public class AdvancedJTable_networkElementTest
{
    private static AdvancedJTable_networkElement table;

    @Mock
    private static GUINetworkDesign networkDesign = mock(GUINetworkDesign.class);

    @Test
    public void forwardingRuleNoAttributesTest()
    {
        table = new AdvancedJTable_forwardingRule(networkDesign);
        Assert.assertFalse(table.hasAttributes());
    }
}
