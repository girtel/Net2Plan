//CACHES:
//	- WORST PROPAGATION CACHE
//	- FORW RULES PER NODE & DEMAND
//	- 


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

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.google.common.collect.Sets;
import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.libraries.GraphUtils.ClosedCycleRoutingException;
import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Quintuple;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/** <p>This class contains a representation of a unicast demand. Unicast demands are defined by its initial and end node, the network layer they belong to, 
 * and their offered traffic. When the routing in the network layer is the type {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, demands are carried
 * throgh {@link com.net2plan.interfaces.networkDesign.Route Route} objects associated to them. If the routing type is {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}, the demand
 * traffic is carried by defining the forwarding rules (for each demand and link, fraction of the demand traffic from the link initial node, that is carried through it).</p>
 * <p> In multilayer design, a demand in a lower layer can be coupled to a link in the upper layer. Then, the demand carried traffic is equal to the
 * upper layer link capacity (thus, upper link capacity and lower layer demand traffic must be measured in the same units).
 * </p>
 *
 * @see com.net2plan.interfaces.networkDesign.Node
 * @see com.net2plan.interfaces.networkDesign.Route
 * @see com.net2plan.interfaces.networkDesign.Link
 * @author Pablo Pavon-Marino
 */
public class Demand extends NetworkElement
{
	final NetworkLayer layer;
	final Node ingressNode;
	final Node egressNode;
	double offeredTraffic;
	double carriedTraffic;
	RoutingCycleType routingCycleType;
	Demand bidirectionalPair;

	
	Set<Route> cache_routes;
	Link coupledUpperLayerLink;
	List<String> mandatorySequenceOfTraversedResourceTypes;
	IntendedRecoveryType recoveryType;

	double cache_worstCasePropagationTimeMs;
	double cache_worstCaseLengthInKm;
	Map<Link,Double> cacheHbH_frs; // cannot be an entry if zero in FR
	Map<Link,Pair<Double,Double>> cacheHbH_normCarriedOccupiedPerLinkCurrentState; // norm carried is respect to demand total CARRIED traffic, occupied capacity is absolute
	Map<Node,Set<Link>> cacheHbH_linksPerNodeWithNonZeroFr; 
	
	public enum IntendedRecoveryType
	{
		/** This information is not specified */
		NOTSPECIFIED,
		/** No reaction to failures to any route of the demand */
		NONE,
		/** An attempt is made to reroute the failed lightpaths of the demand (backup or not, but this makes sense when the routes have no backup) */
		RESTORATION,
		/** To carry the traffic, first the primary is tried, then the backup routes in order. Unused routes have carried traffic zero. No revert action is made */
		PROTECTION_NOREVERT,
		/** To carry the traffic, first the primary is tried, then the backup routes in order. Unused routes have carried traffic zero. If the primary becomes usable, traffic is reverted to it */
		PROTECTION_REVERT,
		/** When read from a file, an unknown type was read */
		UNKNOWNTYPE;
	}
	
	/**
	 * Generates a new Demand.
	 * @param netPlan Design where this demand is attached
	 * @param id Unique Id
	 * @param index Index
	 * @param layer Network Layer where this demand is created
	 * @param ingressNode Ingress node
	 * @param egressNode Egress node
	 * @param offeredTraffic Offered traffic
	 * @param attributes Attributes map
	 */
	Demand (NetPlan netPlan , long id , int index , NetworkLayer layer , Node ingressNode , Node egressNode , double offeredTraffic , AttributeMap attributes)
	{
		super (netPlan , id , index , attributes);

		if (ingressNode.equals (egressNode)) throw new Net2PlanException("Self-demands are not allowed");
		if (offeredTraffic < 0) throw new Net2PlanException("Offered traffic must be non-negative");
		
		this.layer = layer;
		this.ingressNode = ingressNode;
		this.egressNode = egressNode;
		this.offeredTraffic = offeredTraffic;
		this.carriedTraffic = 0;
		this.routingCycleType = RoutingCycleType.LOOPLESS;
		this.cache_routes = new LinkedHashSet<Route> ();
		this.coupledUpperLayerLink = null;
		this.mandatorySequenceOfTraversedResourceTypes = new ArrayList<String> ();
		this.recoveryType = IntendedRecoveryType.NOTSPECIFIED;
		this.cacheHbH_frs = new HashMap<> ();
		this.cacheHbH_normCarriedOccupiedPerLinkCurrentState = new HashMap<> ();
		this.cacheHbH_linksPerNodeWithNonZeroFr = new HashMap<> ();
		this.cache_worstCasePropagationTimeMs = 0;
		this.cache_worstCaseLengthInKm = 0;
		this.bidirectionalPair = null;
	}

	/**
	 * <p>Removes all current information from the current {@code Demand} and copy all the information from the input {@code Demand}</p>.
	 * @param origin Demand to be copied.
	 */
	void copyFrom (Demand origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.offeredTraffic = origin.offeredTraffic;
		this.carriedTraffic = origin.carriedTraffic;
		this.routingCycleType = origin.routingCycleType;
		this.coupledUpperLayerLink = origin.coupledUpperLayerLink == null? null : this.netPlan.getLinkFromId (origin.coupledUpperLayerLink.id);
		this.cache_routes = new LinkedHashSet<Route> ();
		for (Route r : origin.cache_routes) this.cache_routes.add(this.netPlan.getRouteFromId(r.id));
		this.mandatorySequenceOfTraversedResourceTypes = new ArrayList<String> (origin.mandatorySequenceOfTraversedResourceTypes);
		this.recoveryType = origin.recoveryType;
		this.cache_worstCasePropagationTimeMs = origin.cache_worstCasePropagationTimeMs;
		this.cache_worstCaseLengthInKm = origin.cache_worstCaseLengthInKm;
		this.cacheHbH_frs.clear();
		for (Link originLink : origin.cacheHbH_frs.keySet())
			this.cacheHbH_frs.put(netPlan.getLinkFromId(originLink.id), origin.cacheHbH_frs.get(originLink));
		this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.clear();
		for (Link originLink : origin.cacheHbH_normCarriedOccupiedPerLinkCurrentState.keySet())
			this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.put(netPlan.getLinkFromId(originLink.id), origin.cacheHbH_normCarriedOccupiedPerLinkCurrentState.get(originLink));
		this.cacheHbH_linksPerNodeWithNonZeroFr.clear();
		for (Entry<Node,Set<Link>> entry : origin.cacheHbH_linksPerNodeWithNonZeroFr.entrySet())
			this.cacheHbH_linksPerNodeWithNonZeroFr.put(netPlan.getNodeFromId(entry.getKey().id), (Set<Link>) (Set<?>) netPlan.translateCollectionToThisNetPlan(entry.getValue()));
		this.bidirectionalPair = origin.bidirectionalPair == null? null : netPlan.getDemandFromId(origin.bidirectionalPair.getId());
	}


	boolean isDeepCopy (Demand e2)
	{
		if (!super.isDeepCopy(e2)) return false;
		if (layer.id != e2.layer.id) return false;
		if (ingressNode.id != e2.ingressNode.id) return false;
		if (egressNode.id != e2.egressNode.id) return false;
		if ((this.bidirectionalPair == null) != (e2.bidirectionalPair == null)) return false;
		if (this.bidirectionalPair != null) if (this.bidirectionalPair.id != e2.bidirectionalPair.id) return false;
		if (this.offeredTraffic != e2.offeredTraffic) return false;
		if (this.carriedTraffic != e2.carriedTraffic) return false;
		if (this.cache_worstCasePropagationTimeMs != e2.cache_worstCasePropagationTimeMs) return false;
		if (this.cache_worstCaseLengthInKm != e2.cache_worstCaseLengthInKm) return false;
		if (this.routingCycleType != e2.routingCycleType) return false;
		if ((this.coupledUpperLayerLink == null) != (e2.coupledUpperLayerLink == null)) return false; 
		if ((this.coupledUpperLayerLink != null) && (coupledUpperLayerLink.id != e2.coupledUpperLayerLink.id)) return false;
		if (!NetPlan.isDeepCopy(this.cache_routes , e2.cache_routes)) return false;
		if (!this.mandatorySequenceOfTraversedResourceTypes.equals(e2.mandatorySequenceOfTraversedResourceTypes)) return false;
		if (this.recoveryType != e2.recoveryType) return false;
		if (!NetPlan.isDeepCopy(this.cacheHbH_frs , e2.cacheHbH_frs)) return false;
		if (!NetPlan.isDeepCopy(this.cacheHbH_normCarriedOccupiedPerLinkCurrentState , e2.cacheHbH_normCarriedOccupiedPerLinkCurrentState)) return false;
		return true;
	}

	/** Returns the specified intended recovery type for this demand
	 * @return see above
	 */
	public IntendedRecoveryType getIntendedRecoveryType () { return recoveryType; }
	
	/** Sets the intended recovery type for this demand
	 * @param recoveryType the recovery type
	 */
	public void setIntendedRecoveryType (IntendedRecoveryType recoveryType) { this.recoveryType = recoveryType; }
	
	/**
	 * <p>Returns the routes associated to this demand.</p>
	 * <p><b>Important</b>: If network layer routing type is not {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, an exception is thrown.</p>
	 * @return The set of routes as an unmodifiable set, an empty set if no route exists.
	 * */
	public Set<Route> getRoutes()
	{
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		return Collections.unmodifiableSet(cache_routes);
	}

	/**
	 * <p>Returns the routes associated to this demand, but only those that are a backup route.</p>
	 * <p><b>Important</b>: If network layer routing type is not {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, an exception is thrown.</p>
	 * @return The set of routes
	 * */
	public Set<Route> getRoutesAreBackup ()
	{
		return getRoutes ().stream().filter(e -> e.isBackupRoute()).collect(Collectors.toSet());
	}

	/**
	 * <p>Returns the routes associated to this demand, but only those that are have themselves a backup route.</p>
	 * <p><b>Important</b>: If network layer routing type is not {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, an exception is thrown.</p>
	 * @return The set of routes
	 * */
	public Set<Route> getRoutesHaveBackup ()
	{
		return getRoutes ().stream().filter(e -> e.hasBackupRoutes()).collect(Collectors.toSet());
	}

	/**
	 * <p>Returns the routes associated to this demand, but only those that have no backup route themselves.</p>
	 * <p><b>Important</b>: If network layer routing type is not {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, an exception is thrown.</p>
	 * @return The set of routes
	 * */
	public Set<Route> getRoutesHaveNoBackup ()
	{
		return getRoutes ().stream().filter(e -> !e.hasBackupRoutes()).collect(Collectors.toSet());
	}

	/**
	 * <p>Returns the routes associated to this demand, but only those that are not a backup route.</p>
	 * <p><b>Important</b>: If network layer routing type is not {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, an exception is thrown.</p>
	 * @return The set of routes
	 * */
	public Set<Route> getRoutesAreNotBackup ()
	{
		return getRoutes ().stream().filter(e -> !e.isBackupRoute()).collect(Collectors.toSet());
	}
	
	/** 
	 * <p>Returns the worse case end-to-end length in km of the demand traffic. If the routing is source routing, this is the worst length 
	 * (summing the link lengths in km) for all the routes carrying traffic. If the routing is hop-by-hop and loopless, 
	 * the paths followed are computed and the worst case length among all the paths is returned. 
	 * If the hop-by-hop routing has loops, {@code Double.MAX_VALUE} is returned. In multilayer design, when the
	 * traffic of a demand traverses a link that is coupled to a lower layer demand, the link length taken is the 
	 * worst case length of the lower layer demand
	 * of the underlying demand.</p>
	 * @return see above
	 */
	public double getWorstCaseLengthInKm ()
	{
		return cache_worstCaseLengthInKm;
	}
	
	/**
	 * <p>Returns the worse case end-to-end propagation time of the demand traffic. If the routing is source routing, this is the worse propagation time
	 * (summing the link latencies) for all the routes carrying traffic. If the routing is hop-by-hop and loopless, the paths followed are computed and 
	 * the worse case propagation time is returned. If the hop-by-hop routing has loops, {@code Double.MAX_VALUE} is returned. In multilayer design, when the
	 * traffic of a demand traverses a link that is coupled to a lower layer demand, the link latency taken is the worse case propagation time
	 * of the underlying demand.</p>
	 * @return The worse case propagation time in miliseconds
	 * */
	public double getWorstCasePropagationTimeInMs ()
	{
		return cache_worstCasePropagationTimeMs;
//		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
//		double maxPropTimeInMs = 0;
//		if (layer.routingType == RoutingType.SOURCE_ROUTING)
//		{
//			for (Route r : this.cache_routes)
//			{
//				double timeInMs = 0; 
//				for (Link e : r.cache_seqLinksRealPath) 
//					if (e.isCoupled()) 
//						timeInMs += e.coupledLowerLayerDemand.getWorstCasePropagationTimeInMs();
//					else
//						timeInMs += e.getPropagationDelayInMs();
//				maxPropTimeInMs = Math.max (maxPropTimeInMs , timeInMs);
//			}
//		}
//		else
//		{
//			if (this.routingCycleType == RoutingCycleType.CLOSED_CYCLES) return Double.MAX_VALUE;
//			if (this.routingCycleType == RoutingCycleType.OPEN_CYCLES) return Double.MAX_VALUE;
//			List<Demand> d_p = new LinkedList<Demand> ();
//			List<Double> x_p = new LinkedList<Double> ();
//			List<List<Link>> pathList = new LinkedList<List<Link>> ();
//			GraphUtils.convert_xde2xp(netPlan.nodes, layer.links , Arrays.asList(this) , layer.forwardingRulesCurrentFailureState_x_de , d_p, x_p, pathList);
//			Iterator<Demand> it_demand = d_p.iterator();
//			Iterator<Double> it_xp = x_p.iterator();
//			Iterator<List<Link>> it_pathList = pathList.iterator();
//			while (it_demand.hasNext())
//			{
//				final Demand d = it_demand.next();
//				final double trafficInPath = it_xp.next();
//				final List<Link> seqLinks = it_pathList.next();
//				if (trafficInPath > PRECISION_FACTOR)
//				{
//					double propTimeThisSeqLinks = 0; 
//					for (Link e : seqLinks) 
//						if (e.isCoupled()) 
//							propTimeThisSeqLinks += e.coupledLowerLayerDemand.getWorstCasePropagationTimeInMs();
//						else
//							propTimeThisSeqLinks += e.getPropagationDelayInMs();
//					maxPropTimeInMs = Math.max(propTimeThisSeqLinks , propTimeThisSeqLinks);
//				}
//			}
		
//		ERRROR: si acoplado a multicast, no da buenos valores de getWorseCase
//		}
//		return maxPropTimeInMs;
	}

//	private double recursiveWCPropapagationDelay ()
//	{
//		final Map<Node,Double> inNodes = new HashMap<> ();
//		inNodes.put(this.ingressNode, 0.0);
//		do
//		{
//			for (Node n : new ArrayList<> (inNodes.keySet()))
//			{
//				if (n == egressNode) continue;
//				double wcSoFar = inNodes.get(n);
//				for (Link e : cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.keySet())
//					if (e.getOriginNode() == n)
//						if (cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.get(e).getFirst() > 0)
//						{
//							final double thisPathCost = wcSoFar + e.getPropagationDelayInMs();
//							if (inNodes.containsKey(e.getDestinationNode()))
//								inNodes.put(e.getDestinationNode(), Math.max(inNodes.get(e.getDestinationNode()) , thisPathCost));
//							else
//								inNodes.put(e.getDestinationNode(), thisPathCost);
//						}
//				inNodes.remove(n);
//			}
//			
//			if (inNodes.isEmpty()) return Double.MAX_VALUE;
//			if ((inNodes.size() == 1) && (inNodes.keySet().iterator().next() == this.egressNode)) return inNodes.get(egressNode);
//		} while (true);
//	}

	/**
	 * <p>Returns {@code true} if the traffic of the demand is traversing an oversubscribed link, {@code false} otherwise.</p>
	 * @return {@code true} if the traffic is traversing an oversubscribed link, {@code false} otherwise
	 */
	public boolean isTraversingOversubscribedLinks ()
	{
		if (layer.isSourceRouting())
		{
			for (Route r : cache_routes)
				for (Link e : r.getSeqLinks())
					if (e.isOversubscribed())
						return true;
		}
		else
		{
			for (Link e : cacheHbH_normCarriedOccupiedPerLinkCurrentState.keySet())
				if (cacheHbH_normCarriedOccupiedPerLinkCurrentState.get(e).getFirst() > Configuration.precisionFactor)
					if (e.isOversubscribed()) return true;
		}
		return false;
	}
	
	/**
	 * <p>Returns {@code true} if the traffic of the demand is traversing an oversubscribed resource, {@code false} otherwise. If the 
	 * layer is not in a SOURCE ROUTING mode, an Exception is thrown</p>
	 * @return {@code true} if the traffic is traversing an oversubscribed resource, {@code false} otherwise
	 */
	public boolean isTraversingOversubscribedResources ()
	{
		if (layer.routingType != RoutingType.SOURCE_ROUTING) throw new Net2PlanException ("The routing type must be SOURCE ROUTING");
		for (Route r : this.cache_routes)
			for (Resource res : r.getSeqResourcesTraversed()) 
				if (res.isOversubscribed()) return true;
		return false;
	}

	/** Gets the sequence of types of resources defined (the ones that the routes of this demand have to follow). An empty list is 
	 * returned if the method setMandatorySequenceOfTraversedResourceTypes was not called never before for this demand.
	 * @return the list
	 */
	public List<String> getServiceChainSequenceOfTraversedResourceTypes ()
	{
		if (layer.routingType != RoutingType.SOURCE_ROUTING) throw new Net2PlanException ("The routing type must be SOURCE ROUTING");
		return Collections.unmodifiableList(this.mandatorySequenceOfTraversedResourceTypes);
	}

	/** Sets the sequence of types of resources that the routes of this demand have to follow. This method is to make the demand become 
	 * a request of service chains. This method can only be called if the routing type is SOURCE ROUTING, and the demand has no routes 
	 * at the moment.
	 * @param resourceTypesSequence the sequence of types of the resources that has to be traversed by all the routes of this demand. If null, an empty sequence is assumed
	 */
	public void setServiceChainSequenceOfTraversedResourceTypes (List<String> resourceTypesSequence)
	{
		if (layer.routingType != RoutingType.SOURCE_ROUTING) throw new Net2PlanException ("The routing type must be SOURCE ROUTING");
		if (!cache_routes.isEmpty()) throw new Net2PlanException ("The demand must not have routes to execute this method");
		if (resourceTypesSequence == null)
			this.mandatorySequenceOfTraversedResourceTypes = new ArrayList<String> ();
		else
			this.mandatorySequenceOfTraversedResourceTypes = new ArrayList<String> (resourceTypesSequence);
	}
	
	/**
	 * <p>Returns the {@link com.net2plan.interfaces.networkDesign.NetworkLayer NetworkLayer} object this element is attached to.</p>
	 * @return The NetworkLayer object
	 */
	public NetworkLayer getLayer () 
	{ 
		return layer; 
	}

	/**
	 * <p>If routing is {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, return {@code true} if more than one route is associated to this demand (routes down or with zero carried traffic also count).</p>
	 * <p>If routing is {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}, return {@code true} if a node sends traffic of the demand through more than one output link.</p>
	 * @return {@code true} if bifurcated, false otherwise
	 */
	public boolean isBifurcated ()
	{
		if (layer.routingType == RoutingType.SOURCE_ROUTING)
			return this.cache_routes.size () >= 2;
		final Set<Node> initialLinkNodes = new HashSet<> ();
		for (Link e : cacheHbH_normCarriedOccupiedPerLinkCurrentState.keySet())
			if (cacheHbH_normCarriedOccupiedPerLinkCurrentState.get(e).getFirst() > Configuration.precisionFactor)
			{
				if (initialLinkNodes.contains(e.getOriginNode())) return true; // two out links of this node carry traffic
				initialLinkNodes.add(e.getOriginNode());
			}
		return false;
	}

	/**
	 * <p>Returns {@code true} if the carried traffic is strictly less than the offered traffic, up to the Net2Plan-wide precision factor.</p>
	 * @return {@code true} if blocked, false otherwise
	 * @see com.net2plan.interfaces.networkDesign.Configuration
	 */
	public boolean isBlocked ()
	{
		return this.carriedTraffic + Configuration.precisionFactor < offeredTraffic;
	}

	/** Returns if the current demand reflects a service chain request, that is, if the routes are constrained 
	 * to traverse a given sequence of resources of given types.
	 * @return true if the demand defines a service chain request, false otherwise
	 */
	public boolean isServiceChainRequest ()
	{
		return !mandatorySequenceOfTraversedResourceTypes.isEmpty();
	}
	/**
	 * <p>Returns {@code true} if the demand is coupled to a link of an upper layer.</p>
	 * @return {@code true} is coupled, false otherwise
	 */
	public boolean isCoupled ()
	{
		return coupledUpperLayerLink != null;
	}

	/**
	 * Returns the offered traffic of the demand
	 * @return The offered traffic
	 */
	public double getOfferedTraffic()
	{
		return offeredTraffic;
	}

	/**
	 * <p>Returns the non zero forwarding rules as a map of pairs demand-link, and its associated splitting factor (between 0 and 1).</p>
	 * <p><b>Important: </b> If routing type is set to {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING} an exception will be thrown.
	 * @return a map with the forwarding rules. The map is empty if no non-zero forwarding rules are defined.
	 */
	public Map<Pair<Demand,Link>,Double> getForwardingRules ()
	{
		checkAttachedToNetPlanObject ();
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		Map<Pair<Demand,Link>,Double> res = new HashMap<Pair<Demand,Link>,Double> ();
		for (Entry<Link,Double> entry : cacheHbH_frs.entrySet())
			res.put(Pair.of(this, entry.getKey()), entry.getValue());
		return res;
	}

	
	/**
	 * <p>Returns the carried traffic of the demand.</p>
	 * @return The demand carried traffic
	 */
	public double getCarriedTraffic()
	{
		return carriedTraffic;
	}

	/**
	 * <p>Returns the blocked traffic of the demand (offered minus carried, or 0 if carried is above offered).</p>
	 * @return The demand blocked traffic
	 */
	public double getBlockedTraffic()
	{
		return Math.max (0 , offeredTraffic - carriedTraffic);
	}

	/**
	 * <p>Returns the routing cycle type of the demand, indicating if the traffic is routed in a loopless form, with open loops or with closed
	 * loops (the latter can only happen in {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING} routing).</p>
	 * @return The demand routing cycle type
	 * @see com.net2plan.utils.Constants.RoutingCycleType
	 */
	public RoutingCycleType getRoutingCycleType()
	{
		return routingCycleType;
	}


	/**
	 * <p>Returns the demand ingress node.</p>
	 * @return The ingress node
	 */
	public Node getIngressNode()
	{
		return ingressNode;
	}


	/**
	 * <p>Returns the demand egress node.</p>
	 * @return The egress node
	 */
	public Node getEgressNode()
	{
		return egressNode;
	}
	
	/**
	 * <p>If this demand was added using the method {@link com.net2plan.interfaces.networkDesign.NetPlan#addDemandBidirectional(Node, Node, double, Map, NetworkLayer...) addDemandBidirectional()},
	 * returns the demand in the other direction (if it was not previously removed). Returns null otherwise. </p>
	 * @return the demand in the other direction, if this demand was created with {@link com.net2plan.interfaces.networkDesign.NetPlan#addDemandBidirectional(Node, Node, double, Map, NetworkLayer...) addDemandBidirectional()}, or {@code null} otherwise
	 * @see com.net2plan.interfaces.networkDesign.NetPlan#addDemandBidirectional(Node, Node, double, Map, NetworkLayer...)
	 */
	public Demand getBidirectionalPair()
	{
		checkAttachedToNetPlanObject();
		return bidirectionalPair;
	}

	/**
	 * <p>Sets the given demand as the bidirectional pair of this demand. If any of the demands was previously set as bidirectional 
	 * link of other demand, such relation is removed. The demands must be in the same layer, and have opposite end nodes
	 * @param d the other demand
	 */
	public void setBidirectionalPair(Demand d)
	{
		checkAttachedToNetPlanObject();
		d.checkAttachedToNetPlanObject(this.netPlan);
		if (d.layer != this.layer) throw new Net2PlanException ("Wrong layer");
		if (d.ingressNode != this.egressNode || this.ingressNode != d.egressNode) throw new Net2PlanException ("Wrong end nodes");
		if (this.bidirectionalPair != null) this.bidirectionalPair.bidirectionalPair = null;
		if (d.bidirectionalPair != null) d.bidirectionalPair.bidirectionalPair = null;
		this.bidirectionalPair = d;
		d.bidirectionalPair = this;
	}

	/**
	 * Returns true if the demand is bidirectional. That is, it has an associated demand in the other direction
	 * @return see above
	 */
	public boolean isBidirectional()
	{
		checkAttachedToNetPlanObject();
		return bidirectionalPair != null;
	}

	/**
	 * Creates a demand in the opposite direction as this, and with the same attributes, and associate both as bidirectional pairs.
	 * If this demand is already bidirectional, makes nothing and returns null
	 * @return the newly created demand
	 */
	public Demand createBidirectionalPair ()
	{
		checkAttachedToNetPlanObject();
		if (this.isBidirectional()) return null;
		final Demand d = netPlan.addDemand(this.ingressNode, this.egressNode, this.getOfferedTraffic() , this.attributes, this.layer);
		this.bidirectionalPair = d;
		d.bidirectionalPair = this;
		return d;
	}

	/**
	 * <p>Returns the link this demand is coupled to.</p>
	 * @return the coupled link, or {@code null} if the demand is not coupled
	 */
	public Link getCoupledLink ()
	{
		checkAttachedToNetPlanObject();
		return coupledUpperLayerLink;
	}

	/**
	 * <p>Couples this demand to a link in an upper layer. After the link is coupled, its capacity will be always equal to the demand carried traffic.</p>
	 *
	 * <p><b>Important:</b> The link and the demand must belong to different layers, have same end nodes, not be already coupled,
	 * and the traffic units of the demand must be the same as the capacity units of the link. Also, the coupling of layers cannot create a loop.</p>
	 * @param link Link to be coupled
	 */
	public void coupleToUpperLayerLink (Link link)
	{
		netPlan.checkIsModifiable();
		checkAttachedToNetPlanObject();
		final NetworkLayer upperLayer = link.layer;
		final NetworkLayer lowerLayer = this.layer;
		
		if (!link.originNode.equals(this.ingressNode)) throw new Net2PlanException("Link and demand to couple must have the same end nodes");
		if (!link.destinationNode.equals(this.egressNode)) throw new Net2PlanException("Link and demand to couple must have the same end nodes");
		if (upperLayer.equals(lowerLayer)) throw new Net2PlanException("Bad - Trying to couple links and demands in the same layer");
		if (!upperLayer.linkCapacityUnitsName.equals(lowerLayer.demandTrafficUnitsName))
			throw new Net2PlanException(String.format(netPlan.TEMPLATE_CROSS_LAYER_UNITS_NOT_MATCHING, upperLayer.id, upperLayer.linkCapacityUnitsName, lowerLayer.id, lowerLayer.demandTrafficUnitsName));
		if ((link.coupledLowerLayerDemand!= null) || (link.coupledLowerLayerMulticastDemand != null)) throw new Net2PlanException("Link " + link + " at layer " + upperLayer.id + " is already associated to a lower layer demand");
		if (this.coupledUpperLayerLink != null) throw new Net2PlanException("Demand " + this.id + " at layer " + lowerLayer.id + " is already associated to an upper layer demand");
		
		DemandLinkMapping coupling_thisLayerPair = netPlan.interLayerCoupling.getEdge(lowerLayer, upperLayer);
		if (coupling_thisLayerPair == null)
		{
			coupling_thisLayerPair = new DemandLinkMapping();
			boolean valid;
			try { valid = netPlan.interLayerCoupling.addDagEdge(lowerLayer, upperLayer, coupling_thisLayerPair); }
			catch (DirectedAcyclicGraph.CycleFoundException ex) { valid = false; }
			if (!valid) throw new Net2PlanException("Coupling between link " + link + " at layer " + upperLayer + " and demand " + this.id + " at layer " + lowerLayer.id + " would induce a cycle between layers");
		}

		/* Link capacity at the upper layer is equal to the carried traffic at the lower layer */
		link.updateCapacityAndZeroCapacityLinksAndRoutesCaches(carriedTraffic);
		link.coupledLowerLayerDemand = this;
		this.coupledUpperLayerLink = link;
		link.layer.cache_coupledLinks.add (link);
		this.layer.cache_coupledDemands.add (this);
		coupling_thisLayerPair.put(this, link);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Creates a new link in the given layer with same end nodes, and couples it to this demand.</p>
	 * @param newLinkLayer Layer where the link will be created.
	 * @return The new created link
	 */
	public Link coupleToNewLinkCreated (NetworkLayer newLinkLayer)
	{
		netPlan.checkIsModifiable();
		checkAttachedToNetPlanObject();
		newLinkLayer.checkAttachedToNetPlanObject(this.netPlan);
		if (this.layer.equals (newLinkLayer)) throw new Net2PlanException ("Cannot couple a link and a demand in the same layer");
		if (isCoupled()) throw new Net2PlanException ("The demand is already coupled");
		
		Link newLink = null;
		try
		{
			newLink = netPlan.addLink(ingressNode , egressNode , carriedTraffic , netPlan.getNodePairEuclideanDistance(ingressNode , egressNode) , 200000 , null , newLinkLayer);
			coupleToUpperLayerLink(newLink);
		} catch (Exception e) { if (newLink != null) newLink.remove (); throw e; }
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		return newLink;
	}
	
	/**
	 * <p>Decouples this demand from an other layer link. Throws an exception if the demand is not currently coupled.</p>
	 */
	public void decouple()
	{
		netPlan.checkIsModifiable();
		checkAttachedToNetPlanObject();
		if (coupledUpperLayerLink == null) throw new Net2PlanException ("Demand is not coupled to a link");
		
		Link link = this.coupledUpperLayerLink;
		link.checkAttachedToNetPlanObject(netPlan);
		if (link.coupledLowerLayerDemand != this) throw new RuntimeException ("Bad");
		final NetworkLayer upperLayer = link.layer;
		final NetworkLayer lowerLayer = this.layer;

		link.coupledLowerLayerDemand = null;
		this.coupledUpperLayerLink = null;
		link.layer.cache_coupledLinks.remove (link);
		this.layer.cache_coupledDemands.remove(this);
		DemandLinkMapping coupling_thisLayerPair = netPlan.interLayerCoupling.getEdge(lowerLayer, upperLayer);
		coupling_thisLayerPair.remove(this);
		if (coupling_thisLayerPair.isEmpty()) netPlan.interLayerCoupling.removeEdge(lowerLayer , upperLayer);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();

	}
	
	/**
	 * <p>Removes all forwarding rules associated to the demand.</p>
	 * <p><b>Important: </b>If routing type is not {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING} and exception is thrown.</p>
	 */
	public void removeAllForwardingRules()
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		this.updateHopByHopRoutingToGivenFrs(new HashMap<> ());
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Returns the set of demand routes with shortest path (and its cost), using the cost per link array provided. If more than one shortest route exists, all of them are provided.
	 * If the cost vector provided is null, all links have cost one.</p>
	 * @param costs Costs for each link
	 * @return Pair where the first element is a set of routes (may be empty) and the second element the minimum cost (may be {@code Double.MAX_VALUE} if there is no shortest path)
	 */
	public Pair<Set<Route>,Double> computeShortestPathRoutes (double [] costs)
	{
		if (costs == null) costs = DoubleUtils.ones(layer.links.size ()); else if (costs.length != layer.links.size()) throw new Net2PlanException ("The array of costs must have the same length as the number of links in the layer");
		Set<Route> shortestRoutes = new HashSet<Route> ();
		double shortestPathCost = Double.MAX_VALUE;
		for (Route r : cache_routes)
		{
			double cost = 0; for (Link link : r.cache_seqLinksRealPath) cost += costs [link.index];
			if (cost < shortestPathCost) { shortestPathCost = cost; shortestRoutes.clear(); shortestRoutes.add (r); }
			else if (cost == shortestPathCost) { shortestRoutes.add (r); }
		}
		return Pair.of(shortestRoutes , shortestPathCost);
	}

	/**
	 * <p>Returns the set of demand service chains with shortest cost, using the cost per link and cost per resources arrays provided. 
	 * If more than one minimum cost route exists, all of them are provided.
	 * If the link cost vector provided is null, all links have cost one. The same for the resource costs array. 
	 * </p>
	 * @param linkCosts Costs for each link (indexed by link index)
	 * @param resourceCosts the costs of the resources (indexed by resource index)
	 * @return Pair where the first element is a set of routes (may be empty) and the second element the minimum cost (will be {@code Double.MAX_VALUE} if there is no shortest path)
	 */
	public Pair<Set<Route>,Double> computeMinimumCostServiceChains (double [] linkCosts , double [] resourceCosts)
	{
		if (linkCosts == null) linkCosts = DoubleUtils.ones(layer.links.size ()); else if (linkCosts.length != layer.links.size()) throw new Net2PlanException ("The array of costs must have the same length as the number of links in the layer");
		if (resourceCosts == null) resourceCosts = DoubleUtils.ones(netPlan.resources.size ()); else if (resourceCosts.length != netPlan.resources.size ()) throw new Net2PlanException ("The array of resources costs must have the same length as the number of resources");
		Set<Route> minCostRoutes = new HashSet<Route> ();
		double shortestPathCost = Double.MAX_VALUE;
		for (Route r : cache_routes)
		{
			double cost = 0; 
			for (NetworkElement e : r.currentPath) 
				if (e instanceof Link) cost += linkCosts [e.index];
				else if (e instanceof Resource) cost += resourceCosts [e.index];
				else throw new RuntimeException ("Bad");
			if (cost < shortestPathCost) { shortestPathCost = cost; minCostRoutes.clear(); minCostRoutes.add (r); }
			else if (cost == shortestPathCost) { minCostRoutes.add (r); }
		}
		return Pair.of(minCostRoutes , shortestPathCost);
	}

	/**
	 * <p>Removes a demand, and any associated routes or forwarding rules.</p>
	 */
	public void remove()
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (this.coupledUpperLayerLink != null) this.decouple();
		
		if (bidirectionalPair != null) this.bidirectionalPair.bidirectionalPair = null;

		if (layer.routingType == RoutingType.SOURCE_ROUTING)
			for (Route route : new HashSet<Route> (cache_routes)) route.remove();
		else
		{
			for (Link e : this.cacheHbH_frs.keySet()) e.cacheHbH_frs.remove(this);
			for (Link e : this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.keySet())
			{
				final double x_deOccup = this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.get(e).getSecond();
				e.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.remove(this);
				e.cache_carriedTraffic -= x_deOccup; 
				e.cache_occupiedCapacity -= x_deOccup; 
			}
		}
		layer.cache_nodePairDemandsThisLayer.get(Pair.of(ingressNode, egressNode)).remove(this);

        for (String tag : tags) netPlan.cache_taggedElements.get(tag).remove(this);
		netPlan.cache_id2DemandMap.remove(id);
		NetPlan.removeNetworkElementAndShiftIndexes (layer.demands , index);
		ingressNode.cache_nodeOutgoingDemands.remove (this);
		egressNode.cache_nodeIncomingDemands.remove (this);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		removeId();
	}
	
	/**
	 * <p>Sets the offered traffic by a demand. If routing type is {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}, the carried traffic for each link is updated according to the forwarding rules.
	 * In {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, the routes carried traffic is not updated. </p>
	 * @param offeredTraffic The new offered traffic (must be non-negative)
	 */
	public void setOfferedTraffic(double offeredTraffic)
	{
		offeredTraffic = NetPlan.adjustToTolerance(offeredTraffic);
		if (offeredTraffic == this.offeredTraffic) return;
		netPlan.checkIsModifiable();
		if (offeredTraffic < 0) throw new Net2PlanException("Offered traffic must be greater or equal than zero");
		this.offeredTraffic = offeredTraffic;
		if (!layer.isSourceRouting()) updateHopByHopRoutingToGivenFrs(this.cacheHbH_frs);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	
	/**
	 * <p>Returns a {@code String} representation of the demand.</p>
	 * @return {@code String} representation of the demand
	 */
	@Override
	public String toString () { return "d" + index + " (id " + id + ")"; }
	

	/**
	 * <p>Checks the consistency of the cache. If it is incosistent, throws a {@code RuntimeException} </p>
	 */
	void checkCachesConsistency ()
	{
		super.checkCachesConsistency ();
		if (this.bidirectionalPair != null)
		{
			if (this.bidirectionalPair.bidirectionalPair != this) throw new RuntimeException ("Bad");
			if (this.bidirectionalPair.netPlan != this.netPlan) throw new RuntimeException ("Bad");
			if (this.bidirectionalPair.ingressNode != this.egressNode) throw new RuntimeException ("Bad");
			if (this.bidirectionalPair.egressNode != this.ingressNode) throw new RuntimeException ("Bad");
			if (this.bidirectionalPair.layer != this.layer) throw new RuntimeException ("Bad");
		}
		if (!layer.isSourceRouting())
		{
			if (!cacheHbH_frs.keySet().containsAll(cacheHbH_normCarriedOccupiedPerLinkCurrentState.keySet())) throw new RuntimeException();
			
			for (Link e : this.cacheHbH_frs.keySet())
			{
				final double splitFactor = cacheHbH_frs.get(e);
				if (!this.cacheHbH_linksPerNodeWithNonZeroFr.get(e.getOriginNode()).contains(e)) throw new RuntimeException ();
				if (e.cacheHbH_frs.get(this) != splitFactor) throw new RuntimeException ();
				if (splitFactor <= Configuration.precisionFactor) throw new RuntimeException();
				if (splitFactor > 1)throw new RuntimeException();
			}
			for (Link e : this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.keySet())
			{
				final double normTraffic =  this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.get(e).getFirst();
				if (normTraffic < Configuration.precisionFactor) throw new RuntimeException();
				final double cap =  this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.get(e).getSecond();
				if (!e.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.get(this).equals(Pair.of(normTraffic, cap))) throw new RuntimeException ();
				if (Math.abs(normTraffic * offeredTraffic - cap) > 1e-3) throw new RuntimeException ();
			}
			for (Node n : this.cacheHbH_linksPerNodeWithNonZeroFr.keySet())
			{
				double sumFactors = 0;
				for (Link e : cacheHbH_linksPerNodeWithNonZeroFr.get(n))
				{
					if (this.cacheHbH_frs.get(e) == null) 
						throw new RuntimeException();
					sumFactors += cacheHbH_frs.get(e);
				}
				if (sumFactors > 1 + Configuration.precisionFactor) throw new RuntimeException();
			}
		}
		
		double check_carriedTraffic = 0;
		if (layer.routingType == RoutingType.SOURCE_ROUTING)
		{
			for (Route r : cache_routes)
			{
				if (!r.demand.equals (this)) throw new RuntimeException ("Bad");
				check_carriedTraffic += r.getCarriedTraffic();
			}
		}
		else
		{
			for (Pair<Double,Double> pair : this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.values())
				if (Math.abs(pair.getFirst() * this.offeredTraffic - pair.getSecond()) > 1e-3) throw new RuntimeException ();
			for (Link e : egressNode.getIncomingLinks(layer)) if (cacheHbH_normCarriedOccupiedPerLinkCurrentState.containsKey(e)) check_carriedTraffic += this.offeredTraffic * this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.get(e).getFirst ();
			for (Link e : egressNode.getOutgoingLinks(layer)) if (cacheHbH_normCarriedOccupiedPerLinkCurrentState.containsKey(e)) check_carriedTraffic -= this.offeredTraffic * this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.get(e).getFirst ();
		}
		if (Math.abs(carriedTraffic - check_carriedTraffic) > 1e-3) throw new RuntimeException ("Bad, carriedTraffic: " + carriedTraffic + ", check_carriedTraffic: " + check_carriedTraffic);
		if (coupledUpperLayerLink != null)
			if (!coupledUpperLayerLink.coupledLowerLayerDemand.equals (this)) throw new RuntimeException ("Bad");
		if (!layer.demands.contains(this)) throw new RuntimeException ("Bad");
		
		if ((routingCycleType != RoutingCycleType.LOOPLESS) && (!layer.isSourceRouting()))
		{
			if (cache_worstCasePropagationTimeMs != Double.MAX_VALUE) throw new RuntimeException ();
			if (cache_worstCaseLengthInKm != Double.MAX_VALUE) throw new RuntimeException ();
		}
	}


	/** Returns the set of links in this layer, with non zero forwarding rules defined for them. The link may or may not 
	 * carry traffic, and may be or not be failed.
	 * @return see above
	 */
	public Set<Link> getLinksWithNonZeroForwardingRules ()
	{
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		return this.cacheHbH_frs.keySet().stream().filter(e->cacheHbH_frs.get(e) != 0).collect(Collectors.toSet());
	}
	
	/** Returns the set of links in this layer that could potentially carry traffic of this demand, according to the routes/forwarding rules defined, 
	 * if the routes had carried traffic / (hop-by-hop) the demand had offered traffic different to zero.
	 * The method returns  a pair of sets (disjoint or not), first set with the set of links potentially carrying primary traffic and 
	 * second with links in backup routes. Potentially carrying traffic means that 
	 * (i) in source routing, down routes are not included, but all up routes are considered even if the carry zero traffic, 
	 * (ii) in hop-by-hop routing the links are computed even if the demand offered traffic is zero, 
	 * and all the links are considered primary. Naturally, forwarding rules of failed links apply as zero
	 * @return see above
	 */
	public Pair<Set<Link>,Set<Link>> getLinksThisLayerPotentiallyCarryingTraffic  ()
	{
		final Set<Link> resPrimary = new HashSet<> ();
		final Set<Link> resBackup = new HashSet<> ();
		if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING)
		{
			for (Entry<Link,Pair<Double,Double>> entry : this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.entrySet())
				if (entry.getValue().getFirst() > Configuration.precisionFactor)
					resPrimary.add(entry.getKey());
		}
		else
		{
			for (Route r : cache_routes)
			{
				if (r.isDown()) continue;
				for (Link e : r.getSeqLinks()) 
					if (r.isBackupRoute()) resBackup.add(e); else resPrimary.add(e);
			}
		}
		return Pair.of(resPrimary,resBackup);
	}
	
	/* Updates all the network state, to the new situation where the hop-by-hop routing of a demand has changed */
	void updateHopByHopRoutingToGivenFrs (Map<Link,Double> newFrsWithoutZeros)
	{
		final Set<Link> affectedLinks = Sets.union(newFrsWithoutZeros.keySet() , cacheHbH_frs.keySet());
		
		/* set 0 in the down links and the link in-out from the down nodes (they do not send traffic) */
		/* update the cache per node (include failed links if fr > 0) */
		Map<Link,Double> frsToApply = new HashMap<> ();
		Map<Node,Set<Link>> tentativeCacheHbH_linksPerNodeWithNonZeroFr = new HashMap<> (); // tentative since if closed cycles => not used
		for (Entry<Link,Double> fr : newFrsWithoutZeros.entrySet())
		{
			final Link e = fr.getKey();
			final Node a_e = e.getOriginNode();
			final double f_e = fr.getValue();
			if (f_e == 0) continue;
			Set<Link> set = tentativeCacheHbH_linksPerNodeWithNonZeroFr.get(a_e); if (set == null) { set = new HashSet<> (); tentativeCacheHbH_linksPerNodeWithNonZeroFr.put(a_e, set); }
			set.add(e);
			if (e.isDown() || e.getOriginNode().isDown() || e.getDestinationNode().isDown()) continue;
			frsToApply.put(e, f_e);
		}
		
		Quintuple<DoubleMatrix1D, RoutingCycleType , Double , Double , Double> fundMatrixComputation = 
				GraphUtils.computeRoutingFundamentalVector(frsToApply, tentativeCacheHbH_linksPerNodeWithNonZeroFr , ingressNode ,  egressNode);
		if (fundMatrixComputation.getSecond() == RoutingCycleType.CLOSED_CYCLES) 
			throw new ClosedCycleRoutingException("Closed routing cycle for demand " + this); 
		DoubleMatrix1D M = fundMatrixComputation.getFirst ();
		this.routingCycleType = fundMatrixComputation.getSecond();
		double s_egressNode = fundMatrixComputation.getThird();
		this.cache_worstCasePropagationTimeMs = fundMatrixComputation.getFourth();
		this.cache_worstCaseLengthInKm = fundMatrixComputation.getFifth();

		/* update different caches */
//		System.out.println(offeredTraffic);
//		System.out.println(M.get(egressNode.index));
//		System.out.println(M);
//		System.out.println("s_egress: "  + s_egressNode);
		this.cacheHbH_linksPerNodeWithNonZeroFr = tentativeCacheHbH_linksPerNodeWithNonZeroFr;
		carriedTraffic = offeredTraffic * M.get(egressNode.index) * s_egressNode;
		if (coupledUpperLayerLink != null)
			coupledUpperLayerLink.updateCapacityAndZeroCapacityLinksAndRoutesCaches(carriedTraffic);

		if (carriedTraffic > offeredTraffic + 1E-5) throw new RuntimeException ("Bad");
		
		/* update the xde caches (link and demand), and the link occupations */
		for (Link link : affectedLinks)
		{
			Double new_fde = frsToApply.get(link); if (new_fde == null) new_fde = 0.0;
			final Pair<Double,Double> oldOccupInfo = cacheHbH_normCarriedOccupiedPerLinkCurrentState.get(link);
			final double oldXdeOccup = oldOccupInfo == null? 0 : oldOccupInfo.getSecond(); //layer.forwardingRulesCurrentFailureState_x_de.get (demand.index , link.index);
			final double newXdeNormalized = M.get (link.originNode.index) * new_fde; //fowardingRulesThisFailureState_f_e.get (link.index);
			final double newXdeOccup = offeredTraffic * newXdeNormalized; //fowardingRulesThisFailureState_f_e.get (link.index);
			if (newXdeNormalized < -1E-5) throw new RuntimeException ("Bad");
			//System.out.println("Demand " + this + ", link " + link + ", xdeNorm: " + newXdeNormalized + ", newXdeOccup: " + newXdeOccup);
			if (newXdeNormalized <= Configuration.precisionFactor)
			{
				cacheHbH_normCarriedOccupiedPerLinkCurrentState.remove(link);
				link.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.remove(this);
			}
			else
			{
				cacheHbH_normCarriedOccupiedPerLinkCurrentState.put(link, Pair.of(newXdeNormalized, newXdeOccup));
				link.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.put(this, Pair.of(newXdeNormalized, newXdeOccup));
			}
			link.cache_carriedTraffic += newXdeOccup - oldXdeOccup; // in hop-by-hop carried traffic is the same as occupied capacity
			link.cache_occupiedCapacity += newXdeOccup - oldXdeOccup;
			if ((newXdeNormalized > 1e-3) && (!link.isUp)) throw new RuntimeException ("Bad");
		}
		
		/* update the cache_frs in the link and demand */
		for (Link e : this.cacheHbH_frs.keySet())
			e.cacheHbH_frs.remove(this);
		this.cacheHbH_frs = new HashMap<> (newFrsWithoutZeros);
		for (Entry<Link,Double> fr : this.cacheHbH_frs.entrySet())
			fr.getKey().cacheHbH_frs.put(this , fr.getValue());
		
		/* update the routing cycle type */
//		System.out.println ("updateHopByHopRoutingDemand demand: " + demand + ", this demand x_e: " + forwardingRules_x_de.viewRow(demand.index));
	}



}
