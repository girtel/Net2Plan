package com.net2plan.libraries;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Quadruple;
import com.net2plan.utils.Triple;

/**
 * @author Elena Martin-Seoane
 * @version 1.0, November 2017
 */
public class OpticalImpairmentUtils
{

	/** Plank constant in m^2 kg/sec */
	public final static double constant_h = 6.62607004E-34;
	
	/** speed of light in m/s */
	public final static double constant_c = 299792458;

	/* String keys for spectrum Map */
 	public final static String stSpectrum_powerPerChannel_W = "p_ch";
	public final static String stSpectrum_nliNoisePowerg_W = "p_nli";
	public final static String stSpectrum_aseNoisePower_W = "p_ase";
	public final static String stSpectrum_bandwidthPerChannel_THz = "b_eq";

	/* String keys of fiber specs Map */
	public final static String stFiber_alpha_dB_per_km = "alpha";
	public final static String stFiber_alpha1st_dB_per_km_per_THz = "alpha_1st";
	public final static String stFiber_beta2_ps2_per_km = "beta2";
	public final static String stFiber_effectiveArea_um2 = "Aeff";
	public final static String stFiber_n2Coeff_m2_per_W = "n2";

	private OpticalImpairmentUtils()
	{}

	/**
	 * Computes linear, non-linear and total OSNR from the data of the given spectrum
	 * 
	 * @param spectrum (Map with keys: p_ch, p_ase, p_nli)
	 * @return Triple where 1st: linear OSNR per channel,
	 *         2nd: non-linear OSNR per channel,
	 *         3rd: total OSNR per channel
	 */
	public static Triple<double[], double[], double[]> getOSNR(Map<String, double[]> spectrum)
	{

		/* GN Linear and Non-Linear OSNR */
		final double[] linearOSNR_perChannel_dB = linear2dB(DoubleUtils.divide(spectrum.get(stSpectrum_powerPerChannel_W), spectrum.get(stSpectrum_aseNoisePower_W)));
		final double[] nonLinearOSNR_perChannel_dB = linear2dB(
				DoubleUtils.divide(spectrum.get(stSpectrum_powerPerChannel_W), DoubleUtils.sum(spectrum.get(stSpectrum_aseNoisePower_W), spectrum.get(stSpectrum_nliNoisePowerg_W))));

		/* Total OSNR */
		double[] totalOSNR_perChannel_dB = new double[linearOSNR_perChannel_dB.length];
		for (int i = 0; i < linearOSNR_perChannel_dB.length; i++)
			totalOSNR_perChannel_dB[i] = linear2dB(1 / (1 / (dB2linear(linearOSNR_perChannel_dB[i])) + 1 / (dB2linear(nonLinearOSNR_perChannel_dB[i]))));

		return new Triple<>(linearOSNR_perChannel_dB, nonLinearOSNR_perChannel_dB, totalOSNR_perChannel_dB, false);
	}

	/**
	 * Computes the non-linear impairments of a fiber span. Calculations are from the analytic formula of GN-model.
	 * 
	 * @param fiberBeta2_ps2_per_km the dispersion coefficient [ps^2/km]
	 * @param fiberAlpha_dB_per_km the attenuation coefficient [dB/km]
	 * @param fiberAlpha1st_dB_per_km_per_THz alpha slope [dB/km/THz]
	 * @param fiberN2_m2_per_W second-order nonlinear refractive index [m^2/W]
	 * @param fiberEffectiveArea_um2 the effective area of the fiber [um^2]
	 * @param spanLength_km the fiber span length [km]
	 * @param bandwidthPerChannel_THz the bandwidth per channel [THz]
	 * @param centralFrequency_THz the central channel frequency [THz]
	 * @param frequenciesPerChannel_THz the frequency per channel [THz]
	 * @param powerPerChannel_W the power per channel [W]
	 * @return power spectral density of NLI in this span [W/THz]
	 */
	private static double[] computeNLIfiber(double fiberBeta2_ps2_per_km, double fiberAlpha_dB_per_km, double fiberAlpha1st_dB_per_km_per_THz, double fiberN2_m2_per_W, double fiberEffectiveArea_um2,
			double spanLength_km, double[] bandwidthPerChannel_THz, double[] powerPerChannel_W, double centralFrequency_THz, double[] frequenciesPerChannel_THz)
	{

		final int numChannels = frequenciesPerChannel_THz.length;
		final double alpha_linear = fiberAlpha_dB_per_km / 20 / Math.log10(Math.E);
		final double asymptoticEffectiveLength_km = 1 / (2 * alpha_linear);
		final double effectiveLength_km = (1 - Math.exp(-2 * alpha_linear * spanLength_km)) / (2 * alpha_linear);
		final double gamma_per_W_per_km = (2 * Math.PI) * (centralFrequency_THz / constant_c) * (fiberN2_m2_per_W / fiberEffectiveArea_um2) * 1e27;
		final double powerSpectralDensityPerChannel_W_per_THz[] = DoubleUtils.divide(powerPerChannel_W, bandwidthPerChannel_THz);

		final double attenuation_linear[] = getAttenuationCompLinear(fiberAlpha_dB_per_km, fiberAlpha1st_dB_per_km_per_THz, spanLength_km, frequenciesPerChannel_THz);

		double nliComputed[] = new double[numChannels];
		for (int n = 0; n < numChannels; n++)
		{
			double sum = 0;
			for (int i = 0; i < numChannels; i++)
			{
				double psi = 0;
				if (n == i)
					psi = asinh(0.5 * Math.PI * Math.PI * asymptoticEffectiveLength_km * Math.abs(fiberBeta2_ps2_per_km) * bandwidthPerChannel_THz[n] * bandwidthPerChannel_THz[n]);
				else
					psi = asinh(Math.PI * Math.PI * asymptoticEffectiveLength_km * Math.abs(fiberBeta2_ps2_per_km) * bandwidthPerChannel_THz[n]
							* (frequenciesPerChannel_THz[n] - frequenciesPerChannel_THz[i] + 0.5 * bandwidthPerChannel_THz[i]))
							- asinh(Math.PI * Math.PI * asymptoticEffectiveLength_km * Math.abs(fiberBeta2_ps2_per_km) * bandwidthPerChannel_THz[n]
									* (frequenciesPerChannel_THz[n] - frequenciesPerChannel_THz[i] - 0.5 * bandwidthPerChannel_THz[i]));

				sum += powerSpectralDensityPerChannel_W_per_THz[i] * powerSpectralDensityPerChannel_W_per_THz[n] * powerSpectralDensityPerChannel_W_per_THz[n] * psi;
			}

			nliComputed[n] = sum * (16.0 / 27.0) * (gamma_per_W_per_km * effectiveLength_km) * (gamma_per_W_per_km * effectiveLength_km)
					/ (2 * Math.PI * Math.abs(fiberBeta2_ps2_per_km) * asymptoticEffectiveLength_km);

		}

		nliComputed = DoubleUtils.mult(nliComputed, attenuation_linear);

		return DoubleUtils.mult(nliComputed, bandwidthPerChannel_THz);
	}

	/**
	 * Gets the linear attenuation
	 * 
	 * @param fiberAlpha_dB_per_km the attenuation coefficient [dB/km]
	 * @param fiberAlpha1st_dB_per_km_per_THz alpha slope [dB/km/THz]
	 * @param spanLength_km the fiber span length [km]
	 * @param frequenciesPerChannel_THz the frequency per channel [THz]
	 * @return attenuation per channel [linear]
	 */
	private static double[] getAttenuationCompLinear(double fiberAlpha_dB_per_km, double fiberAlpha1st_dB_per_km_per_THz, double spanLength_km, double[] frequenciesPerChannel_THz)
	{
		double[] att = new double[frequenciesPerChannel_THz.length];

		for (int i = 0; i < frequenciesPerChannel_THz.length; i++)
		{
			final double att_dB = fiberAlpha_dB_per_km * spanLength_km + frequenciesPerChannel_THz[i] * fiberAlpha1st_dB_per_km_per_THz * spanLength_km;
			att[i] = Math.pow(10, -Math.abs(att_dB) / 10);
		}

		return att;
	}

	/**
	 * Computes OSNRs and power per channel with the GN formula and PMD according to Net2Plan calculations.
	 *
	 * @param linkElements List of Quadruples where 1st: position (km); 2nd: Type; 3rd: data; 4th: auxData
	 * @param spectrumParameters Map with keys stSpectrum_XX
	 * @param fibersParameters Map with key fiberType, and value a Map of (stFiber_XXX, paramValue)
	 * @param oadm_perChannelOutputPower_W output power for all OADMs (per channel)
	 * @param fiber_PMD_ps_per_sqroot_km PMD fiber coefficient
	 * @param edfa_PMD edfa PMD coefficient
	 * @param pc_PMD PC PMD coefficient
	 * @param oadm_muxDemuxPMD_ps mux OADM PMD coefficient
	 * @param oadm_preAmplifierPMD_ps pre-amplifier OADM PMD coefficient
	 * @param oadm_boosterPMD_ps booster OAMD PMD coefficient
	 * @param frequenciesPerChannel_THz array with the frequencies of each channel
	 * @param centralFrequency_THz the central frequency of the used spectrum
	 * @param tp_inputPowerSensitivityMin_dBm minimum input power of the final transponder
	 * @param tp_inputPowerSensitivityMax_dBm maximum input power of the final transponder
	 * @return a list of elements with a Quadruple:
	 *         1st element is the power per channel before the element,
	 *         2nd, the PMD^2 Net2Plan result before the element,
	 *         3rd, the GN-model spectrum parameters after the element,
	 *         4th, the PMD^2 Net2Plan result after the element,
	 */
	public static List<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>> computeImpairments(List<Quadruple<Double, String, Double, String>> linkElements,
			Map<String, double[]> spectrumParameters, Map<String, Map<String, Double>> fibersParameters, double oadm_perChannelOutputPower_W, double fiber_PMD_ps_per_sqroot_km, double edfa_PMD,
			double pc_PMD, double oadm_muxDemuxPMD_ps, double oadm_preAmplifierPMD_ps, double oadm_boosterPMD_ps, double[] frequenciesPerChannel_THz, double centralFrequency_THz,
			double tp_inputPowerSensitivityMin_dBm, double tp_inputPowerSensitivityMax_dBm)
	{
		final List<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>> res = new LinkedList<Quadruple<Map<String, double[]>, Double, Map<String, double[]>, Double>>();
		final int numChannels = spectrumParameters.get(stSpectrum_powerPerChannel_W).length;

		/* In the transmitter */
		double current_PMDSquared_ps2 = 0;

		for (Quadruple<Double, String, Double, String> element : linkElements)
		{
			final String name = element.getSecond();
			final double elementData = element.getThird();
			final String elementInfo = element.getFourth();

			final Map<String, double[]> prevSpectrum = Maps.newHashMap(spectrumParameters);
			final double prevPMDSquared_ps2 = current_PMDSquared_ps2;

			if (name.equalsIgnoreCase("OADM-ADD"))
			{
				final double noiseFigure_dB = Double.parseDouble(elementInfo);
				
				final Triple<double[], double[], double[]> outputPowers = getSpectrumAfterOADM(oadm_perChannelOutputPower_W, spectrumParameters.get(stSpectrum_powerPerChannel_W),
						spectrumParameters.get(stSpectrum_nliNoisePowerg_W), spectrumParameters.get(stSpectrum_aseNoisePower_W), noiseFigure_dB, centralFrequency_THz,
						frequenciesPerChannel_THz, spectrumParameters.get(stSpectrum_bandwidthPerChannel_THz));

				/* Update spectrum */
				spectrumParameters.put(stSpectrum_powerPerChannel_W, outputPowers.getFirst());
				spectrumParameters.put(stSpectrum_nliNoisePowerg_W, outputPowers.getSecond());
				spectrumParameters.put(stSpectrum_aseNoisePower_W, outputPowers.getThird());

				/* Net2Plan calculations */
				current_PMDSquared_ps2 = Math.pow(oadm_muxDemuxPMD_ps, 2) + Math.pow(oadm_boosterPMD_ps, 2);

			} else if (name.equalsIgnoreCase("OADM-EXPRESS"))
			{

				final double noiseFigure_dB = Double.parseDouble(elementInfo);
				
				final Triple<double[], double[], double[]> outputPowers = getSpectrumAfterOADM(oadm_perChannelOutputPower_W, spectrumParameters.get(stSpectrum_powerPerChannel_W),
						spectrumParameters.get(stSpectrum_nliNoisePowerg_W), spectrumParameters.get(stSpectrum_aseNoisePower_W), noiseFigure_dB, centralFrequency_THz,
						frequenciesPerChannel_THz, spectrumParameters.get(stSpectrum_bandwidthPerChannel_THz));

				/* Update spectrum */
				spectrumParameters.put(stSpectrum_powerPerChannel_W, outputPowers.getFirst());
				spectrumParameters.put(stSpectrum_nliNoisePowerg_W, outputPowers.getSecond());
				spectrumParameters.put(stSpectrum_aseNoisePower_W, outputPowers.getThird());

				/* Net2Plan calculations */
				current_PMDSquared_ps2 += Math.pow(oadm_preAmplifierPMD_ps, 2) + Math.pow(oadm_boosterPMD_ps, 2);

			} else if (name.equalsIgnoreCase("OADM-DROP"))
			{
				final double noiseFigure_dB = Double.parseDouble(elementInfo);
				final double targetOutputPower_W = dB2linear((tp_inputPowerSensitivityMin_dBm + tp_inputPowerSensitivityMax_dBm) / 2) * 1e-3;
				
				final Triple<double[], double[], double[]> outputPowers = getSpectrumAfterOADM(targetOutputPower_W, spectrumParameters.get(stSpectrum_powerPerChannel_W),
						spectrumParameters.get(stSpectrum_nliNoisePowerg_W), spectrumParameters.get(stSpectrum_aseNoisePower_W), noiseFigure_dB, centralFrequency_THz,
						frequenciesPerChannel_THz, spectrumParameters.get(stSpectrum_bandwidthPerChannel_THz));

				/* Update spectrum */
				spectrumParameters.put(stSpectrum_powerPerChannel_W, outputPowers.getFirst());
				spectrumParameters.put(stSpectrum_nliNoisePowerg_W, outputPowers.getSecond());
				spectrumParameters.put(stSpectrum_aseNoisePower_W, outputPowers.getThird());

				/* Net2Plan calculations */
				current_PMDSquared_ps2 += Math.pow(oadm_preAmplifierPMD_ps, 2);

			} else if (name.equalsIgnoreCase("SPAN"))
			{
				final double spanLength_km = elementData;
				final String fiberType = elementInfo;
				final Map<String, Double> fiberParameters = fibersParameters.get(fiberType);

				final double this_alpha = fiberParameters.get(stFiber_alpha_dB_per_km);
				final double this_alpha1st = fiberParameters.get(stFiber_alpha1st_dB_per_km_per_THz);
				final double this_beta2 = fiberParameters.get(stFiber_beta2_ps2_per_km);
				final double this_n2 = fiberParameters.get(stFiber_n2Coeff_m2_per_W);
				final double this_aeff = fiberParameters.get(stFiber_effectiveArea_um2);
				double[] powerPerChannel_W = spectrumParameters.get(stSpectrum_powerPerChannel_W);
				double[] nliNoisePower_W = spectrumParameters.get(stSpectrum_nliNoisePowerg_W);
				double[] aseNoisePower_W = spectrumParameters.get(stSpectrum_aseNoisePower_W);

				/* GN calculations */
				final double[] lin_att = getAttenuationCompLinear(this_alpha, this_alpha1st, spanLength_km, frequenciesPerChannel_THz);

				powerPerChannel_W = DoubleUtils.mult(powerPerChannel_W, lin_att);
				nliNoisePower_W = DoubleUtils.mult(nliNoisePower_W, lin_att);
				aseNoisePower_W = DoubleUtils.mult(aseNoisePower_W, lin_att);

				final double[] nliNoisePowerThisSpan_W = computeNLIfiber(this_beta2, this_alpha, this_alpha1st, this_n2, this_aeff, spanLength_km,
						spectrumParameters.get(stSpectrum_bandwidthPerChannel_THz), spectrumParameters.get(stSpectrum_powerPerChannel_W), this_aeff, nliNoisePower_W);
				nliNoisePower_W = DoubleUtils.sum(nliNoisePower_W, nliNoisePowerThisSpan_W);

				/* Update spectrum */
				spectrumParameters.put(stSpectrum_powerPerChannel_W, powerPerChannel_W);
				spectrumParameters.put(stSpectrum_nliNoisePowerg_W, nliNoisePower_W);
				spectrumParameters.put(stSpectrum_aseNoisePower_W, aseNoisePower_W);

				/* Net2Plan calculations */
				current_PMDSquared_ps2 += spanLength_km * Math.pow(fiber_PMD_ps_per_sqroot_km, 2);

			} else if (name.equalsIgnoreCase("EDFA"))
			{
				final double edfaGain_dB = elementData;
				final double noiseFigure_dB = Double.parseDouble(elementInfo);

				final double[] eqBandwidthPerChannel_THz = spectrumParameters.get(stSpectrum_bandwidthPerChannel_THz);
				double[] powerPerChannel_W = spectrumParameters.get(stSpectrum_powerPerChannel_W);
				double[] nliNoisePower_W = spectrumParameters.get(stSpectrum_nliNoisePowerg_W);
				double[] aseNoisePower_W = spectrumParameters.get(stSpectrum_aseNoisePower_W);

				/* GN calculations */
				final double aux = (dB2linear(edfaGain_dB) - 1) * dB2linear(noiseFigure_dB) * constant_h * 1e24;
				final double[] centralFrequency_array_THz = new double[numChannels];
				Arrays.fill(centralFrequency_array_THz, centralFrequency_THz);
				final double[] aseNoisePowerAdded_W = DoubleUtils.mult(DoubleUtils.mult(DoubleUtils.sum(frequenciesPerChannel_THz, centralFrequency_array_THz), aux), eqBandwidthPerChannel_THz);

				powerPerChannel_W = DoubleUtils.mult(powerPerChannel_W, dB2linear(edfaGain_dB));
				nliNoisePower_W = DoubleUtils.mult(nliNoisePower_W, dB2linear(edfaGain_dB));
				final double[] aseNoisePowerAmplified_W = DoubleUtils.mult(aseNoisePower_W, dB2linear(edfaGain_dB));
				aseNoisePower_W = DoubleUtils.sum(aseNoisePowerAmplified_W, aseNoisePowerAdded_W);

				/* Update spectrum */
				spectrumParameters.put(stSpectrum_powerPerChannel_W, powerPerChannel_W);
				spectrumParameters.put(stSpectrum_nliNoisePowerg_W, nliNoisePower_W);
				spectrumParameters.put(stSpectrum_aseNoisePower_W, aseNoisePower_W);

				/* Net2Plan calculations */
				current_PMDSquared_ps2 += Math.pow(edfa_PMD, 2);
			} else if (name.equalsIgnoreCase("PC"))
			{
				final double pcLoss_dB = elementData;

				double[] powerPerChannel_W = spectrumParameters.get(stSpectrum_powerPerChannel_W);
				double[] nliNoisePower_W = spectrumParameters.get(stSpectrum_nliNoisePowerg_W);
				double[] aseNoisePower_W = spectrumParameters.get(stSpectrum_aseNoisePower_W);

				/* GN calculations */
				final double lin_att = 1 / dB2linear(pcLoss_dB);

				powerPerChannel_W = DoubleUtils.mult(powerPerChannel_W, lin_att);
				nliNoisePower_W = DoubleUtils.mult(nliNoisePower_W, lin_att);
				aseNoisePower_W = DoubleUtils.mult(aseNoisePower_W, lin_att);

				/* Update spectrum */
				spectrumParameters.put(stSpectrum_powerPerChannel_W, powerPerChannel_W);
				spectrumParameters.put(stSpectrum_nliNoisePowerg_W, nliNoisePower_W);
				spectrumParameters.put(stSpectrum_aseNoisePower_W, aseNoisePower_W);

				/* Net2Plan calculations */
				current_PMDSquared_ps2 += Math.pow(pc_PMD, 2);

			} else
			{
				throw new RuntimeException("Unknown element type");
			}

			res.add(Quadruple.of(prevSpectrum, prevPMDSquared_ps2, Maps.newHashMap(spectrumParameters), current_PMDSquared_ps2));
		}
		return res;
	}

	/**
	 * Computes power, ASE noise and NLI noise after an OADM
	 * 
	 * @param oadm_outputPowerPerChannel_W target W per channel at the output of the OADM
	 * @param inputPowerPerChannel_W current W per channel
	 * @param nliNoisePower_W non-linear noise power at the input of the OADM in W
	 * @param aseNoisePower_W linear noise power at the input of the OADM in W
	 * @param noiseFigure_dB noise figure of the OADM in dB
	 * @param centralFrequency_THz the central frequency of the used spectrum in THz
	 * @param frequenciesPerChannel_THz the frequency of each channel in THz
	 * @param eqBandwidthPerChannel_THz equivalent bandwidth per channel in THz
	 * @return spectrum after OADM (power [W], NLI noise power [W], ASE noise power[W])
	 */
	private static Triple<double[], double[], double[]> getSpectrumAfterOADM(double oadm_outputPowerPerChannel_W, double[] inputPowerPerChannel_W, double[] nliNoisePower_W, double[] aseNoisePower_W,
			double noiseFigure_dB, double centralFrequency_THz, double[] frequenciesPerChannel_THz, double eqBandwidthPerChannel_THz[])
	{
		final int numChannels = frequenciesPerChannel_THz.length;

		final double[] targetOutputPower_W = new double[numChannels]; Arrays.fill(targetOutputPower_W, oadm_outputPowerPerChannel_W);

		final double[] gain_linear = DoubleUtils.divide(targetOutputPower_W, inputPowerPerChannel_W);

		if (gain_linear[0] != 1)
		{
			final double[] aux = DoubleUtils.mult(gain_linear, dB2linear(noiseFigure_dB) * constant_h * 1e24);
			final double[] centralFrequency_array_THz = new double[numChannels]; Arrays.fill(centralFrequency_array_THz, centralFrequency_THz);
			final double[] aseNoisePowerAdded_W = DoubleUtils.mult(DoubleUtils.mult(DoubleUtils.sum(frequenciesPerChannel_THz, centralFrequency_array_THz), aux), eqBandwidthPerChannel_THz);

			nliNoisePower_W = DoubleUtils.mult(nliNoisePower_W, gain_linear);
			final double[] aseNoisePowerAmplified_W = DoubleUtils.mult(aseNoisePower_W, gain_linear);
			aseNoisePower_W = DoubleUtils.sum(aseNoisePowerAmplified_W, aseNoisePowerAdded_W);

		}
		return Triple.of(targetOutputPower_W, nliNoisePower_W, aseNoisePower_W);
	}

	/**
	 * Computes the Inverse Hyperbolic Sine of the given value
	 * 
	 * @param x value
	 * @return asinh
	 */
	private static double asinh(double x)
	{
		return Math.log(x + Math.sqrt(x * x + 1));
	}

	/**
	 * Converts dB value into linear
	 * 
	 * @param dB value in dB
	 * @return linear value
	 */
	public static double dB2linear(double dB)
	{
		return Math.pow(10, dB / 10);
	}

	/**
	 * Converts linear value into dBs
	 * 
	 * @param num linear number
	 * @return value in dB
	 */
	public static double linear2dB(double num)
	{
		return 10 * Math.log10(num);
	}

	/**
	 * Converts linear arrays into dBs
	 * 
	 * @param nums linear array
	 * @return values in dB
	 */
	public static double[] linear2dB(double[] nums)
	{
		double[] res_dB = new double[nums.length];
		for (int i = 0; i < nums.length; i++)
			res_dB[i] = linear2dB(nums[i]);

		return res_dB;
	}

	/**
	 * Converts dB array into linear
	 * 
	 * @param dB array of dB values
	 * @return linear nums
	 */
	public static double[] dB2linear(double[] dB)
	{
		double[] res_lin = new double[dB.length];
		for (int i = 0; i < dB.length; i++)
			res_lin[i] = dB2linear(dB[i]);

		return res_lin;
	}

}
