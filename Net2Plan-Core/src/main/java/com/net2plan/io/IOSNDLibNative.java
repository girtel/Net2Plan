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

import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants;
import com.net2plan.internal.plugins.IOFilter;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Importer for the native SNDLib format.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.1
 */
@SuppressWarnings("unchecked")
public class IOSNDLibNative extends IOFilter
{
	private final static String title = "Native SNDLib";
	private final static List<Triple<String, String, String>> parameters;
	
	static
	{
		parameters = new LinkedList<Triple<String, String, String>>();
		parameters.add(Triple.of("sndlib.demandModel", "#select# undirected directed", "Demand model type: directed, undirected"));
		parameters.add(Triple.of("sndlib.linkModel", "#select# undirected directed", "Link model type: directed, undirected"));
		parameters.add(Triple.of("sndlib.nodeCoordinatesType", "#select# lonlat xy", "Demand model type: xy lonlat"));
	}
	
	/**
	 * Default constructor.
	 *
	 * @since 0.3.1
	 */
	public IOSNDLibNative()
	{
		super(title, EnumSet.of(Constants.IOFeature.LOAD_DESIGN), "txt");
	}
	
	@Override
	public String getName()
	{
		return title + " import filter";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		return Collections.unmodifiableList(parameters);
	}
	
	@Override
	public NetPlan readFromFile(File file)
	{
		Map<String, String> options = getCurrentOptions();
		boolean bidirectionalDemands = options.get("sndlib.demandModel").equals("undirected");
		boolean bidirectionalLinks = options.get("sndlib.linkModel").equals("undirected");
		boolean isEuclidean = options.get("sndlib.nodeCoordinatesType").equals("xy");
		
		NetPlan netPlan = new NetPlan();
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))
		{
			String line;
			
			/* Loop until "Nodes" section */
			while ((line = in.readLine()) != null)
			{
				if (line.trim().toLowerCase(Locale.getDefault()).startsWith("nodes ("))
					break;
			}
			
			/* Read all nodes */
			Map<String, Long> nodeName2Id = new HashMap<String, Long>();
			while ((line = in.readLine()) != null)
			{
				line = line.trim();
				
				String[] data = StringUtils.split(line, "() \t");
				if (data.length < 3) break;
				
				double xCoord = Double.parseDouble(data[1]);
				double yCoord = Double.parseDouble(data[2]);
				String name = data[0];
				long nodeId = netPlan.addNode(xCoord, yCoord, name, null).getId ();
				nodeName2Id.put(name, nodeId);
			}

			/* Loop until "Links" section */
			while ((line = in.readLine()) != null)
				if (line.trim().toLowerCase(Locale.getDefault()).startsWith("links ("))
					break;

			/* Read all links */
			while ((line = in.readLine()) != null)
			{
				line = line.trim();

				String[] data = StringUtils.split(line, "() \t");
				if (data.length < 4) break;
				
				String name = data[0];
				long originNodeId = nodeName2Id.get(data[1]); Node originNode = netPlan.getNodeFromId (originNodeId);
				long destinationNodeId = nodeName2Id.get(data[2]); Node destinationNode = netPlan.getNodeFromId (destinationNodeId);
				double capacity = Double.parseDouble(data[3]);
				double lengthInKm = isEuclidean ? netPlan.getNodePairEuclideanDistance(originNode, destinationNode) : netPlan.getNodePairHaversineDistanceInKm(originNode, destinationNode);
				Map<String, String> attributeMap = new LinkedHashMap<String, String>();
				attributeMap.put("name", name);
				
				if (bidirectionalLinks) netPlan.addLinkBidirectional(originNode, destinationNode, capacity, lengthInKm, 200000 , attributeMap);
				else netPlan.addLink(originNode, destinationNode, capacity, lengthInKm, 200000 , attributeMap);
			}

			/* Loop until "Demands" section */
			while ((line = in.readLine()) != null)
				if (line.trim().toLowerCase(Locale.getDefault()).startsWith("demands ("))
					break;

			/* Read all demands */
			while ((line = in.readLine()) != null)
			{
				line = line.trim();

				String[] data = StringUtils.split(line, "() \t");
				if (data.length < 5) break;
				
				String name = data[0];
				long ingressNodeId = nodeName2Id.get(data[1]); Node ingressNode = netPlan.getNodeFromId (ingressNodeId);
				long egressNodeId = nodeName2Id.get(data[2]); Node egressNode = netPlan.getNodeFromId (egressNodeId);
				double offeredTraffic = Double.parseDouble(data[4]);
				Map<String, String> attributeMap = new LinkedHashMap<String, String>();
				attributeMap.put("name", name);
				
				if (bidirectionalDemands) netPlan.addDemandBidirectional(ingressNode, egressNode, offeredTraffic, attributeMap);
				else netPlan.addDemand(ingressNode, egressNode, offeredTraffic, attributeMap);
			}

			/* Loop until "Admissible paths" section */
			while ((line = in.readLine()) != null)
				if (line.trim().toLowerCase(Locale.getDefault()).startsWith("admissible_paths ("))
					break;

			/* Read all admissible paths */
			while ((line = in.readLine()) != null)
			{
				line = line.trim();

				String[] data = StringUtils.split(line, "() \t");
				if (data.length < 1) break;
				
				String demandName = data[0];
				Collection<Demand> demandIds = (Collection<Demand>) NetPlan.getNetworkElementsByAttribute(netPlan.getDemands () , "name", demandName);
				while ((line = in.readLine()) != null)
				{
					line = line.trim();
					
					String[] data1 = StringUtils.split(line, "() \t");
					if (data1.length < 1) break;
					
					String name = data1[0];
					Map<String, String> attributeMap = new LinkedHashMap<String, String>();
					attributeMap.put("name", name);
					
					List<Link> linkMap = new LinkedList<Link> ();
					for(int i = 1; i < data1.length; i++)
						linkMap.addAll ((Collection<Link>) NetPlan.getNetworkElementsByAttribute (netPlan.getLinks () , "name", data1[i]));
					List<Node> nodes = new LinkedList<Node> (); for (Long id : nodeName2Id.values ()) nodes.add (netPlan.getNodeFromId (id));
					for(Demand demand : demandIds)
					{
						final List<Link> seqLinks = GraphUtils.getShortestPath(nodes , linkMap , demand.getIngressNode() , demand.getEgressNode() , null);
						if (seqLinks.isEmpty()) throw new Net2PlanException("Bad");
						netPlan.addRoute(demand, 0, 0 , seqLinks, attributeMap);
					}
				}
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		
		return netPlan;
	}	
}
