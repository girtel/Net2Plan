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

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.codehaus.stax2.XMLStreamReader2;

import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

class ReaderNetPlanN2PVersion_3 extends ReaderNetPlanN2PVersion_2
{
	@Override
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

	protected void parseForwardingRule(NetPlan netPlan, long layerId, XMLStreamReader2 xmlStreamReader , DoubleMatrix2D f_de) throws XMLStreamException
	{
		long linkId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "linkId"));
		long demandId = xmlStreamReader.getAttributeAsLong(xmlStreamReader.getAttributeIndex(null, "demandId"));
		double splittingRatio = xmlStreamReader.getAttributeAsDouble(xmlStreamReader.getAttributeIndex(null, "splittingRatio"));

		f_de.set (mapOldId2Demand.get(Pair.of (layerId,demandId)).index , mapOldId2Link.get(Pair.of(layerId , linkId)).index  , splittingRatio);

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
		netPlan.setRoutingType (RoutingType.HOP_BY_HOP_ROUTING , mapOldId2Layer.get(layerId));
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
		/* Save the forwarding rules */
		throw new RuntimeException("'Hop-by-hop routing' element not parsed correctly (end tag not found)");
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
}


