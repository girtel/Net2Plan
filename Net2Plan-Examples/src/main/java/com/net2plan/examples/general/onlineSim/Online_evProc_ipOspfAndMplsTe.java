package com.net2plan.examples.general.onlineSim;
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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.net2plan.examples.ocnbook.offline.Offline_fa_ospfWeightOptimization_ACO;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.libraries.IPUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import cern.colt.function.tdouble.DoubleDoubleFunction;
import cern.colt.matrix.tdouble.DoubleMatrix1D;

/** 
 * Implements a subset of the reactions of an IP network, where demands of the hop-bu-hop routing type are routed according to 
 * OSPF/ECMP (and thus, its path depends on the IGP weights), while demands of the source-routing type are assumed to be MPLS-TE tunnels. 
 * MPLS-TE tunnels can be of different types, in its reaction to failures: 
 * <ul>
 * <li> CSPF-dynamic. The demand is realized with one route, with a carried traffic and occupied capacity equal to the demand offered traffic. 
 * The route is computed as the shortest path according to IGP weights, using only those links with enough non-used capacity. 
 * For the used capacity, we count only the capacity occupied by other MPLS-TE tunnels reserving at this moment traffic in that link. If no shortest path is found, the full demand traffic is dropped.</li>
 * <li> 1+1 FRR link disjoint. The demand is realized with two routes. The path route is set initially as the 1+1 link disjoint path of minimum cost, using IGP weights. If no two disjoint paths 
 * are found, two routes with maximum disjointness are computed. Both routes occupy the full demand offered traffic in all their links. The main route will carry traffic, unless failed. If so,
 * the other route will carry traffic. If both are failed, none route carries traffic, but both routes still reserve the bandwidth in the links.</li>
 * </ul> 
 * 
 * This algorithm implements the reactions to the following events: 
 * <ul>
 * <li>SimEvent.DemandAdd: Adds a new IP traffic demand, and recomputes the routing (now including the new traffic).</li>
 * <li>SimEvent.DemandRemove: Remvoes an IP traffic demand, and recomputes the routing.</li>
 * <li>SimEvent.DemandModify: Modifies the offered traffic of a demand, and recomputes the routing.</li>
 * <li>SimEvent.LinkAdd: Adds a new IP link to the network, recomputes the routing tables and the routing.</li>
 * <li>SimEvent.LinkRemove: Removes an existing IP link in the network, recomputes the routing tables and the routing.</li>
 * <li>SimEvent.LinkModify: Modifies the capacity of an IP link. </li>
 * <li>SimEvent.NodesAndLinksChangeFailureState: Fails/repairs the indicated nodes and/or IP links, and reacts to such failures as described above.</li>
 * </ul>
 * 
 * This module can be used in conjunction with the {@code Online_evGen_generalGenerator} generator for simulating IP/OSPF networks. 
 * 
 * See the technology conventions used in Net2Plan built-in algorithms and libraries to represent IP/OSPF networks. 
 * @net2plan.keywords IP/OSPF, Network recovery: restoration
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Online_evProc_ipOspfAndMplsTe extends IEventProcessor
{
	private double stat_trafficOffered , stat_trafficCarried , stat_trafficOversubscribed , stat_trafficOutOfLatencyLimit , stat_trafficOfDemandsTraversingOversubscribedLink;
	private double stat_transitoryInitTime , stat_timeLastChangeInNetwork;
	private Map<String, String> net2planParameters;
	
	private InputParameter mplsTeTunnelType = new InputParameter ("mplsTeTunnelType", "#select# cspf-dynamic 1+1-FRR-link-disjoint" , "The type of path computation for MPLS-TE tunnels");
	private InputParameter ospfWeightSettingType = new InputParameter ("ospfWeightSettingType", "#select# static dynamic" , "If static, OSPF weights are not changed. If dynamic, they are recomputed using a built-in heuristic algorithm, ran during the user-defined number of seconds");
	private InputParameter ospfWeightSetting_maxHeuristicTimeInSecs = new InputParameter ("ospfWeightSetting_maxHeuristicTimeInSecs",  (double) 5.0 , "Maximum running time of the OSPF weight setting algorithm" , 0 , false , Double.MAX_VALUE , true);

	@Override
	public String getDescription()
	{
		return "Implements the reactions of an IP network governed by the OSPF/ECMP forwarding policies, for given link weigths";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	@Override
	public void initialize(NetPlan initialNetPlan, Map<String, String> algorithmParameters, Map<String, String> simulationParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		
		this.net2planParameters = new HashMap<> (net2planParameters);
		
		updateState(initialNetPlan);
		
		finishTransitory(0);
		stat_timeLastChangeInNetwork = 0;
	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		/* Update with the network stage since the last event until now */
		NetworkLayer ipLayer = currentNetPlan.getNetworkLayer("IP"); 
		if (ipLayer == null) ipLayer = currentNetPlan.getNetworkLayerDefault(); 
		final double timeSinceLastChange = event.getEventTime() - stat_timeLastChangeInNetwork;
		stat_trafficOffered += timeSinceLastChange * currentNetPlan.getVectorDemandOfferedTraffic(ipLayer).zSum();
		stat_trafficCarried += timeSinceLastChange * currentNetPlan.getVectorDemandCarriedTraffic(ipLayer).zSum();
		stat_trafficOversubscribed += timeSinceLastChange * currentNetPlan.getVectorLinkOversubscribedTraffic(ipLayer).zSum();
		stat_trafficOfDemandsTraversingOversubscribedLink += timeSinceLastChange * currentNetPlan.getVectorDemandOfferedTraffic(ipLayer).zDotProduct(currentNetPlan.getVectorDemandTraversesOversubscribedLink(ipLayer));
		for (Demand d : currentNetPlan.getDemands (ipLayer)) if (d.getWorstCasePropagationTimeInMs() > d.getBlockedTraffic()) stat_trafficOutOfLatencyLimit += timeSinceLastChange * d.getOfferedTraffic();
		
		stat_timeLastChangeInNetwork = event.getEventTime();

		if (event.getEventObject () instanceof SimEvent.DemandAdd)
		{
			SimEvent.DemandAdd ev = (SimEvent.DemandAdd) event.getEventObject ();
			Demand d = currentNetPlan.addDemand(ev.ingressNode, ev.egressNode, ev.offeredTraffic, ev.routingType , null, ev.layer); 
			ev.demandAddedToFillByProcessor = d;
		} else if (event.getEventObject () instanceof SimEvent.DemandRemove)
		{
			SimEvent.DemandRemove ev = (SimEvent.DemandRemove) event.getEventObject ();
			ev.demand.remove (); 
		} else if (event.getEventObject () instanceof SimEvent.DemandModify)
		{
			SimEvent.DemandModify ev = (SimEvent.DemandModify) event.getEventObject ();
			Demand d = ev.demand; 
			if (ev.modificationIsRelativeToCurrentOfferedTraffic) 
				d.setOfferedTraffic(d.getOfferedTraffic() + ev.offeredTraffic);
			else
				d.setOfferedTraffic(ev.offeredTraffic);
		} else if (event.getEventObject () instanceof SimEvent.LinkAdd)
		{
			SimEvent.LinkAdd ev = (SimEvent.LinkAdd) event.getEventObject ();
			Link newLink = currentNetPlan.addLink (ev.originNode , ev.destinationNode, ev.capacity , ev.lengthInKm , ev.propagationSpeedInKmPerSecond , null , ev.layer);
			ev.linkAddedToFillByProcessor = newLink;
		} else if (event.getEventObject () instanceof SimEvent.LinkRemove)
		{
			SimEvent.LinkRemove ev = (SimEvent.LinkRemove) event.getEventObject ();
			ev.link.remove();
		} else if (event.getEventObject () instanceof SimEvent.LinkModify)
		{
			SimEvent.LinkModify ev = (SimEvent.LinkModify) event.getEventObject ();
			ev.link.setCapacity(ev.newCapacity);
		} else if (event.getEventObject () instanceof SimEvent.NodesAndLinksChangeFailureState)
		{
			SimEvent.NodesAndLinksChangeFailureState ev = (SimEvent.NodesAndLinksChangeFailureState) event.getEventObject ();
			currentNetPlan.setLinksAndNodesFailureState(ev.linksToUp , ev.linksToDown , ev.nodesToUp , ev.nodesToDown);
		}
		//else throw new Net2PlanException ("Unknown event type: " + event);

		/* Link weights from netPlan, but the down links have Double.MAX_VALUE weight */
		updateState(currentNetPlan);
//		System.out.println ("-- process CHANGE OSPF ROUTING: linkIGPWeightSetting: "  + linkIGPWeightSetting);
	}

	
	private void updateState (NetPlan np)
	{
		NetworkLayer ipLayer = np.getNetworkLayer("IP"); 
		if (ipLayer == null) ipLayer = np.getNetworkLayerDefault(); 
		
		/* Routing of OSPF traffic */
		if (ospfWeightSettingType.getString().equals("dynamic"))
		{
			final IAlgorithm ospfAlg = new Offline_fa_ospfWeightOptimization_ACO();
			final Map<String,String> algParam = new HashMap<> ();
			for (Triple<String,String,String> par : ospfAlg.getParameters())
				algParam.put(par.getFirst(), par.getSecond());
			algParam.put("algorithm_maxExecutionTimeInSeconds", new Double(this.ospfWeightSetting_maxHeuristicTimeInSecs.getDouble() > 0? this.ospfWeightSetting_maxHeuristicTimeInSecs.getDouble() : 5.0).toString());
			algParam.put("aco_initializationType", "ones");
			ospfAlg.executeAlgorithm(np, algParam, net2planParameters);
		}
		DoubleMatrix1D linkIGPWeightSetting = IPUtils.getLinkWeightVector(np , ipLayer);
		linkIGPWeightSetting.assign (np.getVectorLinkUpState(ipLayer) , new DoubleDoubleFunction () { public double apply (double x , double y) { return y == 1? x : Double.MAX_VALUE; }  } );
		IPUtils.setECMPForwardingRulesFromLinkWeights(np , linkIGPWeightSetting  , np.getDemands(ipLayer).stream().filter(d->!d.isSourceRouting()).collect(Collectors.toSet()) , ipLayer);
		
		/* To account for the occupation of IP links because of MPLS-TE tunnels */
		final int E = np.getNumberOfLinks(ipLayer);
		final double [] occupiedBwPerLinks = new double [E]; 
		final BiFunction<Collection<Link> , Double, Boolean> isEnoughNonReservedBwAvaialable = (l,t)->l.stream().allMatch(e->e.getCapacity() - occupiedBwPerLinks[e.getIndex ()] + Configuration.precisionFactor >= t);
		final BiConsumer<Collection<Link> , Double> reserveBwInLinks = (l,t)->{ for (Link e : l) occupiedBwPerLinks[e.getIndex ()] += t; }; 
		final boolean isCspf = mplsTeTunnelType.getString().equals("cspf-dynamic");
		final boolean is11FrrLinkDisjoint = !isCspf;
		/* Routing of MPLS-TE traffic */
		for (Demand d : np.getDemands(ipLayer))
		{
			if (!d.isSourceRouting()) continue;
			final double bwtoReserve = d.getOfferedTraffic();
			d.removeAllRoutes();
			if (isCspf)
			{
				final List<Link> linksValid = new ArrayList<> (E);
				final Map<Link,Double> weightsValid = new HashMap<> ();
				for (Link e : np.getLinks(ipLayer)) if (e.getCapacity() - occupiedBwPerLinks[e.getIndex()] + Configuration.precisionFactor >= bwtoReserve) { linksValid.add(e); weightsValid.put(e, IPUtils.getLinkWeight(e)); }
				final List<Link> sp = GraphUtils.getShortestPath(np.getNodes(), linksValid, d.getIngressNode(), d.getEgressNode(), weightsValid);
				if (sp.isEmpty())
				{
					continue;
				}
				reserveBwInLinks.accept(sp, bwtoReserve);
				np.addRoute(d, bwtoReserve, bwtoReserve, sp, null);
			}
			else if (is11FrrLinkDisjoint)
			{
				/* Make the tunnel paths. Fixed: does not matter the failed links */
				final List<Link> linksValidControlPlaneEvenIfDownDataPlane = new ArrayList<> (E);
				final SortedMap<Link,Double> weightsValid = new TreeMap<> ();
				for (Link e : np.getLinks(ipLayer)) if (e.getCapacity() - occupiedBwPerLinks[e.getIndex()] + Configuration.precisionFactor >= bwtoReserve) { linksValidControlPlaneEvenIfDownDataPlane.add(e); weightsValid.put(e, IPUtils.getLinkWeight(e)); }
				final List<List<Link>> sps = GraphUtils.getTwoMaximumLinkAndNodeDisjointPaths(np.getNodes(), linksValidControlPlaneEvenIfDownDataPlane, d.getIngressNode(), d.getEgressNode(), weightsValid);
				if (sps.size() != 2) continue; 

				final Route r1 = np.addRoute(d, bwtoReserve, bwtoReserve, sps.get(0), null);
				final Route r2 = np.addRoute(d, 0, bwtoReserve, sps.get(1), null);
				r1.addBackupRoute(r2);
				
				final boolean tunnelReserves = isEnoughNonReservedBwAvaialable.apply(r1.getSeqLinks(), bwtoReserve) && isEnoughNonReservedBwAvaialable.apply(r2.getSeqLinks(), bwtoReserve);
				if (tunnelReserves)
				{
					reserveBwInLinks.accept(r1.getSeqLinks(), bwtoReserve);
					reserveBwInLinks.accept(r2.getSeqLinks(), bwtoReserve);
				}
				final boolean r1Up = !r1.isDown() && !r1.isTraversingZeroCapLinks(); 
				final boolean r2Up = !r2.isDown() && !r2.isTraversingZeroCapLinks(); 
				if (r1Up && tunnelReserves)
				{
					r1.setCarriedTraffic(bwtoReserve, bwtoReserve);
					r2.setCarriedTraffic(0, bwtoReserve);
				}
				else if (r2Up && tunnelReserves)
				{
					r1.setCarriedTraffic(0, bwtoReserve);
					r2.setCarriedTraffic(bwtoReserve, bwtoReserve);
				}
				else
				{
					r1.setCarriedTraffic(0, bwtoReserve);
					r2.setCarriedTraffic(0, bwtoReserve);
				}
			} else throw new RuntimeException ();
		}
	}
	
	@Override
	public void finishTransitory(double simTime)
	{
		this.stat_trafficOffered = 0;
		this.stat_trafficCarried = 0;
		this.stat_trafficOversubscribed = 0;
		this.stat_trafficOutOfLatencyLimit = 0;
		this.stat_trafficOfDemandsTraversingOversubscribedLink = 0;
		this.stat_transitoryInitTime = simTime;
	}

	@Override
	public String finish(StringBuilder output, double simTime)
	{
		final double dataTime = simTime - stat_transitoryInitTime;
		if (dataTime <= 0) { output.append ("<p>No time for data acquisition</p>"); return ""; }
		output.append (String.format("<p>Time average traffic offered / carried / blocking (%f , %f , %f) </p>", stat_trafficOffered / dataTime, stat_trafficCarried / dataTime , stat_trafficOffered == 0? 0 : 1 - (stat_trafficCarried / stat_trafficOffered)));
		output.append (String.format("<p>Time average traffic oversubscribed: %f (sum traffic oversubscription in the links) </p>", stat_trafficOversubscribed / dataTime));
		output.append (String.format("<p>Time average traffic of demands with worse case propagation time out of latency limits: %f</p>", stat_trafficOutOfLatencyLimit / dataTime));
		output.append (String.format("<p>Time average traffic of demands which traverse an oversubscribed link (summing all the demand offered traffic, even if only a fraction of traffic traverses oversubscribed links): %f</p>", stat_trafficOfDemandsTraversingOversubscribedLink / dataTime));
		return "";
	}

}
