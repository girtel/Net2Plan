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
	List<NetworkElement> currentPath; // each object is a Link, or a Resource
	double currentCarriedTrafficIfNotFailing;
	List<Double> currentLinksAndResourcesOccupationIfNotFailing;
	List<NetworkElement> primaryPath; // could traverse removed links/resources
	List<List<NetworkElement>> backupPaths; // could traverse removed links/resources
	List<Link> cache_seqLinksRealPath;
	List<Node> cache_seqNodesRealPath;
	Map<NetworkElement,Double> cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap;  

	Route (NetPlan netPlan , long id , int index , Demand demand , List<? extends NetworkElement> seqLinksAndResourcesTraversed , AttributeMap attributes)
	{
		super (netPlan , id , index , attributes);

		if (!netPlan.equals(demand.netPlan)) throw new RuntimeException ("Bad");
		for (NetworkElement e : seqLinksAndResourcesTraversed) 
		{ 
			if (e instanceof Link) if (!netPlan.equals(e.netPlan)) throw new RuntimeException ("Bad"); 
			else if (e instanceof Resource) if (!netPlan.equals(e.netPlan)) throw new RuntimeException ("Bad");
			else throw new RuntimeException ("Bad");
		}
		netPlan.checkPathValidityForDemand (seqLinksAndResourcesTraversed, demand);
		
		this.layer = demand.getLayer ();
		this.demand = demand;
		this.ingressNode = demand.ingressNode;
		this.egressNode = demand.egressNode;
		this.primaryPath = new ArrayList<NetworkElement> (seqLinksAndResourcesTraversed);
		this.currentPath = new LinkedList<NetworkElement> (seqLinksAndResourcesTraversed);
		this.primaryPath = new LinkedList<NetworkElement> ();
		this.backupPaths = new ArrayList<List<NetworkElement>> ();
		this.currentCarriedTrafficIfNotFailing = 0; 
		this.currentLinksAndResourcesOccupationIfNotFailing = Collections.nCopies(seqLinksAndResourcesTraversed.size() , 0.0);  
		this.cache_seqLinksRealPath = Route.getSeqLinks(seqLinksAndResourcesTraversed); 
		this.cache_seqNodesRealPath = Route.listTraversedNodes(cache_seqLinksRealPath);
		this.cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap = updateLinkResourceOccupationCache ();
	}

	boolean isDeepCopy (Route e2)
	{
		if (!super.isDeepCopy(e2)) return false;
		if (this.layer.id != e2.layer.id) return false;
		if (this.demand.id != e2.demand.id) return false;
		if (this.ingressNode.id != e2.ingressNode.id) return false;
		if (this.egressNode.id != e2.egressNode.id) return false;
		if (!NetPlan.isDeepCopy(this.currentPath , e2.currentPath)) return false;
		if (this.currentCarriedTrafficIfNotFailing != e2.currentCarriedTrafficIfNotFailing) return false;
		if (!this.currentLinksAndResourcesOccupationIfNotFailing.equals(e2.currentLinksAndResourcesOccupationIfNotFailing)) return false;
		if (!NetPlan.isDeepCopy(this.primaryPath , e2.primaryPath)) return false;
		if (backupPaths.size() != e2.backupPaths.size()) return false;
		for (int cont = 0 ; cont < backupPaths.size() ; cont ++)
			if (!NetPlan.isDeepCopy(this.backupPaths.get(cont) , e2.backupPaths.get(cont))) return false;
		if (!NetPlan.isDeepCopy(this.cache_seqLinksRealPath , e2.cache_seqLinksRealPath)) return false;
		if (!NetPlan.isDeepCopy(this.cache_seqNodesRealPath , e2.cache_seqNodesRealPath)) return false;
		if (!NetPlan.isDeepCopy(this.cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap , e2.cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap)) return false;
		return true;
	}

	void copyFrom (Route origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.currentCarriedTrafficIfNotFailing = origin.currentCarriedTrafficIfNotFailing;
		this.currentLinksAndResourcesOccupationIfNotFailing = new ArrayList<Double> (origin.currentLinksAndResourcesOccupationIfNotFailing);

		this.primaryPath = (List<NetworkElement>) getInThisNetPlan(origin.primaryPath);
		this.currentPath = (List<NetworkElement>) getInThisNetPlan(origin.currentPath);
		
		this.backupPaths.clear();
		for (List<NetworkElement> originPath : origin.backupPaths)
			this.backupPaths.add((List<NetworkElement>) getInThisNetPlan(originPath));
		
		this.cache_seqLinksRealPath = (List<Link>) getInThisNetPlan(origin.cache_seqLinksRealPath);
		this.cache_seqNodesRealPath = (List<Node>) getInThisNetPlan(origin.cache_seqNodesRealPath);

		this.cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.clear();
		for (Entry<NetworkElement,Double> e : origin.cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.entrySet()) this.cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.put(getInThisNetPlan (e.getKey()) , e.getValue());
	}


	/** Return the path (sequence of links and resources) defined as the primary path. Note that some links/resources could have been removed, if this is not the current path 
	 * @return The initial sequence of links as an unmodifiable list
	 */
	public List<NetworkElement> getPrimaryPath () { return Collections.unmodifiableList(this.primaryPath); }
	
	/** Return the current path (sequence of links and resources) of the route.  
	 * @return The info
	 */
	public List<NetworkElement> getCurrentPath () { return Collections.unmodifiableList(this.currentPath);}
	
	/** Returns the list of resources this route is traversing (in the traversal order), getting the current path and filtering out the links 
	 * @return the info
	 */
	public List<Resource>  getCurrentSeqResources () { return Route.getSeqResources(this.currentPath);  }
	
	/** Returns the list of links this route is traversing (in the traversal order), getting the current path and filtering out the resources
	 * @return the info
	 */
	public List<Link>  getCurrentSeqLinks () { return Route.getSeqLinks (this.currentPath);  }

	/**
	 * <p>Adds a path to the list of potential backup path of this demand. By default, the path is added at the end of the list. 
	 * @param path the sequence of links and resources to traverse (must be a valid path for the demand, or an exception is thrown)
	 */
	public void addBackupPath (List<NetworkElement> path)
	{
		this.checkAttachedToNetPlanObject();
		netPlan.checkPathValidityForDemand (path, demand);
		netPlan.checkIsModifiable();
		this.backupPaths.add (path);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Adds a protection segment to the list of backup protection segments (both must belong to the same layer).</p>
	 * @param segment The protection segment (must be in the same layer as this route)
	 */
	public void removeBackupPath (int index)
	{
		if ((index < 0) || (index >= this.backupPaths.size())) throw new Net2PlanException ("Wrong position in the backup list");
		this.checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		this.backupPaths.remove (index);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * Returns an unmodifiable list of the backup paths defined (empty list if none was defined)
	 * @return the list
	 */
	public List<List<NetworkElement>> getBackupPathList ()
	{
		List<List<NetworkElement>> res = new ArrayList<List<NetworkElement>> ();
		for (List<NetworkElement> path : this.backupPaths)
			res.add(Collections.unmodifiableList(path));
		return Collections.unmodifiableList(res);
	}
	
	/** Returns the route carried traffic at this moment. Recall that if the route is down (traverses a link or node that is down) its carried traffic is 
	 * automatically set to zero. To retrieve the route carried traffic if all the links and nodes where up, use getCarriedTrafficInNoFailureState
	 * @return the current carried traffic
	 */
	public double getCarriedTraffic()
	{
		return isDown ()? 0.0 : currentCarriedTrafficIfNotFailing;
	}

	/** Returns the route occupied capacity at this moment, at the particular link or resource. If no link or resource is provided, 
	 * the occupied capacity in the first traversed link or resource is assumed. Recall that a route can have loops, and traverse a link/resource more than once, 
	 * so the returned occupation is the sum of the occupations in each pass. 
	 * If the route is down (traverses a link or node that is down) its occupied capacity in links and resources is  automatically set to zero. 
	 * To retrieve the route occupied link or resource capacity if all the links and nodes where up, 
	 * use getOccupiedCapacityInNoFailureState. 
	 * @param e one link or resource where to see the occupation (if not part of the route, zero is returned)
	 * @return the current occupied capacity 
	 */
	public double getOccupiedCapacity (NetworkElement ... e)
	{
		if (isDown ()) return 0.0;
		return getOccupiedCapacityInNoFailureState (e);
	}

	/**
	 * Returns the route amount of carried traffic, if the route was not traversing any failing link or node. Recall that if the route traverses 
	 * faling link/nodes, its carried traffic is automatically set to zero.
	 * @return see description above
	 */
	public double getCarriedTrafficInNoFailureState ()
	{
		return currentCarriedTrafficIfNotFailing;
	}

	/** The same as getOccupiedCapacity, but if the network had no failures
	 * @param e one link or resource where to see the occupation (if not part of the route, zero is returned)
	 * @return the occupied capacity in no failure state
	 */
	public double getOccupiedCapacityInNoFailureState (NetworkElement ... e)
	{
		final NetworkElement linkResource = e.length == 0? currentPath.get(0) : e [0];
		final Double res = cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.get(linkResource);
		return res == null? 0.0 : res;
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


	/** Returns the route number of traversed links. Each link is counted as many 
	 * times as it is traversed
	 * @return the number of hops of the route
	 */
	public int getNumberOfHops()
	{
		return cache_seqLinksRealPath.size();
	}


	/** Returns the route propagation delay in seconds, summing the traversed link propagation delays
	 * @return see description above
	 */
	public double getPropagationDelayInMiliseconds ()
	{
		double accum = 0; 
		for (NetworkElement e : currentPath)
		{
			if (e instanceof Link)
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

	/** Returns a list of double, with one element per link or resource traversed, 
	 * in the order in which they are traversed, meaning the occupation in such traversal.
	 * @return the list of capacity occupations
	 * */
	public List<Double> getSeqOccupiedCapacitiesIfNotFailing()
	{
		return Collections.unmodifiableList(currentLinksAndResourcesOccupationIfNotFailing);
	}

	/** Returns the route current sequence of traversed resources, in the order they are traversed (and thus, a resource will
	 * appear as many times as it is traversed.  
	 * @return the list 
	 * */
	public List<Resource> getSeqResourcesTraversed()
	{
		return Route.getSeqResources(currentPath);
	}

	/** Returns the route current sequence of traversed links (without any resource traversed). 
	 * @return see description above
	 */
	public List<Link> getSeqLinks()
	{
		return Collections.unmodifiableList(cache_seqLinksRealPath);
	}

	/** Returns the route sequence of traversed nodes (when a resource is traversed, the resource node is not added again as a traversal) 
	 * @return see description above
	 * */
	public List<Node> getSeqNodes ()
	{
		return Collections.unmodifiableList(cache_seqNodesRealPath);
	}

	/** Returns the number of times that a particular link is traversed 
	 * @param e the link to check
	 * @return the number of times it is traversed
	 */
	public int getNumberOfTimesLinkIsTraversed (Link e)
	{
		Integer num = e.cache_traversingRoutes.get (this); return num == null? 0 : num;
	}

	/** Returns the number of times that a particular resource is traversed in its real path 
	 * @param r the resource to check
	 * @return the number of times it is traversed (0 if none)
	 */
	public int getNumberOfTimesResourceIsTraversed (Resource r)
	{
		int num = 0; for (NetworkElement e : currentPath) if (e.equals(r)) num ++; 
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
		for (Link link : cache_seqLinksRealPath) 
			link.cache_traversingRoutes.remove(this); 
		demand.cache_routes.remove(this);

		netPlan.cache_id2RouteMap.remove(id);
		layer.cache_routesDown.remove (this);
		NetPlan.removeNetworkElementAndShiftIndexes(layer.routes , index);
		
		/* remove the resources info */
		for (NetworkElement e : cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.keySet())
			if (e instanceof Resource) ((Resource) e).removeTraversingRoute(this);
		
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		removeId();
	}

	/** Sets the route carried traffic and the occupied capacity in the traversed links and resources (typically the same as the carried traffic),
	 * setting it to being the same in all the links and resources traversed. If the route is traversing a failing 
	 * link or node, its current carried traffic and occupied link capacity will be still zero, but if the route becomes up later, its carried traffic and 
	 * occupied link capacities will be the ones stated here. 
	 * @param newCarriedTraffic the new carried traffic 
	 * @param newOccupiedLinkAndResourcesCapacities the new occuppied link and resourcs capacity
	 */
	public void setCarriedTraffic(double newCarriedTraffic , double newOccupiedLinkAndResourcesCapacities)
	{
		setCarriedTraffic (newCarriedTraffic , Collections.nCopies(this.currentPath.size(), newOccupiedLinkAndResourcesCapacities));
	}


	/** Sets the route carried traffic and the occupied capacity in the traversed links and resources, so the capacity occupied can 
	 * be different (but always non-negative) in each.
	 * If the route is traversing a failing link or node, its current carried traffic and occupied 
	 * capacity will be still zero, but if the route becomes up later, its carried traffic and 
	 * occupied link capacities will be the ones stated here. 
	 * @param newCarriedTraffic the new carried traffic 
	 * @param linkAndResourcesOccupationInformation the new occupied capacity in traversed link, segments, and resources. If null, the occupied capacity equals the carried traffic in the links and resources 
	 */
	public void setCarriedTraffic (double newCarriedTraffic , List<Double> linkAndResourcesOccupationInformation)
	{
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		netPlan.checkIsModifiable();
		if (linkAndResourcesOccupationInformation == null)
			linkAndResourcesOccupationInformation = Collections.nCopies(this.currentPath.size(), newCarriedTraffic);
		if (linkAndResourcesOccupationInformation.size() != this.currentPath.size()) throw new Net2PlanException ("Wrong vector size"); 
		for (double val : linkAndResourcesOccupationInformation) if (val < 0) throw new Net2PlanException ("The occupation of a resource cannot be negative");

		/* Update the carried traffic of the route */
		//		System.out.println ("Route: " + this + ", setCarriedTraffic: newCarriedTraffic: " + newCarriedTraffic + ", newOccupiedLinkCapacity: " + newOccupiedLinkCapacity);
		newCarriedTraffic = NetPlan.adjustToTolerance(newCarriedTraffic);
		linkAndResourcesOccupationInformation = linkAndResourcesOccupationInformation.stream().map(e->NetPlan.adjustToTolerance(e)).collect(Collectors.toList());
		if (newCarriedTraffic < 0) throw new Net2PlanException ("Carried traffics must be non-negative");

		this.currentCarriedTrafficIfNotFailing = newCarriedTraffic;
		this.currentLinksAndResourcesOccupationIfNotFailing = new ArrayList<Double> (linkAndResourcesOccupationInformation);

		/* Now the update of the links and resources occupation */
		this.cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap = updateLinkResourceOccupationCache ();

		for (NetworkElement e : cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.keySet())
			if (e instanceof Resource)
				((Resource) e).addTraversingRoute(this , cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.get(e));
			else if (e instanceof Link)
				((Link) e).updateLinkTrafficAndOccupation();
		
		demand.carriedTraffic = 0; for (Route r : demand.cache_routes) demand.carriedTraffic += r.getCarriedTraffic();
		if (demand.coupledUpperLayerLink != null) demand.coupledUpperLayerLink.capacity = demand.carriedTraffic;
		
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}
	
	/** Sets the new sequence of links and resources traversed by the route, the carried traffic and the occupied capacity in the traversed links/resources. 
	 * Recall that while a route traverses failed links/nodes, its carrie traffic and occupied capacities compute as zero
	 * @param newCarriedTraffic
	 * @param newPath
	 * @param newOccupationInformation
	 */
	public void setPath (double newCarriedTraffic , List<? extends NetworkElement> newPath , List<Double> newOccupationInformation)
	{
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		netPlan.checkIsModifiable();
		Pair<List<Link>,List<Resource>> res = netPlan.checkPathValidityForDemand (newPath, demand);
		List<Link> newSeqLinks = res.getFirst();
		List<Resource> newSeqResources = res.getSecond();
		if (newPath.size() != newOccupationInformation.size()) throw new Net2PlanException ("Wrong size of occupation array");
		for (Double val : newOccupationInformation) if (val < 0) throw new Net2PlanException ("The occupation of a link/resource cannot be negative");
		if (newPath.size() != newOccupationInformation.size()) throw new Net2PlanException ("Wrong size of array");
		
		/* Remove the old route trace in the traversed nodes and links */
		this.setCarriedTraffic(0 , 0); // releases all links, segments and resources occupation
		for (Resource resource : this.getSeqResourcesTraversed()) resource.removeTraversingRoute(this); // removes the current route
		for (Link link : this.cache_seqLinksRealPath) 
			link.cache_traversingRoutes.remove (this);
		for (Node node : cache_seqNodesRealPath)
			node.cache_nodeAssociatedRoutes.remove (this);
		layer.cache_routesDown.remove(this);

		/* Update this route info */
		this.currentPath = new LinkedList<NetworkElement> (newPath);
		this.cache_seqLinksRealPath = new LinkedList<Link> (newSeqLinks); 
		boolean isRouteUp = demand.ingressNode.isUp;
		this.cache_seqNodesRealPath = new LinkedList<Node> (); cache_seqNodesRealPath.add (demand.getIngressNode()); 
		for (Link e : cache_seqLinksRealPath) 
		{
			cache_seqNodesRealPath.add (e.getDestinationNode());
			isRouteUp = (isRouteUp && e.isUp && e.destinationNode.isUp);
		}
		if (!isRouteUp) layer.cache_routesDown.add(this);
		
		/* Update traversed links and nodes caches  */
		for (Link link : newSeqLinks) 
		{
			Integer numPassingTimes = link.cache_traversingRoutes.get (this); 
			if (numPassingTimes == null) numPassingTimes = 1; else numPassingTimes ++; link.cache_traversingRoutes.put (this , numPassingTimes);
		}
		for (Node node : cache_seqNodesRealPath)
			node.cache_nodeAssociatedRoutes.add (this);

		setCarriedTraffic (newCarriedTraffic , newOccupationInformation);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/** Sets the given sequence of links/resources as the primary path for this route. The path should be valid for the route (although it may traverse failing links/nodes).
	 * @param primaryPath
	 */
	public void setPrimaryPath (List<? extends NetworkElement> primaryPath)
	{
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		netPlan.checkIsModifiable();
		netPlan.checkPathValidityForDemand (primaryPath, demand);
		this.primaryPath = new ArrayList<NetworkElement> (primaryPath);
	}

	/** Sets the new sequence of links traversed by the route. Since this method receives a list of 
	 * links as input, the new route does not traverse resources. Thus, this 
	 * method cannot be used in service chains. The route carried traffic is not changed. If the current occupied capacity in 
	 * the traversed links is constant (not different link to link), this same amount is the occupied capacity in the links 
	 * of the new path. If not, an exception is thrown (in this case, the setPath method should be used). 
	 * As usual, if (while) the new route traverses failing link or nodes, its current 
	 * carried traffic and occupied link capacities will be zero. 
	 * @param seqLinks the new sequence of links and protection segments
	 */
	public void setSeqLinks(List<Link> seqLinks)
	{
		if (demand.isServiceChainRequest()) throw new Net2PlanException ("This method is not valid for service chains");
		final double firstLinkOccup = this.currentLinksAndResourcesOccupationIfNotFailing.get(0);
		for (double val : currentLinksAndResourcesOccupationIfNotFailing) if (val != firstLinkOccup) throw new Net2PlanException ("This method can only be used when the occupation in all the lnks is the same");
		if (seqLinks.equals(this.cache_seqLinksRealPath)) return;
		setPath(this.currentCarriedTrafficIfNotFailing, seqLinks, Collections.nCopies(seqLinks.size(), firstLinkOccup));
	}

	public String toString () { return "r" + index + " (id " + id + ")"; }

	void checkCachesConsistency ()
	{
//		final NetworkLayer layer;
//		final Demand demand;
//		final Node ingressNode;
//		final Node egressNode;
//		double currentCarriedTrafficIfNotFailing;
//		List<Double> currentLinksAndResourcesOccupationIfNotFailing;
//		List<NetworkElement> primaryPath;
//		List<NetworkElement> currentPath; // each object is a Link, or a Resource

//		List<List<NetworkElement>> backupPaths;
//		List<Link> cache_seqLinksRealPath;
//		List<Node> cache_seqNodesRealPath;
//		Map<NetworkElement,Double> cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap;  
		


		assertTrue (layer.routes.contains(this));
		assertTrue (demand.cache_routes.contains(this));
		assertNotNull (ingressNode.netPlan);
		assertNotNull (egressNode.netPlan);
		assertNotNull (layer.netPlan);
		assertNotNull (primaryPath);
		assertNotNull (currentLinksAndResourcesOccupationIfNotFailing);
		assertNotNull (currentPath);
		assertNotNull (cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap);
		assertNotNull (backupPaths);
		assertNotNull (cache_seqLinksRealPath);
		assertNotNull (cache_seqNodesRealPath);

		netPlan.checkInThisNetPlanAndLayer(currentPath , layer);
		netPlan.checkInThisNetPlanAndLayer(cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.keySet() , layer);
		netPlan.checkInThisNetPlanAndLayer(cache_seqLinksRealPath , layer);
		netPlan.checkInThisNetPlanAndLayer(cache_seqNodesRealPath , layer);
		for (NetworkElement e : currentPath)
		{
			assertNotNull(e);
			if (e instanceof Link) { final Link ee = (Link) e; assertTrue (ee.cache_traversingRoutes.containsKey(this)); }
			if (e instanceof Resource) { final Resource ee = (Resource) e; assertTrue (ee.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.containsKey(this)); }
		}
		for (Entry<NetworkElement,Double> entry : cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.entrySet())
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
				assertTrue(((Link) entry.getKey()).cache_traversingRoutes.get(this) > 0);
			}
		}
		List<Resource> travResources = new LinkedList<Resource> ();
		for (NetworkElement el : currentPath) if (el instanceof Resource) travResources.add((Resource) el);
		for (Resource res : travResources)
		{
			assertEquals (netPlan.cache_id2ResourceMap.get(res.id) , res);
			assertEquals (netPlan.resources.get(res.index) , res);
			assertNotNull(res.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.get(this));
			assertNotNull(cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.get(res));
			assertEquals (res.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.get(this) , cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.get(res) , 0);
		}

		for (Link link : cache_seqLinksRealPath) assertTrue (link.cache_traversingRoutes.containsKey(this));
		for (Node node : cache_seqNodesRealPath) assertTrue (node.cache_nodeAssociatedRoutes.contains(this));
		boolean shouldBeUp = true; for (Link e : cache_seqLinksRealPath) if (!e.isUp) { shouldBeUp = false; break; }
		if (shouldBeUp) for (Node n : cache_seqNodesRealPath) if (!n.isUp) { shouldBeUp = false; break; }
		if (!shouldBeUp != this.isDown())
		{
			System.out.println ("Route : " + this + ", should be up: " + shouldBeUp + ", isDown: " + isDown() + ", carried traffic: " + this.getCarriedTraffic() + ", carried all ok: " + currentCarriedTrafficIfNotFailing);
			for (Link e : cache_seqLinksRealPath)
				System.out.println ("Link e: " + e + ", isUp " + e.isUp);
			for (Node n : cache_seqNodesRealPath)
				System.out.println ("Node n: " + n + ", isUp " + n.isUp);
			throw new RuntimeException("Bad.");
		}
		if (shouldBeUp)
		{
			assertEquals(getCarriedTraffic() , currentCarriedTrafficIfNotFailing , 0.001);
		}
		else
		{
			assertEquals(getCarriedTraffic() , 0 , 0.001);
		}
	}

	/** Given a path, composed of a sequence of links and resources, extracts the list of links, filtering out the resources
	 * @param seqLinksAndResources sequence of links and resources
	 * @return the list of links
	 */
	public static List<Link> getSeqLinks (List<? extends NetworkElement> seqLinksAndResources)
	{
		List<Link> links = new LinkedList<Link> ();
		for (NetworkElement e : seqLinksAndResources) 
			if (e instanceof Link) links.add((Link) e); 
		return links;
	}

	/** Given a path, composed of a sequence of links and resources, extracts the list of resources, filtering out the links
	 * @param seqLinksAndResources sequence of links and resources
	 * @return the list of resources
	 */
	public static List<Resource> getSeqResources (List<? extends NetworkElement> seqLinksAndResources)
	{
		List<Resource> res = new LinkedList<Resource> ();
		for (NetworkElement e : seqLinksAndResources) 
			if (e instanceof Resource) res.add((Resource) e); 
		return res;
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
		else throw new RuntimeException ("Bad");
		if (res == null) throw new RuntimeException ("Element of id: " + e.id + ", of class: " + e.getClass().getName() + ", does not exsit in current NetPlan");
		return res;
	}
	
	private List<? extends NetworkElement> getInThisNetPlan (List<? extends NetworkElement> list)
	{
		List<NetworkElement> res = new ArrayList<NetworkElement> (list.size());
		for (NetworkElement e : list)
		{
			if (e instanceof Link) res.add(this.netPlan.getLinkFromId(e.id));
			else if (e instanceof Resource) res.add(this.netPlan.getResourceFromId(e.id));
			else if (e instanceof Node) res.add(this.netPlan.getNodeFromId(e.id));
			else throw new RuntimeException ("Bad");
			if (res.get(res.size()-1) == null) throw new RuntimeException ("Element of id: " + e.id + ", of class: " + e.getClass().getName() + ", does not exsit in current NetPlan");
		}
		return res;
	}

	private Map<NetworkElement,Double> updateLinkResourceOccupationCache ()
	{
		Map<NetworkElement,Double> res = new HashMap<NetworkElement,Double> ();
		for (int step = 0; step < currentPath.size() ; step ++)
		{
			final NetworkElement e = currentPath.get(step);
			final Double previousAccumOccupCap = res.get(e);
			final Double currentStepOccup = currentLinksAndResourcesOccupationIfNotFailing.get(step);
			res.put(e , previousAccumOccupCap == null? currentStepOccup : currentStepOccup + previousAccumOccupCap);
		}
		return res;
	}
}
