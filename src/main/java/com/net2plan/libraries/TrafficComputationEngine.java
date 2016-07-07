/*******************************************************************************
 * Copyright (c) 2016 Pablo Pavon-Marino.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Pablo Pavon-Marino - Jose-Luis Izquierdo-Zaragoza, up to version 0.3.1
 *     Pablo Pavon-Marino - from version 0.4.0 onwards
 ******************************************************************************/

package com.net2plan.libraries;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cern.colt.matrix.tdouble.DoubleMatrix1D;

import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.ProtectionSegment;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

/**
 * Class implementing different traffic metrics.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class TrafficComputationEngine
{
	private TrafficComputationEngine() { }
	
	/**
	 * Returns the average number of hops (number of links) and km (summing all links) of the multicast trees in the network
	 * 
	 *@param trees List of Multicast Trees
	 * @return First: average number of hops, second average length in km
	 */
	public static Pair<Double,Double> getAverageHopsAndLengthOfMulticastTrees(List<MulticastTree> trees)
	{
		if (trees.isEmpty()) return Pair.of (0.0,0.0);
		double accumKm = 0;
		double accumHops = 0;
		for (MulticastTree tree : trees)
		{
			accumKm += tree.getTreeTotalLengthInKm();
			accumHops += tree.getTreeNumberOfLinks();
		}
		return Pair.of (accumHops / trees.size() , accumKm / trees.size());
	}

//	/**
//	 * Returns the bottleneck (lowest) capacity among the set of traversed links and protection 
//	 * segments. This method takes into account multiple passes through the same link 
//	 * or protection segment.
//	 * 
//	 * @param netPlan Current network design
//	 * @param layerId Layer identifier
//	 * @param path Sequence of links and protection segments (not checked for continuity)
//	 * @return Bottleneck capacity
//	 * @since 0.3.0
//	 */
//	public static double getPathBottleneckCapacity(NetPlan netPlan, List<Link> path)
//	{
//		if (path.isEmpty()) throw new Net2PlanException("Path is empty");
//		
//		double bottleneckCapacity = Double.MAX_VALUE;
//		for (Link e : path)
//		{
//			if (e instanceof ProtectionSegment)
//				bottleneckCapacity = Math.min(bottleneckCapacity, e.capacity - netPlan.getProtectionSegmentSpareCapacity(layerId, -1 - linkId) / linkPasses.get(linkId));
//		}
//		
//		Map<Link, Integer> linkPasses = CollectionUtils.count(path);
//		for(Link linkId : new LinkedHashSet<Long>(linkPasses.keySet()))
//		{
//			if (linkId >= 0)
//				bottleneckCapacity = Math.min(bottleneckCapacity, netPlan.getLinkSpareCapacity(layerId, linkId) / linkPasses.get(linkId));
//			else
//				bottleneckCapacity = Math.min(bottleneckCapacity, netPlan.getProtectionSegmentSpareCapacity(layerId, -1 - linkId) / linkPasses.get(linkId));
//		}
//		
//		return bottleneckCapacity;
//	}
	
	/**
	 * Obtains the average route length among the current routes according to
	 * certain link cost metric. Route lengths are weighted by their carried traffic.
	 * 
	 * @param routes List of routes
	 * @param linkCostMap Cost metric per link
	 * @return Average route length
	 */
	public static double getRouteAverageLength(List<Route> routes , DoubleMatrix1D linkCostMap)
	{
		double R_d = 0;
		double average = 0;
		for (Route r : routes)
		{
			R_d += r.getCarriedTraffic();
			for (Link e : r.getSeqLinksRealPath()) average += r.getCarriedTraffic() * ((linkCostMap == null)? 1 : linkCostMap.get(e.getIndex()));
		}
		return R_d == 0? 0 : average / R_d;
	}
	
	/**
	 * <p>Returns the statistics for protection degree carried traffic. Returned
	 * values are the following:</p>
	 * 
	 * <ul>
	 * <li>Unprotected: Percentage of carried traffic which is not backed up by protection segments</li>
	 * <li>Complete and dedicated protection: Percentage of carried traffic which
	 * has one or more dedicated protection segments covering the whole route with enough bandwidth</li>
	 * <li>Partial and/or shared protection: For cases not covered by the above definitions</li>
	 * </ul>
	 * 
	 * @param netPlan Current network design
	 * @param optionalLayerParameter Network laer (optional)
	 * @return Traffic protection degree
	 */
	public static Triple<Double, Double, Double> getTrafficProtectionDegree(NetPlan netPlan, NetworkLayer ... optionalLayerParameter)
	{
		if (optionalLayerParameter.length >= 2) throw new Net2PlanException ("None or one layer parameter can be supplied");
		NetworkLayer layer = (optionalLayerParameter.length == 1)? optionalLayerParameter [0] : netPlan.getNetworkLayerDefault();
		layer.checkAttachedToNetPlanObject(netPlan);
		if (netPlan.getRoutingType(layer) == RoutingType.HOP_BY_HOP_ROUTING) return Triple.of (1.0,0.0,0.0);
		
		double R_d = netPlan.getVectorDemandCarriedTraffic(layer).zSum();
		double percentageUnprotected = 0;
		double percentageDedicated = 0;
		double percentageShared = 0;

		for (Route route : netPlan.getRoutes(layer))
		{
			final double x_p = route.getCarriedTraffic();
			if (route.getPotentialBackupProtectionSegments().isEmpty()) { percentageUnprotected += x_p; continue; }

			/* Complete and dedicated: we can go from origin to destination, traversing only non currently used protection segments, dedicated only to me, and with enough capacity */
//			Map<Long,Pair<Long,Long>> notCurrentlyUsedProtSegmentMap = new HashMap<Long,Pair<Long,Long>> ();
//			Map<Long,Double> linkSpareCapacityMap = new HashMap<Long,Double> ();
//			for (ProtectionSegment s : route.getPotentialBackupProtectionSegments())
//			{
//				if (route.getCurrentlyTraversedProtectionSegments().contains (s)) continue;
//				if (s.getAssociatedRoutesToWhichIsBackup().size() > 1) continue;
//				notCurrentlyUsedProtSegmentMap.put (s.getId () , Pair.of (s.getOriginNode().getId () , s.getDestinationNode().getId ()));
//				linkSpareCapacityMap.put (s.getId () , s.getReservedCapacityForProtection() - s.getCarriedTraffic());
//			}
//			List<Long> backupSeqProtSegments = GraphUtils.getCapacitatedShortestPath(netPlan.getNodeIds () , notCurrentlyUsedProtSegmentMap , route.getIngressNode().getId () , route.getEgressNode().getId () , null , linkSpareCapacityMap , route.getCarriedTraffic());
			List<Link> notCurrentlyUsedProtSegments = new LinkedList<Link> ();
			Map<Link,Double> linkSpareCapacityMap = new HashMap<Link,Double> ();
			for (ProtectionSegment s : route.getPotentialBackupProtectionSegments())
			{
				if (route.getCurrentlyTraversedProtectionSegments().contains (s)) continue;
				if (s.getAssociatedRoutesToWhichIsBackup().size() > 1) continue;
				notCurrentlyUsedProtSegments.add (s);
				linkSpareCapacityMap.put (s , s.getReservedCapacityForProtection() - s.getCarriedTraffic());
			}
			List<Link> backupSeqProtSegments = GraphUtils.getCapacitatedShortestPath(netPlan.getNodes () , notCurrentlyUsedProtSegments , route.getIngressNode() , route.getEgressNode() , null , linkSpareCapacityMap , route.getCarriedTraffic());
			
			if (backupSeqProtSegments.isEmpty()) percentageShared += x_p; else  percentageDedicated += x_p;
		}

		percentageUnprotected = R_d == 0 ? 0 : 100 * percentageUnprotected / R_d;
		percentageDedicated = R_d == 0 ? 0 : 100 * percentageDedicated / R_d;
		percentageShared = R_d == 0 ? 0 : 100 * percentageShared / R_d;

		return Triple.of(percentageUnprotected, percentageDedicated, percentageShared);
	}
}
