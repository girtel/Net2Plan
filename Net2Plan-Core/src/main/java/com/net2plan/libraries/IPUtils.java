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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections15.Transformer;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.libraries.GraphUtils.JUNGUtils;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Constants;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Quadruple;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraDistance;
import edu.uci.ics.jung.graph.Graph;

/**
 * Class for destination-based routing (IP-like).
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class IPUtils
{
	/**
	 * Link attribute name for IP weight setting.
	 * 
	 */
	public final static String IP_WEIGHT_ATTRIBUTE_NAME = "ospf-linkWeight";
	
	/**
	 * <p>Returns the cost of a routing scheme according to the carried traffic per 
	 * link and link capacities, assuming the IGP-WO cost model.</p>
	 * 
	 * <p>Let <i>E</i> be a set of links, where <i>u<sub>e</sub></i> is the capacity 
	 * for link <i>e &isin; E</i>, and <i>y<sub>e</sub></i> is the carried traffic 
	 * by link <i>e &isin; E</i>. Then, the total cost of the associated routing, 
	 * denoted as <i>C</i>, is the sum for every link <i>e &isin; E</i> of its 
	 * associated cost <i>c<sub>e</sub></i>, which is computed as follows:</p>
	 * 
	 * <ul>
	 * <li><i>c<sub>e</sub></i> =        <i>y<sub>e</sub></i>                                     , if <i>&rho;<sub>e</sub> &isin; [0, 1/3)</i></li>
	 * <li><i>c<sub>e</sub></i> =    3 * <i>y<sub>e</sub></i> -     (2 / 3) * <i>u<sub>e</sub></i>, if <i>&rho;<sub>e</sub> &isin; [1/3, 2/3)</i></li>
	 * <li><i>c<sub>e</sub></i> =   10 * <i>y<sub>e</sub></i> -    (16 / 3) * <i>u<sub>e</sub></i>, if <i>&rho;<sub>e</sub> &isin; [2/3, 9/10)</i></li>
	 * <li><i>c<sub>e</sub></i> =   70 * <i>y<sub>e</sub></i> -   (178 / 3) * <i>u<sub>e</sub></i>, if <i>&rho;<sub>e</sub> &isin; [9/10, 1)</i></li>
	 * <li><i>c<sub>e</sub></i> =  500 * <i>y<sub>e</sub></i> -  (1468 / 3) * <i>u<sub>e</sub></i>, if <i>&rho;<sub>e</sub> &isin; [1, 11/10)</i></li>
	 * <li><i>c<sub>e</sub></i> = 5000 * <i>y<sub>e</sub></i> - (16318 / 3) * <i>u<sub>e</sub></i>, otherwise</li>
	 * </ul>
	 * 
	 * <p>where <i>&rho;<sub>e</sub></i> is the ratio <i>y<sub>e</sub></i> / <i>u<sub>e</sub></i>.</p>
	 * 
	 * <p>Reference: {@code H. Ümit, "Techniques and Tools for Intra-domain Traffic Engineering," Ph.D. Thesis, Université Catholique de Louvain (Belgium), December 2009}</p>
	 * 
	 * @param y_e Carried traffic per link
	 * @param u_e Capacity per link
	 * @return IGP cost
	 */
	public static double calculateIGPCost(DoubleMatrix1D y_e, DoubleMatrix1D u_e)
	{
		double cost = 0;
		
		for(int cont = 0 ; cont < y_e.size () ; cont ++)
		{
			final double y_e_thisLink = y_e.get(cont);
			final double u_e_thisLink = u_e.get(cont);

			double phi = 0;

			double aux = y_e_thisLink;
			if (aux >= phi) phi = aux;

			aux = 3.0 * y_e_thisLink - (2.0 / 3.0) * u_e_thisLink;
			if (aux >= phi) phi = aux;

			aux = 10.0 * y_e_thisLink - (16.0 / 3.0) * u_e_thisLink;
			if (aux >= phi) phi = aux;

			aux = 70.0 * y_e_thisLink - (178.0 / 3.0) * u_e_thisLink;
			if (aux >= phi) phi = aux;

			aux = 500.0 * y_e_thisLink - (1468.0 / 3.0) * u_e_thisLink;
			if (aux >= phi) phi = aux;

			aux = 5000.0 * y_e_thisLink - (16318.0 / 3.0) * u_e_thisLink;
			if (aux >= phi) phi = aux;

			cost += phi;
		}

		return cost;
	}
	
	private static void checkIPWeight(double linkWeight)
	{
		if (linkWeight < 1) throw new Net2PlanException("Link weights must be greater or equal than 1");
	}
	
	/**
	 * Computes the resulting carried traffic according to a link weight setting 
	 * and OSPF/ECMP routing.
	 * 
	 * @param netPlan Network design
	 * @param optionalLayer Network layer (optional)
	 * @param linkWeightVector Cost per link
	 * @return A {@link com.net2plan.utils.Quadruple Quadruple} composed of {@code f_de} (splitting rules), {@code x_de} (carried traffic for demand 'd' through link 'e'), {@code r_d} (end-to-end carried traffic for demand 'd'), and {@code y_e} (carried traffic by link 'e')
	 */
	public static Quadruple<DoubleMatrix2D, DoubleMatrix2D, DoubleMatrix1D, DoubleMatrix1D> computeCarriedTrafficFromIGPWeights(NetPlan netPlan, DoubleMatrix1D linkWeightVector , NetworkLayer ... optionalLayer)
	{
		final NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter (optionalLayer);
		if (linkWeightVector == null) linkWeightVector = IPUtils.getLinkWeightVector (netPlan, layer);
		
		final List<Node> nodes = netPlan.getNodes();
		final List<Link> links = netPlan.getLinks(layer);
		final List<Demand> demands = netPlan.getDemands(layer);
		final DoubleMatrix1D h_d = netPlan.getVectorDemandOfferedTraffic(layer);
		final DoubleMatrix2D f_te = IPUtils.computeECMPRoutingTableMatrix_fte (nodes, links , linkWeightVector);
		final DoubleMatrix2D f_de = GraphUtils.convert_fte2fde(demands, f_te);
		final Quadruple<DoubleMatrix2D , DoubleMatrix1D, DoubleMatrix1D, List<Constants.RoutingCycleType>> q_noFailure = 
				GraphUtils.convert_fde2xde(nodes, links, demands, h_d, f_de);
		final DoubleMatrix2D x_de = q_noFailure.getFirst();
		final DoubleMatrix1D r_d = q_noFailure.getSecond();
		final DoubleMatrix1D y_e = q_noFailure.getThird();
		return Quadruple.of(f_de, x_de, r_d, y_e);
	}

//	/**
//	 *
//	 * @param nodes List of nodes
//	 * @param links List of links
//	 * @param demands List of demands
//	 * @param linkWeightVector Cost per link vector
//	 * @return Forwarding rule mapping, where key is the demand-outgoing link pair and the value is the splitting ratio
//	 */
//	public static DoubleMatrix2D computeECMPForwardingRules_fde (List<Node> nodes , List<Link> links , List<Demand> demands , DoubleMatrix1D linkWeightVector)
//	{
//		final int N = nodes.size();
//		DoubleMatrix2D splittingRatioMap = DoubleFactory2D.sparse.make (demands.size() , links.size());
//		
//		double[][] costMatrix = new double[N][N];
//		for(int n = 0; n < N; n++)
//		{
//			Arrays.fill(costMatrix[n], Double.MAX_VALUE);
//			costMatrix[n][n] = 0;
//		}
//		Map<Link,Double> linkWeightMap = CollectionUtils.toMap (links , linkWeightVector);
//		Transformer<Link, Double> nev = JUNGUtils.getEdgeWeightTransformer(linkWeightMap);
//		
//		Map<Pair<Node, Node>, Set<Link>> linksPerNodePair = new LinkedHashMap<Pair<Node, Node>, Set<Link>>();
//		for(Link link : links)
//		{
//			Pair<Node, Node> nodePair_thisLink = Pair.of (link.getOriginNode() , link.getDestinationNode());
//			final int a_e = link.getOriginNode().getIndex();
//			final int b_e = link.getDestinationNode().getIndex();
//			costMatrix[a_e][b_e] = Math.min(costMatrix[a_e][b_e], nev.transform(link));
//			
//			Set<Link> links_thisNodePair = linksPerNodePair.get(nodePair_thisLink);
//			if (links_thisNodePair == null)
//			{
//				links_thisNodePair = new LinkedHashSet<Link>();
//				linksPerNodePair.put(nodePair_thisLink, links_thisNodePair);
//			}
//			
//			links_thisNodePair.add(link);
//		}
//
//		for(int k = 0; k < N; k++)
//		{
//			for(int i = 0; i < N; i++)
//			{
//				if (i == k) continue;
//				
//				for(int j = 0; j < N; j++)
//				{
//					if (j == k || j == i) continue;
//					
//					double newValue = costMatrix[i][k] + costMatrix[k][j];
//					if (newValue < costMatrix[i][j]) costMatrix[i][j] = newValue;
//				}
//			}
//		}
//		
//		for(Demand demand : demands)
//		{
//			final int b_d = demand.getEgressNode().getIndex ();
//			for (Node node : nodes)
//			{
//				final int n = node.getIndex ();
//				if (n == b_d) continue;
//
//				double distNodeToEgress = costMatrix[n][b_d];
//				if (distNodeToEgress == Double.MAX_VALUE) continue;
//
//				Set<Link> A_t = new LinkedHashSet<Link>();
//				for (Node intermediateNode : nodes)
//				{
//					if (node.equals (intermediateNode)) continue;
//
//					final int m = intermediateNode.getIndex();
//
//					double distIntermediateToEgress = costMatrix[m][b_d];
//					if (distIntermediateToEgress == Double.MAX_VALUE) continue;
//
//					Collection<Link> linksFromNodeToIntermediate = linksPerNodePair.get(Pair.of(node, intermediateNode));
//					if (linksFromNodeToIntermediate == null) continue;
//
//					for (Link link : linksFromNodeToIntermediate)
//					{
//						double weight_thisLink = linkWeightMap.get(link);
//						checkIPWeight(weight_thisLink);
//
//						if (Math.abs(weight_thisLink - (distNodeToEgress - distIntermediateToEgress)) < 1E-10)
//							A_t.add(link);
//					}
//				}
//
//				int outdegree = A_t.size();
//
//				if (outdegree > 0)
//					for (Link link : A_t)
//						splittingRatioMap.set (demand.getIndex () , link.getIndex () , 1.0 / outdegree);
//			}
//		}
//
//		return splittingRatioMap;
//	}

	/**
	 * Computes the routing table matrix according to an OSPF/ECMP scheme. 
	 * Links with a weight of Double.MAX_VALUE are not considered
	 * For each destination node <i>t</i>, and each link <i>e</i>, {@code f_te[t][e]}
	 * sets the fraction of the traffic targeted to node <i>t</i> that arrives
	 * (or is generated in) node <i>a(e)</i> (the initial node of link <i>e</i>),
	 * that is forwarded through link <i>e</i>. It must hold that for every
	 * node <i>n</i> different of <i>t</i>, the sum of the fractions
	 * <i>f<sub>te</sub></i> along its outgoing links must be 1. For every
	 * destination <i>t</i>, <i>f<sub>te</sub></i> = 0 for all the links <i>e</i>
	 * that are outgoing links of <i>t</i>.
	 * @param nodes List of nodes
	 * @param links List of links
	 * @param linkWeightVector Cost per link vector
	 * @return Destination-based routing in the form <i>f<sub>te</sub></i> (fractions of traffic in a node, that is forwarded through each of its output links to node {@code t})
	 */
	public static DoubleMatrix2D computeECMPRoutingTableMatrix_fte (List<Node> nodes, List<Link> links, DoubleMatrix1D linkWeightVector)
	{
		final int N = nodes.size();
		final int E = links.size();

		final Map<Link,Double> linkWeightMap = CollectionUtils.toMap(links, linkWeightVector);
		final Transformer<Link, Double> nev = JUNGUtils.getEdgeWeightTransformer(linkWeightMap);
		final List<Link> linksToConsider = links.stream().filter(e->linkWeightMap.get(e) != Double.MAX_VALUE).collect(Collectors.toList());
		final Graph<Node, Link> graph = JUNGUtils.getGraphFromLinkMap(nodes, linksToConsider);
		final DijkstraDistance<Node,Link> shortestDistanceMatrix = new DijkstraDistance<Node,Link> (graph, nev); 
		
		final DoubleMatrix2D f_te = DoubleFactory2D.sparse.make(N,E);
		
		final Map<Pair<Node, Node>, Pair<Set<Link>,Double>> linksPerNodeSameMinimumCost = new HashMap<>();
		for(Link link : linksToConsider)
		{
			final double newLinkCost = linkWeightMap.get(link);
			final Pair<Node,Node> nodePair_thisLink = Pair.of(link.getOriginNode() , link.getDestinationNode());
			final Pair<Set<Link>,Double> links_thisNodePair = linksPerNodeSameMinimumCost.get(nodePair_thisLink);
			if (links_thisNodePair == null)
			{
				final Set<Link> setForLinkThisNodePair = new HashSet<Link>();
				setForLinkThisNodePair.add(link);
				linksPerNodeSameMinimumCost.put(nodePair_thisLink, Pair.of(setForLinkThisNodePair , newLinkCost));
			}
			else
			{
				final double previousLinksCost = links_thisNodePair.getSecond();
				if (newLinkCost < previousLinksCost) { links_thisNodePair.getFirst().clear(); links_thisNodePair.getFirst().add(link); links_thisNodePair.setSecond(newLinkCost); }
				else if (newLinkCost == previousLinksCost) { links_thisNodePair.getFirst().add(link); }
				else if (newLinkCost > previousLinksCost) { continue; }
			}
		}

		for (Node egressNode : nodes)
		{
			final int t = egressNode.getIndex();
			for (Node sourceNode : nodes)
			{
				final int n = sourceNode.getIndex ();
				if (n == t) continue;
				final Number shortestPathDistance_ij = shortestDistanceMatrix.getDistance(sourceNode, egressNode);
				if (shortestPathDistance_ij == null) continue; // egress not reachable
				final Set<Link> minCostLinks = new HashSet<> (); 
				for (Node intermediateNode : graph.getNeighbors(sourceNode))
				{
					if (nodes.equals (intermediateNode)) continue;
					final Pair<Set<Link>,Double> nodePairLinks = linksPerNodeSameMinimumCost.get(Pair.of(sourceNode, intermediateNode));
					if (nodePairLinks == null) continue;
					if (nodePairLinks.getFirst().isEmpty()) continue;
					final Number costFromIntermediateToEnd = shortestDistanceMatrix.getDistance(intermediateNode, egressNode);
					if (costFromIntermediateToEnd == null) continue;
					final double costThroghThisIntermediate = nodePairLinks.getSecond() + costFromIntermediateToEnd.doubleValue();
					if (Math.abs(shortestPathDistance_ij.doubleValue() - costThroghThisIntermediate) < 1E-10)
						minCostLinks.addAll(nodePairLinks.getFirst());
				}
				final int outdegree = minCostLinks.size();
				if (outdegree > 0)
					for (Link link : minCostLinks)
						f_te.set (t , link.getIndex () , 1.0 / outdegree);
			}
		}
		return f_te;
	}

	/**
	 * Computes the forwarding rules according to an OSPF/ECMP scheme, for the given demands. 
	 * Links with a weight of Double.MAX_VALUE are not considered.
	 * @param nodes List of nodes
	 * @param links List of links
	 * @param demands Demands for which the ECMP forwarding rules will be computed 
	 * @param linkWeightVector Cost per link vector. Links with weight Double.MAX_VALUE are not considered
	 * @return Destination-based routing in the form <i>f<sub>te</sub></i> (fractions of traffic in a node, that is forwarded through each of its output links to node {@code t})
	 */
	public static Triple<List<Demand>,List<Link>,List<Double>> computeECMPForwardinRules (List<Node> nodes, List<Link> links, List<Demand> demands , DoubleMatrix1D linkWeightVector)
	{
		final Map<Link,Double> linkWeightMap = CollectionUtils.toMap(links, linkWeightVector);
		final Transformer<Link, Double> nev = JUNGUtils.getEdgeWeightTransformer(linkWeightMap);
		final List<Link> linksToConsider = links.stream().filter(e->linkWeightMap.get(e) != Double.MAX_VALUE).collect(Collectors.toList());
		final Graph<Node, Link> graph = JUNGUtils.getGraphFromLinkMap(nodes, linksToConsider);
		final DijkstraDistance<Node,Link> shortestDistanceMatrix = new DijkstraDistance<Node,Link> (graph, nev); 
		final List<Demand> frDemands = new ArrayList<> ();
		final List<Link> frLinks = new ArrayList<> ();
		final List<Double> frSplits = new ArrayList<> ();
		
		final Map<Pair<Node, Node>, Pair<Set<Link>,Double>> linksPerNodeSameMinimumCost = new HashMap<>();
		for(Link link : linksToConsider)
		{
			final double newLinkCost = linkWeightMap.get(link);
			final Pair<Node,Node> nodePair_thisLink = Pair.of(link.getOriginNode() , link.getDestinationNode());
			final Pair<Set<Link>,Double> links_thisNodePair = linksPerNodeSameMinimumCost.get(nodePair_thisLink);
			if (links_thisNodePair == null)
			{
				final Set<Link> setForLinkThisNodePair = new HashSet<Link>();
				setForLinkThisNodePair.add(link);
				linksPerNodeSameMinimumCost.put(nodePair_thisLink, Pair.of(setForLinkThisNodePair , newLinkCost));
			}
			else
			{
				final double previousLinksCost = links_thisNodePair.getSecond();
				if (newLinkCost < previousLinksCost) { links_thisNodePair.getFirst().clear(); links_thisNodePair.getFirst().add(link); links_thisNodePair.setSecond(newLinkCost); }
				else if (newLinkCost == previousLinksCost) { links_thisNodePair.getFirst().add(link); }
				else if (newLinkCost > previousLinksCost) { continue; }
			}
		}

		for (Demand demand : demands)
		{
			final Node egressNode = demand.getEgressNode();
			for (Node sourceNode : nodes)
			{
				if (egressNode == sourceNode) continue;
				final Number shortestPathDistance_ij = shortestDistanceMatrix.getDistance(sourceNode, egressNode);
				if (shortestPathDistance_ij == null) continue; // egress not reachable
				final Set<Link> minCostLinks = new HashSet<> (); 
				for (Node intermediateNode : graph.getNeighbors(sourceNode))
				{
					if (nodes.equals (intermediateNode)) continue;
					final Pair<Set<Link>,Double> nodePairLinks = linksPerNodeSameMinimumCost.get(Pair.of(sourceNode, intermediateNode));
					if (nodePairLinks == null) continue;
					if (nodePairLinks.getFirst().isEmpty()) continue;
					final double costThroghThisIntermediate = nodePairLinks.getSecond() + shortestDistanceMatrix.getDistance(intermediateNode, egressNode).doubleValue();
					if (Math.abs(shortestPathDistance_ij.doubleValue() - costThroghThisIntermediate) < 1E-10)
						minCostLinks.addAll(nodePairLinks.getFirst());
				}
				final int outdegree = minCostLinks.size();
				for (Link link : minCostLinks)
				{
					frDemands.add(demand);
					frLinks.add(link);
					frSplits.add(1.0 / outdegree);
				}
			}
		}
		return Triple.of(frDemands, frLinks, frSplits);
	}

	
	/**
	 * Computes the routing table matrix according to an OSPF/ECMP scheme. For
	 * each destination node <i>t</i>, and each link <i>e</i>, {@code f_te[t][e]}
	 * sets the fraction of the traffic targeted to node <i>t</i> that arrives
	 * (or is generated in) node <i>a(e)</i> (the initial node of link <i>e</i>),
	 * that is forwarded through link <i>e</i>. It must hold that for every
	 * node <i>n</i> different of <i>t</i>, the sum of the fractions
	 * <i>f<sub>te</sub></i> along its outgoing links must be 1. For every
	 * destination <i>t</i>, <i>f<sub>te</sub></i> = 0 for all the links <i>e</i>
	 * that are outgoing links of <i>t</i>.
	 *
	 * @param nodeIds Set of node identifiers. It is mandatory that can be iterated in ascending order
	 * @param linkMap Map of links, where the key is the unique link identifier and the value is a {@link com.net2plan.utils.Pair Pair} representing the origin node and the destination node of the link, respectively. It is mandatory that can be iterated in ascending order
	 * @param linkWeightMap Cost per link, where the key is the link identifier and the value is the link weight (null means each link will have a weight equal to '1'). No special iteration-order (i.e. ascending) is required
	 * @return Destination-based routing in the form <i>f<sub>te</sub></i> (fractions of traffic in a node, that is forwarded through each of its output links to node {@code t})
	 */
//	public static DoubleMatrix2D computeECMPRoutingTableMatrix(Set<Long> nodeIds, Map<Long, Pair<Long, Long>> linkMap, Map<Long, Double> linkWeightMap)
//	{
//		int N = nodeIds.size();
//		int E = linkMap.size();
//		DoubleMatrix2D f_te = DoubleFactory2D.sparse.make(N, E);
//		
//		Map<Long, Integer> nodeId2LinearIndexMap = CollectionUtils.convertId2LinearIndexMap(nodeIds);
//		double[][] costMatrix = new double[N][N];
//		for(int n = 0; n < N; n++)
//		{
//			Arrays.fill(costMatrix[n], Double.MAX_VALUE);
//			costMatrix[n][n] = 0;
//		}
//		
//		Transformer<Long, Double> nev = JUNGUtils.getEdgeWeightTransformer(linkWeightMap);
//		
//		Map<Pair<Long, Long>, Set<Long>> linkIdsPerNodePair = new LinkedHashMap<Pair<Long, Long>, Set<Long>>();
//		for(Entry<Long, Pair<Long, Long>> entry : linkMap.entrySet())
//		{
//			long linkId = entry.getKey();
//			Pair<Long, Long> nodePair_thisLink = entry.getValue();
//			int a_e = nodeId2LinearIndexMap.get(nodePair_thisLink.getFirst());
//			int b_e = nodeId2LinearIndexMap.get(nodePair_thisLink.getSecond());
//			costMatrix[a_e][b_e] = Math.min(costMatrix[a_e][b_e], nev.transform(linkId));
//			
//			Set<Long> linkIds_thisNodePair = linkIdsPerNodePair.get(nodePair_thisLink);
//			if (linkIds_thisNodePair == null)
//			{
//				linkIds_thisNodePair = new LinkedHashSet<Long>();
//				linkIdsPerNodePair.put(nodePair_thisLink, linkIds_thisNodePair);
//			}
//			
//			linkIds_thisNodePair.add(linkId);
//		}
//
//		for(int k = 0; k < N; k++)
//		{
//			for(int i = 0; i < N; i++)
//			{
//				if (i == k) continue;
//				
//				for(int j = 0; j < N; j++)
//				{
//					if (j == k || j == i) continue;
//					
//					double newValue = costMatrix[i][k] + costMatrix[k][j];
//					if (newValue < costMatrix[i][j]) costMatrix[i][j] = newValue;
//				}
//			}
//		}
//		
//		Map<Long, Integer> linkId2LinearIndex = CollectionUtils.convertId2LinearIndexMap(linkMap.keySet());
//		for (long egressNodeId : nodeIds)
//		{
//			int t = nodeId2LinearIndexMap.get(egressNodeId);
//			for (long nodeId : nodeIds)
//			{
//				int n = nodeId2LinearIndexMap.get(nodeId);
//				if (n == t) continue;
//
//				double distNodeToEgress = costMatrix[n][t];
//				if (distNodeToEgress == Double.MAX_VALUE) continue;
//
//				Set<Long> A_t = new LinkedHashSet<Long>();
//				for (long intermediateNodeId : nodeIds)
//				{
//					if (nodeId == intermediateNodeId) continue;
//
//					int m = nodeId2LinearIndexMap.get(intermediateNodeId);
//
//					double distIntermediateToEgress = costMatrix[m][t];
//					if (distIntermediateToEgress == Double.MAX_VALUE) continue;
//
//					Collection<Long> linksFromNodeToIntermediate = linkIdsPerNodePair.get(Pair.of(nodeId, intermediateNodeId));
//					if (linksFromNodeToIntermediate == null) continue;
//
//					for (long linkId : linksFromNodeToIntermediate)
//					{
//						double weight_thisLink = linkWeightMap.get(linkId);
//						checkIPWeight(weight_thisLink);
//
//						if (Math.abs(weight_thisLink - (distNodeToEgress - distIntermediateToEgress)) < 1E-10)
//							A_t.add(linkId);
//					}
//				}
//
//				int outdegree = A_t.size();
//
//				if (outdegree > 0)
//				{
//					for (long linkId : A_t)
//					{
//						int e = linkId2LinearIndex.get(linkId);
//						f_te.setQuick(t, e, 1.0 / outdegree);
//					}
//				}
//			}
//		}
//
//		return f_te;
//	}

	/**
	 * Returns the weight associated to a given link.
	 * 
	 * @param link Link
	 * @return Link weight (or {@code Double.MAX_VALUE} if link is down)
	 */
	public static double getLinkWeight(Link link)
	{
		if (link.isDown()) return Double.MAX_VALUE;
		double linkWeight = 1;
		String str_linkWeight = link.getAttribute(IP_WEIGHT_ATTRIBUTE_NAME);
		try	{ linkWeight = Double.parseDouble(str_linkWeight); }
		catch(Throwable e) { }
		
		checkIPWeight(linkWeight);
		
		return linkWeight;
	}
	
	/**
	 * Obtains the vector of ink weights from a given a network design.
	 *
	 * @param netPlan Network design
	 * @param optionalLayer Network layer (optional)
	 * @return Link weight vector
	 */
	public static DoubleMatrix1D getLinkWeightVector (NetPlan netPlan, NetworkLayer ... optionalLayer)
	{
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayer);
		DoubleMatrix1D linkWeights = DoubleFactory1D.dense.make (netPlan.getNumberOfLinks(layer));
		for(Link link : netPlan.getLinks(layer)) linkWeights.set (link.getIndex(), getLinkWeight(link));
	
		return linkWeights;
	}
	
	
//	/**
//	 * Obtains a destination-based routing from a given network design.
//	 *
//	 * @param netPlan Network design
//	 * @return A destination-based routing in the form of fractions <i>f<sub>te</sub></i> (fraction of the traffic targeted to node <i>t</i> that arrives (or is generated in) node <i>a</i>(<i>e</i>) (the initial node of link <i>e</i>), that is forwarded through link <i>e</i>)
//	 * @since 0.3.1
//	 */
//	public static DoubleMatrix2D getRoutingTableMatrix(NetPlan netPlan, NetworkLayer ... optionalLayer)
//	{
//		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayer);
//		if (netPlan.getRoutingType(layer) == RoutingType.HOP_BY_HOP_ROUTING)
//			return netPlan.getMatrixGraphUtils.conv
//		Set<Long> nodeIds = netPlan.getNodeIds();
//		Map<Long, Pair<Long, Long>> linkMap = netPlan.getLinkMap();
//		Map<Long, Pair<Long, Long>> demandMap = netPlan.getDemandMap();
//		Map<Long, Long> d_p = netPlan.getRouteDemandMap();
//		Map<Long, Double> x_p = netPlan.getRouteCarriedTrafficMap();
//		Map<Long, List<Long>> seqLinks = netPlan.getRouteAllSequenceOfLinks();
//
//		convert_xp2fte(List<Node> nodes, List<Link> linkMap, List<Demand> demandMap, List<Route> routes)
//		
//		return GraphUtils.convert_xp2fte(nodeIds, linkMap, demandMap, d_p, x_p, seqLinks);
//	}
//
	/**
	 * Outputs a given set of routing tables to a {@code String}. For debugging purposes.
	 *
	 * @param nodes List of nodes
	 * @param links List of links
	 * @param f_te Destination-based routing in the form of fractions <i>f<sub>te</sub></i> (fraction of the traffic targeted to node <i>t</i> that arrives (or is generated in) node <i>a</i>(<i>e</i>) (the initial node of link <i>e</i>), that is forwarded through link <i>e</i>)
	 * @return A {@code String} from the given routing tables
	 */
	public static String routingTableMatrixToString(List<Node> nodes , List<Link> links , DoubleMatrix2D f_te)
	{
		StringBuilder out = new StringBuilder();
		final int N = nodes.size();
		int i = 0;
		for (Node node : nodes)
		{
			out.append(String.format("Routing table for n%d%n", node.getId ()));

			Set<Link> outgoingLinks_thisNode = node.getOutgoingLinksAllLayers();

			boolean thereAreRoutes = false;

			if (!outgoingLinks_thisNode.isEmpty())
			{
				for (Node egressNode : nodes)
				{
					if (node.equals(egressNode)) continue;

					final int t = egressNode.getIndex ();
					thereAreRoutes = false;
					
					StringBuilder out_thisEgressNode = new StringBuilder();
					out_thisEgressNode.append(String.format("dst: n%d, gw:", egressNode.getId ()));
					for (Link link : outgoingLinks_thisNode)
					{
						final int e = link.getIndex ();
						double trafficValue = f_te.get(t,e);
						if (trafficValue < 0) continue;
						
						out_thisEgressNode.append(String.format(" [e%d (n%d): %f]", link.getId (), links.get(link.getIndex()).getDestinationNode().getId (), trafficValue));
						thereAreRoutes = true;
					}

					out_thisEgressNode.append(String.format("%n"));
					if (thereAreRoutes) out.append(out_thisEgressNode);
				}
			}

			if (!thereAreRoutes) out.append("Empty");
			
			i++;
			if (i < N)
			{
				out.append(String.format("%n"));
			}
		}

		return out.toString();
	}
	
	/**
	 * Sets the OSPF/ECMP forwarding rules in the given design, according to the 
	 * given IGP weight setting. Any previous routing information (either source 
	 * routing or hop-by-hop routing) will be removed. This method calls 
	 * sequentually to {@code computeECMPForwardingRules} and {@code setForwardingRules} methods.
	 * 
	 * @param netPlan Network design
	 * @param optionalLayer Network layer (optional)
	 * @param linkWeightMap Cost per link vector
	 */
	public static void setECMPForwardingRulesFromLinkWeights(NetPlan netPlan, DoubleMatrix1D linkWeightMap , NetworkLayer ... optionalLayer)
	{
		final NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayer);
		final Quadruple<DoubleMatrix2D, DoubleMatrix2D, DoubleMatrix1D, DoubleMatrix1D> q = 
				computeCarriedTrafficFromIGPWeights(netPlan, linkWeightMap , layer);
		final DoubleMatrix2D f_de = q.getFirst ();
		netPlan.setForwardingRules(f_de , layer);
	}
	
	/**
	 * Sets the weight associated to the link.
	 * 
	 * @param link Link
	 * @param linkWeight IGP weight associated to the link (must be greater or equal than one)
	 */
	public static void setLinkWeight(Link link , double linkWeight)
	{
		checkIPWeight(linkWeight);
		link.setAttribute (IP_WEIGHT_ATTRIBUTE_NAME, Double.toString(linkWeight));
	}
	
	/**
	 * Sets the weight associated to every link.
	 *
	 * @param netPlan Network design
	 * @param linkWeight IGP weight associated to each link (must be greater or equal than one)
	 * @param optionalLayer Network layer (optional)
	 */
	public static void setLinkWeights(NetPlan netPlan, double linkWeight , NetworkLayer ... optionalLayer)
	{
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayer);
		checkIPWeight(linkWeight);
		for (Link link : netPlan.getLinks (layer))
			link.setAttribute (IP_WEIGHT_ATTRIBUTE_NAME, Double.toString(linkWeight));
	}

	/**
	 * Sets the weight associated to each link.
	 *
	 * @param netPlan Network design
	 * @param optionalLayer Network layer (optional
	 * @param linkWeightVector IGP weight per link (must be greater or equal than one)
	 */
	public static void setLinkWeights(NetPlan netPlan, DoubleMatrix1D linkWeightVector , NetworkLayer ... optionalLayer)
	{
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayer);
		if (linkWeightVector.size () != netPlan.getLinks (layer).size ()) throw new Net2PlanException ("Wrong array size");
		for (int cont = 0 ; cont < linkWeightVector.size () ; cont ++)
			checkIPWeight(linkWeightVector.get(cont));
		for (int cont = 0 ; cont < linkWeightVector.size () ; cont ++)
		{
			Link link = netPlan.getLink (cont , layer);
			link.setAttribute (IP_WEIGHT_ATTRIBUTE_NAME, Double.toString(linkWeightVector.get (cont)));
		}
	}
}
