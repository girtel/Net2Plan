/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.niw.WNetConstants.WTYPE;
import com.net2plan.utils.Pair;

/** Instances of this class are service chains, realizing service chain requests. A service chain should start in one of the origin nodes of the service chain, and end in 
 * one of the destination nodes of the service chain. The injection traffic of the service chain is the traffic produced by its origin node. Note that when the service chain 
 * traffic traverses a VNF, its volume will increase or decrease according to the expansion factor defined for that VNF.
 * 
 *  
 *
 */
public class WServiceChain extends WAbstractNetworkElement
{
	static String ATTRIBUTE_CURRENTEXPANSIONFACTOR = NIWNAMEPREFIX + "ATTRIBUTE_CURRENTEXPANSIONFACTOR";
	WServiceChain(Route r) 
	{ 
		super (r, Optional.empty()); 
		this.r = r; 
	}

	final private Route r;

	@Override
	public Route getNe() { return r; }

	/** Returns the service chain request that this service chain is realizing
	 * @return see above
	 */
	public WServiceChainRequest getServiceChainRequest () { return new WServiceChainRequest(r.getDemand()); }
	
	/** Returns the sequence of traversed IP links, filtering out any VNF instance traversed
	 * @return see above
	 */
	public List<WIpLink> getSequenceOfTraversedIpLinks () 
	{
		return r.getSeqLinks().stream().
				filter(ee->getNet().getWElement(ee).equals(Optional.of(WTYPE.WIpLink))).
				map(ee->new WIpLink (ee)).filter(e->!e.isVirtualLink()).collect(Collectors.toCollection(ArrayList::new));
	}
	/** Returns the sequence of traversed VNF instances, filtering out any IP link traversed
	 * @return see above
	 */
	public List<WVnfInstance> getSequenceOfTraversedVnfInstances () 
	{
		return r.getSeqResourcesTraversed().stream().
				map(ee->new WVnfInstance (ee)).collect(Collectors.toCollection(ArrayList::new));
	}
	
	public List<Double> getCurrentExpansionFactorApplied () 
	{
		final List<Double> res = getNe().getAttributeAsDoubleList(ATTRIBUTE_CURRENTEXPANSIONFACTOR, null);
		if (res == null) throw new Net2PlanException ("Corrupted design");
		if (res.size() != this.getSequenceOfTraversedVnfInstances().size()) throw new Net2PlanException ("Corrupted design");
		return res;
	}
	

	/** Returns the sequence of traversed IP links and VNF instances traversed
	 * @return see above
	 */
	public List<WNode> getSequenceOfTraversedIpNodesWithoutConsecutiveRepetitions ()
	{
		final List<WNode> res = new ArrayList<> ();
		res.add(this.getA());
		for (WAbstractNetworkElement vnfOrLink : getSequenceOfLinksAndVnfs())
		{
			final WNode n = vnfOrLink.isVnfInstance()? vnfOrLink.getAsVnfInstance().getHostingNode() : vnfOrLink.getAsIpLink().getB();
			if (n.equals(res.get(res.size()-1))) continue;
			res.add(n);
		}
		return res;
	}

	/** Returns the sequence of traversed IP links and VNF instances traversed
	 * @return see above
	 */
	public List<? extends WAbstractNetworkElement> getSequenceOfLinksAndVnfs ()
	{
		return r.getPath().stream().
				filter(ee->getNet().getWElement(ee).isPresent()).
				map(ee->getNet().getWElement(ee).get()).
				filter(e->e.isWIpLink()? !e.getAsIpLink().isVirtualLink() : true).
				collect(Collectors.toCollection(ArrayList::new));
	}
	/** Returns the traffic injected by the service chain origin node in Gbps
	 * @return see above
	 */
	public double getInitiallyInjectedTrafficGbps () { return r.getCarriedTraffic(); }

	/** Sets the traffic injected by the service chain origin node in Gbps, keeping the same path, but applying the current expansion factors applied 
	 * @param injectedTrafficGbps see above
	 */
	public void setInitiallyInjectedTrafficGbps (double injectedTrafficGbps) { setPathAndInitiallyInjectedTraffic(Optional.of(injectedTrafficGbps), Optional.empty() , Optional.empty()); }

	/** Reconfigures the path of the service chain: the sequence of IP links and VNF instances to traverse, keeping the current expansion factors applied 
	 * @param seqIpLinksAndVnfInstances see above
	 */
	public void setPath (List<? extends WAbstractNetworkElement> seqIpLinksAndVnfInstances) { setPathAndInitiallyInjectedTraffic(Optional.empty() , Optional.of(seqIpLinksAndVnfInstances) , Optional.empty()); }

	/** Reconfigures in one shot: (i) the injected traffic by the origin node of the service chain, (ii) the path of the chain: the sequence of traversed IP links and VNF instances, (iii) the current expansion factors applied by the traversed VNFs 
	 * @param optInjectedTrafficGbps see above
	 * @param optSeqIpLinksAndVnfInstances see above
	 * @param sequenceOfExpansionFactorsRespectToInjection see above
	 */
	public void setPathAndInitiallyInjectedTraffic (Optional<Double> optInjectedTrafficGbps , Optional<List<? extends WAbstractNetworkElement>> optSeqIpLinksAndVnfInstances , Optional<List<Double>> sequenceOfExpansionFactorsRespectToInjection)
	{
		final double injectedTrafficGbps = optInjectedTrafficGbps.orElse(this.getInitiallyInjectedTrafficGbps());
		final List<? extends WAbstractNetworkElement> seqIpLinksAndVnfInstances = optSeqIpLinksAndVnfInstances.orElse(this.getSequenceOfLinksAndVnfs());
		if (seqIpLinksAndVnfInstances.stream().filter(ee->ee.isWIpLink()).anyMatch(e->e.getAsIpLink().isBundleMember())) throw new Net2PlanException ("Bundle members cannot be directly traversed by service chains");
		final Pair<List<NetworkElement> , List<Double>> npInfo = computeScPathAndOccupationInformationAndCheckValidity (this.getServiceChainRequest() , injectedTrafficGbps , seqIpLinksAndVnfInstances , sequenceOfExpansionFactorsRespectToInjection.orElse(getCurrentExpansionFactorApplied()));
		r.setPath(injectedTrafficGbps, npInfo.getFirst(), npInfo.getSecond());
		r.setAttributeAsNumberList(ATTRIBUTE_CURRENTEXPANSIONFACTOR, sequenceOfExpansionFactorsRespectToInjection.orElse(getCurrentExpansionFactorApplied()));
	}
	
	static Pair<List<NetworkElement> , List<Double>> computeScPathAndOccupationInformationAndCheckValidity (WServiceChainRequest scRequest , double injectedTrafficGbps , List<? extends WAbstractNetworkElement> seqIpLinksAndVnfInstances , List<Double> sequenceOfExpansionFactorsToApply)
	{
		// checks
		if (sequenceOfExpansionFactorsToApply.size() != scRequest.getDefaultSequenceOfExpansionFactorsRespectToInjection().size()) throw new Net2PlanException ("Wrong size of the expansion factors");
		if (sequenceOfExpansionFactorsToApply.stream().anyMatch(v->v < 0)) throw new Net2PlanException ("Expansion factors cannot be negative");
		if (seqIpLinksAndVnfInstances.isEmpty()) throw new Net2PlanException ("Wrong path");
		if (seqIpLinksAndVnfInstances.stream().anyMatch(e->!e.isVnfInstance() && !e.isWIpLink())) throw new Net2PlanException ("Wrong path");
		final WAbstractNetworkElement firstElement = seqIpLinksAndVnfInstances.get(0);
		final WAbstractNetworkElement lastElement = seqIpLinksAndVnfInstances.get(seqIpLinksAndVnfInstances.size()-1);
		final WNode firstNode =  firstElement instanceof WVnfInstance ? ((WVnfInstance) firstElement).getHostingNode() : ((WIpLink) firstElement).getA();
		final WNode lastNode = lastElement instanceof WVnfInstance ? ((WVnfInstance) lastElement).getHostingNode() : ((WIpLink) lastElement).getB();
		final List<WIpLink> seqIpLinks = seqIpLinksAndVnfInstances.stream().filter(e->e.isWIpLink()).map(ee->(WIpLink) ee).collect(Collectors.toList());
		if (!scRequest.getPotentiallyValidOrigins().isEmpty()) if (!scRequest.getPotentiallyValidOrigins().contains(firstNode)) throw new Net2PlanException ("Wrong path"); 
		if (!scRequest.getPotentiallyValidDestinations().isEmpty()) if (!scRequest.getPotentiallyValidDestinations().contains(lastNode)) throw new Net2PlanException ("Wrong path");
		final List<Double> newOccupationInformation = new ArrayList<> ();
		int numVnfsAlreadyTraversed = 0;
		double currentInjectedTrafficGbps = injectedTrafficGbps;
		final List<Double> expensionFactorsRespectToInitialUserTrafficAfterTravVnf = sequenceOfExpansionFactorsToApply;
		List<NetworkElement> newPathNp = new ArrayList<> ();
		newPathNp.add(firstNode.getIncomingLinkFromAnycastOrigin());
		newOccupationInformation.add(injectedTrafficGbps);
		for (WAbstractNetworkElement element : seqIpLinksAndVnfInstances)
		{
			newPathNp.add (element.getNe());
			newOccupationInformation.add(currentInjectedTrafficGbps);
			if (element.isVnfInstance())
			{
				currentInjectedTrafficGbps = injectedTrafficGbps * expensionFactorsRespectToInitialUserTrafficAfterTravVnf.get(numVnfsAlreadyTraversed); 
				numVnfsAlreadyTraversed ++;
			}
		}
		newPathNp.add(lastNode.getOutgoingLinkToAnycastDestination());
		newOccupationInformation.add(currentInjectedTrafficGbps);
		return Pair.of(newPathNp, newOccupationInformation);
	}

	/** The service chain origin node
	 * @return see above
	 */
	public WNode getA () 
	{
		final WAbstractNetworkElement e = this.getSequenceOfLinksAndVnfs().get(0);
		if (e.isVnfInstance()) return e.getAsVnfInstance().getHostingNode();
		return e.getAsIpLink().getA();
	}
	/** The service chain destination node
	 * @return see above
	 */
	public WNode getB () 
	{
		final List<? extends WAbstractNetworkElement> list =  this.getSequenceOfLinksAndVnfs();
		final WAbstractNetworkElement e = list.get(list.size()-1);
		if (e.isVnfInstance()) return e.getAsVnfInstance().getHostingNode();
		return e.getAsIpLink().getB();
	}

	@Override
	public String toString () { return "SChain(" + this.getInitiallyInjectedTrafficGbps() + "G) " + getA().getName() + "->" + getB().getName(); }

	/** Indicates if this serivce chain is up: not traversing failied links or nodes, and not traversing links with zero capacity
	 * @return see above
	 */
	public boolean isUp () 
	{
		return !getNe().isDown() && !getNe().isTraversingZeroCapLinks();
	}

	/** Opposite to isUp
	 * @return see above
	 */
	public boolean isDown () { return !isUp(); }
	
	/** Removes this service chain, realeasing the resources
	 */
	public void remove () { this.r.remove(); }

	/** Returns the worst case length in km considering the traversed km in the transport layers
	 * @return see above
	 */
	public double getWorstCaseLengthInKm ()
	{
		return this.getSequenceOfTraversedIpLinks().stream().mapToDouble(e->e.getWorstCaseLengthInKm()).sum();
	}

	/** Returns the worst case latency of this service chain, including propagation time in transport layers, and processing time at the traversed VNFs
	 * @return see above
	 */
	public double getWorstCaseLatencyInMs ()
	{
		return this.getSequenceOfTraversedIpLinks().stream().mapToDouble(e->e.getWorstCasePropagationDelayInMs()).sum() + this.getSequenceOfTraversedVnfInstances().stream().mapToDouble(e->e.getProcessingTimeInMs()).sum();
	}

	
	/** Returns the traffic carried in this service now, in the current network failure state
	 * @return see above
	 */
	public double getCurrentCarriedTrafficGbps () { return r.getCarriedTraffic(); }
	
	/** Returns the traffic that is nominally carried in this service if all the traversed IP links and nodes are not failed.
	 * @return see above
	 */
	public double getCarriedTrafficInNoFaillureStateGbps () { return r.getCarriedTrafficInNoFailureState(); }

	void updateNetPlanObjectAndPropagateUpwards ()
	{
		final boolean isOk = !getNe ().isTraversingZeroCapLinks() && !getNe().isDown();
		if (isOk)
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
	public WTYPE getWType() { return WTYPE.WServiceChain; }
	
}
