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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.DialogBuilder;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.InputForDialog;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.monitoring.MonitoringUtils;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IMonitorizableElement;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.niw.OpticalSpectrumManager;
import com.net2plan.niw.WAbstractIpUnicastOrAnycastDemand;
import com.net2plan.niw.WAbstractNetworkElement;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WIpLink;
import com.net2plan.niw.WIpUnicastDemand;
import com.net2plan.niw.WLightpath;
import com.net2plan.niw.WLightpathRequest;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNetConstants;
import com.net2plan.niw.WNetConstants.WTYPE;
import com.net2plan.niw.WNode;
import com.net2plan.niw.WServiceChainRequest;
import com.net2plan.niw.WVnfInstance;
import com.net2plan.utils.Pair;

/**
 */
@SuppressWarnings({ "unchecked", "serial" })
public class Niw_AdvancedJTable_demand extends AdvancedJTable_networkElement<Demand>
{
	private static DecimalFormat df = new DecimalFormat("#.##");
    public Niw_AdvancedJTable_demand(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.DEMANDS , layerThisTable.getName().equals(WNetConstants.ipLayerName)? "IP demands" : "Lightpath requests" , layerThisTable , true , null);
    }

    @Override
  public List<AjtColumnInfo<Demand>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
    	final List<AjtColumnInfo<Demand>> res = new LinkedList<> ();
    	assert callback.getNiwInfo().getFirst();
    	final WNet wNet = callback.getNiwInfo().getSecond(); 
    	final boolean isIpLayer = getTableNetworkLayer().getName ().equals(WNetConstants.ipLayerName);
    	final boolean isWdmLayer = getTableNetworkLayer().getName ().equals(WNetConstants.wdmLayerName);
    	assert isIpLayer || isWdmLayer;
    	assert !(isIpLayer && isWdmLayer);
    	if (isIpLayer)
    	{
        	final SortedMap<Link,SortedMap<String,Pair<Double,Double>>> perLink_qos2occupationAndViolationMap = callback.getDesign().getAllLinksPerQosOccupationAndQosViolationMap(layerThisTable);
        	final BiFunction<Link,Demand,Double> violationGbps = (e,d) -> perLink_qos2occupationAndViolationMap.getOrDefault (e , new TreeMap<>()).getOrDefault (d.getQosType () , Pair.of (0.0 , 0.0)).getSecond ();
        			
        	final Function<Demand,Boolean> isWScr = d ->wNet.getWElement(d).get().isServiceChainRequest();
        	final Function<Demand,Boolean> isWIpUnicast = d ->wNet.getWElement(d).get().isWIpUnicastDemand();
        	final Function<Demand,Boolean> isWAbstractIpD = d ->isWScr.apply(d) || isWIpUnicast.apply(d); 
        	final Function<Demand,WIpUnicastDemand> toWIpUnicast = d -> (WIpUnicastDemand) wNet.getWElement(d).get();
        	final Function<Demand,WServiceChainRequest> toWScr = d ->(WServiceChainRequest) wNet.getWElement(d).get();
        	final Function<Demand,WAbstractIpUnicastOrAnycastDemand> toAbsIp = d ->(WAbstractIpUnicastOrAnycastDemand) wNet.getWElement(d).get();
        	res.add(new AjtColumnInfo<Demand>(this , Node.class, null , "A", "Ingress router/s", null , d->
        	{  
        		if (d.isCoupled ()) throw new RuntimeException ("The IP demand in NIW is coupled. These demands should be filtered out");
        		if (isWIpUnicast.apply(d))
        			return toWIpUnicast.apply(d).getA().getNe ();
        		else
        			return toWScr.apply(d).getPotentiallyValidOrigins().stream().map(e->e.getNe ()).collect (Collectors.toList());
        	} , AGTYPE.NOAGGREGATION , null) );
        	res.add(new AjtColumnInfo<Demand>(this , Node.class, null , "E", "Egress router/s", null , d->
        	{  
        		if (d.isCoupled ()) throw new RuntimeException ("The IP demand in NIW is coupled. These demands should be filtered out");
        		if (isWIpUnicast.apply(d))
        			return toWIpUnicast.apply(d).getB().getNe ();
        		else
        			return toWScr.apply(d).getPotentiallyValidDestinations().stream().map(e->e.getNe ()).collect (Collectors.toList());
        	} , AGTYPE.NOAGGREGATION , null) );
            res.add(new AjtColumnInfo<Demand>(this , Boolean.class, null , "Anycast SCR?", "Indicates if this demand is an anycast service chain request", null , d->!wNet.getWElement(d).get().isWIpUnicastDemand() , AGTYPE.COUNTTRUE , null));
            res.add(new AjtColumnInfo<Demand>(this , String.class, null , "Type", "Indicates if this demand is a IP unicast demand, or an servichain request i.e. potentially anycast, potentially traversing VNFs, and carried by service chains", null , d->isWIpUnicast.apply(d)? "IP unicast demand" : "Service chain request" , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Demand>(this , String.class, null , "Routing type", "For unicast IP demands, indicates if its routing type is HOP-BY-HOP routing, or connection-based source routing. Service chain request are always of the source routing type", null, d->isWIpUnicast.apply(d)? (toWIpUnicast.apply(d).isIpHopByHopRouted()? "Hop-by-hop" : "Source routing") : "Source routing" , AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Demand>(this , String.class, null , "US/DS", "Indicates if this demand is upstream or downstream", null , d->toAbsIp.apply(d).isDownstream()? "Downstream" : "Upstream" , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Demand>(this , Demand.class, null , "Bidirectional pair", "If the demand is bidirectional, provides its bidirectional pair", null , d->toAbsIp.apply(d).isBidirectional()? toAbsIp.apply(d).getBidirectionalPair().get().getNe() : null , AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Offered traffic (Gbps)", "Currently offered traffic by the demand in Gbps", (d,val)->toAbsIp.apply(d).setCurrentOfferedTrafficInGbps((Double) val), d->toAbsIp.apply(d).getCurrentOfferedTrafficInGbps() , AGTYPE.SUMDOUBLE , null));
            res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Carried traffic (Gbps)", "Currently carried traffic by the demand in Gbps", null , d->toAbsIp.apply(d).getCurrentCarriedTrafficGbps() , AGTYPE.SUMDOUBLE , null));
            res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "% Lost traffic", "Percentage of the lost traffic by the demand", null, d->toAbsIp.apply(d).getCurrentOfferedTrafficInGbps() == 0? 0 : 100.0 * toAbsIp.apply(d).getCurrentBlockedTraffic() / toAbsIp.apply(d).getCurrentOfferedTrafficInGbps() , AGTYPE.NOAGGREGATION , d->toAbsIp.apply(d).getCurrentBlockedTraffic() > Configuration.precisionFactor? Color.RED : Color.GREEN));
            res.add(new AjtColumnInfo<Demand>(this , String.class, null , "QoS type", "A used-defined string identifying the QoS type of traffic of the demand. QoS differentiation in the IP links is possible for each QoS type", (d,val)-> toAbsIp.apply(d).setQosType((String)val) , d->toAbsIp.apply(d).getQosType(), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Demand>(this , String.class, null , "WC Oversubscription", "The worst case, among all the traversed links, of the amount of traffic of this demand that is oversubscribed", null , d->d.getTraversedLinksAndCarriedTraffic(false).keySet().stream().mapToDouble (e -> violationGbps.apply(e, d)).max().orElse(0.0) , AGTYPE.NOAGGREGATION , d-> d.getTraversedLinksAndCarriedTraffic(false).keySet().stream().mapToDouble (e -> violationGbps.apply(e, d)).max().orElse(0.0) > Configuration.precisionFactor? Color.red : Color.green));
            res.add(new AjtColumnInfo<Demand>(this , String.class, null , "Bifurcated?", "Indicates whether the demand is satisfied by more than one path from origin to destination", null, d->!d.isSourceRouting() ? "-" : (d.isBifurcated()) ? String.format("Yes (%d)", d.getRoutes().size()) : "No" , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Worst e2e lat (ms)", "Current worst case end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any)", null, d->toAbsIp.apply(d).getWorstCaseEndtoEndLatencyMs() , AGTYPE.NOAGGREGATION , d->{ if (isWScr.apply(d)) return null; final double maxMs = toWIpUnicast.apply(d).getMaximumAcceptableE2EWorstCaseLatencyInMs(); return maxMs <= 0? null : (toWIpUnicast.apply(d).getWorstCaseEndtoEndLatencyMs() > maxMs? Color.RED : null); }));
            res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Worst e2e length (km)", "Current worst case end-to-end propagation length in km (accumulating any lower layer propagation lengths if any)", null, d->toAbsIp.apply(d).getWorstCaseEndtoEndLengthInKm() , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Limit e2e lat (ms)", "Maximum end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any)", (d,val)-> { if(isWIpUnicast.apply (d)) d.setMaximumAcceptableE2EWorstCaseLatencyInMs((Double)val); } , d-> isWIpUnicast.apply (d)? d.getMaximumAcceptableE2EWorstCaseLatencyInMs() : "--" , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Demand>(this , String.class, null , "UD: IP connections", "For IP unicast demands, routed via source-routing, indicates the IP connections carrying traffic of this demand", null, d->isWIpUnicast.apply(d)? toWIpUnicast.apply(d).getIpConnections().stream().map(e->e.getNe()).collect(Collectors.toList()) : "" , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Demand>(this , String.class, null , "SC: VNFs types to traverse", "For service chain requests, the sequence of VNF types that has to be traversed by the service chains", null, d->isWIpUnicast.apply(d)? "" : d.getServiceChainSequenceOfTraversedResourceTypes().stream().collect(Collectors.joining(",")) , AGTYPE.COUNTTRUE , null));
            res.add(new AjtColumnInfo<Demand>(this , Collection.class, null , "SC: # Service chains", "Number of associated service chains, if this is a service chain request", null, d->isWScr.apply(d)? d.getRoutes (): new ArrayList<> () , AGTYPE.SUMINT, null));
            res.add(new AjtColumnInfo<Demand>(this , Collection.class, null , "SC: Max. lancies", "Returns the list of maximum latencies specified for this service chain request. The list contains V+1 values, being V the number of VNFs to traverse. The first V values (i=1,...,V) correspond to the maximum latency from the origin node, to the input of the i-th VNF traversed. The last value correspon to the maximum latency from the origin node to the end node.", null, d->isWIpUnicast.apply(d)? null : toWScr.apply(d).getListMaxLatencyFromOriginToVnStartAndToEndNode_ms().stream().map(e->df.format(e)).collect(Collectors.joining(" ")) , AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Demand>(this , Collection.class, null , "SC: Default Exp. factors", "Gets the sequence of default expansion factors to apply when traversing a VNF, for the service chains realizing this request", null, d->isWIpUnicast.apply(d)? null : toWScr.apply(d).getDefaultSequenceOfExpansionFactorsRespectToInjection().stream().map(e->df.format(e)).collect(Collectors.joining(" ")) , AGTYPE.NOAGGREGATION, null));
            res.addAll(Niw_AdvancedJTable_demand.getMonitoringAndTrafficEstimationColumns(this).stream().map(c->(AjtColumnInfo<Demand>)(AjtColumnInfo<?>)c).collect(Collectors.toList()));
    	}
    	else
    	{
    	      res.add(new AjtColumnInfo<Demand>(this , Node.class, null , "A", "Lighptath request origin", null , d->d.getIngressNode() , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Demand>(this , Node.class, null , "B", "Lighptath request destination", null , d->d.getEgressNode() , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Demand>(this , Demand.class, null , "Bidirectional pair", "If the lightpath request is bidirectional, provides its bidirectional pair", null , d->d.getBidirectionalPair() , AGTYPE.NOAGGREGATION, null));
    	      res.add(new AjtColumnInfo<Demand>(this , Link.class, null , "IP link coupled", "The IP link that this lightpath request is coupled to", null , d-> { final WLightpathRequest lp = new WLightpathRequest(d); if (lp.isCoupledToIpLink()) return lp.getCoupledIpLink().getNe(); else return null; } , AGTYPE.NOAGGREGATION , null) );
    	      res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Line rate (Gbps)", "Line rate of the lightpaths to carry this demand", (d,val)->new WLightpathRequest(d).setLineRateGbps((Double) val), d->new WLightpathRequest(d).getLineRateGbps() , AGTYPE.SUMDOUBLE , null));
    	      res.add(new AjtColumnInfo<Demand>(this , Collection.class, null , "# Lightpaths", "Number of associated lightpaths, realizing this request. If more than one, all are of the same line rate, and in backup relation (e.g. 1+1, one main, one backup)", null, d->d.getRoutes() , AGTYPE.SUMINT, null));
    	      res.add(new AjtColumnInfo<Demand>(this , Boolean.class, null , "1+1?", "Indicates if this lightpath request IS now 1+1 protected, that is, has two lightpaths protecting it in 1+1", null , d->new WLightpathRequest(d).is11Protected() , AGTYPE.COUNTTRUE, d-> new WLightpathRequest(d).is11Protected() != new WLightpathRequest(d).isToBe11Protected() ? Color.red : null) );
    	      res.add(new AjtColumnInfo<Demand>(this , Boolean.class, null , "To be 1+1?", "Indicates if this lightpath request SHOULD BE 1+1 protected", (d,val)->new WLightpathRequest(d).setIsToBe11Protected((Boolean)val) , d->new WLightpathRequest(d).isToBe11Protected() , AGTYPE.COUNTTRUE, null));
    	      res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Up?", "Indicates the lightpath request is up, meaning that at least one lightpath realizing it is up", null, d->!(new WLightpathRequest(d).isBlocked()) , AGTYPE.COUNTTRUE , d->(new WLightpathRequest(d).isBlocked())? Color.RED : Color.GREEN));
    	      res.add(new AjtColumnInfo<Demand>(this , String.class, null , "Tp name", "Name of the transponder realizing the lightpaths of this lightpath request", (d,val)-> new WLightpathRequest(d).setTransponderName((String) val) , d->new WLightpathRequest(d).getTransponderName().orElse("--") , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Worst e2e lat (ms)", "Current worst case end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any)", null, d->d.getWorstCasePropagationTimeInMs() , AGTYPE.NOAGGREGATION , d->{ final double maxMs = d.getMaximumAcceptableE2EWorstCaseLatencyInMs(); return maxMs <= 0? null : (d.getWorstCasePropagationTimeInMs() > maxMs? Color.RED : null); }));
    	      res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Worst e2e length (km)", "Current worst case end-to-end propagation length in km (accumulating any lower layer propagation lengths if any)", null, d->d.getWorstCaseLengthInKm() , AGTYPE.NOAGGREGATION , null));
    	}
      return res;
  }

    @Override
    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
        final List<AjtRcMenu> res = new ArrayList<> ();
    	final WNet wNet = callback.getNiwInfo().getSecond(); 
    	final Function<Demand,Boolean> isWScr = d ->wNet.getWElement(d).get().isServiceChainRequest();
    	final Function<Demand,Boolean> isWIpUnicast = d ->wNet.getWElement(d).get().isWIpUnicastDemand();
    	final Function<Demand,Boolean> isWAbstractIpD = d ->isWScr.apply(d) || isWIpUnicast.apply(d); 
    	final Function<Demand,Boolean> isLpr = d ->wNet.getWType(d).equals (Optional.of(WTYPE.WLightpathRequest)); 
    	final Function<Demand,WIpUnicastDemand> toWIpUnicast = d -> (WIpUnicastDemand) wNet.getWElement(d).get();
    	final Function<Demand,WServiceChainRequest> toWScr = d ->(WServiceChainRequest) wNet.getWElement(d).get();
    	final Function<Demand,WAbstractIpUnicastOrAnycastDemand> toAbsIp = d ->(WAbstractIpUnicastOrAnycastDemand) wNet.getWElement(d).get();
    	final Function<Demand,WLightpathRequest> toLpr = d ->(WLightpathRequest) wNet.getWElement(d).get();
    	final boolean isIpLayer = getTableNetworkLayer().getName ().equals(WNetConstants.ipLayerName);
    	final boolean isWdmLayer = getTableNetworkLayer().getName ().equals(WNetConstants.wdmLayerName);
    	assert isIpLayer || isWdmLayer;
    	assert !(isIpLayer && isWdmLayer);
		final Function<String,Optional<WNode>> nodeByName = st -> 
		{
    		WNode a = wNet.getNodeByName(st).orElse(null);
    		if (a == null) a = wNet.getNodes().stream().filter(n->n.getName().equalsIgnoreCase(st)).findFirst().orElse(null);
    		return Optional.ofNullable(a);
		};
    	
    	if (isIpLayer)
    	{
    		res.add (new AjtRcMenu("Add IP Unicast demand", e->
            {
                DialogBuilder.launch(
                        "Add IP Unicast demand" , 
                        "Please introduce the required data", 
                        "", 
                        this, 
                        Arrays.asList(
                        		InputForDialog.inputTfString("Origin node name", "Introduce the name of the origin node. First tries case sensitive, later case insensitive", 10, ""),
                        		InputForDialog.inputTfString("Destination node name", "Introduce the name of the destination node. First tries case sensitive, later case insensitive", 10, ""),
                        		InputForDialog.inputCheckBox("Upstream?", "Indicate if the demand is upstream. Otherwise, it is considered downstream", false , null)
                        		),
                        (list)->
                        	{
                        		final String aName  = (String) list.get(0).get();
                        		final String bName  = (String) list.get(1).get();
                        		final Boolean upstream = (Boolean) list.get(2).get();
                        		final WNode a = nodeByName.apply(aName).orElse(null);
                        		final WNode b = nodeByName.apply(bName).orElse(null);
                        		if (a == null || b == null) throw new Net2PlanException("Unkown node name. " + (a == null? aName : bName));
                        		wNet.addIpUnicastDemand(a, b, upstream, true);
                        	}
                        );
            } , (a,b)->b>0, null));         		
	        res.add(new AjtRcMenu("Add one IP unicast demand per selected node pair (all if none selected)", null, (a,b)->true, Arrays.asList(
	        		new AjtRcMenu("as hop-by-hop routing (e.g. for OSPF routing)", e->rcMenuFullMeshTraffic(true), (a,b)->true, null),
	        		new AjtRcMenu("as source-routing (e.g. for MPLS-TE routing)", e->rcMenuFullMeshTraffic(false), (a,b)->true, null)
	        		)
	        		));
    		
        	res.add(new AjtRcMenu("Add anycast service chain request", e->
            {
                DialogBuilder.launch(
                        "Add anycast service chain request" , 
                        "Please introduce the required data", 
                        "", 
                        this, 
                        Arrays.asList(
                        		InputForDialog.inputTfString("Origin nodes", "Introduce the names (comma or space separated) of the origin nodes of this asnycast service chain request. For each node, first tries case sensitive, later case insensitive", 20, ""),
                        		InputForDialog.inputTfString("Destination nodes", "Introduce the names (comma or space separated) of the destination nodes of this asnycast service chain request. For each node, first tries case sensitive, later case insensitive", 20, ""),
                        		InputForDialog.inputTfString("VNF types to traverse", "Introduce the names (comma or space separated) of the types of the VNFs to traverse", 20, ""),
                        		InputForDialog.inputCheckBox("Upstream?", "Indicate if the demand is upstream. Otherwise, it is considered downstream", false , null)
                        		),
                        (list)->
                        	{
                        		final String aNames  = (String) list.get(0).get();
                        		final String bNames  = (String) list.get(1).get();
                        		final String vnfTypes = (String) list.get(2).get();
                        		final Boolean upstream = (Boolean) list.get(3).get();
                        		final List<String> aNamesArray = Stream.of (aNames.split(", ")).filter(a->!a.equals("")).collect(Collectors.toList());
                        		final List<String> bNamesArray = Stream.of (bNames.split(", ")).filter(a->!a.equals("")).collect(Collectors.toList());
                        		final List<String> vnfsArray = Stream.of (vnfTypes.split(", ")).filter(a->!a.equals("")).collect(Collectors.toList());
                        		final SortedSet<WNode> aNodes = aNamesArray.stream().map(n->nodeByName.apply(n).orElse(null)).filter(n->n!= null).collect(Collectors.toCollection(TreeSet::new));
                        		final SortedSet<WNode> bNodes = bNamesArray.stream().map(n->nodeByName.apply(n).orElse(null)).filter(n->n!= null).collect(Collectors.toCollection(TreeSet::new));
                        		wNet.addServiceChainRequest(aNodes, bNodes , vnfsArray , upstream , Optional.empty() , Optional.empty());
                        	}
                        );
            } , (a,b)->b>0, null));         		
            res.add(new AjtRcMenu("Add IP source routed connection to selected unicast demands", null , (a, b)->true, Arrays.asList( 
            		new AjtRcMenu("as shortest path in latency", e->
            		{
            			final List<WIpUnicastDemand> ds = getSelectedElements().stream().filter(ee->wNet.getWType(ee).equals(Optional.of(WTYPE.WIpUnicastDemand))).map(ee->wNet.getWElement(ee).get().getAsIpUnicastDemand()).collect(Collectors.toList());
            			final Map<WIpLink , Double> costMap = new HashMap<> ();
            			for (WIpLink ee : wNet.getIpLinks()) costMap.put(ee, ee.getWorstCasePropagationDelayInMs());
            			for (WIpUnicastDemand dd : ds)
            			{
            				if (dd.isIpHopByHopRouted()) continue;
            				final List<List<WIpLink>> sps = wNet.getKShortestIpUnicastPath(1, wNet.getNodes(), wNet.getIpLinks(), dd.getA(), dd.getB(), Optional.of(costMap));
            				if (sps.size() != 1) continue;
            				dd.addIpSourceRoutedConnection(sps.get(0), dd.getCurrentOfferedTrafficInGbps());
            			}
            		} , (a, b)->true, null) , 
            		new AjtRcMenu("as shortest path in num hops", e->
            		{
            			final List<WIpUnicastDemand> ds = getSelectedElements().stream().filter(ee->wNet.getWType(ee).equals(Optional.of(WTYPE.WIpUnicastDemand))).map(ee->wNet.getWElement(ee).get().getAsIpUnicastDemand()).collect(Collectors.toList());
            			final Map<WIpLink , Double> costMap = new HashMap<> ();
            			for (WIpLink ee : wNet.getIpLinks()) costMap.put(ee, 1.0);
            			for (WIpUnicastDemand dd : ds)
            			{
            				if (dd.isIpHopByHopRouted()) continue;
            				final List<List<WIpLink>> sps = wNet.getKShortestIpUnicastPath(1, wNet.getNodes(), wNet.getIpLinks(), dd.getA(), dd.getB(), Optional.of(costMap));
            				if (sps.size() != 1) continue;
            				dd.addIpSourceRoutedConnection(sps.get(0), dd.getCurrentOfferedTrafficInGbps());
            			}
            		} , (a, b)->true, null)  
            		)));
            res.add(new AjtRcMenu("Add service chain to selected service chain requests", null , (a, b)->true, Arrays.asList( 
            		new AjtRcMenu("as shortest path in latency", e->
            		{
            			final List<WServiceChainRequest> ds = getSelectedElements().stream().filter(ee->wNet.getWType(ee).equals(Optional.of(WTYPE.WServiceChainRequest))).map(ee->wNet.getWElement(ee).get().getAsServiceChainRequest()).collect(Collectors.toList());
            			final Map<WIpLink , Double> costMap = new HashMap<> ();
            			for (WIpLink ee : wNet.getIpLinks()) costMap.put(ee, ee.getWorstCasePropagationDelayInMs());
            			final Function<List<WAbstractNetworkElement> ,Double> getCost = sp -> sp.stream().mapToDouble(ee->ee.isWIpLink()? costMap.get((WIpLink)ee) : ((WVnfInstance) ee).getProcessingTimeInMs()).sum();
            			for (WServiceChainRequest dd : ds)
            			{
            				List<WAbstractNetworkElement> bestOption = null;
            				double bestOptionCost = Double.MAX_VALUE;
            				for (WNode a : dd.getPotentiallyValidOrigins())
            					for (WNode b : dd.getPotentiallyValidDestinations())
            					{
                    				final List<List<WAbstractNetworkElement>> sps = wNet.getKShortestServiceChainInIpLayer(1, a, b, dd.getSequenceVnfTypes(), Optional.of(costMap), Optional.empty());
                    				if (sps.size() != 1) continue;
                    				final double cost = getCost.apply(sps.get(0));
                    				if (cost < bestOptionCost) { bestOptionCost = cost; bestOption = sps.get(0); }
            					}
            				if (bestOption != null)
            					dd.addServiceChain(bestOption, dd.getCurrentOfferedTrafficInGbps());
            			}
            		} , (a, b)->true, null) , 
            		new AjtRcMenu("as shortest path in num IP hops", e->
            		{
            			final List<WServiceChainRequest> ds = getSelectedElements().stream().filter(ee->wNet.getWType(ee).equals(Optional.of(WTYPE.WServiceChainRequest))).map(ee->wNet.getWElement(ee).get().getAsServiceChainRequest()).collect(Collectors.toList());
            			final Map<WIpLink , Double> costMap = new HashMap<> ();
            			for (WIpLink ee : wNet.getIpLinks()) costMap.put(ee, 1.0);
            			final Function<List<WAbstractNetworkElement> ,Double> getCost = sp -> sp.stream().mapToDouble(ee->ee.isWIpLink()? costMap.get((WIpLink)ee) : 0.0).sum();
            			for (WServiceChainRequest dd : ds)
            			{
            				List<WAbstractNetworkElement> bestOption = null;
            				double bestOptionCost = Double.MAX_VALUE;
            				for (WNode a : dd.getPotentiallyValidOrigins())
            					for (WNode b : dd.getPotentiallyValidDestinations())
            					{
                    				final List<List<WAbstractNetworkElement>> sps = wNet.getKShortestServiceChainInIpLayer(1, a, b, dd.getSequenceVnfTypes(), Optional.of(costMap), Optional.empty());
                    				if (sps.size() != 1) continue;
                    				final double cost = getCost.apply(sps.get(0));
                    				if (cost < bestOptionCost) { bestOptionCost = cost; bestOption = sps.get(0); }
            					}
            				if (bestOption != null)
            					dd.addServiceChain(bestOption, dd.getCurrentOfferedTrafficInGbps());
            			}
            		} , (a, b)->true, null)  
            		)));
    		
        	res.add(new AjtRcMenu("Remove selected demands", e->getSelectedElements().forEach(dd->toAbsIp.apply (dd).remove ()) , (a,b)->b>0, null));

	        res.add(new AjtRcMenu("Arrange selected IP unicast demands in bidirectional pairs", e->
	        {
	        	final SortedSet<WIpUnicastDemand> nonBidiDemands = getSelectedElements().stream().filter(ee->isWIpUnicast.apply(ee)).map(ee->toWIpUnicast.apply(ee)).filter(ee->!ee.isBidirectional()).collect(Collectors.toCollection(TreeSet::new));
	        	final Map<Pair<WNode,WNode> , WIpUnicastDemand> nodePair2demand = new HashMap<>();
	        	for (WIpUnicastDemand ee : nonBidiDemands)
	        	{
	        		final Pair<WNode,WNode> pair = Pair.of(ee.getA() , ee.getB());
	        		if (nodePair2demand.containsKey(pair)) throw new Net2PlanException ("At most one link per node pair is allowed");
	        		nodePair2demand.put(pair, ee);
	        	}
	        	for (WIpUnicastDemand ee : nonBidiDemands)
	        	{
	        		if (ee.isBidirectional()) continue;
	        		final WIpUnicastDemand opposite = nodePair2demand.get(Pair.of(ee.getB(), ee.getA()));
	        		if (opposite == null) continue;
	        		if (opposite.isBidirectional()) continue;
	        		if (opposite.isDownstream() == ee.isDownstream()) continue;
	        		ee.setBidirectionalPair(opposite);
	        	}
	        }
	        , (a,b)->b>0, null));

        	res.add(new AjtRcMenu("Arrange selected service chains in bidirectional pairs", e->
	        {
	        	final SortedSet<WServiceChainRequest> nonBidiDemands = getSelectedElements().stream().filter(ee->isWScr.apply(ee)).map(ee->toWScr.apply(ee)).filter(ee->!ee.isBidirectional()).collect(Collectors.toCollection(TreeSet::new));
	        	final Map<Pair<SortedSet<WNode>,SortedSet<WNode>> , WServiceChainRequest> nodePair2demand = new HashMap<>();
	        	for (WServiceChainRequest ee : nonBidiDemands)
	        	{
	        		final Pair<SortedSet<WNode>,SortedSet<WNode>> pair = Pair.of(ee.getPotentiallyValidOrigins() , ee.getPotentiallyValidDestinations());
	        		if (nodePair2demand.containsKey(pair)) throw new Net2PlanException ("At most one demand per origin-destination nodes pair is allowed");
	        		nodePair2demand.put(pair, ee);
	        	}
	        	for (WServiceChainRequest ee : nonBidiDemands)
	        	{
	        		if (ee.isBidirectional()) continue;
	        		final WServiceChainRequest opposite = nodePair2demand.get(Pair.of(ee.getPotentiallyValidDestinations(), ee.getPotentiallyValidOrigins()));
	        		if (opposite == null) continue;
	        		if (opposite.isBidirectional()) continue;
	        		if (opposite.isDownstream() == ee.isDownstream()) continue;
	        		ee.setBidirectionalPair(opposite);
	        	}
	        }
	        , (a,b)->b>0, null));
        	res.add(new AjtRcMenu("Set routing type of seleted IP unicast demands", null , (a,b)->b>0, Arrays.asList(
            		new AjtRcMenu("as hop-by-hop routing (e.g. for OSPF routing)", e-> getSelectedElements().stream().filter(d->isWIpUnicast.apply(d)).forEach(dd->toWIpUnicast.apply (dd).setAsHopByHopRouted ()), (a,b)->b>0, null),
            		new AjtRcMenu("as source-routing (e.g. for MPLS-TE routing)", e-> getSelectedElements().stream().filter(d->isWIpUnicast.apply(d)).forEach(dd->toWIpUnicast.apply (dd).setAsSourceRouted ()), (a,b)->b>0, null)
            		)));

    	        res.add(new AjtRcMenu("Set QoS type to selected elements", e->
    	        {
    	            DialogBuilder.launch(
    	            		"Set selected demands QoS type", 
    	                    "Please introduce the QoS type.", 
    	                    "", 
    	                    this, 
    	                    Arrays.asList(InputForDialog.inputTfString ("Qos type", "Introduce the QoS type of the demands" , 10 , "")),
    	                    (list)->
    	                    	{
    	                    		final String qos = (String) list.get(0).get();
    	                    		getSelectedElements().forEach(dd->toAbsIp.apply(dd).setQosType(qos));
    	                    	}
    	                    );
    	        }, (a,b)->b>0, null));
    	    	
    	        
            	res.add(new AjtRcMenu("Set maximum e2e limit to selected unicast demands", e->
    	        {
    	            DialogBuilder.launch(
    	            		"Set maximum e2e limit to selected unicast demands", 
    	                    "Please introduce the maximum end-to-end limit in ms, to set for the selected demands.", 
    	                    "", 
    	                    this, 
    	                    Arrays.asList(InputForDialog.inputTfDouble("Maximum end-to-end limit (ms)", "Introduce the maximum end-to-end limit in miliseconds", 10, 50.0)),
    	                    (list)->
    	                    	{
    	                    		final double newLimit = (Double) list.get(0).get();
    	                    		getSelectedElements().forEach(dd->((Demand)dd).setMaximumAcceptableE2EWorstCaseLatencyInMs(newLimit));
    	                    	}
    	                    );
    	        }, (a,b)->b>0, null));

    	        res.add(new AjtRcMenu("Set selected demands offered traffic", null, (a, b) -> b>0, Arrays.asList(
    	        		new AjtRcMenu("as constant traffic", e ->
    	    			{
    	    	            DialogBuilder.launch(
    	    	                    "Set selected demands offered traffic (Gbps)", 
    	    	                    "Please introduce the offered traffic. Negative values are not allowed", 
    	    	                    "", 
    	    	                    this, 
    	    	                    Arrays.asList(InputForDialog.inputTfDouble("Offered traffic (Gbps)", "Introduce the offered traffic", 10, 0.0)),
    	    	                    (list)->
    	    	                    	{
    	    	                    		final double newOfferedTraffic = (Double) list.get(0).get();
    	    	                    		final List<WAbstractIpUnicastOrAnycastDemand> changedDemands = getSelectedElements().stream().map(ee->(Demand)ee).map(d->toAbsIp.apply(d)).collect(Collectors.toList());
    	    	                        	changedDemands.forEach(d->d.setCurrentOfferedTrafficInGbps(newOfferedTraffic));
    	    	                    	}
    	    	                    );
    	    			}
    	    			, (a, b) -> b>0, null),
    	        		new AjtRcMenu("as a random uniform value", e ->
    	    			{
    	    				final Random rng = new Random ();
    	    	    		final List<WAbstractIpUnicastOrAnycastDemand> changedDemands = getSelectedElements().stream().map(ee->(Demand)ee).map(d->toAbsIp.apply(d)).collect(Collectors.toList());
    	    	        	changedDemands.forEach(d->d.setCurrentOfferedTrafficInGbps(rng.nextDouble()));
    	    			}
    	    			, (a, b) -> b>0, null),
    	        		
    	        		new AjtRcMenu("proportional to sum of end nodes' populations (unicast demands)", e ->
    	    			{
    	    	            DialogBuilder.launch(
    	    	                    "Scale selected demands offered traffic", 
    	    	                    "Please introduce the total traffic to normalize the demands. Negative values are not allowed", 
    	    	                    "", 
    	    	                    this, 
    	    	                    Arrays.asList(InputForDialog.inputTfDouble("Total traffic of selected demands (Gbpd)", "Introduce the total traffic in Gbps, so selected demands will be scaled making them have an aggregated traffic equal to this value", 10, 1000.0)),
    	    	                    (list)->
    	    	                    	{
    	    	                    		final double totalTrafficExpectedGbps = (Double) list.get(0).get();
    	    	                    		final List<WIpUnicastDemand> changedDemands = getSelectedElements().stream().map(ee->(Demand)ee).filter(ee->isWIpUnicast.apply(ee)).map(d->toWIpUnicast.apply(d)).collect(Collectors.toList());
    	    	                    		double totalPopulationSums = changedDemands.stream().mapToDouble(ee->ee.getA().getPopulation() + ee.getB().getPopulation()).sum();
    	    	                    		if (totalPopulationSums <= 0) totalPopulationSums = 1; 
    	    	                    		final double neScalingFactor = totalTrafficExpectedGbps / totalPopulationSums;
    	    	                        	changedDemands.forEach(d->d.setCurrentOfferedTrafficInGbps((d.getA().getPopulation () + d.getB().getPopulation()) * neScalingFactor));
    	    	                    	}
    	    	                    );
    	    			}
    	    			, (a, b) -> b>0, null),

    	        		new AjtRcMenu("as scaled version", e ->
    	    			{
    	    	            DialogBuilder.launch(
    	    	                    "Scale selected demands offered traffic", 
    	    	                    "Please introduce the factor for which the offered traffic will be multiplied. Negative values are not allowed", 
    	    	                    "", 
    	    	                    this, 
    	    	                    Arrays.asList(InputForDialog.inputTfDouble("Scaling factor", "Introduce the scaling factor", 10, 2.0)),
    	    	                    (list)->
    	    	                    	{
    	    	                    		final double neScalingFactor = (Double) list.get(0).get();
    	    	                    		final List<WAbstractIpUnicastOrAnycastDemand> changedDemands = getSelectedElements().stream().map(ee->(Demand)ee).map(d->toAbsIp.apply(d)).collect(Collectors.toList());
    	    	                        	changedDemands.forEach(d->d.setCurrentOfferedTrafficInGbps(d.getCurrentOfferedTrafficInGbps() * neScalingFactor));
    	    	                    	}
    	    	                    );
    	    			}
    	    			, (a, b) -> b>0, null),
    	        		new AjtRcMenu("as normalized version", e ->
    	    			{
    	    	            DialogBuilder.launch(
    	    	                    "Normalize selected demands offered traffic", 
    	    	                    "Please introduce the total traffic, so traffic is scaled to make it sum this amount", 
    	    	                    "", 
    	    	                    this, 
    	    	                    Arrays.asList(InputForDialog.inputTfDouble("Total traffic of selected demands (Gbpd)", "Introduce the total traffic in Gbps, so selected demands will be scaled making them have an aggregated traffic equal to this value", 10, 1000.0)),
    	    	                    (list)->
    	    	                    	{
    	    	                    		final double totalTrafficExpectedGbps = (Double) list.get(0).get();
    	    	                    		final List<WAbstractIpUnicastOrAnycastDemand> changedDemands = getSelectedElements().stream().map(ee->(Demand)ee).map(d->toAbsIp.apply(d)).collect(Collectors.toList());
    	    	                    		final double totalTrafficNowGbps = changedDemands.stream().mapToDouble(ee->ee.getCurrentOfferedTrafficInGbps()).sum();
    	    	                    		final double neScalingFactor = totalTrafficExpectedGbps / totalTrafficNowGbps;
    	    	                        	changedDemands.forEach(d->d.setCurrentOfferedTrafficInGbps(d.getCurrentOfferedTrafficInGbps() * neScalingFactor));
    	    	                    	}
    	    	                    );
    	    			}
    	    			, (a, b) -> true, null)    	        		

    	        		)
    	        		));

    	        res.add(new AjtRcMenu("Set traversed VNF types to selected service chain requests (or all)", e ->
    			{
    	            DialogBuilder.launch(
    	                    "Set traversed VNF types", 
    	                    "Please introduce the comma and/or space separated list of VNF types", 
    	                    "", 
    	                    this, 
    	                    Arrays.asList(InputForDialog.inputTfString("VNF types", "Introduce the VNF types (comma or space separated)", 10, "")),
    	                    (list)->
    	                    	{
    	                    		final String vnfNamesSt = (String) list.get(0).get();
    	                    		final List<String> vnfNames = Stream.of (vnfNamesSt.split(", ")).filter(a->!a.equals("")).collect(Collectors.toList());
    	                    		final List<WServiceChainRequest> changedDemands = getSelectedElements().stream().map(ee->(Demand)ee).filter(d->isWScr.apply(d)).map(d->toWScr.apply(d)).filter(d->d.getServiceChains().isEmpty()).collect(Collectors.toList());
    	                        	changedDemands.forEach(d->d.setSequenceVnfTypes(vnfNames));
    	                    	}
    	                    );
    			}
    			, (a, b) -> true, null));

    	        res.add(new AjtRcMenu("Monitor/forecast...",  null , (a,b)->true, Arrays.asList(
    	                MonitoringUtils.getMenuAddSyntheticMonitoringInfo (this),
    	                MonitoringUtils.getMenuExportMonitoringInfo(this),
    	                MonitoringUtils.getMenuImportMonitoringInfo (this),
    	                MonitoringUtils.getMenuSetMonitoredTraffic(this),
    	                MonitoringUtils.getMenuSetOfferedTrafficAsForecasted (this),
    	                MonitoringUtils.getMenuSetTrafficPredictorAsConstantEqualToTrafficInElement (this),
    	                MonitoringUtils.getMenuPercentileFilterMonitSamples (this) , 
    	                MonitoringUtils.getMenuCreatePredictorTraffic (this),
    	                MonitoringUtils.getMenuForecastDemandTrafficUsingGravityModel (this),
    	                MonitoringUtils.getMenuForecastDemandTrafficFromLinkInfo (this),
    	                MonitoringUtils.getMenuForecastDemandTrafficFromLinkForecast(this),
    	                new AjtRcMenu("Remove traffic predictors of selected elements", e->getSelectedElements().forEach(dd->((Demand)dd).removeTrafficPredictor()) , (a,b)->b>0, null),
    	                new AjtRcMenu("Remove monitored/forecast stored information of selected elements", e->getSelectedElements().forEach(dd->((Demand)dd).getMonitoredOrForecastedOfferedTraffic().removeAllValues()) , (a,b)->b>0, null),
    	                new AjtRcMenu("Remove monitored/forecast stored information...", null , (a,b)->b>0, Arrays.asList(
    	                        MonitoringUtils.getMenuRemoveMonitorInfoBeforeAfterDate (this , true) ,
    	                        MonitoringUtils.getMenuRemoveMonitorInfoBeforeAfterDate (this , false)
    	                		))
    	        		)));

    	} // is ipLayer
    		
    	if (isWdmLayer)
    	{
    		res.add(new AjtRcMenu("Add lightpath request", e->
            {
                DialogBuilder.launch(
                        "Add lightpath request" , 
                        "Please introduce the required data", 
                        "", 
                        this, 
                        Arrays.asList(
                        		InputForDialog.inputTfString("Origin node name", "Introduce the name of the origin node. First tries case sensitive, later case insensitive", 10, ""),
                        		InputForDialog.inputTfString("Destination node name", "Introduce the name of the destination node. First tries case sensitive, later case insensitive", 10, ""),
                        		InputForDialog.inputTfDouble ("Line rate (Gbps)", "Introduce the line rate of the lightpaths realizing this request", 20, 100.0),
                        		InputForDialog.inputCheckBox("Bidirectional?", "Indicate if this lighptath request should be bidirectional, so two unidirectional opposite requests are created", true , null),
                        		InputForDialog.inputCheckBox("1+1 protected?", "Indicate if this lighptath request has to be realized by a 1+1 arrangement of two lightpaths", false , null)
                        		),
                        (list)->
                        	{
                        		final String aName  = (String) list.get(0).get();
                        		final String bName  = (String) list.get(1).get();
                        		final Double lineRateGbps = (Double) list.get(2).get();
                        		final Boolean bidirectional = (Boolean) list.get(3).get();
                        		final Boolean isToBe11Protected = (Boolean) list.get(4).get();
                        		final WNode a = nodeByName.apply(aName).orElse(null);
                        		final WNode b = nodeByName.apply(bName).orElse(null);
                        		if (a == null || b == null) throw new Net2PlanException("Unkown node name. " + (a == null? aName : bName));
                        		final WLightpathRequest lprAb = wNet.addLightpathRequest(a, b, lineRateGbps, isToBe11Protected);
                        		if (bidirectional)
                        		{
                            		final WLightpathRequest lprBa = wNet.addLightpathRequest(b,a, lineRateGbps, isToBe11Protected);
                        			lprAb.setBidirectionalPair(lprBa);
                        		}
                        	}
                        );
            } , (a,b)->true, null));
            res.add(new AjtRcMenu("Add lightpath to selected requests", null , (a, b)->true, Arrays.asList( 
            		new AjtRcMenu("as shortest path, first-fit", e->
            		{
                        DialogBuilder.launch(
                                "Add lightpath to selected requests" , 
                                "Please introduce the required data", 
                                "", 
                                this, 
                                Arrays.asList(
                                		InputForDialog.inputCheckBox("Shortest path in optical latency?", "If checked, the shortest path is computed considering optical latency as link cost, if not, the shortest patrh minimizes the number of traversed fibers", true, null),
                                		InputForDialog.inputTfInt("Number of optical slots (" + df.format(WNetConstants.OPTICALSLOTSIZE_GHZ) + " GHz each)", "Introduce the number of optical slots to reserve for each lightpath", 10, 4)
                                		),
                                (list)->
                                	{
                                		final OpticalSpectrumManager osm = callback.getNiwInfo().getThird();
                                		assert callback.getNiwInfo().getSecond().getNe().equals(callback.getDesign());
                                		final Boolean spLatency = (Boolean) list.get(0).get();
                                		final Integer numContiguousSlots = (Integer) list.get(1).get();
                            			final List<WLightpathRequest> ds = getSelectedElements().stream().filter(ee->wNet.getWType(ee).equals(Optional.of(WTYPE.WLightpathRequest))).map(ee->wNet.getWElement(ee).get().getAsLightpathRequest()).collect(Collectors.toList());
                            			final Map<WFiber , Double> costMap = new HashMap<> ();
                            			for (WFiber ee : wNet.getFibers()) costMap.put(ee, spLatency? ee.getPropagationDelayInMs() : 1.0);
                            			for (WLightpathRequest dd : ds)
                            			{
                            				if (!dd.getLightpaths().isEmpty()) continue;
                            				if (dd.isToBe11Protected())
                            				{
                                				final List<List<WFiber>> sps = wNet.getTwoMaximallyLinkAndNodeDisjointWdmPaths(dd.getA(), dd.getB(), Optional.of(costMap));
                                				if (sps.isEmpty()) continue;
                                				final List<WFiber> seqFibersAbMain = sps.get(0); 
                                				final List<WFiber> seqFibersAbBackup = sps.size() == 1? null : sps.get(1); 
                                				final boolean twoRoutes = seqFibersAbBackup != null;
                                				boolean isBidirectional = dd.isBidirectional() && seqFibersAbMain.stream().allMatch(ee->ee.isBidirectional());
                                				if (twoRoutes) isBidirectional &= seqFibersAbBackup.stream().allMatch(ee->ee.isBidirectional()); 
                            					if (isBidirectional)
                            					{ 
                            						if (!dd.getBidirectionalPair().getLightpaths().isEmpty()) continue;
                                    				final List<WFiber> seqFibersBaMain = Lists.reverse(seqFibersAbMain.stream().map(ee->ee.getBidirectionalPair()).collect(Collectors.toList()));
                                    				final List<WFiber> seqFibersBaBackup = seqFibersAbBackup == null? null : Lists.reverse(seqFibersAbBackup.stream().map(ee->ee.getBidirectionalPair()).collect(Collectors.toList()));
                                    				final List<WFiber> seqFibersAbbAMain = new ArrayList<> (seqFibersAbMain); seqFibersAbbAMain.addAll(seqFibersBaMain);
                                    				final List<WFiber> seqFibersAbbABackup = seqFibersAbBackup == null? null : new ArrayList<> (seqFibersAbBackup); seqFibersAbbABackup.addAll(seqFibersBaBackup);
                                    				if (twoRoutes)
                                    				{ // 1+1 bidirectional 2 routes
                                    					final Optional<Pair<SortedSet<Integer>,SortedSet<Integer>>> slotsTwoRoutes = osm.spectrumAssignment_firstFitTwoRoutes(seqFibersAbbAMain, seqFibersAbbABackup, numContiguousSlots); 
                                        				if (!slotsTwoRoutes.isPresent()) continue;
                                        				final WLightpath lpAbMain = dd.addLightpathUnregenerated(seqFibersAbMain, slotsTwoRoutes.get().getFirst(), false);
                                        				final WLightpath lpBaMain = dd.getBidirectionalPair().addLightpathUnregenerated(seqFibersBaMain, slotsTwoRoutes.get().getFirst(), false);
                                        				osm.allocateOccupation(lpAbMain, seqFibersAbMain, slotsTwoRoutes.get().getFirst());
                                        				osm.allocateOccupation(lpBaMain, seqFibersBaMain, slotsTwoRoutes.get().getFirst());
                                        				final WLightpath lpAbBackup = dd.addLightpathUnregenerated(seqFibersAbBackup, slotsTwoRoutes.get().getSecond(), true);
                                        				final WLightpath lpBaBackup = dd.getBidirectionalPair().addLightpathUnregenerated(seqFibersBaBackup, slotsTwoRoutes.get().getSecond(), true);
                                        				osm.allocateOccupation(lpAbBackup, seqFibersAbBackup, slotsTwoRoutes.get().getSecond());
                                        				osm.allocateOccupation(lpBaBackup, seqFibersBaBackup, slotsTwoRoutes.get().getSecond());
                                    				}
                                    				else
                                    				{ // 1+1 bidirectional , but one route
                                    					final Optional<SortedSet<Integer>> slots = osm.spectrumAssignment_firstFit(seqFibersAbbAMain, numContiguousSlots , Optional.empty()); 
                                        				if (!slots.isPresent()) continue;
                                        				final WLightpath lpAbMain = dd.addLightpathUnregenerated(seqFibersAbMain, slots.get(), false);
                                        				final WLightpath lpBaMain = dd.getBidirectionalPair().addLightpathUnregenerated(seqFibersBaMain, slots.get(), false);
                                        				osm.allocateOccupation(lpAbMain, seqFibersAbMain, slots.get());
                                        				osm.allocateOccupation(lpBaMain, seqFibersBaMain, slots.get());
                                    				}
                            					}
                            					else
                            					{
                            						// 1+1 not bidirectional  
                                    				if (twoRoutes)
                                    				{ // 1+1 not bidirectional, two routes
                                    					final Optional<Pair<SortedSet<Integer>,SortedSet<Integer>>> slotsTwoRoutes = osm.spectrumAssignment_firstFitTwoRoutes(seqFibersAbMain, seqFibersAbBackup, numContiguousSlots); 
                                        				if (!slotsTwoRoutes.isPresent()) continue;
                                        				final WLightpath lpAbMain = dd.addLightpathUnregenerated(seqFibersAbMain, slotsTwoRoutes.get().getFirst(), false);
                                        				osm.allocateOccupation(lpAbMain, seqFibersAbMain, slotsTwoRoutes.get().getFirst());
                                        				final WLightpath lpAbBackup = dd.addLightpathUnregenerated(seqFibersAbBackup, slotsTwoRoutes.get().getSecond(), true);
                                        				osm.allocateOccupation(lpAbBackup, seqFibersAbBackup, slotsTwoRoutes.get().getSecond());
                                    				}
                                    				else
                                    				{// 1+1 not bidirectional, one route
                                    					final Optional<SortedSet<Integer>> slots = osm.spectrumAssignment_firstFit(seqFibersAbMain, numContiguousSlots , Optional.empty()); 
                                        				if (!slots.isPresent()) continue;
                                        				final WLightpath lpAbMain = dd.addLightpathUnregenerated(seqFibersAbMain, slots.get(), false);
                                        				osm.allocateOccupation(lpAbMain, seqFibersAbMain, slots.get());
                                    				}
                            					}
                            				}
                            				else
                            				{
                                				final List<List<WFiber>> sps = wNet.getKShortestWdmPath(1, dd.getA(), dd.getB(), Optional.of(costMap));
                                				if (sps.size() != 1) continue;
                                				final List<WFiber> seqFibersAb = sps.get(0); 
                            					if (dd.isBidirectional() && seqFibersAb.stream().allMatch(ee->ee.isBidirectional()))
                            					{
                            						// not protected, bidirectional
                            						if (!dd.getBidirectionalPair().getLightpaths().isEmpty()) continue;
                                    				final List<WFiber> seqFibersBa = Lists.reverse(seqFibersAb.stream().map(ee->ee.getBidirectionalPair()).collect(Collectors.toList()));
                                    				final List<WFiber> seqFibersAbbA = new ArrayList<> (seqFibersAb); seqFibersAbbA.addAll(seqFibersBa);
                                    				final Optional<SortedSet<Integer>> slots = osm.spectrumAssignment_firstFit(seqFibersAbbA, numContiguousSlots, Optional.empty());
                                    				if (!slots.isPresent()) continue;
                                    				final WLightpath lpAb = dd.addLightpathUnregenerated(seqFibersAb, slots.get(), false);
                                    				final WLightpath lpBa = dd.getBidirectionalPair().addLightpathUnregenerated(seqFibersBa, slots.get(), false);
                                    				osm.allocateOccupation(lpAb, seqFibersAb, slots.get());
                                    				osm.allocateOccupation(lpBa, seqFibersBa, slots.get());
                            					}
                            					else
                            					{ // not protected, unidirectional
                                    				final Optional<SortedSet<Integer>> slots = osm.spectrumAssignment_firstFit(seqFibersAb, numContiguousSlots, Optional.empty());
                                    				if (!slots.isPresent()) continue;
                                    				final WLightpath lp = dd.addLightpathUnregenerated(seqFibersAb, slots.get(), false);
                                    				osm.allocateOccupation(lp, seqFibersAb, slots.get());
                            					}
                            				}
                            			}
                                	}
                                );
            		} , (a, b)->true, null))));

    		res.add(new AjtRcMenu("Set line rate to selected requests", e->
            {
                DialogBuilder.launch(
                        "Set line rate to selected requests" , 
                        "Please introduce the required data", 
                        "", 
                        this, 
                        Arrays.asList(
                        		InputForDialog.inputTfDouble("Line rate (Gbps)", "Introduce the line rate to set to the selected lighpath requests", 10, 100.0)
                        		),
                        (list)->
                        	{
                        		final Double val  = (Double) list.get(0).get();
                        		getSelectedElements().forEach(dd->toLpr.apply(dd).setLineRateGbps(val));
                        	}
                        );
            } , (a,b)->b>0, null));
    		
        	res.add(new AjtRcMenu("Set protection type of selected lightpath requests as", null , (a,b)->b>0, Arrays.asList(
            		new AjtRcMenu("Not 1+1 protection", e-> getSelectedElements().forEach(dd->toLpr.apply(dd).setIsToBe11Protected(false)), (a,b)->b>0, null),
            		new AjtRcMenu("1+1 protection", e->getSelectedElements().forEach(dd->toLpr.apply(dd).setIsToBe11Protected(true)), (a,b)->b>0, null)
            		)));

            res.add(new AjtRcMenu("Set transponder name to selected requests", e->
            {
                DialogBuilder.launch(
                        "Set transponder name to selected requests" , 
                        "Please introduce the required data", 
                        "", 
                        this, 
                        Arrays.asList(
                        		InputForDialog.inputTfString("Transponder name", "Introduce the name of the transponder", 10, "")
                        		),
                        (list)->
                        	{
                        		final String name  = (String) list.get(0).get();
                        		getSelectedElements().forEach(dd->toLpr.apply(dd).setTransponderName(name));
                        	}
                        );
            } , (a,b)->b>0, null));

    	} // is wdm layer




        
        
        return res;
    }

    
    private void rcMenuFullMeshTraffic(boolean isHopByHop)
    {
    	final WNet wNet = callback.getNiwInfo().getSecond(); 
        final Collection<WNode> nodes;
        nodes = (callback.getSelectedElements(AJTableType.NODES , getTableNetworkLayer()).isEmpty()? wNet.getNodes() : callback.getSelectedElements(AJTableType.NODES , getTableNetworkLayer()).stream().map(n->new WNode((Node) n)).filter(n->n.isRegularNode()).collect(Collectors.toList()));
        if (nodes.isEmpty()) throw new Net2PlanException("There are no nodes");
        for (WNode n1 : nodes)
            for (WNode n2 : nodes)
            	if (n1.getId () > n2.getId())
            	{
                	wNet.addIpUnicastDemand(n1, n2, true, isHopByHop);
                	wNet.addIpUnicastDemand(n2, n1, false, isHopByHop);
            		
            	}
        callback.getPickManager().reset();
    }

    public static <TT extends NetworkElement>  List<AjtColumnInfo<IMonitorizableElement>> getMonitoringAndTrafficEstimationColumns (AdvancedJTable_networkElement<TT> table)
    {
    	final List<AjtColumnInfo<IMonitorizableElement>> res = new LinkedList<> ();
        res.add(new AjtColumnInfo<IMonitorizableElement>(table , Integer.class, null , "#Monit points" , "Number of samples of the offered traffic stored, coming from a monitoring or forecasting traffic process", null , d->d.getMonitoredOrForecastedCarriedTraffic().getSize() , AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<IMonitorizableElement>(table , Date.class, null , "First date", "The date of the earliest monitored sample available", null , d->d.getMonitoredOrForecastedCarriedTraffic().getFirstDate() , AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<IMonitorizableElement>(table , Date.class, null , "Last date", "The date of the more recent monitored sample available", null , d->d.getMonitoredOrForecastedCarriedTraffic().getLastDate() , AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<IMonitorizableElement>(table , String.class, null , "Forecast (Gbps)", "Indicates the forecasted traffic in the target date, according to current predictor", null , d->d.getTrafficPredictor().isPresent()? d.getTrafficPredictor().get().getPredictorFunctionNoConfidenceInterval ().apply(table.getTableNetworkLayer().getNetPlan().getCurrentDate()) : "--" , AGTYPE.SUMDOUBLE , null));
        res.add(new AjtColumnInfo<IMonitorizableElement>(table , String.class, null , "Abs. mismatch", "Indicates the absolute value in traffic units of the mismatch between the forecasted value and the current value in this element", null , d-> { final Double f = d.getTrafficPredictor().isPresent()? d.getTrafficPredictor().get().getPredictorFunctionNoConfidenceInterval ().apply(table.getTableNetworkLayer().getNetPlan().getCurrentDate()) : null; if (f == null) return "--"; final double v = d.getCurrentTrafficToAddMonitSample(); return Math.abs(f-v) < Configuration.precisionFactor? 0 : Math.abs(f-v);   }  , AGTYPE.SUMDOUBLE , null));
        res.add(new AjtColumnInfo<IMonitorizableElement>(table , String.class, null , "Forecast type", "Indicates the type of forecast information applied, if any", null , d->d.getTrafficPredictor().isPresent()? d.getTrafficPredictor().get().getTpType ().getName () : "None" , AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<IMonitorizableElement>(table , Double.class, null , "Variance explained", "Indicates the fraction of the variance explained by the predictor (1 means perfectly accurate predictor, 0 means as good as picking the average)", null , d->d.getTrafficPredictor().isPresent()? (d.getTrafficPredictor().get().getStatistics() == null? "--" : d.getTrafficPredictor().get().getStatistics().getRsquared()) : "--" , AGTYPE.NOAGGREGATION , null));
    	return res;
    }

    
}
