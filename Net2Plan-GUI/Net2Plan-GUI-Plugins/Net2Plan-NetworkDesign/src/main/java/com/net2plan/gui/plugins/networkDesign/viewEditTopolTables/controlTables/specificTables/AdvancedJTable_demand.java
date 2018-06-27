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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.MtnDialogBuilder;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.MtnInputForDialog;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.NetworkElementOrFr;
import com.net2plan.gui.utils.StringLabeller;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.StringUtils;

import net.miginfocom.swing.MigLayout;

/**
 */
@SuppressWarnings({ "unchecked", "serial" })
public class AdvancedJTable_demand extends AdvancedJTable_networkElement<Demand>
{
    public AdvancedJTable_demand(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.DEMANDS , layerThisTable , true , null);
    }

    @Override
  public List<AjtColumnInfo<Demand>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
      final List<AjtColumnInfo<Demand>> res = new LinkedList<> ();
      res.add(new AjtColumnInfo<Demand>(this , Node.class, null , "A", "Ingress node", null , d->d.getIngressNode() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Demand>(this , Node.class, null , "B", "Egress node", null , d->d.getEgressNode() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Demand>(this , Demand.class, null , "Bidirectional pair", "If the demand is bidirectional, provides its bidirectional pair", null , d->d.getBidirectionalPair() , AGTYPE.NOAGGREGATION, null));
      res.add(new AjtColumnInfo<Demand>(this , Link.class, null , "Link coupled", "The link that this demand is coupled to (in this or other layer)", null , d->d.getCoupledLink() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Offered traffic (" + getTableNetworkLayer().getLinkCapacityUnits() + ")", "Offered traffic by the demand", (d,val)->d.setOfferedTraffic((Double) val), d->d.getOfferedTraffic() , AGTYPE.SUMDOUBLE , null));
      res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Carried traffic (" + getTableNetworkLayer().getLinkCapacityUnits() + ")", "Carried traffic by the demand", null , d->d.getCarriedTraffic() , AGTYPE.SUMDOUBLE , null));
      res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "% Lost traffic", "Percentage of the lost traffic by the demand", null, d->d.getOfferedTraffic() == 0? 0 : d.getBlockedTraffic() / d.getOfferedTraffic() , AGTYPE.NOAGGREGATION , d->d.getBlockedTraffic() > 0? Color.RED : Color.GREEN));
      res.add(new AjtColumnInfo<Demand>(this , String.class, null , "QoS type", "A used-defined string identifying the type of traffic of the demand", (d,val)-> d.setQoSType((String)val) , d->d.getQosType(), AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Demand>(this , Boolean.class, null , "Source routing?", "", (d,val)->d.setRoutingType((Boolean) val? RoutingType.SOURCE_ROUTING : RoutingType.HOP_BY_HOP_ROUTING), d->d.isSourceRouting() , AGTYPE.COUNTTRUE , null));
      res.add(new AjtColumnInfo<Demand>(this , Boolean.class, null , "Is service chain?", "", null, d->d.isServiceChainRequest() , AGTYPE.COUNTTRUE , null));
      res.add(new AjtColumnInfo<Demand>(this , String.class, null , "Resource types", "The sequence of resource types that has to be traversed by the routes of the demand, if it is a service chain", null, d->d.isSourceRouting()? d.getServiceChainSequenceOfTraversedResourceTypes().stream().collect(Collectors.joining(",")) : "" , AGTYPE.COUNTTRUE , null));
      res.add(new AjtColumnInfo<Demand>(this , String.class, null , "Routing cycles", "Indicates whether there are routing cycles: loopless (no cycle in some route), open cycles (traffic reaches egress node after some cycles in some route), closed cycles (traffic does not reach the egress node in some route)", null, d->d.getRoutingCycleType().name() , AGTYPE.NOAGGREGATION , d->d.getRoutingCycleType() == RoutingCycleType.LOOPLESS? null : Color.ORANGE));
      res.add(new AjtColumnInfo<Demand>(this , String.class, null , "Bifurcated?", "Indicates whether the demand is satisfied by more than one path from origin to destination", null, d->!d.isSourceRouting() ? "-" : (d.isBifurcated()) ? String.format("Yes (%d)", d.getRoutes().size()) : "No" , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Demand>(this , Integer.class, null , "# routes", "Number of associated routes", null, d->!d.isSourceRouting() ? 0 : d.getRoutes().size() , AGTYPE.SUMINT, null));
      res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Worst e2e lat (ms)", "Current worst case end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any)", null, d->d.getWorstCasePropagationTimeInMs() , AGTYPE.NOAGGREGATION , d->{ final double maxMs = d.getMaximumAcceptableE2EWorstCaseLatencyInMs(); return maxMs <= 0? null : (d.getWorstCasePropagationTimeInMs() > maxMs? Color.RED : null); }));
      res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Worst e2e length (km)", "Current worst case end-to-end propagation length in km (accumulating any lower layer propagation lengths if any)", null, d->d.getWorstCaseLengthInKm() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "Limit e2e lat (ms)", "Maximum end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any)", (d,val)-> d.setMaximumAcceptableE2EWorstCaseLatencyInMs((Double)val) , d->d.getMaximumAcceptableE2EWorstCaseLatencyInMs() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Demand>(this , Double.class, null , "CAGR(%)" , "Compound annual growth factor for this demand", (d,val)->d.setOfferedTrafficPerPeriodGrowthFactor((Double) val), d->d.getOfferedTrafficPerPeriodGrowthFactor() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Demand>(this , Integer.class, null , "#Monit points" , "Number of samples of the offered traffic stored, coming from a monitoring or forecasting traffic process", null , d->d.getMonitoredOrForecastedOfferedTraffic().getSize() , AGTYPE.NOAGGREGATION , null));
      return res;
  }

    @Override
    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
    	final NetPlan np = callback.getDesign();
        final List<AjtRcMenu> res = new ArrayList<> ();
        res.add(new AjtRcMenu("Add demand", e->createLinkDemandGUI(NetworkElementType.DEMAND, getTableNetworkLayer () , callback), (a,b)->true, null));
        res.add(new AjtRcMenu("Remove selected demands", e->getSelectedElements().forEach(dd->((Demand)dd).remove()) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Set QoS type to selected demands", e->
        {
            MtnDialogBuilder.launch(
            		"Set selected demands QoS type", 
                    "Please introduce the QoS type.", 
                    "", 
                    this, 
                    Arrays.asList(MtnInputForDialog.inputTfString ("Qos type", "Introduce the QoS type of the demands" , 10 , "")),
                    (list)->
                    	{
                    		final String qos = (String) list.get(0).get();
                    		getSelectedElements().forEach(dd->dd.setQoSType(qos));
                    	}
                    );
        }, (a,b)->b>0, null));
        res.add(new AjtRcMenu("Set routing type of selected demands", e->
        {
            MtnDialogBuilder.launch(
            		"Set routing type", 
                    "Please introduce the QoS type. Source routing or hop-by-hop routing.", 
                    "", 
                    this, 
                    Arrays.asList(MtnInputForDialog.inputTfCombo ("Qos type", "Introduce the QoS type of the demands" , 10, RoutingType.SOURCE_ROUTING , 
                    		Arrays.asList(RoutingType.SOURCE_ROUTING , RoutingType.HOP_BY_HOP_ROUTING) , Arrays.asList("Source routing" , "Hop by hop routing") , (Consumer<RoutingType>) null)),
                    (list)->
                    	{
                    		final String qos = (String) list.get(0).get();
                    		getSelectedElements().forEach(dd->dd.setQoSType(qos));
                    	}
                    );
        }, (a,b)->b>0, null));
        res.add(new AjtRcMenu("Set maximum e2e limit to selected demands", e->
        {
            MtnDialogBuilder.launch(
            		"Set maximum e2e limit to selected demands", 
                    "Please introduce the maximum end-to-end limit in ms, to set for the selected demands.", 
                    "", 
                    this, 
                    Arrays.asList(MtnInputForDialog.inputTfDouble("Maximum end-to-end limit (ms)", "Introduce the maximum end-to-end limit in miliseconds", 10, 50.0)),
                    (list)->
                    	{
                    		final double newLimit = (Double) list.get(0).get();
                    		getSelectedElements().forEach(dd->((Demand)dd).setMaximumAcceptableE2EWorstCaseLatencyInMs(newLimit));
                    	}
                    );
        }, (a,b)->b>0, null));
        
        res.add(new AjtRcMenu("Add one source routing demand per selected node pair (all if none selected)", e->rcMenuFullMeshTraffic(true), (a,b)->true, null));
        res.add(new AjtRcMenu("Add one hop-by-hop routing demand per selected node pair (all if none selected)", e->rcMenuFullMeshTraffic(false), (a,b)->true, null));
        res.add(new AjtRcMenu("Set selected demands offered traffic", e ->
		{
            MtnDialogBuilder.launch(
                    "Set selected demands offered traffic", 
                    "Please introduce the offered traffic. Negative values are not allowed", 
                    "", 
                    this, 
                    Arrays.asList(MtnInputForDialog.inputTfDouble("Offered traffic (" + getTableNetworkLayer().getDemandTrafficUnits() + ")", "Introduce the offered traffic", 10, 0.0)),
                    (list)->
                    	{
                    		final double newOfferedTraffic = (Double) list.get(0).get();
                    		final List<Demand> changedDemands = getSelectedElements().stream().map(ee->(Demand)ee).collect(Collectors.toList());
                    		try
                    		{
                                if (callback.getVisualizationState().isWhatIfAnalysisActive())
                                    callback.getWhatIfAnalysisPane().whatIfDemandOfferedTrafficModified(changedDemands, Collections.nCopies(changedDemands.size(), newOfferedTraffic));
                                else
                                	changedDemands.forEach(d->d.setOfferedTraffic(newOfferedTraffic));
                    			
                    		} catch (Throwable ex) { ex.printStackTrace(); throw new Net2PlanException (ex.getMessage());  }
                    	}
                    );
		}
		, (a, b) -> b>0, null));
        res.add(new AjtRcMenu("Scale selected demands offered traffic", e ->
		{
            MtnDialogBuilder.launch(
                    "Scale selected demands offered traffic", 
                    "Please introduce the factor for which the offered traffic will be multiplied. Negative values are not allowed", 
                    "", 
                    this, 
                    Arrays.asList(MtnInputForDialog.inputTfDouble("Scaling factor", "Introduce the scaling factor", 10, 0.0)),
                    (list)->
                    	{
                    		final double neScalingFactor = (Double) list.get(0).get();
                    		final List<Demand> changedDemands = getSelectedElements().stream().map(ee->(Demand)ee).collect(Collectors.toList());
                    		try
                    		{
                                if (callback.getVisualizationState().isWhatIfAnalysisActive())
                                    callback.getWhatIfAnalysisPane().whatIfDemandOfferedTrafficModified(changedDemands, changedDemands.stream().map(d-> new Double ((d.getOfferedTraffic () * neScalingFactor))).collect(Collectors.toList()));
                                else
                                	changedDemands.forEach(d->d.setOfferedTraffic(d.getOfferedTraffic() * neScalingFactor));
                    		} catch (Throwable ex) { ex.printStackTrace(); throw new Net2PlanException (ex.getMessage());  }
                    	}
                    );
		}
		, (a, b) -> b>0, null));
        res.add(new AjtRcMenu("Set traversed resource types (to selected or all demands in the table)", e ->
		{
			final SortedSet<Demand> selectedDemands = getSelectedElements();
            String[] headers = StringUtils.arrayOf("Order", "Type");
            Object[][] data = {null, null};
            DefaultTableModel model = new ClassAwareTableModelImpl(data, headers);
            AdvancedJTable table = new AdvancedJTable(model);
            JButton addRow = new JButton("Add new traversed resource type");
            addRow.addActionListener(e1 ->
            {
                Object[] newRow = {table.getRowCount(), ""};
                ((DefaultTableModel) table.getModel()).addRow(newRow);
            });
            JButton removeRow = new JButton("Remove selected");
            removeRow.addActionListener(e12 ->
            {
                ((DefaultTableModel) table.getModel()).removeRow(table.getSelectedRow());
                for (int t = 0; t < table.getRowCount(); t++)
                    table.getModel().setValueAt(t, t, 0);
            });
            JButton removeAllRows = new JButton("Remove all");
            removeAllRows.addActionListener(e13 ->
            {
                while (table.getRowCount() > 0)
                    ((DefaultTableModel) table.getModel()).removeRow(0);
            });

            final Set<String> resourceTypes = np.getResources().stream().map(Resource::getType).collect(Collectors.toSet());
            final List<String> sortedResourceTypes = resourceTypes.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
            Object[][] newData = new Object[sortedResourceTypes.size()][headers.length];
            for (int i = 0; i < sortedResourceTypes.size(); i++)
            {
                newData[i][0] = i;
                newData[i][1] = sortedResourceTypes.get(i);
            }

            ((DefaultTableModel) table.getModel()).setDataVector(newData, headers);
            JPanel pane = new JPanel();
            JPanel pane2 = new JPanel();
            pane.setLayout(new BorderLayout());
            pane2.setLayout(new BorderLayout());
            pane.add(new JScrollPane(table), BorderLayout.CENTER);
            pane2.add(addRow, BorderLayout.WEST);
            pane2.add(removeRow, BorderLayout.EAST);
            pane2.add(removeAllRows, BorderLayout.SOUTH);
            pane.add(pane2, BorderLayout.SOUTH);
            final String[] optionsArray = new String[]{"Set to selected demand", "Set to all demands", "Cancel"};
            int result = JOptionPane.showOptionDialog(null, pane, "Set traversed resource types", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, optionsArray, optionsArray[0]);
            if ((result != 0) && (result != 1)) return;
            List<String> newTraversedResourcesTypes = new LinkedList<>();
            for (int j = 0; j < table.getRowCount(); j++)
            {
                String travResourceType = table.getModel().getValueAt(j, 1).toString();
                newTraversedResourcesTypes.add(travResourceType);
            }

            for (Demand selectedDemand : selectedDemands)
                if (!selectedDemand.getRoutes().isEmpty())
                    throw new Net2PlanException("It is not possible to set the resource types traversed to demands with routes");

            for (Demand selectedDemand : selectedDemands)
                selectedDemand.setServiceChainSequenceOfTraversedResourceTypes(newTraversedResourcesTypes);
		}
		, (a, b) -> true, null));

        res.add(new AjtRcMenu("Create and couple upper layer links from uncoupled demands in selection", e ->
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
                int result = JOptionPane.showConfirmDialog(null, pane, "Please select the upper layer to create the link", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (result != JOptionPane.OK_OPTION) return;

                long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                for (Demand d : getSelectedElements())
                    if (!d.isCoupled())
                        d.coupleToNewLinkCreated(np.getNetworkLayerFromId(layerId));
                break;
            }
		}
		, (a, b) -> b>0, null));
        
        res.add(new AjtRcMenu("Couple selected demands to upper layer link", e ->
		{
            Collection<Long> layerIds = np.getNetworkLayerIds();
            final JComboBox<StringLabeller> layerSelector = new WiderJComboBox();
            final JComboBox<StringLabeller> linkSelector = new WiderJComboBox();
            for (long layerId : layerIds)
            {
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
                    NetworkLayer selectedLayer = np.getNetworkLayerFromId(selectedLayerId);

                    linkSelector.removeAllItems();
                    Collection<Link> links_thisLayer = np.getLinks(selectedLayer);
                    for (Link link : links_thisLayer)
                    {
                        if (link.isCoupled()) continue;

                        String originNodeName = link.getOriginNode().getName();
                        String destinationNodeName = link.getDestinationNode().getName();

                        linkSelector.addItem(StringLabeller.unmodifiableOf(link.getId(), "e" + link.getIndex() + " [n" + link.getOriginNode().getIndex() + " (" + originNodeName + ") -> n" + link.getDestinationNode().getIndex() + " (" + destinationNodeName + ")]"));
                    }
                }

                if (linkSelector.getItemCount() == 0)
                {
                    linkSelector.setEnabled(false);
                } else
                {
                    linkSelector.setSelectedIndex(0);
                    linkSelector.setEnabled(true);
                }
            });

            layerSelector.setSelectedIndex(-1);
            layerSelector.setSelectedIndex(0);

            JPanel pane = new JPanel(new MigLayout("", "[][grow]", "[][]"));
            pane.add(new JLabel("Select layer: "));
            pane.add(layerSelector, "growx, wrap");
            pane.add(new JLabel("Select link: "));
            pane.add(linkSelector, "growx, wrap");

            while (true)
            {
                int result = JOptionPane.showConfirmDialog(null, pane, "Please select the upper layer link", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (result != JOptionPane.OK_OPTION) return;

                Long linkId = (Long) ((StringLabeller) linkSelector.getSelectedItem()).getObject();
                if (linkId == null) throw new NullPointerException();

                final Link link = np.getLinkFromId(linkId);
                if (link == null) throw new NullPointerException();

                for (Demand selectedDemand : getSelectedElements())
                    selectedDemand.coupleToUpperOrSameLayerLink(link);
                break;
            }
		}
		, (a, b) -> b>0, null));
        
        res.add(new AjtRcMenu("Decouple selected demands", e->getSelectedElements().forEach(dd->((Demand)dd).decouple()) , (a,b)->b>0, null));

        res.add(new AjtRcMenu("Create and couple links for selected uncoupled demands", e ->
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
                int result = JOptionPane.showConfirmDialog(null, pane, "Please select the upper layer to create links", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (result != JOptionPane.OK_OPTION) return;

                long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                NetworkLayer layer = np.getNetworkLayerFromId(layerId);
                for (Demand demand : getSelectedElements())
                    if (!demand.isCoupled())
                        demand.coupleToNewLinkCreated(layer);
            }
		}
		, (a, b) -> b>0, null));
    
        res.add(new AjtRcMenu("Monitor/forecast...",  null , (a,b)->true, Arrays.asList(
                AdvancedJTable_link.getMenuAddSyntheticMonitoringInfo (this),
                AdvancedJTable_link.getMenuExportMonitoringInfo(this),
                AdvancedJTable_link.getMenuImportMonitoringInfo (this),
                AdvancedJTable_link.getMenuSetMonitoredTraffic(this),                
                AdvancedJTable_link.getMenuPredictTrafficFromSameElementMonitorInfo (this),
                AdvancedJTable_link.getMenuForecastDemandTrafficUsingGravityModel (this),
                AdvancedJTable_link.getMenuForecastDemandTrafficFromLinkInfo (this),
                new AjtRcMenu("Remove all monitored/forecast stored information", e->getSelectedElements().forEach(dd->((Demand)dd).getMonitoredOrForecastedOfferedTraffic().removeAllValues()) , (a,b)->b>0, null),
                new AjtRcMenu("Remove monitored/forecast stored information...", null , (a,b)->b>0, Arrays.asList(
                		AdvancedJTable_link.getMenuRemoveMonitorInfoBeforeAfterDate (this , true) , 
                		AdvancedJTable_link.getMenuRemoveMonitorInfoBeforeAfterDate (this , false) 
                		))
        		)));

        return res;
    }

    
    static void createLinkDemandGUI(final NetworkElementType networkElementType, final NetworkLayer layer , final GUINetworkDesign callback)
    {
    	final boolean isDemand = networkElementType == NetworkElementType.DEMAND;
        final NetPlan netPlan = callback.getDesign();
        final JComboBox<StringLabeller<Node>> originNodeSelector = new WiderJComboBox();
        final JComboBox<StringLabeller<Node>> destinationNodeSelector = new WiderJComboBox();
        final JComboBox<StringLabeller<RoutingType>> routingTypeSelector = new WiderJComboBox();
        routingTypeSelector.addItem(StringLabeller.of(RoutingType.SOURCE_ROUTING, "Source routing"));
        routingTypeSelector.addItem(StringLabeller.of(RoutingType.HOP_BY_HOP_ROUTING, "Hop-by-hop routing"));
        routingTypeSelector.setSelectedIndex(0);
        
        for (Node node : netPlan.getNodes())
        {
            final String nodeName = node.getName().equals("")? "Node " + node.getIndex() : node.getName();
            originNodeSelector.addItem(StringLabeller.of(node, nodeName));
            destinationNodeSelector.addItem(StringLabeller.of(node, nodeName));
        }

        ItemListener nodeListener = new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                final Node originNode = (Node) ((StringLabeller) originNodeSelector.getSelectedItem()).getObject();
                final Node destinationNode = (Node) ((StringLabeller) destinationNodeSelector.getSelectedItem()).getObject();
                callback.putTransientColorInElementTopologyCanvas(Arrays.asList(originNode), Color.GREEN);
                callback.putTransientColorInElementTopologyCanvas(Arrays.asList(destinationNode), Color.CYAN);
            }
        };

        originNodeSelector.addItemListener(nodeListener);
        destinationNodeSelector.addItemListener(nodeListener);

        originNodeSelector.setSelectedIndex(0);
        destinationNodeSelector.setSelectedIndex(1);

        JPanel pane = new JPanel();
        pane.add(networkElementType == NetworkElementType.LINK ? new JLabel("Origin node: ") : new JLabel("Ingress node: "));
        pane.add(originNodeSelector);
        pane.add(Box.createHorizontalStrut(15));
        pane.add(networkElementType == NetworkElementType.LINK ? new JLabel("Destination node: ") : new JLabel("Egress node: "));
        pane.add(destinationNodeSelector);
        if (isDemand)
        {
            pane.add(new JLabel("Routing type: "));
            pane.add(routingTypeSelector);
        }
        while (true)
        {
            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter end nodes for the new " + networkElementType, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            Node originNode = (Node) ((StringLabeller) originNodeSelector.getSelectedItem()).getObject();
            Node destinationNode = (Node) ((StringLabeller) destinationNodeSelector.getSelectedItem()).getObject();
            if (networkElementType == NetworkElementType.LINK)
            {
                final Link e = netPlan.addLink(originNode, destinationNode, 0, 0, 200000, null, layer);
                callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                callback.updateVisualizationAfterChanges();
                callback.getPickManager().pickElements(e);
                callback.updateVisualizationAfterPick();
                callback.addNetPlanChange();

            } else
            {
            	final RoutingType rt = (RoutingType) ((StringLabeller) routingTypeSelector.getSelectedItem()).getObject();
                final Demand d = netPlan.addDemand(originNode, destinationNode, 0, rt , null , layer);
                callback.updateVisualizationAfterChanges();
                callback.getPickManager().pickElements(d);
                callback.updateVisualizationAfterPick();
                callback.addNetPlanChange();
            }

            break;
        }
    }
    
    private void rcMenuFullMeshTraffic(boolean isSourceRouting)
    {
        final NetPlan np = callback.getDesign();
        final Collection<Node> nodes;
        nodes = (callback.getSelectedElements(AJTableType.NODES , getTableNetworkLayer()).isEmpty()? np.getNodes() : (Set<Node>) callback.getSelectedElements(AJTableType.NODES , getTableNetworkLayer()));
        if (nodes.isEmpty()) throw new Net2PlanException("There are no nodes");
        for (Node n1 : nodes)
            for (Node n2 : nodes)
                if (n1.getIndex() < n2.getIndex())
                    np.addDemandBidirectional(n1, n2, 0 , isSourceRouting? RoutingType.SOURCE_ROUTING : RoutingType.HOP_BY_HOP_ROUTING , null , getTableNetworkLayer());
        callback.getPickManager().reset();
    }


    private class ClassAwareTableModelImpl extends ClassAwareTableModel
    {
        public ClassAwareTableModelImpl(Object[][] dataVector, Object[] columnIdentifiers)
        {
            super(dataVector, columnIdentifiers);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            if (columnIndex == 1) return true;
            return false;
        }

        @Override
        public void setValueAt(Object value, int row, int column)
        {
            super.setValueAt(value, row, column);

        }

    }

    
}
