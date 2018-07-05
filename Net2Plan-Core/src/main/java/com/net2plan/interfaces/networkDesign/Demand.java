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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import com.google.common.collect.Sets;
import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.libraries.TrafficSeries;
import com.net2plan.libraries.GraphUtils.ClosedCycleRoutingException;
import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Quintuple;

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
	Demand bidirectionalPair;
	RoutingType routingType;
	double maximumAcceptableE2EWorstCaseLatencyInMs;
	double offeredTrafficGrowthFactorPerPeriodZeroIsNoGrowth;
	String qosType;
	private TrafficSeries monitoredOrForecastedTraffics;
	
	SortedSet<Route> cache_routes;
	Link coupledUpperOrSameLayerLink;
	List<String> mandatorySequenceOfTraversedResourceTypes;
	IntendedRecoveryType recoveryType;

	double cache_worstCasePropagationTimeMs;
	double cache_worstCaseLengthInKm;
	SortedMap<Link,Double> cacheHbH_frs; // cannot be an entry if zero in FR
	SortedMap<Link,Pair<Double,Double>> cacheHbH_normCarriedOccupiedPerLinkCurrentState; // norm carried is respect to demand total CARRIED traffic, occupied capacity is absolute
	SortedMap<Node,SortedSet<Link>> cacheHbH_linksPerNodeWithNonZeroFr; 
	
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
	Demand (NetPlan netPlan , long id , int index , NetworkLayer layer , Node ingressNode , Node egressNode , double offeredTraffic , RoutingType routingType , AttributeMap attributes)
	{
		super (netPlan , id , index , attributes);
		this.setName ("Demand-" + index);
		
		if (ingressNode.equals (egressNode)) throw new Net2PlanException("Self-demands are not allowed");
		if (offeredTraffic < 0) throw new Net2PlanException("Offered traffic must be non-negative");
		
		this.layer = layer;
		this.ingressNode = ingressNode;
		this.egressNode = egressNode;
		this.offeredTraffic = offeredTraffic;
		this.carriedTraffic = 0;
		this.routingType = routingType;
		this.routingCycleType = RoutingCycleType.LOOPLESS;
		this.cache_routes = new TreeSet<Route> ();
		this.coupledUpperOrSameLayerLink = null;
		this.mandatorySequenceOfTraversedResourceTypes = new ArrayList<String> ();
		this.recoveryType = IntendedRecoveryType.NOTSPECIFIED;
		this.cacheHbH_frs = new TreeMap<> ();
		this.cacheHbH_normCarriedOccupiedPerLinkCurrentState = new TreeMap<> ();
		this.cacheHbH_linksPerNodeWithNonZeroFr = new TreeMap<> ();
		this.cache_worstCasePropagationTimeMs = 0;
		this.cache_worstCaseLengthInKm = 0;
		this.bidirectionalPair = null;
		this.maximumAcceptableE2EWorstCaseLatencyInMs = -1;
		this.offeredTrafficGrowthFactorPerPeriodZeroIsNoGrowth = 0;
		this.qosType = null;
		final boolean previousDebug = ErrorHandling.DEBUG;
		ErrorHandling.DEBUG = false;
		this.setQoSType("");
		this.monitoredOrForecastedTraffics = new TrafficSeries ();
		ErrorHandling.DEBUG = previousDebug;
//		Pair<SortedSet<Demand>,SortedSet<MulticastDemand>> demandsEmptyType = layer.cache_qosTypes2DemandMap.get("");
//		if (demandsEmptyType == null) { demandsEmptyType = Pair.of(new TreeSet<>(),new TreeSet<>()); layer.cache_qosTypes2DemandMap.put("", demandsEmptyType); }
//		demandsEmptyType.getFirst().add(this);
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
		this.routingType = origin.routingType;
		this.routingCycleType = origin.routingCycleType;
		this.coupledUpperOrSameLayerLink = origin.coupledUpperOrSameLayerLink == null? null : this.netPlan.getLinkFromId (origin.coupledUpperOrSameLayerLink.id);
		this.cache_routes = new TreeSet<Route> ();
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
		for (Entry<Node,SortedSet<Link>> entry : origin.cacheHbH_linksPerNodeWithNonZeroFr.entrySet())
			this.cacheHbH_linksPerNodeWithNonZeroFr.put(netPlan.getNodeFromId(entry.getKey().id), (SortedSet<Link>) (SortedSet<?>) netPlan.translateCollectionToThisNetPlan(entry.getValue()));
		this.bidirectionalPair = origin.bidirectionalPair == null? null : netPlan.getDemandFromId(origin.bidirectionalPair.getId());
		this.maximumAcceptableE2EWorstCaseLatencyInMs = origin.maximumAcceptableE2EWorstCaseLatencyInMs;
		this.offeredTrafficGrowthFactorPerPeriodZeroIsNoGrowth = origin.offeredTrafficGrowthFactorPerPeriodZeroIsNoGrowth;
		this.qosType = origin.qosType;
		this.monitoredOrForecastedTraffics = origin.monitoredOrForecastedTraffics;
	}

	/** Returns a non-modifiable set of two elements, with the demand end nodes
	 * @return see above
	 */
	public SortedSet<Node> getEndNodes () { return new TreeSet<> (Arrays.asList(ingressNode , egressNode)); }

	boolean isDeepCopy (Demand e2)
	{
		if (!super.isDeepCopy(e2)) return false;
		if (layer.id != e2.layer.id) return false;
		if (ingressNode.id != e2.ingressNode.id) return false;
		if (egressNode.id != e2.egressNode.id) return false;
		if (offeredTrafficGrowthFactorPerPeriodZeroIsNoGrowth != e2.offeredTrafficGrowthFactorPerPeriodZeroIsNoGrowth) return false;
		if ((this.bidirectionalPair == null) != (e2.bidirectionalPair == null)) return false;
		if (this.bidirectionalPair != null) if (this.bidirectionalPair.id != e2.bidirectionalPair.id) return false;
		if (this.offeredTraffic != e2.offeredTraffic) return false;
		if (this.carriedTraffic != e2.carriedTraffic) return false;
		if (!this.qosType.equals(e2.qosType)) return false;
		if (this.cache_worstCasePropagationTimeMs != e2.cache_worstCasePropagationTimeMs) return false;
		if (this.cache_worstCaseLengthInKm != e2.cache_worstCaseLengthInKm) return false;
		if (this.routingType != e2.routingType) return false;
		if (this.routingCycleType != e2.routingCycleType) return false;
		if ((this.coupledUpperOrSameLayerLink == null) != (e2.coupledUpperOrSameLayerLink == null)) return false; 
		if ((this.coupledUpperOrSameLayerLink != null) && (coupledUpperOrSameLayerLink.id != e2.coupledUpperOrSameLayerLink.id)) return false;
		if (!NetPlan.isDeepCopy(this.cache_routes , e2.cache_routes)) return false;
		if (!this.mandatorySequenceOfTraversedResourceTypes.equals(e2.mandatorySequenceOfTraversedResourceTypes)) return false;
		if (this.recoveryType != e2.recoveryType) return false;
		if (!NetPlan.isDeepCopy(this.cacheHbH_frs , e2.cacheHbH_frs)) return false;
		if (!NetPlan.isDeepCopy(this.cacheHbH_normCarriedOccupiedPerLinkCurrentState , e2.cacheHbH_normCarriedOccupiedPerLinkCurrentState)) return false;
		if (!this.monitoredOrForecastedTraffics.equals(e2.monitoredOrForecastedTraffics)) return false;
		return true;
	}

	/** Sets the growth factor of the offered traffic in subsequent periods. 
	 * Offered traffic in period t+1, is the offered traffic in period t, multiplied by (1+growthFactor)
	 * @param growthFactor see above
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

	/** Sets the QoS type of the demand. Any previous type is removed. 
	 * @param newQosType  see above
	 */
	public void setQoSType (String newQosType)
	{
		if (newQosType == null) throw new Net2PlanException ("Wrong value");
		if (this.qosType != null)
		{
			final Pair<SortedSet<Demand>,SortedSet<MulticastDemand>> demandsOldType = layer.cache_qosTypes2DemandMap.get (this.qosType);
			assert demandsOldType.getFirst().contains(this);
			demandsOldType.getFirst().remove(this);
			if (demandsOldType.getFirst().isEmpty() && demandsOldType.getSecond().isEmpty()) 
				layer.cache_qosTypes2DemandMap.remove(this.qosType);
		}
		
		Pair<SortedSet<Demand>,SortedSet<MulticastDemand>> demandsNewType = layer.cache_qosTypes2DemandMap.get(newQosType);
		if (demandsNewType == null) { demandsNewType = Pair.of(new TreeSet<> (),new TreeSet<> ()); layer.cache_qosTypes2DemandMap.put(newQosType, demandsNewType); }
		demandsNewType.getFirst().add(this);
		this.qosType = newQosType;
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/** Return the QoS type of the demand 
	 * @return see above
	 */
	public String getQosType ()
	{
		return this.qosType;
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
	
	/** Returns true if the routing type in this demand is of the type source routing
	 * @return see above
	 */
	public boolean isSourceRouting () { return routingType == RoutingType.SOURCE_ROUTING; }
	
	/**
	 * Checks whether routing type is the expected one. When negative, an exception will be thrown.
	 * @param routingType Expected {@link com.net2plan.utils.Constants.RoutingType RoutingType}
	 */
	public void checkRoutingType(RoutingType routingType)
	{
		if (this.routingType != routingType) throw new Net2PlanException("Routing type of demand " + this + " must be " + routingType);
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
	public SortedSet<Route> getRoutes()
	{
		checkRoutingType(RoutingType.SOURCE_ROUTING);
		return Collections.unmodifiableSortedSet(cache_routes);
	}

    /**
     * <p>Sets the routing type at the given layer. If there is some previous routing information, it
     * will be converted to the new type. If no layer is provided, default layer is assumed. In the conversion from
     * HOP-BY-HOP to SOURCE-ROUTING: (i) the demands with open loops are routed so these loops are removed, and the resulting
     * routing consumes the same or less bandwidth in the demand traversed links, (ii) the demands with closed loops are
     * routed so that the traffic that enters the closed loops is not carried. These modifications are done since open or close
     * loops would require routes with an infinite number of links to be fairly represented.</p>
     * <p>In the conversion to HOP-BY-HOP: An exception is thrown if a demand is service chain demand (since
     * this information would be lost in the hop-by-bop representation). Also, in this conversion, some information
     * can be lost (so a conversion back to SOURCE ROUTING will not produce the original network):
     * the conversion uses the route carried traffics, and discards the information of the the
     * routes occupied capacities in the links. A conversion back will put all the occupied capacities of the routes,
     * equal to the carried traffics (so the original occupied links capacities would be lost, if different).</p>
     *
     * @param newRoutingType         {@link com.net2plan.utils.Constants.RoutingType RoutingType}
     */
    public void setRoutingType(RoutingType newRoutingType)
    {
        netPlan.checkIsModifiable();
        if (routingType == newRoutingType) return;
        if (newRoutingType == RoutingType.HOP_BY_HOP_ROUTING) 
            if (this.isServiceChainRequest())
                throw new Net2PlanException("Cannot perform this operation with service chain demands, since the resource traversing information is lost");

        final int D = layer.demands.size();
        switch (newRoutingType)
        {
            case HOP_BY_HOP_ROUTING:
            {
            	SortedMap<Demand,SortedMap<Link,Double>> newFrs = GraphUtils.convert_xp2fdeMap(cache_routes);
                for (Route r : new ArrayList<> (cache_routes)) r.remove();
                routingType = RoutingType.HOP_BY_HOP_ROUTING;
                if (newFrs.containsKey(this)) updateHopByHopRoutingToGivenFrs(newFrs.get(this));
                break;
            }

            case SOURCE_ROUTING:
            {
                if (!cache_routes.isEmpty()) throw new RuntimeException ();
                final DoubleMatrix2D trafficInLinks_xde = DoubleFactory2D.sparse.make(layer.demands.size() , layer.links.size());
                for (Entry<Link,Pair<Double,Double>> entry : cacheHbH_normCarriedOccupiedPerLinkCurrentState.entrySet())
                	trafficInLinks_xde.set(this.index, entry.getKey().index, entry.getValue().getSecond());
                this.removeAllForwardingRules();
                routingType = RoutingType.SOURCE_ROUTING;
                List<Demand> d_p = new LinkedList<Demand>();
                List<Double> x_p = new LinkedList<Double>();
                List<List<Link>> pathList = new LinkedList<List<Link>>();
                GraphUtils.convert_xde2xp(netPlan.nodes, layer.links, new TreeSet<> (Arrays.asList(this)), trafficInLinks_xde , d_p, x_p, pathList);
                Iterator<Demand> it_demand = d_p.iterator();
                Iterator<Double> it_xp = x_p.iterator();
                Iterator<List<Link>> it_pathList = pathList.iterator();
                while (it_demand.hasNext())
                {
                    final Demand d = it_demand.next();
                    assert d == this;
                    final double trafficInPath = it_xp.next();
                    final List<Link> seqLinks = it_pathList.next();
                    netPlan.addRoute(d, trafficInPath, trafficInPath, seqLinks, null);
                }
                break;
            }

            default:
                throw new RuntimeException("Bad - Unknown routing type " + newRoutingType);
        }
        if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
    }


	
	/**
	 * <p>Returns the routes associated to this demand, but only those that are a backup route.</p>
	 * <p><b>Important</b>: If network layer routing type is not {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, an exception is thrown.</p>
	 * @return The set of routes
	 * */
	public SortedSet<Route> getRoutesAreBackup ()
	{
		return getRoutes ().stream().filter(e -> e.isBackupRoute()).collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * <p>Returns the routes associated to this demand, but only those that are have themselves a backup route.</p>
	 * <p><b>Important</b>: If network layer routing type is not {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, an exception is thrown.</p>
	 * @return The set of routes
	 * */
	public SortedSet<Route> getRoutesHaveBackup ()
	{
		return getRoutes ().stream().filter(e -> e.hasBackupRoutes()).collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * <p>Returns the routes associated to this demand, but only those that have no backup route themselves.</p>
	 * <p><b>Important</b>: If network layer routing type is not {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, an exception is thrown.</p>
	 * @return The set of routes
	 * */
	public SortedSet<Route> getRoutesHaveNoBackup ()
	{
		return getRoutes ().stream().filter(e -> !e.hasBackupRoutes()).collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * <p>Returns the routes associated to this demand, but only those that are not a backup route.</p>
	 * <p><b>Important</b>: If network layer routing type is not {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, an exception is thrown.</p>
	 * @return The set of routes
	 * */
	public SortedSet<Route> getRoutesAreNotBackup ()
	{
		return getRoutes ().stream().filter(e -> !e.isBackupRoute()).collect(Collectors.toCollection(TreeSet::new));
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
	}

	/**
	 * <p>Returns {@code true} if the traffic of the demand is traversing an oversubscribed link, {@code false} otherwise.</p>
	 * @return {@code true} if the traffic is traversing an oversubscribed link, {@code false} otherwise
	 */
	public boolean isTraversingOversubscribedLinks ()
	{
		if (isSourceRouting())
		{
			for (Route r : cache_routes)
			    if (r.getCarriedTraffic() > Configuration.precisionFactor)
    				for (Link e : r.getSeqLinks())
    					if (e.isOversubscribed())
    						return true;
		}
		else
		{
			for (Link e : cacheHbH_normCarriedOccupiedPerLinkCurrentState.keySet())
				if (cacheHbH_normCarriedOccupiedPerLinkCurrentState.get(e).getSecond() > Configuration.precisionFactor)
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
		if (routingType != RoutingType.SOURCE_ROUTING) throw new Net2PlanException ("The routing type must be SOURCE ROUTING");
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
		if (routingType != RoutingType.SOURCE_ROUTING) throw new Net2PlanException ("The routing type must be SOURCE ROUTING");
		return Collections.unmodifiableList(this.mandatorySequenceOfTraversedResourceTypes);
	}

	/** Sets the sequence of types of resources that the routes of this demand have to follow. This method is to make the demand become 
	 * a request of service chains. This method can only be called if the routing type is SOURCE ROUTING, and the demand has no routes 
	 * at the moment.
	 * @param resourceTypesSequence the sequence of types of the resources that has to be traversed by all the routes of this demand. If null, an empty sequence is assumed
	 */
	public void setServiceChainSequenceOfTraversedResourceTypes (List<String> resourceTypesSequence)
	{
		if (routingType != RoutingType.SOURCE_ROUTING) throw new Net2PlanException ("The routing type must be SOURCE ROUTING");
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
		if (routingType == RoutingType.SOURCE_ROUTING)
			return this.cache_routes.size () >= 2;
		final SortedSet<Node> initialLinkNodes = new TreeSet<> ();
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
		return (this.offeredTraffic > 0 && this.carriedTraffic == 0) || (this.carriedTraffic + Configuration.precisionFactor < offeredTraffic);
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
		return coupledUpperOrSameLayerLink != null;
	}
	
	/**
	 * <p>Returns {@code true} if the demand is coupled to a link in the same layer as the demand, false otherwise.</p>
	 * @return see above
	 */
	public boolean isCoupledInSameLayer ()
	{
		if (coupledUpperOrSameLayerLink != null) if (coupledUpperOrSameLayerLink.getLayer() == this.layer) return true;
		return false;
	}
	
	/**
	 * <p>Returns {@code true} if the demand is coupled to a link in a different layer than the demnad (and thus an *upper* layer)
	 * , false otherwise.</p>
	 * @return see above
	 */
	public boolean isCoupledInDifferentLayer ()
	{
		if (coupledUpperOrSameLayerLink != null) if (coupledUpperOrSameLayerLink.getLayer() != this.layer) return true;
		return false;
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
	public SortedMap<Pair<Demand,Link>,Double> getForwardingRules ()
	{
		checkAttachedToNetPlanObject ();
		checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		SortedMap<Pair<Demand,Link>,Double> res = new TreeMap<Pair<Demand,Link>,Double> ();
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
	  * Returns the routing type of the demand. 
	  * @return The routing type of the given layer
	  * @see com.net2plan.utils.Constants.RoutingType
	  */
	 public RoutingType getRoutingType()
	 {
	     return routingType;
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
	 * <p>If this demand was added using the method addDemandBidirectional(),
	 * returns the demand in the other direction (if it was not previously removed). Returns null otherwise. </p>
	 * @return the demand in the other direction, if this demand was created with addDemandBidirectional(), or {@code null} otherwise
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
		if (this.bidirectionalPair == d) return;
		if (this.bidirectionalPair != null)
		    for (Route r : getRoutes()) 
		        r.bidirectionalPair = null;
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
		final Demand d = netPlan.addDemand(this.ingressNode, this.egressNode, this.getOfferedTraffic() , this.routingType , this.attributes, this.layer);
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
		return coupledUpperOrSameLayerLink;
	}

	/**
	 * <p>Couples this demand to a link in an upper layer. After the link is coupled, its capacity will be always equal to the demand carried traffic.</p>
	 *
	 * <p><b>Important:</b> The link and the demand must belong to different layers, have same end nodes, not be already coupled,
	 * and the traffic units of the demand must be the same as the capacity units of the link. Also, the coupling of layers cannot create a loop.</p>
	 * @param link Link to be coupled
	 */
	public void coupleToUpperOrSameLayerLink (Link link)
	{
		netPlan.checkIsModifiable();
		checkAttachedToNetPlanObject();
		final NetworkLayer upperLayer = link.layer;
		final NetworkLayer lowerLayer = this.layer;
		final boolean sameLayerCoupling = (upperLayer == lowerLayer);
		
		if (!link.originNode.equals(this.ingressNode)) throw new Net2PlanException("Link and demand to couple must have the same end nodes");
		if (!link.destinationNode.equals(this.egressNode)) throw new Net2PlanException("Link and demand to couple must have the same end nodes");
//		if (upperLayer.equals(lowerLayer)) throw new Net2PlanException("Bad - Trying to couple links and demands in the same layer");
		if (!upperLayer.linkCapacityUnitsName.equals(lowerLayer.demandTrafficUnitsName))
			throw new Net2PlanException(String.format(netPlan.TEMPLATE_CROSS_LAYER_UNITS_NOT_MATCHING, upperLayer.id, upperLayer.linkCapacityUnitsName, lowerLayer.id, lowerLayer.demandTrafficUnitsName));
		if ((link.coupledLowerOrThisLayerDemand!= null) || (link.coupledLowerLayerMulticastDemand != null)) throw new Net2PlanException("Link " + link + " at layer " + upperLayer.id + " is already associated to a lower layer demand");
		if (this.coupledUpperOrSameLayerLink != null) throw new Net2PlanException("Demand " + this.id + " at layer " + lowerLayer.id + " is already associated to an upper layer demand");
		if (sameLayerCoupling)
		{
			final SortedSet<Link> upIntraLayerPropLinks = link.getIntraLayerUpPropagationIncludingMe();
			final SortedSet<Link> downIntraLayerPropLinks = this.getLinksThisLayerNonZeroFrOrTravRoutes(true);
			if (!Sets.intersection(upIntraLayerPropLinks, downIntraLayerPropLinks).isEmpty()) throw new Net2PlanException ("The intra-layer coupling creates a loop");
			
		}
//		{
//			if (layer.isSourceRouting())
//			{
//				for (Route r : link.getTraversingRoutes())
//					if (r.getDemand().isCoupledInSameLayer())
//						throw new Net2PlanException ("The link to couple in the same layer, is being traversed by demands which are couplde in the same layer");
//			}
//			else
//			{
//				for (Demand d : link.getForwardingRules().entrySet().stream().map(e->e.getKey().getFirst()).collect(Collectors.toList()))
//					if (d.isCoupledInSameLayer())
//						throw new Net2PlanException ("The link to couple in the same layer, is being traversed by demands which are couplde in the same layer");
//					
//			}
//			for (Link e : getLinksThisLayerNonZeroFrOrTravRoutes(false))
//				if (e.isCoupledInSameLayer())
//					throw new Net2PlanException ("The demand to couple in the same layer, is already traversing links coupled in the same layer");
//		}
		
		DemandLinkMapping coupling_thisLayerPair = null;
		if (sameLayerCoupling)
		{
			if (link.isIntraLayerLinkCouplingWithLoops (link.getSameLayerCoupledLinksHierarchicallyUpperCouldPutTrafficHere ())) throw new Net2PlanException ("Making such internal coupling would create coupling loops");
		}
		else
		{
			coupling_thisLayerPair = netPlan.interLayerCoupling.getEdge(lowerLayer, upperLayer);
			if (coupling_thisLayerPair == null)
			{
				coupling_thisLayerPair = new DemandLinkMapping();
				boolean valid;
				try { valid = netPlan.interLayerCoupling.addDagEdge(lowerLayer, upperLayer, coupling_thisLayerPair); }
				catch (DirectedAcyclicGraph.CycleFoundException ex) { valid = false; }
				if (!valid) throw new Net2PlanException("Coupling between link " + link + " at layer " + upperLayer + " and demand " + this.id + " at layer " + lowerLayer.id + " would induce a cycle between layers");
			}
		}

		/* All tests passed */
		link.updateCapacityAndZeroCapacityLinksAndRoutesCaches(carriedTraffic);
		link.coupledLowerOrThisLayerDemand = this;
		this.coupledUpperOrSameLayerLink = link;
		link.layer.cache_coupledLinks.add (link);
		this.layer.cache_coupledDemands.add (this);
		if (!sameLayerCoupling) coupling_thisLayerPair.put(this, link);
		link.updateWorstCasePropagationTraversingUnicastDemandsAndMaybeRoutes();
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
		if (isCoupled()) throw new Net2PlanException ("The demand is already coupled");
		
		Link newLink = null;
		try
		{
			newLink = netPlan.addLink(ingressNode , egressNode , carriedTraffic , netPlan.getNodePairEuclideanDistance(ingressNode , egressNode) , 200000 , null , newLinkLayer);
			coupleToUpperOrSameLayerLink(newLink);
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
		if (coupledUpperOrSameLayerLink == null) throw new Net2PlanException ("Demand is not coupled to a link");
		
		Link link = this.coupledUpperOrSameLayerLink;
		link.checkAttachedToNetPlanObject(netPlan);
		if (link.coupledLowerOrThisLayerDemand != this) throw new RuntimeException ("Bad");
		final NetworkLayer upperLayer = link.layer;
		final NetworkLayer lowerLayer = this.layer;

		link.coupledLowerOrThisLayerDemand = null;
		this.coupledUpperOrSameLayerLink = null;
		link.layer.cache_coupledLinks.remove (link);
		this.layer.cache_coupledDemands.remove(this);
		if (upperLayer != lowerLayer)
		{
			final DemandLinkMapping coupling_thisLayerPair = netPlan.interLayerCoupling.getEdge(lowerLayer, upperLayer);
			coupling_thisLayerPair.remove(this);
			if (coupling_thisLayerPair.isEmpty()) netPlan.interLayerCoupling.removeEdge(lowerLayer , upperLayer);
		}
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
		checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		this.updateHopByHopRoutingToGivenFrs(new TreeMap<> ());
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

    /**
     * <p>Removes all forwarding rules associated to the demand.</p>
     * <p><b>Important: </b>If routing type is not {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING} and exception is thrown.</p>
     */
    public void removeAllRoutes()
    {
        checkAttachedToNetPlanObject();
        netPlan.checkIsModifiable();
        checkRoutingType(RoutingType.SOURCE_ROUTING);
        for (Route r : new ArrayList<> (cache_routes))
            r.remove();
        if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
    }


    /**
	 * <p>Returns the set of demand routes with shortest path (and its cost), using the cost per link array provided. If more than one shortest route exists, all of them are provided.
	 * If the cost vector provided is null, all links have cost one.</p>
	 * @param costs Costs for each link
	 * @return Pair where the first element is a set of routes (may be empty) and the second element the minimum cost (may be {@code Double.MAX_VALUE} if there is no shortest path)
	 */
	public Pair<SortedSet<Route>,Double> computeShortestPathRoutes (double [] costs)
	{
		if (costs == null) costs = DoubleUtils.ones(layer.links.size ()); else if (costs.length != layer.links.size()) throw new Net2PlanException ("The array of costs must have the same length as the number of links in the layer");
		SortedSet<Route> shortestRoutes = new TreeSet<Route> ();
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
	public Pair<SortedSet<Route>,Double> computeMinimumCostServiceChains (double [] linkCosts , double [] resourceCosts)
	{
		if (linkCosts == null) linkCosts = DoubleUtils.ones(layer.links.size ()); else if (linkCosts.length != layer.links.size()) throw new Net2PlanException ("The array of costs must have the same length as the number of links in the layer");
		if (resourceCosts == null) resourceCosts = DoubleUtils.ones(netPlan.resources.size ()); else if (resourceCosts.length != netPlan.resources.size ()) throw new Net2PlanException ("The array of resources costs must have the same length as the number of resources");
		SortedSet<Route> minCostRoutes = new TreeSet<Route> ();
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
		if (this.coupledUpperOrSameLayerLink != null) this.decouple();
		
		if (bidirectionalPair != null) { this.bidirectionalPair.bidirectionalPair = null; this.bidirectionalPair = null; }
		
		if (routingType == RoutingType.SOURCE_ROUTING)
			for (Route route : new TreeSet<Route> (cache_routes)) route.remove();
		else
		{
			for (Link e : this.cacheHbH_frs.keySet()) e.cacheHbH_frs.remove(this);
			for (Link e : this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.keySet())
			{
                final double x_deOccup = this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.get(e).getSecond();
                e.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.remove(this);
                e.cache_totalCarriedTraffic -= x_deOccup; 
                e.cache_totalOccupiedCapacity -= x_deOccup; 
//
//				e.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.remove(this);
//				e.updateLinkTrafficAndOccupation();
//				final double x_deOccup = this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.get(e).getSecond();
//				e.cache_carriedTraffic -= x_deOccup; 
//				e.cache_occupiedCapacity -= x_deOccup; 
			}
		}
		layer.cache_nodePairDemandsThisLayer.get(Pair.of(ingressNode, egressNode)).remove(this);

        for (String tag : tags) netPlan.cache_taggedElements.get(tag).remove(this);
		netPlan.cache_id2DemandMap.remove(id);
		NetPlan.removeNetworkElementAndShiftIndexes (layer.demands , index);
		ingressNode.cache_nodeOutgoingDemands.remove (this);
		egressNode.cache_nodeIncomingDemands.remove (this);
		final Pair<SortedSet<Demand>,SortedSet<MulticastDemand>> qosInfo = layer.cache_qosTypes2DemandMap.get(qosType);
		assert qosInfo != null;
		final boolean removed = qosInfo.getFirst().remove(this);
		assert removed;
		if (qosInfo.getFirst().isEmpty() && qosInfo.getSecond().isEmpty()) layer.cache_qosTypes2DemandMap.remove(qosType);
		final NetPlan npOld = this.netPlan;
        removeId();
        
		if (ErrorHandling.isDebugEnabled()) npOld.checkCachesConsistency();
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
		if (!isSourceRouting()) updateHopByHopRoutingToGivenFrs(this.cacheHbH_frs);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	
	/**
	 * <p>Returns a {@code String} representation of the demand.</p>
	 * @return {@code String} representation of the demand
	 */
	@Override
	public String toString () { return "d" + index + "-L" + layer.index; }
	

	/**
	 * <p>Checks the consistency of the cache. If it is incosistent, throws a {@code RuntimeException} </p>
	 */
	void checkCachesConsistency ()
	{
		super.checkCachesConsistency ();
		if (this.bidirectionalPair != null)
		{
			if (this.bidirectionalPair.bidirectionalPair != this) throw new RuntimeException ("Bad: this.bidirectionalPair.bidirectionalPair : " + this.bidirectionalPair.bidirectionalPair);
			if (this.bidirectionalPair.netPlan != this.netPlan) throw new RuntimeException ("Bad");
			if (this.bidirectionalPair.ingressNode != this.egressNode) throw new RuntimeException ("Bad");
			if (this.bidirectionalPair.egressNode != this.ingressNode) throw new RuntimeException ("Bad");
			if (this.bidirectionalPair.layer != this.layer) throw new RuntimeException ("Bad");
		}
		
		if (this.isCoupledInSameLayer())
		{
			final SortedSet<Link> upIntraLayerPropLinks = coupledUpperOrSameLayerLink.getIntraLayerUpPropagationIncludingMe();
			final SortedSet<Link> downIntraLayerPropLinks = this.getLinksThisLayerNonZeroFrOrTravRoutes(true);
			if (!Sets.intersection(upIntraLayerPropLinks, downIntraLayerPropLinks).isEmpty()) throw new RuntimeException ();
		}
			
		if (!isSourceRouting())
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
		if (routingType == RoutingType.SOURCE_ROUTING)
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
		if (coupledUpperOrSameLayerLink != null)
			if (!coupledUpperOrSameLayerLink.coupledLowerOrThisLayerDemand.equals (this)) throw new RuntimeException ("Bad");
		if (!layer.demands.contains(this)) throw new RuntimeException ("Bad");
		
		if ((routingCycleType != RoutingCycleType.LOOPLESS) && (!isSourceRouting()))
		{
			if (cache_worstCasePropagationTimeMs != Double.MAX_VALUE) throw new RuntimeException ();
			if (cache_worstCaseLengthInKm != Double.MAX_VALUE) throw new RuntimeException ();
		}
	}


	/** Returns the set of links in this layer, with non zero forwarding rules defined for them. The link may or may not 
	 * carry traffic, and may be or not be failed.
	 * @return see above
	 */
	public SortedSet<Link> getLinksWithNonZeroForwardingRules ()
	{
		checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		return this.cacheHbH_frs.keySet().stream().filter(e->cacheHbH_frs.get(e) != 0).collect(Collectors.toCollection(TreeSet::new));
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
	public Pair<SortedSet<Link>,SortedSet<Link>> getLinksNoDownPropagationPotentiallyCarryingTraffic  ()
	{
		final SortedSet<Link> resPrimary = new TreeSet<> ();
		final SortedSet<Link> resBackup = new TreeSet<> ();
		if (routingType == RoutingType.HOP_BY_HOP_ROUTING)
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

	
    /** Returns a map with an entry for each traversed link, and associated to it the demand's traffic carried in that link. 
     * If selected by the user, the carried traffic is given as a fraction respect to the demand offered traffic
     * @param normalizedToOfferedTraffic see above
     * @return  see above
     */
    public SortedMap<Link, Double> getTraversedLinksAndCarriedTraffic(final boolean normalizedToOfferedTraffic)
    {
        if (isSourceRouting())
        {
            final SortedMap<Link, Double> res = new TreeMap<>();
            final double normalizationFactor = (normalizedToOfferedTraffic ? (offeredTraffic <= Configuration.precisionFactor ? 1.0 : 1 / offeredTraffic) : 1.0);
            for (Route r : cache_routes)
                for (Link e : r.getSeqLinks())
                {
                    final Double currentVal = res.get(e);
                    final double newValue = (currentVal == null ? 0 : currentVal) + (r.getCarriedTraffic() * normalizationFactor);
                    res.put(e, newValue);
                }
            return res;
        } else
        {
        	final SortedMap<Link,Double> res = new TreeMap<> ();
            this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.entrySet().forEach(e->res.put(e.getKey() ,normalizedToOfferedTraffic ? e.getValue().getFirst() : e.getValue().getSecond() ));
            return res;
        }
    }
	
	/** Returns the amount of occupied capacity of the demand in a particular link
	 * @param e  see above
	 * @return  see above
	 */
	public double getOccupiedCapacity (Link e)
	{
		if (isSourceRouting())
		{
			double accum = 0;
			for (Route r : getRoutes ())
				if (r.getSeqLinks().contains(e))
					accum += r.getOccupiedCapacity(e);
			return accum;
		}
		else
		{
			final Pair<Double,Double> info = this.cacheHbH_normCarriedOccupiedPerLinkCurrentState.get(e);
			return info == null? 0 : info.getSecond();
		}
	}
	
	
	
	/* Updates all the network state, to the new situation where the hop-by-hop routing of a demand has changed */
	void updateHopByHopRoutingToGivenFrs (SortedMap<Link,Double> newFrsWithoutZeros)
	{
		final SortedSet<Link> affectedLinks = new TreeSet<>(Sets.union(newFrsWithoutZeros.keySet() , cacheHbH_frs.keySet()));
		
		/* set 0 in the down links and the link in-out from the down nodes (they do not send traffic) */
		/* update the cache per node (include failed links if fr > 0) */
		SortedMap<Link,Double> frsToApply = new TreeMap<> ();
		SortedMap<Node,SortedSet<Link>> tentativeCacheHbH_linksPerNodeWithNonZeroFr = new TreeMap<> (); // tentative since if closed cycles => not used
		for (Entry<Link,Double> fr : newFrsWithoutZeros.entrySet())
		{
			final Link e = fr.getKey();
			final Node a_e = e.getOriginNode();
			final double f_e = fr.getValue();
			if (f_e == 0) continue;
			SortedSet<Link> set = tentativeCacheHbH_linksPerNodeWithNonZeroFr.get(a_e); if (set == null) { set = new TreeSet<> (); tentativeCacheHbH_linksPerNodeWithNonZeroFr.put(a_e, set); }
			set.add(e);
			if (e.isDown() || e.getOriginNode().isDown() || e.getDestinationNode().isDown()) continue;
			frsToApply.put(e, f_e);
		}
		
		Quintuple<DoubleMatrix1D, RoutingCycleType , Double , Double , Double> fundMatrixComputation = 
				GraphUtils.computeRoutingFundamentalVector(frsToApply, tentativeCacheHbH_linksPerNodeWithNonZeroFr , ingressNode ,  egressNode);
		if (fundMatrixComputation.getSecond() == RoutingCycleType.CLOSED_CYCLES) 
		{
			System.out.println("Demand: " + this + ", ingress: " + ingressNode+ " -> egress: " + egressNode + ", frs: " + newFrsWithoutZeros);
			throw new ClosedCycleRoutingException("Closed routing cycle for demand " + this);
		}
		DoubleMatrix1D M = fundMatrixComputation.getFirst ();
		this.routingCycleType = fundMatrixComputation.getSecond();
		double s_egressNode = fundMatrixComputation.getThird();
		this.cache_worstCasePropagationTimeMs = fundMatrixComputation.getFourth();
		this.cache_worstCaseLengthInKm = fundMatrixComputation.getFifth();

		/* update different caches */
		this.cacheHbH_linksPerNodeWithNonZeroFr = tentativeCacheHbH_linksPerNodeWithNonZeroFr;
		carriedTraffic = offeredTraffic * M.get(egressNode.index) * s_egressNode;
		if (coupledUpperOrSameLayerLink != null)
			coupledUpperOrSameLayerLink.updateCapacityAndZeroCapacityLinksAndRoutesCaches(carriedTraffic);

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
//			Double new_fde = frsToApply.get(link); if (new_fde == null) new_fde = 0.0;
//			final double newXdeNormalized = M.get (link.originNode.index) * new_fde; //fowardingRulesThisFailureState_f_e.get (link.index);
//			final double newXdeOccup = offeredTraffic * newXdeNormalized; //fowardingRulesThisFailureState_f_e.get (link.index);
//			if (newXdeNormalized < -1E-5) throw new RuntimeException ("Bad");
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
			link.cache_totalCarriedTraffic += newXdeOccup - oldXdeOccup; // in hop-by-hop carried traffic is the same as occupied capacity
			link.cache_totalOccupiedCapacity += newXdeOccup - oldXdeOccup;
			assert !((newXdeNormalized > 1e-3) && (!link.isUp));
		}
		
		/* update the cache_frs in the link and demand */
		for (Link e : this.cacheHbH_frs.keySet())
			e.cacheHbH_frs.remove(this);
		this.cacheHbH_frs = new TreeMap<> (newFrsWithoutZeros);
		for (Entry<Link,Double> fr : this.cacheHbH_frs.entrySet())
			fr.getKey().cacheHbH_frs.put(this , fr.getValue());
		

		
		/* update the routing cycle type */
//		System.out.println ("updateHopByHopRoutingDemand demand: " + demand + ", this demand x_e: " + forwardingRules_x_de.viewRow(demand.index));
	}


	/** For intralayer coupling checks: Returns the links could carry traffic in any failure state, and worst case. This means: all with non-zero FRs, 
	 * or with traversing routes 
	 * @param propagateIntraLayerDownCoupling Propagation intra-layer down coupling
	 * @return Links of the current layer with forwarding rules or traversing routes
	 */
	SortedSet<Link> getLinksThisLayerNonZeroFrOrTravRoutes (boolean propagateIntraLayerDownCoupling)
	{
		final SortedSet<Link> res = new TreeSet<> ();
		if (routingType == RoutingType.HOP_BY_HOP_ROUTING)
		{
			for (Link link : this.cacheHbH_frs.keySet())
			{
					res.add(link);
					if (!propagateIntraLayerDownCoupling || !link.isCoupledInSameLayer()) continue;
					final SortedSet<Link> sameLayerDownProp = link.getCoupledDemand().getLinksThisLayerNonZeroFrOrTravRoutes  (true);
					res.addAll(sameLayerDownProp);
			}
		}
		else
		{
			for (Route r : cache_routes)
			{
				for (Link e : r.getSeqLinks()) 
				{
					res.add(e);
					if (!propagateIntraLayerDownCoupling || !e.isCoupledInSameLayer()) continue;
					final SortedSet<Link> sameLayerDownProp = e.getCoupledDemand().getLinksThisLayerNonZeroFrOrTravRoutes  (true);
					res.addAll(sameLayerDownProp);
				}
			}
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

}
