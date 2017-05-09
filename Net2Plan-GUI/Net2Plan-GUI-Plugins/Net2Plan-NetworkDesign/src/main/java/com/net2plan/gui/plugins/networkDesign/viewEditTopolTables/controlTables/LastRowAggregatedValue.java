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
