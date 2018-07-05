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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
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
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.StringUtils;

/**
 */
@SuppressWarnings({ "unchecked", "serial" })
public class AdvancedJTable_multicastDemand extends AdvancedJTable_networkElement<MulticastDemand>
{
    public AdvancedJTable_multicastDemand(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.MULTICAST_DEMANDS , layerThisTable , true , null);
    }

    @Override
  public List<AjtColumnInfo<MulticastDemand>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
      final List<AjtColumnInfo<MulticastDemand>> res = new LinkedList<> ();
      res.add(new AjtColumnInfo<MulticastDemand>(this , Node.class, null , "A", "Ingress node", null , d->d.getIngressNode() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<MulticastDemand>(this , String.class, null , "Bs", "Egress nodes", null , d->d.getEgressNodes().stream().map(n->n.getName().equals("")? "Node " + n.getIndex() : n.getName()).collect(Collectors.joining(",")) , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<MulticastDemand>(this , Collection.class, null , "Coupled links", "The links that this demnad is coupled to, if any", null , d->d.isCoupled()? d.getCoupledLinks() : "-" , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<MulticastDemand>(this , Double.class, null , "Offered traffic (" + getTableNetworkLayer().getLinkCapacityUnits() + ")", "Offered traffic by the demand", (d,val)->d.setOfferedTraffic((Double) val), d->d.getOfferedTraffic() , AGTYPE.SUMDOUBLE , null));
      res.add(new AjtColumnInfo<MulticastDemand>(this , Double.class, null , "Carried traffic (" + getTableNetworkLayer().getLinkCapacityUnits() + ")", "Carried traffic by the demand", null , d->d.getCarriedTraffic() , AGTYPE.SUMDOUBLE , null));
      res.add(new AjtColumnInfo<MulticastDemand>(this , Double.class, null , "% Lost traffic", "Percentage of the lost traffic by the demand", null, d->d.getOfferedTraffic() == 0? 0 : d.getBlockedTraffic() / d.getOfferedTraffic() , AGTYPE.NOAGGREGATION , d->d.getBlockedTraffic() > 0? Color.RED : Color.GREEN));
      res.add(new AjtColumnInfo<MulticastDemand>(this , String.class, null , "QoS type", "A used-defined string identifying the type of traffic of the demand", (d,val)-> d.setQoSType((String)val) , d->d.getQosType(), AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<MulticastDemand>(this , Boolean.class, null , "All nodes reached?", "True if all the multicast trees of the demand reach all the demand egress nodes", null, d->d.isAllTreesReachingAllEgressNodes() , AGTYPE.COUNTTRUE , d->d.isAllTreesReachingAllEgressNodes()? Color.GREEN : Color.RED));
      res.add(new AjtColumnInfo<MulticastDemand>(this , String.class, null , "Bifurcated?", "Indicates whether the demand is satisfied by more than one multicast tree", null, d->d.isBifurcated() , AGTYPE.COUNTTRUE , null));
      res.add(new AjtColumnInfo<MulticastDemand>(this , Integer.class, null , "# trees", "Number of associated multicast trees", null, d->d.getMulticastTrees().size() , AGTYPE.SUMINT , null));
      res.add(new AjtColumnInfo<MulticastDemand>(this , Double.class, null , "Worst e2e lat (ms)", "Current worst case end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any), from the origin node, to destination nodes reached", null, d->d.getWorseCasePropagationTimeInMs() , AGTYPE.NOAGGREGATION , d->{ final double maxMs = d.getMaximumAcceptableE2EWorstCaseLatencyInMs(); return maxMs <= 0? null : (d.getWorseCasePropagationTimeInMs() > maxMs? Color.RED : null); }));
      res.add(new AjtColumnInfo<MulticastDemand>(this , Double.class, null , "Worst e2e length (km)", "Current worst case end-to-end propagation length in km (accumulating any lower layer propagation lengths if any)", null, d->d.getWorstCaseLengthInKm() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<MulticastDemand>(this , Double.class, null , "Limit e2e lat (ms)", "Maximum end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any)", (d,val)-> d.setMaximumAcceptableE2EWorstCaseLatencyInMs((Double)val) , d->d.getMaximumAcceptableE2EWorstCaseLatencyInMs() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<MulticastDemand>(this , Double.class, null , "CAGR(%)" , "Compound annual growth factor for this demand", (d,val)->d.setOfferedTrafficPerPeriodGrowthFactor((Double) val), d->d.getOfferedTrafficPerPeriodGrowthFactor() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<MulticastDemand>(this , Integer.class, null , "#Monit points" , "Number of samples of the offered traffic stored, coming from a monitoring or forecasting traffic process", null , d->d.getMonitoredOrForecastedOfferedTraffic().getSize() , AGTYPE.NOAGGREGATION , null));
      return res;
  }

    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
    	final NetPlan np = callback.getDesign();
        final List<AjtRcMenu> res = new ArrayList<> ();
        res.add(new AjtRcMenu("Add demand", e->createMulticastDemandGUI(NetworkElementType.MULTICAST_DEMAND, getTableNetworkLayer () , callback), (a,b)->true, null));
        res.add(new AjtRcMenu("Remove selected demands", e->getSelectedElements().forEach(dd->((MulticastDemand)dd).remove()) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Add one broadcast demand per node", e->new BroadcastDemandPerNodeActionListener() , (a,b)->true, null));
        res.add(new AjtRcMenu("Add one demand per ingress node, with random egress nodes", e->new MulticastDemandPerNodeActionListener() , (a,b)->true, null));
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
                    		getSelectedElements().forEach(dd->((MulticastDemand)dd).setMaximumAcceptableE2EWorstCaseLatencyInMs(newLimit));
                    	}
                    );
        }, (a,b)->b>0, null));

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
                    		final List<MulticastDemand> changedDemands = getSelectedElements().stream().map(ee->(MulticastDemand)ee).collect(Collectors.toList());
                    		try
                    		{
                                if (callback.getVisualizationState().isWhatIfAnalysisActive())
                                    callback.getWhatIfAnalysisPane().whatIfMulticastDemandOfferedTrafficModified(changedDemands, Collections.nCopies(changedDemands.size(), newOfferedTraffic));
                                else
                                	changedDemands.forEach(d->d.setOfferedTraffic(newOfferedTraffic));
                    			
                    		} catch (Throwable ex) { ex.printStackTrace(); throw new Net2PlanException (ex.getMessage());  }
                    	}
                    );
		}, (a,b)->b>0, null));
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
                    		final List<MulticastDemand> changedDemands = getSelectedElements().stream().map(ee->(MulticastDemand)ee).collect(Collectors.toList());
                    		try
                    		{
                                if (callback.getVisualizationState().isWhatIfAnalysisActive())
                                    callback.getWhatIfAnalysisPane().whatIfMulticastDemandOfferedTrafficModified(changedDemands, changedDemands.stream().map(d-> new Double ((d.getOfferedTraffic () * neScalingFactor))).collect(Collectors.toList()));
                                else
                                	changedDemands.forEach(d->d.setOfferedTraffic(d.getOfferedTraffic() * neScalingFactor));
                    		} catch (Throwable ex) { ex.printStackTrace(); throw new Net2PlanException (ex.getMessage());  }
                    	}
                    );
		}
		, (a, b) -> b>0, null));

        res.add(new AjtRcMenu("Decouple selected demands", e->getSelectedElements().forEach(dd->((MulticastDemand)dd).decouple()) , (a,b)->b>0, null));

        res.add(new AjtRcMenu("Create and couple upper layer links from uncoupled selected demands", e ->
		{
            List<Long> layerIds = np.getNetworkLayerIds();
            final JComboBox<StringLabeller> layerSelector = new WiderJComboBox();
            for (long layerId : layerIds)
            {
                if (layerId == getTableNetworkLayer().getId()) continue;

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
                for (MulticastDemand demand : getSelectedElements())
                    if (!demand.isCoupled())
                        demand.coupleToNewLinksCreated(layer);
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
                new AjtRcMenu("Remove all monitored/forecast stored information", e->getSelectedElements().forEach(dd->((MulticastDemand)dd).getMonitoredOrForecastedOfferedTraffic().removeAllValues()) , (a,b)->b>0, null),
                new AjtRcMenu("Remove monitored/forecast stored information...", null , (a,b)->b>0, Arrays.asList(
                		AdvancedJTable_link.getMenuRemoveMonitorInfoBeforeAfterDate (this , true) , 
                		AdvancedJTable_link.getMenuRemoveMonitorInfoBeforeAfterDate (this , false) 
                		))
        		)));
        
        return res;

    }
    
    private static void createMulticastDemandGUI(final NetworkElementType networkElementType, final NetworkLayer layer , final GUINetworkDesign callback)
    {
        final NetPlan netPlan = callback.getDesign();

        JTextField textFieldIngressNodeId = new JTextField(20);
        JTextField textFieldEgressNodeIds = new JTextField(20);

        JPanel pane = new JPanel();
        pane.add(new JLabel("Ingress node id: "));
        pane.add(textFieldIngressNodeId);
        pane.add(Box.createHorizontalStrut(15));
        pane.add(new JLabel("Egress node ids (space separated): "));
        pane.add(textFieldEgressNodeIds);

        while (true)
        {
            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter multicast demand ingress node and set of egress nodes", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
            if (textFieldIngressNodeId.getText().isEmpty())
                throw new Net2PlanException("Please, insert the ingress node id");
            if (textFieldEgressNodeIds.getText().isEmpty())
                throw new Net2PlanException("Please, insert the set of egress node ids");
            String ingressNodeId_st = textFieldIngressNodeId.getText();
            String egressNodeId_st = textFieldEgressNodeIds.getText();

            final long ingressNode = Long.parseLong(ingressNodeId_st);
            if (netPlan.getNodeFromId(ingressNode) == null)
                throw new Net2PlanException("Not a valid ingress node id: " + ingressNodeId_st);
            Set<Node> egressNodes = new HashSet<Node>();
            for (String egressNodeIdString : StringUtils.split(egressNodeId_st))
            {
                final long nodeId = Long.parseLong(egressNodeIdString);
                final Node node = netPlan.getNodeFromId(nodeId);
                if (node == null) throw new Net2PlanException("Not a valid egress node id: " + egressNodeIdString);
                egressNodes.add(node);
            }
            netPlan.addMulticastDemand(netPlan.getNodeFromId(ingressNode), egressNodes, 0, null , layer);
            break;
        }
    }


    private class BroadcastDemandPerNodeActionListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            NetPlan netPlan = callback.getDesign();

            if (netPlan.hasMulticastDemands(getTableNetworkLayer()))
            {
                int result = JOptionPane.showConfirmDialog(null, "Remove all existing multicast demands before?", "", JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) return;
                else if (result == JOptionPane.YES_OPTION) netPlan.removeAllMulticastDemands();
            }

            if (netPlan.getNumberOfNodes() < 2) throw new Net2PlanException("At least two nodes are needed");

            for (Node ingressNode : netPlan.getNodes())
            {
                Set<Node> egressNodes = new HashSet<Node>(netPlan.getNodes());
                egressNodes.remove(ingressNode);
                netPlan.addMulticastDemand(ingressNode, egressNodes, 0, null);
            }
        }
    }

    private class MulticastDemandPerNodeActionListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            Random rng = new Random();
            NetPlan netPlan = callback.getDesign();

            if (netPlan.hasMulticastDemands(getTableNetworkLayer()))
            {
                int result = JOptionPane.showConfirmDialog(null, "Remove all existing multicast demands before?", "", JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) return;
                if (result == JOptionPane.YES_OPTION) netPlan.removeAllMulticastDemands();
            }

            if (netPlan.getNumberOfNodes() < 2) throw new Net2PlanException("At least two nodes are needed");

            for (Node ingressNode : netPlan.getNodes())
            {
                Set<Node> egressNodes = new HashSet<Node>();
                for (Node n : netPlan.getNodes()) if ((n != ingressNode) && rng.nextBoolean()) egressNodes.add(n);
                if (egressNodes.isEmpty()) egressNodes.add(netPlan.getNode(ingressNode.getIndex() == 0 ? 1 : 0));
                netPlan.addMulticastDemand(ingressNode, egressNodes, 0, null);
            }
        }

    }
}
