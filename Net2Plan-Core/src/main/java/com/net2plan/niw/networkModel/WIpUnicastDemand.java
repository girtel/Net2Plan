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
public class WIpUnicastDemand extends WAbstractNetworkElement
{
	private static final String ATTNAMECOMMONPREFIX = "WIpUnicastDemand_";
	private static final String ATTNAMESUFFIX_TIMESLOTANDINTENSITYINGBPS = "timeSlotAndInitialInjectionIntensityInGbps";
	private static final String ATTNAMESUFFIX_ISUPSTREAM = "isUpstream";

	private final Demand sc; // from anycastIN to anycastOUT

	WIpUnicastDemand(Demand sc)
	{
		super(sc, Optional.empty());
		this.sc = sc;
		assert sc.getLayer().equals (getNet().getIpLayer().getNe());
		assert !sc.hasTag(WNetConstants.TAGDEMANDIP_INDICATIONISBUNDLE);
		assert !(new WNode(sc.getIngressNode()).isVirtualNode());
		assert !(new WNode(sc.getEgressNode()).isVirtualNode());
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
	 * Returns the opposite service chain request to this, if any
	 * @return
	 */
	public Optional<WIpUnicastDemand> getBidirectionalPair ()
	{
		if (!this.isBidirectional()) return Optional.empty();
		return Optional.of (new WIpUnicastDemand (getNe().getBidirectionalPair()));
	}

	/**
	 * Makes that this IP demand tagged as the opposite one to a given IP demand, and viceversa. The two IP demand must
	 * have opposite end nodes, one must be downstream and the other upstream. Any other opposite relation to other IP demand is released.
	 * @return
	 */
	public void setBidirectionalPair(WIpUnicastDemand scr)
	{
		if (scr == null) throw new Net2PlanException("Please specify a not null IP demand");
		if (!this.getOriginNode().equals(scr.getDestinationNode ())) throw new Net2PlanException("The origin of this IP demand must be the end node of the opposite, and viceversa");
		if (!scr.getOriginNode().equals(this.getDestinationNode())) throw new Net2PlanException("The origin of this IP demand must be the end node of the opposite, and viceversa");
		if (this.isDownstream() == scr.isDownstream()) throw new Net2PlanException("One flow must be tagged as upstream, the other as downstream");
		removeOppositeUnicastDemandRelation();
	}

	/**
	 * If this service chain request has an opposite request associated, removes such association. If not, makes nothing
	 * happens
	 * @return
	 */
	public void removeOppositeUnicastDemandRelation()
	{
		if (!this.isBidirectional()) return;
		getNe().setBidirectionalPair(null);
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

	/**
	 * Get the user-defined traffic intensity associated to a time slot with the given name,
	 * @param timeSlotName see above
	 * @return see above
	 */
	public Optional<Double> getTrafficIntensityInfo(String timeSlotName)
	{
		return getTimeSlotNameAndInitialInjectionIntensityInGbpsList().stream().filter(p -> p.getFirst().equals(timeSlotName)).map(p -> p.getSecond()).findFirst();
	}

	/**
	 * Get the user-defined traffic intensity associated to a time slot with the given index,
	 * @param timeSlotIndex see above
	 * @return a pair with name of the time slot and the traffic intensity value
	 */
	public Optional<Pair<String, Double>> getTrafficIntensityInfo(int timeSlotIndex)
	{
		final List<Pair<String, Double>> res = getTimeSlotNameAndInitialInjectionIntensityInGbpsList();
		if (res.size() <= timeSlotIndex) return Optional.empty();
		return Optional.of(res.get(timeSlotIndex));
	}

	/**
	 * Returns the full user-defined traffic intensity information as a list of pairs, where each pair contains the
	 * user-defined name of the time slot, and the traffic intensity associated to that time slot
	 * @return see above
	 */
	public List<Pair<String, Double>> getTimeSlotNameAndInitialInjectionIntensityInGbpsList()
	{
		final List<String> vals = sc.getAttributeAsStringList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_TIMESLOTANDINTENSITYINGBPS, null);
		if (vals == null) return new ArrayList<>();
		try
		{
			final Set<String> previousTimeSlotIds = new HashSet<>();
			final List<Pair<String, Double>> res = new ArrayList<>();
			final Iterator<String> it = vals.iterator();
			while (it.hasNext())
			{
				final String timeSlotName = it.next();
				final Double intensity = Double.parseDouble(it.next());
				if (previousTimeSlotIds.contains(timeSlotName)) continue;
				else previousTimeSlotIds.add(timeSlotName);
				res.add(Pair.of(timeSlotName, intensity));
			}
			return res;
		} catch (Exception e)
		{
			return new ArrayList<>();
		}
	}

	/**
	 * Sets the full user-defined traffic intensity information as a list of pairs, where each pair contains the
	 * user-defined name of the time slot, and the traffic intensity associated to that time slot
	 * @param info see above
	 */
	public void setTimeSlotNameAndInitialInjectionIntensityInGbpsList(List<Pair<String, Double>> info)
	{
		final List<String> vals = new ArrayList<>();
		final Set<String> previousTimeSlotIds = new HashSet<>();
		for (Pair<String, Double> p : info)
		{
			final String timeSlotName = p.getFirst();
			final Double intensity = p.getSecond();
			if (previousTimeSlotIds.contains(timeSlotName)) continue;
			else previousTimeSlotIds.add(timeSlotName);
			vals.add(timeSlotName);
			vals.add(intensity.toString());
		}
		sc.setAttributeAsStringList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_TIMESLOTANDINTENSITYINGBPS, vals);
	}

	/**
	 * Returns the current offered traffic of this service chain request (that may carried partially, totally or none)
	 * @return see above
	 */
	public double getCurrentOfferedTrafficInGbps()
	{
		return sc.getOfferedTraffic();
	}

	/**
	 * Returns the carried traffic of this request
	 * @return see above
	 */
	public double getCurrentCarriedTrafficGbps ()
	{
		return sc.getCarriedTraffic();
	}

	/**
	 * Returns the carried traffic of this request
	 * @return see above
	 */
	public double getCurrentBlockedTraffic()
	{
		final double dif = sc.getOfferedTraffic() - sc.getCarriedTraffic();
		return dif < Configuration.precisionFactor ? 0 : dif;
	}

	/**
	 * Sets the current offered traffic in Gbps for this service chain
	 * @param offeredTrafficInGbps see above
	 */
	public void setCurrentOfferedTrafficInGbps(double offeredTrafficInGbps)
	{
		sc.setOfferedTraffic(offeredTrafficInGbps);
	}

	/**
	 * Indicates if this is a service chain request is upstream (initiated in the user). Returns false if the service chain
	 * is downstream (ended in the user)
	 * @return see above
	 */
	public boolean isUpstream()
	{
		final Boolean res = getAttributeAsBooleanOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ISUPSTREAM, null);
		assert res != null;
		return res;
	}

	/**
	 * The opposite to isUpstream
	 * @return see above
	 */
	public boolean isDownstream()
	{
		return !isUpstream();
	}

	/**
	 * Sets if this is an upstream service chain request (initiated in the user), with true, of false if the service chain
	 * is downstream (ended in the user)
	 * @param isUpstream see above
	 */
	public void setIsUpstream(boolean isUpstream)
	{
		setAttributeAsBoolean(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ISUPSTREAM, isUpstream);
	}

	@Override
	public Demand getNe()
	{
		return (Demand) associatedNpElement;
	}

	@Override
	public String toString()
	{
		return "WIpUnicastDemand (" + this.getCurrentOfferedTrafficInGbps() + "G) " + getOriginNode() + "->" + getDestinationNode();
	}

	/** Returns the service chains realizing this serice chain request
	 * @return
	 */
	public SortedSet<WServiceChain> getServiceChains () { return getNe().getRoutes().stream().map(r->new WServiceChain(r)).collect(Collectors.toCollection(TreeSet::new)); }

	@Override
	void checkConsistency()
	{
		if (this.wasRemoved()) return;
		if (this.isBidirectional()) assert this.getBidirectionalPair().get().getBidirectionalPair().get().equals(this);
		if (this.isBidirectional()) assert this.getBidirectionalPair().get().isDownstream() == this.isUpstream();
		assert !this.getNe().hasTag(WNetConstants.TAGDEMANDIP_INDICATIONISBUNDLE);
		assert this.getNe().getLayer().equals(getNet().getIpLayer().getNe());
	}

	
}
