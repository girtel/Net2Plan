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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Sets;
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
import com.net2plan.libraries.GraphUtils;

/**
 */
@SuppressWarnings("unchecked")
public class AdvancedJTable_node extends AdvancedJTable_networkElement<Node>
{
    public AdvancedJTable_node(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.NODES , null , layerThisTable , true , n->n.isDown()? Color.RED : null);
    }

    @Override
  public List<AjtColumnInfo<Node>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
    	final NetPlan np = callback.getDesign();
    	final NetworkLayer layer = this.getTableNetworkLayer();
    	final String currentLauyout = np.getPlotNodeLayoutCurrentlyActive();
    	final List<AjtColumnInfo<Node>> res = new LinkedList<> ();
      res.add(new AjtColumnInfo<Node>(this, Boolean.class, null, "Show/hide", "Indicates whether or not the node is visible in the topology canvas", (n, s) -> {
          if ((Boolean) s) callback.getVisualizationState().showOnCanvas(n);
          else callback.getVisualizationState().hideOnCanvas(n);
      }, n -> !callback.getVisualizationState().isHiddenOnCanvas(n), AGTYPE.COUNTTRUE, null));
      res.add(new AjtColumnInfo<Node>(this , String.class, null , "Name", "Node name", (d,val)->d.setName((String) val), d->d.getName() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Node>(this , Boolean.class, null , "Up?", "", (d,val)->
      {
          final boolean isNodeUp = (Boolean) val;
          d.setFailureState(isNodeUp);
      } , d->d.isUp() , AGTYPE.COUNTTRUE , n->n.isUp()? null : Color.RED));
      res.add(new AjtColumnInfo<Node>(this , Double.class, null , "X-coord", "The X coordinate of the node in he current layout. Interpreted as geographical longitude in the map view", (d,val)->d.setXYPositionMap(new Point2D.Double((Double) val , d.getXYPositionMap(currentLauyout).getY()), currentLauyout) , d->d.getXYPositionMap(currentLauyout).getX() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Node>(this , Double.class, null , "Y-coord", "The Y coordinate of the node in he current layout. Interpreted as geographical latitude in the map view", (d,val)->d.setXYPositionMap(new Point2D.Double(d.getXYPositionMap(currentLauyout).getX() , (Double) val), currentLauyout) , d->d.getXYPositionMap(currentLauyout).getY() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Node>(this , Double.class, null , "Population", "The node population", (d,val)->d.setPopulation((Double) val) , d->d.getPopulation() , AGTYPE.SUMDOUBLE , null));
      res.add(new AjtColumnInfo<Node>(this , String.class, null , "Site", "The site this node belongs to", (d,val)->d.setSiteName((String) val) , d->d.getSiteName() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "SRGs", "The SRGs that this node belongs to", null , d->d.getSRGs() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "Resources", "The resources hosted in this node", null , d->d.getResources() , AGTYPE.SUMCOLLECTIONCOUNT , null));
      res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "Out links", "The outgoing links of the node", null , d->d.getOutgoingLinks(layer) , AGTYPE.SUMCOLLECTIONCOUNT , null));
      res.add(new AjtColumnInfo<Node>(this , Collection.class, null , "In links", "The incoming links of the node", null , d->d.getIncomingLinks(layer) , AGTYPE.SUMCOLLECTIONCOUNT , null));
      res.add(new AjtColumnInfo<Node>(this , Double.class, null , "Out capacity (" + layer.getLinkCapacityUnits() + ")", "The sum of the capacity of the outgoing links", null , d->d.getOutgoingLinksCapacity(layer) , AGTYPE.SUMDOUBLE , null));
      res.add(new AjtColumnInfo<Node>(this , Double.class, null , "Out traffic (" + layer.getDemandTrafficUnits() + ")", "The sum of the traffic carried in the outgoing links", null , d->d.getOutgoingLinksTraffic(layer) , AGTYPE.SUMDOUBLE , null));
      res.add(new AjtColumnInfo<Node>(this , Double.class, null , "In capacity (" + layer.getLinkCapacityUnits() + ")", "The sum of the capacity of the incoming links", null , d->d.getIncomingLinksCapacity(layer) , AGTYPE.SUMDOUBLE, null));
      res.add(new AjtColumnInfo<Node>(this , Double.class, null , "In traffic (" + layer.getDemandTrafficUnits() + ")", "The sum of the traffic carried in the incoming links", null , d->d.getIncomingLinksTraffic(layer) , AGTYPE.SUMDOUBLE , null));
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
