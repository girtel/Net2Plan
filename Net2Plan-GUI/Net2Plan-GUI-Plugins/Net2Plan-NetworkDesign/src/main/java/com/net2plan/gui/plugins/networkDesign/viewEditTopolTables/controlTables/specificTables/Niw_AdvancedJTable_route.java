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
    	assert callback.getNiwInfo().getFirst();
    	final WNet wNet = callback.getNiwInfo().getSecond(); 
    	final boolean isIpLayer = getTableNetworkLayer().getName ().equals(WNetConstants.ipLayerName);
    	final boolean isWdmLayer = getTableNetworkLayer().getName ().equals(WNetConstants.wdmLayerName);
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
    		final OpticalSpectrumManager ospec = callback.getNiwInfo().getThird();
    		final OpticalSimulationModule osim = callback.getNiwInfo().getFourth();
    		
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

            
            res.add(new AjtColumnInfo<Route>(this , String.class, null , "# Occupied slots", "Number of occupied optical slots", null , d->toLp.apply(d).getOpticalSlotIds().size() , AGTYPE.SUMINT , null));
            res.add(new AjtColumnInfo<Route>(this , String.class, null , "Occupied slots", "Slots occupied", null , d->toLp.apply(d).getOpticalSlotIds().stream().map(ee->""+ee).collect(Collectors.joining(",")) , AGTYPE.SUMINT , null));
            res.add(new AjtColumnInfo<Route>(this , String.class, null , "Clashing?", "Slots occupied", null , d->ospec.isSpectrumOccupationOk(toLp.apply(d))? "No" : "Yes" , AGTYPE.COUNTTRUE , t->ospec.isSpectrumOccupationOk(toLp.apply(t))? null : Color.RED));
            res.add(new AjtColumnInfo<Route>(this , String.class, null , "Transponder name", "Name of the transponder associated to the lightpath request of this lightpath", null , d->toLp.apply(d).getLightpathRequest().getTransponderName() , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , String.class, Arrays.asList("Optical signal") , "Modulation id", "Identifier of the modulation used in this lightpath", (d,val)->toLp.apply(d).setModulationId((String)val) , d->toLp.apply(d).getModulationId() , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Tp: Min. rx power (dBm)", "The minimum reception power acceptable in the reception side of this lightpath", (d,val)->toLp.apply(d).setTransponderMinimumTolerableReceptionPower_dBm((Double)val) , d->toLp.apply(d).getTransponderMinimumTolerableReceptionPower_dBm(), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Tp: Max. rx power (dBm)", "The maximum reception power acceptable in the reception side of this lightpath", (d,val)->toLp.apply(d).setTransponderMaximumTolerableReceptionPower_dBm((Double)val) , d->toLp.apply(d).getTransponderMaximumTolerableReceptionPower_dBm(), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Tp: Min. OSNR (dB)", "The minimum OSNR, measured at 12.5 GHz of reference bandwidth, acceptable in the reception side of this lightpath", (d,val)->toLp.apply(d).setTransponderMinimumTolerableOsnrAt12_5GHzOfRefBw_dB((Double)val) , d->toLp.apply(d).getTransponderMinimumTolerableOsnrAt12_5GHzOfRefBw_dB(), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Tp: Max. CD (ps/nm)", "The maximum chromatic dispersion in absolutte value, measured in ps/nm, acceptable in the reception side of this lightpath", (d,val)->toLp.apply(d).setTransponderMaximumTolerableCdInAbsoluteValue_perPerNm((Double)val) , d->toLp.apply(d).getTransponderMaximumTolerableCdInAbsoluteValue_perPerNm(), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Tp: Max. PMS (ps)", "The maximum polarization mode dispersion (PMD), measured in ps, acceptable in the reception side of this lightpath", (d,val)->toLp.apply(d).setTransponderMaximumTolerablePmd_ps((Double)val) , d->toLp.apply(d).getTransponderMaximumTolerablePmd_ps(), AGTYPE.NOAGGREGATION , null));

            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Tp out: injection power (dBm)", "The injection power of the lightpath, measured at the output of the transponder", (d,val)->toLp.apply(d).setAddTransponderInjectionPower_dBm((Double)val) , d->toLp.apply(d).getAddTransponderInjectionPower_dBm(), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "1st OADM out: power (dBm)", "The optical power at the transmission end of the lightpath", null , d->osim.getOpticalPerformanceAtTransponderTransmitterEnd(toLp.apply(d)).get(PERLPINFOMETRICS.POWER_DBM), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "1st OADM out: OSNR (dB)", "The OSNR (at 12.5 GHz reference bandwidth) of the lightpath at the transmission end", null , d->osim.getOpticalPerformanceAtTransponderTransmitterEnd(toLp.apply(d)).get(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "1st OADM out: CD (ps/nm)", "The chromatic dispersion in ps/nm of the lightpath at the transmission end", null , d->osim.getOpticalPerformanceAtTransponderTransmitterEnd(toLp.apply(d)).get(PERLPINFOMETRICS.CD_PERPERNM), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "1st OADM out: PMS (ps)", "The polarization mode dispersion (PMD) in ps, of the lightpath at the transmission end", null , d->Math.sqrt(osim.getOpticalPerformanceAtTransponderTransmitterEnd(toLp.apply(d)).get(PERLPINFOMETRICS.PMDSQUARED_PS2)), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Tp rx: power (dBm)", "The optical power at the reception end of the lightpath", null , d->osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(d)).get(PERLPINFOMETRICS.POWER_DBM), AGTYPE.NOAGGREGATION , t-> { final double v = osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(t)).get(PERLPINFOMETRICS.POWER_DBM); if (v > toLp.apply(t).getTransponderMaximumTolerableReceptionPower_dBm()) return Color.red; if (v < toLp.apply(t).getTransponderMinimumTolerableReceptionPower_dBm()) return Color.red; return null; } ));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Tp rx: OSNR (dB)", "The OSNR (at 12.5 GHz reference bandwidth) of the lightpath at the reception end", null , d->osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(d)).get(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW), AGTYPE.NOAGGREGATION , d->osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(d)).get(PERLPINFOMETRICS.OSNRAT12_5GHZREFBW) < toLp.apply(d).getTransponderMinimumTolerableOsnrAt12_5GHzOfRefBw_dB()? Color.red : null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Tp rx: CD (ps/nm)", "The chromatic dispersion in ps/nm of the lightpath at the reception end", null , d->osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(d)).get(PERLPINFOMETRICS.CD_PERPERNM), AGTYPE.NOAGGREGATION , d->Math.abs(osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(d)).get(PERLPINFOMETRICS.CD_PERPERNM)) > toLp.apply(d).getTransponderMaximumTolerableCdInAbsoluteValue_perPerNm()? Color.red : null));
            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Tp rx: PMS (ps)", "The polarization mode dispersion (PMD) in ps, of the lightpath at the reception end", null , d->Math.sqrt(osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(d)).get(PERLPINFOMETRICS.PMDSQUARED_PS2)), AGTYPE.NOAGGREGATION , d->Math.sqrt(osim.getOpticalPerformanceAtTransponderReceiverEnd(toLp.apply(d)).get(PERLPINFOMETRICS.PMDSQUARED_PS2)) > toLp.apply(d).getTransponderMaximumTolerablePmd_ps()? Color.red : null));
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
            res.add(new AjtRcMenu("Remove selected elements", e->getSelectedElements().forEach(dd-> { if (isIpc.apply(dd)) toIpc.apply(dd).remove (); else toSc.apply(dd).remove();  }) , (a,b)->b>0, null) );
            res.add(new AjtRcMenu("Set selected elements injected traffic", null , (a,b)->b>0, Arrays.asList(
            		new AjtRcMenu("As constant value", e->
                    {
                        DialogBuilder.launch(
                                "Set selected elements injected traffic" , 
                                "Please introduce the IP traffic in Gbps. Negative values are not allowed.", 
                                "", 
                                this, 
                                Arrays.asList(InputForDialog.inputTfDouble("IP injected traffic (Gbps)", "Introduce the injected traffic in Gbps for each IP connection / service chain", 10, 0.0)),
                                (list)->
                                	{
                                		final double val = (Double) list.get(0).get();
                                		getSelectedElements().stream().forEach(ee->{ if (isIpc.apply(ee)) toIpc.apply(ee).setCarriedTrafficInNoFailureStateGbps(val); else toSc.apply(ee).setInitiallyInjectedTrafficGbps (val); });
                                	}
                                );
                    } , (a,b)->b>0, null),
            		new AjtRcMenu("Equal to its demand offered traffic", e->getSelectedElements().stream().forEach(ee->{ if (isIpc.apply(ee)) toIpc.apply(ee).setCarriedTrafficInNoFailureStateGbps(toIpc.apply(ee).getIpUnicastDemand().getCurrentOfferedTrafficInGbps()); else toSc.apply(ee).setInitiallyInjectedTrafficGbps (toSc.apply(ee).getServiceChainRequest().getCurrentOfferedTrafficInGbps()); }) , (a,b)->b>0, null)
            		)));

            res.add(new AjtRcMenu("Set selected service chains VNF expansion factors", ee-> 
            {
                DialogBuilder.launch(
                        "Set VNF expansion factors" , 
                        "Please introduce one non-negative value per VNF traversed, indicating the ratio between the output traffic in the respective VNF, respect to the service chain initial injected traffic", 
                        "", 
                        this, 
                        Arrays.asList(InputForDialog.inputTfString("Space-separated expansion factors.", "Introduce the requested information", 10, "")),
                        (list)->
                        	{
                        		final String value = (String) list.get(0).get();
                        		final List<Double> expFactors = Stream.of(value.split(" ")).filter(e->e.isEmpty()).map(e->Double.parseDouble(e)).collect(Collectors.toList());
                        		if (expFactors.stream().anyMatch(e->e<0)) throw new Net2PlanException("All values must be non-negative");
                        		final int numVnfs = expFactors.size();
                        		for (Route r : getSelectedElements())
                        		{
                        			if (!isSc.apply(r)) continue;
                        			final WServiceChain sc = toSc.apply (r);
                        			if (sc.getSequenceOfTraversedVnfInstances().size() != numVnfs) continue;
                        			sc.setPathAndInitiallyInjectedTraffic(Optional.empty(), Optional.empty(), Optional.of(expFactors));
                        		}
                        	}
                        );
            } , (a,b)->b>0, null) );

            
            
    	} // if ipLayer
    	
    	if (isWdmLayer)
    	{
            res.add(new AjtRcMenu("Remove selected elements", e->getSelectedElements().forEach(dd->toLp.apply(dd).remove()) , (a,b)->b>0, null));

//            res.add(new AjtColumnInfo<Route>(this , Double.class, Arrays.asList("Optical signal") , "Injection power (dBm)", "The injection power of the transponder in the ADD part (transmission side)", (d,val)->toLp.apply(d).setAddTransponderInjectionPower_dBm((Double)val) , d->toLp.apply(d).getAddTransponderInjectionPower_dBm(), AGTYPE.NOAGGREGATION , null));

            res.add(new AjtRcMenu("Set optical slots occupied to selected lightpaths", e-> 
            DialogBuilder.launch(
                    "Set optical slots occupied" , 
                    "Please introduce the requested information", 
                    "", 
                    this, 
                    Arrays.asList(InputForDialog.inputTfString("Slots indexes (space separated)", "Introduce the modulation Id", 10, "")),
                    (list)->
                    	{
                    		final String value = (String) list.get(0).get();
                    		final SortedSet<Integer> vals = Stream.of(value.split(" ")).filter(ee->!ee.equals("") && !ee.equals(" ")).map(ee->Integer.parseInt(ee)).collect(Collectors.toCollection(TreeSet::new));
                    		getSelectedElements().stream().map(ee->toLp.apply(ee)).forEach(ee->ee.setOpticalSlotIds(vals));
                    	}
                    ) , (a,b)->b>0, null));

            res.add(new AjtRcMenu("Set modulation id to selected lightpaths", e-> 
            DialogBuilder.launch(
                    "Set modulation id" , 
                    "Please introduce the requested information", 
                    "", 
                    this, 
                    Arrays.asList(InputForDialog.inputTfString("Modulation Id", "Introduce the modulation Id", 10, "")),
                    (list)->
                    	{
                    		final String value = (String) list.get(0).get();
                    		getSelectedElements().stream().map(ee->toLp.apply(ee)).forEach(ee->ee.setModulationId(value));
                    	}
                    ) , (a,b)->b>0, null));
            res.add(new AjtRcMenu("Set transmission injection power (dBm) to selected lightpaths", e-> 
        	DialogBuilder.launch(
                "Set transmission injected power (dBm)" , 
                "Please introduce the requested information", 
                "", 
                this, 
                Arrays.asList(InputForDialog.inputTfDouble("Lightpath power measured at the output of the transponder (dBm)", "Introduce the requested information", 10, WNetConstants.WLIGHTPATH_DEFAULT_TRANSPONDERADDINJECTIONPOWER_DBM)),
                (list)->
                	{
                		final double value = (Double) list.get(0).get();
                		getSelectedElements().stream().map(ee->toLp.apply(ee)).forEach(ee->ee.setAddTransponderInjectionPower_dBm(value));
                	}
                ) , (a,b)->b>0, null));
            
            res.add(new AjtRcMenu("Set minimum acceptable reception power (dBm) to selected lightpaths", e-> 
            	DialogBuilder.launch(
                    "Set minimum acceptable reception power (dBm)" , 
                    "Please introduce the requested information", 
                    "", 
                    this, 
                    Arrays.asList(InputForDialog.inputTfDouble("Minimum acceptable reception power (dBm)", "Introduce the requested information", 10, WNetConstants.WLIGHTPATH_DEFAULT_MINIMUMACCEPTABLERECEPTIONPOWER_DBM)),
                    (list)->
                    	{
                    		final double value = (Double) list.get(0).get();
                    		getSelectedElements().stream().map(ee->toLp.apply(ee)).forEach(ee->ee.setTransponderMinimumTolerableReceptionPower_dBm(value));
                    	}
                    ) , (a,b)->b>0, null));
            res.add(new AjtRcMenu("Set maximum acceptable reception power (dBm) to selected lightpaths", e-> 
        	DialogBuilder.launch(
                "Set maximum acceptable reception power (dBm)" , 
                "Please introduce the requested information", 
                "", 
                this, 
                Arrays.asList(InputForDialog.inputTfDouble("Maximum acceptable reception power (dBm)", "Introduce the requested information", 10, WNetConstants.WLIGHTPATH_DEFAULT_MAXIMUMACCEPTABLERECEPTIONPOWER_DBM)),
                (list)->
                	{
                		final double value = (Double) list.get(0).get();
                		getSelectedElements().stream().map(ee->toLp.apply(ee)).forEach(ee->ee.setTransponderMaximumTolerableReceptionPower_dBm(value));
                	}
                ) , (a,b)->b>0, null));
            res.add(new AjtRcMenu("Set minimum acceptable OSNR (dB) at 12.5 GHZ ref. BW, to selected lightpaths", e-> 
        	DialogBuilder.launch(
                "Set minimum acceptable OSNR (dB) at 12.5 GHZ reference bandwidth" , 
                "Please introduce the requested information", 
                "", 
                this, 
                Arrays.asList(InputForDialog.inputTfDouble("Minimum acceptable reception OSNR (dB) at 12.5 GHZ reference bandwidth", "Introduce the requested information", 10, WNetConstants.WLIGHTPATH_DEFAULT_MINIMUMACCEPTABLEOSNRAT12_5GHZREFBW_DB)),
                (list)->
                	{
                		final double value = (Double) list.get(0).get();
                		getSelectedElements().stream().map(ee->toLp.apply(ee)).forEach(ee->ee.setTransponderMinimumTolerableOsnrAt12_5GHzOfRefBw_dB(value));
                	}
                ) , (a,b)->b>0, null));
            res.add(new AjtRcMenu("Set maximum tolerable absolute CD (ps/nm), to selected lightpaths", e-> 
        	DialogBuilder.launch(
                "Set maximum tolerable absolute chromatic dispersion (CD) in reception" , 
                "Please introduce the requested information", 
                "", 
                this, 
                Arrays.asList(InputForDialog.inputTfDouble("Maximum tolerable absolute CD in reception", "Introduce the requested information", 10, WNetConstants.WLIGHTPATH_DEFAULT_MAXIMUMABSOLUTE_CD_PSPERNM)),
                (list)->
                	{
                		final double value = (Double) list.get(0).get();
                		getSelectedElements().stream().map(ee->toLp.apply(ee)).forEach(ee->ee.setTransponderMaximumTolerableCdInAbsoluteValue_perPerNm(value));
                	}
                ) , (a,b)->b>0, null));
            res.add(new AjtRcMenu("Set maximum tolerable PMD (ps) in reception to selected lightpaths", e-> 
        	DialogBuilder.launch(
                "Set maximum tolerable Polarization Mode Dispersion (PMD) in reception" , 
                "Please introduce the requested information", 
                "", 
                this, 
                Arrays.asList(InputForDialog.inputTfDouble("Maximum PMD (ps) in reception", "Introduce the requested information", 10, WNetConstants.WLIGHTPATH_DEFAULT_MAXIMUMPMD_PS)),
                (list)->
                	{
                		final double value = (Double) list.get(0).get();
                		getSelectedElements().stream().map(ee->toLp.apply(ee)).forEach(ee->ee.setTransponderMaximumTolerablePmd_ps(value));
                	}
                ) , (a,b)->b>0, null));
            
    	}

        return res;
    }
    
}
