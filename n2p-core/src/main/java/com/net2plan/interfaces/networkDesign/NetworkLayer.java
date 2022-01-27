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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.SortedMap;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.SortedSet;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.net2plan.internal.AttributeMap;
import com.net2plan.utils.Pair;

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
	String linkCapacityUnitsName;
	
	ArrayList<Link> links;
	ArrayList<Demand> demands;
	ArrayList<MulticastDemand> multicastDemands;
	ArrayList<Route> routes;
	ArrayList<MulticastTree> multicastTrees;

	SortedSet<Link> cache_linksDown;
	SortedSet<Link> cache_linksZeroCap;
	SortedSet<Link> cache_coupledLinks;
	SortedSet<Demand> cache_coupledDemands;
	SortedSet<MulticastDemand> cache_coupledMulticastDemands;
	
	SortedSet<Route> cache_routesDown;
	SortedSet<Route> cache_routesTravLinkZeroCap;
	SortedSet<MulticastTree> cache_multicastTreesDown;
	SortedSet<MulticastTree> cache_multicastTreesTravLinkZeroCap;
	SortedMap<Pair<Node,Node>,SortedSet<Link>> cache_nodePairLinksThisLayer;
	SortedMap<Pair<Node,Node>,SortedSet<Demand>> cache_nodePairDemandsThisLayer;
	SortedMap<String,Pair<SortedSet<Demand>,SortedSet<MulticastDemand>>> cache_qosTypes2DemandMap;
	
	URL defaultNodeIconURL;

	NetworkLayer(NetPlan netPlan, long id, int index , String demandTrafficUnitsName, String description, String name, String linkCapacityUnitsName, URL defaultNodeIconURL , AttributeMap attributes)
	{
		super(netPlan, id , index , attributes);
		this.setName ("Layer-" + index);

		this.demandTrafficUnitsName = (demandTrafficUnitsName == null)? "" : demandTrafficUnitsName;
		this.description =  (description == null)? "" : description;
		this.name =  (name == null)? "" : name;
		this.linkCapacityUnitsName =  (linkCapacityUnitsName == null)? "" : linkCapacityUnitsName;
		this.defaultNodeIconURL = defaultNodeIconURL;

		this.links = new ArrayList<Link> ();
		this.demands = new ArrayList<Demand> ();
		this.multicastDemands = new ArrayList<MulticastDemand> ();
		this.routes = new ArrayList<Route> ();
		this.multicastTrees = new ArrayList<MulticastTree> ();

		this.cache_linksDown = new TreeSet<Link> ();
		this.cache_linksZeroCap = new TreeSet<>();
		this.cache_coupledLinks = new TreeSet<Link> ();
		this.cache_coupledDemands = new TreeSet<Demand> ();
		this.cache_coupledMulticastDemands = new TreeSet<MulticastDemand> ();

		this.cache_routesDown = new TreeSet<Route> ();
		this.cache_routesTravLinkZeroCap = new TreeSet<>();
		this.cache_multicastTreesDown = new TreeSet<MulticastTree> ();
		this.cache_multicastTreesTravLinkZeroCap = new TreeSet<> ();
		this.cache_nodePairLinksThisLayer = new TreeMap<> ();
		this.cache_nodePairDemandsThisLayer = new TreeMap<> ();
		this.cache_qosTypes2DemandMap = new TreeMap<> ();

//		this.forwardingRulesNoFailureState_f_de = null;
//		this.forwardingRulesCurrentFailureState_x_de = null;
//		this.forwardingRules_Aout_ne = null;
//		this.forwardingRules_Ain_ne = null;
	}

	void copyFrom (NetworkLayer origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");

		this.demandTrafficUnitsName = origin.demandTrafficUnitsName;
		this.description = origin.description;
		this.defaultNodeIconURL = origin.defaultNodeIconURL;
		this.name = origin.name;
		this.linkCapacityUnitsName = origin.linkCapacityUnitsName;
		this.cache_linksDown.clear (); for (Link e : origin.cache_linksDown) this.cache_linksDown.add(this.netPlan.getLinkFromId (e.id));
		this.cache_linksZeroCap.clear (); for (Link e : origin.cache_linksZeroCap) this.cache_linksZeroCap.add(this.netPlan.getLinkFromId (e.id));
		this.cache_coupledLinks.clear (); for (Link e : origin.cache_coupledLinks) this.cache_coupledLinks.add(this.netPlan.getLinkFromId (e.id));
		this.cache_coupledDemands.clear (); for (Demand d : origin.cache_coupledDemands) this.cache_coupledDemands.add(this.netPlan.getDemandFromId (d.id));
		this.cache_coupledMulticastDemands.clear (); for (MulticastDemand d : origin.cache_coupledMulticastDemands) this.cache_coupledMulticastDemands.add(this.netPlan.getMulticastDemandFromId(d.id));
		this.cache_routesDown.clear (); for (Route r : origin.cache_routesDown) this.cache_routesDown.add(this.netPlan.getRouteFromId (r.id));
		this.cache_routesTravLinkZeroCap.clear(); for (Route r : origin.cache_routesTravLinkZeroCap) this.cache_routesTravLinkZeroCap.add(this.netPlan.getRouteFromId (r.id));
		this.cache_multicastTreesDown.clear (); for (MulticastTree t : origin.cache_multicastTreesDown) this.cache_multicastTreesDown.add(this.netPlan.getMulticastTreeFromId (t.id));
		this.cache_multicastTreesTravLinkZeroCap.clear(); for (MulticastTree t : origin.cache_multicastTreesTravLinkZeroCap) this.cache_multicastTreesTravLinkZeroCap.add(this.netPlan.getMulticastTreeFromId (t.id));
		this.cache_nodePairLinksThisLayer.clear(); for (Entry<Pair<Node,Node>,SortedSet<Link>> entry : origin.cache_nodePairLinksThisLayer.entrySet()) this.cache_nodePairLinksThisLayer.put(Pair.of(this.netPlan.getNodeFromId(entry.getKey().getFirst().getId()) , this.netPlan.getNodeFromId(entry.getKey().getSecond().getId())) , (SortedSet<Link>) (SortedSet<?>) this.netPlan.translateCollectionToThisNetPlan(entry.getValue()));
		this.cache_nodePairDemandsThisLayer.clear(); for (Entry<Pair<Node,Node>,SortedSet<Demand>> entry : origin.cache_nodePairDemandsThisLayer.entrySet()) this.cache_nodePairDemandsThisLayer.put(Pair.of(this.netPlan.getNodeFromId(entry.getKey().getFirst().getId()) , this.netPlan.getNodeFromId(entry.getKey().getSecond().getId())) , (SortedSet<Demand>) (SortedSet<?>) this.netPlan.translateCollectionToThisNetPlan(entry.getValue()));
		this.cache_qosTypes2DemandMap.clear(); for (Entry<String,Pair<SortedSet<Demand>,SortedSet<MulticastDemand>>> entry : origin.cache_qosTypes2DemandMap.entrySet()) this.cache_qosTypes2DemandMap.put(entry.getKey() , Pair.of((SortedSet<Demand>) (SortedSet<?>) this.netPlan.translateCollectionToThisNetPlan(entry.getValue().getFirst()) , (SortedSet<MulticastDemand>) (SortedSet<?>) this.netPlan.translateCollectionToThisNetPlan(entry.getValue().getSecond())));
		
		for (Link e : origin.links) this.links.get(e.index).copyFrom(e);
		for (Demand d : origin.demands) this.demands.get(d.index).copyFrom(d);
		for (MulticastDemand d : origin.multicastDemands) this.multicastDemands.get(d.index).copyFrom(d);
		for (Route r : origin.routes) this.routes.get(r.index).copyFrom(r);
		for (MulticastTree t : origin.multicastTrees) this.multicastTrees.get(t.index).copyFrom(t);
	}

	/** Returns true if this layer is the default netowrk layer
	 * @return see above
	 */
	public boolean isDefaultLayer () { return netPlan.getNetworkLayerDefault() == this; }

	/** Returns true if the provided network layer is a deep copy of this
	 * @param e2 the other element
	 * @return see above
	 */
	public boolean isDeepCopy (NetworkLayer e2)
	{
		if (!super.isDeepCopy(e2)) return false;
		if (!this.demandTrafficUnitsName.equals(e2.demandTrafficUnitsName)) return false;
		if (!this.description.equals(e2.description)) return false;
		if ((this.defaultNodeIconURL == null) != (e2.defaultNodeIconURL == null)) return false;
		if (this.defaultNodeIconURL != null) if (!this.defaultNodeIconURL.equals(e2.defaultNodeIconURL)) return false;
		if (!this.name.equals(e2.name)) return false;
		if (!this.linkCapacityUnitsName.equals(e2.linkCapacityUnitsName)) return false;
		if (!NetPlan.isDeepCopy(this.links , e2.links)) return false;
		if (!NetPlan.isDeepCopy(this.demands , e2.demands)) return false;
		if (!NetPlan.isDeepCopy(this.multicastDemands , e2.multicastDemands)) return false;
		if (!NetPlan.isDeepCopy(this.routes , e2.routes)) return false;
		if (!NetPlan.isDeepCopy(this.multicastTrees , e2.multicastTrees)) return false;
		if (!NetPlan.isDeepCopy(this.multicastTrees , e2.multicastTrees)) return false;
//		if ((this.forwardingRulesNoFailureState_f_de != null) && (!this.forwardingRulesNoFailureState_f_de.equals(e2.forwardingRulesNoFailureState_f_de))) return false;
//		if ((this.forwardingRulesCurrentFailureState_x_de != null) && (!this.forwardingRulesCurrentFailureState_x_de.equals(e2.forwardingRulesCurrentFailureState_x_de))) return false;
//		if ((this.forwardingRules_Aout_ne != null) && (!this.forwardingRules_Aout_ne.equals(e2.forwardingRules_Aout_ne))) return false;
//		if ((this.forwardingRules_Ain_ne != null) && (!this.forwardingRules_Ain_ne.equals(e2.forwardingRules_Ain_ne))) return false;
		if (!NetPlan.isDeepCopy(this.cache_linksDown , e2.cache_linksDown)) return false;
		if (!NetPlan.isDeepCopy(this.cache_linksZeroCap , e2.cache_linksZeroCap)) return false;
		if (!NetPlan.isDeepCopy(this.cache_coupledLinks , e2.cache_coupledLinks)) return false;
		if (!NetPlan.isDeepCopy(this.cache_coupledDemands , e2.cache_coupledDemands)) return false;
		if (!NetPlan.isDeepCopy(this.cache_coupledMulticastDemands , e2.cache_coupledMulticastDemands)) return false;
		if (!NetPlan.isDeepCopy(this.cache_routesDown , e2.cache_routesDown)) return false;
		if (!NetPlan.isDeepCopy(this.cache_routesTravLinkZeroCap , e2.cache_routesTravLinkZeroCap)) return false;
		if (!NetPlan.isDeepCopy(this.cache_multicastTreesDown , e2.cache_multicastTreesDown)) return false;
		if (!NetPlan.isDeepCopy(this.cache_multicastTreesTravLinkZeroCap , e2.cache_multicastTreesTravLinkZeroCap )) return false;
		
		final SortedSet<Demand> auxDemandThis = this.cache_nodePairDemandsThisLayer.values().stream().flatMap(e->e.stream()).collect(Collectors.toCollection(TreeSet::new));
		if (!this.cache_qosTypes2DemandMap.keySet().equals(e2.cache_qosTypes2DemandMap.keySet())) return false;
		for (String qos : this.cache_qosTypes2DemandMap.keySet())
		{
			if (!NetPlan.isDeepCopy(this.cache_qosTypes2DemandMap.get(qos).getFirst(), e2.cache_qosTypes2DemandMap.get(qos).getFirst())) return false;
			if (!NetPlan.isDeepCopy(this.cache_qosTypes2DemandMap.get(qos).getSecond(), e2.cache_qosTypes2DemandMap.get(qos).getSecond())) return false;
		}
		final SortedSet<Demand> auxDemandE2 = e2.cache_nodePairDemandsThisLayer.values().stream().flatMap(e->e.stream()).collect(Collectors.toCollection(TreeSet::new));
		if (!NetPlan.isDeepCopy(auxDemandThis , auxDemandE2)) return false;
		final SortedSet<Link> auxLinkThis = this.cache_nodePairLinksThisLayer.values().stream().flatMap(e->e.stream()).collect(Collectors.toCollection(TreeSet::new));
		final SortedSet<Link> auxLinkE2 = e2.cache_nodePairLinksThisLayer.values().stream().flatMap(e->e.stream()).collect(Collectors.toCollection(TreeSet::new));
		if (!NetPlan.isDeepCopy(auxLinkThis , auxLinkE2)) return false;

		return true;
	}
	
	/**
	 * <p>Returns the user-defined URL of the default icon to represent the nodes at this layer.</p>
	 * @return see above
	 */
	public URL getDefaultNodeIconURL() 
	{
		return defaultNodeIconURL;
	}

	/**
	 * <p>Sets the user-defined URL of the default icon to represent the nodes at this layer. </p>
	 * @param defaultNodeIconURL see above. 
	 */
	public void setDefaultNodeIconURL(URL defaultNodeIconURL) 
	{
		this.defaultNodeIconURL = defaultNodeIconURL;
	}

	/**
	 * <p>Returns a {@code String} representation of the network layer.</p>
	 * @return {@code String} representation of the network layer
	 */
	@Override
    public String toString () { return "layer" + index + " (id " + id + ")"; }

	public SortedSet<String> getDemandAndMulticastDemandQosTypes () 
	{ 
		return new TreeSet<> (cache_qosTypes2DemandMap.keySet()); 
	} 
	
	/** Given a Qos type, returns the uncoupled demands in this layer with that QoS type, or empty set if none.
	 * @param qosType see above
	 * @return see above
	 */
	public Pair<SortedSet<Demand>,SortedSet<MulticastDemand>> getDemandsPerQosType (String qosType)
	{
		final Pair<SortedSet<Demand>,SortedSet<MulticastDemand>> demandsCoupledAndNot = cache_qosTypes2DemandMap.get(qosType);
		if (demandsCoupledAndNot == null) return Pair.of(new TreeSet<> () , new TreeSet<> ());
		return Pair.of(Collections.unmodifiableSortedSet(demandsCoupledAndNot.getFirst()), Collections.unmodifiableSortedSet(demandsCoupledAndNot.getSecond()));
	}
	
	@Override
    void checkCachesConsistency ()
	{
		super.checkCachesConsistency ();

		for (Link link : cache_linksDown) if (link.isUp) throw new RuntimeException ("Bad");
		for (Link link : cache_linksZeroCap) if (link.getCapacity() != 0) throw new RuntimeException ("Bad");
		for (Link link : cache_coupledLinks) if ((link.coupledLowerOrThisLayerDemand == null) && (link.coupledLowerLayerMulticastDemand == null)) throw new RuntimeException ("Bad");
		for (Demand demand : cache_coupledDemands) if (demand.coupledUpperOrSameLayerLink == null) throw new RuntimeException ("Bad");
		for (MulticastDemand demand : cache_coupledMulticastDemands) if (demand.coupledUpperLayerLinks == null) throw new RuntimeException ("Bad");
		for (Route route : routes)
		{
			final boolean shoulbBeUp = (route.getSeqLinks().stream().allMatch(e->e.isUp) && (route.getSeqNodes().stream().allMatch(n->n.isUp)));
			final boolean travZeroCapLinks = route.getSeqLinks().stream().anyMatch(e->e.capacity < Configuration.precisionFactor);
			if (shoulbBeUp && cache_routesDown.contains(route)) throw new RuntimeException ();
			if (!shoulbBeUp && !cache_routesDown.contains(route)) throw new RuntimeException ();
			if (travZeroCapLinks && !cache_routesTravLinkZeroCap.contains(route)) throw new RuntimeException ();
			if (!travZeroCapLinks && cache_routesTravLinkZeroCap.contains(route)) throw new RuntimeException ();
		}
		for (MulticastTree tree : multicastTrees)
		{
			final boolean shoulbBeUp = (tree.getLinkSet().stream().allMatch(e->e.isUp) && (tree.getNodeSet().stream().allMatch(n->n.isUp)));
			final boolean travZeroCapLinks = tree.getLinkSet().stream().anyMatch(e->e.capacity < Configuration.precisionFactor);
			if (shoulbBeUp && cache_multicastTreesDown.contains(tree)) throw new RuntimeException ();
			if (!shoulbBeUp && !tree.getLinkSet().isEmpty() && !cache_multicastTreesDown.contains(tree)) 
			    throw new RuntimeException ();
			if (travZeroCapLinks && !cache_multicastTreesTravLinkZeroCap.contains(tree)) throw new RuntimeException ();
			if (!travZeroCapLinks && cache_multicastTreesTravLinkZeroCap.contains(tree)) throw new RuntimeException ();
		}
		for (Demand d : demands)
		{
			assert cache_qosTypes2DemandMap.containsKey(d.qosType);
			assert cache_qosTypes2DemandMap.get(d.qosType).getFirst ().contains(d);
		}
		for (MulticastDemand d : multicastDemands)
		{
			assert cache_qosTypes2DemandMap.containsKey(d.qosType);
			assert cache_qosTypes2DemandMap.get(d.qosType).getSecond ().contains(d);
		}
		final SortedSet<String> currentDemandsQos = demands.stream().map(d->d.qosType).collect(Collectors.toCollection(TreeSet::new));
		final SortedSet<String> currentMDemandsQos = multicastDemands.stream().map(d->d.qosType).collect(Collectors.toCollection(TreeSet::new));
		assert Sets.union(currentDemandsQos, currentMDemandsQos).equals(cache_qosTypes2DemandMap.keySet());
		for (String qosType : cache_qosTypes2DemandMap.keySet())
		{
			assert cache_qosTypes2DemandMap.get(qosType).getFirst().equals(demands.stream().filter(d->d.qosType.equals(qosType)).collect(Collectors.toSet()));
			assert cache_qosTypes2DemandMap.get(qosType).getSecond().equals(multicastDemands.stream().filter(d->d.qosType.equals(qosType)).collect(Collectors.toSet()));
		}
		for (SortedSet<Demand> dd : this.cache_nodePairDemandsThisLayer.values()) for (Demand d : dd) if (d.layer != this || d.netPlan != this.netPlan) throw new RuntimeException ();
		for (SortedSet<Link> ee : this.cache_nodePairLinksThisLayer.values()) for (Link e: ee) if (e.layer != this || e.netPlan != this.netPlan) throw new RuntimeException ();
		for (Entry<Pair<Node,Node>,SortedSet<Demand>> entry : this.cache_nodePairDemandsThisLayer.entrySet())
		{
			for (Demand d : entry.getValue())
			{
				if (d.getIngressNode() != entry.getKey().getFirst()) throw new RuntimeException ();
				if (d.getEgressNode() != entry.getKey().getSecond()) throw new RuntimeException ();
			}
		}
		for (Entry<Pair<Node,Node>,SortedSet<Link>> entry : this.cache_nodePairLinksThisLayer.entrySet())
		{
			for (Link e : entry.getValue())
			{
				if (e.getOriginNode() != entry.getKey().getFirst()) throw new RuntimeException ();
				if (e.getDestinationNode() != entry.getKey().getSecond()) throw new RuntimeException ();
			}
		}
		for (Demand d : this.demands) if (!this.cache_nodePairDemandsThisLayer.get(Pair.of(d.getIngressNode(),d.getEgressNode())).contains(d)) throw new RuntimeException ();
		for (Link e : this.links) if (!this.cache_nodePairLinksThisLayer.get(Pair.of(e.getOriginNode(),e.getDestinationNode())).contains(e)) throw new RuntimeException ();
	}


	public String getDemandTrafficUnits () { return demandTrafficUnitsName; } 
	public String getLinkCapacityUnits () { return linkCapacityUnitsName; } 
	
}
