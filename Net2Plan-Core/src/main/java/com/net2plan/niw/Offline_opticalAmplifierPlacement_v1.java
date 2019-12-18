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
import java.util.Map.Entry;
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
	private InputParameter maxNumUsableOpticalSlotsPerFiber = new InputParameter ("maxNumUsableOpticalSlotsPerFiber", (int) 96  , "Number of optical slots to consider as maximum occupation." , 1 , Integer.MAX_VALUE);
	private InputParameter relativeCostPenalizationOla = new InputParameter ("relativeCostPenalizationOla", (double) 2.0 , "The cost of all amplifiers hardware is supposed to be the same. However, placing an amplifier in the line instead of as booster of pre-amplifier, and thus in the central office, can ha ve a penalization stated by this factor" , 0.0 , false , Double.MAX_VALUE , true);
	private InputParameter minGainDb = new InputParameter ("minGainDb", (double) 6.0 , "Minimum gain acceptable for the amplifiers" , 0.0 , false , Double.MAX_VALUE , true);
	private InputParameter maxGainDb = new InputParameter ("maxGainDb", (double) 6.0 , "Maximum gain acceptable for the amplifiers" , 0.0 , false , Double.MAX_VALUE , true);
	private InputParameter maxInterOlaDistanceKm = new InputParameter ("maxInterOlaDistanceKm", (double) 10.0 , "Maximum distance between two consecutive potential placements for line amplifiers in the same fiber" , 0.0 , false , Double.MAX_VALUE , true);
	private InputParameter initialPowerDensityPostBooster_dBmPerOpticalSlot = new InputParameter ("initialPowerDensityPostBooster_dBmPerOpticalSlot", (double) 0.0 , "The power density that will we enforced in the output of the booster amplifier, that always exists at the start of the line" , -Double.MAX_VALUE , false , Double.MAX_VALUE , false);
	private InputParameter maxOutputPoweramplifier_dBm = new InputParameter ("maxOutputPoweramplifier_dBm", (double) 20.0 , "Maximum acceptable output power at any amplifier. The design should enforce that this power is not exceeded in any " , 0.0 , false , Double.MAX_VALUE , true);
	private InputParameter relativeCostPenalizationOla = new InputParameter ("relativeCostPenalizationOla", (double) 2.0 , "The cost of all amplifiers hardware is supposed to be the same. However, placing an amplifier in the line instead of as booster of pre-amplifier, and thus in the central office, can ha ve a penalization stated by this factor" , 0.0 , false , Double.MAX_VALUE , true);

	
	private InputParameter overideBaseResourcesInfo = new InputParameter ("overideBaseResourcesInfo", true , "If true, the current resources in tne input n2p are removed, and for each node aone CPU, RAM and HD resources are created, with the capacities defined in input parameter defaultCPU_RAM_HD_Capacities");
	private InputParameter defaultCPU_RAM_HD_Capacities = new InputParameter ("defaultCPU_RAM_HD_Capacities", "100 100 100" , "The default capacity values (space separated) of CPU, RAM, HD");
	private InputParameter overideSequenceTraversedNFVs = new InputParameter ("overideSequenceTraversedNFVs", true , "If true, all demands will reset the sequence of NFVs to traverse, to this (NFV types in this param are ; separated)");
	private InputParameter defaultSequenceNFVsToTraverse = new InputParameter ("defaultSequenceNFVsToTraverse", "FW NAT" , "The default sequence of NFVs that demands must traverse");
	private InputParameter solverName = new InputParameter ("solverName", "#select# cplex glpk xpress", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");

	
//	int maxNumUsableOpticalSlotsPerFiber , double relativeCostPenalizationOla ,
//	double maxInterOlaDistanceKm , 
//	double initialPowerDensityPostBooster_dBmPerOpticalSlot ,
//	double maxOutputPoweramplifier_dBm , 
//	double minPowerDensityAtTransponderDrop_dBmPerOpticalSlot , 
//	double maxTransponderInjectionPowerDensity_dBmPerOpticalSlot)

	
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
		if (wNet.getFibers().stream().anyMatch(e->e.isOriginOadmConfiguredToEqualizeOutput())) throw new Net2PlanException ("Channel power cannot be power equalized in the OADMs");
		
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
		
	private void placeAmplifiersAndInjectionPower (List<WFiber> line ,  
			int maxNumUsableOpticalSlotsPerFiber , double relativeCostPenalizationOla ,
			double minGainDb , double maxGainDb , 
			double maxInterOlaDistanceKm , 
			double initialPowerDensityPostBooster_dBmPerOpticalSlot ,
			double maxOutputPoweramplifier_dBm , 
			double minPowerDensityAtTransponderDrop_dBmPerOpticalSlot , 
			double maxTransponderInjectionPowerDensity_dBmPerOpticalSlot)
	{	
		final int E = line.size();
		if (line.isEmpty()) throw new Net2PlanException ("Empty line");
		final LinkedList<WNode> seqNodesButFirst = line.stream().map(e->e.getB()).collect(Collectors.toCollection(LinkedList::new));
		final List<WNode> seqNodes = new ArrayList<> (); seqNodes.add(line.get(0).getA()); seqNodes.addAll(seqNodesButFirst);
		if (seqNodes.size() != new HashSet<> (seqNodes).size()) throw new Net2PlanException ("A node cannot be traversed more than once");
		final int N = seqNodes.size();
		if (seqNodes.stream().anyMatch(n->!n.getOpticalSwitchingArchitecture().isOadmGeneric())) throw new Net2PlanException ("All OADMs must be of the generic type");
		final WNode lastNodeOfTheLine = line.get(line.size()-1).getB();
		
		
		/* Potential amplifier positions */
		final List<Pair<WFiber , Double>> oaPositionsKmFromFiberOrigin_p = new ArrayList<> ();
		final List<Double> fixedAttenuationBeforeOaDb_p = new ArrayList<> ();
		final Function<Pair<WFiber,Double>,Boolean> isBooster = p->p.getSecond() == 0;
		final Function<Pair<WFiber,Double>,Boolean> isPreamplifier = p->p.getSecond() == p.getFirst().getLengthInKm();
		final Function<Pair<WFiber,Double>,Boolean> isOla = p->!isBooster.apply(p) && !isPreamplifier.apply(p);
		final SortedMap<WNode , Integer> fromOriginAfterBoosterToInputOadmAfterPreamplifier_alreadyTraversedOas= new TreeMap<> ();
		final SortedMap<WNode , Double > fromOriginAfterBoosterToInputOadmAfterPreamplifier_attenuationDb = new TreeMap<> ();
		double attenuationFromOriginAfterBoosterToAfterPreamp_dB = 0;
		for (int contFiber = 0; contFiber < line.size() ; contFiber ++)
		{
			final WFiber e = line.get(contFiber);
			
			if (!e.equals(line.get(0)))
			{
				oaPositionsKmFromFiberOrigin_p.add(Pair.of(e, 0.0)); // BOOSTER (IF NOT FIRST LINK)
				fixedAttenuationBeforeOaDb_p.add(attenuationFromOriginAfterBoosterToAfterPreamp_dB);
			}
			
			final double fiberAttenuationDb = e.getLengthInKm() * e.getAttenuationCoefficient_dbPerKm();
			final int numOlas = (int) Math.ceil(e.getLengthInKm() / maxInterOlaDistanceKm) - 1;
			if (numOlas > 0) // OLAS
			{
				final double interOlaDistanceKm = e.getLengthInKm() / (numOlas + 1);
				assert interOlaDistanceKm <= maxInterOlaDistanceKm;
				for (int olaIndex = 0; olaIndex < numOlas ; olaIndex ++)
				{
					final double positionInFiber_km = (olaIndex + 1) * interOlaDistanceKm;
					oaPositionsKmFromFiberOrigin_p.add(Pair.of(e,  positionInFiber_km));
					fixedAttenuationBeforeOaDb_p.add(attenuationFromOriginAfterBoosterToAfterPreamp_dB + positionInFiber_km * e.getAttenuationCoefficient_dbPerKm());
				}
			}
			oaPositionsKmFromFiberOrigin_p.add(Pair.of(e, e.getLengthInKm())); // PREAMPLIFIER
			fixedAttenuationBeforeOaDb_p.add(attenuationFromOriginAfterBoosterToAfterPreamp_dB + e.getLengthInKm() * e.getAttenuationCoefficient_dbPerKm());
			
			attenuationFromOriginAfterBoosterToAfterPreamp_dB += fiberAttenuationDb;
			final WNode dropNode = e.getB();
			final OadmArchitecture_generic oadmDropNode = (OadmArchitecture_generic) dropNode.getOpticalSwitchingArchitecture();
			fromOriginAfterBoosterToInputOadmAfterPreamplifier_alreadyTraversedOas.put(dropNode, oaPositionsKmFromFiberOrigin_p.size());
			fromOriginAfterBoosterToInputOadmAfterPreamplifier_attenuationDb.put(dropNode, attenuationFromOriginAfterBoosterToAfterPreamp_dB);
			if (contFiber != line.size()-1)
			{
				final WFiber nextFiber = e.getB().getOutgoingFibers().stream().filter(ee->!ee.equals(e.getBidirectionalPair())).findFirst().get();
				attenuationFromOriginAfterBoosterToAfterPreamp_dB += oadmDropNode.getExpressAttenuation_dB(e, nextFiber).get();
			}
		}
		final int P = oaPositionsKmFromFiberOrigin_p.size();
		assert fixedAttenuationBeforeOaDb_p.size() == P;

		/* Just a check */
		final double checkTotalAttenuation_1 = fromOriginAfterBoosterToInputOadmAfterPreamplifier_attenuationDb.get(seqNodesButFirst.getLast()); 
		double checkTotalAttenuation_2 = 0;
		for (int contFiber = 0; contFiber < line.size() ; contFiber ++)
		{
			final WFiber e = line.get(contFiber);
			final WFiber next = contFiber == line.size() - 1? null : line.get(contFiber + 1);
			checkTotalAttenuation_2 += e.getLengthInKm() * e.getAttenuationCoefficient_dbPerKm();
			checkTotalAttenuation_2 += contFiber == line.size() - 1? 0.0 : e.getB().getOpticalSwitchingArchitecture().getAsOadmGeneric ().getExpressAttenuation_dB(e, next).get();
		}
		assert Math.abs(checkTotalAttenuation_1 - checkTotalAttenuation_2) < 1e-3;

		
		/*************************************************************************************************************************/
		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();
		
		op.addDecisionVariable("gain_p", false, new int [] {1 , P} , 0 , maxGainDb);
		op.addDecisionVariable("placeOa_p", true, new int [] {1 , P} , 0 , 1);
		final double maxInitialPowerPostBooster_dBm = OpticalSimulationModule.linear2dB (OpticalSimulationModule.dB2linear(initialPowerDensityPostBooster_dBmPerOpticalSlot) * maxNumUsableOpticalSlotsPerFiber);
		op.setInputParameter("initialPowerDensityPostBooster_dBmPerOpticalSlot", initialPowerDensityPostBooster_dBmPerOpticalSlot);
		op.setInputParameter("maxNumUsableOpticalSlotsPerFiber", maxNumUsableOpticalSlotsPerFiber);
		op.setInputParameter("maxInitialPowerPostBooster_dBm", maxInitialPowerPostBooster_dBm);
		op.setInputParameter("minGainDb", minGainDb);
		op.setInputParameter("maxGainDb", maxGainDb);
		op.setInputParameter("cost_p", oaPositionsKmFromFiberOrigin_p.stream().map(p->isOla.apply(p)? relativeCostPenalizationOla : 1.0).collect(Collectors.toList()) , "row");
		op.setInputParameter("lossToCompensate_dB", fromOriginAfterBoosterToInputOadmAfterPreamplifier_attenuationDb.get(lastNodeOfTheLine));
		op.setInputParameter("maxOutputPoweramplifier_dBm", maxOutputPoweramplifier_dBm);
		op.setInputParameter("minPowerDensityAtTransponderDrop_dBmPerOpticalSlot", minPowerDensityAtTransponderDrop_dBmPerOpticalSlot);
		op.setInputParameter("maxTransponderInjectionPowerDensity_dBmPerOpticalSlot", maxTransponderInjectionPowerDensity_dBmPerOpticalSlot);
		op.setInputParameter("fixedAttenuationBeforeOaDb_p", fixedAttenuationBeforeOaDb_p , "row");

		op.setObjectiveFunction("minimize", "cost_p * placeOa_p'");
		
		op.addConstraint("gain_p <= maxGainDb * placeOa_p"); // no amplifier => no gain, and if amplifier, below max gain
		op.addConstraint("gain_p >= minGainDb * placeOa_p"); // amplifier => greater than min gain
		op.addConstraint("sum (gain_p) == lossToCompensate_dB"); // no amplifier => no gain
		for (int p = 0 ; p < P ; p ++)
		{
			final double [] traversedOasBefore_p = new double [P]; for (int pp = 0; pp < p ; pp ++) traversedOasBefore_p [pp] = 1.0;
			op.setInputParameter("bigNumber", Math.abs (maxInitialPowerPostBooster_dBm + P * maxGainDb - fixedAttenuationBeforeOaDb_p.get(p) - maxOutputPoweramplifier_dBm) * 2);
			op.setInputParameter("p", p);
			op.setInputParameter("traversedOasBefore_p", traversedOasBefore_p , "row");

			
			/* Not OA output power saturation */
			op.addConstraint("maxInitialPowerPostBooster_dBm + traversedOasBefore_p * gain_p' - fixedAttenuationBeforeOaDb_p(p) <= maxOutputPoweramplifier_dBm + (1-placeOa_p(p)) * bigNumber"); // amplifier output power is not saturating
			
			/* Enough power density at DROP */
			if (isPreamplifier.apply(oaPositionsKmFromFiberOrigin_p.get(p)))
			{
				final WFiber inFiber = oaPositionsKmFromFiberOrigin_p.get(p).getFirst();
				final WNode dropOrAddNode = inFiber.getB();

				/* Power at drop is enough */
				final double fixedOadmDropAttenuationAfterPreamp_dB = dropOrAddNode.getOpticalSwitchingArchitecture().getDropAttenuation_dB(inFiber , Optional.empty());
				op.setInputParameter("fixedOadmDropAttenuationAfterPreamp_dB", fixedOadmDropAttenuationAfterPreamp_dB);
				op.addConstraint("initialPowerDensityPostBooster_dBmPerOpticalSlot + traversedOasBefore_p * gain_p'"
						+ " - fixedAttenuationBeforeOaDb_p(p) + gain_p(p) - fixedOadmDropAttenuationAfterPreamp_dB "
						+ ">= minPowerDensityAtTransponderDrop_dBmPerOpticalSlot"); // amplifier output power is not saturating

				/* Power at add can be balanced, when outgoing for any out fiber */
				for (WFiber outFiber : dropOrAddNode.getOutgoingFibers())
				{
					if (outFiber.equals(inFiber.getBidirectionalPair())) continue;
					final double fixedOadmExpressAttenuationAfterPreamp_dB = dropOrAddNode.getOpticalSwitchingArchitecture().getExpressAttenuation_dB(inFiber , outFiber).get();
					final double fixedOadmAddAttenuation_dB = dropOrAddNode.getOpticalSwitchingArchitecture().getAddAttenuation_dB(outFiber, Optional.empty(), 1);
					op.setInputParameter("fixedOadmExpressAttenuationAfterPreamp_dB", fixedOadmExpressAttenuationAfterPreamp_dB);
					op.setInputParameter("fixedOadmAddAttenuation_dB", fixedOadmAddAttenuation_dB);

					op.addConstraint("initialPowerDensityPostBooster_dBmPerOpticalSlot + traversedOasBefore_p * gain_p'"
							+ " - fixedAttenuationBeforeOaDb_p(p) + gain_p(p) - fixedOadmExpressAttenuationAfterPreamp_dB "
							+ ">= maxTransponderInjectionPowerDensity_dBmPerOpticalSlot - fixedOadmAddAttenuation_dB"); // amplifier output power is not saturating
				}
				
			}
		}
		
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());
	
		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		

		/*************************************************************************************************************************/
		/************************ CREATE THE OUTPUT DESIGN FROM THE SOLUTION ****************************************************************/
		/*************************************************************************************************************************/
		final List<Double> gain_p = op.getPrimalSolution("gain_p").toList();
		final List<Double> placeOa_p = op.getPrimalSolution("placeOa_p").toList();
		
		/* Check in decision variables and constraints  */
		for (int p = 0; p < P ; p ++)
		{
			final boolean hasOa = placeOa_p.get(p) > 1e-1;
			final double gainDb = gain_p.get(p); 
			if (!hasOa &&  gainDb > 1e-3) throw new RuntimeException();
			if (hasOa && gainDb < minGainDb - 1e-3) throw new RuntimeException();
			if (hasOa && gainDb > maxGainDb + 1e-3) throw new RuntimeException();
		}


		/* Remove all the optical amplifiers */
		for (WFiber fiber : line)
		{
			fiber.removeOpticalLineAmplifiers();
			fiber.setIsExistingBoosterAmplifierAtOriginOadm(false);
			fiber.setIsExistingPreamplifierAtDestinationOadm(false);
		}
		final SortedMap<WFiber , List<OpticalAmplifierInfo>> olasPerFiber = new TreeMap<> ();
		for (int p = 0; p < P ; p ++)
		{
			final WFiber fiber = oaPositionsKmFromFiberOrigin_p.get(p).getFirst();
			final double positionFromFiberOrigin_km = oaPositionsKmFromFiberOrigin_p.get(p).getSecond();
			final boolean amplifierExists = placeOa_p.get(p) > 1e-3;
			if (!amplifierExists) continue;
			
			/* Set existence or not */
			final OpticalAmplifierInfo info;
			if (isBooster.apply(oaPositionsKmFromFiberOrigin_p.get(p))) info = OpticalAmplifierInfo.getDefaultBooster();
			else if (isPreamplifier.apply(oaPositionsKmFromFiberOrigin_p.get(p))) info = OpticalAmplifierInfo.getDefaultPreamplifier();
			else info = OpticalAmplifierInfo.getDefaultOla(positionFromFiberOrigin_km);
			
			info.setGainDb(gain_p.get(p));
			info.setMaxAcceptableGainDb(maxGainDb);
			info.setMinAcceptableGainDb(minGainDb);
			info.setMaxAcceptableOutputPower_dBm(maxOutputPoweramplifier_dBm);

			if (isBooster.apply(oaPositionsKmFromFiberOrigin_p.get(p)))
			{
				fiber.setIsExistingBoosterAmplifierAtOriginOadm(true);
				fiber.setOriginBoosterAmplifierInfo(info);
			}
			else if (isPreamplifier.apply(oaPositionsKmFromFiberOrigin_p.get(p))) 
			{
				fiber.setIsExistingPreamplifierAtDestinationOadm(true);
				fiber.setDestinationPreAmplifierInfo(info);
			}
			else 
			{
				List<OpticalAmplifierInfo> olasThisFiber = olasPerFiber.get(fiber);
				if (olasThisFiber == null) { olasThisFiber = new ArrayList<> (); olasPerFiber.put(fiber, olasThisFiber); }
				olasThisFiber.add(info);
			}
		}
		/* Add OLAs */
		for (Entry<WFiber,List<OpticalAmplifierInfo>> entry : olasPerFiber.entrySet())
			entry.getKey().setOlaTraversedInfo(entry.getValue());
		
		/* */
		PENDIENTE: CHEQUEAR QUE LA SOLUCION CUMPLE LO QUE QUEREMOS EN CUANTO A POWER, USANDO FUNCIONES DEL OPTICAL SIMULATION SI ES POSIBLE

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
