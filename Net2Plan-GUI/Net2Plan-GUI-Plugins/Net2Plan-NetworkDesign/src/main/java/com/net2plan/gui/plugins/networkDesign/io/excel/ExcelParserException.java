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
package com.net2plan.gui.plugins.networkDesign.io.excel;

import com.net2plan.interfaces.networkDesign.Net2PlanException;

/**
 * @author Jorge San Emeterio
 * @date 11-Nov-16
 */
public class ExcelParserException extends Net2PlanException
{
    public ExcelParserException()
    {
        super();
    }

    public ExcelParserException(final String message)
    {
        super(message);
    }
}
