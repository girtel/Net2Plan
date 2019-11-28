/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
	public static class LpSignalState
	{
		private double power_dbm;
		private double cd_psPerNm;
		private double pmdSquared_ps2;
		private double osnrAt12_5GhzRefBw;
		
		@Override
		public String toString() 
		{
			return "LpSignalState [power_dbm=" + power_dbm + ", cd_psPerNm=" + cd_psPerNm + ", pmdSquared_ps2="
					+ pmdSquared_ps2 + ", osnrAt12_5GhzRefBw=" + osnrAt12_5GhzRefBw + "]";
		}
		public LpSignalState(double power_dbm, double cd_psPerNm, double pmdSquared_ps2, double osnrAt12_5GhzRefBw) {
			super();
			this.power_dbm = power_dbm;
			this.cd_psPerNm = cd_psPerNm;
			this.pmdSquared_ps2 = pmdSquared_ps2;
			this.osnrAt12_5GhzRefBw = osnrAt12_5GhzRefBw;
		}
		public LpSignalState() {
			super();
			this.power_dbm = -1;
			this.cd_psPerNm = -1;
			this.pmdSquared_ps2 = -1;
			this.osnrAt12_5GhzRefBw = -1;
		}
		public double getPower_dbm() {
			return power_dbm;
		}
		public double getCd_psPerNm() {
			return cd_psPerNm;
		}
		public double getPmdSquared_ps2() {
			return pmdSquared_ps2;
		}
		public double getOsnrAt12_5GhzRefBw() {
			return osnrAt12_5GhzRefBw;
		}
		public void setPower_dbm(double power_dbm) {
			this.power_dbm = power_dbm;
		}
		public void setCd_psPerNm(double cd_psPerNm) {
			this.cd_psPerNm = cd_psPerNm;
		}
		public void setPmdSquared_ps2(double pmdSquared_ps2) {
			this.pmdSquared_ps2 = pmdSquared_ps2;
		}
		public void setOsnrAt12_5GhzRefBw(double osnrAt12_5GhzRefBw) {
			this.osnrAt12_5GhzRefBw = osnrAt12_5GhzRefBw;
		}
		public LpSignalState getCopy () { return new LpSignalState(power_dbm, cd_psPerNm, pmdSquared_ps2, osnrAt12_5GhzRefBw); }
	}
	
	/** speed of light in m/s */
   public final static double constant_c = 299792458; 
   /** Planck constant m^2 kg/sec */
   public final static double constant_h = 6.62607004E-34; 
	private final WNet wNet;
	final private SortedMap<WLightpath,LpSignalState> perLpPerMetric_valAtDropTransponderEnd = new TreeMap<> ();
	final private SortedMap<WFiber,SortedMap<WLightpath,Pair<LpSignalState,LpSignalState>>> perFiberPerLpPerMetric_valStartAfterBoosterEndBeforePreampl = new TreeMap<> ();
	final private SortedMap<WFiber,Quadruple<Double,Double,List<Double>,List<Double>>> perFiberTotalPower_valStartEndAndAtEachOlaInputOutput = new TreeMap<> ();
	final private SortedMap<WFiber,SortedMap<WLightpath,SortedMap<Integer , Pair<LpSignalState,LpSignalState>>>> perFiberPerLpPerOlaIndexMetric_valStartAfterBoosterEndBeforePreampl = new TreeMap<> ();
	
	public OpticalSimulationModule (WNet wNet) 
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

    public OpticalSimulationModule updateAllPerformanceInfo ()
    {
    	System.out.println("Update all performance info");
   	 for (WFiber e : wNet.getFibers())
   	 {
   		 perFiberPerLpPerMetric_valStartAfterBoosterEndBeforePreampl.put(e, new TreeMap<> ());
   		 perFiberPerLpPerOlaIndexMetric_valStartAfterBoosterEndBeforePreampl.put(e, new TreeMap <> ());
   		 for (WLightpath lp : e.getTraversingLps())
   			perFiberPerLpPerOlaIndexMetric_valStartAfterBoosterEndBeforePreampl.get(e).put(lp, new TreeMap<> ());
   	 }
   	 for (WLightpath lp : wNet.getLightpaths())
   	 {
   		 final int numOpticalSlots = lp.getOpticalSlotIds().size();
   		 final double centralFrequency_hz = 1e12 * lp.getCentralFrequencyThz();
   		 Optional<Pair<LpSignalState,LpSignalState>> previousFiberInfo = Optional.empty();
   		 final List<WFiber> lpSeqFibers = lp.getSeqFibers();
   		 for (int contFiber = 0; contFiber < lpSeqFibers.size() ; contFiber ++)
   		 {
   			 final WFiber fiber = lpSeqFibers.get(contFiber);
   	   		 final boolean firstFiber = contFiber == 0;
   			 final Pair<LpSignalState,LpSignalState> infoToAdd = Pair.of(new LpSignalState(), new LpSignalState());
   			 final SortedMap<Integer , Pair<LpSignalState,LpSignalState>> infoToAddPerOla = new TreeMap<> ();
   			 perFiberPerLpPerMetric_valStartAfterBoosterEndBeforePreampl.get(fiber).put(lp, infoToAdd);
   			 perFiberPerLpPerOlaIndexMetric_valStartAfterBoosterEndBeforePreampl.get(fiber).put(lp, infoToAddPerOla);
   			
   			 final IOadmArchitecture oadm_a = fiber.getA().getOpticalSwitchingArchitecture();
   			 final LpSignalState state_startFiberBeforeBooster;
   			 final WFiber previousFiber = contFiber == 0? null : lpSeqFibers.get(contFiber-1);
   			 if (firstFiber)
   				state_startFiberBeforeBooster = oadm_a.getOutLpStateForAddedLp(new LpSignalState(lp.getAddTransponderInjectionPower_dBm() , 0.0, 0.0, Double.MAX_VALUE), lp.getDirectionlessAddModuleIndexInOrigin(), lp.getSeqFibers().get(0) , numOpticalSlots);
   			 else
   			 {
   				 final LpSignalState beforePreviousFiberEndPreampl = previousFiberInfo.get().getSecond();
   				 final LpSignalState afterPreviousFiberEndPreampl = previousFiber.isExistingPreamplifierAtDestinationOadm()? 
   						getStateAfterOpticalAmplifier (centralFrequency_hz , beforePreviousFiberEndPreampl , previousFiber.getDestinationPreAmplifierGain_dB().get() , previousFiber.getDestinationPreAmplifierCdCompensation_psPerNm().get() , previousFiber.getDestinationPreAmplifierPmd_ps().get() , previousFiber.getDestinationPreAmplifierNoiseFactor_dB().get()) : 
   							beforePreviousFiberEndPreampl.getCopy();
				state_startFiberBeforeBooster = oadm_a.getOutLpStateForExpressLp(afterPreviousFiberEndPreampl, previousFiber, fiber , numOpticalSlots);
   			 }
   			 final LpSignalState state_startFiberAfterBooster = fiber.isExistingBoosterAmplifierAtOriginOadm()? 
	   						getStateAfterOpticalAmplifier (centralFrequency_hz , state_startFiberBeforeBooster , fiber.getOriginBoosterAmplifierGain_dB().get() , fiber.getOriginBoosterAmplifierCdCompensation_psPerNm().get() , fiber.getOriginBoosterAmplifierPmd_ps().get() , fiber.getOriginBoosterAmplifierNoiseFactor_dB().get()) : 
	   							state_startFiberBeforeBooster.getCopy();
	   		infoToAdd.setFirst(state_startFiberAfterBooster);
			 LpSignalState stateOutputLastOlaOrInitialOadmAfterBooster = state_startFiberAfterBooster;
			 final int numOlas = fiber.getNumberOfOpticalLineAmplifiersTraversed();
			 for (int contOla = 0; contOla < numOlas ; contOla ++)
			 {
				 final double distFromLastOlaOrInitialOadm_km = fiber.getAmplifierPositionsKmFromOrigin_km().get(contOla) - (contOla == 0? 0 : fiber.getAmplifierPositionsKmFromOrigin_km().get(contOla-1));
				 assert distFromLastOlaOrInitialOadm_km >= 0;
				 final LpSignalState stateBeforeTheOla = getStateAfterFiberKm (stateOutputLastOlaOrInitialOadmAfterBooster , fiber , distFromLastOlaOrInitialOadm_km);
				 final LpSignalState stateAfterTheOla = getStateAfterOpticalAmplifier(centralFrequency_hz, stateBeforeTheOla, 
						 fiber.getOlaGains_dB().get(contOla), 
						 fiber.getOlaCdCompensation_psPerNm().get(contOla), 
						 fiber.getOlaPmd_ps().get(contOla), 
						 fiber.getOlaNoiseFactor_dB().get(contOla));
				 final Pair<LpSignalState,LpSignalState> infoThisOla = Pair.of(stateBeforeTheOla, stateAfterTheOla);
				infoToAddPerOla.put(contOla, infoThisOla);
				stateOutputLastOlaOrInitialOadmAfterBooster = stateAfterTheOla;
			 }
			 final double distFromLastOlaOrInitialOadm_km = fiber.getLengthInKm() - (numOlas == 0? 0 : fiber.getAmplifierPositionsKmFromOrigin_km().get(numOlas-1));
			 final LpSignalState stateAtTheEndOfFiberBeforePreamplifier = getStateAfterFiberKm (stateOutputLastOlaOrInitialOadmAfterBooster , fiber , distFromLastOlaOrInitialOadm_km);
	   		infoToAdd.setSecond(stateAtTheEndOfFiberBeforePreamplifier);
   			 previousFiberInfo = Optional.of(infoToAdd);
   		 }
   	 }
   	 
   	 assert perFiberPerLpPerOlaIndexMetric_valStartAfterBoosterEndBeforePreampl.keySet().containsAll(wNet.getFibers());
   	 assert wNet.getFibers().stream().allMatch(e->e.getTraversingLps().equals(perFiberPerLpPerOlaIndexMetric_valStartAfterBoosterEndBeforePreampl.get(e).keySet()));
   	 assert wNet.getFibers().stream().allMatch(e->e.getTraversingLps().stream().allMatch(lp->perFiberPerLpPerOlaIndexMetric_valStartAfterBoosterEndBeforePreampl.get(e).get(lp).size() == e.getNumberOfOpticalLineAmplifiersTraversed()));
   	 
   	 /* Update the per lp information at add and end */
   	 for (WLightpath lp : wNet.getLightpaths())
   	 {
   		 final double centralFrequency_hz = 1e12 * lp.getCentralFrequencyThz();
   		 final WFiber lastFiber = lp.getSeqFibers().get(lp.getSeqFibers().size()-1);
   		 final WNode lastOadm = lastFiber.getB();
   		 final LpSignalState state_beforePreamplLastFiber = perFiberPerLpPerMetric_valStartAfterBoosterEndBeforePreampl.get(lastFiber).get(lp).getSecond();
		 final LpSignalState state_afterPreamplLastFiber = lastFiber.isExistingPreamplifierAtDestinationOadm()? 
						getStateAfterOpticalAmplifier (centralFrequency_hz , state_beforePreamplLastFiber , lastFiber.getDestinationPreAmplifierGain_dB().get() , lastFiber.getDestinationPreAmplifierCdCompensation_psPerNm().get() , lastFiber.getDestinationPreAmplifierPmd_ps().get() , lastFiber.getDestinationPreAmplifierNoiseFactor_dB().get()) : 
							state_beforePreamplLastFiber.getCopy();
		final LpSignalState state_afterOadm = lastOadm.getOpticalSwitchingArchitecture().getOutLpStateForDroppedLp(state_afterPreamplLastFiber, lastFiber, lp.getDirectionlessDropModuleIndexInDestination());
   		 perLpPerMetric_valAtDropTransponderEnd.put(lp, state_afterOadm);
   	 }
   	 
   	 
   	 /* Update the total power per fiber */
   	 for (WFiber fiber : wNet.getFibers())
   	 {
   		 final double powerAtStart_dBm = linear2dB(fiber.getTraversingLps().stream().map(lp->perFiberPerLpPerMetric_valStartAfterBoosterEndBeforePreampl.get(fiber).get(lp).getFirst().getPower_dbm()).
   				 mapToDouble (v->dB2linear(v)).sum ());
   		 final double powerAtEnd_dBm = linear2dB(fiber.getTraversingLps().stream().map(lp->perFiberPerLpPerMetric_valStartAfterBoosterEndBeforePreampl.get(fiber).get(lp).getSecond().getPower_dbm()).
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
   		 perFiberTotalPower_valStartEndAndAtEachOlaInputOutput.put(fiber, Quadruple.of(powerAtStart_dBm, powerAtEnd_dBm , powerInputOla_dBm , powerOutputOla_dBm));
   	 }
   	 
   	 return this;
    }
        
    public List<Double> getTotalPowerAtAmplifierInputs_dBm (WFiber fiber)
    {
    	return perFiberTotalPower_valStartEndAndAtEachOlaInputOutput.get(fiber).getThird();
    }
    public List<Double> getTotalPowerAtAmplifierOutputs_dBm (WFiber fiber)
    {
    	return perFiberTotalPower_valStartEndAndAtEachOlaInputOutput.get(fiber).getFourth();
    }
    public double getTotalPowerAtAmplifierInput_dBm (WFiber fiber , int indexAmplifierInFiber)
    {
   	 if (indexAmplifierInFiber < 0 || indexAmplifierInFiber >= fiber.getNumberOfOpticalLineAmplifiersTraversed()) throw new Net2PlanException ("Wrong index");
   	 return perFiberTotalPower_valStartEndAndAtEachOlaInputOutput.get(fiber).getThird().get(indexAmplifierInFiber);
    }
    public double getTotalPowerAtAmplifierOutput_dBm (WFiber fiber , int indexAmplifierInFiber)
    {
   	 if (indexAmplifierInFiber < 0 || indexAmplifierInFiber >= fiber.getNumberOfOpticalLineAmplifiersTraversed()) throw new Net2PlanException ("Wrong index");
   	 return perFiberTotalPower_valStartEndAndAtEachOlaInputOutput.get(fiber).getFourth().get(indexAmplifierInFiber);
    }
    public Pair<LpSignalState,LpSignalState> getOpticalPerformanceOfLightpathAtFiberEnds (WFiber fiber , WLightpath lp)
    {
   	 if (!perFiberPerLpPerMetric_valStartAfterBoosterEndBeforePreampl.containsKey(fiber)) return null;
   	 if (!perFiberPerLpPerMetric_valStartAfterBoosterEndBeforePreampl.get(fiber).containsKey(lp)) return null;
   	 return perFiberPerLpPerMetric_valStartAfterBoosterEndBeforePreampl.get(fiber).get(lp);
    }
    public Pair<Double,Double> getTotalPowerAtFiberEnds_dBm (WFiber fiber)
    {
   	 if (!perFiberTotalPower_valStartEndAndAtEachOlaInputOutput.containsKey(fiber)) return null;
   	 return Pair.of(
   			perFiberTotalPower_valStartEndAndAtEachOlaInputOutput.get(fiber).getFirst (), 
   			perFiberTotalPower_valStartEndAndAtEachOlaInputOutput.get(fiber).getSecond ());
    }
    public LpSignalState getOpticalPerformanceAtTransponderReceiverEnd (WLightpath lp)
    {
    	return perLpPerMetric_valAtDropTransponderEnd.get(lp);
    }
    public Pair<LpSignalState,LpSignalState> getOpticalPerformanceOfLightpathAtAmplifierInputAndOutput (WLightpath lp , WFiber e , int olaIndex)
    {
    	if (olaIndex >= e.getNumberOfOpticalLineAmplifiersTraversed()) throw new Net2PlanException ("Wrong amplifier index");
    	if (!lp.getSeqFibers().contains(e)) throw new Net2PlanException ("Wrong amplifier fiber");
    	final  Pair<LpSignalState,LpSignalState> res = perFiberPerLpPerOlaIndexMetric_valStartAfterBoosterEndBeforePreampl.getOrDefault(e , new TreeMap<> ()).getOrDefault(lp, new TreeMap<> ()).get(olaIndex); 
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

    private static LpSignalState getStateAfterFiberKm (LpSignalState initialState , WFiber e , double kmOfFiberTraversed)
    {
    	final double power_dbm = initialState.getPower_dbm() - e.getAttenuationCoefficient_dbPerKm() * kmOfFiberTraversed;
    	final double cd_psPerNm = initialState.getCd_psPerNm() + e.getChromaticDispersionCoeff_psPerNmKm() * kmOfFiberTraversed;
    	final double pmdSquared_ps2 = initialState.getPmdSquared_ps2() + Math.pow(e.getPmdLinkDesignValueCoeff_psPerSqrtKm() , 2) * kmOfFiberTraversed;
    	return new LpSignalState(power_dbm, cd_psPerNm, pmdSquared_ps2, initialState.getOsnrAt12_5GhzRefBw());
    }
    private static LpSignalState getStateAfterOpticalAmplifier (double centralFrequency_hz , LpSignalState initialState , double gain_db , double cdCompensation_psPerNm , double pmd_ps , double noiseFigure_dB)
    {
    	final double power_dbm = initialState.getPower_dbm() + gain_db;
    	final double cd_psPerNm = initialState.getCd_psPerNm() + cdCompensation_psPerNm;
    	final double pmdSquared_ps2 = initialState.getPmdSquared_ps2() + Math.pow(pmd_ps , 2);
    	final double osnrContributedByAmplifier_dB = linear2dB(osnrContributionEdfaRefBw12dot5GHz_linear(centralFrequency_hz, noiseFigure_dB, initialState.getPower_dbm()));
    	final double osnrRefBw12_5_dB = osnrInDbUnitsAccummulation_dB (Arrays.asList(initialState.getOsnrAt12_5GhzRefBw() , osnrContributedByAmplifier_dB));
    	return new LpSignalState(power_dbm, cd_psPerNm, pmdSquared_ps2, osnrRefBw12_5_dB);
    }
    
}
