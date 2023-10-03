/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jfree.data.json.impl.JSONObject;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.niw.WFlexAlgo.FlexAlgoProperties;
import com.net2plan.niw.WNetConstants.WTYPE;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;

/**
 * This class represents an IP over WDM network with potential VNF placement. This is the main model class, that gives
 * access to the key methods to play with the network
 *
 */
public class WNet extends WAbstractNetworkElement
{
	private static final String ATTNAME_VNFTYPELIST = NIWNAMEPREFIX + "VnfTypeListMatrix";
	private static final String ATTNAME_USERSERVICELIST = NIWNAMEPREFIX + "userServiceListMatrix";
	private static final String ATTNAME_WDMOPTICALSLOTSIZEGHZ = NIWNAMEPREFIX + "wdmOpticalSlotSizeGhz";
	private static final String ATTNAME_FLEXALGOINFO = NIWNAMEPREFIX + "flexAlgoInfo";
	

	public Map<Integer,FlexAlgoProperties> getFlexAlgoInfo ()
	{
		modifyFlexAlgo(7, f->f.associatedSids.add(""));
		
	}
	
	public void modifyFlexAlgo (int flexAlgoId , Consumer<FlexAlgoProperties> whatToDoInFlexAlgo  )
	{
		// read the attribute and extract the lex aglo
		// aplly the consumer function to the flex algo
		// write the attribute again with the full flex algo repo 
	}
	
	
	/** Creates a WNet object from a NetPlan object. Does not check its consistency as a valid NIW design
	 * @param np see above
	 */
	public WNet(NetPlan np)
	{
		super(np, Optional.empty());
		this.np = np;
	}
	
	/** Returns the size in GHz to be considered as the optical slot size in the system. Typically, 100 GHz, 50 GHz or 12.5 GHz.
	 * Attending to the ITU conventions, central frequency (in THz) of slot n is given by 193.1 + (n * slotSizeInGhz/1000) 
	 * @return see above
	 */
	public double getWdmOpticalSlotSizeInGHz () { final double val = getNe().getAttributeAsDouble(ATTNAME_WDMOPTICALSLOTSIZEGHZ, WNetConstants.DEFAULT_OPTICALSLOTSIZE_GHZ); return val <= 0? WNetConstants.DEFAULT_OPTICALSLOTSIZE_GHZ : val; }

	/** Sets the size in GHz to be considered as the optical slot size in the system. Typically, 100 GHz, 50 GHz or 12.5 GHz.
	 * Attending to the ITU conventions, central frequency (in THz) of slot n is given by 193.1 + (n * slotSizeInGhz/1000) 
	 * @param slotSizeInGHz see above
	 */
	public void setWdmOpticalSlotSizeInGHz (double slotSizeInGHz) { getNe().setAttribute(ATTNAME_WDMOPTICALSLOTSIZEGHZ, slotSizeInGHz); }

	/** Indicates if the NetPlan object is a valid NIW-readable design
	 * @param np see above
	 * @return see above
	 */
	public static boolean isNiwValidDesign (NetPlan np)
	{
		try
		{
			final WNet design = new WNet (np);
			design.checkConsistency();
			return true;
		} catch (Throwable e) { e.printStackTrace(); return false; }
	}

	/** Makes some checks, and make the lighptath carried traffics, ip carried traffic in bundles, and service chain 
	 * carried traffic, be consistent to the failure state in the netplan elements
	 */
	public void updateNetPlanObjectInternalState ()
	{
		/* Updates the NetPlan object in the carried traffic of lighptaths */
		for (WLightpathRequest lpr : this.getLightpathRequests())
		{
			if (!lpr.hasLightpathsAssigned()) continue;
			if (!lpr.is11Protected())
			{
				final WLightpath lp = getLightpaths().get(0);
				lp.getNe().setCarriedTraffic(lp.isUp()? lpr.getLineRateGbps() : 0.0 , null);
			}
			else
			{
				final List<WLightpath> lps = getLightpaths();
				if (lps.get(0).isUp()) 
				{ 
					lps.get(0).getNe().setCarriedTraffic(lpr.getLineRateGbps(), null);   
					lps.get(1).getNe().setCarriedTraffic(0.0, null);   
				}
				else
				{
					lps.get(0).getNe().setCarriedTraffic(0.0 , null);   
					lps.get(1).getNe().setCarriedTraffic(lps.get(1).isUp()? lpr.getLineRateGbps() : 0.0 , null);   
				}
			}
		}
		
		/* Updates the NetPlan object in the carried traffic of IP bundles */
		for (WIpLink ipLink : getIpLinks())
		{
			if (ipLink.isBundleOfIpLinks())
			{
				for (WIpLink member : ipLink.getBundledIpLinks())
				{
					assert member.getNe().getTraversingRoutes().size() == 1;
					final Route r = member.getNe().getTraversingRoutes().first();
					r.setCarriedTraffic(member.getNominalCapacityGbps(), member.getNominalCapacityGbps());
				}
			} 
		}

		/* Updates the NetPlan object in the carried traffic of service chains */
		for (WServiceChain sc : getServiceChains())
		{
			final boolean isOk = !sc.getNe ().isTraversingZeroCapLinks() && !sc.getNe().isDown();
			if (isOk)
				sc.getNe().setCarriedTraffic(sc.getNe().getCarriedTrafficInNoFailureState(), sc.getNe().getSeqOccupiedCapacitiesIfNotFailing ());
			else
				sc.getNe().setCarriedTraffic(0.0, Collections.nCopies(sc.getNe().getSeqOccupiedCapacitiesIfNotFailing ().size(), 0.0));
		}

		/* Updates the worst case latency and length info of */
		//for (WIpUnicastDemand d : this.getIpUnicastDemands())
	}
	
	private final NetPlan np;

	@Override
	public NetPlan getNe()
	{
		return (NetPlan) associatedNpElement;
	}

	/**
	 * Returns the object identifying the WDM layer
	 * @return see above
	 */
	public Optional<WLayerWdm> getWdmLayer()
	{
		return np.getNetworkLayers().stream().filter(e->e.getName().equals(WNetConstants.wdmLayerName)).map(e->new WLayerWdm(e)).findFirst();
	}

	/**
	 * Returns the object identifying the IP layer
	 * @return see above
	 */
	public Optional<WLayerIp> getIpLayer()
	{
		return np.getNetworkLayers().stream().filter(e->e.getName().equals(WNetConstants.ipLayerName)).map(e->new WLayerIp(e)).findFirst();
	}

	/** Add a SRG, with no associated failing elements, a MTTR of 12 hours, and MTTF of 1 year of 365 days
	 * @return see above
	 */
	public WSharedRiskGroup addSharedRiskGroup ()
	{
		final SharedRiskGroup srg = getNe().addSRG(WNetConstants.DEFAULT_SRG_MTTFHOURS, WNetConstants.DEFAULT_SRG_MTTRHOURS, null);
		return new WSharedRiskGroup(srg);
	}

	/**
	 * Returns the list of defined shared risk groups, in increasing order according to its id
	 * @return see above
	 */
	public List<WSharedRiskGroup> getSrgs ()
	{
		return np.getSRGs().stream().map(n -> new WSharedRiskGroup(n)).collect(Collectors.toCollection(ArrayList::new));
	}
	
	/** Returns the number of nodes in the design, sa returned by getNodes
	 * @return see above
	 */
	public int getNumberOfNodes () { return (int) np.getNodes().stream().map(n -> new WNode(n)).filter(n -> !n.isVirtualNode()).count (); }
	
	/**
	 * Returns the list of network nodes, in increasing order according to its id
	 * @return see above
	 */
	public List<WNode> getNodes()
	{
		return np.getNodes().stream().map(n -> new WNode(n)).filter(n -> !n.isVirtualNode()).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Returns the list of fibers, in increasing order according to its id
	 * @return see above
	 */
	public List<WFiber> getFibers()
	{
		if (!isWithWdmLayer()) return new ArrayList<> ();
		return np.getLinks(getWdmLayer().get().getNe()).stream().map(n -> new WFiber(n)).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Returns the list of lightpath requests, in increasing order according to its id
	 * @return see above
	 */
	public List<WLightpathRequest> getLightpathRequests()
	{
		if (!isWithWdmLayer()) return new ArrayList<> ();
		return np.getDemands(getWdmLayer().get().getNe()).stream().
				filter(d->{ WTYPE t = getWType(d).orElse(null); return t == null? false : t.isLightpathRequest(); }).
				map(n -> new WLightpathRequest(n)).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Returns the list of lightpaths, in increasing order according to its id
	 * @return see above
	 */
	public List<WLightpath> getLightpaths()
	{
		if (!isWithWdmLayer()) return new ArrayList<> ();
		return np.getRoutes(getWdmLayer().get().getNe()).stream().
				filter(d->{ WTYPE t = getWType(d).orElse(null); return t == null? false : t.isLightpath(); }).
				map(n -> new WLightpath(n)).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Returns the list of IP links, in increasing order according to its id
	 * @return see above
	 */
	public List<WIpLink> getIpLinks()
	{
		if (!isWithIpLayer ()) return new ArrayList<> ();
		return np.getLinks(getIpLayer().get().getNe()).stream().
				filter(d->{ WTYPE t = getWType(d).orElse(null); return t == null? false : t.isIpLink(); }).
				map(n -> new WIpLink(n)).filter(e -> !e.isVirtualLink()).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Returns the list of service chain requests, in increasing order according to its id
	 * @return see above
	 */
	public List<WServiceChainRequest> getServiceChainRequests()
	{
		if (!isWithIpLayer ()) return new ArrayList<> ();
		return np.getDemands(getIpLayer().get().getNe()).stream().
				filter(d->{ WTYPE t = getWType(d).orElse(null); return t == null? false : t.isServiceChainRequest(); }).
				map(n -> new WServiceChainRequest(n)).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Returns the list of IP unicast demands, in increasing order according to its id
	 * @return see above
	 */
	public List<WIpUnicastDemand> getIpUnicastDemands ()
	{
		if (!isWithIpLayer ()) return new ArrayList<> ();
		return np.getDemands(getIpLayer().get().getNe()).stream().
				filter(d->{ WTYPE t = getWType(d).orElse(null); return t == null? false : t.isIpUnicastDemand(); }).
				map(n -> new WIpUnicastDemand(n)).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Returns the list of service chains, in increasing order according to its id
	 * @return see above
	 */
	public List<WServiceChain> getServiceChains()
	{
		if (!isWithIpLayer ()) return new ArrayList<> ();
		return np.getRoutes(getIpLayer().get().getNe()).stream().
				filter(d->{ WTYPE t = getWType(d).orElse(null); return t == null? false : t.isServiceChain(); }).
				map(n -> new WServiceChain(n)).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Returns the list of IP source routed connections, in increasing order according to its id
	 * @return see above
	 */
	public List<WIpSourceRoutedConnection> getIpSourceRoutedConnections()
	{
		if (!isWithIpLayer ()) return new ArrayList<> ();
		return np.getRoutes(getIpLayer().get().getNe()).stream().
				filter(d->{ WTYPE t = getWType(d).orElse(null); return t == null? false : t.isIpSourceRoutedConnection(); }).
				map(n -> new WIpSourceRoutedConnection(n)).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Saves this network in the given file
	 * @param f see above
	 */
	public void saveToFile(File f)
	{
		getNetPlan().saveToFile(f);
	}

	/**
	 * Creates an empty design with no nodes, links etc.
	 * @param withIpLayer see above
	 * @param withWdmLayer see above
	 * @return see above
	 */
	public static WNet createEmptyDesign(boolean withIpLayer , boolean withWdmLayer)
	{
		if (!withIpLayer && !withWdmLayer) throw new Net2PlanException ("The design must have at least one layer");
		final NetPlan np = new NetPlan();
		final NetworkLayer initialDefaultLayer = np.getNetworkLayerDefault();
		if (withWdmLayer)
			np.addLayer(WNetConstants.wdmLayerName, WNetConstants.wdmLayerName, "", "", null, null);
		if (withIpLayer)
		{
			np.addLayer(WNetConstants.ipLayerName, WNetConstants.ipLayerName, "", "", null, null);
			np.addNode(0, 0, WNetConstants.WNODE_NAMEOFANYCASTORIGINNODE, null).addTag(WNetConstants.TAGNODE_INDICATIONVIRTUALORIGINNODE);
			np.addNode(0, 0, WNetConstants.WNODE_NAMEOFANYCASTDESTINATION, null).addTag(WNetConstants.TAGNODE_INDICATIONVIRTUALDESTINATIONNODE);
		}
		np.removeNetworkLayer(initialDefaultLayer);
		final WNet res = new WNet(np);
		return res;
	}

	/**
	 * Creates a design, loading it from a file
	 * @param f see above
	 * @return see above
	 */
	public static WNet loadFromFile(File f)
	{
		return new WNet(NetPlan.loadFromFile(f));
	}

	boolean isNpIpLayer (NetworkLayer layer) { final WLayerIp l = getIpLayer().orElse (null); return l == null? false : l.getNe().equals(layer); }
	
	boolean isNpWdmLayer (NetworkLayer layer) { final WLayerWdm l = getWdmLayer().orElse (null); return l == null? false : l.getNe().equals(layer); }

	public boolean isWithWdmLayer () { return getWdmLayer().isPresent(); } 
	
	public boolean isWithIpLayer () { return getIpLayer().isPresent(); }

	WNode getAnycastOriginNode()
	{
		final Set<Node> nodes = getNe ().getTaggedNodes(WNetConstants.TAGNODE_INDICATIONVIRTUALORIGINNODE);
		assert nodes.size() <= 1;
		if (nodes.size() == 1)
			return new WNode (nodes.iterator().next());
		final Node n = np.addNode(0, 0, WNetConstants.WNODE_NAMEOFANYCASTORIGINNODE, null);
		n.addTag(WNetConstants.TAGNODE_INDICATIONVIRTUALORIGINNODE);
		return new WNode (n);
	}

	WNode getAnycastDestinationNode()
	{
		final Set<Node> nodes = getNe ().getTaggedNodes(WNetConstants.TAGNODE_INDICATIONVIRTUALDESTINATIONNODE);
		assert nodes.size() <= 1;
		if (nodes.size() == 1)
			return new WNode (nodes.iterator().next());
		final Node n = np.addNode(0, 0, WNetConstants.WNODE_NAMEOFANYCASTDESTINATION, null);
		n.addTag(WNetConstants.TAGNODE_INDICATIONVIRTUALDESTINATIONNODE);
		return new WNode (n);
	}

	public String getUnusedValidNodeName () 
	{
		int index = getNumberOfNodes();
		while (true)
		{
			final String name = "Node" + index;
			if (getNodes().stream().anyMatch(n -> n.getName().equals(name))) { index ++; continue; }
			return name;
		}
	}
	
	/**
	 * Adds a node to the design
	 * @param xCoord the x coordinate
	 * @param yCoord the y coordinate
	 * @param name the node name, that should be unique
	 * @param type the type of node: a user-defined string
	 * @return see above
	 */
	public WNode addNode(double xCoord, double yCoord, String name, String type)
	{
		if (name == null) ex("Names cannot be null");
		if (name.contains(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)) throw new Net2PlanException("Names cannot contain the character: " + WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);
		if (getNodes().stream().anyMatch(n -> n.getName().equals(name))) ex("Names cannot be repeated");
		final WNode n = new WNode(getNetPlan().addNode(xCoord, yCoord, name, null));
		n.setType(type);
		
		/* Add the virtual links from the anycast nodes to this node */ 
		if (isWithIpLayer())
		{
			final Link anycastOriginToNode = getNetPlan().addLink(getAnycastOriginNode().getNe(), n.getNe(), Double.MAX_VALUE, 1, WNetConstants.WFIBER_DEFAULT_PROPAGATIONSPEEDKMPERSEC, null, getIpLayer().get().getNe());
			anycastOriginToNode.setAttribute(WIpLink.ATTNAMECOMMONPREFIX + WIpLink.ATTNAMESUFFIX_NOMINALCAPACITYGBPS, Double.MAX_VALUE);
	
			final Link nodeToAnycastDestination = getNetPlan().addLink(n.getNe(), getAnycastDestinationNode().getNe() , Double.MAX_VALUE, 1, WNetConstants.WFIBER_DEFAULT_PROPAGATIONSPEEDKMPERSEC, null, getIpLayer().get().getNe());
			nodeToAnycastDestination.setAttribute(WIpLink.ATTNAMECOMMONPREFIX + WIpLink.ATTNAMESUFFIX_NOMINALCAPACITYGBPS, Double.MAX_VALUE);
		}
		return n;
	}

	/**
	 * Adds a fiber to the design
	 * @param a the origin node
	 * @param b the destination node
	 * @param validOpticalSlotRanges the pairs (init slot , end slot) with the valid optical slot ranges for the fiber
	 * @param lengthInKm the user-defined length in km. If negative, the harversine distance is set for the link length,
	 *        that considers node (x,y) coordinates a longitude-latitude pairs
	 * @param isBidirectional indicates if the fiber to add is bidirectional
	 * @return the first element of the pair is the fiber a- b, the second is b-a or null in non-bidirectional cases
	 */
	public Pair<WFiber, WFiber> addFiber(WNode a, WNode b, List<Pair<Integer,Integer>> validOpticalSlotRanges, double lengthInKm, boolean isBidirectional)
	{
		if (!isWithWdmLayer ()) throw new Net2PlanException ("WDM layer does not exist");
		checkInThisWNet(a, b);
		if (validOpticalSlotRanges == null) validOpticalSlotRanges = WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES;
		if (lengthInKm < 0) lengthInKm = getNetPlan().getNodePairHaversineDistanceInKm(a.getNe(), b.getNe());
		final SortedSet<Integer> opticalSlots = WFiber.computeValidOpticalSlotIds(validOpticalSlotRanges);
		if (isBidirectional)
		{
			final Pair<Link, Link> ee = getNetPlan().addLinkBidirectional(a.getNe(), b.getNe(), opticalSlots.size(), lengthInKm, WNetConstants.WFIBER_DEFAULT_PROPAGATIONSPEEDKMPERSEC, null, getWdmLayer().get().getNe());
			WFiber fiber1 = new WFiber(ee.getFirst());
			fiber1.setValidOpticalSlotRanges(validOpticalSlotRanges);
			WFiber fiber2 = new WFiber(ee.getSecond());
			fiber2.setValidOpticalSlotRanges(validOpticalSlotRanges);
			return Pair.of(fiber1, fiber2);
		} else
		{
			final Link ee = getNetPlan().addLink(a.getNe(), b.getNe(), opticalSlots.size(), lengthInKm, WNetConstants.WFIBER_DEFAULT_PROPAGATIONSPEEDKMPERSEC, null, getWdmLayer().get().getNe());
			WFiber fiber = new WFiber(ee);
			fiber.setValidOpticalSlotRanges(validOpticalSlotRanges);
			return Pair.of(fiber, null);
		}
	}

	/**
	 * Adds a lightpath request to the design
	 * @param a The origin node of the lightpath request
	 * @param b The destination node of the lightpath request
	 * @param lineRateGbps The nominal line rate in Gbps
	 * @param isToBe11Protected indicates if it is requested to have a 1+1 setting assigned to this lightpath
	 * @return the lightpath request created
	 */
	public WLightpathRequest addLightpathRequest(WNode a, WNode b, double lineRateGbps, boolean isToBe11Protected)
	{
		if (!isWithWdmLayer ()) throw new Net2PlanException ("WDM layer does not exist");
		checkInThisWNet(a, b);
		final WLightpathRequest lpReq = new WLightpathRequest(getNetPlan().addDemand(a.getNe(), b.getNe(), lineRateGbps, RoutingType.SOURCE_ROUTING, null, getWdmLayer().get().getNe()));
		lpReq.setIsToBe11Protected(isToBe11Protected);
		return lpReq;
	}

	/**
	 * Returns the nodes in the network that are supposed to have a connection to the network core (the core nodes and those
	 * connecting links are not part of the network)
	 * @return see above
	 */
	public SortedSet<WNode> getNodesConnectedToCore()
	{
		return getNodes().stream().filter(n -> n.isConnectedToNetworkCore()).collect(Collectors.toCollection(TreeSet::new));
	}

	/** Removes the IP layer in the design, if any, and if not the only layer
	 */
	public void removeIpLayer ()
	{
		final NetPlan np = getNet().getNe(); 
		if (np.getNumberOfLayers() == 1) return;
		if (!isWithIpLayer()) return;
		getAnycastOriginNode().remove();
		getAnycastDestinationNode().remove();
		np.removeNetworkLayer(getIpNpLayer().get());
	}
	
	/** Removes the WDM layer in the design, if any, and if not the only layer
	 */
	public void removeWdmLayer ()
	{
		final NetPlan np = getNet().getNe(); 
		if (np.getNumberOfLayers() == 1) return;
		if (!isWithWdmLayer()) return;
		np.removeNetworkLayer(getWdmNpLayer().get());
	}
	

	/** Adds an IP layer to the design, if it does not have already one
	 */
	public void addIpLayer ()
	{
		if (this.isWithIpLayer()) return;
		np.addLayer(WNetConstants.ipLayerName, WNetConstants.ipLayerName, "", "", null, null);
		np.addNode(0, 0, WNetConstants.WNODE_NAMEOFANYCASTORIGINNODE, null).addTag(WNetConstants.TAGNODE_INDICATIONVIRTUALORIGINNODE);
		np.addNode(0, 0, WNetConstants.WNODE_NAMEOFANYCASTDESTINATION, null).addTag(WNetConstants.TAGNODE_INDICATIONVIRTUALDESTINATIONNODE);
	}
	
	/** Adds a WDM layer to the design, if it does not have already one
	 */
	public void addWdmLayer ()
	{
		if (this.isWithWdmLayer()) return;
		np.addLayer(WNetConstants.wdmLayerName, WNetConstants.wdmLayerName, "", "", null, null);
	}

	/**
	 * Adds a request for an upstream or downstream unidirectional service chain, for the given user service. The user
	 * service will define the sequence of VNF types that the flow must traverse, and will indicate the set of potential end
	 * nodes (for upstream flows) or potential initial nodes (for downstream flows).
	 * @param userInjectionNode the node where the traffic starts (in upstream flows) or ends (in downstream flows)
	 * @param isUpstream if true, the flow is supposed to start in the injection node, if false, the flow is supposed to end
	 *        in the injection node
	 * @param userService the user service defining this service chain request.
	 * @return see above
	 */
	public WServiceChainRequest addServiceChainRequest(WNode userInjectionNode, boolean isUpstream, WUserService userService)
	{
		if (!isWithIpLayer ()) throw new Net2PlanException ("IP layer does not exist");
		checkInThisWNet(userInjectionNode);
		final Demand scNp = getNetPlan().addDemand(getAnycastOriginNode().getNe(), getAnycastDestinationNode().getNe(), 0.0, RoutingType.SOURCE_ROUTING, null, getIpNpLayer().get());
		final WServiceChainRequest scReq = new WServiceChainRequest(scNp);
		final BiConsumer<Collection<WNode>,Boolean> setAttributeEndNodesOriginTrue = (stNames , isOrigin) ->
		{
			final String atName = isOrigin? WServiceChainRequest.ATTNAMECOMMONPREFIX + WServiceChainRequest.ATTNAMESUFFIX_VALIDINPUTNODENAMES : WServiceChainRequest.ATTNAMECOMMONPREFIX + WServiceChainRequest.ATTNAMESUFFIX_VALIDOUTPUTNODENAMES;
			final List<String> resNames = stNames.stream().map(n -> n.getName()).collect(Collectors.toList());
			scReq.getNe().setAttributeAsStringList(atName, resNames);
		};
		if (isUpstream)
		{
			/* Directly modify the attribute, if not an assert there breaks */
			scNp.setServiceChainSequenceOfTraversedResourceTypes(userService.getListVnfTypesToTraverseUpstream());
			scReq.setIsUpstream(true);
			setAttributeEndNodesOriginTrue.accept(Arrays.asList(userInjectionNode), true);
			if (userService.isEndingInCoreNode()) setAttributeEndNodesOriginTrue.accept(getNodesConnectedToCore() , false);
			else setAttributeEndNodesOriginTrue.accept(new ArrayList<> () , false);
			scReq.setDefaultSequenceOfExpansionFactorsRespectToInjection(userService.getSequenceTrafficExpansionFactorsRespectToBaseTrafficUpstream());
			scReq.setListMaxLatencyFromInitialToVnfStart_ms(userService.getListMaxLatencyFromInitialToVnfStart_ms_upstream());
		} else
		{
			scNp.setServiceChainSequenceOfTraversedResourceTypes(userService.getListVnfTypesToTraverseDownstream());
			scReq.setIsUpstream(false);
			setAttributeEndNodesOriginTrue.accept(Arrays.asList(userInjectionNode), false);
			if (userService.isEndingInCoreNode()) setAttributeEndNodesOriginTrue.accept(getNodesConnectedToCore(), true);
			else setAttributeEndNodesOriginTrue.accept(new ArrayList<> (), true);
			scReq.setDefaultSequenceOfExpansionFactorsRespectToInjection(userService.getSequenceTrafficExpansionFactorsRespectToBaseTrafficDownstream());
			scReq.setListMaxLatencyFromInitialToVnfStart_ms(userService.getListMaxLatencyFromInitialToVnfStart_ms_downstream());
		}
		return scReq;
	}

	/**
	 * Adds a request for an upstream or downstream unidirectional service chain, for the given user service. The user
	 * service will define the sequence of VNF types that the flow must traverse, and will indicate the set of potential end
	 * nodes (for upstream flows) or potential initial nodes (for downstream flows).
	 * @param userInjectionNode the node where the traffic starts (in upstream flows) or ends (in downstream flows)
	 * @param isUpstream if true, the flow is supposed to start in the injection node, if false, the flow is supposed to end
	 *        in the injection node
	 * @param userService the user service defining this service chain request.
	 * @return see above
	 */
	/**
	 * Adds a request for an upstream or downstream unidirectional service chain, with the given characteristics. 
	 * The user will define the sequence of VNF types that the flow must traverse, and will indicate the set of potential end
	 * nodes (for upstream flows) or potential initial nodes (for downstream flows). Also the default sequence of expansion factors 
	 * of the traffic when traversing the VNF, and the list with the maximum latency from the origin
	 * node, to the input of the i-th VNF traversed. The last value of such list is the maximum latency from the origin node to
	 * the end node for the service chains realizing this request. 
	 * value for the 
	 * @param originNodes see above
	 * @param endNodes see above
	 * @param vnfTypesToTraverse see above
	 * @param isUpstream see above
	 * @param defaultSequenceOfExpansionFactors see above
	 * @param listMaxLatencyFromInitialToVnfStart_ms see above
	 * @return  see above
	 */
	public WServiceChainRequest addServiceChainRequest(SortedSet<WNode> originNodes , SortedSet<WNode> endNodes , 
			List<String> vnfTypesToTraverse , 
			boolean isUpstream , 
			Optional<List<Double>> defaultSequenceOfExpansionFactors , 
			Optional<List<Double>> listMaxLatencyFromInitialToVnfStart_ms)
	{
		if (!isWithIpLayer ()) throw new Net2PlanException ("IP layer does not exist");
		checkInThisWNetCol(originNodes);
		checkInThisWNetCol(endNodes);
		final Demand scNp = getNetPlan().addDemand(getAnycastOriginNode().getNe(), getAnycastDestinationNode().getNe(), 0.0, RoutingType.SOURCE_ROUTING, null, getIpNpLayer().get());
		final WServiceChainRequest scReq = new WServiceChainRequest(scNp);
		scNp.setServiceChainSequenceOfTraversedResourceTypes(vnfTypesToTraverse);
		scReq.setIsUpstream(isUpstream);
		/* Set origin nodes directly */
		scReq.getNe().setAttributeAsStringList(WServiceChainRequest.ATTNAMECOMMONPREFIX + WServiceChainRequest.ATTNAMESUFFIX_VALIDINPUTNODENAMES, originNodes.stream().map(n -> n.getName()).collect(Collectors.toList()));
		//scReq.setPotentiallyValidOrigins(originNodes);
		scReq.setPotentiallyValidDestinations(endNodes);
		final int numVnfs = vnfTypesToTraverse.size();
		List<Double> defaultSeqExpFactor = defaultSequenceOfExpansionFactors.orElse(null);
		if (defaultSeqExpFactor == null) defaultSeqExpFactor = Collections.nCopies(numVnfs, 1.0);
		if (defaultSeqExpFactor.size() != numVnfs) defaultSeqExpFactor = Collections.nCopies(numVnfs, 1.0);
		scReq.setDefaultSequenceOfExpansionFactorsRespectToInjection(defaultSeqExpFactor);
		List<Double> actual_listMaxLatencyFromInitialToVnfStart_ms = listMaxLatencyFromInitialToVnfStart_ms.orElse(null);
		if (actual_listMaxLatencyFromInitialToVnfStart_ms == null) actual_listMaxLatencyFromInitialToVnfStart_ms = Collections.nCopies(numVnfs + 1, Double.MAX_VALUE);
		if (actual_listMaxLatencyFromInitialToVnfStart_ms.size() != numVnfs + 1) actual_listMaxLatencyFromInitialToVnfStart_ms = Collections.nCopies(numVnfs + 1, Double.MAX_VALUE);
		scReq.setListMaxLatencyFromInitialToVnfStart_ms(actual_listMaxLatencyFromInitialToVnfStart_ms);
		return scReq;
	}

	
	/**
	 * Adds a request for a unidirectional anycast service chain with hand picked initial and end nodes.
	 * @param initialNodes list of nodes that can be the origin of this flow
	 * @param endNodes list of nodes that can be the destination of this flow (different to origin or not)
	 * @param listVnfTypes list of IDs of the VNF types that must be traversed
	 * @param defaultSequenceOfExpansionFactorsRespectToInjection sequence of expansion factors to define as default for
	 *        this service chain request (same size as the list of VNFs, if 1.0 means no compression/expansion after
	 *        traversing the VNF)
	 * @param maxLatencyFromInitialToVnfStartMs List of maximum latencies specified for this service chain request. The list
	 *        contains V+1 values, being V the number of VNFs to traverse. The first V values (i=1,...,V) correspond to the
	 *        maximum latency from the origin node, to the input of the i-th VNF traversed. The last value corresponds to
	 *        the maximum latency from the origin node to the end node.
	 * @param isUpstream if this request should be tagged as upstream of downstream
	 * @param userServiceIdString an informational user-defined String to indicate a user service ID that this service chain
	 *        belongs to. Such user service may exist or not
	 * @return the created element
	 */
	public WServiceChainRequest addServiceChainRequest(SortedSet<WNode> initialNodes, SortedSet<WNode> endNodes, List<String> listVnfTypes, List<Double> defaultSequenceOfExpansionFactorsRespectToInjection,
			List<Double> maxLatencyFromInitialToVnfStartMs, boolean isUpstream, String userServiceIdString)
	{
		if (!isWithIpLayer ()) throw new Net2PlanException ("IP layer does not exist");
		checkInThisWNetCol(initialNodes);
		checkInThisWNetCol(endNodes);
		final Demand scNp = getNetPlan().addDemand(getAnycastOriginNode().getNe(), getAnycastDestinationNode().getNe(), 0.0, RoutingType.SOURCE_ROUTING, null, getIpNpLayer().get());
		final WServiceChainRequest scReq = new WServiceChainRequest(scNp);
		scReq.setQosType(userServiceIdString);
		scNp.setServiceChainSequenceOfTraversedResourceTypes(listVnfTypes);
		scReq.setPotentiallyValidOrigins(initialNodes);
		scReq.setPotentiallyValidDestinations(endNodes);
		scReq.setDefaultSequenceOfExpansionFactorsRespectToInjection(defaultSequenceOfExpansionFactorsRespectToInjection);
		scReq.setListMaxLatencyFromInitialToVnfStart_ms(maxLatencyFromInitialToVnfStartMs);
		scReq.setIsUpstream(isUpstream);
		return scReq;
	}

	/**
	 * Adds a new unicast IP demand Returns the list of IP unicast demands, in increasing order according to its id
	 * @param initialNode see above
	 * @param endNode see above
	 * @param isUpstream see above
	 * @param isHopByHopRouted see above
	 * @return see above
	 */
	public WIpUnicastDemand addIpUnicastDemand (WNode initialNode , WNode endNode , boolean isUpstream, boolean isHopByHopRouted)
	{
		if (!isWithIpLayer ()) throw new Net2PlanException ("IP layer does not exist");
		checkInThisWNet(initialNode);
		checkInThisWNet(endNode);
		final Demand scNp = getNetPlan().addDemand(initialNode.getNe(), endNode.getNe(), 0.0, isHopByHopRouted? RoutingType.HOP_BY_HOP_ROUTING : RoutingType.SOURCE_ROUTING, null, getIpNpLayer().get());
		final WIpUnicastDemand scReq = new WIpUnicastDemand(scNp);
		scReq.setIsUpstream(isUpstream);
		return scReq;
	}

	/**
	 * Adds an IP link to the network
	 * @param a The IP link origin node
	 * @param b The IP lin kend node
	 * @param nominalLineRateGbps The nominal line rate of the IP link in Gbps
	 * @return the first element of the pair is the a-b IP link, the second is the b-a IP link 
	 */
	public Pair<WIpLink, WIpLink> addIpLinkBidirectional (WNode a, WNode b, double nominalLineRateGbps)
	{
		if (!isWithIpLayer ()) throw new Net2PlanException ("IP layer does not exist");
		checkInThisWNet(a, b);
		final Pair<Link, Link> ee = getNetPlan().addLinkBidirectional(a.getNe(), b.getNe(), nominalLineRateGbps, 1, WNetConstants.WFIBER_DEFAULT_PROPAGATIONSPEEDKMPERSEC, null, getIpLayer().get().getNe());
		return Pair.of(WIpLink.createFromAdd(ee.getFirst(), nominalLineRateGbps), WIpLink.createFromAdd(ee.getSecond(), nominalLineRateGbps));
	}

	/**
	 * Returns the network node with the indicated name, if any
	 * @param name see above
	 * @return see above
	 */
	public Optional<WNode> getNodeByName(String name)
	{
		return getNodes().stream().filter(n -> n.getName().equals(name)).findFirst();
	}

	/**
	 * Returns the map associating the VNF type, with the associated VNF object, as was defined by the user
	 * @return see above
	 */
	public List<WVnfType> getVnfTypes()
	{
		final List<WVnfType> res = new ArrayList<>();
		final List<List<String>> matrix = getNe().getAttributeAsStringMatrix(ATTNAME_VNFTYPELIST, null);
		if (matrix == null) throw new Net2PlanException("Wrong format");
		for (List<String> row : matrix)
		{
			if (row.size() != 9) throw new Net2PlanException("Wrong format");
			final String vnfTypeName = row.get(0);
			if (vnfTypeName.contains(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)) throw new Net2PlanException("VNF type names cannot contain the character: " + WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);
			if (res.stream().anyMatch(v -> vnfTypeName.equals(v.getVnfTypeName()))) throw new Net2PlanException("VNF type names must be unique");
			final double maxInputTraffic_Gbps = Double.parseDouble(row.get(1));
			final double numCpus = Double.parseDouble(row.get(2));
			final double numRam = Double.parseDouble(row.get(3));
			final double numHd = Double.parseDouble(row.get(4));
			final double processingTimeMs = Double.parseDouble(row.get(5));
			final boolean isConstrained = Boolean.parseBoolean(row.get(6));
			final SortedSet<String> nodeNames = new TreeSet<>(Arrays.asList(row.get(7).split(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)));
			final String arbitraryParamString = row.get(8);
			res.add(new WVnfType(vnfTypeName, maxInputTraffic_Gbps, numCpus, numRam, numHd, processingTimeMs, isConstrained ? Optional.of(nodeNames) : Optional.empty(), arbitraryParamString));
		}
		return res;
	}

	/**
	 * Returns the WVnfType object assigned to that name, if any
	 * @param typeName see above
	 * @return see above
	 */
	public Optional<WVnfType> getVnfType(String typeName)
	{
		return getVnfTypes().stream().filter(v -> v.getVnfTypeName().equals(typeName)).findFirst();
	}

	/**
	 * Adds a new VNF type to the network, with the provided information
	 * @param info see above
	 */
	public void addOrUpdateVnfType(WVnfType info)
	{
		SortedSet<WVnfType> newInfo = new TreeSet<>();
		if (getNe().getAttributeAsStringMatrix(ATTNAME_VNFTYPELIST, null) != null)
		{
			newInfo = new TreeSet<>(this.getVnfTypes());
		}
		newInfo.add(info);
		this.setVnfTypesRemovingPreviousInfo(newInfo);
	}

	/**
	 * Removes a defined VNF type
	 * @param vnfTypeName see above
	 */
	public void removeVnfType(String vnfTypeName)
	{
		final SortedSet<WVnfType> newInfo = this.getVnfTypes().stream().filter(v -> !v.getVnfTypeName().equals(vnfTypeName)).collect(Collectors.toCollection(TreeSet::new));
		this.setVnfTypesRemovingPreviousInfo(newInfo);
	}

	/**
	 * Sets a new VNF type map, removing all previous VNF types defined
	 * @param newInfo see above
	 */
	public void setVnfTypesRemovingPreviousInfo(SortedSet<WVnfType> newInfo)
	{
		final List<List<String>> matrix = new ArrayList<>();
		for (WVnfType entry : newInfo)
		{
			final List<String> infoThisVnf = new LinkedList<>();
			infoThisVnf.add(entry.getVnfTypeName());
			infoThisVnf.add(entry.getMaxInputTrafficPerVnfInstance_Gbps() + "");
			infoThisVnf.add(entry.getOccupCpu() + "");
			infoThisVnf.add(entry.getOccupRamGBytes() + "");
			infoThisVnf.add(entry.getOccupHdGBytes() + "");
			infoThisVnf.add(entry.getProcessingTime_ms() + "");
			infoThisVnf.add(new Boolean(entry.isConstrainedToBeInstantiatedOnlyInUserDefinedNodes()).toString());
			infoThisVnf.add(entry.getValidMetroNodesForInstantiation().stream().collect(Collectors.joining(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)));
			infoThisVnf.add(entry.getArbitraryParamString());
			matrix.add(infoThisVnf);
		}
		np.setAttributeAsStringMatrix(ATTNAME_VNFTYPELIST, matrix);
	}

	/**
	 * Returns the set of VNF type names defined
	 * @return see above
	 */
	public SortedSet<String> getVnfTypeNames()
	{
		return getVnfTypes().stream().map(v -> v.getVnfTypeName()).collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * Returns the user service information defined, in a map with key the user service identifier, and value the user
	 * service object
	 * @return see above
	 */
	public SortedMap<String, WUserService> getUserServicesInfo()
	{
		final SortedMap<String, WUserService> res = new TreeMap<>();
		final List<List<String>> matrix = getNe().getAttributeAsStringMatrix(ATTNAME_USERSERVICELIST, null);
		if (matrix == null) throw new Net2PlanException("Wrong format");
		for (List<String> row : matrix)
		{
			if (row.size() != 10) throw new Net2PlanException("Wrong format");
			final String userServiceUniqueId = row.get(0);
			if (res.containsKey(userServiceUniqueId)) throw new Net2PlanException("User service names must be unique");
			final List<String> listVnfTypesToTraverseUpstream = Arrays.asList(StringUtils.split(row.get(1), WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER));
			final List<String> listVnfTypesToTraverseDownstream = Arrays.asList(StringUtils.split(row.get(2), WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER));
			final List<Double> sequenceTrafficExpansionFactorsRespectToBaseTrafficUpstream = Arrays.asList(row.get(3).split(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)).stream().map(s -> Double.parseDouble(s)).collect(Collectors.toList());
			final List<Double> sequenceTrafficExpansionFactorsRespectToBaseTrafficDownstream = Arrays.asList(row.get(4).split(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)).stream().map(s -> Double.parseDouble(s)).collect(Collectors.toList());
			final List<Double> listMaxLatencyFromInitialToVnfStart_ms_upstream = Arrays.asList(row.get(5).split(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)).stream().map(s -> Double.parseDouble(s)).collect(Collectors.toList());
			final List<Double> listMaxLatencyFromInitialToVnfStart_ms_downstream = Arrays.asList(row.get(6).split(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)).stream().map(s -> Double.parseDouble(s)).collect(Collectors.toList());
			final double injectionDownstreamExpansionFactorRespecToBaseTrafficUpstream = Double.parseDouble(row.get(7));
			final boolean isEndingInCoreNode = Boolean.parseBoolean(row.get(8));
			final String arbitraryParamString = row.get(9);
			res.put(userServiceUniqueId,
					new WUserService(userServiceUniqueId, listVnfTypesToTraverseUpstream, listVnfTypesToTraverseDownstream, sequenceTrafficExpansionFactorsRespectToBaseTrafficUpstream, sequenceTrafficExpansionFactorsRespectToBaseTrafficDownstream,
							listMaxLatencyFromInitialToVnfStart_ms_upstream, listMaxLatencyFromInitialToVnfStart_ms_downstream, injectionDownstreamExpansionFactorRespecToBaseTrafficUpstream, isEndingInCoreNode, arbitraryParamString));
		}
		return res;
	}

	/**
	 * Adds or updates a user service to the defined repository of user services
	 * @param info see above
	 */
	public void addOrUpdateUserService(WUserService info)
	{
		SortedMap<String, WUserService> newInfo = new TreeMap<>();
		if (getNe().getAttributeAsStringMatrix(ATTNAME_USERSERVICELIST, null) != null) newInfo = this.getUserServicesInfo();
		newInfo.put(info.getUserServiceUniqueId(), info);
		this.setUserServicesInfo(newInfo);
	}

	/**
	 * Removes a user service definition from the internal repo
	 * @param userServiceName see above
	 */
	public void removeUserServiceInfo(String userServiceName)
	{
		final SortedMap<String, WUserService> newInfo = this.getUserServicesInfo();
		newInfo.remove(userServiceName);
		this.setUserServicesInfo(newInfo);
	}

	/**
	 * Sets a new user service id to info map, removing all previous user services defined
	 * @param newInfo see above
	 */
	public void setUserServicesInfo(SortedMap<String, WUserService> newInfo)
	{
		final List<List<String>> matrix = new ArrayList<>();
		for (Entry<String, WUserService> entry : newInfo.entrySet())
		{
			final List<String> infoThisVnf = new LinkedList<>();
			infoThisVnf.add(entry.getKey());
			infoThisVnf.add(entry.getValue().getListVnfTypesToTraverseUpstream().stream().collect(Collectors.joining(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)));
			infoThisVnf.add(entry.getValue().getListVnfTypesToTraverseDownstream().stream().collect(Collectors.joining(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)));
			infoThisVnf.add(entry.getValue().getSequenceTrafficExpansionFactorsRespectToBaseTrafficUpstream().stream().map(n -> n.toString()).collect(Collectors.joining(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)) + "");
			infoThisVnf.add(entry.getValue().getSequenceTrafficExpansionFactorsRespectToBaseTrafficDownstream().stream().map(n -> n.toString()).collect(Collectors.joining(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)) + "");
			infoThisVnf.add(entry.getValue().getListMaxLatencyFromInitialToVnfStart_ms_upstream().stream().map(n -> n.toString()).collect(Collectors.joining(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)) + "");
			infoThisVnf.add(entry.getValue().getListMaxLatencyFromInitialToVnfStart_ms_downstream().stream().map(n -> n.toString()).collect(Collectors.joining(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)) + "");
			infoThisVnf.add(entry.getValue().getInjectionDownstreamExpansionFactorRespecToBaseTrafficUpstream() + "");
			infoThisVnf.add(new Boolean(entry.getValue().isEndingInCoreNode()).toString());
			infoThisVnf.add(entry.getValue().getArbitraryParamString());
			matrix.add(infoThisVnf);
		}
		np.setAttributeAsStringMatrix(ATTNAME_USERSERVICELIST, matrix);
	}

	/**
	 * Returns all the unique ids (names) of the user services defined
	 * @return see above
	 */
	public SortedSet<String> getUserServiceNames()
	{
		return new TreeSet<>(getUserServicesInfo().keySet());
	}

	static void ex(String s)
	{
		throw new Net2PlanException(s);
	}

	/**
	 * Returns a set with all the VNF instances in the network of the given type
	 * @param type see above
	 * @return see above
	 */
	public SortedSet<WVnfInstance> getVnfInstances(String type)
	{
		return np.getResources(type).stream().map(r -> new WVnfInstance(r)).collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * Returns a set with all the VNF instances in the network of any type
	 * @return see above
	 */
	public SortedSet<WVnfInstance> getVnfInstances()
	{
		return np.getResources().stream().map(r -> new WVnfInstance(r)).filter(e -> !e.isBaseResourceNotAVnf()).collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * Returns a ranking with the k-shortest paths for service chains, given the end nodes, and the sequence of types of
	 * VNFs that it should traverse.
	 * @param k the maximum number of paths to return in the ranking
	 * @param a the origin node
	 * @param b the end node
	 * @param sequenceOfVnfTypesToTraverse the sequence of VNF types to be traversed, in the given order
	 * @param optionalCostMapOrElseLatency an optional map of the cost to assign to traversing each IP link, if none, the
	 *        cost is assumed to the IP link latency
	 * @param optionalVnfCostMapOrElseLatency an optional map of the cost to assign to traverse each VNF instance, if none,
	 *        the cost is assumed to be the VNF instance processing time
	 * @return a list of up to k paths, where each path is a sequence of WIpLink and WVnfInstance objects forming a service
	 *         chain from a to b
	 */
	public List<List<WAbstractNetworkElement>> getKShortestServiceChainInIpLayer(int k, WNode a, WNode b, List<String> sequenceOfVnfTypesToTraverse, Optional<Map<WIpLink, Double>> optionalCostMapOrElseLatency,
			Optional<Map<WVnfInstance, Double>> optionalVnfCostMapOrElseLatency)
	{
		if (!isWithIpLayer ()) throw new Net2PlanException ("IP layer does not exist");
		checkInThisWNet(a, b);
		if (optionalCostMapOrElseLatency.isPresent()) checkInThisWNetCol(optionalCostMapOrElseLatency.get().keySet());
		if (optionalVnfCostMapOrElseLatency.isPresent()) checkInThisWNetCol(optionalVnfCostMapOrElseLatency.get().keySet());
		final Node anycastNode_a = getAnycastOriginNode().getNe();
		final Node anycastNode_b = getAnycastDestinationNode().getNe();
		final List<Link> npLinks = np.getLinks(getIpNpLayer().get()).stream().filter(e -> !e.getOriginNode().equals(anycastNode_a) && !e.getDestinationNode().equals(anycastNode_b)).collect(Collectors.toList());
		final Node originNode = a.getNe();
		final Node destinationNode = b.getNe();
		final List<String> sequenceOfResourceTypesToTraverse = sequenceOfVnfTypesToTraverse;
		final DoubleMatrix1D linkCost = DoubleFactory1D.dense.make(npLinks.size());
		for (int cont = 0; cont < npLinks.size(); cont++)
		{
			final WIpLink e = new WIpLink(npLinks.get(cont));
			linkCost.set(cont, optionalCostMapOrElseLatency.isPresent() ? optionalCostMapOrElseLatency.get().get(e) : e.getWorstCasePropagationDelayInMs());
		}
		final Map<Resource, Double> resourceCost = new HashMap<>();
		for (String vnfType : new HashSet<>(sequenceOfResourceTypesToTraverse))
		{
			for (WVnfInstance vfnInstance : getVnfInstances(vnfType))
			{
				final double cost = optionalVnfCostMapOrElseLatency.isPresent() ? optionalVnfCostMapOrElseLatency.get().get(vfnInstance) : vfnInstance.getProcessingTimeInMs();
				resourceCost.put(vfnInstance.getNe(), cost);
			}
		}
		final List<Pair<List<NetworkElement>, Double>> kShortest = GraphUtils.getKMinimumCostServiceChains(npLinks, originNode, destinationNode, sequenceOfResourceTypesToTraverse, linkCost, resourceCost, k, -1, -1, -1, -1, null);
		final List<List<WAbstractNetworkElement>> res = new ArrayList<>(kShortest.size());
		for (Pair<List<NetworkElement>, Double> npPath : kShortest)
		{
			final List<WAbstractNetworkElement> wpath = npPath.getFirst().stream().map(e -> e instanceof Link ? new WIpLink((Link) e) : new WVnfInstance((Resource) e)).collect(Collectors.toList());
			res.add(wpath);
		}
		return res;
	}

	/**
	 * Returns the k shortest paths between origin and destination, using only the IP links and Nodes passed
	 * @param k number of paths (max)
	 * @param nodes list of nodes
	 * @param links list of links
	 * @param a origin node
	 * @param b destination node
	 * @param optionalCostMapOrElseLatency optional map with the cost to assign to each IP link, tom compute the shortest
	 *        pahts. If empty, cost is one per link
	 * @return see above
	 */
	public List<List<WIpLink>> getKShortestIpUnicastPath(int k, Collection<WNode> nodes, Collection<WIpLink> links, WNode a, WNode b, Optional<Map<WIpLink, Double>> optionalCostMapOrElseLatency)
	{
		checkInThisWNet(a, b);
		if (optionalCostMapOrElseLatency.isPresent()) checkInThisWNetCol(optionalCostMapOrElseLatency.get().keySet());
		final List<Link> npLinks = links.stream().map(e -> e.getNe()).collect(Collectors.toList());
		final List<Node> npNodes = nodes.stream().map(e -> e.getNe()).collect(Collectors.toList());
		final Node np_a = a.getNe();
		final Node np_b = b.getNe();
		final Map<Link, Double> linkCostMap = optionalCostMapOrElseLatency.isPresent() ? optionalCostMapOrElseLatency.get().entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getNe(), e -> e.getValue())) : null;
		final List<List<Link>> kNpPaths = GraphUtils.getKLooplessShortestPaths(npNodes, npLinks, np_a, np_b, linkCostMap, k, -1, -1, -1, -1, -1, -1);
		return kNpPaths.stream().map(l -> l.stream().map(e -> new WIpLink(e)).collect(Collectors.toList())).collect(Collectors.toList());
	}

	/**
	 * Returns a ranking with the k-shortest paths composed as a sequence of WFiber links in the network, given the origin
	 * and end nodes of the paths.
	 * @param k the maximum number of paths to return in the ranking
	 * @param a the origin node
	 * @param b the end node
	 * @param optionalCostMapOrElseLatency an optional map of the cost to assign to traversing each WFiber link, if none,
	 *        the cost is assumed to be the WFiber latency
	 * @return a list of up to k paths, where each path is a sequence of WFiber objects forming a path from a to b
	 */
	public List<List<WFiber>> getKShortestWdmPath(int k, WNode a, WNode b, Optional<Map<WFiber, Double>> optionalCostMapOrElseLatency)
	{
		if (!isWithWdmLayer ()) throw new Net2PlanException ("WDM layer does not exist");
		checkInThisWNet(a, b);
		if (optionalCostMapOrElseLatency == null) optionalCostMapOrElseLatency = Optional.empty();
		if (optionalCostMapOrElseLatency.isPresent()) checkInThisWNetCol(optionalCostMapOrElseLatency.get().keySet());

		final List<Node> nodes = np.getNodes();
		final List<Link> links = np.getLinks(getWdmNpLayer().get());
		final Map<Link, Double> linkCostMap = new HashMap<>();
		for (Link e : links)
		{
			final double cost;
			if (optionalCostMapOrElseLatency.isPresent()) cost = optionalCostMapOrElseLatency.get().getOrDefault(new WFiber(e), e.getPropagationDelayInMs());
			else cost = e.getPropagationDelayInMs();
			linkCostMap.put(e, cost);
		}
		final List<List<Link>> npRes = GraphUtils.getKLooplessShortestPaths(nodes, links, a.getNe(), b.getNe(), linkCostMap, k, -1, -1, -1, -1, -1, -1);
		final List<List<WFiber>> res = new ArrayList<>(npRes.size());
		for (List<Link> npPath : npRes)
			res.add(npPath.stream().map(e -> new WFiber(e)).collect(Collectors.toList()));
		return res;
	}


	/**
	 * Returns two maximally link disjoint WDM paths between the given nodes. If exactly one path exists, one path is returned. If no paths exist, no paths are returned.
	 * @param a the origin node
	 * @param b the end node
	 * @param optionalCostMapOrElseLatency an optional map of the cost to assign to traversing each WFiber link, if none,
	 *        the cost is assumed to be the WFiber latency
	 * @return a list of up to k paths, where each path is a sequence of WFiber objects forming a path from a to b
	 */
	public List<List<WFiber>> getTwoMaximallyLinkAndNodeDisjointWdmPaths(WNode a, WNode b, Optional<Map<WFiber, Double>> optionalCostMapOrElseLatency)
	{
		final List<Node> nodes = getNodes().stream().map(e->e.getNe()).collect(Collectors.toList());
		final List<Link> links = getFibers().stream().map(e->e.getNe()).collect(Collectors.toList());
		final SortedMap<Link,Double> costMap = new TreeMap<> ();
		for (Link e : links) 
			if (optionalCostMapOrElseLatency.isPresent()) 
				costMap.put(e, optionalCostMapOrElseLatency.get().get(new WFiber(e)));
			else
				costMap.put(e, (new WFiber(e)).getPropagationDelayInMs());
		final List<List<Link>> sps = GraphUtils.getTwoMaximumLinkAndNodeDisjointPaths(nodes, links, a.getNe(), b.getNe(), costMap);
		final List<List<WFiber>> res = new ArrayList<> ();
		for (List<Link> ee : sps) res.add(ee.stream().map(e->new WFiber(e)).collect(Collectors.toList()));
		return res;
	}

	
	void checkInThisWNet(WAbstractNetworkElement... ne)
	{
		for (WAbstractNetworkElement e : ne)
			if (e.getNetPlan() != this.getNetPlan()) throw new Net2PlanException("The element does not belong to this network");
	}

	void checkInThisWNetCol(Collection<? extends WAbstractNetworkElement> ne)
	{
		for (WAbstractNetworkElement e : ne)
			if (e.getNetPlan() != this.getNetPlan()) throw new Net2PlanException("The element does not belong to this network");
	}

	/**
	 * Indicates if a network is an exact copy of other network in every aspect
	 * @param other see above
	 * @return see above
	 */
	public boolean isDeepCopy(WNet other)
	{
		if (this == other) return true;
		return this.getNe().isDeepCopy(other.getNe());
	}

	/**
	 * Adds an instance of the VNF of the given type, in the indicated host node, applying it the given name
	 * @param hostNode see above
	 * @param name see above
	 * @param type see above
	 * @return the created instance
	 */
	public WVnfInstance addVnfInstance(WNode hostNode, String name, WVnfType type)
	{
		if (name.contains(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)) throw new Net2PlanException("Names cannot contain the character: " + WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);
		if (type.isConstrainedToBeInstantiatedOnlyInUserDefinedNodes()) if (!type.getValidMetroNodesForInstantiation().contains(hostNode.getName())) throw new Net2PlanException("An instance of that VNF type cannot be instantiated in that host node");
		final Map<Resource, Double> cpuRamHdOccupied = new HashMap<>();
		cpuRamHdOccupied.put(hostNode.getCpuBaseResource(), type.getOccupCpu());
		cpuRamHdOccupied.put(hostNode.getRamBaseResource(), type.getOccupRamGBytes());
		cpuRamHdOccupied.put(hostNode.getHdBaseResource(), type.getOccupHdGBytes());
		final Resource resource = np.addResource(type.getVnfTypeName(), name, Optional.of(hostNode.getNe()), type.getMaxInputTrafficPerVnfInstance_Gbps(), "Gbps", cpuRamHdOccupied, type.getProcessingTime_ms(), null);
		return new WVnfInstance(resource);
	}

	/**
	 * Adds an instance of the VNF of the given type, in the indicated host node, applying it the given name, and other info
	 * @param hostNode  see above
	 * @param name see above
	 * @param type see above
	 * @param capacityGbps see above
	 * @param occupiedCpu see above
	 * @param occupiedRamGB see above
	 * @param occupiedHdGb see above
	 * @param processingTimeMs see above
	 * @return see above
	 */
	public WVnfInstance addVnfInstance(WNode hostNode, String name, String type , double capacityGbps , double occupiedCpu , double occupiedRamGB , double occupiedHdGb , double processingTimeMs)
	{
		if (name.contains(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)) throw new Net2PlanException("Names cannot contain the character: " + WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);
		final Map<Resource, Double> cpuRamHdOccupied = new HashMap<>();
		cpuRamHdOccupied.put(hostNode.getCpuBaseResource(), occupiedCpu);
		cpuRamHdOccupied.put(hostNode.getRamBaseResource(), occupiedRamGB);
		cpuRamHdOccupied.put(hostNode.getHdBaseResource(), occupiedHdGb);
		final Resource resource = np.addResource(type, name, Optional.of(hostNode.getNe()), capacityGbps, "Gbps", cpuRamHdOccupied, processingTimeMs, null);
		return new WVnfInstance(resource);
	}

	
	/**
	 * Get the propagation delay given a list of Fibers to traverse.
	 * @param fiberLinks see above
	 * @return see above
	 */
	public double getPropagationDelay(List<WFiber> fiberLinks)
	{
		double propagationDelay = 0;
		for (WFiber fiber : fiberLinks)
		{
			propagationDelay = propagationDelay + fiber.getNe().getPropagationDelayInMs();
		}

		return propagationDelay;
	}

	@Override
	public String toString()
	{
		return "WNet";
	}

	public SortedSet<WIpLink> getNodePairIpLinks(WNode a, WNode b)
	{
		if (!isWithIpLayer ()) return new TreeSet<> ();
		final Set<Link> links = getNe().getNodePairLinks(a.getNe(), b.getNe(), false, getIpLayer().get().getNe());
		return links.stream().map(e -> new WIpLink(e)).collect(Collectors.toCollection(TreeSet::new));
	}
	public SortedSet<WFiber> getNodePairFibers (WNode a, WNode b)
	{
		if (!isWithWdmLayer ()) return new TreeSet<> ();
		final Set<Link> links = getNe().getNodePairLinks(a.getNe(), b.getNe(), false, getWdmLayer().get().getNe());
		return links.stream().map(e -> new WFiber(e)).collect(Collectors.toCollection(TreeSet::new));
	}
	
	@Override
	public void checkConsistency()
	{
    	final Function<NetworkLayer,Boolean> isIp = d -> { final WAbstractNetworkElement ee = getWElement(d).orElse(null); return ee == null? false : ee.isLayerIp(); };
    	final Function<NetworkLayer,Boolean> isWdm = d -> { final WAbstractNetworkElement ee = getWElement(d).orElse(null); return ee == null? false : ee.isLayerWdm(); };
    	final int numIpLayers = (int) getNe().getNetworkLayers().stream().filter(e->isIp.apply(e)).count ();
    	final int numWdmLayers = (int) getNe().getNetworkLayers().stream().filter(e->isWdm.apply(e)).count ();
    	if (numIpLayers > 1 || numWdmLayers > 1) throw new Net2PlanException ();
    	if (numIpLayers + numWdmLayers != getNe().getNumberOfLayers()) 
    		throw new Net2PlanException ("Num IP layers: " + numIpLayers + ", num WDM layers: " + numWdmLayers + ", num layers: " + getNe().getNumberOfLayers());

    	getNodes().forEach(e->e.checkConsistency());
		getFibers().forEach(e->e.checkConsistency());
		getLightpaths().forEach(e->e.checkConsistency());
		getLightpathRequests().forEach(e->e.checkConsistency());
		getIpLinks().forEach(e->e.checkConsistency());
		getIpUnicastDemands().forEach(e->e.checkConsistency());
		getServiceChains().forEach(e->e.checkConsistency());
		getServiceChainRequests().forEach(e->e.checkConsistency());
		getVnfInstances().forEach(e->e.checkConsistency());
		if (getIpLayer().isPresent()) getIpLayer().get().checkConsistency();
		if (getWdmLayer ().isPresent()) getWdmLayer().get().checkConsistency();
		getSrgs().forEach(s->s.checkConsistency());
	}

	public Optional<WTYPE> getWType (NetworkElement e)
	{
		if (e.getNetPlan() != this.getNe()) return Optional.empty();
		
		if (e == getNe ()) return Optional.of (WTYPE.WNet);
		
		if (e instanceof Node) return Optional.of (WTYPE.WNode);
		if (e instanceof Link)
		{
			final Link ee = (Link) e;
			final boolean isIpLayer = ee.getLayer().getName ().equals(WNetConstants.ipLayerName);
			final boolean isWdmLayer = ee.getLayer().getName ().equals(WNetConstants.wdmLayerName);
			assert isIpLayer || isWdmLayer;
			if (isIpLayer)
			{
				if (new WNode (ee.getOriginNode()).isVirtualNode()) return Optional.empty();
				if (new WNode (ee.getDestinationNode()).isVirtualNode()) return Optional.empty();
				return Optional.of (WTYPE.WIpLink);
			}
			if (isWdmLayer) return Optional.of (WTYPE.WFiber);
		}
		if (e instanceof Demand)
		{
			final Demand ee = (Demand) e;
			final boolean isIpLayer = ee.getLayer().getName ().equals(WNetConstants.ipLayerName);
			final boolean isWdmLayer = ee.getLayer().getName ().equals(WNetConstants.wdmLayerName);
			assert isIpLayer || isWdmLayer;
			if (isIpLayer && ee.isCoupled())
			{
				assert ee.isCoupledInSameLayer();
				assert ee.hasTag(WNetConstants.TAGDEMANDIP_INDICATIONISBUNDLE);
				return Optional.empty(); // bundles
			}
			if (isIpLayer && new WNode (ee.getIngressNode()).isRegularNode())
				return Optional.of (WTYPE.WIpUnicastDemand);
			if (isIpLayer && new WNode (ee.getIngressNode()).isVirtualNode())
				return Optional.of (WTYPE.WServiceChainRequest);
			if (isWdmLayer) return Optional.of (WTYPE.WLightpathRequest);
		}
		if (e instanceof Route)
		{
			final Route ee = (Route) e;
			final boolean isIpLayer = ee.getLayer().getName ().equals(WNetConstants.ipLayerName);
			final boolean isWdmLayer = ee.getLayer().getName ().equals(WNetConstants.wdmLayerName);
			assert isIpLayer || isWdmLayer;
			if (isIpLayer)
			{
				if (ee.getDemand().isCoupled()) return Optional.empty();
				if (isIpLayer && new WNode (ee.getIngressNode()).isRegularNode())
				{
					assert new WNode (ee.getEgressNode()).isRegularNode();
					return Optional.of (WTYPE.WIpSourceRoutedConnection);
				}
				assert new WNode (ee.getIngressNode()).isVirtualNode();
				assert new WNode (ee.getEgressNode()).isVirtualNode();
				return Optional.of (WTYPE.WServiceChain);
			}
			if (isWdmLayer) return Optional.of (WTYPE.WLightpath);
		}
		if (e instanceof Resource)
		{
			final Resource ee = (Resource) e;
			return Optional.of (WTYPE.WVnfInstance);
		}
		if (e instanceof SharedRiskGroup)
		{
			final SharedRiskGroup ee = (SharedRiskGroup) e;
			return Optional.of(WTYPE.WSharedRiskGroup);
		}
		if (e instanceof NetworkLayer)
		{
			final NetworkLayer ee = (NetworkLayer) e;
			if (ee.getName().equals(WNetConstants.wdmLayerName)) return Optional.of (WTYPE.WLayerWdm);
			if (ee.getName().equals(WNetConstants.ipLayerName)) return Optional.of (WTYPE.WLayerIp);
		}
		return Optional.empty();
	}
	
	public Optional<WAbstractNetworkElement> getWElement (NetworkElement e)
	{
		if (e.getNetPlan() != this.getNe()) return Optional.empty();
		
		if (e == getNe ()) return Optional.of (this);
		
		if (e instanceof Node) return Optional.of (new WNode ((Node) e));
		if (e instanceof Link)
		{
			final Link ee = (Link) e;
			final boolean isIpLayer = ee.getLayer().getName ().equals(WNetConstants.ipLayerName);
			final boolean isWdmLayer = ee.getLayer().getName ().equals(WNetConstants.wdmLayerName);
			assert isIpLayer || isWdmLayer;
			if (isIpLayer)
			{
				if (new WNode (ee.getOriginNode()).isVirtualNode()) return Optional.empty();
				if (new WNode (ee.getDestinationNode()).isVirtualNode()) return Optional.empty();
				return Optional.of (new WIpLink(ee));
			}
			if (isWdmLayer) return Optional.of (new WFiber(ee));
		}
		if (e instanceof Demand)
		{
			final Demand ee = (Demand) e;
			final boolean isIpLayer = ee.getLayer().getName ().equals(WNetConstants.ipLayerName);
			final boolean isWdmLayer = ee.getLayer().getName ().equals(WNetConstants.wdmLayerName);
			assert isIpLayer || isWdmLayer;
			if (isIpLayer && ee.isCoupled())
			{
				assert ee.isCoupledInSameLayer();
				assert ee.hasTag(WNetConstants.TAGDEMANDIP_INDICATIONISBUNDLE);
				return Optional.empty(); // bundles
			}
			if (isIpLayer && new WNode (ee.getIngressNode()).isRegularNode())
				return Optional.of (new WIpUnicastDemand(ee));
			if (isIpLayer && new WNode (ee.getIngressNode()).isVirtualNode())
				return Optional.of (new WServiceChainRequest(ee));
			if (isWdmLayer) return Optional.of (new WLightpathRequest(ee));
		}
		if (e instanceof Route)
		{
			final Route ee = (Route) e;
			final boolean isIpLayer = ee.getLayer().getName ().equals(WNetConstants.ipLayerName);
			final boolean isWdmLayer = ee.getLayer().getName ().equals(WNetConstants.wdmLayerName);
			assert isIpLayer || isWdmLayer;
			if (isIpLayer)
			{
				if (ee.getDemand().isCoupled()) return Optional.empty();
				if (isIpLayer && new WNode (ee.getIngressNode()).isRegularNode())
				{
					assert new WNode (ee.getEgressNode()).isRegularNode();
					return Optional.of (new WIpSourceRoutedConnection(ee));
				}
				assert new WNode (ee.getIngressNode()).isVirtualNode();
				assert new WNode (ee.getEgressNode()).isVirtualNode();
				return Optional.of (new WServiceChain(ee));
			}
			if (isWdmLayer) return Optional.of (new WLightpath(ee));
		}
		if (e instanceof Resource)
		{
			final Resource ee = (Resource) e;
			return Optional.of (new WVnfInstance(ee));
		}
		if (e instanceof SharedRiskGroup)
		{
			final SharedRiskGroup ee = (SharedRiskGroup) e;
			return Optional.of (new WSharedRiskGroup(ee));
		}
		if (e instanceof NetworkLayer)
		{
			final NetworkLayer ee = (NetworkLayer) e;
			if (ee.getName().equals(WNetConstants.wdmLayerName)) return Optional.of(new WLayerWdm(ee));
			if (ee.getName().equals(WNetConstants.ipLayerName)) return Optional.of(new WLayerIp(ee));
		}
		
		return Optional.empty();
	}
	
	/** Returns the node element with the given id, if any
	 * @param id see above
	 * @return see above
	 */
	public Optional<WNode> getNodeFromId (long id)
	{
		final Node n = getNe().getNodeFromId(id);
		if (n == null) return Optional.empty();
		if (getWType(n).equals (Optional.of(WTYPE.WNode))) return Optional.of(new WNode (n));
		return Optional.empty();
	}
	
	/** Returns the fiber element with the given id, if any
	 * @param id see above
	 * @return see above
	 */
	public Optional<WFiber> getFiberFromId (long id)
	{
		final Link n = getNe().getLinkFromId(id);
		if (n == null) return Optional.empty();
		if (getWType(n).equals (Optional.of(WTYPE.WFiber))) return Optional.of(new WFiber (n));
		return Optional.empty();
	}
	
	@Override
	public WTYPE getWType() { return WTYPE.WNet; }


	/** Sets all the resources as non-failed
	 */
	public void setFailureStateAllUp ()
	{
		getNe().setAllNodesFailureState(true);
		for (NetworkLayer layer : getNe().getNetworkLayers ())
			getNe().setAllLinksFailureState(true , layer);
	}





	/* Segment Routing information */
	public void addFlexAlgo(String k, String calculationType, String weightType, Optional<List<String>> usingSidList, Optional<List<String>> ipLinkList)
	{
		// Check that all parameters are not empty
		assert !k.isEmpty(); assert  !calculationType.isEmpty(); assert !weightType.isEmpty();

		// Check that k, calculationType and weightType are integers
//		try
//		{
//			Integer.parseInt(k);
//			Integer.parseInt(calculationType);
//			Integer.parseInt(weightType);
//		} catch (NumberFormatException nfe) { System.out.println("Flex algo data is incorrect"); return; }




		StringBuilder flexValue = new StringBuilder(calculationType + " " + weightType + " ");


		if(usingSidList.isPresent())
		{
			StringBuilder sidList = new StringBuilder();
			for(String sid: usingSidList.get())
				sidList.append(",").append(sid);

			flexValue.append(" ").append(sidList.toString());
		}

		if(ipLinkList.isPresent())
		{
			StringBuilder linkList = new StringBuilder();
			for(String link: ipLinkList.get())
				linkList.append(",").append(link);

			flexValue.append(" ").append(linkList.toString());
		}

		getNe().setAttribute("flexAlgo_"+k, flexValue.toString());
	}

	public JSONObject getFlexAlgo(String k)
	{
		if(!getNe().getAttributes().containsKey("flexAlgo_" + k)) return null;

		String flexValue = getNe().getAttribute("flexAlgo_" + k);
		String[] fields = flexValue.split(" ");

		JSONObject jsonFlexAlgo = new JSONObject();
		jsonFlexAlgo.put("k", k);
		jsonFlexAlgo.put("calculationType", fields[0]);
		jsonFlexAlgo.put("weightType", fields[1]);

		String[] sidListValues = fields[2].split(",");
		jsonFlexAlgo.put("usingSidList", Arrays.asList(sidListValues));

		String[] ipLinkListValues = fields[3].split(",");
		jsonFlexAlgo.put("ipLinkList", Arrays.asList(ipLinkListValues));

		return jsonFlexAlgo;
	}

	public void removeFlexAlgo(String k)
	{
		if(getNe().getAttributes().containsKey("flexAlgo_"+k)) getNe().removeAttribute("flexAlgo_"+k);
	}

	
}
