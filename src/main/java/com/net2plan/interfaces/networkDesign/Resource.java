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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.Triple;

/** <p>.</p> 
 * @author Pablo Pavon-Marino
 */
public class Resource extends NetworkElement
{
	boolean isUp;
	Node hostNode;
	String capacityMeasurementUnits;
	double capacity;
	double ratioOutputVsInputTrafficTraversingRoutes; // 0 means no output whatever input, 1 means same, 0.5 means half output respect to input
	List<Triple<Resource,Double,Double>> resourcesConsumedInformation; // resource, fixed amount, proportional-to-in-traffic amount
	List<Route> traversingRoutes;
	
	Resource (NetPlan netPlan , long id , int index , Node hostNode , double capacity , String capacityMeasurementUnits, double ratioOutputVsInputTrafficTraversingRoutes , List<Triple<Resource,Double,Double>> resourcesConsumedInformation , AttributeMap attributes)
	{
		super (netPlan , id , index , attributes);

		if (!netPlan.equals(hostNode.netPlan)) throw new Net2PlanException ("The Resource host node is in a different NetPlan object (or removed)"); 
		if (capacity < 0) throw new Net2PlanException ("The capacity of a resource cannot be negative"); 
		if (ratioOutputVsInputTrafficTraversingRoutes < 0) throw new Net2PlanException ("The output vs input consumption ratio of a resource cannot be negative");
		for (Triple<Resource,Double,Double> resInfo : resourcesConsumedInformation)
		{
			final Resource r = resInfo.getFirst();
			if (!netPlan.equals(r.netPlan)) throw new Net2PlanException ("The consumed resources of a resource must be in the same NetPlan object");
			if (r.hostNode != hostNode) throw new Net2PlanException ("All the resources consumed by a resource must be in the same node resource");
			if (resInfo.getSecond() < 0) throw new Net2PlanException ("The fixed par of the consumed resources cannot be negative");
			if (resInfo.getThird() < 0) throw new Net2PlanException ("The proportional par of the consumed resources cannot be negative");
		}
		this.isUp = true;
		this.hostNode = hostNode;
		this.capacityMeasurementUnits = capacityMeasurementUnits;
		this.capacity = capacity;
		this.ratioOutputVsInputTrafficTraversingRoutes = ratioOutputVsInputTrafficTraversingRoutes; 
		this.resourcesConsumedInformation = new ArrayList<Triple<Resource,Double,Double>> (resourcesConsumedInformation); 
		this.traversingRoutes = new ArrayList<Route> ();
	}

	void copyFrom (Resource origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.meanTimeToFailInHours = origin.meanTimeToFailInHours;
		this.meanTimeToRepairInHours = origin.meanTimeToRepairInHours;
		this.links.clear (); for (Link e : origin.links) this.links.add(this.netPlan.getLinkFromId (e.id));
		this.nodes.clear (); for (Node n : origin.nodes) this.nodes.add(this.netPlan.getNodeFromId (n.id));
	}

	/**
	 * <p>Returns the set of nodes associated to the SRG (fail, when the SRG is in failure state)</p>
	 * @return The set of failing nodes, as an unmodifiable set
	 */
	public Set<Node> getNodes()
	{
		return Collections.unmodifiableSet(nodes);
	}

	/**
	 * <p>Returns all the links affected by the SRG at all the layers: the links affected, and the input and output links of the affected nodes</p>
	 * @return All the affected links
	 */
	public Set<Link> getAffectedLinks ()
	{
		Set<Link> res = new HashSet<Link> ();
		res.addAll (links);
		for (Node n : nodes) { res.addAll (n.cache_nodeIncomingLinks); res.addAll (n.cache_nodeOutgoingLinks); }
		return res;
	}

	/**
	 * <p>Returns all the links affected by this SRG, but only those at a particular layer. This includes the links involved, and the input and output links of the affected nodes</p>
	 * @param layer Network layer
	 * @return All the affected links at a given layer
	 */
	public Set<Link> getAffectedLinks (NetworkLayer layer)
	{
		Set<Link> res = new HashSet<Link> ();
		for (Link e : links) if (e.layer == layer) res.add (e);
		for (Node n : nodes) 
		{
			for (Link e : n.cache_nodeIncomingLinks) if (e.layer == layer) res.add (e);
			for (Link e : n.cache_nodeOutgoingLinks) if (e.layer == layer) res.add (e);
		}
		return res;
	}

	/**
	 * <p>Returns the set of routes affected by the SRG (fail, when the SRG is in failure state). </p>
	 * @return The set of failing routes
	 */
	public Set<Route> getAffectedRoutes ()
	{
		Set<Route> res = new HashSet<Route> ();
		for (Link e : links) res.addAll (e.cache_traversingRoutes.keySet());
		for (Node n : nodes) res.addAll (n.cache_nodeAssociatedRoutes);
		return res;
	}

	/**
	 * <p>Returns the set of routes in the given layer affected by the SRG (fail, when the SRG is in failure state)</p>
	 * @param layer Network layer
	 * @return The failing routes belonging to that layer
	 */
	public Set<Route> getAffectedRoutes (NetworkLayer layer)
	{
		Set<Route> res = new HashSet<Route> ();
		for (Link e : links) for (Route r : e.cache_traversingRoutes.keySet()) if (r.layer.equals(layer)) res.add (r);
		for (Node n : nodes) for (Route r : n.cache_nodeAssociatedRoutes) if (r.layer.equals(layer)) res.add (r);
		return res;
	}

	/**
	 * <p>Returns the set of multicast trees affected by the SRG (fail, when the SRG is in failure state). </p>
	 * @return The set of failing multicast trees
	 */
	public Set<MulticastTree> getAffectedMulticastTrees ()
	{
		Set<MulticastTree> res = new HashSet<MulticastTree> ();
		for (Link e : links) res.addAll (e.cache_traversingTrees);
		for (Node n : nodes) res.addAll (n.cache_nodeAssociatedulticastTrees);
		return res;
	}

	/**
	 * <p>Returns the set of multicast trees in the given layer affected by the SRG (fail, when the SRG is in failure state)</p>
	 * @param layer Network layer
	 * @return The failing multicast trees belonging to the given layer
	 */
	public Set<MulticastTree> getAffectedMulticastTrees (NetworkLayer layer)
	{
		Set<MulticastTree> res = new HashSet<MulticastTree> ();
		for (Link e : links) for (MulticastTree t : e.cache_traversingTrees) if (t.layer.equals(layer)) res.add (t);
		for (Node n : nodes) for (MulticastTree t : n.cache_nodeAssociatedulticastTrees) if (t.layer.equals(layer)) res.add (t);
		return res;
	}

	/**
	 * <p>Returns the set of protection segments affected by the SRG (fail, when the SRG is in failure state). </p>
	 * @return The set of failing protection segments
	 */
	public Set<ProtectionSegment> getAffectedProtectionSegments ()
	{
		Set<ProtectionSegment> res = new HashSet<ProtectionSegment> ();
		for (Link e : links) res.addAll (e.cache_traversingSegments);
		for (Node n : nodes) res.addAll (n.cache_nodeAssociatedSegments);
		return res;
	}

	/**
	 * <p>Returns the set of protection segments in the given layer affected by the SRG (fail, when the SRG is in failure state)</p>
	 * @param layer Network layer
	 * @return The failing protection segments belonging to the given layer
	 */
	public Set<ProtectionSegment> getAffectedProtectionSegments (NetworkLayer layer)
	{
		Set<ProtectionSegment> res = new HashSet<ProtectionSegment> ();
		for (Link e : links) for (ProtectionSegment s : e.cache_traversingSegments) if (s.layer.equals(layer)) res.add (s);
		for (Node n : nodes) for (ProtectionSegment s : n.cache_nodeAssociatedSegments) if (s.layer.equals(layer)) res.add (s);
		return res;
	}

	/**
	 * <p>Returns the set of links associated to the SRG (fail, when the SRG is in failure state).</p>
	 * @return The set of failing links, as an unmodifiable set
	 */
	public Set<Link> getLinks()
	{
		return Collections.unmodifiableSet(links);
	}

	/**
	 * <p>Returns the set of links associated to the SRG (fail, when the SRG is in failure state), but only those belonging to the given layer. </p>
	 * @param layer the layer
	 * @return The set of failing links
	 */
	public Set<Link> getLinks(NetworkLayer layer)
	{
		Set<Link> res = new HashSet<Link> ();
		for (Link e : links) if (e.layer.equals(layer)) res.add (e);
		return res;
	}

	/**
	 * <p>Returns the mean time to fail (MTTF) in hours of the SRG, that is, the average time between when it is repaired, and its next failure.</p>
	 * @return The MTTF in hours
	 */
	public double getMeanTimeToFailInHours()
	{
		return meanTimeToFailInHours;
	}

	/**
	 * <p>Sets the mean time to fail (MTTF) in hours of the SRG, that is, the average time between it is repaired, and the next failure.</p>
	 * @param value The new MTTF (it must be greater than zero)
	 */
	public void setMeanTimeToFailInHours(double value)
	{
		if (value <= 0) throw new Net2PlanException ("A positive value is expected");
		this.meanTimeToFailInHours = value;
	}

	/**
	 * <p>Returns the mean time to repair (MTTR) of the SRG, that is, the average time between a failure occurs, and it is repaired.</p>
	 * @return the MTTR in hours
	 */
	public double getMeanTimeToRepairInHours()
	{
		return meanTimeToRepairInHours;
	}

	/**
	 * <p>Sets the mean time to repair (MTTR) in hours of the SRG, that is, the average time  between a failure occurs, and it is repaired.</p>
	 * @param value the nnew MTTR (negative values are not accepted)
	 */
	public void setMeanTimeToRepairInHours(double value)
	{
		if (value < 0) throw new Net2PlanException ("A positive value is expected");
		this.meanTimeToRepairInHours = value;
	}

	/**
	 * <p>Returns the availability (between 0 and 1) of the SRG. That is, the fraction of time that the SRG is in non failure state. </p>
	 * <p>Availability is computed as: MTTF/ (MTTF + MTTR)</p>
	 * @return MTTF/ (MTTF + MTTR)
	 */
	public double getAvailability()
	{
		return meanTimeToFailInHours / (meanTimeToFailInHours + meanTimeToRepairInHours);
	}

	/**
	 * <p>Sets nodes and links associated to the  SRG as down (in case they are not yet). The network state is updated with the affected routes,
	 * segments, trees and hop-by-hop routing associated to the new nodes/links down </p>
	 */
	public void setAsDown ()
	{
		checkAttachedToNetPlanObject();
		netPlan.setLinksAndNodesFailureState (null , links , null , nodes);
	}
	
	/**
	 * <p>Removes a link from the set of links of the SRG. If the link is not in the SRG, no action is taken</p>
	 * @param e Link to be removed
	 */
	public void removeLink (Link e)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		e.cache_srgs.remove (this);
		links.remove (e);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}
	
	/**
	 * <p>Removes a node from the set of nodes of the SRG. If the node is not in the SRG, no action is taken</p>
	 * @param n Node to be removed
	 */
	public void removeNode (Node n)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		n.cache_nodeSRGs.remove (this);
		nodes.remove (n);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Removes this SRG.</p>
	 */
	public void remove()
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();

		for (Node node : nodes) node.cache_nodeSRGs.remove (this);
		for (Link link : links) link.cache_srgs.remove (this);
		netPlan.cache_id2srgMap.remove (id);
		NetPlan.removeNetworkElementAndShiftIndexes(netPlan.srgs , index);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		removeId ();
	}

	/**
	 * <p>Adds a link to the SRG. The object cannot be a protection segment (recall that segments are subclasses of Link). </p>
	 * @param link Link to add
	 */
	public void addLink(Link link)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		link.checkAttachedToNetPlanObject(this.netPlan);
		if (link instanceof ProtectionSegment) throw new Net2PlanException ("Protections segments cannot be added as links of a SRG");

		link.cache_srgs.add(this);
		this.links.add(link);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Adds a node to the SRG.</p>
	 * @param node Node to add
	 */
	public void addNode(Node node)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		node.checkAttachedToNetPlanObject(this.netPlan);

		node.cache_nodeSRGs.add(this);
		this.nodes.add(node);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Returns a {@code String} representation of the Shared Risk Group.</p>
	 * @return {@code String} representation of the SRG
	 */
	public String toString () { return "srg" + index + " (id " + id + ")"; }

	void checkCachesConsistency ()
	{
		for (Link link : links) if (!link.cache_srgs.contains(this)) throw new RuntimeException ("Bad");
		for (Node node : nodes) if (!node.cache_nodeSRGs.contains(this)) throw new RuntimeException ("Bad");
	}


}
