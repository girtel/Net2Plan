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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
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
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.niw.OpticalSimulationModule;
import com.net2plan.niw.OpticalSimulationModule.PERLPINFOMETRICS;
import com.net2plan.niw.OpticalSpectrumManager;
import com.net2plan.niw.WIpSourceRoutedConnection;
import com.net2plan.niw.WLightpath;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNetConstants;
import com.net2plan.niw.WNode;
import com.net2plan.niw.WServiceChain;
import com.net2plan.niw.WVnfInstance;

/**
 */
@SuppressWarnings({ "unchecked", "serial" })
public class Niw_AdvancedJTable_resource extends AdvancedJTable_networkElement<Resource>
{
	private static DecimalFormat df = new DecimalFormat("#.##");
	
    public Niw_AdvancedJTable_resource(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.RESOURCES , "VNF instances" , layerThisTable , true , null);
    }

    @Override
  public List<AjtColumnInfo<Resource>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
	final List<AjtColumnInfo<Resource>> res = new LinkedList<> ();
	assert callback.getNiwInfo().getFirst();
	final WNet wNet = callback.getNiwInfo().getSecond(); 
	final Function<Resource,WVnfInstance> toVnf = d -> (WVnfInstance) wNet.getWElement(d).get();
	final Function<Route,Boolean> isVnf = d -> wNet.getWElement(d).get().isVnfInstance();
	
	  res.add(new AjtColumnInfo<Resource>(this , String.class, null , "Name", "VNF instance name", (d,val)->toVnf.apply(d).setName((String) val), d->toVnf.apply(d).getName() , AGTYPE.NOAGGREGATION , null));
	  res.add(new AjtColumnInfo<Resource>(this , String.class, null , "Type", "VNF type", null , d->toVnf.apply(d).getType() , AGTYPE.NOAGGREGATION , null));
	  res.add(new AjtColumnInfo<Resource>(this , Boolean.class, null , "Up?", "If the VNF is up or down (just the state of its hosting node)", null , d->toVnf.apply(d).getHostingNode().isUp() , AGTYPE.COUNTTRUE , d->toVnf.apply(d).getHostingNode().isUp()? null : Color.red));
	  res.add(new AjtColumnInfo<Resource>(this , Node.class, null , "Host node", "The node hosting this VNF", null , d->toVnf.apply(d).getHostingNode().getNe() , AGTYPE.NOAGGREGATION , null));
	  res.add(new AjtColumnInfo<Resource>(this , Double.class, null , "Capacity (Gbps)", "The VNF current capacity in Gbps", (d,val)-> toVnf.apply(d).setCapacityInGbpsOfInputTraffic(Optional.of((Double) val) , Optional.empty(), Optional.empty(), Optional.empty()) , d->toVnf.apply(d).getCurrentCapacityInGbps() , AGTYPE.NOAGGREGATION , null));
	  res.add(new AjtColumnInfo<Resource>(this , Double.class, null , "Occupied capacity (Gbps)", "The current occupied capacity of the VNF in Gbps", null , d->toVnf.apply(d).getOccupiedCapacityInGbps() , AGTYPE.NOAGGREGATION , null));
	  res.add(new AjtColumnInfo<Resource>(this , Double.class, null , "Utilization", "The current utilization of the VNF (occupied capacity vs. capacity)", null , d->toVnf.apply(d).getCurrentUtilization() , AGTYPE.MAXDOUBLE , d-> { final double u = toVnf.apply(d).getCurrentUtilization(); if (u == 1) return Color.YELLOW; return u > 1? Color.RED : null;  }  ));
	  res.add(new AjtColumnInfo<Resource>(this , Double.class, null , "Assigned CPUs", "The number of assigned CPUs to this VNF", (d,val)-> toVnf.apply(d).setCapacityInGbpsOfInputTraffic(Optional.empty(), Optional.of((Double) val), Optional.empty(), Optional.empty()), d->toVnf.apply(d).getOccupiedCpus() , AGTYPE.SUMDOUBLE , null ));
	  res.add(new AjtColumnInfo<Resource>(this , Double.class, null , "Assigned RAM (GB)", "The amount of assigned RAM to this VNF, in GB", (d,val)-> toVnf.apply(d).setCapacityInGbpsOfInputTraffic(Optional.empty(), Optional.empty(), Optional.of((Double) val), Optional.empty()), d->toVnf.apply(d).getOccupiedRamInGB() , AGTYPE.SUMDOUBLE , null ));
	  res.add(new AjtColumnInfo<Resource>(this , Double.class, null , "Assigned HD (TB)", "The amount of assigned HD to this VNF, in TB", (d,val)-> toVnf.apply(d).setCapacityInGbpsOfInputTraffic(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(1000 * (Double) val)), d->toVnf.apply(d).getOccupiedHdInGB()/1000.0 , AGTYPE.SUMDOUBLE , null ));
	  res.add(new AjtColumnInfo<Resource>(this , Collection.class, null , "Trav. SCs", "The traversing IP service chains", null , d->toVnf.apply(d).getTraversingServiceChains().stream().map(e->e.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
	  res.add(new AjtColumnInfo<Resource>(this , Double.class, null , "Processing time (ms)", "The processing time of the VNF, added as delay to all the traffic units traversing it", (d,val)->toVnf.apply(d).setProcessingTimeInMs((Double) val) , d->toVnf.apply(d).getProcessingTimeInMs(), AGTYPE.MAXDOUBLE , null));
	
	  return res;
  	}

    @Override
    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
    	final NetPlan np = callback.getDesign();
        final List<AjtRcMenu> res = new ArrayList<> ();
    	assert callback.getNiwInfo().getFirst();
    	final WNet wNet = callback.getNiwInfo().getSecond(); 
    	final Function<Resource,WVnfInstance> toVnf = d -> (WVnfInstance) wNet.getWElement(d).get();
    	final Function<Route,Boolean> isVnf = d -> wNet.getWElement(d).get().isVnfInstance();
		final Function<String,Optional<WNode>> nodeByName = st -> 
		{
    		WNode a = wNet.getNodeByName(st).orElse(null);
    		if (a == null) a = wNet.getNodes().stream().filter(n->n.getName().equalsIgnoreCase(st)).findFirst().orElse(null);
    		return Optional.ofNullable(a);
		};

		
        res.add(new AjtRcMenu("Add VNF instance", e->
    	DialogBuilder.launch(
                "Add VNF instance" , 
                "Please introduce the requested information", 
                "", 
                this, 
                Arrays.asList(
                		InputForDialog.inputTfString("VNF host node name", "The name of the node where the VNF will be hosted", 10, ""),
                		InputForDialog.inputTfString("VNF instance type", "The type of the VNF instance", 10, ""),
                		InputForDialog.inputTfString("VNF instance name", "The name of VNF instance", 10, ""),
                		InputForDialog.inputTfDouble("Capacity (Gbps)", "The capacity of this instance in terms of Gbps that is able to process", 10, 100.0),
                		InputForDialog.inputTfDouble("Allocated CPU", "The amount of CPU allocated to this VNF instance", 10, 100.0),
                		InputForDialog.inputTfDouble("Allocated RAM (GB)", "The amount of RAM allocated to this VNF instance in GigaBytes", 10, 100.0),
                		InputForDialog.inputTfDouble("Allocated HD (TB)", "The amount of storage allocated to this VNF instance in TeraBytes", 10, 1.0),
                		InputForDialog.inputTfDouble("Processing time (ms)", "The processing time in ms, added as delay to the traversing traffic", 10, 1.0)
                		),
                (list)->
                	{
                		final String node = (String) list.get(0).get();
                		final String type = (String) list.get(1).get();
                		final String name = (String) list.get(2).get();
                		final double capacityGbps = (Double) list.get(3).get();
                		final double occupiedCpu = (Double) list.get(4).get();
                		final double occupiedRamGB = (Double) list.get(5).get();
                		final double occupiedHdGb = (Double) list.get(6).get();
                		final double processingTimeMs = (Double) list.get(7).get();
                		final WNode hostNode = nodeByName.apply(node).orElse(null);
                		if (hostNode == null) throw new Net2PlanException("Unkown node name. " + node);
                		wNet.addVnfInstance(hostNode, name, type, capacityGbps, occupiedCpu, occupiedRamGB, occupiedHdGb, processingTimeMs);
                	}
                )
        , (a,b)->true, null) );

        res.add(new AjtRcMenu("Remove selected VNFs", e->getSelectedElements().forEach(dd-> toVnf.apply(dd).remove ()) , (a,b)->b>0, null) );

        res.add(new AjtRcMenu("Set capacity (Gbps) of selected VNF instances", e-> 
    	DialogBuilder.launch(
            "Set capacity (Gbps) of selected VNF instances" , 
            "Please introduce the requested information", 
            "", 
            this, 
            Arrays.asList(InputForDialog.inputTfDouble("Capacity (Gbps)", "Introduce the requested information", 10, 100.0)),
            (list)->
            	{
            		final double value = (Double) list.get(0).get();
            		getSelectedElements().stream().map(ee->toVnf.apply(ee)).forEach(ee->ee.setCapacityInGbpsOfInputTraffic(Optional.of(value) , Optional.empty(), Optional.empty(), Optional.empty()));
            	}
            ) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Set number of allocated CPUs of selected VNF instances", e-> 
    	DialogBuilder.launch(
            "Set number of allocated CPUs of selected VNF instances" , 
            "Please introduce the requested information", 
            "", 
            this, 
            Arrays.asList(InputForDialog.inputTfDouble("Number of allocated CPUs", "Introduce the requested information", 10, 1.0)),
            (list)->
            	{
            		final double value = (Double) list.get(0).get();
            		getSelectedElements().stream().map(ee->toVnf.apply(ee)).forEach(ee->ee.setCapacityInGbpsOfInputTraffic(Optional.empty() , Optional.of(value), Optional.empty(), Optional.empty()));
            	}
            ) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Set amount of RAM (GB) allocated to selected VNF instances", e-> 
    	DialogBuilder.launch(
            "Set amount of RAM (GB) allocated to selected VNF instances" , 
            "Please introduce the requested information", 
            "", 
            this, 
            Arrays.asList(InputForDialog.inputTfDouble("RAM (GigaBytes)", "Introduce the requested information", 10, 32.0)),
            (list)->
            	{
            		final double value = (Double) list.get(0).get();
            		getSelectedElements().stream().map(ee->toVnf.apply(ee)).forEach(ee->ee.setCapacityInGbpsOfInputTraffic(Optional.empty() , Optional.empty(), Optional.of(value), Optional.empty()));
            	}
            ) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Set amount of HD storage (TB) allocated to selected VNF instances", e-> 
    	DialogBuilder.launch(
            "Set amount of HD storage (TB) allocated to selected VNF instances" , 
            "Please introduce the requested information", 
            "", 
            this, 
            Arrays.asList(InputForDialog.inputTfDouble("HD (TeraBytes)", "Introduce the requested information", 10, 1.0)),
            (list)->
            	{
            		final double value = (Double) list.get(0).get();
            		getSelectedElements().stream().map(ee->toVnf.apply(ee)).forEach(ee->ee.setCapacityInGbpsOfInputTraffic(Optional.empty() , Optional.empty(), Optional.empty(), Optional.of(value*1000)));
            	}
            ) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Set processing time (ms) of selected VNF instances", e-> 
    	DialogBuilder.launch(
            "Set processing time (ms) of selected VNF instances" , 
            "Please introduce the requested information", 
            "", 
            this, 
            Arrays.asList(InputForDialog.inputTfDouble("Processing time (ms)", "Introduce the requested information", 10, 10.0)),
            (list)->
            	{
            		final double value = (Double) list.get(0).get();
            		getSelectedElements().stream().map(ee->toVnf.apply(ee)).forEach(ee->ee.setProcessingTimeInMs(value));
            	}
            ) , (a,b)->b>0, null));

        return res;
    }
    
}
