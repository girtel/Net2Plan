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
package com.net2plan.examples.general.reports;

import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Constants.OrderingType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import java.text.DecimalFormat;
import java.util.*;

/**
 * <p>
 * This report shows line engineering information for WDM links in a multilayer optical network. The
 * impairment calculations are inspired in the procedures described in the 2009 ITU-T WDM manual
 * "Optical fibres, cabbles and systems".
 * </p>
 * <p>
 * The report assumes that the WDM network follows the scheme:
 * </p>
 * <ul>
 * <li>In the net2plan object, nodes are OADMs, links are fiber links, and routes are lightpaths:
 * WDM channels optically switched at intermediate nodes.</li>
 * <li>Nodes are connected by unidirectional fiber links. Fiber link distance is given by the link
 * length. Other specifications are given by fiber_XXX input parameters. The fiber can be split into
 * spans if optical amplifers (EDFAs) and/or dispersion compensating modules (DCMs) are placed along
 * the fiber.</li>
 * <li>Optical line amplifiers (EDFAs) can be located in none, one or more positions in the fiber
 * link, separating them in different spans. EDFAs are supposed to operate in the automatic gain
 * control mode. Thus, the gain is the same, whatever the number of input WDM channels. EDFA
 * positions (as distance in km from the link start to the EDFA location) and EDFA gains (assumed in
 * dB) are read from the "edfaPositions_km" and "edfaGains_dB" attributes of the links. The format
 * of both attributes are the same: a string of numbers separated by spaces. The <i>i</i>-th number
 * corresponding to the position/gain of the <i>i</i>-th EDFA. If the attributes do not exist, it is
 * assumed that no EDFAs are placed in this link. EDFA specifications are given by "edfa_XXX"
 * parameters</li>
 * <li>Dispersion compensating modules (DCMs) can be located in none, one or more positions in the
 * fiber link, separating them in different spans. If a DCM and a EDFA have the same location, it is
 * assumed that the DCM is placed first, to reduce the non-linear effects. DCM positions (as
 * distance in km from the link start to the DCM location) are read from the "dcmPositions_km"
 * attribute of the link, and the same format as with "edfaPositions_km" attribute is expected. If
 * the attribute does not exist, it is assumed that no DCMs are placed in this link. DCM
 * specifications are given by "dcm_XXX" parameters</li>
 * <li>Fiber links start and end in OADM modules, that permit adding, dropping and optically switch
 * individual WDM channels. OADMs have a pre-amplifier (traversed by drop and express channels) and
 * a boost amplifier (traversed by add and express channels). They are supposed to equalize the
 * channel power at their outputs, to a fixed value (added and express channels will thus have the
 * same power in the fibers). Also, OADMs attenuate appropriately the optical signal coming from the
 * pre-amplifier, in the drop channels, so that they fall within the receiver sensitivity range.
 * OADM noise figures for add, drop and express channels are given as input parameters. PMD values
 * for add, drop and express channels are computed assumming that: (i) add channel traverse a
 * multiplexer and the booster, (ii) drop channels travese the pre-amplifier and a demultiplexer,
 * (iii) express channels traverse the two amplifiers. The required parameters are provided in
 * oadm_XXX parameters.</li>
 * <li>Each channel ends in a receiver, with specifications given by "tp_XXX" parameters.</li>
 * </ul>
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
public class Report_WDM_lineEngineering implements IReport
{
	/* Constants */
	private final static double constant_c = 299792458; /* speed of light in m/s */
	private final static double constant_h = 6.626E-34; /* Plank constant m^2 kg/sec */

	/* Input parameters */
	private NetPlan netPlan;
	private Map<String, String> reportParameters;

	/* Usable wavelengths */
	private final InputParameter channels_minChannelLambda_nm = new InputParameter("channels_minChannelLambda_nm", (double) 1530.33, "Channel minimum wavelength in nm");
	private final InputParameter channels_channelSpacing_GHz = new InputParameter("channels_channelSpacing_GHz", (double) 100, "Channel spacing in GHz");
	private final InputParameter channels_maxNumChannels = new InputParameter("channels_maxNumChannels", (int) 16, "Maximum number of WDM channels that will be used");
	private double channels_maxChannelLambda_nm;

	/* Fiber specifications */
	private final InputParameter fiber_attenuation_dB_per_km = new InputParameter("fiber_attenuation_dB_per_km", (double) 0.25, "Fiber attenuation in dB/km");
	private final InputParameter fiber_worseChromaticDispersion_ps_per_nm_per_km = new InputParameter("fiber_worseChromaticDispersion_ps_per_nm_per_km", (double) 6, "Chromatic dispersion of the fiber in ps/nm/km");
	private final InputParameter fiber_PMD_ps_per_sqroot_km = new InputParameter("fiber_PMD_ps_per_sqroot_km", (double) 0.4, "Polarization mode dispersion per km^0.5 of fiber (PMD_Q link factor)");

	/* Transponder specifications */
	private final InputParameter tp_maxChromaticDispersionTolerance_ps_per_nm = new InputParameter("tp_maxChromaticDispersionTolerance_ps_per_nm", (double) 800, "Maximum chromatic dispersion tolerance in ps/nm at the receiver");
	private final InputParameter tp_minOSNR_dB = new InputParameter("tp_minOSNR_dB", (double) 7, "Minimum OSNR needed at the receiver");
	private final InputParameter tp_minWavelength_nm = new InputParameter("tp_minWavelength_nm", (double) 1529.55, "Minimum wavelength usable by the transponder");
	private final InputParameter tp_maxWavelength_nm = new InputParameter("tp_maxWavelength_nm", (double) 1561.84, "Maximum wavelength usable by the transponder");
	private final InputParameter tp_pmdTolerance_ps = new InputParameter("tp_pmdTolerance_ps", (double) 10, "Maximum tolarance of polarizarion mode dispersion (mean of differential group delay) in ps at the receiver");
	private final InputParameter tp_inputPowerSensitivityMin_dBm = new InputParameter("tp_inputPowerSensitivityMin_dBm", (double) -20, "Minimum input power at the receiver in dBm");
	private final InputParameter tp_inputPowerSensitivityMax_dBm = new InputParameter("tp_inputPowerSensitivityMax_dBm", (double) -8, "Maximum input power at the receiver in dBm");

	/* OADM specs */
	private final InputParameter oadm_perChannelOutputPower_dBm = new InputParameter("oadm_perChannelOutputPower_dBm", (double) 6, "Output power per channel at the OADM in dBm");
	private final InputParameter oadm_perChannelMinInputPower_dBm = new InputParameter("oadm_perChannelMinInputPower_dBm", (double) -19, "Minimum power needed at the OADM input");
	private final InputParameter oadm_perChannelMaxInputPower_dBm = new InputParameter("oadm_perChannelMaxInputPower_dBm", (double) 1000, "Maximum power admitted at the OADM input");
	private final InputParameter oadm_muxDemuxPMD_ps = new InputParameter("oadm_muxDemuxPMD_ps", (double) 0.5, "PMD of the mux/demux inside the OADMs. Does not affect express lightpaths");
	private final InputParameter oadm_preAmplifierPMD_ps = new InputParameter("oadm_preAmplifierPMD_ps", (double) 0.5, "PMD off OADM preamplifier");
	private final InputParameter oadm_boosterPMD_ps = new InputParameter("oadm_boosterPMD_ps", (double) 0.5, "PMD off OADM booster amplifier");
	private final InputParameter oadm_addChannelNoiseFactor_dB = new InputParameter("oadm_addChannelNoiseFactor_dB", (double) 6, "Noise factor observed by add channels");
	private final InputParameter oadm_dropChannelNoiseFactor_dB = new InputParameter("oadm_dropChannelNoiseFactor_dB", (double) 6, "Noise factor observed by drop channels");
	private final InputParameter oadm_expressChannelNoiseFactor_dB = new InputParameter("oadm_expressChannelNoiseFactor_dB", (double) 10, "Noise factor observed by express channels");
	private final InputParameter oadm_noiseFactorReferenceBandwidth_nm = new InputParameter("oadm_expressChannelNoiseFactor_dB", (double) 10, "Noise factor observed by express channels");
	private final InputParameter oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm = new InputParameter("oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm", (double) 150,
			"Maximum chromatic dispersion tolerance in ps/nm at te end of each link (absolute value)");

	/* Optical line amplifier specifications */
	private final InputParameter edfa_minWavelength_nm = new InputParameter("edfa_minWavelength_nm", (double) 1530, "Minimum wavelength usable by the EDFA");
	private final InputParameter edfa_maxWavelength_nm = new InputParameter("edfa_maxWavelength_nm", (double) 1563, "Maximum wavelength usable by the EDFA");
	private final InputParameter edfa_minInputPower_dBm = new InputParameter("edfa_minInputPower_dBm", (double) -29, "Minimum input power at the EDFA");
	private final InputParameter edfa_maxInputPower_dBm = new InputParameter("edfa_maxInputPower_dBm", (double) 2, "Maximum input power at the EDFA");
	private final InputParameter edfa_minOutputPower_dBm = new InputParameter("edfa_minOutputPower_dBm", (double) -6, "Minimum output power at the EDFA");
	private final InputParameter edfa_maxOutputPower_dBm = new InputParameter("edfa_maxOutputPower_dBm", (double) 19, "Maximum output power at the EDFA");
	private final InputParameter edfa_minGain_dB = new InputParameter("edfa_minGain_dB", (double) 17, "Minimum gain at the EDFA");
	private final InputParameter edfa_maxGain_dB = new InputParameter("edfa_maxGain_dB", (double) 23, "Maximum gain at the EDFA");
	private final InputParameter edfa_PMD_ps = new InputParameter("edfa_PMD_ps", (double) 0.5, "Polarization mode dispersion in ps added by the EDFA");
	private final InputParameter edfa_noiseFactorMaximumGain_dB = new InputParameter("edfa_noiseFactorMaximumGain_dB", (double) 5,
			"Noise factor at the EDFA when the gain is in its upper limit (linear interpolation is used to estimate the noise figura at other gains)");
	private final InputParameter edfa_noiseFactorMinimumGain_dB = new InputParameter("edfa_noiseFactorMinimumGain_dB", (double) 5,
			"Noise factor at the EDFA when the gain is in its lower limit (linear interpolation is used to estimate the noise figura at other gains)");
	private final InputParameter edfa_noiseFactorReferenceBandwidth_nm = new InputParameter("edfa_noiseFactorReferenceBandwidth_nm", (double) 0.5, "Reference bandwidth that measures the noise factor at the EDFA");

	/* Dispersion compensation modules specifications */
	private final InputParameter dcm_channelDispersionMax_ps_per_nm  = new InputParameter("dcm_channelDispersionMax_ps_per_nm", (double)-827, "Max (in absolute value) dispersion compensation a DCM can place (ps/nm). It is assumed to be the same for all wavelengths");
	private final InputParameter dcm_channelDispersionMin_ps_per_nm = new InputParameter("dcm_channelDispersionMin_ps_per_nm", (double)-276, "Min (in absolute value) dispersion compensation a DCM can place (ps/nm). It is assumed to be the same for all wavelengths");
	private final InputParameter dcm_PMD_ps = new InputParameter("dcm_PMD_ps", (double) 0.7, "Polarization mode dispersion in ps added by the DCM");
	private final InputParameter dcm_insertionLoss_dB = new InputParameter("dcm_insertionLoss_dB", (double) 3.5, "Maximum insertion loss added by the DCM");

	/* OSNR penalties */
	private final InputParameter osnrPenalty_CD_dB = new InputParameter("osnrPenalty_CD_dB", (double) 1, "OSNR penalty caused by residual chromatic dispersion (assumed within limits)");
	private final InputParameter osnrPenalty_nonLinear_dB = new InputParameter("osnrPenalty_nonLinear_dB", (double) 2, "OSNR penalty caused by the non-linear effects SPM, XPM, FWM and Brillouin / Raman scattering");
	private final InputParameter osnrPenalty_PMD_dB = new InputParameter("osnrPenalty_PMD_dB", (double) 0.5, "OSNR penalty caused by the polarization mode dispersion (assumed within limits)");
	private final InputParameter osnrPenalty_PDL_dB = new InputParameter("osnrPenalty_PDL_dB", (double) 0.3, "OSNR penalty caused by polarization dispersion losses");
	private final InputParameter osnrPenalty_transmitterChirp_dB = new InputParameter("osnrPenalty_transmitterChirp_dB", (double) 0.5, "OSNR penalty caused by transmitter chirp ");
	private final InputParameter osnrPenalty_OADMCrosstalk_dB = new InputParameter("osnrPenalty_OADMCrosstalk_dB", (double) 0.8, "OSNR penalty caused by the crosstalk at the OADMs");
	private final InputParameter osnrPenalty_unassignedMargin_dB = new InputParameter("osnrPenalty_unassignedMargin_dB", (double) 3, "OSNR penalty caused by not assigned margins (e.g. random effects, aging, ...)");
	private double osnrPenalty_SUM_dB;

	@Override
	public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
	{
		/* Input parameters */
		this.netPlan = netPlan;
		this.reportParameters = reportParameters;

		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, reportParameters);
		
		/* Usable wavelengths */
		channels_maxChannelLambda_nm = constant_c / ((constant_c / channels_minChannelLambda_nm.getDouble()) - (channels_maxNumChannels.getInt() - 1) * channels_channelSpacing_GHz.getDouble());
		
		/* OSNR penalties */
		osnrPenalty_SUM_dB = osnrPenalty_CD_dB.getDouble() + osnrPenalty_nonLinear_dB.getDouble() + osnrPenalty_PMD_dB.getDouble() + osnrPenalty_PDL_dB.getDouble()
		+ osnrPenalty_transmitterChirp_dB.getDouble() + osnrPenalty_OADMCrosstalk_dB.getDouble() + osnrPenalty_unassignedMargin_dB.getDouble();

		Map<Link, LinkedList<Triple<Double, String, Double>>> elements_e = new LinkedHashMap<Link, LinkedList<Triple<Double, String, Double>>>();
		Map<Link, LinkedList<Pair<double[], double[]>>> impairments_e = new LinkedHashMap<Link, LinkedList<Pair<double[], double[]>>>();
		Map<Link, LinkedList<String>> warnings_e = new LinkedHashMap<Link, LinkedList<String>>();

		for (Link link : netPlan.getLinks())
		{
			final List<Link> seqLinks = new LinkedList<Link>();
			seqLinks.add(link);
			final LinkedList<Triple<Double, String, Double>> elementPositions = getElementPositionsList(seqLinks);
			final LinkedList<Pair<double[], double[]>> impairmentsAtInputAndOutputs = computeImpairments(elementPositions);
			final LinkedList<String> warningMessages = computeWarningMessages(elementPositions, impairmentsAtInputAndOutputs);

			elements_e.put(link, elementPositions);
			impairments_e.put(link, impairmentsAtInputAndOutputs);
			warnings_e.put(link, warningMessages);
		}

		Map<Route, LinkedList<Triple<Double, String, Double>>> elements_r = new LinkedHashMap<Route, LinkedList<Triple<Double, String, Double>>>();
		Map<Route, LinkedList<Pair<double[], double[]>>> impairments_r = new LinkedHashMap<Route, LinkedList<Pair<double[], double[]>>>();
		Map<Route, LinkedList<String>> warnings_r = new LinkedHashMap<Route, LinkedList<String>>();
		for (Route r : netPlan.getRoutes())
		{
			final List<Link> seqLinks = r.getSeqLinks();
			final LinkedList<Triple<Double, String, Double>> elementPositions = getElementPositionsList(seqLinks);
			final LinkedList<Pair<double[], double[]>> impairmentsAtInputAndOutputs = computeImpairments(elementPositions);
			final LinkedList<String> warningMessages = computeWarningMessages(elementPositions, impairmentsAtInputAndOutputs);

			elements_r.put(r, elementPositions);
			impairments_r.put(r, impairmentsAtInputAndOutputs);
			warnings_r.put(r, warningMessages);
		}

		return printReport(elements_e, impairments_e, warnings_e, elements_r, impairments_r, warnings_r);
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

	private LinkedList<Pair<double[], double[]>> computeImpairments(LinkedList<Triple<Double, String, Double>> elementPositions)
	{
		LinkedList<Pair<double[], double[]>> res = new LinkedList<Pair<double[], double[]>>();

		/* In the transmitter */
		double current_powerPerChannel_dBm = oadm_perChannelOutputPower_dBm.getDouble();
		double current_CD_ps_per_nm = 0;
		double current_PMDSquared_ps2 = 0;
		double current_OSNR_linear = Double.MAX_VALUE; /* no noise */

		if (!elementPositions.getFirst().getSecond().equalsIgnoreCase("OADM-ADD"))
			throw new RuntimeException("The route should start in a OADM-ADD element");
		if (!elementPositions.getLast().getSecond().equalsIgnoreCase("OADM-DROP"))
			throw new RuntimeException("The route should end in a OADM-DROP element");

		for (Triple<Double, String, Double> element : elementPositions)
		{
			final String name = element.getSecond();
			final double elementData = element.getThird();

			double[] prevState = DoubleUtils.arrayOf(current_powerPerChannel_dBm, current_CD_ps_per_nm, current_PMDSquared_ps2, current_OSNR_linear);
			if (name.equalsIgnoreCase("OADM-ADD"))
			{
				current_OSNR_linear = updateOSNRAfterEDFA(Double.MAX_VALUE, oadm_addChannelNoiseFactor_dB.getDouble(), oadm_noiseFactorReferenceBandwidth_nm.getDouble(), current_powerPerChannel_dBm);
				current_powerPerChannel_dBm = oadm_perChannelOutputPower_dBm.getDouble();
				current_CD_ps_per_nm = 0;
				current_PMDSquared_ps2 = Math.pow(oadm_muxDemuxPMD_ps.getDouble(), 2) + Math.pow(oadm_boosterPMD_ps.getDouble(), 2);

			} else if (name.equalsIgnoreCase("OADM-EXPRESS"))
			{
				current_OSNR_linear = updateOSNRAfterEDFA(current_OSNR_linear, oadm_expressChannelNoiseFactor_dB.getDouble(), oadm_noiseFactorReferenceBandwidth_nm.getDouble(), current_powerPerChannel_dBm);
				current_powerPerChannel_dBm = oadm_perChannelOutputPower_dBm.getDouble();
				current_PMDSquared_ps2 += Math.pow(oadm_preAmplifierPMD_ps.getDouble(), 2) + Math.pow(oadm_boosterPMD_ps.getDouble(), 2);
			} else if (name.equalsIgnoreCase("OADM-DROP"))
			{
				current_OSNR_linear = updateOSNRAfterEDFA(current_OSNR_linear, oadm_dropChannelNoiseFactor_dB.getDouble(), oadm_noiseFactorReferenceBandwidth_nm.getDouble(), current_powerPerChannel_dBm);
				current_powerPerChannel_dBm = (tp_inputPowerSensitivityMin_dBm.getDouble() + tp_inputPowerSensitivityMax_dBm.getDouble()) / 2;
				current_PMDSquared_ps2 += Math.pow(oadm_preAmplifierPMD_ps.getDouble(), 2);
			} else if (name.equalsIgnoreCase("SPAN"))
			{
				final double spanLength_km = elementData;
				current_powerPerChannel_dBm -= fiber_attenuation_dB_per_km.getDouble() * spanLength_km;
				current_CD_ps_per_nm += fiber_worseChromaticDispersion_ps_per_nm_per_km.getDouble() * spanLength_km;
				current_PMDSquared_ps2 += spanLength_km * Math.pow(fiber_PMD_ps_per_sqroot_km.getDouble(), 2);
			} else if (name.equalsIgnoreCase("EDFA"))
			{
				final double edfaGain_dB = elementData;
				final double edfa_noiseFactorThisGain_dB = edfa_noiseFactorMinimumGain_dB.getDouble() + (edfaGain_dB - edfa_minGain_dB.getDouble()) * (edfa_noiseFactorMaximumGain_dB.getDouble() - edfa_noiseFactorMinimumGain_dB.getDouble()) / (edfa_maxGain_dB.getDouble() - edfa_minGain_dB.getDouble());
				if ((edfa_noiseFactorThisGain_dB < Math.min(edfa_noiseFactorMinimumGain_dB.getDouble(), edfa_noiseFactorMaximumGain_dB.getDouble())) || (edfa_noiseFactorThisGain_dB > Math.max(edfa_noiseFactorMinimumGain_dB.getDouble(), edfa_noiseFactorMaximumGain_dB.getDouble())))
					throw new RuntimeException("Bad");

				current_OSNR_linear = updateOSNRAfterEDFA(current_OSNR_linear, edfa_noiseFactorThisGain_dB, edfa_noiseFactorReferenceBandwidth_nm.getDouble(), current_powerPerChannel_dBm);
				current_powerPerChannel_dBm += edfaGain_dB;
				current_PMDSquared_ps2 += Math.pow(edfa_PMD_ps.getDouble(), 2);
			} else if (name.equalsIgnoreCase("DCM"))
			{
				final double cdCompensated_ps_per_nm = elementData;
				current_powerPerChannel_dBm -= dcm_insertionLoss_dB.getDouble();
				current_CD_ps_per_nm += cdCompensated_ps_per_nm;
				current_PMDSquared_ps2 += Math.pow(dcm_PMD_ps.getDouble(), 2);
			} else
			{
				throw new RuntimeException("Unknown element type");
			}

			double[] postState = DoubleUtils.arrayOf(current_powerPerChannel_dBm, current_CD_ps_per_nm, current_PMDSquared_ps2, current_OSNR_linear);
			res.add(Pair.of(prevState, postState));
		}
		return res;
	}

	private LinkedList<String> computeWarningMessages(LinkedList<Triple<Double, String, Double>> elementPositions, LinkedList<Pair<double[], double[]>> impairmentsAtInputAndOutputs)
	{
		final double numChannels_dB = linear2dB(channels_maxNumChannels.getInt());
		LinkedList<String> res = new LinkedList<String>();

		Iterator<Triple<Double, String, Double>> it_elementPositions = elementPositions.iterator();
		Iterator<Pair<double[], double[]>> it_impairments = impairmentsAtInputAndOutputs.iterator();

		int numberOfExpressOADMs = 0;
		while (it_elementPositions.hasNext())
		{
			String st = "";

			final Triple<Double, String, Double> thisElement = it_elementPositions.next();
			final Pair<double[], double[]> thisImpairments = it_impairments.next();

			final double initialPosition_km = thisElement.getFirst();
			final String name = thisElement.getSecond();
			final double elementData = thisElement.getThird();

			final double[] prevImp = thisImpairments.getFirst();
			final double[] postImp = thisImpairments.getSecond();

			final double pre_powerPerChannel_dBm = prevImp[0];
			final double pre_CD_ps_per_nm = prevImp[1];

			final double post_powerPerChannel_dBm = postImp[0];
			final double post_CD_ps_per_nm = postImp[1];
			final double post_PMDSquared_ps2 = postImp[2];
			final double post_OSNR_linear = postImp[3];

			if (name.equalsIgnoreCase("OADM-ADD"))
			{
				/* Wavelengths in use within transponder range */
				if (channels_minChannelLambda_nm.getDouble() < tp_minWavelength_nm.getDouble())
					st += "Wavelength " + channels_minChannelLambda_nm + " nm is outside the transponder range [" + tp_minWavelength_nm + " nm, " + tp_maxWavelength_nm + " nm]";
				if (channels_maxChannelLambda_nm > tp_maxWavelength_nm.getDouble())
					st += "Wavelength " + channels_maxChannelLambda_nm + " nm is outside the transponder range [" + tp_minWavelength_nm + " nm, " + tp_maxWavelength_nm + " nm]";

				/* Output power within limits */
				if (Math.abs(post_powerPerChannel_dBm - oadm_perChannelOutputPower_dBm.getDouble()) > 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-ADD output is " + post_powerPerChannel_dBm + " dBm. It should be: " + oadm_perChannelOutputPower_dBm;
			} else if (name.equalsIgnoreCase("OADM-EXPRESS"))
			{
				/* Input power within limits */
				if (pre_powerPerChannel_dBm < oadm_perChannelMinInputPower_dBm.getDouble() - 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-EXPRESS input is " + pre_powerPerChannel_dBm + " dBm. It should be between [" + oadm_perChannelMinInputPower_dBm + ", " + oadm_perChannelMaxInputPower_dBm + "] dBm";
				if (pre_powerPerChannel_dBm > oadm_perChannelMaxInputPower_dBm.getDouble() + 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-EXPRESS input is " + pre_powerPerChannel_dBm + " dBm. It should be between [" + oadm_perChannelMinInputPower_dBm + ", " + oadm_perChannelMaxInputPower_dBm + "] dBm";

				/* Output power within limits */
				if (Math.abs(post_powerPerChannel_dBm - oadm_perChannelOutputPower_dBm.getDouble()) > 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-EXPRESS output is " + post_powerPerChannel_dBm + " dBm. It should be: " + oadm_perChannelOutputPower_dBm;

				numberOfExpressOADMs++;
			} else if (name.equalsIgnoreCase("OADM-DROP"))
			{
				final double cdLimit = numberOfExpressOADMs == 0 ? oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm.getDouble() : tp_maxChromaticDispersionTolerance_ps_per_nm.getDouble();

				/* CD at input */
				if (Math.abs(pre_CD_ps_per_nm) > cdLimit + 1E-3)
					st += "At " + initialPosition_km + "km: CD at the input of OADM-DROP is " + pre_CD_ps_per_nm + " dBm. It should be in absolute value below: " + cdLimit;

				/* Input power within limits */
				if (pre_powerPerChannel_dBm < oadm_perChannelMinInputPower_dBm.getDouble() - 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-DROP input is " + pre_powerPerChannel_dBm + " dBm. It should be between [" + oadm_perChannelMinInputPower_dBm + "," + oadm_perChannelMaxInputPower_dBm + "] dBm";
				if (pre_powerPerChannel_dBm > oadm_perChannelMaxInputPower_dBm.getDouble() + 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-DROP input is " + pre_powerPerChannel_dBm + " dBm. It should be between [" + oadm_perChannelMinInputPower_dBm + "," + oadm_perChannelMaxInputPower_dBm + "] dBm";

				/* Output power within limits */
				if (post_powerPerChannel_dBm < tp_inputPowerSensitivityMin_dBm.getDouble() - 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-DROP output is " + post_powerPerChannel_dBm + ". It should be between [" + tp_inputPowerSensitivityMin_dBm + "," + tp_inputPowerSensitivityMax_dBm + "] dBm";
				if (post_powerPerChannel_dBm > tp_inputPowerSensitivityMax_dBm.getDouble() + 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-DROP output is " + post_powerPerChannel_dBm + ". It should be between [" + tp_inputPowerSensitivityMin_dBm + "," + tp_inputPowerSensitivityMax_dBm + "] dBm";

				/* Chromatic dispersion in the receiver */
				if (post_CD_ps_per_nm > tp_maxChromaticDispersionTolerance_ps_per_nm.getDouble())
					st += "At " + initialPosition_km + "km: Chromatic dispersion at the RECEIVER is " + post_CD_ps_per_nm + " ps/nm. It should be within +- " + tp_maxChromaticDispersionTolerance_ps_per_nm + " ps/nm";
				if (post_CD_ps_per_nm < -tp_maxChromaticDispersionTolerance_ps_per_nm.getDouble())
					st += "At " + initialPosition_km + "km: Chromatic dispersion at the RECEIVER is " + post_CD_ps_per_nm + " ps/nm. It should be within +- " + tp_maxChromaticDispersionTolerance_ps_per_nm + " ps/nm";

				/* OSNR within limits */
				if (linear2dB(post_OSNR_linear) < tp_minOSNR_dB.getDouble() + osnrPenalty_SUM_dB)
					st += "At " + initialPosition_km + "km: OSNR at the RECEIVER is " + linear2dB(post_OSNR_linear) + " dB. It is below the tolerance plus margin " + tp_minOSNR_dB + " dB " + osnrPenalty_SUM_dB + " dB = "
							+ (tp_minOSNR_dB.getDouble() + osnrPenalty_SUM_dB) + " dB)";

				/* PMD tolerance at the receiver */
				final double pmdAtReceiver = Math.sqrt(post_PMDSquared_ps2);
				if (pmdAtReceiver > tp_pmdTolerance_ps.getDouble())
					st += "At " + initialPosition_km + "km: PMD at the RECEIVER is " + pmdAtReceiver + " ps. It is above the maximum PMD tolerance (" + tp_pmdTolerance_ps + " ps)";
			} else if (name.equalsIgnoreCase("SPAN"))
			{} else if (name.equalsIgnoreCase("EDFA"))
			{
				final double edfaGain_dB = elementData;

				/* Wavelengths within limits */
				if (channels_minChannelLambda_nm.getDouble() < edfa_minWavelength_nm.getDouble())
					st += "Wavelength " + channels_minChannelLambda_nm + " nm is outside the transponder range [" + edfa_minWavelength_nm + " nm, " + edfa_maxWavelength_nm + " nm]";
				if (channels_maxChannelLambda_nm > edfa_maxWavelength_nm.getDouble())
					st += "Wavelength " + channels_maxChannelLambda_nm + " nm is outside the transponder range [" + edfa_minWavelength_nm + " nm, " + edfa_maxWavelength_nm + " nm]";

				/* Gain within limits */
				if (edfaGain_dB < edfa_minGain_dB.getDouble() - 1E-3)
					st += "At " + initialPosition_km + "km: EDFA gain is " + edfaGain_dB + " dB. It should be between [" + edfa_minGain_dB + ", " + edfa_maxGain_dB + "] dB";
				if (edfaGain_dB > edfa_maxGain_dB.getDouble() + 1E-3)
					st += "At " + initialPosition_km + "km: EDFA gain is " + edfaGain_dB + " dB. It should be between [" + edfa_minGain_dB + ", " + edfa_maxGain_dB + "] dB";

				/* Input power within limits */
				if (pre_powerPerChannel_dBm < edfa_minInputPower_dBm.getDouble() - 1E-3)
					st += "At " + initialPosition_km + "km: Power at the EDFA input is (is one WDM channel) " + pre_powerPerChannel_dBm + " dBm. It should be between [" + edfa_minInputPower_dBm + ", " + edfa_maxInputPower_dBm + "] dBm";
				if (pre_powerPerChannel_dBm + numChannels_dB > edfa_maxInputPower_dBm.getDouble() + 1E-3)
					st += "At " + initialPosition_km + "km: Power at the EDFA input is (if all WDM channels are active) " + (pre_powerPerChannel_dBm + numChannels_dB) + " dBm. It should be between [" + edfa_minInputPower_dBm + ","
							+ edfa_maxInputPower_dBm + "] dBm";

				/* Output power within limits */
				if (post_powerPerChannel_dBm < edfa_minOutputPower_dBm.getDouble() - 1E-3)
					st += "At " + initialPosition_km + "km: Power at the EDFA output is (is one WDM channel) " + post_powerPerChannel_dBm + " dBm. It should be between [" + edfa_minOutputPower_dBm + ", " + edfa_maxOutputPower_dBm + "] dBm";
				if (post_powerPerChannel_dBm + numChannels_dB > edfa_maxOutputPower_dBm.getDouble() + 1E-3)
					st += "At " + initialPosition_km + "km: Power at the EDFA output is (if all WDM channels are active) " + (post_powerPerChannel_dBm + numChannels_dB) + " dBm. It should be between [" + edfa_minOutputPower_dBm + ", "
							+ edfa_maxOutputPower_dBm + "] dBm";
			} else if (name.equalsIgnoreCase("DCM"))
			{
				final double dcmCompensation_ps_per_nm = elementData;
				if ((dcmCompensation_ps_per_nm < dcm_channelDispersionMax_ps_per_nm.getDouble()) || (dcmCompensation_ps_per_nm > dcm_channelDispersionMin_ps_per_nm.getDouble()))
					st += "At " + initialPosition_km + "km: DCM compensation is " + dcmCompensation_ps_per_nm + " ps/nm. It should be between [" + dcm_channelDispersionMax_ps_per_nm + ", " + dcm_channelDispersionMin_ps_per_nm + "] ps/nm";
			} else
			{
				throw new RuntimeException("Unknown element type");
			}

			res.add(st);
		}

		return res;
	}

	private LinkedList<Triple<Double, String, Double>> getElementPositionsList(List<Link> seqLinks)
	{
		LinkedList<Triple<Double, String, Double>> res = new LinkedList<Triple<Double, String, Double>>();
		double currentDistanceFromRouteInit_km = 0;

		res.add(Triple.of(currentDistanceFromRouteInit_km, "OADM-ADD", (double) seqLinks.get(0).getOriginNode().getId()));
		for (Link e : seqLinks)
		{
			final double d_e = e.getLengthInKm();
			final String st_edfaPositions_km = e.getAttribute("edfaPositions_km") == null ? "" : e.getAttribute("edfaPositions_km");
			final String st_edfaGains_dB = e.getAttribute("edfaGains_dB") == null ? "" : e.getAttribute("edfaGains_dB");
			final String st_dcmPositions_km = e.getAttribute("dcmPositions_km") == null ? "" : e.getAttribute("dcmPositions_km");
			final String st_dcmCDCompensation_ps_per_nm = e.getAttribute("dcmCDCompensation_ps_per_nm") == null ? "" : e.getAttribute("dcmCDCompensation_ps_per_nm");
			final double[] edfaPositions_km = StringUtils.toDoubleArray(StringUtils.split(st_edfaPositions_km));
			final double[] edfaGains_dB = StringUtils.toDoubleArray(StringUtils.split(st_edfaGains_dB));
			final double[] dcmPositions_km = StringUtils.toDoubleArray(StringUtils.split(st_dcmPositions_km));
			final double[] dcmCDCompensation_ps_per_nm = StringUtils.toDoubleArray(StringUtils.split(st_dcmCDCompensation_ps_per_nm));
			if (edfaPositions_km.length != edfaGains_dB.length)
				throw new Net2PlanException("Link: " + e + ". Number of elements in edfaPositions_km is not equal to the number of elements in edfaGains_dB");
			if (dcmPositions_km.length != dcmCDCompensation_ps_per_nm.length)
				throw new Net2PlanException("Link: " + e + ". Number of elements in dcmPositions_km is not equal to the number of elements in dcmCDCompensation_ps_per_nm");

			for (double edfaPosition : edfaPositions_km)
				if ((edfaPosition < 0) || (edfaPosition > d_e))
					throw new Net2PlanException("Link: " + e + ". Wrong OA position: " + edfaPosition + ", link length = " + d_e);

			for (double dcmPosition : dcmPositions_km)
				if ((dcmPosition < 0) || (dcmPosition > d_e))
					throw new Net2PlanException("Link: " + e + ". Wrong DCM position: " + ", link length = " + d_e);

			/* Place in a sorted form the spans, dcms and EDFAS. If DCM and EDFA colocated, DCM goes first */
			double[] dcmAndEDFAPositions_km = DoubleUtils.concatenate(dcmPositions_km, edfaPositions_km);
			int[] sortedDCMAndEDFAPositionsIndexes = dcmAndEDFAPositions_km.length == 0 ? new int[0] : DoubleUtils.sortIndexes(dcmAndEDFAPositions_km, OrderingType.ASCENDING);
			double posKmLastElementThisLink_km = 0;
			for (int cont = 0; cont < sortedDCMAndEDFAPositionsIndexes.length; cont++)
			{
				final int indexInCommonArray = sortedDCMAndEDFAPositionsIndexes[cont];
				final boolean isDCM = (indexInCommonArray < dcmPositions_km.length);
				final double posFromLinkInit_km = dcmAndEDFAPositions_km[indexInCommonArray];
				final double previousSpanLength = (Math.abs(posFromLinkInit_km - posKmLastElementThisLink_km) < 1E-3) ? 0 : posFromLinkInit_km - posKmLastElementThisLink_km;

				if (previousSpanLength < 0)
					throw new RuntimeException("Bad");

				if (previousSpanLength > 0)
				{
					res.add(Triple.of(currentDistanceFromRouteInit_km, "SPAN", previousSpanLength));
					currentDistanceFromRouteInit_km += previousSpanLength;
					posKmLastElementThisLink_km += previousSpanLength;
				}

				if (isDCM)
					res.add(Triple.of(currentDistanceFromRouteInit_km, "DCM", dcmCDCompensation_ps_per_nm[indexInCommonArray]));
				else
					res.add(Triple.of(currentDistanceFromRouteInit_km, "EDFA", edfaGains_dB[indexInCommonArray - dcmPositions_km.length]));
			}

			/* Last span of the link before the OADM */
			final double lastSpanOfLink_km = (Math.abs(d_e - posKmLastElementThisLink_km) < 1E-3) ? 0 : d_e - posKmLastElementThisLink_km;

			if (lastSpanOfLink_km < 0)
				throw new RuntimeException("Bad");

			if (lastSpanOfLink_km > 0)
			{
				res.add(Triple.of(currentDistanceFromRouteInit_km, "SPAN", lastSpanOfLink_km));
				currentDistanceFromRouteInit_km += lastSpanOfLink_km;
				posKmLastElementThisLink_km += lastSpanOfLink_km;
			}

			/* OADM at the end of the link */
			final long endNodeLink = e.getDestinationNode().getId();
			final long lastLink = seqLinks.get(seqLinks.size() - 1).getId();
			if (e.getId() == lastLink)
				res.add(Triple.of(currentDistanceFromRouteInit_km, "OADM-DROP", (double) endNodeLink));
			else
				res.add(Triple.of(currentDistanceFromRouteInit_km, "OADM-EXPRESS", (double) endNodeLink));
		}

		/* Check current distance equals the sum of the traversed links */
		double sumLinks = 0;
		for (Link e : seqLinks)
			sumLinks += e.getLengthInKm();

		if (Math.abs(sumLinks - currentDistanceFromRouteInit_km) > 1E-3)
			throw new RuntimeException("Bad");
		return res;
	}

	private static double dB2Linear(double dB)
	{
		return Math.pow(10, dB / 10);
	}

	private static double linear2dB(double num)
	{
		return 10 * Math.log10(num);
	}

	private String printReport(Map<Link, LinkedList<Triple<Double, String, Double>>> elements_e, Map<Link, LinkedList<Pair<double[], double[]>>> impairments_e, Map<Link, LinkedList<String>> warnings_e,
			Map<Route, LinkedList<Triple<Double, String, Double>>> elements_r, Map<Route, LinkedList<Pair<double[], double[]>>> impairments_r, Map<Route, LinkedList<String>> warnings_r)
	{
		StringBuilder out = new StringBuilder();
		DecimalFormat df_2 = new DecimalFormat("###.##");

		out.append("<html><body>");
		out.append("<head><title>WDM line engineering in multilayer (lightpath based) networks</title></head>");
		out.append("<h1>WDM line engineering report for lighptath-based networks</h1>");

		out.append(
				"<p>This report shows line engineering information for WDM links in a multilayer optical network. The impairment calculations are inspired in the procedures described in the 2009 ITU-T WDM manual  \"Optical fibres, cabbles and systems\".</p>");
		out.append("<p>The report assumes that the WDM network follows the scheme:</p>");
		out.append("<ul>");
		out.append("<li>In the net2plan object, nodes are OADMs, links are fiber links, and routes are lightpaths: WDM channels optically switched at intermediate nodes. </li>");
		out.append("<li>Nodes are connected by unidirectional fiber links. Fiber link distance is given by the link length.");
		out.append("Other specifications are given by fiber_XXX input parameters. The fiber can be split into spans if optical amplifers (EDFAs) ");
		out.append("and/or dispersion compensating modules (DCMs) are placed along the fiber.</li>");
		out.append("<li>Optical line amplifiers (EDFAs) can be located in none, one or more positions in the");
		out.append("fiber link, separating them in different spans. EDFAs are supposed to operate in the ");
		out.append("automatic gain control mode. Thus, the gain is the same, whatever the number of input");
		out.append("WDM channels. EDFA positions (as distance in km from the link start to the EDFA location) ");
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

		out.append("<h2>PER LINK INFORMATION SUMMARY - Signal metrics at the input of end OADM</h2>");
		out.append("<table border='1'>");
		out.append(
				"<tr><th><b>Link #</b></th><th><b>Length (km)</b></th><th><b># EDFAs</b></th><th><b># DCMs</b></th><th><b>Chromatic Dispersion (ps/nm)</b></th><th><b>OSNR (dB)</b></th><th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th><th><b>Warnings</b></th></tr>");
		for (Link e : netPlan.getLinks())
		{
			final double d_e = e.getLengthInKm();
			final String st_a_e = e.getOriginNode().getName();
			final String st_b_e = e.getDestinationNode().getName();
			LinkedList<Triple<Double, String, Double>> el = elements_e.get(e);
			LinkedList<Pair<double[], double[]>> imp = impairments_e.get(e);
			LinkedList<String> w = warnings_e.get(e);

			int numEDFAs = 0;
			for (Triple<Double, String, Double> t : el)
				if (t.getSecond().equalsIgnoreCase("EDFA"))
					numEDFAs++;

			int numDCMs = 0;
			for (Triple<Double, String, Double> t : el)
				if (t.getSecond().equalsIgnoreCase("DCM"))
					numDCMs++;

			final double[] impInfoInputOADM = imp.getLast().getFirst();
			StringBuilder warnings = new StringBuilder();
			for (String s : w)
				warnings.append(s);

			out.append("<tr><td>").append(e).append(" (").append(st_a_e).append(" --> ").append(st_b_e).append(") </td><td>").append(df_2.format(d_e)).append("</td><td>").append(numEDFAs).append("</td><td>").append(numDCMs).append("</td><td>")
					.append(df_2.format(impInfoInputOADM[1])).append("</td><td>").append(df_2.format(linear2dB(impInfoInputOADM[3]))).append("</td><td>").append(df_2.format(impInfoInputOADM[0])).append("</td><td>")
					.append(df_2.format(Math.sqrt(impInfoInputOADM[2]))).append("</td><td>").append(warnings).append("</td></tr>");
		}
		out.append("</table>");

		out.append("<h2>PER ROUTE INFORMATION SUMMARY - Signal metrics at the transponder</h2>");
		out.append("<table border='1'>");
		out.append(
				"<tr><th><b>Route #</b></th><th><b>Length (km)</b></th><th><b># EDFAs</b></th><th><b># DCMs</b></th><th><b>Chromatic Dispersion (ps/nm)</b></th><th><b>OSNR (dB)</b></th><th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th><th><b>Warnings</b></th></tr>");
		for (Route r : netPlan.getRoutes())
		{
			final double d_r = r.getLengthInKm();
			final String st_a_r = r.getIngressNode().getName();
			final String st_b_r = r.getEgressNode().getName();
			LinkedList<Triple<Double, String, Double>> el = elements_r.get(r);
			LinkedList<Pair<double[], double[]>> imp = impairments_r.get(r);
			LinkedList<String> w = warnings_r.get(r);

			int numEDFAs = 0;
			for (Triple<Double, String, Double> t : el)
				if (t.getSecond().equalsIgnoreCase("EDFA"))
					numEDFAs++;

			int numDCMs = 0;
			for (Triple<Double, String, Double> t : el)
				if (t.getSecond().equalsIgnoreCase("DCM"))
					numDCMs++;

			final double[] impInfoInputOADM = imp.getLast().getFirst();
			StringBuilder warnings = new StringBuilder();
			for (String s : w)
				warnings.append(s);

			out.append("<tr><td>").append(r).append(" (").append(st_a_r).append(" --> ").append(st_b_r).append(") </td><td>").append(df_2.format(d_r)).append("</td><td>").append(numEDFAs).append("</td><td>").append(numDCMs).append("</td><td>")
					.append(df_2.format(impInfoInputOADM[1])).append("</td><td>").append(df_2.format(linear2dB(impInfoInputOADM[3]))).append("</td><td>").append(df_2.format(impInfoInputOADM[0])).append("</td><td>")
					.append(df_2.format(Math.sqrt(impInfoInputOADM[2]))).append("</td><td>").append(warnings.toString()).append("</td>" + "</tr>");
		}
		out.append("</table>");

		out.append("<h2>PER-LINK DETAILED INFORMATION </h2>");
		out.append("<p>Number of links: ").append(netPlan.getNumberOfLinks()).append("</p>");

		for (Link e : netPlan.getLinks())
		{
			final double d_e = e.getLengthInKm();
			final String st_a_e = e.getOriginNode().getName();
			final String st_b_e = e.getDestinationNode().getName();
			LinkedList<Triple<Double, String, Double>> el = elements_e.get(e);
			LinkedList<Pair<double[], double[]>> imp = impairments_e.get(e);
			LinkedList<String> w = warnings_e.get(e);
			final String st_edfaPositions_km = e.getAttribute("edfaPositions_km") == null ? "" : e.getAttribute("edfaPositions_km");
			final String st_edfaGains_dB = e.getAttribute("edfaGains_dB") == null ? "" : e.getAttribute("edfaGains_dB");
			final String st_dcmPositions_km = e.getAttribute("dcmPositions_km") == null ? "" : e.getAttribute("dcmPositions_km");

			out.append("<h3>LINK # ").append(e).append(" (").append(st_a_e).append(" --> ").append(st_b_e).append(")</h3>");
			out.append("<table border=\"1\">");
			out.append("<caption>Link input information</caption>");
			out.append("<tr><td>Link length (km)</td><td>").append(d_e).append("</td></tr>");
			out.append("<tr><td>EDFA positions (km)</td><td>").append(st_edfaPositions_km).append("</td></tr>");
			out.append("<tr><td>EDFA gains (dB)</td><td>").append(st_edfaGains_dB).append("</td></tr>");
			out.append("<tr><td>DCM positions (km)</td><td>").append(st_dcmPositions_km).append("</td></tr>");
			out.append("</table>");

			out.append("<table border=\"1\">");
			out.append("<caption>Signal metrics evolution</caption>");
			out.append(
					"<tr><th><b>Position (km)</b></th><th><b>Position (description)</b></th><th><b>Chromatic Dispersion (ps/nm)</b></th><th><b>OSNR (dB)</b></th><th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th><th><b>Warnings</b></th></tr>");
			Iterator<Triple<Double, String, Double>> it_el = el.iterator();
			Iterator<Pair<double[], double[]>> it_imp = imp.iterator();
			Iterator<String> it_w = w.iterator();
			while (it_el.hasNext())
			{
				final Triple<Double, String, Double> this_el = it_el.next();
				final Pair<double[], double[]> this_imp = it_imp.next();
				final String this_warnings = it_w.next();

				final double pos_km = this_el.getFirst();
				String elementType = this_el.getSecond();
				final double elementAuxData = this_el.getThird();
				final double[] prevInfo = this_imp.getFirst();

				if (elementType.equalsIgnoreCase("EDFA"))
					elementType += " (gain " + elementAuxData + " dB)";
				else if (elementType.equalsIgnoreCase("SPAN"))
					elementType += " (" + elementAuxData + " km)";
				else if (elementType.equalsIgnoreCase("DCM"))
					elementType += " (comp " + elementAuxData + " ps/nm)";

				out.append("<tr><td>").append(df_2.format(pos_km)).append("</td><td>" + "Input of ").append(elementType).append("</td><td>").append(df_2.format(prevInfo[1])).append("</td><td>").append(df_2.format(linear2dB(prevInfo[3])))
						.append("</td><td>").append(df_2.format(prevInfo[0])).append("</td><td>").append(df_2.format(Math.sqrt(prevInfo[2]))).append("</td><td>").append(this_warnings).append("</td>" + "</tr>");

			}
			out.append("</table>");
		}

		out.append("<h2>PER-LIGHTPATH DETAILED INFORMATION</h2>");
		out.append("<p>Number of lightpaths: ").append(netPlan.getNumberOfRoutes()).append("</p>");

		for (Route r : netPlan.getRoutes())
		{
			final double d_r = r.getLengthInKm();
			final String st_a_r = r.getIngressNode().getName();
			final String st_b_r = r.getEgressNode().getName();
			LinkedList<Triple<Double, String, Double>> el = elements_r.get(r);
			LinkedList<Pair<double[], double[]>> imp = impairments_r.get(r);
			LinkedList<String> w = warnings_r.get(r);

			out.append("<h3>ROUTE # ").append(r).append(" (").append(st_a_r).append(" --> ").append(st_b_r).append("), Length: ").append(d_r).append(" km</h3>");
			out.append("<table border=\"1\">");
			out.append("<caption>Signal metrics evolution</caption>");
			out.append(
					"<tr><th><b>Position (km)</b></th><th><b>Position (description)</b></th><th><b>Chromatic Dispersion (ps/nm)</b></th><th><b>OSNR (dB)</b></th><th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th><th><b>Warnings</b></th></tr>");
			Iterator<Triple<Double, String, Double>> it_el = el.iterator();
			Iterator<Pair<double[], double[]>> it_imp = imp.iterator();
			Iterator<String> it_w = w.iterator();
			while (it_el.hasNext())
			{
				final Triple<Double, String, Double> this_el = it_el.next();
				final Pair<double[], double[]> this_imp = it_imp.next();
				final String this_warnings = it_w.next();

				final double pos_km = this_el.getFirst();
				String elementType = this_el.getSecond();
				final double elementAuxData = this_el.getThird();
				final double[] prevInfo = this_imp.getFirst();
				if (elementType.equalsIgnoreCase("EDFA"))
					elementType += " (gain " + elementAuxData + " dB)";
				else if (elementType.equalsIgnoreCase("SPAN"))
					elementType += " (" + elementAuxData + " km)";
				else if (elementType.equalsIgnoreCase("DCM"))
					elementType += " (comp " + elementAuxData + " ps/nm)";

				out.append("<tr><td>").append(df_2.format(pos_km)).append("</td><td>" + "Input of ").append(elementType).append("</td><td>").append(df_2.format(prevInfo[1])).append("</td><td>").append(df_2.format(linear2dB(prevInfo[3])))
						.append("</td><td>").append(df_2.format(prevInfo[0])).append("</td><td>").append(df_2.format(Math.sqrt(prevInfo[2]))).append("</td><td>").append(this_warnings).append("</td>" + "</tr>");

			}
			final Triple<Double, String, Double> this_el = el.getLast();
			final Pair<double[], double[]> this_imp = imp.getLast();
			final String this_warnings = w.getLast();
			final double pos_km = this_el.getFirst();
			final double[] postInfo = this_imp.getSecond();
			out.append("<tr><td>").append(df_2.format(pos_km)).append("</td><td>" + "Receiver" + "</td><td>").append(df_2.format(postInfo[1])).append("</td><td>").append(df_2.format(linear2dB(postInfo[3]))).append("</td><td>")
					.append(df_2.format(postInfo[0])).append("</td><td>").append(df_2.format(Math.sqrt(postInfo[2]))).append("</td><td>").append(this_warnings).append("</td>" + "</tr>");

			out.append("</table>");
		}

		out.append("</body></html>");
		return out.toString();
	}

	private double updateOSNRAfterEDFA(double currentOSNR_linear, double noiseFactor_dB, double noiseFactorReferenceBandwidth_nm, double inputPowerPerChannel_dBm)
	{
		final double edfa_NF_linear = dB2Linear(noiseFactor_dB);
		final double highestFrequencyChannel_Hz = constant_c / (channels_minChannelLambda_nm.getDouble() * 1e-9);
		final double referenceBandwidthAtHighestFrequency_Hz = -highestFrequencyChannel_Hz + constant_c / ((channels_minChannelLambda_nm.getDouble() - noiseFactorReferenceBandwidth_nm) * 1e-9);
		final double inputPower_linear = dB2Linear(inputPowerPerChannel_dBm) * 1E-3;
		final double thisEDFAAddedNoise_linear = edfa_NF_linear * constant_h * highestFrequencyChannel_Hz * referenceBandwidthAtHighestFrequency_Hz;
		final double addedOSNRThisOA_linear = inputPower_linear / thisEDFAAddedNoise_linear;
		final double new_OSNR_linear = (currentOSNR_linear == Double.MAX_VALUE) ? addedOSNRThisOA_linear : 1 / (1 / currentOSNR_linear + 1 / addedOSNRThisOA_linear);
		return new_OSNR_linear;
	}
}
