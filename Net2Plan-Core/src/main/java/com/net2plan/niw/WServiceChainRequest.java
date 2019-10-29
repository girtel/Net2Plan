/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.niw.WNetConstants.WTYPE;
import com.net2plan.utils.Pair;

/**
 * <p>
 * Instances of this class are service chain requests in the IP layer. Service chain requests can be realized by one or
 * more service chains, so each service chain should start in one among a set of given input nodes, and end in one out
 * of the given output nodes, and traverse in its path VNF instances, so that the sequence of types of VNFs to traverse
 * is specified in the request.
 * </p>
 * <p>
 * The service chain request is also characterized by the total amount of traffic offered by the initial node.
 * </p>
 * <p>
 * Every time that the traffic traverses a VNF, its volume can change (e.g. traffic can be compressed or decompressed),
 * and the user can specify the compressing/decompressing ratios using so called expansion factors. The expansion factor
 * of a VNF is the output traffic of a given service chain, respect to the traffic at the ORIGIN of the service chain
 * (not at the origin of the VNF).
 * </p>
 * <p>
 * Also, the user can specify the maximum acceptable latency limit, from the origin node, to (i) the input of each VNF,
 * and (ii) the maximum latency to the flow end node.
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
public class WServiceChainRequest extends WAbstractIpUnicastOrAnycastDemand
{
	static final String ATTNAMECOMMONPREFIX = NIWNAMEPREFIX + "ServiceChainRequest_";
	static final String ATTNAMESUFFIX_VALIDINPUTNODENAMES = "validInputNodeNames";
	static final String ATTNAMESUFFIX_VALIDOUTPUTNODENAMES = "validOutputNodeNames";
	private static final String ATTNAMESUFFIX_SEQUENCEOFEXPANSIONFACTORRESPECTTOINJECTION = "sequenceOfPerVnfExpansionFactorsRespectToInjection";
	private static final String ATTNAMESUFFIX_LISTMAXLATENCYFROMINITIALTOVNFSTART_MS = "limitMaxLatencyFromInitialToVnfStart_ms";
	private static final String ATTNAMESUFFIX_BIDIRECTIONALPAIRNPDEMANDID = "bidirectionalPairNpDemandId";

	public WServiceChainRequest(Demand sc)
	{
		super(sc);
	}

	/**
	 * Indicates if this unidirectional service chain request has associated an opposite service chain request-. If this
	 * flow is upstream, the opposite is downstream. An initial valid node for this stream is an ending valid node of the
	 * opposite and viceversa. VNFs traversed do not need to be the same.
	 * @return see above
	 */
	public boolean isBidirectional()
	{
		return getBidirectionalPair().isPresent();
	}

	/**
	 * Returns the opposite service chain request to this, if any
	 * @return see above
	 */
	@Override
	public Optional<WServiceChainRequest> getBidirectionalPair()
	{
		final Double id = getNe().getAttributeAsDouble(ATTNAMESUFFIX_BIDIRECTIONALPAIRNPDEMANDID, null);
		if (id == null) return Optional.empty();
		final Demand d = getNe().getNetPlan().getDemandFromId(id.longValue());
		return d == null ? Optional.empty() : Optional.of(new WServiceChainRequest(d));
	}

	/**
	 * Makes that this service chain request is tagged as the opposite one to a given SCR, and viceversa. The two SCRs must
	 * have opposite nodes (an origin node of this is destination in the other and viceversa), one must be downstream and
	 * the other upstream. Any other opposite relation to other SCR is released.
	 * @param scr see above
	 */
	public void  setBidirectionalPair(WAbstractIpUnicastOrAnycastDemand scr)
	{
		if (scr == null) throw new Net2PlanException("Please specify a not null service chain request");
		if (!(scr instanceof WServiceChainRequest)) throw new Net2PlanException("Please specify a not null service chain request");
		final WServiceChainRequest scrr = (WServiceChainRequest) scr;
		if (!this.getPotentiallyValidOrigins().equals(scrr.getPotentiallyValidDestinations())) throw new Net2PlanException("The origins of this service chain request must be the end nodes of the opposite, and viceversa");
		if (!scrr.getPotentiallyValidOrigins().equals(this.getPotentiallyValidDestinations())) throw new Net2PlanException("The origins of this service chain request must be the end nodes of the opposite, and viceversa");
		if (this.isDownstream() == scrr.isDownstream()) throw new Net2PlanException("One flow must be tagged as upstream, the other as downstream");
		removeBidirectionalPairRelation();
		getNe().setAttribute(ATTNAMESUFFIX_BIDIRECTIONALPAIRNPDEMANDID, scr.getNe().getId());
		scr.getNe().setAttribute(ATTNAMESUFFIX_BIDIRECTIONALPAIRNPDEMANDID, this.getNe().getId());
	}

	/**
	 * If this service chain request has an opposite request associated, removes such association. If not, makes nothing
	 * happens
	 */
	public void removeBidirectionalPairRelation()
	{
		if (!this.isBidirectional()) return;
		getBidirectionalPair().get().getNe().setAttribute(ATTNAMESUFFIX_BIDIRECTIONALPAIRNPDEMANDID, -1);
		getNe().setAttribute(ATTNAMESUFFIX_BIDIRECTIONALPAIRNPDEMANDID, -1);
	}

	/**
	 * Returns the number of VNFs that service chains of this request should traverse
	 * @return see above
	 */
	public int getNumberVnfsToTraverse()
	{
		return sc.getServiceChainSequenceOfTraversedResourceTypes().size();
	}

	/**
	 * Returns the list of maximum latencies specified for this service chain request. The list contains V+1 values, being V
	 * the number of VNFs to traverse. The first V values (i=1,...,V) correspond to the maximum latency from the origin
	 * node, to the input of the i-th VNF traversed. The last value correspon to the maximum latency from the origin node to
	 * the end node.
	 * @return see above
	 */
	public List<Double> getListMaxLatencyFromOriginToVnStartAndToEndNode_ms()
	{
		final int numVnfs = getNumberVnfsToTraverse();
		if (numVnfs == 0) return Arrays.asList();
		final Double[] defaultVal = new Double[getNumberVnfsToTraverse()];
		Arrays.fill(defaultVal, Double.MAX_VALUE);
		final List<Double> res = getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_LISTMAXLATENCYFROMINITIALTOVNFSTART_MS, Arrays.asList(defaultVal));
		return res.stream().map(v -> v <= 0 ? Double.MAX_VALUE : v).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Sets the information of the list of maximum latencies (in ms) specified for this service chain. See
	 * getListMaxLatencyFromOriginToVnStartAndToEndNode_ms for an explanation
	 * @param maxLatencyList_ms see above
	 */
	public void setListMaxLatencyFromInitialToVnfStart_ms(List<Double> maxLatencyList_ms)
	{
		final int numVnfs = getNumberVnfsToTraverse();
		if (maxLatencyList_ms.size() != numVnfs + 1) throw new Net2PlanException("Wrong number of latency factors");
		getNe().setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_LISTMAXLATENCYFROMINITIALTOVNFSTART_MS, (List<Number>) (List<?>) maxLatencyList_ms);
	}

	/**
	 * Returns a list with the information of the traffic expansion factors when traversing the VNFs selected as default for
	 * the service chains realizing this request. This is a list with as many elements as the number of VNFs to traverse
	 * (V). If the i-th element of the list is X, means that for each traffic unit injected by the service chain origin
	 * node, X traffic units will be produced at the output of the i-th service chain.
	 * @return see above
	 */
	public List<Double> getDefaultSequenceOfExpansionFactorsRespectToInjection()
	{
		final int numVnfs = getNumberVnfsToTraverse();
		if (numVnfs == 0) return Arrays.asList();
		final Double[] defaultVal = new Double[getNumberVnfsToTraverse()];
		Arrays.fill(defaultVal, 1.0);
		return getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_SEQUENCEOFEXPANSIONFACTORRESPECTTOINJECTION, Arrays.asList(defaultVal));
	}

	/**
	 * Adds a service chain to satisfy traffic of this service chain request. The service chain should have an initial node
	 * in the set of valid initial nodes, an end node in the set of valid end nodes, and traverse VNF instances in the order
	 * indicated by the service chain request. The expansion factors of traffic when traversing the VNFs to apply are the
	 * default ones applied to this service chain.
	 * @param sequenceOfIpLinksAndResources the sequence of WIpLink and WVnfInstance objects realizing this service chain
	 * @param injectionTrafficGbps the traffic injected by the origin node of the service chain. Note that VNFs will change
	 *        the volumes of the traversed flows, according to the expansion factors defined for the service chain
	 * @return see above
	 */
	public WServiceChain addServiceChain(List<? extends WAbstractNetworkElement> sequenceOfIpLinksAndResources, double injectionTrafficGbps)
	{
		final Pair<List<NetworkElement>, List<Double>> npInfo = WServiceChain.computeScPathAndOccupationInformationAndCheckValidity(this, injectionTrafficGbps, sequenceOfIpLinksAndResources, getDefaultSequenceOfExpansionFactorsRespectToInjection());
		final Route r = getNe().getNetPlan().addServiceChain(this.sc, injectionTrafficGbps, npInfo.getSecond(), npInfo.getFirst(), null);
		r.setAttributeAsNumberList(WServiceChain.ATTRIBUTE_CURRENTEXPANSIONFACTOR, getDefaultSequenceOfExpansionFactorsRespectToInjection());
		return new WServiceChain(r);
	}

	/**
	 * Adds a service chain to satisfy traffic of this service chain request. The service chain should have an initial node
	 * in the set of valid initial nodes, an end node in the set of valid end nodes, and traverse VNF instances in the order
	 * indicated by the service chain request. The expansion factors of traffic when traversing the VNFs to apply are the
	 * default ones applied to this service chain.
	 * @param sequenceOfIpLinksAndResources the sequence of WIpLink and WVnfInstance objects realizing this service chain
	 * @param injectionTrafficGbps the traffic injected by the origin node of the service chain. Note that VNFs will change
	 *        the volumes of the traversed flows, according to the expansion factors defined for the service chain
	 * @param sequenceOfExpansionFactorsRespectToInjection the sequence of expansion factors to apply to this service chain
	 * @return see above
	 */
	public WServiceChain addServiceChain(List<? extends WAbstractNetworkElement> sequenceOfIpLinksAndResources, double injectionTrafficGbps, List<Double> sequenceOfExpansionFactorsRespectToInjection)
	{
		final Pair<List<NetworkElement>, List<Double>> npInfo = WServiceChain.computeScPathAndOccupationInformationAndCheckValidity(this, injectionTrafficGbps, sequenceOfIpLinksAndResources, sequenceOfExpansionFactorsRespectToInjection);
		final Route r = getNe().getNetPlan().addServiceChain(this.sc, injectionTrafficGbps, npInfo.getSecond(), npInfo.getFirst(), null);
		r.setAttributeAsNumberList(WServiceChain.ATTRIBUTE_CURRENTEXPANSIONFACTOR, getDefaultSequenceOfExpansionFactorsRespectToInjection());
		return new WServiceChain(r);
	}

	/**
	 * Sets a list with the information of the traffic expansion factors when traversing the VNFs. This is a list with as
	 * many elements as the number of VNFs to traverse (V). If the i-th element of the list is X, means that for each
	 * traffic unit injected by the service chain origin node, X traffic units will be produced at the output of the i-th
	 * service chain.
	 * @param sequenceOfExpansionFactors see above
	 */
	public void setDefaultSequenceOfExpansionFactorsRespectToInjection(List<Double> sequenceOfExpansionFactors)
	{
		final int numVnfs = getNumberVnfsToTraverse();
		if (sequenceOfExpansionFactors.size() != numVnfs) throw new Net2PlanException("Wrong number of expansion factors");
		getNe().setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_SEQUENCEOFEXPANSIONFACTORRESPECTTOINJECTION, (List<Number>) (List<?>) sequenceOfExpansionFactors);
	}

	/**
	 * Returns the potentially valid origin nodes for the service chains of this request
	 * @return see above
	 */
	public SortedSet<WNode> getPotentiallyValidOrigins()
	{
		final List<String> resNames = sc.getAttributeAsStringList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_VALIDINPUTNODENAMES, null);
		if (resNames == null) throw new Net2PlanException("The list of initial nodes is empty");
		final SortedSet<WNode> res = new TreeSet<>();
		if (resNames.isEmpty()) res.addAll(this.getNet().getNodes());
		else
		{
			for (String name : resNames)
			{
				final WNode nn = this.getNet().getNodeByName(name).orElse(null);
				if (nn != null) res.add(nn);
			}
		}
		if (res.isEmpty()) throw new Net2PlanException("The list of initial nodes is empty");
		return res;
	}

	/**
	 * Sets the potentially valid origin nodes for the service chains of this request
	 * @param validOrigins see above
	 */
	public void setPotentiallyValidOrigins(SortedSet<WNode> validOrigins)
	{
		if (validOrigins.isEmpty()) throw new Net2PlanException ("Nodes cannot be an empty set");
		final List<String> resNames = validOrigins.stream().map(n -> n.getName()).collect(Collectors.toList());
		if (getPotentiallyValidDestinations().stream().anyMatch(n->validOrigins.contains(n))) 
				throw new Net2PlanException("Origin nodes cannot also be destination nodes");
		sc.setAttributeAsStringList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_VALIDINPUTNODENAMES, resNames);
	}

	/**
	 * Returns the potentially valid destination nodes for the service chains of this request
	 * @return see above
	 */
	public SortedSet<WNode> getPotentiallyValidDestinations()
	{
		final List<String> resNames = sc.getAttributeAsStringList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_VALIDOUTPUTNODENAMES, null);
		if (resNames == null) throw new Net2PlanException("The list of initial nodes is empty");
		final SortedSet<WNode> res = new TreeSet<>();
		if (resNames.isEmpty()) res.addAll(this.getNet().getNodes());
		else
		{
			for (String name : resNames)
			{
				final WNode nn = this.getNet().getNodeByName(name).orElse(null);
				if (nn != null) res.add(nn);
			}
		}
		if (res.isEmpty()) throw new Net2PlanException("The list of destination nodes is empty");
		return res;
	}

	/**
	 * Sets the potentially valid destination nodes for the service chains of this request
	 * @param validDestinations see above
	 */
	public void setPotentiallyValidDestinations(SortedSet<WNode> validDestinations)
	{
		if (validDestinations.isEmpty()) throw new Net2PlanException ("Nodes cannot be an empty set");
		final List<String> resNames = validDestinations.stream().map(n -> n.getName()).collect(Collectors.toList());
		if (getPotentiallyValidOrigins().stream().anyMatch(n->validDestinations.contains(n))) 
			throw new Net2PlanException("Origin nodes cannot also be destination nodes");
		sc.setAttributeAsStringList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_VALIDOUTPUTNODENAMES, resNames);
	}

	/**
	 * Returns the sequence of the VNF types that should be traversed by the service chains realizing this request
	 * @return see above
	 */
	public List<String> getSequenceVnfTypes()
	{
		return sc.getServiceChainSequenceOfTraversedResourceTypes();
	}

	/** Sets the sequence of the VNF types that should be traversed by the service chains realizing this request. The request cannot have 
	 * service chains at the moment, or an exception is raised
	 * @param seqVnfTypes see above
	 */
	public void setSequenceVnfTypes(List<String> seqVnfTypes)
	{
		if (!this.getServiceChains().isEmpty()) throw new Net2PlanException ("VNF types can only be modified in service chain requests without service chains");
		sc.setServiceChainSequenceOfTraversedResourceTypes(seqVnfTypes);
	}

	
	/**
	 * Returns the carried traffic of this request if all the service chains where non-failed (al their traversed IP links and nodes where up)
	 * @return see above
	 */
	public double getCarriedTrafficInNonFailureStateGbps ()
	{
		return getServiceChains().stream().mapToDouble(e->e.getCarriedTrafficInNoFaillureStateGbps()).sum();
	}

	@Override
	public String toString()
	{
		return "SChainReq(" + this.getCurrentOfferedTrafficInGbps() + "G) " + getPotentiallyValidOrigins() + "->" + getPotentiallyValidDestinations();
	}

	/** Returns the service chains realizing this serice chain request
	 * @return see above
	 */
	public SortedSet<WServiceChain> getServiceChains () { return getNe().getRoutes().stream().map(r->new WServiceChain(r)).collect(Collectors.toCollection(TreeSet::new)); }

	@Override
	void checkConsistency()
	{
		if (this.wasRemoved()) return;
		assert Math.abs(getCurrentCarriedTrafficGbps() - getServiceChains().stream().mapToDouble(sc->sc.getCurrentCarriedTrafficGbps()).sum ()) < 1e-3;
		assert getDefaultSequenceOfExpansionFactorsRespectToInjection().size() == getNumberVnfsToTraverse();
		if (this.isBidirectional()) assert this.getBidirectionalPair().get().getBidirectionalPair().get().equals(this);
		if (this.isBidirectional()) assert this.getBidirectionalPair().get().isDownstream() == this.isUpstream();
		assert getServiceChains().stream().allMatch(s->s.getServiceChainRequest().equals(this));
	}

	/** Removes this service chain request, and any underlying service chains satisfying it
	 */
	public void remove () { this.getNe().remove(); }

	@Override
	public WTYPE getWType() { return WTYPE.WServiceChainRequest; }

	@Override
	public double getWorstCaseEndtoEndLatencyMs() 
	{
		return getServiceChains().stream().filter(e->e.isUp()).mapToDouble(e->e.getWorstCaseLatencyInMs()).max().orElse(Double.MAX_VALUE);
	}

	@Override
	public double getWorstCaseEndtoEndLengthInKm() 
	{
		return getServiceChains().stream().filter(e->e.isUp()).mapToDouble(e->e.getWorstCaseLengthInKm()).max().orElse(Double.MAX_VALUE);
	}

}
