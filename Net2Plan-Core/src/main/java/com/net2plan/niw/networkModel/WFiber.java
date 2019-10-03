/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw.networkModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.utils.Pair;

/**
 * This class represents a unidirectional WDM fiber between two OADM (Optical Add-Drop Multiplexer) network nodes
 * (optical DWM switches). The fiber can have a number of optical line amplifiers in user-defined positions. Some
 * physical information can be provided describing the fiber (e.g. attenuation, chromatic dispersion, PMD), and the
 * amplifiers. The fiber is supposed to be able to propagate one or more ranges of optical slots, and not be able to
 * propagate the rest.
 * 
 * 
 */
/**
 * @author Pablo
 *
 */
public class WFiber extends WAbstractNetworkElement
{
	private static final String ATTNAMECOMMONPREFIX = "Fiber_";
	private static final String ATTNAMESUFFIX_ATTENUATIONCOEFFICIENTDBPERKM = "FiberAttenuationCoefficient_dbPerKm";
	private static final String ATTNAMESUFFIX_FIBERLINKDESIGNVALUEPMD_PSPERSQRKM = "FiberPmdCoef_psPerSqrKm";
	private static final String ATTNAMESUFFIX_FIBERCHROMATICDISPCOEFF_PSPERNMKM = "FiberCdCoef_psPerNmKm";
	private static final String ATTNAMESUFFIX_AMPLIFIERPOSITIONFROMORIGINNODE_KM = "AmplifierPositionsFromOriginNode_km";
	private static final String ATTNAMESUFFIX_AMPLIFIERGAINS_DB = "AmplifierGains_dB";
	private static final String ATTNAMESUFFIX_AMPLIFIERMINGAINS_DB = "AmplifierMinGains_dB";
	private static final String ATTNAMESUFFIX_AMPLIFIERMAXGAINS_DB = "AmplifierMaxGains_dB";
	private static final String ATTNAMESUFFIX_AMPLIFIERMININPUTPOWER_DBM = "AmplifierMinInputPower_dBm";
	private static final String ATTNAMESUFFIX_AMPLIFIERMAXINPUTPOWER_DBM = "AmplifierMaxInputPower_dBm";

	
	private static final String ATTNAMESUFFIX_AMPLIFIERPMD_PS = "AmplifierPmd_ps";
	private static final String ATTNAMESUFFIX_AMPLIFIERCDCOMPENSARION_PSPERNM = "AmplifierPmd_cdCompensation_psPerNm";
	private static final String ATTNAMESUFFIX_AMPLIFIERNOISEFACTOR_DB = "AmplifierNF_dB";
	private static final String ATTNAMESUFFIX_VALIDOPTICALSLOTRANGES = "OpticalSlotRanges";
	private final Link e;
	private static final String ATTNAMESUFFIX_ARBITRARYPARAMSTRING = "ArbitraryString";
	private int numberAmplifiersToTraverse = 0;
	
	
	private WFiber (Optional<Integer> indexIfDummyFiber)
	{
		super(null , indexIfDummyFiber);
		this.e = null;
	}
	
	public static WFiber createDummyFiber (int indexIfDummyFiber)
	{
		return new WFiber (Optional.of(indexIfDummyFiber));
	}
	
	
	/**
	 * Each element can be associated a user defined param string. This method sets such string
	 * @param s the string
	 */
	public void setArbitraryParamString(String s)
	{
		getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ARBITRARYPARAMSTRING, s);
	}

	/**
	 * Each element can be associated a user defined param string. This method gets such string
	 * @return see above
	 */
	public String getArbitraryParamString()
	{
		return getNe().getAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ARBITRARYPARAMSTRING, "");
	}

	public WFiber(Link e)
	{
		super(e , Optional.empty());
		this.e = e;
		assert !getA().isVirtualNode() && !getB().isVirtualNode();
	}

	@Override
	public Link getNe()
	{
		return e;
	}

	/**
	 * Returns the origin node of the link
	 * @return see above
	 */
	public WNode getA()
	{
		return new WNode(e.getOriginNode());
	}

	/**
	 * Returns the destination node of the link
	 * @return see above
	 */
	public WNode getB()
	{
		return new WNode(e.getDestinationNode());
	}

	/**
	 * Returns the link length in km, as defined by the user
	 * @return see above
	 */
	public double getLengthInKm()
	{
		return e.getLengthInKm();
	}

	/**
	 * Sets the link length in km
	 * @param lenghtInKm see above
	 */
	public void setLenghtInKm(double lenghtInKm)
	{
		e.setLengthInKm(lenghtInKm);
	}

	/**
	 * Returns true if the fiber is part of a bidirectional fiber pair
	 * @return see above
	 */
	public boolean isBidirectional()
	{
		return e.isBidirectional();
	}

	/**
	 * Returns the fiber user-defined attenutation coefficient in dB per km
	 * @return see above
	 */
	public double getAttenuationCoefficient_dbPerKm()
	{
		return getAttributeAsDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ATTENUATIONCOEFFICIENTDBPERKM, WNetConstants.WFIBER_DEFAULT_ATTCOEFFICIENTDBPERKM);
	}

	/**
	 * Sets the fiber attenuation coefficient in dB per km
	 * @param attenuationCoef_dbPerKm see above
	 */
	public void setAttenuationCoefficient_dbPerKm(double attenuationCoef_dbPerKm)
	{
		e.setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ATTENUATIONCOEFFICIENTDBPERKM, new Double(attenuationCoef_dbPerKm).toString());
	}

	/**
	 * Returns the PMD fiber design value in ps per square root of km
	 * @return see above
	 */
	public double getPmdLinkDesignValueCoeff_psPerSqrtKm()
	{
		return getAttributeAsDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_FIBERLINKDESIGNVALUEPMD_PSPERSQRKM, WNetConstants.WFIBER_DEFAULT_PMDCOEFF_PSPERSQRKM);
	}

	/**
	 * Sets the PMD fiber design value in ps per square root of km
	 * @param pmdCoeff_psPerSqrKm see above
	 */
	public void setPmdLinkDesignValueCoeff_psPerSqrtKm(double pmdCoeff_psPerSqrKm)
	{
		e.setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_FIBERLINKDESIGNVALUEPMD_PSPERSQRKM, new Double(pmdCoeff_psPerSqrKm).toString());
	}

	/**
	 * Returns the fiber chromatic dispersion coefficient, in ps per nm / km
	 * @return see above
	 */
	public double getChromaticDispersionCoeff_psPerNmKm()
	{
		return getAttributeAsDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_FIBERCHROMATICDISPCOEFF_PSPERNMKM, WNetConstants.WFIBER_DEFAULT_CDCOEFF_PSPERNMKM);
	}

	/**
	 * Sets the fiber chromatic dispersion coefficient, in ps per nm / km
	 * @param cdCoeff_psPerNmKm see above
	 */
	public void setChromaticDispersionCoeff_psPerNmKm(double cdCoeff_psPerNmKm)
	{
		e.setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_FIBERCHROMATICDISPCOEFF_PSPERNMKM, new Double(cdCoeff_psPerNmKm).toString());
	}

	/**
	 * If this fiber is part of a bidirectional fiber pair, returns the opposite fiber. If not returns null
	 * @return see above
	 */
	public WFiber getBidirectionalPair()
	{
		if (!this.isBidirectional()) throw new Net2PlanException("Not a bidirectional link");
		return new WFiber(e.getBidirectionalPair());
	}

	/**
	 * Gets the list of amplifier positions, measured as km from the link start
	 * @return see above
	 */
	public List<Double> getAmplifierPositionsKmFromOrigin_km()
	{
		return getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERPOSITIONFROMORIGINNODE_KM, new ArrayList<>());
	}

	/**
	 * Returns the current number of optical line amplifiers traversed
	 * @return see above
	 */
	public int getNumberOfOpticalLineAmplifiersTraversed()
	{
		return getAmplifierPositionsKmFromOrigin_km().size();
	}

	/**
	 * Get minimum possible gain of the amplifiers traversed, in the same order as they are traversed. Defaults to 15 dB
	 * @return see above
	 */
	public List<Double> getAmplifierMinAcceptableGains_dB()
	{
		return getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERMINGAINS_DB, Collections.nCopies(getNumberOfOpticalLineAmplifiersTraversed(), 15.0));
	}
	/**
	 * Get maximum possible gain of the amplifiers traversed, in the same order as they are traversed. Defaults to 30 dB
	 * @return see above
	 */
	public List<Double> getAmplifierMaxAcceptableGains_dB()
	{
		return getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERMAXGAINS_DB, Collections.nCopies(getNumberOfOpticalLineAmplifiersTraversed(), 30.0));
	}
	/**
	 * Get minimum possible total input power of the amplifiers traversed, in the same order as they are traversed. Defaults to -30 dBm
	 * @return see above
	 */
	public List<Double> getAmplifierMinAcceptableInputPower_dBm()
	{
		return getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERMININPUTPOWER_DBM, Collections.nCopies(getNumberOfOpticalLineAmplifiersTraversed(), -30.0));
	}
	/**
	 * Get maximum possible total input power of the amplifiers traversed, in the same order as they are traversed. Defaults to 10 dBm
	 * @return see above
	 */
	public List<Double> getAmplifierMaxAcceptableInputPower_dBm()
	{
		return getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERMAXINPUTPOWER_DBM, Collections.nCopies(getNumberOfOpticalLineAmplifiersTraversed(), 10.0));
	}

	
	
	/**
	 * Get gains of the amplifiers traversed, in the same order as they are traversed
	 * @return see above
	 */
	public List<Double> getAmplifierGains_dB()
	{
		return getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERGAINS_DB, Collections.nCopies(getNumberOfOpticalLineAmplifiersTraversed(), WNetConstants.WFIBER_DEFAULT_AMPLIFIERGAIN_DB.get(0)));
	}

	/**
	 * Gets the PMD in ps added to the signal by the amplifier
	 * @return see above
	 */
	public List<Double> getAmplifierPmd_ps()
	{
		return getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERPMD_PS, Collections.nCopies(getNumberOfOpticalLineAmplifiersTraversed(), WNetConstants.WFIBER_DEFAULT_AMPLIFIERPMD_PS.get(0)));
	}

	/**
	 * Gets the chromatic dispersion compensation in the amplifier (if any) in ps per nm
	 * @return see above
	 */
	public List<Double> getAmplifierCdCompensarion_psPerNm ()
	{
		return getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERCDCOMPENSARION_PSPERNM, Collections.nCopies(getNumberOfOpticalLineAmplifiersTraversed(), WNetConstants.WFIBER_DEFAULT_AMPLIFIERCDCOMPENSATION.get(0)));
	}


	/**
	 * Gets the list of amplifier noise factors in dB
	 * @return see above
	 */
	public List<Double> getAmplifierNoiseFactor_dB()
	{
		return getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERNOISEFACTOR_DB, Collections.nCopies(getNumberOfOpticalLineAmplifiersTraversed(), WNetConstants.WFIBER_DEFAULT_AMPLIFIERNOISEFACTOR_DB.get(0)));
	}

	/**
	 * Sets the traversed amplifiers information (passing null in a parameter leaves it as it is)
	 * @param positionFromLinkOrigin_km list of positions in km, counting from the link origin
	 * @param gains_db for each amplifier, the gain in dB applied
	 * @param noiseFactors_dB for each amplifier, the ASE noise factor added
	 * @param pmd_ps for each amplifier, the PMD factor applied
	 * @param cdCompensarion_psPerNm for each amplifier, the chromatic dispersion compensation in each amplifier
	 * @param minimumGain_dB minimum gain in dB configurable for the amplifier
	 * @param maximumGain_dB maximum gain in dB configurable for the amplifier
	 * @param minimumInputPower_dBm minimum total input power at amplifier inputs acceptable for the amplifier
	 * @param maximumInputPower_dBm maximum total input power at amplifier inputs acceptable for the amplifier
	 */
	public void setAmplifiersTraversedInfo(List<Double> positionFromLinkOrigin_km, 
			List<Double> gains_db, 
			List<Double> noiseFactors_dB, 
			List<Double> pmd_ps , 
			List<Double> cdCompensation_psPerNm ,
			List<Double> minimumGain_dB , 
			List<Double> maximumGain_dB , 
			List<Double> minimumInputPower_dBm , 
			List<Double> maximumInputPower_dBm)
	{
		final int numAmplifiers = positionFromLinkOrigin_km.size();
		if (gains_db != null) if (gains_db.size() != numAmplifiers) throw new Net2PlanException("Wrong number of Amplifier Gains in dB");
		if (pmd_ps != null) if (pmd_ps.size() != numAmplifiers) throw new Net2PlanException("Wrong number of PMD values");
		if (noiseFactors_dB != null) if (noiseFactors_dB.size() != numAmplifiers) throw new Net2PlanException("Wrong number of noise factors");
		if (cdCompensation_psPerNm != null) if (cdCompensation_psPerNm.size() != numAmplifiers) throw new Net2PlanException("Wrong number of CD compensation values");
		if (positionFromLinkOrigin_km != null) if (positionFromLinkOrigin_km.stream().anyMatch(p -> p < 0 || p > getLengthInKm())) throw new Net2PlanException("Amplifier outside of position");

		if (minimumGain_dB != null) if (minimumGain_dB.size() != numAmplifiers) throw new Net2PlanException("Wrong number of Amplifier Minimum Gains");
		if (maximumGain_dB != null) if (maximumGain_dB.size() != numAmplifiers) throw new Net2PlanException("Wrong number of Amplifier Maximum Gains");
		if (minimumInputPower_dBm != null) if (minimumInputPower_dBm.size() != numAmplifiers) throw new Net2PlanException("Wrong number of Amplifier Minimum Input Power");
		if (maximumInputPower_dBm != null) if (maximumInputPower_dBm.size() != numAmplifiers) throw new Net2PlanException("Wrong number of Amplifier Maximum Input PowerB");
		
		
		if (positionFromLinkOrigin_km != null) e.setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERPOSITIONFROMORIGINNODE_KM, (List<Number>) (List<?>) positionFromLinkOrigin_km);
		if (gains_db != null) e.setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERGAINS_DB, (List<Number>) (List<?>) gains_db);
		if (noiseFactors_dB != null) e.setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERNOISEFACTOR_DB, (List<Number>) (List<?>) noiseFactors_dB);
		if (pmd_ps != null) e.setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERPMD_PS, (List<Number>) (List<?>) pmd_ps);
		if (cdCompensation_psPerNm != null) e.setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERCDCOMPENSARION_PSPERNM, (List<Number>) (List<?>) cdCompensation_psPerNm);
		if (minimumGain_dB != null) e.setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERMINGAINS_DB, (List<Number>) (List<?>) minimumGain_dB);
		if (maximumGain_dB != null) e.setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERMAXGAINS_DB, (List<Number>) (List<?>) maximumGain_dB);
		if (minimumInputPower_dBm != null) e.setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERMININPUTPOWER_DBM, (List<Number>) (List<?>) minimumInputPower_dBm);
		if (maximumInputPower_dBm != null) e.setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_AMPLIFIERMAXINPUTPOWER_DBM, (List<Number>) (List<?>) maximumInputPower_dBm);
	}

	/**
	 * Sets the slot ranges that this fiber can propagate. This is a list of 2*K elements, being K the number of contiguous
	 * ranges. First number in a range is the id of the initial optical slot, second number is the end. For instance, a
	 * range setting [0 300 320 360] means that the fiber is able to propagate the optical slots from 0 to 300 both
	 * included, and from 320 to 360 both included.
	 * @param listInitialEndSlotRanges see above
	 */
	public final void setValidOpticalSlotRanges(List<Integer> listInitialEndSlotRanges)
	{
		final SortedSet<Integer> cache_validSlotsIds = computeValidOpticalSlotIds(listInitialEndSlotRanges);
		e.setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_VALIDOPTICALSLOTRANGES, (List<Number>) (List<?>) listInitialEndSlotRanges);
	}

	public double getAccumulatedChromaticDispersion ()
	{
		return this.getLengthInKm() * this.getChromaticDispersionCoeff_psPerNmKm() + this.getAmplifierCdCompensarion_psPerNm().stream().mapToDouble(e->e).sum();
	}
	
	/**
	 * Returns the valid optical slot ranges as defined by the user
	 * @return see above
	 */
	public final List<Integer> getValidOpticalSlotRanges()
	{
		return getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_VALIDOPTICALSLOTRANGES, WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES).stream().map(ee -> ee.intValue()).collect(Collectors.toList());
	}

	/**
	 * Returns the set of all the optical slot ids usable in the fiber, according to the valid optical slot ranges defined
	 * by the user
	 * @return see above
	 */
	public final SortedSet<Integer> getValidOpticalSlotIds()
	{
		return computeValidOpticalSlotIds(getValidOpticalSlotRanges());
	}

	static final SortedSet<Integer> computeValidOpticalSlotIds(List<Integer> validOpticalSlotRanges)
	{
		final SortedSet<Integer> res = new TreeSet<>();
		final Iterator<Integer> it = validOpticalSlotRanges.iterator();
		while (it.hasNext())
		{
			final int startRange = it.next();
			if (!it.hasNext()) throw new Net2PlanException("Invalid optical slot ranges");
			final int endRange = it.next();
			if (endRange < startRange) throw new Net2PlanException("Invalid optical slot ranges");
			for (int i = startRange; i <= endRange; i++)
				res.add(i);
		}
		return res;
	}

	/**
	 * Returns the set of lightpaths that traverse this fiber.
	 * @return see above
	 */
	public SortedSet<WLightpathUnregenerated> getTraversingLps()
	{
		return e.getTraversingRoutes().stream().map(r -> new WLightpathUnregenerated(r)).collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * Returns the set of lightpaths that traverse this fiber
	 * @return see above
	 */
	public SortedSet<WLightpathRequest> getTraversingLpRequestsInAtLeastOneLp()
	{
		return e.getTraversingRoutes().stream().map(r -> new WLightpathRequest(r.getDemand())).collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * Returns true is the fiber is up, false otherwise
	 * @return see above
	 */
	public boolean isUp()
	{
		return e.isUp();
	}

	/**
	 * Returns true is the fiber is down, false otherwise
	 * @return see above
	 */
	public boolean isDown()
	{
		return !this.isUp();
	}


	/**
	 * Sets this fiber as up (not failed)
	 */
	public void setAsUp()
	{
		e.setFailureState(true);
		for (WLightpathRequest lpReq : this.getTraversingLpRequestsInAtLeastOneLp())
			lpReq.updateNetPlanObjectAndPropagateUpwards();
	}

	/**
	 * Sets this fiber as down (failed). Traversing lighptaths will be set to failed also
	 */
	public void setAsDown()
	{
		e.setFailureState(false);
		for (WLightpathRequest lpReq : this.getTraversingLpRequestsInAtLeastOneLp())
			lpReq.updateNetPlanObjectAndPropagateUpwards();
	}

	/**
	 * Remove this fiber
	 */
	public void remove()
	{
		this.setAsDown();
		e.remove();
	}

	/**
	 * Returns the minimum and maximum valid optical slot ids as defined by the user
	 * @return see above
	 */
	public Pair<Integer, Integer> getMinMaxValidSlotId()
	{
		final SortedSet<Integer> res = this.getValidOpticalSlotIds();
		return Pair.of(res.first(), res.last());
	}

	/**
	 * Returns the total number of valid optica slots in this fiber, as defined by the user
	 * @return see above
	 */
	public int getNumberOfValidOpticalChannels()
	{
		return getValidOpticalSlotIds().size();
	}

	@Override
	public String toString()
	{
		if (getNe () == null) return "Dummy fiber";
		return "Fiber " + getA().getName() + "->" + getB().getName();
	}

	@Override
	void checkConsistency()
	{
		if (this.wasRemoved()) return;
		if (this.isBidirectional())
			assert this.getBidirectionalPair().getBidirectionalPair().equals(this);
		for (WLightpathUnregenerated lp : getTraversingLps())
			assert lp.getSeqFibers().contains(this);
		assert getA().getOutgoingFibers().contains(this);
		assert getB().getIncomingFibers().contains(this);
		if (this.isDown()) assert getTraversingLps().stream().allMatch(lp->lp.isDown());
	}

}
