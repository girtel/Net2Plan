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


import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.IPUtils;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 
 * <p>Implements the reactions of an IP over WDM multilayer network, where the IP traffic is carried over lightpaths routed over 
 * a topology of fiber links with a fixed or a flexible wavelength grid.</p>
 * 
 * <p>The design follows the assumptions described in {@link com.net2plan.libraries.WDMUtils WDMUtils} Net2Plan library</p>
 * 
 * <p>This algorithm implements the reactions of an IP over WDM multilayer network. Internally, the algorithm just coordinates the reactions of the WDM layer and the 
 * IP layer, each of them implemented by the {@code Online_evProv_wdm} and {@code Online_evProc_ipOspf} modules. The coordination actions are basically during the 
 * failure processes, propagating the failures and repairs at the WDM layer, as links down/up events in the IP layer.</p> 
 * 
 * <p>The algorithm reacts to the following events:</p>
 * <ul>
 * <li>WDMUtils.LightpathAdd: Adds the corresponding lightpath to the network, if enough resources exist for it, calling to the WDM layer module. If so, it is added also as an IP link, and the IP routing modified, by calling the IP module.</li>
 * <li>WDMUtils.LightpathRemove: Removes the corresponding lightpath, releasing the resources and updating both layers.</li>
 * <li>SimEvent.DemandAdd: (only for the IP layer) Forwards this event to the IP layer module. The target module behaves appropriately.</li>
 * <li>SimEvent.DemandModify: Forwards this event to the WDM or IP layer modules, depending on the event associated layer. The target module behaves appropriately.</li>
 * <li>SimEvent.DemandRemove: (only for the IP layer) Forwards this event to the IP layer module. The target module behaves appropriately.</li>
 * <li>SimEvent.NodesAndLinksChangeFailureState: This event is first forwarded to the WDM layer module, with the fails/repairs associated to its layer. Then, the event 
 * is sent to the IP layer, updating the IP links failures and repairs, depending on the results of the WDM layer. For instance, any WDM failures that result in a WDM 
 * demand carrying no traffic (affected by a failure for which it could not recover), is propagated to the IP layer as a failing IP link.</li>
 * </ul>
 * 
 * <p>This module can be used in conjunction with the {@code Online_evGen_ipOverWdm} generator for simulating IP over WDM designs.</p> 
 * 
 * <p>See the technology conventions used in Net2Plan built-in algorithms and libraries to represent IP and WDM networks.</p> 
 * @net2plan.keywords IP/OSPF, WDM, Multilayer, Network recovery: protection, Network recovery: restoration
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Online_evProc_ipOverWdm extends IEventProcessor
{
	private InputParameter wdmNumFrequencySlotsPerFiber = new InputParameter ("wdmNumFrequencySlotsPerFiber", (int) 40 , "Set the number of frequency slots per fiber. If < 1, the number of slots set in the input file is used.");
	private InputParameter wdmRwaType = new InputParameter ("wdmRwaType", "#select# srg-disjointness-aware-route-first-fit alternate-routing least-congested-routing load-sharing" , "Criteria to decide the route of a connection among the available paths");
	private InputParameter wdmK = new InputParameter ("wdmK", (int) 2 , "Maximum number of admissible paths per demand" , 1 , Integer.MAX_VALUE);
	private InputParameter wdmRandomSeed = new InputParameter ("wdmRandomSeed", (long) 1 , "Seed for the random generator (-1 means random)");
	private InputParameter wdmMaxLightpathNumHops = new InputParameter ("wdmMaxLightpathNumHops", (int) -1 , "A lightpath cannot have more than this number of hops. A non-positive number means this limit does not exist");
	private InputParameter wdmRemovePreviousLightpaths = new InputParameter ("wdmRemovePreviousLightpaths", false  , "If true, previous lightpaths are removed from the system during initialization.");
//	private InputParameter wdmProtectionTypeToNewRoutes = new InputParameter ("wdmProtectionTypeToNewRoutes", "#select# none 1+1-link-disjoint 1+1-node-disjoint 1+1-srg-disjoint" , "");
//	private InputParameter wdmProtectionTypeIfProtectionToNewRoutes = new InputParameter ("wdmProtectionTypeIfProtectionToNewRoutes", "#select# 1+1-link-disjoint 1+1-node-disjoint 1+1-srg-disjoint" , "");
	private InputParameter wdmDefaultAndNewRouteRevoveryType = new InputParameter ("wdmDefaultAndNewRouteRevoveryType", "#select# none restoration 1+1-link-disjoint 1+1-node-disjoint 1+1-srg-disjoint" , "New lightpaths are not protected, or are protected by a 1+1 link disjoint, or a node disjoint or a SRG disjoint lightpath");
	private InputParameter wdmTransponderTypesInfo = new InputParameter ("wdmTransponderTypesInfo", "10 1 1 9600 1" , "Transpoder types separated by \";\" . Each type is characterized by the space-separated values: (i) Line rate in Gbps, (ii) cost of the transponder, (iii) number of slots occupied in each traversed fiber, (iv) optical reach in km (a non-positive number means no reach limit), (v) cost of the optical signal regenerator (regenerators do NOT make wavelength conversion ; if negative, regeneration is not possible).");

	private InputParameter ipMaximumE2ELatencyMs = new InputParameter ("ipMaximumE2ELatencyMs", (double) -1 , "Maximum end-to-end latency of the traffic of an IP demand to consider it as lost traffic (a non-positive value means no limit)");

//	private InputParameter ipOverWdmNetworkRecoveryType = new InputParameter ("ipOverWdmNetworkRecoveryType", "#select# static-lps-OSPF-rerouting 1+1-lps-OSPF-rerouting lp-restoration-OSPF-rerouting" , "The recovery type the network will apply. If static lps, the VT is overdimensioned to tolerate single SRG failures. In the 1+1 case, link disjoit backup lps are created. If lps are 1+1 protected or have lp restoration, the VT is dimensioned to carry all IP traffic in the no failure state.");
	
	private NetworkLayer wdmLayer , ipLayer;
	private IEventProcessor ospfNetwork , wdmNetwork;
	
	@Override
	public String getDescription()
	{
		return "Implements the reactions of an IP over WDM multilayer network, where the IP traffic is carried over fixed rate lightpaths, routed over a topology of fiber links with a fixed wavelength grid";
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
		this.wdmLayer = initialNetPlan.getNetworkLayer("WDM"); if (wdmLayer == null) throw new Net2PlanException ("WDM layer not found");
		if (initialNetPlan.getNumberOfLayers() != 2) throw new Net2PlanException ("The input design must have two layers");

		this.ospfNetwork = new Online_evProc_ipOspf();
		this.wdmNetwork = new Online_evProc_wdm();

		Map<String,String> wdmParam = InputParameter.createMapFromInputParameters(new InputParameter [] 
				{ wdmNumFrequencySlotsPerFiber , wdmRwaType ,  wdmK , wdmRandomSeed , wdmTransponderTypesInfo ,  
				wdmMaxLightpathNumHops , wdmRemovePreviousLightpaths , wdmDefaultAndNewRouteRevoveryType } );

		this.wdmNetwork.initialize(initialNetPlan , wdmParam , simulationParameters , net2planParameters);

		Map<String,String> ipParam = InputParameter.createMapFromInputParameters(new InputParameter [] { ipMaximumE2ELatencyMs } );
		this.ospfNetwork.initialize(initialNetPlan , ipParam , simulationParameters , net2planParameters);

		Set<Link> ipLinksDownBecauseOfWDMLayer = new HashSet<Link> (); for (Link ipLink : initialNetPlan.getLinks (ipLayer)) if (ipLink.getCapacity() == 0)  ipLinksDownBecauseOfWDMLayer.add (ipLink); 
		SimEvent.NodesAndLinksChangeFailureState evIp = new SimEvent.NodesAndLinksChangeFailureState(null , null , null , ipLinksDownBecauseOfWDMLayer);
		ospfNetwork.processEvent(initialNetPlan , new SimEvent(0 , SimEvent.DestinationModule.EVENT_GENERATOR , -1 , evIp));
	}
	
	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		/* First, the WDM layer reacts */
		if (event.getEventObject () instanceof WDMUtils.LightpathAdd)
		{
			WDMUtils.LightpathAdd addLpEvent = (WDMUtils.LightpathAdd) event.getEventObject ();
			wdmNetwork.processEvent(currentNetPlan , event);
			if (addLpEvent.lpAddedToFillByProcessor != null)
			{
				Route addedLp = addLpEvent.lpAddedToFillByProcessor;
				/* The lightpath was added */
				SimEvent.LinkAdd evIp = new SimEvent.LinkAdd(addLpEvent.ingressNode , addLpEvent.egressNode , ipLayer , addLpEvent.lineRateGbps , addedLp.getLengthInKm() , addedLp.getPropagationSpeedInKmPerSecond());
				ospfNetwork.processEvent(currentNetPlan , new SimEvent(event.getEventTime() , SimEvent.DestinationModule.EVENT_GENERATOR , -1 , evIp));
				if (evIp.linkAddedToFillByProcessor == null) throw new RuntimeException ("Bad");
				IPUtils.setLinkWeight(evIp.linkAddedToFillByProcessor , (double) 1);
				final Demand lpWdmDemand = addedLp.getDemand(); if (lpWdmDemand.getRoutes().size() != 1) throw new Net2PlanException ("Each lightpath must be hosted in its own WDM demand");
				lpWdmDemand.coupleToUpperLayerLink(evIp.linkAddedToFillByProcessor);
			}
		}
		else if (event.getEventObject () instanceof WDMUtils.LightpathRemove)
		{
			WDMUtils.LightpathRemove removeLpEvent = (WDMUtils.LightpathRemove) event.getEventObject ();
			SimEvent.LinkRemove evIp = new SimEvent.LinkRemove(removeLpEvent.lp.getDemand().getCoupledLink());
			ospfNetwork.processEvent(currentNetPlan , new SimEvent(event.getEventTime() , SimEvent.DestinationModule.EVENT_GENERATOR , -1 , evIp));
			SimEvent.DemandRemove evWdmDemandRemove = new SimEvent.DemandRemove(removeLpEvent.lp.getDemand());
			wdmNetwork.processEvent(currentNetPlan , new SimEvent(event.getEventTime() , SimEvent.DestinationModule.EVENT_GENERATOR , -1 , evWdmDemandRemove));
		}
		else if (event.getEventObject () instanceof SimEvent.DemandModify) 
		{
			if (((SimEvent.DemandModify) event.getEventObject ()).demand.getLayer() == wdmLayer) wdmNetwork.processEvent(currentNetPlan , event); 
			else if (((SimEvent.DemandModify) event.getEventObject ()).demand.getLayer() == ipLayer) ospfNetwork.processEvent(currentNetPlan , event);
		} 
		/* Events for the IP layer */
		else if (event.getEventObject () instanceof SimEvent.DemandAdd)
		{
			if (((SimEvent.DemandAdd) event.getEventObject ()).layer == ipLayer) ospfNetwork.processEvent(currentNetPlan , event);
			throw new Net2PlanException ("This algorithm does not handle events of the type DemandAdd, for the WDM layer");
		}
		else if (event.getEventObject () instanceof SimEvent.DemandRemove)
		{
			if (((SimEvent.DemandRemove) event.getEventObject ()).demand.getLayer() == ipLayer) ospfNetwork.processEvent(currentNetPlan , event);
			throw new Net2PlanException ("This algorithm does not handle events of the type DemandRemove, for the WDM layer");
		}
		else if (event.getEventObject () instanceof SimEvent.NodesAndLinksChangeFailureState)
		{
			/* each layer processes all node events, and the link events of its layer */
			SimEvent.NodesAndLinksChangeFailureState ev = (SimEvent.NodesAndLinksChangeFailureState) event.getEventObject ();
			Set<Link> ipLinksUp = new HashSet<Link> ();
			Set<Link> wdmLinksUp = new HashSet<Link> ();
			Set<Link> ipLinksDown = new HashSet<Link> ();
			Set<Link> wdmLinksDown = new HashSet<Link> ();
//			System.out.println ("-- ev.linksDown: " + ev.linksDown);

			if (ev.linksToDown != null) for (Link e : ev.linksToDown) if (e.getLayer () == ipLayer) ipLinksDown.add (e); else if (e.getLayer () == wdmLayer) wdmLinksDown.add (e); 
			if (ev.linksToUp != null) for (Link e : ev.linksToUp) if (e.getLayer () == ipLayer) ipLinksUp.add (e); else if (e.getLayer () == wdmLayer) wdmLinksUp.add (e); 

//			System.out.println ("-- original ip links up: " + ipLinksUp);
//			System.out.println ("-- original ip links down: " + ipLinksDown);
//			System.out.println ("-- original wdm links up: " + wdmLinksUp);
//			System.out.println ("-- original wdm links down: " + wdmLinksDown);
			
			/* Failures at WDM layer are processed */
//			System.out.println ("-- process wdm links up/down: " + wdmLinksDown + ", wdmLinksUp: " + wdmLinksUp + ", ev.nodesToUp : " + ev.nodesToUp  + ", ev.nodesToDown: " +  ev.nodesToDown);
			SimEvent.NodesAndLinksChangeFailureState evWdm = new SimEvent.NodesAndLinksChangeFailureState(ev.nodesToUp , ev.nodesToDown , wdmLinksUp , wdmLinksDown);
			wdmNetwork.processEvent(currentNetPlan , new SimEvent(event.getEventTime() , SimEvent.DestinationModule.EVENT_GENERATOR , -1 , evWdm));
			
			/* Lightpaths with no traffic carried, mean that they are down => OSPF should treat them as down IP links => propagate these failures to IP layer */
			Set<Link> ipLinksDownBecauseOfWDMLayer = new HashSet<Link> (); for (Link ipLink : currentNetPlan.getLinks (ipLayer)) if (ipLink.getCapacity() == 0)  ipLinksDownBecauseOfWDMLayer.add (ipLink); 
//			System.out.println ("-- ipLinksDownBecauseOfWDMLayer: " + ipLinksDownBecauseOfWDMLayer);
			ipLinksDown.addAll (ipLinksDownBecauseOfWDMLayer);
//			System.out.println ("-- total ipLinksDown: " + ipLinksDown);
			ipLinksUp = new HashSet<Link> (currentNetPlan.getLinks (ipLayer)); ipLinksUp.removeAll (ipLinksDown);
//			System.out.println ("-- ipLinksUp: " + ipLinksUp);
//			System.out.println ("-- ev.nodesToUp: " + ev.nodesToUp);
//			System.out.println ("-- ev.nodesToDown: " + ev.nodesToDown);
			SimEvent.NodesAndLinksChangeFailureState evIp = new SimEvent.NodesAndLinksChangeFailureState(ev.nodesToUp , ev.nodesToDown , ipLinksUp , ipLinksDown);
			ospfNetwork.processEvent(currentNetPlan , new SimEvent(event.getEventTime() , SimEvent.DestinationModule.EVENT_GENERATOR , -1 , evIp));
		}
		else if(event.getEventObject() instanceof WDMUtils.LightpathModify) 
		{
			wdmNetwork.processEvent(currentNetPlan, new SimEvent(event.getEventTime(), SimEvent.DestinationModule.EVENT_GENERATOR, - 1, (WDMUtils.LightpathModify)event.getEventObject()));
		}
		else throw new Net2PlanException ("Unknown event type: " + event);
		
	}	
	

	@Override
	public String finish(StringBuilder output, double simTime)
	{
		output.append ("WDM LAYER OUTPUT");
		wdmNetwork.finish(output , simTime);
		output.append ("<p></p>IP LAYER OUTPUT");
		
		ospfNetwork.finish(output , simTime);
		return "";
	}

	@Override
	public void finishTransitory(double simTime)
	{
		wdmNetwork.finishTransitory(simTime);
		ospfNetwork.finishTransitory(simTime);
	}

}
