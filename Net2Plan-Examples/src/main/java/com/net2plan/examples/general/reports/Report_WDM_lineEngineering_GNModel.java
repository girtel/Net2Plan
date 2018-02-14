package com.net2plan.examples.general.reports;

import com.net2plan.interfaces.networkDesign.IReport;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.libraries.OpticalImpairmentUtils;
import com.net2plan.utils.Constants.OrderingType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Quadruple;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections15.ListUtils;
import org.apache.commons.lang.ArrayUtils;

/**
 * <p>
 * This report shows line engineering information for WDM links in a multilayer optical network. The impairment calculations are based on the Gaussian Noise Model developed by Politecnico di Torino and inspired in the procedures described in the 2009
 * ITU-T WDM manual "Optical fibres, cabbles and systems".
 * </p>
 * <p>
 * The report assumes that the WDM network follows the scheme:
 * </p>
 * <ul>
 * <li>In the net2plan object, nodes are OADMs, links are fiber links, and routes are lightpaths: WDM channels optically switched at intermediate nodes.</li>
 * <li>Nodes are connected by unidirectional fiber links. Fiber link distance is given by the link length. Other specifications are given by fibers_XXX input parameters, each one describing the parameter for the fiber types specified in fibers_types,
 * in the same order and separated by spaces. The fiber can be split into spans if optical amplifers (EDFAs) and/or passive components (PCs) are placed along the fiber. These spans can be of different fiber types as long as they are described in a
 * link attribute called "fiberTypes". Must be separated by spaces and, in case that there were more spans than elements of the attribute, the default type given in "fiber_default_type" would be used.</li>
 * <li>Optical line amplifiers (EDFAs) can be located in none, one or more positions in the fiber link, separating them in different spans. EDFAs are supposed to operate in the automatic gain control mode. Thus, the gain is the same, whatever the
 * number of input WDM channels. EDFA positions (as distance" in km from the link start to the EDFA location), EDFA gains (assumed in dB) and EDFA noise figures (in dB) are read from the "edfaPositions_km", "edfaGains_dB" and "edfaNoiseFigures_dB"
 * attributes of the links. The format of all attributes are the same: a string of numbers separated by spaces. The <i>i</i>-th number corresponding to the position/gain of the <i>i</i>-th EDFA. If the attributes do not exist, it is assumed that no
 * EDFAs are placed in this link. EDFA specifications are given by "edfa_XXX" parameters</li>
 * <li>There are not Dispersion compensating modules (DCMs) in the topoology, since the Gaussian Noise Model is used.</li>
 * <li>Passive components are described by the link attributes "pcPositions_km" and "pcLosses_dB". The <i>i</i>-th number corresponding to the position/loss of the <i>i</i>-th PC. If the attributes do not exist, it is assumed that no PCs are placed
 * in this link. Other specifications for PC will be described in teh pc_XXX input parameters.</li>
 * <li>Fiber links start and end in OADM modules, that permit adding, dropping and optically switch individual WDM channels. OADMs have a pre-amplifier (traversed by drop and express channels) and a boost amplifier (traversed by add and express
 * channels). They are supposed to equalize the channel power at their outputs, to a fixed value (added and express channels will thus have the same power in the fibers). Also, OADMs attenuate appropriately the optical signal coming from the
 * pre-amplifier, in the drop channels, so that they fall within the receiver sensitivity range. OADM noise figures for add, drop and express channels are given as input parameters. PMD values for add, drop and express channels are computed assumming
 * that: (i) add channel traverse a multiplexer and the booster, (ii) drop channels travese the pre-amplifier and a demultiplexer, (iii) express channels traverse the two amplifiers. The required parameters are provided in oadm_XXX parameters.</li>
 * <li>Each channel ends in a receiver, with specifications given by "tp_XXX" parameters.</li>
 * </ul>
 * <p>
 * The basic checks performed are:
 * </p>
 * <ul>
 * <li>For each link, signal power levels are within operating ranges at the oadm/edfas, both when the link has one single active channel, or when all the "gn_spec_nCh" are active</li>
 * <li>For each route (lightpath), OSNR (Optical Signal to Noise Ration) is within the operating range at the receiver. A set of margins are considered to account to several not directly considered impairments.</li>
 * <li>For each route (lightpath), PMD (Polarization mode dispersion) is within the operating range at the receiver</li>
 * </ul>
 * 
 * @net2plan.keywords WDM, Multilayer
 * @author Pablo Pavon-Marino, Elena Martin-Seoane
 * @version 1.3, November 2017 */
public class Report_WDM_lineEngineering_GNModel implements IReport
{
	/* Constants */
	private final static double	delta_f					= 6.25E-3;	/* GHz of the slots in the grid */
	private final static double	infinityThreshold_dB	= 300;		/* starting value to consider the OSNR perfect */

	/* Input parameters */
	private NetPlan				netPlan;
	private Map<String, String>	reportParameters;

	/* GN General parameters */
	private final InputParameter	gn_gen_f0_THz	= new InputParameter("gn_gen_f0_THz", (double) 192.075, "Starting frequency of the laser grid used to describe the WDM system [THz]");
	private final InputParameter	gn_gen_ns		= new InputParameter("gn_gen_ns", (int) 800, "Number of 6.25 GHz slots in the grid");

	/* Usable wavelengths */
	private double	channels_minChannelLambda_nm;
	private double	channels_maxChannelLambda_nm;

	/* GN spectrum description */
	private final InputParameter	gn_spec_nCh				= new InputParameter("gn_spec_nCh", (int) 16, "Number of used channels defined in the spectrum.");
	private final InputParameter	gn_spec_laserPosition	= new InputParameter("gn_spec_laserPosition", "false false true false false false", "A list of booleans indicating whether a laser is turned on or not (per each channel)");
	private final InputParameter	gn_spec_bandwidthCh_THz	= new InputParameter("gn_spec_bandwidthCh_THz", (double) 0.032, "The -3 dB WDM channel bandwidth (for a root raised cosine, it is equal to the symbol rate)");

	/* Fiber specifications */
	private final InputParameter	fiber_PMD_ps_per_sqroot_km			= new InputParameter("fiber_PMD_ps_per_sqroot_km", (double) 0.4, "Polarization mode dispersion per km^0.5 of fiber (PMD_Q link factor)");
	private final InputParameter	fiber_default_type					= new InputParameter("fiber_default_type", "SMF",
			"A string calling the type of fiber described (can be override by the 'fiberTypes' Net2Plan attribute). Must be a value from 'fibers_types'.");
	/* GN Fiber parameters */
	private final InputParameter	fibers_alpha_dB_per_km				= new InputParameter("fibers_alpha_dB_per_km", "0.2 0.22", "The attenuation coefficient for each fiber type [dB/km]");
	private final InputParameter	fibers_alpha1st_dB_per_km_per_THz	= new InputParameter("fibers_alpha1st_dB_per_km_per_THz", "0 0",
			"The first derivative of alpha indicating the alpha slope for each fiber type [dB/km/THz]. Should be zero if you assume a flat attenuation with respect to the frequency");
	private final InputParameter	fibers_beta2_ps2_per_km				= new InputParameter("fibers_beta2_ps2_per_km", "21.27 21", "The dispersion coefficient for each fiber type [ps^2/km]");
	private final InputParameter	fibers_n2_m2_per_W					= new InputParameter("fibers_n2_m2_per_W", "2.5E-20 2.5E-20", "Second-order nonlinear refractive index for each fiber type [m^2/W]. A typical value is 2.5E-20 m^2/W");
	private final InputParameter	fibers_Aeff_um2						= new InputParameter("fibers_Aeff_um2", "77.77 70", "The effective area for each fiber type [um^2]");
	private final InputParameter	fibers_types						= new InputParameter("fibers_types", "SMF NZDF", "The names of the fiber types described in the other fibers_XXX parameters. They MUST BE ordered.");
	private final InputParameter	fibers_numberOfFiberTypes			= new InputParameter("fibers_numberOfFiberTypes", (int) 2, "The number of different fiber types described. Must be equal to the length of the others fibers_XXX parameters.");

	/* Transponder specifications */
	private final InputParameter	tp_minOSNR_dB					= new InputParameter("tp_minOSNR_dB", (double) 7, "Minimum OSNR needed at the receiver");
	private final InputParameter	tp_minWavelength_nm				= new InputParameter("tp_minWavelength_nm", (double) 1529.55, "Minimum wavelength usable by the transponder");
	private final InputParameter	tp_maxWavelength_nm				= new InputParameter("tp_maxWavelength_nm", (double) 1561.84, "Maximum wavelength usable by the transponder");
	private final InputParameter	tp_pmdTolerance_ps				= new InputParameter("tp_pmdTolerance_ps", (double) 10, "Maximum tolarance of polarizarion mode dispersion (mean of differential group delay) in ps at the receiver");
	private final InputParameter	tp_inputPowerSensitivityMin_dBm	= new InputParameter("tp_inputPowerSensitivityMin_dBm", (double) -20, "Minimum input power at the receiver in dBm");
	private final InputParameter	tp_inputPowerSensitivityMax_dBm	= new InputParameter("tp_inputPowerSensitivityMax_dBm", (double) -8, "Maximum input power at the receiver in dBm");

	/* OADM specs */
	private final InputParameter	oadm_outputPowerPerChannel_W		= new InputParameter("oadm_outputPowerPerChannel_W", (double) 1E-3, "The WDM channel power at the output of the OADM [W]");
	private final InputParameter	oadm_perChannelMinInputPower_dBm	= new InputParameter("oadm_perChannelMinInputPower_dBm", (double) -19, "Minimum power needed at the OADM input");
	private final InputParameter	oadm_perChannelMaxInputPower_dBm	= new InputParameter("oadm_perChannelMaxInputPower_dBm", (double) 1000, "Maximum power admitted at the OADM input");
	private final InputParameter	oadm_muxDemuxPMD_ps					= new InputParameter("oadm_muxDemuxPMD_ps", (double) 0.5, "PMD of the mux/demux inside the OADMs. Does not affect express lightpaths");
	private final InputParameter	oadm_preAmplifierPMD_ps				= new InputParameter("oadm_preAmplifierPMD_ps", (double) 0.5, "PMD off OADM preamplifier");
	private final InputParameter	oadm_boosterPMD_ps					= new InputParameter("oadm_boosterPMD_ps", (double) 0.5, "PMD off OADM booster amplifier");
	private final InputParameter	oadm_addChannelNoiseFactor_dB		= new InputParameter("oadm_addChannelNoiseFactor_dB", (double) 6, "Noise factor observed by add channels");
	private final InputParameter	oadm_dropChannelNoiseFactor_dB		= new InputParameter("oadm_dropChannelNoiseFactor_dB", (double) 6, "Noise factor observed by drop channels");
	private final InputParameter	oadm_expressChannelNoiseFactor_dB	= new InputParameter("oadm_expressChannelNoiseFactor_dB", (double) 10, "Noise factor observed by express channels");

	/* Optical line amplifier specifications */
	private final InputParameter	edfa_minWavelength_nm			= new InputParameter("edfa_minWavelength_nm", (double) 1530, "Minimum wavelength usable by the EDFA");
	private final InputParameter	edfa_maxWavelength_nm			= new InputParameter("edfa_maxWavelength_nm", (double) 1563, "Maximum wavelength usable by the EDFA");
	private final InputParameter	edfa_minInputPower_dBm			= new InputParameter("edfa_minInputPower_dBm", (double) -29, "Minimum input power at the EDFA");
	private final InputParameter	edfa_maxInputPower_dBm			= new InputParameter("edfa_maxInputPower_dBm", (double) 2, "Maximum input power at the EDFA");
	private final InputParameter	edfa_minOutputPower_dBm			= new InputParameter("edfa_minOutputPower_dBm", (double) -6, "Minimum output power at the EDFA");
	private final InputParameter	edfa_maxOutputPower_dBm			= new InputParameter("edfa_maxOutputPower_dBm", (double) 19, "Maximum output power at the EDFA");
	private final InputParameter	edfa_minGain_dB					= new InputParameter("edfa_minGain_dB", (double) 17, "Minimum gain at the EDFA");
	private final InputParameter	edfa_maxGain_dB					= new InputParameter("edfa_maxGain_dB", (double) 23, "Maximum gain at the EDFA");
	private final InputParameter	edfa_PMD_ps						= new InputParameter("edfa_PMD_ps", (double) 0.5, "Polarization mode dispersion in ps added by the EDFA");
	private final InputParameter	edfa_default_noiseFactor_dB		= new InputParameter("edfa_default_noiseFactor_dB", (double) 3, "Default noise factor used when the link does not have the attribute");
	private final InputParameter	edfa_noiseFactorMaximumGain_dB	= new InputParameter("edfa_noiseFactorMaximumGain_dB", (double) 5,
			"Noise factor at the EDFA when the gain is in its upper limit (linear interpolation is used to estimate the noise figure at other gains)");
	private final InputParameter	edfa_noiseFactorMinimumGain_dB	= new InputParameter("edfa_noiseFactorMinimumGain_dB", (double) 5,
			"Noise factor at the EDFA when the gain is in its lower limit (linear interpolation is used to estimate the noise figure at other gains)");

	/* PC specs */
	private final InputParameter pc_PMD_ps = new InputParameter("pc_PMD_ps", (double) 0.5, "Polarization mode dispersion in ps added by the PC");

	/* OSNR penalties */
	private final InputParameter	osnrPenalty_nonLinear_dB		= new InputParameter("osnrPenalty_nonLinear_dB", (double) 2, "OSNR penalty caused by the non-linear effects SPM, XPM, FWM and Brillouin / Raman scattering");
	private final InputParameter	osnrPenalty_PMD_dB				= new InputParameter("osnrPenalty_PMD_dB", (double) 0.5, "OSNR penalty caused by the polarization mode dispersion (assumed within limits)");
	private final InputParameter	osnrPenalty_PDL_dB				= new InputParameter("osnrPenalty_PDL_dB", (double) 0.3, "OSNR penalty caused by polarization dispersion losses");
	private final InputParameter	osnrPenalty_transmitterChirp_dB	= new InputParameter("osnrPenalty_transmitterChirp_dB", (double) 0.5, "OSNR penalty caused by transmitter chirp ");
	private final InputParameter	osnrPenalty_OADMCrosstalk_dB	= new InputParameter("osnrPenalty_OADMCrosstalk_dB", (double) 0.8, "OSNR penalty caused by the crosstalk at the OADMs");
	private final InputParameter	osnrPenalty_unassignedMargin_dB	= new InputParameter("osnrPenalty_unassignedMargin_dB", (double) 3, "OSNR penalty caused by not assigned margins (e.g. random effects, aging, ...)");
	private double					osnrPenalty_SUM_dB;

	/* Global parameters */
	private Map<String, double[]>		spectrumParameters;
	Map<String, Map<String, Double>>	fiberParameters;
	private double						centralFreq_THz;
	private double[]					frequenciesPerChannel_THz;

	@Override
	public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
	{
		/* Input parameters */
		this.netPlan = netPlan;
		this.reportParameters = reportParameters;

		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, reportParameters);
		fiberParameters = getFiberSpecsMap();

		/* Initialize GN Parameters */
		centralFreq_THz = gn_gen_f0_THz.getDouble() + (Math.floor(gn_gen_ns.getInt() / 2.0) * delta_f);
		final Boolean[] laser_position = getLaserPositions(StringUtils.toBooleanArray(StringUtils.split(gn_spec_laserPosition.getString())));
		frequenciesPerChannel_THz = getBasebandFrequency(laser_position);

		/* Usable wavelengths */
		channels_minChannelLambda_nm = OpticalImpairmentUtils.constant_c / ((gn_gen_f0_THz.getDouble() * 1e3) + gn_gen_ns.getInt() * delta_f);
		channels_maxChannelLambda_nm = (OpticalImpairmentUtils.constant_c / (gn_gen_f0_THz.getDouble() * 1e12)) * 1e9;

		/* OSNR penalties */
		osnrPenalty_SUM_dB = osnrPenalty_nonLinear_dB.getDouble() + osnrPenalty_PMD_dB.getDouble() + osnrPenalty_PDL_dB.getDouble() + osnrPenalty_transmitterChirp_dB.getDouble() + osnrPenalty_OADMCrosstalk_dB.getDouble()
				+ osnrPenalty_unassignedMargin_dB.getDouble();

		final Map<Link, List<Quadruple<Double, String, Double, String>>> elements_e = new LinkedHashMap<Link, List<Quadruple<Double, String, Double, String>>>();
		final Map<Link, List<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>>> impairments_e = new LinkedHashMap<Link, List<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>>>();
		final Map<Link, List<String>> warnings_e = new LinkedHashMap<Link, List<String>>();

		for (Link link : netPlan.getLinks())
		{
			final List<Link> seqLinks = new LinkedList<Link>();
			seqLinks.add(link);
			final List<Quadruple<Double, String, Double, String>> elementPositions = getElementPositionsListPerLightpath(seqLinks);

			spectrumParameters = initializeSpectrum();

			final List<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>> impairmentsAtInputAndOutputs = OpticalImpairmentUtils.computeImpairments(elementPositions, spectrumParameters, fiberParameters,
					oadm_outputPowerPerChannel_W.getDouble(), fiber_PMD_ps_per_sqroot_km.getDouble(), edfa_PMD_ps.getDouble(), pc_PMD_ps.getDouble(), oadm_muxDemuxPMD_ps.getDouble(), oadm_preAmplifierPMD_ps.getDouble(),
					oadm_boosterPMD_ps.getDouble(), frequenciesPerChannel_THz, centralFreq_THz, tp_inputPowerSensitivityMin_dBm.getDouble(), tp_inputPowerSensitivityMax_dBm.getDouble());
			final List<String> warningMessages = computeWarningMessages(elementPositions, impairmentsAtInputAndOutputs);

			elements_e.put(link, elementPositions);
			impairments_e.put(link, impairmentsAtInputAndOutputs);
			warnings_e.put(link, warningMessages);
		}

		final Map<Route, List<Quadruple<Double, String, Double, String>>> elements_r = new LinkedHashMap<Route, List<Quadruple<Double, String, Double, String>>>();
		final Map<Route, List<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>>> impairments_r = new LinkedHashMap<Route, List<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>>>();
		final Map<Route, List<String>> warnings_r = new LinkedHashMap<Route, List<String>>();
		for (Route r : netPlan.getRoutes())
		{
			final List<Link> seqLinks = r.getSeqLinks();
			final List<Quadruple<Double, String, Double, String>> elementPositions = getElementPositionsListPerLightpath(seqLinks);

			spectrumParameters = initializeSpectrum();

			final List<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>> impairmentsAtInputAndOutputs = OpticalImpairmentUtils.computeImpairments(elementPositions, spectrumParameters, fiberParameters,
					oadm_outputPowerPerChannel_W.getDouble(), fiber_PMD_ps_per_sqroot_km.getDouble(), edfa_PMD_ps.getDouble(), pc_PMD_ps.getDouble(), oadm_muxDemuxPMD_ps.getDouble(), oadm_preAmplifierPMD_ps.getDouble(),
					oadm_boosterPMD_ps.getDouble(), frequenciesPerChannel_THz, centralFreq_THz, tp_inputPowerSensitivityMin_dBm.getDouble(), tp_inputPowerSensitivityMax_dBm.getDouble());
			final List<String> warningMessages = computeWarningMessages(elementPositions, impairmentsAtInputAndOutputs);

			elements_r.put(r, elementPositions);
			impairments_r.put(r, impairmentsAtInputAndOutputs);
			warnings_r.put(r, warningMessages);
		}

		return printReport(elements_e, impairments_e, warnings_e, elements_r, impairments_r, warnings_r);
	}

	@Override
	public String getDescription()
	{
		return "This report shows line engineering information for WDM links in the network. " + " The report assumes that the WDM network follows the scheme:\n"
				+ " * In the net2plan object, nodes are OADMs, links are fiber links  and routes are lightpaths:\n" + "WDM channels optically switched at intermediate nodes.\n"
				+ " * Nodes are connected by unidirectional fiber links. Fiber link distance is" + " given by the link length. Other specifications are given by fibers_XXX input parameters, each one describing the"
				+ "parameter for the fiber types specified in fibers_types, in the same order and separated by" + "spaces. The fiber can be split into spans if optical amplifers (EDFAs) and/or passive components"
				+ "(PCs) are placed along the fiber. These spans can be of different fiber types as long as they are" + "described in a link attribute called \"fiberTypes\". Must be separated by spaces and, in case that"
				+ "there were more spans than elements of the attribute, the default type given in" + "\"fiber_default_type\" would be used." + " * Optical line amplifiers (EDFAs) can be located in none, one or more"
				+ " positions in the fiber link, separating them into different spans. EDFAs are" + " supposed to operate in the automatic gain control mode. Thus, the gain is the"
				+ " same, whatever the number of input WDM channels. EDFA positions (as distance" + " in km from the link start to the EDFA location), EDFA gains (assumed in"
				+ " dB) and EDFA noise figures (in dB) are read from the \"edfaPositions_km\", \"edfaGains_dB\" and \"edfaNoiseFigures_dB\"" + " attributes of the links. The format of all attributes will be the same: a string of numbers"
				+ " separated by spaces. The i-th number corresponding to the position/gain of the" + " i-th EDFA. If the attributes do not exist, it is assumed that no EDFAs"
				+ " are placed in this link. EDFA specifications are given by \"edfa_XXX\" parameters.\n" + " * Passive components are described by the link attributes \"pcPositions_km\" and \"pcLosses_dB\".\n"
				+ " The i-th number corresponding to the position/loss of the i-th PC.\n" + " If the attributes do not exist, it is assumed that no PCs are placed in this link. \n" + " Further description in the HTML generated.";
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
		return "WDM line engineering with GN calculations";
	}

	/** Checks if the number of elements in the fiber input parameters is always the same. If it is, returns a Map for each type of fiber with its parameters
	 * 
	 * @return Map("FiberTypeName", Map("param", value)) */
	private Map<String, Map<String, Double>> getFiberSpecsMap()
	{
		final Map<String, Map<String, Double>> fiberSpecs = new HashMap<String, Map<String, Double>>();

		final String[] fiberTypes = StringUtils.split(fibers_types.getString());
		final double[] fiberAlphas = StringUtils.toDoubleArray(StringUtils.split(fibers_alpha_dB_per_km.getString()));
		final double[] fiberAlpha1sts = StringUtils.toDoubleArray(StringUtils.split(fibers_alpha1st_dB_per_km_per_THz.getString()));
		final double[] fiberBeta2s = StringUtils.toDoubleArray(StringUtils.split(fibers_beta2_ps2_per_km.getString()));
		final double[] fiberAeffs = StringUtils.toDoubleArray(StringUtils.split(fibers_Aeff_um2.getString()));
		final double[] fiberN2s = StringUtils.toDoubleArray(StringUtils.split(fibers_n2_m2_per_W.getString()));
		final int numFiberTypes = fibers_numberOfFiberTypes.getInt();

		if (numFiberTypes != fiberTypes.length || numFiberTypes != fiberAlphas.length || numFiberTypes != fiberAlpha1sts.length || numFiberTypes != fiberBeta2s.length || numFiberTypes != fiberAeffs.length || numFiberTypes != fiberN2s.length)
			throw new Net2PlanException("Incorrect number of fiber parameters.");

		boolean containsDefaultType = false;
		for (String string : fiberTypes)
			if (string.equalsIgnoreCase(fiber_default_type.getString()))
				containsDefaultType = true;

		if (!containsDefaultType)
			throw new Net2PlanException("fiber_default_type is not contained in the fibers_types list.");

		for (int i = 0; i < fiberTypes.length; i++)
		{
			final String fiberType = fiberTypes[i];

			final Map<String, Double> specs_thisType = new HashMap<String, Double>();
			specs_thisType.put(OpticalImpairmentUtils.stFiber_alpha_dB_per_km, fiberAlphas[i]);
			specs_thisType.put(OpticalImpairmentUtils.stFiber_alpha1st_dB_per_km_per_THz, fiberAlpha1sts[i]);
			specs_thisType.put(OpticalImpairmentUtils.stFiber_beta2_ps2_per_km, fiberBeta2s[i]);
			specs_thisType.put(OpticalImpairmentUtils.stFiber_effectiveArea_um2, fiberAeffs[i]);
			specs_thisType.put(OpticalImpairmentUtils.stFiber_n2Coeff_m2_per_W, fiberN2s[i]);

			fiberSpecs.put(fiberType, specs_thisType);
		}

		return fiberSpecs;
	}

	/** Initializes all spectrum parameters with the given input parameters
	 * 
	 * @return initial spectrum */
	private Map<String, double[]> initializeSpectrum()
	{
		final Map<String, double[]> spectrumParameters = new HashMap<>();
		final int numChannels = gn_spec_nCh.getInt();

		final double[] bandwidthPerChannel_THz = new double[numChannels];
		final double[] powerPerChannel_W = new double[numChannels];
		final double[] aseNoisePower_W = new double[numChannels];
		final double[] nliNoisePower_W = new double[numChannels];

		Arrays.fill(bandwidthPerChannel_THz, gn_spec_bandwidthCh_THz.getDouble());
		Arrays.fill(powerPerChannel_W, oadm_outputPowerPerChannel_W.getDouble());
		Arrays.fill(aseNoisePower_W, 0);
		Arrays.fill(nliNoisePower_W, 0);

		spectrumParameters.put(OpticalImpairmentUtils.stSpectrum_bandwidthPerChannel_THz, bandwidthPerChannel_THz);
		spectrumParameters.put(OpticalImpairmentUtils.stSpectrum_powerPerChannel_W, powerPerChannel_W);
		spectrumParameters.put(OpticalImpairmentUtils.stSpectrum_aseNoisePower_W, aseNoisePower_W);
		spectrumParameters.put(OpticalImpairmentUtils.stSpectrum_nliNoisePowerg_W, nliNoisePower_W);

		return spectrumParameters;
	}

	/** Gets an array of booleans with the status of the lasers for all channels
	 * 
	 * @param lp laser positions per channel
	 * @return extended array with the lp of every channel */
	private Boolean[] getLaserPositions(boolean[] lp)
	{

		List<Boolean> lasers = new LinkedList<>();
		final List<Boolean> lps = Arrays.asList(ArrayUtils.toObject(lp));

		for (int i = 0; i < gn_spec_nCh.getInt(); i++)
			lasers = ListUtils.union(lasers, lps);

		return lasers.toArray(new Boolean[lasers.size()]);

	}

	/** Initializes frequencies for each channel
	 * 
	 * @param laser_position boolean whether a laser is turn on or not
	 * @return frequencies per channel */
	private double[] getBasebandFrequency(Boolean[] laser_position)
	{
		double[] frequenciesPerChannel_THz = DoubleUtils.zeros(gn_spec_nCh.getInt());

		int count = 0;
		for (int i = 0; i < laser_position.length; i++)
			if (laser_position[i])
				frequenciesPerChannel_THz[count++] = (gn_gen_f0_THz.getDouble() - centralFreq_THz) + delta_f * i;

		return frequenciesPerChannel_THz;
	}

	/** Gets the network warnings for the elements and impairments given
	 * 
	 * @param elementPositions List of Quadruple of(position [km], type, 3rd: data; 4th: auxData)
	 * @param impairmentsAtInputAndOutputs List of Quadruple of (before element Map(paramName, value), before element PMD, after element Map(paramName, value), after element PMD)
	 * @return warnings */
	private List<String> computeWarningMessages(List<Quadruple<Double, String, Double, String>> elementPositions, List<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>> impairmentsAtInputAndOutputs)
	{
		final double numChannels_dB = OpticalImpairmentUtils.linear2dB(gn_spec_nCh.getInt());
		final int centralChannel = Math.floorDiv(gn_spec_nCh.getInt(), 2);
		final List<String> res = new LinkedList<String>();

		final Iterator<Quadruple<Double, String, Double, String>> it_elementPositions = elementPositions.iterator();
		final Iterator<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>> it_impairments = impairmentsAtInputAndOutputs.iterator();

		while (it_elementPositions.hasNext())
		{
			String st = "";

			final Quadruple<Double, String, Double, String> thisElement = it_elementPositions.next();
			final Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double> thisImpairments = it_impairments.next();

			final double initialPosition_km = thisElement.getFirst();
			final String name = thisElement.getSecond();
			final double elementData = thisElement.getThird();

			final Map<String, double[]> preImpSpectrum = thisImpairments.getFirst();
			final double pre_powerPerChannel_dBm = OpticalImpairmentUtils.linear2dB(preImpSpectrum.get(OpticalImpairmentUtils.stSpectrum_powerPerChannel_W)[centralChannel] * 1e3);

			final Map<String, double[]> postImpSpectrum = thisImpairments.getThird();
			final double post_powerPerChannel_dBm = OpticalImpairmentUtils.linear2dB(postImpSpectrum.get(OpticalImpairmentUtils.stSpectrum_powerPerChannel_W)[centralChannel] * 1e3);

			if (name.equalsIgnoreCase("OADM-ADD"))
			{
				/* Wavelengths in use within transponder range */
				if (channels_minChannelLambda_nm < tp_minWavelength_nm.getDouble())
					st += "Wavelength " + channels_minChannelLambda_nm + " nm is outside the transponder range [" + tp_minWavelength_nm.getDouble() + " nm, " + tp_maxWavelength_nm.getDouble() + " nm]";
				if (channels_maxChannelLambda_nm > tp_maxWavelength_nm.getDouble())
					st += "Wavelength " + channels_maxChannelLambda_nm + " nm is outside the transponder range [" + tp_minWavelength_nm.getDouble() + " nm, " + tp_maxWavelength_nm.getDouble() + " nm]";

				/* Output power within limits */
				if (Math.abs(post_powerPerChannel_dBm - OpticalImpairmentUtils.linear2dB(oadm_outputPowerPerChannel_W.getDouble() * 1e3)) > 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-ADD output is " + post_powerPerChannel_dBm + " dBm. It should be: " + OpticalImpairmentUtils.linear2dB(oadm_outputPowerPerChannel_W.getDouble() * 1e3);

			} else if (name.equalsIgnoreCase("OADM-EXPRESS"))
			{
				/* Input power within limits */
				if (pre_powerPerChannel_dBm < oadm_perChannelMinInputPower_dBm.getDouble() - 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-EXPRESS input is " + pre_powerPerChannel_dBm + " dBm. It should be between [" + oadm_perChannelMinInputPower_dBm.getDouble() + ", "
							+ oadm_perChannelMaxInputPower_dBm.getDouble() + "] dBm";
				if (pre_powerPerChannel_dBm > oadm_perChannelMaxInputPower_dBm.getDouble() + 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-EXPRESS input is " + pre_powerPerChannel_dBm + " dBm. It should be between [" + oadm_perChannelMinInputPower_dBm.getDouble() + ", "
							+ oadm_perChannelMaxInputPower_dBm.getDouble() + "] dBm";

				/* Output power within limits */
				if (Math.abs(post_powerPerChannel_dBm - OpticalImpairmentUtils.linear2dB(oadm_outputPowerPerChannel_W.getDouble() * 1e3)) > 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-EXPRESS output is " + post_powerPerChannel_dBm + " dBm. It should be: " + OpticalImpairmentUtils.linear2dB(oadm_outputPowerPerChannel_W.getDouble() * 1e3);

			} else if (name.equalsIgnoreCase("OADM-DROP"))
			{
				final double post_PMDSquared_ps2 = thisImpairments.getFourth();
				final Triple<double[], double[], double[]> post_OSNR_linear = OpticalImpairmentUtils.getOSNR(postImpSpectrum);

				/* Input power within limits */
				if (pre_powerPerChannel_dBm < oadm_perChannelMinInputPower_dBm.getDouble() - 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-DROP input is " + pre_powerPerChannel_dBm + " dBm. It should be between [" + oadm_perChannelMinInputPower_dBm.getDouble() + ","
							+ oadm_perChannelMaxInputPower_dBm.getDouble() + "] dBm";
				if (pre_powerPerChannel_dBm > oadm_perChannelMaxInputPower_dBm.getDouble() + 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-DROP input is " + pre_powerPerChannel_dBm + " dBm. It should be between [" + oadm_perChannelMinInputPower_dBm.getDouble() + ","
							+ oadm_perChannelMaxInputPower_dBm.getDouble() + "] dBm";

				/* Output power within limits */
				if (post_powerPerChannel_dBm < tp_inputPowerSensitivityMin_dBm.getDouble() - 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-DROP output is " + post_powerPerChannel_dBm + ". It should be between [" + tp_inputPowerSensitivityMin_dBm.getDouble() + "," + tp_inputPowerSensitivityMax_dBm.getDouble()
							+ "] dBm";
				if (post_powerPerChannel_dBm > tp_inputPowerSensitivityMax_dBm.getDouble() + 1E-3)
					st += "At " + initialPosition_km + "km: Power at the OADM-DROP output is " + post_powerPerChannel_dBm + ". It should be between [" + tp_inputPowerSensitivityMin_dBm.getDouble() + "," + tp_inputPowerSensitivityMax_dBm.getDouble()
							+ "] dBm";

				/* OSNR within limits */
				if (OpticalImpairmentUtils.linear2dB(post_OSNR_linear.getThird()[centralChannel]) < tp_minOSNR_dB.getDouble() + osnrPenalty_SUM_dB)
					st += "At " + initialPosition_km + "km: OSNR at the RECEIVER is " + OpticalImpairmentUtils.linear2dB(post_OSNR_linear.getThird()[centralChannel]) + " dB. It is below the tolerance plus margin " + tp_minOSNR_dB.getDouble()
							+ " dB + penalties " + osnrPenalty_SUM_dB + " dB = " + (tp_minOSNR_dB.getDouble() + osnrPenalty_SUM_dB) + " dB)";

				/* PMD tolerance at the receiver */
				final double pmdAtReceiver = Math.sqrt(post_PMDSquared_ps2);
				if (pmdAtReceiver > tp_pmdTolerance_ps.getDouble())
					st += "At " + initialPosition_km + "km: PMD at the RECEIVER is " + pmdAtReceiver + " ps. It is above the maximum PMD tolerance (" + tp_pmdTolerance_ps.getDouble() + " ps)";

			} else if (name.equalsIgnoreCase("SPAN"))
			{} else if (name.equalsIgnoreCase("EDFA"))
			{
				final double edfaGain_dB = elementData;

				/* Wavelengths within limits */
				if (channels_minChannelLambda_nm < edfa_minWavelength_nm.getDouble())
					st += "Wavelength " + channels_minChannelLambda_nm + " nm is outside the transponder range [" + edfa_minWavelength_nm.getDouble() + " nm, " + edfa_maxWavelength_nm.getDouble() + " nm]";
				if (channels_maxChannelLambda_nm > edfa_maxWavelength_nm.getDouble())
					st += "Wavelength " + channels_maxChannelLambda_nm + " nm is outside the transponder range [" + edfa_minWavelength_nm.getDouble() + " nm, " + edfa_maxWavelength_nm.getDouble() + " nm]";

				/* Gain within limits */
				if (edfaGain_dB < edfa_minGain_dB.getDouble() - 1E-3)
					st += "At " + initialPosition_km + "km: EDFA gain is " + edfaGain_dB + " dB. It should be between [" + edfa_minGain_dB.getDouble() + ", " + edfa_maxGain_dB.getDouble() + "] dB";
				if (edfaGain_dB > edfa_maxGain_dB.getDouble() + 1E-3)
					st += "At " + initialPosition_km + "km: EDFA gain is " + edfaGain_dB + " dB. It should be between [" + edfa_minGain_dB.getDouble() + ", " + edfa_maxGain_dB.getDouble() + "] dB";

				/* Input power within limits */
				if (pre_powerPerChannel_dBm < edfa_minInputPower_dBm.getDouble() - 1E-3)
					st += "At " + initialPosition_km + "km: Power at the EDFA input is (is one WDM channel) " + pre_powerPerChannel_dBm + " dBm. It should be between [" + edfa_minInputPower_dBm.getDouble() + ", " + edfa_maxInputPower_dBm.getDouble()
							+ "] dBm";
				if (pre_powerPerChannel_dBm + numChannels_dB > edfa_maxInputPower_dBm.getDouble() + 1E-3)
					st += "At " + initialPosition_km + "km: Power at the EDFA input is (if all WDM channels were active) " + (pre_powerPerChannel_dBm + numChannels_dB) + " dBm. It should be between [" + edfa_minInputPower_dBm.getDouble() + ","
							+ edfa_maxInputPower_dBm.getDouble() + "] dBm";

				/* Output power within limits */
				if (post_powerPerChannel_dBm < edfa_minOutputPower_dBm.getDouble() - 1E-3)
					st += "At " + initialPosition_km + "km: Power at the EDFA output is (is one WDM channel) " + post_powerPerChannel_dBm + " dBm. It should be between [" + edfa_minOutputPower_dBm.getDouble() + ", "
							+ edfa_maxOutputPower_dBm.getDouble() + "] dBm";
				if (post_powerPerChannel_dBm + numChannels_dB > edfa_maxOutputPower_dBm.getDouble() + 1E-3)
					st += "At " + initialPosition_km + "km: Power at the EDFA output is (if all WDM channels were active) " + (post_powerPerChannel_dBm + numChannels_dB) + " dBm. It should be between [" + edfa_minOutputPower_dBm.getDouble() + ", "
							+ edfa_maxOutputPower_dBm.getDouble() + "] dBm";
			} else if (name.equalsIgnoreCase("PC"))
			{} else
			{
				throw new RuntimeException("Unknown element type");
			}

			res.add(st);
		}

		return res;
	}

	/** Gets all the elements in the given link or lightpath
	 * 
	 * @param seqLinks list of links
	 * @return List of elements as a Quadruple object where: first is the position of the element (km), second the type of element, third the main parameter (i.e. OA=gain, PC=loss, SPAN=length, OADM=nodeID), fourth other information (i.e.
	 *         OA=noiseFigure, SPAN=fiberType, OADM=noiseFigure). */
	private List<Quadruple<Double, String, Double, String>> getElementPositionsListPerLightpath(List<Link> seqLinks)
	{
		final List<Quadruple<Double, String, Double, String>> res = new LinkedList<Quadruple<Double, String, Double, String>>();
		double currentDistanceFromRouteInit_km = 0;

		for (int index = 0; index < seqLinks.size(); index++)
		{
			final Link e = seqLinks.get(index);
			int oadmCounter = 0;

			final double d_e = e.getLengthInKm();
			final String st_edfaPositions_km = e.getAttribute("edfaPositions_km") == null ? "" : e.getAttribute("edfaPositions_km");
			final String st_edfaGains_dB = e.getAttribute("edfaGains_dB") == null ? "" : e.getAttribute("edfaGains_dB");
			final String st_edfaNoiseFigures_dB = e.getAttribute("edfaNoiseFigures_dB") == null ? "" : e.getAttribute("edfaNoiseFigures_dB");
			final String st_pcPositions_km = e.getAttribute("pcPositions_km") == null ? "" : e.getAttribute("pcPositions_km");
			final String st_pcLosses_dB = e.getAttribute("pcLosses_dB") == null ? "" : e.getAttribute("pcLosses_dB");
			final String st_oadmNoiseFigures_dB = e.getAttribute("oadmNoiseFigures_dB") == null ? "" : e.getAttribute("oadmNoiseFigures_dB");
			final String st_fiberTypes = e.getAttribute("fiberTypes") == null ? "" : e.getAttribute("fiberTypes");

			final double[] edfaPositions_km = StringUtils.toDoubleArray(StringUtils.split(st_edfaPositions_km));
			final double[] edfaGains_dB = StringUtils.toDoubleArray(StringUtils.split(st_edfaGains_dB));
			final double[] edfaNoiseFigures_dB = StringUtils.toDoubleArray(StringUtils.split(st_edfaNoiseFigures_dB));
			final double[] pcPositions_km = StringUtils.toDoubleArray(StringUtils.split(st_pcPositions_km));
			final double[] pcLosses_dB = StringUtils.toDoubleArray(StringUtils.split(st_pcLosses_dB));
			final double[] oadmNoiseFigures_dB = StringUtils.toDoubleArray(StringUtils.split(st_oadmNoiseFigures_dB));
			final String[] fiberTypes = StringUtils.split(st_fiberTypes);

			/* Basic checks */
			if (edfaPositions_km.length != edfaGains_dB.length)
				throw new Net2PlanException("Link: " + e + ". Number of elements in edfaPositions_km is not equal to the number of elements in edfaGains_dB");

			if (pcPositions_km.length != pcLosses_dB.length)
				throw new Net2PlanException("Link: " + e + ". Number of elements in pcPositions_km is not equal to the number of elements in pcLosses_dB");

			for (double edfaPosition : edfaPositions_km)
				if ((edfaPosition < 0) || (edfaPosition > d_e))
					throw new Net2PlanException("Link: " + e + ". Wrong OA position: " + edfaPosition + ", link length = " + d_e);

			for (double pcPosition : pcPositions_km)
				if ((pcPosition < 0) || (pcPosition > d_e))
					throw new Net2PlanException("Link: " + e + ". Wrong PC position: " + pcPosition + ", link length = " + d_e);

			for (double noiseFigure_dB : edfaNoiseFigures_dB)
				if ((noiseFigure_dB < Math.min(edfa_noiseFactorMinimumGain_dB.getDouble(), edfa_noiseFactorMaximumGain_dB.getDouble()))
						|| (noiseFigure_dB > Math.max(edfa_noiseFactorMinimumGain_dB.getDouble(), edfa_noiseFactorMaximumGain_dB.getDouble())))
					throw new RuntimeException("Bad EDFA Noise Factor, out of range");

			/* All links and lightpaths allways begin with an OADM-ADD */
			if (index == 0)
				if (oadmNoiseFigures_dB.length > 0)
					res.add(Quadruple.of(currentDistanceFromRouteInit_km, "OADM-ADD", (double) e.getOriginNode().getId(), oadmNoiseFigures_dB[oadmCounter++] + ""));
				else
					res.add(Quadruple.of(currentDistanceFromRouteInit_km, "OADM-ADD", (double) e.getOriginNode().getId(), oadm_addChannelNoiseFactor_dB.getDouble() + ""));

			/* Place in a sorted form the spans, PCs and EDFAS. If PC and EDFA placed, PC goes first */
			final double[] pcAndEDFAPositions_km = DoubleUtils.concatenate(pcPositions_km, edfaPositions_km);
			final int[] sortedPCAndEDFAPositionsIndexes = pcAndEDFAPositions_km.length == 0 ? new int[0] : DoubleUtils.sortIndexes(pcAndEDFAPositions_km, OrderingType.ASCENDING);
			double posKmLastElementThisLink_km = 0;
			int fiberSpans = 0;
			for (int cont = 0; cont < sortedPCAndEDFAPositionsIndexes.length; cont++)
			{
				final int indexInCommonArray = sortedPCAndEDFAPositionsIndexes[cont];
				final boolean isPC = (indexInCommonArray < pcPositions_km.length);
				final double posFromLinkInit_km = pcAndEDFAPositions_km[indexInCommonArray];
				final double previousSpanLength = (Math.abs(posFromLinkInit_km - posKmLastElementThisLink_km) < 1E-3) ? 0 : posFromLinkInit_km - posKmLastElementThisLink_km;

				if (previousSpanLength < 0)
					throw new RuntimeException("Bad");

				if (previousSpanLength > 0)
				{
					if (fiberSpans < fiberTypes.length)
						res.add(Quadruple.of(currentDistanceFromRouteInit_km, "SPAN", previousSpanLength, fiberTypes[fiberSpans++]));
					else
						res.add(Quadruple.of(currentDistanceFromRouteInit_km, "SPAN", previousSpanLength, fiber_default_type.getString()));
					currentDistanceFromRouteInit_km += previousSpanLength;
					posKmLastElementThisLink_km += previousSpanLength;
				}

				if (isPC)
					res.add(Quadruple.of(currentDistanceFromRouteInit_km, "PC", pcLosses_dB[indexInCommonArray], null));
				else if (edfaNoiseFigures_dB.length > 0 && edfaNoiseFigures_dB.length < (indexInCommonArray - pcPositions_km.length))
					res.add(Quadruple.of(currentDistanceFromRouteInit_km, "EDFA", edfaGains_dB[indexInCommonArray - pcPositions_km.length], edfaNoiseFigures_dB[indexInCommonArray - pcPositions_km.length] + ""));
				else
					res.add(Quadruple.of(currentDistanceFromRouteInit_km, "EDFA", edfaGains_dB[indexInCommonArray - pcPositions_km.length], edfa_default_noiseFactor_dB.getDouble()+""));
			}

			/* Last span of the link before the OADM */
			final double lastSpanOfLink_km = (Math.abs(d_e - posKmLastElementThisLink_km) < 1E-3) ? 0 : d_e - posKmLastElementThisLink_km;

			if (lastSpanOfLink_km < 0)
				throw new RuntimeException("Bad");

			if (lastSpanOfLink_km > 0)
			{
				if (fiberSpans < fiberTypes.length)
					res.add(Quadruple.of(currentDistanceFromRouteInit_km, "SPAN", lastSpanOfLink_km, fiberTypes[fiberSpans++]));
				else
					res.add(Quadruple.of(currentDistanceFromRouteInit_km, "SPAN", lastSpanOfLink_km, fiber_default_type.getString()));
				currentDistanceFromRouteInit_km += lastSpanOfLink_km;
				posKmLastElementThisLink_km += lastSpanOfLink_km;
			}

			/* OADM at the end of the link */
			final long endNodeLink = e.getDestinationNode().getId();
			final long lastLink = seqLinks.get(seqLinks.size() - 1).getId();
			if (e.getId() == lastLink)
				if (oadmCounter < oadmNoiseFigures_dB.length)
					res.add(Quadruple.of(currentDistanceFromRouteInit_km, "OADM-DROP", (double) endNodeLink, oadmNoiseFigures_dB[oadmCounter++] + ""));
				else
					res.add(Quadruple.of(currentDistanceFromRouteInit_km, "OADM-DROP", (double) endNodeLink, oadm_dropChannelNoiseFactor_dB.getDouble() + ""));
			else if (oadmCounter < oadmNoiseFigures_dB.length)
				res.add(Quadruple.of(currentDistanceFromRouteInit_km, "OADM-EXPRESS", (double) endNodeLink, oadmNoiseFigures_dB[oadmCounter++] + ""));
			else
				res.add(Quadruple.of(currentDistanceFromRouteInit_km, "OADM-EXPRESS", (double) endNodeLink, oadm_expressChannelNoiseFactor_dB.getDouble() + ""));

		}

		/* Check current distance equals the sum of the traversed links */
		double sumLinks = 0;
		for (Link e : seqLinks)
			sumLinks += e.getLengthInKm();

		if (Math.abs(sumLinks - currentDistanceFromRouteInit_km) > 1E-3)
			throw new RuntimeException("Bad");
		return res;
	}

	private String printReport(Map<Link, List<Quadruple<Double, String, Double, String>>> elements_e, Map<Link, List<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>>> impairments_e, Map<Link, List<String>> warnings_e,
			Map<Route, List<Quadruple<Double, String, Double, String>>> elements_r, Map<Route, List<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>>> impairments_r, Map<Route, List<String>> warnings_r)
	{
		final StringBuilder out = new StringBuilder();
		final DecimalFormat df_2 = new DecimalFormat("###.##");
		final int centralChannel = Math.floorDiv(gn_spec_nCh.getInt(), 2);

		out.append("<html><body>");
		out.append("<head><title>WDM line engineering in multilayer (lightpath based) networks with GN calculations</title></head>");
		out.append("<h1>WDM line engineering report for lighptath-based networks with GN calculations</h1>");

		out.append(
				"<h3>This report shows line engineering information for WDM links in a multilayer optical network. " + "The impairment calculations are based on the Gaussian Noise Model developed by Politecnico di Torino and their analytic formula."
						+ " Other calculations are inspired in the procedures described in the 2009 ITU-T WDM manual  \"Optical fibres, cabbles and systems\".</h3>");

		out.append("<p>The report assumes that the WDM network follows the scheme:</p>");

		out.append("<ul>");
		out.append("<li>In the net2plan object, nodes are OADMs, links are fiber links, and routes are lightpaths: " + "WDM channels optically switched at intermediate nodes. </li>");
		out.append("<li>Nodes are connected by unidirectional fiber links. Fiber link distance is given by the link " + "length. Other specifications are given by fibers_XXX input parameters, each one describing the "
				+ "parameter for the fiber types specified in fibers_types, in the same order and separated by " + "spaces. The fiber can be split into spans if optical amplifers (EDFAs) and/or passive components "
				+ "(PCs) are placed along the fiber. These spans can be of different fiber types as long as they are " + "described in a link attribute called \"fiberTypes\". Must be separated by spaces and, in case that "
				+ "there were more spans than elements of the attribute, the default type given in \"fiber_default_type\" " + "would be used.</li>");
		out.append("<li>Optical line amplifiers (EDFAs) can be located in none, one or more positions in the fiber" + " link, separating them in different spans. EDFAs are supposed to operate in the automatic gain"
				+ " control mode. Thus, the gain is the same, whatever the number of input WDM channels. EDFA" + " positions (as distance\" in km from the link start to the EDFA location), EDFA gains (assumed in"
				+ " dB) and EDFA noise figures (in dB) are read from the \"edfaPositions_km\", \"edfaGains_dB\" and" + " \"edfaNoiseFigures_dB\" attributes of the links. The format of all attributes are the same: a"
				+ " string of numbers separated by spaces. The <i>i</i>-th number corresponding to the position/gain" + " of the <i>i</i>-th EDFA. If the attributes do not exist, it is assumed that no EDFAs are placed in this link. "
				+ "EDFA specifications are given by \"edfa_XXX\" parameters</li>");
		out.append("<li>There are not Dispersion compensating modules (DCMs) in the topoology, since the Gaussian Noise Model is used.</li>");
		out.append("<li>Passive components are described by the link attributes \"pcPositions_km\" and \"pcLosses_dB\"." + " The <i>i</i>-th number corresponding to the position/loss of the <i>i</i>-th PC. If the"
				+ " attributes do not exist, it is assumed that no PCs are placed in this link. Other specifications for Passive Components" + " will be described in teh pc_XXX input parameters.</li>");
		out.append("<li>Fiber links start and end in OADM modules, that permit adding, dropping and optically switch" + " individual WDM channels. OADMs have a pre-amplifier (traversed by drop and express channels) and"
				+ " a boost amplifier (traversed by add and express channels). They are supposed to equalize the" + " channel power at their outputs, to a fixed value (added and express channels will thus have the"
				+ " same power in the fibers). Also, OADMs attenuate appropriately the optical signal coming from the" + " pre-amplifier, in the drop channels, so that they fall within the receiver sensitivity range."
				+ " OADM noise figures for add, drop and express channels are given as input parameters. PMD values" + " for add, drop and express channels are computed assumming that: (i) add channel traverse a"
				+ " multiplexer and the booster, (ii) drop channels travese the pre-amplifier and a demultiplexer," + " (iii) express channels traverse the two amplifiers. The required parameters are provided in oadm_XXX parameters.</li>");
		out.append("<li>Each channel ends in a receiver, with specifications given by \"tp_XXX\" parameters.</li>");
		out.append("</ul></p>");
		out.append("<p>The basic checks performed are:</p>");
		out.append("<ul>");
		out.append("<li>For each link, signal power levels are within operating ranges at the oadm/edfas, both when the link has one single active channel, or when all the");
		out.append("\"gn_spec_nCh\" are active</li>");
		out.append("<li>For each route (lightpath), OSNR (Optical Signal to Noise Ration) is within the operating range at the receiver.");
		out.append("A set of margins are considered to account to several not directly considered impairments. </li>");
		out.append("<li>For each route (lightpath), PMD (Polarization mode dispersion) is within the operating range at the receiver</li>");
		out.append("</ul></p>");

		out.append("<h2>Input Parameters</h2>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Name</b></th><th><b>Value</b></th><th><b>Description</b></th>");

		for (Triple<String, String, String> paramDef : getParameters())
		{
			final String name = paramDef.getFirst();
			final String description = paramDef.getThird();
			final String value = reportParameters.get(name);
			out.append("<tr><td>").append(name).append("</td><td>").append(value).append("</td><td>").append(description).append("</td></tr>");
		}
		out.append("</table>");

		out.append("<h2>PER LINK INFORMATION SUMMARY - Signal metrics at the input of end OADM</h2>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Link #</b></th><th><b>Length (km)</b></th><th><b># EDFAs</b></th><th><b># PCs</b></th><th><b>OSNR total (dB)</b></th>"
				+ "<th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th><th><b>Warnings</b></th></tr>");

		for (Link e : netPlan.getLinks())
		{
			final double d_e = e.getLengthInKm();
			final String st_a_e = e.getOriginNode().getName();
			final String st_b_e = e.getDestinationNode().getName();
			final List<Quadruple<Double, String, Double, String>> el = elements_e.get(e);
			final List<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>> imp = impairments_e.get(e);
			final List<String> w = warnings_e.get(e);

			int numEDFAs = 0;
			for (Quadruple<Double, String, Double, String> t : el)
				if (t.getSecond().equalsIgnoreCase("EDFA"))
					numEDFAs++;

			int numPCs = 0;
			for (Quadruple<Double, String, Double, String> t : el)
				if (t.getSecond().equalsIgnoreCase("PC"))
					numPCs++;

			final Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double> impInfoInputOADM = imp.get(imp.size() - 1);
			final Map<String, double[]> prevSpectrum = impInfoInputOADM.getFirst();
			final double totalOSNR_dB = OpticalImpairmentUtils.getOSNR(prevSpectrum).getThird()[centralChannel];

			final StringBuilder warnings = new StringBuilder();
			for (String s : w)
				warnings.append(s);

			out.append("<tr><td>").append(e).append(" (").append(st_a_e).append(" --> ").append(st_b_e).append(") </td><td>").append(df_2.format(d_e)).append("</td><td>").append(numEDFAs).append("</td><td>").append(numPCs).append("</td><td>")
					.append((totalOSNR_dB > infinityThreshold_dB) ? "&infin;" : df_2.format(totalOSNR_dB)).append("</td><td>")
					.append(df_2.format(OpticalImpairmentUtils.linear2dB(prevSpectrum.get(OpticalImpairmentUtils.stSpectrum_powerPerChannel_W)[centralChannel] * 1e3))).append("</td><td>").append(df_2.format(Math.sqrt(impInfoInputOADM.getSecond())))
					.append("</td><td>").append(warnings).append("</td></tr>");
		}
		out.append("</table>");

		out.append("<h2>PER ROUTE INFORMATION SUMMARY - Signal metrics at the input of last OADM</h2>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Route #</b></th><th><b>Length (km)</b></th><th><b># EDFAs</b></th><th><b># PCs</b></th>"
				+ "<th><b>OSNR total (dB)</b></th><th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th><th><b>Warnings</b></th></tr>");
		for (Route r : netPlan.getRoutes())
		{
			final double d_r = r.getLengthInKm();
			final String st_a_r = r.getIngressNode().getName();
			final String st_b_r = r.getEgressNode().getName();
			final List<Quadruple<Double, String, Double, String>> el = elements_r.get(r);
			final List<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>> imp = impairments_r.get(r);
			final List<String> w = warnings_r.get(r);

			int numEDFAs = 0;
			for (Quadruple<Double, String, Double, String> t : el)
				if (t.getSecond().equalsIgnoreCase("EDFA"))
					numEDFAs++;

			int numPCs = 0;
			for (Quadruple<Double, String, Double, String> t : el)
				if (t.getSecond().equalsIgnoreCase("PC"))
					numPCs++;

			final Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double> impInfoInputOADM = imp.get(imp.size() - 1);
			final Map<String, double[]> preSpectrum = impInfoInputOADM.getFirst();
			final double totalOSNR_dB = OpticalImpairmentUtils.getOSNR(preSpectrum).getThird()[centralChannel];

			final StringBuilder warnings = new StringBuilder();
			for (String s : w)
				warnings.append(s);

			out.append("<tr><td>").append(r).append(" (").append(st_a_r).append(" --> ").append(st_b_r).append(") </td><td>").append(df_2.format(d_r)).append("</td><td>").append(numEDFAs).append("</td><td>").append(numPCs).append("</td><td>")
					.append((totalOSNR_dB > infinityThreshold_dB) ? "&infin;" : df_2.format(totalOSNR_dB)).append("</td><td>")
					.append(df_2.format(OpticalImpairmentUtils.linear2dB(preSpectrum.get(OpticalImpairmentUtils.stSpectrum_powerPerChannel_W)[centralChannel] * 1e3))).append("</td><td>").append(df_2.format(Math.sqrt(impInfoInputOADM.getSecond())))
					.append("</td><td>").append(warnings.toString()).append("</td>" + "</tr>");

		}
		out.append("</table>");

		out.append("<h2>PER-LINK DETAILED INFORMATION </h2>");
		out.append("<p>Number of links: ").append(netPlan.getNumberOfLinks()).append("</p>");

		for (Link e : netPlan.getLinks())
		{
			final double d_e = e.getLengthInKm();
			final String st_a_e = e.getOriginNode().getName();
			final String st_b_e = e.getDestinationNode().getName();
			final List<Quadruple<Double, String, Double, String>> el = elements_e.get(e);
			final List<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>> imp = impairments_e.get(e);
			final List<String> w = warnings_e.get(e);
			final String st_edfaPositions_km = e.getAttribute("edfaPositions_km") == null ? "" : e.getAttribute("edfaPositions_km");
			final String st_edfaGains_dB = e.getAttribute("edfaGains_dB") == null ? "" : e.getAttribute("edfaGains_dB");
			final String st_edfaNoiseFigures_dB = e.getAttribute("edfaNoiseFigures_dB") == null ? "" : e.getAttribute("edfaNoiseFigures_dB");
			final String st_pcPositions_km = e.getAttribute("pcPositions_km") == null ? "" : e.getAttribute("pcPositions_km");
			final String st_pcLosses_dB = e.getAttribute("pcLosses_dB") == null ? "" : e.getAttribute("pcLosses_dB");

			out.append("<h3>LINK # ").append(e).append(" (").append(st_a_e).append(" --> ").append(st_b_e).append(")</h3>");
			out.append("<table border=\"1\">");
			out.append("<caption>Link information</caption>");
			out.append("<tr><td>Link length (km)</td><td>").append(d_e).append("</td></tr>");
			out.append("<tr><td>EDFA positions (km)</td><td>").append(st_edfaPositions_km).append("</td></tr>");
			out.append("<tr><td>EDFA gains (dB)</td><td>").append(st_edfaGains_dB).append("</td></tr>");
			out.append("<tr><td>EDFA Noise Figures (dB)</td><td>").append(st_edfaNoiseFigures_dB).append("</td></tr>");
			out.append("<tr><td>PC positions (km)</td><td>").append(st_pcPositions_km).append("</td></tr>");
			out.append("<tr><td>PC losses (dB)</td><td>").append(st_pcLosses_dB).append("</td></tr>");
			out.append("</table>");

			out.append("<table border=\"1\">");
			out.append("<caption>Signal metrics evolution at the output of each element.</caption>");
			out.append("<tr><th><b>Position (km)</b></th><th><b>Position (description)</b></th><th><b>OSNR total(dB)</b></th>"
					+ "<th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th><th><b>Warnings</b></th></tr>");

			final Iterator<Quadruple<Double, String, Double, String>> it_el = el.iterator();
			final Iterator<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>> it_imp = imp.iterator();
			final Iterator<String> it_w = w.iterator();
			while (it_el.hasNext())
			{
				final Quadruple<Double, String, Double, String> this_el = it_el.next();
				final Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double> this_imp = it_imp.next();
				final String this_warnings = it_w.next();

				final double pos_km = this_el.getFirst();
				String elementType = this_el.getSecond();
				final double elementData = this_el.getThird();
				final String elementAuxData = this_el.getFourth();

				final Map<String, double[]> postSpectrum = this_imp.getThird();
				final double totalOSNR_dB = OpticalImpairmentUtils.getOSNR(postSpectrum).getThird()[centralChannel];

				if (elementType.equalsIgnoreCase("EDFA"))
					elementType += " (G: " + elementData + " dB, F: " + elementAuxData + " dB)";
				else if (elementType.equalsIgnoreCase("SPAN"))
					elementType += " (Type: " + elementAuxData + ", " + elementData + " km)";
				else if (elementType.equalsIgnoreCase("PC"))
					elementType += " (L: " + elementData + " dB)";

				out.append("<tr><td>").append(df_2.format(pos_km)).append("</td><td>" + "Output of ").append(elementType).append("</td><td>").append((totalOSNR_dB > infinityThreshold_dB) ? "&infin;" : df_2.format(totalOSNR_dB)).append("</td><td>")
						.append(df_2.format(OpticalImpairmentUtils.linear2dB(postSpectrum.get(OpticalImpairmentUtils.stSpectrum_powerPerChannel_W)[centralChannel] * 1e3))).append("</td><td>").append(df_2.format(Math.sqrt(this_imp.getSecond())))
						.append("</td><td>").append(this_warnings).append("</td>" + "</tr>");

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
			final List<Quadruple<Double, String, Double, String>> el = elements_r.get(r);
			final List<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>> imp = impairments_r.get(r);
			final List<String> w = warnings_r.get(r);

			out.append("<h3>ROUTE # ").append(r).append(" (").append(st_a_r).append(" --> ").append(st_b_r).append("), Length: ").append(d_r).append(" km</h3>");
			out.append("<table border=\"1\">");
			out.append("<caption>Signal metrics evolution</caption>");
			out.append("<tr><th><b>Position (km)</b></th><th><b>Position (description)</b></th><th><b>OSNR total (dB)</b></th>"
					+ "<th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th><th><b>Warnings</b></th></tr>");

			final Iterator<Quadruple<Double, String, Double, String>> it_el = el.iterator();
			final Iterator<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>> it_imp = imp.iterator();
			final Iterator<String> it_w = w.iterator();
			while (it_el.hasNext())
			{
				final Quadruple<Double, String, Double, String> this_el = it_el.next();
				final Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double> this_imp = it_imp.next();
				final String this_warnings = it_w.next();

				final double pos_km = this_el.getFirst();
				String elementType = this_el.getSecond();
				final double elementData = this_el.getThird();
				final String elementAuxData = this_el.getFourth();

				final Map<String, double[]> postSpectrum = this_imp.getThird();
				final double totalOSNR_dB = OpticalImpairmentUtils.getOSNR(postSpectrum).getThird()[centralChannel];

				if (elementType.equalsIgnoreCase("EDFA"))
					elementType += " (G: " + elementData + " dB, NF: " + elementAuxData + " dB)";
				else if (elementType.equalsIgnoreCase("SPAN"))
					elementType += " (Type: " + elementAuxData + ", l:" + elementData + " km)";
				else if (elementType.equalsIgnoreCase("PC"))
					elementType += " (L: " + elementData + " dB)";

				out.append("<tr><td>").append(df_2.format(pos_km)).append("</td><td>" + "Output of ").append(elementType).append("</td><td>").append((totalOSNR_dB > infinityThreshold_dB) ? "&infin;" : df_2.format(totalOSNR_dB)).append("</td><td>")
						.append(df_2.format(OpticalImpairmentUtils.linear2dB(postSpectrum.get(OpticalImpairmentUtils.stSpectrum_powerPerChannel_W)[centralChannel] * 1e3))).append("</td><td>").append(df_2.format(Math.sqrt(this_imp.getSecond())))
						.append("</td><td>").append(this_warnings).append("</td>" + "</tr>");

			}

			out.append("</table>");
		}

		out.append("</body></html>");
		return out.toString();
	}

}
