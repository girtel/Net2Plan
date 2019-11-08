/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
public class OpticalSpectrumManagerOld
{
	private WNet wNet;
	final private SortedMap<WFiber,SortedMap<Integer,SortedSet<WLightpath>>> occupation_f_s_ll = new TreeMap<> ();
	final private SortedMap<WLightpath,SortedMap<WFiber,SortedSet<Integer>>> occupation_ll_f_s = new TreeMap<> ();

	private OpticalSpectrumManagerOld (WNet wNet) { this.wNet = wNet; }
	
	/** Creates this object, asociated to a given network
	 * @param net the network
	 * @return see above
	 */
	public static OpticalSpectrumManagerOld createFromRegularLps (WNet net)
    {
		final OpticalSpectrumManagerOld osm = new OpticalSpectrumManagerOld(net);
		for (WLightpath lp : net.getLightpaths())
			osm.allocateOccupation(lp, lp.getSeqFibers(), lp.getOpticalSlotIds());
        return osm;
    }

	/** Resets this object, makes it associated to a given network and according to their lightpaths
	 * @param net the network
	 * @return see above
	 */
	public OpticalSpectrumManagerOld resetFromRegularLps (WNet net)
    {
		this.wNet = net;
		this.occupation_f_s_ll.clear();
		this.occupation_ll_f_s.clear();
		for (WLightpath lp : net.getLightpaths())
			this.allocateOccupation(lp, lp.getSeqFibers(), lp.getOpticalSlotIds());
        return this;
    }


	/** Returns the set of the optical slots ids that are idle in ALL the fibers provided 
     * @param wdmLinks the set of fibers
     * @return see above
     */
    public SortedSet<Integer> getAvailableSlotIds (Collection<WFiber> wdmLinks)
    {
   	 checkSameWNet(wdmLinks);
        if (wdmLinks.isEmpty()) throw new Net2PlanException ("No WDM links");
        final SortedSet<Integer> validSlotIds = this.getIdleOpticalSlotIds(wdmLinks.iterator().next());
        final Iterator<WFiber> itLink = wdmLinks.iterator();
        itLink.next();
        while (itLink.hasNext())
            validSlotIds.retainAll(this.getIdleOpticalSlotIds(itLink.next()));
        return validSlotIds;
    }
    
    /** Given a fiber, returns a map with the occupied optical slot ids, mapped to the set of lightpaths that occupy it. 
     * Note that if more than one lightpath occupies a given slot, means that spectrum clashing occurs in that slot   
     * @param fiber the input fiber
     * @return see above
     */
    public SortedMap<Integer,SortedSet<WLightpath>> getOccupiedResources (WFiber fiber)
    {
   	 checkSameWNet(fiber);
    	return this.occupation_f_s_ll.getOrDefault(fiber, new TreeMap<> ());
    }

    /** Given a fiber, returns the set of optical slots occupied by at least one traversing lightpath
     * @param fiber see above
     * @return see above
     */
    public SortedSet<Integer> getOccupiedOpticalSlotIds (WFiber fiber)
    {
   	 checkSameWNet(fiber);
		SortedMap<Integer,SortedSet<WLightpath>> occupiedSlotsPerLightpath = this.occupation_f_s_ll.get(fiber);
		if(occupiedSlotsPerLightpath == null)
			return new TreeSet<>();
    	return new TreeSet<> (occupiedSlotsPerLightpath.keySet());
    }

    /** Given a set of fibers and a set of optical slots, returns true if ALL the optical slots are idle in ALL the fibers 
     * @param wdmLinks the fibers
     * @param slotIds the optical slots
     * @return see above
     */
    public boolean isAllocatable (Collection<WFiber> wdmLinks , SortedSet<Integer> slotIds)
    {
   	 checkSameWNet(wdmLinks);
        if (wdmLinks.size() != new HashSet<> (wdmLinks).size()) return false;
        for (WFiber e : wdmLinks)
            if (!this.isOpticalSlotIdsValidAndIdle(e , slotIds))
                return false;
        return true;
    }

    /** Returns true if the indicated lightpath is already accounted in this optical spectrm manager. This means that its occupation 
     * of optical slots in the traversed fibers is accounted for. Note that if the lightpath ocupation was introduced, and then 
     * its route is changed, the occupied resources should be updated by the user in optical spectrum manager.  
     * @param lp the lightpath
     * @return see above
     */
    public boolean isAlreadyAccounted (WLightpath lp) { checkSameWNet(lp); return this.occupation_ll_f_s.get(lp) != null; }

    /** Accounts for the occupation of a lightpath, updating the information in the spectrum manager
     * @param lp the lightpath
     * @param wdmLinks the set of fibers where optical resources are occupied by this lightpath. This is typically the set of 
     * lightpath traversed fibers. In filterless technologies, this may also include other fibers not intentionally traversed, 
     * but where the spectrum is also occupied
     * @param slotIds the optical slot ids
     * @return see above
     */
    public boolean allocateOccupation (WLightpath lp , Collection<WFiber> wdmLinks , SortedSet<Integer> slotIds)
    {
   	 checkSameWNet(wdmLinks);
   	 checkSameWNet(lp);
    	if (isAlreadyAccounted(lp)) throw new Net2PlanException ("The lightpath has been already accounted for");
    	if (wdmLinks.isEmpty() || slotIds.isEmpty()) return false;
    	boolean clashesWithPreviousAllocations = false;
    	for (WFiber fiber : wdmLinks)
    	{
    		SortedMap<Integer,SortedSet<WLightpath>> thisFiberInfo = this.occupation_f_s_ll.get(fiber);
    		if (thisFiberInfo == null) { thisFiberInfo = new TreeMap<> (); this.occupation_f_s_ll.put(fiber, thisFiberInfo); }
    		for (int slotId : slotIds)
    		{
    			SortedSet<WLightpath> currentCollidingLps = thisFiberInfo.get(slotId);
    			if (currentCollidingLps == null) { currentCollidingLps = new TreeSet<> (); thisFiberInfo.put(slotId, currentCollidingLps); }
    			if (!currentCollidingLps.isEmpty()) clashesWithPreviousAllocations = true;
    			currentCollidingLps.add(lp);
    		}
    	}
    	this.occupation_ll_f_s.put(lp, wdmLinks.stream().collect(Collectors.toMap(e->e, e->new TreeSet<> (slotIds) , (a,b)->b , TreeMap::new)));
    	return clashesWithPreviousAllocations;
    }

    /** Releases all the optical slots occupied for a given lightpath in this manager
     * @param lp the lightpath
     */
    public void releaseOccupation (WLightpath lp)
    {
   	 checkSameWNet(lp);
    	final SortedMap<WFiber,SortedSet<Integer>> occupiedResources = occupation_ll_f_s.get(lp);
    	if (occupiedResources == null) return;
    	occupation_ll_f_s.remove(lp);
    	
    	for (Entry<WFiber,SortedSet<Integer>> resource : occupiedResources.entrySet())
    	{
    		final WFiber fiber = resource.getKey();
    		final SortedSet<Integer> slotIds = resource.getValue();
    		SortedMap<Integer,SortedSet<WLightpath>> thisFiberInfo = this.occupation_f_s_ll.get(fiber);
    		assert fiber != null;
    		for (int slotId : slotIds)
    		{
    			final SortedSet<WLightpath> thisLpAndOthers = thisFiberInfo.get(slotId);
    			assert thisLpAndOthers != null;
    			assert thisLpAndOthers.contains(lp);
    			thisLpAndOthers.remove(lp);
    			if (thisLpAndOthers.isEmpty()) 
    			{
    				thisFiberInfo.remove(slotId);
    				if (thisFiberInfo.isEmpty()) this.occupation_f_s_ll.remove(fiber);
    			}
    		}
    	}
    }


    /** Searches for a first-fit assignment, where in each hop, one fiber is chosen. Given a set of hops (each hop with at least one fiber as an option), the number of contiguous optical slots needed, 
     * and (optionally) an initial optical slot (so optical slots of lower id are not consiedered), this method searches for 
     * the lowest-id contiguous range of slots that are available in all the indicated fibers. Note that if the set of fibers 
     * passes more than once in the same fiber, no assignment is possible, and Optional.empty is returned
     * @param seqAdjacenciesFibers_ab see above
     * @param numContiguousSlotsRequired see above
     * @param unusableSlots see above
     * @return see above. If no idle range is found, Optional.empty is returned. 
     */
    public Optional<Pair<List<Pair<WFiber,WFiber>> , SortedSet<Integer>>> spectrumAssignment_firstFitForAdjacenciesBidi (Collection<Pair<WNode,WNode>> seqAdjacenciesFibers_ab, int numContiguousSlotsRequired , SortedSet<Integer> unusableSlots)
    {
   	 	assert !seqAdjacenciesFibers_ab.isEmpty();
   	 	assert numContiguousSlotsRequired > 0;
        /* If a fiber is traversed more than once, there is no possible assignment */
   	  final Map<Pair<WNode,WNode> , Pair<SortedSet<Integer> , List<Pair<WFiber,WFiber>>>> mapInfoConsideringAllBidi = new HashMap<> ();
   	  final SortedSet<WFiber> allFibersToCheckRepetitions = new TreeSet<> ();
   	  for (Pair<WNode,WNode> nn : seqAdjacenciesFibers_ab)
   	  {
			  final WNode a = nn.getFirst();
			  final WNode b = nn.getSecond();
			  final SortedSet<WFiber> fibersAb = wNet.getNodePairFibers(a, b);
			  final SortedSet<WFiber> fibersBa = wNet.getNodePairFibers(b , a);
			  if (fibersAb.stream().anyMatch(f->!f.isBidirectional())) throw new Net2PlanException ("All fibers must be bidirectional");
			  if (fibersBa.stream().anyMatch(f->!f.isBidirectional())) throw new Net2PlanException ("All fibers must be bidirectional");
			  final List<Pair<WFiber,WFiber>> abBa = new ArrayList<> ();
			  final SortedSet<Integer> idleOpticalSlotRangesInitialSlots = new TreeSet<> ();
			  for (WFiber ab : fibersAb)
			  {
				  if (allFibersToCheckRepetitions.contains(ab)) throw new Net2PlanException ("A fiber appears more than once in an option");
				  if (allFibersToCheckRepetitions.contains(ab.getBidirectionalPair())) throw new Net2PlanException ("A fiber appears more than once in an option");
				  allFibersToCheckRepetitions.add(ab);
				  allFibersToCheckRepetitions.add(ab.getBidirectionalPair());
				  abBa.add(Pair.of(ab, ab.getBidirectionalPair()));
		   		  SortedSet<Integer> optionsAb = getIdleOpticalSlotRangesInitialSlots(ab, numContiguousSlotsRequired);
		   		  SortedSet<Integer> optionsBa = getIdleOpticalSlotRangesInitialSlots(ab.getBidirectionalPair(), numContiguousSlotsRequired);
		   		  optionsAb.removeAll(unusableSlots);
		   		  optionsBa.removeAll(unusableSlots);
		   		  idleOpticalSlotRangesInitialSlots.addAll(Sets.intersection(optionsAb, optionsBa));
			  }
			  mapInfoConsideringAllBidi.put(nn, Pair.of(idleOpticalSlotRangesInitialSlots, abBa));
   	  }
//   	  final SortedMap<WFiber , SortedSet<Integer>> assignmentOptions = new TreeMap<> ();
//   	  for (WFiber e : allFiberOptions)
//   	  {
//   		  SortedSet<Integer> options = getIdleOpticalSlotRangesInitialSlots(e, numContiguousSlotsRequired);
//           if (minimumInitialSlotId.isPresent())
//         	  options = options.tailSet(minimumInitialSlotId.get());
//   		  assignmentOptions.put(e, options);
//   	  }
   	  SortedSet<Integer> validSlotIdsToReturn = null;
   	  for (Pair<WNode,WNode> nn : seqAdjacenciesFibers_ab)
   	  {
           final SortedSet<Integer> validSlotIdsThisHop = mapInfoConsideringAllBidi.get(nn).getFirst();
           if (validSlotIdsToReturn == null) validSlotIdsToReturn = validSlotIdsThisHop; else validSlotIdsToReturn.retainAll(validSlotIdsThisHop);
   	  }
   	  if (validSlotIdsToReturn.isEmpty()) return Optional.empty();
   	  final int firstSlot = validSlotIdsToReturn.first();
   	  final SortedSet<Integer> res_rangetoReturn = new TreeSet<> (); for (int i = 0; i < numContiguousSlotsRequired ; i ++) res_rangetoReturn.add(firstSlot + i);
   	  final List<Pair<WFiber,WFiber>> res_fibersUsed = new ArrayList<> (); 
   	  for (Pair<WNode,WNode> hop_ab :  seqAdjacenciesFibers_ab)
   	  {
   		  final List<Pair<WFiber,WFiber>> fibersThisHop = mapInfoConsideringAllBidi.get(hop_ab).getSecond();
   		  for (Pair<WFiber,WFiber> bidiPair : fibersThisHop)
   		  {
   			  final WFiber ab = bidiPair.getFirst();
   			  final WFiber ba = bidiPair.getSecond();
   			  if (this.isOpticalSlotIdsValidAndIdle(ab, res_rangetoReturn) && this.isOpticalSlotIdsValidAndIdle(ba, res_rangetoReturn))
   			  	{ res_fibersUsed.add(bidiPair); break; }
   		  }
   	  }
   	  assert res_fibersUsed.size() == seqAdjacenciesFibers_ab.size();
   	  return Optional.of(Pair.of(res_fibersUsed , res_rangetoReturn));
    }

    
    /** Searches for a first-fit assignment. Given a set of fibers to occupy, the number of contiguous optical slots needed, 
     * and (optionally) an initial optical slot (so optical slots of lower id are not consiedered), this method searches for 
     * the lowest-id contiguous range of slots that are available in all the indicated fibers. Note that if the set of fibers 
     * passes more than once in the same fiber, no assignment is possible, and Optional.empty is returned
     * @param seqFibers see above
     * @param numContiguousSlotsRequired see above
     * @param minimumInitialSlotId see above
     * @return see above. If no idle range is found, Optional.empty is returned. 
     */
    public Optional<SortedSet<Integer>> spectrumAssignment_firstFit(Collection<WFiber> seqFibers, int numContiguousSlotsRequired , Optional<Integer> minimumInitialSlotId)
    {
    	checkSameWNet(seqFibers);
        assert !seqFibers.isEmpty();
        assert numContiguousSlotsRequired > 0;
        
        /* If a fiber is traversed more than once, there is no possible assignment */
        if (!(seqFibers instanceof Set)) if (new HashSet<> (seqFibers).size() != seqFibers.size()) return Optional.empty();
        SortedSet<Integer> intersectionValidSlots = getAvailableSlotIds(seqFibers);
        if (minimumInitialSlotId.isPresent())
            intersectionValidSlots = intersectionValidSlots.tailSet(minimumInitialSlotId.get());
        if (intersectionValidSlots.size() < numContiguousSlotsRequired) return Optional.empty();
        
        final LinkedList<Integer> rangeValid = new LinkedList<> ();
        for (int slotId : intersectionValidSlots)
        {
            if (!rangeValid.isEmpty())
                if (rangeValid.getLast() != slotId - 1)
                    rangeValid.clear();
            rangeValid.add(slotId);
            assert rangeValid.size() <= numContiguousSlotsRequired;
            if (rangeValid.size() == numContiguousSlotsRequired) return Optional.of(new TreeSet<> (rangeValid));
        }
        return Optional.empty();
    }

    /** Searches for a first-fit assignment for the two given paths, so optical slots can be different for each. 
     * Given two sets of fibers to occupy (paths), the number of contiguous optical slots needed in each, 
     * this method searches for the two lowest-id contiguous ranges of slots, so the first range is available in the first path,
     * the second range is available in the second path. Note that if any path contains a fiber more than once, no allocation is 
     * possible. Note that if path1 and path2 have common fibers, the optical slots returned will always be disjoint.
     * @param seqFibers_1 see above
     * @param seqFibers_2 see above
     * @param numContiguousSlotsRequired see above
     * @return see above. If no idle range is found, Optional.empty is returned. 
     */
    public Optional<Pair<SortedSet<Integer>,SortedSet<Integer>>> spectrumAssignment_firstFitTwoRoutes(Collection<WFiber> seqFibers_1, Collection<WFiber> seqFibers_2 , int numContiguousSlotsRequired)
    {
   	 checkSameWNet(seqFibers_1);
   	 checkSameWNet(seqFibers_2);
        /* If a fiber is traversed more than once in any path, there is no possible assignment */
        if (!(seqFibers_1 instanceof Set)) if (new HashSet<> (seqFibers_1).size() != seqFibers_1.size()) return Optional.empty();
        if (!(seqFibers_2 instanceof Set)) if (new HashSet<> (seqFibers_2).size() != seqFibers_2.size()) return Optional.empty();
        final boolean haveLinksInCommon = !Sets.intersection(new HashSet<>(seqFibers_1)  , new HashSet<>(seqFibers_2)).isEmpty();
        if (!haveLinksInCommon)
        {
            final Optional<SortedSet<Integer>> firstRouteInitialSlot = spectrumAssignment_firstFit(seqFibers_1, numContiguousSlotsRequired, Optional.empty());
            if (!firstRouteInitialSlot.isPresent()) return Optional.empty();
            final Optional<SortedSet<Integer>> secondRouteInitialSlot = spectrumAssignment_firstFit(seqFibers_2, numContiguousSlotsRequired, Optional.empty());
            if (!secondRouteInitialSlot.isPresent()) return Optional.empty();
            return Optional.of(Pair.of(firstRouteInitialSlot.get(), secondRouteInitialSlot.get()));
        }

        /* With links in common */
        final SortedSet<Integer> fistPathValidSlots = getAvailableSlotIds(seqFibers_1);
        final SortedSet<Integer> secondPathValidSlots = getAvailableSlotIds(seqFibers_2);
        for(int initialSlot_1 :  fistPathValidSlots)
        {
            if (!isValidOpticalSlotIdRange(fistPathValidSlots, initialSlot_1, numContiguousSlotsRequired)) continue;
            for(int initialSlot_2 :  secondPathValidSlots)
            {
                if (Math.abs(initialSlot_1 - initialSlot_2) < numContiguousSlotsRequired) continue;
                if (!isValidOpticalSlotIdRange(secondPathValidSlots, initialSlot_2, numContiguousSlotsRequired)) continue;
                final SortedSet<Integer> range1 = new TreeSet<> ();
                final SortedSet<Integer> range2 = new TreeSet<> ();
                for (int cont = 0; cont < numContiguousSlotsRequired ; cont ++) { range1.add(initialSlot_1 + cont);  range2.add(initialSlot_2+cont); } 
                return Optional.of(Pair.of(range1, range2));
            }           
        }
        return Optional.empty();
    }

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
        
        for (WFiber e : occupation_f_s_ll.keySet().stream().sorted((e1,e2)->Integer.compare(occupation_f_s_ll.get(e2).size (), occupation_f_s_ll.get(e1).size ())).collect(Collectors.toList()))
        {
            final SortedMap<Integer,SortedSet<WLightpath>> occupThisLink = occupation_f_s_ll.get(e);
            final int numOchSubpaths = occupThisLink.values().stream().flatMap(s->s.stream()).collect(Collectors.toSet()).size();
            final int numOccupSlots = occupThisLink.size();
            final boolean hasClashing = occupThisLink.values().stream().anyMatch(s->s.size() > 1);
            st.append("Link " + e + ". Occup slots: " + numOccupSlots + ", cap: " + e.getNumberOfValidOpticalChannels() + ", num Och subpaths: " + numOchSubpaths + ", clashing: " + hasClashing + RETURN);
        }
        return st.toString();
    }

    /** Returns true if the design is ok respect to spectrum occupation: no optical slot in any fiber is occupied by more than one 
     * lightpath, and no optical slot in a fiber is outside the valid range for that fiber
     * @return see above
     */
    public boolean isSpectrumOccupationOk ()
    {
        for (Entry<WFiber,SortedMap<Integer,SortedSet<WLightpath>>> occup_e : occupation_f_s_ll.entrySet())
        {
            final WFiber e = occup_e.getKey();
            assert e.isBidirectional();
            final SortedMap<Integer,SortedSet<WLightpath>> occup = occup_e.getValue();
            if (!e.getValidOpticalSlotIds().containsAll(occup.keySet())) return false;
            for (Set<WLightpath> rs : occup.values())
                if (rs.size() != 1) return false;
        }       
        return true;
    }

    /** Returns true if the design is ok respect to spectrum occupation for that lightpath: all optical slots occupied are valid and with no clashing with other lightpaths
     * @param lp see above
     * @return see above
     */
    public boolean isSpectrumOccupationOk (WLightpath lp)
    {
   	 for (WFiber e : lp.getSeqFibers())
   	 {
			 if (!e.getValidOpticalSlotIds().containsAll(lp.getOpticalSlotIds())) return false;
			 final SortedMap<Integer,SortedSet<WLightpath>> occup_e = getOccupiedResources(e);
			 for (int s : lp.getOpticalSlotIds())
   		 {
				 final SortedSet<WLightpath> occupThisSlos = occup_e.getOrDefault(s , new TreeSet<> ());
				 assert occupThisSlos.contains(lp);
				 if (occupThisSlos.size() > 1) return false;
   		 }
   	 }
        return true;
    }
    
    private static boolean isValidOpticalSlotIdRange (SortedSet<Integer> validSlots , int initialSlot , int numContiguousSlots)
    {
        for (int cont = 0; cont < numContiguousSlots ; cont ++)
            if (!validSlots.contains(initialSlot + cont)) return false;
        return true;
    }

	/** Returns the optical slots that are usable (valid and idle) in the given fiber
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

	/** Returns the optical slots where there is wavelength clashing, i.e. more than one traversing lightpath is using this slot
	 * @param wdmLink see above
	 * @return  see above
	 */
	public SortedSet<Integer> getClashingOpticalSlotIds (WFiber wdmLink)
	{
		checkSameWNet(wdmLink);
		return getOccupiedResources (wdmLink).entrySet().stream().filter(e->e.getValue().size() > 1).map(e->e.getKey()).collect(Collectors.toCollection(TreeSet::new));
	}

	/** Returns the number optical slots where there is wavelength clashing, i.e. more than one traversing lightpath is using this slot
	 * @param wdmLink see above
	 * @return  see above
	 */
	public int getNumberOfClashingOpticalSlotIds (WFiber wdmLink)
	{
		checkSameWNet(wdmLink);
		return (int) getOccupiedResources (wdmLink).entrySet().stream().filter(e->e.getValue().size() > 1).count();
	}
	
	/** Returns the optical slots that are usable (valid and idle) in the given fiber
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

		
	
	/** Indicates if the optical slots are usable (valid and idle) in the given fiber
	 * @param wdmLink see above
	 * @param slotsIds see above
	 * @return see above
	 */
	public boolean isOpticalSlotIdsValidAndIdle (WFiber wdmLink , SortedSet<Integer> slotsIds)
	{
		checkSameWNet(wdmLink);
		return getIdleOpticalSlotIds(wdmLink).containsAll(slotsIds);
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
    
    /** Given a contigous path, candidate to be assigned to a unicast lightpath, computes
     *
     * 1) the fibers where the signal would propagate (this includes e.g. fibers where signal propagates if traversing filterless nodes),
     * 2) the list of cycles occurring (if any) that would result in laser loops,
     * 3) an indication if the path is multipath-free. A lightpath is not multipath free when any fiber in the
     * legitimate path is receiving the output power of the signal more than once, coming from different paths, and thus destroying the signal.
     *
     *
    * @param links the sequence of fibers to be traversed. Must be a contigous path.
    * @return see above
    */
   public static Triple<SortedSet<WFiber>,List<List<WFiber>>,Boolean> getPropagatingFibersLasingLoopsAndIsMultipathOk (List<WFiber> links)
    {
   	 if (links.isEmpty()) throw new Net2PlanException ("The path is empty");
   	 if (getContinousSequenceOfNodes(links).stream().allMatch(n->n.getOpticalSwitchingArchitecture().isNeverCreatingWastedSpectrum())) return Triple.of(new TreeSet<> (links), new  ArrayList<> (), true);
   	 
   	 final WFiber dummyFiberAdd = WFiber.createDummyFiber(0);
   	 final WFiber dummyFiberDrop = WFiber.createDummyFiber(1);
   	 
   	 /* Construct a graph starting from add fiber. Stop when all fibers have been processed */
   	 final SortedSet<WFiber> fibersPendingToProcess = new TreeSet<> ();
   	 fibersPendingToProcess.add(dummyFiberAdd);
   	 final SortedSet<WFiber> fibersAlreadyProcessed = new TreeSet<> ();
   	 final DefaultDirectedGraph<WFiber , Object> propagationGraph = new DefaultDirectedGraph<> (Object.class);
		 propagationGraph.addVertex(dummyFiberAdd);
   	 while (!fibersPendingToProcess.isEmpty())
   	 {
   		 final WFiber fiberToProcess = fibersPendingToProcess.first();
   		 if (fibersAlreadyProcessed.contains(fiberToProcess)) { fibersPendingToProcess.remove(fiberToProcess); continue; }
   		 assert propagationGraph.containsVertex(fiberToProcess);
   		 /* process the fiber */
   		 if (fiberToProcess.equals(dummyFiberAdd))
   		 {
   			 /* Add lightpath dummy fiber */
   			 final WNode addNode = links.get(0).getA();
      		 for (WFiber propFiber : addNode.getOpticalSwitchingArchitecture().getOutFibersIfAddToOutputFiber(links.get(0)))
      		 {
      			 if (!propagationGraph.containsVertex(propFiber)) propagationGraph.addVertex(propFiber);
      			 propagationGraph.addEdge(dummyFiberAdd, propFiber);
      			 fibersPendingToProcess.add(propFiber);
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
      		 final int indexOfFiberInPath = links.indexOf(fiberToProcess);
   			 final boolean isExpress = indexOfFiberInPath >= 0 && (indexOfFiberInPath < links.size()-1);
   			 final boolean isDrop = indexOfFiberInPath == links.size() - 1;
   			 if (isExpress)
   			 {
      			 final WFiber inFiber = links.get(indexOfFiberInPath); assert inFiber.equals(fiberToProcess);
      			 final WFiber outFiber = links.get(indexOfFiberInPath + 1);
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
   	 final DirectedSimpleCycles<WFiber,Object> cycleDetector = new JohnsonSimpleCycles<> (propagationGraph); 
   	 final List<List<WFiber>> lasingCycles = cycleDetector.findSimpleCycles();
   	 return Triple.of(propagatedNonDummyFibers , lasingCycles, multipathFree);
    }
    
    
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
    private static List<WNode> getContinousSequenceOfNodes (List<WFiber> path) { if (path.isEmpty()) return new ArrayList<> (); final List<WNode> res = new ArrayList<> (path.size() + 1); res.add(path.get(0).getA()); for (WFiber e : path) { if (!e.getA ().equals(res.get(res.size()-1))) throw new Net2PlanException ("Not contiguous"); res.add(e.getB()); }  return res; }


    
    
    
}
