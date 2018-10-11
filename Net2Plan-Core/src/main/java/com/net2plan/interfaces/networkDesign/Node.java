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

import java.awt.geom.Point2D;
import java.net.URL;
import java.util.Collections;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Collectors;

import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.UnmodifiablePoint2D;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;


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
	SortedMap<String,Point2D> mapLayout2NodeXYPositionMap;
	boolean isUp;
	double population;
	String siteName;
	SortedSet<String> planningDomains;
	
	SortedSet<Link> cache_nodeIncomingLinks;
	SortedSet<Link> cache_nodeOutgoingLinks;
	SortedSet<Demand> cache_nodeIncomingDemands;
	SortedSet<Demand> cache_nodeOutgoingDemands;
	SortedSet<MulticastDemand> cache_nodeIncomingMulticastDemands;
	SortedSet<MulticastDemand> cache_nodeOutgoingMulticastDemands;
	SortedSet<SharedRiskGroup> cache_nodeNonDynamicSRGs;
	SortedSet<Route> cache_nodeAssociatedRoutes;
	SortedSet<MulticastTree> cache_nodeAssociatedulticastTrees;
	SortedSet<Resource> cache_nodeResources;
	SortedMap<NetworkLayer,Pair<URL,Double>> mapLayer2URLSpecificIcon;
	
	/**
	 * Default constructor.
	 *
	 * @param netPlan Network topology
	 * @param id Node ID
	 * @param index Node index
	 * @param xCoord Node X Coord
	 * @param yCoord Node Y Coord
	 * @param name Node name
	 * @param attributes Node attributes
	 * @since 0.2.0
	 */
	protected Node (NetPlan netPlan , long id , int index , double xCoord, double yCoord, String name, AttributeMap attributes)
	{
		super (netPlan , id , index , attributes);
		
		this.mapLayout2NodeXYPositionMap = new TreeMap<> ();
                if (netPlan != null)
                    for (String layoutId : netPlan.cache_definedPlotNodeLayouts)
                        this.mapLayout2NodeXYPositionMap.put(layoutId, new UnmodifiablePoint2D(xCoord, yCoord));
                else
                    this.mapLayout2NodeXYPositionMap.put(NetPlan.PLOTLAYTOUT_DEFAULTNODELAYOUTNAME, new UnmodifiablePoint2D(xCoord, yCoord));
                
		this.name = name == null? "" : name;
		this.isUp = true;
		
		this.cache_nodeIncomingLinks = new TreeSet<Link> ();
		this.cache_nodeOutgoingLinks = new TreeSet<Link> ();
		this.cache_nodeIncomingDemands = new TreeSet<Demand> ();
		this.cache_nodeOutgoingDemands = new TreeSet<Demand> ();
		this.cache_nodeIncomingMulticastDemands = new TreeSet<MulticastDemand> ();
		this.cache_nodeOutgoingMulticastDemands = new TreeSet<MulticastDemand> ();
		this.cache_nodeNonDynamicSRGs = new TreeSet<SharedRiskGroup> ();
		this.cache_nodeResources = new TreeSet<Resource> ();
		this.cache_nodeAssociatedRoutes = new TreeSet<Route> ();
		this.cache_nodeAssociatedulticastTrees = new TreeSet<MulticastTree> ();
		this.mapLayer2URLSpecificIcon = new TreeMap <> ();
		this.planningDomains = new TreeSet<> ();
		this.population = 0;
		this.siteName = null;
	}
	
	void copyFrom (Node origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.name = origin.name;
		this.population = origin.population;
		this.siteName = origin.siteName;
		this.mapLayout2NodeXYPositionMap = new TreeMap<> ();
		for (Entry<String,Point2D> entry : origin.mapLayout2NodeXYPositionMap.entrySet())
		    this.mapLayout2NodeXYPositionMap.put(entry.getKey(), new UnmodifiablePoint2D(entry.getValue().getX(), entry.getValue().getY()));
		this.isUp = origin.isUp;
		this.mapLayer2URLSpecificIcon.clear(); for (NetworkLayer l : origin.mapLayer2URLSpecificIcon.keySet()) this.mapLayer2URLSpecificIcon.put(this.netPlan.getNetworkLayerFromId(l.getId()) , origin.mapLayer2URLSpecificIcon.get(l));
		this.cache_nodeIncomingLinks.clear (); for (Link e : origin.cache_nodeIncomingLinks) this.cache_nodeIncomingLinks.add(this.netPlan.getLinkFromId (e.id));
		this.cache_nodeOutgoingLinks.clear (); for (Link e : origin.cache_nodeOutgoingLinks) this.cache_nodeOutgoingLinks.add(this.netPlan.getLinkFromId (e.id));
		this.cache_nodeIncomingDemands.clear (); for (Demand d : origin.cache_nodeIncomingDemands) this.cache_nodeIncomingDemands.add(this.netPlan.getDemandFromId (d.id));
		this.cache_nodeOutgoingDemands.clear (); for (Demand d : origin.cache_nodeOutgoingDemands) this.cache_nodeOutgoingDemands.add(this.netPlan.getDemandFromId (d.id));
		this.cache_nodeIncomingMulticastDemands.clear (); for (MulticastDemand d : origin.cache_nodeIncomingMulticastDemands) this.cache_nodeIncomingMulticastDemands.add(this.netPlan.getMulticastDemandFromId(d.id));
		this.cache_nodeOutgoingMulticastDemands.clear (); for (MulticastDemand d : origin.cache_nodeOutgoingMulticastDemands) this.cache_nodeOutgoingMulticastDemands.add(this.netPlan.getMulticastDemandFromId(d.id));
		this.cache_nodeNonDynamicSRGs.clear (); for (SharedRiskGroup s : origin.cache_nodeNonDynamicSRGs) this.cache_nodeNonDynamicSRGs.add(this.netPlan.getSRGFromId(s.id));
		this.cache_nodeResources.clear(); for (Resource r : origin.cache_nodeResources) this.cache_nodeResources.add(this.netPlan.getResourceFromId(r.id));
		this.cache_nodeAssociatedRoutes.clear (); for (Route r : origin.cache_nodeAssociatedRoutes) this.cache_nodeAssociatedRoutes.add(this.netPlan.getRouteFromId (r.id));
		this.cache_nodeAssociatedulticastTrees.clear (); for (MulticastTree t : origin.cache_nodeAssociatedulticastTrees) this.cache_nodeAssociatedulticastTrees.add(this.netPlan.getMulticastTreeFromId(t.id));
	}
	
	boolean isDeepCopy (Node e2)
	{
		if (!super.isDeepCopy(e2)) return false;
		if (!this.name.equals(e2.name)) return false;
		if (!this.mapLayout2NodeXYPositionMap.equals(e2.mapLayout2NodeXYPositionMap)) return false;
		if (this.isUp != e2.isUp) return false;
		if (this.population != e2.population) return false;
		if ((this.siteName == null) != (e2.siteName == null)) return false;
		if (this.siteName != null) if (!this.siteName.equals(e2.siteName)) return false;
		
		if (!NetPlan.isDeepCopy(this.mapLayer2URLSpecificIcon , e2.mapLayer2URLSpecificIcon)) return false;
		if (!NetPlan.isDeepCopy(this.cache_nodeIncomingLinks , e2.cache_nodeIncomingLinks)) return false;
		if (!NetPlan.isDeepCopy(this.cache_nodeOutgoingLinks , e2.cache_nodeOutgoingLinks)) return false;
		if (!NetPlan.isDeepCopy(this.cache_nodeIncomingDemands , e2.cache_nodeIncomingDemands)) return false;
		if (!NetPlan.isDeepCopy(this.cache_nodeOutgoingDemands , e2.cache_nodeOutgoingDemands)) return false;
		if (!NetPlan.isDeepCopy(this.cache_nodeIncomingMulticastDemands , e2.cache_nodeIncomingMulticastDemands)) return false;
		if (!NetPlan.isDeepCopy(this.cache_nodeOutgoingMulticastDemands , e2.cache_nodeOutgoingMulticastDemands)) return false;
		if (!NetPlan.isDeepCopy(this.cache_nodeNonDynamicSRGs , e2.cache_nodeNonDynamicSRGs)) return false;
		if (!NetPlan.isDeepCopy(this.cache_nodeResources, e2.cache_nodeResources)) return false;
		if (!NetPlan.isDeepCopy(this.cache_nodeAssociatedRoutes , e2.cache_nodeAssociatedRoutes)) return false;
		if (!NetPlan.isDeepCopy(this.cache_nodeAssociatedulticastTrees , e2.cache_nodeAssociatedulticastTrees)) return false;
		return true;
	}
	
	/** Sets the name of the site this node is associated to, removing previous site name information. If site is null, the current site name is
	 * removed, and the node becomes not attached to any site name.
	 * @param site te site name
	 */
	public void setSiteName (String site)
	{
		if  ((site == null) && (this.siteName == null)) return;
		if (site == null) 
		{
			if (this.siteName != null)
			{
				final SortedSet<Node> nodesOldSiteNames = netPlan.cache_nodesPerSiteName.get(this.siteName);
				nodesOldSiteNames.remove(this);
				if (nodesOldSiteNames.isEmpty()) netPlan.cache_nodesPerSiteName.remove(this.siteName);
			}
			this.siteName = null;
		}
		else
		{
			if (site.equals(this.siteName)) return;
			if (this.siteName != null) 
			{
				final SortedSet<Node> nodesOldSiteNames = netPlan.cache_nodesPerSiteName.get(this.siteName);
				nodesOldSiteNames.remove(this);
				if (nodesOldSiteNames.isEmpty()) netPlan.cache_nodesPerSiteName.remove(this.siteName);
			}
			SortedSet<Node> nodesThisNewSiteName = netPlan.cache_nodesPerSiteName.get(site);
			if (nodesThisNewSiteName == null)
			{
				nodesThisNewSiteName = new TreeSet<> ();
				netPlan.cache_nodesPerSiteName.put(site, nodesThisNewSiteName);
			}
			nodesThisNewSiteName.add(this);
			this.siteName = site;
		}
	}

	/** Returns the name of the site this node is associated to (or null if none)
	 * @return see above
	 */
	public String getSiteName () { return this.siteName; }
	
	/** Sets the population of the node (must be non-negative)
	 * @param population the population
	 */
	public void setPopulation (double population)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (population < 0) throw new Net2PlanException ("Node population must be non-negative");
		this.population = population;
		this.name = name == null? "" : name;
	}
	
	/** Returns the node population
	 * @return see above
	 */
	public double getPopulation () { return population; }
	
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


	/** Returns the total carried in the links of te given layer entering the node.
	 * If no layer is provided, the default layer is assumed
	 * @param optionalLayerParameter Network layer (optional)
	 * @return see above
	 */
	public double getIncomingLinksTraffic (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (Link e : cache_nodeIncomingLinks) if (e.layer.equals (layer)) accum += e.getCarriedTraffic();
		return accum;
	}

	/** Returns the total carried in the links of te given layer entering the node.
	 * If no layer is provided, the default layer is assumed
	 * @param optionalLayerParameter Network layer (optional)
	 * @return see above
	 */
	public double getOutgoingLinksTraffic (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (Link e : cache_nodeOutgoingLinks) if (e.layer.equals (layer)) accum += e.getCarriedTraffic();
		return accum;
	}

	/** Returns the total capacity in the links of te given layer entering the node.
	 * If no layer is provided, the default layer is assumed
	 * @param optionalLayerParameter Network layer (optional)
	 * @return see above
	 */
	public double getIncomingLinksCapacity (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (Link e : cache_nodeIncomingLinks) if (e.layer.equals (layer)) accum += e.capacity;
		return accum;
	}

	/** Returns the total capacity in the links of te given layer entering the node.
	 * If no layer is provided, the default layer is assumed
	 * @param optionalLayerParameter Network layer (optional)
	 * @return see above
	 */
	public double getOutgoingLinksCapacity (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (Link e : cache_nodeOutgoingLinks) if (e.layer.equals (layer)) accum += e.capacity;
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
	public SortedSet<Node> getInNeighbors (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		SortedSet<Node> res = new TreeSet<Node> ();
		for (Link e : cache_nodeIncomingLinks) if (e.layer.equals (layer)) res.add (e.originNode);
		return res;
	}

	/**
	 * <p>Returns the nodes directly connected to this, with links initiated in this node at the given layer. If no layer is provided, the default layer is assumed</p>
	 * @param optionalLayerParameter Network layer
	 * @return The set of neighbor nodes
	 */
	public SortedSet<Node> getOutNeighbors (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		SortedSet<Node> res = new TreeSet<Node> ();
		for (Link e : cache_nodeOutgoingLinks) if (e.layer.equals (layer)) res.add (e.destinationNode);
		return res;
	}

	/**
	 * <p>Returns the nodes directly connected to this, with links initiated in this node at any layer.</p>
	 * @return The set of neighbor nodes
	 */
	public SortedSet<Node> getOutNeighborsAllLayers ()
	{
		SortedSet<Node> res = new TreeSet<Node> ();
		for (Link e : cache_nodeOutgoingLinks) res.add (e.destinationNode);
		return res;
	}

	/**
	 * <p>Returns the nodes directly connected to this, with links ending in this node at any layer.</p>
	 * @return The set of neighbor nodes
	 */
	public SortedSet<Node> getInNeighborsAllLayers ()
	{
		SortedSet<Node> res = new TreeSet<Node> ();
		for (Link e : cache_nodeIncomingLinks) res.add (e.originNode);
		return res;
	}

	/**
	 * <p>Returns node position in the map, in the given layout (or the currently active if none) </p>
	 * @param layout  see above
	 * @return The position in the map used for visualization
	 */
	public Point2D getXYPositionMap(String ...layout)
	{
        if (layout.length > 1) throw new Net2PlanException ("At most one layout can be provided");
	    if (layout.length > 0)
	    {
            if (!netPlan.cache_definedPlotNodeLayouts.contains(layout [0])) throw new Net2PlanException ("The layout does not exist");
            return this.mapLayout2NodeXYPositionMap.get(layout [0]);
	    }
		return this.mapLayout2NodeXYPositionMap.get(netPlan.getPlotNodeLayoutCurrentlyActive());
	}

	/**
	 * <p>Sets the node position in the map, in the current layout, used for visualization </p>
	 * @param pos New node position
     * @param layout optionally, we can provide the layout where to set the position
	 */
	public void setXYPositionMap(Point2D pos , String ...layout)
	{
        checkAttachedToNetPlanObject();
	    if (layout.length > 1) throw new Net2PlanException ("At most one layout can be set");
		netPlan.checkIsModifiable();
		if (layout.length == 0)
		    this.mapLayout2NodeXYPositionMap.put(netPlan.getPlotNodeLayoutCurrentlyActive() , new UnmodifiablePoint2D (pos.getX() , pos.getY()));
		else
		{
		    if (!netPlan.cache_definedPlotNodeLayouts.contains(layout [0])) throw new Net2PlanException ("The layout *" + (layout[0]) + "* does not exist. Current layouts: " + netPlan.cache_definedPlotNodeLayouts);
            this.mapLayout2NodeXYPositionMap.put(layout [0] , new UnmodifiablePoint2D (pos.getX() , pos.getY()));
		}
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
	public SortedSet<Link> getIncomingLinks(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		SortedSet<Link> res = new TreeSet<Link> (); for (Link e : cache_nodeIncomingLinks) if (e.layer.equals(layer)) res.add (e);
		return res;
	}

	/** Returns the layers where this node is working: has at least one link, or demand, or multicast demand at the given layer associated to it. 
	 * @return see above
	 */
	public SortedSet<NetworkLayer> getWorkingLayers ()
	{
		final SortedSet<NetworkLayer> res = new TreeSet<>();
		res.addAll (cache_nodeIncomingLinks.stream().map(e->e.getLayer()).collect(Collectors.toSet()));
		res.addAll (cache_nodeOutgoingLinks.stream().map(e->e.getLayer()).collect(Collectors.toSet()));
		res.addAll (cache_nodeIncomingDemands.stream().map(e->e.getLayer()).collect(Collectors.toSet()));
		res.addAll (cache_nodeOutgoingDemands.stream().map(e->e.getLayer()).collect(Collectors.toSet()));
		res.addAll (cache_nodeIncomingMulticastDemands.stream().map(e->e.getLayer()).collect(Collectors.toSet()));
		res.addAll (cache_nodeOutgoingMulticastDemands.stream().map(e->e.getLayer()).collect(Collectors.toSet()));
		return res;
	}

	/** Returns true has at least one link, or demand, or multicast demand at the given layer associated to it.
	 * @param layer Network layer
	 * @return see above
	 */
	public boolean isWorkingAtLayer(NetworkLayer layer)
	{
		if (!getOutgoingLinks(layer).isEmpty()) return true;
		if (!getIncomingLinks(layer).isEmpty()) return true;
		if (!getOutgoingDemands(layer).isEmpty()) return true;
		if (!getIncomingDemands(layer).isEmpty()) return true;
		if (!getOutgoingMulticastDemands(layer).isEmpty()) return true;
		if (!getIncomingMulticastDemands(layer).isEmpty()) return true;
		return false;
	}

	/** Returns true if the node is fully isolated at all layers, with no links nor demands affecting it
	 * @return see above
	 */
	public boolean isIsolated ()
	{
		if (!getOutgoingLinksAllLayers().isEmpty()) return false;
		if (!getIncomingLinksAllLayers().isEmpty()) return false;
		if (!getOutgoingDemandsAllLayers().isEmpty()) return false;
		if (!getIncomingDemandsAllLayers().isEmpty()) return false;
		if (!getOutgoingMulticastDemandsAllLayers().isEmpty()) return false;
		if (!getIncomingMulticastDemandsAllLayers().isEmpty()) return false;
		return true;
	}

	/**
	 * <p>Returns the set of links initiated in the node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The set of outgoing links, or an empty set if none
	 */
	public SortedSet<Link> getOutgoingLinks(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		SortedSet<Link> res = new TreeSet<Link> (); for (Link e : cache_nodeOutgoingLinks) if (e.layer.equals(layer)) res.add (e);
		return res;
	}

	/**
	 * <p>Returns the set of links initiated in the node in all layer.</p>
	 * @return The set of outgoing links, or an empty set if none (as an unmodifiable set)
	 */
	public SortedSet<Link> getOutgoingLinksAllLayers ()
	{
		return Collections.unmodifiableSortedSet(cache_nodeOutgoingLinks);
	}

	/**
	 * <p>Returns the set of links ending in the node at any layer.</p>
	 * @return The set of incoming links, or an empty set if none (as an unmodifiable set)	 */
	public SortedSet<Link> getIncomingLinksAllLayers ()
	{
		return Collections.unmodifiableSortedSet(cache_nodeIncomingLinks);
	}

	/**
	 * <p>Returns the set of demands initiated in the node, in any layer. </p>
	 * @return the set of demands, or an empty set if none (as an unmodifiable set)
	 */
	public SortedSet<Demand> getOutgoingDemandsAllLayers ()
	{
		return Collections.unmodifiableSortedSet(cache_nodeOutgoingDemands);
	}

	/**
	 * <p>Returns the set of demands ending in the node, in any layer. </p>
	 * @return The set of demands, or an empty set if none (as an unmodifiable set)
	 */
	public SortedSet<Demand> getIncomingDemandsAllLayers ()
	{
		return Collections.unmodifiableSortedSet(cache_nodeIncomingDemands);
	}

	/**
	 * <p>Returns the set of multicast demands initiated in the node, in any layer. </p>
	 * @return The set of demands, or an empty set if none (as an unmodifiable set)
	 */
	public SortedSet<MulticastDemand> getOutgoingMulticastDemandsAllLayers ()
	{
		return Collections.unmodifiableSortedSet(cache_nodeOutgoingMulticastDemands);
	}

	/**
	 * <p>Returns the set of multicast demands ending in the node, in any layer.</p>
	 * @return The set of demands, or an empty set if none (as an unmodifiable set)
	 */
	public SortedSet<MulticastDemand> getIncomingMulticastDemandsAllLayers ()
	{
		return Collections.unmodifiableSortedSet(cache_nodeIncomingMulticastDemands);
	}

	/**
	 * <p>Returns the set of demands ending in the node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return the demands, or an empty set if none
	 */
	public SortedSet<Demand> getIncomingDemands(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		SortedSet<Demand> res = new TreeSet<Demand> (); for (Demand e : cache_nodeIncomingDemands) if (e.layer.equals(layer)) res.add (e);
		return res;
	}

	/**
	 * <p>SortedSet the failure state of the node: up or down. Returns the previous failure state. The routing is automatically updated, making the traffic
	 * of the traversing routes as zero, and the hop-by-hop routing is updated as if the forwarding rules of input and output links were zero</p>
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
	public SortedSet<Route> getIncomingRoutes(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		SortedSet<Route> res = new TreeSet<Route> (); for (Demand d : cache_nodeIncomingDemands) if (d.layer.equals(layer)) res.addAll (d.cache_routes);
		return res;
	}

	/**
	 * <p>Returns the set of multicast trees ending in the node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The multicast trees, or an empty set if none
	 */
	public SortedSet<MulticastTree> getIncomingMulticastTrees (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		SortedSet<MulticastTree> res = new TreeSet<MulticastTree> (); for (MulticastDemand e : cache_nodeIncomingMulticastDemands) if (e.layer.equals(layer)) res.addAll (e.cache_multicastTrees);
		return res;
	}

	/**
	 * <p>Returns the set of routes initiated in the node, in the given layer. If no layer is provided, the default layer is assumed. </p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The set of routes, or an empty set if none
	 */
	public SortedSet<Route> getOutgoingRoutes(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		SortedSet<Route> res = new TreeSet<Route> (); for (Demand d : cache_nodeOutgoingDemands) if (d.layer.equals(layer)) res.addAll (d.cache_routes);
		return res;
	}

	/**
	 * <p>Returns the set of multicast tree initiated in the node, in the given layer. If no layer is provided, the default layer is assumed. </p>
	 * @param optionalLayerParameter The layer
	 * @return The multicast trees, or an empty set if none
	 */
	public SortedSet<MulticastTree> getOutgoingMulticastTrees (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		SortedSet<MulticastTree> res = new TreeSet<MulticastTree> (); for (MulticastDemand e : cache_nodeOutgoingMulticastDemands) if (e.layer.equals(layer)) res.addAll (e.cache_multicastTrees);
		return res;
	}


	/**
	 * <p>Returns the set of demands initiated in the node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The demands, or an empty set if none
	 */
	public SortedSet<Demand> getOutgoingDemands(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		SortedSet<Demand> res = new TreeSet<Demand> (); for (Demand e : cache_nodeOutgoingDemands) if (e.layer.equals(layer)) res.add (e);
		return res;
	}

	/**
	 * <p>Returns the set of multicast demands ending in the node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The demands, or an empty set if none
	 */
	public SortedSet<MulticastDemand> getIncomingMulticastDemands(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		SortedSet<MulticastDemand> res = new TreeSet<MulticastDemand> (); for (MulticastDemand e : cache_nodeIncomingMulticastDemands) if (e.layer.equals(layer)) res.add (e);
		return res;
	}

	/**
	 * <p>Returns the set of multicast demands initiated in the node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The demands, or an empty set if none
	 */
	public SortedSet<MulticastDemand> getOutgoingMulticastDemands(NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		SortedSet<MulticastDemand> res = new TreeSet<MulticastDemand> (); for (MulticastDemand e : cache_nodeOutgoingMulticastDemands) if (e.layer.equals(layer)) res.add (e);
		return res;
	}

	/**
	 * <p>Returns the set of routes that start, end or traverse this node, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The routes, or an empty set if none
	 */
	public SortedSet<Route> getAssociatedRoutes (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		SortedSet<Route> res = new TreeSet<Route> (); 
		for (Link e : cache_nodeIncomingLinks) if (e.layer.equals (layer)) res.addAll (e.cache_traversingRoutes.keySet()); 
		for (Link e : cache_nodeOutgoingLinks) if (e.layer.equals (layer)) res.addAll (e.cache_traversingRoutes.keySet()); 
		return res;
	}

    /**
     * <p>Returns the set of routes that traverse this node but not as the origin node of the first link, nor 
     * the ending node of the last link
     * @param optionalLayerParameter Network layer (optional)
     * @return The routes, or an empty set if none
     */
    public SortedSet<Route> getExpressRoutes (NetworkLayer ... optionalLayerParameter)
    {
        checkAttachedToNetPlanObject();
        NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
        SortedSet<Route> res = new TreeSet<Route> (); 
        for (Link e : cache_nodeIncomingLinks)
        {
            if (!e.layer.equals (layer)) continue;
            for (Route r : e.cache_traversingRoutes.keySet())
                if (r.getSeqIntermediateNodes().contains(this))
                    res.add(r);
        }
        for (Link e : cache_nodeOutgoingLinks) 
        {
            if (!e.layer.equals (layer)) continue;
            for (Route r : e.cache_traversingRoutes.keySet())
                if (r.getSeqIntermediateNodes().contains(this))
                    res.add(r);
        }
        return res;
    }


	/**
	 * <p>Returns the set of multicast trees that start, end or traverse this node, in the given layer. If no layer is provided, the default layer is assumed. </p>
	 * @param optionalLayerParameter Network layer (optional
	 * @return The trees, or an empty set if none
	 */
	public SortedSet<MulticastTree> getAssociatedMulticastTrees (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		SortedSet<MulticastTree> res = new TreeSet<MulticastTree> (); 
		for (Link e : cache_nodeIncomingLinks) if (e.layer.equals (layer)) res.addAll (e.cache_traversingTrees); 
		for (Link e : cache_nodeOutgoingLinks) if (e.layer.equals (layer)) res.addAll (e.cache_traversingTrees); 
		return res;
	}

	/**
	 * <p>Returns the set of shared risk groups (SRGs) this node belongs to. </p>
	 * @return The set of SRGs as an unmodifiable set
	 */
	public SortedSet<SharedRiskGroup> getSRGs()
	{
	    final SortedSet<SharedRiskGroup> res = new TreeSet<> (cache_nodeNonDynamicSRGs);
	    for (SharedRiskGroup srg : netPlan.cache_dynamicSrgs)
	        if (srg.getNodes().contains(this)) res.add(srg);
		return res;
	}

	/** Returns the set of resources that this node hosts. If no resource is provided, all the resources of all types are provided. If one ore more optional parameters type are given, 
	 * a set with the hosted resources of any of those types is returned (or an empty set if none)
	 * @param type one or more optional types (optional)
	 * @return the set
	 */
	public SortedSet<Resource> getResources (String ... type)
	{
		if (type.length == 0) return Collections.unmodifiableSortedSet(cache_nodeResources);
		SortedSet<Resource> res = new TreeSet<Resource> ();
		for (Resource r : cache_nodeResources) for (String thisType : type) if (r.type.equals(thisType)) { res.add(r); break; }
		return res;
	}
	
	
	/**
	 * <p>Removes a node, and any associated link, demand, route or forwarding rule.</p>
	 */
	public void remove ()
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();

		for (Resource resource : new LinkedList<Resource> (cache_nodeResources)) resource.remove();
		for (MulticastTree tree : new LinkedList<MulticastTree> (cache_nodeAssociatedulticastTrees)) tree.remove ();
		for (Route route : new LinkedList<Route> (cache_nodeAssociatedRoutes)) route.remove ();
		for (SharedRiskGroup srg : new LinkedList<SharedRiskGroup> (cache_nodeNonDynamicSRGs)) srg.remove ();
		for (Link link : new LinkedList<Link> (cache_nodeIncomingLinks)) link.remove (); 
		for (Link link : new LinkedList<Link> (cache_nodeOutgoingLinks)) link.remove (); 
		for (Demand demand : new LinkedList<Demand> (cache_nodeIncomingDemands)) demand.remove ();
		for (Demand demand : new LinkedList<Demand> (cache_nodeOutgoingDemands)) demand.remove ();
		for (MulticastDemand demand : new LinkedList<MulticastDemand> (cache_nodeIncomingMulticastDemands)) demand.remove ();
		for (MulticastDemand demand : new LinkedList<MulticastDemand> (cache_nodeOutgoingMulticastDemands)) demand.remove ();

		netPlan.cache_id2NodeMap.remove (id);
        for (String tag : tags) netPlan.cache_taggedElements.get(tag).remove(this);
		NetPlan.removeNetworkElementAndShiftIndexes(netPlan.nodes , this.index);
        final NetPlan npOld = this.netPlan;
        removeId();
        if (ErrorHandling.isDebugEnabled()) npOld.checkCachesConsistency();
	}

	/**
	 * <p>Returns a {@code String} representation of the node.</p>
	 * @return {@code String} representation of the node
	 */
	@Override
    public String toString () { return name.equals("")? "n" + index : name; }

	/**
	 * <p>Returns the set of forwarding rules of links initiated in the node of the given layer,
	 * which have a non-zero splitting factor.  If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The map associating the demand-link pair, with the associated splitting factor
	 */
	public SortedMap<Pair<Demand,Link>,Double> getForwardingRules (NetworkLayer ... optionalLayerParameter)
	{
		checkAttachedToNetPlanObject();
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		SortedMap<Pair<Demand,Link>,Double> res = new TreeMap<Pair<Demand,Link>,Double> ();
		for (Link e : getOutgoingLinks(layer))
			for (Entry<Demand,Double> entry : e.cacheHbH_frs.entrySet())
				res.put(Pair.of(entry.getKey(), e), entry.getValue());
		return res;
	}

	/**
	 *  <p>Returns the set of forwarding rules of links initiated in the node and associated to the given demand (the links are then in the same layer
	 * as the demand), that have a non-zero splitting factor</p>
	 * @param demand The demand
	 * @return The map associating the demand-link pair, with the associated splitting factor
	 */
	public SortedMap<Pair<Demand,Link>,Double> getForwardingRules (Demand demand)
	{
		NetworkLayer layer = demand.layer;
		layer.checkAttachedToNetPlanObject(netPlan); 
		demand.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		SortedMap<Pair<Demand,Link>,Double> res = new TreeMap<Pair<Demand,Link>,Double> ();
		for (Link e : getOutgoingLinks(layer))
		{
			final Double splitFactor = demand.cacheHbH_frs.get(e);
			if (splitFactor != null)
				res.put (Pair.of(demand, e) , splitFactor);
		}
		return res;
	}
	
	
	/** Returns the url of the icon specified by the user to represent this node at the given layer, or null if none
	 * @param layer the layer
	 * @return the url
	 */
	public URL getUrlNodeIcon (NetworkLayer layer)
	{
		final Pair<URL,Double> res = mapLayer2URLSpecificIcon.get(layer); 
		return res == null? null : res.getFirst();
	}
	
	/** Returns the relative size (a number greater than zero), of the icon of this node in the GUI.
	 * @param layer the layer
	 * @return see above
	 */
	public double getNodeIconRelativeSize (NetworkLayer layer)
	{
		final Pair<URL,Double> res = mapLayer2URLSpecificIcon.get(layer);
		if (res == null) return 1.0;
		final Double nodeIconRelativeSize = res.getSecond();
		if (nodeIconRelativeSize == null) return 1;
		return nodeIconRelativeSize <= 0 ? 1 : nodeIconRelativeSize;
	}
	

	/** Sets the url of the icon specified by the user to represent this node at the given layer, and the relative size respect to other nodes. 
	 * Previous url and relative size for this layer (if any) will be removed
	 * @param layer the layer
	 * @param url the url
	 * @param relativeSize the relative size (strictly positive number)
	 */
	public void setUrlNodeIcon (NetworkLayer layer , URL url , Double relativeSize)
	{
		if (relativeSize == null) relativeSize = 1.0; 
		mapLayer2URLSpecificIcon.put(layer , Pair.of(url , relativeSize <= 0? 1.0 : relativeSize));
	}

	/** Removes any previous url of the node icon for this layer (if any)
	 * @param layer the layer
	 */
	public void removeUrlNodeIcon (NetworkLayer layer)
	{
		mapLayer2URLSpecificIcon.remove(layer);
	}

	@Override
    void checkCachesConsistency ()
	{
		super.checkCachesConsistency ();

		if (isUp && netPlan.cache_nodesDown.contains(this)) throw new RuntimeException ("Bad");
		if (!isUp && !netPlan.cache_nodesDown.contains(this)) throw new RuntimeException ("Bad");
		for (Link link : cache_nodeIncomingLinks) if (link.destinationNode != this) throw new RuntimeException ("Bad");
		for (Link link : cache_nodeOutgoingLinks) if (link.originNode != this) throw new RuntimeException ("Bad");
		for (Demand demand : cache_nodeIncomingDemands) if (demand.egressNode != this) throw new RuntimeException ("Bad");
		for (Demand demand : cache_nodeOutgoingDemands) if (demand.ingressNode != this) throw new RuntimeException ("Bad");
		for (MulticastDemand demand : cache_nodeIncomingMulticastDemands) if (!demand.egressNodes.contains(this)) throw new RuntimeException ("Bad");
		for (MulticastDemand demand : cache_nodeOutgoingMulticastDemands) if (demand.ingressNode != this) throw new RuntimeException ("Bad");
		for (SharedRiskGroup srg : cache_nodeNonDynamicSRGs) if (!srg.getNodes().contains(this)) throw new RuntimeException ("Bad");
		for (Route route : cache_nodeAssociatedRoutes) if (!route.cache_seqNodesRealPath.contains(this)) throw new RuntimeException ("Bad: " + cache_nodeAssociatedRoutes);
		for (MulticastTree tree : cache_nodeAssociatedulticastTrees) if (!tree.cache_traversedNodes.contains(this)) throw new RuntimeException ("Bad");
		if (!this.mapLayout2NodeXYPositionMap.keySet().equals(netPlan.cache_definedPlotNodeLayouts)) throw new RuntimeException ("Bad");
	}

	/** Returns the set of planning domains this node belongs to (could be empty)
	 * @return see above
	 */
	public SortedSet<String> getPlanningDomains ()
	{
		return Collections.unmodifiableSortedSet(this.planningDomains);
	}


	/** Remove this node from the given planning domain, if it belongs to it
	 *
	 * @param planningDomain Planning domain name
	 */
	public void removeFromPlanningDomain (String planningDomain)
	{
		if (!this.planningDomains.contains(planningDomain)) return;
		this.planningDomains.remove(planningDomain);
		netPlan.cache_planningDomain2nodes.get(planningDomain).remove(this);
	}

	/** Remove this node from the given planning domain, if it belongs to it
	 *
	 * @param planningDomain Planning domain name
	 */
	public void addToPlanningDomain (String planningDomain)
	{
		if (!netPlan.cache_planningDomain2nodes.keySet().contains(planningDomain)) throw new Net2PlanException ("Wrong planning domain");
		if (this.planningDomains.contains(planningDomain)) return;
		this.planningDomains.add(planningDomain);
		netPlan.cache_planningDomain2nodes.get(planningDomain).add(this);
	}
	

}
