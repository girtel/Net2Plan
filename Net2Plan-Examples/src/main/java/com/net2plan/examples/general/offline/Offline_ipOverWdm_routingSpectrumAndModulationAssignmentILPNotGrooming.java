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
package com.net2plan.examples.general.offline;


import cern.colt.list.tdouble.DoubleArrayList;
import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.IntUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import java.util.*;

/**
 * Algorithm based on an ILP solving the Routing, Spectrum, Modulation Assignment (RSMA) problem with regenerator placement, in flexi (elastic) or fixed grid optical WDM networks, with or without fault tolerance, latency and/or lightpath bidirectionality requisites.
 * 
 * <p>The input design is assumed to have a WDM layer compatible with {@link com.net2plan.libraries.WDMUtils WDMUtils} Net2Plan 
 * library usual assumptions:</p>
 * <ul>
 * <li>Each network node is assumed to be an Optical Add/Drop Multiplexer WDM node</li>
 * <li>Each network link at WDM layer is assumed to be an optical fiber.</li>
 * <li>The spectrum in the fibers is assumed to be composed of a number of frequency slots. 
 * In fixed-grid network, each frequency slot would correspond to a wavelength channel. 
 * In flexi-grid networks, it is just a frequency slot, and lightpaths can occupy more than one. In any case, 
 * two lightpaths that use overlapping frequency slots cannot traverse the same fiber, since their signals would mix.</li>
 * <li>Each traffic demand is a need to transmit an amount of Gbps between two nodes. 
 * A demand traffic can be carried using one or more lightpaths.</li>
 * </ul>
 * 
 * <p>Each lightpath produced by the design is returned as a {@code Route} object. Protection lightpaths in 1+1 case 
 * are returned as ProtectionSegment objects attached to the {@code Route}. They represent a set of reserved frequency slots in 
 * a set of fibers, to be used to protect other lightpaths.</p>
 * 
 * <p>Each lightpath starts and ends in a transponder. The user is able to define a set of available transpoder types, so 
 * the design can create lightpaths using any combination of them. The information user-defined per transponder is:</p>
 * <ul>
 * <li>Line rate in Gbps (typically 10, 40, 100 in fixed-grid cases, and other multiples in flexi-grid networks).</li> 
 * <li>Cost</li> 
 * <li>Number of frequency slots occupied (for a given line rate, this depends on the modulation the transponder uses)</li> 
 * <li>Optical reach in km: Maximum allowed length in km of the lightpaths with this transponder. 
 * Higher distances can be reached using signal regenerators.</li> 
 * <li>Cost of a regenerator for this transponder. Regenerators can be placed at intermdiate nodes of the lightpath route, 
 * regenerate its optical signal, and then permit extending its reach. We consider that regenerators cannot change the 
 * frequency slots occupied by the lightpath (that is, they are not capable of wavelength conversion)</li> 
 * </ul>
 * <p> In addition, the user can force the design to use bidirectional transponders (the usual case). This means that (i) in 
 * each transponder we have the transmission and reception side inseparable (so you cannot e.g. buy just the transmission side), 
 * and (ii) a couple of transponders (of course of the same type) being the end points of a lightpath from node 1 to node 2, 
 * also are used for a lightpath from node 2 to node 1, (iii) note however that the two opposite lightpaths can follow arbitrary routes 
 * (they do not have to traverse the reversed sequence of nodes). This bidirectionality constraint is a common requirement if 
 * the lightpath will be used for carrying IP traffic, since IP assumes that its links (the lightpaths) are bidirectional pipes.</p>
 * 
 * <p>We assume that all the fibers use the same wavelength grid, composed of a user-defined number of frequency slots. 
 * The user can also select a limit in the maximum propagation delay of a lightpath. </p>
 * <p>The output design consists in the set of lightpaths to establish, in the 1+1 case also with a 1+1 lightpath each. 
 * Each lightpath is characterized by the transponder type used (which sets its line rate and number of occupied slots in the 
 * traversed fibers), the particular set of contiguous frequency slots occupied, and the set of signal regeneration points (if any). 
 * This information is stored in the {@code Route} and {@code ProtectionSegment} object using the regular methods in WDMUTils, 
 * and can be retrieved in the same form (e.g. by a report showing the WDM network information). 
 * If a feasible solution is not found (one where all the demands are satisfied with the given constraints), a message is shown.</p>.  
 * 
 * <h2>Failure tolerance</h2>
 * <p>The user can choose among three possibilities for designing the network:</p>
 * <ul>
 * <li>No failure tolerant: The lightpaths established should be enough to carry the traffic of all the demands when no failure 
 * occurs in the network, but any losses are accepted if the network suffers failures in links or nodes.</li>
 * <li>Tolerant to single-SRG (Shared Risk Group) failures with static lightpaths: All the traffic demands should be satisfied using 
 * lightpaths, so that under any single-SRG failure (SRGs are taken from the input design), the surviving lightpaths are enough 
 * to carry the 100% of the traffic. Note that lightpaths are static, in the sense that they are not rerouted when affected by a 
 * failure (they just also fail), and the design should just overprovision the number of lightpaths to establish with that in mind.</li>
 * <li>1+1 SRG-disjoint protection: This is another form to provide single-SRG failure tolerance. Each lightpath is backed up by 
 * a SRG-disjoint lightpath (returned as a {@code ProtectionSegment} object). The backup lightpath uses the same type of transponder 
 * as the primary (and thus the same line rate, an occupies the same number of slots), its path is SRG-disjoint, and the particular 
 * set of slots occupied can be different. </li>
 * </ul>
 * <h2>Optimization targets</h2>
 * <p>The user can choose between two optimization targets:</p>
 * <ul>
 * <li>Minimizing the total cost, given by the transponders and regenerators (if any).</li>
 * <li>Minimizing congestion at the WDM layer. This means looking for the design that maximizes the number of idle slots 
 * in the bottleneck fiber (the one with less idle frequency slots). </li>
 * </ul>
 * <h2>Use cases</h2>
 * <p>This algorithm is quite general, and fits a number of use cases designing WDM networks, for instance:</p>
 * <ul>
 * <li>Single line rate, fixed grid networks: Then, one single type of transponder will be available, which occupies one frequency slot</li>
 * <li>Mixed-Line Rate fixed-grid networks: In this case, several transponders can be available at different line rates and with different optical 
 * reaches. However, all of them occupy one slot (one wavelength channel)</li>
 * <li>Single line rate, flexi-grid networks using varying-modulation transponders: Several transponders are available (or the same 
 * transponder with varying configurations), all of them with the same line rate, but thanks to the different usable modulations 
 * they can have different optical reaches and/or number of occupied slots.</li>
 * <li>Multiple line rate, flexi-grid networks using Bandwidth Variable Transponders: Here, it is possible to use different transponders 
 * with different line rates, e.g. to reflect more sophisticated transponders which can have different configurations, varying its line rate, 
 * optical reach, and number of occupied slots.</li>
 * <li>...</li>
 * </ul>
 * <h2>Some details of the algorithm</h2>
 * <p>The algorithm is based on solving a set of MILPs (Mixed Integer Linear Programs) interfacing from JOM to the user-defined 
 * solver. The algorithm uses a flow-path formulation, where a number of candidate paths are pre-computed for each demand. The 
 * user-defined parameter {@code k} limits the maximum number of paths computed per demand. In the 1+1 case, the usable SRG-disjoint 
 * path pairs for each demand are computed from the candidate paths. Increasing the number of paths {@code k} also increases 
 * the computational complexity of the algorithm, but can produce better solutions. In some occasions, a low {@code k} number is the 
 * reason for the algorithm failing in finding a feasible solution.</p>
 * <p>The details of the algorithm will be provided in a publication currently under elaboration.</p>
 * 
 * @net2plan.keywords JOM, WDM
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
@SuppressWarnings("unchecked")
public class Offline_ipOverWdm_routingSpectrumAndModulationAssignmentILPNotGrooming implements IAlgorithm
{
	private InputParameter k = new InputParameter ("k", (int) 5 , "Maximum number of admissible paths per input-output node pair" , 1 , Integer.MAX_VALUE);
	private InputParameter solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter numFrequencySlotsPerFiber = new InputParameter ("numFrequencySlotsPerFiber", (int) 40 , "Number of wavelengths per link" , 1, Integer.MAX_VALUE);
	private InputParameter transponderTypesInfo = new InputParameter ("transponderTypesInfo", "10 1 1 9600 1" , "Transpoder types separated by \";\" . Each type is characterized by the space-separated values: (i) Line rate in Gbps, (ii) cost of the transponder, (iii) number of slots occupied in each traversed fiber, (iv) optical reach in km (a non-positive number means no reach limit), (v) cost of the optical signal regenerator (regenerators do NOT make wavelength conversion ; if negative, regeneration is not possible).");
	private InputParameter ipLayerIndex = new InputParameter ("ipLayerIndex", (int) 1 , "Index of the IP layer (-1 means default layer)");
	private InputParameter wdmLayerIndex = new InputParameter ("wdmLayerIndex", (int) 0 , "Index of the WDM layer (-1 means default layer)");
	private InputParameter networkRecoveryType = new InputParameter ("networkRecoveryType", "#select# not-fault-tolerant single-srg-tolerant-static-lp 1+1-srg-disjoint-lps" , "Establish if the design should be tolerant or not to single SRG failures (SRGs are as defined in the input NetPlan). First option is that the design should not be fault tolerant, the second means that failed lightpaths are not recovered, but an overprovisioned should be made so enough lightpaths survive to carry all the traffic in every failure. The third means that each lightpath is 1+1 protceted by a SRG-disjoint one, that uses the same transponder");
	private InputParameter optimizationTarget = new InputParameter ("optimizationTarget", "#select# min-cost maximin-fiber-number-idle-slots" , "Type of optimization target. Choose among minimize the network cost given by the su mof the transponders cost, and maximize the number of idle frequency slots in the fiber with less idle frequency slot");
	private InputParameter maxPropagationDelayMs = new InputParameter ("maxPropagationDelayMs", (double) -1 , "Maximum allowed propagation time of a lighptath in miliseconds. If non-positive, no limit is assumed");
	private InputParameter bidirectionalTransponders = new InputParameter ("bidirectionalTransponders", true , "If true, the transponders used are bidirectional. Then, the number of lightpaths of each type from node 1 to node 2, equals the number of lightpahts from 2 to 1 (see that both directions can have different routes and slots)");
	private NetworkLayer wdmLayer, ipLayer;
	private Demand.IntendedRecoveryType recoveryTypeNewLps;

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		/* Create a two-layer IP over WDM design if the input is single layer */
		if (netPlan.getNumberOfLayers() == 1)
		{
			this.wdmLayer = netPlan.getNetworkLayerDefault();
			netPlan.setDemandTrafficUnitsName("Gbps");
			netPlan.setLinkCapacityUnitsName("Frequency slots");
			this.ipLayer = netPlan.addLayer("IP" , "IP layer" , "Gbps" , "Gbps" , null , null);
			for (Demand wdmDemand : netPlan.getDemands(wdmLayer))
				netPlan.addDemand(wdmDemand.getIngressNode(), wdmDemand.getEgressNode() , wdmDemand.getOfferedTraffic() , wdmDemand.getAttributes() , ipLayer);
		}
		else
		{
			this.wdmLayer = wdmLayerIndex.getInt () == -1? netPlan.getNetworkLayerDefault() : netPlan.getNetworkLayer(wdmLayerIndex.getInt ());
			this.ipLayer = ipLayerIndex.getInt () == -1? netPlan.getNetworkLayerDefault() : netPlan.getNetworkLayer(ipLayerIndex.getInt ());
		}

		final NetworkLayer wdmLayer = wdmLayerIndex.getInt () == -1? netPlan.getNetworkLayerDefault() : netPlan.getNetworkLayer(wdmLayerIndex.getInt ());

		/* Basic checks */
		final int N = netPlan.getNumberOfNodes();
		final int Ewdm = netPlan.getNumberOfLinks(wdmLayer);
		final int Dip = netPlan.getNumberOfDemands(ipLayer);
		final int S = numFrequencySlotsPerFiber.getInt();
		if (N == 0 || Ewdm == 0 || Dip == 0) throw new Net2PlanException("This algorithm requires a topology with links and a demand set");

		if (networkRecoveryType.getString().equals("not-fault-tolerant") || networkRecoveryType.getString().equals("single-srg-tolerant-static-lp"))
			recoveryTypeNewLps = Demand.IntendedRecoveryType.NONE;
		else if (networkRecoveryType.getString().equals("1+1-srg-disjoint-lps"))
			recoveryTypeNewLps = Demand.IntendedRecoveryType.PROTECTION_REVERT;
		else throw new Net2PlanException ("Wrong input parameters");

		/* Store transpoder info */
		WDMUtils.TransponderTypesInfo tpInfo = new WDMUtils.TransponderTypesInfo(transponderTypesInfo.getString());
		final int T = tpInfo.getNumTypes();
		
		/* Remove all routes in current netPlan object. Initialize link capacities and attributes, and demand offered traffic */
		netPlan.removeAllLinks (ipLayer);
		netPlan.removeAllDemands (wdmLayer);
		netPlan.removeAllMulticastDemands(wdmLayer);
		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING , wdmLayer);
		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING , ipLayer);

		/* Compute the candidate path list of possible paths */
		final Map<Pair<Node,Node>,List<List<Link>>> cpl = netPlan.computeUnicastCandidatePathList(netPlan.getVectorLinkLengthInKm(wdmLayer) , k.getInt(), tpInfo.getMaxOpticalReachKm() , -1, maxPropagationDelayMs.getDouble(), -1, -1, -1 , null , wdmLayer);
		final Map<Pair<Node,Node>,List<Pair<List<Link>,List<Link>>>> cpl11 = networkRecoveryType.getString().equals("1+1-srg-disjoint-lps")? NetPlan.computeUnicastCandidate11PathList(cpl,0) : null;
		
		/* Compute the CPL, adding the routes */
		/* 1+1 case: as many routes as 1+1 valid pairs (then, the same sequence of links can be in more than one Route). The route index and segment index are the same (1+1 pair index) */
		/* rest of the cases: each sequence of links appears at most once */
		Map<Link,Double> linkLengthMap = new HashMap<Link,Double> (); for (Link e : netPlan.getLinks(wdmLayer)) linkLengthMap.put(e , e.getLengthInKm());
		final int maximumNumberOfPaths = T*k.getInt()*Dip;
		List<Integer> transponderType_p = new ArrayList<Integer> (maximumNumberOfPaths);
		List<Double> cost_p = new ArrayList<Double> (maximumNumberOfPaths); 
		List<Double> lineRate_p = new ArrayList<Double> (maximumNumberOfPaths); 
		List<Integer> numSlots_p = new ArrayList<Integer> (maximumNumberOfPaths);
		List<List<Link>> seqLinks_p = new ArrayList<List<Link>> (maximumNumberOfPaths);
		List<List<Link>> seqLinks2_p = cpl11 == null? null : new ArrayList<List<Link>> (maximumNumberOfPaths);
		List<int []> regPositions_p = new ArrayList<int []> (maximumNumberOfPaths);
		List<int []> regPositions2_p = new ArrayList<int []> (maximumNumberOfPaths);
		List<Demand> ipDemand_p = new ArrayList<Demand> (maximumNumberOfPaths);
		for (Demand ipDemand : netPlan.getDemands(ipLayer))
		{
			boolean atLeastOnePathOrPathPair = false;
			for (int t = 0 ; t < T ; t ++)
			{
				final Pair<Node,Node> nodePair = Pair.of(ipDemand.getIngressNode() , ipDemand.getEgressNode());
				final double regeneratorCost = tpInfo.getRegeneratorCost(t);
				final boolean isRegenerable = regeneratorCost >= 0;
				for (Object sp : cpl11 != null? cpl11.get(nodePair) : cpl.get(nodePair))
				{
					List<Link> firstPath = (cpl11 == null)? ((List<Link>) sp) : ((Pair<List<Link>,List<Link>>) sp).getFirst(); 
					List<Link> secondPath = (cpl11 == null)? null : ((Pair<List<Link>,List<Link>>) sp).getSecond (); 
					if (!isRegenerable && (getLengthInKm(firstPath) > tpInfo.getOpticalReachKm(t))) break;
					if (secondPath != null) if (!isRegenerable && (getLengthInKm(secondPath) > tpInfo.getOpticalReachKm(t))) break;

					final int [] regPositions1 = isRegenerable? WDMUtils.computeRegeneratorPositions(firstPath , tpInfo.getOpticalReachKm(t)) : new int [firstPath.size()];
					final int [] regPositions2 = cpl11 == null? null : isRegenerable? WDMUtils.computeRegeneratorPositions(secondPath , tpInfo.getOpticalReachKm(t)) : new int [secondPath.size()];
					final int numRegeneratorsNeeded = !isRegenerable? 0 : (int) IntUtils.sum(regPositions1) + (secondPath == null? 0 : (int) IntUtils.sum(regPositions2)) ;
					final double costOfLightpathOr11Pair = tpInfo.getCost(t) * (cpl11 != null? 2 : 1) + (tpInfo.getRegeneratorCost(t) * numRegeneratorsNeeded);

					cost_p.add (costOfLightpathOr11Pair);
					transponderType_p.add (t);
					lineRate_p.add(tpInfo.getLineRateGbps(t));
					numSlots_p.add(tpInfo.getNumSlots(t));
					ipDemand_p.add(ipDemand);
					seqLinks_p.add(firstPath);
					regPositions_p.add(regPositions1);
					if (cpl11 != null) { seqLinks2_p.add(secondPath); regPositions2_p.add(regPositions2); }
					atLeastOnePathOrPathPair = true;
				}
			}
			if (!atLeastOnePathOrPathPair) throw new Net2PlanException ("There are no possible routes (or 1+1 pairs) for a demand (" + ipDemand + "). The topology may be not connected enough, or the optical reach may be too small");
		}
		final int P = transponderType_p.size(); // one per potential sequence of links (or 1+1 pairs of sequences) and transponder

		/* Compute some important matrices for the formulation */
		DoubleMatrix2D A_dp = DoubleFactory2D.sparse.make(Dip,P); /* 1 is path p is assigned to demand d */
		DoubleMatrix2D A_ep = DoubleFactory2D.sparse.make(Ewdm,P); /* 1 if path (or primary path in 1+1) p travserses link e */
		DoubleMatrix2D A2_ep = DoubleFactory2D.sparse.make(Ewdm,P); /* 1 if backup path of pair p traverses link e */
		DoubleMatrix2D A_psrg = DoubleFactory2D.sparse.make(P,netPlan.getNumberOfSRGs()); /* 1 if path p fails when srg fails */
		double [][] feasibleAssignment_ps = new double [P][S];
		DoubleMatrix2D [] A_n1Mn2p = new DoubleMatrix2D [T];
		if (bidirectionalTransponders.getBoolean())
			for (int t = 0 ; t < T ; t ++) A_n1Mn2p [t] = DoubleFactory2D.sparse.make(N*N , P);
		for (int p = 0 ; p < P ; p ++)
		{
			A_dp.set(ipDemand_p.get(p).getIndex() , p , 1.0);
			for (Link e : seqLinks_p.get(p)) A_ep.set (e.getIndex() , p , 1.0);
			if (cpl11 != null) for (Link e : seqLinks2_p.get(p)) A2_ep.set (e.getIndex() , p , 1.0);
			if (networkRecoveryType.getString().equals("single-srg-tolerant-static-lp"))
				for (Link e : seqLinks_p.get(p))
				{
					for (SharedRiskGroup srg : e.getSRGs()) A_psrg.set(p,srg.getIndex(),1.0);
					for (SharedRiskGroup srg : e.getOriginNode().getSRGs()) A_psrg.set(p,srg.getIndex(),1.0);
					for (SharedRiskGroup srg : e.getDestinationNode().getSRGs()) A_psrg.set(p,srg.getIndex(),1.0);
				}
			for (int s = 0; s < S + 1 - numSlots_p.get(p) ; s ++)
				feasibleAssignment_ps [p][s] = 1;
			if (bidirectionalTransponders.getBoolean())
			{
				final int t = transponderType_p.get(p);
				final int n1 = ipDemand_p.get(p).getIngressNode().getIndex();
				final int n2 = ipDemand_p.get(p).getEgressNode().getIndex();
				final int nLower = n1 < n2? n1 : n2;
				final int nGreater = n1 > n2? n1 : n2;
				A_n1Mn2p [t].set(nLower + N * nGreater , p , nLower == n1? 1.0 : -1.0);
			}
		}
		
		
		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();

		/* Add the decision variables to the problem */
		/* p is the route index and s the initial slot. In 1+1, p is the 1+1 pair index (equal to the route and segment indexes), s the initial slot of the primary (route)   */
		op.addDecisionVariable("x_ps", true, new int[] {P, S}, new DoubleMatrixND (new int [] {P,S}) , new DoubleMatrixND (feasibleAssignment_ps)); /* 1 if lightpath d(p) is routed through path p in wavelength w */
		if (cpl11 != null)
			/* p is the 1+1 pair index (equal to the route and segment indexes), s the initial slot of the backup (segment)   */
			op.addDecisionVariable("x2_ps", true, new int[] {P, S}, new DoubleMatrixND (new int [] {P,S}) , new DoubleMatrixND (feasibleAssignment_ps)); /* 1 if lightpath d(p) is routed through path p in wavelength w */
		
		/* Set some input parameters */
		op.setInputParameter("S", S);
		op.setInputParameter("h_d", netPlan.getVectorDemandOfferedTraffic(ipLayer), "row");
		op.setInputParameter("rate_p", lineRate_p , "row");
		op.setInputParameter("c_p", cost_p , "row");
		op.setInputParameter("A_dp", A_dp);
		op.setInputParameter("A_ep", A_ep); // in 1+1, only occupation of the primaries (p is the 1+1 pair index, equal to route and segment indexes)
		if (cpl11 != null)
			op.setInputParameter("A2_ep", A2_ep); 
		
		/* Sets the objective function */
		if (optimizationTarget.getString().equals("min-cost"))
			op.setObjectiveFunction("minimize", "sum(c_p * x_ps)"); /* sum_ps (c_p · x_ps). In 1+1 the cost is multiplied by two */
		else if (optimizationTarget.getString().equals("maximin-fiber-number-idle-slots"))
		{
			op.addDecisionVariable("u", true, new int[] {1, 1} , 0 , S); /* Number of idle slots in the worst case fiber */
			op.setObjectiveFunction("maximize", "u");
			op.setInputParameter("numSlots_p", numSlots_p , "row");
			if (cpl11 == null)
				op.addConstraint("sum(A_ep * diag(numSlots_p) * x_ps , 2) <= S - u");
			else
				op.addConstraint("sum(A_ep * diag(numSlots_p) * x_ps + A2_ep * diag(numSlots_p) * x2_ps , 2) <= S - u");
		}
			
		/* Carry enough traffic in each demand to satisfy the minimum traffic to carry (no failure state) */
		/* sum_{s , p \in P_d} rate_p x_ps >= h_d, for each d */
		op.addConstraint("A_dp * diag (rate_p) * x_ps * ones([S; 1]) >= h_d'"); /* each lightpath d: is carried in exactly one p-w --> sum_{p in P_d, w} x_dp <= 1, for all d */
		if (cpl11 != null)
			op.addConstraint("sum(x_ps,2) == sum(x2_ps,2)"); /* The number of lightpath in primary of 1+1 pair p and inthe backup is the same  */

		/* (no 1+1, but fault tolerant) Carry enough traffic in each demand to satisfy the minimum traffic to carry, in all of the single-SRG failure states */
		if (networkRecoveryType.getString().equals("single-srg-tolerant-static-lp"))
		{
			/* sum_{s , p \in P_d, p survives} rate_p x_ps >= h_d, for each d, for each srg */
			for (int srg = 0 ; srg < A_psrg.columns() ; srg ++)
			{
				/* Asrg_pp_ Diagonal pxp matrix, with a one for surviving paths */
				DoubleMatrix2D Asrg_pp = DoubleFactory2D.sparse.make(P,P);
				for (int p = 0; p < P ; p ++) Asrg_pp.set(p,p,1 - A_psrg.get(p , srg));
				op.setInputParameter("Asrg_pp" , Asrg_pp);
				op.addConstraint("A_dp * Asrg_pp * diag (rate_p) * x_ps * ones([S; 1]) >= h_d'"); /* each lightpath d: is carried in exactly one p-w --> sum_{p in P_d, w} x_dp <= 1, for all d */
			}
		}
		
		/* Frequency-slot clashing */
		/* \sum_t \sum_{p \in P_e, sinit {s-numSlots(t),s} x_ps <= 1, for each e, s   */
		String constraintString = "";
		for (int t = 0; t < T ; t ++)
		{
			{
				final String name_At_pp = "A" + Integer.toString(t) + "_pp";
				final String name_At_s1s2 = "A" + Integer.toString(t) + "_s1s2";
				/* At_pp, diagonal matrix, 1 if path p is associated to a transponder of type t */
				DoubleMatrix2D At_pp = DoubleFactory2D.sparse.make(P,P);
				int p = 0; for (int type : transponderType_p) { if (type == t) At_pp.set(p,p,1.0); p++; }
				/* At_s1s2, upper triangular matrix, 1 if a transponder of type with initial slot s1, occupied slots s2 (depends on number of slots occupied) */
				DoubleMatrix2D At_s1s2 = DoubleFactory2D.sparse.make(S,S);
				for (int s1 = 0 ; s1 < S ; s1 ++)
					for (int cont = 0 ; cont < tpInfo.getNumSlots(t) ; cont ++)
						if (s1 - cont >= 0) At_s1s2.set(s1-cont,s1,1.0); 
				op.setInputParameter(name_At_pp, At_pp);
				op.setInputParameter(name_At_s1s2, At_s1s2);
				constraintString += (t == 0? "" : " + ") + "( A_ep * " + name_At_pp + " * x_ps * " + name_At_s1s2 + " ) "; 
			}

			if (cpl11 != null) // sum also the slots occupied by the 
			{
				final String name_A2t_pp = "A2" + Integer.toString(t) + "_pp";
				final String name_A2t_s1s2 = "A2" + Integer.toString(t) + "_s1s2";
				/* At_pp, diagonal matrix, 1 if path p is associated to a transponder of type t */
				DoubleMatrix2D A2t_pp = DoubleFactory2D.sparse.make(P,P);
				int p = 0; for (int type : transponderType_p) { if (type == t) A2t_pp.set(p,p,1.0); p++; }
				/* At_s1s2, upper triangular matrix, 1 if a transponder of type with initial slot s1, occupied slots s2 (depends on number of slots occupied) */
				DoubleMatrix2D A2t_s1s2 = DoubleFactory2D.sparse.make(S,S);
				for (int s1 = 0 ; s1 < S ; s1 ++)
					for (int cont = 0 ; cont < tpInfo.getNumSlots(t) ; cont ++)
						if (s1 - cont >= 0) A2t_s1s2.set(s1-cont,s1,1.0); 
				op.setInputParameter(name_A2t_pp, A2t_pp);
				op.setInputParameter(name_A2t_s1s2, A2t_s1s2);
				constraintString += " + ( A2_ep * " + name_A2t_pp + " * x2_ps * " + name_A2t_s1s2 + " ) "; 
			}
		}		
		op.addConstraint(constraintString + " <= 1"); /* wavelength-clashing constraints --> sum_{p in P_e, w} x_pw <= 1, for all e,w */

		/* Bidirectional constraints */
		if (bidirectionalTransponders.getBoolean())
		{
			for (int t = 0 ; t < T ; t ++)
			{
				op.setInputParameter("At_n1Mn2p" , A_n1Mn2p [t]);
				if (cpl11 != null)
					op.addConstraint("sum (At_n1Mn2p * (x_ps + x2_ps) , 2) == 0");
				else
					op.addConstraint("sum (At_n1Mn2p * x_ps , 2) == 0");
			}
		}
		
		/* Call the solver to solve the problem */
		op.solve(solverName.getString(), "solverLibraryName", solverLibraryName.getString() , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble());

		/* If a feasible solution was not found, quit (this may also happen if after the maximum solver time no feasible solution is found) */
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");

		/* Retrieve the optimum solutions */
		DoubleMatrix2D x_ps = op.getPrimalSolution("x_ps").view2D();
		DoubleMatrix2D x2_ps = cpl11 == null? null : op.getPrimalSolution("x2_ps").view2D();

		/* Create the lightpaths according to the solutions given */
		WDMUtils.setFibersNumFrequencySlots(netPlan , numFrequencySlotsPerFiber.getInt() , wdmLayer);
		IntArrayList slots = new IntArrayList (); DoubleArrayList vals = new DoubleArrayList ();
		IntArrayList slots2 = new IntArrayList (); DoubleArrayList vals2 = new DoubleArrayList ();
		for (int p = 0; p < P ; p ++)
		{
			slots.clear(); vals.clear();
			x_ps.viewRow(p).getNonZeros(slots , vals);
			if (cpl11 != null) 
			{ 
				slots2.clear(); vals.clear(); x2_ps.viewRow(p).getNonZeros(slots2 , vals2);
				if (slots.size() != slots2.size()) throw new RuntimeException ("Bad");
			} 
			if (slots.size() == 0) continue;
			
			final Demand ipDemand = ipDemand_p.get(p);
			for (int cont = 0 ; cont < slots.size() ; cont ++)
			{
				final int s = slots.get (cont);
				final Demand newWDMDemand = netPlan.addDemand(ipDemand.getIngressNode() , ipDemand.getEgressNode() , 
						lineRate_p.get(p) , null , wdmLayer);
				final Route r = WDMUtils.addLightpath(newWDMDemand , new WDMUtils.RSA(seqLinks_p.get(p) , s , numSlots_p.get(p) , regPositions_p.get(p)) , lineRate_p.get(p));
				final Link ipLink = newWDMDemand.coupleToNewLinkCreated(ipLayer);
				final double ipTrafficToCarry = Math.min(lineRate_p.get(p) , ipDemand.getBlockedTraffic());
				netPlan.addRoute(ipDemand , ipTrafficToCarry , ipTrafficToCarry , Arrays.asList(ipLink), null);
				if (cpl11 != null)
				{
					final int s2 = slots2.get(cont);
					final Route backupRoute = WDMUtils.addLightpath(newWDMDemand , new WDMUtils.RSA(seqLinks2_p.get(p) , s2 , numSlots_p.get(p) , regPositions2_p.get(p)) , 0);
					r.addBackupRoute(backupRoute);
				}
			}
		}
		
		WDMUtils.checkResourceAllocationClashing(netPlan,true,true,wdmLayer);

		return "Ok!";
	}

	private static double getLengthInKm (Collection<Link> r) { double res = 0; for (Link e : r) res += e.getLengthInKm(); return res; }
	
	@Override
	public String getDescription()
	{
		return "Algorithm based on an ILP solving the Routing, Spectrum, Modulation Assignment (RSMA) problem with regenerator placement, in flexi (elastic) or fixed grid optical WDM networks, with or without fault tolerance, latency and/or lightpath bidirectionality requisites (see the Javadoc for details).";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
}
