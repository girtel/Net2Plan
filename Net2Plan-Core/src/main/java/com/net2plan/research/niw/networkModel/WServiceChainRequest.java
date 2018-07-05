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

import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Pair;

import java.util.*;
import java.util.stream.Collectors;

/** <p>Instances of this class are service chain requests in the IP layer. Service chain requests can be realized by one or more service chains, so each 
 * service chain should start in one among a set of given input nodes, and end in one out of the given output nodes, 
 * and traverse in its path VNF instances, so that the sequence of types of VNFs to traverse is specified in the request.</p> 
 * <p>The service chain request is also characterized by the total amount of traffic offered by the initial node. </p>
 * <p>Every time that the traffic traverses a VNF, 
 * its volume can change (e.g. traffic can be compressed or decompressed), and the user can specify the 
 * compressing/decompressing ratios using so called expansion factors. The expansion factor of a VNF is the output traffic of a given service chain, respect to the 
 * traffic at the ORIGIN of the service chain (not at the origin of the VNF). </p>   
 * <p>Also, the user can specify the maximum acceptable latency limit, from the origin node, to 
 * (i) the input of each VNF, and (ii) the maximum latency to the flow end node. </p>   
 * <p>Service chains can be tagged to be upstream or downstream (user-defined tag).</p> 
 * <p>The offered traffic of a service chain is the current amount of traffic offered (maybe carried or not). It is possible to associate to a service chain request, 
 * information of the different offered traffics that would be produced at different time slots.</p> 
 */
public class WServiceChainRequest extends WAbstractNetworkElement
{
	private static final String ATTNAMECOMMONPREFIX = "ServiceChainRequest_";
	private static final String ATTNAMESUFFIX_TIMESLOTANDINTENSITYINGBPS = "timeSlotAndInitialInjectionIntensityInGbps";
	private static final String ATTNAMESUFFIX_USERSERVICENAME = "userServiceName";
	private static final String ATTNAMESUFFIX_ISUPSTREAM = "isUpstream";
	private static final String ATTNAMESUFFIX_VALIDINPUTNODENAMES = "validInputNodeNames";
	private static final String ATTNAMESUFFIX_VALIDOUTPUTNODENAMES = "validOutputNodeNames";
	private static final String ATTNAMESUFFIX_SEQUENCEOFEXPANSIONFACTORRESPECTTOINJECTION = "sequenceOfPerVnfExpansionFactorsRespectToInjection";
	private static final String ATTNAMESUFFIX_LISTMAXLATENCYFROMINITIALTOVNFSTART_MS = "limitMaxLatencyFromInitialToVnfStart_ms";

	private final Demand sc; // from anycastIN to anycastOUT
	
	WServiceChainRequest(Demand sc) 
	{ 
		super(sc); 
		this.sc = sc; 
		assert sc.getLayer().getIndex() == 1;
		assert new WNode (sc.getIngressNode()).isVirtualNode();
		assert new WNode (sc.getEgressNode()).isVirtualNode();
	}

	/** Returns the number of VNFs that service chains of this request should traverse
	 * @return see above
	 */
	public int getNumberVnfsToTraverse () { return sc.getServiceChainSequenceOfTraversedResourceTypes().size(); }
	
	/** Returns the list of maximum latencies specified for this service chain request. The list contains V+1 values, being 
	 * V the number of VNFs to traverse. The first V values (i=1,...,V) correspond to the maximum latency from the origin node, to the 
	 * input of the i-th VNF traversed. The last value correspon to the maximum latency from the origin node to the end node.
	 * @return see above
	 */
	public List<Double> getListMaxLatencyFromOriginToVnStartAndToEndNode_ms ()
	{
		final int numVnfs = getNumberVnfsToTraverse();
		if (numVnfs == 0) return Arrays.asList();
		final Double [] defaultVal = new Double [getNumberVnfsToTraverse()]; Arrays.fill(defaultVal, Double.MAX_VALUE);
		final List<Double> res = getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_LISTMAXLATENCYFROMINITIALTOVNFSTART_MS, Arrays.asList(defaultVal));
		return res.stream().map(v->v <= 0? Double.MAX_VALUE : v).collect(Collectors.toCollection(ArrayList::new));
	}
	/** Sets the information of the list of maximum latencies (in ms) specified for this service chain. See getListMaxLatencyFromOriginToVnStartAndToEndNode_ms 
	 * for an explanation
	 * @param maxLatencyList_ms see above
	 */
	public void setListMaxLatencyFromInitialToVnfStart_ms (List<Double>  maxLatencyList_ms) 
	{
		final int numVnfs = getNumberVnfsToTraverse();
		if (maxLatencyList_ms.size() != numVnfs + 1) throw new Net2PlanException ("Wrong number of latency factors");
		getNe().setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_LISTMAXLATENCYFROMINITIALTOVNFSTART_MS, (List<Number>) (List<?>) maxLatencyList_ms);
	}
	
	/** Returns a list with the information of the traffic expansion factors when traversing the VNFs. This is a list with as many 
	 * elements as the number of VNFs to traverse (V). If the i-th element of the list is X, means that for each traffic unit injected 
	 * by the service chain origin node, X traffic units will be produced at the output of the i-th service chain.  
	 * @return see above
	 */
	public List<Double> getSequenceOfExpansionFactorsRespectToInjection () 
	{
		final int numVnfs = getNumberVnfsToTraverse();
		if (numVnfs == 0) return Arrays.asList();
		final Double [] defaultVal = new Double [getNumberVnfsToTraverse()]; Arrays.fill(defaultVal, 1.0);
		return getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_SEQUENCEOFEXPANSIONFACTORRESPECTTOINJECTION, Arrays.asList(defaultVal));
	}

	/** Adds a service chain to satisfy traffic of this service chain request. The service chain should have an initial node in the set of valid initial nodes, an end 
	 * node in the set of valid end nodes, and traverse VNF instances in the order indicated by the service chain request.
	 * @param sequenceOfIpLinksAndResources the sequence of WIpLink and WVnfInstance objects realizing this service chain
	 * @param injectionTrafficGbps the traffic injected by the origin node of the service chain. Note that VNFs will change the volumes of the traversed flows, according to the expansion factors defined for the service chain 
	 * @return see above
	 */
	public WServiceChain addServiceChain (List<? extends WAbstractNetworkElement> sequenceOfIpLinksAndResources , double injectionTrafficGbps)
	{
		final Pair<List<NetworkElement> , List<Double>> npInfo = WServiceChain.computeScPathAndOccupationInformationAndCheckValidity(this, injectionTrafficGbps, sequenceOfIpLinksAndResources);
		final Route r = getNe().getNetPlan().addServiceChain(this.sc, injectionTrafficGbps, npInfo.getSecond(), npInfo.getFirst(), null);
		return new WServiceChain(r);
	}
	
	/** Sets a list with the information of the traffic expansion factors when traversing the VNFs. This is a list with as many 
	 * elements as the number of VNFs to traverse (V). If the i-th element of the list is X, means that for each traffic unit injected 
	 * by the service chain origin node, X traffic units will be produced at the output of the i-th service chain.  
	 * @param sequenceOfExpansionFactors see above
	 */
	public void setSequenceOfExpansionFactorsRespectToInjection (List<Double>  sequenceOfExpansionFactors) 
	{
		final int numVnfs = getNumberVnfsToTraverse();
		if (sequenceOfExpansionFactors.size() != numVnfs) throw new Net2PlanException ("Wrong number of expansion factors");
		getNe().setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_SEQUENCEOFEXPANSIONFACTORRESPECTTOINJECTION, (List<Number>) (List<?>) sequenceOfExpansionFactors);
	}
	
	/** Returns the potentially valid origin nodes for the service chains of this request
	 * @return see above
	 */
	public SortedSet<WNode> getPotentiallyValidOrigins () 
	{ 
		final List<String> resNames = sc.getAttributeAsStringList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_VALIDINPUTNODENAMES , null);
		if (resNames == null) throw new Net2PlanException ("The list of initial nodes is empty");
		final SortedSet<WNode> res = new TreeSet<> ();
		if (resNames.isEmpty()) res.addAll(this.getNet().getNodes());
		else
		{
			for (String name : resNames) { final WNode nn = this.getNet().getNodeByName(name).orElse(null); if (nn != null) res.add(nn); } 
		}
		if (res.isEmpty()) throw new Net2PlanException ("The list of initial nodes is empty");
		return res;
	}

	/** Sets the potentially valid origin nodes for the service chains of this request
	 * @param validOrigins see above
	 */
	public void setPotentiallyValidOrigins (SortedSet<WNode> validOrigins) 
	{ 
		final List<String> resNames = validOrigins.stream().map(n->n.getName()).collect(Collectors.toList()); 
		sc.setAttributeAsStringList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_VALIDINPUTNODENAMES , resNames);
	}

	/** Returns the potentially valid destination nodes for the service chains of this request
	 * @return see above
	 */
	public SortedSet<WNode> getPotentiallyValidDestinations () 
	{ 
		final List<String> resNames = sc.getAttributeAsStringList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_VALIDOUTPUTNODENAMES , null);
		if (resNames == null) throw new Net2PlanException ("The list of initial nodes is empty");
		final SortedSet<WNode> res = new TreeSet<> ();
		if (resNames.isEmpty()) res.addAll(this.getNet().getNodes());
		else
		{
			for (String name : resNames) { final WNode nn = this.getNet().getNodeByName(name).orElse(null); if (nn != null) res.add(nn); } 
		}
		if (res.isEmpty()) throw new Net2PlanException ("The list of destination nodes is empty");
		return res;
	}
	/** Sets the potentially valid destination nodes for the service chains of this request
	 * @param validDestinations see above
	 */
	public void setPotentiallyValidDestinations (SortedSet<WNode> validDestinations) 
	{ 
		final List<String> resNames = validDestinations.stream().map(n->n.getName()).collect(Collectors.toList()); 
		sc.setAttributeAsStringList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_VALIDOUTPUTNODENAMES , resNames);
	}

	/** Get the user-defined traffic intensity associated to a time slot with the given name, 
	 * @param timeSlotName see above
	 * @return see above
	 */
	public Optional<Double> getTrafficIntensityInfo (String timeSlotName)
	{
		return getTimeSlotNameAndInitialInjectionIntensityInGbpsList().stream().filter(p->p.getFirst().equals(timeSlotName)).map(p->p.getSecond()).findFirst();  
	}
	
	/** Get the user-defined traffic intensity associated to a time slot with the given index, 
	 * @param timeSlotIndex see above
	 * @return a pair with name of the time slot and the traffic intensity value
	 */
	public Optional<Pair<String,Double>> getTrafficIntensityInfo (int timeSlotIndex)
	{
		final List<Pair<String,Double>> res = getTimeSlotNameAndInitialInjectionIntensityInGbpsList();
		if (res.size() <= timeSlotIndex) return Optional.empty();  
		return Optional.of(res.get(timeSlotIndex));  
	}

	/** Returns the sequence of the VNF types that should be traversed by the service chains realizing this request
	 * @return see above
	 */
	public List<String> getSequenceVnfTypes () { return sc.getServiceChainSequenceOfTraversedResourceTypes(); }
	
	/** Returns the name of the user service that this service chain request is associated to 
	 * @return see above
	 */
	public String getUserServiceName () 
	{ 
		final String res = getAttributeOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_USERSERVICENAME , null);
		assert res != null; 
		return res;
	}
	/** Sets the name of the user service that this service chain request is associated to 
	 * @param userServiceName see above
	 */
	public void setUserServiceName (String userServiceName) 
	{ 
		getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_USERSERVICENAME , userServiceName);
	}

	/** Returns the full user-defined traffic intensity information as a list of pairs, where each pair contains the user-defined name of the time slot, and the traffic intensity 
	 * associated to that time slot 
	 * @return see above
	 */
	public List<Pair<String,Double>> getTimeSlotNameAndInitialInjectionIntensityInGbpsList ()
	{
		final List<String> vals = sc.getAttributeAsStringList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_TIMESLOTANDINTENSITYINGBPS, null);
		if (vals == null) return new ArrayList<> ();
		try
		{
			final Set<String> previousTimeSlotIds = new HashSet<> ();
			final List<Pair<String,Double>> res = new ArrayList<> ();
			final Iterator<String> it = vals.iterator();
			while (it.hasNext())
			{
				final String timeSlotName = it.next();
				final Double intensity = Double.parseDouble(it.next());
				if (previousTimeSlotIds.contains(timeSlotName)) continue; else previousTimeSlotIds.add(timeSlotName);
				res.add(Pair.of(timeSlotName, intensity));
			}
			return res;
		} catch (Exception e) { return new ArrayList<> (); }
	}
	/** Sets the full user-defined traffic intensity information as a list of pairs, where each pair contains the user-defined name of the time slot, and the traffic intensity 
	 * associated to that time slot 
	 * @param info see above
	 */
	public void setTimeSlotNameAndInitialInjectionIntensityInGbpsList (List<Pair<String,Double>> info)
	{
		final List<String> vals = new ArrayList<> ();
		final Set<String> previousTimeSlotIds = new HashSet<> ();
		for (Pair<String,Double> p : info)
		{
			final String timeSlotName = p.getFirst();
			final Double intensity = p.getSecond();
			if (previousTimeSlotIds.contains(timeSlotName)) continue; else previousTimeSlotIds.add(timeSlotName);
			vals.add(timeSlotName); vals.add(intensity.toString());
		}
		sc.setAttributeAsStringList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_TIMESLOTANDINTENSITYINGBPS, vals);
	}
	
	/** Returns the current offered traffic of this service chain request (that may carried partially, totally or none)
	 * @return see above
	 */
	public double getCurrentOfferedTrafficInGbps () { return sc.getOfferedTraffic(); }
	
	/** Sets the current offered traffic in Gbps for this service chain
	 * @param offeredTrafficInGbps see above
	 */
	public void setCurrentOfferedTrafficInGbps (double offeredTrafficInGbps) 
	{
		sc.setOfferedTraffic(offeredTrafficInGbps); 
	}

	/** Indicates if this is an upstream service chain request (initiated in the user). Returns false if the service chain is downstream (ended in the user)
	 * @return see above
	 */
	public boolean isUpstream () { final Boolean res = getAttributeAsBooleanOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ISUPSTREAM, null); assert res != null; return res; }

	/** Sets if this is an upstream service chain request (initiated in the user), with true, of false if the service chain is downstream (ended in the user)
	 * @param isUpstream see above
	 */
	public void setIsUpstream (boolean isUpstream) 
	{ 
		setAttributeAsBoolean(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ISUPSTREAM, isUpstream); 
	}
	
	@Override
	public Demand getNe() { return (Demand) e; }

	public String toString () { return "SChainReq(" + this.getCurrentOfferedTrafficInGbps() + "G) " + getPotentiallyValidOrigins() + "->" + getPotentiallyValidDestinations(); }

}
