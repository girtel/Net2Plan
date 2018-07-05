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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_abstractElement.AGTYPE;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.gui.utils.StringLabeller;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.IPUtils;
import com.net2plan.utils.Pair;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import net.miginfocom.swing.MigLayout;

/**
 */
public class AdvancedJTable_forwardingRule extends AdvancedJTable_networkElement<Pair<Demand,Link>>
{
    public AdvancedJTable_forwardingRule(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.FORWARDINGRULES , layerThisTable , true , null);
    }

    @Override
    public List<AjtColumnInfo<Pair<Demand,Link>>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
    	final NetPlan np = callback.getDesign();
    	final NetworkLayer layer = this.getTableNetworkLayer();
      final List<AjtColumnInfo<Pair<Demand,Link>>> res = new LinkedList<> ();
      res.add(new AjtColumnInfo<Pair<Demand,Link>>(this , Node.class, null , "Node", "The node where the forwading rule is placed: the origin node of the forwarding rule link", null , d->d.getSecond().getOriginNode() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Pair<Demand,Link>>(this , Demand.class, null , "Demand", "The demand associated to the forwarding rule", null , d->d.getFirst() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Pair<Demand,Link>>(this , Link.class, null , "Link", "The link associated to the forwarding rule", null , d->d.getSecond() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Pair<Demand,Link>>(this , Double.class, null , "Splitting ratio" , "The fraction of the traffic of the demand that reaches the link origin node, that is forwarded to the forwarding rule link", null , d->np.getForwardingRuleSplittingFactor(d.getFirst(), d.getSecond()) , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Pair<Demand,Link>>(this , Double.class, null , "Carried traffic (" + layer.getDemandTrafficUnits() + ")" , "Traffic affected by this forwarding rule", null , d->np.getForwardingRuleCarriedTraffic(d.getFirst(), d.getSecond()) , AGTYPE.SUMDOUBLE , null));
      return res;
  }
  
  public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
  {
	  final NetPlan np = callback.getDesign();
  	final NetworkLayer layer = this.getTableNetworkLayer();
      final List<AjtRcMenu> res = new ArrayList<> ();
      res.add(new AjtRcMenu("Add forwarding rule", e->createForwardingRuleGUI(callback , layer), (a,b)->b>0, null));
      res.add(new AjtRcMenu("Remove selected rules", e->
      {
    	  final List<Demand> demands = getSelectedElements().stream().map(d->d.getFirst()).collect(Collectors.toList());
    	  final List<Link> links = getSelectedElements().stream().map(d->d.getSecond()).collect(Collectors.toList());
    	  final List<Double> splittingFactors = getSelectedElements().stream().map(d->new Double (0)).collect(Collectors.toList());
    	  np.setForwardingRules(demands, links, splittingFactors, true);
      }, (a,b)->b>0, null));
      res.add(new AjtRcMenu("Generate ECMP forwarding rules from link IGP weights", e->
      {
          DoubleMatrix1D linkWeightMap = IPUtils.getLinkWeightVector(np);
          IPUtils.setECMPForwardingRulesFromLinkWeights(np, linkWeightMap);
      }, (a,b)->b>0, null));

      return res;
  }
  
    private static void createForwardingRuleGUI(final GUINetworkDesign callback , final NetworkLayer layer)
    {
        final NetPlan netPlan = callback.getDesign();
        final JComboBox<StringLabeller> nodeSelector = new WiderJComboBox();
        final JComboBox<StringLabeller> linkSelector = new WiderJComboBox();
        final JComboBox<StringLabeller> demandSelector = new WiderJComboBox();
        final JTextField txt_splittingRatio = new JTextField(5);

        ItemListener nodeListener = new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                JComboBox<StringLabeller> me = (JComboBox) e.getSource();
                linkSelector.removeAllItems();
                final Node node = (Node) ((StringLabeller) me.getSelectedItem()).getObject();
                for (Link link : node.getOutgoingLinks(layer))
                {
                    String originNodeLabel = "Node " + link.getOriginNode().getId();
                    if (!link.getOriginNode().getName().isEmpty())
                        originNodeLabel += " (" + link.getOriginNode().getName() + ")";
                    String destinationNodeLabel = "Node " + link.getDestinationNode().getId();
                    if (!link.getDestinationNode().getName().isEmpty())
                        destinationNodeLabel += " (" + link.getDestinationNode().getName() + ")";
                    String linkLabel = "e" + link.getId() + ": " + originNodeLabel + " -> " + destinationNodeLabel;
                    linkSelector.addItem(StringLabeller.of(link, linkLabel));
                }

                linkSelector.setSelectedIndex(0);
            }
        };

        ItemListener linkDemandListener = new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                Demand demand = (Demand) ((StringLabeller) demandSelector.getSelectedItem()).getObject();
                Link link = (Link) ((StringLabeller) linkSelector.getSelectedItem()).getObject();
                double splittingRatio;
                if (netPlan.getForwardingRuleSplittingFactor(demand, link) > 0)
                {
                    splittingRatio = netPlan.getForwardingRuleSplittingFactor(demand, link);
                } else
                {
                    Node node = link.getOriginNode();
                    Map<Pair<Demand, Link>, Double> forwardingRules_thisNode = node.getForwardingRules(demand);
                    double totalSplittingRatio = 0;
                    for (Double value : forwardingRules_thisNode.values()) totalSplittingRatio += value;

                    splittingRatio = Math.max(0, 1 - totalSplittingRatio);
                }

                txt_splittingRatio.setText(Double.toString(splittingRatio));
            }
        };

        nodeSelector.addItemListener(nodeListener);

        for (Node node : netPlan.getNodes())
        {
            if (node.getOutgoingLinks(layer).isEmpty()) continue;

            final String nodeName = node.getName();
            String nodeLabel = "Node " + node.getId();
            if (!nodeName.isEmpty()) nodeLabel += " (" + nodeName + ")";

            nodeSelector.addItem(StringLabeller.of(node, nodeLabel));
        }

        linkSelector.addItemListener(linkDemandListener);
        demandSelector.addItemListener(linkDemandListener);

        for (Demand demand : netPlan.getDemands(layer))
        {
            String ingressNodeLabel = "Node " + demand.getIngressNode().getId();
            if (!demand.getIngressNode().getName().isEmpty())
                ingressNodeLabel += " (" + demand.getIngressNode().getName() + ")";
            String egressNodeLabel = "Node " + demand.getEgressNode().getId();
            if (!demand.getEgressNode().getName().isEmpty())
                egressNodeLabel += " (" + demand.getEgressNode().getName() + ")";
            String demandLabel = "d" + demand.getId() + ": " + ingressNodeLabel + " -> " + egressNodeLabel;
            demandSelector.addItem(StringLabeller.of(demand, demandLabel));
        }

        nodeSelector.setSelectedIndex(0);
        demandSelector.setSelectedIndex(0);

        JPanel pane = new JPanel(new MigLayout("fill", "[][grow]", "[][][][][]"));
        pane.add(new JLabel("Node where to install the rule: "));
        pane.add(nodeSelector, "wrap");
        pane.add(new JLabel("Outgoing link: "));
        pane.add(linkSelector, "wrap");
        pane.add(new JLabel("Demand: "));
        pane.add(demandSelector, "wrap");
        pane.add(new JLabel("Splitting ratio: "));
        pane.add(txt_splittingRatio, "wrap");

        while (true)
        {
            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter information for the new forwarding rule", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            long demandId = (long) ((StringLabeller) demandSelector.getSelectedItem()).getObject();
            long linkId = (long) ((StringLabeller) linkSelector.getSelectedItem()).getObject();

            double splittingRatio;
            try
            {
                splittingRatio = Double.parseDouble(txt_splittingRatio.getText());
                if (splittingRatio <= 0) throw new RuntimeException();
            } catch (Throwable e)
            {
                ErrorHandling.showErrorDialog("Splitting ratio must be a non-negative non-zero number", "Error adding forwarding rule");
                continue;
            }
            netPlan.setForwardingRules(Arrays.asList(netPlan.getDemandFromId(demandId)), Arrays.asList(netPlan.getLinkFromId(linkId)) , Arrays.asList(splittingRatio) , true);
            break;
        }
    }
}
