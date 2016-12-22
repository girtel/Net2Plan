// 20161205 sigo con reader v5 y con la parte de save, para el route

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;


/**
 * <p>This class contains a representation of a unidirectional route, an structure used to carry traffic of unicast demands at a layer,
 *  when the layer routing type is source routing. Routes are characterized by the unicast demand they carry traffic of, the traversed links which should 
 *   form a path from the demand ingress node to the demand egress node, the amount of traffic of the demand that they carry (in traffic units of the layer), 
 *   amount of link capacity that they occupy in the traversed links (in link capacoty units for its layer). </p>
 *   <p> Naturally, the links and the demand belong to the same layer, the layer of the route. </p>
 * <p> Routes can be rerouted (change in its sequence of links), and its carried traffic and occupied capacity can change, but not its associate demand. 
 * If a route traverses a link or node that is down, its carried traffic automatically drops to zero. If the failing links/nodes are set to up state, its 
 * carried traffic and occupied link capacity goes back to the value before the failure. </p>
 * <p> Routes can be associated to an arbitrary number of protection segments. Protection segments are just reserved capacity in the links, that a route may 
 * use. Typically, a built-in or user-made algorithm that reacts to link and node failures decides how and when the assigned protection segments to a route are used. 
 *   </p>
 * @author Pablo Pavon-Marino
 * @since 0.4.0
 */

public class Route extends NetworkElement
{
	final NetworkLayer layer;
	final Demand demand;
	final Node ingressNode;
	final Node egressNode;
	final List<NetworkElement> initialSeqLinksAndResourcesTraversedWhenCreated; // each object is a Link, or a Resource 
	final List<Double> initialLinksAndResourcesOccupation;  // for each resource, the total occupation in it (if more than one pass, the total effect
	final double initialCarriedTrafficWhenCreated; // each object is a Link, or a Resource 
	List<NetworkElement> seqLinksSegmentsAndResourcesTraversed; // each object is a Link, or a Resource
	Set<ProtectionSegment> potentialBackupSegments;
	double carriedTrafficIfNotFailing;
	List<Double> occupiedLinksSegmentsAndResourcesCapacitiesIfNotFailing;
	List<Link> cache_seqLinksAndProtectionSegments;
	List<Link> cache_seqLinksRealPath;
	List<Node> cache_seqNodesRealPath;
	Map<NetworkElement,Double> cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap;  // for each resource, the total occupation in it (if more than one pass, the total effect

	Route (NetPlan netPlan , long id , int index , Demand demand , double initialCarriedTrafficWhenCreated , List<? extends NetworkElement> seqLinksRealPathAndResourcesTraversedWhenCreated , List<Double> occupationInformationInTraversedLinksAndResources , AttributeMap attributes)
	{
		super (netPlan , id , index , attributes);

		if (!netPlan.equals(demand.netPlan)) throw new RuntimeException ("Bad");
		for (NetworkElement e : seqLinksRealPathAndResourcesTraversedWhenCreated) 
		{ 
			if (e instanceof Link) if (!netPlan.equals(e.netPlan)) throw new RuntimeException ("Bad"); 
			if (e instanceof ProtectionSegment) throw new RuntimeException ("Bad"); 
			if (e instanceof Resource) if (!netPlan.equals(e.netPlan)) throw new RuntimeException ("Bad");  
		}
		Pair<List<Link>,List<Resource>> pairListLinkListResources = netPlan.checkPathValidityForDemand (seqLinksRealPathAndResourcesTraversedWhenCreated, demand);
		if (occupationInformationInTraversedLinksAndResources.size() != seqLinksRealPathAndResourcesTraversedWhenCreated.size()) throw new Net2PlanException ("Wrong input parameters in the route");
		for (Double val : occupationInformationInTraversedLinksAndResources) if (val < 0) throw new Net2PlanException ("The occupation of a link or a resource cannot be negative");
		if (initialCarriedTrafficWhenCreated < 0) throw new Net2PlanException ("Carried traffic cannot be negative");
		
		this.layer = demand.getLayer ();
		this.demand = demand;
		this.ingressNode = demand.ingressNode;
		this.egressNode = demand.egressNode;
		this.initialSeqLinksAndResourcesTraversedWhenCreated = new ArrayList<NetworkElement> (seqLinksRealPathAndResourcesTraversedWhenCreated);
		this.initialLinksAndResourcesOccupation = new ArrayList<Double> (occupationInformationInTraversedLinksAndResources);
		this.seqLinksSegmentsAndResourcesTraversed = new LinkedList<NetworkElement> (seqLinksRealPathAndResourcesTraversedWhenCreated);
		this.potentialBackupSegments = new HashSet <ProtectionSegment> ();
		this.initialCarriedTrafficWhenCreated = initialCarriedTrafficWhenCreated;
		this.carriedTrafficIfNotFailing = 0; 
		this.occupiedLinksSegmentsAndResourcesCapacitiesIfNotFailing = new ArrayList<Double> (occupationInformationInTraversedLinksAndResources); 
		this.cache_seqLinksAndProtectionSegments = Route.listLinksAndSegmentsWithoutResources(seqLinksSegmentsAndResourcesTraversed);
		this.cache_seqLinksRealPath = Route.listLinksRealPath(seqLinksRealPathAndResourcesTraversedWhenCreated); 
		this.cache_seqNodesRealPath = Route.listTraversedNodes(cache_seqLinksRealPath);
		this.cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap = updateLinkSegmentResourceOccupationCache ();
	}

	boolean isDeepCopy (Route e2)
	{
		if (!super.isDeepCopy(e2)) return false;
		if (this.layer.id != e2.layer.id) return false;
		if (this.demand.id != e2.demand.id) return false;
		if (this.ingressNode.id != e2.ingressNode.id) return false;
		if (this.egressNode.id != e2.egressNode.id) return false;
		if (!NetPlan.isDeepCopy(this.initialSeqLinksAndResourcesTraversedWhenCreated , e2.initialSeqLinksAndResourcesTraversedWhenCreated)) return false;
		if (!this.initialLinksAndResourcesOccupation.equals(e2.initialLinksAndResourcesOccupation)) return false;
		if (!NetPlan.isDeepCopy(this.seqLinksSegmentsAndResourcesTraversed , e2.seqLinksSegmentsAndResourcesTraversed)) return false;
		if (!NetPlan.isDeepCopy(this.cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap , e2.cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap)) return false;
		if (!NetPlan.isDeepCopy(this.potentialBackupSegments , e2.potentialBackupSegments)) return false;
		if (this.carriedTrafficIfNotFailing != e2.carriedTrafficIfNotFailing) return false;
		if (!this.occupiedLinksSegmentsAndResourcesCapacitiesIfNotFailing.equals(e2.occupiedLinksSegmentsAndResourcesCapacitiesIfNotFailing)) return false;
		if (!NetPlan.isDeepCopy(this.cache_seqLinksAndProtectionSegments , e2.cache_seqLinksAndProtectionSegments)) return false;
		if (!NetPlan.isDeepCopy(this.cache_seqLinksRealPath , e2.cache_seqLinksRealPath)) return false;
		if (!NetPlan.isDeepCopy(this.cache_seqNodesRealPath , e2.cache_seqNodesRealPath)) return false;
		if (!NetPlan.isDeepCopy(this.cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap , e2.cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap)) return false;
		return true;
	}

	void copyFrom (Route origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.carriedTrafficIfNotFailing = origin.carriedTrafficIfNotFailing;
		this.occupiedLinksSegmentsAndResourcesCapacitiesIfNotFailing = new ArrayList<Double> (origin.occupiedLinksSegmentsAndResourcesCapacitiesIfNotFailing);

		
		this.initialSeqLinksAndResourcesTraversedWhenCreated.clear();
		for (NetworkElement e : origin.initialSeqLinksAndResourcesTraversedWhenCreated) this.initialSeqLinksAndResourcesTraversedWhenCreated.add(getInThisNetPlan (e));

		this.initialLinksAndResourcesOccupation.clear();
		this.initialLinksAndResourcesOccupation.addAll(origin.initialLinksAndResourcesOccupation);

		this.seqLinksSegmentsAndResourcesTraversed.clear();
		for (NetworkElement e : origin.seqLinksSegmentsAndResourcesTraversed) this.seqLinksSegmentsAndResourcesTraversed.add(getInThisNetPlan (e));
		
		this.potentialBackupSegments.clear();
		for (NetworkElement e : origin.potentialBackupSegments) this.potentialBackupSegments.add((ProtectionSegment) getInThisNetPlan (e));

		this.cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap.clear();
		for (Entry<NetworkElement,Double> e : origin.cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap.entrySet()) this.cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap.put(getInThisNetPlan (e.getKey()) , e.getValue());

		this.cache_seqLinksAndProtectionSegments.clear();
		for (NetworkElement e : origin.cache_seqLinksAndProtectionSegments) this.cache_seqLinksAndProtectionSegments.add((Link) getInThisNetPlan (e));

		this.cache_seqLinksRealPath.clear();
		for (NetworkElement e : origin.cache_seqLinksRealPath) this.cache_seqLinksRealPath.add((Link) getInThisNetPlan (e));

		this.cache_seqNodesRealPath.clear();
		for (NetworkElement e : origin.cache_seqNodesRealPath) this.cache_seqNodesRealPath.add((Node) getInThisNetPlan (e));
	}


	/** Return the sequence of links of the route when it was created (before any rerouting operation could be made). It can be used to revert the route 
	 * to its initial sequence of links.
	 * @return The initial sequence of links as an unmodifiable list
	 */
	public List<Link> getInitialSequenceOfLinks () { return Route.listLinksRealPath(this.initialSeqLinksAndResourcesTraversedWhenCreated); }
	
	/** Return a list of elements Link and Pair<Resource,Double>, which corresponds to the sequence of link and resources traversed 
	 * initially, when the route was created. The Pair structure contains the resource traversed, and the occupied capacity in this traversal
	 * @return The sequence info
	 */
	public List<NetworkElement> getInitialSeqLinksAndResourcesTraversed () { return Collections.unmodifiableList(this.initialSeqLinksAndResourcesTraversedWhenCreated);}

	/** Return a list of elements Link and Pair<Resource,Double>, which corresponds to the current sequence of links (not protection segments, but the real path) and resources traversed 
	 * initially. The Pair structure contains the resource traversed, and the occupied capacity in this traversal
	 * @return The sequence info
	 */
	public List<NetworkElement> getCurrentSeqLinksAndResourcesTraversed () { return Collections.unmodifiableList(this.seqLinksSegmentsAndResourcesTraversed);}
	
	/** Return the set of resources this route is traversing 
	 * @return the info
	 */
	public List<Resource>  getCurrentResourcesTraversed () { return seqLinksSegmentsAndResourcesTraversed.stream().filter(e -> e instanceof Resource).map(e -> (Resource) e).collect(Collectors.toCollection(LinkedList::new)); }
	
	/** Return the set of resources this route traversed when it was first created (note that come of these resources may have been removed)
	 * @return the info
	 */
	public List<Resource>  getInitialResourcesTraversed () { return initialSeqLinksAndResourcesTraversedWhenCreated.stream().filter(e -> e instanceof Resource).map(e -> (Resource) e).collect(Collectors.toCollection(LinkedList::new)); }

	/** Return the total amount of capacity occupied by this route in the given resource (0 if not traversed, or route is down). Total means that 
	 * if a route passes the resource more than once, the occupations are summed up
	 * @param resource the resource
	 * @return the total occupation
	 */
	public double getResourceCurrentTotalOccupation (Resource resource) 
	{
		Double res = cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap.get(resource);
		if ((res == null) || isDown()) return 0; else return res;
	}

	/** Return the total amount of capacity occupied by this route in the given link (0 if not traversed, or route is down). Total means that 
	 * if a route passes the link more than once, the occupations are summed up
	 * @param link the link
	 * @return the total occupation
	 */
	public double getLinkCurrentTotalOccupation (Link link) 
	{
		Double res = cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap.get(link);
		if ((res == null) || isDown()) return 0; else return res;
	}

	/**
	 * <p>Adds a protection segment to the list of backup protection segments (both must belong to the same layer).</p>
	 * @param segment The protection segment (must be in the same layer as this route)
	 */
	public void addProtectionSegment(ProtectionSegment segment)
	{
		this.checkAttachedToNetPlanObject();
		segment.checkAttachedToNetPlanObject(this.netPlan);
		if (!segment.layer.equals(this.layer)) throw new Net2PlanException("The protection and the route must be of the same network layer");
		netPlan.checkIsModifiable();
		this.potentialBackupSegments.add (segment);
		segment.associatedRoutesToWhichIAmPotentialBackup.add (this);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}



	/** Returns the route carried traffic at this moment. Recall that if the route is down (traverses a link or node that is down) its carried traffic is 
	 * automatically set to zero. To retrieve the route carried traffic if all the links and nodes where up, use getCarriedTrafficInNoFailureState

	 * @return the current carried traffic
	 */
	public double getCarriedTraffic()
	{
		return isDown ()? 0.0 : carriedTrafficIfNotFailing;
	}

	/** Returns the route occupied capacity at this moment, at the particular link or resource. If no link or resource is provided, 
	 * the occupied capacity in the first traversed link, the first time it is traversed (recall that a route can have loops) is assumed. 
	 * If the route is down (traverses a link or node that is down) its occupied capacity in links and resources is  automatically set to zero. 
	 * To retrieve the route occupied link or resource capacity if all the links and nodes where up, 
	 * use getOccupiedCapacityInNoFailureState. If a link or resource is provided, the occupied capacity is teh sum of the occupations of all the times 
	 * that the route traverses the link/resource
	 * @param e one link or resource where to see the occupation (if not part of the route, zero is returned)
	 * @return the current occupied capacity 
	 */
	public double getOccupiedCapacity (NetworkElement ... e)
	{
		if (isDown ()) return 0.0;
		final NetworkElement linkResource = e.length == 0? cache_seqLinksRealPath.get(0) : e [0];
		final Double res = cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap.get(linkResource);
		return res == null? 0.0 : res;
	}

	/**
	 * Returns the route amount of carried traffic, if the tree was not traversing any failing link or node. Recall that if the route traverses 
	 * faling link/nodes, its carried traffic is automatically set to zero.
	 * @return see description above
	 */
	public double getCarriedTrafficInNoFailureState ()
	{
		return carriedTrafficIfNotFailing;
	}

	/** The same as getOccupiedCapacity, but if the network had no failures
	 * @param e one link or resource where to see the occupation (if not part of the route, zero is returned)
	 * @return the occupied capacity in no failure state
	 */
	public double getOccupiedCapacityInNoFailureState (NetworkElement ... e)
	{
		final NetworkElement linkResource = e.length == 0? cache_seqLinksRealPath.get(0) : e [0];
		final Double res = cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap.get(linkResource);
		return res == null? 0.0 : res;
	}

	

	/** Returns a list with the currently traversed protection segments for this route, in the same order in which they are traversed. Then, if a route 
	 * traverses a segmetn more than once, it appears more than once in this list.
	 * @return the list of traversed segments (or an empty list if none)
	 */
	public List<ProtectionSegment> getCurrentlyTraversedProtectionSegments ()
	{
		List<ProtectionSegment> res = new LinkedList<ProtectionSegment> ();
		for (Link e : cache_seqLinksAndProtectionSegments) if (e instanceof ProtectionSegment) res.add ((ProtectionSegment) e);
		return res;
	}

	/** Reverts the route to its initial state: the initial set of links and resources traversed when it was created, 
	 * occupying the same amount of capacity in each as when it was created, and carrying the same traffic as when was created. 
	 * If the original path is not possible since some links ore resources where deleted, an exception is thrown. 
	 * Recall that if the new route traverses failing link or nodes, its carried traffic and occupied link capacities drop to zero, and if not 
	 * they will be the base values in the "no failure state". 
	 */
	public void revertToInitialSequenceOfLinks () 
	{ 
		checkAttachedToNetPlanObject(); 
		for (NetworkElement e : this.initialSeqLinksAndResourcesTraversedWhenCreated)
			if (e.netPlan == null) throw new Net2PlanException ("Cannot revert to the previous path since a link or resource in the initial path was removed");
		this.setCarriedTrafficAndPath (this.initialCarriedTrafficWhenCreated, this.initialSeqLinksAndResourcesTraversedWhenCreated , this.initialLinksAndResourcesOccupation);
	}

	/** Returns true if the initial sequence of links and resources traversed when the route was created is at this moment NOT traversing failing links or nodes, and thus is not 
	 * subject to any failure. It returns false otherwise
	 * @return see description above
	 */
	public boolean isUpTheInitialSequenceOfLinks () 
	{
		checkAttachedToNetPlanObject();
		if (!ingressNode.isUp) return false; 
		for (Link e : Route.listLinksRealPath(initialSeqLinksAndResourcesTraversedWhenCreated))
		{
			if (e.netPlan != netPlan) return false;
			if (!e.destinationNode.isUp) return false;
			if (!e.isUp) return false;
		}
		return true;
	}
	
	/** Returns the demand that the route is associated to
	 * @return the unicast demand
	 */
	public Demand getDemand()
	{
		return demand;
	}

	/** Returns the route egress node, which is the egress node if its associated demand
	 * @return the route egress node, which is the egress node if its associated demand
	 */
	public Node getEgressNode()
	{
		return egressNode;
	}

	/**
	 * Returns the first node in the route that is in up state after all failures in the route. In 
	 * case there is no failure, it will return the last node in the route. In 
	 * case the last node in the route is failed, returns null.
	 * @return see description above
	 */
	public Node getFirstAvailableNodeAfterFailures()
	{
		checkAttachedToNetPlanObject();
		
		Node firstAvailableNodeAfterFailures = null;
		ListIterator<Link> it = cache_seqLinksRealPath.listIterator(cache_seqLinksRealPath.size());
		while(it.hasPrevious())
		{
			Link link = it.previous();
			Node destinationNode = link.destinationNode;
			if (!destinationNode.isUp) break;
			firstAvailableNodeAfterFailures = destinationNode;
			if (!link.isUp)
				break;
			else if (!it.hasPrevious())
				if (link.originNode.isUp) firstAvailableNodeAfterFailures = link.originNode;
		}
		if (firstAvailableNodeAfterFailures == this.ingressNode) firstAvailableNodeAfterFailures = this.egressNode; 
		return firstAvailableNodeAfterFailures;
	}


	/**
	 * Returns the first node starting from the route ingress node that is in up state, and is immediatly before 
	 * a failed resource for this route. In case there is no failure, it will return the last node in the sequence. In 
	 * case the first node in the path is failed, it will return null.
	 * @return the node
	 */
	public Node getFirstAvailableNodeBeforeFailures()
	{
		Node firstAvailableNodeBeforeFailures = null;
		ListIterator<Link> it = cache_seqLinksRealPath.listIterator();
		while(it.hasNext())
		{
			Link link = it.next();
			Node originNode = link.originNode;
			if (!originNode.isUp) break;
			firstAvailableNodeBeforeFailures = originNode;
			if (!link.isUp)
				break;
			else if (!it.hasNext())
				if (link.destinationNode.isUp) firstAvailableNodeBeforeFailures = link.destinationNode;
		}
		return firstAvailableNodeBeforeFailures;
	}

	/** Returns the route ingress node, which is the ingress node if its associated demand
	 * @return the route ingress node, which is the ingress node if its associated demand
	 */
	public Node getIngressNode()
	{
		return ingressNode;
	}


	/** Returns the route layer
	 * @return the layer
	 * */
	public NetworkLayer getLayer()
	{
		return layer;
	}


	/** Returns the route length in km, summing the traversed link lengths, as many times as the link is traversed. 
	 * @return the length in km
	 */
	public double getLengthInKm ()
	{
		double accum = 0; for (Link e : cache_seqLinksRealPath) accum += e.lengthInKm;
		return accum;
	}


	/** Returns the route number of traversed links, counting also the links inside any traversed protection segment. Each link is counted as many 
	 * times as it is traversed
	 * @return the number of hops of the route
	 */
	public int getNumberOfHops()
	{
		return cache_seqLinksRealPath.size();
	}


	/** Returns the list of protection segments that are registered as potential segmet backups to this route
	 * @return the potential backup segments as an unmodifiable list
	 */
	public Set<ProtectionSegment> getPotentialBackupProtectionSegments ()
	{
		return Collections.unmodifiableSet(potentialBackupSegments);
	}


	/** Returns the route propagation delay in seconds, summing the traversed link propagation delays
	 * @return see description above
	 */
	public double getPropagationDelayInMiliseconds ()
	{
		double accum = 0; 
		for (NetworkElement e : seqLinksSegmentsAndResourcesTraversed)
		{
			if (e instanceof ProtectionSegment)
				accum += ((ProtectionSegment) e).getPropagationDelayInMs();
			else if (e instanceof Link)
				accum += ((Link) e).getPropagationDelayInMs();
			else if (e instanceof Resource)
				accum += ((Resource) e).processingTimeToTraversingTrafficInMs;
		}
		return accum;
	}

	/** Returns the route average propagation speed in km per second, as the ratio between the total route length and the total route delay 
	 * (not including the processing time in the traversed resources)
	 * @return see description above
	 * */
	public double getPropagationSpeedInKmPerSecond ()
	{
		double accumDelaySeconds = 0; double accumLengthKm = 0; for (Link e : cache_seqLinksRealPath) { accumDelaySeconds += e.lengthInKm / e.propagationSpeedInKmPerSecond; accumLengthKm += e.lengthInKm; }
		return accumLengthKm / accumDelaySeconds;
	}

	/** Returns the route current sequence of traversed links and protection segments (which are subclasses of Link). 
	 * @return the list of traversed links/segments as an unmodifiable list
	 * */
	public List<Link> getSeqLinksAndProtectionSegments()
	{
		return Collections.unmodifiableList(cache_seqLinksAndProtectionSegments);
	}

	/** Returns the route current sequence of traversed resources, in the order they are traversed (and thus, a resource will
	 * appear as many times as it is traversed.  
	 * @return the list 
	 * */
	public List<Resource> getSeqResourcesTraversed()
	{
		List<Resource> res = new LinkedList<Resource> ();
		for (NetworkElement e : seqLinksSegmentsAndResourcesTraversed) if (e instanceof Resource) res.add ((Resource) e );
		return res;
	}

	/** Returns the route initial sequence of traversed resources, in the order they were traversed (and thus, a resource will
	 * appear as many times as it is traversed.  Note that some resources may have been removed
	 * @return the list 
	 * */
	public List<Resource> getInitialSeqResourcesTraversed()
	{
		List<Resource> res = new LinkedList<Resource> ();
		for (NetworkElement e : initialSeqLinksAndResourcesTraversedWhenCreated) if (e instanceof Resource) res.add ((Resource) e );
		return res;
	}

	/** Returns the route sequence of traversed links. Traversed protection segments are converted into their sequence of links
	 * @return see description above
	 */
	public List<Link> getSeqLinksRealPath()
	{
		return Collections.unmodifiableList(cache_seqLinksRealPath);
	}

	/** Returns the route sequence of traversed links (so protection segments are expanded), including traversed resources. 
	 * @return see description above
	 */
	public List<NetworkElement> getSeqLinksRealPathAndResources()
	{
		return Route.listLinksRealPathAndResources (seqLinksSegmentsAndResourcesTraversed);
	}

	/** Returns the route sequence of traversed nodes (the sequence corresponds to the real, so traversed protection segments are converted into their links before computing the traversed nodes)
	 * @return see description above
	 * */
	public List<Node> getSeqNodesRealPath ()
	{
		return Collections.unmodifiableList(cache_seqNodesRealPath);
	}

	/** Returns the number of times that a particular link (not a protection segment) is traversed in its real path (that when the traversed protection segments are expended in its links)
	 * @param e the link to check
	 * @return the number of times it is traversed
	 */
	public int getNumberOfTimesLinkIsTraversed (Link e)
	{
		if (e instanceof ProtectionSegment) throw new Net2PlanException ("This method is just for links, not protection segments");
		Integer num = e.cache_traversingRoutes.get (this); return num == null? 0 : num;
	}

	/** Returns the number of times that a particular resource is traversed in its real path 
	 * @param r the resource to check
	 * @return the number of times it is traversed (0 if none)
	 */
	public int getNumberOfTimesResourceIsTraversed (Resource r)
	{
		int num = 0; for (NetworkElement e : seqLinksSegmentsAndResourcesTraversed) if (e.equals(r)) num ++; 
		return num;
	}

	/** Returns the SRGs the route is affected by (any traversed node or link is in the SRG)
	 * @return see description above
	 */
	public Set<SharedRiskGroup> getSRGs ()
	{
		Set<SharedRiskGroup> res = new HashSet<SharedRiskGroup> ();
		for (Link e : cache_seqLinksRealPath) res.addAll (e.cache_srgs);
		for (Node n : cache_seqNodesRealPath) res.addAll (n.cache_nodeSRGs);
		return res;
	}
	
	/** Returns true if the route has loops (traverses a node more than once), and false otherwise
	 * @return see description above
	 */
	public boolean hasLoops ()
	{
		Set<Node> nodes = new HashSet<Node> ();
		nodes.addAll (cache_seqNodesRealPath);
		return nodes.size () < cache_seqNodesRealPath.size();
	}
	
	/**
	 * Returns true if the route is traversing a link or node that is down. Returns false otherwise
	 * @return see description above
	 */
	public boolean isDown ()
	{
		return layer.cache_routesDown.contains(this);
	}
	
	/**
	 * <p>Removes this route.</p>
	 */
	public void remove ()
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		this.setCarriedTraffic(0, 0); // release all previous occupation
		
		for (Node node : cache_seqNodesRealPath) node.cache_nodeAssociatedRoutes.remove(this);
		for (Link linkOrSegment : cache_seqLinksAndProtectionSegments) 
		{ 
			linkOrSegment.cache_traversingRoutes.remove(this); 
			if (linkOrSegment instanceof ProtectionSegment)
				for (Link link : ((ProtectionSegment) linkOrSegment).seqLinks)
					link.cache_traversingRoutes.remove(this); 
		}
		demand.cache_routes.remove(this);
		for (ProtectionSegment segment : potentialBackupSegments) segment.associatedRoutesToWhichIAmPotentialBackup.remove(this);

		netPlan.cache_id2RouteMap.remove(id);
		layer.cache_routesDown.remove (this);
		NetPlan.removeNetworkElementAndShiftIndexes(layer.routes , index);
		
		/* remove the resources info */
		for (NetworkElement e : cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap.keySet())
			if (e instanceof Resource) ((Resource) e).removeTraversingRoute(this);
		
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		removeId();
	}

	/**
	 * <p>Removes a protection segment from the list of backup protection segments of a route. 
	 * If the segment is being used, an exception is thrown. If the segment was not in the list, nothing happens</p>
	 * @param segment the segment to remove
	 */
	public void removeProtectionSegmentFromBackupSegmentList(ProtectionSegment segment)
	{
		if (cache_seqLinksAndProtectionSegments.contains(segment)) throw new Net2PlanException ("The segment cannot be removed from the list while being used b ythis route");
		potentialBackupSegments.remove (segment);
		segment.associatedRoutesToWhichIAmPotentialBackup.remove (this);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	
	/** Sets the route carried traffic and the occupied capacity in the traversed links and resources (typically the same as the carried traffic),
	 * setting it to being the same in all the links and resuorces traversed. If the route is traversing a failing 
	 * link or node, its current carried traffic and occupied link capacity will be still zero, but if the route becomes up later, its carried traffic and 
	 * occupied link capacities will be the ones stated here. 
	 * @param newCarriedTraffic the new carried traffic 
	 * @param newOccupiedLinkAndResourcesCapacities the new occuppied link and resourcs capacity
	 */
	public void setCarriedTraffic(double newCarriedTraffic , double newOccupiedLinkAndResourcesCapacities)
	{
		setCarriedTraffic (newCarriedTraffic , Collections.nCopies(this.seqLinksSegmentsAndResourcesTraversed.size(), newOccupiedLinkAndResourcesCapacities));
	}


	/** Sets the route carried traffic and the occupied capacity in the traversed links and resources, so the capacity occupied can 
	 * be different (but always non-negative) ni each.
	 * If the route is traversing a failing link or node, its current carried traffic and occupied 
	 * capacity will be still zero, but if the route becomes up later, its carried traffic and 
	 * occupied link capacities will be the ones stated here. 
	 * @param newCarriedTraffic the new carried traffic 
	 * @param linkAndResourcesOccupationInformation the new occupied capacity in traversed link, segments, and resources. If null, the occupied capacity equals the carried traffic in the links, segments and resources 
	 */
	public void setCarriedTraffic (double newCarriedTraffic , List<Double> linkAndResourcesOccupationInformation)
	{
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		netPlan.checkIsModifiable();
		if (linkAndResourcesOccupationInformation == null)
			linkAndResourcesOccupationInformation = Collections.nCopies(this.seqLinksSegmentsAndResourcesTraversed.size(), newCarriedTraffic);
		if (linkAndResourcesOccupationInformation.size() != this.seqLinksSegmentsAndResourcesTraversed.size()) throw new Net2PlanException ("Wrong vector size"); 
		for (double val : linkAndResourcesOccupationInformation) if (val < 0) throw new Net2PlanException ("The occupation of a resource cannot be negative");

		/* Update the carried traffic of the route */
		//		System.out.println ("Route: " + this + ", setCarriedTraffic: newCarriedTraffic: " + newCarriedTraffic + ", newOccupiedLinkCapacity: " + newOccupiedLinkCapacity);
		newCarriedTraffic = NetPlan.adjustToTolerance(newCarriedTraffic);
		linkAndResourcesOccupationInformation = linkAndResourcesOccupationInformation.stream().map(e->NetPlan.adjustToTolerance(e)).collect(Collectors.toList());
		if (newCarriedTraffic < 0) throw new Net2PlanException ("Carried traffics must be non-negative");

		this.carriedTrafficIfNotFailing = newCarriedTraffic;
		this.occupiedLinksSegmentsAndResourcesCapacitiesIfNotFailing = new ArrayList<Double> (linkAndResourcesOccupationInformation);
		for (Link linkOrSegment : cache_seqLinksAndProtectionSegments) 
		{ 
			if (linkOrSegment instanceof Link) ((Link) linkOrSegment).updateLinkTrafficAndOccupation();
			if (linkOrSegment instanceof ProtectionSegment)
				for (Link link : ((ProtectionSegment) linkOrSegment).seqLinks)
					link.updateLinkTrafficAndOccupation();
		}
		demand.carriedTraffic = 0; for (Route r : demand.cache_routes) demand.carriedTraffic += r.getCarriedTraffic();
		if (demand.coupledUpperLayerLink != null) demand.coupledUpperLayerLink.capacity = demand.carriedTraffic;
		
		/* Now the update of the links and resources occupation */
		this.cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap = updateLinkSegmentResourceOccupationCache ();
		for (NetworkElement e : cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap.keySet())
			if (e instanceof Resource)
				((Resource) e).addTraversingRoute(this , cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap.get(e));
		
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}
	
	/** Sets the new sequence of links and/or protection segments traversed by the route. 
	 * The carried traffic If the new route traverses failing link or nodes, its current 
	 * carried traffic and occupied link capacities will be zero. If not, will be the base ones in the no failure state
	 * @param cache_seqLinksAndProtectionSegments the new sequence of links and protection segments
	 */
	public void setCarriedTrafficAndPath (double newCarriedTraffic , List<? extends NetworkElement> seqLinksSegmentsAndResources , List<Double> linkAndResourcesOccupationInformation)
	{
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		netPlan.checkIsModifiable();
		Pair<List<Link>,List<Resource>> res = netPlan.checkPathValidityForDemand (seqLinksSegmentsAndResources, demand);
		List<Link> newSeqLinksAndProtectionSegments = res.getFirst();
		List<Resource> newSeqTraversedResources = res.getSecond();
//		if (newResourceOccupation == null && demand.isServiceChainRequest()) throw new Net2PlanException ("This route is a service chain, for rerouting please use the method that sets the new sequence of links and resources travsersed, as well as the new resources occupation");
//		if (newResourceOccupation == null) newResourceOccupation = new HashMap<Resource,Double> ();
//		if (!newResourceOccupation.keySet().equals(new HashSet<Resource> (newSeqTraversedResources))) throw new Net2PlanException ("The resources occupation info is not valid");
//		for (Double val : newResourceOccupation.values()) if (val < 0) throw new Net2PlanException ("The occupation of a resource cannot be negative");
		
		/* Copy paste previous setSeqLinksAndProtectionSegments */
		for (Link lps : newSeqLinksAndProtectionSegments) 
			if (lps instanceof ProtectionSegment) if (!potentialBackupSegments.contains(lps)) throw new Net2PlanException ("The protection segment is not registered as a backup for the route"); 

		/* Remove the old route trace in the traversed nodes and links */
		this.setCarriedTraffic(0 , 0); // releases all links, segments and resources occupation
		for (Resource resource : this.getCurrentResourcesTraversed()) resource.removeTraversingRoute(this); // removes the current route
		for (Link lps : this.cache_seqLinksAndProtectionSegments) 
		{
			lps.cache_traversingRoutes.remove (this);
			if (lps instanceof ProtectionSegment)
				for (Link link : ((ProtectionSegment) lps).seqLinks)
					link.cache_traversingRoutes.remove(this); 
		}
		for (Node node : cache_seqNodesRealPath)
			node.cache_nodeAssociatedRoutes.remove (this);
		layer.cache_routesDown.remove(this);

		/* Update this route info */
		this.seqLinksSegmentsAndResourcesTraversed = new LinkedList<NetworkElement> (seqLinksSegmentsAndResources);
		this.cache_seqLinksAndProtectionSegments = new LinkedList<Link> (newSeqLinksAndProtectionSegments);
		this.cache_seqLinksRealPath = new LinkedList<Link> (); 
		for (Link lps : newSeqLinksAndProtectionSegments) 
			if (lps instanceof ProtectionSegment) 
				cache_seqLinksRealPath.addAll (((ProtectionSegment) lps).seqLinks);  
			else 
				cache_seqLinksRealPath.add (lps);
		boolean isRouteUp = demand.ingressNode.isUp;
		this.cache_seqNodesRealPath = new LinkedList<Node> (); cache_seqNodesRealPath.add (demand.getIngressNode()); 
		for (Link e : cache_seqLinksRealPath) 
		{
			cache_seqNodesRealPath.add (e.getDestinationNode());
			isRouteUp = (isRouteUp && e.isUp && e.destinationNode.isUp);
		}
		if (!isRouteUp) layer.cache_routesDown.add(this);
		
		/* Update traversed links and nodes caches  */
		for (Link lps : newSeqLinksAndProtectionSegments) 
		{
			Integer numPassingTimes = lps.cache_traversingRoutes.get (this); 
			if (numPassingTimes == null) numPassingTimes = 1; else numPassingTimes ++; lps.cache_traversingRoutes.put (this , numPassingTimes);
			if (lps instanceof ProtectionSegment)
				for (Link link : ((ProtectionSegment) lps).seqLinks)
				{
					numPassingTimes = link.cache_traversingRoutes.get (this); 
					if (numPassingTimes == null) numPassingTimes = 1; else numPassingTimes ++; link.cache_traversingRoutes.put (this , numPassingTimes);
				}
		}
		for (Node node : cache_seqNodesRealPath)
			node.cache_nodeAssociatedRoutes.add (this);

		setCarriedTraffic (newCarriedTraffic , linkAndResourcesOccupationInformation);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/** Sets the new sequence of links and/or protection segments traversed by the route. Since this method receives a list of 
	 * links (proper links or protection segments) as input, the new route does not traverse resources. Thus, this 
	 * method cannot be used in service chain. The route carried traffic is not changed. If the current occupied capacity in 
	 * the traversed links is constant (not different link to link), this same amount is the occupied capacity in the links 
	 * of the new path. If not, an exception is thrown (in this case, other method should be used). 
	 * As usual, if (while) the new route traverses failing link or nodes, its current 
	 * carried traffic and occupied link capacities will be zero. 
	 * @param seqLinksAndProtectionSegments the new sequence of links and protection segments
	 */
	public void setSeqLinksAndProtectionSegments(List<Link> seqLinksAndProtectionSegments)
	{
		if (demand.isServiceChainRequest()) throw new Net2PlanException ("This method is not valid for service chains");
		final double firstLinkOccup = this.occupiedLinksSegmentsAndResourcesCapacitiesIfNotFailing.get(0);
		for (double val : occupiedLinksSegmentsAndResourcesCapacitiesIfNotFailing) if (val != firstLinkOccup) throw new Net2PlanException ("This method can only be used when the occupation in all the lnks is the same");
		if (seqLinksAndProtectionSegments.equals(this.cache_seqLinksAndProtectionSegments)) return;
		setCarriedTrafficAndPath(this.carriedTrafficIfNotFailing, seqLinksAndProtectionSegments, Collections.nCopies(seqLinksAndProtectionSegments.size(), firstLinkOccup));
	}

	public String toString () { return "r" + index + " (id " + id + ")"; }

//	final Node ingressNode;
//	final Node egressNode;
//	final List<NetworkElement> initialSeqLinksAndResourcesTraversedWhenCreated; // each object is a Link, or a Resource 
//	final List<Double> initialLinksAndResourcesOccupation;  // for each resource, the total occupation in it (if more than one pass, the total effect
//	final double initialCarriedTrafficWhenCreated; // each object is a Link, or a Resource 
//	List<NetworkElement> seqLinksSegmentsAndResourcesTraversed; // each object is a Link, or a Resource
//	Set<ProtectionSegment> potentialBackupSegments;
//	double carriedTrafficIfNotFailing;
//	List<Double> occupiedLinksSegmentsAndResourcesCapacitiesIfNotFailing;
//	List<Link> cache_seqLinksAndProtectionSegments;
//	List<Link> cache_seqLinksRealPath;
//	List<Node> cache_seqNodesRealPath;
//	Map<NetworkElement,Double> cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap;  // for each resource, the total occupation in it (if more than one pass, the total effect
	void checkCachesConsistency ()
	{
		assertTrue (layer.routes.contains(this));
		assertTrue (demand.cache_routes.contains(this));
		assertNotNull (ingressNode.netPlan);
		assertNotNull (egressNode.netPlan);
		assertNotNull (layer.netPlan);
		assertNotNull (initialSeqLinksAndResourcesTraversedWhenCreated);
		assertNotNull (initialLinksAndResourcesOccupation);
		assertNotNull (seqLinksSegmentsAndResourcesTraversed);
		assertNotNull (cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap);
		assertNotNull (potentialBackupSegments);
		assertNotNull (cache_seqLinksAndProtectionSegments);
		assertNotNull (cache_seqLinksRealPath);
		assertNotNull (cache_seqNodesRealPath);

		netPlan.checkInThisNetPlanAndLayer(seqLinksSegmentsAndResourcesTraversed , layer);
		netPlan.checkInThisNetPlanAndLayer(cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap.keySet() , layer);
		netPlan.checkInThisNetPlanAndLayer(potentialBackupSegments , layer);
		netPlan.checkInThisNetPlanAndLayer(cache_seqLinksAndProtectionSegments , layer);
		netPlan.checkInThisNetPlanAndLayer(cache_seqLinksAndProtectionSegments , layer);
		netPlan.checkInThisNetPlanAndLayer(cache_seqLinksRealPath , layer);
		netPlan.checkInThisNetPlanAndLayer(cache_seqNodesRealPath , layer);
		//netPlan.checkInThisNetPlanAndLayer(initialSeqLinksAndResourcesTraversedWhenCreated , layer); // do not check, since initial route could have removed links now
		//netPlan.checkInThisNetPlanAndLayer(initialResourcesOccupationMap.keySet () , layer); // do not check, since initial route could have removed links now
		for (NetworkElement e : seqLinksSegmentsAndResourcesTraversed)
		{
			assertNotNull(e);
			if (e instanceof Link) { final Link ee = (Link) e; assertTrue (ee.cache_traversingRoutes.containsKey(this)); }
			if (e instanceof Resource) { final Resource ee = (Resource) e; assertTrue (ee.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.containsKey(this)); }
		}
		for (Entry<NetworkElement,Double> entry : cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap.entrySet())
		{
			assertNotNull(entry.getKey());
			assertNotNull(entry.getValue());
			if (entry.getKey() instanceof Resource)
			{
				assertNotNull(((Resource) entry.getKey()).cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute);
				assertEquals(((Resource) entry.getKey()).cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.get(this) , entry.getValue() , 0);
			}
			else if (entry.getKey() instanceof Link)
			{
				assertNotNull(((Link) entry.getKey()).cache_traversingRoutes.containsKey(this));
				assertEquals(((Link) entry.getKey()).cache_traversingRoutes.get(this) , entry.getValue() , 0);
			}
		}
		List<Resource> travResources = new LinkedList<Resource> ();
		for (NetworkElement el : seqLinksSegmentsAndResourcesTraversed) if (el instanceof Resource) travResources.add((Resource) el);
		for (Resource res : travResources)
		{
			assertEquals (netPlan.cache_id2ResourceMap.get(res.id) , res);
			assertEquals (netPlan.resources.get(res.index) , res);
			assertNotNull(res.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.get(this));
			assertNotNull(cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap.get(res));
			assertEquals (res.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.get(this) , cache_linkSegmentsAndResourcesTraversedAndOccupiedCapIfnotFailMap.get(res) , 0);
		}

		for (Link link : cache_seqLinksRealPath) assertTrue (link.cache_traversingRoutes.containsKey(this));
		for (Node node : cache_seqNodesRealPath) assertTrue (node.cache_nodeAssociatedRoutes.contains(this));
		for (ProtectionSegment segment : potentialBackupSegments) assertTrue (segment.associatedRoutesToWhichIAmPotentialBackup.contains(this));
		boolean shouldBeUp = true; for (Link e : cache_seqLinksRealPath) if (!e.isUp) { shouldBeUp = false; break; }
		if (shouldBeUp) for (Node n : cache_seqNodesRealPath) if (!n.isUp) { shouldBeUp = false; break; }
		if (!shouldBeUp != this.isDown())
		{
			System.out.println ("Route : " + this + ", should be up: " + shouldBeUp + ", isDown: " + isDown() + ", carried traffic: " + this.getCarriedTraffic() + ", carried all ok: " + carriedTrafficIfNotFailing);
			for (Link e : cache_seqLinksRealPath)
				System.out.println ("Link e: " + e + ", isUp " + e.isUp);
			for (Node n : cache_seqNodesRealPath)
				System.out.println ("Node n: " + n + ", isUp " + n.isUp);
			throw new RuntimeException("Bad.");
		}
		if (shouldBeUp)
		{
			assertEquals(getCarriedTraffic() , carriedTrafficIfNotFailing , 0.001);
		}
		else
		{
			assertEquals(getCarriedTraffic() , 0 , 0.001);
		}
	}

	private static List<Link> listLinksRealPath (List<? extends NetworkElement> listLinksResourcesAndSegments)
	{
		List<Link> links = new LinkedList<Link> ();
		for (NetworkElement e : listLinksResourcesAndSegments) 
			if (e instanceof ProtectionSegment) for (Link ee : ((ProtectionSegment) e).seqLinks) links.add(ee);
			else if (e instanceof Link) links.add((Link) e); 
		return links;
	}

	private static List<NetworkElement> listLinksRealPathAndResources (List<? extends NetworkElement> listLinksResourcesAndSegments)
	{
		List<NetworkElement> linksAndResources = new LinkedList<NetworkElement> ();
		for (NetworkElement e : listLinksResourcesAndSegments)
			if (e instanceof ProtectionSegment) for (Link ee : ((ProtectionSegment) e).seqLinks) linksAndResources.add(ee);
			else if ((e instanceof Link) || (e instanceof Resource)) linksAndResources.add(e); 
		return linksAndResources;
	}

	private static List<Link> listLinksAndSegmentsWithoutResources (List<? extends NetworkElement> listLinksResourcesAndSegments)
	{
		List<Link> links = new LinkedList<Link> ();
		for (NetworkElement e : listLinksResourcesAndSegments) if (e instanceof Link) links.add((Link) e); 
		return links;
	}

	private static List<Node> listTraversedNodes (List<Link> path)
	{
		List<Node> res = new LinkedList<Node> (); res.add (path.get(0).originNode); for (Link e : path) res.add (e.getDestinationNode());
		return res;
	}

	private NetworkElement getInThisNetPlan (NetworkElement e)
	{
		NetworkElement res = null;
		if (e instanceof Link) res = this.netPlan.getLinkFromId(e.id);
		else if (e instanceof Resource) res = this.netPlan.getResourceFromId(e.id);
		else if (e instanceof Node) res = this.netPlan.getNodeFromId(e.id);
		else if (e instanceof ProtectionSegment) res = this.netPlan.getProtectionSegmentFromId(e.id);
		else throw new RuntimeException ("Bad");
		if (res == null) throw new RuntimeException ("Element of id: " + e.id + ", of class: " + e.getClass().getName() + ", does not exsit in current NetPlan");
		return res;
	}
	
	private Map<NetworkElement,Double> updateLinkSegmentResourceOccupationCache ()
	{
		Map<NetworkElement,Double> res = new HashMap<NetworkElement,Double> ();
		for (int step = 0; step < seqLinksSegmentsAndResourcesTraversed.size() ; step ++)
		{
			final NetworkElement e = seqLinksSegmentsAndResourcesTraversed.get(step);
			final Double previousAccumOccupCap = res.get(e);
			final Double currentStepOccup = occupiedLinksSegmentsAndResourcesCapacitiesIfNotFailing.get(step);
			res.put(e , (previousAccumOccupCap == null? 0 : previousAccumOccupCap) + currentStepOccup);
		}
		return res;
	}
}
