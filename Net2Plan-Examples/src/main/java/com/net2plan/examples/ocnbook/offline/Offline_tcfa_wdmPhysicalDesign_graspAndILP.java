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




package com.net2plan.examples.ocnbook.offline;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Constants.SearchType;
import com.net2plan.utils.*;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

/**
 * This algorithm is devoted to solve the several network planning problems in an optical WDM network (fiber placement, RWA, under different recovery schemes), appearing in the case study in the book section mentioned below. 
 * @net2plan.description 
 * @net2plan.keywords WDM, Topology assignment (TA), Flow assignment (FA), GRASP, JOM
 * @net2plan.ocnbooksections Section 12.10
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_tcfa_wdmPhysicalDesign_graspAndILP implements IAlgorithm
{
	/* Stat */
	private double stat_totalCost , stat_costinks , stat_costCirc , stat_costNodes;
	private int stat_numLinks , stat_numCirc , stat_numNodesDeg2 , stat_numNodesDeg3;
	private int stat_numItGRASP , stat_numLSIterationsReducingCost , stat_numSolverCalls , stat_numSolverCallsOk;
	private double stat_totalTimeSecs , stat_totalTimeCreateProgramsSecs , stat_totalTimeSolverSecs;
	
	/* Parameters */
	private DoubleMatrix1D linkCost_e;
	private Random rng;
	private NetPlan netPlan;
	
	/* Control */
	private int Efm, N, nSRGs , D , R; 
	private DoubleMatrix1D numCircH_d; 
	private DoubleMatrix2D A_er , A_dr , A_rs , A_se , Aout_ne , Ain_ne, Abid_dd , Abid_rr , Abid_ee;
	private Map<Demand,Demand> opposite_d;
	private Map<Route,Route> opposite_r;
	private int [][] routeList_d; // for each demand, the array of route ids assigned to it
	private int [][] traversedLinks_r; // for each demand, the array of route ids assigned to it

	
	private InputParameter algorithm_randomSeed = new InputParameter ("algorithm_randomSeed", (long) 1 , "Seed of the random number generator");
	private InputParameter algorithm_outputFileNameRoot = new InputParameter ("algorithm_outputFileNameRoot", "tcfaWDM_graspAndILP" , "Root of the file name to be used in the output files. If blank, no output");
	private InputParameter algorithm_maxExecutionTimeInSeconds = new InputParameter ("algorithm_maxExecutionTimeInSeconds", (double) 300 , "Algorithm maximum running time in seconds" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter tcfa_circuitCost = new InputParameter ("tcfa_circuitCost", (double) 3.0 , "Cost of each circuit" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter tcfa_circuitCapacity_Gbps = new InputParameter ("tcfa_circuitCapacity_Gbps", (double) 10.0 , "Capacity of an optical circuit" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter tcfa_srgType = new InputParameter ("tcfa_srgType" , "#select# perBidirectionalLinkBundle noFailure", "Determines how the SRGs are initialized. The design must be tolerant to single SRG failures");
	private InputParameter tcfa_srgMttfPer1000Km_hrs = new InputParameter ("tcfa_srgMttfPer1000Km_hrs" , (double) 4380 , "Mean Time To Fail (MTTF) of the SRGs defined is this value multiplied by the link length divided by 1000",  0 , false , Double.MAX_VALUE , true);
	private InputParameter tcfa_srgMttr_hrs = new InputParameter ("tcfa_srgMttr_hrs" , (double) 24 , "Mean Time To Repair (MTTR) of the SRGs",  0 , true , Double.MAX_VALUE , true);
	private InputParameter tcfa_recoveryType = new InputParameter ("tcfa_recoveryType" , "#select# 1+1 shared restoration", "Determines the type of network recovery mechanism in the network.");
	private InputParameter tcfa_maxPathLengthInKm = new InputParameter ("tcfa_maxPathLengthInKm" , (double) 10000 , "Maximum length that a lightpath can have",  0 , true , Double.MAX_VALUE , true);
	private InputParameter tcfa_maxPathNumberOfHops = new InputParameter ("tcfa_maxPathNumberOfHops" , (int) 4 , "Maximum number of hops that a lightpath can make",  1 , Integer.MAX_VALUE);
	private InputParameter tcfa_maxNumberPathsPerDemand = new InputParameter ("tcfa_maxNumberPathsPerDemand" , (int) 200 , "Maximum number of paths in the candidate path list, for each demand",  1 , Integer.MAX_VALUE);
	private InputParameter tcfa_linkCapacity_numCirc = new InputParameter ("tcfa_linkCapacity_numCirc" , (int) 80 , "Number of wavelengths available per fiber",  1 , Integer.MAX_VALUE);
	private InputParameter tcfa_linkCostPerKm = new InputParameter ("tcfa_linkCostPerKm" , (double) 0.4 , "Cost of one km of optical fiber",  0 , true  , Double.MAX_VALUE , true);
	private InputParameter tcfa_costNodeDegree2 = new InputParameter ("tcfa_costNodeDegree2" , (double) 4.0 , "Cost of one OADM of degree lower or equal than two",  0 , true  , Double.MAX_VALUE , true);
	private InputParameter tcfa_costNodeDegreeHigherThan2 = new InputParameter ("tcfa_costNodeDegreeHigherThan2" , (double) 4.0 , "Cost of one OADM of degree higher than two",  0 , true  , Double.MAX_VALUE , true);
	private InputParameter tcfa_networkDiameterKmToNormalize = new InputParameter ("tcfa_networkDiameterKmToNormalize" , (double) 4500 , "The link lengths are normalized so the longest link has this value",  0 , true  , Double.MAX_VALUE , true);
	private InputParameter tcfa_maxNumIterations = new InputParameter ("tcfa_maxNumIterations", (int) 10000 , "Maximum number of iterations" , 1 , Integer.MAX_VALUE);
	private InputParameter algorithm_solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter algorithm_solverLibraryName = new InputParameter ("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter algorithm_maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) 2 , "Maximum time granted to the solver in to solve each problem instance. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter tcfa_bidirectionalLinks = new InputParameter ("tcfa_bidirectionalLinks", true , "If true, the topology of fibers deployed is constrained to be bidirectional (the same number of fibers in both directions)");

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		if (netPlan.getNumberOfNodes() == 0) throw new Net2PlanException ("The input design has no nodes");
		
		
		try{ 
		this.netPlan = netPlan;
		/* Initializations */

		netPlan.removeAllLinks(); netPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
		this.N = netPlan.getNumberOfNodes();
		this.rng = new Random (algorithm_randomSeed.getLong ());
		
		/* Initialize links */
		for (Node n1 : netPlan.getNodes()) for (Node n2 : netPlan.getNodes()) if (n1 != n2) netPlan.addLink(n1, n2, 0.01, netPlan.getNodePairEuclideanDistance(n1, n2), 200000 , null);
		final double linkLengthKmFromEuclideanDistanceFactor = tcfa_networkDiameterKmToNormalize.getDouble() / netPlan.getVectorLinkLengthInKm().getMaxLocation() [0];
		for (Link e : netPlan.getLinks()) e.setLengthInKm(e.getLengthInKm() * linkLengthKmFromEuclideanDistanceFactor);
		this.Efm = netPlan.getNumberOfLinks(); if (Efm != N*(N-1)) throw new RuntimeException ("Bad, Efm" + Efm + ", N: " + N);
		this.linkCost_e = DoubleFactory1D.dense.make(Efm); for (Link e : netPlan.getLinks ()) linkCost_e.set(e.getIndex () , tcfa_linkCostPerKm.getDouble() * e.getLengthInKm());
		
		/* If there is no traffic, create uniform traffic matrix */
		if (netPlan.getNumberOfDemands() == 0)
			for (Node n1 : netPlan.getNodes()) for (Node n2 : netPlan.getNodes()) if (n1 != n2) netPlan.addDemand(n1, n2, tcfa_circuitCapacity_Gbps.getDouble(), null);

		
		/* Convert the demand offered traffic in the upper multiple of circuit capacity */
		DoubleMatrix2D trafficMatrix = netPlan.getMatrixNode2NodeOfferedTraffic();
		netPlan.removeAllDemands();
		for (Node n1 : netPlan.getNodes()) for (Node n2 : netPlan.getNodes()) if (n1.getIndex () > n2.getIndex ())
		{
			final double maxTrafficBidir = Math.max(trafficMatrix.get(n1.getIndex (), n2.getIndex ()), trafficMatrix.get(n2.getIndex (), n1.getIndex ()));
			netPlan.addDemand(n1, n2, tcfa_circuitCapacity_Gbps.getDouble() * Math.ceil(maxTrafficBidir / tcfa_circuitCapacity_Gbps.getDouble()) , null); 
			netPlan.addDemand(n2, n1, tcfa_circuitCapacity_Gbps.getDouble() * Math.ceil(maxTrafficBidir / tcfa_circuitCapacity_Gbps.getDouble()) , null); 
		}
			
		/* If 1+1 then decouple demands in one per channel */
		if (tcfa_recoveryType.getString ().equals("1+1"))
		{
			NetPlan npcopy = netPlan.copy();
			netPlan.removeAllDemands();
			for (Demand d : npcopy.getDemands())
			{
				final int numChannels = (int) Math.round (d.getOfferedTraffic() / tcfa_circuitCapacity_Gbps.getDouble ());
				for (int cont = 0 ; cont < numChannels ; cont ++) netPlan.addDemand(netPlan.getNodeFromId (d.getIngressNode().getId ()), netPlan.getNodeFromId (d.getEgressNode().getId ()), tcfa_circuitCapacity_Gbps.getDouble () , d.getAttributes());
			}
		}

		/* Initialize routes and demands to make it bidirectional */
		Pair<Map<Demand,Demand>,Map<Route,Route>> pair = initializeNetPlanLinksBidirDemandsAndRoutes (netPlan);
		this.opposite_d = pair.getFirst();
		this.opposite_r = pair.getSecond();

		/* Initialize demand info */
		this.D = netPlan.getNumberOfDemands();
		this.numCircH_d = netPlan.getVectorDemandOfferedTraffic().assign(DoubleFunctions.div(this.tcfa_circuitCapacity_Gbps.getDouble ())).assign(DoubleFunctions.rint);
		//System.out.println("Total number of circuits to establish: " + numCircH_d.zSum());
		
		//if (1==1) throw new RuntimeException ("Stop");
		this.R = netPlan.getNumberOfRoutes();
		//System.out.println ("numCircH_d: " + numCircH_d);

		/* Initialize SRG information */
		initializeSRGs(netPlan);
		this.nSRGs = netPlan.getNumberOfSRGs();
		this.routeList_d = new int [D][]; for (Demand d : netPlan.getDemands ()) routeList_d [d.getIndex ()] = getIndexes (d.getRoutes ());
		this.traversedLinks_r = new int [R][]; for (Route r : netPlan.getRoutes ()) traversedLinks_r [r.getIndex ()] = getIndexes (r.getSeqLinks());
		
		
		/* Initialize aux arrays for speed-up */
		this.A_dr = netPlan.getMatrixDemand2RouteAssignment();
		for (Demand d : netPlan.getDemands ()) if (d.getRoutes().isEmpty()) throw new Net2PlanException ("A demand has no assigned routes");
		this.A_er = netPlan.getMatrixLink2RouteAssignment();
		this.A_se = DoubleFactory2D.dense.make(1 + nSRGs , Efm , 1.0); // 1 if link OK, 0 if fails
		for (int contSRG = 0 ; contSRG < nSRGs ; contSRG ++)
			for (Link e : netPlan.getSRG (contSRG).getLinksAllLayers())
				A_se.set(contSRG+1 , e.getIndex () , 0.0);
		this.A_rs = DoubleFactory2D.dense.make(R , 1 + nSRGs , 1.0); // 1 if link OK, 0 if fails
		for (Route r : netPlan.getRoutes ()) for (Link e : r.getSeqLinks()) for (SharedRiskGroup srg : e.getSRGs())
					A_rs.set(r.getIndex (),1+srg.getIndex () , 0.0);

		/* Check if the problem may have a solution: demands have at least one path in any failure state  */
		if (A_dr.zMult(A_rs, null).getMinLocation() [0] == 0) { System.out.println("A_dr.zMult(A_rs, null): " + A_dr.zMult(A_rs, null)); throw new Net2PlanException ("Some demands cannot be routed in some failure state. We need more paths! (e.g. extend the path reach)"); }

		this.Aout_ne = netPlan.getMatrixNodeLinkOutgoingIncidence();
		this.Ain_ne = netPlan.getMatrixNodeLinkIncomingIncidence(); 
		this.Abid_dd = DoubleFactory2D.sparse.make(D,D); for (Entry<Demand,Demand> entry : opposite_d.entrySet()) { Abid_dd.set(entry.getKey().getIndex (), entry.getValue().getIndex (), 1.0); Abid_dd.set(entry.getValue().getIndex (), entry.getKey().getIndex (), 1.0); } 
		this.Abid_rr = DoubleFactory2D.sparse.make(R,R); for (Entry<Route,Route> entry : opposite_r.entrySet()) { Abid_rr.set(entry.getKey().getIndex (), entry.getValue().getIndex (), 1.0); Abid_rr.set(entry.getValue().getIndex (), entry.getKey().getIndex (), 1.0); } 
		this.Abid_ee = DoubleFactory2D.sparse.make(Efm,Efm); for (Link e : netPlan.getLinks ()) { Abid_ee.set(e.getIndex (), oppositeLink(e).getIndex (), 1.0);  Abid_ee.set(oppositeLink(e).getIndex (), e.getIndex (), 1.0); }
		
		final long algorithmInitialtime = System.nanoTime();
		final long algorithmEndtime = algorithmInitialtime + (long) (algorithm_maxExecutionTimeInSeconds.getDouble() * 1E9);

		/* Up stage */
		ArrayList<Integer> shuffledDemands = new ArrayList<Integer> (D); for (int d = 0 ; d < D ; d ++) shuffledDemands.add(d);
		Collections.shuffle(shuffledDemands , rng);
		if (tcfa_recoveryType.getString ().equals("shared"))
		{
			DoubleMatrix1D best_xr = null; DoubleMatrix1D best_pe = null; double bestCost = Double.MAX_VALUE;
			int iterationCounter = 0;
			while ((System.nanoTime() < algorithmEndtime) && (iterationCounter < tcfa_maxNumIterations.getInt ()))
			{
				DoubleMatrix1D thisIt_xr = greedyAndLocalSearch_shared(algorithmEndtime , netPlan);
				DoubleMatrix1D thisIt_pe = A_er.zMult(thisIt_xr, null); thisIt_pe.assign(DoubleFunctions.div(tcfa_linkCapacity_numCirc.getInt ())).assign(DoubleFunctions.ceil);
				double thisItCost = computeCost_shared(thisIt_xr, thisIt_pe);
				if (thisItCost < bestCost) { best_xr = thisIt_xr; best_pe = thisIt_pe; bestCost = thisItCost; }
				iterationCounter ++;
			}
			stat_numItGRASP = iterationCounter;
			for (Route r : netPlan.getRoutes ()) r.setCarriedTraffic(tcfa_circuitCapacity_Gbps.getDouble () * best_xr.get(r.getIndex ()) , tcfa_circuitCapacity_Gbps.getDouble () * best_xr.get(r.getIndex ()));
			for (Link e : netPlan.getLinks ()) e.setCapacity(tcfa_linkCapacity_numCirc.getInt () * tcfa_circuitCapacity_Gbps.getDouble () * best_pe.get(e.getIndex()));
		} else if (tcfa_recoveryType.getString ().equals("1+1"))
		{
			DoubleMatrix1D best_xr = null; DoubleMatrix1D best_x2r = null; DoubleMatrix1D best_pe = null; double bestCost = Double.MAX_VALUE;
			int iterationCounter = 0;
			while ((System.nanoTime() < algorithmEndtime) && (iterationCounter < tcfa_maxNumIterations.getInt ()))
			{
				Pair<DoubleMatrix1D,DoubleMatrix1D> thisItSolution = greedyAndLocalSearch_11(algorithmEndtime , netPlan);
				DoubleMatrix1D thisIt_xr = thisItSolution.getFirst();   
				DoubleMatrix1D thisIt_x2r = thisItSolution.getSecond();
				DoubleMatrix1D thisIt_pe = computePe_11(thisIt_xr, thisIt_x2r);
				double thisItCost = computeCost_11(thisIt_xr, thisIt_x2r , thisIt_pe);
				if (thisItCost < bestCost) { best_xr = thisIt_xr; best_x2r = thisIt_x2r; best_pe = thisIt_pe; bestCost = thisItCost; }
				iterationCounter ++;
			}
			stat_numItGRASP = iterationCounter;
			for (Demand d : netPlan.getDemands ())
			{
				if (Math.abs(best_xr.viewSelection(routeList_d [d.getIndex ()]).zSum() - 1) > 1E-3) throw new RuntimeException ("Bad");
				if (Math.abs(best_x2r.viewSelection(routeList_d [d.getIndex ()]).zSum() - 1) > 1E-3) throw new RuntimeException ("Bad");
				Route primaryRoute = null; Route backupRoute = null;  
				for (Route r : d.getRoutes ()) if (Math.abs(best_xr.get(r.getIndex ()) - 1) <= 1e-3) { primaryRoute = r; primaryRoute.setCarriedTraffic(tcfa_circuitCapacity_Gbps.getDouble () , tcfa_circuitCapacity_Gbps.getDouble ()); break; }
				for (Route r : d.getRoutes ()) if (Math.abs(best_x2r.get(r.getIndex ()) - 1) <= 1e-3) { backupRoute = r; backupRoute.setCarriedTraffic(0 , tcfa_circuitCapacity_Gbps.getDouble ()); break; }
				primaryRoute.addBackupRoute(backupRoute);
			}
			for (Link e : netPlan.getLinks ()) e.setCapacity(tcfa_linkCapacity_numCirc.getInt () * tcfa_circuitCapacity_Gbps.getDouble () * best_pe.get(e.getIndex()));
		
		} else if (tcfa_recoveryType.getString ().equals("restoration"))
		{
			DoubleMatrix2D best_xrs = null; DoubleMatrix1D best_pe = null; double bestCost = Double.MAX_VALUE;
			int iterationCounter = 0;
			while ((System.nanoTime() < algorithmEndtime) && (iterationCounter < tcfa_maxNumIterations.getInt ()))
			{
				DoubleMatrix2D thisIt_xrs = greedyAndLocalSearch_restoration(algorithmEndtime , netPlan);
				DoubleMatrix1D thisIt_pe = computePe_restoration (thisIt_xrs); 
				double thisItCost = computeCost_shared(thisIt_xrs.viewColumn(0), thisIt_pe);
				if (thisItCost < bestCost) { best_xrs = thisIt_xrs; best_pe = thisIt_pe; bestCost = thisItCost; }
				iterationCounter ++;
			}
			stat_numItGRASP = iterationCounter;
			for (Route r : netPlan.getRoutes ()) r.setCarriedTraffic(tcfa_circuitCapacity_Gbps.getDouble () * best_xrs.get(r.getIndex (),0) , tcfa_circuitCapacity_Gbps.getDouble () * best_xrs.get(r.getIndex (),0));
			for (Link e : netPlan.getLinks ()) e.setCapacity(tcfa_linkCapacity_numCirc.getInt () * tcfa_circuitCapacity_Gbps.getDouble () * best_pe.get(e.getIndex()));
		} else throw new RuntimeException ("Bad");		
		
		/* Remove unused routes and links */
		for (Route r : new HashSet<Route>(netPlan.getRoutes())) if (!r.isBackupRoute() && r.getCarriedTraffic() == 0) r.remove();
		for (Link e : new HashSet<Link>(netPlan.getLinks())) if (e.getCapacity() == 0) e.remove();
		
		Quadruple<Double,Double,Double,Double> q = computeCost (netPlan);
		stat_totalCost = q.getFirst();
		stat_costinks = q.getSecond();
		stat_costNodes = q.getThird();
		stat_costCirc = q.getFourth();
		stat_numLinks = (int) (netPlan.getVectorLinkCapacity().zSum () / (tcfa_linkCapacity_numCirc.getInt () * tcfa_circuitCapacity_Gbps.getDouble ())); 
		stat_numCirc = (int) Math.round(netPlan.getVectorDemandCarriedTraffic().zSum() / tcfa_circuitCapacity_Gbps.getDouble ()); if (tcfa_recoveryType.getString ().equals("1+1")) stat_numCirc *=2; 
		for (Node n : netPlan.getNodes()) if (n.getOutgoingLinks().size() > 2) stat_numNodesDeg3 ++; else stat_numNodesDeg2 ++;
		stat_totalTimeSecs = (System.nanoTime() - algorithmInitialtime)*1e-9;
		checkSolution (netPlan);

		double averageLinkUtilization = netPlan.getVectorLinkUtilization().zSum() / stat_numLinks;
		final String fileNameStem = algorithm_outputFileNameRoot.getString() + "_" + tcfa_srgType.getString() + "_c" + tcfa_circuitCapacity_Gbps.getDouble () + "_t" + algorithm_maxExecutionTimeInSeconds.getDouble() + "_r" + tcfa_recoveryType.getString ();  
		try 
		{
			PrintWriter pw = new PrintWriter (new File (fileNameStem + "_allResults.txt"));
			pw.println(stat_totalCost );
			pw.println( stat_costinks );
			pw.println( stat_costNodes );
			pw.println( stat_costCirc );
			pw.println( stat_numLinks );
			pw.println( stat_numCirc );
			pw.println( stat_numNodesDeg2 );
			pw.println( stat_numNodesDeg3 );
			pw.println( stat_numItGRASP );
			pw.println( stat_numLSIterationsReducingCost );
			pw.println( stat_numSolverCalls );
			pw.println( stat_numSolverCallsOk );
			pw.println( stat_totalTimeSecs );
			pw.println( stat_totalTimeCreateProgramsSecs );
			pw.println( stat_totalTimeSolverSecs );
			pw.println( averageLinkUtilization );
			pw.close ();
		} catch (Exception e) { e.printStackTrace(); throw new RuntimeException ("Not possible to write in File " + fileNameStem  + "_allResults.txt"); } 


		netPlan.setAttribute("stat_totalCost", ""+stat_totalCost);
		netPlan.setAttribute("stat_costinks", ""+stat_costinks);
		netPlan.setAttribute("stat_costNodes", ""+stat_costNodes);
		netPlan.setAttribute("stat_costCirc", ""+stat_costCirc);
		netPlan.setAttribute("stat_numLinks", ""+stat_numLinks);
		netPlan.setAttribute("stat_numCirc", ""+stat_numCirc);
		netPlan.setAttribute("stat_numNodesDeg2", ""+stat_numNodesDeg2);
		netPlan.setAttribute("stat_numNodesDeg3", ""+stat_numNodesDeg3);
		netPlan.setAttribute("stat_numItGRASP", ""+stat_numItGRASP);
		netPlan.setAttribute("stat_numLSIterationsReducingCost", ""+stat_numLSIterationsReducingCost);
		netPlan.setAttribute("stat_numSolverCalls", ""+stat_numSolverCalls);
		netPlan.setAttribute("stat_numSolverCallsOk", ""+stat_numSolverCallsOk);
		netPlan.setAttribute("stat_totalTimeSecs", ""+stat_totalTimeSecs);
		netPlan.setAttribute("stat_totalTimeCreateProgramsSecs", ""+stat_totalTimeCreateProgramsSecs);
		netPlan.setAttribute("stat_totalTimeSolverSecs", ""+stat_totalTimeSolverSecs);

		netPlan.saveToFile(new File (fileNameStem + ".n2p"));

//		if (netPlan.getDemandTotalBlockedTraffic() > 1E-3) throw new RuntimeException ("Bad");
//		if (netPlan.getLinksOversubscribed().size() > 0) throw new RuntimeException ("Bad");
		//System.out.println("JOM sol: cost: " + stat_totalCost + ", num links: "+ stat_numLinks);

		return "Ok! cost: " + stat_totalCost + ", num links: "+ stat_numLinks;

		} catch (Exception e) { e.printStackTrace(); throw new RuntimeException ("BAD OUT"); }  

	}

	@Override
	public String getDescription()
	{
		return "This algorithm is devoted to solve the several network planning problems appearing in the case study in Section 12.10 of the book mentioned below. The context is the planning of a WDM optical network, given a set of node locations, and a set of end-to-end optical connections (lightpaths) to establish. The algorithm should decide on the fibers to deploy, and the routing of the lightpaths on the fibers, minimizing the total network cost. The cost model includes the fiber renting cost (paid to a dark fiber provider), OADM cost (separating between OADMs of degree two, and degree higher than two), and the cost of the transponders. The network must be tolerant to a set of user-defined failures (defined by SRGs). The algorithm permits choosing between two recovery types: 1+1, and shared protection. In the latter, the number of lightpaths between two nodes that survive to any of the failures is at least the number of optical connections requested for that node pair. For solving the problem, the algorithm uses a GRASP heuristic, to govern the iterative search that is performed using small ILPs solved with JOM in each iteration.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	private Quadruple<Double,Double,Double,Double> computeCost (NetPlan  np)
	{
		double costLinks = 0;
		for (Link e : np.getLinks ())
			costLinks += linkCost_e.get(e.getIndex ()) * e.getCapacity() / (tcfa_linkCapacity_numCirc.getInt () * tcfa_circuitCapacity_Gbps.getDouble ());
		
		double costCircuits = 0;
		for (Route r : np.getRoutes ()) // sums both lps and 1+1 if there are any
		{
			final double lineRate = r.isBackupRoute()? r.getRoutesIAmBackup().iterator().next().getCarriedTrafficInNoFailureState() : r.getCarriedTrafficInNoFailureState();
			costCircuits += tcfa_circuitCost.getDouble () * (lineRate / tcfa_circuitCapacity_Gbps.getDouble ());
		}
		double costNodes = 0;
		for (Node n : np.getNodes())
		{
			final int nodeDegree = (int) Math.max(n.getOutgoingLinks().size(), n.getIncomingLinks().size()); 
			costNodes += (nodeDegree <= 2)? tcfa_costNodeDegree2.getDouble () : tcfa_costNodeDegreeHigherThan2.getDouble ();
		}
		final double totalCost = costLinks+  costCircuits + costNodes;

		System.out.println("Total cost: " + totalCost + ", links %: " + (costLinks/totalCost) + ", circ %: " + (costCircuits/totalCost) + ", nodes %: " + (costNodes/totalCost));
		
		System.out.println("-- From np cost total :" + totalCost + ", links: " + costLinks + ", nodes: " + costNodes + ", circ: " + costCircuits);

		return Quadruple.of(totalCost,costLinks,costNodes,costCircuits);
	}
	
	private double computeCost_shared (DoubleMatrix1D x_r , DoubleMatrix1D p_e)
	{
		double costLinks = p_e.zDotProduct(linkCost_e);
		final double costCircuits = tcfa_circuitCost.getDouble () * x_r.zSum();
		double costNodes = 0;
		DoubleMatrix1D degree_n = Aout_ne.zMult(p_e, null);
		for (int n = 0; n < N ; n ++)
			costNodes += (degree_n.get(n) <= 2)? tcfa_costNodeDegree2.getDouble () : tcfa_costNodeDegreeHigherThan2.getDouble ();
		final double totalCost = costLinks+  costCircuits + costNodes;
		//System.out.println("-- shared cost total :" + totalCost + ", links: " + costLinks + ", nodes: " + costNodes + ", circ: " + costCircuits);
		return totalCost;
	}
	private double computeCost_11 (DoubleMatrix1D x_r , DoubleMatrix1D x2_r , DoubleMatrix1D p_e)
	{
		double costLinks = p_e.zDotProduct(linkCost_e);
		final double costCircuits = tcfa_circuitCost.getDouble () * (x_r.zSum() + x2_r.zSum());
		double costNodes = 0;
		DoubleMatrix1D degree_n = Aout_ne.zMult(p_e, null);
		for (int n = 0; n < N ; n ++)
			costNodes += (degree_n.get(n) <= 2)? tcfa_costNodeDegree2.getDouble () : tcfa_costNodeDegreeHigherThan2.getDouble ();
		final double totalCost = costLinks+  costCircuits + costNodes;
		//System.out.println("-- shared cost total :" + totalCost + ", links: " + costLinks + ", nodes: " + costNodes + ", circ: " + costCircuits);
		return totalCost;
	}

	private static Link oppositeLink (Link e) { return e.getNetPlan ().getNodePairLinks(e.getDestinationNode(), e.getOriginNode() , false).iterator().next(); }
	
	private Pair<Map<Demand,Demand>,Map<Route,Route>> initializeNetPlanLinksBidirDemandsAndRoutes (NetPlan np)
	{
		/* Remove lower half demands from np */
		np.removeAllRoutes(); 
		for (Node n1 : np.getNodes()) for (Node n2 : np.getNodes()) if (n1.getIndex () > n2.getIndex ()) for (Demand d : np.getNodePairDemands(n1, n2,false)) d.remove ();
		np.addRoutesFromCandidatePathList(netPlan.computeUnicastCandidatePathList(null , tcfa_maxNumberPathsPerDemand.getInt(), tcfa_maxPathLengthInKm.getDouble(), tcfa_maxPathNumberOfHops.getInt(), -1, -1, -1, -1 , null));
		
		/* Add symmetric demands and routes */
		Map<Demand,Demand> opposite_d = new HashMap<Demand,Demand> ();
		Map<Route,Route> opposite_r = new HashMap<Route,Route> ();
		for (Demand d : new HashSet<Demand> (np.getDemands()))
		{
			final Demand opDemand = np.addDemand(d.getEgressNode(), d.getIngressNode(), d.getOfferedTraffic(), null);
			opposite_d.put(d,opDemand);
			opposite_d.put(opDemand,d);
			for (Route r : new HashSet<Route> (d.getRoutes ()))
			{
				final Route oppRoute = np.addRoute(opDemand, r.getCarriedTraffic(), r.getOccupiedCapacity() , oppositeSeqLinks (r.getSeqLinks()), null);
				opposite_r.put(r,oppRoute); opposite_r.put(oppRoute,r);
			}
		}
		return Pair.of(opposite_d,opposite_r);
	}

	private void initializeSRGs (NetPlan np)
	{
		np.removeAllSRGs();
		if (tcfa_srgType.getString ().equals("perBidirectionalLinkBundle"))
		{
			for (Node n1 : np.getNodes()) for (Node n2 : np.getNodes()) if (n1.getIndex () < n2.getIndex())
			{
				final double linkLengthKm = np.getNodePairLinks(n1, n2,false).iterator().next().getLengthInKm();
				final SharedRiskGroup srg = np.addSRG(tcfa_srgMttfPer1000Km_hrs.getDouble () * linkLengthKm / 1000, tcfa_srgMttr_hrs.getDouble (), null);
				for (Link e : np.getNodePairLinks(n1, n2,true)) srg.addLink(e); 
			}
		}
		else if (tcfa_srgType.getString ().equals("noFailure"))
		{
		} else throw new Net2PlanException ("Wrong SRG type");
	}
	
	private List<Link> oppositeSeqLinks  (List<Link> s)
	{
		LinkedList<Link> oppList = new LinkedList<Link> ();
		for (Link e : s) oppList.addFirst(oppositeLink(e));
		return (List<Link>) oppList;
	}

	private void checkSolution (NetPlan np)
	{
		if (np.getDemandsBlocked().size() != 0)
		{
			System.out.println("np.getDemandsBlocked(): " + np.getDemandsBlocked());
			System.out.println("np.getDemandOfferedTrafficMap(): " + np.getVectorDemandOfferedTraffic());
			System.out.println("np.getDemandBlockedTrafficMap(): " + np.getVectorDemandBlockedTraffic());
			throw new RuntimeException ("Bad");
		}
		if (np.getLinksOversubscribed().size() != 0)
		{
			System.out.println("np.getLinksOversubscribed(): " + np.getLinksOversubscribed());
			System.out.println("np.getLinkCapacityMap(): " + np.getVectorLinkCapacity());
			System.out.println("np.getLinkCarriedTrafficMap(): " + np.getVectorLinkCarriedTraffic());
			throw new RuntimeException ("Bad");
		}
		
		/* Check bidirectional topology */
		for (Node n1 : np.getNodes()) 
			for (Node n2 : np.getNodes()) 
				if (n1.getIndex () > n2.getIndex ()) 
				{
					Set<Link> thisLinks = np.getNodePairLinks(n1, n2,false);
					Set<Link> oppLinks = np.getNodePairLinks(n2, n1,false);
					if (thisLinks.size() > 1) throw new RuntimeException ("Bad");
					if (oppLinks.size() > 1) throw new RuntimeException ("Bad");
					if (tcfa_bidirectionalLinks.getBoolean())
					{
						if (thisLinks.size() != oppLinks.size()) throw new RuntimeException ("Bad");
						if (thisLinks.size() > 0)
							if (thisLinks.iterator().next().getCapacity() != oppLinks.iterator().next().getCapacity() ) throw new RuntimeException ("Bad");
					}
				}

		/* Check bidirectional routes */
		for (Route r : np.getRoutes())
		{
			final Route oppRoute = opposite_r.get(r);
			if (!np.getRoutes().contains(oppRoute)) throw new RuntimeException ("Bad");
			if (r.getCarriedTraffic() != oppRoute.getCarriedTraffic()) throw new RuntimeException ("Bad");
		}

		if (tcfa_recoveryType.getString ().equals("1+1"))
		{ // one prot segment, and link disjoint
			for (Route r : np.getRoutesAreNotBackup())
			{
				if (r.getBackupRoutes().size() != 1) throw new RuntimeException ("Bad: " + r.getBackupRoutes().size());
				final Route backupRoute = r.getBackupRoutes().get(0);
				List<Link> seqLinks = new ArrayList<Link> (r.getSeqLinks());
				seqLinks.retainAll(backupRoute.getSeqLinks());
				if (!seqLinks.isEmpty()) 
				{
					System.out.println ("Route : "+  r + " links: " + r.getSeqLinks() + ", backup route " + backupRoute + " links: " + backupRoute.getSeqLinks() + ", route carried traffic: " + r.getCarriedTraffic() + ", backup occupied capacity: " + backupRoute.getOccupiedCapacity());
					throw new RuntimeException ("Bad");
				}
			}
		}
		
	}
	
	private DoubleMatrix1D jomStep_shared (Set<Integer> demandsChange , DoubleMatrix1D maxPe , DoubleMatrix1D  x_r)
	{
		final long initTimeBeforeCreatingProgram = System.nanoTime();
		if (demandsChange.isEmpty()) throw new RuntimeException ("Bad");
		final int [] dChange = IntUtils.toArray(demandsChange);
		int [] rChange = new int [0]; for (int d : dChange) rChange = IntUtils.concatenate(rChange,  routeList_d [d]);
		final int Rrest = rChange.length;
		x_r.viewSelection(rChange).assign(0);
		DoubleMatrix2D A_dRestrRest = A_dr.viewSelection(dChange, rChange);
		DoubleMatrix2D A_erRest = A_er.viewSelection(null, rChange);
		DoubleMatrix2D A_rRests = A_rs.viewSelection(rChange,null);
		DoubleMatrix1D occupiedWithoutChangingR_e = A_er.zMult (x_r,null);
		DoubleMatrix2D Abid_rRestrRest = DoubleFactory2D.sparse.make(Rrest,Rrest);
		for (int rRest = 0 ; rRest < Rrest ; rRest ++) 
		{ 
			final int r = rChange [rRest]; 
			final int opp_r = opposite_r.get(netPlan.getRoute(r)).getIndex ();
			final int [] oppRrest = IntUtils.find(rChange, opp_r, SearchType.ALL);
			if (oppRrest.length != 1) throw new RuntimeException ("Bad");
			Abid_rRestrRest.set(rRest , oppRrest [0] , 1.0); Abid_rRestrRest.set(oppRrest [0], rRest , 1);
		}
		OptimizationProblem op = new OptimizationProblem();
		op.setInputParameter("R",R);
		op.setInputParameter("U",tcfa_linkCapacity_numCirc.getInt ());
		op.setInputParameter("c_e", linkCost_e.toArray() , "row");
		op.setInputParameter("c",  tcfa_circuitCost.getDouble ());
		op.setInputParameter("d3Cost",  tcfa_costNodeDegreeHigherThan2.getDouble ());
		op.setInputParameter("onesS",  DoubleUtils.ones(1+nSRGs) , "row");
		op.setInputParameter("onesE",  DoubleUtils.ones(Efm) , "row");
		op.setInputParameter("A_dRestrRest", new DoubleMatrixND (A_dRestrRest));
		op.setInputParameter("h_dRest", numCircH_d.viewSelection(dChange).toArray(), "row");
		op.setInputParameter("A_rRests", new DoubleMatrixND (A_rRests));
		op.setInputParameter("A_erRest", new DoubleMatrixND (A_erRest));
		op.setInputParameter("Aout_ne", new DoubleMatrixND (Aout_ne));
		op.setInputParameter("Abid_rRestrRest", new DoubleMatrixND (Abid_rRestrRest));
		op.setInputParameter("occup_e", occupiedWithoutChangingR_e.toArray(),  "row");
		
		op.addDecisionVariable("d3_n", true, new int[] { 1, N }, 0, 1); 
		op.addDecisionVariable("p_e", true, new int[] { 1, Efm }, DoubleUtils.zeros(Efm) , (maxPe == null)? DoubleUtils.constantArray(Efm, Double.MAX_VALUE) : maxPe.toArray());
		op.addDecisionVariable("x_rRest", true, new int[] { 1, Rrest}, 0 ,  Double.MAX_VALUE); /* 1 if there is a link from node i to node j, 0 otherwise */

		op.setObjectiveFunction("minimize", "sum(c_e .* p_e) + c * sum (x_rRest) + d3Cost * sum(d3_n)");
		op.addConstraint("A_dRestrRest * diag(x_rRest) * A_rRests >= h_dRest' * onesS"); /* the flow-conservation constraints (NxD constraints) */
		op.addConstraint("A_erRest * x_rRest' + occup_e' <= U * p_e'"); /* the capacity constraints (E constraints) */
		op.addConstraint("Aout_ne * p_e' <= 2 + R * d3_n'"); /* the capacity constraints (E constraints) */
		op.addConstraint("x_rRest == x_rRest * Abid_rRestrRest");
		if (tcfa_bidirectionalLinks.getBoolean()) { op.setInputParameter("Abid_ee", new DoubleMatrixND (Abid_ee)); op.addConstraint("p_e == p_e * Abid_ee"); } 

		stat_totalTimeCreateProgramsSecs += eTime (initTimeBeforeCreatingProgram); 
				
		double solverTimeAllowed = algorithm_maxSolverTimeInSeconds.getDouble();
		do 
		{
			final long t = System.nanoTime(); 
			try 
			{ 
				stat_numSolverCalls ++; op.solve(algorithm_solverName.getString() , "solverLibraryName", algorithm_solverLibraryName.getString () ,  "maxSolverTimeInSeconds" , solverTimeAllowed);
			} catch (Exception e) { System.out.println ("-- EXTRA TIME FOR SOLVER, NOW: " + solverTimeAllowed); solverTimeAllowed += 2; stat_totalTimeSolverSecs += eTime(t);  continue;} 
			stat_totalTimeSolverSecs += eTime(t);  
			if (solverTimeAllowed != algorithm_maxSolverTimeInSeconds.getDouble()) System.out.println ("-- EXTRA TIME FOR SOLVER, NOW: " + solverTimeAllowed);
			solverTimeAllowed *= 2; // duplicate the time if not feasible solution is found
		} while (!op.solutionIsFeasible());		
		stat_numSolverCallsOk ++;
		final double [] new_x_rRest = op.getPrimalSolution("x_rRest").to1DArray();
		x_r.viewSelection(rChange).assign(new_x_rRest);
		return x_r;
	}

	private Pair<DoubleMatrix1D,DoubleMatrix1D> jomStep_11 (Set<Integer> demandsChange ,  DoubleMatrix1D maxPe , DoubleMatrix1D x_r , DoubleMatrix1D x2_r)
	{
//		System.out.println ("demandsChange :" + demandsChange);
		final long initTimeBeforeCreatingProgram = System.nanoTime();

		if (demandsChange.isEmpty()) throw new RuntimeException ("Bad");
		final int [] dChange = IntUtils.toArray(demandsChange);
//		System.out.println ("dChange :" + Arrays.toString(dChange));
		int [] rChange = new int [0]; for (int d : dChange) rChange = IntUtils.concatenate(rChange,  routeList_d [d]);
		final int Rrest = rChange.length;
		final int Drest = dChange.length;
		x_r.viewSelection(rChange).assign(0);
		x2_r.viewSelection(rChange).assign(0);
		DoubleMatrix2D A_dRestrRest = A_dr.viewSelection(dChange, rChange);
		DoubleMatrix2D A_erRest = A_er.viewSelection(null, rChange);
		DoubleMatrix2D A_rRests = A_rs.viewSelection(rChange,null);
		DoubleMatrix1D occupiedWithoutChangingR_e = A_er.zMult (x_r,null);
		DoubleMatrix1D backup_occupiedWithoutChangingR_e = A_er.zMult (x2_r,null);
		for (int e = 0;e < Efm; e ++) occupiedWithoutChangingR_e.set(e, occupiedWithoutChangingR_e.get(e) + backup_occupiedWithoutChangingR_e.get(e));
		DoubleMatrix2D Abid_rRestrRest = DoubleFactory2D.sparse.make(Rrest,Rrest);
		for (int rRest = 0 ; rRest < Rrest ; rRest ++) 
		{ 
			final int r = rChange [rRest]; final int opp_r = opposite_r.get(netPlan.getRoute (r)).getIndex ();
			final int [] oppRrest = IntUtils.find(rChange, opp_r, SearchType.ALL); if (oppRrest.length != 1) throw new RuntimeException ("Bad");
			Abid_rRestrRest.set(rRest , oppRrest [0] , 1.0); Abid_rRestrRest.set(oppRrest [0], rRest , 1);
		}
		
		OptimizationProblem op = new OptimizationProblem();
		op.setInputParameter("R",R);
		op.setInputParameter("U",tcfa_linkCapacity_numCirc.getInt ());
		op.setInputParameter("c_e", linkCost_e.toArray() , "row");
		op.setInputParameter("c",  tcfa_circuitCost.getDouble ());
		op.setInputParameter("d3Cost",  tcfa_costNodeDegreeHigherThan2.getDouble ());
		op.setInputParameter("onesS",  DoubleUtils.ones(1+nSRGs) , "row");
		op.setInputParameter("onesE",  DoubleUtils.ones(Efm) , "row");
		op.setInputParameter("A_dRestrRest", new DoubleMatrixND (A_dRestrRest));
		op.setInputParameter("h_dRest", numCircH_d.viewSelection(dChange).toArray(), "row");
		op.setInputParameter("A_rRests", new DoubleMatrixND (A_rRests));
		op.setInputParameter("A_erRest", new DoubleMatrixND (A_erRest));
		op.setInputParameter("Aout_ne", new DoubleMatrixND (Aout_ne));
		op.setInputParameter("Abid_rRestrRest", new DoubleMatrixND (Abid_rRestrRest));
		op.setInputParameter("occup_e", occupiedWithoutChangingR_e.toArray(),  "row");
		
		op.addDecisionVariable("d3_n", true, new int[] { 1, N }, 0, 1); 
		op.addDecisionVariable("p_e", true, new int[] { 1, Efm }, DoubleUtils.zeros(Efm) , (maxPe == null)? DoubleUtils.constantArray(Efm, Double.MAX_VALUE) : maxPe.toArray());
		op.addDecisionVariable("x_rRest", true, new int[] { 1, Rrest}, 0 ,  1); 
		op.addDecisionVariable("x2_rRest", true, new int[] { 1, Rrest}, 0 ,  1); 

		op.setObjectiveFunction("minimize", "sum(c_e .* p_e) + d3Cost * sum(d3_n)");
		op.addConstraint("A_dRestrRest * x_rRest' == h_dRest'"); /* the flow-conservation constraints (NxD constraints) */
		op.addConstraint("A_dRestrRest * x2_rRest' == h_dRest'"); /* the flow-conservation constraints (NxD constraints) */
		op.addConstraint("A_erRest * (x_rRest+x2_rRest)' + occup_e' <= U * p_e'"); /* the capacity constraints (E constraints) */
		op.addConstraint("Aout_ne * p_e' <= 2 + R*d3_n'"); /* the capacity constraints (E constraints) */
		op.addConstraint("A_dRestrRest * diag(x_rRest+x2_rRest) * A_erRest' <= 1"); 
		op.addConstraint("x_rRest == x_rRest * Abid_rRestrRest"); 
		op.addConstraint("x2_rRest == x2_rRest * Abid_rRestrRest"); 
		if (tcfa_bidirectionalLinks.getBoolean()) { op.setInputParameter("Abid_ee", new DoubleMatrixND (Abid_ee)); op.addConstraint("p_e == p_e * Abid_ee"); } 

//		System.out.println("Abid_rr sum filas: " + Abid_rRestrRest.zMult(DoubleFactory1D.dense.make(Rrest,1), null));
//		System.out.println("Abid_rr sum cols: " +  Abid_rRestrRest.zMult(DoubleFactory1D.dense.make(Rrest,1), null , 1 , 0 , true));
//		System.out.println("Abid_rr non zeros: " +  Abid_rRestrRest.zSum());
		stat_totalTimeCreateProgramsSecs += eTime (initTimeBeforeCreatingProgram); 
		
		double solverTimeAllowed = algorithm_maxSolverTimeInSeconds.getDouble ();
		do 
		{ 
			final long t = System.nanoTime(); 
			try 
			{
				stat_numSolverCalls ++; op.solve(algorithm_solverName.getString() , "solverLibraryName", algorithm_solverLibraryName.getString () ,  "maxSolverTimeInSeconds" , solverTimeAllowed);
			} catch (Exception e) { System.out.println ("-- EXTRA TIME FOR SOLVER, NOW: " + solverTimeAllowed); solverTimeAllowed += 2; stat_totalTimeSolverSecs += eTime(t);  continue; }
			if (solverTimeAllowed != algorithm_maxSolverTimeInSeconds.getDouble ()) System.out.println ("-- EXTRA TIME FOR SOLVER, NOW: " + solverTimeAllowed);
			stat_totalTimeSolverSecs += eTime(t);  
			solverTimeAllowed *= 2; // duplicate the time if not feasible solution is found
			//System.out.println ("Time to create program: " + (1E-9*(t-initTimeBeforeCreatingProgram)) + ", solver time " + ((System.nanoTime()-t)*1E-9) + " s ; ");
		} while (!op.solutionIsFeasible());		
		stat_numSolverCallsOk ++; 
		final double[] new_x_rRest = op.getPrimalSolution("x_rRest").to1DArray();
		final double[] new_x2_rRest = op.getPrimalSolution("x2_rRest").to1DArray();

		x_r.viewSelection(rChange).assign(new_x_rRest);
		x2_r.viewSelection(rChange).assign(new_x2_rRest);
		return Pair.of(x_r,x2_r);
	}

	private DoubleMatrix2D jomStep_restoration (Set<Integer> demandsChange ,  DoubleMatrix1D maxPe , DoubleMatrix2D  x_rs)
	{
		final long initTimeBeforeCreatingProgram = System.nanoTime();
		if (demandsChange.isEmpty()) throw new RuntimeException ("Bad");
		final int [] dChange = IntUtils.toArray(demandsChange);
		int [] rChange = new int [0]; for (int d : dChange) rChange = IntUtils.concatenate(rChange,  routeList_d [d]);
		final int Rrest = rChange.length;
		final int Drest = dChange.length;
		x_rs.viewSelection(rChange,null).assign(0);
//		DoubleMatrix2D A_dRestrRest = A_dr.viewSelection(dChange, rChange);
		DoubleMatrix2D A_dRestrRest = DoubleFactory2D.sparse.make(Drest,Rrest);
		for (int rRest = 0 ; rRest < Rrest ; rRest ++) 
		{ 
			final int r = rChange [rRest]; final int dRest [] = IntUtils.find(dChange, netPlan.getRoute (r).getDemand().getIndex () , SearchType.ALL);
			if (dRest.length  != 1) throw new RuntimeException ("Bad");
			A_dRestrRest.set(dRest [0], rRest , 1.0);
		}
		
		DoubleMatrix2D A_erRest = A_er.viewSelection(null, rChange);
		DoubleMatrix2D A_rRests = A_rs.viewSelection(rChange,null);
		DoubleMatrix2D occupiedWithoutChangingR_es = A_er.zMult (x_rs,null);
		DoubleMatrix2D Abid_rRestrRest = DoubleFactory2D.sparse.make(Rrest,Rrest);
		for (int rRest = 0 ; rRest < Rrest ; rRest ++) 
		{ 
			final int r = rChange [rRest]; final int opp_r = opposite_r.get(netPlan.getRoute (r)).getIndex ();
			final int [] oppRrest = IntUtils.find(rChange, opp_r, SearchType.ALL);
			if (oppRrest.length != 1) throw new RuntimeException ("Bad");
			Abid_rRestrRest.set(rRest , oppRrest [0] , 1.0); Abid_rRestrRest.set(oppRrest [0], rRest , 1);
		}
		
		OptimizationProblem op = new OptimizationProblem();
		op.setInputParameter("R",R);
		op.setInputParameter("U",tcfa_linkCapacity_numCirc.getInt ());
		op.setInputParameter("c_e", linkCost_e.toArray() , "row");
		op.setInputParameter("c",  tcfa_circuitCost.getDouble ());
		op.setInputParameter("d3Cost",  tcfa_costNodeDegreeHigherThan2.getDouble ());
		op.setInputParameter("onesS",  DoubleUtils.ones(1+nSRGs) , "row");
		op.setInputParameter("onesE",  DoubleUtils.ones(Efm) , "row");
		op.setInputParameter("A_dRestrRest", new DoubleMatrixND (A_dRestrRest));
		op.setInputParameter("h_dRest", numCircH_d.viewSelection(dChange).toArray(), "row");
		op.setInputParameter("A_rRests", new DoubleMatrixND (A_rRests));
		op.setInputParameter("A_erRest", new DoubleMatrixND (A_erRest));
		op.setInputParameter("Aout_ne", new DoubleMatrixND (Aout_ne));
		op.setInputParameter("Abid_rRestrRest", new DoubleMatrixND (Abid_rRestrRest));
		op.setInputParameter("occup_es", occupiedWithoutChangingR_es.toArray());
		
		op.addDecisionVariable("d3_n", true, new int[] { 1, N }, 0, 1); 
		op.addDecisionVariable("p_e", true, new int[] { 1, Efm }, DoubleUtils.zeros(Efm) , (maxPe == null)? DoubleUtils.constantArray(Efm, Double.MAX_VALUE) : maxPe.toArray());
		op.addDecisionVariable("x_rRests", true, new int[] { Rrest, 1+nSRGs}, 0 , Double.MAX_VALUE); /* 1 if there is a link from node i to node j, 0 otherwise */

		op.setObjectiveFunction("minimize", "sum(c_e .* p_e) + d3Cost * sum(d3_n)");
		op.addConstraint("A_dRestrRest * x_rRests(all,0) == h_dRest'"); /* the flow-conservation constraints (NxD constraints) */
		op.addConstraint("A_dRestrRest * (x_rRests .* A_rRests) >= h_dRest' * onesS"); /* the flow-conservation constraints (NxD constraints) */
		op.addConstraint("A_erRest * x_rRests + occup_es <= U * p_e' * onesS"); /* the capacity constraints (E constraints) */
		op.addConstraint("Aout_ne * p_e' <= 2 + R*d3_n'"); /* the capacity constraints (E constraints) */
		op.addConstraint("x_rRests == Abid_rRestrRest * x_rRests"); 
		op.addConstraint("x_rRests >= diag(x_rRests(all,0)) * A_rRests"); // if not affected by failure, do not change 
		if (tcfa_bidirectionalLinks.getBoolean()) { op.setInputParameter("Abid_ee", new DoubleMatrixND (Abid_ee)); op.addConstraint("p_e == p_e * Abid_ee"); } 

		stat_totalTimeCreateProgramsSecs += eTime (initTimeBeforeCreatingProgram); 

		double solverTimeAllowed = algorithm_maxSolverTimeInSeconds.getDouble ();
		do 
		{ 
			final long t = System.nanoTime(); 
			try { stat_numSolverCalls ++; op.solve(algorithm_solverName.getString() , "solverLibraryName", algorithm_solverLibraryName.getString () ,  "maxSolverTimeInSeconds" , solverTimeAllowed); } catch (Exception e) { System.out.println ("-- EXTRA TIME FOR SOLVER, NOW: " + solverTimeAllowed); solverTimeAllowed += 2; stat_totalTimeSolverSecs += eTime(t);  continue;} 
			if (solverTimeAllowed != algorithm_maxSolverTimeInSeconds.getDouble ()) System.out.println ("-- EXTRA TIME FOR SOLVER, NOW: " + solverTimeAllowed);
			stat_totalTimeSolverSecs += eTime(t);  
			solverTimeAllowed *= 2; // duplicate the time if not feasible solution is found
			//System.out.println ("Time to create program: " + (1E-9*(t-initTimeBeforeCreatingProgram)) + ", solver time " + ((System.nanoTime()-t)*1E-9) + " s ; ");
		} while (!op.solutionIsFeasible());		
		stat_numSolverCallsOk ++; 
		final DoubleMatrix2D new_x_rRests = op.getPrimalSolution("x_rRests").view2D();
		final DoubleMatrix1D new_pe = DoubleFactory1D.dense.make(op.getPrimalSolution("p_e").to1DArray());
		x_rs.viewSelection(rChange, null).assign(new_x_rRests);
		return x_rs;
	}

	private DoubleMatrix1D greedyAndLocalSearch_shared (long algorithmEndtime , NetPlan np)
	{
		final long initGreedy = System.nanoTime();
		ArrayList<Pair<Node,Node>> shuffledNodePairs = new ArrayList<Pair<Node,Node>> (N*(N-1)/2); 
		for (Node n1 : np.getNodes ()) for (Node n2 : np.getNodes ()) if (n1.getIndex () < n2.getIndex ()) shuffledNodePairs.add(Pair.of(n1, n2));
		Collections.shuffle(shuffledNodePairs , rng);
		DoubleMatrix1D current_xr = DoubleFactory1D.dense.make(R);
		for (Pair<Node,Node> nodePair : shuffledNodePairs)
		{
			Set<Integer> thisDemand = new HashSet<Integer> ();
			for (Demand d : np.getNodePairDemands(nodePair.getFirst(), nodePair.getSecond() , true))
				thisDemand.add(d.getIndex());
//			System.out.println("thsiDemand: " + thisDemand);
			if (jomStep_shared(thisDemand , null , current_xr) == null) throw new RuntimeException ("Returned null");
		}
		DoubleMatrix1D current_pe = A_er.zMult(current_xr, null); current_pe.assign(DoubleFunctions.div(tcfa_linkCapacity_numCirc.getInt ())).assign(DoubleFunctions.ceil);
		double current_cost = computeCost_shared(current_xr, current_pe); 
		//DoubleMatrix1D best_xr = current_xr.copy(); DoubleMatrix1D best_pe = current_pe.copy(); double bestCost = ;
		
		//System.out.println("---- Greedy solution Time " + (1E-9*(System.nanoTime() - initGreedy)) +",  Cost : "+  current_cost + ", num links: " + current_pe.zSum());

		while (System.nanoTime() < algorithmEndtime) 
		{
			//DoubleMatrix1D bestNeighbor_xr = null; DoubleMatrix1D bestNeighbor_pe = null; double bestNeighborCost = Double.MAX_VALUE; Pair<Long,Long> bestNeighborNodePair = null;
			Collections.shuffle(shuffledNodePairs ,rng);
			final long initLSIteration = System.nanoTime();

			boolean improvedSolution = false;
			for (Pair<Node,Node> nodePair : shuffledNodePairs)
			{
//				System.out.println("Pair: " + nodePair + ". Tabu list: " + tabuList_n1n2 + ", IS TABU: " + tabuList_n1n2.contains(nodePair));
				Set<Integer> demandsToChange = new HashSet<Integer> (); 
				for (Demand d : np.getNodePairDemands(nodePair.getFirst(), nodePair.getSecond() , true))
					demandsToChange.add(d.getIndex ()); 
				DoubleMatrix1D thisNeighbor_xr = current_xr.copy(); // do not modify the original one
				if (jomStep_shared(demandsToChange , null , thisNeighbor_xr) == null) throw new RuntimeException ("Returned null");
				DoubleMatrix1D thisNeighbor_pe = A_er.zMult(thisNeighbor_xr, null); thisNeighbor_pe.assign(DoubleFunctions.div(tcfa_linkCapacity_numCirc.getInt ())).assign(DoubleFunctions.ceil);
				final double thisNeighborCost = computeCost_shared (thisNeighbor_xr , thisNeighbor_pe);
				if (thisNeighborCost < current_cost)
				{ 
					current_xr = thisNeighbor_xr; current_pe = thisNeighbor_pe; current_cost = thisNeighborCost; improvedSolution = true; 
					System.out.println("NEW BEST COST: " + current_cost + ", num links: " + current_pe.zSum()); 
					break; 
				}
			}			
			
			//System.out.println("- Local search TIME: " + ((System.nanoTime()-initLSIteration)*1e-9) + ", current cost: " + current_cost + ", num links: " + current_pe.zSum() + ", IMPROVED: " + improvedSolution);

			if (!improvedSolution) break; else stat_numLSIterationsReducingCost ++; // end grasp iteration 
		}
		
		return current_xr;
	}

	private Pair<DoubleMatrix1D,DoubleMatrix1D> greedyAndLocalSearch_11 (long algorithmEndtime , NetPlan np)
	{
		final long initGreedy = System.nanoTime();
		ArrayList<Pair<Node,Node>> shuffledNodePairs = new ArrayList<Pair<Node,Node>> (N*(N-1)/2); 
		for (Node n1 : np.getNodes ()) for (Node n2 : np.getNodes ()) if (n1.getIndex () < n2.getIndex ()) shuffledNodePairs.add(Pair.of(n1, n2));
		Collections.shuffle(shuffledNodePairs , rng);
		DoubleMatrix1D current_xr = DoubleFactory1D.dense.make(R);
		DoubleMatrix1D current_x2r = DoubleFactory1D.dense.make(R);
		for (Pair<Node,Node> nodePair : shuffledNodePairs)
		{
			Set<Integer> thisDemand = new HashSet<Integer> ();
			for (Demand d : np.getNodePairDemands(nodePair.getFirst(), nodePair.getSecond() , true))
				thisDemand.add(d.getIndex());
			final long t = System.nanoTime(); if (jomStep_11(thisDemand , null , current_xr , current_x2r) == null) throw new RuntimeException ("Returned null");
			//System.out.println("Node pair " + nodePair + " time increasing it: " + (1E-9*(System.nanoTime()-t)) + ", num demands: " + thisDemand.size());
		}
		DoubleMatrix1D current_pe = computePe_11 (current_xr , current_xr);
		double current_cost = computeCost_11(current_xr, current_x2r , current_pe); 

		//System.out.println("---- Greedy solution Time " + (1E-9*(System.nanoTime() - initGreedy)) +",  Cost : "+  current_cost + ", num links: " + current_pe.zSum());

		while (System.nanoTime() < algorithmEndtime) 
		{
			Collections.shuffle(shuffledNodePairs ,rng);
			final long initLSIteration = System.nanoTime();

			boolean improvedSolution = false;
			for (Pair<Node,Node> nodePair : shuffledNodePairs)
			{
				Set<Integer> demandsToChange = new HashSet<Integer> (); 
				for (Demand d : np.getNodePairDemands(nodePair.getFirst(), nodePair.getSecond() , true))
					demandsToChange.add(d.getIndex()); 
				DoubleMatrix1D thisNeighbor_xr = current_xr.copy(); // do not modify the original one
				DoubleMatrix1D thisNeighbor_x2r = current_x2r.copy(); // do not modify the original one
				if (jomStep_11(demandsToChange , null , thisNeighbor_xr , thisNeighbor_x2r) == null) throw new RuntimeException ("Returned null");
				DoubleMatrix1D thisNeighbor_pe = computePe_11(thisNeighbor_xr, thisNeighbor_x2r);
				final double thisNeighborCost = computeCost_11 (thisNeighbor_xr , thisNeighbor_x2r , thisNeighbor_pe);
				if (thisNeighborCost < current_cost) 
				{ 
					current_xr = thisNeighbor_xr; current_x2r = thisNeighbor_x2r; current_pe = thisNeighbor_pe; current_cost = thisNeighborCost; improvedSolution = true; 
					System.out.println("NEW BEST COST: " + current_cost + ", num links: " + current_pe.zSum()); 
					break;
				}
			}			
			
			//System.out.println("- Local search TIME: " + ((System.nanoTime()-initLSIteration)*1e-9) + ", current cost: " + current_cost + ", num links: " + current_pe.zSum() + ", IMPROVED: " + improvedSolution);

			if (!improvedSolution) break; else stat_numLSIterationsReducingCost ++; // end grasp iteration 
		}
		
		return Pair.of(current_xr , current_x2r);
	}
	private DoubleMatrix2D greedyAndLocalSearch_restoration (long algorithmEndtime , NetPlan np)
	{
		final long initGreedy = System.nanoTime();
		ArrayList<Pair<Node,Node>> shuffledNodePairs = new ArrayList<Pair<Node,Node>> (N*(N-1)/2); 
		for (Node n1 : np.getNodes ()) for (Node n2 : np.getNodes ()) if (n1.getIndex () < n2.getIndex()) shuffledNodePairs.add(Pair.of(n1, n2));
		Collections.shuffle(shuffledNodePairs , rng);
		DoubleMatrix2D current_xrs = DoubleFactory2D.dense.make(R,1+nSRGs);
		for (Pair<Node,Node> nodePair : shuffledNodePairs)
		{
			Set<Integer> thisDemand = new HashSet<Integer> ();
			for (Demand d : np.getNodePairDemands(nodePair.getFirst(), nodePair.getSecond(),true))
				thisDemand.add(d.getIndex ());
//			System.out.println("thsiDemand: " + thisDemand);
			if (jomStep_restoration(thisDemand , null , current_xrs) == null) throw new RuntimeException ("Returned null");
		}
		DoubleMatrix1D current_pe = computePe_restoration(current_xrs);
		double current_cost = computeCost_shared(current_xrs.viewColumn(0), current_pe); 
		
		//System.out.println("---- Greedy solution Time " + (1E-9*(System.nanoTime() - initGreedy)) +",  Cost : "+  current_cost + ", num links: " + current_pe.zSum() + ", SRGs: " + nSRGs);

		while (System.nanoTime() < algorithmEndtime) 
		{
			Collections.shuffle(shuffledNodePairs ,rng);
			final long initLSIteration = System.nanoTime();

			boolean improvedSolution = false;
			for (Pair<Node,Node> nodePair : shuffledNodePairs)
			{
				Set<Integer> demandsToChange = new HashSet<Integer> (); 
				for (Demand d : np.getNodePairDemands(nodePair.getFirst(), nodePair.getSecond() , true))
					demandsToChange.add(d.getIndex()); 
				DoubleMatrix2D thisNeighbor_xrs = current_xrs.copy(); // do not modify the original one
				if (jomStep_restoration(demandsToChange , null , thisNeighbor_xrs) == null) throw new RuntimeException ("Returned null");
				DoubleMatrix1D thisNeighbor_pe = computePe_restoration(thisNeighbor_xrs);
				final double thisNeighborCost = computeCost_shared (thisNeighbor_xrs.viewColumn(0) , thisNeighbor_pe);
				if (thisNeighborCost < current_cost) 
				{ 
					current_xrs = thisNeighbor_xrs; current_pe = thisNeighbor_pe; current_cost = thisNeighborCost; improvedSolution = true; 
					System.out.println("NEW BEST COST: " + current_cost + ", num links: " + current_pe.zSum()); 
					break; 
				}
			}			
			
			//System.out.println("- Local search TIME: " + ((System.nanoTime()-initLSIteration)*1e-9) + ", current cost: " + current_cost + ", num links: " + current_pe.zSum() + ", IMPROVED: " + improvedSolution);

			if (!improvedSolution) break; else stat_numLSIterationsReducingCost ++; // end grasp iteration 
		}
		
		return current_xrs;
	}
	
	private DoubleMatrix1D computePe_11 (DoubleMatrix1D x_r , DoubleMatrix1D x2_r)
	{
		DoubleMatrix1D p_e = A_er.zMult(x_r, null);
		DoubleMatrix1D backup_occup_e = A_er.zMult(x2_r, null);
		for (int e = 0 ; e < Efm ; e ++) p_e.set(e , p_e.get(e) + backup_occup_e.get(e));
		p_e.assign(DoubleFunctions.div(tcfa_linkCapacity_numCirc.getInt ())).assign(DoubleFunctions.ceil);
		return p_e;
	}
	private DoubleMatrix1D computePe_restoration (DoubleMatrix2D x_rs)
	{
		DoubleMatrix2D p_es = A_er.zMult(x_rs, null); DoubleMatrix1D p_e = DoubleFactory1D.dense.make(Efm); 
		for (int e = 0; e < Efm ; e ++) p_e.set(e , p_es.viewRow(e).getMaxLocation() [0]); // the maximum per state s
		p_e.assign(DoubleFunctions.div(tcfa_linkCapacity_numCirc.getInt ())).assign(DoubleFunctions.ceil);
		return p_e;
	}
	
	private double eTime (long init) { return 1E-9*(System.nanoTime() - init); }
	
	private static int [] getIndexes (Collection<? extends NetworkElement> col) { final int [] res = new int [col.size ()]; int cont = 0; for (NetworkElement el : col) res [cont++] = el.getIndex (); return res; }
	
	
}