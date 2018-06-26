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

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.SortedSet;
import java.util.TreeSet;

import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.ErrorHandling;

/** <p>This class contains a representation of a Shared Risk Group (SRG). This is a concept representing a risk of failure in the network,
 * such that if this risk becomes true, a particular set of links and/or nodes simultaneously fail. For instance, a SRG can be the risk of 
 * accidentally cutting a duct containing multiple links. Its associated failiing resources would be all the links contained in the duct.</p>
 * <p> The SRG is characterized by the links/nodes failing, and the MTTF (meen time to fail) and MTTR (mean time to repair) values 
 * that describe statistically the average time between a reparation of the risk effects and a new risk occurrence (MTTF), and the time between 
 * a risk occurrence until it is repaired (MTTR).</p> 
 * @author Pablo Pavon-Marino
 */
/**
 * @author Pablo
 *
 */
/**
 * @author Pablo
 *
 */
public class SharedRiskGroup extends NetworkElement
{
	private SortedSet<Node> nodesIfNonDynamic;
	SortedSet<Link> linksIfNonDynamic;
	double meanTimeToFailInHours;
	double meanTimeToRepairInHours;
	DynamicSrgImplementation dynamicSrgImplementation;
	

	SharedRiskGroup (NetPlan netPlan , long id , int index , SortedSet<Node> nodes , SortedSet<Link> links , double meanTimeToFailInHours ,  double meanTimeToRepairInHours , AttributeMap attributes)
	{
		super (netPlan , id , index , attributes);
		this.setName ("Srg-" + index);

		if (links == null) links = new TreeSet<Link> ();
		if (nodes == null) nodes = new TreeSet<Node> ();
		
		for (Link e : links) if (!netPlan.equals(e.netPlan)) throw new RuntimeException ("Bad"); 
		for (Node n : nodes) if (!netPlan.equals(n.netPlan)) throw new RuntimeException ("Bad");

		this.nodesIfNonDynamic = new TreeSet<Node> (nodes);
		this.linksIfNonDynamic = new TreeSet<Link> (links);
		this.meanTimeToFailInHours = meanTimeToFailInHours;
		this.meanTimeToRepairInHours = meanTimeToRepairInHours;
		for (Node n : nodes) n.cache_nodeNonDynamicSRGs.add(this);
        for (Link e : links) e.cache_nonDynamicSrgs.add(this);
        this.dynamicSrgImplementation = null;
	}

    SharedRiskGroup (NetPlan netPlan , long id , int index , String className , String dynamicInitializationString , double meanTimeToFailInHours ,  double meanTimeToRepairInHours , AttributeMap attributes)
    {
        super (netPlan , id , index , attributes);
		this.setName ("Srg-" + index);
        this.nodesIfNonDynamic = new TreeSet<> ();
        this.linksIfNonDynamic = new TreeSet<> ();
        this.meanTimeToFailInHours = meanTimeToFailInHours;
        this.meanTimeToRepairInHours = meanTimeToRepairInHours;
        try { this.dynamicSrgImplementation = (DynamicSrgImplementation) Class.forName(className).newInstance(); } catch (Exception e) { e.printStackTrace(); throw new Net2PlanException ("SRG Dynamic initialization error");} 
        this.dynamicSrgImplementation.initialize(dynamicInitializationString , netPlan);
    }

    public boolean isDynamicSrg () { return dynamicSrgImplementation != null; }

    /** Returns the object implementing the dynamic decision of which nodes/links belong to this SRG
     * @return  see above
     */
    public DynamicSrgImplementation getDynamicSrgImplementation () { return dynamicSrgImplementation; }

    public void setDynamicImplementation (DynamicSrgImplementation impl)
    {
        if (!isDynamicSrg()) throw new Net2PlanException ("The SRG is not dynamic");
        this.dynamicSrgImplementation = impl;
    }
    
    void copyFrom (SharedRiskGroup origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.meanTimeToFailInHours = origin.meanTimeToFailInHours;
		this.meanTimeToRepairInHours = origin.meanTimeToRepairInHours;
		if (origin.isDynamicSrg())
		{
		    this.linksIfNonDynamic.clear();
            this.nodesIfNonDynamic.clear();
            try { this.dynamicSrgImplementation = (DynamicSrgImplementation) Class.forName(origin.dynamicSrgImplementation.getClass().getName()).newInstance(); } catch (Exception e) { e.printStackTrace(); throw new Net2PlanException ("SRG Dynamic initialization error");} 
            this.dynamicSrgImplementation.initialize(origin.dynamicSrgImplementation.getInitializationString (), this.netPlan);
		}
		else
		{
	        this.linksIfNonDynamic.clear (); for (Link e : origin.linksIfNonDynamic) this.linksIfNonDynamic.add(this.netPlan.getLinkFromId (e.id));
	        this.nodesIfNonDynamic.clear (); for (Node n : origin.nodesIfNonDynamic) this.nodesIfNonDynamic.add(this.netPlan.getNodeFromId (n.id));
	        this.dynamicSrgImplementation = null;
		}
	}

    public boolean isStaticEmptySrg () { return !isDynamicSrg() && this.linksIfNonDynamic.isEmpty() && this.nodesIfNonDynamic.isEmpty(); }
    
    public boolean isDynamicCurrentlyEmptySrg () { return isDynamicSrg() && this.getLinksAllLayers().isEmpty() && this.getNodes().isEmpty(); }

    public static SharedRiskGroup getDummyEmptyStaticSrg (NetPlan np)
    {
        final SharedRiskGroup srg = np.addSRG(1, 1, null);
        srg.remove();
        return srg;
    }
    
    public static SortedSet<SharedRiskGroup> getSrgListCompletedWithDummyNoFailureStateIfNeeded (Collection<SharedRiskGroup> sourceSrgs , NetPlan np)
    {
        if (sourceSrgs == null) throw new Net2PlanException ("Wrong input parameters");
        final SortedSet<SharedRiskGroup> res = new TreeSet<> (sourceSrgs);
        final boolean hasNoFailure = res.stream().anyMatch(s->s.isStaticEmptySrg());
        if (!hasNoFailure) res.add(getDummyEmptyStaticSrg(np));
        return res;
    }

    boolean isDeepCopy (SharedRiskGroup e2)
	{
		if (!super.isDeepCopy(e2)) return false;
		if (this.meanTimeToFailInHours != e2.meanTimeToFailInHours) return false;
		if (this.meanTimeToRepairInHours != e2.meanTimeToRepairInHours) return false;
		if (this.isDynamicSrg())
		{
		    if (!e2.isDynamicSrg()) return false;
		    if (!this.dynamicSrgImplementation.getClass().getName().equals(e2.dynamicSrgImplementation.getClass().getName())) return false;
		    if (!this.dynamicSrgImplementation.getInitializationString().equals(e2.dynamicSrgImplementation.getInitializationString())) return false;
            if (!NetPlan.isDeepCopy(this.getLinksAllLayers() , e2.getLinksAllLayers())) return false;
            if (!NetPlan.isDeepCopy(this.getNodes() , e2.getNodes())) return false;
		}
		else
		{
		    if (e2.isDynamicSrg()) return false;
	        if (!NetPlan.isDeepCopy(this.nodesIfNonDynamic , e2.nodesIfNonDynamic)) return false;
	        if (!NetPlan.isDeepCopy(this.linksIfNonDynamic, e2.linksIfNonDynamic)) return false;
		}
		return true;
	}

	/**
	 * <p>Returns the set of nodes associated to the SRG (fail, when the SRG is in failure state)</p>
	 * @return The set of failing nodes, as an unmodifiable set
	 */
	public SortedSet<Node> getNodes()
	{
	    if (isDynamicSrg()) return dynamicSrgImplementation.getNodes();
		return Collections.unmodifiableSortedSet(nodesIfNonDynamic);
	}

	/**
	 * <p>Returns all the links affected by the SRG at all the layers: the links affected, and the input and output links of the affected nodes</p>
	 * @return All the affected links
	 */
	public SortedSet<Link> getAffectedLinksAllLayers ()
	{
		SortedSet<Link> res = new TreeSet<Link> ();
		res.addAll (getLinksAllLayers());
		for (Node n : getNodes ()) { res.addAll (n.cache_nodeIncomingLinks); res.addAll (n.cache_nodeOutgoingLinks); }
		return res;
	}

	/**
	 * <p>Returns all the links affected by this SRG, but only those at a particular layer. This includes the links involved, and the input and output links of the affected nodes</p>
	 * @param layer Network layer
	 * @return All the affected links at a given layer
	 */
	public SortedSet<Link> getAffectedLinks (NetworkLayer layer)
	{
		SortedSet<Link> res = new TreeSet<Link> ();
		for (Link e : getLinksAllLayers()) if (e.layer == layer) res.add (e);
		for (Node n : getNodes ()) 
		{
			for (Link e : n.cache_nodeIncomingLinks) if (e.layer == layer) res.add (e);
			for (Link e : n.cache_nodeOutgoingLinks) if (e.layer == layer) res.add (e);
		}
		return res;
	}

	/**
	 * <p>Returns the set of routes affected by the SRG (fail, when the SRG is in failure state). </p>
	 * @return The set of failing routes
	 */
	public SortedSet<Route> getAffectedRoutesAllLayers ()
	{
		SortedSet<Route> res = new TreeSet<Route> ();
		for (Link e : getLinksAllLayers()) res.addAll (e.cache_traversingRoutes.keySet());
		for (Node n : getNodes()) res.addAll (n.cache_nodeAssociatedRoutes);
		return res;
	}

	/**
	 * <p>Returns the set of routes in the given layer affected by the SRG (fail, when the SRG is in failure state)</p>
	 * @param layer Network layer
	 * @return The failing routes belonging to that layer
	 */
	public SortedSet<Route> getAffectedRoutes (NetworkLayer layer)
	{
		SortedSet<Route> res = new TreeSet<Route> ();
		for (Link e : getLinksAllLayers()) for (Route r : e.cache_traversingRoutes.keySet()) if (r.layer.equals(layer)) res.add (r);
		for (Node n : getNodes()) for (Route r : n.cache_nodeAssociatedRoutes) if (r.layer.equals(layer)) res.add (r);
		return res;
	}

	/**
	 * <p>Returns the set of multicast trees affected by the SRG (fail, when the SRG is in failure state). </p>
	 * @return The set of failing multicast trees
	 */
	public SortedSet<MulticastTree> getAffectedMulticastTreesAllLayers ()
	{
		SortedSet<MulticastTree> res = new TreeSet<MulticastTree> ();
		for (Link e : getLinksAllLayers()) res.addAll (e.cache_traversingTrees);
		for (Node n : getNodes()) res.addAll (n.cache_nodeAssociatedulticastTrees);
		return res;
	}

	/**
	 * <p>Returns the set of multicast trees in the given layer affected by the SRG (fail, when the SRG is in failure state)</p>
	 * @param layer Network layer
	 * @return The failing multicast trees belonging to the given layer
	 */
	public SortedSet<MulticastTree> getAffectedMulticastTrees (NetworkLayer layer)
	{
		SortedSet<MulticastTree> res = new TreeSet<MulticastTree> ();
		for (Link e : getLinksAllLayers()) for (MulticastTree t : e.cache_traversingTrees) if (t.layer.equals(layer)) res.add (t);
		for (Node n : getNodes ()) for (MulticastTree t : n.cache_nodeAssociatedulticastTrees) if (t.layer.equals(layer)) res.add (t);
		return res;
	}


	
	/** Returns true if any element of the given collection of links, nodes and resources is affected by te SRG (fails if the SRG fails), false otherwise
	 * @param col the collection
	 * @return see above
	 */
	public boolean affectsAnyOf (Collection<? extends NetworkElement> col)
	{
	    final SortedSet<Node> nodesThisSrg = getNodes ();
		for (NetworkElement e : col)
		{
			if (e instanceof Link)
			{
				final Link ee = (Link) e;
				if (nodesThisSrg.contains(ee.originNode)) return true;
				if (nodesThisSrg.contains(ee.destinationNode)) return true;
				if (getLinksAllLayers().contains(ee)) return true;
			}
			else if (e instanceof Resource)
			{
				final Resource ee = (Resource) e;
				if (nodesThisSrg.contains(ee.hostNode)) return true;
			}
			else if (e instanceof Node)
			{
				final Node ee = (Node) e;
				if (nodesThisSrg.contains(ee)) return true;
			}
			else throw new Net2PlanException ("The collection can contain only links, resources and/or nodes");
		}
		return false;
	}
	
	/**
	 * <p>Returns the set of links associated to the SRG (fail, when the SRG is in failure state).</p>
	 * @return The set of failing links, as an unmodifiable set
	 */
	public SortedSet<Link> getLinksAllLayers()
	{
	    if (isDynamicSrg()) return dynamicSrgImplementation.getLinksAllLayers();
		return Collections.unmodifiableSortedSet(linksIfNonDynamic);
	}

	/**
	 * <p>Returns the set of links associated to the SRG (fail, when the SRG is in failure state), but only those belonging to the given layer. </p>
	 * @param layer the layer
	 * @return The set of failing links
	 */
	public SortedSet<Link> getLinks(NetworkLayer layer)
	{
		SortedSet<Link> res = new TreeSet<Link> ();
		for (Link e : getLinksAllLayers()) if (e.layer.equals(layer)) res.add (e);
		return res;
	}

	/**
	 * <p>Returns the mean time to fail (MTTF) in hours of the SRG, that is, the average time between when it is repaired, and its next failure.</p>
	 * @return The MTTF in hours
	 */
	public double getMeanTimeToFailInHours()
	{
		return meanTimeToFailInHours;
	}

	/**
	 * <p>Sets the mean time to fail (MTTF) in hours of the SRG, that is, the average time between it is repaired, and the next failure.</p>
	 * @param value The new MTTF (it must be greater than zero)
	 */
	public void setMeanTimeToFailInHours(double value)
	{
		if (value <= 0) throw new Net2PlanException ("A positive value is expected");
		this.meanTimeToFailInHours = value;
	}

	/**
	 * <p>Returns the mean time to repair (MTTR) of the SRG, that is, the average time between a failure occurs, and it is repaired.</p>
	 * @return the MTTR in hours
	 */
	public double getMeanTimeToRepairInHours()
	{
		return meanTimeToRepairInHours;
	}

	/**
	 * <p>Sets the mean time to repair (MTTR) in hours of the SRG, that is, the average time  between a failure occurs, and it is repaired.</p>
	 * @param value the nnew MTTR (negative values are not accepted)
	 */
	public void setMeanTimeToRepairInHours(double value)
	{
		if (value < 0) throw new Net2PlanException ("A positive value is expected");
		this.meanTimeToRepairInHours = value;
	}

	/**
	 * <p>Returns the availability (between 0 and 1) of the SRG. That is, the fraction of time that the SRG is in non failure state. </p>
	 * <p>Availability is computed as: MTTF/ (MTTF + MTTR)</p>
	 * @return MTTF/ (MTTF + MTTR)
	 */
	public double getAvailability()
	{
		return meanTimeToFailInHours / (meanTimeToFailInHours + meanTimeToRepairInHours);
	}

	/**
	 * <p>Sets nodes and links associated to the  SRG as down (in case they are not yet). The network state is updated with the affected routes,
	 * segments, trees and hop-by-hop routing associated to the new nodes/links down </p>
	 */
	public void setAsDown ()
	{
		checkAttachedToNetPlanObject();
		netPlan.setLinksAndNodesFailureState (null , getLinksAllLayers() , null , getNodes ());
	}
	
	/**
	 * <p>Sets nodes and links associated to the  SRG as up (in case they are not yet). The network state is updated with the affected routes,
	 * segments, trees and hop-by-hop routing associated to the new nodes/links down </p>
	 */
	public void setAsUp ()
	{
		checkAttachedToNetPlanObject();
		netPlan.setLinksAndNodesFailureState (getLinksAllLayers() , null , getNodes (), null);
	}

	/**
	 * <p>Removes a link from the set of links of the SRG. If the link is not in the SRG, no action is taken</p>
	 * @param e Link to be removed
	 */
	public void removeLink (Link e)
	{
	    if (isDynamicSrg()) throw new Net2PlanException ("Cannot modify dynamic SRGs");
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		e.cache_nonDynamicSrgs.remove (this); 
		linksIfNonDynamic.remove (e);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}
	
	/**
	 * <p>Removes a node from the set of nodes of the SRG. If the node is not in the SRG, no action is taken</p>
	 * @param n Node to be removed
	 */
	public void removeNode (Node n)
	{
        if (isDynamicSrg()) throw new Net2PlanException ("Cannot modify dynamic SRGs");
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		n.cache_nodeNonDynamicSRGs.remove (this);
		nodesIfNonDynamic.remove (n);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Removes this SRG.</p>
	 */
	public void remove()
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();

		if (!isDynamicSrg())
		{
	        for (Node node : nodesIfNonDynamic) node.cache_nodeNonDynamicSRGs.remove (this);
	        for (Link link : linksIfNonDynamic) link.cache_nonDynamicSrgs.remove (this);
		}
		else
		{
	        dynamicSrgImplementation.remove(); // release memory
		}
        for (String tag : tags) netPlan.cache_taggedElements.get(tag).remove(this);
		netPlan.cache_id2srgMap.remove (id);
		NetPlan.removeNetworkElementAndShiftIndexes(netPlan.srgs , index);
		if (isDynamicSrg()) netPlan.cache_dynamicSrgs.remove(this);
        final NetPlan npOld = this.netPlan;
        removeId();
        if (ErrorHandling.isDebugEnabled()) npOld.checkCachesConsistency();
	}

	/**
	 * <p>Adds a link to the SRG. If the link is already part of the srg, no action is taken.</p>
	 * @param link Link to add
	 */
	public void addLink(Link link)
	{
        if (isDynamicSrg()) throw new Net2PlanException ("Cannot modify dynamic SRGs");
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		link.checkAttachedToNetPlanObject(this.netPlan);
		if (this.linksIfNonDynamic.contains(link)) return;
		link.cache_nonDynamicSrgs.add(this);
		this.linksIfNonDynamic.add(link);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Adds a node to the SRG. If the node is already part of the SRG, no action is taken</p>
	 * @param node Node to add
	 */
	public void addNode(Node node)
	{
        if (isDynamicSrg()) throw new Net2PlanException ("Cannot modify dynamic SRGs");
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		node.checkAttachedToNetPlanObject(this.netPlan);
		if (this.nodesIfNonDynamic.contains(node)) return;
		node.cache_nodeNonDynamicSRGs.add(this);
		this.nodesIfNonDynamic.add(node);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
	}

	/**
	 * <p>Returns a {@code String} representation of the Shared Risk Group.</p>
	 * @return {@code String} representation of the SRG
	 */
	public String toString () 
	{ 
	    if (isDynamicSrg())
	        return "SRG" + index + ". Dynamic: " + dynamicSrgImplementation.toString();
	    else
	        return "SRG" + index + ", links: " + linksIfNonDynamic + ", nodes: " + nodesIfNonDynamic; 
	}

	void checkCachesConsistency ()
	{
		super.checkCachesConsistency ();

		assert getNodes ().stream().allMatch(n->n.getNetPlan()== this.netPlan);
        assert getLinksAllLayers ().stream().allMatch(n->n.getNetPlan()== this.netPlan);
		
		if (isDynamicSrg())
		{
	        for (Link link : linksIfNonDynamic) if (!link.cache_nonDynamicSrgs.contains(this)) throw new RuntimeException ("Bad");
	        for (Node node : nodesIfNonDynamic) if (!node.cache_nodeNonDynamicSRGs.contains(this)) throw new RuntimeException ("Bad");
		}
	}

}
