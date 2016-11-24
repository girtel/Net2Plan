package com.net2plan.gui.utils.windows.utils;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Credits to user YAS_Bangamuwage from stack overflow for his <a href="http://stackoverflow.com/questions/8080438/mouseevent-of-jtabbedpane">BasicTabbedPaneUIWrapper</a>.
 *
 * @author Jorge San Emeterio
 * @date 24-Nov-16
 */
public class BasicTabbedPaneUIWrapper extends BasicTabbedPaneUI
{
    private MouseAdapter menuAdapter;

    private MouseAdapter getMenuAdapter()
    {
        if (menuAdapter == null)
        {
            menuAdapter =
                    new MouseAdapter()
                    {
                        @Override
                        public void mouseReleased(final MouseEvent e)
                        {
                            //implement to stop right click tab selection
                            //implement to show pop up menu

                            if (SwingUtilities.isLeftMouseButton(e))
                            {
                            }

                            System.out.println("Hola mundo");
                        }
                    };
        }

        return menuAdapter;
    }

    @Override
    protected MouseListener createMouseListener()
    {
        return super.createMouseListener();
    }
}

