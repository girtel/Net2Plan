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

import com.net2plan.interfaces.networkDesign.NetPlan;
import org.junit.BeforeClass;
import org.junit.Test;

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
    public void getPickNavigationBackElement() throws Exception
    {
        throw new UnsupportedOperationException("");
    }

    @Test
    public void getPickNavigationForwardElement() throws Exception
    {
        throw new UnsupportedOperationException("");
    }

    @Test
    public void addElement() throws Exception
    {
        throw new UnsupportedOperationException("");
    }

    @Test
    public void addForwardingRule() throws Exception
    {
        throw new UnsupportedOperationException("");
    }

}