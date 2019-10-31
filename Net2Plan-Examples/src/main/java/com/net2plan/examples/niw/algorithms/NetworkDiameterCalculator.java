package com.net2plan.examples.niw.algorithms;

import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.research.niw.networkModel.ImportMetroNetwork;
import com.net2plan.research.niw.networkModel.WNet;
import com.net2plan.research.niw.networkModel.*;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jxmapviewer.util.GraphicsUtilities;

public class NetworkDiameterCalculator implements IAlgorithm
{

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		
		System.out.println("creating topology");
		
		final String pathExcel = algorithmParameters.get("pathExcel");	

		File excelPath = new File(pathExcel);
		WNet wNet = ImportMetroNetwork.importFromExcelFile(excelPath);		
		netPlan.copyFrom(wNet.getNetPlan());
		//wNet = new WNet(netPlan);
		int networkDiameter = 0;
		
		for(Node originNode : netPlan.getNodes()) 
		{
			for(Node destinationNode : netPlan.getNodes())
			{
				if(originNode != destinationNode)
				{
					List<Link> seqLinks = GraphUtils.getShortestPath(netPlan.getNodes(), netPlan.getLinks(), originNode, destinationNode, null);
					if(seqLinks.size()>=networkDiameter)
						{
						System.out.println("\n");
						System.out.println("################ New candidate to be the network diameter ################");
						System.out.println(seqLinks.size());
						networkDiameter = seqLinks.size();	
						for(Link link : seqLinks)
						{
							System.out.print(link.toString()+" ");
						}
						}
				}
			}
				
		}
		
		return "Network Diameter = " + networkDiameter;
	}

	@Override
	public String getDescription()
	{
		return "Compute the network diameter for a given network with nodes and links.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		final List<Triple<String, String, String>> param = new LinkedList<Triple<String, String, String>> ();

		param.add (Triple.of ("pathExcel" , "MHTopology_Nodes_Links_Small.xlsx" , "Excel file"));
		
		return param;
	}
}