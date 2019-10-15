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

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_abstractElement.AGTYPE;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.DialogBuilder;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.InputForDialog;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNode;
import com.net2plan.niw.WSharedRiskGroup;
import com.net2plan.niw.WVnfInstance;

/**
 */
@SuppressWarnings({ "unchecked", "serial" })
public class Niw_AdvancedJTable_srg extends AdvancedJTable_networkElement<SharedRiskGroup>
{
	private static DecimalFormat df = new DecimalFormat("#.##");
	
    public Niw_AdvancedJTable_srg(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.SRGS , "SRGs" , layerThisTable , true , null);
    }

    @Override
  public List<AjtColumnInfo<SharedRiskGroup>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
	final List<AjtColumnInfo<SharedRiskGroup>> res = new LinkedList<> ();
	assert callback.getNiwInfo().getFirst();
	final WNet wNet = callback.getNiwInfo().getSecond(); 
	final Function<SharedRiskGroup,WSharedRiskGroup> toSrg = d -> (WSharedRiskGroup) wNet.getWElement(d).get();

	res.add(new AjtColumnInfo<SharedRiskGroup>(this , Collection.class, null , "Nodes", "The nodes belonging to this SRG, and thus failing when the SRG occurs", null , d->toSrg.apply(d).getFailingNodes ().stream().map(e->e.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
	res.add(new AjtColumnInfo<SharedRiskGroup>(this , Collection.class, null , "Fibers", "The fibers belonging to this SRG, and thus failing when the SRG occurs", null , d->toSrg.apply(d).getFailingFibers ().stream().map(e->e.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
    res.add(new AjtColumnInfo<SharedRiskGroup>(this , Double.class, null , "MTTF (hours)" , "The average Mean-Time-To-Fail value measued in hours (the time since the element is repaired until it fails again)", (d,val)->toSrg.apply(d).setMeanTimeToFailInHours((Double) val), d->toSrg.apply(d).getMeanTimeToFailInHours() , AGTYPE.MAXDOUBLE , null));
    res.add(new AjtColumnInfo<SharedRiskGroup>(this , Double.class, null , "MTTR (hours)" , "The average Mean-Time-To-Repair value measued in hours (the time betweem the element fails, and is up again since it is repaired)", (d,val)->toSrg.apply(d).setMeanTimeToRepairInHours((Double) val), d->toSrg.apply(d).getMeanTimeToRepairInHours() , AGTYPE.MAXDOUBLE , null));
    res.add(new AjtColumnInfo<SharedRiskGroup>(this , Double.class, null , "Availability" , "The probability of findig the element not failed (MTTF / (MTTF + MTTR)) ", null , d->toSrg.apply(d).getAvailability() , AGTYPE.MAXDOUBLE , null));
	  return res;
  	}

    @Override
    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
    	assert callback.getNiwInfo().getFirst();
    	final WNet wNet = callback.getNiwInfo().getSecond(); 
    	final Function<SharedRiskGroup,WSharedRiskGroup> toSrg = d -> (WSharedRiskGroup) wNet.getWElement(d).get();
        final List<AjtRcMenu> res = new ArrayList<> ();
		final Function<String,Optional<WNode>> nodeByName = st -> 
		{
    		WNode a = wNet.getNodeByName(st).orElse(null);
    		if (a == null) a = wNet.getNodes().stream().filter(n->n.getName().equalsIgnoreCase(st)).findFirst().orElse(null);
    		return Optional.ofNullable(a);
		};

        res.add(new AjtRcMenu("Add SRG", e->wNet.addSharedRiskGroup() , (a,b)->b>0, null) );
		res.add(new AjtRcMenu("Remove selected SRGs", e->getSelectedElements().forEach(dd-> toSrg.apply(dd).remove ()) , (a,b)->b>0, null) );

		res.add(new AjtRcMenu("Add SRGs from model", e->getSelectedElements().forEach(dd-> toSrg.apply(dd).remove ()) , (a,b)->b>0, null) );

		
        res.add(new AjtRcMenu("Add SRGs from model", e->
        {
        	final List<String> models = Arrays.asList(
        			"One SRG per node" , 
        			"One SRG per unidirectional fiber" , 
        			"One SRG per bidirectional duct");
            DialogBuilder.launch(
            		"Add SRG from model", 
                    "Please introduce the information below.", 
                    "", 
                    this, 
                    Arrays.asList(
                    		InputForDialog.inputTfCombo ("SRG creation scheme" , "Please introduce the scheme to follow when generating the SRGs" , 20 , models.get(0) , models , models ,null),
                    		InputForDialog.inputTfDouble("MTTF (hours)", "Mean-Time-To-Fail in hours, to set for all the SRGs", 10, 365*24.0),
                    		InputForDialog.inputTfDouble("MTTR (hours)", "Mean-Time-To-Repair in hours, to set for all the SRGs", 10, 12.0),
                    		InputForDialog.inputCheckBox ("Remove existing SRGs?" , "Indicates if the existing SRGs should be removed before creating the new ones" , false , null)
                    		),
                    (list)->
                    	{
                    		final String srgModel = (String) list.get(0).get();
                    		final double mttf = (Double) list.get(1).get();
                    		final double mttr = (Double) list.get(2).get();
                    		final boolean removeExistingSRGs = (Boolean) list.get(3).get();
                    		if (removeExistingSRGs) for (WSharedRiskGroup srg : new ArrayList<> (wNet.getSrgs())) srg.remove();
                    		if (srgModel.equals(models.get(0)))
                    		{
                    			for (WNode n : wNet.getNodes())
                    				wNet.addSharedRiskGroup().setMeanTimeToFailInHours(mttf).setMeanTimeToRepairInHours(mttr).addFailingNode(n);
                    		}
                    		else if (srgModel.equals(models.get(1)))
                    		{
                    			for (WNode n : wNet.getNodes())
                    				wNet.addSharedRiskGroup().setMeanTimeToFailInHours(mttf).setMeanTimeToRepairInHours(mttr).addFailingNode(n);
                    		}
                    	}
                    );
        }, (a,b)->true, null));

		
		
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
