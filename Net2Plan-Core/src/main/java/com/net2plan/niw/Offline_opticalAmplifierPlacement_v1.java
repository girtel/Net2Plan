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
package com.net2plan.niw;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
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

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DirectedMultigraph;

import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

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
public class Offline_opticalAmplifierPlacement_v1 implements IAlgorithm
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

		/* Initialize variables */
		final int N = wNet.getNodes().size();

		if (wNet.getFibers().stream().anyMatch(e->!e.isBidirectional())) throw new Net2PlanException ("Fibers must be bidirectional");
		
		final SortedSet<WNode> threePlusDegreeOadms = new TreeSet<> ();
		final List<List<WFiber>> lines = new ArrayList<> ();
		final Function<WFiber , List<WFiber>> getLineStartingFrom = e->
		{
			final List<WFiber> thisLine = new ArrayList<> ();
			WFiber current = e;
			while (true)
			{
				thisLine.add(current);
				if (current.getB().getOutgoingFibers().size() > 2) break; // reaches a degree 3 or 4 OADM
				if (current.getB().getOutgoingFibers().size() <= 1) break; // end of line
				current = current.getB().getOutgoingFibers().stream().filter(ee->!ee.equals(current.getBidirectionalPair())).findFirst().get();
			}
			return thisLine;
		};
		/* lines starting or ending in multidegrees and/or leafs */
		for (WNode n : wNet.getNodes())
		{
			if (n.getOutgoingFibers().size() > 2 || n.getIncomingFibers().size() > 2)
			{
				threePlusDegreeOadms.add(n);
				for (WFiber outGoing : n.getOutgoingFibers())
					lines.add(getLineStartingFrom.apply(outGoing));
			}
		}
		/* Search for rings. There, break it in the first OADM which is not filterless, or if none, in the first */
		final Graph<WNode,WFiber> degree12Graph = new DirectedMultigraph<>(WFiber.class);
		for (WNode n : wNet.getNodes())
			if (n.getOutgoingFibers().size() <= 2) 
				degree12Graph.addVertex(n);
		for (WFiber e : wNet.getFibers())
			if (e.getA().getOutgoingFibers().size() <= 2 && e.getB().getOutgoingFibers().size() <= 2)
				degree12Graph.addEdge(e.getA(), e.getB() , e);
		final ConnectivityInspector<WNode, WFiber> connInspector = new ConnectivityInspector<>(degree12Graph);
		for (Set<WNode> potentialRing : connInspector.connectedSets())
		{
			final boolean isRing = potentialRing.stream().allMatch(e->e.getOutgoingFibers().size() == 2);
			if (isRing)
			{
				final SortedSet<WNode> orderedRing = new TreeSet<> (potentialRing);
				final WNode breakingNode = orderedRing.stream().filter(n->n.getOpticalSwitchingArchitecture().isNeverCreatingWastedSpectrum()).findFirst().orElse(orderedRing.first());
				for (WFiber initialFiber : breakingNode.getOutgoingFibers())
					lines.add(getLineStartingFrom.apply(initialFiber));
			}
		}
		

		/* Everything is separated into lines. For each line:
		 * 1) Place amplifiers & set gains to all.
		 * 2) Set added lightpaths injection power, so 
		 * --- Sensitivity target of the transponders are met
		 * --- Injection power of 
		 *  */
		// sum of all the gains equals the line attenuation
		// drop power is above margin for all (below no problem: we add attenuators)
		// BALANCE: intermediate nodes can inject at the same power as observed all the injection powers are the same, potentially we have attenuations => 
		for (List<WFiber> line : lines)
			placeAmplifiersAndInjectionPower (line);
			
		return "Ok";
	}
		
	private void placeAmplifiersAndInjectionPower (List<WFiber> line , double relativeCostPenalizationOla ,
			double minGainDb , double maxGainDb , double maxInterOlaDistanceKm , double initialLinePowerPostBooster_dBm , 
			double minPowerAtTransponderDrop_dBm , double maxTransponderInjectionPower_dBm)
	{	
		final int E = line.size();
		if (line.isEmpty()) throw new Net2PlanException ("Empty line");
		final LinkedList<WNode> seqNodesButFirst = line.stream().map(e->e.getB()).collect(Collectors.toCollection(LinkedList::new));
		final List<WNode> seqNodes = new ArrayList<> (); seqNodes.add(line.get(0).getA()); seqNodes.addAll(seqNodesButFirst);
		if (seqNodes.size() != new HashSet<> (seqNodes).size()) throw new Net2PlanException ("A node cannot be traversed more than once");
		final int N = seqNodes.size();
		if (seqNodesButFirst.stream().anyMatch(n->!n.getOpticalSwitchingArchitecture().isOadmGeneric())) throw new Net2PlanException ("All OADMs must be of the generic type");
		final WNode lastNodeOfTheLine = line.get(line.size()-1).getB();
		
		
		/* Potential amplifier positions */
		final List<Pair<WFiber , Double>> oaPositions_nkm = new ArrayList<> ();
		final Function<Pair<WFiber,Double>,Boolean> isBooster = p->p.getSecond() == 0;
		final Function<Pair<WFiber,Double>,Boolean> isPreamplifier = p->p.getSecond() == p.getFirst().getLengthInKm();
		final Function<Pair<WFiber,Double>,Boolean> isOla = p->!isBooster.apply(p) && !isPreamplifier.apply(p);
		final SortedMap<WNode , Pair<Integer , Double>> numOaPositionsAndAttenuationFromOriginAfterBoosterToTransponderOadmInputAfterPreamplifier_dB = new TreeMap<> ();
		double attenuationFromOriginAfterBoosterToAfterPreamp_dB = 0;
		for (WFiber e : line)
		{
			if (!e.equals(line.get(0))) oaPositions_nkm.add(Pair.of(e, 0.0)); // BOOSTER (IF NOT FIRST LINK)
			final double fiberAttenuationDb = e.getLengthInKm() * e.getAttenuationCoefficient_dbPerKm();
			final int numOlas = (int) Math.ceil(e.getLengthInKm() / maxInterOlaDistanceKm) - 1;
			if (numOlas > 0) // OLAS
			{
				final double interOlaDistanceKm = e.getLengthInKm() / (numOlas + 1);
				assert interOlaDistanceKm <= maxInterOlaDistanceKm;
				for (int olaIndex = 0; olaIndex < numOlas ; olaIndex ++)
					oaPositions_nkm.add(Pair.of(e, (olaIndex + 1) * interOlaDistanceKm ));
			}
			oaPositions_nkm.add(Pair.of(e, e.getLengthInKm())); // PREAMPLIFIER
			
			attenuationFromOriginAfterBoosterToAfterPreamp_dB += fiberAttenuationDb;
			final WNode dropNode = e.getB();
			final OadmArchitecture_generic oadmDropNode = (OadmArchitecture_generic) dropNode.getOpticalSwitchingArchitecture();
			numOaPositionsAndAttenuationFromOriginAfterBoosterToTransponderOadmInputAfterPreamplifier_dB.put(dropNode, Pair.of(oaPositions_nkm.size () , attenuationFromOriginAfterBoosterToAfterPreamp_dB));
			attenuationFromOriginAfterBoosterToAfterPreamp_dB += oadmDropNode.getParameters().getExpressAttenuationForFiber0ToFiber0_dB().get();
		}
		assert numOaPositionsAndAttenuationFromOriginAfterBoosterToTransponderOadmInputAfterPreamplifier_dB.get(seqNodesButFirst.getLast()).getSecond() == 
				line.stream().mapToDouble(e->e.getLengthInKm() * e.getAttenuationCoefficient_dbPerKm()).sum() + 
				seqNodesButFirst.stream().filter(n->!n.equals(lastNodeOfTheLine)).mapToDouble(n-> n.getOpticalSwitchingArchitecture().getAsOadmGeneric ().getParameters().getExpressAttenuationForFiber0ToFiber0_dB().get()).sum();
		final int P = oaPositions_nkm.size();
		
		/*************************************************************************************************************************/
		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();
		
		op.addDecisionVariable("gain_p", false, new int [] {1 , P} , 0 , maxGainDb);
		op.addDecisionVariable("placeOa_p", true, new int [] {1 , P} , 0 , 1);
		
		op.setInputParameter("INITIALLINEPOWERPOSTBOOSTER_DBM", initialLinePowerPostBooster_dBm);
		op.setInputParameter("MINGAIN_DB", minGainDb);
		op.setInputParameter("MAXGAIN_DB", maxGainDb);
		op.setInputParameter("COST_P", oaPositions_nkm.stream().map(p->isOla.apply(p)? relativeCostPenalizationOla : 1.0).collect(Collectors.toList()) , "row");
		op.setInputParameter("TOTALLOSS_DB", oaPositions_nkm.stream().map(p->isOla.apply(p)? relativeCostPenalizationOla : 1.0).collect(Collectors.toList()) , "row");
		op.setInputParameter("minPowerAtTransponderDrop_dBm", minPowerAtTransponderDrop_dBm);
		op.setInputParameter("maxTransponderInjectionPower_dBm", maxTransponderInjectionPower_dBm);

		op.setObjectiveFunction("minimize", "COST_P * placeOa_p'");
		
		op.addConstraint("gain_p <= MAXGAIN_DB * placeOa_p"); // no amplifier => no gain, and if amplifier, below max gain
		op.addConstraint("gain_p >= MINGAIN_DB * placeOa_p"); // amplifier => greater than min gain
		op.addConstraint("sum (gain_p) == TOTALLOSS_DB"); // no amplifier => no gain
		for (int contFiber = 0 ; contFiber < line.size() ; contFiber ++)
		{
			final WFiber inFiber = line.get(contFiber);
			final WNode dropOrAddNode = inFiber.getB();
			
			/* Power at drop is enough */
			final int numOasBeforeDrop = numOaPositionsAndAttenuationFromOriginAfterBoosterToTransponderOadmInputAfterPreamplifier_dB.get(dropOrAddNode).getFirst();
			final double fixedAttenuationAfterPreamp_dB = numOaPositionsAndAttenuationFromOriginAfterBoosterToTransponderOadmInputAfterPreamplifier_dB.get(dropOrAddNode).getSecond();
			final double fixedOadmDropAttenuationAfterPreamp_dB = dropOrAddNode.getOpticalSwitchingArchitecture().getDropAttenuation_dB(inFiber , Optional.empty());
			final double [] affectedOas_p = new double [P]; for (int p = 0 ; p < numOasBeforeDrop ; p ++) affectedOas_p [p] = 1.0;
			op.setInputParameter("affectingOas_p", affectedOas_p , "row");
			op.setInputParameter("fixedAttenuationAfterPreamp_dB", fixedAttenuationAfterPreamp_dB);
			op.setInputParameter("fixedOadmDropAttenuationAfterPreamp_dB", fixedOadmDropAttenuationAfterPreamp_dB);
			op.addConstraint("INITIALLINEPOWERPOSTBOOSTER_DBM + (affectingOas_p * gain_p') - fixedAttenuationAfterPreamp_dB - fixedOadmDropAttenuationAfterPreamp_dB >= minPowerAtTransponderDrop_dBm");

			/* Power at add can be balanced, when outgoing for any out fiber */
			for (WFiber outFiber : dropOrAddNode.getOutgoingFibers())
			{
				if (outFiber.equals(inFiber.getBidirectionalPair())) continue;
				if (outFiber.isOriginOadmConfiguredToEqualizeOutput()) continue; // in this case, power balance is assured here because of this
				final double fixedOadmExpressAttenuationAfterPreamp_dB = dropOrAddNode.getOpticalSwitchingArchitecture().getExpressAttenuation_dB(inFiber , outFiber , 1);
				final double fixedOadmAddAttenuation_dB = dropOrAddNode.getOpticalSwitchingArchitecture().getAddAttenuation_dB(outFiber, Optional.empty(), 1);
				op.setInputParameter("fixedOadmExpressAttenuationAfterPreamp_dB", fixedOadmExpressAttenuationAfterPreamp_dB);
				op.setInputParameter("fixedOadmAddAttenuation_dB", fixedOadmAddAttenuation_dB);
				op.addConstraint("INITIALLINEPOWERPOSTBOOSTER_DBM + (affectingOas_p * gain_p') - fixedAttenuationAfterPreamp_dB - fixedOadmExpressAttenuationAfterPreamp_dB <= maxTransponderInjectionPower_dBm - fixedOadmAddAttenuation_dB");
			}
		}
		
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());
	
		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		

		/*************************************************************************************************************************/
		/************************ CREATE THE OUTPUT DESIGN FROM THE SOLUTION ****************************************************************/
		/*************************************************************************************************************************/
		final DoubleMatrix2D xx_scr_eup = op.getPrimalSolution("xx_scr_eup").view2D();
		
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

	@Override
	public String getDescription()
	{
		return "Algorithm for optical amplifier placement";
	}
	
	
	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

}
