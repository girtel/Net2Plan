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


package com.net2plan.gui.utils;

import javax.swing.*;
import java.awt.*;

/**
 * Class implementing some icons.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class TabIcon implements Icon {
    private final static int WIDTH = 16;
    private final static int HEIGHT = 16;

    /**
     * Icon types.
     *
     * @since 0.2.0
     */
    public static enum IconType {
        /**
         * Plus sign ('+')
         *
         * @since 0.2.0
         */
        PLUS_SIGN,

        /**
         * Times sign ('x')
         *
         * @since 0.2.0
         */
        TIMES_SIGN
    }

    ;

    private final IconType iconType;
    private int x_pos;
    private int y_pos;

    /**
     * Default constructor.
     *
     * @param iconType Type of icon
     * @since 0.2.0
     */
    public TabIcon(IconType iconType) {
        this.iconType = iconType;
    }

    @Override
    public int getIconHeight() {
        return HEIGHT;
    }

    @Override
    public int getIconWidth() {
        return WIDTH;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        this.x_pos = x;
        this.y_pos = y;

        Color col = g.getColor();
        g.setColor(Color.BLACK);
        int y_p = y + 2;

		/* Border */
        g.drawLine(x + 1, y_p, x + 12, y_p);
        g.drawLine(x + 1, y_p + 13, x + 12, y_p + 13);
        g.drawLine(x, y_p + 1, x, y_p + 12);
        g.drawLine(x + 13, y_p + 1, x + 13, y_p + 12);

        switch (iconType) {
            case PLUS_SIGN:
                g.drawLine(x + 3, y_p + 7, x + 10, y_p + 7);
                g.drawLine(x + 3, y_p + 6, x + 10, y_p + 6);
                g.drawLine(x + 7, y_p + 3, x + 7, y_p + 10);
                g.drawLine(x + 6, y_p + 3, x + 6, y_p + 10);
                break;

            case TIMES_SIGN:
                g.drawLine(x + 3, y_p + 3, x + 10, y_p + 10);
                g.drawLine(x + 3, y_p + 4, x + 9, y_p + 10);
                g.drawLine(x + 4, y_p + 3, x + 10, y_p + 9);
                g.drawLine(x + 10, y_p + 3, x + 3, y_p + 10);
                g.drawLine(x + 10, y_p + 4, x + 4, y_p + 10);
                g.drawLine(x + 9, y_p + 3, x + 3, y_p + 9);
                break;

            default:
                break;
        }

        g.setColor(col);
    }

    /**
     * Returns the bounds of the icon.
     *
     * @return Bounds of the icon
     * @since 0.2.0
     */
    public Rectangle getBounds() {
        return new Rectangle(x_pos, y_pos, WIDTH, HEIGHT);
    }
}
