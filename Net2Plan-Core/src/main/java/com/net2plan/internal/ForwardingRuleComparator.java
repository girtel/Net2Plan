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



 




package com.net2plan.internal;

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Pair;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator for forwarding rules. First sort in ascending order of installed node 
 * identifier, then by demand identifier, and finally by link identifier.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
public class ForwardingRuleComparator implements Comparator<Pair<Long, Long>>, Serializable
{
	private static final long serialVersionUID = 1L;
	
	private final NetPlan netPlan;
	private final long layerId;
	
	/**
	 * Constructor.
	 * 
	 * @param netPlan Network design
	 * @since 0.3.0
	 */
	public ForwardingRuleComparator(NetPlan netPlan)
	{
		this(netPlan, netPlan.getNetworkLayerDefault ().getId());
	}
		
	/**
	 * Constructor.
	 * 
	 * @param netPlan Network design
	 * @param layerId Layer identifier
	 * @since 0.3.0
	 */
	public ForwardingRuleComparator(NetPlan netPlan, long layerId)
	{
		this.netPlan = netPlan;
		this.layerId = layerId;
	}
		
	@Override
	public int compare(Pair<Long, Long> o1, Pair<Long, Long> o2)
	{
		NetworkLayer layer = netPlan.getNetworkLayerFromId(layerId);
		Node node1 = netPlan.getLinkFromId (o1.getSecond()).getOriginNode();
		Node node2 = netPlan.getLinkFromId (o2.getSecond()).getOriginNode();
		
		int compareNodes = Long.compare(node1.getId (), node2.getId ());
		if (compareNodes != 0) return compareNodes;

		int compareDemands = o1.getFirst().compareTo(o2.getFirst());
		if (compareDemands != 0) return compareDemands;
		
		int compareLinks = o1.getSecond().compareTo(o2.getSecond());
		return compareLinks;
	}
}
