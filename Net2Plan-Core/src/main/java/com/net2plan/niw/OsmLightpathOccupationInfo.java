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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

public class OsmLightpathOccupationInfo
{
	private final List<WFiber> legitimate_seqLinks;
	private final Optional<Pair<WNode,Integer>> legitimate_addDirlessModule;
	private final Optional<Pair<WNode,Integer>> legitimate_dropDirlessModule;
	private final Optional<SortedSet<Integer>> occupiedSlots;
	private SortedSet<OsmOpticalSignalPropagationElement> waste_elements = null;
	public OsmLightpathOccupationInfo(List<WFiber> legitimate_seqLinks,
			Optional<Pair<WNode, Integer>> legitimate_addDirlessModule,
			Optional<Pair<WNode, Integer>> legitimate_dropDirlessModule,
			Optional<SortedSet<Integer>> occupiedSlots) 
	{
		if (legitimate_seqLinks.size() != new HashSet<> (legitimate_seqLinks).size()) throw new Net2PlanException ("Invalid lightpath path");
		this.legitimate_seqLinks = legitimate_seqLinks;
		this.legitimate_addDirlessModule = legitimate_addDirlessModule;
		this.legitimate_dropDirlessModule = legitimate_dropDirlessModule;
		this.occupiedSlots = occupiedSlots;
	}

	public Optional<OsmLightpathOccupationInfo> getBidirectionalPair ()
	{
		if (!this.getSeqFibersLegitimateSignal().stream().allMatch(f->f.isBidirectional())) return Optional.empty();
		final List<WFiber> ba = Lists.reverse(getSeqFibersLegitimateSignal().stream().map(e->e.getBidirectionalPair()).collect(Collectors.toList())); 
		return Optional.of(new OsmLightpathOccupationInfo(ba, 
				this.getDirectionlessDropModule(), 
				this.getDirectionlessAddModule(), 
				this.getOccupiedSlotIds()));
	}
	
	public boolean isWithAllLegitimateFibersBidirectional () { return this.getSeqFibersLegitimateSignal().stream().allMatch(f->f.isBidirectional()); }
	
	public List<OsmOpticalSignalPropagationElement> getLegitimateSequenceOfTraversedOpticalElements ()
	{
		final List<OsmOpticalSignalPropagationElement> res =  new ArrayList<> (legitimate_seqLinks.size() + 2);
		if (this.isAddedInDirectionfullModule()) 
			res.add(OsmOpticalSignalPropagationElement.asAddDirful(legitimate_seqLinks.get(0)));
		else
			res.add(OsmOpticalSignalPropagationElement.asAddDirless(this.getDirectionlessAddModule().get()));
		for (WFiber e : this.getSeqFibersLegitimateSignal()) 
			res.add(OsmOpticalSignalPropagationElement.asFiber(e));
		if (this.isDroppedInDirectionfullModule()) 
			res.add(OsmOpticalSignalPropagationElement.asDropDirful(legitimate_seqLinks.get(legitimate_seqLinks.size()-1)));
		else
			res.add(OsmOpticalSignalPropagationElement.asDropDirless(this.getDirectionlessDropModule().get()));
		return res;
	}
	
	public WNode getA () { return legitimate_seqLinks.get(0).getA(); }
	
	public WNode getB () { return legitimate_seqLinks.get(legitimate_seqLinks.size()-1).getB(); }

	public boolean isMyLegitimateClashingWithLegitimateOrWasteSignalOf (OsmLightpathOccupationInfo otherLpInterferingMyLegitimate)
	{
		for (WFiber e : this.getSeqFibersLegitimateSignal())
		{
			if (otherLpInterferingMyLegitimate.getSeqFibersLegitimateSignal().contains(e))
				return true;
			if (otherLpInterferingMyLegitimate.getFibersWithWasteSignal().contains(e))
				return true;
		}
		if (this.getDirectionlessAddModule().isPresent())
		{
			if (this.getDirectionlessAddModule().equals(otherLpInterferingMyLegitimate.getDirectionlessAddModule())) 
				return true;
			if (otherLpInterferingMyLegitimate.getAddDirectionlessModulesWithWasteSignal().contains (this.getDirectionlessAddModule().get()))
				return true;
		}
		if (this.getDirectionlessDropModule().isPresent())
		{
			if (this.getDirectionlessDropModule().equals(otherLpInterferingMyLegitimate.getDirectionlessDropModule())) 
				return true;
			if (otherLpInterferingMyLegitimate.getDropDirectionlessModulesWithWasteSignal().contains (this.getDirectionlessDropModule().get()))
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
	
	public void resetWasteOccupationInfo () { this.waste_elements = null;  }
	
	public SortedSet<OsmOpticalSignalPropagationElement> getOpticalElementsWithWasteSignal () 
	{
		if (waste_elements == null) this.updateWasteOccupationInfo();
		return Collections.unmodifiableSortedSet(this.waste_elements); 
	}
	
	public SortedSet<WFiber> getFibersWithWasteSignal () 
	{
		if (waste_elements == null) this.updateWasteOccupationInfo();
		return waste_elements.stream().filter(e->e.isFiber()).map(e->e.getFiber()).collect(Collectors.toCollection(TreeSet::new));
	}
	public List<Pair<WNode,Integer>> getAddDirectionlessModulesWithWasteSignal () 
	{
		if (waste_elements == null) this.updateWasteOccupationInfo();
		return waste_elements.stream().filter(e->e.isDirlessAdd()).map(e->e.getDirlessAddModule()).collect(Collectors.toList());
	}
	public List<Pair<WNode,Integer>> getDropDirectionlessModulesWithWasteSignal () 
	{
		if (waste_elements == null) this.updateWasteOccupationInfo();
		return waste_elements.stream().filter(e->e.isDirlessDrop()).map(e->e.getDirlessDropModule()).collect(Collectors.toList());
	}
	public List<WFiber> getAddDirectionfulModulesWithWasteSignal () 
	{
		if (waste_elements == null) this.updateWasteOccupationInfo();
		return waste_elements.stream().filter(e->e.isDirfulAdd()).map(e->e.getDirfulAddOutFiber()).collect(Collectors.toList());
	}
	public List<WFiber> getDropDirectionfulModulesWithWasteSignal () 
	{
		if (waste_elements == null) this.updateWasteOccupationInfo();
		return waste_elements.stream().filter(e->e.isDirfulDrop()).map(e->e.getDirfulDropInFiber()).collect(Collectors.toList());
	}
	
	public List<WFiber> getSeqFibersLegitimateSignal () { return Collections.unmodifiableList(this.legitimate_seqLinks); }
	public Optional<Pair<WNode,Integer>> getDirectionlessAddModule () { return this.legitimate_addDirlessModule; }
	public Optional<Pair<WNode,Integer>> getDirectionlessDropModule () { return this.legitimate_dropDirlessModule; }
	public Optional<Integer> getDirectionlessAddModuleIndex () { return this.legitimate_addDirlessModule.isPresent()? Optional.of(legitimate_addDirlessModule.get().getSecond()) : Optional.empty(); }
	public Optional<Integer> getDirectionlessDropModuleIndex () { return this.legitimate_dropDirlessModule.isPresent()? Optional.of(legitimate_dropDirlessModule.get().getSecond()) : Optional.empty(); }
	public boolean isAddedInDirectionlessModule () { return this.getDirectionlessAddModule().isPresent(); }
	public boolean isDroppedInDirectionlessModule () { return this.getDirectionlessDropModule().isPresent(); }
	public boolean isAddedInDirectionfullModule () { return !this.isAddedInDirectionlessModule(); }
	public boolean isDroppedInDirectionfullModule () { return !this.isDroppedInDirectionlessModule(); }
	
	public boolean isWithFiberCyclesInLegitimateSignal ()
	{
		return legitimate_seqLinks.size() != new HashSet<> (legitimate_seqLinks).size();
	}
	public boolean isWithSelfClashing ()
	{
		if (isWithFiberCyclesInLegitimateSignal()) return true;
		final SortedSet<WFiber> wasteFibers = getFibersWithWasteSignal();
		for (WFiber e : getSeqFibersLegitimateSignal()) if (wasteFibers.contains(e)) return true;
		if (getDirectionlessAddModule().isPresent())
			if (getAddDirectionlessModulesWithWasteSignal().contains(getDirectionlessAddModule().get()))
				return true;
		if (getDirectionlessDropModule().isPresent())
			if (getDropDirectionlessModulesWithWasteSignal().contains(getDirectionlessDropModule().get()))
				return true;
		return false;
	}
	public Optional<SortedSet<Integer>> getOccupiedSlotIds () { return this.occupiedSlots; }
	private void updateWasteOccupationInfo ()
	{
		final List<WFiber> leg_fibers = new ArrayList<> (getSeqFibersLegitimateSignal());
		if (leg_fibers.isEmpty()) throw new Net2PlanException ("The path is empty");
	   	if (OpticalSpectrumManager.getPathNodes(leg_fibers).stream().allMatch(n->n.getOpticalSwitchingArchitecture().isNeverCreatingWastedSpectrum()))
	   	{
	   		this.waste_elements = new TreeSet<> ();
	   		return; // all empty
	   	}
	   	 
	   	 final OsmOpticalSignalPropagationElement legitimateAddModule = this.isAddedInDirectionlessModule()? OsmOpticalSignalPropagationElement.asAddDirless(this.getDirectionlessAddModule().get()) : OsmOpticalSignalPropagationElement.asAddDirful(leg_fibers.get(0));
	   	 final OsmOpticalSignalPropagationElement legitimateDropModule = this.isDroppedInDirectionlessModule()? OsmOpticalSignalPropagationElement.asDropDirless(this.getDirectionlessDropModule().get()) : OsmOpticalSignalPropagationElement.asDropDirful(leg_fibers.get(leg_fibers.size()-1));
	   	 final OsmOpticalSignalPropagationElement legitimateFirstFiber = OsmOpticalSignalPropagationElement.asFiber(leg_fibers.get(0));
	   	 final OsmOpticalSignalPropagationElement legitimateLastFiber = OsmOpticalSignalPropagationElement.asFiber(leg_fibers.get(leg_fibers.size()-1));
	   	 
	   	 /* Construct a graph starting from add fiber. Stop when all fibers have been processed */
	   	 final Set<OsmOpticalSignalPropagationElement> elementsPendingToProcess = new HashSet<> ();
	   	 elementsPendingToProcess.add(legitimateAddModule);
	   	 final Set<OsmOpticalSignalPropagationElement> elementsAlreadyProcessed = new HashSet<> ();
	   	 final DirectedMultigraph<OsmOpticalSignalPropagationElement , Object> propagationGraph = new DirectedMultigraph<> (Object.class); // the boolean in the link indicates "is legitimate". 
	   	 propagationGraph.addVertex(legitimateAddModule);
	   	 while (!elementsPendingToProcess.isEmpty())
	   	 {
	   		 final OsmOpticalSignalPropagationElement elementToProcess = elementsPendingToProcess.iterator().next();
	   		 if (elementsAlreadyProcessed.contains(elementToProcess)) { elementsPendingToProcess.remove(elementToProcess); continue; }
	   		 assert propagationGraph.containsVertex(elementToProcess);
	   		 /* process the fiber */
	   		final Set<OsmOpticalSignalPropagationElement> outElements = new HashSet<> ();
	   		 if (elementToProcess.isDirfulAdd() || elementToProcess.isDirlessAdd())
	   		 {
	   			 /* An add module */
	   			 final WNode addNode = elementToProcess.isDirfulAdd()? elementToProcess.getDirfulAddOutFiber().getA() : elementToProcess.getDirlessAddModule().getFirst();
	   			 final Optional<OsmOpticalSignalPropagationElement> intendedOutputElement = addNode.equals(this.getA())? Optional.of(legitimateFirstFiber) : Optional.empty(); 
	   			 outElements.addAll(addNode.getOpticalSwitchingArchitecture().getOutElements(elementToProcess, intendedOutputElement));
	   		 } else if (elementToProcess.isDirfulDrop() || elementToProcess.isDirlessDrop())
	   		 {
	   			 final WNode dropNode = elementToProcess.isDirfulDrop()? elementToProcess.getDirfulDropInFiber().getB() : elementToProcess.getDirlessDropModule().getFirst();
	   			 outElements.addAll(dropNode.getOpticalSwitchingArchitecture().getOutElements(elementToProcess, Optional.empty()));
	   		 } else if (elementToProcess.isFiber())
	   		 {
	   			 final WFiber fiberToProcess = elementToProcess.getFiber();
	   			 final WNode switchNode = fiberToProcess.getB();
	   			 final SortedSet<WFiber> outFibersUnavoidable = switchNode.getOpticalSwitchingArchitecture().getOutFibersUnavoidablePropagationFromInputFiber(fiberToProcess);
	      		 final int indexOfFiberInPath = leg_fibers.indexOf(fiberToProcess);
	   			 final boolean isExpress = indexOfFiberInPath >= 0 && (indexOfFiberInPath < leg_fibers.size()-1);
	   			 final boolean isDrop = indexOfFiberInPath == leg_fibers.size() - 1;
	   			 final Optional<OsmOpticalSignalPropagationElement> intendedOutputElement;
	   			 if (isExpress)
	   				 intendedOutputElement = Optional.of(OsmOpticalSignalPropagationElement.asFiber(leg_fibers.get(indexOfFiberInPath + 1)));
	   			 else if (isDrop)
	   				intendedOutputElement = Optional.of(legitimateDropModule);
	   			 else 
	   				 intendedOutputElement = Optional.empty();
	   			 outElements.addAll(outFibersUnavoidable.stream().map(f->OsmOpticalSignalPropagationElement.asFiber(f)).collect(Collectors.toList()));
	   			 outElements.addAll(switchNode.getOpticalSwitchingArchitecture().getOutElements(elementToProcess, intendedOutputElement));
	   		 }
      		 for (OsmOpticalSignalPropagationElement nextElement : outElements)
      		 {
      			 if (!propagationGraph.containsVertex(nextElement)) propagationGraph.addVertex(nextElement);
      			 propagationGraph.addEdge(legitimateAddModule, nextElement);
      			 elementsPendingToProcess.add(nextElement);
      		 }
      		elementsAlreadyProcessed.add (elementToProcess);
	   		 elementsPendingToProcess.remove(elementToProcess);

      		 
//	   		 if (elementToProcess.equals(legitimateAddModule))
//	   		 {
//	   			 /* Add lightpath dummy fiber */
//	   			 final WNode addNode = leg_fibers.get(0).getA();
//	      		 for (WFiber propFiber : addNode.getOpticalSwitchingArchitecture().getOutFibersIfAddToOutputFiber(leg_fibers.get(0) , this.getDirectionlessAddModule().isPresent()))
//	      		 {
//	      			final OsmOpticalSignalPropagationElement propFiberAsElement = OsmOpticalSignalPropagationElement.asFiber(propFiber);
//	      			 if (!propagationGraph.containsVertex(propFiberAsElement)) propagationGraph.addVertex(propFiberAsElement);
//	      			 final boolean nextFiberIsLegitimatePropagation = propFiber.equals(this.getSeqFibersLegitimateSignal().get(0));
//	      			 propagationGraph.addEdge(legitimateAddModule, propFiberAsElement , nextFiberIsLegitimatePropagation);
//	      			 elementsPendingToProcess.add(propFiberAsElement);
//	      		 }
//	      		 for (Pair<WNode,Integer> propDropModule : addNode.getOpticalSwitchingArchitecture().getOutFibersIfAddToOutputFiber(leg_fibers.get(0) , this.getDirectionlessAddModule().isPresent()))
//	      		 {
//	      			final OsmOpticalSignalPropagationElement propFiberAsElement = OsmOpticalSignalPropagationElement.asFiber(propFiber);
//	      			 if (!propagationGraph.containsVertex(propFiberAsElement)) propagationGraph.addVertex(propFiberAsElement);
//	      			 final boolean nextFiberIsLegitimatePropagation = propFiber.equals(this.getSeqFibersLegitimateSignal().get(0));
//	      			 propagationGraph.addEdge(legitimateAddModule, propFiberAsElement , nextFiberIsLegitimatePropagation);
//	      			 elementsPendingToProcess.add(propFiberAsElement);
//	      		 }
//	      		 
//	      		 
//	      		 
//	   		 } else if (fiberToProcess.equals(legitimateDropModule))
//	   		 {
//	   			 /* Drop lightpath dummy fiber => do nothing */
//	   		 } else
//	   		 {
//	   			 final WNode switchNode = fiberToProcess.getB();
//	      		 for (WFiber propFiber : switchNode.getOpticalSwitchingArchitecture().getOutFibersUnavoidablePropagationFromInputFiber(fiberToProcess))
//	      		 {
//	      			 if (!propagationGraph.containsVertex(propFiber)) propagationGraph.addVertex(propFiber);
//	      			 propagationGraph.addEdge(fiberToProcess, propFiber);
//	      			 fibersPendingToProcess.add(propFiber);
//	      		 }
//	      		 final int indexOfFiberInPath = leg_fibers.indexOf(fiberToProcess);
//	   			 final boolean isExpress = indexOfFiberInPath >= 0 && (indexOfFiberInPath < leg_fibers.size()-1);
//	   			 final boolean isDrop = indexOfFiberInPath == leg_fibers.size() - 1;
//	   			 if (isExpress)
//	   			 {
//	      			 final WFiber inFiber = leg_fibers.get(indexOfFiberInPath); assert inFiber.equals(fiberToProcess);
//	      			 final WFiber outFiber = leg_fibers.get(indexOfFiberInPath + 1);
//	      			 final WNode expressNode = outFiber.getA();
//	      			 assert expressNode.equals(inFiber.getB());
//	         		 for (WFiber propFiber : expressNode.getOpticalSwitchingArchitecture().getOutFibersIfExpressFromInputToOutputFiber(inFiber , outFiber))
//	         		 {
//	         			 if (!propagationGraph.containsVertex(propFiber)) propagationGraph.addVertex(propFiber);
//	         			 propagationGraph.addEdge(inFiber, propFiber);
//	         			 fibersPendingToProcess.add(propFiber);
//	         		 }
//	   			 } else if (isDrop)
//	   			 {
//	      			 if (!propagationGraph.containsVertex(legitimateDropModule)) propagationGraph.addVertex(legitimateDropModule);
//	      			 propagationGraph.addEdge(fiberToProcess, legitimateDropModule);
//	      			 fibersPendingToProcess.add(legitimateDropModule);
//	   			 }
//	   		 }
	   	 }
	   	 
	   	 if (!propagationGraph.containsVertex(legitimateAddModule)) throw new Net2PlanException ("The signal of this lightpath is not starting in the add module");
	   	 if (!propagationGraph.containsVertex(legitimateDropModule)) throw new Net2PlanException ("The signal of this lightpath is not reaching the drop node");
	   	 for (WFiber e : this.getSeqFibersLegitimateSignal())
		   	 if (!propagationGraph.containsVertex(OsmOpticalSignalPropagationElement.asFiber(e))) throw new Net2PlanException ("The signal of this lightpath is not traversing the legitimate paths");

	   	 this.waste_elements = new TreeSet<> ();
	   	 this.waste_elements.addAll(propagationGraph.vertexSet());
	   	 this.waste_elements.removeAll(this.getLegitimateSequenceOfTraversedOpticalElements ());
	   	 boolean alreadyALegitimateElementWithWaste = false;
	   	 for (OsmOpticalSignalPropagationElement oe : this.getLegitimateSequenceOfTraversedOpticalElements ())
	   	 {
	   		 alreadyALegitimateElementWithWaste |= this.waste_elements.contains(oe);
	   		 if (alreadyALegitimateElementWithWaste) this.waste_elements.add(oe); 
	   	 }
	}



}
