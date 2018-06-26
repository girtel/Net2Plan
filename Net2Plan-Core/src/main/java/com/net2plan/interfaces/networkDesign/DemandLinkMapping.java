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

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

class DemandLinkMapping
{
	private NetworkLayer demandSideLayer;
	private NetworkLayer linkSideLayer;
	final SortedMap<Demand, Link> demandLinkMapping;
	final SortedMap<MulticastDemand, SortedSet<Link>> multicastDemandLinkMapping;
	
	public DemandLinkMapping()
	{
		demandSideLayer = null;
		linkSideLayer = null;
		demandLinkMapping = new TreeMap<>();
		multicastDemandLinkMapping = new TreeMap<>();
	}
	
	public boolean isEmpty()
	{
		return (demandLinkMapping.isEmpty() && multicastDemandLinkMapping.isEmpty());
	}
	
	public void put(Demand demand , Link link)
	{
		demandLinkMapping.put(demand, link);
		if (demandSideLayer == null) demandSideLayer = demand.layer; else if (!demandSideLayer.equals(demand.layer)) throw new RuntimeException ("Bad");
		if (linkSideLayer == null) linkSideLayer = link.layer; else if (!linkSideLayer.equals(link.layer)) throw new RuntimeException ("Bad");
	}
	
	public void put(MulticastDemand demand , SortedSet<Link> links)
	{
		multicastDemandLinkMapping.put(demand, links);
		if (demandSideLayer == null) demandSideLayer = demand.layer; else if (!demandSideLayer.equals(demand.layer)) throw new RuntimeException ("Bad");
		if (linkSideLayer == null) linkSideLayer = links.iterator().next().layer; else if (!linkSideLayer.equals(links.iterator().next().layer)) throw new RuntimeException ("Bad");
	}
	
	public void remove(Demand demand)
	{
		demandLinkMapping.remove(demand);
	}
	
	public void remove(MulticastDemand demand)
	{
		multicastDemandLinkMapping.remove(demand);
	}

	public SortedMap<Demand,Link> getDemandMap () { return demandLinkMapping; }

	public SortedMap<MulticastDemand,SortedSet<Link>> getMulticastDemandMap () { return multicastDemandLinkMapping; }

	public NetworkLayer getDemandSideLayer () { return demandSideLayer; }
	public NetworkLayer getLinkSideLayer () { return linkSideLayer; }
	
	@Override
	public String toString()
	{
		return demandLinkMapping.toString() + " ; " + multicastDemandLinkMapping;
	}
}

