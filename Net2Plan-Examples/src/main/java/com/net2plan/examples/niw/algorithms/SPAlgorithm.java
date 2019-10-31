package com.net2plan.examples.niw.algorithms;

import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.research.niw.networkModel.ImportMetroNetwork;
import com.net2plan.research.niw.networkModel.WNet;
import com.net2plan.research.niw.networkModel.*;
import com.net2plan.utils.Triple;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SPAlgorithm implements IAlgorithm
{

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		
		System.out.println("creating topology");
		
		final String pathExcel = algorithmParameters.get("pathExcel");	

		File excelPath = new File(pathExcel);
		WNet wNet = ImportMetroNetwork.importFromExcelFile(excelPath);		
		netPlan.copyFrom(wNet.getNetPlan());
		wNet = new WNet(netPlan);
		
		netPlan.removeAllLinks();
		
		System.out.println("####################################### RUNNING ALGORITHM #######################################");
		
		for (WNode node : wNet.getNodes())
		{
			if(node.getType().equals("AMEN"))
			{
				double distance = Double.MAX_VALUE;
				WNode finalNode = null;
				
				for (WNode candidateNode : wNet.getNodes())
				{
					if(candidateNode.getType().equals("MCEN")) 
					{
						double finalDistance = netPlan.getNodePairHaversineDistanceInKm(node.getNe(), candidateNode.getNe());

						if(finalDistance < distance) 
						{
							finalNode = candidateNode.getAsNode();
							System.out.println(finalNode);
							distance = finalDistance;
							System.out.println(distance);

						}
					
					}
				}
				wNet.addIpLink(node, finalNode, 0.0, false);
				finalNode.setPoputlation(finalNode.getPopulation()+node.getPopulation());
			}
			
		}
		
		return "Excel file has been loaded successfully";
	}

	@Override
	public String getDescription()
	{
		return "Test Excel file";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		final List<Triple<String, String, String>> param = new LinkedList<Triple<String, String, String>> ();

		param.add (Triple.of ("pathExcel" , "MetroNetwork_SmallExample.xlsx" , "Excel file"));
		
		return param;
	}
}
