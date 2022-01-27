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


package com.net2plan.utils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
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

    /**
     * Thanks to user Boann at StackOverflow for his <a href="http://stackoverflow.com/questions/16846078/jfilechoosershowsavedialog-cant-get-the-value-of-the-extension-file-chosen">getSelectedFileWithExtension</a>
     *
     * @param c JFileChooser where the file is selected.
     * @return File including the extension from the file filter.
     */
    public static File getSelectedFileWithExtension(JFileChooser c) {
        File file = c.getSelectedFile();
        if (c.getFileFilter() instanceof FileNameExtensionFilter)
        {
            String[] exts = ((FileNameExtensionFilter)c.getFileFilter()).getExtensions();

            String nameLower = file.getName().toLowerCase();
            for (String ext : exts)
            {
                if (nameLower.endsWith('.' + ext.toLowerCase()))
                {
                    return file;
                }
            }

            file = new File(file.toString() + '.' + exts[0]);
        }
        return file;
    }
}
