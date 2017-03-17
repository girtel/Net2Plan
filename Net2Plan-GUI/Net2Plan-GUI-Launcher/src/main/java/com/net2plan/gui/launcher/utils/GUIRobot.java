package com.net2plan.gui.launcher.utils;

import java.awt.*;

/**
 * Created by Jorge San Emeterio on 17/03/17.
 */
public class GUIRobot extends Robot
{
    public GUIRobot() throws AWTException
    {
        super();
    }

    private void type(int i, int m)
    {
        this.delay(40);
        this.keyPress(m);
        this.keyPress(i);
        this.keyRelease(m);
        this.keyRelease(i);
    }

    private void type(int i)
    {
        this.delay(40);
        this.keyPress(i);
        this.keyRelease(i);
    }

    private void type(String s)
    {
        byte[] bytes = s.getBytes();
        for (byte b : bytes)
        {
            int code = b;
            // keycode only handles [A-Z] (which is ASCII decimal [65-90])
            if (code > 96 && code < 123) code = code - 32;
            this.delay(40);
            this.keyPress(code);
            this.keyRelease(code);
        }
    }
}
