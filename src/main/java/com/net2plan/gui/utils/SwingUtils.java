/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/


package com.net2plan.gui.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;

/**
 * A collection of utility methods for Swing.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 0.2.3
 */
public class SwingUtils {
    /**
     * Configures a {@code JDialog} to be closed when {@code ESC} key is pressed.
     *
     * @param dialog {@code JDialog}
     * @since 0.2.3
     */
    public static void configureCloseDialogOnEscape(JDialog dialog) {
        Container contentPane = dialog.getContentPane();
        if (contentPane instanceof JComponent)
            ((JComponent) contentPane).registerKeyboardAction(new CloseDialogOnEscape(dialog), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    /**
     * Returns all the components (and their children) associated to the given container.
     *
     * @param container Container
     * @return {@code Component} list
     * @since 0.3.0
     */
    public static List<Component> getAllComponents(Container container) {
        List<Component> out = new LinkedList<Component>();
        for (Component comp : container.getComponents()) {
            out.add(comp);
            if (comp instanceof Container) out.addAll(getAllComponents((Container) comp));
        }

        return out;
    }

    /**
     * Enables/disables a {@code JComponent} and all its associated children.
     *
     * @param component {@code JComponent}
     * @param enabled   true if this component should be enabled, false otherwise
     * @since 0.3.0
     */
    public static void setEnabled(JComponent component, boolean enabled) {
        component.setEnabled(enabled);

        List<Component> children = getAllComponents(component);
        for (Component child : children)
            if (child instanceof JComponent)
                ((JComponent) child).setEnabled(enabled);
    }

    private static class CloseDialogOnEscape implements ActionListener {
        private final JDialog dialog;

        public CloseDialogOnEscape(JDialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            dialog.setVisible(false);
        }
    }
}
