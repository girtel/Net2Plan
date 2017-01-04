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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.mutable.MutableLong;
import org.codehaus.stax2.XMLStreamReader2;

import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.LongUtils;

class ReaderNetPlanN2PVersion_5 implements IReaderNetPlan //extends NetPlanFormat_v3
{
	private boolean hasAlreadyReadOneLayer;
	private XMLStreamReader2 xmlStreamReader;
	
	public void create(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		this.hasAlreadyReadOneLayer = false;
		this.xmlStreamReader = xmlStreamReader;
		parseNetwork(netPlan);

//		System.out.println ("End ReaderNetPlan_v5: " + netPlan + " ----------- ");
		
		netPlan.checkCachesConsistency();
	}

	protected void parseNetwork(NetPlan netPlan) throws XMLStreamException
	{
		final String networkDescription_thisNetPlan = getString ("description");
		final String networkName_thisNetPlan = getString ("name");
		final Long nexElementId_thisNetPlan = getLong ("nextElementId");
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
							parseLayer(netPlan);
							break;

						case "node":
							parseNode(netPlan);
							break;

						case "resource":
							parseResource(netPlan);
							break;

						case "srg":
							parseSRG(netPlan);
							break;

						case "layerCouplingDemand":
							final long upperLayerLinkId = getLong ("upperLayerLinkId");
							final long lowerLayerDemandId = getLong ("lowerLayerDemandId");
							netPlan.getDemandFromId(lowerLayerDemandId).coupleToUpperLayerLink(netPlan.getLinkFromId(upperLayerLinkId));
							break;

						case "layerCouplingMulticastDemand":
							final long lowerLayerMulticastDemandId = getLong ("lowerLayerDemandId");
							final Set<Link> setLinksToCouple = getLinkSetFromIds(netPlan , getListLong("upperLayerLinkIds"));
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
	
	private void parseNode(NetPlan netPlan) throws XMLStreamException
	{
		final long nodeId = getLong ("id");
		if (nodeId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		final double xCoord = getDouble ("xCoord");
		final double yCoord = getDouble ("yCoord");
		final String nodeName = getString ("name");
		boolean isUp = true; try { isUp = getBoolean ("isUp"); } catch (Exception e) {} 
		//netPlan.nextNodeId = new MutableLong(nodeId);
		Node newNode = netPlan.addNode(nodeId , xCoord, yCoord, nodeName, null);
		newNode.setFailureState(isUp);
		readAndAddAttributesToEnd(newNode, "node");
	}

	private void parseDemand(NetPlan netPlan, long layerId) throws XMLStreamException
	{
		final long demandId = getLong ("id");
		if (demandId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		final long ingressNodeId = getLong ("ingressNodeId");
		final long egressNodeId = getLong ("egressNodeId");
		final double offeredTraffic = getDouble ("offeredTraffic");
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

	private void parseRoute(NetPlan netPlan, long layerId) throws XMLStreamException
	{
		final long routeId = getLong ("id");
		if (routeId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		final long demandId = getLong ("demandId");
		
		final double currentCarriedTrafficIfNotFailing = getDouble ("currentCarriedTrafficIfNotFailing");
		final List<Double> currentLinksAndResourcesOccupationIfNotFailing = getListDouble("currentLinksAndResourcesOccupationIfNotFailing");
		final List<NetworkElement> currentPath = getLinkAndResorceListFromIds(netPlan, getListLong("currentPath"));
		final Route newRoute = netPlan.addServiceChain(netPlan.getDemandFromId(demandId), currentCarriedTrafficIfNotFailing, 
				currentLinksAndResourcesOccupationIfNotFailing, currentPath, null);
		
		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();

			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "primaryPath":
							newRoute.setPrimaryPath(getLinkAndResorceListFromIds(netPlan, getListLong("path")));
							break;
						case "backupPath":
							newRoute.addBackupPath(getLinkAndResorceListFromIds(netPlan, getListLong("path")));
							break;
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
	
	
	private void parseForwardingRule(NetPlan netPlan, long layerId) throws XMLStreamException
	{
		final long linkId = getLong ("linkId");
		final long demandId = getLong ("demandId");
		final double splittingRatio = getDouble ("splittingRatio");

		netPlan.getNetworkLayerFromId(layerId).forwardingRules_f_de.set (netPlan.getDemandFromId(demandId).index , netPlan.getLinkFromId(linkId).index  , splittingRatio);
		readAndAddAttributesToEnd(null, "forwardingRule");
	}

	private void parseHopByHopRouting(NetPlan netPlan, long layerId) throws XMLStreamException
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
							parseForwardingRule(netPlan, layerId);
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

	private void parseLink(NetPlan netPlan, long layerId) throws XMLStreamException
	{
		final long linkId = getLong ("id");
		if (linkId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		final long originNodeId = getLong ("originNodeId");
		final long destinationNodeId = getLong ("destinationNodeId");
		final double capacity = getDouble ("capacity");
		final double lengthInKm = getDouble ("lengthInKm");
		final double propagationSpeedInKmPerSecond = getDouble ("propagationSpeedInKmPerSecond");
		boolean isUp = true; try { isUp = getBoolean ("isUp"); } catch (Exception e) {} 

		Link newLink = netPlan.addLink(linkId , netPlan.getNodeFromId(originNodeId), netPlan.getNodeFromId(destinationNodeId), capacity, lengthInKm, propagationSpeedInKmPerSecond, null , netPlan.getNetworkLayerFromId(layerId));
		newLink.setFailureState(isUp);
		readAndAddAttributesToEnd(newLink, "link");
	}

	
	private void parseSRG(NetPlan netPlan) throws XMLStreamException
	{
		final long srgId = getLong ("id");
		if (srgId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		final double meanTimeToFailInHours = getDouble ("meanTimeToFailInHours");
		final double meanTimeToRepairInHours = getDouble ("meanTimeToRepairInHours");
		SharedRiskGroup newSRG = netPlan.addSRG(srgId , meanTimeToFailInHours, meanTimeToRepairInHours, null);
		Set<Node> srgNodes = getNodeSetFromIds(netPlan, getListLong("nodes"));
		Set<Link> srgLinks = getLinkSetFromIds(netPlan, getListLong("links"));
		for (Node n : srgNodes) newSRG.addNode(n);
		for (Link e : srgLinks) newSRG.addLink(e);
		readAndAddAttributesToEnd(newSRG, "srg");
	}

	private void parseResource(NetPlan netPlan) throws XMLStreamException
	{
		final long resId = getLong ("id");
		if (resId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		final long hostNodeId = getLong ("hostNodeId");
		if (netPlan.getNodeFromId(hostNodeId) == null) throw new Net2PlanException ("Could not find the hot node of a resource when reading");
		final String type = getString ("type");
		final String name = getString ("name");
		final String capacityMeasurementUnits = getString ("capacityMeasurementUnits");
		final double processingTimeToTraversingTrafficInMs = getDouble ("processingTimeToTraversingTrafficInMs");
		final double capacity = getDouble ("capacity");
		final List<Double> baseResourceAndOccupiedCapacitiesMap = getListDouble("baseResourceAndOccupiedCapacitiesMap");
		Map<Resource,Double> occupiedCapacitiesInBaseResources = getResourceOccupationMap(netPlan, baseResourceAndOccupiedCapacitiesMap); 
		Resource newResource = netPlan.addResource(resId , type , name , netPlan.getNodeFromId(hostNodeId) , capacity , capacityMeasurementUnits , 
				occupiedCapacitiesInBaseResources , processingTimeToTraversingTrafficInMs , null);
		readAndAddAttributesToEnd(newResource, "resource");
	}
	
	private void parseLayer(NetPlan netPlan) throws XMLStreamException
	{
		final long layerId = getLong ("id");
		if (layerId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		final String demandTrafficUnitsName = getString ("demandTrafficUnitsName");
		final String layerDescription = getString ("description");
		final String layerName = getString ("name");
		final String linkCapacityUnitsName = getString ("linkCapacityUnitsName");
		final boolean isDefaultLayer = getBoolean ("isDefaultLayer");
		
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
							newLayer.setAttribute(getString ("key"), getString ("name"));
							break;

						case "demand":
							parseDemand(netPlan, layerId);
							break;

						case "multicastDemand":
							parseMulticastDemand(netPlan, layerId);
							break;

						case "multicastTree":
							parseMulticastTree(netPlan, layerId);
							break;

						case "link":
							parseLink(netPlan, layerId);
							break;

						case "hopByHopRouting":
							parseHopByHopRouting(netPlan, layerId);
							break;

						case "sourceRouting":
							parseSourceRouting(netPlan, layerId);
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

	private void parseMulticastDemand(NetPlan netPlan, long layerId) throws XMLStreamException
	{
		final long demandId = getLong ("id");
		if (demandId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		final long ingressNodeId = getLong ("ingressNodeId");
		Set<Node> newEgressNodes = getNodeSetFromIds(netPlan ,  getListLong("egressNodeIds"));
		final double offeredTraffic = getDouble ("offeredTraffic");
		final MulticastDemand newDemand = netPlan.addMulticastDemand(demandId , netPlan.getNodeFromId(ingressNodeId), newEgressNodes , offeredTraffic, null , netPlan.getNetworkLayerFromId(layerId));
		readAndAddAttributesToEnd(newDemand, "multicastDemand");
	}

	private void parseSourceRouting(NetPlan netPlan, long layerId) throws XMLStreamException
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
						case "route":
							parseRoute(netPlan, layerId);
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
	
	private void parseMulticastTree(NetPlan netPlan, long layerId) throws XMLStreamException
	{
		final long treeId = getLong ("id");
		if (treeId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		final long demandId = getLong ("demandId");
		final double carriedTraffic = getDouble ("carriedTraffic");
		double occupiedCapacity = getDouble ("occupiedCapacity");
		double carriedTrafficIfNotFailing = carriedTraffic; try { carriedTrafficIfNotFailing = getDouble ("carriedTrafficIfNotFailing"); } catch (Exception e) {} 
		double occupiedLinkCapacityIfNotFailing = occupiedCapacity; try { occupiedLinkCapacityIfNotFailing = getDouble ("occupiedLinkCapacityIfNotFailing"); } catch (Exception e) {} 
		if (occupiedCapacity < 0) occupiedCapacity = carriedTraffic;
		final MulticastDemand demand = netPlan.getMulticastDemandFromId(demandId);
		final Set<Link> initialSetLinks_link = getLinkSetFromIds(netPlan, getListLong ("initialSetLinks"));
		final Set<Link> currentSetLinks_link = getLinkSetFromIds(netPlan, getListLong ("currentSetLinks"));
		final MulticastTree newTree = netPlan.addMulticastTree(treeId , demand , carriedTrafficIfNotFailing , occupiedLinkCapacityIfNotFailing , initialSetLinks_link , null);
		newTree.setLinks(currentSetLinks_link);
		readAndAddAttributesToEnd (newTree , "multicastTree");
	}

	private boolean getBoolean (String name) { return Boolean.parseBoolean(xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, name))); }
	private String getString (String name) throws XMLStreamException 	{ return xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, name));	}
	private long getLong (String name) throws XMLStreamException 	{ return xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, name)); }
	private List<Long> getListLong (String name) throws XMLStreamException 	{ return LongUtils.toList(xmlStreamReader.getAttributeAsLongArray(xmlStreamReader.getAttributeIndex(null, name))); }
	private List<Double> getListDouble (String name) throws XMLStreamException 	{ return DoubleUtils.toList(xmlStreamReader.getAttributeAsDoubleArray(xmlStreamReader.getAttributeIndex(null, name))); }
	private double getDouble (String name) throws XMLStreamException 	{ return xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, name)); }
	private static Set<Link> getLinkSetFromIds (NetPlan np , Collection<Long> ids) 
	{
		Set<Link> res = new HashSet<Link> (); for (long id : ids) res.add(np.getLinkFromId(id)); return res; 
	}
	private static Set<Node> getNodeSetFromIds (NetPlan np , Collection<Long> ids) 
	{
		Set<Node> res = new HashSet<Node> (); for (long id : ids) res.add(np.getNodeFromId(id)); return res; 
	}
	private static List<Link> getLinkListFromIds (NetPlan np , Collection<Long> ids) 
	{
		List<Link> res = new LinkedList<Link> (); for (long id : ids) res.add(np.getLinkFromId(id)); return res; 
	}
	private static List<NetworkElement> getLinkAndResorceListFromIds (NetPlan np , Collection<Long> ids) 
	{
		List<NetworkElement> res = new LinkedList<NetworkElement> (); 
		for (long id : ids)
		{
			NetworkElement e = np.getLinkFromId(id);
			if (e == null) e = np.getResourceFromId(id);
			if (e == null) throw new Net2PlanException ("Unknown id in the list");
			res.add(e); 
		}
		return res;
	}
	private static Map<Resource,Double> getResourceOccupationMap (NetPlan np , List<Double> resAndOccList)
	{
		Map<Resource,Double> res = new HashMap<Resource,Double> ();
		Iterator<Double> it = resAndOccList.iterator();
		while (it.hasNext())
		{
			final long id = (long) (double) it.next();
			if (!it.hasNext()) throw new Net2PlanException ("Wrong array size");
			final double val = it.next();
			final Resource r = np.getResourceFromId(id); if (r == null) throw new Net2PlanException ("Unknown resource id");
			res.put(r, val);
		}
		return res;
	}
	
	private void readAndAddAttributesToEnd (NetworkElement updateElement , String endingTag) throws XMLStreamException
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
						case "attribute":
							String key = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "key"));
							String name = xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value"));
							if (updateElement != null) updateElement.setAttribute(key, name);
							break;

						default:
							throw new RuntimeException("Bad");
					}

					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals(endingTag)) return;
					break;
			}
		}

		throw new RuntimeException("'" + endingTag +"' tag not parsed correctly (end tag not found)");

	}
	
}
