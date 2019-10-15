/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.niw.WNetConstants.WTYPE;
import com.net2plan.utils.Pair;

/** Instances of this class are service chains, realizing service chain requests. A service chain should start in one of the origin nodes of the service chain, and end in 
 * one of the destination nodes of the service chain. The injection traffic of the service chain is the traffic produced by its origin node. Note that when the service chain 
 * traffic traverses a VNF, its volume will increase or decrease according to the expansion factor defined for that VNF.
 * 
 *  
 *
 */
public class WSharedRiskGroup extends WAbstractNetworkElement
{
	static String ATTRIBUTE_CURRENTEXPANSIONFACTOR = "ATTRIBUTE_CURRENTEXPANSIONFACTOR";
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
		return getNe().getLinks(getNet().getWdmNpLayer()).stream().map(n->new WFiber(n)).collect(Collectors.toCollection(TreeSet::new));
	}

	/** Adds a failing node to the SRG 
	 * @param node see above
	 */
	public void addFailingNode (WNode node)
	{
		if (node.isVirtualNode()) throw new Net2PlanException ("Cannot add virtual nodes to the SRG");
		getNe().addNode (node.getNe());
	}

	/** Removes this SRG
	 */
	public void remove () 
	{
		getNe().remove();
	}
	
	/** Removes a failing node from the SRG. If the node is not in the SRG, no action is taken 
	 * @param node see above
	 */
	public void removeFailingNode (WNode node)
	{
		if (node.isVirtualNode()) throw new Net2PlanException ("Cannot add virtual nodes to the SRG");
		getNe().removeNode (node.getNe());
	}
	
	/** Adds a failing fiber to the SRG 
	 * @param fiber see above
	 */
	public void addFailingFiber (WFiber fiber)
	{
		getNe().addLink (fiber.getNe());
	}

	/** Removes a failing fiber from the SRG. If it is not in the SRG, no action is taken 
	 * @param fiber see above
	 */
	public void removeFailingFiber (WFiber fiber)
	{
		getNe().removeLink (fiber.getNe());
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
