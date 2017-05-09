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
