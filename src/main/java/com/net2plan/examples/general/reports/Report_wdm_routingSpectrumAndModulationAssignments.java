/*******************************************************************************
 * Copyright (c) 2016 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 ******************************************************************************/




 





package com.net2plan.examples.general.reports;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IReport;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.ProtectionSegment;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.libraries.TrafficComputationEngine;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

/**
 * </ul>
 * @net2plan.keywords WDM
 */
public class Report_wdm_routingSpectrumAndModulationAssignments implements IReport
{
	/* Input parameters */
	private NetPlan netPlan;
	private NetworkLayer originalDefaultLayer;
	private Map<String, String> reportParameters;
	private int maxNumberSlots;
	private Pair<Map<Pair<Link,Integer>,List<Route>> , Map<Pair<Link,Integer>,List<ProtectionSegment>>> occupInfo;

	private InputParameter wdmLayerIndex = new InputParameter ("wdmLayerIndex", (int) 0 , "Index of the WDM layer (-1 means default layer)");

	@Override
	public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, reportParameters);

		/* Input parameters */
		this.netPlan = netPlan;
		this.reportParameters = reportParameters;
		this.originalDefaultLayer = netPlan.getNetworkLayerDefault();
		final NetworkLayer wdmLayer = wdmLayerIndex.getInt () == -1? netPlan.getNetworkLayerDefault() : netPlan.getNetworkLayer(wdmLayerIndex.getInt ());
		this.netPlan.setNetworkLayerDefault(wdmLayer);


		Map<Link, LinkedList<String>> warnings_e = new LinkedHashMap<Link, LinkedList<String>>();

		String res = printReport();
		netPlan.setNetworkLayerDefault(originalDefaultLayer);
		return res;
	}

	@Override
	public String getDescription()
	{
		return "This report shows line engineering information for WDM links in the network. Further description in the HTML generated.";
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
		return "WDM line engineering";
	}

	private String printReport()
	{
		StringBuilder out = new StringBuilder();
		DecimalFormat df_2 = new DecimalFormat("###.##");

		out.append("<html><body>");
		out.append("<head><title>WDM line engineering in multilayer (lightpath based) networks</title></head>");
		out.append("<h1>WDM line engineering report for lighptath-based networks</h1>");

		out.append("<p>This report shows line engineering information for WDM links in a multilayer optical network. The impairment calculations are inspired in the procedures described in the 2009 ITU-T WDM manual  \"Optical fibres, cabbles and systems\".</p>");
		out.append("<p>The report assumes that the WDM network follows the scheme:</p>");
		out.append("<ul>");
		out.append("<li>In the net2plan object, nodes are OADMs, links are fiber links, and routes are lightpaths: WDM channels optically switched at intermediate nodes. </li>");
		out.append("<li>Nodes are connected by unidirectional fiber links. Fiber link distance is given by the link length.");
		out.append("Other specifications are given by fiber_XXX input parameters. The fiber can be split into spans if optical amplifers (EDFAs)");
		out.append("and/or dispersion compensating modules (DCMs) are placed along the fiber.</li>");
		out.append("<li>Optical line amplifiers (EDFAs) can be located in none, one or more positions in the");
		out.append("fiber link, separating them in different spans. EDFAs are supposed to operate in the ");
		out.append("automatic gain control mode. Thus, the gain is the same, whatever the number of input");
		out.append("WDM channels. EDFA positions (as distance in km from the link start to the EDFA location)");
		out.append("and EDFA gains (assumed in dB) are read from the \"edfaPositions_km\" and \"edfaGains_dB\" ");
		out.append("attributes of the links. The format of both attributes are the same: a string of numbers ");
		out.append("separated by spaces. The <i>i</i>-th number corresponding to the position/gain of the ");
		out.append("<i>i</i>-th EDFA. If the attributes do not exist, it is assumed that no EDFAs are placed ");
		out.append("in this link. EDFA specifications are given by \"edfa_XXX\" parameters</li>");
		out.append("<li>Dispersion compensating modules (DCMs) can be located in none, one or more positions");
		out.append("in the fiber link, separating them in different spans. If a DCM and a EDFA have the same ");
		out.append("location, it is assumed that the DCM is placed first, to reduce the non-linear effects. DCM ");
		out.append("positions (as distance in km from the link start to the DCM location) are read from the ");
		out.append("\"dcmPositions_km\" attribute of the link, and the same format as with \"edfaPositions_km\" ");
		out.append("attribute is expected. If the attribute does not exist, it is assumed that no DCMs are ");
		out.append("placed in this link. DCM specifications are given by \"dcm_XXX\" parameters</li>");
		out.append("<li>Fiber links start and end in OADM modules, that permit adding, dropping and optically switch");
		out.append("individual WDM channels. OADMs have a pre-amplifier (traversed by drop and express channels) and ");
		out.append("a boost amplifier (traversed by add and express channels). They are supposed to equalize the ");
		out.append("channel power at their outputs, to a fixed value (added and express channels will thus have the same power in the fibers).");
		out.append("Also, OADMs attenuate appropriately the optical signal coming from the pre-amplifier, in the drop channels,");
		out.append("so that they fall within the receiver sensitivity range. OADM noise figures for add, drop and express channels");
		out.append("are given as input parameters. PMD values for add, drop and express channels are computed assumming that: (i) ");
		out.append("add channel traverse a multiplexer and the booster, (ii) drop channels travese the pre-amplifier and a demultiplexer,");
		out.append("(iii) express channels traverse the two amplifiers. The required parameters are provided in oadm_XXX parameters. </li>");
		out.append("<li>Each channel ends in a receiver, with specifications given by \"tp_XXX\" parameters.</li>");
		out.append("</ul></p>");
		out.append("<p>The basic checks performed are:</p>");
		out.append("<ul>");
		out.append("<li>For each link, signal power levels are within operating ranges at the oadm/edfas/dcms, both when the link has one single active channel, or when all the");
		out.append("\"channels_maxNumChannels\" are active</li>");
		out.append("<li>For each link, chromatic dispersion is within the limits set per link</li>");
		out.append("<li>For each route (lightpath), chromatic dispersion is within the limits of the receiver.</li>");
		out.append("<li>For each route (lightpath), OSNR (Optical Signal to Noise Ration) is within the operating range at the receiver.");
		out.append("A set of margins are considered to account to several not directly considered impairments. </li>");
		out.append("<li>For each route (lightpath), PMD (Polarization mode dispersion) is within the operating range at the receiver</li>");
		out.append("</ul></p>");

		out.append("<h2>Click to go to...</h2>");
		out.append("<ul>");
		out.append("<li><a href=\"#inputParameters\">Input parameters</a></li>");
		out.append("<li><a href=\"#generalStats\">General statistics</a></li>");
		out.append("<li><a href=\"#linkStats\">Per fiber statistics (including slot occupation map)</a></li>");
		out.append("</ul>");

		
		out.append("<h2><a name=\"inputParameters\"></a>Input Parameters</h2>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Name</b></th><th><b>Value</b></th><th><b>Description</b></th>");

		
		
		for (Triple<String, String, String> paramDef : getParameters())
		{
			String name = paramDef.getFirst();
			String description = paramDef.getThird();
			String value = reportParameters.get(name);
			out.append("<tr><td>").append(name).append("</td><td>").append(value).append("</td><td>").append(description).append("</td></tr>");
		}
		out.append("</table>");

		Statistics stat = new Statistics(netPlan,netPlan.getNetworkLayerDefault());
		out.append("<h2><a name=\"generalStats\"></a>GENERAL STATISTICS - Signal metrics at the input of end OADM</h2>");
		out.append("<table border='1'>");
		out.append("<tr><th align=\"left\"><b>OADM stats</b></th></tr>");
		out.append("<tr><td align=\"left\">Number of OADMs</td><td>" + netPlan.getNumberOfNodes() + "</td></tr>");
		out.append("<tr><td align=\"left\">Node in degree (min/average/max)</td><td>" + stat.nodeInDegree.toString(df_2) + "</td></tr>");
		out.append("<tr><td align=\"left\">Node out degree (min/average/max)</td><td>" + stat.nodeOutDegree.toString(df_2) + "</td></tr>");
		
		out.append("<tr><th align=\"left\"><b>Fiber link stats</b></td></tr>");
		out.append("<tr><td align=\"left\">Number of fibers</td><td>" + netPlan.getNumberOfLinks() + "</td></tr>");
		out.append("<tr><td align=\"left\">Number of frequency slots per fiber (min/average/max)</td><td>" + stat.numberFrequencySlotsPerLink + "</td></tr>");
		out.append("<tr><td align=\"left\">Utilization per fiber (min/average/max)</td><td>" + stat.numberFrequencySlotsPerLink.toString(df_2) + "</td></tr>");
		out.append("<tr><td align=\"left\">The topology of fibers is bidirectional (in fibers and number of slots)?</td><td>" + stat.bidirectionalLinks + "</td></tr>");
		
		out.append("<tr><th align=\"left\"><b>Traffic stats</b></td></tr>");
		out.append("<tr><td align=\"left\">Number of demands</td><td>" + netPlan.getNumberOfDemands() + "</td></tr>");
		final double offeredTraffic = netPlan.getVectorDemandOfferedTraffic().zSum();
		final double blockedTraffic = netPlan.getVectorDemandBlockedTraffic().zSum();
		out.append("<tr><td align=\"left\">Total offered traffic</td><td>" + df_2.format(offeredTraffic) + "</td></tr>");
		out.append("<tr><td align=\"left\">Total blocked traffic</td><td>" + df_2.format(blockedTraffic) + "(block prob: " + df_2.format(offeredTraffic == 0? 0 : blockedTraffic / offeredTraffic) + ")" + "</td></tr>");
		out.append("<tr><td align=\"left\">The offered traffic is symmetric?</td><td>" + stat.bidirectionalDemands + "</td></tr>");

		out.append("<tr><th align=\"left\"><b>Lightpath stats</b></td></tr>");
		out.append("<tr><td align=\"left\">Number of lightpaths (route objects)</td><td>" + netPlan.getNumberOfRoutes() + "</td></tr>");
		out.append("<tr><td align=\"left\">The lightpaths are bidirectional? (same number of the same line rate between each node pair)?</td><td>" + stat.bidirectionalRoutes + "</td></tr>");
		out.append("<tr><td align=\"left\">Some demands are carried by more than one lightpath?</td><td>" + stat.unicastRoutingBifurcated + "</td></tr>");
		out.append("<tr><td align=\"left\">Lightpath length in km (min/average/max)</td><td>" + stat.lpLengthKm.toString(df_2) + "</td></tr>");
		out.append("<tr><td align=\"left\">Lightpath length in num hops (min/average/max)</td><td>" + stat.lpLengthHops.toString(df_2) + "</td></tr>");
		out.append("<tr><td align=\"left\">Lightpath propagation delay in ms (min/average/max)</td><td>" + stat.lpLengthMs.toString(df_2) + "</td></tr>");
		
		out.append("<tr><th align=\"left\"><b>Resilience stats</b></th></tr>");
		out.append("<tr><td align=\"left\">Number of reserved protection segments</td><td>" + netPlan.getNumberOfProtectionSegments() + "</td></tr>");
		out.append("<tr><td align=\"left\">The protection lightpaths are bidirectional? (same number of the same number of slots reserved between each node pair)?</td><td>" + stat.bidirectionalProtectionSegments + "</td></tr>");
		out.append("<tr><td align=\"left\">Fiber capacity (number of slots) reserved for protection (min/average/max)</td><td>" + stat.fiberCapacityReservedForProtection.toString(df_2) + "</td></tr>");
		final Triple<Double, Double, Double> resilienceInfo = TrafficComputationEngine.getTrafficProtectionDegree(netPlan);
		out.append("<tr><td align=\"left\">% of carried traffic unprotected</td><td>" + df_2.format(resilienceInfo.getFirst()) + " %" + "</td></tr>");
		out.append("<tr><td align=\"left\">% of carried traffic with complete and dedicated protection (e.g. 1+1)</td><td>" + df_2.format(resilienceInfo.getSecond()) + " %" + "</td></tr>");
		out.append("<tr><td align=\"left\">% of carried traffic with partial and/or shared protection</td><td>" + df_2.format(resilienceInfo.getThird()) + " %" + "</td></tr>");
		out.append("</table>");
		
		/* Per link information */
		this.maxNumberSlots = (int) WDMUtils.getVectorFiberNumFrequencySlots(netPlan).getMaxLocation() [0];
		this.occupInfo = getSlotOccupancy (netPlan);
		out.append("<h2><a name=\"linkStats\"></a>PER FIBER INFORMATION SUMMARY</h2>");
		out.append("<p>This table shows information for each fiber. In particular, the slots occupied, with a link to the lightpaths occupying it, either for regular lightpaths, or lightpaths defined as protection segments that reserve slots:</p>");
		out.append("<ul>");
		out.append("<li>Black: The slot number is higher than the capacity declared for the link, and is not assigned to any lightpath.</li>");
		out.append("<li>White: The slot is within the fiber capacity, and is not assigned to any lightpath.</li>");
		out.append("<li>Green: The slot is within the fiber capacity, and is occupied by one regular lightpath and assigned to no protection lightpath.</li>");
		out.append("<li>Yellow: The slot is within the fiber capacity, and is occupied by zero regular lightpaths and assigned to one protection lightpath.</li>");
		out.append("<li>Red: The slot is within the fiber capacity, and is occupied by more than one lightpath (summing regular and protection), or is outside the link capacity and is assigned to at leastone lightpath.</li>");
		out.append("</ul>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Fiber #</b></th><th><b>Origin node</b></th><th><b>Dest. node</b></th><th><b>% slots used</b></th><th><b>Ok?</b></th>");
		for (int s = 0; s < maxNumberSlots ; s ++) out.append("<th>" + s + "</th>");
		out.append("</tr>");
		
		for (Link e : netPlan.getLinks())
		{
			final int numSlotsThisFiber = WDMUtils.getFiberNumFrequencySlots(e);
			out.append("<tr>");
			out.append("<td><a name=\"fiber" + e.getIndex() + "\">" + e.getIndex() + " (id: " + e.getId() + ")"+ "</a></td>");
			out.append("<td>" + e.getOriginNode().getIndex() + " (" + e.getOriginNode().getName() + ")"+ "</td>");
			out.append("<td>" + e.getDestinationNode().getIndex() + " (" + e.getDestinationNode().getName() + ")"+ "</td>");
			int numSlotsUsedThisFiber = 0; 
			boolean everythingOk = true;
			StringBuffer thisLine = new StringBuffer ();
			for (int s = 0; s < maxNumberSlots ; s ++)
			{
				List<Route> lps = occupInfo.getFirst().get(Pair.of(e,s));
				List<ProtectionSegment> lpProts = occupInfo.getSecond().get(Pair.of(e,s));
				String color = "";
				final boolean inFiberCapacity = (s < numSlotsThisFiber); 
				final int numLps = lps == null? 0 : lps.size();
				final int numProts = lpProts == null? 0 : lpProts.size();
				numSlotsUsedThisFiber += numLps + numProts > 0? 1 : 0;
				if (!inFiberCapacity && (numLps + numProts == 0)) color = "black";
				else if (!inFiberCapacity && (numLps + numProts > 0)) { color = "red"; everythingOk = false; }
				else if (inFiberCapacity && (numLps + numProts == 0)) color = "white";
				else if (inFiberCapacity && (numLps == 1) && (numProts == 0)) color = "PaleGreen";
				else if (inFiberCapacity && (numLps == 0) && (numProts == 1)) color = "yellow";
				else { color = "red"; everythingOk = false; }
				thisLine.append("<td bgcolor=\"" + color + "\">");
				if (lps != null) for (Route r : lps) thisLine.append("<a href=\"#lp" + r.getIndex() + "\">L </a>");
				if (lpProts != null) for (ProtectionSegment segment : lpProts) thisLine.append("<a href=\"#lpProt" + segment.getIndex() + "\">P </a>");
				thisLine.append("</td>");
			}
			out.append("<td>" + ((double) numSlotsUsedThisFiber) / ((double) numSlotsThisFiber) + "</td>");
			out.append("<td bfcolor=\"" + (everythingOk? "PaleGreen" : "red")   +"\">" + (everythingOk? "Yes" : "No") +  "</td>");
			out.append(thisLine.toString());
			out.append("</tr>");
		}
		out.append("</table>");

		/* Per Route lightpath information */
		out.append("<h2><a name=\"routeStats\"></a>PER LIGHTPATH INFORMATION SUMMARY</h2>");
		out.append("<p>This table shows information for each regular lightpath: lightpaths defined as Route objects in the design (in opposition to lightpaths defined as ProtectionSegments that reserve a slots in the links).</p>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Lighpath Route #</b></th><th><b>Demand #</b></th><th><b>Origin node</b></th>"
				+ "<th><b>Dest. node</b></th><th><b>Length (km)</b></th><th><b>Propagation delay (ms)</b></th>"
				+ "<th><b>Line rate (Gbps)</b></th><th><b>Num. slots</b></th><th><b>Occupied slots</b></th>"
				+ "<th><b>Wavelength conversion?</b></th><th><b>Wavelength contiguity?</b></th></th>"
				+ "<th><b>Num. regenerators (reg. nodes)</b></th><b>Protection segments assigned</b></th></tr><th><b>Ok?</b></th></tr>");
		for (Route r : netPlan.getRoutes())
		{
			WDMUtils.RSA rsa = new WDMUtils.RSA(r , false);
			out.append("<tr>");
			out.append("<td><a name=\"lp" + r.getIndex() + "\">" + r.getIndex() + " (id: " + r.getId() + ")"+ "</a></td>");
			out.append("<td>" + r.getDemand().getIndex() + "</td>");
			out.append("<td>" + r.getIngressNode().getIndex() + " (" + r.getIngressNode().getName() + ")"+ "</td>");
			out.append("<td>" + r.getEgressNode().getIndex() + " (" + r.getEgressNode().getName() + ")"+ "</td>");
			out.append("<td>" + df_2.format(r.getLengthInKm()) + "</td>");
			out.append("<td>" + df_2.format(r.getPropagationDelayInMiliseconds()) + "</td>");
			out.append("<td>" + df_2.format(r.getCarriedTraffic()) + "</td>");
			out.append("<td>" + rsa.getNumSlots() + "</td>");
			out.append("<td>" + occupiedSlotsString(rsa) + "</td>");
			out.append("<td>" + rsa.hasFrequencySlotConversions() + "</td>");
			out.append("<td>" + rsa.isFrequencySlotContiguous () + "</td>");
			List<Node> regPoints = rsa.getSignalRegenerationNodes (); 
			out.append("<td>" + regPoints.size() + (regPoints.isEmpty()? "" : "(" + regPoints + ")"));
			out.append("</td>");
			out.append("<td>");
			for (ProtectionSegment segment : r.getPotentialBackupProtectionSegments())
				out.append("<a href=\"#lpProt" + segment.getIndex() + "\">" + segment.getIndex() + "</a> ");
			out.append("</td>");
			out.append("<td>" + isOk(rsa) + "</td>");
			out.append("</tr>");
		}
		out.append("</table>");

		/* Per protection lightpath information */
		out.append("<h2><a name=\"routeStats\"></a>PER PROTECTION LIGHTPATH INFORMATION SUMMARY</h2>");
		out.append("<p>This table shows information for each so-called protection lightpath: lightpaths defined as ProtectionSegment objects in the design that reserve slots in the links.</p>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Lighpath ProtectionSegment #</b></th><th><b>Primary routes #</b></th><th><b>Origin node</b></th>"
				+ "<th><b>Dest. node</b></th><th><b>Length (km)</b></th><th><b>Propagation delay (ms)</b></th>"
				+ "<th><b>Num. slots</b></th><th><b>Occupied slots</b></th>"
				+ "<th><b>Wavelength conversion?</b></th><th><b>Wavelength contiguity?</b></th></th>"
				+ "<th><b>Num. regenerators (reg. nodes)</b></th><th><b>Ok?</b></th></tr>");
		for (ProtectionSegment segment : netPlan.getProtectionSegments ())
		{
			WDMUtils.RSA rsa = new WDMUtils.RSA(segment);
			out.append("<tr>");
			out.append("<td><a name=\"lpProt" + segment.getIndex() + "\">" + segment.getIndex() + " (id: " + segment.getId() + ")"+ "</a></td>");
			out.append("<td>");
			for (Route r : segment.getAssociatedRoutesToWhichIsBackup())
				out.append("<a href=\"#lp" + r.getIndex() + "\">" + r.getIndex() + "</a> ");
			out.append("</td>");
			out.append("<td>" + segment.getOriginNode().getIndex() + " (" + segment.getOriginNode().getName() + ")"+ "</td>");
			out.append("<td>" + segment.getDestinationNode().getIndex() + " (" + segment.getDestinationNode().getName() + ")"+ "</td>");
			out.append("<td>" + df_2.format(segment.getLengthInKm()) + "</td>");
			out.append("<td>" + df_2.format(segment.getPropagationDelayInMs()) + "</td>");
			out.append("<td>" + rsa.getNumSlots() + "</td>");
			out.append("<td>" + occupiedSlotsString(rsa) + "</td>");
			out.append("<td>" + rsa.hasFrequencySlotConversions() + "</td>");
			out.append("<td>" + rsa.isFrequencySlotContiguous () + "</td>");
			List<Node> regPoints = rsa.getSignalRegenerationNodes (); 
			
			out.append("<td>" + regPoints.size() + (regPoints.isEmpty()? "" : "(" + regPoints + ")"));
			out.append("</td>");
			out.append("<td>" + isOk(rsa) + "</td>");
			out.append("</tr>");
		}
		out.append("</table>");
		
		
//		out.append("<h2>PER ROUTE INFORMATION SUMMARY - Signal metrics at the transponder</h2>");
//		out.append("<table border='1'>");
//		out.append("<tr><th><b>Route #</b></th><th><b>Length (km)</b></th><th><b># EDFAs</b></th><th><b># DCMs</b></th><th><b>Chromatic Dispersion (ps/nm)</b></th><th><b>OSNR (dB)</b></th><th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th><th><b>Warnings</b></th></tr>");
//		for (Route r : netPlan.getRoutes())
//		{
//			final double d_r = r.getLengthInKm();
//			final String st_a_r = r.getIngressNode().getName ();
//			final String st_b_r = r.getEgressNode().getName ();
//			LinkedList<Triple<Double, String, Double>> el = elements_r.get(r);
//			LinkedList<Pair<double[], double[]>> imp = impairments_r.get(r);
//			LinkedList<String> w = warnings_r.get(r);
//
//			int numEDFAs = 0;
//			for (Triple<Double, String, Double> t : el)
//				if (t.getSecond().equalsIgnoreCase("EDFA"))
//					numEDFAs++;
//
//			int numDCMs = 0;
//			for (Triple<Double, String, Double> t : el)
//				if (t.getSecond().equalsIgnoreCase("DCM"))
//					numDCMs++;
//
//			final double[] impInfoInputOADM = imp.getLast().getFirst();
//			StringBuilder warnings = new StringBuilder();
//			for (String s : w) warnings.append("<p>").append(s).append("</p>");
//
//			out.append("<tr><td>").append(r).append(" (").append(st_a_r).append(" --> ").append(st_b_r).append(") </td><td>").append(df_2.format(d_r)).append("</td><td>").append(numEDFAs).append("</td><td>").append(numDCMs).append("</td><td>").append(df_2.format(impInfoInputOADM[1])).append("</td><td>").append(df_2.format(linear2dB(impInfoInputOADM[3]))).append("</td><td>").append(df_2.format(impInfoInputOADM[0])).append("</td><td>").append(df_2.format(Math.sqrt(impInfoInputOADM[2]))).append("</td><td>").append(warnings.toString()).append("</td>" + "</tr>");
//		}
//		out.append("</table>");
//
//		out.append("<h2>PER-LINK DETAILED INFORMATION </h2>");
//		out.append("<p>Number of links: ").append(netPlan.getNumberOfLinks()).append("</p>");
//
//		for (Link e : netPlan.getLinks())
//		{
//			final double d_e = e.getLengthInKm();
//			final String st_a_e = e.getOriginNode().getName ();
//			final String st_b_e = e.getDestinationNode().getName ();
//			LinkedList<Triple<Double, String, Double>> el = elements_e.get(e);
//			LinkedList<Pair<double[], double[]>> imp = impairments_e.get(e);
//			LinkedList<String> w = warnings_e.get(e);
//			final String st_edfaPositions_km = e.getAttribute("edfaPositions_km") == null ? "" : e.getAttribute("edfaPositions_km");
//			final String st_edfaGains_dB = e.getAttribute("edfaGains_dB") == null ? "" : e.getAttribute("edfaGains_dB");
//			final String st_dcmPositions_km = e.getAttribute("dcmPositions_km") == null ? "" : e.getAttribute("dcmPositions_km");
//
//			out.append("<h3>LINK # ").append(e).append(" (").append(st_a_e).append(" --> ").append(st_b_e).append(")</h3>");
//			out.append("<table border=\"1\">");
//			out.append("<caption>Link input information</caption>");
//			out.append("<tr><td>Link length (km)</td><td>").append(d_e).append("</td></tr>");
//			out.append("<tr><td>EDFA positions (km)</td><td>").append(st_edfaPositions_km).append("</td></tr>");
//			out.append("<tr><td>EDFA gains (dB)</td><td>").append(st_edfaGains_dB).append("</td></tr>");
//			out.append("<tr><td>DCM positions (km)</td><td>").append(st_dcmPositions_km).append("</td></tr>");
//			out.append("</table>");
//
//			out.append("<table border=\"1\">");
//			out.append("<caption>Signal metrics evolution</caption>");
//			out.append("<tr><th><b>Position (km)</b></th><th><b>Position (description)</b></th><th><b>Chromatic Dispersion (ps/nm)</b></th><th><b>OSNR (dB)</b></th><th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th><th><b>Warnings</b></th></tr>");
//			Iterator<Triple<Double, String, Double>> it_el = el.iterator();
//			Iterator<Pair<double[], double[]>> it_imp = imp.iterator();
//			Iterator<String> it_w = w.iterator();
//			while (it_el.hasNext())
//			{
//				final Triple<Double, String, Double> this_el = it_el.next();
//				final Pair<double[], double[]> this_imp = it_imp.next();
//				final String this_warnings = it_w.next();
//
//				final double pos_km = this_el.getFirst();
//				String elementType = this_el.getSecond();
//				final double elementAuxData = this_el.getThird();
//				final double[] prevInfo = this_imp.getFirst();
//
//				if (elementType.equalsIgnoreCase("EDFA")) elementType += " (gain " + elementAuxData + " dB)";
//				else if (elementType.equalsIgnoreCase("SPAN")) elementType += " (" + elementAuxData + " km)";
//				else if (elementType.equalsIgnoreCase("DCM")) elementType += " (comp " + elementAuxData + " ps/nm)";
//
//				out.append("<tr><td>").append(df_2.format(pos_km)).append("</td><td>" + "Input of ").append(elementType).append("</td><td>").append(df_2.format(prevInfo[1])).append("</td><td>").append(df_2.format(linear2dB(prevInfo[3]))).append("</td><td>").append(df_2.format(prevInfo[0])).append("</td><td>").append(df_2.format(Math.sqrt(prevInfo[2]))).append("</td><td>").append(this_warnings).append("</td>" + "</tr>");
//
//			}
//			out.append("</table>");
//		}
//
//		out.append("<h2>PER-LIGHTPATH DETAILED INFORMATION</h2>");
//		out.append("<p>Number of lightpaths: ").append(netPlan.getNumberOfRoutes()).append("</p>");
//
//		for (Route r : netPlan.getRoutes())
//		{
//			final double d_r = r.getLengthInKm();
//			final String st_a_r = r.getIngressNode().getName();
//			final String st_b_r = r.getEgressNode().getName();
//			LinkedList<Triple<Double, String, Double>> el = elements_r.get(r);
//			LinkedList<Pair<double[], double[]>> imp = impairments_r.get(r);
//			LinkedList<String> w = warnings_r.get(r);
//
//			out.append("<h3>ROUTE # ").append(r).append(" (").append(st_a_r).append(" --> ").append(st_b_r).append("), Length: ").append(d_r).append(" km</h3>");
//			out.append("<table border=\"1\">");
//			out.append("<caption>Signal metrics evolution</caption>");
//			out.append("<tr><th><b>Position (km)</b></th><th><b>Position (description)</b></th><th><b>Chromatic Dispersion (ps/nm)</b></th><th><b>OSNR (dB)</b></th><th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th><th><b>Warnings</b></th></tr>");
//			Iterator<Triple<Double, String, Double>> it_el = el.iterator();
//			Iterator<Pair<double[], double[]>> it_imp = imp.iterator();
//			Iterator<String> it_w = w.iterator();
//			while (it_el.hasNext())
//			{
//				final Triple<Double, String, Double> this_el = it_el.next();
//				final Pair<double[], double[]> this_imp = it_imp.next();
//				final String this_warnings = it_w.next();
//
//				final double pos_km = this_el.getFirst();
//				String elementType = this_el.getSecond();
//				final double elementAuxData = this_el.getThird();
//				final double[] prevInfo = this_imp.getFirst();
//				if (elementType.equalsIgnoreCase("EDFA")) elementType += " (gain " + elementAuxData + " dB)";
//				else if (elementType.equalsIgnoreCase("SPAN")) elementType += " (" + elementAuxData + " km)";
//				else if (elementType.equalsIgnoreCase("DCM")) elementType += " (comp " + elementAuxData + " ps/nm)";
//
//				out.append("<tr><td>").append(df_2.format(pos_km)).append("</td><td>" + "Input of ").append(elementType).append("</td><td>").append(df_2.format(prevInfo[1])).append("</td><td>").append(df_2.format(linear2dB(prevInfo[3]))).append("</td><td>").append(df_2.format(prevInfo[0])).append("</td><td>").append(df_2.format(Math.sqrt(prevInfo[2]))).append("</td><td>").append(this_warnings).append("</td>" + "</tr>");
//
//			}
//			final Triple<Double, String, Double> this_el = el.getLast();
//			final Pair<double[], double[]> this_imp = imp.getLast();
//			final String this_warnings = w.getLast();
//			final double pos_km = this_el.getFirst();
//			final double[] postInfo = this_imp.getSecond();
//			out.append("<tr><td>").append(df_2.format(pos_km)).append("</td><td>" + "Receiver" + "</td><td>").append(df_2.format(postInfo[1])).append("</td><td>").append(df_2.format(linear2dB(postInfo[3]))).append("</td><td>").append(df_2.format(postInfo[0])).append("</td><td>").append(df_2.format(Math.sqrt(postInfo[2]))).append("</td><td>").append(this_warnings).append("</td>" + "</tr>");
//
//			out.append("</table>");
//		}

		out.append("</body></html>");
		return out.toString();
	}

	private Pair<Map<Pair<Link,Integer>,List<Route>> , Map<Pair<Link,Integer>,List<ProtectionSegment>>> getSlotOccupancy (NetPlan netPlan)
	{
		Map<Pair<Link,Integer>,List<Route>> lpOccup = new HashMap<Pair<Link,Integer>,List<Route>> (); 
		Map<Pair<Link,Integer>,List<ProtectionSegment>> protOccup = new HashMap<Pair<Link,Integer>,List<ProtectionSegment>> ();
		for (Route r : netPlan.getRoutes())
		{
			WDMUtils.RSA rsa = new WDMUtils.RSA(r , false);
			for (int contLink = 0; contLink < rsa.seqLinks.size() ; contLink ++)
			{
				final Link e = rsa.seqLinks.get(contLink);
				for (int s = 0; s < rsa.getNumSlots() ; s ++)
				{
					Pair<Link,Integer> key = Pair.of(e , rsa.seqFrequencySlots_se.get(s,contLink));
					List<Route> list = lpOccup.get(key); if (list == null) { list = new LinkedList<Route> (); lpOccup.put(key,list); }
					list.add(r);
				 }
			}
		}
		for (ProtectionSegment r : netPlan.getProtectionSegments())
		{
			WDMUtils.RSA rsa = new WDMUtils.RSA(r);
			for (int contLink = 0; contLink < rsa.seqLinks.size() ; contLink ++)
			{
				final Link e = rsa.seqLinks.get(contLink);
				for (int s = 0; s < rsa.getNumSlots() ; s ++)
				{
					Pair<Link,Integer> key = Pair.of(e , rsa.seqFrequencySlots_se.get(s,contLink));
					List<ProtectionSegment> list = protOccup.get(key); if (list == null) { list = new LinkedList<ProtectionSegment> (); protOccup.put(key,list); }
					list.add(r);
				 }
			}
		}
		return Pair.of(lpOccup , protOccup);
	}

	private boolean isOk (WDMUtils.RSA rsa)
	{
		int counterLink = 0;
		for (Link e : rsa.seqLinks)
		{
			for (int contS = 0; contS < rsa.getNumSlots() ; contS ++)
			{
				final int s = rsa.seqFrequencySlots_se.get(contS , counterLink);
				if (s >= WDMUtils.getFiberNumFrequencySlots(e)) return false;
				final int numLps = occupInfo.getFirst().get(Pair.of(e,s)) == null? 0 : occupInfo.getFirst().get(Pair.of(e,s)).size();
				final int numLpsProt = occupInfo.getSecond().get(Pair.of(e,s)) == null? 0 : occupInfo.getSecond().get(Pair.of(e,s)).size();
				if (numLps+numLpsProt > 1) return false;
			}
			counterLink ++;
		}
		return true;
	}
	
	private String occupiedSlotsString (WDMUtils.RSA rsa)
	{
		if (!rsa.hasFrequencySlotConversions() && rsa.isFrequencySlotContiguous ()) return rsa.seqFrequencySlots_se.get(0,0) + "-"  +rsa.seqFrequencySlots_se.get(rsa.getNumSlots()-1,0);
		String res = "";
		for (int e = 0 ; e < rsa.seqFrequencySlots_se.rows() ; e ++)
		{
			res += "<p>";
			for (int s = 0 ; s < rsa.getNumSlots() ; s ++) res += rsa.seqFrequencySlots_se.get(e,s) + " ";
			res += "</p>";
		}
		return rsa.toString();
	}
	
	private static class Statistics
	{
		private MinMaxAvCollector nodeInDegree = new MinMaxAvCollector ();
		private MinMaxAvCollector nodeOutDegree = new MinMaxAvCollector ();
		private boolean bidirectionalLinks;
		private boolean bidirectionalDemands;
		private boolean bidirectionalRoutes;
		private boolean bidirectionalProtectionSegments;
		private MinMaxAvCollector numberFrequencySlotsPerLink = new MinMaxAvCollector ();
		private boolean unicastRoutingBifurcated;
		private MinMaxAvCollector linkUtilizationIncludingProtSegments = new MinMaxAvCollector ();
		private MinMaxAvCollector lpLengthKm = new MinMaxAvCollector ();
		private MinMaxAvCollector lpLengthHops = new MinMaxAvCollector ();
		private MinMaxAvCollector lpLengthMs = new MinMaxAvCollector ();
		private MinMaxAvCollector fiberCapacityReservedForProtection = new MinMaxAvCollector ();
		
		private Statistics (NetPlan netPlan , NetworkLayer wdmLayer) 
		{ 
			for (Node n : netPlan.getNodes())
			{
				nodeInDegree.add(n.getIncomingLinks(wdmLayer).size());
				nodeOutDegree.add(n.getOutgoingLinks(wdmLayer).size());
			}
			bidirectionalLinks = GraphUtils.isWeightedBidirectional(netPlan.getNodes() , netPlan.getLinks() , netPlan.getVectorLinkCapacity());
			bidirectionalDemands = GraphUtils.isWeightedBidirectional(netPlan.getNodes() , netPlan.getDemands() , netPlan.getVectorDemandOfferedTraffic());
			bidirectionalRoutes = GraphUtils.isWeightedBidirectional(netPlan.getNodes() , netPlan.getRoutes () , netPlan.getVectorRouteCarriedTraffic());
			bidirectionalProtectionSegments = GraphUtils.isWeightedBidirectional(netPlan.getNodes() , netPlan.getProtectionSegments() , netPlan.getVectorProtectionSegmentOccupiedCapacity());
			for (Link e : netPlan.getLinks())
			{
				numberFrequencySlotsPerLink.add((double) WDMUtils.getFiberNumFrequencySlots(e));
				linkUtilizationIncludingProtSegments.add(e.getOccupiedCapacityIncludingProtectionSegments());
				fiberCapacityReservedForProtection.add(e.getOccupiedCapacityIncludingProtectionSegments() - e.getOccupiedCapacityNotIncludingProtectionSegments());
			}
			unicastRoutingBifurcated = false;
			for (Demand d : netPlan.getDemands())
				if (d.getRoutes().size() > 1) { unicastRoutingBifurcated = false; break; }
			for (Route r : netPlan.getRoutes())
			{
				lpLengthHops.add(r.getNumberOfHops());
				lpLengthKm.add(r.getLengthInKm());
				lpLengthMs.add(r.getPropagationDelayInMiliseconds());
			}
		}
		
	}
	
	private static class MinMaxAvCollector
	{
		private double min, max, accum;
		private int numSamples;
		MinMaxAvCollector () { min = 0; max = 0; accum = 0; numSamples = 0; }
		void add (double sample) { min = Math.min(min,sample); max = Math.max(max,sample); accum += sample; }
		double getAv () { return numSamples == 0? 0 : accum/numSamples;  }
		public String toString () { return min + " / " + getAv() + " / " + max; } 
		public String toString (DecimalFormat df) { return df.format(min) + " / " + df.format(getAv()) + " / " + df.format(max); } 
	}
}
