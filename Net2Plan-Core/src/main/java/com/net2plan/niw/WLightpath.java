/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.niw.WNetConstants.WTYPE;

/** This class represents a unidirectional lightpath of a given line rate, being a main path or backup path of a lightpath request.
 * The lighptath is unregenerated, and has no wavelength conversion: the same set of optical slot are occupied in all the 
 * traversed fibers
 *
 */
public class WLightpath extends WAbstractNetworkElement
{
	private static final String ATTNAMECOMMONPREFIX = NIWNAMEPREFIX + "Lp_";
	private static final String ATTNAMESUFFIX_OCCUPIEDSLOTIDS = "occupiedSlotIds";
	private static final String ATTNAMESUFFIX_MODULATIONID = "modulationId";
	private static final String ATTNAMESUFFIX_ADDTRANSPONDERINJECTIONPOWER_DBM = "addTransponderInjectionPower_dBm";
	private static final String ATTNAMESUFFIX_RECEIVERMINIMUMPOWER_DBM = "receiverMinimumPower_dBm";
	private static final String ATTNAMESUFFIX_RECEIVERMAXIMUMPOWER_DBM = "receiverMaximumPower_dBm";
	private static final String ATTNAMESUFFIX_RECEIVERMAXIMUMCDABSOLUTEVALUE_PSPERNM = "receiverMaximumCdAbsoluteValue_psPerNM";
	private static final String ATTNAMESUFFIX_RECEIVERMAXIMUMPMD_PS = "receiverMaximumPmd_ps";
	private static final String ATTNAMESUFFIX_RECEIVERMINIMUMOSNR_DB = "receiverMinimumOsnr_dB";
	private static final String ATTNAMESUFFIX_ADDMODULEINDEXINORIGIN = "addModuleIndexInOrigin";
	private static final String ATTNAMESUFFIX_DROPMODULEINDEXINDESTINATION = "dropModuleIndexInDestination";

	WLightpath (Route r) { super (r, Optional.empty());  }

	@Override
	public Route getNe () { return (Route) associatedNpElement; }

	/** Returns the index of the add module in the origin OADM. Empty if origin node is not directionless, or if index stored is negative
	 * @return see above
	 */
	public Optional<Integer> getAddModuleIndexInOriginIfDirectionless () 
	{
		if (!getA().getOpticalSwitchingArchitecture().isDirectionLess()) return Optional.empty();
		final int index = getNe().getAttributeAsDouble (ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ADDMODULEINDEXINORIGIN, -1.0).intValue(); 
		return index < 0? Optional.empty() : Optional.of(index); 
	} 
	
	/** Returns the index of the drop module in the destination OADM. Empty if destination node is not directionless, or if index stored is negative
	 * @return see above
	 */
	public Optional<Integer> getDropModuleIndexInDestinationIfDirectionless () 
	{  
		if (!getB().getOpticalSwitchingArchitecture().isDirectionLess()) return Optional.empty();
		final int index = getNe().getAttributeAsDouble (ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_DROPMODULEINDEXINDESTINATION, -1.0).intValue(); 
		return index < 0? Optional.empty() : Optional.of(index); 
	} 

	/** Sets the index of the add module used by the lightpath in the origin node. Negative indexes mean no information
	 * @param index
	 */
	public void setAddModuleIndexInOriginIfDirectionless (Optional<Integer> index) 
	{  
		getNe().setAttribute (ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ADDMODULEINDEXINORIGIN, index.isPresent()? -1 : index.get()); 
	} 
	
	/** Sets the index of the drop module used by the lightpath in the destination node. Negative indexes mean no information
	 * @param index
	 */
	public void setDropModuleIndexInDestinationIfDirectionless (Optional<Integer> index) 
	{  
		getNe().setAttribute (ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_DROPMODULEINDEXINDESTINATION, index.isPresent()? -1 : index.get()); 
	} 
	
	/** The origin node of the lighptath, that must the origin node of the associated lightpath request
	 * @return see above
	 */
	public WNode getA () { return new WNode (getNe().getIngressNode()); }
	/** The destination node of the lighptath, that must the destination node of the associated lightpath request
	 * @return see above
	 */
	public WNode getB () { return new WNode (getNe().getEgressNode()); }
	/** Returns the associated lightpath request
	 * @return see above
	 */
	public WLightpathRequest getLightpathRequest () { return new WLightpathRequest(getNe().getDemand()); }

	/** Returns the nodes where this lightpath is optically switched: the sequence of nodes removing the first and the last
	 * @return see above
	 */
	public List<WNode> getNodesWhereThisLightpathIsExpressOpticallySwitched () { final List<WNode> seqNodes = getSeqNodes(); return new ArrayList<> (seqNodes.subList(1, seqNodes.size()-1)); }
	
	/** Returns the user-defined identifier of the modulation associated to the transponder realizing this lightpath
	 * @return see above
	 */
	public String getModulationId () { return getNe().getAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_MODULATIONID, ""); }
	/** Sets the user-defined identifier of the modulation associated to the transponder realizing this lightpath
	 * @param modulationId see above
	 */
	public void setModulationId (String modulationId) { getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_MODULATIONID, modulationId); }
	
	/** Returns the central frequency of this lightpath in THz
	 * @return see above
	 */
	public double getCentralFrequencyThz ()
	{
		final double centralSlot = (getOpticalSlotIds().first() + getOpticalSlotIds().last())/2.0;
		return WNetConstants.CENTRALFREQUENCYOFOPTICALSLOTZERO_THZ + WNetConstants.OPTICALSLOTSIZE_GHZ * 1e-3 * centralSlot; 
	}
	
	/** Returns the lowest frequency of this lightpath in THz
	 * @return see above
	 */
	public double getLowestFrequencyThz ()
	{
		final double centralSlot = getOpticalSlotIds().first() - WNetConstants.OPTICALSLOTSIZE_GHZ *0.5;
		return WNetConstants.CENTRALFREQUENCYOFOPTICALSLOTZERO_THZ + WNetConstants.OPTICALSLOTSIZE_GHZ * 1e-3 * centralSlot; 
	}
	
	/** Returns the highest frequency of this lightpath in THz
	 * @return see above
	 */
	public double getHighestFrequencyThz ()
	{
		final double centralSlot = getOpticalSlotIds().last() + WNetConstants.OPTICALSLOTSIZE_GHZ *0.5;
		return WNetConstants.CENTRALFREQUENCYOFOPTICALSLOTZERO_THZ + WNetConstants.OPTICALSLOTSIZE_GHZ * 1e-3 * centralSlot; 
	}

	//	private static final String ATTNAMESUFFIX_RECEIVERMAXIMUMPMD_PS = "receiverMaximumPmd_ps";
//	private static final String ATTNAMESUFFIX_RECEIVERMINIMUMOSNR_DB = "receiverMinimumOsnr_dB";


	/** Returns the tolerance of the receiver side in terms of chromatic dispersion value at the receiver, in absolute value. Defaults to -DOUBLE.MAX_VALUE
	 * @return see above
	 */
	public double getTransponderMaximumTolerableCdInAbsoluteValue_perPerNm () { return getNe().getAttributeAsDouble(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_RECEIVERMAXIMUMCDABSOLUTEVALUE_PSPERNM, WNetConstants.WLIGHTPATH_DEFAULT_MAXIMUMABSOLUTE_CD_PSPERNM); }
	/** Returns the tolerance of the receiver side in terms of OSNR at 12.5 GHz of reference bandwidth. Defaults to 20 dB
	 * @return see above
	 */
	public double getTransponderMinimumTolerableOsnrAt12_5GHzOfRefBw_dB () { return getNe().getAttributeAsDouble(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_RECEIVERMINIMUMOSNR_DB, WNetConstants.WLIGHTPATH_DEFAULT_MINIMUMACCEPTABLEOSNRAT12_5GHZREFBW_DB); }
	/** Returns the tolerance of the receiver side in terms of Polarization Mode Dispersion (PMD), in ps. Defaults to Double.MAX_VALUE
	 * @return see above
	 */
	public double getTransponderMaximumTolerablePmd_ps () { return getNe().getAttributeAsDouble(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_RECEIVERMAXIMUMPMD_PS, WNetConstants.WLIGHTPATH_DEFAULT_MAXIMUMPMD_PS); }

	/** Returns the tolerance of the receiver side in terms of chromatic dispersion value at the receiver, in absolute value. 
	 * @param cdTolerance_psPerNm see above
	 */
	public void setTransponderMaximumTolerableCdInAbsoluteValue_perPerNm (double cdTolerance_psPerNm) { getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_RECEIVERMAXIMUMCDABSOLUTEVALUE_PSPERNM, cdTolerance_psPerNm); }
	/** Sets the tolerance of the receiver side in terms of OSNR at a reference bandwidth of 12.5 GHz. 
	 * @param osnrTolerance_dB see above
	 */
	public void setTransponderMinimumTolerableOsnrAt12_5GHzOfRefBw_dB (double osnrTolerance_dB) { getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_RECEIVERMINIMUMOSNR_DB, osnrTolerance_dB); }
	/** Sets the tolerance of the receiver side in terms of Polarization Mode Dispersion (PMD), in ps. 
	 * @param pmdTolerance_ps see above
	 */
	public void setTransponderMaximumTolerablePmd_ps (double pmdTolerance_ps) { getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_RECEIVERMAXIMUMPMD_PS, pmdTolerance_ps); }

	
	
	/** Returns the injection power in dBm at the output of the ADD transponder, before entering the OADM switching matrix. Defaults to 0 dBm
	 * @return see above
	 */
	public double getAddTransponderInjectionPower_dBm () { return getNe().getAttributeAsDouble(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ADDTRANSPONDERINJECTIONPOWER_DBM, WNetConstants.WLIGHTPATH_DEFAULT_TRANSPONDERADDINJECTIONPOWER_DBM); }

	/** Returns the tolerance of the receiver side in terms of maximum power that can receive. Defaults to DOUBLE.MAX_VALUE
	 * @return see above
	 */
	public double getTransponderMaximumTolerableReceptionPower_dBm () { return getNe().getAttributeAsDouble(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_RECEIVERMAXIMUMPOWER_DBM, WNetConstants.WLIGHTPATH_DEFAULT_MAXIMUMACCEPTABLERECEPTIONPOWER_DBM); }

	/** Returns the tolerance of the receiver side in terms of minimum power that can receive. Defaults to -20 dBm 
	 * @return see above
	 */
	public double getTransponderMinimumTolerableReceptionPower_dBm () { return getNe().getAttributeAsDouble(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_RECEIVERMINIMUMPOWER_DBM, WNetConstants.WLIGHTPATH_DEFAULT_MINIMUMACCEPTABLERECEPTIONPOWER_DBM); }

	/** Sets the tolerance of the receiver side in terms of maximum power that can receive. 
	 * @param power_dBm see above
	 */
	public void setTransponderMaximumTolerableReceptionPower_dBm (double power_dBm) { getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_RECEIVERMAXIMUMPOWER_DBM, power_dBm); }

	/** Sets the tolerance of the receiver side in terms of minimum power that can receive. 
	 * @param power_dBm see above
	 */
	public void setTransponderMinimumTolerableReceptionPower_dBm (double power_dBm) { getNe().setAttribute(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_RECEIVERMINIMUMPOWER_DBM, power_dBm); }
	
	/** sets the injection power in dBm at the output of the ADD transponder, before entering the OADM switching matrix. 
	 * @param powerInDbm see above
	 */
	public void setAddTransponderInjectionPower_dBm (double powerInDbm) { getNe().setAttribute (ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_ADDTRANSPONDERINJECTIONPOWER_DBM, powerInDbm); }
	
	/** Indicates if this is a backup lightpath, of a main path in the same lightpath request
	 * @return see above
	 */
	public boolean isBackupLightpath () { return getNe().isBackupRoute(); }
	/** Indicates if this is a main lightpath that has a backup lightpath in the same lightpath request
	 * @return see above
	 */
	public boolean isMainLightpathWithBackupRoutes () { return getNe().hasBackupRoutes(); }

	/** Adds a backup lightpath to this lightpath
	 * @param backup see above
	 */
	public void addBackupLightpath (WLightpath backup) { getNe().addBackupRoute(backup.getNe ()); }
	
	/** Returns the list of lightpath designated to be backup of this one
	 * @return see above
	 */
	public List<WLightpath> getBackupLightpaths () { return getNe().getBackupRoutes().stream().map(rr->new WLightpath(rr)).collect(Collectors.toList()); }
	
	/** Returns the lightpaths from which this is backup, or an empty set
	 * @return see above
	 */
	public SortedSet<WLightpath> getPrimaryLightpathsOfThisBackupLightpath () 
	{
		if (!this.isBackupLightpath()) return new TreeSet<> ();
		return getNe().getRoutesIAmBackup().stream().map(rr->new WLightpath(rr)).collect(Collectors.toCollection(TreeSet::new));
	}

	
	/** Removes a backup lightpath 
	 * @param backup see above
	 */
	public void removeBackupLightpath (WLightpath backup) { getNe().removeBackupRoute(backup.getNe ()); }

	/** Sets this lightpath as a backup lightpath of other lightpath in this request 
	 * @param mainLp see above
	 */
	public void setAsBackupLightpath (WLightpath mainLp) { mainLp.addBackupLightpath(this); }
	
	/** Returns the sequence of fibers traversed by this lp
	 * @return see above
	 */
	public List<WNode> getSeqNodes () { return getNe().getSeqNodes().stream().map(e->new WNode(e)).collect(Collectors.toList()); }

	/** Returns the sequence of fibers traversed by this lp
	 * @return see above
	 */
	public List<WFiber> getSeqFibers () { return getNe().getSeqLinks().stream().map(e->new WFiber(e)).collect(Collectors.toList()); }
	/** Changes the sequence of fibers traversed by this lightpath
	 * @param newSeqFibers see above
	 */
	public void setSeqFibers (List<WFiber> newSeqFibers) { getNe().setSeqLinks(newSeqFibers.stream().map(ee->ee.getNe()).collect(Collectors.toList())); }
	/** Returns the set of optical slots occupied in all the traversed fibers of this lightpath
	 * @return see above
	 */
	public SortedSet<Integer> getOpticalSlotIds () { SortedSet<Integer> res = getAttributeAsSortedSetIntegerOrDefault(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_OCCUPIEDSLOTIDS , null);if (res == null) throw new RuntimeException (); return res;  }
	/** Changes the set of optical slots occupied in all the traversed fibers of this lightpath
	 * @param slotsIds see above
	 */
	public void setOpticalSlotIds (SortedSet<Integer> slotsIds) { getNe().setAttributeAsNumberList(ATTNAMECOMMONPREFIX + ATTNAMESUFFIX_OCCUPIEDSLOTIDS, new ArrayList<> (slotsIds)); }
	/** Returns the length in km of this lighptath, as the sum of the lengths of the traversed fibers
	 * @return see above
	 */
	public double getLengthInKm () { return getSeqFibers().stream().mapToDouble(e->e.getLengthInKm()).sum(); }
	/** Returns the propagation delay in ms of this lighptath, as the sum of the propagation delays of the traversed fibers
	 * @return see above
	 */
	public double getPropagationDelayInMs () { return getSeqFibers().stream().mapToDouble(e->e.getPropagationDelayInMs()).sum(); }
	
	/** Indicates if this lightpath is up (does not traverse any failed fiber/node)
	 * @return see above
	 */
	public boolean isUp () { return !getNe().isDown(); }

	/** Indicates if this lightpath is down (traverses a failed fiber/node)
	 * @return see above
	 */
	public boolean isDown () { return !this.isUp(); }

	/** Returns the number of optical slots occupied by the lightpath 
	 * @return see above
	 */
	public double getNumberOccupiedSlotIds () { return getOpticalSlotIds().size(); }

	/** Removes this lightpath
	 */
	public void remove () {  getNe().remove(); this.getLightpathRequest().updateNetPlanObjectAndPropagateUpwards(); }

	@Override
	public String toString () { return "LpUnreg(" + getLightpathRequest().getLineRateGbps() + "G) " + getA().getName() + "->" + getB().getName(); }

	@Override
	void checkConsistency()
	{
		if (this.wasRemoved()) return;
		assert getA().getAddedLigtpaths().contains(this);
		assert getB().getDroppedLigtpaths().contains(this);
		assert getLightpathRequest().getLightpaths().contains(this);
		if (this.isBackupLightpath()) assert !this.getPrimaryLightpathsOfThisBackupLightpath().isEmpty(); 
		if (this.isBackupLightpath()) assert this.getPrimaryLightpathsOfThisBackupLightpath().stream().allMatch(lp->lp.getBackupLightpaths().contains(this)); 
		if (this.isMainLightpathWithBackupRoutes()) assert !this.getBackupLightpaths().isEmpty();
		if (this.isMainLightpathWithBackupRoutes()) assert this.getBackupLightpaths().stream().allMatch(lp->lp.getPrimaryLightpathsOfThisBackupLightpath().contains(this));
		assert getSeqFibers().stream().allMatch(e->e.getTraversingLps().contains(this));
	}

	@Override
	public WTYPE getWType() { return WTYPE.WLightpath; }

}
