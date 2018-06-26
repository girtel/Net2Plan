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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.ElementSelection;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.gui.utils.JScrollPopupMenu;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.StringUtils;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

/**
 */
@SuppressWarnings({ "unchecked", "serial" })
public class AdvancedJTable_multicastTree extends AdvancedJTable_networkElement<MulticastTree>
{
    public AdvancedJTable_multicastTree(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.MULTICAST_TREES , layerThisTable , true , null);
    }

    @Override
  public List<AjtColumnInfo<MulticastTree>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
      final List<AjtColumnInfo<MulticastTree>> res = new LinkedList<> ();
      res.add(new AjtColumnInfo<MulticastTree>(this , Node.class, null , "A", "Ingress node", null , d->d.getIngressNode() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<MulticastTree>(this , String.class, null , "Bs", "Egress nodes", null , d->d.getMulticastDemand().getEgressNodes().stream().map(n->n.getName().equals("")? "Node " + n.getIndex() : n.getName()).collect(Collectors.joining(",")) , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<MulticastTree>(this , String.class, null , "Bs (reached)", "Reached egress nodes", null , d->d.getEgressNodesReached().stream().map(n->n.getName().equals("")? "Node " + n.getIndex() : n.getName()).collect(Collectors.joining(",")) , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<MulticastTree>(this , Integer.class, null , "# Bs not reached", "Number of non-reached egress nodes", null , d->d.getMulticastDemand().getEgressNodes().size() - d.getEgressNodesReached().size() , AGTYPE.SUMINT , d-> d.getMulticastDemand().getEgressNodes().size() - d.getEgressNodesReached().size() > 0? Color.RED : null));
      res.add(new AjtColumnInfo<MulticastTree>(this , MulticastDemand.class, null , "Demand", "Associated multicast demand", null , d->d.getMulticastDemand() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<MulticastTree>(this , Double.class, null , "Carried traffic (" + getTableNetworkLayer().getDemandTrafficUnits() + ")", "Carried traffic by the multicast tree", null , d->d.getCarriedTraffic() , AGTYPE.SUMDOUBLE , null));
      res.add(new AjtColumnInfo<MulticastTree>(this , Double.class, null , "Occupied capacity (" + getTableNetworkLayer().getLinkCapacityUnits() + ")", "Occupied capacity in the traversed links", null, d->d.getOccupiedLinkCapacity() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<MulticastTree>(this , Collection.class, null , "Traversed links", "Traversed links in the non-failure state", null, d->d.getLinkSet() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<MulticastTree>(this , Double.class, null , "Total tree length (km)", "Sum of the lengths of all the tree links (accumulating any lower layer propagation lengths if any)", null, d->d.getTreeTotalLengthInKm() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<MulticastTree>(this , Double.class, null , "Worst e2e lat (ms)", "Current worst case end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any), from the origin node, to destination nodes reached", null, d->d.getTreeMaximumPropagationDelayInMs() , AGTYPE.NOAGGREGATION , d->{ final double m = d.getMulticastDemand().getMaximumAcceptableE2EWorstCaseLatencyInMs(); if (m >= 0) return null; return d.getTreeMaximumPropagationDelayInMs () > m? Color.RED : null; }));
      res.add(new AjtColumnInfo<MulticastTree>(this , Double.class, null , "Worst e2e length (km)", "Current worst case end-to-end propagation length in km (accumulating any lower layer propagation lengths if any)", null, d->d.getTreeMaximumPathLengthInKm() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<MulticastTree>(this , Double.class, null , "Worst e2e length (hops)", "Current worst case end-to-end propagation length in number of traversed links", null, d->d.getTreeMaximumPathLengthInHops() , AGTYPE.NOAGGREGATION , null));
      return res;
  }

    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
    	final NetPlan np = callback.getDesign();
        final List<AjtRcMenu> res = new ArrayList<> ();
        res.add(new AjtRcMenu("Add multicast tree", e->createMulticastTreeGUI(callback), (a,b)->true, null));
        res.add(new AjtRcMenu("Remove selected trees", e->getSelectedElements().forEach(dd->((MulticastTree)dd).remove()) , (a,b)->b>0, null));

        res.add(new AjtRcMenu("Add one tree per demand, minimizing end-to-end average number of traversed links", e->new MulticastTreeMinE2EActionListener(true, false) , (a,b)->true, null));
        res.add(new AjtRcMenu("Add one tree per demand, minimizing end-to-end average traversed length in km", e->new MulticastTreeMinE2EActionListener(false, false) , (a,b)->true, null));
        res.add(new AjtRcMenu("Add one tree per demand, minimizing number of links in the tree (uses default ILP solver)", e->new MulticastTreeMinE2EActionListener(true, true) , (a,b)->true, null));
        res.add(new AjtRcMenu("Add one tree per demand, minimizing number of km of the links in the tree (uses default ILP solver)", e->new MulticastTreeMinE2EActionListener(false, true) , (a,b)->true, null));
        
        
        return res;
    }

    private static void createMulticastTreeGUI(final GUINetworkDesign callback)
    {
        final NetPlan netPlan = callback.getDesign();

        JTextField textFieldDemandIndex = new JTextField(20);
        JTextField textFieldLinkIndexes = new JTextField(20);
        JPanel pane = new JPanel();
        pane.add(new JLabel("Multicast demand index: "));
        pane.add(textFieldDemandIndex);
        pane.add(Box.createHorizontalStrut(15));
        pane.add(new JLabel("Link indexes (space separated): "));
        pane.add(textFieldLinkIndexes);

        while (true)
        {
            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter multicast tree demand index and link indexes", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            if (textFieldDemandIndex.getText().isEmpty())
                throw new Net2PlanException("Please, insert the multicast demand index");
            if (textFieldLinkIndexes.getText().isEmpty()) throw new Net2PlanException("Please, insert the link indexes");
            MulticastDemand demand = netPlan.getMulticastDemand(Integer.parseInt(textFieldDemandIndex.getText()));
            Set<Link> links = new HashSet<Link>();
            for (String linkString : StringUtils.split(textFieldLinkIndexes.getText()))
                links.add(netPlan.getLink(Integer.parseInt(linkString)));
            netPlan.addMulticastTree(demand, 0, 0, links, null);
            break;
        }
    }



    private class MulticastTreeMinE2EActionListener implements ActionListener
    {
        final boolean isMinHops;
        final boolean minCost;

        private MulticastTreeMinE2EActionListener(boolean isMinHops, boolean minCost)
        {
            this.isMinHops = isMinHops;
            this.minCost = minCost;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            NetPlan netPlan = callback.getDesign();
            List<Link> links = netPlan.getLinks();
            final int E = links.size();
            List<MulticastTree> addedTrees = new LinkedList<MulticastTree>();

            // Ask for current element removal
            if (netPlan.hasMulticastTrees(getTableNetworkLayer()))
            {
                final int answer = JOptionPane.showConfirmDialog(null, "Remove all existing multicast trees?", "", JOptionPane.YES_NO_CANCEL_OPTION);
                if (answer == JOptionPane.OK_OPTION) netPlan.removeAllMulticastTrees(getTableNetworkLayer());
                if (answer == JOptionPane.CANCEL_OPTION || answer == JOptionPane.CLOSED_OPTION) return;
            }

            if (minCost)
            {
                DoubleMatrix1D linkCosts = isMinHops ? DoubleFactory1D.dense.make(E, 1) : netPlan.getVectorLinkLengthInKm();
                String solverName = Configuration.getDefaultIlpSolverName();
                String solverLibraryName = Configuration.getDefaultSolverLibraryName(solverName);
                DoubleMatrix2D Aout_ne = netPlan.getMatrixNodeLinkOutgoingIncidence();
                DoubleMatrix2D Ain_ne = netPlan.getMatrixNodeLinkIncomingIncidence();
                for (MulticastDemand demand : netPlan.getMulticastDemands())
                {
                    Set<Link> linkSet = GraphUtils.getMinimumCostMulticastTree(getTableNetworkLayer(), Aout_ne, Ain_ne, linkCosts, demand.getIngressNode(), demand.getEgressNodes(), E, -1, -1.0, -1.0, solverName, solverLibraryName, 5.0);
                    addedTrees.add(netPlan.addMulticastTree(demand, demand.getOfferedTraffic(), demand.getOfferedTraffic(), linkSet, null));
                }
            } else
            {
                Map<Link, Double> linkCostMap = new HashMap<Link, Double>();
                for (Link link : netPlan.getLinks()) linkCostMap.put(link, isMinHops ? 1 : link.getLengthInKm());
                for (MulticastDemand demand : netPlan.getMulticastDemands())
                {
                    Set<Link> linkSet = new HashSet<Link>();
                    for (Node egressNode : demand.getEgressNodes())
                    {
                        List<Link> seqLinks = GraphUtils.getShortestPath(netPlan.getNodes(), netPlan.getLinks(), demand.getIngressNode(), egressNode, linkCostMap);
                        if (seqLinks.isEmpty())
                            throw new Net2PlanException("The multicast tree cannot be created since the network is not connected");
                        linkSet.addAll(seqLinks);
                    }
                    addedTrees.add(netPlan.addMulticastTree(demand, demand.getOfferedTraffic(), demand.getOfferedTraffic(), linkSet, null));
                }
            }
        }

    }

    private List<MulticastTree> getVisibleElementsInTable()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final NetworkLayer layer = getTableNetworkLayer();
        return rf == null ? callback.getDesign().getMulticastTrees(layer) : rf.getVisibleMulticastTrees(layer);
    }
}
