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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.io.excel.ExcelReader;
import com.net2plan.gui.plugins.networkDesign.io.excel.ExcelWriter;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.MtnDialogBuilder;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.MtnInputForDialog;
import com.net2plan.gui.utils.StringLabeller;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.libraries.TrafficMatrixForecastUtils;
import com.net2plan.libraries.TrafficMatrixForecastUtils.TmEstimationResults;
import com.net2plan.libraries.TrafficMatrixGenerationModels;
import com.net2plan.libraries.TrafficSeries;
import com.net2plan.libraries.TrafficSeries.FITTINGTYPE;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.SwingUtils;

import cern.colt.matrix.tdouble.DoubleMatrix2D;
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
        res.add(new AjtRcMenu("Generate full-mesh (link length as Euclidean distance)", e->new FullMeshTopologyActionListener(this , callback, true), (a,b)->true, null));
        res.add(new AjtRcMenu("Generate full-mesh (link length as Haversine distance)", e->new FullMeshTopologyActionListener(this , callback, false), (a,b)->true, null));
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
            MtnDialogBuilder.launch(
                    "Set selected links capacity" , 
                    "Please introduce the link capacity. Negative values are not allowed. The capacity will be assigned to not coupled links", 
                    "", 
                    this, 
                    Arrays.asList(MtnInputForDialog.inputTfDouble("Link capacity (" + getTableNetworkLayer().getLinkCapacityUnits() + ")", "Introduce the link capacity", 10, 0.0)),
                    (list)->
                    	{
                    		final double newLinkCapacity = (Double) list.get(0).get();
                    		getSelectedElements().stream().filter(ee->!ee.isCoupled()).forEach(ee->ee.setCapacity(newLinkCapacity));
                    	}
                    );
        } , (a,b)->b>0, null));

        
        res.add(new AjtRcMenu("Set selected links capacity to match a given utilization", e->
        {
            MtnDialogBuilder.launch(
                    "Set selected links capacity to match utilization" , 
                    "Please introduce the link target utilization. Negative values are not allowed. The capacity will be assigned to not coupled links", 
                    "", 
                    this, 
                    Arrays.asList(MtnInputForDialog.inputTfDouble("Link utilization", "Introduce the link utilization", 10, 0.5)),
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
            MtnDialogBuilder.launch(
                    "Set selected links length (km)" , 
                    "Please introduce the link length. Negative values are not allowed. The length will be assigned to not coupled links", 
                    "", 
                    this, 
                    Arrays.asList(MtnInputForDialog.inputTfDouble("Link length (km)", "Introduce the link length", 10, 0.0)),
                    (list)->
                    	{
                    		final double newLinkLength = (Double) list.get(0).get();
                    		getSelectedElements().stream().filter(ee->!ee.isCoupled()).forEach(ee->ee.setLengthInKm(newLinkLength));
                    	}
                    );
        } , (a,b)->b>0, null));

        res.add(new AjtRcMenu("Scale selected links length", e->
        {
            MtnDialogBuilder.launch(
                    "Scale selected links length (km)" , 
                    "Please introduce the scaling factor for which the link lengths will be multiplied. Negative values are not allowed. The length will be assigned to not coupled links", 
                    "", 
                    this, 
                    Arrays.asList(MtnInputForDialog.inputTfDouble("Scaling factor", "Introduce the scaling factor", 10, 1.0)),
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
                AdvancedJTable_link.getMenuAddSyntheticMonitoringInfo (this),
                AdvancedJTable_link.getMenuExportMonitoringInfo(this),
                AdvancedJTable_link.getMenuImportMonitoringInfo (this),
                AdvancedJTable_link.getMenuSetMonitoredTraffic(this),                
                AdvancedJTable_link.getMenuPredictTrafficFromSameElementMonitorInfo (this),
                AdvancedJTable_link.getMenuForecastDemandTrafficUsingGravityModel (this),
                AdvancedJTable_link.getMenuForecastDemandTrafficFromLinkInfo (this),
                new AjtRcMenu("Remove all monitored/forecast stored information", e->getSelectedElements().forEach(dd->((Link)dd).getMonitoredOrForecastedCarriedTraffic().removeAllValues()) , (a,b)->b>0, null),
                new AjtRcMenu("Remove monitored/forecast stored information...", null , (a,b)->b>0, Arrays.asList(
                		AdvancedJTable_link.getMenuRemoveMonitorInfoBeforeAfterDate (this , true) , 
                		AdvancedJTable_link.getMenuRemoveMonitorInfoBeforeAfterDate (this , false) 
                		))
        		)));
        

        return res;
    }
    
    
    static class FullMeshTopologyActionListener implements ActionListener
    {
        private final GUINetworkDesign callback;
        private final boolean euclidean;
        private AdvancedJTable_networkElement table;
        
        public FullMeshTopologyActionListener(AdvancedJTable_networkElement table , GUINetworkDesign callback, boolean euclidean)
        {
            this.callback = callback;
            this.euclidean = euclidean;
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e)
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

    public static <T extends NetworkElement>  AjtRcMenu getMenuAddSyntheticMonitoringInfo (AdvancedJTable_networkElement<T> table)
    {
    	final boolean isLinkTable =  table.getAjType() == AJTableType.LINKS;
    	final boolean isDemandTable =  table.getAjType() == AJTableType.DEMANDS;
    	final boolean isMDemandTable =  table.getAjType() == AJTableType.MULTICAST_DEMANDS;
    	final String elementName = isLinkTable? "link" : (isDemandTable? "demand" : "multicast demand");
    	final SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    	
    	if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
        return new AjtRcMenu("Add synthetic monitoring trace to selected links", e->
        {
            MtnDialogBuilder.launch(
                    "Add monitoring trace to selected " + elementName , 
                    "This option permits creating in selected elements, a trace of monitored values", 
                    "", 
                    table, 
                    Arrays.asList(
                    		MtnInputForDialog.inputTfString("Initial GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the initial date in the indicated format", 10, dateFormatGmt.format(new Date ())), 
                    		MtnInputForDialog.inputTfDouble("Interval between samples (seconds)", "Introduce intreval (in seconds) between consecutive smaples", 10, 3600.0),
                    		MtnInputForDialog.inputTfInt("Number of samples", "Introduce the number of monitoring samples to create", 10, 7*24),
                    		MtnInputForDialog.inputCheckBox(isLinkTable? "Use current carried traffic as initial traffic?" : "Use current offered traffic as initial traffic?", "If selected, the current traffic value is used as initial traffic", true , null),
                    		MtnInputForDialog.inputTfDouble("Starting traffic", "Introduce the initial traffic (in demand traffic units: '" + table.getTableNetworkLayer().getDemandTrafficUnits() + "')", 10, 1.0),
                    		MtnInputForDialog.inputTfCombo("Growth type", "The growth traffic type. Exponential means that traffic at year T+1 is GF times the traffic at year T, where GF is the growth factor. If linear, traffic at year T+1 is the traffic at T plus GF", 20 , TrafficSeries.FITTINGTYPE.LINEAR , Arrays.asList(TrafficSeries.FITTINGTYPE.values()) , Arrays.asList(TrafficSeries.FITTINGTYPE.values()).stream().map(ee->ee.getName ()).collect (Collectors.toList()) , null),
                    		MtnInputForDialog.inputTfDouble("Growth factor (per year)", "If exponential growth, this is the compound annual growth rate (CAGR) (adimensional value), if linear growth, this is the traffic growth per year in traffic units" + table.getTableNetworkLayer().getDemandTrafficUnits() + "')", 10, 1.0),
                    		MtnInputForDialog.inputTfDouble("Noise coefficient of variation", "A normal traffic noise centered in 0 and typical deviation given by this value multiplied by the current traffic, is added to the estimation. Note that negative traffics are later truncated to zero", 10, 1.0),
                    		MtnInputForDialog.inputCheckBox("Remove previous monitoring values?", "If selected, the current monitored values are removed", true , null)
                    		),
                    (list)->
                    	{
                    		final Date initialDate;
                    		try { initialDate = dateFormatGmt.parse((String) list.get(0).get()); } catch (Exception exc) { throw new Net2PlanException ("Wrong date format"); } 
                    		final long intervalBetweenSamplesInSeconds = ((Double) list.get(1).get()).longValue();
                    		final int numberOfSamples = (Integer) list.get(2).get();
                    		final boolean useCarriedTrafficAsInitial = (Boolean) list.get(3).get(); 
                    		final double initialTrafficIfNotCurrent = (Double) list.get(4).get();
                    		final FITTINGTYPE growthType = (FITTINGTYPE) list.get(5).get();
                    		final double growthFactorPerYear = (Double) list.get(6).get();
                    		final double noiseRelativeTypicalDeviationRespectToAverage = (Double) list.get(7).get();
                    		final boolean removePreviousMonitValues = (Boolean) list.get(8).get(); 
                    		
                    		/* Remove previous */
                    		if (removePreviousMonitValues) table.getSelectedElements().forEach(ee->
                    			{
                    				if (isLinkTable) ((Link) ee).getMonitoredOrForecastedCarriedTraffic().removeAllValues();
                    				else if (isDemandTable) ((Demand) ee).getMonitoredOrForecastedOfferedTraffic().removeAllValues();
                    				else if (isMDemandTable) ((MulticastDemand) ee).getMonitoredOrForecastedOfferedTraffic().removeAllValues();
                    			}
                    		);

                			/* Add values */
                			for (T ee: table.getSelectedElements())
                			{
                				Double initialTraffic = null;
                				if (!useCarriedTrafficAsInitial)
                					initialTraffic = initialTrafficIfNotCurrent;
                				else
                				{
                    				if (isLinkTable) initialTraffic = ((Link) ee).getCarriedTraffic();
                    				else if (isDemandTable) initialTraffic = ((Demand) ee).getOfferedTraffic();
                    				else if (isMDemandTable) initialTraffic = ((MulticastDemand) ee).getOfferedTraffic();
                				}
                				assert initialTraffic != null;
                				TrafficSeries tm = null;
                				if (isLinkTable) tm = ((Link) ee).getMonitoredOrForecastedCarriedTraffic();
                				else if (isDemandTable) tm = ((Demand) ee).getMonitoredOrForecastedOfferedTraffic();
                				else if (isMDemandTable) tm = ((MulticastDemand) ee).getMonitoredOrForecastedOfferedTraffic();
                				assert tm != null;
                				
                				tm.addSyntheticMonitoringTrace(growthType, initialDate, intervalBetweenSamplesInSeconds, 
                						numberOfSamples, 
                						initialTraffic, 
                						growthFactorPerYear, noiseRelativeTypicalDeviationRespectToAverage);
                			}

                    	}
                    );
        } , (a,b)->b>0, null);
    }

    public static <T extends NetworkElement>  AjtRcMenu getMenuExportMonitoringInfo (AdvancedJTable_networkElement<T> table)
    {
    	final boolean isLinkTable =  table.getAjType() == AJTableType.LINKS;
    	final boolean isDemandTable =  table.getAjType() == AJTableType.DEMANDS;
    	final boolean isMDemandTable =  table.getAjType() == AJTableType.MULTICAST_DEMANDS;
    	final String elementName = isLinkTable? "link" : (isDemandTable? "demand" : "multicast demand");
    	if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
        return new AjtRcMenu("Export monitoring/forecast values to Excel", e->
        {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            FileFilter xlsFilter = new FileNameExtensionFilter("Excel 2003 file (*.xls)", "xls");
            FileFilter xlsxFilter = new FileNameExtensionFilter("Excel 2007 file (*.xlsx)", "xlsx");
            fileChooser.addChoosableFileFilter(xlsFilter);
            fileChooser.addChoosableFileFilter(xlsxFilter);
            final int result = fileChooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION)
            {
                final File file = SwingUtils.getSelectedFileWithExtension(fileChooser);
                if (file.exists())
                {
                    int option = JOptionPane.showConfirmDialog(null, "File already exists.\nOverwrite?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (option == JOptionPane.YES_OPTION)
                        file.delete();
                    else
                        return;
                }
                final int nLinks = table.getSelectedElements().size();
                final List<T> selectedElements = new ArrayList<> (table.getSelectedElements());
                final List<Date> allDates = new ArrayList<> (table.getSelectedElements().stream().map(ee->
                {
    				if (isLinkTable) return ((Link) ee).getMonitoredOrForecastedCarriedTraffic().getValues().keySet();
    				else if (isDemandTable) return ((Demand) ee).getMonitoredOrForecastedOfferedTraffic().getValues().keySet();
    				else if (isMDemandTable) return ((MulticastDemand) ee).getMonitoredOrForecastedOfferedTraffic().getValues().keySet();
    				return null;
                }).filter(ee->ee != null).flatMap(ee->ee.stream()).collect(Collectors.toCollection(TreeSet::new)));
                final int nDates = allDates.size();
                final Object[][] tableData = new Object[nDates + 1][nLinks + 1];
                /* */
                tableData [0][0] = "Link Id";
                for (int contDate = 0; contDate < nDates ; contDate ++) 
                	tableData [1+contDate][0] = allDates.get(contDate);
                for (int contLink = 0; contLink < nLinks ; contLink ++)
                {
                	final T element = selectedElements.get(contLink);
                	tableData [0][1+contLink] = element.getId();
                    for (int contDate = 0; contDate < nDates ; contDate ++) 
                    {
                    	final Date date = allDates.get(contDate);
                    	Double monitInfo = null;
        				if (isLinkTable) monitInfo = ((Link) element).getMonitoredOrForecastedCarriedTraffic().getValueOrNull(date);
        				else if (isDemandTable) monitInfo = ((Demand) element).getMonitoredOrForecastedOfferedTraffic().getValueOrNull(date);
        				else if (isMDemandTable) monitInfo = ((MulticastDemand) element).getMonitoredOrForecastedOfferedTraffic().getValueOrNull(date);
                    	if (monitInfo != null) tableData [1+contDate][1+contLink] = monitInfo.doubleValue();
                    }
                }	
                final String layerName = table.getTableNetworkLayer().getName().equals("")? "Layer " + table.getTableNetworkLayer().getIndex() : table.getTableNetworkLayer().getName();
                ExcelWriter.writeToFile(file, layerName + " - Link monit info", tableData);
            }
        } , (a,b)->b>0, null);

    }

    
    public static <T extends NetworkElement>  AjtRcMenu getMenuImportMonitoringInfo (AdvancedJTable_networkElement<T> table)
    {
    	final boolean isLinkTable =  table.getAjType() == AJTableType.LINKS;
    	final boolean isDemandTable =  table.getAjType() == AJTableType.DEMANDS;
    	final boolean isMDemandTable =  table.getAjType() == AJTableType.MULTICAST_DEMANDS;
    	final String elementName = isLinkTable? "link" : (isDemandTable? "demand" : "multicast demand");
    	if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
    	
    	return new AjtRcMenu("Import monitoring/forecast values from Excel", e->
        {
        	final NetPlan np = table.getTableNetworkLayer().getNetPlan();
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            FileFilter xlsFilter = new FileNameExtensionFilter("Excel 2003 file (*.xls)", "xls");
            FileFilter xlsxFilter = new FileNameExtensionFilter("Excel 2007 file (*.xlsx)", "xlsx");
            fileChooser.addChoosableFileFilter(xlsFilter);
            fileChooser.addChoosableFileFilter(xlsxFilter);
            final int result = fileChooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION)
            {
                final File file = SwingUtils.getSelectedFileWithExtension(fileChooser);
                if (!file.exists()) throw new Net2PlanException ("File does not exist");
                final Map<String, Object[][]> sheetName2Values = ExcelReader.readFile(file);
                for (Object [][] thisSheetInfo : sheetName2Values.values())
                {
                	if (thisSheetInfo.length <= 1) continue;
                	final int nCols = thisSheetInfo [0].length;
                	final int nRows = thisSheetInfo.length;
                	/* Get columns */
                	final SortedMap<Date , Integer> dates2RowIndex = new TreeMap<> ();
                	for (int contRow = 1; contRow < nRows ; contRow ++)
                	{
                		try
                		{
                    		final Date rowDate = (Date) thisSheetInfo [contRow][0];
                    		dates2RowIndex.put(rowDate, contRow);
                		} catch (Exception ee) {}
                	}
                	/* Fill info for links  */
                	for (int contCol = 1; contCol < nCols ; contCol ++)
                	{
                		final long neId;
                		NetworkElement ne = null;
                		try
                		{
                			final Object cell = thisSheetInfo [0][contCol];
                			if (cell instanceof Number)
                				neId = ((Number) cell).longValue();
                			else if (cell instanceof String)
                				neId = Long.parseLong((String) cell);
                			else throw new RuntimeException ();
            				if (isLinkTable) ne = np.getLinkFromId (neId);
            				else if (isDemandTable) ne = np.getDemandFromId (neId);
            				else if (isMDemandTable) ne = np.getMulticastDemandFromId (neId);
            				if (ne == null) continue;
                		} catch (Exception ee) { continue; }
                		for (Entry<Date,Integer> dateInfo : dates2RowIndex.entrySet())
                		{
                    		try
                    		{
	                			final Date date = dateInfo.getKey ();
	                			final int rowExcel = dateInfo.getValue();
	                			final Object cell = thisSheetInfo [rowExcel][contCol];
	                			final double val;
	                			if (cell instanceof Number) val = ((Number) cell).doubleValue();
	                			else if (cell instanceof String) val = Double.parseDouble((String) cell);
	                			else throw new RuntimeException ();
	            				if (isLinkTable) ((Link) ne).getMonitoredOrForecastedCarriedTraffic().addValue(date, val);
	            				else if (isDemandTable) ((Demand) ne).getMonitoredOrForecastedOfferedTraffic().addValue(date, val);
	            				else if (isMDemandTable) ((MulticastDemand) ne).getMonitoredOrForecastedOfferedTraffic().addValue(date, val);
                    		} catch (Exception ee) {}
                		}
                	}
                }
            }
        } , (a,b)->true, null);
    	
    }

    
    
    public static <T extends NetworkElement>  AjtRcMenu getMenuSetMonitoredTraffic (AdvancedJTable_networkElement<T> table)
    {
    	final boolean isLinkTable =  table.getAjType() == AJTableType.LINKS;
    	final boolean isDemandTable =  table.getAjType() == AJTableType.DEMANDS;
    	final boolean isMDemandTable =  table.getAjType() == AJTableType.MULTICAST_DEMANDS;
    	final String elementName = isLinkTable? "link" : (isDemandTable? "demand" : "multicast demand");
    	if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
    	final SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		return new AjtRcMenu("Set monitored traffic for selected elements", e->
                {
                	final Calendar now = Calendar.getInstance();
                    MtnDialogBuilder.launch(
                            "Select the traffic value, and the target date of the monitoring", 
                            "Please introduce the traffic amount and the date in which the traffic will be stored as monitored traffic.", 
                            "", 
                            table, 
                            Arrays.asList(
                            		MtnInputForDialog.inputTfString("Introducce the GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the GMT date in the indicated format", 10, dateFormatGmt.format(new Date ())), 
                            		MtnInputForDialog.inputTfDouble("Traffic (" + table.getTableNetworkLayer().getDemandTrafficUnits() + ")", "Introduce the traffic in the indicated units", 10, 0.0),
                            		MtnInputForDialog.inputCheckBox("Use element current traffic?", "If selected, the current element traffic value is used, instead of the traffic amount indicated by the user", true , null)
                            		),
                            (list)->
                            	{
                            		final Date date;
                            		try { date = dateFormatGmt.parse((String) list.get(0).get()); } catch (Exception exc) { throw new Net2PlanException ("Wrong date format"); } 
                            		final boolean useCurrentElementTraffic = (Boolean) list.get(1).get();
                            		final double trafficConstantUserDefined = (Double) list.get(2).get();
                            		if (isDemandTable)
                            			table.getSelectedElements().forEach(d->((Demand)d).getMonitoredOrForecastedOfferedTraffic().addValue(date, useCurrentElementTraffic? ((Demand)d).getOfferedTraffic(): trafficConstantUserDefined));
                            		else if (isMDemandTable)
                            			table.getSelectedElements().forEach(d->((MulticastDemand)d).getMonitoredOrForecastedOfferedTraffic().addValue(date, useCurrentElementTraffic? ((MulticastDemand)d).getOfferedTraffic(): trafficConstantUserDefined));
                            		else if (isLinkTable)
                            			table.getSelectedElements().forEach(d->((Link)d).getMonitoredOrForecastedCarriedTraffic().addValue(date, useCurrentElementTraffic? ((Link)d).getCarriedTraffic(): trafficConstantUserDefined));
                            	}
                            );
                }
                , (a,b)->b>0, null);
    	
    }

    
    public static <T extends NetworkElement>  AjtRcMenu getMenuPredictTrafficFromSameElementMonitorInfo (AdvancedJTable_networkElement<T> table)
    {
    	final boolean isLinkTable =  table.getAjType() == AJTableType.LINKS;
    	final boolean isDemandTable =  table.getAjType() == AJTableType.DEMANDS;
    	final boolean isMDemandTable =  table.getAjType() == AJTableType.MULTICAST_DEMANDS;
    	if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
    	final SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		return new AjtRcMenu("Forecast selected elements traffic from monitor info", e->
                {
                	final Calendar now = Calendar.getInstance();
                	
                    MtnDialogBuilder.launch(
                            "Select the traffic value, and the target date of the monitoring", 
                            "Please introduce the date to predict, and statistical parameters.", 
                            "", 
                            table, 
                            Arrays.asList(
                            		MtnInputForDialog.inputTfString("Introducce the GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the GMT date in the indicated format", 10, dateFormatGmt.format(new Date ())), 
                            		MtnInputForDialog.inputTfCombo("Fitting type", "The fitting type to use to predict the traffic growth evolution. Exponential means that traffic is fitted to an exponential function of time, linear to a linear function of time", 20 , TrafficSeries.FITTINGTYPE.LINEAR , Arrays.asList(TrafficSeries.FITTINGTYPE.values()) , Arrays.asList(TrafficSeries.FITTINGTYPE.values()).stream().map(ee->ee.getName ()).collect (Collectors.toList()) , null),
                            		MtnInputForDialog.inputTfDouble("Probability of underestimation", "The predicted traffic will be such that the probability of the traffic to be higher that the prediction is the given probabilty. A value of 0.5 provides an unbiased (neither conservative nor optimistic) estimation", 10, 0.05),
                            		MtnInputForDialog.inputTfCombo("Save in...", "Indicates where the prediction will be stored", 20 , "As new monitoring sample" , isLinkTable? Arrays.asList("As new monitoring sample") : Arrays.asList("As new monitoring sample" , "As current demand offered traffic") , isLinkTable? Arrays.asList("As new monitoring sample") : Arrays.asList("As new monitoring sample" , "As current demand offered traffic") , null)
                            		),
                            (list)->
                            	{
                            		final SortedMap<T, Double> varianceExplained = new TreeMap<> ();
                            		final Date date;
                            		try { date = dateFormatGmt.parse((String) list.get(0).get()); } catch (Exception exc) { throw new Net2PlanException ("Wrong date format"); } 
                            		final FITTINGTYPE fittingType = (FITTINGTYPE) list.get(1).get();
                            		final double probSubestimation = (Double) list.get(2).get();
                            		final boolean storeAsNewSample = ((String) list.get(3).get()).equals("As new monitoring sample");
                            		if (probSubestimation <= 0 || probSubestimation >= 1) throw new Net2PlanException ("Wrong value of probability");
                            		for (T ee : table.getSelectedElements())
                            		{
                            			final TrafficSeries tm = isDemandTable? ((Demand)ee).getMonitoredOrForecastedOfferedTraffic() : ((MulticastDemand) ee).getMonitoredOrForecastedOfferedTraffic();
                            			if (tm.getSize() < 3) throw new Net2PlanException ("Not enough data to make the analysis for element of index: " + ee.getIndex());
                            		}
                            		for (T ee : table.getSelectedElements())
                            		{
                            			final TrafficSeries tm = isDemandTable? ((Demand)ee).getMonitoredOrForecastedOfferedTraffic() : ((MulticastDemand) ee).getMonitoredOrForecastedOfferedTraffic();
                            			final TrafficSeries.TrafficPredictor tp = tm.getFunctionPredictionSoProbSubestimationIsBounded(fittingType);
                            			final double val = tp.getPredictorFunction(probSubestimation).apply(date);
                            			varianceExplained.put(ee, tp.getRegResuls().getRSquared());
                            			if (storeAsNewSample) tm.addValue(date, val);
                            			else
                            			if (isDemandTable) ((Demand)ee).setOfferedTraffic(val); else ((MulticastDemand)ee).setOfferedTraffic(val);
                            		}
                            		final String RETURN = String.format("%n");
                            		final DecimalFormat df = new DecimalFormat("#.##"); 
                            		final double minR2 = varianceExplained.values().stream().mapToDouble(ee->ee).min().orElse(0);
                            		final double maxR2 = varianceExplained.values().stream().mapToDouble(ee->ee).max().orElse(0);
                            		final double avR2 = varianceExplained.isEmpty()? 0.0 : varianceExplained.values().stream().mapToDouble(ee->ee).sum() / ((double) varianceExplained.size());
                            		final String message = "Number of elements: " + table.getSelectedElements().size() + RETURN + "% of variance explained in each demand [MIN / AVG / MAX]" + (fittingType.isExponential()? " (applied to log(traffic))" : "") + ": [" + df.format(minR2) + " / " + df.format(avR2) + " / " + df.format(maxR2); 
                            		JOptionPane.showMessageDialog(null, message , "Output info", JOptionPane.INFORMATION_MESSAGE);
                            	}
                            );
                }
                , (a,b)->b>0, null);
    	
    }

    public static <T extends NetworkElement>  AjtRcMenu getMenuRemoveMonitorInfoBeforeAfterDate (AdvancedJTable_networkElement<T> table , boolean beforeTrueAfterFalse)
    {
    	final boolean isLinkTable =  table.getAjType() == AJTableType.LINKS;
    	final boolean isDemandTable =  table.getAjType() == AJTableType.DEMANDS;
    	final boolean isMDemandTable =  table.getAjType() == AJTableType.MULTICAST_DEMANDS;
    	if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
    	final SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		return new AjtRcMenu(beforeTrueAfterFalse? "before date..." : "after date..." , e->
                {
                	final Calendar now = Calendar.getInstance();
                    MtnDialogBuilder.launch(
                            "Select the limit date for removing (including that date)", 
                            "Please introduce the limit date for removing the monitoring info (info at that specfic date will be also removed).", 
                            "", 
                            table, 
                            Arrays.asList(
                            		MtnInputForDialog.inputTfString("Introducce the GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the GMT date in the indicated format", 10, dateFormatGmt.format(new Date ()))
                            		),
                            (list)->
                            	{
                            		final Date date;
                            		try { date = dateFormatGmt.parse((String) list.get(0).get()); } catch (Exception exc) { throw new Net2PlanException ("Wrong date format"); } 
                            		if (isDemandTable)
                            		{
                            			if (beforeTrueAfterFalse)
                            				table.getSelectedElements().forEach(d->((Demand)d).getMonitoredOrForecastedOfferedTraffic().removeAllValuesBeforeOrEqual(date));
                            			else
                            				table.getSelectedElements().forEach(d->((Demand)d).getMonitoredOrForecastedOfferedTraffic().removeAllValuesAfterOrEqual(date));
                            		}
                            		else if (isMDemandTable)
                            		{
                            			if (beforeTrueAfterFalse)
                            				table.getSelectedElements().forEach(d->((MulticastDemand)d).getMonitoredOrForecastedOfferedTraffic().removeAllValuesBeforeOrEqual(date));
                            			else
                            				table.getSelectedElements().forEach(d->((MulticastDemand)d).getMonitoredOrForecastedOfferedTraffic().removeAllValuesAfterOrEqual(date));
                            		}
                            		else if (isLinkTable)
                            		{
                            			if (beforeTrueAfterFalse)
                            				table.getSelectedElements().forEach(d->((Link)d).getMonitoredOrForecastedCarriedTraffic().removeAllValuesBeforeOrEqual(date));
                            			else
                            				table.getSelectedElements().forEach(d->((Link)d).getMonitoredOrForecastedCarriedTraffic().removeAllValuesAfterOrEqual(date));
                            		}
                            	}
                            );
                }
                , (a,b)->b>0, null);
    	
    }

    
    public static <T extends NetworkElement>  AjtRcMenu getMenuForecastDemandTrafficUsingGravityModel (AdvancedJTable_networkElement<T> table)
    {
    	final SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    	
		return new AjtRcMenu("Forecast demands traffic using gravity model...", null , (a,b)->true, Arrays.asList(
				new AjtRcMenu("from monitored traffic", e->
                {
                	final NetworkLayer layer = table.getTableNetworkLayer();
                	final NetPlan np = layer.getNetPlan();
                	final List<Link> links = np.getLinks(layer);
                	final List<Demand> demands = np.getDemands(layer);
                	if (links.isEmpty()) throw new Net2PlanException("No links in this layer");
                	if (np.hasMulticastDemands(layer)) throw new Net2PlanException("The link cannot have multicast demands");
                	if (demands.stream().map(d->d.getIngressNode()).anyMatch(n->n.getOutgoingLinks(layer).isEmpty())) throw new Net2PlanException ("A demand origin node has no output links");
                	if (demands.stream().map(d->d.getEgressNode()).anyMatch(n->n.getIncomingLinks(layer).isEmpty())) throw new Net2PlanException ("A demand end node has no input links");
                	final SortedSet<Date> datesWithEnoughInformationFromGM = TrafficMatrixForecastUtils.getDatesWhereGravityModelCanBeApplied (layer);
                	if (datesWithEnoughInformationFromGM.isEmpty()) throw new Net2PlanException ("No dates exist with enough monitoring indformation to apply the gravity model");
                    MtnDialogBuilder.launch(
                    		"Forecast demands traffic using gravity model",
                            "Please indicate the requested parameters. The demands offered traffic will be estimated according to link monitored carried "
                            + "traffic values of the given date, and stored in the same date. Only dates where enough monitoring information in the links exist are acceptable for this method. "
                            + "The estimations will be made for all the dates with enough information in the provided range", 
                            "", 
                            table, 
                            Arrays.asList(
                            		MtnInputForDialog.inputTfString("Introduce the initial GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the GMT date in the indicated format", 10, dateFormatGmt.format(datesWithEnoughInformationFromGM.first())), 
                            		MtnInputForDialog.inputTfString("Introduce the end GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the GMT date in the indicated format", 10, dateFormatGmt.format(datesWithEnoughInformationFromGM.last())) 
                            		),
                            (list)->
                            	{
                            		final Date initialDate, endDate;
                            		try { initialDate = dateFormatGmt.parse((String) list.get(0).get()); } catch (Exception exc) { throw new Net2PlanException ("Wrong date format"); }
                            		try { endDate = dateFormatGmt.parse((String) list.get(1).get()); } catch (Exception exc) { throw new Net2PlanException ("Wrong date format"); }
                            		final SortedSet<Date> datesToApplyEstimation = datesWithEnoughInformationFromGM.stream().filter(d->!d.before(initialDate) && !d.after(endDate)).collect(Collectors.toCollection(TreeSet::new));
                            		if (datesToApplyEstimation.isEmpty()) throw new Net2PlanException ("No dates are selected, no gravity model estimations are performed");
                            		for (Date date : datesToApplyEstimation)
                            		{
                                    	final SortedMap<Demand,Double> estimTrafficGm = TrafficMatrixForecastUtils.getGravityModelEstimationFromMonitorTraffic(layer , date);
                                    	estimTrafficGm.entrySet().forEach(ee->ee.getKey().getMonitoredOrForecastedOfferedTraffic().addValue (date , ee.getValue()));
                            		}
                            		JOptionPane.showMessageDialog(null, "Gravity model applied to " + datesToApplyEstimation.size() + " dates", "Output info", JOptionPane.INFORMATION_MESSAGE);
                            	}
                            );
                }
                , (a,b)->true, null) , 
				
				new AjtRcMenu("from current link carried traffic", e->
                {
                	final NetworkLayer layer = table.getTableNetworkLayer();
                	final NetPlan np = layer.getNetPlan();
                	final List<Link> links = np.getLinks(layer);
                	final List<Demand> demands = np.getDemands(layer);
                	if (links.isEmpty()) throw new Net2PlanException("No links in this layer");
                	if (np.hasMulticastDemands(layer)) throw new Net2PlanException("The link cannot have multicast demands");
                	if (demands.stream().map(d->d.getIngressNode()).anyMatch(n->n.getOutgoingLinks(layer).isEmpty())) throw new Net2PlanException ("A demand origin node has no output links");
                	if (demands.stream().map(d->d.getEgressNode()).anyMatch(n->n.getIncomingLinks(layer).isEmpty())) throw new Net2PlanException ("A demand end node has no input links");
                	final SortedMap<Demand,Double> estimTrafficGm = TrafficMatrixForecastUtils.getGravityModelEstimationFromCurrentCarriedTraffic(layer);
                	estimTrafficGm.entrySet().forEach(ee->ee.getKey().setOfferedTraffic(ee.getValue()));
            		JOptionPane.showMessageDialog(null, "Gravity model corretly applied", "Output info", JOptionPane.INFORMATION_MESSAGE);
                }
                , (a,b)->true, null) 
				));
    }

    public static <T extends NetworkElement>  AjtRcMenu getMenuForecastDemandTrafficFromLinkInfo (AdvancedJTable_networkElement<T> table)
    {
    	final NetworkLayer layer = table.getTableNetworkLayer();
    	final NetPlan np = layer.getNetPlan();
    	
    	final boolean isLinkTable =  table.getAjType() == AJTableType.LINKS;
    	final boolean isDemandTable =  table.getAjType() == AJTableType.DEMANDS;
    	final boolean isMDemandTable =  table.getAjType() == AJTableType.MULTICAST_DEMANDS;
    	if (!isLinkTable && !isDemandTable && !isMDemandTable) throw new RuntimeException ();
    	final SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    	final SortedSet<Date> datesWihtAtLeastOneLinkMonitInfo = TrafficMatrixForecastUtils.getDatesWithAtLeastOneLinkMonitorInfo(layer);
    	final SortedSet<Date> datesWihtAtLeastOneDemandMonitInfo = TrafficMatrixForecastUtils.getDatesWithAtLeastOneUnicastDemandMonitorInfo(layer);
    	final SortedSet<Date> datesWihtAtLeastOneMDemandMonitInfo = TrafficMatrixForecastUtils.getDatesWithAtLeastOneMulticastDemandMonitorInfo(layer);
    	final SortedSet<Date> datesWithDemandMDemandOrLinkInfo = new TreeSet<> ();
    	datesWithDemandMDemandOrLinkInfo.addAll(datesWihtAtLeastOneLinkMonitInfo);
    	datesWithDemandMDemandOrLinkInfo.addAll(datesWihtAtLeastOneDemandMonitInfo);
    	datesWithDemandMDemandOrLinkInfo.addAll(datesWihtAtLeastOneMDemandMonitInfo);
		return new AjtRcMenu("Forecast demands traffic from link monitor info", e->
                {
                	if (datesWithDemandMDemandOrLinkInfo.isEmpty()) throw new Net2PlanException ("No monitoring information available");
                	final List<String> optionsInputDemandMonit = Arrays.asList(
                			"No input demand information" , 
                			"Use gravity model estimation from link monit." , 
                			"Same date demand monitoring info" , 
                			"Zero traffic");
                    MtnDialogBuilder.launch(
                    		"Forecast demands traffic from current link info",
                            "Please indicate the requested parameters. The demands and multicast demands offered traffic will be estimated according to current link monitored carried traffic values. "
                            + "Note that this may make the carried traffics to change", 
                            "", 
                            table, 
                            Arrays.asList(
                            		MtnInputForDialog.inputTfString("Introduce the initial GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the GMT date in the indicated format", 10, dateFormatGmt.format(new Date ())), 
                            		MtnInputForDialog.inputTfString("Introduce the end GMT date (format yyyy-MM-dd HH:mm:ss)", "Introduce the GMT date in the indicated format", 10, dateFormatGmt.format(new Date ())), 
                            		MtnInputForDialog.inputTfDouble("Balance between link monitoring vs. demands previous information", "The predicted traffic will be such that the probability of the traffic to be higher that the prediction is the given probabilty. A value of 0.5 provides an unbiased (neither conservative nor optimistic) estimation", 10, 0.5),
                            		MtnInputForDialog.inputTfCombo("Input of demand monitoring", "Where the demand monitoring information will come from", 20 , null , optionsInputDemandMonit , null , null),
                            		MtnInputForDialog.inputCheckBox("Apply only to dates with full link monitoring info", "If selected, the estimation will be applied only to those dates for which we have monitoring information for ALL the links", true , null)
                            		),
                            (list)->
                            	{
                            		final Date initialDate , endDate;
                            		try { initialDate = dateFormatGmt.parse((String) list.get(0).get()); } catch (Exception exc) { throw new Net2PlanException ("Wrong date format"); } 
                            		try { endDate = dateFormatGmt.parse((String) list.get(1).get()); } catch (Exception exc) { throw new Net2PlanException ("Wrong date format"); }
                            		if (endDate.before(initialDate)) throw new Net2PlanException ("End date cannot be before initial date");
                            		final double coeff_preferFitRouting0PreferFitDemand1 = (Double) list.get(2).get();
                            		if (coeff_preferFitRouting0PreferFitDemand1 < 0 || coeff_preferFitRouting0PreferFitDemand1 > 1) throw new Net2PlanException ("Wrong value of balance coefficient");
                            		final int indexSelectionInputDemandMonit = optionsInputDemandMonit.indexOf((String) list.get(3).get());
                            		final boolean applyOnlyForDatesWithFullLinkMonitInfo = (Boolean) list.get(3).get();
                            		assert indexSelectionInputDemandMonit != -1;
                            		final SortedSet<Date> datesToApplyEstimation = datesWithDemandMDemandOrLinkInfo.stream().
                            				filter(d->!d.before(initialDate) && !d.after(endDate)).
                            				filter(d->applyOnlyForDatesWithFullLinkMonitInfo? np.getLinks(layer).stream().allMatch(ee->ee.getMonitoredOrForecastedCarriedTraffic().hasValue(d)) : true).
                            				collect(Collectors.toCollection(TreeSet::new));
                            		if (indexSelectionInputDemandMonit == 1)
                            			datesToApplyEstimation.retainAll(TrafficMatrixForecastUtils.getDatesWhereGravityModelCanBeApplied(layer));
                            		if (datesToApplyEstimation.isEmpty()) throw new Net2PlanException ("No dates are eligible for traffic matrix estimations are performed");
                            		for (Date date : datesToApplyEstimation)
                            		{
                            			final SortedMap<Link,Double> inputMonitInfo_someLinks = new TreeMap<> (np.getLinks(layer).stream().filter(ee->ee.getMonitoredOrForecastedCarriedTraffic().hasValue(date)).collect(Collectors.toMap(ee->ee, ee->ee.getMonitoredOrForecastedCarriedTraffic().getValueOrNull(date))));
                            			final TmEstimationResults esimRes;
                                		if (indexSelectionInputDemandMonit == 0)
                                		{
                                			/* No demand information is used */
                                			esimRes = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, inputMonitInfo_someLinks, null, null, coeff_preferFitRouting0PreferFitDemand1);
                                		} else if (indexSelectionInputDemandMonit == 1)
                                		{
                                			/* Use gravity model */
                                			final SortedMap<Demand,Double> gravityModelEstim = TrafficMatrixForecastUtils.getGravityModelEstimationFromMonitorTraffic(layer, date);
                                			esimRes = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, inputMonitInfo_someLinks, gravityModelEstim, null, coeff_preferFitRouting0PreferFitDemand1);
                                		} else if (indexSelectionInputDemandMonit == 2)
                                		{
                                			/* Same date demand monitoring info */
                                			final Map<Demand,Double> demandEstim = np.getDemands(layer).stream().filter(ee->ee.getMonitoredOrForecastedOfferedTraffic().hasValue(date)).collect(Collectors.toMap(ee->ee, ee->ee.getMonitoredOrForecastedOfferedTraffic().getValueOrNull(date)));
                                			final Map<MulticastDemand,Double> mdemandEstim = np.getMulticastDemands(layer).stream().filter(ee->ee.getMonitoredOrForecastedOfferedTraffic().hasValue(date)).collect(Collectors.toMap(ee->ee, ee->ee.getMonitoredOrForecastedOfferedTraffic().getValueOrNull(date)));
                                			esimRes = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, inputMonitInfo_someLinks, demandEstim, mdemandEstim, coeff_preferFitRouting0PreferFitDemand1);
                                		} else if (indexSelectionInputDemandMonit == 3)
                                		{
                                			/* Demand is zero traffic */
                                			final Map<Demand,Double> demandEstim = np.getDemands(layer).stream().collect(Collectors.toMap(ee->ee, ee->0.0));
                                			final Map<MulticastDemand,Double> mdemandEstim = np.getMulticastDemands(layer).stream().collect(Collectors.toMap(ee->ee, ee->0.0));
                                			esimRes = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, inputMonitInfo_someLinks, demandEstim, mdemandEstim, coeff_preferFitRouting0PreferFitDemand1);
                                		} else throw new RuntimeException ();

                                		/* Store the information */
                                		for (Demand d : np.getDemands(layer)) 
                                			d.getMonitoredOrForecastedOfferedTraffic().addValue(date , esimRes.getEstimationDemand(d));
                                		for (MulticastDemand d : np.getMulticastDemands(layer)) 
                                			d.getMonitoredOrForecastedOfferedTraffic().addValue(date , esimRes.getEstimationMDemand(d));
                            		}                            		
                            		JOptionPane.showMessageDialog(null, "Estimation applied to " + datesToApplyEstimation.size() + " dates", "Output info", JOptionPane.INFORMATION_MESSAGE);
                            	}
                            );
                }
                , (a,b)->b>0, null);
    	
    }

    
}
