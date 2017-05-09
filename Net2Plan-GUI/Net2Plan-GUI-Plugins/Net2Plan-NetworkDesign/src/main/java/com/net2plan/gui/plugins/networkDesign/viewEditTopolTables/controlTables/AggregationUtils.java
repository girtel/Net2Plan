package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

/**
 * @author Jorge San Emeterio
 * @date 9/05/17
 */
public class AggregationUtils
{
    public static void updateRowSum(double[] aggreg, int index, double val)
    {
        aggreg[index] += val;
    }

    public static void updateRowMax(double[] aggreg, int index, double val)
    {
        aggreg[index] = Math.max(val, aggreg[index]);
    }

    public static void updateRowCount(double[] aggreg, int index, Object val)
    {
        aggreg[index] = aggreg[index] + 1;
    }
}
