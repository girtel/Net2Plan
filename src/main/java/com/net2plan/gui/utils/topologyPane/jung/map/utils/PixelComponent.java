package com.net2plan.gui.utils.topologyPane.jung.map.utils;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Jorge San Emeterio on 14/10/2016.
 */
public class PixelComponent extends JComponent
{
    private Color color;

    public PixelComponent(Color color)
    {
        super();
        this.color = color;
    }

    public PixelComponent()
    {
        this(Color.BLACK);
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g.setColor(color);
        g.fillOval(0, 0, 200, 200);
    }
}
