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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.niw.DefaultStatelessSimulator;
import com.net2plan.niw.WAbstractNetworkElement;
import com.net2plan.niw.WIpLink;
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
	private InputParameter shortestPathType = new InputParameter ("shortestPathType", "#select# hops km" , "Criteria to compute the shortest path. Valid values: 'hops' or 'km'");
	private InputParameter nfvTypesInfo = new InputParameter ("nfvTypesInfo", "NAT 1 1 1 1.0 ; FW 1 1 1 1.0" , "Info of NFVs that could be placed, separated by ';'. Each NFV info has five space-separated parameters: 1) type, 2) CPU use per Gbps, 3) RAM use per Gbps, 4) HD use per Gbps, 5) processing time in ms");
	private InputParameter overideBaseResourcesInfo = new InputParameter ("overideBaseResourcesInfo", true , "If true, the current resources in tne input n2p are removed, and for each node aone CPU, RAM and HD resources are created, with the capacities defined in input parameter defaultCPU_RAM_HD_Capacities");
	private InputParameter defaultCPU_RAM_HD_Capacities = new InputParameter ("defaultCPU_RAM_HD_Capacities", "100 100 100" , "THe default capacity values (space separated) of CPU, RAM, HD");
	private InputParameter overideSequenceTraversedNFVs = new InputParameter ("overideSequenceTraversedNFVs", true , "If true, all demands will reset the sequence of NFVs to traverse, to this (NFV types in this param are ; separated)");
	private InputParameter defaultSequenceNFVsToTraverse = new InputParameter ("defaultSequenceNFVsToTraverse", "FW NAT" , "The default sequence of NFVs that demands must traverse");
	private InputParameter solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");

	private InputParameter weightOf = new InputParameter ("optimizationTarget", "#select# min-total-cost" , "Type of optimization target. Choose among (i) minimize the link BW plus the cost of instantiated resources (assumed measured in cost units equal to the cost of one link BW unit)");
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
		DoubleMatrix1D nfvCpu_f = DoubleFactory1D.dense.make(NUMNFVTYPES);
		DoubleMatrix1D nfvRam_f = DoubleFactory1D.dense.make(NUMNFVTYPES);
		DoubleMatrix1D nfvHardDisk_f = DoubleFactory1D.dense.make(NUMNFVTYPES);
		DoubleMatrix1D nfvProcTimeMs_f = DoubleFactory1D.dense.make(NUMNFVTYPES);
		for (String nfvInfo : nfvsInfoArray_f)
		{
			final String [] fields = StringUtils.split(nfvInfo , " ");
			if (fields.length != 6) throw new Net2PlanException ("Wrong parameter format for NFV info");
			final String type = fields [0];
			if (nfvType_f.contains(type)) throw new Net2PlanException ("Wrong parameter format for NFV info: cannot repeat NFV types");
			final int index = nfvType_f.size();
			nfvType_f.add (type);
			nfvCpu_f.set(index, Double.parseDouble(fields [1]));
			nfvRam_f.set(index, Double.parseDouble(fields [2]));
			nfvHardDisk_f.set(index, Double.parseDouble(fields [3]));
			nfvProcTimeMs_f.set(index, Double.parseDouble(fields [4]));
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
		{
			final String type = nfvType_f.get(indexType);
			for (WNode n : wNet.getNodes())
			{
				final AugmentedNode an = new AugmentedNode(n, indexType, ans.size()); 
				ans.add(an);
				type2ans.get(type).add(an);
			}
		}
		final int NUMANS = ans.size();
		
		/* Create the Eup links. Anycast origin to any augmented node, any augmented node to anycast destination, all-to-all augmented nodes, even with themselves */
		final List<EupLink> index2eup = new ArrayList<> (NUMANS * NUMANS - 2 * (NUMNFVTYPES * N)); 
		final Map<Pair<AugmentedNode,AugmentedNode> , EupLink> mapAnPair2EupLink = new HashMap<> (); 
		for (AugmentedNode an1 : ans)
			for (AugmentedNode an2 : ans)
			{
				if (an1.isAnycastDestination ()) continue;
				if (an2.isAnycastOrigin()) continue;
				final EupLink e = new EupLink(an1 , an2, index2eup.size()); 
				index2eup.add(e);
				final EupLink prevLink = mapAnPair2EupLink.put(Pair.of(an1, an2), e);
				assert prevLink == null;
			}
		final int NUMEUPS = index2eup.size();

		final List<WServiceChainRequest> index2scr = new ArrayList<> (wNet.getServiceChainRequests());
		final SortedMap<WServiceChainRequest , Integer> scr2index = new TreeMap<> (); for (WServiceChainRequest scr : index2scr) scr2index.put (scr , scr2index.size ());
		
		final List<WIpLink> index2ipLink = new ArrayList<> (wNet.getIpLinks());
		final SortedMap<WIpLink , Integer> ipLink2index = new TreeMap<> (); for (WIpLink ee : index2ipLink) ipLink2index.put (ee , ipLink2index.size ());

		final List<WNode> index2wnode = new ArrayList<> (wNet.getNodes());
		final SortedMap<WNode , Integer> wnode2index = new TreeMap<> (); for (WNode ee : index2wnode) wnode2index.put (ee , wnode2index.size ());

		final DoubleMatrix2D A_an_eup = DoubleFactory2D.sparse.make (NUMANS , NUMEUPS);
		for (EupLink e : index2eup) A_an_eup.set(e.getA ().getIndexInIlp() , e.getIndexInIlp(), 1.0);
		for (EupLink e : index2eup) A_an_eup.set(e.getB ().getIndexInIlp() , e.getIndexInIlp(), -1.0);
		
		final DoubleMatrix2D A_an_scr = DoubleFactory2D.sparse.make (NUMANS , D);
		A_an_scr.viewColumn(0).assign(1.0); // all SCRs start in anycast origin
		A_an_scr.viewColumn(1).assign(-1.0); // all SCRs start in anycast destination

		final DoubleMatrix2D A_eup_eip = DoubleFactory2D.sparse.make (NUMEUPS ,E_ip);
		final DoubleMatrix1D latencyMs_eup = DoubleFactory1D.dense.make (NUMEUPS);
//		final Pair<Map<Pair<WNode,WNode> , SortedMap<WIpLink,Double>> , Map<Pair<WNode,WNode> , Double>> infoRoutingIgp = getPotentialIpRoutingNormalizedCarrideTrafficAndMaxLatencyMsNoFailureState_ecmp (wNet);
		final Pair<Map<Pair<WNode,WNode> , List<WIpLink>> , Map<Pair<WNode,WNode> , Double>> infoRoutingIgp = getPotentialIpRoutingNormalizedCarrideTrafficAndMaxLatencyMsNoFailureState_shortestPath(wNet, true);
		final Map<Pair<WNode,WNode> , List<WIpLink>> nodePair2linkNormalizedTraffic = infoRoutingIgp.getFirst();
		final Map<Pair<WNode,WNode> , Double> nodePair2WorstcaseLatencyMs = infoRoutingIgp.getSecond();
		for (EupLink e : index2eup) 
		{
			final WNode a = e.getA().getNode();
			final WNode b = e.getB().getNode();
			for (WIpLink ipLink : nodePair2linkNormalizedTraffic.get(Pair.of(a, b)))
				A_eup_eip.set(e.getIndexInIlp(), ipLink2index.get(ipLink), 1.0);
			final double latencyIpLinksMs = nodePair2WorstcaseLatencyMs.get(Pair.of(a, b));
			final double latencyEndVnfMs = nfvProcTimeMs_f.get(e.getB().getIndexType());
			latencyMs_eup.set(e.getIndexInIlp(), latencyEndVnfMs + latencyIpLinksMs);
		}			

		final DoubleMatrix2D AcpuPerGbps_eup_n = DoubleFactory2D.sparse.make (NUMEUPS , N);
		final DoubleMatrix2D AramPerGbps_eup_n = DoubleFactory2D.sparse.make (NUMEUPS , N);
		final DoubleMatrix2D AhdPerGbps_eup_n = DoubleFactory2D.sparse.make (NUMEUPS , N);
		for (EupLink e : index2eup) 
		{
			final WNode b = e.getB().getNode();
			final int typeIndexOfDestination = e.getB().getIndexType();
			final double cpuPerGbps = nfvCpu_f.get(typeIndexOfDestination);
			final double ramPerGbps = nfvRam_f.get(typeIndexOfDestination);
			final double hdPerGbps = nfvHardDisk_f.get(typeIndexOfDestination);
			AcpuPerGbps_eup_n.set(e.getIndexInIlp(), wnode2index.get(b), cpuPerGbps);
			AramPerGbps_eup_n.set(e.getIndexInIlp(), wnode2index.get(b), ramPerGbps);
			AhdPerGbps_eup_n.set(e.getIndexInIlp(), wnode2index.get(b), hdPerGbps);
		}			
		
		final DoubleMatrix2D A_an_n = DoubleFactory2D.sparse.make (NUMANS , N);
		for (AugmentedNode an : ans)
			if (!an.isAnycastDestination && !an.isAnycastOrigin)
				A_an_n.set(an.getIndexInIlp(), wnode2index.get(an.getNode()), 1.0);
		
		final DoubleMatrix1D cpu_n = DoubleFactory1D.dense.make(N);
		final DoubleMatrix1D ram_n = DoubleFactory1D.dense.make(N);
		final DoubleMatrix1D hardDisk_n = DoubleFactory1D.dense.make(N);
		for (WNode n : index2wnode)
		{
			cpu_n.set(wnode2index.get(n), n.getTotalNumCpus());
			ram_n.set(wnode2index.get(n), n.getTotalRamGB());
			hardDisk_n.set(wnode2index.get(n), n.getTotalHdGB());
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
		op.setInputParameter("cpu_f", nfvCpu_f , "row"); /* for each NFV type, its CPU */
		op.setInputParameter("ram_f", nfvRam_f , "row"); /* for each NFV type, its RAM */
		op.setInputParameter("hardDisk_f", nfvHardDisk_f , "row"); /* for each NFV type, its HD */
		op.setInputParameter("nfvProcTimeMs_f", nfvProcTimeMs_f , "row"); /* for each NFV type, its processing time in ms */
		op.setInputParameter("cpu_n", cpu_n , "row"); /* for each node, CPU capacity  */
		op.setInputParameter("ram_n", ram_n , "row"); /* for each node, RAM capacity  */
		op.setInputParameter("hardDisk_n", hardDisk_n , "row"); /* for each node, HD capacity  */
		
		/* Write the problem formulations */
		/* Forbiden SCR-EUPs: For each SCR only some */
		final DoubleMatrixND acceptable_scr_eup = new DoubleMatrixND(new int [] {D ,  NUMEUPS} , "sparse");
		final DoubleMatrixND expFactor_scr_eup = new DoubleMatrixND(new int [] {D ,  NUMEUPS} , "sparse");
		for (WServiceChainRequest scr : wNet.getServiceChainRequests())
		{
			final int indexScr = scr2index.get(scr);
			final List<String> travTypes = new ArrayList<> (scr.getSequenceVnfTypes());
			if (travTypes.isEmpty()) 
			{ 
				acceptable_scr_eup.set(new int [] {0, 1}, 1.0);
				expFactor_scr_eup.set(new int [] {0, 1}, 1.0);
				continue; 
			}
			/* From anycast origin to initial node */
			for (AugmentedNode an : type2ans.get(travTypes.get(0)))
			{
				final EupLink link = mapAnPair2EupLink.get(Pair.of(ans.get(0), an));
				assert link != null;
				acceptable_scr_eup.set(new int [] { indexScr, link.getIndexInIlp() }, 1.0);
				expFactor_scr_eup.set(new int [] { indexScr, link.getIndexInIlp() }, 1.0);
			}
			/* From a node to anycast destination node */
			for (AugmentedNode an : type2ans.get(travTypes.get(travTypes.size()-1)))
			{
				final EupLink link = mapAnPair2EupLink.get(Pair.of(an , ans.get(1)));
				assert link != null;
				acceptable_scr_eup.set(new int [] {indexScr, link.getIndexInIlp()}, 1.0);
				expFactor_scr_eup.set(new int [] {indexScr, link.getIndexInIlp()}, scr.getDefaultSequenceOfExpansionFactorsRespectToInjection().get(scr.getDefaultSequenceOfExpansionFactorsRespectToInjection().size()-1)); /////
			}
			/* Only those acceptable type-type pairs that are acceptable */
			for (int indexTypeToTraverse = 1 ; indexTypeToTraverse < travTypes.size() ; indexTypeToTraverse ++)
			{
				final String thisTypeToTraverse = travTypes.get(indexTypeToTraverse);
				final String previousTypeToTraverse = travTypes.get(indexTypeToTraverse-1);
				for (AugmentedNode previousAn : type2ans.get(previousTypeToTraverse))
				{
					for (AugmentedNode thisAn : type2ans.get(thisTypeToTraverse))
					{
						final EupLink link = mapAnPair2EupLink.get(Pair.of(previousAn , thisAn));
						assert link != null;
						acceptable_scr_eup.set(new int [] {indexScr, link.getIndexInIlp()}, 1.0);
						expFactor_scr_eup.set(new int [] {indexScr, link.getIndexInIlp()}, scr.getDefaultSequenceOfExpansionFactorsRespectToInjection().get(indexTypeToTraverse-1)); /////
					}
				}
			}
		}
		
		op.addDecisionVariable("xx_scr_eup", true , new int[] { D, NUMEUPS}, new DoubleMatrixND (new int [] {D , NUMEUPS}, "sparse"),acceptable_scr_eup); /* number of times SCR passes up link EUP */
		
		op.setObjectiveFunction("minimize", "sum ((traf_scr * (xx_scr_eup .* expFactor_scr_eup)) * A_eup_eip)"); // minimize the total consumed traffic in the IP links

		op.setInputParameter("traf_scr", index2scr.stream().map(e->e.getCurrentOfferedTrafficInGbps()).collect(Collectors.toList())  , "row"); /* 1 in position (n,e) if link e starts in n, -1 if it ends in n, 0 otherwise */
		op.setInputParameter("maxLatencyMs_scr", index2scr.stream().map(e->e.getMaxLatencyFromOriginEndNode_ms()).collect(Collectors.toList())  , "row"); /* 1 in position (n,e) if link e starts in n, -1 if it ends in n, 0 otherwise */
		op.setInputParameter("cap_eIp", index2ipLink.stream().map(e->e.getCurrentCapacityGbps()).collect(Collectors.toList())  , "row"); /* 1 in position (n,e) if link e starts in n, -1 if it ends in n, 0 otherwise */
		op.setInputParameter("latencyMs_eup", latencyMs_eup  , "row"); /* 1 in position (n,e) if link e starts in n, -1 if it ends in n, 0 otherwise */
		op.setInputParameter("A_an_scr", A_an_scr); /* 1 in position (n,e) if link e starts in n, -1 if it ends in n, 0 otherwise */
		op.setInputParameter("A_an_eup", A_an_eup); /* 1 in position (n,d) if demand d starts in n, -1 if it ends in n, 0 otherwise */
		op.setInputParameter("A_eup_eip", A_eup_eip); /* Fraction of traffic of eup traversing ip link eipLink (according to OSPF) */
		op.setInputParameter("AcpuPerGbps_eup_n", AcpuPerGbps_eup_n); /* Amount of CPU consumed in node n per GBps in eup */
		op.setInputParameter("AramPerGbps_eup_n", AramPerGbps_eup_n); /* Amount of RAM consumed in node n per GBps in eup */
		op.setInputParameter("AhdPerGbps_eup_n", AhdPerGbps_eup_n); /* Amount of HD consumed in node n per GBps in eup */
		op.setInputParameter("expFactor_scr_eup", expFactor_scr_eup); /* For each SCR, and EUP traversed, the expansion factor: ration between Gbps in the EUP and Gbps injected */
		
		op.addConstraint("A_an_eup * (xx_scr_eup') == A_an_scr"); /* All SCRs are carried */
		op.addConstraint("(traf_scr * (xx_scr_eup .* expFactor_scr_eup)) * A_eup_eip <= cap_eIp "); /* Enough capacity in the IP links */
		op.addConstraint("(traf_scr * (xx_scr_eup .* expFactor_scr_eup)) * Acpu_eup_n <= cpu_n "); /* Enough CPUs */
		op.addConstraint("(traf_scr * (xx_scr_eup .* expFactor_scr_eup)) * Aram_eup_n <= ram_n "); /* Enough RAM in the nodes */
		op.addConstraint("(traf_scr * (xx_scr_eup .* expFactor_scr_eup)) * Ahd_eup_n <= hd_n "); /* Enough HD in the nodes */
		op.addConstraint("xx_scr_eup * latencyMs_eup  <= maxLatencyMs_scr' "); /* End-to-end latency respected in all the service chain requests */
	
		System.out.println ("solverLibraryName: " +  solverLibraryName.getString ());
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());
		System.out.println ("solverLibraryName: " +  solverLibraryName.getString ());
	
		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		
		/* Save the solution found in the netPlan object */
		final DoubleMatrix2D xx_scr_eup = op.getPrimalSolution("xx_scr_eup").view2D();
		final Function<EupLink , List<WIpLink>> travIpLinks = e->nodePair2linkNormalizedTraffic.get (Pair.of(e.getA () , e.getB ()));
		final Function<Collection<EupLink> , List<? extends WAbstractNetworkElement>> travIpLinksAndVnfs = eupLinks -> 
			{
				final List<? extends WAbstractNetworkElement> res = new ArrayList<> ();
				final EupLink firstEup = eupLinks.stream().filter(an->an.isAnycastOrigin ()).findFirst().orElse(null);
				assert firstEup != null;
				res.add(firstEup);
				if (eupLinks.size() == 1) return res;
				EupLink currentEupLink = firstEup;
				while (true) 
				{
					final AugmentedNode endNode = currentEupLink.getB();
					final EupLink nextLink = eup
				}
				final EupLink lastEup = eupLinks.stream().filter(an->an.isAnycastOrigin ()).findFirst().orElse(null);
				
			};
		
		
		for (int indexScr = 0 ; indexScr < D ; indexScr ++)
		{
			final WServiceChainRequest scr = index2scr.get(indexScr);
			final List<EupLink> eupTraversedLinks = new ArrayList<> ();
			for (int indexEup = 0; indexEup < NUMEUPS ; indexEup ++)
				if (xx_scr_eup.get(indexScr , indexEup) > Configuration.precisionFactor) 
					eupTraversedLinks.add(index2eup.get(indexEup));
			eupTraversedLinks.get(0).getA().get
			
			
			problemas para recuperar el orden!!!
			
			if (eupTraversedLinks.size() == 1)
			{
				final EupLink ee = eupTraversedLinks.get(0);
				scr.addServiceChain(travIpLinks.apply(ee), scr.getCurrentOfferedTrafficInGbps());
			}
			else
			{
				
			}
				
			
			
			
		}
		
		
		return "Ok!: The solution found is guaranteed to be optimal: " + op.solutionIsOptimal() + ". Number routes = " + netPlan.getNumberOfRoutes();
	}

	private static Pair<Map<Pair<WNode,WNode> , SortedMap<WIpLink,Double>> , Map<Pair<WNode,WNode> , Double>> getPotentialIpRoutingNormalizedCarrideTrafficAndMaxLatencyMsNoFailureState_ecmp (WNet net)
	{
		assert net.getIpUnicastDemands().isEmpty();
		for (WNode n1 : net.getNodes())
			for (WNode n2 : net.getNodes())
				if (!n1.equals(n2))
					net.addIpUnicastDemand(n1, n2, false, true).setCurrentOfferedTrafficInGbps(1.0);

		final DefaultStatelessSimulator alg = new DefaultStatelessSimulator();
		alg.executeAlgorithm(net.getNe(), InputParameter.getDefaultParameters(alg.getParameters()), new HashMap<> ());

		final Map<Pair<WNode,WNode>,SortedMap<WIpLink,Double>> fractionOfTrafficFromOspf = new HashMap<> ();
		final Map<Pair<WNode,WNode>,Double> worstCaseLatencyMs = new HashMap<> ();
		for (WIpUnicastDemand d : net.getIpUnicastDemands())
		{
			fractionOfTrafficFromOspf.put(Pair.of(d.getA(), d.getB()), d.getTraversedIpLinksAndCarriedTraffic(true));
			worstCaseLatencyMs.put(Pair.of(d.getA(), d.getB()), d.getWorstCaseEndtoEndLatencyMs());
		}
		for (WIpUnicastDemand d : new ArrayList<> (net.getIpUnicastDemands())) d.remove();
		assert net.getIpUnicastDemands().isEmpty();
		return Pair.of (fractionOfTrafficFromOspf , worstCaseLatencyMs );
	}

	private static Pair<Map<Pair<WNode,WNode> , List<WIpLink>> , Map<Pair<WNode,WNode> , Double>> getPotentialIpRoutingNormalizedCarrideTrafficAndMaxLatencyMsNoFailureState_shortestPath (WNet net , boolean trueIsMinimLatencyFalseMinimHops)
	{
		assert net.getIpUnicastDemands().isEmpty();
		final Map<Pair<WNode,WNode>,List<WIpLink>> fractionOfTrafficFromOspf = new HashMap<> ();
		final Map<Pair<WNode,WNode>,Double> worstCaseLatencyMs = new HashMap<> ();
		final Map<WIpLink,Double> costMap = new HashMap<> ();
		for (WIpLink e : net.getIpLinks()) costMap.put(e, trueIsMinimLatencyFalseMinimHops? e.getWorstCasePropagationDelayInMs() : 1.0);
		
		for (WNode n1 : net.getNodes())
			for (WNode n2 : net.getNodes())
			{
				if (n1.equals(n2)) continue;
				final List<List<WIpLink>> kPaths = net.getKShortestIpUnicastPath(1, net.getNodes(), net.getIpLinks(), n1, n2, Optional.of(costMap));
				if (kPaths.isEmpty()) throw new Net2PlanException ("The IP toplology is not connected");
				fractionOfTrafficFromOspf.put(Pair.of(n1, n2), kPaths.get(0));
				worstCaseLatencyMs.put(Pair.of(n1, n2), kPaths.get(0).stream().mapToDouble(e->e.getWorstCasePropagationDelayInMs()).sum());
			}
		return Pair.of (fractionOfTrafficFromOspf , worstCaseLatencyMs );
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
	};

}
