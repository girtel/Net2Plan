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
package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport;

import com.net2plan.internal.ErrorHandling;

/**
 * @author Jorge San Emeterio
 * @date 09-Feb-17
 */
public class OSMException extends RuntimeException
{
    public OSMException(final String message)
    {
        ErrorHandling.showErrorDialog(message, "Could not display OSM");
    }

    public OSMException(final String message, final String title)
    {
        ErrorHandling.showErrorDialog(message, title);
    }
}
