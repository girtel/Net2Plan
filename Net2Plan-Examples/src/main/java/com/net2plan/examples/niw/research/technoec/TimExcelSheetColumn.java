package com.net2plan.examples.niw.research.technoec;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.net2plan.interfaces.networkDesign.Net2PlanException;

public class TimExcelSheetColumn<T>
{
	final private String colName;
	final private Class typeOfValue;
	final private Function<T, Boolean> checkValueFunction;

	TimExcelSheetColumn(String colName, Class typeOfValue, Function<T, Boolean> checkValueFunction)
	{
		this.colName = colName.trim();
		this.typeOfValue = typeOfValue;
		this.checkValueFunction = checkValueFunction;
	}

	public String getColName()
	{
		return this.colName;
	}

	public Class getTypeOfValue()
	{
		return this.typeOfValue;
	}

	public Function<T, Boolean> getCheckValueFunction()
	{
		return this.checkValueFunction;
	}

	public List<T> getColumnValuesInTable(Object[][] vals)
	{
		if (vals.length == 0) throw new Net2PlanException("Empty table");
		for (int col = 0; col < vals[0].length; col++)
		{
			final Object o = vals[0][col];
			if (!(o instanceof String)) continue;
			final String s = (String) o;
			if (!s.trim().equalsIgnoreCase(this.getColName())) continue;
			final List<T> res = new ArrayList<>(vals.length - 1);
			for (int row = 1; row < vals.length; row++)
			{
				try
				{
					final T val = (T) getTypeOfValue().cast(vals[row][col]);
					final boolean ok = getCheckValueFunction().apply(val);
					if (!ok) throw new Net2PlanException("Value " + val + " in column " + this.getColName() + " and row " + row + ", is not passing the value check");
					res.add(val);
				} catch (Exception exc)
				{
					exc.printStackTrace();
					if (exc instanceof Net2PlanException) throw exc;
					throw new Net2PlanException("Error reading row " + row + " in column " + getColName() + ". Val read = " + vals[row][col]);
				}
			}
			return res;
		}
		throw new Net2PlanException("Column " + this.getColName() + " not found in the Excel");
	}
}
