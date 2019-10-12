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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_abstractElement.AGTYPE;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.DialogBuilder;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.InputForDialog;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.monitoring.MonitoringUtils;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.niw.OpticalSimulationModule;
import com.net2plan.niw.OpticalSimulationModule.PERLPINFOMETRICS;
import com.net2plan.niw.OpticalSpectrumManager;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WIpLink;
import com.net2plan.niw.WIpSourceRoutedConnection;
import com.net2plan.niw.WLightpath;
import com.net2plan.niw.WLightpathRequest;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNetConstants;
import com.net2plan.niw.WNetConstants.WTYPE;
import com.net2plan.niw.WNode;
import com.net2plan.niw.WServiceChain;
import com.net2plan.utils.Pair;

/**
 */
@SuppressWarnings({ "unchecked", "serial" })
public class Niw_AdvancedJTable_route extends AdvancedJTable_networkElement<Route>
{
	private static DecimalFormat df = new DecimalFormat("#.##");
	
    public Niw_AdvancedJTable_route(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.ROUTES , layerThisTable.getName().equals(WNetConstants.ipLayerName)? "IP connections" : "Lightpaths" , layerThisTable , true , null);
    }

    @Override
  public List<AjtColumnInfo<Route>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
    	final List<AjtColumnInfo<Route>> res = new LinkedList<> ();
    	final WNet wNet = new WNet (callback.getDesign()); 
    	final boolean isIpLayer = getTableNetworkLayer().equals(wNet.getIpLayer().getNe());
    	final boolean isWdmLayer = getTableNetworkLayer().equals(wNet.getWdmLayer().getNe());
    	assert isIpLayer || isWdmLayer;
    	assert !(isIpLayer && isWdmLayer);
    	final Function<Route,WServiceChain> toSc = d -> (WServiceChain) wNet.getWElement(d).get();
    	final Function<Route,WIpSourceRoutedConnection> toIpc = d -> (WIpSourceRoutedConnection) wNet.getWElement(d).get();
    	final Function<Route,WLightpath> toLp = d ->(WLightpath) wNet.getWElement(d).get();
    	final Function<Route,Boolean> isSc = d -> wNet.getWElement(d).get().isServiceChain();
    	final Function<Route,Boolean> isIpc = d -> wNet.getWElement(d).get().isIpConnection();
    	final Function<Route,Boolean> isLp = d -> wNet.getWElement(d).get().isLightpath();
    	
        res.add(new AjtColumnInfo<Route>(this , Node.class, null , "A", "Origin node", null , d->isLp.apply(d)? toLp.apply(d).getA().getNe() : (isIpc.apply(d)? toIpc.apply(d).getA().getNe() : toSc.apply(d).getA().getNe()) , AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<Route>(this , Node.class, null , "B", "Destination node", null , d->isLp.apply(d)? toLp.apply(d).getB().getNe() : (isIpc.apply(d)? toIpc.apply(d).getB().getNe() : toSc.apply(d).getB().getNe()) , AGTYPE.NOAGGREGATION , null));

    	if (isIpLayer)
    	{
            res.add(new AjtColumnInfo<Route>(this , String.class, null , "Type", "Indicates if this demand is a IP connection or a service chain realizing a service chain request, potentially traversing VNFs", null , d->isIpc.apply(d)? "IP connection" : "Service chain" , AGTYPE.NOAGGREGATION , null));
    		res.add(new AjtColumnInfo<Route>(this , Demand.class, null , "Demand / Service chain req.", "Associated IP unicast demand or service chain request of this element", null , d->isIpc.apply(d)? toIpc.apply(d).getIpUnicastDemand().getNe() : toSc.apply(d).getServiceChainRequest().getNe() , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Boolean.class, null , "Up?", "Indicates if this IP connection or service chain is up (not traversing any down node or IP link)", null , d->isIpc.apply(d)? !toIpc.apply(d).isDown() : !toSc.apply(d).isDown() , AGTYPE.COUNTTRUE , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, null , "Target carried IP traffic (Gbps)", "Nominal IP traffic in Gbps to be carried by this IP connection or service chain (in this case the one injected by the origin node), in the non-failure state", (d,val)-> { if (isIpc.apply(d)) toIpc.apply(d).setCarriedTrafficInNoFailureStateGbps((Double) val); else toSc.apply(d).setInitiallyInjectedTrafficGbps((Double) val); } , d->isIpc.apply(d)? toIpc.apply(d).getCarriedTrafficInNoFailureStateGbps() : toSc.apply(d).getCarriedTrafficInNoFaillureStateGbps() , AGTYPE.SUMDOUBLE , null) );
            res.add(new AjtColumnInfo<Route>(this , Double.class, null , "Current IP traffic (Gbps)", "Current carried IP traffic by this IP connection or this service chain. In the latter case it is the traffic injected by the origin node", null , d->isIpc.apply(d)? toIpc.apply(d).getCurrentCarriedTrafficGbps() : toSc.apply(d).getCurrentCarriedTrafficGbps() , AGTYPE.SUMDOUBLE , null));
            res.add(new AjtColumnInfo<Route>(this , Collection.class, null , "# IP links", "Number of traversed IP links", null , d->isIpc.apply(d)? toIpc.apply(d).getSequenceOfTraversedIpLinks().stream().map(e->e.getNe()).collect(Collectors.toList()) : toSc.apply(d).getSequenceOfTraversedIpLinks().stream().map(e->e.getNe()).collect(Collectors.toList()), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Collection.class, null , "# IP nodes", "Number of traversed IP nodes", null , d->isIpc.apply(d)? toIpc.apply(d).getSequenceOfTraversedIpNodes().stream().map(e->e.getNe()).collect(Collectors.toList()) : toSc.apply(d).getSequenceOfTraversedIpNodesWithoutConsecutiveRepetitions().stream().map(e->e.getNe()).collect(Collectors.toList()), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Collection.class, null , "# VNFs", "Number of traversed VNF instances", null , d->isIpc.apply(d)? "--" : toSc.apply(d).getSequenceOfTraversedVnfInstances().stream().map(e->e.getNe()).collect(Collectors.toList()), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Collection.class, null , "SC: VNF expansion factors", "For each VNF, the expansion factor applied to the traffic. That is, the ratio between the traffic at the output and the input of the VNF. E.g. 0.5 if the VNF compresses the traffic a 50%", null , d->isIpc.apply(d)? "--" : toSc.apply(d).getCurrentExpansionFactorApplied().stream().map(e->"" + df.format(e)).collect(Collectors.joining(" ")), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, null , "Length (km)", "Length of the IP connection, considering also the length of the transport layer WDM links traversed if any", null , d->isIpc.apply(d)? toIpc.apply(d).getWorstCaseLengthInKm() : toSc.apply(d).getWorstCaseLengthInKm() , AGTYPE.MAXDOUBLE , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, null , "E2e latency (ms)", "End-to-end latency considering links traversed, and propagation time in transport layer links if any, and processing time in the VNFs for the service chains", null , d->isIpc.apply(d)? toIpc.apply(d).getWorstCasePropgationLatencyInMs() : toSc.apply(d).getWorstCaseLatencyInMs() , AGTYPE.MAXDOUBLE , d->{ if (isSc.apply(d)) return null; final double m = toIpc.apply(d).getIpUnicastDemand().getMaximumAcceptableE2EWorstCaseLatencyInMs(); if (m >= 0) return null; return toIpc.apply(d).getWorstCasePropgationLatencyInMs() > m? Color.RED : null; }));
    	}
    	else
    	{
    		final OpticalSpectrumManager ospec = OpticalSpectrumManager.createFromRegularLps(wNet);
    		final OpticalSimulationModule osim = new OpticalSimulationModule (wNet);
    		osim.updateAllPerformanceInfo();
    		
            res.add(new AjtColumnInfo<Route>(this , Demand.class, null , "Lp request", "Associated lightpath request", null , d->toLp.apply(d).getLightpathRequest().getNe() , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Boolean.class, null , "Up?", "Indicates if this lightpath is up (not traversing any down node or fiber)", null , d->!toLp.apply(d).isDown() , AGTYPE.COUNTTRUE , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, null , "Target line rate (Gbps)", "Nominal line rate for this lightpath (its rate if up)", null , d->toLp.apply(d).getLightpathRequest().getLineRateGbps() , AGTYPE.SUMDOUBLE , null));
            res.add(new AjtColumnInfo<Route>(this , Collection.class, null , "# fibers", "Number of traversed fibers links", null , d->toLp.apply(d).getSeqFibers().stream().map(e->e.getNe()).collect(Collectors.toList()) , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Collection.class, null , "# OADMs", "Number of traversed OADM nodes", null , d->toLp.apply(d).getSeqNodes().stream().map(e->e.getNe()).collect(Collectors.toList()) , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, null , "Length (km)", "Length of the lightpath connection, considering the length of the fiber links traversed", null , d->toLp.apply(d).getLengthInKm() , AGTYPE.MAXDOUBLE , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, null , "E2e latency (ms)", "End-to-end latency of the lightpath in ms, considering the fibers traversed", null , d->toLp.apply(d).getPropagationDelayInMs() , AGTYPE.MAXDOUBLE , null));
            res.add(new AjtColumnInfo<Route>(this , Boolean.class, null , "Is backup?", "Indicates if this lightpath is a backup lightpath", null , d->toLp.apply(d).isBackupLightpath() , AGTYPE.COUNTTRUE , null));
            res.add(new AjtColumnInfo<Route>(this , Collection.class, null , "Main lps", "If this is a backup lightpath, indicates the main lightpath associated to it", null , d->toLp.apply(d).isBackupLightpath()? toLp.apply(d).getPrimaryLightpathsOfThisBackupLightpath().stream().map(ee->ee.getNe()).collect(Collectors.toList()) : "--", AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Collection.class, null , "Backup lps", "If this is a primary lightpath, indicates the backup lightpaths associated to it", null , d->toLp.apply(d).isBackupLightpath()? "--" : toLp.apply(d).getBackupLightpaths().stream().map(ee->ee.getNe()).collect(Collectors.toList()) , AGTYPE.NOAGGREGATION , null));

            
            res.add(new AjtColumnInfo<Route>(this , String.class, Arrays.asList("Spectrum occupation") , "# Occupied slots", "Number of occupied optical slots", null , d->toLp.apply(d).getOpticalSlotIds().size() , AGTYPE.SUMINT , null));
            res.add(new AjtColumnInfo<Route>(this , String.class, Arrays.asList("Spectrum occupation") , "Occupied slots", "Slots occupied", null , d->toLp.apply(d).getOpticalSlotIds().stream().map(ee->""+ee).collect(Collectors.joining(",")) , AGTYPE.SUMINT , null));
            res.add(new AjtColumnInfo<Route>(this , String.class, Arrays.asList("Spectrum occupation") , "Clashing?", "Slots occupied", null , d->ospec.isSpectrumOccupationOk(toLp.apply(d))? "No" : "Yes" , AGTYPE.COUNTTRUE , t->ospec.isSpectrumOccupationOk(toLp.apply(t))? null : Color.RED));
            res.add(new AjtColumnInfo<Route>(this , String.class, Arrays.asList("Transponder info") , "Transponder name", "Name of the transponder associated to the lightpath request of this lightpath", null , d->toLp.apply(d).getLightpathRequest().getTransponderName() , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , String.class, Arrays.asList("Transponder info") , "Modulation id", "Identifier of the modulation used in this lightpath", (d,val)->toLp.apply(d).setModulationId((String)val) , d->toLp.apply(d).getModulationId() , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Transponder info") , "Min. rx power (dBm)", "The minimum reception power acceptable in the reception side of this lightpath", (d,val)->toLp.apply(d).setTransponderMinimumTolerableReceptionPower_dBm((Double)val) , d->toLp.apply(d).getTransponderMinimumTolerableReceptionPower_dBm(), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Transponder info") , "Max. rx power (dBm)", "The maximum reception power acceptable in the reception side of this lightpath", (d,val)->toLp.apply(d).setTransponderMaximumTolerableReceptionPower_dBm((Double)val) , d->toLp.apply(d).getTransponderMaximumTolerableReceptionPower_dBm(), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Transponder info") , "Min. OSNR (dB)", "The minimum OSNR, measured at 12.5 GHz of reference bandwidth, acceptable in the reception side of this lightpath", (d,val)->toLp.apply(d).setTransponderMinimumTolerableOsnrAt12_5GHzOfRefBw_dB((Double)val) , d->toLp.apply(d).getTransponderMinimumTolerableOsnrAt12_5GHzOfRefBw_dB(), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Transponder info") , "Max. CD (ps/nm)", "The maximum chromatic dispersion in absolutte value, measured in ps/nm, acceptable in the reception side of this lightpath", (d,val)->toLp.apply(d).setTransponderMaximumTolerableCdInAbsoluteValue_perPerNm((Double)val) , d->toLp.apply(d).getTransponderMaximumTolerableCdInAbsoluteValue_perPerNm(), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Transponder info") , "Max. PMS (ps)", "The maximum polarization mode dispersion (PMD), measured in ps, acceptable in the reception side of this lightpath", (d,val)->toLp.apply(d).setTransponderMaximumTolerablePmd_ps((Double)val) , d->toLp.apply(d).getTransponderMaximumTolerablePmd_ps(), AGTYPE.NOAGGREGATION , null));

            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Injection power (dBm)", "The injection power of the transponder in the ADD part (transmission side)", (d,val)->toLp.apply(d).setAddTransponderInjectionPower_dBm((Double)val) , d->toLp.apply(d).getAddTransponderInjectionPower_dBm(), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Tx end: power (dBm)", "The optical power at the transmission end of the lightpath", null , d->osim.getOpticalPerformanceAtTransponderTransmitterEnd(toLp.apply(d)).get(PERLPINFOMETRICS.POWER_DBM), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Tx end: OSNR (dB)", "The OSNR (at 12.5 GHz reference bandwidth) of the lightpath at the transmission end", null , d->osim.getOpticalPerformanceAtTransponderTransmitterEnd(toLp.apply(d)).get(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Tx end: CD (ps/nm)", "The chromatic dispersion in ps/nm of the lightpath at the transmission end", null , d->osim.getOpticalPerformanceAtTransponderTransmitterEnd(toLp.apply(d)).get(PERLPINFOMETRICS.CD_PERPERNM), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Tx end: PMS (ps)", "The polarization mode dispersion (PMD) in ps, of the lightpath at the transmission end", null , d->Math.sqrt(osim.getOpticalPerformanceAtTransponderTransmitterEnd(toLp.apply(d)).get(PERLPINFOMETRICS.PMDSQUARED_PS2)), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Rx end: power (dBm)", "The optical power at the reception end of the lightpath", null , d->osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(d)).get(PERLPINFOMETRICS.POWER_DBM), AGTYPE.NOAGGREGATION , t-> { final double v = osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(t)).get(PERLPINFOMETRICS.POWER_DBM); if (v > toLp.apply(t).getTransponderMaximumTolerableReceptionPower_dBm()) return Color.red; if (v < toLp.apply(t).getTransponderMinimumTolerableReceptionPower_dBm()) return Color.red; return null; } ));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Rx end: OSNR (dB)", "The OSNR (at 12.5 GHz reference bandwidth) of the lightpath at the reception end", null , d->osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(d)).get(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW), AGTYPE.NOAGGREGATION , d->osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(d)).get(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW) < toLp.apply(d).getTransponderMinimumTolerableOsnrAt12_5GHzOfRefBw_dB()? Color.red : null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Rx end: CD (ps/nm)", "The chromatic dispersion in ps/nm of the lightpath at the reception end", null , d->osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(d)).get(PERLPINFOMETRICS.CD_PERPERNM), AGTYPE.NOAGGREGATION , d->Math.abs(osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(d)).get(PERLPINFOMETRICS.CD_PERPERNM)) > toLp.apply(d).getTransponderMaximumTolerableCdInAbsoluteValue_perPerNm()? Color.red : null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Rx end: PMS (ps)", "The polarization mode dispersion (PMD) in ps, of the lightpath at the reception end", null , d->Math.sqrt(osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(d)).get(PERLPINFOMETRICS.PMDSQUARED_PS2)), AGTYPE.NOAGGREGATION , d->Math.sqrt(osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(d)).get(PERLPINFOMETRICS.PMDSQUARED_PS2)) > toLp.apply(d).getTransponderMaximumTolerablePmd_ps()? Color.red : null));
    	}
      return res;
  	}

    @Override
    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
    	final NetPlan np = callback.getDesign();
        final List<AjtRcMenu> res = new ArrayList<> ();
    	final WNet wNet = new WNet (callback.getDesign()); 
    	final boolean isIpLayer = getTableNetworkLayer().equals(wNet.getIpLayer().getNe());
    	final boolean isWdmLayer = getTableNetworkLayer().equals(wNet.getWdmLayer().getNe());
    	assert isIpLayer || isWdmLayer;
    	assert !(isIpLayer && isWdmLayer);
    	final Function<Route,WServiceChain> toSc = d -> (WServiceChain) wNet.getWElement(d).get();
    	final Function<Route,WIpSourceRoutedConnection> toIpc = d -> (WIpSourceRoutedConnection) wNet.getWElement(d).get();
    	final Function<Route,WLightpath> toLp = d ->(WLightpath) wNet.getWElement(d).get();
    	final Function<Route,Boolean> isSc = d -> wNet.getWElement(d).get().isServiceChain();
    	final Function<Route,Boolean> isIpc = d -> wNet.getWElement(d).get().isIpConnection();
    	final Function<Route,Boolean> isLp = d -> wNet.getWElement(d).get().isLightpath();
		final Function<String,Optional<WNode>> nodeByName = st -> 
		{
    		WNode a = wNet.getNodeByName(st).orElse(null);
    		if (a == null) a = wNet.getNodes().stream().filter(n->n.getName().equalsIgnoreCase(st)).findFirst().orElse(null);
    		return Optional.ofNullable(a);
		};

    	if (isIpLayer)
    	{

    		
    		
    		res.add(new AjtRcMenu("Add IP connection", null , (a,b)->true, Arrays.asList(
    				new AjtRcMenu("as shortest path in lantecy") , 
    				e->
    	            {
    	              DialogBuilder.launch(
    	              "Add IP source routed connection" , 
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
    	            } ,     (a,b)->true, null)				
    				)
    				));
            
            res.add(new AjtRcMenu("Bundle selected IP links when possible", e->
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
            , (a,b)->true, null));

            res.add(new AjtRcMenu("Unbundle selected LAGs", e->
            {
            	for (Link d : getSelectedElements())
            	{
            		final WIpLink ee = toWIpLink.apply(d);
            		if (!ee.isBundleOfIpLinks()) continue;
            		ee.unbundleBidirectional();
            	}
            }
            , (a,b)->true, null));
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
            res.add(new AjtRcMenu("Set selected links nominal capacity", null , (a,b)->b>0, Arrays.asList(
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
                                		getSelectedElements().stream().map(ee->toWIpLink.apply(ee)).filter(ee->!ee.isCoupledtoLpRequest()).forEach (ee->ee.setNominalCapacityGbps (newLinkCapacity));
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
            		
            		new AjtRcMenu("As the harversine node pair distance", e->
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
                                		getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setLenghtInKm(scalingFactor * ee.getLengthIfNotCoupledInKm()));
                                	}
                                );
                    } , (a,b)->b>0, null) ,         		
            		new AjtRcMenu("As the euclidean node pair distance", e->
                    {
                		getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setLenghtInKm(np.getNodePairEuclideanDistance(ee.getNe().getOriginNode(), ee.getNe().getDestinationNode())));
                    } , (a,b)->b>0, null) ,
            		
            		new AjtRcMenu("As the harversine node pair distance", e->
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

            res.add(new AjtRcMenu("Set optical line amplifiers (OLA) info to selected fibers", e->
            {
            	final WFiber firstFiber = getSelectedElements().stream().map(ee->toWFiber.apply(ee)).findFirst().orElse(null);
            	if (firstFiber == null) return;
            	DialogBuilder.launch(
                    "Set optical line amplifiers (OLA) info" , 
                    "Please introduce the requested information. All the lists are space-separated and with the same number of elements, one per optical line amplifier" , 
                    "", 
                    this, 
                    Arrays.asList(
                    		InputForDialog.inputTfString("OLA positions (km from fiber init)", "A space separated list, wiht as many elements as OLAs, and the OLA position in km from the fiber start point.", 10, firstFiber.getAmplifierPositionsKmFromOrigin_km().stream().map(ee->df.format(ee)).collect(Collectors.joining(" "))),
                    		InputForDialog.inputTfString("OLA gains (dB)", "A space separated list, wiht as many elements as OLAs, and the OLA gains in dB.", 10, firstFiber.getAmplifierGains_dB().stream().map(ee->df.format(ee)).collect(Collectors.joining(" "))),
                    		InputForDialog.inputTfString("OLA noise factors (dB)", "A space separated list, wiht as many elements as OLAs, and the OLA noise factors in dB.", 10, firstFiber.getAmplifierNoiseFactor_dB().stream().map(ee->df.format(ee)).collect(Collectors.joining(" "))),
                    		InputForDialog.inputTfString("OLA PMDs (ps)", "A space separated list, wiht as many elements as OLAs, and the OLA added PMD in ps.", 10, firstFiber.getAmplifierPmd_ps().stream().map(ee->df.format(ee)).collect(Collectors.joining(" "))),
                    		InputForDialog.inputTfString("OLA CD compensation (ps/nm)", "A space separated list, wiht as many elements as OLAs, and the OLA chromatic dispersion that is compensated within the OLA in ps/nm.", 10, firstFiber.getAmplifierCdCompensation_psPerNm().stream().map(ee->df.format(ee)).collect(Collectors.joining(" "))),
                    		InputForDialog.inputTfString("OLA minimum acceptable gain (dB)", "A space separated list, wiht as many elements as OLAs, and the OLA minimum acceptable gain in dB.", 10, firstFiber.getAmplifierMinAcceptableGains_dB().stream().map(ee->df.format(ee)).collect(Collectors.joining(" "))),
                    		InputForDialog.inputTfString("OLA maximum acceptable gain (dB)", "A space separated list, wiht as many elements as OLAs, and the OLA maximum acceptable gain in dB.", 10, firstFiber.getAmplifierMaxAcceptableGains_dB().stream().map(ee->df.format(ee)).collect(Collectors.joining(" "))),
                    		InputForDialog.inputTfString("OLA minimum acceptable input power (dBm)", "A space separated list, wiht as many elements as OLAs, and the OLA minimum acceptable input power in dBm.", 10, firstFiber.getAmplifierMinAcceptableInputPower_dBm().stream().map(ee->df.format(ee)).collect(Collectors.joining(" "))),
                    		InputForDialog.inputTfString("OLA maximum acceptable input power (dBm)", "A space separated list, wiht as many elements as OLAs, and the OLA maximum acceptable input power in dBm.", 10, firstFiber.getAmplifierMaxAcceptableInputPower_dBm().stream().map(ee->df.format(ee)).collect(Collectors.joining(" ")))
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
                    		getSelectedElements().stream().map(ee->toWFiber.apply(ee)).forEach(ee->ee.setAmplifiersTraversedInfo(posKm, gainDb , nfDb , pmdPs , cd , minGain , maxGain , minPower , maxPower));
                    	}
                    ); 
            } , (a,b)->b>0, null));
    	}

        return res;
    }
    
}
