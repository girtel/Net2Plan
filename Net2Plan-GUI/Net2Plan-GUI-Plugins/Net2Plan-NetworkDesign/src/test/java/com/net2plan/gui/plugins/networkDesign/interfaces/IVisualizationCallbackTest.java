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

package com.net2plan.gui.plugins.networkDesign.interfaces;

import com.net2plan.gui.plugins.GUINetworkDesign;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.assertNotNull;

/**
 * @author Jorge San Emeterio Villalain
 * @date 3/04/17
 */
public class IVisualizationCallbackTest
{
    private static IVisualizationCallback callback;

    @BeforeClass
    public static void setUp()
    {
        callback = new GUINetworkDesign();
        ((GUINetworkDesign) callback).configure(new JPanel());
    }

    @Test
    public void getDesignTest()
    {
        assertNotNull(callback.getDesign());
    }
}