/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/

package com.net2plan.libraries;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;

import java.util.List;

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
			for (Link e : r.getSeqLinks()) average += r.getCarriedTraffic() * ((linkCostMap == null)? 1 : linkCostMap.get(e.getIndex()));
		}
		return R_d == 0? 0 : average / R_d;
	}
	
	/**
	 * <p>Returns the fraction of the total carried traffic in the given layer that has at least one backup path defined. 
	 * If a layer is not defined, the defaul layer is used.</p>
	 * @param netPlan Current network design
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Traffic protection degree
	 */
	public static double getTrafficProtectionDegree(NetPlan netPlan, NetworkLayer ... optionalLayerParameter)
	{
		if (optionalLayerParameter.length >= 2) throw new Net2PlanException ("None or one layer parameter can be supplied");
		NetworkLayer layer = (optionalLayerParameter.length == 1)? optionalLayerParameter [0] : netPlan.getNetworkLayerDefault();
		layer.checkAttachedToNetPlanObject(netPlan);
		if (netPlan.getRoutingType(layer) == RoutingType.HOP_BY_HOP_ROUTING) return 0;
		
		double totalCarriedTraffic = 0;
		double totalCarriedAndProtectedTraffic = 0;

		for (Route route : netPlan.getRoutes(layer))
		{
			totalCarriedTraffic += route.getCarriedTraffic();
			if (route.hasBackupRoutes())
				totalCarriedAndProtectedTraffic += route.getCarriedTraffic();
		}
		return totalCarriedTraffic == 0? 0 : totalCarriedAndProtectedTraffic / totalCarriedTraffic;
	}
}
