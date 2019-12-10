/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.niw.WNetConstants.WTYPE;
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
	private static final String ATTNAMECOMMONPREFIX = NIWNAMEPREFIX + "Fiber_";
	private static final String ATTNAMESUFFIX_ATTENUATIONCOEFFICIENTDBPERKM = "FiberAttenuationCoefficient_dbPerKm";
	private static final String ATTNAMESUFFIX_FIBERLINKDESIGNVALUEPMD_PSPERSQRKM = "FiberPmdCoef_psPerSqrKm";
	private static final String ATTNAMESUFFIX_FIBERCHROMATICDISPCOEFF_PSPERNMKM = "FiberCdCoef_psPerNmKm";

	
	private static final String ATTNAMESUFFIX_OPTICALLINEAMPLIFIERSINFO = "OpticalLineAmplifiersInfo";
	private static final String ATTNAMESUFFIX_ASIDE_BOOSTERAMPLIFIERINFO = "aSideBoosterAmplifierInfo";
	private static final String ATTNAMESUFFIX_BSIDE_PREAMPLIFIERINFO = "bSidePreAmplifierInfo";
	private static final String ATTNAMESUFFIX_ASIDE_EXISTSBOOSTERAMPLIFIER = "aSideExistsBoosterAmplifier";
	private static final String ATTNAMESUFFIX_BSIDE_EXISTSPREAMPLIFIER = "bSideExistsPreAmplifier";
	private static final String ATTNAMESUFFIX_ISOUTPUTSPECTRUMEQUALIZED = "isOutputSpectrumEqualized";
	private static final String ATTNAMESUFFIX_EQUALIZATIONTARGET_OUTPUTDSP_MWPERGHZ = "equalizationTarget_mwPerGhz";

	private static final String ATTNAMESUFFIX_VALIDOPTICALSLOTRANGES = "OpticalSlotRanges";
	private static final String ATTNAMESUFFIX_ARBITRARYPARAMSTRING = "ArbitraryString";
	private int numberAmplifiersToTraverse = 0;
	
	
	private WFiber (Optional<Integer> indexIfDummyFiber)
	{
		super(null , indexIfDummyFiber);
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
	}

	@Override
	public Link getNe()
	{
		return (Link) associatedNpElement;
	}

	/**
	 * Returns the origin node of the link
	 * @return see above
	 */
	public WNode getA()
	{
		return new WNode(getNe().getOriginNode());
	}

	/**
	 * Returns the destination node of the link
	 * @return see above
	 */
	public WNode getB()
	{
		return new WNode(getNe().getDestinationNode());
	}

	/**
	 * Returns the link length in km, as defined by the user
	 * @return see above
	 */
	public double getLengthInKm()
	{
		return getNe().getLengthInKm();
	}

	/**
	 * Returns the link propagation delay in ms
	 * @return see above
	 */
	public double getPropagationDelayInMs()
	{
		return getNe().getPropagationDelayInMs();
	}


	/**
	 * Sets the link length in km
	 * @param lenghtInKm see above
	 */
	public void setLenghtInKm(double lenghtInKm)
	{
		getNe().setLengthInKm(lenghtInKm);
	}

	/**
	 * Returns true if the fiber is part of a bidirectional fiber pair
	 * @return see above
	 */
	public boolean isBidirectional()
	{
		return getNe().isBidirectional();
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
		getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ATTENUATIONCOEFFICIENTDBPERKM, new Double(attenuationCoef_dbPerKm).toString());
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
		getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_FIBERLINKDESIGNVALUEPMD_PSPERSQRKM, new Double(pmdCoeff_psPerSqrKm).toString());
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
		getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_FIBERCHROMATICDISPCOEFF_PSPERNMKM, new Double(cdCoeff_psPerNmKm).toString());
	}

	/**
	 * If this fiber is part of a bidirectional fiber pair, returns the opposite fiber. If not returns null
	 * @return see above
	 */
	public WFiber getBidirectionalPair()
	{
		if (!this.isBidirectional()) throw new Net2PlanException("Not a bidirectional link");
		return new WFiber(getNe().getBidirectionalPair());
	}

	
	/** Returns the information associated to the optical line amplifiers in this fiber
	 * @return
	 */
	public List<OpticalAmplifierInfo> getOpticalLineAmplifiersInfo ()
	{
		final List<String> info = getAttributeAsListStringOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_OPTICALLINEAMPLIFIERSINFO, Arrays.asList());
		return info.stream().map(s->OpticalAmplifierInfo.createFromString(s).orElse(null)).filter(s->s!=null).collect(Collectors.toList());
	}

	/**
	 * Returns the current number of optical line amplifiers traversed
	 * @return see above
	 */
	public int getNumberOfOpticalLineAmplifiersTraversed()
	{
		return getOpticalLineAmplifiersInfo().size();
	}

	private List<Double> getList (String attribNameSuffix , double defaultValue)
	{
		final int numOlas = getNumberOfOpticalLineAmplifiersTraversed();
		final List<Double> res = getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + attribNameSuffix, Collections.nCopies(numOlas, defaultValue));
		return res.size() != numOlas? Collections.nCopies(numOlas, defaultValue) : res;
	}
	
	/**
	 * Sets the traversed amplifiers information 
	 * @param olas information of the olas
	 */
	public void setOlaTraversedInfo(List<OpticalAmplifierInfo> olas)
	{
		getNe ().setAttributeAsStringList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_OPTICALLINEAMPLIFIERSINFO, olas.stream().map(o->o.writeToString()).collect(Collectors.toList()));
	}

	/**
	 * Removes the traversed amplifiers in the fiber 
	 */
	public void removeOpticalLineAmplifiers()
	{
		setOlaTraversedInfo(Arrays.asList());
	}

	/**
	 * Sets the slot ranges that this fiber can propagate. This is a list of 2*K elements, being K the number of contiguous
	 * ranges. First number in a range is the id of the initial optical slot, second number is the end. For instance, a
	 * range setting [0 300 320 360] means that the fiber is able to propagate the optical slots from 0 to 300 both
	 * included, and from 320 to 360 both included.
	 * @param listInitialEndSlotRanges see above
	 */
	public final void setValidOpticalSlotRanges(List<Pair<Integer,Integer>> listInitialEndSlotRanges)
	{
		final SortedSet<Integer> cache_validSlotsIds = computeValidOpticalSlotIds(listInitialEndSlotRanges);
		final List<Integer> auxList = new ArrayList<> ();
		listInitialEndSlotRanges.forEach(p->{ auxList.add(p.getFirst()); auxList.add(p.getSecond()); } ); 
		getNe().setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_VALIDOPTICALSLOTRANGES, (List<Number>) (List<?>) auxList);
	}

	public double getAccumulatedChromaticDispersion_psPerNm ()
	{
		return this.getLengthInKm() * this.getChromaticDispersionCoeff_psPerNmKm() + this.getOpticalLineAmplifiersInfo().stream().mapToDouble(e->e.getCdCompensationPsPerNm()).sum();
	}
	
	/**
	 * Returns the valid optical slot ranges as defined by the user
	 * @return see above
	 */
	public final List<Pair<Integer,Integer>> getValidOpticalSlotRanges()
	{
		final List<Pair<Integer,Integer>> res = new ArrayList<> ();
		final List<Integer> auxList = getAttributeAsListDoubleOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_VALIDOPTICALSLOTRANGES, WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES_LISTDOUBLE).stream().map(ee -> ee.intValue()).collect(Collectors.toList());
		final Iterator<Integer> it = auxList.iterator();
		while (it.hasNext())
		{
			final int startRange = it.next();
			if (!it.hasNext()) throw new Net2PlanException("Invalid optical slot ranges");
			final int endRange = it.next();
			if (endRange < startRange) throw new Net2PlanException("Invalid optical slot ranges");
			res.add(Pair.of(startRange, endRange));
		}
		return res;
	}

	/** Returns the net gain of the fiber, considering optical line amplifiers and fiber attenuation
	 * @return see above
	 */
	public double getNetGain_dB ()
	{
		return getOpticalLineAmplifiersInfo().stream().mapToDouble(e->e.getGainDb()).sum() - getLengthInKm() * getAttenuationCoefficient_dbPerKm();
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

	static final SortedSet<Integer> computeValidOpticalSlotIds(List<Pair<Integer,Integer>> validOpticalSlotRanges)
	{
		final SortedSet<Integer> res = new TreeSet<>();
		for (Pair<Integer,Integer> p : validOpticalSlotRanges)
			for (int i = p.getFirst(); i <= p.getSecond(); i++)
				res.add(i);
		return res;
	}

	/**
	 * Returns the set of lightpaths that traverse this fiber.
	 * @return see above
	 */
	public SortedSet<WLightpath> getTraversingLps()
	{
		return getNe().getTraversingRoutes().stream().map(r -> new WLightpath(r)).collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * Returns the set of lightpaths that traverse this fiber
	 * @return see above
	 */
	public SortedSet<WLightpathRequest> getTraversingLpRequestsInAtLeastOneLp()
	{
		return getNe().getTraversingRoutes().stream().map(r -> new WLightpathRequest(r.getDemand())).collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * Returns true is the fiber is up, false otherwise
	 * @return see above
	 */
	public boolean isUp()
	{
		return getNe().isUp();
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
		getNe().setFailureState(true);
		for (WLightpathRequest lpReq : this.getTraversingLpRequestsInAtLeastOneLp())
			lpReq.updateNetPlanObjectAndPropagateUpwards();
	}

	/**
	 * Sets this fiber as down (failed). Traversing lighptaths will be set to failed also
	 */
	public void setAsDown()
	{
		getNe().setFailureState(false);
		for (WLightpathRequest lpReq : this.getTraversingLpRequestsInAtLeastOneLp())
			lpReq.updateNetPlanObjectAndPropagateUpwards();
	}

	/**
	 * Remove this fiber
	 */
	public void remove()
	{
		this.setAsDown();
		getNe().remove();
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
		for (WLightpath lp : getTraversingLps())
			assert lp.getSeqFibers().contains(this);
		assert getA().getOutgoingFibers().contains(this);
		assert getB().getIncomingFibers().contains(this);
		if (this.isDown()) assert getTraversingLps().stream().allMatch(lp->lp.isDown());
	}

	/** Returns the SRGs that this fiber belongs to, i.e. the ones that make this node fail
	 * @return see above
	 */
	public SortedSet<WSharedRiskGroup> getSrgsThisElementIsAssociatedTo ()
	{
		return getNe().getSRGs().stream().map(s->new WSharedRiskGroup(s)).collect(Collectors.toCollection(TreeSet::new));
	}

	@Override
	public WTYPE getWType() { return WTYPE.WFiber; }
	

	public boolean isOkAllGainsOfLineAmplifiers ()
	{
		return getOpticalLineAmplifiersInfo().stream().allMatch(o->o.isOkGainBetweenMargins());
	}

	
	/**
	 * Makes that this fiber to be tagged as the opposite one to other, and viceversa. 
	 * @param fiber see above
	 */
	public void  setBidirectionalPair(WFiber fiber)
	{
		if (fiber == null) throw new Net2PlanException("Please specify a not null fiber");
		if (!this.getA().equals(fiber.getB())) throw new Net2PlanException ("Fibers have no opposite end nodes");
		if (!this.getB().equals(fiber.getA())) throw new Net2PlanException ("Fibers have no opposite end nodes");
		removeBidirectionalPairRelation();
		getNe().setBidirectionalPair(fiber.getNe());
	}

	/**
	 * If this fiber has an opposite fiber associated, removes such association. If not, makes nothing
	 */
	public void removeBidirectionalPairRelation()
	{
		if (!this.isBidirectional()) return;
		getNe().setBidirectionalPair(null);
	}

	
	/** Returns the information of the booster amplifier at fiber origin if any.
	 * @return see above
	 */
	public Optional<OpticalAmplifierInfo> getOriginBoosterAmplifierInfo ()
	{
		if (!isExistingBoosterAmplifierAtOriginOadm()) return Optional.empty();
		final String s = getNe().getAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ASIDE_BOOSTERAMPLIFIERINFO);
		final OpticalAmplifierInfo info = OpticalAmplifierInfo.createFromString(s).orElse(OpticalAmplifierInfo.getDefaultBooster());
		return Optional.of (info);
	}
	/** Sets the information of the booster amplifier at fiber origin.
	 * @return see above
	 */
	public void setOriginBoosterAmplifierInfo (OpticalAmplifierInfo info)
	{
		getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ASIDE_BOOSTERAMPLIFIERINFO , info.writeToString());
	}
	
	/** Returns the information of the preamplifier in the link, if exists
	 * @return
	 */
	public Optional<OpticalAmplifierInfo> getDestinationPreAmplifierInfo ()
	{
		if (!isExistingPreamplifierAtDestinationOadm()) return Optional.empty();
		final String s = getNe().getAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_BSIDE_PREAMPLIFIERINFO);
		final OpticalAmplifierInfo info = OpticalAmplifierInfo.createFromString(s).orElse(OpticalAmplifierInfo.getDefaultPreamplifier());
		return Optional.of (info);
	}
	/** Sets the information of the preamplifier amplifier at fiber destination.
	 * @return see above
	 */
	public void setDestinationPreAmplifierInfo (OpticalAmplifierInfo info)
	{
		getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_BSIDE_PREAMPLIFIERINFO , info.writeToString());
	}

	/** Indicates if a pre-amplifier exists at the OADM at the end of the fiber.
	 * @return see above
	 */
	public boolean isExistingPreamplifierAtDestinationOadm ()
	{
		return getNe().getAttributeAsDouble(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_BSIDE_EXISTSPREAMPLIFIER, 0.0) == 1;
	}

	/** Indicates if a booster-amplifier exists at the OADM at the start of the fiber.
	 * @return see above
	 */
	public boolean isExistingBoosterAmplifierAtOriginOadm ()
	{
		return getNe().getAttributeAsDouble(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ASIDE_EXISTSBOOSTERAMPLIFIER, 0.0) == 1;
	}
	/** Sets if exists a pre-amplifier exists at the OADM at the end of the fiber.
	 * @param exists see above
	 */
	public void setIsExistingPreamplifierAtDestinationOadm (boolean exists)
	{
		getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_BSIDE_EXISTSPREAMPLIFIER, exists? 1 : 0);
		if (exists && !this.getDestinationPreAmplifierInfo().isPresent())
			this.setDestinationPreAmplifierInfo(OpticalAmplifierInfo.getDefaultPreamplifier());
	}
	/** Indicates if a booster-amplifier exists at the OADM at the start of the fiber.
	 * @param exists see above
	 */
	public void setIsExistingBoosterAmplifierAtOriginOadm (boolean exists)
	{
		getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ASIDE_EXISTSBOOSTERAMPLIFIER, exists? 1 : 0);
		if (exists && !this.getOriginBoosterAmplifierInfo().isPresent())
			this.setOriginBoosterAmplifierInfo(OpticalAmplifierInfo.getDefaultBooster());
	}
	
	/** Returns the spectral density enforced to all the channels by the origin OADM, right before the booster.
	 * @return see above
	 */
	public Optional<Double> getOriginOadmSpectrumEqualizationTargetBeforeBooster_mwPerGhz ()
	{
		if (getNe().getAttributeAsDouble (ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ISOUTPUTSPECTRUMEQUALIZED, 0.0).equals (0.0)) return Optional.empty();
		final Double val = getNe().getAttributeAsDouble (ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_EQUALIZATIONTARGET_OUTPUTDSP_MWPERGHZ, null);
		return Optional.ofNullable(val);
	}

	/** Indicates if the origin OADM is configured to equalize the power for this fiber, fixing the power density in all the channels right before the booster amplifier
	 * @return see above
	 */
	public boolean isOriginOadmConfiguredToEqualizeOutput ()
	{
		return getOriginOadmSpectrumEqualizationTargetBeforeBooster_mwPerGhz().isPresent();
	}

	/** Sets tthe spectral density enforced to all the channels by the origin OADM, right before the booster.
	 * @param valInMwPerGhz see above
	 */
	public void setOriginOadmSpectrumEqualizationTargetBeforeBooster_mwPerGhz (Optional<Double> valInMwPerGhz)
	{
		if (valInMwPerGhz.isPresent())
		{
			getNe().setAttribute (ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ISOUTPUTSPECTRUMEQUALIZED, 1);
			getNe().setAttribute (ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_EQUALIZATIONTARGET_OUTPUTDSP_MWPERGHZ, valInMwPerGhz.get());
		} else
		{
			getNe().setAttribute (ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ISOUTPUTSPECTRUMEQUALIZED, 0);
		}
	}
	

	
}
