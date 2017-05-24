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






package com.net2plan.io;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tint.IntFactory2D;
import cern.colt.matrix.tint.IntMatrix2D;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants;
import com.net2plan.internal.plugins.IOFilter;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.IntUtils;
import com.net2plan.utils.Triple;
import org.w3c.dom.*;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Importer filter for network designs from MatPlanWDM tool ({@code .xml}).
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.1
 * @see <a href='http://girtel.upct.es/~ppavon/matplanwdm/'>MatPlanWDM: An educational tool for network planning in wavelength-routing networks</a>
 */
public class IOMatPlanWDM_design extends IOFilter
{
	private final static String TITLE = "MatPlanWDM";
	private final static List<Triple<String, String, String>> PARAMETERS;
	private final static EntityResolver ENTITY_RESOLVER_DTD;
	
	static
	{
		PARAMETERS = new LinkedList<Triple<String, String, String>>();
		PARAMETERS.add(Triple.of("matplanwdm.defaultLightpathCapacity", "40", "Default lightpath capacity (Gbps)"));
		ENTITY_RESOLVER_DTD = new EntityResolverDTD();
	}
	
	/**
	 * Default constructor.
	 *
	 * @since 0.3.1
	 */
	public IOMatPlanWDM_design()
	{
		super(TITLE, EnumSet.of(Constants.IOFeature.LOAD_DESIGN), "xml");
	}
	
	@Override
	public String getName()
	{
		return TITLE + " design import filter";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		return PARAMETERS;
	}
	
	@Override
	public NetPlan readFromFile(File file)
	{
		try
		{
			InputStream inputStream = new FileInputStream(file);
			Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

			InputSource is = new InputSource(reader);
			is.setEncoding(StandardCharsets.UTF_8.name());

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setEntityResolver(ENTITY_RESOLVER_DTD);
			
			Document doc = db.parse(is);
			doc.getDocumentElement().normalize();

			NetPlan templateNetPlan = new NetPlan();

			/* Get network properties */
			NamedNodeMap networkAttributesMap = doc.getDocumentElement().getAttributes();
			int numNetworkAttrib = networkAttributesMap.getLength();
			for (int attribId = 0; attribId < numNetworkAttrib; attribId++)
			{
				String attribName = networkAttributesMap.item(attribId).getNodeName();
				String attribValue = networkAttributesMap.item(attribId).getNodeValue();

				switch (attribName)
				{
					case "title":
						templateNetPlan.setNetworkName(attribValue);
						break;

					case "description":
						templateNetPlan.setNetworkDescription(attribValue);
						break;
						
					default:
						templateNetPlan.setAttribute(attribName, attribValue);
						break;
				}
			}
			
			NodeList layers = doc.getElementsByTagName("layer");
			int numLayers = layers.getLength();
			
			Map<String, String> options = getCurrentOptions();
			double lightpathCapacity = Double.parseDouble(options.get("matplanwdm.defaultLightpathCapacity"));
			
			/* First, process physical topology */
			boolean alreadyProcessedPhysicalTopology = false;
			for (int layerId = 0; layerId < numLayers; layerId++)
			{
				Element layer = ((Element) layers.item(layerId));
				if (layer.getAttribute("id").equals("physicalTopology"))
				{
					if (alreadyProcessedPhysicalTopology) throw new Net2PlanException("Physical topology was already processed");
					alreadyProcessedPhysicalTopology = true;

					NodeList lightpathCapacityElement = layer.getElementsByTagName("lightpathCapacity");
					switch(lightpathCapacityElement.getLength())
					{
						case 1:
							lightpathCapacity = Double.parseDouble(((Element) lightpathCapacityElement.item(0)).getAttribute("value"));
							break;

						case 0:
							break;
							
						default:
							throw new Net2PlanException("'lightpathCapacity' was already processed");
					}
					
					templateNetPlan.getNetworkLayer (0).setName ("Physical topology");
					
					NodeList nodeList = ((Element) layer).getElementsByTagName("node");
					NodeList fiberList = ((Element) layer).getElementsByTagName("fiber");
					
					int N = nodeList.getLength();
					int E = fiberList.getLength();

					for (int i = 0; i < N; i++)
					{
						Element node = ((Element) nodeList.item(i));

						double xCoord = Double.parseDouble(node.getAttribute("xCoord"));
						double yCoord = Double.parseDouble(node.getAttribute("yCoord"));
						String name = node.getAttribute("nodeName");

						Map<String, String> nodeAttributes = new HashMap<String, String>();
						nodeAttributes.put("id", node.getAttribute("id"));
						nodeAttributes.put("level", node.getAttribute("nodeLevel"));
						nodeAttributes.put("timezone", node.getAttribute("nodeTimezone"));
						nodeAttributes.put("eoTransmitter", ((Element) node.getElementsByTagName("eoTransmitter").item(0)).getAttribute("number"));
						nodeAttributes.put("oeReceiver", ((Element) node.getElementsByTagName("oeReceiver").item(0)).getAttribute("number"));
						nodeAttributes.put("wc", ((Element) node.getElementsByTagName("wc").item(0)).getAttribute("number"));

						final Node n = templateNetPlan.addNode(xCoord, yCoord, name, nodeAttributes);
						n.setPopulation(Double.parseDouble(node.getAttribute("nodePopulation")));
					}

					for (int i = 0; i < E; i++)
					{
						Element fiber = ((Element) fiberList.item(i));

						Node originNode = (Node) NetPlan.getNetworkElementByAttribute (templateNetPlan.getNodes () , "id", fiber.getAttribute("origNodeId"));
						Node destinationNode = (Node) NetPlan.getNetworkElementByAttribute (templateNetPlan.getNodes () , "id", fiber.getAttribute("destNodeId"));
						double linkLengthInKm = Double.parseDouble(fiber.getAttribute("linkLengthInKm"));
						int numWavelengths = Integer.parseInt(fiber.getAttribute("numberWavelengths"));

						Map<String, String> fiberAttributes = new HashMap<String, String>();
						fiberAttributes.put("id", fiber.getAttribute("id"));

						templateNetPlan.addLink(originNode, destinationNode, numWavelengths, linkLengthInKm, 200000 , fiberAttributes);
					}
				}
			}
			
			/* Then, process traffic demands and virtual topology */
			int numTimeSlots = (numLayers - 1) / 2;
			if (numTimeSlots == 0) return templateNetPlan;
			
			int layerDemandId = 0;
			int layerVirtualTopologyId = 0;
			
			NetPlan netPlan = templateNetPlan.copy();
			for(int timeSlotId = 0; timeSlotId < numTimeSlots; timeSlotId++)
			{
				NetworkLayer wdmLayer = timeSlotId == 0 ? netPlan.getNetworkLayer ((int) 0) : netPlan.addLayerFrom(templateNetPlan.getNetworkLayer((int) 0));
				NetworkLayer ipLayer = netPlan.addLayer(null, null, null, null, null , null);
				
				if (numTimeSlots == 1)
				{
					wdmLayer.setName ("Physical topology");
					ipLayer.setName ("Virtual topology");
				}
				else
				{
					wdmLayer.setName ("Physical topology (time slot " + timeSlotId + ")");
					ipLayer.setName ("Virtual topology (time slot " + timeSlotId + ")");
				}
				
				do
				{
					Element layer = ((Element) layers.item(layerVirtualTopologyId));
					layerVirtualTopologyId++;
					
					if (layer.getAttribute("id").equals("virtualTopology"))
					{
						NodeList lightpathList = ((Element) layer).getElementsByTagName("lightpath");
						int LP = lightpathList.getLength();
						for (int i = 0; i < LP; i++)
						{
							Element lightpath = ((Element) lightpathList.item(i));

							Node originNode = (Node) NetPlan.getNetworkElementByAttribute (netPlan.getNodes () , "id", lightpath.getAttribute("origNodeId"));
							Node destinationNode = (Node) NetPlan.getNetworkElementByAttribute (netPlan.getNodes () , "id", lightpath.getAttribute("destNodeId"));
							
							Map<String, String> lightpathAttributes = new HashMap<String, String>();
							lightpathAttributes.put("id", lightpath.getAttribute("id"));
							lightpathAttributes.put("serialNumber", lightpath.getAttribute("serialNumber"));

							Demand lightpathDemand = netPlan.addDemand(originNode, destinationNode, lightpathCapacity, lightpathAttributes , wdmLayer);
							
							Element lightpathRouting = ((Element) lightpath.getElementsByTagName("lightpathRouting").item(0));
							NodeList fiberList_thisLightpath = ((Element) lightpathRouting).getElementsByTagName("fiberInLightpath");
							List<Link> fiberTable_thisLightpath = new LinkedList<Link>();
							Map<Link, Integer> fiberWavelengthMap = new LinkedHashMap<Link, Integer>();
							int E = fiberList_thisLightpath.getLength();
							for(int j = 0; j < E; j++)
							{
								Element fiber = ((Element) fiberList_thisLightpath.item(j));
//								long fiberId = netPlan.getLinkByAttribute(wdmLayerId, "id", fiber.getAttribute("id"));
								Link fiberLink = ((Link) netPlan.getNetworkElementByAttribute (netPlan.getLinks (wdmLayer) , "id", fiber.getAttribute("origNodeId")));
								int wavelengthId = Integer.parseInt(fiber.getAttribute("wavelengthId")) - 1;
								
								fiberTable_thisLightpath.add(fiberLink);
								fiberWavelengthMap.put(fiberLink, wavelengthId);
							}
							
							List<Link> seqFiberLinks = GraphUtils.getShortestPath(netPlan.getNodes () , fiberTable_thisLightpath, originNode, destinationNode, null);
							if (seqFiberLinks.isEmpty()) throw new Net2PlanException("No feasible route for lightpath " + lightpathDemand.getId ());
							Route lightpathRoute = netPlan.addRoute(lightpathDemand, lightpathCapacity, 1.0, seqFiberLinks, null);
							int[] seqWavelengths = IntUtils.toArray(CollectionUtils.select(fiberWavelengthMap, seqFiberLinks));
							IntMatrix2D seqFreqSlots = IntFactory2D.dense.make(new int [][] {seqWavelengths} );
							WDMUtils.setLightpathRSAAttributes(lightpathRoute, new WDMUtils.RSA(seqFiberLinks , seqFreqSlots) , true);
							WDMUtils.setLightpathRSAAttributes(lightpathRoute, new WDMUtils.RSA(seqFiberLinks , seqFreqSlots) , false);
							lightpathDemand.coupleToNewLinkCreated(ipLayer);
						}
						
						break;
					}
				}
				while (layerVirtualTopologyId < numLayers);
				
				do
				{
					Element layer = ((Element) layers.item(layerDemandId));
					layerDemandId++;
					
					if (layer.getAttribute("id").equals("trafficDemand"))
					{
						DoubleMatrix2D matXde = DoubleFactory2D.sparse.make (netPlan.getNumberOfDemands(ipLayer),netPlan.getNumberOfLinks(ipLayer));
						
						NodeList flowList = ((Element) layer).getElementsByTagName("flow");
						int F = flowList.getLength();
						for (int i = 0; i < F; i++)
						{
							Element flow = ((Element) flowList.item(i));

							Node originNode = (Node) netPlan.getNetworkElementByAttribute (netPlan.getNodes () , "id", flow.getAttribute("origNodeId"));
							Node destinationNode = (Node) netPlan.getNetworkElementByAttribute (netPlan.getNodes () , "id", flow.getAttribute("destNodeId"));
							double h_d = Double.parseDouble(flow.getAttribute("flowAverageRateDemand"));
							
							Map<String, String> demandAttributes = new HashMap<String, String>();
							demandAttributes.put("id", flow.getAttribute("id"));
							demandAttributes.put("serialNumber", flow.getAttribute("serialNumber"));
							if (flow.hasAttribute("flowPriority")) demandAttributes.put("flowPriority", flow.getAttribute("flowPriority"));
							if (flow.hasAttribute("flowInitTime")) demandAttributes.put("flowInitTime", flow.getAttribute("flowInitTime"));
							if (flow.hasAttribute("flowDuration")) demandAttributes.put("flowDuration", flow.getAttribute("flowDuration"));

							long demandId = netPlan.addDemand(originNode, destinationNode, h_d, demandAttributes , ipLayer).getId ();
							
							Element flowRouting = ((Element) flow.getElementsByTagName("flowRouting").item(0));
							NodeList lightpathList_thisFlow = ((Element) flowRouting).getElementsByTagName("lightpathInFlow");
							int E = lightpathList_thisFlow.getLength();
							for(int j = 0; j < E; j++)
							{
								Element lightpath = ((Element) lightpathList_thisFlow.item(j));
								long lightpathId = ((Link) netPlan.getNetworkElementByAttribute (netPlan.getLinks (ipLayer) , "id", lightpath.getAttribute("id"))).getId ();

								
								double x_de_thisDemand_thisLightpath = Double.parseDouble(lightpath.getAttribute("averageRate"));
								
//								x_de.put(Pair.of(demandId, lightpathId), x_de_thisDemand_thisLightpath);
								matXde.set (netPlan.getDemandFromId(demandId).getIndex() , netPlan.getLinkFromId(lightpathId).getIndex() , x_de_thisDemand_thisLightpath);
							}
						}
						
						if (matXde.zSum () != 0)
						{
							netPlan.setRoutingFromDemandLinkCarriedTraffic(matXde , false , false , ipLayer);
						}
						
						break;
					}
				}
				while (layerDemandId < numLayers);
				
				WDMUtils.checkResourceAllocationClashing(netPlan, false, false , wdmLayer);
			}
			
			return netPlan;
		}
		catch (ParserConfigurationException | SAXException | IOException | DOMException | NumberFormatException | UnsupportedOperationException | Net2PlanException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private static class EntityResolverDTD implements EntityResolver
	{
		@Override
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
		{
			return systemId.isEmpty() ? null : new InputSource(new StringReader(""));
		}
	}	
}
