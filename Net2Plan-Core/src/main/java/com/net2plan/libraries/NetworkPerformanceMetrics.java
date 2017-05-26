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

import cern.colt.function.tdouble.DoubleFunction;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.jet.math.tdouble.DoubleFunctions;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Constants;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.IntUtils;

import java.util.*;

/**
 * <p>Class providing static methods to compute several performance
 * metrics using classical formulae (i.e. Erlang-B or Kaufman-Roberts recursion
 * for call blocking probability...).</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class NetworkPerformanceMetrics
{
	private NetworkPerformanceMetrics() { }
	
	/**
	 * <p>Checks the current network state and, maybe, returns some warnings about 
	 * link oversubscription, demand blocking, and so on.</p>
	 * 
	 * <p><b>Important</b>: {@code NetPlan} objects are always valid and consistent from a technology-agnostic point of view</p>
	 *
	 * @param netPlan Input network design
	 * @param net2planParameters A key-value map with {@code Net2Plan}-wide configuration options
	 * @return List of warnings (empty if none)
	 */
	public static List<String> checkNetworkState(NetPlan netPlan, Map<String, String> net2planParameters)
	{
		final double PRECISIONFACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));

		List<String> warnings = new LinkedList<String>();

		if (!netPlan.hasNodes()) warnings.add("Node set is not defined");

		boolean isMultiLayer = netPlan.isMultilayer();
		for (NetworkLayer layer : netPlan.getNetworkLayers())
		{
			String layerName = layer.getName ();
			String thisLayerMsg = "";
			if (isMultiLayer)
			{
				thisLayerMsg = String.format(" at layer of index %d and id %d", layer.getIndex () , layer.getId ());
				if (!layerName.isEmpty()) thisLayerMsg += " (" + layerName + ")";
			}
			
			if (!netPlan.hasLinks(layer))
			{
				warnings.add("Link set is not defined" + thisLayerMsg);
			}
			else
			{
				DoubleMatrix1D vector_ue = netPlan.getVectorLinkCapacity(layer);
				double max_u_e = vector_ue.size () == 0? 0 : vector_ue.getMaxLocation() [0];
				if (max_u_e < PRECISIONFACTOR) warnings.add("All the links have zero capacity" + thisLayerMsg);

				Collection<Link> links_thisLayer = netPlan.getLinks(layer);
				DoubleMatrix1D rho_e = netPlan.getVectorLinkUtilization(layer);
				boolean someLinksOvercongested = false;
				boolean someLinksCongested = false;
				int numCoupledLinks = 0;
				for (Link link : links_thisLayer)
				{
					if (link.isCoupled()) numCoupledLinks++;
					
					double rho_e_thisLink = rho_e.get(link.getIndex());
					if (DoubleUtils.isEqualWithinRelativeTolerance(rho_e_thisLink, 1, PRECISIONFACTOR))
					{
						someLinksCongested = true;
					}
					else if (rho_e_thisLink > 1)
					{
						someLinksOvercongested = true;
					}
				}

				if (someLinksOvercongested) warnings.add("Some links are overcongested (carried traffic > link capacity)" + thisLayerMsg);
				else if (someLinksCongested) warnings.add("Some links have 100% utilization" + thisLayerMsg);
				
				if (numCoupledLinks > 0 && numCoupledLinks < links_thisLayer.size()) warnings.add("Not all links are coupled" + thisLayerMsg);
			}
			
			Collection<Demand> demands_thisLayer = netPlan.getDemands(layer);
			if (!netPlan.hasDemands(layer))
			{
				warnings.add("Traffic demand set is not defined" + thisLayerMsg);
			}
			else
			{
				int numCoupledDemands = 0;
				for (Demand demand : demands_thisLayer) if (demand.isCoupled()) numCoupledDemands++;

				if (netPlan.getVectorDemandBlockedTraffic(layer).getMaxLocation() [0] > PRECISIONFACTOR) warnings.add("Traffic losses (unicast): Not all the traffic is being carried" + thisLayerMsg);

				if (numCoupledDemands > 0 && numCoupledDemands < demands_thisLayer.size()) warnings.add("Some unicast demands are coupled, but not all (this is ok, this just to check that it is intentional)" + thisLayerMsg);
			}

			Collection<MulticastDemand> multicastDemands_thisLayer = netPlan.getMulticastDemands(layer);
			if (!netPlan.hasMulticastDemands(layer))
			{
				warnings.add("Multicast traffic demand set is not defined" + thisLayerMsg);
			}
			else
			{
				int numCoupledDemands = 0;
				for (MulticastDemand demand : multicastDemands_thisLayer) if (demand.isCoupled()) numCoupledDemands++;

				if (netPlan.getVectorMulticastDemandBlockedTraffic(layer).getMaxLocation() [0] > PRECISIONFACTOR) warnings.add("Traffic losses (multicast): Not all the traffic is being carried" + thisLayerMsg);

				if (numCoupledDemands > 0 && numCoupledDemands < multicastDemands_thisLayer.size()) warnings.add("Some unicast demands are coupled, but not all (this is ok, this just to check that it is intentional)" + thisLayerMsg);
			}


			if (netPlan.getRoutingType(layer) == Constants.RoutingType.SOURCE_ROUTING)
			{
				if (!netPlan.hasRoutes(layer))
				{
					warnings.add("Routing is not defined" + thisLayerMsg);
				}
				else
				{
					Collection<Route> routes_thisLayer = netPlan.getRoutes(layer);
					boolean routingCycles = false;
					for (Route route : routes_thisLayer)
					{
						List<Node> sequenceOfNodes = route.getSeqNodes();
						Set<Node> uniqueNodeIds = new HashSet<Node> (sequenceOfNodes);

						if (uniqueNodeIds.size() != sequenceOfNodes.size())
						{
							routingCycles = true;
							break;
						}
					}

					if (routingCycles) warnings.add("Some routes have cycles" + thisLayerMsg);
				}
			}
			else
			{
				if (!netPlan.hasForwardingRules(layer))
				{
					warnings.add("Forwarding rules not defined" + thisLayerMsg);
				}
				else
				{
					boolean hasClosedCycles = false;
					boolean hasOpenCycles = false;
					for (Demand demand : demands_thisLayer)
					{
						Constants.RoutingCycleType routingCycleType_thisDemand = demand.getRoutingCycleType();
						if (routingCycleType_thisDemand == Constants.RoutingCycleType.CLOSED_CYCLES)
						{
							hasClosedCycles = true;
							break;
						}
						else if (routingCycleType_thisDemand == Constants.RoutingCycleType.OPEN_CYCLES)
						{
							hasOpenCycles = true;
						}
					}
					
					if (hasClosedCycles) warnings.add("Forwarding rules not well-defined (traffic enters into a closed cycle) " + thisLayerMsg);
					else if (hasOpenCycles) warnings.add("Forwarding rules non-strictly well-defined (some node is visited more than once)" + thisLayerMsg);
				}
			}
		}
		
		return warnings;
	}

	/**
	 * <p>Returns the probability of call blocking in a {@code M/M/n/n} queue 
	 * system.</p>
	 * 
	 * <p>Reference implementation: {@code [1] S. Qiao, L. Qiao, "A Robust and Efficient Algorithm for Evaluating Erlang B Formula", Technical Report CAS98-03, McMaster University (Canada), October 1998}</p>
	 *
	 * @param numberOfServers Number of servers (i.e. link capacity in integer units). It must be greater or equal than zero
	 * @param load Traffic load (i.e. carried traffic by the link). It must be greater or equal than zero
	 * @return Call blocking probability
	 */
	public static double erlangBLossProbability(int numberOfServers, double load)
	{
		if (numberOfServers < 0) throw new Net2PlanException("Number of server must be greater or equal than 0");
		if (load < 0) throw new Net2PlanException("System load must be greater or equal than 0");
		
		/* Initialize output variable */
		double gradeOfService = 0;
		if (load > 1.0E-10)
		{
			double s = 0;
			for (int i = 1; i <= numberOfServers; i++) s = (1 + s) * (i / load);

			gradeOfService = 1 / (1 + s);
		}
		return gradeOfService;
	}

	/**
	 * <p>Returns the number of servers (i.e. link capacity) to achieve a given grade 
	 * of service (i.e. call blocking probability) under a given load in a 
	 * {@code M/M/n/n} queue system.</p>
	 *
	 * <p>Reference implementation: {@code [1] S. Qiao, L. Qiao, "A Robust and Efficient Algorithm for Evaluating Erlang B Formula", Technical Report CAS98-03, McMaster University (Canada), October 1998}</p>
	 *
	 * @param gradeOfService Grade of service (i.e. call blocking probability). It must be greater or equal than zero
	 * @param load Traffic load (i.e. carried traffic by the link). It must be greater or equal than zero
	 * @return Number of servers
	 */
	public static int inverseErlangB(double gradeOfService, double load)
	{
		if (gradeOfService < 0) throw new Net2PlanException("Grade of service must be greater or equal than 0");
		if (load < 0) throw new Net2PlanException("System load must be greater or equal than 0");
		
		/* Initialize output variable */
		int numberOfServers = 0;
		if (load > 1.0E-10)
		{
			int l = 0;
			int r = (int) Math.ceil(load);
			double fR = erlangBLossProbability(r, load);
			while (fR > gradeOfService)
			{
				l = r;
				r += 32;
				fR = erlangBLossProbability(r, load);
			}
			
			while (r - l > 1)
			{
				int m = (int) Math.ceil((double) (l + r) / 2);
				double fMid = erlangBLossProbability(m, load);
				if (fMid > gradeOfService) l = m;
				else r = m;
			}
			
			numberOfServers = r;
		}
		
		return numberOfServers;
	}

	/**
	 * Computes the Kaufman-Roberts recursion for a multi-rate loss model system.
	 *
	 * @param u_e Link capacity (in integer units). It must be greater or equal than zero
	 * @param h_p Traffic volume vector. Each element is referred to a connection of type {@code p} and must be greater than zero
	 * @param s_p Capacity units occupied in the link by each accepted connection of type {@code p}. Each element must be greater or equal than one
	 * @return Vector of connection blocking probability. Each element is referred to a connection of type {@code p}
	 */
	public static double[] kaufmanRobertsRecursion(int u_e, double[] h_p, int[] s_p)
	{
		if (u_e <= 0) throw new Net2PlanException("Link capacity must be greater or equal than zero");
		
		if (h_p.length != s_p.length) throw new Net2PlanException("Length of offered traffic vector and connection size vector don't match");
		
		double[] A_k = h_p;
		if (DoubleUtils.minValue(A_k) < 0) throw new Net2PlanException("Offered traffic must be greater or equal than zero");

		int[] b_k = s_p;
		int[] b_k_maxMinValues = IntUtils.maxMinValues(b_k);
		if (b_k_maxMinValues[1] < 1) throw new Net2PlanException("Connection size must be greater or equal than one");

		int K = h_p.length;
		int offset = b_k_maxMinValues[0];
		double[] g = new double[1 + u_e + offset];
		Arrays.fill(g, 0);
		g[offset - 1] = 1;
		for (int c = 1; c <= u_e; c++)
		{
			for (int k = 1; k <= K; k++)
				g[offset + c - 1] += A_k[k - 1] * g[offset + c - b_k[k - 1] - 1];
			
			g[offset + c - 1] = g[offset + c - 1] / c;
		}
		double sumG = DoubleUtils.sum(g);
		
		double[] pb_k = new double[K];
		Arrays.fill(pb_k, 0);
		for (int k = 1; k <= K; k++)
			for (int c = u_e - b_k[k - 1] + 1; c <= u_e; c++)
				pb_k[k - 1] += (1 / sumG) * g[offset + c - 1];
		
		return pb_k;
	}
	
	public static double alphaUtility (DoubleMatrix1D vals , final double alphaFactor)
	{
		if (alphaFactor < 0) throw new Net2PlanException ("Fairness alpha factor must be non-negative");
		if (alphaFactor == 0) return vals.zSum();
		if (alphaFactor == 1) return vals.aggregate(DoubleFunctions.plus, DoubleFunctions.log);
		return vals.aggregate (DoubleFunctions.plus, new DoubleFunction () { public double apply (double x) { return (Math.pow(x,1-alphaFactor)/(1-alphaFactor));   } } );
	}
	
	public static double jainFairnessIndex (DoubleMatrix1D vals)
	{
		double sumVals = 0; double sumValsSquare = 0;
		for (int cont = 0; cont < vals.size () ; cont ++) { sumVals += vals.get(cont); sumValsSquare += Math.pow (vals.get(cont) , 2); }
		return Math.pow (sumVals,2) / (vals.size () * sumValsSquare);
	}
	
}
