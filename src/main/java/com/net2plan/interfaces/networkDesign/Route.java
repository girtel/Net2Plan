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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.junit.Assert.*;

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
	final Map<Resource,Double> initialResourcesOccupationMap;  // for each resource, the total occupation in it (if more than one pass, the total effect
	List<NetworkElement> seqLinksSegmentsAndResourcesTraversed; // each object is a Link, or a Resource
	Map<Resource,Double> resourcesOccupationMap;  // for each resource, the total occupation in it (if more than one pass, the total effect
	Set<ProtectionSegment> potentialBackupSegments;
	List<Link> cache_seqLinksAndProtectionSegments;
	List<Link> cache_seqLinksRealPath;
	List<Node> cache_seqNodesRealPath;
	double carriedTraffic , carriedTrafficIfNotFailing;
	double occupiedLinkCapacity , occupiedLinkCapacityIfNotFailing;

	Route (NetPlan netPlan , long id , int index , Demand demand , List<? extends NetworkElement> seqLinksRealPathAndResourcesTraversedWhenCreated , Map <Resource,Double> occupationInformationInTraversedResources , AttributeMap attributes)
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
		if (occupationInformationInTraversedResources == null && demand.isServiceChainRequest()) throw new Net2PlanException ("This route is a service chain, for rerouting please use the method that sets the new sequence of links and resources travsersed, as well as the new resources occupation");
		if (occupationInformationInTraversedResources == null) occupationInformationInTraversedResources = new HashMap<Resource,Double> ();
		if (!occupationInformationInTraversedResources.keySet().equals(new HashSet<Resource> (pairListLinkListResources.getSecond()))) throw new Net2PlanException ("The resources occupation info is not valid");
		for (Double val : occupationInformationInTraversedResources.values()) if (val < 0) throw new Net2PlanException ("The occupation of a resource cannot be negative");
		
		this.initialSeqLinksAndResourcesTraversedWhenCreated = new LinkedList<NetworkElement> (seqLinksRealPathAndResourcesTraversedWhenCreated);
		this.seqLinksSegmentsAndResourcesTraversed = new LinkedList<NetworkElement> (seqLinksRealPathAndResourcesTraversedWhenCreated);
		this.initialResourcesOccupationMap = new HashMap<Resource,Double> (occupationInformationInTraversedResources);
		this.resourcesOccupationMap = new HashMap<Resource,Double> (occupationInformationInTraversedResources);
		this.potentialBackupSegments = new HashSet <ProtectionSegment> ();

		this.cache_seqLinksRealPath = Route.listLinksRealPath(seqLinksRealPathAndResourcesTraversedWhenCreated); 
		this.layer = demand.getLayer ();
		this.demand = demand;
		this.ingressNode = demand.ingressNode;
		this.egressNode = demand.egressNode;
		this.cache_seqLinksAndProtectionSegments = Route.listLinksAndSegmentsWithoutResources(seqLinksSegmentsAndResourcesTraversed);
		this.cache_seqLinksRealPath = Route.listLinksRealPath(seqLinksSegmentsAndResourcesTraversed);
		this.cache_seqNodesRealPath = Route.listTraversedNodes(cache_seqLinksRealPath);
		this.carriedTrafficIfNotFailing = 0; 
		this.occupiedLinkCapacityIfNotFailing = 0; 
		this.carriedTraffic = 0;
		this.occupiedLinkCapacity = 0;
	}

	void copyFrom (Route origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.carriedTraffic = origin.carriedTraffic;
		this.occupiedLinkCapacity = origin.occupiedLinkCapacity;
		this.carriedTrafficIfNotFailing = origin.carriedTrafficIfNotFailing;
		this.occupiedLinkCapacityIfNotFailing = origin.occupiedLinkCapacityIfNotFailing;

		
		this.initialSeqLinksAndResourcesTraversedWhenCreated.clear();
		for (NetworkElement e : origin.initialSeqLinksAndResourcesTraversedWhenCreated) this.initialSeqLinksAndResourcesTraversedWhenCreated.add(getInThisNetPlan (e));

		this.initialResourcesOccupationMap.clear();
		for (Entry<Resource,Double> e : origin.initialResourcesOccupationMap.entrySet()) this.initialResourcesOccupationMap.put((Resource) getInThisNetPlan (e.getKey()) , e.getValue());

		this.seqLinksSegmentsAndResourcesTraversed.clear();
		for (NetworkElement e : origin.seqLinksSegmentsAndResourcesTraversed) this.seqLinksSegmentsAndResourcesTraversed.add(getInThisNetPlan (e));
		
		this.resourcesOccupationMap.clear();
		for (Entry<Resource,Double> e : origin.resourcesOccupationMap.entrySet()) this.resourcesOccupationMap.put((Resource) getInThisNetPlan (e.getKey()) , e.getValue());

		this.potentialBackupSegments.clear();
		for (NetworkElement e : origin.potentialBackupSegments) this.potentialBackupSegments.add((ProtectionSegment) getInThisNetPlan (e));

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
	public Set<Resource>  getCurrentResourcesTraversed () { return resourcesOccupationMap.keySet(); }
	
	/** Return the total amount of capacity occupied by this route in the given resource (0 if not traversed). Total means that 
	 * if a route passes the resource more than once, the occupations are summed up
	 * @param resource the resource
	 * @return the total occupation
	 */
	public double getResourceCurrentTotalOccupation (Resource resource) 
	{
		Double res = resourcesOccupationMap.get(resource);
		if (res == null) return 0; else return res;
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
		return carriedTraffic;
	}

	/** Returns the route occupied capacity at the traversing links at this moment. Recall that if the route is down (traverses a link or node that is down) its 
	 * occupied link capacity is  automatically set to zero. To retrieve the route occupied link capacity if all the links and nodes where up, 
	 * use getOccupiedLinkCapacityInNoFailureState. Recall that if a route traverses a link more than once, the total occupied capacity in that link is 
	 * the returned value multiplied by the number of times the route traverses the link
	 * @return the current occupied capacity in each traversing link
	 */
	public double getOccupiedCapacity ()
	{
		return occupiedLinkCapacity;
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

	/**
	 * Returns the route capacity occupied in the links, if the route was not traversing any failing link or node. Recall that if the tree traverses 
	 * faling link/nodes, its occupied link capacity is automatically set to zero.
	 * @return see description above
	 */
	public double getOccupiedCapacityInNoFailureState ()
	{
		return occupiedLinkCapacityIfNotFailing;
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

	/** Reverts the route to its initial state: the initial set of links and resources traversed when it was created, ocupying the same amount of capacity in each. 
	 * Equivalent to setSeqLinksSegmentsAndResourcesOccupation(getInitialSeqLinksAndResourcesTraversed ()). 
	 * That is, it reroutes this route to the original sequence of links when 
	 * it was created. Recall that if the new route traverses failing link or nodes, its carried traffic and occupied link capacities drop to zero, and if not 
	 * they will be the base values in the "no failure state". Also, if the original route now traverses links or resources that were removed, an exception is thrown
	 */
	public void revertToInitialSequenceOfLinks () { checkAttachedToNetPlanObject(); this.setSeqLinksSegmentsAndResourcesOccupation(this.initialSeqLinksAndResourcesTraversedWhenCreated , initialResourcesOccupationMap); }

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
		Integer num = e.cache_traversingRoutes.get (e); return num == null? 0 : num;
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
		checkAttachedToNetPlanObject();
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
		this.setCarriedTrafficAndResourcesOccupationInformation(0 , 0 , null); // release all previous occupation
		
		// PABLO: AQUI ACTUALIZAR EL RELEASE REALIZADO EN TODAS LAS CACHES.

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
		for (Resource resource : resourcesOccupationMap.keySet())
			resource.removeTraversingRoute(this);
		
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		removeId();
	}

	/**
	 * <p>Removes a protection segment from the list of backup protection segments of a route. If the segment was not in the list, nothing happens</p>
	 * @param segment the segment to remove
	 */
	public void removeProtectionSegmentFromBackupSegmentList(ProtectionSegment segment)
	{
		potentialBackupSegments.remove (segment);
		segment.associatedRoutesToWhichIAmPotentialBackup.remove (this);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	
	/** Sets the route carried traffic and occupied capacity in the traversed links (typically the same amount). If the route is traversing a failing 
	 * link or node, its current carried traffic and occupied link capacity will be still zero, but if the route becomes up later, its carried traffic and 
	 * occupied link capacities will be the ones stated here
	 * @param newCarriedTraffic the new carried traffic 
	 * @param newOccupiedLinkCapacity the new occuppied link capacity
	 */
	public void setCarriedTraffic(double newCarriedTraffic , double newOccupiedLinkCapacity)
	{
		if (demand.isServiceChainRequest ()) throw new Net2PlanException ("This method is not valid for routes that must define a service chain. "
				+ "Try the equivalent method for service chains, where both the set of links and "
				+ "set of resources and their occupations are settled");
		setCarriedTrafficAndResourcesOccupationInformation (newCarriedTraffic , newOccupiedLinkCapacity , null);
	}


	/** Sets the route carried traffic and occupied capacity in the traversed links (typically the same amount), as well as the occupation 
	 * in the traversed resources. If the route is traversing a failing link or node, its current carried traffic and occupied link 
	 * capacity will be still zero, but if the route becomes up later, its carried traffic and 
	 * occupied link capacities will be the ones stated here. Regarding the resource occupation map, if a traversed resource is not included, 
	 * its occupied capacity is set to zero  
	 * @param newCarriedTraffic the new carried traffic 
	 * @param newOccupiedLinkCapacity the new occuppied link capacity
	 * @param occupationInformationInTraversedResources if null, all resources are released 
	 */
	public void setCarriedTrafficAndResourcesOccupationInformation (double newCarriedTraffic , double newOccupiedLinkCapacity , Map <Resource,Double> occupationInformationInTraversedResources)
	{
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		netPlan.checkIsModifiable();
		if (occupationInformationInTraversedResources == null) occupationInformationInTraversedResources = new HashMap<Resource,Double> ();
		for (double val : occupationInformationInTraversedResources.values()) if (val < 0) throw new Net2PlanException ("The occupation of a resource cannot be negative");
		for (Entry<Resource,Double> entry : occupationInformationInTraversedResources.entrySet())
		{
			if (!this.resourcesOccupationMap.containsKey(entry.getKey())) throw new Net2PlanException ("Cannot change the occupation of a non-traversed resource");
			if (entry.getValue() < 0) throw new Net2PlanException ("Resource occupation cannot be negative");
		}

		/* Update the carried traffic of the route */
		//		System.out.println ("Route: " + this + ", setCarriedTraffic: newCarriedTraffic: " + newCarriedTraffic + ", newOccupiedLinkCapacity: " + newOccupiedLinkCapacity);
		newCarriedTraffic = NetPlan.adjustToTolerance(newCarriedTraffic);
		newOccupiedLinkCapacity = NetPlan.adjustToTolerance(newOccupiedLinkCapacity);
		if ((newCarriedTraffic < 0) || (newOccupiedLinkCapacity < 0)) throw new Net2PlanException ("Carried traffics and occupied link capacities must be non-negative");
		final double oldCarriedTraffic = this.carriedTraffic;
		final double oldOccupiedLinkCapacity = this.occupiedLinkCapacity;
		this.carriedTrafficIfNotFailing = newCarriedTraffic;
		this.occupiedLinkCapacityIfNotFailing = newOccupiedLinkCapacity;
		if (this.isDown()) { this.carriedTraffic = 0; this.occupiedLinkCapacity = 0;  } else { this.carriedTraffic = newCarriedTraffic; this.occupiedLinkCapacity = newOccupiedLinkCapacity; }

		for (Link linkOrSegment : cache_seqLinksAndProtectionSegments) 
		{ 
			linkOrSegment.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments += this.carriedTraffic - oldCarriedTraffic;
			linkOrSegment.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments += this.occupiedLinkCapacity - oldOccupiedLinkCapacity;
			if (linkOrSegment.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments < -1e-3) throw new RuntimeException ("Bad");
			if (linkOrSegment.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments < -1e-3) throw new RuntimeException ("Bad");
			if (linkOrSegment instanceof ProtectionSegment)
			{
				for (Link link : ((ProtectionSegment) linkOrSegment).seqLinks)
				{
					link.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments += (this.carriedTraffic - oldCarriedTraffic);
					link.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments += (this.occupiedLinkCapacity - oldOccupiedLinkCapacity);
					if (link.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments < -1e-3) throw new RuntimeException ("Bad");
					if (link.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments < -1e-3) throw new RuntimeException ("Bad");
				}
			}
		}
		demand.carriedTraffic += this.carriedTraffic - oldCarriedTraffic;
		if (demand.coupledUpperLayerLink != null) demand.coupledUpperLayerLink.capacity = demand.carriedTraffic;
		
		/* Now the update of the resources */
		for (Resource res : new HashSet<Resource> (this.resourcesOccupationMap.keySet()))
		{
			Double newOccupation = occupationInformationInTraversedResources.get(res);
			this.resourcesOccupationMap.put(res , newOccupation == null? 0.0 : newOccupation);
			res.addTraversingRoute(this , newOccupiedLinkCapacity);
		}

		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}
	


//	List<NetworkElement> seqLinksSegmentsAndResourcesTraversed; // each object is a Link, or a Resource
//	Map<Resource,Double> resourcesOccupationMap;  // for each resource, the total occupation in it (if more than one pass, the total effect
//	List<Link> cache_seqLinksAndProtectionSegments;
//	List<Link> cache_seqLinksRealPath;
//	List<Node> cache_seqNodesRealPath;

	/** Sets the new sequence of links and/or protection segments traversed by the route. If the route is a service chain,
	 * an error is returned (other method should be used). If the new route traverses failing link or nodes, its current 
	 * carried traffic and occupied link capacities will be zero. If not, will be the base ones in the no failure state
	 * @param cache_seqLinksAndProtectionSegments the new sequence of links and protection segments
	 */
	public void setSeqLinksSegmentsAndResourcesOccupation (List<? extends NetworkElement> seqLinksSegmentsAndResources , Map<Resource,Double> newResourceOccupation)
	{
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		netPlan.checkIsModifiable();
		Pair<List<Link>,List<Resource>> res = netPlan.checkPathValidityForDemand (seqLinksSegmentsAndResources, demand);
		List<Link> newSeqLinksAndProtectionSegments = res.getFirst();
		List<Resource> newSeqTraversedResources = res.getSecond();
		if (newResourceOccupation == null && demand.isServiceChainRequest()) throw new Net2PlanException ("This route is a service chain, for rerouting please use the method that sets the new sequence of links and resources travsersed, as well as the new resources occupation");
		if (newResourceOccupation == null) newResourceOccupation = new HashMap<Resource,Double> ();
		if (!newResourceOccupation.keySet().equals(new HashSet<Resource> (newSeqTraversedResources))) throw new Net2PlanException ("The resources occupation info is not valid");
		for (Double val : newResourceOccupation.values()) if (val < 0) throw new Net2PlanException ("The occupation of a resource cannot be negative");
		
		/* Copy paste previous setSeqLinksAndProtectionSegments */
		for (Link lps : newSeqLinksAndProtectionSegments) 
			if (lps instanceof ProtectionSegment) if (!potentialBackupSegments.contains(lps)) throw new Net2PlanException ("The protection segment is not registered as a backup for the route"); 

		/* Remove the old route trace in the traversed nodes and links */
		final double currentCarriedTrafficIfNotFail = this.carriedTrafficIfNotFailing;
		final double currentOccupiedCapacityIfNotFail = this.occupiedLinkCapacityIfNotFailing;
		this.setCarriedTrafficAndResourcesOccupationInformation(0 , 0 , null); // releases all resources
		for (Resource resource : resourcesOccupationMap.keySet()) resource.removeTraversingRoute(this); // removes the current route
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

		/* Update the resources occupation map, add the resources as traversed ones, with zero occupation */
		resourcesOccupationMap.clear();
		for (Entry<Resource,Double> entry : newResourceOccupation.entrySet())
			resourcesOccupationMap.put(entry.getKey() , 0.0);
		
		setCarriedTrafficAndResourcesOccupationInformation(currentCarriedTrafficIfNotFail , currentOccupiedCapacityIfNotFail , newResourceOccupation);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();

		
		
	}

	/** Sets the new sequence of links and/or protection segments traversed by the route. If the route is a service chain,
	 * an error is returned (other method should be used). If the new route traverses failing link or nodes, its current 
	 * carried traffic and occupied link capacities will be zero. If not, will be the base ones in the no failure state
	 * @param seqLinksAndProtectionSegments the new sequence of links and protection segments
	 */
	public void setSeqLinksAndProtectionSegments(List<Link> seqLinksAndProtectionSegments)
	{
		if (seqLinksAndProtectionSegments.equals(this.cache_seqLinksAndProtectionSegments)) return;
		setSeqLinksSegmentsAndResourcesOccupation (seqLinksAndProtectionSegments , null);
	}


	
	public String toString () { return "r" + index + " (id " + id + ")"; }

//	final List<NetworkElement> initialSeqLinksAndResourcesTraversedWhenCreated; // each object is a Link, or a Resource 
//	final Map<Resource,Double> initialResourcesOccupationMap;  // for each resource, the total occupation in it (if more than one pass, the total effect
//	List<NetworkElement> seqLinksSegmentsAndResourcesTraversed; // each object is a Link, or a Resource
//	Map<Resource,Double> resourcesOccupationMap;  // for each resource, the total occupation in it (if more than one pass, the total effect
//	Set<ProtectionSegment> potentialBackupSegments;
//	List<Link> cache_seqLinksAndProtectionSegments;
//	List<Link> cache_seqLinksRealPath;
//	List<Node> cache_seqNodesRealPath;
//	double carriedTraffic , carriedTrafficIfNotFailing;
//	double occupiedLinkCapacity , occupiedLinkCapacityIfNotFailing;
	void checkCachesConsistency ()
	{
		if (!layer.routes.contains(this)) throw new RuntimeException ("Bad");
		if (!demand.cache_routes.contains(this)) throw new RuntimeException ("Bad");
		if (initialSeqLinksAndResourcesTraversedWhenCreated == null) throw new RuntimeException ("Route " + this + ", initialSeqLinksAndResourcesTraversedWhenCreated == null");
		if (initialResourcesOccupationMap == null) throw new RuntimeException ("Route " + this + ", initialResourcesOccupationMap == null");
		if (seqLinksSegmentsAndResourcesTraversed == null) throw new RuntimeException ("Route " + this + ", seqLinksSegmentsAndResourcesTraversed == null");
		if (resourcesOccupationMap == null) throw new RuntimeException ("Route " + this + ", resourcesOccupationMap == null");
		if (potentialBackupSegments == null) throw new RuntimeException ("Route " + this + ", potentialBackupSegments == null");
		if (cache_seqLinksAndProtectionSegments == null) throw new RuntimeException ("Route " + this + ", cache_seqLinksAndProtectionSegments == null");
		if (cache_seqLinksRealPath == null) throw new RuntimeException ("Route " + this + ", cache_seqLinksRealPath == null");
		if (cache_seqNodesRealPath == null) throw new RuntimeException ("Route " + this + ", cache_seqNodesRealPath == null");

		netPlan.checkInThisNetPlanAndLayer(seqLinksSegmentsAndResourcesTraversed , layer);
		netPlan.checkInThisNetPlanAndLayer(resourcesOccupationMap.keySet() , layer);
		netPlan.checkInThisNetPlanAndLayer(potentialBackupSegments , layer);
		netPlan.checkInThisNetPlanAndLayer(cache_seqLinksAndProtectionSegments , layer);
		netPlan.checkInThisNetPlanAndLayer(cache_seqLinksRealPath , layer);
		netPlan.checkInThisNetPlanAndLayer(cache_seqNodesRealPath , layer);
		//netPlan.checkInThisNetPlanAndLayer(initialSeqLinksAndResourcesTraversedWhenCreated , layer); // do not check, since initial route could have removed links now
		//netPlan.checkInThisNetPlanAndLayer(initialResourcesOccupationMap.keySet () , layer); // do not check, since initial route could have removed links now
		for (NetworkElement e : seqLinksSegmentsAndResourcesTraversed)
		{
			assertNotNull(e);
			if (e instanceof Link) { final Link ee = (Link) e; assertTrue (ee.cache_traversingRoutes.containsKey(this)); }
			if (e instanceof Resource) { final Resource ee = (Resource) e; assertTrue (ee.cache_traversingRoutesAndOccupiedCapacities.containsKey(this)); }
		}
		for (Entry<Resource,Double> entry : resourcesOccupationMap.entrySet())
		{
			assertNotNull(entry.getKey());
			assertNotNull(entry.getValue());
			assertTrue(entry.getKey().cache_traversingRoutesAndOccupiedCapacities.get(this) == entry.getValue());
		}
		List<Resource> travResources = new LinkedList<Resource> ();
		for (NetworkElement el : seqLinksSegmentsAndResourcesTraversed) if (el instanceof Resource) travResources.add((Resource) el);
		for (Resource res : travResources)
		{
			if (netPlan.cache_id2ResourceMap.get(res.id) != res) throw new RuntimeException ("Bad");
			if (!netPlan.resources.get(res.index).equals(res)) throw new RuntimeException ("Bad");
			if (!res.cache_traversingRoutesAndOccupiedCapacities.containsKey(this)) throw new RuntimeException ("Bad");
			if (res.cache_traversingRoutesAndOccupiedCapacities.get(this) != resourcesOccupationMap.get(res)) throw new RuntimeException ("Bad");
		}

		for (Link link : cache_seqLinksRealPath) if (!link.cache_traversingRoutes.containsKey(this)) throw new RuntimeException ("Bad");
		for (Node node : cache_seqNodesRealPath) if (!node.cache_nodeAssociatedRoutes.contains(this)) throw new RuntimeException ("Bad");
		for (ProtectionSegment segment : potentialBackupSegments) if (!segment.associatedRoutesToWhichIAmPotentialBackup.contains(this)) throw new RuntimeException ("Bad");
		boolean shouldBeUp = true; for (Link e : cache_seqLinksRealPath) if (!e.isUp) { shouldBeUp = false; break; }
		if (shouldBeUp) for (Node n : cache_seqNodesRealPath) if (!n.isUp) { shouldBeUp = false; break; }
		if (!shouldBeUp != this.isDown())
		{
			System.out.println ("Route : " + this + ", should be up: " + shouldBeUp + ", isDown: " + isDown() + ", carried traffic: " + carriedTraffic + ", carried all ok: " + carriedTrafficIfNotFailing);
			for (Link e : cache_seqLinksRealPath)
				System.out.println ("Link e: " + e + ", isUp " + e.isUp);
			for (Node n : cache_seqNodesRealPath)
				System.out.println ("Node n: " + n + ", isUp " + n.isUp);
			throw new RuntimeException("Bad.");
		}
		if (shouldBeUp)
		{
			if (carriedTraffic != carriedTrafficIfNotFailing) throw new RuntimeException ("Bad");
		}
		else
		{
			if (carriedTraffic != 0) throw new RuntimeException ("Bad");
		}
	}

	private static List<Link> listLinksRealPath (List<? extends NetworkElement> listLinksResourcesAndSegments)
	{
		List<Link> links = new LinkedList<Link> ();
		for (NetworkElement e : listLinksResourcesAndSegments) if (e instanceof Link) links.add((Link) e); else if (e instanceof ProtectionSegment) for (Link ee : ((ProtectionSegment) e).seqLinks) links.add(ee);
		return links;
	}

	private static List<NetworkElement> listLinksRealPathAndResources (List<? extends NetworkElement> listLinksResourcesAndSegments)
	{
		List<NetworkElement> linksAndResources = new LinkedList<NetworkElement> ();
		for (NetworkElement e : listLinksResourcesAndSegments) if (e instanceof Link || e instanceof Resource) linksAndResources.add(e); else if (e instanceof ProtectionSegment) for (Link ee : ((ProtectionSegment) e).seqLinks) linksAndResources.add(ee);
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
}
