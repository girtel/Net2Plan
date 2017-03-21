package com.net2plan.gui.plugins.debug;

import com.net2plan.gui.utils.Robot;
import com.net2plan.interfaces.IGUIPluginWrapper;

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
                throw new RuntimeException("Unknown mode: " + mode + " in wrapper: " + this.getClass().getName());
            case 1:
                return;
            case 2:
                if (!parameters.containsKey("netplan") || parameters.get("netplan") == null || parameters.get("netplan").isEmpty())
                    throw new RuntimeException("NetPlan parameter is needed for launching mode 2 of: " + this.getClass().getName());

                mode2();
                break;
        }
    }

    private void mode2()
    {
        final String netplanURL = parameters.get("netplan");

        try
        {
            Robot robot = new Robot();

            // Close table window
            robot.type(KeyEvent.VK_F4, KeyEvent.VK_ALT);

            robot.delay(200);

            // Open up load menu
            robot.type(KeyEvent.VK_O, KeyEvent.VK_CONTROL);

            robot.delay(200);

            robot.copy(netplanURL);
            robot.paste();
            robot.type(KeyEvent.VK_ENTER);
        } catch (AWTException e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }
}
