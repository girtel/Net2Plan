package com.net2plan.examples.niw.research.technoec;


import com.net2plan.interfaces.networkDesign.Net2PlanException;

import java.util.List;
import java.util.SortedMap;

public class TimExcel_A2TrafficSheet extends TimExcelSheet
{

	final TimExcelSheetColumn<String> nodeName = new TimExcelSheetColumn<String>("M-H node name", String.class, s -> true);
	final TimExcelSheetColumn<Double> downTrafficTotal_2019 = new TimExcelSheetColumn<Double>("2019 Down", Double.class, s -> (s) >= 0);
    final TimExcelSheetColumn<Double> downTrafficTotal_2022 = new TimExcelSheetColumn<Double>("2022 Down", Double.class, s -> (s) >= 0);
    final TimExcelSheetColumn<Double> downTrafficTotal_2025 = new TimExcelSheetColumn<Double>("2025 Down", Double.class, s -> (s) >= 0);
    final TimExcelSheetColumn<Double> upTrafficTotal_2019 = new TimExcelSheetColumn<Double>("2019 Up", Double.class, s -> (s) >= 0);
    final TimExcelSheetColumn<Double> upTrafficTotal_2022 = new TimExcelSheetColumn<Double>("2022 Up", Double.class, s -> (s) >= 0);
    final TimExcelSheetColumn<Double> upTrafficTotal_2025 = new TimExcelSheetColumn<Double>("2025 Up", Double.class, s -> (s) >= 0);

    public TimExcel_A2TrafficSheet(SortedMap<String, Object[][]> valsAllExcels)
    {
        super(valsAllExcels);
        this.checkIntraSheetValidity();
    }

	@Override
	public final String getSheetName()
	{
		return "Web";
	}

	public List<Double> getDownstreamTotalTraffic(String year)
	{
        if (year.equals("2019")) return downTrafficTotal_2019.getColumnValuesInTable(getVals());
        else if (year.equals("2022")) return downTrafficTotal_2022.getColumnValuesInTable(getVals());
        else if (year.equals("2025")) return downTrafficTotal_2025.getColumnValuesInTable(getVals());
        else throw new Net2PlanException("Not a valid year");
	}

    public List<Double> getUpstreamTotalTraffic(String year)
    {
        if (year.equals("2019")) return upTrafficTotal_2019.getColumnValuesInTable(getVals());
        else if (year.equals("2022")) return upTrafficTotal_2022.getColumnValuesInTable(getVals());
        else if (year.equals("2025")) return upTrafficTotal_2025.getColumnValuesInTable(getVals());
        else throw new Net2PlanException("Not a valid year");
    }

	public List<String> getNodeNames()
	{
		return nodeName.getColumnValuesInTable(getVals());
	}


	public void checkIntraSheetValidity()
	{
		final List<Double> downTotalTraffic = getDownstreamTotalTraffic("2019");
		final List<Double> upTotalTraffic = getUpstreamTotalTraffic("2019");
		final List<String> nodeNames = getNodeNames();
		if (downTotalTraffic == null) throw new Net2PlanException("Total upstream traffic column not read");
        if (upTotalTraffic == null) throw new Net2PlanException("Total downstream traffic UPF column not read");
		if (nodeNames == null) throw new Net2PlanException("Origin node column not read");
		final int numElements = this.getNumRows();
		if (downTotalTraffic.size() != numElements) throw new Net2PlanException("Not all the columns have the same size");
        if (upTotalTraffic.size() != numElements) throw new Net2PlanException("Not all the columns have the same size");
		if (nodeNames.size() != numElements) throw new Net2PlanException("Not all the columns have the same size");
	}
}
