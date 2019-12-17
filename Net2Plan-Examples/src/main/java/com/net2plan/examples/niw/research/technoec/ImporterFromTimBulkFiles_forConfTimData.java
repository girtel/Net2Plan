/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/
package com.net2plan.examples.niw.research.technoec;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.net2plan.examples.niw.research.technoec.TimExcel_NodeListSheet_forConfTimData.NODETYPE;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.SystemUtils;
import com.net2plan.niw.ExcelReader;
import com.net2plan.niw.OadmArchitecture_generic;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNode;
import com.net2plan.niw.WServiceChainRequest;
import com.net2plan.niw.WVnfType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Quadruple;
import com.net2plan.utils.Triple;

public class ImporterFromTimBulkFiles_forConfTimData implements IAlgorithm
{
	public static String VNFTYPE_GENERICSERVER = "GenericServer";
	public static String VNFTYPE_UPF = "UserPlaneFunction";
	public static String VNFTYPE_CACHECOMMONAMENANDMCEN = "GenericCacheSystem";
	public static String ATTNAME_WNODE_A1TRAFFICWEIGHT = "weightA1Traffic";
	public static String ATTNAME_WNODE_A2TRAFFICWEIGHT = "weightA2Traffic";
	public static String ATTNAME_WNODE_A3TRAFFICWEIGHT = "weightA3Traffic";

	public enum CACHINGTYPE
	{
		AMENANDMCEN, ONLYCLOSESTMCEN
	};

	public enum MH_USERSERVICES
	{
		// Definition of the traffic types
		A1_FIXEDRESIDENTIAL(1, CACHINGTYPE.ONLYCLOSESTMCEN),
		A1_FIXEDBUSINESS(1, CACHINGTYPE.ONLYCLOSESTMCEN),
		A1_MOBILE4G5G(1, CACHINGTYPE.ONLYCLOSESTMCEN),
		A2_FIXEDRESIDENTIAL(2, CACHINGTYPE.ONLYCLOSESTMCEN),
		A2_FIXEDBUSINESS(2, CACHINGTYPE.ONLYCLOSESTMCEN),
		A2_MOBILE4G5G(2, CACHINGTYPE.ONLYCLOSESTMCEN),
		A3_FIXEDRESIDENTIAL(3, CACHINGTYPE.AMENANDMCEN),
		A3_FIXEDBUSINESS(3, CACHINGTYPE.AMENANDMCEN),
		A3_MOBILE4G5G(3, CACHINGTYPE.AMENANDMCEN);

		final private int trafficTypeA1A2A3;
		final private CACHINGTYPE upfType1Or2;

		private MH_USERSERVICES(int trafficTypeA1A2A3, CACHINGTYPE cachingAppliedToService)
		{
			this.trafficTypeA1A2A3 = trafficTypeA1A2A3;
			this.upfType1Or2 = cachingAppliedToService;
		}

		public boolean isA1Traffic()
		{
			return getTrafficTypeA1A2A3() == 1;
		}

		public boolean isA2Traffic()
		{
			return getTrafficTypeA1A2A3() == 2;
		}

		public boolean isA3Traffic()
		{
			return getTrafficTypeA1A2A3() == 3;
		}

		public int getTrafficTypeA1A2A3()
		{
			return this.trafficTypeA1A2A3;
		}

		public boolean isFixedRedidential()
		{
			return this == A1_FIXEDRESIDENTIAL || this == A2_FIXEDRESIDENTIAL|| this == A3_FIXEDRESIDENTIAL;
		}

		public boolean isFixedBusiness()
		{
			return this == A1_FIXEDBUSINESS || this == A2_FIXEDBUSINESS || this == A3_FIXEDBUSINESS;
		}
		
		public boolean isMobile4G5G ()
		{
			return this == A1_MOBILE4G5G || this == A2_MOBILE4G5G || this == A3_MOBILE4G5G;
		}

	}

	private InputParameter excelFilePath = new InputParameter("excelFilePath", "resources/excelFiles/Traffic_DenseUrbanMetro_M-H_D2.3.xlsx", "The path to the Excel file in the TIM format");
	private InputParameter amenDefaultCpuRamGbHdGb = new InputParameter("amenDefaultCpuRamGbHdGb", "1e12 1e12 1e12", "Space separated amount of IT resources in the AMENs");
	private InputParameter mcenDefaultCpuRamGbHdGb = new InputParameter("mcenDefaultCpuRamGbHdGb", "1e12 1e12 1e12", "Space separated amount of IT resources in the MCENs not connected to BB");
	private InputParameter mcenBbDefaultCpuRamGbHdGb = new InputParameter("mcenBbDefaultCpuRamGbHdGb", "1e12 1e12 1e12", "Space separated amount of IT resources in the MCENs connected to BB");
    private InputParameter yearDataForAllTrafficTypes = new InputParameter("yearDataForAllTrafficTypes", "#select# 2019 2022 2025", "Available yearly data for all the traffic types.");
	private InputParameter perHourTrafficFactorRespectToPeakInFixedResidentialTraffic = new InputParameter("perHourTrafficFactorRespectToPeakInResidentialTraffic",
			"0.739 0.251 0.063 0.024 0.027 0.026 0.066 0.193 0.322 0.383 0.330 0.222 0.224 0.356 0.480 0.480 0.414 0.385 0.420 0.546 0.694 0.844 1.000 0.960",
			"Space separated values, between 0 and 1, indicating, for the residential traffic, the fraction of traffic intensity in each hour, respect to the traffic of the same service in the its busy hour");
	private InputParameter perHourTrafficFactorRespectToPeakInFixedBusinessTraffic = new InputParameter("perHourTrafficFactorRespectToPeakInBusinessTraffic",
			"0.179 0.141 0.09 0.051 0.038 0.013 0.000 0.002 0.256 0.641 0.846 0.949 1.000 0.923 0.692 0.667 0.769 0.667 0.564 0.436 0.282 0.180 0.154 0.154",
			"Space separated values, between 0 and 1, indicating, for the residential traffic, the fraction of traffic intensity in each hour, respect to the traffic of the same service in the its busy hour");
	private InputParameter perHourTrafficFactorRespectToPeakInMobileTraffic = new InputParameter("perHourTrafficFactorRespectToPeakInMobileTraffic",
			"0.55 0.32 0.18 0.13 0.09 0.09 0.15 0.35 0.58 0.61 0.63 0.67 0.69 0.82 0.89 0.85 0.84 0.86 0.9 0.92 0.9 1.0 0.98 0.8 0.55",
			"Space separated values, between 0 and 1, indicating, for the residential traffic, the fraction of traffic intensity in each hour, respect to the traffic of the same service in the its busy hour");
	private InputParameter a1Traffic_fractionRespectToTotal = new InputParameter("a1Traffic_fractionRespectToTotal", 0.1, "Percentage of traffic of type A1: P2P", 0, true, 1.0, true);
	private InputParameter a2Traffic_fractionRespectToTotal = new InputParameter("a2Traffic_fractionRespectToTotal", 0.3, "Percentage of traffic that is of type A2: \"heterogeneous web traffic\"", 0, true, 1.0, true);
	private InputParameter fiberDefaultValidSlotRanges = new InputParameter("fiberDefaultValidSlotRanges", "0 , 15000",
			"This is a list of 2*K elements, being K the number of contiguous ranges. First number in a range is \r\n"
					+ "	 * the id of the initial optical slot, second number is the end. For instance, a range setting [0,  300, 320, 360] means that the fiber is able to propagate the \r\n"
					+ "	 * optical slots from 0 to 300 both included, and from 320 to 360 both included.  ");
	private InputParameter opticalSwiInputParameter = new InputParameter("opticalSwiInputParameter","#select# option2 option1","Option 1: All = ROADM; Option 2: MCEN = ROADM, AMEN (>2) AMEN (=2) = Filterless D&W");


    @Override
	public String executeAlgorithm(NetPlan np, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		// First of all, initialize all parameters
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		final WNet res = importWNetFromExcel();
		np.assignFrom(res.getNe());
		return "Ok";
	}

	public WNet importWNetFromExcel()
	{
		System.out.println("###################### Importing file Reading " + excelFilePath.getString() + " ######################");

		final File excelFile = new File(excelFilePath.getString());

        final SortedMap<String, Object[][]> fileData = new TreeMap<>(ExcelReader.readFile(excelFile));
		Configuration.setOption("precisionFactor", new Double(1e-6).toString());
		Configuration.precisionFactor = 1e-6;
		final WNet net = WNet.createEmptyDesign(true ,true);

//		if (a1Traffic_fractionRespectToTotal.getDouble() + a2Traffic_fractionRespectToTotal.getDouble() > 1) throw new Net2PlanException("Traffic fractions cannot exceed 100%");
		final List<Double> trafficProfileFixedResidentialTraffic = Stream.of(perHourTrafficFactorRespectToPeakInFixedResidentialTraffic.getString().split(" ")).map(v -> Double.parseDouble(v)).collect(Collectors.toCollection(ArrayList::new));
		final List<Double> trafficProfileFixedBusinessTraffic = Stream.of(perHourTrafficFactorRespectToPeakInFixedBusinessTraffic.getString().split(" ")).map(v -> Double.parseDouble(v)).collect(Collectors.toCollection(ArrayList::new));
		final List<Double> trafficProfileMobile4g5gTraffic = Stream.of(perHourTrafficFactorRespectToPeakInMobileTraffic.getString().split(" ")).map(v -> Double.parseDouble(v)).collect(Collectors.toCollection(ArrayList::new));

		if (trafficProfileFixedResidentialTraffic.stream().anyMatch(e -> e < 0 || e > 1)) throw new Net2PlanException("Traffic profiles must have numbers between zero and 1");
		if (trafficProfileFixedBusinessTraffic.stream().anyMatch(e -> e < 0 || e > 1)) throw new Net2PlanException("Traffic profiles must have numbers between zero and 1");
		if (trafficProfileMobile4g5gTraffic.stream().anyMatch(e -> e < 0 || e > 1)) throw new Net2PlanException("Traffic profiles must have numbers between zero and 1");

		/* Nodes sheet */
		final TimExcel_NodeListSheet_forConfTimData nodeListSheet = new TimExcel_NodeListSheet_forConfTimData(fileData);
		nodeListSheet.checkIntraSheetValidity();

        // Import Per Node traffic
        final List<Double> downResidentialTrafficGbps_n = nodeListSheet.getDownstreamResidentialTrafficGbps_n(yearDataForAllTrafficTypes.getString());
        final List<Double> downBusinessTrafficGbps_n = nodeListSheet.getDownstreamBusinessTrafficGbps_n(yearDataForAllTrafficTypes.getString());
        final List<Double> down4GTraffiGbps_n = nodeListSheet.getDownstream4GTrafficGbps_n(yearDataForAllTrafficTypes.getString());
        final List<Double> down5GTrafficGbps_n = nodeListSheet.getDownstream5GTrafficGbps_n(yearDataForAllTrafficTypes.getString());
        final List<Double> downTotalTrafficGbps_n = nodeListSheet.getTotalTrafficGbps_n(yearDataForAllTrafficTypes.getString());

        // Import Per node traffic types
        final TimExcel_A1TrafficSheet a1TypeTraffic = new TimExcel_A1TrafficSheet(fileData);
        nodeListSheet.checkIntraSheetValidity();

        final List<Double> a1DownTotalTrafficGbps_n = a1TypeTraffic.getDownstreamUPFTrafficGbps(yearDataForAllTrafficTypes.getString());
        final List<Double> a1UpTotalTrafficGbps_n = a1TypeTraffic.getUpstreamUPFTrafficGbps(yearDataForAllTrafficTypes.getString());
        final double a1SummingAllNodesTotalDownTrafficGbps = a1DownTotalTrafficGbps_n.stream().mapToDouble(Double::doubleValue).sum();

        final TimExcel_A2TrafficSheet a2TypeTrafficGbps = new TimExcel_A2TrafficSheet(fileData);
        nodeListSheet.checkIntraSheetValidity();

        final List<Double> a2DownTotalTrafficGbps_n = a2TypeTrafficGbps.getDownstreamTotalTraffic(yearDataForAllTrafficTypes.getString());
        final List<Double> a2UpTotalTrafficGbps_n = a2TypeTrafficGbps.getUpstreamTotalTraffic(yearDataForAllTrafficTypes.getString());
        final double a2SummingAllNodesTotalDownTrafficGbps = a2DownTotalTrafficGbps_n.stream().mapToDouble(Double::doubleValue).sum();

        final TimExcel_A3TrafficSheet a3TypeTrafficGbps = new TimExcel_A3TrafficSheet(fileData);
        nodeListSheet.checkIntraSheetValidity();

        final List<Double> a3DownTotalTrafficGbps_n = a3TypeTrafficGbps.getDownstreamTotalTraffic(yearDataForAllTrafficTypes.getString());
        final List<Double> a3UpTotalTrafficGbps_n = a3TypeTrafficGbps.getUpstreamTotalTraffic(yearDataForAllTrafficTypes.getString());
        final double a3SummingAllNodesTotalDownTrafficGbps = a3DownTotalTrafficGbps_n.stream().mapToDouble(Double::doubleValue).sum();

        final double totalDownTrafficSummingA1A2A3InNetworkGbps = a1SummingAllNodesTotalDownTrafficGbps + a2SummingAllNodesTotalDownTrafficGbps + a3SummingAllNodesTotalDownTrafficGbps;

        // Add data to nodes
//        final List<Double> a1Weights = nodeListSheet.getWeightA1Traffic();
//        final List<Double> a2Weights = nodeListSheet.getWeightA2Traffic();
//        final List<Double> a3Weights = nodeListSheet.getWeightA3Traffic();

        final List<Double> a1WeightRespectToTotal_n = a1DownTotalTrafficGbps_n.stream().map(v-> v/a1SummingAllNodesTotalDownTrafficGbps).collect(Collectors.toCollection(ArrayList::new));
        final List<Double> a2WeightsRespectToTotal_n = a2DownTotalTrafficGbps_n.stream().map(v-> v/a2SummingAllNodesTotalDownTrafficGbps).collect(Collectors.toCollection(ArrayList::new));
        final List<Double> a3WeightsRespectToTotal_n = a3DownTotalTrafficGbps_n.stream().map(v-> v/a3SummingAllNodesTotalDownTrafficGbps).collect(Collectors.toCollection(ArrayList::new));

		for (int rowAfterTitleRow = 0; rowAfterTitleRow < nodeListSheet.getNumRows(); rowAfterTitleRow++)
		{
			final double xCoord = Math.cos(2 * Math.PI / nodeListSheet.getNumRows());
			final double yCoord = Math.sin(2 * Math.PI / nodeListSheet.getNumRows());
			final String nodeName = nodeListSheet.getNodeNames().get(rowAfterTitleRow);
			final NODETYPE nodeType = nodeListSheet.getNodeTypes().get(rowAfterTitleRow);
			final WNode n = net.addNode(xCoord, yCoord, nodeName, nodeType.getColValue());

			n.setIsConnectedToNetworkCore(nodeType.isMcenBb());
			n.setPoputlation(0.0);
			final String cpuRamVals = (nodeType.isAmen() ? amenDefaultCpuRamGbHdGb.getString() : (nodeType.isMcenNotBb() ? mcenDefaultCpuRamGbHdGb.getString() : mcenBbDefaultCpuRamGbHdGb.getString()));
			final List<Double> cpuRamValsDouble = Stream.of(cpuRamVals.split(" ")).map(v -> Double.parseDouble(v)).collect(Collectors.toCollection(ArrayList::new));
			n.setTotalNumCpus(cpuRamValsDouble.get(0));
			n.setTotalRamGB(cpuRamValsDouble.get(1));
			n.setTotalHdGB(cpuRamValsDouble.get(2));
			n.setArbitraryParamString("");
			n.setAttributeAsDouble(ATTNAME_WNODE_A1TRAFFICWEIGHT, a1WeightRespectToTotal_n.get(rowAfterTitleRow));
			n.setAttributeAsDouble(ATTNAME_WNODE_A2TRAFFICWEIGHT, a2WeightsRespectToTotal_n.get(rowAfterTitleRow));
			n.setAttributeAsDouble(ATTNAME_WNODE_A3TRAFFICWEIGHT, a3WeightsRespectToTotal_n.get(rowAfterTitleRow));
		}
		assert nodeListSheet.getNumRows() == net.getNodes().size();

		final TimExcel_LinksSheet linksListSheet = new TimExcel_LinksSheet(fileData, nodeListSheet.getNodeNames());
		linksListSheet.checkIntraSheetValidity(nodeListSheet.getNodeNames());
		for (int rowAfterTitleRow = 0; rowAfterTitleRow < linksListSheet.getNumRows(); rowAfterTitleRow++)
		{
			final String linkUid = linksListSheet.getLinkUniqueCode().get(rowAfterTitleRow);
			final String nodeNameA = linksListSheet.getNodeNameA().get(rowAfterTitleRow);
			final String nodeNameB = linksListSheet.getNodeNameB().get(rowAfterTitleRow);
			final double distanceKm = linksListSheet.getLinkDistanceKm().get(rowAfterTitleRow);
			final WNode a = net.getNodeByName(nodeNameA).orElseThrow(() -> new Net2PlanException("Unkown node name: " + nodeNameA));
			final WNode b = net.getNodeByName(nodeNameB).orElseThrow(() -> new Net2PlanException("Unkown node name: " + nodeNameB));
			final List<Integer> validOpticalSlotRangesAsList = Stream.of(fiberDefaultValidSlotRanges.getString().split(",")).map(d -> Integer.parseInt(d.trim())).map(d -> d.intValue()).collect(Collectors.toList());
			final List<Pair<Integer,Integer>> validOpticalSlotRanges = new ArrayList<> ();
			if (validOpticalSlotRangesAsList.isEmpty() || validOpticalSlotRangesAsList.size() % 2 != 0) throw new Net2PlanException ("Wrong slot ranges");
			final Iterator<Integer> it = validOpticalSlotRangesAsList.iterator();
			while (it.hasNext())
			{
				final int first = it.next();
				final int second = it.next();
				validOpticalSlotRanges.add(Pair.of(first, second));
			}
			final Pair<WFiber, WFiber> pair = net.addFiber(a, b, validOpticalSlotRanges, distanceKm, true);
			for (WFiber e : Arrays.asList(pair.getFirst(), pair.getSecond()))
			{
				if (e == null) continue;
				e.setArbitraryParamString(linkUid);
			}
		}

		for (WNode n : net.getNodes()) {
            setOpticalSwitchNodeType(n,opticalSwiInputParameter.getString());
        }

		/* Create the types of VNFs used */
		net.addOrUpdateVnfType(new WVnfType(VNFTYPE_UPF, Double.MAX_VALUE, 0, 0, 0, 0.0, Optional.empty(), null));
		net.addOrUpdateVnfType(new WVnfType(VNFTYPE_GENERICSERVER, Double.MAX_VALUE, 0, 0, 0, 0.0, Optional.empty(), null));
		net.addOrUpdateVnfType(new WVnfType(VNFTYPE_CACHECOMMONAMENANDMCEN, Double.MAX_VALUE, 0, 0, 0, 0.0, Optional.empty(), null));

		/* Create all the non-vertical service chain requests */
		final List<WNode> allNodes = net.getNodes();

		final List<Quadruple<Double,Double,Double,Double>> weight01ApplicableUpDown_ofFixedResFixedBusMobileInNodeAndTotalDownGbpsForCheck_n = new ArrayList<> ();
		for (int contNode = 0; contNode < nodeListSheet.getNumRows(); contNode++)
		{
			final WNode originNode = allNodes.get(contNode);
			final SortedSet<WNode> initialNodes = Sets.newTreeSet(Arrays.asList(originNode));
			final SortedSet<WNode> endNodes = new TreeSet<>(allNodes);
			final double fixedResDownThisNodeGbps = downResidentialTrafficGbps_n.get(contNode);
			final double fixedBusDownThisNodeGbps = downBusinessTrafficGbps_n.get(contNode);
			final double mobileDownThisNodeGbps = down4GTraffiGbps_n.get(contNode) + down5GTrafficGbps_n.get(contNode);
			final double totalDownThisNodeGbps = fixedResDownThisNodeGbps + fixedBusDownThisNodeGbps + mobileDownThisNodeGbps;
			weight01ApplicableUpDown_ofFixedResFixedBusMobileInNodeAndTotalDownGbpsForCheck_n.add(Quadruple.of(fixedResDownThisNodeGbps/totalDownThisNodeGbps, fixedBusDownThisNodeGbps/totalDownThisNodeGbps, mobileDownThisNodeGbps/totalDownThisNodeGbps, totalDownThisNodeGbps));
		}
		assert weight01ApplicableUpDown_ofFixedResFixedBusMobileInNodeAndTotalDownGbpsForCheck_n.stream().allMatch(q->Math.abs(q.getFirst() + q.getSecond() + q.getThird() - 1) < 1e-3);

		for (MH_USERSERVICES us : MH_USERSERVICES.values())
		{
			final List<String> listVnfTypesUpstreamThisService;
			final List<Double> defaultSequenceOfExpansionFactorsRespectToInjectionUpstream;
			final List<Double> maxLatencyFromInitialToVnfStartUpstreamMs;
			if (us.isA1Traffic())
			{
				/* Service chains allocating this request will have to traverse ONE instance of VNF type "cache" */
				listVnfTypesUpstreamThisService = Arrays.asList(VNFTYPE_UPF);
				defaultSequenceOfExpansionFactorsRespectToInjectionUpstream = Arrays.asList(1.0);
				maxLatencyFromInitialToVnfStartUpstreamMs = Arrays.asList(Double.MAX_VALUE, Double.MAX_VALUE);
			} else if (us.isA2Traffic())
			{
				/* Service chains allocating this request will have to traverse TWO instances of VNF type "cache" */
				listVnfTypesUpstreamThisService = Arrays.asList(VNFTYPE_UPF , VNFTYPE_GENERICSERVER);
				defaultSequenceOfExpansionFactorsRespectToInjectionUpstream = Arrays.asList(1.0 , 1.0);
				maxLatencyFromInitialToVnfStartUpstreamMs = Arrays.asList(Double.MAX_VALUE, Double.MAX_VALUE , Double.MAX_VALUE);
			} else if (us.isA3Traffic())
			{
				/* Service chains allocating this request will have to traverse TWO instances of VNF type "cache" */
				listVnfTypesUpstreamThisService = Arrays.asList(VNFTYPE_UPF , VNFTYPE_CACHECOMMONAMENANDMCEN);
				defaultSequenceOfExpansionFactorsRespectToInjectionUpstream = Arrays.asList(1.0 , 1.0);
				maxLatencyFromInitialToVnfStartUpstreamMs = Arrays.asList(Double.MAX_VALUE, Double.MAX_VALUE , Double.MAX_VALUE);
			} else throw new RuntimeException();
			if (listVnfTypesUpstreamThisService == null) throw new RuntimeException();
			for (int contNode = 0; contNode < nodeListSheet.getNumRows(); contNode++)
			{
				final WNode originNode = allNodes.get(contNode);
				final SortedSet<WNode> initialNodes = Sets.newTreeSet(Arrays.asList(originNode));
				final SortedSet<WNode> endNodes = new TreeSet<>(allNodes);
				final WServiceChainRequest scReqUps = net.addServiceChainRequest(initialNodes, endNodes, listVnfTypesUpstreamThisService, defaultSequenceOfExpansionFactorsRespectToInjectionUpstream, maxLatencyFromInitialToVnfStartUpstreamMs, true, us.name());
				final WServiceChainRequest scReqDowns = net.addServiceChainRequest(endNodes, initialNodes, Lists.reverse(listVnfTypesUpstreamThisService), Lists.reverse(defaultSequenceOfExpansionFactorsRespectToInjectionUpstream), Lists.reverse(maxLatencyFromInitialToVnfStartUpstreamMs), false, us.name());
				scReqUps.setBidirectionalPair(scReqDowns);

				final double downTrafficSummingResidentialBusinessAndMobileGbps;
				final double upTrafficSummingResidentialBusinessAndMobileGbps;

//                System.out.println("Traffic type: " + us.getTrafficTypeA1A2A3());

                if (us.isA1Traffic())
				{
                    downTrafficSummingResidentialBusinessAndMobileGbps = a1DownTotalTrafficGbps_n.get(contNode);
					upTrafficSummingResidentialBusinessAndMobileGbps = a1UpTotalTrafficGbps_n.get(contNode);
				} else if (us.isA2Traffic())
				{
					downTrafficSummingResidentialBusinessAndMobileGbps = a2DownTotalTrafficGbps_n.get(contNode);
					upTrafficSummingResidentialBusinessAndMobileGbps = a2UpTotalTrafficGbps_n.get(contNode);
				} else if (us.isA3Traffic())
				{
					downTrafficSummingResidentialBusinessAndMobileGbps = a3DownTotalTrafficGbps_n.get(contNode);
					upTrafficSummingResidentialBusinessAndMobileGbps = a3UpTotalTrafficGbps_n.get(contNode);
				} else throw new RuntimeException();
				final double fractionToApplyResidentialBusinnesFixedOrMobile;
				final List<Double> timeProfileOfTrafficInFractions;
				if (us.isFixedRedidential())
				{
					fractionToApplyResidentialBusinnesFixedOrMobile = weight01ApplicableUpDown_ofFixedResFixedBusMobileInNodeAndTotalDownGbpsForCheck_n.get(contNode).getFirst();
					timeProfileOfTrafficInFractions = trafficProfileFixedResidentialTraffic;
				}
				else if (us.isFixedBusiness())
				{
                    fractionToApplyResidentialBusinnesFixedOrMobile = weight01ApplicableUpDown_ofFixedResFixedBusMobileInNodeAndTotalDownGbpsForCheck_n.get(contNode).getSecond();
					timeProfileOfTrafficInFractions = trafficProfileFixedBusinessTraffic;
				}
				else if (us.isMobile4G5G())
				{
					timeProfileOfTrafficInFractions = trafficProfileMobile4g5gTraffic;
					fractionToApplyResidentialBusinnesFixedOrMobile = weight01ApplicableUpDown_ofFixedResFixedBusMobileInNodeAndTotalDownGbpsForCheck_n.get(contNode).getThird();
				}
				else throw new RuntimeException("Traffic type: " + us.getTrafficTypeA1A2A3());
				
             final List<Double> trafficPerTimeSlotToApplyDownstream = timeProfileOfTrafficInFractions.stream()
                     .map(v -> v * downTrafficSummingResidentialBusinessAndMobileGbps * fractionToApplyResidentialBusinnesFixedOrMobile)
                     .collect(Collectors.toList());
             final List<Double> trafficPerTimeSlotToApplyUpstream = timeProfileOfTrafficInFractions.stream()
                   .map(v -> v * upTrafficSummingResidentialBusinessAndMobileGbps * fractionToApplyResidentialBusinnesFixedOrMobile)
                   .collect(Collectors.toList());

				scReqDowns.setTimeSlotNameAndInitialInjectionIntensityInGbpsList(addNameWithHourToList(trafficPerTimeSlotToApplyDownstream));
				scReqUps.setTimeSlotNameAndInitialInjectionIntensityInGbpsList(addNameWithHourToList(trafficPerTimeSlotToApplyUpstream));
			}
		}

		/* Checks */
		final double totalDownsTrafficInBusyhour = net.getServiceChainRequests().stream().filter(sc -> sc.isDownstream()).map(sc -> sc.getTimeSlotNameAndInitialInjectionIntensityInGbpsList())
				.mapToDouble(l -> l.stream().mapToDouble(p -> p.getSecond()).max().orElse(0.0)).sum();
		final double totalDownTrafficInAllNodesFromExcel = downTotalTrafficGbps_n.stream().mapToDouble(v-> v).sum();
		assert Math.abs(totalDownsTrafficInBusyhour - totalDownTrafficSummingA1A2A3InNetworkGbps) < 1e-3;
		assert Math.abs(totalDownsTrafficInBusyhour - totalDownTrafficInAllNodesFromExcel) < 1e-3;
		double totalDownsTrafficA1 = net.getServiceChainRequests().stream().filter(sc -> sc.isDownstream()).filter(scr -> MH_USERSERVICES.valueOf(scr.getQosType()).isA1Traffic())
				.map(sc -> sc.getTimeSlotNameAndInitialInjectionIntensityInGbpsList()).mapToDouble(l -> l.stream().mapToDouble(p -> p.getSecond()).max().orElse(0.0)).sum();
		assert Math.abs(totalDownsTrafficA1 - totalDownTrafficSummingA1A2A3InNetworkGbps * a1Traffic_fractionRespectToTotal.getDouble()) < 1e-3;
		final double totalDownsTrafficA2 = net.getServiceChainRequests().stream().filter(sc -> sc.isDownstream()).filter(sc -> MH_USERSERVICES.valueOf(sc.getQosType()).isA2Traffic())
				.map(sc -> sc.getTimeSlotNameAndInitialInjectionIntensityInGbpsList()).mapToDouble(l -> l.stream().mapToDouble(p -> p.getSecond()).max().orElse(0.0)).sum();
		assert Math.abs(totalDownsTrafficA2 - totalDownTrafficSummingA1A2A3InNetworkGbps * a2Traffic_fractionRespectToTotal.getDouble()) < 1e-3;

		return net;
	}

	@Override
	public String getDescription()
	{
		return "This importer file converts from an Excel file as provided by TIM, and creates the N2P file with all the information there";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	private static double readDouble(Object[] cells, int index, Double... defaultVal)
	{
		if (index >= cells.length) return defaultVal[0];
		if (cells[index] == null) if (defaultVal.length > 0) return defaultVal[0];
		else throw new Net2PlanException("Cell unkown instance " + (cells[index]).getClass().getName());
		if (cells[index] instanceof Number) return ((Number) cells[index]).doubleValue();
		if (cells[index] instanceof String) return Double.parseDouble((String) cells[index]);
		if (defaultVal.length > 0) return defaultVal[0];
		else throw new Net2PlanException("Cell unkown instance " + (cells[index]).getClass().getName());
	}

	private static int readInt(Object[] cells, int index)
	{
		if (index >= cells.length) throw new Net2PlanException("Unexisting cell of column: " + index + ". Num columns in this row: " + cells.length);
		if (cells[index] == null) return 0;
		if (cells[index] instanceof Number) return ((Number) cells[index]).intValue();
		if (cells[index] instanceof String) return Integer.parseInt((String) cells[index]);
		throw new Net2PlanException("Cell unkown instance " + (cells[index]).getClass().getName());
	}

	private static String readString(Object[] cells, int index, String... defaultVal)
	{
		if (index >= cells.length) return defaultVal[0];
		if (cells[index] == null) if (defaultVal.length > 0) return defaultVal[0];
		else throw new Net2PlanException("Cell unkown instance " + (cells[index]).getClass().getName());
		if (cells[index] instanceof Number) return ((Number) cells[index]).toString();
		if (cells[index] instanceof String) return (String) cells[index];
		if (defaultVal.length > 0) return defaultVal[0];
		else throw new Net2PlanException("Cell unkown instance " + (cells[index]).getClass().getName());
	}

	private static boolean readBoolean(Object[] cells, int index)
	{
		return readDouble(cells, index) != 0;
	}

	/* JLRG 12/06 */
	private static List<Integer> readIntegerList(Object[] cells, int index, String separator)
	{
		final String st = readString(cells, index);
		return Arrays.asList(st.split(separator)).stream().map(s -> s.trim()).map(s -> Integer.parseInt(s)).collect(Collectors.toCollection(ArrayList::new));
	}

	private static List<Double> readDoubleList(Object[] cells, int index, String separator, List<Double>... defaultVal)
	{
		final String st = readString(cells, index, "");
		if (st.length() == 0) return defaultVal[0];
		else return Arrays.asList(st.split(separator)).stream().map(s -> s.trim()).map(s -> Double.parseDouble(s)).collect(Collectors.toCollection(ArrayList::new));
	}

	private static List<String> readStringList(Object[] cells, int index, String separator)
	{
		final String st = readString(cells, index);
		return Arrays.asList(st.split(separator)).stream().map(s -> s.trim()).collect(Collectors.toCollection(ArrayList::new));
	}

	private static List<Pair<String, Double>> addNameWithHourToList(List<Double> list)
	{
		final List<Pair<String, Double>> res = new ArrayList<>(list.size());
		for (Double v : list)
			res.add(Pair.of("" + res.size(), v));
		return res;
	}

	private void addIconToNodeTypeIpLayer(WNet net, WNode node)
    {
		if (net.getWdmLayer().isPresent())
		{
	        if(node.getOpticalSwitchingArchitecture().isPotentiallyWastingSpectrum())
	            try 
	        	{
	                node.getNe().setUrlNodeIcon(net.getWdmLayer().get().getNe(), new File(SystemUtils.getCurrentDir() + File.separator + "resources" + File.separator + "icons" + File.separator + "dropAndWaste.png").toURI().toURL(), 1.0);
	            } catch (MalformedURLException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
	            }
	        else if (node.getOpticalSwitchingArchitecture().isNeverCreatingWastedSpectrum())
	            try {
	                node.getNe().setUrlNodeIcon(net.getWdmLayer().get().getNe(), new File(SystemUtils.getCurrentDir() + File.separator + "resources" + File.separator + "icons" + File.separator + "roadm.png").toURI().toURL(), 1.0);
	            } catch (MalformedURLException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
	            }
	        else
	            throw new Net2PlanException("Optical Switch Type is not valid");
		}
		
		if (net.getIpLayer().isPresent())
		{
	        if (NODETYPE.isAmen(node)) {
	            try {
	                node.getNe().setUrlNodeIcon(net.getIpLayer().get().getNe(), new File(SystemUtils.getCurrentDir() + File.separator + "resources" + File.separator + "icons" + File.separator + "amen.png").toURI().toURL(),1.0);
	            } catch (MalformedURLException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
	            }
	        }
	        else if (NODETYPE.isMcenNotBb(node)) {
	            try {
	                node.getNe().setUrlNodeIcon(net.getIpLayer().get().getNe(), new File(SystemUtils.getCurrentDir() + File.separator +  "resources" + File.separator + "icons" + File.separator + "mcen.png").toURI().toURL(),1.0);
	            } catch (MalformedURLException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
	            }
	        }
	        else if (NODETYPE.isMcenBb(node)) {
	            try {
	                node.getNe().setUrlNodeIcon(net.getIpLayer().get().getNe(), new File(SystemUtils.getCurrentDir() + File.separator + "resources" + File.separator + "icons" + File.separator + "mcenbb.png").toURI().toURL(),1.0);
	            } catch (MalformedURLException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
	            }
	        }
	        else
	            throw new Net2PlanException("NODETYPE not valid");
		}

    }


    private static void setOpticalSwitchNodeType (WNode node, String option)
    {
    	if (!node.getOpticalSwitchingArchitecture().isOadmGeneric()) throw new Net2PlanException ("Nodes must be of the generic OADM type");
    	final OadmArchitecture_generic.Parameters param = node.getOpticalSwitchingArchitecture().getAsOadmGeneric().getParameters();
        if(option.equals("option1"))
        {
            if(NODETYPE.isMcenBbOrNot(node))
                param.setArchitectureTypeAsBroadcastAndSelect();
            else if (NODETYPE.isAmen(node))
                param.setArchitectureTypeAsBroadcastAndSelect();
            else
                new Net2PlanException("Node type is wrong.");
            node.getOpticalSwitchingArchitecture().getAsOadmGeneric().updateParameters(param);
        }
        else if(option.equals("option2"))
        {

            if(NODETYPE.isMcenBbOrNot(node))
                param.setArchitectureTypeAsBroadcastAndSelect();
            else if (NODETYPE.isAmen(node) && getNodeDegree(node) > 2)
                param.setArchitectureTypeAsBroadcastAndSelect();
            else if (NODETYPE.isAmen(node) && getNodeDegree(node) <= 2)
                param.setArchitectureTypeAsFilterless();
            else
                new Net2PlanException("Node type is wrong.");
            node.getOpticalSwitchingArchitecture().getAsOadmGeneric().updateParameters(param);
        }
        else
            new Net2PlanException("The option is not correct.");

    }




    private static int getNodeDegree(WNode node)
    {
        return node.getIncomingFibers().size();
    }


}
