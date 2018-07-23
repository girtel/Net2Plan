// 


// PABLO: NEED TO PUT THE CAPACITIES IN THE LINKS, AND TO ADD THE VIRTUAL LINKS BEFORE I CAN SOLVE THIS. IT WILL TAKE TIME!!!
// lA VELOCIDAD LIBRE ES EL CAMPO V0PRT
// METER EL LINKPOLY


package com.net2plan.examples.smartCity.utn;

import java.util.List;
import java.util.Map;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

/** This is a template to be used in the lab work, a starting point for the students to develop their programs
 * 
 */
public class UTNNguyenDupuisNetworkCreator implements IAlgorithm
{
	/** The method called by Net2Plan to run the algorithm (when the user presses the "Execute" button)
	 * @param netPlan The input network design. The developed algorithm should modify it: it is the way the new design is returned
	 * @param algorithmParameters Pair name-value for the current value of the input parameters
	 * @param net2planParameters Pair name-value for some general parameters of Net2Plan
	 * @return
	 */
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Typically, you start reading the input parameters */
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		/* Create the nodes */
		netPlan.removeAllNodes();
		netPlan.addNode(0 , 2 , "Node 0" , null);
		netPlan.addNode(5 , 1 , "Node 1" , null);
		netPlan.addNode(5 , 0 , "Node 2" , null);
		netPlan.addNode(1 , 0 , "Node 3" , null);
		netPlan.addNode(2 , 1 , "Node 4" , null);
		netPlan.addNode(3 , 2 , "Node 5" , null);
		netPlan.addNode(4 , 2 , "Node 6" , null);
		netPlan.addNode(5 , 3 , "Node 7" , null);
		netPlan.addNode(3 , 0 , "Node 8" , null);
		netPlan.addNode(3 , 1 , "Node 9" , null);
		netPlan.addNode(4 , 1 , "Node 10" , null);
		netPlan.addNode(1 , 3 , "Node 11" , null);
		netPlan.addNode(4 , 0 , "Node 12" , null);
		
		/*  orginNode, destNode, ca , Qa , prior flow */
		final double [][] linkInformation = new double [][] { 		
		{1,	5,	7,	700,	507}, 
		{1,	12,	9,	560,	700}, 
		{2,	8,	9,	700,	348}, 
		{2,	11,	9,	280,	298}, 
		{3,	11,	9,	560,	380}, 
		{3,	13,	11,	560,	333}, 
		{4,	5,	12,	560,	220}, 
		{4,	9,	5,	375,	495}, 
		{5,	1,	7,	700,	512}, 
		{5,	4,	12,	560,	192}, 
		{5,	6,	12,	420,	475}, 
		{5,	9,	9,	420,	252}, 
		{6,	5,	12,	420,	450}, 
		{6,	7,	5,	700,	473}, 
		{6,	10,	4,	280,	177}, 
		{6,	12,	6,	140,	193}, 
		{7,	6,	5,	700,	461}, 
		{7,	8,	9,	700,	402}, 
		{7,	11,	4,	700,	214}, 
		{8,	2,	9,	700,	376}, 
		{8,	7,	9,	700,	407}, 
		{8,	12,	14,	560,	593}, 
		{9,	4,	5,	375,	520}, 
		{9,	5,	9,	420,	254}, 
		{9,	10,	5,	280,	412}, 
		{9,	13,	9,	280,	337}, 
		{10,	6,	4,	280,	181}, 
		{10,	9,	4,	280,	441}, 
		{10,	11,	4,	700,	589}, 
		{11,	2,	9,	280,	301}, 
		{11,	3,	8,	560,	358}, 
		{11,	7,	4,	700,	197}, 
		{11,	10,	4,	700,	623}, 
		{12,	1,	9,	560,	610}, 
		{12,	6,	7,	140,	174}, 
		{12,	8,	14,	560,	709}, 
		{13,	3,	11,	560,	337}, 
		{13,	9,	9,	280,	333}};
		final int E = linkInformation.length;
		final double c_a [] = new double [E];
		final double q_a [] = new double [E];
		final double priorFlow_a [] = new double [E];
		for (int e = 0; e < E ; e ++)
		{
			final double [] linkInfo = linkInformation [e];
			final Node s = netPlan.getNode((int) linkInfo [0]-1);
			final Node t = netPlan.getNode((int) linkInfo [1]-1);
			c_a [e] = linkInfo [2];
			q_a [e] = linkInfo [3];
			final Link link = netPlan.addLink(s , t , q_a [e] , netPlan.getNodePairEuclideanDistance(s,t) , 50 , null);
			priorFlow_a [e] = linkInfo [4];
			link.setAttribute(UTNConstants.ATTRNAME_C0A , "" + c_a [e]);
			link.setAttribute(UTNConstants.ATTRNAME_MONITOREDVEHICUCLERATE , "" + priorFlow_a [e]);
		}
		
		/*  */
		final double [][] demandInformation = new double [][] { 		
		{1,	2,	210,	260},
		{1,	3,	130,	517},
		{1,	8,	320,	430},
		{2,	1,	210,	235},
		{2,	4,	320,	353},
		{2,	12,	50,	58},
		{3,	1,	430,	516},
		{3,	4,	110,	144},
		{3,	12,	40,	52},
		{4,	2,	320,	359},
		{4,	3,	110,	131},
		{4,	8,	210,	228},
		{8,	1,	320,	370},
		{8,	4,	210,	215},
		{8,	12,	60,	66},
		{12,	2,	50,	59},
		{12,	3,	40,	47},
		{12,	8,	60,	78}};
		final int D = demandInformation.length;
		for (int d = 0; d < D ; d ++)
		{
			final double [] demandInfo = demandInformation [d];
			final Node s = netPlan.getNode((int) demandInfo [0]-1);
			final Node t = netPlan.getNode((int) demandInfo [1]-1);
			final Demand demand = netPlan.addDemand(s , t , demandInfo [2] , RoutingType.SOURCE_ROUTING ,  null);
			demand.setAttribute(UTNConstants.ATTRNAME_MONITOREDVEHICUCLERATE , "" + demandInfo [3]);
		}
		
		
		return "Ok!"; // this is the message that will be shown in the screen at the end of the algorithm
	}

	/** Returns a description message that will be shown in the graphical user interface
	 */
	@Override
	public String getDescription()
	{
		return "This algorithm creates the NguyenDupuis topology for urban traffic tests";
	}

	
	/** Returns the list of input parameters of the algorithm. For each parameter, you shoudl return a Triple with its name, default value and a description
	 * @return
	 */
	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
}
