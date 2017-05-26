// 20161205 sigo con reader v5 y con la parte de save, para el route

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
import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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

@SuppressWarnings("unchecked")
public class Route extends NetworkElement
{
	final NetworkLayer layer;
	final Demand demand;
	final Node ingressNode;
	final Node egressNode;
	List<NetworkElement> currentPath; // each object is a Link, or a Resource
	double currentCarriedTrafficIfNotFailing;
	List<Double> currentLinksAndResourcesOccupationIfNotFailing;
	List<NetworkElement> initialStatePath; // could traverse removed links/resources
	List<Double> initialStateOccupationIfNotFailing; // could traverse removed links/resources
	double initialStateCarriedTrafficIfNotFailing;
	List<Route> backupRoutes;
	List<Link> cache_seqLinksRealPath;
	List<Node> cache_seqNodesRealPath;
	Map<NetworkElement,Double> cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap;
	Set<Route> cache_routesIAmBackUp;
	boolean cache_hasLoops;
	double cache_propagationDelayMs;

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
		this.currentPath = new LinkedList<NetworkElement> (seqLinksAndResourcesTraversed);
		this.backupRoutes = new ArrayList<Route> ();
		this.currentCarriedTrafficIfNotFailing = 0;
		this.currentLinksAndResourcesOccupationIfNotFailing = Collections.nCopies(seqLinksAndResourcesTraversed.size() , 0.0);
		this.initialStateCarriedTrafficIfNotFailing = -1;
		this.initialStateOccupationIfNotFailing = null;
		this.initialStatePath = new ArrayList<NetworkElement> (seqLinksAndResourcesTraversed);
		this.cache_seqLinksRealPath = Route.getSeqLinks(seqLinksAndResourcesTraversed);
		this.cache_seqNodesRealPath = Route.listTraversedNodes(cache_seqLinksRealPath);
		this.cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap = updateLinkResourceOccupationCache ();
		this.cache_routesIAmBackUp = new HashSet<Route> ();
		this.cache_hasLoops = hasLoops (cache_seqNodesRealPath);
		if (cache_hasLoops) demand.routingCycleType = RoutingCycleType.OPEN_CYCLES;
		this.cache_propagationDelayMs = 0;
		this.updatePropagationAndProcessingDelayInMiliseconds();
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
		if (this.initialStateCarriedTrafficIfNotFailing != e2.initialStateCarriedTrafficIfNotFailing) return false;
		if (!this.initialStateOccupationIfNotFailing.equals(e2.initialStateOccupationIfNotFailing)) return false;
		if (!NetPlan.isDeepCopy(this.initialStatePath , e2.initialStatePath)) return false;
		if (backupRoutes.size() != e2.backupRoutes.size()) return false;
		if (!NetPlan.isDeepCopy(this.backupRoutes , e2.backupRoutes)) return false;
		if (!NetPlan.isDeepCopy(this.cache_seqLinksRealPath , e2.cache_seqLinksRealPath)) return false;
		if (!NetPlan.isDeepCopy(this.cache_seqNodesRealPath , e2.cache_seqNodesRealPath)) return false;
		if (!NetPlan.isDeepCopy(this.cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap , e2.cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap)) return false;
		if (!NetPlan.isDeepCopy(this.cache_routesIAmBackUp , e2.cache_routesIAmBackUp)) return false;
		return true;
	}

	void copyFrom (Route origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.currentCarriedTrafficIfNotFailing = origin.currentCarriedTrafficIfNotFailing;
		this.currentLinksAndResourcesOccupationIfNotFailing = new ArrayList<Double> (origin.currentLinksAndResourcesOccupationIfNotFailing);
		this.initialStateCarriedTrafficIfNotFailing = origin.initialStateCarriedTrafficIfNotFailing;
		this.initialStateOccupationIfNotFailing = new ArrayList<Double> (origin.initialStateOccupationIfNotFailing);
		this.initialStatePath = (List<NetworkElement>) getInThisNetPlan(origin.initialStatePath);
		this.currentPath = (List<NetworkElement>) getInThisNetPlan(origin.currentPath);
		this.backupRoutes = (List<Route>) getInThisNetPlan(origin.backupRoutes);
		this.cache_routesIAmBackUp = (Set<Route>) getInThisNetPlan(origin.cache_routesIAmBackUp);
		this.cache_seqLinksRealPath = (List<Link>) getInThisNetPlan(origin.cache_seqLinksRealPath);
		this.cache_seqNodesRealPath = (List<Node>) getInThisNetPlan(origin.cache_seqNodesRealPath);
		this.cache_hasLoops = origin.cache_hasLoops;
		this.cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.clear();
		for (Entry<NetworkElement,Double> e : origin.cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.entrySet()) this.cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.put(getInThisNetPlan (e.getKey()) , e.getValue());
	}


	/** Indicates if the route is a serivce chain: traverses resources
	 * @return see above
	 */
	public boolean isServiceChain () { return demand.isServiceChainRequest(); }


	/** Return the set of routes that this route is a designated as a backup for them
	 * @return The initial sequence of links as an unmodifiable list
	 */
	public Set<Route> getRoutesIAmBackup () { return Collections.unmodifiableSet(this.cache_routesIAmBackUp); }

	/** Returns the list of backup routes for this route (the ones defined as backup by the user)
	 * @return The initial sequence of links as an unmodifiable list
	 */
	public List<Route> getBackupRoutes () { return Collections.unmodifiableList(this.backupRoutes); }

	/** Returns the initial state of the route (the one when it was created): the carried traffic (in the no-failure state), its path (sequence of links and/or resources), and the occupation information (occupation in each link/resource travserse).
	 * Note that some links/resources of this initial state could no longer exist
	 * @return The info
	 */
	public Triple<Double,List<NetworkElement>,List<Double>> getInitialState () { return Triple.of(initialStateCarriedTrafficIfNotFailing, Collections.unmodifiableList(initialStatePath), Collections.unmodifiableList(initialStateOccupationIfNotFailing)); }

	/** Return the current path (sequence of links and resources) of the route.
	 * @return The info
	 */
	public List<NetworkElement> getPath () { return Collections.unmodifiableList(this.currentPath);}

	/** Returns true if this route has been defined as a backup route for other
	 * @return the info
	 */
	public boolean isBackupRoute () { return !cache_routesIAmBackUp.isEmpty(); }

	/** Returns true if this route has at least one backup route defined
	 * @return the info
	 */
	public boolean hasBackupRoutes () { return !backupRoutes.isEmpty(); }

	/**
	 * <p>Adds an existing route backupRoute in the same demand, designating it as a backup of this route.
	 * For this to happen, this route cannot have backup routes itself, or an exception is thrown.</p>
	 * @param backupRoute the backup route
	 */
	public void addBackupRoute (Route backupRoute)
	{
		if (backupRoute == null) throw new Net2PlanException ("The passed element is NULL");
		this.checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (!backupRoute.demand.equals(demand)) throw new Net2PlanException ("The backup route must be of the same demand as the primary");
		if (backupRoute.hasBackupRoutes()) throw new Net2PlanException ("A backup route cannot have backup routes itself");
		if (this.backupRoutes.contains(backupRoute)) throw new Net2PlanException ("The route is already a backup route");
		this.backupRoutes.add (backupRoute);
		backupRoute.cache_routesIAmBackUp.add(this);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Removes the given backupRoute from the backupRoute list of this route</p>
	 * @param backupRoute The route to remove as backup
	 */
	public void removeBackupRoute (Route backupRoute)
	{
		if (backupRoute == null) throw new Net2PlanException ("The passed element is NULL");
		this.checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (!backupRoutes.contains(backupRoute)) throw new Net2PlanException ("This route is not a backup");
		backupRoute.cache_routesIAmBackUp.remove(this);
		this.backupRoutes.remove (backupRoute);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
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
	 * @param e one link or resource where to see the occupation (if not part of the route, zero is returned). If no element is passed, the result is the one of the first traversed element
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
		return cache_propagationDelayMs;
	}
	void updatePropagationAndProcessingDelayInMiliseconds ()
	{
		this.cache_propagationDelayMs = 0;
		double thisRouteLengthKm = 0;
		for (NetworkElement e : currentPath)
		{
			if (e instanceof Link)
			{
				cache_propagationDelayMs += ((Link) e).getPropagationDelayInMs();
				thisRouteLengthKm += ((Link) e).getLengthInKm();
			}
			else if (e instanceof Resource)
				cache_propagationDelayMs += ((Resource) e).processingTimeToTraversingTrafficInMs;
		}
		demand.cache_worstCasePropagationTimeMs = Math.max(demand.cache_worstCasePropagationTimeMs, this.cache_propagationDelayMs);
		demand.cache_worstCaseLengthInKm = Math.max(demand.cache_worstCaseLengthInKm, thisRouteLengthKm);
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

	/** Returns the number of times that a particular link or resource is traversed
	 * @param e the link or resource to check
	 * @return the number of times it is traversed
	 */
	public int getNumberOfTimesIsTraversed (NetworkElement e)
	{
		if (e instanceof Link)
		{
			Integer num = ((Link) e).cache_traversingRoutes.get (this); return num == null? 0 : num;
		}
		else if (e instanceof Resource)
		{
			int num = 0; for (NetworkElement ee : currentPath) if (ee.equals(e)) num ++;
			return num;
		}
		else throw new Net2PlanException ("This method can be called only for links and resources");
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
		return this.cache_hasLoops;
	}
	private static boolean hasLoops (List<Node> seqNodes)
	{
		Set<Node> nodes = new HashSet<Node> (seqNodes);
		return nodes.size () < seqNodes.size();
	}

	/**
	 * Returns true if the route is traversing a link or node that is down. Returns false otherwise
	 * @return see description above
	 */
	public boolean isDown ()
	{
		return layer.cache_routesDown.contains(this);
	}

	/** Returns true if the route is traversing a link with zero capacity
	 * @return see above
	 */
	public boolean isTraversingZeroCapLinks ()
	{
		return layer.cache_routesTravLinkZeroCap.contains(this);
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

		for (Route backupRoute : backupRoutes) backupRoute.cache_routesIAmBackUp.remove(this);
		for (Route primaryRoute : cache_routesIAmBackUp) primaryRoute.backupRoutes.remove(this);

		netPlan.cache_id2RouteMap.remove(id);
		layer.cache_routesDown.remove (this);
		layer.cache_linksZeroCap.remove(this);
		NetPlan.removeNetworkElementAndShiftIndexes(layer.routes , index);

		/* remove the resources info */
		for (NetworkElement e : cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.keySet())
			if (e instanceof Resource) ((Resource) e).removeTraversingRoute(this);

        for (String tag : tags) netPlan.cache_taggedElements.get(tag).remove(this);
        
        /* potentially update demand routing cycle type */
        if (cache_hasLoops)
        {
        	demand.routingCycleType = RoutingCycleType.LOOPLESS;
        	for (Route r : demand.cache_routes) if (r.cache_hasLoops) { demand.routingCycleType = RoutingCycleType.OPEN_CYCLES; break; }
        }

        if (demand.cache_worstCasePropagationTimeMs <= this.cache_propagationDelayMs)
        {
        	demand.cache_worstCasePropagationTimeMs = 0;
        	for (Route r : demand.cache_routes) demand.cache_worstCasePropagationTimeMs = Math.max(demand.cache_worstCasePropagationTimeMs, r.cache_propagationDelayMs);
        }

        if (demand.cache_worstCaseLengthInKm <= this.getLengthInKm())
        {
        	demand.cache_worstCaseLengthInKm = 0;
        	for (Route r : demand.cache_routes) demand.cache_worstCaseLengthInKm = Math.max(demand.cache_worstCaseLengthInKm, r.getLengthInKm());
        }

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
	 * @param linkAndResourcesOccupationInformation the new occupied capacity in traversed link and resources. If null, the occupied capacities are kept unchanged
	 */
	public void setCarriedTraffic (double newCarriedTraffic , List<Double> linkAndResourcesOccupationInformation)
	{
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		netPlan.checkIsModifiable();
		final double oldRouteCarriedTrafficIfNotFailing = this.currentCarriedTrafficIfNotFailing;
		final boolean isThisRouteDown = this.isDown();
		if (linkAndResourcesOccupationInformation == null)
			linkAndResourcesOccupationInformation = new ArrayList<Double> (this.currentLinksAndResourcesOccupationIfNotFailing); //Collections.nCopies(this.currentPath.size(), newCarriedTraffic);
		if (linkAndResourcesOccupationInformation.size() != this.currentPath.size()) throw new Net2PlanException ("Wrong vector size");
		for (double val : linkAndResourcesOccupationInformation) if (val < 0) throw new Net2PlanException ("The occupation of a resource cannot be negative");

		/* Update the carried traffic of the route */
		//		System.out.println ("Route: " + this + ", setCarriedTraffic: newCarriedTraffic: " + newCarriedTraffic + ", newOccupiedLinkCapacity: " + newOccupiedLinkCapacity);
		newCarriedTraffic = NetPlan.adjustToTolerance(newCarriedTraffic);
		linkAndResourcesOccupationInformation = linkAndResourcesOccupationInformation.stream().map(e->NetPlan.adjustToTolerance(e)).collect(Collectors.toList());
		if (newCarriedTraffic < 0) throw new Net2PlanException ("Carried traffics must be non-negative");

		/* Update the initial state if this is the first time this is called */
		if (initialStateCarriedTrafficIfNotFailing == -1)
		{
			this.initialStateCarriedTrafficIfNotFailing = newCarriedTraffic;
			this.initialStateOccupationIfNotFailing = new ArrayList<Double> (linkAndResourcesOccupationInformation);
			if (initialStateOccupationIfNotFailing.size() != initialStatePath.size()) throw new RuntimeException ("Bad");
		}

		this.currentCarriedTrafficIfNotFailing = newCarriedTraffic;
		this.currentLinksAndResourcesOccupationIfNotFailing = new ArrayList<Double> (linkAndResourcesOccupationInformation);

		/* Now the update of the links and resources occupation */
		this.cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap = updateLinkResourceOccupationCache ();

		demand.carriedTraffic = 0; for (Route r : demand.cache_routes) demand.carriedTraffic += r.getCarriedTraffic();
		if (demand.coupledUpperLayerLink != null)
			demand.coupledUpperLayerLink.updateCapacityAndZeroCapacityLinksAndRoutesCaches(demand.carriedTraffic);

		for (NetworkElement e : cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.keySet())
			if (e instanceof Resource)
				((Resource) e).addTraversingRoute(this , cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.get(e));
			else if (e instanceof Link)
				((Link) e).updateLinkTrafficAndOccupation();


		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/** Sets the new sequence of links and/or resources traversed by the route, carried traffic, and occupied capacity in the traversed links/resources
	 * If the new route traverses failing link or nodes, its current
	 * carried traffic and occupied link capacities will be zero. If not, will be the base ones in the no failure state
	 * @param newCarriedTraffic new amount of carried traffic
	 * @param newPath the new sequence of links and resources
	 * @param newOccupationInformation the new link/resource occupation info
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
		layer.cache_routesTravLinkZeroCap.remove(this);

		/* Update this route info */
		this.currentPath = new LinkedList<NetworkElement> (newPath);
		this.cache_seqLinksRealPath = new LinkedList<Link> (newSeqLinks);
		boolean isRouteUp = demand.ingressNode.isUp;
		boolean isRouteTravZeroCapLinks = false;
		this.cache_seqNodesRealPath = new LinkedList<Node> (); cache_seqNodesRealPath.add (demand.getIngressNode());
		for (Link e : cache_seqLinksRealPath)
		{
			cache_seqNodesRealPath.add (e.getDestinationNode());
			isRouteUp = (isRouteUp && e.isUp && e.destinationNode.isUp);
			if (e.capacity < Configuration.precisionFactor) isRouteTravZeroCapLinks = true;
		}
		if (!isRouteUp) layer.cache_routesDown.add(this);
		if (isRouteTravZeroCapLinks) layer.cache_routesTravLinkZeroCap.add(this);
		/* Update traversed links and nodes caches  */
		for (Link link : newSeqLinks)
		{
			Integer numPassingTimes = link.cache_traversingRoutes.get (this);
			if (numPassingTimes == null) numPassingTimes = 1; else numPassingTimes ++; link.cache_traversingRoutes.put (this , numPassingTimes);
		}
		for (Node node : cache_seqNodesRealPath)
			node.cache_nodeAssociatedRoutes.add (this);
		this.cache_hasLoops = hasLoops(cache_seqNodesRealPath);
		if (cache_hasLoops) demand.routingCycleType = RoutingCycleType.OPEN_CYCLES;

		this.updatePropagationAndProcessingDelayInMiliseconds();

		setCarriedTraffic (newCarriedTraffic , newOccupationInformation);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
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
		super.checkCachesConsistency ();

		if (!(layer.routes.contains(this))) throw new RuntimeException();
		if (!(demand.cache_routes.contains(this))) throw new RuntimeException();

		if (ingressNode.netPlan == null) throw new RuntimeException();
		if (egressNode.netPlan == null) throw new RuntimeException();
		if (layer.netPlan == null) throw new RuntimeException();
		if (initialStatePath == null) throw new RuntimeException();
		if (currentLinksAndResourcesOccupationIfNotFailing == null) throw new RuntimeException();
		if (currentPath == null) throw new RuntimeException();
		if (cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap == null) throw new RuntimeException();
		if (backupRoutes == null) throw new RuntimeException();
		if (cache_seqLinksRealPath == null) throw new RuntimeException();
		if (cache_seqNodesRealPath == null) throw new RuntimeException();
		if (cache_routesIAmBackUp == null) throw new RuntimeException();

		if (!this.cache_hasLoops != (new HashSet<> (cache_seqNodesRealPath).size() == cache_seqNodesRealPath.size())) throw new RuntimeException();

		netPlan.checkInThisNetPlanAndLayer(currentPath , layer);
		netPlan.checkInThisNetPlanAndLayer(cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.keySet() , layer);
		netPlan.checkInThisNetPlanAndLayer(cache_seqLinksRealPath , layer);
		netPlan.checkInThisNetPlanAndLayer(cache_seqNodesRealPath , layer);
		netPlan.checkInThisNetPlanAndLayer(cache_routesIAmBackUp , layer);
		for (NetworkElement e : currentPath)
		{
		    if (e == null) throw new RuntimeException();
			if (e instanceof Link)
			{
			    final Link ee = (Link) e;
			    if (!(ee.cache_traversingRoutes.containsKey(this))) throw new RuntimeException();
			}
			if (e instanceof Resource)
			{
			    final Resource ee = (Resource) e;
			    if (!(ee.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.containsKey(this))) throw new RuntimeException();
			}
		}
		for (Entry<NetworkElement,Double> entry : cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.entrySet())
		{
		    if (entry.getKey() == null) throw new RuntimeException();
		    if (entry.getValue() == null) throw new RuntimeException();

			if (entry.getKey() instanceof Resource)
			{
                if (((Resource) entry.getKey()).cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute == null) throw new RuntimeException();

                if (Double.compare(((Resource) entry.getKey()).cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.get(this), entry.getValue()) != 0)
                    throw new RuntimeException();
			}
			else if (entry.getKey() instanceof Link)
			{
			    if (!(((Link) entry.getKey()).cache_traversingRoutes.containsKey(this))) throw new RuntimeException();
			    if (!(((Link) entry.getKey()).cache_traversingRoutes.get(this) > 0)) throw new RuntimeException();
			}
		}
		List<Resource> travResources = new LinkedList<Resource> ();
		for (NetworkElement el : currentPath) if (el instanceof Resource) travResources.add((Resource) el);
		for (Resource res : travResources)
		{
		    if (netPlan.cache_id2ResourceMap.get(res.id) != res) throw new RuntimeException();
		    if (netPlan.resources.get(res.index) != res) throw new RuntimeException();

			if (res.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.get(this) == null) throw new RuntimeException();
			if (cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.get(res) == null) throw new RuntimeException();

			if (Double.compare(res.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.get(this), cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.get(res)) != 0)
			    throw new RuntimeException();
		}

		for (Link link : cache_seqLinksRealPath)
		    if (!(link.cache_traversingRoutes.containsKey(this))) throw new RuntimeException();

		for (Node node : cache_seqNodesRealPath)
		    if (!(node.cache_nodeAssociatedRoutes.contains(this))) throw new RuntimeException();

		boolean shouldBeUp = true; for (Link e : cache_seqLinksRealPath) if (!e.isUp) { shouldBeUp = false; break; }
		boolean travZeroCapLinks = cache_seqLinksRealPath.stream().anyMatch(e->e.capacity<Configuration.precisionFactor);
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
		    if (Math.abs(getCarriedTraffic() - currentCarriedTrafficIfNotFailing) > 0.001) throw new RuntimeException();
		} else
		{
		    if (Math.abs(getCarriedTraffic()) > 0.001) throw new RuntimeException();
		}
		if (travZeroCapLinks)
		    if (!(layer.cache_routesTravLinkZeroCap.contains(this))) throw new RuntimeException();
		else
		    if (!layer.cache_routesTravLinkZeroCap.contains(this)) throw new RuntimeException();

		for (Route r : backupRoutes) if(!(r.cache_routesIAmBackUp.contains(this))) throw new RuntimeException();
		for (Route r : cache_routesIAmBackUp) if(!(r.backupRoutes.contains(this))) throw new RuntimeException();
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
			else if (e instanceof Route) res.add(this.netPlan.getRouteFromId(e.id));
			else throw new RuntimeException ("Bad");
			if (res.get(res.size()-1) == null) throw new RuntimeException ("Element of id: " + e.id + ", of class: " + e.getClass().getName() + ", does not exsit in current NetPlan");
		}
		return res;
	}

	private Set<? extends NetworkElement> getInThisNetPlan (Set<? extends NetworkElement> list)
	{
		Set<NetworkElement> res = new HashSet<NetworkElement> (list.size());
		for (NetworkElement e : list)
		{
			NetworkElement ee = null;
			if (e instanceof Link) ee = this.netPlan.getLinkFromId(e.id);
			else if (e instanceof Resource) ee = this.netPlan.getResourceFromId(e.id);
			else if (e instanceof Node) ee = this.netPlan.getNodeFromId(e.id);
			else if (e instanceof Route) ee = this.netPlan.getRouteFromId(e.id);
			else throw new RuntimeException ("Bad");
			if (ee == null) throw new RuntimeException ("Element of id: " + e.id + ", of class: " + e.getClass().getName() + ", does not exsit in current NetPlan");
			res.add(ee);
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
