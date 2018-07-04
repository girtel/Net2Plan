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

package com.net2plan.research.niw.networkModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.Sets;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.utils.Pair;

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
	final private SortedMap<WFiber,SortedMap<Integer,SortedSet<WLightpathUnregenerated>>> occupation_f_s_ll = new TreeMap<> ();
	final private SortedMap<WLightpathUnregenerated,SortedMap<WFiber,SortedSet<Integer>>> occupation_ll_f_s = new TreeMap<> ();

	private OpticalSpectrumManager () {}
	
	/** Creates this object, asociated to a given network
	 * @param net the network
	 * @return see above
	 */
	public static OpticalSpectrumManager createFromRegularLps (WNet net)
    {
		final OpticalSpectrumManager osm = new OpticalSpectrumManager();
		for (WLightpathUnregenerated lp : net.getLightpaths())
			osm.allocateOccupation(lp, lp.getSeqFibers(), lp.getOpticalSlotIds());
        return osm;
    }

    /** Returns the set of the optical slots ids that are idle in ALL the fibers provided 
     * @param wdmLinks the set of fibers
     * @return see above
     */
    public SortedSet<Integer> getAvailableSlotIds (Collection<WFiber> wdmLinks)
    {
        if (wdmLinks.isEmpty()) throw new Net2PlanException ("No WDM links");
        final SortedSet<Integer> validSlotIds = this.getIdleOpticalSlotIds(wdmLinks.iterator().next());
        final Iterator<WFiber> itLink = wdmLinks.iterator();
        itLink.next();
        while (itLink.hasNext())
            validSlotIds.retainAll(this.getIdleOpticalSlotIds(itLink.next()));
        return validSlotIds;
    }
    
    /** Given a lightpath request, returns the optical slots occupied in the traversed fibers
     * @param lp the lightpath request
     * @return see above
     */
    public SortedMap<WFiber,SortedSet<Integer>> getOccupiedResources (WLightpathRequest lp)
    {
    	return this.occupation_ll_f_s.getOrDefault(lp, new TreeMap<> ());
    }

    /** Given a fiber, returns a map with the occupied optical slot ids, mapped to the set of lightpaths that occupy it. 
     * Note that if more than one lightpath occupies a given slot, means that spectrum clashing occurs in that slot   
     * @param fiber the input fiber
     * @return see above
     */
    public SortedMap<Integer,SortedSet<WLightpathUnregenerated>> getOccupiedResources (WFiber fiber)
    {
    	return this.occupation_f_s_ll.getOrDefault(fiber, new TreeMap<> ());
    }

    /** Given a fiber, returns the set of optical slots occupied by at least one traversing lightpath
     * @param fiber see above
     * @return see above
     */
    public SortedSet<Integer> getOccupiedOpticalSlotIds (WFiber fiber)
    {
		SortedMap<Integer,SortedSet<WLightpathUnregenerated>> occupiedSlotsPerLightpath = this.occupation_f_s_ll.get(fiber);
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
    public boolean isAlreadyAccounted (WLightpathUnregenerated lp) { return this.occupation_ll_f_s.get(lp) != null; }

    /** Accounts for the occupation of a lightpath, updating the information in the spectrum manager
     * @param lp the lightpath
     * @param wdmLinks the set of fibers where optical resources are occupied by this lightpath. This is typically the set of 
     * lightpath traversed fibers. In filterless technologies, this may also include other fibers not intentionally traversed, 
     * but where the spectrum is also occupied
     * @param slotIds the optical slot ids
     * @return see above
     */
    public boolean allocateOccupation (WLightpathUnregenerated lp , Collection<WFiber> wdmLinks , SortedSet<Integer> slotIds)
    {
    	if (isAlreadyAccounted(lp)) throw new Net2PlanException ("The lightpath has been already accounted for");
    	if (wdmLinks.isEmpty() || slotIds.isEmpty()) return false;
    	boolean clashesWithPreviousAllocations = false;
    	for (WFiber fiber : wdmLinks)
    	{
    		SortedMap<Integer,SortedSet<WLightpathUnregenerated>> thisFiberInfo = this.occupation_f_s_ll.get(fiber);
    		if (thisFiberInfo == null) { thisFiberInfo = new TreeMap<> (); this.occupation_f_s_ll.put(fiber, thisFiberInfo); }
    		for (int slotId : slotIds)
    		{
    			SortedSet<WLightpathUnregenerated> currentCollidingLps = thisFiberInfo.get(slotId);
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
    public void releaseOccupation (WLightpathUnregenerated lp)
    {
    	final SortedMap<WFiber,SortedSet<Integer>> occupiedResources = occupation_ll_f_s.get(lp);
    	if (occupiedResources == null) return;
    	occupation_ll_f_s.remove(lp);
    	
    	for (Entry<WFiber,SortedSet<Integer>> resource : occupiedResources.entrySet())
    	{
    		final WFiber fiber = resource.getKey();
    		final SortedSet<Integer> slotIds = resource.getValue();
    		SortedMap<Integer,SortedSet<WLightpathUnregenerated>> thisFiberInfo = this.occupation_f_s_ll.get(fiber);
    		assert fiber != null;
    		for (int slotId : slotIds)
    		{
    			final SortedSet<WLightpathUnregenerated> thisLpAndOthers = thisFiberInfo.get(slotId);
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
    public List<List<WFiber>> getRegenerationPoints (List<WFiber> seqFibers, double maxUnregeneratedDistanceInKm)
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
            final SortedMap<Integer,SortedSet<WLightpathUnregenerated>> occupThisLink = occupation_f_s_ll.get(e);
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
        for (Entry<WFiber,SortedMap<Integer,SortedSet<WLightpathUnregenerated>>> occup_e : occupation_f_s_ll.entrySet())
        {
            final WFiber e = occup_e.getKey();
            assert e.isBidirectional();
            final SortedMap<Integer,SortedSet<WLightpathUnregenerated>> occup = occup_e.getValue();
            if (!e.getValidOpticalSlotIds().containsAll(occup.keySet())) return false;
            for (Set<WLightpathUnregenerated> rs : occup.values())
                if (rs.size() != 1) return false;
        }       
        return true;
    }

    
    /** Checks if the optical slot occupation --
     * 
     */
    public void checkNetworkSlotOccupation ()
    {
        for (Entry<WFiber,SortedMap<Integer,SortedSet<WLightpathUnregenerated>>> occup_e : occupation_f_s_ll.entrySet())
        {
            final WFiber e = occup_e.getKey();
            assert e.isBidirectional();
            final SortedMap<Integer,SortedSet<WLightpathUnregenerated>> occup = occup_e.getValue();
            if (!e.getValidOpticalSlotIds().containsAll(occup.keySet())) throw new Net2PlanException ("The optical slots occupied at link " + e + " are outside the valid range");
            for (Set<WLightpathUnregenerated> rs : occup.values())
                if (rs.size() != 1) throw new Net2PlanException ("The optical slots occupation is not correct");
        }       
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
		final SortedSet<Integer> res = wdmLink.getValidOpticalSlotIds();
		res.removeAll(getOccupiedOpticalSlotIds(wdmLink));
		return res;
	}
	
	/** Indicates if the optical slots are usable (valid and idle) in the given fiber
	 * @param wdmLink see above
	 * @param slotsIds see above
	 * @return see above
	 */
	public boolean isOpticalSlotIdsValidAndIdle (WFiber wdmLink , SortedSet<Integer> slotsIds)
	{
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

	
	
}
