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

package com.net2plan.research.metrohaul.networkModel;

import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** This class represents a node in the network, capable of initiating or ending IP and WDM links, as well as lightpaths and service chains
 */
/**
 * @author Pablo
 *
 */
public class WNode extends WAbstractNetworkElement
{
	private static final String ATTNAMECOMMONPREFIX = "Node_";
	private static final String ATTNAMESUFFIX_TYPE = "type";
	private static final String ATTNAMESUFFIX_ISCONNECTEDTOCORE = "isConnectedToNetworkCore";
	private static final String RESOURCETYPE_CPU = WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER + "CPU";
	private static final String RESOURCETYPE_RAM = WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER + "RAM";
	private static final String RESOURCETYPE_HD = WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER + "HD";
	private static final String ATTNAMESUFFIX_ARBITRARYPARAMSTRING = "ArbitraryString";
	public void setArbitraryParamString (String s) { getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ARBITRARYPARAMSTRING, s); }
	public String getArbitraryParamString () { return getNe().getAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ARBITRARYPARAMSTRING , ""); }
	
	
	private final Node n;
	
	WNode (Node n) { super (n); this.n = n; }

	
	Resource getCpuBaseResource ()
	{
		final Set<Resource> cpuResources = n.getResources(RESOURCETYPE_CPU);
		assert cpuResources.size() < 2;
		if (cpuResources.isEmpty()) setTotalNumCpus(0);
		assert cpuResources.size() == 1;
		return n.getResources(RESOURCETYPE_CPU).iterator().next();
	}
	Resource getRamBaseResource ()
	{
		final Set<Resource> ramResources = n.getResources(RESOURCETYPE_RAM);
		assert ramResources.size() < 2;
		if (ramResources.isEmpty()) setTotalRamGB(0);
		assert ramResources.size() == 1;
		return n.getResources(RESOURCETYPE_RAM).iterator().next();
	}
	Resource getHdBaseResource ()
	{
		final Set<Resource> hdResources = n.getResources(RESOURCETYPE_HD);
		assert hdResources.size() < 2;
		if (hdResources.isEmpty()) setTotalHdGB(0);
		assert hdResources.size() == 1;
		return n.getResources(RESOURCETYPE_HD).iterator().next();
	}
	
	boolean isVirtualNode () { return n.getIndex() <= 1; }

	public Node getNe () { return (Node) e; }

	/** Returns the node name, which must be unique among all the nodes
	 * @return see above
	 */
	public String getName () { return n.getName(); }
	
	/** Sets the node name, which must be unique among all the nodes
	 * @param name see above
	 */
	public void setName (String name) 
	{ 
		if (name == null) WNet.ex("Names cannot be null");
		if (name.contains(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)) throw new Net2PlanException("Names cannot contain the character: " + WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);  
		if (getNet().getNodes().stream().anyMatch(n->n.getName().equals(name))) WNet.ex("Names cannot be repeated");
		if (name.contains(" ")) throw new Net2PlanException("Names cannot contain spaces");  
		n.setName(name); 
	}
	/** Returns the (X,Y) node position
	 * @return see above
	 */
	public Point2D getNodePositionXY () { return n.getXYPositionMap(); }
	/** Sets the (X,Y) node position
	 * @param position see above
	 */
	public void setNodePositionXY (Point2D position) { n.setXYPositionMap(position); }
	/** Returns the user-defined node type
	 * @return see above
	 */
	public String getType () { return getAttributeOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_TYPE , ""); }
	/** Sets the user-defined node type
	 * @param type see above
	 */
	public void setType (String type) { n.setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_TYPE , type); }
	/** Returns if this node is connected to a core node (core nodes are not in the design)
	 * @return see above
	 */
	public boolean isConnectedToNetworkCore () { return getAttributeAsBooleanOrDefault(ATTNAMECOMMONPREFIX +ATTNAMESUFFIX_ISCONNECTEDTOCORE , WNetConstants.WNODE_DEFAULT_ISCONNECTEDTOCORE); }
	/** Sets if this node is assumed to be connected to a core node (core nodes are not in the design)
	 * @param isConnectedToCore see above
	 */
	public void setIsConnectedToNetworkCore (boolean isConnectedToCore) { n.setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ISCONNECTEDTOCORE , new Boolean (isConnectedToCore).toString()); }
	/** Returns the user-defined node population
	 * @return see above
	 */
	public double getPopulation () { return n.getPopulation(); }
	/** Sets the user-defined node population
	 * @param population see above
	 */
	public void setPoputlation (double population) { n.setPopulation(population); }
	/** Returns the number of CPUs available in the node for instantiation of VNFs
	 * @return see above
	 */
	public double getTotalNumCpus () { return n.getResources(RESOURCETYPE_CPU).stream().mapToDouble(r->r.getCapacity()).sum (); }
	/** Sets the number of CPUs available in the node for instantiation of VNFs
	 * @param totalNumCpus see above
	 */
	public void setTotalNumCpus (double totalNumCpus) 
	{ 
		final Set<Resource> res = n.getResources(RESOURCETYPE_CPU);
		if (res.size() > 1) throw new Net2PlanException ("Format error");
		if (res.isEmpty()) 
			res.add(n.getNetPlan().addResource(RESOURCETYPE_CPU, RESOURCETYPE_CPU, Optional.of(n), totalNumCpus, RESOURCETYPE_CPU, new HashMap<> (), 0.0, null));
		else 
			res.iterator().next().setCapacity(totalNumCpus, new HashMap<> ());
	}
	/** Returns the total RAM (in GBytes) available in the node for instantiation of VNFs
	 * @return see above
	 */
	public double getTotalRamGB () { return n.getResources(RESOURCETYPE_RAM).stream().mapToDouble(r->r.getCapacity()).sum (); }
	/** Sets the total RAM (in GBytes) available in the node for instantiation of VNFs
	 * @param totalRamGB see above
	 */
	public void setTotalRamGB (double totalRamGB) 
	{ 
		final Set<Resource> res = n.getResources(RESOURCETYPE_RAM);
		if (res.size() > 1) throw new Net2PlanException ("Format error");
		if (res.isEmpty()) 
			res.add(n.getNetPlan().addResource(RESOURCETYPE_RAM, RESOURCETYPE_RAM, Optional.of(n), totalRamGB, "GB", new HashMap<> (), 0.0, null));
		else 
			res.iterator().next().setCapacity(totalRamGB, new HashMap<> ());
	}
	/** Returns the total hard disk size (in GBytes) available in the node for instantiation of VNFs
	 * @return see above
	 */
	public double getTotalHdGB () { return n.getResources(RESOURCETYPE_HD).stream().mapToDouble(r->r.getCapacity()).sum (); }
	/** Sets the total hard disk size (in GBytes) available in the node for instantiation of VNFs
	 * @param totalHdGB see above
	 */
	public void setTotalHdGB (double totalHdGB) 
	{ 
		final Set<Resource> res = n.getResources(RESOURCETYPE_HD);
		if (res.size() > 1) throw new Net2PlanException ("Format error");
		if (res.isEmpty()) 
			res.add(n.getNetPlan().addResource(RESOURCETYPE_HD, RESOURCETYPE_HD, Optional.of(n), totalHdGB, "GB", new HashMap<> (), 0.0, null));
		else 
			res.iterator().next().setCapacity(totalHdGB, new HashMap<> ());
	}
	/** Returns the current number of occupied CPUs by the instantiated VNFs
	 * @return see above
	 */
	public double getOccupiedCpus () { return getCpuBaseResource().getOccupiedCapacity(); } 
	/** Returns the current amount of occupied hard-disk (in giga bytes) by the instantiated VNFs
	 * @return see above
	 */
	public double getOccupiedHdGB () { return getHdBaseResource().getOccupiedCapacity(); } 
	/** Returns the current amount of occupied RAM (in giga bytes) by the instantiated VNFs
	 * @return see above
	 */
	public double getOccupiedRamGB () { return getRamBaseResource().getOccupiedCapacity(); } 
	
	
	/** Indicates if the node is up or down (failed)
	 * @return see above
	 */
	public boolean isUp () { return n.isUp(); }

	/** Returns the set of outgoing fibers of the node
	 * @return see above
	 */
	public SortedSet<WFiber> getOutgoingFibers () { return n.getOutgoingLinks(getNet().getWdmLayer().getNe()).stream().map(ee->new WFiber(ee)).collect(Collectors.toCollection(TreeSet::new)); }
	/** Returns the set of incoming fibers to the node
	 * @return see above
	 */
	public SortedSet<WFiber> getIncomingFibers () { return n.getIncomingLinks(getNet().getWdmLayer().getNe()).stream().map(ee->new WFiber(ee)).collect(Collectors.toCollection(TreeSet::new)); }
	
	/** Returns the set of outgoing IP links of the node
	 * @return see above
	 */
	public SortedSet<WIpLink> getOutgoingIpLinks () { return n.getOutgoingLinks(getNet().getIpLayer().getNe()).stream().map(ee->new WIpLink(ee)).filter(e->!e.isVirtualLink()).collect(Collectors.toCollection(TreeSet::new)); }
	/** Returns the set of incoming IP links to the node
	 * @return see above
	 */
	public SortedSet<WIpLink> getIncomingIpLinks () { return n.getIncomingLinks(getNet().getIpLayer().getNe()).stream().map(ee->new WIpLink(ee)).filter(e->!e.isVirtualLink()).collect(Collectors.toCollection(TreeSet::new)); }
	
	/** Returns the set of outgoing lightpath requests of the node
	 * @return see above
	 */
	public SortedSet<WLightpathRequest> getOutgoingLigtpathRequests () { return n.getOutgoingDemands(getNet().getWdmLayer().getNe()).stream().map(ee->new WLightpathRequest(ee)).collect(Collectors.toCollection(TreeSet::new)); }
	/** Returns the set of incoming lightpath requests to the node
	 * @return see above
	 */
	public SortedSet<WLightpathRequest> getIncomingLigtpathRequests () { return n.getIncomingDemands(getNet().getWdmLayer().getNe()).stream().map(ee->new WLightpathRequest(ee)).collect(Collectors.toCollection(TreeSet::new)); }

	/** Returns the set of outgoing lightpaths of the node
	 * @return see above
	 */
	public SortedSet<WLightpathUnregenerated> getOutgoingLigtpaths () { return n.getOutgoingRoutes(getNet().getWdmLayer().getNe()).stream().map(ee->new WLightpathUnregenerated(ee)).collect(Collectors.toCollection(TreeSet::new)); }
	/** Returns the set of incoming, outgoing and traversing lightpaths to the node
	 * @return see above
	 */
	public SortedSet<WLightpathUnregenerated> getInOutOrTraversingLigtpaths () { return n.getAssociatedRoutes(getNet().getWdmLayer().getNe()).stream().map(ee->new WLightpathUnregenerated(ee)).collect(Collectors.toCollection(TreeSet::new)); }
	/** Returns the set of incoming lightpaths to the node
	 * @return see above
	 */
	public SortedSet<WLightpathUnregenerated> getIncomingLigtpaths () { return n.getIncomingRoutes(getNet().getWdmLayer().getNe()).stream().map(ee->new WLightpathUnregenerated(ee)).collect(Collectors.toCollection(TreeSet::new)); }

	/** Returns the set of outgoing service chain requests of the node: those which have the node as a potential injection node
	 * @return see above
	 */
	public SortedSet<WServiceChainRequest> getOutgoingServiceChainRequests () { return getNet().getServiceChainRequests().stream().filter(sc->sc.getPotentiallyValidOrigins().contains(this)).collect(Collectors.toCollection(TreeSet::new)); }
	/** Returns the set of incoming service chain requests of the node: those which have the node as a potential end node
	 * @return see above
	 */
	public SortedSet<WServiceChainRequest> getIncomingServiceChainRequests () { return getNet().getServiceChainRequests().stream().filter(sc->sc.getPotentiallyValidDestinations().contains(this)).collect(Collectors.toCollection(TreeSet::new)); }

	/** Returns the set of outgoing service chains of the node, including those starting in a VNF in the node
	 * @return see above
	 */
	public SortedSet<WServiceChain> getOutgoingServiceChains () { return n.getAssociatedRoutes(getNet().getIpLayer().getNe()).stream().map(ee->new WServiceChain(ee)).filter(sc->sc.getA().equals(this)).collect(Collectors.toCollection(TreeSet::new)); }
	/** Returns the set of incoming service chains to the node, including those ended in a VNF in the node
	 * @return see above
	 */
	public SortedSet<WServiceChain> getIncomingServiceChains () { return n.getAssociatedRoutes(getNet().getIpLayer().getNe()).stream().map(ee->new WServiceChain(ee)).filter(sc->sc.getB().equals(this)).collect(Collectors.toCollection(TreeSet::new)); }
	/** Returns the set of incoming, outgoing and traversing service chains to the node. 
	 * @return see above
	 */
	public SortedSet<WServiceChain> getInOutOrTraversingServiceChains () { return n.getAssociatedRoutes(getNet().getIpLayer().getNe()).stream().map(ee->new WServiceChain(ee)).collect(Collectors.toCollection(TreeSet::new)); }

	Link getIncomingLinkFromAnycastOrigin () 
	{ 
		return n.getNetPlan().getNodePairLinks(getNet().getAnycastOriginNode().getNe(), n, false, getIpNpLayer()).stream().findFirst().orElseThrow(()->new RuntimeException()); 
	}
	Link getOutgoingLinkToAnycastDestination () 
	{ 
		return n.getNetPlan().getNodePairLinks(n , getNet().getAnycastDestinationNode().getNe(), false, getIpNpLayer()).stream().findFirst().orElseThrow(()->new RuntimeException()); 
	}
	
	/** Sets the node as up (working, non-failed)
	 */
	public void setAsUp () 
	{ 
		n.setFailureState(true);
		final SortedSet<WLightpathRequest> affectedDemands = new TreeSet<> ();
		getOutgoingFibers().forEach(f->affectedDemands.addAll(f.getTraversingLpRequestsInAtLeastOneLp()));
		getIncomingFibers().forEach(f->affectedDemands.addAll(f.getTraversingLpRequestsInAtLeastOneLp()));
		for (WLightpathRequest lpReq : affectedDemands)
			lpReq.internalUpdateOfRoutesCarriedTrafficFromFailureState();
	}
	
	/** Sets the node as down (failed), so traversing IP links or lightpaths become down
	 */
	public void setAsDown () 
	{ 
		n.setFailureState(false);
		final SortedSet<WLightpathRequest> affectedDemands = new TreeSet<> ();
		getOutgoingFibers().forEach(f->affectedDemands.addAll(f.getTraversingLpRequestsInAtLeastOneLp()));
		getIncomingFibers().forEach(f->affectedDemands.addAll(f.getTraversingLpRequestsInAtLeastOneLp()));
		for (WLightpathRequest lpReq : affectedDemands)
			lpReq.internalUpdateOfRoutesCarriedTrafficFromFailureState();
	}

	/** Removes this node, and all the ending and initiated links, or traversing lightpaths or service chains 
	 * 
	 */
	public void remove () { this.setAsDown(); n.remove(); }

	public String toString (){ return "Node " + getName(); }

	/** Returns all the VNF instances in this node, of any type
	 * @return see above
	 */
	public SortedSet<WVnfInstance> getAllVnfInstances ()
	{
		return n.getResources().stream().filter(r->!r.getType().contains(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)).map(r->new WVnfInstance(r)).collect(Collectors.toCollection(TreeSet::new));
	}
	
	/** Returns all the VNF instances in this node, of the given type
	 * @param type see above
	 * @return see above
	 */
	public SortedSet<WVnfInstance> getVnfInstances (String type)
	{
		if (type.contains(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)) throw new Net2PlanException("Names cannot contain the character: " + WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);  
		return n.getResources().stream().filter(r->!r.getType().contains(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)).map(r->new WVnfInstance(r)).filter(v->v.getType().equals(type)).collect(Collectors.toCollection(TreeSet::new));
	}

}
