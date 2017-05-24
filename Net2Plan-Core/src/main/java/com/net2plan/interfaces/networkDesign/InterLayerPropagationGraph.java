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
package com.net2plan.interfaces.networkDesign;

import com.google.common.collect.Sets;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class InterLayerPropagationGraph
{
	public class IPGNode
	{
		final Demand d;
		final Link e;
		final Pair<MulticastDemand,Node> mdn;
		IPGNode(Demand d, Link e, Pair<MulticastDemand, Node> mdn)
		{
			this.d = d;
			this.e = e;
			this.mdn = mdn;
			if (mdn != null) if (!mdn.getFirst().getEgressNodes().contains(mdn.getSecond())) throw new Net2PlanException ("Bad egress node");
		}
		boolean isLink () { return e != null; }
		boolean isDemand () { return d != null; }
		boolean isMulticastFlow () { return mdn != null; }
		Demand getDemand () { return d; }
		Link getLink () { return e;}
		Pair<MulticastDemand,Node> getMulticastDemandAndNode () { return mdn; }
		public String toString () { return "d: " + d  + ", e: " + e + ", MD: " + mdn; }
	}

	private final Set<IPGNode> initialIPGVertices;
	private Map<Demand,IPGNode> demand2IGPVertexMap;
	
	public Set<IPGNode> getInitialIPGVertices() 
	{
		return this.initialIPGVertices;
	}
	public Map<Demand, IPGNode> getDemand2IGPVertexMap() 
	{
		return this.demand2IGPVertexMap;
	}
	public Map<Link, IPGNode> getLink2IGPVertexMap() 
	{
		return this.link2IGPVertexMap;
	}
	public Map<Pair<MulticastDemand, Node>, IPGNode> getmDemandAndNode2VertexMap() 
	{
		return this.mDemandAndNode2VertexMap;
	}
	public boolean isUpWardsTrueDownwardsFalse() 
	{
		return this.upWardsTrueDownwardsFalse;
	}
	public DirectedAcyclicGraph<IPGNode, Object> getInterLayerPropagationGraph () { return this.interLayerPropagationGraph; }


	private Map<Link,IPGNode> link2IGPVertexMap;
	private Map<Pair<MulticastDemand,Node>,IPGNode> mDemandAndNode2VertexMap;
	private final boolean upWardsTrueDownwardsFalse;
	private final DirectedAcyclicGraph<IPGNode, Object> interLayerPropagationGraph;
	public InterLayerPropagationGraph (Set<Demand> initialDemands , Set<Link> initialLinks ,
			Set<Pair<MulticastDemand,Node>> initialMDemands , boolean upWards)
	{
		this.interLayerPropagationGraph  = new DirectedAcyclicGraph<IPGNode, Object>(Object.class);
		this.demand2IGPVertexMap = new HashMap<> ();
		this.link2IGPVertexMap = new HashMap<> ();
		this.mDemandAndNode2VertexMap = new HashMap<> ();
		this.initialIPGVertices = Sets.newHashSet();
		if (initialDemands != null)
			for (Demand d : initialDemands)
			{
				final IPGNode initialNode = new IPGNode (d , null , null);	
				this.initialIPGVertices.add(initialNode);
				this.interLayerPropagationGraph.addVertex(initialNode);
				this.demand2IGPVertexMap.put(d ,  initialNode);
			}
		if (initialLinks != null)
			for (Link e : initialLinks)
			{
				final IPGNode initialNode = new IPGNode (null , e , null);	
				this.initialIPGVertices.add(initialNode);
				this.interLayerPropagationGraph.addVertex(initialNode);
				this.link2IGPVertexMap.put(e ,  initialNode);
			}
		if (initialMDemands != null)
			for (Pair<MulticastDemand,Node> m : initialMDemands)
			{
				final IPGNode initialNode = new IPGNode (null , null , m);	
				this.initialIPGVertices.add(initialNode);
				this.interLayerPropagationGraph.addVertex(initialNode);
				this.mDemandAndNode2VertexMap.put(m ,  initialNode);
			}
		this.upWardsTrueDownwardsFalse = upWards;
		for (IPGNode initialNode : this.initialIPGVertices)
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
			final Demand downGraphNodeDemand = e.getCoupledDemand();
			final MulticastDemand downGraphNodeMulticastDemand = e.getCoupledMulticastDemand();
			if (downGraphNodeDemand != null)
				addEdgeAddingNewVertexAndPropagatingIfNeeded(downGraphNodeDemand, initialNode, this.upWardsTrueDownwardsFalse);
			else
				addEdgeAddingNewVertexAndPropagatingIfNeeded(Pair.of(downGraphNodeMulticastDemand , e.getDestinationNode()), initialNode, this.upWardsTrueDownwardsFalse);
		}
		else if (initialNode.isDemand())
		{
			final Demand d = initialNode.getDemand();
			Pair<Set<Link>,Set<Link>> thisLayerLinksTraversingSameTrafficInfo = d.getLinksThisLayerPotentiallyCarryingTraffic();
			for (Link downGraphNodeLink : Sets.union(thisLayerLinksTraversingSameTrafficInfo.getFirst(), thisLayerLinksTraversingSameTrafficInfo.getSecond()))
				addEdgeAddingNewVertexAndPropagatingIfNeeded(downGraphNodeLink, initialNode, this.upWardsTrueDownwardsFalse);
		}
		else if (initialNode.isMulticastFlow())
		{
			final Pair<MulticastDemand,Node> mPair = initialNode.getMulticastDemandAndNode();
			final Set<Link> downGraphNodeLinks = mPair.getFirst().getLinksThisLayerPotentiallyCarryingTraffic(mPair.getSecond());
			for (Link e : downGraphNodeLinks)
				addEdgeAddingNewVertexAndPropagatingIfNeeded(e, initialNode, this.upWardsTrueDownwardsFalse);
		}
		else throw new RuntimeException("initialNode: " + initialNode);
	}
	private void addVertexAndEdgesToGraphFromInitialIPGUpwards (IPGNode initialNode) throws RuntimeException
	{
		if (initialNode.isDemand())
		{
			final Demand d = initialNode.getDemand();
			if (!d.isCoupled()) return;
			final Link upGraphNodeLink = d.getCoupledLink();
			addEdgeAddingNewVertexAndPropagatingIfNeeded(upGraphNodeLink, initialNode, this.upWardsTrueDownwardsFalse);
		}
		else if (initialNode.isLink())
		{
			final Link e = initialNode.getLink();
			Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> thisLayerLinksTraversingSameTrafficInfo = 
					e.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  ();
			final Set<Demand> demandsPuttingPrimaryOrBackupTraffic = Sets.union(thisLayerLinksTraversingSameTrafficInfo.getFirst().keySet() , thisLayerLinksTraversingSameTrafficInfo.getSecond().keySet());
			final Set<Pair<MulticastDemand,Node>> mDemandsAndNodesPuttingTraffic = thisLayerLinksTraversingSameTrafficInfo.getThird().keySet();
			for (Demand upGraphNodeDemand : demandsPuttingPrimaryOrBackupTraffic)
				addEdgeAddingNewVertexAndPropagatingIfNeeded(upGraphNodeDemand, initialNode, this.upWardsTrueDownwardsFalse);
			for (Pair<MulticastDemand,Node> upGraphNodeMulticastDemand : mDemandsAndNodesPuttingTraffic)
				addEdgeAddingNewVertexAndPropagatingIfNeeded(upGraphNodeMulticastDemand, initialNode, this.upWardsTrueDownwardsFalse);
		}
		else if (initialNode.isMulticastFlow())
		{
			final Pair<MulticastDemand,Node> mPair = initialNode.getMulticastDemandAndNode();
			if (!mPair.getFirst().isCoupled()) return;
			final Link upGraphNodeLink = mPair.getFirst().getCoupledLinks().stream().filter (e->e.getDestinationNode() == mPair.getSecond()).findFirst().get();
			addEdgeAddingNewVertexAndPropagatingIfNeeded(upGraphNodeLink, initialNode, this.upWardsTrueDownwardsFalse);
		}
		else throw new RuntimeException();
	}
	

	
	private void addEdgeAddingNewVertexAndPropagatingIfNeeded (Link e , IPGNode edgeInitialNode , boolean propagateUpwards)
	{
		IPGNode ipgNode = link2IGPVertexMap.get(e);
		final boolean newlyCreatedIPG = (ipgNode == null); 
		if (newlyCreatedIPG)
		{
			ipgNode = new IPGNode (null , e , null);
			link2IGPVertexMap.put(e , ipgNode);
			this.interLayerPropagationGraph.addVertex(ipgNode);
		}
		try { this.interLayerPropagationGraph.addDagEdge(edgeInitialNode , ipgNode , new Object ()); } catch (CycleFoundException ex) { throw new RuntimeException(ex.getMessage()); }
		if (newlyCreatedIPG) 
			if (propagateUpwards) 
				addVertexAndEdgesToGraphFromInitialIPGUpwards(ipgNode);
			else
				addVertexAndEdgesToGraphFromInitialIPGDownwards(ipgNode);
	}
	private void addEdgeAddingNewVertexAndPropagatingIfNeeded (Demand d , IPGNode edgeInitialNode , boolean propagateUpwards)
	{
		IPGNode ipgNode = demand2IGPVertexMap.get(d);
		final boolean newlyCreatedIPG = (ipgNode == null); 
		if (newlyCreatedIPG)
		{
			ipgNode = new IPGNode (d , null, null);
			demand2IGPVertexMap.put(d , ipgNode);
			this.interLayerPropagationGraph.addVertex(ipgNode);
		}
		try { this.interLayerPropagationGraph.addDagEdge(edgeInitialNode , ipgNode , new Object ()); } catch (CycleFoundException ex) { throw new RuntimeException(ex.getMessage()); }
		if (newlyCreatedIPG) 
			if (propagateUpwards) 
				addVertexAndEdgesToGraphFromInitialIPGUpwards(ipgNode);
			else
				addVertexAndEdgesToGraphFromInitialIPGDownwards(ipgNode);
	}
	private void addEdgeAddingNewVertexAndPropagatingIfNeeded (Pair<MulticastDemand,Node> m , IPGNode edgeInitialNode , boolean propagateUpwards)
	{
		IPGNode ipgNode = mDemandAndNode2VertexMap.get(m);
		final boolean newlyCreatedIPG = (ipgNode == null); 
		if (newlyCreatedIPG)
		{
			ipgNode = new IPGNode (null , null, m);
			mDemandAndNode2VertexMap.put(m , ipgNode);
			this.interLayerPropagationGraph.addVertex(ipgNode);
		}
		try { this.interLayerPropagationGraph.addDagEdge(edgeInitialNode , ipgNode , new Object ()); } catch (CycleFoundException ex) { throw new RuntimeException(ex.getMessage()); }
		if (newlyCreatedIPG) 
			if (propagateUpwards) 
				addVertexAndEdgesToGraphFromInitialIPGUpwards(ipgNode);
			else
				addVertexAndEdgesToGraphFromInitialIPGDownwards(ipgNode);
	}

	public Set<Link> getLinksInGraph ()
	{
		return interLayerPropagationGraph.vertexSet().stream().filter(e->e.isLink()).map(e->e.getLink()).collect(Collectors.toSet());
	}
	public Set<Demand> getDemandsInGraph ()
	{
		return interLayerPropagationGraph.vertexSet().stream().filter(e->e.isDemand()).map(e->e.getDemand()).collect(Collectors.toSet());
	}
	public Set<Pair<MulticastDemand,Node>> getMulticastDemandFlowsInGraph ()
	{
		return interLayerPropagationGraph.vertexSet().stream().filter(e->e.isMulticastFlow()).map(e->e.getMulticastDemandAndNode()).collect(Collectors.toSet());
	}

}
