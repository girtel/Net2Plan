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

import cern.colt.list.tdouble.DoubleArrayList;
import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import junit.framework.Assert;

import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Quadruple;

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

	Set<SharedRiskGroup> cache_srgs;
	Map<Route,Integer> cache_traversingRoutes; // for each traversing route, the number of times it traverses this link (in seqLinksRealPath). If the route has segments, their internal route counts also
	Set<MulticastTree> cache_traversingTrees;

	
	Demand coupledLowerLayerDemand;
	MulticastDemand coupledLowerLayerMulticastDemand;
	
	
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
		this.capacity = capacity;
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
	}

	boolean isDeepCopy (Link e2)
	{
		if (!super.isDeepCopy(e2)) return false;
		if (layer.id != e2.layer.id) return false;
		if (originNode.id != e2.originNode.id) return false;
		if (destinationNode.id != e2.destinationNode.id) return false;
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
		return true;
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
		
		Map<Pair<Demand,Link>,Double> res = new HashMap<Pair<Demand,Link>,Double> ();
		IntArrayList ds = new IntArrayList (); DoubleArrayList vals = new DoubleArrayList ();
		layer.forwardingRulesNoFailureState_f_de.viewColumn (index). getNonZeros(ds,vals);
		for (int cont = 0 ; cont < ds.size () ; cont ++)
			res.put (Pair.of (layer.demands.get(ds.get(cont)) , this) , vals.get(cont));
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
	 * @param linkCapacity The link capacity (must be non-negative)
	 */
	public void setCapacity(double linkCapacity)
	{
		linkCapacity = NetPlan.adjustToTolerance(linkCapacity);
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (linkCapacity < 0) throw new Net2PlanException ("Negative link capacities are not possible");
		if ((coupledLowerLayerDemand != null) || (coupledLowerLayerMulticastDemand != null)) throw new Net2PlanException ("Coupled links cannot change its capacity");
		this.capacity = linkCapacity;
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
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
		return lengthInKm;
	}

	/**
	 * <p>Sets the link length in km.</p>
	 * @param lengthInKm New link length in km (must be non-negative)
	 */
	public void setLengthInKm(double lengthInKm)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (lengthInKm < 0) throw new Net2PlanException ("Link lengths cannot be negative");
		this.lengthInKm = lengthInKm;
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
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (speed <= 0) throw new Net2PlanException ("Propagation speeds must be positive");
		this.propagationSpeedInKmPerSecond = speed;
	}


	/** <p>Returns the link propagation delay in miliseconds.</p>
	 * @return The propagation delay in miliseconds
	 */
	public double getPropagationDelayInMs()
	{
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
	 * <p>Returns the {@link com.net2plan.interfaces.networkDesign.MulticastTree Multicast Trees} traversing the link.</p>
	 * @return An unmodifiable {@code Set} of traversing multicast trees. If no tree exists, an empty set is returned
	 */
	public Set<MulticastTree> getTraversingTrees()
	{
		return Collections.unmodifiableSet(cache_traversingTrees);
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
		try { newDemand.coupleToUpperLayerLink(this); } catch (RuntimeException e) { newDemand.remove (); System.out.println ("************* Bad **********"); throw e; }
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
		layer.forwardingRulesNoFailureState_f_de.viewColumn (this.index).assign(0);
		/* update the routing of the demands traversing the eliminated link (others are unaffected) */
		for (Demand d : layer.demands) if (layer.forwardingRulesCurrentFailureState_x_de.get(d.index , this.index) > 0) layer.updateHopByHopRoutingDemand(d);
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
		return (Link) netPlan.getNetworkElementByAttribute(layer.links , netPlan.KEY_STRING_BIDIRECTIONALCOUPLE, "" + this.id);
	}

	/**
	 * <p>Removes the link. The routing is updated removing any associated routes, protection segments, multicast trees or forwarding rules.</p>
	 * <p>If the link is coupled to a unicast or multicast demand, it is first decoupled. In the multicast case, this means that the multicast
	 * demand is decoupled from <b>ALL</b> the links it is coupled to. </p>
	 */
	public void remove()
	{
		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();

		if (this.coupledLowerLayerDemand != null) 
			this.coupledLowerLayerDemand.decouple();
		else if (this.coupledLowerLayerMulticastDemand != null)
			this.coupledLowerLayerMulticastDemand.decouple();
		
		layer.cache_linksDown.remove (this);
		netPlan.cache_id2LinkMap.remove(id);
		originNode.cache_nodeOutgoingLinks.remove (this);
		destinationNode.cache_nodeIncomingLinks.remove (this);
		for (SharedRiskGroup srg : this.cache_srgs) srg.links.remove(this);
		for (MulticastTree tree : new LinkedList<MulticastTree> (cache_traversingTrees)) tree.remove ();

		if (layer.routingType == RoutingType.SOURCE_ROUTING)
		{
			for (Route route : new HashSet<Route> (cache_traversingRoutes.keySet())) route.remove ();
			NetPlan.removeNetworkElementAndShiftIndexes (layer.links , index);
		}
		else
		{
			final int D = layer.demands.size ();
			final int N = netPlan.nodes.size ();
			layer.forwardingRules_Aout_ne = DoubleFactory2D.sparse.appendColumns(layer.forwardingRules_Aout_ne.viewPart(0, 0, N , index), layer.forwardingRules_Aout_ne.viewPart(0 , index + 1, N , layer.links.size() - index - 1));
			layer.forwardingRules_Ain_ne = DoubleFactory2D.sparse.appendColumns(layer.forwardingRules_Ain_ne.viewPart(0, 0, N , index), layer.forwardingRules_Ain_ne.viewPart(0 , index + 1, N , layer.links.size() - index - 1));
			layer.forwardingRulesNoFailureState_f_de = DoubleFactory2D.sparse.appendColumns(layer.forwardingRulesNoFailureState_f_de.viewPart(0, 0, D , index), layer.forwardingRulesNoFailureState_f_de.viewPart(0 , index + 1, D , layer.links.size() - index - 1));
			DoubleMatrix1D x_d_linkToRemove = layer.forwardingRulesCurrentFailureState_x_de.viewColumn(index).copy ();
			layer.forwardingRulesCurrentFailureState_x_de = DoubleFactory2D.sparse.appendColumns(layer.forwardingRulesCurrentFailureState_x_de.viewPart(0, 0, D , index), layer.forwardingRulesCurrentFailureState_x_de.viewPart(0 , index + 1, D , layer.links.size() - index - 1));
			NetPlan.removeNetworkElementAndShiftIndexes (layer.links , index);
			for (Demand d : layer.demands) if (x_d_linkToRemove.get(d.index) > PRECISION_FACTOR) layer.updateHopByHopRoutingDemand(d);
		}

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
		if (layer.netPlan != this.netPlan) throw new RuntimeException ("Bad");
		if (!layer.links.contains(this)) throw new RuntimeException ("Bad");
		double check_carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments = 0;
		double check_occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments = 0;

		if (isUp && layer.cache_linksDown.contains(this)) throw new RuntimeException ("Bad");
		if (!isUp && !layer.cache_linksDown.contains(this)) throw new RuntimeException ("Bad");
		if (!isUp)
		{
			if (Math.abs(cache_carriedTraffic) > 1e-3)
			{
				System.out.println ("Link " + this + ", is down, and carried traffic is: " + cache_carriedTraffic);
				System.out.println ("Traversing routes: " + cache_traversingRoutes);
				for (Route r : cache_traversingRoutes.keySet())
					System.out.println ("Route " + r + ", isDown? " + r.isDown() + ", carriedTraffic: " + r.getCarriedTraffic() + ", carried of all ok: " + r.currentCarriedTrafficIfNotFailing);
				if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING) System.out.println ("f_d for this link, all demands: " + layer.forwardingRulesNoFailureState_f_de.viewColumn(index));
				if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING) System.out.println ("x_d for this link, all demands: " + layer.forwardingRulesCurrentFailureState_x_de.viewColumn(index));
				throw new RuntimeException ("Bad");
			}
			if (Math.abs(cache_occupiedCapacity) > 1e-3) throw new RuntimeException ("Bad");
			for (Route r : cache_traversingRoutes.keySet()) if ((r.getCarriedTraffic() != 0) || (r.getOccupiedCapacity(this) != 0)) throw new RuntimeException ("Bad");
			for (MulticastTree r : cache_traversingTrees) if ((r.getCarriedTraffic() != 0) || (r.getOccupiedLinkCapacity() != 0)) throw new RuntimeException ("Bad");
		}
		
		for (SharedRiskGroup srg : netPlan.srgs) if (this.cache_srgs.contains(srg) != srg.links.contains(this)) throw new RuntimeException ("Bad");

		if (layer.routingType == RoutingType.SOURCE_ROUTING)
		{
//			System.out.println ("The routes traversing link " + this + " in layer: " + layer + " are " + cache_traversingRoutes);
//			for (Route r : cache_traversingRoutes.keySet()) System.out.println ("-- route: " + r + ", seq links real path: " + r.seqLinksRealPath);
			for (Route route : layer.routes)
			{
				if (this.cache_traversingRoutes.containsKey(route) != route.cache_seqLinksRealPath.contains(this)) throw new RuntimeException ("Bad. link: " + this + ", route: " + route + ", this.cache_traversingRoutes: " + this.cache_traversingRoutes + ", route.seqLinksRealPath: "+  route.cache_seqLinksRealPath);
				if (this.cache_traversingRoutes.containsKey(route))
				{
					int numPasses = 0; for (Link linkRoute : route.cache_seqLinksRealPath) if (linkRoute == this) numPasses ++; if (numPasses != this.cache_traversingRoutes.get(route)) throw new RuntimeException ("Bad");
					check_carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments += route.getCarriedTraffic();
					check_occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments += route.getOccupiedCapacity(this);
//					System.out.println ("Route " + route + ", traverses this link (" + this + "), numPasses: " + this.cache_traversingRoutes.get(route) + ", its carried traffic is: " + route.carriedTraffic + " and thus accumlates a carried traffic of: " + (route.carriedTraffic * this.cache_traversingRoutes.get(route)));
				}
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

		if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING)
		{
			final double x_e = layer.forwardingRulesCurrentFailureState_x_de.viewColumn(index).zSum();
			check_carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments += x_e;
			check_occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments += x_e;
		}

		if (Math.abs(cache_carriedTraffic - check_carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments) > 1E-3) 
		{
			System.out.println ("Link "+ this + ", is Up: " + isUp + ", trav routes: " + cache_traversingRoutes);
			for (Route r : cache_traversingRoutes.keySet()) System.out.println ("-- trav route: " + r + ", is down: " + r.isDown() + ", carried: " + r.getCarriedTraffic() + ", carried all ok: " + r.currentCarriedTrafficIfNotFailing + ", seqlinks real: " + r.cache_seqLinksRealPath);
			throw new RuntimeException ("Bad: Link: " + this + ". carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments: " + cache_carriedTraffic + ", check_carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments: " + check_carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments);
		}
		org.junit.Assert.assertEquals(cache_occupiedCapacity , check_occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments , 1E-3);
		
		if (coupledLowerLayerDemand != null)
			if (!coupledLowerLayerDemand.coupledUpperLayerLink.equals (this)) throw new RuntimeException ("Bad");
		if (coupledLowerLayerMulticastDemand != null)
			if (!coupledLowerLayerMulticastDemand.coupledUpperLayerLinks.containsValue(this)) throw new RuntimeException ("Bad");
	}
	
	void updateLinkTrafficAndOccupation ()
	{
		this.cache_carriedTraffic = 0;
		this.cache_occupiedCapacity = 0;
		for (Entry<Route,Integer> entry : cache_traversingRoutes.entrySet())
		{
			this.cache_carriedTraffic += entry.getKey().getCarriedTraffic();
			this.cache_occupiedCapacity += entry.getKey().getOccupiedCapacity(this);
		}
		for (MulticastTree t : cache_traversingTrees)
		{
			this.cache_carriedTraffic += t.getCarriedTraffic();
			this.cache_occupiedCapacity += t.getOccupiedLinkCapacity();
		}
	}

	/** Returns the set of links in this layer carry the traffic that traverses this link, before and after traversing it,
	 *  according to the routes/forwarding rules defined. 
	 * The method returns  a pair of sets (disjoint or not). In hop-by-hop routing mode second set is empty. 
	 * In source routing mode, first set includes the links considerinf non-backup routes, second set considering backup routes
	 * @param assumeNoFailureState in this case, the links are computed as if all network link/nodes are in no-failure state
	 * @return see above
	 */
	public Pair<Set<Link>,Set<Link>> getOtherLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  (boolean assumeNoFailureState)
	{
		final double tolerance = Configuration.precisionFactor;
		Set<Link> resPrimary = new HashSet<> ();
		Set<Link> resBackup = new HashSet<> ();
		if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING)
		{
			final int index_ae = originNode.index;
			final int index_be = destinationNode.index;
			final boolean someLinksFailed = !layer.cache_linksDown.isEmpty() || !netPlan.cache_nodesDown.isEmpty();
			for (Demand d : layer.demands)
			{
				final int index_ad = d.getIngressNode().index;
				if (d.getOfferedTraffic() == 0) continue;
				DoubleMatrix1D f_e;// = layer.forwardingRulesNoFailureState_f_de.viewRow(d.index);
				DoubleMatrix2D fundMatrix;
				if (someLinksFailed && !assumeNoFailureState)
				{
					f_e = layer.forwardingRulesNoFailureState_f_de.viewRow(d.index).copy();
					for (Link e : layer.cache_linksDown) f_e.set(e.index, 0);
					for (Node n : netPlan.cache_nodesDown)
					{
						for (Link e : n.getOutgoingLinks(layer)) f_e.set(e.index, 0);
						for (Link e : n.getIncomingLinks(layer)) f_e.set(e.index, 0);
					}
					fundMatrix = d.computeRoutingFundamentalMatrixDemand(f_e).getFirst();
				}
				else
				{
					f_e = layer.forwardingRulesNoFailureState_f_de.viewRow(d.index);
					fundMatrix = d.computeRoutingFundamentalMatrixDemand(f_e).getFirst();
				}
				/* If this link does not carry traffic of the demand => continue */
				if (fundMatrix.get(index_ad, index_ae) * f_e.get(index_ae) < tolerance) continue;
				/* See the links that carry traffic of this demand AND such traffic traversed BEFORE or AFTER this link */
				for (Link candLink : layer.links)
				{
					/* Candidate link does not carry demand traffic => continue */
					if (fundMatrix.get(index_ad, candLink.originNode.index) * f_e.get(candLink.index) == 0) continue; 
					/* if the traffic outgoing cand link enters my link, include it */
					if (fundMatrix.get(candLink.destinationNode.index , index_ae) > tolerance) resPrimary.add(candLink);
					/* if the traffic outgoing my link link enters cand link, include it */
					if (fundMatrix.get(index_be , candLink.originNode.index) > tolerance) resPrimary.add(candLink);
				}
			}
		}
		else
		{
			for (Route r : cache_traversingRoutes.keySet())
			{
				if (!assumeNoFailureState && r.isDown()) continue;
				for (Link e : r.getSeqLinks()) 
					if (r.isBackupRoute()) resBackup.add(e); else resPrimary.add(e);
			}
		}
		return Pair.of(resPrimary,resBackup);
	}
}
