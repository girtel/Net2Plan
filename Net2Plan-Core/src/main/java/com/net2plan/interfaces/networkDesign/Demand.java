//CACHES:
//	- WORST PROPAGATION CACHE
//	- FORW RULES PER NODE & DEMAND
//	- 


/*******************************************************************************


 * 
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

package com.net2plan.interfaces.networkDesign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.libraries.GraphUtils.ClosedCycleRoutingException;
import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

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
	
	
	Set<Route> cache_routes;
	Link coupledUpperLayerLink;
	List<String> mandatorySequenceOfTraversedResourceTypes;
	IntendedRecoveryType recoveryType;

	double cache_worstCasePropagationTimeMs;
	Map<Link,Triple<Double,Double,Double>> cache_frOptionalNormCarriedOccupiedPerLinkCurrentState; // cannot be an entry if zero in FR
	Map<Node,Set<Link>> cache_linksPerNodeWithNonZeroFr; 
	
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
		this.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState = new HashMap<> ();
		this.cache_linksPerNodeWithNonZeroFr = new HashMap<> ();
		this.cache_worstCasePropagationTimeMs = 0;
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
		this.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.clear();
		for (Link originLink : origin.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.keySet())
			this.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.put(netPlan.getLinkFromId(originLink.id), origin.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.get(originLink));
		this.cache_linksPerNodeWithNonZeroFr.clear();
		for (Entry<Node,Set<Link>> entry : origin.cache_linksPerNodeWithNonZeroFr.entrySet())
			this.cache_linksPerNodeWithNonZeroFr.put(netPlan.getNodeFromId(entry.getKey().id), (Set<Link>) (Set<?>) netPlan.translateCollectionToThisNetPlan(entry.getValue()));
	}


	boolean isDeepCopy (Demand e2)
	{
		if (!super.isDeepCopy(e2)) return false;
		if (layer.id != e2.layer.id) return false;
		if (ingressNode.id != e2.ingressNode.id) return false;
		if (egressNode.id != e2.egressNode.id) return false;
		if (this.offeredTraffic != e2.offeredTraffic) return false;
		if (this.carriedTraffic != e2.carriedTraffic) return false;
		if (this.cache_worstCasePropagationTimeMs != e2.cache_worstCasePropagationTimeMs) return false;
		if (this.routingCycleType != e2.routingCycleType) return false;
		if ((this.coupledUpperLayerLink == null) != (e2.coupledUpperLayerLink == null)) return false; 
		if ((this.coupledUpperLayerLink != null) && (coupledUpperLayerLink.id != e2.coupledUpperLayerLink.id)) return false;
		if (!NetPlan.isDeepCopy(this.cache_routes , e2.cache_routes)) return false;
		if (!this.mandatorySequenceOfTraversedResourceTypes.equals(e2.mandatorySequenceOfTraversedResourceTypes)) return false;
		if (this.recoveryType != e2.recoveryType) return false;
		if (!NetPlan.isDeepCopy(this.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState , e2.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState)) return false;
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

	private double recursiveWCPropapagationDelay ()
	{
		final Map<Node,Double> inNodes = new HashMap<> ();
		inNodes.put(this.ingressNode, 0.0);
		do
		{
			for (Node n : new ArrayList<> (inNodes.keySet()))
			{
				if (n == egressNode) continue;
				double wcSoFar = inNodes.get(n);
				for (Link e : cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.keySet())
					if (e.getOriginNode() == n)
						if (cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.get(e).getFirst() > 0)
						{
							final double thisPathCost = wcSoFar + e.getPropagationDelayInMs();
							if (inNodes.containsKey(e.getDestinationNode()))
								inNodes.put(e.getDestinationNode(), Math.max(inNodes.get(e.getDestinationNode()) , thisPathCost));
							else
								inNodes.put(e.getDestinationNode(), thisPathCost);
						}
				inNodes.remove(n);
			}
			
			if (inNodes.isEmpty()) return Double.MAX_VALUE;
			if ((inNodes.size() == 1) && (inNodes.keySet().iterator().next() == this.egressNode)) return inNodes.get(egressNode);
		} while (true);
	}

	/**
	 * <p>Returns {@code true} if the traffic of the demand is traversing an oversubscribed link, {@code false} otherwise.</p>
	 * @return {@code true} if the traffic is traversing an oversubscribed link, {@code false} otherwise
	 */
	public boolean isTraversingOversubscribedLinks ()
	{
		for (Link e : cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.keySet())
			if (cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.get(e).getSecond() > Configuration.precisionFactor)
				if (e.isOversubscribed()) return true;
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
		for (Link e : cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.keySet())
			if (cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.get(e).getSecond() > Configuration.precisionFactor)
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
		for (Entry<Link,Triple<Double,Double,Double>> entry : cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.entrySet())
			res.put(Pair.of(this, entry.getKey()), entry.getValue().getFirst());
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
		return (Demand) netPlan.getNetworkElementByAttribute(layer.demands , netPlan.KEY_STRING_BIDIRECTIONALCOUPLE, "" + this.id);
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
		link.checkAttachedToNetPlanObject(this.netPlan);
		
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
		link.capacity = carriedTraffic;
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
		for (Link e : this.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.keySet())
			e.cache_frOptionalCarriedOccupiedPerTraversingDemandCurrentState.remove(this);
		this.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.clear();
		this.cache_linksPerNodeWithNonZeroFr.clear();
		this.updateHopByHopRouting();
//		layer.forwardingRulesNoFailureState_f_de.viewRow (this.index).assign(0);
//		layer.updateHopByHopRoutingDemand(this);
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
		
		if (layer.routingType == RoutingType.SOURCE_ROUTING)
			for (Route route : new HashSet<Route> (cache_routes)) route.remove();
		else
		{
			for (Link travLink : this.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.keySet())
			{
				final double x_de = this.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.get(travLink).getSecond();
				travLink.cache_frOptionalCarriedOccupiedPerTraversingDemandCurrentState.remove(this);
				travLink.cache_carriedTraffic -= x_de; 
				travLink.cache_occupiedCapacity -= x_de; 
			}
//			
//			layer.forwardingRulesNoFailureState_f_de = DoubleFactory2D.sparse.appendRows(layer.forwardingRulesNoFailureState_f_de.viewPart(0, 0, index, E), layer.forwardingRulesNoFailureState_f_de.viewPart(index + 1, 0, layer.demands.size() - index - 1, E));
//			DoubleMatrix1D x_e = layer.forwardingRulesCurrentFailureState_x_de.viewRow (index).copy ();
//			layer.forwardingRulesCurrentFailureState_x_de = DoubleFactory2D.sparse.appendRows(layer.forwardingRulesCurrentFailureState_x_de.viewPart(0, 0, index, E), layer.forwardingRulesCurrentFailureState_x_de.viewPart(index + 1, 0, layer.demands.size() - index - 1, E));
//			for (Link link : layer.links) { link.cache_carriedTraffic -= x_e.get(link.index); link.cache_occupiedCapacity -= x_e.get(link.index); }
		}
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
		if (!layer.isSourceRouting()) updateHopByHopRouting();
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	
	/**
	 * <p>Returns a {@code String} representation of the demand.</p>
	 * @return {@code String} representation of the demand
	 */
	@Override
	public String toString () { return "d" + index + " (id " + id + ")"; }
	

//	/**
//	 * <p>Computes the fundamental matrix of the absorbing Markov chain in the current hop-by-hop routing, for the
//	 * given demand.</p>
//	 * <p>Returns a {@link com.net2plan.utils.Quadruple Quadruple} object where:</p>
//	 * <ol type="i">
//	 *     <li>the fundamental matrix (NxN, where N is the number of nodes)</li>
//	 *     <li>the type of routing of the demand (loopless, with open cycles or with close cycles)</li>
//	 *     <li>the fraction of demand traffic that arrives to the destination node and is absorbed there (it may be less than one if the routing has cycles that involve the destination node)</li>
//	 *     <li>an array with fraction of the traffic that arrives to each node, that is dropped (this happens only in nodes different to the destination, when the sum of the forwarding percentages on the outupt links is less than one)</li>
//	 * </ol>
//	 * <p><b>Important: </b>If the routing type is not {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}, an exception is thrown.</p>
//	 * @param forwardingRulesToUse_f_e if null, the current forwarding rules in the no failure state are used. If not null, this row defines the forwarding rules to apply in the matrix computation
//	 * @return See description above
//	 */
//	public Triple<DoubleMatrix1D, RoutingCycleType,  Double> computeRoutingFundamentalVectorDemand(Map<Link,Double> frs)
//	{
//		DoubleMatrix1D f_e = forwardingRulesToUse_f_e == null? layer.forwardingRulesNoFailureState_f_de.viewRow(index) : forwardingRulesToUse_f_e;
//		return layer.computeRoutingFundamentalVector(f_e, this.ingressNode, this.egressNode);
//	}

	
	/**
	 * <p>Checks the consistency of the cache. If it is incosistent, throws a {@code RuntimeException} </p>
	 */
	void checkCachesConsistency ()
	{
		super.checkCachesConsistency ();
		for (Entry<Link,Triple<Double,Double,Double>> entry : this.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.entrySet())
		{
			final Link e = entry.getKey();
			if (!e.cache_frOptionalCarriedOccupiedPerTraversingDemandCurrentState.get(this).equals(entry.getValue())) throw new RuntimeException ();
			if (!this.cache_linksPerNodeWithNonZeroFr.get(e.getOriginNode()).contains(e)) throw new RuntimeException ();
			final double splitFactor = this.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.get(e).getFirst();
			final double traffic =  this.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.get(e).getSecond();
			final double cap =  this.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.get(e).getThird();
			if (splitFactor <= 0) throw new RuntimeException();
			if (splitFactor > 1)throw new RuntimeException();
		}
		for (Node n : this.cache_linksPerNodeWithNonZeroFr.keySet())
			for (Link e : cache_linksPerNodeWithNonZeroFr.get(n))
				if (this.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.get(e) == null) 
					throw new RuntimeException();
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
			for (Link e : egressNode.getIncomingLinks(layer)) check_carriedTraffic += this.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.get(e).getSecond ();
			for (Link e : egressNode.getOutgoingLinks(layer)) check_carriedTraffic -= this.cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.get(e).getSecond ();
		}
		if (Math.abs(carriedTraffic - check_carriedTraffic) > 1e-3) throw new RuntimeException ("Bad, carriedTraffic: " + carriedTraffic + ", check_carriedTraffic: " + check_carriedTraffic);
		if (coupledUpperLayerLink != null)
			if (!coupledUpperLayerLink.coupledLowerLayerDemand.equals (this)) throw new RuntimeException ("Bad");
		if (!layer.demands.contains(this)) throw new RuntimeException ("Bad");
	}

	/** Returns the set of links in this layer that could potentially carry traffic of this demand, according to the routes/forwarding rules defined.
	 * The method returns  a pair of sets (disjoint or not), first set with the set of links potentially carrying primary traffic and 
	 * second with links in backup routes. Potentially carrying traffic means that 
	 * (i) in source routing, down routes are not included, but all up routes are considered even if the carry zero traffic, 
	 * (ii) in hop-by-hop routing the links are computed even if the demand offered traffic is zero, and all the links are considered primary.
	 * @param assumeNoFailureState in this case, the links are computed as if all network link/nodes are in no-failure state
	 * capacity in it
	 * @return see above
	 */
	public Pair<Set<Link>,Set<Link>> getLinksThisLayerPotentiallyCarryingTraffic  (boolean assumeNoFailureState)
	{
		final double tolerance = Configuration.precisionFactor;
		Set<Link> resPrimary = new HashSet<> ();
		Set<Link> resBackup = new HashSet<> ();
		if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING)
		{
			final boolean someLinksFailed = !layer.cache_linksDown.isEmpty() || !netPlan.cache_nodesDown.isEmpty();
			DoubleMatrix1D x_e = null;
			if (someLinksFailed)
			{
				DoubleMatrix1D f_e = layer.forwardingRulesNoFailureState_f_de.viewRow(index).copy();
				if (!assumeNoFailureState)
				{
					for (Link e : layer.cache_linksDown) f_e.set(e.index, 0);
					for (Node n : netPlan.cache_nodesDown)
					{
						for (Link e : n.getOutgoingLinks(layer)) f_e.set(e.index, 0);
						for (Link e : n.getIncomingLinks(layer)) f_e.set(e.index, 0);
					}
				}
				Triple<DoubleMatrix1D, RoutingCycleType , Double> fundMatrixComputation = GraphUtils.computeRoutingFundamentalVector(f_e);
				DoubleMatrix2D M = fundMatrixComputation.getFirst ();
				x_e = DoubleFactory1D.dense.make(layer.links.size());
				for (Link link : layer.links)
				{
					final double newXdeTrafficOneUnit = M.get (ingressNode.index , link.originNode.index) * f_e.get (link.index);
					x_e.set(link.index , newXdeTrafficOneUnit);
				}			
			}
			else
			{
				x_e = layer.forwardingRulesCurrentFailureState_x_de.viewRow(getIndex()); 
			}
			for (int e = 0 ; e < x_e.size() ; e ++) if (x_e.get(e) > tolerance) resPrimary.add(layer.links.get(e));
		}
		else
		{
			for (Route r : cache_routes)
			{
				if (!assumeNoFailureState && r.isDown()) continue;
				for (Link e : r.getSeqLinks()) 
					if (r.isBackupRoute()) resBackup.add(e); else resPrimary.add(e);
			}
		}
		return Pair.of(resPrimary,resBackup);
	}
	

	/* Updates all the network state, to the new situation where the hop-by-hop routing of a demand has changed */
	void updateHopByHopRouting ()
	{
		/* set 0 in the down links and the link in-out from the down nodes (they do not send traffic) */
		Map<Link,Double> frs = new HashMap<> ();
		for (Entry<Link,Triple<Double,Double,Double>> existingFr : cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.entrySet())
		{
			final Link e = existingFr.getKey();
			if (e.isDown() || e.getOriginNode().isDown() || e.getDestinationNode().isDown()) continue;
			frs.put(e, existingFr.getValue().getFirst());
		}
		Triple<DoubleMatrix1D, RoutingCycleType , Double> fundMatrixComputation = GraphUtils.computeRoutingFundamentalVector(frs, ingressNode ,  egressNode);
		DoubleMatrix1D M = fundMatrixComputation.getFirst ();
		double s_egressNode = fundMatrixComputation.getThird();

		/* update the demand routing cycle information */
		routingCycleType = fundMatrixComputation.getSecond();
		if (routingCycleType == RoutingCycleType.CLOSED_CYCLES) 
			throw new ClosedCycleRoutingException("Closed routing cycle for demand " + this); 
		
		/* update the demand carried traffic */
		carriedTraffic = offeredTraffic * M.get(egressNode.index) * s_egressNode;
		if (coupledUpperLayerLink != null) coupledUpperLayerLink.capacity = carriedTraffic;

		if (carriedTraffic > offeredTraffic + 1E-5) throw new RuntimeException ("Bad");
		
		/* update the carried traffic of this demand in each link */
		for (Entry<Link,Double> linkInfo : frs.entrySet())
		{
			final Link link = linkInfo.getKey();
			final double splitFactor = linkInfo.getValue();
			final Triple<Double,Double,Double> currentOccupInfo = cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.get(link);
			final double oldXde = currentOccupInfo == null? 0 : currentOccupInfo.getSecond(); //layer.forwardingRulesCurrentFailureState_x_de.get (demand.index , link.index);
			final double newXde = offeredTraffic * M.get (link.originNode.index) * frs.get(link); //fowardingRulesThisFailureState_f_e.get (link.index);
			if (newXde < -1E-5) throw new RuntimeException ("Bad");
			cache_frOptionalNormCarriedOccupiedPerLinkCurrentState.put(link, Triple.of(splitFactor, newXde, newXde));
			link.cache_frOptionalCarriedOccupiedPerTraversingDemandCurrentState.put(this, Triple.of(splitFactor, newXde, newXde));
//			layer.forwardingRulesCurrentFailureState_x_de.set (demand.index , link.index , newXde);
			link.cache_carriedTraffic += newXde - oldXde; // in hop-by-hop carried traffic is the same as occupied capacity
			link.cache_occupiedCapacity += newXde - oldXde;
			if ((newXde > 1e-3) && (!link.isUp)) throw new RuntimeException ("Bad");
		}

		if (routingCycleType == RoutingCycleType.LOOPLESS)
			this.cache_worstCasePropagationTimeMs = GraphUtils.computeWorstCasePropagationDelayMsForLoopLess(frs, this.cache_linksPerNodeWithNonZeroFr, ingressNode, egressNode);
		else
			this.cache_worstCasePropagationTimeMs = Double.MAX_VALUE;
		
//		System.out.println ("updateHopByHopRoutingDemand demand: " + demand + ", this demand x_e: " + forwardingRules_x_de.viewRow(demand.index));
	}


}
