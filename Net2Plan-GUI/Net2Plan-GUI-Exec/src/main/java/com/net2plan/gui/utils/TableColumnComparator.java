package com.net2plan.gui.utils;

import java.util.Comparator;
import java.util.List;

import javax.swing.RowSorter;
import javax.swing.SortOrder;

import com.net2plan.utils.Pair;

/**
 * @author Jorge San Emeterio
 * @date 9/19/17
 */
public class TableColumnComparator implements Comparator<Object>
{
    private final RowSorter rs;

    public TableColumnComparator(RowSorter rs)
    {
        this.rs = rs;
    }

    @Override
    public int compare(Object o1, Object o2)
    {
        if (o1 instanceof LastRowAggregatedValue)
        {
            final boolean ascending = ((List<? extends RowSorter.SortKey>) rs.getSortKeys()).get(0).getSortOrder() == SortOrder.ASCENDING;
            return ascending ? 1 : -1;
        }
        if (o2 instanceof LastRowAggregatedValue)
        {
            final boolean ascending = ((List<? extends RowSorter.SortKey>) rs.getSortKeys()).get(0).getSortOrder() == SortOrder.ASCENDING;
            return ascending ? -1 : 1;
        }
        if (o1 instanceof Boolean)
        {
            final Boolean oo1 = (Boolean) o1;
            final Boolean oo2 = (Boolean) o2;
            return oo1.compareTo(oo2);
        }
        if (o1 instanceof Number && o2 instanceof Number)
        {
            final Number oo1 = (Number) o1;
            final Number oo2 = (Number) o2;
            return new Double(oo1.doubleValue()).compareTo(new Double(oo2.doubleValue()));
        }
        if (o1 instanceof Pair && o2 instanceof Pair)
        {
            final Number oo1 = (Number) ((Pair) o1).getFirst();
            final Number oo2 = (Number) ((Pair) o2).getFirst();
            return new Double(oo1.doubleValue()).compareTo(new Double(oo2.doubleValue()));
        }
        if (o1 instanceof Number)
            return -1;
        if (o2 instanceof Number)
            return 1;

        String oo1 = o1.toString();
        String oo2 = o2.toString();

        return oo1.compareTo(oo2);
    }
}
