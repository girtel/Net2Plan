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

import com.net2plan.internal.IExternal;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Set;

/**
 * Unchecked exception to indicate no runnable code was found in the input file.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.1
 */
public class NoRunnableCodeFound extends RuntimeException {
    private final static String TEMPLATE = "No runnable code found in file %s. Are you loading a .class/.jar file with code (%s) for this tool?";

    /**
     * Default constructor.
     *
     * @param f        File where runnable code should be found
     * @param _classes Set of admissible classes for runnable code
     * @since 0.3.1
     */
    public NoRunnableCodeFound(File f, Set<Class<? extends IExternal>> _classes) {
        super(String.format(TEMPLATE, f.toString(), StringUtils.join(toString(_classes), ", ")));
    }

    private static String[] toString(Set<Class<? extends IExternal>> _classes) {
        String[] out = new String[_classes.size()];
        int i = 0;
        for (Class<? extends IExternal> _class : _classes)
            out[i++] = _class.getName();

        return out;
    }
}
