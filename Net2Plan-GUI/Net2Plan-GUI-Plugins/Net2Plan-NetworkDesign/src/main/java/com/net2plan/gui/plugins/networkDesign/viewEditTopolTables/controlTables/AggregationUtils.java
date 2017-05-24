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

import com.net2plan.internal.ErrorHandling;

/**
 * @author Jorge San Emeterio
 * @date 9/05/17
 */
public class AggregationUtils
{
    public static void updateRowSum(double[] aggreg, int index, Object val)
    {
        try
        {
            double value = Double.parseDouble(val.toString());
            aggreg[index] += value;
        } catch (NumberFormatException e)
        {
            ErrorHandling.log("Wrong number format for: " + val.toString());
        }
    }

    public static void updateRowMax(double[] aggreg, int index, Object val)
    {
        try
        {
            double value = Double.parseDouble(val.toString());
            aggreg[index] = Math.max(value, aggreg[index]);
        } catch (NumberFormatException e)
        {
            ErrorHandling.log("Wrong number format for: " + val.toString());
        }
    }

    public static void updateRowCount(double[] aggreg, int index, int amount)
    {
        aggreg[index] = aggreg[index] + amount;
    }
}
