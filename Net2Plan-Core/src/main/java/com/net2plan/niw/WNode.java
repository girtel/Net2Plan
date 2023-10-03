/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.awt.geom.Point2D;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.niw.WNetConstants.WTYPE;
import com.net2plan.utils.StringUtils;

/**
 * This class represents a node in the network, capable of initiating or ending IP and WDM links, as well as lightpaths
 * and service chains
 */

/**
 * @author Pablo
 */
public class WNode extends WAbstractNetworkElement
{
//	public enum OPTICALSWITCHTYPE
//	{
//		ROADM (
//				e->new TreeSet<> (Arrays.asList(e)) , // add
//				e->new TreeSet<> (), // drop
//				(e1,e2)->new TreeSet<> (Arrays.asList(e2)), // express
//				e->new TreeSet<> () // unavoidable: propagates whatever you do
//				) 
//		, FILTERLESS_DROPANDWASTENOTDIRECTIONLESS (
//				e->new TreeSet<> (Arrays.asList(e)), // add
//				e->e.getB().getOutgoingFibers().stream().filter(ee->e.isBidirectional()? !ee.equals(e.getBidirectionalPair())    : true).collect(Collectors.toCollection(TreeSet::new)), // drop
//				(e1,e2)->e1.getB().getOutgoingFibers().stream().filter(ee->e1.isBidirectional()? !ee.equals(e1.getBidirectionalPair())    : true).collect(Collectors.toCollection(TreeSet::new)), // express
//				e->e.getB().getOutgoingFibers().stream().filter(ee->e.isBidirectional()? !ee.equals(e.getBidirectionalPair())    : true).collect(Collectors.toCollection(TreeSet::new)) // unavoidable: propagates whatever you do
//				);
//
//		private final Function<WFiber , SortedSet<WFiber>> outFibersIfAddToOutputFiber;
//		private final Function<WFiber , SortedSet<WFiber>> outFibersIfDropFromInputFiber;
//		private final BiFunction<WFiber , WFiber , SortedSet<WFiber>> outFibersIfExpressFromInputToOutputFiber;
//		private final Function<WFiber , SortedSet<WFiber>> outFibersUnavoidablePropagationFromInputFiber;
//		
//		private OPTICALSWITCHTYPE(Function<WFiber, SortedSet<WFiber>> outFibersIfAddToOutputFiber, 
//				Function<WFiber, SortedSet<WFiber>> outFibersIfDropFromInputFiber, 
//				BiFunction<WFiber, WFiber, SortedSet<WFiber>> outFibersIfExpressFromInputToOutputFiber,
//				Function<WFiber, SortedSet<WFiber>> outFibersUnavoidablePropagationFromInputFiber)
//		{
//			this.outFibersIfAddToOutputFiber = outFibersIfAddToOutputFiber;
//			this.outFibersIfDropFromInputFiber = outFibersIfDropFromInputFiber;
//			this.outFibersIfExpressFromInputToOutputFiber = outFibersIfExpressFromInputToOutputFiber;
//			this.outFibersUnavoidablePropagationFromInputFiber = outFibersUnavoidablePropagationFromInputFiber;
//		}
//		public static OPTICALSWITCHTYPE getDefault () { return ROADM; }
//		public boolean isRoadm () { return this == ROADM; }
//		public String getShortName () { return isRoadm()? "ROADM" : "Filterless"; }
//		public boolean isDropAndWaste () { return this == OPTICALSWITCHTYPE.FILTERLESS_DROPANDWASTENOTDIRECTIONLESS; }
//		public Function<WFiber, SortedSet<WFiber>> getOutFibersIfAddToOutputFiber()
//		{
//			return this.outFibersIfAddToOutputFiber;
//		}
//		public Function<WFiber, SortedSet<WFiber>> getOutFibersIfDropFromInputFiber()
//		{
//			return this.outFibersIfDropFromInputFiber;
//		}
//		public BiFunction<WFiber, WFiber, SortedSet<WFiber>> getOutFibersIfExpressFromInputToOutputFiber()
//		{
//			return this.outFibersIfExpressFromInputToOutputFiber;
//		}
//		public Function<WFiber, SortedSet<WFiber>> getOutFibersUnavoidablePropagationFromInputFiber()
//		{
//			return this.outFibersUnavoidablePropagationFromInputFiber;
//		}
//	}

    static final String ATTNAMECOMMONPREFIX = NIWNAMEPREFIX + "Node_";
    private static final String ATTNAMESUFFIX_TYPE = "type";
    private static final String ATTNAMESUFFIX_ISCONNECTEDTOCORE = "isConnectedToNetworkCore";
    private static final String RESOURCETYPE_CPU = WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER + "CPU";
    private static final String RESOURCETYPE_RAM = WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER + "RAM";
    private static final String RESOURCETYPE_HD = WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER + "HD";
    private static final String ATTNAMESUFFIX_ARBITRARYPARAMSTRING = "ArbitraryString";
    private static final String ATTNAME_OPTICALSWITCHTYPE = "ATTNAME_OPTICALSWITCHTYPE";
    static final String ATTNAME_OPTICALSWITCHTYPEINITSTRING = "ATTNAME_OPTICALSWITCHTYPE_INITSTRING";
    private static final String ATTNAMESUFFIX_OADMNUMADDDROPMODULES = "oadmNumAddDropModules";
    private static final String ATTNAMESUFFIX_HASDIRECTEDMODULES = "oadmHasDirectedAddDropModules";
    private static final String ATTNAMESUFFIX_SR_SIDLIST = "sidList";

    public void setArbitraryParamString(String s)
    {
        getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ARBITRARYPARAMSTRING, s);
    }

    public String getArbitraryParamString()
    {
        return getNe().getAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ARBITRARYPARAMSTRING, "");
    }

    private final Node n;

    public WNode(Node n)
    {
        super(n, Optional.empty()); this.n = n;
    }

    Resource getCpuBaseResource()
    {
        final Set<Resource> cpuResources = n.getResources(RESOURCETYPE_CPU); assert cpuResources.size() < 2;
        if (cpuResources.isEmpty()) setTotalNumCpus(0); assert n.getResources(RESOURCETYPE_CPU).size() == 1;
        return n.getResources(RESOURCETYPE_CPU).iterator().next();
    }

    Resource getRamBaseResource()
    {
        final Set<Resource> ramResources = n.getResources(RESOURCETYPE_RAM); assert ramResources.size() < 2;
        if (ramResources.isEmpty()) setTotalRamGB(0); assert n.getResources(RESOURCETYPE_RAM).size() == 1;
        return n.getResources(RESOURCETYPE_RAM).iterator().next();
    }

    Resource getHdBaseResource()
    {
        final Set<Resource> hdResources = n.getResources(RESOURCETYPE_HD); assert hdResources.size() < 2;
        if (hdResources.isEmpty()) setTotalHdGB(0); assert n.getResources(RESOURCETYPE_HD).size() == 1;
        return n.getResources(RESOURCETYPE_HD).iterator().next();
    }

    public boolean isVirtualNode()
    {
        return n.hasTag(WNetConstants.TAGNODE_INDICATIONVIRTUALORIGINNODE) || n.hasTag(WNetConstants.TAGNODE_INDICATIONVIRTUALDESTINATIONNODE);
    }

    public boolean isRegularNode()
    {
        return !isVirtualNode();
    }

    @Override
    public Node getNe()
    {
        return (Node) associatedNpElement;
    }

    /**
     * Returns the number of add/drop modules that are directionless (and thus are implemented as regular degrees), in the OADM
     *
     * @return see above
     */
    public int getOadmNumAddDropDirectionlessModules()
    {
        final int index = getNe().getAttributeAsDouble(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_OADMNUMADDDROPMODULES, 1.0).intValue();
        return index < 0 ? 0 : index;
    }

    /**
     * Indicates if this node architecture has directed add/drop modules in the degrees, where to place the lighpaths
     *
     * @return see above
     */
    public boolean isOadmWithDirectedAddDropModulesInTheDegrees()
    {
        final int index = getNe().getAttributeAsDouble(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_HASDIRECTEDMODULES, 1.0).intValue();
        return index != 0;
    }


    /**
     * Indicates if this node architecture has directed add/drop modules in the degrees, where to place the lighpaths
     *
     * @param isWithDirectedAddDropModules see above
     */
    public void setIsOadmWithDirectedAddDropModulesInTheDegrees(boolean isWithDirectedAddDropModules)
    {
        getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_HASDIRECTEDMODULES, isWithDirectedAddDropModules ? 1 : 0);
    }

    /**
     * Sets the number of add/drop modules in the OADM that are directionless and thus are implemented as regular degrees (greater or equal to zero)
     *
     * @param numModules see above
     */
    public void setOadmNumAddDropDirectionlessModules(int numModules)
    {
        getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_OADMNUMADDDROPMODULES, numModules < 0 ? 0 : numModules);
    }

    /**
     * Returns the lightpaths added in this node, in directed (non-directionless) modules
     *
     * @return see above
     */
    public SortedSet<WLightpath> getAddedLightpathsInDirectedModule()
    {
        return getAddedLigtpaths().stream().filter(lp -> !lp.getDirectionlessAddModuleIndexInOrigin().isPresent()).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the lightpaths dropped in this node, in directed (non-directionless) modules
     *
     * @return see above
     */
    public SortedSet<WLightpath> getDroppedLightpathsInDirectedModule()
    {
        return getDroppedLigtpaths().stream().filter(lp -> !lp.getDirectionlessDropModuleIndexInDestination().isPresent()).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the lightpaths added in this node, connected in the indicated add directionless module index
     *
     * @param directionlessAddModuleIndex see above
     * @return see above
     */
    public SortedSet<WLightpath> getAddedLightpathsInDirectionlessModule(int directionlessAddModuleIndex)
    {
        if (directionlessAddModuleIndex < 0) return new TreeSet<>();
        return getAddedLigtpaths().stream().filter(lp -> lp.getDirectionlessAddModuleIndexInOrigin().isPresent()).filter(lp -> lp.getDirectionlessAddModuleIndexInOrigin().get() == directionlessAddModuleIndex).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the lightpaths added in this node, connected in the indicated drop directionless module index
     *
     * @param directionlessDropModuleIndex see above
     * @return see above
     */
    public SortedSet<WLightpath> getDroppedLightpathsInDirectionlessModule(int directionlessDropModuleIndex)
    {
        if (directionlessDropModuleIndex < 0) return new TreeSet<>();
        return getDroppedLigtpaths().stream().filter(lp -> lp.getDirectionlessDropModuleIndexInDestination().isPresent()).filter(lp -> lp.getDirectionlessDropModuleIndexInDestination().get() == directionlessDropModuleIndex).collect(Collectors.toCollection(TreeSet::new));
    }


    /**
     * Returns the node name, which must be unique among all the nodes
     *
     * @return see above
     */
    public String getName()
    {
        return n.getName();
    }

    /**
     * Sets the node name, which must be unique among all the nodes
     *
     * @param name see above
     */
    public void setName(String name)
    {
        if (name == null) WNet.ex("Names cannot be null");
        if (name.contains(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER))
            throw new Net2PlanException("Names cannot contain the character: " + WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);
        if (getNet().getNodes().stream().anyMatch(n -> n.getName().equals(name))) WNet.ex("Names cannot be repeated");
        if (name.contains(" ")) throw new Net2PlanException("Names cannot contain spaces"); n.setName(name);
    }


    /**
     * Sets the icon to show in Net2Plan GUI for node at the WDM layer, and the relative size respect to other nodes.
     *
     * @param urlIcon      see above
     * @param relativeSize see above
     */
    public void setWdmIcon(URL urlIcon, double relativeSize)
    {
        if (!getNet().isWithWdmLayer()) return; getNe().setUrlNodeIcon(getWdmNpLayer().get(), urlIcon, relativeSize);
    }

    /**
     * Sets the icon to show in Net2Plan GUI for node at the IP layer, and the relative size respect to other nodes.
     *
     * @param urlIcon      see above
     * @param relativeSize see above
     */
    public void setIpIcon(URL urlIcon, double relativeSize)
    {
        if (!getNet().isWithIpLayer()) return; getNe().setUrlNodeIcon(getIpNpLayer().get(), urlIcon, relativeSize);
    }

    /**
     * Returns the url of the icon specified by the user for the WDM layer, or null if none
     *
     * @return the url
     */
    public Optional<URL> getUrlNodeIconWdm()
    {
        if (!getNet().isWithWdmLayer()) return Optional.empty();
        return Optional.ofNullable(getNe().getUrlNodeIcon(getWdmNpLayer().get()));
    }

    /**
     * Returns the url of the icon specified by the user for the WDM layer, or null if none
     *
     * @return the url
     */
    public Optional<URL> getUrlNodeIconIp()
    {
        if (!getNet().isWithIpLayer()) return Optional.empty();
        return Optional.ofNullable(getNe().getUrlNodeIcon(getIpNpLayer().get()));
    }

    /**
     * Returns the relative size in GUI Net2Plan visualization for the icon in the WDM layer fos this node
     *
     * @return see above
     */
    public double getIconRelativeSizeInWdm()
    {
        if (!getNet().isWithWdmLayer()) return 1.0; return getNe().getNodeIconRelativeSize(getWdmNpLayer().get());
    }

    /**
     * Returns the relative size in GUI Net2Plan visualization for the icon in the WDM layer fos this node
     *
     * @return see above
     */
    public double getIconRelativeSizeInIp()
    {
        if (!getNet().isWithIpLayer()) return 1.0; return getNe().getNodeIconRelativeSize(getIpNpLayer().get());
    }

    /**
     * Returns the (X,Y) node position
     *
     * @return see above
     */
    public Point2D getNodePositionXY()
    {
        return n.getXYPositionMap();
    }

    /**
     * Sets the (X,Y) node position
     *
     * @param position see above
     */
    public void setNodePositionXY(Point2D position)
    {
        n.setXYPositionMap(position);
    }

    /**
     * Returns the user-defined node type
     *
     * @return see above
     */
    public String getType()
    {
        return getAttributeOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_TYPE, "");
    }

    /**
     * Sets the user-defined node type
     *
     * @param type see above
     */
    public void setType(String type)
    {
        n.setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_TYPE, type);
    }

    /**
     * Returns if this node is connected to a core node (core nodes are not in the design)
     *
     * @return see above
     */
    public boolean isConnectedToNetworkCore()
    {
        return getAttributeAsBooleanOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ISCONNECTEDTOCORE, WNetConstants.WNODE_DEFAULT_ISCONNECTEDTOCORE);
    }

    /**
     * Sets if this node is assumed to be connected to a core node (core nodes are not in the design)
     *
     * @param isConnectedToCore see above
     */
    public void setIsConnectedToNetworkCore(boolean isConnectedToCore)
    {
        n.setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ISCONNECTEDTOCORE, Boolean.valueOf(isConnectedToCore).toString());
    }

    /**
     * Returns the user-defined node population
     *
     * @return see above
     */
    public double getPopulation()
    {
        return n.getPopulation();
    }

    /**
     * Sets the user-defined node population
     *
     * @param population see above
     */
    public void setPoputlation(double population)
    {
        n.setPopulation(population);
    }

    /**
     * Returns the number of CPUs available in the node for instantiation of VNFs
     *
     * @return see above
     */
    public double getTotalNumCpus()
    {
        return n.getResources(RESOURCETYPE_CPU).stream().mapToDouble(r -> r.getCapacity()).sum();
    }

    /**
     * Sets the number of CPUs available in the node for instantiation of VNFs
     *
     * @param totalNumCpus see above
     */
    public void setTotalNumCpus(double totalNumCpus)
    {
        final Set<Resource> res = n.getResources(RESOURCETYPE_CPU);
        if (res.size() > 1) throw new Net2PlanException("Format error"); if (res.isEmpty())
        n.getNetPlan().addResource(RESOURCETYPE_CPU, RESOURCETYPE_CPU, Optional.of(n), totalNumCpus, RESOURCETYPE_CPU, new HashMap<>(), 0.0, null);
    else res.iterator().next().setCapacity(totalNumCpus, new HashMap<>());
    }

    /**
     * Returns the total RAM (in GBytes) available in the node for instantiation of VNFs
     *
     * @return see above
     */
    public double getTotalRamGB()
    {
        return n.getResources(RESOURCETYPE_RAM).stream().mapToDouble(r -> r.getCapacity()).sum();
    }

    /**
     * Sets the total RAM (in GBytes) available in the node for instantiation of VNFs
     *
     * @param totalRamGB see above
     */
    public void setTotalRamGB(double totalRamGB)
    {
        final Set<Resource> res = n.getResources(RESOURCETYPE_RAM);
        if (res.size() > 1) throw new Net2PlanException("Format error"); if (res.isEmpty())
        res.add(n.getNetPlan().addResource(RESOURCETYPE_RAM, RESOURCETYPE_RAM, Optional.of(n), totalRamGB, "GB", new HashMap<>(), 0.0, null));
    else res.iterator().next().setCapacity(totalRamGB, new HashMap<>());
    }

    /**
     * Returns the total hard disk size (in GBytes) available in the node for instantiation of VNFs
     *
     * @return see above
     */
    public double getTotalHdGB()
    {
        return n.getResources(RESOURCETYPE_HD).stream().mapToDouble(r -> r.getCapacity()).sum();
    }

    /**
     * Sets the total hard disk size (in GBytes) available in the node for instantiation of VNFs
     *
     * @param totalHdGB see above
     */
    public void setTotalHdGB(double totalHdGB)
    {
        final Set<Resource> res = n.getResources(RESOURCETYPE_HD);
        if (res.size() > 1) throw new Net2PlanException("Format error"); if (res.isEmpty())
        res.add(n.getNetPlan().addResource(RESOURCETYPE_HD, RESOURCETYPE_HD, Optional.of(n), totalHdGB, "GB", new HashMap<>(), 0.0, null));
    else res.iterator().next().setCapacity(totalHdGB, new HashMap<>());
    }

    /**
     * Returns the current number of occupied CPUs by the instantiated VNFs
     *
     * @return see above
     */
    public double getOccupiedCpus()
    {
        return getCpuBaseResource().getOccupiedCapacity();
    }

    /**
     * Returns the current amount of occupied hard-disk (in giga bytes) by the instantiated VNFs
     *
     * @return see above
     */
    public double getOccupiedHdGB()
    {
        return getHdBaseResource().getOccupiedCapacity();
    }

    /**
     * Returns the current amount of occupied RAM (in giga bytes) by the instantiated VNFs
     *
     * @return see above
     */
    public double getOccupiedRamGB()
    {
        return getRamBaseResource().getOccupiedCapacity();
    }

    /**
     * Indicates if the node is up or down (failed)
     *
     * @return see above
     */
    public boolean isUp()
    {
        return n.isUp();
    }

    /**
     * Returns the set of nodes, for which there is a fiber form this node to them
     *
     * @return see above
     */
    public SortedSet<WNode> getNeighborNodesViaOutgoingFibers()
    {
        return getOutgoingFibers().stream().map(e -> e.getB()).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of nodes, for which there is a fiber from them to this node
     *
     * @return see above
     */
    public SortedSet<WNode> getNeighborNodesViaIncomingFibers()
    {
        return getIncomingFibers().stream().map(e -> e.getA()).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of outgoing fibers of the node
     *
     * @return see above
     */
    public SortedSet<WFiber> getOutgoingFibers()
    {
        if (!getNet().isWithWdmLayer()) return new TreeSet<>();
        return n.getOutgoingLinks(getNet().getWdmLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isWFiber();
        }).map(ee -> new WFiber(ee)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of incoming fibers to the node
     *
     * @return see above
     */
    public SortedSet<WFiber> getIncomingFibers()
    {
        if (!getNet().isWithWdmLayer()) return new TreeSet<>();
        return n.getIncomingLinks(getNet().getWdmLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isWFiber();
        }).map(ee -> new WFiber(ee)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of outgoing IP links of the node
     *
     * @return see above
     */
    public SortedSet<WIpLink> getOutgoingIpLinks()
    {
        if (!getNet().isWithIpLayer()) return new TreeSet<>();
        return n.getOutgoingLinks(getNet().getIpLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isIpLink();
        }).map(ee -> new WIpLink(ee)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the IP connections, realizing IP unicast demands, that are initiated in this node
     *
     * @return see above
     */
    public SortedSet<WIpSourceRoutedConnection> getOutgoingIpConnections()
    {
        if (!getNet().isWithIpLayer()) return new TreeSet<>();
        return n.getOutgoingRoutes(getNet().getIpLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isIpSourceRoutedConnection();
        }).map(ee -> new WIpSourceRoutedConnection(ee)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the IP connections, realizing IP unicast demands, that are ended in this node
     *
     * @return see above
     */
    public SortedSet<WIpSourceRoutedConnection> getIncomingIpConnections()
    {
        if (!getNet().isWithIpLayer()) return new TreeSet<>();
        return n.getIncomingRoutes(getNet().getIpLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isIpSourceRoutedConnection();
        }).map(ee -> new WIpSourceRoutedConnection(ee)).collect(Collectors.toCollection(TreeSet::new));
    }


    /**
     * Returns the set of incoming IP links to the node
     *
     * @return see above
     */
    public SortedSet<WIpLink> getIncomingIpLinks()
    {
        if (!getNet().isWithIpLayer()) return new TreeSet<>();
        return n.getIncomingLinks(getNet().getIpLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isIpLink();
        }).map(ee -> new WIpLink(ee)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of outgoing lightpath requests of the node
     *
     * @return see above
     */
    public SortedSet<WLightpathRequest> getOutgoingLigtpathRequests()
    {
        if (!getNet().isWithWdmLayer()) return new TreeSet<>();
        return n.getOutgoingDemands(getNet().getWdmLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isLightpathRequest();
        }).map(ee -> new WLightpathRequest(ee)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of outgoing lightpaths of the node
     *
     * @return see above
     */
    public SortedSet<WLightpath> getOutgoingLigtpaths()
    {
        return getOutgoingLigtpathRequests().stream().map(e -> e.getLightpaths()).flatMap(e -> e.stream()).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of incoming lightpaths of the node
     *
     * @return see above
     */
    public SortedSet<WLightpath> getIncomingLigtpaths()
    {
        return getIncomingLigtpathRequests().stream().map(e -> e.getLightpaths()).flatMap(e -> e.stream()).collect(Collectors.toCollection(TreeSet::new));
    }


    /**
     * Returns the set of incoming lightpath requests to the node
     *
     * @return see above
     */
    public SortedSet<WLightpathRequest> getIncomingLigtpathRequests()
    {
        if (!getNet().isWithWdmLayer()) return new TreeSet<>();
        return n.getIncomingDemands(getNet().getWdmLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isLightpathRequest();
        }).map(ee -> new WLightpathRequest(ee)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of outgoing lightpaths of the node
     *
     * @return see above
     */
    public SortedSet<WLightpath> getAddedLigtpaths()
    {
        if (!getNet().isWithWdmLayer()) return new TreeSet<>();
        return n.getOutgoingRoutes(getNet().getWdmLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isLightpath();
        }).map(ee -> new WLightpath(ee)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of incoming, outgoing and traversing lightpaths to the node
     *
     * @return see above
     */
    public SortedSet<WLightpath> getInOutOrTraversingLigtpaths()
    {
        if (!getNet().isWithWdmLayer()) return new TreeSet<>();
        return n.getAssociatedRoutes(getNet().getWdmLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isLightpath();
        }).map(ee -> new WLightpath(ee)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of incoming, outgoing and traversing lightpaths to the node
     *
     * @return see above
     */
    public SortedSet<WLightpath> getExpressSwitchedLightpaths()
    {
        if (!getNet().isWithWdmLayer()) return new TreeSet<>();
        return n.getAssociatedRoutes(getNet().getWdmLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isLightpath();
        }).map(ee -> new WLightpath(ee)).filter(lp -> lp.getNodesWhereThisLightpathIsExpressOpticallySwitched().contains(this)).collect(Collectors.toCollection(TreeSet::new));
    }


    /**
     * Returns the set of incoming lightpaths to the node
     *
     * @return see above
     */
    public SortedSet<WLightpath> getDroppedLigtpaths()
    {
        if (!getNet().isWithWdmLayer()) return new TreeSet<>();
        return n.getIncomingRoutes(getNet().getWdmLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isLightpath();
        }).map(ee -> new WLightpath(ee)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of outgoing service chain requests of the node: those which have the node as a potential injection
     * node
     *
     * @return see above
     */
    public SortedSet<WServiceChainRequest> getOutgoingServiceChainRequests()
    {
        return getNet().getServiceChainRequests().stream().filter(sc -> sc.getPotentiallyValidOrigins().contains(this)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of outgoing unicast IP demands of the node: those which have the node as origin
     *
     * @return see above
     */
    public SortedSet<WIpUnicastDemand> getOutgoingIpUnicastDemands()
    {
        if (!getNet().isWithIpLayer()) return new TreeSet<>();
        return getNe().getOutgoingDemands(getNet().getIpLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isIpUnicastDemand();
        }).map(d -> new WIpUnicastDemand(d)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of incoming unicast IP demands of the node: those which have the node as destination
     *
     * @return see above
     */
    public SortedSet<WIpUnicastDemand> getIncomingIpUnicastDemands()
    {
        if (!getNet().isWithIpLayer()) return new TreeSet<>();
        return getNe().getIncomingDemands(getNet().getIpLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isIpUnicastDemand();
        }).map(d -> new WIpUnicastDemand(d)).collect(Collectors.toCollection(TreeSet::new));
    }


    /**
     * Returns the set of incoming service chain requests of the node: those which have the node as a potential end node
     *
     * @return see above
     */
    public SortedSet<WServiceChainRequest> getIncomingServiceChainRequests()
    {
        return getNet().getServiceChainRequests().stream().filter(sc -> sc.getPotentiallyValidDestinations().contains(this)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of outgoing service chains of the node, including those starting in a VNF in the node
     *
     * @return see above
     */
    public SortedSet<WServiceChain> getOutgoingServiceChains()
    {
        if (!getNet().isWithIpLayer()) return new TreeSet<>();
        return n.getAssociatedRoutes(getNet().getIpLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isServiceChain();
        }).map(ee -> new WServiceChain(ee)).filter(sc -> sc.getA().equals(this)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of incoming service chains to the node, including those ended in a VNF in the node
     *
     * @return see above
     */
    public SortedSet<WServiceChain> getIncomingServiceChains()
    {
        if (!getNet().isWithIpLayer()) return new TreeSet<>();
        return n.getAssociatedRoutes(getNet().getIpLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isServiceChain();
        }).map(ee -> new WServiceChain(ee)).filter(sc -> sc.getB().equals(this)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the set of incoming, outgoing and traversing service chains to the node.
     *
     * @return see above
     */
    public SortedSet<WServiceChain> getInOutOrTraversingServiceChains()
    {
        if (!getNet().isWithIpLayer()) return new TreeSet<>();
        return n.getAssociatedRoutes(getNet().getIpLayer().get().getNe()).stream().filter(d -> {
            WTYPE t = getNet().getWType(d).orElse(null); return t == null ? false : t.isServiceChain();
        }).map(ee -> new WServiceChain(ee)).collect(Collectors.toCollection(TreeSet::new));
    }

    Link getIncomingLinkFromAnycastOrigin()
    {
        assert getNet().isWithIpLayer();
        return n.getNetPlan().getNodePairLinks(getNet().getAnycastOriginNode().getNe(), n, false, getIpNpLayer().get()).stream().findFirst().orElseThrow(() -> new RuntimeException());
    }

    Link getOutgoingLinkToAnycastDestination()
    {
        assert getNet().isWithIpLayer();
        return n.getNetPlan().getNodePairLinks(n, getNet().getAnycastDestinationNode().getNe(), false, getIpNpLayer().get()).stream().findFirst().orElseThrow(() -> new RuntimeException());
    }

    /**
     * Sets the node as up (working, non-failed)
     */
    public void setAsUp()
    {
        n.setFailureState(true); final SortedSet<WLightpathRequest> affectedDemands = new TreeSet<>();
        getOutgoingFibers().forEach(f -> affectedDemands.addAll(f.getTraversingLpRequestsInAtLeastOneLp()));
        getIncomingFibers().forEach(f -> affectedDemands.addAll(f.getTraversingLpRequestsInAtLeastOneLp()));
        for (WLightpathRequest lpReq : affectedDemands)
            lpReq.updateNetPlanObjectAndPropagateUpwards();
    }

    /**
     * Sets the node as down (failed), so traversing IP links or lightpaths become down
     */
    public void setAsDown()
    {
        n.setFailureState(false); final SortedSet<WLightpathRequest> affectedDemands = new TreeSet<>();
        getOutgoingFibers().forEach(f -> affectedDemands.addAll(f.getTraversingLpRequestsInAtLeastOneLp()));
        getIncomingFibers().forEach(f -> affectedDemands.addAll(f.getTraversingLpRequestsInAtLeastOneLp()));
        for (WLightpathRequest lpReq : affectedDemands)
            lpReq.updateNetPlanObjectAndPropagateUpwards();
    }

    /**
     * Removes this node, and all the ending and initiated links, or traversing lightpaths or service chains
     */
    public void remove()
    {
        this.setAsDown(); n.remove();
    }

    @Override
    public String toString()
    {
        return "Node " + getName();
    }

    /**
     * Returns all the VNF instances in this node, of any type
     *
     * @return see above
     */
    public SortedSet<WVnfInstance> getAllVnfInstances()
    {
        return n.getResources().stream().filter(r -> !r.getType().contains(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)).map(r -> new WVnfInstance(r)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns all the VNF instances in this node, of the given type
     *
     * @param type see above
     * @return see above
     */
    public SortedSet<WVnfInstance> getVnfInstances(String type)
    {
        if (type.contains(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER))
            throw new Net2PlanException("Names cannot contain the character: " + WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);
        return n.getResources().stream().filter(r -> !r.getType().contains(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)).map(r -> new WVnfInstance(r)).filter(v -> v.getType().equals(type)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns all the VNF instances in this node, of the given type
     *
     * @return see above
     */
    public SortedSet<WVnfInstance> getVnfInstances()
    {
        return n.getResources().stream().map(r -> new WVnfInstance(r)).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Indicates if this node has a filterless brodcast operation with the add, dropped and express lightpaths
     *
     * @return see above
     */
    public IOadmArchitecture getOpticalSwitchingArchitecture()
    {
        try
        {
            final String classFullName = this.getNe().getAttribute(ATTNAMECOMMONPREFIX + ATTNAME_OPTICALSWITCHTYPE, null);
            if (classFullName == null) throw new RuntimeException();
            final Class classOfOadmArchit = Class.forName(classFullName);
            final IOadmArchitecture arc = (IOadmArchitecture) classOfOadmArchit.getConstructor().newInstance();
            arc.initialize(this); return arc;
        } catch (Exception e)
        {
//			e.printStackTrace();
            final OadmArchitecture_generic res = new OadmArchitecture_generic();
            this.getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAME_OPTICALSWITCHTYPE, OadmArchitecture_generic.class.getName());
            res.initialize(this); return res;
        }
    }

    public void setOpticalSwitchArchitecture(Class opticalArchitectureClass)
    {
        if (!IOadmArchitecture.class.isAssignableFrom(opticalArchitectureClass))
            throw new Net2PlanException("The architecture is not an instance of the appropriate class");
        getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAME_OPTICALSWITCHTYPE, opticalArchitectureClass.getName());
    }

    @Override
    void checkConsistency()
    {
        if (this.wasRemoved()) return; assert getIncomingFibers().stream().allMatch(e -> e.getB().equals(this));
        assert getOutgoingFibers().stream().allMatch(e -> e.getA().equals(this));
        assert getIncomingIpConnections().stream().allMatch(e -> e.getB().equals(this));
        assert getOutgoingIpConnections().stream().allMatch(e -> e.getA().equals(this));
        assert getIncomingLigtpathRequests().stream().allMatch(e -> e.getB().equals(this));
        assert getOutgoingLigtpathRequests().stream().allMatch(e -> e.getA().equals(this));
        assert getDroppedLigtpaths().stream().allMatch(e -> e.getB().equals(this));
        assert getAddedLigtpaths().stream().allMatch(e -> e.getA().equals(this));
        assert getIncomingServiceChainRequests().stream().allMatch(e -> e.getPotentiallyValidDestinations().contains(this));
        assert getOutgoingServiceChainRequests().stream().allMatch(e -> e.getPotentiallyValidOrigins().contains(this));
        assert getIncomingServiceChains().stream().allMatch(e -> e.getB().equals(this));
        assert getOutgoingServiceChains().stream().allMatch(e -> e.getA().equals(this));
        assert getInOutOrTraversingLigtpaths().stream().allMatch(e -> e.getSeqNodes().contains(this));
        assert getInOutOrTraversingServiceChains().stream().allMatch(e -> e.getSequenceOfTraversedIpNodesWithoutConsecutiveRepetitions().contains(this));
        assert getNeighborNodesViaIncomingFibers().stream().allMatch(n -> n.getNeighborNodesViaOutgoingFibers().contains(this));
        assert getNeighborNodesViaOutgoingFibers().stream().allMatch(n -> n.getNeighborNodesViaIncomingFibers().contains(this));
        assert getVnfInstances().stream().allMatch(v -> v.getHostingNode().equals(this));
    }


    /**
     * Returns the SRGs that this node belongs to, i.e. the ones that make this node fail
     *
     * @return see above
     */
    public SortedSet<WSharedRiskGroup> getSrgsThisElementIsAssociatedTo()
    {
        return getNe().getSRGs().stream().map(s -> new WSharedRiskGroup(s)).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public WTYPE getWType() { return WTYPE.WNode; }





    /* Segment Routing information */

    /**
     * Set the SID list that identifies this node inside the Segment Routing Network (inside the SRGB).
     * It is usually 2 SIDs, one that allows locating the node somehow undefined, and another one that usually
     * points to the loopback address, for Flex-Algo usages.
     * @param sidList - A String list containing the SIDs that belongs to this Node
     */
    public void setSidList(Optional<List<String>> sidList)
    {
        if (!sidList.isPresent()) return;

        // At this point, everything is correct, save the list
//        this.setAttribute(ATTNAMESUFFIX_SR_SIDLIST, StringUtils.collectionToString(sidList.get()));
        this.setAttribute(ATTNAMESUFFIX_SR_SIDLIST, Arrays.toString(sidList.get().toArray()));
    }


    /**
     * Returns the set of SID that belongs to the node, if applies.
     * @return - String list containing the SIDs that belong to this node.
     */
    public Optional<List<String>> getSidList()
    {
        assert getNe().getAttributes().containsKey(ATTNAMESUFFIX_SR_SIDLIST);

        // Get "sidList" from the attributes of the WNet, where it is stored
        final String sidListString = this.getNe().getAttribute(ATTNAMESUFFIX_SR_SIDLIST);

        if (sidListString == null || sidListString.isEmpty()) return Optional.empty();

        final String finalSidList = sidListString.substring(1, sidListString.length() - 1); // Remove "["..."]"

        List<String> sidList = Arrays.asList(finalSidList.split(","));

        return Optional.ofNullable(sidList);
    }

    public String getSidListAsNiceLookingString()
    {
        Optional<List<String>> sidList = getSidList();

        if(!sidList.isPresent() || sidList.get().isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for(String sid: sidList.get())
            sb.append(sid).append(" ");

        return  sb.toString();
    }


    /**
     * Checks whether this node has some SID, meaning it is participating in a SR network.
     * @return true or false, if the node has some SID.
     */
    public boolean participatesAtSegmentRouting()
    {
        return this.getNe().getAttributes().containsKey(ATTNAMESUFFIX_SR_SIDLIST);
    }
}
