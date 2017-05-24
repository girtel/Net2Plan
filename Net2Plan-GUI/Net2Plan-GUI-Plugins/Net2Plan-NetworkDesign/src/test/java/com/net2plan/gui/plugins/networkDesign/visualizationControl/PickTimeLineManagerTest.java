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

package com.net2plan.gui.plugins.networkDesign.visualizationControl;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.utils.Pair;
import org.junit.Before;
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

    @Before
    public void setUp()
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
        timeLineManager.addElement(netPlan, netPlan.getNode(0));
        timeLineManager.addElement(netPlan, netPlan.getNode(1));
        timeLineManager.addElement(netPlan, netPlan.getNode(2));

        netPlan.getNode(1).remove();

        assertEquals(netPlan.getNode(0), timeLineManager.getPickNavigationBackElement());
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
        final Link link = netPlan.addLink(netPlan.getNode(0), netPlan.getNode(1), 0, 0, 1e3, null);
        final Demand demand = netPlan.addDemand(netPlan.getNode(0), netPlan.getNode(1), 0, null);

        final Pair<Demand, Link> forwardingRule = Pair.unmodifiableOf(demand, link);

        timeLineManager.addElement(netPlan, netPlan.getNode(0));
        timeLineManager.addElement(netPlan, forwardingRule);

        assertEquals(netPlan.getNode(0), timeLineManager.getPickNavigationBackElement());
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

        timeLineManager.getPickNavigationBackElement();

        assertEquals(netPlan.getNode(0), timeLineManager.getPickNavigationBackElement());

        timeLineManager.getPickNavigationForwardElement();

        assertEquals(netPlan.getNode(2), timeLineManager.getPickNavigationForwardElement());
    }

    @Test
    public void repeatAdditionForwardingRule()
    {
        final Link link = netPlan.addLink(netPlan.getNode(0), netPlan.getNode(1), 0, 0, 1e3, null);
        final Demand demand = netPlan.addDemand(netPlan.getNode(0), netPlan.getNode(1), 0, null);

        final Pair<Demand, Link> forwardingRule = Pair.unmodifiableOf(demand, link);

        timeLineManager.addElement(netPlan, forwardingRule);
        timeLineManager.addElement(netPlan, forwardingRule);

        assertEquals(null, timeLineManager.getPickNavigationBackElement());
        assertEquals(null, timeLineManager.getPickNavigationForwardElement());
    }
}