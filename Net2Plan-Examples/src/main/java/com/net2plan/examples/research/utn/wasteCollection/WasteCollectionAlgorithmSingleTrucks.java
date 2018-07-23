package com.net2plan.examples.research.utn.wasteCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

public class WasteCollectionAlgorithmSingleTrucks implements IAlgorithm 
{
	public static String ATTNAME_ISCONTAINER = "Container";
	public static String ATTNAME_CONTAINERCAPACITY_KG = "containerCapacity_kg";
	public static String ATTNAME_CONTAINEROCCUPATION_KG = "containerOccupation_kg";
	public static String ATTNAME_CONTAINERISFULL = "IsFull";
	
	private InputParameter indexStartingNode = new InputParameter ("indexStartingNode", (int) 23 , "Index of the node where the truck starts" , 0 , Integer.MAX_VALUE);
	private InputParameter indexEndingNode = new InputParameter ("indexEndingNode", (int) 1 , "Index of the node where the truck ends" , 0 , Integer.MAX_VALUE);
	private InputParameter initializeContainerInfo = new InputParameter ("initializeContainerInfo", true , "Initialize the container information");
	private InputParameter randomNumberSeed = new InputParameter ("randomNumberSeed", (long) 1 , "Seed for the random number generator");
	private InputParameter minPercentageToCollect = new InputParameter ("minPercentageToCollect", (double) 95.0 , "Minimum percentage for considering a full container to be collected" , 0.0 , true , 100.0 , true);
	private InputParameter initializingCapacityOfEachContainer_kg = new InputParameter ("initializingCapacityOfEachContainer_kg", (double) 100.0 , "Value of capacity of each container in kg set when initializing" , 0.0 , true , Double.MAX_VALUE , false);
	private InputParameter truckCollectingCapacity_kg = new InputParameter ("truckCollectingCapacity_kg", (double) 26000.0 , "Capacity of each truck to collect" , 0.0 , true , Double.MAX_VALUE , false);
	private InputParameter solverName = new InputParameter ("solverName", "#select# cplex mipcl glpk xpress", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters) 
	{
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		final int N = netPlan.getNumberOfNodes();
		final int E = netPlan.getNumberOfLinks();
		if (indexStartingNode.getInt() >= N) throw new Net2PlanException("Wrong node index");
		if (indexEndingNode.getInt() >= N) throw new Net2PlanException("Wrong node index");

		netPlan.removeAllUnicastRoutingInformation();
		
		final SortedSet<Node> originNodesToCandidateLink= new TreeSet<>();
		final SortedSet<Node> destinationNodesToCandidateLink= new TreeSet<>();
		final SortedSet<Link> candidateLinks= new TreeSet<Link>();
		final Node startingNode = netPlan.getNode(indexStartingNode.getInt());
		final Node endingNode = netPlan.getNode(indexEndingNode.getInt());

		/* If asked to initialize the topology */
		if (initializeContainerInfo.getBoolean())
		{
			final Random rng = new Random (randomNumberSeed.getLong());
			final List<Link> containers = netPlan.getLinks().stream().filter(e->e.getAttribute(ATTNAME_ISCONTAINER).equals("true")).collect(Collectors.toList());
			for (Link container : containers)
			{
				final double capacityInKg = container.getAttributeAsDouble(ATTNAME_CONTAINERCAPACITY_KG, initializingCapacityOfEachContainer_kg.getDouble());
				container.setAttribute(ATTNAME_CONTAINERCAPACITY_KG, capacityInKg);
				container.setAttribute(ATTNAME_CONTAINEROCCUPATION_KG, 0.0);
				container.setAttribute(ATTNAME_CONTAINERISFULL, "false");
				final double currentOccupationPercentage = 100 * rng.nextDouble();
				if (currentOccupationPercentage < minPercentageToCollect.getDouble()) continue; 
				container.setAttribute(ATTNAME_CONTAINERISFULL, "true");
				container.setAttribute(ATTNAME_CONTAINEROCCUPATION_KG, currentOccupationPercentage * capacityInKg / 100.0);
			}
		}
		final double totalWasteToCollect = netPlan.getLinks().stream().mapToDouble(e->e.getAttributeAsDouble(ATTNAME_CONTAINEROCCUPATION_KG, 0.0)).sum();
		if (totalWasteToCollect > truckCollectingCapacity_kg.getDouble())
			throw new Net2PlanException ("More than one truck is needed: The truck capacity is " + truckCollectingCapacity_kg.getDouble() + " kgs, and we have to collect " + totalWasteToCollect + " kgs");
		
		/* Build information about the links you are forced to pass */
		for(Link e:netPlan.getLinks())  
		{
			if (e.getAttribute(ATTNAME_CONTAINERISFULL).equalsIgnoreCase("true"))
			{
				candidateLinks.add(e);
				originNodesToCandidateLink.add(e.getOriginNode());
				destinationNodesToCandidateLink.add(e.getDestinationNode());
				if(!startingNode.equals(e.getDestinationNode()))
					netPlan.addDemand(startingNode, e.getDestinationNode(), 1, RoutingType.SOURCE_ROUTING , null);
			}
		}
			
		// varias formas de sacar las demandas ofrecidas
		final int D = netPlan.getNumberOfDemands();
		OptimizationProblem op = new OptimizationProblem();
		
		op.addDecisionVariable("x_e", true, new int[]{1,E}, 0, Double.MAX_VALUE);
		op.addDecisionVariable("x_de", true, new int[]{D,E}, 0, 1);
		
		op.setInputParameter("A_nd", netPlan.getMatrixNodeDemandIncidence()); /* 1 in position (n,d) if demand d starts in n, -1 if it ends in n, 0 otherwise */
		op.setInputParameter("A_ne", netPlan.getMatrixNodeLinkIncidence()); /* 1 in position (n,e) if link e starts in n, -1 if it ends in n, 0 otherwise */
		op.setInputParameter("d_e", netPlan.getVectorLinkLengthInKm(), "row");
		op.setInputParameter("Eprima", candidateLinks.stream().map(e->e.getIndex()).collect(Collectors.toList()), "row");

		// Minimize the traversed distance
		op.setObjectiveFunction("minimize", "sum(x_e.*d_e)");

		final double [] flowConservTruck_n = new double [N];
		flowConservTruck_n [startingNode.getIndex()]  = 1;
		flowConservTruck_n [endingNode.getIndex()]  = -1;
		op.setInputParameter("fcTruck", flowConservTruck_n , "row");
		op.setInputParameter("onesD", DoubleFactory1D.dense.make(D , 1.0) , "row");
		
		op.addConstraint("x_e (Eprima) >= 1"); /* You have to pass through the needed links */
		op.addConstraint("A_ne * (x_e') == fcTruck'"); /* the flow-conservation constraints for the truck in the links */
		op.addConstraint("A_ne * (x_de') == A_nd"); /* the flow-conservation constraints (NxD constraints) */
		op.addConstraint("x_de <= onesD' * x_e"); /* If the link is not in the path, cannot be traversed by the demands */

		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		
		final double [] x_e = op.getPrimalSolution("x_e").to1DArray();
		final DoubleMatrix2D x_de = op.getPrimalSolution("x_de").view2D();
		//netPlan.setRoutingFromDemandLinkCarriedTraffic(x_de , false , false);
		double totalCost = op.getOptimalCost();

		/* Checks */
		for (int e = 0; e < E ; e ++)
		{
			final Link link = netPlan.getLink(e);
			final int truckNumPasses = (int) Math.round(x_e [e]); 
			if (truckNumPasses == 0)
			{
				if (candidateLinks.contains(link)) throw new RuntimeException ();
				if (x_de.viewColumn(e).zSum() > 1e-3) throw new RuntimeException ();
			}
			else if (truckNumPasses >= 1)
			{
			} else throw new RuntimeException ("Bad");
		}
		
		/* Check get the path */
		final List<Link> sequenceOfLinks = new ArrayList<> ();
		final SortedMap<Link,Integer> truckPasses = new TreeMap<> ();
		for (int e = 0; e < E ; e ++) if (x_e [e] > 1e-3) incremMap (truckPasses , netPlan.getLink(e) , (int) x_e [e]);
		final SortedMap<Link,Integer> truckPassesPending = new TreeMap<> (truckPasses);
		Node currentNode = startingNode;
		System.out.println("truckPassesPending: " + truckPassesPending);
		System.out.println("sequenceOfLinks: " + sequenceOfLinks);
		while (!truckPassesPending.isEmpty())
		{
			final Node currentNodeCopy = currentNode;
			final SortedSet<Link> outLinksCurrentNode = truckPassesPending.keySet().stream().filter(e->e.getOriginNode().equals(currentNodeCopy)).collect(Collectors.toCollection(TreeSet::new));
			final SortedSet<Link> inLinksCurrentNode = truckPassesPending.keySet().stream().filter(e->e.getDestinationNode().equals(currentNodeCopy)).collect(Collectors.toCollection(TreeSet::new));
			if (outLinksCurrentNode.size() != inLinksCurrentNode.size() + 1) throw new RuntimeException ();
			Link linkToAddToPath = null;
			if (inLinksCurrentNode.isEmpty()) // don't have to come back to this node
			{
				if (outLinksCurrentNode.size() != 1) throw new RuntimeException ();
				linkToAddToPath = outLinksCurrentNode.iterator().next();
			}
			else // still have to come back to this node at least once
			{
				for (Link outLink : outLinksCurrentNode)
				{
					final Node nextNode = outLink.getDestinationNode();
					final List<Link> linksUsable = truckPassesPending.keySet().stream().filter(e->!e.equals(outLink)).collect(Collectors.toList());
					final List<Link> pathComingBackToCurrentNode = GraphUtils.getShortestPath(netPlan.getNodes(), linksUsable , nextNode, currentNode, null);
					if (pathComingBackToCurrentNode != null) if (!pathComingBackToCurrentNode.isEmpty())
					{
						linkToAddToPath = outLink; break;
					}
				}
			}
			
			if (linkToAddToPath == null) throw new RuntimeException ("I could not make a path");
			sequenceOfLinks.add(linkToAddToPath);
			incremMap(truckPassesPending, linkToAddToPath, -1);
			currentNode = linkToAddToPath.getDestinationNode();
		}
		if (!currentNode.equals(endingNode)) throw new RuntimeException ();

		netPlan.removeAllDemands();
		final Demand dTruck = netPlan.addDemand(startingNode, endingNode, 1.0, RoutingType.SOURCE_ROUTING, null);
		netPlan.addRoute(dTruck, 1.0, 1.0, sequenceOfLinks, null);

		if (Math.abs(totalCost - truckPasses.entrySet().stream().mapToDouble(e->e.getKey().getLengthInKm() * e.getValue()).sum()) > 1e-3) throw new RuntimeException (); 
		
		return "totalCost: " + totalCost ;

	}

	@Override
	public String getDescription()
	{
		return "This algorithm computes the optimum route of a truck to minimize the total distance traversed, for collecting wastes in a city. "
				+ "The city roads are links, and the city interconnections are nodes. Wastes are collected from containers that are placed in links, with a capacity and occupied capacity in kg of waste. "
				+ "Trucks have a maximum capacity. Only containers with an occupied capacity over a threshold are selected for being collected. ";
	}

	@Override
	public List<Triple<String, String, String>> getParameters() 
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
	
	private static <T> void incremMap (Map<T,Integer> map , T key , int increm)
	{
		if (map.containsKey(key))
		{
			final int newValue = increm + map.get(key);
			if (newValue == 0) map.remove(key); else map.put(key, newValue);
		}
		else if (increm != 0)  map.put(key, increm);
	}
	
	
}
