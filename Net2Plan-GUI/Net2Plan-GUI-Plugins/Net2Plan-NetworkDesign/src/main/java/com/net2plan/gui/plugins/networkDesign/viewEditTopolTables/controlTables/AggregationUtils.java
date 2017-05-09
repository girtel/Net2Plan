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
        if (val instanceof Double)
            aggreg[index] += (Double) val;
        else
            ErrorHandling.log("Could not aggregate value of type: " + val.getClass());
    }

    public static void updateRowMax(double[] aggreg, int index, Object val)
    {
        if (val instanceof Double)
            aggreg[index] = Math.max((Double) val, aggreg[index]);
        else
            ErrorHandling.log("Could not aggregate value of type: " + val.getClass());
    }

    public static void updateRowCount(double[] aggreg, int index, Object val)
    {
        aggreg[index] = aggreg[index] + 1;
    }
}
