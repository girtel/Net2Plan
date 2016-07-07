/*******************************************************************************
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

import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;

import java.util.*;
import java.util.Map.Entry;

/**
 * <p>This class contains a representation of a protection segment. This is a contiguous sequence of links in a particular network layer
 *  (with a routing of type source routnig), and an amount of reserved capacity in each of the traversed links. A protection segment can be associated
 *   to an arbitrary number of routes in the same network layer. Its reserved capacity can be used as backup for those routes, if the primary route fails. 
 *   When a route fails, the rerouting using associated protection segments is not automatic: it would be the role of a built-in or user-defined algorithm 
 *   how the routes should be rerouted in the associated segments.
 * </p>
 * <p> A ProtectionSegment object is a subclass of Link. This has no special importance for the regular user. It becomes useful when writing 
 * network recovery algoithms, since the routes can be rerouted using protection segments as if they were links </p> 
 * @author Pablo Pavon-Marino
 */
public class ProtectionSegment extends Link 
{
	List<Link> seqLinks;
	List<Node> seqNodes;
	Set<Route> associatedRoutesToWhichIAmPotentialBackup;
	
	
	ProtectionSegment (NetPlan netPlan , long id , int index , List<Link> seqLinks , double reservedCapacity , AttributeMap attributes)
	{
		super (netPlan , id , index , seqLinks , reservedCapacity , attributes); // reserved capacity is the underlying link capacity. Its carried traffic, is its carried traffic

		for (Link e : seqLinks) if (!netPlan.equals(e.netPlan)) throw new RuntimeException ("Bad");
		
		this.seqLinks = new LinkedList<Link> (seqLinks);
		this.seqNodes = new LinkedList<Node> (); seqNodes.add (this.getOriginNode()); for (Link e : seqLinks) seqNodes.add (e.getDestinationNode());
		this.associatedRoutesToWhichIAmPotentialBackup = new HashSet<Route> ();
	}

	void copyFrom (ProtectionSegment origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.seqLinks.clear (); for (Link e : origin.seqLinks) this.seqLinks.add(this.netPlan.getLinkFromId (e.id));
		this.seqNodes.clear (); for (Node n : origin.seqNodes) this.seqNodes.add(this.netPlan.getNodeFromId (n.id));
		this.associatedRoutesToWhichIAmPotentialBackup.clear (); for (Route r : origin.associatedRoutesToWhichIAmPotentialBackup) this.associatedRoutesToWhichIAmPotentialBackup.add(this.netPlan.getRouteFromId (r.id));

		this.capacity = origin.capacity;
		this.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments = origin.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments;
		this.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments = origin.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments;
		this.lengthInKm = origin.lengthInKm;
		this.propagationSpeedInKmPerSecond = origin.propagationSpeedInKmPerSecond;
		this.isUp = origin.isUp;
		this.cache_traversingRoutes = new HashMap<Route,Integer> (); for (Entry<Route,Integer> r : origin.cache_traversingRoutes.entrySet()) this.cache_traversingRoutes.put(this.netPlan.getRouteFromId(r.getKey ().id) , r.getValue());
		this.cache_srgs = new HashSet<SharedRiskGroup> (); for (SharedRiskGroup srg : origin.cache_srgs) this.cache_srgs.add(this.netPlan.getSRGFromId(srg.id));
		this.cache_traversingTrees = new HashSet<MulticastTree> (); if (!origin.cache_traversingTrees.isEmpty()) throw new RuntimeException ("Bad");
		this.cache_traversingSegments = new HashSet<ProtectionSegment> (); if (!origin.cache_traversingSegments.isEmpty()) throw new RuntimeException ("Bad");
		this.coupledLowerLayerDemand = null; if (origin.coupledLowerLayerDemand != null) throw new RuntimeException ("Bad");
		this.coupledLowerLayerMulticastDemand = null; if (origin.coupledLowerLayerMulticastDemand != null) throw new RuntimeException ("Bad");
	}

	/**
	 * <p>Returns the protection segment sequence of links.</p>
	 * @return The sequence of links as an unmodifiable list
	 */
	public List<Link> getSeqLinks()
	{
		return Collections.unmodifiableList(seqLinks);
	}

	/** <p>Returns the segment propagation delay in miliseconds, traversing all the links.</p>
	 * @return The propagation delay in miliseconds
	 */
	public double getPropagationDelayInMs()
	{
		double accum = 0; for (Link e : seqLinks) accum += e.getPropagationDelayInMs(); return accum;
	}

	
	/**
	 * <p>Returns the protection segment sequence of nodes</p>
	 * @return The sequence of nodes as an unmodifiable list
	 */
	public List<Node> getSeqNodes()
	{
		return Collections.unmodifiableList(seqNodes);
	}

	/**
	 * <p>Returns the utilization of the protection segment: capacity occupied by the carried traffic, divided by reserved capacity.</p>
	 * @return The utilization
	 */
	public double getUtilization ()
	{
		return occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments / capacity;
	}

	/**
	 * <p>Returns true if the protection segment is traversing a link or node that is down. Then, it will not be able to carry traffic</p>
	 * @return {@code true} if the segment is down
	 */
	public boolean isDown ()
	{
		checkAttachedToNetPlanObject();
		return layer.cache_segmentsDown.contains(this);
	}

	/**
	 * <p>The carried traffic (in traffic units) of the routes traversing this segment.</p>
	 * @return The carried traffic
	 */
	public double getCarriedTraffic () { return carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments; }

	/**
	 * <p>The occupied link capacity (in link capacity units) of the routes traversing this segment.</p>
	 * @return The occupied link capacity
	 */
	public double getOccupiedLinkCapacity () { return occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments; }
	
	
	/** This method cannot be called for protection segments (raises an exception) 
	 */
	public Map<Pair<Demand,Link>,Double> getForwardingRules () { throw new Net2PlanException ("This method cannot be called for ProtectionSegments"); } 
	/** This method cannot be called for protection segments (raises an exception) 
	 */
	public void setCapacity(double linkCapacity) { throw new Net2PlanException ("This method cannot be called for ProtectionSegments"); } 
	/** This method cannot be called for protection segments (raises an exception) 
	 */
	public double getCarriedTrafficNotIncludingProtectionSegments() { throw new Net2PlanException ("This method cannot be called for ProtectionSegments"); } 
	/** This method cannot be called for protection segments (raises an exception) 
	 */
	public double getCarriedTrafficIncludingProtectionSegments() { throw new Net2PlanException ("This method cannot be called for ProtectionSegments"); } 
	/** This method cannot be called for protection segments (raises an exception) 
	 */
	public double getUtilizationIncludingProtectionSegments() { throw new Net2PlanException ("This method cannot be called for ProtectionSegments"); } 
	/** This method cannot be called for protection segments (raises an exception) 
	 */
	public double getUtilizationNotIncludingProtectionSegments() { throw new Net2PlanException ("This method cannot be called for ProtectionSegments"); } 
	/** This method cannot be called for protection segments (raises an exception) 
	 */
	public double getOccupiedCapacityNotIncludingProtectionSegments() { throw new Net2PlanException ("This method cannot be called for ProtectionSegments"); } 
	/** This method cannot be called for protection segments (raises an exception) 
	 */
	public double getOccupiedCapacityIncludingProtectionSegments() { throw new Net2PlanException ("This method cannot be called for ProtectionSegments"); } 
	/** This method cannot be called for protection segments (raises an exception) 
	 */
	public void setLengthInKm(double lengthInKm) { throw new Net2PlanException ("This method cannot be called for ProtectionSegments"); } 
	/** This method cannot be called for protection segments (raises an exception) 
	 */
	public void setPropagationSpeedInKmPerSecond(double speed) { throw new Net2PlanException ("This method cannot be called for ProtectionSegments"); } 
	/** This method cannot be called for protection segments (raises an exception) 
	 */
	public void coupleToLowerLayerDemand (Demand demand) { throw new Net2PlanException ("This method cannot be called for ProtectionSegments"); } 
	/** This method cannot be called for protection segments (raises an exception) 
	 */
	public Demand coupleToNewDemandCreated (NetworkLayer newLinkLayer) { throw new Net2PlanException ("This method cannot be called for ProtectionSegments"); } 
	/** This method cannot be called for protection segments (raises an exception) 
	 */
	public void removeAllForwardingRules() { throw new Net2PlanException ("This method cannot be called for ProtectionSegments"); } 
	/** This method cannot be called for protection segments (raises an exception) 
	 */
	public Set<ProtectionSegment> getTraversingProtectionSegments() { throw new Net2PlanException ("This method cannot be called for ProtectionSegments"); } 
	/** This method cannot be called for protection segments (raises an exception) 
	 */
	public boolean setFailureState (boolean setAsUp) { throw new Net2PlanException ("This method cannot be called for ProtectionSegments"); } 

		
	/**
	 * <p>Returns the set of SRGs the that affect this protection segment.</p>
	 * @return The set of SRGs (empty if none)
	 */
	public Set<SharedRiskGroup> getSRGs()
	{
		Set<SharedRiskGroup> res = new HashSet<SharedRiskGroup> ();
		for (Link e : seqLinks) res.addAll (e.cache_srgs);
		for (Node n : seqNodes) res.addAll (n.cache_nodeSRGs);
		return res;
	}

	//	/**
//	 * Returns true if the protection segment was is operationally inactive. The Net2Plan kernel reacts then as if it did not exist and the associated resources were not occupied 
//	 * (similarly to wha happens when the protection segment is down because it traverses failing nodes or links). 
//	 */
//	public boolean isOperationallyDeactivated ()
//	{
//		return isOperationallyDeactivated;
//	}
//
//	/**
//	 * Sets the operational state (activ or inactive) of the protection segment. If set to inactive, it is as if it did not exist, and the allocated resources are released
//	 */
//	public boolean setOperationalActivationState (boolean activateIt)
//	{
//		final boolean old_isOperationallyDeactivated = isOperationallyDeactivated;
//		this.isOperationallyDeactivated = !activateIt;
//		return old_isOperationallyDeactivated;
//	}
//
	/**
	 * <p>Returns {@code false} if the protection segment is traversing a link or node that is down</p>
	 * @return {@code true} if up, false if down
	 */
	public boolean isUp ()
	{
		return !isDown ();
	}

	/**
	 * <p>Returns {@code tue} if the protection segment is dedicated: assigned to exactly one route</p>
	 * @return {@code True} if dedicated, {@code false} otherwise
	 * */
	public boolean isDedicated()
	{
		return associatedRoutesToWhichIAmPotentialBackup.size() == 1;
	}

	/** <p>Returns the routes this segment is a potential backing up segment</p>
	 * @return The set of associated routes
	 */
	public Set<Route> getAssociatedRoutesToWhichIsBackup()
	{
		return associatedRoutesToWhichIAmPotentialBackup;
	}
	
	/**
	 * <p>Returns the segment number of traversed links.</p>
	 * @return The number of links
	 * */
	public int getNumberOfHops()
	{
		return seqLinks.size();
	}

	/**
	 * <p>Returns the segment reserved capacity in the traversed links.</p>
	 * @return The capacity reserved for protection
	 */
	public double getReservedCapacityForProtection ()
	{
		return this.capacity;
	}

	/** Sets the segment reserved capacity in the traversed links
	 * @param capacity the reserved capacity (in link capacity units)
	 */
	public void setReservedCapacity (double capacity)
	{
		capacity = NetPlan.adjustToTolerance(capacity);
		if (capacity < 0) throw new Net2PlanException ("Reserved capacities must be non-negative");
		this.capacity = capacity;
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Removes a protection segment. Routes currently traversing the segment are also removed. Routes not currently traversing the protection segment, 
	 * but that have this segment as a potential backup, are not removed. </p>
	 */
	public void remove ()
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);

		Map<Route,Integer> previousCache = new HashMap<Route,Integer> (cache_traversingRoutes); 
		
		for (Route route : new LinkedList<Route> (cache_traversingRoutes.keySet())) route.remove ();

		if (Math.abs (this.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments) > 1e-3) 
		{
			System.out.println ("SEgment " + this);
			System.out.println ("PREVIOUS cache_traversingRoutes " + previousCache);
			System.out.println ("cache_traversingRoutes " + cache_traversingRoutes);
			System.out.println ("this.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments " + this.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments);
			throw new RuntimeException ("Bad: carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments: " + carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments);
		}
		netPlan.cache_id2ProtectionSegmentMap.remove(id);
		NetPlan.removeNetworkElementAndShiftIndexes(layer.protectionSegments , index);
		for (Node node : seqNodes) node.cache_nodeAssociatedSegments.remove(this);
		for (Link link : seqLinks) link.cache_traversingSegments.remove (this);
		for (Route route : associatedRoutesToWhichIAmPotentialBackup) route.potentialBackupSegments.remove (this);
		layer.cache_segmentsDown.remove (this);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		removeId();
	}

	/**
	 * <p>Returns a {@code String} representation of the protection segment.</p>
	 * @return {@code String} representation of the protection segment
	 */
	public String toString () { return "segment" + index + " (id " + id + ")"; }

	void checkCachesConsistency ()
	{
		if (!layer.protectionSegments.contains(this)) throw new RuntimeException ("Bad");
		for (Link link : seqLinks) if (!link.cache_traversingSegments.contains(this)) throw new RuntimeException ("Bad");
		for (Node node : seqNodes) if (!node.cache_nodeAssociatedSegments.contains(this)) throw new RuntimeException ("Bad");
		for (Route route : associatedRoutesToWhichIAmPotentialBackup) if (!route.potentialBackupSegments.contains(this)) throw new RuntimeException ("Bad");
		boolean shouldBeUp = true; for (Link e : seqLinks) if (!e.isUp) { shouldBeUp = false; break; }
		if (shouldBeUp) for (Node n : seqNodes) if (!n.isUp) { shouldBeUp = false; break; }
		if (!shouldBeUp != this.isDown()) throw new RuntimeException("Bad");
		if (!shouldBeUp)
		{
			if (carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments != 0) throw new RuntimeException ("Bad");
		}
		double checkCarryingTraffic = 0; double checkOccupiedSegmentCapacity = 0;
		for (Entry<Route,Integer> entry : this.cache_traversingRoutes.entrySet())
		{
			final Route r = entry.getKey();
			final Integer times = entry.getValue(); if (times < 1) throw new RuntimeException ("Bad");
			if (!r.seqLinksAndProtectionSegments.contains(this)) throw new RuntimeException ("Bad");
			if (!shouldBeUp && (r.carriedTraffic > 1e-3)) throw new RuntimeException ("Bad");
			if (!shouldBeUp && (r.occupiedLinkCapacity > 1e-3)) throw new RuntimeException ("Bad");
			checkCarryingTraffic += times * r.carriedTraffic; 
			checkOccupiedSegmentCapacity += times * r.occupiedLinkCapacity;
		}
		if (Math.abs(checkCarryingTraffic - this.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments) > 1e-3)
		{
			System.out.println ("this: " + this);
			System.out.println ("shouldBeUp: " + shouldBeUp);
			System.out.println ("cache_traversingRoutes: " + cache_traversingRoutes);
			System.out.println ("checkCarryingTraffic: " + checkCarryingTraffic);
			System.out.println ("carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments: " + carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments);
			throw new RuntimeException ("Bad");
		}
		if (Math.abs(checkOccupiedSegmentCapacity - this.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments) > 1e-3) throw new RuntimeException ("Bad");
	}

}
