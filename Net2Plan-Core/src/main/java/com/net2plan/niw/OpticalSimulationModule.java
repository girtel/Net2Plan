/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Quadruple;


/** This class is used to account for the occupation of the optical spectrum in the network.  
 * The object can be created from an existing network. To make a valid optical design, the user is responsible of 
 * using this object methods to check if routing and spectrum assignments (RSAs) of new lightpaths are valid. Also, this object 
 * includes some methods for simple RSA recommendations, e.g. first-fit assignments
 * (occupy idle and valid resources)
 * Occupation is represented by optical slots, each defined by an integer. The central frequency of optical slot i is 193.1+i*0.0125 THz.
 * All optical slots are supposed to have the same width 12.5 GHz (see WNetConstants)
 *
 */
public class OpticalSimulationModule
{
	/** speed of light in m/s */
   public final static double constant_c = 299792458; 
   /** Planck constant m^2 kg/sec */
   public final static double constant_h = 6.62607004E-34; 
	private final WNet wNet;
	public enum PERLPINFOMETRICS { POWER_DBM , CD_PERPERNM , PMDSQUARED_PS2 , OSNRAT12_5GHZREFBW }
	public enum PERFIBERINFOMETRICS { TOTALPOWER_DBM }
	final private SortedMap<WLightpath,Map<PERLPINFOMETRICS , Pair<Double,Double>>> perPerLpPerMetric_valStartEnd = new TreeMap<> ();
	final private SortedMap<WFiber,SortedMap<WLightpath,Map<PERLPINFOMETRICS , Pair<Double,Double>>>> perFiberPerLpPerMetric_valStartEnd = new TreeMap<> ();
	final private SortedMap<WFiber,Map<PERFIBERINFOMETRICS , Quadruple<Double,Double,List<Double>,List<Double>>>> perFiberPerMetric_valStartEndAndAtEachOlaInputOutput = new TreeMap<> ();
	
	public OpticalSimulationModule (WNet wNet) { this.wNet = wNet; }
	
    public static double osnrInDbUnitsAccummulation_dB (List<Double> osnrs_dB)
    {
        if (osnrs_dB.size () == 0) return Double.MAX_VALUE;
        double resDenom_linear = 0; for (double osnr_dB : osnrs_dB) { if (osnr_dB == Double.MAX_VALUE) continue; resDenom_linear += 1.0 / dB2linear(osnr_dB); }
        return resDenom_linear == 0? Double.MIN_VALUE : linear2dB(1.0 / resDenom_linear);
    }
    public static double dB2linear(double dB)
    {
        return Math.pow(10, dB / 10);
    }
    public static double linear2dB(double num)
    {
        return num == 0? -Double.MAX_VALUE : 10 * Math.log10(num);
    }
    public static double osnrContributionEdfaRefBw12dot5GHz_linear (double centralFrequencyChannel_Hz , double noiseFactor_dB, double inputPowerPerChannel_dBm)
    {
        final double inputPower_W = dB2linear(inputPowerPerChannel_dBm) * 1E-3;
        final double edfa_NF_linear = dB2linear(noiseFactor_dB);
        final double referenceBandwidthAtHighestFrequency_Hz = 12.5 * 1e9;
        final double thisEDFAAddedNoise_W = edfa_NF_linear * constant_h * centralFrequencyChannel_Hz * referenceBandwidthAtHighestFrequency_Hz;
        final double addedOSNRThisOA_linear = inputPower_W / thisEDFAAddedNoise_W;
        return addedOSNRThisOA_linear;
    }

    public OpticalSimulationModule updateAllPerformanceInfo ()
    {
   	 for (WFiber e : wNet.getFibers())
   		 perFiberPerLpPerMetric_valStartEnd.put(e, new TreeMap<> ());

   	 for (WLightpath lp : wNet.getLightpaths())
   	 {
   		 final int numOpticalSlots = lp.getOpticalSlotIds().size();
   		 final double centralFrequency_hz = WNetConstants.CENTRALFREQUENCYOFOPTICALSLOTZERO_THZ * 1e12 + 12.5e9 * (lp.getOpticalSlotIds().first() + lp.getOpticalSlotIds().last())/2.0;
   		 boolean firstFiber = true;
   		 Optional<Map<PERLPINFOMETRICS,Pair<Double,Double>>> previousFiberInfo = Optional.empty();
   		 for (WFiber fiber : lp.getSeqFibers())
   		 {
   			 final Map<PERLPINFOMETRICS,Pair<Double,Double>> infoToAdd = new HashMap<> ();
   			 perFiberPerLpPerMetric_valStartEnd.get(fiber).put(lp, infoToAdd);
   			 final WNode oadm_a = fiber.getA();
   			 final double totalFiberGain_dB = fiber.getAmplifierGains_dB().stream().mapToDouble(e->e).sum();
   			 final double totalFiberAttenuation_dB = fiber.getAttenuationCoefficient_dbPerKm() * fiber.getLengthInKm();
   			 final double totalFiberCdBalance_psPerNm = fiber.getChromaticDispersionCoeff_psPerNmKm() * fiber.getLengthInKm() + fiber.getAmplifierCdCompensation_psPerNm().stream().mapToDouble(e->e).sum();
   			 final double totalFiberPmdSquaredBalance_ps2 = fiber.getLengthInKm() * Math.pow(fiber.getPmdLinkDesignValueCoeff_psPerSqrtKm() , 2) + fiber.getAmplifierPmd_ps().stream().mapToDouble(e->Math.pow(e, 2)).sum();
   			 final boolean isAEqualizing = oadm_a.isConfiguredToEqualizeOutput();
   			 final double startFiber_powerLp_dBm;
   			 if (firstFiber)
   			 {
   				 if (isAEqualizing) 
   					 startFiber_powerLp_dBm = linear2dB(numOpticalSlots * oadm_a.getOutputSpectrumEqualizationTarget_mwPerGhz().get() * WNetConstants.OPTICALSLOTSIZE_GHZ); 
   				 else
   					 startFiber_powerLp_dBm = lp.getAddTransponderInjectionPower_dBm() + oadm_a.getAddGain_dB();
   				 infoToAdd.put(PERLPINFOMETRICS.POWER_DBM, Pair.of(startFiber_powerLp_dBm, startFiber_powerLp_dBm + totalFiberGain_dB - totalFiberAttenuation_dB));
   				 infoToAdd.put(PERLPINFOMETRICS.CD_PERPERNM, Pair.of(0.0, totalFiberCdBalance_psPerNm));
   				 infoToAdd.put(PERLPINFOMETRICS.PMDSQUARED_PS2, Pair.of(0.0, totalFiberPmdSquaredBalance_ps2));
   				 final List<Double> osnrAddedEachEdfa_db = new ArrayList<> ();
   				 /* OSNR at the start of fiber */
   				 final double osnrAtTheStartOfFiber_dB = linear2dB(osnrContributionEdfaRefBw12dot5GHz_linear(centralFrequency_hz, oadm_a.getAddNoiseFactor_dB(), lp.getAddTransponderInjectionPower_dBm()));
   				 osnrAddedEachEdfa_db.add(osnrAtTheStartOfFiber_dB);
   				 for (int contOla = 0; contOla < fiber.getAmplifierGains_dB().size() ; contOla ++)
   				 {
   					 final double noiseFactor_db = fiber.getAmplifierNoiseFactor_dB().get(contOla);
   					 final double kmFromStartFiber = fiber.getAmplifierPositionsKmFromOrigin_km().get(contOla);
   					 final double sumGainsTraversedAmplifiers_db = IntStream.range(0, contOla).mapToDouble(olaIndex -> fiber.getAmplifierGains_dB().get(olaIndex)).sum();
   					 final double lpPowerAtInputOla_dBm = startFiber_powerLp_dBm - kmFromStartFiber * fiber.getAttenuationCoefficient_dbPerKm() + sumGainsTraversedAmplifiers_db;
   					 final double osnrContributionThisOla_db = linear2dB(osnrContributionEdfaRefBw12dot5GHz_linear(centralFrequency_hz, noiseFactor_db, lpPowerAtInputOla_dBm));
   					 osnrAddedEachEdfa_db.add (osnrContributionThisOla_db); 
   				 }
   				 final double osnrEndOfFiber_dB = osnrInDbUnitsAccummulation_dB (osnrAddedEachEdfa_db);
   				 infoToAdd.put(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW, Pair.of(Double.MAX_VALUE, osnrEndOfFiber_dB));
   			 }
   			 else
   			 {
   				 if (isAEqualizing) 
   					 startFiber_powerLp_dBm = linear2dB(numOpticalSlots * oadm_a.getOutputSpectrumEqualizationTarget_mwPerGhz().get() * WNetConstants.OPTICALSLOTSIZE_GHZ); 
   				 else
   					 startFiber_powerLp_dBm = previousFiberInfo.get().get(PERLPINFOMETRICS.POWER_DBM).getSecond() + oadm_a.getExpressGain_dB();
   				 infoToAdd.put(PERLPINFOMETRICS.POWER_DBM, Pair.of(startFiber_powerLp_dBm, startFiber_powerLp_dBm + totalFiberGain_dB - totalFiberAttenuation_dB));
   				 final double previousFiberEndCd_perPerNm = previousFiberInfo.get().get(PERLPINFOMETRICS.CD_PERPERNM).getSecond();
   				 final double previousFiberEndPmdSquared_ps2= previousFiberInfo.get().get(PERLPINFOMETRICS.PMDSQUARED_PS2).getSecond();
   				 infoToAdd.put(PERLPINFOMETRICS.CD_PERPERNM, Pair.of(previousFiberEndCd_perPerNm, previousFiberEndCd_perPerNm + totalFiberCdBalance_psPerNm));
   				 infoToAdd.put(PERLPINFOMETRICS.PMDSQUARED_PS2, Pair.of(previousFiberEndPmdSquared_ps2, previousFiberEndPmdSquared_ps2 + totalFiberPmdSquaredBalance_ps2));
   				 final List<Double> osnrAddedEachEdfaAndOadm_db = new ArrayList<> ();
   				 /* OSNR at the start of fiber */
   				 final double powerAtEndOfLastFiber_dBm = previousFiberInfo.get().get(PERLPINFOMETRICS.POWER_DBM).getSecond();
   				 final double osnrAtEndOfLastFiber_dB = previousFiberInfo.get().get(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW).getSecond();
   				 final double osnrContributedByExpressOadm_dB = linear2dB(osnrContributionEdfaRefBw12dot5GHz_linear(centralFrequency_hz, oadm_a.getExpressNoiseFactor_dB(), powerAtEndOfLastFiber_dBm));
   				 final double osnrAtStartOfFiber_dB = osnrInDbUnitsAccummulation_dB (Arrays.asList(osnrAtEndOfLastFiber_dB , osnrContributedByExpressOadm_dB));
   				 osnrAddedEachEdfaAndOadm_db.add(osnrAtStartOfFiber_dB);
   				 for (int contOla = 0; contOla < fiber.getAmplifierGains_dB().size() ; contOla ++)
   				 {
   					 final double noiseFactor_db = fiber.getAmplifierNoiseFactor_dB().get(contOla);
   					 final double kmFromStartFiber = fiber.getAmplifierPositionsKmFromOrigin_km().get(contOla);
   					 final double sumGainsTraversedAmplifiers_db = IntStream.range(0, contOla).mapToDouble(olaIndex -> fiber.getAmplifierGains_dB().get(olaIndex)).sum();
   					 final double lpPowerAtInputOla_dBm = startFiber_powerLp_dBm - kmFromStartFiber * fiber.getAttenuationCoefficient_dbPerKm() + sumGainsTraversedAmplifiers_db;
   					 final double osnrContributionThisOla_db = linear2dB(osnrContributionEdfaRefBw12dot5GHz_linear(centralFrequency_hz, noiseFactor_db, lpPowerAtInputOla_dBm));
   					 osnrAddedEachEdfaAndOadm_db.add (osnrContributionThisOla_db); 
   				 }
   				 final double osnrEndOfFiber_dB = osnrInDbUnitsAccummulation_dB (osnrAddedEachEdfaAndOadm_db);
   				 infoToAdd.put(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW, Pair.of(osnrAtStartOfFiber_dB, osnrEndOfFiber_dB));
   			 }
   			 
   			 previousFiberInfo = Optional.of(infoToAdd);
   			 firstFiber = false;
   		 }
   	 }
   	 
   	 /* Update the per lp information at add and end */
   	 for (WLightpath lp : wNet.getLightpaths())
   	 {
   		 final double centralFrequency_hz = WNetConstants.CENTRALFREQUENCYOFOPTICALSLOTZERO_THZ * 1e12 + 12.5e9 * (lp.getOpticalSlotIds().first() + lp.getOpticalSlotIds().last())/2.0;
   		 final Map<PERLPINFOMETRICS,Pair<Double,Double>> vals = new HashMap<> ();
   		 final WFiber lastFiber = lp.getSeqFibers().get(lp.getSeqFibers().size()-1);
   		 final WNode lastOadm = lastFiber.getB();
   		 final double inputLastOadm_power_dBm = perFiberPerLpPerMetric_valStartEnd.get(lastFiber).get(lp).get(PERLPINFOMETRICS.POWER_DBM).getSecond();
   		 final double inputLastOadm_cd_psPerNm = perFiberPerLpPerMetric_valStartEnd.get(lastFiber).get(lp).get(PERLPINFOMETRICS.CD_PERPERNM).getSecond();
   		 final double inputLastOadm_pmdSquared_ps2 = perFiberPerLpPerMetric_valStartEnd.get(lastFiber).get(lp).get(PERLPINFOMETRICS.PMDSQUARED_PS2).getSecond();
   		 final double inputLastOadm_onsnr_dB = perFiberPerLpPerMetric_valStartEnd.get(lastFiber).get(lp).get(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW).getSecond();

   		 final double drop_power_dBm = inputLastOadm_power_dBm + lastOadm.getDropGain_dB();
   		 final double drop_cd_psPerNm = inputLastOadm_cd_psPerNm;
   		 final double drop_pmdSquared_ps2 = inputLastOadm_pmdSquared_ps2 + Math.pow(lastOadm.getDropPmd_ps() ,2);
   		 final double addedOsnrByDropOadm = osnrContributionEdfaRefBw12dot5GHz_linear(centralFrequency_hz, lastOadm.getDropNoiseFactor_dB(), inputLastOadm_power_dBm);
   		 final double drop_osnr12_5RefBw_dB = osnrInDbUnitsAccummulation_dB(Arrays.asList(inputLastOadm_onsnr_dB , addedOsnrByDropOadm));

   		 vals.put(PERLPINFOMETRICS.POWER_DBM, Pair.of(lp.getAddTransponderInjectionPower_dBm(), drop_power_dBm));
   		 vals.put(PERLPINFOMETRICS.CD_PERPERNM, Pair.of(0.0, drop_cd_psPerNm));
   		 vals.put(PERLPINFOMETRICS.PMDSQUARED_PS2, Pair.of(0.0, drop_pmdSquared_ps2));
   		 vals.put(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW, Pair.of(Double.MAX_VALUE, drop_osnr12_5RefBw_dB));
   		 
   		 perPerLpPerMetric_valStartEnd.put(lp, vals);
   	 }
   	 
   	 /* Update the total power per fiber */
   	 for (WFiber fiber : wNet.getFibers())
   	 {
   		 final Map<PERFIBERINFOMETRICS , Quadruple<Double,Double,List<Double>,List<Double>>> vals = new HashMap<> ();
   		 final double powerAtStart_dBm = linear2dB(fiber.getTraversingLps().stream().map(lp->perFiberPerLpPerMetric_valStartEnd.get(fiber).get(lp).get(PERLPINFOMETRICS.POWER_DBM).getFirst()).
   				 mapToDouble (v->dB2linear(v)).sum ());
   		 final double powerAtEnd_dBm = linear2dB(fiber.getTraversingLps().stream().map(lp->perFiberPerLpPerMetric_valStartEnd.get(fiber).get(lp).get(PERLPINFOMETRICS.POWER_DBM).getSecond()).
   				 mapToDouble (v->dB2linear(v)).sum ());
   		 
   		 final List<Double> powerInputOla_dBm = new ArrayList<> ();
   		 final List<Double> powerOutputOla_dBm = new ArrayList<> ();
			 for (int contOla = 0; contOla < fiber.getAmplifierGains_dB().size() ; contOla ++)
			 {
				 final double kmFromStartFiber = fiber.getAmplifierPositionsKmFromOrigin_km().get(contOla);
				 final double sumGainsTraversedAmplifiers_db = IntStream.range(0, contOla).mapToDouble(olaIndex -> fiber.getAmplifierGains_dB().get(olaIndex)).sum();
				 final double powerAtOutputThisOla_dBm = powerAtStart_dBm - kmFromStartFiber * fiber.getAttenuationCoefficient_dbPerKm() + sumGainsTraversedAmplifiers_db;
				 final double powerAtInputThisOla_dBm = powerAtOutputThisOla_dBm - fiber.getAmplifierGains_dB().get(contOla);
				 powerInputOla_dBm.add(powerAtInputThisOla_dBm);
				 powerOutputOla_dBm.add(powerAtOutputThisOla_dBm);
			 }
   		 vals.put(PERFIBERINFOMETRICS.TOTALPOWER_DBM, Quadruple.of(powerAtStart_dBm, powerAtEnd_dBm , powerInputOla_dBm , powerOutputOla_dBm));
   		 perFiberPerMetric_valStartEndAndAtEachOlaInputOutput.put(fiber, vals);
   	 }
   	 
   	 return this;
    }
        
    public List<Double> getTotalPowerAtAmplifierInputs_dBm (WFiber fiber)
    {
    	return perFiberPerMetric_valStartEndAndAtEachOlaInputOutput.get(fiber).get(PERFIBERINFOMETRICS.TOTALPOWER_DBM).getThird();
    }
    public List<Double> getTotalPowerAtAmplifierOutputs_dBm (WFiber fiber)
    {
    	return perFiberPerMetric_valStartEndAndAtEachOlaInputOutput.get(fiber).get(PERFIBERINFOMETRICS.TOTALPOWER_DBM).getFourth();
    }
    public double getTotalPowerAtAmplifierInput_dBm (WFiber fiber , int indexAmplifierInFiber)
    {
   	 if (indexAmplifierInFiber < 0 || indexAmplifierInFiber >= fiber.getNumberOfOpticalLineAmplifiersTraversed()) throw new Net2PlanException ("Wrong index");
   	 return perFiberPerMetric_valStartEndAndAtEachOlaInputOutput.get(fiber).get(PERFIBERINFOMETRICS.TOTALPOWER_DBM).getThird().get(indexAmplifierInFiber);
    }
    public double getTotalPowerAtAmplifierOutput_dBm (WFiber fiber , int indexAmplifierInFiber)
    {
   	 if (indexAmplifierInFiber < 0 || indexAmplifierInFiber >= fiber.getNumberOfOpticalLineAmplifiersTraversed()) throw new Net2PlanException ("Wrong index");
   	 return perFiberPerMetric_valStartEndAndAtEachOlaInputOutput.get(fiber).get(PERFIBERINFOMETRICS.TOTALPOWER_DBM).getFourth().get(indexAmplifierInFiber);
    }
    public Map<PERLPINFOMETRICS , Pair<Double,Double>> getOpticalPerformanceOfLightpathAtFiberEnds (WFiber fiber , WLightpath lp)
    {
   	 if (!perFiberPerLpPerMetric_valStartEnd.containsKey(fiber)) return null;
   	 if (!perFiberPerLpPerMetric_valStartEnd.get(fiber).containsKey(lp)) return null;
   	 return perFiberPerLpPerMetric_valStartEnd.get(fiber).get(lp);
    }
    public Pair<Double,Double> getTotalPowerAtFiberEnds_dBm (WFiber fiber)
    {
   	 if (!perFiberPerMetric_valStartEndAndAtEachOlaInputOutput.containsKey(fiber)) return null;
   	 return Pair.of(
   			 perFiberPerMetric_valStartEndAndAtEachOlaInputOutput.get(fiber).get(PERFIBERINFOMETRICS.TOTALPOWER_DBM).getFirst (), 
   			 perFiberPerMetric_valStartEndAndAtEachOlaInputOutput.get(fiber).get(PERFIBERINFOMETRICS.TOTALPOWER_DBM).getSecond ());
    }
    public Map<PERLPINFOMETRICS , Double> getOpticalPerformanceAtTransponderReceiverEnd (WLightpath lp)
    {
   	 final Map<PERLPINFOMETRICS , Double> res = new HashMap<> ();
   	 for (PERLPINFOMETRICS type : PERLPINFOMETRICS.values())
   		 res.put(type, perPerLpPerMetric_valStartEnd.get(lp).get(type).getSecond());
   	 return res;
    }
    public Map<PERLPINFOMETRICS , Double> getOpticalPerformanceAtTransponderTransmitterEnd (WLightpath lp)
    {
   	 final Map<PERLPINFOMETRICS , Double> res = new HashMap<> ();
   	 for (PERLPINFOMETRICS type : PERLPINFOMETRICS.values())
   		 res.put(type, perPerLpPerMetric_valStartEnd.get(lp).get(type).getFirst());
   	 return res;
    }

    public static double nm2thz (double wavelengthInNm)
    {
    	return constant_c / (1000 * wavelengthInNm);
    }
    public static double thz2nm (double freqInThz)
    {
    	return constant_c / (1000 * freqInThz);
    }
    public static double getLowestFreqfSlotTHz (int slot)
    {
    	return WNetConstants.CENTRALFREQUENCYOFOPTICALSLOTZERO_THZ + (slot - 0.5)  * (WNetConstants.OPTICALSLOTSIZE_GHZ/1000);
    }
    public static double getHighestFreqfSlotTHz (int slot)
    {
    	return WNetConstants.CENTRALFREQUENCYOFOPTICALSLOTZERO_THZ + (slot + 0.5)  * (WNetConstants.OPTICALSLOTSIZE_GHZ/1000);
    }
    
    public boolean isOkOpticalPowerAtAmplifierInputAllOlas (WFiber e)
    {
		final List<Double> minInputPowerDbm = e.getAmplifierMinAcceptableInputPower_dBm();
		final List<Double> maxInputPowerDbm = e.getAmplifierMaxAcceptableInputPower_dBm();
		for (int cont = 0; cont < minInputPowerDbm.size() ; cont ++)
		{
			final double inputPowerDbm = this.getTotalPowerAtAmplifierInput_dBm(e, cont);
			if (inputPowerDbm < minInputPowerDbm.get(cont)) return false;
			if (inputPowerDbm > maxInputPowerDbm.get(cont)) return false;
		}
		return true;
    }
    
}
