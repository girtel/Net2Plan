/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import java.util.stream.Collectors;

import org.jgrapht.alg.cycle.DirectedSimpleCycles;
import org.jgrapht.alg.cycle.JohnsonSimpleCycles;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DirectedMultigraph;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;


/** This class is used to account for the occupation of the optical spectrum in the network.  
 * The object can be created from an existing network. To make a valid optical design, the user is responsible of 
 * using this object methods to check if routing and spectrum assignments (RSAs) of new lightpaths are valid. Also, this object 
 * includes some methods for simple RSA recommendations, e.g. first-fit assignments
 * (occupy idle and valid resources)
 * Occupation is represented by optical slots, each defined by an integer. The central frequency of optical slot i is 193.1+i*0.0125 THz.
 * All optical slots are supposed to have the same width 12.5 GHz (see WNetConstants)
 *
 */
public class OpticalSpectrumManager
{
	public enum OpticalSignalOccupationType { LEGITIMATESIGNAL , WASTESIGNAL; public boolean isWaste() { return this == OpticalSignalOccupationType.WASTESIGNAL;} public boolean isLegitimate () { return this == OpticalSignalOccupationType.LEGITIMATESIGNAL;  }  }
	
	private class SlotOccupationManager <T>
	{
		final private Map<T,SortedMap<Integer,SortedSet<WLightpath>>> occupation_element_s_ll = new HashMap<> ();
		public SlotOccupationManager() {}
		public void clear () { occupation_element_s_ll.clear();  }
		public Map<T,SortedMap<Integer,SortedSet<WLightpath>>> getFullPerElementOccupationMap () { return Collections.unmodifiableMap(occupation_element_s_ll); }
		public SortedMap<Integer,SortedSet<WLightpath>> getOccupiedSlotIds (T element) 
		{ 
			return Collections.unmodifiableSortedMap(occupation_element_s_ll.getOrDefault(element, new TreeMap<> ())); 	
		}
		public int getNumberOfOccupiedSlotIds (T element) 
		{ 
			return occupation_element_s_ll.getOrDefault(element, new TreeMap<> ()).size(); 	
		}
		public Set<T> getElementsWithAtLeastOneSlotOccupied  () 
		{ 
			return Collections.unmodifiableSet(occupation_element_s_ll.keySet()); 	
		}
	    public SortedSet<Integer> getOccupiedOpticalSlotIds (T element)
	    {
			SortedMap<Integer,SortedSet<WLightpath>> occupiedSlotsPerLightpath = this.occupation_element_s_ll.get(element);
			if(occupiedSlotsPerLightpath == null)
				return new TreeSet<>();
	    	return new TreeSet<> (occupiedSlotsPerLightpath.keySet());
	    }
	    public void allocateOccupation (T element , WLightpath lp , SortedSet<Integer> slotIds)
	    {
	    	if (slotIds.isEmpty()) return;
	    	boolean clashesWithPreviousAllocations = false;
    		SortedMap<Integer,SortedSet<WLightpath>> thisElementInfo = this.occupation_element_s_ll.get(element);
    		if (thisElementInfo == null) { thisElementInfo = new TreeMap<> (); this.occupation_element_s_ll.put(element, thisElementInfo); }
    		for (int slotId : slotIds)
    		{
    			SortedSet<WLightpath> currentCollidingLps = thisElementInfo.get(slotId);
    			if (currentCollidingLps == null) { currentCollidingLps = new TreeSet<> (); thisElementInfo.put(slotId, currentCollidingLps); }
    			if (!currentCollidingLps.isEmpty()) clashesWithPreviousAllocations = true;
    			currentCollidingLps.add(lp);
    		}
	    }

	    public void releaseOccupation (WLightpath lp , Collection<T> occupiedResources)
	    {
	    	if (occupiedResources == null) return;
    		final SortedSet<Integer> slotIds = lp.getOpticalSlotIds();
	    	for (T element : occupiedResources)
	    	{
	    		SortedMap<Integer,SortedSet<WLightpath>> thisFiberInfo = this.occupation_element_s_ll.get(element);
	    		for (int slotId : slotIds)
	    		{
	    			final SortedSet<WLightpath> thisLpAndOthers = thisFiberInfo.get(slotId);
	    			assert thisLpAndOthers != null;
	    			assert thisLpAndOthers.contains(lp);
	    			thisLpAndOthers.remove(lp);
	    			if (thisLpAndOthers.isEmpty()) 
	    			{
	    				thisFiberInfo.remove(slotId);
	    				if (thisFiberInfo.isEmpty()) this.occupation_element_s_ll.remove(element);
	    			}
	    		}
	    	}
	    }
	}

	
	
	
	private WNet wNet;
	final private SlotOccupationManager<WFiber> legitimateSignal_perFiberOccupation = new SlotOccupationManager<>();
	final private SlotOccupationManager<Pair<WNode,Integer>> legitimateSignal_directionlessAddOccupation = new SlotOccupationManager<>();
	final private SlotOccupationManager<Pair<WNode,Integer>> legitimateSignal_directionlessDropOccupation = new SlotOccupationManager<>();
	final private SlotOccupationManager<WFiber> wasteSignal_perFiberOccupation = new SlotOccupationManager<>();
	final private SlotOccupationManager<Pair<WNode,Integer>> wasteSignal_directionlessAddOccupation = new SlotOccupationManager<>();
	final private SlotOccupationManager<Pair<WNode,Integer>> wasteSignal_directionlessDropOccupation = new SlotOccupationManager<>();
	final private SortedMap<WLightpath , OsmLightpathOccupationInfo> lightpathsIncluded = new TreeMap<> ();
	
	
	//	
//	final private Map<Pair<WNode,Integer>,SortedMap<Integer,SortedSet<WLightpath>>> directionlessAddOccupation_nm_s_ll = new HashMap<> ();
//	final private Map<Pair<WNode,Integer>,SortedMap<Integer,SortedSet<WLightpath>>> directionlessDropOccupation_nm_s_ll = new HashMap<> ();
//	final private SortedMap<WLightpath,SortedMap<WFiber,SortedSet<Integer>>> occupation_ll_f_s = new TreeMap<> ();
//	final private SortedMap<WLightpath,Triple<WNode,Integer,SortedSet<Integer>>> directionlessAddOccupation_ll_nms = new TreeMap<> ();
//	final private SortedMap<WLightpath,Triple<WNode,Integer,SortedSet<Integer>>> directionlessDropOccupation_ll_nms = new TreeMap<> ();

	private OpticalSpectrumManager (WNet wNet) { this.wNet = wNet; }
	
	/** Creates this object, asociated to a given network
	 * @param net the network
	 * @return see above
	 */
	public static OpticalSpectrumManager createFromRegularLps (WNet net)
    {
		final OpticalSpectrumManager osm = new OpticalSpectrumManager(net);
		osm.resetFromRegularLps(net);
        return osm;
    }

	/** Resets this object, makes it associated to a given network and according to their lightpaths
	 * @param net the network
	 * @return see above
	 */
	public OpticalSpectrumManager resetFromRegularLps (WNet net)
    {
		this.wNet = net;
		this.wasteSignal_perFiberOccupation.clear();
		this.wasteSignal_directionlessAddOccupation.clear();
		this.wasteSignal_directionlessDropOccupation.clear();
		this.legitimateSignal_perFiberOccupation.clear();
		this.legitimateSignal_directionlessAddOccupation.clear();
		this.legitimateSignal_directionlessDropOccupation.clear();
		this.lightpathsIncluded.clear();
		for (WLightpath lp : net.getLightpaths())
		{
			final Optional<Integer> addDirectionlessModuleIndex = lp.getDirectionlessAddModuleIndexInOrigin();
			final Optional<Integer> dropDirectionlessModuleIndex = lp.getDirectionlessDropModuleIndexInDestination();
			this.allocateOccupation(lp,Optional.empty());
		}
        return this;
    }


	/** FA: Returns the set of the optical slots ids that are idle in ALL the fibers provided and also, if given, in the add and drop directionless modules, so they are not occupied by legitimate or waste signals
     * @param wdmLinks the set of fibers
     * @param addNodeDirectionlessBank see above
     * @param dropNodeDirectionlessBank see above
     * @return see above
     */
    public SortedSet<Integer> getAvailableSlotIds (Collection<WFiber> wdmLinks , Optional<Pair<WNode,Integer>> addNodeDirectionlessBank , Optional<Pair<WNode,Integer>> dropNodeDirectionlessBank) 
    {
    	checkSameWNet(wdmLinks);
        if (wdmLinks.isEmpty()) throw new Net2PlanException ("No WDM links");
        final Iterator<WFiber> itLink = wdmLinks.iterator();
        final WFiber firstLink = itLink.next();
        final SortedSet<Integer> validSlotIds = this.getIdleOpticalSlotIds(firstLink);
        while (itLink.hasNext())
            validSlotIds.retainAll(this.getIdleOpticalSlotIds(itLink.next()));
        if (addNodeDirectionlessBank.isPresent())
            validSlotIds.removeAll(this.getOccupiedOpticalSlotIdsInDirectionlessAddModule(addNodeDirectionlessBank.get().getFirst() , addNodeDirectionlessBank.get().getSecond()));
        if (dropNodeDirectionlessBank.isPresent())
            validSlotIds.removeAll(this.getOccupiedOpticalSlotIdsInDirectionlessDropModule(dropNodeDirectionlessBank.get().getFirst() , dropNodeDirectionlessBank.get().getSecond()));
        return validSlotIds;
    }

	/** FA: Returns the set of the optical slots ids that are idle in ALL the fibers provided and also, if given, in the add and drop directionless modules, so they are not occupied by legitimate or waste signals
     * @param wdmLinks the set of fibers
     * @param addNodeDirectionlessBank see above
     * @param dropNodeDirectionlessBank see above
     * @return see above
     */
    public SortedSet<Integer> getAvailableSlotIdsEmptyOrWithWaste (Collection<WFiber> wdmLinks , Optional<Pair<WNode,Integer>> addNodeDirectionlessBank , Optional<Pair<WNode,Integer>> dropNodeDirectionlessBank) 
    {
    	checkSameWNet(wdmLinks);
        if (wdmLinks.isEmpty()) throw new Net2PlanException ("No WDM links");
        final Iterator<WFiber> itLink = wdmLinks.iterator();
        final WFiber firstLink = itLink.next();

        final SortedSet<Integer> validSlotIds = this.getOpticalSlotIdsEmptyOrWithWaste(firstLink);
        while (itLink.hasNext())
            validSlotIds.retainAll(this.getOpticalSlotIdsEmptyOrWithWaste(itLink.next()));
        if (addNodeDirectionlessBank.isPresent())
            validSlotIds.removeAll(this.getOccupiedWithLegitimateSignalOpticalSlotIdsInDirectionlessAddModule(addNodeDirectionlessBank.get().getFirst() , addNodeDirectionlessBank.get().getSecond()));
        if (dropNodeDirectionlessBank.isPresent())
            validSlotIds.removeAll(this.getOccupiedWithLegitimateSignalOpticalSlotIdsInDirectionlessDropModule(dropNodeDirectionlessBank.get().getFirst() , dropNodeDirectionlessBank.get().getSecond()));
        return validSlotIds;
    }

    
    /** FA: Given a fiber, returns a map with the occupied optical slot ids, both caused by legitimate signals, mapped to the set of lightpaths that occupy it. 
     * Note that if more than one lightpath occupies a given slot, means that spectrum clashing occurs in that slot   
     * @param fiber the input fiber
     * @return see above
     */
    public SortedMap<Integer,SortedSet<WLightpath>> getOccupiedResources (WFiber fiber , OpticalSignalOccupationType signalOccupationType)
    {
    	checkSameWNet(fiber);
    	return this.legitimateSignal_perFiberOccupation.getOccupiedSlotIds(fiber);
    }

    /** FA: Given a node and the index of the directionless add module, and the type of optical signal of interest (waste or legitimate), returns a map with the occupied optical slot ids, mapped to the set of lightpaths that occupy it.
     * @param node see above
     * @param directionlessModuleIndex  see above
     * @param signalType see above
     * @return see above
     */
    public SortedMap<Integer,SortedSet<WLightpath>> getOccupiedResourcesInDirectionlessAddModule (WNode node , int directionlessModuleIndex , OpticalSignalOccupationType signalType)
    {
    	checkSameWNet(node);
    	final Pair<WNode,Integer> id = Pair.of(node, directionlessModuleIndex);
    	return signalType.isLegitimate()? legitimateSignal_directionlessAddOccupation.getOccupiedSlotIds(id) : wasteSignal_directionlessAddOccupation.getOccupiedSlotIds(id); 
    }

    /** FA: Given a node and the index of the directionless drop module, and the type of optical signal of interest (waste or legitimate), returns a map with the occupied optical slot ids, mapped to the set of lightpaths that occupy it.
     * @param node see above
     * @param directionlessModuleIndex  see above
     * @param signalType see above
     * @return see above
     */
    public SortedMap<Integer,SortedSet<WLightpath>> getOccupiedResourcesInDirectionlessDropModule (WNode node , int directionlessModuleIndex , OpticalSignalOccupationType signalType)
    {
    	checkSameWNet(node);
    	final Pair<WNode,Integer> id = Pair.of(node, directionlessModuleIndex);
    	return signalType.isLegitimate()? legitimateSignal_directionlessDropOccupation.getOccupiedSlotIds(id) : wasteSignal_directionlessDropOccupation.getOccupiedSlotIds(id); 
    }

    /** FA: Given a fiber, returns the set of optical slots occupied by at least one traversing lightpath, in its waste of legitimate signal
     * @param fiber see above
     * @return see above
     */
    public SortedSet<Integer> getOccupiedOpticalSlotIds (WFiber fiber)
    {
    	checkSameWNet(fiber);
    	final SortedSet<Integer> res = legitimateSignal_perFiberOccupation.getOccupiedOpticalSlotIds(fiber);
    	res.addAll(wasteSignal_perFiberOccupation.getOccupiedOpticalSlotIds(fiber));
    	return res;
    }

    /** FA: Given a fiber, returns the set of optical slots occupied by at least one traversing lightpath in its legitimate signal path
     * @param fiber see above
     * @return see above
     */
    public SortedSet<Integer> getOpticalSlotIdsWithLegitimateSignal (WFiber fiber)
    {
    	checkSameWNet(fiber);
    	return legitimateSignal_perFiberOccupation.getOccupiedOpticalSlotIds(fiber);
    }

    
    /** FA: Given a potential lightpath occupation information, returns true if this lp would be allocatable, meaning that: 
     * 1) It is not a self-clashing occupation: lightpaths traversing a fiber more than once, or where the its own waste signal overlaps the path of its legitimate signal
     * 2) The sequence of fibers and add/drop dirless modules are idle (not occupied by aby waste or legitimate signal of other lightpaths)
     * 3) The waste signal appearing in some fibers or add/drop directionless modules overlaps with other lightpath LEGITIMATE paths/modules. 
     * Note that it is accepted that waste signals of different lightpaths clash between them
     * @param occupationInformation see above
     * @return see above
     */
    public boolean isAllocatable (OsmLightpathOccupationInfo occupationInformation)
    {
    	if (!occupationInformation.getOccupiedSlotIds().isPresent()) throw new Net2PlanException ("Please provide spectrum occupation"); 
    	if (occupationInformation.isWithSelfClashing()) return false;
    	final SortedSet<Integer> slotIds = occupationInformation.getOccupiedSlotIds().get();
    	/* Legitimate fibers are fully free */
        for (WFiber e : occupationInformation.getSeqFibersLegitimateSignal())
            if (!this.isOpticalSlotIdsValidAndIdle(e , slotIds))
                return false;
    	/* Legitimate ADD directionless module is fully free */
        if (occupationInformation.getDirectionlessAddModule().isPresent())
        	if (!this.isOpticalSlotIdsValidAndIdleInAddDirectionlessModule(occupationInformation.getDirectionlessAddModule().get().getFirst(), occupationInformation.getDirectionlessAddModule().get().getSecond(), slotIds))
        		return false;
    	/* Legitimate DROP directionless module is fully free */
        if (occupationInformation.getDirectionlessDropModule().isPresent())
        	if (!this.isOpticalSlotIdsValidAndIdleInDropDirectionlessModule(occupationInformation.getDirectionlessDropModule().get().getFirst(), occupationInformation.getDirectionlessDropModule().get().getSecond(), slotIds))
        		return false;
        /* Fibers with waste spectrum are not occupied by legitimate signals */
        for (WFiber e : occupationInformation.getFibersWithWasteSignal())
        	for (int slotWithLegitimateSignalOtherLps : this.legitimateSignal_perFiberOccupation.getOccupiedOpticalSlotIds(e))
        		if (slotIds.contains(slotWithLegitimateSignalOtherLps))
        			return false;
        /* Dirless add modules with waste spectrum are not occupied by legitimate signals */
        for (Pair<WNode,Integer> e : occupationInformation.getAddDirectionlessModulesWithWasteSignal())
        	for (int slotWithLegitimateSignalOtherLps : this.legitimateSignal_directionlessAddOccupation.getOccupiedOpticalSlotIds(e))
        		if (slotIds.contains(slotWithLegitimateSignalOtherLps))
        			return false;
        /* Dirless drop modules with waste spectrum are not occupied by legitimate signals */
        for (Pair<WNode,Integer> e : occupationInformation.getDropDirectionlessModulesWithWasteSignal())
        	for (int slotWithLegitimateSignalOtherLps : this.legitimateSignal_directionlessDropOccupation.getOccupiedOpticalSlotIds(e))
        		if (slotIds.contains(slotWithLegitimateSignalOtherLps))
        			return false;
        return true;
    }

    /** Indicates if this lightpath has already been accounted for
     * @param lp see above
     * @return see above
     */
    public boolean isAlreadyAllocated (WLightpath lp) { return this.lightpathsIncluded.containsKey(lp); }
    
    /** Accounts for the occupation of a lightpath, accounting for both the occupation caused by the legitimate optical path, and for the wasted spectrum caused by filterless switching in the nodes
     * @param lp the lightpath
     * @param optionalOccupationInformation the lightpath occupation information. If not present, it is automatically computed from the current lightpath information
     */
    public void allocateOccupation (WLightpath lp , Optional<OsmLightpathOccupationInfo> optionalOccupationInformation)
    {
   	 	checkSameWNet(lp);
   	 	if (isAlreadyAllocated(lp)) throw new Net2PlanException ("This lightpath has already been allocated. Release it first. ");
   	 	final OsmLightpathOccupationInfo occupationInformation = optionalOccupationInformation.orElse(lp.getOpticalOccupationInformation ());
    	if (!occupationInformation.getOccupiedSlotIds().isPresent()) throw new Net2PlanException ("Please provide spectrum occupation"); 
    	final SortedSet<Integer> slotIds = occupationInformation.getOccupiedSlotIds().get();
    	if (slotIds.isEmpty()) return;
    	for (WFiber fiber : occupationInformation.getSeqFibersLegitimateSignal())
    		legitimateSignal_perFiberOccupation.allocateOccupation(fiber, lp, slotIds);
    	if (occupationInformation.getDirectionlessAddModule().isPresent())
    		legitimateSignal_directionlessAddOccupation.allocateOccupation(occupationInformation.getDirectionlessAddModule().get(), lp, slotIds);
    	if (occupationInformation.getDirectionlessDropModule().isPresent())
    		legitimateSignal_directionlessDropOccupation.allocateOccupation(occupationInformation.getDirectionlessDropModule().get(), lp, slotIds);
    	for (WFiber fiber : occupationInformation.getFibersWithWasteSignal())
    		wasteSignal_perFiberOccupation.allocateOccupation(fiber, lp, slotIds);
    	for (Pair<WNode,Integer> module : occupationInformation.getAddDirectionlessModulesWithWasteSignal())
    		wasteSignal_directionlessAddOccupation.allocateOccupation(module , lp, slotIds);
    	for (Pair<WNode,Integer> module : occupationInformation.getDropDirectionlessModulesWithWasteSignal())
    		wasteSignal_directionlessDropOccupation.allocateOccupation(module , lp, slotIds);
    }

    /** Releases all the optical slots occupied for a given lightpath in this manager
     * @param lp the lightpath
     */
    public void releaseOccupation (WLightpath lp)
    {
    	checkSameWNet(lp);
    	final OsmLightpathOccupationInfo occup = this.lightpathsIncluded.get(lp);
    	if (occup == null) return;
		legitimateSignal_perFiberOccupation.releaseOccupation(lp, occup.getSeqFibersLegitimateSignal()); 
    	if (occup.getDirectionlessAddModule().isPresent())
    		legitimateSignal_directionlessAddOccupation.releaseOccupation(lp , Arrays.asList(occup.getDirectionlessAddModule().get()));
    	if (occup.getDirectionlessDropModule().isPresent())
    		legitimateSignal_directionlessDropOccupation.releaseOccupation(lp , Arrays.asList(occup.getDirectionlessDropModule().get()));
		wasteSignal_perFiberOccupation.releaseOccupation(lp, occup.getFibersWithWasteSignal());
		wasteSignal_directionlessAddOccupation.releaseOccupation(lp, occup.getAddDirectionlessModulesWithWasteSignal());
		wasteSignal_directionlessDropOccupation.releaseOccupation(lp, occup.getDropDirectionlessModulesWithWasteSignal());
    }

//    /** Searches for a first-fit assignment, where in each hop, one fiber is chosen. Given a set of hops (each hop with at least one fiber as an option),
//     * optional directionless add and drop module to occupy,  
//     * the number of contiguous optical slots needed, 
//     * and (optionally) an initial optical slot (so optical slots of lower id are not consiedered), this method searches for 
//     * the lowest-id contiguous range of slots that are available in all the indicated fibers and directionless modules. Note that if the set of fibers 
//     * passes more than once in the same fiber, no assignment is possible, and Optional.empty is returned
//     * @param seqAdjacenciesFibers_ab see above
//     * @param directionlessAddModuleAb see above
//     * @param directionlessDropModuleAb see above
//     * @param directionlessAddModuleBa see above
//     * @param directionlessDropModuleBa see above
//     * @param numContiguousSlotsRequired see above
//     * @param unusableSlots see above
//     * @return see above. If no idle range is found, Optional.empty is returned. 
//     */
//    public Optional<Pair<List<Pair<WFiber,WFiber>> , SortedSet<Integer>>> spectrumAssignment_firstFitForAdjacenciesBidi (Collection<Pair<WNode,WNode>> seqAdjacenciesFibers_ab,
//    		Optional<Pair<WNode,Integer>> directionlessAddModuleAbDropModuleBa , 
//    		Optional<Pair<WNode,Integer>> directionlessAddModuleBaDropModuleAb , 
//    		int numContiguousSlotsRequired , SortedSet<Integer> unusableSlots)
//    {
//   	 	assert !seqAdjacenciesFibers_ab.isEmpty();
//   	 	assert numContiguousSlotsRequired > 0;
//   	 	/* Get valid fibers and first slots ids to return, according to the fibers */
//        /* If a fiber is traversed more than once, there is no possible assignment */
//   	  final Map<Pair<WNode,WNode> , Pair<SortedSet<Integer> , List<Pair<WFiber,WFiber>>>> mapInfoConsideringAllBidi = new HashMap<> ();
//   	  final SortedSet<WFiber> allFibersToCheckRepetitions = new TreeSet<> ();
//   	  for (Pair<WNode,WNode> nn : seqAdjacenciesFibers_ab)
//   	  {
//			  final WNode a = nn.getFirst();
//			  final WNode b = nn.getSecond();
//			  final SortedSet<WFiber> fibersAb = wNet.getNodePairFibers(a, b);
//			  final SortedSet<WFiber> fibersBa = wNet.getNodePairFibers(b , a);
//			  if (fibersAb.stream().anyMatch(f->!f.isBidirectional())) throw new Net2PlanException ("All fibers must be bidirectional");
//			  if (fibersBa.stream().anyMatch(f->!f.isBidirectional())) throw new Net2PlanException ("All fibers must be bidirectional");
//			  final List<Pair<WFiber,WFiber>> abBa = new ArrayList<> ();
//			  final SortedSet<Integer> idleOpticalSlotRangesInitialSlots = new TreeSet<> ();
//			  for (WFiber ab : fibersAb)
//			  {
//				  if (allFibersToCheckRepetitions.contains(ab)) throw new Net2PlanException ("A fiber appears more than once in an option");
//				  if (allFibersToCheckRepetitions.contains(ab.getBidirectionalPair())) throw new Net2PlanException ("A fiber appears more than once in an option");
//				  allFibersToCheckRepetitions.add(ab);
//				  allFibersToCheckRepetitions.add(ab.getBidirectionalPair());
//				  abBa.add(Pair.of(ab, ab.getBidirectionalPair()));
//		   		  SortedSet<Integer> optionsAb = getIdleOpticalSlotRangesInitialSlots(ab, numContiguousSlotsRequired);
//		   		  SortedSet<Integer> optionsBa = getIdleOpticalSlotRangesInitialSlots(ab.getBidirectionalPair(), numContiguousSlotsRequired);
//		   		  optionsAb.removeAll(unusableSlots);
//		   		  optionsBa.removeAll(unusableSlots);
//		   		  idleOpticalSlotRangesInitialSlots.addAll(Sets.intersection(optionsAb, optionsBa));
//			  }
//			  mapInfoConsideringAllBidi.put(nn, Pair.of(idleOpticalSlotRangesInitialSlots, abBa));
//   	  }
//   	  SortedSet<Integer> validSlotIdsToReturn = null;
//   	  for (Pair<WNode,WNode> nn : seqAdjacenciesFibers_ab)
//   	  {
//           final SortedSet<Integer> validSlotIdsThisHop = mapInfoConsideringAllBidi.get(nn).getFirst();
//           if (validSlotIdsToReturn == null) validSlotIdsToReturn = validSlotIdsThisHop; else validSlotIdsToReturn.retainAll(validSlotIdsThisHop);
//   	  }
//
//   	  /* Filter out valid options Get valid fibers and first slots ids to return, according to the fibers */
//   	  Integer firstSlotToReturn = null;
//   	  for (int potentiallyValidFirstSlotId : validSlotIdsToReturn)
//   	  {
//   		  boolean isOk = true;
//   	   	  for (boolean isAb : new boolean [] {true,false})
//   	   	  {
//   	   	   	  for (boolean isAdd : new boolean [] {true,false})
//   	   	   	  {
//   	   	   		  final SlotOccupationManager<Pair<WNode,Integer>> manager = isAdd? legitimateSignal_directionlessAddOccupation : legitimateSignal_directionlessDropOccupation; 
//   	   	   		  final Pair<WNode,Integer> dirlessModule = (isAdd? (isAb? directionlessAddModuleAbDropModuleBa : directionlessAddModuleBaDropModuleAb) : (isAb? directionlessAddModuleBaDropModuleAb : directionlessAddModuleAbDropModuleBa)).orElse(null);
//   	   	   		  if (dirlessModule == null) continue;
//   	   	   		  final SortedMap<Integer,SortedSet<WLightpath>> occupiedSlots = manager.getOccupiedSlotIds(dirlessModule); 
//   	   	   		  for (int i = 0; i < numContiguousSlotsRequired ; i ++)
//   	   	   			  if (occupiedSlots.containsKey(potentiallyValidFirstSlotId + i)) { isOk = false; break; }
//   	   	   	  }
//   	   	   	  if (!isOk) break;
//   	   	  }
//   	   	  if (isOk) { firstSlotToReturn = potentiallyValidFirstSlotId; break; }
//   	  }
//   	  if (firstSlotToReturn == null) return Optional.empty();
//   	  
//   	  final SortedSet<Integer> res_rangetoReturn = new TreeSet<> (); for (int i = 0; i < numContiguousSlotsRequired ; i ++) res_rangetoReturn.add(firstSlotToReturn + i);
//   	  final List<Pair<WFiber,WFiber>> res_fibersUsed = new ArrayList<> (); 
//   	  for (Pair<WNode,WNode> hop_ab :  seqAdjacenciesFibers_ab)
//   	  {
//   		  final List<Pair<WFiber,WFiber>> fibersThisHop = mapInfoConsideringAllBidi.get(hop_ab).getSecond();
//   		  for (Pair<WFiber,WFiber> bidiPair : fibersThisHop)
//   		  {
//   			  final WFiber ab = bidiPair.getFirst();
//   			  final WFiber ba = bidiPair.getSecond();
//   			  if (this.isOpticalSlotIdsValidAndIdle(ab, res_rangetoReturn) && this.isOpticalSlotIdsValidAndIdle(ba, res_rangetoReturn))
//   			  	{ res_fibersUsed.add(bidiPair); break; }
//   		  }
//   	  }
//   	  assert res_fibersUsed.size() == seqAdjacenciesFibers_ab.size();
//   	  return Optional.of(Pair.of(res_fibersUsed , res_rangetoReturn));
//    }

    /** Searches for a first-fit assignment, where in each hop, one fiber is chosen. Given a set of hops (each hop with at least one fiber as an option),
     * optional directionless add and drop module to occupy,  
     * the number of contiguous optical slots needed, 
     * and (optionally) an initial optical slot (so optical slots of lower id are not consiedered), this method searches for 
     * the lowest-id contiguous range of slots that are available in all the indicated fibers and directionless modules. Note that if the set of fibers 
     * passes more than once in the same fiber, no assignment is possible, and Optional.empty is returned
     * @param seqAdjacenciesFibers_ab see above
     * @param a see above
     * @param b see above
     * @param directionlessAddModuleAb the same as the drop module in direction B-A
     * @param directionlessDropModuleAb the same as the add module in direction B-A
     * @param numContiguousSlotsRequired see above
     * @param minimumInitialSlotId see above
     * @param unusableSlots see above
     * @return see above. If no idle range is found, Optional.empty is returned. 
     */
    public Optional<Pair<List<Pair<WFiber,WFiber>> , SortedSet<Integer>>> spectrumAssignment_firstFitForAdjacenciesBidi (Collection<Pair<WNode,WNode>> seqAdjacenciesFibers_ab,
    		WNode a , WNode b , Optional<Integer> directionlessAddModuleAb , Optional<Integer> directionlessDropModuleAb , 
    		int numContiguousSlotsRequired , Optional<Integer> minimumInitialSlotId , SortedSet<Integer> unusableSlots)
    {
   	 	assert !seqAdjacenciesFibers_ab.isEmpty();
   	 	assert numContiguousSlotsRequired > 0;
   	 	
   	 	/* If loops in the travsersed nodes => we do not allow that */
   	 	final List<SortedSet<WNode>> seqBidiAdjacencies_ab = seqAdjacenciesFibers_ab.stream().map(p->new TreeSet<>(Arrays.asList(p.getFirst() , p.getSecond()))).collect (Collectors.toList());
   	 	if (seqBidiAdjacencies_ab.size() != new HashSet<> (seqBidiAdjacencies_ab).size()) throw new Net2PlanException ("Paths with cycles in traversed adjacencies are not allowed");
   	 	
   	 	/* Get valid fibers and first slots ids to return, according to the fibers */
        /* If a fiber is traversed more than once, there is no possible assignment */
   	 	final List<List<WFiber>> validFibersAb = seqAdjacenciesFibers_ab.stream().
   	 			map(p->wNet.getNodePairFibers(p.getFirst(), p.getSecond()).stream().filter(e->e.isBidirectional()).collect(Collectors.toList())).
   	 			collect (Collectors.toList());
   	 	final List<List<WFiber>> possiblePathsAb = Lists.cartesianProduct(validFibersAb);
   	 	SortedSet<Integer> acceptableRange = null;
   	 	List<WFiber> acceptablePathAb = null;
   	 	for (List<WFiber> pathAb : possiblePathsAb)
   	 	{
   	 		final List<WFiber> pathAbBa = new ArrayList<> ();
   	 		for (WFiber ab : pathAb) { pathAbBa.add(ab); pathAbBa.add(ab.getBidirectionalPair()); }
   	 		final OsmLightpathOccupationInfo lpOccupationAbbA = new OsmLightpathOccupationInfo(pathAbBa, 
   	 				directionlessAddModuleAb.isPresent()? Optional.of(Pair.of(a, directionlessAddModuleAb.get())) : Optional.empty(), 
   	 					directionlessDropModuleAb.isPresent()? Optional.of(Pair.of(b, directionlessDropModuleAb.get())) : Optional.empty(), 
   	 					Optional.empty());
   	 		final SortedSet<Integer> forbidenSlotsBecauseOfAddAndDropBaAndUsable = new TreeSet<> (unusableSlots);
   	 		final Integer addModuleBa = directionlessDropModuleAb.orElse(null);
   	 		final Integer dropModuleBa = directionlessAddModuleAb.orElse(null);
   	 		if (addModuleBa != null)
   	 		forbidenSlotsBecauseOfAddAndDropBaAndUsable.addAll(this.getOccupiedOpticalSlotIdsInDirectionlessAddModule(b, addModuleBa));
   	 		if (dropModuleBa != null)
   	 		forbidenSlotsBecauseOfAddAndDropBaAndUsable.addAll(this.getOccupiedOpticalSlotIdsInDirectionlessDropModule(a, dropModuleBa));
   	 		acceptableRange = spectrumAssignment_firstFit(lpOccupationAbbA, numContiguousSlotsRequired, minimumInitialSlotId, forbidenSlotsBecauseOfAddAndDropBaAndUsable).orElse(null);
   	 		if (acceptableRange != null) { acceptablePathAb = pathAb; break; }
   	 	}
   	 	
   	 	if (acceptableRange == null) return Optional.empty();
	   	final List<Pair<WFiber,WFiber>> res_fibersUsed = acceptablePathAb.stream().map(e->Pair.of(e, e.getBidirectionalPair())).collect(Collectors.toList());
	   	assert res_fibersUsed.size() == seqAdjacenciesFibers_ab.size();
	   	return Optional.of(Pair.of(res_fibersUsed , acceptableRange));
    }
    
    
//    /** Searches for a first-fit assignment. Given a set of fibers to occupy, the optional add and drop directionless modules used, the number of contiguous optical slots needed, 
//     * and (optionally) an initial optical slot (so optical slots of lower id are not consiedered), this method searches for 
//     * the lowest-id contiguous range of slots that are available in all the indicated fibers and directionless modules. Note that if the set of fibers 
//     * passes more than once in the same fiber, no assignment is possible, and Optional.empty is returned
//     * @param seqFibers see above
//     * @param directionlessAddModule see above
//     * @param directionlessDropModule see above
//     * @param numContiguousSlotsRequired see above
//     * @param minimumInitialSlotId see above
//     * @param forbidenSlotIds see above
//     * @return see above. If no idle range is found, Optional.empty is returned. 
//     */
//    public Optional<SortedSet<Integer>> spectrumAssignment_firstFit(Collection<WFiber> seqFibers, 	
//    		Optional<Pair<WNode,Integer>> directionlessAddModule , 
//    		Optional<Pair<WNode,Integer>> directionlessDropModule , 
//    		int numContiguousSlotsRequired , Optional<Integer> minimumInitialSlotId  , SortedSet<Integer> forbidenSlotIds)
//    {
//    	checkSameWNet(seqFibers);
//        assert !seqFibers.isEmpty();
//        assert numContiguousSlotsRequired > 0;
//        
//        /* If a fiber is traversed more than once, there is no possible assignment */
//        if (!(seqFibers instanceof Set)) if (new HashSet<> (seqFibers).size() != seqFibers.size()) return Optional.empty();
//        SortedSet<Integer> intersectionValidSlots = getAvailableSlotIds(seqFibers , directionlessAddModule , directionlessDropModule);
//        if (minimumInitialSlotId.isPresent())
//            intersectionValidSlots = intersectionValidSlots.tailSet(minimumInitialSlotId.get());
//        intersectionValidSlots.removeAll(forbidenSlotIds);
//        if (intersectionValidSlots.size() < numContiguousSlotsRequired) return Optional.empty();
//        
//        final LinkedList<Integer> rangeValid = new LinkedList<> ();
//        for (int slotId : intersectionValidSlots)
//        {
//            if (!rangeValid.isEmpty())
//                if (rangeValid.getLast() != slotId - 1)
//                    rangeValid.clear();
//            rangeValid.add(slotId);
//            assert rangeValid.size() <= numContiguousSlotsRequired;
//            if (rangeValid.size() == numContiguousSlotsRequired) return Optional.of(new TreeSet<> (rangeValid));
//        }
//        return Optional.empty();
//    }

    /** Searches for a first-fit assignment. Given a set of fibers to occupy, the optional add and drop directionless modules used, the number of contiguous optical slots needed, 
     * and (optionally) an initial optical slot (so optical slots of lower id are not consiedered), this method searches for 
     * the lowest-id contiguous range of slots that are available in all the indicated fibers and directionless modules. Note that if the set of fibers 
     * passes more than once in the same fiber, no assignment is possible, and Optional.empty is returned
     * @param lpOccupation see above
     * @param numContiguousSlotsRequired see above
     * @param minimumInitialSlotId see above
     * @param forbidenSlotIds see above
     * @return see above
     */
    public Optional<SortedSet<Integer>> spectrumAssignment_firstFit (OsmLightpathOccupationInfo lpOccupation , int numContiguousSlotsRequired , Optional<Integer> minimumInitialSlotId , SortedSet<Integer> forbidenSlotIds)
    {
    	assert !lpOccupation.getSeqFibersLegitimateSignal().isEmpty();
        assert numContiguousSlotsRequired > 0;

        /* If a fiber is traversed more than once, there is no possible assignment */
        if (lpOccupation.isWithSelfClashing()) return Optional.empty();

        /* Empty slots for legitimate fibers, and add/drop dirless modules  */
        SortedSet<Integer> intersectionValidSlots = getAvailableSlotIds(lpOccupation.getSeqFibersLegitimateSignal() , lpOccupation.getDirectionlessAddModule() , lpOccupation.getDirectionlessDropModule());
        /* Retain slots without legitimate signal in wasted fibers */
        intersectionValidSlots.retainAll(getAvailableSlotIdsEmptyOrWithWaste(lpOccupation.getFibersWithWasteSignal() , Optional.empty() , Optional.empty()));
        /* Remove slots with legitimate signal in any of the wasted add dirless ports */
        for (Pair<WNode,Integer> module : lpOccupation.getAddDirectionlessModulesWithWasteSignal())
        	intersectionValidSlots.removeAll(this.getOccupiedWithLegitimateSignalOpticalSlotIdsInDirectionlessAddModule(module.getFirst(), module.getSecond()));
        /* Remove slots with legitimate signal in any of the wasted drop dirless ports */
        for (Pair<WNode,Integer> module : lpOccupation.getDropDirectionlessModulesWithWasteSignal())
        	intersectionValidSlots.removeAll(this.getOccupiedWithLegitimateSignalOpticalSlotIdsInDirectionlessDropModule(module.getFirst(), module.getSecond()));
        /* Remove invalid slots below the mandated threshold */
        if (minimumInitialSlotId.isPresent())
        	intersectionValidSlots = intersectionValidSlots.tailSet(minimumInitialSlotId.get());
        /* Remove forbiden slot ids */
        intersectionValidSlots.removeAll(forbidenSlotIds);
        if (intersectionValidSlots.size() < numContiguousSlotsRequired) return Optional.empty();

        final LinkedList<Integer> lastXValidSlots = new LinkedList<> ();
        for (int slotId : intersectionValidSlots)
        {
        	if (lastXValidSlots.size() >= numContiguousSlotsRequired) lastXValidSlots.remove(0);
        	lastXValidSlots.add(slotId);
        	if (lastXValidSlots.size() != numContiguousSlotsRequired) continue;
        	if (lastXValidSlots.getLast() - lastXValidSlots.getFirst() == numContiguousSlotsRequired-1) 
        		return Optional.of(new TreeSet<>(lastXValidSlots));
        }
        return Optional.empty();
    }

    
    
    /** Returns the set of all the initial optical slots of a range of the given size, so that all the slots in a range are usable for allocation considering that: i) 
     * the fibers and add/drop modules of the legitimate path are empty, and ii) the fibers and add/drop modules with waste signal do not overlap with the legitimate signal of this or other path. 
     * If the lightpath occupation has potentially self-clashing (e.g. traverses a fiber more than once in its legitimate path or its waste signal clashes with its legitimate path), returns an empty set. 
     * @param lpOccupation see above
     * @param numContiguousSlotsRequired see above
     * @param minimumInitialSlotId see above
     * @param forbidenSlotIds see above
     * @return see above
     */
    public SortedSet<Integer> spectrumAssignment_getAllPotentialFirstSlots (OsmLightpathOccupationInfo lpOccupation , int numContiguousSlotsRequired , Optional<Integer> minimumInitialSlotId , SortedSet<Integer> forbidenSlotIds)
    {
    	assert !lpOccupation.getSeqFibersLegitimateSignal().isEmpty();
        assert numContiguousSlotsRequired > 0;

        /* If a fiber is traversed more than once, there is no possible assignment */
        if (lpOccupation.isWithSelfClashing()) return new TreeSet<> ();

        /* Empty slots for legitimate fibers, and add/drop dirless modules  */
        SortedSet<Integer> intersectionValidSlots = getAvailableSlotIds(lpOccupation.getSeqFibersLegitimateSignal() , lpOccupation.getDirectionlessAddModule() , lpOccupation.getDirectionlessDropModule());
        /* Retain slots without legitimate signal in wasted fibers */
        intersectionValidSlots.retainAll(getAvailableSlotIdsEmptyOrWithWaste(lpOccupation.getFibersWithWasteSignal() , Optional.empty() , Optional.empty()));
        /* Remove slots with legitimate signal in any of the wasted add dirless ports */
        for (Pair<WNode,Integer> module : lpOccupation.getAddDirectionlessModulesWithWasteSignal())
        	intersectionValidSlots.removeAll(this.getOccupiedWithLegitimateSignalOpticalSlotIdsInDirectionlessAddModule(module.getFirst(), module.getSecond()));
        /* Remove slots with legitimate signal in any of the wasted drop dirless ports */
        for (Pair<WNode,Integer> module : lpOccupation.getDropDirectionlessModulesWithWasteSignal())
        	intersectionValidSlots.removeAll(this.getOccupiedWithLegitimateSignalOpticalSlotIdsInDirectionlessDropModule(module.getFirst(), module.getSecond()));
        /* Remove invalid slots below the mandated threshold */
        if (minimumInitialSlotId.isPresent())
        	intersectionValidSlots = intersectionValidSlots.tailSet(minimumInitialSlotId.get());
        /* Remove the forbidden slots if any */
        intersectionValidSlots.removeAll(forbidenSlotIds);
        
        if (intersectionValidSlots.size() < numContiguousSlotsRequired) return new TreeSet<> ();

        final SortedSet<Integer> validFirstSlotsOfContiguousRanges = new TreeSet<> ();
        
        final LinkedList<Integer> lastXValidSlots = new LinkedList<> ();
        for (int slotId : intersectionValidSlots)
        {
        	if (lastXValidSlots.size() >= numContiguousSlotsRequired) lastXValidSlots.remove(0);
        	lastXValidSlots.add(slotId);
        	if (lastXValidSlots.size() != numContiguousSlotsRequired) continue;
        	if (lastXValidSlots.getLast() - lastXValidSlots.getFirst() == numContiguousSlotsRequired-1) 
        		validFirstSlotsOfContiguousRanges.add(lastXValidSlots.getFirst());
        }
        return validFirstSlotsOfContiguousRanges;
    }

    /** Searches for a first-fit assignment for the two given paths, so optical slots can be different for each. 
     * Given two sets of fibers to occupy (paths), the optinal add/drop modules to occupy in each case, the number of contiguous optical slots needed in each, 
     * this method searches for the two lowest-id contiguous ranges of slots, so the first range is available in the first path,
     * the second range is available in the second path. Note that if any path contains a fiber more than once, no allocation is 
     * possible. Note that if path1 and path2 have common fibers, the optical slots returned will always be disjoint. In contrast, the add modules of the two paths or the drop modules of the two paths could be the same 
     * @param lp1 see above
     * @param lp2 see above
     * @param numContiguousSlotsRequired see above
     * @param minimumInitialSlotId see above
     * @param forbidenSlotIds see above
     * @return see above. If no idle range is found, Optional.empty is returned. 
     */
    public Optional<Pair<SortedSet<Integer>,SortedSet<Integer>>> spectrumAssignment_firstFitTwoRoutes (OsmLightpathOccupationInfo lp1, OsmLightpathOccupationInfo lp2 , int numContiguousSlotsRequired , Optional<Integer> minimumInitialSlotId , SortedSet<Integer> forbidenSlotIds)
    {
        /* If a fiber is traversed more than once in any path, there is no possible assignment */
    	if (lp1.isWithSelfClashing()) return Optional.empty();
    	if (lp2.isWithSelfClashing()) return Optional.empty();
        final boolean mutuallyClashingFree = lp1.isMutuallyClashingFreeWith(lp2);
        if (mutuallyClashingFree)
        {
            final Optional<SortedSet<Integer>> firstRouteInitialSlot = spectrumAssignment_firstFit(lp1 , numContiguousSlotsRequired, minimumInitialSlotId , forbidenSlotIds);
            if (!firstRouteInitialSlot.isPresent()) return Optional.empty();
            final Optional<SortedSet<Integer>> secondRouteInitialSlot = spectrumAssignment_firstFit(lp2 , numContiguousSlotsRequired, minimumInitialSlotId , forbidenSlotIds);
            if (!secondRouteInitialSlot.isPresent()) return Optional.empty();
            return Optional.of(Pair.of(firstRouteInitialSlot.get(), secondRouteInitialSlot.get()));
        }

        /* With links in common */
        final SortedSet<Integer> fistPathValidSlots = spectrumAssignment_getAllPotentialFirstSlots(lp1, numContiguousSlotsRequired, minimumInitialSlotId , forbidenSlotIds);
        final SortedSet<Integer> secondPathValidSlots = spectrumAssignment_getAllPotentialFirstSlots(lp2, numContiguousSlotsRequired, minimumInitialSlotId , forbidenSlotIds);
        for(int initialSlot_1 :  fistPathValidSlots)
            for(int initialSlot_2 :  secondPathValidSlots)
            {
                if (Math.abs(initialSlot_1 - initialSlot_2) < numContiguousSlotsRequired) continue;
                final SortedSet<Integer> range1 = new TreeSet<> ();
                final SortedSet<Integer> range2 = new TreeSet<> ();
                for (int cont = 0; cont < numContiguousSlotsRequired ; cont ++) { range1.add(initialSlot_1 + cont);  range2.add(initialSlot_2+cont); } 
                return Optional.of(Pair.of(range1, range2));
            }           
        return Optional.empty();
    }

    
//    /** Searches for a first-fit assignment for the two given paths, so optical slots can be different for each. 
//     * Given two sets of fibers to occupy (paths), the optinal add/drop modules to occupy in each case, the number of contiguous optical slots needed in each, 
//     * this method searches for the two lowest-id contiguous ranges of slots, so the first range is available in the first path,
//     * the second range is available in the second path. Note that if any path contains a fiber more than once, no allocation is 
//     * possible. Note that if path1 and path2 have common fibers, the optical slots returned will always be disjoint. In contrast, the add modules of the two paths or the drop modules of the two paths could be the same 
//     * @param seqFibers_1 see above
//     * @param seqFibers_2 see above
//     * @param directionlessAddModule_1 see above
//     * @param directionlessDropModule_1 see above
//     * @param directionlessAddModule_2 see above
//     * @param directionlessDropModule_2 see above
//     * @param numContiguousSlotsRequired see above
//     * @return see above. If no idle range is found, Optional.empty is returned. 
//     */
//    public Optional<Pair<SortedSet<Integer>,SortedSet<Integer>>> spectrumAssignment_firstFitTwoRoutes(Collection<WFiber> seqFibers_1, Collection<WFiber> seqFibers_2 ,
//    		Optional<Pair<WNode,Integer>> directionlessAddModule_1 , 
//    		Optional<Pair<WNode,Integer>> directionlessDropModule_1 , 
//    		Optional<Pair<WNode,Integer>> directionlessAddModule_2 , 
//    		Optional<Pair<WNode,Integer>> directionlessDropModule_2 , 
//    		int numContiguousSlotsRequired)
//    {
//    	checkSameWNet(seqFibers_1);
//   	 	checkSameWNet(seqFibers_2);
//        /* If a fiber is traversed more than once in any path, there is no possible assignment */
//        if (!(seqFibers_1 instanceof Set)) if (new HashSet<> (seqFibers_1).size() != seqFibers_1.size()) return Optional.empty();
//        if (!(seqFibers_2 instanceof Set)) if (new HashSet<> (seqFibers_2).size() != seqFibers_2.size()) return Optional.empty();
//        final boolean haveLinksInCommon = !Sets.intersection(new HashSet<>(seqFibers_1)  , new HashSet<>(seqFibers_2)).isEmpty();
//        if (!haveLinksInCommon)
//        {
//            final Optional<SortedSet<Integer>> firstRouteInitialSlot = spectrumAssignment_firstFit(seqFibers_1, directionlessAddModule_1 , directionlessDropModule_1 , numContiguousSlotsRequired, Optional.empty());
//            if (!firstRouteInitialSlot.isPresent()) return Optional.empty();
//            final Optional<SortedSet<Integer>> secondRouteInitialSlot = spectrumAssignment_firstFit(seqFibers_2, directionlessAddModule_2 , directionlessDropModule_2 , numContiguousSlotsRequired, Optional.empty());
//            if (!secondRouteInitialSlot.isPresent()) return Optional.empty();
//            return Optional.of(Pair.of(firstRouteInitialSlot.get(), secondRouteInitialSlot.get()));
//        }
//
//        /* With links in common */
//        final SortedSet<Integer> fistPathValidSlots = getAvailableSlotIds(seqFibers_1 , directionlessAddModule_1 , directionlessDropModule_1);
//        final SortedSet<Integer> secondPathValidSlots = getAvailableSlotIds(seqFibers_2 , directionlessAddModule_2 , directionlessDropModule_2);
//        for(int initialSlot_1 :  fistPathValidSlots)
//        {
//            if (!isValidOpticalSlotIdRange(fistPathValidSlots, initialSlot_1, numContiguousSlotsRequired)) continue;
//            for(int initialSlot_2 :  secondPathValidSlots)
//            {
//                if (Math.abs(initialSlot_1 - initialSlot_2) < numContiguousSlotsRequired) continue;
//                if (!isValidOpticalSlotIdRange(secondPathValidSlots, initialSlot_2, numContiguousSlotsRequired)) continue;
//                final SortedSet<Integer> range1 = new TreeSet<> ();
//                final SortedSet<Integer> range2 = new TreeSet<> ();
//                for (int cont = 0; cont < numContiguousSlotsRequired ; cont ++) { range1.add(initialSlot_1 + cont);  range2.add(initialSlot_2+cont); } 
//                return Optional.of(Pair.of(range1, range2));
//            }           
//        }
//        return Optional.empty();
//    }

    /** Given a sequence of fibers (i.e. an intended path of a lightpath) and the maximum unreqenerated distance applicable 
     * for it (maximum km that can traverse without OEO regeneration), returns a partitioning of the input sequence of fibers 
     * into one or more OCh paths (List of WFiber), so that each OCh path has a length lower or equal than the maxUnregeneratedDistanceInKm, 
     * and the number of OChs is minimized. 
     * @param seqFibers see above
     * @param maxUnregeneratedDistanceInKm  see above 
     * @return  see above
     */
    public static List<List<WFiber>> getRegenerationPoints (List<WFiber> seqFibers, double maxUnregeneratedDistanceInKm)
    {
        final List<List<WFiber>> res = new ArrayList<> ();
        res.add(new ArrayList<> ());

        double accumDistance = 0;
        for (WFiber fiber : seqFibers)
        {
            final double fiberLengthInKm = fiber.getLengthInKm();
            if (fiberLengthInKm > maxUnregeneratedDistanceInKm)
                throw new Net2PlanException(String.format("Fiber %d is longer (%f km) than the maximum distance without regenerators (%f km)", fiber.getId(), fiberLengthInKm, maxUnregeneratedDistanceInKm));
            accumDistance += fiberLengthInKm;
            if (accumDistance > maxUnregeneratedDistanceInKm)
            {
                res.add(new ArrayList<> (Arrays.asList(fiber)));
                accumDistance = fiberLengthInKm;
            }
            else
                res.get(res.size()-1).add(fiber);
            
        }
        return res;
    }

    /** Prints a report with the occupation (for debugging pruposes)
     * @return see above
     */
    public String printReport ()
    {
        final StringBuffer st = new StringBuffer ();
        final String RETURN = System.getProperty("line.separator");
        st.append("--- PER FIBER INFORMATION ---" + RETURN);
        for (WFiber e : wNet.getFibers().stream().sorted((e1,e2)->Integer.compare(legitimateSignal_perFiberOccupation.getNumberOfOccupiedSlotIds(e2), legitimateSignal_perFiberOccupation.getNumberOfOccupiedSlotIds(e1))).collect(Collectors.toList()))
        {
            final SortedMap<Integer,SortedSet<WLightpath>> legitimate_occupThisLink = legitimateSignal_perFiberOccupation.getOccupiedSlotIds(e);
            final SortedMap<Integer,SortedSet<WLightpath>> waste_occupThisLink = wasteSignal_perFiberOccupation.getOccupiedSlotIds(e);
            final int legOrWaste_numOccupSlots = Sets.union (legitimate_occupThisLink.keySet() , waste_occupThisLink.keySet()).size();
            final int leg_numOchSubpaths = legitimate_occupThisLink.values().stream().flatMap(s->s.stream()).collect(Collectors.toSet()).size();
            final int leg_numOccupSlots = legitimate_occupThisLink.size();
            final int waste_numOchSubpaths = waste_occupThisLink.values().stream().flatMap(s->s.stream()).collect(Collectors.toSet()).size();
            final int waste_numOccupSlots = waste_occupThisLink.size();
            final boolean hasClashing = leg_numOchSubpaths > 1 || (leg_numOchSubpaths == 1 && waste_numOchSubpaths > 0);
            st.append("Link " + e + ". Occup slots LEG or WASTE: " + legOrWaste_numOccupSlots + "(LEG: " + leg_numOccupSlots + ", WASTE: " + waste_numOccupSlots + "), cap: " + e.getNumberOfValidOpticalChannels() + ", num Och subpaths: " + leg_numOchSubpaths + ", clashing: " + hasClashing + RETURN);
        }
        st.append("--- PER DIRECTIONLESS ADD MODULE INFORMATION ---" + RETURN);
        for (Pair<WNode,Integer> e : legitimateSignal_directionlessAddOccupation.getElementsWithAtLeastOneSlotOccupied().stream().
        		sorted((e1,e2)->{ final int c1 = e1.getFirst().compareTo(e2.getFirst()); if (c1 != 0) return c1; return Integer.compare(e1.getSecond(), e2.getSecond()); }).
        		collect(Collectors.toList()))
        {
            final SortedMap<Integer,SortedSet<WLightpath>> legitimateSignal_occupThisLink = legitimateSignal_directionlessAddOccupation.getOccupiedSlotIds(e);
            final int legitimateSignal_numOchSubpaths = legitimateSignal_occupThisLink.values().stream().flatMap(s->s.stream()).collect(Collectors.toSet()).size();
            final int legitimateSignal_numOccupSlots = legitimateSignal_occupThisLink.size();
            final SortedMap<Integer,SortedSet<WLightpath>> wasteSignal_occupThisLink = legitimateSignal_directionlessAddOccupation.getOccupiedSlotIds(e);
            final int wasteSignal_numOchSubpaths = wasteSignal_occupThisLink.values().stream().flatMap(s->s.stream()).collect(Collectors.toSet()).size();
            final int wasteSignal_numOccupSlots = wasteSignal_occupThisLink.size();
            final int legOrWaste_numOccupSlots = Sets.union (legitimateSignal_occupThisLink.keySet() , wasteSignal_occupThisLink.keySet()).size();
            final boolean hasClashing = legitimateSignal_numOchSubpaths > 1 || (legitimateSignal_numOchSubpaths == 1 && wasteSignal_numOchSubpaths > 0);
            st.append("Directionless add module: Node " + e.getFirst() + " - Index: " + e.getSecond() + ". Occup slots LEG or WASTE: " + legOrWaste_numOccupSlots + "(LEG: " + legitimateSignal_numOccupSlots + ", WASTE: " + wasteSignal_numOccupSlots + "), num Och subpaths: " + legitimateSignal_numOchSubpaths + ", clashing: " + hasClashing + RETURN);
        }
        st.append("--- PER DIRECTIONLESS DROP MODULE INFORMATION ---" + RETURN);
        for (Pair<WNode,Integer> e : legitimateSignal_directionlessDropOccupation.getElementsWithAtLeastOneSlotOccupied().stream().
        		sorted((e1,e2)->{ final int c1 = e1.getFirst().compareTo(e2.getFirst()); if (c1 != 0) return c1; return Integer.compare(e1.getSecond(), e2.getSecond()); }).
        		collect(Collectors.toList()))
        {
            final SortedMap<Integer,SortedSet<WLightpath>> legitimateSignal_occupThisLink = legitimateSignal_directionlessDropOccupation.getOccupiedSlotIds(e);
            final int legitimateSignal_numOchSubpaths = legitimateSignal_occupThisLink.values().stream().flatMap(s->s.stream()).collect(Collectors.toSet()).size();
            final int legitimateSignal_numOccupSlots = legitimateSignal_occupThisLink.size();
            final SortedMap<Integer,SortedSet<WLightpath>> wasteSignal_occupThisLink = legitimateSignal_directionlessDropOccupation.getOccupiedSlotIds(e);
            final int wasteSignal_numOchSubpaths = wasteSignal_occupThisLink.values().stream().flatMap(s->s.stream()).collect(Collectors.toSet()).size();
            final int wasteSignal_numOccupSlots = wasteSignal_occupThisLink.size();
            final int legOrWaste_numOccupSlots = Sets.union (legitimateSignal_occupThisLink.keySet() , wasteSignal_occupThisLink.keySet()).size();
            final boolean hasClashing = legitimateSignal_numOchSubpaths > 1 || (legitimateSignal_numOchSubpaths == 1 && wasteSignal_numOchSubpaths > 0);
            st.append("Directionless drop module: Node " + e.getFirst() + " - Index: " + e.getSecond() + ". Occup slots LEG or WASTE: " + legOrWaste_numOccupSlots + "(LEG: " + legitimateSignal_numOccupSlots + ", WASTE: " + wasteSignal_numOccupSlots + "), num Och subpaths: " + legitimateSignal_numOchSubpaths + ", clashing: " + hasClashing + RETURN);
        }
        return st.toString();
    }

    /** Returns true if the design is ok respect to spectrum occupation: no optical slot in any fiber nor directionless module is occupied by more than one 
     * lightpath, and no optical slot in a fiber is outside the valid range for that fiber
     * @return see above
     */
    public boolean isSpectrumOccupationOk ()
    {
        for (WFiber e : legitimateSignal_perFiberOccupation.getElementsWithAtLeastOneSlotOccupied())
        {
            final SortedMap<Integer,SortedSet<WLightpath>> legitimate = legitimateSignal_perFiberOccupation.getOccupiedSlotIds(e);
            final SortedMap<Integer,SortedSet<WLightpath>> waste = wasteSignal_perFiberOccupation.getOccupiedSlotIds(e);
            for (Entry<Integer,SortedSet<WLightpath>> entryLegit : legitimate.entrySet())
            {
            	final int slot = entryLegit.getKey();
            	final int numLegitLps = entryLegit.getValue().size();
            	if (numLegitLps > 1) return false;
            	if (numLegitLps == 1 && waste.getOrDefault(slot, new TreeSet<> ()).size() > 0) return false;
            }
        }
        for (Pair<WNode,Integer> e : legitimateSignal_directionlessAddOccupation.getElementsWithAtLeastOneSlotOccupied())
        {
            final SortedMap<Integer,SortedSet<WLightpath>> legitimate = legitimateSignal_directionlessAddOccupation.getOccupiedSlotIds(e);
            final SortedMap<Integer,SortedSet<WLightpath>> waste = wasteSignal_directionlessAddOccupation.getOccupiedSlotIds(e);
            for (Entry<Integer,SortedSet<WLightpath>> entryLegit : legitimate.entrySet())
            {
            	final int slot = entryLegit.getKey();
            	final int numLegitLps = entryLegit.getValue().size();
            	if (numLegitLps > 1) return false;
            	if (numLegitLps == 1 && waste.getOrDefault(slot, new TreeSet<> ()).size() > 0) return false;
            }
        }
        for (Pair<WNode,Integer> e : legitimateSignal_directionlessDropOccupation.getElementsWithAtLeastOneSlotOccupied())
        {
            final SortedMap<Integer,SortedSet<WLightpath>> legitimate = legitimateSignal_directionlessDropOccupation.getOccupiedSlotIds(e);
            final SortedMap<Integer,SortedSet<WLightpath>> waste = wasteSignal_directionlessDropOccupation.getOccupiedSlotIds(e);
            for (Entry<Integer,SortedSet<WLightpath>> entryLegit : legitimate.entrySet())
            {
            	final int slot = entryLegit.getKey();
            	final int numLegitLps = entryLegit.getValue().size();
            	if (numLegitLps > 1) return false;
            	if (numLegitLps == 1 && waste.getOrDefault(slot, new TreeSet<> ()).size() > 0) return false;
            }
        }
        return true;
    }

    /** Returns true if the design is ok respect to spectrum occupation for that lightpath: all optical slots occupied in the fiber are valid,
     *  and with no clashing with other lightpaths in the fibers nor in the directionless add/drop modules (if any)
     * @param lp see above
     * @return see above
     */
    public boolean isSpectrumOccupationOk (WLightpath lp)
    {
	   	 for (WFiber e : lp.getSeqFibers())
	   	 {
				 if (!e.getValidOpticalSlotIds().containsAll(lp.getOpticalSlotIds())) return false;
	            final SortedMap<Integer,SortedSet<WLightpath>> legitimate = legitimateSignal_perFiberOccupation.getOccupiedSlotIds(e);
	            final SortedMap<Integer,SortedSet<WLightpath>> waste = wasteSignal_perFiberOccupation.getOccupiedSlotIds(e);
				 for (int slot : lp.getOpticalSlotIds())
		   		 {
	            	final int numLegitLps = legitimate.getOrDefault(slot , new TreeSet<> ()).size();
	            	if (numLegitLps > 1) return false;
	            	if (numLegitLps == 1 && waste.getOrDefault(slot, new TreeSet<> ()).size() > 0) return false;
		   		 }
	   	 }
	   	 for (boolean isAdd : new boolean [] { true , false} )
	   	 {
	   		 final Integer dirlessIndex = (isAdd? lp.getDirectionlessAddModuleIndexInOrigin() : lp.getDirectionlessDropModuleIndexInDestination()).orElse(null);
	   		 if (dirlessIndex == null) continue;
	   		 final Pair<WNode,Integer> e = Pair.of(isAdd? lp.getA() : lp.getB(), dirlessIndex); 
			 final SortedMap<Integer,SortedSet<WLightpath>> legitimate = isAdd? legitimateSignal_directionlessAddOccupation.getOccupiedSlotIds(e) : legitimateSignal_directionlessDropOccupation.getOccupiedSlotIds(e);
			 final SortedMap<Integer,SortedSet<WLightpath>> waste = isAdd? wasteSignal_directionlessAddOccupation.getOccupiedSlotIds(e) : wasteSignal_directionlessDropOccupation.getOccupiedSlotIds(e);
			 for (int slot : lp.getOpticalSlotIds())
			 {
            	final int numLegitLps = legitimate.getOrDefault(slot , new TreeSet<> ()).size();
            	if (numLegitLps > 1) return false;
            	if (numLegitLps == 1 && waste.getOrDefault(slot, new TreeSet<> ()).size() > 0) return false;
			 }
	   	 }
        return true;
    }
    
//    /** Checks if the optical slot occupation --
//     */
//    public void checkNetworkSlotOccupation () // PABLO
//    {
//        for (Entry<WFiber,SortedMap<Integer,SortedSet<WLightpath>>> occup_e : occupation_f_s_ll.entrySet())
//        {
//            final WFiber e = occup_e.getKey();
//            assert e.isBidirectional();
//            final SortedMap<Integer,SortedSet<WLightpath>> occup = occup_e.getValue();
//            if (!e.getValidOpticalSlotIds().containsAll(occup.keySet())) throw new Net2PlanException ("The optical slots occupied at link " + e + " are outside the valid range");
//            for (Set<WLightpath> rs : occup.values())
//                if (rs.size() != 1) throw new Net2PlanException ("The optical slots occupation is not correct");
//        }       
//    }
    
    private static boolean isValidOpticalSlotIdRange (SortedSet<Integer> validSlots , int initialSlot , int numContiguousSlots)
    {
        for (int cont = 0; cont < numContiguousSlots ; cont ++)
            if (!validSlots.contains(initialSlot + cont)) return false;
        return true;
    }

	/** FA: Returns the optical slots that are usable (valid and idle, not occupied by waste or legitimate signal) in the given fiber
	 * @param wdmLink see above
	 * @return  see above
	 */
	public SortedSet<Integer> getIdleOpticalSlotIds (WFiber wdmLink)
	{
		checkSameWNet(wdmLink);
		final SortedSet<Integer> res = wdmLink.getValidOpticalSlotIds();
		res.removeAll(getOccupiedOpticalSlotIds(wdmLink));
		return res;
	}

	/** FA: Returns the optical slots that are empty, or occupied just by waste signals, but not by a legitimate signal of a lightpath, in the given fiber
	 * @param wdmLink see above
	 * @return  see above
	 */
	public SortedSet<Integer> getOpticalSlotIdsEmptyOrWithWaste (WFiber wdmLink)
	{
		checkSameWNet(wdmLink);
		final SortedSet<Integer> res = wdmLink.getValidOpticalSlotIds();
		res.removeAll(getOpticalSlotIdsWithLegitimateSignal(wdmLink));
		return res;
	}

	/** FA: Returns the optical slots that are occupied (by waste or legitimate signals) in the given directionless add module
	 * @param node see above
	 * @param directionlessModuleIndex see above
	 * @return see above
	 */
	public SortedSet<Integer> getOccupiedOpticalSlotIdsInDirectionlessAddModule (WNode node , int directionlessModuleIndex)
	{
		checkSameWNet(node);
		final SortedSet<Integer> res = new TreeSet<> (getOccupiedResourcesInDirectionlessAddModule(node, directionlessModuleIndex , OpticalSignalOccupationType.LEGITIMATESIGNAL).keySet());
		res.addAll(getOccupiedResourcesInDirectionlessAddModule(node, directionlessModuleIndex , OpticalSignalOccupationType.WASTESIGNAL).keySet());
		return res;
	}

	/** FA: Returns the optical slots that are occupied by a legitimate signals in the given directionless add module
	 * @param node see above
	 * @param directionlessModuleIndex see above
	 * @return see above
	 */
	public SortedSet<Integer> getOccupiedWithLegitimateSignalOpticalSlotIdsInDirectionlessAddModule (WNode node , int directionlessModuleIndex)
	{
		checkSameWNet(node);
		return new TreeSet<> (getOccupiedResourcesInDirectionlessAddModule(node, directionlessModuleIndex , OpticalSignalOccupationType.LEGITIMATESIGNAL).keySet());
	}

	/** FA: Returns the optical slots that are occupied by a legitimate signals in the given directionless drop module
	 * @param node see above
	 * @param directionlessModuleIndex see above
	 * @return see above
	 */
	public SortedSet<Integer> getOccupiedWithLegitimateSignalOpticalSlotIdsInDirectionlessDropModule (WNode node , int directionlessModuleIndex)
	{
		checkSameWNet(node);
		return new TreeSet<> (getOccupiedResourcesInDirectionlessDropModule(node, directionlessModuleIndex , OpticalSignalOccupationType.LEGITIMATESIGNAL).keySet());
	}

	
	/** FA: Returns the optical slots that are occupied (by waste or legitimate signals) in the given directionless add module
	 * @param node see above
	 * @param directionlessModuleIndex see above
	 * @return see above
	 */
	public SortedSet<Integer> getOccupiedOpticalSlotIdsInDirectionlessDropModule (WNode node , int directionlessModuleIndex)
	{
		checkSameWNet(node);
		final SortedSet<Integer> res = new TreeSet<> (getOccupiedResourcesInDirectionlessDropModule(node, directionlessModuleIndex , OpticalSignalOccupationType.LEGITIMATESIGNAL).keySet());
		res.addAll(getOccupiedResourcesInDirectionlessDropModule(node, directionlessModuleIndex , OpticalSignalOccupationType.WASTESIGNAL).keySet());
		return res;
	}


	/** FA: Returns the optical slots where there is wavelength clashing, i.e. a LEGITIMATE lightpath signal is occupying it, together with the waste or legitimate signal of this or other lightpath
	 * @param wdmLink see above
	 * @return  see above
	 */
	public SortedSet<Integer> getClashingOpticalSlotIds (WFiber wdmLink)
	{
		checkSameWNet(wdmLink);
		final SortedSet<Integer> res = new TreeSet<> ();
		final SortedMap<Integer,SortedSet<WLightpath>> legitimate = getOccupiedResources (wdmLink , OpticalSignalOccupationType.LEGITIMATESIGNAL);
		final SortedMap<Integer,SortedSet<WLightpath>> waste = getOccupiedResources (wdmLink , OpticalSignalOccupationType.WASTESIGNAL);
		for (Entry<Integer,SortedSet<WLightpath>> entryOfLegitimateOccupation : legitimate.entrySet())
		{
			final int slotId = entryOfLegitimateOccupation.getKey();
			if (entryOfLegitimateOccupation.getValue().isEmpty()) continue; 
			if (entryOfLegitimateOccupation.getValue().size() > 1) { res.add(slotId); continue; }
			if (!waste.getOrDefault(slotId, new TreeSet<> ()).isEmpty())
				res.add(slotId);
		}
		return res;
	}

	/** FA: Returns the optical slots where there is wavelength clashing in the directionless add module, i.e. a LEGITIMATE lightpath signal is occupying it, together with the waste or legitimate signal of this or other lightpath
	 * @param wdmLink see above
	 * @return  see above
	 */
	public SortedSet<Integer> getClashingOpticalSlotIdsInDirectionlessAddModule (WNode node , int addDirectionlessModule)
	{
		checkSameWNet(node);
		final SortedSet<Integer> res = new TreeSet<> ();
		final SortedMap<Integer,SortedSet<WLightpath>> legitimate = getOccupiedResourcesInDirectionlessAddModule (node , addDirectionlessModule , OpticalSignalOccupationType.LEGITIMATESIGNAL);
		final SortedMap<Integer,SortedSet<WLightpath>> waste = getOccupiedResourcesInDirectionlessAddModule (node , addDirectionlessModule , OpticalSignalOccupationType.WASTESIGNAL);
		for (Entry<Integer,SortedSet<WLightpath>> entryOfLegitimateOccupation : legitimate.entrySet())
		{
			final int slotId = entryOfLegitimateOccupation.getKey();
			if (entryOfLegitimateOccupation.getValue().isEmpty()) continue; 
			if (entryOfLegitimateOccupation.getValue().size() > 1) { res.add(slotId); continue; }
			if (!waste.getOrDefault(slotId, new TreeSet<> ()).isEmpty())
				res.add(slotId);
		}
		return res;
	}

	/** FA: Returns the optical slots where there is wavelength clashing in the directionless drop module, i.e. a LEGITIMATE lightpath signal is occupying it, together with the waste or legitimate signal of this or other lightpath
	 * @param wdmLink see above
	 * @return  see above
	 */
	public SortedSet<Integer> getClashingOpticalSlotIdsInDirectionlessDropModule (WNode node , int dropDirectionlessModule)
	{
		checkSameWNet(node);
		final SortedSet<Integer> res = new TreeSet<> ();
		final SortedMap<Integer,SortedSet<WLightpath>> legitimate = getOccupiedResourcesInDirectionlessDropModule (node , dropDirectionlessModule , OpticalSignalOccupationType.LEGITIMATESIGNAL);
		final SortedMap<Integer,SortedSet<WLightpath>> waste = getOccupiedResourcesInDirectionlessDropModule (node , dropDirectionlessModule , OpticalSignalOccupationType.WASTESIGNAL);
		for (Entry<Integer,SortedSet<WLightpath>> entryOfLegitimateOccupation : legitimate.entrySet())
		{
			final int slotId = entryOfLegitimateOccupation.getKey();
			if (entryOfLegitimateOccupation.getValue().isEmpty()) continue; 
			if (entryOfLegitimateOccupation.getValue().size() > 1) { res.add(slotId); continue; }
			if (!waste.getOrDefault(slotId, new TreeSet<> ()).isEmpty())
				res.add(slotId);
		}
		return res;
	}


	/** FA: Returns the number optical slots where there is wavelength clashing, i.e. 1) the legitimate signal of two or more lightpaths is using it, or 2) the legitimate signal of one lightpath and the waste signal of the same or any other lightpath is using it
	 * @param wdmLink see above
	 * @return  see above
	 */
	public int getNumberOfClashingOpticalSlotIds (WFiber wdmLink)
	{
		checkSameWNet(wdmLink);
		return getClashingOpticalSlotIds (wdmLink).size();
	}
	
	/** FA: Returns the optical slots that are usable (valid and idle, not occupied by waste or legitimate signals of any lightpath) in the given fiber
	 * @param wdmLink see above
	 * @param numContiguousSlots see above
	 * @return  see above
	 */
	public SortedSet<Integer> getIdleOpticalSlotRangesInitialSlots (WFiber wdmLink , int numContiguousSlots)
	{
		final SortedSet<Integer> idleSlots = getIdleOpticalSlotIds (wdmLink);
		final SortedSet<Integer> res = new TreeSet<> ();
		for (int s : idleSlots)
		{
			boolean ok = true;
			for (int cont = 0; cont < numContiguousSlots ; cont ++)
				if (!idleSlots.contains(s+cont)) { ok = false; break; }
			if (ok) res.add(s);
		}
		return res;
	}

		
	/** FA: Indicates if the optical slots are usable (valid and idle, not occupied by waste or legitimate optical signals of any lightpath) in the given fiber
	 * @param wdmLink see above
	 * @param slotsIds see above
	 * @return see above
	 */
	public boolean isOpticalSlotIdsValidAndIdle (WFiber wdmLink , SortedSet<Integer> slotsIds)
	{
		checkSameWNet(wdmLink);
		return getIdleOpticalSlotIds(wdmLink).containsAll(slotsIds);
	}
	
	/** FA: Indicates if the optical slots are usable (valid and idle, not occupied by waste or legitimate optical signals of any lightpath) in the given add directionless module index
	 * @param node see above
	 * @param directionlessModuleIndex see above
	 * @param slotsIds see above
	 * @return see above
	 */
	public boolean isOpticalSlotIdsValidAndIdleInAddDirectionlessModule (WNode node , int directionlessModuleIndex , SortedSet<Integer> slotsIds)
	{
		checkSameWNet(node);
		return getOccupiedOpticalSlotIdsInDirectionlessAddModule(node, directionlessModuleIndex).stream().allMatch(slot->!slotsIds.contains(slot));
	}
	
	/** FA: Indicates if the optical slots are usable (valid and idle, not occupied by waste or legitimate optical signals of any lightpath) in the given drop directionless module index
	 * @param node see above
	 * @param directionlessModuleIndex see above
	 * @param slotsIds see above
	 * @return see above
	 */
	public boolean isOpticalSlotIdsValidAndIdleInDropDirectionlessModule (WNode node , int directionlessModuleIndex , SortedSet<Integer> slotsIds)
	{
		checkSameWNet(node);
		return getOccupiedOpticalSlotIdsInDirectionlessDropModule(node, directionlessModuleIndex).stream().allMatch(slot->!slotsIds.contains(slot));
	}
	

	
	/** For the provided collection of fibers, indicates the minimum and maximum optical slot id that is valid for all the 
     * fibers in the collection
     * @param wdmLinks see above
     * @return see above
     */
    public static Pair<Integer,Integer> getMinimumAndMaximumValidSlotIdsInTheGrid (Collection<WFiber> wdmLinks)
    {
        if (wdmLinks.isEmpty()) throw new Net2PlanException ("No WDM links");
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        for (WFiber wdmLink : wdmLinks)
        {
        	final Pair<Integer,Integer> minMax = wdmLink.getMinMaxValidSlotId();
            min = Math.max(min, minMax.getFirst());
            max = Math.min(max, minMax.getSecond());
        }
        return Pair.of(min, max);
    }

    /** Returns a list with the lasing loops in the network. These are loops of traversed fibers, where the signal would propagate indefinitely without being blocked by 
     * any optical switch. This occurs e.g. in rings where all the nodes are filterless. 
    * @return see above
    */
   public List<List<WFiber>> getUnavoidableLasingLoops ()
    {
		final DefaultDirectedGraph<WFiber , Object> graphFiberToFiberPropagation = new DefaultDirectedGraph<WFiber , Object>(Object.class);
     	 for (WFiber fiber : wNet.getFibers())
     		 graphFiberToFiberPropagation.addVertex(fiber);

     	 for (WNode node : wNet.getNodes())
     	 {
     		 final IOadmArchitecture type = node.getOpticalSwitchingArchitecture();
     		 for (WFiber inFiber : node.getIncomingFibers())
     		 {
     			 final SortedSet<WFiber> outFibersProp = type.getOutFibersUnavoidablePropagationFromInputFiber(inFiber);
     			 for (WFiber propFiber : outFibersProp)
     				 graphFiberToFiberPropagation.addEdge(inFiber , propFiber);
     		 }
     	 }
		final DirectedSimpleCycles<WFiber,Object> cycleDetector = new JohnsonSimpleCycles<> (graphFiberToFiberPropagation); 
   	return cycleDetector.findSimpleCycles();
    }
    
//    /** Given a contigous path, candidate to be assigned to a unicast lightpath, computes
//     *
//     * 1) the fibers where the signal would propagate (this includes e.g. fibers where signal propagates if traversing filterless nodes),
//     * 2) the list of cycles occurring (if any) that would result in laser loops,
//     * 3) an indication if the path is multipath-free. A lightpath is not multipath free when any fiber in the
//     * legitimate path is receiving the output power of the signal more than once, coming from different paths, and thus destroying the signal.
//     *
//     *
//    * @param links the sequence of fibers to be traversed. Must be a contigous path.
//    * @return see above
//    */
//   public static Triple<SortedSet<WFiber>,List<List<WFiber>>,Boolean> getPropagatingFibersLasingLoopsAndIsMultipathOk (List<WFiber> links)
//    {
////	   Idea:
////		   - Devuelva solo waste
////		   - Multipath ok es que no haya un waste mio solape con legitimate mio
////		   - Incluya Pair<WNode,Integer> de drop modules en non-directionless, ocupados
////		   - El add module ocupado se supone que es iunicamente el de origen
//		   
//	   
//	   
//	   
//   	 if (links.isEmpty()) throw new Net2PlanException ("The path is empty");
//   	 if (getContinousSequenceOfNodes(links).stream().allMatch(n->n.getOpticalSwitchingArchitecture().isNeverCreatingWastedSpectrum())) return Triple.of(new TreeSet<> (links), new  ArrayList<> (), true);
//   	 
//   	 final WFiber dummyFiberAdd = WFiber.createDummyFiber(0);
//   	 final WFiber dummyFiberDrop = WFiber.createDummyFiber(1);
//   	 
//   	 /* Construct a graph starting from add fiber. Stop when all fibers have been processed */
//   	 final SortedSet<WFiber> fibersPendingToProcess = new TreeSet<> ();
//   	 fibersPendingToProcess.add(dummyFiberAdd);
//   	 final SortedSet<WFiber> fibersAlreadyProcessed = new TreeSet<> ();
//   	 final DefaultDirectedGraph<WFiber , Object> propagationGraph = new DefaultDirectedGraph<> (Object.class);
//		 propagationGraph.addVertex(dummyFiberAdd);
//   	 while (!fibersPendingToProcess.isEmpty())
//   	 {
//   		 final WFiber fiberToProcess = fibersPendingToProcess.first();
//   		 if (fibersAlreadyProcessed.contains(fiberToProcess)) { fibersPendingToProcess.remove(fiberToProcess); continue; }
//   		 assert propagationGraph.containsVertex(fiberToProcess);
//   		 /* process the fiber */
//   		 if (fiberToProcess.equals(dummyFiberAdd))
//   		 {
//   			 /* Add lightpath dummy fiber */
//   			 final WNode addNode = links.get(0).getA();
//      		 for (WFiber propFiber : addNode.getOpticalSwitchingArchitecture().getOutFibersIfAddToOutputFiber(links.get(0)))
//      		 {
//      			 if (!propagationGraph.containsVertex(propFiber)) propagationGraph.addVertex(propFiber);
//      			 propagationGraph.addEdge(dummyFiberAdd, propFiber);
//      			 fibersPendingToProcess.add(propFiber);
//      		 }
//   		 } else if (fiberToProcess.equals(dummyFiberDrop))
//   		 {
//   			 /* Drop lightpath dummy fiber => do nothing */
//   		 } else
//   		 {
//   			 final WNode switchNode = fiberToProcess.getB();
//      		 for (WFiber propFiber : switchNode.getOpticalSwitchingArchitecture().getOutFibersUnavoidablePropagationFromInputFiber(fiberToProcess))
//      		 {
//      			 if (!propagationGraph.containsVertex(propFiber)) propagationGraph.addVertex(propFiber);
//      			 propagationGraph.addEdge(fiberToProcess, propFiber);
//      			 fibersPendingToProcess.add(propFiber);
//      		 }
//      		 final int indexOfFiberInPath = links.indexOf(fiberToProcess);
//   			 final boolean isExpress = indexOfFiberInPath >= 0 && (indexOfFiberInPath < links.size()-1);
//   			 final boolean isDrop = indexOfFiberInPath == links.size() - 1;
//   			 if (isExpress)
//   			 {
//      			 final WFiber inFiber = links.get(indexOfFiberInPath); assert inFiber.equals(fiberToProcess);
//      			 final WFiber outFiber = links.get(indexOfFiberInPath + 1);
//      			 final WNode expressNode = outFiber.getA();
//      			 assert expressNode.equals(inFiber.getB());
//         		 for (WFiber propFiber : expressNode.getOpticalSwitchingArchitecture().getOutFibersIfExpressFromInputToOutputFiber(inFiber , outFiber))
//         		 {
//         			 if (!propagationGraph.containsVertex(propFiber)) propagationGraph.addVertex(propFiber);
//         			 propagationGraph.addEdge(inFiber, propFiber);
//         			 fibersPendingToProcess.add(propFiber);
//         		 }
//   			 } else if (isDrop)
//   			 {
//      			 if (!propagationGraph.containsVertex(dummyFiberDrop)) propagationGraph.addVertex(dummyFiberDrop);
//      			 propagationGraph.addEdge(fiberToProcess, dummyFiberDrop);
//      			 fibersPendingToProcess.add(dummyFiberDrop);
//   			 }
//   		 }
//   		fibersAlreadyProcessed.add (fiberToProcess);
//   		 fibersPendingToProcess.remove(fiberToProcess);
//   	 }
//   	 
//   	 if (!propagationGraph.containsVertex(dummyFiberDrop)) throw new Net2PlanException ("The signal of this lightpath is not reaching the drop node");
//
//   	 final SortedSet<WFiber> propagatedNonDummyFibers = propagationGraph.vertexSet().stream().filter(e->!e.equals(dummyFiberDrop) && !e.equals(dummyFiberAdd)).collect(Collectors.toCollection(TreeSet::new));
//   	 
//   	 boolean multipathFree = links.stream().allMatch(v->propagationGraph.incomingEdgesOf(v).size() == 1);
//   	 multipathFree &= propagationGraph.incomingEdgesOf(dummyFiberDrop).size() == 1;
//   	 final DirectedSimpleCycles<WFiber,Object> cycleDetector = new JohnsonSimpleCycles<> (propagationGraph); 
//   	 final List<List<WFiber>> lasingCycles = cycleDetector.findSimpleCycles();
//   	 return Triple.of(propagatedNonDummyFibers , lasingCycles, multipathFree);
//    }
    
    
    private void checkSameWNet (WAbstractNetworkElement...abstractNetworkElements)
    {
   	 for (WAbstractNetworkElement e : abstractNetworkElements) if (e.getNetPlan() != this.wNet.getNetPlan()) throw new Net2PlanException ("Different wNet object");
    }
    private void checkSameWNet(Collection<? extends WAbstractNetworkElement> abstractNetworkElements)
    {
   	 for (WAbstractNetworkElement e : abstractNetworkElements) if (e.getNetPlan() != this.wNet.getNetPlan()) throw new Net2PlanException ("Different wNet object");
    }

    private static boolean isContinuousUnicastPath (List<WFiber> path) { WFiber prevLink = null; for (WFiber e : path) { if (prevLink != null) { if (!prevLink.getB().equals(e.getA())) return false; } prevLink = e; } return true; }
    private static boolean isPassingSameNodeMoreThanOnce (List<WFiber> path) { final SortedSet<WNode> nodes = new TreeSet<> (); nodes.add(path.get(0).getA()); for (WFiber e : path) { if (nodes.contains(e.getB())) return true; nodes.add(e.getB()); } return false; }
    public static List<WNode> getContinousSequenceOfNodes (List<WFiber> path) { if (path.isEmpty()) return new ArrayList<> (); final List<WNode> res = new ArrayList<> (path.size() + 1); res.add(path.get(0).getA()); for (WFiber e : path) { if (!e.getA ().equals(res.get(res.size()-1))) throw new Net2PlanException ("Not contiguous"); res.add(e.getB()); }  return res; }


    
    
    
}
