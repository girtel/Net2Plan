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

import com.net2plan.internal.plugins.IOFilter;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.Locale;

/**
 * Extends JFileChooser to avoid the problem of users overwritting an existing file without any warning.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class FileChooserConfirmOverwrite extends JFileChooser {
    /**
     * Default constructor.
     *
     * @since 0.2.0
     */
    public FileChooserConfirmOverwrite() {
        super();
    }

    /**
     * Constructor that allows to set the current directory.
     *
     * @param currentDirectory Current directory
     * @since 0.2.0
     */
    public FileChooserConfirmOverwrite(File currentDirectory) {
        super(currentDirectory);
    }

    @Override
    public void approveSelection() {
        File f = getSelectedFile();

        if (getDialogType() == SAVE_DIALOG) {
            FileFilter fileFilter = getFileFilter();
            String extension = null;
            if (fileFilter instanceof FileNameExtensionFilter) {
                String[] extensions = ((FileNameExtensionFilter) fileFilter).getExtensions();
                if (extensions.length > 0) extension = extensions[0];
            } else if (fileFilter instanceof IOFilter) {
                IOFilter ioFilter = (IOFilter) fileFilter;
                if (!ioFilter.acceptAllExtensions()) extension = ioFilter.getAcceptedExtensions().iterator().next();
            }

            if (extension != null && !extension.isEmpty()) {
                String filePath = f.getPath();
                if (!filePath.toLowerCase(Locale.getDefault()).endsWith("." + extension)) {
                    f = new File(filePath + "." + extension);
                    setSelectedFile(f);
                }
            }

            if (f.exists()) {
                int result = JOptionPane.showConfirmDialog(this, "The file exists, overwrite?", "Existing file", JOptionPane.YES_NO_CANCEL_OPTION);

                switch (result) {
                    case JOptionPane.YES_OPTION:
                        super.approveSelection();
                        return;

                    case JOptionPane.NO_OPTION:
                    case JOptionPane.CLOSED_OPTION:
                        return;

                    case JOptionPane.CANCEL_OPTION:
                        cancelSelection();
                        return;

                    default:
                        throw new RuntimeException("Bad");
                }
            }
        }

        super.approveSelection();
    }
}
