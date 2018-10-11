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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.DialogBuilder;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.InputForDialog;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.monitoring.MonitoringUtils;
import com.net2plan.gui.utils.StringLabeller;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;

import net.miginfocom.swing.MigLayout;

/**
 */
@SuppressWarnings({ "unchecked", "serial" })
public class AdvancedJTable_link extends AdvancedJTable_networkElement<Link>
{
	
    public AdvancedJTable_link(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.LINKS , layerThisTable , true , e->e.isUp()? null : Color.RED);
    }

    @Override
  public List<AjtColumnInfo<Link>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
    	final NetPlan np = callback.getDesign();
    	final NetworkLayer layer = this.getTableNetworkLayer();
    	final Map<Link,SortedMap<String,Pair<Double,Double>>> allLinksPerQosOccupationAndQosViolationMap = np.getAllLinksPerQosOccupationAndQosViolationMap (layer);
     
     final List<AjtColumnInfo<Link>> res = new LinkedList<> ();
      res.add(new AjtColumnInfo<Link>(this, Boolean.class, null, "Show/hide", "Indicates whether or not the link is visible in the topology canvas", (n, s) -> {
          if ((Boolean) s) callback.getVisualizationState().showOnCanvas(n);
          else callback.getVisualizationState().hideOnCanvas(n);
      }, n -> !callback.getVisualizationState().isHiddenOnCanvas(n), AGTYPE.COUNTTRUE, null));
      res.add(new AjtColumnInfo<Link>(this , Node.class, null , "A", "Origin node", null , d->d.getOriginNode() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Link>(this , Node.class, null , "B", "Destination node", null , d->d.getDestinationNode() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Link>(this , Link.class, null , "Bidirectional pair", "If the link is bidirectional, provides its bidirectional pair", null , d->d.getBidirectionalPair() , AGTYPE.NOAGGREGATION, null));
      res.add(new AjtColumnInfo<Link>(this , Boolean.class, null , "Up?", "", (d,val)->
      {
          final boolean isLinkUp = (Boolean) val;
          try
          {
              if (callback.getVisualizationState().isWhatIfAnalysisActive())
                  callback.getWhatIfAnalysisPane().whatIfLinkNodesFailureStateChanged(null, null, isLinkUp ? Sets.newHashSet(d) : null, isLinkUp ? null : Sets.newHashSet(d));
              else
                  d.setFailureState(isLinkUp);
          } catch (Throwable ee) { ee.printStackTrace(); throw new Net2PlanException (ee.getMessage()); }
      } , d->d.isUp() , AGTYPE.COUNTTRUE , e->e.isUp()? null : Color.RED));
      res.add(new AjtColumnInfo<Link>(this , String.class, null , "Trav. QoS types" , "The QoS types of the traversing trafffics", null , d->allLinksPerQosOccupationAndQosViolationMap.getOrDefault(d, new TreeMap<> ()).entrySet().stream().filter(ee->ee.getValue().getFirst() > 0).map(ee->ee.getKey()).collect(Collectors.joining(",")) , AGTYPE.SUMDOUBLE , null));
      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Total QoS violation (" + layer.getLinkCapacityUnits() + ")" , "The total amount of link capacity that is being used outside the QoS contract, and thus the traffic using it would not be carried (and thus would be blocked) if the network applied a drop policy to it, or just if such extra capacity is not present in the link", null , d->allLinksPerQosOccupationAndQosViolationMap.getOrDefault(d, new TreeMap<> ()).values().stream().mapToDouble(ee->ee.getSecond()).sum() , AGTYPE.SUMDOUBLE , e->allLinksPerQosOccupationAndQosViolationMap.getOrDefault(e, new TreeMap<> ()).values().stream().mapToDouble(ee->ee.getSecond()).sum() == 0? null : Color.RED));
      res.add(new AjtColumnInfo<Link>(this , String.class, null , "QoS scheduling" , "The scheduling configuration for the link QoS enforcement. For each QoS type, the priority assigned (lower better), and the maximum link utilization allowed for it", null , d->d.getQosTypePriorityAndMaxLinkUtilizationMap().toString() , AGTYPE.NOAGGREGATION, null));
      res.add(new AjtColumnInfo<Link>(this , NetworkElement.class, null , "Coupled demand", "The demand or multicast demand that this link is coupled to, if any", null , d->d.getCoupledDemand() != null? d.getCoupledDemand() : d.getCoupledMulticastDemand() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Capacity (" + getTableNetworkLayer().getLinkCapacityUnits() + ")", "Link capacity", (d,val)->{ if (!d.isCoupled()) d.setCapacity((Double) val); }, d->d.getCapacity() , AGTYPE.SUMDOUBLE , e->e.getCapacity() == 0? Color.YELLOW : null));
      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Occupied capacity (" + getTableNetworkLayer().getLinkCapacityUnits() + ")", "Link occupied capacity", null , d->d.getOccupiedCapacity() , AGTYPE.SUMDOUBLE , null));
      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Carried traffic (" + getTableNetworkLayer().getDemandTrafficUnits() + ")", "Link carried traffic", null , d->d.getCarriedTraffic() , AGTYPE.SUMDOUBLE , null));
      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Utilization", "Link utilization (occupied capacity vs. capacity)", null , d->d.getUtilization() , AGTYPE.NOAGGREGATION, e->{ final double v = e.getUtilization(); return v == 1? Color.YELLOW : (v > 1?  Color.RED : null  ); }));
      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Length (km)", "Link length in km, considering the worst case lower layer propagation", null , d->d.getLengthInKm() , AGTYPE.SUMDOUBLE, null));
      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Prop. speed (km/sec)", "Link average propagation speed in km per second", (d,val)->{ if (!d.isCoupled()) d.setPropagationSpeedInKmPerSecond((Double)val); } , d->d.getPropagationSpeedInKmPerSecond() , AGTYPE.NOAGGREGATION, null));
      res.add(new AjtColumnInfo<Link>(this , Double.class, null , "Prop. delay (ms)", "Link propagation delay in ms, considering the worst case lower layer propagation of the traffic", null , d->d.getPropagationDelayInMs() , AGTYPE.MAXDOUBLE, null));
      res.add(new AjtColumnInfo<Link>(this , Collection.class, null , "Routes", "Traversing routes", null , d->d.getTraversingRoutes() , AGTYPE.NOAGGREGATION, null));
      res.add(new AjtColumnInfo<Link>(this , Collection.class, null , "Forw. rules", "Forwarding rules defined for this link", null , d->d.getForwardingRules().keySet() , AGTYPE.NOAGGREGATION, null));
      res.add(new AjtColumnInfo<Link>(this , Collection.class, null , "Trees", "Traversing multicast trees", null , d->d.getTraversingTrees() , AGTYPE.NOAGGREGATION, null));
      res.add(new AjtColumnInfo<Link>(this , Collection.class, null , "SRGs", "The SRGs that this link belongs to", null , d->d.getSRGs() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Link>(this , Integer.class, null , "#Monit points" , "Number of samples of the carried traffic stored, coming from a monitoring or forecasting traffic process", null , d->d.getMonitoredOrForecastedCarriedTraffic().getSize() , AGTYPE.NOAGGREGATION , null));
      return res;
  }

    @Override
    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
    	final NetPlan np = callback.getDesign();
        final List<AjtRcMenu> res = new ArrayList<> ();
        res.add(new AjtRcMenu("Add link", e->AdvancedJTable_demand.createLinkDemandGUI(NetworkElementType.LINK, getTableNetworkLayer () , callback), (a,b)->true, null));
        res.add(new AjtRcMenu("Remove selected links", e->getSelectedElements().forEach(dd->((Link)dd).remove()) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Generate full-mesh (link length as Euclidean distance)", e->new FullMeshTopology(this , callback, true), (a, b)->true, null));
        res.add(new AjtRcMenu("Generate full-mesh (link length as Haversine distance)", e->new FullMeshTopology(this , callback, false), (a, b)->true, null));
        res.add(new AjtRcMenu("Show selected links", e->getSelectedElements().forEach(ee->callback.getVisualizationState().showOnCanvas(ee)) , (a,b)->true, null));
        res.add(new AjtRcMenu("Hide selected links", e->getSelectedElements().forEach(ee->callback.getVisualizationState().hideOnCanvas(ee)) , (a,b)->true, null));
        res.add(new AjtRcMenu("Decouple selected links", e->getSelectedElements().forEach(dd->((Link)dd).decouple()) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Create lower layer coupled demand from uncoupled links in selection", e->
        {
            Collection<Long> layerIds = np.getNetworkLayerIds();
            final JComboBox<StringLabeller> layerSelector = new WiderJComboBox();
            for (long layerId : layerIds)
            {
                final String layerName = np.getNetworkLayerFromId(layerId).getName();
                String layerLabel = "Layer " + layerId;
                if (!layerName.isEmpty()) layerLabel += " (" + layerName + ")";
                layerSelector.addItem(StringLabeller.of(layerId, layerLabel));
            }

            layerSelector.setSelectedIndex(0);

            JPanel pane = new JPanel();
            pane.add(new JLabel("Select layer: "));
            pane.add(layerSelector);

            while (true)
            {
                int result = JOptionPane.showConfirmDialog(null, pane, "Please select the lower layer to create the demand", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (result != JOptionPane.OK_OPTION) return;
                final long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                for (Link link : getSelectedElements ())
                    if (!link.isCoupled())
                        link.coupleToNewDemandCreated(np.getNetworkLayerFromId(layerId) , RoutingType.SOURCE_ROUTING);
                break;
            }
        } , (a,b)->b>0, null));

        res.add(new AjtRcMenu("Couple link to lower layer demand", e->
        {
            Collection<Long> layerIds = np.getNetworkLayerIds();
            final JComboBox<StringLabeller> layerSelector = new WiderJComboBox();
            final JComboBox<StringLabeller> demandSelector = new WiderJComboBox();
            for (long layerId : layerIds)
            {
                if (layerId == this.getTableNetworkLayer().getId()) continue;

                final String layerName = np.getNetworkLayerFromId(layerId).getName();
                String layerLabel = "Layer " + layerId;
                if (!layerName.isEmpty()) layerLabel += " (" + layerName + ")";
                layerSelector.addItem(StringLabeller.of(layerId, layerLabel));
            }

            layerSelector.addItemListener(e1 ->
            {
                if (layerSelector.getSelectedIndex() >= 0)
                {
                    long selectedLayerId = (Long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();

                    demandSelector.removeAllItems();
                    for (Demand demand : np.getDemands(np.getNetworkLayerFromId(selectedLayerId)))
                    {
                        if (demand.isCoupled()) continue;
                        long ingressNodeId = demand.getIngressNode().getId();
                        long egressNodeId = demand.getEgressNode().getId();
                        String ingressNodeName = demand.getIngressNode().getName();
                        String egressNodeName = demand.getEgressNode().getName();

                        demandSelector.addItem(StringLabeller.unmodifiableOf(demand.getId(), "d" + demand.getId() + " [n" + ingressNodeId + " (" + ingressNodeName + ") -> n" + egressNodeId + " (" + egressNodeName + ")]"));
                    }
                }

                if (demandSelector.getItemCount() == 0)
                {
                    demandSelector.setEnabled(false);
                } else
                {
                    demandSelector.setSelectedIndex(0);
                    demandSelector.setEnabled(true);
                }
            });

            layerSelector.setSelectedIndex(-1);
            layerSelector.setSelectedIndex(0);

            JPanel pane = new JPanel(new MigLayout("", "[][grow]", "[][]"));
            pane.add(new JLabel("Select layer: "));
            pane.add(layerSelector, "growx, wrap");
            pane.add(new JLabel("Select demand: "));
            pane.add(demandSelector, "growx, wrap");

            while (true)
            {
                int result = JOptionPane.showConfirmDialog(null, pane, "Please select the lower layer demand", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (result != JOptionPane.OK_OPTION) return;
                final Long demandId = (Long) ((StringLabeller) demandSelector.getSelectedItem()).getObject();
                final Demand demand = np.getDemandFromId(demandId);
                final Link link = getSelectedElements().first();
                demand.coupleToUpperOrSameLayerLink(link);
                break;
            }
        } , (a,b)->b>0, null));

        res.add(new AjtRcMenu("Set selected links capacity", e->
        {
            DialogBuilder.launch(
                    "Set selected links capacity" , 
                    "Please introduce the link capacity. Negative values are not allowed. The capacity will be assigned to not coupled links", 
                    "", 
                    this, 
                    Arrays.asList(InputForDialog.inputTfDouble("Link capacity (" + getTableNetworkLayer().getLinkCapacityUnits() + ")", "Introduce the link capacity", 10, 0.0)),
                    (list)->
                    	{
                    		final double newLinkCapacity = (Double) list.get(0).get();
                    		getSelectedElements().stream().filter(ee->!ee.isCoupled()).forEach(ee->ee.setCapacity(newLinkCapacity));
                    	}
                    );
        } , (a,b)->b>0, null));

        
        res.add(new AjtRcMenu("Set selected links capacity to match a given utilization", e->
        {
            DialogBuilder.launch(
                    "Set selected links capacity to match utilization" , 
                    "Please introduce the link target utilization. Negative values are not allowed. The capacity will be assigned to not coupled links", 
                    "", 
                    this, 
                    Arrays.asList(InputForDialog.inputTfDouble("Link utilization", "Introduce the link utilization", 10, 0.5)),
                    (list)->
                    	{
                    		final double newLinkUtilization = (Double) list.get(0).get();
                    		if (newLinkUtilization <= 0) throw new Net2PlanException ("Link utilization must be positive");
                    		getSelectedElements().stream().filter(ee->!ee.isCoupled()).forEach(ee->ee.setCapacity(ee.getOccupiedCapacity() /  newLinkUtilization));
                    	}
                    );
        } , (a,b)->b>0, null));

        res.add(new AjtRcMenu("Set selected links length", e->
        {
            DialogBuilder.launch(
                    "Set selected links length (km)" , 
                    "Please introduce the link length. Negative values are not allowed. The length will be assigned to not coupled links", 
                    "", 
                    this, 
                    Arrays.asList(InputForDialog.inputTfDouble("Link length (km)", "Introduce the link length", 10, 0.0)),
                    (list)->
                    	{
                    		final double newLinkLength = (Double) list.get(0).get();
                    		getSelectedElements().stream().filter(ee->!ee.isCoupled()).forEach(ee->ee.setLengthInKm(newLinkLength));
                    	}
                    );
        } , (a,b)->b>0, null));

        res.add(new AjtRcMenu("Scale selected links length", e->
        {
            DialogBuilder.launch(
                    "Scale selected links length (km)" , 
                    "Please introduce the scaling factor for which the link lengths will be multiplied. Negative values are not allowed. The length will be assigned to not coupled links", 
                    "", 
                    this, 
                    Arrays.asList(InputForDialog.inputTfDouble("Scaling factor", "Introduce the scaling factor", 10, 1.0)),
                    (list)->
                    	{
                    		final double scalingFactor = (Double) list.get(0).get();
                    		getSelectedElements().stream().filter(ee->!ee.isCoupled()).forEach(ee->ee.setLengthInKm(scalingFactor * ee.getLengthInKm()));
                    	}
                    );
        } , (a,b)->b>0, null));

        res.add(new AjtRcMenu("Set selected links length as the euclidean node pair distance", e->
        {
    		getSelectedElements().stream().filter(ee->!ee.isCoupled()).forEach(ee->ee.setLengthInKm(np.getNodePairEuclideanDistance(ee.getOriginNode(), ee.getDestinationNode())));
        } , (a,b)->b>0, null));

        res.add(new AjtRcMenu("Set selected links length as the harversine node pair distance", e->
        {
    		getSelectedElements().stream().filter(ee->!ee.isCoupled()).forEach(ee->ee.setLengthInKm(np.getNodePairHaversineDistanceInKm(ee.getOriginNode(), ee.getDestinationNode())));
        } , (a,b)->b>0, null));
        
        res.add(new AjtRcMenu("Monitor/forecast...",  null , (a,b)->true, Arrays.asList(
                MonitoringUtils.getMenuAddSyntheticMonitoringInfo (this),
                MonitoringUtils.getMenuExportMonitoringInfo(this),
                MonitoringUtils.getMenuImportMonitoringInfo (this),
                MonitoringUtils.getMenuSetMonitoredTraffic(this),
                MonitoringUtils.getMenuPredictTrafficFromSameElementMonitorInfo (this),
                MonitoringUtils.getMenuForecastDemandTrafficUsingGravityModel (this),
                MonitoringUtils.getMenuForecastDemandTrafficFromLinkInfo (this),
                new AjtRcMenu("Remove all monitored/forecast stored information", e->getSelectedElements().forEach(dd->((Link)dd).getMonitoredOrForecastedCarriedTraffic().removeAllValues()) , (a,b)->b>0, null),
                new AjtRcMenu("Remove monitored/forecast stored information...", null , (a,b)->b>0, Arrays.asList(
                        MonitoringUtils.getMenuRemoveMonitorInfoBeforeAfterDate (this , true) ,
                        MonitoringUtils.getMenuRemoveMonitorInfoBeforeAfterDate (this , false)
                		))
        		)));
        

        return res;
    }
    
    
    static class FullMeshTopology
    {
        private final GUINetworkDesign callback;
        private final boolean euclidean;
        private AdvancedJTable_networkElement table;
        
        public FullMeshTopology(AdvancedJTable_networkElement table , GUINetworkDesign callback, boolean euclidean)
        {
            this.callback = callback;
            this.euclidean = euclidean;
            this.table = table;
            create();
        }

        public void create()
        {
            assert callback != null;

            NetPlan netPlan = callback.getDesign();

            // Ask for current element removal
            if (netPlan.hasLinks(table.getTableNetworkLayer ()))
            {
                final int answer = JOptionPane.showConfirmDialog(null, "Remove all existing links?", "", JOptionPane.YES_NO_CANCEL_OPTION);
                if (answer == JOptionPane.CANCEL_OPTION || answer == JOptionPane.CLOSED_OPTION) return;
                if (answer == JOptionPane.OK_OPTION) netPlan.removeAllLinks(table.getTableNetworkLayer ());
            }

            for (long nodeId_1 : netPlan.getNodeIds())
            {
                for (long nodeId_2 : netPlan.getNodeIds())
                {
                    if (nodeId_1 >= nodeId_2) continue;
                    Node n1 = netPlan.getNodeFromId(nodeId_1);
                    Node n2 = netPlan.getNodeFromId(nodeId_2);

                    Pair<Link, Link> out = netPlan.addLinkBidirectional(n1, n2, 0, euclidean ? netPlan.getNodePairEuclideanDistance(n1, n2) : netPlan.getNodePairHaversineDistanceInKm(n1, n2), 200000, null);
                }
            }
            callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
            callback.updateVisualizationAfterChanges();
            callback.addNetPlanChange();
        }

    }


    
}
