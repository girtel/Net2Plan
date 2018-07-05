package com.net2plan.gui.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;

/**
 * @author Jorge San Emeterio
 * @date 8/11/17
 */
public class InfNumberFormat extends DecimalFormat
{
    public InfNumberFormat(String s)
    {
        super(s);
    }

    @Override
    public StringBuffer format(double var1, StringBuffer var3, FieldPosition var4)
    {
        if (var1 == Double.MAX_VALUE || var1 == Float.MAX_VALUE)
            return super.format(Double.POSITIVE_INFINITY, var3, var4);
        if (var1 == -Double.MAX_VALUE || var1 == -Float.MAX_VALUE)
            return super.format(Double.NEGATIVE_INFINITY, var3, var4);

        return super.format(var1, var3, var4);
    }

    @Override
    public StringBuffer format(long var1, StringBuffer var3, FieldPosition var4)
    {
        if (var1 == Long.MAX_VALUE || var1 == Integer.MAX_VALUE)
            return super.format(Double.POSITIVE_INFINITY, var3, var4);
        if (var1 == -Long.MAX_VALUE || var1 == -Integer.MAX_VALUE)
            return super.format(Double.NEGATIVE_INFINITY, var3, var4);

        return super.format(var1, var3, var4);
    }
}
