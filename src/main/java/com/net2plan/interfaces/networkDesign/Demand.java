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
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.jet.math.tdouble.DoublePlusMultSecond;
import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Quadruple;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import java.util.*;

/** <p>This class contains a representation of a unicast demand. Unicast demands are defined by its initial and end node, the network layer they belong to, 
 * and their offered traffic. When the routing in the network layer is the type {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, demands are carried
 * throgh {@link com.net2plan.interfaces.networkDesign.Route Route} objects associated to them. If the routing type is {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}, the demand
 * traffic is carried by defining the forwarding rules (for each demand and link, fraction of the demand traffic from the link initial node, that is carried through it).</p>
 * <p> In multilayer design, a demand in a lower layer can be coupled to a link in the upper layer. Then, the demand carried traffic is equal to the
 * upper layer link capacity (thus, upper link capacity and lower layer demand traffic must be measured in the same units).
 * </p>
 *
 * @see com.net2plan.interfaces.networkDesign.Node
 * @see com.net2plan.interfaces.networkDesign.Route
 * @see com.net2plan.interfaces.networkDesign.Link
 * @author Pablo Pavon-Marino
 */
public class Demand extends NetworkElement
{
	final NetworkLayer layer;
	final Node ingressNode;
	final Node egressNode;
	double offeredTraffic;
	double carriedTraffic;
	RoutingCycleType routingCycleType;
	
	Set<Route> cache_routes;
	Link coupledUpperLayerLink;

	/**
	 * Generates a new Demand.
	 * @param netPlan Design where this demand is attached
	 * @param id Unique Id
	 * @param index Index
	 * @param layer Network Layer where this demand is created
	 * @param ingressNode Ingress node
	 * @param egressNode Egress node
	 * @param offeredTraffic Offered traffic
	 * @param attributes Attributes map
	 */
	Demand (NetPlan netPlan , long id , int index , NetworkLayer layer , Node ingressNode , Node egressNode , double offeredTraffic , AttributeMap attributes)
	{
		super (netPlan , id , index , attributes);
		
		if (ingressNode.equals (egressNode)) throw new Net2PlanException("Self-demands are not allowed");
		if (offeredTraffic < 0) throw new Net2PlanException("Offered traffic must be non-negative");
		
		this.layer = layer;
		this.ingressNode = ingressNode;
		this.egressNode = egressNode;
		this.offeredTraffic = offeredTraffic;
		this.carriedTraffic = 0;
		this.routingCycleType = RoutingCycleType.LOOPLESS;
		this.cache_routes = new LinkedHashSet<Route> ();
		this.coupledUpperLayerLink = null;
	}

	/**
	 * <p>Removes all current information from the current {@code Demand} and copy all the information from the input {@code Demand}</p>.
	 * @param origin Demand to be copied.
	 */
	void copyFrom (Demand origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.offeredTraffic = origin.offeredTraffic;
		this.carriedTraffic = origin.carriedTraffic;
		this.routingCycleType = origin.routingCycleType;
		this.coupledUpperLayerLink = origin.coupledUpperLayerLink == null? null : this.netPlan.getLinkFromId (origin.coupledUpperLayerLink.id);
		this.cache_routes = new LinkedHashSet<Route> ();
		for (Route r : origin.cache_routes) this.cache_routes.add(this.netPlan.getRouteFromId(r.id));
	}


	/**
	 * <p>Returns the routes associated to this demand.</p>
	 * <p><b>Important</b>: If network layer routing type is not {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, an exception is thrown.</p>
	 * @return The set of routes as an unmodifiable set, an empty set if no route exists.
	 * */
	public Set<Route> getRoutes()
	{
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		return Collections.unmodifiableSet(cache_routes);
	}

	/**
	 * <p>Returns the worse case end-to-end propagation time of the demand traffic. If the routing is source routing, this is the worse propagation time
	 * (summing the link latencies) for all the routes carrying traffic. If the routing is hop-by-hop and loopless, the paths followed are computed and 
	 * the worse case propagation time is returned. If the hop-by-hop routing has loops, {@code Double.MAX_VALUE} is returned. In multilayer design, when the
	 * traffic of a demand traverses a link that is coupled to a lower layer demand, the link latency taken is the worse case propagation time
	 * of the underlying demand.</p>
	 * @return The worse case propagation time in miliseconds
	 * */
	public double getWorseCasePropagationTimeInMs ()
	{
		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
		double maxPropTimeInMs = 0;
		if (layer.routingType == RoutingType.SOURCE_ROUTING)
		{
			for (Route r : this.cache_routes)
			{
				double timeInMs = 0; 
				for (Link e : r.seqLinksRealPath) 
					if (e.isCoupled()) 
						timeInMs += e.coupledLowerLayerDemand.getWorseCasePropagationTimeInMs();
					else
						timeInMs += e.getPropagationDelayInMs();
				maxPropTimeInMs = Math.max (maxPropTimeInMs , timeInMs);
			}
		}
		else
		{
			if (this.routingCycleType == RoutingCycleType.CLOSED_CYCLES) return Double.MAX_VALUE;
			if (this.routingCycleType == RoutingCycleType.OPEN_CYCLES) return Double.MAX_VALUE;
			List<Demand> justThisDemand = new LinkedList<Demand> (); justThisDemand.add(this);
			List<Demand> d_p = new LinkedList<Demand> ();
			List<Double> x_p = new LinkedList<Double> ();
			List<List<Link>> pathList = new LinkedList<List<Link>> ();
			GraphUtils.convert_xde2xp(netPlan.nodes, layer.links , justThisDemand , layer.forwardingRules_x_de , d_p, x_p, pathList);
			Iterator<Demand> it_demand = d_p.iterator();
			Iterator<Double> it_xp = x_p.iterator();
			Iterator<List<Link>> it_pathList = pathList.iterator();
			while (it_demand.hasNext())
			{
				final Demand d = it_demand.next();
				final double trafficInPath = it_xp.next();
				final List<Link> seqLinks = it_pathList.next();
				if (trafficInPath > PRECISION_FACTOR)
				{
					double propTimeThisSeqLinks = 0; 
					for (Link e : seqLinks) 
						if (e.isCoupled()) 
							propTimeThisSeqLinks += e.coupledLowerLayerDemand.getWorseCasePropagationTimeInMs();
						else
							propTimeThisSeqLinks += e.getPropagationDelayInMs();
					maxPropTimeInMs = Math.max(propTimeThisSeqLinks , propTimeThisSeqLinks);
				}
			}
		}
		return maxPropTimeInMs;
	}
	
	/**
	 * <p>Returns {@code true} if the traffic of the demand is traversing an oversubscribed link, {@code false} otherwise.</p>
	 * @return {@code true} if the traffic is traversing an oversubscribed link, {@code false} otherwise
	 */
	public boolean isTraversingOversubscribedLinks ()
	{
		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
		if (layer.routingType == RoutingType.SOURCE_ROUTING)
		{
			for (Route r : this.cache_routes)
				for (Link e : r.seqLinksRealPath) 
					if (e.isOversubscribed()) return true;
		}
		else
		{
			if (this.routingCycleType == RoutingCycleType.CLOSED_CYCLES) return true;
			for (Link e : layer.links) 
				if (layer.forwardingRules_x_de.get(index,e.index) > PRECISION_FACTOR) 
					if (e.isOversubscribed()) return true;
		}
		return false;
	}
	
	/**
	 * <p>Returns the {@link com.net2plan.interfaces.networkDesign.NetworkLayer NetworkLayer} object this element is attached to.</p>
	 * @return The NetworkLayer object
	 */
	public NetworkLayer getLayer () 
	{ 
		return layer; 
	}

	/**
	 * <p>If routing is {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, return {@code true} if more than one route is associated to this demand (routes down or with zero carried traffic also count).</p>
	 * <p>If routing is {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}, return {@code true} if a node sends traffic of the demand through more than one output link.</p>
	 * @return {@code true} if bifurcated, false otherwise
	 */
	public boolean isBifurcated ()
	{
		if (layer.routingType == RoutingType.SOURCE_ROUTING)
			return this.cache_routes.size () >= 2;
		for (Node node : netPlan.nodes)
		{
			int numOutLinksCarryingTraffic = 0; for (Link e : node.getOutgoingLinks(layer)) if (layer.forwardingRules_x_de.get(index,e.index) > 0) numOutLinksCarryingTraffic ++;
			if (numOutLinksCarryingTraffic > 1) return true;
		}
		return false;
	}

	/**
	 * <p>Returns {@code true} if the carried traffic is strictly less than the offered traffic, up to the Net2Plan-wide precision factor.</p>
	 * @return {@code true} if blocked, false otherwise
	 * @see com.net2plan.interfaces.networkDesign.Configuration
	 */
	public boolean isBlocked ()
	{
		return this.carriedTraffic + Configuration.precisionFactor < offeredTraffic;
	}

	/**
	 * <p>Returns {@code true} if the demand is coupled to a link of an upper layer.</p>
	 * @return {@code true} is coupled, false otherwise
	 */
	public boolean isCoupled ()
	{
		return coupledUpperLayerLink != null;
	}

	/**
	 * Returns the offered traffic of the demand
	 * @return The offered traffic
	 */
	public double getOfferedTraffic()
	{
		return offeredTraffic;
	}

	/**
	 * <p>Returns the non zero forwarding rules as a map of pairs demand-link, and its associated splitting factor (between 0 and 1).</p>
	 * <p><b>Important: </b> If routing type is set to {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING} an exception will be thrown.
	 * @return a map with the forwarding rules. The map is empty if no non-zero forwarding rules are defined.
	 */
	public Map<Pair<Demand,Link>,Double> getForwardingRules ()
	{
		checkAttachedToNetPlanObject ();
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);

		Map<Pair<Demand,Link>,Double> res = new HashMap<Pair<Demand,Link>,Double> ();
		IntArrayList es = new IntArrayList (); DoubleArrayList vals = new DoubleArrayList ();
		layer.forwardingRules_f_de.viewRow (index). getNonZeros(es,vals);
		for (int cont = 0 ; cont < es.size () ; cont ++)
			res.put (Pair.of (this, layer.links.get(es.get(cont))) , vals.get(cont));
		return res;
	}

	
	/**
	 * <p>Returns the carried traffic of the demand.</p>
	 * @return The demand carried traffic
	 */
	public double getCarriedTraffic()
	{
		return carriedTraffic;
	}

	/**
	 * <p>Returns the blocked traffic of the demand (offered minus carried, or 0 if carried is above offered).</p>
	 * @return The demand blocked traffic
	 */
	public double getBlockedTraffic()
	{
		return Math.max (0 , offeredTraffic - carriedTraffic);
	}

	/**
	 * <p>Returns the routing cycle type of the demand, indicating if the traffic is routed in a loopless form, with open loops or with closed
	 * loops (the latter can only happen in {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING} routing).</p>
	 * @return The demand routing cycle type
	 * @see com.net2plan.utils.Constants.RoutingCycleType
	 */

	public RoutingCycleType getRoutingCycleType()
	{
		checkAttachedToNetPlanObject ();
		if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING)
			return routingCycleType;
		else
			for (Route r : cache_routes) if (r.hasLoops()) return RoutingCycleType.OPEN_CYCLES;
		return RoutingCycleType.LOOPLESS;
	}


	/**
	 * <p>Returns the demand ingress node.</p>
	 * @return The ingress node
	 */
	public Node getIngressNode()
	{
		return ingressNode;
	}


	/**
	 * <p>Returns the demand egress node.</p>
	 * @return The egress node
	 */
	public Node getEgressNode()
	{
		return egressNode;
	}
	
	/**
	 * <p>If this demand was added using the method {@link com.net2plan.interfaces.networkDesign.NetPlan#addDemandBidirectional(Node, Node, double, Map, NetworkLayer...) addDemandBidirectional()},
	 * returns the demand in the other direction (if it was not previously removed). Returns null otherwise. </p>
	 * @return the demand in the other direction, if this demand was created with {@link com.net2plan.interfaces.networkDesign.NetPlan#addDemandBidirectional(Node, Node, double, Map, NetworkLayer...) addDemandBidirectional()}, or {@code null} otherwise
	 * @see com.net2plan.interfaces.networkDesign.NetPlan#addDemandBidirectional(Node, Node, double, Map, NetworkLayer...)
	 */
	public Demand getBidirectionalPair()
	{
		checkAttachedToNetPlanObject();
		return (Demand) netPlan.getNetworkElementByAttribute(layer.demands , netPlan.KEY_STRING_BIDIRECTIONALCOUPLE, "" + this.id);
	}

	/**
	 * <p>Returns the link this demand is coupled to.</p>
	 * @return the coupled link, or {@code null} if the demand is not coupled
	 */
	public Link getCoupledLink ()
	{
		checkAttachedToNetPlanObject();
		return coupledUpperLayerLink;
	}

	/**
	 * <p>Couples this demand to a link in an upper layer. After the link is coupled, its capacity will be always equal to the demand carried traffic.</p>
	 *
	 * <p><b>Important:</b> The link and the demand must belong to different layers, have same end nodes, not be already coupled,
	 * and the traffic units of the demand must be the same as the capacity units of the link. Also, the coupling of layers cannot create a loop.</p>
	 * @param link Link to be coupled
	 */
	public void coupleToUpperLayerLink (Link link)
	{
		netPlan.checkIsModifiable();
		checkAttachedToNetPlanObject();
		link.checkAttachedToNetPlanObject(this.netPlan);
		
		final NetworkLayer upperLayer = link.layer;
		final NetworkLayer lowerLayer = this.layer;
		
		if (!link.originNode.equals(this.ingressNode)) throw new Net2PlanException("Link and demand to couple must have the same end nodes");
		if (!link.destinationNode.equals(this.egressNode)) throw new Net2PlanException("Link and demand to couple must have the same end nodes");
		if (upperLayer.equals(lowerLayer)) throw new Net2PlanException("Bad - Trying to couple links and demands in the same layer");
		if (!upperLayer.linkCapacityUnitsName.equals(lowerLayer.demandTrafficUnitsName))
			throw new Net2PlanException(String.format(netPlan.TEMPLATE_CROSS_LAYER_UNITS_NOT_MATCHING, upperLayer.id, upperLayer.linkCapacityUnitsName, lowerLayer.id, lowerLayer.demandTrafficUnitsName));
		if ((link.coupledLowerLayerDemand!= null) || (link.coupledLowerLayerMulticastDemand != null)) throw new Net2PlanException("Link " + link + " at layer " + upperLayer.id + " is already associated to a lower layer demand");
		if (this.coupledUpperLayerLink != null) throw new Net2PlanException("Demand " + this.id + " at layer " + lowerLayer.id + " is already associated to an upper layer demand");
		
		DemandLinkMapping coupling_thisLayerPair = netPlan.interLayerCoupling.getEdge(lowerLayer, upperLayer);
		if (coupling_thisLayerPair == null)
		{
			coupling_thisLayerPair = new DemandLinkMapping();
			boolean valid;
			try { valid = netPlan.interLayerCoupling.addDagEdge(lowerLayer, upperLayer, coupling_thisLayerPair); }
			catch (DirectedAcyclicGraph.CycleFoundException ex) { valid = false; }
			if (!valid) throw new Net2PlanException("Coupling between link " + link + " at layer " + upperLayer + " and demand " + this.id + " at layer " + lowerLayer.id + " would induce a cycle between layers");
		}

		/* Link capacity at the upper layer is equal to the carried traffic at the lower layer */
		link.capacity = carriedTraffic;
		link.coupledLowerLayerDemand = this;
		this.coupledUpperLayerLink = link;
		link.layer.cache_coupledLinks.add (link);
		this.layer.cache_coupledDemands.add (this);
		coupling_thisLayerPair.put(this, link);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Creates a new link in the given layer with same end nodes, and couples it to this demand.</p>
	 * @param newLinkLayer Layer where the link will be created.
	 * @return The new created link
	 */
	public Link coupleToNewLinkCreated (NetworkLayer newLinkLayer)
	{
		netPlan.checkIsModifiable();
		checkAttachedToNetPlanObject();
		newLinkLayer.checkAttachedToNetPlanObject(this.netPlan);
		if (this.layer.equals (newLinkLayer)) throw new Net2PlanException ("Cannot couple a link and a demand in the same layer");
		if (isCoupled()) throw new Net2PlanException ("The demand is already coupled");
		
		Link newLink = null;
		try
		{
			newLink = netPlan.addLink(ingressNode , egressNode , carriedTraffic , netPlan.getNodePairEuclideanDistance(ingressNode , egressNode) , 200000 , null , newLinkLayer);
			coupleToUpperLayerLink(newLink);
		} catch (Exception e) { if (newLink != null) newLink.remove (); throw e; }
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		return newLink;
	}
	
	/**
	 * <p>Decouples this demand from an other layer link. Throws an exception if the demand is not currently coupled.</p>
	 */
	public void decouple()
	{
		netPlan.checkIsModifiable();
		checkAttachedToNetPlanObject();
		if (coupledUpperLayerLink == null) throw new Net2PlanException ("Demand is not coupled to a link");
		
		Link link = this.coupledUpperLayerLink;
		link.checkAttachedToNetPlanObject(netPlan);
		if (link.coupledLowerLayerDemand != this) throw new RuntimeException ("Bad");
		final NetworkLayer upperLayer = link.layer;
		final NetworkLayer lowerLayer = this.layer;

		link.coupledLowerLayerDemand = null;
		this.coupledUpperLayerLink = null;
		link.layer.cache_coupledLinks.remove (link);
		this.layer.cache_coupledDemands.remove(this);
		DemandLinkMapping coupling_thisLayerPair = netPlan.interLayerCoupling.getEdge(lowerLayer, upperLayer);
		coupling_thisLayerPair.remove(this);
		if (coupling_thisLayerPair.isEmpty()) netPlan.interLayerCoupling.removeEdge(lowerLayer , upperLayer);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();

	}
	
	/**
	 * <p>Removes all forwarding rules associated to the demand.</p>
	 * <p><b>Important: </b>If routing type is not {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING} and exception is thrown.</p>
	 */
	public void removeAllForwardingRules()
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		layer.forwardingRules_f_de.viewRow (this.index).assign(0);
		layer.updateHopByHopRoutingDemand(this);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Returns the set of demand routes with shortest path (and its cost), using the cost per link array provided. If more than one shortest route exists, all of them are provided.
	 * If the cost vector provided is null, all links have cost one. If the route traverses a protection segment, the cost of its links is summed.</p>
	 * @param costs Costs for each link
	 * @return Pair where the first element is a set of routes (may be empty) and the second element the minimum cost (may be {@code Double.MAX_VALUE} if there is no shortest path)
	 */
	public Pair<Set<Route>,Double> computeShortestPathRoutes (double [] costs)
	{
		if (costs == null) costs = DoubleUtils.ones(layer.links.size ()); else if (costs.length != layer.links.size()) throw new Net2PlanException ("The array of costs must have the same length as the number of links in the layer");
		Set<Route> shortestRoutes = new HashSet<Route> ();
		double shortestPathCost = Double.MAX_VALUE;
		for (Route r : cache_routes)
		{
			double cost = 0; for (Link link : r.seqLinksRealPath) cost += costs [link.index];
			if (cost < shortestPathCost) { shortestPathCost = cost; shortestRoutes.clear(); shortestRoutes.add (r); }
		}
		return Pair.of(shortestRoutes , shortestPathCost);
	}


	/**
	 * <p>Removes a demand, and any associated routes or forwarding rules.</p>
	 */
	public void remove()
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (this.coupledUpperLayerLink != null) this.decouple();
		
		if (layer.routingType == RoutingType.SOURCE_ROUTING)
			for (Route route : new HashSet<Route> (cache_routes)) route.remove();
		else
		{
			final int E = layer.links.size ();
			layer.forwardingRules_f_de = DoubleFactory2D.sparse.appendRows(layer.forwardingRules_f_de.viewPart(0, 0, index, E), layer.forwardingRules_f_de.viewPart(index + 1, 0, layer.demands.size() - index - 1, E));
			DoubleMatrix1D x_e = layer.forwardingRules_x_de.viewRow (index).copy ();
			layer.forwardingRules_x_de = DoubleFactory2D.sparse.appendRows(layer.forwardingRules_x_de.viewPart(0, 0, index, E), layer.forwardingRules_x_de.viewPart(index + 1, 0, layer.demands.size() - index - 1, E));
			for (Link link : layer.links) { link.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments -= x_e.get(link.index); link.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments -= x_e.get(link.index); }
		}
		
		netPlan.cache_id2DemandMap.remove(id);
		NetPlan.removeNetworkElementAndShiftIndexes (layer.demands , index);
		ingressNode.cache_nodeOutgoingDemands.remove (this);
		egressNode.cache_nodeIncomingDemands.remove (this);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		removeId();
	}
	
	/**
	 * <p>Sets the offered traffic by a demand. If routing type is {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}, the carried traffic for each link is updated according to the forwarding rules.
	 * In {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, the routes carried traffic is not updated. </p>
	 * @param offeredTraffic The new offered traffic (must be non-negative)
	 */
	public void setOfferedTraffic(double offeredTraffic)
	{
		offeredTraffic = NetPlan.adjustToTolerance(offeredTraffic);
		netPlan.checkIsModifiable();
		if (offeredTraffic < 0) throw new Net2PlanException("Offered traffic must be greater or equal than zero");
		this.offeredTraffic = offeredTraffic;
		if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING) layer.updateHopByHopRoutingDemand(this);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	
	/**
	 * <p>Returns a {@code String} representation of the demand.</p>
	 * @return {@code String} representation of the demand
	 */
	@Override
	public String toString () { return "d" + index + " (id " + id + ")"; }
	
	/**
	 * <p>Computes the fundamental matrix of the absorbing Markov chain in the current hop-by-hop routing, for the
	 * given demand.</p>
	 * <p>Returns a {@link com.net2plan.utils.Quadruple Quadruple} object where:</p>
	 * <ol type="i">
	 *     <li>the fundamental matrix (NxN, where N is the number of nodes)</li>
	 *     <li>the type of routing of the demand (loopless, with open cycles or with close cycles)</li>
	 *     <li>the fraction of demand traffic that arrives to the destination node and is absorbed there (it may be less than one if the routing has cycles that involve the destination node)</li>
	 *     <li>an array with fraction of the traffic that arrives to each node, that is dropped (this happens only in nodes different to the destination, when the sum of the forwarding percentages on the outupt links is less than one)</li>
	 * </ol>
	 * <p><b>Important: </b>If the routing type is not {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}, an exception is thrown.</p>
	 * @return See description above
	 */
	public Quadruple<DoubleMatrix2D, RoutingCycleType,  Double , DoubleMatrix1D> computeRoutingFundamentalMatrixDemand ()
	{
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);

		final int N = netPlan.nodes.size ();
		DoubleMatrix1D f_e = layer.forwardingRules_f_de.viewRow(index);
		/* q_n1n2 is the fraction of the traffic in n1 that is routed to n2 */
		DoubleMatrix2D q_nn = layer.forwardingRules_Aout_ne.zMult(DoubleFactory2D.sparse.diagonal(f_e) , null);
		q_nn = q_nn.zMult(layer.forwardingRules_Ain_ne , null , 1 , 0 , false , true);
		DoubleMatrix1D sumOut_n = q_nn.zMult(DoubleFactory1D.dense.make(N , 1.0) , null); // sum of the rules outgoing from the node
		DoubleMatrix1D i_n = DoubleFactory1D.dense.make (N); // dropped
		double s_n = -1;
		for (int n = 0 ; n < N ; n ++)
		{
			if ((sumOut_n.get(n) < -1E-5) || (sumOut_n.get(n) > 1+1E-5)) throw new RuntimeException ("Bad");
			if (n == egressNode.index)
				s_n = 1 - sumOut_n.get(n);
			else
				i_n.set (n , 1 - sumOut_n.get(n));
		}
		DoubleMatrix2D iMinusQ = DoubleFactory2D.dense.identity(N);
		iMinusQ.assign(q_nn , DoublePlusMultSecond.minusMult(1));
		DoubleMatrix2D M;
		try { M = new DenseDoubleAlgebra().inverse(iMinusQ); }
		catch(IllegalArgumentException e) { return Quadruple.of (null , RoutingCycleType.CLOSED_CYCLES , s_n , i_n) ; }

		for (int contN = 0; contN < N; contN++)
			if (Math.abs(M.get(contN, contN) - 1) > 1e-5)
				return Quadruple.of(M, RoutingCycleType.OPEN_CYCLES , s_n , i_n);
		
		return Quadruple.of(M, RoutingCycleType.LOOPLESS , s_n , i_n);
	}

	/**
	 * <p>Checks the consistency of the cache. If it is incosistent, throws a {@code RuntimeException} </p>
	 */
	void checkCachesConsistency ()
	{
		double check_carriedTraffic = 0;
		if (layer.routingType == RoutingType.SOURCE_ROUTING)
		{
			for (Route r : cache_routes)
			{
				if (!r.demand.equals (this)) throw new RuntimeException ("Bad");
				check_carriedTraffic += r.carriedTraffic;
			}
		}
		else
		{
			for (Link e : egressNode.getIncomingLinks(layer)) check_carriedTraffic += layer.forwardingRules_x_de.get(index,e.index);
			for (Link e : egressNode.getOutgoingLinks(layer)) check_carriedTraffic -= layer.forwardingRules_x_de.get(index,e.index);
		}
		if (Math.abs(carriedTraffic - check_carriedTraffic) > 1e-3) throw new RuntimeException ("Bad, carriedTraffic: " + carriedTraffic + ", check_carriedTraffic: " + check_carriedTraffic);
		if (coupledUpperLayerLink != null)
			if (!coupledUpperLayerLink.coupledLowerLayerDemand.equals (this)) throw new RuntimeException ("Bad");
		if (!layer.demands.contains(this)) throw new RuntimeException ("Bad");
	}
	
}
