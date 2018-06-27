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

import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Instances of this class are service chains, realizing service chain requests. A service chain should start in one of the origin nodes of the service chain, and end in 
 * one of the destination nodes of the service chain. The injection traffic of the service chain is the traffic produced by its origin node. Note that when the service chain 
 * traffic traverses a VNF, its volume will increase or decrease according to the expansion factor defined for that VNF.
 * 
 *  
 *
 */
public class WServiceChain extends WAbstractNetworkElement
{
	WServiceChain(Route r) 
	{ 
		super (r); 
		this.r = r; 
		assert r.getLayer().getIndex() == 1; 
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
		return r.getSeqLinks().stream().map(ee->new WIpLink (ee)).filter(e->!e.isVirtualLink()).collect(Collectors.toCollection(ArrayList::new));
	}
	/** Returns the sequence of traversed VNF instances, filtering out any IP link traversed
	 * @return see above
	 */
	public List<WVnfInstance> getSequenceOfTraversedVnfInstances () 
	{
		return r.getSeqResourcesTraversed().stream().map(ee->new WVnfInstance (ee)).collect(Collectors.toCollection(ArrayList::new));
	}
	/** Returns the sequence of traversed IP links and VNF instances traversed
	 * @return see above
	 */
	public List<? extends WAbstractNetworkElement> getSequenceOfLinksAndVnfs ()
	{
		return r.getPath().stream().map(ee->(WAbstractNetworkElement) (ee instanceof Link? new WIpLink ((Link) ee) : new WVnfInstance ((Resource) ee))).filter(e->e.isWIpLink()? !e.getAsIpLink().isVirtualLink() : true).collect(Collectors.toCollection(ArrayList::new));
	}
	/** Returns the traffic injected by the service chain origin node in Gbps
	 * @return see above
	 */
	public double getInitiallyInjectedTrafficGbps () { return r.getCarriedTraffic(); }

	/** Sets the traffic injected by the service chain origin node in Gbps
	 * @param injectedTrafficGbps see above
	 */
	public void setInitiallyInjectedTrafficGbps (double injectedTrafficGbps) { setPathAndInitiallyInjectedTraffic(Optional.of(injectedTrafficGbps), Optional.empty()); }

	/** Reconfigures the path of the service chain: the sequence of IP links and VNF instances to traverse
	 * @param seqIpLinksAndVnfInstances see above
	 */
	public void setPath (List<? extends WAbstractNetworkElement> seqIpLinksAndVnfInstances) { setPathAndInitiallyInjectedTraffic(Optional.empty() , Optional.of(seqIpLinksAndVnfInstances)); }

	/** Reconfigures in one shot: (i) the injected traffic by the origin node of the service chain, and (ii) the path of the chain: the sequence of traversed IP links and VNF instances
	 * @param optInjectedTrafficGbps see above
	 * @param optSeqIpLinksAndVnfInstances see above
	 */
	public void setPathAndInitiallyInjectedTraffic (Optional<Double> optInjectedTrafficGbps , Optional<List<? extends WAbstractNetworkElement>> optSeqIpLinksAndVnfInstances)
	{
		final double injectedTrafficGbps = optInjectedTrafficGbps.orElse(this.getInitiallyInjectedTrafficGbps());
		final List<? extends WAbstractNetworkElement> seqIpLinksAndVnfInstances = optSeqIpLinksAndVnfInstances.orElse(this.getSequenceOfLinksAndVnfs());
		final Pair<List<NetworkElement> , List<Double>> npInfo = computeScPathAndOccupationInformationAndCheckValidity (this.getServiceChainRequest() , injectedTrafficGbps , seqIpLinksAndVnfInstances);
		r.setPath(injectedTrafficGbps, npInfo.getFirst(), npInfo.getSecond());
	}
	
	static Pair<List<NetworkElement> , List<Double>> computeScPathAndOccupationInformationAndCheckValidity (WServiceChainRequest scRequest , double injectedTrafficGbps , List<? extends WAbstractNetworkElement> seqIpLinksAndVnfInstances)
	{
		// checks
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
		final List<Double> expensionFactorsRespectToInitialUserTrafficAfterTravVnf = scRequest.getSequenceOfExpansionFactorsRespectToInjection();
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
	public WNode getA () { return this.getSequenceOfTraversedIpLinks().get(0).getA(); }
	/** The service chain destination node
	 * @return see above
	 */
	public WNode getB () { final List<WIpLink> links = getSequenceOfTraversedIpLinks(); return links.get(links.size()-1).getB(); }

	public String toString () { return "SChain(" + this.getInitiallyInjectedTrafficGbps() + "G) " + getA().getName() + "->" + getB().getName(); }

	/** Removes this service chain, realeasing the resources
	 */
	public void remove () { this.r.remove(); }
	
}
