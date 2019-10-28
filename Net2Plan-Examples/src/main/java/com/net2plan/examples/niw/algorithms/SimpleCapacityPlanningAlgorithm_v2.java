/* Problems: Islands are not linear (seldom they are). Many degree > 2 AMENs Some AMEN islands are huge (41 nodes) */

/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/


package com.net2plan.examples.niw.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.shortestpath.SuurballeKDisjointShortestPaths;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import com.google.common.collect.Lists;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.niw.OpticalSpectrumManager;
import com.net2plan.niw.DefaultStatelessSimulator;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WIpLink;
import com.net2plan.niw.WIpUnicastDemand;
import com.net2plan.niw.WLightpath;
import com.net2plan.niw.WLightpathRequest;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNode;
import com.net2plan.niw.WServiceChainRequest;
import com.net2plan.niw.WVnfInstance;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Quintuple;
import com.net2plan.utils.Triple;



public class SimpleCapacityPlanningAlgorithm_v2 implements IAlgorithm
{
	private static boolean DEBUG = true;

	private OpticalSpectrumManager osm;

	private org.jgrapht.graph.DirectedWeightedMultigraph<WNode, Object> wdmShortestPathKmCalculator_graph;
	private org.jgrapht.alg.shortestpath.DijkstraShortestPath<WNode, Object> wdmShortestPathKmCalculator;

	private InputParameter fiberDefaultValidSlotRanges = new InputParameter("fiberDefaultValidSlotRanges", "0 , 320",
			"This is a list of 2*K elements, being K the number of contiguous ranges. First number in a range is \r\n"
					+ "	 * the id of the initial optical slot, second number is the end. For instance, a range setting [0 300 320 360] means that the fiber is able to propagate the \r\n"
					+ "	 * optical slots from 0 to 300 both included, and from 320 to 360 both included.  ");

	private InputParameter use11MaxWdmDisjointLightpathsIfPossible = new InputParameter("use11MaxWdmDisjointLightpathsIfPossible", true , "");
	
	private InputParameter minimumUtilizationToJustifyIpLinkInSmallerTransponder = new InputParameter("minimumUtilizationToJustifyIpLinkInSmallerTransponder", 0.1,
			"IP links with a traffic below this utilization is the smaller (in rate) transponder possible was used, are attempted to be removed, and its traffic groomed", 0, false, 1.0, true);

    private InputParameter transponderOptionsNameRateGbpsCostReachKmNumSlots = new InputParameter("transponderOptionsRateGbpsCostReachKmNumSlots", "TP 100 5 5000 8",
            "Space-separated tuple of transponder name, rate in Gbps, cost, reack in km and number of optical slots, information of different transponders is comma separated");

	@Override
	public String executeAlgorithm(NetPlan np, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		// First of all, initialize all parameters
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		final WNet wNet = new WNet (np);
		try { wNet.checkConsistency(); } catch (Throwable e) { e.printStackTrace(); throw new Net2PlanException ("Not a NIW-valid design: " + e.getMessage()); }

		Configuration.setOption("precisionFactor", new Double(1e-9).toString());
		Configuration.precisionFactor = 1e-9;

		/* Remove current parts */
		for (WLightpathRequest e : new ArrayList<> (wNet.getLightpathRequests())) e.remove();
		for (WIpLink e : new ArrayList<> (wNet.getIpLinks())) e.removeBidirectional();
		for (WServiceChainRequest e : new ArrayList<> (wNet.getServiceChainRequests())) e.remove();
		for (WIpUnicastDemand e : wNet.getIpUnicastDemands()) e.setAsHopByHopRouted();
		for (WVnfInstance e : new ArrayList<>(wNet.getVnfInstances())) e.remove();

		/* Modify the fiber structure for the algorithm */
		final List<Integer> validOpticalSlotRangesList = Stream.of(fiberDefaultValidSlotRanges.getString().split(",")).map(d -> Integer.parseInt(d.trim())).map(d -> d.intValue()).collect(Collectors.toList());
		final List<Pair<Integer,Integer>> validOpticalSlotRanges = new ArrayList<> ();
		final Iterator<Integer> itList = validOpticalSlotRangesList.iterator();
		while (itList.hasNext()) validOpticalSlotRanges.add(Pair.of(itList.next(), itList.next()));
		for (WFiber e : wNet.getFibers ())
			e.setValidOpticalSlotRanges(validOpticalSlotRanges);

		/* All the fibers should have all the optical slots idle */
		this.osm = OpticalSpectrumManager.createFromRegularLps(wNet);
		assert wNet.getFibers().stream().allMatch(e->osm.getOccupiedOpticalSlotIds(e).isEmpty());
		assert wNet.getFibers().stream().allMatch(e->e.isBidirectional());

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

		/* Creates the base topology of IP links, and adds a graph to it */
		createIpToplogy(wNet , algorithmParameters , net2planParameters);

		/* Create the lightpath requests, and then apply the RWAs */
		for (WIpLink e : wNet.getIpLinks())
			if (e.getId() > e.getBidirectionalPair().getId()) 
				createBundleOfIpLinkWithLightpathsAndAssignTransponders(e);

		/* Reroute the traffic appropriately */
		DefaultStatelessSimulator.run(wNet , Optional.empty());
		
		/* Check all the WDM links for which fault tolerance is not possible for topological reasons */
//		System.out.println("-------------------------------------------------------------------------");
//		System.out.println("--- WDM cut fault tolerance NOT possible for topological reasons WDM cuts in node pairs. Affected IP bundleS: (" +ipBundleLinksWithoutWdmFaultTolerancePossible.size() + "): " + ipBundleLinksWithoutWdmFaultTolerancePossible);
//		System.out.println("-------------------------------------------------------------------------");

		
		/* Check that all is carried, no oversubscription, WDM links */
		if (DEBUG)
		{
			assert wNet.getServiceChainRequests().isEmpty();
			assert wNet.getIpUnicastDemands().stream().allMatch(d->d.getCurrentBlockedTraffic() < 1e-3);
			assert wNet.getIpLinks().stream().allMatch(e->e.getCarriedTrafficGbps() <= e.getCurrentCapacityGbps() + 1e-3);
			assert wNet.getIpLinks().stream().allMatch(e->e.isBundleMember() || e.isBundleOfIpLinks());
			assert wNet.getIpLinks().stream().filter(e->e.isBundleMember()).allMatch(e->e.isCoupledtoLpRequest());
			assert wNet.getLightpathRequests().stream().allMatch(e->e.getLightpaths().size() == 1);
			assert this.osm.isSpectrumOccupationOk ();
			
			if (use11MaxWdmDisjointLightpathsIfPossible.getBoolean())
			{
				assert wNet.getLightpathRequests().stream().allMatch(e->e.getLightpaths().size() == 2);
				assert wNet.getLightpathRequests().stream().allMatch(e->e.getLightpaths().get(0).isMainLightpathWithBackupRoutes());
				assert wNet.getLightpathRequests().stream().allMatch(e->e.getLightpaths().get(1).isBackupLightpath());
				assert wNet.getLightpathRequests().stream().allMatch(e->!e.getLightpaths().get(0).equals (e.getLightpaths().get(1)));
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

	private Quintuple<String, Double, Double, Double, Integer> getTransponderNameRateGbpsCostReachKmNumSlots(int index)
	{
		final String s = transponderOptionsNameRateGbpsCostReachKmNumSlots.getString();
		final String[] vals = s.split(",");
		if (index >= vals.length) throw new RuntimeException();
		final String sTransponder = vals[index].trim();
		final List<String> valsThisTransponder = Stream.of(sTransponder.split(" ")).collect(Collectors.toList());
		return Quintuple.of(valsThisTransponder.get(0), Double.parseDouble(valsThisTransponder.get(1)), Double.parseDouble(valsThisTransponder.get(2)), Double.parseDouble(valsThisTransponder.get(3)),
				(int) Double.parseDouble(valsThisTransponder.get(4)));
	}

	private int getNumberOfDefinedTransponders()
	{
		final String s = transponderOptionsNameRateGbpsCostReachKmNumSlots.getString();
		final String[] vals = s.split(",");
		return vals.length;
	}

	public List<Quintuple<String, Double, Double, Double, Integer>> getTransponderTypes()
	{
		final List<Quintuple<String, Double, Double, Double, Integer>> listOfTransponders = new ArrayList<>();

		for (int i = 0; i < getNumberOfDefinedTransponders(); i++)
			listOfTransponders.add(getTransponderNameRateGbpsCostReachKmNumSlots(i));
		return listOfTransponders;
	}

	private void createIpToplogy(WNet wNet , Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Create the base topology of IP links. Do not add those forbidden because of policy / not feasilbe because of
		 * transponder reach */
		for (WNode a : wNet.getNodes())
		{
			for (WNode b : wNet.getNodes())
			{
				if (a.getId() > b.getId())
				{
					/* Check if a transponder can make it */
					final double spDistanceWdmKm = this.wdmShortestPathKmCalculator.getPathWeight(a, b);
					if (spDistanceWdmKm ==  Double.POSITIVE_INFINITY) throw new Net2PlanException("There is no WDM path from node " + a + " to node " + b);
					final int numTps = getNumberOfDefinedTransponders();
					final boolean noTpCanMakeIt = IntStream.rangeClosed(0, numTps - 1).allMatch(i -> getTransponderNameRateGbpsCostReachKmNumSlots(i).getFourth() < spDistanceWdmKm);
					if (noTpCanMakeIt) continue;
					wNet.addIpLinkBidirectional(a, b, Double.MAX_VALUE);
				}
			}
		}

		/* Loop for removing low utilized IP links if possible */
		assert wNet.getIpLinks().stream().allMatch(ee->!ee.isBundleMember());
		final double utTh = minimumUtilizationToJustifyIpLinkInSmallerTransponder.getDouble();
		if (utTh <= 0) return;
		boolean atLeastOneIpLinkRemoved = true;
		
		while (atLeastOneIpLinkRemoved)
		{
			atLeastOneIpLinkRemoved = false;
			
			DefaultStatelessSimulator.run(wNet , Optional.empty());
			
			for (WIpLink e : wNet.getIpLinks().stream().sorted((e1, e2) -> Double.compare(e1.getCarriedTrafficGbps(), e2.getCarriedTrafficGbps())).collect(Collectors.toList()))
			{
				if (e.wasRemoved()) continue;
				if (e.isBidirectional()) if (e.getId() <= e.getBidirectionalPair().getId()) continue;
				final int numTps = getNumberOfDefinedTransponders();
				final double minRateGbps = IntStream.rangeClosed(0, numTps - 1).mapToDouble(i -> getTransponderNameRateGbpsCostReachKmNumSlots(i).getSecond()).min().orElse(0.0);
				if (minRateGbps <= 0) continue;
				final double ipLinkTrafficGbpsWc = Math.max(e.getCarriedTrafficGbps() , e.getBidirectionalPair().getCarriedTrafficGbps());
				if (ipLinkTrafficGbpsWc < minRateGbps * utTh)
				{
					/* Try to remove link */
					final WNode a = e.getA();
					final WNode b = e.getB();
					
					/* Actually remove the IP link and its bidi pair */
					e.removeBidirectional();

					/* If removing the IP adjacency leaves the end nodes unconnected => not possible, add the IP link again */
					final boolean theIPLinkHasTraffic = ipLinkTrafficGbpsWc > Configuration.precisionFactor;
					final boolean theIpLinkEndNodesBecomeDisconnected = wNet.getKShortestIpUnicastPath(1, wNet.getNodes(), wNet.getIpLinks(), a, b, Optional.empty()).isEmpty(); 
					if (theIPLinkHasTraffic && theIpLinkEndNodesBecomeDisconnected)
					{
						wNet.addIpLinkBidirectional (a, b, Double.MAX_VALUE);
						continue;
					}
						
					atLeastOneIpLinkRemoved = true;
					break;
				}
				if (!atLeastOneIpLinkRemoved) break;
			}
		}
		
		/* Return with an updated design */
		DefaultStatelessSimulator.run(wNet , Optional.empty());
		
		assert wNet.getIpLinks().stream().allMatch(e->Math.max(e.getCarriedTrafficGbps() , e.getBidirectionalPair().getCarriedTrafficGbps()) > Configuration.precisionFactor);
	}

	private void createBundleOfIpLinkWithLightpathsAndAssignTransponders(WIpLink ipBidiLink)
	{
		assert ipBidiLink.isBidirectional();
		assert !ipBidiLink.isCoupledtoLpRequest();
		assert !ipBidiLink.getBidirectionalPair().isCoupledtoLpRequest();
		assert !ipBidiLink.isBundleOfIpLinks();
		assert !ipBidiLink.getBidirectionalPair().isBundleOfIpLinks();
		assert !ipBidiLink.isBundleMember();
		assert !ipBidiLink.getBidirectionalPair().isBundleMember();

		final WNet wNet = ipBidiLink.getNet();
		final Function<List<Pair<WNode,WNode>>,Double> pathLengthKm = p -> p.stream().mapToDouble(pair -> wNet.getNodePairFibers(pair.getFirst(), pair.getSecond()).first().getLengthInKm()).sum();
		final double reachInKmOfLongerReachTransponder = getTransponderTypes().stream().mapToDouble(tp->tp.getFourth()).max().orElse(-1.0);

        /* Compute the lps to establish and their routes, limiting to those within at least one transponder pair */
         List<List<Pair<WNode, WNode>>> twoOrOneWdmPathsForThisIpLink = use11MaxWdmDisjointLightpathsIfPossible.getBoolean() ?
                 getTwoMaximumLinkDisjointPaths(wdmShortestPathKmCalculator_graph, ipBidiLink.getA(), ipBidiLink.getB()) :
                 Arrays.asList(getShortestPathAsSeqNodePairs(wdmShortestPathKmCalculator_graph, wdmShortestPathKmCalculator, ipBidiLink.getA(), ipBidiLink.getB()));
        twoOrOneWdmPathsForThisIpLink = twoOrOneWdmPathsForThisIpLink.stream().filter(p->pathLengthKm.apply(p) <= reachInKmOfLongerReachTransponder).collect(Collectors.toList());
                 
         if (twoOrOneWdmPathsForThisIpLink.isEmpty()) throw new Net2PlanException("No valid WDM path for this IP link");
         if (twoOrOneWdmPathsForThisIpLink.stream().anyMatch(l -> l.isEmpty())) throw new Net2PlanException("No WDM path for this IP link");
         assert twoOrOneWdmPathsForThisIpLink.size() <= 2;

         final double worstCasePathLength = twoOrOneWdmPathsForThisIpLink.stream().mapToDouble(p -> pathLengthKm.apply(p)).max().getAsDouble();
         final double trafficToCarryNoFailureStateGbps = Math.max(ipBidiLink.getCarriedTrafficGbps(), ipBidiLink.getBidirectionalPair().getCarriedTrafficGbps());

            // Order: 1) number transponders would be needed, 2) order of the user
         final double commonRateForAllLpsThisIpLinkGbps = IntStream.range(0, getNumberOfDefinedTransponders()).mapToObj(i -> getTransponderNameRateGbpsCostReachKmNumSlots(i)).sorted((tp1, tp2) -> {
                final double rateGbps1 = tp1.getSecond();
                final int numLps1 = (int) Math.ceil(trafficToCarryNoFailureStateGbps / rateGbps1);
                final double rateGbps2 = tp2.getSecond();
                final int numLps2 = (int) Math.ceil(trafficToCarryNoFailureStateGbps / rateGbps2);
                if (numLps1 != numLps2) return Integer.compare(numLps1, numLps2);
                return Double.compare(rateGbps1, rateGbps2);
            }).map(tp -> tp.getSecond()).distinct().findFirst().get();

         final Quintuple<String, Double, Double, Double, Integer> choiceOfTransponderForThisRateAndReach = IntStream.range(0, getNumberOfDefinedTransponders()).
                    mapToObj(i -> getTransponderNameRateGbpsCostReachKmNumSlots(i)).
                    filter(tp -> tp.getSecond() == commonRateForAllLpsThisIpLinkGbps).
                    filter(tp -> tp.getFourth() >= worstCasePathLength).
                    findFirst().orElse(null);

         if (choiceOfTransponderForThisRateAndReach == null) throw new Net2PlanException ("No transponder has been found matching the requirements");
         
         
		final int numLpsEachPathOrThePathOfOnlyOne = (int) Math.ceil(trafficToCarryNoFailureStateGbps / commonRateForAllLpsThisIpLinkGbps);
		final int numSlotsPerLp = choiceOfTransponderForThisRateAndReach.getFifth();

		/* If all required lps created, end */
		/* If not all required lps created, then we need to try other rate. Remove all the lps created if any */

		final List<WLightpathRequest> lpsCreatedAb = new ArrayList<>();
		for (int pathIndex0Or1 : Arrays.asList(0,1))
		{
			if (pathIndex0Or1 == 1 && twoOrOneWdmPathsForThisIpLink.size() == 1) continue;
			final List<Pair<WNode,WNode>> spAbNodeSeq_baIsReversePath = twoOrOneWdmPathsForThisIpLink.get(pathIndex0Or1);
			
			for (int numLp = 0; numLp < numLpsEachPathOrThePathOfOnlyOne ; numLp ++)
			{
				/* Assignment with both bidi-directions using the same slots */
				Optional<Pair<List<Pair<WFiber,WFiber>> , SortedSet<Integer>>> assignment = this.osm.spectrumAssignment_firstFitForAdjacenciesBidi (spAbNodeSeq_baIsReversePath, numSlotsPerLp , new TreeSet<> ());
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
				this.osm.allocateOccupation(lpAb, seqFibersAb, assignment.get().getSecond());
				this.osm.allocateOccupation(lpBa, seqFibersBa, assignment.get().getSecond());
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

	
	
}
