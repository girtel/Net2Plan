/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw.networkModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.niw.networkModel.WNetConstants.WTYPE;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;

/**
 * <p>
 * Instances of this class are unicast IP demands, realizable via hop-by-hop or via source routing. No VNFs traversed.
 * </p>
 * <p>
 * The service chain request is also characterized by the total amount of traffic offered by the initial node.
 * </p>
 * <p>
 * Also, the user can specify the maximum acceptable latency limit to the flow end node.
 * </p>
 * <p>
 * Service chains can be tagged to be upstream or downstream (user-defined tag).
 * </p>
 * <p>
 * The offered traffic of a service chain is the current amount of traffic offered (maybe carried or not). It is
 * possible to associate to a service chain request, information of the different offered traffics that would be
 * produced at different time slots.
 * </p>
 */
public class WIpUnicastDemand extends WAbstractIpUnicastOrAnycastDemand
{
	public WIpUnicastDemand(Demand sc)
	{
		super(sc);
	}

   /** Returns a map with an entry for each traversed link, and associated to it the demand's traffic carried in that link. 
    * If selected by the user, the carried traffic is given as a fraction respect to the demand offered traffic
	 * @return see above
	 */
	public SortedMap<WIpLink , Double> getTraversedIpLinksAndCarriedTraffic (final boolean normalizedToOfferedTraffic) 
	{
		final SortedMap<WIpLink , Double> res = new TreeMap<> ();
		for (Entry<Link,Double> entry : getNe ().getTraversedLinksAndCarriedTraffic(normalizedToOfferedTraffic).entrySet())
			res.put(new WIpLink(entry.getKey()), entry.getValue());
		return res;
	}
	
	/**
	 * Indicates if this unidirectional service chain request has associated an opposite service chain request-. If this
	 * flow is upstream, the opposite is downstream. An initial valid node for this stream is an ending valid node of the
	 * opposite and viceversa. VNFs traversed do not need to be the same.
	 * @return see above
	 */
	public boolean isBidirectional()
	{
		return getNe().isBidirectional();
	}

	/**
	 * Returns the maximum acceptable latency from the orign to the destination node is miliseconds
	 * @return see above
	 */
	public double getMaximumAcceptableE2EWorstCaseLatencyInMs()
	{
		return getNe ().getMaximumAcceptableE2EWorstCaseLatencyInMs();
	}

	/**
	 * Sets the maximum acceptable latency from the orign to the destination node is miliseconds. A non-positive value, translates to an infinite (Double.MAX_VALUE) limit 
	 * @param maxLatencyList_ms see above
	 */
	public void setMaximumAcceptableE2EWorstCaseLatencyInMs(double maxLatencyList_ms)
	{
		getNe ().setMaximumAcceptableE2EWorstCaseLatencyInMs(maxLatencyList_ms);
	}

	/** Indicates if this demand is tagged to be routed via hop-by-hop IP routing (e.g. OSPF)
	 * @return see above
	 */
	public boolean isIpHopByHopRouted () 
	{ 
		return getNe ().getRoutingType() == RoutingType.HOP_BY_HOP_ROUTING; 
	}
	
	/** Indicates if this demand is tagged to be routed via source-routing (e.g. via an MPLS-TE circuit)
	 * @return see above
	 */
	public boolean isIpSourceRouted () 
	{ 
		return getNe ().getRoutingType() == RoutingType.SOURCE_ROUTING; 
	}
	
	/** Sets this IP demand to be routed via hop-by-hop IP routing
	 */
	public void setAsHopByHopRouted () 
	{
		if (this.isIpHopByHopRouted()) return;
		this.getNe().setRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
	}

	/** Sets this IP demand to be routed via source-routing (e.g. MPLS-TE)
	 */
	public void setAsSourceRouted () 
	{
		if (this.isIpSourceRouted()) return;
		this.getNe().setRoutingType(RoutingType.SOURCE_ROUTING);
	}
	
	/**
	 * Returns the origin node of this IP demand
	 * @return see above
	 */
	public WNode getOriginNode ()
	{
		return new WNode (getNe().getIngressNode());
	}

	/**
	 * Returns the destination node of this IP demand
	 * @return see above
	 */
	public WNode getDestinationNode ()
	{
		return new WNode (getNe().getEgressNode());
	}

	
	/** Returns the IP connections realizing this demand. If the demand is hop-by-hop routed, an empty returns an empty set
	 * @return see above
	 */
	public SortedSet<WMplsTeTunnel> getIpConnections ()
	{
		if (this.isIpHopByHopRouted()) return new TreeSet<> ();
		return getNe().getRoutes().stream().
				filter(d->{ WTYPE t = getNet().getWType(d).orElse(null); return t == null? false : t.isMplsTeTunnel(); }).
				map(ee -> new WMplsTeTunnel(ee)).collect(Collectors.toCollection(TreeSet::new));
	}

	@Override
	public String toString()
	{
		return "WIpUnicastDemand (" + this.getCurrentOfferedTrafficInGbps() + "G) " + getOriginNode() + "->" + getDestinationNode();
	}

	@Override
	void checkConsistency()
	{
		if (this.wasRemoved()) return;
		if (this.isBidirectional()) assert this.getBidirectionalPair().get().getBidirectionalPair().get().equals(this);
		if (this.isBidirectional()) assert this.getBidirectionalPair().get().isDownstream() == this.isUpstream();
		assert !this.getNe().hasTag(WNetConstants.TAGDEMANDIP_INDICATIONISBUNDLE);
		assert this.getNe().getLayer().equals(getNet().getIpLayer().getNe());
	}

	/**
	 * Makes that this IP demand tagged as the opposite one to a given IP demand, and viceversa. The two IP demand must
	 * have opposite end nodes, one must be downstream and the other upstream. Any other opposite relation to other IP demand is released.
	 * @return
	 */
	@Override
	public void setBidirectionalPair(WAbstractIpUnicastOrAnycastDemand d) 
	{
		if (d == null) throw new Net2PlanException("Please specify a not null IP demand");
		if (!(d instanceof WIpUnicastDemand)) throw new Net2PlanException("Please specify a not null IP demand");
		final WIpUnicastDemand dd = (WIpUnicastDemand) d;
		if (!this.getOriginNode().equals(dd.getDestinationNode ())) throw new Net2PlanException("The origin of this IP demand must be the end node of the opposite, and viceversa");
		if (!dd.getOriginNode().equals(this.getDestinationNode())) throw new Net2PlanException("The origin of this IP demand must be the end node of the opposite, and viceversa");
		if (this.isDownstream() == dd.isDownstream()) throw new Net2PlanException("One flow must be tagged as upstream, the other as downstream");
		removeBidirectionalPairRelation();
		getNe().setBidirectionalPair(d.getNe());
	}

	/**
	 * If this service chain request has an opposite request associated, removes such association. If not, makes nothing
	 * happens
	 * @return
	 */
	@Override
	public void removeBidirectionalPairRelation() 
	{
		if (!this.isBidirectional()) return;
		getNe().setBidirectionalPair(null);
	}

	/**
	 * Returns the opposite service chain request to this, if any
	 * @return
	 */
	@Override
	public Optional<WIpUnicastDemand> getBidirectionalPair ()
	{
		if (!this.isBidirectional()) return Optional.empty();
		return Optional.of (new WIpUnicastDemand (getNe().getBidirectionalPair()));
	}

	@Override
	public void remove() 
	{
		getNe ().remove();
	}

	@Override
	public WTYPE getWType() { return WTYPE.WIpUnicastDemand; }

}
