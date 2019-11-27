package com.net2plan.niw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jgrapht.alg.cycle.DirectedSimpleCycles;
import org.jgrapht.alg.cycle.JohnsonSimpleCycles;
import org.jgrapht.graph.DirectedMultigraph;

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

public class OsmLightpathOccupationInfo
{
	private final List<WFiber> legitimate_seqLinks;
	private final Optional<Pair<WNode,Integer>> legitimate_addDirlessModule;
	private final Optional<Pair<WNode,Integer>> legitimate_dropDirlessModule;
	private final SortedSet<Integer> occupiedSlots;
	private List<Pair<WNode,Integer>> waste_addDirlessModules = null;
	private List<Pair<WNode,Integer>> waste_dropDirlessModules = null;
	private SortedSet<WFiber> waste_fibers = null;
	public OsmLightpathOccupationInfo(List<WFiber> legitimate_seqLinks,
			Optional<Pair<WNode, Integer>> legitimate_addDirlessModule,
			Optional<Pair<WNode, Integer>> legitimate_dropDirlessModule,
			SortedSet<Integer> occupiedSlots) 
	{
		this.legitimate_seqLinks = legitimate_seqLinks;
		this.legitimate_addDirlessModule = legitimate_addDirlessModule;
		this.legitimate_dropDirlessModule = legitimate_dropDirlessModule;
		this.occupiedSlots = occupiedSlots;
	}
	
	public boolean isMyLegitimateClashingWithLegitimateOrWasteSignalOf (OsmLightpathOccupationInfo otherLpInterferingMyLegitimate)
	{
		for (WFiber e : this.getSeqFibersLegitimateSignal())
		{
			if (otherLpInterferingMyLegitimate.getSeqFibersLegitimateSignal().contains(e))
				return true;
			if (otherLpInterferingMyLegitimate.getFibersWithWasteSignal().contains(e))
				return true;
		}
		if (this.getDirectionlessAddModuleLegitimateSignal().isPresent())
		{
			if (this.getDirectionlessAddModuleLegitimateSignal().equals(otherLpInterferingMyLegitimate.getDirectionlessAddModuleLegitimateSignal())) 
				return true;
			if (otherLpInterferingMyLegitimate.getAddDirectionlessModulesWithWasteSignal().contains (this.getDirectionlessAddModuleLegitimateSignal().get()))
				return true;
		}
		if (this.getDirectionlessDropModuleLegitimateSignal().isPresent())
		{
			if (this.getDirectionlessDropModuleLegitimateSignal().equals(otherLpInterferingMyLegitimate.getDirectionlessDropModuleLegitimateSignal())) 
				return true;
			if (otherLpInterferingMyLegitimate.getDropDirectionlessModulesWithWasteSignal().contains (this.getDirectionlessDropModuleLegitimateSignal().get()))
				return true;
		}
		return false;
	}

	public boolean isMutuallyClashingFreeWith (OsmLightpathOccupationInfo otherLp)
	{
		if (this.isMyLegitimateClashingWithLegitimateOrWasteSignalOf (otherLp)) return false;
		if (otherLp.isMyLegitimateClashingWithLegitimateOrWasteSignalOf (this)) return false;
		return true;
	}
	
	public void resetWasteOccupationInfo () { this.waste_addDirlessModules = null;  this.waste_dropDirlessModules = null; this.waste_fibers = null; }
	public SortedSet<WFiber> getFibersWithWasteSignal () 
	{
		if (waste_fibers == null) this.updateWasteOccupationInfo();
		return Collections.unmodifiableSortedSet(this.waste_fibers);
	}
	public List<Pair<WNode,Integer>> getAddDirectionlessModulesWithWasteSignal () 
	{
		if (waste_fibers == null) this.updateWasteOccupationInfo();
		return Collections.unmodifiableList(this.waste_addDirlessModules);
	}
	public List<Pair<WNode,Integer>> getDropDirectionlessModulesWithWasteSignal () 
	{
		if (waste_fibers == null) this.updateWasteOccupationInfo();
		return Collections.unmodifiableList(this.waste_dropDirlessModules);
	}
	public List<WFiber> getSeqFibersLegitimateSignal () { return Collections.unmodifiableList(this.legitimate_seqLinks); }
	public Optional<Pair<WNode,Integer>> getDirectionlessAddModuleLegitimateSignal () { return this.legitimate_addDirlessModule; }
	public Optional<Pair<WNode,Integer>> getDirectionlessDropModuleLegitimateSignal () { return this.legitimate_dropDirlessModule; }

	public boolean isWithFiberCyclesInLegitimateSignal ()
	{
		return legitimate_seqLinks.size() != new HashSet<> (legitimate_seqLinks).size();
	}
	public boolean isWithSelfClashing ()
	{
		if (isWithFiberCyclesInLegitimateSignal()) return true;
		final SortedSet<WFiber> wasteFibers = getFibersWithWasteSignal();
		for (WFiber e : getSeqFibersLegitimateSignal()) if (wasteFibers.contains(e)) return true;
		if (getDirectionlessAddModuleLegitimateSignal().isPresent())
			if (getAddDirectionlessModulesWithWasteSignal().contains(getDirectionlessAddModuleLegitimateSignal().get()))
				return true;
		if (getDirectionlessDropModuleLegitimateSignal().isPresent())
			if (getDropDirectionlessModulesWithWasteSignal().contains(getDirectionlessDropModuleLegitimateSignal().get()))
				return true;
		return false;
	}
	public SortedSet<Integer> getOccupiedSlotIds () { return this.occupiedSlots; }
	private void updateWasteOccupationInfo ()
	{
   		this.waste_addDirlessModules = new ArrayList<> ();
   		this.waste_dropDirlessModules = new ArrayList<>();
   		this.waste_fibers = new TreeSet<> ();
   		
		final List<WFiber> leg_fibers = new ArrayList<> (getSeqFibersLegitimateSignal());
		if (leg_fibers.isEmpty()) throw new Net2PlanException ("The path is empty");
	   	if (OpticalSpectrumManager.getContinousSequenceOfNodes(leg_fibers).stream().allMatch(n->n.getOpticalSwitchingArchitecture().isNeverCreatingWastedSpectrum()))
	   		return; // all empty
	   	 
	   	 final OsmOpticalSignalPropagationElement dummyFiberAdd = this.getDirectionlessAddModuleLegitimateSignal().isPresent()? OsmOpticalSignalPropagationElement.asAddDirless(this.getDirectionlessAddModuleLegitimateSignal().get()) : OsmOpticalSignalPropagationElement.asAddDirful();
	   	 final OsmOpticalSignalPropagationElement dummyFiberDrop = this.getDirectionlessDropModuleLegitimateSignal().isPresent()? OsmOpticalSignalPropagationElement.asDropDirless(this.getDirectionlessDropModuleLegitimateSignal().get()) : OsmOpticalSignalPropagationElement.asDropDirful();
	   	 
	   	 /* Construct a graph starting from add fiber. Stop when all fibers have been processed */
	   	 final Set<OsmOpticalSignalPropagationElement> elementsPendingToProcess = new HashSet<> ();
	   	elementsPendingToProcess.add(dummyFiberAdd);
	   	 final Set<OsmOpticalSignalPropagationElement> elementsAlreadyProcessed = new HashSet<> ();
	   	 final DirectedMultigraph<OsmOpticalSignalPropagationElement , Boolean> propagationGraph = new DirectedMultigraph<> (Boolean.class); // the boolean in the link indicates "is legitimate". 
	   	 propagationGraph.addVertex(dummyFiberAdd);
	   	 while (!elementsPendingToProcess.isEmpty())
	   	 {
	   		 final OsmOpticalSignalPropagationElement fiberToProcess = elementsPendingToProcess.iterator().next();
	   		 if (elementsAlreadyProcessed.contains(fiberToProcess)) { elementsPendingToProcess.remove(fiberToProcess); continue; }
	   		 assert propagationGraph.containsVertex(fiberToProcess);
	   		 /* process the fiber */
	   		 if (fiberToProcess.equals(dummyFiberAdd))
	   		 {
	   			 /* Add lightpath dummy fiber */
	   			 final WNode addNode = leg_fibers.get(0).getA();
	      		 for (WFiber propFiber : addNode.getOpticalSwitchingArchitecture().getOutFibersIfAddToOutputFiber(leg_fibers.get(0) , this.getDirectionlessAddModuleLegitimateSignal().isPresent()))
	      		 {
	      			final OsmOpticalSignalPropagationElement propFiberAsElement = OsmOpticalSignalPropagationElement.asFiber(propFiber);
	      			 if (!propagationGraph.containsVertex(propFiberAsElement)) propagationGraph.addVertex(propFiberAsElement);
	      			 final boolean nextFiberIsLegitimatePropagation = propFiber.equals(this.getSeqFibersLegitimateSignal().get(0));
	      			 propagationGraph.addEdge(dummyFiberAdd, propFiberAsElement , nextFiberIsLegitimatePropagation);
	      			 elementsPendingToProcess.add(propFiberAsElement);
	      		 }
	      		 for (Pair<WNode,Integer> propDropModule : addNode.getOpticalSwitchingArchitecture().getOutFibersIfAddToOutputFiber(leg_fibers.get(0) , this.getDirectionlessAddModuleLegitimateSignal().isPresent()))
	      		 {
	      			final OsmOpticalSignalPropagationElement propFiberAsElement = OsmOpticalSignalPropagationElement.asFiber(propFiber);
	      			 if (!propagationGraph.containsVertex(propFiberAsElement)) propagationGraph.addVertex(propFiberAsElement);
	      			 final boolean nextFiberIsLegitimatePropagation = propFiber.equals(this.getSeqFibersLegitimateSignal().get(0));
	      			 propagationGraph.addEdge(dummyFiberAdd, propFiberAsElement , nextFiberIsLegitimatePropagation);
	      			 elementsPendingToProcess.add(propFiberAsElement);
	      		 }
	      		 
	      		 
	      		 
	   		 } else if (fiberToProcess.equals(dummyFiberDrop))
	   		 {
	   			 /* Drop lightpath dummy fiber => do nothing */
	   		 } else
	   		 {
	   			 final WNode switchNode = fiberToProcess.getB();
	      		 for (WFiber propFiber : switchNode.getOpticalSwitchingArchitecture().getOutFibersUnavoidablePropagationFromInputFiber(fiberToProcess))
	      		 {
	      			 if (!propagationGraph.containsVertex(propFiber)) propagationGraph.addVertex(propFiber);
	      			 propagationGraph.addEdge(fiberToProcess, propFiber);
	      			 fibersPendingToProcess.add(propFiber);
	      		 }
	      		 final int indexOfFiberInPath = leg_fibers.indexOf(fiberToProcess);
	   			 final boolean isExpress = indexOfFiberInPath >= 0 && (indexOfFiberInPath < leg_fibers.size()-1);
	   			 final boolean isDrop = indexOfFiberInPath == leg_fibers.size() - 1;
	   			 if (isExpress)
	   			 {
	      			 final WFiber inFiber = leg_fibers.get(indexOfFiberInPath); assert inFiber.equals(fiberToProcess);
	      			 final WFiber outFiber = leg_fibers.get(indexOfFiberInPath + 1);
	      			 final WNode expressNode = outFiber.getA();
	      			 assert expressNode.equals(inFiber.getB());
	         		 for (WFiber propFiber : expressNode.getOpticalSwitchingArchitecture().getOutFibersIfExpressFromInputToOutputFiber(inFiber , outFiber))
	         		 {
	         			 if (!propagationGraph.containsVertex(propFiber)) propagationGraph.addVertex(propFiber);
	         			 propagationGraph.addEdge(inFiber, propFiber);
	         			 fibersPendingToProcess.add(propFiber);
	         		 }
	   			 } else if (isDrop)
	   			 {
	      			 if (!propagationGraph.containsVertex(dummyFiberDrop)) propagationGraph.addVertex(dummyFiberDrop);
	      			 propagationGraph.addEdge(fiberToProcess, dummyFiberDrop);
	      			 fibersPendingToProcess.add(dummyFiberDrop);
	   			 }
	   		 }
	   		fibersAlreadyProcessed.add (fiberToProcess);
	   		 fibersPendingToProcess.remove(fiberToProcess);
	   	 }
	   	 
	   	 if (!propagationGraph.containsVertex(dummyFiberDrop)) throw new Net2PlanException ("The signal of this lightpath is not reaching the drop node");

	   	 final SortedSet<WFiber> propagatedNonDummyFibers = propagationGraph.vertexSet().stream().filter(e->!e.equals(dummyFiberDrop) && !e.equals(dummyFiberAdd)).collect(Collectors.toCollection(TreeSet::new));
	   	 
	   	 
	   	 boolean multipathFree = links.stream().allMatch(v->propagationGraph.incomingEdgesOf(v).size() == 1);
	   	 multipathFree &= propagationGraph.incomingEdgesOf(dummyFiberDrop).size() == 1;
	   	 final DirectedSimpleCycles<OsmOpticalSignalPropagationElement,Boolean> cycleDetector = new JohnsonSimpleCycles<> (propagationGraph); 
	   	 final List<List<WFiber>> lasingCycles = cycleDetector.findSimpleCycles();
	   	 return Triple.of(propagatedNonDummyFibers , lasingCycles, multipathFree);
	}



}
