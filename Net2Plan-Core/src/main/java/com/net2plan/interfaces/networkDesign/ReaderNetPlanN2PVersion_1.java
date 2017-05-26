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

import org.codehaus.stax2.XMLStreamReader2;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.*;

class ReaderNetPlanN2PVersion_1 implements IReaderNetPlan
{
	Map<Long, List<Long>> backupSegmentMap = new LinkedHashMap<Long, List<Long>>();
	
	@Override
	public void create(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		parseNetwork(netPlan, xmlStreamReader);
		
		/* Protection segments are not added now */
//		for(Entry<Long, List<Long>> entry : backupSegmentMap.entrySet())
//		{
//			long routeId = entry.getKey();
//			List<Long> backupSegmentList = entry.getValue();
//			for(long segmentId : backupSegmentList)
//				netPlan.defaultLayer.routes.get((int) routeId).addProtectionSegment(netPlan.defaultLayer.protectionSegments.get ((int) segmentId));
//		}
		
	}

	protected void parseDemand(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long ingressNodeId = -1;
		long egressNodeId = -1;
		double offeredTraffic = 0;
		Map<String, String> attributeMap = new LinkedHashMap<String, String>();

		int numAttributes = xmlStreamReader.getAttributeCount();
		for(int i = 0; i < numAttributes; i++)
		{
			String localName = xmlStreamReader.getAttributeLocalName(i);
			switch(localName)
			{
				case "ingressNodeId":
					ingressNodeId = xmlStreamReader.getAttributeAsLong(i);
					break;
					
				case "egressNodeId":
					egressNodeId = xmlStreamReader.getAttributeAsLong(i);
					break;
					
				case "offeredTrafficInErlangs":
					offeredTraffic = xmlStreamReader.getAttributeAsDouble(i);
					break;
					
				default:
					attributeMap.put(localName, xmlStreamReader.getAttributeValue(i));
					break;
			}
		}

		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					throw new RuntimeException("No nested elements allowed for 'Demand' element");

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("demandEntry"))
					{
						netPlan.addDemand(netPlan.nodes.get((int) ingressNodeId), netPlan.nodes.get((int) egressNodeId), offeredTraffic, attributeMap);
						return;
					}
					break;
			}
		}

		throw new RuntimeException("'Demand' element not parsed correctly (end tag not found)");
	}

	protected void parseDemandSet(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "demandEntry":
							parseDemand(netPlan, xmlStreamReader);
							break;

						default:
							throw new RuntimeException("Bad");
					}
					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("demandSet")) return;
					break;
			}
		}

		throw new RuntimeException("'Demand set' element not parsed correctly (end tag not found)");
	}
	
	protected void parseLink(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		Map<String, String> attributeMap = new LinkedHashMap<String, String>();
		double linkCapacity = 0;
		double linkLengthInKm = 0;
		long originNodeId = -1;
		long destinationNodeId = -1;

		int numAttributes = xmlStreamReader.getAttributeCount();
		for(int i = 0; i < numAttributes; i++)
		{
			String localName = xmlStreamReader.getAttributeLocalName(i);
			switch(localName)
			{
				case "originNodeId":
					originNodeId = xmlStreamReader.getAttributeAsLong(i);
					break;
					
				case "destinationNodeId":
					destinationNodeId = xmlStreamReader.getAttributeAsLong(i);
					break;
					
				case "linkCapacityInErlangs":
					linkCapacity = xmlStreamReader.getAttributeAsDouble(i);
					break;
					
				case "linkLengthInKm":
					linkLengthInKm = xmlStreamReader.getAttributeAsDouble(i);
					break;
					
				default:
					attributeMap.put(localName, xmlStreamReader.getAttributeValue(i));
					break;
			}
		}

		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					throw new RuntimeException("No nested elements allowed for 'Link' element");

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("link"))
					{
						netPlan.addLink(netPlan.nodes.get((int) originNodeId), netPlan.nodes.get((int) destinationNodeId), linkCapacity, linkLengthInKm, 200000 , attributeMap);
						return;
					}
					break;
			}
		}

		throw new RuntimeException("'Link' element not parsed correctly (end tag not found)");
	}

	protected void parseNetwork(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		int numAttributes = xmlStreamReader.getAttributeCount();
		for(int i = 0; i < numAttributes; i++)
		{
			String localName = xmlStreamReader.getAttributeLocalName(i);
			switch(localName)
			{
				case "description":
					netPlan.setNetworkDescription(xmlStreamReader.getAttributeValue(i));
					break;
					
				case "name":
					netPlan.setNetworkName(xmlStreamReader.getAttributeValue(i));
					break;
					
				default:
					netPlan.setAttribute(localName, xmlStreamReader.getAttributeValue(i));
					break;
			}
		}
		
		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();
			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "physicalTopology":
							parsePhysicalTopology(netPlan, xmlStreamReader);
							break;

						case "demandSet":
							parseDemandSet(netPlan, xmlStreamReader);
							break;
							
						case "routingInfo":
							parseRoutingInfo(netPlan, xmlStreamReader);
							break;

						case "protectionInfo":
							parseProtectionInfo(netPlan, xmlStreamReader);
							break;
							
						case "srgInfo":
							parseSRGInfo(netPlan, xmlStreamReader);
							
						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("network")) return;
					break;
			}
		}

		throw new RuntimeException("'Network' element not parsed correctly (end tag not found)");
	}

	protected void parseNode(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		double xCoord = 0;
		double yCoord = 0;
		String nodeName = null;
		Map<String, String> attributeMap = new LinkedHashMap<String, String>();
		
		int numAttributes = xmlStreamReader.getAttributeCount();
		for(int i = 0; i < numAttributes; i++)
		{
			String localName = xmlStreamReader.getAttributeLocalName(i);
			switch(localName)
			{
				case "xCoord":
					xCoord = xmlStreamReader.getAttributeAsDouble(i);
					break;
					
				case "yCoord":
					yCoord = xmlStreamReader.getAttributeAsDouble(i);
					break;
					
				case "name":
					nodeName = xmlStreamReader.getAttributeValue(i);
					break;
					
				default:
					attributeMap.put(localName, xmlStreamReader.getAttributeValue(i));
			}
		}
		
		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					throw new RuntimeException("No nested elements allowed for 'Node' element");

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("node"))
					{
						netPlan.addNode(xCoord, yCoord, nodeName, attributeMap);
						return;
					}
					break;
			}
		}

		throw new RuntimeException("'Node' element not parsed correctly (end tag not found)");
	}

	protected void parsePhysicalTopology(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "node":
							parseNode(netPlan, xmlStreamReader);
							break;
							
						case "link":
							parseLink(netPlan, xmlStreamReader);
							break;

						default:
							throw new RuntimeException("Bad");
					}
					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("physicalTopology")) return;
					break;
			}
		}

		throw new RuntimeException("'Physical topology' element not parsed correctly (end tag not found)");
	}

	protected void parseProtectionInfo(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
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
							parseProtectionSegment(netPlan, xmlStreamReader);
							break;

						default:
							throw new RuntimeException("Bad");
					}
					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("protectionInfo")) return;
					break;
			}
		}

		throw new RuntimeException("'Protection info' element not parsed correctly (end tag not found)");
	}

	protected void parseProtectionSegment(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		double reservedBandwidthInErlangs = 0;
		Map<String, String> attributeMap = new LinkedHashMap<String, String>();
		
		int numAttributes = xmlStreamReader.getAttributeCount();
		for(int i = 0; i < numAttributes; i++)
		{
			String localName = xmlStreamReader.getAttributeLocalName(i);
			switch(localName)
			{
				case "reservedBandwidthInErlangs":
					reservedBandwidthInErlangs = xmlStreamReader.getAttributeAsDouble(i);
					break;
					
				default:
					attributeMap.put(localName, xmlStreamReader.getAttributeValue(i));
			}
		}
		
		List<Long> seqLinks = new LinkedList<Long>();
		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "linkEntry":
							seqLinks.add(xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id")));
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("protectionSegment"))
					{
						List<Link> newSeqLinks = new LinkedList<Link> (); for (long linkId : seqLinks) newSeqLinks.add (netPlan.defaultLayer.links.get((int) linkId));
						/* Protection segments are not added */
						//netPlan.addProtectionSegment(newSeqLinks, reservedBandwidthInErlangs, attributeMap);
						return;
					}
					break;
			}
		}

		throw new RuntimeException("'Protection segment' element not parsed correctly (end tag not found)");
	}

	protected void parseRoute(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		long demandId = -1;
		double carriedTrafficInErlangs = 0;
		Map<String, String> attributeMap = new LinkedHashMap<String, String>();
		
		int numAttributes = xmlStreamReader.getAttributeCount();
		for(int i = 0; i < numAttributes; i++)
		{
			String localName = xmlStreamReader.getAttributeLocalName(i);
			switch(localName)
			{
				case "carriedTrafficInErlangs":
					carriedTrafficInErlangs = xmlStreamReader.getAttributeAsDouble(i);
					break;
					
				case "demandId":
					demandId = xmlStreamReader.getAttributeAsLong(i);
					break;
					
				default:
					attributeMap.put(localName, xmlStreamReader.getAttributeValue(i));
			}
		}
		
		List<Long> seqLinks = new LinkedList<Long>();
		List<Long> backupSegmentList = new LinkedList<Long>();
		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "linkEntry":
							seqLinks.add(xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id")));
							break;
							
						case "protectionSegmentEntry":
							backupSegmentList.add(xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id")));
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("route"))
					{
						List<Link> newSeqLinks = new LinkedList<Link> (); for (long linkId : seqLinks) newSeqLinks.add (netPlan.defaultLayer.links.get((int) linkId));
						Route route = netPlan.addRoute(netPlan.defaultLayer.demands.get ((int) demandId), carriedTrafficInErlangs, carriedTrafficInErlangs , newSeqLinks, attributeMap);
						backupSegmentMap.put((long) route.index, backupSegmentList);
						return;
					}
					break;
			}
		}

		throw new RuntimeException("'Route' element not parsed correctly (end tag not found)");
	}

	protected void parseRoutingInfo(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "route":
							parseRoute(netPlan, xmlStreamReader);
							break;

						default:
							throw new RuntimeException("Bad");
					}
					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("routingInfo")) return;
					break;
			}
		}

		throw new RuntimeException("'Routing info' element not parsed correctly (end tag not found)");
	}

	protected void parseSRG(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		double mttf = 0;
		double mttr = 0;
		Map<String, String> attributeMap = new LinkedHashMap<String, String>();
		
		int numAttributes = xmlStreamReader.getAttributeCount();
		for(int i = 0; i < numAttributes; i++)
		{
			String localName = xmlStreamReader.getAttributeLocalName(i);
			switch(localName)
			{
				case "mttf":
					mttf = xmlStreamReader.getAttributeAsDouble(i);
					break;
					
				case "mttr":
					mttr = xmlStreamReader.getAttributeAsDouble(i);
					break;
					
				default:
					attributeMap.put(localName, xmlStreamReader.getAttributeValue(i));
			}
		}
		
		Set<Long> nodeIds_thisSRG = new LinkedHashSet<Long>();
		Set<Long> linkIds_thisSRG = new LinkedHashSet<Long>();

		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "linkEntry":
							linkIds_thisSRG.add(xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id")));
							break;

						case "nodeEntry":
							nodeIds_thisSRG.add(xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "id")));
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("srg"))
					{
						SharedRiskGroup srg = netPlan.addSRG(mttf, mttr, attributeMap);
						for(long nodeId : nodeIds_thisSRG) srg.addNode (netPlan.nodes.get((int) nodeId));
						for(long linkId : linkIds_thisSRG) srg.addLink (netPlan.defaultLayer.links.get((int) linkId));
						return;
					}
					break;
			}
		}

		throw new RuntimeException("'SRG' element not parsed correctly (end tag not found)");
	}

	protected void parseSRGInfo(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "srg":
							parseSRG(netPlan, xmlStreamReader);
							break;

						default:
							throw new RuntimeException("Bad");
					}
					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("srgInfo")) return;
					break;
			}
		}

		throw new RuntimeException("'SRG info' element not parsed correctly (end tag not found)");
	}
}
