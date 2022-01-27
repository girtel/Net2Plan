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




 




package com.net2plan.examples.ocnbook.reports;

import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

/**
 * This report receives as an input a network design, the network recovery scheme algorithm, and a set of network risks (SRGs), and computes 
 * the availability of each network and each demand, in the no-failure state, and in each of the single-SRG failure states. 
 * 
 * @net2plan.keywords Network recovery: protection , Network recovery: restoration
 * @net2plan.ocnbooksections Section 3.7.3
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Report_perSRGFailureAnalysis implements IReport
{
	private InputParameter provisioningAlgorithm = new InputParameter ("provisioningAlgorithm" , "#eventProcessor#" , "Algorithm to process failure events");
	private InputParameter considerTrafficInOversubscribedLinksAsLost = new InputParameter ("considerTrafficInOversubscribedLinksAsLost" , true , "If true, all the demands whose traffic (even only a fraction of it) traverses an oversubscribed link, are considered that all its treaffic is blocked, as they are supposed to fail to satisfy QoS agreements");
	private InputParameter maximumE2ELatencyMs = new InputParameter ("maximumE2ELatencyMs", (double) -1 , "Maximum end-to-end latency of the traffic of any demand (a non-positive value means no limit). All the traffic of demands where a fraction of its traffic can exceed this value, are considered as lost, as they are supposed to fail to satisfy QoS agreements");
	private InputParameter failureModel = new InputParameter ("failureModel" , "#select# perBidirectionalLinkBundle SRGfromNetPlan perNode perLink perDirectionalLinkBundle" , "Failure model selection: SRGfromNetPlan, perNode, perLink, perDirectionalLinkBundle, perBidirectionalLinkBundle");
	private InputParameter rootNameOfOutFiles = new InputParameter ("rootNameOfOutFiles" , "./reportPerSRGFailure" , "For each single-SRG failure state and for the no-failure state, a n2p file is produced with the result of the network in that state. The file is named XXX_srgIndex.n2p, and XXX_noFailure.n2p, where XXX is this parameter");

	private IEventProcessor algorithm;
	private double PRECISION_FACTOR;
	
	@Override
	public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, reportParameters);

		String algorithmFile = reportParameters.get("provisioningAlgorithm_file");
		String algorithmName = reportParameters.get("provisioningAlgorithm_classname");
		String algorithmParam = reportParameters.get("provisioningAlgorithm_parameters");
		if (algorithmFile.isEmpty() || algorithmName.isEmpty()) throw new Net2PlanException("A provisioning algorithm must be defined");
		this.PRECISION_FACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));

		Map<String, String> algorithmParameters = StringUtils.stringToMap(algorithmParam);
		switch (failureModel.getString ())
		{
			case "SRGfromNetPlan":
				break;

			case "perNode":
				SRGUtils.configureSRGs(netPlan, 1, 1 , SRGUtils.SharedRiskModel.PER_NODE, true);
				break;

			case "perLink":
				SRGUtils.configureSRGs(netPlan, 1, 1 , SRGUtils.SharedRiskModel.PER_LINK, true);
				break;

			case "perDirectionalLinkBundle":
				SRGUtils.configureSRGs(netPlan, 1, 1, SRGUtils.SharedRiskModel.PER_DIRECTIONAL_LINK_BUNDLE, true);
				break;

			case "perBidirectionalLinkBundle":
				SRGUtils.configureSRGs(netPlan, 1, 1, SRGUtils.SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE, true);
				break;

			default:
				throw new Net2PlanException("Failure model not valid. Please, check algorithm parameters description");
		}

		/* Compute the n2p for the non failure state */
		netPlan.setAllNodesFailureState(true);
		for (NetworkLayer layer : netPlan.getNetworkLayers ())
			netPlan.setAllLinksFailureState(true , layer);
		if (!netPlan.getLinksDownAllLayers().isEmpty() || !netPlan.getNodesDown().isEmpty()) throw new RuntimeException ("Bad");
		NetPlan npNoFailure = netPlan.copy ();
		npNoFailure.saveToFile(new File (rootNameOfOutFiles.getString () + "_noFailure"));

		/* Initialize the provisioning algorithm */
		NetPlan npForProducingAllFailures = netPlan.copy ();
		Set<Link> npForProducingAllFailures_linksAllLayers = new HashSet<Link> (); for (NetworkLayer layer : npForProducingAllFailures.getNetworkLayers()) npForProducingAllFailures_linksAllLayers.addAll (npForProducingAllFailures.getLinks (layer));

		this.algorithm = ClassLoaderUtils.getInstance(new File(algorithmFile), algorithmName, IEventProcessor.class , null);
		this.algorithm.initialize(npForProducingAllFailures , algorithmParameters , reportParameters , net2planParameters);

		/* Compute the other network states */
		List<NetPlan> npsFailureStates = new ArrayList<NetPlan> (netPlan.getNumberOfSRGs());
		for (int srgIndex = 0 ; srgIndex < netPlan.getNumberOfSRGs() ; srgIndex ++)
		{
			if (!npForProducingAllFailures.getLinksDownAllLayers().isEmpty() || !npForProducingAllFailures.getNodesDown().isEmpty()) throw new RuntimeException ("Bad");

			/* Fail this SRG */
			final SharedRiskGroup srg = npForProducingAllFailures.getSRG(srgIndex);
			Set<Link> linksToSetAsDown = new HashSet<Link> ();
			Set<Node> nodesToSetAsDown = new HashSet<Node> ();
			nodesToSetAsDown.addAll (srg.getNodes ());
			linksToSetAsDown.addAll (srg.getLinksAllLayers ());
			
			/* Make the algorithm process the event of nodes and links down */
			SimEvent.NodesAndLinksChangeFailureState failureInfo = new SimEvent.NodesAndLinksChangeFailureState(null , nodesToSetAsDown , null , linksToSetAsDown);
			algorithm.processEvent(npForProducingAllFailures, new SimEvent(0, SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , failureInfo)); 
		
			/* Save a coy of the new state */
			npForProducingAllFailures.saveToFile(new File (rootNameOfOutFiles.getString () + "_srgIndex_" + srgIndex));
			npsFailureStates.add (npForProducingAllFailures.copy ());
			
			/* Go back to the no failure state */
			SimEvent.NodesAndLinksChangeFailureState repairInfo = new SimEvent.NodesAndLinksChangeFailureState(npForProducingAllFailures.getNodes() , null , npForProducingAllFailures_linksAllLayers , null);
			algorithm.processEvent(npForProducingAllFailures, new SimEvent(0, SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , repairInfo));
		}
		
		return printReport(npNoFailure , npsFailureStates , reportParameters);
	}
	
	@Override
	public String getDescription()
	{
		return "This report receives as an input a network design, the network recovery scheme algorithm, and a set of network risks (SRGs), and computes the availability of each network and each demand, in the no-failure state, and in each of the single-SRG failure states. ";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
	

	@Override
	public String getTitle()
	{
		return "Single-SRG failure analysis report";
	}
	
	private String printReport(NetPlan npNoFailure , List<NetPlan> npFailureStates , Map<String,String> reportParameters)
	{
		StringBuilder out = new StringBuilder();
		DecimalFormat df_6 = new DecimalFormat("#.######");
		out.append("<html><body>");
		out.append("<head><title>" + getTitle () + "</title></head>");
		out.append("<h1>Introduction</h1>");
		out.append("<p>In contrast to the availability report, here only single failure states are simulated, where each " +
				"failure state is given by a single SRG going down, and all failure states have the same probability to occur.</p>" +
				"	<p>Definitions and conventions about failure states, availability, and so on, are the same as for the availability report.</p>");
		out.append("<p>The report produces some summary information of the network, like demand blocking traffic, " +
				"traffic traversing oversubscribed links, or traffic with excessive latency, for each of the network states." );
		out.append("<p>In addition, the report saves one N2P file for each single-SRG failing network state, and one file for the no-failure state." +
				" They are supposed to be used for more careful inspection of the state of the network under those situations");
		out.append("<h1>Global information</h1>");
		out.append("<h2>Input Parameters</h2>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Name</b></th><th><b>Value</b></th><th><b>Description</b></th>");
		for (Triple<String, String, String> paramDef : getParameters())
		{
			String name = paramDef.getFirst();
			String description = paramDef.getThird();
			String value = reportParameters.get(name);
			out.append("<tr><td>").append(name).append("</td><td>").append(value).append("</td><td>").append(description).append("</td></tr>");
		}
		out.append("<tr><td>Number of network layers: </td><td>" + npNoFailure.getNumberOfLayers() + "</td><td></td></tr>");
		out.append("<tr><td>Number of SRGs defined: </td><td>" + npNoFailure.getNumberOfSRGs() + "</td><td></td></tr>");
		out.append("</table>");

		out.append("<h1>PER LAYER INFORMATION SUMMARY</h1>");
		for (int layerIndex = 0 ; layerIndex < npNoFailure.getNumberOfLayers () ; layerIndex ++)
		{
			NetworkLayer noFailureLayer = npNoFailure.getNetworkLayer (layerIndex);

			out.append("<h2>Layer " + noFailureLayer.getName () + ", index = " + noFailureLayer.getIndex () + ", id = " + noFailureLayer.getId () + "</h2>");

			if (npNoFailure.getNumberOfDemands(noFailureLayer) != 0)
			{
				out.append("<h3>Unicast traffic</h3>");
				out.append("<table border='1'>");
				out.append("<tr><th><b>SRG Index failed</b></th><th><b>Offered traffic</b></th><th><b>Blocked traffic (%)</b></th><th><b>Offered traffic traversing oversubscribed links (%)</b></th><th><b>Offered traffic of demands with excessive latency (%)</b></th><th><b>Total blocked traffic [out of contract] (%)</b></th><th><b>% of demands fully ok</b></th></tr>");
				printReport (npNoFailure , noFailureLayer , out , "No failure" , true);
				for (int srgIndex = 0 ; srgIndex < npNoFailure.getNumberOfSRGs() ; srgIndex ++)
					printReport (npFailureStates.get(srgIndex) , npFailureStates.get(srgIndex).getNetworkLayer (layerIndex) , out , "" + srgIndex , true);
				out.append("</table>");
			}

			if (npNoFailure.getNumberOfMulticastDemands(noFailureLayer) != 0)
			{
				out.append("<h3>Multicast traffic</h3>");
				out.append("<table border='1'>");
				out.append("<tr><th><b>SRG Index failed</b></th><th><b>Offered traffic</b></th><th><b>Blocked traffic (%)</b></th><th><b>Offered traffic traversing oversubscribed links (%)</b></th><th><b>Offered traffic of demands with excessive latency (%)</b></th><th><b>Total blocked traffic [out of contract] (%)</b></th><th><b>% of demands fully ok</b></th></tr>");
				printReport (npNoFailure , noFailureLayer , out , "No failure" , false);
				for (int srgIndex = 0 ; srgIndex < npNoFailure.getNumberOfSRGs() ; srgIndex ++)
					printReport (npFailureStates.get(srgIndex) , npFailureStates.get(srgIndex).getNetworkLayer (layerIndex) , out , "" + srgIndex , false);
				out.append("</table>");
			}
		}
		
		return out.toString();
	}

	private void printReport (NetPlan np , NetworkLayer layer , StringBuilder out , String rowTitle , boolean unicastDemands)
	{
		double totalOfferedTraffic = 0;
		double totalBlockedTraffic = 0; 
		double totalTrafficOfDemandsTraversingOversubscribedLinks = 0;
		double totalTrafficOfDemandsWithExcessiveWorseCaseLatency = 0;
		double totalBlockedConsideringUserDefinedExtraLimitations = 0; 
		int numberOfDemandsWithoutBlocking = 0;
		if (unicastDemands)
		{
			for (Demand d : np.getDemands (layer))
			{
				totalOfferedTraffic += d.getOfferedTraffic();
				totalBlockedTraffic += d.getBlockedTraffic();
				final boolean travOversuscribedLink = d.isTraversingOversubscribedLinks();
				final boolean hasExcessiveLatency = (maximumE2ELatencyMs.getDouble() > 0) && (d.getWorstCasePropagationTimeInMs() > maximumE2ELatencyMs.getDouble());
				if (considerTrafficInOversubscribedLinksAsLost.getBoolean())
					totalTrafficOfDemandsTraversingOversubscribedLinks += travOversuscribedLink? d.getOfferedTraffic() : 0;
				if (hasExcessiveLatency) 
					totalTrafficOfDemandsWithExcessiveWorseCaseLatency += d.getOfferedTraffic();
				if (hasExcessiveLatency || (considerTrafficInOversubscribedLinksAsLost.getBoolean() && travOversuscribedLink)) 
					totalBlockedConsideringUserDefinedExtraLimitations += d.getOfferedTraffic();
				else
				{
					totalBlockedConsideringUserDefinedExtraLimitations += d.getBlockedTraffic();
					if (d.getBlockedTraffic() < PRECISION_FACTOR) numberOfDemandsWithoutBlocking ++;
				}
			}
		}
		else
		{
			for (MulticastDemand d : np.getMulticastDemands (layer))
			{
				totalOfferedTraffic += d.getOfferedTraffic();
				totalBlockedTraffic += d.getBlockedTraffic();
				final boolean travOversuscribedLink = d.isTraversingOversubscribedLinks();
				final boolean hasExcessiveLatency = (maximumE2ELatencyMs.getDouble() > 0) && (d.getWorseCasePropagationTimeInMs() > maximumE2ELatencyMs.getDouble());
				if (considerTrafficInOversubscribedLinksAsLost.getBoolean())
					totalTrafficOfDemandsTraversingOversubscribedLinks += travOversuscribedLink? d.getOfferedTraffic() : 0;
				if (hasExcessiveLatency) 
					totalTrafficOfDemandsWithExcessiveWorseCaseLatency += d.getOfferedTraffic();
				if (hasExcessiveLatency || (considerTrafficInOversubscribedLinksAsLost.getBoolean() && travOversuscribedLink)) 
					totalBlockedConsideringUserDefinedExtraLimitations += d.getOfferedTraffic();
				else
				{
					totalBlockedConsideringUserDefinedExtraLimitations += d.getBlockedTraffic();
					if (d.getBlockedTraffic() < PRECISION_FACTOR) numberOfDemandsWithoutBlocking ++;
				}
			}
		}

		out.append("<tr><td> " + rowTitle + 
				"</td><td>" + valString(totalOfferedTraffic) + 
				"</td><td>" + valString(totalBlockedTraffic) + percentage(totalBlockedTraffic,totalOfferedTraffic) + 
				"</td><td> " + valString(totalTrafficOfDemandsTraversingOversubscribedLinks) + percentage(totalTrafficOfDemandsTraversingOversubscribedLinks,totalOfferedTraffic) + 
				"</td><td>" + valString(totalTrafficOfDemandsWithExcessiveWorseCaseLatency) + percentage(totalTrafficOfDemandsWithExcessiveWorseCaseLatency,totalOfferedTraffic)  + 
				"</td><td>" + valString(totalBlockedConsideringUserDefinedExtraLimitations) + percentage(totalBlockedConsideringUserDefinedExtraLimitations,totalOfferedTraffic) + 
				"</td><td>" + percentage(numberOfDemandsWithoutBlocking,unicastDemands? np.getNumberOfDemands (layer) : np.getNumberOfMulticastDemands (layer)) + "</td></tr>");
	}

	private String percentage (double val , double total) { double res = 100 * val/total; if (res < 1e-8) res = 0; return " (" + valString(res) + " %)"; }
	private String percentage (int val , int total) { double res = 100 * ((double) val)/ ((double) total); if (res < 1e-8) res = 0; return " (" + valString(res) + " %)"; }
	private String valString(double traffic) { return String.format ("%.3f" , traffic < PRECISION_FACTOR? 0 : traffic); }
	
}
