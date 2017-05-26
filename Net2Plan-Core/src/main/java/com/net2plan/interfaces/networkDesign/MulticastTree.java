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
import com.net2plan.utils.Triple;

import java.util.*;
import java.util.Map.Entry;


/**
 * <p>This class contains a representation of a unidirectional multicast tree, an structure used to carry traffic from multicast demands. Multicast trees are characterized
 * by the {@link com.net2plan.interfaces.networkDesign.MulticastDemand Multicast Demand} they carry traffic from, the traversed {@link com.net2plan.interfaces.networkDesign.Link Links}, the amount of traffic carried (in traffic units) and the
 * amount of link capacity they occupy in the traversed links (in link capacity units). The links and the demand belong to the same layer, the layer of the multicas tree. </p>
 * <p> Multicast trees input node and output nodes are that of the associated multicast demand. The set of traversed links must make a tree,
 *  originated in the input node, and that reaches all the output nodes. The tree is such that there is a unique form
 * of reaching end nodes from the ingress node. This means that all the nodes in the tree have one input link, but the origin that has none.
 * Also, it means that the number of traversed nodes equals the number of links plus one.</p>
 * <p> Multicast trees can be rerouted (change in its set of links), and its carried traffic and occupied capacity can change. If a tree traverses a link or 
 * node that is down, its carried traffic automatically drops to zero. If the failing links/nodes are set to up state, its carried traffic and occupied link capacity 
 * goes back to the value before the failure. </p>
 *
 * @see com.net2plan.interfaces.networkDesign.Node
 * @see com.net2plan.interfaces.networkDesign.Link
 * @see com.net2plan.interfaces.networkDesign.MulticastDemand
 * @author Pablo Pavon-Marino
 * @since 0.4.0
 */
public class MulticastTree extends NetworkElement
{
	final NetworkLayer layer;
	final MulticastDemand demand;
	Map<Node,List<Link>> pathToEgressNode;
	Set<Link> linkSet;
	final Set<Link> initialSetLinksWhenWasCreated;
	double carriedTrafficIfNotFailing;
	double occupiedLinkCapacityIfNotFailing;
	Set<Node> cache_traversedNodes;
	Map<Node,Link> cache_ingressLinkOfNode;
	Map<Node,Set<Link>> cache_egressLinksOfNode;
	

	MulticastTree (NetPlan netPlan , long id , int index,  MulticastDemand demand , Set<Link> links , AttributeMap attributes)
	{
		super (netPlan , id , index , attributes);
		
		this.pathToEgressNode = netPlan.checkMulticastTreeValidityForDemand(links, demand).getFirst();
		this.layer = demand.layer;
		this.demand = demand;
		this.linkSet = new HashSet<Link> (links);
		this.initialSetLinksWhenWasCreated = new HashSet<Link> (links);
		this.carriedTrafficIfNotFailing = 0; 
		this.occupiedLinkCapacityIfNotFailing = 0; 
		Triple<Set<Node>,Map<Node,Link>,Map<Node,Set<Link>>> caches = updateCaches ();	
		this.cache_traversedNodes = caches.getFirst ();
		this.cache_ingressLinkOfNode = caches.getSecond ();
		this.cache_egressLinksOfNode = caches.getThird ();
	}

	
	boolean isDeepCopy (MulticastTree e2)
	{
		if (!super.isDeepCopy(e2)) return false;
		if (layer.id != e2.layer.id) return false;
		if (demand.id != e2.demand.id) return false;
		if (!NetPlan.isDeepCopy(this.pathToEgressNode.keySet() , e2.pathToEgressNode.keySet ())) return false;
		for (Node n1 : pathToEgressNode.keySet())
		{
			final Node n2 = e2.netPlan.getNodeFromId(n1.id);
			final Collection<Long> l1Links = NetPlan.getIds(pathToEgressNode.get(n1));
			final Collection<Long> l2Links = NetPlan.getIds(e2.pathToEgressNode.get(n2));
			if (!l1Links.equals(l2Links)) return false;
		}
		if (!NetPlan.isDeepCopy(this.linkSet , e2.linkSet)) return false;
		if (!NetPlan.isDeepCopy(this.initialSetLinksWhenWasCreated , e2.initialSetLinksWhenWasCreated)) return false;
		if (!NetPlan.isDeepCopy(this.cache_traversedNodes , e2.cache_traversedNodes)) return false;
		if (this.carriedTrafficIfNotFailing != e2.carriedTrafficIfNotFailing) return false;
		if (this.occupiedLinkCapacityIfNotFailing != e2.occupiedLinkCapacityIfNotFailing) return false;
		if (!NetPlan.isDeepCopy(this.cache_ingressLinkOfNode.keySet() , e2.cache_ingressLinkOfNode.keySet ())) return false;
		if (!NetPlan.isDeepCopy(this.cache_egressLinksOfNode.keySet() , e2.cache_egressLinksOfNode.keySet ())) return false;
		for (Node n1 : cache_ingressLinkOfNode.keySet())
		{
			final Node n2 = e2.netPlan.getNodeFromId(n1.id);
			if (cache_ingressLinkOfNode.get(n1).id != e2.cache_ingressLinkOfNode.get(n2).id) return false;
		}
		for (Node n1 : cache_egressLinksOfNode.keySet())
		{
			final Node n2 = e2.netPlan.getNodeFromId(n1.id);
			if (!NetPlan.isDeepCopy(this.cache_egressLinksOfNode.get(n1) , e2.cache_egressLinksOfNode.get(n2))) return false;
		}
		return true;
	}

	
	/**
	 * <p>Returns the initial {@code Set} of {@link com.net2plan.interfaces.networkDesign.Link Links} of the tree when it was created. That is, before any tree rerouting action was made.</p>
	 * @return The {@code Set} of links, as an unmodifiable set
	 */
	public Set<Link> getInitialLinkSet () { return Collections.unmodifiableSet(this.initialSetLinksWhenWasCreated); }


	/**
	 * <p>Changes the set of {@link com.net2plan.interfaces.networkDesign.Link Links} of the tree to the ones when the tree was created. That is, before any tree rerouting action was made.</p>
	 * It is equivalent to {@code setLinks(getInitialLinkSet())}. Recall that if the new link set traverses failing link or nodes, its carried traffic and occupied link capacities drop to zero, and if not 
	 * they will be the base values in the "no failure state". Also, if the original tree now traverses links that were removed, an exception is thrown
	 * @see #setLinks(Set)
	 * @see #getInitialLinkSet()
	 */
	public void revertToInitialSetOfLinks () { this.setLinks(initialSetLinksWhenWasCreated); }

	/**
	 * <p>Returns {@code true} if the initial set of {@link com.net2plan.interfaces.networkDesign.Link Links} of the tree when it was created is not subject to any failure: traversed links
	 * and {@link com.net2plan.interfaces.networkDesign.Node Nodes} exist (were not removed) and are up. Returns {@code false} otherwise.</p>
	 * @return See description above
	 */
	public boolean isUpTheInitialLinkSet () 
	{
		checkAttachedToNetPlanObject();
		if (!demand.ingressNode.isUp) return false; 
		for (Link e : initialSetLinksWhenWasCreated)
		{
			if (e.netPlan != netPlan) return false;
			if (!e.destinationNode.isUp) return false;
			if (!e.isUp) return false;
		}
		return true;
	}

	void copyFrom (MulticastTree origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");

		this.linkSet.clear (); for (Link e : origin.linkSet) this.linkSet.add((Link) this.netPlan.getPeerElementInThisNetPlan (e));
		this.initialSetLinksWhenWasCreated.clear (); for (Link originLink : origin.initialSetLinksWhenWasCreated) this.initialSetLinksWhenWasCreated.add (this.netPlan.getLinkFromId (originLink.getId ()));  
		this.cache_traversedNodes.clear (); for (Node n : origin.cache_traversedNodes) this.cache_traversedNodes.add((Node) this.netPlan.getPeerElementInThisNetPlan (n));
		this.cache_ingressLinkOfNode.clear (); 
		for (Entry<Node,Link> neOrigin : origin.cache_ingressLinkOfNode.entrySet()) 
		{
			final Node nOrigin = neOrigin.getKey (); final Node nThis = (Node) this.netPlan.getPeerElementInThisNetPlan (nOrigin);
			final Link eOrigin = neOrigin.getValue (); final Link eThis = (eOrigin == null)? (Link) null : (Link) this.netPlan.getPeerElementInThisNetPlan (eOrigin);
			this.cache_ingressLinkOfNode.put(nThis , eThis);
		}
		this.pathToEgressNode.clear ();
		for (Node nOrigin : origin.pathToEgressNode.keySet()) 
		{
			List<Link> originPath = origin.pathToEgressNode.get(nOrigin);
			List<Link> newPath = new LinkedList<Link> (); for (Link originLink : originPath) newPath.add((Link) this.netPlan.getPeerElementInThisNetPlan (originLink));
			this.pathToEgressNode.put((Node) this.netPlan.getPeerElementInThisNetPlan (nOrigin) , newPath);
		}
		this.cache_egressLinksOfNode.clear ();
		for (Node nOrigin : origin.cache_egressLinksOfNode.keySet()) 
		{
			Set<Link> originEgressLinks = origin.cache_egressLinksOfNode.get(nOrigin);
			Set<Link> newEgressLinks = new HashSet<Link> (); for (Link originLink : originEgressLinks) newEgressLinks.add((Link) this.netPlan.getPeerElementInThisNetPlan (originLink));
			this.cache_egressLinksOfNode.put((Node) this.netPlan.getPeerElementInThisNetPlan (nOrigin) , newEgressLinks);
		}
	}


	
	/**
	 * <p>Returns the {@link com.net2plan.interfaces.networkDesign.NetworkLayer NetworkLayer} object this element belongs to.</p>
	 * @return The {@code NetworkLayer}
	 */
	public NetworkLayer getLayer () 
	{ 
		return layer; 
	}

	/**
	 * <p>Returns the {@link com.net2plan.interfaces.networkDesign.SharedRiskGroup SharedRiskGroups} (SRGs) the multicast tree is affected by.</p>
	 * @return The {@code Set} of affecting SRGs, or an empty {@code Set} if none
	 */
	public Set<SharedRiskGroup> getSRGs ()
	{
		checkAttachedToNetPlanObject();
		Set<SharedRiskGroup> res = new HashSet<SharedRiskGroup> ();
		for (Link e : linkSet) res.addAll (e.cache_srgs);
		for (Node n : cache_traversedNodes) res.addAll (n.cache_nodeSRGs);
		return res;
	}

	/**
	 * <p>Returns the tree associated {@link com.net2plan.interfaces.networkDesign.MulticastDemand Multicast Demand}.</p>
	 * @return the associated multicast demand
	 */
	public MulticastDemand getMulticastDemand() 
	{ 
		return demand; 	
	}

	/**
	 * <p>Returns the tree ingress {@link com.net2plan.interfaces.networkDesign.Node Node}, that is, the one of the {@link com.net2plan.interfaces.networkDesign.MulticastDemand Multicast Demand}.</p>
	 * @return The ingress (initial) node
	 */
	public Node getIngressNode() 
	{ 
		return demand.getIngressNode (); 	
	}

	/**
	 * <p>Returns the {@code Set} of egress {@link com.net2plan.interfaces.networkDesign.Node Nodes} of the tree (i.e the ones of the associated multicast demand).</p>
	 * @return The {@code Set} of egress nodes, as an unmodifiable set
	 */
	public Set<Node> getEgressNodes() 
	{ 
		return Collections.unmodifiableSet(pathToEgressNode.keySet()); 	
	}

	/**
	 * <p>Given an egress {@link com.net2plan.interfaces.networkDesign.Node Node}, returns the unique sequence of tree {@link com.net2plan.interfaces.networkDesign.Link Links} from the ingress node to it.</p>
	 * @param egressNode The egress node
	 * @return The sequence of links as an unmodifiable {@code List}, or {@code null} if {@code egressNode} is not an egress node of the tree
	 */
	public List<Link> getSeqLinksToEgressNode(Node egressNode) 
	{ 
		final List<Link> seqLinks = pathToEgressNode.get(egressNode); return (seqLinks == null)? null : Collections.unmodifiableList(seqLinks); 
	}

	/**
	 * <p>Sets the new set of {@link com.net2plan.interfaces.networkDesign.Link Links} of the multicast tree. The set of links must make a valid tree for the associated multicast demand. If the new tree
	 * traverses failing link or nodes, its carried traffic and occupied capacity is set to zero (while the tree traverses failing resources).</p>
	 * @param newLinkSet The new set of links
	 */
	public void setLinks (Set<Link> newLinkSet)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		Map<Node,List<Link>> newPathToEgressNode = netPlan.checkMulticastTreeValidityForDemand (newLinkSet , demand).getFirst();

		/* Remove the old tree trace in the traversed nodes and links */
		final double currentCarriedTrafficIfAllOk = carriedTrafficIfNotFailing;
		final double currentOccupiedCapacityIfAllOk = occupiedLinkCapacityIfNotFailing;
		setCarriedTraffic(0, 0);
		layer.cache_multicastTreesDown.remove(this);
		layer.cache_multicastTreesTravLinkZeroCap.remove(this);
		for (Link e : this.linkSet) 
			e.cache_traversingTrees.remove (this);
		for (Node node : this.cache_traversedNodes)
			node.cache_nodeAssociatedulticastTrees.remove (this);

		/* update the tree information of the object */
		this.linkSet = newLinkSet;
		this.pathToEgressNode = newPathToEgressNode;
		Triple<Set<Node>,Map<Node,Link>,Map<Node,Set<Link>>> caches = updateCaches ();	
		this.cache_traversedNodes = caches.getFirst ();
		this.cache_ingressLinkOfNode = caches.getSecond ();
		this.cache_egressLinksOfNode = caches.getThird ();
		
		/* Update traversed links and nodes caches  */
		boolean treeIsUp = demand.ingressNode.isUp;
		boolean treeIsTravZeroCapLink = false;
		for (Link e : newLinkSet) 
		{ 
			e.cache_traversingTrees.add (this); 
			treeIsUp = (treeIsUp && e.isUp && e.destinationNode.isUp);
			if (e.capacity < Configuration.precisionFactor) treeIsTravZeroCapLink = true;
		}
		for (Node node : cache_traversedNodes)
			node.cache_nodeAssociatedulticastTrees.add (this);
		if (!treeIsUp) layer.cache_multicastTreesDown.add (this);
		if (treeIsTravZeroCapLink) layer.cache_multicastTreesTravLinkZeroCap.add(this);
		setCarriedTraffic(currentCarriedTrafficIfAllOk, currentOccupiedCapacityIfAllOk);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}


	/**
	 * <p>Returns the {@code Set} of {@link com.net2plan.interfaces.networkDesign.Link Links} of the tree</p>
	 * @return The current {@code Set} of links of the tree, as an unmodifiable set
	 * @see #getInitialLinkSet()
	 */
	public Set<Link> getLinkSet() 
	{ 
		return Collections.unmodifiableSet(linkSet); 
	}

	/**
	 * <p>Returns {@code true} if the multicast tree is traversing a {@link com.net2plan.interfaces.networkDesign.Link Link} or {@link com.net2plan.interfaces.networkDesign.Node Node} that is down,
	 * {@code false} otherwise.</p>
	 * @return {@code True} if all the links the tree is traversing are up, {@code false} otherwise
	 */
	public boolean isDown ()
	{
		return layer.cache_multicastTreesDown.contains(this);
	}

	/** Returns true if the tree is traversing a link with zero capacity
	 * @return see above
	 */
	public boolean isTraversingZeroCapLinks () 
	{
		return layer.cache_multicastTreesTravLinkZeroCap.contains(this);
	}
	
	/**
	 * <p>Returns the {@code Set} of {@link com.net2plan.interfaces.networkDesign.Node Nodes} in the tree: those that are the initial or end node of a
	 * {@link com.net2plan.interfaces.networkDesign.Link Link} in the tree.
	 * The number of traversed nodes in a tree equals the number of travsersed links plus one.</p>
	 * @return The {@code Set} of traversed nodes as an unmodifiable set
	 */
	public Set<Node> getNodeSet () 
	{ 
		return Collections.unmodifiableSet(cache_traversedNodes); 
	}

	/**
	 * <p>Returns the input {@link com.net2plan.interfaces.networkDesign.Link Link} in the tree, entering {@link com.net2plan.interfaces.networkDesign.Node Node} {@code n}. If {@code n} is not in the tree
	 * is the ingress node, returns {@code null}.</p>
	 * @param n The tree node
	 * @return The input link of the tree that enters the node. If {@code n} is the tree ingress node or does not belong to the tree, it returns {@code null}
	 */
	public Link getIngressLinkOfNode (Node n) 
	{ 
		return cache_ingressLinkOfNode.get(n);  
	}

	/**
	 * <p>Returns the the {@code Set} of egress links of {@link com.net2plan.interfaces.networkDesign.Node Node} {@code n} in the tree. If the node is not in the tree, returns {@code null}.</p>
	 * @param n The tree node
	 * @return The unmodifiable {@code Set} of links, or {@code null} if {@code n} is not in the tree
	 */
	public Set<Link> getOutputLinkOfNode (Node n) 
	{
		final Set<Link> res = this.cache_egressLinksOfNode.get(n);
		return (res == null)? null : Collections.unmodifiableSet(res);
	}

	/**
	 * <p>Returns the sum of the {@link com.net2plan.interfaces.networkDesign.Link Links} length in kms of the tree.</p>
	 * @return The tree total length in km
	 */
	public double getTreeTotalLengthInKm () 
	{
		checkAttachedToNetPlanObject();
		double accum = 0; for (Link e : getLinkSet()) accum += e.getLengthInKm();
		return accum;
	}


	/**
	 * <p>Returns the length in kms of the longest path from the ingress {@link com.net2plan.interfaces.networkDesign.Node Node} to the egress nodes.</p>
	 * @return The maximum end to end length in km for the tree
	 */
	public double getTreeMaximumPathLengthInKm () 
	{
		checkAttachedToNetPlanObject();
		double max = 0; 
		for (List<Link> seqLinks : pathToEgressNode.values())
		{
			double accum = 0; for (Link e : seqLinks) accum += e.getLengthInKm();
			max = Math.max(max, accum);
		}
		return max;
	}

	/**
	 * <p>Returns the average length in kms among the paths from the ingress {@link com.net2plan.interfaces.networkDesign.Node Node} to each of the egress nodes.</p>
	 * @return The average length in kilometers among the paths from the ingress to egress nodes
	 */
	public double getTreeAveragePathLengthInKm () 
	{
		checkAttachedToNetPlanObject();
		double accum = 0; 
		for (List<Link> seqLinks : pathToEgressNode.values())
			for (Link e : seqLinks) accum += e.getLengthInKm();
		return accum / demand.egressNodes.size();
	}

	/**
	 * <p>Returns the propagation delay in miliseconds of the longest (in terms of propagation delay) path from the ingress {@link com.net2plan.interfaces.networkDesign.Node Node} to the egress
	 * nodes.</p>
	 * @return The maximum end to end propagation delay in miliseconds
	 */
	public double getTreeMaximumPropagationDelayInMs () 
	{
		checkAttachedToNetPlanObject();
		double max = 0; 
		for (List<Link> seqLinks : pathToEgressNode.values())
		{
			double accum = 0; for (Link e : seqLinks) accum += e.getPropagationDelayInMs();
			max = Math.max(max, accum);
		}
		return max;
	}

	/**
	 * <p>Returns the average propagation delay in miliseconds among the paths from the ingress {@link com.net2plan.interfaces.networkDesign.Node Node} to the egress nodes.</p>
	 * @return Te average propagation delay ins ms among the pahs from the ingress node to the egress nodes
	 */
	public double getTreeAveragePropagationDelayInMs () 
	{
		checkAttachedToNetPlanObject();
		double accum = 0; 
		for (List<Link> seqLinks : pathToEgressNode.values())
			for (Link e : seqLinks) accum += e.getPropagationDelayInMs();
		return accum / demand.egressNodes.size();
	}

	/**
	 * <p>Returns the number of hops of the longest path (in number of hops) from the ingress {@link com.net2plan.interfaces.networkDesign.Node Node} to the egress nodes.</p>
	 * @return The maximum end to end number of hops of the tree
	 */
	public int getTreeMaximumPathLengthInHops () 
	{
		checkAttachedToNetPlanObject();
		int max = 0; 
		for (List<Link> seqLinks : pathToEgressNode.values())
			max = Math.max(max, seqLinks.size());
		return max;
	}

	/**
	 * <p>Returns the average number of hops among the paths from the ingress {@link com.net2plan.interfaces.networkDesign.Node Node} to the egress nodes.</p>
	 * @return The average number of hops among the paths from ingress node to egress nodes
	 */
	public double getTreeAveragePathLengthInHops () 
	{
		checkAttachedToNetPlanObject();
		int accum = 0; 
		for (List<Link> seqLinks : pathToEgressNode.values())
			accum += seqLinks.size();
		return ((double) accum) / demand.egressNodes.size();
	}

	/**
	 * <p>Returns the number of {@link com.net2plan.interfaces.networkDesign.Link Links} of the multicast tree.</p>
	 * @return The number of {@code Links}
	 */
	public int getTreeNumberOfLinks () 
	{
		return linkSet.size();
	}

	/**
	 * <p>Returns the multicast tree carried traffic. If the multicast tree is down, the carried traffic and occupied capacities are zero.
	 * To retrieve the multicast tree carried traffic if all the links and nodes where up, use {@link #getCarriedTrafficInNoFailureState() getCarriedTrafficInNoFailureState()}.</p>
	 * @return The carrief traffic by the multicast tree (see description above)
	 */
	public double getCarriedTraffic () 
	{
		return isDown ()? 0.0 : carriedTrafficIfNotFailing;
	}

	/**
	 * <p>Returns the multicast tree current amount of capacity occupied in the traversed {@link com.net2plan.interfaces.networkDesign.Link Links}. If the multicast tree is down, the
	 * occupied links capacities are zero. To retrieve the multicast tree occupied link capacity if all the links and nodes where up, use {@link #getOccupiedLinkCapacityInNoFailureState()}.</p>
	 * @return The occupied link capacity
	 */
	public double getOccupiedLinkCapacity () 
	{
		return isDown ()? 0.0 : occupiedLinkCapacityIfNotFailing;
	}
	
	/**
	 * <p>Returns the multicast tree amount of carried traffic, assuming the tree is traversing no failing link or node. If the tree traverses
	 * faling link/nodes, its carried traffic is automatically set to zero.</p>
	 * @return The carried traffic assuming a no failure state
	 */
	public double getCarriedTrafficInNoFailureState () 
	{
		return carriedTrafficIfNotFailing;
	}

	/**
	 * <p>Returns the multicast tree capacity occupied in the {@link com.net2plan.interfaces.networkDesign.Link Links}, assuming the tree is traversing no failing link or node. If the
	 * tree traverses faling link/nodes, its occupied link capacity is automatically set to zero.</p>
	 * @return The occupied link capacity assuming a no failure state
	 */
	public double getOccupiedLinkCapacityInNoFailureState () 
	{
		return occupiedLinkCapacityIfNotFailing;
	}


	/**
	 * <p>Sets the tree carried traffic and occupied capacity in the traversed {@link com.net2plan.interfaces.networkDesign.Link Links} (typically the same amount), to be applied if the tree
	 * does not traverse any failing links or {@link com.net2plan.interfaces.networkDesign.Node Nodes}. If the tree failing links or nodes, its carried traffic and occupied link capacities
	 * are temporary set to zero. Carried traffic and link capacities must be non-negative.</p>
	 * @param newCarriedTraffic The new carried traffic in the traffic units
	 * @param newOccupiedLinkCapacity The new occupied capacity in the traversed link, in link capacity units
	 */
	public void setCarriedTraffic(double newCarriedTraffic , double newOccupiedLinkCapacity)
	{
		newCarriedTraffic = NetPlan.adjustToTolerance(newCarriedTraffic);
		newOccupiedLinkCapacity = NetPlan.adjustToTolerance(newOccupiedLinkCapacity);

		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if ((newCarriedTraffic < 0) || (newOccupiedLinkCapacity < 0)) throw new Net2PlanException ("Carried traffics and occupied link capacities must be non-negative");
//		final double extraCarriedTraffic = isDown ()? 0.0 : this.carriedTrafficIfNotFailing - newCarriedTraffic;
//		final double extraOccupiedLinkCapacity = isDown ()? 0.0 : this.occupiedLinkCapacityIfNotFailing - newOccupiedLinkCapacity;
		this.carriedTrafficIfNotFailing = newCarriedTraffic;
		this.occupiedLinkCapacityIfNotFailing = newOccupiedLinkCapacity;
//		if (this.isDown()) { this.carriedTraffic = 0; this.occupiedLinkCapacity = 0;  } else { this.carriedTraffic = newCarriedTraffic; this.occupiedLinkCapacity = newOccupiedLinkCapacity; }
		
		/* Update the links, with the carried traffic depending on the link state */
		for (Link link : linkSet)
			link.updateLinkTrafficAndOccupation();
		demand.carriedTraffic = 0; for (MulticastTree t : demand.cache_multicastTrees) demand.carriedTraffic += t.getCarriedTraffic();
		if (demand.coupledUpperLayerLinks != null) 
			for (Link e : demand.coupledUpperLayerLinks.values())
				e.updateCapacityAndZeroCapacityLinksAndRoutesCaches(demand.carriedTraffic);  
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Removes this multicast tree.</p>
	 */
	public void remove ()
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();

		setCarriedTraffic(0, 0);
		netPlan.cache_id2MulticastTreeMap.remove(id);
		NetPlan.removeNetworkElementAndShiftIndexes(layer.multicastTrees , index);
		for (Link link : linkSet) 
			link.cache_traversingTrees.remove(this); 
		for (Node node : cache_traversedNodes) node.cache_nodeAssociatedulticastTrees.remove(this);
		demand.cache_multicastTrees.remove(this);
		layer.cache_multicastTreesDown.remove(this);
		layer.cache_multicastTreesTravLinkZeroCap.remove(this);
        for (String tag : tags) netPlan.cache_taggedElements.get(tag).remove(this);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		removeId();
	}

	
	
	/* From the set of paths and the set of links (are supposed to be vaild paths and associated link set), check
	 * if the */
	private Triple<Set<Node>,Map<Node,Link>,Map<Node,Set<Link>>> updateCaches ()
	{
		Set<Node> traversedNodes = new HashSet<Node> (); 
		Map<Node,Link> ingressLinkOfNode = new HashMap<Node,Link> ();
		Map<Node,Set<Link>> egressLinksOfNode = new HashMap<Node,Set<Link>> ();

		/* update for the ingress node info */
		Node ingressNode = demand.getIngressNode ();
		traversedNodes.add(ingressNode);
		ingressLinkOfNode.put (ingressNode , null);
		egressLinksOfNode.put(ingressNode, new HashSet<Link> ());
		for (List<Link> seqLinks : pathToEgressNode.values())
			for (Link e : seqLinks)
			{
				final Node linkInitNode = e.getOriginNode();
				final Node linkEndNode = e.getDestinationNode();
				final boolean endNodeIsNewNode = traversedNodes.add(linkEndNode);
				if (endNodeIsNewNode)
					egressLinksOfNode.put(linkEndNode , new HashSet<Link> ());
				else
					if (ingressLinkOfNode.get(linkEndNode) != e) throw new RuntimeException ("Bad");
				ingressLinkOfNode.put(linkEndNode , e);
				egressLinksOfNode.get(linkInitNode).add(e);
			}
		
		/* check */
		Set<Link> linkSetFromEgressLinks = new HashSet<Link> ();
		for (Set<Link> links : egressLinksOfNode.values())
			linkSetFromEgressLinks.addAll(links);
		if (!this.linkSet.equals(linkSetFromEgressLinks)) throw new RuntimeException ("Bad");

		Set<Link> linkSetFromIngressLinks = new HashSet<Link> ();
		for (Link link : ingressLinkOfNode.values())
			if (link != null) linkSetFromIngressLinks.add(link);
		if (!this.linkSet.equals(linkSetFromIngressLinks)) throw new RuntimeException ("Bad: linkSet: " + linkSet + ", linkSetFromIngress: " + linkSetFromIngressLinks);
		
		return Triple.of (traversedNodes , ingressLinkOfNode , egressLinksOfNode);
	}

	/* Check the sequence of links is a path in the NetPlan object without loops (no node has two input links) */
	private boolean isLooplessPath (NetPlan netPlan , NetworkLayer layer ,  List<Link> seqLinks)
	{
		if (seqLinks == null) return false;
		if (seqLinks.isEmpty()) return false;
		final Link firstLink = seqLinks.iterator().next();
		final Node pathInitialNode = firstLink.getOriginNode();
		Set<Node> traversedNodes = new HashSet<Node> (); traversedNodes.add(pathInitialNode);
		Node previousLinkOutNode = pathInitialNode;
		for (Link e : seqLinks)
		{
			e.checkAttachedToNetPlanObject(netPlan);
			if (!e.layer.equals(layer)) throw new Net2PlanException ("The link " + e + " does not belong to the multicast tree layer");
			final Node linkInitialNode = e.getOriginNode();
			final Node linkEndNode = e.getDestinationNode();
			if (previousLinkOutNode != linkInitialNode) return false;
			if (traversedNodes.contains(linkEndNode)) return false; // loop
			traversedNodes.add(linkEndNode); 
			previousLinkOutNode = linkEndNode;
		}
		return true;
	}

	/**
	 * <p>Returns a {@code String} representation of the multicast tree.</p>
	 * @return {@code String} representation of the multicast tree
	 */
	public String toString () { return "mt" + index + " (id " + id + ")"; }

	
	void checkCachesConsistency ()
	{
		super.checkCachesConsistency ();

		if (!layer.multicastTrees.contains(this)) throw new RuntimeException ("Bad");
		if (!demand.cache_multicastTrees.contains(this)) throw new RuntimeException ("Bad");
		if (linkSet == null) throw new RuntimeException ("Multicast tree " + this + ", linkSet == null");
		if (initialSetLinksWhenWasCreated == null) throw new RuntimeException ("Multicast Tree  " + this + ", initialSetLinksWhenWasCreated == null");
		netPlan.checkInThisNetPlanAndLayer(linkSet , layer);
		for (Link link : linkSet) if (!link.cache_traversingTrees.contains(this)) throw new RuntimeException ("Bad");
		for (Node node : cache_traversedNodes) if (!node.cache_nodeAssociatedulticastTrees.contains(this)) throw new RuntimeException ("Bad");
		boolean shouldBeUp = true; for (Link e : linkSet) if (!e.isUp) { shouldBeUp = false; break; }
		if (shouldBeUp) for (Node n : cache_traversedNodes) if (!n.isUp) { shouldBeUp = false; break; }
		if (!shouldBeUp != this.isDown()) throw new RuntimeException("Bad");
		if (shouldBeUp)
		{
			if (getCarriedTraffic() != carriedTrafficIfNotFailing) throw new RuntimeException ("Bad");
		}
		else
		{
			if (getCarriedTraffic() != 0) throw new RuntimeException ("Bad");
		}
	}


}
