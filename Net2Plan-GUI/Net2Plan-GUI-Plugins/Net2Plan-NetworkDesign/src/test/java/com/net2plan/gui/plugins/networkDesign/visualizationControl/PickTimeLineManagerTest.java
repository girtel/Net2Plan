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

package com.net2plan.gui.plugins.networkDesign.visualizationControl;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.utils.Pair;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Jorge San Emeterio Villalain
 * @date 23/03/17
 */
public class PickTimeLineManagerTest
{
    private static NetPlan netPlan;
    private static PickTimeLineManager timeLineManager;

    @BeforeClass
    public static void setUp()
    {
        netPlan = new NetPlan();

        netPlan.addNode(0, 0, "Node 1", null);
        netPlan.addNode(0, 0, "Node 2", null);
        netPlan.addNode(0, 0, "Node 3", null);
        netPlan.addNode(0, 0, "Node 4", null);

        netPlan.checkCachesConsistency();

        timeLineManager = new PickTimeLineManager();
    }

    @Test
    public void getPickNavigationBackElement()
    {
        timeLineManager.addElement(netPlan, netPlan.getNode(0));
        timeLineManager.addElement(netPlan, netPlan.getNode(1));
        timeLineManager.addElement(netPlan, netPlan.getNode(2));

        assertEquals(netPlan.getNode(1), timeLineManager.getPickNavigationBackElement());
    }

    @Test
    public void getPickNavigationForwardElement()
    {
        timeLineManager.addElement(netPlan, netPlan.getNode(0));
        timeLineManager.addElement(netPlan, netPlan.getNode(1));
        timeLineManager.addElement(netPlan, netPlan.getNode(2));
        timeLineManager.getPickNavigationBackElement();

        assertEquals(netPlan.getNode(2), timeLineManager.getPickNavigationForwardElement());
    }

    @Test
    public void checkLowerLimit()
    {
        timeLineManager.addElement(netPlan, netPlan.getNode(0));
        timeLineManager.addElement(netPlan, netPlan.getNode(1));
        timeLineManager.getPickNavigationBackElement();

        assertEquals(null, timeLineManager.getPickNavigationBackElement());
    }

    @Test
    public void checkUpperLimit()
    {
        timeLineManager.addElement(netPlan, netPlan.getNode(0));

        assertEquals(null, timeLineManager.getPickNavigationForwardElement());
    }

    @Test(expected = RuntimeException.class)
    public void addNullNetPlan()
    {
        timeLineManager.addElement(null, netPlan.getNode(0));
    }

    @Test(expected = RuntimeException.class)
    public void addNullElement()
    {
        timeLineManager.addElement(netPlan, (NetworkElement) null);
    }

    @Test
    public void removeMiddleElement()
    {
        final NetPlan aux = netPlan.copy();

        timeLineManager.addElement(aux, aux.getNode(0));
        timeLineManager.addElement(aux, aux.getNode(1));
        timeLineManager.addElement(aux, aux.getNode(2));

        aux.getNode(1).remove();

        assertEquals(aux.getNode(0), timeLineManager.getPickNavigationBackElement());
    }

    @Test
    public void exceedLimit()
    {
        timeLineManager = new PickTimeLineManager(2);

        timeLineManager.addElement(netPlan, netPlan.getNode(0));
        timeLineManager.addElement(netPlan, netPlan.getNode(1));
        timeLineManager.addElement(netPlan, netPlan.getNode(2));

        timeLineManager.getPickNavigationBackElement();

        assertEquals(null, timeLineManager.getPickNavigationBackElement());
    }

    @Test
    public void changeCourse()
    {
        timeLineManager.addElement(netPlan, netPlan.getNode(0));
        timeLineManager.addElement(netPlan, netPlan.getNode(1));
        timeLineManager.addElement(netPlan, netPlan.getNode(2));

        timeLineManager.getPickNavigationBackElement();
        timeLineManager.getPickNavigationBackElement();

        timeLineManager.addElement(netPlan, netPlan.getNode(3));

        assertEquals(null, timeLineManager.getPickNavigationForwardElement());
        assertEquals(netPlan.getNode(0), timeLineManager.getPickNavigationBackElement());
    }

    @Test
    public void addForwardingRule()
    {
        final NetPlan aux = netPlan.copy();

        final Link link = aux.addLink(aux.getNode(0), aux.getNode(1), 0, 0, 1e3, null);
        final Demand demand = aux.addDemand(aux.getNode(0), aux.getNode(1), 0, null);

        final Pair<Demand, Link> forwardingRule = Pair.unmodifiableOf(demand, link);

        timeLineManager.addElement(aux, aux.getNode(0));
        timeLineManager.addElement(aux, forwardingRule);

        assertEquals(aux.getNode(0), timeLineManager.getPickNavigationBackElement());
        assertEquals(forwardingRule, timeLineManager.getPickNavigationForwardElement());
        assertEquals(null, timeLineManager.getPickNavigationForwardElement());
    }

    @Test
    public void repeatAddition()
    {
        // Element
        timeLineManager.addElement(netPlan, netPlan.getNode(0));
        timeLineManager.addElement(netPlan, netPlan.getNode(1));
        timeLineManager.addElement(netPlan, netPlan.getNode(1));
        timeLineManager.addElement(netPlan, netPlan.getNode(2));

        assertEquals(null, timeLineManager.getPickNavigationForwardElement());
        assertEquals(netPlan.getNode(1), timeLineManager.getPickNavigationBackElement());

        // Forwarding rule
        final NetPlan aux = netPlan.copy();

        timeLineManager = new PickTimeLineManager();

        final Link link = aux.addLink(aux.getNode(0), aux.getNode(1), 0, 0, 1e3, null);
        final Demand demand = aux.addDemand(aux.getNode(0), aux.getNode(1), 0, null);

        final Pair<Demand, Link> forwardingRule = Pair.unmodifiableOf(demand, link);

        timeLineManager.addElement(aux, forwardingRule);
        timeLineManager.addElement(aux, forwardingRule);

        assertEquals(null, timeLineManager.getPickNavigationBackElement());
        assertEquals(null, timeLineManager.getPickNavigationForwardElement());
    }
}