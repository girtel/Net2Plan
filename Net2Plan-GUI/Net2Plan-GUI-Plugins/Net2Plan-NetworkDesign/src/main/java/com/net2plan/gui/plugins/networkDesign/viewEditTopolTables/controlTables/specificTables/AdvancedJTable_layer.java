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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;

/**
 */
@SuppressWarnings({ "unchecked", "serial" })
public class AdvancedJTable_layer extends AdvancedJTable_networkElement<NetworkLayer>
{
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_NAME = 2;
    
    public AdvancedJTable_layer(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.LAYERS , layerThisTable , false , null);
    }


    @Override
  public List<AjtColumnInfo<NetworkLayer>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
      final List<AjtColumnInfo<NetworkLayer>> res = new LinkedList<> ();
      res.add(new AjtColumnInfo<NetworkLayer>(this , String.class, null , "Name", "Layer name", (d,val)->d.setName((String)val) , d->d.getName() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<NetworkLayer>(this , String.class, null , "Description", "Layer description", (d,val)->d.setDescription((String)val) , d->d.getDescription() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<NetworkLayer>(this , String.class, null , "Demand traffic units", "Name of the traffic units of the demands", (d,val)->d.getNetPlan().setDemandTrafficUnitsName((String)val, d) , d->d.getDemandTrafficUnits() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<NetworkLayer>(this , String.class, null , "Link capacity units", "Name of the capacity units of the links", (d,val)->d.getNetPlan().setLinkCapacityUnitsName((String)val, d) , d->d.getLinkCapacityUnits() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# links", "Number of links", null , d->d.getNetPlan().getNumberOfLinks(d) , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# demands", "Number of demands", null , d->d.getNetPlan().getNumberOfDemands(d) , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# multicast demands", "Number of multicast demands", null , d->d.getNetPlan().getNumberOfMulticastDemands(d) , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# routes", "Number of routes", null , d->d.getNetPlan().getNumberOfRoutes(d) , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# forw. rules", "Number of forwarding rules", null , d->d.getNetPlan().getNumberOfForwardingRules(d) , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<NetworkLayer>(this , Integer.class, null , "# multicast trees", "Number of multicast trees", null , d->d.getNetPlan().getNumberOfMulticastTrees(d) , AGTYPE.NOAGGREGATION , null));
      return res;
  }


    @Override
    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
    	final NetPlan np = callback.getDesign();
    	;
        final List<AjtRcMenu> res = new ArrayList<> ();
        res.add(new AjtRcMenu("Add layer", e-> { np.addLayer("Layer " + np.getNumberOfLayers(), null, null, null, null, null); } , (a,b)->true, null));
        res.add(new AjtRcMenu("Remove selected layers", e->getSelectedElements().forEach(dd->np.removeNetworkLayer((NetworkLayer) dd)) , (a,b)->b>0, null));
        return res;
    }
}

