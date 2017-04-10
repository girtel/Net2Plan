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
import com.net2plan.gui.plugins.networkDesign.ElementSelection;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author Jorge San Emeterio Villalain
 * @date 10/04/17
 */
public class AdvancedJTable_node_actions_Test
{
    private static AdvancedJTable_node nodeTable;
    private static List<JComponent> forcedList;

    private static GUINetworkDesign networkDesign;
    private static NetPlan netPlan;

    private static ElementSelection selection;

    @BeforeClass
    public static void setUp()
    {
        networkDesign = new GUINetworkDesign();
        networkDesign.configure(new JPanel());

        netPlan = new NetPlan();
        final Node node1 = netPlan.addNode(0, 0, "Node 1", null);
        final Node node2 = netPlan.addNode(0, 0, "Node 2", null);
        final Node node3 = netPlan.addNode(0, 0, "Node 3", null);

        nodeTable = new AdvancedJTable_node(networkDesign);

        selection = new ElementSelection(Constants.NetworkElementType.NODE, Arrays.asList(node1, node2));

        forcedList = nodeTable.getForcedOptions(selection);
    }

    @Test
    public void showNodeTest()
    {
    }
}