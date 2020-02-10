package com.net2plan.examples.niw.algorithms.mhTechnoEconomicsAlgorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.SortedMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.niw.WNode;

public class TimExcel_NodeListSheet_forConfTimData extends TimExcelSheet
{
	public enum NODETYPE
	{
		AMEN("Metro Aggregation"), MCEN("Metro Core"), MCENBB("Metro Core Backbone");

		private final String colValue;

		private NODETYPE(String colValue)
		{
			this.colValue = colValue;
		}

		public String getColValue()
		{
			return colValue;
		}

		public boolean isAmen()
		{
			return this == AMEN;
		}

		public boolean isMcenNotBb()
		{
			return this == MCEN;
		}

		public boolean isMcenBb()
		{
			return this == MCENBB;
		}

		public static NODETYPE getByName(String name)
		{
			final NODETYPE res = Stream.of(values()).filter(v -> v.getColValue().trim().equalsIgnoreCase(name.trim())).findFirst().orElse(null);
			if (res == null) throw new Net2PlanException("Unknown node type: " + name);
			return res;
		}

		public static boolean isAmen(WNode n)
		{
			return NODETYPE.getByName(n.getType()).isAmen();
		}

		public static boolean isMcenNotBb(WNode n)
		{
			return NODETYPE.getByName(n.getType()).isMcenNotBb();
		}

		public static boolean isMcenBb(WNode n)
		{
			return NODETYPE.getByName(n.getType()).isMcenBb();
		}

		public static boolean isMcenBbOrNot(WNode n)
		{
			return NODETYPE.getByName(n.getType()).isMcenBb() || NODETYPE.getByName(n.getType()).isMcenNotBb();
		}

		public static NODETYPE getNodeType(WNode a)
		{
			return NODETYPE.getByName(a.getType());
		}
	}

	final TimExcelSheetColumn<String> mhName = new TimExcelSheetColumn<String>("M-H name", String.class, s -> true);
	final TimExcelSheetColumn<String> nodeType = new TimExcelSheetColumn<String>("Node Type", String.class, s -> Stream.of(NODETYPE.values()).anyMatch(v -> v == null ? false : s.trim().equalsIgnoreCase(v.getColValue())));

    final TimExcelSheetColumn<Double> residentialTraffic_2019 = new TimExcelSheetColumn<Double>("2019 Res", Double.class, s -> (s) >= 0 );
    final TimExcelSheetColumn<Double> residentialTraffic_2022 = new TimExcelSheetColumn<Double>("2022 Res", Double.class, s -> (s) >= 0 );
    final TimExcelSheetColumn<Double> residentialTraffic_2025 = new TimExcelSheetColumn<Double>("2025 Res", Double.class, s -> (s) >= 0 );

    final TimExcelSheetColumn<Double> businessTraffic_2019 = new TimExcelSheetColumn<Double>("2019 Bus", Double.class, s -> (s) >= 0 );
    final TimExcelSheetColumn<Double> businessTraffic_2022 = new TimExcelSheetColumn<Double>("2022 Bus", Double.class, s -> (s) >= 0 );
    final TimExcelSheetColumn<Double> businessTraffic_2025 = new TimExcelSheetColumn<Double>("2025 Bus", Double.class, s -> (s) >= 0 );

    final TimExcelSheetColumn<Double> r4GTraffic_2019 = new TimExcelSheetColumn<Double>("2019 4G", Double.class, s -> (s) >= 0 );
    final TimExcelSheetColumn<Double> r4GTraffic_2022 = new TimExcelSheetColumn<Double>("2022 4G", Double.class, s -> (s) >= 0 );
    final TimExcelSheetColumn<Double> r4GTraffic_2025 = new TimExcelSheetColumn<Double>("2025 4G", Double.class, s -> (s) >= 0 );

    final TimExcelSheetColumn<Double> r5GTraffic_2019 = new TimExcelSheetColumn<Double>("2019 5G", Double.class, s -> (s) >= 0 );
    final TimExcelSheetColumn<Double> r5GTraffic_2022 = new TimExcelSheetColumn<Double>("2022 5G", Double.class, s -> (s) >= 0 );
    final TimExcelSheetColumn<Double> r5GTraffic_2025 = new TimExcelSheetColumn<Double>("2025 5G", Double.class, s -> (s) >= 0 );

    final TimExcelSheetColumn<Double> totalTraffic_2019 = new TimExcelSheetColumn<Double>("2019 Total", Double.class, s -> (s) >= 0 );
    final TimExcelSheetColumn<Double> totalTraffic_2022 = new TimExcelSheetColumn<Double>("2022 Total", Double.class, s -> (s) >= 0 );
    final TimExcelSheetColumn<Double> totalTraffic_2025 = new TimExcelSheetColumn<Double>("2025 Total", Double.class, s -> (s) >= 0 );


	public TimExcel_NodeListSheet_forConfTimData(SortedMap<String, Object[][]> valsAllExcels)
	{
		super(valsAllExcels);
        this.checkIntraSheetValidity();
	}

	@Override
	public final String getSheetName()
	{
		return "Node_List";
	}

	public List<String> getNodeNames()
	{
		return mhName.getColumnValuesInTable(getVals());
	}

	public List<NODETYPE> getNodeTypes()
	{
		final List<String> res = nodeType.getColumnValuesInTable(getVals());
		return res.stream().map(s -> TimExcel_NodeListSheet_forConfTimData.NODETYPE.getByName(s)).collect(Collectors.toCollection(ArrayList::new));
	}

	public List<Double> getDownstreamResidentialTrafficGbps_n(String year)
    {
	    if (year.equals("2019")) return residentialTraffic_2019.getColumnValuesInTable(getVals());
	    else if (year.equals("2022")) return residentialTraffic_2022.getColumnValuesInTable(getVals());
	    else if (year.equals("2025")) return residentialTraffic_2025.getColumnValuesInTable(getVals());
	    else throw new Net2PlanException("Not a valid year");
    }

    public List<Double> getDownstreamBusinessTrafficGbps_n(String year)
    {
        if (year.equals("2019")) return businessTraffic_2019.getColumnValuesInTable(getVals());
        else if (year.equals("2022")) return businessTraffic_2022.getColumnValuesInTable(getVals());
        else if (year.equals("2025")) return businessTraffic_2025.getColumnValuesInTable(getVals());
        else throw new Net2PlanException("Not a valid year");
    }

    public List<Double> getDownstream4GTrafficGbps_n(String year)
    {
        if (year.equals("2019")) return r4GTraffic_2019.getColumnValuesInTable(getVals());
        else if (year.equals("2022")) return r4GTraffic_2022.getColumnValuesInTable(getVals());
        else if (year.equals("2025")) return r4GTraffic_2025.getColumnValuesInTable(getVals());
        else throw new Net2PlanException("Not a valid year");
    }

    public List<Double> getDownstream5GTrafficGbps_n(String year)
    {
        if (year.equals("2019")) return r5GTraffic_2019.getColumnValuesInTable(getVals());
        else if (year.equals("2022")) return r5GTraffic_2022.getColumnValuesInTable(getVals());
        else if (year.equals("2025")) return r5GTraffic_2025.getColumnValuesInTable(getVals());
        else throw new Net2PlanException("Not a valid year");
    }

    public List<Double> getTotalTrafficGbps_n(String year)
    {
        if (year.equals("2019")) return totalTraffic_2019.getColumnValuesInTable(getVals());
        else if (year.equals("2022")) return totalTraffic_2022.getColumnValuesInTable(getVals());
        else if (year.equals("2025")) return totalTraffic_2025.getColumnValuesInTable(getVals());
        else throw new Net2PlanException("Not a valid year");
    }


	public void checkIntraSheetValidity()
	{
		final List<String> nodeNames = getNodeNames();
		final List<NODETYPE> nodeTypes = getNodeTypes();
		final List<Double> resTraff2019 = getDownstreamResidentialTrafficGbps_n("2019");
		final List<Double> busTraff2019 = getDownstreamBusinessTrafficGbps_n("2019");
		final List<Double> r4GTraff2019 = getDownstream4GTrafficGbps_n("2019");
		final List<Double> r5GTraff2019 = getDownstream5GTrafficGbps_n("2019");

		if (nodeNames == null) throw new Net2PlanException("Node names column not read");
		if (nodeTypes == null) throw new Net2PlanException("Node types  column not read");

		if (resTraff2019 == null) throw new Net2PlanException("Residential traffic column not read");
		if (busTraff2019 == null) throw new Net2PlanException("Businnes traffic column not read");
		if (r4GTraff2019 == null) throw new Net2PlanException("4G traffic column not read");
		if (r5GTraff2019 == null) throw new Net2PlanException("5G column not read");

		final int numElements = this.getNumRows();
		if (nodeTypes.size() != numElements) throw new Net2PlanException("Not all the columns have the same size");
		if (resTraff2019.size() != numElements) throw new Net2PlanException("Not all the columns have the same size");
		if (busTraff2019.size() != numElements) throw new Net2PlanException("Not all the columns have the same size");
		if (r4GTraff2019.size() != numElements) throw new Net2PlanException("Not all the columns have the same size");
		if (r5GTraff2019.size() != numElements) throw new Net2PlanException("Not all the columns have the same size");

//		if (Math.abs(weightA1Traffic.stream().mapToDouble(v -> v).sum() - 1) > 1e-3) throw new Net2PlanException("Weight A1 column does not sum 1");
//		if (Math.abs(weightA2Traffic.stream().mapToDouble(v -> v).sum() - 1) > 1e-3) throw new Net2PlanException("Weight A2 column does not sum 1");
//		if (Math.abs(weightA3Traffic.stream().mapToDouble(v -> v).sum() - 1) > 1e-3) throw new Net2PlanException("Weight A3 column does not sum 1");
		if (nodeNames.size() != (new HashSet<>(nodeNames)).size()) throw new Net2PlanException("Node names are not unique");
	}
}
