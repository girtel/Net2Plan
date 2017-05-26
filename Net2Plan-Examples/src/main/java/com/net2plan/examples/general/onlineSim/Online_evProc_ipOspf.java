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


import cern.colt.function.tdouble.DoubleDoubleFunction;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.IPUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.List;
import java.util.Map;

/** 
 * Implements the reactions of an IP network governed by the OSPF/ECMP forwarding policies, for given link weigths
 * 
 * This algorithm implements the reactions of an IP network governed by the OSPF/ECMP forwarding policies, for given link weigths, to the following events: 
 * <ul>
 * <li>SimEvent.DemandAdd: Adds a new IP traffic demand, and recomputes the routing (now including the new traffic).</li>
 * <li>SimEvent.DemandRemove: Remvoes an IP traffic demand, and recomputes the routing.</li>
 * <li>SimEvent.DemandModify: Modifies the offered traffic of a demand, and recomputes the routing.</li>
 * <li>SimEvent.LinkAdd: Adds a new IP link to the network, recomputes the routing tables and the routing.</li>
 * <li>SimEvent.LinkRemove: Removes an existing IP link in the network, recomputes the routing tables and the routing.</li>
 * <li>SimEvent.LinkModify: Modifies the capacity of an IP link. The routing is not modified, since OSPF does not react to capacity changes.</li>
 * <li>SimEvent.NodesAndLinksChangeFailureState: Fails/repairs the indicated nodes and/or IP links, and reacts to such failures as OSPF does: the failed links are removed from the routing tables, and the network routing recomputed.</li>
 * </ul>
 * 
 * This module can be used in conjunction with the {@code Online_evGen_generalGenerator} generator for simulating IP/OSPF networks. 
 * 
 * See the technology conventions used in Net2Plan built-in algorithms and libraries to represent IP/OSPF networks. 
 * @net2plan.keywords IP/OSPF, Network recovery: restoration
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class Online_evProc_ipOspf extends IEventProcessor
{
	private InputParameter ipMaximumE2ELatencyMs = new InputParameter ("ipMaximumE2ELatencyMs", (double) -1 , "Maximum end-to-end latency of the traffic of an IP demand to consider it as lost traffic (a non-positive value means no limit)");
	private NetworkLayer ipLayer;
	private double stat_trafficOffered , stat_trafficCarried , stat_trafficOversubscribed , stat_trafficOutOfLatencyLimit , stat_trafficOfDemandsTraversingOversubscribedLink;
	private double stat_transitoryInitTime , stat_timeLastChangeInNetwork;
	
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

		this.ipLayer = initialNetPlan.getNetworkLayer("IP"); if (ipLayer == null) throw new Net2PlanException ("IP layer not found");
		
		initialNetPlan.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , ipLayer);
		DoubleMatrix1D linkIGPWeightSetting = IPUtils.getLinkWeightVector(initialNetPlan , ipLayer);
		linkIGPWeightSetting.assign (initialNetPlan.getVectorLinkUpState(ipLayer) , new DoubleDoubleFunction () { public double apply (double x , double y) { return y == 1? x : Double.MAX_VALUE; }  } );
		IPUtils.setECMPForwardingRulesFromLinkWeights(initialNetPlan , linkIGPWeightSetting , ipLayer);
		
		finishTransitory(0);
		stat_timeLastChangeInNetwork = 0;
	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		/* Update with the network stage since the last event until now */
		final double timeSinceLastChange = event.getEventTime() - stat_timeLastChangeInNetwork;
		stat_trafficOffered += timeSinceLastChange * currentNetPlan.getVectorDemandOfferedTraffic(this.ipLayer).zSum();
		stat_trafficCarried += timeSinceLastChange * currentNetPlan.getVectorDemandCarriedTraffic(this.ipLayer).zSum();
		stat_trafficOversubscribed += timeSinceLastChange * currentNetPlan.getVectorLinkOversubscribedTraffic(this.ipLayer).zSum();
		stat_trafficOfDemandsTraversingOversubscribedLink += timeSinceLastChange * currentNetPlan.getVectorDemandOfferedTraffic(this.ipLayer).zDotProduct(currentNetPlan.getVectorDemandTraversesOversubscribedLink(this.ipLayer));
		if (ipMaximumE2ELatencyMs.getDouble () > 0) for (Demand d : currentNetPlan.getDemands (ipLayer)) if (d.getWorstCasePropagationTimeInMs() > ipMaximumE2ELatencyMs.getDouble ()) stat_trafficOutOfLatencyLimit += timeSinceLastChange * d.getOfferedTraffic();
		
		stat_timeLastChangeInNetwork = event.getEventTime();

		if (event.getEventObject () instanceof SimEvent.DemandAdd)
		{
			SimEvent.DemandAdd ev = (SimEvent.DemandAdd) event.getEventObject ();
			Demand d = currentNetPlan.addDemand(ev.ingressNode, ev.egressNode, ev.offeredTraffic, null, ev.layer); 
			ev.demandAddedToFillByProcessor = d;
		} else if (event.getEventObject () instanceof SimEvent.DemandRemove)
		{
			SimEvent.DemandRemove ev = (SimEvent.DemandRemove) event.getEventObject ();
			ev.demand.remove (); 
		} else if (event.getEventObject () instanceof SimEvent.DemandModify)
		{
			SimEvent.DemandModify ev = (SimEvent.DemandModify) event.getEventObject ();
			Demand d = ev.demand; 
//			System.out.print ("IPOSPF DemandModify: demand " + d + ", " + (ev.modificationIsRelativeToCurrentOfferedTraffic? "RELATIVE" : "ABSOLUTE") + " , old demand offered: " + d.getOfferedTraffic() + " ");
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
		else throw new Net2PlanException ("Unknown event type: " + event);

		/* Link weights from netPlan, but the down links have Double.MAX_VALUE weight */
		DoubleMatrix1D linkIGPWeightSetting = IPUtils.getLinkWeightVector(currentNetPlan , ipLayer);
		linkIGPWeightSetting.assign (currentNetPlan.getVectorLinkUpState(ipLayer) , new DoubleDoubleFunction () { public double apply (double x , double y) { return y == 1? x : Double.MAX_VALUE; }  } );
		IPUtils.setECMPForwardingRulesFromLinkWeights(currentNetPlan , linkIGPWeightSetting , ipLayer);
//		System.out.println ("-- CHANGE OSPF ROUTING: linkIGPWeightSetting: "  + linkIGPWeightSetting);
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
