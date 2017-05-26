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

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants;
import com.net2plan.internal.plugins.IOFilter;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Importer filter for BRITE topology generator ({@code .brite}).
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.1
 * @see <a href='http://www.cs.bu.edu/brite/user_manual/node29.html'>The BRITE Output Format</a>
 */
public class IOBrite extends IOFilter
{
	private final static String TITLE = "BRITE";
	
	/**
	 * Default constructor.
	 *
	 * @since 0.3.1
	 */
	public IOBrite()
	{
		super(TITLE, EnumSet.of(Constants.IOFeature.LOAD_DESIGN), "brite");
	}
	
	@Override
	public String getName()
	{
		return TITLE + " import filter";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		return null;
	}
	
	@Override
	public NetPlan readFromFile(File file)
	{
		NetPlan netPlan = new NetPlan();
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))
 		{
			String line;
			
			/* Loop until "Nodes" section */
			while ((line = in.readLine()) != null)
				if (line.trim().toLowerCase(Locale.getDefault()).startsWith("nodes: ("))
					break;
			
			/* Read all nodes */
			Map<String, Long> nodeName2Id = new HashMap<String, Long>();
			while ((line = in.readLine()) != null)
			{
				line = line.trim();
				
				String[] data = StringUtils.split(line, " \t");
				if (data.length < 7) break;
				
				double xCoord = Double.parseDouble(data[1]);
				double yCoord = Double.parseDouble(data[2]);
				String name = data[0];
				Node node = netPlan.addNode(xCoord, yCoord, name, null);
				String asId = data[5];
				String type = data[6];
				
				node.setAttribute("ASid", asId);
				node.setAttribute("type", type);
				nodeName2Id.put(name, node.getId ());
			}

			/* Loop until "Nodes" section */
			while ((line = in.readLine()) != null)
			{
				if (line.trim().toLowerCase(Locale.getDefault()).startsWith("edges: ("))
					break;
			}

			/* Read all links */
			while ((line = in.readLine()) != null)
			{
				line = line.trim();

				String[] data = StringUtils.split(line, " \t");
				long originNodeId = nodeName2Id.get(data[1]);
				long destinationNodeId = nodeName2Id.get(data[2]);
				Node originNode = netPlan.getNodeFromId (originNodeId);
				Node destinationNode = netPlan.getNodeFromId (destinationNodeId);
				double capacityInGbps = Double.parseDouble(data[5]);
				double lengthInKm = Double.parseDouble(data[3]);
				double propagationTimeInMs = Double.parseDouble(data[4]);
				if (propagationTimeInMs <= 0)
				{
					netPlan.addLink(originNode, destinationNode, capacityInGbps, lengthInKm, 200000, null);
				}
				else
				{
					double propagationSpeedInKmPerSecond = lengthInKm / (propagationTimeInMs * 1e-3);
					netPlan.addLink(originNode, destinationNode, capacityInGbps, lengthInKm, propagationSpeedInKmPerSecond, null);
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
