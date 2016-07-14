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

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.mutable.MutableLong;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.Version;
import com.net2plan.internal.XMLUtils;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.libraries.GraphUtils.ClosedCycleRoutingException;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;

import cern.colt.function.tdouble.DoubleFunction;
import cern.colt.list.tdouble.DoubleArrayList;
import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

/**
 * <p>Class defining a complete multi-layer network structure. Layers may 
 * contain links, demands, routes and protection segments or forwarding rules, 
 * while nodes and SRGs are defined with a network-wide scope, that is, they 
 * appear in all layers. To simplify handling of single-layer designs, methods that may refer to 
 * layer-specific elements have an optiona input parameter of type {@link com.net2plan.interfaces.networkDesign.NetworkLayer NetworkLayer}.
 * In case of not using this optional parameter, the action
 * will be applied to the default layer (by default it is first defined layer), 
 * which can be modified using the {@link #setNetworkLayerDefault(NetworkLayer)} setNetworkLayerDefault())}
 * method.</p>
 * 
 * <p>Internal representation of the {@code NetPlan} object is based on:</p>
 * <ul>
 *     <li>{@link com.net2plan.interfaces.networkDesign.NetworkLayer NetworkLayer} objects.</li>
 *     <li>{@link com.net2plan.interfaces.networkDesign.Node Node} objects.</li>
 *     <li>{@link com.net2plan.interfaces.networkDesign.Link Link} objects.</li>
 *     <li>{@link com.net2plan.interfaces.networkDesign.Route Route} objects (depending on the selected {@link com.net2plan.utils.Constants.RoutingCycleType Routing Type}.</li>
 *     <li>{@link com.net2plan.interfaces.networkDesign.Demand Unicast} or {@link com.net2plan.interfaces.networkDesign.MulticastDemand Multicast} demand objects. </li>
 *     <li>{@link com.net2plan.interfaces.networkDesign.ProtectionSegment Protection Segment} objects.</li>
 *     <li>{@link com.net2plan.interfaces.networkDesign.SharedRiskGroup Shared Risk Group} objects</li>
 * </ul>
 *
 * <p>Each element has a unique identifier (represented as {@code long}) assigned the moment they are created, and two elements
 * (irregardless of their type) cannot share this identifier. This id is incremental (but not necessarily consecutive) and perpetual (i.e after removing an element identifiers are no renumbered).</p>
 * <p>Also, elements have an index number that identify them among their own type (nodes, links, etc.). Indexes are renumbered when an element is removed and are zero base, that is the first
 * element of its type is always 0 and the last is N-1 (where N is the total number of elements of the same type).</p>
 *
 * <p>An instance of a NetPlan object can be set as unmodifiable through the
 * {@link NetPlan#setModifiableState(boolean)} setModifiableState())} method. The instance will work
 * transparently as {@code NetPlan} object unless you try to change it. Calling 
 * any method that can potentially change the network (e.g. add/set/remove methods) 
 * throws an {@code UnsupportedOperationException}.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class NetPlan extends NetworkElement
{
	final static String TEMPLATE_CROSS_LAYER_UNITS_NOT_MATCHING = "Link capacity units at layer %d (%s) do not match demand traffic units at layer %d (%s)";
	final static String TEMPLATE_NO_MORE_NETWORK_ELEMENTS_ALLOWED = "Maximum number of identifiers in the design reached";
	final static String TEMPLATE_NO_MORE_NODES_ALLOWED = "Maximum node identifier reached";
	final static String TEMPLATE_NO_MORE_LINKS_ALLOWED = "Maximum link identifier for layer %d reached";
	final static String TEMPLATE_NO_MORE_DEMANDS_ALLOWED = "Maximum demand identifier for layer %d reached";
	final static String TEMPLATE_NO_MORE_MULTICASTDEMANDS_ALLOWED = "Maximum multicast demand identifier for layer %d reached";
	final static String TEMPLATE_NO_MORE_ROUTES_ALLOWED = "Maximum route identifier for layer %d reached";
	final static String TEMPLATE_NO_MORE_MULTICASTTREES_ALLOWED = "Maximum multicast tree identifier for layer %d reached";
	final static String TEMPLATE_NO_MORE_SEGMENTS_ALLOWED = "Maximum protection segment identifier for layer %d reached";
	final static String TEMPLATE_NO_MORE_SRGS_ALLOWED = "Maximum SRG identifier reached";
	final static String TEMPLATE_NOT_ACTIVE_LAYER = "Layer %d is not in the network";
	final static String TEMPLATE_NOT_ACTIVE_NODE = "Node %d is not in the network";
	final static String TEMPLATE_NOT_ACTIVE_LINK = "Link %d is not in the network";
	final static String TEMPLATE_NOT_ACTIVE_DEMAND = "Demand %d is not in the network";
	final static String TEMPLATE_NOT_ACTIVE_MULTICASTDEMAND = "Multicast demand %d is not in the network";
	final static String TEMPLATE_NOT_ACTIVE_ROUTE = "Route %d is not in the network";
	final static String TEMPLATE_NOT_ACTIVE_MULTICASTTREE = "Multicast tree %d is not in the network";
	final static String TEMPLATE_NOT_ACTIVE_FORWARDING_RULE = "Forwarding rule (%d, %d) is not in the network";
	final static String TEMPLATE_NOT_ACTIVE_SEGMENT = "Protection segment %d is not in the network";
	final static String TEMPLATE_NOT_ACTIVE_SRG = "SRG %d is not in the network";
	final static String TEMPLATE_DEMAND_NOT_ACTIVE_IN_LAYER = "Demand %d is not in layer %d";
	final static String TEMPLATE_LINK_NOT_ACTIVE_IN_LAYER = "Link %d is not in layer %d";
	final static String TEMPLATE_ROUTE_NOT_ACTIVE_IN_LAYER = "Route %d is not in layer %d";
	final static String TEMPLATE_MULTICASTDEMAND_NOT_ACTIVE_IN_LAYER = "Multicast demand %d is not in layer %d";
	final static String TEMPLATE_MULTICASTTREE_NOT_ACTIVE_IN_LAYER = "Multicast tree %d is not in layer %d";
	final static String TEMPLATE_PROTECTIONSEGMENT_NOT_ACTIVE_IN_LAYER = "Protection segment %d is not in layer %d";
	final static String TEMPLATE_FORWARDINGRULE_NOT_ACTIVE_IN_LAYER = "Forwarding rule %d is not in layer %d";
	final static String TEMPLATE_ROUTE_NOT_ALL_LINKS_SAME_LAYER = "Not all of the links of the route belong to the same layer";
	final static String TEMPLATE_SEGMENT_NOT_ALL_LINKS_SAME_LAYER = "Not all of the links of the protection segment belong to the same layer";
	final static String TEMPLATE_MULTICASTTREE_NOT_ALL_LINKS_SAME_LAYER = "Not all of the links of the multicast tree belong to the same layer";
	final static String TEMPLATE_NOT_OF_THE_SAME_LAYER_ROUTE_AND_SEGMENT = "The route %d and the segment %d are of different layers (%d, %d)";
	final static String UNMODIFIABLE_EXCEPTION_STRING = "Unmodifiable NetState object - can't be changed";
	final static String KEY_STRING_BIDIRECTIONALCOUPLE = "bidirectionalCouple";
	
	RoutingType DEFAULT_ROUTING_TYPE = RoutingType.SOURCE_ROUTING;
	boolean isModifiable;
	String networkDescription;
	String networkName;
	
	NetworkLayer defaultLayer;
	

	MutableLong nextElementId;
	
	ArrayList<NetworkLayer> layers;
	ArrayList<Node> nodes;
	ArrayList<SharedRiskGroup> srgs;

	HashSet<Node> cache_nodesDown;
	Map<Long,Node> cache_id2NodeMap;
	Map<Long,NetworkLayer> cache_id2LayerMap;
	Map<Long,Link> cache_id2LinkMap;
	Map<Long,Demand> cache_id2DemandMap;
	Map<Long,MulticastDemand> cache_id2MulticastDemandMap;
	Map<Long,Route> cache_id2RouteMap;
	Map<Long,MulticastTree> cache_id2MulticastTreeMap;
	Map<Long,ProtectionSegment> cache_id2ProtectionSegmentMap;
	Map<Long,SharedRiskGroup> cache_id2srgMap;

	DirectedAcyclicGraph<NetworkLayer, DemandLinkMapping> interLayerCoupling;

	
	/**
	 * <p>Default constructor. Creates an empty design</p>
	 *
	 * @since 0.4.0
	 */
	public NetPlan()
	{
		super (null , 0 , 0 , new AttributeMap());

		this.netPlan = this;
		DEFAULT_ROUTING_TYPE = RoutingType.SOURCE_ROUTING;
		isModifiable = true;
		
		networkDescription = "";
		networkName = "";
		
		nextElementId = new MutableLong(1);

		layers = new ArrayList<NetworkLayer> ();
		nodes = new ArrayList<Node> ();
		srgs= new ArrayList<SharedRiskGroup> ();
		
		cache_nodesDown = new HashSet<Node> ();
		this.cache_id2NodeMap = new HashMap <Long,Node> ();
		this.cache_id2LayerMap = new HashMap <Long,NetworkLayer> ();
		this.cache_id2srgMap= new HashMap<Long,SharedRiskGroup> ();
		this.cache_id2LinkMap = new HashMap<Long,Link> ();
		this.cache_id2DemandMap = new HashMap<Long,Demand> ();
		this.cache_id2MulticastDemandMap = new HashMap<Long,MulticastDemand> ();
		this.cache_id2RouteMap = new HashMap<Long,Route> ();
		this.cache_id2MulticastTreeMap = new HashMap<Long,MulticastTree> ();
		this.cache_id2ProtectionSegmentMap = new HashMap<Long,ProtectionSegment> ();
		
		interLayerCoupling = new DirectedAcyclicGraph<NetworkLayer, DemandLinkMapping>(DemandLinkMapping.class);

		defaultLayer = addLayer("Layer 0", null, null, null, null);
	}

	
	/********************************************************************************************************/
	/********************************************************************************************************/
	/********************************* INIT BLOCK ***********************************************************************/
	/********************************************************************************************************/
	/********************************************************************************************************/
	/********************************************************************************************************/
	/********************************************************************************************************/

  /**
	 * <p>Generates a new network design from a given {@code .n2p} file.</p>
	 * 
	 * @param file {@code .n2p} file
	 * @since 0.2.0
	 */
	public NetPlan(File file)
	{
		this();
		NetPlan np = loadFromFile(file);
		if (ErrorHandling.isDebugEnabled()) np.checkCachesConsistency();
		assignFrom(np);
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
//		System.out.println ("End NetPlan(File file): " + netPlan + " ----------- ");
	}

  /**
	 * <p>Generates a new network design from an input stream.</p>
	 *
	 * @param inputStream Input stream
	 * @since 0.3.1
	 */
	public NetPlan(InputStream inputStream)
	{
		this();
		
		try
		{
			XMLInputFactory2 xmlInputFactory = (XMLInputFactory2) XMLInputFactory2.newInstance();
			XMLStreamReader2 xmlStreamReader = (XMLStreamReader2) xmlInputFactory.createXMLStreamReader(inputStream);
		
			while(xmlStreamReader.hasNext())
			{
				int eventType = xmlStreamReader.next();
				switch (eventType)
				{
					case XMLEvent.START_ELEMENT:
						String elementName = xmlStreamReader.getName().toString();
						if (!elementName.equals("network")) throw new RuntimeException("Root element must be 'network'");
						
						IReaderNetPlan netPlanFormat;
						int index = xmlStreamReader.getAttributeIndex(null, "version");
						if (index == -1)
						{
							System.out.println ("Version 1");
							netPlanFormat = new ReaderNetPlan_v1();
						}
						else
						{
							int version = xmlStreamReader.getAttributeAsInt(index);
							switch(version)
							{
								case 2:
									System.out.println ("Version 2");
									netPlanFormat = new ReaderNetPlan_v2();
									break;

								case 3:
									System.out.println ("Version 3");
									netPlanFormat = new ReaderNetPlan_v3();
									break;

								case 4:
									System.out.println ("Version 4");
									netPlanFormat = new ReaderNetPlan_v4 ();
									break;

								default:
									throw new Net2PlanException("Wrong version number");
							}
						}

						netPlanFormat.create(this, xmlStreamReader);
						if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
						return;

					default:
						break;
				}
			}		
		}
		catch (Net2PlanException e)
		{
			if (ErrorHandling.isDebugEnabled()) ErrorHandling.printStackTrace(e);
			throw(e);
		}
		catch (FactoryConfigurationError | Exception e)
		{
			if (ErrorHandling.isDebugEnabled()) ErrorHandling.printStackTrace(e);
			throw new RuntimeException(e);
		}
		
		throw new Net2PlanException("Not a valid .n2p file");
	}

	/**
	 * <p>Returns the unique ids of the provided network elements.</p>
	 * @param collection Network elements
	 * @return Unique ids of the provided network elements. If the input {@code Collection} is a {@code Set}, the returned collection is a {@code HashSet}. If the input 
	 * {@code Collection} is a {@code List}, an {@code ArrayList} is returned}.
	 */
	public static Collection<Long> getIds (Collection<? extends NetworkElement> collection)
	{
		Collection<Long> res = (collection instanceof Set)? new HashSet<Long> () : new ArrayList<Long> (collection.size ()); 
		for (NetworkElement e : collection)
			res.add (e.id);
		return res;
	}

	/**
	 * <p>Returns the indexes of the provided network elements. </p>
	 * @param collection Network elements
	 * @return Indexes of the provided network elements. If the input {@code Collection} is a {@code Set}, the returned collection is a {@code HashSet}. If the input 
	 * {@code Collection} is a {@code List}, an {@code ArrayList} is returned}.
	 */
	public static Collection<Integer> getIndexes (Collection<? extends NetworkElement> collection)
	{
		Collection<Integer> res = (collection instanceof Set)? new HashSet<Integer> () : new ArrayList<Integer> (collection.size ()); 
		for (NetworkElement e : collection)
			res.add (e.index);
		return res;
	}

	/**
	 * <p>Static factory method to get a {@link com.net2plan.interfaces.networkDesign.NetPlan NetPlan} object from a {@code .n2p} file.</p>
	 * @param file Input file
	 * @return A network design
	 */
	public static NetPlan loadFromFile(File file)
	{
		try
		{
			InputStream inputStream = new FileInputStream(file);
			NetPlan np = new NetPlan(inputStream);
			if (ErrorHandling.isDebugEnabled()) np.checkCachesConsistency();
			return np;
		}
		catch(FileNotFoundException e)
		{
			throw new Net2PlanException(e.getMessage());
		}
	}

	/**
	 * <p>Removes the network element contained in the list which has the given index, and shifts the indexes of the rest of the elements accordingly.</p>
	 * @param x Network elements
	 * @param indexToRemove Index to remove
	 */
	static void removeNetworkElementAndShiftIndexes (ArrayList<? extends NetworkElement> x , int indexToRemove)
	{
		x.remove(indexToRemove);
		for (int newIndex = indexToRemove ; newIndex < x.size () ; newIndex ++)
			{ x.get(newIndex).index = newIndex; }
	}

	/**
	 * <p>Adds new traffic demands froma traffic matrix given as a {@code DoubleMatrix2D} object.</p>
	 *
	 * <p><b>Important: </b>Previous demands will be removed.</p>
	 * <p><b>Important</b>: Self-demands are not allowed.</p>
	 *
	 * @param trafficMatrix Traffix matrix where i-th row is the ingress node, the j-th column the egress node and each entry the offered traffic
	 * @param optionalLayerParameter Network layer to which to add the demands (optional)
	 * @return A list with the newly created demands
	 * @see com.net2plan.interfaces.networkDesign.Demand
	 */
	public List<Demand> addDemandsFromTrafficMatrix (DoubleMatrix2D trafficMatrix , NetworkLayer ... optionalLayerParameter)
	{
		trafficMatrix = NetPlan.adjustToTolerance(trafficMatrix);
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		List<Demand> demands = new LinkedList<Demand> ();
		if ((trafficMatrix.rows () != nodes.size ()) || (trafficMatrix.columns () != nodes.size ())) throw new Net2PlanException ("Wrong matrix size");
		for (int n1 = 0 ; n1 < nodes.size () ; n1 ++) for (int n2 = 0 ; n2 < nodes.size () ; n2 ++) if (n1 != n2) demands.add (addDemand (nodes.get(n1) , nodes.get(n2) , trafficMatrix.get(n1,n2) , null , layer));
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
		return demands;
	}
	
	/**
	 * <p>Adds a new traffic demand.</p>
	 * 
	 * <p><b>Important</b>: Self-demands are not allowed.</p>
	 *
	 * @param optionalLayerParameter Network layer to which add the demand (optional)
	 * @param ingressNode Ingress node
	 * @param egressNode Egress node
	 * @param offeredTraffic Offered traffic by this demand. It must be greater or equal than zero
	 * @param attributes Map for user-defined attributes ({@code null} means 'no attribute'). Each key represents the attribute name, whereas value represents the attribute value
	 * @return The newly added demand object
	 * @see com.net2plan.interfaces.networkDesign.Demand
	 */
	public Demand addDemand(Node ingressNode, Node egressNode, double offeredTraffic, Map<String, String> attributes , NetworkLayer ... optionalLayerParameter)
	{
		offeredTraffic = NetPlan.adjustToTolerance(offeredTraffic);
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		checkInThisNetPlan(ingressNode);
		checkInThisNetPlan(egressNode);
		if (ingressNode.equals (egressNode)) throw new Net2PlanException("Self-demands are not allowed");
		if (offeredTraffic < 0) throw new Net2PlanException ("Offered traffic must be non-negative");
		
		final long demandId = nextElementId.longValue();
		nextElementId.increment();

		Demand demand = new Demand (this , demandId , layer.demands.size () , layer , ingressNode , egressNode , offeredTraffic , new AttributeMap (attributes));

		cache_id2DemandMap.put (demandId , demand);
		layer.demands.add (demand);
		egressNode.cache_nodeIncomingDemands.add (demand);
		ingressNode.cache_nodeOutgoingDemands.add (demand);

		if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING)
		{
			layer.forwardingRules_f_de = DoubleFactory2D.sparse.appendRow(layer.forwardingRules_f_de , DoubleFactory1D.sparse.make (layer.links.size ()));
			layer.forwardingRules_x_de = DoubleFactory2D.sparse.appendRow(layer.forwardingRules_x_de , DoubleFactory1D.sparse.make (layer.links.size ()));
		}
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
		return demand;
	}

	/**
	 * <p>Adds two demands, one in each direction,.</p>
	 *
	 * <p><b>Important</b>: Self-demands are not allowed.</p>
	 *
	 * @param ingressNode Identifier of the ingress node
	 * @param egressNode Identifier of the egress node
	 * @param offeredTraffic Offered traffic by this demand. It must be greater or equal than zero
	 * @param attributes Map for user-defined attributes ({@code null} means 'no attribute'). Each key represents the attribute name, whereas value represents the attribute value
	 * @param optionalLayerParameter Network layer to which add the demand (optional)
	 * @see com.net2plan.interfaces.networkDesign.Demand
	 * @see com.net2plan.utils.Pair
	 * @return A pair object with the two newly created demands
	 */
	public Pair<Demand, Demand> addDemandBidirectional(Node ingressNode, Node egressNode, double offeredTraffic, Map<String, String> attributes , NetworkLayer ... optionalLayerParameter)
	{
		offeredTraffic = NetPlan.adjustToTolerance(offeredTraffic);
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		checkInThisNetPlan(ingressNode);
		checkInThisNetPlan(egressNode);
		if (ingressNode.equals (egressNode)) throw new Net2PlanException("Self-demands are not allowed");
		if (offeredTraffic < 0) throw new Net2PlanException ("Offered traffic must be non-negative");

		Demand d1 = addDemand (ingressNode, egressNode, offeredTraffic , attributes , layer);
		Demand d2 = addDemand (egressNode, ingressNode, offeredTraffic , attributes , layer);
		d1.setAttribute(KEY_STRING_BIDIRECTIONALCOUPLE , "" + d2.id);
		d2.setAttribute(KEY_STRING_BIDIRECTIONALCOUPLE , "" + d1.id);
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
		return Pair.of (d1,d2);
	}

	/**
	 * <p>Adds a new layer.</p>
	 *
	 * @param name Layer name ({@code null} means empty)
	 * @param description Layer description ({@code null} means empty)
	 * @param linkCapacityUnitsName Textual description of link capacity units ({@code null} means empty)
	 * @param demandTrafficUnitsName Textual description of demand traffic units ({@code null} means empty)
	 * @param attributes Map for user-defined attributes ({@code null} means 'no attribute'). Each key represents the attribute name, whereas value represents the attribute value
	 * @return The newly created layer object
	 * @see com.net2plan.interfaces.networkDesign.NetworkLayer
	 */
	public NetworkLayer addLayer(String name, String description, String linkCapacityUnitsName, String demandTrafficUnitsName, Map<String, String> attributes)
	{
		checkIsModifiable();
		if (name == null) name = "";
		if (description == null) description = "";
		if (linkCapacityUnitsName == null) linkCapacityUnitsName = "";
		if (demandTrafficUnitsName == null) demandTrafficUnitsName = "";
		
		final long id = nextElementId.longValue();
		nextElementId.increment();
		
		NetworkLayer layer = new NetworkLayer (this , id, layers.size() , demandTrafficUnitsName, description, name, linkCapacityUnitsName, new AttributeMap (attributes));
		
		interLayerCoupling.addVertex(layer);
		cache_id2LayerMap.put (id , layer);
		layers.add (layer);
		if (layers.size () == 1) defaultLayer = layer;

		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
		
		return layer;
	}

	/**
	 * <p>Creates a new layer and adds the links, routes etc. from the input layer. The number of nodes in the two designs must be the same (the unique ids may differ).
	 * Any coupling information in the origin layer is omitted. Links, demands, multicast demands, routes, multicast trees and protecion segments
	 * will have the same index within the layer in the origin and in the copied layers, but may have different unique ids.</p>
	 * @param origin Layer to be copied
	 * @return The newly created layer object
	 * @see com.net2plan.interfaces.networkDesign.NetworkLayer
	 * @see com.net2plan.interfaces.networkDesign.Node
	 */
	public NetworkLayer addLayerFrom (NetworkLayer origin)
	{
		checkIsModifiable();
		checkInThisNetPlan(origin);

		ArrayList<Node> originNodes = origin.netPlan.nodes;
		if (originNodes.size () != nodes.size ()) throw new Net2PlanException ("The number of nodes in the origin design and this design must be the same");
		
		NetworkLayer newLayer = addLayer(origin.name, origin.description, origin.linkCapacityUnitsName, origin.demandTrafficUnitsName, origin.getAttributes());
		
		for (Link originLink : origin.links)
			this.addLink(nodes.get(originLink.originNode.index) , nodes.get(originLink.destinationNode.index) , originLink.capacity , originLink.lengthInKm , originLink.propagationSpeedInKmPerSecond , originLink.attributes , newLayer);
		for (Demand originDemand : origin.demands)
			this.addDemand(nodes.get(originDemand.ingressNode.index) , nodes.get(originDemand.egressNode.index) , originDemand.offeredTraffic , originDemand.attributes , newLayer);
		for (MulticastDemand originDemand : origin.multicastDemands)
		{
			Set<Node> newEgressNodes = new HashSet<Node> (); for (Node originEgress : originDemand.egressNodes) newEgressNodes.add (nodes.get(originEgress.index));
			this.addMulticastDemand(nodes.get(originDemand.ingressNode.index) , newEgressNodes , originDemand.offeredTraffic , originDemand.attributes , newLayer);
		}
		for (ProtectionSegment originSegment: origin.protectionSegments)
		{
			List<Link> newSeqLinks = new LinkedList<Link> (); for (Link originLink : originSegment.seqLinks) newSeqLinks.add (newLayer.links.get (originLink.index));
			this.addProtectionSegment(newSeqLinks , originSegment.capacity , originSegment.attributes);
		}
		for (Route originRoute : origin.routes)
		{
			List<Link> newSeqLinksRealPath = new LinkedList<Link> (); for (Link originLink : originRoute.seqLinksRealPath) newSeqLinksRealPath.add (newLayer.links.get (originLink.index));
			Route newRoute = this.addRoute(newLayer.demands.get(originRoute.demand.index) , originRoute.carriedTraffic , originRoute.occupiedLinkCapacity , newSeqLinksRealPath , originRoute.attributes);
			List<Link> newSeqLinksAndProtectionSegment = new LinkedList<Link> (); 
			for (Link originLink : originRoute.seqLinksAndProtectionSegments) 
				if (originLink instanceof ProtectionSegment)
					newSeqLinksAndProtectionSegment.add (newLayer.protectionSegments.get (originLink.index));
				else
					newSeqLinksAndProtectionSegment.add (newLayer.links.get (originLink.index));
			newRoute.setSeqLinksAndProtectionSegments(newSeqLinksAndProtectionSegment);
		}
		for (MulticastTree originTree: origin.multicastTrees)
		{
			Set<Link> newSetLinks = new HashSet<Link> (); for (Link originLink : originTree.linkSet) newSetLinks.add (newLayer.links.get (originLink.index));
			this.addMulticastTree(newLayer.multicastDemands.get(originTree.demand.index) , originTree.carriedTraffic , originTree.occupiedLinkCapacity , newSetLinks , originTree.attributes);
		}

		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
		return newLayer;
	}

	/**
	 * <p>Adds a new link.</p>
	 *
	 * <p><b>Important</b>: Self-links are not allowed.</p>
	 *
	 * @param originNode Link origin node
	 * @param destinationNode Link destination node
	 * @param capacity Link capacity. It must be greather or equal to zero
	 * @param lengthInKm Link length. It must be greater or equal than zero. Physical distance between node pais can be otainer through the {@link #getNodePairEuclideanDistance(Node, Node) getNodePairEuclideanDistance}
	 *                   (for Euclidean distance) or {@link #getNodePairHaversineDistanceInKm(Node, Node) getNodePairHaversineDistanceInKm} (for airlinea distance) methods.
	 * @param propagationSpeedInKmPerSecond Link propagation speed in km/s. It must be greater than zero ({@code Double.MAX_VALUE} means no propagation delay, a non-positive value is changed into
	 *                                         200000 km/seg, a typical speed of light in the wires)
	 * @param attributes Map for user-defined attributes ({@code null} means 'no attribute'). Each key represents the attribute name, whereas value represents the attribute value
	 * @param optionalLayerParameter Network layer to which add the link (optional)
	 * @return The newly created link object
	 * @see com.net2plan.interfaces.networkDesign.Link
	 * @see com.net2plan.interfaces.networkDesign.Node
	 */
	public Link addLink(Node originNode, Node destinationNode , double capacity, double lengthInKm, double propagationSpeedInKmPerSecond, Map<String, String> attributes , NetworkLayer ... optionalLayerParameter)
	{
		capacity = NetPlan.adjustToTolerance(capacity);
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		checkInThisNetPlan(originNode);
		checkInThisNetPlan(destinationNode);
		if (originNode.equals (destinationNode)) throw new Net2PlanException("Self-links are not allowed");
		if (capacity < 0) throw new Net2PlanException ("Link capacity must be non-negative");
		if (lengthInKm < 0) throw new Net2PlanException ("Link length must be non-negative");
		if (propagationSpeedInKmPerSecond <= 0) throw new Net2PlanException ("Propagation speed must be positive");

		final long linkId = nextElementId.longValue();
		nextElementId.increment();

		Link link = new Link (this, linkId , layer.links.size (),  layer , originNode,  destinationNode , lengthInKm , propagationSpeedInKmPerSecond , capacity , new AttributeMap (attributes));

		cache_id2LinkMap.put (linkId , link);
		layer.links.add (link);
		originNode.cache_nodeOutgoingLinks.add (link);
		destinationNode.cache_nodeIncomingLinks.add (link);

		if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING)
		{
			layer.forwardingRules_f_de = DoubleFactory2D.sparse.appendColumn(layer.forwardingRules_f_de , DoubleFactory1D.sparse.make (layer.demands.size ()));
			layer.forwardingRules_x_de = DoubleFactory2D.sparse.appendColumn(layer.forwardingRules_x_de , DoubleFactory1D.sparse.make (layer.demands.size ()));
			layer.forwardingRules_Aout_ne = DoubleFactory2D.sparse.appendColumn(layer.forwardingRules_Aout_ne , DoubleFactory1D.sparse.make (netPlan.nodes.size ()));
			layer.forwardingRules_Ain_ne = DoubleFactory2D.sparse.appendColumn(layer.forwardingRules_Ain_ne , DoubleFactory1D.sparse.make (netPlan.nodes.size ()));
			layer.forwardingRules_Aout_ne.set(originNode.index, layer.forwardingRules_Aout_ne.columns()-1, 1.0);
			layer.forwardingRules_Ain_ne.set(destinationNode.index, layer.forwardingRules_Ain_ne.columns()-1, 1.0);
		}

		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
		return link;
	}

	/**
	 * <p>Adds two links, one in each direction.</p>
	 * <p><b>Important</b>: Self-links are not allowed.</p>
	 *
	 * @param originNode Link origin node
	 * @param destinationNode Link destination node
	 * @param capacity Link capacity. It must be greather or equal to zero
	 * @param lengthInKm Link length. It must be greater or equal than zero. Physical distance between node pais can be otainer through the {@link #getNodePairEuclideanDistance(Node, Node) getNodePairEuclideanDistance}
	 *                   (for Euclidean distance) or {@link #getNodePairHaversineDistanceInKm(Node, Node) getNodePairHaversineDistanceInKm} (for airlinea distance) methods.
	 * @param propagationSpeedInKmPerSecond Link propagation speed in km/s. It must be greater than zero ({@code Double.MAX_VALUE} means no propagation delay, a non-positive value is changed into
	 *                                         200000 km/seg, a typical speed of light in the wires)
	 * @param attributes Map for user-defined attributes ({@code null} means 'no attribute'). Each key represents the attribute name, whereas value represents the attribute value
	 * @param optionalLayerParameter Network layer to which add the links (optional)
	 * @return A {@code Pair} object with the two newly created links
	 * @see com.net2plan.interfaces.networkDesign.Link
	 * @see com.net2plan.utils.Pair
	 * @see com.net2plan.interfaces.networkDesign.Node
	 */
	public Pair<Link, Link> addLinkBidirectional(Node originNode, Node destinationNode , double capacity, double lengthInKm, double propagationSpeedInKmPerSecond, Map<String, String> attributes , NetworkLayer ... optionalLayerParameter)
	{
		capacity = NetPlan.adjustToTolerance(capacity);
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		checkInThisNetPlan(originNode);
		checkInThisNetPlan(destinationNode);
		if (originNode.equals (destinationNode)) throw new Net2PlanException("Self-links are not allowed");
		if (capacity < 0) throw new Net2PlanException ("Link capacity must be non-negative");
		if (lengthInKm < 0) throw new Net2PlanException ("Link length must be non-negative");
		if (propagationSpeedInKmPerSecond <= 0) throw new Net2PlanException ("Propagation speed must be positive");

		Link link1 = addLink (originNode, destinationNode , capacity, lengthInKm, propagationSpeedInKmPerSecond, attributes , layer);
		Link link2 = addLink (destinationNode , originNode , capacity, lengthInKm, propagationSpeedInKmPerSecond, attributes , layer);
		link1.setAttribute(KEY_STRING_BIDIRECTIONALCOUPLE , "" + link2.id);
		link2.setAttribute(KEY_STRING_BIDIRECTIONALCOUPLE , "" + link1.id);
		
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();

		return Pair.of (link1,link2);
	}

	/**
	 * <p>Adds a new multicast traffic demand.</p>
	 * <p><b>Important</b>: Ingress node cannot be also an egress node.</p>
	 *
	 * @param ingressNode Ingress node
	 * @param egressNodes Egress node
	 * @param offeredTraffic Offered traffic of the demand. It must be greater or equal than zero
	 * @param attributes Map for user-defined attributes ({@code null} means 'no attribute'). Each key represents the attribute name, whereas value represents the attribute value
	 * @param optionalLayerParameter Network layer to which add the demand (optional)
	 * @return The newly created multicast demand
	 * @see com.net2plan.interfaces.networkDesign.MulticastDemand
	 * @see com.net2plan.interfaces.networkDesign.Node
	 */
	public MulticastDemand addMulticastDemand(Node ingressNode , Set<Node> egressNodes , double offeredTraffic, Map<String, String> attributes , NetworkLayer ... optionalLayerParameter)
	{
		offeredTraffic = NetPlan.adjustToTolerance(offeredTraffic);
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		checkInThisNetPlan(ingressNode);
		checkInThisNetPlan(egressNodes);
		if (egressNodes.contains (ingressNode)) throw new Net2PlanException("The ingress node cannot be also an egress node");
		if (offeredTraffic < 0) throw new Net2PlanException ("Offered traffics must be non-negative");

		for (Node n : egressNodes) n.checkAttachedToNetPlanObject(this); 
		if (egressNodes.contains(ingressNode)) throw new Net2PlanException("The ingress node is also an egress node of the multicast demand");
		if (offeredTraffic < 0) throw new Net2PlanException ("Offered traffic must be non-negative");

		final long demandId = nextElementId.longValue();
		nextElementId.increment();
		
		MulticastDemand demand = new MulticastDemand (this , demandId , layer.multicastDemands.size () , layer , ingressNode , egressNodes , offeredTraffic , new AttributeMap (attributes));

		cache_id2MulticastDemandMap.put (demandId , demand);
		layer.multicastDemands.add (demand);
		for (Node n : egressNodes) n.cache_nodeIncomingMulticastDemands.add (demand);
		ingressNode.cache_nodeOutgoingMulticastDemands.add (demand);
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
		return demand;
	}

	/**
	 * <p>Adds a new traffic multicast tree.</p>
	 *
	 * <p><b>Important</b>: Routing type must be {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}.</p>
	 *
	 * @param demand Multi cast demand to be associated with the tree
	 * @param carriedTraffic Carried traffic. It must be greater or equal than zero
	 * @param occupiedLinkCapacity Occupied link capacity. If -1, it will be assumed to be equal to the carried traffic. Otherwise, it must be greater or equal than zero
	 * @param linkSet {@code Set} of links of the multicast tree
	 * @param attributes Map for user-defined attributes ({@code null} means 'no attribute'). Each key represents the attribute name, whereas value represents the attribute value
	 * @return The newly reated multicast tree
	 * @see com.net2plan.interfaces.networkDesign.MulticastTree
	 * @see com.net2plan.interfaces.networkDesign.MulticastDemand
	 * @see com.net2plan.interfaces.networkDesign.Link
	 */
	public MulticastTree addMulticastTree(MulticastDemand demand , double carriedTraffic, double occupiedLinkCapacity, Set<Link> linkSet , Map<String, String> attributes)
	{
		carriedTraffic = NetPlan.adjustToTolerance(carriedTraffic);
		occupiedLinkCapacity = NetPlan.adjustToTolerance(occupiedLinkCapacity);
		checkIsModifiable();
		checkInThisNetPlan(demand);
		checkMulticastTreeValidityForDemand(linkSet , demand);
		if (carriedTraffic < 0) throw new Net2PlanException ("Carried traffic must be non-negative");
		if (occupiedLinkCapacity < 0) occupiedLinkCapacity = carriedTraffic; 
		NetworkLayer layer = demand.layer;
		
		final long treeId = nextElementId.longValue();
		nextElementId.increment();

		MulticastTree tree = new MulticastTree(this, treeId , layer.multicastTrees.size() , demand, linkSet , new AttributeMap(attributes));

		cache_id2MulticastTreeMap.put(treeId, tree);
		layer.multicastTrees.add(tree);
		boolean treeIsUp = true;
		for (Node node : tree.cache_traversedNodes) { node.cache_nodeAssociatedulticastTrees.add (tree); if (!node.isUp) treeIsUp = false; }
		for (Link link : linkSet)
		{ 
			if (!link.isUp) treeIsUp = false;
			link.cache_traversingTrees.add (tree); 
		}
		if (!treeIsUp) layer.cache_multicastTreesDown.add (tree);
		demand.cache_multicastTrees.add (tree); 
		tree.setCarriedTraffic(carriedTraffic , occupiedLinkCapacity);
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
		return tree;
	}

	/**
	 * <p>Adds a new node to the network. Nodes are associated to all layers.</p>
	 * @param xCoord Node position in x-axis
	 * @param yCoord Node position in y-axis
	 * @param name Node name ({@code null} will be converted to "Node " + node identifier)
	 * @param attributes Map for user-defined attributes ({@code null} means 'no attribute'). Each key represents the attribute name, whereas value represents the attribute value
	 * @return The newly created node object
	 * @see com.net2plan.interfaces.networkDesign.Node
	 */
	public Node addNode(double xCoord, double yCoord, String name, Map<String, String> attributes)
	{
		checkIsModifiable();

		final long nodeId = nextElementId.longValue();
		nextElementId.increment();
		
		Node node = new Node(this, nodeId, nodes.size(), xCoord, yCoord, name, new AttributeMap(attributes));
		
		nodes.add (node);
		cache_id2NodeMap.put (nodeId , node);
		
		for (NetworkLayer layer : layers)
		{
			final int E = layer.links.size();
			if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING)
			{
				layer.forwardingRules_Aout_ne = DoubleFactory2D.sparse.appendRow(layer.forwardingRules_Aout_ne , DoubleFactory1D.sparse.make (E));
				layer.forwardingRules_Ain_ne = DoubleFactory2D.sparse.appendRow(layer.forwardingRules_Ain_ne , DoubleFactory1D.sparse.make (E));
			}
		}
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
		return node;
	}

	/**
	 * <p>Adds a new protection segment. All the links must belong to the same layer</p>
	 *
	 * <p><b>Important</b>: Routing type must be {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}.</p>
	 * @param sequenceOfLinks Sequence of links
	 * @param reservedCapacity Reserved capacity in each link from {@code sequenceOfLinks}. It must be greater or equal than zero
	 * @param attributes Map for user-defined attributes ({@code null} means 'no attribute'). Each key represents the attribute name, whereas value represents the attribute value
	 * @return The newly created protetion segment object
	 * @see com.net2plan.interfaces.networkDesign.ProtectionSegment
	 * @see com.net2plan.interfaces.networkDesign.Link
	 */
	public ProtectionSegment addProtectionSegment(List<Link> sequenceOfLinks, double reservedCapacity, Map<String, String> attributes)
	{
		reservedCapacity = NetPlan.adjustToTolerance(reservedCapacity);

		checkIsModifiable();
		NetworkLayer layer = checkContiguousPath (sequenceOfLinks , null , null , null);
		checkInThisNetPlanAndLayer(sequenceOfLinks , layer);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		if (reservedCapacity < 0) throw new Net2PlanException ("Reserved capacity must be non-negative");
		if (sequenceOfLinks.size () != new HashSet<Link> (sequenceOfLinks).size()) throw new Net2PlanException ("Protection segments cannot traverse the same link more than once");
		
		
		final long segmentId = nextElementId.longValue();
		nextElementId.increment();
		ProtectionSegment segment = new ProtectionSegment (this, segmentId , layer.protectionSegments.size (),  sequenceOfLinks , reservedCapacity , new AttributeMap (attributes));

		cache_id2ProtectionSegmentMap.put (segmentId , segment);
		layer.protectionSegments.add (segment);
		boolean segmentIsUp = true;
		for (Link link : sequenceOfLinks) { link.cache_traversingSegments.add (segment); if (!link.isUp) segmentIsUp = false; }
		for (Node node : segment.seqNodes) {node.cache_nodeAssociatedSegments.add (segment) ; if (!node.isUp) segmentIsUp = false; }
		if (!segmentIsUp) layer.cache_segmentsDown.add (segment);
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
		return segment;
	}

	/**
	 * <p>Adds a new traffic route </p>
	 *
	 * <p><b>Important</b>: Routing type must be {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}.</p>
	 * @param demand Demand associated to the route
	 * @param carriedTraffic Carried traffic. It must be greater or equal than zero
	 * @param occupiedLinkCapacity Occupied link capacity, it must be greater or equal than zero.
	 * @param sequenceOfLinks Sequence of links traversed by the route
	 * @param attributes Map for user-defined attributes ({@code null} means 'no attribute'). Each key represents the attribute name, whereas value represents the attribute value
	 * @return The newly created route object
	 * @see com.net2plan.interfaces.networkDesign.Route
	 * @see com.net2plan.interfaces.networkDesign.Demand
	 * @see com.net2plan.interfaces.networkDesign.Link
	 */
	public Route addRoute(Demand demand , double carriedTraffic, double occupiedLinkCapacity, List<Link> sequenceOfLinks, Map<String, String> attributes)
	{
		carriedTraffic = NetPlan.adjustToTolerance(carriedTraffic);
		occupiedLinkCapacity = NetPlan.adjustToTolerance(occupiedLinkCapacity);

		checkIsModifiable();
		checkInThisNetPlan(demand);
		checkPathValidityForDemand(sequenceOfLinks, demand);
		demand.layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		
		
		if (carriedTraffic < 0) throw new Net2PlanException ("Carried traffic must be non-negative");
		if (occupiedLinkCapacity < 0) occupiedLinkCapacity = carriedTraffic;
		NetworkLayer layer = demand.layer;
		
		final long routeId = nextElementId.longValue();
		nextElementId.increment();
		
		Route route = new Route(this, routeId, layer.routes.size(), demand , sequenceOfLinks , new AttributeMap(attributes));
		
		layer.routes.add(route);
		cache_id2RouteMap.put (routeId,route);
		boolean isUpThisRoute = true;
		for (Node node : route.seqNodesRealPath) { node.cache_nodeAssociatedRoutes.add (route); if (!node.isUp) isUpThisRoute = false; } 
		for (Link link : route.seqLinksRealPath) 
		{ 
			Integer numPassingTimes = link.cache_traversingRoutes.get (route); if (numPassingTimes == null) numPassingTimes = 1; else numPassingTimes ++; 
			link.cache_traversingRoutes.put (route , numPassingTimes);
			if (!link.isUp) isUpThisRoute = false;
		}
		demand.cache_routes.add (route);
		if (!isUpThisRoute) layer.cache_routesDown.add (route);
		route.setCarriedTraffic(carriedTraffic , occupiedLinkCapacity);
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
		return route;
	}

	/**
	 * <p>Adds traffic routes specified by those paths that satisfy the candidate path list options described below. Existing routes will not be removed.</p>
	 *
	 * <p><b>Important</b>: Routing type must be {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}.</p>
	 *
	 * <p>The candidate path list elaborated contains a set of paths
	 * computed for each unicast demand in the network, based on the k-shortest path idea. In general, for every demand k
	 * paths are computed with the shortest weight according to some weights
	 * assigned to the links.</p>
	 * <p>The computation of paths can be configured via {@code "parameter=value"} options. There are several options to
	 * configure, which can be combined:</p>
	 *
	 * <ul>
	 * 		<li>{@code K}: Number of desired loopless shortest paths (default: 3). If <i>K'</i>&lt;{@code K} different paths are found between the demand node pairs, then only <i>K'</i> paths are
	 * 		included in the candidate path list</li>
	 * 		<li>{@code maxLengthInKm}: Maximum path length measured in kilometers allowed (default: Double.MAX_VALUE)</li>
	 * 		<li>{@code maxNumHops}: Maximum number of hops allowed (default: Integer.MAX_VALUE)</li>
	 * 		<li>{@code maxPropDelayInMs}: Maximum propagation delay in miliseconds allowed in a path (default: Double.MAX_VALUE)</li>
	 * 		<li>{@code maxRouteCost}: Maximum path weight allowed (default: Double.MAX_VALUE)</li>
	 * 		<li>{@code maxRouteCostFactorRespectToShortestPath}: Maximum path weight factor with respect to the shortest path weight (default: Double.MAX_VALUE)</li>
	 *		<li>{@code maxRouteCostRespectToShortestPath}: Maximum path weight with respect to the shortest path weight (default: Double.MAX_VALUE). While the previous one is a multiplicative factor, this one is an additive factor</li>
	 * </ul>
	 *
	 * @param costs Link weight vector for the shortest path algorithm
	 * @param paramValuePairs Parameters to be passed to the class to tune its operation. An even number of {@code String} is to be passed. For each {@code String} pair, first {@code String}
	 *                           must be the name of the parameter, second a {@code String} with its value. If no name-value pairs are set, default values are used
	 * @return Map with all the computed paths (values) per demands (keys)
	 */
	public Map<Demand,List<List<Link>>> computeUnicastCandidatePathList (double [] costs , String... paramValuePairs)
	{
		return computeUnicastCandidatePathList(defaultLayer, costs, paramValuePairs);
	}

	/**
	 * <p>Computes a list of disjoint path pairs for each demand, using the paths in the input candidate path list given.</p>
	 *
	 * @param cpl Candidate path list per demand
	 * @param disjointType Type of disjointness: 0 for SRG-disjoint, 1 for link and node disjoint, other value means link disjoint
	 * @return List of disjoint path pairs for each demand
	 * @since 0.4.0
	 */
	public static Map<Demand,List<Pair<List<Link>,List<Link>>>> computeUnicastCandidate11PathList (Map<Demand,List<List<Link>>> cpl , int disjointType)
	{
		final boolean srgDisjoint = disjointType == 0;
		final boolean linkAndNodeDisjoint = disjointType == 1;
		final boolean linkDisjoint = !srgDisjoint && !linkAndNodeDisjoint;
		Map<Demand,List<Pair<List<Link>,List<Link>>>> result = new HashMap<Demand,List<Pair<List<Link>,List<Link>>>> ();
		for (Demand d : cpl.keySet())
		{
			for (List<Link> path : cpl.get(d)) d.netPlan.checkInThisNetPlanAndLayer(path , d.layer);
			List<Pair<List<Link>,List<Link>>> pairs11ThisDemand = new ArrayList<Pair<List<Link>,List<Link>>> ();
			final List<List<Link>> paths = new ArrayList<List<Link>> (cpl.get(d));
			final int P_d = paths.size ();
			for (int firstPathIndex = 0; firstPathIndex < P_d-1 ; firstPathIndex ++)
			{
				final List<Link> firstPath = paths.get(firstPathIndex);
				final Set<Link> firstPathLinks = new HashSet<Link> (firstPath);
				Set<Node> firstPathNodesButLastAndFirst = null;
				Set<SharedRiskGroup> firstPathSRGs = null;
				if (linkAndNodeDisjoint)
				{
					List<Node> firstPathSeqNodes = GraphUtils.convertSequenceOfLinksToSequenceOfNodes(firstPath);
					firstPathNodesButLastAndFirst = new HashSet<Node> (firstPathSeqNodes);
					firstPathNodesButLastAndFirst.remove(d.ingressNode);
					firstPathNodesButLastAndFirst.remove(d.egressNode);
				}	else if (srgDisjoint)
				{
					firstPathSRGs = SRGUtils.getAffectingSRGs(firstPathLinks);
				}
				for (int secondPathIndex = firstPathIndex + 1; secondPathIndex < P_d ; secondPathIndex ++)
				{
					List<Link> secondPath = paths.get(secondPathIndex);
					boolean disjoint = true; boolean firstLink = true;
					if (linkDisjoint)
					{
						for (Link e : secondPath) if (firstPathLinks.contains(e)) { disjoint = false; break; }
					} else if (linkAndNodeDisjoint)
					{
						for (Link e : secondPath) 
						{
							if (firstPathLinks.contains(e)) { disjoint = false; break; }
							if (firstLink) firstLink = false; else { if (firstPathNodesButLastAndFirst.contains(e.originNode)) disjoint = false; break;  }
						}
					} else if (srgDisjoint)
					{
						for (SharedRiskGroup srg : SRGUtils.getAffectingSRGs(secondPath)) if (firstPathSRGs.contains(srg)) { disjoint = false; break; }
					}
					if (disjoint)
					{
						checkDisjointness (firstPath , secondPath , disjointType);
						pairs11ThisDemand.add (Pair.of(firstPath, secondPath));
					}
				}
			}
			result.put (d , pairs11ThisDemand);
		}
		return result;
	}

	private static void checkDisjointness (List<Link> p1 , List<Link> p2 , int disjointnessType)
	{
		if ((p1 == null) || (p2 == null)) throw new RuntimeException ("Bad");
		
		if (disjointnessType == 0) // SRG disjoint
		{
			Set<SharedRiskGroup> srg1 = new HashSet<SharedRiskGroup> (); for (Link e : p1) srg1.addAll (e.getSRGs());
			for (Link e : p2) for (SharedRiskGroup srg : e.getSRGs()) if (srg1.contains(srg)) throw new RuntimeException ("Bad");
		}
		else if (disjointnessType == 1) // link and node
		{
			Set<NetworkElement> resourcesP1 = new HashSet<NetworkElement> (); boolean firstLink = true; 
			for (Link e : p1) { resourcesP1.add (e); if (firstLink) firstLink = false; else resourcesP1.add (e.getOriginNode()); }
			for (Link e : p2) if (resourcesP1.contains(e)) throw new RuntimeException ("Bad");
		}
		else if (disjointnessType == 2) // link 
		{
			Set<Link> linksP1 = new HashSet<Link> (p1); 
			for (Link e : p2) if (linksP1.contains (e)) throw new RuntimeException ("Bad: p1: " + p1 + ", p2: " + p2);
		}
		else throw new RuntimeException ("Bad");
			
	}

	/**
	 * <p>Adds traffic routes specified by those paths that satisfy the candidate path list options described below. Existing routes will not be removed.</p>
	 *
	 * <p><b>Important</b>: Routing type must be {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}.</p>
	 *
	 * <p>The candidate path list elaborated contains a set of paths
	 * computed for each unicast demand in the network, based on the k-shortest path idea. In general, for every demand k
	 * paths are computed with the shortest weight according to some weights
	 * assigned to the links.</p>
	 * <p>The computation of paths can be configured via {@code "parameter=value"} options. There are several options to
	 * configure, which can be combined:</p>
	 *
	 * <ul>
	 * 		<li>{@code K}: Number of desired loopless shortest paths (default: 3). If <i>K'&lt;</i>{@code K} different paths are found between the demand node pairs, then only <i>K'</i> paths are
	 * 		included in the candidate path list</li>
	 * 		<li>{@code maxLengthInKm}: Maximum path length measured in kilometers allowed (default: Double.MAX_VALUE)</li>
	 * 		<li>{@code maxNumHops}: Maximum number of hops allowed (default: Integer.MAX_VALUE)</li>
	 * 		<li>{@code maxPropDelayInMs}: Maximum propagation delay in miliseconds allowed in a path (default: Double.MAX_VALUE)</li>
	 * 		<li>{@code maxRouteCost}: Maximum path weight allowed (default: Double.MAX_VALUE)</li>
	 * 		<li>{@code maxRouteCostFactorRespectToShortestPath}: Maximum path weight factor with respect to the shortest path weight (default: Double.MAX_VALUE)</li>
	 *		<li>{@code maxRouteCostRespectToShortestPath}: Maximum path weight with respect to the shortest path weight (default: Double.MAX_VALUE). While the previous one is a multiplicative factor, this one is an additive factor</li>
	 * </ul>
	 *
	 * @param layer Network layer
	 * @param costs Link weight vector for the shortest path algorithm. If null, all the links have cost one
	 * @param paramValuePairs Parameters to be passed to the class to tune its operation. An even number of {@code String} is to be passed. For each {@code String} pair, first {@code String}
	 *           must be the name of the parameter, second a {@code String} with its value. If no name-value pairs are set, default values are used
	 * @return Map with all the computed paths (values) per demands (keys)
	 */
	public Map<Demand,List<List<Link>>> computeUnicastCandidatePathList (NetworkLayer layer , double [] costs , String... paramValuePairs)
	{
		checkIsModifiable();
		checkInThisNetPlan(layer);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		if (costs != null) if (costs.length != layer.links.size()) throw new Net2PlanException ("The array of costs must have the same length as the number of links in the layer");

		int K = 3;
		double maxLengthInKm = Double.MAX_VALUE;
		int maxNumHops = Integer.MAX_VALUE;
		double maxPropDelayInMs = Double.MAX_VALUE;
		double maxRouteCost = Double.MAX_VALUE;
		double maxRouteCostFactorRespectToShortestPath = Double.MAX_VALUE;
		double maxRouteCostRespectToShortestPath = Double.MAX_VALUE;
		int numParameters = (int) (paramValuePairs.length / 2);
		if (numParameters * 2 != paramValuePairs.length) throw new Net2PlanException("A parameter has not assigned its value");
		for (int contParam = 0; contParam < numParameters; contParam++)
		{
			String parameter = paramValuePairs[contParam * 2];
			String value = paramValuePairs[contParam * 2 + 1];

			if (parameter.equalsIgnoreCase("K"))
			{
				K = Integer.parseInt(value);
				if (K <= 0) throw new Net2PlanException("'K' parameter must be greater than zero");
			}
			else if (parameter.equalsIgnoreCase("maxLengthInKm"))
				maxLengthInKm = Double.parseDouble(value) <= 0? Double.MAX_VALUE : Double.parseDouble(value);
			else if (parameter.equalsIgnoreCase("maxPropDelayInMs"))
				maxPropDelayInMs = Double.parseDouble(value) <= 0? Double.MAX_VALUE : Double.parseDouble(value);
			else if (parameter.equalsIgnoreCase("maxNumHops"))
				maxNumHops = Integer.parseInt(value) <= 0? Integer.MAX_VALUE : Integer.parseInt(value); 
			else if (parameter.equalsIgnoreCase("maxRouteCost"))
				maxRouteCost = Double.parseDouble(value) <= 0? Double.MAX_VALUE : Double.parseDouble(value);
			else if (parameter.equalsIgnoreCase("maxRouteCostFactorRespectToShortestPath"))
				maxRouteCostFactorRespectToShortestPath = Double.parseDouble(value) <= 0? Double.MAX_VALUE : Double.parseDouble(value);
			else if (parameter.equalsIgnoreCase("maxRouteCostRespectToShortestPath"))
				maxRouteCostRespectToShortestPath = Double.parseDouble(value) <= 0? Double.MAX_VALUE : Double.parseDouble(value);
			else
				throw new RuntimeException("Unknown parameter " + parameter);
		}

		Map<Demand,List<List<Link>>> cpl = new HashMap<Demand,List<List<Link>>> ();
		Map<Link,Double> linkCostMap = new HashMap<Link,Double> (); 
		for (Link e : layer.links) linkCostMap.put (e , costs == null? 1.0 : costs [e.index]);
		for(Demand d : layer.demands) cpl.put (d , GraphUtils.getKLooplessShortestPaths(nodes , layer.links , d.ingressNode , d.egressNode , linkCostMap , K , maxLengthInKm, maxNumHops, maxPropDelayInMs , maxRouteCost , maxRouteCostFactorRespectToShortestPath ,	maxRouteCostRespectToShortestPath ));
		return cpl;
	}

	/**
	 * <p>Adds multiples routes from a Candidate Path List.</p>
	 * @param cpl {@code Map} where the keys are demands and the values a list of sequence of links (each sequence is a route)
	 */
	public void addRoutesFromCandidatePathList (Map<Demand,List<List<Link>>> cpl)
	{
		checkIsModifiable();
		List<Route> routes = new LinkedList<Route> ();
		try
		{
			for(Entry<Demand,List<List<Link>>> entry : cpl.entrySet())
				for (List<Link> seqLinks : entry.getValue())
					routes.add (addRoute(entry.getKey() , 0 , 0 , seqLinks , null));
		} catch (Exception e) { for (Route r : routes) r.remove (); throw e; } 
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Same as {@code addRoutesFromCandidatePathList(computeUnicastCandidatePathList(layer , costs , paramValuePairs);}
	 * @param layer Network layer
	 * @param costs Link weight vector for the shortest path algorithm
	 * @param paramValuePairs Parameters to be passed to the class to tune its operation. An even number of {@code String} is to be passed. For each {@code String} pair, first {@code String}
	 *           must be the name of the parameter, second a {@code String} with its value. If no name-value pairs are set, default values are used
	 */
	public void addRoutesFromCandidatePathList (NetworkLayer layer , double [] costs , String... paramValuePairs)
	{
		addRoutesFromCandidatePathList(computeUnicastCandidatePathList(layer , costs , paramValuePairs));
	}

	/**
	 * <p>Same as {@code addRoutesFromCandidatePathList(computeUnicastCandidatePathList(costs , paramValuePairs);}
	 * @param costs Link weight vector for the shortest path algorithm
	 * @param paramValuePairs Parameters to be passed to the class to tune its operation. An even number of {@code String} is to be passed. For each {@code String} pair, first {@code String}
	 *           must be the name of the parameter, second a {@code String} with its value. If no name-value pairs are set, default values are used
	 */
	public void addRoutesFromCandidatePathList (double [] costs , String... paramValuePairs)
	{
		addRoutesFromCandidatePathList(computeUnicastCandidatePathList(costs , paramValuePairs));
	}
	
	public void addRoutesAndProtectionSegmentFromCandidate11PathList (Map<Demand,List<Pair<List<Link>,List<Link>>>> cpl11)
	{
		checkIsModifiable();
		List<Route> routes = new LinkedList<Route> ();
		List<ProtectionSegment> segments = new LinkedList<ProtectionSegment> ();
		try
		{
			for(Entry<Demand,List<Pair<List<Link>,List<Link>>>> entry : cpl11.entrySet())
				for (Pair<List<Link>,List<Link>> pathPair : entry.getValue())
				{
					final Route newRoute = addRoute(entry.getKey() , 0 , 0 , pathPair.getFirst() , null);
					routes.add (newRoute); 
					final ProtectionSegment newSegment = addProtectionSegment(pathPair.getSecond(), 0, null); 
					segments.add (newSegment);
					newRoute.addProtectionSegment(newSegment);
				}
		} catch (Exception e) { for (Route r : routes) r.remove (); for (ProtectionSegment s : segments) s.remove (); throw e; } 
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	//	public void addMulticastTreesFromCandidateTreeList (double [] costs , String solverName , String solverLibraryName , double maxSolverTimeInSecondsPerTree , String... paramValuePairs)
//	{
//		addMulticastTreesFromCandidateTreeList (defaultLayer , costs , solverName , solverLibraryName , maxSolverTimeInSecondsPerTree , paramValuePairs);
//	}
//

	/**
	 * The same as {@code computeMulticastCandidatePathList} for the default layer
	 * @param costs see {@code computeMulticastCandidatePathList}
	 * @param solverName see {@code computeMulticastCandidatePathList}
	 * @param solverLibraryName see {@code computeMulticastCandidatePathList}
	 * @param maxSolverTimeInSecondsPerTree see {@code computeMulticastCandidatePathList}
	 * @param candidateTreeListParamValuePairs see {@code computeMulticastCandidatePathList}
	 * @return see {@code computeMulticastCandidatePathList}
	 */
	public Map<MulticastDemand,List<Set<Link>>> computeMulticastCandidatePathList (DoubleMatrix1D costs , String solverName , String solverLibraryName , double maxSolverTimeInSecondsPerTree , String... candidateTreeListParamValuePairs)
	{
		return computeMulticastCandidatePathList (defaultLayer , costs , solverName  , solverLibraryName , maxSolverTimeInSecondsPerTree , candidateTreeListParamValuePairs);
	}

	/**
	 * <p>Adds multicast trees specified by those trees that satisfy the options described below. Existing multicast trees will not be removed.</p>
	 *
	 * <p>The candidate tree list elaborated contains a set of multicast trees (each one is a set of links) computed for each multicast demand 
	 * in the network. To compute it, a ILP formulation is solved for each new multicast tree. In general, for every multicast demand k
	 * trees are computed ranked according to its cost (weight) according to some link weights.</p>
	 * <p>The computation of paths can be configured via {@code "parameter=value"} options. There are several options to
	 * configure, which can be combined:</p>
	 *
	 * <ul>
	 * 		<li>{@code K}: Number of desired multicast trees per demand (default: 3). If <i>K'&lt;</i>{@code K} different trees are found for the multicast demand, then only <i>K'</i> are
	 * 		included in the candidate list</li>
	 * 		<li>{@code maxCopyCapability}: the maximum number of copies of an input traffic a node can make. Then, a node can have at most this number of ouput links carrying traffic of a multicast tree (default: Double.MAX_VALUE)</li>
	 * 		<li>{@code maxE2ELengthInKm}: Maximum path length measured in kilometers allowed for any tree, from the origin node, to any destination node (default: Double.MAX_VALUE)</li>
	 * 		<li>{@code maxE2ENumHops}: Maximum number of hops allowed for any tree, from the origin node, to any destination node (default: Integer.MAX_VALUE)</li>
	 * 		<li>{@code maxE2EPropDelayInMs}: Maximum propagation delay in miliseconds allowed in a path, for any tree, from the origin node, to any destination node  (default: Double.MAX_VALUE)</li>
	 * 		<li>{@code maxTreeCost}: Maximum tree weight allowed, summing the weights of the links (default: Double.MAX_VALUE)</li>
	 * 		<li>{@code maxTreeCostFactorRespectToMinimumCostTree}: Trees with higher weight (cost) than the cost of the minimum cost tree, multiplied by this factor, are not returned (default: Double.MAX_VALUE)</li>
	 *		<li>{@code maxTreeCostRespectToMinimumCostTree}: Trees with higher weight (cost) than the cost of the minimum cost tree, plus this factor, are not returned (default: Double.MAX_VALUE). While the previous one is a multiplicative factor, this one is an additive factor</li>
	 * </ul>
	 *
	 * @param layer the layer for which the candidate multicast tree list is computed
	 * @param linkCosts Link weight vector for the shortest path algorithm. If {@code null}, a vector of ones is assumed
	 * @param solverName the name of the solver to call for the internal formulation of the algorithm
	 * @param solverLibraryName the solver library name
	 * @param maxSolverTimeInSecondsPerTree the maximum time the solver is allowed for each of the internal formulations (one for each new tree). 
	 * The best solution found so far is returned. If non-positive, no time limit is set
	 * @param candidateTreeListParamValuePairs Parameters to be passed to the class to tune its operation. An even number of {@code String} is to be passed. For each {@code String} pair, first {@code String}
	 *           must be the name of the parameter, second a {@code String} with its value. If no name-value pairs are set, default values are used
	 * @return Map with a list of all the computed trees (a tree is a set of links) per multicast demands
	 */
	public Map<MulticastDemand,List<Set<Link>>> computeMulticastCandidatePathList (NetworkLayer layer, DoubleMatrix1D linkCosts , String solverName , String solverLibraryName , double maxSolverTimeInSecondsPerTree , String... candidateTreeListParamValuePairs)
	{
		checkInThisNetPlan(layer);
		if (linkCosts == null) linkCosts = DoubleFactory1D.dense.make (layer.links.size () , 1); 
		if (linkCosts.size () != layer.links.size()) throw new Net2PlanException ("The array of costs must have the same length as the number of links in the layer");
		Map<MulticastDemand,List<Set<Link>>> cpl = new HashMap<MulticastDemand,List<Set<Link>>> (); 
		int K = 3;
		int maxCopyCapability = Integer.MAX_VALUE;
		double maxE2ELengthInKm = Double.MAX_VALUE;
		int maxE2ENumHops = Integer.MAX_VALUE;
		double maxE2EPropDelayInMs = Double.MAX_VALUE;
		double maxTreeCost = Double.MAX_VALUE;
		double maxTreeCostFactorRespectToMinimumCostTree = Double.MAX_VALUE;
		double maxTreeCostRespectToMinimumCostTree = Double.MAX_VALUE;
		
		int numParameters = (int) (candidateTreeListParamValuePairs.length / 2);
		if (numParameters * 2 != candidateTreeListParamValuePairs.length) throw new Net2PlanException("A parameter has not assigned its value");

		for (int contParam = 0; contParam < numParameters; contParam++)
		{
			String parameter = candidateTreeListParamValuePairs[contParam * 2];
			String value = candidateTreeListParamValuePairs[contParam * 2 + 1];

			if (parameter.equalsIgnoreCase("K"))
			{
				K = Integer.parseInt(value);
				if (K <= 0) throw new Net2PlanException("'K' parameter must be greater than zero");
			}
			else if (parameter.equalsIgnoreCase("maxCopyCapability"))
				maxCopyCapability = Integer.parseInt (value) <= 0? Integer.MAX_VALUE : Integer.parseInt (value);
			else if (parameter.equalsIgnoreCase("maxE2ELengthInKm"))
				maxE2ELengthInKm = Double.parseDouble(value) <= 0? Double.MAX_VALUE : Double.parseDouble(value);
			else if (parameter.equalsIgnoreCase("maxE2EPropDelayInMs"))
				maxE2EPropDelayInMs = Double.parseDouble(value) <= 0? Double.MAX_VALUE : Double.parseDouble(value);
			else if (parameter.equalsIgnoreCase("maxE2ENumHops"))
				maxE2ENumHops = Integer.parseInt (value) <= 0? Integer.MAX_VALUE : Integer.parseInt (value);
			else if (parameter.equalsIgnoreCase("maxTreeCost"))
				maxTreeCost = Double.parseDouble(value) <= 0? Double.MAX_VALUE : Double.parseDouble(value);
			else if (parameter.equalsIgnoreCase("maxTreeCostFactorRespectToMinimumCostTree"))
				maxTreeCostFactorRespectToMinimumCostTree = Double.parseDouble(value) <= 0? Double.MAX_VALUE : Double.parseDouble(value);
			else if (parameter.equalsIgnoreCase("maxTreeCostRespectToMinimumCostTree"))
				maxTreeCostRespectToMinimumCostTree = Double.parseDouble(value) <= 0? Double.MAX_VALUE : Double.parseDouble(value);
			else
				throw new RuntimeException("Unknown parameter " + parameter);
		}

		final DoubleMatrix2D Aout_ne = (layer.routingType == RoutingType.SOURCE_ROUTING)? getMatrixNodeLinkOutgoingIncidence(layer) : layer.forwardingRules_Aout_ne;
		final DoubleMatrix2D Ain_ne = (layer.routingType == RoutingType.SOURCE_ROUTING)? getMatrixNodeLinkIncomingIncidence(layer) : layer.forwardingRules_Ain_ne;
		
		for (MulticastDemand d : layer.multicastDemands)
		{
			List<Set<Link>> trees = GraphUtils.getKMinimumCostMulticastTrees(layer.links , d.getIngressNode() , d.getEgressNodes() , Aout_ne , Ain_ne , linkCosts , solverName , solverLibraryName , maxSolverTimeInSecondsPerTree , K , maxCopyCapability , maxE2ELengthInKm , maxE2ENumHops, maxE2EPropDelayInMs , maxTreeCost , maxTreeCostFactorRespectToMinimumCostTree , maxTreeCostRespectToMinimumCostTree);
			cpl.put(d, trees);
		}
		return cpl;
	}

	/**
	 * <p>Adds multiple multicast trees from a Candidate Tree list.</p>
	 * @param cpl {@code Map} where the keys are multicast demands and the values lists of link sets
	 * @see com.net2plan.interfaces.networkDesign.MulticastDemand
	 * @see com.net2plan.interfaces.networkDesign.Link
	 */
	public void addMulticastTreesFromCandidateTreeList (Map<MulticastDemand,List<Set<Link>>> cpl)
	{
		checkIsModifiable();
		List<MulticastTree> trees = new LinkedList<MulticastTree> ();
		try 
		{
			for(Entry<MulticastDemand,List<Set<Link>>> entry : cpl.entrySet())
				for (Set<Link> linkSet : entry.getValue())
					trees.add(addMulticastTree(entry.getKey(), 0, 0, linkSet, null));
		} catch (Exception e) { for (MulticastTree t : trees) t.remove (); throw e; }
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Adds a new SRG.</p>
	 * @param mttfInHours Mean Time To Fail (in hours). Must be greater than zero
	 * @param mttrInHours Mean Time To Repair (in hours). Must be greater than zero
	 * @param attributes Map for user-defined attributes ({@code null} means 'no attribute'). Each key represents the attribute name, whereas value represents the attribute value
	 * @return The newly created shared risk group object
	 * @see com.net2plan.interfaces.networkDesign.SharedRiskGroup
	 */
	public SharedRiskGroup addSRG(double mttfInHours, double mttrInHours, Map<String, String> attributes)
	{
		checkIsModifiable();
		if (mttfInHours <= 0) throw new Net2PlanException ("Mean Time To Fail must be a positive value");
		if (mttrInHours <= 0) throw new Net2PlanException ("Mean Time To Repair must be a positive value");

		final long srgId = nextElementId.longValue();
		nextElementId.increment();
		
		SharedRiskGroup srg = new SharedRiskGroup(this, srgId, srgs.size(), new HashSet<Node> (), new HashSet<Link> () , mttfInHours , mttrInHours , new AttributeMap(attributes));
		
		srgs.add (srg);
		cache_id2srgMap.put (srgId , srg);
		
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
		return srg;
	}


	/**
	 * <p>Assigns the information from the input {@code NetPlan}.</p>
	 * 
	 * <p><b>Important</b>: A shadow copy is made, so changes in the input object will be reflected in this one. For deep copies use {@link #copyFrom(com.net2plan.interfaces.networkDesign.NetPlan) copyFrom()}</p>
	 * 
	 * @param netPlan Network plan to be copied
	 * @since 0.3.0
	 */
	public void assignFrom(NetPlan netPlan)
	{
		checkIsModifiable();

		this.DEFAULT_ROUTING_TYPE = netPlan.DEFAULT_ROUTING_TYPE;
		this.isModifiable = netPlan.isModifiable;
		this.networkDescription = netPlan.networkDescription;
		this.networkName = netPlan.networkName;
		this.defaultLayer = netPlan.defaultLayer;
		this.nextElementId = netPlan.nextElementId;
		this.layers = netPlan.layers;
		this.nodes = netPlan.nodes;
		this.srgs = netPlan.srgs;
		this.cache_nodesDown = netPlan.cache_nodesDown;
		this.cache_id2LayerMap = netPlan.cache_id2LayerMap;
		this.cache_id2NodeMap = netPlan.cache_id2NodeMap;
		this.cache_id2LinkMap = netPlan.cache_id2LinkMap;
		this.cache_id2DemandMap = netPlan.cache_id2DemandMap;
		this.cache_id2MulticastDemandMap = netPlan.cache_id2MulticastDemandMap;
		this.cache_id2RouteMap = netPlan.cache_id2RouteMap;
		this.cache_id2MulticastTreeMap = netPlan.cache_id2MulticastTreeMap;
		this.cache_id2ProtectionSegmentMap = netPlan.cache_id2ProtectionSegmentMap;
		this.cache_id2srgMap = netPlan.cache_id2srgMap;
		this.interLayerCoupling = netPlan.interLayerCoupling;
		this.attributes.clear (); this.attributes.putAll(netPlan.attributes);
		for (Node node : netPlan.nodes) node.netPlan = this;
		for (SharedRiskGroup srg : netPlan.srgs) srg.netPlan = this;
		for (NetworkLayer layer : netPlan.layers)
		{
			layer.netPlan = this;
			for (Link e : layer.links) e.netPlan = this;
			for (Demand e : layer.demands) e.netPlan = this;
			for (MulticastDemand e : layer.multicastDemands) e.netPlan = this;
			for (Route e : layer.routes) e.netPlan = this;
			for (ProtectionSegment e : layer.protectionSegments) e.netPlan = this;
			for (MulticastTree e : layer.multicastTrees) e.netPlan = this;
		}
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Checks that the input sequence of links belong to the same layer and follow a contiguous path. Throws an exception if the path is not contiguous or the sequence of links
	 * is empty. </p>
	 * @param links Sequence of links
	 * @param layer Network layer
	 * @param originNode Origin node
	 * @param destinationNode Destination node
	 * @return The input layer
	 */
	public NetworkLayer checkContiguousPath (List<Link> links , NetworkLayer layer , Node originNode , Node destinationNode)
	{
		if (links.isEmpty()) throw new Net2PlanException ("Empty sequence of links");
		final Link firstLink = links.iterator().next ();
		if (layer == null) layer = firstLink.layer;
		checkInThisNetPlan(layer);
		if (originNode != null) checkInThisNetPlan(originNode);
		if (destinationNode != null) checkInThisNetPlan(destinationNode);
		for (Link e : links)
			if (!e.layer.equals (layer)) throw new Net2PlanException ("The path contains links not attached to the appropriate NetPlan object or layer");
			
		if ((originNode != null) && (!firstLink.originNode.equals (originNode))) throw new Net2PlanException ("The initial node of the sequence of links is not correct");
		Node endNodePreviousLink = firstLink.originNode;
		for (Link link : links)
		{
			if (!endNodePreviousLink.equals (link.originNode)) throw new Net2PlanException ("This is not a contigous sequence of links");
			endNodePreviousLink = link.destinationNode;
		}
		if ((destinationNode != null) && !(endNodePreviousLink.equals (destinationNode))) throw new Net2PlanException  ("The end node of the sequence of links is not correct");
		return layer;
	}

	/**
	 * <p>Checks if a demand exists in this {@code NetPlan} object. Throws an exception if nonexistent.</p>
	 * @param id Demand id
	 */
	void checkExistsDemand (long id) { if (cache_id2DemandMap.get(id) == null) throw new Net2PlanException ("Demand of id: " + id + " does not exist"); }

	/**
	 * <p>Checks if a link exists in this {@code NetPlan} object. Throws an exception if nonexistent.</p>
	 * @param id Link id
	 */
	void checkExistsLink (long id) { if (cache_id2LinkMap.get(id) == null) throw new Net2PlanException ("Link of id: " + id + " does not exist"); }

	/**
	 * <p>Checks if a multicast demand exists in this {@code NetPlan} object. Throws an exception if nonexistent.</p>
	 * @param id Multicast demand id
	 */
	void checkExistsMulticastDemand (long id) { if (cache_id2MulticastDemandMap.get(id) == null) throw new Net2PlanException ("Multicast demand of id: " + id + " does not exist"); }

	/**
	 * <p>Checks if a multicast tree exists in this {@code NetPlan} object. Throws an exception if nonexistent.</p>
	 * @param id Multicast tree id
	 */
	void checkExistsMulticastTree (long id) { if (cache_id2MulticastTreeMap.get(id) == null) throw new Net2PlanException ("Multicast tree of id: " + id + " does not exist"); }

	/**
	 * <p>Checks if a network layer exists in this {@code NetPlan} object. Throws an exception if nonexistent.</p>
	 * @param id Network layer id
	 */
	void checkExistsNetworkLayer (long id) { if (cache_id2LayerMap.get(id) == null) throw new Net2PlanException ("Network layer of id: " + id + " does not exist"); }

	/**
	 * <p>Checks if a node exists in this {@code NetPlan} object. Throws an exception if nonexistent.</p>
	 * @param id Node id
	 */
	void checkExistsNode (long id) { if (cache_id2NodeMap.get(id) == null) throw new Net2PlanException ("Node of id: " + id + " does not exist"); }

	/**
	 * <p>Checks if a protection segment exists in this {@code NetPlan} object. Throws an exception if nonexistent.</p>
	 * @param id Protection segment id
	 */
	void checkExistsProtectionSegment (long id) { if (cache_id2ProtectionSegmentMap.get(id) == null) throw new Net2PlanException ("ProtectionSegment of id: " + id + " does not exist"); }

	/**
	 * <p>Checks if a route exists in this {@code NetPlan} object. Throws an exception if nonexistent.</p>
	 * @param id Route id
	 */
	void checkExistsRoute (long id) { if (cache_id2RouteMap.get(id) == null) throw new Net2PlanException ("Route of id: " + id + " does not exist"); }

	/**
	 * <p>Checks if a shared risk group exists in this {@code NetPlan} object. Throws an exception if nonexistent.</p>
	 * @param id the SRG identifier
	 */
	void checkExistsSRG (long id) { if (cache_id2srgMap.get(id) == null) throw new Net2PlanException ("Shared risk group of id: " + id + " does not exist"); }

	/**
	 * <p>Checks if all the elements are attached to this {@code NetPlan} object. 
	 * .</p>
	 * @param elements Collection of network elements
	 * @see com.net2plan.interfaces.networkDesign.NetworkElement
	 */
	void checkInThisNetPlan(Collection<? extends NetworkElement> elements)
	{
		for (NetworkElement e : elements)
			checkInThisNetPlan(e);
	}

	/**
	 * <p>Checks if an element is attached to this {@code NetPlan} object. It is checked also if the element belongs to the input layer.</p>
	 * @param elements Network element
	 * @param optionalLayer Network layer (optional)
	 */
	void checkInThisNetPlanAndLayer(Collection<? extends NetworkElement> elements, NetworkLayer ... optionalLayer)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayer);
		for (NetworkElement e : elements)
			checkInThisNetPlanAndLayer(e, layer);
	}

	/**
	 * <p>Checks if a network element exists in this {@code NetPlan} object. Throws an exception if nonexistent.</p>
	 * @param e Network element
	 *
	 */
	void checkInThisNetPlan(NetworkElement e)
	{
		if (e == null) throw new Net2PlanException ("Network element is null");
		if (e.netPlan != this) throw new Net2PlanException ("Element " + e + " is not attached to this NetPlan object.");
		if (e instanceof Node) { if (e != nodes.get(e.index)) throw new Net2PlanException ("Element " + e + " is not the same object as the one in netPlan object."); }
		else if (e instanceof SharedRiskGroup) { if (e != srgs.get(e.index)) throw new Net2PlanException ("Element " + e + " is not the same object as the one in netPlan object."); }
		else if (e instanceof NetworkLayer) { if (e != layers.get(e.index)) throw new Net2PlanException ("Element " + e + " is not the same object as the one in netPlan object."); }
	}

	/**
	 * <p>Checks if an element is attached to this {@code NetPlan} object. It is checked also if the element belongs to the input layer.</p>
	 * @param e Network element
	 * @param layer Network layer (optional)
	 */
	void checkInThisNetPlanAndLayer(NetworkElement e, NetworkLayer layer)
	{
		if (e == null) throw new Net2PlanException ("Network element is null");
		if (e.netPlan != this) throw new Net2PlanException ("Element " + e + " is not attached to this NetPlan object.");
		if (e instanceof Node) { if (e != nodes.get(e.index)) throw new Net2PlanException ("Element " + e + " is not the same object as the one in netPlan object."); }
		else if (e instanceof SharedRiskGroup) { if (e != srgs.get(e.index)) throw new Net2PlanException ("Element " + e + " is not the same object as the one in netPlan object."); }
		else if (e instanceof NetworkLayer) { if (e != layers.get(e.index)) throw new Net2PlanException ("Element " + e + " is not the same object as the one in netPlan object."); }
		if (layer != null) 
		{
			layer.checkAttachedToNetPlanObject(this);
			if (e instanceof Demand) { if (e != layer.demands.get(e.index)) throw new Net2PlanException ("Element " + e + " is not the same object as the one in netPlan object."); }
			else if (e instanceof ProtectionSegment) { if (e != layer.protectionSegments.get(e.index)) throw new Net2PlanException ("Element " + e + " is not the same object as the one in netPlan object."); }
			else if (e instanceof Link) { if (e != layer.links.get(e.index)) throw new Net2PlanException ("Element " + e + " is not the same object as the one in netPlan object."); }
			else if (e instanceof MulticastDemand) { if (e != layer.multicastDemands.get(e.index)) throw new Net2PlanException ("Element " + e + " is not the same object as the one in netPlan object."); }
			else if (e instanceof MulticastTree) { if (e != layer.multicastTrees.get(e.index)) throw new Net2PlanException ("Element " + e + " is not the same object as the one in netPlan object."); }
			else if (e instanceof Route) { if (e != layer.routes.get(e.index)) throw new Net2PlanException ("Element " + e + " is not the same object as the one in netPlan object."); }
		}
	}

	/**
	 * <p>Checks if the {@code NetPlan} object is modifiable. When negative, an exception will be thrown.</p>
	 * 
	 * @since 0.4.0
	 */
	void checkIsModifiable()
	{
		if (!isModifiable) throw new UnsupportedOperationException(UNMODIFIABLE_EXCEPTION_STRING);
	}

	/**
	 * <p>Checks if a set of links is valid for a given multicast demand. If it is not, an exception will be thrown. If it is valid, a map is returned with the
	 * unique sequence of links in the tree, from the ingress node to each egress node of the multicast demand.</p>
	 *
	 * @param linkSet Sequence of links
	 * @param demand Multicast demand
	 * @return Map where the keys are nodes and the values are sequences of links (see description)
	 * @see com.net2plan.interfaces.networkDesign.Node
	 * @see com.net2plan.interfaces.networkDesign.Link
	 * @see com.net2plan.interfaces.networkDesign.MulticastDemand
	 */
	Pair<Map<Node,List<Link>> , Set<Node>> checkMulticastTreeValidityForDemand(Set<Link> linkSet, MulticastDemand demand)
	{
		if (linkSet.isEmpty()) throw new Net2PlanException ("The multicast tree is empty");
		checkInThisNetPlan(demand);
		final NetworkLayer layer = demand.layer;
		checkInThisNetPlanAndLayer(linkSet , demand.layer);
			
		Map<Node,List<Link>> pathToEgressNode = new HashMap<Node,List<Link>> ();
		Map<Link,Double> linkCost = new HashMap<Link,Double> (); 
		for (Link link : layer.links) linkCost.put (link , (linkSet.contains (link))? 1 : Double.MAX_VALUE);
		Set<Link> actuallyTraversedLinks = new HashSet<Link> ();
		for (Node egressNode : demand.egressNodes)
		{
			final List<Link> seqLinks =  GraphUtils.getShortestPath(nodes , layer.links , demand.ingressNode , egressNode , linkCost);
			if (seqLinks == null) throw new Net2PlanException ("No path to an end node");
			pathToEgressNode.put(egressNode, seqLinks);
			actuallyTraversedLinks.addAll(seqLinks);
		}
		
		if (!linkSet.equals(actuallyTraversedLinks)) throw new Net2PlanException ("Some links in the link set provided are not traversed. The structure may not be a tree");
		
		Set<Node> traversedNodes = new HashSet<Node> (); for (Link e : linkSet) { traversedNodes.add (e.originNode); traversedNodes.add (e.destinationNode); }
		if (traversedNodes.size() != linkSet.size() + 1) throw new Net2PlanException ("It is not a tree");
		
		return Pair.of (pathToEgressNode,traversedNodes);
	}
	
	/**
	 * <p>Checks if a sequence of links is valid, that is all the links follow a contiguous path from the demand ingress node to the egress node. If the sequence
	 * is not valid, an exception is thrown.</p>
	 * @param path Sequence of links
	 * @param d Demand
	 * @see com.net2plan.interfaces.networkDesign.Demand
	 * @see com.net2plan.interfaces.networkDesign.Link
	 */
	void checkPathValidityForDemand(List<Link> path, Demand d)
	{
		checkInThisNetPlan(d);
		checkInThisNetPlanAndLayer(path , d.layer);
		checkContiguousPath (path , d.layer , d.ingressNode, d.egressNode);
	}

	/**
	 * <p>Returns a deep copy of the current design.</p>
	 * 
	 * @return Deep copy of the current design
	 * @since 0.2.0
	 */
	public NetPlan copy()
	{
		this.checkCachesConsistency();
		NetPlan netPlan = new NetPlan();
		netPlan.checkCachesConsistency();
		netPlan.copyFrom(this);
//		System.out.println ("************** En el copy () *********************************************************");
		this.checkCachesConsistency();
		netPlan.checkCachesConsistency();
		return netPlan;
	}

	/**
	 * <p>Removes all information from the current {@code NetPlan} and copy the information from the input {@code NetPlan}.</p>
	 * @param originNetPlan Network plan to be copied from
	 */
	public void copyFrom(NetPlan originNetPlan)
	{
		checkIsModifiable();
		if (originNetPlan == this) return;
		if (originNetPlan == null) throw new Net2PlanException ("A NetPlan object must be provided");

		this.attributes.clear(); this.attributes.putAll (originNetPlan.attributes);
		this.netPlan = this;
		this.layers = new ArrayList<NetworkLayer> ();
		this.nodes = new ArrayList<Node> ();
		this.srgs = new ArrayList<SharedRiskGroup> ();
		this.cache_nodesDown = new HashSet<Node> ();
		this.cache_id2NodeMap = new HashMap <Long,Node> ();
		this.cache_id2LayerMap = new HashMap <Long,NetworkLayer> ();
		this.cache_id2srgMap= new HashMap<Long,SharedRiskGroup> ();
		this.cache_id2LinkMap = new HashMap<Long,Link> ();
		this.cache_id2DemandMap = new HashMap<Long,Demand> ();
		this.cache_id2MulticastDemandMap = new HashMap<Long,MulticastDemand> ();
		this.cache_id2RouteMap = new HashMap<Long,Route> ();
		this.cache_id2MulticastTreeMap = new HashMap<Long,MulticastTree> ();
		this.cache_id2ProtectionSegmentMap = new HashMap<Long,ProtectionSegment> ();
		this.DEFAULT_ROUTING_TYPE = originNetPlan.DEFAULT_ROUTING_TYPE;
		this.isModifiable = true;
		this.networkDescription = originNetPlan.networkDescription;
		this.networkName = originNetPlan.networkName;
		this.nextElementId = originNetPlan.nextElementId;
		this.interLayerCoupling = new DirectedAcyclicGraph<NetworkLayer, DemandLinkMapping>(DemandLinkMapping.class);

		/* Create the new network elements, not all the fields filled */
		for (Node originNode : originNetPlan.nodes) 
		{
			Node newElement = new Node (this , originNode.id , originNode.index , originNode.nodeXYPositionMap.getX(), originNode.nodeXYPositionMap.getY(), originNode.name, originNode.attributes); 
			cache_id2NodeMap.put(originNode.id, newElement);
			nodes.add (newElement);
			if (!originNode.isUp) cache_nodesDown.add (newElement);
		}
		for (SharedRiskGroup originSrg : originNetPlan.srgs) 
		{
			SharedRiskGroup newElement = new SharedRiskGroup (this , originSrg.id , originSrg.index , null , null, originSrg.meanTimeToFailInHours , originSrg.meanTimeToRepairInHours , originSrg.attributes); 
			cache_id2srgMap.put(originSrg.id, newElement);
			srgs.add (newElement);
		}
		for (NetworkLayer originLayer : originNetPlan.layers)
		{
			NetworkLayer newLayer = new NetworkLayer (this , originLayer.id , originLayer.index , originLayer.demandTrafficUnitsName , originLayer.description , originLayer.name , originLayer.linkCapacityUnitsName , originLayer.attributes); 
			cache_id2LayerMap.put(originLayer.id, newLayer);
			layers.add (newLayer);
			if (originLayer.id == originNetPlan.defaultLayer.id)
				this.defaultLayer = newLayer;
			
			for (Demand originDemand : originLayer.demands) 
			{
				Demand newElement = new Demand (this , originDemand.id , originDemand.index , newLayer , this.cache_id2NodeMap.get(originDemand.ingressNode.id) , this.cache_id2NodeMap.get(originDemand.egressNode.id) , originDemand.offeredTraffic , originDemand.attributes); 
				cache_id2DemandMap.put(originDemand.id, newElement);
				newLayer.demands.add (newElement);
			}
			for (MulticastDemand originDemand : originLayer.multicastDemands) 
			{
				Set<Node> newEgressNodes = new HashSet<Node> (); for (Node oldEgressNode : originDemand.egressNodes) newEgressNodes.add (this.cache_id2NodeMap.get(oldEgressNode.id));
				MulticastDemand newElement = new MulticastDemand (this , originDemand.id , originDemand.index , newLayer , this.cache_id2NodeMap.get(originDemand.ingressNode.id) , newEgressNodes , originDemand.offeredTraffic , originDemand.attributes); 
				cache_id2MulticastDemandMap.put(originDemand.id, newElement);
				newLayer.multicastDemands.add (newElement);
			}
			for (Link originLink : originLayer.links) 
			{
				Link newElement = new Link (this , originLink.id , originLink.index , newLayer , this.cache_id2NodeMap.get(originLink.originNode.id) , this.cache_id2NodeMap.get(originLink.destinationNode.id) , originLink.lengthInKm , originLink.propagationSpeedInKmPerSecond , originLink.capacity , originLink.attributes); 
				cache_id2LinkMap.put(originLink.id, newElement);
				newLayer.links.add (newElement);
			}
			for (Route originRoute : originLayer.routes) 
			{
				List<Link> newSeqLinksRealPath = new LinkedList<Link> ();
				for (Link oldLink : originRoute.seqLinksRealPath)
				{
					Link newLink = this.cache_id2LinkMap.get(oldLink.id);
					newSeqLinksRealPath.add (newLink);
				}
				Route newElement = new Route (this , originRoute.id , originRoute.index , cache_id2DemandMap.get(originRoute.demand.id) , newSeqLinksRealPath , originRoute.attributes); 
				newElement.carriedTraffic = originRoute.carriedTraffic;
				newElement.carriedTrafficIfNotFailing = originRoute.carriedTrafficIfNotFailing;
				newElement.occupiedLinkCapacity = originRoute.occupiedLinkCapacity;
				newElement.occupiedLinkCapacityIfNotFailing = originRoute.occupiedLinkCapacityIfNotFailing;
				cache_id2RouteMap.put(originRoute.id, newElement);
				newLayer.routes.add (newElement);
			}
			for (MulticastTree originTree : originLayer.multicastTrees) 
			{
				Set<Link> newSetLinks = new HashSet<Link> ();
				for (Link oldLink : originTree.linkSet) newSetLinks.add (this.cache_id2LinkMap.get(oldLink.id));
				MulticastTree newElement = new MulticastTree (this , originTree.id , originTree.index , cache_id2MulticastDemandMap.get(originTree.demand.id) , newSetLinks , originTree.attributes); 
				cache_id2MulticastTreeMap.put(originTree.id, newElement);
				newLayer.multicastTrees.add (newElement);
				newElement.carriedTraffic = originTree.carriedTraffic;
				newElement.occupiedLinkCapacity = originTree.occupiedLinkCapacity;
				newElement.carriedTrafficIfNotFailing = originTree.carriedTrafficIfNotFailing;
				newElement.occupiedLinkCapacityIfNotFailing = originTree.occupiedLinkCapacityIfNotFailing;
			}
			for (ProtectionSegment originSegment : originLayer.protectionSegments) 
			{
				List<Link> newSeqLinks = new LinkedList<Link> ();
				for (Link oldLink : originSegment.seqLinks) newSeqLinks.add (this.cache_id2LinkMap.get(oldLink.id));
				ProtectionSegment newElement = new ProtectionSegment (this , originSegment.id , originSegment.index , newSeqLinks , originSegment.capacity , originSegment.attributes); 
				cache_id2ProtectionSegmentMap.put(originSegment.id, newElement);
				newLayer.protectionSegments.add (newElement);
			}
		}

		/* Copy the rest of the fields  */
		for (Node newNode : this.nodes) newNode.copyFrom (originNetPlan.nodes.get(newNode.index));
		for (SharedRiskGroup newSrg : this.srgs) newSrg.copyFrom (originNetPlan.srgs.get(newSrg.index));
		for (NetworkLayer newLayer : this.layers) newLayer.copyFrom (originNetPlan.layers.get(newLayer.index));

		this.interLayerCoupling = new DirectedAcyclicGraph<NetworkLayer, DemandLinkMapping>(DemandLinkMapping.class);
		for (NetworkLayer layer : originNetPlan.interLayerCoupling.vertexSet()) this.interLayerCoupling.addVertex(this.netPlan.getNetworkLayerFromId(layer.id));
		for (DemandLinkMapping mapping : originNetPlan.interLayerCoupling.edgeSet())
		{
			if (mapping.isEmpty()) throw new RuntimeException ("Bad");
			DemandLinkMapping newMapping = new DemandLinkMapping();
			for (Entry<Demand,Link> originEntry : mapping.getDemandMap().entrySet())
				newMapping.put (this.netPlan.getDemandFromId(originEntry.getKey().id) , this.netPlan.getLinkFromId(originEntry.getValue().id));
			for (Entry<MulticastDemand,Set<Link>> originEntry : mapping.getMulticastDemandMap().entrySet())
			{
				Set<Link> newSetLink = new HashSet<Link> (); for (Link originLink : originEntry.getValue()) newSetLink.add (this.netPlan.getLinkFromId(originLink.id));
				newMapping.put (this.netPlan.getMulticastDemandFromId(originEntry.getKey().id) , newSetLink);
			}
			
			try { this.interLayerCoupling.addDagEdge(newMapping.getDemandSideLayer () , newMapping.getLinkSideLayer (), newMapping); } catch (Exception e) { throw new RuntimeException ("Bad: " + e); }
		}
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Returns the values of a given attribute for all the provided network elements.</p>
	 * @param collection Collection of network elements
	 * @param attribute Attribute name
	 * @return Map with the value of each attribute per network element (value may be {@code null} if not defined
	 * @see com.net2plan.interfaces.networkDesign.NetworkElement
	 */
	public static Map<NetworkElement,String> getAttributes (Collection<? extends NetworkElement> collection , String attribute)
	{
		Map<NetworkElement,String> attributes = new HashMap<NetworkElement,String> (); 
		for (NetworkElement e : collection)
			attributes.put (e , e.getAttribute (attribute));
		return attributes;
	}

	/**
	 * <p>Returns the values of a given attribute for all the provided network elements, as a {@code DoubleMatrix1D} vector.</p>
	 * @param collection Collection of network elements
	 * @param attributeName Attribute name
	 * @param defaultValue If an element has value for the attribute, or it is not a double (it fails in the {@code Double.parseDouble} conversion), then this value is set
	 * @return array with one element per {@code NetworkElement}, in the same order as the input {@code Collection}.
	 * @see com.net2plan.interfaces.networkDesign.NetworkElement
	 */
	public static DoubleMatrix1D getAttributeValues (Collection<? extends NetworkElement> collection , String attributeName , double defaultValue)
	{
		DoubleMatrix1D res = DoubleFactory1D.dense.make(collection.size());
		int counter = 0;
		for (NetworkElement e : collection)
		{
			double val = defaultValue;
			String value = e.getAttribute(attributeName);
			if (value != null)
				try { val = Double.parseDouble(value); } catch (Exception ex) {}
			res.set(counter ++ , val);
		}
		return res;
	}
	
	/**
	 * <p>Returns the demand with the given index.</p>
	 * @param index Demand index
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The demand with the given index (or {@code null} if nonexistent, index lower than 0 or greater than the number of elements minus one)
	 * @see com.net2plan.interfaces.networkDesign.Demand
	 */
	public Demand getDemand  (int index , NetworkLayer ... optionalLayerParameter) { NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter (optionalLayerParameter); if ((index < 0) || (index > layer.demands.size () -1)) return null; else return layer.demands.get(index); }

	/**
	 * <p>Returns the demand with the given unique identifier.</p>
	 * @param uid Demand unique id
	 * @return The demand with the given unique id (or {@code null} if nonexistent)
	 * @see com.net2plan.interfaces.networkDesign.Demand
	 */
	public Demand getDemandFromId (long uid) { return cache_id2DemandMap.get(uid); }

	/**
	 * <p>Returns the array of demand unique ids for the given layer (i-th position, corresponds to index i). If no layer is provided, the default layer is used.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return ArrayList of demand ids for the given layer (or the default layer if no input was provided)
	 * @see com.net2plan.interfaces.networkDesign.Demand
	 */
	public ArrayList<Long> getDemandIds (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		ArrayList<Long> res = new ArrayList<Long> (); for (Demand e : layer.demands) res.add (e.id);
		return res; 
	}

	/**
	 * <p>Returns the array of demands for the given layer (i-th position, corresponds to index i). If no layer is provided, the default layer is used.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return List of demands for the given layer (or the default layer if no input was provided)
	 * @see com.net2plan.interfaces.networkDesign.Demand
	 */
	public List<Demand> getDemands (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return (List<Demand>) Collections.unmodifiableList(layer.demands); 
	}

	/**
	 * <p>Returns the demands that have blocked traffic in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return List of demands with blocked traffic
	 * @see com.net2plan.interfaces.networkDesign.Demand
	 */
	public List<Demand> getDemandsBlocked (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		List<Demand> res = new LinkedList<Demand> (); for (Demand d : layer.demands) if (d.isBlocked()) res.add (d);
		return res;
	}

	/**
	 * <p>Returns the set of unicast demands that are coupled.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return {@code Set} of unicast demands that have blocked traffic for the given layer (or the defaukt layer if no input was provided)
	 */
	public Set<Demand> getDemandsCoupled (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<Demand> res = new HashSet<Demand> ();
		for (Demand demand : layer.demands) if (demand.coupledUpperLayerLink != null) res.add (demand);
		return res; 
	}

	/**
	 * <p>Returns the total blocked traffic, summing up all the unicast demands, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 *
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Total blocked trafic
	 */
	public double getDemandTotalBlockedTraffic(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (Demand d : layer.demands) accum += Math.max(0 , d.offeredTraffic - d.carriedTraffic); 
		return accum;
	}

	/**
	 * <p>Returns the total carried traffic, summing up for all the unicast demands, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Total carried traffic
	 */
	public double getDemandTotalCarriedTraffic(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (Demand d : layer.demands) accum += d.carriedTraffic; 
		return accum;
	}

	/**
	 * <p>Returns the total offered traffic, summing up for all the unicast demands, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Total offered traffic
	 */
	public double getDemandTotalOfferedTraffic(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (Demand d : layer.demands) accum += d.offeredTraffic; 
		return accum;
	}

	/**
	 * <p>Returns the name of the traffic units of the demands of the given layer. If no layer is provided, the default layer is assumed.</p>
	 *
	 * @param optionalLayerParameter Network layer (optional)
	 * @return {@code String} with the traffic units name of the demands
	 */
	public String getDemandTrafficUnitsName (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return layer.demandTrafficUnitsName; 
	}

	/**
	 * <p>Returns the traffic that is carried using a forwarding rule, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 *
	 * <p><b>Important</b>: Routing type must be {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}.</p>
	 * @param demand Outgoing demand
	 * @param link Link
	 * @return Carried traffic by the given link from the given demand using a forwarding rule
	 */
	public double getForwardingRuleCarriedTraffic(Demand demand , Link link)
	{
		checkInThisNetPlan(demand);
		checkInThisNetPlanAndLayer(link , demand.layer);
		demand.layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		return demand.layer.forwardingRules_x_de.get(demand.index,link.index);
	}

	/**
	 * <p>Returns the forwarding rules for the given layer. If no layer is provided, the default layer is assumed.</p>
	 *
	 * <p><b>Important</b>: Routing type must be {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The forwarding rules as a map of splitting factor (value) per demand and link (key)
	 * @see com.net2plan.utils.Pair
	 */
	public Map<Pair<Demand,Link>,Double> getForwardingRules (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);

		Map<Pair<Demand,Link>,Double> res = new HashMap<Pair<Demand,Link>,Double> ();
		IntArrayList ds = new IntArrayList (); IntArrayList es = new IntArrayList (); DoubleArrayList vals = new DoubleArrayList ();
		layer.forwardingRules_f_de.getNonZeros(ds,es,vals);
		for (int cont = 0 ; cont < ds.size () ; cont ++)
			res.put (Pair.of (layer.demands.get(ds.get(cont)) , layer.links.get(es.get(cont))) , vals.get(cont));
		return res;
	}

	/**
	 * <p>Returns the splitting factor of the forwarding rule of the given demand and link. If no layer is provided, default layer is assumed.</p>
	 * @param demand Outgoing demand
	 * @param link Link
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The splitting factor
	 */
	public double getForwardingRuleSplittingFactor (Demand demand , Link link , NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		checkInThisNetPlanAndLayer(demand , layer);
		checkInThisNetPlanAndLayer(link , layer);
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		return layer.forwardingRules_f_de.get(demand.index,link.index);
	}

	/**
	 * <p>Returns the network layer with the given unique identifier.</p>
	 * @param index Network layer index
	 * @return The network layer with the given index ({@code null} if it does not exist)
	 */
	public NetworkLayer getNetworkLayer (int index) { if ((index < 0) || (index > layers.size () -1)) return null; else return layers.get(index); }

	/**
	 * <p>Returns the network layer with the given name.</p>
	 * @param name Network layer name
	 * @return The network layer with the given name ({@code null} if it does not exist)
	 */
	public NetworkLayer getNetworkLayer (String name) { for (NetworkLayer layer : layers) if (layer.name.equals(name)) return layer; return null; }

	/**
	 * <p>Returns the node with the given index.</p>
	 * @param index Node index
	 * @return The node with the given index ({@code null} if it does not exist, index iss lesser than zero or greater than the number of elements minus one)
	 */
	public Node getNode (int index) { if ((index < 0) || (index > nodes.size () -1)) return null; else return nodes.get(index); }

	/**
	 * <p>Returns the link with the given index in the given layer. If no layer is provided, default layer is assumed.</p>
	 * @param index Link index
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Link with the given index or {@code null} if it does not exist, index islower than 0, or greater than the number of links minus one
	 */
	public Link getLink  (int index , NetworkLayer ... optionalLayerParameter) { NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter); if ((index < 0) || (index > layer.links.size () -1)) return null; else return layer.links.get(index); }

	/**
	 * <p>Returns the link with the given unique identifier.</p>
	 * @param uid Link unique id
	 * @return Link with the given id, or {@code null} if it does not exist
	 */
	public Link getLinkFromId (long uid) { checkAttachedToNetPlanObject(); return cache_id2LinkMap.get(uid); }

	/**
	 * <p>Returns the name of the capacity units of the links of the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The name of capacity units
	 */
	public String getLinkCapacityUnitsName (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return layer.linkCapacityUnitsName; 
	}

//	/**
//	 * <p>Returns a map where keys are the unique id of network elements and the values are a pair of uid representing the end nodes.</p>
//	 * @param links Network elements
//	 * @return A map with the end nodes unique ids (values) of each network element (key)
//	 * @see com.net2plan.utils.Pair
//	 */
//	public Map<Long,Pair<Long,Long>> getLinkIdMap (Collection<? extends NetworkElement> links)
//	{
//		checkInThisNetPlan(links);
//		Map<Long,Pair<Long,Long>> map = new HashMap<Long,Pair<Long,Long>> ();
//		if (links.isEmpty()) return map;
//		if (links.iterator().next() instanceof Link)
//			for (Link e : (List<Link>) links)
//			{
//				map.put(e.id, Pair.of(e.originNode.id, e.destinationNode.id));
//			}
//		else if (links.iterator().next() instanceof Demand)
//			for (Demand e : (List<Demand>) links)
//			{
//				map.put(e.id, Pair.of(e.ingressNode.id, e.egressNode.id));
//			}
//		else if (links.iterator().next() instanceof Route)
//			for (Route e : (List<Route>) links)
//			{
//				map.put(e.id, Pair.of(e.ingressNode.id, e.egressNode.id));
//			}
//		else throw new Net2PlanException ("Only can make id link maps for links, demands, routes and protection segments");
//		return map;
//	}

	/**
	 * <p>Returns the array of link ids for the given layer (i-th position, corresponds to index i). If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional
	 * @return Array of link ids
	 */
	public ArrayList<Long> getLinkIds (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		ArrayList<Long> res = new ArrayList<Long> (); for (Link e : layer.links) res.add (e.id);
		return res; 
	}

	/**
	 * <p>Return a list of all the links in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return All the links for the given (or default) layer
	 */
	public List<Link> getLinks (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return (List<Link>) Collections.unmodifiableList(layer.links); 
	}

	/**
	 * <p>Returns the set of links that are a bottleneck, i.e the fraction of occupied capacity respect to the total (including the capacities in the protection segments)
	 * is highest. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The {@code Set} of bottleneck links
	 */
	public Set<Link> getLinksAreBottleneck (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double maxRho = 0;
		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
		Set<Link> res = new HashSet<Link> ();
		for (Link e : layer.links) 
			if (Math.abs (e.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments / e.capacity - maxRho) < PRECISION_FACTOR) 
				res.add (e);
			else if (e.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments / e.capacity > maxRho)
			{
				maxRho = e.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments / e.capacity;
				res.clear (); res.add (e);
			}
		return res; 
	}

	/**
	 * <p>Returns the set of links that have zero capacity in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The {@code Set} of links with zero capacity
	 */
	public Set<Link> getLinksWithZeroCapacity (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
		Set<Link> res = new HashSet<Link> ();
		for (Link e : layer.links) if (e.capacity < PRECISION_FACTOR) res.add (e);
		return res; 
	}

	/**
	 * <p>Returns the set of links that are coupled to a multicast demand in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The {@code Set} of links coupled to a multicast demand
	 * @see com.net2plan.interfaces.networkDesign.MulticastDemand
	 */
	public Set<Link> getLinksCoupledToMulticastDemands (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<Link> res = new HashSet<Link> ();
		for (Link link : layer.links) if (link.coupledLowerLayerMulticastDemand != null) res.add (link);
		return res; 
	}

	/**
	 * <p>Returns the set of links that are couple to a unicast demand in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The {@code Set} of links coupled to a unicast demand
	 * @see com.net2plan.interfaces.networkDesign.Demand
	 */
	public Set<Link> getLinksCoupledToUnicastDemands (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<Link> res = new HashSet<Link> ();
		for (Link link : layer.links) if (link.coupledLowerLayerDemand != null) res.add (link);
		return res; 
	}

	/**
	 * <p>Returns the set of links that are down in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The {@code Set} of links that are down
	 */
	public Set<Link> getLinksDown (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return Collections.unmodifiableSet(layer.cache_linksDown); 
	}

	/**
	 * <p>Returns the set of links that are down in all layers.</p>
	 * @return The {@code Set} of links down
	 */
	public Set<Link> getLinksDownAllLayers ()
	{
		Set<Link> res = new HashSet<Link> (); for (NetworkLayer layer : layers) res.addAll (layer.cache_linksDown); return res;
	}

	/**
	 * <p>Returns the set of links that are up in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer )optional)
	 * @return The {@code Set} of links that are up
	 */
	public Set<Link> getLinksUp (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<Link> res = new HashSet<Link> (layer.links); res.removeAll (layer.cache_linksDown); return res;
	}

	/**
	 * <p>Returns the set of links that are up in all layers.</p>
	 * @return The {@code Set} of links that are up
	 */
	public Set<Link> getLinksUpAllLayers ()
	{
		Set<Link> res = new HashSet<Link> (); for (NetworkLayer layer : layers) res.addAll (getLinksUp (layer)); return res;
	}

	/**
	 * <p>Returns the set of links oversuscribed: the total occupied capacity (including the traffic in the protection segments) exceeds the link capacity
	 * (including the reserved capacity by the protection segments). If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The {@code Set} of oversubscribed links
	 */
	public Set<Link> getLinksOversubscribed (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<Link> res = new HashSet<Link> ();
		for (Link e : layer.links) if (e.capacity < e.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments) res.add (e);
		return res; 
	}

	/**
	 * <p>Returns the demand-link incidence matrix (a <i>D</i>x<i>E</i> matrix in
	 * which an element <i>&delta;<sub>de</sub></i> is equal to the number of
	 * times which traffic routes carrying traffic from demand <i>d</i> traverse
	 * link <i>e</i>). If no layer is provided, the default layer is assumed.</p>
	 *
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The demand-link incidence matrix
	 */
	public DoubleMatrix2D getMatrixDemand2LinkAssignment(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix2D delta_dr = getMatrixDemand2RouteAssignment(layer);
		DoubleMatrix2D delta_er = getMatrixLink2RouteAssignment(layer);
		
		return delta_dr.zMult(delta_er.viewDice(), null);
	}

	/**
	 * <p>Returns the route-srg incidence matrix (an <i>R</i>x<i>S</i> matrix in which an element <i>&delta;<sub>rs</sub></i> equals 1 if route <i>r</i>
	 * fails when SRG <i>s</i> is affected. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The route-srg incidence matrix
	 */
	public DoubleMatrix2D getMatrixRoute2SRGAffecting (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix2D A_rs = DoubleFactory2D.sparse.make (layer.routes.size () , srgs.size ());
		for (Route r : layer.routes) for (Link e : r.seqLinksRealPath) for (SharedRiskGroup srg : e.cache_srgs) A_rs.set (r.index , srg.index , 1.0);
		return A_rs;
	}

	/**
	 * <p>Returns the protection segment-srg incidence matrix (an <i>P</i>x<i>S</i> matrix in which an element <i>&delta;<sub>ps</sub></i> equals 1 if protection segment <i>p</i>
	 * fails when SRG <i>s</i> is affected. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The protection segment-srg incidence matrix
	 */
	public DoubleMatrix2D getMatrixProtectionSegment2SRGAffecting (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix2D A_rs = DoubleFactory2D.sparse.make (layer.protectionSegments.size () , srgs.size ());
		for (ProtectionSegment r : layer.protectionSegments) for (Link e : r.seqLinks) for (SharedRiskGroup srg : e.cache_srgs) A_rs.set (r.index , srg.index , 1.0);
		return A_rs;
	}

	/**
	 * <p>Returns the multicast tree-srg incidence matrix (an <i>T</i>x<i>S</i> matrix in which an element <i>&delta;<sub>ts</sub></i> equals 1 when multicast tree <i>t</i>
	 * fails when SRG <i>s</i> is affected. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The multicast tree-srg incidence matrix
	 */
	public DoubleMatrix2D getMatrixMulticastTree2SRGAffecting (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix2D A_rs = DoubleFactory2D.sparse.make (layer.multicastTrees.size () , srgs.size ());
		for (MulticastTree r : layer.multicastTrees) for (Link e : r.linkSet) for (SharedRiskGroup srg : e.cache_srgs) A_rs.set (r.index , srg.index , 1.0);
		return A_rs;
	}

	/**
	 * <p>Returns the multicast demand-link incidence matrix (a <i>D</i>x<i>E</i> matrix in
	 * which an element <i>&delta;<sub>de</sub></i> is equal to the number of
	 * times which multicast trees carrying traffic from demand <i>d</i> traverse
	 * link <i>e</i>). If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The multicast demand-link incidence matrix
	 */
	public DoubleMatrix2D getMatrixMulticastDemand2LinkAssignment(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix2D delta_dr = getMatrixMulticastDemand2MulticastTreeAssignment(layer);
		DoubleMatrix2D delta_er = getMatrixLink2MulticastTreeAssignment(layer);
		
		return delta_dr.zMult(delta_er.viewDice(), null);
	}

	/**
	 * <p>Returns the demand-link incidence matrix (<i>D</i>x<i>E</i> in which an element <i>&delta;<sub>de</sub></i> is equal to the amount of traffic of each demand carried in each link). Rows
	 * and columns are in increasing order of demand and link identifiers, respectively. If no layer is provided, the default layer is assumed</p>
	 *
	 * <p><b>Important</b>: Routing type must be {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Splitting ratio matrix
	 */
	public DoubleMatrix2D getMatrixDemand2LinkTrafficCarried(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING) return layer.forwardingRules_x_de.copy();
		DoubleMatrix2D x_de = DoubleFactory2D.sparse.make (layer.demands.size () , layer.links.size ());
		for (Route r : layer.routes) for (Link e : r.seqLinksRealPath) x_de.set (r.demand.index , e.index , x_de.get(r.demand.index , e.index) + r.carriedTraffic);
		return x_de;
	}


	public DoubleMatrix2D getMatrixDestination2LinkTrafficCarried(NetworkLayer ... optionalLayerParameter)
	{
		DoubleMatrix2D x_de = getMatrixDemand2LinkTrafficCarried(optionalLayerParameter);
		return this.getMatrixNodeDemandIncomingIncidence().zMult(x_de , null);
	}

	/**
	 * <p>Returns the demand-route incidence matrix (a <i>D</i>x<i>R</i> matrix in
	 * which an element <i>&delta;<sub>dr</sub></i> is equal to 1 if traffic
	 * route <i>r</i> is able to carry traffic from demand <i>d</i>). If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The demand-route incidence matrix
	 */
	public DoubleMatrix2D getMatrixDemand2RouteAssignment (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		int D = layer.demands.size ();
		int R = layer.routes.size ();
		DoubleMatrix2D delta_dr = DoubleFactory2D.sparse.make(D, R);
		for (Route r : layer.routes) delta_dr.set (r.demand.index , r.index , 1);
		return delta_dr;
	}

	/**
	 * <p>Returns the multicast demand-multicast tree incidence matrix (a <i>D</i>x<i>T</i> matrix in
	 * which an element <i>&delta;<sub>dt</sub></i> is equal to 1 if multicast tree <i>t</i> is able to carry traffic from multicast demand <i>d</i>).
	 * If no layer is provided, the default layer is
	 * assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The multicast demand-multicast tree incidence matrix
	 */
	public DoubleMatrix2D getMatrixMulticastDemand2MulticastTreeAssignment (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		int D = layer.multicastDemands.size ();
		int T = layer.multicastTrees.size ();
		DoubleMatrix2D delta_dt = DoubleFactory2D.sparse.make(D, T);
		for (MulticastTree t : layer.multicastTrees) delta_dt.set (t.demand.index , t.index , 1);
		return delta_dt;
	}

	/**
	 * <p>Returns the splitting ratio matrix (fractions of traffic entering a node from
	 * demand 'd', leaving that node through link 'e'). Rows and columns are in
	 * increasing order of demand and link identifiers, respectively.  If no layer is provided, the default layer is assumed</p>
	 *
	 * <p><b>Important</b>: Routing type must be {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Splitting ratio matrix
	 */
	public DoubleMatrix2D getMatrixDemandBasedForwardingRules (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING) return layer.forwardingRules_f_de.copy ();
		else return GraphUtils.convert_xp2xde(layer.links , layer.demands , layer.routes);
	}

	/**
	 * <p>A destination-based routing in the form of fractions <i>f<sub>te</sub></i> (fraction of the traffic targeted to node <i>t</i> that arrives (or is generated in) node <i>a</i>(<i>e</i>)
	 * (the initial node of link <i>e</i>), that is forwarded through link <i>e</i>). If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayer Network layer (optional)
	 * @return A destination-based routing from a given network design
	 */
	public DoubleMatrix2D getMatrixDestinationBasedForwardingRules (NetworkLayer ... optionalLayer)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayer);
		DoubleMatrix2D x_te = null;
		if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING)
			x_te = GraphUtils.convert_xde2xte(nodes ,layer.links , layer.demands , layer.forwardingRules_f_de);
		else
			x_te = GraphUtils.convert_xp2xte(nodes,layer.links,layer.demands,layer.routes);
		return GraphUtils.convert_xte2fte(nodes , layer.links , x_te);
	}
	
	/**
	 * Returns the link-protection segment assignment matrix (an <i>E</i>x<i>R</i> matrix in 
	 * which an element <i>&delta;<sub>ep</sub></i> is equal to the number of 
	 * times which protection segment <i>r</i> traverses link <i>e</i>).
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The link-protection segment assignment matrix
	 */
	public DoubleMatrix2D getMatrixLink2ProtectionSegmentAssignment (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		DoubleMatrix2D delta_er = DoubleFactory2D.sparse.make(layer.links.size(), layer.protectionSegments.size());
		for (ProtectionSegment r : layer.protectionSegments) for (Link e : r.seqLinks) delta_er.set (e.index , r.index , delta_er.get (e.index , r.index) + 1);
		return delta_er;
	}

	/**
	 *<p>Returns the link-route incidence matrix (an <i>E</i>x<i>R</i> matrix in
	 * which an element <i>&delta;<sub>ep</sub></i> is equal to the number of
	 * times which traffic route <i>r</i> traverses link <i>e</i>). If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The link-route incidene matrix
	 */
	public DoubleMatrix2D getMatrixLink2RouteAssignment (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		int E = layer.links.size();
		int R = layer.routes.size ();
		DoubleMatrix2D delta_er = DoubleFactory2D.sparse.make(E, R);
		for (Route r : layer.routes) for (Link e : r.seqLinksRealPath) delta_er.set (e.index , r.index , delta_er.get (e.index , r.index) + 1);
		return delta_er;
	}

	/**
	 * <p>Returns the link-multicast incidence matrix (an <i>E</i>x<i>T</i> matrix in which an element <i>&delta;<sub>et</sub></i> is equal
	 * to the number of times a multicast tree <i>t</i> traverse link <i>e</i>. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The link-multicast tree incidence matrix
	 */
	public DoubleMatrix2D getMatrixLink2MulticastTreeAssignment (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		int E = layer.links.size();
		int T = layer.multicastTrees.size ();
		DoubleMatrix2D delta_et = DoubleFactory2D.sparse.make(E, T);
		for (MulticastTree t : layer.multicastTrees) for (Link e : t.linkSet) delta_et.set (e.index , t.index , delta_et.get (e.index , t.index) + 1);
		return delta_et;
	}

	/**
	 * <p>Returns the link-srg assignment matrix (an <i>E</i>x<i>S</i> matrix in which an element <i>&delta;<sub>es</sub></i> equals 1 if link <i>e</i>
	 * fails when SRG <i>s</i> is affected. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The link-srg incidence matrix
	 */
	public DoubleMatrix2D getMatrixLink2SRGAssignment (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix2D delta_es = DoubleFactory2D.sparse.make (layer.links.size() , srgs.size());
		for (SharedRiskGroup s : srgs) 
		{
			for (Link e : s.links) if (e.layer.equals (layer)) delta_es.set (e.index , s.index , 1);
			for (Node n : s.nodes)
			{
				for (Link e : n.cache_nodeIncomingLinks) if (e.layer.equals(layer)) delta_es.set (e.index , s.index , 1);
				for (Link e : n.cache_nodeOutgoingLinks) if (e.layer.equals(layer)) delta_es.set (e.index , s.index , 1);
			}
		}
		return delta_es;
	}

	/**
	 * <p>Returns the multicast demand-link incidence matrix (<i>D</i>x<i>E</i> in which an element <i>&delta;<sub>de</sub></i> is equal to the amount of traffic of
	 * each multicast demand carried in each link). Rows
	 * and columns are in increasing order of demand and link identifiers, respectively. If no layer is provided, the default layer is assumed</p>
	 *
	 * <p><b>Important</b>: Routing type must be {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Splitting ratio matrix
	 */
	public DoubleMatrix2D getMatrixMulticastDemand2LinkTrafficCarried(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix2D x_de = DoubleFactory2D.sparse.make (layer.multicastDemands.size () , layer.links.size ());
		for (MulticastTree t : layer.multicastTrees) for (Link e : t.linkSet) x_de.set (t.demand.index , e.index , x_de.get(t.demand.index , e.index) + t.carriedTraffic);
		return x_de;
	}

	/**
	 * <p>Returns the <i>N</i>x<i>N</i> Euclidean distance matrix (derived
	 * from node coordinates), where <i>N</i> is the number of nodes within the network.</p>
	 * @return The Euclidean distance matrix
	 */
	public DoubleMatrix2D getMatrixNode2NodeEuclideanDistance ()
	{
		int N = nodes.size();
		DoubleMatrix2D out = DoubleFactory2D.dense.make(N, N);
		for(Node node_1 : nodes)
		{
			for (Node node_2 : nodes)
			{
				if (node_1.index >= node_2.index) continue;
				double physicalDistance = getNodePairEuclideanDistance(node_1, node_2);
				out.setQuick(node_1.index, node_2.index, physicalDistance);
				out.setQuick(node_2.index, node_1.index, physicalDistance);
			}
		}
		return out;
	}

	/**
	 * <p>Returns the link-link bidirectionality matrix (a <i>E</i>x<i>E</i> matrix where the element <i>&delta;<sub>ee'</sub></i> equals 1 when each position <i>e</i> and <i>e'</i> represent a bidirectional
	 * link at the given layer. If no layer is provided, default layer is assumed</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The link-link bidirectionality matrix
	 */
	public DoubleMatrix2D getMatrixLink2LinkBidirectionalityMatrix (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		final int E = layer.links.size();
		DoubleMatrix2D out = DoubleFactory2D.dense.make(E, E);
		for(Link e_1 : layer.links)
		{
			final String idBidirPair_st = e_1.getAttribute(KEY_STRING_BIDIRECTIONALCOUPLE);
			if (idBidirPair_st == null) throw new Net2PlanException ("Some links are not bidirectional. Use this method for networks created using addLinkBidirectional");
			final long idBidirPair = Long.parseLong (idBidirPair_st);
			final Link linkPair = netPlan.getLinkFromId(idBidirPair);
			if (linkPair == null) throw new Net2PlanException ("Some links are not bidirectional.");
			if ((linkPair.getOriginNode() != e_1.getDestinationNode()) ||(linkPair.getDestinationNode() != e_1.getOriginNode())) throw new Net2PlanException ("Some links are not bidirectional.");
			out.set(e_1.getIndex () , linkPair.getIndex () , 1.0);
			out.set(linkPair.getIndex () , e_1.getIndex () , 1.0);
		}
		return out;
	}
	
	/**
	 * <p>Returns the <i>N</i>x<i>N</i> Haversine distance matrix (derived
	 * from node coordinates, where 'xCoord' is equal to longitude and 'yCoord' 
	 * is equal to latitude), where <i>N</i> is the number of nodes within the network.</p>
	 *
	 * @return Haversine distance matrix
	 * @see <a href="http://www.movable-type.co.uk/scripts/latlong.html">Calculate distance, bearing and more between Latitude/Longitude points</a>
	 */
	public DoubleMatrix2D getMatrixNode2NodeHaversineDistanceInKm ()
	{
		int N = nodes.size();
		DoubleMatrix2D out = DoubleFactory2D.dense.make(N, N);
		for(Node node_1 : nodes)
		{
			for (Node node_2 : nodes)
			{
				if (node_1.index >= node_2.index) continue;
				double physicalDistance = getNodePairHaversineDistanceInKm(node_1, node_2);
				out.setQuick(node_1.index, node_2.index, physicalDistance);
				out.setQuick(node_2.index, node_1.index, physicalDistance);
			}
		}
		return out;
	}

	/**
	 * <p>Returns the traffic matrix, where rows and columns represent the ingress
	 * node and the egress node, respectively, in increasing order of identifier. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The traffic matrix
	 */
	public DoubleMatrix2D getMatrixNode2NodeOfferedTraffic(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		int N = nodes.size();
		DoubleMatrix2D trafficMatrix = DoubleFactory2D.dense.make(N, N);
		for (Demand d : layer.demands)
			trafficMatrix.setQuick(d.ingressNode.index, d.egressNode.index, trafficMatrix.get(d.ingressNode.index, d.egressNode.index) + d.offeredTraffic);
		return trafficMatrix;
	}

	/**
	 * <p>Returns a <i>N</i>x<i>N</i> matrix where each position accounts from the humber of demands that node <i>i</i> (row) as ingress node and <i>j</i> (column) as egress node.
	 * If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Adjacency matrix
	 */
	public DoubleMatrix2D getMatrixNodeDemandAdjacency (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		int N = nodes.size ();
		DoubleMatrix2D delta_nn = DoubleFactory2D.sparse.make(N , N);
		for (Demand d : layer.demands) delta_nn.set (d.ingressNode.index , d.egressNode.index , delta_nn.get (d.ingressNode.index , d.egressNode.index) + 1);
		return delta_nn;
	}

	/**
	 * <p>Returns a <i>N</i>x<i>N</i> matrix where each position accounts from the humber of multicast demands that node <i>i</i> (row) as ingress node and <i>j</i> (column) as an egress node.
	 * If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Adjacency matrix
	 */
	public DoubleMatrix2D getMatrixNodeMulticastDemandAdjacency (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		int N = nodes.size ();
		DoubleMatrix2D delta_nn = DoubleFactory2D.sparse.make(N , N);
		for (MulticastDemand d : layer.multicastDemands) for (Node egressNode : d.egressNodes) delta_nn.set (d.ingressNode.index , egressNode.index , delta_nn.get (d.ingressNode.index , egressNode.index) + 1);
		return delta_nn;
	}

	/**
	 * <p>Returns the node-demand incidence matrix (a <i>N</i>x<i>D</i> in which an element <i>&delta;<sub>nd</sub></i> equals 1 if <i>n</i> is the ingress node of <i>d</i>,
	 * -1 if <i>n</i> is the egress node of <i>d</i> and 0 otherwise).
	 * If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer
	 * @return The node-demand incidence matrix
	 */
	public DoubleMatrix2D getMatrixNodeDemandIncidence (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		int N = nodes.size ();
		int D = layer.demands.size ();
		DoubleMatrix2D delta_nd = DoubleFactory2D.sparse.make(N , D);
		for (Demand d : layer.demands)
		{
			delta_nd.set (d.ingressNode.index , d.index , 1);
			delta_nd.set (d.egressNode.index , d.index , -1);
		}
		return delta_nd;
	}

	/**
	 * <p>Returns the node-multicast demand incidence matrix (a <i>N</i>x<i>D</i> in which an element <i>&delta;<sub>nd</sub></i> equals 1 if <i>n</i> is the ingress node of <i>d</i>,
	 * -1 if <i>n</i> is an egress node of <i>d</i> and 0 otherwise).
	 * If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer
	 * @return The node-multicast demand incidence matrix
	 */
	public DoubleMatrix2D getMatrixNodeMulticastDemandIncidence (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		int N = nodes.size ();
		int MD = layer.multicastDemands.size ();
		DoubleMatrix2D delta_nd = DoubleFactory2D.sparse.make(N , MD);
		for (MulticastDemand d : layer.multicastDemands)
		{
			delta_nd.set (d.ingressNode.index , d.index , 1);
			for (Node egressNode : d.egressNodes) delta_nd.set (egressNode.index , d.index , -1);
		}
		return delta_nd;
	}

	/**
	 * <p>Returns the node-demand incoming incidence matrix (a <i>N</i>x<i>D</i> matrix in which element <i>&delta;<sub>nd</sub></i> equals 1 if demand <i>d</i> is terminated
	 * in node <i>n</i> and 0 otherwise. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The node-demand incoming incidence matrix
	 */
	public DoubleMatrix2D getMatrixNodeDemandIncomingIncidence (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		int N = nodes.size ();
		int D = layer.demands.size ();
		DoubleMatrix2D delta_nd = DoubleFactory2D.sparse.make(N , D);
		for (Demand d : layer.demands)
			delta_nd.set (d.egressNode.index , d.index , 1);
		return delta_nd;
	}

	/**
	 * <p>Returns the node-multicast demand incoming incidence matrix (a <i>N</i>x<i>D</i> matrix in which element <i>&delta;<sub>nd</sub></i> equals 1 if multicast demand <i>d</i> is terminated
	 * in node <i>n</i> and 0 otherwise. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The node-multicast demand incoming incidence matrix
	 */
	public DoubleMatrix2D getMatrixNodeMulticastDemandIncomingIncidence (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		int N = nodes.size ();
		int MD = layer.multicastDemands.size ();
		DoubleMatrix2D delta_nd = DoubleFactory2D.sparse.make(N , MD);
		for (MulticastDemand d : layer.multicastDemands)
			for (Node egressNode : d.egressNodes) delta_nd.set (egressNode.index , d.index , 1);
		return delta_nd;
	}

	/**
	 * <p>Returns the node-demand outgoing incidence matrix (a <i>N</i>x<i>D</i> matrix in which element <i>&delta;<sub>nd</sub></i> equals 1 if demand <i>d</i> is initiated
	 * in node <i>n</i> and 0 otherwise. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The node-demand outgoing incidence matrix
	 */
	public DoubleMatrix2D getMatrixNodeDemandOutgoingIncidence (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		int N = nodes.size ();
		int D = layer.demands.size ();
		DoubleMatrix2D delta_nd = DoubleFactory2D.sparse.make(N , D);
		for (Demand d : layer.demands)
			delta_nd.set (d.ingressNode.index , d.index , 1);
		return delta_nd;
	}

	/**
	 * <p>Returns the node-multicast demand outgoing incidence matrix (a <i>N</i>x<i>D</i> matrix in which element <i>&delta;<sub>nd</sub></i> equals 1 if demand <i>d</i> is initiated
	 * in node <i>n</i> and 0 otherwise. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The node-multicast demand outgoing incidence matrix
	 */
	public DoubleMatrix2D getMatrixNodeMulticastDemandOutgoingIncidence (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		int N = nodes.size ();
		int MD = layer.multicastDemands.size ();
		DoubleMatrix2D delta_nd = DoubleFactory2D.sparse.make(N , MD);
		for (MulticastDemand d : layer.multicastDemands)
			delta_nd.set (d.ingressNode.index , d.index , 1);
		return delta_nd;
	}

	/**
	 * <p>Returns the node-link adjacency matrix (a <i>N</i>x<i>N</i> matrix in which element <i>&delta;<sub>ij</sub></i> is equals to the number of links from node <i>i</i> to node <i>j</i>.
	 *  If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The node-link adjacency matrix
	 */
	public DoubleMatrix2D getMatrixNodeLinkAdjacency (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		int N = nodes.size ();
		DoubleMatrix2D delta_nn = DoubleFactory2D.sparse.make(N , N);
		for (Link e : layer.links)
			delta_nn.set (e.originNode.index , e.destinationNode.index , delta_nn.get (e.originNode.index , e.destinationNode.index) + 1);
		return delta_nn;
	}

	/**
	 * <p>Returns the node-link incidence matrix (a <i>N</i>x<i>E</i> matrix in which element <i>&delta;<sub>ne</sub></i> equals 1 if link <i>e</i> is initiated in node <i>n</i>, -1
	 * if link <i>e</i> ends in node <i>n</i>, and 0 otherwise. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The node-link incidence matrix
	 */
	public DoubleMatrix2D getMatrixNodeLinkIncidence (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		int N = nodes.size ();
		int E = layer.links.size ();
		DoubleMatrix2D delta_ne = DoubleFactory2D.sparse.make(N , E);
		for (Link e : layer.links)
		{
			delta_ne.set (e.originNode.index , e.index , 1);
			delta_ne.set (e.destinationNode.index , e.index , -1);
		}
		return delta_ne;
	}

	/**
	 * <p>Returns the node-link incoming incidence matrix (a <i>N</i>x<i>E</i> matrix in which element <i>&delta;<sub>ne</sub></i> equals 1 if link <i>e</i> is terminated in node <i>n</i>,
	 * and 0 otherwise. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The node-link incoming incidence matrix
	 */
	public DoubleMatrix2D getMatrixNodeLinkIncomingIncidence (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		int N = nodes.size ();
		int E = layer.links.size ();
		DoubleMatrix2D delta_ne = DoubleFactory2D.sparse.make(N , E);
		for (Link e : layer.links)
			delta_ne.set (e.destinationNode.index , e.index , 1);
		return delta_ne;
	}

	/**
	 * <p>Returns the node-link outgoing incidence matrix (a <i>N</i>x<i>E</i> matrix in which element <i>&delta;<sub>ne</sub></i> equals 1 if link <i>e</i> is initiated in node <i>n</i>,
	 * and 0 otherwise. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The node-link outgoing incidence matrix
	 */
	public DoubleMatrix2D getMatrixNodeLinkOutgoingIncidence (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		int N = nodes.size ();
		int E = layer.links.size ();
		DoubleMatrix2D delta_ne = DoubleFactory2D.sparse.make(N , E);
		for (Link e : layer.links)
			delta_ne.set (e.originNode.index , e.index , 1);
		return delta_ne;
	}

	/**
	 * <p>Returns the multicast demand with the given index in the given layer. If no layer is provided, default layer is assumed.</p>
	 * @param index Multicast demand index
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The multicast demand ({@code null} if it does not exist, or index is lower than 0, or greater the number of elements minus one).
	 */
	public MulticastDemand getMulticastDemand  (int index , NetworkLayer ... optionalLayerParameter) { NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter); if ((index < 0) || (index > layer.multicastDemands.size () -1)) return null; else return layer.multicastDemands.get(index); }

	/**
	 * <p>Returns the multicast demand with the given unique identifier.</p>
	 * @param uid Multicast demand unique id
	 * @return The multicast demand, or {@code null} if it does not exist
	 */
	public MulticastDemand getMulticastDemandFromId (long uid) { checkAttachedToNetPlanObject(); return cache_id2MulticastDemandMap.get(uid); }

	/**
	 * <p>Returns the array of multicast demand ids for the given layer (i-th position, corresponds to index i). If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The {@code ArrayList} of multicast demand unique ids
	 */
	public ArrayList<Long> getMulticastDemandIds (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		ArrayList<Long> res = new ArrayList<Long> (); for (MulticastDemand e : layer.multicastDemands) res.add (e.id);
		return res; 
	}

	/**
	 * <p>Returns the list of multicast demands for the given layer (i-th position, corresponds to multicast demand with index i). If no layer is provided, the default layer is used</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The list of multicast demands
	 */
	public List<MulticastDemand> getMulticastDemands (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return (List<MulticastDemand>) Collections.unmodifiableList(layer.multicastDemands); 
	}

	/**
	 * <p>Returns the multicast demands that have blocked traffic in the given layer. If no layer is provided, the default layer is assumed</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The multicast demands with blocked traffic
	 */
	public List<MulticastDemand> getMulticastDemandsBlocked (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		List<MulticastDemand> res = new LinkedList<MulticastDemand> (); for (MulticastDemand d : layer.multicastDemands) if (d.isBlocked()) res.add (d);
		return res;
	}

	/**
	 * <p>Returns the set of multicas demands that are coupled.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return the {@code Set} of multicast demands
	 */
	public Set<MulticastDemand> getMulticastDemandsCoupled (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<MulticastDemand> res = new HashSet<MulticastDemand> ();
		for (MulticastDemand demand : layer.multicastDemands) if (demand.coupledUpperLayerLinks != null) res.add (demand);
		return res; 
	}

	/**
	 * <p>Returns the total blocked traffic, summing up for all the multicast demands, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The total blocked traffic for all multicast demands
	 */
	public double getMulticastDemandTotalBlockedTraffic(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (MulticastDemand d : layer.multicastDemands) accum += Math.max(0 , d.offeredTraffic - d.carriedTraffic); 
		return accum;
	}

	/**
	 * <p>Returns the total carried traffic, summing up for all the multicast demands, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The total carried traffic for all multicast demands
	 */
	public double getMulticastDemandTotalCarriedTraffic(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (MulticastDemand d : layer.multicastDemands) accum += d.carriedTraffic; 
		return accum;
	}

	/**
	 * <p>Returns the total offered traffic, summing up for all the multicast demands, in the given layer. If no layer is provided, the default layer is assumed</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return the total offered traffic for all multicast demands
	 */
	public double getMulticastDemandTotalOfferedTraffic(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		double accum = 0; for (MulticastDemand d : layer.multicastDemands) accum += d.offeredTraffic; 
		return accum;
	}

	/**
	 * <p>Returns the multicast tree with the given index in the given layer. if no layer is provided, default layer is assumed.</p>
	 * @param index Multicast tree index
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The multicast tree  ({@code null}if it does not exist, index islower than 0, or greater than the number of elements minus one)
	 */
	public MulticastTree getMulticastTree (int index , NetworkLayer ... optionalLayerParameter) { NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter); if ((index < 0) || (index > layer.multicastTrees.size () -1)) return null; else return layer.multicastTrees.get(index); }

	/**
	 * <p>Returns the multicast tree with the given unique identifier.</p>
	 * @param uid Multicast tree unique id
	 * @return The multicast tree ({@code null} if it does not exist
	 */
	public MulticastTree getMulticastTreeFromId (long uid) { checkAttachedToNetPlanObject(); return cache_id2MulticastTreeMap.get(uid); }

	/**
	 * <p>Returns the array of multicast tree ids for the given layer (i-th position, corresponds to index i). If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return the {@code ArrayList} of multicast trees unique ids
	 */
	public ArrayList<Long> getMulticastTreeIds (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		ArrayList<Long> res = new ArrayList<Long> (); for (MulticastTree e : layer.multicastTrees) res.add (e.id);
		return res; 
	}

	/**
	 * <p>Returns the array of multicast trees for the given layer (i-th position, corresponds to index i). If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The {@code List} of multicast trees
	 */
	public List<MulticastTree> getMulticastTrees (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return (List<MulticastTree>) Collections.unmodifiableList(layer.multicastTrees); 
	}

	/**
	 * <p>Returns the set of multicast trees that are down (i.e. that traverse a link or node that has failed).</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return the {@code Set} of multicast trees that are down
	 */
	public Set<MulticastTree> getMulticastTreesDown (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Set<MulticastTree> res = new HashSet<MulticastTree> ();
		for (MulticastTree r : layer.multicastTrees) if (r.isDown()) res.add (r);
		return res; 
	}

	/**
	 * <p>Returns the description associated to the {@code NetPlan} object</p>
	 * @return The network description as a {@code String}
	 */
	public String getNetworkDescription ()
	{
		return this.networkDescription;
	}

	/**
	 * <p>Return the network element with the given unique id.</p>
	 * @param id Unique id
	 * @return The network element ({@code null} if it does not exist)
	 * @see com.net2plan.interfaces.networkDesign.NetworkElement
	 */
	public NetworkElement getNetworkElement (long id) 
	{
		NetworkElement e;
		e = cache_id2DemandMap.get(id); if (e != null) return e;
		e = cache_id2LayerMap.get(id); if (e != null) return e;
		e = cache_id2LinkMap.get(id); if (e != null) return e;
		e = cache_id2MulticastDemandMap.get(id); if (e != null) return e;
		e = cache_id2MulticastTreeMap.get(id); if (e != null) return e;
		e = cache_id2NodeMap.get(id); if (e != null) return e;
		e = cache_id2ProtectionSegmentMap.get(id); if (e != null) return e;
		e = cache_id2RouteMap.get(id); if (e != null) return e;
		e = cache_id2srgMap.get(id); if (e != null) return e;
		return null;
	}

	/**
	 * <p>Returns the first network element among the ones passed as input parameters, that has the given key-value as attribute. </p>
	 *
	 * @param listOfElements Lit of network elements
	 * @param attribute Attribute name
	 * @param value Attribute value
	 * @return First network element encountered with the given key-value ({@code null}if no network element matches or the input {@code null} or empty
	 * @see com.net2plan.interfaces.networkDesign.NetworkElement
	 */
	public static NetworkElement getNetworkElementByAttribute(Collection<? extends NetworkElement> listOfElements , String attribute, String value)
	{
		if (listOfElements == null) return null;
		for (NetworkElement e : listOfElements) 
		{
			String atValue = e.attributes.get (attribute);
			if (atValue != null)
				if (atValue.equals(value)) return e;
		}
		return null;
	}
	
	/**
	 * Returns the next identifier for a new network element (layer, node, link, demand...)
	 *
	 * @return Next identifier
	 */
	public long getNetworkElementNextId()
	{
		final long elementId = nextElementId.longValue();
		if (elementId < 0) throw new Net2PlanException(TEMPLATE_NO_MORE_NETWORK_ELEMENTS_ALLOWED);
		
		return elementId;
	}

	/**
	 * <p>Returns all the network elements among the ones passed as input parameters, that have the given key-value as attribute.
	 * Returns an empty list if no network element mathces this, the input list of elements is null or empty</p>
	 * @param listOfElements List of network elements
	 * @param attribute Attribute name
	 * @param value Attribute value
	 * @return List of all network elements with the attribute key-value
	 */
	public static Collection<? extends NetworkElement> getNetworkElementsByAttribute(Collection <? extends NetworkElement> listOfElements , String attribute, String value)
	{
		List<NetworkElement> res = new LinkedList<NetworkElement> ();
		if (listOfElements == null) return res;
		for (NetworkElement e : listOfElements) 
		{
			String atValue = e.attributes.get (attribute);
			if (atValue != null)
				if (atValue.equals(value)) res.add (e);
		}
		return res;
	}

	/**
	 * <p>Returns the network layer with the given unique identifier.</p>
	 * @param uid Network layer unique id
	 * @return The network layer with the given id ({@code null} if it does not exist)
	 */
	public NetworkLayer getNetworkLayerFromId (long uid) { return cache_id2LayerMap.get(uid); }

	/**
	 * <p> Return the default network layer.</p>
	 * @return The default network layer
	 */
	public NetworkLayer getNetworkLayerDefault () { return defaultLayer; }

	/**
	 * <p>Returns the array of layer ids (i-th position, corresponds to index i).</p>
	 * @return The {@code ArraList} of network layers
	 */
	public ArrayList<Long> getNetworkLayerIds () { ArrayList<Long> res = new ArrayList<Long> (); for (NetworkLayer n : layers) res.add (n.id); return res; }

	/**
	 * <p>Returns network layers in bottom-up order, that is, starting from the
	 * lower layers to the upper layers following coupling relationships. For layers 
	 * at the same hierarchical level, no order is guaranteed.</p>
	 * 
	 * @return The {@code Set} of network layers in topological order
	 * @since 0.3.0
	 */
	public Set<NetworkLayer> getNetworkLayerInTopologicalOrder()
	{
		Set<NetworkLayer> layers_topologicalSort = new LinkedHashSet<NetworkLayer>();
		Iterator<NetworkLayer> it = interLayerCoupling.iterator();
		while(it.hasNext())	layers_topologicalSort.add(it.next());
		return layers_topologicalSort;
	}

	/**
	 * <p>Returns the array of network layers (i-th position, corresponds to index i).</p>
	 * @return The {@code List} of network layers
	 */
	public List<NetworkLayer> getNetworkLayers () { return (List<NetworkLayer>) Collections.unmodifiableList(layers); }

	/**
	 * <p>Returns the name associated to the netPlan object</p>
	 * @return The network name
	 */
	public String getNetworkName ()
	{
		return this.networkName;
	}

	/**
	 * <p>Returns the node with the given unique identifier.</p>
 	 * @param uid Node unique id
	 * @return The node with the given id ({@code null} if it does not exist)
	 */
	public Node getNodeFromId (long uid) { return cache_id2NodeMap.get(uid); }

	/**
	 * <p>Returns the first node with the given name.</p>
	 * @param name Node name
	 * @return The first node with the given name ({@code null} if it does not exist)
	 */
  public Node getNodeByName(String name)
  {
  	for (Node n : nodes) if (n.name.equals (name)) return n;
  	return null;
  }

	/**
	 * <p>Returns the array of node ids (i-th position, corresponds to index i)</p>
	 * @return The {@code ArrayList} of nodes
	 */
	public ArrayList<Long> getNodeIds () { ArrayList<Long> res = new ArrayList<Long> (); for (Node n : nodes) res.add (n.id); return res; }

	/**
	 * <p>Gets the set of demands at the given layer from the input nodes (if {@code returnDemandsInBothDirections} is {@code true}, also the reversed links are included). If no layer is provided, the default layer is assumed.</p>
	 * @param originNode Origin node
	 * @param destinationNode Destination node
	 * @param returnDemandsInBothDirections Assume both directions
	 * @param optionalLayerParameter network layer (optional)
	 * @return the {@code Set} of demands that have the origin and destination for the given input
	 */
	public Set<Demand> getNodePairDemands (Node originNode, Node destinationNode , boolean returnDemandsInBothDirections , NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		checkInThisNetPlan(originNode);
		checkInThisNetPlan(destinationNode);
		Set<Demand> res = new HashSet<Demand> ();
		for (Demand e : originNode.cache_nodeOutgoingDemands) if (e.layer.equals (layer) && e.egressNode.equals (destinationNode)) res.add (e);
		if (returnDemandsInBothDirections) for (Demand e : originNode.cache_nodeIncomingDemands) if (e.layer.equals (layer) && e.ingressNode.equals (destinationNode)) res.add (e);
		return res;
	}

	/**
	 * <p>Gets the Euclidean distance for a node pair.</p>
	 * @param originNode Origin node
	 * @param destinationNode Destination node
	 * @return The Euclidean distance between a node pair
	 */
	public double getNodePairEuclideanDistance(Node originNode, Node destinationNode)
	{
		checkInThisNetPlan(originNode);
		checkInThisNetPlan(destinationNode);
		return Math.sqrt (Math.pow (originNode.nodeXYPositionMap.getX()  - destinationNode.nodeXYPositionMap.getX(), 2) + Math.pow (originNode.nodeXYPositionMap.getY()  - destinationNode.nodeXYPositionMap.getY() , 2));
	}

	/**
	 * <p>Gets the Haversine distance for a node pair.</p>
	 * @param originNode Origin node
	 * @param destinationNode Destination node
	 * @return The Harvesine distance between a node pair
	 * @see <a href="http://www.movable-type.co.uk/scripts/latlong.html">Calculate distance, bearing and more between Latitude/Longitude points</a>
	 */
	public double getNodePairHaversineDistanceInKm(Node originNode, Node destinationNode)
	{
		checkInThisNetPlan(originNode);
		checkInThisNetPlan(destinationNode);
		return GraphUtils.computeHaversineDistanceInKm(originNode.nodeXYPositionMap, destinationNode.nodeXYPositionMap);
	}

	/**
	 * <p>Gets the set of links at the given layer from the given nodes (if {@code returnLinksInBothDirections} is {@code true}, also the reversed links are included). If no layer is provided, the default layer is assumed.</p>
	 * @param originNode Origin node
	 * @param destinationNode Destination node
	 * @param returnLinksInBothDirections Include links in both direction
	 * @param optionalLayerParameter Network layer (optional)
	 * @return the {@code Set} of links between a pair of nodes
	 */
	public Set<Link> getNodePairLinks (Node originNode, Node destinationNode , boolean returnLinksInBothDirections , NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		checkInThisNetPlan(originNode);
		checkInThisNetPlan(destinationNode);
		Set<Link> res = new HashSet<Link> ();
		for (Link e : originNode.cache_nodeOutgoingLinks) if (e.layer.equals (layer) && e.destinationNode.equals (destinationNode)) res.add (e);
		if (returnLinksInBothDirections) for (Link e : originNode.cache_nodeIncomingLinks) if (e.layer.equals (layer) && e.originNode.equals (destinationNode)) res.add (e);
		return res;
	}

	/**
	 * <p>Gets the set of routes at the given layer from the given nodes (if {@code returnRoutesInBothDirections} is {@code true}, also the reversed routes are included).
	 * If no layer is provided, the default layer is assumed.</p>
	 * @param originNode Origin node
	 * @param destinationNode Destination node
	 * @param returnRoutesInBothDirections Include routes in both directions
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The {@code Set} of routes between a pair of nodes
	 */
	public Set<Route> getNodePairRoutes (Node originNode, Node destinationNode , boolean returnRoutesInBothDirections , NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		checkInThisNetPlan(originNode);
		checkInThisNetPlan(destinationNode);
		Set<Route> res = new HashSet<Route> ();
		for (Demand e : originNode.cache_nodeOutgoingDemands) if (e.layer.equals (layer) && e.egressNode.equals (destinationNode)) res.addAll (e.cache_routes);
		if (returnRoutesInBothDirections) for (Demand e : originNode.cache_nodeIncomingDemands) if (e.layer.equals (layer) && e.ingressNode.equals (destinationNode)) res.addAll (e.cache_routes);
		return res;
	}

	/**
	 * <p>Return the set of protection segments at the given layer for the given nodes (if {@code returnSegmentsInBothDirections} is {@code true}, also the reversed protection segments are included)
	 * If no layer is provided, default layer is assumed.</p>
	 * @param originNode Origin node
	 * @param destinationNode Destination node
	 * @param returnSegmentsInBothDirections Include protection segments in both directions
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The {@code Set} of protection segments between a pair of nodes
	 */
	public Set<ProtectionSegment> getNodePairProtectionSegments (Node originNode, Node destinationNode , boolean returnSegmentsInBothDirections , NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		checkInThisNetPlan(originNode);
		checkInThisNetPlan(destinationNode);
		Set<ProtectionSegment> res = new HashSet<ProtectionSegment> ();
		for (ProtectionSegment s : originNode.cache_nodeAssociatedSegments)
		{
			if ((s.originNode == originNode) && (s.destinationNode == destinationNode)) res.add (s);
			if (returnSegmentsInBothDirections) if ((s.originNode == destinationNode) && (s.destinationNode == originNode)) res.add (s);
		}
		return res;
	}

	/**
	 * <p>Returns the array of nodes (i-th position, corresponds to index i).</p>
 	 * @return The {@code List} of nodes
	 */
	public List<Node> getNodes () { return (List<Node>) Collections.unmodifiableList(nodes); }

//	/**
//	 * Computes the shortest path between the end nodes, using the given link costs (if null, the shortest path is in number of hops), for the 
//	 * given layer. If no layer is provided, the default layer is assumed. An infinite cost makes the link unusable. Returns null if no path exists
//	 */
//	public List<Link> computeShortestPath (Node origin , Node destination , DoubleMatrix1D linkCosts , NetworkLayer ... optionalLayerParameter)
//	{
//		checkIsModifiable();
//		if (optionalLayerParameter.length >= 2) throw new Net2PlanException ("None or one layer parameter can be supplied");
//		NetworkLayer layer = (optionalLayerParameter.length == 1)? optionalLayerParameter [0] : defaultLayer;
//		if (linkCosts == null) linkCosts = DoubleFactory1D.dense.make (layer.links.size() , 1.0);
//		if (linkCosts.size () != layer.links.size()) throw new Net2PlanException ("Wrong cost vector");
//		final Map<Long,Pair<Long,Long>> netPlanLinkMap = getLinkIdMap (layer.links);
//		Map<Long,Double> linkCost = new HashMap<Long,Double> ();
//		for (Link link : layer.links)
//			linkCost.put(link.id, linkCosts.get(link.index));
//		final List<Long> seqLinkIds =  GraphUtils.getShortestPath(netPlanLinkMap, origin.id , destination.id , linkCost);
//		List<Link> seqLinks = new LinkedList<Link> ();  for (long id : seqLinkIds) seqLinks.add (cache_id2LinkMap.get(id));
//		return seqLinks;
//	}
//

	/**
	 * <p>Returns the set of nodes that are down (iteration order corresponds to ascending order of indexes).</p>
	 * @return The {@code Set} of nodes that are down
	 */
	public Set<Node> getNodesDown ()
	{
		return Collections.unmodifiableSet(cache_nodesDown); 
	}

	/**
	 * <p>Returns the set of nodes that are up (iteration order correspond to ascending order of indexes).</p>
	 * @return The {@code Set} of nodes that are up
	 */
	public Set<Node> getNodesUp ()
	{
		Set<Node> res = new HashSet<Node> (nodes); res.removeAll(cache_nodesDown); return res;
	}

	/**
	 * <p>Returns the number of demands at the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The number of demands
	 */
	public int getNumberOfDemands (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return layer.demands.size(); 
	}

	/**
	 * <p>Returns the number of non-zero forwarding rules at the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The number of non-zero forwarding rules
	 */
	public int getNumberOfForwardingRules (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		IntArrayList ds = new IntArrayList (); IntArrayList es = new IntArrayList (); DoubleArrayList vals = new DoubleArrayList (); 
		layer.forwardingRules_f_de.getNonZeros(ds , es , vals);
		return ds.size (); 
	}

	/**
	 * <p>Returns the number of layers defined.</p>
	 * @return The number of layers
	 */
	public int getNumberOfLayers () { return layers.size(); }

	/**
	 * <p>Returns the number of links at the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter network layer
	 * @return The number of links
	 */
	public int getNumberOfLinks (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return layer.links.size(); 
	}

	/**
	 * <p>Returns the number of multicast demands at the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The number of multicast demands
	 */
	public int getNumberOfMulticastDemands (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return layer.multicastDemands.size(); 
	}

	/**
	 * <p>Returns the number of multicast trees at the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The number of multicast trees
	 */
	public int getNumberOfMulticastTrees (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return layer.multicastTrees.size(); 
	}

	/**
	 * <p>Returns the number of network nodes,</p>
	 * @return The number of nodes
	 */
	public int getNumberOfNodes () { return nodes.size(); }

	/**
	 * <p>Returns the number of node pairs.</p>
	 * @return The number of node pairs
	 */
	public int getNumberOfNodePairs () { return nodes.size() * (nodes.size()-1); }

	/**
	 * <p>Returns the number of protection segments at the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The number of protection segments
	 */
	public int getNumberOfProtectionSegments (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		return layer.protectionSegments.size(); 
	}

	/**
	 * <p>Returns the number of routes at the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The number of routes
	 */
	public int getNumberOfRoutes (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		return layer.routes.size(); 
	}

	/**
	 * <p>Returns the number of shared risk groups (SRGs) defined</p>
	 * @return The number of defined shared risk groups
	 */
	public int getNumberOfSRGs () { return srgs.size(); }

	/**
	 * <p>Returns the protection segment with the given index in the given layer. if no layer is provided, default layer is assumed.</p>
	 * @param index Protection segment index
	 * @param optionalLayerParameter network layer (optional)
	 * @return The protection segment with the given index ({@code null} if it does not exist, index is lesser thatn zero or greater that the number of elements minus one)
	 */
	public ProtectionSegment getProtectionSegment  (int index , NetworkLayer ... optionalLayerParameter) { NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter); layer.checkRoutingType(RoutingType.SOURCE_ROUTING); if ((index < 0) || (index > layer.protectionSegments.size () -1)) return null; else return layer.protectionSegments.get(index); }

	/**
	 * <p>Returns the protection segment with the given unique identifier.</p>
	 * @param uid Protection segment unique id
	 * @return The protection segment with the given id ({@code null} if it does not exist)
	 */
	public ProtectionSegment getProtectionSegmentFromId (long uid) { return cache_id2ProtectionSegmentMap.get(uid); }

	/**
	 * <p>Returns the array of protection segment ids for the given layer (i-th position, corresponds to index i). If no layer is provided, the default layer is assumed</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The {@code ArrayList} of protection segments unique ids
	 */
	public ArrayList<Long> getProtectionSegmentIds (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		ArrayList<Long> res = new ArrayList<Long> (); for (ProtectionSegment e : layer.protectionSegments) res.add (e.id);
		return res; 
	}

	/**
	 * <p>Returns the array of protection segmets for the given layer (i-th position, corresponds to index i). If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The {@code List} of protection segments
	 */
	public List<ProtectionSegment> getProtectionSegments (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		return (List<ProtectionSegment>) Collections.unmodifiableList(layer.protectionSegments); 
	}

	/**
	 * <p>Returns the set of protection segments that are down (traverse a link or node that is failed). If no layer is provided, default layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The {@code Set} of protection segments that are down
	 */
	public Set<ProtectionSegment> getProtectionSegmentsDown (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		Set<ProtectionSegment> res = new HashSet<ProtectionSegment> ();
		for (ProtectionSegment r : layer.protectionSegments) if (r.isDown()) res.add (r);
		return res; 
	}

	/**
	 * <p>Returns the route with the given index in the given layer. if no layer is provided, default layer is assumed</p>
	 * @param index Route index
	 * @param optionalLayerParameter network layer (optional)
	 * @return The route with the given index ({@code null} if it does not exist, index is lesser than zero or greater than the number of elements minus one)
	 */
	public Route getRoute  (int index , NetworkLayer ... optionalLayerParameter) { NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter); layer.checkRoutingType(RoutingType.SOURCE_ROUTING); layer.checkRoutingType(RoutingType.SOURCE_ROUTING); if ((index < 0) || (index > layer.routes.size () -1)) return null; else return layer.routes.get(index); }

	/**
	 * <p>Returns the route with the given unique identifier.</p>
	 * @param uid Rute unique id
	 * @return The route with the given id ({@code null} if it does not exist)
	 */
	public Route getRouteFromId (long uid) { return cache_id2RouteMap.get(uid); }

	/**
	 * <p>Returns the array of route ids for the given layer (i-th position, corresponds to index i). If no layer is provided, the default layer is assumed</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The {@code ArrayList} of route ides
	 */
	public ArrayList<Long> getRouteIds (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		ArrayList<Long> res = new ArrayList<Long> (); for (Route e : layer.routes) res.add (e.id);
		return res; 
	}

	/**
	 * <p>Returns the array of route ids for the given layer (i-th position, corresponds to index i). If no layer is provided, the default layer is assumed</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return the {@code List} of routes
	 */
	public List<Route> getRoutes (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		return (List<Route>) Collections.unmodifiableList(layer.routes); 
	}

	/**
	 * <p>Returns the set of routes that are down (traverse a link or node that is failed). If no layer is provided, default layer is assumed</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The {@code Set} of routes that are down
	 */
	public Set<Route> getRoutesDown (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		Set<Route> res = new HashSet<Route> ();
		for (Route r : layer.routes) if (r.isDown()) res.add (r);
		return res; 
	}

	/**
	 * <p>Returns the routing type of the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The routing type of the given layer
	 * @see com.net2plan.utils.Constants.RoutingType
	 */
	public RoutingType getRoutingType(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return layer.routingType;
	}

	/**
	 * <p>Returns the shared risk group with the given unique identifier.</p>
	 * @param uid SRG unique id
	 * @return The shared risk group with the given unique id (@code null if it does not exist)
	 */
	public SharedRiskGroup getSRGFromId (long uid) { return cache_id2srgMap.get(uid); }

	/**
	 * <p>Returns the shared risk group with the given index</p>
	 * @param index SRG index
	 * @return The shared risk group with the given index ({@code null} if it does not exist, index is lesser than zero or greater than the number of elements minus one)
	 */
	public SharedRiskGroup getSRG (int index) { if ((index < 0) || (index > srgs.size () -1)) return null; else return srgs.get(index);  }
	

	/**
	 * <p>Returns the array of shared risk group ids (i-th position, corresponds to index i)</p>
	 * @return The {@code ArrayList} of Shared Risk Groups unique ids
	 */
	public ArrayList<Long> getSRGIds () { ArrayList<Long> res = new ArrayList<Long> (); for (SharedRiskGroup n : srgs) res.add (n.id); return res; }

	/**
	 * <p>Returns the array of shared risk groups (i-th position, corresponds to index i).</p>
	 * @return The {@code List} of Shared Risk Groups
	 */
	public List<SharedRiskGroup> getSRGs () { return (List<SharedRiskGroup>) Collections.unmodifiableList(srgs); }

	/**
	 * <p>Returns a vector with the carried traffic per demand, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector of carried traffic per demand
	 */
	public DoubleMatrix1D getVectorDemandCarriedTraffic(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.demands.size());
		for (Demand e : layer.demands) res.set(e.index, e.carriedTraffic);
		return res;
	}

	/**
	 * <p>Retuns a vector with the values of all given network elements for the given attribute key.</p>
	 * <p><b>Important:</b> Each element must have the attribute set and value must be of type {@code double}</p>
	 * @param collection Network elements
	 * @param attributeKey Attribute name
	 * @return The vector of values for the given attribute name for all network elements
	 */
	public DoubleMatrix1D getVectorAttributeValues (Collection<? extends NetworkElement> collection , String attributeKey)
	{
		DoubleMatrix1D res = DoubleFactory1D.dense.make (collection.size());
		int counter = 0;
		for (NetworkElement e : collection) if (e.getAttribute(attributeKey) != null) res.set (counter ++ , Double.parseDouble(e.getAttribute(attributeKey))); else throw new Net2PlanException ("The element does not contain the attribute " + attributeKey);
		return res;
	}

	/**
	 * <p>Sets the given attributes values to all the given network elements.</p>
	 * @param collection Network elements
	 * @param attributeKey Attribute name
	 * @param values Attribute values (must have the same size as {@code collection}
	 */
	public void setVectorAttributeValues (Collection<? extends NetworkElement> collection , String attributeKey , DoubleMatrix1D values)
	{
		if (values.size () != collection.size()) throw new Net2PlanException ("The number of elements in the collection and the number of values to assign must be the same");
		int counter = 0;
		for (NetworkElement e : collection) e.setAttribute(attributeKey , Double.toString(values.get (counter ++)));
	}

	/**
	 * <p>Returns the vector with the worst propagation time (in ms) per demand at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the worst propagation time in ms per demand
	 */
	public DoubleMatrix1D getVectorDemandWorseCasePropagationTimeInMs (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.demands.size());
		for (Demand e : layer.demands) res.set(e.index, e.getWorseCasePropagationTimeInMs());
		return res;
	}

	/**
	 * <p>Returns the vector with the worst propagation time (in ms) per multicast demand at the given layer. i-th vector corresponds to i-th index of the element.. If no layer is provided, default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the worst propagation time in ms per multicast demand
	 */
	public DoubleMatrix1D getVectorMulticastDemandWorseCasePropagationTimeInMs (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.demands.size());
		for (MulticastDemand e : layer.multicastDemands) res.set(e.index, e.getWorseCasePropagationTimeInMs());
		return res;
	}

	/**
	 * <p>Returns a vector where each index equals the demand index and the value is 1 if said demand traverses oversubscrined links, 0 otherwise. If no layer is provided, default layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The vector indicating if each demand traverses oversubscribed links
	 */
	public DoubleMatrix1D getVectorDemandTraversesOversubscribedLink (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.demands.size());
		for (Demand e : layer.demands) res.set(e.index, e.isTraversingOversubscribedLinks()? 1 : 0);
		return res;
	}

	/**
	 * <p>Returns the vector indicating wheter a multicast demnd traverses (1) or not (0) oversubscribes links at the given layer.
	 * i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed </p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The vector indicating if each multicast demand traverses oversubscribed links
	 */
	public DoubleMatrix1D getVectorMulticastDemandTraversesOversubscribedLink (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.multicastDemands.size());
		for (MulticastDemand e : layer.multicastDemands) res.set(e.index, e.isTraversingOversubscribedLinks()? 1 : 0);
		return res;
	}

	/**
	 * <p>Returns a vector with the availability per Shared Risk Group (SRG).</p>
	 * @return The vector with the availability per Shared Risk group
	 */
	public DoubleMatrix1D getVectorSRGAvailability ()
	{
		DoubleMatrix1D res = DoubleFactory1D.dense.make(srgs.size());
		for (SharedRiskGroup e : srgs) res.set(e.index, e.getAvailability());
		return res;
	}

	/**
	 * <p>Returns a vector with the offered traffic per demand, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The vector with the offered traffic per demand
	 */
	public DoubleMatrix1D getVectorDemandOfferedTraffic(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.demands.size());
		for (Demand e : layer.demands) res.set(e.index, e.offeredTraffic);
		return res;
	}

	/**
	 * <p>Returns a vector with the blocked traffic per demand, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the blocked traffic per demand
	 */
	public DoubleMatrix1D getVectorDemandBlockedTraffic(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.demands.size());
		for (Demand e : layer.demands) res.set(e.index, Math.max (0 , e.offeredTraffic - e.carriedTraffic));
		return res;
	}

	/**
	 * <p>Returns a vector with the capacity per link, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The vector with the capacity per link
	 */
	public DoubleMatrix1D getVectorLinkCapacity(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.links.size());
		for (Link e : layer.links) res.set(e.index, e.capacity);
		return res;
	}

	/**
	 * <p>Returns a vector with the capacity per link, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the spare capacity per link
	 */
	public DoubleMatrix1D getVectorLinkSpareCapacity(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.links.size());
		for (Link e : layer.links) res.set(e.index, Math.max(0 , e.capacity - e.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments));
		return res;
	}

	/**
	 * <p>Returns a vector with the capacity per link reserved for protection, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the capacity per link reserver for protection
	 */
	public DoubleMatrix1D getVectorLinkCapacityReservedForProtection (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.links.size());
		for (Link e : layer.links) res.set(e.index, e.getReservedCapacityForProtection());
		return res;
	}

	/**
	 * <p>Returns a vector with the length in km in the links, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the length un km per link
	 */
	public DoubleMatrix1D getVectorLinkLengthInKm (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.links.size());
		for (Link e : layer.links) res.set(e.index, e.lengthInKm);
		return res;
	}

	/**
	 * <p>Returns a vector with the propagation delay in milliseconds in the links, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the propagation time per link
	 */
	public DoubleMatrix1D getVectorLinkPropagationDelayInMiliseconds (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.links.size());
		for (Link e : layer.links) res.set(e.index, e.getPropagationDelayInMs());
		return res;
	}

	/**
	 * <p>Returns a vector with the propagation speed in km/s in the links, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The vector with the propagation speed in km/s per link
	 */
	public DoubleMatrix1D getVectorLinkPropagationSpeedInKmPerSecond (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.links.size());
		for (Link e : layer.links) res.set(e.index, e.propagationSpeedInKmPerSecond);
		return res;
	}

	/**
	 * <p>Returns a vector with the total carried traffic per link (counting the traffic in the traversed protection segments), at the given layer. i-th vector corresponds to i-th index of the element.
	 * If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the total carried traffic per link
	 */
	public DoubleMatrix1D getVectorLinkTotalCarriedTraffic (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.links.size());
		for (Link e : layer.links) res.set(e.index, e.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments);
		return res;
	}

	/**
	 * <p>Returns a vector with the total occupied capacity in the links (counting the capacity occupied by the traffic in the traversed protection segments), at the given layer.
	 * i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the tootal occupied capacity per link
	 */
	public DoubleMatrix1D getVectorLinkTotalOccupiedCapacity (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.links.size());
		for (Link e : layer.links) res.set(e.index, e.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments);
		return res;
	}

	/**
	 * <p>Returns a vector with the up/down state in the links (1 up, 0 down), at the given layer. i-th vector corresponds to i-th index of the element.
	 * If no layer is provided, the defaulf layer is assumed</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the up/down state in the links
	 */
	public DoubleMatrix1D getVectorLinkUpState (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.links.size());
		for (Link e : layer.links) res.set(e.index, e.isUp? 1 : 0);
		return res;
	}

	/**
	 * <p>Returns a vector with the utilization per link, at the given layer. Utilization is measured as the total link occupied capacity by the link traffic
	 * including the one in the protection segments, divided by the total link capacity (accounting the reserved capacity by the protection segments).
	 * i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the utilization per link
	 */
	public DoubleMatrix1D getVectorLinkUtilizationIncludingProtectionSegments (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.links.size());
		for (Link e : layer.links) res.set(e.index, e.getUtilizationIncludingProtectionSegments());
		return res;
	}

	/**
	 * <p>Returns a vector with the utilization per link, at the given layer. Utilization is measured as the total link occupied capacity by the link traffic
	 * <b>not</b> including the one in the protection segments, divided by the total link capacity, <b>not</b> including that reserved by the protection segments.
	 * i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The link utilization vector (not including protection segments)
	 */
	public DoubleMatrix1D getVectorLinkUtilizationNotIncludingProtectionSegments (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.links.size());
		for (Link e : layer.links) res.set(e.index, e.getUtilizationNotIncludingProtectionSegments());
		return res;
	}

	/**
	 * <p>Returns a vector with the oversubscibed traffic (oversubscribed traffic being the sum of all carried traffic, including protection segments minus the capacity, or 0 if such substraction is negative) in each link at the given layer.
	 * i-th vector corresponds to i-th index of the element. If no layer is provided, default layer is assumed. </p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The vector with the oversubscribed traffic per link
	 */
	public DoubleMatrix1D getVectorLinkOversubscribedTraffic (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.links.size());
		for (Link e : layer.links) res.set(e.index, Math.max(0 , e.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments - e.capacity));
		return res;
	}

	/**
	 * <p>Returns a vector with the carried traffic per multicast demand, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The vector with the carried traffic per multicast demand
	 */
	public DoubleMatrix1D getVectorMulticastDemandCarriedTraffic(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.multicastDemands.size());
		for (MulticastDemand e : layer.multicastDemands) res.set(e.index, e.carriedTraffic);
		return res;
	}

	/**
	 * <p>Returns a vector with the offered traffic per multicast demand, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The vector with the offered traffic per multicast demand
	 */
	public DoubleMatrix1D getVectorMulticastDemandOfferedTraffic(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.multicastDemands.size());
		for (MulticastDemand e : layer.multicastDemands) res.set(e.index, e.offeredTraffic);
		return res;
	}

	/**
	 * <p>Returns a vector with the blocked traffic per multicast demand, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The vector with the blocked traffic per multicast demand
	 */
	public DoubleMatrix1D getVectorMulticastDemandBlockedTraffic(NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.multicastDemands.size());
		for (MulticastDemand e : layer.multicastDemands) res.set(e.index, Math.max(0 , e.offeredTraffic - e.carriedTraffic));
		return res;
	}

	/**
	 * <p>Returns a vector with the carried traffic per multicast tree, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The vector with the carried traffic per multicast tree
	 */
	public DoubleMatrix1D getVectorMulticastTreeCarriedTraffic (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.multicastTrees.size());
		for (MulticastTree e : layer.multicastTrees) res.set(e.index, e.carriedTraffic);
		return res;
	}

	/**
	 * <p>Returns a vector with the number of links per multicast tree, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The vector with the number of links per multicast tree
	 */
	public DoubleMatrix1D getVectorMulticastTreeNumberOfLinks (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.multicastTrees.size());
		for (MulticastTree e : layer.multicastTrees) res.set(e.index, e.linkSet.size ());
		return res;
	}

	/**
	 * <p>Returns a vector with the avergage number of hops per multicast tree at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed. </p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The average number of hops per multicast tree
	 */
	public DoubleMatrix1D getVectorMulticastTreeAverageNumberOfHops (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.multicastTrees.size());
		for (MulticastTree e : layer.multicastTrees) res.set(e.index, e.getTreeAveragePathLengthInHops());
		return res;
	}

	/**
	 * <p>Returns a vector with the occupied capacity traffic per multicast tree, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the occupied capacity per multicast tree
	 */
	public DoubleMatrix1D getVectorMulticastTreeOccupiedCapacity (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.multicastTrees.size());
		for (MulticastTree e : layer.multicastTrees) res.set(e.index, e.occupiedLinkCapacity);
		return res;
	}

	/**
	 * <p>Returns a vector with the up/down state of the nodes (1 up, 0 down). i-th vector corresponds to i-th index of the element</p>
	 * @return The vector with the up/down state of the nodes.
	 */
	public DoubleMatrix1D getVectorNodeUpState ()
	{
		DoubleMatrix1D res = DoubleFactory1D.dense.make(nodes.size());
		for (Node e : nodes) res.set(e.index, e.isUp? 1 : 0);
		return res;
	}

	/**
	 * <p>Returns the vector with the total outgoing offered traffic per node at the given layer. i-th vector corresponds to i-th index of the element. if no layer is provided, default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the total outgoing offered traffic per node
	 */
	public DoubleMatrix1D getVectorNodeIngressUnicastTraffic (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(nodes.size());
		for (Node n : nodes) { double traf = 0; for (Demand d: n.cache_nodeOutgoingDemands) if (d.layer == layer) traf += d.offeredTraffic; res.set (n.index , traf); }
		return res;
	}

	/**
	 * <p>Returns the vector with the total incoming offered traffic per node at the given layer. i-th vector corresponds to i-th index of the element. if no layer is provided, default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the total incoming offered traffic per node
	 */
	public DoubleMatrix1D getVectorNodeEgressUnicastTraffic (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(nodes.size());
		for (Node n : nodes) { double traf = 0; for (Demand d: n.cache_nodeIncomingDemands) if (d.layer == layer) traf += d.offeredTraffic; res.set (n.index , traf); }
		return res;
	}

	/**
	 * <p>Returns a vector with the carried traffic per protection segment, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the carried traffic per protection segment
	 */
	public DoubleMatrix1D getVectorProtectionSegmentCarriedTraffic (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.protectionSegments.size());
		for (ProtectionSegment e : layer.protectionSegments) res.set(e.index, e.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments);
		return res;
	}

	/**
	 * <p>Returns a vector with the length in km per protection segment, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The vector with the length in km per protection segment
	 */
	public DoubleMatrix1D getVectorProtectionSegmentLengthInKm (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.protectionSegments.size());
		for (ProtectionSegment e : layer.protectionSegments) res.set(e.index, e.lengthInKm);
		return res;
	}

	/**
	 * <p>Returns a vector with the number of links per protection segment, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the number of links per protection segment
	 */
	public DoubleMatrix1D getVectorProtectionSegmentNumberOfLinks (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.protectionSegments.size());
		for (ProtectionSegment e : layer.protectionSegments) res.set(e.index, e.seqLinks.size());
		return res;
	}

	/**
	 * <p>Returns a vector with the occupied capacity traffic per protection segment, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the occupied capacity traffic per protection segment
	 */
	public DoubleMatrix1D getVectorProtectionSegmentOccupiedCapacity (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.protectionSegments.size());
		for (ProtectionSegment e : layer.protectionSegments) res.set(e.index, e.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments);
		return res;
	}

	/**
	 * <p>Returns a vector with the carried traffic per route, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the carried traffic per route
	 */
	public DoubleMatrix1D getVectorRouteCarriedTraffic (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.routes.size());
		for (Route e : layer.routes) res.set(e.index, e.carriedTraffic);
		return res;
	}

	/**
	 * <p>Returns a vector with the offered traffic (from its associated demand) per route at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed. </p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the offered traffic per route (from is associated demand)
	 */
	public DoubleMatrix1D getVectorRouteOfferedTrafficOfAssociatedDemand (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.routes.size());
		for (Route e : layer.routes) res.set(e.index, e.demand.offeredTraffic);
		return res;
	}

	/**
	 * <p>Returns a vector with the offered traffic per multicast tree from its associated multicast demand, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the offered traffic per multicast tree from its associated multicast demand
	 */
	public DoubleMatrix1D getVectorMulticastTreeOfferedTrafficOfAssociatedMulticastDemand (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.multicastTrees.size());
		for (MulticastTree e : layer.multicastTrees) res.set(e.index, e.demand.offeredTraffic);
		return res;
	}

	/**
	 * <p>Returns a vector with the length in km per route, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the length in km per route
	 */
	public DoubleMatrix1D getVectorRouteLengthInKm (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.routes.size());
		for (Route e : layer.routes) res.set(e.index, e.getLengthInKm());
		return res;
	}

	/**
	 * <p>Returns a vector with the propagation delay in seconds per route, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The vector with the propagation delay in seconds per route
	 */
	public DoubleMatrix1D getVectorRoutePropagationDelayInMiliseconds (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.routes.size());
		for (Route e : layer.routes) res.set(e.index, e.getPropagationDelayInMiliseconds());
		return res;
	}

	/**
	 * <p>Returns a vector with the number of links per route (including the links in the traversed protection segments if any), at the given layer. i-th vector corresponds to i-th index of the element.
	 * If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return The vector with the number of links per route
	 */
	public DoubleMatrix1D getVectorRouteNumberOfLinks (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.routes.size());
		for (Route e : layer.routes) res.set(e.index, e.seqLinksRealPath.size());
		return res;
	}

	/**
	 * <p>Returns an array with the cost of each route in the layer. The cost of a route is given by the sum
	 * of the costs of its links, given by the provided cost vector. If the route traverses protection segments, the cost
	 * of its links is included. If the cost vector provided is {@code null},
	 * all links have cost one.</p>
	 * @param costs Costs array
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The cost of each route in the network
	 * @see com.net2plan.interfaces.networkDesign.Route
	 */
	public DoubleMatrix1D computeRouteCostVector (double [] costs , NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		if (costs == null) costs = DoubleUtils.ones(layer.links.size ()); else if (costs.length != layer.links.size()) throw new Net2PlanException ("The array of costs must have the same length as the number of links in the layer");
		DoubleMatrix1D res = DoubleFactory1D.dense.make (layer.routes.size());
		for (Route r : layer.routes) for (Link link : r.seqLinksRealPath) res.set (r.index , res.get(r.index) + costs [link.index]);
		return res;
	}

	/**
	 * <p>Returns an array with the cost of each multicast tree in the layer. The cost of a multicast tree is given by the sum
	 * of the costs in its links, given by the provided cost vector. If the cost vector provided is {@code null},
	 * all links have cost one. </p>
	 * @param costs Costs array
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The cost of each multicast tree
	 * @see com.net2plan.interfaces.networkDesign.MulticastTree
	 */
	public DoubleMatrix1D computeMulticastTreeCostVector (double [] costs , NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		if (costs.length != layer.links.size()) throw new Net2PlanException ("The array of costs must have the same length as the number of links in the layer");
		DoubleMatrix1D res = DoubleFactory1D.dense.make (layer.routes.size());
		for (MulticastTree t : layer.multicastTrees) for (Link link : t.linkSet) res.set (t.index , res.get(t.index) + costs [link.index]);
		return res;
	}

	/**
	 * <p>Returns a vector with the occupied capacity traffic per route, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return The vector with the occupied traffic per route
	 */
	public DoubleMatrix1D getVectorRouteOccupiedCapacity (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		DoubleMatrix1D res = DoubleFactory1D.dense.make(layer.routes.size());
		for (Route e : layer.routes) res.set(e.index, e.occupiedLinkCapacity);
		return res;
	}

	/**
	 * <p>Returns {@code true} if the network has one or more demands at the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return {@code True} if the network has demands at the given layer, {@code false} otherwise
	 */
	public boolean hasDemands (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return layer.demands.size() > 0; 
	}

	/**
	 * <p>Returns {@code true} if the network has at least one non-zero forwarding rule splitting ratio in any demand-link pair, in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter network layer (optional)
	 * @return {@code True} if the network has at least one forwarding rule, {@code false} otherwise
	 */
	public boolean hasForwardingRules (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		IntArrayList rows = new IntArrayList (); IntArrayList cols = new IntArrayList (); DoubleArrayList vals = new DoubleArrayList ();
		layer.forwardingRules_f_de.getNonZeros(rows,cols,vals);
		return (rows.size () != 0); 
	}

	/**
	 * <p>Returns {@code true} if the network has one or more links at the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return {@code True} if the network has one or more links, {@code false} otherwise
	 */
	public boolean hasLinks (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return layer.links.size() > 0; 
	}

	/**
	 * <p>Returns {@code true} if the network has one or more multicast demands at the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return {@code True} if the network has one or more multicast demand, {@code false} otherwise
	 */
	public boolean hasMulticastDemands (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return layer.multicastDemands.size() > 0; 
	}

	/**
	 * <p>Returns {@code true} if the network has one or more multicast trees at the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return {@code True} if the network has one or more multicast trees, {@code false} otherwise
	 */
	public boolean hasMulticastTrees (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		return layer.multicastTrees.size() > 0; 
	}

	/**
	 * <p>Returns {@code true} if the network has nodes.</p>
	 * @return {@code True} if the network has nodes, {@code false} otherwise
	 */
	public boolean hasNodes () { return nodes.size() > 0; }

	/**
	 * <p>Returns {@code true} if the network has one or more protection segments at the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return {@code True} if the network has one or more protection segments, {@code false} otherwise
	 */
	public boolean hasProtectionSegments (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		return layer.protectionSegments.size() > 0; 
	}

	/**
	 * <p>Returns {@code true} if the network has one or moreroutes at the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return {@code True} if the network has one or more routes, {@code false} otherwise
	 */
	public boolean hasRoutes (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		return layer.routes.size() > 0; 
	}

	/**
	 * <p>Returns {@code true} if the network has one or more shared risk groups (SRGs) defined.</p>
	 * @return {@code True} if the network has SRGs defined, {@code false} otherwise
	 */
	public boolean hasSRGs () { return srgs.size() > 0; }

	/**
	 * <p>Returns {@code true} if the network has at least one routing cycle at the given layer. If no layer is provided, default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return {@code True} if there is at least one routing cycle, {@code false} otherwise
	 */
	public boolean hasUnicastRoutingLoops (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		for (Demand d : layer.demands)
			if (d.getRoutingCycleType() != RoutingCycleType.LOOPLESS) return true;
		return false;
	}

	/**
	 * <p>Indicates whether or not a further coupling between two layers would be valid.
	 * This can be used to check if a new coupling would create cycles in the topology
	 * of layers, that is, if layer hierarchy is not broken.</p>
	 * @param lowerLayer Network lower layer
	 * @param upperLayer Network upper layer
	 * @return {@code True} if coupling between both input layers is valid, {@code false} otherwise
	 */
	public boolean isLayerCouplingValid(NetworkLayer lowerLayer , NetworkLayer upperLayer)
	{
		checkInThisNetPlan(lowerLayer);
		checkInThisNetPlan(upperLayer);
		if (lowerLayer.equals (upperLayer)) return false;
		DemandLinkMapping coupling_thisLayerPair = interLayerCoupling.getEdge(lowerLayer, upperLayer);
		if (coupling_thisLayerPair == null)
		{
			coupling_thisLayerPair = new DemandLinkMapping();
			boolean valid;
			try { valid = interLayerCoupling.addDagEdge(lowerLayer, upperLayer, coupling_thisLayerPair); }
			catch (DirectedAcyclicGraph.CycleFoundException ex) { valid = false; }
			if (valid) interLayerCoupling.removeEdge(lowerLayer, upperLayer);
			else return false;
		}
		
		return true;
	}

	/**
	 * <p>Returns {@code true} if in the given layer, the traffic of any multicast demand is carried by more than one multicast tree. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return {@code True} if traffic from at least one multicast demand is carried trhough two or more multicast trees, {@code false} otherwise
	 */
	public boolean isMulticastRoutingBifurcated (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		for (MulticastDemand d : layer.multicastDemands)
			if (d.isBifurcated ()) return true;
		return false;
	}

	/**
	 * <p>Returns {@code true} if the network has more than one layer.</p>
	 * @return {@code True} if the network has two or more layers, {@code false} otherwise
	 */
	public boolean isMultilayer () { return layers.size() > 1; }

	/**
	 * <p>Returns {@code true} if the network has just one layer</p>
	 * @return {@code True} if network has only one layer, {@code false} otherwise
	 */
	public boolean isSingleLayer () { return layers.size() == 1; }

	/**
	 * <p>Returns {@code true} if in the given layer, the traffic of any demand is carried by more than one route (in {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING}),
	 * or a node sends traffic of a demand to more than
	 * one link (in {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}). If no layer is provided, the default layer is assumed</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @return {@code True} if unicast traffic is bifurcated (see description), {@code false} otherwise
	 */
	public boolean isUnicastRoutingBifurcated (NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		for (Demand d : layer.demands)
			if (d.isBifurcated ()) return true;
		return false;
	}

	/**
	 * <p>Indicates whether or not a path, multicast tree or arbitrary collection of links (and/or protection segments) is up.
	 * This means that all links are up, and all end nodes of the links are also up</p>
	 * @param linksAndOrProtectionSegments Sequence of links
	 * @return {@code True} if all links in the sequence are up, {@code false} otherwise
	 */
	public boolean isUp (Collection<Link> linksAndOrProtectionSegments)
	{
		checkInThisNetPlan(linksAndOrProtectionSegments);
		for (Link e : linksAndOrProtectionSegments)
		{
			e.checkAttachedToNetPlanObject(this);
			if (!e.isUp ()) return false;
			if (!e.originNode.isUp) return false;
			if (!e.destinationNode.isUp) return false;
		}
		return true;
	}

	/**
	 * <p>Removes a layer, and any associated link, demand, route, protection segment or forwarding rule. If this layer is the default, the new default layer is the one with index 0 (the smallest identifier) </p>
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void removeNetworkLayer (NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (netPlan.layers.size () == 1) throw new Net2PlanException("At least one layer must exist");

		for (ProtectionSegment segment : new LinkedList<ProtectionSegment> (layer.protectionSegments)) segment.remove ();
		for (Route route : new LinkedList<Route> (layer.routes)) route.remove ();
		for (MulticastTree tree : new LinkedList<MulticastTree> (layer.multicastTrees)) tree.remove ();
		for (Link link : new LinkedList<Link> (layer.links)) link.remove ();
		for (Demand demand : new LinkedList<Demand> (layer.demands)) demand.remove ();
		for (MulticastDemand demand : new LinkedList<MulticastDemand> (layer.multicastDemands)) demand.remove ();
		
		netPlan.interLayerCoupling.removeVertex(layer);
		netPlan.cache_id2LayerMap.remove(layer.id);
		NetPlan.removeNetworkElementAndShiftIndexes(netPlan.layers , layer.index);
		if (netPlan.defaultLayer.equals(layer)) netPlan.defaultLayer = netPlan.layers.get (0);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		removeId ();
	}

	/**
	 * <p>Removes all the demands defined in the given layer. If no layer is provided, default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void removeAllDemands(NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		for (Demand d : new ArrayList<Demand> (layer.demands)) d.remove ();
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Removes all the forwarding rules in the given layer. If no layer is provided, default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void removeAllForwardingRules(NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);

		layer.forwardingRules_f_de = DoubleFactory2D.sparse.make (layer.demands.size() , layer.links.size());
		layer.forwardingRules_x_de = DoubleFactory2D.sparse.make (layer.demands.size() , layer.links.size());
		for (Demand d : layer.demands)
		{
			d.routingCycleType = RoutingCycleType.LOOPLESS;
			d.carriedTraffic = 0; if (d.coupledUpperLayerLink != null) d.coupledUpperLayerLink.capacity = d.carriedTraffic;
		}
		for (Link e : layer.links)
		{
			e.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments = e.getMulticastCarriedTraffic();
			e.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments = e.getMulticastOccupiedLinkCapacity();
		}
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Removes all the links defined in the given layer. If no layer is provided, default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void removeAllLinks(NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		for (Link e : new ArrayList<Link> (layer.links)) e.remove ();
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Removes all the multicast demands defined in the given layer. If no layer is provided, default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void removeAllMulticastDemands(NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		for (MulticastDemand d : new ArrayList<MulticastDemand> (layer.multicastDemands)) d.remove ();
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Removes all the multicast trees  defined in the given layer. If no layer is provided, default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void removeAllMulticastTrees (NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		for (MulticastTree t : new ArrayList<MulticastTree> (layer.multicastTrees)) t.remove ();
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Removes all the multicast trees carrying no traffic and occupying no link capacity defined in the given layer. If no layer is provided, default layer is used.</p>
	 *
	 * @param toleranceTrafficAndCapacityValueToConsiderUnusedTree Tolerance capacity to consider a link unsused
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void removeAllMulticastTreesUnused (double toleranceTrafficAndCapacityValueToConsiderUnusedTree , NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		for (MulticastTree t : new ArrayList<MulticastTree> (layer.multicastTrees)) if ((t.carriedTraffic < toleranceTrafficAndCapacityValueToConsiderUnusedTree) && (t.occupiedLinkCapacity < toleranceTrafficAndCapacityValueToConsiderUnusedTree)) t.remove ();
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Removes all the network layers (everything but the nodes and the SRGs). Removes all the links, demands and multicast demands of the defualt layer, it does not remove it </p>
	 */
	public void removeAllNetworkLayers ()
	{
		checkIsModifiable();
		for (NetworkLayer layer : new ArrayList<NetworkLayer> (layers)) 
		{
			if (layer != defaultLayer) { removeNetworkLayer (layer); continue; }
			removeAllLinks(layer);
			removeAllDemands(layer);
			removeAllMulticastDemands(layer);
		}
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Removes all the network nodes.</p>
	 */
	public void removeAllNodes ()
	{
		checkIsModifiable();
		for (NetworkLayer layer : layers) if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING) removeAllForwardingRules(layer); // to speed up things
		for (Node n : new ArrayList<Node> (nodes)) n.remove ();
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Removes all the protection segments defined in the given layer. If no layer is provided, default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void removeAllProtectionSegments(NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		for (ProtectionSegment s : new ArrayList<ProtectionSegment> (layer.protectionSegments)) s.remove ();
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Removes all the routes defined in the given layer. If no layer is provided, default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void removeAllRoutes(NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		for (Route r : new ArrayList<Route> (layer.routes)) r.remove ();
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Removes all the routes defined in the given layer that do not carry traffic nor occupy link capacity in the given layer. If no layer is provided, default layer is assumed.</p>
	 * @param toleranceTrafficAndCapacityValueToConsiderUnusedRoute Tolerance traffic to consider a route unused
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void removeAllRoutesUnused (double toleranceTrafficAndCapacityValueToConsiderUnusedRoute , NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		for (Route r : new ArrayList<Route> (layer.routes)) if ((r.carriedTraffic < toleranceTrafficAndCapacityValueToConsiderUnusedRoute) && (r.occupiedLinkCapacity < toleranceTrafficAndCapacityValueToConsiderUnusedRoute)) r.remove ();
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Removes all the unsused links (those whith lesser capacity than {@code toleranceCapacityValueToConsiderUnusedLink}) defined in the given layer. If no layer is provided, default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 * @param toleranceCapacityValueToConsiderUnusedLink Tolerance capacity to consider a link unsused
	 */
	public void removeAllLinksUnused (double toleranceCapacityValueToConsiderUnusedLink , NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		for (Link e : new ArrayList<Link> (layer.links)) if (e.capacity < toleranceCapacityValueToConsiderUnusedLink) e.remove ();
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Removes all the routing information (unicast and multicast) for the given layer, irrespective of the routing type
	 * setting. For source routing, all routes and protection segments are removed.
	 * For hop-by-hop routing, all forwarding rules are removed. If no layer is provided, the default layer is assumed.</p>
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void removeAllUnicastRoutingInformation(NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		switch(layer.routingType)
		{
			case SOURCE_ROUTING:
				removeAllRoutes(layer);
				removeAllProtectionSegments(layer);
				break;
				
			case HOP_BY_HOP_ROUTING:
				removeAllForwardingRules(layer);
				break;
			default:
				throw new RuntimeException("Bad");
		}
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Removes all the shared risk groups.</p>
	 */
	public void removeAllSRGs ()
	{
		checkIsModifiable();
		for (SharedRiskGroup s : new ArrayList<SharedRiskGroup> (srgs)) s.remove ();
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Resets the state of the network to an empty {@code NetPlan}.</p>
	 */
	public void reset()
	{
		checkIsModifiable();
		assignFrom(new NetPlan());
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Saves the current network plan to a given file. If extension {@code .n2p}
	 * is not in the file name, it will be added automatically.</p>
	 * @param file Output file
	 */
	public void saveToFile(File file)
	{
		String filePath = file.getPath();
		if (!filePath.toLowerCase(Locale.getDefault()).endsWith(".n2p")) file = new File(filePath + ".n2p");
		
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(file);
			saveToOutputStream(fos);
			
		}
		catch (FileNotFoundException e)
		{
			if (fos != null)
			{
				try { fos.close(); }
				catch (IOException ex) { }
			}
			
			throw new Net2PlanException(e.getMessage());
		}
	}

	/**
	 * <p>Saves the current network plan to a given output stream.</p>
	 * @param outputStream Output stream
	 */
	public void saveToOutputStream(OutputStream outputStream)
	{
		try
		{
			XMLOutputFactory2 output = (XMLOutputFactory2) XMLOutputFactory2.newFactory();
			XMLStreamWriter2 writer = (XMLStreamWriter2) output.createXMLStreamWriter(outputStream);
			
			writer.writeStartDocument("UTF-8", "1.0");
			
			XMLUtils.indent(writer, 0);
			writer.writeStartElement("network");
			writer.writeAttribute("description", getNetworkDescription());
			writer.writeAttribute("name", getNetworkName());
			writer.writeAttribute("version", Version.getFileFormatVersion());
			
			//Set<Long> nodeIds_thisNetPlan = new HashSet<Long> (getNodeIds());
			for (Node node : nodes)
			{
				boolean emptyNode = node.attributes.isEmpty();
				
				XMLUtils.indent(writer, 1);
				if (emptyNode) writer.writeEmptyElement("node");
				else writer.writeStartElement("node");
				
				Point2D position = node.nodeXYPositionMap;
				writer.writeAttribute("id", Long.toString(node.id));
				writer.writeAttribute("xCoord", Double.toString(position.getX()));
				writer.writeAttribute("yCoord", Double.toString(position.getY()));
				writer.writeAttribute("name", node.name);
				writer.writeAttribute("isUp", Boolean.toString (node.isUp));

				for (Entry<String, String> entry : node.attributes.entrySet())
				{
					XMLUtils.indent(writer, 2);
					writer.writeEmptyElement("attribute");
					writer.writeAttribute("key", entry.getKey());
					writer.writeAttribute("value", entry.getValue());
				}

				if (!emptyNode)
				{
					XMLUtils.indent(writer, 1);
					writer.writeEndElement();
				}
			}
			
			for(NetworkLayer layer : layers)
			{
				XMLUtils.indent(writer, 1);
				writer.writeStartElement("layer");

				writer.writeAttribute("id", Long.toString(layer.id));
				writer.writeAttribute("name", layer.name);
				writer.writeAttribute("description", layer.description);
				writer.writeAttribute("linkCapacityUnitsName", layer.linkCapacityUnitsName);
				writer.writeAttribute("demandTrafficUnitsName", layer.demandTrafficUnitsName);

				for(Link link : layer.links)
				{
					boolean emptyLink = link.attributes.isEmpty();
					
					XMLUtils.indent(writer, 2);
					if (emptyLink) writer.writeEmptyElement("link");
					else writer.writeStartElement("link");
					
					writer.writeAttribute("id", Long.toString(link.id));
					writer.writeAttribute("originNodeId", Long.toString(link.originNode.id));
					writer.writeAttribute("destinationNodeId", Long.toString(link.destinationNode.id));
					writer.writeAttribute("capacity", Double.toString(link.capacity));
					writer.writeAttribute("lengthInKm", Double.toString(link.lengthInKm));
					writer.writeAttribute("propagationSpeedInKmPerSecond", Double.toString(link.propagationSpeedInKmPerSecond));
					writer.writeAttribute("isUp", Boolean.toString(link.isUp));

					for (Entry<String, String> entry : link.attributes.entrySet())
					{
						XMLUtils.indent(writer, 3);
						writer.writeEmptyElement("attribute");
						writer.writeAttribute("key", entry.getKey());
						writer.writeAttribute("value", entry.getValue());
					}

					if (!emptyLink)
					{
						XMLUtils.indent(writer, 2);
						writer.writeEndElement();
					}
				}

				for(Demand demand : layer.demands)
				{
					boolean emptyDemand = demand.attributes.isEmpty();
					
					XMLUtils.indent(writer, 2);
					if (emptyDemand) writer.writeEmptyElement("demand");
					else writer.writeStartElement("demand");
					
					writer.writeAttribute("id", Long.toString(demand.id));
					writer.writeAttribute("ingressNodeId", Long.toString(demand.ingressNode.id));
					writer.writeAttribute("egressNodeId", Long.toString(demand.egressNode.id));
					writer.writeAttribute("offeredTraffic", Double.toString(demand.offeredTraffic));

					for (Entry<String, String> entry : demand.attributes.entrySet())
					{
						XMLUtils.indent(writer, 3);
						writer.writeEmptyElement("attribute");
						writer.writeAttribute("key", entry.getKey());
						writer.writeAttribute("value", entry.getValue());
					}

					if (!emptyDemand)
					{
						XMLUtils.indent(writer, 2);
						writer.writeEndElement();
					}
				}

				for(MulticastDemand demand : layer.multicastDemands)
				{
					boolean emptyDemand = demand.attributes.isEmpty();
					
					XMLUtils.indent(writer, 2);
					if (emptyDemand) writer.writeEmptyElement("multicastDemand");
					else writer.writeStartElement("multicastDemand");
					
					writer.writeAttribute("id", Long.toString(demand.id));
					writer.writeAttribute("ingressNodeId", Long.toString(demand.ingressNode.id));
					List<Long> egressNodeIds = new LinkedList<Long> (); for (Node n : demand.egressNodes) egressNodeIds.add (n.id);
					writer.writeAttribute("egressNodeIds", CollectionUtils.join(egressNodeIds, " "));
					writer.writeAttribute("offeredTraffic", Double.toString(demand.offeredTraffic));

					for (Entry<String, String> entry : demand.attributes.entrySet())
					{
						XMLUtils.indent(writer, 3);
						writer.writeEmptyElement("attribute");
						writer.writeAttribute("key", entry.getKey());
						writer.writeAttribute("value", entry.getValue());
					}

					if (!emptyDemand)
					{
						XMLUtils.indent(writer, 2);
						writer.writeEndElement();
					}
				}

				for(MulticastTree tree : layer.multicastTrees)
				{
					boolean emptyTree = tree.attributes.isEmpty();

					XMLUtils.indent(writer, 3);
					if (emptyTree) writer.writeEmptyElement("multicastTree");
					else writer.writeStartElement("multicastTree");

					writer.writeAttribute("id", Long.toString(tree.id));
					writer.writeAttribute("demandId", Long.toString(tree.demand.id));
					writer.writeAttribute("carriedTraffic", Double.toString(tree.carriedTraffic));
					writer.writeAttribute("occupiedCapacity", Double.toString(tree.occupiedLinkCapacity));
					writer.writeAttribute("carriedTrafficIfNotFailing", Double.toString(tree.carriedTrafficIfNotFailing));
					writer.writeAttribute("occupiedLinkCapacityIfNotFailing", Double.toString(tree.occupiedLinkCapacityIfNotFailing));
					List<Long> linkIds = new LinkedList<Long> (); for (Link e : tree.linkSet) linkIds.add (e.id);
					writer.writeAttribute("currentSetLinks", CollectionUtils.join(linkIds , " "));
					linkIds = new LinkedList<Long> (); for (Link e : tree.initialSetLinksWhenWasCreated) linkIds.add (e.id);
					writer.writeAttribute("linkIds", CollectionUtils.join(linkIds , " "));

					for (Entry<String, String> entry : tree.attributes.entrySet())
					{
						XMLUtils.indent(writer, 4);
						writer.writeEmptyElement("attribute");
						writer.writeAttribute("key", entry.getKey());
						writer.writeAttribute("value", entry.getValue());
					}

					if (!emptyTree)
					{
						XMLUtils.indent(writer, 3);
						writer.writeEndElement();
					}
				}

				
				if (layer.routingType == RoutingType.SOURCE_ROUTING)
				{
					boolean emptySourceRouting = !hasProtectionSegments(layer) && !hasRoutes(layer);
					XMLUtils.indent(writer, 2);
					if (emptySourceRouting) writer.writeEmptyElement("sourceRouting");
					else writer.writeStartElement("sourceRouting");
					
					for(ProtectionSegment segment : layer.protectionSegments)
					{
						boolean emptySegment = segment.attributes.isEmpty();

						XMLUtils.indent(writer, 3);
						if (emptySegment) writer.writeEmptyElement("protectionSegment");
						else writer.writeStartElement("protectionSegment");

						writer.writeAttribute("id", Long.toString(segment.id));
						writer.writeAttribute("reservedCapacity", Double.toString(segment.capacity));

						List<Long> seqLinks = new LinkedList<Long> (); for (Link e : segment.seqLinks) seqLinks.add (e.id);
						writer.writeAttribute("seqLinks", CollectionUtils.join(seqLinks, " "));

						for (Entry<String, String> entry : segment.attributes.entrySet())
						{
							XMLUtils.indent(writer, 4);
							writer.writeEmptyElement("attribute");
							writer.writeAttribute("key", entry.getKey());
							writer.writeAttribute("value", entry.getValue());
						}

						if (!emptySegment)
						{
							XMLUtils.indent(writer, 3);
							writer.writeEndElement();
						}
					}

					for(Route route : layer.routes)
					{
						boolean emptyRoute = route.attributes.isEmpty();

						XMLUtils.indent(writer, 3);
						if (emptyRoute) writer.writeEmptyElement("route");
						else writer.writeStartElement("route");

						writer.writeAttribute("id", Long.toString(route.id));
						writer.writeAttribute("demandId", Long.toString(route.demand.id));
						writer.writeAttribute("carriedTraffic", Double.toString(route.carriedTraffic));
						writer.writeAttribute("occupiedCapacity", Double.toString(route.occupiedLinkCapacity));
						writer.writeAttribute("carriedTrafficIfNotFailing", Double.toString(route.carriedTrafficIfNotFailing));
						writer.writeAttribute("occupiedLinkCapacityIfNotFailing", Double.toString(route.occupiedLinkCapacityIfNotFailing));

						List<Long> initialSeqLinksWhenCreated = new LinkedList<Long> (); for (Link e : route.initialSeqLinksWhenCreated) initialSeqLinksWhenCreated.add (e.id);
						List<Long> seqLinksAndProtectionSegments = new LinkedList<Long> (); for (Link e : route.seqLinksAndProtectionSegments) seqLinksAndProtectionSegments.add (e.id);
						List<Long> backupSegmentList = new LinkedList<Long> (); for (ProtectionSegment e : route.potentialBackupSegments) backupSegmentList.add (e.id);
						writer.writeAttribute("seqLinks", CollectionUtils.join(initialSeqLinksWhenCreated, " "));
						writer.writeAttribute("currentSeqLinksAndProtectionSegments", CollectionUtils.join(seqLinksAndProtectionSegments, " "));
						writer.writeAttribute("backupSegmentList", CollectionUtils.join(backupSegmentList, " "));

						for (Entry<String, String> entry : route.attributes.entrySet())
						{
							XMLUtils.indent(writer, 4);
							writer.writeEmptyElement("attribute");
							writer.writeAttribute("key", entry.getKey());
							writer.writeAttribute("value", entry.getValue());
						}

						if (!emptyRoute)
						{
							XMLUtils.indent(writer, 3);
							writer.writeEndElement();
						}
					}

					if (!emptySourceRouting)
					{
						XMLUtils.indent(writer, 2);
						writer.writeEndElement();
					}
				}
				else
				{
					boolean emptyHopByHopRouting = !hasForwardingRules(layer);
					XMLUtils.indent(writer, 2);
					if (emptyHopByHopRouting) writer.writeEmptyElement("hopByHopRouting");
					else writer.writeStartElement("hopByHopRouting");
					IntArrayList rows = new IntArrayList (); IntArrayList cols = new IntArrayList (); DoubleArrayList vals = new DoubleArrayList ();
					layer.forwardingRules_f_de.getNonZeros(rows,cols,vals);
					for(int cont = 0 ; cont < rows.size () ; cont ++)
					{
						final int indexDemand = rows.get (cont);
						final int indexLink = cols.get (cont);
						final double splittingRatio = vals.get (cont);
						XMLUtils.indent(writer, 3);
						writer.writeEmptyElement("forwardingRule");
						writer.writeAttribute("demandId", Long.toString(layer.demands.get(indexDemand).id));
						writer.writeAttribute("linkId", Long.toString(layer.links.get(indexLink).id));
						writer.writeAttribute("splittingRatio", Double.toString(splittingRatio));
					}

					if (!emptyHopByHopRouting)
					{
						XMLUtils.indent(writer, 2);
						writer.writeEndElement();
					}
				}

				for (Entry<String, String> entry : layer.attributes.entrySet())
				{
					XMLUtils.indent(writer, 2);
					writer.writeEmptyElement("attribute");
					writer.writeAttribute("key", entry.getKey());
					writer.writeAttribute("value", entry.getValue());
				}

				XMLUtils.indent(writer, 1);
				writer.writeEndElement();
			}
			
			for (SharedRiskGroup srg : srgs)
			{
				boolean emptySRG = srg.attributes.isEmpty() && srg.nodes.isEmpty() && srg.links.isEmpty();
				
				XMLUtils.indent(writer, 1);
				if (emptySRG) writer.writeEmptyElement("srg");
				else writer.writeStartElement("srg");
				
				writer.writeAttribute("id", Long.toString(srg.id));
				writer.writeAttribute("meanTimeToFailInHours", Double.toString(srg.meanTimeToFailInHours));
				writer.writeAttribute("meanTimeToRepairInHours", Double.toString(srg.meanTimeToRepairInHours));

				for (Node node : srg.nodes)
				{
					XMLUtils.indent(writer, 2);
					writer.writeEmptyElement("node");
					writer.writeAttribute("id", Long.toString(node.id));
				}

				for (Link link : srg.links)
				{
					XMLUtils.indent(writer, 2);
					writer.writeEmptyElement("link");
					writer.writeAttribute("linkId", Long.toString(link.id));
				}

				for (Entry<String, String> entry : srg.attributes.entrySet())
				{
					XMLUtils.indent(writer, 2);
					writer.writeEmptyElement("attribute");
					writer.writeAttribute("key", entry.getKey());
					writer.writeAttribute("value", entry.getValue());
				}

				if (!emptySRG)
				{
					XMLUtils.indent(writer, 1);
					writer.writeEndElement();
				}
			}

			
			for (DemandLinkMapping d_e : interLayerCoupling.edgeSet())
			{
				for (Entry<Demand,Link> coupling : d_e.demandLinkMapping.entrySet())
				{
					XMLUtils.indent(writer, 1);
					writer.writeEmptyElement("layerCouplingDemand");
					writer.writeAttribute("lowerLayerDemandId", "" + coupling.getKey().id);
					writer.writeAttribute("upperLayerLinkId", "" + coupling.getValue().id);
				}
				for (Entry<MulticastDemand,Set<Link>> coupling : d_e.multicastDemandLinkMapping.entrySet())
				{
					XMLUtils.indent(writer, 1);
					writer.writeEmptyElement("layerCouplingMulticastDemand");
					List<Long> linkIds = new LinkedList<Long> (); for (Link e : coupling.getValue()) linkIds.add (e.id);
					writer.writeAttribute("lowerLayerDemandId", "" + coupling.getKey().id);
					writer.writeAttribute("upperLayerLinkIds", CollectionUtils.join(linkIds , " "));
				}
			}					
				
			for (Entry<String, String> entry : this.attributes.entrySet())
			{
				XMLUtils.indent(writer, 1);
				writer.writeEmptyElement("attribute");
				writer.writeAttribute("key", entry.getKey());
				writer.writeAttribute("value", entry.getValue());
			}

			XMLUtils.indent(writer, 0);
			writer.writeEndElement();
			writer.writeEndDocument();
			writer.flush();
			writer.close();
		}
		catch (XMLStreamException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * <p>Sets the failure state (up or down) for all the links in the given layer. If no layer is provided, the default layer is assumed.</p>
	 * @param setAsUp {@code true} up, {@code false} down
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void setAllLinksFailureState (boolean setAsUp , NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		if (setAsUp)
			setLinksAndNodesFailureState (layer.links , null , null , null);
		else
			setLinksAndNodesFailureState (null , layer.links , null , null);
	}

	/**
	 * <p>Sets the failure state (up or down) for all the nodes.</p>
	 * @param setAsUp {@code true} up, {@code false} down
	 */
	public void setAllNodesFailureState (boolean setAsUp)
	{
		checkIsModifiable();
		if (setAsUp)
			setLinksAndNodesFailureState (null , null , nodes , null);
		else
			setLinksAndNodesFailureState (null , null , null , nodes);
	}

	/**
	 * <p>Changes the failure state of the links and updates the routes/trees/segments (they do not carry traffic nor occupy capacity), and hop-by-hop routing
	 * (no traffic is forwarded in links down)</p>
	 * @param linksToSetAsUp Links to set as up
	 * @param linksToSetAsDown Links to set as down
	 * @param nodesToSetAsUp Nodes to set as up
	 * @param nodesToSetAsDown Nodes to set as down
	 */
	public void setLinksAndNodesFailureState (Collection<Link> linksToSetAsUp , Collection<Link> linksToSetAsDown , Collection<Node> nodesToSetAsUp , Collection<Node> nodesToSetAsDown)
	{
		checkIsModifiable();
		if (linksToSetAsUp != null) checkInThisNetPlan(linksToSetAsUp);
		if (linksToSetAsDown != null) checkInThisNetPlan(linksToSetAsDown);
		if (nodesToSetAsUp != null) checkInThisNetPlan(nodesToSetAsUp);
		if (nodesToSetAsDown != null) checkInThisNetPlan(nodesToSetAsDown);
		Set<Link> affectedLinks = new HashSet<Link> (); 
		
//		System.out.println ("setLinksAndNodesFailureState : links to up: " + linksToSetAsUp + ", links to down: " + linksToSetAsDown + ", nodes up: " + nodesToSetAsUp + ", nodes down: " + nodesToSetAsDown);
		
		/* Take all the affected links, including the in/out links of nodes. Update their state up/down and the cache of links and nodes up down, but not the routes, trees etc. */
		if (linksToSetAsUp != null) for (Link e : linksToSetAsUp) if (!e.isUp) { e.isUp = true; e.layer.cache_linksDown.remove (e); affectedLinks.add (e); }
		if (linksToSetAsDown != null) for (Link e : linksToSetAsDown) if (e.isUp) { e.isUp = false; e.layer.cache_linksDown.add (e); affectedLinks.add (e); }
		if (nodesToSetAsUp != null)
			for (Node node : nodesToSetAsUp) 
				if (!node.isUp)
				{
					node.isUp = true;
					cache_nodesDown.remove (node); 
					affectedLinks.addAll (node.cache_nodeOutgoingLinks); 
					affectedLinks.addAll (node.cache_nodeIncomingLinks);
				}
		if (nodesToSetAsDown != null)
			for (Node node : nodesToSetAsDown) 
				if (node.isUp)
				{
					node.isUp = false;
					cache_nodesDown.add (node); 
					affectedLinks.addAll (node.cache_nodeOutgoingLinks); 
					affectedLinks.addAll (node.cache_nodeIncomingLinks);
				}

		Set<NetworkLayer> affectedLayersHopByHopRouting = new HashSet<NetworkLayer> ();
		Set<Route> affectedRoutesSourceRouting = new HashSet<Route> ();
		Set<ProtectionSegment> affectedSegmentsSourceRouting = new HashSet<ProtectionSegment> ();
		Set<MulticastTree> affectedTrees = new HashSet<MulticastTree> ();

		for (Link link : affectedLinks)
		{
			if (link.layer.routingType == RoutingType.HOP_BY_HOP_ROUTING)
			{
				if (affectedLayersHopByHopRouting.contains(link.layer)) continue;
				affectedLayersHopByHopRouting.add (link.layer);
			}
			else
			{
				affectedRoutesSourceRouting.addAll (link.cache_traversingRoutes.keySet());
				affectedSegmentsSourceRouting.addAll (link.cache_traversingSegments);
			}
			affectedTrees.addAll (link.cache_traversingTrees);
		}

//		System.out.println ("affected routes: " + affectedRoutesSourceRouting);
		for (NetworkLayer affectedLayer : affectedLayersHopByHopRouting) for (Demand d : affectedLayer.demands) d.layer.updateHopByHopRoutingDemand(d);
		netPlan.updateFailureStateRoutesTreesSegments(affectedRoutesSourceRouting);
		netPlan.updateFailureStateRoutesTreesSegments(affectedSegmentsSourceRouting);
		netPlan.updateFailureStateRoutesTreesSegments(affectedTrees);
		
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Sets the name of the units in which the offered traffic is measured (e.g. "Gbps") at the given layer. If no layer is provided, default layer is assumed.</p>
	 * @param demandTrafficUnitsName Traffic units name
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void setDemandTrafficUnitsName(String demandTrafficUnitsName , NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		try{
		for(Demand d : layer.demands)
			if (d.coupledUpperLayerLink != null) 
				if (!demandTrafficUnitsName.equals(layer.demandTrafficUnitsName))
					throw new Net2PlanException("Demand traffic units name cannot be modified since there is some coupling with other layers");
		} catch (Exception e) { e.printStackTrace(); throw e; }
		layer.demandTrafficUnitsName = demandTrafficUnitsName;
	}

	/**
	 * <p>Sets the layer description. If no layer is provided, default layer is used.</p>
	 * @param description Layer description
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void setDescription (String description , NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.description = description;
	}

	/**
	 * <p>Adds a new forwarding rule (or override an existing one), to the layer of the demand and link (must be in the same layer).</p>
	 *
	 * <p><b>Important</b>: Routing type must be {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}.</p>
	 * @param demand Demand
	 * @param link Link
	 * @param splittingRatio Splitting ratio (fraction of traffic from demand 'd' entering to the origin node of link 'e', going through link 'e').
	 *                          Must be equal or greater than 0 and equal or lesser than 1.
	 * @return The previous value of the forwarding rule
	 */
	public double setForwardingRule(Demand demand , Link link , double splittingRatio)
	{
		checkIsModifiable();
		checkInThisNetPlan(demand);
		final NetworkLayer layer = demand.layer;
		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
		checkInThisNetPlanAndLayer(link , demand.layer);
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		if (splittingRatio < 0) throw new Net2PlanException("Splitting ratio must be greater or equal than zero");
		if (splittingRatio > 1) throw new Net2PlanException("Splitting ratio must be lower or equal than one");
		double sumOutFde = 0; for (Link e : link.originNode.getOutgoingLinks(layer)) sumOutFde += layer.forwardingRules_f_de.get(demand.index, e.index);
		final double previousValueFr = layer.forwardingRules_f_de.get(demand.index , link.index);
		if (sumOutFde + splittingRatio - previousValueFr > 1 + PRECISION_FACTOR) throw new Net2PlanException("The sum of splitting factors for outgoing links cannot exceed one");
		
		layer.forwardingRules_f_de.set(demand.index , link.index , splittingRatio);
		layer.updateHopByHopRoutingDemand (demand);
		
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
		return previousValueFr;
	}

	/**
	 * <p>Adds a set of forwarding rules (or override existing ones). Demands, links and ratio are processed sequentially. All the elements must be in the same layer.</p>
	 *
	 * <p><b>Important</b>: Routing type must be {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING}.</p>
	 * @param demands Demands
	 * @param links Links
	 * @param splittingFactors Splitting ratios (fraction of traffic from demand 'd' entering to the origin node of link 'e', going through link 'e').
	 *                          Each value must be equal or greater than 0 and equal or lesser than 1.
	 * @param includeUnusedRules Inclue {@code true} or not {@code false} unused rules
	 */
	public void setForwardingRules(Collection<Demand> demands , Collection<Link> links , Collection<Double> splittingFactors , boolean includeUnusedRules)
	{
		checkIsModifiable();
		if ((demands.size () != links.size ()) || (demands.size () != splittingFactors.size ())) throw new Net2PlanException ("The number of demands, links and aplitting factors must be the same");
		if (demands.isEmpty()) return;
		NetworkLayer layer = demands.iterator().next().layer;
		checkInThisNetPlanAndLayer(demands , layer);
		checkInThisNetPlanAndLayer(links , layer);
		for (double sf : splittingFactors) if ((sf < 0) || (sf > 1)) throw new Net2PlanException("Splitting ratio must be greater or equal than zero and lower or equal than one");
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		
		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
		DoubleMatrix2D original_fde = layer.forwardingRules_f_de.copy();
		
		Iterator<Demand> it_d = demands.iterator();
		Iterator<Link> it_e = links.iterator();
		Iterator<Double> it_sf = splittingFactors.iterator();
		Set<Demand> modifiedDemands = new HashSet<Demand> ();
		while (it_d.hasNext())
		{
			final Demand demand = it_d.next ();
			final Link link = it_e.next ();
			final double splittingFactor = it_sf.next();
			if (includeUnusedRules || (splittingFactor > 1E-5))
			{
				if (splittingFactor != layer.forwardingRules_f_de.get(demand.index , link.index))
				{
					layer.forwardingRules_f_de.set(demand.index , link.index , splittingFactor);
					modifiedDemands.add (demand);
				}
			}
		}

		DoubleMatrix2D A_dn = layer.forwardingRules_f_de.zMult(layer.forwardingRules_Aout_ne , null , 1 , 0 , false , true); // traffic of demand d that leaves node n
		if (A_dn.size() > 0) if (A_dn.getMaxLocation() [0] > 1 + PRECISION_FACTOR)
		{
			layer.forwardingRules_f_de = original_fde;
			throw new Net2PlanException ("The sum of the splitting factors of the output links of a node cannot exceed one");
		}

		for (Demand demand : modifiedDemands)
			layer.updateHopByHopRoutingDemand (demand);
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}
	
	/**
	 * <p>Sets the forwarding rules for the given design. Any previous routing 
	 * information (either source routing or hop-by-hop routing) will be removed.</p>
	 * @param f_de For each demand <i>d</i> (<i>d = 0</i> refers to the first demand in {@code demandIds}, <i>d = 1</i>
	 *                refers to the second one, and so on), and each link <i>e</i> (<i>e = 0</i> refers to the first link in {@code NetPlan} object, <i>e = 1</i>
	 *                refers to the second one, and so on), {@code f_de[d][e]} sets the fraction of the traffic from demand <i>d</i> that arrives (or is generated in) node <i>a(e)</i> (the origin node of link <i>e</i>), that is forwarded through link <i>e</i>. It must hold that for every node <i>n</i> different of <i>b(d)</i>, the sum of the fractions <i>f<sub>te</sub></i> along its outgoing links must be lower or equal than 1
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void setForwardingRules(DoubleMatrix2D f_de , NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		layer.checkRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		int D = layer.demands.size ();
		int E = layer.links.size ();
		if (f_de.rows() != D || f_de.columns() != E) throw new Net2PlanException("'f_de' should be a " + D + " x" + E + " matrix (demands x links)");
		if ((D == 0) || (E == 0)) { layer.forwardingRules_f_de = f_de; return; }
		if ((D > 0) && (E > 0))
			if ((f_de.getMinLocation() [0] < -1e-3) || (f_de.getMaxLocation() [0] > 1 + 1e-3)) throw new Net2PlanException("Splitting ratios must be greater or equal than zero and lower or equal than one");
		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
		DoubleMatrix2D A_dn = layer.forwardingRules_f_de.zMult(layer.forwardingRules_Aout_ne , null , 1 , 0 , false , true); // traffic of demand d that leaves node n
		if (A_dn.size () > 0) if (A_dn.getMaxLocation() [0] > 1 + PRECISION_FACTOR)
			throw new Net2PlanException ("The sum of the splitting factors of the output links of a node cannot exceed one");

		layer.forwardingRules_f_de = f_de;
		for (Demand d : layer.demands) layer.updateHopByHopRoutingDemand(d);
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Sets the name of the units in which the link capacity is measured (e.g. "Gbps") at the given layer. If no ayer is provided, the default layer is assumed.</p>
	 * @param name Capacity units name
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void setLinkCapacityUnitsName(String name , NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		try{
		for(Link e : layer.links)
			if ((e.coupledLowerLayerDemand != null) || (e.coupledLowerLayerMulticastDemand != null))
				if (!name.equals(layer.linkCapacityUnitsName))
				{
					throw new Net2PlanException("Link capacity units name cannot be modified since there is some coupling with other layers");
				}
		} catch (Exception e) { e.printStackTrace(); throw e; }
		layer.linkCapacityUnitsName = name;
	}
	
	/**
	 * <p>Sets the network description.</p>
	 * 
	 * @param description Description (when {@code null}, it will be set to empty)
	 * @since 0.2.3
	 */
	public void setNetworkDescription(String description)
	{
		checkIsModifiable();
		networkDescription = description == null ? "" : description;
	}

	/**
	 * <p>Sets the default network layer.</p>
	 * @param layer The default network layer
	 */
	public void setNetworkLayerDefault (NetworkLayer layer) { checkInThisNetPlan(layer); this.defaultLayer = layer; }
	
	/**
	 * <p>Sets the network name.</p>
	 *
	 * @param name Name 
	 * @since 0.2.3
	 */
	public void setNetworkName(String name)
	{
		checkIsModifiable();
		networkName = name == null ? "" : name;
	}

	/**
	 * <p>Checks the flow conservation constraints, and returns the traffic carried per demand.</p>
	 * @param x_de Amount of traffic from demand {@code d} (i-th row) transmitted through link e (j-th column) in traffic units
	 * @param xdeValueAsFractionsRespectToDemandOfferedTraffic If {@code true}, {@code x_de} values are measured as fractions [0,1] instead of traffic units
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Carried traffic per demand (i-th position is the demand index and the value is the carried traffic)
	 */
	public DoubleMatrix1D checkMatrixDemandLinkCarriedTrafficFlowConservationConstraints (DoubleMatrix2D x_de , boolean xdeValueAsFractionsRespectToDemandOfferedTraffic , NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
		if ((x_de.rows() != layer.demands.size()) || (x_de.columns() != layer.links.size ())) throw new Net2PlanException ("Wrong size of x_de matrix");
		if (x_de.size ()  > 0) if (x_de.getMinLocation() [0] < -PRECISION_FACTOR) throw new Net2PlanException ("Carried traffics cannot be negative");
		final DoubleMatrix2D trafficBased_xde = xdeValueAsFractionsRespectToDemandOfferedTraffic? DoubleFactory2D.sparse.diagonal(getVectorDemandOfferedTraffic(layer)).zMult (x_de , null) : x_de;
		final DoubleMatrix2D A_ne = netPlan.getMatrixNodeLinkIncidence(layer);
		final DoubleMatrix2D Div_dn = trafficBased_xde.zMult (A_ne.viewDice () , null); // out traffic minus in traffic of demand d in node n

		DoubleMatrix1D r_d = DoubleFactory1D.dense.make (layer.demands.size ());
		for (Demand d : layer.demands)
		{
			final double outTrafficOfIngress = Div_dn.get(d.index , d.ingressNode.index);
			final double outTrafficOfEgress = Div_dn.get(d.index , d.egressNode.index);
			if (outTrafficOfIngress < -PRECISION_FACTOR) throw new Net2PlanException ("Flow conservation constraints are not met for this matrix (demand index " + d.index + ")");
			if (outTrafficOfEgress > PRECISION_FACTOR) throw new Net2PlanException ("Flow conservation constraints are not met for this matrix (demand index " + d.index + ")");
			if (Math.abs(outTrafficOfIngress + outTrafficOfEgress) > PRECISION_FACTOR) throw new Net2PlanException ("Flow conservation constraints are not met for this matrix (demand index " + d.index + ")");
			IntArrayList ns = new IntArrayList (); DoubleArrayList vals = new DoubleArrayList ();
			Div_dn.viewRow (d.index).getNonZeros(ns , vals);
			for (int cont = 0; cont < ns.size () ; cont ++)
			{
				final double divergence = vals.get(cont);
				final int n = ns.get (cont);
				if ((divergence > PRECISION_FACTOR) && (n != d.ingressNode.index)) throw new Net2PlanException ("Flow conservation constraints are not met for this matrix (demand index " + d.index + ")");
				if ((divergence < -PRECISION_FACTOR) && (n != d.egressNode.index)) throw new Net2PlanException ("Flow conservation constraints are not met for this matrix (demand index " + d.index + ")");
			}
			r_d.set(d.index , outTrafficOfIngress);
		}
		return r_d;
	}

	/**
	 * <p>Checks the flow conservation constraints, and returns the traffic carried per input output node pair.</p>
	 * @param x_te Amount of traffic targeted to node {@code t} (i-th row) transmitted through link e (j-th column) in traffi units
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Carried traffic per input (i-th row) output (j-th column) node pair
	 */
	public DoubleMatrix2D checkMatrixDestinationLinkCarriedTrafficFlowConservationConstraints (DoubleMatrix2D x_te , NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
		if ((x_te.rows() != nodes.size()) || (x_te.columns() != layer.links.size ())) throw new Net2PlanException ("Wrong size of x_te matrix");
		if (x_te.size ()  > 0) if (x_te.getMinLocation() [0] < -PRECISION_FACTOR) throw new Net2PlanException ("Carried traffics cannot be negative");
		final DoubleMatrix2D A_ne = netPlan.getMatrixNodeLinkIncidence(layer);
		DoubleMatrix2D h_st = DoubleFactory2D.dense.make (nodes.size () , nodes.size ());
		
		final DoubleMatrix2D Div_tn = x_te.zMult (A_ne.viewDice () , null); // out traffic minus in traffic of demand d in node n
		for (Node t : nodes)
			for (Node n : nodes)
			{
				final double h_nt = Div_tn.get(t.index,n.index);
				if ((t == n) && (h_nt > PRECISION_FACTOR)) throw new Net2PlanException ("Flow conservation constraints are not met for this matrix (destination index " + t.index + ", node index " + n.index + ")");
				else if ((t != n) && (h_nt < -PRECISION_FACTOR)) throw new Net2PlanException ("Flow conservation constraints are not met for this matrix (destination index " + t.index + ", node index " + n.index + ")");
				h_st.set (n.index , t.index , h_nt);
			}
		return h_st;
	}

	/**
	 * <p>Adds traffic routes (or forwarding rules, depending on the routing type) from demand-link routing at the given layer. 
	 * If no layer is provided, default layer is assumed. If the routing is SOURCE-ROUTING, the new routing will have no closed nor open loops. If the routing is 
	 * HOP-BY-HOP routing, the new routing can have open loops. However, if the routing has closed loops (which were not removed), a {@code ClosedCycleRoutingException} 
	 * will be thrown.</p>
	 * @param x_de Matrix containing the amount of traffic from demand <i>d</i> (rows) which traverses link <i>e</i> (columns)
	 * @param xdeValueAsFractionsRespectToDemandOfferedTraffic If {@code true}, {code x_de} contains the fraction of the carried traffic (between 0 and 1)
	 * @param removeCycles  If true, the open and closed loops are eliminated from the routing before any processing is done. The form in which this is done guarantees that the resulting 
	 * routing uses the same or less traffic in the links for each destination than the original routing. For removing the cycles, 
	 * the method calls to {@code removeCyclesFrom_xte} using the default ILP solver defined in Net2Plan, and no limit in the maximum solver running time.
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void setRoutingFromDemandLinkCarriedTraffic(DoubleMatrix2D x_de , boolean xdeValueAsFractionsRespectToDemandOfferedTraffic , boolean removeCycles , NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		final DoubleMatrix2D trafficBased_xde = xdeValueAsFractionsRespectToDemandOfferedTraffic? DoubleFactory2D.sparse.diagonal(getVectorDemandOfferedTraffic(layer)).zMult (x_de , null) : x_de;
		checkMatrixDemandLinkCarriedTrafficFlowConservationConstraints (trafficBased_xde , false , layer);

		if (removeCycles) x_de = GraphUtils.removeCyclesFrom_xde(nodes, layer.links, layer.demands, x_de, xdeValueAsFractionsRespectToDemandOfferedTraffic , Configuration.getOption("defaultILPSolver") , null, -1);
		
		if (layer.routingType == RoutingType.SOURCE_ROUTING)
		{
			removeAllRoutes(layer);
			removeAllProtectionSegments(layer);
			
			/* Convert the x_de variables into a set of routes for each demand */
			List<Demand> demands = new LinkedList<Demand>();
			List<Double> x_p = new LinkedList<Double>();
			List<List<Link>> seqLinks = new LinkedList<List<Link>>();
			GraphUtils.convert_xde2xp(nodes , layer.links , layer.demands , trafficBased_xde , demands, x_p, seqLinks);

			/* Update netPlan object adding the calculated routes */
			Iterator<Demand> demands_it = demands.iterator();
			Iterator<List<Link>> seqLinks_it = seqLinks.iterator();
			Iterator<Double> x_p_it = x_p.iterator();
			while(x_p_it.hasNext())
			{
				Demand demand = demands_it.next();
				List<Link> seqLinks_thisPath = seqLinks_it.next();
				double x_p_thisPath = x_p_it.next();
				addRoute(demand , x_p_thisPath , x_p_thisPath , seqLinks_thisPath, null);
			}
		}
		else if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING)
		{
			DoubleMatrix2D f_de = GraphUtils.convert_xde2fde(nodes , layer.links , layer.demands , trafficBased_xde);
			setForwardingRules(f_de,  layer);
		}
		else throw new RuntimeException ("Bad");
		
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Adds traffic routes (or forwarding rules, depending on the routing type) from destination-link routing at the given layer. 
	 * If no layer is provided, default layer is assumed. If the routing is SOURCE-ROUTING, the new routing will have no closed nor open loops. If the routing is 
	 * HOP-BY-HOP routing, the new routing can have open loops. However, if the routing has closed loops (which were not removed), a {@code ClosedCycleRoutingException} 
	 * will be thrown </p>
	 * @param x_te  For each destination node <i>t</i> (rows), and each link <i>e</i> (columns), {@code f_te[t][e]} represents the traffic targeted to node <i>t</i> that arrives (or is generated
	 *                 in) node a(e) (the origin node of link e), that is forwarded through link e
	 * @param removeCycles  If true, the open and closed loops are eliminated from the routing before any processing is done. The form in which this is done guarantees that the resulting 
	 * routing uses the same or less traffic in the links for each destination than the original routing. For removing the cycles, 
	 * the method calls to {@code removeCyclesFrom_xte} using the default ILP solver defined in Net2Plan, and no limit in the maximum solver running time.
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void setRoutingFromDestinationLinkCarriedTraffic(DoubleMatrix2D x_te , boolean removeCycles , NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		checkMatrixDestinationLinkCarriedTrafficFlowConservationConstraints(x_te , layer);

		if (removeCycles) x_te = GraphUtils.removeCyclesFrom_xte(nodes, layer.links, getMatrixNode2NodeOfferedTraffic(layer), x_te, Configuration.getOption("defaultILPSolver") , null, -1);
		
		if (layer.routingType == RoutingType.SOURCE_ROUTING)
		{
			removeAllRoutes(layer);
			removeAllProtectionSegments(layer);
			
			/* Convert the x_te variables into a set of routes for each demand */
			DoubleMatrix2D f_te = GraphUtils.convert_xte2fte(nodes , layer.links , x_te);
			List<Demand> demands_p = new LinkedList<Demand>();
			List<Double> x_p = new LinkedList<Double>();
			List<List<Link>> seqLinks_p = new LinkedList<List<Link>>();
			GraphUtils.convert_fte2xp(nodes , layer.links , layer.demands , getVectorDemandOfferedTraffic(layer) , f_te, demands_p, x_p, seqLinks_p);
			
			/* Update netPlan object adding the calculated routes */
			Iterator<Demand> demands_it = demands_p.iterator();
			Iterator<Double> x_p_it = x_p.iterator();
			Iterator<List<Link>> seqLinks_it = seqLinks_p.iterator();
			while(x_p_it.hasNext())
			{
				Demand demand = demands_it.next();
				List<Link> seqLinks_thisPath = seqLinks_it.next();
				double x_p_thisPath = x_p_it.next();
				addRoute(demand , x_p_thisPath , x_p_thisPath , seqLinks_thisPath, null);
			}
		}
		else if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING)
		{
			DoubleMatrix2D f_te = GraphUtils.convert_xte2fte(nodes , layer.links , x_te);
			DoubleMatrix2D f_de = GraphUtils.convert_fte2fde(layer.demands , f_te);
			setForwardingRules(f_de,  layer);
		}
		else throw new RuntimeException ("Bad");

		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Sets the routing type at the given layer. If there is some previous routing information, it
	 * will be converted to the new type. If no layer is provided, default layer is assumed. In the conversion from 
	 * HOP-BY-HOP to SOURCE-ROUTING: (i) the demands with open loops are routed so these loops are removed, and the resulting 
	 * routing consumes the same or less bandwidth in the demand traversed links, (ii) the demands with closed loops are 
	 * routed so that the traffic that enters the closed loops is not carried. These modifications are done since open or close 
	 * loops would require routes with an infinite number of links to be fairly represented.</p>
	 * 
	 * @param optionalLayerParameter Network layer (optional)
	 * @param newRoutingType {@link com.net2plan.utils.Constants.RoutingType RoutingType}
	 */
	public void setRoutingType(RoutingType newRoutingType , NetworkLayer ... optionalLayerParameter)
	{
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		if (layer.routingType == newRoutingType) return;
		
		switch(newRoutingType)
		{
			case HOP_BY_HOP_ROUTING:
			{
				layer.forwardingRules_x_de = GraphUtils.convert_xp2xde(layer.links, layer.demands, layer.routes);
				layer.forwardingRules_Aout_ne = this.getMatrixNodeLinkOutgoingIncidence(layer);
				layer.forwardingRules_Ain_ne = this.getMatrixNodeLinkIncomingIncidence(layer);
				layer.forwardingRules_f_de = DoubleFactory2D.sparse.make (layer.demands.size(),layer.links.size ());
				DoubleMatrix2D A_dn = layer.forwardingRules_x_de.zMult(layer.forwardingRules_Aout_ne , null , 1 , 0 , false , true); // traffic of demand d that leaves node n
				removeAllProtectionSegments(layer);
				removeAllRoutes(layer);
				layer.routingType = RoutingType.HOP_BY_HOP_ROUTING;
				
				IntArrayList ds = new IntArrayList();
				IntArrayList es = new IntArrayList();
				DoubleArrayList trafs = new DoubleArrayList();
				layer.forwardingRules_x_de.getNonZeros(ds , es , trafs);
				for (int cont = 0 ; cont < ds.size() ; cont ++)
				{
					final int d = ds.get(cont); final int e = es.get(cont);
					double outTraf = A_dn.get (d , layer.links.get(e).originNode.index);
					layer.forwardingRules_f_de.set (d , e , outTraf == 0? 0 : trafs.get(cont) / outTraf );
				}
				/* update link and demand carried traffics, and demand routing cycle type */
				layer.forwardingRules_x_de.assign(0); // this is recomputed inside next call
				for (Demand d : layer.demands) layer.updateHopByHopRoutingDemand(d);
				
				break;
			}	
			
			case SOURCE_ROUTING:
			{	
				layer.routingType = RoutingType.SOURCE_ROUTING;
				removeAllRoutes(layer);
				removeAllProtectionSegments(layer);
				for (Link e : layer.links)
				{
					e.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments = e.getMulticastCarriedTraffic();
					e.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments = e.getMulticastOccupiedLinkCapacity();
				}
				for (Demand d : layer.demands) d.carriedTraffic = 0;
				
				List<Demand> d_p = new LinkedList<Demand> ();
				List<Double> x_p = new LinkedList<Double> ();
				List<List<Link>> pathList = new LinkedList<List<Link>> ();
				GraphUtils.convert_xde2xp(nodes, layer.links , layer.demands , layer.forwardingRules_x_de , d_p, x_p, pathList);
				Iterator<Demand> it_demand = d_p.iterator();
				Iterator<Double> it_xp = x_p.iterator();
				Iterator<List<Link>> it_pathList = pathList.iterator();
				while (it_demand.hasNext())
				{
					final Demand d = it_demand.next();
					final double trafficInPath = it_xp.next();
					final List<Link> seqLinks = it_pathList.next();
					addRoute(d, trafficInPath , trafficInPath , seqLinks , null);
				}
//				final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
//				final int E = layer.links.size ();
//				Graph<Node, Link> graph = JUNGUtils.getGraphFromLinkMap(nodes , layer.links);
//				Transformer<Link, Double> nev = JUNGUtils.getEdgeWeightTransformer(null);
//				for (Demand demand : layer.demands)
//				{
//					Node ingressNode = demand.getIngressNode();
//					Node egressNode = demand.getEgressNode();
//					DoubleMatrix1D x_e = layer.forwardingRules_x_de.viewRow(demand.getIndex()).copy();
//					
//					Collection<Link> incomingLinksToIngressNode = graph.getInEdges(ingressNode);
//					Collection<Link> outgoingLinksFromIngressNode = graph.getOutEdges(ingressNode);
//					if (incomingLinksToIngressNode == null) incomingLinksToIngressNode = new LinkedHashSet<Link>();
//					if (outgoingLinksFromIngressNode == null) outgoingLinksFromIngressNode = new LinkedHashSet<Link>();
//
//					double divAtIngressNode = 0;
//					for(Link link : outgoingLinksFromIngressNode)
//						divAtIngressNode += x_e.get(link.getIndex());
//					
//					for(Link link : incomingLinksToIngressNode)
//						divAtIngressNode -= x_e.get(link.getIndex());
//					
//					while (divAtIngressNode > PRECISION_FACTOR)
//					{
//						List<Link> candidateLinks = new LinkedList<Link>(); for (int e = 0; e < E ; e ++) if (x_e.get(e) > 0)candidateLinks.add(layer.links.get(e));
//						if (candidateLinks.isEmpty()) break;
//						
//						Graph<Node, Link> auxGraph = JUNGUtils.filterGraph(graph, null, null, candidateLinks, null);
//						List<Link> seqLinks = JUNGUtils.getShortestPath(auxGraph, nev, ingressNode, egressNode);
//						if (seqLinks.isEmpty()) break;
//
//						double trafficInPath = Double.MAX_VALUE;
//						for(Link link : seqLinks) trafficInPath = Math.min(trafficInPath, x_e.get(link.getIndex()));
//						
//						trafficInPath = Math.min(trafficInPath, divAtIngressNode);
//						divAtIngressNode -= trafficInPath;
//
//						addRoute(demand, trafficInPath , trafficInPath , seqLinks , null);
//
//						for (Link link  : seqLinks)
//						{
//							double newTrafficValue_thisLink = x_e.get(link.getIndex()) - trafficInPath;
//							if (newTrafficValue_thisLink < PRECISION_FACTOR) x_e.set(link.getIndex() , 0);
//							else x_e.set(link.getIndex(), newTrafficValue_thisLink);
//						}
//
//						if (divAtIngressNode <= PRECISION_FACTOR) break;
//					}
//				}
				
				/* Remove previous 'hop-by-hop' routing information */
				layer.forwardingRules_f_de = null;
				layer.forwardingRules_x_de = null;
				layer.forwardingRules_Ain_ne = null;
				layer.forwardingRules_Aout_ne = null;
				break;
			}
			
			default:
				throw new RuntimeException("Bad - Unknown routing type " + newRoutingType);
		}
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Sets the traffic demands at the given layer from a given traffic matrix, removing any previous
	 * demand. If no layer is provided, default layer is assumed.</p>
	 * 
	 * <p><b>Important</b>: Matrix values must be strictly non-negative and matrix size have to be <i>N</i>x<i>N</i> (where <i>N</i> is the number of nodes)
	 * 
	 * @param optionalLayerParameter Network layer (optional
	 * @param trafficMatrix Traffic matrix
	 * @since 0.3.1
	 */
	public void setTrafficMatrix(DoubleMatrix2D trafficMatrix, NetworkLayer ... optionalLayerParameter)
	{
		trafficMatrix = NetPlan.adjustToTolerance(trafficMatrix);
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		removeAllDemands(layer);
		int N = nodes.size ();
		if ((trafficMatrix.rows () != N) || (trafficMatrix.columns() != N)) throw new Net2PlanException ("Bad number of rows-columns in the traffic matrix");
		if (trafficMatrix.size () > 0) if (trafficMatrix.getMinLocation() [0] < 0) throw new Net2PlanException ("Offered traffic must be a non-negative");
		for (int n1 = 0 ; n1 < N ; n1 ++)
			for (int n2 = 0 ; n2 < N ; n2 ++)
			{
				if (n1 == n2) continue;
				addDemand(nodes.get(n1) , nodes.get(n2) , trafficMatrix.getQuick(n1, n2), null , layer);
			}
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Sets the vector of the offered traffic per demand, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided,
	 * the defaulf layer is assumed.</p>
	 * @param offeredTrafficVector Offered traffic vector
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void setVectorDemandOfferedTraffic(DoubleMatrix1D offeredTrafficVector , NetworkLayer ... optionalLayerParameter)
	{
		offeredTrafficVector = NetPlan.adjustToTolerance(offeredTrafficVector);
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		if (offeredTrafficVector.size() != layer.demands.size()) throw new Net2PlanException ("Wrong veector size");
		if (offeredTrafficVector.size () > 0) if (offeredTrafficVector.getMinLocation() [0] < 0) throw new Net2PlanException("Offered traffic must be greater or equal than zero");
		for (Demand d : layer.demands)
		{
			d.offeredTraffic = offeredTrafficVector.get (d.index);
			if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING) layer.updateHopByHopRoutingDemand(d);
		}
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Sets the vector of the link capacities, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param linkCapacities Link capacities
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void setVectorLinkCapacity (DoubleMatrix1D linkCapacities , NetworkLayer ... optionalLayerParameter)
	{
		linkCapacities = NetPlan.adjustToTolerance(linkCapacities);
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		if (linkCapacities.size() != layer.links.size()) throw new Net2PlanException ("Wrong veector size");
		if (linkCapacities.size () > 0) if (linkCapacities.getMinLocation() [0] < 0) throw new Net2PlanException("Link capacities must be greater or equal than zero");
		for (Link e : layer.links) if ((e.coupledLowerLayerDemand != null) || (e.coupledLowerLayerMulticastDemand != null)) throw new Net2PlanException ("Coupled links cannot change its capacity");
		for (Link e : layer.links)
			e.capacity = linkCapacities.get (e.index);
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Sets the vector of the offered traffic per multicast demand, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is
	 * assumed.</p>
	 * @param offeredTrafficVector Offered traffic vector
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void setVectorMulticastDemandOfferedTraffic(DoubleMatrix1D offeredTrafficVector , NetworkLayer ... optionalLayerParameter)
	{
		offeredTrafficVector = NetPlan.adjustToTolerance(offeredTrafficVector);
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		if (offeredTrafficVector.size() != layer.multicastDemands.size()) throw new Net2PlanException ("Wrong veector size");
		if (offeredTrafficVector.size () > 0) if (offeredTrafficVector.getMinLocation() [0] < 0) throw new Net2PlanException("Offered traffic must be greater or equal than zero");
		for (MulticastDemand d : layer.multicastDemands)
			d.offeredTraffic = offeredTrafficVector.get (d.index);
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Sets the vector of the multicast trees carried traffics and occupied link capacities, at the given layer. i-th vector corresponds to i-th index of the element.
	 * If occupied link capacities are {@code null}, the same amount as the carried traffic is assumed. If no layer is provided, the defaulf layer is assumed.</p>
	 * @param carriedTraffic Carried traffic vector
	 * @param occupiedLinkCapacity Occupied link capacity vector
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void setVectorMulticastTreeCarriedTrafficAndOccupiedLinkCapacities (DoubleMatrix1D carriedTraffic , DoubleMatrix1D occupiedLinkCapacity , NetworkLayer ... optionalLayerParameter)
	{
		carriedTraffic = NetPlan.adjustToTolerance(carriedTraffic);
		occupiedLinkCapacity = NetPlan.adjustToTolerance(occupiedLinkCapacity);
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		if (occupiedLinkCapacity == null) occupiedLinkCapacity = carriedTraffic;
		if (carriedTraffic.size() != layer.multicastTrees.size()) throw new Net2PlanException ("Wrong veector size");
		if (carriedTraffic.size() != occupiedLinkCapacity.size()) throw new Net2PlanException ("Wrong veector size");
		if (carriedTraffic.size () > 0) if (carriedTraffic.getMinLocation() [0] < 0) throw new Net2PlanException("Carried traffics must be greater or equal than zero");
		if (occupiedLinkCapacity.size () > 0) if (occupiedLinkCapacity.getMinLocation() [0] < 0) throw new Net2PlanException("Occupied link capacities must be greater or equal than zero");
		for (MulticastTree t : layer.multicastTrees)
			t.setCarriedTraffic(carriedTraffic.get (t.index) , occupiedLinkCapacity.get(t.index));
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Sets the vector of the reserved capacity for protection segments, at the given layer. i-th vector corresponds to i-th index of the element. If no layer is provided, the defaulf layer is
	 * assumed.</p>
	 * @param segmentCapacities Protection segment reserved capacities vector
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void setVectorProtectionSegmentReservedCapacity (DoubleMatrix1D segmentCapacities , NetworkLayer ... optionalLayerParameter)
	{
		segmentCapacities = NetPlan.adjustToTolerance(segmentCapacities);
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		if (segmentCapacities.size() != layer.protectionSegments.size()) throw new Net2PlanException ("Wrong veector size");
		if (segmentCapacities.size () > 0) if (segmentCapacities.getMinLocation() [0] < 0) throw new Net2PlanException("Protection segment reserved capacities must be greater or equal than zero");
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		for (ProtectionSegment s : layer.protectionSegments)
			s.capacity = segmentCapacities.get (s.index);
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}

	/**
	 * <p>Sets the vector of the route carried traffics and occupied link capacities, at the given layer. i-th vector corresponds to i-th index of the element.
	 * If occupied link capacities are {@code null}, the same amount as the carried traffic is assumed. If no layer is provided, the defaulf layer is assumed,</p>
	 * @param carriedTraffic Carried traffic vector
	 * @param occupiedLinkCapacity Occupied link capacity vector
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public void setVectorRouteCarriedTrafficAndOccupiedLinkCapacities (DoubleMatrix1D carriedTraffic , DoubleMatrix1D occupiedLinkCapacity , NetworkLayer ... optionalLayerParameter)
	{
		carriedTraffic = NetPlan.adjustToTolerance(carriedTraffic);
		occupiedLinkCapacity = NetPlan.adjustToTolerance(occupiedLinkCapacity);
		checkIsModifiable();
		NetworkLayer layer = checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		if (occupiedLinkCapacity == null) occupiedLinkCapacity = carriedTraffic;
		if (carriedTraffic.size() != layer.routes.size()) throw new Net2PlanException ("Wrong veector size");
		if (carriedTraffic.size() != occupiedLinkCapacity.size()) throw new Net2PlanException ("Wrong veector size");
		if (carriedTraffic.size () > 0) if (carriedTraffic.getMinLocation() [0] < 0) throw new Net2PlanException("Carried traffics must be greater or equal than zero");
		if (occupiedLinkCapacity.size () > 0) if (occupiedLinkCapacity.getMinLocation() [0] < 0) throw new Net2PlanException("Occupied link capacities must be greater or equal than zero");
		layer.checkRoutingType(RoutingType.SOURCE_ROUTING);
		for (Route r : layer.routes)
			r.setCarriedTraffic(carriedTraffic.get (r.index) , occupiedLinkCapacity.get(r.index));
		if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
	}
	
	/**
	 * Returns a {@code String} representation of the network design.
	 *
	 * @return String representation of the network design
	 * @since 0.2.0
	 */
	@Override
	public String toString()
	{
		return toString(layers);
	}

	/**
	 * Returns a {@code String} representation of the network design only for the 
	 * given layers.
	 * 
	 * @param layers Network layers
	 * @return String representation of the network design
	 */
	public String toString(Collection<NetworkLayer> layers)
	{
		StringBuilder netPlanInformation = new StringBuilder();
		String NEWLINE = StringUtils.getLineSeparator();
		
		int N = nodes.size ();
		if (N == 0)
		{
			netPlanInformation.append("Empty network");
//			return netPlanInformation.toString();
		}
		
		int L = getNumberOfLayers();
		int numSRGs = getNumberOfSRGs();
		netPlanInformation.append("Network information");
		netPlanInformation.append(NEWLINE);
		netPlanInformation.append("--------------------------------");
		netPlanInformation.append(NEWLINE).append(NEWLINE);
		netPlanInformation.append("Name: ").append(this.networkName).append(NEWLINE);
		netPlanInformation.append("Description: ").append(networkDescription).append(NEWLINE);
		Map<String, String> networkAttributes_thisNetPlan = this.attributes;
		netPlanInformation.append("Attributes: ");
		netPlanInformation.append(networkAttributes_thisNetPlan.isEmpty() ? "none" : StringUtils.mapToString(networkAttributes_thisNetPlan, "=", ", "));
		netPlanInformation.append(NEWLINE);
		netPlanInformation.append("Number of layers: ").append(L).append(NEWLINE);
		netPlanInformation.append("Default layer: ").append(defaultLayer.id).append(NEWLINE);
		netPlanInformation.append("Number of nodes: ").append(N).append(NEWLINE);
		netPlanInformation.append("Number of SRGs: ").append(numSRGs).append(NEWLINE);
		
		netPlanInformation.append(NEWLINE);
		netPlanInformation.append("Node information");
		netPlanInformation.append(NEWLINE);
		netPlanInformation.append("--------------------------------");
		netPlanInformation.append(NEWLINE).append(NEWLINE);
		
		for (Node node : nodes)
		{
			String nodeInformation = String.format("n%d (id %d), state: %s, position: (%.3g, %.3g), name: %s, attributes: %s", node.index , node.id , !node.isUp ? "down" : "up", node.nodeXYPositionMap.getX(), node.nodeXYPositionMap.getY(), node.name , node.attributes.isEmpty() ? "none" : node.attributes);
			netPlanInformation.append(nodeInformation);
			netPlanInformation.append(NEWLINE);
		}

		for (NetworkLayer layer : layers)
		{
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("Layer ").append(layer.index + " (id: " + layer.id + ")").append(" information");
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("--------------------------------");
			netPlanInformation.append(NEWLINE).append(NEWLINE);
			netPlanInformation.append("Name: ").append(layer.name).append(NEWLINE);
			netPlanInformation.append("Description: ").append(layer.description).append(NEWLINE);
			netPlanInformation.append("Link capacity units: ").append(layer.linkCapacityUnitsName).append(NEWLINE);
			netPlanInformation.append("Demand traffic units: ").append(layer.demandTrafficUnitsName).append(NEWLINE);
			netPlanInformation.append("Routing type: ").append(layer.routingType).append(NEWLINE);

			if (!layer.attributes.isEmpty())
			{
				netPlanInformation.append("Attributes: ");
				netPlanInformation.append(StringUtils.mapToString(layer.attributes , "=", ", "));
				netPlanInformation.append(NEWLINE);
			}

			netPlanInformation.append("Number of links: ").append(layer.links.size ()).append(NEWLINE);
			netPlanInformation.append("Number of demands: ").append(layer.demands.size ()).append(NEWLINE);
			netPlanInformation.append("Number of multicast demands: ").append(layer.multicastDemands.size ()).append(NEWLINE);
			netPlanInformation.append("Number of multicast trees: ").append(layer.multicastTrees.size ()).append(NEWLINE);
			if (layer.routingType == RoutingType.SOURCE_ROUTING)
			{
				netPlanInformation.append("Number of routes: ").append(layer.routes.size ()).append(NEWLINE);
				netPlanInformation.append("Number of protection segments: ").append(layer.protectionSegments.size ()).append(NEWLINE);
			}

			if (!layer.links.isEmpty())
			{
				netPlanInformation.append(NEWLINE);
				netPlanInformation.append("Link information");
				netPlanInformation.append(NEWLINE);
				netPlanInformation.append("--------------------------------");
				netPlanInformation.append(NEWLINE).append(NEWLINE);

				for (Link link : layer.links)
				{
					String linkInformation = String.format("e%d (id %d), n%d (%s) -> n%d (%s), state: %s, capacity: %.3g, length: %.3g km, propagation speed: %.3g km/s, carried traffic (incl. segments): %.3g , occupied capacity (incl. traffic in segments): %.3g, attributes: %s", link.index,  link.id , link.originNode.id, link.originNode.name, link.destinationNode.id, link.destinationNode.name , !link.isUp? "down" : "up", link.capacity , link.lengthInKm , link.propagationSpeedInKmPerSecond, link.carriedTrafficSummingRoutesAndCarriedTrafficByProtectionSegments , link.occupiedCapacitySummingRoutesAndCarriedTrafficByProtectionSegments , link.attributes.isEmpty() ? "none" : link.attributes);
					netPlanInformation.append(linkInformation);
					
					if (link.coupledLowerLayerDemand != null)
						netPlanInformation.append(String.format(", associated to demand %d (id %d) at layer %d (id %d)", link.coupledLowerLayerDemand.index , link.coupledLowerLayerDemand.id , link.coupledLowerLayerDemand.layer.index , link.coupledLowerLayerDemand.layer.id));
					if (link.coupledLowerLayerMulticastDemand != null)
						netPlanInformation.append(String.format(", associated to multicast demand %d (id %d) at layer %d (id %d)", link.coupledLowerLayerMulticastDemand.index , link.coupledLowerLayerMulticastDemand.id , link.coupledLowerLayerMulticastDemand.layer.index , link.coupledLowerLayerMulticastDemand.layer.id));
					
					netPlanInformation.append(NEWLINE);
				}
			}

			if (!layer.demands.isEmpty())
			{
				netPlanInformation.append(NEWLINE);
				netPlanInformation.append("Demand information");
				netPlanInformation.append(NEWLINE);
				netPlanInformation.append("--------------------------------");
				netPlanInformation.append(NEWLINE).append(NEWLINE);

				for (Demand demand : layer.demands)
				{
					String demandInformation = String.format("d%d (id %d), n%d (%s) -> n%d (%s), offered traffic: %.3g, carried: %.3g, attributes: %s", demand.index , demand.id,  demand.ingressNode.id, demand.ingressNode.name , demand.egressNode.id, demand.egressNode.name , demand.offeredTraffic , demand.carriedTraffic , demand.attributes.isEmpty() ? "none" : demand.attributes);
					netPlanInformation.append(demandInformation);

					if (demand.coupledUpperLayerLink != null)
						netPlanInformation.append(String.format(", associated to link %d (id %d) in layer %d (id %d)", demand.coupledUpperLayerLink.index, demand.coupledUpperLayerLink.id , demand.coupledUpperLayerLink.layer.index , demand.coupledUpperLayerLink.layer.id));
					
					netPlanInformation.append(NEWLINE);
				}
			}

			if (!layer.multicastDemands.isEmpty())
			{
				netPlanInformation.append(NEWLINE);
				netPlanInformation.append("Multicast demand information");
				netPlanInformation.append(NEWLINE);
				netPlanInformation.append("--------------------------------");
				netPlanInformation.append(NEWLINE).append(NEWLINE);

				for (MulticastDemand demand : layer.multicastDemands)
				{
					String demandInformation = String.format("d%d (id %d, n%d (%s) -> " , demand.index , demand.id , demand.ingressNode.id, demand.ingressNode.name);
					for (Node n : demand.egressNodes) demandInformation += " " + n + " (" + n.name  + ")";
					demandInformation += String.format (", offered traffic: %.3g, carried:  %.3g, attributes: %s", demand.offeredTraffic, demand.carriedTraffic , demand.attributes.isEmpty() ? "none" : demand.attributes);
					netPlanInformation.append(demandInformation);

					if (demand.coupledUpperLayerLinks != null)
					{
						netPlanInformation.append(", associated to links ");
						for (Link e : demand.coupledUpperLayerLinks.values ()) netPlanInformation.append (" " + e.index + " (id " + e.id + "), ");
						netPlanInformation.append(" of layer: " + demand.coupledUpperLayerLinks.values ().iterator().next().index + " (id " + demand.coupledUpperLayerLinks.values ().iterator().next().id + ")");
					}
					
					netPlanInformation.append(NEWLINE);
				}
			}

			if (layer.routingType == RoutingType.SOURCE_ROUTING)
			{
				if (!layer.routes.isEmpty())
				{
					netPlanInformation.append(NEWLINE);
					netPlanInformation.append("Routing information");
					netPlanInformation.append(NEWLINE);
					netPlanInformation.append("--------------------------------");
					netPlanInformation.append(NEWLINE).append(NEWLINE);

					for (Route route : layer.routes)
					{
						String str_seqNodes = CollectionUtils.join(route.seqNodesRealPath , " => ");
						String str_seqLinks = CollectionUtils.join(route.seqLinksRealPath , " => ");
						String backupSegments = route.potentialBackupSegments.isEmpty() ? "none" : CollectionUtils.join(route.potentialBackupSegments, ", ");

						String routeInformation = String.format("r%d (id %d), demand: d%d (id %d), carried traffic: %.3g, occupied capacity: %.3g, seq. links: %s, seq. nodes: %s, backup segments: %s, attributes: %s", route.index , route.id , route.demand.index , route.demand.id, route.carriedTraffic, route.occupiedLinkCapacity, str_seqLinks, str_seqNodes, backupSegments, route.attributes.isEmpty() ? "none" : route.attributes);
						netPlanInformation.append(routeInformation);
						netPlanInformation.append(NEWLINE);
					}
				}
				
				if (!layer.protectionSegments.isEmpty())
				{
					netPlanInformation.append(NEWLINE);
					netPlanInformation.append("Protection segment information");
					netPlanInformation.append(NEWLINE);
					netPlanInformation.append("--------------------------------");
					netPlanInformation.append(NEWLINE).append(NEWLINE);

					for (ProtectionSegment segment : layer.protectionSegments)
					{
						String str_seqLinks = CollectionUtils.join(segment.seqLinks, " => ");
						String str_seqNodes = CollectionUtils.join(segment.seqNodes, " => ");

						String segmentInformation = String.format("s%d (id %d), reserved bandwidth: %.3g, seq. links: %s, seq. nodes: %s, attributes: %s", segment.index , segment.id , segment.capacity , str_seqLinks, str_seqNodes, segment.attributes.isEmpty() ? "none" : segment.attributes);
						netPlanInformation.append(segmentInformation);
						netPlanInformation.append(NEWLINE);
					}
				}

				if (!layer.multicastTrees.isEmpty())
				{
					netPlanInformation.append(NEWLINE);
					netPlanInformation.append("Multicast trees information");
					netPlanInformation.append(NEWLINE);
					netPlanInformation.append("--------------------------------");
					netPlanInformation.append(NEWLINE).append(NEWLINE);

					for (MulticastTree tree : layer.multicastTrees)
					{
						String str_seqNodes = CollectionUtils.join(tree.cache_traversedNodes , " , ");
						String str_seqLinks = CollectionUtils.join(tree.linkSet , " , ");

						String treeInformation = String.format("mt%d (id %d), multicast demand: md%d (id %d), carried traffic: %.3g, occupied capacity: %.3g, links: %s, nodes: %s, attributes: %s", tree.index , tree.id , tree.demand.index , tree.demand.id , tree.carriedTraffic, tree.occupiedLinkCapacity, str_seqLinks, str_seqNodes, tree.attributes.isEmpty() ? "none" : tree.attributes);
						netPlanInformation.append(treeInformation);
						netPlanInformation.append(NEWLINE);
					}
				}

			}
			else
			{
				netPlanInformation.append(NEWLINE);
				netPlanInformation.append("Forwarding information");
				netPlanInformation.append(NEWLINE);
				netPlanInformation.append("--------------------------------");
				netPlanInformation.append(NEWLINE).append(NEWLINE);
				netPlanInformation.append("f_de: ").append(NEWLINE);
				netPlanInformation.append(DoubleFactory2D.dense.make (layer.forwardingRules_f_de.toArray())).append(NEWLINE);
				netPlanInformation.append("x_de: ").append(NEWLINE);
				netPlanInformation.append(layer.forwardingRules_x_de).append(NEWLINE);
			}
		}
		
		if (!srgs.isEmpty())
		{
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("Shared-risk group information");
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("--------------------------------");
			netPlanInformation.append(NEWLINE).append(NEWLINE);

			for (SharedRiskGroup srg : srgs)
			{
				String aux_nodes = srg.nodes.isEmpty() ? "none" : CollectionUtils.join(srg.nodes, " ");
				String aux_links = srg.links.isEmpty() ? "none" : CollectionUtils.join(srg.links, " ");
				
				String aux_mttf = StringUtils.secondsToYearsDaysHoursMinutesSeconds(srg.meanTimeToFailInHours * 3600);
				String aux_mttr = StringUtils.secondsToYearsDaysHoursMinutesSeconds(srg.meanTimeToRepairInHours * 3600);

				String srgInformation = String.format("srg%d (id %d), nodes: %s, links: %s, mttf: %s, mttr: %s, attributes: %s", srg.index , srg.id , aux_nodes, aux_links.toString(), aux_mttf, aux_mttr, srg.attributes.isEmpty() ? "none" : srg.attributes);
				netPlanInformation.append(srgInformation);
				netPlanInformation.append(NEWLINE);
			}
		}

		return netPlanInformation.toString();
	}
//	/**
//	 * Returns an unmodifiable view of the network.
//	 *
//	 * @return An unmodifiable view of the network
//	 * @since 0.2.3
//	 */
//	public NetPlan unmodifiableView()
//	{
//		NetPlan netPlan = new NetPlan();
//		netPlan.assignFrom(this);
//		netPlan.isModifiable = false;
//		return netPlan;
//	}

	/**
	 * <p>Sets the {@code NetPlan} so it cannot be modified</p>
	 * @param isModifiable }If {@code true}, the object can be modified, {@code false} otherwise
	 * @return The previous modifiable state
	 */
	public boolean setModifiableState (boolean isModifiable)
	{
		final boolean oldState = this.isModifiable;
		this.isModifiable = isModifiable;
		return oldState;
	}

	/**
	 * <p>Checks if the given layer is valid and belongs to this {@code NetPlan} design. Throws and exception if the input is invalid.</p>
	 * @param optionalLayer Network layer (optional)
	 * @return The given layer (or the defaukt layer if no input was given)
	 */
	public NetworkLayer checkInThisNetPlanOptionalLayerParameter (NetworkLayer ... optionalLayer)
	{
		if (optionalLayer.length >= 2) throw new Net2PlanException ("None or one layer parameter can be supplied");
		if (optionalLayer.length == 1) { checkInThisNetPlan(optionalLayer [0]); return optionalLayer [0]; }
		return defaultLayer;
	}
	NetworkElement getPeerElementInThisNetPlan (NetworkElement e)
	{
		NetworkElement res;
		if (e instanceof NetPlan) throw new RuntimeException ("Bad");
		if (e.netPlan == null) throw new RuntimeException ("Bad");
		if (e.netPlan == this) throw new RuntimeException ("Bad");
		if (e instanceof Node) res = this.cache_id2NodeMap.get (e.id);
		else if (e instanceof Demand) res = this.cache_id2DemandMap.get (e.id);
		else if (e instanceof ProtectionSegment) res = this.cache_id2ProtectionSegmentMap.get (e.id);
		else if (e instanceof Link) res = this.cache_id2LinkMap.get (e.id);
		else if (e instanceof MulticastDemand) res = this.cache_id2MulticastDemandMap.get (e.id);
		else if (e instanceof MulticastTree) res = this.cache_id2MulticastTreeMap.get (e.id);
		else if (e instanceof Node) res = this.cache_id2NodeMap.get (e.id);
		else if (e instanceof Route) res = this.cache_id2RouteMap.get (e.id);
		else if (e instanceof SharedRiskGroup) res = this.cache_id2srgMap.get (e.id);
		else  throw new RuntimeException ("Bad");
		if (res.index != e.index) throw new RuntimeException ("Bad");
		return res;
	}
	/* Receives a collection of routes, segments or multicast trees, and updates its failure state according to the traversing links and nodes */
	void updateFailureStateRoutesTreesSegments (Collection<? extends NetworkElement> set)
	{
		for (NetworkElement thisElement : set)
			updateFailureStateRoutesTreesSegments (thisElement);
	}
	/* Receives a route, segment or multicast tree, and updates its failure state according to the traversing links and nodes */
	void updateFailureStateRoutesTreesSegments (NetworkElement thisElement)
	{
		if (thisElement instanceof Route)
		{
			Route route = (Route) thisElement;
			boolean previouslyUp = !route.layer.cache_routesDown.contains (route);
			boolean isUp = true;
			for (Link e : route.seqLinksRealPath) if (!e.isUp) { isUp = false; break; }
			if (isUp) for (Node e : route.seqNodesRealPath) if (!e.isUp) { isUp = false; break; }
//			System.out.println ("Route :" + route + ", previously up:" + previouslyUp + ", isUp: " + isUp);
//			for (Link e : route.seqLinksRealPath)
//				System.out.println ("Link " + e + ", isUp: " + e.isUp);
//			for (Node n : route.seqNodesRealPath)
//				System.out.println ("Node " + n + ", isUp: " + n.isUp);
			if (isUp == previouslyUp) return;
			if (isUp) // from down to up
			{
				if (route.occupiedLinkCapacity != 0) throw new RuntimeException ("Bad");
				if (route.carriedTraffic != 0) throw new RuntimeException ("Bad");
				route.layer.cache_routesDown.remove (route); 
//				System.out.println ("down to up: route.layer.cache_routesDown: " + route.layer.cache_routesDown);
			}
			else 
			{
				if (route.occupiedLinkCapacity != route.occupiedLinkCapacityIfNotFailing) throw new RuntimeException ("Bad");
				if (route.carriedTraffic != route.carriedTrafficIfNotFailing) throw new RuntimeException ("Bad");
				route.layer.cache_routesDown.add (route); 
//				System.out.println ("up to down : route.layer.cache_routesDown: " + route.layer.cache_routesDown);
			}
			final boolean previousDebug = ErrorHandling.isDebugEnabled(); ErrorHandling.setDebug(false);
 			route.setCarriedTraffic(route.carriedTrafficIfNotFailing , route.occupiedLinkCapacityIfNotFailing);
			ErrorHandling.setDebug(previousDebug);
		} 
		else if (thisElement instanceof MulticastTree)
		{
			MulticastTree tree = (MulticastTree) thisElement;
			boolean previouslyUp = !tree.layer.cache_multicastTreesDown.contains (tree);
			boolean isUp = true;
			for (Link e : tree.linkSet) if (!e.isUp) { isUp = false; break; }
			for (Node e : tree.cache_traversedNodes) if (!e.isUp) { isUp = false; break; }
			if (isUp == previouslyUp) return;
			if (isUp) // from down to up
			{
				if (tree.occupiedLinkCapacity != 0) throw new RuntimeException ("Bad");
				if (tree.carriedTraffic != 0) throw new RuntimeException ("Bad");
				tree.layer.cache_multicastTreesDown.remove (tree); 
			}
			else 
			{
				if (tree.occupiedLinkCapacity != tree.occupiedLinkCapacityIfNotFailing) throw new RuntimeException ("Bad");
				if (tree.carriedTraffic != tree.carriedTrafficIfNotFailing) throw new RuntimeException ("Bad");
				tree.layer.cache_multicastTreesDown.add (tree); 
			}
			final boolean previousDebug = ErrorHandling.isDebugEnabled(); ErrorHandling.setDebug(false);
			tree.setCarriedTraffic(tree.carriedTrafficIfNotFailing , tree.occupiedLinkCapacityIfNotFailing); // updates links since all are up
			ErrorHandling.setDebug(previousDebug);
		}
		else if (thisElement instanceof ProtectionSegment)
		{
			ProtectionSegment segment = (ProtectionSegment) thisElement;
			boolean previouslyUp = !segment.layer.cache_segmentsDown.contains (segment);
			boolean isUp = true;
			for (Link e : segment.seqLinks) if (!e.isUp) { isUp = false; break; }
			for (Node e : segment.seqNodes) if (!e.isUp) { isUp = false; break; }
			if (isUp == previouslyUp) return;
			if (isUp) // from down to up
			{
				segment.layer.cache_segmentsDown.remove (segment); 
			}
			else 
			{
				segment.layer.cache_segmentsDown.add (segment); 
			}
		}
		else throw new RuntimeException ("Bad");
	}

//
//	private Map<String,String> copyAttributeMap (Map<String,String> map)
//	{
//		Map<String,String> m = new HashMap<String,String> ();
//		for (Entry<String,String> e : map.entrySet ()) m.put(e.getKey(),e.getValue());
//		return m;
//	}

	void checkCachesConsistency (List<? extends NetworkElement> list , Map<Long,? extends NetworkElement> cache , boolean mustBeSameSize)
	{
		if (mustBeSameSize) if (cache.size () != list.size ()) throw new RuntimeException ("Bad: cache: " + cache+  ", list: " + list);
		for (int cont = 0 ; cont < list.size () ; cont ++) 
		{
			NetworkElement e = list.get(cont);
			if (e.index != cont)  throw new RuntimeException ("Bad");
			if (cache.get(e.id) == null) throw new RuntimeException ("Bad");
			if (!e.equals (cache.get(e.id))) throw new RuntimeException ("Bad: list: " + list + ", cache: " + cache + ", e: " + e + ", cache.get(e.id): " + cache.get(e.id));
			if (e != cache.get(e.id)) throw new RuntimeException ("Bad");
		}
	}

	/**
	 * <p>For debug purposes: Checks the consistency of the internal cache (nodes, srgs, layers, links, demands, multicast demands, multicast trees, routes, protection segments). If any
	 * inconsistency is found an exception is thrown.</p>
	 */
	public void checkCachesConsistency ()
	{
//		System.out.println ("Check caches consistency of object: " + hashCode());
		checkCachesConsistency (nodes , cache_id2NodeMap , true);
		checkCachesConsistency (srgs , cache_id2srgMap , true);
		checkCachesConsistency (layers , cache_id2LayerMap , true);
		for (NetworkLayer layer : layers)
		{
			checkCachesConsistency (layer.links , cache_id2LinkMap , false);
			checkCachesConsistency (layer.demands , cache_id2DemandMap , false);
			checkCachesConsistency (layer.multicastDemands , cache_id2MulticastDemandMap , false);
			checkCachesConsistency (layer.multicastTrees , cache_id2MulticastTreeMap , false);
			checkCachesConsistency (layer.protectionSegments , cache_id2ProtectionSegmentMap, false);
			checkCachesConsistency (layer.routes , cache_id2RouteMap , false);
		}

		
		this.checkInThisNetPlan (nodes);
		this.checkInThisNetPlan (srgs);
		this.checkInThisNetPlan (layers);
		this.checkInThisNetPlan (defaultLayer);

		for (Node node : nodes) node.checkCachesConsistency(); 
		for (SharedRiskGroup srg : srgs) srg.checkCachesConsistency();
		for (NetworkLayer layer : layers)
		{
			this.checkInThisNetPlanAndLayer (layer.links , layer);
			this.checkInThisNetPlanAndLayer (layer.demands , layer);
			this.checkInThisNetPlanAndLayer (layer.multicastDemands , layer);
			this.checkInThisNetPlanAndLayer (layer.multicastTrees , layer);
			this.checkInThisNetPlanAndLayer (layer.routes , layer);
			this.checkInThisNetPlanAndLayer (layer.protectionSegments , layer);
			layer.checkCachesConsistency();
			for (Link link : layer.links) link.checkCachesConsistency();
			for (Demand demand : layer.demands) demand.checkCachesConsistency();
			for (MulticastDemand demand : layer.multicastDemands) demand.checkCachesConsistency();
			for (Route route : layer.routes) route.checkCachesConsistency();
			for (MulticastTree tree : layer.multicastTrees) tree.checkCachesConsistency();
			for (ProtectionSegment segment : layer.protectionSegments) segment.checkCachesConsistency();
			
			if (layer.routingType == RoutingType.HOP_BY_HOP_ROUTING)
			{
				for (int d = 0 ; d < layer.demands.size() ; d ++)
				{
					final Demand demand = layer.demands.get (d);
					for (int e = 0 ; e < layer.links.size () ; e ++)
					{
						final Link link = layer.links.get(e);
						final double f_de = layer.forwardingRules_f_de.get(d, e);
						final double x_de = layer.forwardingRules_x_de.get(d, e);
						if (f_de < 0) throw new RuntimeException ("Bad");
						if (f_de > 1) throw new RuntimeException ("Bad");
						final Node a_e = layer.links.get(e).originNode;
						double linkInitialNodeOutTraffic = 0; double linkInitialNodeOutRules = 0;
						for (Link outLink : a_e.getOutgoingLinks(layer))
						{
							final int out_a_e = outLink.index;
							linkInitialNodeOutTraffic += layer.forwardingRules_x_de.get(d, out_a_e);
							linkInitialNodeOutRules += layer.forwardingRules_f_de.get(d, out_a_e);
						}
						final boolean linkUp = link.isUp && link.originNode.isUp && link.destinationNode.isUp;
						double linkInitialNodeInTraffic = (link.originNode == demand.ingressNode)? demand.offeredTraffic : 0; 
						for (Link inLink : a_e.getIncomingLinks(layer)) linkInitialNodeInTraffic += layer.forwardingRules_x_de.get(d, inLink.index);
						if (linkInitialNodeOutRules > 1 + 1E-3) throw new RuntimeException ("Bad");
						if (!linkUp && (x_de > 1E-3))throw new RuntimeException ("Bad. outTraffic: " + linkInitialNodeOutTraffic + " and link " + link + " is down. NetPlan: " + this);
						if (linkUp && (linkInitialNodeInTraffic > 1e-3)) if (Math.abs(f_de - x_de/linkInitialNodeInTraffic) > 1e-4) throw new RuntimeException ("Bad. demand index: " + d + ", link : " + link + " (isUp? )" + link.isUp + ", nodeInTraffic: " + linkInitialNodeInTraffic + ", x_de: " + x_de + ", f_de: " + f_de + ", f_de - x_de/nodeInTraffic: " + (f_de - x_de/linkInitialNodeInTraffic));
						if (linkInitialNodeOutTraffic < 1e-3) if (x_de > 1e-3) throw new RuntimeException ("Bad");
					}
				}
			}
		}		
		
		/* Check the interlayer object */
		Set<NetworkLayer> layersAccordingToInterLayer = interLayerCoupling.vertexSet();
		if (!layersAccordingToInterLayer.containsAll(layers)) throw new RuntimeException ("Bad");
		if (layersAccordingToInterLayer.size () != layers.size()) throw new RuntimeException ("Bad");
		for (DemandLinkMapping mapping : interLayerCoupling.edgeSet())
		{
			NetworkLayer linkLayer = mapping.getLinkSideLayer();
			NetworkLayer demandLayer = mapping.getDemandSideLayer();
			linkLayer.checkAttachedToNetPlanObject(this);
			demandLayer.checkAttachedToNetPlanObject(this);
			for (Entry<Demand,Link> e :  mapping.getDemandMap().entrySet())
			{
				e.getKey().checkAttachedToNetPlanObject(this);
				e.getValue().checkAttachedToNetPlanObject(this);
			}
		}
		
		if (layers.get (defaultLayer.index) != defaultLayer) throw new RuntimeException ("Bad"); 
	}
	
	static double adjustToTolerance (double val) { final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor")); return (Math.abs(val) < PRECISION_FACTOR)? 0 : val; }
	static DoubleMatrix1D adjustToTolerance (DoubleMatrix1D vals) 
	{
		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor")); 
		return vals.copy ().assign (new DoubleFunction () { public double apply (double x) { return Math.abs(x) < PRECISION_FACTOR ? 0 : x; } }); 
	}
	static DoubleMatrix2D adjustToTolerance (DoubleMatrix2D vals) 
	{
		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor")); 
		return vals.copy ().assign (new DoubleFunction () { public double apply (double x) { return Math.abs(x) < PRECISION_FACTOR ? 0 : x; } }); 
	}


}



