package com.net2plan.gui.launcher;

import com.net2plan.gui.GUINet2Plan;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUITrafficDesign;
import com.net2plan.internal.plugins.IGUIModule;
import com.net2plan.internal.plugins.PluginSystem;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Created by Jorge San Emeterio on 17/03/17.
 */
public class GUINetworkDesignLauncher
{
    public static void main(String[] args)
    {
        GUINet2Plan.main(args);
        PluginSystem.addPlugin(IGUIModule.class, GUINetworkDesign.class);
        PluginSystem.addPlugin(IGUIModule.class, GUITrafficDesign.class);
        PluginSystem.loadExternalPlugins();
        GUINet2Plan.refreshMenu();

        try
        {
            executeRobot();
        } catch (AWTException e)
        {
            e.printStackTrace();
        }
    }

    private static void executeRobot() throws AWTException
    {
        final Robot robot = new Robot();
        robot.setAutoDelay(40);
        robot.setAutoWaitForIdle(true);

        // Run NetworkDesign tool
        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_1);

        robot.delay(1000);

        robot.keyRelease(KeyEvent.VK_1);
        robot.keyRelease(KeyEvent.VK_ALT);

        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_F4);
        robot.keyRelease(KeyEvent.VK_F4);
        robot.keyRelease(KeyEvent.VK_ALT);

        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_O);

        robot.keyRelease(KeyEvent.VK_O);
        robot.keyRelease(KeyEvent.VK_CONTROL);
    }


}
