/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jgrapht.graph.DirectedAcyclicGraph;

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.niw.WNetConstants.WTYPE;
import com.net2plan.utils.Constants.RoutingType;

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

	/**
	 * Adds a source routed IP connection to this unicast demand, in the case that it is of the source routed type
	 * @param sequenceOfIpLinks the sequence of IP links traversed
	 * @param carriedTrafficGbpsInNonFailureState the traffic to inject in the non failure state
	 * @return the IP source routed connection object
	 */
	public WIpSourceRoutedConnection addIpSourceRoutedConnection (List<WIpLink> sequenceOfIpLinks, double carriedTrafficGbpsInNonFailureState)
	{
		if (this.isIpHopByHopRouted()) throw new Net2PlanException ("The IP unicast demand is hop-by-hop routed: no IP source routed connection can be added");
		final Route r = getNe().getNetPlan().addRoute(this.getNe(), carriedTrafficGbpsInNonFailureState, carriedTrafficGbpsInNonFailureState, sequenceOfIpLinks.stream().map(e->e.getNe()).collect(Collectors.toList()) , null);
		return new WIpSourceRoutedConnection(r);
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
	
	/** Indicates if this demand is tagged to be routed via source-routing (e.g. via an IP source routed connection)
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

	/** Sets this IP demand to be routed via source-routing (e.g. IP source routed connection)
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
	public WNode getA ()
	{
		return new WNode (getNe().getIngressNode());
	}

	/**
	 * Returns the destination node of this IP demand
	 * @return see above
	 */
	public WNode getB ()
	{
		return new WNode (getNe().getEgressNode());
	}

	
	/** Returns the IP connections realizing this demand. If the demand is hop-by-hop routed, an empty returns an empty set
	 * @return see above
	 */
	public SortedSet<WIpSourceRoutedConnection> getIpConnections ()
	{
		if (this.isIpHopByHopRouted()) return new TreeSet<> ();
		return getNe().getRoutes().stream().
				filter(d->{ WTYPE t = getNet().getWType(d).orElse(null); return t == null? false : t.isIpSourceRoutedConnection(); }).
				map(ee -> new WIpSourceRoutedConnection(ee)).collect(Collectors.toCollection(TreeSet::new));
	}

	@Override
	public String toString()
	{
		return "WIpUnicastDemand (" + this.getCurrentOfferedTrafficInGbps() + "G) " + getA() + "->" + getB();
	}

	@Override
	void checkConsistency()
	{
		if (this.wasRemoved()) return;
		if (this.isBidirectional()) assert this.getBidirectionalPair().get().getBidirectionalPair().get().equals(this);
		if (this.isBidirectional()) assert this.getBidirectionalPair().get().isDownstream() == this.isUpstream();
		assert !this.getNe().hasTag(WNetConstants.TAGDEMANDIP_INDICATIONISBUNDLE);
		assert this.getNe().getLayer().equals(getNet().getIpLayer().get().getNe());
	}

	/**
	 * Makes that this IP demand tagged as the opposite one to a given IP demand, and viceversa. The two IP demand must
	 * have opposite end nodes, one must be downstream and the other upstream. Any other opposite relation to other IP demand is released.
	 * @param d see above
	 */
	@Override
	public void setBidirectionalPair(WAbstractIpUnicastOrAnycastDemand d) 
	{
		if (d == null) throw new Net2PlanException("Please specify a not null IP demand");
		if (!(d instanceof WIpUnicastDemand)) throw new Net2PlanException("Please specify a not null IP demand");
		final WIpUnicastDemand dd = (WIpUnicastDemand) d;
		if (!this.getA().equals(dd.getB ())) throw new Net2PlanException("The origin of this IP demand must be the end node of the opposite, and viceversa");
		if (!dd.getA().equals(this.getB())) throw new Net2PlanException("The origin of this IP demand must be the end node of the opposite, and viceversa");
		if (this.isDownstream() == dd.isDownstream()) throw new Net2PlanException("One flow must be tagged as upstream, the other as downstream");
		removeBidirectionalPairRelation();
		getNe().setBidirectionalPair(d.getNe());
	}

	/**
	 * If this service chain request has an opposite request associated, removes such association. If not, makes nothing
	 * happens
	 */
	@Override
	public void removeBidirectionalPairRelation() 
	{
		if (!this.isBidirectional()) return;
		getNe().setBidirectionalPair(null);
	}

	/**
	 * Returns the opposite service chain request to this, if any
	 * @return see above
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

	@Override
	public double getWorstCaseEndtoEndLatencyMs() 
	{
		if (this.isIpSourceRouted()) return getIpConnections().stream().filter(e->e.isUp()).mapToDouble(e->e.getWorstCasePropgationLatencyInMs()).max().orElse(Double.MAX_VALUE);
		return computeWorstCaseE2eCost(e->e.getWorstCasePropagationDelayInMs(), Optional.empty());
	}

	@Override
	public double getWorstCaseEndtoEndLengthInKm() 
	{
		if (this.isIpSourceRouted()) return getIpConnections().stream().filter(e->e.isUp()).mapToDouble(e->e.getWorstCaseLengthInKm()).max().orElse(Double.MAX_VALUE);
		return computeWorstCaseE2eCost(e->e.getWorstCaseLengthInKm(), Optional.empty());
	}

    private double computeWorstCaseE2eCost (Function<WIpLink,Double> costFunction , Optional<SortedMap<WNode,SortedSet<WIpLink>>> optinalOutFrs)
    {
    	final WNode ingressNode = this.getA();
    	final WNode egressNode = this.getB();
        final SortedMap<WNode,WIpLink> inNodesAndPrevLinkInLongestPath_latLengthHops = new TreeMap<> ();
        final BiFunction<WNode , Function<WIpLink,Double> , Double> longestPathSoFarIngressToNode = (n,cf) -> 
        {
            double accumCost = 0;
            int numHops = 0;
            WNode currentNode = n; 
            while (true)
            {
                final WIpLink prevLink = inNodesAndPrevLinkInLongestPath_latLengthHops.get(currentNode);
                if (prevLink == null) return Double.MAX_VALUE;
                accumCost += cf.apply(prevLink);
                numHops ++;
                currentNode = prevLink.getA();
                if (currentNode.equals(ingressNode)) break;
                if (numHops > inNodesAndPrevLinkInLongestPath_latLengthHops.size() + 2) throw new RuntimeException();
            }
            return accumCost;
        };
        
        final SortedMap<WNode,SortedSet<WIpLink>> outFrs;
        if (optinalOutFrs.isPresent()) outFrs = optinalOutFrs.get();
        else
        {
    		outFrs = new TreeMap<> ();
    		for (Entry<WIpLink,Double> entry : getTraversedIpLinksAndCarriedTraffic(true).entrySet())
    		{
    			if (entry.getValue() < Configuration.precisionFactor) continue;
    			final WIpLink e = entry.getKey();
    			final WNode a = e.getA();
    			SortedSet<WIpLink> links = outFrs.get(a);
    			if (links == null) { links = new TreeSet<> (); outFrs.put(a, links); }
    			links.add(e);
    		}
        }
        
        /* If the end node has out links, loop => infinite length */
        if (!outFrs.getOrDefault(egressNode , new TreeSet<> ()).isEmpty()) return Double.MAX_VALUE;
        
        final SortedSet<WNode> currentNodes = new TreeSet<> ();
        currentNodes.add(ingressNode);
        do
        {
            for (WNode n : new ArrayList<> (currentNodes))
            {
                if (n.equals(egressNode))
                {
                    final SortedSet<WIpLink> outFrsEgressNode = outFrs.get(n);
                    if (outFrsEgressNode == null) continue;
                    /* check if there are outgoing links of the egress node carrying traffic => cycle */
                    currentNodes.remove(egressNode);
                    if (currentNodes.isEmpty()) return longestPathSoFarIngressToNode.apply(egressNode, costFunction);
                    continue;
                }
                
                /* Usual loop */
                final WIpLink wcSoFarToCurrentNode = inNodesAndPrevLinkInLongestPath_latLengthHops.get(n);
                final SortedSet<WIpLink> outgoingFrsThisNode = outFrs.getOrDefault(n , new TreeSet<> ());
                if (outgoingFrsThisNode != null) 
                    for (WIpLink e : outgoingFrsThisNode)
                    {
                        final double thisPathCost = (wcSoFarToCurrentNode == null? 0 : longestPathSoFarIngressToNode.apply(n, costFunction)) + costFunction.apply(e);
                        final WNode nextNode = e.getB();
                        if (inNodesAndPrevLinkInLongestPath_latLengthHops.containsKey(nextNode))
                        {
                            if (thisPathCost > longestPathSoFarIngressToNode.apply(nextNode , costFunction))
                                inNodesAndPrevLinkInLongestPath_latLengthHops.put(nextNode, e); 
                        }
                        else
                            inNodesAndPrevLinkInLongestPath_latLengthHops.put(nextNode, e);
                        currentNodes.add(e.getB());
                    }
                currentNodes.remove(n);
            }
            
            if (currentNodes.isEmpty()) 
                return longestPathSoFarIngressToNode.apply(egressNode , costFunction);
            if ((currentNodes.size() == 1) && (currentNodes.iterator().next().equals(egressNode))) 
                return longestPathSoFarIngressToNode.apply(egressNode , costFunction);
        } while (true);
    }
	
	
}
