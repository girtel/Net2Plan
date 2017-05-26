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
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
	double cache_carriedTraffic;
	double cache_occupiedCapacity;
	double lengthInKm;
	double propagationSpeedInKmPerSecond;
	boolean isUp;
	Link bidirectionalPair;

	Set<SharedRiskGroup> cache_srgs;
	Map<Route,Integer> cache_traversingRoutes; // for each traversing route, the number of times it traverses this link (in seqLinksRealPath). If the route has segments, their internal route counts also
	Set<MulticastTree> cache_traversingTrees;
	Demand coupledLowerLayerDemand;
	MulticastDemand coupledLowerLayerMulticastDemand;
	
	Map<Demand,Double> cacheHbH_frs;
	Map<Demand,Pair<Double,Double>> cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState; // carried is normalized respect to demand total CARRIED traffic
	
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

		if (!netPlan.equals(layer.netPlan)) throw new RuntimeException ("Bad");
		if (!netPlan.equals(originNode.netPlan)) throw new RuntimeException ("Bad");
		if (!netPlan.equals(destinationNode.netPlan)) throw new RuntimeException ("Bad");
		this.layer = layer;
		this.originNode = originNode;
		this.destinationNode = destinationNode;
		this.cache_carriedTraffic = 0;
		this.cache_occupiedCapacity = 0;
		this.lengthInKm = lengthInKm;
		this.propagationSpeedInKmPerSecond = propagationSpeedInKmPerSecond;
		this.isUp = true;
		this.coupledLowerLayerDemand = null;
		this.coupledLowerLayerMulticastDemand = null;
		this.cache_srgs = new HashSet<SharedRiskGroup> ();
		this.cache_traversingRoutes = new HashMap<Route,Integer> ();
		this.cache_traversingTrees = new HashSet<MulticastTree> ();
		this.cacheHbH_frs = new HashMap<> ();
		this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState = new HashMap<> ();
		this.capacity = capacity;
		this.bidirectionalPair = null;
		if (capacity < Configuration.precisionFactor) layer.cache_linksZeroCap.add(this); // do not call here the regular updae function on purpose, there is no previous capacity info
	}

	void copyFrom (Link origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.capacity = origin.capacity;
		this.cache_carriedTraffic = origin.cache_carriedTraffic;
		this.cache_occupiedCapacity = origin.cache_occupiedCapacity;
		this.lengthInKm = origin.lengthInKm;
		this.propagationSpeedInKmPerSecond = origin.propagationSpeedInKmPerSecond;
		this.isUp = origin.isUp;
		this.cache_srgs = new HashSet<SharedRiskGroup> ();
		this.cache_traversingRoutes = new HashMap<Route,Integer> ();
		this.cache_traversingTrees = new HashSet<MulticastTree> ();
		for (SharedRiskGroup s : origin.cache_srgs) this.cache_srgs.add(this.netPlan.getSRGFromId(s.id));
		for (Entry<Route,Integer> r : origin.cache_traversingRoutes.entrySet()) this.cache_traversingRoutes.put(this.netPlan.getRouteFromId(r.getKey ().id) , r.getValue());
		for (MulticastTree t : origin.cache_traversingTrees) this.cache_traversingTrees.add(this.netPlan.getMulticastTreeFromId(t.id));
		this.coupledLowerLayerDemand = origin.coupledLowerLayerDemand == null? null : this.netPlan.getDemandFromId(origin.coupledLowerLayerDemand.id);
		this.coupledLowerLayerMulticastDemand = origin.coupledLowerLayerMulticastDemand == null? null : this.netPlan.getMulticastDemandFromId(origin.coupledLowerLayerMulticastDemand.id);
		this.cacheHbH_frs.clear(); 
		for (Entry<Demand,Double> fr : origin.cacheHbH_frs.entrySet()) 
			this.cacheHbH_frs.put(netPlan.getDemandFromId(fr.getKey().id), fr.getValue());
		this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.clear();
		for (Entry<Demand,Pair<Double,Double>> fr : origin.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.entrySet()) 
			this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.put(netPlan.getDemandFromId(fr.getKey().id), Pair.of(fr.getValue().getFirst(), fr.getValue().getSecond()));
		this.bidirectionalPair = origin.bidirectionalPair == null? null : netPlan.getLinkFromId(origin.bidirectionalPair.getId());
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
		if (this.cache_carriedTraffic != e2.cache_carriedTraffic) return false;
		if (this.cache_occupiedCapacity != e2.cache_occupiedCapacity) return false;
		if (this.lengthInKm != e2.lengthInKm) return false;
		if (this.propagationSpeedInKmPerSecond != e2.propagationSpeedInKmPerSecond) return false;
		if (this.isUp != e2.isUp) return false;
		if ((this.coupledLowerLayerDemand == null) != (e2.coupledLowerLayerDemand == null)) return false; 
		if ((this.coupledLowerLayerDemand != null) && (coupledLowerLayerDemand.id != e2.coupledLowerLayerDemand.id)) return false;
		if ((this.coupledLowerLayerMulticastDemand == null) != (e2.coupledLowerLayerMulticastDemand == null)) return false; 
		if ((this.coupledLowerLayerMulticastDemand != null) && (coupledLowerLayerMulticastDemand.id != e2.coupledLowerLayerMulticastDemand.id)) return false;
		if (!NetPlan.isDeepCopy(this.cache_srgs , e2.cache_srgs)) return false;
		if (!NetPlan.isDeepCopy(this.cache_traversingRoutes , e2.cache_traversingRoutes)) return false;
		if (!NetPlan.isDeepCopy(this.cache_traversingTrees , e2.cache_traversingTrees)) return false;
		if (!NetPlan.isDeepCopy(this.cacheHbH_frs , e2.cacheHbH_frs)) return false;
		if (!NetPlan.isDeepCopy(this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState , e2.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState)) return false;
		return true;
	}
	
	
	/** Returns the set of demands in this layer, with non zero forwarding rules defined for them in this link. 
	 * The link may or may not carry traffic, and may be or not be failed.
	 * @return see above
	 */
	public Set<Demand> getDemandsWithNonZeroForwardingRules ()
	{
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		return this.cacheHbH_frs.keySet().stream().filter(d->cacheHbH_frs.get(d) != 0).collect(Collectors.toSet());
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
		return coupledLowerLayerDemand;
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
	public boolean isCoupled () {  return ((coupledLowerLayerDemand != null) || (coupledLowerLayerMulticastDemand != null)); }
	
	/**
	 * <p>Returns the non zero forwarding rules that are defined in the link. If the routing type of the layer where the link is attached is not
	 * {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING} an exception is thrown.</p>
	 * @return Map of demand-link pairs, associated to {@code [0,1]} splitting factors
	 */
	public Map<Pair<Demand,Link>,Double> getForwardingRules ()
	{
		checkAttachedToNetPlanObject();
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		
		Map<Pair<Demand,Link>,Double> res = new HashMap<> ();
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
		return capacity;
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
		if ((coupledLowerLayerDemand != null) || (coupledLowerLayerMulticastDemand != null)) throw new Net2PlanException ("Coupled links cannot change its capacity");
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
		return cache_carriedTraffic;
	}
	
	
	/** Returns the set of routes traversing the link that are designated as backup of other route
	 * @return see above
	 */
	public Set<Route> getTraversingBackupRoutes ()
	{
		return getTraversingRoutes().stream ().filter(e->e.isBackupRoute()).collect(Collectors.toSet());
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
		if ((capacity == 0) && (cache_occupiedCapacity > 0)) return Double.POSITIVE_INFINITY;
		return capacity == 0? 0 : cache_occupiedCapacity / capacity;
	}
	
	/** <p>Returns the link occupied capacity (in link capacity units). </p>
	 * @return The occupied capacity as described above
	 * */
	public double getOccupiedCapacity()
	{
		return cache_occupiedCapacity;
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
		if (coupledLowerLayerDemand != null) return coupledLowerLayerDemand.getWorstCaseLengthInKm();
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
		if (coupledLowerLayerDemand != null) return coupledLowerLayerDemand.getWorstCasePropagationTimeInMs();
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
		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
		return (cache_occupiedCapacity > capacity + PRECISION_FACTOR);
	}
	
	/** <p>Returns the Shared Risk Groups ({@link com.net2plan.interfaces.networkDesign.SharedRiskGroup SRGs}) the link belongs to. The link fails when any of these SRGs is down.</p>
	 * @return the set of SRGs
	 */
	public Set<SharedRiskGroup> getSRGs()
	{
		return Collections.unmodifiableSet(cache_srgs);
	}

	/**
	 * <p>Returns the {@link com.net2plan.interfaces.networkDesign.Route Routes} traversing the link. If network layer routing type is not
	 * {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, an exception is thrown.</p>
	 * @return An unmodifiable {@code Set} of traversing routes. If no routes exist, an empty {@code Set} is returned
	 */
	public Set<Route> getTraversingRoutes()
	{
		layer.checkRoutingType (RoutingType.SOURCE_ROUTING);
		return Collections.unmodifiableSet(cache_traversingRoutes.keySet());
	}

	/**
	 * <p>Returns the number of routes traversing this link. 
	 * @return see above
	 */
	public int getNumberOfTraversingRoutes()
	{
		layer.checkRoutingType (RoutingType.SOURCE_ROUTING);
		return cache_traversingRoutes.size();
	}

	/**
	 * <p>Returns the {@link com.net2plan.interfaces.networkDesign.MulticastTree Multicast Trees} traversing the link.</p>
	 * @return An unmodifiable {@code Set} of traversing multicast trees. If no tree exists, an empty set is returned
	 */
	public Set<MulticastTree> getTraversingTrees()
	{
		return Collections.unmodifiableSet(cache_traversingTrees);
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
		layer.checkRoutingType (RoutingType.HOP_BY_HOP_ROUTING);
		return cacheHbH_frs.size();
	}

	/**
	 * <p>Couples the link to a unicast {@link com.net2plan.interfaces.networkDesign.Demand} in the lower layer.</p>
	 * @param demand The demand to be coupled to
	 */
	public void coupleToLowerLayerDemand (Demand demand)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		demand.coupleToUpperLayerLink(this);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}
	
	/**
	 * <p>Creates a new {@link com.net2plan.interfaces.networkDesign.Demand Demand} in the given layer, with same end nodes as the link, and then couples the link to the new created demand.</p>
	 * @param newDemandLayer The layer where the demand will be created (and coupled to the link)
	 * @return The created demand
	 */
	public Demand coupleToNewDemandCreated (NetworkLayer newDemandLayer)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		newDemandLayer.checkAttachedToNetPlanObject(this.netPlan);
		if (this.layer.equals (newDemandLayer)) throw new Net2PlanException ("Cannot couple a link and a demand in the same layer");
		Demand newDemand = netPlan.addDemand(originNode ,  destinationNode , capacity , null , newDemandLayer);
		try { newDemand.coupleToUpperLayerLink(this); } catch (RuntimeException e) { newDemand.remove (); throw e; }
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
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		for (Demand d : new ArrayList<> (this.cacheHbH_frs.keySet()))
		{
			final Map<Link,Double> frsThatDemand = new HashMap<> (d.cacheHbH_frs);
			frsThatDemand.remove(this);
			d.updateHopByHopRoutingToGivenFrs(frsThatDemand);
		}
		this.cacheHbH_frs.clear();
		this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.clear();
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>If the link was added using {@link com.net2plan.interfaces.networkDesign.NetPlan#addLinkBidirectional(Node, Node, double, double, double, Map, NetworkLayer...) addLinkBidirectional()},
	 * returns the link in the opposite direction (if it was not previously removed). Returns {@code null} otherwise.</p>
	 * @return The link in the opposite direction, or {@code null} if none
	 */
	public Link getBidirectionalPair()
	{
		checkAttachedToNetPlanObject();
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
		checkAttachedToNetPlanObject();
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
		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();

		if (this.coupledLowerLayerDemand != null) 
			this.coupledLowerLayerDemand.decouple();
		else if (this.coupledLowerLayerMulticastDemand != null)
			this.coupledLowerLayerMulticastDemand.decouple();
		
		if (bidirectionalPair != null) this.bidirectionalPair.bidirectionalPair = null;
		
		layer.cache_linksDown.remove (this);
		layer.cache_linksZeroCap.remove(this);
		netPlan.cache_id2LinkMap.remove(id);
		originNode.cache_nodeOutgoingLinks.remove (this);
		destinationNode.cache_nodeIncomingLinks.remove (this);
		layer.cache_nodePairLinksThisLayer.get(Pair.of(originNode, destinationNode)).remove(this);
		
		for (SharedRiskGroup srg : this.cache_srgs) srg.links.remove(this);
		for (MulticastTree tree : new LinkedList<MulticastTree> (cache_traversingTrees)) tree.remove ();

		if (layer.routingType == RoutingType.SOURCE_ROUTING)
			for (Route route : new HashSet<Route> (cache_traversingRoutes.keySet())) route.remove ();
		else
			this.removeAllForwardingRules();

		NetPlan.removeNetworkElementAndShiftIndexes (layer.links , index);
        for (String tag : tags) netPlan.cache_taggedElements.get(tag).remove(this);

		ErrorHandling.DEBUG = previousErrorHandling;
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		removeId();
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
	public String toString () { return "e" + index + " (id " + id + ")"; }

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
		if (capacity < Configuration.precisionFactor && !layer.cache_linksZeroCap.contains(this)) throw new RuntimeException ("Bad");
		if (capacity >= Configuration.precisionFactor && layer.cache_linksZeroCap.contains(this)) throw new RuntimeException ("Bad");
		if (!isUp)
		{
			if (Math.abs(cache_carriedTraffic) > 1e-3)
				throw new RuntimeException ("Bad");
			if (Math.abs(cache_occupiedCapacity) > 1e-3) throw new RuntimeException ("Bad");
			for (Route r : cache_traversingRoutes.keySet()) if ((r.getCarriedTraffic() != 0) || (r.getOccupiedCapacity(this) != 0)) throw new RuntimeException ("Bad");
			for (MulticastTree r : cache_traversingTrees) if ((r.getCarriedTraffic() != 0) || (r.getOccupiedLinkCapacity() != 0)) throw new RuntimeException ("Bad");
		}
		
		for (SharedRiskGroup srg : netPlan.srgs) if (this.cache_srgs.contains(srg) != srg.links.contains(this)) throw new RuntimeException ("Bad");

		double check_carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments = 0;
		double check_occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments = 0;
		if (layer.routingType == RoutingType.SOURCE_ROUTING)
		{
			for (Route route : layer.routes)
			{
				if (this.cache_traversingRoutes.containsKey(route) != route.cache_seqLinksRealPath.contains(this)) throw new RuntimeException ("Bad. link: " + this + ", route: " + route + ", this.cache_traversingRoutes: " + this.cache_traversingRoutes + ", route.seqLinksRealPath: "+  route.cache_seqLinksRealPath);
				if (this.cache_traversingRoutes.containsKey(route))
				{
					int numPasses = 0; for (Link linkRoute : route.cache_seqLinksRealPath) if (linkRoute == this) numPasses ++; if (numPasses != this.cache_traversingRoutes.get(route)) throw new RuntimeException ("Bad");
					check_carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments += route.getCarriedTraffic();
					check_occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments += route.getOccupiedCapacity(this);
				}
			}
		}
		else 
		{
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
				check_carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments += cap;
				check_occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments += cap;
			}
		}
		for (MulticastTree tree : layer.multicastTrees)
		{
			if (this.cache_traversingTrees.contains(tree) != tree.linkSet.contains(this)) throw new RuntimeException ("Bad");
			if (this.cache_traversingTrees.contains(tree))
			{
				check_carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments += tree.getCarriedTraffic();
				check_occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments += tree.getOccupiedLinkCapacity();
			}
		}

		if (Math.abs(cache_carriedTraffic - check_carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments) > 1E-3) 
			throw new RuntimeException ("Bad: Link: " + this + ". carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments: " + cache_carriedTraffic + ", check_carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments: " + check_carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments);

		if (cache_occupiedCapacity != check_occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments)
		{
			if (check_occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments != 0)
			{
				if ((cache_occupiedCapacity / check_occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments) > (1 + 1E-3)) throw new RuntimeException();
			} else
			{
				if (cache_occupiedCapacity > 1E-3 || cache_occupiedCapacity < -1E-3) throw new RuntimeException();
			}
		}

		if (coupledLowerLayerDemand != null)
			if (!coupledLowerLayerDemand.coupledUpperLayerLink.equals (this)) throw new RuntimeException ("Bad");
		if (coupledLowerLayerMulticastDemand != null)
			if (!coupledLowerLayerMulticastDemand.coupledUpperLayerLinks.containsValue(this)) throw new RuntimeException ("Bad");
	}
	
	void updateLinkTrafficAndOccupation ()
	{
		this.cache_carriedTraffic = 0;
		this.cache_occupiedCapacity = 0;
		if (layer.isSourceRouting())
		{
			for (Entry<Route,Integer> entry : cache_traversingRoutes.entrySet())
			{
				this.cache_carriedTraffic += entry.getKey().getCarriedTraffic();
				this.cache_occupiedCapacity += entry.getKey().getOccupiedCapacity(this);
			}
		}
		else
		{
			for (Entry<Demand,Pair<Double,Double>> entry : this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.entrySet())
			{
				this.cache_carriedTraffic += entry.getValue().getSecond();
				this.cache_occupiedCapacity += entry.getValue().getSecond();
			}
		}
		for (MulticastTree t : cache_traversingTrees)
		{
			this.cache_carriedTraffic += t.getCarriedTraffic();
			this.cache_occupiedCapacity += t.getOccupiedLinkCapacity();
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
	public Triple<Set<Demand>,Set<Demand>,Set<MulticastDemand>> getDemandsPotentiallyTraversingThisLink ()
	{
		final Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> res = 
				getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  ();
		return Triple.of(new HashSet<> (res.getFirst().keySet()), new HashSet<> (res.getSecond().keySet()), res.getThird().keySet().stream().map(p->p.getFirst()).collect(Collectors.toSet()));
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
	public Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  ()
	{
		final Map<Demand,Set<Link>> resPrimary = new HashMap<> ();
		final Map<Demand,Set<Link>> resBackup = new HashMap<> ();
		if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING)
		{
			for (Demand travDemands : this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.keySet())
				resPrimary.put(travDemands , travDemands.cacheHbH_normCarriedOccupiedPerLinkCurrentState.keySet());
		}
		else
		{
			for (Route r : cache_traversingRoutes.keySet())
			{
				if (r.isDown()) continue;
				for (Link e : r.getSeqLinks())
				{
					if (r.isBackupRoute())
					{
						Set<Link> linksSoFar = resBackup.get(r.getDemand());
						if (linksSoFar == null) { linksSoFar = new HashSet<> (); resBackup.put(r.getDemand(), linksSoFar); }
						linksSoFar.add(e);
					}
					else 
					{
						Set<Link> linksSoFar = resPrimary.get(r.getDemand());
						if (linksSoFar == null) { linksSoFar = new HashSet<> (); resPrimary.put(r.getDemand(), linksSoFar); }
						linksSoFar.add(e);
					}
				}
			}
		}
		
		Map<Pair<MulticastDemand,Node>,Set<Link>> resMCast = new HashMap<> ();
		for (MulticastTree t : cache_traversingTrees)
		{
			for (Node egressNode : t.getMulticastDemand().getEgressNodes())
			{
				final List<Link> pathToEgressNode = t.getSeqLinksToEgressNode(egressNode);
				if (!pathToEgressNode.contains(this)) continue;
				if (!netPlan.isUp (pathToEgressNode)) continue;
				Set<Link> linksSoFar = resMCast.get(Pair.of(t.getMulticastDemand() , egressNode));
				if (linksSoFar == null) { linksSoFar = new HashSet<> (); resMCast.put(Pair.of(t.getMulticastDemand() , egressNode), linksSoFar); }
				linksSoFar.addAll(pathToEgressNode);
			}
		}
		return Triple.of(resPrimary,resBackup,resMCast);
	}


	/** Returns the set of links in this layer carry the traffic that traverses this link, before and after traversing it,
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
	public Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> getLinksLowerLayersPotentiallyCarryingTrafficTraversingThisLink  ()
	{
		final Map<Demand,Set<Link>> resPrimary = new HashMap <> ();
		final Map<Demand,Set<Link>> resBackup = new HashMap <> ();
		final Map<Pair<MulticastDemand,Node>,Set<Link>> resMCast = new HashMap <> ();
		
		if (!isCoupled()) return Triple.of(resPrimary, resBackup,resMCast);
		if (!this.isUp) return Triple.of(resPrimary, resBackup,resMCast);
		
		if (this.coupledLowerLayerDemand != null)
		{
			Pair<Set<Link>,Set<Link>> pair = coupledLowerLayerDemand.getLinksThisLayerPotentiallyCarryingTraffic  ();
			resPrimary.put(coupledLowerLayerDemand, pair.getFirst());
			resBackup.put(coupledLowerLayerDemand, pair.getSecond());
			for (Link lowerLayerLink_primary : pair.getFirst())
			{
				final Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> linksLayersTwoBelow = lowerLayerLink_primary.getLinksLowerLayersPotentiallyCarryingTrafficTraversingThisLink  ();
				resPrimary.putAll(linksLayersTwoBelow.getFirst()); // primary traffic in lower layer, and in two layers below -> is primary
				resBackup.putAll(linksLayersTwoBelow.getSecond()); // primary traffic in lower layer, and backup in two layers below -> is backup
				resMCast.putAll (linksLayersTwoBelow.getThird());
			}
			for (Link lowerLayerLink_backup : pair.getFirst())
			{
				final Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> linksLayersTwoBelow = lowerLayerLink_backup.getLinksLowerLayersPotentiallyCarryingTrafficTraversingThisLink  ();
				resBackup.putAll(linksLayersTwoBelow.getFirst()); // primary traffic in lower layer, and in two layers below -> is primary
				resBackup.putAll(linksLayersTwoBelow.getSecond()); // primary traffic in lower layer, and backup in two layers below -> is backup
				resMCast.putAll (linksLayersTwoBelow.getThird());
			}
		}
		else if (this.coupledLowerLayerMulticastDemand != null)
		{
			Set<Link> linksLowerLayer = coupledLowerLayerMulticastDemand.getLinksThisLayerPotentiallyCarryingTraffic  (this.destinationNode);
			resMCast.put(Pair.of(coupledLowerLayerMulticastDemand, this.destinationNode) , linksLowerLayer);
			for (Link lowerLayerLink : linksLowerLayer)
			{
				final Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> linksLayersTwoBelow = lowerLayerLink.getLinksLowerLayersPotentiallyCarryingTrafficTraversingThisLink  ();
				resPrimary.putAll(linksLayersTwoBelow.getFirst()); // primary traffic in lower layer, and in two layers below -> is primary
				resBackup.putAll(linksLayersTwoBelow.getSecond()); // primary traffic in lower layer, and backup in two layers below -> is backup
				resMCast.putAll (linksLayersTwoBelow.getThird());
			}
		}
		return Triple.of(resPrimary,resBackup,resMCast);
	}

//	public Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>>
//		getLinksUpperLayersPotentiallyPuttingTrafficInThisLink  (boolean assumeNoFailureState , Triple<Set<Demand>,Set<Demand>,Set<Pair<MulticastDemand,Node>>> thisLayerDemandsPuttingTrafficThisLink)
//	{
//		if (thisLayerDemandsPuttingTrafficThisLink == null)
//		{
//			Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> triple = 
//					this.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  ();
//			thisLayerDemandsPuttingTrafficThisLink = Triple.of(triple.getFirst().keySet(), triple.getSecond().keySet(), triple.getThird().keySet());
//		}
//		
//		/* Add upper layer info*/
//		final Map<Demand,Set<Link>> res_unicastPrimary = new HashMap<> ();
//		final Map<Demand,Set<Link>> res_unicastBackup = new HashMap<> ();
//		final Map<Pair<MulticastDemand,Node>,Set<Link>> res_multicast = new HashMap<> ();
//
//		/* Propagate to two layers up, and accumulate results */
//		for (Demand thisLayerDemand : thisLayerDemandsPuttingTrafficThisLink.getFirst())
//		{
//			if (thisLayerDemand.isCoupled())
//			{
//				final Link upperLayerLink = thisLayerDemand.coupledUpperLayerLink;
//				Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> res_upperLayer =
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
//				Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> res_upperLayer =
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
//				Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> res_upperLayer =
//						upperLayerLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink();
//				res_unicastPrimary.putAll(res_upperLayer.getFirst()); // primary traffic in primary links -> primary
//				res_unicastBackup.putAll(res_upperLayer.getSecond()); // primary traffic in backup links -> backup
//				res_multicast.putAll(res_upperLayer.getThird()); 
//			}
//		}
//		return Triple.of(res_unicastPrimary, res_unicastBackup , res_multicast);
//	}


	private void updateWorstCasePropagationTraversingUnicastDemandsAndMaybeRoutes ()
	{
		if (layer.isSourceRouting())
		{
			/* updates route and associated demand times */
			for (Route r : cache_traversingRoutes.keySet()) r.updatePropagationAndProcessingDelayInMiliseconds();
		}
		else
		{
			final Set<Demand> demandsToUpdate = new HashSet<> ();
			for (Entry<Demand,Pair<Double,Double>> entry : this.cacheHbH_normCarriedOccupiedPerTraversingDemandCurrentState.entrySet())
				if (entry.getValue().getFirst() > Configuration.precisionFactor) demandsToUpdate.add(entry.getKey());
			for (Demand d : demandsToUpdate) 
				if (d.routingCycleType == RoutingCycleType.LOOPLESS)
				{
					final Pair<Double,Double> p = GraphUtils.computeWorstCasePropagationDelayAndLengthInKmMsForLoopLess(d.cacheHbH_frs, d.cacheHbH_linksPerNodeWithNonZeroFr, d.ingressNode, d.egressNode);
					d.cache_worstCasePropagationTimeMs = p.getFirst();
					d.cache_worstCaseLengthInKm = p.getSecond();
				}
		}
	}
	

}
