package com.net2plan.gui.utils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;

public class Robot extends java.awt.Robot
{
    public Robot() throws AWTException
    {
        super();

        this.setAutoDelay(40);
        this.setAutoWaitForIdle(true);
    }

    public void type(int i, int m)
    {
        this.delay(40);
        this.keyPress(m);
        this.keyPress(i);
        this.keyRelease(m);
        this.keyRelease(i);
    }

    public void type(int i)
    {
        this.delay(40);
        this.keyPress(i);
        this.keyRelease(i);
    }

    public void type(String s)
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

    public void copy(final String toCopy)
    {
        StringSelection selection = new StringSelection(toCopy);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
    }

    public void paste()
    {
        this.type(KeyEvent.VK_V, KeyEvent.VK_CONTROL);
    }
}
