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
package com.net2plan.examples.niw.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.niw.WIpSourceRoutedConnection;
import com.net2plan.niw.WIpUnicastDemand;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNode;
import com.net2plan.niw.WServiceChain;
import com.net2plan.niw.WServiceChainRequest;
import com.net2plan.niw.WVnfInstance;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

/**
 * Algorithm based on an ILP solving several variants of the service chain allocation problem in networks with nodes 
 * equipped with IT resources (CPU, RAM, HD), and the possibility to instantiate user-defined virtualized network functions (VNFs).
 * 
 * <p>The demands in the input design are service chain requests. The design produces one or more routes (just one 
 * if non-bifurcated routing option is used) from the demand input to the output nodes, traversing the resources of 
 * the required types (e.g. firewall, NAT, ...). Resource types to traverse are user defined. Each resource type is associated 
 * a cost, a capacity, and an amount of IT resources it consumes when instantiated (CPU, RAM, HD). The algorithms produces 
 * a design where all the service chain requests are satisfied, traversing the appropriate user-defined resources so that no 
 * resource is oversubscribed, and IT resources (CPU, HD, RAM) in the nodes are also not oversubscribed.</p>
 * 
 * <p>The algorithm solves an ILP based on a flow-path formulation, where for each demand, a maximum of {@code k} (user-defined 
 * parameter) minimum cost service chains are enumerated, using a variant of the k-shortest path problem. 
 * Each candidate service chain for a demand traverses the appropriate resources 
 * in the appropriate order. The formulation optimally searches among all the options for all the demands, the best global solution. </p>
 * 
 * <p>The result is returned by instanting the appropriate Demand (service chain request), Route (service chain) and 
 * Resource (virtualized functions, and It resources) objects in the output design.</p>
 * <p>The details of the algorithm will be provided in a publication currently under elaboration.</p>
 * 
 * @net2plan.keywords JOM, NFV
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_nfvPlacementILP_v1 implements IAlgorithm
{
	private InputParameter k = new InputParameter ("k", (int) 5 , "Maximum number of admissible service chain paths per demand" , 1 , Integer.MAX_VALUE);
	private InputParameter shortestPathType = new InputParameter ("shortestPathType", "#select# hops km" , "Criteria to compute the shortest path. Valid values: 'hops' or 'km'");
	private InputParameter nfvTypesInfo = new InputParameter ("nfvTypesInfo", "NAT 1 1 1 1 1 ; FW 1 1 1 1 1" , "Info of NFVs that could be placed, separated by ';'. Each NFV info has six space-separated parameters: 1) type, 2) cost (measured in same units as the cost of one BW unit in a link),  3) CPU use, 4) RAM use, 5) HD use, 6) capacity in same units as traffic");
	private InputParameter overideBaseResourcesInfo = new InputParameter ("overideBaseResourcesInfo", true , "If true, the current resources in tne input n2p are removed, and for each node aone CPU, RAM and HD resources are created, with the capacities defined in input parameter defaultCPU_RAM_HD_Capacities");
	private InputParameter defaultCPU_RAM_HD_Capacities = new InputParameter ("defaultCPU_RAM_HD_Capacities", "100 100 100" , "THe default capacity values (space separated) of CPU, RAM, HD");
	private InputParameter overideSequenceTraversedNFVs = new InputParameter ("overideSequenceTraversedNFVs", true , "If true, all demands will reset the sequence of NFVs to traverse, to this (NFV types in this param are ; separated)");
	private InputParameter defaultSequenceNFVsToTraverse = new InputParameter ("defaultSequenceNFVsToTraverse", "FW NAT" , "The default sequence of NFVs that demands must traverse");
	private InputParameter solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");

	private InputParameter optimizationTarget = new InputParameter ("optimizationTarget", "#select# min-total-cost" , "Type of optimization target. Choose among (i) minimize the link BW plus the cost of instantiated resources (assumed measured in cost units equal to the cost of one link BW unit)");
	private InputParameter maxLengthInKmPerSubpath = new InputParameter ("maxLengthInKmPerSubpath", (double) -1 , "Subpaths (parts of the path split by resources) longer than this in km are considered not admissible. A non-positive number means this limit does not exist");
	private InputParameter maxNumHopsPerSubpath = new InputParameter ("maxNumHopsPerSubpath", (int) -1 , "Subpaths (parts of the path split by resources) longer than this in number of hops are considered not admissible. A non-positive number means this limit does not exist");
	private InputParameter maxPropDelayInMsPerSubpath = new InputParameter ("maxPropDelayInMsPerSubpath", (double) -1 , "Subpaths (parts of the path split by resources) longer than this in propagation delay (in ms) are considered not admissible. A non-positive number means this limit does not exist");

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		if (solverName.getString ().equalsIgnoreCase("ipopt")) throw new Net2PlanException ("With IPOPT solver, the routing cannot be constrained to be non-bifurcated");
		
		
		final WNet wNet = new WNet (netPlan);
		try { wNet.checkConsistency(); } catch (Throwable e) { e.printStackTrace(); throw new Net2PlanException ("Not a NIW-valid design: " + e.getMessage()); }

		Configuration.setOption("precisionFactor", new Double(1e-9).toString());
		Configuration.precisionFactor = 1e-9;

		for (WIpUnicastDemand d : new ArrayList<> (wNet.getIpUnicastDemands())) d.remove();
		for (WIpSourceRoutedConnection d : new ArrayList<> (wNet.getIpSourceRoutedConnections())) d.remove();
		for (WServiceChain d : new ArrayList<> (wNet.getServiceChains())) d.remove();
		for (WVnfInstance d : new ArrayList<> (wNet.getVnfInstances())) d.remove();
		
		/* Initialize variables */
		final int E_ip = wNet.getIpLinks().size();
		final int D = wNet.getServiceChainRequests().size();
		final int N = wNet.getNodes().size();
		if (E_ip == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology with links and a demand set");
	
		if (overideBaseResourcesInfo.getBoolean())
		{
			final List<Double> cpuRamHdCap = Arrays.stream(StringUtils.split(defaultCPU_RAM_HD_Capacities.getString(), " ")).map(e -> Double.parseDouble(e)).collect (Collectors.toList());
			for (WVnfInstance d : new ArrayList<> (wNet.getVnfInstances())) d.remove();
			for (WNode n : wNet.getNodes())
			{
				n.setTotalNumCpus(cpuRamHdCap.get(0));
				n.setTotalRamGB(cpuRamHdCap.get(1));
				n.setTotalHdGB(cpuRamHdCap.get(2));
			}
		}
		if (overideSequenceTraversedNFVs.getBoolean())
		{
			final List<String> nfvsToTraverse = Arrays.stream(StringUtils.split(defaultSequenceNFVsToTraverse.getString() , " ")).collect(Collectors.toList());
			for (WServiceChainRequest d : wNet.getServiceChainRequests())
				d.setSequenceVnfTypes(nfvsToTraverse);
		}
		
		/*****************************************************************/

		String [] nfvsInfoArray_f = StringUtils.split(nfvTypesInfo.getString() , ";");
		final int NUMNFVTYPES = nfvsInfoArray_f.length;
		final List<String> nfvType_f = new ArrayList<String> ();
		DoubleMatrix1D nfvCost_f = DoubleFactory1D.dense.make(NUMNFVTYPES);
		DoubleMatrix1D nfvCpu_f = DoubleFactory1D.dense.make(NUMNFVTYPES);
		DoubleMatrix1D nfvRam_f = DoubleFactory1D.dense.make(NUMNFVTYPES);
		DoubleMatrix1D nfvHardDisk_f = DoubleFactory1D.dense.make(NUMNFVTYPES);
		DoubleMatrix1D nfvCap_f = DoubleFactory1D.dense.make(NUMNFVTYPES);
		for (String nfvInfo : nfvsInfoArray_f)
		{
			final String [] fields = StringUtils.split(nfvInfo , " ");
			if (fields.length != 6) throw new Net2PlanException ("Wrong parameter format for NFV info");
			final String type = fields [0];
			if (nfvType_f.contains(type)) throw new Net2PlanException ("Wrong parameter format for NFV info: cannot repeat NFV types");
			final int index = nfvType_f.size();
			nfvType_f.add (type);
			nfvCost_f.set(index, Double.parseDouble(fields [1]));
			nfvCpu_f.set(index, Double.parseDouble(fields [2]));
			nfvRam_f.set(index, Double.parseDouble(fields [3]));
			nfvHardDisk_f.set(index, Double.parseDouble(fields [4]));
			nfvCap_f.set(index, Double.parseDouble(fields [5]));
		}

		/* Check all SCRs traverse VNF types as the ones provided */
		if (wNet.getServiceChainRequests().stream().anyMatch(d->d.getSequenceVnfTypes().size() != new HashSet<> (d.getSequenceVnfTypes()).size ())) throw new Net2PlanException ("Service chain requests cannot traverse a VNf of a given type more than once");
		for (WServiceChainRequest scr : wNet.getServiceChainRequests()) 
			if (scr.getSequenceVnfTypes().stream().anyMatch(tt->!nfvType_f.contains(tt))) throw new Net2PlanException ("Service chain requests must have types among the ones defined in the input parameters");
		
		
		final List<AugmentedNode> ans = new ArrayList<> (2 + NUMNFVTYPES * N); 
		final Map<String , List<AugmentedNode>> type2ans = new HashMap<> (); for (String t : nfvType_f) type2ans.put(t, new ArrayList<> ());
		ans.add(new AugmentedNode(true, ans.size())); 
		ans.add(new AugmentedNode(false, ans.size()));
		for (int indexType = 0; indexType < NUMNFVTYPES ; indexType ++)
			for (WNode n : wNet.getNodes())
			{
				final AugmentedNode an = new AugmentedNode(n, indexType, ans.size()); 
				ans.add(an);
				type2ans.get(nfvRam_f.get(indexType)).add(an);
			}
		final int NUMANS = ans.size();
		
		final List<EupLink> eupLinks = new ArrayList<> (NUMANS * NUMANS - 2 * (NUMNFVTYPES * N)); 
		final Map<Pair<AugmentedNode,AugmentedNode> , EupLink> mapAnPair2EupLink = new HashMap<> (); 
		for (AugmentedNode an1 : ans)
			for (AugmentedNode an2 : ans)
			{
				if (an1.isAnycastDestination ()) continue;
				if (an2.isAnycastOrigin()) continue;
				final EupLink e = new EupLink(an1 , an2, eupLinks.size()); 
				eupLinks.add(e);
				final EupLink prevLink = mapAnPair2EupLink.put(Pair.of(an1, an2), e);
				assert prevLink == null;
			}
		final int NUMEUPS = eupLinks.size();

		final List<WServiceChainRequest> index2scr = new ArrayList<> (wNet.getServiceChainRequests());
		final SortedMap<WServiceChainRequest , Integer> scr2index = new TreeMap<> (); for (WServiceChainRequest scr : index2scr) scr2index.put (scr , scr2index.size ());
		
		final DoubleMatrix2D A_an_eup = DoubleFactory2D.sparse.make (NUMANS , NUMEUPS);
		for (EupLink e : eupLinks) A_an_eup.set(e.getA ().getIndexInIlp() , e.getIndexInIlp(), 1.0);
		for (EupLink e : eupLinks) A_an_eup.set(e.getB ().getIndexInIlp() , e.getIndexInIlp(), -1.0);
		
		final DoubleMatrix2D A_an_scr = DoubleFactory2D.sparse.make (NUMANS , D);
		A_an_scr.viewColumn(0).assign(1.0); // all SCRs start in anycast origin
		A_an_scr.viewColumn(1).assign(-1.0); // all SCRs start in anycast destination
		
		DoubleMatrix1D cpu_an = DoubleFactory1D.dense.make(NUMANS);
		DoubleMatrix1D ram_an = DoubleFactory1D.dense.make(NUMANS);
		DoubleMatrix1D hardDisk_an = DoubleFactory1D.dense.make(NUMANS);
		for (AugmentedNode an : ans)
		{
			if (an.isAnycastOrigin || an.isAnycastDestination) continue;
			final WNode n = an.getNode();
			cpu_an.set(an.getIndexInIlp(), n.getTotalNumCpus());
			ram_an.set(an.getIndexInIlp(), n.getTotalRamGB());
			hardDisk_an.set(an.getIndexInIlp(), n.getTotalHdGB());
		}

//		/* Instantiate "preliminary" NFV resources in the nodes, not consuming any base resource, and with a capacity as if one single instance existed. 
//		 * This is needed so service chain paths can be precomputed. 
//		 * If a node has not enough CPU/RAM/HD to even instantiate one single instance of a NFV type, do not add this resource in that node */
//		for (WNode n : wNet.getNodes())
//		{
//			for (int indexNFVType = 0 ; indexNFVType < NUMNFVTYPES ; indexNFVType ++)
//			{
//				if (nfvCpu_f.get(indexNFVType) > n.getTotalNumCpus()) continue;
//				if (nfvRam_f.get(indexNFVType)  > n.getTotalRamGB()) continue;
//				if (nfvHardDisk_f.get(indexNFVType)  > n.getTotalHdGB()) continue;
//				wNet.addVnfInstance(n, "", nfvType_f.get(indexNFVType) , nfvCap_f.get(indexNFVType) , 0.0 , 0.0 , 0.0 , 0.0);
//			}
//		}
		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();
	
		/* Set some input parameters to the problem */
		op.setInputParameter("u_ip", wNet.getIpLinks().stream().map(e->e.getCurrentCapacityGbps()).collect(Collectors.toList()), "row"); /* for each link, its unused capacity (the one not used by any mulitcast trees) */
		op.setInputParameter("A_dp", netPlan.getMatrixDemand2RouteAssignment()); /* 1 in position (d,p) if demand d is served by path p, 0 otherwise */ 
		op.setInputParameter("A_ep", netPlan.getMatrixLink2RouteAssignment()); /* 1 in position (e,p) if link e is traversed by path p, 0 otherwise */
		op.setInputParameter("h_d", netPlan.getVectorDemandOfferedTraffic(), "row"); /* for each demand, its offered traffic */
		op.setInputParameter("h_p", netPlan.getVectorRouteOfferedTrafficOfAssociatedDemand () , "row"); /* for each path, the offered traffic of its demand */
		op.setInputParameter("c_f", nfvCost_f , "row"); /* for each NFV type, its cost */
		op.setInputParameter("cpu_f", nfvCpu_f , "row"); /* for each NFV type, its CPU */
		op.setInputParameter("ram_f", nfvRam_f , "row"); /* for each NFV type, its RAM */
		op.setInputParameter("hardDisk_f", nfvHardDisk_f , "row"); /* for each NFV type, its HD */
		op.setInputParameter("cap_f", nfvCap_f , "row"); /* for each NFV type, its capacity */
		op.setInputParameter("cpu_an", cpu_an , "row"); /* for each node, CPU capacity  */
		op.setInputParameter("ram_an", ram_an , "row"); /* for each node, RAM capacity  */
		op.setInputParameter("hardDisk_an", hardDisk_an , "row"); /* for each node, HD capacity  */
		
		/* Write the problem formulations */
		if (optimizationTarget.getString ().equals ("min-total-cost")) 
		{
			/* Forbiden SCR-EUPs: For each SCR only some */
			final DoubleMatrix2D acceptable_scr_eup = DoubleFactory2D.sparse.make (D , NUMEUPS);
			for (WServiceChainRequest scr : wNet.getServiceChainRequests())
			{
				final int indexScr = scr2index.get(scr);
				final List<String> travTypes = new ArrayList<> (scr.getSequenceVnfTypes());
				if (travTypes.isEmpty()) { acceptable_scr_eup.set(0, 1, 1.0);  continue; }
				/* From anycast origin to initial node */
				for (AugmentedNode an : type2ans.get(travTypes.get(0)))
				{
					final EupLink link = mapAnPair2EupLink.get(Pair.of(ans.get(0), an));
					assert link != null;
					acceptable_scr_eup.set(indexScr, link.getIndexInIlp(), 1.0);
				}
				/* From anycast origin to initial node */
				for (AugmentedNode an : type2ans.get(travTypes.get(0)))
				{
					final EupLink link = mapAnPair2EupLink.get(Pair.of(ans.get(0), an));
					assert link != null;
					acceptable_scr_eup.set(indexScr, link.getIndexInIlp(), 1.0);
				}
				for (int indexTypeToTraverse = 0 ; indexTypeToTraverse < travTypes.size() ; indexTypeToTraverse ++)
				{
					final String thisTypeToTraverse = travTypes.get(indexTypeToTraverse);
					
				}
			}
			
			
			op.addDecisionVariable("xx_scr_eup", true , new int[] { D, NUMEUPS}, 0, Double.MAX_VALUE); /* number of times SCR passes up link EUP */
			
			op.setInputParameter("A_an_scr", A_an_scr); /* 1 in position (n,e) if link e starts in n, -1 if it ends in n, 0 otherwise */
			op.setInputParameter("A_an_eup", A_an_eup); /* 1 in position (n,d) if demand d starts in n, -1 if it ends in n, 0 otherwise */
			op.addConstraint("A_an_eup * (xx_scr_eup') == A_an_scr"); /* the flow-conservation constraints (NxD constraints) */

			op.setObjectiveFunction("minimize", "sum (l_p .* h_p .* xx_p) + sum (y_nf * c_f') "); 

			op.addConstraint(expression);
			
//			sum (scr, eup traversing eIp) traf(scr) * xscreup * A_eupeIP <= cap(eIP) for all eip
//			sum (scr, eup ending in  n, and this is traversing vnf of type t) traf(scr * fraction goes through vnf type) * xscreup  <= cap(eIP) for all eip , for all node n and vnf type t
			
			
			
			
			op.addConstraint("A_dp * xx_p' == 1"); /* for each demand, the 100% of the traffic is carried (summing the associated paths) */
			op.addConstraint("A_ep * (h_p .* xx_p)' <= u_e'"); /* the traffic in each link cannot exceed its capacity  */
			op.addConstraint("y_nf * cpu_f' <= cpu_n'"); /* the VFs instantiated in the node cannot consume more CPU than the node has */
			op.addConstraint("y_nf * ram_f' <= ram_n'"); /* the VFs instantiated in the node cannot consume more RAM than the node has */
			op.addConstraint("y_nf * hardDisk_f' <= hardDisk_n'"); /* the VFs instantiated in the node cannot consume more hard disk than the node has */
			
			for (int indexNFVType = 0 ; indexNFVType < NUMNFVTYPES ; indexNFVType ++)
			{
				final String type = nfvType_f.get(indexNFVType);
				Pair<List<Resource>,DoubleMatrix2D> info = netPlan.getMatrixResource2RouteAssignment(type); 
				op.setInputParameter("f", indexNFVType); /* K in position (res,p) if resource res is traversed by path p K times */
				op.setInputParameter("A_nfvp", info.getSecond()); /* K in position (res,p) if resource res is traversed by path p K times */
				op.setInputParameter("u_nfv", nfvCap_f.get(indexNFVType)); /* for each resource link, its unused capacity (the one not used by any mulitcast trees) */
				op.addConstraint("A_nfvp * (h_p .* xx_p)' <= y_nf(all,f) * u_nfv"); /* the traffic in each link cannot exceed its capacity  */
			}
		}
		else throw new Net2PlanException ("Unknown optimization target " + optimizationTarget.getString());
	
		
		System.out.println ("solverLibraryName: " +  solverLibraryName.getString ());
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());
		//op.solve(solverName.getString (), "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());
	
		System.out.println ("solverLibraryName: " +  solverLibraryName.getString ());
	
		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		
		/* Save the solution found in the netPlan object */
		final DoubleMatrix1D xx_p = op.getPrimalSolution("xx_p").view1D();
		final DoubleMatrix2D y_nf = op.getPrimalSolution("y_nf").view2D();

		for (Route r : netPlan.getRoutes())
		{
			final double carriedTrafficAndOccupiedLinkCapacity = Math.max(0 , xx_p.get(r.getIndex())) * r.getDemand().getOfferedTraffic();
			r.setCarriedTraffic(carriedTrafficAndOccupiedLinkCapacity, carriedTrafficAndOccupiedLinkCapacity);
		}

		for (Node n : netPlan.getNodes())
		{
			final Resource cpu = n.getResources("CPU").iterator().next(); 
			final Resource ram = n.getResources("RAM").iterator().next(); 
			final Resource hd = n.getResources("HD").iterator().next(); 
			for (int indexNFVType = 0 ; indexNFVType < NUMNFVTYPES ; indexNFVType ++)
			{
				final String type = nfvType_f.get(indexNFVType);
				if (n.getResources(type).isEmpty()) continue;
				final Resource nfv = n.getResources(type).iterator().next();
				final int this_ynf = (int) y_nf.get(n.getIndex(), indexNFVType);
				if (this_ynf == 0) { nfv.remove(); continue; }
				Map<Resource,Double> occupyInBaseResources = new HashMap<Resource,Double> ();
				occupyInBaseResources.put(cpu, this_ynf * nfvCpu_f.get(indexNFVType));
				occupyInBaseResources.put(ram, this_ynf * nfvRam_f.get(indexNFVType));
				occupyInBaseResources.put(hd, this_ynf * nfvHardDisk_f.get(indexNFVType));
				nfv.setCapacity(this_ynf * nfvCap_f.get(indexNFVType), occupyInBaseResources);
			}
		}
		
		netPlan.removeAllRoutesUnused(PRECISION_FACTOR); // routes with zero traffic (or close to zero, with PRECISION_FACTOR tolerance)
	
		return "Ok!: The solution found is guaranteed to be optimal: " + op.solutionIsOptimal() + ". Number routes = " + netPlan.getNumberOfRoutes();
	}
	
	@Override
	public String getDescription()
	{
		return "Algorithm for NFV placement";
	}
	
	
	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	
	static class AugmentedNode
	{
		final WNode node; final int indexType; final int indexInIlp; final boolean isAnycastOrigin; final boolean isAnycastDestination;
		AugmentedNode (WNode n , int indexType , int indexInLp) { this (n , indexType , indexInLp , false , false); }
		AugmentedNode (WNode n , int indexType , int indexInLp , boolean isAnycastOrigin, boolean isAnycastDestination) { this.node = n; this.indexType = indexType; this.indexInIlp = indexInLp; this.isAnycastOrigin = isAnycastOrigin; this.isAnycastDestination = isAnycastDestination; }
		AugmentedNode (boolean isOrigin , int indexInLp) { this(null, -1 , indexInLp , isOrigin , !isOrigin); }
		public int getIndexInIlp () { return indexInIlp; }
		public int getIndexType () { return indexType; }
		public boolean isAnycastOrigin () { return this.isAnycastOrigin; }
		public boolean isAnycastDestination () { return this.isAnycastDestination; }
		public WNode getNode () { assert node != null; return node; }
	};
	static class EupLink
	{
		final AugmentedNode a; final AugmentedNode b; final int indexInIlp; 
		EupLink (AugmentedNode a , AugmentedNode b , int indexInIlp) { this.a = a ; this.b = b ; this.indexInIlp = indexInIlp; }
		public int getIndexInIlp () { return indexInIlp; }
		public AugmentedNode getA () { return a; }
		public AugmentedNode getB () { return b; }
		}
	};

	
}