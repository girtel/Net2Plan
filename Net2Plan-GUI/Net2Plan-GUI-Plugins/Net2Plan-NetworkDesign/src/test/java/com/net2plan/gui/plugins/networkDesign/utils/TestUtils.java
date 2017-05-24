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
package com.net2plan.gui.plugins.networkDesign.utils;

import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.ComponentLookupScope;
import org.assertj.swing.core.Robot;

/**
 * @author Jorge San Emeterio
 * @date 23/05/17
 */
public class TestUtils
{
    private static final Robot robot;

    static
    {
        robot = BasicRobot.robotWithCurrentAwtHierarchy();
        robot.settings().componentLookupScope(ComponentLookupScope.ALL);
    }

    public static Robot getRobot()
    {
        return robot;
    }
}
