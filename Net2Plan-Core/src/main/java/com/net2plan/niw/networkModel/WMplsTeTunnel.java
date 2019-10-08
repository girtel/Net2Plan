/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw.networkModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.niw.networkModel.WNetConstants.WTYPE;
import com.net2plan.utils.Pair;

/** Instances of this class are service chains, realizing service chain requests. A service chain should start in one of the origin nodes of the service chain, and end in 
 * one of the destination nodes of the service chain. The injection traffic of the service chain is the traffic produced by its origin node. Note that when the service chain 
 * traffic traverses a VNF, its volume will increase or decrease according to the expansion factor defined for that VNF.
 * 
 *  
 *
 */
public class WMplsTeTunnel extends WAbstractNetworkElement
{
	WMplsTeTunnel(Route r) 
	{ 
		super (r, Optional.empty()); 
	}

	@Override
	public Route getNe() { return (Route) associatedNpElement; }

	/** Returns the IP unicast demand that this IP connection belongs to
	 * @return see above
	 */
	public WIpUnicastDemand getIpUnicastDemand () { return new WIpUnicastDemand(getNe().getDemand()); }
	
	/** Returns the sequence of traversed IP links, filtering out any VNF instance traversed
	 * @return see above
	 */
	public List<WIpLink> getSequenceOfTraversedIpLinks () 
	{
		return getNe().getSeqLinks().stream().map(ee->new WIpLink (ee)).filter(e->!e.isVirtualLink()).collect(Collectors.toCollection(ArrayList::new));
	}
	
	/** Returns the sequence of traversed IP links and VNF instances traversed
	 * @return see above
	 */
	public List<WNode> getSequenceOfTraversedIpNodes ()
	{
		final List<WNode> res = new ArrayList<> ();
		res.add(this.getA());
		for (WIpLink e : getSequenceOfTraversedIpLinks())
			res.add(e.getB());
		return res;
	}

	/** Returns the traffic that this connection is carrying when the path has no failures
	 * @return see above
	 */
	public double getCarriedTrafficInNoFailureStateGbps () { return getNe().getCarriedTrafficInNoFailureState(); }

	/** Sets the traffic in Gbps that this connection is carrying when the path has no failures
	 * @param trafficGbps see above
	 */
	public void setCarriedTrafficInNoFailureStateGbps (double trafficGbps) { getNe().setCarriedTraffic(trafficGbps, trafficGbps); }

	/** Returns the IP traffic in Gbps that this connection is currently carrying 
	 * @return see above
	 */
	public double getCurrentCarriedTrafficGbps () { return getNe().getCarriedTraffic(); }

	/** Reconfigures the path of the IP connecton: the sequence of IP links to traverse 
	 * @param seqIpLinks see above
	 */
	public void setPath (List<WIpLink> seqIpLinks) 
	{
		if (seqIpLinks.stream().anyMatch(e->e.isBundleMember())) throw new Net2PlanException ("Cannot directly route traffic in a LAG bundle member");
		final double trafGbs = getCarriedTrafficInNoFaillureStateGbps(); 
		getNe().setPath(trafGbs, seqIpLinks.stream().map(e->e.getNe()).collect(Collectors.toList()) , Collections.nCopies(seqIpLinks.size(), trafGbs)); 
	}

	/** The origin node of this IP connection
	 * @return see above
	 */
	public WNode getA () 
	{
		return new WNode (getNe().getIngressNode());
	}
	/** The destination node of this IP connection
	 * @return see above
	 */
	public WNode getB () 
	{
		return new WNode (getNe().getEgressNode());
	}

	@Override
	public String toString () { return "MPLS-TE tunnel(" + this.getCarriedTrafficInNoFaillureStateGbps() + "G) " + getA().getName() + "->" + getB().getName(); }

	/** Indicates if this serivce chain is up: not traversing failied links or nodes, and not traversing links with zero capacity
	 * @return
	 */
	public boolean isUp () 
	{
		return getA().isUp() && 
				getB().isUp() && 
				getSequenceOfTraversedIpLinks().stream().allMatch(e->e.isUp() && e.getCurrentCapacityGbps() > Configuration.precisionFactor);
	}

	/** Opposite to isUp
	 * @return see above
	 */
	public boolean isDown () { return !isUp(); }
	
	/** Removes this service chain, realeasing the resources
	 */
	public void remove () { getNe().remove(); }

	void updateNetPlanObjectAndPropagateUpwards ()
	{
		if (this.isUp())
			getNe().setCarriedTraffic(getNe().getCarriedTrafficInNoFailureState(), getNe().getSeqOccupiedCapacitiesIfNotFailing ());
		else
			getNe().setCarriedTraffic(0.0, Collections.nCopies(getNe().getSeqOccupiedCapacitiesIfNotFailing ().size(), 0.0));
	}

	@Override
	void checkConsistency()
	{
		if (this.wasRemoved()) return;
		assert getA().getOutgoingServiceChains().contains(this);
		assert getB().getIncomingServiceChains().contains(this);
		if (isDown()) assert getCurrentCarriedTrafficGbps() == 0;
		assert getSequenceOfTraversedIpLinks().stream().allMatch(e->e.getTraversingServiceChains().contains(this));
		assert getSequenceOfTraversedVnfInstances().stream().allMatch(v->v.getTraversingServiceChains().contains(this));
		assert getCurrentExpansionFactorApplied().size() == getSequenceOfTraversedVnfInstances().size();
	}
	
	@Override
	public WTYPE getWType() { return WTYPE.WMplsTeTunnel; }

}
