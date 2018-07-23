package com.net2plan.examples.research.utn.wasteCollection;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
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
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.DoubleMatrix3D;

/** This algorithm implements a waste collection scheme. More details in future versions
 *
 */
public class WasteCollectionAlgorithmMultipleTrucks implements IAlgorithm 
{
	public final static String ATTNAME_ISCONTAINER = "Container";
	public final static String ATTNAME_CONTAINERCAPACITY_KG = "containerCapacity_kg";
	public final static String ATTNAME_CONTAINEROCCUPATION_KG = "containerOccupation_kg";
	public final static String ATTNAME_CONTAINERISFULL = "IsFull";
	public final static String ATTNAME_TRUCKNUMCONTAINERSCOLLECTED = "truck_numContainersCollected";
	public final static String ATTNAME_TRUCKSUMCOLLECTEDWASTE_KG = "truck_sumCollectedWaste_kg";
	
	
	private InputParameter indexStartingNode = new InputParameter ("indexStartingNode", (int) 23 , "Index of the node where the truck starts" , 0 , Integer.MAX_VALUE);
	private InputParameter indexEndingNode = new InputParameter ("indexEndingNode", (int) 1 , "Index of the node where the truck ends" , 0 , Integer.MAX_VALUE);
	private InputParameter initializeContainerInfo = new InputParameter ("initializeContainerInfo", true , "Initialize the container information");
	private InputParameter randomNumberSeed = new InputParameter ("randomNumberSeed", (long) 1 , "Seed for the random number generator");
	private InputParameter minPercentageToCollect = new InputParameter ("minPercentageToCollect", (double) 95.0 , "Minimum percentage for considering a full container to be collected" , 0.0 , true , 100.0 , true);
	private InputParameter initializingCapacityOfEachContainer_kg = new InputParameter ("initializingCapacityOfEachContainer_kg", (double) 100.0 , "Value of capacity of each container in kg set when initializing" , 0.0 , true , Double.MAX_VALUE , false);
	private InputParameter truckCollectingCapacity_kg = new InputParameter ("truckCollectingCapacity_kg", (double) 2600.0 , "Capacity of each truck to collect" , 0.0 , true , Double.MAX_VALUE , false);
	private InputParameter maxNumberOfTrucks = new InputParameter ("maxNumberOfTrucks", (int) 10 , "Maximum number of trucks that can be used" , 1 , Integer.MAX_VALUE);
	private InputParameter fixedCostPerUsingATruck = new InputParameter ("fixedCostPerUsingATruck", (double) 1000.0 , "Fixed cost of using a truck, whatever length it traverses" , 0.0 , true , Double.MAX_VALUE , false);
	private InputParameter variableCostPerKmUsingATruck = new InputParameter ("variableCostPerKmUsingATruck", (double) 0.19 , "Cost per km of using a truck" , 00.0 , true , Double.MAX_VALUE , false);
	private InputParameter solverName = new InputParameter ("solverName", "#select# cplex mipcl glpk xpress", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter numberOfRuns = new InputParameter ("numberOfRuns", (int) 1 , "The number of runs of this algorithm to be produced" , 1 , Integer.MAX_VALUE);
	private InputParameter nameOfOutputFolderWithResults = new InputParameter ("nameOfOutputFolderWithResults", "outputFolderWithResults" , "The full path of the folder where result files are stored. The files include 1) inputParameters.txt, 2) outputSummary.txt [row 1: #trucks, row 2: km per truck, row 3: max kg collected per truck, row 4: #containers collected per truck], 3) runN2p_xxx.n2p file are created with the output n2p of each run, where xxx is the run index 0,1,..."); 
	
	
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters) 
	{
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		final File folder = new File (nameOfOutputFolderWithResults.getString());
		if (folder.exists() && folder.isFile()) throw new Net2PlanException ("The folder is a file");
		if (!folder.exists()) folder.mkdirs();
		final List<String> inputParamString = algorithmParameters.entrySet().stream().map(e->e.getKey() + ": " + e.getValue()).collect(Collectors.toList());
		writeFile (new File (folder , "inputParameters.txt") , inputParamString);

		String infoString = "";
		NetPlan thisRun = null;
		final Random rng = new Random (randomNumberSeed.getLong());
		for (int run = 0; run < numberOfRuns.getInt() ; run ++)
		{
			thisRun = this.runExeceution(netPlan , rng);
			final List<String> summaryString = new ArrayList<> ();
			summaryString.add(thisRun.getNumberOfRoutes() + "");
			final int wcPassesInAlink = thisRun.getLinks().stream().mapToInt(e->e.getTraversingRoutesAndMultiplicity().values().stream().mapToInt(ee->(int)ee).sum()).max().orElse(0);
			summaryString.add(wcPassesInAlink + "");
			summaryString.add(thisRun.getRoutes ().stream().map(r->r.getLengthInKm() + "").collect(Collectors.joining(" ")));
			summaryString.add(thisRun.getRoutes ().stream().map(r->r.getAttributeAsDouble(ATTNAME_TRUCKNUMCONTAINERSCOLLECTED, null) + "").collect(Collectors.joining(" ")));
			summaryString.add(thisRun.getRoutes ().stream().map(r->r.getAttributeAsDouble(ATTNAME_TRUCKSUMCOLLECTEDWASTE_KG, null) + "").collect(Collectors.joining(" ")));
			writeFile (new File (folder , "summaryResults_" + run  + ".txt") , summaryString);
			thisRun.saveToFile(new File (folder , "n2pFile_" + run + ".n2p"));
			final double totalKm = netPlan.getRoutes().stream().mapToDouble(r->r.getLengthInKm()).sum();
			final double totalWasteCollected = thisRun.getRoutes ().stream().mapToDouble(r->r.getAttributeAsDouble(ATTNAME_TRUCKSUMCOLLECTEDWASTE_KG, 0.0)).sum();
			infoString = "Used trucks = " + thisRun.getNumberOfRoutes() + ", total km: " + totalKm + ", total waste collected: " + totalWasteCollected;
		}
		if (thisRun != null) netPlan.assignFrom(thisRun); // we return the last run
		return infoString;
	}

	@Override
	public String getDescription()
	{
		return "This algorithm computes the optimum number of trucks and their routes to minimize the total cost, for collecting wastes in a city. "
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
	
	private NetPlan runExeceution (NetPlan origNp , Random rng)
	{
		final NetPlan netPlan = origNp.copy();
		
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
		if (totalWasteToCollect > truckCollectingCapacity_kg.getDouble() * maxNumberOfTrucks.getInt())
			throw new Net2PlanException ("More than " + maxNumberOfTrucks.getInt () + " trucks are needed: The truck capacity is " + truckCollectingCapacity_kg.getDouble() + " kgs, and we have to collect " + totalWasteToCollect + " kgs, and we have " + maxNumberOfTrucks.getInt() + " trucks");
		
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
			
		final int D = netPlan.getNumberOfDemands();
		final int T = maxNumberOfTrucks.getInt();
		OptimizationProblem op = new OptimizationProblem();
		
		op.addDecisionVariable("y_t", true, new int[]{1,T}, 0, 1); // 1 if truck t is being used
		op.addDecisionVariable("x_te", true, new int[]{T,E}, 0, Double.MAX_VALUE); // number of passes of truck t in link e
		op.addDecisionVariable("w_te", true, new int[]{T,E}, 0, 1); // if the truck t collects in link e
		op.addDecisionVariable("x_tde", true, new int[]{T,D,E}, 0, 1); // if the truck t, passes through link e in its shortest path from the origin of the truck, to the origin of the link e 
		
		
		final double [] x_eprima = new double [E]; for (Link e : candidateLinks) x_eprima [e.getIndex()] = 1;
		op.setInputParameter("A_nd", netPlan.getMatrixNodeDemandIncidence()); /* 1 in position (n,d) if demand d starts in n, -1 if it ends in n, 0 otherwise */
		op.setInputParameter("A_ne", netPlan.getMatrixNodeLinkIncidence()); /* 1 in position (n,e) if link e starts in n, -1 if it ends in n, 0 otherwise */
		op.setInputParameter("d_e", netPlan.getVectorLinkLengthInKm(), "row");
		op.setInputParameter("x_eprima", x_eprima , "row");
		op.setInputParameter("cf", fixedCostPerUsingATruck.getDouble());
		op.setInputParameter("cvPerKm", variableCostPerKmUsingATruck.getDouble());
		final double [] flowConservTruck_n = new double [N];
		flowConservTruck_n [startingNode.getIndex()]  = 1;
		flowConservTruck_n [endingNode.getIndex()]  = -1;
		op.setInputParameter("fcTruck", flowConservTruck_n , "row");
		op.setInputParameter("onesD", DoubleFactory1D.dense.make(D , 1.0) , "row");
		op.setInputParameter("onesE", DoubleFactory1D.dense.make(E , 1.0) , "row");
		op.setInputParameter("onesT", DoubleFactory1D.dense.make(T , 1.0) , "row");
		op.setInputParameter("BIGNUMBER", T * E);
		op.setInputParameter("TRUCKCAP", truckCollectingCapacity_kg.getDouble());
		final List<Double> capacityInKgPerContainer_e = netPlan.getLinks().stream().map(e->e.getAttributeAsDouble(ATTNAME_CONTAINERCAPACITY_KG, 0.0)).collect(Collectors.toList()); 
		op.setInputParameter("u_e", capacityInKgPerContainer_e , "row");
		
		op.setObjectiveFunction("minimize", "cvPerKm * sum (x_te * d_e') + cf * sum (y_t)");

		op.addConstraint("x_te <= BIGNUMBER * y_t' * onesE"); /* No truck => no passes in any link */
		op.addConstraint("w_te <= x_te"); /* Not passes thorough a link => cannot collect from that link */
		for (int d = 0; d < D ; d ++)
			op.addConstraint("sum (x_tde(all , " + d + ",all),2) <= BIGNUMBER * x_te"); /* Truck does not pass => not usable for continuity check */
		
		for (int t = 0 ;  t < T ; t ++)
		{
			op.setInputParameter("t", t);

			op.addConstraint("A_ne * (x_te(t,all))' == fcTruck' * y_t(t)"); /* the flow-conservation constraints for each truck in the links */
			op.addConstraint("A_ne * (sum(x_tde(t,all,all),1))' == y_t(t) * A_nd"); /* the flow-conservation constraints (NxD constraints) */
			op.addConstraint("sum(x_tde(t,all,all),1) <= onesD' * x_te(t,all)"); /* If the link is not in the path, cannot be traversed by the demands */
		}
		op.addConstraint("sum(w_te , 1) == x_eprima"); /* Exactly one truck collects each container */
		
		op.addConstraint("w_te * u_e' <= y_t' * TRUCKCAP");
		
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");

		final DoubleMatrix1D y_t = op.getPrimalSolution("y_t").view1D();
		final DoubleMatrix2D x_te = op.getPrimalSolution("x_te").view2D();
		final DoubleMatrix2D w_te = op.getPrimalSolution("w_te").view2D();
		final DoubleMatrix3D x_tde = op.getPrimalSolution("x_tde").view3D("sparse");
		double totalCost = op.getOptimalCost();

		netPlan.removeAllDemands();
		
		/* Checks */
		for (int t = 0; t < T ; t ++)
		{
			final boolean truckExists = y_t.get(t) > 1e-3;
			if (!truckExists)
			{
				if (x_te.viewRow(t).zSum() > 1e-3) throw new RuntimeException ();
				if (w_te.viewRow(t).zSum() > 1e-3) throw new RuntimeException ();
				if (x_tde.viewSlice(t).zSum() > 1e-3) throw new RuntimeException ("x_tde.viewSlice(t): " + x_tde.viewSlice(t) + ", x_tde rows: " + x_tde.rows() + ", cols: " + x_tde.columns() + ", slices: " + x_tde.slices());
				double wasteCollected = 0;
				for (int e = 0; e < E ; e ++) wasteCollected += w_te.get(t, e) * netPlan.getLink(e).getAttributeAsDouble(ATTNAME_CONTAINERCAPACITY_KG, 0.0);
				if (wasteCollected > truckCollectingCapacity_kg.getDouble()) throw new RuntimeException ();
					
			} else
			{
				if (x_te.viewRow(t).zSum() < 1e-3) throw new RuntimeException ();
				if (w_te.viewRow(t).zSum() < 1e-3) throw new RuntimeException ();
			}
			for (int e = 0; e < E ; e ++)
			{
				if (x_te.get(t, e) < 1e-3 && w_te.get(t, e) > 1e-3) throw new RuntimeException (); // cannot collect e if not traversing e
			}			
		}
		
		/* container collected or not */
		for (int e = 0; e < E ; e ++)
		{
			final Link link = netPlan.getLink(e);
			if (candidateLinks.contains(link))
			{
				if (Math.abs(w_te.viewColumn(e).zSum() - 1.0) > 1e-3) throw new RuntimeException ();
			}
			else
			{
				if (Math.abs(w_te.viewColumn(e).zSum() - 0.0) > 1e-3) throw new RuntimeException ();
			}
		}
		
		/* Check get the path */
		for (int t = 0 ; t < T ; t ++)
		{
			if (y_t.get(t) < 1e-3) continue;

			final SortedMap<Link,Integer> truckPasses = new TreeMap<> ();
			final List<Link> sequenceOfLinksThisTruck = new ArrayList<> ();
			for (int e = 0; e < E ; e ++) if (x_te.get(t, e) > 1e-3) incremMap (truckPasses , netPlan.getLink(e) , (int) x_te.get(t, e));
			final SortedMap<Link,Integer> truckPassesPending = new TreeMap<> (truckPasses);
			Node currentNode = startingNode;
			System.out.println("truckPassesPending: " + truckPassesPending);
			System.out.println("sequenceOfLinks this truck: " + sequenceOfLinksThisTruck);
			while (!truckPassesPending.isEmpty())
			{
				final Node currentNodeCopy = currentNode;
				final SortedSet<Link> outLinksCurrentNode = truckPassesPending.keySet().stream().filter(e->e.getOriginNode().equals(currentNodeCopy)).collect(Collectors.toCollection(TreeSet::new));
				final SortedSet<Link> inLinksCurrentNode = truckPassesPending.keySet().stream().filter(e->e.getDestinationNode().equals(currentNodeCopy)).collect(Collectors.toCollection(TreeSet::new));
				assert outLinksCurrentNode.size() == inLinksCurrentNode.size() + 1;
				Link linkToAddToPath = null;
				if (inLinksCurrentNode.isEmpty()) // don't have to come back to this node
				{
					assert outLinksCurrentNode.size() == 1;
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
				sequenceOfLinksThisTruck.add(linkToAddToPath);
				incremMap(truckPassesPending, linkToAddToPath, -1);
				currentNode = linkToAddToPath.getDestinationNode();
			}
			assert currentNode.equals(endingNode);

			final Demand dTruck = netPlan.addDemand(startingNode, endingNode, 1.0, RoutingType.SOURCE_ROUTING, null);
			final Route truckRoute = netPlan.addRoute(dTruck, 1.0, 1.0, sequenceOfLinksThisTruck, null);
			assert Math.abs(totalCost - truckPasses.entrySet().stream().mapToDouble(e->e.getKey().getLengthInKm() * e.getValue()).sum()) <= 1e-3; 

			double wasteCollectedThisTruck = 0; for (int e = 0; e < E ; e ++) wasteCollectedThisTruck += w_te.get(t, e) * netPlan.getLink(e).getAttributeAsDouble(ATTNAME_CONTAINEROCCUPATION_KG, 0.0);
			
			truckRoute.setAttribute(ATTNAME_TRUCKNUMCONTAINERSCOLLECTED, (int) w_te.viewRow(t).zSum());
			truckRoute.setAttribute(ATTNAME_TRUCKSUMCOLLECTEDWASTE_KG,wasteCollectedThisTruck);
		}
		final double numUsedTrucks = y_t.zSum();
		if (numUsedTrucks != netPlan.getNumberOfRoutes()) throw new RuntimeException ();
		final double totalKm = x_te.zMult(netPlan.getVectorLinkLengthInKm() , null).zSum();
		if (Math.abs (totalKm - netPlan.getRoutes().stream().mapToDouble(r->r.getLengthInKm()).sum()) > 1e-3) throw new RuntimeException ();
		
		return netPlan;
	}
	
	private static void writeFile (File file , List<String> rows)
	{
		PrintWriter of = null;
		try 
		{ 
			of = new PrintWriter (new FileWriter (file));
			for (String row : rows)
				of.println(row);
			of.close();
		} catch (Exception e) { e.printStackTrace(); if (of != null) of.close(); throw new Net2PlanException ("File error"); }
		
	}
	
}
