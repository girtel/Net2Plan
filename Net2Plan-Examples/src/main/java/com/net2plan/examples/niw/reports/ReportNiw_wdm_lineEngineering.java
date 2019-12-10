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
package com.net2plan.examples.niw.reports;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.poi.dev.OOXMLPrettyPrint;

import com.net2plan.interfaces.networkDesign.IReport;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.niw.OpticalAmplifierInfo;
import com.net2plan.niw.OpticalSimulationModule;
import com.net2plan.niw.OpticalSpectrumManager;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WLightpath;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNetConstants;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

/**
 * <p>
 * This report shows line engineering information for WDM links in a multilayer optical network. The
 * impairment calculations are inspired in the procedures described in the 2009 ITU-T WDM manual
 * "Optical fibres, cabbles and systems".
 * </p>
 * <p>
 * The report assumes that the WDM network follows the conventions of the NIW library.
 * </p>
 * <p>
 * The basic checks performed are:
 * </p>
 * <ul>
 * <li>For each link, signal power levels are within operating ranges at the oadm/edfas/dcms, both
 * when the link has one single active channel, or when all the "channels_maxNumChannels" are
 * active</li>
 * <li>For each link, chromatic dispersion is within the limits set per link</li>
 * <li>For each route (lightpath), chromatic dispersion is within the limits of the receiver.</li>
 * <li>For each route (lightpath), OSNR (Optical Signal to Noise Ration) is within the operating
 * range at the receiver. A set of margins are considered to account to several not directly
 * considered impairments.</li>
 * <li>For each route (lightpath), PMD (Polarization mode dispersion) is within the operating range
 * at the receiver</li>
 * </ul>
 * 
 * @author Pablo Pavon-Marino
 * @version 1.2, October 2017
 */
public class ReportNiw_wdm_lineEngineering implements IReport
{
	/* Input parameters */
	private WNet wNet;
	private Map<String, String> reportParameters;
	private static final DecimalFormat df_2 = new DecimalFormat("###.##");
	private static final Function<Double,String> df = v -> v >= 1e50? "\u221E" : v <= -1e50? "-\u221E" : df_2.format(v);

	
	private InputParameter printPerLightpathDetailedInformation = new InputParameter ("printPerLightpathDetailedInformation", true , "If true, adds a table per lightpath with detailed information");

	@Override
	public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, reportParameters);

		/* Input parameters */
		this.wNet = new WNet (netPlan);
		wNet.updateNetPlanObjectInternalState();
		
		this.reportParameters = reportParameters;

		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, reportParameters);
		
		return printReport(printPerLightpathDetailedInformation.getBoolean());
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

	private String printReport(boolean printPerLightpathDetailedInformation)
	{
		StringBuilder out = new StringBuilder();
		final OpticalSpectrumManager osm = OpticalSpectrumManager.createFromRegularLps(wNet); 
		final OpticalSimulationModule osim = new OpticalSimulationModule (wNet);
		osim.updateAllPerformanceInfo();
		
		out.append("<html><body>");
		out.append("<head><title>WDM line engineering in multilayer (lightpath based) networks</title></head>");
		out.append("<h1>WDM line engineering report for lighptath-based networks</h1>");

		out.append(
				"<p>This report shows line engineering information for WDM links in a multilayer optical network. The impairment calculations are inspired in the procedures described in the 2009 ITU-T WDM manual  \"Optical fibres, cabbles and systems\".</p>");
		out.append("<p>The report assumes that the NetPlan design is created using the NIW library.</p>");
		out.append("<p>The basic checks performed are:</p>");
		out.append("<ul>");
		out.append("<li>For each link, signal power levels are within operating ranges at the oadm/optical amplifiers</li>");
		out.append("<li>For each link, chromatic dispersion is within the limits set per link</li>");
		out.append("<li>For each lightpath, received power is within the limits of the receiver.</li>");
		out.append("<li>For each lightpath, chromatic dispersion is within the limits of the receiver.</li>");
		out.append("<li>For each lightpath, OSNR (Optical Signal to Noise Ration) is within the operating range at the receiver.</li>");
		out.append("<li>For each lightpath, PMD (Polarization mode dispersion) is within the operating range at the receiver</li>");
		out.append("</ul></p>");

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
		out.append("</table>");

		out.append("<h2>PER FIBER INFORMATION SUMMARY</h2>");
		out.append("<table border='1'>");
		out.append(
				"<tr><th><b>Fiber #</b></th>"
				+ "<th><b>Length (km)</b></th>"
				+ "<th><b>Amplified slot index ranges</b></th>"
				+ "<th><b># EDFAs / DCMs</b></th>"
				+ "<th><b># Traversing lighptaths</b></th>"
				+ "<th><b># Optical slots occupied / total (%)</b></th>"
				+ "<th><b>Accum. chromatic Dispersion (ps/nm)</b></th>"
				+ "<th><b>Total power fiber input (dBm)</b></th>"
				+ "<th><b>Total power fiber output (dBm)</b></th>"
				+ "<th><b>Warnings</b></th></tr>");
		for (WFiber e : wNet.getFibers())
		{
			final String st_a_e = e.getA().getName();
			final String st_b_e = e.getB().getName();
			
			out.append("<tr>").
			append("<td>" + st_a_e + " --> " + st_b_e + "</td>").
			append("<td>" + df.apply(e.getLengthInKm()) + "</td>").
			append("<td>" + e.getValidOpticalSlotRanges() + "</td>").
			append("<td>" + e.getNumberOfOpticalLineAmplifiersTraversed() + "</td>").
			append("<td>" + e.getTraversingLps().size() + "</td>").
			append("<td>" + osm.getOccupiedOpticalSlotIds(e).size() + " / " + e.getNumberOfValidOpticalChannels() + " (" + df.apply(100.0 * osm.getOccupiedOpticalSlotIds(e).size() /  e.getNumberOfValidOpticalChannels())   + "%)</td>").
			append("<td>" + df.apply(e.getAccumulatedChromaticDispersion_psPerNm()) + "</td>").
			append("<td>" + df.apply(osim.getTotalPowerAtFiberEnds_dBm(e).getFirst()) + "</td>").
			append("<td>" + df.apply(osim.getTotalPowerAtFiberEnds_dBm(e).getSecond()) + "</td>");
			
			final StringBuffer st = new StringBuffer ();
			final List<OpticalAmplifierInfo> olas = e.getOpticalLineAmplifiersInfo();
			for (int contOla = 0; contOla < olas.size() ; contOla ++)
			{
				final OpticalAmplifierInfo ola = olas.get(contOla);
				final double distKm = ola.getOlaPositionInKm().get();
				final double gain_dB = ola.getGainDb();
				final double powerAtOutput_dBm = osim.getTotalPowerAtAmplifierOutput_dBm(e, contOla);
				if (powerAtOutput_dBm < ola.getMinAcceptableOutputPower_dBm() || powerAtOutput_dBm > ola.getMaxAcceptableOutputPower_dBm())
					st.append("<p>EDFA-" + contOla + " ("+ df.apply(distKm) + " km)" + ": Power at the output is " + df.apply(powerAtOutput_dBm) + " dBm. It should be between [" + df.apply(ola.getMinAcceptableOutputPower_dBm()) + ", " + df.apply(ola.getMaxAcceptableOutputPower_dBm()) + "] dBm</p>");
				if (!ola.isOkGainBetweenMargins())
					st.append("<p>EDFA-" + contOla + " ("+ df.apply(distKm) + " km)" + ": Gain is " + df.apply(gain_dB) + " dB. It should be between [" + df.apply(ola.getMinAcceptableGainDb()) + ", " + df.apply(ola.getMaxAcceptableGainDb()) + "] dBm</p>");
			}
			out.append("<td>" + st.toString() + "</td>");
			out.append("</tr>");
		}
		out.append("</table>");

		out.append("<h2>PER LIGHTPATH INFORMATION SUMMARY - Signal metrics at the transponder</h2>");
		out.append("<table border='1'>");
		out.append(
				"<tr><th><b>Lightpath Id</b></th>" 
				+ "<th><b>Length (km)</b></th>"
				+ "<th><b># Occupied spectrum (#slots / GHz) </b></th>"
				+ "<th><b># EDFAs/DCMs traversed</b></th>"
				+ "<th><b># OADMs traversed</b></th>"
				+ "<th><b>Tx. Power [in add transponder] (dBm)</b></th>"
				+ "<th><b>Rx. Power [in drop transponder] (dBm)</b></th>"
				+ "<th><b>Rx. Chromatic Dispersion (ps/nm)</b></th>"
				+ "<th><b>Rx. OSNR (dB)</b></th>"
				+ "<th><b>Rx. PMD (ps)</b></th>"
				+ "<th><b>Warnings</b></th></tr>");
		for (WLightpath r : wNet.getLightpaths())
		{
			final String st_a_e = r.getA().getName();
			final String st_b_e = r.getB().getName();
			
			out.append("<tr>").
			append("<td>Id " + r.getId() + ". " + st_a_e + " --> " + st_b_e + "</td>").
			append("<td>" + df.apply(r.getLengthInKm()) + "</td>").
			append("<td>" + r.getOpticalSlotIds().size() + " (" + df.apply(r.getOpticalSlotIds().size()*WNetConstants.OPTICALSLOTSIZE_GHZ) + " GHz)" + "</td>").
			append("<td>" + r.getSeqFibers().stream().mapToInt(e->e.getNumberOfOpticalLineAmplifiersTraversed()).sum() + "</td>").
			append("<td>" + (r.getSeqFibers().size()+1) + "</td>").
			append("<td>" + df.apply(r.getAddTransponderInjectionPower_dBm()) + "</td>");
			final double powerReceiver_dBm = osim.getOpticalPerformanceAtTransponderReceiverEnd(r).getPower_dbm();
			final double cdReceiver_psPernm = osim.getOpticalPerformanceAtTransponderReceiverEnd(r).getCd_psPerNm();
			final double osnrReceiver_dB = osim.getOpticalPerformanceAtTransponderReceiverEnd(r).getOsnrAt12_5GhzRefBw();
			final double pmdReceiver_ps = Math.sqrt(osim.getOpticalPerformanceAtTransponderReceiverEnd(r).getPmdSquared_ps2());
			final boolean ok_powerReceiver = powerReceiver_dBm >= r.getTransponderMinimumTolerableReceptionPower_dBm() && powerReceiver_dBm <= r.getTransponderMaximumTolerableReceptionPower_dBm();
			final boolean ok_cdReceiver = Math.abs(cdReceiver_psPernm) <= r.getTransponderMaximumTolerableCdInAbsoluteValue_perPerNm();
			final boolean ok_osnrReceiver = osnrReceiver_dB >= r.getTransponderMinimumTolerableOsnrAt12_5GHzOfRefBw_dB();
			final boolean ok_pmdReceiver = pmdReceiver_ps <= r.getTransponderMaximumTolerablePmd_ps();
			out.append("<td bgcolor=\"" + (ok_powerReceiver?"PaleGreen":"Red") +"\">" + df.apply(powerReceiver_dBm) + "</td>");
			out.append("<td bgcolor=\"" + (ok_cdReceiver?"PaleGreen":"Red") +"\">" + df.apply(cdReceiver_psPernm) + "</td>");
			out.append("<td bgcolor=\"" + (ok_osnrReceiver?"PaleGreen":"Red") +"\">" + df.apply(osnrReceiver_dB) + "</td>");
			out.append("<td bgcolor=\"" + (ok_pmdReceiver?"PaleGreen":"Red") +"\">" + df.apply(pmdReceiver_ps) + "</td>");

			final StringBuffer st = new StringBuffer ();
			if (!ok_powerReceiver)
				st.append("<p>Rx power is " + df.apply(powerReceiver_dBm) + " dBm. It should be between [" + df.apply(r.getTransponderMinimumTolerableReceptionPower_dBm()) + ", " + df.apply(r.getTransponderMaximumTolerableReceptionPower_dBm()) + "] dBm</p>");
			if (!ok_cdReceiver)
				st.append("<p>Rx CD is " + df.apply(cdReceiver_psPernm) + " ps/nm. Absolute value should be below " + df.apply(r.getTransponderMaximumTolerableCdInAbsoluteValue_perPerNm())+ "</p>");
			if (!ok_osnrReceiver)
				st.append("<p>Rx OSNR is " + df.apply(osnrReceiver_dB) + " dB. Should be over " + df.apply(r.getTransponderMinimumTolerableOsnrAt12_5GHzOfRefBw_dB())+ "</p>");
			if (!ok_pmdReceiver)
				st.append("<p>Rx PMS is " + df.apply(pmdReceiver_ps) + " ps. Should be below " + df.apply(r.getTransponderMaximumTolerablePmd_ps())+ "</p>");
			out.append("<td>" + st.toString() + "</td>");
			out.append("</tr>");
		}
		out.append("</table>");

		if (printPerLightpathDetailedInformation)
		{
			out.append("<h2>PER LIGHTPATH INFORMATION DETAILED - performances along its way </h2>");
			for (WLightpath lp : wNet.getLightpaths())
			{
				out.append("<h3>LIGHTPATH " + lp.getId() + " - [" + lp.getA().getName() + " -> " + lp.getB().getName() + "] (" + df.apply(lp.getLengthInKm()) + " km)</h2>");
				out.append("<table border='1'>");
				out.append(
						"<tr><th><b>Node name</b></th>" 
						+ "<th><b>Node type</b></th>"
						+ "<th><b>Distance from lightpath origin (km)</b></th>"
						+ "<th><b>Power at input (dBm)</b></th>"
						+ "<th><b>Power at output (dBm)</b></th>"
						+ "<th><b>OSNR at input (dB)</b></th>"
						+ "<th><b>OSNR at output (dB)</b></th>"
						+ "<th><b>CD at input (ps/nm)</b></th>"
						+ "<th><b>CD at output (ps/nm)</b></th>"
						+ "<th><b>PMD at input (ps)</b></th>"
						+ "<th><b>PMD at output (ps)</b></th>"
						+ "</tr>");

				final WFiber firstFiber = lp.getSeqFibers().get(0);
				out.
				append("<tr>").
				append("<td>" + lp.getA().getName() + "</td>").
				append("<td>" + "OADM " + "</td>").
				append("<td>" + "0.0"  +"</td>").
				append("<td>" + df.apply(lp.getAddTransponderInjectionPower_dBm()) + " dBm" + "</td>").
				append("<td>" + df.apply(osim.getOpticalPerformanceOfLightpathAtFiberEnds(firstFiber , lp).getFirst().getPower_dbm()) + "</td>").
				append("<td> -- </td>").
				append("<td>" + df.apply(osim.getOpticalPerformanceOfLightpathAtFiberEnds(firstFiber , lp).getFirst().getOsnrAt12_5GhzRefBw()) + "</td>").
				append("<td> 0.0 </td>").
				append("<td>" + df.apply(osim.getOpticalPerformanceOfLightpathAtFiberEnds(firstFiber , lp).getFirst().getCd_psPerNm()) + "</td>").
				append("<td> 0.0 </td>").
				append("<td>" + df.apply(Math.sqrt(osim.getOpticalPerformanceOfLightpathAtFiberEnds(firstFiber , lp).getFirst().getPmdSquared_ps2())) + "</td>").
				append("</tr>");
				final List<WFiber> seqFibers = lp.getSeqFibers();
				final Function<Integer,Double> distanceAfterTravXFibers = f -> IntStream.range(0,f).mapToDouble(i->seqFibers.get(i).getLengthInKm()).sum () ;
				for (int contFiber = 0; contFiber < seqFibers.size() ; contFiber ++)
				{
					final WFiber e = seqFibers.get(contFiber);
					final int numOlas = e.getNumberOfOpticalLineAmplifiersTraversed();
					final boolean lastFiber = contFiber == seqFibers.size() - 1;
					final WFiber nextFiber = lastFiber ? null : seqFibers.get(contFiber + 1);
					final List<OpticalAmplifierInfo> olas = e.getOpticalLineAmplifiersInfo();
					for (int olaIndex = 0; olaIndex < numOlas ; olaIndex ++)
					{
						out.
						append("<tr>").
						append("<td>" + "OLA " + olaIndex + "</td>").
						append("<td>" + "OLA " + "</td>").
						append("<td>" + distanceAfterTravXFibers.apply(contFiber) + olas.get(olaIndex).getOlaPositionInKm().get() +"</td>").
						append("<td>" + df.apply(osim.getOpticalPerformanceOfLightpathAtAmplifierInputAndOutput(lp , e, olaIndex).getFirst().getPower_dbm()) + " dBm" + "</td>").
						append("<td>" + df.apply(osim.getOpticalPerformanceOfLightpathAtAmplifierInputAndOutput(lp , e, olaIndex).getSecond().getPower_dbm()) + " dBm" + "</td>").
						append("<td>" + df.apply(osim.getOpticalPerformanceOfLightpathAtAmplifierInputAndOutput(lp , e, olaIndex).getFirst().getOsnrAt12_5GhzRefBw()) + " dB" + "</td>").
						append("<td>" + df.apply(osim.getOpticalPerformanceOfLightpathAtAmplifierInputAndOutput(lp , e, olaIndex).getSecond().getOsnrAt12_5GhzRefBw()) + " dB" + "</td>").
						append("<td>" + df.apply(osim.getOpticalPerformanceOfLightpathAtAmplifierInputAndOutput(lp , e, olaIndex).getFirst().getCd_psPerNm()) + " ps/nm" + "</td>").
						append("<td>" + df.apply(osim.getOpticalPerformanceOfLightpathAtAmplifierInputAndOutput(lp , e, olaIndex).getSecond().getCd_psPerNm()) + " ps/nm" + "</td>").
						append("<td>" + df.apply(Math.sqrt(osim.getOpticalPerformanceOfLightpathAtAmplifierInputAndOutput(lp , e, olaIndex).getFirst().getPmdSquared_ps2())) + " ps" + "</td>").
						append("<td>" + df.apply(Math.sqrt(osim.getOpticalPerformanceOfLightpathAtAmplifierInputAndOutput(lp , e, olaIndex).getSecond().getPmdSquared_ps2())) + " ps" + "</td>").
						append("</tr>");
					}
					
					out.
					append("<tr>").
					append("<td>" + e.getB().getName() + "</td>").
					append("<td>" + "OADM " + "</td>").
					append("<td>" + distanceAfterTravXFibers.apply(contFiber+1) +"</td>").
					append("<td>" + df.apply(osim.getOpticalPerformanceOfLightpathAtFiberEnds(e , lp).getSecond().getPower_dbm())  +"</td>").
					append("<td>" + df.apply(lastFiber? osim.getOpticalPerformanceAtTransponderReceiverEnd(lp).getPower_dbm() : osim.getOpticalPerformanceOfLightpathAtFiberEnds(nextFiber , lp).getFirst().getPower_dbm())  +"</td>").
					append("<td>" + df.apply(osim.getOpticalPerformanceOfLightpathAtFiberEnds(e , lp).getSecond().getOsnrAt12_5GhzRefBw())  +"</td>").
					append("<td>" + df.apply(lastFiber? osim.getOpticalPerformanceAtTransponderReceiverEnd(lp).getOsnrAt12_5GhzRefBw() : osim.getOpticalPerformanceOfLightpathAtFiberEnds(nextFiber , lp).getFirst().getOsnrAt12_5GhzRefBw())  +"</td>").
					append("<td>" + df.apply(osim.getOpticalPerformanceOfLightpathAtFiberEnds(e , lp).getSecond().getCd_psPerNm())  +"</td>").
					append("<td>" + df.apply(lastFiber? osim.getOpticalPerformanceAtTransponderReceiverEnd(lp).getCd_psPerNm() : osim.getOpticalPerformanceOfLightpathAtFiberEnds(nextFiber , lp).getFirst().getCd_psPerNm())  +"</td>").
					append("<td>" + df.apply(Math.sqrt(osim.getOpticalPerformanceOfLightpathAtFiberEnds(e , lp).getSecond().getPmdSquared_ps2()))  +"</td>").
					append("<td>" + df.apply(Math.sqrt(lastFiber? osim.getOpticalPerformanceAtTransponderReceiverEnd(lp).getPmdSquared_ps2() : osim.getOpticalPerformanceOfLightpathAtFiberEnds(nextFiber , lp).getFirst().getPmdSquared_ps2()))  +"</td>").
					append("</tr>");
				}
				out.append("</table>");
			}
		}
		
		
		out.append("</body></html>");
		return out.toString();
	}

}
