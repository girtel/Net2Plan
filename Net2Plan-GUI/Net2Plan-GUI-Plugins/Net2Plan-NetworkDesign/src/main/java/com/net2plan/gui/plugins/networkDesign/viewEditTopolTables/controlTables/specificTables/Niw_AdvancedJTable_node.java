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
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.niw.networkModel.WNet;
import com.net2plan.niw.networkModel.WNetConstants;
import com.net2plan.niw.networkModel.WNode;

/**
 */
@SuppressWarnings("unchecked")
public class Niw_AdvancedJTable_node extends AdvancedJTable_networkElement<Node>
{
	private static DecimalFormat df = new DecimalFormat("#.##");
    public Niw_AdvancedJTable_node(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.NODES , layerThisTable.getName().equals(WNetConstants.ipLayerName)? "IP routers" : "OADMs" , layerThisTable , true , n->n.isDown()? Color.RED : null);
    }

    @Override
  public List<AjtColumnInfo<Node>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
    	final List<AjtColumnInfo<Node>> res = new LinkedList<> ();

    	final WNet wNet = new WNet (callback.getDesign()); 
    	final Function<Node,WNode> toWNode = n -> { final WNode nn = new WNode (n); assert !nn.isVirtualNode();  return nn; };
    	final boolean isIpLayer = getTableNetworkLayer().equals(wNet.getIpLayer().getNe());
    	final boolean isWdmLayer = getTableNetworkLayer().equals(wNet.getWdmLayer().getNe());
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
      }
      else if (isWdmLayer)
      {
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "Out LP requests", "The outgoing lightpath requests of the node", null , d->toWNode.apply(d).getOutgoingLigtpathRequests().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "In LP requests", "The incoming lightpath requests of the node", null , d->toWNode.apply(d).getIncomingLigtpathRequests().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "ADD LPs", "The outgoing lightpaths of the node", null , d->toWNode.apply(d).getOutgoingLigtpaths().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "DROP LPs", "The incoming lightpaths of the node", null , d->toWNode.apply(d).getIncomingLigtpaths().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "EXPRESS LPs", "The lightpaths that are express to this node", null , d->toWNode.apply(d).getExpressSwitchedLightpaths().stream().map(n->n.getNe()).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "ADD optical rate (Gbps)", "The sum of the line rates of the lightpaths added in this node", null , d->toWNode.apply(d).getOutgoingLigtpaths().stream().mapToDouble(n->n.getLightpathRequest().getLineRateGbps()).sum() , AGTYPE.SUMDOUBLE , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "DROP optical rate (Gbps)", "The sum of the line rates of the lightpaths dropped in this node", null , d->toWNode.apply(d).getIncomingLigtpaths().stream().mapToDouble(n->n.getLightpathRequest().getLineRateGbps()).sum() , AGTYPE.SUMDOUBLE , null));
          res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "EXPRESS optical rate (Gbps)", "The sum of the line rates of the lightpaths express in this node", null , d->toWNode.apply(d).getExpressSwitchedLightpaths().stream().mapToDouble(n->n.getLightpathRequest().getLineRateGbps()).sum() , AGTYPE.SUMDOUBLE , null));
      }
      return res;
  }

    
    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
    	final NetPlan np = callback.getDesign();
        final List<AjtRcMenu> res = new ArrayList<> ();
        res.add(new AjtRcMenu("Add node", e->np.addNode (0 , 0 , "Node " + np.getNumberOfNodes() , null), (a,b)->true, null));
        res.add(new AjtRcMenu("Remove selected nodes", e->getSelectedElements().forEach(dd->((Node)dd).remove()) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Show selected nodes", e->getSelectedElements().forEach(ee->callback.getVisualizationState().showOnCanvas(ee)) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Hide selected nodes", e->getSelectedElements().forEach(ee->callback.getVisualizationState().hideOnCanvas(ee)) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Switch selected nodes' coordinates from (x,y) to (y,x)", e->getSelectedElements().forEach(node->node.setXYPositionMap(new Point2D.Double(node.getXYPositionMap().getY(), node.getXYPositionMap().getX()))) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Create planning domain restricted to selected nodes", e->np.restrictDesign(getSelectedElements()) , (a,b)->b>0, null));
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
                                    for (Node ee : getSelectedElements())
                                    {
                                    	final double newX = x + radius * Math.cos(contNode * radQuantum);
                                    	final double newY = y + radius * Math.sin(contNode * radQuantum);
                                    	contNode ++;
                                    	ee.setXYPositionMap(new Point2D.Double(newX ,  newY));
                                    }
                                }
                            );
        		}
        		, (a,b)->true, null),
        		new AjtRcMenu("To match length information", e-> 
        		{
                    DialogBuilder.launch(
                            "Indicate the coordinates of the circle center, and the radius", 
                            "Please introduce the requested data.",
                            "", 
                            this, 
                            Arrays.asList(
                                    InputForDialog.inputTfDouble("Minimum X coordinate", "Introduce the minimum valu for the node X position", 10, -10.0),
                                    InputForDialog.inputTfDouble("Minimum Y coordinate", "Introduce the minimum valu for the node Y position", 10, -10.0)
                                    ),
                            (list)->
                                {
                                    final double minX = ((Double) list.get(0).get());
                                    final double minY = ((Double) list.get(1).get());
                                    throw new Net2PlanException ("The algorithm is not available");
                                }
                            );
        		}
        		, (a,b)->true, null)
        		
        		
        		)));

        return res;
    }
}
