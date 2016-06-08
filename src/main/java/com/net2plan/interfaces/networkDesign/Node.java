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
import cern.colt.matrix.tdouble.DoubleFactory2D;
import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.UnmodifiablePoint2D;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;

import java.awt.geom.Point2D;
import java.util.*;


/** <p>This class contains a representation of a node.
 * <ul>
 *     <li>A node has a name and a (X,Y) position in a map. It can be up or down (failed).</li>
 *     <li>In multilayer networks, nodes can have links at different layers. </li>
 *     <li>A node can belong to one or more shared risk groups (SRG).</li>
 *     <li>A node can be up or down. The behavior respect to the routing in the input and output nodes links, of having a node down, is equivalent
 *  to making all the node input and output links as down.</li>
 *</ul>
 * @author Pablo Pavon-Marino
 * @since 0.4.0 */
public class Node extends NetworkElement
{
	String name;
	Point2D nodeXYPositionMap;
	boolean isUp;
	
	Set<Link> cache_nodeIncomingLinks;
	Set<Link> cache_nodeOutgoingLinks;
	Set<Demand> cache_nodeIncomingDemands;
	Set<Demand> cache_nodeOutgoingDemands;
	Set<MulticastDemand> cache_nodeIncomingMulticastDemands;
	Set<MulticastDemand> cache_nodeOutgoingMulticastDemands;
	Set<SharedRiskGroup> cache_nodeSRGs;
	Set<Route> cache_nodeAssociatedRoutes;
	Set<ProtectionSegment> cache_nodeAssociatedSegments;
	Set<MulticastTree> cache_nodeAssociatedulticastTrees;


	/**
	 * Default constructor.
	 *
	 * @since 0.2.0
	 */
	protected Node (NetPlan netPlan , long id , int index , double xCoord, double yCoord, String name, AttributeMap attributes)
	{
		super (netPlan , id , index , attributes);
		
		this.nodeXYPositionMap = new UnmodifiablePoint2D(xCoord, yCoord);
		this.name = name == null? "" : name;
		this.isUp = true;
		
		this.cache_nodeIncomingLinks = new HashSet<Link> ();
		this.cache_nodeOutgoingLinks = new HashSet<Link> ();
		this.cache_nodeIncomingDemands = new HashSet<Demand> ();
		this.cache_nodeOutgoingDemands = new HashSet<Demand> ();
		this.cache_nodeIncomingMulticastDemands = new HashSet<MulticastDemand> ();
		this.cache_nodeOutgoingMulticastDemands = new HashSet<MulticastDemand> ();
		this.cache_nodeSRGs = new HashSet<SharedRiskGroup> ();
		this.cache_nodeAssociatedRoutes = new HashSet<Route> ();
		this.cache_nodeAssociatedSegments = new HashSet<ProtectionSegment> ();
		this.cache_nodeAssociatedulticastTrees = new HashSet<MulticastTree> ();

	
	}
	
	void copyFrom (Node origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.name = origin.name;
		this.nodeXYPositionMap = new UnmodifiablePoint2D(origin.nodeXYPositionMap.getX() , origin.nodeXYPositionMap.getY());
		this.isUp = origin.isUp;
		this.cache_nodeIncomingLinks.clear (); for (Link e : origin.cache_nodeIncomingLinks) this.cache_nodeIncomingLinks.add(this.netPlan.getLinkFromId (e.id));
		this.cache_nodeOutgoingLinks.clear (); for (Link e : origin.cache_nodeOutgoingLinks) this.cache_nodeOutgoingLinks.add(this.netPlan.getLinkFromId (e.id));
		this.cache_nodeIncomingDemands.clear (); for (Demand d : origin.cache_nodeIncomingDemands) this.cache_nodeIncomingDemands.add(this.netPlan.getDemandFromId (d.id));
		this.cache_nodeOutgoingDemands.clear (); for (Demand d : origin.cache_nodeOutgoingDemands) this.cache_nodeOutgoingDemands.add(this.netPlan.getDemandFromId (d.id));
		this.cache_nodeIncomingMulticastDemands.clear (); for (MulticastDemand d : origin.cache_nodeIncomingMulticastDemands) this.cache_nodeIncomingMulticastDemands.add(this.netPlan.getMulticastDemandFromId(d.id));
		this.cache_nodeOutgoingMulticastDemands.clear (); for (MulticastDemand d : origin.cache_nodeOutgoingMulticastDemands) this.cache_nodeOutgoingMulticastDemands.add(this.netPlan.getMulticastDemandFromId(d.id));
		this.cache_nodeSRGs.clear (); for (SharedRiskGroup s : origin.cache_nodeSRGs) this.cache_nodeSRGs.add(this.netPlan.getSRGFromId(s.id));
		this.cache_nodeAssociatedRoutes.clear (); for (Route r : origin.cache_nodeAssociatedRoutes) this.cache_nodeAssociatedRoutes.add(this.netPlan.getRouteFromId (r.id));
		this.cache_nodeAssociatedSegments.clear (); for (ProtectionSegment s : origin.cache_nodeAssociatedSegments) this.cache_nodeAssociatedSegments.add(this.netPlan.getProtectionSegmentFromId(s.id));
		this.cache_nodeAssociatedulticastTrees.clear (); for (MulticastTree t : origin.cache_nodeAssociatedulticastTrees) this.cache_nodeAssociatedulticastTrees.add(this.netPlan.getMulticastTreeFromId(t.id));
	}
	
	/**
	 * <p>Returns the node name</p>
	 * @return The node name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * <p>Sets the node name. It does not have to be unique among the network nodes</p>
	 * @param name New ndoe name
	 */
	public void setName(String name)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		this.name = name == null? "" : name;
	}

	/**
	 * <p>Returns the total unicast offered traffic ending in the node, counting the demands at the given layer.
	 * If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The total unicast egress traffic ending in the node
	 */
	public double getEgressOfferedTraffic (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (Demand d : cache_nodeIncomingDemands) if (d.layer.equals (layer)) accum += d.offeredTraffic;
		return accum;
	}

	/**
	 * <p>Returns the total unicast offered traffic initited in the node, counting the demands at the given layer.
	 * If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The total offered traffic initiated in the node
	 */
	public double getIngressOfferedTraffic (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (Demand d : cache_nodeOutgoingDemands) if (d.layer.equals (layer)) accum += d.offeredTraffic;
		return accum;
	}

	/**
	 * <p>Returns the total multicast offered traffic ending in the node, counting the multicast demands at the given layer.
	 * If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The total multicast offered traffic ending in the node
	 */
	public double getEgressOfferedMulticastTraffic (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (MulticastDemand d : cache_nodeIncomingMulticastDemands) if (d.layer.equals (layer)) accum += d.offeredTraffic;
		return accum;
	}

	/**
	 * <p>Returns the total multicast offered traffic initiated in the node, counting the multicast demands at the given layer.
	 * If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The total multicast offered traffic initiated in the node
	 */
	public double getIngressOfferedMulticastTraffic (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (MulticastDemand d : cache_nodeOutgoingMulticastDemands) if (d.layer.equals (layer)) accum += d.offeredTraffic;
		return accum;
	}

	/**
	 * <p>Removes all forwarding rules associated to the node for a given layer (that is, of layer links outgoing from the node). If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter the layer 
	 */
	public void removeAllForwardingRules(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		for (Link e : cache_nodeOutgoingLinks) if (e.layer.equals (layer)) e.removeAllForwardingRules();
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	
	/**
	 * <p>Returns the total current carried traffic ending in the node, counting the demands at the given layer.
	 * If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The carried traffic ending in the node
	 */
	public double getEgressCarriedTraffic (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (Demand d : cache_nodeIncomingDemands) if (d.layer.equals (layer)) accum += d.carriedTraffic;
		return accum;
	}

	/**
	 * <p>Returns the total current carried traffic initiated in the node, counting the demands at the given layer.
	 * If no layer is provided, the default layer is assumed</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The total carried traffic initiated in the node
	 */
	public double getIngressCarriedTraffic (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (Demand d : cache_nodeOutgoingDemands) if (d.layer.equals (layer)) accum += d.carriedTraffic;
		return accum;
	}

	/**
	 * <p>Returns the total current multicast carried traffic ending in the node, counting the multicast demands at the given layer.
	 * If no layer is provided, the default layer is assumed. </p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The carried traffic ending in the node
	 */
	public double getEgressCarriedMulticastTraffic (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (MulticastDemand d : cache_nodeIncomingMulticastDemands) if (d.layer.equals (layer)) accum += d.carriedTraffic;
		return accum;
	}

	/**
	 * <p>Returns the total current multicast carried traffic initiated in the node, counting the multicast demands at the given layer.
	 * If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The total multicast carried traffic initiated in the node
	 */
	public double getIngressCarriedMulticastTraffic (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (MulticastDemand d : cache_nodeOutgoingMulticastDemands) if (d.layer.equals (layer)) accum += d.carriedTraffic;
		return accum;
	}

	/**
	 * <p>Returns the nodes directly connected to this, with links ending in this node at the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The set of neighbor nodes
	 */
	public Set<Node> getInNeighbors (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<Node> res = new HashSet<Node> ();
		for (Link e : cache_nodeIncomingLinks) if (e.layer.equals (layer)) res.add (e.originNode);
		return res;
	}

	/**
	 * <p>Returns the nodes directly connected to this, with links initiated in this node at the given layer. If no layer is provided, the default layer is assumed</p>
	 * @param optionalLayerParameter Network layer
	 * @return The set of neighbor nodes
	 */
	public Set<Node> getOutNeighbors (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<Node> res = new HashSet<Node> ();
		for (Link e : cache_nodeOutgoingLinks) if (e.layer.equals (layer)) res.add (e.destinationNode);
		return res;
	}

	/**
	 * <p>Returns the nodes directly connected to this, with links initiated in this node at any layer.</p>
	 * @return The set of neighbor nodes
	 */
	public Set<Node> getOutNeighborsAllLayers ()
	{
		Set<Node> res = new HashSet<Node> ();
		for (Link e : cache_nodeOutgoingLinks) res.add (e.destinationNode);
		return res;
	}

	/**
	 * <p>Returns the nodes directly connected to this, with links ending in this node at any layer.</p>
	 * @return The set of neighbor nodes
	 */
	public Set<Node> getInNeighborsAllLayers ()
	{
		Set<Node> res = new HashSet<Node> ();
		for (Link e : cache_nodeIncomingLinks) res.add (e.originNode);
		return res;
	}

	/**
	 * <p>Returns node position in the map</p>
	 * @return The position in the map used for visualization
	 */
	public Point2D getXYPositionMap()
	{
		return nodeXYPositionMap;
	}

	/**
	 * <p>Sets the node position in the map, used for visualization </p>
	 * @param pos New node position
	 * @see java.awt.geom.Point2D
	 */
	public void setXYPositionMap(Point2D pos)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		this.nodeXYPositionMap = pos;
	}

	/**
	 * <p>Returns true if the node is up (no failure state)</p>
	 * @return See description above
	 */
	public boolean isUp()
	{
		return isUp;
	}

	/**
	 * <p>Returns true if the node is down (failure state)</p>
	 * @return See description above
	 */
	public boolean isDown()
	{
		return !isUp;
	}

	/**
	 * <p>Returns the set of links ending in the node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter the layer
	 * @return the of incoming links, or an empty set if none
	 */
	public Set<Link> getIncomingLinks(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<Link> res = new HashSet<Link> (); for (Link e : cache_nodeIncomingLinks) if (e.layer.equals(layer)) res.add (e);
		return res;
	}

	/**
	 * <p>Returns the set of links initiated in the node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The set of outgoing links, or an empty set if none
	 */
	public Set<Link> getOutgoingLinks(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<Link> res = new HashSet<Link> (); for (Link e : cache_nodeOutgoingLinks) if (e.layer.equals(layer)) res.add (e);
		return res;
	}

	/**
	 * <p>Returns the set of links initiated in the node in all layer.</p>
	 * @return The set of outgoing links, or an empty set if none (as an unmodifiable set)
	 */
	public Set<Link> getOutgoingLinksAllLayers ()
	{
		return Collections.unmodifiableSet(cache_nodeOutgoingLinks);
	}

	/**
	 * <p>Returns the set of links ending in the node at any layer.</p>
	 * @return The set of incoming links, or an empty set if none (as an unmodifiable set)	 */
	public Set<Link> getIncomingLinksAllLayers ()
	{
		return Collections.unmodifiableSet(cache_nodeIncomingLinks);
	}

	/**
	 * <p>Returns the set of demands initiated in the node, in any layer. </p>
	 * @return the set of demands, or an empty set if none (as an unmodifiable set)
	 */
	public Set<Demand> getOutgoingDemandsAllLayers ()
	{
		return Collections.unmodifiableSet(cache_nodeOutgoingDemands);
	}

	/**
	 * <p>Returns the set of demands ending in the node, in any layer. </p>
	 * @return The set of demands, or an empty set if none (as an unmodifiable set)
	 */
	public Set<Demand> getIncomingDemandsAllLayers ()
	{
		return Collections.unmodifiableSet(cache_nodeIncomingDemands);
	}

	/**
	 * <p>Returns the set of multicast demands initiated in the node, in any layer. </p>
	 * @return The set of demands, or an empty set if none (as an unmodifiable set)
	 */
	public Set<MulticastDemand> getOutgoingMulticastDemandsAllLayers ()
	{
		return Collections.unmodifiableSet(cache_nodeOutgoingMulticastDemands);
	}

	/**
	 * <p>Returns the set of multicast demands ending in the node, in any layer.</p>
	 * @return The set of demands, or an empty set if none (as an unmodifiable set)
	 */
	public Set<MulticastDemand> getIncomingMulticastDemandsAllLayers ()
	{
		return Collections.unmodifiableSet(cache_nodeIncomingMulticastDemands);
	}

	/**
	 * <p>Returns the set of demands ending in the node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return the demands, or an empty set if none
	 */
	public Set<Demand> getIncomingDemands(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<Demand> res = new HashSet<Demand> (); for (Demand e : cache_nodeIncomingDemands) if (e.layer.equals(layer)) res.add (e);
		return res;
	}

	/**
	 * <p>Set the failure state of the node: up or down. Returns the previous failure state. The routing is automatically updated, making the traffic
	 * of the traversing routes and segments as zero, and the hop-by-hop routing is updated as if the forwarding rules of input and output links were zero</p>
	 * @param setAsUp The new failure state ({@code true} up, {@code false} down)
	 * @return The previous failure state
	 */
	public boolean setFailureState (boolean setAsUp)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		
		if (isUp == setAsUp) return isUp;
		List<Node> aux = new LinkedList<Node> (); aux.add(this);
		if (setAsUp) netPlan.setLinksAndNodesFailureState (null , null , aux , null); else netPlan.setLinksAndNodesFailureState (null, null , null , aux); 
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		return !setAsUp; // the previous state
	}

	
	/**
	 * <p>Returns the set of routes ending in the node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer
	 * @return The routes, or an empty set if none
	 */
	public Set<Route> getIncomingRoutes(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		Set<Route> res = new HashSet<Route> (); for (Demand e : cache_nodeIncomingDemands) if (e.layer.equals(layer)) res.addAll (e.cache_routes);
		return res;
	}

	/**
	 * <p>Returns the set of multicast trees ending in the node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The multicast trees, or an empty set if none
	 */
	public Set<MulticastTree> getIncomingMulticastTrees (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<MulticastTree> res = new HashSet<MulticastTree> (); for (MulticastDemand e : cache_nodeIncomingMulticastDemands) if (e.layer.equals(layer)) res.addAll (e.cache_multicastTrees);
		return res;
	}

	/**
	 * <p>Returns the set of routes initiated in the node, in the given layer. If no layer is provided, the default layer is assumed. </p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The set of routes, or an empty set if none
	 */
	public Set<Route> getOutgoingRoutes(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		Set<Route> res = new HashSet<Route> (); for (Demand e : cache_nodeOutgoingDemands) if (e.layer.equals(layer)) res.addAll (e.cache_routes);
		return res;
	}

	/**
	 * <p>Returns the set of multicast tree initiated in the node, in the given layer. If no layer is provided, the default layer is assumed. </p>
	 * @param optionalLayerParameter The layer
	 * @return The multicast trees, or an empty set if none
	 */
	public Set<MulticastTree> getOutgoingMulticastTrees (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<MulticastTree> res = new HashSet<MulticastTree> (); for (MulticastDemand e : cache_nodeOutgoingMulticastDemands) if (e.layer.equals(layer)) res.addAll (e.cache_multicastTrees);
		return res;
	}


	/**
	 * <p>Returns the set of demands initiated in the node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The demands, or an empty set if none
	 */
	public Set<Demand> getOutgoingDemands(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<Demand> res = new HashSet<Demand> (); for (Demand e : cache_nodeOutgoingDemands) if (e.layer.equals(layer)) res.add (e);
		return res;
	}

	/**
	 * <p>Returns the set of multicast demands ending in the node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The demands, or an empty set if none
	 */
	public Set<MulticastDemand> getIncomingMulticastDemands(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<MulticastDemand> res = new HashSet<MulticastDemand> (); for (MulticastDemand e : cache_nodeIncomingMulticastDemands) if (e.layer.equals(layer)) res.add (e);
		return res;
	}

	/**
	 * <p>Returns the set of multicast demands initiated in the node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The demands, or an empty set if none
	 */
	public Set<MulticastDemand> getOutgoingMulticastDemands(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<MulticastDemand> res = new HashSet<MulticastDemand> (); for (MulticastDemand e : cache_nodeOutgoingMulticastDemands) if (e.layer.equals(layer)) res.add (e);
		return res;
	}

	/**
	 * <p>Returns the set of routes that start, end or traverse this node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The routes, or an empty set if none
	 */
	public Set<Route> getAssociatedRoutes (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<Route> res = new HashSet<Route> (); 
		for (Link e : cache_nodeIncomingLinks) if (e.layer.equals (layer)) res.addAll (e.cache_traversingRoutes.keySet()); 
		for (Link e : cache_nodeOutgoingLinks) if (e.layer.equals (layer)) res.addAll (e.cache_traversingRoutes.keySet()); 
		return res;
	}

	/**
	 * <p>Returns the set of multicast trees that start, end or traverse this node, in the given layer. If no layer is provided, the default layer is assumed. </p>
	 * @param optionalLayerParameter Network layer (optional
	 * @return The trees, or an empty set if none
	 */
	public Set<MulticastTree> getAssociatedMulticastTrees (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<MulticastTree> res = new HashSet<MulticastTree> (); 
		for (Link e : cache_nodeIncomingLinks) if (e.layer.equals (layer)) res.addAll (e.cache_traversingTrees); 
		for (Link e : cache_nodeOutgoingLinks) if (e.layer.equals (layer)) res.addAll (e.cache_traversingTrees); 
		return res;
	}

	/**
	 * <p>Returns the set of protection segments that start, end or traverse this node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The segments, or an empty set if none
	 */
	public Set<ProtectionSegment> getAssociatedProtectionSegments (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<ProtectionSegment> res = new HashSet<ProtectionSegment> (); 
		for (ProtectionSegment s : cache_nodeAssociatedSegments) if (s.layer.equals (layer)) res.add (s);
		return res;
	}

	/**
	 * <p>Returns the set of shared risk groups (SRGs) this node belongs to. </p>
	 * @return The set of SRGs as an unmodifiable set
	 */
	public Set<SharedRiskGroup> getSRGs()
	{
		return (Set<SharedRiskGroup>) Collections.unmodifiableSet(cache_nodeSRGs);
	}

	/**
	 * <p>Removes a node, and any associated link, demand, route, protection segment or forwarding rule.</p>
	 */
	public void remove ()
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();

		for (MulticastTree tree : new LinkedList<MulticastTree> (cache_nodeAssociatedulticastTrees)) tree.remove ();
		for (ProtectionSegment segment : new LinkedList<ProtectionSegment> (cache_nodeAssociatedSegments)) segment.remove ();
		for (Route route : new LinkedList<Route> (cache_nodeAssociatedRoutes)) route.remove ();
		for (SharedRiskGroup srg : new LinkedList<SharedRiskGroup> (cache_nodeSRGs)) srg.remove ();
		for (Link link : new LinkedList<Link> (cache_nodeIncomingLinks)) link.remove ();
		for (Link link : new LinkedList<Link> (cache_nodeOutgoingLinks)) link.remove ();
		for (Demand demand : new LinkedList<Demand> (cache_nodeIncomingDemands)) demand.remove ();
		for (Demand demand : new LinkedList<Demand> (cache_nodeOutgoingDemands)) demand.remove ();
		for (MulticastDemand demand : new LinkedList<MulticastDemand> (cache_nodeIncomingMulticastDemands)) demand.remove ();
		for (MulticastDemand demand : new LinkedList<MulticastDemand> (cache_nodeOutgoingMulticastDemands)) demand.remove ();

		for (NetworkLayer layer : netPlan.layers)
		{
			if (layer.routingType != RoutingType.HOP_BY_HOP_ROUTING) continue;
			final int E = layer.links.size ();
			layer.forwardingRules_Aout_ne = DoubleFactory2D.sparse.appendRows(layer.forwardingRules_Aout_ne.viewPart(0, 0, index , E).copy () , layer.forwardingRules_Aout_ne.viewPart(index + 1, 0 , netPlan.nodes.size() - index - 1 , E).copy ());
			layer.forwardingRules_Ain_ne = DoubleFactory2D.sparse.appendRows(layer.forwardingRules_Ain_ne.viewPart(0, 0, index , E), layer.forwardingRules_Ain_ne.viewPart(index + 1, 0 , netPlan.nodes.size() - index - 1 , E));
		}

		netPlan.cache_id2NodeMap.remove (id);
		NetPlan.removeNetworkElementAndShiftIndexes(netPlan.nodes , this.index);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		removeId ();
	}

	/**
	 * <p>Returns a {@code String} representation of the node.</p>
	 * @return {@code String} representation of the node
	 */
	public String toString () { return "n" + index + " (id " + id + ")"; }

	/**
	 * <p>Returns the set of forwarding rules of links initiated in the node of the given layer,
	 * which have a non-zero splitting factor.  If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The map associating the demand-link pair, with the associated splitting factor
	 */
	public Map<Pair<Demand,Link>,Double> getForwardingRules (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		Map<Pair<Demand,Link>,Double> res = new HashMap<Pair<Demand,Link>,Double> ();
		for (Link e : getOutgoingLinks(layer))
		{
			IntArrayList ds = new IntArrayList (); DoubleArrayList vals = new DoubleArrayList ();
			layer.forwardingRules_f_de.viewColumn(e.index).getNonZeros(ds,vals);
			for (int cont = 0 ; cont < ds.size () ; cont ++)
				res.put (Pair.of (layer.demands.get(ds.get(cont)) , e) , vals.get(cont));
		}
		return res;
	}

	/**
	 *  <p>Returns the set of forwarding rules of links initiated in the node and associated to the given demand (the links are then in the same layer
	 * as the demand), that have a non-zero splitting factor</p>
	 * @param demand The demand
	 * @return The map associating the demand-link pair, with the associated splitting factor
	 */
	public Map<Pair<Demand,Link>,Double> getForwardingRules (Demand demand)
	{
		NetworkLayer layer = demand.layer;
		layer.checkAttachedToNetPlanObject(netPlan); 
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		Map<Pair<Demand,Link>,Double> res = new HashMap<Pair<Demand,Link>,Double> ();
		for (Link e : getOutgoingLinks(layer))
			if (layer.forwardingRules_f_de.get(demand.index , e.index) > 0)
				res.put (Pair.of (demand , e) , layer.forwardingRules_f_de.get(demand.index , e.index));
		return res;
	}
	
	void checkCachesConsistency ()
	{
		if (isUp && netPlan.cache_nodesDown.contains(this)) throw new RuntimeException ("Bad");
		if (!isUp && !netPlan.cache_nodesDown.contains(this)) throw new RuntimeException ("Bad");
		for (Link link : cache_nodeIncomingLinks) if (link.destinationNode != this) throw new RuntimeException ("Bad");
		for (Link link : cache_nodeOutgoingLinks) if (link.originNode != this) throw new RuntimeException ("Bad");
		for (Demand demand : cache_nodeIncomingDemands) if (demand.egressNode != this) throw new RuntimeException ("Bad");
		for (Demand demand : cache_nodeOutgoingDemands) if (demand.ingressNode != this) throw new RuntimeException ("Bad");
		for (MulticastDemand demand : cache_nodeIncomingMulticastDemands) if (!demand.egressNodes.contains(this)) throw new RuntimeException ("Bad");
		for (MulticastDemand demand : cache_nodeOutgoingMulticastDemands) if (demand.ingressNode != this) throw new RuntimeException ("Bad");
		for (SharedRiskGroup srg : cache_nodeSRGs) if (!srg.nodes.contains(this)) throw new RuntimeException ("Bad");
		for (Route route : cache_nodeAssociatedRoutes) if (!route.seqNodesRealPath.contains(this)) throw new RuntimeException ("Bad: " + cache_nodeAssociatedRoutes);
		for (ProtectionSegment segment : cache_nodeAssociatedSegments) if (!segment.seqNodes.contains(this)) throw new RuntimeException ("Bad");
		for (MulticastTree tree : cache_nodeAssociatedulticastTrees) if (!tree.cache_traversedNodes.contains(this)) throw new RuntimeException ("Bad");
	}
	
}
