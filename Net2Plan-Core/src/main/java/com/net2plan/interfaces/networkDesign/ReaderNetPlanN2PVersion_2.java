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

import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.LongUtils;
import com.net2plan.utils.Pair;
import org.codehaus.stax2.XMLStreamReader2;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class ReaderNetPlanN2PVersion_2 implements IReaderNetPlan
{
	protected Map<Long,Node> mapOldId2Node;
	protected Map<Long,SharedRiskGroup> mapOldId2Srg;
	protected Map<Long,NetworkLayer> mapOldId2Layer;
	protected Map<Pair<Long,Long>,Link> mapOldId2Link;
	protected Map<Pair<Long,Long>,Demand> mapOldId2Demand;
	protected Map<Pair<Long,Long>,Route> mapOldId2Route;
	//protected Map<Pair<Long,Long>,ProtectionSegment> mapOldId2ProtectionSegment;
	protected boolean hasAlreadyReadOneLayer;
	
	
	@Override
	public void create(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		this.mapOldId2Node = new HashMap<Long,Node> ();
		this.mapOldId2Srg = new HashMap<Long,SharedRiskGroup> ();
		this.mapOldId2Layer = new HashMap<Long,NetworkLayer> ();
		this.mapOldId2Link = new HashMap<Pair<Long,Long>,Link> ();
		this.mapOldId2Demand = new HashMap<Pair<Long,Long>,Demand> ();
		this.mapOldId2Route = new HashMap<Pair<Long,Long>,Route> ();
		//this.mapOldId2ProtectionSegment = new HashMap<Pair<Long,Long>,ProtectionSegment> ();
		this.hasAlreadyReadOneLayer = false;
		
		parseNetwork(netPlan, xmlStreamReader);
		
		// System.out.println ("netPlan leido: --- " + netPlan + " --- netPlan leido");
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();

	}
	
	protected void parseDemand(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long demandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		long ingressNodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "ingressNodeId"));
		long egressNodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "egressNodeId"));
		double offeredTraffic = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "offeredTraffic"));
//		netPlan.nextDemandId.put(layerId, demandId);
		Demand newDemand = netPlan.addDemand(mapOldId2Node.get(ingressNodeId), mapOldId2Node.get(egressNodeId), offeredTraffic, null , mapOldId2Layer.get(layerId));
		mapOldId2Demand.put (Pair.of (layerId , demandId) , newDemand);

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
					if (endElementName.equals("demand")) return;
					break;
			}
		}

		throw new RuntimeException("'Demand' element not parsed correctly (end tag not found)");
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
		mapOldId2Layer.put (layerId , newLayer);
		
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

						case "link":
							parseLink(netPlan, layerId, xmlStreamReader);
							break;

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
					if (endElementName.equals("layer")) return;
					break;
			}
		}

		throw new RuntimeException("'Layer' element not parsed correctly (end tag not found)");
	}

	protected void parseLink(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long linkId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		long originNodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "originNodeId"));
		long destinationNodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "destinationNodeId"));
		double capacity = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "capacity"));
		double lengthInKm = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "lengthInKm"));
		double propagationSpeedInKmPerSecond = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "propagationSpeedInKmPerSecond"));
//		netPlan.nextLinkId.put(layerId, linkId);
		Link newLink = netPlan.addLink(mapOldId2Node.get(originNodeId), mapOldId2Node.get(destinationNodeId), capacity, lengthInKm, propagationSpeedInKmPerSecond, null , mapOldId2Layer.get (layerId));
		mapOldId2Link.put (Pair.of (layerId , linkId) , newLink);

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

						case "layerCoupling":
							long upperLayerLinkId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "upperLayerLinkId"));
							long upperLayerId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "upperLayerId"));
							long lowerLayerDemandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "lowerLayerDemandId"));
							long lowerLayerId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "lowerLayerId"));
							mapOldId2Demand.get(Pair.of(lowerLayerId, lowerLayerDemandId)).coupleToUpperLayerLink(mapOldId2Link.get(Pair.of (upperLayerId, upperLayerLinkId)));
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("network"))
					{
//						if (!hasAlreadyReadOneLayer && netPlan.layers.size() > 1) netPlan.removeNetworkLayer (netPlan.layers.get(0));
						return;
					}

					break;
			}
		}

		throw new RuntimeException("'Network' element not parsed correctly (end tag not found)");
	}

	protected void parseNode(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long nodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		double xCoord = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "xCoord"));
		double yCoord = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "yCoord"));
		String nodeName = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "name"));
		Node newNode = netPlan.addNode(xCoord, yCoord, nodeName, null);
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

	protected void parseProtectionSegment(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long segmentId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		double reservedCapacity = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "reservedCapacity"));
		List<Long> seqLinks = LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "seqLinks")));
//		netPlan.nextSegmentId.put(layerId, segmentId);
		List<Link> newSeqLinks = new LinkedList<Link> (); for (long linkId : seqLinks) newSeqLinks.add (mapOldId2Link.get(Pair.of(layerId , linkId)));
		//ProtectionSegment newSegment = netPlan.addProtectionSegment(newSeqLinks, reservedCapacity, null);
		//mapOldId2ProtectionSegment.put (Pair.of(layerId , segmentId) , newSegment);
		
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
							//newSegment.setAttribute(key, name);
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

	protected void parseRoute(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long routeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		long demandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "demandId"));
		double carriedTraffic = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "carriedTraffic"));
		double occupiedCapacity = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "occupiedCapacity"));
		if (occupiedCapacity < 0) occupiedCapacity = carriedTraffic;
		List<Long> seqLinks = LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "seqLinks")));
		List<Long> backupSegmentList = LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "backupSegmentList")));
//		netPlan.nextRouteId.put(layerId, routeId);
		List<Link> newSeqLinks = new LinkedList<Link> (); for (long linkId : seqLinks) newSeqLinks.add (mapOldId2Link.get(Pair.of(layerId , linkId)));
		Route newRoute = netPlan.addRoute(mapOldId2Demand.get(Pair.of(layerId , demandId)) , carriedTraffic, occupiedCapacity, newSeqLinks, null);
		mapOldId2Route.put (Pair.of (layerId,routeId),newRoute);
		//for(long segmentId : backupSegmentList) newRoute.addProtectionSegment(mapOldId2ProtectionSegment.get(Pair.of(layerId , segmentId)));
		
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

	protected void parseSRG(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long srgId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		double meanTimeToFailInHours = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "meanTimeToFailInHours"));
		double meanTimeToRepairInHours = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "meanTimeToRepairInHours"));
//		netPlan.nextSRGId = new MutableLong(srgId);
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
							long layerId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "layerId"));
							long linkId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "linkId"));
							newSRG.addLink(mapOldId2Link.get(Pair.of(layerId , linkId)));
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
}

