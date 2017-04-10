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

package com.net2plan.gui.plugins;

import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.utils.Pair;
import org.apache.commons.collections15.BidiMap;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.*;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Jorge San Emeterio Villalain
 * @date 3/04/17
 */
public class GUINetworkDesignTest
{
    private static GUINetworkDesign callback;

    @BeforeClass
    public static void setUp()
    {
        callback = new GUINetworkDesign();
        callback.configure(new JPanel());

        final NetPlan netPlan = new NetPlan();
        callback.setDesign(netPlan);
        final Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> pair = VisualizationState.generateCanvasDefaultVisualizationLayerInfo(netPlan);
        callback.getVisualizationState().setCanvasLayerVisibilityAndOrder(callback.getDesign(), pair.getFirst(), pair.getSecond());
    }

    @Test
    public void getDesignTest()
    {
        assertNotNull(callback.getDesign());
    }

    @Test
    public void getVisualizationStateTest()
    {
        assertNotNull(callback.getVisualizationState());
    }

    @Test
    public void setDesignTest()
    {
        final NetPlan netPlan = new NetPlan();
        callback.setDesign(netPlan);
        assertEquals(netPlan, callback.getDesign());
    }
}