package com.net2plan.gui.utils.viewEditWindows.utils;

import com.net2plan.gui.GUINet2Plan;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Jorge San Emeterio on 14/09/2016.
 */
public final class WindowUtils
{
    private WindowUtils()
    {
    }

    public static void clearFloatingWindows()
    {
        Window[] windows = Window.getWindows();

        for (Window window : windows)
        {
            if (!(window instanceof GUINet2Plan))
            {
                window.setVisible(false);
                window.dispose();
            }
        }
    }

    public static void setFrameBottomRight(final JFrame frame)
    {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
        int x = (int) rect.getMaxX() - frame.getWidth();
        int y = (int) rect.getMaxY() - frame.getHeight();
        frame.setLocation(x, y);
    }

    public static void setWindowLeftSide(final JFrame frame)
    {
        frame.setExtendedState(JFrame.NORMAL);
        giveFrameHalfScreen(frame);

        int x = 0;
        int y = 0;
        frame.setLocation(x, y);
    }

    public static void setWindowRightSide(final JFrame frame)
    {
        frame.setExtendedState(JFrame.NORMAL);
        giveFrameHalfScreen(frame);

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
        int x = (int) rect.getMaxX() - frame.getWidth();
        int y = 0;
        frame.setLocation(x, y);
    }

    private static void giveFrameHalfScreen(final JFrame frame)
    {
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        final Rectangle winSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        final int taskBarHeight = screenSize.height - winSize.height;

        final int width = screenSize.width / 2;
        final int height = screenSize.height - taskBarHeight;

        frame.setSize(new Dimension(width, height));
    }
}
