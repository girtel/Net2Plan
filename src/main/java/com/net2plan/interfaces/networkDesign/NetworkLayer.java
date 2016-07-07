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

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.net2plan.internal.AttributeMap;
import com.net2plan.libraries.GraphUtils.ClosedCycleRoutingException;
import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Quadruple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** <p>This class contains a representation of a network layer. This is an structure which contains a set of demands, multicast demands and links. 
 * It also is characterized by a routing type, which can be {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}, or
 * {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}. In layers with source routing, the
 * traffic is carried through routes and protection segments. In layers based on hop-by-hop routing, the traffic is forwarded as mandated by forwarding rules defined.
 * A layer can have a description message and a name, as well as attributes. The capacity of the links and the traffic of the demands can be measured 
 * in different units. Note that in this schema, while links are associated to a single layer, nodes are not associated to layers, since they can have 
 * input and output links at different layers. </p>
 * 
 * @author Pablo Pavon-Marino
 * @since 0.4.0 */
public class NetworkLayer extends NetworkElement
{
	String demandTrafficUnitsName;
	String description;
	String name;
	String linkCapacityUnitsName;
	RoutingType routingType;
	
	ArrayList<Link> links;
	ArrayList<Demand> demands;
	ArrayList<MulticastDemand> multicastDemands;
	ArrayList<Route> routes;
	ArrayList<MulticastTree> multicastTrees;
	ArrayList<ProtectionSegment> protectionSegments;

	DoubleMatrix2D forwardingRules_f_de; // splitting ratios
	DoubleMatrix2D forwardingRules_x_de; // carried traffics (both routings)
	DoubleMatrix2D forwardingRules_Aout_ne; // 1 if link e is outgoing from n, 0 otherwise
	DoubleMatrix2D forwardingRules_Ain_ne; // 1 if link e is incominng from n, 0 otherwise
	Set<Link> cache_linksDown;
	Set<Link> cache_coupledLinks;
	Set<Demand> cache_coupledDemands;
	Set<MulticastDemand> cache_coupledMulticastDemands;
	
	Set<Route> cache_routesDown;
	Set<MulticastTree> cache_multicastTreesDown;
	Set<ProtectionSegment> cache_segmentsDown;

	NetworkLayer(NetPlan netPlan, long id, int index , String demandTrafficUnitsName, String description, String name, String linkCapacityUnitsName, AttributeMap attributes)
	{
		super(netPlan, id , index , attributes);
		this.demandTrafficUnitsName = (demandTrafficUnitsName == null)? "" : demandTrafficUnitsName;
		this.description =  (description == null)? "" : description;
		this.name =  (name == null)? "" : name;
		this.linkCapacityUnitsName =  (linkCapacityUnitsName == null)? "" : linkCapacityUnitsName;
		this.routingType = RoutingType.SOURCE_ROUTING;

		this.links = new ArrayList<Link> ();
		this.demands = new ArrayList<Demand> ();
		this.multicastDemands = new ArrayList<MulticastDemand> ();
		this.routes = new ArrayList<Route> ();
		this.multicastTrees = new ArrayList<MulticastTree> ();
		this.protectionSegments = new ArrayList<ProtectionSegment> ();

		this.cache_linksDown = new HashSet<Link> ();
		this.cache_coupledLinks = new HashSet<Link> ();
		this.cache_coupledDemands = new HashSet<Demand> ();
		this.cache_coupledMulticastDemands = new HashSet<MulticastDemand> ();

		this.cache_routesDown = new HashSet<Route> ();
		this.cache_multicastTreesDown = new HashSet<MulticastTree> ();
		this.cache_segmentsDown = new HashSet<ProtectionSegment> ();
		this.forwardingRules_f_de = null;
		this.forwardingRules_x_de = null;
		this.forwardingRules_Aout_ne = null;
		this.forwardingRules_Ain_ne = null;
	}

	void copyFrom (NetworkLayer origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");

		this.demandTrafficUnitsName = origin.demandTrafficUnitsName;
		this.description = origin.description;
		this.name = origin.name;
		this.linkCapacityUnitsName = origin.linkCapacityUnitsName;
		this.routingType = origin.routingType;
//		for (Link e : origin.links) this.links.add((Link) this.netPlan.getPeerElementInThisNetPlan (e));
//		for (Demand d : origin.demands) this.demands.add((Demand) this.netPlan.getPeerElementInThisNetPlan (d));
//		for (MulticastDemand d : origin.multicastDemands) this.multicastDemands.add((MulticastDemand) this.netPlan.getPeerElementInThisNetPlan (d));
//		for (Route r : origin.routes) this.routes.add((Route) this.netPlan.getPeerElementInThisNetPlan (r));
//		for (MulticastTree t : origin.multicastTrees) this.multicastTrees.add((MulticastTree) this.netPlan.getPeerElementInThisNetPlan (t));
//		for (ProtectionSegment s : origin.protectionSegments) this.protectionSegments.add((ProtectionSegment) this.netPlan.getPeerElementInThisNetPlan (s));
		if (origin.routingType == routingType.HOP_BY_HOP_ROUTING)
		{
			this.forwardingRules_f_de = origin.forwardingRules_f_de.copy();
			this.forwardingRules_x_de = origin.forwardingRules_x_de.copy();
			this.forwardingRules_Aout_ne = origin.forwardingRules_Aout_ne.copy();
			this.forwardingRules_Ain_ne = origin.forwardingRules_Ain_ne.copy();
		}
		else
		{
			this.forwardingRules_f_de = null;
			this.forwardingRules_x_de = null;
			this.forwardingRules_Aout_ne = null;
			this.forwardingRules_Ain_ne = null;
		}
		
		this.cache_linksDown.clear (); for (Link e : origin.cache_linksDown) this.cache_linksDown.add(this.netPlan.getLinkFromId (e.id));
		this.cache_coupledLinks.clear (); for (Link e : origin.cache_coupledLinks) this.cache_coupledLinks.add(this.netPlan.getLinkFromId (e.id));
		this.cache_coupledDemands.clear (); for (Demand d : origin.cache_coupledDemands) this.cache_coupledDemands.add(this.netPlan.getDemandFromId (d.id));
		this.cache_coupledMulticastDemands.clear (); for (MulticastDemand d : origin.cache_coupledMulticastDemands) this.cache_coupledMulticastDemands.add(this.netPlan.getMulticastDemandFromId(d.id));
		this.cache_routesDown.clear (); for (Route r : origin.cache_routesDown) this.cache_routesDown.add(this.netPlan.getRouteFromId (r.id));
		this.cache_multicastTreesDown.clear (); for (MulticastTree t : origin.cache_multicastTreesDown) this.cache_multicastTreesDown.add(this.netPlan.getMulticastTreeFromId (t.id));
		this.cache_segmentsDown.clear (); for (ProtectionSegment s : origin.cache_segmentsDown) this.cache_segmentsDown.add(this.netPlan.getProtectionSegmentFromId(s.id));
		
		for (Link e : origin.links) this.links.get(e.index).copyFrom(e);
		for (Demand d : origin.demands) this.demands.get(d.index).copyFrom(d);
		for (MulticastDemand d : origin.multicastDemands) this.multicastDemands.get(d.index).copyFrom(d);
		for (Route r : origin.routes) this.routes.get(r.index).copyFrom(r);
		for (MulticastTree t : origin.multicastTrees) this.multicastTrees.get(t.index).copyFrom(t);
		for (ProtectionSegment s : origin.protectionSegments) this.protectionSegments.get(s.index).copyFrom(s);
	}
	
	
//	/**
//	 * Returns the name of the units in which the offered traffic is measured (e.g. "Gbps")
//	 * @since 0.4.0
//	 */
//	public String getDemandTrafficUnitsName() 
//	{
//		return demandTrafficUnitsName;
//	}
//
	/**
	 * <p>Returns the user-defined layer description</p>
	 * @return The layer description
	 */
	public String getDescription() 
	{
		return description;
	}

	/**
	 * <p>Sets the user-defined layer description</p>
	 * @param description The description message
	 */
	public void setDescription(String description) 
	{
		this.description = description;
	}


	/**
	 * <p>Returns the layer name. It does not have to be unique among layers</p>
	 * @return  The layer name
	 */
	public String getName() 
	{
		return name;
	}

	/**
	 * <p>Sets the layer name. It does not have to be unique among layers.</p>
	 * @param name New layer name
	 */
	public void setName(String name) 
	{
		this.name = name;
	}

	/**
	 * Checks whether routing type is the expected one. When negative, an exception will be thrown.
	 * @param routingType Expected {@link com.net2plan.utils.Constants.RoutingType RoutingType}
	 */
	public void checkRoutingType(RoutingType routingType)
	{
		if (this.routingType != routingType) throw new Net2PlanException("Routing type of layer " + this + " must be " + routingType);
	}

	/* Updates all the network state, to the new situation where the hop-by-hop routing of a demand has changed */
	void updateHopByHopRoutingDemand (Demand demand)
	{
		NetworkLayer layer = demand.layer;
		if (layer != this) throw new RuntimeException ("Bad");

//		System.out.println ("updateHopByHopRoutingDemand demand: " + demand + ", cache links down: " + cache_linksDown);

		/* set 0 in the down links and the link in-out from the down nodes (they do not send traffic) */
		DoubleMatrix1D oldForwardingThisDemand = layer.forwardingRules_f_de.viewRow(demand.index).copy();
		for (Link downLink : cache_linksDown) 
			forwardingRules_f_de.set(demand.index , downLink.index , 0);
		for (Node downNode : netPlan.cache_nodesDown)
		{
			for (Link downLink : downNode.cache_nodeIncomingLinks) 
				if (downLink.layer.equals (this))
					forwardingRules_f_de.set(demand.index , downLink.index , 0);
			for (Link downLink : downNode.cache_nodeOutgoingLinks) 
				if (downLink.layer.equals (this)) 
					forwardingRules_f_de.set(demand.index , downLink.index , 0);
		}

//		System.out.println ("updateHopByHopRoutingDemand demand: " + demand + ", forwardingthisdemand_e: " + forwardingRules_f_de.viewRow(demand.index));

		Quadruple<DoubleMatrix2D, RoutingCycleType , Double , DoubleMatrix1D> fundMatrixComputation = demand.computeRoutingFundamentalMatrixDemand ();
		DoubleMatrix2D M = fundMatrixComputation.getFirst ();
		double s_egressNode = fundMatrixComputation.getThird();
		DoubleMatrix1D dropFraction_n = fundMatrixComputation.getFourth();

		/* update the demand routing cycle information */
		demand.routingCycleType = fundMatrixComputation.getSecond();
		if (demand.routingCycleType == RoutingCycleType.CLOSED_CYCLES) { forwardingRules_f_de.viewRow(demand.index).assign(oldForwardingThisDemand); throw new ClosedCycleRoutingException("Closed routing cycle for demand " + demand); }
		
		/* update the demand carried traffic */
		demand.carriedTraffic = demand.offeredTraffic * M.get(demand.ingressNode.index , demand.egressNode.index) * s_egressNode;
		if (demand.coupledUpperLayerLink != null) demand.coupledUpperLayerLink.capacity = demand.carriedTraffic;

		if (demand.carriedTraffic > demand.offeredTraffic + 1E-5) throw new RuntimeException ("Bad");
		
		/* update the carried traffic of this demand in each link */
		final double h_d = demand.offeredTraffic;
		for (Link link : links)
		{
			final double oldXde = layer.forwardingRules_x_de.get (demand.index , link.index);
			final double newXde = h_d * M.get (demand.ingressNode.index , link.originNode.index) * layer.forwardingRules_f_de.get (demand.index , link.index);
			if (newXde < -1E-5) throw new RuntimeException ("Bad");
			layer.forwardingRules_x_de.set (demand.index , link.index , newXde);
			link.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments += newXde - oldXde; // in hop-by-hop carried traffic is the same as occupied capacity
			link.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments += newXde - oldXde;
			if ((newXde > 1e-3) && (!link.isUp)) throw new RuntimeException ("Bad");
		}

//		System.out.println ("updateHopByHopRoutingDemand demand: " + demand + ", this demand x_e: " + forwardingRules_x_de.viewRow(demand.index));

		/* Put back the original values of FRs in the down links  */
		forwardingRules_f_de.viewRow(demand.index).assign(oldForwardingThisDemand);
	}

	/**
	 * <p>Returns a {@code String} representation of the network layer.</p>
	 * @return {@code String} representation of the network layer
	 */
	public String toString () { return "layer" + index + " (id " + id + ")"; }

	
	void checkCachesConsistency ()
	{
		final int N = netPlan.nodes.size();
		final int E = links.size ();
		final int D = demands.size ();
		if (routingType == RoutingType.HOP_BY_HOP_ROUTING)
		{
			if ((forwardingRules_f_de.rows () != D) || (forwardingRules_f_de.columns () != E)) throw new RuntimeException ("Bad");
			if ((forwardingRules_x_de.rows () != D) || (forwardingRules_x_de.columns () != E)) throw new RuntimeException ("Bad");
			if ((forwardingRules_Aout_ne.rows () != N) || (forwardingRules_Aout_ne.columns () != E)) throw new RuntimeException ("Bad");
			if ((forwardingRules_Ain_ne.rows () != N) || (forwardingRules_Ain_ne.columns () != E)) throw new RuntimeException ("Bad");
			if ((D > 0) && (E > 0))
			{
				DoubleMatrix2D F_dn = forwardingRules_f_de.zMult(forwardingRules_Aout_ne , null , 1 , 0 , false , true);
				if (F_dn.getMaxLocation() [0] > 1 + 1e-5) throw new RuntimeException ("Bad");
			}
		}
		for (Link link : cache_linksDown) if (link.isUp) throw new RuntimeException ("Bad");
		for (Link link : cache_coupledLinks) if ((link.coupledLowerLayerDemand == null) && (link.coupledLowerLayerMulticastDemand == null)) throw new RuntimeException ("Bad");
		for (Demand demand : cache_coupledDemands) if (demand.coupledUpperLayerLink == null) throw new RuntimeException ("Bad");
		for (MulticastDemand demand : cache_coupledMulticastDemands) if (demand.coupledUpperLayerLinks == null) throw new RuntimeException ("Bad");
		for (Route route : cache_routesDown) if (!route.isDown()) throw new RuntimeException ("Bad");
		for (MulticastTree tree : cache_multicastTreesDown) if (!tree.isDown()) throw new RuntimeException ("Bad");
		for (ProtectionSegment segment : cache_segmentsDown) if (!segment.isDown()) throw new RuntimeException ("Bad");
	}


}
