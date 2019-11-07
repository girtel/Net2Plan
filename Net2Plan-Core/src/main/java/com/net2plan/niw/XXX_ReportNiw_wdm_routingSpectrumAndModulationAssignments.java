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




 





package com.net2plan.niw;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.IReport;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

/**
 *
 * <p>This report collects information about the Routing and Spectrum assignment in the network, as well as other general information about the WDM layer.</p>
 * <p>This report is valid for the designs construcgted using the NIW library, which permits flexi-grid, and multi-modulation networks.
 * <p>The report provides a number of statistics regarding frequency slot occupation. It also warns about possible frequency slot clashing 
 *  two lightpaths using the same slot in the same fibers)</p>
 * @net2plan.keywords WDM
 * @author Pablo Pavon-Marino
 */ 
public class XXX_ReportNiw_wdm_routingSpectrumAndModulationAssignments implements IReport
{
	/* Input parameters */

	@Override
	public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, reportParameters);

		/* Input parameters */
		final WNet net = new WNet (netPlan);
		net.updateNetPlanObjectInternalState();
		String res = printReport(net , reportParameters);
		return res;
	}

	@Override
	public String getDescription()
	{
		return "This report shows routing and spectrum occupation of the ligtpaths in the network. Requires a NIW valid design.";
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
		return "NIW: Routing spectrum and modulation assignment";
	}

	private String printReport(WNet net , Map<String, String> reportParameters)
	{
		StringBuilder out = new StringBuilder();
		DecimalFormat df_2 = new DecimalFormat("###.##");

		out.append("<html><body>");
		out.append("<head><title>WDM Lightpath Routing and Spectrum Assignment (fixed or flexi-grid networks) report</title></head>");
		out.append("<h1>WDM Lightpath Routing and Spectrum Assignment report (fixed or flexi-grid networks)</h1>");

		out.append("<p>This report collects information about the Routing and Spectrum assignment in the network, as well as other general information about the WDM layer.</p>");
		out.append("<p>This report is valid for the designs construcgted using the NIW library, which permits flexi-grid, and multi-modulation networks.");
		out.append("<p>The report provides a number of statistics regarding frequency slot occupation. It also warns about possible frequency slot clashing");
		out.append("two lightpaths using the same slot in the same fibers)</p>");

		out.append("<h2>Click to go to...</h2>");
		out.append("<ul>");
		out.append("<li><a href=\"#inputParameters\">Input parameters</a></li>");
		out.append("<li><a href=\"#generalStats\">General statistics</a></li>");
		out.append("<li><a href=\"#linkStats\">Per fiber statistics (including slot occupation map)</a></li>");
		out.append("<li><a href=\"#routeStats\">Per regular lightpath statistics</a></li>");
		out.append("<li><a href=\"#protectionStats\">Per protection lightpath statistics</a></li>");
		out.append("<li><a href=\"#nodeStats\">Per OADM statistics</a></li>");
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

		/* Check that the topology is well formed */
		final OpticalSpectrumManager osm = OpticalSpectrumManager.createFromRegularLps(net); 

		out.append("<h2><a name=\"lasingLoops\"></a>LASING LOOPS CAUSED BY DROP & WASTE NODES</h2>");
		final List<List<WFiber>> lasingLoopse = osm.getUnavoidableLasingLoops();
		out.append("<p>Lasing loops are cycles of traversed WDM fibers that involve drop-and-waste nodes, where the signal would propagate indefinitely without being blocked by any optical switch. "
				+ "This occurs e.g. in rings where all the nodes are filterless. Table below indicates the lasing loops in the network, if any.</p>");
		if (lasingLoopse.isEmpty())
		{
			out.append("<table border='1'>");
			out.append("<tr><th bgcolor=\"PaleGreen\"><b>No lasing loops in the WDM topology</b></th></tr>");
			out.append("</table>");
		}
		else
		{
			out.append("<table border='1'>");
			out.append("<tr><th bgcolor=\"red\"><b>Lasing loopse (" + lasingLoopse.size() + ")</b></th></tr>");
			int cont = 0;
			for (List<WFiber> lasingLoop : lasingLoopse)
				out.append("<tr><td><b>" + (cont++) + "</b></td><td><b>" + seqNodesString(lasingLoop) + "</b></td></tr>");
			out.append("</table>");
		}
		
		out.append("<h2><a name=\"generalStats\"></a>GENERAL STATISTICS - Signal metrics at the input of end OADM</h2>");
		out.append("<table border='1'>");
		final int numFilterlessOadms = (int) net.getNodes().stream().filter(n->n.getOpticalSwitchingArchitecture().isPotentiallyWastingSpectrum()).count();
		final int numPureRoadms = (int) net.getNodes().stream().filter(n->n.getOpticalSwitchingArchitecture().isNeverCreatingWastedSpectrum()).count();
		out.append("<tr><th align=\"left\" colspan=\"2\"><b>OADM stats</b></th></tr>");
		out.append("<tr><td align=\"left\">Number of OADMs (#total / #Non-blocking ROADM / #Filterless)</td><td>" + net.getNodes().size() + " / " + numPureRoadms + " / " + numFilterlessOadms  + "</td></tr>");
		out.append("<tr><td align=\"left\">Number of OADMs</td><td>" + net.getNodes().size() + "</td></tr>");
		out.append("<tr><td align=\"left\">Node in degree (min/average/max)</td><td>" + MinMaxAvCollector.fromList(net.getNodes().stream().map(n->n.getIncomingFibers().size()).collect(Collectors.toList())).toString(df_2) + "</td></tr>");
		out.append("<tr><td align=\"left\">Node out degree (min/average/max)</td><td>" + MinMaxAvCollector.fromList(net.getNodes().stream().map(n->n.getOutgoingFibers().size()).collect(Collectors.toList())).toString(df_2) + "</td></tr>");

		
		out.append("<tr><th align=\"left\" colspan=\"2\"><b>Fiber link stats</b></td></tr>");
		out.append("<tr><td align=\"left\">Number of fibers</td><td>" + net.getFibers().size() + "</td></tr>");
		out.append("<tr><td align=\"left\">Number of frequency slots per fiber (min/average/max)</td><td>" + MinMaxAvCollector.fromList(net.getFibers().stream().map(n->n.getNumberOfValidOpticalChannels()).collect(Collectors.toList())).toString(df_2) + "</td></tr>");
		out.append("<tr><td align=\"left\">Number of slots occupied per fiber (min/average/max)</td><td>" + MinMaxAvCollector.fromList(net.getFibers().stream().map(n->osm.getOccupiedOpticalSlotIds(n).size()).collect(Collectors.toList())).toString(df_2) + "</td></tr>");
		
		out.append("<tr><th align=\"left\" colspan=\"2\"><b>Traffic stats</b></td></tr>");
		out.append("<tr><td align=\"left\">Number of lightpath requests (1+1 protected or not)</td><td>" + net.getLightpathRequests().size() + "</td></tr>");
		out.append("<tr><td align=\"left\">Number of 1+1 lightpath requests</td><td>" + net.getLightpathRequests().stream().filter(l->l.is11Protected()).count() + "</td></tr>");
		final double offeredTraffic = net.getLightpathRequests().stream().mapToDouble(l->l.getLineRateGbps()).sum();
		final double blockedTraffic = net.getLightpathRequests().stream().filter(l->l.isBlocked()).mapToDouble(l->l.getLineRateGbps()).sum();
		out.append("<tr><td align=\"left\">Total offered traffic (Gbps) in lightpaths</td><td>" + df_2.format(offeredTraffic) + "</td></tr>");
		out.append("<tr><td align=\"left\">Total traffic (Gbps) in blocked lightpaths </td><td>" + df_2.format(blockedTraffic) + "(block prob: " + df_2.format(offeredTraffic == 0? 0 : blockedTraffic / offeredTraffic) + ")" + "</td></tr>");

		out.append("<tr><th align=\"left\" colspan=\"2\"><b>Lightpath stats</b></td></tr>");
		out.append("<tr><td align=\"left\">Number of lightpaths (1+1 count as two)</td><td>" + net.getLightpaths().size() + "</td></tr>");
		out.append("<tr><td align=\"left\">Lightpath length in km (min/average/max)</td><td>" + MinMaxAvCollector.fromList(net.getLightpaths().stream().map(n->n.getLengthInKm()).collect(Collectors.toList())).toString(df_2) + "</td></tr>");
		out.append("<tr><td align=\"left\">Lightpath length in num hops (min/average/max)</td><td>" + MinMaxAvCollector.fromList(net.getLightpaths().stream().map(n->n.getSeqFibers().size()).collect(Collectors.toList())).toString(df_2) + "</td></tr>");
		out.append("<tr><td align=\"left\">Lightpath propagation delay in ms (min/average/max)</td><td>" + MinMaxAvCollector.fromList(net.getLightpaths().stream().map(n->n.getPropagationDelayInMs()).collect(Collectors.toList())).toString(df_2) + "</td></tr>");
		out.append("<tr><td align=\"left\">Lightpath number of occupied optical slots (min/average/max)</td><td>" + MinMaxAvCollector.fromList(net.getLightpaths().stream().map(n->n.getNumberOccupiedSlotIds()).collect(Collectors.toList())).toString(df_2) + "</td></tr>");
		out.append("</table>");

		
		/* Per link information */
		out.append("<h2><a name=\"linkStats\"></a>PER FIBER INFORMATION SUMMARY</h2>");
		out.append("<p>This table shows information for each fiber. In particular, the slots occupied, with a link to the lightpaths occupying it, either for regular lightpaths (L), or backup lightpaths in a 1+1 pair (P) that reserve slots:</p>");
		out.append("<ul>");
		out.append("<li>Black: The slot number is invalid for the link, and is not assigned to any lightpath.</li>");
		out.append("<li>White: The slot is a valid spectrum for the fiber, but is not assigned to any lightpath.</li>");
		out.append("<li>Green: The slot is a valid spectrum for the fiber, and is occupied by one regular lightpath (not backup).</li>");
		out.append("<li>Yellow: The slot is a valid spectrum for the fiber, and is occupied by one backup lightpath.</li>");
		out.append("<li>Red: The slot is within the fiber capacity, and is occupied by more than one lightpath (summing regular and backup), or is outside the link capacity and is assigned to at leastone lightpath.</li>");
		out.append("</ul>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Fiber #</b></th><th><b>Origin node</b></th><th><b>Dest. node</b></th><th><b>% slots used</b></th><th><b>Ok?</b></th>");
		final int minFiberSlot = net.getFibers().stream().mapToInt(e->e.getValidOpticalSlotIds().first()).min().orElse(0);
		final int maxFiberSlot = net.getFibers().stream().mapToInt(e->e.getValidOpticalSlotIds().last()).max().orElse(0);
		
		for (int s = minFiberSlot; s <= maxFiberSlot ; s ++) out.append("<th>" + s + "</th>");
		out.append("</tr>");
		
		
		for (WFiber e : net.getFibers())
		{
		   final SortedMap<Integer,SortedSet<WLightpath>> occupiedResources_e = osm.getOccupiedResources (e);
		   final SortedSet<Integer> validOpticalSlotsIds_e = e.getValidOpticalSlotIds();
		   
			out.append("<tr>");
			out.append("<td><a name=\"fiber" + e.getNe().getIndex() + "\">" + e.getNe().getIndex() + " (id: " + e.getId() + ")"+ "</a></td>");
			out.append("<td>" + printNode (e.getA().getNe()) + "</td>");
			out.append("<td>" + printNode (e.getB().getNe()) + "</td>");
			boolean everythingOk = true;
			StringBuffer thisLine = new StringBuffer ();
			for (int s = minFiberSlot; s <= maxFiberSlot ; s ++)
			{
				String color = "";
				final boolean inFiberCapacity = validOpticalSlotsIds_e.contains(s); 
				final SortedSet<WLightpath> lps = occupiedResources_e.getOrDefault(s, new TreeSet<> ());
				final int numLpsBackup = (int) lps.stream().filter(ee -> ee.isBackupLightpath()).count();
				final int numLpsPrimary = lps.size() - numLpsBackup;
				if (!inFiberCapacity && (numLpsPrimary + numLpsBackup == 0)) color = "black";
				else if (!inFiberCapacity && (numLpsPrimary + numLpsBackup > 0)) { color = "red"; everythingOk = false; }
				else if (inFiberCapacity && (numLpsPrimary + numLpsBackup == 0)) color = "white";
				else if (inFiberCapacity && (numLpsPrimary == 1) && (numLpsBackup == 0)) color = "PaleGreen";
				else if (inFiberCapacity && (numLpsPrimary == 0) && (numLpsBackup == 1)) color = "yellow";
				else { color = "red"; everythingOk = false; }
				thisLine.append("<td bgcolor=\"" + color + "\">");
				for (WLightpath r : lps) thisLine.append("<a href=\"#lp" + r.getNe().getIndex() + "\">L" + r.getNe().getIndex() + " </a>");
				thisLine.append("</td>");
			}
			out.append("<td>" + (occupiedResources_e.size()) / ((double) validOpticalSlotsIds_e.size()) + "</td>");
			out.append("<td bfcolor=\"" + (everythingOk? "PaleGreen" : "red")   +"\">" + (everythingOk? "Yes" : "No") +  "</td>");
			out.append(thisLine.toString());
			out.append("</tr>");
		}
		out.append("</table>");

		/* Per Route lightpath information */
		out.append("<h2><a name=\"routeStats\"></a>PER LIGHTPATH (NON BACKUP) INFORMATION SUMMARY</h2>");
		out.append("<p>This table shows information for each regular lightpath (not backup lightpaths).</p>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Lighpath Route #</b></th><th><b>Demand #</b></th><th><b>Origin node</b></th>"
				+ "<th><b>Dest. node</b></th><th><b>Trav. nodes</b></th><th><b>Length (km)</b></th><th><b>Propagation delay (ms)</b></th>"
				+ "<th><b>Line rate (Gbps)</b></th><th><b>Num. slots</b></th><th><b>Occupied slots</b></th>"
				+ "<th><b>Backup routes assigned</b></th><th><b>Ok RSA?</b></th></tr>");
		for (WLightpath r : net.getLightpaths())
		{
			if (r.isBackupLightpath()) continue;
			out.append("<tr>");
			out.append("<td><a name=\"lp" + r.getNe().getIndex() + "\">" + r.getNe().getIndex() + " (id: " + r.getId() + ")"+ "</a></td>");
			out.append("<td>" + r.getNe().getDemand().getIndex() + "</td>");
			out.append("<td>" + printNode(r.getNe().getIngressNode())+ "</td>");
			out.append("<td>" + printNode(r.getNe().getEgressNode())+ "</td>");
			out.append("<td>" + seqNodesString(r.getSeqFibers()) + "</td>");
			out.append("<td>" + df_2.format(r.getLengthInKm()) + "</td>");
			out.append("<td>" + df_2.format(r.getPropagationDelayInMs()) + "</td>");
			out.append("<td>" + df_2.format(r.getLightpathRequest().getLineRateGbps()) + "</td>");
			out.append("<td>" + r.getNumberOccupiedSlotIds() + "</td>");
			out.append("<td>" + occupiedSlotsString(r.getOpticalSlotIds()) + "</td>");
			out.append("<td>");
			for (WLightpath backupRoute : r.getBackupLightpaths())
				out.append("<a href=\"#lpProt" + backupRoute.getNe().getIndex() + "\">BU" + backupRoute.getNe().getIndex() + "</a> ");
			out.append("</td>");
			final boolean isOk = osm.isSpectrumOccupationOk(r);
			out.append("<td bgcolor=\""  +  (isOk? "PaleGreen" : "red") +"\">" + (isOk? "Yes" : "No") + "</td>");
			out.append("</tr>");
		}
		out.append("</table>");

		/* Per protection lightpath information */
		out.append("<h2><a name=\"protectionStats\"></a>PER BACKUP LIGHTPATH INFORMATION SUMMARY</h2>");
		out.append("<p>This table shows information for each so-called backup lightpath, that is designated as backup of one or more regular lightpaths.</p>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Lighpath Route Index #</b></th><th><b>Primary routes #</b></th><th><b>Origin node</b></th>"
				+ "<th><b>Dest. node</b></th><th><b>Trav. nodes</b></th><th><b>Length (km)</b></th><th><b>Propagation delay (ms)</b></th>"
				+ "<th><b>Num. slots</b></th><th><b>Occupied slots</b></th>"
				+ "<th><b>Ok?</b></th></tr>");
		for (WLightpath segment : net.getLightpaths())
		{
			if (!segment.isBackupLightpath()) continue;
			out.append("<tr>");
			out.append("<td><a name=\"lpProt" + segment.getNe().getIndex() + "\">" + segment.getNe().getIndex() + " (id: " + segment.getId() + ")"+ "</a></td>");
			out.append("<td>");
			for (WLightpath r : segment.getPrimaryLightpathsOfThisBackupLightpath())
				out.append("<a href=\"#lp" + r.getNe().getIndex() + "\">L" + r.getNe().getIndex() + "</a> ");
			out.append("</td>");
			out.append("<td>" + printNode(segment.getA().getNe()) + "</td>");
			out.append("<td>" + printNode(segment.getB().getNe()) + "</td>");
			out.append("<td>" + seqNodesString(segment.getSeqFibers()) + "</td>");
			out.append("<td>" + df_2.format(segment.getLengthInKm()) + "</td>");
			out.append("<td>" + df_2.format(segment.getPropagationDelayInMs()) + "</td>");
			out.append("<td>" + segment.getOpticalSlotIds().size() + "</td>");
			out.append("<td>" + occupiedSlotsString(segment.getOpticalSlotIds()) + "</td>");
			final boolean isOk = osm.isSpectrumOccupationOk(segment);
			out.append("<td bgcolor=\""  +  (isOk? "PaleGreen" : "red") +"\">" + (isOk? "Yes" : "No") + "</td>");
			out.append("</tr>");
		}
		out.append("</table>");

		
		/* Per OADM information */
		out.append("<h2><a name=\"nodeStats\"></a>PER OADM NODE INFORMATION SUMMARY</h2>");
		out.append("<p>This table shows information for each Optical Add/Drop Multiplexer (OADM) node in the network.</p>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>OADM # and name</b></th><th><b>OADM type</b></th><th><b>Num. input fibers</b></th><th><b>Num. output fibers</b></th>"
				+ "<th><b>Num. add lps total (reg/backup)</b></th><th><b>Num. drop lps total (reg/prot)</b></th><th><b>Num. express lps total (reg/backup)</b></th>"
				+ "</tr>");
		for (WNode n : net.getNodes())
		{
			final int addRegLps = (int) n.getAddedLigtpaths().stream ().filter(e -> !e.isBackupLightpath()).count();
			final int dropRegLps = (int) n.getDroppedLigtpaths().stream ().filter(e -> !e.isBackupLightpath()).count();
			final int expressRegLps = (int) n.getInOutOrTraversingLigtpaths().stream ().filter(e->!e.getA().equals(n) && !e.getB().equals(n)).filter(e -> !e.isBackupLightpath()).count();
			final int addBackupLps = (int) n.getAddedLigtpaths().stream ().filter(e -> e.isBackupLightpath()).count();
			final int dropBackupLps = (int) n.getDroppedLigtpaths().stream ().filter(e -> e.isBackupLightpath()).count();
			final int expressBackupLps = (int) n.getInOutOrTraversingLigtpaths().stream ().filter(e->!e.getA().equals(n) && !e.getB().equals(n)).filter(e -> e.isBackupLightpath()).count();
			out.append("<tr>");
			out.append("<td><a name=\"node" + n.getNe().getIndex() + "\">n" + n.getNe().getIndex() + " (" + n.getName() + ")" + "</a></td>");
			out.append("<td>" + n.getOpticalSwitchingArchitecture().getShortName() + "</td>");
			out.append("<td>" + n.getIncomingFibers().size() + "</td>");
			out.append("<td>" + n.getOutgoingFibers().size() + "</td>");
			out.append("<td>" + (addRegLps+addBackupLps) + "(" + addRegLps + " / " + addBackupLps + ")" + "</td>");
			out.append("<td>" + (dropRegLps+dropBackupLps) + "(" + dropRegLps + " / " + dropBackupLps + ")" + "</td>");
			out.append("<td>" + (expressRegLps+expressBackupLps) + "(" + expressRegLps + " / " + expressBackupLps + ")" + "</td>");
		}
		out.append("</table>");

		
		out.append("</body></html>");
		return out.toString();
	}

	private static String occupiedSlotsString (Collection<Integer> slots)
	{
		return slots.stream().map(s->""+s).collect(Collectors.joining(" "));
	}
	
	private String seqNodesString(List<WFiber> seqLinks)
	{
		if (seqLinks.isEmpty()) return "";
		String st = "[ " + printNode(seqLinks.get(0).getA().getNe());
		for (WFiber e : seqLinks)
			st += " -> " + printNode(e.getB().getNe());
		return st + " ]";
	}

	private static String printNode (Node n) { return "<a href=\"#node" + n.getIndex() + "\">n" + n.getIndex() + " (" + n.getName() + ")</a>"; }
	
	private static class MinMaxAvCollector
	{
		private double min = 0, max = 0, accum = 0;
		private int numSamples = 0;
		static MinMaxAvCollector fromList (List<? extends Number> samples) 
		{
			if (samples.isEmpty()) return new MinMaxAvCollector();
			final MinMaxAvCollector res = new MinMaxAvCollector();
			res.min = Double.MAX_VALUE; res.max = -Double.MAX_VALUE; res.accum = 0; res.numSamples = samples.size();
			for (Number v : samples) 
			{
				res.min = Math.min(v.doubleValue(), res.min); res.max = Math.max(v.doubleValue(), res.max); res.accum += v.doubleValue();
			}
			return res;
		}
		MinMaxAvCollector () { min = 0; max = 0; accum = 0; numSamples = 0; }
		void add (double sample) { min = numSamples == 0? sample : Math.min(min,sample); max = numSamples == 0? sample : Math.max(max,sample); accum += sample; numSamples ++; }
		double getAv () { return numSamples == 0? 0 : accum/numSamples;  }
		@Override
		public String toString () { return min + " / " + getAv() + " / " + max; } 
		public String toString (DecimalFormat df) { return df.format(min) + " / " + df.format(getAv()) + " / " + df.format(max); } 
	}
}
