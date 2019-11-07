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
import java.awt.geom.Point2D;
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
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.DialogBuilder;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.InputForDialog;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.niw.IOadmArchitecture;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNetConstants;
import com.net2plan.niw.WNode;

/**
 */
@SuppressWarnings("unchecked")
public class Niw_AdvancedJTable_node extends AdvancedJTable_networkElement<Node>
{
	private static final DecimalFormat df = new DecimalFormat("#.##");
	private static Function<Double,String> df2 = d -> d <= -Double.MAX_VALUE ? "-\u221E" : d >= Double.MAX_VALUE? "\u221E" : df.format(d);
    public Niw_AdvancedJTable_node(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.NODES , layerThisTable.getName().equals(WNetConstants.ipLayerName)? "IP routers" : "OADMs" , layerThisTable , true , n->n.isDown()? Color.RED : null);
    }

    @Override
  public List<AjtColumnInfo<Node>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
    	final List<AjtColumnInfo<Node>> res = new LinkedList<> ();

    	assert callback.getNiwInfo().getFirst();
    	final WNet wNet = callback.getNiwInfo().getSecond(); 
    	final Function<Node,WNode> toWNode = n -> { final WNode nn = new WNode (n); assert !nn.isVirtualNode();  return nn; };
    	final boolean isIpLayer = getTableNetworkLayer().getName ().equals(WNetConstants.ipLayerName);
    	final boolean isWdmLayer = getTableNetworkLayer().getName ().equals(WNetConstants.wdmLayerName);
    	assert isIpLayer || isWdmLayer;
    	assert !(isIpLayer && isWdmLayer);

    	final String currentLauyout = wNet.getNe().getPlotNodeLayoutCurrentlyActive();

    	res.add(new AjtColumnInfo<Node>(this, Boolean.class, null, "Show/hide", "Indicates whether or not the node is visible in the topology canvas", (n, s) -> {
          if ((Boolean) s) callback.getVisualizationState().showOnCanvas(n);
          else callback.getVisualizationState().hideOnCanvas(n);
      }, n -> !callback.getVisualizationState().isHiddenOnCanvas(n), AGTYPE.COUNTTRUE, null));
    	
      res.add(new AjtColumnInfo<Node>(this , String.class, null , "Name", "Node name", (d,val)->toWNode.apply(d).setName((String) val), d->toWNode.apply(d).getName() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Node>(this , Boolean.class, null , "Up?", "", (d,val)->
      {
          final boolean isNodeUp = (Boolean) val;
          if (isNodeUp) new WNode (d).setAsUp(); else new WNode (d).setAsDown();  
      } , d->new WNode(d).isUp() , AGTYPE.COUNTTRUE , n->new WNode(n).isUp()? null : Color.RED));
      res.add(new AjtColumnInfo<Node>(this , Double.class, null , "X-coord", "The X coordinate of the node in he current layout. Interpreted as geographical longitude in the map view", (d,val)->d.setXYPositionMap(new Point2D.Double((Double) val , d.getXYPositionMap(currentLauyout).getY()), currentLauyout) , d->d.getXYPositionMap(currentLauyout).getX() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Node>(this , Double.class, null , "Y-coord", "The Y coordinate of the node in he current layout. Interpreted as geographical latitude in the map view", (d,val)->d.setXYPositionMap(new Point2D.Double(d.getXYPositionMap(currentLauyout).getX() , (Double) val), currentLauyout) , d->d.getXYPositionMap(currentLauyout).getY() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Node>(this , Double.class, null , "Population", "The node population", (d,val)->toWNode.apply(d).setPoputlation((Double) val) , d->toWNode.apply(d).getPopulation() , AGTYPE.SUMDOUBLE , null));
      res.add(new AjtColumnInfo<Node>(this , String.class, null , "Site", "The site this node belongs to", (d,val)->d.setSiteName((String) val) , d->d.getSiteName() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "SRGs", "The SRGs that this node belongs to", null , d->toWNode.apply(d).getSrgsThisElementIsAssociatedTo().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.NOAGGREGATION , null));
      if (isIpLayer)
      {
    	  res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "VNFs", "The VNF instances in this node", null , d->toWNode.apply(d).getVnfInstances().stream().map(v->v.getNe ()).collect(Collectors.toList())  , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "Out IP links", "The outgoing IP links of the node", null , d->toWNode.apply(d).getOutgoingIpLinks().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "In IP links", "The incoming IP links of the node", null , d->toWNode.apply(d).getIncomingIpLinks().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Double.class, null , "Out capacity (Gbps)", "The sum of the current capacity of the outgoing IP links", null , d->toWNode.apply(d).getOutgoingIpLinks().stream().mapToDouble(n->n.getCurrentCapacityGbps()).sum() , AGTYPE.SUMDOUBLE , null));
          res.add(new AjtColumnInfo<Node>(this , Double.class, null , "In capacity (Gbps)", "The sum of the current capacity of the incoming IP links", null , d->toWNode.apply(d).getIncomingIpLinks().stream().mapToDouble(n->n.getCurrentCapacityGbps()).sum() , AGTYPE.SUMDOUBLE , null));
          res.add(new AjtColumnInfo<Node>(this , Double.class, null , "Out IP traffic (Gbps)", "The sum of the traffic carried in the outgoing IP links", null , d->toWNode.apply(d).getOutgoingIpLinks().stream().mapToDouble(n->n.getCarriedTrafficGbps()).sum() , AGTYPE.SUMDOUBLE , null));
          res.add(new AjtColumnInfo<Node>(this , Double.class, null , "In IP traffic (Gbps)", "The sum of the traffic carried in the incoming IP links", null , d->toWNode.apply(d).getIncomingIpLinks().stream().mapToDouble(n->n.getCarriedTrafficGbps()).sum() , AGTYPE.SUMDOUBLE , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "Out IP demands", "The unicast IP demands that have this node as origin", null , d->toWNode.apply(d).getOutgoingIpUnicastDemands().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "In IP demands", "The unicast IP demands that have this node as origin", null , d->toWNode.apply(d).getIncomingIpUnicastDemands().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "Out SCReqs", "The service chain requests that have this node as potential origin", null , d->toWNode.apply(d).getOutgoingServiceChainRequests().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "In SCReqs", "The service chain requests that have this node as potential destination", null , d->toWNode.apply(d).getIncomingServiceChainRequests().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "Out SCs", "The service chains initiated in this node", null , d->toWNode.apply(d).getOutgoingServiceChains().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "In SCs", "The service chains ending in this node", null , d->toWNode.apply(d).getIncomingServiceChains().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          
          /* Pending information of HD/RAM etc capacicites */
          res.add(new AjtColumnInfo<Node>(this , Double.class, Arrays.asList("CPU/RAM/HD") , "Node CPUs", "The CPUs available in the node", (d,val)->toWNode.apply(d).setTotalNumCpus((Double)val) , d->toWNode.apply(d).getTotalNumCpus(), AGTYPE.NOAGGREGATION , null));
          res.add(new AjtColumnInfo<Node>(this , Double.class, Arrays.asList("CPU/RAM/HD") , "Node RAM (GB)", "The amount of RAM in the node,in GB", (d,val)->toWNode.apply(d).setTotalRamGB((Double)val) , d->toWNode.apply(d).getTotalRamGB(), AGTYPE.NOAGGREGATION , null));
          res.add(new AjtColumnInfo<Node>(this , Double.class, Arrays.asList("CPU/RAM/HD") , "Node HD (TB)", "The amount of hard-disk storage in TeraBytes", (d,val)->toWNode.apply(d).setTotalHdGB(0.001 * (Double)val) , d->toWNode.apply(d).getTotalHdGB()*1000.0, AGTYPE.NOAGGREGATION , null));
          res.add(new AjtColumnInfo<Node>(this , Double.class, Arrays.asList("CPU/RAM/HD") , "Occupied CPUs", "The CPUs occupied in the node", null , d->toWNode.apply(d).getOccupiedCpus(), AGTYPE.NOAGGREGATION , e->toWNode.apply(e).getOccupiedCpus() > toWNode.apply(e).getTotalNumCpus()? Color.red : null));
          res.add(new AjtColumnInfo<Node>(this , Double.class, Arrays.asList("CPU/RAM/HD") , "Occupied RAM (GB)", "The amount of RAM occupied in the node, in GB", null , d->toWNode.apply(d).getOccupiedRamGB(), AGTYPE.NOAGGREGATION , e->toWNode.apply(e).getOccupiedRamGB() > toWNode.apply(e).getTotalRamGB()? Color.red : null));
          res.add(new AjtColumnInfo<Node>(this , Double.class, Arrays.asList("CPU/RAM/HD") , "Occupied HD (TB)", "The amount of storage occupied in the node, in TeraBytes", null , d->toWNode.apply(d).getOccupiedHdGB() * 1000, AGTYPE.NOAGGREGATION , e->toWNode.apply(e).getOccupiedHdGB() > toWNode.apply(e).getTotalHdGB()? Color.red : null));
          
      }
      else if (isWdmLayer)
      {
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "Sw. type", "The switching type of the architecture. At this moment e.g. filterless (drop-and-waste), or non-blocking OADM", null , d->toWNode.apply(d).getOpticalSwitchingArchitecture().getShortName() , AGTYPE.NOAGGREGATION, null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "Out LP requests", "The outgoing lightpath requests of the node", null , d->toWNode.apply(d).getOutgoingLigtpathRequests().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "In LP requests", "The incoming lightpath requests of the node", null , d->toWNode.apply(d).getIncomingLigtpathRequests().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "ADD LPs", "The outgoing lightpaths of the node", null , d->toWNode.apply(d).getAddedLigtpaths().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "DROP LPs", "The incoming lightpaths of the node", null , d->toWNode.apply(d).getDroppedLigtpaths().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "EXPRESS LPs", "The lightpaths that are express to this node", null , d->toWNode.apply(d).getExpressSwitchedLightpaths().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "ADD optical rate (Gbps)", "The sum of the line rates of the lightpaths added in this node", null , d->toWNode.apply(d).getAddedLigtpaths().stream().mapToDouble(n->n.getLightpathRequest().getLineRateGbps()).sum() , AGTYPE.SUMDOUBLE , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "DROP optical rate (Gbps)", "The sum of the line rates of the lightpaths dropped in this node", null , d->toWNode.apply(d).getDroppedLigtpaths().stream().mapToDouble(n->n.getLightpathRequest().getLineRateGbps()).sum() , AGTYPE.SUMDOUBLE , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "EXPRESS optical rate (Gbps)", "The sum of the line rates of the lightpaths express in this node", null , d->toWNode.apply(d).getExpressSwitchedLightpaths().stream().mapToDouble(n->n.getLightpathRequest().getLineRateGbps()).sum() , AGTYPE.SUMDOUBLE , null));
          
          /* Pending here all the OADM information about physical impairments */
//          res.add(new AjtColumnInfo<Node>(this , Double.class, null , "Sw fabric nominal loss (dB)", "The attenuation of the optical switch fabric", (d,val)->toWNode.apply(d).setOadmSwitchFabricAttenuation_dB((Double)val) , d->toWNode.apply(d).getOadmSwitchFabricAttenuation_dB(), AGTYPE.NOAGGREGATION , null));
//          res.add(new AjtColumnInfo<Node>(this , Double.class, null , "Sw fabric PMD (ps)", "The PMD of the optical switch fabric", (d,val)->toWNode.apply(d).setOadmSwitchFabricPmd_ps((Double)val) , d->toWNode.apply(d).getOadmSwitchFabricPmd_ps(), AGTYPE.NOAGGREGATION , null));
      }
      return res;
  }

    
    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
        final List<AjtRcMenu> res = new ArrayList<> ();
    	assert callback.getNiwInfo().getFirst();
    	final WNet wNet = callback.getNiwInfo().getSecond(); 
    	final boolean isIpLayer = getTableNetworkLayer().getName ().equals(WNetConstants.ipLayerName);
    	final boolean isWdmLayer = getTableNetworkLayer().getName ().equals(WNetConstants.wdmLayerName);
    	assert isIpLayer || isWdmLayer;
    	assert !(isIpLayer && isWdmLayer);
    	final Function<Node,WNode> toWNode = d -> (WNode) wNet.getWElement(d).get();
        res.add(new AjtRcMenu("Add node", e->wNet.addNode (0 , 0 , wNet.getUnusedValidNodeName () , null), (a,b)->true, null));
        res.add(new AjtRcMenu("Remove selected nodes", e->getSelectedElements().forEach(dd->toWNode.apply(dd).remove()) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Show selected nodes", e->getSelectedElements().forEach(ee->callback.getVisualizationState().showOnCanvas(ee)) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Hide selected nodes", e->getSelectedElements().forEach(ee->callback.getVisualizationState().hideOnCanvas(ee)) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Switch selected nodes' coordinates from (x,y) to (y,x)", e->getSelectedElements().forEach(node->node.setXYPositionMap(new Point2D.Double(node.getXYPositionMap().getY(), node.getXYPositionMap().getX()))) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Re-arrange selected nodes", null , (a,b)->b>0, Arrays.asList(
        		new AjtRcMenu("Equispaced in a circunference", e-> 
        		{
                    DialogBuilder.launch(
                            "Indicate the coordinates of the circle center, and the radius", 
                            "Please introduce the requested data.",
                            "", 
                            this, 
                            Arrays.asList(
                                    InputForDialog.inputTfDouble("X position of the center", "Introduce the X position of the circle center", 10, 0.0),
                                    InputForDialog.inputTfDouble("Y position of the center", "Introduce the Y position of the circle center", 10, 0.0),
                                    InputForDialog.inputTfDouble("Radius", "Introduce the radius", 10, 100.0)
                                    ),
                            (list)->
                                {
                                    final double x = ((Double) list.get(0).get());
                                    final double y = ((Double) list.get(1).get());
                                    final double radius = ((Double) list.get(2).get());
                                    if (radius <= 0) throw new Net2PlanException ("The circle radius must e strictly positive");
                                    if (getSelectedElements().isEmpty()) throw new Net2PlanException ("No nodes are selected");
                                    int contNode = 0;
                                    final double radQuantum = 2 * Math.PI / getSelectedElements().size(); 
                                    for (Node np_ee : getSelectedElements())
                                    {
                                    	final WNode ee = toWNode.apply(np_ee);
                                    	final double newX = x + radius * Math.cos(contNode * radQuantum);
                                    	final double newY = y + radius * Math.sin(contNode * radQuantum);
                                    	contNode ++;
                                    	ee.setNodePositionXY(new Point2D.Double(newX ,  newY));
                                    }
                                }
                            );
        		}
        		, (a,b)->true, null)
        		)));
        
        
        if (isWdmLayer)
        {
        	final List<AjtRcMenu> menusForEachOpticalSwitchType = new ArrayList<> ();
        	for (IOadmArchitecture arc : IOadmArchitecture.availableRepresentatives)
        		menusForEachOpticalSwitchType.add(new AjtRcMenu(arc.getShortName(), e->getSelectedElements().forEach(ee->toWNode.apply(ee).setOpticalSwitchArchitecture(arc.getClass ())) , (a,b)->b>0, null));
            res.add(new AjtRcMenu("Set optical switching type of selected OADMs", null , (a,b)->b>0, menusForEachOpticalSwitchType));
        	
        	
        	
//            res.add(new AjtRcMenu("Set optical switching type of selected OADMs", null , (a,b)->b>0, Arrays.asList(
//            		new AjtRcMenu("As non-blocking OADM", e->getSelectedElements().forEach(ee->toWNode.apply(ee).setOpticalSwitchType(WNode.OPTICALSWITCHTYPE.ROADM)) , (a,b)->b>0, null),
//            		new AjtRcMenu("As filterless drop and waste", e->getSelectedElements().forEach(ee->toWNode.apply(ee).setOpticalSwitchType(WNode.OPTICALSWITCHTYPE.FILTERLESS_DROPANDWASTENOTDIRECTIONLESS)) , (a,b)->b>0, null)
//            		)));
//            res.add(new AjtRcMenu("Set attenuation of optical switch fabric to selected OADMs", e->
//                DialogBuilder.launch(
//                        "Set attenuation of optical switch fabric to selected OADMs", 
//                        "Please introduce the requested data.",
//                        "", 
//                        this, 
//                        Arrays.asList(
//                                InputForDialog.inputTfDouble("OADM switch fabric attenuation (dB)", "Introduce the attenuation of the optical switch fabric", 10, 0.0)
//                                ),
//                        (list)->
//                            {
//                                final double att = ((Double) list.get(0).get());
//                                getSelectedElements().forEach(dd->toWNode.apply(dd).setOadmSwitchFabricAttenuation_dB(att));
//                            }
//                        )
//             , (a,b)->b>0, null));
//            res.add(new AjtRcMenu("Set PMD of optical switch fabric to selected OADMs", e->
//            DialogBuilder.launch(
//                    "Set PMD of optical switch fabric to selected OADMs", 
//                    "Please introduce the requested data.",
//                    "", 
//                    this, 
//                    Arrays.asList(
//                            InputForDialog.inputTfDouble("OADM switch fabric PMD (ps)", "Introduce the PMD added by the optical switch fabric", 10, 0.0)
//                            ),
//                    (list)->
//                        {
//                            final double pmd = ((Double) list.get(0).get());
//                            getSelectedElements().forEach(dd->toWNode.apply(dd).setOadmSwitchFabricPmd_ps(pmd));
//                        }
//                    )
//         , (a,b)->b>0, null));
        	
        }
        
        return res;
    }
}
