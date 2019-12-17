package com.net2plan.examples.niw.research.technoec;

import com.net2plan.interfaces.networkDesign.Net2PlanException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

public class TimExcel_A1TrafficSheet extends TimExcelSheet
{

	final TimExcelSheetColumn<String> nodeName = new TimExcelSheetColumn<String>("M-H node name", String.class, s -> true);
	final TimExcelSheetColumn<Double> downTrafficUPF_2019 = new TimExcelSheetColumn<Double>("2019 Down", Double.class, s -> (s) >= 0);
    final TimExcelSheetColumn<Double> downTrafficUPF_2022 = new TimExcelSheetColumn<Double>("2022 Down", Double.class, s -> (s) >= 0);
    final TimExcelSheetColumn<Double> downTrafficUPF_2025 = new TimExcelSheetColumn<Double>("2025 Down", Double.class, s -> (s) >= 0);
    final TimExcelSheetColumn<Double> upTrafficUPF_2019 = new TimExcelSheetColumn<Double>("2019 Up", Double.class, s -> (s) >= 0);
    final TimExcelSheetColumn<Double> upTrafficUPF_2022 = new TimExcelSheetColumn<Double>("2022 Up", Double.class, s -> (s) >= 0);
    final TimExcelSheetColumn<Double> upTrafficUPF_2025 = new TimExcelSheetColumn<Double>("2025 Up", Double.class, s -> (s) >= 0);

	public TimExcel_A1TrafficSheet(SortedMap<String, Object[][]> valsAllExcels, List<String> listNodeNames)
	{
		super(valsAllExcels);
		this.checkIntraSheetValidity();
	}

	@Override
	public final String getSheetName()
	{
		return "Peer2Peer";
	}

    public TimExcel_A1TrafficSheet(SortedMap<String, Object[][]> valsAllExcels)
    {
        super(valsAllExcels);
        this.checkIntraSheetValidity();
    }


	public List<Double> getDownstreamUPFTrafficGbps(String year)
	{
        if (year.equals("2019")) return downTrafficUPF_2019.getColumnValuesInTable(getVals());
        else if (year.equals("2022")) return downTrafficUPF_2022.getColumnValuesInTable(getVals());
        else if (year.equals("2025")) return downTrafficUPF_2025.getColumnValuesInTable(getVals());
        else throw new Net2PlanException("Not a valid year");
	}

    public List<Double> getUpstreamUPFTrafficGbps(String year)
    {
        if (year.equals("2019")) return upTrafficUPF_2019.getColumnValuesInTable(getVals());
        else if (year.equals("2022")) return upTrafficUPF_2022.getColumnValuesInTable(getVals());
        else if (year.equals("2025")) return upTrafficUPF_2025.getColumnValuesInTable(getVals());
        else throw new Net2PlanException("Not a valid year");
    }

	public List<String> getNodeNames()
	{
		return nodeName.getColumnValuesInTable(getVals());
	}


	public void checkIntraSheetValidity()
	{
		final List<Double> downUPFTraffic = getDownstreamUPFTrafficGbps("2019");
		final List<Double> upUPFTraffic = getUpstreamUPFTrafficGbps("2019");
		final List<String> nodeNames = getNodeNames();
		if (upUPFTraffic == null) throw new Net2PlanException("Upstream traffic UPF column not read");
        if (downUPFTraffic == null) throw new Net2PlanException("Downstream traffic UPF column not read");
		if (nodeNames == null) throw new Net2PlanException("Origin node column not read");
		final int numElements = this.getNumRows();
		if (upUPFTraffic.size() != numElements) throw new Net2PlanException("Not all the columns have the same size");
        if (downUPFTraffic.size() != numElements) throw new Net2PlanException("Not all the columns have the same size");
		if (nodeNames.size() != numElements) throw new Net2PlanException("Not all the columns have the same size");
	}
}
