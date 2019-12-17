package com.net2plan.examples.niw.research.technoec;


import java.util.List;
import java.util.SortedMap;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Net2PlanException;

public abstract class TimExcelSheet
{
	final private Object[][] vals;

	public TimExcelSheet(SortedMap<String, Object[][]> valsAllExcels)
	{
		final List<Object[][]> sheetsWithMatchingNames = valsAllExcels.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(this.getSheetName())).map(e -> e.getValue()).collect(Collectors.toList());
		if (sheetsWithMatchingNames.size() > 1) throw new Net2PlanException("More than one sheet has a matching name with " + this.getSheetName());
		if (sheetsWithMatchingNames.isEmpty()) throw new Net2PlanException("No sheet has a matching name with " + this.getSheetName());
		this.vals = sheetsWithMatchingNames.iterator().next();
	}

	public abstract String getSheetName();

	public final int getNumRows()
	{
		if (vals.length == 0) throw new Net2PlanException("Empty sheet");
		return vals.length - 1;
	}

	public final Object[][] getVals()
	{
		return vals;
	}

}
