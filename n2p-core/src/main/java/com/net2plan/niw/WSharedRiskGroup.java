/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.niw.WNetConstants.WTYPE;

/** Instances of this class are service chains, realizing service chain requests. A service chain should start in one of the origin nodes of the service chain, and end in 
 * one of the destination nodes of the service chain. The injection traffic of the service chain is the traffic produced by its origin node. Note that when the service chain 
 * traffic traverses a VNF, its volume will increase or decrease according to the expansion factor defined for that VNF.
 * 
 *  
 *
 */
public class WSharedRiskGroup extends WAbstractNetworkElement
{
	WSharedRiskGroup(SharedRiskGroup r) 
	{ 
		super (r, Optional.empty()); 
	}

	@Override
	public SharedRiskGroup getNe() { return (SharedRiskGroup) this.associatedNpElement; }

	/** Returns the nodes associated to this SRG
	 * @return see above
	 */
	public SortedSet<WNode> getFailingNodes () 
	{
		return getNe().getNodes().stream().map(n->new WNode(n)).filter(n->!n.isVirtualNode()).collect(Collectors.toCollection(TreeSet::new));
	}

	/** Returns the fibers associated to this SRG
	 * @return see above
	 */
	public SortedSet<WFiber> getFailingFibers () 
	{
		if (!getNet().isWithWdmLayer()) return new TreeSet<> ();
		return getNe().getLinks(getNet().getWdmNpLayer().get()).stream().map(n->new WFiber(n)).collect(Collectors.toCollection(TreeSet::new));
	}

	/** Returns the IP links associated to this SRG
	 * @return see above
	 */
	public SortedSet<WIpLink> getFailingIpLinks () 
	{
		if (!getNet().isWithIpLayer()) return new TreeSet<> ();
		return getNe().getLinks(getNet().getIpNpLayer().get()).stream().map(n->new WIpLink(n)).collect(Collectors.toCollection(TreeSet::new));
	}

	/** Adds a failing node to the SRG 
	 * @param node see above
	 */
	public void addFailingNode (WNode node)
	{
		if (node.isVirtualNode()) throw new Net2PlanException ("Cannot add virtual nodes to the SRG");
		getNe().addNode (node.getNe());
	}

	/** Adds failing nodes to the SRG 
	 * @param nodes see above
	 */
	public void addFailingNodes (Collection<WNode> nodes)
	{
		for (WNode n : nodes) addFailingNode(n);
	}


	/** Removes this SRG
	 */
	public void remove () 
	{
		getNe().remove();
	}

	/** Removes all the failing nodes in this SRG
	 */
	public void removeAllFailingNodes ()
	{
		for (WNode n : new ArrayList<> (getFailingNodes ()))
			this.removeFailingNode(n);
	}
	
	/** Removes all the failing fibers in this SRG
	 */
	public void removeAllFailingFibers ()
	{
		for (WFiber n : new ArrayList<> (getFailingFibers ()))
			this.removeFailingFiber(n);
	}
	/** Removes all the failing IP links in this SRG
	 */
	public void removeAllFailingIpLinks ()
	{
		for (WIpLink n : new ArrayList<> (getFailingIpLinks ()))
			this.removeFailingIpLink(n);
	}
	

	/** Removes a failing node from the SRG. If the node is not in the SRG, no action is taken 
	 * @param node see above
	 */
	public void removeFailingNode (WNode node)
	{
		if (node.isVirtualNode()) return;
		getNe().removeNode (node.getNe());
	}
	
	/** Adds a failing fiber to the SRG 
	 * @param fiber see above
	 */
	public void addFailingFiber (WFiber fiber)
	{
		getNe().addLink (fiber.getNe());
	}

	/** Adds failing fibers to the SRG 
	 * @param fibers see above
	 */
	public void addFailingFibers (Collection<WFiber> fibers)
	{
		for (WFiber e : fibers) getNe().addLink (e.getNe());
	}

	/** Adds failing IP links to the SRG 
	 * @param links see above
	 */
	public void addFailingIpLinks (Collection<WIpLink> links)
	{
		for (WIpLink e : links) getNe().addLink (e.getNe());
	}

	/** Adds one failing IP link to the SRG 
	 * @param link see above
	 */
	public void addFailingIpLink (WIpLink link)
	{
		addFailingIpLinks(Arrays.asList(link));
	}
	
	
	/** Removes a failing fiber from the SRG. If it is not in the SRG, no action is taken 
	 * @param fiber see above
	 */
	public void removeFailingFiber (WFiber fiber)
	{
		getNe().removeLink (fiber.getNe());
	}

	/** Removes a failing IP link from the SRG. If it is not in the SRG, no action is taken 
	 * @param ipLink see above
	 */
	public void removeFailingIpLink (WIpLink ipLink)
	{
		getNe().removeLink (ipLink.getNe());
	}

	/**
	 * <p>Returns the mean time to fail (MTTF) in hours of the SRG, that is, the average time between when it is repaired, and its next failure.</p>
	 * @return The MTTF in hours
	 */
	public double getMeanTimeToFailInHours()
	{
		return getNe().getMeanTimeToFailInHours ();
	}

	/**
	 * <p>Returns the availability (between 0 and 1) of the SRG. That is, the fraction of time that the SRG is in non failure state. </p>
	 * <p>Availability is computed as: MTTF/ (MTTF + MTTR)</p>
	 * @return MTTF/ (MTTF + MTTR)
	 */
	public double getAvailability()
	{
		return getMeanTimeToFailInHours() / (getMeanTimeToFailInHours() + getMeanTimeToRepairInHours());
	}

	/**
	 * <p>Sets the mean time to fail (MTTF) in hours of the SRG, that is, the average time between it is repaired, and the next failure.</p>
	 * @param value The new MTTF (it must be greater than zero)
	 * @return this element (for convenience)
	 */
	public WSharedRiskGroup setMeanTimeToFailInHours(double value)
	{
		if (value <= 0) throw new Net2PlanException ("A positive value is expected");
		getNe().setMeanTimeToFailInHours (value);
		return this;
	}

	/**
	 * <p>Returns the mean time to repair (MTTR) of the SRG, that is, the average time between a failure occurs, and it is repaired.</p>
	 * @return the MTTR in hours
	 */
	public double getMeanTimeToRepairInHours()
	{
		return getNe().getMeanTimeToRepairInHours ();
	}

	
	
	/**
	 * <p>Sets the mean time to repair (MTTR) in hours of the SRG, that is, the average time  between a failure occurs, and it is repaired.</p>
	 * @param value the new MTTR (negative values are not accepted)
	 * @return this element (for convenience)
	 */
	public WSharedRiskGroup setMeanTimeToRepairInHours(double value)
	{
		if (value < 0) throw new Net2PlanException ("A positive value is expected");
		getNe().setMeanTimeToRepairInHours(value);
		return this;
	}

	@Override
	void checkConsistency()
	{
		if (this.wasRemoved()) return;
		assert this.getFailingNodes().stream().allMatch(n->n.getSrgsThisElementIsAssociatedTo().contains(this));
		assert this.getFailingFibers().stream().allMatch(n->n.getSrgsThisElementIsAssociatedTo().contains(this));
	}

	@Override
	public WTYPE getWType() { return WTYPE.WSharedRiskGroup; }

}
