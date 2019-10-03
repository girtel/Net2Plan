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

import com.net2plan.interfaces.networkDesign.IReport;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.niw.networkModel.OpticalSimulationModule;
import com.net2plan.niw.networkModel.OpticalSpectrumManager;
import com.net2plan.niw.networkModel.WFiber;
import com.net2plan.niw.networkModel.WLightpathUnregenerated;
import com.net2plan.niw.networkModel.WNet;
import com.net2plan.niw.networkModel.WNetConstants;
import com.net2plan.niw.networkModel.OpticalSimulationModule.PERLPINFOMETRICS;
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
 * @net2plan.keywords WDM, Multilayer
 * @author Pablo Pavon-Marino
 * @version 1.2, October 2017
 */
public class ReportNiw_wdm_lineEngineering implements IReport
{
	/* Input parameters */
	private WNet wNet;
	private Map<String, String> reportParameters;

	@Override
	public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
	{
		/* Input parameters */
		this.wNet = new WNet (netPlan);
		wNet.updateNetPlanObjectInternalState();
		
		this.reportParameters = reportParameters;

		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, reportParameters);
		
		return printReport();
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
			append("<td>" + df_2.format(e.getLengthInKm()) + "</td>").
			append("<td>" + e.getValidOpticalSlotRanges() + "</td>").
			append("<td>" + e.getNumberOfOpticalLineAmplifiersTraversed() + "</td>").
			append("<td>" + e.getTraversingLps().size() + "</td>").
			append("<td>" + osm.getOccupiedOpticalSlotIds(e).size() + " / " + e.getNumberOfValidOpticalChannels() + " (" + df_2.format(100.0 * osm.getOccupiedOpticalSlotIds(e).size() /  e.getNumberOfValidOpticalChannels())   + "%)</td>").
			append("<td>" + df_2.format(e.getAccumulatedChromaticDispersion()) + "</td>").
			append("<td>" + df_2.format(osim.getTotalPowerAtFiberEnds(e).getFirst()) + "</td>").
			append("<td>" + df_2.format(osim.getTotalPowerAtFiberEnds(e).getSecond()) + "</td>");
			
			final StringBuffer st = new StringBuffer ();
			for (int contOla = 0; contOla < e.getNumberOfOpticalLineAmplifiersTraversed() ; contOla ++)
			{
				final double distKm = e.getAmplifierPositionsKmFromOrigin_km().get(contOla);
				final double powerAtInput_dBm = osim.getTotalPowerAtAmplifierInput_dBm(e, contOla);
				final double gain_dB = e.getAmplifierGains_dB().get(contOla);
				if (powerAtInput_dBm < e.getAmplifierMinAcceptableInputPower_dBm().get(contOla) || powerAtInput_dBm > e.getAmplifierMaxAcceptableInputPower_dBm().get(contOla))
					st.append("<p>EDFA-" + contOla + " ("+ df_2.format(distKm) + " km)" + ": Power at the input is " + df_2.format(powerAtInput_dBm) + " dBm. It should be between [" + df_2.format(e.getAmplifierMinAcceptableInputPower_dBm().get(contOla)) + ", " + df_2.format(e.getAmplifierMaxAcceptableInputPower_dBm().get(contOla)) + "] dBm</p>");
				if (gain_dB < e.getAmplifierMinAcceptableGains_dB().get(contOla) || gain_dB > e.getAmplifierMaxAcceptableGains_dB().get(contOla))
					st.append("<p>EDFA-" + contOla + " ("+ df_2.format(distKm) + " km)" + ": Gain is " + df_2.format(gain_dB) + " dB. It should be between [" + df_2.format(e.getAmplifierMinAcceptableGains_dB().get(contOla)) + ", " + df_2.format(e.getAmplifierMaxAcceptableGains_dB().get(contOla)) + "] dBm</p>");
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
		for (WLightpathUnregenerated r : wNet.getLightpaths())
		{
			final String st_a_e = r.getA().getName();
			final String st_b_e = r.getB().getName();
			
			out.append("<tr>").
			append("<td>Id " + r.getId() + ". " + st_a_e + " --> " + st_b_e + "</td>").
			append("<td>" + df_2.format(r.getLengthInKm()) + "</td>").
			append("<td>" + r.getOpticalSlotIds().size() + " (" + df_2.format(r.getOpticalSlotIds().size()*WNetConstants.OPTICALSLOTSIZE_GHZ) + " GHz)" + "</td>").
			append("<td>" + r.getSeqFibers().stream().mapToInt(e->e.getNumberOfOpticalLineAmplifiersTraversed()).sum() + "</td>").
			append("<td>" + (r.getSeqFibers().size()+1) + "</td>").
			append("<td>" + df_2.format(r.getAddTransponderInjectionPower_dBm()) + "</td>");
			final double powerReceiver_dBm = osim.getOpticalPerformanceAtTransponderReceiverEnd_dBm(r).get(PERLPINFOMETRICS.POWER_DBM);
			final double cdReceiver_psPernm = osim.getOpticalPerformanceAtTransponderReceiverEnd_dBm(r).get(PERLPINFOMETRICS.CD_PERPERNM);
			final double osnrReceiver_dB = osim.getOpticalPerformanceAtTransponderReceiverEnd_dBm(r).get(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW);
			final double pmdReceiver_ps = Math.sqrt(osim.getOpticalPerformanceAtTransponderReceiverEnd_dBm(r).get(PERLPINFOMETRICS.PMDSQUARED_PS2));
			final boolean ok_powerReceiver = powerReceiver_dBm >= r.getTransponderMinimumTolerableReceptionPower_dBm() && powerReceiver_dBm <= r.getTransponderMinimumTolerableReceptionPower_dBm();
			final boolean ok_cdReceiver = Math.abs(cdReceiver_psPernm) <= r.getTransponderMinimumTolerableCdInAbsoluteValue_perPerNm();
			final boolean ok_osnrReceiver = osnrReceiver_dB >= r.getTransponderMinimumTolerableOsnrAt12_5GHzOfRefBw_dB();
			final boolean ok_pmdReceiver = pmdReceiver_ps >= r.getTransponderMaximumTolerablePmd_ps();
			out.append("<td bgcolor=\"" + (ok_powerReceiver?"PaleGreen":"Red") +"\">" + df_2.format(powerReceiver_dBm) + "</td>");
			out.append("<td bgcolor=\"" + (ok_cdReceiver?"PaleGreen":"Red") +"\">" + df_2.format(cdReceiver_psPernm) + "</td>");
			out.append("<td bgcolor=\"" + (ok_osnrReceiver?"PaleGreen":"Red") +"\">" + df_2.format(osnrReceiver_dB) + "</td>");
			out.append("<td bgcolor=\"" + (ok_pmdReceiver?"PaleGreen":"Red") +"\">" + df_2.format(pmdReceiver_ps) + "</td>");

			final StringBuffer st = new StringBuffer ();
			if (!ok_powerReceiver)
				st.append("<p>Rx power is " + df_2.format(powerReceiver_dBm) + " dBm. It should be between [" + df_2.format(r.getTransponderMinimumTolerableReceptionPower_dBm()) + ", " + df_2.format(r.getTransponderMinimumTolerableReceptionPower_dBm()) + "] dBm</p>");
			if (!ok_cdReceiver)
				st.append("<p>Rx CD is " + df_2.format(cdReceiver_psPernm) + " ps/nm. Absolute value should be below " + df_2.format(r.getTransponderMinimumTolerableCdInAbsoluteValue_perPerNm())+ "</p>");
			if (!ok_osnrReceiver)
				st.append("<p>Rx OSNR is " + df_2.format(osnrReceiver_dB) + " dB. Should be over " + df_2.format(r.getTransponderMinimumTolerableOsnrAt12_5GHzOfRefBw_dB())+ "</p>");
			if (!ok_pmdReceiver)
				st.append("<p>Rx PMS is " + df_2.format(pmdReceiver_ps) + " ps. Should be below " + df_2.format(r.getTransponderMaximumTolerablePmd_ps())+ "</p>");
			out.append("<td>" + st.toString() + "</td>");
			out.append("</tr>");
		}
		out.append("</table>");

		out.append("</body></html>");
		return out.toString();
	}

}
