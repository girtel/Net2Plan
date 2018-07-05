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

public class LastRowAggregatedValue
{
    private String value;
    private InfNumberFormat numberFormat = new InfNumberFormat("##.##");

    public static final String EMPTY_VALUE = "---";

    public LastRowAggregatedValue()
    {
        value = EMPTY_VALUE;
    }
    public LastRowAggregatedValue(Number val)
    {
        this(val.doubleValue());
    }
    public LastRowAggregatedValue(int val)
    {
        this((double) val);
    }

    public LastRowAggregatedValue(double val)
    {
        final boolean isDecimal = val % 1 != 0;

        if (isDecimal)
            value = numberFormat.format(val);
        else
            value = numberFormat.format(((Number) val).intValue());
    }

    public LastRowAggregatedValue(String value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return value;
    }
}
