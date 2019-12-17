/* Problems: Islands are not linear (seldom they are). Many degree > 2 AMENs Some AMEN islands are huge (41 nodes) */

/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/


package com.net2plan.examples.niw.research.technoec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import org.jgrapht.alg.shortestpath.SuurballeKDisjointShortestPaths;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.net2plan.examples.niw.research.technoec.ImporterFromTimBulkFiles_forConfTimData.MH_USERSERVICES;
import com.net2plan.examples.niw.research.technoec.TimExcel_NodeListSheet_forConfTimData.NODETYPE;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.niw.OpticalSpectrumManager;
import com.net2plan.niw.WAbstractNetworkElement;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WIpLink;
import com.net2plan.niw.WLightpath;
import com.net2plan.niw.WLightpathRequest;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNode;
import com.net2plan.niw.WServiceChain;
import com.net2plan.niw.WServiceChainRequest;
import com.net2plan.niw.WVnfInstance;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Quintuple;
import com.net2plan.utils.Triple;

import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;



public class JltTecnoec1_capacityPlanningAlgorithm implements IAlgorithm
{
	public static String VNFINSTANCENAME_FAKEINSTANCE = "InstanceOutsideMetro";
	public static String VNFINSTANCENAME_REALINSTANCE = "InstanceInThisNode";

	private OpticalSpectrumManager osm;

	private org.jgrapht.graph.DirectedWeightedMultigraph<WNode, Object> wdmShortestPathKmCalculator_graph;
	private org.jgrapht.alg.shortestpath.KShortestSimplePaths<WNode, Object> wdmKShortestPathKmCalculator;
	private org.jgrapht.alg.shortestpath.DijkstraShortestPath<WNode, Object> wdmShortestPathKmCalculator;

	private Map<Pair<WNode, Set<NODETYPE>>, Optional<WNode>> cache_closestNodeOfGivenType = new HashMap<>();
	
	
	private InputParameter fiberDefaultValidSlotRanges = new InputParameter("fiberDefaultValidSlotRanges", "0 , 320",
			"This is a list of 2*K elements, being K the number of contiguous ranges. First number in a range is \r\n"
					+ "	 * the id of the initial optical slot, second number is the end. For instance, a range setting [0 300 320 360] means that the fiber is able to propagate the \r\n"
					+ "	 * optical slots from 0 to 300 both included, and from 320 to 360 both included.  ");
//	private InputParameter numParallelFibersPeNodePair = new InputParameter("numParallelFibersPeNodePair", 15 , "Number of fibers to add between each node of the topology" , 0 , Integer.MAX_VALUE);
	
	private InputParameter folderToPlaceResults = new InputParameter("folderToPlaceResults", "./", "The folder where the N2P file, as well as some text files with the results are placed. If empty, nothing is written as output");

	private InputParameter addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible = new InputParameter("addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible", true , "");
	
	private InputParameter indexOfTimeSlotToAplyMinus1MakesWorstCaseAllSlots = new InputParameter("indexOfTimeSlotToAplyMinus1MakesWorstCaseAllSlots", 0, "");

	private InputParameter a1Traffic_fractionExchangedOutsideMetro = new InputParameter("a1Traffic_fractionExchanedOutsideMetro", 0.33, "Fraction of the traffic of this type, that comes/goes outside the metro network", 0, true, 1.0, true);
	private InputParameter a2Traffic_fractionExchangedOutsideMetro = new InputParameter("a2Traffic_fractionExchangedOutsideMetro", 0.8, "Fraction of the traffic of this type, that comes/goes outside the metro network", 0, true, 1.0, true);

	private InputParameter a3Traffic_fractionExchangedOutsideMetro = new InputParameter("a3Traffic_fractionExchangedOutsideMetro", 0.25, "Fraction of the traffic of this type, that comes/goes outside the metro network", 0, true, 1.0, true);
	private InputParameter a3Traffic_fractionOnInitialAmen = new InputParameter("a3Traffic_fractionOnInitialAmen", 0.25, "Fraction of the traffic of this type, that comes/goes from the same AMEN node where the user is", 0, true, 1.0, true);
	private InputParameter a3Traffic_fractionOnNearestMcen = new InputParameter("a3Traffic_fractionOnNearestMcen", 0.25, "Fraction of the traffic of this type, that comes/goes from the MCEN closest to the AMEN node where the user is", 0, true, 1.0,
			true);

	private InputParameter minimumUtilizationToJustifyIpLinkInSmallerTransponder = new InputParameter("minimumUtilizationToJustifyIpLinkInSmallerTransponder", 0.1,
			"IP links with a traffic below this utilization is the smaller (in rate) transponder possible was used, are attempted to be removed, and its traffic groomed", 0, false, 1.0, true);

	private InputParameter ipTopologyConstraints_amen2amenSameIslandAllowed = new InputParameter("ipTopologyConstraints_amen2amenSameIslandAllowed", true,
			"If the IP links between two AMENs in the same AMEN island (i.e. reachable traversing only AMENs) are allowed or not");
	private InputParameter ipTopologyConstraints_amen2amenDifferentIslandAllowed = new InputParameter("ipTopologyConstraints_amen2amenDifferentIslandAllowed", true,
			"If the IP links between two AMENs in different AMEN islands (i.e. not reachable traversing only AMENs) are allowed or not");
	private InputParameter ipTopologyConstraints_amen2mcenNotBorderToAmenIslandAllowed = new InputParameter("ipTopologyConstraints_amen2mcenNotBorderToAmenIslandAllowed", true,
			"If the IP links between an AMEN node, and an MCEN that is not in the frontier of the AMEN island the AMEN belongs to, are allowed or not");

//	private InputParameter transponderOptionsNameRateGbpsCostReachKmNumSlots_amenLps = new InputParameter("transponderOptionsRateGbpsCostReachKmNumSlots_amenLps", "TP1 10 1 101 2 , TP2 25 2 101 4",
//            "Applied for the lps involving an AMEN. Space-separated tuple of transponder name, rate in Gbps, cost, reack in km and number of optical slots, information of different transponders is comma separated");

    private InputParameter transponderOptionsNameRateGbpsCostReachKmNumSlots_amenLps = new InputParameter("transponderOptionsRateGbpsCostReachKmNumSlots_amenLps", "TP2 25 2 101 4",
            "Applied for the lps involving an AMEN. Space-separated tuple of transponder name, rate in Gbps, cost, reack in km and number of optical slots, information of different transponders is comma separated");


    private InputParameter transponderOptionsNameRateGbpsCostReachKmNumSlots_mcen2mcenLps = new InputParameter("transponderOptionsRateGbpsCostReachKmNumSlots_mcen2mcenLps", "TPOther 100 5 200 8",
			"Applied for the lps involving two MCENs. Space-separated tuple of name, rate in Gbps, cost, reack in km and number of optical slots, information of different transponders is comma separated");
//	private InputParameter nodesAreFilterless = new InputParameter("amenNodesAreFilterless", "#select none all-amens all-degree2-amens", "The type of nodes that will have a filterless operation");

	private InputParameter kFactorForWdmShortestPath = new InputParameter("kFactorForWdmShortestPath", 2, "The k-factor in the k-shortest path computation for the lightpaths", 1, Integer.MAX_VALUE);

	private List<AmenGenericIsland> genericAmenIslands;
	private McenGenericIsland mcenIsland;
	private boolean DEBUG = false;
    List<WFiber> originalFibers = new ArrayList<>();
    List<Integer> newFibers = new ArrayList<>();
    List<Integer> newFiberSIndex = new ArrayList<>();
	
	
	@Override
	public String executeAlgorithm(NetPlan np, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		// First of all, initialize all parameters
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		final WNet wNet = new WNet(np);
		Configuration.setOption("precisionFactor", new Double(1e-9).toString());
		Configuration.precisionFactor = 1e-9;

		/* Remove current parts */
		for (WLightpathRequest lp : new ArrayList<>(wNet.getLightpathRequests()))
			lp.remove();
		for (WServiceChain sc : new ArrayList<>(wNet.getServiceChains()))
			sc.remove();
		for (WIpLink sc : new ArrayList<>(wNet.getIpLinks()))
			sc.removeBidirectional();
		for (WVnfInstance sc : new ArrayList<>(wNet.getVnfInstances()))
			sc.remove();

		/* Check if the topology can have lasing problems */
		final List<List<WFiber>> unavoidableLasingLoops = OpticalSpectrumManager.createFromRegularLps(wNet).getUnavoidableLasingLoops();
		if (!unavoidableLasingLoops.isEmpty()) throw new Net2PlanException("There are intrinsic lasing loops in the topology: " + unavoidableLasingLoops);

		/* Modify the fiber structure for the algorithm */
		final List<Pair<Integer,Integer>> validOpticalSlotRanges = getValidOpticalSlotRange (fiberDefaultValidSlotRanges.getString());
		
        originalFibers = wNet.getFibers();
        for(int i = 0; i < originalFibers.size(); i++){
            newFibers.add(0);
            newFiberSIndex.add(0);
        }


		for (WFiber e : wNet.getFibers ())
			e.setValidOpticalSlotRanges(validOpticalSlotRanges);
//		for (WNode a : wNet.getNodes())
//			for (WNode b : wNet.getNodes())
//			{
//				if (a.getId () >= b.getId()) continue;
//				final SortedSet<WFiber> currentFibers = wNet.getNodePairFibers(a, b);
//				if (currentFibers.isEmpty()) continue;
//				if (currentFibers.size() > numParallelFibersPeNodePair.getInt()) throw new Net2PlanException ("A node pair has an excessive number of fibers");
//				final int numExtraFibers = numParallelFibersPeNodePair.getInt() - currentFibers.size();
//				for (int cont = 0 ; cont < numExtraFibers ; cont ++)
//					wNet.addFiber(a, b, validOpticalSlotRanges, currentFibers.first ().getLengthInKm() , true);
//			}
//		for (WNode a : wNet.getNodes())
//			for (WNode b : wNet.getNodes())
//				if (!a.equals(b))
//					assert wNet.getNodePairFibers(a, b).size() == 0 || wNet.getNodePairFibers(a, b).size() == numParallelFibersPeNodePair.getInt(); 
		
		/* All the fibers should have all the optical slots idle */
		this.osm = OpticalSpectrumManager.createFromRegularLps(wNet);
		assert wNet.getFibers().stream().allMatch(e->osm.getOccupiedOpticalSlotIds(e).isEmpty());
		assert wNet.getFibers().stream().allMatch(e->e.isBidirectional());
		/* Check that there are no laising loops */
		if (!osm.getUnavoidableLasingLoops().isEmpty()) throw new Net2PlanException ("The WDM plant has lasing loops: " + osm.getUnavoidableLasingLoops() + ". Some nodes must be converted into ROADMs");
		
		/* Add VNF instances of the required VNFs, in all the nodes */
		if (!wNet.getVnfTypeNames().equals(new TreeSet<>(Arrays.asList(ImporterFromTimBulkFiles_forConfTimData.VNFTYPE_UPF, ImporterFromTimBulkFiles_forConfTimData.VNFTYPE_GENERICSERVER, ImporterFromTimBulkFiles_forConfTimData.VNFTYPE_CACHECOMMONAMENANDMCEN))))
			throw new Net2PlanException("VNF types are not what expected: " + wNet.getVnfTypeNames());
		for (WNode n : wNet.getNodes())
		{
			wNet.addVnfInstance(n, VNFINSTANCENAME_REALINSTANCE, wNet.getVnfType(ImporterFromTimBulkFiles_forConfTimData.VNFTYPE_UPF).get());
			wNet.addVnfInstance(n, VNFINSTANCENAME_REALINSTANCE, wNet.getVnfType(ImporterFromTimBulkFiles_forConfTimData.VNFTYPE_CACHECOMMONAMENANDMCEN).get());
			if (NODETYPE.isMcenBb(n))
			{
				wNet.addVnfInstance(n, VNFINSTANCENAME_REALINSTANCE, wNet.getVnfType(ImporterFromTimBulkFiles_forConfTimData.VNFTYPE_GENERICSERVER).get());
				wNet.addVnfInstance(n, VNFINSTANCENAME_FAKEINSTANCE, wNet.getVnfType(ImporterFromTimBulkFiles_forConfTimData.VNFTYPE_GENERICSERVER).get());
				wNet.addVnfInstance(n, VNFINSTANCENAME_FAKEINSTANCE, wNet.getVnfType(ImporterFromTimBulkFiles_forConfTimData.VNFTYPE_CACHECOMMONAMENANDMCEN).get());
			}
		}

		/* Initialize the AMEN islands and the MCEN CORE island */
		this.genericAmenIslands = getAmenGenericIslands(wNet);
		this.mcenIsland = getMcenIsland(wNet);
//		for (AmenGenericIsland i : genericAmenIslands)
//			System.out.println("AMEN island (" + i.getAmens().size() + " nodes). Linear? " + i.isLinear() + ", amens: " + i.getAmens() + ", mcens connected: " + i.getMcensConnectedViaInFibers());

		/* Initialize the graph for WDM shortest path (in km) computations */
		this.wdmShortestPathKmCalculator_graph = new DirectedWeightedMultigraph<WNode,Object>(Object.class);
		for (WNode n : wNet.getNodes())
			wdmShortestPathKmCalculator_graph.addVertex(n);
		for (WFiber e : wNet.getFibers())
			if (!wdmShortestPathKmCalculator_graph.containsEdge(e.getA() , e.getB()))
			{
				final Object o = new Object ();
				wdmShortestPathKmCalculator_graph.addEdge(e.getA(), e.getB() , o);
				wdmShortestPathKmCalculator_graph.setEdgeWeight(o, e.getLengthInKm());
			}
		this.wdmShortestPathKmCalculator = new org.jgrapht.alg.shortestpath.DijkstraShortestPath<>(wdmShortestPathKmCalculator_graph);
		this.wdmKShortestPathKmCalculator = new KShortestSimplePaths<>(wdmShortestPathKmCalculator_graph);


        int numberOfDW = 0;
        for(WNode n : wNet.getNodes())
        {
            if(n.getOpticalSwitchingArchitecture().isPotentiallyWastingSpectrum())
                numberOfDW++;
        }

        System.out.println("Number of D&W Nodes: " + numberOfDW);

		/* Creates the base topology of IP links, and adds a graph to it */
		createBaseTopologyOfIpLinks(wNet);

		/* Initialize the graph for IP shortest path (in num hops) computations for the BASE TOPOLOGY */
		edu.uci.ics.jung.graph.Graph<WNode, WIpLink> spCalculatorInNumHopsForMaximumIpTopology_graph = new DirectedOrderedSparseMultigraph<>();
		for (WNode n : wNet.getNodes())
			spCalculatorInNumHopsForMaximumIpTopology_graph.addVertex(n);
		for (WIpLink e : wNet.getIpLinks())
			spCalculatorInNumHopsForMaximumIpTopology_graph.addEdge(e, e.getA(), e.getB());
		edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath<WNode, WIpLink> spCalculatorInNumHopsForMaximumIpTopology = new edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath<>(spCalculatorInNumHopsForMaximumIpTopology_graph, e -> 1.0);

		/* Make IP part design of the BASE MAXIMAL IP topology */
		double totalTraf = 0;
		for (WServiceChainRequest scr : wNet.getServiceChainRequests())
		{
			if (indexOfTimeSlotToAplyMinus1MakesWorstCaseAllSlots.getInt() < 0) totalTraf += scr.getTimeSlotNameAndInitialInjectionIntensityInGbpsList().stream().mapToDouble(p -> p.getSecond()).max().orElse(0.0);
			else
			{
				if (indexOfTimeSlotToAplyMinus1MakesWorstCaseAllSlots.getInt() >= scr.getTimeSlotNameAndInitialInjectionIntensityInGbpsList().size()) throw new Net2PlanException("No traffic information for that slot index");
				totalTraf += scr.getTrafficIntensityInfo(indexOfTimeSlotToAplyMinus1MakesWorstCaseAllSlots.getInt()).get().getSecond();
			}
		}
		System.out.println("indexOfTimeSlotToAplyMinus1MakesWorstCaseAllSlots: " + indexOfTimeSlotToAplyMinus1MakesWorstCaseAllSlots.getInt() + " - total traf: " + totalTraf);

		/* Set the traffic for the Service Chain Requests */
		for (WServiceChainRequest scr : wNet.getServiceChainRequests())
		{
			final double scrCurrentTraffic;
			if (indexOfTimeSlotToAplyMinus1MakesWorstCaseAllSlots.getInt() < 0)
			{
				scrCurrentTraffic = scr.getTimeSlotNameAndInitialInjectionIntensityInGbpsList().stream().mapToDouble(p -> p.getSecond()).max().orElse(0.0);
			} else
			{
				if (indexOfTimeSlotToAplyMinus1MakesWorstCaseAllSlots.getInt() >= scr.getTimeSlotNameAndInitialInjectionIntensityInGbpsList().size()) throw new Net2PlanException("No traffic information for that slot index");
				scrCurrentTraffic = scr.getTrafficIntensityInfo(indexOfTimeSlotToAplyMinus1MakesWorstCaseAllSlots.getInt()).get().getSecond();
			}
			scr.setCurrentOfferedTrafficInGbps(scrCurrentTraffic);
		}

		/* */
		final List<WNode> mcenBbs = wNet.getNodes().stream().filter(n->NODETYPE.isMcenBb(n)).collect(Collectors.toList());
		final SortedMap<WNode , SortedMap<WIpLink , Double>> ipTrafficPerNodePairPerFailure_f = new TreeMap<> ();
		if (addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible.getBoolean())
		{

            for (WNode mcenBbFailedInCorePart : mcenBbs)
			{
				/* Remmove all service chains */
				for (WServiceChain sc : new ArrayList<> (wNet.getServiceChains())) sc.remove();
				
				/* Allocate service chains in this failure */
				for (WServiceChainRequest scr : wNet.getServiceChainRequests())
					generateServiceChainForCurrentTraffic(scr , Optional.of(mcenBbFailedInCorePart) , spCalculatorInNumHopsForMaximumIpTopology);

				/* */
				final SortedMap<WIpLink , Double> trafficIpLinksThisFailure = new TreeMap<> ();
				for (WIpLink e : wNet.getIpLinks())
					trafficIpLinksThisFailure.put(e, trafficIpLinksThisFailure.getOrDefault(e, 0.0) + e.getCarriedTrafficGbps());
				ipTrafficPerNodePairPerFailure_f.put(mcenBbFailedInCorePart, trafficIpLinksThisFailure);
			}
			assert ipTrafficPerNodePairPerFailure_f.values().stream().allMatch(s->s.keySet().equals(new TreeSet<> (wNet.getIpLinks())));
		}
		
		/* Make the allocation for the non-failure state */
		for (WServiceChain sc : new ArrayList<> (wNet.getServiceChains())) sc.remove();
		for (WServiceChainRequest scr : wNet.getServiceChainRequests())
			generateServiceChainForCurrentTraffic(scr , Optional.empty() , spCalculatorInNumHopsForMaximumIpTopology);

		System.out.println("Postprocessing IP topology stage. Number of BIDI IP links BEFORE postprocessing: " + (wNet.getIpLinks().size() / 2));
		/* Removes IP links, and updates the map of worst case traffic in each IP link */
		final int numberOfRemovedIpBidiLinks = postProcessingOgIpTopologyRemovingSmallIpLinks(wNet , ipTrafficPerNodePairPerFailure_f);
		System.out.println("Postprocessing IP topology stage. Number of BIDI IP links removed: " + numberOfRemovedIpBidiLinks);
		System.out.println("Postprocessing IP topology stage. Number of BIDI IP links AFTER postprocessing: " + (wNet.getIpLinks().size() / 2));

		/* Create the lightpath requests, and then apply the RWAs */
		int i = 0;
		final SortedSet<WIpLink> ipBundleLinksWithoutWdmFaultTolerancePossible = new TreeSet<> ();
		for (WIpLink e : wNet.getIpLinks())
		{
			if (e.getId() > e.getBidirectionalPair().getId()) 
			{
				final boolean noWdmFaultTolerancePossible = createBundleOfIpLinkWithLightpathsAndAssignTransponders(e , ipTrafficPerNodePairPerFailure_f);
				if (noWdmFaultTolerancePossible)
				{
					assert e.isBundleOfIpLinks();
					assert e.getBidirectionalPair().isBundleOfIpLinks();
					ipBundleLinksWithoutWdmFaultTolerancePossible.add(e);
					ipBundleLinksWithoutWdmFaultTolerancePossible.add(e.getBidirectionalPair());
				}
			}
			i++;
			// System.out.println(i+"/"+wNet.getIpLinks().size());
		}

		/* Remove fibers unused */
		for (WFiber e : new ArrayList<> (wNet.getFibers()))
		{
            assert e.isBidirectional();
			if (e.getId() < e.getBidirectionalPair().getId()) continue;
			final boolean noLp = e.getTraversingLps().isEmpty() && e.getBidirectionalPair().getTraversingLps().isEmpty() && !originalFibers.contains(e);

//            assert noLp == this.osm.getOccupiedOpticalSlotIds(e).isEmpty();
			if (noLp)
			{
				e.getBidirectionalPair().remove();
				e.remove();
			}
		}

		/* Check all the WDM links for which fault tolerance is not possible for topological reasons */
//		System.out.println("-------------------------------------------------------------------------");
//		System.out.println("--- WDM cut fault tolerance NOT possible for topological reasons WDM cuts in node pairs. Affected IP bundleS: (" +ipBundleLinksWithoutWdmFaultTolerancePossible.size() + "): " + ipBundleLinksWithoutWdmFaultTolerancePossible);
//		System.out.println("-------------------------------------------------------------------------");

		
		/* Check that all is carried, no oversubscription, WDM links */
		if (DEBUG)
		{
			assert wNet.getServiceChainRequests().stream().allMatch(scr -> scr.getCurrentBlockedTraffic() == 0);
			assert wNet.getServiceChains().stream ().allMatch(sc->sc.getSequenceOfTraversedIpLinks().stream().allMatch(e->!e.wasRemoved()));
			assert wNet.getIpLinks().stream().allMatch(e->e.getCarriedTrafficGbps() <= e.getCurrentCapacityGbps() + 1e-3);
			assert wNet.getIpLinks().stream().allMatch(e->e.isBundleMember() || e.isBundleOfIpLinks());
			assert wNet.getIpLinks().stream().filter(e->e.isBundleMember()).allMatch(e->e.isCoupledtoLpRequest());
			assert wNet.getLightpathRequests().stream().allMatch(e->e.getLightpaths().size() == 1);
			assert this.osm.isSpectrumOccupationOk ();
			
			if (addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible.getBoolean())
			{
				/* Check tolerance to WDM failures. Now the traffic is that of the non-failure state  */
				final Set<SortedSet<WNode>> testedNodePairWdmFailures = new HashSet<> ();
				for (WFiber e : wNet.getFibers())
				{
					final SortedSet<WNode> endNodes = ImmutableSortedSet.of(e.getA() , e.getB());
					if (testedNodePairWdmFailures.contains(endNodes)) continue; 
					testedNodePairWdmFailures.add(endNodes);
				
					final SortedSet<WFiber> nodePairFibers = new TreeSet<> ();
					nodePairFibers.addAll(wNet.getNodePairFibers(e.getA(), e.getB()));
					nodePairFibers.addAll(wNet.getNodePairFibers(e.getB(), e.getA()));
					final SortedMap<WIpLink , SortedSet<WIpLink>> ipLinkBundle2MembersFailed = new TreeMap<> ();
					for (WFiber fiber : nodePairFibers)
					{
						for (WLightpath lp : fiber.getTraversingLps())
						{
							assert lp.getLightpathRequest().isCoupledToIpLink();
							final WIpLink ipLinkMember = lp.getLightpathRequest().getCoupledIpLink();
							assert ipLinkMember.isBundleMember();
							final WIpLink ipLinkBundle = ipLinkMember.getBundleParentIfMember();
							SortedSet<WIpLink> members = ipLinkBundle2MembersFailed.get(ipLinkBundle);
							if (members == null) { members = new TreeSet<> (); ipLinkBundle2MembersFailed.put (ipLinkBundle , members); }
							members.add(ipLinkMember);
						}
					}
					for (Entry<WIpLink , SortedSet<WIpLink>> entry : ipLinkBundle2MembersFailed.entrySet())
					{
						final WIpLink bundle = entry.getKey();
						assert bundle.isBundleOfIpLinks();
						if (ipBundleLinksWithoutWdmFaultTolerancePossible.contains(bundle)) continue;
						final SortedSet<WIpLink> members = entry.getValue();
						assert members.size() >= 1;
						final double perLpCapacityGbps = members.first().getCurrentCapacityGbps();
						assert members.stream().allMatch(ee->ee.getCurrentCapacityGbps() == perLpCapacityGbps);
						/* Surviving members have enough capacity for the traffic */ 
//						System.out.println("Bundle: " + bundle + ", carried: " + bundle.getCarriedTrafficGbps() + " , capacity: " + bundle.getCurrentCapacityGbps() + ", cap per lp: " + perLpCapacityGbps + ", members failed: " + members);
//						if (bundle.getCarriedTrafficGbps() > 1e-3 + bundle.getCurrentCapacityGbps() - perLpCapacityGbps * members.size()) 
//						{
//							System.out.println("Bundle: " + bundle + ", carried: " + bundle.getCarriedTrafficGbps() + " , capacity: " + bundle.getCurrentCapacityGbps() + ", cap per lp: " + perLpCapacityGbps + ", members failed: " + members);
//							for (WIpLink m : members) System.out.println("Member : " + m + ", path: " + m.getCoupledLpRequest().getLightpaths().get(0).getSeqFibers());
//						}
						assert bundle.getCarriedTrafficGbps() <= 1e-3 + bundle.getCurrentCapacityGbps() - perLpCapacityGbps * members.size();
					}
					assert ipLinkBundle2MembersFailed.values().stream().allMatch(set->set.stream().mapToDouble(ipl->ipl.getCurrentCapacityGbps()).distinct().count () == 1);
				}

				/* Check tolerance to MCENBB failures */
				final edu.uci.ics.jung.graph.Graph<WNode, WIpLink> spCalculatorCurrentIpBundlesTopology_graph = new DirectedOrderedSparseMultigraph<>();
				for (WNode n : wNet.getNodes())
					spCalculatorCurrentIpBundlesTopology_graph.addVertex(n);
				for (WIpLink ee : wNet.getIpLinks())
					if (ee.isBundleOfIpLinks())
						spCalculatorCurrentIpBundlesTopology_graph.addEdge(ee, ee.getA(), ee.getB());
				final edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath<WNode, WIpLink> spCalculatorCurrentIpBundlesTopology_dijkstra = new edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath<>(spCalculatorCurrentIpBundlesTopology_graph, e->1.0);
				for (WNode mcenBbFailedInCorePart : mcenBbs)
				{
					/* Remove all service chains */
					for (WServiceChain sc : new ArrayList<> (wNet.getServiceChains())) sc.remove();
					
					/* Allocate service chains in this failure */
					for (WServiceChainRequest scr : wNet.getServiceChainRequests())
					{
						assert !scr.wasRemoved();
						assert !mcenBbFailedInCorePart.wasRemoved();
						generateServiceChainForCurrentTraffic(scr , Optional.of(mcenBbFailedInCorePart) , spCalculatorCurrentIpBundlesTopology_dijkstra);
					}
					assert wNet.getServiceChainRequests().stream().allMatch(scr -> scr.getCurrentBlockedTraffic() == 0);
					assert wNet.getIpLinks().stream().allMatch(e->e.getCarriedTrafficGbps() <= e.getCurrentCapacityGbps() + 1e-3);
				}
			}
		}
		
		return "Ok";
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

	private List<AmenGenericIsland> getAmenGenericIslands(WNet wNet)
	{
		final SortedSet<WNode> amenNodes = wNet.getNodes().stream().filter(e -> NODETYPE.isAmen(e)).collect(Collectors.toCollection(TreeSet::new));
		final List<WFiber> fibersBetweenAmens = wNet.getFibers().stream().filter(e -> NODETYPE.isAmen(e.getA()) && NODETYPE.isAmen(e.getB())).collect(Collectors.toList());
		final List<Set<Node>> connectedComponentsAmens = GraphUtils.getConnectedComponents(amenNodes.stream().map(n -> n.getNe()).collect(Collectors.toList()), fibersBetweenAmens.stream().map(e -> e.getNe()).collect(Collectors.toList()));
		final List<AmenGenericIsland> res = new ArrayList<>();
		for (Set<Node> cc : connectedComponentsAmens)
		{
			final SortedSet<WNode> amensCc = cc.stream().map(n -> new WNode(n)).collect(Collectors.toCollection(TreeSet::new));
			final AmenGenericIsland island = new AmenGenericIsland(amensCc);
			res.add(island);
		}
		for (int cont1 = 0; cont1 < res.size(); cont1++)
			for (int cont2 = cont1 + 1; cont2 < res.size(); cont2++)
				if (!Sets.intersection(res.get(cont1).getAmens(), res.get(cont2).getAmens()).isEmpty()) throw new RuntimeException();
		final SortedSet<WNode> allCcsAdded = new TreeSet<>();
		res.forEach(nn -> allCcsAdded.addAll(nn.getAmens()));
		if (allCcsAdded.size() != amenNodes.size()) throw new RuntimeException();
		return res;
	}

	private McenGenericIsland getMcenIsland(WNet wNet)
	{
		final SortedSet<WNode> mcenNodes = wNet.getNodes().stream().filter(e -> NODETYPE.isMcenBbOrNot(e)).collect(Collectors.toCollection(TreeSet::new));
		final List<WFiber> fibersBetweenMcens = wNet.getFibers().stream().filter(e -> NODETYPE.isMcenBbOrNot(e.getA()) && NODETYPE.isMcenBbOrNot(e.getB())).collect(Collectors.toList());
		final List<Set<Node>> connectedComponentsMcens = GraphUtils.getConnectedComponents(mcenNodes.stream().map(n -> n.getNe()).collect(Collectors.toList()), fibersBetweenMcens.stream().map(e -> e.getNe()).collect(Collectors.toList()));
		if (connectedComponentsMcens.size() != 1) throw new Net2PlanException("The number of MCEN islands is not one: " + connectedComponentsMcens);
		final SortedSet<WNode> mcensCc = connectedComponentsMcens.get(0).stream().map(n -> new WNode(n)).collect(Collectors.toCollection(TreeSet::new));
		return new McenGenericIsland(mcensCc);
	}

	private void generateServiceChainForCurrentTraffic(WServiceChainRequest scr , Optional<WNode> mcenBbWithFailureInItsCorePart , edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath<WNode, WIpLink> spCalculatorInNumHopsForCurrentIpTopology)
	{
		// System.out.println("Processing SCR: " + scr.getUserServiceName() + ", " + (scr.isUpstream() ? "Upstream" :
		// "Downstream") + " . Offered: " + scr.getCurrentOfferedTrafficInGbps());
		final WNet wNet = scr.getNet();
		final MH_USERSERVICES userService = MH_USERSERVICES.valueOf(scr.getQosType());
		if (scr.isUpstream() && scr.getPotentiallyValidOrigins().size() != 1) throw new Net2PlanException("The origin of the traffic is not unique");
		if (scr.isDownstream() && scr.getPotentiallyValidDestinations().size() != 1) throw new Net2PlanException("The destination of the traffic is not unique");
//		final Function<WNode,Boolean> isFailed = n->failedMcenBb.isPresent()? failedMcenBb.get().equals(n) : false;
		
		if (userService.isA1Traffic())
		{
			if (scr.isUpstream())
			{
				final WNode a = scr.getPotentiallyValidOrigins().first();
				
				final WNode closestMcenBb = getClosestNodeOfGivenType(a, mcenBbWithFailureInItsCorePart , NODETYPE.MCENBB).orElse(null);
				if (closestMcenBb == null) throw new Net2PlanException("Node " + a + " has no WDM path to any MCEN BB node");
				final List<WIpLink> a2closestMcenBb = a.equals(closestMcenBb) ? null : getShortespIpPathInNumHops(a, closestMcenBb, spCalculatorInNumHopsForCurrentIpTopology);

				/* SC: Traffic from user to VNF in UPF 1 in MCENBB node */
				final WVnfInstance vnfInstanceInBb = getVnfInstanceUpfType(closestMcenBb);
				{
					final double trafOutsideMetroGbps = scr.getCurrentOfferedTrafficInGbps() * this.a1Traffic_fractionExchangedOutsideMetro.getDouble();
					final double carriedTrafficBefore = scr.getCurrentCarriedTrafficGbps();
					if (a.equals(closestMcenBb)) scr.addServiceChain(Arrays.asList(vnfInstanceInBb), trafOutsideMetroGbps);
					else
					{
						final List<WAbstractNetworkElement> path = new ArrayList<>();
						path.addAll(getShortespIpPathInNumHops(a, closestMcenBb, spCalculatorInNumHopsForCurrentIpTopology));
						path.add(vnfInstanceInBb);
						scr.addServiceChain(path, trafOutsideMetroGbps);
					}
					// if (Math.abs(carriedTrafficBefore + trafOutsideMetroGbps - scr.getCurrentCarriedTraffic()) > 1e-3)
					// System.out.println("carriedTrafficBefore: " + carriedTrafficBefore + "; trafOutsideMetroGbps: " +
					// trafOutsideMetroGbps + ", scr.getCurrentCarriedTraffic(): " + scr.getCurrentCarriedTraffic());
				}
				/* SCs: Traffic from UPF1 to users in all the metro nodes */
				for (WNode n : wNet.getNodes())
				{
					final double trafToGivenNode = scr.getCurrentOfferedTrafficInGbps() * (1 - this.a1Traffic_fractionExchangedOutsideMetro.getDouble()) * getA1Fraction(n);
					final List<WAbstractNetworkElement> path = new ArrayList<>();
					final List<WIpLink> closestMcenBb2n = n.equals(closestMcenBb) ? null : getShortespIpPathInNumHops(closestMcenBb, n, spCalculatorInNumHopsForCurrentIpTopology);
					if (a2closestMcenBb != null) path.addAll(a2closestMcenBb);
					path.add(vnfInstanceInBb);
					if (closestMcenBb2n != null) path.addAll(closestMcenBb2n);
					final double carriedTrafficBefore = scr.getCurrentCarriedTrafficGbps();
					scr.addServiceChain(path, trafToGivenNode);
					// if (Math.abs(carriedTrafficBefore + trafToGivenNode - scr.getCurrentCarriedTraffic()) > 1e-3)
					// System.out.println("carriedTrafficBefore: " + carriedTrafficBefore + "; trafToGivenNode: " + trafToGivenNode + ",
					// scr.getCurrentCarriedTraffic(): " + scr.getCurrentCarriedTraffic());
					// System.out.println("CurrentCarried: " + scr.getCurrentCarriedTraffic());
				}
			} else // Downstream A1 type (P2P)
			{
				final WNode b = scr.getPotentiallyValidDestinations().first();
				final WNode closestMcenBb = getClosestNodeOfGivenType(b, mcenBbWithFailureInItsCorePart , NODETYPE.MCENBB).orElse(null);
				if (closestMcenBb == null) throw new Net2PlanException("Node " + b + " has no WDM path to any MCEN BB node");

				/* SC: Traffic from user to VNF in UPF 1 in MCENBB node */
				final WVnfInstance vnfInstanceInBb = getVnfInstanceUpfType(closestMcenBb);
				final double trafOutsideMetroGbps = scr.getCurrentOfferedTrafficInGbps() * this.a1Traffic_fractionExchangedOutsideMetro.getDouble();
				if (b.equals(closestMcenBb)) scr.addServiceChain(Arrays.asList(vnfInstanceInBb), trafOutsideMetroGbps);
				else
				{
					final List<WAbstractNetworkElement> path = new ArrayList<>();
					path.add(vnfInstanceInBb);
					path.addAll(getShortespIpPathInNumHops(closestMcenBb, b, spCalculatorInNumHopsForCurrentIpTopology));
					scr.addServiceChain(path, trafOutsideMetroGbps);
				}

				/* SCs: Traffic from UPF1 to users in all the metro nodes */
				final List<WIpLink> closestMcenBb2b = b.equals(closestMcenBb) ? null : getShortespIpPathInNumHops(closestMcenBb, b, spCalculatorInNumHopsForCurrentIpTopology);
				for (WNode n : wNet.getNodes())
				{
					final double trafToGivenNode = scr.getCurrentOfferedTrafficInGbps() * (1 - this.a1Traffic_fractionExchangedOutsideMetro.getDouble()) * getA1Fraction(n);
					final List<WAbstractNetworkElement> path = new ArrayList<>();
					final List<WIpLink> n2closestMcenBb2 = n.equals(closestMcenBb) ? null : getShortespIpPathInNumHops(n, closestMcenBb, spCalculatorInNumHopsForCurrentIpTopology);
					if (n2closestMcenBb2 != null) path.addAll(n2closestMcenBb2);
					path.add(vnfInstanceInBb);
					if (closestMcenBb2b != null) path.addAll(closestMcenBb2b);
					scr.addServiceChain(path, trafToGivenNode);
				}
			}
		} else if (userService.isA2Traffic())
		{
			if (scr.isUpstream())
			{
				final WNode a = scr.getPotentiallyValidOrigins().first();
				final WNode closestMcenBb = getClosestNodeOfGivenType(a, mcenBbWithFailureInItsCorePart , NODETYPE.MCENBB).orElse(null);
				if (closestMcenBb == null) throw new Net2PlanException("Node " + a + " has no WDM path to any MCEN BB node");

				final WVnfInstance vnfInstanceUpfInBb = getVnfInstanceUpfType(closestMcenBb);
				final WVnfInstance vnfInstanceServerInBb_real = getVnfInstanceServerType(closestMcenBb, true);
				final WVnfInstance vnfInstanceServerInBb_fake = getVnfInstanceServerType(closestMcenBb, false);
				if (a.equals(closestMcenBb))
				{
					scr.addServiceChain(Arrays.asList(vnfInstanceUpfInBb, vnfInstanceServerInBb_real), scr.getCurrentOfferedTrafficInGbps() * (1 - this.a2Traffic_fractionExchangedOutsideMetro.getDouble()));
					scr.addServiceChain(Arrays.asList(vnfInstanceUpfInBb, vnfInstanceServerInBb_fake), scr.getCurrentOfferedTrafficInGbps() * this.a2Traffic_fractionExchangedOutsideMetro.getDouble());
				} else
				{
					final List<WIpLink> a2mcenbb = getShortespIpPathInNumHops(a, closestMcenBb, spCalculatorInNumHopsForCurrentIpTopology);
					final List<WAbstractNetworkElement> path = new ArrayList<>();
					path.addAll(a2mcenbb);
					path.add(vnfInstanceUpfInBb);
					path.add(vnfInstanceServerInBb_real);
					scr.addServiceChain(path, scr.getCurrentOfferedTrafficInGbps() * (1 - this.a2Traffic_fractionExchangedOutsideMetro.getDouble()));
					path.clear();
					path.addAll(a2mcenbb);
					path.add(vnfInstanceUpfInBb);
					path.add(vnfInstanceServerInBb_fake);
					scr.addServiceChain(path, scr.getCurrentOfferedTrafficInGbps() * this.a2Traffic_fractionExchangedOutsideMetro.getDouble());
				}
			} else // Downstream A2 type (P2P)
			{
				final WNode b = scr.getPotentiallyValidDestinations().first();
				final WNode closestMcenBb = getClosestNodeOfGivenType(b, mcenBbWithFailureInItsCorePart , NODETYPE.MCENBB).orElse(null);
				if (closestMcenBb == null) throw new Net2PlanException("Node " + b + " has no WDM path to any MCEN BB node");

				final WVnfInstance vnfInstanceUpfInBb = getVnfInstanceUpfType(closestMcenBb);
				final WVnfInstance vnfInstanceServerInBb_real = getVnfInstanceServerType(closestMcenBb, true);
				final WVnfInstance vnfInstanceServerInBb_fake = getVnfInstanceServerType(closestMcenBb, false);
				if (b.equals(closestMcenBb))
				{
					scr.addServiceChain(Arrays.asList(vnfInstanceServerInBb_real, vnfInstanceUpfInBb), scr.getCurrentOfferedTrafficInGbps() * (1 - this.a2Traffic_fractionExchangedOutsideMetro.getDouble()));
					scr.addServiceChain(Arrays.asList(vnfInstanceServerInBb_fake, vnfInstanceUpfInBb), scr.getCurrentOfferedTrafficInGbps() * this.a2Traffic_fractionExchangedOutsideMetro.getDouble());
				} else
				{
					final List<WIpLink> mcenbb2node = getShortespIpPathInNumHops(closestMcenBb, b, spCalculatorInNumHopsForCurrentIpTopology);
					final List<WAbstractNetworkElement> path = new ArrayList<>();
					path.add(vnfInstanceServerInBb_real);
					path.add(vnfInstanceUpfInBb);
					path.addAll(mcenbb2node);
					scr.addServiceChain(path, scr.getCurrentOfferedTrafficInGbps() * (1 - this.a2Traffic_fractionExchangedOutsideMetro.getDouble()));
					path.clear();
					path.add(vnfInstanceServerInBb_fake);
					path.add(vnfInstanceUpfInBb);
					path.addAll(mcenbb2node);
					scr.addServiceChain(path, scr.getCurrentOfferedTrafficInGbps() * this.a2Traffic_fractionExchangedOutsideMetro.getDouble());
				}
			}
		} else if (userService.isA3Traffic())
		{
			if (scr.isUpstream())
			{
				final WNode a = scr.getPotentiallyValidOrigins().first();
				final WNode closestMcenBbOrNot = getClosestNodeOfGivenType(a, mcenBbWithFailureInItsCorePart , NODETYPE.MCENBB, NODETYPE.MCEN).orElse(null);
				final WNode closestMcenBb = getClosestNodeOfGivenType(a, mcenBbWithFailureInItsCorePart , NODETYPE.MCENBB).orElse(null);
				if (closestMcenBb == null) throw new Net2PlanException("Node " + a + " has no WDM path to any MCEN node");
				if (closestMcenBbOrNot == null) throw new Net2PlanException("Node " + a + " has no WDM path to any MCEN BB node");

				/* Traffic stays in the AMEN */
				scr.addServiceChain(Arrays.asList(getVnfInstanceUpfType(a), getVnfInstanceCacheType(a, true)), scr.getCurrentOfferedTrafficInGbps() * this.a3Traffic_fractionOnInitialAmen.getDouble());
				/* Traffic to closest MCEN */
				if (a.equals(closestMcenBbOrNot)) scr.addServiceChain(Arrays.asList(getVnfInstanceUpfType(a), getVnfInstanceCacheType(closestMcenBbOrNot, true)), scr.getCurrentOfferedTrafficInGbps() * this.a3Traffic_fractionOnNearestMcen.getDouble());
				else
				{
					final List<WAbstractNetworkElement> path = new ArrayList<>();
					path.add(getVnfInstanceUpfType(a));
					path.addAll(getShortespIpPathInNumHops(a, closestMcenBbOrNot, spCalculatorInNumHopsForCurrentIpTopology));
					path.add(getVnfInstanceCacheType(closestMcenBbOrNot, true));
					scr.addServiceChain(path, scr.getCurrentOfferedTrafficInGbps() * this.a3Traffic_fractionOnNearestMcen.getDouble());
				}
				/* Traffic to closest MCEN BB */
				final List<WIpLink> a2mcen = a.equals(closestMcenBbOrNot) ? null : getShortespIpPathInNumHops(a, closestMcenBbOrNot, spCalculatorInNumHopsForCurrentIpTopology);
				final List<WIpLink> mcen2mcenBb = closestMcenBbOrNot.equals(closestMcenBb) ? null : getShortespIpPathInNumHops(closestMcenBbOrNot, closestMcenBb, spCalculatorInNumHopsForCurrentIpTopology);
				{
					final double fractionToClosestMcenBb = 1 - a3Traffic_fractionExchangedOutsideMetro.getDouble() - a3Traffic_fractionOnInitialAmen.getDouble() - a3Traffic_fractionOnNearestMcen.getDouble();
					final List<WAbstractNetworkElement> pathTrafficToBbReal = new ArrayList<>();
					pathTrafficToBbReal.add(getVnfInstanceUpfType(a));
					if (a2mcen != null) pathTrafficToBbReal.addAll(a2mcen);
					if (mcen2mcenBb != null) pathTrafficToBbReal.addAll(mcen2mcenBb);
					pathTrafficToBbReal.add(getVnfInstanceCacheType(closestMcenBb, true));
					scr.addServiceChain(pathTrafficToBbReal, scr.getCurrentOfferedTrafficInGbps() * fractionToClosestMcenBb);
				}
				/* Traffic outside metro */
				{
					final List<WAbstractNetworkElement> pathTrafficToBbFake = new ArrayList<>();
					pathTrafficToBbFake.add(getVnfInstanceUpfType(a));
					if (a2mcen != null) pathTrafficToBbFake.addAll(a2mcen);
					if (mcen2mcenBb != null) pathTrafficToBbFake.addAll(mcen2mcenBb);
					pathTrafficToBbFake.add(getVnfInstanceCacheType(closestMcenBb, false));
					scr.addServiceChain(pathTrafficToBbFake, scr.getCurrentOfferedTrafficInGbps() * a3Traffic_fractionExchangedOutsideMetro.getDouble());
				}
			} else
			{
				final WNode b = scr.getPotentiallyValidDestinations().first();
				final WNode closestMcenBbOrNot = getClosestNodeOfGivenType(b, mcenBbWithFailureInItsCorePart , NODETYPE.MCENBB, NODETYPE.MCEN).orElse(null);
				final WNode closestMcenBb = getClosestNodeOfGivenType(b, mcenBbWithFailureInItsCorePart , NODETYPE.MCENBB).orElse(null);
				if (closestMcenBb == null) throw new Net2PlanException("Node " + b + " has no WDM path to any MCEN node");
				if (closestMcenBbOrNot == null) throw new Net2PlanException("Node " + b + " has no WDM path to any MCEN BB node");

				/* Traffic stays in the AMEN */
				scr.addServiceChain(Arrays.asList(getVnfInstanceCacheType(b, true), getVnfInstanceUpfType(b)), scr.getCurrentOfferedTrafficInGbps() * this.a3Traffic_fractionOnInitialAmen.getDouble());
				/* Traffic to closest MCEN */
				if (b.equals(closestMcenBbOrNot)) scr.addServiceChain(Arrays.asList(getVnfInstanceCacheType(closestMcenBbOrNot, true), getVnfInstanceUpfType(b)), scr.getCurrentOfferedTrafficInGbps() * this.a3Traffic_fractionOnNearestMcen.getDouble());
				else
				{
					final List<WAbstractNetworkElement> path = new ArrayList<>();
					path.add(getVnfInstanceCacheType(closestMcenBbOrNot, true));
					path.addAll(getShortespIpPathInNumHops(closestMcenBbOrNot, b, spCalculatorInNumHopsForCurrentIpTopology));
					path.add(getVnfInstanceUpfType(b));
					scr.addServiceChain(path, scr.getCurrentOfferedTrafficInGbps() * this.a3Traffic_fractionOnNearestMcen.getDouble());
				}

				/* Traffic to closest MCEN BB */
				final List<WIpLink> mcen2b = b.equals(closestMcenBbOrNot) ? null : getShortespIpPathInNumHops(closestMcenBbOrNot, b, spCalculatorInNumHopsForCurrentIpTopology);
				final List<WIpLink> mcenBb2mcen = closestMcenBbOrNot.equals(closestMcenBb) ? null : getShortespIpPathInNumHops(closestMcenBb, closestMcenBbOrNot, spCalculatorInNumHopsForCurrentIpTopology);
				{
					final double fractionToClosestMcenBb = 1 - a3Traffic_fractionExchangedOutsideMetro.getDouble() - a3Traffic_fractionOnInitialAmen.getDouble() - a3Traffic_fractionOnNearestMcen.getDouble();
					final List<WAbstractNetworkElement> pathTrafficToBbReal = new ArrayList<>();
					pathTrafficToBbReal.add(getVnfInstanceCacheType(closestMcenBb, true));
					if (mcenBb2mcen != null) pathTrafficToBbReal.addAll(mcenBb2mcen);
					if (mcen2b != null) pathTrafficToBbReal.addAll(mcen2b);
					pathTrafficToBbReal.add(getVnfInstanceUpfType(b));
					scr.addServiceChain(pathTrafficToBbReal, scr.getCurrentOfferedTrafficInGbps() * fractionToClosestMcenBb);
				}
				/* Traffic outside metro */
				{
					final List<WAbstractNetworkElement> pathTrafficToBbFake = new ArrayList<>();
					pathTrafficToBbFake.add(getVnfInstanceCacheType(closestMcenBb, false));
					if (mcenBb2mcen != null) pathTrafficToBbFake.addAll(mcenBb2mcen);
					if (mcen2b != null) pathTrafficToBbFake.addAll(mcen2b);
					pathTrafficToBbFake.add(getVnfInstanceUpfType(b));
					scr.addServiceChain(pathTrafficToBbFake, scr.getCurrentOfferedTrafficInGbps() * a3Traffic_fractionExchangedOutsideMetro.getDouble());
				}
			}
		} else throw new RuntimeException();

		if (Math.abs(scr.getCurrentOfferedTrafficInGbps() - scr.getCurrentCarriedTrafficGbps()) > 1e-3) throw new RuntimeException();
	}

	private Optional<WNode> getClosestNodeOfGivenType(WNode a, Optional<WNode> failedNode , NODETYPE... nodeType)
	{
		final Pair<WNode, Set<NODETYPE>> keyInCache = Pair.of(a, new HashSet<>(Arrays.asList(nodeType)));
		if (cache_closestNodeOfGivenType.containsKey(keyInCache)) return cache_closestNodeOfGivenType.get(keyInCache);

		for (NODETYPE type : nodeType)
			if (NODETYPE.getNodeType(a) == type) return Optional.of(a);
		final WNet wNet = a.getNet();
		WNode nodeWithSp = null;
		double minCostSp = Double.MAX_VALUE;
		for (WNode node : wNet.getNodes())
		{
			if (!Stream.of(nodeType).anyMatch(type -> NODETYPE.getNodeType(node) == type)) continue;
			if (failedNode.isPresent()) if (failedNode.get().equals(node)) continue;
			
			final double cost = this.wdmShortestPathKmCalculator.getPathWeight(a, node); 
			if (cost ==  Double.POSITIVE_INFINITY) continue;
			if (cost < minCostSp)
			{
				minCostSp = cost;
				nodeWithSp = node;
			}
		}
		cache_closestNodeOfGivenType.put(keyInCache, Optional.ofNullable(nodeWithSp));
		if (nodeWithSp != null && failedNode.isPresent()) assert !failedNode.get().equals(nodeWithSp); 
		return Optional.ofNullable(nodeWithSp);
	}

	public List<WIpLink> getShortespIpPathInNumHops(WNode a, WNode b, edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath<WNode, WIpLink> spNumHopsCalculator)
	{
		if (a.equals(b)) throw new Net2PlanException("A and B are the same node");
		final List<WIpLink> sp = spNumHopsCalculator.getPath(a, b);
		if (sp == null) throw new Net2PlanException("There is no IP path from node " + a + " to node " + b);
		if (sp.isEmpty()) throw new Net2PlanException("There is no IP path from node " + a + " to node " + b);
		return sp;
	}

	private static double getA1Fraction(WNode n)
	{
		final Double res = n.getAttributeAsDoubleOrDefault(ImporterFromTimBulkFiles_forConfTimData.ATTNAME_WNODE_A1TRAFFICWEIGHT, null);
		if (res == null) throw new RuntimeException();
		return res;
	}

	private static WVnfInstance getVnfInstanceCacheType(WNode a, boolean realInstance)
	{
		final List<WVnfInstance> res = a.getVnfInstances(ImporterFromTimBulkFiles_forConfTimData.VNFTYPE_CACHECOMMONAMENANDMCEN).stream().filter(v -> realInstance ? v.getName().equals(VNFINSTANCENAME_REALINSTANCE) : v.getName().equals(VNFINSTANCENAME_FAKEINSTANCE))
				.collect(Collectors.toList());
		if (res.size() != 1) throw new RuntimeException();
		return res.get(0);
	}

	private static WVnfInstance getVnfInstanceServerType(WNode a, boolean realInstance)
	{
		final List<WVnfInstance> res = a.getVnfInstances(ImporterFromTimBulkFiles_forConfTimData.VNFTYPE_GENERICSERVER).stream().filter(v -> realInstance ? v.getName().equals(VNFINSTANCENAME_REALINSTANCE) : v.getName().equals(VNFINSTANCENAME_FAKEINSTANCE))
				.collect(Collectors.toList());
		if (res.size() != 1) throw new RuntimeException();
		return res.get(0);
	}

	private static WVnfInstance getVnfInstanceUpfType(WNode a)
	{
		final List<WVnfInstance> res = a.getVnfInstances(ImporterFromTimBulkFiles_forConfTimData.VNFTYPE_UPF).stream().collect(Collectors.toList());
		if (res.size() != 1) throw new RuntimeException();
		assert res.get(0).getName().equals(VNFINSTANCENAME_REALINSTANCE);
		return res.get(0);
	}

	private Quintuple<String, Double, Double, Double, Integer> getTransponderNameRateGbpsCostReachKmNumSlots(int index, boolean mcen2mcen)
	{
		final String s = mcen2mcen ? transponderOptionsNameRateGbpsCostReachKmNumSlots_mcen2mcenLps.getString() : transponderOptionsNameRateGbpsCostReachKmNumSlots_amenLps.getString();
		final String[] vals = s.split(",");
		if (index >= vals.length) throw new RuntimeException();
		final String sTransponder = vals[index].trim();
		final List<String> valsThisTransponder = Stream.of(sTransponder.split(" ")).collect(Collectors.toList());
		return Quintuple.of(valsThisTransponder.get(0), Double.parseDouble(valsThisTransponder.get(1)), Double.parseDouble(valsThisTransponder.get(2)), Double.parseDouble(valsThisTransponder.get(3)),
				(int) Double.parseDouble(valsThisTransponder.get(4)));
	}

	private int getNumberOfDefinedTransponders(boolean mcen2mcen)
	{
		final String s = mcen2mcen ? transponderOptionsNameRateGbpsCostReachKmNumSlots_mcen2mcenLps.getString() : transponderOptionsNameRateGbpsCostReachKmNumSlots_amenLps.getString();
		final String[] vals = s.split(",");
		return vals.length;
	}

	public List<Quintuple<String, Double, Double, Double, Integer>> getAmenTransponders()
	{
		final List<Quintuple<String, Double, Double, Double, Integer>> listOfTransponders = new ArrayList<>();

		for (int i = 0; i < getNumberOfDefinedTransponders(false); i++)
			listOfTransponders.add(getTransponderNameRateGbpsCostReachKmNumSlots(i, false));

		return listOfTransponders;
	}

	public List<Quintuple<String, Double, Double, Double, Integer>> getMcenTransponders()
	{
		final List<Quintuple<String, Double, Double, Double, Integer>> listOfTransponders = new ArrayList<>();

		for (int i = 0; i < getNumberOfDefinedTransponders(true); i++)
			listOfTransponders.add(getTransponderNameRateGbpsCostReachKmNumSlots(i, true));

		return listOfTransponders;
	}

	private void createBaseTopologyOfIpLinks(WNet wNet)
	{
		/* Create the base topology of IP links. Do not add those forbidden because of policy / not feasilbe because of
		 * transponder reach */
		for (WNode a : wNet.getNodes())
		{
			final boolean aAmen = NODETYPE.isAmen(a);
			final AmenGenericIsland amenIslandA = aAmen ? genericAmenIslands.stream().filter(i -> i.getAmens().contains(a)).findFirst().orElse(null) : null;
			for (WNode b : wNet.getNodes())
			{
				final boolean bAmen = NODETYPE.isAmen(b);
				final AmenGenericIsland amenIslandB = bAmen ? genericAmenIslands.stream().filter(i -> i.getAmens().contains(b)).findFirst().orElse(null) : null;
				if (a.getId() > b.getId())
				{
					if (!ipTopologyConstraints_amen2amenSameIslandAllowed.getBoolean() && aAmen && bAmen && amenIslandA.equals(amenIslandB)) continue;
					if (!ipTopologyConstraints_amen2amenDifferentIslandAllowed.getBoolean() && aAmen && bAmen && !amenIslandA.equals(amenIslandB)) continue;
					if (!ipTopologyConstraints_amen2mcenNotBorderToAmenIslandAllowed.getBoolean() && (aAmen != bAmen))
					{
						final WNode mcen = aAmen ? b : a;
						final AmenGenericIsland amenIsland = aAmen ? amenIslandA : amenIslandB;
						if (!amenIsland.getMcensConnectedViaOutFibers().contains(mcen)) continue;
					}

					/* Check if a transponder can make it */
					final double spDistanceWdmKm = this.wdmShortestPathKmCalculator.getPathWeight(a, b);
					if (spDistanceWdmKm ==  Double.POSITIVE_INFINITY) throw new Net2PlanException("There is no WDM path from node " + a + " to node " + b);
					final boolean mcen2mcen = !aAmen && !bAmen;
					final int numTps = getNumberOfDefinedTransponders(mcen2mcen);
					final boolean noTpCanMakeIt = IntStream.rangeClosed(0, numTps - 1).allMatch(i -> getTransponderNameRateGbpsCostReachKmNumSlots(i, mcen2mcen).getFourth() < spDistanceWdmKm);
					if (noTpCanMakeIt) continue;

					final Pair<WIpLink, WIpLink> ee = wNet.addIpLinkBidirectional(a, b, Double.MAX_VALUE);
				}
			}
		}
	}

	private int postProcessingOgIpTopologyRemovingSmallIpLinks(WNet wNet , SortedMap<WNode , SortedMap<WIpLink , Double>> ipTrafficPerNodePairPerFailure_f)
	{
		assert wNet.getIpLinks().stream().allMatch(ee->!ee.isBundleMember());
		final double utTh = minimumUtilizationToJustifyIpLinkInSmallerTransponder.getDouble();
		if (utTh <= 0) return 0;
		boolean atLeastOneIpLinkRemoved = true;
		int numberOfRemovedIpBidiLinks = 0;
		final Function<WIpLink , Double> worstTrafficNoFailureAndMcenBBFailure = ipLink -> 
		{
			final double noFailureTraf = ipLink.getCarriedTrafficGbps();
			final double wcFailureTraf = ipTrafficPerNodePairPerFailure_f.keySet().stream().mapToDouble(n->ipTrafficPerNodePairPerFailure_f.get(n).get(ipLink)).max().orElseThrow(RuntimeException::new);
			return Math.max(noFailureTraf, wcFailureTraf);
		};
		
		final edu.uci.ics.jung.graph.Graph<WNode, WIpLink> spCalculatorThisTopology_graph = new DirectedOrderedSparseMultigraph<>();
		for (WNode n : wNet.getNodes())
			spCalculatorThisTopology_graph.addVertex(n);
		for (WIpLink ee : wNet.getIpLinks())
			spCalculatorThisTopology_graph.addEdge(ee, ee.getA(), ee.getB());

		
		while (atLeastOneIpLinkRemoved)
		{
			atLeastOneIpLinkRemoved = false;
			for (WIpLink e : wNet.getIpLinks().stream().sorted((e1, e2) -> Double.compare(e1.getCarriedTrafficGbps(), e2.getCarriedTrafficGbps())).collect(Collectors.toList()))
			{
				if (e.wasRemoved()) continue;
				if (e.isBidirectional()) if (e.getId() <= e.getBidirectionalPair().getId()) continue;
				final boolean aAmen = NODETYPE.isAmen(e.getA());
				final boolean bAmen = NODETYPE.isAmen(e.getB());
				final boolean mcen2mcen = !aAmen && !bAmen;
				final int numTps = getNumberOfDefinedTransponders(mcen2mcen);
				final double minRateGbps = IntStream.rangeClosed(0, numTps - 1).mapToDouble(i -> getTransponderNameRateGbpsCostReachKmNumSlots(i, mcen2mcen).getSecond()).min().orElse(0.0);
				if (minRateGbps <= 0) continue;
				final double ipLinkTrafficGbpsWc;
				if (addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible.getBoolean())
					ipLinkTrafficGbpsWc = Arrays.asList(e.getCarriedTrafficGbps() , 
						e.getBidirectionalPair().getCarriedTrafficGbps() , worstTrafficNoFailureAndMcenBBFailure.apply(e) , 
						worstTrafficNoFailureAndMcenBBFailure.apply(e.getBidirectionalPair())).stream().mapToDouble(ee->ee).max().orElseThrow(RuntimeException::new); 
				else
					ipLinkTrafficGbpsWc = Math.max(e.getCarriedTrafficGbps() , e.getBidirectionalPair().getCarriedTrafficGbps());
				if (ipLinkTrafficGbpsWc < minRateGbps * utTh)
				{
					/* Try to remove link */
					final WNode a = e.getA();
					final WNode b = e.getB();
					final edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath<WNode, WIpLink> modifiedCalculator = new edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath<>(spCalculatorThisTopology_graph,
							link -> link.wasRemoved() || link.equals(e) || link.equals(e.getBidirectionalPair()) ? Integer.MAX_VALUE : 1.0);
					final List<WIpLink> surroundingPath_ab = modifiedCalculator.getPath(a, b);
					final List<WIpLink> surroundingPath_ba = modifiedCalculator.getPath(b, a);
					if (surroundingPath_ab.contains(e)) continue;
					if (surroundingPath_ba.contains(e.getBidirectionalPair())) continue;
					if (surroundingPath_ab.stream().anyMatch(ee -> ee.wasRemoved())) continue;
					if (surroundingPath_ab.contains(e)) continue;
					if (surroundingPath_ba.contains(e.getBidirectionalPair())) continue;
					if (surroundingPath_ba.stream().anyMatch(ee -> ee.wasRemoved())) continue;
					for (boolean isAb : new boolean[] { true, false })
					{
						final WIpLink ipLink = isAb ? e : e.getBidirectionalPair();
						final List<WIpLink> surroundingPath = isAb ? surroundingPath_ab : surroundingPath_ba;
						for (WServiceChain sc : ipLink.getTraversingServiceChains())
						{
							final List<WAbstractNetworkElement> newPath = new ArrayList<>();
							sc.getSequenceOfLinksAndVnfs().forEach(element -> {
								if (element.equals(ipLink)) newPath.addAll(surroundingPath);
								else newPath.add(element);
							});
							sc.setPath(newPath);
						}
					}
					
					/* Update the map of worst case traffic */
					for (WNode failedMcenBb : ipTrafficPerNodePairPerFailure_f.keySet())
					{
						final double wcTrafficInLinkToRemove_ab = ipTrafficPerNodePairPerFailure_f.get(failedMcenBb).get(e);
						final double wcTrafficInLinkToRemove_ba = ipTrafficPerNodePairPerFailure_f.get(failedMcenBb).get(e.getBidirectionalPair());
						for (WIpLink surrounding : surroundingPath_ab)
							ipTrafficPerNodePairPerFailure_f.get(failedMcenBb).put(surrounding, ipTrafficPerNodePairPerFailure_f.get(failedMcenBb).getOrDefault (surrounding , 0.0) + wcTrafficInLinkToRemove_ab);
						for (WIpLink surrounding : surroundingPath_ba)
							ipTrafficPerNodePairPerFailure_f.get(failedMcenBb).put(surrounding, ipTrafficPerNodePairPerFailure_f.get(failedMcenBb).getOrDefault (surrounding , 0.0) + wcTrafficInLinkToRemove_ba);
						ipTrafficPerNodePairPerFailure_f.get(failedMcenBb).remove(e);
						ipTrafficPerNodePairPerFailure_f.get(failedMcenBb).remove(e.getBidirectionalPair());
					}

					/* Actually remove the IP link and its bidi pair */
					e.removeBidirectional();

					assert ipTrafficPerNodePairPerFailure_f.values().stream().allMatch(s->s.keySet().equals(new TreeSet<> (wNet.getIpLinks())));

					numberOfRemovedIpBidiLinks++;
					atLeastOneIpLinkRemoved = true;
				}

			}
		}
		
		assert ipTrafficPerNodePairPerFailure_f.values().stream().allMatch(ee->ee.keySet().equals(new TreeSet<> (wNet.getIpLinks())));
		
		return numberOfRemovedIpBidiLinks;
	}

	private boolean createBundleOfIpLinkWithLightpathsAndAssignTransponders(WIpLink ipBidiLink , SortedMap<WNode , SortedMap<WIpLink , Double>> ipTrafficPerNodePairPerFailure_f)
	{
		assert ipBidiLink.isBidirectional();
		assert !ipBidiLink.isCoupledtoLpRequest();
		assert !ipBidiLink.getBidirectionalPair().isCoupledtoLpRequest();
		assert !ipBidiLink.isBundleOfIpLinks();
		assert !ipBidiLink.getBidirectionalPair().isBundleOfIpLinks();
		assert !ipBidiLink.isBundleMember();
		assert !ipBidiLink.getBidirectionalPair().isBundleMember();

		final WNet wNet = ipBidiLink.getNet();

		boolean atLeastOneLp = false;
        Quintuple<String, Double, Double, Double, Integer> choiceOfTransponderForThisRateAndReach = null;
        List<List<Pair<WNode, WNode>>> twoOrOneWdmPathsForThisIpLink = null;
        final boolean mcen2mcen = !NODETYPE.isAmen(ipBidiLink.getA()) && !NODETYPE.isAmen(ipBidiLink.getB());
        double commonRateForAllLpsThisIpLinkGbps = 0;
        double trafficToCarryInEachPathOrThePathIfOnlyOne = 0;
        boolean onlyOnePathExists = false;
        boolean faultTolerance_inner = addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible.getBoolean();


		/* Compute the lps to establish and their routes */
		do {
//            final List<List<Pair<WNode, WNode>>> twoOrOneWdmPathsForThisIpLink = addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible.getBoolean() ?
//                    getTwoMaximumLinkDisjointPaths(wdmShortestPathKmCalculator_graph, ipBidiLink.getA(), ipBidiLink.getB()) :
//                    Arrays.asList(getShortestPathAsSeqNodePairs(wdmShortestPathKmCalculator_graph, wdmShortestPathKmCalculator, ipBidiLink.getA(), ipBidiLink.getB()));

            twoOrOneWdmPathsForThisIpLink = faultTolerance_inner ?
                    getTwoMaximumLinkDisjointPaths(wdmShortestPathKmCalculator_graph, ipBidiLink.getA(), ipBidiLink.getB()) :
                    Arrays.asList(getShortestPathAsSeqNodePairs(wdmShortestPathKmCalculator_graph, wdmShortestPathKmCalculator, ipBidiLink.getA(), ipBidiLink.getB()));

            if (twoOrOneWdmPathsForThisIpLink.isEmpty()) throw new Net2PlanException("No WDM path for this IP link");
            if (twoOrOneWdmPathsForThisIpLink.stream().anyMatch(l -> l.isEmpty()))
                throw new Net2PlanException("No WDM path for this IP link");
            assert twoOrOneWdmPathsForThisIpLink.size() <= 2;
            final boolean onlyOnePathExists_inner = twoOrOneWdmPathsForThisIpLink.size() == 1;
            onlyOnePathExists = onlyOnePathExists_inner;
            //if (onlyOnePathExists) System.out.println("ONLY ONE PATH EXISTS FOR THE IP LINK: " + ipBidiLink);

            final double worstCasePathLength = twoOrOneWdmPathsForThisIpLink.stream().mapToDouble(path -> path.stream().mapToDouble(pair -> wNet.getNodePairFibers(pair.getFirst(), pair.getSecond()).first().getLengthInKm()).sum()).max().getAsDouble();
            final double trafficToCarryNoFailureStateGbps = Math.max(ipBidiLink.getCarriedTrafficGbps(), ipBidiLink.getBidirectionalPair().getCarriedTrafficGbps());
            final double wcTrafficToCarryMcenBbFailureGbps = faultTolerance_inner ?
                    ipTrafficPerNodePairPerFailure_f.values().stream().mapToDouble(ee -> ee.get(ipBidiLink)).max().orElseThrow(RuntimeException::new) :
                    0.0;
            /* If one path => no WDM fault tolerance requested: just max between no failure and MCENBB failure
             * If two paths => WDM fault tolerance requested: if WDM fails, other path needs enough for noFail traffic. If MCEN BB fails: summing two paths need enough for MCENBB traffic    */
            final double trafficToCarryInEachPathOrThePathIfOnlyOne_inner = onlyOnePathExists_inner ? Math.max(trafficToCarryNoFailureStateGbps, wcTrafficToCarryMcenBbFailureGbps) : Math.max(trafficToCarryNoFailureStateGbps, wcTrafficToCarryMcenBbFailureGbps / 2);
            trafficToCarryInEachPathOrThePathIfOnlyOne = trafficToCarryInEachPathOrThePathIfOnlyOne_inner;

            // Order: 1) number transponders would be needed, 2) order of the user
            final double commonRateForAllLpsThisIpLinkGbps_inner = IntStream.range(0, getNumberOfDefinedTransponders(mcen2mcen)).mapToObj(i -> getTransponderNameRateGbpsCostReachKmNumSlots(i, mcen2mcen)).sorted((tp1, tp2) -> {
                final double rateGbps1 = tp1.getSecond();
                final int numLps1 = (int) Math.ceil(trafficToCarryInEachPathOrThePathIfOnlyOne_inner / rateGbps1);
                final double rateGbps2 = tp2.getSecond();
                final int numLps2 = (int) Math.ceil(trafficToCarryInEachPathOrThePathIfOnlyOne_inner / rateGbps2);
                if (numLps1 != numLps2) return Integer.compare(numLps1, numLps2);
                return Double.compare(rateGbps1, rateGbps2);
            }).map(tp -> tp.getSecond()).distinct().findFirst().get();

            commonRateForAllLpsThisIpLinkGbps = commonRateForAllLpsThisIpLinkGbps_inner;

            choiceOfTransponderForThisRateAndReach = IntStream.range(0, getNumberOfDefinedTransponders(mcen2mcen)).
//            final Quintuple<String, Double, Double, Double, Integer> choiceOfTransponderForThisRateAndReach = IntStream.range(0, getNumberOfDefinedTransponders(mcen2mcen)).
                    mapToObj(i -> getTransponderNameRateGbpsCostReachKmNumSlots(i, mcen2mcen)).
                    filter(tp -> tp.getSecond() == commonRateForAllLpsThisIpLinkGbps_inner).
                    filter(tp -> tp.getFourth() >= worstCasePathLength).
                    findFirst().orElse(null);

            if(choiceOfTransponderForThisRateAndReach != null) {
                atLeastOneLp = true;
            }
            else
                faultTolerance_inner = false;

//            if (choiceOfTransponderForThisRateAndReach == null)
//            {
//                System.out.println("mcen2mcen: " + mcen2mcen);
//                for(int i = 0; i <getNumberOfDefinedTransponders(mcen2mcen); i++)
//                    System.out.println("Transponder " + i + ": " + getTransponderNameRateGbpsCostReachKmNumSlots(i,mcen2mcen));
//
//                System.out.println("trafficToCarryInEachPathOrThePathIfOnlyOne: " + trafficToCarryInEachPathOrThePathIfOnlyOne);
//                System.out.println("worstCasePathLength: " + worstCasePathLength);
//                System.out.println("commonRateForAllLpsThisIpLinkGbps: " + commonRateForAllLpsThisIpLinkGbps);
//            }

        }while (atLeastOneLp != true);

		if (choiceOfTransponderForThisRateAndReach == null) throw new Net2PlanException ("No transponder option exists for this rate and reach"); // no transponder has enough reach at this rate
		final int numLpsEachPathOrThePathOfOnlyOne = (int) Math.ceil(trafficToCarryInEachPathOrThePathIfOnlyOne / commonRateForAllLpsThisIpLinkGbps);
		final int numSlotsPerLp = choiceOfTransponderForThisRateAndReach.getFifth();

//		/* Compute the traversed WDM links, considering filterless propagation if any */
//		final List<SortedSet<WFiber>> occupiedExtraWdmLinksBecauseDropAndWasteAbBa_perPath = new ArrayList<> (2);
//		for (List<Pair<WNode,WNode>> path : twoOrOneWdmPathsForThisIpLink)
//		{
//			final SortedSet<WNode> traversedNodes = new TreeSet<> (); path.forEach(p->{ traversedNodes.add(p.getFirst()); traversedNodes.add(p.getSecond()); });
//			if (traversedNodes.stream().allMatch(n->n.getOpticalSwitchingArchitecture().isNeverCreatingWastedSpectrum())) 
//			{
//				occupiedExtraWdmLinksBecauseDropAndWasteAbBa_perPath.add(new TreeSet<> ());
//				continue;
//			}
//			/* At least one D&W somewhere */
//			final List<WFiber> spAbFirstFiberInBundle = path.stream().map(p->wNet.getNodePairFibers(p.getFirst (), p.getSecond()).first()).collect(Collectors.toList());
//			final List<WFiber> spBaFirstFiberInBundle = Lists.reverse(path.stream().map(p->wNet.getNodePairFibers(p.getSecond(), p.getFirst()).first()).collect(Collectors.toList()));
//			assert Sets.intersection(new HashSet<> (spAbFirstFiberInBundle), new HashSet<> (spBaFirstFiberInBundle)).isEmpty();
//			final Triple<SortedSet<WFiber>, List<List<WFiber>>, Boolean> lasingInfoAb = this.osm.getPropagatingFibersLasingLoopsAndIsMultipathOk(spAbFirstFiberInBundle);
//			final Triple<SortedSet<WFiber>, List<List<WFiber>>, Boolean> lasingInfoBa = this.osm.getPropagatingFibersLasingLoopsAndIsMultipathOk(spBaFirstFiberInBundle);
//			final boolean isUsableAb = lasingInfoAb.getSecond().isEmpty() && lasingInfoAb.getThird();
//			final boolean isUsableBa = lasingInfoBa.getSecond().isEmpty() && lasingInfoBa.getThird();
//			if (!isUsableAb || !isUsableBa) throw new Net2PlanException ("The topology has lasing loops for the path " + path);
//			final SortedSet<WFiber> thisPath_occupiedExtraWdmLinksBecauseDropAndWasteAbBa = new TreeSet<> ();
//			thisPath_occupiedExtraWdmLinksBecauseDropAndWasteAbBa.addAll (lasingInfoAb.getFirst());
//			thisPath_occupiedExtraWdmLinksBecauseDropAndWasteAbBa.addAll (lasingInfoBa.getFirst());
//			thisPath_occupiedExtraWdmLinksBecauseDropAndWasteAbBa.removeAll(spAbFirstFiberInBundle);
//			thisPath_occupiedExtraWdmLinksBecauseDropAndWasteAbBa.removeAll(spBaFirstFiberInBundle);
//			occupiedExtraWdmLinksBecauseDropAndWasteAbBa_perPath.add(thisPath_occupiedExtraWdmLinksBecauseDropAndWasteAbBa);
//			assert thisPath_occupiedExtraWdmLinksBecauseDropAndWasteAbBa.stream().allMatch(e->!path.contains(Pair.of(e.getA(), e.getB())));
//		}

		/* If all required lps created, end */
		/* If not all required lps created, then we need to try other rate. Remove all the lps created if any */

		final List<WLightpathRequest> lpsCreatedAb = new ArrayList<>();
		for (int pathIndex0Or1 : Arrays.asList(0,1))
		{
			if (pathIndex0Or1 == 1 && twoOrOneWdmPathsForThisIpLink.size() == 1) continue;
			final List<Pair<WNode,WNode>> spAbNodeSeq_baIsReversePath = twoOrOneWdmPathsForThisIpLink.get(pathIndex0Or1);
//			final SortedSet<WFiber> thisPath_occupiedExtraWdmLinksBecauseDropAndWasteAbBa = occupiedExtraWdmLinksBecauseDropAndWasteAbBa_perPath.get(pathIndex0Or1);
			
			for (int numLp = 0; numLp < numLpsEachPathOrThePathOfOnlyOne ; numLp ++)
			{
//				final SortedSet<Integer> unusableSlotsCausedByDropAndWaste = thisPath_occupiedExtraWdmLinksBecauseDropAndWasteAbBa.stream().
//						map(e->this.osm.getOccupiedOpticalSlotIds(e)).flatMap(e->e.stream()).collect(Collectors.toCollection(TreeSet::new));
				
				/* Assignment with both bidi-directions using the same slots */
				Optional<Pair<List<Pair<WFiber,WFiber>> , SortedSet<Integer>>> assignment = this.osm.spectrumAssignment_firstFitForAdjacenciesBidi(spAbNodeSeq_baIsReversePath, 
						ipBidiLink.getA(), ipBidiLink.getB(), 
						Optional.empty() , Optional.empty() , 
						numSlotsPerLp, Optional.empty(), 
						new TreeSet<> ());

//				Optional<Pair<List<Pair<WFiber,WFiber>> , SortedSet<Integer>>> assignment = this.osm.spectrumAssignment_firstFitForAdjacenciesBidi (spAbNodeSeq_baIsReversePath, numSlotsPerLp , unusableSlotsCausedByDropAndWaste);
				if (!assignment.isPresent())
				{
					final List<Pair<Integer,Integer>> validOpticalSlotRanges = getValidOpticalSlotRange (fiberDefaultValidSlotRanges.getString());
					final Map<SortedSet<WNode> , Integer> numIdleSlotsBusiestFiberBothDirections = new HashMap<> ();
					for (Pair<WNode,WNode> nodePair : spAbNodeSeq_baIsReversePath)
					{
						final WNode a = nodePair.getFirst();
						final WNode b = nodePair.getSecond();
						final SortedSet<WNode> key = ImmutableSortedSet.of(a , b);
						assert !numIdleSlotsBusiestFiberBothDirections.containsKey(key);
						final SortedSet<WFiber> fibersAb = wNet.getNodePairFibers(a, b);
						final SortedSet<WFiber> fibersBa = wNet.getNodePairFibers(b, a);
						final int numIdleSlotsBusiestFiber_ab = fibersAb.stream().mapToInt(e->osm.getIdleOpticalSlotIds(e).size()).max().orElse(0);
						final int numIdleSlotsBusiestFiber_ba = fibersBa.stream().mapToInt(e->osm.getIdleOpticalSlotIds(e).size()).max().orElse(0);
						numIdleSlotsBusiestFiberBothDirections.put(key, Math.min(numIdleSlotsBusiestFiber_ab, numIdleSlotsBusiestFiber_ba));
					}
					/* Order the fiber pairs according to its number of idle slots, less to more */
					final List<SortedSet<WNode>> orderedNodePairsToUpdate = numIdleSlotsBusiestFiberBothDirections.keySet().stream().
							sorted ((p1,p2)->Integer.compare(numIdleSlotsBusiestFiberBothDirections.get(p1), numIdleSlotsBusiestFiberBothDirections.get(p2))).
							collect (Collectors.toList());
					for (SortedSet<WNode> nodePairToAddFiber : orderedNodePairsToUpdate)
					{
						assert nodePairToAddFiber.size() == 2;
						final WNode a = nodePairToAddFiber.first();
						final WNode b = nodePairToAddFiber.last();
						final double dist_km = wNet.getNodePairFibers(a, b).first().getLengthInKm();
                        Pair<WFiber,WFiber> newFiber = wNet.addFiber(a, b, validOpticalSlotRanges, dist_km, true);

						/* Cannot be filterless now, if we have to upgrade the topology */
						if (a.getOpticalSwitchingArchitecture().isPotentiallyWastingSpectrum() || b.getOpticalSwitchingArchitecture().isPotentiallyWastingSpectrum()) 
						{
							a.getOpticalSwitchingArchitecture().getAsOadmGeneric().updateParameters(a.getOpticalSwitchingArchitecture().getAsOadmGeneric().getParameters().setArchitectureTypeAsBroadcastAndSelect());
							b.getOpticalSwitchingArchitecture().getAsOadmGeneric().updateParameters(b.getOpticalSwitchingArchitecture().getAsOadmGeneric().getParameters().setArchitectureTypeAsBroadcastAndSelect());
							
							/* Making this change can imply that allocated waste slots to previous lightpaths are no longer, waste.
							 * This means OpticalSpectrumManager has a pessimistic view of occupation. */ 
						}
						assignment = this.osm.spectrumAssignment_firstFitForAdjacenciesBidi(spAbNodeSeq_baIsReversePath, 
								ipBidiLink.getA(), ipBidiLink.getB(), 
								Optional.empty() , Optional.empty() , 
								numSlotsPerLp, Optional.empty(), 
								new TreeSet<> ());

                        final int originalFiberIndex = originalFibers.indexOf(wNet.getNodePairFibers(a, b).first());
                        final int originalFiberIndexBi = originalFibers.indexOf(wNet.getNodePairFibers(b, a).first());
                        final int totalNewFibersThisLink = newFibers.get(originalFiberIndex) + 1 ;
                        final int totalNewFibersThisLinkBi = newFibers.get(originalFiberIndex) + 1 ;
                        newFibers.set(originalFiberIndex, totalNewFibersThisLink);
                        newFibers.set(originalFiberIndexBi,totalNewFibersThisLinkBi);

                        newFiberSIndex.set(originalFiberIndex, wNet.getFibers().indexOf(newFiber.getFirst()));
                        newFiberSIndex.set(originalFiberIndexBi,wNet.getFibers().indexOf(newFiber.getSecond()));

						if (assignment.isPresent()) break;
					}
				}
				
				if (!assignment.isPresent()) throw new Net2PlanException ("Not enough bandwidth, nor available new fiber for a lightpath");
				
				final List<WFiber> seqFibersAb = assignment.get().getFirst().stream().map(p->p.getFirst()).collect(Collectors.toList ());
				final List<WFiber> seqFibersBa = Lists.reverse(assignment.get().getFirst().stream().map(p->p.getSecond()).collect(Collectors.toList ()));
				final WLightpathRequest lprAb = wNet.addLightpathRequest(ipBidiLink.getA(), ipBidiLink.getB(), commonRateForAllLpsThisIpLinkGbps, false);
				final WLightpathRequest lprBa = wNet.addLightpathRequest(ipBidiLink.getB(), ipBidiLink.getA(), commonRateForAllLpsThisIpLinkGbps, false);
				lprAb.setBidirectionalPair(lprBa);
				final WLightpath lpAb = lprAb.addLightpathUnregenerated(seqFibersAb, assignment.get().getSecond(), false);
				// lpA.setAttribute("TpName",tp.getFirst());
				final WLightpath lpBa = lprBa.addLightpathUnregenerated(seqFibersBa, assignment.get().getSecond(), false);
				lprAb.setTransponderName(choiceOfTransponderForThisRateAndReach.getFirst());
				lprBa.setTransponderName(choiceOfTransponderForThisRateAndReach.getFirst());
				lpsCreatedAb.add(lprAb);
				this.osm.allocateOccupation(lpAb , Optional.empty());
				this.osm.allocateOccupation(lpBa , Optional.empty());
			}
		}

		if (lpsCreatedAb.isEmpty())
			throw new Net2PlanException("Impossible to create the required lightpaths for the IP link " + ipBidiLink + ", rate: " + commonRateForAllLpsThisIpLinkGbps);

		/* Create the WIpLinks and bundle them */
		for (WLightpathRequest lprAb : lpsCreatedAb)
		{
			final Pair<WIpLink, WIpLink> ipLinkAbBa = wNet.addIpLinkBidirectional(ipBidiLink.getA(), ipBidiLink.getB(), lprAb.getLineRateGbps());
			lprAb.coupleToIpLink(ipLinkAbBa.getFirst());
			lprAb.getBidirectionalPair().coupleToIpLink(ipLinkAbBa.getSecond());
		}
		ipBidiLink.setIpLinkAsBundleOfIpLinksBidirectional(lpsCreatedAb.stream().map(lpr -> lpr.getCoupledIpLink()).collect(Collectors.toCollection(TreeSet::new)));
		ipBidiLink.getBidirectionalPair().setIpLinkAsBundleOfIpLinksBidirectional(lpsCreatedAb.stream().map(lpr -> lpr.getBidirectionalPair()).map(lpr -> lpr.getCoupledIpLink()).collect(Collectors.toCollection(TreeSet::new)));

        final List<List<Pair<WNode, WNode>>> twoOrOneWdmPathsForThisIpLink_final = new LinkedList<>(twoOrOneWdmPathsForThisIpLink) ;
		final boolean notFaultToleranceToWdmFailureAchieved = onlyOnePathExists? true : twoOrOneWdmPathsForThisIpLink_final.get(0).stream().anyMatch(nn->twoOrOneWdmPathsForThisIpLink_final.get(1).contains(nn));
//		if (notFaultToleranceToWdmFailureAchieved)
//			System.out.println("ONLY ONE PATH EXISTS OR TWO PATHS WITH COMMON WDM HOPS FOR THE IP LINK: " + ipBidiLink);
		
		return notFaultToleranceToWdmFailureAchieved;
	}

	public OpticalSpectrumManager getOpticalSpectrumManager () { return this.osm; }

	
	private static <V,E> List<List<Pair<V,V>>> getTwoMaximumLinkDisjointPaths (DirectedWeightedMultigraph<V , E> graph , V originNode, V destinationNode)
   {
		final Function<E,Pair<V,V>> toNodePair = e -> Pair.of(graph.getEdgeSource(e), graph.getEdgeTarget(e));
		
			final List<GraphPath<V,E>> paths = new SuurballeKDisjointShortestPaths<>(graph).getPaths(originNode, destinationNode, 2);
       List<List<E>> resAsLinks = paths.stream().map(gp->gp.getEdgeList()).collect(Collectors.toCollection(ArrayList::new));
       
       if (resAsLinks.size() == 1)
       {
           final Set<E> traversedLinks = new HashSet<> (resAsLinks.get(0));
           final Map<E,Double> originalLinkCost = new HashMap<> ();
           double sumLinkCostsExcludingDoubleMaxValues = 0;
           for (E e : graph.edgeSet())
           {
           	final double currentWeight = graph.getEdgeWeight(e);
           	if (currentWeight != Double.MAX_VALUE && currentWeight != Double.POSITIVE_INFINITY) sumLinkCostsExcludingDoubleMaxValues += currentWeight;
           	if (traversedLinks.contains(e))
           		originalLinkCost.put(e, currentWeight);
           }
           for (E edgeToChangeWeight : originalLinkCost.keySet()) graph.setEdgeWeight(edgeToChangeWeight, sumLinkCostsExcludingDoubleMaxValues + 1.0);
           final DijkstraShortestPath<V , E> dsp = new DijkstraShortestPath<>(graph);
           final List<E> spModified = dsp.getPath(originNode, destinationNode).getEdgeList();
           for (Entry<E,Double> modifiedCost : originalLinkCost.entrySet()) graph.setEdgeWeight(modifiedCost.getKey(), modifiedCost.getValue());
           if (!resAsLinks.get(0).equals(spModified))
         	  resAsLinks.add(spModified); // only add if there is at least one different hop
       }
       return resAsLinks.stream().map(gp->gp.stream().map(ee->toNodePair.apply(ee)).collect(Collectors.toList())).collect(Collectors.toCollection(ArrayList::new));
   }

	private static <V,E> List<Pair<V,V>> getShortestPathAsSeqNodePairs (org.jgrapht.graph.DirectedWeightedMultigraph<V,E> graph , org.jgrapht.alg.shortestpath.DijkstraShortestPath<V , E> spCalculator , V originNode, V destinationNode)
   {
		final Function<E,Pair<V,V>> toNodePair = e -> Pair.of(graph.getEdgeSource(e), graph.getEdgeTarget(e));
		final GraphPath<V,E> path = spCalculator.getPath(originNode, destinationNode);
		if (path == null) return new ArrayList<> ();
      return path.getEdgeList().stream().map(ee->toNodePair.apply(ee)).collect(Collectors.toList());
   }

   public List<Integer> getNewFibers(){
	    return newFibers;
   }

    public List<Integer> getNewFibersIndexes(){
        return newFiberSIndex;
    }

    
    private static List<Pair<Integer,Integer>> getValidOpticalSlotRange (String commaSeparatedNumbers)
    {
		final List<Integer> validOpticalSlotRangesAsList = Stream.of(commaSeparatedNumbers.split(",")).map(d -> Integer.parseInt(d.trim())).map(d -> d.intValue()).collect(Collectors.toList());
		final List<Pair<Integer,Integer>> validOpticalSlotRanges = new ArrayList<> ();
		if (validOpticalSlotRangesAsList.isEmpty() || validOpticalSlotRangesAsList.size() % 2 != 0) throw new Net2PlanException ("Wrong slot ranges");
		final Iterator<Integer> it = validOpticalSlotRangesAsList.iterator();
		while (it.hasNext())
		{
			final int first = it.next();
			final int second = it.next();
			validOpticalSlotRanges.add(Pair.of(first, second));
		}
		return validOpticalSlotRanges;
    }
	
}
