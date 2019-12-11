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


package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.DialogBuilder;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.InputForDialog;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.monitoring.MonitoringUtils;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.niw.IOadmArchitecture;
import com.net2plan.niw.OadmArchitecture_generic;
import com.net2plan.niw.OadmArchitecture_generic.Parameters;
import com.net2plan.niw.OpticalAmplifierInfo;
import com.net2plan.niw.OpticalSimulationModule;
import com.net2plan.niw.OpticalSpectrumManager;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WIpLink;
import com.net2plan.niw.WLightpathRequest;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNetConstants;
import com.net2plan.niw.WNetConstants.WTYPE;
import com.net2plan.niw.WNode;
import com.net2plan.utils.Pair;

/**
 */
@SuppressWarnings({ "unchecked", "serial" })
public class Niw_AdvancedJTable_link extends AdvancedJTable_networkElement<Link>
{
	private static final DecimalFormat df = new DecimalFormat("#.##");
	private static Function<Double,String> df2 = d -> d <= -Double.MAX_VALUE ? "-\u221E" : d >= Double.MAX_VALUE? "\u221E" : df.format(d);
	
    public Niw_AdvancedJTable_link(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.LINKS , layerThisTable.getName().equals(WNetConstants.ipLayerName)? "IP links" : "Fibers" , layerThisTable , true , e->e.isUp()? null : Color.RED);
    }

    @Override
  public List<AjtColumnInfo<Link>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
    	final List<AjtColumnInfo<Link>> res = new LinkedList<> ();
    	assert callback.getNiwInfo().getFirst();
    	final WNet wNet = callback.getNiwInfo().getSecond(); 
    	final boolean isIpLayer = getTableNetworkLayer().getName ().equals(WNetConstants.ipLayerName);
    	final boolean isWdmLayer = getTableNetworkLayer().getName ().equals(WNetConstants.wdmLayerName);
    	assert isIpLayer || isWdmLayer;
    	assert !(isIpLayer && isWdmLayer);
    	final Function<Link,WFiber> toWFiber = d -> (WFiber) wNet.getWElement(d).get();
    	final Function<Link,WIpLink> toWIpLink = d ->(WIpLink) wNet.getWElement(d).get();
    	final Map<Link,SortedMap<String,Pair<Double,Double>>> allLinksPerQosOccupationAndQosViolationMap = isIpLayer? callback.getDesign().getAllLinksPerQosOccupationAndQosViolationMap (getTableNetworkLayer()) : null;
    	
        res.add(new AjtColumnInfo<Link>(this, Boolean.class, null, "Show/hide", "Indicates whether or not the link is visible in the topology canvas", (n, s) -> {
            if ((Boolean) s) callback.getVisualizationState().showOnCanvas(n);
            else callback.getVisualizationState().hideOnCanvas(n);
        }, n -> !callback.getVisualizationState().isHiddenOnCanvas(n), AGTYPE.COUNTTRUE, null));
        res.add(new AjtColumnInfo<Link>(this , Node.class, null , "A", "Origin node", null , d->d.getOriginNode() , AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<Link>(this , Node.class, null , "B", "Destination node", null , d->d.getDestinationNode() , AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<Link>(this , Link.class, null , "Bidirectional pair", "If the link is bidirectional, provides its bidirectional pair", null , d->d.getBidirectionalPair() , AGTYPE.NOAGGREGATION, null));

    	if (isIpLayer)
    	{
		      res.add(new AjtColumnInfo<Link>(this , String.class, null , "Trav. QoS types" , "The QoS types of the traversing IP demands", null , d->allLinksPerQosOccupationAndQosViolationMap.getOrDefault(d, new TreeMap<> ()).entrySet().stream().filter(ee->ee.getValue().getFirst() > 0).map(ee->ee.getKey()).collect(Collectors.joining(",")) , AGTYPE.SUMDOUBLE , null));
		      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Total QoS violation (Gbps)" , "The total amount of IP link capacity that is being used outside the QoS contract, so the traffic using it would not be carried (and thus would be blocked) if the network applied a drop policy to it, or just if such extra capacity is not present in the link", null , d->toWIpLink.apply(d).isBundleMember()? "--" : allLinksPerQosOccupationAndQosViolationMap.getOrDefault(d, new TreeMap<> ()).values().stream().mapToDouble(ee->ee.getSecond()).sum() , AGTYPE.SUMDOUBLE , e->toWIpLink.apply(e).isBundleMember()? null : allLinksPerQosOccupationAndQosViolationMap.getOrDefault(e, new TreeMap<> ()).values().stream().mapToDouble(ee->ee.getSecond()).sum() == 0? null : Color.RED));
		      res.add(new AjtColumnInfo<Link>(this , String.class, null , "QoS scheduling" , "The scheduling configuration for the link QoS enforcement. For each QoS type, the priority assigned (lower better), and the maximum link utilization allowed for it", null , d->d.getQosTypePriorityAndMaxLinkUtilizationMap().toString() , AGTYPE.NOAGGREGATION, null));
		      res.add(new AjtColumnInfo<Link>(this , NetworkElement.class, null , "Lp request coupled", "The lightpath request that is coupled to this IP link, if any", null , d->toWIpLink.apply(d).isCoupledtoLpRequest()? toWIpLink.apply(d).getCoupledLpRequest().getNe () : null , AGTYPE.NOAGGREGATION , null));
		      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Nominal cap. (Gbps)", "Nominal capacity of the IP link. If a bundle, the sum of the nominal capacities of the members. If coupled to a lightpath, the lightpath line rate. If not, a user-defined value", (d,val)->{ final WIpLink e = toWIpLink.apply(d); if (!e.isBundleOfIpLinks() && !e.isCoupledtoLpRequest()) toWIpLink.apply(d).setNominalCapacityGbps((Double) val); }, d->toWIpLink.apply(d).getNominalCapacityGbps() , AGTYPE.SUMDOUBLE , null));
		      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Current cap. (Gbps)", "Current capacity of the IP link. If a bundle, the sum of the current capacities of the members. If coupled to a lightpath, the lightpath line rate if up, or zero if down.", null , d->toWIpLink.apply(d).getCurrentCapacityGbps() , AGTYPE.SUMDOUBLE , null));
		      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Occupied cap. (Gbps)", "Occupied capacity of the IP link.", null , d->toWIpLink.apply(d).isBundleMember()? "--" : toWIpLink.apply(d).getCarriedTrafficGbps() , AGTYPE.SUMDOUBLE , null));
		      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Utilization (%)", "IP link utilization (occupied capacity vs. current capacity)", null , d->toWIpLink.apply(d).isBundleMember()? "--" : toWIpLink.apply(d).getCurrentUtilization() , AGTYPE.MAXDOUBLE , null));
//		      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Utilization (%)", "IP link utilization (occupied capacity vs. current capacity)", null , d->toWIpLink.apply(d).getCurrentUtilization() , AGTYPE.MAXDOUBLE , null));
		      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Length (km)", "IP link length in km, considering the worst case WDM layer propagation, and worst case IP member propagation if is a bundle. Can be edited: the user-defined value would be then used if the IP link is not coupled to a lightpath, and is not a bundle", (d,val)->toWIpLink.apply(d).setLengthIfNotCoupledInKm((Double) val) , d->toWIpLink.apply(d).getWorstCaseLengthInKm()  , AGTYPE.SUMDOUBLE, null));
		      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Latency (ms)", "IP link propagation delay in ms, considering the worst case WDM layer propagation, and worst case IP member propagation if is a bundle.", null , d->toWIpLink.apply(d).getWorstCasePropagationDelayInMs() , AGTYPE.MAXDOUBLE, null));
		      res.add(new AjtColumnInfo<Link>(this , String.class, null , "Type", "Indicates if the link is a Link Aggregation Group of other IP links, or a LAG member, or a regular IP link (not a bundle of IP links, nor a LAG member).", null , d->toWIpLink.apply(d).isBundleOfIpLinks()? "LAG" : (toWIpLink.apply(d).isBundleMember()? "LAG-member" : "Regular IP link") , AGTYPE.NOAGGREGATION, null));
		      res.add(new AjtColumnInfo<Link>(this , NetworkElement.class, null , "Parent LAG", "If the IP link is member of a LAG, indicates the parent LAG IP link.", null , d->toWIpLink.apply(d).isBundleMember()? toWIpLink.apply(d).getBundleParentIfMember().getNe () : "--" , AGTYPE.NOAGGREGATION, null));
		      res.add(new AjtColumnInfo<Link>(this , Collection.class, null , "LAG members", "If the IP link is a LAG bundle, this column links to the LAG members.", null , d->toWIpLink.apply(d).isBundleOfIpLinks()? toWIpLink.apply(d).getBundledIpLinks().stream().map(e->e.getNe()).collect (Collectors.toList()) : "--" , AGTYPE.NOAGGREGATION, null));
		      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "IGP weight", "The strictly positive weight to be used for IGP routing calculations", (d,val)->toWIpLink.apply(d).setIgpWeight((Double) val) , d->toWIpLink.apply(d).isBundleMember()? "--" : toWIpLink.apply(d).getIgpWeight() , AGTYPE.NOAGGREGATION, null));
		      res.add(new AjtColumnInfo<Link>(this , Collection.class, null , "Trav. Unicast demands", "Unicast demands routed through this IP link (empty for bundle members)", null , d->toWIpLink.apply(d).getTraversingIpUnicastDemands().stream().map(e->e.getNe()).collect(Collectors.toList()) , AGTYPE.NOAGGREGATION, null));
		      res.add(new AjtColumnInfo<Link>(this , Collection.class, null , "Trav. IP connections", "IP source routed connections routed through this IP link (empty for bundle members)", null , d->toWIpLink.apply(d).getTraversingIpUnicastDemands().stream().map(e->e.getNe()).collect(Collectors.toList()) , AGTYPE.NOAGGREGATION, null));
		      res.add(new AjtColumnInfo<Link>(this , Collection.class, null , "Trav. SCs", "Service chains routed through this IP link (empty for bundle members)", null , d->toWIpLink.apply(d).getTraversingServiceChains().stream().map(e->e.getNe()).collect(Collectors.toList()) , AGTYPE.NOAGGREGATION, null));

		      res.addAll(AdvancedJTable_demand.getMonitoringAndTrafficEstimationColumns(this).stream().map(c->(AjtColumnInfo<Link>)(AjtColumnInfo<?>)c).collect(Collectors.toList()));
    	}
    	else
    	{
    		final WNet net = callback.getNiwInfo().getSecond();
    		final OpticalSpectrumManager ospec = callback.getNiwInfo().getThird();
    		final OpticalSimulationModule osim = callback.getNiwInfo().getFourth();
    		final SortedSet<WFiber> fibersInLasingLoops = new TreeSet<> ();
    		ospec.getUnavoidableLasingLoops().forEach (list->fibersInLasingLoops.addAll(list));
    		final SortedMap<WFiber,List<OpticalAmplifierInfo>> olas_e = new TreeMap<> ();
    		for (WFiber e : net.getFibers()) olas_e.put(e, e.getOpticalLineAmplifiersInfo());
    		
		      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Length (km)", "WDM link length in km", (d,val)->toWFiber.apply(d).setLenghtInKm((Double) val) , d->toWFiber.apply(d).getLengthInKm()  , AGTYPE.SUMDOUBLE, null));
		      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Latency (ms)", "WDM link latency in ms", null , d->toWFiber.apply(d).getPropagationDelayInMs()  , AGTYPE.MAXDOUBLE, null));
		      res.add(new AjtColumnInfo<Link>(this , Collection.class, null , "SRGs", "The SRGs that this fiber link belongs to", null , d->toWFiber.apply(d).getSrgsThisElementIsAssociatedTo().stream().map(e->e.getNe()).collect(Collectors.toList()) , AGTYPE.NOAGGREGATION , null));
		      res.add(new AjtColumnInfo<Link>(this , Boolean.class, null , "Up?", "", (d,val)->
		      {
		          final boolean isLinkUp = (Boolean) val;
		          if (isLinkUp) toWFiber.apply(d).setAsUp(); else toWFiber.apply(d).setAsDown(); 
		      } , d->toWFiber.apply(d).isUp() , AGTYPE.COUNTTRUE , e->e.isUp()? null : Color.RED));
		      //fibras..
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Freq. ranges (THz)", "The ranges of the frequencies that are valid in this fiber, e.g. because of affecting amplifiers limits and/or fiber limits", null , d->
    	      {
    	    	  return toWFiber.apply(d).getValidOpticalSlotRanges().stream().
    	    			  map(p->df2.apply(OpticalSimulationModule.getLowestFreqfSlotTHz(p.getFirst())) + " - " + df2.apply(OpticalSimulationModule.getHighestFreqfSlotTHz(p.getSecond()))).
    	    			  collect(Collectors.joining(", "));
    	      }, AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Spectrum (THz)", "The total available spectrum in the fiber to be used by WDM channels", null , d->
    	      {
    	    	  final int numChannels = toWFiber.apply(d).getNumberOfValidOpticalChannels();
    	    	  return df2.apply(numChannels * WNetConstants.OPTICALSLOTSIZE_GHZ / 1000.0);
    	      }, AGTYPE.NOAGGREGATION , null));
		      res.add(new AjtColumnInfo<Link>(this , Boolean.class, null , "In lasing loop?", "", null , d->fibersInLasingLoops.contains(toWFiber.apply(d)) , AGTYPE.COUNTTRUE , e->fibersInLasingLoops.contains(toWFiber.apply(e))? Color.RED : null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Coef. CD ps/nm/km", "Chromatic disperion coefficient in ps/nm/km, assumed to be the same in all the wavelengths", (d,val)->{ final WFiber e = toWFiber.apply(d); e.setChromaticDispersionCoeff_psPerNmKm((Double)val); }, d->toWFiber.apply(d).getChromaticDispersionCoeff_psPerNmKm() , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Coef. PMD-Q ps/sqrt(km)", "PMD fiber coefficient, typically called Link Design Value, or PMD-Q. Measured in ps per square root of km of fiber", (d,val)->{ final WFiber e = toWFiber.apply(d); e.setPmdLinkDesignValueCoeff_psPerSqrtKm((Double)val); }, d->toWFiber.apply(d).getPmdLinkDesignValueCoeff_psPerSqrtKm() , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Coef. Attenuation dB/km", "Fiber attenuation coefficient, measured in dB/km, assumed to be the same for all the wavelengths", (d,val)->{ final WFiber e = toWFiber.apply(d); e.setAttenuationCoefficient_dbPerKm((Double)val); }, d->toWFiber.apply(d).getAttenuationCoefficient_dbPerKm() , AGTYPE.NOAGGREGATION , null));

    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "# Valid slots", "Number of valid slots (each of " + WNetConstants.OPTICALSLOTSIZE_GHZ + " GHz) in this fiber", null , d->toWFiber.apply(d).getValidOpticalSlotIds().size() , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "# Occupied slots", "Number of occupied slots (each of " + WNetConstants.OPTICALSLOTSIZE_GHZ + " GHz) in this fiber", null , d->ospec.getOccupiedOpticalSlotIds(toWFiber.apply(d)).size() , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "# Idle slots", "Number of idle slots (each of " + WNetConstants.OPTICALSLOTSIZE_GHZ + " GHz) in this fiber", null , d->ospec.getIdleOpticalSlotIds(toWFiber.apply(d)).size() , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "# Clashing slots", "Number of slots (each of " + WNetConstants.OPTICALSLOTSIZE_GHZ + " GHz) occupied by two or more lightpaths, whose signal would be destroyed", null , d->ospec.getNumberOfClashingOpticalSlotIds(toWFiber.apply(d)) , AGTYPE.NOAGGREGATION , d->ospec.getNumberOfClashingOpticalSlotIds(toWFiber.apply(d)) > 0? Color.RED : null));
              res.add(new AjtColumnInfo<Link>(this , String.class, null , "Out power equalization (mW/GHz)", "If set, means that the power at the start of the fiber (before the booster, if any) is equalized by the WSS associated to this degree in the origin OADM. Then, here we indicate the power density enforced by the WSS inside the OADM switch fabric for this degree, and thus before the booster amplifier. The power is expressed as mW per GHz", null , d->toWFiber.apply(d).getOriginOadmSpectrumEqualizationTargetBeforeBooster_mwPerGhz().isPresent()? toWFiber.apply(d).getOriginOadmSpectrumEqualizationTargetBeforeBooster_mwPerGhz().get() : "No equalization", AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Max. to min. lp power density ratio (dB)", "The ratio between the highest and the lowest power density among the traversing lightpaths, measured at the start of this fiber (after the booster). If the fiber and amplifiers affect equally all the wavelengths, this ratio is supposed to be constant along the fiber", null , d->{ final Double v = osim.getMaxtoMinPerPowerDensityRatioAmongTraversingLightpathsAtFiberInput_dB(toWFiber.apply(d)).orElse(null); return v ==null? "--" : df2.apply(v); } , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Net gain (dB)" , "Net gain of this fiber link, considering effect of line amplifiers and fiber attenuation, but not considering booster or pre-amplifiers", null , d->toWFiber.apply(d).getNetGain_dB() , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Net CD (ps/nm)" , "Net accummulated chromatic dispersion in the WDM link, considering fiber CD coefficient, and potnetial compensation in the line amplifiers, but not considering any effect of booster or pre-amplifiers", null , d->toWFiber.apply(d).getAccumulatedChromaticDispersion_psPerNm() , AGTYPE.NOAGGREGATION , null));
    	
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Booster input power (dBm)", "Total power at the input of the booster amplifier", null , d->{ final WFiber ee = toWFiber.apply(d); if (!ee.getOriginBoosterAmplifierInfo().isPresent()) return "--"; return df2.apply(osim.getTotalPowerAtFiberEnds_dBm(ee).getFirst() - ee.getOriginBoosterAmplifierInfo().get().getGainDb()); } , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "WDM link input power (dBm)", "Total power at the start of the WDM link, the same as the output power of the booster amplifier if exists. In red if the booster exists and its power is not within appropriate margins", null , d->{ final WFiber ee = toWFiber.apply(d); return df2.apply(osim.getTotalPowerAtFiberEnds_dBm(ee).getFirst()); } , AGTYPE.NOAGGREGATION , d->{ final WFiber ee = toWFiber.apply(d); if (!ee.getOriginBoosterAmplifierInfo().isPresent()) return null; return osim.getTotalPowerAtFiberEnds_dBm(ee).getFirst() > ee.getOriginBoosterAmplifierInfo().get().getMaxAcceptableOutputPower_dBm()? Color.red : null; }));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "OLA input power (dBm)", "Total power at the input of the OLAs", null , d->osim.getTotalPowerAtAmplifierInputs_dBm(toWFiber.apply(d)).stream().map(e->df2.apply(e)).collect(Collectors.joining(" ")) , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "OLA output power (dBm)", "Total power at the output of the OLAs", null , d->osim.getTotalPowerAtAmplifierOutputs_dBm(toWFiber.apply(d)).stream().map(e->df2.apply(e)).collect(Collectors.joining(" ")) , AGTYPE.NOAGGREGATION , d->osim.isOkOpticalPowerAtAmplifierInputAllOlas(toWFiber.apply(d))? null : Color.RED));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "WDM link output power (dBm)", "Total power at the end of the WDM link, but before the pre-amplifier if any.", null , d->{ final WFiber ee = toWFiber.apply(d); return df2.apply(osim.getTotalPowerAtFiberEnds_dBm(ee).getSecond()); } , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Preamp. output power (dBm)", "Total power at the output of the pre-amplifier. In red if the preamplifier exists and its power is not within appropriate margins", null , d->{ final WFiber ee = toWFiber.apply(d); if (!ee.getDestinationPreAmplifierInfo().isPresent()) return "--"; return df2.apply(osim.getTotalPowerAtFiberEnds_dBm(ee).getSecond() + ee.getDestinationPreAmplifierInfo().get().getGainDb()); } , AGTYPE.NOAGGREGATION , d->{ final WFiber ee = toWFiber.apply(d); if (!ee.getDestinationPreAmplifierInfo().isPresent()) return null; return osim.getTotalPowerAtFiberEnds_dBm(ee).getSecond() + ee.getDestinationPreAmplifierInfo().get().getGainDb() > ee.getDestinationPreAmplifierInfo().get().getMaxAcceptableOutputPower_dBm()? Color.red : null; }));

              res.add(new AjtColumnInfo<Link>(this , Boolean.class, Arrays.asList("Optical amplifiers' params") , "Booster?", "Indicates if exists a booster amplifier at the start of this fiber", (d,val)->toWFiber.apply(d).setIsExistingBoosterAmplifierAtOriginOadm((Boolean)val) , d->toWFiber.apply(d).isExistingBoosterAmplifierAtOriginOadm(), AGTYPE.COUNTTRUE , null));
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Booster gain (dB)", "The gain of the booster amplifier at the start of the fiber", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingBoosterAmplifierAtOriginOadm()) ee.setOriginBoosterAmplifierInfo(ee.getOriginBoosterAmplifierInfo().get().setGainDb((Double)val));  } , d->toWFiber.apply(d).getOriginBoosterAmplifierInfo().isPresent()? toWFiber.apply(d).getOriginBoosterAmplifierInfo().get().getGainDb() : "--", AGTYPE.NOAGGREGATION , d->{ final OpticalAmplifierInfo i = toWFiber.apply(d).getOriginBoosterAmplifierInfo().orElse(null); if (i == null) return null; return i.isOkGainBetweenMargins()? null : Color.red;  })); 
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Booster noise factor (dB)", "The noise factor of the booster amplifier at the start of the fiber", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingBoosterAmplifierAtOriginOadm()) ee.setOriginBoosterAmplifierInfo(ee.getOriginBoosterAmplifierInfo().get().setNoiseFigureDb((Double)val));  } , d->toWFiber.apply(d).getOriginBoosterAmplifierInfo().isPresent()? toWFiber.apply(d).getOriginBoosterAmplifierInfo().get().getNoiseFigureDb() : "--", AGTYPE.NOAGGREGATION , null));
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Booster CD (ps/nm)", "The chromatic dispersion compensated at the booster amplifier at the start of the fiber", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingBoosterAmplifierAtOriginOadm()) ee.setOriginBoosterAmplifierInfo(ee.getOriginBoosterAmplifierInfo().get().setCdCompensationPsPerNm((Double)val));  } , d->toWFiber.apply(d).getOriginBoosterAmplifierInfo().isPresent()? toWFiber.apply(d).getOriginBoosterAmplifierInfo().get().getCdCompensationPsPerNm() : "--", AGTYPE.NOAGGREGATION , null));
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Booster PMD (ps)", "The PMD added of the booster amplifier at the start of the fiber", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingBoosterAmplifierAtOriginOadm()) ee.setOriginBoosterAmplifierInfo(ee.getOriginBoosterAmplifierInfo().get().setPmdPs((Double)val));  } , d->toWFiber.apply(d).getOriginBoosterAmplifierInfo().isPresent()? toWFiber.apply(d).getOriginBoosterAmplifierInfo().get().getPmdPs() : "--", AGTYPE.NOAGGREGATION , null));
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Booster Min Gain (dB)", "The minimum acceptable gain for the booster amplifier", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingBoosterAmplifierAtOriginOadm()) ee.setOriginBoosterAmplifierInfo(ee.getOriginBoosterAmplifierInfo().get().setMinAcceptableGainDb((Double)val));  } , d->toWFiber.apply(d).getOriginBoosterAmplifierInfo().isPresent()? toWFiber.apply(d).getOriginBoosterAmplifierInfo().get().getMinAcceptableGainDb() : "--", AGTYPE.NOAGGREGATION , null));
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Booster Max Gain (dB)", "The maximum acceptable gain for the booster amplifier", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingBoosterAmplifierAtOriginOadm()) ee.setOriginBoosterAmplifierInfo(ee.getOriginBoosterAmplifierInfo().get().setMaxAcceptableGainDb((Double)val));  } , d->toWFiber.apply(d).getOriginBoosterAmplifierInfo().isPresent()? toWFiber.apply(d).getOriginBoosterAmplifierInfo().get().getMaxAcceptableGainDb() : "--", AGTYPE.NOAGGREGATION , null));
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Booster Min Out power (dBm)", "The minimum acceptable output power for the booster amplifier", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingBoosterAmplifierAtOriginOadm()) ee.setOriginBoosterAmplifierInfo(ee.getOriginBoosterAmplifierInfo().get().setMinAcceptableOutputPower_dBm((Double)val));  } , d->toWFiber.apply(d).getOriginBoosterAmplifierInfo().isPresent()? toWFiber.apply(d).getOriginBoosterAmplifierInfo().get().getMinAcceptableOutputPower_dBm() : "--", AGTYPE.NOAGGREGATION , null));
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Booster Max Out power (dBm)", "The maximum acceptable output power for the booster amplifier", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingBoosterAmplifierAtOriginOadm()) ee.setOriginBoosterAmplifierInfo(ee.getOriginBoosterAmplifierInfo().get().setMaxAcceptableOutputPower_dBm((Double)val));  } , d->toWFiber.apply(d).getOriginBoosterAmplifierInfo().isPresent()? toWFiber.apply(d).getOriginBoosterAmplifierInfo().get().getMaxAcceptableOutputPower_dBm() : "--", AGTYPE.NOAGGREGATION , null));

              res.add(new AjtColumnInfo<Link>(this , Boolean.class, Arrays.asList("Optical amplifiers' params") , "Preamplifier?", "Indicates if exists a pre-amplifier at the end of this fiber", (d,val)->toWFiber.apply(d).setIsExistingPreamplifierAtDestinationOadm((Boolean)val) , d->toWFiber.apply(d).isExistingPreamplifierAtDestinationOadm(), AGTYPE.COUNTTRUE , null));
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Preamp. gain (dB)", "The gain of the pre-amplifier at the end of the fiber", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingPreamplifierAtDestinationOadm()) ee.setDestinationPreAmplifierInfo(ee.getDestinationPreAmplifierInfo().get().setGainDb((Double)val));  } , d->toWFiber.apply(d).getDestinationPreAmplifierInfo().isPresent()? toWFiber.apply(d).getDestinationPreAmplifierInfo().get().getGainDb() : "--", AGTYPE.NOAGGREGATION , d->{ final OpticalAmplifierInfo i = toWFiber.apply(d).getDestinationPreAmplifierInfo().orElse(null); if (i == null) return null; return i.isOkGainBetweenMargins()? null : Color.red;  }));
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Preamp. noise factor (dB)", "The noise factor of the pre-amplifier at the end of the fiber", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingPreamplifierAtDestinationOadm()) ee.setDestinationPreAmplifierInfo(ee.getDestinationPreAmplifierInfo().get().setNoiseFigureDb((Double)val));  } , d->toWFiber.apply(d).getDestinationPreAmplifierInfo().isPresent()? toWFiber.apply(d).getDestinationPreAmplifierInfo().get().getNoiseFigureDb() : "--", AGTYPE.NOAGGREGATION , null));
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Preamp. CD (ps/nm)", "The chromatic dispersion compensated at the pre-amplifier at the end of the fiber", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingPreamplifierAtDestinationOadm()) ee.setDestinationPreAmplifierInfo(ee.getDestinationPreAmplifierInfo().get().setCdCompensationPsPerNm((Double)val));  } , d->toWFiber.apply(d).getDestinationPreAmplifierInfo().isPresent()? toWFiber.apply(d).getDestinationPreAmplifierInfo().get().getCdCompensationPsPerNm() : "--", AGTYPE.NOAGGREGATION , null));
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Preamp. PMD (ps)", "The PMD added of the pre-amplifier at the end of the fiber", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingPreamplifierAtDestinationOadm()) ee.setDestinationPreAmplifierInfo(ee.getDestinationPreAmplifierInfo().get().setPmdPs((Double)val));  } , d->toWFiber.apply(d).getDestinationPreAmplifierInfo().isPresent()? toWFiber.apply(d).getDestinationPreAmplifierInfo().get().getPmdPs() : "--", AGTYPE.NOAGGREGATION , null));
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Preamp. Min Gain (dB)", "The minimum acceptable gain for the pre-amplifier at the end of the fiber", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingPreamplifierAtDestinationOadm()) ee.setDestinationPreAmplifierInfo(ee.getDestinationPreAmplifierInfo().get().setMinAcceptableGainDb((Double)val));  } , d->toWFiber.apply(d).getDestinationPreAmplifierInfo().isPresent()? toWFiber.apply(d).getDestinationPreAmplifierInfo().get().getMinAcceptableGainDb() : "--", AGTYPE.NOAGGREGATION , null));
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Preamp. Max Gain (dB)", "The maximum acceptable gain for the pre-amplifier at the end of the fiber", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingPreamplifierAtDestinationOadm()) ee.setDestinationPreAmplifierInfo(ee.getDestinationPreAmplifierInfo().get().setMaxAcceptableGainDb((Double)val));  } , d->toWFiber.apply(d).getDestinationPreAmplifierInfo().isPresent()? toWFiber.apply(d).getDestinationPreAmplifierInfo().get().getMaxAcceptableGainDb() : "--", AGTYPE.NOAGGREGATION , null));
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Preamp. Min Out power (dBm)", "The minimum acceptable output power for the pre-amplifier at the end of the fiber", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingPreamplifierAtDestinationOadm()) ee.setDestinationPreAmplifierInfo(ee.getDestinationPreAmplifierInfo().get().setMinAcceptableOutputPower_dBm((Double)val));  } , d->toWFiber.apply(d).getDestinationPreAmplifierInfo().isPresent()? toWFiber.apply(d).getDestinationPreAmplifierInfo().get().getMinAcceptableOutputPower_dBm() : "--", AGTYPE.NOAGGREGATION , null));
              res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "Preamp. Max Out power (dBm)", "The maximum acceptable output power for the pre-amplifier at the end of the fiber", (d,val)->{ final WFiber ee = toWFiber.apply(d); if (ee.isExistingPreamplifierAtDestinationOadm()) ee.setDestinationPreAmplifierInfo(ee.getDestinationPreAmplifierInfo().get().setMaxAcceptableOutputPower_dBm((Double)val));  } , d->toWFiber.apply(d).getDestinationPreAmplifierInfo().isPresent()? toWFiber.apply(d).getDestinationPreAmplifierInfo().get().getMaxAcceptableOutputPower_dBm() : "--", AGTYPE.NOAGGREGATION , null));

    	      res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "# OLAs", "Number of optical line amplifiers. Nota that each OLA can have chromatic dispersion compensation", null , d->toWFiber.apply(d).getNumberOfOpticalLineAmplifiersTraversed() , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "OLA pos (km)", "Positions of OLAs, in km from the fiber start", null , d->toWFiber.apply(d).getOpticalLineAmplifiersInfo().stream().map(e->df2.apply(e.getOlaPositionInKm().get())).collect(Collectors.joining(" ")) , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "OLA gains (dB)", "Gains in dB of the OLAs", null , d->toWFiber.apply(d).getOpticalLineAmplifiersInfo().stream().map(e->df2.apply(e.getGainDb())).collect(Collectors.joining(" ")) , AGTYPE.NOAGGREGATION , d->toWFiber.apply(d).isOkAllGainsOfLineAmplifiers()? null : Color.RED));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "OLA Min gains (dB)", "Minimum gains acceptable for the OLAs", null , d->toWFiber.apply(d).getOpticalLineAmplifiersInfo().stream().map(e->df2.apply(e.getMinAcceptableGainDb())).collect(Collectors.joining(" ")) , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "OLA Max gains (dB)", "Maximum gains acceptable for the OLAs", null , d->toWFiber.apply(d).getOpticalLineAmplifiersInfo().stream().map(e->df2.apply(e.getMaxAcceptableGainDb())).collect(Collectors.joining(" ")) , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "OLA NFs (dB)", "Noise factors in dB of the OLAs", null , d->toWFiber.apply(d).getOpticalLineAmplifiersInfo().stream().map(e->df2.apply(e.getNoiseFigureDb())).collect(Collectors.joining(" ")) , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "OLA CD (ps/nm)", "Chromatic dispersion compensation inside this OLA if any", null , d->toWFiber.apply(d).getOpticalLineAmplifiersInfo().stream().map(e->df2.apply(e.getCdCompensationPsPerNm())).collect(Collectors.joining(" ")) , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "OLA PMD (ps)", "PMD factor for this OLA", null , d->toWFiber.apply(d).getOpticalLineAmplifiersInfo().stream().map(e->df2.apply(e.getPmdPs())).collect(Collectors.joining(" ")) , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "OLA Min output power (dBm)", "Minimum acceptable power at the output for the OLAs", null , d->toWFiber.apply(d).getOpticalLineAmplifiersInfo().stream().map(e->df2.apply(e.getMinAcceptableOutputPower_dBm())).collect(Collectors.joining(" ")) , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Link>(this , Double.class, Arrays.asList("Optical amplifiers' params") , "OLA Max output power (dBm)", "Maximum acceptable power at the output for the OLAs", null , d->toWFiber.apply(d).getOpticalLineAmplifiersInfo().stream().map(e->df2.apply(e.getMaxAcceptableOutputPower_dBm())).collect(Collectors.joining(" ")) , AGTYPE.NOAGGREGATION , null));
    	}
      return res;
  	}

    @Override
    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
    	final NetPlan np = callback.getDesign();
        final List<AjtRcMenu> res = new ArrayList<> ();
    	assert callback.getNiwInfo().getFirst();
    	final WNet wNet = callback.getNiwInfo().getSecond(); 
    	final boolean isIpLayer = getTableNetworkLayer().getName ().equals(WNetConstants.ipLayerName);
    	final boolean isWdmLayer = getTableNetworkLayer().getName ().equals(WNetConstants.wdmLayerName);
    	assert isIpLayer || isWdmLayer;
    	assert !(isIpLayer && isWdmLayer);
    	final Function<Link,WFiber> toWFiber = d -> (WFiber) wNet.getWElement(d).get();
    	final Function<Link,WIpLink> toWIpLink = d ->(WIpLink) wNet.getWElement(d).get();
    	final Function<Link,Boolean> isWIpLink = d -> { final WTYPE y = wNet.getWType(d).orElse (null); return y == null? false : y.isIpLink(); };
		final Function<String,Optional<WNode>> nodeByName = st -> 
		{
    		WNode a = wNet.getNodeByName(st).orElse(null);
    		if (a == null) a = wNet.getNodes().stream().filter(n->n.getName().equalsIgnoreCase(st)).findFirst().orElse(null);
    		return Optional.ofNullable(a);
		};



    	if (isIpLayer)
    	{
            res.add(new AjtRcMenu("Add bidirectional IP link", e->
            {
              DialogBuilder.launch(
              "Add IP link" , 
              "Please introduce the information required.", 
              "", 
              this, 
              Arrays.asList(
              		InputForDialog.inputTfString("Input node name", "Introduce the name of the input node", 10, ""),
              		InputForDialog.inputTfString("End node name", "Introduce the name of the end node", 10, ""),
              		InputForDialog.inputTfDouble ("Line rate (Gbps)", "Introduce the line rate in Gbps of the IP link", 10, 100.0)
              		),
              (list)->
              	{
            		final String aName  = (String) list.get(0).get();
            		final String bName  = (String) list.get(1).get();
            		final double rateGbps = (Double) list.get(2).get();
            		final WNode a = nodeByName.apply(aName).orElse(null);
            		final WNode b = nodeByName.apply(bName).orElse(null);
            		if (a == null || b == null) throw new Net2PlanException("Unkown node name. " + (a == null? aName : bName));
            		wNet.addIpLinkBidirectional(a, b, rateGbps);
              	}
              );
            }
            , (a,b)->true, null));

            res.add(new AjtRcMenu("Link Aggregation Group (LAG) options", null , (a,b)->true, Arrays.asList(
            		new AjtRcMenu("Create LAGs bundling selected IP links", e->
                    {
                    	final Map<Pair<WNode,WNode>,SortedSet<WIpLink>> selectedNonBundlesLowHigh = new HashMap<> ();
                    	for (Link d : getSelectedElements())
                    	{
                    		final WIpLink ee = toWIpLink.apply(d);
                    		if (ee.isBundleOfIpLinks()) continue;
                    		if (ee.isBundleMember()) continue;
                    		assert ee.isBidirectional();
                    		final WIpLink ipLinkAb = ee.getId() < ee.getBidirectionalPair().getId()? ee : ee.getBidirectionalPair();
                    		final Pair<WNode,WNode> ab = Pair.of(ipLinkAb.getA(), ipLinkAb.getB());
                    		SortedSet<WIpLink> previousAbs = selectedNonBundlesLowHigh.get(ab);
                    		if (previousAbs == null)  { previousAbs = new TreeSet<> (); selectedNonBundlesLowHigh.put (ab , previousAbs); }
                    		previousAbs.add(ipLinkAb);
                    	}
                    	for (SortedSet<WIpLink> linksAb : selectedNonBundlesLowHigh.values())
                    	{
                    		if (linksAb.isEmpty()) continue;
                    		final WNode a = linksAb.first().getA();
                    		final WNode b = linksAb.first().getB();
                    		final Pair<WIpLink , WIpLink> lag = wNet.addIpLinkBidirectional(a, b, 0.0);
                    		lag.getFirst().setIpLinkAsBundleOfIpLinksBidirectional(linksAb);
                    	}
                    }
                    , (a,b)->true, null),
            		new AjtRcMenu("Convert selected links into LAGs, and create LAG members", e->
                    {
                        DialogBuilder.launch(
                                "Convert selected links into LAGs, and create LAG members" , 
                                "Indicate the capacity of each member (Gbps). The minimum number of members is created for each LAG, so the LAG capacity is at least equal to the required one.", 
                                "", 
                                this, 
                                Arrays.asList(
                                		InputForDialog.inputTfDouble("LAG member line rate (Gbps)", "All the LAG member will be of this rate. Enough number of them is created", 10, 100.0)
                                		),
                                (list)->
                                	{
                                		final Double rateGbps = (Double) list.get(0).get();
                                		if(rateGbps <= 0) throw new Net2PlanException("Member capacities must be positive");
                                    	for (Link d : getSelectedElements())
                                    	{
                                    		final WIpLink ee = toWIpLink.apply(d);
                                    		if (ee.isBundleMember()) continue;
                                    		if (ee.isBundleOfIpLinks()) continue;
                                    		final int numOfMembers = (int) Math.ceil(ee.getNominalCapacityGbps() / rateGbps);
                                    		final SortedSet<WIpLink> linkMembersAb = new TreeSet<> ();
                                    		while (linkMembersAb.size() < numOfMembers)
                                    			linkMembersAb.add(wNet.addIpLinkBidirectional(ee.getA(), ee.getB(), rateGbps).getFirst());
                                    		ee.setIpLinkAsBundleOfIpLinksBidirectional(linkMembersAb);
                                    	}
                                	}          		
                                );
                    }
                    , (a,b)->true, null),
            		new AjtRcMenu("Unbundle selected LAGs", e->
                    {
                    	for (Link d : getSelectedElements())
                    	{
                    		final WIpLink ee = toWIpLink.apply(d);
                    		if (!ee.isBundleOfIpLinks()) continue;
                    		ee.unbundleBidirectional();
                    	}
                    }
                    , (a,b)->true, null)
            		)));

            res.add(new AjtRcMenu("Remove selected IP links", e->getSelectedElements().forEach(dd->toWIpLink.apply(dd).removeBidirectional()) , (a,b)->b>0, null));
            res.add(new AjtRcMenu("Generate full-mesh", null , (a, b)->true, Arrays.asList( 
            		new AjtRcMenu("Link length as Euclidean distance", e->
            		{
            			for (WNode n1 : wNet.getNodes ())
            				for (WNode n2 : wNet.getNodes ())
            					if (n1.getId() < n2.getId ())
            					{
            						final Pair<WIpLink,WIpLink> p = wNet.addIpLinkBidirectional(n1, n2, 0.0);
            						p.getFirst().setLengthIfNotCoupledInKm(wNet.getNe().getNodePairEuclideanDistance(n1.getNe(), n2.getNe()));
            						p.getSecond().setLengthIfNotCoupledInKm(wNet.getNe().getNodePairEuclideanDistance(n1.getNe(), n2.getNe()));
            					}
            		} , (a, b)->true, null) , 
            		new AjtRcMenu("Link length as geographical distance", e->
            		{
            			for (WNode n1 : wNet.getNodes ())
            				for (WNode n2 : wNet.getNodes ())
            					if (n1.getId() < n2.getId ())
            					{
            						final Pair<WIpLink,WIpLink> p = wNet.addIpLinkBidirectional(n1, n2, 0.0);
            						p.getFirst().setLengthIfNotCoupledInKm(wNet.getNe().getNodePairHaversineDistanceInKm(n1.getNe(), n2.getNe()));
            						p.getSecond().setLengthIfNotCoupledInKm(wNet.getNe().getNodePairHaversineDistanceInKm(n1.getNe(), n2.getNe()));
            					}
            		} , (a, b)->true, null) 
            		)));
            res.add(new AjtRcMenu("Decouple selected IP links", e->getSelectedElements().stream().filter(dd->!toWIpLink.apply(dd).isBundleOfIpLinks ()).forEach(dd-> { toWIpLink.apply(dd).decoupleFromLightpathRequest(); toWIpLink.apply(dd).getBidirectionalPair().decoupleFromLightpathRequest(); }   ) , (a,b)->b>0, null) );

            res.add(new AjtRcMenu("Create & couple lightpath requests for uncoupled selected links", e->
            {
            	for (Link d : getSelectedElements())
            	{
            		final WIpLink ee = toWIpLink.apply(d);
            		if (ee.isBundleOfIpLinks()) continue;
            		assert ee.isBidirectional();
            		if (ee.isCoupledtoLpRequest()) continue;
            		if (ee.getBidirectionalPair().isCoupledtoLpRequest()) continue;
            		final WLightpathRequest lprAb = wNet.addLightpathRequest(ee.getA(), ee.getB(), ee.getNominalCapacityGbps(), false);
            		final WLightpathRequest lprBa = wNet.addLightpathRequest(ee.getB(), ee.getA(), ee.getNominalCapacityGbps(), false);
            		lprAb.setBidirectionalPair(lprBa);
            		ee.coupleToLightpathRequest(lprAb);
            		ee.getBidirectionalPair().coupleToLightpathRequest(lprBa);
            	}
            } , (a,b)->b>0, null));
            res.add(new AjtRcMenu("Couple IP link to lightpath request", e->
            {
            	final WIpLink ipLinkAb = toWIpLink.apply(getSelectedElements().first());
            	final List<WLightpathRequest> lprsAb = new ArrayList<> (wNet.getLightpathRequests().stream().
            			filter(ee->ee.getA().equals (ipLinkAb.getA())).
            			filter(ee->ee.getB().equals (ipLinkAb.getB())).
            			filter(ee->!ee.isCoupledToIpLink()).
            			filter(ee->ee.getLineRateGbps() == ipLinkAb.getNominalCapacityGbps()).
            			collect(Collectors.toCollection(TreeSet::new))); 
            	final List<WLightpathRequest> lprsBa = new ArrayList<> (wNet.getLightpathRequests().stream().
            			filter(ee->ee.getB().equals (ipLinkAb.getA())).
            			filter(ee->ee.getA().equals (ipLinkAb.getB())).
            			filter(ee->!ee.isCoupledToIpLink()).
            			filter(ee->ee.getLineRateGbps() == ipLinkAb.getNominalCapacityGbps()).
            			collect(Collectors.toCollection(TreeSet::new))); 
                DialogBuilder.launch(
                "Couple IP link to lightpath request" , 
                "Please introduce the information required.", 
                "", 
                this, 
                Arrays.asList(
                		InputForDialog.inputTfCombo("Lp request A-B", "Introduce the lightpath request to couple in direction A-B", 10, lprsAb.get(0), lprsAb, null, null),
                		InputForDialog.inputTfCombo("Lp request B-A", "Introduce the lightpath request to couple in direction B-A", 10, lprsBa.get(0), lprsBa, null, null)
                		),
                (list)->
                	{
              		final WLightpathRequest ab  = (WLightpathRequest) list.get(0).get();
              		final WLightpathRequest ba  = (WLightpathRequest) list.get(1).get();
              		ipLinkAb.coupleToLightpathRequest(ab);
              		ipLinkAb.getBidirectionalPair().coupleToLightpathRequest(ba);
                	}          		
                );
            } , (a,b)->b==1, null));
            res.add(new AjtRcMenu("Set selected links capacity", null , (a,b)->b>0, Arrays.asList(
            		new AjtRcMenu("As constant value", e->
                    {
                        DialogBuilder.launch(
                                "Set selected links nominal capacity" , 
                                "Please introduce the IP link nominal capacity. Negative values are not allowed. The capacity will be assigned to not coupled links", 
                                "", 
                                this, 
                                Arrays.asList(InputForDialog.inputTfDouble("IP link nominal capacity (Gbps)", "Introduce the link capacity", 10, 0.0)),
                                (list)->
                                	{
                                		final double newLinkCapacity = (Double) list.get(0).get();
                                		getSelectedElements().stream().map(ee->toWIpLink.apply(ee)).filter(ee->!ee.isCoupledtoLpRequest()).
                                			forEach (ee->{ ee.setNominalCapacityGbps (newLinkCapacity);  } );
                                	}
                                );
                    } , (a,b)->b>0, null),
            		new AjtRcMenu("To match a given utilization", e->
                    {
                        DialogBuilder.launch(
                                "Set selected links capacity to match utilization" , 
                                "Please introduce the link target utilization. Negative values are not allowed. The capacity will be assigned to not coupled links", 
                                "", 
                                this, 
                                Arrays.asList(
                                		InputForDialog.inputTfDouble("Link utilization", "Introduce the link utilization", 10, 0.9),
                                		InputForDialog.inputTfDouble("Capacity module (if > 0, capacities are multiple of this)", "Introduce the capacity module, so the link capacity will be the lowest multiple of this quantity that matches the required utilization limit. A non-positive value means no modular capacity is applied", 10, 100.0),
                                		InputForDialog.inputCheckBox("Bidirectional modules", "If checked, the module will have a capacity which is the largest between the traffic in both directions", true, null)
                                		),
                                (list)->
                                	{
                                		final double newLinkUtilization = (Double) list.get(0).get();
                                		final double capacityModule = (Double) list.get(1).get();
                                		final boolean isBidirectional = (Boolean) list.get(2).get();
                                		if (newLinkUtilization <= 0) throw new Net2PlanException ("Link utilization must be positive");
                                		getSelectedElements().stream().map(ee->toWIpLink.apply(ee)).filter(ee->!ee.isCoupledtoLpRequest()).forEach(ee->
                                		{
                                			double occupiedCap = isBidirectional && ee.isBidirectional ()? Math.max (ee.getCarriedTrafficGbps() , ee.getBidirectionalPair ().getCarriedTrafficGbps ()) : ee.getCarriedTrafficGbps ();
                                			if (newLinkUtilization > 0) occupiedCap /= newLinkUtilization;
                                			if (capacityModule > 0) occupiedCap = capacityModule * Math.ceil(occupiedCap / capacityModule);
                                			ee.setNominalCapacityGbps(occupiedCap);
                                		});
                                	}
                                );
                    } , (a,b)->b>0, null)
            		)));
            res.add(new AjtRcMenu("Set selected links length as", null , (a,b)->b>0, Arrays.asList(
            		new AjtRcMenu("Constant value", e->
                    {
                        DialogBuilder.launch(
                                "Set selected links length (km)" , 
                                "Please introduce the link length. Negative values are not allowed. The length will be assigned to not coupled links", 
                                "", 
                                this, 
                                Arrays.asList(InputForDialog.inputTfDouble("Link length (km)", "Introduce the link length", 10, 0.0)),
                                (list)->
                                	{
                                		final double newLinkLength = (Double) list.get(0).get();
                                		getSelectedElements().stream().map(ee->toWIpLink.apply(ee)).filter(ee->!ee.isCoupledtoLpRequest()).forEach(ee->ee.setLengthIfNotCoupledInKm(newLinkLength));
                                	}
                                );
                    } , (a,b)->b>0, null) , 
            		
            		new AjtRcMenu("Scaled version of current lengths", e->
                    {
                        DialogBuilder.launch(
                                "Scale selected links length (km)" , 
                                "Please introduce the scaling factor for which the link lengths will be multiplied. Negative values are not allowed. The length will be assigned to not coupled links", 
                                "", 
                                this, 
                                Arrays.asList(InputForDialog.inputTfDouble("Scaling factor", "Introduce the scaling factor", 10, 1.0)),
                                (list)->
                                	{
                                		final double scalingFactor = (Double) list.get(0).get();
                                		getSelectedElements().stream().map(ee->toWIpLink.apply(ee)).filter(ee->!ee.isCoupledtoLpRequest()).forEach(ee->ee.setLengthIfNotCoupledInKm(scalingFactor * ee.getLengthIfNotCoupledInKm()));
                                	}
                                );
                    } , (a,b)->b>0, null) ,         		
            		new AjtRcMenu("As the euclidean node pair distance", e->
                    {
                		getSelectedElements().stream().map(ee->toWIpLink.apply(ee)).filter(ee->!ee.isCoupledtoLpRequest()).forEach(ee->ee.setLengthIfNotCoupledInKm(np.getNodePairEuclideanDistance(ee.getNe().getOriginNode(), ee.getNe().getDestinationNode())));
                    } , (a,b)->b>0, null) ,
            		
            		new AjtRcMenu("As the geographical node pair distance", e->
                    {
                		getSelectedElements().stream().map(ee->toWIpLink.apply(ee)).filter(ee->!ee.isCoupledtoLpRequest()).forEach(ee->ee.setLengthIfNotCoupledInKm(np.getNodePairHaversineDistanceInKm(ee.getNe().getOriginNode(), ee.getNe().getDestinationNode())));
                    } , (a,b)->b>0, null)
            		)));

            res.add(new AjtRcMenu("Set IGP link weights of selected links", null , (a, b)->true, Arrays.asList( 
            		new AjtRcMenu("as constant value", e->
                    {
                        DialogBuilder.launch(
                                "Set IGP weight as constant value" , 
                                "Please introduce the IGP weight for the selected links. Non-positive values are not allowed.", 
                                "", 
                                this, 
                                Arrays.asList(InputForDialog.inputTfDouble("IGP weight", "Introduce the IGP weight for selected links", 10, 1.0)),
                                (list)->
                                	{
                                		final double newLinWeight = (Double) list.get(0).get();
                                		if (newLinWeight <= 0) throw new Net2PlanException ("IGP weights must be strictly positive");
                                		getSelectedElements().stream().map(ee->toWIpLink.apply(ee)).forEach(ee->ee.setIgpWeight(newLinWeight));
                                	}
                                );
                    } , (a,b)->b>0, null) , 
            		new AjtRcMenu("proportional to link latency", e->
                    {
                        DialogBuilder.launch(
                                "Set IGP weight proportional to latency" , 
                                "Please introduce the information required for computing the IGP weight.", 
                                "", 
                                this, 
                                Arrays.asList(
                                		InputForDialog.inputTfDouble("IGP weight to links of minimum latency", "Introduce the IGP weight to assign to the links of the minimum latency among the selected ones. IGP weight must be strictly positive.", 10, 1.0),
                                		InputForDialog.inputTfDouble("IGP weight to links of maximum latency", "Introduce the IGP weight to assign to the links of the maximum latency among the selected ones, IGP weight must be strictly positive.", 10, 10.0),
                                		InputForDialog.inputCheckBox("Round the weights to closest integer?", "If cheked, the weights will be rounded to the closest integer, with a minimum value of one.", true, null)
                                		),
                                (list)->
                                	{
                                    	final double minLatency = getSelectedElements().stream().mapToDouble(ee->toWIpLink.apply(ee).getWorstCasePropagationDelayInMs()).min().orElse(0.0);
                                    	final double maxLatency = getSelectedElements().stream().mapToDouble(ee->toWIpLink.apply(ee).getWorstCasePropagationDelayInMs()).max().orElse(0.0);
                                    	final double difLatency = maxLatency - minLatency;
                                		final double minLatencyWeight = (Double) list.get(0).get();
                                		final double maxLatencyWeight = (Double) list.get(1).get();
                                		final boolean roundToInteger = (Boolean) list.get(2).get();
                                		final double difWeight = maxLatencyWeight - minLatencyWeight;
                                		if (minLatencyWeight <= 0 || maxLatencyWeight <= 0) throw new Net2PlanException ("Weights must be positive");
                            			for (Link linkNp : getSelectedElements())
                            			{
                            				final WIpLink ee = toWIpLink.apply(linkNp);
                            				double linkWeight = difLatency == 0? minLatencyWeight : minLatencyWeight + difWeight * (ee.getWorstCasePropagationDelayInMs() - minLatency) / difLatency;  
                            				if (roundToInteger) linkWeight = Math.max(1, Math.round(linkWeight));
                            				if (linkWeight <= 0) throw new Net2PlanException ("Weights must be positive");
                            				ee.setIgpWeight(linkWeight);
                            			}
                                	}
                                );
                    } , (a,b)->b>0, null) ,
            		new AjtRcMenu("inversely proportional to capacity", e->
                    {
                        DialogBuilder.launch(
                                "Set IGP weight inversely proportional to link capacity" , 
                                "Please introduce the information required for computing the IGP weight.", 
                                "", 
                                this, 
                                Arrays.asList(
                                		InputForDialog.inputTfDouble("Reference bandwidth (to assign IGP weight one)", "Introduce the reference bandwidth (REFBW), measured in the same units as the traffic. IGP weight of link of capacity c is REFBW/c. REFBW must be positive", 10, 0.1),
                                		InputForDialog.inputCheckBox("Round the weights to closest integer?", "If cheked, the weights will be rounded to the closest integer, with a minimum value of one.", true, null)
                                		),
                                (list)->
                                	{
                                		final double refBw = (Double) list.get(0).get();
                                		final boolean roundToInteger = (Boolean) list.get(1).get();
                                		if (refBw <= 0) throw new Net2PlanException ("The reference bandwidth must be positive");
                            			for (Link eeNp : getSelectedElements())
                            			{
                            				final WIpLink ee = toWIpLink.apply(eeNp);
                            				double linkWeight = ee.getCurrentCapacityGbps() == 0? Double.MAX_VALUE : refBw / ee.getCurrentCapacityGbps();  
                            				if (roundToInteger) linkWeight = Math.max(1, Math.round(linkWeight));
                            				if (linkWeight <= 0) throw new Net2PlanException ("Weights must be positive");
                            				ee.setIgpWeight(linkWeight);
                            			}
                                	}
                                );
                    } , (a,b)->b>0, null) 
            		)));
            
            res.add(new AjtRcMenu("Monitor/forecast...",  null , (a,b)->true, Arrays.asList(
                    MonitoringUtils.getMenuAddSyntheticMonitoringInfo (this),
                    MonitoringUtils.getMenuExportMonitoringInfo(this),
                    MonitoringUtils.getMenuImportMonitoringInfo (this),
                    MonitoringUtils.getMenuSetMonitoredTraffic(this),
                    MonitoringUtils.getMenuSetTrafficPredictorAsConstantEqualToTrafficInElement (this),
                    MonitoringUtils.getMenuAddLinkMonitoringInfoSimulatingTrafficVariations (this),
                    MonitoringUtils.getMenuPercentileFilterMonitSamples (this) , 
                    MonitoringUtils.getMenuCreatePredictorTraffic (this),
                    MonitoringUtils.getMenuForecastDemandTrafficUsingGravityModel (this),
                    MonitoringUtils.getMenuForecastDemandTrafficFromLinkInfo (this),
                    MonitoringUtils.getMenuForecastDemandTrafficFromLinkForecast(this),
                    new AjtRcMenu("Remove traffic predictors of selected elements", e->getSelectedElements().forEach(dd->((Link)dd).removeTrafficPredictor()) , (a,b)->b>0, null),
                    new AjtRcMenu("Remove monitored/forecast stored information of selected elements", e->getSelectedElements().forEach(dd->((Link)dd).getMonitoredOrForecastedCarriedTraffic().removeAllValues()) , (a,b)->b>0, null),
                    new AjtRcMenu("Remove monitored/forecast stored information...", null , (a,b)->b>0, Arrays.asList(
                            MonitoringUtils.getMenuRemoveMonitorInfoBeforeAfterDate (this , true) ,
                            MonitoringUtils.getMenuRemoveMonitorInfoBeforeAfterDate (this , false)
                    		))
            		)));
            
    	} // if ipLayer
    	
    	if (isWdmLayer)
    	{
            res.add(new AjtRcMenu("Add fiber", e->
            {
              DialogBuilder.launch(
              "Add fiber" , 
              "Please introduce the information required.", 
              "", 
              this, 
              Arrays.asList(
              		InputForDialog.inputTfString("Input node name", "Introduce the name of the input node", 10, ""),
              		InputForDialog.inputTfString("End node name", "Introduce the name of the end node", 10, ""),
              		InputForDialog.inputCheckBox("Bidirectional?", "If checked, two fibers in opposite directions are created", true, null)
              		),
              (list)->
              	{
            		final String aName  = (String) list.get(0).get();
            		final String bName  = (String) list.get(1).get();
            		final boolean isBidirectional = (Boolean) list.get(2).get();
            		final WNode a = nodeByName.apply(aName).orElse(null);
            		final WNode b = nodeByName.apply(bName).orElse(null);
            		if (a == null || b == null) throw new Net2PlanException("Unkown node name. " + (a == null? aName : bName));
            		wNet.addFiber(a, b, WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES, -1.0 , isBidirectional);
              	}
              );
            }
            , (a,b)->true, null));

            res.add(new AjtRcMenu("Remove selected fibers", e->getSelectedElements().forEach(dd->toWFiber.apply(dd).remove()) , (a,b)->b>0, null));
            res.add(new AjtRcMenu("Generate full-mesh of fibers", e->
            		{
    			for (WNode n1 : wNet.getNodes ())
    				for (WNode n2 : wNet.getNodes ())
    					if (n1.getId() < n2.getId ())
    						wNet.addFiber(n1, n2, WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES, -1.0, true);
            		} , (a, b)->true, null));
            res.add(new AjtRcMenu("Set selected links length as", null , (a,b)->b>0, Arrays.asList(
            		new AjtRcMenu("Constant value", e->
                    {
                        DialogBuilder.launch(
                                "Set selected links length (km)" , 
                                "Please introduce the link length. Negative values are not allowed. The length will be assigned to not coupled links", 
                                "", 
                                this, 
                                Arrays.asList(InputForDialog.inputTfDouble("Link length (km)", "Introduce the link length", 10, 0.0)),
                                (list)->
                                	{
                                		final double newLinkLength = (Double) list.get(0).get();
                                		getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setLenghtInKm(newLinkLength));
                                	}
                                );
                    } , (a,b)->b>0, null) , 
            		
            		new AjtRcMenu("Scaled version of current lengths", e->
                    {
                        DialogBuilder.launch(
                                "Scale selected links length (km)" , 
                                "Please introduce the scaling factor for which the link lengths will be multiplied. Negative values are not allowed. The length will be assigned to not coupled links", 
                                "", 
                                this, 
                                Arrays.asList(InputForDialog.inputTfDouble("Scaling factor", "Introduce the scaling factor", 10, 1.0)),
                                (list)->
                                	{
                                		final double scalingFactor = (Double) list.get(0).get();
                                		getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setLenghtInKm(scalingFactor * ee.getLengthInKm()));
                                	}
                                );
                    } , (a,b)->b>0, null) ,         		
            		new AjtRcMenu("As the euclidean node pair distance", e->
                    {
                		getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setLenghtInKm(np.getNodePairEuclideanDistance(ee.getNe().getOriginNode(), ee.getNe().getDestinationNode())));
                    } , (a,b)->b>0, null) ,
            		
            		new AjtRcMenu("As the geographical node pair distance", e->
                    {
                		getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setLenghtInKm(np.getNodePairHaversineDistanceInKm(ee.getNe().getOriginNode(), ee.getNe().getDestinationNode())));
                    } , (a,b)->b>0, null)
            		)));

            res.add(new AjtRcMenu("Arrange selected fibers in bidirectional pairs", e->
            {
            	final SortedSet<WFiber> nonBidiLinks = getSelectedElements().stream().map(ee->toWFiber.apply(ee)).filter(ee->!ee.isBidirectional()).collect(Collectors.toCollection(TreeSet::new));
            	final Map<Pair<WNode,WNode> , WFiber> nodePair2link = new HashMap<>();
            	for (WFiber ee : nonBidiLinks)
            	{
            		final Pair<WNode,WNode> pair = Pair.of(ee.getA() , ee.getB());
            		if (nodePair2link.containsKey(pair)) throw new Net2PlanException ("At most one link per node pair is allowed");
            		nodePair2link.put(pair, ee);
            	}
            	for (WFiber ee : nonBidiLinks)
            	{
            		if (ee.isBidirectional()) continue;
            		final WFiber opposite = nodePair2link.get(Pair.of(ee.getB(), ee.getA()));
            		if (opposite == null) continue;
            		if (opposite.isBidirectional()) continue;
            		ee.setBidirectionalPair(opposite);
            	}
            }
            , (a,b)->b>0, null));
            res.add(new AjtRcMenu("Remove bidirectional relation of selected fibers", e-> getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.removeBidirectionalPairRelation()) , (a,b)->b>0, null));
            
            res.add(new AjtRcMenu("Set attenuation coef. dB/km to selected fibers", e-> 
            DialogBuilder.launch(
                    "Set attenuation coefficient (dB/km)" , 
                    "Please introduce the requested information", 
                    "", 
                    this, 
                    Arrays.asList(InputForDialog.inputTfDouble("Attenuation coefficient (dB/km)", "Introduce the attenuation coefficient", 10, WNetConstants.WFIBER_DEFAULT_ATTCOEFFICIENTDBPERKM)),
                    (list)->
                    	{
                    		final double value = (Double) list.get(0).get();
                    		getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setAttenuationCoefficient_dbPerKm(value));
                    	}
                    ) , (a,b)->b>0, null));

            res.add(new AjtRcMenu("Set chromatic dispersion coef. ps/nm/km to selected fibers", e-> 
            DialogBuilder.launch(
                    "Set chromatic dispersion coefficient (ps/nm/km)" , 
                    "Please introduce the requested information", 
                    "", 
                    this, 
                    Arrays.asList(InputForDialog.inputTfDouble("Chromatic dispersion coefficient (ps/nm/km)", "Introduce the chromatic dispersion coefficient", 10, WNetConstants.WFIBER_DEFAULT_CDCOEFF_PSPERNMKM)),
                    (list)->
                    	{
                    		final double value = (Double) list.get(0).get();
                    		getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setChromaticDispersionCoeff_psPerNmKm(value));
                    	}
                    ) , (a,b)->b>0, null));
            res.add(new AjtRcMenu("Set PMD link design value (ps/sqrt(km)) to selected fibers", e-> 
            DialogBuilder.launch(
                    "Set PMD link design value (ps/sqrt(km))" , 
                    "Please introduce the requested information", 
                    "", 
                    this, 
                    Arrays.asList(InputForDialog.inputTfDouble("Polarization Model Dispersion (PMD) coefficient (ps/sqrt(km))", "Introduce the PMD link design value.", 10, WNetConstants.WFIBER_DEFAULT_PMDCOEFF_PSPERSQRKM)),
                    (list)->
                    	{
                    		final double value = (Double) list.get(0).get();
                    		getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setChromaticDispersionCoeff_psPerNmKm(value));
                    	}
                    ) , (a,b)->b>0, null));
            res.add(new AjtRcMenu("Set valid optical slot ranges to selected fibers", e-> 
            DialogBuilder.launch(
                    "Set valid optical slot ranges " , 
                    "Please introduce the requested information. Each slot has a size of " + WNetConstants.OPTICALSLOTSIZE_GHZ + " GHz. Slot zero is centered at the frequency of " + WNetConstants.CENTRALFREQUENCYOFOPTICALSLOTZERO_THZ + " THz", 
                    "", 
                    this, 
                    Arrays.asList(InputForDialog.inputTfString("Space-separated indexes of the initial and final slots", "An even number of integers separated by spaces. Each pair of integers is the initial and end slot of a range of frequencies that is usable in this fiber.", 10, WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES_LISTDOUBLE.stream().map(ee->""+ee.intValue()).collect (Collectors.joining(" ")))),
                    (list)->
                    	{
                    		final String auxListSt = (String) list.get(0).get();
                    		final List<Integer> auxList = Stream.of(auxListSt.split(" ")).map(ee->Integer.parseInt(ee)).collect(Collectors.toList());
                    		final Iterator<Integer> it = auxList.iterator();
                    		final List<Pair<Integer,Integer>> listSlotsRanges = new ArrayList<> ();
                    		while (it.hasNext())
                    		{
                    			final int startRange = it.next();
                    			if (!it.hasNext()) throw new Net2PlanException("Invalid optical slot ranges");
                    			final int endRange = it.next();
                    			if (endRange < startRange) throw new Net2PlanException("Invalid optical slot ranges");
                    			listSlotsRanges.add(Pair.of(startRange, endRange));
                    		}
                    		getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setValidOpticalSlotRanges(listSlotsRanges));
                    	}
                    ) , (a,b)->b>0, null));

            res.add(new AjtRcMenu("Set fiber origin OADM pre-booster power equalization", e->
            {
            	final WFiber firstFiber = getSelectedElements().stream().map(ee->toWFiber.apply(ee)).findFirst().orElse(null);
            	if (firstFiber == null) return;
            	DialogBuilder.launch(
                    "SSet fiber origin OADM pre-booster power equalization to selected fibers" , 
                    "Please introduce the requested information." , 
                    "", 
                    this, 
                    Arrays.asList(
                    		InputForDialog.inputCheckBox("Optical equalization applied?", "Indicate if the optical equalization should be applied", false , null),
                    		InputForDialog.inputTfDouble("Pre-booster power density (mW per GHz)", "If the previous option is checked, the power density of the channels to be enforced for this degree by the origin OADM, right before the booster amplifier", 10, 1.0/50.0)
                    	),
                    (list)->
                    	{
                    		final Boolean equalize = (Boolean) list.get(0).get();
                    		final Double powerDensity = (Double) list.get(1).get();
                    		if (equalize)
                    			getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setOriginOadmSpectrumEqualizationTargetBeforeBooster_mwPerGhz(Optional.of(powerDensity)));
                    		else
                    			getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setOriginOadmSpectrumEqualizationTargetBeforeBooster_mwPerGhz(Optional.empty()));
                    	}
                    ); 
            } , (a,b)->b>0, null));
            
            res.add(new AjtRcMenu("Set fiber initial booster amplifier", null , (a,b)->b>0, Arrays.asList(
            		new AjtRcMenu("place a booster amplifier", e->getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setIsExistingBoosterAmplifierAtOriginOadm(true)) , (a,b)->b>0, null),
            		new AjtRcMenu("remove booster amplifier (if any)", e->getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setIsExistingBoosterAmplifierAtOriginOadm(false)) , (a,b)->b>0, null),
            		new AjtRcMenu("set booster gain (if any) to compensate OADM attenuation for express lightpaths", e->
    				{
    					for (WFiber ee: getSelectedElements().stream().map(ee->toWFiber.apply(ee)).filter(ee->ee.getOriginBoosterAmplifierInfo().isPresent()).collect(Collectors.toList()))
    					{
    						final OpticalAmplifierInfo info = ee.getOriginBoosterAmplifierInfo().get();
    						if (!(ee.getA ().getOpticalSwitchingArchitecture() instanceof OadmArchitecture_generic)) continue;
    						final Parameters oadmParam = ((OadmArchitecture_generic) ee.getA ().getOpticalSwitchingArchitecture()).getParameters();
    						final Double attenuatationExpressInputModule_dB = oadmParam.getExpressAttenuationForFiber0ToFiber0_dB().orElse(null);
    						if (attenuatationExpressInputModule_dB == null) continue;
    						info.setGainDb(attenuatationExpressInputModule_dB);
    						ee.setOriginBoosterAmplifierInfo(info);
    					}
    				}
   				 , (a,b)->b>0, null),
            		new AjtRcMenu("set booster amplification info to selected fibers", e->
                    {
                    	final WFiber firstFiber = getSelectedElements().stream().map(ee->toWFiber.apply(ee)).findFirst().orElse(null);
                    	final OpticalAmplifierInfo oaInfo = OpticalAmplifierInfo.getDefaultBooster ();
                    	if (firstFiber == null) return;
                    	DialogBuilder.launch(
                            "Set fiber initial node booster amplification info to selected fibers" , 
                            "Please introduce the requested information." , 
                            "", 
                            this, 
                            Arrays.asList(
                            		InputForDialog.inputCheckBox("Booster amplifier exists?", "Indicate if a booster amplifier exists at the start of this fiber", true , null),
                            		InputForDialog.inputTfDouble("Booster gain (dB)", "The gain of the booster amplifier, if exists", 10, oaInfo.getGainDb ()),
                            		InputForDialog.inputTfDouble("Booster noise factor (dB)", "The noise factor of the booster amplifier in dB, if exists", 10, oaInfo.getNoiseFigureDb()),
                            		InputForDialog.inputTfDouble("Booster CD compensation (ps/nm)", "The chromatic dispersion compensation of the booster amplifier in ps/nm, if exists", 10, oaInfo.getCdCompensationPsPerNm()),
                            		InputForDialog.inputTfDouble("Booster PMD (ps)", "The PMD added by the booster amplifier in ps, if exists", 10, oaInfo.getPmdPs()),
                            		InputForDialog.inputTfDouble("Booster Min acceptable gain (dB)", "The minimum acceptable gain (in dB) of the amplifier hardware, if exists", 10, oaInfo.getMinAcceptableGainDb()),
                            		InputForDialog.inputTfDouble("Booster Max acceptable gain (dB)", "The maximum acceptable gain (in dB) of the amplifier hardware, if exists", 10, oaInfo.getMaxAcceptableGainDb()),
                            		InputForDialog.inputTfDouble("Booster Min acceptable output power (dBm)", "The minimum acceptable output power (in dBm) of the amplifier hardware, if exists", 10, oaInfo.getMinAcceptableOutputPower_dBm()),
                            		InputForDialog.inputTfDouble("Booster Max acceptable output power (dBm)", "The maximum acceptable output power (in dBm) of the amplifier hardware, if exists", 10, oaInfo.getMaxAcceptableOutputPower_dBm())
                            	),
                            (list)->
                            	{
                            		final Boolean oaExists = (Boolean) list.get(0).get();
                            		final Double gainDb = (Double) list.get(1).get();
                            		final Double noisefigureDb = (Double) list.get(2).get();
                            		final Double cdPsNm = (Double) list.get(3).get();
                            		final Double pmdPs = (Double) list.get(4).get();
                            		final Double minGainnDb = (Double) list.get(5).get();
                            		final Double maxGainDb = (Double) list.get(6).get();
                            		final Double minPowerDbm = (Double) list.get(7).get();
                            		final Double maxPowerDbm = (Double) list.get(8).get();
                            		if (oaExists)
                            		{
                            			oaInfo.setGainDb(gainDb).
                            				setNoiseFigureDb(noisefigureDb).
                            				setCdCompensationPsPerNm(cdPsNm).
                            				setPmdPs(pmdPs).
                            				setMinAcceptableGainDb(minGainnDb).
                            				setMaxAcceptableGainDb(maxGainDb).
                            				setMinAcceptableOutputPower_dBm(minPowerDbm).
                            				setMaxAcceptableOutputPower_dBm(maxPowerDbm);
                            			getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setIsExistingBoosterAmplifierAtOriginOadm(true));
                            			getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setOriginBoosterAmplifierInfo(oaInfo));
                            		}
                            		else
                            			getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setIsExistingBoosterAmplifierAtOriginOadm(false));
                            	}
                            ); 
                    } , (a,b)->b>0, null)
            		)));

            res.add(new AjtRcMenu("Set fiber end node pre-amplification", null , (a,b)->b>0, Arrays.asList(
            		new AjtRcMenu("place a pre-amplifier", e->getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setIsExistingPreamplifierAtDestinationOadm(true)) , (a,b)->b>0, null),
            		new AjtRcMenu("remove pre-amplifier (if any)", e->getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setIsExistingPreamplifierAtDestinationOadm(false)) , (a,b)->b>0, null),
            		new AjtRcMenu("set pre-amplifier gain (if any) to compensate previous span losses", e->
            				{
            					for (WFiber ee: getSelectedElements().stream().map(ee->toWFiber.apply(ee)).filter(ee->ee.getDestinationPreAmplifierInfo().isPresent()).collect(Collectors.toList()))
            					{
            						final OpticalAmplifierInfo info = ee.getDestinationPreAmplifierInfo().get();
            						final double previousSpanLength = ee.getLengthInKm() - (ee.getOpticalLineAmplifiersInfo().isEmpty()? 0 : ee.getOpticalLineAmplifiersInfo().get(ee.getOpticalLineAmplifiersInfo().size()-1).getOlaPositionInKm().get());
            						info.setGainDb(Math.max(0, previousSpanLength) * ee.getAttenuationCoefficient_dbPerKm());
            						ee.setDestinationPreAmplifierInfo(info);
            					}
            				}
           				 , (a,b)->b>0, null),
            		new AjtRcMenu("set pre-amplification info to selected fibers", e->
                    {
                    	final WFiber firstFiber = getSelectedElements().stream().map(ee->toWFiber.apply(ee)).findFirst().orElse(null);
                    	if (firstFiber == null) return;
                    	final OpticalAmplifierInfo oaInfo = OpticalAmplifierInfo.getDefaultPreamplifier();
                    	DialogBuilder.launch(
                            "Set fiber end node pre-amplification info to selected fibers" , 
                            "Please introduce the requested information." , 
                            "", 
                            this, 
                            Arrays.asList(
                            		InputForDialog.inputCheckBox("Pre-amplifier exists?", "Indicate if a preamplifier amplifier exists at the start of this fiber", true , null),
                            		InputForDialog.inputTfDouble("Preamplifier gain (dB)", "The gain of the preamplifier, if exists", 10, oaInfo.getGainDb ()),
                            		InputForDialog.inputTfDouble("Preamplifier noise factor (dB)", "The noise factor of the preamplifier in dB, if exists", 10, oaInfo.getNoiseFigureDb()),
                            		InputForDialog.inputTfDouble("Preamplifier CD compensation (ps/nm)", "The chromatic dispersion compensation of the preamplifier in ps/nm, if exists", 10, oaInfo.getCdCompensationPsPerNm()),
                            		InputForDialog.inputTfDouble("Preamplifier PMD (ps)", "The PMD added by the preamplifier in ps, if exists", 10, oaInfo.getPmdPs()),
                            		InputForDialog.inputTfDouble("Preamplifier Min acceptable gain (dB)", "The minimum acceptable gain (in dB) of the amplifier hardware, if exists", 10, oaInfo.getMinAcceptableGainDb()),
                            		InputForDialog.inputTfDouble("Preamplifier Max acceptable gain (dB)", "The maximum acceptable gain (in dB) of the amplifier hardware, if exists", 10, oaInfo.getMaxAcceptableGainDb()),
                            		InputForDialog.inputTfDouble("Preamplifier Min acceptable output power (dBm)", "The minimum acceptable output power (in dBm) of the amplifier hardware, if exists", 10, oaInfo.getMinAcceptableOutputPower_dBm()),
                            		InputForDialog.inputTfDouble("Preamplifier Max acceptable output power (dBm)", "The maximum acceptable output power (in dBm) of the amplifier hardware, if exists", 10, oaInfo.getMaxAcceptableOutputPower_dBm())
                            	),
                            (list)->
                            	{
                            		final Boolean oaExists = (Boolean) list.get(0).get();
                            		final Double gainDb = (Double) list.get(1).get();
                            		final Double noisefigureDb = (Double) list.get(2).get();
                            		final Double cdPsNm = (Double) list.get(3).get();
                            		final Double pmdPs = (Double) list.get(4).get();
                            		final Double minGainnDb = (Double) list.get(5).get();
                            		final Double maxGainDb = (Double) list.get(6).get();
                            		final Double minPowerDbm = (Double) list.get(7).get();
                            		final Double maxPowerDbm = (Double) list.get(8).get();
                            		if (oaExists)
                            		{
                            			oaInfo.setGainDb(gainDb).
                            				setNoiseFigureDb(noisefigureDb).
                            				setCdCompensationPsPerNm(cdPsNm).
                            				setPmdPs(pmdPs).
                            				setMinAcceptableGainDb(minGainnDb).
                            				setMaxAcceptableGainDb(maxGainDb).
                            				setMinAcceptableOutputPower_dBm(minPowerDbm).
                            				setMaxAcceptableOutputPower_dBm(maxPowerDbm);
                            			getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setIsExistingPreamplifierAtDestinationOadm(true));
                            			getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setDestinationPreAmplifierInfo(oaInfo));
                            		}
                            		else
                            			getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setIsExistingPreamplifierAtDestinationOadm(false));
                            	}
                            ); 
                    } , (a,b)->b>0, null)
            		)));
            
            
            res.add(new AjtRcMenu("Optical line amplifiers", null , (a,b)->true, Arrays.asList(

            		new AjtRcMenu("Remove existing OLAs in selected fibers", e->
                    {
                    	final WFiber firstFiber = getSelectedElements().stream().map(ee->toWFiber.apply(ee)).findFirst().orElse(null);
                    	if (firstFiber == null) return;
                    	getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.removeOpticalLineAmplifiers());
                    } , (a,b)->b>0, null),

            		new AjtRcMenu("Set optical line amplifiers (OLA) info to selected fibers", e->
                    {
                    	final WFiber firstFiber = getSelectedElements().stream().map(ee->toWFiber.apply(ee)).findFirst().orElse(null);
                    	if (firstFiber == null) return;
                    	final List<OpticalAmplifierInfo> ffOlas = firstFiber.getOpticalLineAmplifiersInfo();
                    	DialogBuilder.launch(
                            "Set optical line amplifiers (OLA) info" , 
                            "Please introduce the requested information. All the lists are space-separated and with the same number of elements, one per optical line amplifier" , 
                            "", 
                            this, 
                            Arrays.asList(
                            		InputForDialog.inputTfString("OLA positions (km from fiber init)", "A space separated list, wiht as many elements as OLAs, and the OLA position in km from the fiber start point.", 10, ffOlas.stream().map(ee->df2.apply(ee.getOlaPositionInKm ().get())).collect(Collectors.joining(" "))),
                            		InputForDialog.inputTfString("OLA gains (dB)", "A space separated list, wiht as many elements as OLAs, and the OLA gains in dB.", 10, ffOlas.stream().map(ee->df2.apply(ee.getGainDb ())).collect(Collectors.joining(" "))),
                            		InputForDialog.inputTfString("OLA noise factors (dB)", "A space separated list, wiht as many elements as OLAs, and the OLA noise factors in dB.", 10, ffOlas.stream().map(ee->df2.apply(ee.getNoiseFigureDb ())).collect(Collectors.joining(" "))),
                            		InputForDialog.inputTfString("OLA PMDs (ps)", "A space separated list, wiht as many elements as OLAs, and the OLA added PMD in ps.", 10, ffOlas.stream().map(ee->df2.apply(ee.getPmdPs ())).collect(Collectors.joining(" "))),
                            		InputForDialog.inputTfString("OLA CD compensation (ps/nm)", "A space separated list, wiht as many elements as OLAs, and the OLA chromatic dispersion that is compensated within the OLA in ps/nm.", 10, ffOlas.stream().map(ee->df2.apply(ee.getCdCompensationPsPerNm ())).collect(Collectors.joining(" "))),
                            		InputForDialog.inputTfString("OLA minimum acceptable gain (dB)", "A space separated list, wiht as many elements as OLAs, and the OLA minimum acceptable gain in dB.", 10, ffOlas.stream().map(ee->df2.apply(ee.getMinAcceptableGainDb ())).collect(Collectors.joining(" "))),
                            		InputForDialog.inputTfString("OLA maximum acceptable gain (dB)", "A space separated list, wiht as many elements as OLAs, and the OLA maximum acceptable gain in dB.", 10, ffOlas.stream().map(ee->df2.apply(ee.getMaxAcceptableGainDb ())).collect(Collectors.joining(" "))),
                            		InputForDialog.inputTfString("OLA minimum acceptable output power (dBm)", "A space separated list, wiht as many elements as OLAs, and the OLA minimum acceptable output power in dBm.", 10, ffOlas.stream().map(ee->df2.apply(ee.getMinAcceptableOutputPower_dBm())).collect(Collectors.joining(" "))),
                            		InputForDialog.inputTfString("OLA maximum acceptable output power (dBm)", "A space separated list, wiht as many elements as OLAs, and the OLA maximum acceptable output power in dBm.", 10, ffOlas.stream().map(ee->df2.apply(ee.getMaxAcceptableOutputPower_dBm())).collect(Collectors.joining(" ")))
                            	),
                            (list)->
                            	{
                            		final List<Double> posKm = Stream.of(((String) list.get(0).get()).split(" ")).map(ee->Double.parseDouble(ee)).collect(Collectors.toList());
                            		final List<Double> gainDb = Stream.of(((String) list.get(1).get()).split(" ")).map(ee->Double.parseDouble(ee)).collect(Collectors.toList());
                            		final List<Double> nfDb = Stream.of(((String) list.get(2).get()).split(" ")).map(ee->Double.parseDouble(ee)).collect(Collectors.toList());
                            		final List<Double> pmdPs = Stream.of(((String) list.get(3).get()).split(" ")).map(ee->Double.parseDouble(ee)).collect(Collectors.toList());
                            		final List<Double> cd = Stream.of(((String) list.get(4).get()).split(" ")).map(ee->Double.parseDouble(ee)).collect(Collectors.toList());
                            		final List<Double> minGain = Stream.of(((String) list.get(5).get()).split(" ")).map(ee->Double.parseDouble(ee)).collect(Collectors.toList());
                            		final List<Double> maxGain = Stream.of(((String) list.get(6).get()).split(" ")).map(ee->Double.parseDouble(ee)).collect(Collectors.toList());
                            		final List<Double> minPower = Stream.of(((String) list.get(7).get()).split(" ")).map(ee->Double.parseDouble(ee)).collect(Collectors.toList());
                            		final List<Double> maxPower = Stream.of(((String) list.get(8).get()).split(" ")).map(ee->Double.parseDouble(ee)).collect(Collectors.toList());
                            		final List<OpticalAmplifierInfo> olasInfo = new ArrayList<> ();
                            		for (int cont = 0 ; cont < posKm.size() ; cont ++)
                            		{
                            			final OpticalAmplifierInfo oa = OpticalAmplifierInfo.getDefaultOla(posKm.get(cont));
                            			oa.setGainDb(gainDb.get(cont));
                            			oa.setNoiseFigureDb(nfDb.get(cont));
                            			oa.setPmdPs(pmdPs.get(cont));
                            			oa.setCdCompensationPsPerNm(cd.get(cont));
                            			oa.setMinAcceptableGainDb(minGain.get(cont));
                            			oa.setMaxAcceptableGainDb(maxGain.get(cont));
                            			oa.setMinAcceptableOutputPower_dBm(minPower.get(cont));
                            			oa.setMaxAcceptableOutputPower_dBm(maxPower.get(cont));
                            			olasInfo.add(oa);
                            		}
                            		getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setOlaTraversedInfo(olasInfo));
                            	}
                            ); 
                    } , (a,b)->b>0, null),

            		new AjtRcMenu("Add OLAs uniformly spaced to selected fibers", e-> 
                    DialogBuilder.launch(
                            "Add OLAs uniformly spaced to selected fibers" , 
                            "Please introduce the requested information.", 
                            "", 
                            this, 
                            Arrays.asList(InputForDialog.inputTfDouble("Maximum distance without amplification (km)", "The OLAs are placed equispacedly. The number of OLAs in a fiber is computed such that the maximum inter-OLA and OADM-to-OLA distance is equal or below this", 10, 80.0)),
                            (list)->
                            	{
                            		final Double maxDistanceKm = (Double) list.get(0).get();
                            		for (WFiber fiber : getSelectedElements().stream().map(ee->toWFiber.apply(ee)).collect(Collectors.toList()))
                            		{
                            			fiber.removeOpticalLineAmplifiers();
                            			final int numOlas = Math.floor(fiber.getLengthInKm() / maxDistanceKm) == fiber.getLengthInKm() / maxDistanceKm? (int) (fiber.getLengthInKm() / maxDistanceKm) - 1 : (int) Math.floor(fiber.getLengthInKm() / maxDistanceKm);
                            			final double interOlaDistanceKm = fiber.getLengthInKm() / (numOlas + 1);
                                		final List<OpticalAmplifierInfo> olasInfo = new ArrayList<> ();
                            			for (int cont = 0; cont < numOlas ; cont ++) olasInfo.add(OpticalAmplifierInfo.getDefaultOla((cont+1) * interOlaDistanceKm));
                            			fiber.setOlaTraversedInfo(olasInfo);
                            		}
                            	}
                            ) , (a,b)->b>0, null),

            		new AjtRcMenu("Set OLA gains to compensate previous span losses", e-> 
            		{
                		for (WFiber fiber : getSelectedElements().stream().map(ee->toWFiber.apply(ee)).collect(Collectors.toList()))
                		{
                    		final List<OpticalAmplifierInfo> olasInfo = fiber.getOpticalLineAmplifiersInfo();
                			for (int olaCont = 0; olaCont < olasInfo.size() ; olaCont ++)
                			{
                				final OpticalAmplifierInfo thisOla = olasInfo.get(olaCont);
                				final double startPreviousScanKm = olaCont == 0? 0.0 : olasInfo.get(olaCont-1).getOlaPositionInKm().get();
                				final double lengthPreviousSpanKm = thisOla.getOlaPositionInKm().get() - startPreviousScanKm;
                				final double attenuationPreviousSpan_dB = Math.abs(fiber.getAttenuationCoefficient_dbPerKm() * lengthPreviousSpanKm);
                				thisOla.setGainDb(attenuationPreviousSpan_dB);
                			}
                			fiber.setOlaTraversedInfo(olasInfo);
                		}
            		}
            		, (a,b)->b>0, null),
            		
            		new AjtRcMenu("Set OLA gains to compensate next span losses", e-> 
            		{
                		for (WFiber fiber : getSelectedElements().stream().map(ee->toWFiber.apply(ee)).collect(Collectors.toList()))
                		{
                    		final List<OpticalAmplifierInfo> olasInfo = fiber.getOpticalLineAmplifiersInfo();
                			for (int olaCont = 0; olaCont < olasInfo.size() ; olaCont ++)
                			{
                				final OpticalAmplifierInfo thisOla = olasInfo.get(olaCont);
                				final double endNextScanKm = olaCont == olasInfo.size()-1? fiber.getLengthInKm() : thisOla.getOlaPositionInKm().get();
                				final double lengthNextSpanKm = endNextScanKm - thisOla.getOlaPositionInKm().get();
                				final double attenuationNextSpan_dB = Math.abs(fiber.getAttenuationCoefficient_dbPerKm() * lengthNextSpanKm);
                				thisOla.setGainDb(attenuationNextSpan_dB);
                			}
                			fiber.setOlaTraversedInfo(olasInfo);
                		}
            		}
            		, (a,b)->b>0, null),

            		new AjtRcMenu("Set OLA gains as a constant value", e-> 
            		{
            			DialogBuilder.launch(
                                "Set OLA gains as constant value" , 
                                "Please introduce the requested information.", 
                                "", 
                                this, 
                                Arrays.asList(InputForDialog.inputTfDouble("Gain (dB)", "Gain (dB) of the OLAs", 10, WNetConstants.WFIBER_DEFAULT_OLAGAIN_DB.get(0))),
                                (list)->
                                	{
                                		final Double gainDb = (Double) list.get(0).get();
                                		for (WFiber fiber : getSelectedElements().stream().map(ee->toWFiber.apply(ee)).collect(Collectors.toList()))
                                		{
                                    		final List<OpticalAmplifierInfo> olasInfo = fiber.getOpticalLineAmplifiersInfo();
                                    		olasInfo.forEach(o->o.setGainDb(gainDb));
                                			fiber.setOlaTraversedInfo(olasInfo);
                                		}
                                	}
                                );
            		}
            		, (a,b)->b>0, null),

            		new AjtRcMenu("Set OLA noise factors as a constant value", e-> 
            		{
            			DialogBuilder.launch(
                                "Set OLA noise factors as constant value" , 
                                "Please introduce the requested information.", 
                                "", 
                                this, 
                                Arrays.asList(InputForDialog.inputTfDouble("Noise factor (dB)", "Gain (dB) of the OLAs", 10, WNetConstants.WFIBER_DEFAULT_OLANOISEFACTOR_DB.get(0))),
                                (list)->
                                	{
                                		final Double noiseFactorDb = (Double) list.get(0).get();
                                		for (WFiber fiber : getSelectedElements().stream().map(ee->toWFiber.apply(ee)).collect(Collectors.toList()))
                                		{
                                    		final List<OpticalAmplifierInfo> olasInfo = fiber.getOpticalLineAmplifiersInfo();
                                    		olasInfo.forEach(o->o.setNoiseFigureDb(noiseFactorDb));
                                			fiber.setOlaTraversedInfo(olasInfo);
                                		}
                                	}
                                );
            		}
            		, (a,b)->b>0, null),

            		new AjtRcMenu("Set OLA PMD (ps) as a constant value", e-> 
            		{
            			DialogBuilder.launch(
                                "Set OLA PMS as constant value" , 
                                "Please introduce the requested information.", 
                                "", 
                                this, 
                                Arrays.asList(InputForDialog.inputTfDouble("PMD (ps)", "PMD (ps) of the OLAs", 10, WNetConstants.WFIBER_DEFAULT_OLAPMD_PS.get(0))),
                                (list)->
                                	{
                                		final Double pmdPs = (Double) list.get(0).get();
                                		for (WFiber fiber : getSelectedElements().stream().map(ee->toWFiber.apply(ee)).collect(Collectors.toList()))
                                		{
                                    		final List<OpticalAmplifierInfo> olasInfo = fiber.getOpticalLineAmplifiersInfo();
                                    		olasInfo.forEach(o->o.setPmdPs(pmdPs));
                                			fiber.setOlaTraversedInfo(olasInfo);
                                		}
                                	}
                                );
            		}
            		, (a,b)->b>0, null),

            		new AjtRcMenu("Set OLA CD compensation (ps/nm) as a constant value", e-> 
            		{
            			DialogBuilder.launch(
                                "Set OLA CD compensation (ps/nm) as constant value" , 
                                "Please introduce the requested information.", 
                                "", 
                                this, 
                                Arrays.asList(InputForDialog.inputTfDouble("CD compensation (ps/nm)", "CD compensation (ps/nm) of the OLAs", 10, WNetConstants.WFIBER_DEFAULT_OLACDCOMPENSATION.get(0))),
                                (list)->
                                	{
                                		final Double cdPsPerNm = (Double) list.get(0).get();
                                		for (WFiber fiber : getSelectedElements().stream().map(ee->toWFiber.apply(ee)).collect(Collectors.toList()))
                                		{
                                    		final List<OpticalAmplifierInfo> olasInfo = fiber.getOpticalLineAmplifiersInfo();
                                    		olasInfo.forEach(o->o.setCdCompensationPsPerNm(cdPsPerNm));
                                			fiber.setOlaTraversedInfo(olasInfo);
                                		}
                                	}
                                );
            		}
            		, (a,b)->b>0, null),

            		new AjtRcMenu("Set OLA maximum and minimum gain as constant values", e-> 
            		{
            			DialogBuilder.launch(
                                "Set OLA maximum and minimum gain as constant values" , 
                                "Please introduce the requested information.", 
                                "", 
                                this, 
                                Arrays.asList(InputForDialog.inputTfDouble("Minimum gain (dB)", "Minimum gain (dB) of the OLA", 10, 19.0),
                                		InputForDialog.inputTfDouble("Maximum gain (dB)", "Maximum gain (dB) of the OLA", 10, 23.0)),
                                (list)->
                                	{
                                		final Double minGainDb = (Double) list.get(0).get();
                                		final Double maxGainDb = (Double) list.get(1).get();
                                		if (minGainDb > maxGainDb) throw new Net2PlanException ("Invalid gain values");
                                		for (WFiber fiber : getSelectedElements().stream().map(ee->toWFiber.apply(ee)).collect(Collectors.toList()))
                                		{
                                    		final List<OpticalAmplifierInfo> olasInfo = fiber.getOpticalLineAmplifiersInfo();
                                    		olasInfo.forEach(o->o.setMinAcceptableGainDb(minGainDb));
                                    		olasInfo.forEach(o->o.setMaxAcceptableGainDb(maxGainDb));
                                			fiber.setOlaTraversedInfo(olasInfo);
                                		}
                                	}
                                );
            		}
            		, (a,b)->b>0, null),
            		
            		new AjtRcMenu("Set OLA maximum and minimum output power as constant values", e-> 
            		{
            			DialogBuilder.launch(
                                "Set OLA maximum and minimum output power as constant values" , 
                                "Please introduce the requested information.", 
                                "", 
                                this, 
                                Arrays.asList(InputForDialog.inputTfDouble("Minimum output power (dBm)", "Minimum output power (dBm) of the OLA", 10, -29.0),
                                		InputForDialog.inputTfDouble("Maximum output power (dmB)", "Maximum output power (dmB) of the OLA", 10, 2.0)),
                                (list)->
                                	{
                                		final Double minOutputPowerDbm = (Double) list.get(0).get();
                                		final Double maxOutputPowerDbm = (Double) list.get(1).get();
                                		if (minOutputPowerDbm > maxOutputPowerDbm) throw new Net2PlanException ("Invalid gain values");
                                		for (WFiber fiber : getSelectedElements().stream().map(ee->toWFiber.apply(ee)).collect(Collectors.toList()))
                                		{
                                    		final List<OpticalAmplifierInfo> olasInfo = fiber.getOpticalLineAmplifiersInfo();
                                    		olasInfo.forEach(o->o.setMinAcceptableOutputPower_dBm(minOutputPowerDbm));
                                    		olasInfo.forEach(o->o.setMaxAcceptableOutputPower_dBm(maxOutputPowerDbm));
                                			fiber.setOlaTraversedInfo(olasInfo);
                                		}
                                	}
                                );
            		}
            		, (a,b)->b>0, null)

            		)));


            
    	}

        return res;
    }
    
}
