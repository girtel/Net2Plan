/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.utils.Pair;

/**
 * <p>
 * This is an abstract class that has the common behavior of unicast IP demands, and anycast service chains<p>
 * </p>
 */
public abstract class WAbstractIpUnicastOrAnycastDemand extends WAbstractNetworkElement
{
	private static final String ATTNAMECOMMONPREFIX = "WAbstractIpUnicastOrAnycastDemand_";
	private static final String ATTNAMESUFFIX_TIMESLOTANDINTENSITYINGBPS = "timeSlotAndInitialInjectionIntensityInGbps";
	private static final String ATTNAMESUFFIX_ISUPSTREAM = "isUpstream";

	protected final Demand sc; // from anycastIN to anycastOUT

	WAbstractIpUnicastOrAnycastDemand(Demand sc)
	{
		super(sc, Optional.empty());
		this.sc = sc;
	}

	public abstract boolean isBidirectional();

	public abstract void setBidirectionalPair (WAbstractIpUnicastOrAnycastDemand scr);

	public abstract void removeBidirectionalPairRelation ();
	
	public abstract Optional<? extends WAbstractIpUnicastOrAnycastDemand> getBidirectionalPair ();


	/**
	 * Get the user-defined traffic intensity associated to a time slot with the given name,
	 * @param timeSlotName see above
	 * @return see above
	 */
	public final Optional<Double> getTrafficIntensityInfo(String timeSlotName)
	{
		return getTimeSlotNameAndInitialInjectionIntensityInGbpsList().stream().filter(p -> p.getFirst().equals(timeSlotName)).map(p -> p.getSecond()).findFirst();
	}

	/**
	 * Get the user-defined traffic intensity associated to a time slot with the given index,
	 * @param timeSlotIndex see above
	 * @return a pair with name of the time slot and the traffic intensity value
	 */
	public final Optional<Pair<String, Double>> getTrafficIntensityInfo(int timeSlotIndex)
	{
		final List<Pair<String, Double>> res = getTimeSlotNameAndInitialInjectionIntensityInGbpsList();
		if (res.size() <= timeSlotIndex) return Optional.empty();
		return Optional.of(res.get(timeSlotIndex));
	}

	/**
	 * Returns the name of the user-defined QoS type of this service chain request 
	 * @return see above
	 */
	public final String getQosType()
	{
		return getNe().getQosType();
	}

	/**
	 * Sets the QoS type of this service chain request 
	 * @param qosType see above
	 */
	public final void setQosType(String qosType)
	{
		getNe().setQoSType(qosType);
	}

	/**
	 * Returns the full user-defined traffic intensity information as a list of pairs, where each pair contains the
	 * user-defined name of the time slot, and the traffic intensity associated to that time slot
	 * @return see above
	 */
	public final List<Pair<String, Double>> getTimeSlotNameAndInitialInjectionIntensityInGbpsList()
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
	public final void setTimeSlotNameAndInitialInjectionIntensityInGbpsList(List<Pair<String, Double>> info)
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
	public final double getCurrentOfferedTrafficInGbps()
	{
		return sc.getOfferedTraffic();
	}

	/**
	 * Returns the carried traffic of this request
	 * @return see above
	 */
	public final double getCurrentCarriedTrafficGbps ()
	{
		return sc.getCarriedTraffic();
	}

	/**
	 * Returns the carried traffic of this request
	 * @return see above
	 */
	public final double getCurrentBlockedTraffic()
	{
		final double dif = sc.getOfferedTraffic() - sc.getCarriedTraffic();
		return dif < Configuration.precisionFactor ? 0 : dif;
	}

	/**
	 * Sets the current offered traffic in Gbps for this service chain
	 * @param offeredTrafficInGbps see above
	 */
	public final void setCurrentOfferedTrafficInGbps(double offeredTrafficInGbps)
	{
		sc.setOfferedTraffic(offeredTrafficInGbps);
	}

	/**
	 * Indicates if this is a service chain request is upstream (initiated in the user). Returns false if the service chain
	 * is downstream (ended in the user)
	 * @return see above
	 */
	public final boolean isUpstream()
	{
		final Boolean res = getAttributeAsBooleanOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ISUPSTREAM, false);
		assert res != null;
		return res;
	}

	/**
	 * The opposite to isUpstream
	 * @return see above
	 */
	public final boolean isDownstream()
	{
		return !isUpstream();
	}

	/**
	 * Sets if this is an upstream service chain request (initiated in the user), with true, of false if the service chain
	 * is downstream (ended in the user)
	 * @param isUpstream see above
	 */
	public final void setIsUpstream(boolean isUpstream)
	{
		setAttributeAsBoolean(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ISUPSTREAM, isUpstream);
	}

	@Override
	public final Demand getNe()
	{
		return (Demand) associatedNpElement;
	}

	public abstract double getWorstCaseEndtoEndLatencyMs ();

	public abstract double getWorstCaseEndtoEndLengthInKm ();

	
//	/** Returns the worst case latency in milliseconds of any flow realizing this IP demand, considering all the underlying layers  
//	 * @return see above
//	 */
//	public final double getWorstCaseEndtoEndLatencyMs ()
//	{
//		return getNe().getWorstCasePropagationTimeInMs();
//	}
	
//	/** Returns the worst case length in km of any flow realizing this IP demand, considering all the underlying layers  
//	 * @return see above
//	 */
//	public final double getWorstCaseEndtoEndLengthInKm ()
//	{
//		return getNe().getWorstCaseLengthInKm();
//	}

	public abstract void remove ();
	
}
