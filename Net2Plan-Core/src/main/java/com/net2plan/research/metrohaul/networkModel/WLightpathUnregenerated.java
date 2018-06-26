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

package com.net2plan.research.metrohaul.networkModel;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Route;

/** This class represents a unidirectional lightpath of a given line rate, being a main path or backup path of a lightpath request.
 * The lighptath is unregenerated, and has no wavelength conversion: the same set of optical slot are occupied in all the 
 * traversed fibers
 *
 */
public class WLightpathUnregenerated extends WAbstractNetworkElement
{
	private static final String ATTNAMECOMMONPREFIX = "UnregLp_";
	private static final String ATTNAMESUFFIX_OCCUPIEDSLOTIDS = "occupiedSlotIds";
	private static final String ATTNAMESUFFIX_MODULATIONID = "modulationId";

	private final Route r;

	WLightpathUnregenerated (Route r) { super (r); this.r = r; }

	public Route getNe () { return (Route) e; }
	
	
	/** The origin node of the lighptath, that must the origin node of the associated lightpath request
	 * @return see above
	 */
	public WNode getA () { return new WNode (r.getIngressNode()); }
	/** The destination node of the lighptath, that must the destination node of the associated lightpath request
	 * @return see above
	 */
	public WNode getB () { return new WNode (r.getEgressNode()); }
	/** Returns the associated lightpath request
	 * @return see above
	 */
	public WLightpathRequest getLightpathRequest () { return new WLightpathRequest(r.getDemand()); }

	/** Returns the user-defined identifier of the modulation associated to the transponder realizing this lightpath
	 * @return see above
	 */
	public String getModulationId () { return r.getAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_MODULATIONID, ""); }
	/** Sets the user-defined identifier of the modulation associated to the transponder realizing this lightpath
	 * @param modulationId see above
	 */
	public void setModulationId (String modulationId) { r.setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_MODULATIONID, modulationId); }
	/** Indicates if this is a backup lightpath, of a main path in the same lightpath request
	 * @return see above
	 */
	public boolean isBackupLightpath () { return r.isBackupRoute(); }
	/** Indicates if this is a main lightpath that has a backup lightpath in the same lightpath request
	 * @return see above
	 */
	public boolean isMainLightpathWithBackupRoutes () { return r.hasBackupRoutes(); }

	/** Adds a backup lightpath to this lightpath
	 * @param backup see above
	 */
	public void addBackupLightpath (WLightpathUnregenerated backup) { r.addBackupRoute(backup.getNe ()); }
	
	/** Removes a backup lightpath 
	 * @param backup see above
	 */
	public void removeBackupLightpath (WLightpathUnregenerated backup) { r.removeBackupRoute(backup.getNe ()); }

	/** Sets this lightpath as a backup lightpath of other lightpath in this request 
	 * @param mainLp see above
	 */
	public void setAsBackupLightpath (WLightpathUnregenerated mainLp) { mainLp.addBackupLightpath(this); }
	
	/** Returns the sequence of fibers traversed by this lp
	 * @return see above
	 */
	public List<WFiber> getSeqFibers () { return r.getSeqLinks().stream().map(e->new WFiber(e)).collect(Collectors.toList()); }
	/** Changes the sequence of fibers traversed by this lightpath
	 * @param newSeqFibers see above
	 */
	public void setSeqFibers (List<WFiber> newSeqFibers) { r.setSeqLinks(newSeqFibers.stream().map(ee->ee.getNe()).collect(Collectors.toList())); }
	/** Returns the set of optical slots occupied in all the traversed fibers of this lightpath
	 * @return see above
	 */
	public SortedSet<Integer> getOpticalSlotIds () { SortedSet<Integer> res = getAttributeAsSortedSetIntegerOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_OCCUPIEDSLOTIDS , null);if (res == null) throw new RuntimeException (); return res;  }
	/** Changes the set of optical slots occupied in all the traversed fibers of this lightpath
	 * @param slotsIds see above
	 */
	public void setOpticalSlotIds (SortedSet<Integer> slotsIds) { r.setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_OCCUPIEDSLOTIDS, new ArrayList<> (slotsIds)); }
	/** Returns the length in km of this lighptath, as the sum of the lengths of the traversed fibers
	 * @return see above
	 */
	public double getLengthInKm () { return r.getLengthInKm(); }
	/** Returns the propagation delay in ms of this lighptath, as the sum of the propagation delays of the traversed fibers
	 * @return see above
	 */
	public double getPropagationDelayInMs () { return r.getPropagationDelayInMiliseconds(); }
	
	/** Indicates if this lightpath is up (does not traverse any failed fiber/node)
	 * @return see above
	 */
	public boolean isUp () { return !r.isDown(); }
	/** Returns the number of optical slots occupied by the lightpath 
	 * @return see above
	 */
	public double getNumberOccupiedSlotIds () { return getOpticalSlotIds().size(); } 
	
	/** Removes this lightpath
	 */
	public void remove () {  r.remove(); this.getLightpathRequest().internalUpdateOfRoutesCarriedTrafficFromFailureState(); }

	public String toString () { return "LpUnreg(" + getLightpathRequest().getLineRateGbps() + "G) " + getA().getName() + "->" + getB().getName(); }

}
