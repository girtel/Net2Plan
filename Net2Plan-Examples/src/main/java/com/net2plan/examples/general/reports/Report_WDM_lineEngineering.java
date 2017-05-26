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
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import java.text.DecimalFormat;
import java.util.*;

/**
 * <p>
 * This report shows line engineering information for WDM links in a multilayer
 * optical network. The impairment calculations are inspired in the procedures
 * described in the 2009 ITU-T WDM manual "Optical fibres, cabbles and
 * systems".</p>
 *
 * <p>
 * The report assumes that the WDM network follows the scheme:</p>
 * <ul>
 * <li>In the net2plan object, nodes are OADMs, links are fiber links, and
 * routes are lightpaths: WDM channels optically switched at intermediate nodes.
 * </li>
 * <li>Nodes are connected by unidirectional fiber links. Fiber link distance is
 * given by the link length. Other specifications are given by fiber_XXX input
 * parameters. The fiber can be split into spans if optical amplifers (EDFAs)
 * and/or dispersion compensating modules (DCMs) are placed along the
 * fiber.</li>
 * <li>Optical line amplifiers (EDFAs) can be located in none, one or more
 * positions in the fiber link, separating them in different spans. EDFAs are
 * supposed to operate in the automatic gain control mode. Thus, the gain is the
 * same, whatever the number of input WDM channels. EDFA positions (as distance
 * in km from the link start to the EDFA location) and EDFA gains (assumed in
 * dB) are read from the "edfaPositions_km" and "edfaGains_dB" attributes of the
 * links. The format of both attributes are the same: a string of numbers
 * separated by spaces. The <i>i</i>-th number corresponding to the
 * position/gain of the
 * <i>i</i>-th EDFA. If the attributes do not exist, it is assumed that no EDFAs
 * are placed in this link. EDFA specifications are given by "edfa_XXX"
 * parameters</li>
 * <li>Dispersion compensating modules (DCMs) can be located in none, one or
 * more positions in the fiber link, separating them in different spans. If a
 * DCM and a EDFA have the same location, it is assumed that the DCM is placed
 * first, to reduce the non-linear effects. DCM positions (as distance in km
 * from the link start to the DCM location) are read from the "dcmPositions_km"
 * attribute of the link, and the same format as with "edfaPositions_km"
 * attribute is expected. If the attribute does not exist, it is assumed that no
 * DCMs are placed in this link. DCM specifications are given by "dcm_XXX"
 * parameters</li>
 * <li>Fiber links start and end in OADM modules, that permit adding, dropping
 * and optically switch individual WDM channels. OADMs have a pre-amplifier
 * (traversed by drop and express channels) and a boost amplifier (traversed by
 * add and express channels). They are supposed to equalize the channel power at
 * their outputs, to a fixed value (added and express channels will thus have
 * the same power in the fibers). Also, OADMs attenuate appropriately the
 * optical signal coming from the pre-amplifier, in the drop channels, so that
 * they fall within the receiver sensitivity range. OADM noise figures for add,
 * drop and express channels are given as input parameters. PMD values for add,
 * drop and express channels are computed assumming that: (i) add channel
 * traverse a multiplexer and the booster, (ii) drop channels travese the
 * pre-amplifier and a demultiplexer, (iii) express channels traverse the two
 * amplifiers. The required parameters are provided in oadm_XXX parameters.
 * </li>
 * <li>Each channel ends in a receiver, with specifications given by "tp_XXX"
 * parameters.</li>
 * </ul>
 *
 * <p>
 * The basic checks performed are:</p>
 * <ul>
 * <li>For each link, signal power levels are within operating ranges at the
 * oadm/edfas/dcms, both when the link has one single active channel, or when
 * all the "channels_maxNumChannels" are active</li>
 * <li>For each link, chromatic dispersion is within the limits set per
 * link</li>
 * <li>For each route (lightpath), chromatic dispersion is within the limits of
 * the receiver.</li>
 * <li>For each route (lightpath), OSNR (Optical Signal to Noise Ration) is
 * within the operating range at the receiver. A set of margins are considered
 * to account to several not directly considered impairments. </li>
 * <li>For each route (lightpath), PMD (Polarization mode dispersion) is within
 * the operating range at the receiver</li>
 * </ul>
 * @net2plan.keywords WDM, Multilayer
 * @author Pablo Pavon-Marino
 * @version 1.1, May 2015
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
	private double channels_minChannelLambda_nm;
	private double channels_channelSpacing_GHz;
	private int channels_maxNumChannels;
	private double channels_maxChannelLambda_nm;

	/* Fiber specifications  */
	private double fiber_attenuation_dB_per_km;
	private double fiber_worseChromaticDispersion_ps_per_nm_per_km;
	private double fiber_PMD_ps_per_sqroot_km;

	/* Transponder specifications */
	private double tp_maxChromaticDispersionTolerance_ps_per_nm;
	private double tp_minOSNR_dB;
	private double tp_minWavelength_nm;
	private double tp_maxWavelength_nm;
	private double tp_pmdTolerance_ps;
	private double tp_inputPowerSensitivityMin_dBm;
	private double tp_inputPowerSensitivityMax_dBm;

	/* OADM specs */
	private double oadm_perChannelOutputPower_dBm;
	private double oadm_perChannelMinInputPower_dBm;
	private double oadm_perChannelMaxInputPower_dBm;
	private double oadm_muxDemuxPMD_ps;
	private double oadm_preAmplifierPMD_ps;
	private double oadm_boosterPMD_ps;
	private double oadm_addChannelNoiseFactor_dB;
	private double oadm_dropChannelNoiseFactor_dB;
	private double oadm_expressChannelNoiseFactor_dB;
	private double oadm_noiseFactorReferenceBandwidth_nm;
	private double oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm;

	/* Optical line amplifier specifications */
	private double edfa_minWavelength_nm;
	private double edfa_maxWavelength_nm;
	private double edfa_minInputPower_dBm;
	private double edfa_maxInputPower_dBm;
	private double edfa_minOutputPower_dBm;
	private double edfa_maxOutputPower_dBm;
	private double edfa_minGain_dB;
	private double edfa_maxGain_dB;
	private double edfa_PMD_ps;
	private double edfa_noiseFactorMaximumGain_dB;
	private double edfa_noiseFactorMinimumGain_dB;
	private double edfa_noiseFactorReferenceBandwidth_nm;

	/* Dispersion compensation modules specifications */
	private double dcm_channelDispersionMax_ps_per_nm;
	private double dcm_channelDispersionMin_ps_per_nm;
	private double dcm_PMD_ps;
	private double dcm_insertionLoss_dB;

	/* OSNR penalties */
	private double osnrPenalty_CD_dB;
	private double osnrPenalty_nonLinear_dB;
	private double osnrPenalty_PMD_dB;
	private double osnrPenalty_PDL_dB;
	private double osnrPenalty_transmitterChirp_dB;
	private double osnrPenalty_OADMCrosstalk_dB;
	private double osnrPenalty_unassignedMargin_dB;
	private double osnrPenalty_SUM_dB;

	@Override
	public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
	{
		/* Input parameters */
		this.netPlan = netPlan;
		this.reportParameters = reportParameters;

		/* Usable wavelengths */
		channels_minChannelLambda_nm = Double.parseDouble(reportParameters.get("channels_minChannelLambda_nm"));
		channels_channelSpacing_GHz = Double.parseDouble(reportParameters.get("channels_channelSpacing_GHz"));
		channels_maxNumChannels = Integer.parseInt(reportParameters.get("channels_maxNumChannels"));
		channels_maxChannelLambda_nm = constant_c / ((constant_c / channels_minChannelLambda_nm) - (channels_maxNumChannels - 1) * channels_channelSpacing_GHz);

		/* Fiber specifications  */
		fiber_attenuation_dB_per_km = Double.parseDouble(reportParameters.get("fiber_attenuation_dB_per_km"));
		fiber_worseChromaticDispersion_ps_per_nm_per_km = Double.parseDouble(reportParameters.get("fiber_worseChromaticDispersion_ps_per_nm_per_km"));
		fiber_PMD_ps_per_sqroot_km = Double.parseDouble(reportParameters.get("fiber_PMD_ps_per_sqroot_km"));

		/* Transponder specifications */
		tp_maxChromaticDispersionTolerance_ps_per_nm = Double.parseDouble(reportParameters.get("tp_maxChromaticDispersionTolerance_ps_per_nm"));
		tp_minOSNR_dB = Double.parseDouble(reportParameters.get("tp_minOSNR_dB"));
		tp_minWavelength_nm = Double.parseDouble(reportParameters.get("tp_minWavelength_nm"));
		tp_maxWavelength_nm = Double.parseDouble(reportParameters.get("tp_maxWavelength_nm"));
		tp_pmdTolerance_ps = Double.parseDouble(reportParameters.get("tp_pmdTolerance_ps"));
		tp_inputPowerSensitivityMin_dBm = Double.parseDouble(reportParameters.get("tp_inputPowerSensitivityMin_dBm"));
		tp_inputPowerSensitivityMax_dBm = Double.parseDouble(reportParameters.get("tp_inputPowerSensitivityMax_dBm"));

		/* OADM specs */
		oadm_perChannelOutputPower_dBm = Double.parseDouble(reportParameters.get("oadm_perChannelOutputPower_dBm"));
		oadm_perChannelMinInputPower_dBm = Double.parseDouble(reportParameters.get("oadm_perChannelMinInputPower_dBm"));
		oadm_perChannelMaxInputPower_dBm = Double.parseDouble(reportParameters.get("oadm_perChannelMaxInputPower_dBm"));
		oadm_muxDemuxPMD_ps = Double.parseDouble(reportParameters.get("oadm_muxDemuxPMD_ps"));
		oadm_preAmplifierPMD_ps = Double.parseDouble(reportParameters.get("oadm_preAmplifierPMD_ps"));
		oadm_boosterPMD_ps = Double.parseDouble(reportParameters.get("oadm_boosterPMD_ps"));
		oadm_addChannelNoiseFactor_dB = Double.parseDouble(reportParameters.get("oadm_addChannelNoiseFactor_dB"));
		oadm_dropChannelNoiseFactor_dB = Double.parseDouble(reportParameters.get("oadm_dropChannelNoiseFactor_dB"));
		oadm_expressChannelNoiseFactor_dB = Double.parseDouble(reportParameters.get("oadm_expressChannelNoiseFactor_dB"));
		oadm_noiseFactorReferenceBandwidth_nm = Double.parseDouble(reportParameters.get("oadm_noiseFactorReferenceBandwidth_nm"));
		oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm = Double.parseDouble(reportParameters.get("oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm"));

		/* Optical line amplifier specifications */
		edfa_minWavelength_nm = Double.parseDouble(reportParameters.get("edfa_minWavelength_nm"));
		edfa_maxWavelength_nm = Double.parseDouble(reportParameters.get("edfa_maxWavelength_nm"));
		edfa_minInputPower_dBm = Double.parseDouble(reportParameters.get("edfa_minInputPower_dBm"));
		edfa_maxInputPower_dBm = Double.parseDouble(reportParameters.get("edfa_maxInputPower_dBm"));
		edfa_minOutputPower_dBm = Double.parseDouble(reportParameters.get("edfa_minOutputPower_dBm"));
		edfa_maxOutputPower_dBm = Double.parseDouble(reportParameters.get("edfa_maxOutputPower_dBm"));
		edfa_minGain_dB = Double.parseDouble(reportParameters.get("edfa_minGain_dB"));
		edfa_maxGain_dB = Double.parseDouble(reportParameters.get("edfa_maxGain_dB"));
		edfa_PMD_ps = Double.parseDouble(reportParameters.get("edfa_PMD_ps"));
		edfa_noiseFactorMaximumGain_dB = Double.parseDouble(reportParameters.get("edfa_noiseFactorMaximumGain_dB"));
		edfa_noiseFactorMinimumGain_dB = Double.parseDouble(reportParameters.get("edfa_noiseFactorMinimumGain_dB"));
		edfa_noiseFactorReferenceBandwidth_nm = Double.parseDouble(reportParameters.get("edfa_noiseFactorReferenceBandwidth_nm"));

		/* Dispersion compensation modules specifications */
		dcm_channelDispersionMax_ps_per_nm = Double.parseDouble(reportParameters.get("dcm_channelDispersionMax_ps_per_nm"));
		dcm_channelDispersionMin_ps_per_nm = Double.parseDouble(reportParameters.get("dcm_channelDispersionMin_ps_per_nm"));
		dcm_PMD_ps = Double.parseDouble(reportParameters.get("dcm_PMD_ps"));
		dcm_insertionLoss_dB = Double.parseDouble(reportParameters.get("dcm_insertionLoss_dB"));

		/* OSNR penalties */
		osnrPenalty_CD_dB = Double.parseDouble(reportParameters.get("osnrPenalty_CD_dB"));
		osnrPenalty_nonLinear_dB = Double.parseDouble(reportParameters.get("osnrPenalty_nonLinear_dB"));
		osnrPenalty_PMD_dB = Double.parseDouble(reportParameters.get("osnrPenalty_PMD_dB"));
		osnrPenalty_PDL_dB = Double.parseDouble(reportParameters.get("osnrPenalty_PDL_dB"));
		osnrPenalty_transmitterChirp_dB = Double.parseDouble(reportParameters.get("osnrPenalty_transmitterChirp_dB"));
		osnrPenalty_OADMCrosstalk_dB = Double.parseDouble(reportParameters.get("osnrPenalty_OADMCrosstalk_dB"));
		osnrPenalty_unassignedMargin_dB = Double.parseDouble(reportParameters.get("osnrPenalty_unassignedMargin_dB"));
		osnrPenalty_SUM_dB = osnrPenalty_CD_dB + osnrPenalty_nonLinear_dB + osnrPenalty_PMD_dB + osnrPenalty_PDL_dB + osnrPenalty_transmitterChirp_dB + osnrPenalty_OADMCrosstalk_dB + osnrPenalty_unassignedMargin_dB;

		Map<Link, LinkedList<Triple<Double, String, Double>>> elements_e = new LinkedHashMap<Link, LinkedList<Triple<Double, String, Double>>>();
		Map<Link, LinkedList<Pair<double[], double[]>>> impairments_e = new LinkedHashMap<Link, LinkedList<Pair<double[], double[]>>>();
		Map<Link, LinkedList<String>> warnings_e = new LinkedHashMap<Link, LinkedList<String>>();

		for (Link link : netPlan.getLinks())
		{
			final List<Link> seqLinks = new LinkedList<Link>(); seqLinks.add (link);
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
		List<Triple<String, String, String>> parameters = new LinkedList<Triple<String, String, String>>();

		/* Usable wavelengths */
		parameters.add(Triple.of("channels_minChannelLambda_nm", "1530.33", "Channel minimum wavelength in nm"));
		parameters.add(Triple.of("channels_channelSpacing_GHz", "100", "Channel spacing in GHz"));
		parameters.add(Triple.of("channels_maxNumChannels", "16", "Maximum number of WDM channels that will be used"));

		/* Fiber specifications  */
		parameters.add(Triple.of("fiber_attenuation_dB_per_km", "0.25", "Fiber attenuation in dB/km"));
		parameters.add(Triple.of("fiber_worseChromaticDispersion_ps_per_nm_per_km", "6", "Chromatic dispersion of the fiber in ps/nm/km"));
		parameters.add(Triple.of("fiber_PMD_ps_per_sqroot_km", "0.4", "Polarization mode dispersion per km^0.5 of fiber (PMD_Q link factor)"));

		/* Transponder specifications */
		parameters.add(Triple.of("tp_maxChromaticDispersionTolerance_ps_per_nm", "800", "Maximum chromatic dispersion tolerance in ps/nm at the receiver"));
		parameters.add(Triple.of("tp_minOSNR_dB", "7", "Minimum OSNR needed at the receiver"));
		parameters.add(Triple.of("tp_inputPowerSensitivityMin_dBm", "-20", "Minimum input power at the receiver in dBm"));
		parameters.add(Triple.of("tp_inputPowerSensitivityMax_dBm", "-8", "Maximum input power at the receiver in dBm"));
		parameters.add(Triple.of("tp_minWavelength_nm", "1529.55", "Minimum wavelength usable by the transponder"));
		parameters.add(Triple.of("tp_maxWavelength_nm", "1561.84", "Maximum wavelength usable by the transponder"));
		parameters.add(Triple.of("tp_pmdTolerance_ps", "10", "Maximum tolarance of polarizarion mode dispersion (mean of differential group delay) in ps at the receiver"));

		/* Optical amplifier specifications */
		parameters.add(Triple.of("edfa_minWavelength_nm", "1530", "Minimum wavelength usable by the EDFA"));
		parameters.add(Triple.of("edfa_maxWavelength_nm", "1563", "Maximum wavelength usable by the EDFA"));
		parameters.add(Triple.of("edfa_minInputPower_dBm", "-29", "Minimum input power at the EDFA"));
		parameters.add(Triple.of("edfa_maxInputPower_dBm", "2", "Maximum input power at the EDFA"));
		parameters.add(Triple.of("edfa_minOutputPower_dBm", "-6", "Minimum output power at the EDFA"));
		parameters.add(Triple.of("edfa_maxOutputPower_dBm", "19", "Maximum output power at the EDFA"));
		parameters.add(Triple.of("edfa_minGain_dB", "17", "Minimum gain at the EDFA"));
		parameters.add(Triple.of("edfa_maxGain_dB", "23", "Maximum gain at the EDFA"));
		parameters.add(Triple.of("edfa_PMD_ps", "0.5", "Polarization mode dispersion in ps added by the EDFA"));
		parameters.add(Triple.of("edfa_noiseFactorMaximumGain_dB", "5", "Noise factor at the EDFA when the gain is in its upper limit (linear interpolation is used to estimate the noise figura at other gains)"));
		parameters.add(Triple.of("edfa_noiseFactorMinimumGain_dB", "5", "Noise factor at the EDFA when the gain is in its lower limit (linear interpolation is used to estimate the noise figura at other gains)"));
		parameters.add(Triple.of("edfa_noiseFactorReferenceBandwidth_nm", "0.5", "Reference bandwidth that measures the noise factor at the EDFA"));

		/* OADM specs */
		parameters.add(Triple.of("oadm_perChannelOutputPower_dBm", "6", "Output power per channel at the OADM in dBm"));
		parameters.add(Triple.of("oadm_perChannelMinInputPower_dBm", "-19", "Minimum power needed at the OADM input"));
		parameters.add(Triple.of("oadm_perChannelMaxInputPower_dBm", "1000", "Maximum power admitted at the OADM input"));
		parameters.add(Triple.of("oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm", "150", "Maximum chromatic dispersion tolerance in ps/nm at te end of each link (absolute value)"));
		parameters.add(Triple.of("oadm_muxDemuxPMD_ps", "0.5", "PMD of the mux/demux inside the OADMs. Does not affect express lightpaths"));
		parameters.add(Triple.of("oadm_preAmplifierPMD_ps", "0.5", "PMD off OADM preamplifier"));
		parameters.add(Triple.of("oadm_boosterPMD_ps", "0.5", "PMD off OADM booster amplifier"));
		parameters.add(Triple.of("oadm_addChannelNoiseFactor_dB", "6", "Noise factor observed by add channels"));
		parameters.add(Triple.of("oadm_dropChannelNoiseFactor_dB", "6", "Noise factor observed by drop channels"));
		parameters.add(Triple.of("oadm_expressChannelNoiseFactor_dB", "10", "Noise factor observed by express channels"));
		parameters.add(Triple.of("oadm_noiseFactorReferenceBandwidth_nm", "0.5", "Reference bandwidth that measures the noise factor at the OADM amplifiers"));

		/* Dispersion compensation modules specifications */
		parameters.add(Triple.of("dcm_channelDispersionMax_ps_per_nm", "-827", "Max (in absolute value) dispersion compensation a DCM can place (ps/nm). It is assumed to be the same for all wavelengths"));
		parameters.add(Triple.of("dcm_channelDispersionMin_ps_per_nm", "-276", "Min (in absolute value) dispersion compensation a DCM can place (ps/nm). It is assumed to be the same for all wavelengths"));
		parameters.add(Triple.of("dcm_PMD_ps", "0.7", "Polarization mode dispersion in ps added by the DCM"));
		parameters.add(Triple.of("dcm_insertionLoss_dB", "3.5", "Maximum insertion loss added by the DCM"));

		/* OSNR penalties */
		parameters.add(Triple.of("osnrPenalty_CD_dB", "1", "OSNR penalty caused by residual chromatic dispersion (assumed within limits)"));
		parameters.add(Triple.of("osnrPenalty_nonLinear_dB", "2", "OSNR penalty caused by the non-linear effects SPM, XPM, FWM and Brillouin / Raman scattering"));
		parameters.add(Triple.of("osnrPenalty_PMD_dB", "0.5", "OSNR penalty caused by the polarization mode dispersion (assumed within limits)"));
		parameters.add(Triple.of("osnrPenalty_PDL_dB", "0.3", "OSNR penalty caused by polarization dispersion losses"));
		parameters.add(Triple.of("osnrPenalty_transmitterChirp_dB", "0.5", "OSNR penalty caused by transmitter chirp "));
		parameters.add(Triple.of("osnrPenalty_OADMCrosstalk_dB", "0.8", "OSNR penalty caused by the crosstalk at the OADMs"));
		parameters.add(Triple.of("osnrPenalty_unassignedMargin_dB", "3", "OSNR penalty caused by not assigned margins (e.g. random effects, aging, ...)"));

		return parameters;
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
		double current_powerPerChannel_dBm = oadm_perChannelOutputPower_dBm;
		double current_CD_ps_per_nm = 0;
		double current_PMDSquared_ps2 = 0;
		double current_OSNR_linear = Double.MAX_VALUE; /* no noise */

		if (!elementPositions.getFirst().getSecond().equalsIgnoreCase("OADM-ADD")) throw new RuntimeException("The route should start in a OADM-ADD element");
		if (!elementPositions.getLast().getSecond().equalsIgnoreCase("OADM-DROP")) throw new RuntimeException("The route should end in a OADM-DROP element");

		for (Triple<Double, String, Double> element : elementPositions)
		{
			final String name = element.getSecond();
			final double elementData = element.getThird();

			double[] prevState = DoubleUtils.arrayOf(current_powerPerChannel_dBm, current_CD_ps_per_nm, current_PMDSquared_ps2, current_OSNR_linear);
			if (name.equalsIgnoreCase("OADM-ADD"))
			{
				current_OSNR_linear = updateOSNRAfterEDFA(Double.MAX_VALUE, oadm_addChannelNoiseFactor_dB, oadm_noiseFactorReferenceBandwidth_nm, current_powerPerChannel_dBm);
				current_powerPerChannel_dBm = oadm_perChannelOutputPower_dBm;
				current_CD_ps_per_nm = 0;
				current_PMDSquared_ps2 = Math.pow(oadm_muxDemuxPMD_ps, 2) + Math.pow(oadm_boosterPMD_ps, 2);

			}
			else if (name.equalsIgnoreCase("OADM-EXPRESS"))
			{
				current_OSNR_linear = updateOSNRAfterEDFA(current_OSNR_linear, oadm_expressChannelNoiseFactor_dB, oadm_noiseFactorReferenceBandwidth_nm, current_powerPerChannel_dBm);
				current_powerPerChannel_dBm = oadm_perChannelOutputPower_dBm;
				current_PMDSquared_ps2 += Math.pow(oadm_preAmplifierPMD_ps, 2) + Math.pow(oadm_boosterPMD_ps, 2);
			}
			else if (name.equalsIgnoreCase("OADM-DROP"))
			{
				current_OSNR_linear = updateOSNRAfterEDFA(current_OSNR_linear, oadm_dropChannelNoiseFactor_dB, oadm_noiseFactorReferenceBandwidth_nm, current_powerPerChannel_dBm);
				current_powerPerChannel_dBm = (tp_inputPowerSensitivityMin_dBm + tp_inputPowerSensitivityMax_dBm) / 2;
				current_PMDSquared_ps2 += Math.pow(oadm_preAmplifierPMD_ps, 2);
			}
			else if (name.equalsIgnoreCase("SPAN"))
			{
				final double spanLength_km = elementData;
				current_powerPerChannel_dBm -= fiber_attenuation_dB_per_km * spanLength_km;
				current_CD_ps_per_nm += fiber_worseChromaticDispersion_ps_per_nm_per_km * spanLength_km;
				current_PMDSquared_ps2 += spanLength_km * Math.pow(fiber_PMD_ps_per_sqroot_km, 2);
			}
			else if (name.equalsIgnoreCase("EDFA"))
			{
				final double edfaGain_dB = elementData;
				final double edfa_noiseFactorThisGain_dB = edfa_noiseFactorMinimumGain_dB + (edfaGain_dB - edfa_minGain_dB) * (edfa_noiseFactorMaximumGain_dB - edfa_noiseFactorMinimumGain_dB) / (edfa_maxGain_dB - edfa_minGain_dB);
				if ((edfa_noiseFactorThisGain_dB < Math.min(edfa_noiseFactorMinimumGain_dB, edfa_noiseFactorMaximumGain_dB)) || (edfa_noiseFactorThisGain_dB > Math.max(edfa_noiseFactorMinimumGain_dB, edfa_noiseFactorMaximumGain_dB)))
					throw new RuntimeException("Bad");

				current_OSNR_linear = updateOSNRAfterEDFA(current_OSNR_linear, edfa_noiseFactorThisGain_dB, edfa_noiseFactorReferenceBandwidth_nm, current_powerPerChannel_dBm);
				current_powerPerChannel_dBm += edfaGain_dB;
				current_PMDSquared_ps2 += Math.pow(edfa_PMD_ps, 2);
			}
			else if (name.equalsIgnoreCase("DCM"))
			{
				final double cdCompensated_ps_per_nm = elementData;
				current_powerPerChannel_dBm -= dcm_insertionLoss_dB;
				current_CD_ps_per_nm += cdCompensated_ps_per_nm;
				current_PMDSquared_ps2 += Math.pow(dcm_PMD_ps, 2);
			}
			else
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
		final double numChannels_dB = linear2dB((double) channels_maxNumChannels);
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
				/* Wavelengths in use within transponder range  */
				if (channels_minChannelLambda_nm < tp_minWavelength_nm)	st += "Wavelength " + channels_minChannelLambda_nm + " nm is outside the transponder range [" + tp_minWavelength_nm + " nm, " + tp_maxWavelength_nm + " nm]";
				if (channels_maxChannelLambda_nm > tp_maxWavelength_nm) st += "Wavelength " + channels_maxChannelLambda_nm + " nm is outside the transponder range [" + tp_minWavelength_nm + " nm, " + tp_maxWavelength_nm + " nm]";

				/* Output power within limits */
				if (Math.abs(post_powerPerChannel_dBm - oadm_perChannelOutputPower_dBm) > 1E-3) st += "At " + initialPosition_km + "km: Power at the OADM-ADD output is " + post_powerPerChannel_dBm + " dBm. It should be: " + oadm_perChannelOutputPower_dBm;
			}
			else if (name.equalsIgnoreCase("OADM-EXPRESS"))
			{
				/* Input power within limits */
				if (pre_powerPerChannel_dBm < oadm_perChannelMinInputPower_dBm - 1E-3) st += "At " + initialPosition_km + "km: Power at the OADM-EXPRESS input is " + pre_powerPerChannel_dBm + " dBm. It should be between [" + oadm_perChannelMinInputPower_dBm + ", " + oadm_perChannelMaxInputPower_dBm + "] dBm";
				if (pre_powerPerChannel_dBm > oadm_perChannelMaxInputPower_dBm + 1E-3) st += "At " + initialPosition_km + "km: Power at the OADM-EXPRESS input is " + pre_powerPerChannel_dBm + " dBm. It should be between [" + oadm_perChannelMinInputPower_dBm + ", " + oadm_perChannelMaxInputPower_dBm + "] dBm";

				/* Output power within limits */
				if (Math.abs(post_powerPerChannel_dBm - oadm_perChannelOutputPower_dBm) > 1E-3) st += "At " + initialPosition_km + "km: Power at the OADM-EXPRESS output is " + post_powerPerChannel_dBm + " dBm. It should be: " + oadm_perChannelOutputPower_dBm;

				numberOfExpressOADMs++;
			}
			else if (name.equalsIgnoreCase("OADM-DROP"))
			{
				final double cdLimit = numberOfExpressOADMs == 0 ? oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm : tp_maxChromaticDispersionTolerance_ps_per_nm;
				
				/* CD at input */
				if (Math.abs(pre_CD_ps_per_nm) > cdLimit + 1E-3) st += "At " + initialPosition_km + "km: CD at the input of OADM-DROP is " + pre_CD_ps_per_nm + " dBm. It should be in absolute value below: " + cdLimit;

				/* Input power within limits */
				if (pre_powerPerChannel_dBm < oadm_perChannelMinInputPower_dBm - 1E-3) st += "At " + initialPosition_km + "km: Power at the OADM-DROP input is " + pre_powerPerChannel_dBm + " dBm. It should be between [" + oadm_perChannelMinInputPower_dBm + "," + oadm_perChannelMaxInputPower_dBm + "] dBm";
				if (pre_powerPerChannel_dBm > oadm_perChannelMaxInputPower_dBm + 1E-3) st += "At " + initialPosition_km + "km: Power at the OADM-DROP input is " + pre_powerPerChannel_dBm + " dBm. It should be between [" + oadm_perChannelMinInputPower_dBm + "," + oadm_perChannelMaxInputPower_dBm + "] dBm";

				/* Output power within limits */
				if (post_powerPerChannel_dBm < tp_inputPowerSensitivityMin_dBm - 1E-3) st += "At " + initialPosition_km + "km: Power at the OADM-DROP output is " + post_powerPerChannel_dBm + ". It should be between [" + tp_inputPowerSensitivityMin_dBm + "," + tp_inputPowerSensitivityMax_dBm + "] dBm";
				if (post_powerPerChannel_dBm > tp_inputPowerSensitivityMax_dBm + 1E-3) st += "At " + initialPosition_km + "km: Power at the OADM-DROP output is " + post_powerPerChannel_dBm + ". It should be between [" + tp_inputPowerSensitivityMin_dBm + "," + tp_inputPowerSensitivityMax_dBm + "] dBm";

				/* Chromatic dispersion in the receiver */
				if (post_CD_ps_per_nm > tp_maxChromaticDispersionTolerance_ps_per_nm) st += "At " + initialPosition_km + "km: Chromatic dispersion at the RECEIVER is " + post_CD_ps_per_nm + " ps/nm. It should be within +- " + tp_maxChromaticDispersionTolerance_ps_per_nm + " ps/nm";
				if (post_CD_ps_per_nm < -tp_maxChromaticDispersionTolerance_ps_per_nm) st += "At " + initialPosition_km + "km: Chromatic dispersion at the RECEIVER is " + post_CD_ps_per_nm + " ps/nm. It should be within +- " + tp_maxChromaticDispersionTolerance_ps_per_nm + " ps/nm";

				/* OSNR within limits */
				if (linear2dB(post_OSNR_linear) < tp_minOSNR_dB + osnrPenalty_SUM_dB) st += "At " + initialPosition_km + "km: OSNR at the RECEIVER is " + linear2dB(post_OSNR_linear) + " dB. It is below the tolerance plus margin " + tp_minOSNR_dB + " dB " + osnrPenalty_SUM_dB + " dB = " + (tp_minOSNR_dB + osnrPenalty_SUM_dB) + " dB)";

				/* PMD tolerance at the receiver */
				final double pmdAtReceiver = Math.sqrt(post_PMDSquared_ps2);
				if (pmdAtReceiver > tp_pmdTolerance_ps) st += "At " + initialPosition_km + "km: PMD at the RECEIVER is " + pmdAtReceiver + " ps. It is above the maximum PMD tolerance (" + tp_pmdTolerance_ps + " ps)";
			}
			else if (name.equalsIgnoreCase("SPAN"))
			{
			}
			else if (name.equalsIgnoreCase("EDFA"))
			{
				final double edfaGain_dB = elementData;
				
				/* Wavelengths within limits */
				if (channels_minChannelLambda_nm < edfa_minWavelength_nm) st += "Wavelength " + channels_minChannelLambda_nm + " nm is outside the transponder range [" + edfa_minWavelength_nm + " nm, " + edfa_maxWavelength_nm + " nm]";
				if (channels_maxChannelLambda_nm > edfa_maxWavelength_nm) st += "Wavelength " + channels_maxChannelLambda_nm + " nm is outside the transponder range [" + edfa_minWavelength_nm + " nm, " + edfa_maxWavelength_nm + " nm]";

				/* Gain within limits */
				if (edfaGain_dB < edfa_minGain_dB - 1E-3) st += "At " + initialPosition_km + "km: EDFA gain is " + edfaGain_dB + " dB. It should be between [" + edfa_minGain_dB + ", " + edfa_maxGain_dB + "] dB";
				if (edfaGain_dB > edfa_maxGain_dB + 1E-3) st += "At " + initialPosition_km + "km: EDFA gain is " + edfaGain_dB + " dB. It should be between [" + edfa_minGain_dB + ", " + edfa_maxGain_dB + "] dB";

				/* Input power within limits */
				if (pre_powerPerChannel_dBm < edfa_minInputPower_dBm - 1E-3) st += "At " + initialPosition_km + "km: Power at the EDFA input is (is one WDM channel) " + pre_powerPerChannel_dBm + " dBm. It should be between [" + edfa_minInputPower_dBm + ", " + edfa_maxInputPower_dBm + "] dBm";
				if (pre_powerPerChannel_dBm + numChannels_dB > edfa_maxInputPower_dBm + 1E-3) st += "At " + initialPosition_km + "km: Power at the EDFA input is (if all WDM channels are active) " + (pre_powerPerChannel_dBm + numChannels_dB) + " dBm. It should be between [" + edfa_minInputPower_dBm + "," + edfa_maxInputPower_dBm + "] dBm";

				/* Output power within limits */
				if (post_powerPerChannel_dBm < edfa_minOutputPower_dBm - 1E-3) st += "At " + initialPosition_km + "km: Power at the EDFA output is (is one WDM channel) " + post_powerPerChannel_dBm + " dBm. It should be between [" + edfa_minOutputPower_dBm + ", " + edfa_maxOutputPower_dBm + "] dBm";
				if (post_powerPerChannel_dBm + numChannels_dB > edfa_maxOutputPower_dBm + 1E-3) st += "At " + initialPosition_km + "km: Power at the EDFA output is (if all WDM channels are active) " + (post_powerPerChannel_dBm + numChannels_dB) + " dBm. It should be between [" + edfa_minOutputPower_dBm + ", " + edfa_maxOutputPower_dBm + "] dBm";
			}
			else if (name.equalsIgnoreCase("DCM"))
			{
				final double dcmCompensation_ps_per_nm = elementData;
				if ((dcmCompensation_ps_per_nm < dcm_channelDispersionMax_ps_per_nm) || (dcmCompensation_ps_per_nm > dcm_channelDispersionMin_ps_per_nm)) st += "At " + initialPosition_km + "km: DCM compensation is " + dcmCompensation_ps_per_nm + " ps/nm. It should be between [" + dcm_channelDispersionMax_ps_per_nm + ", " + dcm_channelDispersionMin_ps_per_nm + "] ps/nm";
			}
			else
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

		res.add(Triple.of(currentDistanceFromRouteInit_km, "OADM-ADD", (double) seqLinks.get(0).getOriginNode().getId ()));
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
			if (edfaPositions_km.length != edfaGains_dB.length) throw new Net2PlanException("Link: " + e + ". Number of elements in edfaPositions_km is not equal to the number of elements in edfaGains_dB");
			if (dcmPositions_km.length != dcmCDCompensation_ps_per_nm.length) throw new Net2PlanException("Link: " + e + ". Number of elements in dcmPositions_km is not equal to the number of elements in dcmCDCompensation_ps_per_nm");

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

				if (previousSpanLength < 0) throw new RuntimeException("Bad");

				if (previousSpanLength > 0)
				{
					res.add(Triple.of(currentDistanceFromRouteInit_km, "SPAN", previousSpanLength));
					currentDistanceFromRouteInit_km += previousSpanLength;
					posKmLastElementThisLink_km += previousSpanLength;
				}
				
				if (isDCM) res.add(Triple.of(currentDistanceFromRouteInit_km, "DCM", dcmCDCompensation_ps_per_nm[indexInCommonArray]));
				else res.add(Triple.of(currentDistanceFromRouteInit_km, "EDFA", edfaGains_dB[indexInCommonArray - dcmPositions_km.length]));
			}

			/* Last span of the link before the OADM */
			final double lastSpanOfLink_km = (Math.abs(d_e - posKmLastElementThisLink_km) < 1E-3) ? 0 : d_e - posKmLastElementThisLink_km;

			if (lastSpanOfLink_km < 0) throw new RuntimeException("Bad");

			if (lastSpanOfLink_km > 0)
			{
				res.add(Triple.of(currentDistanceFromRouteInit_km, "SPAN", lastSpanOfLink_km));
				currentDistanceFromRouteInit_km += lastSpanOfLink_km;
				posKmLastElementThisLink_km += lastSpanOfLink_km;
			}

			/* OADM at the end of the link */
			final long endNodeLink = e.getDestinationNode().getId ();
			final long lastLink = seqLinks.get(seqLinks.size() - 1).getId ();
			if (e.getId () == lastLink) res.add(Triple.of(currentDistanceFromRouteInit_km, "OADM-DROP", (double) endNodeLink));
			else res.add(Triple.of(currentDistanceFromRouteInit_km, "OADM-EXPRESS", (double) endNodeLink));
		}

		/* Check current distance equals the sum of the traversed links */
		double sumLinks = 0;
		for (Link e : seqLinks) sumLinks += e.getLengthInKm();

		if (Math.abs(sumLinks - currentDistanceFromRouteInit_km) > 1E-3) throw new RuntimeException("Bad");
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

	private String printReport(Map<Link, LinkedList<Triple<Double, String, Double>>> elements_e, Map<Link, LinkedList<Pair<double[], double[]>>> impairments_e, Map<Link, LinkedList<String>> warnings_e, Map<Route, LinkedList<Triple<Double, String, Double>>> elements_r, Map<Route, LinkedList<Pair<double[], double[]>>> impairments_r, Map<Route, LinkedList<String>> warnings_r)
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
		out.append("<tr><th><b>Link #</b></th><th><b>Length (km)</b></th><th><b># EDFAs</b></th><th><b># DCMs</b></th><th><b>Chromatic Dispersion (ps/nm)</b></th><th><b>OSNR (dB)</b></th><th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th><th><b>Warnings</b></th></tr>");
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
			for (String s : w) warnings.append(s);

			out.append("<tr><td>").append(e).append(" (").append(st_a_e).append(" --> ").append(st_b_e).append(") </td><td>").append(df_2.format(d_e)).append("</td><td>").append(numEDFAs).append("</td><td>").append(numDCMs).append("</td><td>").append(df_2.format(impInfoInputOADM[1])).append("</td><td>").append(df_2.format(linear2dB(impInfoInputOADM[3]))).append("</td><td>").append(df_2.format(impInfoInputOADM[0])).append("</td><td>").append(df_2.format(Math.sqrt(impInfoInputOADM[2]))).append("</td><td>").append(warnings).append("</td></tr>");
		}
		out.append("</table>");

		out.append("<h2>PER ROUTE INFORMATION SUMMARY - Signal metrics at the transponder</h2>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Route #</b></th><th><b>Length (km)</b></th><th><b># EDFAs</b></th><th><b># DCMs</b></th><th><b>Chromatic Dispersion (ps/nm)</b></th><th><b>OSNR (dB)</b></th><th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th><th><b>Warnings</b></th></tr>");
		for (Route r : netPlan.getRoutes())
		{
			final double d_r = r.getLengthInKm();
			final String st_a_r = r.getIngressNode().getName ();
			final String st_b_r = r.getEgressNode().getName ();
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
			for (String s : w) warnings.append(s);

			out.append("<tr><td>").append(r).append(" (").append(st_a_r).append(" --> ").append(st_b_r).append(") </td><td>").append(df_2.format(d_r)).append("</td><td>").append(numEDFAs).append("</td><td>").append(numDCMs).append("</td><td>").append(df_2.format(impInfoInputOADM[1])).append("</td><td>").append(df_2.format(linear2dB(impInfoInputOADM[3]))).append("</td><td>").append(df_2.format(impInfoInputOADM[0])).append("</td><td>").append(df_2.format(Math.sqrt(impInfoInputOADM[2]))).append("</td><td>").append(warnings.toString()).append("</td>" + "</tr>");
		}
		out.append("</table>");

		out.append("<h2>PER-LINK DETAILED INFORMATION </h2>");
		out.append("<p>Number of links: ").append(netPlan.getNumberOfLinks()).append("</p>");

		for (Link e : netPlan.getLinks())
		{
			final double d_e = e.getLengthInKm();
			final String st_a_e = e.getOriginNode().getName ();
			final String st_b_e = e.getDestinationNode().getName ();
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
			out.append("<tr><th><b>Position (km)</b></th><th><b>Position (description)</b></th><th><b>Chromatic Dispersion (ps/nm)</b></th><th><b>OSNR (dB)</b></th><th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th><th><b>Warnings</b></th></tr>");
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

				if (elementType.equalsIgnoreCase("EDFA")) elementType += " (gain " + elementAuxData + " dB)";
				else if (elementType.equalsIgnoreCase("SPAN")) elementType += " (" + elementAuxData + " km)";
				else if (elementType.equalsIgnoreCase("DCM")) elementType += " (comp " + elementAuxData + " ps/nm)";

				out.append("<tr><td>").append(df_2.format(pos_km)).append("</td><td>" + "Input of ").append(elementType).append("</td><td>").append(df_2.format(prevInfo[1])).append("</td><td>").append(df_2.format(linear2dB(prevInfo[3]))).append("</td><td>").append(df_2.format(prevInfo[0])).append("</td><td>").append(df_2.format(Math.sqrt(prevInfo[2]))).append("</td><td>").append(this_warnings).append("</td>" + "</tr>");

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
			out.append("<tr><th><b>Position (km)</b></th><th><b>Position (description)</b></th><th><b>Chromatic Dispersion (ps/nm)</b></th><th><b>OSNR (dB)</b></th><th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th><th><b>Warnings</b></th></tr>");
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
				if (elementType.equalsIgnoreCase("EDFA")) elementType += " (gain " + elementAuxData + " dB)";
				else if (elementType.equalsIgnoreCase("SPAN")) elementType += " (" + elementAuxData + " km)";
				else if (elementType.equalsIgnoreCase("DCM")) elementType += " (comp " + elementAuxData + " ps/nm)";

				out.append("<tr><td>").append(df_2.format(pos_km)).append("</td><td>" + "Input of ").append(elementType).append("</td><td>").append(df_2.format(prevInfo[1])).append("</td><td>").append(df_2.format(linear2dB(prevInfo[3]))).append("</td><td>").append(df_2.format(prevInfo[0])).append("</td><td>").append(df_2.format(Math.sqrt(prevInfo[2]))).append("</td><td>").append(this_warnings).append("</td>" + "</tr>");

			}
			final Triple<Double, String, Double> this_el = el.getLast();
			final Pair<double[], double[]> this_imp = imp.getLast();
			final String this_warnings = w.getLast();
			final double pos_km = this_el.getFirst();
			final double[] postInfo = this_imp.getSecond();
			out.append("<tr><td>").append(df_2.format(pos_km)).append("</td><td>" + "Receiver" + "</td><td>").append(df_2.format(postInfo[1])).append("</td><td>").append(df_2.format(linear2dB(postInfo[3]))).append("</td><td>").append(df_2.format(postInfo[0])).append("</td><td>").append(df_2.format(Math.sqrt(postInfo[2]))).append("</td><td>").append(this_warnings).append("</td>" + "</tr>");

			out.append("</table>");
		}

		out.append("</body></html>");
		return out.toString();
	}

	private double updateOSNRAfterEDFA(double currentOSNR_linear, double noiseFactor_dB, double noiseFactorReferenceBandwidth_nm, double inputPowerPerChannel_dBm)
	{
		final double edfa_NF_linear = dB2Linear(noiseFactor_dB);
		final double highestFrequencyChannel_Hz = constant_c / (channels_minChannelLambda_nm * 1e-9);
		final double referenceBandwidthAtHighestFrequency_Hz = -highestFrequencyChannel_Hz + constant_c / ((channels_minChannelLambda_nm - noiseFactorReferenceBandwidth_nm) * 1e-9);
		final double inputPower_linear = dB2Linear(inputPowerPerChannel_dBm) * 1E-3;
		final double thisEDFAAddedNoise_linear = edfa_NF_linear * constant_h * highestFrequencyChannel_Hz * referenceBandwidthAtHighestFrequency_Hz;
		final double addedOSNRThisOA_linear = inputPower_linear / thisEDFAAddedNoise_linear;
		final double new_OSNR_linear = (currentOSNR_linear == Double.MAX_VALUE) ? addedOSNRThisOA_linear : 1 / (1 / currentOSNR_linear + 1 / addedOSNRThisOA_linear);
		return new_OSNR_linear;
	}
}
