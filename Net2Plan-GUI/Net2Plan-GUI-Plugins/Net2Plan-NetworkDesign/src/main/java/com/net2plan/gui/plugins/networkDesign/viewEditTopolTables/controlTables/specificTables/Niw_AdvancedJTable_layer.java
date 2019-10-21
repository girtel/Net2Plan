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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.niw.WAbstractNetworkElement;
import com.net2plan.niw.WLayerIp;
import com.net2plan.niw.WLayerWdm;
import com.net2plan.niw.WNet;

/**
 */
@SuppressWarnings({ "unchecked", "serial" })
public class Niw_AdvancedJTable_layer extends AdvancedJTable_networkElement<NetworkLayer>
{
	private static DecimalFormat df = new DecimalFormat("#.##");
    public Niw_AdvancedJTable_layer(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.LAYERS , "Layers" , layerThisTable , false , null);
    }

    @Override
  public List<AjtColumnInfo<NetworkLayer>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
    	final WNet wNet = callback.getNiwInfo().getSecond(); 
    	final Function<NetworkLayer,Boolean> isIp = d -> { final WAbstractNetworkElement ee = wNet.getWElement(d).orElse(null); return ee == null? false : ee.isLayerIp(); };
    	final Function<NetworkLayer,Boolean> isWdm = d -> { final WAbstractNetworkElement ee = wNet.getWElement(d).orElse(null); return ee == null? false : ee.isLayerWdm(); };
    	final Function<NetworkLayer,WLayerIp> toIp = d -> (WLayerIp) wNet.getWElement(d).get();
    	final Function<NetworkLayer,WLayerWdm> toWdm = d -> (WLayerWdm) wNet.getWElement(d).get();
    	final Function<NetworkLayer,WAbstractNetworkElement> toAbs = d -> wNet.getWElement(d).get();
    	
    	final List<AjtColumnInfo<NetworkLayer>> res = new LinkedList<> ();
        res.add(new AjtColumnInfo<NetworkLayer>(this , String.class, null , "Name", "Layer name", null , d->isIp.apply(d)? toIp.apply(d).getName() : (isWdm.apply(d)? toWdm.apply(d).getName() : "--"), AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<NetworkLayer>(this , String.class, null , "Description", "Layer description", (d,val)->{  if (isIp.apply(d)) toIp.apply(d).setDescription((String) val); else if (isWdm.apply(d)) toWdm.apply(d).setDescription((String) val); } , d->isIp.apply(d)? toIp.apply(d).getDescription() : (isWdm.apply(d)? toWdm.apply(d).getDescription() : "--") , AGTYPE.NOAGGREGATION , null) );
        res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# Nodes", "Number of nodes", null , d->wNet.getNumberOfNodes() , AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# SRGs", "Number of shared risk groups defined", null , d->wNet.getSrgs().size() , AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# Fibers", "Number of fibers defined", null , d->isWdm.apply(d)? wNet.getFibers().size () : "--", AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# Lightpaths", "Number of lightpaths", null , d->isWdm.apply(d)? wNet.getLightpaths().size ()  : "--", AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# Lp. requests", "Number of lightpath requests", null , d->isWdm.apply(d)? wNet.getLightpathRequests().size ()  : "--", AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# IP links", "Number of IP links (unidi)", null , d->isIp.apply(d)? wNet.getIpLinks().size() : "--" , AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# IP source-routed flows", "Number of IP source routed connections", null , d->isIp.apply(d)? wNet.getIpSourceRoutedConnections().size() : "--"  , AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# IP unic. demands", "Number of IP unicast demands", null , d->isIp.apply(d)? wNet.getIpUnicastDemands().size() : "--"  , AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# IP SC requests", "Number of IP service chain requestst (potentially anycast)", null , d->isIp.apply(d)? wNet.getServiceChainRequests().size() : "--"  , AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# IP SCs", "Number of serivce chains", null , d->isIp.apply(d)? wNet.getServiceChains().size() : "--"  , AGTYPE.NOAGGREGATION , null));
        res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# VNF instances", "Number of VNf instances", null , d->isIp.apply(d)? wNet.getVnfInstances().size() : "--"  , AGTYPE.NOAGGREGATION , null));
        return res;
  }

    @Override
    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
        final List<AjtRcMenu> res = new ArrayList<> ();
    	final WNet wNet = callback.getNiwInfo().getSecond(); 
    	final Function<NetworkLayer,Boolean> isIp = d -> { final WAbstractNetworkElement ee = wNet.getWElement(d).orElse(null); return ee == null? false : ee.isLayerIp(); };
    	final Function<NetworkLayer,Boolean> isWdm = d -> { final WAbstractNetworkElement ee = wNet.getWElement(d).orElse(null); return ee == null? false : ee.isLayerWdm(); };
        res.add(new AjtRcMenu("Remove selected layers", e->getSelectedElements().forEach(d->{if (isIp.apply (d)) wNet.removeIpLayer(); else if (isWdm.apply(d)) wNet.removeWdmLayer(); }) , (a,b)->b>0, null) );
        res.add(new AjtRcMenu("Add IP layer", e-> { if (wNet.getIpLayer().isPresent()) return; wNet.addIpLayer();  } , (a,b)->true, null)  );
        res.add(new AjtRcMenu("Add WDM layer", e-> { if (wNet.getWdmLayer().isPresent()) return; wNet.addWdmLayer();  } , (a,b)->true, null)  );
        return res;
    }


    
}
