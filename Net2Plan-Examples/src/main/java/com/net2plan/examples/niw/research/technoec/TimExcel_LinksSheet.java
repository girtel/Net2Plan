package com.net2plan.examples.niw.research.technoec;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import com.net2plan.interfaces.networkDesign.Net2PlanException;

public class TimExcel_LinksSheet extends TimExcelSheet
{
	final TimExcelSheetColumn<String> linkMhCode = new TimExcelSheetColumn<String>("Link M-H Code", String.class, s -> true);
	final TimExcelSheetColumn<String> nodeNameA = new TimExcelSheetColumn<String>("Node M-H Code (A)", String.class, s -> true);
	final TimExcelSheetColumn<String> nodeNameB = new TimExcelSheetColumn<String>("Node M-H Code (B)", String.class, s -> true);
	final TimExcelSheetColumn<Double> distanceKm = new TimExcelSheetColumn<Double>("Distance [km]", Double.class, s -> (s) >= 0);

	public TimExcel_LinksSheet(SortedMap<String, Object[][]> valsAllExcels, List<String> listNodeNames)
	{
		super(valsAllExcels);
		this.checkIntraSheetValidity(listNodeNames);
	}

	@Override
	public final String getSheetName()
	{
		return "Links";
	}

	public List<String> getLinkUniqueCode()
	{
		return linkMhCode.getColumnValuesInTable(getVals());
	}

	public List<String> getNodeNameA()
	{
		return nodeNameA.getColumnValuesInTable(getVals());
	}

	public List<String> getNodeNameB()
	{
		return nodeNameB.getColumnValuesInTable(getVals());
	}

	public List<Double> getLinkDistanceKm()
	{
		return distanceKm.getColumnValuesInTable(getVals());
	}

	public void checkIntraSheetValidity(List<String> listNodeNames)
	{
		final List<String> linkUid = getLinkUniqueCode();
		final List<String> nodeNameA = getNodeNameA();
		final List<String> nodeNameB = getNodeNameB();
		final List<Double> distanceKm = getLinkDistanceKm();
		if (linkUid == null) throw new Net2PlanException("Link UIS column not read");
		if (nodeNameA == null) throw new Net2PlanException("Origin node column not read");
		if (nodeNameB == null) throw new Net2PlanException("Destination node column not read");
		if (distanceKm == null) throw new Net2PlanException("Link distance column not read");
		final int numElements = this.getNumRows();
		if (linkUid.size() != numElements) throw new Net2PlanException("Not all the columns have the same size");
		if (nodeNameA.size() != numElements) throw new Net2PlanException("Not all the columns have the same size");
		if (nodeNameB.size() != numElements) throw new Net2PlanException("Not all the columns have the same size");
		if (distanceKm.size() != numElements) throw new Net2PlanException("Not all the columns have the same size");
		if (linkUid.size() != (new HashSet<>(linkUid)).size()) throw new Net2PlanException("Link uids are not unique");
		final Set<String> setNodeNames = new HashSet<>(listNodeNames);
		for (int cont = 0; cont < numElements; cont++)
		{
			final String a = nodeNameA.get(cont);
			final String b = nodeNameB.get(cont);
			if (a.equals(b)) throw new Net2PlanException("Link in row " + cont + " has both ends in the same node: " + a);
			if (!setNodeNames.contains(a)) throw new Net2PlanException("Link in row " + cont + ": origin node not found: " + a);
			if (!setNodeNames.contains(b)) throw new Net2PlanException("Link in row " + cont + ": origin node not found: " + b);
		}
	}
}
