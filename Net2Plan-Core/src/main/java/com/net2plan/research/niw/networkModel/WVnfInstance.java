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

package com.net2plan.research.niw.networkModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.Resource;

/** An instance of this class is a VNF instance hosted in a node, of a given type, with a given capacity. The characteristics of the 
 * VNF instance are:
 * <ul>
 * <li>Hosting node: the node where this VNF instance exists</li>
 * <li>Type: The type of VNF</li>
 * <li>Name: A user-defined string with the name of the VNF instance</li>
 * <li>Processing time in ms: The processing time added to the traversing traffic.</li>
 * <li>CPU, RAM, HD consumed: The amount of such resources of the node consumed by the VNF instance</li>
 * <li>Capacity: measured as the amount of input Gbps that this VNF instance can process</li>
 * <li></li>
 * <li></li>
 * <li></li>
 * </ul>
 */
public class WVnfInstance extends WAbstractNetworkElement
{
	WVnfInstance(Resource r) { super (r); this.r = r; }

	final private Resource r;

	@Override
	public Resource getNe() { return r; }
	
	/** Returns the hosting node of the VNF instance
	 * @return see above
	 */
	public WNode getHostingNode () { return new WNode (r.getHostNode().get()); }
	
	/** Returns the VNF type of this instance
	 * @return see above
	 */
	public String getType () { return r.getType(); }
	
	/** Returns the user-defined name of this instance
	 * @return see above
	 */
	public String getName () { return r.getName(); }
	/** Sets the user-defined name for this instance
	 * @param name see above
	 */
	public void setName (String name) { r.setName(name); }
	
	/** Returns the processing time in ms added to the traversing traffic to account for the end-to-end latencies
	 * @return see above
	 */
	public double getProcessingTimeInMs () { return r.getProcessingTimeToTraversingTrafficInMs(); }
	/** Sets the processing time in ms added to the traversing traffic to account for the end-to-end latencies
	 * @param procTimeInMs see above
	 */
	public void setProcessingTimeInMs (double procTimeInMs) { r.setProcessingTimeToTraversingTrafficInMs(procTimeInMs); }
	
	/** Returns the service chains that are traversing this VNF instance
	 * @return see above
	 */
	public SortedSet<WServiceChain> getTraversingServiceChains () { return r.getTraversingRoutes().stream().map(rr->new WServiceChain(rr)).collect(Collectors.toCollection(TreeSet::new)); }

	/** Returns the service chains requests that have at least one service chain that is traversing this VNF instance
	 * @return see above
	 */
	public SortedSet<WServiceChainRequest> getTraversingServiceChainRequests () { return getTraversingServiceChains().stream().map(rr->rr.getServiceChainRequest()).collect(Collectors.toCollection(TreeSet::new)); }

	/** Returns the VNF capacity (measured as Gbps of the traversing service chains, at the input of the VNF) that is 
	 * currently consumed by the traversing service chains 
	 * @return see above
	 */
	public double getOccupiedCapacityInGbps () { return r.getOccupiedCapacity(); }
	
	/** Returns the VNF capacity occupied by a given service chain, or zero if (i) the service chain is down, (ii) the service chain is not traversing this VNF instance.
	 * The VNF capacity occupied is the service chain traffic at the input of the VNF   
	 * @param sc see above
	 * @return see above
	 */
	public double getOccupiedCapacityByTraversingRouteInGbps (WServiceChain sc) 
	{
		return r.getTraversingRouteOccupiedCapacity(sc.getNe());
	}

	/** Sets the VNF capacity and RAM/CPU/HD occupation of this instance
	 * @param newProcessingCapacityInGbps the new processing capacity of the VNF, measured as the maximum amount of Gbps considering the traffic of the traversing service chains, at the input of the VNF 
	 * @param newOccupiedCpu the amount of CPU occupied by this instance
	 * @param newOccupiedRam the amount of RAM in GBytes occupied by this instance
	 * @param newOccupiedHd the amount of hard disk in Gbytes occupied by this instance
	 */
	public void setCapacityInGbpsOfInputTraffic (double newProcessingCapacityInGbps , double newOccupiedCpu , double newOccupiedRam , double newOccupiedHd)
	{
		final Map<Resource,Double> map = new HashMap<> ();
		map.put (getHostingNode().getCpuBaseResource() , newOccupiedCpu);
		map.put (getHostingNode().getRamBaseResource() , newOccupiedRam);
		map.put (getHostingNode().getHdBaseResource() , newOccupiedHd);		
		r.setCapacity(newProcessingCapacityInGbps, map);
	}

	/** Sets the capacity of this VNF instance as well as the CPU, RAM, HD consumption, so the VNF capacity is the minimum multiple of the
	 * capacity of the instance VNF type. As an example, if the VNF type sets a per-instance capacity of 1 Gbps, consuming 1 CPU, 2 GBs RAM and 3 GBs HD, and the current 
	 * VNF instance receives 1.5 GBps of traffic, the new VNF instance is scaled to have 2 Gbps of capacity, consuming 2 CPUs, 4 GBs RAM and 6 GBs HD.
	 */
	public void scaleVnfCapacityAndConsumptionToBaseInstanceMultiple ()
	{
		final WVnfType vnfType = getNet().getVnfType(this.getType()).orElse(null);
		if (vnfType == null) throw new Net2PlanException ("No detailed information of the VNF type is available");
		final double occupiedCapacityGbps = this.getOccupiedCapacityInGbps();
		final double capacityOfEachSubinstance = vnfType.getMaxInputTrafficPerVnfInstance_Gbps();
		final int numSubinstances = (int) Math.ceil(occupiedCapacityGbps / capacityOfEachSubinstance);
		setCapacityInGbpsOfInputTraffic(numSubinstances * capacityOfEachSubinstance , numSubinstances * vnfType.getOccupCpu(), numSubinstances * vnfType.getOccupRamGBytes(), numSubinstances * vnfType.getOccupHdGBytes());
	}
	
	/** Returns the current capacity of this VNF
	 * @return see above
	 */
	public double getCurrentCapacityInGbps () { return r.getCapacity(); } 
	
	/** Returns the amount of occupied CPUs by this VNf instance
	 * @return see above
	 */
	public double getOccupiedCpus () 
	{ 
		return r.getCapacityOccupiedInBaseResource(getHostingNode().getCpuBaseResource());
	}
	/** Returns the amount of occupied hard-disk in Gbytes by this VNf instance
	 * @return see above
	 */
	public double getOccupiedHdInGB () 
	{ 
		return r.getCapacityOccupiedInBaseResource(getHostingNode().getHdBaseResource());
	}
	/** Returns the amount of occupied RAM in Gbytes by this VNf instance
	 * @return see above
	 */
	public double getOccupiedRamInGB () 
	{ 
		return r.getCapacityOccupiedInBaseResource(getHostingNode().getRamBaseResource());
	}

	public String toString () { return "VnfInstance(" + this.getType() + "," + getHostingNode() + ")"; }

	/** Removes the current VNF instance, and all the service chains traversing it
	 */
	public void remove () { r.remove(); }

	/** Returns the WVnfType object assigned to this instance type, if any 
	 * @return see above
	 */
	public Optional<WVnfType> getVnfType ()
	{
		return getNet().getVnfType(this.getType ());
	}

	
	boolean isBaseResourceNotAVnf () { return this.getType().contains(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER); }
	
}
