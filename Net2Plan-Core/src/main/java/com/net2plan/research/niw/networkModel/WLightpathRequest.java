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

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Net2PlanException;

/** This class represents a request to establish a unidirectional lightpath between two nodes, for a given line rate. The 
 * lightpath request can be blocked (no lighptaths assigned), or relalizaed by one or two (1+1) lightpaths.
 *
 */
public class WLightpathRequest extends WAbstractNetworkElement
{
	final private Demand d;
	private static final String ATTNAMECOMMONPREFIX = "LightpathRequest_";
	private static final String ATTNAMESUFFIX_ISTOBE11PROTECTED = "isToBe11Protected";

	
	WLightpathRequest (Demand d) 
	{ 
		super (d); 
		this.d = d; 
		assert d.getRoutes().size() <= 2; 
		if (d.getRoutes().size() == 2) assert d.getRoutesAreBackup().size() == 1; 
		assert !getA().isVirtualNode() && !getB().isVirtualNode();  
	}
	
	public Demand getNe () { return (Demand) e; }

	
	/** The lightpath request origin node
	 * @return see above
	 */
	public WNode getA () { return new WNode (getNe().getIngressNode()); }
	/** The lightpath request destination node
	 * @return see above
	 */
	public WNode getB () { return new WNode (getNe().getEgressNode()); }
	/** Indicates if the lightpath is part of a bidirectinal pair of lighptaths 
	 * @return see above
	 */
	public boolean isBidirectional () { return getNe().isBidirectional(); }
	/** If the lightpath is part of a bidirectional pair, returns the opposite lightpath request, if not returns null 
	 * @return see above
	 */
	public WLightpathRequest getBidirectionalPair () { assert this.isBidirectional(); return new WLightpathRequest(d.getBidirectionalPair()); }

	/** Returns the set of lightpaths realizing this request. Typically one in unprotected ligtpaths, two in 1+1 settings
	 * @return see above
	 */
	public List<WLightpathUnregenerated> getLightpaths () { return getNe().getRoutes().stream().map(r->new WLightpathUnregenerated(r)).collect(Collectors.toList()); }
	/** Returns the line rate of the lighptath request in Gbps
	 * @return see above
	 */
	public double getLineRateGbps () { return d.getOfferedTraffic(); }
	/** Sets the line rate of the lighptath request in Gbps
	 * @param lineRateGbps see above
	 */
	public void setLineRateGbps (double lineRateGbps) { d.setOfferedTraffic(lineRateGbps); }
	/** Indicates if the lighptath request is supported by a 1+1 setting of two lightpaths 
	 * @return see above
	 */
	public boolean is11Protected () { return !d.getRoutesAreBackup().isEmpty(); }
	/** Indicates if the user has requested this lightpath request to be realized by a 1+1 setting
	 * @return see above
	 */
	public boolean isToBe11Protected () { return getAttributeAsBooleanOrDefault(ATTNAMECOMMONPREFIX +ATTNAMESUFFIX_ISTOBE11PROTECTED , WNetConstants.WLPREQUEST_DEFAULT_ISTOBE11PROTECTED); }
	/** Sets if this lightpath request has to be realized by a 1+1 setting or not
	 * @param isToBe11Protected see above
	 */
	public void setIsToBe11Protected (boolean isToBe11Protected) { d.setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ISTOBE11PROTECTED , new Boolean (isToBe11Protected).toString()); }
	/** Returns the length of this lightpath request, as the physical length of fibers traversed by the lightpaths carrying this request. If the lightpath is 1+1 protected, 
	 * the longest path length is returned. If the request has assigned no lighptaths, Double.MAX_VALUE is returned.
	 * @return see above
	 */
	public double getWorstCaseLengthInKm () 
	{
		return getLightpaths().stream().mapToDouble(lp->lp.getLengthInKm()).max().orElse(Double.MAX_VALUE);
	}
	/** Returns the propagation delay in ms of this lightpath request, as the propagation delay of fibers traversed by the lightpaths carrying this request. If the lightpath is 1+1 protected, 
	 * the longest path length is returned. If the request has assigned no lighptaths, Double.MAX_VALUE is returned.
	 * @return see above
	 */
	public double getWorstCasePropagationDelayMs () 
	{
		return getLightpaths().stream().mapToDouble(lp->lp.getPropagationDelayInMs()).max().orElse(Double.MAX_VALUE);
	}
	
	/** Indicates if this lighptath request has lightpaths assigned
	 * @return see above
	 */
	public boolean hasLightpathsAssigned () { return !d.getRoutes().isEmpty(); }
	/** Indicates if the request is blocked, meaning (i) has no lightpaths assigned, or (ii) assigned lightpaths are all down
	 * @return see above
	 */
	public boolean isBlocked () 
	{
		if (!hasLightpathsAssigned()) return true;
		return !getLightpaths().stream().anyMatch(lp->lp.isUp());
	}
	
	/** Return the current lightpath request capacity: its nominal line rate if up, or zero if down
	 * @return see above
	 */
	public double getCurrentCapacityGbps ()
	{
		return isBlocked()? 0.0 : getLineRateGbps();
	}

	/** Removes this lightpath request. This automatically removes any lightpath realizing this request.
	 * 
	 */
	public void remove () { d.remove(); }

	
	/** Adds a lightpath realizing this lightpath request. A demand can be assigned at most two lightpaths, one main and one backup.
	 * @param sequenceFibers the sequence of traversed fibers
	 * @param occupiedSlots the setof occupied slots
	 * @param isBackupRoute indicates if this is a backup route (needs a main lightpath to be already added to the request).
	 * @return see above
	 */
	public WLightpathUnregenerated addLightpathUnregenerated (List<WFiber> sequenceFibers , 
			SortedSet<Integer> occupiedSlots ,
			boolean isBackupRoute)
	{
		getNet().checkInThisWNetCol(sequenceFibers);
		final int numRoutesAlready = this.getLightpaths().size();
		if (numRoutesAlready == 2) throw new Net2PlanException ("Already two lightpaths");
		if (numRoutesAlready == 1 && !this.isToBe11Protected()) throw new Net2PlanException ("Already one lightpath");
		if (numRoutesAlready == 1 && !isBackupRoute) throw new Net2PlanException ("Not a backup lightpath");
		if (numRoutesAlready == 0 && isBackupRoute) throw new Net2PlanException ("Not a main lightpath yet");
		final WLightpathUnregenerated lp12 = new WLightpathUnregenerated(getNetPlan().addRoute(getNe(), 
				this.getLineRateGbps(), 
				occupiedSlots.size(), sequenceFibers.stream().map(ee->ee.getNe()).collect(Collectors.toList()), 
				null));
		lp12.setOpticalSlotIds(occupiedSlots);
		if (isBackupRoute) lp12.setAsBackupLightpath(getLightpaths().get(0));
		this.internalUpdateOfRoutesCarriedTrafficFromFailureState();
		return lp12;
	}
	
	/** Couples this lightpath request to an IP link
	 * @param ipLink see above
	 */
	public void coupleToIpLink (WIpLink ipLink) { getNet().checkInThisWNet(ipLink); ipLink.coupleToLightpathRequest(this); }
	
	/** Decouples this lightpath request to the IP link it was coupled to, if any
	 * 
	 */
	public void decouple () { if (!d.isCoupled()) return; d.decouple(); }
	
	/** Indicates if the lightpath is coupled to an IP link
	 * @return see above
	 */
	public boolean isCoupledToIpLink () { return d.isCoupled(); } 
	
	/** Gets the coupled IP links, or raises an exception if none
	 * @return see above
	 */
	public WIpLink getCoupledIpLink () { if (!isCoupledToIpLink()) throw new Net2PlanException ("Not coupled"); return new WIpLink(d.getCoupledLink()); }

	
	void internalUpdateOfRoutesCarriedTrafficFromFailureState ()
	{
		if (!hasLightpathsAssigned()) return;
		if (!is11Protected())
		{
			final WLightpathUnregenerated lp = getLightpaths().get(0);
			lp.getNe().setCarriedTraffic(lp.isUp()? getLineRateGbps() : 0.0 , null);
		}
		else
		{
			final List<WLightpathUnregenerated> lps = getLightpaths();
			if (lps.get(0).isUp()) 
			{ 
				lps.get(0).getNe().setCarriedTraffic(getLineRateGbps(), null);   
				lps.get(1).getNe().setCarriedTraffic(0.0, null);   
			}
			else
			{
				lps.get(0).getNe().setCarriedTraffic(0.0 , null);   
				lps.get(1).getNe().setCarriedTraffic(lps.get(1).isUp()? getLineRateGbps() : 0.0 , null);   
			}
		}
	}

	public String toString () { return "LpRequest(" + getLineRateGbps() + "G) " + getA().getName() + "->" + getB().getName(); }

}
