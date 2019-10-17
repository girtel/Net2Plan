/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.Arrays;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.libraries.IPUtils;
import com.net2plan.niw.WNetConstants.WTYPE;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

/**
 * This class represents an unidirectional IP link. IP links can be realized by optical lightpaths or not. If yes, the
 * IP link is said to be "coupled" to the lightpath, and then line rate of the IP link is that of the lightpath, and the
 * IP link will be down whenever the lighptath is down
 *
 */
public class WIpLink extends WAbstractNetworkElement
{
	static final String ATTNAMECOMMONPREFIX = "IpLink_";
	static final String ATTNAMESUFFIX_NOMINALCAPACITYGBPS = "nominalCapacityGbps";

	final private Link npLink;

	WIpLink(Link e)
	{
		super(e , Optional.empty());
		this.npLink = e;
	}

	static WIpLink createFromAdd(Link e, double nominalCapacityGbps)
	{
		final WIpLink res = new WIpLink(e);
		e.setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_NOMINALCAPACITYGBPS, nominalCapacityGbps);
		return res;
	}

	@Override
	public Link getNe()
	{
		return (Link) associatedNpElement;
	}

	/**
	 * Returns the IP link orgin node
	 * @return see above
	 */
	public WNode getA()
	{
		return new WNode(npLink.getOriginNode());
	}

	/**
	 * Returns the IP link destination node
	 * @return see above
	 */
	public WNode getB()
	{
		return new WNode(npLink.getDestinationNode());
	}

	/**
	 * Returns true if the IP link is part of a pair of IP bidirectional links
	 * @return see above
	 */
	public boolean isBidirectional()
	{
		return npLink.isBidirectional();
	}

	/**
	 * Returns true is the IP links is virtual. This means that it starts or ends in a virtual node. Regular users do not
	 * need to use such links.
	 * @return see above
	 */
	public boolean isVirtualLink()
	{
		return getA().isVirtualNode() || getB().isVirtualNode();
	}

	/** Returns the IGP weight configured for this IP link, or Double.MAX_VALUE if the link is failed, or the weight is 
	 * non-positive
	 * @return see above
	 */
	public double getIgpWeight ()
	{
		return IPUtils.getLinkWeight(getNe ());
	}
	
	/** Sets the IGP weight configured for this IP link
	 * @return see above
	 */
	public void setIgpWeight (double newWeight)
	{
		IPUtils.setLinkWeight(getNe () , newWeight);
	}
	
	
	/**
	 * If the IP link is part of a pair of IP bidirectional links, returns the opposite link pair. If not returns null.
	 * @return see above
	 */
	public WIpLink getBidirectionalPair()
	{
		if (!this.isBidirectional()) throw new Net2PlanException("Not a bidirectional link");
		return new WIpLink(npLink.getBidirectionalPair());
	}

	/**
	 * Returns the user-defined link length in km, which is assumed to be the valid one when the IP link is not coupled to a
	 * lightpath
	 * @return see above
	 */
	public double getLengthIfNotCoupledInKm()
	{
		return npLink.getLengthInKm();
	}

	/**
	 * sets the user-defined link length in km, which is assumed to be the valid one when the IP link is not coupled to a
	 * lightpath
	 * @param lengthInKm see above
	 */
	public void setLengthIfNotCoupledInKm(double lengthInKm)
	{
		npLink.setLengthInKm(lengthInKm);
	}

	/**
	 * Returns the link nominal capacity in Gbps. For IP links this is the nominal line rate of the underlying lightpath.
	 * For non-coupled IP links, this is a value user defined.
	 * @return see above
	 */
	public double getNominalCapacityGbps()
	{
		if (this.isCoupledtoLpRequest()) return getCoupledLpRequest().getLineRateGbps();
		if (this.isBundleOfIpLinks()) return getBundledIpLinks().stream().mapToDouble(e->e.getNominalCapacityGbps()).sum();
		final Double res = npLink.getAttributeAsDouble(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_NOMINALCAPACITYGBPS, null);
		if (res == null) return 0;
		return Math.max(0, res);
	}

	/**
	 * Sets the link nominal capacity in Gbps, a value used only when the link is not a bundle, and not coupled to a lightpath request. Negative values are truncated to zero.
	 * @return see above
	 */
	public void setNominalCapacityGbps(double capacityGbps)
	{
		npLink.setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_NOMINALCAPACITYGBPS, Math.max(capacityGbps, 0));
		if (!this.isBundleOfIpLinks() && !this.isCoupledtoLpRequest())
			npLink.setCapacity(Math.max(capacityGbps, 0));
	}

	/**
	 * Returns the current IP link capacity. In general, the nominal capacity if the link is up, or zero if the link is down
	 * @return see above
	 */
	public double getCurrentCapacityGbps()
	{
		if (this.isBundleOfIpLinks())
			return getBundledIpLinks().stream().mapToDouble(e->e.getCurrentCapacityGbps()).sum();
		return npLink.getCapacity();
	}

	/**
	 * Returns the current IP traffic carried by the IP link in Gbps.
	 * @return see above
	 */
	public double getCarriedTrafficGbps()
	{
		return npLink.getOccupiedCapacity();
	} // not the carried traffic, since service chains can compress/decompress traffic

	/**
	 * Returns the current IP link utilization (occupied capacity vs. current capacity)
	 * @return see above
	 */
	public double getCurrentUtilization()
	{
		final double occup = this.getCarriedTrafficGbps();
		final double cap = this.getCurrentCapacityGbps();
		if (occup < Configuration.precisionFactor) return 0;
		if (cap < Configuration.precisionFactor) return Double.MAX_VALUE;
		return occup / cap;
	} 


	/**
	 * Couples this IP link to the provided lightpath request. The lightpath line rate and the IP link nominal rate must be
	 * the same. If the link is already coupled, an exception is thrown
	 * @param lpReq see above
	 */
	public void coupleToLightpathRequest(WLightpathRequest lpReq)
	{
		if (this.isBundleOfIpLinks()) throw new Net2PlanException("The IP link is a bundle");
		if (this.isCoupledtoLpRequest()) throw new Net2PlanException("The IP link is already coupled. Please decouple it first");
		if (this.getNominalCapacityGbps() != lpReq.getLineRateGbps()) throw new Net2PlanException("Cannot couple an IP link with a different line rate of the lightpath request");
		npLink.coupleToLowerLayerDemand(lpReq.getNe());
		this.updateNetPlanObjectAndPropagateUpwards();
	}

	/**
	 * Decouples this IP link, if it is coupled to a lightpath
	 * 
	 */
	public void decoupleFromLightpathRequest ()
	{
		if (this.isBundleOfIpLinks()) throw new Net2PlanException ("This IP link is a bundle. To unbundle use unbundle method");
		if (!npLink.isCoupled()) return;
		npLink.getCoupledDemand().decouple();
		npLink.setCapacity(getNominalCapacityGbps());
		this.updateNetPlanObjectAndPropagateUpwards();
	}

	/** Returns true if this IP link is a member of a bundle
	 * @return see above
	 */
	public boolean isBundleMember () 
	{
		if (getNe().getTraversingRoutes().size() != 1) return false;
		return getNe().getTraversingRoutes().first().getDemand().hasTag(WNetConstants.TAGDEMANDIP_INDICATIONISBUNDLE);
	}
	
	/**
	 * If this IP link is a bundle, it removes the bundling, so previous bundle members are now regular IP links. 
	 * It makes all this in this link and its opposite link 
	 * 
	 */
	public void unbundleBidirectional ()
	{
		if (!this.isBundleOfIpLinks()) throw new Net2PlanException ("This IP link is not a bundle.");
		assert this.getBidirectionalPair().isBundleOfIpLinks();
		getNe().getBidirectionalPair().getCoupledDemand().remove();
		getNe().getCoupledDemand().remove();
		npLink.setCapacity(getNominalCapacityGbps());
		npLink.getBidirectionalPair().setCapacity(getNominalCapacityGbps());
		this.getBidirectionalPair().updateNetPlanObjectAndPropagateUpwards();
		this.updateNetPlanObjectAndPropagateUpwards();
	}

	/**
	 * Indicates if this IP link is coupled to a lighptath
	 * @return see above
	 */
	public boolean isCoupledtoLpRequest()
	{
		return npLink.isCoupledInDifferentLayer();
	}

	/**
	 * Returns the lightpath that this IP link is coupled to, or raises an exception if this IP link is not coupled
	 * @return see above
	 */
	public WLightpathRequest getCoupledLpRequest()
	{
		if (!isCoupledtoLpRequest()) throw new Net2PlanException("Not coupled");
		return new WLightpathRequest(npLink.getCoupledDemand());
	}

	/**
	 * Returns the length of this link. If the link is NOT coupled, this is the user-defined length. If it is coupled, the
	 * length is the physical length of fibers traversed by the lightpath carrying this link. If the lightpath is 1+1
	 * protected, the longest path length is returned.
	 * @return see above
	 */
	public double getWorstCaseLengthInKm()
	{
		if (this.isCoupledtoLpRequest()) return getCoupledLpRequest().getWorstCaseLengthInKm();
		if (this.isBundleOfIpLinks()) return getBundledIpLinks().stream().mapToDouble(e->e.getWorstCaseLengthInKm()).max().orElse(Double.MAX_VALUE);
		return getLengthIfNotCoupledInKm();
	}

	/**
	 * Returns the propagation delay in ms of this link. If the link is NOT coupled, this is the user-defined length divided
	 * by the user defined propagatio speed. If it is coupled, the length is computed from the physical propagation delay of
	 * the fibers traversed by the lightpath carrying this link. If the lightpath is 1+1 protected, the longest path
	 * propagation delay is returned.
	 * @return see above
	 */
	public double getWorstCasePropagationDelayInMs()
	{
		if (this.isCoupledtoLpRequest()) return getCoupledLpRequest().getWorstCasePropagationDelayMs();
		if (this.isBundleOfIpLinks()) return getBundledIpLinks().stream().mapToDouble(e->e.getWorstCasePropagationDelayInMs()).max().orElse(Double.MAX_VALUE);
		return npLink.getPropagationDelayInMs();
	}

	@Override
	public String toString()
	{
		return "IpLink(" + getNominalCapacityGbps() + "G) " + getA().getName() + "->" + getB().getName();
	}

	public void removeBidirectional()
	{
		if (this.isCoupledtoLpRequest()) { this.getBidirectionalPair().decoupleFromLightpathRequest(); this.decoupleFromLightpathRequest(); }
		if (this.isBundleOfIpLinks()) { this.unbundleBidirectional(); }

		final Link ee = getNe().getBidirectionalPair();
		getNe().remove();
		ee.remove();
	}

	public WIpLink getBundleParentIfMember ()
	{
		if (!this.isBundleMember()) throw new Net2PlanException ("This IP link is not a bundle member");
		return new WIpLink (getNe().getTraversingRoutes().first().getDemand().getCoupledLink());
	}
	
	/** Returns the service chains traversing this IP links
	 * @return see above
	 */
	public SortedSet<WServiceChain> getTraversingServiceChains ()
	{
		if (this.isBundleMember()) return new TreeSet<> (); 
		return getNe().getTraversingRoutes().stream().
				filter(d->{ WTYPE t = getNet().getWType(d).orElse(null); return t == null? false : t.isServiceChain(); }).
				map(r->new WServiceChain(r)).collect(Collectors.toCollection(TreeSet::new));
	}

	/** Returns the IP connections traversing this IP links
	 * @return see above
	 */
	public SortedSet<WIpSourceRoutedConnection> getTraversingIpConnections ()
	{
		if (this.isBundleMember()) return new TreeSet<> (); 
		return getNe().getTraversingRoutes().stream().
				filter(d->{ WTYPE t = getNet().getWType(d).orElse(null); return t == null? false : t.isIpSourceRoutedConnection(); }).
				map(r->new WIpSourceRoutedConnection(r)).collect(Collectors.toCollection(TreeSet::new));
	}

	/** Returns the traversing IP unicast demands 
	 * @return see above
	 */
	public SortedSet<WIpUnicastDemand> getTraversingIpUnicastDemands ()
	{
		if (this.isBundleMember()) return new TreeSet<> (); 
		final Triple<SortedSet<Demand>,SortedSet<Demand>,SortedSet<MulticastDemand>> travInfo = getNe().getDemandsPotentiallyTraversingThisLink();
		final SortedSet<Demand> demands = new TreeSet<> ();
		demands.addAll(travInfo.getFirst());
		demands.addAll(travInfo.getSecond());
		return demands.stream().filter(r->getNet().getWElement(r).isPresent()).filter(r->getNet().getWElement(r).get().isWIpUnicastDemand()).map(r->new WIpUnicastDemand(r)).collect(Collectors.toCollection(TreeSet::new));
	}

	public boolean isBundleOfIpLinks () { return getNe().isCoupledInSameLayer(); }
	
	public boolean isIpLinkCoupledToLightpath () { return getNe().isCoupledInDifferentLayer(); }
	
	public SortedSet<WIpLink> getBundledIpLinks ()
	{
		if (!this.isBundleOfIpLinks()) throw new Net2PlanException ("This IP link is not a bundle");
		final SortedSet<WIpLink> res = new TreeSet<> ();
		for (Route r : getNe ().getCoupledDemand().getRoutes())
		{
			if (r.getSeqLinks().size() != 1) throw new RuntimeException ();
			res.add(new WIpLink(r.getSeqLinks().get(0)));
		}
		return res;
	}
	
	public void setIpLinkAsBundleOfIpLinksBidirectional (SortedSet<WIpLink> ipLinksToBundleAb)
	{
		if (!getNet().isWithIpLayer()) throw new Net2PlanException ("The design has no IP layer");
		
		if (!this.isBidirectional()) throw new Net2PlanException ("All IP link must be bidirectional");
		if (this.isBundleMember() || this.isBundleOfIpLinks()) throw new Net2PlanException ("This elements cannot be a bundle nor a bundle member");
		if (this.getBidirectionalPair().isBundleMember() || this.getBidirectionalPair().isBundleOfIpLinks()) throw new Net2PlanException ("This elements cannot be a bundle nor a bundle member");
		if (ipLinksToBundleAb.stream().anyMatch(e->!e.isBidirectional())) throw new Net2PlanException ("All IP links must be bidirectional");
		final SortedSet<WIpLink> ipLinksToBundleBa = ipLinksToBundleAb.stream().map(e->e.getBidirectionalPair()).collect(Collectors.toCollection(TreeSet::new));
		final SortedSet<WIpLink> ipLinksToBundleAbBa = new TreeSet<> (ipLinksToBundleAb);
		ipLinksToBundleAbBa.addAll(ipLinksToBundleBa);
		if (ipLinksToBundleAb.stream().anyMatch(e->!e.getA().equals(this.getA()))) throw new Net2PlanException ("All IP links to bundle must have the same end nodes");
		if (ipLinksToBundleAb.stream().anyMatch(e->!e.getB().equals(this.getB()))) throw new Net2PlanException ("All IP links to bundle must have the same end nodes");
		if (ipLinksToBundleAbBa.stream().anyMatch(e->e.isBundleOfIpLinks())) throw new Net2PlanException ("All IP links to bundle cannot be themselves bundles");
		if (ipLinksToBundleAbBa.stream().anyMatch(e->e.isBundleMember())) throw new Net2PlanException ("All IP links to bundle cannot be bundle members already");
		if (ipLinksToBundleAbBa.stream().anyMatch(e->!e.getTraversingServiceChains().isEmpty())) throw new Net2PlanException ("The IP links to bundle cannot have service chains");
		if (ipLinksToBundleAbBa.stream().anyMatch(e->!e.getTraversingIpConnections().isEmpty())) throw new Net2PlanException ("The IP links to bundle cannot have IP connections");
		if (ipLinksToBundleAbBa.stream().anyMatch(e->!e.getTraversingIpUnicastDemands().isEmpty())) throw new Net2PlanException ("The IP links to bundle cannot have traversing IP demands");
		
		final Pair<Demand,Demand> demandToCreate = getNet().getNe().addDemandBidirectional(getA().getNe (), getB().getNe (), 0, RoutingType.SOURCE_ROUTING, null, getNet().getIpLayer().get().getNe());
		demandToCreate.getFirst().addTag(WNetConstants.TAGDEMANDIP_INDICATIONISBUNDLE);
		demandToCreate.getSecond().addTag(WNetConstants.TAGDEMANDIP_INDICATIONISBUNDLE);
		for (WIpLink memberAb : ipLinksToBundleAb)
		{
			final Route rAb = getNet().getNe().addRoute(demandToCreate.getFirst(), 0, 0, Arrays.asList(memberAb.getNe()), null);
			final Route rBa = getNet().getNe().addRoute(demandToCreate.getSecond(), 0, 0, Arrays.asList(memberAb.getBidirectionalPair().getNe()), null);
			rAb.setBidirectionalPair(rBa);
		}
		demandToCreate.getFirst().coupleToUpperOrSameLayerLink(this.getNe());
		demandToCreate.getSecond().coupleToUpperOrSameLayerLink(this.getBidirectionalPair().getNe());
		ipLinksToBundleAbBa.forEach(e->e.updateNetPlanObjectAndPropagateUpwards());
		assert ipLinksToBundleAbBa.stream().allMatch(e->e.isBundleMember());
		assert ipLinksToBundleAb.stream().allMatch(e->e.getBundleParentIfMember().equals(this));
		assert ipLinksToBundleBa.stream().allMatch(e->e.getBundleParentIfMember().equals(this.getBidirectionalPair()));
	}

	void updateNetPlanObjectAndPropagateUpwards ()
	{
		if (this.isBundleMember())
		{
			this.getBundleParentIfMember().updateNetPlanObjectAndPropagateUpwards();
		} else if (this.isBundleOfIpLinks())
		{
			for (WIpLink e : this.getBundledIpLinks())
			{
				assert e.getNe().getTraversingRoutes().size() == 1;
				final Route r = e.getNe().getTraversingRoutes().first();
				r.setCarriedTraffic(e.getNe().getCapacity(), e.getNe().getCapacity());
			}
			for (WServiceChain sc : this.getTraversingServiceChains())
				sc.updateNetPlanObjectAndPropagateUpwards();
		} else
		{
			// regular IP link, not member, not bundle
			for (WServiceChain sc : this.getTraversingServiceChains())
				sc.updateNetPlanObjectAndPropagateUpwards();
		}
	}

	/** Sets this IP link as failed
	 */
	public void setAsDown () 
	{
		getNe().setFailureState(false);
		updateNetPlanObjectAndPropagateUpwards();
	}

	/** Sets this IP link as non-failed (up). Note that if the IP link is coupled to a lightpath, and the lightpath is down, 
	 * the IP link will still be down
	 */
	public void setAsUp () 
	{
		getNe().setFailureState(true);
		updateNetPlanObjectAndPropagateUpwards();
	}

	/** Returns in this link is down: failed, or coupled to a lightpath that is failed, or with zero capacity
	 * @return see above
	 */
	public boolean isDown () 
	{
		return getNe().isDown() || getNe().getCapacity() < Configuration.precisionFactor;
	}
	
	/** the opposite to isDown
	 * @return see above
	 */
	public boolean isUp () 
	{
		return !this.isDown();
	}
	
	@Override
	void checkConsistency()
	{
		if (this.wasRemoved()) return;
		assert getA().getOutgoingIpLinks().contains(this);
		assert getB().getIncomingIpLinks().contains(this);
		if (this.isBidirectional()) assert this.getBidirectionalPair().getBidirectionalPair().equals(this);
		if (this.isBundleOfIpLinks()) assert getBundledIpLinks().stream().allMatch(e->e.getBundleParentIfMember().equals(this));
		if (this.isBundleOfIpLinks()) assert !this.isCoupledtoLpRequest();
		if (this.isBundleMember()) assert getBundleParentIfMember().getBundledIpLinks().contains(this);
		if (this.isBundleMember()) assert getTraversingServiceChains().isEmpty();
		if (this.isCoupledtoLpRequest()) assert getCoupledLpRequest().getCoupledIpLink().equals(this);
		if (this.isUp()) assert getCurrentCapacityGbps() > Configuration.precisionFactor;
		if (this.isUp() && this.isCoupledtoLpRequest()) assert !this.getCoupledLpRequest().isBlocked();
	}

	@Override
	public WTYPE getWType() { return WTYPE.WIpLink; }

}
