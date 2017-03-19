package com.net2plan.gui.launcher.wrapper;

import com.net2plan.gui.launcher.utils.GUIRobot;
import org.apache.commons.cli.ParseException;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Map;

/**
 * Created by Jorge San Emeterio on 18/03/2017.
 */
public class GUINetworkDesignWrapper implements IGUIPluginWrapper
{
    private Map<String, String> parameters;

    @Override
    public void launchMode(final int mode, final Map<String, String> parameters)
    {
        this.parameters = parameters;

        switch (mode)
        {
            default:
                System.err.println("Unknown mode: " + mode + " in wrapper: " + this.getClass().getName());
            case 1:
                return;
            case 2:
                mode2();
                break;
        }
    }

    private void mode2()
    {
        final String netplanURL = parameters.get("netplan");

        GUIRobot robot = null;
        try
        {
            robot = new GUIRobot();

            // Close table window
            robot.type(KeyEvent.VK_F4, KeyEvent.VK_ALT);

            robot.delay(200);

            // Open up load menu
            robot.type(KeyEvent.VK_O, KeyEvent.VK_CONTROL);

            robot.delay(200);

            robot.type(netplanURL);
            robot.type(KeyEvent.VK_INSERT);
        } catch (AWTException e)
        {
            e.printStackTrace();
        }
    }
}
