package com.net2plan.interfaces.networkDesign;

import java.util.Map;
import java.util.Set;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import com.google.common.collect.Sets;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

public class InterLayerPropagationGraph
{
	class IPGNode
	{
		final Demand d;
		final Link e;
		final Pair<MulticastDemand,Node> mdn;
		IPGNode(Demand d, Link e, Pair<MulticastDemand, Node> mdn)
		{
			this.d = d;
			this.e = e;
			this.mdn = mdn;
		}
		boolean isLink () { return e != null; }
		boolean isDemand () { return d != null; }
		boolean isMulticastFlow () { return mdn != null; }
		Demand getDemand () { return d; }
		Link getLink () { return e;}
		MulticastDemand getMulticastDemand () { return mdn.getFirst(); }
		Node getMDDestinationNode () { return mdn.getSecond(); }
	}
	class IPGLink
	{
		final boolean isBackup;
		IPGLink(boolean isBackup)
		{
			this.isBackup = isBackup;
		}
	}

	private Set<IPGNode> initialIPGNodes;
	private boolean upWardsTrueDownwardsFalse;
	private DirectedAcyclicGraph<IPGNode, IPGLink> interLayerCoupling;
	public InterLayerPropagationGraph (Demand initialDemand , boolean upWards , boolean assumeFailure)
	{
		this.initialIPGNodes = Sets.newHashSet(new IPGNode (initialDemand , null , null));
		this.upWardsTrueDownwardsFalse = upWards;
		this.interLayerCoupling  = new DirectedAcyclicGraph<IPGNode, IPGLink>(IPGLink.class);
		if (upWardsTrueDownwardsFalse) addVertexAndEdgesToGraphFromInitialIPGUpwards (initialIPGNodes.iterator().next());
		else addVertexAndEdgesToGraphFromInitialIPGDownwards (initialIPGNodes.iterator().next());
			
	}
	private void addVertexAndEdgesToGraphFromInitialIPGDownwards (IPGNode initialNode)
	{
		
	}
	private void addVertexAndEdgesToGraphFromInitialIPGUpwards (IPGNode initialNode)
	{
		if (initialNode.isDemand())
		{
			final Demand d = initialNode.getDemand();
			if (!d.isCoupled()) return;
			final IPGNode upIPGNode = new IPGNode (null , d.getCoupledLink() , null);
			this.interLayerCoupling.addEdge(initialNode , upIPGNode);
			addVertexAndEdgesToGraphFromInitialIPGDownwards(upIPGNode);
		}
		else if (initialNode.isLink())
		{
			final Link e = initialNode.getLink();
			Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> e.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  (boolean assumeNoFailureState)
			e.getDemandsCarryinTrafficHere ();
			if (!d.isCoupled()) return;
			final IPGNode upIPGNode = new IPGNode (null , d.getCoupledLink() , null);
			this.interLayerCoupling.addEdge(initialNode , upIPGNode);
			addVertexAndEdgesToGraphFromInitialIPGDownwards(upIPGNode);
			
		}
		else
		{
			
		}
			
		coupling_thisLayerPair = new DemandLinkMapping();
		boolean valid;
		try { valid = interLayerCoupling.addDagEdge(lowerLayer, upperLayer, coupling_thisLayerPair); }
		catch (DirectedAcyclicGraph.CycleFoundException ex) { valid = false; }
		if (valid) interLayerCoupling.removeEdge(lowerLayer, upperLayer);
		else return false;

	}

}
