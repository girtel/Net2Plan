package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

/**
 * @author Jorge San Emeterio
 * @date 9/05/17
 */
public class AggregationUtils
{
    public static void updateRowSum(Object[] data, double[] aggreg, int index, double val)
    {
        data[index] = val;
        aggreg[index] += val;
    }

    public static void updateRowMax(Object[] data, double[] aggreg, int index, double val)
    {
        data[index] = val;
        aggreg[index] = Math.max(val, aggreg[index]);
    }

    public static void updateRowCount(Object[] data, double[] aggreg, int index, Object val)
    {
        data[index] = val;
        aggreg[index] = aggreg[index] + 1;
    }
}
