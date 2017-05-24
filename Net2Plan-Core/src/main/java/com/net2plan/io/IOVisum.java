// 20160505 I did not add LINKPOLY: it seems to me that it is not a real link, but a form of stating how to print the existing link between two nodes, when the link in opposite directions does not follow the same course
// 


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

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants;
import com.net2plan.internal.plugins.IOFilter;
import com.net2plan.utils.Triple;

import java.io.File;
import java.util.*;

/**
 * Importer filter for BRITE topology generator ({@code .brite}).
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.1
 * @see <a href='http://www.cs.bu.edu/brite/user_manual/node29.html'>The BRITE Output Format</a>
 */
public class IOVisum extends IOFilter
{
	public final static String ATTRIBUTEPREFIX = "visum_";
	private final static double DEFAULTLINKSPEEDKMSEC = 50.0 / 3600.0;
	private final static String TITLE = "VISUM";
	
	/**
	 * Default constructor.
	 *
	 * @since 0.3.1
	 */
	public IOVisum()
	{
		super(TITLE, EnumSet.of(Constants.IOFeature.LOAD_DESIGN), "accdb");
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

		try
		{
			/* Load the nodes */
			Table tableNode = DatabaseBuilder.open(file).getTable("NODE");
			Map<String,Node> mapNo2Node = new HashMap<String,Node> ();
			
			Set<String> columnNames = new HashSet<String> ();
			for (Column c : tableNode.getColumns()) columnNames.add(c.getName());
			
			for(Row row : tableNode) 
			{
				final String no = readField(row , "NO");
				final String name = readField(row , "NAME");
				final double xCoord = Double.parseDouble(readField(row , "XCOORD"));
				final double yCoord = Double.parseDouble(readField(row , "YCOORD"));
				Map<String,String> att = new HashMap<String,String> ();
				att.put (ATTRIBUTEPREFIX + "isZone" , "false");
				for (String columnName : columnNames)
					att.put(ATTRIBUTEPREFIX + columnName , readField(row , columnName));
				Node newNode = netPlan.addNode(xCoord , yCoord , name , att);
				mapNo2Node.put(no , newNode);
			}

			/* Load the zones (the centroids, sources and destinations of traffic) */
			Table tableZones = DatabaseBuilder.open(file).getTable("ZONE");
			Map<String,Node> mapNoOfZone2Node = new HashMap<String,Node> ();
			columnNames = new HashSet<String> ();
			for (Column c : tableZones.getColumns()) columnNames.add(c.getName());
			for(Row row : tableZones) 
			{
				final String no = readField(row , "NO");
				final String name = readField(row , "NAME");
				final double xCoord = Double.parseDouble(readField(row , "XCOORD"));
				final double yCoord = Double.parseDouble(readField(row , "YCOORD"));
				Map<String,String> att = new HashMap<String,String> ();
				att.put (ATTRIBUTEPREFIX + "isZone" , "true");
				for (String columnName : columnNames)
					att.put(ATTRIBUTEPREFIX + columnName , readField(row , columnName));
				Node newNode = netPlan.addNode(xCoord , yCoord , name , att);
				mapNoOfZone2Node.put(no , newNode);
			}
			
			/* Load the zones (the centroids, sources and destinations of traffic) */
			Table tableConnector = DatabaseBuilder.open(file).getTable("CONNECTOR");
			columnNames = new HashSet<String> ();
			for (Column c : tableConnector.getColumns()) columnNames.add(c.getName());
			for(Row row : tableConnector) 
			{
				final String zoneNo = readField(row , "ZONENO");
				final String nodeNo = readField(row , "NODENO");
				final String directionConnector = readField(row , "DIRECTION");
				final double lengthInKm = Double.parseDouble(readField(row , "LENGTH"));
				Map<String,String> att = new HashMap<String,String> ();
				att.put (ATTRIBUTEPREFIX + "isConnector" , "true");
				for (String columnName : columnNames)
					att.put(ATTRIBUTEPREFIX + columnName , readField(row , columnName));
				final Node centroidNode = mapNoOfZone2Node.get(zoneNo);
				final Node endNode = mapNo2Node.get(nodeNo);
				if ((centroidNode == null) || (endNode == null)) throw new RuntimeException ("VISUM reader: A connector has no defined input nodes");
				if (directionConnector.equalsIgnoreCase("O"))
					netPlan.addLink(centroidNode , endNode , 0 , lengthInKm , DEFAULTLINKSPEEDKMSEC , att);
				else if (directionConnector.equalsIgnoreCase("D"))
					netPlan.addLink(endNode , centroidNode , 0 , lengthInKm , DEFAULTLINKSPEEDKMSEC , att);
				else throw new RuntimeException ("VISUM reader: " + "Unknown direction");
			}

			/* Load the links */
			Table tableLink = DatabaseBuilder.open(file).getTable("LINK");
			columnNames = new HashSet<String> ();
			for (Column c : tableLink.getColumns()) columnNames.add(c.getName());
			for(Row row : tableLink) 
			{
				final String fromNodeNo = readField(row , "FROMNODENO");
				final String toNodeNo = readField(row , "TONODENO");
				final double lengthInKm = Double.parseDouble(readField(row , "LENGTH"));
				// PABLO: HOW TO SET THE CAPACITIES? HOW TO SET THE PROPAGATION SPEED? WHICH ARE THESE FIELDS?
				Map<String,String> att = new HashMap<String,String> ();
				att.put (ATTRIBUTEPREFIX + "isConnector" , "false");
				for (String columnName : columnNames)
					att.put(ATTRIBUTEPREFIX + columnName , readField(row , columnName));
				final Node originNode = mapNo2Node.get(fromNodeNo);
				final Node destinationNode = mapNo2Node.get(toNodeNo);
				if ((originNode == null) || (destinationNode == null)) throw new RuntimeException ("VISUM reader: A link has no defined input nodes");
				netPlan.addLink(originNode , destinationNode , 0 , lengthInKm , DEFAULTLINKSPEEDKMSEC , att);
			}

			/* Load the demands from the matrix file */
			String odMatrixFileName = file.getAbsolutePath();
			final String fileExtension = odMatrixFileName.substring(odMatrixFileName.lastIndexOf(".") + 1);
			odMatrixFileName = odMatrixFileName.substring(0 , odMatrixFileName.lastIndexOf(".")) + "-odMatrix." + fileExtension;
			if (new File (odMatrixFileName).exists()) // if the demand file exists, open it
			{
				Table tableDemand = DatabaseBuilder.open(new File (odMatrixFileName)).getTable("Vista de matriz");
				
				columnNames = new HashSet<String> ();
				for (Column c : tableDemand.getColumns()) columnNames.add(c.getName());
				System.out.println("Column names: " + columnNames);
				for(Row row : tableDemand) 
				{
					final String fromNodeNo = readField(row , "FROM");
					final String toNodeNo = readField(row , "TO");
					final String fromName = readField(row , "FROMNAME");
					final String toName = readField(row , "TONAME");
					final double value = Double.parseDouble(readField(row , "VALUE"));
					// PABLO: HOW TO SET THE CAPACITIES? HOW TO SET THE PROPAGATION SPEED? WHICH ARE THESE FIELDS?
					Map<String,String> att = new HashMap<String,String> ();
					for (String columnName : columnNames)
						att.put(ATTRIBUTEPREFIX + columnName , readField(row , columnName));
					final Node originNode = mapNoOfZone2Node.get(fromNodeNo);
					final Node destinationNode = mapNoOfZone2Node.get(toNodeNo);
					if ((originNode == null) || (destinationNode == null)) throw new RuntimeException ("VISUM reader: A demand has no defined input nodes");
					if (!originNode.getName().equals(fromName)) throw new RuntimeException ("VISUM reader: When reading the OD matrix, the origin node NO and the node NAME do not match the ones in the node table");
					if (!destinationNode.getName().equals(toName)) throw new RuntimeException ("VISUM reader: When reading the OD matrix, the origin node NO and the node NAME do not match the ones in the node table");
					if (originNode == destinationNode) 
					{
						System.out.println ("VISUM reader: A demand has the same ingress and egress node (" + fromName + ") and non-zero value (" + value + "). The demand is ignored.");
						continue;
					}
					Demand newDemand = netPlan.addDemand(originNode , destinationNode , value , att);
				}
			}
			
			
			
		} catch (Exception e) { e.printStackTrace(); new RuntimeException(e); } 
		
//		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))
// 		{
//			String line;
//			
//			/* Loop until "Nodes" section */
//			while ((line = in.readLine()) != null)
//				if (line.trim().toLowerCase(Locale.getDefault()).startsWith("nodes: ("))
//					break;
//			
//			/* Read all nodes */
//			Map<String, Long> nodeName2Id = new HashMap<String, Long>();
//			while ((line = in.readLine()) != null)
//			{
//				line = line.trim();
//				
//				String[] data = StringUtils.split(line, " \t");
//				if (data.length < 7) break;
//				
//				double xCoord = Double.parseDouble(data[1]);
//				double yCoord = Double.parseDouble(data[2]);
//				String name = data[0];
//				Node node = netPlan.addNode(xCoord, yCoord, name, null);
//				String asId = data[5];
//				String type = data[6];
//				
//				node.setAttribute("ASid", asId);
//				node.setAttribute("type", type);
//				nodeName2Id.put(name, node.getId ());
//			}
//
//			/* Loop until "Nodes" section */
//			while ((line = in.readLine()) != null)
//			{
//				if (line.trim().toLowerCase(Locale.getDefault()).startsWith("edges: ("))
//					break;
//			}
//
//			/* Read all links */
//			while ((line = in.readLine()) != null)
//			{
//				line = line.trim();
//
//				String[] data = StringUtils.split(line, " \t");
//				long originNodeId = nodeName2Id.get(data[1]);
//				long destinationNodeId = nodeName2Id.get(data[2]);
//				Node originNode = netPlan.getNodeFromId (originNodeId);
//				Node destinationNode = netPlan.getNodeFromId (destinationNodeId);
//				double capacityInGbps = Double.parseDouble(data[5]);
//				double lengthInKm = Double.parseDouble(data[3]);
//				double propagationTimeInMs = Double.parseDouble(data[4]);
//				if (propagationTimeInMs <= 0)
//				{
//					netPlan.addLink(originNode, destinationNode, capacityInGbps, lengthInKm, 200000, null);
//				}
//				else
//				{
//					double propagationSpeedInKmPerSecond = lengthInKm / (propagationTimeInMs * 1e-3);
//					netPlan.addLink(originNode, destinationNode, capacityInGbps, lengthInKm, propagationSpeedInKmPerSecond, null);
//				}
//			}
//		}
//		catch (IOException e)
//		{
//			throw new RuntimeException(e);
//		}
		
		return netPlan;
	}	
	
	private String readField (Row row , String fieldName)
	{
		Object value = row.get(fieldName);
		return value == null? "" : value.toString();
	}
	
}
