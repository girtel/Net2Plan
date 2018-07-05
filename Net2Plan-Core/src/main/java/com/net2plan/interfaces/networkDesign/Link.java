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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.libraries.TrafficSeries;
import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

/** <p>This class contains a representation of a link. A link is characterized by its initial and end node, the network layer it belongs to, 
 * and its capacity, measured in the layer link capacity units. When the routing type at the link layer is {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, the link
 * carries traffic of the traversing routes, and can have some capacity reserved by protection segments. If the routing type is {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}, the
 * link carried traffic depends on the demands offered traffic and the forwarding rules. In both routing types, links can be part of multicast trees and
 * carry their traffic.</p>
 * <ul>
 *		<li>A link has a length in km and a propagation speed (km/sec). Both determine its propagation latency.</li>
 *		<li>A link can belong to one or more shared risk groups (SRG).</li>
 *		<li>A link can be up or down. If a link is down, the carried traffic of all its traversing routes and segments is set
 * 			to zero ({@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}), the splitting factors of the forwarding rules through it are set to zero
 * 			({@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}), and the carried traffic of any traversing multicast tree is set to zero.</li>
 * 		<li> A link can be coupled to a lower layer unicast demand, a lower layer multicast demand, or none. If coupled, the link
 * 			capacity is made equal to the demand (unicast or multicast) carried traffic. The link capacity and the demand traffic must be measured in the same units</li>
 * 	</ul>
 * @author Pablo Pavon-Marino
 * @since 0.4.0
 */

public class Link extends NetworkElement
{
	
	final NetworkLayer layer;
	final Node originNode;
	final Node destinationNode;
	double capacity;
	double cache_totalCarriedTraffic;
	double cache_totalOccupiedCapacity;
	double lengthInKm;
	double propagationSpeedInKmPerSecond;
	boolean isUp;
	Link bidirectionalPair;
	private SortedMap<String,Pair<Integer,Double>> qos2PriorityMaxLinkCapPercentage;
	private TrafficSeries monitoredOrForecastedTraffics;
//	private SortedMap<String,Pair<Double,Double>> cache_perQoSOccupationAndQosViolationMap;

	SortedSet<SharedRiskGroup> cache_nonDynamicSrgs;
	SortedMap<Route,Integer> cache_traversingRoutes; // for each traversing route, the number of times it traverses this link (in seqLinksRealPath). If the route has segments, their internal route counts also
	SortedSet<MulticastTree> cache_traversingTrees;
	Demand coupledLowerOrThisLayerDemand;
	MulticastDemand coupledLowerLayerMulticastDemand;
	
	SortedMap<Demand,Double> cacheHbH_frs;
	SortedMap<Demand,Pair<Double,Double>> cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState; // carried is normalized respect to demand total CARRIED traffic
	
	public double getOccupiedCapacityFromDemand (Demand d)
	{
	    if (d.isSourceRouting())
	    {
	        double res = 0;
	        for (Route travRoute : cache_traversingRoutes.keySet())
	            if (travRoute.getDemand().equals(d)) 
	                res += travRoute.getOccupiedCapacity(this);
	        return res;
	    }
	    else
	    {
	        final Pair<Double,Double> normalizedRespectoToDemandCarriedAndCarried = cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.get(d);
	        if (normalizedRespectoToDemandCarriedAndCarried == null) return 0;
	        return normalizedRespectoToDemandCarriedAndCarried.getSecond();
	    }
	}
	
	/**
	 * Default constructor, when the link is a link (and not a protection segment)
	 *
	 * @param netPlan Network topology
	 * @param id Link ID
	 * @param index Link index
	 * @param layer Network layer
	 * @param originNode Origin node
	 * @param destinationNode Destination node
	 * @param lengthInKm Link's length
	 * @param propagationSpeedInKmPerSecond Link's propagation speed
	 * @param capacity Link's capacity
	 * @param attributes Link's attributes
	 */
	protected Link (NetPlan netPlan , long id , int index , NetworkLayer layer , Node originNode , Node destinationNode , double lengthInKm , double propagationSpeedInKmPerSecond , double capacity , AttributeMap attributes)
	{
		super (netPlan , id , index , attributes);
		this.setName ("Link-" + index);

		if (netPlan != null)
		{
		    assert netPlan.equals(layer.netPlan);
            assert netPlan.equals(originNode.netPlan);
            assert netPlan.equals(destinationNode.netPlan);
		}
		this.layer = layer;
		this.originNode = originNode;
		this.destinationNode = destinationNode;
		this.cache_totalCarriedTraffic = 0;
		this.cache_totalOccupiedCapacity = 0;
		this.lengthInKm = lengthInKm;
		this.propagationSpeedInKmPerSecond = propagationSpeedInKmPerSecond;
		this.isUp = true;
		this.coupledLowerOrThisLayerDemand = null;
		this.coupledLowerLayerMulticastDemand = null;
		this.cache_nonDynamicSrgs = new TreeSet<SharedRiskGroup> ();
		this.cache_traversingRoutes = new TreeMap<Route,Integer> ();
		this.cache_traversingTrees = new TreeSet<MulticastTree> ();
		this.cacheHbH_frs = new TreeMap<> ();
		this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState = new TreeMap<> ();
		this.capacity = capacity;
		this.bidirectionalPair = null;
		this.qos2PriorityMaxLinkCapPercentage = new TreeMap<> ();
		this.monitoredOrForecastedTraffics = new TrafficSeries ();
		if (capacity < Configuration.precisionFactor) if (layer != null) layer.cache_linksZeroCap.add(this); // do not call here the regular updae function on purpose, there is no previous capacity info
	}

	void copyFrom (Link origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.capacity = origin.capacity;
		this.cache_totalCarriedTraffic = origin.cache_totalCarriedTraffic;
		this.cache_totalOccupiedCapacity = origin.cache_totalOccupiedCapacity;
		this.lengthInKm = origin.lengthInKm;
		this.propagationSpeedInKmPerSecond = origin.propagationSpeedInKmPerSecond;
		this.isUp = origin.isUp;
		this.cache_nonDynamicSrgs = new TreeSet<SharedRiskGroup> ();
		this.cache_traversingRoutes = new TreeMap<Route,Integer> ();
		this.cache_traversingTrees = new TreeSet<MulticastTree> ();
		this.qos2PriorityMaxLinkCapPercentage = new TreeMap<> ();
		for (Entry<String,Pair<Integer,Double>> ee : origin.qos2PriorityMaxLinkCapPercentage.entrySet())
			this.qos2PriorityMaxLinkCapPercentage.put(ee.getKey(), Pair.of (ee.getValue().getFirst() , ee.getValue().getSecond()));
		for (SharedRiskGroup s : origin.cache_nonDynamicSrgs) this.cache_nonDynamicSrgs.add(this.netPlan.getSRGFromId(s.id));
		for (Entry<Route,Integer> r : origin.cache_traversingRoutes.entrySet()) this.cache_traversingRoutes.put(this.netPlan.getRouteFromId(r.getKey ().id) , r.getValue());
		for (MulticastTree t : origin.cache_traversingTrees) this.cache_traversingTrees.add(this.netPlan.getMulticastTreeFromId(t.id));
		this.coupledLowerOrThisLayerDemand = origin.coupledLowerOrThisLayerDemand == null? null : this.netPlan.getDemandFromId(origin.coupledLowerOrThisLayerDemand.id);
		this.coupledLowerLayerMulticastDemand = origin.coupledLowerLayerMulticastDemand == null? null : this.netPlan.getMulticastDemandFromId(origin.coupledLowerLayerMulticastDemand.id);
		this.cacheHbH_frs.clear(); 
		for (Entry<Demand,Double> fr : origin.cacheHbH_frs.entrySet()) 
			this.cacheHbH_frs.put(netPlan.getDemandFromId(fr.getKey().id), fr.getValue());
		this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.clear();
		for (Entry<Demand,Pair<Double,Double>> fr : origin.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.entrySet()) 
			this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.put(netPlan.getDemandFromId(fr.getKey().id), Pair.of(fr.getValue().getFirst(), fr.getValue().getSecond()));
		this.bidirectionalPair = origin.bidirectionalPair == null? null : netPlan.getLinkFromId(origin.bidirectionalPair.getId());
		this.monitoredOrForecastedTraffics = origin.monitoredOrForecastedTraffics;
	}

	boolean isDeepCopy (Link e2)
	{
		if (!super.isDeepCopy(e2)) return false;
		if (layer.id != e2.layer.id) return false;
		if (originNode.id != e2.originNode.id) return false;
		if (destinationNode.id != e2.destinationNode.id) return false;
		if ((this.bidirectionalPair == null) != (e2.bidirectionalPair == null)) return false;
		if (this.bidirectionalPair != null) if (this.bidirectionalPair.id != e2.bidirectionalPair.id) return false;
		if (this.capacity != e2.capacity) return false;
		if (this.cache_totalCarriedTraffic != e2.cache_totalCarriedTraffic) return false;
		if (this.cache_totalOccupiedCapacity != e2.cache_totalOccupiedCapacity) return false;
		if (this.lengthInKm != e2.lengthInKm) return false;
		if (this.propagationSpeedInKmPerSecond != e2.propagationSpeedInKmPerSecond) return false;
		if (this.qos2PriorityMaxLinkCapPercentage.equals(e2.qos2PriorityMaxLinkCapPercentage)) return false;
		if (this.isUp != e2.isUp) return false;
		if ((this.coupledLowerOrThisLayerDemand == null) != (e2.coupledLowerOrThisLayerDemand == null)) return false; 
		if ((this.coupledLowerOrThisLayerDemand != null) && (coupledLowerOrThisLayerDemand.id != e2.coupledLowerOrThisLayerDemand.id)) return false;
		if ((this.coupledLowerLayerMulticastDemand == null) != (e2.coupledLowerLayerMulticastDemand == null)) return false; 
		if ((this.coupledLowerLayerMulticastDemand != null) && (coupledLowerLayerMulticastDemand.id != e2.coupledLowerLayerMulticastDemand.id)) return false;
		if (!NetPlan.isDeepCopy(this.cache_nonDynamicSrgs , e2.cache_nonDynamicSrgs)) return false;
		if (!NetPlan.isDeepCopy(this.cache_traversingRoutes , e2.cache_traversingRoutes)) return false;
		if (!NetPlan.isDeepCopy(this.cache_traversingTrees , e2.cache_traversingTrees)) return false;
		if (!NetPlan.isDeepCopy(this.cacheHbH_frs , e2.cacheHbH_frs)) return false;
		if (!NetPlan.isDeepCopy(this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState , e2.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState)) return false;
		if (!this.monitoredOrForecastedTraffics.equals(e2.monitoredOrForecastedTraffics)) return false;
		return true;
	}

	

	/** Returns the total traffic occupied in the link by the demands of a given QoS type, and 
	 * the amount out of that traffic that is violating the QoS
	 * @param qosType see above
	 * @return see above
	 */
	public Pair<Double,Double> getQosOccupationAndQosViolation (String qosType)
	{
		final SortedMap<String,Pair<Double,Double>> qos2OccupAndViol = getPerQoSOccupationAndQosViolationMap ();
		if (qos2OccupAndViol.containsKey(qosType))
			return qos2OccupAndViol.get(qosType); 
		return Pair.of(0.0, 0.0);
	}
	
	/** Returns the priority (lower better) and link percentage maximum utilization, assigned to the 
	 * QoS type indicated. If none information was defined, the worst priority Integer.MAX_VALUE and the a 
	 * link utilization of 100% is returned 
	 * @param qosType see above
	 * @return see above
	 */
	public Pair<Integer,Double> getQosTypePriorityAndMaxLinkUtilization (String qosType)
	{
		if (this.qos2PriorityMaxLinkCapPercentage.containsKey(qosType))
		{
//			System.out.println("qos2PriorityMaxLinkCapPercentage: qos: " + qosType + ", " + this.qos2PriorityMaxLinkCapPercentage.get(qosType));
			return this.qos2PriorityMaxLinkCapPercentage.get(qosType);
		}
		return Pair.of(Integer.MAX_VALUE, 1.0);
	}

	/** Returns the priority (lower better) and link percentage maximum utilization, assigned to the 
	 * QoS type indicated, for each of the QoS types with traffic traversing the link.
	 * @return see above
	 */
	public SortedMap<String,Pair<Integer,Double>> getQosTypePriorityAndMaxLinkUtilizationMap ()
	{
		return Collections.unmodifiableSortedMap(qos2PriorityMaxLinkCapPercentage);
	}

	
	/** Sets the priority (lower better) and link percentage maximum utilization, assigned to the 
	 * QoS type indicated. 
	 * @param qosType  see above
	 * @param priority  see above
	 * @param maxLinkUtilization must be between zero and 1
	 */
	public void setQosTypePriorityAndMaxLinkUtilization (String qosType , int priority , double maxLinkUtilization)
	{
		if (maxLinkUtilization <0  || maxLinkUtilization > 1) throw new Net2PlanException ("Maximum link utilizations must be between zero and one");
		this.qos2PriorityMaxLinkCapPercentage.put (qosType , Pair.of(priority, maxLinkUtilization));
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/** Removes any information on the priority (lower better) and link percentage maximum utilization, assigned to the 
	 * QoS type indicated. 
	 * @param qosType  see above
	 */
	public void removeQosTypePriorityAndMaxLinkUtilization (String qosType)
	{
		this.qos2PriorityMaxLinkCapPercentage.remove (qosType);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	
	/** Returns for each traversing demand, a pair with i) the occupied capacity in the link of that 
	 * demand, ii) the amount of such occupied capacity that is violating the demand QoS assigned capacity. 
	 * Demands with zero traffic in the link (and thus zero QoS violation), may not appear in the map
	 * @return  see above
	 */
	public SortedMap<String,Pair<Double,Double>> getPerQoSOccupationAndQosViolationMap ()
	{
		final SortedMap<String,Pair<Double,Double>> res = new TreeMap<> ();
		double check_totalCarriedTraffic = 0;
		double check_cache_totalOccupiedCapacity = 0;
		for (Entry<Route,Integer> travRouteInfo : cache_traversingRoutes.entrySet())
		{
			final Route r = travRouteInfo.getKey();
			final double carriedTraffic = r.getCarriedTraffic();
			final double occupiedCapacity = r.getOccupiedCapacity(this);
			check_totalCarriedTraffic += carriedTraffic;
			check_cache_totalOccupiedCapacity += occupiedCapacity;
			final String qosType = r.getDemand().getQosType();
			Pair<Double,Double> thisQosTypeInfoSoFar = res.get(qosType);
			if (thisQosTypeInfoSoFar == null) { thisQosTypeInfoSoFar = Pair.of(0.0, 0.0); res.put(qosType, thisQosTypeInfoSoFar); }
			thisQosTypeInfoSoFar.setFirst(thisQosTypeInfoSoFar.getFirst() + occupiedCapacity);
		}

		/* Add the info of the demands with forwarding rules */
		for (Entry<Demand,Pair<Double,Double>> entryInfo : this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.entrySet())
		{
			final Demand demand = entryInfo.getKey();
			final String qosType = demand.getQosType();
			final double occupiedCapacityAndCarriedTraffic = entryInfo.getValue().getSecond();
			Pair<Double,Double> thisQosTypeInfoSoFar = res.get(qosType);
			if (thisQosTypeInfoSoFar == null) { thisQosTypeInfoSoFar = Pair.of(0.0, 0.0); res.put(qosType, thisQosTypeInfoSoFar); }
			thisQosTypeInfoSoFar.setFirst(thisQosTypeInfoSoFar.getFirst() + occupiedCapacityAndCarriedTraffic);
			check_totalCarriedTraffic += occupiedCapacityAndCarriedTraffic;
			check_cache_totalOccupiedCapacity += occupiedCapacityAndCarriedTraffic;
		}
		for (MulticastTree t : cache_traversingTrees)
		{
			final MulticastDemand demand = t.getMulticastDemand();
			final String qosType = demand.getQosType();
			final double occupiedCapacity = t.getOccupiedLinkCapacity();
			final double carriedTraffic = t.getCarriedTraffic();
			Pair<Double,Double> thisQosTypeInfoSoFar = res.get(qosType);
			if (thisQosTypeInfoSoFar == null) { thisQosTypeInfoSoFar = Pair.of(0.0, 0.0); res.put(qosType, thisQosTypeInfoSoFar); }
			thisQosTypeInfoSoFar.setFirst(thisQosTypeInfoSoFar.getFirst() + occupiedCapacity);
			check_totalCarriedTraffic += carriedTraffic;
			check_cache_totalOccupiedCapacity += occupiedCapacity;
		}

		final List<String> sortedByPriorityQosTypes = new ArrayList<> (res.keySet());
//		System.out.println("BEFORE SORT: sortedByPriorityQosTypes: " + sortedByPriorityQosTypes);
		Collections.sort(sortedByPriorityQosTypes , (e1,e2)->Integer.compare(getQosTypePriorityAndMaxLinkUtilization(e1).getFirst (), getQosTypePriorityAndMaxLinkUtilization (e2).getFirst()));
//		System.out.println("qos2PriorityMaxLinkCapPercentage: " + qos2PriorityMaxLinkCapPercentage);
//		System.out.println("AFTER SORT: sortedByPriorityQosTypes: " + sortedByPriorityQosTypes);
		/* For each QoS type */
		double availableLinkCapacity = getCapacity(); 
		for (String qosType : sortedByPriorityQosTypes)
		{
			final double qosTypeMaxAssignCap = getCapacity() * this.getQosTypePriorityAndMaxLinkUtilization(qosType).getSecond();
			final double totalOccupiedCapacityThisQos = res.get(qosType).getFirst();
			final double assignedCapacityThisQos = Math.min(Math.min(totalOccupiedCapacityThisQos, qosTypeMaxAssignCap) , availableLinkCapacity);
			final double totalQosViolationThisQos = Math.max(0, totalOccupiedCapacityThisQos - assignedCapacityThisQos);
			final double fractionOfTrafficInQosViolation = totalOccupiedCapacityThisQos <= Configuration.precisionFactor? 0 : totalQosViolationThisQos / totalOccupiedCapacityThisQos;
			availableLinkCapacity = Math.max(0, availableLinkCapacity - assignedCapacityThisQos);
			res.get(qosType).setSecond(totalOccupiedCapacityThisQos * fractionOfTrafficInQosViolation);
		}
		return res;
	}

	/** Returns the set of demands in this layer, with non zero forwarding rules defined for them in this link. 
	 * The link may or may not carry traffic, and may be or not be failed.
	 * @return see above
	 */
	public SortedSet<Demand> getDemandsWithNonZeroForwardingRules ()
	{
		return this.cacheHbH_frs.keySet().stream().filter(d->cacheHbH_frs.get(d) != 0).collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * <p>Returns the link origin node.</p>
	 * @return The origin node
	 */
	public Node getOriginNode()
	{
		return originNode;
	}

	/**
	 * <p>Returns the link destination {@link com.net2plan.interfaces.networkDesign.Node Node}.</p>
	 * @return The destination node
	 */
	public Node getDestinationNode()
	{
		return destinationNode;
	}


	/**
	 * <p>Returns the {@link com.net2plan.interfaces.networkDesign.NetworkLayer NetworkLayer} the link belongs to.</p>
	 * @return The layer
	 */
	public NetworkLayer getLayer()
	{
		return layer;
	}
	
	/**
	 * <p>Returns the unicast {@link com.net2plan.interfaces.networkDesign.Demand Demand} the link is coupled to, or {@code null} if none.</p>
	 * @return The coupled demand (or {@code null}
	 */
	public Demand getCoupledDemand ()
	{
		checkAttachedToNetPlanObject();
		return coupledLowerOrThisLayerDemand;
	}

	/**
	 *  <p>Returns the {@link com.net2plan.interfaces.networkDesign.MulticastDemand MulticastDemand} the link is coupled to, or {@code null} if none.</p>
	 * @return The coupled multicast demand (or {@code null}
	 */
	public MulticastDemand getCoupledMulticastDemand ()
	{
		checkAttachedToNetPlanObject();
		return coupledLowerLayerMulticastDemand;
	}

	/**
	 * <p>Returns {@code True} if the link is coupled to a {@link com.net2plan.interfaces.networkDesign.Demand Demand} from other layer, {@code false} otherwise.</p>
	 * @return {@code True} if the link is coupled, {@code false} otherwise
	 */
	public boolean isCoupled () {  return ((coupledLowerOrThisLayerDemand != null) || (coupledLowerLayerMulticastDemand != null)); }
	
	/**
	 * <p>Returns the non zero forwarding rules that are defined in the link. If the routing type of the layer where the link is attached is not
	 * {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING} an exception is thrown.</p>
	 * @return SortedMap of demand-link pairs, associated to {@code [0,1]} splitting factors
	 */
	public SortedMap<Pair<Demand,Link>,Double> getForwardingRules ()
	{
		checkAttachedToNetPlanObject();
		
		SortedMap<Pair<Demand,Link>,Double> res = new TreeMap<> ();
		for (Entry<Demand,Double> fr : this.cacheHbH_frs.entrySet())
			res.put(Pair.of(fr.getKey() ,  this), fr.getValue());
		return res;
	}

	/**
	 * <p>Returns the link capacity.</p>
	 * @return The link capacity
	 */
	public double getCapacity()
	{
		return capacity <= Configuration.precisionFactor? 0 : capacity;
	}

	/**
	 * <p>Sets the link capacity.</p>
	 * @param newLinkCapacity The link capacity (must be non-negative)
	 */
	public void setCapacity(double newLinkCapacity)
	{
		newLinkCapacity = NetPlan.adjustToTolerance(newLinkCapacity);
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (newLinkCapacity < 0) throw new Net2PlanException ("Negative link capacities are not possible");
		if ((coupledLowerOrThisLayerDemand != null) || (coupledLowerLayerMulticastDemand != null)) throw new Net2PlanException ("Coupled links cannot change its capacity");
		updateCapacityAndZeroCapacityLinksAndRoutesCaches (newLinkCapacity);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	void updateCapacityAndZeroCapacityLinksAndRoutesCaches (double newCapacity) 
	{
		final boolean fromZeroToMore = (this.capacity < Configuration.precisionFactor) && (newCapacity >= Configuration.precisionFactor); 
		final boolean fromMoreToZero = (this.capacity >= Configuration.precisionFactor) && (newCapacity < Configuration.precisionFactor); 
		this.capacity = newCapacity;
		if (fromMoreToZero)
		{
			layer.cache_linksZeroCap.add(this);
			for (Route r : cache_traversingRoutes.keySet()) layer.cache_routesTravLinkZeroCap.add(r);
			for (MulticastTree t : cache_traversingTrees) layer.cache_multicastTreesTravLinkZeroCap.add(t);
		}
		else if (fromZeroToMore)
		{
			layer.cache_linksZeroCap.remove(this);
			for (Route r : cache_traversingRoutes.keySet())
			{
				if (r.cache_seqLinksRealPath.stream().allMatch(e->e.getCapacity() >= Configuration.precisionFactor))
					layer.cache_routesTravLinkZeroCap.remove(r);
			}
			for (MulticastTree t : cache_traversingTrees)
			{
				if (t.linkSet.stream().allMatch(e->e.getCapacity() >= Configuration.precisionFactor))
					layer.cache_multicastTreesTravLinkZeroCap.remove(t);
			}
		}
	}

	/**
	 * <p>Returns the link carried traffic (in traffic demand units).</p>
	 * @return The carried traffic
	 */
	public double getCarriedTraffic()
	{
		return cache_totalCarriedTraffic;
	}
	
	
	/** Returns the set of routes traversing the link that are designated as backup of other route
	 * @return see above
	 */
	public SortedSet<Route> getTraversingBackupRoutes ()
	{
		return getTraversingRoutes().stream ().filter(e->e.isBackupRoute()).collect(Collectors.toCollection(TreeSet::new));
	}
	
	/** Returns the set number of routes traversing the link that are designated as backup of other route
	 * @return see above
	 */
	public int getNumberOfTraversingBackupRoutes ()
	{
		return (int) getTraversingRoutes().stream ().filter(e->e.isBackupRoute()).count();
	}

	/**
	 * <p>Returns the link utilization, measured as the ratio between the total occupied capacity in the link and the total link capacity.</p>
	 * @return The utilization as described above. If the link has zero capacity and strictly positive occupied capacity, Double.POSITIVE_INFINITY is returned 
	 * */
	public double getUtilization()
	{
		if ((capacity <= Configuration.precisionFactor) && (cache_totalOccupiedCapacity > Configuration.precisionFactor)) return Double.POSITIVE_INFINITY;
		return capacity <= Configuration.precisionFactor? 0 : cache_totalOccupiedCapacity / capacity;
	}
	
	/** <p>Returns the link occupied capacity (in link capacity units). </p>
	 * @return The occupied capacity as described above
	 * */
	public double getOccupiedCapacity()
	{
		return cache_totalOccupiedCapacity <= Configuration.precisionFactor? 0 : cache_totalOccupiedCapacity;
	}
	
	/** <p>Returns the link occupied capacity (in link capacity units). </p>
	 * @return The occupied capacity as described above
	 * */
	public double getOccupiedCapacityOnlyBackupRoutes ()
	{
		return cache_traversingRoutes.keySet().stream ().filter(e -> e.isBackupRoute()).mapToDouble(e -> e.getOccupiedCapacity(this)).sum ();
	}

	/**
	 * <p>Returns the link length in km.</p>
	 * @return The link length in km
	 */
	public double getLengthInKm()
	{
		if (coupledLowerOrThisLayerDemand != null) return coupledLowerOrThisLayerDemand.getWorstCaseLengthInKm();
		if (coupledLowerLayerMulticastDemand != null) return coupledLowerLayerMulticastDemand.getWorstCaseLengthInKm();
		return lengthInKm;
	}

	/**
	 * <p>Sets the link length in km.</p>
	 * @param lengthInKm New link length in km (must be non-negative)
	 */
	public void setLengthInKm(double lengthInKm)
	{
		if (this.isCoupled()) throw new Net2PlanException ("The length of coupled links cannot be changed");
		if (lengthInKm == this.lengthInKm) return;
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (lengthInKm < 0) throw new Net2PlanException ("Link lengths cannot be negative");
		this.lengthInKm = lengthInKm;
		this.updateWorstCasePropagationTraversingUnicastDemandsAndMaybeRoutes();
	}

	/** <p>Returns the link propagation speed in km per second.</p>
	 * @return The link propagation speed in km per second
	 */
	public double getPropagationSpeedInKmPerSecond()
	{
		return propagationSpeedInKmPerSecond;
	}

	/**
	 * <p>Returns the sum of the traffic carried by the {@link com.net2plan.interfaces.networkDesign.MulticastTree MulticastTree} traversing the link.</p>
	 * @return The carried traffic (in traffic units)
	 */
	public double getMulticastCarriedTraffic()
	{
		double accum = 0; for (MulticastTree t : cache_traversingTrees) accum += t.getCarriedTraffic();
		return accum;
	}

	/**
	 * <p>Returns the sum of the occupied link capacity by the traffic of the {@link com.net2plan.interfaces.networkDesign.MulticastTree Multicast Trees} traversing the link.</p>
	 * @return The occupied link capacity (link capacity units)
	 */
	public double getMulticastOccupiedLinkCapacity()
	{
		double accum = 0; for (MulticastTree t : cache_traversingTrees) accum += t.getOccupiedLinkCapacity();
		return accum;
	}

	/** <p>Sets the link propagation speed in km per second.</p>
	 * @param speed The speed in km per second
	 */
	public void setPropagationSpeedInKmPerSecond(double speed)
	{
		if (this.isCoupled()) throw new Net2PlanException ("The propagation speed of coupled links cannot be changed");
		if (this.propagationSpeedInKmPerSecond == speed) return;
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (speed <= 0) throw new Net2PlanException ("Propagation speeds must be positive");
		this.propagationSpeedInKmPerSecond = speed;
		this.updateWorstCasePropagationTraversingUnicastDemandsAndMaybeRoutes();
	}


	/** <p>Returns the link propagation delay in miliseconds.</p>
	 * @return The propagation delay in miliseconds
	 */
	public double getPropagationDelayInMs()
	{
		if (coupledLowerOrThisLayerDemand != null) return coupledLowerOrThisLayerDemand.getWorstCasePropagationTimeInMs();
		if (coupledLowerLayerMulticastDemand != null) return coupledLowerLayerMulticastDemand.getWorseCasePropagationTimeInMs();
		return ((propagationSpeedInKmPerSecond == Double.MAX_VALUE) || (propagationSpeedInKmPerSecond <= 0))? 0 : 1000 * lengthInKm / propagationSpeedInKmPerSecond;
	}

	/**
	 * <p>Returns whether the link is up (not in failure) or down.</p>
	 * @return {@code true} if up, {@code false} if down
	 */
	public boolean isUp()
	{
		return isUp;
	}

	/**
	 * <p>Returns whether the link is down (in failure) or not.</p>
	 * @return {@code True} if down, {@code false} if up
	 */
	public boolean isDown()
	{
		return !isUp;
	}

	/** <p>Returns true if the link is oversubscribed, up to the Net2Plan defined precision factor. That is, if the occupied capacity by the traversing
	 * traffic exceeds the link capacity plus said precision factor. In {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, the link capacity includes any reserved capacity, and the occupied
	 * link capacity includes that from the traversing routes and protection segments. </p>
	 * @return {@code True} if oversubscribed, {@code false} otherwise
	 * @see com.net2plan.interfaces.networkDesign.Configuration
	 */
	public boolean isOversubscribed ()
	{
		return (cache_totalOccupiedCapacity > capacity + Configuration.precisionFactor);
	}
	
    /** <p>Returns the amount of traffic over the link capacity. </p>
     * @return {@code True} if oversubscribed, {@code false} otherwise
     * @see com.net2plan.interfaces.networkDesign.Configuration
     */
    public double getOversubscribedTraffic ()
    {
        return Math.max(0, cache_totalOccupiedCapacity - (capacity + Configuration.precisionFactor)); 
    }
    

    /** <p>Returns the Shared Risk Groups ({@link com.net2plan.interfaces.networkDesign.SharedRiskGroup SRGs}) the link belongs to. The link fails when any of these SRGs is down.</p>
	 * @return the set of SRGs
	 */
	public SortedSet<SharedRiskGroup> getSRGs()
	{
	    final SortedSet<SharedRiskGroup> res = new TreeSet<> (cache_nonDynamicSrgs);
        for (SharedRiskGroup srg : netPlan.cache_dynamicSrgs)
            if (srg.getLinksAllLayers().contains(this)) res.add(srg);
        return res;
	}

	/**
	 * <p>Returns the {@link com.net2plan.interfaces.networkDesign.Route Routes} traversing the link. If network layer routing type is not
	 * {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, an exception is thrown.</p>
	 * @return An unmodifiable {@code SortedSet} of traversing routes. If no routes exist, an empty {@code SortedSet} is returned
	 */
	public SortedSet<Route> getTraversingRoutes()
	{
		return new TreeSet<> (cache_traversingRoutes.keySet());
	}

    /**
     * <p>Returns a map with the routes traversing this link, and the number of times they traverse it
     * @return see above
     */
    public SortedMap<Route,Integer> getTraversingRoutesAndMultiplicity()
    {
        return Collections.unmodifiableSortedMap(cache_traversingRoutes);
    }


    /**
	 * <p>Returns the number of routes traversing this link. 
	 * @return see above
	 */
	public int getNumberOfTraversingRoutes()
	{
		return cache_traversingRoutes.size();
	}

	/**
	 * <p>Returns the {@link com.net2plan.interfaces.networkDesign.MulticastTree Multicast Trees} traversing the link.</p>
	 * @return An unmodifiable {@code SortedSet} of traversing multicast trees. If no tree exists, an empty set is returned
	 */
	public SortedSet<MulticastTree> getTraversingTrees()
	{
		return Collections.unmodifiableSortedSet(cache_traversingTrees);
	}

	/**
	 * Returns the number of multicast trees traversing this link
	 * @return see above
	 */
	public int getNumberOfTraversingTrees()
	{
		return cache_traversingTrees.size();
	}
	
	/** Returns the number of forwarding rules defined for this link (with non-zero split factor)
	 * @return see above
	 */
	public int getNumberOfForwardingRules ()
	{
		return cacheHbH_frs.size();
	}

	SortedSet<Link> getIntraLayerUpPropagationIncludingMe ()
	{
		final SortedSet<Link> res = new TreeSet<> ();
		res.add(this);
		for (Demand d : Sets.union(this.cacheHbH_frs.keySet() , cache_traversingRoutes.keySet().stream().map(r->r.getDemand()).collect(Collectors.toSet())))
			if (d.isCoupledInSameLayer())
			{
				final Link upCoupledLink = d.getCoupledLink();
				if (!res.contains(upCoupledLink))
				{
					final SortedSet<Link> upperLinks = upCoupledLink.getIntraLayerUpPropagationIncludingMe();
					res.addAll(upperLinks);
				}
			}
		return res;
	}
	
	SortedSet<Link> getIntraLayerDownPropagationIncludingMe ()
	{
		final SortedSet<Link> res = new TreeSet<> ();
		res.add(this);
		if (!isCoupledInSameLayer()) return res;
		res.addAll(this.coupledLowerOrThisLayerDemand.getLinksThisLayerNonZeroFrOrTravRoutes(true));
		return res;
	}

	
	/**
	 * <p>Couples the link to a unicast {@link com.net2plan.interfaces.networkDesign.Demand} in the lower layer.</p>
	 * @param demand The demand to be coupled to
	 */
	public void coupleToLowerLayerDemand (Demand demand)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		demand.coupleToUpperOrSameLayerLink(this);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}
	
	/**
	 * <p>Creates a new {@link com.net2plan.interfaces.networkDesign.Demand Demand} in the given layer, with same end nodes as the link, and then couples the link to the new created demand.</p>
	 * @param newDemandLayer The layer where the demand will be created (and coupled to the link)
	 * @param routingTypeDemand Routing type
	 * @return The created demand
	 */
	public Demand coupleToNewDemandCreated (NetworkLayer newDemandLayer , RoutingType routingTypeDemand)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		newDemandLayer.checkAttachedToNetPlanObject(this.netPlan);
		Demand newDemand = netPlan.addDemand(originNode ,  destinationNode , capacity , routingTypeDemand , null , newDemandLayer);
		try { newDemand.coupleToUpperOrSameLayerLink(this); } catch (RuntimeException e) { newDemand.remove (); throw e; }
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		return newDemand;
	}
	
	/**
	 * <p>Removes all forwarding rules associated to the link.</p>
	 * <p>If routing type is not {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}, an exeception is thrown.</p>
	 */
	public void removeAllForwardingRules()
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		for (Demand d : new ArrayList<> (this.cacheHbH_frs.keySet()))
		{
			final SortedMap<Link,Double> frsThatDemand = new TreeMap<> (d.cacheHbH_frs);
			frsThatDemand.remove(this);
			d.updateHopByHopRoutingToGivenFrs(frsThatDemand);
		}
		this.cacheHbH_frs.clear();
		this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.clear();
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>If the link was added using addLinkBidirectional()
	 * returns the link in the opposite direction (if it was not previously removed). Returns {@code null} otherwise.</p>
	 * @return The link in the opposite direction, or {@code null} if none
	 */
	public Link getBidirectionalPair()
	{
		return bidirectionalPair;
	}

	/**
	 * <p>Sets the given link as the bidirectional pair of this link. If any of the links was previously set as bidirectional 
	 * link of other link, such relation is removed. The links must bein the same layer, and have opposite end nodes
	 * @param e the other link
	 */
	public void setBidirectionalPair(Link e)
	{
		checkAttachedToNetPlanObject();
		e.checkAttachedToNetPlanObject(this.netPlan);
		if (e.layer != this.layer) throw new Net2PlanException ("Wrong layer");
		if (e.originNode != this.destinationNode || this.originNode != e.destinationNode) throw new Net2PlanException ("Wrong end nodes");
		if (this.bidirectionalPair != null) this.bidirectionalPair.bidirectionalPair = null;
		if (e.bidirectionalPair != null) e.bidirectionalPair.bidirectionalPair = null;
		this.bidirectionalPair = e;
		e.bidirectionalPair = this;
	}

	/**
	 * Returns true if the link is bidirectional. That is, it has an associated link in the other direction
	 * @return see above
	 */
	public boolean isBidirectional()
	{
		return bidirectionalPair != null;
	}

	/**
	 * Creates a link in the opposite direction as this, and with the same attributes, and associate both as bidirectional pairs.
	 * If this link is already a bidirectional link, makes nothing and returns null
	 * @return the newly created link
	 */
	public Link createBidirectionalPair ()
	{
		checkAttachedToNetPlanObject();
		if (this.isBidirectional()) return null;
		final Link e = netPlan.addLink(this.destinationNode, this.originNode, this.getCapacity(), this.getLengthInKm(), this.getPropagationDelayInMs(), this.attributes, this.layer);
		this.bidirectionalPair = e;
		e.bidirectionalPair = this;
		return e;
	}
	
	/**
	 * <p>Removes the link. The routing is updated removing any associated routes, protection segments, multicast trees or forwarding rules.</p>
	 * <p>If the link is coupled to a unicast or multicast demand, it is first decoupled. In the multicast case, this means that the multicast
	 * demand is decoupled from <b>ALL</b> the links it is coupled to. </p>
	 */
	public void remove()
	{
		final boolean previousErrorHandling = ErrorHandling.DEBUG; 
		ErrorHandling.DEBUG = false;
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();

		if (this.coupledLowerOrThisLayerDemand != null) 
			this.coupledLowerOrThisLayerDemand.decouple();
		else if (this.coupledLowerLayerMulticastDemand != null)
			this.coupledLowerLayerMulticastDemand.decouple();
		
		if (bidirectionalPair != null) this.bidirectionalPair.bidirectionalPair = null;
		
		layer.cache_linksDown.remove (this);
		layer.cache_linksZeroCap.remove(this);
		netPlan.cache_id2LinkMap.remove(id);
		originNode.cache_nodeOutgoingLinks.remove (this);
		destinationNode.cache_nodeIncomingLinks.remove (this);
		layer.cache_nodePairLinksThisLayer.get(Pair.of(originNode, destinationNode)).remove(this);
		
		for (SharedRiskGroup srg : this.cache_nonDynamicSrgs) srg.linksIfNonDynamic.remove(this);
		for (MulticastTree tree : new LinkedList<MulticastTree> (cache_traversingTrees)) tree.remove ();

		for (Route route : new TreeSet<Route> (cache_traversingRoutes.keySet())) route.remove ();
		this.removeAllForwardingRules();

		NetPlan.removeNetworkElementAndShiftIndexes (layer.links , index);
        for (String tag : tags) netPlan.cache_taggedElements.get(tag).remove(this);

		ErrorHandling.DEBUG = previousErrorHandling;
		final NetPlan npOld = this.netPlan;
	    removeId();
	    if (ErrorHandling.isDebugEnabled()) npOld.checkCachesConsistency();
	}
	
	/**
	 * <p>Sets the failure state of the link: up or down and returns the previous failure state. The routing is updated if the failure state changed.</p>
	 * @param setAsUp The new failure state: {@code true} if up, {@code false} if down
	 * @return the previous failure state
	 */
	public boolean setFailureState (boolean setAsUp)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (this.isUp == setAsUp) return this.isUp;
		List<Link> aux = new LinkedList<Link> (); aux.add(this);
		if (setAsUp) netPlan.setLinksAndNodesFailureState (aux , null , null , null); else netPlan.setLinksAndNodesFailureState (null, aux , null , null); 
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		return !setAsUp; // the previous state
	}

	/**
	 * <p>Returns a {@code String} representation of the link.</p>
	 * @return {@code String} representation of the link
	 */
	@Override
    public String toString () { return "e" + index + "-L" + layer.index + "(" + originNode.getName() + "->" + destinationNode.getName() + ")"; }

	@Override
    void checkCachesConsistency ()
	{
		super.checkCachesConsistency ();
		if (layer.netPlan != this.netPlan) throw new RuntimeException ("Bad");
		if (!layer.links.contains(this)) throw new RuntimeException ("Bad");
		if (this.bidirectionalPair != null)
		{
			if (this.bidirectionalPair.bidirectionalPair != this) throw new RuntimeException ("Bad");
			if (this.bidirectionalPair.netPlan != this.netPlan) throw new RuntimeException ("Bad");
			if (this.bidirectionalPair.originNode != this.destinationNode) throw new RuntimeException ("Bad");
			if (this.bidirectionalPair.destinationNode != this.originNode) throw new RuntimeException ("Bad");
			if (this.bidirectionalPair.layer != this.layer) throw new RuntimeException ("Bad");
		}
		
		if (isUp && layer.cache_linksDown.contains(this)) throw new RuntimeException ("Bad");
		if (!isUp && !layer.cache_linksDown.contains(this)) throw new RuntimeException ("Bad");
		if (isCoupled ()) if (capacity > 0 && (!originNode.isUp || !destinationNode.isUp)) throw new RuntimeException ("Bad. Link: " + this + ", capacity: " + this.capacity + ", isUp: " + this.isUp + ", origin up: " + originNode.isUp + ", dest up: " + destinationNode.isUp);
		
		if (capacity < Configuration.precisionFactor && !layer.cache_linksZeroCap.contains(this)) throw new RuntimeException ("Bad");
		if (capacity >= Configuration.precisionFactor && layer.cache_linksZeroCap.contains(this)) throw new RuntimeException ("Bad");
		if (!isUp)
		{
			if (Math.abs(cache_totalCarriedTraffic) > 1e-3)
				throw new RuntimeException ("Bad");
			if (Math.abs(cache_totalOccupiedCapacity) > 1e-3) throw new RuntimeException ("Bad");
			for (Route r : cache_traversingRoutes.keySet()) if ((r.getCarriedTraffic() != 0) || (r.getOccupiedCapacity(this) != 0)) throw new RuntimeException ("Bad");
			for (MulticastTree r : cache_traversingTrees) if ((r.getCarriedTraffic() != 0) || (r.getOccupiedLinkCapacity() != 0)) throw new RuntimeException ("Bad");
		}
		
		if (this.isCoupledInSameLayer())
		{
			final SortedSet<Link> upIntraLayerPropLinks = getIntraLayerUpPropagationIncludingMe();
			final SortedSet<Link> downIntraLayerPropLinks = coupledLowerOrThisLayerDemand.getLinksThisLayerNonZeroFrOrTravRoutes(true);
			if (!Sets.intersection(upIntraLayerPropLinks, downIntraLayerPropLinks).isEmpty()) throw new RuntimeException ();
		}

		for (SharedRiskGroup srg : netPlan.srgs) 
		{
		    if (!srg.isDynamicSrg())
	            if (this.cache_nonDynamicSrgs.contains(srg) != srg.getLinksAllLayers().contains(this)) 
	                throw new RuntimeException ("Bad");
		}

		for (Demand d : cacheHbH_frs.keySet())
			if (d.isSourceRouting()) throw new RuntimeException ();
		for (Demand d : cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.keySet())
			if (d.isSourceRouting()) throw new RuntimeException ();
		for (Route r : cache_traversingRoutes.keySet())
			if (!r.getDemand().isSourceRouting()) throw new RuntimeException ();
		
		
		double check_totalCarriedTraffic = 0;
		double check_totalOccupiedCapacity = 0;
		SortedMap<String,Double> check_occupAndViolationPerQos = new TreeMap<> ();
		for (Route route : layer.routes)
		{
			if (!route.getDemand().isSourceRouting()) throw new RuntimeException ();
			if (this.cache_traversingRoutes.containsKey(route) != route.cache_seqLinksRealPath.contains(this)) throw new RuntimeException ("Bad. link: " + this + ", route: " + route + ", this.cache_traversingRoutes: " + this.cache_traversingRoutes + ", route.seqLinksRealPath: "+  route.cache_seqLinksRealPath);
			if (this.cache_traversingRoutes.containsKey(route))
			{
				int numPasses = 0; for (Link linkRoute : route.cache_seqLinksRealPath) if (linkRoute == this) numPasses ++; if (numPasses != this.cache_traversingRoutes.get(route)) throw new RuntimeException ("Bad");
				check_totalCarriedTraffic += route.getCarriedTraffic();
				check_totalOccupiedCapacity += route.getOccupiedCapacity(this);
				final String qos = route.demand.getQosType();
				if (check_occupAndViolationPerQos.containsKey(qos))
					check_occupAndViolationPerQos.put(qos , check_occupAndViolationPerQos.get(qos) + route.getOccupiedCapacity(this));
				else
					check_occupAndViolationPerQos.put(qos , route.getOccupiedCapacity(this));
			}
		}

//			System.out.println(cacheHbH_frs.keySet());
//			System.out.println(cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.keySet());
		if (!cacheHbH_frs.keySet().containsAll(cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.keySet())) throw new RuntimeException();

		for (Demand d : this.cacheHbH_frs.keySet())
		{
			final double splitFactor = cacheHbH_frs.get(d);
			if (splitFactor <= 0) throw new RuntimeException();
			if (splitFactor > 1 + Configuration.precisionFactor)throw new RuntimeException("split: " + splitFactor);
			if (d.cacheHbH_frs.get(this) != splitFactor) throw new RuntimeException ();
		}
		for (Demand d : this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.keySet())
		{
//				System.out.println("Demand " + d + ", cache frs: " + d.cacheHbH_frs);
			final double normTraffic =  this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.get(d).getFirst();
			final double cap =  this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.get(d).getSecond();
			if (!d.cacheHbH_normCarriedOccupiedPerLinkCurrentState.get(this).equals(Pair.of(normTraffic, cap))) throw new RuntimeException ();
//				System.out.println("Nomr " + normTraffic + ", d.carried " + d.carriedTraffic + ", capOccupied: " + cap);
			if (Math.abs(normTraffic * d.offeredTraffic - cap) > 1e-3) throw new RuntimeException();
			check_totalCarriedTraffic += cap;
			check_totalOccupiedCapacity += cap;
			final String qos = d.getQosType();
			if (check_occupAndViolationPerQos.containsKey(qos))
				check_occupAndViolationPerQos.put(qos , check_occupAndViolationPerQos.get(qos) + cap);
			else
				check_occupAndViolationPerQos.put(qos , cap);
		}
		for (MulticastTree tree : layer.multicastTrees)
		{
			if (this.cache_traversingTrees.contains(tree) != tree.linkSet.contains(this)) throw new RuntimeException ("Bad");
			if (this.cache_traversingTrees.contains(tree))
			{
				check_totalCarriedTraffic += tree.getCarriedTraffic();
				check_totalOccupiedCapacity += tree.getOccupiedLinkCapacity();
				final String qos = tree.getMulticastDemand().getQosType();
				if (check_occupAndViolationPerQos.containsKey(qos))
					check_occupAndViolationPerQos.put(qos , check_occupAndViolationPerQos.get(qos) + tree.getOccupiedLinkCapacity());
				else
					check_occupAndViolationPerQos.put(qos , tree.getOccupiedLinkCapacity());
			}
		}

		if (Math.abs(cache_totalCarriedTraffic - check_totalCarriedTraffic) > 1E-3) 
			throw new RuntimeException ("Bad: Link: " + this + ". carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments: " + cache_totalCarriedTraffic + ", check_carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments: " + check_totalCarriedTraffic);

		if (cache_totalOccupiedCapacity != check_totalOccupiedCapacity)
		{
			if (check_totalOccupiedCapacity != 0)
			{
				if ((cache_totalOccupiedCapacity / check_totalOccupiedCapacity) > (1 + 1E-3)) throw new RuntimeException();
			} else
			{
				if (cache_totalOccupiedCapacity > 1E-3 || cache_totalOccupiedCapacity < -1E-3) throw new RuntimeException();
			}
		}

		if (coupledLowerOrThisLayerDemand != null)
			if (!coupledLowerOrThisLayerDemand.coupledUpperOrSameLayerLink.equals (this)) throw new RuntimeException ("Bad");
		if (coupledLowerLayerMulticastDemand != null)
			if (!coupledLowerLayerMulticastDemand.coupledUpperLayerLinks.containsValue(this)) throw new RuntimeException ("Bad");

		double check_totalOccupied = 0;
		double check_totalViolated = 0;

		final SortedMap<String,Pair<Double,Double>> qos2OccupAndViol = getPerQoSOccupationAndQosViolationMap ();

		final SortedSet<String> qosUnion = new TreeSet<> (Sets.union(qos2OccupAndViol.keySet(), check_occupAndViolationPerQos.keySet()));
		for (String qos : qosUnion)
		{
			final double occupiedQosMap = qos2OccupAndViol.containsKey(qos)? qos2OccupAndViol.get(qos).getFirst() : 0;
			final double violatedQosMap = qos2OccupAndViol.containsKey(qos)? qos2OccupAndViol.get(qos).getSecond() : 0;
			assert violatedQosMap <= occupiedQosMap + Configuration.precisionFactor;
			final double occupiedCheck = check_occupAndViolationPerQos.containsKey(qos)? check_occupAndViolationPerQos.get(qos) : 0;
			assert Math.abs(occupiedCheck - occupiedQosMap) < 1e-3;
			check_totalOccupied += occupiedQosMap;
			check_totalViolated += violatedQosMap;
		}
		assert Math.abs(check_totalOccupied - this.getOccupiedCapacity()) < 1e-3;
		assert check_totalViolated <= check_totalOccupied + Configuration.precisionFactor;
		assert getCapacity() + Configuration.precisionFactor >= (check_totalOccupied - check_totalViolated); 
	}
	
	/** Returns the set of QoS types that have violated traffic in this link
	 * @return  see above
	 */
	public SortedSet<String> getQosViolated () 
	{ 
		final SortedMap<String,Pair<Double,Double>> qos2OccupAndViol = getPerQoSOccupationAndQosViolationMap ();
		return qos2OccupAndViol.entrySet().stream().filter(e->e.getValue().getSecond() > Configuration.precisionFactor).map(e->e.getKey()).collect(Collectors.toCollection(TreeSet::new)); 
	}

	/** Returns true if the traffic of the given QoS is violating the QoS in the link
	 * @param qosType  see above
	 * @return  see above
	 */
	public boolean isQoSViolated (String qosType) 
	{ 
		final SortedMap<String,Pair<Double,Double>> qos2OccupAndViol = getPerQoSOccupationAndQosViolationMap ();
		final Pair<Double,Double> pair = qos2OccupAndViol.get(qosType);
		return pair == null? false : pair.getSecond() > Configuration.precisionFactor;
	}
	
	void updateLinkTrafficAndOccupation ()
	{
		/* Add the info of the demands with traversing routes */
		this.cache_totalCarriedTraffic = 0;
		this.cache_totalOccupiedCapacity = 0;
		for (Entry<Route,Integer> travRouteInfo : cache_traversingRoutes.entrySet())
		{
			final Route r = travRouteInfo.getKey();
			final double carriedTraffic = r.getCarriedTraffic();
			final double occupiedCapacity = r.getOccupiedCapacity(this);
			this.cache_totalCarriedTraffic += carriedTraffic;
			this.cache_totalOccupiedCapacity += occupiedCapacity;
		}

		/* Add the info of the demands with forwarding rules */
		for (Entry<Demand,Pair<Double,Double>> entryInfo : this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.entrySet())
		{
			final Demand demand = entryInfo.getKey();
			final double occupiedCapacityAndCarriedTraffic = entryInfo.getValue().getSecond();
			this.cache_totalCarriedTraffic += occupiedCapacityAndCarriedTraffic;
			this.cache_totalOccupiedCapacity += occupiedCapacityAndCarriedTraffic;
		}
		for (MulticastTree t : cache_traversingTrees)
		{
			final MulticastDemand demand = t.getMulticastDemand();
			final double occupiedCapacity = t.getOccupiedLinkCapacity();
			final double carriedTraffic = t.getCarriedTraffic();
			this.cache_totalCarriedTraffic += carriedTraffic;
			this.cache_totalOccupiedCapacity += occupiedCapacity;
		}
	}

	/** Returns the set of demands that could potentially put traffic in this link, 
	 *  according to the routes/forwarding rules defined. 
	 *  Potentially carrying traffic means that (i) in source routing, down routes are not included, but all up routes 
	 *  are considered even if the carry zero traffic, 
	 * (ii) in hop-by-hop routing the demands with zero offered traffic are also included. 
	 * All the links are considered primary.
	 * The method returns  three set: (i) demands putting traffic in primary routes (or hop-by-hop routing), 
	 * (ii) in source routing, demands with backup routes traversing this link, (iii) multicast demands 
	 * with trees traversing this link
	 * @return see above
	 */
	public Triple<SortedSet<Demand>,SortedSet<Demand>,SortedSet<MulticastDemand>> getDemandsPotentiallyTraversingThisLink ()
	{
		final Triple<SortedMap<Demand,SortedSet<Link>>,SortedMap<Demand,SortedSet<Link>>,SortedMap<Pair<MulticastDemand,Node>,SortedSet<Link>>> res = 
				getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  ();
		return Triple.of(new TreeSet<> (res.getFirst().keySet()), new TreeSet<> (res.getSecond().keySet()), res.getThird().keySet().stream().map(p->p.getFirst()).collect(Collectors.toCollection(TreeSet::new)));
	}
	
	/** Returns the set of links in this layer (including this) that carry the traffic that traverses this link, before and after traversing it,
	 *  according to the routes/forwarding rules defined. 
	 *  Potentially carrying traffic means that (i) in source routing, down routes are not included, but all up routes 
	 *  are considered even if the carry zero traffic, 
	 * (ii) in hop-by-hop routing the links are computed even if the demand offered traffic is zero. All the links are considered primary.
	 * The method returns  three maps: (i) first map, with one entry per demand whose traffic traverses this node, associated to 
	 * the other links (aside of this) that this link traffic also traverses, but are not part of backup routes, 
	 * (ii) second map, the same as before, but only links belonging to backup routes, (iii) other links with multicast trees that traverse this 
	 * node, being a key its associated multicast demands.
	 *  
	 * @return see above
	 */
	public Triple<SortedMap<Demand,SortedSet<Link>>,SortedMap<Demand,SortedSet<Link>>,SortedMap<Pair<MulticastDemand,Node>,SortedSet<Link>>> getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  ()
	{
		final SortedMap<Demand,SortedSet<Link>> resPrimary = new TreeMap<> ();
		final SortedMap<Demand,SortedSet<Link>> resBackup = new TreeMap<> ();

		for (Demand travDemands : this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.keySet())
			resPrimary.put(travDemands , new TreeSet<> (travDemands.cacheHbH_normCarriedOccupiedPerLinkCurrentState.keySet()));
		
		for (Route r : cache_traversingRoutes.keySet())
		{
			if (r.isDown()) continue;
			for (Link e : r.getSeqLinks())
			{
				if (r.isBackupRoute())
				{
					SortedSet<Link> linksSoFar = resBackup.get(r.getDemand());
					if (linksSoFar == null) { linksSoFar = new TreeSet<> (); resBackup.put(r.getDemand(), linksSoFar); }
					linksSoFar.add(e);
				}
				else 
				{
					SortedSet<Link> linksSoFar = resPrimary.get(r.getDemand());
					if (linksSoFar == null) { linksSoFar = new TreeSet<> (); resPrimary.put(r.getDemand(), linksSoFar); }
					linksSoFar.add(e);
				}
			}
		}
		
		SortedMap<Pair<MulticastDemand,Node>,SortedSet<Link>> resMCast = new TreeMap<> ();
		for (MulticastTree t : cache_traversingTrees)
		{
			for (Node egressNode : t.getEgressNodesReached())
			{
				final List<Link> pathToEgressNode = t.getSeqLinksToEgressNode(egressNode);
				if (!pathToEgressNode.contains(this)) continue;
				if (!netPlan.isUp (pathToEgressNode)) continue;
				SortedSet<Link> linksSoFar = resMCast.get(Pair.of(t.getMulticastDemand() , egressNode));
				if (linksSoFar == null) { linksSoFar = new TreeSet<> (); resMCast.put(Pair.of(t.getMulticastDemand() , egressNode), linksSoFar); }
				linksSoFar.addAll(pathToEgressNode);
			}
		}
		return Triple.of(resPrimary,resBackup,resMCast);
	}


	/** Returns the set of links in lower layers carry the traffic that traverses this link, before and after traversing it,
	 *  according to the routes/forwarding rules defined. 
	 *  Potentially carrying traffic means that (i) in source routing, down routes are not included, but all up routes 
	 *  are considered even if the carry zero traffic, 
	 * (ii) in hop-by-hop routing the links are computed even if the demand offered traffic is zero. All the links are considered primary.
	 * The method returns  three maps: (i) first map, with one entry per demand whose traffic traverses this node, associated to 
	 * the other links (aside of this) that this link traffic also traverses, but are not part of backup routes, 
	 * (ii) second map, the same as before, but only links belonging to backup routes, (iii) other links with multicast trees that traverse this 
	 * node, being a key its associated multicast demands.
	 * <p>Note: this link is not included in any set an any map </p>
	 *  
	 * capacity in it
	 * @return see above
	 */
	public Triple<SortedMap<Demand,SortedSet<Link>>,SortedMap<Demand,SortedSet<Link>>,SortedMap<Pair<MulticastDemand,Node>,SortedSet<Link>>> getLinksDownPropagationPotentiallyCarryingTrafficTraversingThisLink  ()
	{
		final SortedMap<Demand,SortedSet<Link>> resPrimary = new TreeMap <> ();
		final SortedMap<Demand,SortedSet<Link>> resBackup = new TreeMap <> ();
		final SortedMap<Pair<MulticastDemand,Node>,SortedSet<Link>> resMCast = new TreeMap <> ();
		
		if (!isCoupled()) return Triple.of(resPrimary, resBackup,resMCast);
		if (!this.isUp) return Triple.of(resPrimary, resBackup,resMCast);
		
		if (this.coupledLowerOrThisLayerDemand != null)
		{
			final Pair<SortedSet<Link>,SortedSet<Link>> pair = coupledLowerOrThisLayerDemand.getLinksNoDownPropagationPotentiallyCarryingTraffic  ();
			resPrimary.put(coupledLowerOrThisLayerDemand, pair.getFirst());
			resBackup.put(coupledLowerOrThisLayerDemand, pair.getSecond());
			for (Link lowerLayerLink_primary : pair.getFirst())
			{
				final Triple<SortedMap<Demand,SortedSet<Link>>,SortedMap<Demand,SortedSet<Link>>,SortedMap<Pair<MulticastDemand,Node>,SortedSet<Link>>> linksLayersTwoBelow = lowerLayerLink_primary.getLinksDownPropagationPotentiallyCarryingTrafficTraversingThisLink  ();
				resPrimary.putAll(linksLayersTwoBelow.getFirst()); // primary traffic in lower layer, and in two layers below -> is primary
				resBackup.putAll(linksLayersTwoBelow.getSecond()); // primary traffic in lower layer, and backup in two layers below -> is backup
				resMCast.putAll (linksLayersTwoBelow.getThird());
			}
			for (Link lowerLayerLink_backup : pair.getSecond())
			{
				final Triple<SortedMap<Demand,SortedSet<Link>>,SortedMap<Demand,SortedSet<Link>>,SortedMap<Pair<MulticastDemand,Node>,SortedSet<Link>>> linksLayersTwoBelow = lowerLayerLink_backup.getLinksDownPropagationPotentiallyCarryingTrafficTraversingThisLink  ();
				resBackup.putAll(linksLayersTwoBelow.getFirst()); // primary traffic in lower layer, and in two layers below -> is primary
				resBackup.putAll(linksLayersTwoBelow.getSecond()); // primary traffic in lower layer, and backup in two layers below -> is backup
				resMCast.putAll (linksLayersTwoBelow.getThird());
			}
		}
		else if (this.coupledLowerLayerMulticastDemand != null)
		{
			SortedSet<Link> linksLowerLayer = coupledLowerLayerMulticastDemand.getLinksNoDownPropagationPotentiallyCarryingTraffic  (this.destinationNode);
			resMCast.put(Pair.of(coupledLowerLayerMulticastDemand, this.destinationNode) , linksLowerLayer);
			for (Link lowerLayerLink : linksLowerLayer)
			{
				final Triple<SortedMap<Demand,SortedSet<Link>>,SortedMap<Demand,SortedSet<Link>>,SortedMap<Pair<MulticastDemand,Node>,SortedSet<Link>>> linksLayersTwoBelow = lowerLayerLink.getLinksDownPropagationPotentiallyCarryingTrafficTraversingThisLink  ();
				resPrimary.putAll(linksLayersTwoBelow.getFirst()); // primary traffic in lower layer, and in two layers below -> is primary
				resBackup.putAll(linksLayersTwoBelow.getSecond()); // primary traffic in lower layer, and backup in two layers below -> is backup
				resMCast.putAll (linksLayersTwoBelow.getThird());
			}
		}
		return Triple.of(resPrimary,resBackup,resMCast);
	}

//	public Triple<SortedMap<Demand,SortedSet<Link>>,SortedMap<Demand,SortedSet<Link>>,SortedMap<Pair<MulticastDemand,Node>,SortedSet<Link>>>
//		getLinksUpperLayersPotentiallyPuttingTrafficInThisLink  (boolean assumeNoFailureState , Triple<SortedSet<Demand>,SortedSet<Demand>,SortedSet<Pair<MulticastDemand,Node>>> thisLayerDemandsPuttingTrafficThisLink)
//	{
//		if (thisLayerDemandsPuttingTrafficThisLink == null)
//		{
//			Triple<SortedMap<Demand,SortedSet<Link>>,SortedMap<Demand,SortedSet<Link>>,SortedMap<Pair<MulticastDemand,Node>,SortedSet<Link>>> triple = 
//					this.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  ();
//			thisLayerDemandsPuttingTrafficThisLink = Triple.of(triple.getFirst().keySet(), triple.getSecond().keySet(), triple.getThird().keySet());
//		}
//		
//		/* Add upper layer info*/
//		final SortedMap<Demand,SortedSet<Link>> res_unicastPrimary = new TreeMap<> ();
//		final SortedMap<Demand,SortedSet<Link>> res_unicastBackup = new TreeMap<> ();
//		final SortedMap<Pair<MulticastDemand,Node>,SortedSet<Link>> res_multicast = new TreeMap<> ();
//
//		/* Propagate to two layers up, and accumulate results */
//		for (Demand thisLayerDemand : thisLayerDemandsPuttingTrafficThisLink.getFirst())
//		{
//			if (thisLayerDemand.isCoupled())
//			{
//				final Link upperLayerLink = thisLayerDemand.coupledUpperLayerLink;
//				Triple<SortedMap<Demand,SortedSet<Link>>,SortedMap<Demand,SortedSet<Link>>,SortedMap<Pair<MulticastDemand,Node>,SortedSet<Link>>> res_upperLayer =
//						upperLayerLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink();
//				res_unicastPrimary.putAll(res_upperLayer.getFirst()); // primary traffic in primary links -> primary
//				res_unicastBackup.putAll(res_upperLayer.getSecond()); // primary traffic in backup links -> backup
//				res_multicast.putAll(res_upperLayer.getThird()); 
//			}
//		}
//		for (Demand thisLayerDemand : thisLayerDemandsPuttingTrafficThisLink.getSecond())
//		{
//			if (thisLayerDemand.isCoupled())
//			{
//				final Link upperLayerLink = thisLayerDemand.coupledUpperLayerLink;
//				Triple<SortedMap<Demand,SortedSet<Link>>,SortedMap<Demand,SortedSet<Link>>,SortedMap<Pair<MulticastDemand,Node>,SortedSet<Link>>> res_upperLayer =
//						upperLayerLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink();
//				res_unicastBackup.putAll(res_upperLayer.getFirst()); // backup traffic in primary links -> backup
//				res_unicastBackup.putAll(res_upperLayer.getSecond()); // backup traffic in backup links -> backup
//				res_multicast.putAll(res_upperLayer.getThird()); 
//			}
//		}
//
//		for (Pair<MulticastDemand,Node> thisLayerMDemandInfo : thisLayerDemandsPuttingTrafficThisLink.getThird())
//		{
//			final MulticastDemand thisLayerMDemand = thisLayerMDemandInfo.getFirst();
//			final Node mDemandEgressNode = thisLayerMDemandInfo.getSecond();
//			if (thisLayerMDemand.isCoupled()) 
//			{
//				final Link upperLayerLink = thisLayerMDemand.coupledUpperLayerLinks.get(mDemandEgressNode);
//				Triple<SortedMap<Demand,SortedSet<Link>>,SortedMap<Demand,SortedSet<Link>>,SortedMap<Pair<MulticastDemand,Node>,SortedSet<Link>>> res_upperLayer =
//						upperLayerLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink();
//				res_unicastPrimary.putAll(res_upperLayer.getFirst()); // primary traffic in primary links -> primary
//				res_unicastBackup.putAll(res_upperLayer.getSecond()); // primary traffic in backup links -> backup
//				res_multicast.putAll(res_upperLayer.getThird()); 
//			}
//		}
//		return Triple.of(res_unicastPrimary, res_unicastBackup , res_multicast);
//	}


	void updateWorstCasePropagationTraversingUnicastDemandsAndMaybeRoutes ()
	{
		/* updates route and associated demand times */
		for (Route r : cache_traversingRoutes.keySet())
			r.updatePropagationAndProcessingDelayInMiliseconds();

		final SortedSet<Demand> demandsToUpdate = new TreeSet<> ();
		for (Entry<Demand,Pair<Double,Double>> entry : this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.entrySet())
			if (entry.getValue().getFirst() > Configuration.precisionFactor) demandsToUpdate.add(entry.getKey());
		for (Demand d : demandsToUpdate) 
			if (d.routingCycleType == RoutingCycleType.LOOPLESS)
			{
				final Pair<Double,Double> p = GraphUtils.computeWorstCasePropagationDelayAndLengthInKmMsForLoopLess(d.cacheHbH_frs, d.cacheHbH_linksPerNodeWithNonZeroFr, d.ingressNode, d.egressNode);
				d.cache_worstCasePropagationTimeMs = p.getFirst();
				d.cache_worstCaseLengthInKm = p.getSecond();
				if (d.coupledUpperOrSameLayerLink != null)
					d.coupledUpperOrSameLayerLink.updateWorstCasePropagationTraversingUnicastDemandsAndMaybeRoutes();
			}
	}

	/**
	 * <p>Returns {@code true} if the link is coupled to a demand in the same layer as the demand, false otherwise.</p>
	 * @return see above
	 */
	public boolean isCoupledInSameLayer ()
	{
		if (coupledLowerOrThisLayerDemand != null) if (coupledLowerOrThisLayerDemand.getLayer() == this.layer) return true;
		return false;
	}
	
	/**
	 * <p>Returns {@code true} if the link is coupled to a demand in a different layer than the demnad (and thus an *upper* layer)
	 * , false otherwise.</p>
	 * @return see above
	 */
	public boolean isCoupledInDifferentLayer ()
	{
		if (coupledLowerOrThisLayerDemand != null) if (coupledLowerOrThisLayerDemand.getLayer() != this.layer) return true;
		return false;
	}

	boolean isIntraLayerLinkCouplingWithLoops (SortedSet<Link> sameLayerAlreadyCoupledLinks)
	{
		if (!isCoupledInSameLayer()) return false;
		if (coupledLowerOrThisLayerDemand == null) throw new RuntimeException ();
		if (sameLayerAlreadyCoupledLinks == null) sameLayerAlreadyCoupledLinks = new TreeSet<> ();
		assert sameLayerAlreadyCoupledLinks.stream().allMatch(e->e.isCoupledInSameLayer());
		final Pair<SortedSet<Link>,SortedSet<Link>> pairLinkSetsThisLayerWithTraffic = coupledLowerOrThisLayerDemand.getLinksNoDownPropagationPotentiallyCarryingTraffic();
		final SortedSet<Link> linksThisLayerWithMyTraffic = new TreeSet<> (Sets.union(pairLinkSetsThisLayerWithTraffic.getFirst(), pairLinkSetsThisLayerWithTraffic.getSecond()));
		/* If I am traversing a link already that was previously intra coupled, this traffic enters in a loop */
		if (!Sets.intersection(sameLayerAlreadyCoupledLinks, linksThisLayerWithMyTraffic).isEmpty ()) 
			return true;
		for (Link e : linksThisLayerWithMyTraffic)
		{
			if (!e.isCoupledInSameLayer()) continue;
			if (e.isIntraLayerLinkCouplingWithLoops(new TreeSet<> (Sets.union(sameLayerAlreadyCoupledLinks, Sets.newHashSet(e))))) return true;
		}
		return false;
	}
	
	
	/** Gets the links in same layer that (1) are intra coupled, (2) its traffic flows down to me [I may be coupled or not]
	 *
	 * @return List of links
	 */
	SortedSet<Link> getSameLayerCoupledLinksHierarchicallyUpperCouldPutTrafficHere ()
	{
		final SortedSet<Link> res = new TreeSet<> ();
		for (Route r : cache_traversingRoutes.keySet())
			if (r.getDemand().isCoupledInSameLayer())
			{
				res.add(r.getDemand().getCoupledLink());
				res.addAll(r.getDemand().getCoupledLink().getSameLayerCoupledLinksHierarchicallyUpperCouldPutTrafficHere());
			}
		for (Demand d : this.getDemandsWithNonZeroForwardingRules())
			if (d.isCoupledInSameLayer())
			{
				res.add(d.getCoupledLink());
				res.addAll(d.getCoupledLink().getSameLayerCoupledLinksHierarchicallyUpperCouldPutTrafficHere());
			}
		return res;
	}
	
	/** Gets the links in same layer that (1) are intra coupled, (2) its traffic flows down to me [I may be coupled or not]
	 *
	 * @return List of links
	 */
	SortedSet<Link> getSameLayerCoupledLinksHierarchicallyLowerCouldPutTrafficHere ()
	{
		final SortedSet<Link> res = new TreeSet<> ();
		for (Route r : cache_traversingRoutes.keySet())
			if (r.getDemand().isCoupledInSameLayer())
			{
				res.add(r.getDemand().getCoupledLink());
				res.addAll(r.getDemand().getCoupledLink().getSameLayerCoupledLinksHierarchicallyUpperCouldPutTrafficHere());
			}
		for (Demand d : this.getDemandsWithNonZeroForwardingRules())
			if (d.isCoupledInSameLayer())
			{
				res.add(d.getCoupledLink());
				res.addAll(d.getCoupledLink().getSameLayerCoupledLinksHierarchicallyUpperCouldPutTrafficHere());
			}
		return res;
	}

	public void decouple () 
	{
		if (this.coupledLowerOrThisLayerDemand != null) this.coupledLowerOrThisLayerDemand.decouple();
		else if (this.coupledLowerLayerMulticastDemand != null) this.coupledLowerLayerMulticastDemand.decouple();
	}
	
	/** Returns the object contianing the monitored or forecasted time series information for the carried traffic
	 * @return see above
	 */
	public TrafficSeries getMonitoredOrForecastedCarriedTraffic () { return this.monitoredOrForecastedTraffics; }

	
	/** Sets the new time series for the monitored or forecasted offered traffic, eliminating any previous values 
	 * @param newTimeSeries  see above
	 */
	public void setMonitoredOrForecastedCarriedTraffic (TrafficSeries newTimeSeries) { this.monitoredOrForecastedTraffics = new TrafficSeries (newTimeSeries.getValues()); }

}
