package com.net2plan.gui.viewEditTopolTables;

import com.google.common.collect.Sets;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.utils.Pair;

import java.util.*;
import java.util.stream.Collectors;

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
	
	private final List<? extends Object> filterAnd (List<? extends Object> l1 , List<? extends Object> l2)
	{
		final Set<? extends Object> resSet = Sets.intersection(new HashSet<> (l1) , new HashSet<> (l2)); 
		List<Object> resList = new LinkedList<> ();
		for (Object o : l1) if (resSet.contains(o)) resList.add(o); // keep the same order as in List 1
		return resList;
	}



	protected static List<? extends NetworkElement> orderSet (Set<? extends NetworkElement> set)
	{
		List res = new ArrayList<NetworkElement> (set);
		Collections.sort(res , new Comparator<NetworkElement> () { public int compare (NetworkElement e1 , NetworkElement e2) { return Integer.compare(e1.getIndex() , e2.getIndex()); }  } );
		return res;
	}
	protected static List<Pair<Demand,Link>> orderSetFR (Set<Pair<Demand,Link>> set)
	{
		List res = new ArrayList<Pair<Demand,Link>> (set);
		Collections.sort(res , new Comparator<Pair<Demand,Link>> () { public int compare (Pair<Demand,Link> e1 , Pair<Demand,Link> e2) { return Integer.compare(e1.getFirst().getIndex() , e2.getFirst ().getIndex()); }  } );
		return res;
	}
	
	protected void updateAllButLinksDemandsMDemandsUsingExistingInfo (Set<Link> linksAllLayers , Set<Demand> demandsAllLayers , Set<MulticastDemand> mDemandsAllLayers , Set<NetworkLayer> layersToKeepAllElements)
	{
		/* update the rest according to this */
		for (NetworkLayer layer : netPlan.getNetworkLayers())
		{
			final boolean isSourceRouting = layer.isSourceRouting();
			if (layersToKeepAllElements.contains (layer))
			{
				/* keep this layer unchanged */
				vDemands.put(layer , netPlan.getDemands(layer));
				if (!isSourceRouting) vFRs.put(layer , new ArrayList<> (netPlan.getForwardingRules(layer).keySet()));
				if (isSourceRouting) vRoutes.put(layer , netPlan.getRoutes(layer));
				vLinks.put(layer ,netPlan.getLinks(layer));
				vMDemands.put(layer , netPlan.getMulticastDemands(layer));
				vTrees.put(layer , netPlan.getMulticastTrees(layer));
				vNodes.put(layer , netPlan.getNodes());
				vResources.put(layer , netPlan.getResources());
				vSRGs.put(layer , netPlan.getSRGs());
				continue;
			}
				
			/* Here if we filter out by this layer */
			final Set<Link> links = linksAllLayers.stream().filter(e->e.getLayer().equals(layer)).collect(Collectors.toSet());
			final Set<Demand> demands = demandsAllLayers.stream().filter(e->e.getLayer().equals(layer)).collect(Collectors.toSet());
			final Set<MulticastDemand> mDemands = mDemandsAllLayers.stream().filter(e->e.getLayer().equals(layer)).collect(Collectors.toSet());
			
			final Set<Route> routes = new HashSet<> ();
			final Set<MulticastTree> trees = new HashSet<> ();
			final Set<Resource> resources = new HashSet<> ();
			final Set<SharedRiskGroup> srgs = new HashSet<> ();
			final Set<Pair<Demand,Link>> frs = new HashSet<> ();
			final Set<Node> nodes = new HashSet<> ();
			
			/* Update the FRs: all of the ones associated to the demands */
			if (!isSourceRouting) 
				for (Demand d : demands) frs.addAll(d.getForwardingRules().keySet());

			/* The routes and the resources: all the ones of a demand in 
			 * the set that traverse a link in the set => for them the traversed resources */
			if (isSourceRouting)
				for (Link e : links)
					for (Route r : e.getTraversingRoutes()) 
						if (demands.contains(r.getDemand()))
						{
							routes.add(r);
							resources.addAll(r.getSeqResourcesTraversed());
						}
			
			/* The multicast trees: the ones in the multicast demands AND also traverse involved links */
			for (Link e : links)
				for (MulticastTree t : e.getTraversingTrees()) 
					if (mDemands.contains(t.getMulticastDemand()))
						trees.add(t);

			/* The nodes: the ones in any link */
			for (Link e : links)
				{ nodes.add(e.getOriginNode()); nodes.add(e.getDestinationNode()); }
			
			/* The SRGs: the ones affecting any of the links in the set */
			srgs.addAll(SRGUtils.getAffectingSRGs(links));
			
			vDemands.put(layer , (List<Demand>) orderSet(demands));
			vFRs.put(layer , (List<Pair<Demand,Link>>) orderSetFR(frs));
			vLinks.put(layer , (List<Link>) orderSet(links));
			vMDemands.put(layer , (List<MulticastDemand>) orderSet(mDemands));
			vTrees.put(layer , (List<MulticastTree>) orderSet(trees));
			vNodes.put(layer , (List<Node>) orderSet(nodes));
			vResources.put(layer , (List<Resource>) orderSet(resources));
			vRoutes.put(layer , (List<Route>) orderSet(routes));
			vSRGs.put(layer , (List<SharedRiskGroup>) orderSet(srgs));
		}
	}
	
    protected static Pair<Set<Demand>,Set<Pair<MulticastDemand,Node>>> getDownCoupling (Collection<Link> links)
    {
    	final Set<Demand> res_1 = new HashSet<> ();
    	final Set<Pair<MulticastDemand,Node>> res_2 = new HashSet<> ();
    	for (Link link : links)
    	{
    		if (link.getCoupledDemand() != null) res_1.add(link.getCoupledDemand());
    		else if (link.getCoupledMulticastDemand() != null) res_2.add(Pair.of(link.getCoupledMulticastDemand() , link.getDestinationNode()));
    	}
    	return Pair.of(res_1 , res_2);
    	
    }
    protected static Set<Link> getUpCoupling (Collection<Demand> demands , Collection<Pair<MulticastDemand,Node>> mDemands)
    {
    	final Set<Link> res = new HashSet<> ();
    	if (demands != null)
    		for (Demand d : demands)
    			if (d.isCoupled()) res.add(d.getCoupledLink());
    	if (mDemands != null)
    		for (Pair<MulticastDemand,Node> md : mDemands)
    		{
    			if (md.getFirst().isCoupled())
        			res.add(md.getFirst().getCoupledLinks().stream().filter (e->e.getDestinationNode() == md.getSecond()).findFirst().get());
//    			System.out.println(md.getFirst().getCoupledLinks().stream().map(e->e.getDestinationNode()).collect(Collectors.toList()));
//    			System.out.println(md.getSecond());
    		}
    	return res;
    }


}
