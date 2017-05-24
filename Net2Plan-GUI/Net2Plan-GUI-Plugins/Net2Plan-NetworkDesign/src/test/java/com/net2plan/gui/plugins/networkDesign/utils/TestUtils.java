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
