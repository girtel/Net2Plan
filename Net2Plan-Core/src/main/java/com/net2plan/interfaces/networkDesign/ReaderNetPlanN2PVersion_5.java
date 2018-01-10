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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.mutable.MutableLong;
import org.codehaus.stax2.XMLStreamReader2;

import com.google.common.collect.Sets;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.ProfileUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.LongUtils;
import com.net2plan.utils.Pair;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

class ReaderNetPlanN2PVersion_5 implements IReaderNetPlan //extends NetPlanFormat_v3
{
	private boolean hasAlreadyReadOneLayer;
	private XMLStreamReader2 xmlStreamReader;
	private Map<Route,List<Long>> backupRouteIdsMap;
	private Map<Long , List<Pair<Node,URL>>> nodeAndLayerToIconURLMap;
	
	public void create(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException
	{
		ProfileUtils.printTime("" , -1);
		this.hasAlreadyReadOneLayer = false;
		this.xmlStreamReader = xmlStreamReader;
		this.backupRouteIdsMap = new HashMap<Route,List<Long>> ();
		this.nodeAndLayerToIconURLMap = new HashMap<> ();

		parseNetwork(netPlan);

//		System.out.println ("End ReaderNetPlan_v5: " + netPlan + " ----------- ");
		
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		ProfileUtils.printTime("Reading n2p file");
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
		while (true) { try { netPlan.addGlobalPlanningDomain(getString ("planningDomain_" + (netPlan.getGlobalPlanningDomains().size())));  } catch(Exception e) { break; }   } 

		while(xmlStreamReader.hasNext())
		{
			xmlStreamReader.next();
			switch(xmlStreamReader.getEventType())
			{
				case XMLEvent.START_ELEMENT:
					String startElementName = xmlStreamReader.getName().toString();
					switch(startElementName)
					{
						case "tag":
							netPlan.addTag(xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value")));
							break;
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
		double population = 0; try { population = getDouble ("population"); } catch (Exception e) {}
		String siteName = null; try { siteName = getString ("siteName"); } catch (Exception e) {}
		boolean isUp = true; try { isUp = getBoolean ("isUp"); } catch (Exception e) {} 
		final Set<String> planningDomains = new HashSet<> ();
		while (true) { try { planningDomains.add(getString ("planningDomain_" + (planningDomains.size())));  } catch(Exception e) { break; }   } 

		Node newNode = netPlan.addNode(nodeId , xCoord, yCoord, nodeName, null);
		for (String pd : planningDomains) newNode.addToPlanningDomain(pd);
		newNode.setFailureState(isUp);
		newNode.setPopulation(population);
		if (siteName != null) newNode.setSiteName(siteName);
		
		/* read the icons information and put it in a map for later (layers are not created yet!) */
		for (long layerId : getListLong("layersWithIconsDefined"))
		{
			List<Pair<Node,URL>> iconsThisLayerSoFar = nodeAndLayerToIconURLMap.get (layerId);
			if (iconsThisLayerSoFar == null) { iconsThisLayerSoFar = new LinkedList<> (); nodeAndLayerToIconURLMap.put(layerId , iconsThisLayerSoFar); }
			URL url = null; try { url = new URL (getString ("nodeIconURLLayer_" + layerId)); } catch (Exception e) {}
			iconsThisLayerSoFar.add(Pair.of(newNode , url));
		}
		
		readAndAddAttributesToEndAndPdForNodes(newNode, "node");
	}

	private void parseDemand(NetPlan netPlan, long layerId) throws XMLStreamException
	{
		final long demandId = getLong ("id");
		if (demandId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		final long ingressNodeId = getLong ("ingressNodeId");
		final long egressNodeId = getLong ("egressNodeId");
		final double offeredTraffic = getDouble ("offeredTraffic");
		Demand.IntendedRecoveryType recoveryType;
		try { recoveryType = Demand.IntendedRecoveryType.valueOf(getString("intendedRecoveryType")); } 
		catch (XMLStreamException e) { recoveryType = Demand.IntendedRecoveryType.NOTSPECIFIED; }
		catch (Exception e) { recoveryType = Demand.IntendedRecoveryType.UNKNOWNTYPE; }
		long bidirectionalPairId = -1; try { bidirectionalPairId = getLong ("bidirectionalPairId"); } catch (Exception e) {}
		
		Demand newDemand = netPlan.addDemand(demandId , netPlan.getNodeFromId(ingressNodeId), netPlan.getNodeFromId(egressNodeId), offeredTraffic, null , netPlan.getNetworkLayerFromId(layerId));
		newDemand.setIntendedRecoveryType(recoveryType);
		final Demand bidirPairDemand = bidirectionalPairId == -1? null : netPlan.getDemandFromId(bidirectionalPairId); 
		if (bidirPairDemand != null)
		{
			if (bidirPairDemand.isBidirectional()) throw new RuntimeException ();
			bidirPairDemand.setBidirectionalPair(newDemand);
		}
		
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
						case "tag":
							newDemand.addTag(xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value")));
							break;
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

		/* Initial route may not exist, if so current equals the initial */
		List<NetworkElement> initialStatePath = new ArrayList<NetworkElement> (currentPath);
		boolean initialPathExists = true; 
		try { initialStatePath = getLinkAndResorceListFromIds(netPlan, getListLong("initialStatePath")); } catch (Exception e) { initialPathExists = false; } 
		final double initialStateCarriedTrafficIfNotFailing = initialPathExists? getDouble ("initialStateCarriedTrafficIfNotFailing") : currentCarriedTrafficIfNotFailing;
		final List<Double> initialStateOccupationIfNotFailing = initialPathExists? getListDouble("initialStateOccupationIfNotFailing") : new ArrayList<Double> (currentLinksAndResourcesOccupationIfNotFailing);
		
		final Route newRoute = netPlan.addServiceChain(routeId , netPlan.getDemandFromId(demandId), initialStateCarriedTrafficIfNotFailing, 
				initialStateOccupationIfNotFailing, initialStatePath, null);
		newRoute.setPath(currentCarriedTrafficIfNotFailing, currentPath, currentLinksAndResourcesOccupationIfNotFailing);

		/* To be added at the end: backup routes may not exist yet */
		this.backupRouteIdsMap.put(newRoute ,  getListLong ("backupRoutes")); 

		readAndAddAttributesToEndAndPdForNodes(newRoute, "route");
	}
	
	
	private void parseForwardingRule(NetPlan netPlan, long layerId , DoubleMatrix2D f_de) throws XMLStreamException
	{
		final long linkId = getLong ("linkId");
		final long demandId = getLong ("demandId");
		final double splittingRatio = getDouble ("splittingRatio");

		f_de.set (netPlan.getDemandFromId(demandId).index , netPlan.getLinkFromId(linkId).index  , splittingRatio);
		readAndAddAttributesToEndAndPdForNodes(null, "forwardingRule");
	}

	private void parseHopByHopRouting(NetPlan netPlan, long layerId) throws XMLStreamException
	{
		netPlan.setRoutingType (RoutingType.HOP_BY_HOP_ROUTING , netPlan.getNetworkLayerFromId(layerId) );
		final NetworkLayer layer = netPlan.getNetworkLayerFromId(layerId);
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
							parseForwardingRule(netPlan, layerId,f_de);
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
						netPlan.setForwardingRules(f_de , thisLayer); 
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
		long bidirectionalPairId = -1; try { bidirectionalPairId = getLong ("bidirectionalPairId"); } catch (Exception e) {}
		boolean isUp = true; try { isUp = getBoolean ("isUp"); } catch (Exception e) {} 

		Link newLink = netPlan.addLink(linkId , netPlan.getNodeFromId(originNodeId), netPlan.getNodeFromId(destinationNodeId), capacity, lengthInKm, propagationSpeedInKmPerSecond, null , netPlan.getNetworkLayerFromId(layerId));
		newLink.setFailureState(isUp);
		final Link bidirPairLink = bidirectionalPairId == -1? null : netPlan.getLinkFromId(bidirectionalPairId); 
		if (bidirPairLink != null)
		{
			if (bidirPairLink.isBidirectional()) throw new RuntimeException ();
			bidirPairLink.setBidirectionalPair(newLink);
		}
		readAndAddAttributesToEndAndPdForNodes(newLink, "link");
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
		readAndAddAttributesToEndAndPdForNodes(newSRG, "srg");
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

		URL urlIcon = null; try { urlIcon = new URL (getString ("urlIcon")); } catch (Exception e) {}
		final List<Double> baseResourceAndOccupiedCapacitiesMap = getListDouble("baseResourceAndOccupiedCapacitiesMap");
		Map<Resource,Double> occupiedCapacitiesInBaseResources = getResourceOccupationMap(netPlan, baseResourceAndOccupiedCapacitiesMap); 
		Resource newResource = netPlan.addResource(resId , type , name , netPlan.getNodeFromId(hostNodeId) , capacity , capacityMeasurementUnits , 
				occupiedCapacitiesInBaseResources , processingTimeToTraversingTrafficInMs , null);
		newResource.setUrlIcon(urlIcon);
		readAndAddAttributesToEndAndPdForNodes(newResource, "resource");
	}
	
	private void parseLayer(NetPlan netPlan) throws XMLStreamException
	{
		final long layerId = getLong ("id");
		if (layerId >= netPlan.nextElementId.toLong()) throw new Net2PlanException ("A network element has an id higher than the nextElementId");
		final String demandTrafficUnitsName = getString ("demandTrafficUnitsName");
		final String layerDescription = getString ("description");
		final String layerName = getString ("name");
		final String linkCapacityUnitsName = getString ("linkCapacityUnitsName");
		URL defaultNodeIconURL = null;
		try { defaultNodeIconURL = new URL (getString ("defaultNodeIconURL")); } catch (Exception e) {}
		final boolean isDefaultLayer = getBoolean ("isDefaultLayer");
		
		NetworkLayer newLayer;
		if (!hasAlreadyReadOneLayer)
		{
			if (netPlan.layers.size() != 1) throw new RuntimeException ("Bad");
			if (netPlan.layers.get (0).id != layerId)
			{
				// the Id of first layer is different => create a new one and remove the existing
				newLayer = netPlan.addLayer(layerId , layerName, layerDescription, linkCapacityUnitsName, demandTrafficUnitsName, defaultNodeIconURL , null);
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
			newLayer = netPlan.addLayer(layerId , layerName, layerDescription, linkCapacityUnitsName, demandTrafficUnitsName, defaultNodeIconURL , null);
		}

		/* write the node icons information, that is already there */
		if (nodeAndLayerToIconURLMap.containsKey(newLayer.getId()))
			for (Pair<Node,URL> iconInfo : nodeAndLayerToIconURLMap.get(newLayer.getId()))
				iconInfo.getFirst().setUrlNodeIcon(newLayer , iconInfo.getSecond());
		
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
						case "tag":
							newLayer.addTag(xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value")));
							break;

						case "attribute":
							newLayer.setAttribute(getString ("key"), getString ("value"));
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
		readAndAddAttributesToEndAndPdForNodes(newDemand, "multicastDemand");
	}

	private void parseSourceRouting(NetPlan netPlan, long layerId) throws XMLStreamException
	{
		netPlan.setRoutingType (RoutingType.SOURCE_ROUTING , netPlan.getNetworkLayerFromId(layerId));
		this.backupRouteIdsMap.clear(); // in multiple layers, we have to refresh this
		
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
							throw new RuntimeException("Bad: " + startElementName);
					}
					break;

				case XMLEvent.END_ELEMENT:
					String endElementName = xmlStreamReader.getName().toString();
					if (endElementName.equals("sourceRouting"))
					{
						/* Before returning, we add the backup routes */
						for (Entry<Route,List<Long>> entry : this.backupRouteIdsMap.entrySet())
						{
							final Route primary = entry.getKey();
							for (long backupId : entry.getValue()) primary.addBackupRoute(netPlan.getRouteFromId(backupId));
						}
						return;
					}
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
		final double carriedTraffic = getDouble ("carriedTrafficIfNotFailing");
		double occupiedCapacity = getDouble ("occupiedLinkCapacityIfNotFailing");
		double carriedTrafficIfNotFailing = carriedTraffic; try { carriedTrafficIfNotFailing = getDouble ("carriedTrafficIfNotFailing"); } catch (Exception e) {} 
		double occupiedLinkCapacityIfNotFailing = occupiedCapacity; try { occupiedLinkCapacityIfNotFailing = getDouble ("occupiedLinkCapacityIfNotFailing"); } catch (Exception e) {} 
		if (occupiedCapacity < 0) occupiedCapacity = carriedTraffic;

		final MulticastDemand demand = netPlan.getMulticastDemandFromId(demandId);
		final Set<Link> initialSetLinks_link = getLinkSetFromIds(netPlan, getListLong ("initialSetLinks"));
		final Set<Link> currentSetLinks_link = getLinkSetFromIds(netPlan, getListLong ("currentSetLinks"));
		final MulticastTree newTree = netPlan.addMulticastTree(treeId , demand , carriedTrafficIfNotFailing , occupiedLinkCapacityIfNotFailing , initialSetLinks_link , null);
		newTree.setLinks(currentSetLinks_link);
		readAndAddAttributesToEndAndPdForNodes (newTree , "multicastTree");
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
	
	private void readAndAddAttributesToEndAndPdForNodes (NetworkElement updateElement , String endingTag) throws XMLStreamException
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
						case "tag":
							updateElement.addTag(xmlStreamReader.getAttributeValue(xmlStreamReader.getAttributeIndex(null, "value")));
							break;

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
