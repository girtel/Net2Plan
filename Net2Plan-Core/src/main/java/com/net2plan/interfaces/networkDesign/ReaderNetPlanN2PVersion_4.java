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

import com.net2plan.utils.Constants.RoutingType;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.LongUtils;
import org.codehaus.stax2.XMLStreamReader2;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.*;

class ReaderNetPlanN2PVersion_4 implements IReaderNetPlan //extends NetPlanFormat_v3
{
	final static String KEY_STRING_BIDIRECTIONALCOUPLE = "bidirectionalCouple";
	protected Map<Long,Node> mapOldId2Node;
	protected Map<Long,SharedRiskGroup> mapOldId2Srg;
	protected Map<Long,NetworkLayer> mapOldId2Layer;
	protected Map<Long,Link> mapOldId2Link;
	protected Map<Long,Demand> mapOldId2Demand;
	protected Map<Long,MulticastDemand> mapOldId2MulticastDemand;
	protected Map<Long,Route> mapOldId2Route;
	protected Map<Long,MulticastTree> mapOldId2MulticastTree;
	//protected Map<Long,ProtectionSegment> mapOldId2ProtectionSegment;
	protected boolean hasAlreadyReadOneLayer;
	
	public void create(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		this.mapOldId2Node = new HashMap<Long,Node> ();
		this.mapOldId2Srg = new HashMap<Long,SharedRiskGroup> ();
		this.mapOldId2Layer = new HashMap<Long,NetworkLayer> ();
		this.mapOldId2Link = new HashMap<Long,Link> ();
		this.mapOldId2Demand = new HashMap<Long,Demand> ();
		this.mapOldId2MulticastDemand = new HashMap<Long,MulticastDemand> ();
		this.mapOldId2Route = new HashMap<Long,Route> ();
		this.mapOldId2MulticastTree = new HashMap<Long,MulticastTree> ();
		//this.mapOldId2ProtectionSegment = new HashMap<Long,ProtectionSegment> ();
		this.hasAlreadyReadOneLayer = false;
		parseNetwork(netPlan, xmlStreamReader);

//		System.out.println ("End ReaderNetPlan_v4: " + netPlan + " ----------- ");
		
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	protected void parseNetwork(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		String networkDescription_thisNetPlan = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "description"));
		String networkName_thisNetPlan = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "name"));
		netPlan.setNetworkDescription(networkDescription_thisNetPlan);
		netPlan.setNetworkName(networkName_thisNetPlan);

		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();
			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "attribute":
							String key = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "key"));
							String name = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value"));
							netPlan.setAttribute(key, name);
							break;

						case "layer":
							parseLayer(netPlan, xmlStreamReader);
							break;

						case "node":
							parseNode(netPlan, xmlStreamReader);
							break;

						case "srg":
							parseSRG(netPlan, xmlStreamReader);
							break;

						case "layerCouplingDemand":
							long upperLayerLinkId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "upperLayerLinkId"));
							long lowerLayerDemandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "lowerLayerDemandId"));
							mapOldId2Demand.get(lowerLayerDemandId).coupleToUpperLayerLink(mapOldId2Link.get(upperLayerLinkId));
							break;

						case "layerCouplingMulticastDemand":
							List<Long> upperLayerLinkIds = LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "upperLayerLinkIds")));
							long lowerLayerMulticastDemandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "lowerLayerDemandId"));
							Set<Link> setLinksToCouple = new HashSet<Link> (); for (long oldLinkId : upperLayerLinkIds) setLinksToCouple.add (mapOldId2Link.get(oldLinkId));
							mapOldId2MulticastDemand.get(lowerLayerMulticastDemandId).couple(setLinksToCouple);
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("network"))
					{
//						if (!netPlan.hasReadLayerZero && netPlan.layers.size() > 1) netPlan.removeNetworkLayer (netPlan.layers.get(0));
						return;
					}

					break;
			}
		}

		throw new RuntimeException("'Network' element not parsed correctly (end tag not found)");
	}
	
	protected void parseProtectionSegment(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long segmentId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		double reservedCapacity = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "reservedCapacity"));
		List<Long> seqLinks = LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "seqLinks")));
//		netPlan.nextSegmentId.put(layerId, segmentId);
		List<Link> newSeqLinks = new LinkedList<Link> (); for (long linkId : seqLinks) newSeqLinks.add (mapOldId2Link.get(linkId));
//		ProtectionSegment newSegment = netPlan.addProtectionSegment(newSeqLinks, reservedCapacity, null);
//		mapOldId2ProtectionSegment.put (segmentId , newSegment);
		
		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "attribute":
							String key = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "key"));
							String name = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value"));
//							newSegment.setAttribute(key, name);
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("protectionSegment")) return;
					break;
			}
		}

		throw new RuntimeException("'Protection segment' element not parsed correctly (end tag not found)");
	}

	protected void parseNode(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long nodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		double xCoord = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "xCoord"));
		double yCoord = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "yCoord"));
		String nodeName = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "name"));
		boolean isUp = true; try { isUp = xmlStreamReader.getAttributeAsBoolean(xmlStreamReader.getAttributeIndex(null, "isUp")); } catch (Exception e) {} 
		//netPlan.nextNodeId = new MutableLong(nodeId);
		Node newNode = netPlan.addNode(xCoord, yCoord, nodeName, null);
		newNode.setFailureState(isUp);
		mapOldId2Node.put (nodeId , newNode);

		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();
			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "attribute":
							String key = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "key"));
							String name = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value"));
							newNode.setAttribute(key, name);
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("node")) return;
					break;
			}
		}

		throw new RuntimeException("'Node' element not parsed correctly (end tag not found)");
	}

	protected void parseDemand(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long demandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		long ingressNodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "ingressNodeId"));
		long egressNodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "egressNodeId"));
		double offeredTraffic = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "offeredTraffic"));
//		netPlan.nextDemandId.put(layerId, demandId);
		Demand newDemand = netPlan.addDemand(mapOldId2Node.get(ingressNodeId), mapOldId2Node.get(egressNodeId), offeredTraffic, null , mapOldId2Layer.get(layerId));
		mapOldId2Demand.put (demandId , newDemand);

		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "attribute":
							String key = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "key"));
							String name = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value"));
							if (key.equals(KEY_STRING_BIDIRECTIONALCOUPLE)) name = "" + mapOldId2Demand.get(Long.parseLong(name)).getId();
							newDemand.setAttribute(key, name);
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("demand")) return;
					break;
			}
		}

		throw new RuntimeException("'Demand' element not parsed correctly (end tag not found)");
	}

	protected void parseRoute(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long routeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		long demandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "demandId"));
		double carriedTraffic = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "carriedTraffic"));
		double occupiedCapacity = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "occupiedCapacity"));
		double carriedTrafficIfNotFailing = carriedTraffic; try { carriedTrafficIfNotFailing = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "carriedTrafficIfNotFailing")); } catch (Exception e) {} 
		double occupiedLinkCapacityIfNotFailing = occupiedCapacity; try { occupiedLinkCapacityIfNotFailing = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "occupiedLinkCapacityIfNotFailing")); } catch (Exception e) {} 
		if (occupiedLinkCapacityIfNotFailing < 0) occupiedLinkCapacityIfNotFailing = carriedTrafficIfNotFailing;
		List<Long> initialSeqLinksWhenCreated = LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "seqLinks")));
		List<Long> currentSeqLinksAndProtectionSegments = new LinkedList<Long> (initialSeqLinksWhenCreated);
		try { currentSeqLinksAndProtectionSegments = LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "currentSeqLinksAndProtectionSegments"))); } catch (Exception e) { }
		List<Long> backupSegmentList = LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "backupSegmentList")));
//		netPlan.nextRouteId.put(layerId, routeId);
		List<Link> initialSeqLinksWhenCreated_link = new LinkedList<Link> (); for (long linkId : initialSeqLinksWhenCreated) initialSeqLinksWhenCreated_link.add (mapOldId2Link.get(linkId));
		List<Link> seqLinksAndProtectionSegments_link = new LinkedList<Link> (); 
		for (long linkId : currentSeqLinksAndProtectionSegments) 
			if (mapOldId2Link.get(linkId) != null) seqLinksAndProtectionSegments_link.add (mapOldId2Link.get(linkId));
			//else if (mapOldId2ProtectionSegment.get(linkId) != null) seqLinksAndProtectionSegments_link.add (mapOldId2ProtectionSegment.get(linkId));
			else throw new Net2PlanException ("Error parsing route information (current sequence of links and protection segments)");
		Route newRoute = netPlan.addRoute(mapOldId2Demand.get(demandId) , carriedTrafficIfNotFailing, occupiedLinkCapacityIfNotFailing, initialSeqLinksWhenCreated_link, null);
		mapOldId2Route.put (routeId,newRoute);
		//for(long segmentId : backupSegmentList) newRoute.addProtectionSegment(mapOldId2ProtectionSegment.get(segmentId));
		newRoute.setSeqLinks(seqLinksAndProtectionSegments_link);
		if (Math.abs (newRoute.getCarriedTraffic() - carriedTraffic) > 1e-3) throw new RuntimeException ("Bad");
		if (Math.abs (newRoute.getOccupiedCapacity() - occupiedCapacity) > 1e-3) throw new RuntimeException ("Bad");
		
		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "attribute":
							String key = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "key"));
							String name = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value"));
							newRoute.setAttribute(key, name);
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("route")) return;
					break;
			}
		}

		throw new RuntimeException("'Route' element not parsed correctly (end tag not found)");
	}
	
	
	protected void parseForwardingRule(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader , DoubleMatrix2D f_de) throws XMLStreamException
	{
		long linkId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "linkId"));
		long demandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "demandId"));
		double splittingRatio = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "splittingRatio"));

		f_de.set (mapOldId2Demand.get(demandId).index , mapOldId2Link.get(linkId).index  , splittingRatio);

		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "attribute":
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("forwardingRule")) return;
					break;
			}
		}

		throw new RuntimeException("'Forwarding rule' element not parsed correctly (end tag not found)");
	}

	protected void parseHopByHopRouting(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		netPlan.setRoutingType (RoutingType.HOP_BY_HOP_ROUTING , mapOldId2Layer.get(layerId) );
		final NetworkLayer layer = mapOldId2Layer.get(layerId);
		final int D = netPlan.getNumberOfDemands(layer);
		final int E = netPlan.getNumberOfLinks(layer);
		DoubleMatrix2D f_de = DoubleFactory2D.sparse.make (D,E);

		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "forwardingRule":
							parseForwardingRule(netPlan, layerId, xmlStreamReader , f_de);
							break;

						default:
							throw new RuntimeException("Bad");
					}
					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("hopByHopRouting")) 
					{ 
						NetworkLayer thisLayer = mapOldId2Layer.get(layerId); 
						netPlan.setForwardingRules(f_de , thisLayer); 
						return;
					}
					break;
			}
		}
		
		throw new RuntimeException("'Hop-by-hop routing' element not parsed correctly (end tag not found)");
	}

	protected void parseLink(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long linkId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		long originNodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "originNodeId"));
		long destinationNodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "destinationNodeId"));
		double capacity = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "capacity"));
		double lengthInKm = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "lengthInKm"));
		double propagationSpeedInKmPerSecond = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "propagationSpeedInKmPerSecond"));
		boolean isUp = true; try { isUp = xmlStreamReader.getAttributeAsBoolean(xmlStreamReader.getAttributeIndex(null, "isUp")); } catch (Exception e) {} 

		Link newLink = netPlan.addLink(mapOldId2Node.get(originNodeId), mapOldId2Node.get(destinationNodeId), capacity, lengthInKm, propagationSpeedInKmPerSecond, null , mapOldId2Layer.get (layerId));
		mapOldId2Link.put (linkId , newLink);
		newLink.setFailureState(isUp);
		
		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();
			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "attribute":
							String key = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "key"));
							String name = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value"));
							if (key.equals(KEY_STRING_BIDIRECTIONALCOUPLE)) name = "" + mapOldId2Link.get(Long.parseLong(name)).getId();
							newLink.setAttribute(key, name);
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("link")) return;
					break;
			}
		}

		throw new RuntimeException("'Link' element not parsed correctly (end tag not found)");
	}

	
	protected void parseSRG(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long srgId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		double meanTimeToFailInHours = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "meanTimeToFailInHours"));
		double meanTimeToRepairInHours = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "meanTimeToRepairInHours"));
		SharedRiskGroup newSRG = netPlan.addSRG(meanTimeToFailInHours, meanTimeToRepairInHours, null);
		mapOldId2Srg.put (srgId , newSRG);
		
		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "attribute":
							String key = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "key"));
							String name = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value"));
							newSRG.setAttribute(key, name);
							break;

						case "node":
							long nodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
							newSRG.addNode(mapOldId2Node.get(nodeId));
							break;

						case "link":
//							long layerId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "layerId"));
							long linkId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "linkId"));
							newSRG.addLink(mapOldId2Link.get(linkId));
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("srg")) return;
					break;
			}
		}

		throw new RuntimeException("'SRG' element not parsed correctly (end tag not found)");
	}


	protected void parseLayer(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long layerId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		String demandTrafficUnitsName = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "demandTrafficUnitsName"));
		String layerDescription = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "description"));
		String layerName = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "name"));
		String linkCapacityUnitsName = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "linkCapacityUnitsName"));

		NetworkLayer newLayer;
		if (!hasAlreadyReadOneLayer)
		{
			if (netPlan.layers.size() != 1) throw new RuntimeException ("Bad");
			newLayer = netPlan.layers.get (0);
			newLayer.demandTrafficUnitsName = demandTrafficUnitsName;
			newLayer.description = layerDescription;
			newLayer.name = layerName;
			newLayer.linkCapacityUnitsName= linkCapacityUnitsName;
			hasAlreadyReadOneLayer = true;
		}
		else
		{
			newLayer = netPlan.addLayer(layerName, layerDescription, linkCapacityUnitsName, demandTrafficUnitsName, null , null);
		}

		mapOldId2Layer.put(layerId , newLayer);

		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "attribute":
							String key = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "key"));
							String name = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value"));
							newLayer.setAttribute(key, name);
							break;

						case "demand":
							parseDemand(netPlan, layerId, xmlStreamReader);
							break;

						case "multicastDemand":
							parseMulticastDemand(netPlan, layerId, xmlStreamReader);
							break;

						case "multicastTree":
							parseMulticastTree(netPlan, layerId, xmlStreamReader);
							break;

						case "link":
							parseLink(netPlan, layerId, xmlStreamReader);
							break;

						case "hopByHopRouting":
							parseHopByHopRouting(netPlan, layerId, xmlStreamReader);
							break;

						case "sourceRouting":
							parseSourceRouting(netPlan, layerId, xmlStreamReader);
							break;

						default:
							throw new RuntimeException("Bad child (" + startElementName + ") for layer element");
					}
					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("layer")) return;
					break;
			}
		}

		throw new RuntimeException("'Layer' element not parsed correctly (end tag not found)");
	}

	protected void parseMulticastDemand(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long demandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		long ingressNodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "ingressNodeId"));
		Set<Long> egressNodeIds = new HashSet <Long>(LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "egressNodeIds"))));
		Set<Node> newEgressNodes = new HashSet<Node> (); for (long nodeId : egressNodeIds) newEgressNodes.add (mapOldId2Node.get(nodeId));
		
		double offeredTraffic = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "offeredTraffic"));
		MulticastDemand newDemand = netPlan.addMulticastDemand(mapOldId2Node.get(ingressNodeId), newEgressNodes , offeredTraffic, null , mapOldId2Layer.get(layerId));
		mapOldId2MulticastDemand.put (demandId , newDemand);
		
		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "attribute":
							String key = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "key"));
							String name = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value"));
							newDemand.setAttribute(key, name);
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("multicastDemand")) return;
					break;
			}
		}

		throw new RuntimeException("'MulticastDemand' element not parsed correctly (end tag not found)");
	}

	protected void parseSourceRouting(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		netPlan.setRoutingType (RoutingType.SOURCE_ROUTING , mapOldId2Layer.get(layerId));

		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "protectionSegment":
							parseProtectionSegment(netPlan, layerId, xmlStreamReader);
							break;

						case "route":
							parseRoute(netPlan, layerId, xmlStreamReader);
							break;

						default:
							throw new RuntimeException("Bad");
					}
					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("sourceRouting")) return;
					break;
			}
		}

		throw new RuntimeException("'Source routing' element not parsed correctly (end tag not found)");
	}
	
	protected void parseMulticastTree(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long treeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		long demandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "demandId"));
		double carriedTraffic = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "carriedTraffic"));
		double occupiedCapacity = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "occupiedCapacity"));
		double carriedTrafficIfNotFailing = carriedTraffic; try { carriedTrafficIfNotFailing = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "carriedTrafficIfNotFailing")); } catch (Exception e) {} 
		double occupiedLinkCapacityIfNotFailing = occupiedCapacity; try { occupiedLinkCapacityIfNotFailing = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "occupiedLinkCapacityIfNotFailing")); } catch (Exception e) {} 
		if (occupiedCapacity < 0) occupiedCapacity = carriedTraffic;
		MulticastDemand demand = mapOldId2MulticastDemand.get(demandId);
		Set<Long> currentSetLinks = new HashSet<Long> (LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "currentSetLinks"))));
		Set<Long> initialSetLinksWhenWasCreated = new HashSet<Long> (LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "linkIds"))));
		Set<Link> initialSetLinks_link = new HashSet<Link> (); for (long linkId : initialSetLinksWhenWasCreated) initialSetLinks_link.add (mapOldId2Link.get(linkId));
		Set<Link> currentSetLinks_link = new HashSet<Link> (); for (long linkId : currentSetLinks) currentSetLinks_link.add (mapOldId2Link.get(linkId));
		MulticastTree newTree = netPlan.addMulticastTree(demand , carriedTrafficIfNotFailing , occupiedLinkCapacityIfNotFailing , initialSetLinks_link , null);
		mapOldId2MulticastTree.put (treeId , newTree);
		newTree.setLinks(currentSetLinks_link);
		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "attribute":
							String key = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "key"));
							String name = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value"));
							newTree.setAttribute(key, name);
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("multicastTree")) return;
					break;
			}
		}

		throw new RuntimeException("'Multicast tree' element not parsed correctly (end tag not found)");
	}
	
}
