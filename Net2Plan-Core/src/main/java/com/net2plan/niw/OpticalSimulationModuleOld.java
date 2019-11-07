/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
public class OpticalSimulationModuleOld
{
	/** speed of light in m/s */
   public final static double constant_c = 299792458; 
   /** Planck constant m^2 kg/sec */
   public final static double constant_h = 6.62607004E-34; 
	private final WNet wNet;
	public enum PERLPINFOMETRICS { POWER_DBM , CD_PERPERNM , PMDSQUARED_PS2 , OSNRAT12_5GHZREFBW }
	public enum PERFIBERINFOMETRICS { TOTALPOWER_DBM }
	final private SortedMap<WLightpath,Map<PERLPINFOMETRICS , Double>> perLpPerMetric_valAtDropTransponderEnd = new TreeMap<> ();
	final private SortedMap<WFiber,SortedMap<WLightpath,Map<PERLPINFOMETRICS , Pair<Double,Double>>>> perFiberPerLpPerMetric_valStartEnd = new TreeMap<> ();
	final private SortedMap<WFiber,Map<PERFIBERINFOMETRICS , Quadruple<Double,Double,List<Double>,List<Double>>>> perFiberPerMetric_valStartEndAndAtEachOlaInputOutput = new TreeMap<> ();
	final private SortedMap<WFiber,SortedMap<WLightpath,SortedMap<Integer , Map<PERLPINFOMETRICS , Pair<Double,Double>>>>> perFiberPerLpPerOlaIndexMetric_valStartEnd = new TreeMap<> ();
	
	public OpticalSimulationModuleOld (WNet wNet) 
	{
		this.wNet = wNet;
		this.updateAllPerformanceInfo();
	}
	
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
    	if (noiseFactor_dB == -Double.MAX_VALUE) return Double.MAX_VALUE;
        final double inputPower_W = dB2linear(inputPowerPerChannel_dBm) * 1E-3;
        final double edfa_NF_linear = dB2linear(noiseFactor_dB);
        final double referenceBandwidthAtHighestFrequency_Hz = 12.5 * 1e9;
        final double thisEDFAAddedNoise_W = edfa_NF_linear * constant_h * centralFrequencyChannel_Hz * referenceBandwidthAtHighestFrequency_Hz;
        final double addedOSNRThisOA_linear = inputPower_W / thisEDFAAddedNoise_W;
        return addedOSNRThisOA_linear;
    }

    public OpticalSimulationModuleOld updateAllPerformanceInfo ()
    {
    	System.out.println("Update all performance info");
   	 for (WFiber e : wNet.getFibers())
   	 {
   		 perFiberPerLpPerMetric_valStartEnd.put(e, new TreeMap<> ());
   		 perFiberPerLpPerOlaIndexMetric_valStartEnd.put(e, new TreeMap <> ());
   		 for (WLightpath lp : e.getTraversingLps())
   			perFiberPerLpPerOlaIndexMetric_valStartEnd.get(e).put(lp, new TreeMap<> ());
   	 }
   	 for (WLightpath lp : wNet.getLightpaths())
   	 {
   		 final int numOpticalSlots = lp.getOpticalSlotIds().size();
   		 final double centralFrequency_hz = 1e12 * lp.getCentralFrequencyThz();
   		 Optional<Map<PERLPINFOMETRICS,Pair<Double,Double>>> previousFiberInfo = Optional.empty();
   		 final List<WFiber> lpSeqFibers = lp.getSeqFibers();
   		 for (int contFiber = 0; contFiber < lpSeqFibers.size() ; contFiber ++)
   		 {
   			 final WFiber fiber = lpSeqFibers.get(contFiber);
   	   		 final boolean firstFiber = contFiber == 0;
   			 final Map<PERLPINFOMETRICS,Pair<Double,Double>> infoToAdd = new HashMap<> ();
   			 final SortedMap<Integer , Map<PERLPINFOMETRICS,Pair<Double,Double>>> infoToAddPerOla = new TreeMap<> ();
   			 perFiberPerLpPerMetric_valStartEnd.get(fiber).put(lp, infoToAdd);
   			 perFiberPerLpPerOlaIndexMetric_valStartEnd.get(fiber).put(lp, infoToAddPerOla);
   			
   			 final WNode oadm_a = fiber.getA();
   			 final double totalJustFiberGain_dB = fiber.getOlaGains_dB().stream().mapToDouble(e->e).sum();
   			 final double totalJustFiberAttenuation_dB = fiber.getAttenuationCoefficient_dbPerKm() * fiber.getLengthInKm();
   			 final double totalJustFiberCdBalance_psPerNm = fiber.getChromaticDispersionCoeff_psPerNmKm() * fiber.getLengthInKm() + fiber.getOlaCdCompensation_psPerNm().stream().mapToDouble(e->e).sum();
   			 final double totalJustFiberPmdSquaredBalance_ps2 = fiber.getLengthInKm() * Math.pow(fiber.getPmdLinkDesignValueCoeff_psPerSqrtKm() , 2) + fiber.getOlaPmd_ps().stream().mapToDouble(e->Math.pow(e, 2)).sum();
   			 final double startFiberAfterBooster_powerLp_dBm;
   			 final double startFiberBeforeBooster_powerLp_dBm;
   			 final WFiber previousFiber = contFiber == 0? null : lpSeqFibers.get(contFiber-1);
			 if (fiber.isOriginOadmConfiguredToEqualizeOutput()) 
				startFiberBeforeBooster_powerLp_dBm = linear2dB(numOpticalSlots * fiber.getOriginOadmSpectrumEqualizationTargetBeforeBooster_mwPerGhz().get() * WNetConstants.OPTICALSLOTSIZE_GHZ); 
			 else
				startFiberBeforeBooster_powerLp_dBm = 
				(firstFiber? lp.getAddTransponderInjectionPower_dBm() : previousFiberInfo.get().get(PERLPINFOMETRICS.POWER_DBM).getSecond() + previousFiber.getDestinationPreAmplifierGain_dB().orElse(0.0))
				- oadm_a.getOadmSwitchFabricAttenuation_dB();
			 startFiberAfterBooster_powerLp_dBm = startFiberBeforeBooster_powerLp_dBm + fiber.getOriginBoosterAmplifierGain_dB().orElse(0.0); 
			 final double startFiberAfterBooster_cd_psPerNm = firstFiber? fiber.getOriginBoosterAmplifierCdCompensation_psPerNm().orElse(0.0) : previousFiberInfo.get().get(PERLPINFOMETRICS.CD_PERPERNM).getSecond() + previousFiber.getDestinationPreAmplifierCdCompensation_psPerNm().orElse(0.0) + fiber.getOriginBoosterAmplifierCdCompensation_psPerNm().orElse(0.0);
			 final double startFiberAfterBooster_pmd_ps2 = firstFiber? Math.pow(oadm_a.getOadmSwitchFabricPmd_ps(), 2) + Math.pow(fiber.getOriginBoosterAmplifierPmd_ps().orElse(0.0), 2) : 
				 previousFiberInfo.get().get(PERLPINFOMETRICS.PMDSQUARED_PS2).getSecond () + Math.pow(previousFiber.getDestinationPreAmplifierPmd_ps().orElse(0.0), 2) + Math.pow(oadm_a.getOadmSwitchFabricPmd_ps(), 2) + Math.pow(fiber.getOriginBoosterAmplifierPmd_ps().orElse(0.0), 2);
			 infoToAdd.put(PERLPINFOMETRICS.POWER_DBM, Pair.of(startFiberAfterBooster_powerLp_dBm, startFiberAfterBooster_powerLp_dBm + totalJustFiberGain_dB - totalJustFiberAttenuation_dB));
			 infoToAdd.put(PERLPINFOMETRICS.CD_PERPERNM, Pair.of(startFiberAfterBooster_cd_psPerNm , startFiberAfterBooster_cd_psPerNm + totalJustFiberCdBalance_psPerNm));
			 infoToAdd.put(PERLPINFOMETRICS.PMDSQUARED_PS2, Pair.of(startFiberAfterBooster_pmd_ps2, startFiberAfterBooster_pmd_ps2 + totalJustFiberPmdSquaredBalance_ps2));
			 final double osnrAtStartOfFiber_dB;
			 /* OSNR at the start of fiber */
			 if (firstFiber)
			 {
				 osnrAtStartOfFiber_dB = !fiber.isExistingBoosterAmplifierAtOriginOadm()? Double.MAX_VALUE : linear2dB(osnrContributionEdfaRefBw12dot5GHz_linear(centralFrequency_hz, fiber.getOriginBoosterAmplifierNoiseFactor_dB().get(), startFiberBeforeBooster_powerLp_dBm));
			 }
			 else
			 {
   				 final double powerAtEndOfLastFiber_dBm = previousFiberInfo.get().get(PERLPINFOMETRICS.POWER_DBM).getSecond();
   				 final double osnrAtEndOfLastFiber_dB = previousFiberInfo.get().get(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW).getSecond();
   				 final double osnrContributedByOadmPreamplifier_dB = linear2dB(osnrContributionEdfaRefBw12dot5GHz_linear(centralFrequency_hz, previousFiber.getDestinationPreAmplifierNoiseFactor_dB().orElse(-Double.MAX_VALUE), powerAtEndOfLastFiber_dBm));
   				 final double osnrContributedByOadmBooster_dB = linear2dB(osnrContributionEdfaRefBw12dot5GHz_linear(centralFrequency_hz, fiber.getOriginBoosterAmplifierNoiseFactor_dB().orElse(-Double.MAX_VALUE), startFiberBeforeBooster_powerLp_dBm));
   				 osnrAtStartOfFiber_dB = osnrInDbUnitsAccummulation_dB (Arrays.asList(osnrAtEndOfLastFiber_dB , osnrContributedByOadmPreamplifier_dB , osnrContributedByOadmBooster_dB));
			 }
			 final List<Double> osnrAccumulation_db = new ArrayList<> ();
			 osnrAccumulation_db.add(osnrAtStartOfFiber_dB);
			 for (int contOla = 0; contOla < fiber.getOlaGains_dB().size() ; contOla ++)
			 {
				 final double noiseFactor_db = fiber.getOlaNoiseFactor_dB().get(contOla);
				 final double kmFromStartFiber = fiber.getAmplifierPositionsKmFromOrigin_km().get(contOla);
				 final double sumGainsTraversedAmplifiersBeforeMe_db = IntStream.range(0, contOla).mapToDouble(olaIndex -> fiber.getOlaGains_dB().get(olaIndex)).sum();
				 final double lpPowerAtInputOla_dBm = infoToAdd.get(PERLPINFOMETRICS.POWER_DBM).getFirst() - kmFromStartFiber * fiber.getAttenuationCoefficient_dbPerKm() + sumGainsTraversedAmplifiersBeforeMe_db;
				 final double lpCdAtInputOla_perPerNm = infoToAdd.get(PERLPINFOMETRICS.CD_PERPERNM).getFirst() + fiber.getAmplifierPositionsKmFromOrigin_km().get(contOla) * fiber.getChromaticDispersionCoeff_psPerNmKm() + IntStream.range(0, contOla).mapToDouble(ee->fiber.getOlaCdCompensation_psPerNm().get(ee)).sum();
				 final double lpPmdSquaredAtInputOla_perPerNm = infoToAdd.get(PERLPINFOMETRICS.PMDSQUARED_PS2).getFirst() + fiber.getAmplifierPositionsKmFromOrigin_km().get(contOla) * Math.pow(fiber.getPmdLinkDesignValueCoeff_psPerSqrtKm(),2) + IntStream.range(0, contOla).mapToDouble(ee->Math.pow(fiber.getOlaPmd_ps().get(ee) , 2)).sum();
				 final double lpOsnrAtInputOla_dB = osnrInDbUnitsAccummulation_dB (osnrAccumulation_db);
				 final double osnrContributionThisOla_db = linear2dB(osnrContributionEdfaRefBw12dot5GHz_linear(centralFrequency_hz, noiseFactor_db, lpPowerAtInputOla_dBm));
				 osnrAccumulation_db.add (osnrContributionThisOla_db); 
				 final double lpOsnrAtOutputOla_dB = osnrInDbUnitsAccummulation_dB (osnrAccumulation_db);
				 final Map<PERLPINFOMETRICS , Pair<Double,Double>> infoThisOla = new HashMap<> ();
				infoThisOla.put(PERLPINFOMETRICS.POWER_DBM, Pair.of (lpPowerAtInputOla_dBm , lpPowerAtInputOla_dBm + fiber.getOlaGains_dB().get(contOla)));
				infoThisOla.put(PERLPINFOMETRICS.CD_PERPERNM, Pair.of (lpCdAtInputOla_perPerNm , lpCdAtInputOla_perPerNm + fiber.getOlaCdCompensation_psPerNm().get(contOla)));
				infoThisOla.put(PERLPINFOMETRICS.PMDSQUARED_PS2, Pair.of (lpPmdSquaredAtInputOla_perPerNm , lpPmdSquaredAtInputOla_perPerNm + Math.pow(1.0 ,2)));
				infoThisOla.put(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW, Pair.of (lpOsnrAtInputOla_dB , lpOsnrAtOutputOla_dB));
				infoToAddPerOla.put(contOla, infoThisOla);
			 }
			 final double osnrEndOfFiber_dB = osnrInDbUnitsAccummulation_dB (osnrAccumulation_db);
			 infoToAdd.put(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW, Pair.of(Double.MAX_VALUE, osnrEndOfFiber_dB));

   			 previousFiberInfo = Optional.of(infoToAdd);
   		 }
   	 }
   	 
   	 assert perFiberPerLpPerOlaIndexMetric_valStartEnd.keySet().containsAll(wNet.getFibers());
   	 assert wNet.getFibers().stream().allMatch(e->e.getTraversingLps().equals(perFiberPerLpPerOlaIndexMetric_valStartEnd.get(e).keySet()));
   	 assert wNet.getFibers().stream().allMatch(e->e.getTraversingLps().stream().allMatch(lp->perFiberPerLpPerOlaIndexMetric_valStartEnd.get(e).get(lp).size() == e.getNumberOfOpticalLineAmplifiersTraversed()));
   	 
   	 /* Update the per lp information at add and end */
   	 for (WLightpath lp : wNet.getLightpaths())
   	 {
   		 final double centralFrequency_hz = 1e12 * lp.getCentralFrequencyThz();
   		 final Map<PERLPINFOMETRICS,Double> vals = new HashMap<> ();
   		 final WFiber lastFiber = lp.getSeqFibers().get(lp.getSeqFibers().size()-1);
   		 final WNode lastOadm = lastFiber.getB();
   		 final double inputLastOadm_power_dBm = perFiberPerLpPerMetric_valStartEnd.get(lastFiber).get(lp).get(PERLPINFOMETRICS.POWER_DBM).getSecond();
   		 final double inputLastOadm_cd_psPerNm = perFiberPerLpPerMetric_valStartEnd.get(lastFiber).get(lp).get(PERLPINFOMETRICS.CD_PERPERNM).getSecond();
   		 final double inputLastOadm_pmdSquared_ps2 = perFiberPerLpPerMetric_valStartEnd.get(lastFiber).get(lp).get(PERLPINFOMETRICS.PMDSQUARED_PS2).getSecond();
   		 final double inputLastOadm_onsnr_dB = perFiberPerLpPerMetric_valStartEnd.get(lastFiber).get(lp).get(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW).getSecond();

   		 final double drop_power_dBm = inputLastOadm_power_dBm + lastFiber.getDestinationPreAmplifierGain_dB().orElse(0.0) - lastOadm.getOadmSwitchFabricAttenuation_dB();
   		 final double drop_cd_psPerNm = inputLastOadm_cd_psPerNm + lastFiber.getDestinationPreAmplifierCdCompensation_psPerNm().orElse(0.0);
   		 final double drop_pmdSquared_ps2 = inputLastOadm_pmdSquared_ps2 + Math.pow(lastFiber.getDestinationPreAmplifierPmd_ps().orElse(0.0) ,2) + Math.pow(lastOadm.getOadmSwitchFabricPmd_ps(),2) ;
   		 final double addedOsnrByDropOadm = osnrContributionEdfaRefBw12dot5GHz_linear(centralFrequency_hz, lastFiber.getDestinationPreAmplifierNoiseFactor_dB().orElse(-Double.MAX_VALUE), inputLastOadm_power_dBm);
   		 final double drop_osnr12_5RefBw_dB = osnrInDbUnitsAccummulation_dB(Arrays.asList(inputLastOadm_onsnr_dB , addedOsnrByDropOadm));

   		 vals.put(PERLPINFOMETRICS.POWER_DBM, drop_power_dBm);
   		 vals.put(PERLPINFOMETRICS.CD_PERPERNM, drop_cd_psPerNm);
   		 vals.put(PERLPINFOMETRICS.PMDSQUARED_PS2, drop_pmdSquared_ps2);
   		 vals.put(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW, drop_osnr12_5RefBw_dB);
   		 
   		 perLpPerMetric_valAtDropTransponderEnd.put(lp, vals);
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
			 for (int contOla = 0; contOla < fiber.getOlaGains_dB().size() ; contOla ++)
			 {
				 final double kmFromStartFiber = fiber.getAmplifierPositionsKmFromOrigin_km().get(contOla);
				 final double sumGainsTraversedAmplifiersBeforeThisOla_db = IntStream.range(0, contOla).mapToDouble(olaIndex -> fiber.getOlaGains_dB().get(olaIndex)).sum();
				 final double powerAtInputThisOla_dBm = powerAtStart_dBm - kmFromStartFiber * fiber.getAttenuationCoefficient_dbPerKm() + sumGainsTraversedAmplifiersBeforeThisOla_db;
				 final double powerAtOutputThisOla_dBm = powerAtInputThisOla_dBm + fiber.getOlaGains_dB().get(contOla);
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
    	return Collections.unmodifiableMap(perLpPerMetric_valAtDropTransponderEnd.get(lp));
    }
    public Map<PERLPINFOMETRICS , Pair<Double,Double>> getOpticalPerformanceOfLightpathAtAmplifierInputAndOutput (WLightpath lp , WFiber e , int olaIndex)
    {
    	if (olaIndex >= e.getNumberOfOpticalLineAmplifiersTraversed()) throw new Net2PlanException ("Wrong amplifier index");
    	if (!lp.getSeqFibers().contains(e)) throw new Net2PlanException ("Wrong amplifier fiber");
    	final Map<PERLPINFOMETRICS , Pair<Double,Double>> res = perFiberPerLpPerOlaIndexMetric_valStartEnd.getOrDefault(e , new TreeMap<> ()).getOrDefault(lp, new TreeMap<> ()).get(olaIndex); 
    	if (res == null) throw new Net2PlanException ("Unknown value");
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
		final List<Double> minOutputPowerDbm = e.getOlaMinAcceptableOutputPower_dBm();
		final List<Double> maxOutputPowerDbm = e.getOlaMaxAcceptableOutputPower_dBm();
		for (int cont = 0; cont < minOutputPowerDbm.size() ; cont ++)
		{
			final double outputPowerDbm = this.getTotalPowerAtAmplifierOutput_dBm(e, cont);
			if (outputPowerDbm < minOutputPowerDbm.get(cont)) return false;
			if (outputPowerDbm > maxOutputPowerDbm.get(cont)) return false;
		}
		return true;
    }
    
}
