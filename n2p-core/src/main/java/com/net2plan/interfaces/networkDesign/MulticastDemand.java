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

package com.net2plan.interfaces.networkDesign;

import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.TrafficPredictor;
import com.net2plan.libraries.TrafficSeries;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Pair;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.*;

//import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

/**
 * <p>This class contains a representation of a multicast demand. Multicast demands are defined by its initial node, a set of different end nodes (all different
 * from the initial node)and end node, the network layer they belong to, and their offered traffic. Multicast demand traffic is carried through multicast trees
 * with the same end nodes.</p> 
 * <p> In multilayer designs, a multicast demand in a lower layer can be coupled to a set of links in the upper layer: one link between the demand 
 * initial node, and each demand end node. Then, the demand carried traffic is equal to the upper layer link capacities (thus, upper link capacity
 * and lower layer demand traffic must be measured in the same units). 
 * </p>
 * 
 * @author Pablo Pavon-Marino
 * @since 0.4.0 */

public class MulticastDemand extends NetworkElement implements IMonitorizableElement
{
	final NetworkLayer layer;
	final Node ingressNode;
	final SortedSet<Node> egressNodes;
	double offeredTraffic;
	double carriedTraffic;
	double maximumAcceptableE2EWorstCaseLatencyInMs;
	double offeredTrafficGrowthFactorPerPeriodZeroIsNoGrowth;
	String qosType;
	private TrafficSeries monitoredOrForecastedTraffics;
    private TrafficPredictor trafficPredictor;
	
	SortedMap<Node,Link> coupledUpperLayerLinks;
	SortedSet<MulticastTree> cache_multicastTrees;

	MulticastDemand (NetPlan netPlan , long id , int index , NetworkLayer layer , Node ingressNode , Set<Node> egressNodes , double offeredTraffic , AttributeMap attributes)
	{
		super (netPlan , id , index , attributes);
		this.setName ("MulticastDemand-" + index);

		if (!netPlan.equals(layer.netPlan)) throw new RuntimeException ("Bad");
		if (!netPlan.equals(ingressNode.netPlan)) throw new RuntimeException ("Bad");
		for (Node n : egressNodes) if (!netPlan.equals(n.netPlan)) throw new RuntimeException ("Bad");

//		if (egressNodes.contains(ingressNode)) throw new Net2PlanException("The ingress node of the multicast demand is also an egress node");
//		if (offeredTraffic < 0) throw new Net2PlanException("Offered traffic must be non-negative");

		this.layer = layer;
		this.ingressNode = ingressNode;
		this.egressNodes = new TreeSet<Node> (egressNodes);
		this.offeredTraffic = offeredTraffic;
		this.carriedTraffic = 0;
		this.cache_multicastTrees = new TreeSet<MulticastTree> ();
		this.coupledUpperLayerLinks = null;
		this.maximumAcceptableE2EWorstCaseLatencyInMs = -1;
		this.offeredTrafficGrowthFactorPerPeriodZeroIsNoGrowth = 0;
		this.qosType = null;
		this.monitoredOrForecastedTraffics = new TrafficSeries ();
		this.trafficPredictor = null;
		setQoSType("");
	}

	void copyFrom (MulticastDemand origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.offeredTraffic = origin.offeredTraffic;
		this.carriedTraffic = origin.carriedTraffic;
		this.maximumAcceptableE2EWorstCaseLatencyInMs = origin.maximumAcceptableE2EWorstCaseLatencyInMs;
		this.offeredTrafficGrowthFactorPerPeriodZeroIsNoGrowth = origin.offeredTrafficGrowthFactorPerPeriodZeroIsNoGrowth;
		this.qosType = origin.qosType;
		this.cache_multicastTrees = new TreeSet<MulticastTree> ();
		for (MulticastTree t : origin.cache_multicastTrees) this.addMulticastTree((MulticastTree) this.netPlan.getPeerElementInThisNetPlan (t));
		if (origin.coupledUpperLayerLinks == null)
			this.coupledUpperLayerLinks = null;
		else
		{
			this.coupledUpperLayerLinks = new TreeMap<> ();
			for (Node nOrigin : origin.coupledUpperLayerLinks.keySet()) 
				this.coupledUpperLayerLinks.put(this.netPlan.getNodeFromId (nOrigin.id) , this.netPlan.getLinkFromId (origin.coupledUpperLayerLinks.get(nOrigin).id));
		}
		this.monitoredOrForecastedTraffics = origin.monitoredOrForecastedTraffics;
		this.trafficPredictor = origin.trafficPredictor;
	}

	/** Sets the maximum latency in ms acceptable for the demand. A non-positive value is equivalent to no limit 
	 * @param maxLatencyMs see above
	 */
	public void setMaximumAcceptableE2EWorstCaseLatencyInMs (double maxLatencyMs)
	{
		this.maximumAcceptableE2EWorstCaseLatencyInMs = maxLatencyMs;
	}

	/** Returns the maximum e2e latency registered as acceptable for the demand (in ms)
	 * @return see above
	 */
	public double getMaximumAcceptableE2EWorstCaseLatencyInMs ()
	{
		return maximumAcceptableE2EWorstCaseLatencyInMs <= 0? Double.MAX_VALUE : maximumAcceptableE2EWorstCaseLatencyInMs;
	}
	

	/** Sets the QoS type of the demand. Any previous type is removed. 
	 * @param newQosType  see above
	 */
	public void setQoSType (String newQosType)
	{
		if (newQosType == null) throw new Net2PlanException ("Wrong value");
		if (this.qosType != null)
		{
			final Pair<SortedSet<Demand>,SortedSet<MulticastDemand>> demandsOldType = layer.cache_qosTypes2DemandMap.get (this.qosType);
			assert demandsOldType.getSecond().contains(this);
			demandsOldType.getSecond().remove(this);
			if (demandsOldType.getFirst().isEmpty() && demandsOldType.getSecond().isEmpty()) 
				layer.cache_qosTypes2DemandMap.remove(this.qosType);
		}
		
		Pair<SortedSet<Demand>,SortedSet<MulticastDemand>> demandsNewType = layer.cache_qosTypes2DemandMap.get(newQosType);
		if (demandsNewType == null) { demandsNewType = Pair.of(new TreeSet<> (),new TreeSet<> ()); layer.cache_qosTypes2DemandMap.put(newQosType, demandsNewType); }
		demandsNewType.getSecond().add(this);
		this.qosType = newQosType;
	}


	/** Return the QoS type of the demand 
	 * @return  see above
	 */
	public String getQosType ()
	{
		return this.qosType;
	}

	/** Sets the growth factor of the offered traffic in subsequent periods. 
	 * Offered traffic in period t+1, is the offered traffic in period t, multiplied by (1+growthFactor)
	 * @param growthFactor  see above
	 */
	public void setOfferedTrafficPerPeriodGrowthFactor (double growthFactor)
	{
		if (growthFactor < -1) throw new Net2PlanException ("The growth factor cannot be lower than -1");
		this.offeredTrafficGrowthFactorPerPeriodZeroIsNoGrowth = growthFactor;
	}

	/** Return the growth factor of the offered traffic in subsequent periods.
	 * @return  see above
	 */
	public double getOfferedTrafficPerPeriodGrowthFactor ()
	{
		return offeredTrafficGrowthFactorPerPeriodZeroIsNoGrowth;
	}
	

	boolean isDeepCopy (MulticastDemand e2)
	{
		if (!super.isDeepCopy(e2)) return false;
		if (layer.id != e2.layer.id) return false;
		if (ingressNode.id != e2.ingressNode.id) return false;
		if (this.offeredTraffic != e2.offeredTraffic) return false;
		if (this.carriedTraffic != e2.carriedTraffic) return false;
		if (this.offeredTrafficGrowthFactorPerPeriodZeroIsNoGrowth != e2.offeredTrafficGrowthFactorPerPeriodZeroIsNoGrowth) return false;
		if (!this.qosType.equals(e2.qosType)) return false;
		if (!NetPlan.isDeepCopy(this.egressNodes , e2.egressNodes)) return false;
		if (!NetPlan.isDeepCopy(this.cache_multicastTrees , e2.cache_multicastTrees)) return false;
		if ((this.coupledUpperLayerLinks == null) != (e2.coupledUpperLayerLinks == null)) return false; 
		if (coupledUpperLayerLinks != null)
		{
			if (!NetPlan.isDeepCopy(this.coupledUpperLayerLinks.keySet() , e2.coupledUpperLayerLinks.keySet())) return false;
			for (Node n1 : coupledUpperLayerLinks.keySet())
			{
				final Node n2 = e2.netPlan.getNodeFromId(n1.id);
				final Link l2 = e2.coupledUpperLayerLinks.get(n2);
				if (l2 == null) return false;
				if (l2.id != this.coupledUpperLayerLinks.get(n1).id) return false;
			}
		}
		if (!this.monitoredOrForecastedTraffics.equals(e2.monitoredOrForecastedTraffics)) return false;
		if ((this.trafficPredictor == null) != (e2.trafficPredictor == null)) return false;
		if (trafficPredictor != null) if (!this.trafficPredictor.equals(e2.trafficPredictor)) return false;
		return true;
	}

	
	/**
	 * <p>Returns the {@link com.net2plan.interfaces.networkDesign.NetworkLayer NetworkLayer} object this element belongs to</p>
	 * @return The {@code NetworkLayer}
	 */
	public NetworkLayer getLayer () 
	{ 
		return layer; 
	}

	/**
	 * <p>Returns the worse case end-to-end propagation time of the demand traffic. This is the worse end-to-end propagation time
	 * (summing the {@link com.net2plan.interfaces.networkDesign.Link Link} latencies) for each of the paths from the origin {@link com.net2plan.interfaces.networkDesign.Node Node}, to each output node,
	 * among all the {@link com.net2plan.interfaces.networkDesign.MulticastTree Multicast Trees} carrying traffic
	 * of this demand. In multilayer designs, when the traffic of a multicast tree traverses a link that is coupled to a lower layer demand,
	 * the link latency taken is the worse case propagation time of the underlying demand.</p>
	 * @return The worse case propagation time in miliseconds
	 */
	public double getWorseCasePropagationTimeInMs ()
	{
		double maxPropTimeInMs = 0;
		for (MulticastTree t : this.cache_multicastTrees)
			maxPropTimeInMs = Math.max(maxPropTimeInMs, t.getTreeMaximumPropagationDelayInMs()); 
		return maxPropTimeInMs;
	}

	/**
	 * <p>Returns the worst case end-to-end length in km of this demand traffic. 
	 * This is the worse end-to-end length in km for each of the paths from the origin 
	 * node to each output node, among all the trees carrying traffic
	 * of this demand. In multilayer designs, when the traffic of a multicast tree traverses a link 
	 * that is coupled to a lower layer demand,
	 * the link length taken is the worst case length of the underlying demand.</p>
	 * @return see above
	 */
	public double getWorstCaseLengthInKm ()
	{
		double maxLengthInKm = 0;
		for (MulticastTree t : this.cache_multicastTrees)
			maxLengthInKm = Math.max(maxLengthInKm, t.getTreeMaximumPathLengthInKm()); 
		return maxLengthInKm;
	}

	/**
	 * <p>Returns {@code true} if the traffic of the demand is traversing an oversubscribed {@link com.net2plan.interfaces.networkDesign.Link Link}.</p>
	 * @return {@code True} if the traffic of the demand is traversing an oversubscribed, {@code false} otherwise
	 */
	public boolean isTraversingOversubscribedLinks ()
	{
		for (MulticastTree t : this.cache_multicastTrees)
			for (Link e : t.linkSet) 
				if (e.isOversubscribed()) return true;
		return false;
	}

	/**
	 * <p>Returns the demand ingress Returns the demand ingress {@link com.net2plan.interfaces.networkDesign.Node Node}</p>
	 * @return The ingress node
	 */
	public Node getIngressNode()
	{
		return ingressNode;
	}


	/**
	 * <p>Returns the {@code SortedSet} of demand egress {@link com.net2plan.interfaces.networkDesign.Node Nodes}.</p>
	 * @return The {@code SortedSet} of demand egress nodes as an unmodifiable set
	 */
	public SortedSet<Node> getEgressNodes()
	{
		return Collections.unmodifiableSortedSet(egressNodes);
	}

	/**
	 * <p>Returns the offered traffic of the demand, in traffic units.</p>
	 * @return The demand offered traffic
	 */
	public double getOfferedTraffic()
	{
		return offeredTraffic;
	}


	/**
	 * <p>Returns the carried traffic of the demand, summing all the associated {@link com.net2plan.interfaces.networkDesign.MulticastTree Multicast Trees}</p>
	 * @return The demand carried traffic, in traffic units
	 */
	public double getCarriedTraffic()
	{
		return carriedTraffic;
	}

	/**
	 * <p>Returns the blocked traffic of the demand (offered minus carried, or 0 if carried is above offered).</p>
	 * @return The blocked traffic
	 */
	public double getBlockedTraffic()
	{
		return Math.max (0 , offeredTraffic - carriedTraffic);
	}

	/**
	 * <p>Returns the multicast tree of this demand with lowest cost (and its cost), using the cost per link array provided.
	 * If more than one minimum cost tree exists, all of them are returned. If the cost vector provided is {@code null},
	 * all links are assigned a cost of one.</p>
	 *
	 * @param costs Link weights
	 * @return A {@code Pair} where the first element is a {@code SortedSet} of minimum cost trees, and the second element is the common minimum cost
	 */
	public Pair<SortedSet<MulticastTree>,Double> computeMinimumCostMulticastTrees (double [] costs)
	{
		checkAttachedToNetPlanObject();

		if (costs == null) costs = DoubleUtils.ones(layer.links.size ()); else if (costs.length != layer.links.size()) throw new Net2PlanException ("The array of costs must have the same length as the number of links in the layer");
		SortedSet<MulticastTree> minCostTrees = new TreeSet<MulticastTree> ();
		double minCost = Double.MAX_VALUE;
		for (MulticastTree t : cache_multicastTrees)
		{
			double cost = 0; for (Link link : t.linkSet) cost += costs [link.index];
			if (cost < minCost) { minCost = cost; minCostTrees.clear(); minCostTrees.add (t); }
			else if (cost == minCost) { minCostTrees.add (t); }
		}
		return Pair.of(minCostTrees , minCost);
	}

	/**
	 * <p>Returns {@code true} if more than one {@link com.net2plan.interfaces.networkDesign.MulticastTree Multicast Tree} is associated to the demand (trees with zero carried traffic, or that are
	 * down also count).</p>
	 * @return {@code True} if traffic is bifurcated between two or more multicast trees (see above), or {@code false} otherwise
	 */
	public boolean isBifurcated ()
	{
		return this.cache_multicastTrees.size () >= 2;
	}

	/**
	 * <p>Returns {@code true} if the carried traffic is strictly less than the offered traffic, up to the Net2Plan
	 * precision factor.</p>
	 * @return {@code True} if the demand has blocked traffic, {@code false} otherwise
	 * @see com.net2plan.interfaces.networkDesign.Configuration
	 */
	public boolean isBlocked ()
	{
		return this.carriedTraffic  + Configuration.precisionFactor < offeredTraffic;
	}

	/**
	 * <p>Returns the {@link com.net2plan.interfaces.networkDesign.MulticastTree Multicast Trees} associated to this demand.</p>
	 * @return The {@code SortedSet} of {@code MulticastTree} as an unmodifable set (empty if no trees exist)
	 */
	public SortedSet<MulticastTree> getMulticastTrees ()
	{
		return Collections.unmodifiableSortedSet(cache_multicastTrees);
	}

	/** Returns true if all the multicast trees of the demand, reach all the multicast demand egress nodes 
	 * @return  see above
	 */
	public boolean isAllTreesReachingAllEgressNodes () 
	{
		return getMulticastTrees().stream().allMatch(t->t.isReachinAllDemandEgressNodes());
	}

	
	/**
	 * Associates a new Multicast Tree to this Multicast Demand.
	 * @param multicastTree The Multicast Tree to be added.
	 */
	void addMulticastTree(MulticastTree multicastTree)
	{
		this.cache_multicastTrees.add(multicastTree);
	}

	/**
	 * <p>Couples this demand to the given {@code SortedSet} of {@link com.net2plan.interfaces.networkDesign.Link Links} in the upper layer. The number of links equals to the number of egress nodes of the
	 * demand. Links must start in the demand ingress node, and end in each of the multicast demand egress nodes.</p>
	 * <p><b>Important:</b> This demand and the links must not be already coupled.</p>
	 * @param links The set of links to couple to
	 */
	public void couple(Set<Link> links)
	{
		this.checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (links.size () != egressNodes.size ()) throw new Net2PlanException ("Multicast demands can be coupled to a set of links, starting in the demand ingress node, and ending in each of the multicast demand egress nodes");
		final NetworkLayer lowerLayer = layer;
		final NetworkLayer upperLayer = links.iterator().next().layer;
		if (coupledUpperLayerLinks != null) throw new Net2PlanException("Demand " + id + " at layer " + lowerLayer.id + " is already associated to an upper layer demand");
		if (upperLayer.equals(lowerLayer)) throw new Net2PlanException("Bad - Trying to couple links and demands in the same layer");
		if (!upperLayer.linkCapacityUnitsName.equals(lowerLayer.demandTrafficUnitsName))
			throw new Net2PlanException(String.format(netPlan.TEMPLATE_CROSS_LAYER_UNITS_NOT_MATCHING, upperLayer.id, upperLayer.linkCapacityUnitsName, lowerLayer.id, lowerLayer.demandTrafficUnitsName));
		SortedMap<Node,Link> mapLink2EgressNode = new TreeMap<> ();
		for (Link link : links)
		{
			link.checkAttachedToNetPlanObject(netPlan);
			if ((link.coupledLowerOrThisLayerDemand!= null) || (link.coupledLowerLayerMulticastDemand != null)) throw new Net2PlanException("Link " + link + " at layer " + upperLayer.id + " is already associated to a lower layer demand");
			if (!link.originNode.equals(ingressNode)) throw new Net2PlanException ("Multicast demands can be coupled to a set of links, starting in the demand ingress node, and ending in each of the multicast demand egress nodes");
			if (!link.layer.equals(upperLayer)) throw new Net2PlanException ("Multicast demands can be coupled to a set of links, starting in the demand ingress node, and ending in each of the multicast demand egress nodes");
			mapLink2EgressNode.put(link.destinationNode, link);
		}
		if (!mapLink2EgressNode.keySet().equals(egressNodes)) throw new Net2PlanException ("Multicast demands can be coupled to a set of links, starting in the demand ingress node, and ending in each of the multicast demand egress nodes");
		
		DemandLinkMapping coupling_thisLayerPair = netPlan.interLayerCoupling.getEdge(lowerLayer, upperLayer);
		if (coupling_thisLayerPair == null)
		{
			coupling_thisLayerPair = new DemandLinkMapping();
			boolean valid;
			try { valid = netPlan.interLayerCoupling.addEdge(lowerLayer, upperLayer, coupling_thisLayerPair); }
			catch (IllegalArgumentException ex) { valid = false; }
			if (!valid) throw new Net2PlanException("Coupling between link set " + links + " at layer " + upperLayer + " and multicast demand " + id + " at layer " + lowerLayer.id + " would induce a cycle between layers");
		}

		/* Link capacity at the upper layer is equal to the carried traffic at the lower layer */
		coupledUpperLayerLinks = new TreeMap<> ();
		layer.cache_coupledMulticastDemands.add (this);
		for (Link link : links)
		{
			link.updateCapacityAndZeroCapacityLinksAndRoutesCaches(carriedTraffic);  
			link.coupledLowerLayerMulticastDemand = this;
			link.layer.cache_coupledLinks.add (link);
			this.coupledUpperLayerLinks.put(link.destinationNode, link);
			link.updateWorstCasePropagationTraversingUnicastDemandsAndMaybeRoutes();
		}
		coupling_thisLayerPair.put(this, new TreeSet<Link> (links));
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Creates new links in the given layer, from the demand ingress node to each one of the demand egress nodes, and couples these newly created links to the multicast demand.</p>
	 * @param newLinkLayer The layer where the new links will be created
	 * @return The set of created links, already coupled to the demand
	 */
	public SortedSet<Link> coupleToNewLinksCreated (NetworkLayer newLinkLayer)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		newLinkLayer.checkAttachedToNetPlanObject(this.netPlan);
		if (this.layer.equals (newLinkLayer)) throw new Net2PlanException ("Cannot couple a link and a demand in the same layer");
		if (isCoupled()) throw new Net2PlanException ("The multicast demand is already coupled");
		SortedSet<Link> newLinks = new TreeSet<Link> ();
		try
		{
			for (Node egressNode : egressNodes)
			{
				Link newLink = netPlan.addLink(ingressNode , egressNode , carriedTraffic , netPlan.getNodePairEuclideanDistance(ingressNode , egressNode) , 200000 , null , newLinkLayer);
				newLinks.add (newLink);
			}
			couple (newLinks);
		} catch (Exception e) { for (Link link : newLinks) link.remove (); throw e; }
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		return newLinks;
	}
	
	/**
	 * <p>Returns the {@code SortedSet} of {@link com.net2plan.interfaces.networkDesign.Link Links} this demand is coupled to, or an empty {@code SortedSet} if this demand is not coupled.</p>
	 * @return The {@code SortedSet} of links (a copy , any changes will not affect the coupling)
	 */
	public SortedSet<Link> getCoupledLinks ()
	{ 
		checkAttachedToNetPlanObject();
		return coupledUpperLayerLinks == null? new TreeSet<Link> () : new TreeSet<Link> (coupledUpperLayerLinks.values());
	}

	/**
	 * <p>Returns {@code true} if the demand is coupled.</p>
	 * @return {@code true} if the demand is coupled, {@code false} otherwise
	 */
	public boolean isCoupled () { return coupledUpperLayerLinks != null; }

	/**
	 * <p>Decouples this mulicast demand from a set of upper layer {@link com.net2plan.interfaces.networkDesign.Link Links}. If the demand is not coupled, an exception is thrown</p>
	 */
	public void decouple()
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (coupledUpperLayerLinks == null) throw new Net2PlanException ("The multicast demand is not coupled");
		Collection<Link> links = coupledUpperLayerLinks.values();
		for (Link link : links) link.checkAttachedToNetPlanObject(this.netPlan); 
		for (Link link : links) if (link.coupledLowerLayerMulticastDemand != this) throw new RuntimeException ("Bad"); 
		final NetworkLayer upperLayer = links.iterator().next().layer;
		final NetworkLayer lowerLayer = layer;

		upperLayer.cache_coupledLinks.removeAll(links);
		layer.cache_coupledMulticastDemands.remove(this);
		for (Link link : links)
			link.coupledLowerLayerMulticastDemand = null;

		DemandLinkMapping coupling_thisLayerPair = netPlan.interLayerCoupling.getEdge(lowerLayer, upperLayer);
		coupling_thisLayerPair.remove(this);
		if (coupling_thisLayerPair.isEmpty()) netPlan.interLayerCoupling.removeEdge(lowerLayer , upperLayer);
		coupledUpperLayerLinks = null;
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}
	
	/**
	 * <p>Removes this multicast demand, and any associated multicast trees. If the demand is coupled, it is first decoupled. </p>
	 */
	public void remove()
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (this.coupledUpperLayerLinks != null) this.decouple ();
		
		for (MulticastTree tree : new TreeSet<MulticastTree> (cache_multicastTrees)) tree.remove();

		netPlan.cache_id2MulticastDemandMap.remove(id);
		NetPlan.removeNetworkElementAndShiftIndexes (layer.multicastDemands , index);
		ingressNode.cache_nodeOutgoingMulticastDemands.remove (this);
		for (Node egressNode : egressNodes) egressNode.cache_nodeIncomingMulticastDemands.remove (this);
        for (String tag : tags) netPlan.cache_taggedElements.get(tag).remove(this);
		final Pair<SortedSet<Demand>,SortedSet<MulticastDemand>> qosInfo = layer.cache_qosTypes2DemandMap.get(qosType);
		assert qosInfo != null;
		final boolean removed = qosInfo.getSecond().remove(this);
		assert removed;
		if (qosInfo.getFirst().isEmpty() && qosInfo.getSecond().isEmpty()) layer.cache_qosTypes2DemandMap.remove(qosType);

        final NetPlan npOld = this.netPlan;
        removeId();
        if (ErrorHandling.isDebugEnabled()) npOld.checkCachesConsistency();
	}

	
	/**
	 * <p>Sets the offered traffic by a multicast demand. This does not change the carried traffic, which only depends on the associated multicast trees. </p>
	 * @param offeredTraffic Offered traffic
	 */
	public void setOfferedTraffic(double offeredTraffic)
	{
		offeredTraffic = NetPlan.adjustToTolerance(offeredTraffic);
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (offeredTraffic < 0) throw new Net2PlanException("Offered traffic must be greater or equal than zero");
		this.offeredTraffic = offeredTraffic;
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Returns a {@code String} representation of the multicast demand.</p>
	 * @return {@code String} representation of the multicast demand
	 */
	public String toString () { return "md" + index + " (id " + id + ")"; }

	void checkCachesConsistency ()
	{
		super.checkCachesConsistency ();

		double check_arriedTraffic = 0;
		for (MulticastTree t : cache_multicastTrees)
		{
			if (!t.demand.equals (this)) throw new RuntimeException ("Bad");
			check_arriedTraffic += t.getCarriedTraffic();
		}
		if (carriedTraffic != check_arriedTraffic) throw new RuntimeException ("Bad");
		if (coupledUpperLayerLinks != null)
			for (Link e : coupledUpperLayerLinks.values ())
				if (!e.coupledLowerLayerMulticastDemand.equals (this)) throw new RuntimeException ("Bad");
		if (!layer.multicastDemands.contains(this)) throw new RuntimeException ("Bad");
	}

    /** Returns a map with an entry for each traversed link, and associated to it the demand's traffic carried in that link. 
     * If selected by the user, the carried traffic is given as a fraction respect to the demand offered traffic
     * @param normalizedToOfferedTraffic  see above
     * @return  see above
     */
    public SortedMap<Link, Double> getTraversedLinksAndCarriedTraffic(final boolean normalizedToOfferedTraffic)
    {
        final SortedMap<Link, Double> res = new TreeMap<>();
        final double normalizationFactor = (normalizedToOfferedTraffic ? (offeredTraffic <= Configuration.precisionFactor ? 1.0 : 1 / offeredTraffic) : 1.0);
        for (MulticastTree r : getMulticastTrees())
            for (Link e : r.getLinkSet())
            {
                final Double currentVal = res.get(e);
                final double newValue = (currentVal == null ? 0 : currentVal) + (r.getCarriedTraffic() * normalizationFactor);
                res.put(e, newValue);
            }
        return res;
    }

	
	/** Returns the set of links in this layer that could potentially carry traffic of this multicast demand, 
	 * when flowing from the origin node to the given egress node, according to the multicast trees defined. 
	 * These are the links that are part of any path from demand ingress, the given egress node 
	 * defined, in any multicast tree. If one of these paths contains a failed link, none of these 
	 * links of such multicast tree are considered.
	 * @param egressNode the egress node
	 * @return see above
	 */
	public SortedSet<Link> getLinksNoDownPropagationPotentiallyCarryingTraffic  (Node egressNode)
	{
		checkAttachedToNetPlanObject();
		if (!this.egressNodes.contains(egressNode)) throw new Net2PlanException ("This is not an egress node of the multicast demand");
		
		SortedSet<Link> res = new TreeSet<> ();
		for (MulticastTree t : cache_multicastTrees)
		{
			List<Link> pathToTarget = t.getSeqLinksToEgressNode(egressNode);
			if (pathToTarget == null) continue;
			if (!netPlan.isUp(pathToTarget)) continue;
			res.addAll(pathToTarget);
		}
		return res;
	}

	/** Returns the object contianing the monitored or forecasted time series information for the offered traffic
	 * @return see above
	 */
	public TrafficSeries getMonitoredOrForecastedOfferedTraffic () { return this.monitoredOrForecastedTraffics; }

	/** Sets the new time series for the monitored or forecasted offered traffic, eliminating any previous values 
	 * @param newTimeSeries  see above
	 */
	public void setMonitoredOrForecastedOfferedTraffic (TrafficSeries newTimeSeries) { this.monitoredOrForecastedTraffics = new TrafficSeries (newTimeSeries.getValues()); }

	@Override
	public TrafficSeries getMonitoredOrForecastedCarriedTraffic()
	{
        return this.monitoredOrForecastedTraffics;
	}

	@Override
	public void setMonitoredOrForecastedCarriedTraffic(TrafficSeries newTimeSeries) 
	{
		this.monitoredOrForecastedTraffics = newTimeSeries;
	}

	@Override
	public double getCurrentTrafficToAddMonitSample() 
	{
		return getOfferedTraffic();
	}

	@Override
	public boolean isPossibleToSetCurrentTrafficAsGivenMonitSample() 
	{
		return true;
	}

	@Override
	public void setCurrentTrafficToGivenMonitSample(double traffic) 
	{
		this.setOfferedTraffic(traffic);
	}

	@Override
	public Optional<TrafficPredictor> getTrafficPredictor() 
	{
		return Optional.ofNullable(this.trafficPredictor);
	}

	@Override
	public void setTrafficPredictor(TrafficPredictor tp) 
	{
		this.trafficPredictor = tp;
	}

	@Override
	public void removeTrafficPredictor() 
	{
		this.trafficPredictor = null;
	}


}
