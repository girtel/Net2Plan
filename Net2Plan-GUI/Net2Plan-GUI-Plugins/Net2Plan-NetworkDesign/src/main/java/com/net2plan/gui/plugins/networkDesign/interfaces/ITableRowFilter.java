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
package com.net2plan.gui.plugins.networkDesign.interfaces;

import com.google.common.collect.Sets;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Pair;

import java.util.*;

@SuppressWarnings("unchecked")
public abstract class ITableRowFilter
{
	protected final Map<NetworkLayer,List<Demand>> vDemands;
	protected final Map<NetworkLayer,List<Pair<Demand,Link>>> vFRs;
	protected final Map<NetworkLayer,List<Link>> vLinks;
	protected final Map<NetworkLayer,List<MulticastDemand>> vMDemands;
	protected final Map<NetworkLayer,List<MulticastTree>> vTrees;
	protected final Map<NetworkLayer,List<Node>> vNodes;
	protected final Map<NetworkLayer,List<Resource>> vResources;
	protected final Map<NetworkLayer,List<Route>> vRoutes;
	protected final Map<NetworkLayer,List<SharedRiskGroup>> vSRGs;
	protected final NetPlan netPlan;
	protected List<String> chainOfDescriptionsPreviousFiltersComposingThis;

	public enum FilterCombinationType { INCLUDEIF_AND, INCLUDEIF_OR };
	
	/* Baseline constructor: everything is filtered out */
	public ITableRowFilter (NetPlan netPlan)
	{
		this.netPlan = netPlan;
		this.vDemands = new HashMap<> (); 
		this.vFRs = new HashMap<> ();
		this.vLinks = new HashMap<> ();
		this.vMDemands= new HashMap<> ();
		this.vTrees = new HashMap<> ();
		this.vNodes = new HashMap<> ();
		this.vResources = new HashMap<> ();
		this.vRoutes = new HashMap<> ();
		this.vSRGs = new HashMap<> ();
		this.chainOfDescriptionsPreviousFiltersComposingThis = new LinkedList<> ();
		for (NetworkLayer layer : netPlan.getNetworkLayers())
		{
			this.vDemands.put(layer , new ArrayList<> ());
			this.vFRs.put(layer , new ArrayList<> ());
			this.vLinks.put(layer , new ArrayList<> ());
			this.vMDemands.put(layer , new ArrayList<> ());
			this.vTrees.put(layer , new ArrayList<> ());
			this.vNodes.put(layer , new ArrayList<> ());
			this.vResources.put(layer , new ArrayList<> ());
			this.vRoutes.put(layer , new ArrayList<> ());
			this.vSRGs.put(layer , new ArrayList<> ());
		}
	}

	public abstract String getDescription ();
	
	public final List<Demand> getVisibleDemands (NetworkLayer layer) 	{ return Collections.unmodifiableList(vDemands.get(layer)); }
	public final List<Pair<Demand,Link>> getVisibleForwardingRules (NetworkLayer layer) { return Collections.unmodifiableList(vFRs.get(layer)); }
	public final List<Link> getVisibleLinks (NetworkLayer layer) { return Collections.unmodifiableList(vLinks.get(layer)); }
	public final List<MulticastDemand> getVisibleMulticastDemands (NetworkLayer layer) { return Collections.unmodifiableList(vMDemands.get(layer)); }
	public final List<MulticastTree> getVisibleMulticastTrees (NetworkLayer layer) { return Collections.unmodifiableList(vTrees.get(layer)); }
	public final List<Node> getVisibleNodes (NetworkLayer layer) { return Collections.unmodifiableList(vNodes.get(layer)); }
	public final List<Resource> getVisibleResources (NetworkLayer layer) { return Collections.unmodifiableList(vResources.get(layer)); }
	public final List<Route> getVisibleRoutes (NetworkLayer layer) { return Collections.unmodifiableList(vRoutes.get(layer)); }
	public final List<SharedRiskGroup> getVisibleSRGs (NetworkLayer layer) { return Collections.unmodifiableList(vSRGs.get(layer)); }

	public final boolean hasDemands (NetworkLayer layer) { return !vDemands.get(layer).isEmpty(); }
	public final boolean hasLinks (NetworkLayer layer) { return !vLinks.get(layer).isEmpty(); }
	public final boolean hasForwardingRules (NetworkLayer layer) { return !vFRs.get(layer).isEmpty(); }
	public final boolean hasMulticastDemands (NetworkLayer layer) { return !vMDemands.get(layer).isEmpty(); }
	public final boolean hasMulticastTrees (NetworkLayer layer) { return !vTrees.get(layer).isEmpty(); }
	public final boolean hasNodes (NetworkLayer layer) { return !vNodes.get(layer).isEmpty(); }
	public final boolean hasResources (NetworkLayer layer) { return !vResources.get(layer).isEmpty(); }
	public final boolean hasRoutes (NetworkLayer layer) { return !vRoutes.get(layer).isEmpty(); }
	public final boolean hasSRGs (NetworkLayer layer) { return !vSRGs.get(layer).isEmpty(); }

	public final int getNumberOfDemands (NetworkLayer layer) { return vDemands.get(layer).size(); }
	public final int getNumberOfLinks (NetworkLayer layer) { return vLinks.get(layer).size(); }
	public final int getNumberOfForwardingRules (NetworkLayer layer) { return vFRs.get(layer).size(); }
	public final int getNumberOfMulticastDemands (NetworkLayer layer) { return vMDemands.get(layer).size(); }
	public final int getNumberOfMulticastTrees (NetworkLayer layer) { return vTrees.get(layer).size(); }
	public final int getNumberOfNodes (NetworkLayer layer) { return vNodes.get(layer).size(); }
	public final int getNumberOfResources (NetworkLayer layer) { return vResources.get(layer).size(); }
	public final int getNumberOfRoutes (NetworkLayer layer) { return vRoutes.get(layer).size(); }
	public final int getNumberOfSRGs (NetworkLayer layer) { return vSRGs.get(layer).size(); }

	public final void recomputeApplyingShowIf_ThisAndThat (ITableRowFilter that)
	{
		if (this.netPlan != that.netPlan) throw new RuntimeException();
		if (!this.netPlan.getNetworkLayers().equals(that.netPlan.getNetworkLayers())) throw new RuntimeException();
		for (NetworkLayer layer : this.netPlan.getNetworkLayers())
		{
			vDemands.put(layer , (List<Demand>) filterAnd(this.vDemands.get(layer) , that.vDemands.get(layer)));
			vFRs.put(layer , (List<Pair<Demand,Link>>) filterAnd(this.vFRs.get(layer) , that.vFRs.get(layer)));
			vLinks.put(layer , (List<Link>) filterAnd(this.vLinks.get(layer) , that.vLinks.get(layer)));
			vMDemands.put(layer , (List<MulticastDemand>) filterAnd(this.vMDemands.get(layer) , that.vMDemands.get(layer)));
			vTrees.put(layer , (List<MulticastTree>) filterAnd(this.vTrees.get(layer) , that.vTrees.get(layer)));
			vNodes.put(layer , (List<Node>) filterAnd(this.vNodes.get(layer) , that.vNodes.get(layer)));
			vResources.put(layer , (List<Resource>) filterAnd(this.vResources.get(layer) , that.vResources.get(layer)));
			vRoutes.put(layer , (List<Route>) filterAnd(this.vRoutes.get(layer) , that.vRoutes.get(layer)));
			vSRGs.put(layer , (List<SharedRiskGroup>) filterAnd(this.vSRGs.get(layer) , that.vSRGs.get(layer)));
		}
		chainOfDescriptionsPreviousFiltersComposingThis.add(that.getDescription());
	}
	
	public final void recomputeApplyingShowIf_ThisOrThat (ITableRowFilter that)
	{
		if (this.netPlan != that.netPlan) throw new RuntimeException();
		if (!this.netPlan.getNetworkLayers().equals(that.netPlan.getNetworkLayers())) throw new RuntimeException();
		for (NetworkLayer layer : this.netPlan.getNetworkLayers())
		{
			vDemands.put(layer , (List<Demand>) filterOr(this.vDemands.get(layer) , that.vDemands.get(layer)));
			vFRs.put(layer , (List<Pair<Demand,Link>>) filterOr(this.vFRs.get(layer) , that.vFRs.get(layer)));
			vLinks.put(layer , (List<Link>) filterOr(this.vLinks.get(layer) , that.vLinks.get(layer)));
			vMDemands.put(layer , (List<MulticastDemand>) filterOr(this.vMDemands.get(layer) , that.vMDemands.get(layer)));
			vTrees.put(layer , (List<MulticastTree>) filterOr(this.vTrees.get(layer) , that.vTrees.get(layer)));
			vNodes.put(layer , (List<Node>) filterOr(this.vNodes.get(layer) , that.vNodes.get(layer)));
			vResources.put(layer , (List<Resource>) filterOr(this.vResources.get(layer) , that.vResources.get(layer)));
			vRoutes.put(layer , (List<Route>) filterOr(this.vRoutes.get(layer) , that.vRoutes.get(layer)));
			vSRGs.put(layer , (List<SharedRiskGroup>) filterOr(this.vSRGs.get(layer) , that.vSRGs.get(layer)));
		}
		chainOfDescriptionsPreviousFiltersComposingThis.add(that.getDescription());
	}

	
	private final List<? extends Object> filterAnd (List<? extends Object> l1 , List<? extends Object> l2)
	{
		final List<Object> resList = new LinkedList<> ();
		for (Object o : l1) if (l2.contains(o)) resList.add(o); // keep the same order as in List 1
		return resList;
	}

	private final List<? extends Object> filterOr (List<? extends Object> l1 , List<? extends Object> l2)
	{
		final List<Object> resList = new ArrayList<> (l1);
		for (Object o : l2) if (!l1.contains(o)) resList.add(o); // first the ones in l1, then the ones in l2-l1 
		return resList;
	}

	protected final static List<? extends NetworkElement> orderSet (Set<? extends NetworkElement> set)
	{
		List res = new ArrayList<NetworkElement> (set);
		Collections.sort(res , new Comparator<NetworkElement> () { public int compare (NetworkElement e1 , NetworkElement e2) { return Integer.compare(e1.getIndex() , e2.getIndex()); }  } );
		return res;
	}
	protected final static List<Pair<Demand,Link>> orderSetFR (Set<Pair<Demand,Link>> set)
	{
		List res = new ArrayList<Pair<Demand,Link>> (set);
		Collections.sort(res , new Comparator<Pair<Demand,Link>> () { public int compare (Pair<Demand,Link> e1 , Pair<Demand,Link> e2) { return Integer.compare(e1.getFirst().getIndex() , e2.getFirst ().getIndex()); }  } );
		return res;
	}

}
