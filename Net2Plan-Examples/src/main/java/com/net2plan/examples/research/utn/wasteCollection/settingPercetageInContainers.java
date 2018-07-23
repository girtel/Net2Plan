package com.net2plan.examples.research.utn.wasteCollection;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Triple;


import java.io.File;
import java.io.FileWriter;


public class settingPercetageInContainers implements IAlgorithm 
{
	private NetPlan netPlan;

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters) 
	{
		int numContainers = 0;
		int fullContainers = 0;
		int notFullContainers = 0;
		double totalWasteReferencia = 0;
		double totalWasteToCollect = 0;
		
		double minPercentageToCollect = Double.parseDouble(algorithmParameters.get("minPercentageToCollect"));
		this.netPlan = netPlan;
		final int Nodes = netPlan.getNumberOfNodes();//N�mero total de nodos
		final int Links = netPlan.getNumberOfLinks();//N�mero total de links
		
		
		
		
		for(Link e:netPlan.getLinks())  // recorres todos los objetos enlace
		{
			if(e.getAttribute("Container").equals("true")) // he puesto como si el atributo que almacenas es SI/NO a que tenga contenedor
			{
				numContainers++;
				Double rndNumber = Math.random()*100;  // generas el n�mero aleatorio, que as� no se hace, es con la librer�a Math.
				totalWasteReferencia = totalWasteReferencia + rndNumber/100*600/1000;
				//System.out.println(rndNumber);
				String PerNum = rndNumber.toString();
		        e.setAttribute("Percentage", PerNum); // cuidado aqu� que �value� debe ser string y lo obtienes como double. Tienes que buscar en la clase Double el m�todo para cambiarlo, que no me acuerdo ahora.
		        if(rndNumber>= minPercentageToCollect)
		        {
		        	e.setAttribute("IsFull", "true"); //Para luego utilizar solo los enlaces >80% para el camino �ptimo
		        	//System.out.println("enlace " + e.getIndex() + " a " + e.getAttribute("IsFull"));
		        	fullContainers ++;
		        	totalWasteToCollect = totalWasteToCollect+rndNumber/100*600/1000;
						
		        }
		        else
		        {
		        	e.setAttribute("IsFull", "false");
		        	//System.out.println("enlace " + e.getIndex() + " a " + e.getAttribute("IsFull"));
		        	notFullContainers ++;
		        }
			}
					}
		
		for(Link e:netPlan.getLinks())  // recorres todos los objetos enlace
		{		
			if(e.getAttribute("Container").equals("true") && e.getAttribute("IsFull").equals("true"))
			{
				Node Dest= e.getDestinationNode();
				Dest.setAttribute("isValid", "true");
			}else{
				Node Dest= e.getDestinationNode();
				Dest.setAttribute("isValid", "false");
			}
		}
				
		try{
			File archive = new File("resultados.txt");
			FileWriter writeFile =new FileWriter(archive,true);
			writeFile.write("Percentage: " + minPercentageToCollect );
			writeFile.write(" FullContainers: " + fullContainers );
			writeFile.write("NotFullContainers: " + notFullContainers);
			writeFile.close();
		}
		catch(Exception e)
		{
			System.out.println("Error al escribir");
		}
		
		
		
		return "TotalWasteReferencia " + totalWasteReferencia  + "totalWasteTrue " + totalWasteToCollect + "Percentage: " + minPercentageToCollect + "TotalLinks: " + Links + "TotalNodes: " + Nodes + "TotalContainers: " + numContainers + " FullContainers: " + fullContainers + "NotFullContainers: " + notFullContainers;
	}

	@Override
	public String getDescription()
	{
		return "This algorithm check if a link has a container. In that case, the algorithm sets a percentage of garbage for each container."
				+ "If percentage is higher or equal 80%, the atribbute isFull is set to true. Otherwise, is set to false. This will give us information"
						+ "about what containers must be visited to collect garbage, trying to optimize the path";
	}

	@Override
	public List<Triple<String, String, String>> getParameters() 
	{
		List<Triple<String, String, String>> algorithmParameters = new LinkedList<Triple<String, String, String>>();
		algorithmParameters.add (Triple.of ("minPercentageToCollect" , "95" , "porcentaje m�nimo de llenado para recogida de residuos"));
//		
		return algorithmParameters;
	}
	
}
