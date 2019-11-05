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
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
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
import com.net2plan.utils.Quintuple;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

/**
 * Algorithm based on an ILP solving the VNF placement problem, for given IP links, assuming shortest path routing, and limited 
 * CPU, RAM and HD resources in the nodes, satisfying end to end latency constraints, and considering VNFs that may compress/decompress IP traffic.
 * <p>The result is returned by adding one service chain to each request, realizing it, and dimensioning the VNF instances appropriately.</p>
 * <p>The details of the algorithm will be provided in a publication currently under elaboration.</p>
 * 
 * @net2plan.keywords JOM, NFV, NIW
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_nfvPlacementILP_v1 implements IAlgorithm
{
	private InputParameter shortestPathType = new InputParameter ("shortestPathType", "#select# hops latency" , "Criteria to compute the shortest path. Valid values: 'hops' or 'km'");
	private InputParameter nfvTypesInfo = new InputParameter ("nfvTypesInfo", "NAT 1 1 1 1.0 ; FW 1 1 1 1.0" , "Info of NFVs that could be placed, separated by ';'. Each NFV info has five space-separated parameters: 1) type, 2) CPU use per Gbps, 3) RAM use per Gbps, 4) HD use per Gbps, 5) processing time in ms");
	private InputParameter overideBaseResourcesInfo = new InputParameter ("overideBaseResourcesInfo", true , "If true, the current resources in tne input n2p are removed, and for each node aone CPU, RAM and HD resources are created, with the capacities defined in input parameter defaultCPU_RAM_HD_Capacities");
	private InputParameter defaultCPU_RAM_HD_Capacities = new InputParameter ("defaultCPU_RAM_HD_Capacities", "100 100 100" , "The default capacity values (space separated) of CPU, RAM, HD");
	private InputParameter overideSequenceTraversedNFVs = new InputParameter ("overideSequenceTraversedNFVs", true , "If true, all demands will reset the sequence of NFVs to traverse, to this (NFV types in this param are ; separated)");
	private InputParameter defaultSequenceNFVsToTraverse = new InputParameter ("defaultSequenceNFVsToTraverse", "FW NAT" , "The default sequence of NFVs that demands must traverse");
	private InputParameter solverName = new InputParameter ("solverName", "#select# cplex glpk xpress", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");

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

		
		/*************************************************************************************************************************/
		/************************ INITIALIZE SOME INFORMATION ****************************************************************/
		/*************************************************************************************************************************/

		/* Remove some previous information */
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
		
		final List<Quintuple<String,Double,Double,Double,Double>> nfvTypesIncludingV12_cpuRamHdLat = new ArrayList<> (); 
		nfvTypesIncludingV12_cpuRamHdLat.add(Quintuple.of(";" , 0.0 , 0.0, 0.0 , 0.0)); // the VNF "start of service chain". Added for convenience
		nfvTypesIncludingV12_cpuRamHdLat.add(Quintuple.of(";;" , 0.0 , 0.0, 0.0 , 0.0)); // the VNF "end of service chain". Added for convenience
		for (String nfvInfo : StringUtils.split(nfvTypesInfo.getString() , ";"))
		{
			final String [] fields = StringUtils.split(nfvInfo , " ");
			if (fields.length != 5) throw new Net2PlanException ("Wrong parameter format for NFV info");
			nfvTypesIncludingV12_cpuRamHdLat.add(Quintuple.of(fields[0] , Double.parseDouble(fields [1]) , Double.parseDouble(fields [2]), Double.parseDouble(fields [3]) , Double.parseDouble(fields [4])));
		}
		if (nfvTypesIncludingV12_cpuRamHdLat.size() != nfvTypesIncludingV12_cpuRamHdLat.stream().map(type->type.getFirst()).collect (Collectors.toSet()).size ()) 
			throw new Net2PlanException("Type names cannot be repeated");
		
		final int NUM_EXTVNFYPES = nfvTypesIncludingV12_cpuRamHdLat.size();
		final List<String> eVnfTypes_t = nfvTypesIncludingV12_cpuRamHdLat.stream().map(e->e.getFirst()).collect(Collectors.toCollection(ArrayList::new));
		final Function<String,Integer> getTypeIndex = createMapIndex (nfvTypesIncludingV12_cpuRamHdLat.stream().map(e->e.getFirst()).collect(Collectors.toList()));
		
		/* Check all SCRs traverse VNF types as the ones provided */
		if (wNet.getServiceChainRequests().stream().anyMatch(d->d.getSequenceVnfTypes().size() != new HashSet<> (d.getSequenceVnfTypes()).size ())) throw new Net2PlanException ("Service chain requests cannot traverse a VNf of a given type more than once");
		for (WServiceChainRequest scr : wNet.getServiceChainRequests()) 
			if (scr.getSequenceVnfTypes().stream().anyMatch(tt->getTypeIndex.apply(tt) == null)) throw new Net2PlanException ("Service chain requests must have types among the ones defined in the input parameters");
		
		/* Create the "AugmentedNodes": one for anycast origin, anycast destination, and then one for each (node,VNFExtendedtype) pair */
		final List<AugmentedNode> ans = new ArrayList<> (2 + NUM_EXTVNFYPES * N); 
		final Map<String , List<AugmentedNode>> type2ans = new HashMap<> (); for (String t : eVnfTypes_t) type2ans.put(t, new ArrayList<> ());
		ans.add(new AugmentedNode(true, ans.size())); 
		ans.add(new AugmentedNode(false, ans.size()));
		for (int indexType = 0; indexType < NUM_EXTVNFYPES ; indexType ++)
		{
			final String type = eVnfTypes_t.get(indexType);
			for (WNode n : wNet.getNodes())
			{
				final AugmentedNode an = new AugmentedNode(n, indexType, ans.size()); 
				ans.add(an);
				type2ans.get(type).add(an);
			}
		}
		final AugmentedNode anycastOrigin = ans.get(0);
		final AugmentedNode anycastDestination = ans.get(1);
		final int NUMANS = ans.size();
		
		/* Create the Eup links. Anycast origin to any augmented node, any augmented node to anycast destination, all-to-all augmented nodes, even with themselves */
		final List<EupLink> index2eup = new ArrayList<> (NUMANS * NUMANS - 2 * NUMANS); 
		final Map<Pair<AugmentedNode,AugmentedNode> , EupLink> mapAnPair2EupLink = new HashMap<> (); 
		for (AugmentedNode an1 : ans)
			for (AugmentedNode an2 : ans)
			{
				if (an1.isAnycastDestination ()) continue;
				if (an2.isAnycastOrigin()) continue;
				if (an1.isAnycastOrigin() && an2.isAnycastDestination()) continue;
				final EupLink e = new EupLink(an1 , an2, index2eup.size()); 
				index2eup.add(e);
				final EupLink prevLink = mapAnPair2EupLink.put(Pair.of(an1, an2), e);
				assert prevLink == null;
			}
		final int NUMEUPS = index2eup.size();
		assert NUMEUPS == NUMANS * NUMANS - 2 * NUMANS;

		final List<WServiceChainRequest> index2scr = new ArrayList<> (wNet.getServiceChainRequests());
		final SortedMap<WServiceChainRequest , Integer> scr2index = new TreeMap<> (); for (WServiceChainRequest scr : index2scr) scr2index.put (scr , scr2index.size ());
		
		final List<WIpLink> index2ipLink = new ArrayList<> (wNet.getIpLinks());
		final SortedMap<WIpLink , Integer> ipLink2index = new TreeMap<> (); for (WIpLink ee : index2ipLink) ipLink2index.put (ee , ipLink2index.size ());

		final List<WNode> index2wnode = new ArrayList<> (wNet.getNodes());
		final SortedMap<WNode , Integer> wnode2index = new TreeMap<> (); for (WNode ee : index2wnode) wnode2index.put (ee , wnode2index.size ());

		
		/*************************************************************************************************************************/
		/************************ SOLVE THE MODEL ****************************************************************/
		/*************************************************************************************************************************/
		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();

		/* A_an_eup: 1 if link eup starts in node an, -1 if it ends in an, 0 otherwise */
		final DoubleMatrix2D A_an_eup = DoubleFactory2D.sparse.make (NUMANS , NUMEUPS);
		for (EupLink e : index2eup) A_an_eup.set(e.getA ().getIndexInIlp() , e.getIndexInIlp(), 1.0);
		for (EupLink e : index2eup) A_an_eup.set(e.getB ().getIndexInIlp() , e.getIndexInIlp(), -1.0);
		
		/* A_an_scr: 1 if scr starts in node an, -1 if it ends in an, 0 otherwise */
		final DoubleMatrix2D A_an_scr = DoubleFactory2D.sparse.make (NUMANS , D);
		A_an_scr.viewRow(0).assign(1.0); // all SCRs start in anycast origin
		A_an_scr.viewRow(1).assign(-1.0); // all SCRs start in anycast destination

		/* A_eup_eip: fraction [0,Inf] of traffic in eup, that traveses IP link eip */
		/* latencyMs_eup: latency in ms associated to the sum of: 1) IP links of link eup, and 2) END VNF of eup  */
		final DoubleMatrix2D A_eup_eip = DoubleFactory2D.sparse.make (NUMEUPS ,E_ip);
		final DoubleMatrix1D latencyMs_eup = DoubleFactory1D.dense.make (NUMEUPS);
//		final Pair<Map<Pair<WNode,WNode> , SortedMap<WIpLink,Double>> , Map<Pair<WNode,WNode> , Double>> infoRoutingIgp = getPotentialIpRoutingNormalizedCarrideTrafficAndMaxLatencyMsNoFailureState_ecmp (wNet);
		final boolean trueIsMinimLatencyFalseMinimHops = shortestPathType.getString().equals("latency"); 
		final Pair<Map<Pair<WNode,WNode> , List<WIpLink>> , Map<Pair<WNode,WNode> , Double>> infoRoutingIgp = 
				getPotentialIpRoutingNormalizedCarrideTrafficAndMaxLatencyMsNoFailureState_shortestPath(wNet, trueIsMinimLatencyFalseMinimHops);
		final Map<Pair<WNode,WNode> , List<WIpLink>> nodePair2TraversedIpLinks = infoRoutingIgp.getFirst();
		final Map<Pair<WNode,WNode> , Double> nodePair2WorstcaseLatencyMs = infoRoutingIgp.getSecond();
		for (EupLink e : index2eup) 
		{
			final WNode a = e.getA().getNode();
			final WNode b = e.getB().getNode();
			if (a == null || b == null) continue;
			final double latencyEndVnfMs = nfvTypesIncludingV12_cpuRamHdLat.get (e.getB().getIndexType()).getFifth();
			final double latencyIpLinksMs;
			if (a.equals(b))
				latencyIpLinksMs = 0.0;
			else
			{
				for (WIpLink ipLink : nodePair2TraversedIpLinks.get(Pair.of(a, b)))
					A_eup_eip.set(e.getIndexInIlp(), ipLink2index.get(ipLink), 1.0);
				latencyIpLinksMs = nodePair2WorstcaseLatencyMs.get(Pair.of(a, b));
			}
			latencyMs_eup.set(e.getIndexInIlp(), latencyEndVnfMs + latencyIpLinksMs);
		}			

		/* AcpuPerGbps_eup_n: cpus occupied in node n caused by VNF at the END of eup, per Gbps traversing eup  */
		/* AramPerGbps_eup_n: same for RAM */
		/* AhdPerGbps_eup_n: same for HD */
		final DoubleMatrix2D AcpuPerGbps_eup_n = DoubleFactory2D.sparse.make (NUMEUPS , N);
		final DoubleMatrix2D AramPerGbps_eup_n = DoubleFactory2D.sparse.make (NUMEUPS , N);
		final DoubleMatrix2D AhdPerGbps_eup_n = DoubleFactory2D.sparse.make (NUMEUPS , N);
		for (EupLink e : index2eup) 
		{
			final WNode b = e.getB().getNode();
			if (b == null) continue;
			final int typeIndexOfDestination = e.getB().getIndexType();
			final double cpuPerGbps = nfvTypesIncludingV12_cpuRamHdLat.get(typeIndexOfDestination).getSecond();
			final double ramPerGbps = nfvTypesIncludingV12_cpuRamHdLat.get(typeIndexOfDestination).getThird();
			final double hdPerGbps = nfvTypesIncludingV12_cpuRamHdLat.get(typeIndexOfDestination).getFourth();
			AcpuPerGbps_eup_n.set(e.getIndexInIlp(), wnode2index.get(b), cpuPerGbps);
			AramPerGbps_eup_n.set(e.getIndexInIlp(), wnode2index.get(b), ramPerGbps);
			AhdPerGbps_eup_n.set(e.getIndexInIlp(), wnode2index.get(b), hdPerGbps);
		}			
		
		/* A_an_n: 1 if augmented node an is associatde to node n */
		final DoubleMatrix2D A_an_n = DoubleFactory2D.sparse.make (NUMANS , N);
		for (AugmentedNode an : ans)
			if (!an.isAnycastDestination && !an.isAnycastOrigin)
				A_an_n.set(an.getIndexInIlp(), wnode2index.get(an.getNode()), 1.0);
		
		/* Set some input parameters to the problem */
		op.setInputParameter("cpu_f", nfvTypesIncludingV12_cpuRamHdLat.stream().map(e->e.getSecond()).collect (Collectors.toList()) , "row"); /* for each NFV type, its CPU */
		op.setInputParameter("ram_f", nfvTypesIncludingV12_cpuRamHdLat.stream().map(e->e.getThird()).collect (Collectors.toList()) , "row"); /* for each NFV type, its RAM */
		op.setInputParameter("hardDisk_f", nfvTypesIncludingV12_cpuRamHdLat.stream().map(e->e.getFourth()).collect (Collectors.toList()) , "row"); /* for each NFV type, its HD */
		op.setInputParameter("nfvProcTimeMs_f", nfvTypesIncludingV12_cpuRamHdLat.stream().map(e->e.getFifth()).collect (Collectors.toList()) , "row"); /* for each NFV type, its processing time in ms */
		op.setInputParameter("cpu_n", wNet.getNodes().stream().map(e->e.getTotalNumCpus()).collect(Collectors.toList()) , "row"); /* for each node, CPU capacity  */
		op.setInputParameter("ram_n", wNet.getNodes().stream().map(e->e.getTotalRamGB()).collect(Collectors.toList()) , "row"); /* for each node, RAM capacity  */
		op.setInputParameter("hardDisk_n", wNet.getNodes().stream().map(e->e.getTotalHdGB()).collect(Collectors.toList()) , "row"); /* for each node, HD capacity  */
		
		/* Write the problem formulations */
		/* Forbiden SCR-EUPs: For each SCR only some */
		/* acceptable_scr_eup: 1 the SCR scr could traverse the EUP eup, 0 otherwise */
		final DoubleMatrixND acceptable_scr_eup = new DoubleMatrixND(new int [] {D ,  NUMEUPS} , "sparse");

		/* expFactor_scr_eup: The factor that multiplies the Gbps offered in SCR, to have the Gbps in EUP, WHEN the SCR traverses EUP */
		final DoubleMatrixND expFactor_scr_eup = new DoubleMatrixND(new int [] {D ,  NUMEUPS} , "sparse");
		for (WServiceChainRequest scr : wNet.getServiceChainRequests())
		{
			final int indexScr = scr2index.get(scr);
			/* From anycast origin to all its potential initial nodes (extended VNF type "SCRSTART" == ";") */
			for (AugmentedNode an : type2ans.get(";"))
			{
				if (an.getNode() == null) continue;
				if (!scr.getPotentiallyValidOrigins().contains(an.getNode())) continue;
				final EupLink link = mapAnPair2EupLink.get(Pair.of(anycastOrigin, an));
				assert link != null;
				acceptable_scr_eup.set(new int [] { indexScr, link.getIndexInIlp() }, 1.0);
				expFactor_scr_eup.set(new int [] { indexScr, link.getIndexInIlp() }, 1.0);
			}
			/* From all its potential end nodes (extended VNF type "SCREND" == ";;") to anycast destination */
			for (AugmentedNode an : type2ans.get(";;"))
			{
				if (an.getNode() == null) continue;
				if (!scr.getPotentiallyValidDestinations().contains(an.getNode())) continue;
				final EupLink link = mapAnPair2EupLink.get(Pair.of(an , anycastDestination));
				assert link != null;
				acceptable_scr_eup.set(new int [] { indexScr, link.getIndexInIlp() }, 1.0);
				expFactor_scr_eup.set(new int [] { indexScr, link.getIndexInIlp() }, 1.0);
			}
			/* Only those acceptable type-type pairs that are acceptable */
			final List<String> trueTravTypes = new ArrayList<> (scr.getSequenceVnfTypes());
			for (int indexTrueTypeToTraverse = 0 ; indexTrueTypeToTraverse < trueTravTypes.size() + 1 ; indexTrueTypeToTraverse ++)
			{
				final String previousExtendedTypeToTraverse = indexTrueTypeToTraverse == 0? ";" : trueTravTypes.get(indexTrueTypeToTraverse-1);
				final String thisExtendedTypeToTraverse = indexTrueTypeToTraverse == trueTravTypes.size()? ";;" : trueTravTypes.get(indexTrueTypeToTraverse);
				final double expansionFactor = indexTrueTypeToTraverse == 0? 1.0 : scr.getDefaultSequenceOfExpansionFactorsRespectToInjection().get(indexTrueTypeToTraverse-1); 
				for (AugmentedNode previousAn : type2ans.get(previousExtendedTypeToTraverse))
				{
					for (AugmentedNode thisAn : type2ans.get(thisExtendedTypeToTraverse))
					{
						final EupLink link = mapAnPair2EupLink.get(Pair.of(previousAn , thisAn));
						assert link != null;
						acceptable_scr_eup.set(new int [] {indexScr, link.getIndexInIlp()}, 1.0);
						expFactor_scr_eup.set(new int [] {indexScr, link.getIndexInIlp()}, expansionFactor); /////
					}
				}
			}
		}
		
		op.addDecisionVariable("xx_scr_eup", true , new int[] { D, NUMEUPS}, new DoubleMatrixND (new int [] {D , NUMEUPS}, "sparse"),acceptable_scr_eup); /* number of times SCR passes up link EUP */
//		op.addDecisionVariable("xx_scr_eup", true , new int[] { D, NUMEUPS}, 0 , 1);
		
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
		
		op.setObjectiveFunction("minimize", "sum ((traf_scr * (xx_scr_eup .* expFactor_scr_eup)) * A_eup_eip)"); // minimize the total consumed traffic in the IP links

		op.addConstraint("A_an_eup * (xx_scr_eup') == A_an_scr"); /* All SCRs are carried */
		op.addConstraint("(traf_scr * (xx_scr_eup .* expFactor_scr_eup)) * A_eup_eip <= cap_eIp "); /* Enough capacity in the IP links */
		op.addConstraint("(traf_scr * (xx_scr_eup .* expFactor_scr_eup)) * AcpuPerGbps_eup_n <= cpu_n "); /* Enough CPUs */
		op.addConstraint("(traf_scr * (xx_scr_eup .* expFactor_scr_eup)) * AramPerGbps_eup_n <= ram_n "); /* Enough RAM in the nodes */
		op.addConstraint("(traf_scr * (xx_scr_eup .* expFactor_scr_eup)) * AhdPerGbps_eup_n <= hardDisk_n "); /* Enough HD in the nodes */
		op.addConstraint("xx_scr_eup * latencyMs_eup'  <= maxLatencyMs_scr' "); /* End-to-end latency respected in all the service chain requests */
	
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());
	
		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		

		/*************************************************************************************************************************/
		/************************ CREATE THE OUTPUT DESIGN FROM THE SOLUTION ****************************************************************/
		/*************************************************************************************************************************/
		final DoubleMatrix2D xx_scr_eup = op.getPrimalSolution("xx_scr_eup").view2D();
		final Function<Collection<EupLink> , List<? extends WAbstractNetworkElement>> travIpLinksAndVnfs = eupLinks -> 
			{
				final List<WAbstractNetworkElement> res = new ArrayList<> ();
				AugmentedNode currentNode = anycastOrigin;
				final Set<EupLink> traversedEupLinks = new HashSet<> ();
				while (true) 
				{
					final AugmentedNode initialNode = currentNode;
					assert eupLinks.stream().filter(e->e.getA().equals (initialNode)).count() == 1;
					final EupLink nextEup = eupLinks.stream().filter(e->e.getA().equals (initialNode)).findFirst().orElse(null);
					assert nextEup != null;
					assert !traversedEupLinks.contains(nextEup); traversedEupLinks.add(nextEup);
					final AugmentedNode endNode = nextEup.getB();
					if (initialNode.equals(anycastOrigin)) { currentNode = endNode; continue; }
					if (endNode.equals(anycastDestination)) break;
					final List<WIpLink> travIpLinks = nodePair2TraversedIpLinks.get (Pair.of(nextEup.getA ().getNode() , nextEup.getB ().getNode()));
					res.addAll(travIpLinks);
					final String endExtendedVnf = nfvTypesIncludingV12_cpuRamHdLat.get(nextEup.getB().getIndexType()).getFirst(); 
					assert !endExtendedVnf.equals(";");
					if (!endExtendedVnf.equals(";;"))
					{
						final SortedSet<WVnfInstance> vnf = nextEup.getB().getNode().getVnfInstances(endExtendedVnf);
						assert vnf.size() == 1;
						res.add(vnf.first());
					}
					currentNode = nextEup.getB();
				}
				return res;
			};

		/* Create all the VNF instances, with no capacity, 0 CPU etc. Only set the processing latency  */
		for (WNode n : wNet.getNodes())
		{
			for (Quintuple<String,Double,Double,Double,Double> vnfType : nfvTypesIncludingV12_cpuRamHdLat)
			{
				if (vnfType.getFirst().equals(";") || vnfType.getFirst().equals(";;")) continue;
 				wNet.addVnfInstance(n, "", vnfType.getFirst() , 0.0 , 0.0 , 0.0 , 0.0 , vnfType.getFifth());
			}
		}
		
		/* Set the paths of all the SCRs */
		for (int indexScr = 0 ; indexScr < D ; indexScr ++)
		{
			final WServiceChainRequest scr = index2scr.get(indexScr);
			final List<EupLink> unorderedEupTraversed = new ArrayList<> ();
			for (int eup = 0; eup < NUMEUPS ; eup ++)
				if (xx_scr_eup.get(indexScr, eup) > Configuration.precisionFactor) 
					unorderedEupTraversed.add(index2eup.get(eup));
			scr.addServiceChain(travIpLinksAndVnfs.apply(unorderedEupTraversed), scr.getCurrentOfferedTrafficInGbps());
		}

		/* The VNF instance capacities, CPUs, RAM, HD are set according to its actual traffic. */
		for (WVnfInstance vnf : new ArrayList<> (wNet.getVnfInstances()))
		{
			final Quintuple<String,Double,Double,Double,Double> info = nfvTypesIncludingV12_cpuRamHdLat.get(getTypeIndex.apply(vnf.getType()));
			final double trafProcessedGbps = vnf.getOccupiedCapacityInGbps();
			if (trafProcessedGbps < Configuration.precisionFactor) 
				vnf.remove();
			else
				vnf.setCapacityInGbpsOfInputTraffic(Optional.of (trafProcessedGbps), 
					Optional.of (trafProcessedGbps * info.getSecond()), 
					Optional.of (trafProcessedGbps * info.getThird()), 
					Optional.of (trafProcessedGbps * info.getFourth()));
		}
		
		
		/****************************************************************************************************************/
		/**************************      CHECK THE SOLUTION    **********************************************************/
		/****************************************************************************************************************/
		assert wNet.getServiceChainRequests().stream().allMatch(e->e.getCurrentOfferedTrafficInGbps()> Configuration.precisionFactor);
		assert wNet.getServiceChainRequests().stream().allMatch(e->e.getCurrentBlockedTraffic() == 0);
		assert wNet.getServiceChainRequests().stream().allMatch(e->e.getServiceChains().size() == 1);
		assert wNet.getVnfInstances().stream().allMatch(e->e.getOccupiedCapacityInGbps() <= e.getCurrentCapacityInGbps() + Configuration.precisionFactor);
		for (WNode n : wNet.getNodes())
		{
			assert n.getTotalNumCpus() >= n.getVnfInstances().stream().mapToDouble(e->e.getOccupiedCpus()).sum() + Configuration.precisionFactor;
			assert n.getTotalRamGB() >= n.getVnfInstances().stream().mapToDouble(e->e.getOccupiedRamInGB()).sum() + Configuration.precisionFactor;
			assert n.getTotalHdGB() >= n.getVnfInstances().stream().mapToDouble(e->e.getOccupiedHdInGB()).sum() + Configuration.precisionFactor;
		}
		assert wNet.getIpLinks().stream().allMatch(e->e.getCarriedTrafficGbps() <= e.getCurrentCapacityGbps() + Configuration.precisionFactor);

		return "Ok!: The solution found is guaranteed to be optimal: " + op.solutionIsOptimal();
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
		{
			for (WNode n2 : net.getNodes())
			{
				if (n1.equals(n2)) continue;
				final List<List<WIpLink>> kPaths = net.getKShortestIpUnicastPath(1, net.getNodes(), net.getIpLinks(), n1, n2, Optional.of(costMap));
				if (kPaths.isEmpty()) throw new Net2PlanException ("The IP toplology is not connected");
				fractionOfTrafficFromOspf.put(Pair.of(n1, n2), kPaths.get(0));
				worstCaseLatencyMs.put(Pair.of(n1, n2), kPaths.get(0).stream().mapToDouble(e->e.getWorstCasePropagationDelayInMs()).sum());
			}
			fractionOfTrafficFromOspf.put(Pair.of(n1, n1), new ArrayList<> ());
			worstCaseLatencyMs.put(Pair.of(n1, n1), 0.0);
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
		public WNode getNode () { return node; }
		public String toString () { return isAnycastOrigin? "AnycastOrigin" : isAnycastDestination? "AnycastDest" : getNode().toString(); }
	};
	static class EupLink
	{
		final AugmentedNode a; final AugmentedNode b; final int indexInIlp; 
		EupLink (AugmentedNode a , AugmentedNode b , int indexInIlp) { this.a = a ; this.b = b ; this.indexInIlp = indexInIlp; }
		public int getIndexInIlp () { return indexInIlp; }
		public AugmentedNode getA () { return a; }
		public AugmentedNode getB () { return b; }
		public String toString () { return getA().toString() + "->"+ getB().toString(); }
	};

	private static <T> Function<T,Integer> createMapIndex (Collection<T> list) 
	{
		final Map<T,Integer> res = new HashMap<> ();
		for (T e : list) res.put(e , res.size());
		return e->{ final Integer index = res.get(e); return index; };
	}
	

}
