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
package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

public class LastRowAggregatedValue
{
    private String value;

    public LastRowAggregatedValue()
    {
        value = "---";
    }

    public LastRowAggregatedValue(int val)
    {
        value = "" + val;
    }

    public LastRowAggregatedValue(double val)
    {
        value = String.format("%.2f", val);
    }

    public LastRowAggregatedValue(String value)
    {
        this.value = value;
    }

    public String toString()
    {
        return value;
    }
}
