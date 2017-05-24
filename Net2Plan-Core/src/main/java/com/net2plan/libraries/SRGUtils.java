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

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.google.common.collect.Sets;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides a set of static methods which can be useful when dealing with network resilience.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
@SuppressWarnings("unchecked")
public class SRGUtils
{
	private SRGUtils() { }
	
	/**
	 * Type of shared-risk model.
	 *
	 */
	public enum SharedRiskModel
	{
		/**
		 * Defines a SRG per each node. Links are not associated to any SRG.
		 */
		PER_NODE,
		
		/**
		 * Defines a SRG per each unidirectional link. Nodes are not associated to any SRG.
		 */
		PER_LINK,
		
		/**
		 * Defines a SRG (one per direction) containing all links between a node pair. Nodes are not associated to any SRG.
		 */
		PER_DIRECTIONAL_LINK_BUNDLE,
		
		/**
		 * Defines a SRG containing all links between a node pair. Nodes are not associated to any SRG.
		 */
		PER_BIDIRECTIONAL_LINK_BUNDLE
	};
	
	
	/**
	 * Computes the probability to find the network on each failure state.
	 * 
	 * @param A_sf SRGs failing (columns) per each failure state (rows)
	 * @param A_f Availability value per SRG
	 * @return Probability to find the network in each failure state
	 */
	public static DoubleMatrix1D computeStateProbabilities(DoubleMatrix2D A_sf , DoubleMatrix1D A_f)
	{
		final int NUMSRGS = (int) A_f.size ();
		final int NUMSTATES = A_sf.rows ();
		if (NUMSRGS != A_sf.columns ()) throw new Net2PlanException ("Wrong vector size");
		DoubleMatrix1D pi_s = DoubleFactory1D.dense.make (NUMSTATES);
		if (A_sf.rows() == 0) return pi_s;
		if ((A_f.getMaxLocation() [0] > 1) || (A_f.getMinLocation() [0] < 0)) throw new RuntimeException("Availability must be in range [0, 1]");
		
		for (int s = 0 ; s < A_sf.rows () ; s ++)
		{
			double pi_s_thisState = 1;
			for (int srg = 0; srg < A_sf.columns () ; srg ++)
				pi_s_thisState *= A_sf.get(s,srg) == 1 ? (1 - A_f.get(srg)) : A_f.get(srg);
			pi_s.set(s , pi_s_thisState);
		}
		if (pi_s.zSum() > 1) throw new RuntimeException("Bad - Summation of state probabilities is greater than one (repeated states?) " + pi_s.zSum());
		return pi_s;
	}

	/**
	 * Configures the SRGs into the network design at the given layer. Existing SRGs will be removed.
	 * @param netPlan A network design containing a physical topology
	 * @param defaultMTTF Default value for Mean Time To Fail (in hours). Zero or negative value means {@code Double.MAX_VALUE}
	 * @param defaultMTTR Default value for Mean Time To Repair (in hours). Zero or negative value are not allowed
	 * @param sharedRiskModel Model defining SRGs
	 * @param removeExistingSRGs Wheter or not to remove existing SRGs
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public static void configureSRGs(NetPlan netPlan , double defaultMTTF, double defaultMTTR, SharedRiskModel sharedRiskModel, boolean removeExistingSRGs , NetworkLayer ... optionalLayerParameter)
	{
		if (optionalLayerParameter.length >= 2) throw new Net2PlanException ("None or one layer parameter can be supplied");
		NetworkLayer layer = (optionalLayerParameter.length == 1)? optionalLayerParameter [0] : netPlan.getNetworkLayerDefault();
		List<Node> nodesVector = netPlan.getNodes();
		List<Link> links = netPlan.getLinks(layer);

		if (defaultMTTF <= 0) defaultMTTF = Double.MAX_VALUE;
		if (defaultMTTR <= 0) throw new Net2PlanException("'defaultMTTR' must be greater than zero");

		if (removeExistingSRGs) netPlan.removeAllSRGs();
		
		switch (sharedRiskModel)
		{
			case PER_BIDIRECTIONAL_LINK_BUNDLE:

				for (int n1 = 0; n1 < nodesVector.size (); n1++)
				{
					for (int n2 = n1 + 1; n2 < nodesVector.size (); n2++)
					{
						Collection<Link> nodePairLinks = netPlan.getNodePairLinks(nodesVector.get(n1), nodesVector.get(n2) , true , layer);
						if (nodePairLinks.isEmpty()) continue;

					SharedRiskGroup srg = netPlan.addSRG(defaultMTTF, defaultMTTR, null);
						for(Link link : nodePairLinks)
							srg.addLink(link);
					}
				}

				break;

			case PER_DIRECTIONAL_LINK_BUNDLE:

				for (Node originNode : nodesVector)
				{
					for (Node destinationNode : nodesVector)
					{
						if (originNode.equals(destinationNode)) continue;
						
						Collection<Link> nodePairLinks = netPlan.getNodePairLinks(originNode, destinationNode , false , layer);
						if (nodePairLinks.isEmpty())
						{
							continue;
						}

						SharedRiskGroup srg = netPlan.addSRG(defaultMTTF, defaultMTTR, null);
						for(Link link : nodePairLinks)
							srg.addLink(link);
					}
				}

				break;

			case PER_LINK:

				for (Link link : links)
				{
					SharedRiskGroup srg = netPlan.addSRG(defaultMTTF, defaultMTTR, null);
					srg.addLink (link);
				}

				break;

			case PER_NODE:

				for (Node node : nodesVector)
				{
					SharedRiskGroup srg = netPlan.addSRG(defaultMTTF, defaultMTTR, null);
					srg.addNode (node);
				}

				break;

			default:
				throw new RuntimeException("Bad - Invalid failure model type");
		}
	}
	
	/**
	 * Returns the set of SRGs going down on each failure state.
	 * 
	 * @param srgs SRGs
	 * @param considerNoFailureState Flag to indicate whether or not no failure state is included
	 * @param considerDoubleFailureStates Flag to indicate whether or not double failure states are included
	 * @return List of SRGs going down on each failure state
	 */
	public static List<Set<SharedRiskGroup>> enumerateFailureStates(Collection<SharedRiskGroup> srgs, boolean considerNoFailureState, boolean considerDoubleFailureStates)
	{
		List<Set<SharedRiskGroup>> F_s = new LinkedList<Set<SharedRiskGroup>>();

		/* No failure */
		if (considerNoFailureState) F_s.add(new HashSet<SharedRiskGroup>());

		/* Single failure */
		for (SharedRiskGroup srg : srgs)
		{
			Set<SharedRiskGroup> aux = new HashSet<SharedRiskGroup>(); aux.add(srg);
			F_s.add(aux);
		}

		/* Double failures */
		if (considerDoubleFailureStates)
		{
			for (SharedRiskGroup srg_1 : srgs)
			{
				for (SharedRiskGroup srg_2 : srgs)
				{
					if (srg_2.getIndex () <= srg_1.getIndex ()) continue;
					
					Set<SharedRiskGroup> aux = new HashSet<SharedRiskGroup>(); aux.add(srg_1); aux.add(srg_2);
					F_s.add(aux);
				}
			}
		}

		return F_s;
	}

	/**
	 * Returns a matrix indicating the SRGs going down (columns) on each failure state (rows).
	 * 
	 * @param srgs SRGs
	 * @param considerNoFailureState Flag to indicate whether or not no failure state is included
	 * @param considerDoubleFailureStates Flag to indicate whether or not double failure states are included
	 * @return Matrix of SRGs going down on each failure state
	 */
	public static DoubleMatrix2D getMatrixFailureState2SRG (Collection<SharedRiskGroup> srgs, boolean considerNoFailureState, boolean considerDoubleFailureStates)
	{
		List<Set<SharedRiskGroup>> F_s = SRGUtils.enumerateFailureStates(srgs, considerNoFailureState, considerDoubleFailureStates);
		final int F = F_s.size ();
		final int S = srgs.size ();
		DoubleMatrix2D A_fs = DoubleFactory2D.sparse.make (F,S);
		int f = 0;
		for (Set<SharedRiskGroup> failingSRGs : F_s)
		{
			for (SharedRiskGroup srg : failingSRGs) A_fs.set (f , srg.getIndex () , 1.0);
			f ++;
		}
		return A_fs;		
	}

	/**
	 * Returns a matrix indicating the SRGs going down (columns) on each failure state (rows).
	 *
	 * @param srgs SRGs
	 * @param considerNoFailureState Flag to indicate whether or not no failure state is included
	 * @param considerDoubleFailureStates Flag to indicate whether or not double failure states are included
	 * @return Matrix of SRGs going down on each failure state
	 */
	public static DoubleMatrix2D getMatrixFailureStates2SRG (Collection<SharedRiskGroup> srgs, boolean considerNoFailureState, boolean considerDoubleFailureStates)
	{
		final int numberOfFailureStates = (considerNoFailureState ? 1 : 0) + srgs.size () +  (considerDoubleFailureStates ? srgs.size () * (srgs.size () - 1) / 2 : 0);
		final DoubleMatrix2D F_s = DoubleFactory2D.sparse.make (numberOfFailureStates , srgs.size ());

		/* Single failure */
		int stateIndex = 0;
		
		/* no failure */
		if (considerNoFailureState) stateIndex ++;
		
		/* single failure */
		for (SharedRiskGroup srg : srgs) F_s.set (stateIndex ++ , srg.getIndex () , 1.0);

		/* Double failures */
		if (considerDoubleFailureStates)
			for (SharedRiskGroup srg_1 : srgs)
				for (SharedRiskGroup srg_2 : srgs)
				{
					if (srg_2.getIndex () <= srg_1.getIndex ()) continue;
					F_s.set (stateIndex , srg_1.getIndex () , 1.0);
					F_s.set (stateIndex++ , srg_2.getIndex () , 1.0);
				}
		return F_s;
	}

	/**
	 * Indicates whether SRG definition follows one of the predefined models (per 
	 * node, per link...), or 'Mixed' otherwise (or 'None' if no SRGs are defined).
	 * 
	 * @param netPlan Current network design
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Description of the current SRG model
	 */
	public static String getSRGModel(NetPlan netPlan, NetworkLayer ... optionalLayerParameter)
	{
		if (optionalLayerParameter.length >= 2) throw new Net2PlanException ("None or one layer parameter can be supplied");
		NetworkLayer layer = (optionalLayerParameter.length == 1)? optionalLayerParameter [0] : netPlan.getNetworkLayerDefault ();
		layer.checkAttachedToNetPlanObject(netPlan);
		
		boolean isAnOneSRGPerNodeModel = true;

		int N = netPlan.getNumberOfNodes();
		int E = netPlan.getNumberOfLinks(layer);
		int numSRGs = netPlan.getNumberOfSRGs();
		Collection<SharedRiskGroup> srgs_thisNetPlan = netPlan.getSRGs();
		Set<Node> nodesInSRGs = new LinkedHashSet<Node>();
		for (SharedRiskGroup srg : srgs_thisNetPlan)
		{
			if (!srg.getLinksAllLayers().isEmpty())
			{
				isAnOneSRGPerNodeModel = false;
				break;
			}

			switch (srg.getNodes().size())
			{
				case 0:
					continue;

				case 1:
					Node node = srg.getNodes ().iterator().next();
					if (nodesInSRGs.contains(node)) isAnOneSRGPerNodeModel = false;
					else nodesInSRGs.add(node);

					break;

				default:
					isAnOneSRGPerNodeModel = false;
					break;
			}

			if (!isAnOneSRGPerNodeModel) break;
		}

		if (N == 0 || nodesInSRGs.size() != N) isAnOneSRGPerNodeModel = false;
		nodesInSRGs.clear();

		boolean isAnOneSRGPerLinkModel = true;

		Set<Link> linksInSRG = new LinkedHashSet<Link>();
		for (SharedRiskGroup srg : srgs_thisNetPlan)
		{
//			Set<Long> nodeIds_thisSRG = netPlan.getSRGNodes(srgId);
//			Set<Long> linkIds_thisSRG = netPlan.getSRGLinks(layerId, srgId);

			if (!srg.getNodes ().isEmpty())
			{
				isAnOneSRGPerLinkModel = false;
				break;
			}

			switch (srg.getLinksAllLayers ().size())
			{
				case 0:
					continue;

				case 1:
					Link link = srg.getLinksAllLayers ().iterator().next();
					if (linksInSRG.contains(link)) isAnOneSRGPerLinkModel = false;
					else linksInSRG.add(link);

					break;

				default:
					isAnOneSRGPerLinkModel = false;
					break;
			}

			if (!isAnOneSRGPerLinkModel) break;
		}

		if (E == 0 || linksInSRG.size() != E) isAnOneSRGPerLinkModel = false;
		linksInSRG.clear();

		boolean isAnOneSRGPerLinkBundleModel = true;

		for (SharedRiskGroup srg : srgs_thisNetPlan)
		{
//			Set<Long> nodeIds_thisSRG = netPlan.getSRGNodes(srgId);
//			Set<Long> linkIds_thisSRG = netPlan.getSRGLinks(layerId, srgId);

			if (!srg.getNodes().isEmpty())
			{
				isAnOneSRGPerLinkBundleModel = false;
				break;
			}

			if (srg.getLinksAllLayers().isEmpty()) continue;
			
			Link firstLink = srg.getLinksAllLayers().iterator().next();
			Collection<Link> links_thisNodePair = netPlan.getNodePairLinks(firstLink.getOriginNode() , firstLink.getDestinationNode() , false , layer);
			
			for (Link link : links_thisNodePair)
			{
				if (linksInSRG.contains(link) || !links_thisNodePair.contains(link)) isAnOneSRGPerLinkBundleModel = false;
				if (!isAnOneSRGPerLinkBundleModel) break;

				linksInSRG.add(link);
			}

			if (!isAnOneSRGPerLinkBundleModel) break;
		}

		if (E == 0 || linksInSRG.size() != E) isAnOneSRGPerLinkBundleModel = false;
		linksInSRG.clear();

		boolean isAnOneSRGPerBidiLinkBundleModel = true;

		for (SharedRiskGroup srg : srgs_thisNetPlan)
		{
//			Set<Long> nodeIds_thisSRG = netPlan.getSRGNodes(srgId);
//			Set<Long> linkIds_thisSRG = netPlan.getSRGLinks(layerId, srgId);

			if (!srg.getNodes ().isEmpty())
			{
				isAnOneSRGPerBidiLinkBundleModel = false;
				break;
			}

			if (srg.getLinksAllLayers ().isEmpty()) continue;
			
			Link firstLink = srg.getLinksAllLayers().iterator().next();
			Collection<Link> links_thisNodePair = netPlan.getNodePairLinks(firstLink.getOriginNode() , firstLink.getDestinationNode() , true , layer);
			
			for (Link link : srg.getLinksAllLayers ())
			{
				if (linksInSRG.contains(link) || !srg.getLinksAllLayers ().contains(link)) isAnOneSRGPerBidiLinkBundleModel = false;
				if (!isAnOneSRGPerBidiLinkBundleModel) break;

				linksInSRG.add(link);
			}

			if (!isAnOneSRGPerBidiLinkBundleModel) break;
		}

		if (E == 0 || linksInSRG.size() != E) isAnOneSRGPerBidiLinkBundleModel = false;
		linksInSRG.clear();
		
		String srgModel;
		if (isAnOneSRGPerNodeModel && (!isAnOneSRGPerLinkModel && !isAnOneSRGPerLinkBundleModel && !isAnOneSRGPerBidiLinkBundleModel))
			srgModel = "One SRG per node";
		else if (isAnOneSRGPerBidiLinkBundleModel && !isAnOneSRGPerNodeModel)
			srgModel = "One SRG per bidirectional link bundle";
		else if (isAnOneSRGPerLinkBundleModel && !isAnOneSRGPerNodeModel && !isAnOneSRGPerBidiLinkBundleModel)
			srgModel = "One SRG per unidirectional link bundle";
		else if (isAnOneSRGPerLinkModel && !isAnOneSRGPerNodeModel && !isAnOneSRGPerLinkBundleModel && !isAnOneSRGPerBidiLinkBundleModel)
			srgModel = "One SRG per unidirectional link";
		else
			srgModel = numSRGs > 0 ? "Mixed" : "None";

		return srgModel;
	}

	/**
	 * Returns a set of SRGs that are affecting the given links.
	 * @param links Collection of links
	 * @return SRGs affecting the links
	 */
	public static Set<SharedRiskGroup> getAffectingSRGs (Collection<Link> links)
	{
		if (links == null) return new HashSet<SharedRiskGroup> ();
		if (links.isEmpty()) return new HashSet<SharedRiskGroup> ();
		final NetPlan np = links.iterator().next().getNetPlan();
		Set<SharedRiskGroup> res = new HashSet<SharedRiskGroup> ();
		res.addAll (links.iterator().next().getOriginNode().getSRGs());
		for (Link e : links) 
		{
			res.addAll (e.getSRGs()); res.addAll (e.getDestinationNode().getSRGs());
		}
		return res;
	}

	/**
	 * <p>Returns the percentage of SRG disjointness of traffic routes and their backup paths defined.</p>
	 * 
	 * <p><b>Important</b>: In case there are no SRGs, then it will return zero.</p>
	 * 
	 * @param netPlan Current network design
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Two SRG disjointness values (either considering or not end nodes)
	 */
	public static Pair<Double, Double> getSRGDisjointnessPercentage(NetPlan netPlan, NetworkLayer ... optionalLayerParameter)
	{
		if (optionalLayerParameter.length >= 2) throw new Net2PlanException ("None or one layer parameter can be supplied");
		NetworkLayer layer = (optionalLayerParameter.length == 1)? optionalLayerParameter [0] : netPlan.getNetworkLayerDefault();
		layer.checkAttachedToNetPlanObject(netPlan);
		if (!netPlan.hasSRGs()) return Pair.of(0.0, 0.0);
		
		int accum_routeSRGDisjoint_withEndNodes = 0;
		int accum_routeSRGDisjoint_withoutEndNodes = 0;
		
		int R = netPlan.getNumberOfRoutes(layer);
		for (Route route : netPlan.getRoutes (layer))
		{
			Collection<List<Link>> backupPaths = route.getBackupRoutes().stream().map(e -> e.getSeqLinks()).collect(Collectors.toList());
			if (backupPaths.isEmpty()) continue;

			boolean srgDisjoint_withEndNodes = true;
			boolean srgDisjoint_withoutEndNodes = true;

			List<Link> seqLinks = route.getSeqLinks();
			List<Node> seqNodes = route.getSeqNodes();

			Collection<SharedRiskGroup> routeSRGIds = route.getSRGs();
			Set<SharedRiskGroup> backupPathSRGs = new LinkedHashSet<SharedRiskGroup>();
			for(List<Link> path : backupPaths) backupPathSRGs.addAll(getAffectingSRGs(path));
			
			Set<SharedRiskGroup> commonSRGs_withEndNodes = CollectionUtils.intersect(routeSRGIds, backupPathSRGs);
			if (!commonSRGs_withEndNodes.isEmpty()) srgDisjoint_withEndNodes = false;

			Set<SharedRiskGroup> routeSRGIds_withoutEndNodes = new LinkedHashSet<SharedRiskGroup>();
			Iterator<Node> nodeIt = seqNodes.iterator(); nodeIt.next();
			while(nodeIt.hasNext())
				if (nodeIt.hasNext())
					routeSRGIds_withoutEndNodes.addAll(nodeIt.next ().getSRGs());
			
			for (Link link : seqLinks)
				routeSRGIds_withoutEndNodes.addAll(link.getSRGs());
				
			Set<SharedRiskGroup> commonSRGs_withoutEndNodes = CollectionUtils.intersect(routeSRGIds_withoutEndNodes, backupPathSRGs);
			if (!commonSRGs_withoutEndNodes.isEmpty()) srgDisjoint_withoutEndNodes = false;

			if (srgDisjoint_withEndNodes) accum_routeSRGDisjoint_withEndNodes++;
			if (srgDisjoint_withoutEndNodes) accum_routeSRGDisjoint_withoutEndNodes++;
		}

		double percentageRouteSRGDisjointness_withoutEndNodes = R > 0 ? 100 * accum_routeSRGDisjoint_withoutEndNodes / (double) R : 0;
		double percentageRouteSRGDisjointness_withEndNodes = R > 0 ? 100 * accum_routeSRGDisjoint_withEndNodes / (double) R : 0;

		return Pair.of(percentageRouteSRGDisjointness_withEndNodes, percentageRouteSRGDisjointness_withoutEndNodes);
	}
	
	/** Returns true if the two collection of links in path1 and paths (not necessarily a 
	 * sequence of contiguous links forming a path), are srg disjoint
	 * @param path1 the first collection of links
	 * @param path2 the second collection of links
	 * @return see above
	 */
	public static boolean isSRGDisjoint (Collection<Link> path1 , Collection<Link> path2)
	{
		final Set<SharedRiskGroup> srgs1 = SRGUtils.getAffectingSRGs(path1);
		final Set<SharedRiskGroup> srgs2 = SRGUtils.getAffectingSRGs(path2);
		return Sets.intersection(srgs1 , srgs2).isEmpty();
	}
	
	/** Returns true if the given design is tolerant to single SRG failures at the given layer: that is, no traffic of any 
	 * unicast not multicast demand is blocked when such SRG fails
	 * @param np the design 
	 * @param failureTolerantLayer the layer where we check tolerance
	 * @return see above
	 */
	public static boolean isSingleSRGFailureTolerant (NetPlan np , NetworkLayer failureTolerantLayer)
	{
		final Set<Node> originalNpFailingNodes = new HashSet<> (np.getNodesDown());
		final Set<Link> originalNpFailingLinks = new HashSet<> (np.getLinksDownAllLayers());
		np.setLinksAndNodesFailureState(originalNpFailingLinks , null , originalNpFailingNodes , null);
		final double precFactor = Configuration.precisionFactor;
		if (failureTolerantLayer.getNetPlan() != np) throw new Net2PlanException ("The input layer does not belong to the input NetPlan");
		for (SharedRiskGroup srg : np.getSRGs())
		{
			srg.setAsDown();
			if (np.getVectorDemandBlockedTraffic(failureTolerantLayer).zSum() > precFactor) return false;
			if (np.getVectorMulticastDemandBlockedTraffic(failureTolerantLayer).zSum() > precFactor) return false;
			if (np.getVectorLinkOversubscribedTraffic(failureTolerantLayer).zSum() > precFactor) return false;
			srg.setAsUp();
		}
		np.setLinksAndNodesFailureState(null, originalNpFailingLinks , null , originalNpFailingNodes);
		return true;
	}
	
}
