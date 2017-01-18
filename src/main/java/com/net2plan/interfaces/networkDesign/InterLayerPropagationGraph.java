package com.net2plan.interfaces.networkDesign;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;

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
		public boolean equals (Object o) 
		{ 
			if (!(o instanceof IPGNode)) return false;
			final IPGNode oCast = (IPGNode) o;
			if (this.d != oCast.d) return false;
			if (this.e != oCast.e) return false;
			if ((this.mdn == null) != (oCast.mdn == null)) return false;
			if (this.mdn != null)
			{
				if (this.mdn.getFirst() != oCast.mdn.getFirst()) return false;
				if (this.mdn.getSecond() != oCast.mdn.getSecond()) return false;
			}
			return true;
		}
		boolean isLink () { return e != null; }
		boolean isDemand () { return d != null; }
		boolean isMulticastFlow () { return mdn != null; }
		Demand getDemand () { return d; }
		Link getLink () { return e;}
		Pair<MulticastDemand,Node> getMulticastDemandAndNode () { return mdn; }
	}
	class IPGLink
	{
//		boolean carriesBackupTrafficOfGraphSource;
//		boolean carriesPrimaryTrafficOfGraphSource;
		IPGLink()
		{
//			this.carriesBackupTrafficOfGraphSource = false;
//			this.carriesPrimaryTrafficOfGraphSource = false;
		}
//		IPGLink(boolean carriesBackupTrafficOfGraphSource, boolean carriesPrimaryTrafficOfGraphSource) 
//		{
////			this.carriesBackupTrafficOfGraphSource = carriesBackupTrafficOfGraphSource;
////			this.carriesPrimaryTrafficOfGraphSource = carriesPrimaryTrafficOfGraphSource;
//		}
		
	}

	private final boolean assumeNonFailureState;
	private final Set<IPGNode> initialIPGNodes;
	private Map<Demand,IPGNode> demandNodesMap;
	private Map<Link,IPGNode> linkNodesMap;
	private Map<Pair<MulticastDemand,Node>,IPGNode> mDemandAndNodeNodesMap;
	private final boolean upWardsTrueDownwardsFalse;
	private final DirectedAcyclicGraph<IPGNode, IPGLink> interLayerCoupling;
	public InterLayerPropagationGraph (Set<Demand> initialDemands , Set<Link> initialLinks , Set<Pair<MulticastDemand,Node>> initialMDemands , boolean upWards , boolean assumeNonFailureState)
	{
		this.assumeNonFailureState = assumeNonFailureState;
		this.interLayerCoupling  = new DirectedAcyclicGraph<IPGNode, IPGLink>(IPGLink.class);
		this.demandNodesMap = new HashMap<> ();
		this.linkNodesMap = new HashMap<> ();
		this.mDemandAndNodeNodesMap = new HashMap<> ();
		this.initialIPGNodes = Sets.newHashSet();
		if (initialDemands != null)
			for (Demand d : initialDemands)
			{
				final IPGNode initialNode = new IPGNode (d , null , null);	
				this.initialIPGNodes.add(initialNode);
				this.interLayerCoupling.addVertex(initialNode);
				this.demandNodesMap.put(d ,  initialNode);
			}
		if (initialLinks != null)
			for (Link e : initialLinks)
			{
				final IPGNode initialNode = new IPGNode (null , e , null);	
				this.initialIPGNodes.add(initialNode);
				this.interLayerCoupling.addVertex(initialNode);
				this.linkNodesMap.put(e ,  initialNode);
			}
		if (initialMDemands != null)
			for (Pair<MulticastDemand,Node> m : initialMDemands)
			{
				final IPGNode initialNode = new IPGNode (null , null , m);	
				this.initialIPGNodes.add(initialNode);
				this.interLayerCoupling.addVertex(initialNode);
				this.mDemandAndNodeNodesMap.put(m ,  initialNode);
			}
		this.upWardsTrueDownwardsFalse = upWards;
		for (IPGNode initialNode : this.initialIPGNodes)
		{
			if (upWardsTrueDownwardsFalse) addVertexAndEdgesToGraphFromInitialIPGUpwards (initialNode);
			else addVertexAndEdgesToGraphFromInitialIPGDownwards (initialNode);
		}	
	}
	private void addVertexAndEdgesToGraphFromInitialIPGDownwards (IPGNode initialNode)
	{
		if (initialNode.isLink())
		{
			final Link e = initialNode.getLink();
			if (!e.isCoupled()) return;
			final Demand downGraphNodeLink = e.getCoupledDemand();
			final Pair<IPGNode,Boolean> downIPGNodeInfo = getExistingGraphNodeOrCreateOneIfNotExistsAndAddToGraph(downGraphNodeLink);
			final IPGNode downIPGNode = downIPGNodeInfo.getFirst();
			final boolean ipgWasNewlyCreated = downIPGNodeInfo.getSecond();
			final IPGLink downLink = new IPGLink ();
			try { this.interLayerCoupling.addDagEdge(initialNode , downIPGNode , downLink); } catch (CycleFoundException ex) { throw new RuntimeException(ex.getMessage()); }
			if (ipgWasNewlyCreated)	addVertexAndEdgesToGraphFromInitialIPGDownwards(downIPGNode);
		}
		else if (initialNode.isDemand())
		{
			aquiiiii
			final Demand d = initialNode.getDemand();
			Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> thisLayerLinksTraversingSameTrafficInfo = 
					e.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  (this.assumeNonFailureState);
			final Set<Demand> demandsPuttingPrimaryOrBackupTraffic = Sets.union(thisLayerLinksTraversingSameTrafficInfo.getFirst().keySet() , thisLayerLinksTraversingSameTrafficInfo.getSecond().keySet());
			final Set<Pair<MulticastDemand,Node>> mDemandsAndNodesPuttingTraffic = thisLayerLinksTraversingSameTrafficInfo.getThird().keySet();
			for (Demand upGraphNodeDemand : demandsPuttingPrimaryOrBackupTraffic)
			{
				final Pair<IPGNode,Boolean> upIPGNodeInfo = getExistingGraphNodeOrCreateOneIfNotExistsAndAddToGraph(upGraphNodeDemand);
				final IPGNode upIPGNode = upIPGNodeInfo.getFirst();
				final boolean ipgWasNewlyCreated = upIPGNodeInfo.getSecond();
				final IPGLink upLink = new IPGLink ();
				try { this.interLayerCoupling.addDagEdge(initialNode , upIPGNode , upLink); } catch (CycleFoundException ex) { throw new RuntimeException(ex.getMessage()); }
				if (ipgWasNewlyCreated)	addVertexAndEdgesToGraphFromInitialIPGDownwards(upIPGNode);
			}
			for (Pair<MulticastDemand,Node> upGraphNodeMulticastDemand : mDemandsAndNodesPuttingTraffic)
			{
				final Pair<IPGNode,Boolean> upIPGNodeInfo = getExistingGraphNodeOrCreateOneIfNotExistsAndAddToGraph(upGraphNodeMulticastDemand);
				final IPGNode upIPGNode = upIPGNodeInfo.getFirst();
				final boolean ipgWasNewlyCreated = upIPGNodeInfo.getSecond();
				final IPGLink upLink = new IPGLink ();
				try { this.interLayerCoupling.addDagEdge(initialNode , upIPGNode , upLink); } catch (CycleFoundException ex) { throw new RuntimeException(ex.getMessage()); }
				if (ipgWasNewlyCreated)	addVertexAndEdgesToGraphFromInitialIPGDownwards(upIPGNode);
			}
		}
		else if (initialNode.isMulticastFlow())
		{
			final Pair<MulticastDemand,Node> mPair = initialNode.getMulticastDemandAndNode();
			if (!mPair.getFirst().isCoupled()) return;
			final Link upGraphNodeLink = mPair.getFirst().getCoupledLinks().stream().filter (e->e.getDestinationNode() == mPair.getSecond()).findFirst().get();
			final Pair<IPGNode,Boolean> upIPGNodeInfo = getExistingGraphNodeOrCreateOneIfNotExistsAndAddToGraph(upGraphNodeLink);
			final IPGNode upIPGNode = upIPGNodeInfo.getFirst();
			final boolean ipgWasNewlyCreated = upIPGNodeInfo.getSecond();
			final IPGLink upLink = new IPGLink ();
			try { this.interLayerCoupling.addDagEdge(initialNode , upIPGNode , upLink); } catch (CycleFoundException e) { throw new RuntimeException(e.getMessage()); }
			if (ipgWasNewlyCreated)	addVertexAndEdgesToGraphFromInitialIPGDownwards(upIPGNode);
		}
		else throw new RuntimeException();
	}
	private void addVertexAndEdgesToGraphFromInitialIPGUpwards (IPGNode initialNode) throws RuntimeException
	{
		if (initialNode.isDemand())
		{
			final Demand d = initialNode.getDemand();
			if (!d.isCoupled()) return;
			final Link upGraphNodeLink = d.getCoupledLink();
			final Pair<IPGNode,Boolean> upIPGNodeInfo = getExistingGraphNodeOrCreateOneIfNotExistsAndAddToGraph(upGraphNodeLink);
			final IPGNode upIPGNode = upIPGNodeInfo.getFirst();
			final boolean ipgWasNewlyCreated = upIPGNodeInfo.getSecond();
			final IPGLink upLink = new IPGLink ();
			try { this.interLayerCoupling.addDagEdge(initialNode , upIPGNode , upLink); } catch (CycleFoundException e) { throw new RuntimeException(e.getMessage()); }
			if (ipgWasNewlyCreated)	addVertexAndEdgesToGraphFromInitialIPGDownwards(upIPGNode);
		}
		else if (initialNode.isLink())
		{
			final Link e = initialNode.getLink();
			Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> thisLayerLinksTraversingSameTrafficInfo = 
					e.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  (this.assumeNonFailureState);
			final Set<Demand> demandsPuttingPrimaryOrBackupTraffic = Sets.union(thisLayerLinksTraversingSameTrafficInfo.getFirst().keySet() , thisLayerLinksTraversingSameTrafficInfo.getSecond().keySet());
			final Set<Pair<MulticastDemand,Node>> mDemandsAndNodesPuttingTraffic = thisLayerLinksTraversingSameTrafficInfo.getThird().keySet();
			for (Demand upGraphNodeDemand : demandsPuttingPrimaryOrBackupTraffic)
			{
				final Pair<IPGNode,Boolean> upIPGNodeInfo = getExistingGraphNodeOrCreateOneIfNotExistsAndAddToGraph(upGraphNodeDemand);
				final IPGNode upIPGNode = upIPGNodeInfo.getFirst();
				final boolean ipgWasNewlyCreated = upIPGNodeInfo.getSecond();
				final IPGLink upLink = new IPGLink ();
				try { this.interLayerCoupling.addDagEdge(initialNode , upIPGNode , upLink); } catch (CycleFoundException ex) { throw new RuntimeException(ex.getMessage()); }
				if (ipgWasNewlyCreated)	addVertexAndEdgesToGraphFromInitialIPGDownwards(upIPGNode);
			}
			for (Pair<MulticastDemand,Node> upGraphNodeMulticastDemand : mDemandsAndNodesPuttingTraffic)
			{
				final Pair<IPGNode,Boolean> upIPGNodeInfo = getExistingGraphNodeOrCreateOneIfNotExistsAndAddToGraph(upGraphNodeMulticastDemand);
				final IPGNode upIPGNode = upIPGNodeInfo.getFirst();
				final boolean ipgWasNewlyCreated = upIPGNodeInfo.getSecond();
				final IPGLink upLink = new IPGLink ();
				try { this.interLayerCoupling.addDagEdge(initialNode , upIPGNode , upLink); } catch (CycleFoundException ex) { throw new RuntimeException(ex.getMessage()); }
				if (ipgWasNewlyCreated)	addVertexAndEdgesToGraphFromInitialIPGDownwards(upIPGNode);
			}
		}
		else if (initialNode.isMulticastFlow())
		{
			final Pair<MulticastDemand,Node> mPair = initialNode.getMulticastDemandAndNode();
			if (!mPair.getFirst().isCoupled()) return;
			final Link upGraphNodeLink = mPair.getFirst().getCoupledLinks().stream().filter (e->e.getDestinationNode() == mPair.getSecond()).findFirst().get();
			final Pair<IPGNode,Boolean> upIPGNodeInfo = getExistingGraphNodeOrCreateOneIfNotExistsAndAddToGraph(upGraphNodeLink);
			final IPGNode upIPGNode = upIPGNodeInfo.getFirst();
			final boolean ipgWasNewlyCreated = upIPGNodeInfo.getSecond();
			final IPGLink upLink = new IPGLink ();
			try { this.interLayerCoupling.addDagEdge(initialNode , upIPGNode , upLink); } catch (CycleFoundException e) { throw new RuntimeException(e.getMessage()); }
			if (ipgWasNewlyCreated)	addVertexAndEdgesToGraphFromInitialIPGDownwards(upIPGNode);
		}
		else throw new RuntimeException();
	}
	
	private Pair<IPGNode,Boolean> getExistingGraphNodeOrCreateOneIfNotExistsAndAddToGraph (Link e)
	{
		IPGNode ipgNode = linkNodesMap.get(e);
		final boolean newlyCreatedIPG = (ipgNode == null); 
		if (newlyCreatedIPG)
		{
			ipgNode = new IPGNode (null , e , null);
			linkNodesMap.put(e , ipgNode);
			this.interLayerCoupling.addVertex(ipgNode);
		}
		return Pair.of(ipgNode,newlyCreatedIPG);
		
	}
	private Pair<IPGNode,Boolean> getExistingGraphNodeOrCreateOneIfNotExistsAndAddToGraph (Demand d)
	{
		IPGNode ipgNode = demandNodesMap.get(d);
		final boolean newlyCreatedIPG = (ipgNode == null); 
		if (newlyCreatedIPG)
		{
			ipgNode = new IPGNode (d , null, null);
			demandNodesMap.put(d , ipgNode);
			this.interLayerCoupling.addVertex(ipgNode);
		}
		return Pair.of(ipgNode,newlyCreatedIPG);
	}
	private Pair<IPGNode,Boolean> getExistingGraphNodeOrCreateOneIfNotExistsAndAddToGraph (Pair<MulticastDemand,Node> m)
	{
		IPGNode ipgNode = mDemandAndNodeNodesMap.get(m);
		final boolean newlyCreatedIPG = (ipgNode == null); 
		if (newlyCreatedIPG)
		{
			ipgNode = new IPGNode (null , null, m);
			mDemandAndNodeNodesMap.put(m , ipgNode);
			this.interLayerCoupling.addVertex(ipgNode);
		}
		return Pair.of(ipgNode,newlyCreatedIPG);
	}

}
