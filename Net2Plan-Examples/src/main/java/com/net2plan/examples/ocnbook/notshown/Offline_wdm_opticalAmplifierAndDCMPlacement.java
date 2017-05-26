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




 




package com.net2plan.examples.ocnbook.notshown;


import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Offline_wdm_opticalAmplifierAndDCMPlacement implements IAlgorithm
{
	private InputParameter wdmLayerIndex = new InputParameter ("wdmLayerIndex", (int) 0 , "Index of the WDM layer (-1 means default layer)");
	private InputParameter channels_maxNumChannels = new InputParameter ("channels_maxNumChannels", (int) 16 , "Maximum number of WDM channels that will be used" , 1 , Integer.MAX_VALUE);
	private InputParameter fiber_attenuation_dB_per_km = new InputParameter ("fiber_attenuation_dB_per_km", (double) 0.25 , "Fiber attenuation in dB/km");
	private InputParameter fiber_worseChromaticDispersion_ps_per_nm_per_km = new InputParameter ("fiber_worseChromaticDispersion_ps_per_nm_per_km", (double) 6 , "Chromatic dispersion of the fiber in ps/nm/km");
	private InputParameter oadm_perChannelOutputPower_dBm = new InputParameter ("oadm_perChannelOutputPower_dBm", (double) 6 , "Output power per channel at the OADM in dBm");
	private InputParameter oadm_perChannelMinInputPower_dBm = new InputParameter ("oadm_perChannelMinInputPower_dBm", (double) -19 , "Minimum power needed at the OADM input");
	private InputParameter oadm_perChannelMaxInputPower_dBm = new InputParameter ("oadm_perChannelMaxInputPower_dBm", (double) 1000 , "Maximum power admitted at the OADM input");
	private InputParameter oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm = new InputParameter ("oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm", (double) 150 , "Maximum chromatic dispersion tolerance in ps/nm at te end of each link (absolute value)");
	private InputParameter edfa_minInputPower_dBm = new InputParameter ("edfa_minInputPower_dBm", (double) -29 , "Minimum input power at the EDFA");
	private InputParameter edfa_maxInputPower_dBm = new InputParameter ("edfa_maxInputPower_dBm", (double) 2 , "Maximum input power at the EDFA");
	private InputParameter edfa_minOutputPower_dBm = new InputParameter ("edfa_minOutputPower_dBm", (double) -6 , "Minimum output power at the EDFA");
	private InputParameter edfa_maxOutputPower_dBm = new InputParameter ("edfa_maxOutputPower_dBm", (double) 19 , "Maximum output power at the EDFA");
	private InputParameter edfa_minGain_dB = new InputParameter ("edfa_minGain_dB", (double) 17 , "Minimum gain at the EDFA");
	private InputParameter edfa_maxGain_dB = new InputParameter ("edfa_maxGain_dB", (double) 23 , "Maximum gain at the EDFA");
	private InputParameter dcm_channelDispersionMax_ps_per_nm = new InputParameter ("dcm_channelDispersionMax_ps_per_nm", (double) -827 , "Dispersion compensation (ps/nm) assumed constant in all the channels for DCM type 1");
	private InputParameter dcm_channelDispersionMin_ps_per_nm = new InputParameter ("dcm_channelDispersionMin_ps_per_nm", (double) -276 , "Dispersion compensation (ps/nm) assumed constant in all the channels for DCM type 2");
	private InputParameter dcm_insertionLoss_dB = new InputParameter ("dcm_insertionLoss_dB", (double) 3.5 , "Insertion loss added by the DCMs");

	private NetworkLayer wdmLayer; 
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		/* Initialize some variables */
		this.wdmLayer = wdmLayerIndex.getInt () == -1? netPlan.getNetworkLayerDefault() : netPlan.getNetworkLayer(wdmLayerIndex.getInt ());

		/* Usable wavelengths */
		final double channels_maxNumChannels_dB = 10 * Math.log10(channels_maxNumChannels.getInt());

//		/* Fiber specifications  */
//		final double fiber_attenuation_dB_per_km = Double.parseDouble(algorithmParameters.get("fiber_attenuation_dB_per_km"));
//		final double fiber_worseChromaticDispersion_ps_per_nm_per_km = Double.parseDouble(algorithmParameters.get("fiber_worseChromaticDispersion_ps_per_nm_per_km"));
//
//		/* Transponder and OADM specifications */
//		final double oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm = Double.parseDouble(algorithmParameters.get("oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm"));
//		final double oadm_perChannelOutputPower_dBm = Double.parseDouble(algorithmParameters.get("oadm_perChannelOutputPower_dBm"));
//		final double oadm_perChannelMinInputPower_dBm = Double.parseDouble(algorithmParameters.get("oadm_perChannelMinInputPower_dBm"));
//		final double oadm_perChannelMaxInputPower_dBm = Double.parseDouble(algorithmParameters.get("oadm_perChannelMaxInputPower_dBm"));
//
//		/* Optical amplifier specifications */
//		final double edfa_minInputPower_dBm = Double.parseDouble(algorithmParameters.get("edfa_minInputPower_dBm"));
//		final double edfa_maxInputPower_dBm = Double.parseDouble(algorithmParameters.get("edfa_maxInputPower_dBm"));
//		final double edfa_minOutputPower_dBm = Double.parseDouble(algorithmParameters.get("edfa_minOutputPower_dBm"));
//		final double edfa_maxOutputPower_dBm = Double.parseDouble(algorithmParameters.get("edfa_maxOutputPower_dBm"));
//		final double edfa_minGain_dB = Double.parseDouble(algorithmParameters.get("edfa_minGain_dB"));
//		final double edfa_maxGain_dB = Double.parseDouble(algorithmParameters.get("edfa_maxGain_dB"));
//
//		/* Dispersion compensation modules specifications */
//		final double dcm_channelDispersionMax_ps_per_nm = Double.parseDouble(algorithmParameters.get("dcm_channelDispersionMax_ps_per_nm"));
//		final double dcm_channelDispersionMin_ps_per_nm = Double.parseDouble(algorithmParameters.get("dcm_channelDispersionMin_ps_per_nm"));
//		final double dcm_insertionLoss_dB = Double.parseDouble(algorithmParameters.get("dcm_insertionLoss_dB"));

		
		final double checkMargin = 0.001;
		
		for (Link e : netPlan.getLinks (wdmLayer))
		{
			final double d_e_km = e.getLengthInKm();
			
			/* Compute the number of DCMs needed to have the required CD at the receiver */
			final Pair<Integer,Double> dcmsInfo = computeNumberDCMsRequired (d_e_km , fiber_worseChromaticDispersion_ps_per_nm_per_km.getDouble() , dcm_channelDispersionMax_ps_per_nm.getDouble() , dcm_channelDispersionMin_ps_per_nm.getDouble() , oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm.getDouble());
			final int numberDCMsRequired = dcmsInfo.getFirst();
			final double compensatinPerDCM_ps_per_nm = dcmsInfo.getSecond();
			final double resultingChromaticDispersion_ps_per_nm = d_e_km * fiber_worseChromaticDispersion_ps_per_nm_per_km.getDouble() + numberDCMsRequired * compensatinPerDCM_ps_per_nm;

//			System.out.println("numberDCMsRequired: " + numberDCMsRequired);
//			System.out.println("resultingChromaticDispersion_ps_per_nm: " + resultingChromaticDispersion_ps_per_nm);
//			System.out.println("oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm: " + oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm);

			if ((resultingChromaticDispersion_ps_per_nm > oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm.getDouble()) || (resultingChromaticDispersion_ps_per_nm < -oadm_maxAbsoluteChromaticDispersionAtInput_ps_per_nm.getDouble()))
				throw new RuntimeException ("The resulting CD is not within limits");
			
			final double totalLinkLosses_dB = d_e_km * fiber_attenuation_dB_per_km.getDouble() + numberDCMsRequired * dcm_insertionLoss_dB.getDouble();

			final double minimumRequiredLinkAmplification_dB =  oadm_perChannelMinInputPower_dBm.getDouble() + totalLinkLosses_dB - oadm_perChannelOutputPower_dBm.getDouble();
			final double maximumRequiredLinkAmplification_dB =  oadm_perChannelMaxInputPower_dBm.getDouble() + totalLinkLosses_dB - oadm_perChannelOutputPower_dBm.getDouble();

			final int minNumberOfLineEDFAsMaxGain = (int) Math.max(0, Math.ceil(minimumRequiredLinkAmplification_dB / edfa_maxGain_dB.getDouble()));
			final double commonGainForAllLineEDFAs_dB = (minNumberOfLineEDFAsMaxGain == 0)? edfa_minGain_dB.getDouble() : Math.max(edfa_minGain_dB.getDouble() ,  minimumRequiredLinkAmplification_dB / minNumberOfLineEDFAsMaxGain);

//			System.out.println("... minimumRequiredLinkAmplification_dB: " + minimumRequiredLinkAmplification_dB);
//			System.out.println("maximumRequiredLinkAmplification_dB: " + maximumRequiredLinkAmplification_dB);
//			System.out.println("numberDCMsRequired: " + numberDCMsRequired);
//			System.out.println("minNumberOfLineEDFAsMaxGain: " + minNumberOfLineEDFAsMaxGain);
//			System.out.println("totalLinkLosses_dB: " + totalLinkLosses_dB);
			
			if ((commonGainForAllLineEDFAs_dB * minNumberOfLineEDFAsMaxGain > maximumRequiredLinkAmplification_dB + checkMargin) || (commonGainForAllLineEDFAs_dB * minNumberOfLineEDFAsMaxGain < minimumRequiredLinkAmplification_dB - checkMargin)) 
				throw new Net2PlanException ("Impossible to make the design: link length: " + d_e_km + ", minimum num EDFAS required: " + minNumberOfLineEDFAsMaxGain + ", common gain: " + commonGainForAllLineEDFAs_dB + ", input power end OADM:" + (oadm_perChannelOutputPower_dBm.getDouble() -totalLinkLosses_dB + commonGainForAllLineEDFAs_dB * minNumberOfLineEDFAsMaxGain)); 

			if (numberDCMsRequired > minNumberOfLineEDFAsMaxGain + 1) throw new Net2PlanException ("The number of DCMs should not exceed the number of EDFAs");
			
			if ((commonGainForAllLineEDFAs_dB < edfa_minGain_dB.getDouble() - checkMargin) || (commonGainForAllLineEDFAs_dB > edfa_maxGain_dB.getDouble() + checkMargin)) throw new Net2PlanException ("Impossible to assign a valid constant gain to EDFAs: Link length: " + d_e_km + ", gain: " + commonGainForAllLineEDFAs_dB);
			
			final double targetPerChannelPowerAtAllEDFAsInputs_dBm = edfa_maxOutputPower_dBm.getDouble() - commonGainForAllLineEDFAs_dB - channels_maxNumChannels_dB;
//			System.out.println("targetPerChannelPowerAtAllEDFAsInputs_dBm: " + targetPerChannelPowerAtAllEDFAsInputs_dBm);
//			System.out.println("edfa_minOutputPower_dBm: " + edfa_minOutputPower_dBm);
//			System.out.println("commonGainForAllLineEDFAs_dB: " + commonGainForAllLineEDFAs_dB);
			
			if (targetPerChannelPowerAtAllEDFAsInputs_dBm < edfa_minInputPower_dBm.getDouble()) throw new RuntimeException ("The input power at line EDFA is not enough when there is only one WDM channel");
			if ((targetPerChannelPowerAtAllEDFAsInputs_dBm + channels_maxNumChannels_dB > edfa_maxInputPower_dBm.getDouble()) || (targetPerChannelPowerAtAllEDFAsInputs_dBm < edfa_minInputPower_dBm.getDouble())) throw new Net2PlanException ("Impossible to assign a valid input power at EDFAs");
			
			LinkedList<Pair<Double,Double>> edfaPositionsAndGains = new LinkedList<Pair<Double,Double>> (); 
			LinkedList<Pair<Double,Double>> dcmPositionsAndCompensations = new LinkedList<Pair<Double,Double>> (); 
			for (int contEDFA = 0 ; contEDFA < minNumberOfLineEDFAsMaxGain ; contEDFA ++)
			{
				/* compute current power */
				final double currentPosition_km = (edfaPositionsAndGains.size() == 0)? 0 : edfaPositionsAndGains.getLast().getFirst();
				final double current_powerPerChannel_dBm = oadm_perChannelOutputPower_dBm.getDouble() - currentPosition_km * fiber_attenuation_dB_per_km.getDouble() - dcmPositionsAndCompensations.size() * dcm_insertionLoss_dB.getDouble() + edfaPositionsAndGains.size() * commonGainForAllLineEDFAs_dB;
				final boolean nextEDFAHasDCM = (dcmPositionsAndCompensations.size () < numberDCMsRequired);
				
				/* Compute next EDFA position so that has the target power at its input */
				final double distanceToNextEDFA = (current_powerPerChannel_dBm - targetPerChannelPowerAtAllEDFAsInputs_dBm - ((nextEDFAHasDCM)? dcm_insertionLoss_dB.getDouble() : 0)) / fiber_attenuation_dB_per_km.getDouble();
				
				edfaPositionsAndGains.add(Pair.of(currentPosition_km + distanceToNextEDFA , commonGainForAllLineEDFAs_dB)); 
				if (nextEDFAHasDCM) dcmPositionsAndCompensations.add (Pair.of(currentPosition_km + distanceToNextEDFA , compensatinPerDCM_ps_per_nm));
			}
			if (dcmPositionsAndCompensations.size() == numberDCMsRequired-1)
				dcmPositionsAndCompensations.add (Pair.of(d_e_km , compensatinPerDCM_ps_per_nm));
			if (dcmPositionsAndCompensations.size() != numberDCMsRequired) throw new RuntimeException ("Bad: dcmPositionsAndCompensations: " + dcmPositionsAndCompensations +  ", numberDCMsRequired: "+ numberDCMsRequired);

			/* Set the attributes */
			String st_edfaPositions_km = "";
			String st_edfaGains_dB = "";
			String st_dcmPositions_km = "";
			String st_dcmCDCompensation_ps_per_nm = "";

			for (Pair<Double,Double> pos_gain : edfaPositionsAndGains)
				{ st_edfaPositions_km += pos_gain.getFirst() + " "; st_edfaGains_dB += pos_gain.getSecond() + " "; }
			for (Pair<Double,Double> pos_CD : dcmPositionsAndCompensations)
				{ st_dcmPositions_km += pos_CD.getFirst() + " "; st_dcmCDCompensation_ps_per_nm += pos_CD.getSecond() + " "; }
			
			/* Set the link attributes */
			e.setAttribute ("edfaPositions_km" , st_edfaPositions_km);
			e.setAttribute ("edfaGains_dB" , st_edfaGains_dB);
			e.setAttribute ("dcmPositions_km" , st_dcmPositions_km);
			e.setAttribute ("dcmCDCompensation_ps_per_nm" , st_dcmCDCompensation_ps_per_nm);
		}
		
		return "Ok!";
	}

	@Override
	public String getDescription()
	{
		StringBuilder aux = new StringBuilder();
		aux.append("Place the EDFA and DCM at WDM links.");
		return aux.toString();
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	private Pair<Integer,Double> computeNumberDCMsRequired (double linkTotalLength_km , double fiber_worseChromaticDispersion_ps_per_nm_per_km , double dcm_channelDispersionMax_ps_per_nm , double dcm_channelDispersionMin_ps_per_nm , double tp_maxChromaticDispersionTolerance_ps_per_nm)
	{
		final double dispersionToCompensate_ps_per_nm = linkTotalLength_km * fiber_worseChromaticDispersion_ps_per_nm_per_km;
		final double minimumDispersionToCompensate_ps_per_nm = Math.max(0, linkTotalLength_km * fiber_worseChromaticDispersion_ps_per_nm_per_km - tp_maxChromaticDispersionTolerance_ps_per_nm);
		final int numIfAllTMax = (int) Math.ceil(minimumDispersionToCompensate_ps_per_nm / Math.abs(dcm_channelDispersionMax_ps_per_nm));
		double cdToCompensatePerDCM_ps_per_nm = -dispersionToCompensate_ps_per_nm / numIfAllTMax;
//		System.out.println("--- cdToCompensatePerDCM_ps_per_nm: " + cdToCompensatePerDCM_ps_per_nm);
//		System.out.println("--- dcm_channelDispersionMax_ps_per_nm: " + dcm_channelDispersionMax_ps_per_nm);
//		System.out.println("--- dcm_channelDispersionMin_ps_per_nm: " + dcm_channelDispersionMin_ps_per_nm);
		if (cdToCompensatePerDCM_ps_per_nm < dcm_channelDispersionMax_ps_per_nm)
			cdToCompensatePerDCM_ps_per_nm = dcm_channelDispersionMax_ps_per_nm;
		if (cdToCompensatePerDCM_ps_per_nm > dcm_channelDispersionMin_ps_per_nm)
			cdToCompensatePerDCM_ps_per_nm = dcm_channelDispersionMin_ps_per_nm;

		final double chromaticDispersion_ps_per_nm = linkTotalLength_km * fiber_worseChromaticDispersion_ps_per_nm_per_km + numIfAllTMax * cdToCompensatePerDCM_ps_per_nm;
	
//		System.out.println("dispersionToCompensate_ps_per_nm: " + dispersionToCompensate_ps_per_nm);
//		System.out.println("minimumDispersionToCompensate_ps_per_nm: " + minimumDispersionToCompensate_ps_per_nm);
//		System.out.println("numIfAllTMax: " + numIfAllTMax);
//		System.out.println("cdToCompensatePerDCM_ps_per_nm: " + cdToCompensatePerDCM_ps_per_nm);
//		System.out.println("dcm_channelDispersionMax_ps_per_nm: " + dcm_channelDispersionMax_ps_per_nm);
//		System.out.println("chromaticDispersion_ps_per_nm: " + chromaticDispersion_ps_per_nm);
//		System.out.println("tp_maxChromaticDispersionTolerance_ps_per_nm: " + tp_maxChromaticDispersionTolerance_ps_per_nm);
		

		if ((chromaticDispersion_ps_per_nm > tp_maxChromaticDispersionTolerance_ps_per_nm) || (chromaticDispersion_ps_per_nm < -tp_maxChromaticDispersionTolerance_ps_per_nm))
			throw new RuntimeException ("Could not find a DCM setting");
		return Pair.of(numIfAllTMax, cdToCompensatePerDCM_ps_per_nm);
	}
	
}
