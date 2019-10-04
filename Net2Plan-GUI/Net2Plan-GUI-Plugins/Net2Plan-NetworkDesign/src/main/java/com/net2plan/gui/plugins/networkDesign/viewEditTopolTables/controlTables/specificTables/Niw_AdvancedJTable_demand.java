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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.DialogBuilder;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.InputForDialog;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.monitoring.MonitoringUtils;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.PickManager;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.StringLabeller;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IMonitorizableElement;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.niw.networkModel.WAbstractIpUnicastOrAnycastDemand;
import com.net2plan.niw.networkModel.WIpUnicastDemand;
import com.net2plan.niw.networkModel.WLightpathRequest;
import com.net2plan.niw.networkModel.WNet;
import com.net2plan.niw.networkModel.WNetConstants;
import com.net2plan.niw.networkModel.WNode;
import com.net2plan.niw.networkModel.WServiceChainRequest;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;

import net.miginfocom.swing.MigLayout;

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

    	final WNet wNet = new WNet (callback.getDesign()); 
    	final boolean isIpLayer = getTableNetworkLayer().equals(wNet.getIpLayer().getNe());
    	final boolean isWdmLayer = getTableNetworkLayer().equals(wNet.getWdmLayer().getNe());
    	assert isIpLayer || isWdmLayer;
    	assert !(isIpLayer && isWdmLayer);
    	if (isIpLayer)
    	{
        	final SortedMap<Link,SortedMap<String,Pair<Double,Double>>> perLink_qos2occupationAndViolationMap = callback.getDesign().getAllLinksPerQosOccupationAndQosViolationMap(layerThisTable);
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
        			return toWIpUnicast.apply(d).getOriginNode().getNe ();
        		else
        			return toWScr.apply(d).getPotentiallyValidOrigins().stream().map(e->e.getNe ()).collect (Collectors.toList());
        	} , AGTYPE.NOAGGREGATION , null) );
        	res.add(new AjtColumnInfo<Demand>(this , Node.class, null , "E", "Egress router/s", null , d->
        	{  
        		if (d.isCoupled ()) throw new RuntimeException ("The IP demand in NIW is coupled. These demands should be filtered out");
        		if (isWIpUnicast.apply(d))
        			return toWIpUnicast.apply(d).getDestinationNode().getNe ();
        		else
        			return toWScr.apply(d).getPotentiallyValidDestinations().stream().map(e->e.getNe ()).collect (Collectors.toList());
        	} , AGTYPE.NOAGGREGATION , null) );
            res.add(new AjtColumnInfo<Demand>(this , Boolean.class, null , "Anycast SCR?", "Indicates if this demand is an anycast service chain request", null , d->!wNet.getWElement(d).get().isWIpUnicastDemand() , AGTYPE.COUNTTRUE , null));
            res.add(new AjtColumnInfo<Demand>(this , Boolean.class, null , "Type", "Indicates if this demand is a IP unicast demand, or an servichain request i.e. potentially anycast, potentially traversing VNFs, and carried by service chains", null , d->isWIpUnicast.apply(d)? "IP unicast demand" : "Service chain request" , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Demand>(this , String.class, null , "Routing type", "For unicast IP demands, indicates if its routing type is HOP-BY-HOP routing, or connection-based source routing. Service chain request are always of the source routing type", null, d->isWIpUnicast.apply(d)? (toWIpUnicast.apply(d).isIpHopByHopRouted()? "Hop-by-hop" : "Source routing") : "Source routing" , AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Demand>(this , Boolean.class, null , "US/DS", "Indicates if this demand is upstream or downstream", null , d->toAbsIp.apply(d).isDownstream()? "Downstream" : "Upstream" , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Demand>(this , Demand.class, null , "Bidirectional pair", "If the demand is bidirectional, provides its bidirectional pair", null , d->toAbsIp.apply(d).getBidirectionalPair().orElse(null) , AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Offered traffic (Gbps)", "Currently offered traffic by the demand in Gbps", (d,val)->toAbsIp.apply(d).setCurrentOfferedTrafficInGbps((Double) val), d->toAbsIp.apply(d).getCurrentOfferedTrafficInGbps() , AGTYPE.SUMDOUBLE , null));
            res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Carried traffic (Gbps)", "Currently carried traffic by the demand in Gbps", null , d->toAbsIp.apply(d).getCurrentCarriedTrafficGbps() , AGTYPE.SUMDOUBLE , null));
            res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "% Lost traffic", "Percentage of the lost traffic by the demand", null, d->toAbsIp.apply(d).getCurrentOfferedTrafficInGbps() == 0? 0 : toAbsIp.apply(d).getCurrentBlockedTraffic() / toAbsIp.apply(d).getCurrentOfferedTrafficInGbps() , AGTYPE.NOAGGREGATION , d->d.getBlockedTraffic() > Configuration.precisionFactor? Color.RED : Color.GREEN));
            res.add(new AjtColumnInfo<Demand>(this , String.class, null , "QoS type", "A used-defined string identifying the QoS type of traffic of the demand. QoS differentiation in the IP links is possible for each QoS type", (d,val)-> toAbsIp.apply(d).setQosType((String)val) , d->toAbsIp.apply(d).getQosType(), AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Demand>(this , String.class, null , "WC Oversubscription", "The worst case, among all the traversed links, of the amount of traffic of this demand that is oversubscribed", null , d->d.getTraversedLinksAndCarriedTraffic(false).keySet().stream().mapToDouble (e -> perLink_qos2occupationAndViolationMap.get(e).get(d.getQosType()).getSecond()).max().orElse(0.0), AGTYPE.NOAGGREGATION , d-> d.getTraversedLinksAndCarriedTraffic(false).keySet().stream().mapToDouble (e -> perLink_qos2occupationAndViolationMap.get(e).get(d.getQosType()).getSecond()).max().orElse(0.0) > Configuration.precisionFactor? Color.red : Color.green));
            res.add(new AjtColumnInfo<Demand>(this , String.class, null , "Bifurcated?", "Indicates whether the demand is satisfied by more than one path from origin to destination", null, d->!d.isSourceRouting() ? "-" : (d.isBifurcated()) ? String.format("Yes (%d)", d.getRoutes().size()) : "No" , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Worst e2e lat (ms)", "Current worst case end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any)", null, d->toAbsIp.apply(d).getWorstCaseEndtoEndLatencyMs() , AGTYPE.NOAGGREGATION , d->{ if (isWScr.apply(d)) return null; final double maxMs = toWIpUnicast.apply(d).getMaximumAcceptableE2EWorstCaseLatencyInMs(); return maxMs <= 0? null : (toWIpUnicast.apply(d).getWorstCaseEndtoEndLatencyMs() > maxMs? Color.RED : null); }));
            res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Worst e2e length (km)", "Current worst case end-to-end propagation length in km (accumulating any lower layer propagation lengths if any)", null, d->d.getWorstCaseLengthInKm() , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Limit e2e lat (ms)", "Maximum end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any)", (d,val)-> { if(isWIpUnicast.apply (d)) d.setMaximumAcceptableE2EWorstCaseLatencyInMs((Double)val); } , d-> isWIpUnicast.apply (d)? d.getMaximumAcceptableE2EWorstCaseLatencyInMs() : "--" , AGTYPE.NOAGGREGATION , null));
            res.add(new AjtColumnInfo<Demand>(this , String.class, null , "SC: VNFs types to traverse", "For service chain requests, the sequence of VNF types that has to be traversed by the service chains", null, d->isWIpUnicast.apply(d)? d.getServiceChainSequenceOfTraversedResourceTypes().stream().collect(Collectors.joining(",")) : "" , AGTYPE.COUNTTRUE , null));
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
    	      res.add(new AjtColumnInfo<Demand>(this , Boolean.class, null , "1+1?", "Indicates if this lightpath request is 1+1 protected", null, d->new WLightpathRequest(d).is11Protected() , AGTYPE.COUNTTRUE, null));
    	      res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Up?", "Indicates the lightpath request is up, meaning that at least one lightpath realizing it is up", null, d->!(new WLightpathRequest(d).isBlocked()) , AGTYPE.COUNTTRUE , d->(new WLightpathRequest(d).isBlocked())? Color.RED : Color.GREEN));
    	      res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Tp name", "Name of the transponder realizing the lightpaths of this lightpath request", (d,val)-> new WLightpathRequest(d).setTransponderName((String) val) , d->new WLightpathRequest(d).getTransponderName() , AGTYPE.NOAGGREGATION , null));
    	      res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Worst e2e lat (ms)", "Current worst case end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any)", null, d->d.getWorstCasePropagationTimeInMs() , AGTYPE.NOAGGREGATION , d->{ final double maxMs = d.getMaximumAcceptableE2EWorstCaseLatencyInMs(); return maxMs <= 0? null : (d.getWorstCasePropagationTimeInMs() > maxMs? Color.RED : null); }));
    	      res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Worst e2e length (km)", "Current worst case end-to-end propagation length in km (accumulating any lower layer propagation lengths if any)", null, d->d.getWorstCaseLengthInKm() , AGTYPE.NOAGGREGATION , null));
    	}
      return res;
  }

    @Override
    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
    	final NetPlan np = callback.getDesign();
        final List<AjtRcMenu> res = new ArrayList<> ();
    	final WNet wNet = new WNet (callback.getDesign()); 
    	final Function<Demand,Boolean> isWScr = d ->wNet.getWElement(d).get().isServiceChainRequest();
    	final Function<Demand,Boolean> isWIpUnicast = d ->wNet.getWElement(d).get().isWIpUnicastDemand();
    	final Function<Demand,Boolean> isWAbstractIpD = d ->isWScr.apply(d) || isWIpUnicast.apply(d); 
    	final Function<Demand,WIpUnicastDemand> toWIpUnicast = d -> (WIpUnicastDemand) wNet.getWElement(d).get();
    	final Function<Demand,WServiceChainRequest> toWScr = d ->(WServiceChainRequest) wNet.getWElement(d).get();
    	final Function<Demand,WAbstractIpUnicastOrAnycastDemand> toAbsIp = d ->(WAbstractIpUnicastOrAnycastDemand) wNet.getWElement(d).get();
    	final boolean isIpLayer = getTableNetworkLayer().equals(wNet.getIpLayer().getNe());
    	final boolean isWdmLayer = getTableNetworkLayer().equals(wNet.getWdmLayer().getNe());
    	assert isIpLayer || isWdmLayer;
    	assert !(isIpLayer && isWdmLayer);
		final Function<String,Optional<WNode>> nodeByName = st -> 
		{
    		WNode a = wNet.getNodeByName(st).orElse(null);
    		if (a == null) a = wNet.getNodes().stream().filter(n->n.getName().equalsIgnoreCase(st)).findFirst().orElse(null);
    		return Optional.ofNullable(a);
		};
    	
    	if (isIpLayer) res.add (new AjtRcMenu("Add IP Unicast demand", e->
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
    	
    	if (isIpLayer) res.add(new AjtRcMenu("Add anycast service chain request", e->
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

    	if (isWdmLayer) res.add(new AjtRcMenu("Add lightpath request", e->
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
                    		InputForDialog.inputCheckBox("1+1 protected?", "Indicate if this lighptath request has to be realized by a 1+1 arrangement of two lightpaths", false , null)
                    		),
                    (list)->
                    	{
                    		final String aName  = (String) list.get(0).get();
                    		final String bName  = (String) list.get(1).get();
                    		final double lineRateGbps = (double) list.get(2).get();
                    		final Boolean isToBe11Protected = (Boolean) list.get(3).get();
                    		final WNode a = nodeByName.apply(aName).orElse(null);
                    		final WNode b = nodeByName.apply(bName).orElse(null);
                    		if (a == null || b == null) throw new Net2PlanException("Unkown node name. " + (a == null? aName : bName));
                    		wNet.addLightpathRequest(a, b, lineRateGbps, isToBe11Protected);
                    	}
                    );
        } , (a,b)->b>0, null));         		

        res.add(new AjtRcMenu("Remove selected demands", e->getSelectedElements().forEach(dd->toAbsIp.apply (dd).remove ()) , (a,b)->b>0, null));

        res.add(new AjtRcMenu("Arrange selected IP unicast demands in bidirectional pairs", e->
        {
        	final SortedSet<WIpUnicastDemand> nonBidiDemands = getSelectedElements().stream().filter(ee->isWIpUnicast.apply(ee)).map(ee->toWIpUnicast.apply(ee)).filter(ee->!ee.isBidirectional()).collect(Collectors.toCollection(TreeSet::new));
        	final Map<Pair<WNode,WNode> , WIpUnicastDemand> nodePair2demand = new HashMap<>();
        	for (WIpUnicastDemand ee : nonBidiDemands)
        	{
        		final Pair<WNode,WNode> pair = Pair.of(ee.getOriginNode() , ee.getDestinationNode());
        		if (nodePair2demand.containsKey(pair)) throw new Net2PlanException ("At most one link per node pair is allowed");
        		nodePair2demand.put(pair, ee);
        	}
        	for (WIpUnicastDemand ee : nonBidiDemands)
        	{
        		if (ee.isBidirectional()) continue;
        		final WIpUnicastDemand opposite = nodePair2demand.get(Pair.of(ee.getDestinationNode(), ee.getOriginNode()));
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
        
        res.add(new AjtRcMenu("Add one IP unicast demand per selected node pair (all if none selected)", null, (a,b)->true, Arrays.asList(
        		new AjtRcMenu("as hop-by-hop routing (e.g. for OSPF routing)", e->rcMenuFullMeshTraffic(true), (a,b)->true, null),
        		new AjtRcMenu("as source-routing (e.g. for MPLS-TE routing)", e->rcMenuFullMeshTraffic(false), (a,b)->true, null)
        		)
        		));
        res.add(new AjtRcMenu("Set selected demands offered traffic", e ->
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
		, (a, b) -> b>0, null));
        res.add(new AjtRcMenu("Set selected demands offered traffic randomly", e ->
		{
			final Random rng = new Random ();
    		final List<WAbstractIpUnicastOrAnycastDemand> changedDemands = getSelectedElements().stream().map(ee->(Demand)ee).map(d->toAbsIp.apply(d)).collect(Collectors.toList());
        	changedDemands.forEach(d->d.setCurrentOfferedTrafficInGbps(rng.nextDouble()));
		}
		, (a, b) -> b>0, null));
        res.add(new AjtRcMenu("Scale selected demands offered traffic", e ->
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
		, (a, b) -> b>0, null));
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

        return res;
    }

    
    private void rcMenuFullMeshTraffic(boolean isHopByHop)
    {
        final WNet wNet = new WNet (callback.getDesign());
        final Collection<WNode> nodes;
        nodes = (callback.getSelectedElements(AJTableType.NODES , getTableNetworkLayer()).isEmpty()? wNet.getNodes() : (Set<WNode>) callback.getSelectedElements(AJTableType.NODES , getTableNetworkLayer()).stream().map(n->new WNode((Node) n)).filter(n->n.isRegularNode()).collect(Collectors.toList()));
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
