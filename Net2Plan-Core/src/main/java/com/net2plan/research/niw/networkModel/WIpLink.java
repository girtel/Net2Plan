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

import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;

/** This class represents an unidirectional IP link. IP links can be realized by optical lightpaths or not. If yes, the IP link is said to be 
 * "coupled" to the lightpath, and then line rate of the IP link is that of the lightpath, and the IP link will be down whenever the 
 * lighptath is down
 *
 */
public class WIpLink extends WAbstractNetworkElement
{
	private static final String ATTNAMECOMMONPREFIX = "IpLink_";
	private static final String ATTNAMESUFFIX_NOMINALCAPACITYGBPS = "nominalCapacityGbps";

	final private Link npLink;

	WIpLink (Link e) { super (e); this.npLink = e; }	

	static WIpLink createFromAdd (Link e , double nominalCapacityGbps) 
	{ 
		final WIpLink res = new WIpLink(e);
		e.setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_NOMINALCAPACITYGBPS, nominalCapacityGbps);
		return res; 
	}	

	public Link getNe () { return (Link) e; }

	/** Returns the IP link orgin node
	 * @return  see above
	 */
	public WNode getA() { return new WNode (npLink.getOriginNode()); }
	/** Returns the IP link destination node
	 * @return  see above
	 */
	public WNode getB() { return new WNode (npLink.getDestinationNode()); }
	/** Returns true if the IP link is part of a pair of IP bidirectional links
	 * @return see above
	 */
	public boolean isBidirectional () { return npLink.isBidirectional(); }
	
	/** Returns true is the IP links is virtual. This means that it starts or ends in a virtual node. Regular users 
	 * do not need to use such links.
	 * @return see above
	 */
	public boolean isVirtualLink () { return getA().isVirtualNode() || getB().isVirtualNode(); }
	
	/** If the IP link is part of a pair of IP bidirectional links, returns the opposite link pair. If not returns null.
	 * @return  see above
	 */
	public WIpLink getBidirectionalPair () { if (!this.isBidirectional()) throw new Net2PlanException ("Not a bidirectional link"); return new WIpLink (npLink.getBidirectionalPair()); }

	/** Returns the user-defined link length in km, which is assumed to be the valid one when the IP link is not coupled to a lightpath
	 * @return  see above
	 */ 
	public double getLengthIfNotCoupledInKm () { return npLink.getLengthInKm(); } 

	/** sets the user-defined link length in km, which is assumed to be the valid one when the IP link is not coupled to a lightpath
	 * @param lengthInKm  see above
	 */
	public void setLengthIfNotCoupledInKm (double lengthInKm) { npLink.setLengthInKm(lengthInKm); } 
	
	/** Returns the link nominal capacity in Gbps. For IP links this is the nominal line rate of the underlying lightpath. 
	 * For non-coupled IP links, this is a value user defined.
	 * @return  see above
	 */
	public double getNominalCapacityGbps () 
	{ 
		if (this.isCoupledtoLpRequest()) return getCoupledLpRequest().getLineRateGbps();
		
		final Double res = npLink.getAttributeAsDouble(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_NOMINALCAPACITYGBPS, null); 
		if (res == null) throw new RuntimeException (); return res; 
	} 
	
	/** Returns the current IP link capacity. In general, the nominal capacity if the link is up, or zero if the link is down
	 * @return see above
	 */
	public double getCurrentCapacityGbps () { return npLink.getCapacity(); } 
	
	/** Returns the current IP traffic carried by the IP link in Gbps.
	 * @return see above
	 */
	public double getCarriedTrafficGbps () { return npLink.getOccupiedCapacity(); } // not the carried traffic, since service chains can compress/decompress traffic

	/** Couples this IP link to the provided lightpath request. The lightpath line rate and the IP link nominal rate must be the same.
	 * If the link is already coupled, an exception is thrown
	 * @param lpReq see above
	 */
	public void coupleToLightpathRequest (WLightpathRequest lpReq) 
	{ 
		if (this.isCoupledtoLpRequest()) throw new Net2PlanException ("The IP link is already coupled. Please decouple it first");
		if (this.getNominalCapacityGbps() != lpReq.getLineRateGbps()) throw new Net2PlanException ("Cannot couple an IP link with a different line rate of the lightpath request");
		npLink.coupleToLowerLayerDemand(lpReq.getNe()); 
	}
	
	/** Decouples this IP link, if it is coupled to a lightpath
	 * 
	 */
	public void decouple () { if (!npLink.isCoupled()) return; npLink.getCoupledDemand().decouple(); }
	
	/** Indicates if this IP link is coupled to a lighptath 
	 * @return see above
	 */
	public boolean isCoupledtoLpRequest () { return npLink.isCoupled(); } 
	
	/** Returns the lightpath that this IP link is coupled to, or raises an exception if this IP link is not coupled
	 * @return see above
	 */
	public WLightpathRequest getCoupledLpRequest () { if (!isCoupledtoLpRequest()) throw new Net2PlanException ("Not coupled"); return new WLightpathRequest(npLink.getCoupledDemand()); }

	/** Returns the length of this link. If the link is NOT coupled, this is the user-defined length. If it is coupled, 
	 * the length is the physical length of fibers traversed by the lightpath carrying this link. If the lightpath is 1+1 protected, 
	 * the longest path length is returned. 
	 * @return see above
	 */
	public double getWorstCaseLengthInKm ()
	{
		if (this.isCoupledtoLpRequest()) return getCoupledLpRequest().getWorstCaseLengthInKm();
		return getLengthIfNotCoupledInKm();
	}

	/** Returns the propagation delay in ms of this link. If the link is NOT coupled, this is the user-defined length divided by 
	 * the user defined propagatio speed. If it is coupled, the length is computed from the physical propagation delay of the fibers 
	 * traversed by the lightpath carrying this link. If the lightpath is 1+1 protected, the longest path propagation delay is returned. 
	 * @return see above
	 */
	public double getWorstCasePropagationDelayInMs ()
	{
		if (this.isCoupledtoLpRequest()) return getCoupledLpRequest().getWorstCasePropagationDelayMs();
		return npLink.getPropagationDelayInMs();
	}

	public String toString () { return "IpLink(" + getNominalCapacityGbps() + "G) " + getA().getName() + "->" + getB().getName(); }
	
}
