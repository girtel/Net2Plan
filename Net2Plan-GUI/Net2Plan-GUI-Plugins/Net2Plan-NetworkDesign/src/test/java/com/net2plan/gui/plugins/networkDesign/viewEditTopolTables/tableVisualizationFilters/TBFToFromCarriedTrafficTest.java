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
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Jorge San Emeterio Villalain
 * @date 6/04/17
 */
public class TBFToFromCarriedTrafficTest
{
    private static NetPlan netPlan;
    private static NetworkLayer layer;
    private static ElementSelection selection;
    private static TBFToFromCarriedTraffic filter;

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
    public void filterTest()
    {

    }
}