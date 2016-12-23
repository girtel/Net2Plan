/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/




 





package com.net2plan.interfaces.networkDesign;

import com.net2plan.internal.AttributeMap;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.LongUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.codehaus.stax2.XMLStreamReader2;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.*;

class ReaderNetPlanN2PVersion_5 implements IReaderNetPlan //extends NetPlanFormat_v3
{
//	private Map<Long,NetworkElement> mapId2NewNE;
//	protected Map<Long,Node> mapOldId2Node;
//	protected Map<Long,SharedRiskGroup> mapOldId2Srg;
//	protected Map<Long,Resource> mapOldId2Resource;
//	protected Map<Long,NetworkLayer> mapOldId2Layer;
//	protected Map<Long,Link> mapOldId2Link;
//	protected Map<Long,Demand> mapOldId2Demand;
//	protected Map<Long,MulticastDemand> mapOldId2MulticastDemand;
//	protected Map<Long,Route> mapOldId2Route;
//	protected Map<Long,MulticastTree> mapOldId2MulticastTree;
//	protected Map<Long,ProtectionSegment> mapOldId2ProtectionSegment;
	protected boolean hasAlreadyReadOneLayer;
	
	public void create(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
//		this.mapOldId2Node = new HashMap<Long,Node> ();
//		this.mapOldId2Srg = new HashMap<Long,SharedRiskGroup> ();
//		this.mapOldId2Resource = new HashMap<Long,Resource> ();
//		this.mapOldId2Layer = new HashMap<Long,NetworkLayer> ();
//		this.mapOldId2Link = new HashMap<Long,Link> ();
//		this.mapOldId2Demand = new HashMap<Long,Demand> ();
//		this.mapOldId2MulticastDemand = new HashMap<Long,MulticastDemand> ();
//		this.mapOldId2Route = new HashMap<Long,Route> ();
//		this.mapOldId2MulticastTree = new HashMap<Long,MulticastTree> ();
//		this.mapOldId2ProtectionSegment = new HashMap<Long,ProtectionSegment> ();
		this.hasAlreadyReadOneLayer = false;
		parseNetwork(netPlan, xmlStreamReader);

//		System.out.println ("End ReaderNetPlan_v5: " + netPlan + " ----------- ");
		
		netPlan.checkCachesConsistency();
	}

	protected void parseNetwork(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		String networkDescription_thisNetPlan = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "description"));
		String networkName_thisNetPlan = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "name"));
		final Long nexElementId_thisNetPlan = Long.parseLong(xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "nextElementId")));
		netPlan.setNetworkDescription(networkDescription_thisNetPlan);
		netPlan.setNetworkName(networkName_thisNetPlan);
		netPlan.nextElementId = new MutableLong(nexElementId_thisNetPlan);
		if (netPlan.nextElementId.toLong() <= 0) throw new Net2PlanException ("A network element has an id higher than the nextElementId");

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

						case "resource":
							parseResource(netPlan, xmlStreamReader);
							break;

						case "srg":
							parseSRG(netPlan, xmlStreamReader);
							break;

						case "layerCouplingDemand":
							long upperLayerLinkId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "upperLayerLinkId"));
							long lowerLayerDemandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "lowerLayerDemandId"));
							netPlan.getDemandFromId(lowerLayerDemandId).coupleToUpperLayerLink(netPlan.getLinkFromId(upperLayerLinkId));
							break;

						case "layerCouplingMulticastDemand":
							List<Long> upperLayerLinkIds = LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "upperLayerLinkIds")));
							long lowerLayerMulticastDemandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "lowerLayerDemandId"));
							Set<Link> setLinksToCouple = new HashSet<Link> (); for (long linkId : upperLayerLinkIds) setLinksToCouple.add (netPlan.getLinkFromId(linkId));
							netPlan.getMulticastDemandFromId(lowerLayerMulticastDemandId).couple(setLinksToCouple);
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
		if (segmentId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		double reservedCapacity = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "reservedCapacity"));
		List<Long> seqLinks = LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "seqLinks")));
		List<Link> newSeqLinks = new LinkedList<Link> (); for (long linkId : seqLinks) newSeqLinks.add (netPlan.getLinkFromId(linkId));
		ProtectionSegment newSegment = netPlan.addProtectionSegment(segmentId , newSeqLinks, reservedCapacity, null);
		
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
							newSegment.setAttribute(key, name);
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
		if (nodeId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		double xCoord = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "xCoord"));
		double yCoord = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "yCoord"));
		String nodeName = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "name"));
		boolean isUp = true; try { isUp = xmlStreamReader.getAttributeAsBoolean(xmlStreamReader.getAttributeIndex(null, "isUp")); } catch (Exception e) {} 
		//netPlan.nextNodeId = new MutableLong(nodeId);
		Node newNode = netPlan.addNode(nodeId , xCoord, yCoord, nodeName, null);
		newNode.setFailureState(isUp);

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
		if (demandId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		long ingressNodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "ingressNodeId"));
		long egressNodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "egressNodeId"));
		double offeredTraffic = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "offeredTraffic"));
		Demand newDemand = netPlan.addDemand(demandId , netPlan.getNodeFromId(ingressNodeId), netPlan.getNodeFromId(egressNodeId), offeredTraffic, null , netPlan.getNetworkLayerFromId(layerId));

		List<String> mandatorySequenceOfTraversedResourceTypes = new LinkedList<String> ();
		boolean finalElementRead = false;
		while(xmlStreamReader.hasNext() && !finalElementRead)
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
							if (key.equals(NetPlan.KEY_STRING_BIDIRECTIONALCOUPLE)) name = "" + netPlan.getDemandFromId(Long.parseLong(name)).getId();
							newDemand.setAttribute(key, name);
							break;

						case "serviceChainResourceTypeOfSequence":
							String type = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "type"));
							mandatorySequenceOfTraversedResourceTypes.add(type);
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("demand")) { finalElementRead = true; break; }
			}
		}

		if (!finalElementRead) throw new RuntimeException("'Demand' element not parsed correctly (end tag not found)");
		newDemand.setServiceChainSequenceOfTraversedResourceTypes(mandatorySequenceOfTraversedResourceTypes);
	}

	protected void parseRoute(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long routeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		if (routeId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		long demandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "demandId"));
		//double carriedTraffic = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "carriedTraffic"));
		//double occupiedCapacity = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "occupiedCapacity"));
		double carriedTrafficIfNotFailing = 0.0; try { carriedTrafficIfNotFailing = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "carriedTrafficIfNotFailing")); } catch (Exception e) {} 
		double occupiedLinkCapacityIfNotFailing = 0.0; try { occupiedLinkCapacityIfNotFailing = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "occupiedLinkCapacityIfNotFailing")); } catch (Exception e) {} 
		if (occupiedLinkCapacityIfNotFailing < 0) occupiedLinkCapacityIfNotFailing = carriedTrafficIfNotFailing;
		
		List<Long> initialSeqLinksAndResources = LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "initialSeqLinksAndResources")));
		List<Double> initialResourceOccupationMap = DoubleUtils.toList(xmlStreamReader.getAttributeAsDoubleArray(xmlStreamReader.getAttributeIndex(null, "initialResourceOccupationMap")));
		List<Long> currentSeqLinksSegmentsAndResources = LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "currentSeqLinksSegmentsAndResources")));
		List<Double> currentResourceOccupationMap = DoubleUtils.toList(xmlStreamReader.getAttributeAsDoubleArray(xmlStreamReader.getAttributeIndex(null, "currentResourceOccupationMap")));
//		List<Long> currentSeqLinksAndProtectionSegments = new LinkedList<Long> (initialSeqLinksWhenCreated);
//		try { currentSeqLinksAndProtectionSegments = LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "currentSeqLinksAndProtectionSegments"))); } catch (Exception e) { }
		List<Long> backupSegmentList = LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "backupSegmentList")));
		List<NetworkElement> initialSeqLinksAndResources_obj = new LinkedList<NetworkElement> (); 
		for (long linkOrResourceId : initialSeqLinksAndResources)
			if (netPlan.getLinkFromId(linkOrResourceId) != null) initialSeqLinksAndResources_obj.add (netPlan.getLinkFromId(linkOrResourceId));
			else if (netPlan.getResourceFromId(linkOrResourceId) != null) initialSeqLinksAndResources_obj.add (netPlan.getResourceFromId(linkOrResourceId));
			else throw new Net2PlanException ("Error parsing route information. Unknown element of id: " + linkOrResourceId);
		List<NetworkElement> currentSeqLinksSegmentsAndResources_obj = new LinkedList<NetworkElement> (); 
		for (long linkOrSegmentOrResourceId : currentSeqLinksSegmentsAndResources)
			if (netPlan.getLinkFromId(linkOrSegmentOrResourceId) != null) currentSeqLinksSegmentsAndResources_obj.add (netPlan.getLinkFromId(linkOrSegmentOrResourceId));
			else if (netPlan.getProtectionSegmentFromId(linkOrSegmentOrResourceId) != null) currentSeqLinksSegmentsAndResources_obj.add (netPlan.getProtectionSegmentFromId(linkOrSegmentOrResourceId));
			else if (netPlan.getResourceFromId(linkOrSegmentOrResourceId) != null) currentSeqLinksSegmentsAndResources_obj.add (netPlan.getResourceFromId(linkOrSegmentOrResourceId));
			else throw new Net2PlanException ("Error parsing route information. Unknown element of id: " + linkOrSegmentOrResourceId);
		Map<Resource,Double> initialResourceOccupationMap_obj = new HashMap<Resource,Double> ();
		{
			Iterator<Double> it = initialResourceOccupationMap.iterator();
			while (it.hasNext())
			{
				final long id = (long) (double) it.next();
				final double cap = it.next();
				if (netPlan.getResourceFromId(id) == null) throw new Net2PlanException ("Error parsing route information. Unknown element of id: " + id);
				initialResourceOccupationMap_obj.put(netPlan.getResourceFromId(id) , cap);
			}
		}
		Map<Resource,Double> currentResourceOccupationMap_obj = new HashMap<Resource,Double> ();
		{
			Iterator<Double> it = currentResourceOccupationMap.iterator();
			while (it.hasNext())
			{
				final long id = (long) (double) it.next();
				final double cap = it.next();
				if (netPlan.getResourceFromId(id) == null) throw new Net2PlanException ("Error parsing route information. Unknown element of id: " + id);
				currentResourceOccupationMap_obj.put(netPlan.getResourceFromId(id) , cap);
			}
		}
		
//		List<Link> initialSeqLinksAndResources_obj = new LinkedList<Link> (); for (long linkId : initialSeqLinksAndResources) initialSeqLinksAndResources_obj.add (netPlan.getLinkFromId(linkId));
//		List<Link> seqLinksAndProtectionSegments_link = new LinkedList<Link> (); 
//		for (long linkId : currentSeqLinksAndProtectionSegments) 
//			if (netPlan.getLinkFromId(linkId) != null) seqLinksAndProtectionSegments_link.add (netPlan.getLinkFromId(linkId));
//			else if (mapOldId2ProtectionSegment.get(linkId) != null) seqLinksAndProtectionSegments_link.add (mapOldId2ProtectionSegment.get(linkId));
//			else throw new Net2PlanException ("Error parsing route information (current sequence of links and protection segments)");
//		Route newRoute = netPlan.addRoute(mapOldId2Demand.get(demandId) , carriedTrafficIfNotFailing, occupiedLinkCapacityIfNotFailing, initialSeqLinksWhenCreated_link, null);
		Route newRoute = netPlan.addServiceChain(routeId , netPlan.getDemandFromId(demandId) , carriedTrafficIfNotFailing, occupiedLinkCapacityIfNotFailing, initialSeqLinksAndResources_obj, initialResourceOccupationMap_obj , null);
		for(long segmentId : backupSegmentList) newRoute.addProtectionSegment(netPlan.getProtectionSegmentFromId(segmentId));
		newRoute.setSeqLinksSegmentsAndResourcesOccupation(currentSeqLinksSegmentsAndResources_obj , currentResourceOccupationMap_obj);
		if (Math.abs (newRoute.getCarriedTrafficInNoFailureState() - carriedTrafficIfNotFailing) > 1e-3) throw new RuntimeException ("Bad");
		if (Math.abs (newRoute.getOccupiedCapacityInNoFailureState() - occupiedLinkCapacityIfNotFailing) > 1e-3) throw new RuntimeException ("Bad");
		
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
	
	
	protected void parseForwardingRule(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long linkId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "linkId"));
		long demandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "demandId"));
		double splittingRatio = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "splittingRatio"));

		netPlan.getNetworkLayerFromId(layerId).forwardingRules_f_de.set (netPlan.getDemandFromId(demandId).index , netPlan.getLinkFromId(linkId).index  , splittingRatio);

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
		netPlan.setRoutingType (RoutingType.HOP_BY_HOP_ROUTING , netPlan.getNetworkLayerFromId(layerId) );

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
							parseForwardingRule(netPlan, layerId, xmlStreamReader);
							break;

						default:
							throw new RuntimeException("Bad");
					}
					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("hopByHopRouting")) 
					{ 
						NetworkLayer thisLayer = netPlan.getNetworkLayerFromId(layerId); 
						netPlan.setForwardingRules(netPlan.getMatrixDemandBasedForwardingRules(thisLayer).copy() , thisLayer); 
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
		if (linkId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		long originNodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "originNodeId"));
		long destinationNodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "destinationNodeId"));
		double capacity = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "capacity"));
		double lengthInKm = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "lengthInKm"));
		double propagationSpeedInKmPerSecond = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "propagationSpeedInKmPerSecond"));
		boolean isUp = true; try { isUp = xmlStreamReader.getAttributeAsBoolean(xmlStreamReader.getAttributeIndex(null, "isUp")); } catch (Exception e) {} 

		Link newLink = netPlan.addLink(linkId , netPlan.getNodeFromId(originNodeId), netPlan.getNodeFromId(destinationNodeId), capacity, lengthInKm, propagationSpeedInKmPerSecond, null , netPlan.getNetworkLayerFromId(layerId));
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
							if (key.equals(NetPlan.KEY_STRING_BIDIRECTIONALCOUPLE)) name = "" + netPlan.getLinkFromId(Long.parseLong(name)).getId();
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
		if (srgId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		double meanTimeToFailInHours = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "meanTimeToFailInHours"));
		double meanTimeToRepairInHours = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "meanTimeToRepairInHours"));
		SharedRiskGroup newSRG = netPlan.addSRG(srgId , meanTimeToFailInHours, meanTimeToRepairInHours, null);
		
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
							newSRG.addNode(netPlan.getNodeFromId(nodeId));
							break;

						case "link":
							long linkId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "linkId"));
							newSRG.addLink(netPlan.getLinkFromId(linkId));
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

	protected void parseResource(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long resId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		if (resId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		long hostNodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "hostNodeId"));
		if (netPlan.getNodeFromId(hostNodeId) == null) throw new Net2PlanException ("Could not find the hot node of a resource when reading");
		String type = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "type"));
		String name = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "name"));
		String capacityMeasurementUnits = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "capacityMeasurementUnits"));
		double processingTimeToTraversingTrafficInMs = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "processingTimeToTraversingTrafficInMs"));
		double capacity = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "capacity"));

		Map<String,String> attributes = new HashMap<String,String> ();
		Map<Resource,Double> occupiedCapacitiesInBaseResources = new HashMap<Resource,Double> ();
		boolean finalElementReached = false;
		while(xmlStreamReader.hasNext() && !finalElementReached)
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "attribute":
							final String key = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "key"));
							final String value = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value"));
							attributes.put(key,value);
							break;

						case "baseResource":
							final long baseResourceId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
							final double occupiedCapacity = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "occupiedCapacity"));
							if (netPlan.getResourceFromId(baseResourceId) == null) throw new Net2PlanException ("Cannot find base resource of id: " + baseResourceId);
							occupiedCapacitiesInBaseResources.put(netPlan.getResourceFromId(baseResourceId) , occupiedCapacity);
							break;
						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("resource")) { finalElementReached = true; break; }
			}
		}

		if (!finalElementReached) throw new RuntimeException("'Resource' element not parsed correctly (end tag not found)");
		netPlan.addResource(resId , type , name , netPlan.getNodeFromId(hostNodeId) , capacity , capacityMeasurementUnits , 
				occupiedCapacitiesInBaseResources , processingTimeToTraversingTrafficInMs , new AttributeMap(attributes));
	}
	
	protected void parseLayer(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long layerId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id"));
		if (layerId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		String demandTrafficUnitsName = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "demandTrafficUnitsName"));
		String layerDescription = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "description"));
		String layerName = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "name"));
		String linkCapacityUnitsName = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "linkCapacityUnitsName"));
		final boolean isDefaultLayer = Boolean.parseBoolean(xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "isDefaultLayer")));
		
		NetworkLayer newLayer;
		if (!hasAlreadyReadOneLayer)
		{
			if (netPlan.layers.size() != 1) throw new RuntimeException ("Bad");
			if (netPlan.layers.get (0).id != layerId)
			{
				// the Id of first layer is different => create a new one and remove the existing
				newLayer = netPlan.addLayer(layerId , layerName, layerDescription, linkCapacityUnitsName, demandTrafficUnitsName, null);
				netPlan.removeNetworkLayer(netPlan.layers.get (0));
			}
			else
			{
				newLayer = netPlan.layers.get (0); // it already has the right Id
				newLayer.demandTrafficUnitsName = demandTrafficUnitsName;
				newLayer.description = layerDescription;
				newLayer.name = layerName;
				newLayer.linkCapacityUnitsName= linkCapacityUnitsName;
			}
			hasAlreadyReadOneLayer = true;
		}
		else
		{
			newLayer = netPlan.addLayer(layerId , layerName, layerDescription, linkCapacityUnitsName, demandTrafficUnitsName, null);
		}

		if (isDefaultLayer) netPlan.setNetworkLayerDefault(newLayer);

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
		if (demandId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		long ingressNodeId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "ingressNodeId"));
		Set<Long> egressNodeIds = new HashSet <Long>(LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "egressNodeIds"))));
		Set<Node> newEgressNodes = new HashSet<Node> (); for (long nodeId : egressNodeIds) newEgressNodes.add (netPlan.getNodeFromId(nodeId));
		
		double offeredTraffic = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "offeredTraffic"));
		MulticastDemand newDemand = netPlan.addMulticastDemand(demandId , netPlan.getNodeFromId(ingressNodeId), newEgressNodes , offeredTraffic, null , netPlan.getNetworkLayerFromId(layerId));
		
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
		netPlan.setRoutingType (RoutingType.SOURCE_ROUTING , netPlan.getNetworkLayerFromId(layerId));

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
		if (treeId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		long demandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "demandId"));
		double carriedTraffic = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "carriedTraffic"));
		double occupiedCapacity = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "occupiedCapacity"));
		double carriedTrafficIfNotFailing = carriedTraffic; try { carriedTrafficIfNotFailing = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "carriedTrafficIfNotFailing")); } catch (Exception e) {} 
		double occupiedLinkCapacityIfNotFailing = occupiedCapacity; try { occupiedLinkCapacityIfNotFailing = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "occupiedLinkCapacityIfNotFailing")); } catch (Exception e) {} 
		if (occupiedCapacity < 0) occupiedCapacity = carriedTraffic;
		MulticastDemand demand = netPlan.getMulticastDemandFromId(demandId);
		Set<Long> currentSetLinks = new HashSet<Long> (LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "currentSetLinks"))));
		Set<Long> initialSetLinksWhenWasCreated = new HashSet<Long> (LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, "linkIds"))));
		Set<Link> initialSetLinks_link = new HashSet<Link> (); for (long linkId : initialSetLinksWhenWasCreated) initialSetLinks_link.add (netPlan.getLinkFromId(linkId));
		Set<Link> currentSetLinks_link = new HashSet<Link> (); for (long linkId : currentSetLinks) currentSetLinks_link.add (netPlan.getLinkFromId(linkId));
		MulticastTree newTree = netPlan.addMulticastTree(treeId , demand , carriedTrafficIfNotFailing , occupiedLinkCapacityIfNotFailing , initialSetLinks_link , null);
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
