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
package com.net2plan.gui.plugins.utils;

import com.net2plan.interfaces.IGUIModeWrapper;

import java.util.Map;

/**
 * Created by Jorge San Emeterio on 19/03/2017.
 */
public class GUITrafficDesignModeWrapper implements IGUIModeWrapper
{
    private Map<String, String> parameters;

    @Override
    public void launchRoutine(int mode, Map<String, String> parameters)
    {
        this.parameters = parameters;

        switch (mode)
        {
            default:
                throw new RuntimeException("Unknown mode: " + mode + " in wrapper: " + this.getClass().getName());
            case 1:
                return;
        }
    }
}
