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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.utils.CellRenderers;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ButtonColumn;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.StringLabeller;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.SwingUtils;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import net.miginfocom.swing.MigLayout;

/**
 */
public class AdvancedJTable_route extends AdvancedJTable_networkElement<Route>
{
    public AdvancedJTable_route(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.ROUTES , layerThisTable , true , r->r.isDown() && r.getCarriedTrafficInNoFailureState() > 0? Color.RED : null);
    }

    @Override
  public List<AjtColumnInfo<Route>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
    	final NetPlan np = callback.getDesign();
    	final NetworkLayer layer = this.getTableNetworkLayer();
      final List<AjtColumnInfo<Route>> res = new LinkedList<> ();
      res.add(new AjtColumnInfo<Route>(this , Demand.class, null , "Demand", "Associated demand", null , d->d.getDemand() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Route>(this , Node.class, null , "A", "Ingress node", null , d->d.getIngressNode() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Route>(this , Node.class, null , "B", "Egress node", null , d->d.getEgressNode() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Route>(this , Boolean.class, null , "Is up?", "The route is considered up if it is not traversing failed links or nodes", null , d->!d.isDown() , AGTYPE.COUNTTRUE, r->r.getCarriedTrafficInNoFailureState() > 0 && r.isDown()? Color.RED : null));
      res.add(new AjtColumnInfo<Route>(this , Boolean.class, null , "Trav. 0-cap links?", "Indicates if the route is traversing links with zero capacity", null , d->!d.isTraversingZeroCapLinks() , AGTYPE.COUNTTRUE, r->r.getCarriedTrafficInNoFailureState() > 0 && r.isTraversingZeroCapLinks()? Color.RED : null));
      res.add(new AjtColumnInfo<Route>(this , Demand.class, null , "Bidirectional pair", "If the route is bidirectional, provides its bidirectional pair", null , d->d.getBidirectionalPair() , AGTYPE.NOAGGREGATION, null));
      res.add(new AjtColumnInfo<Route>(this , Collection.class, null , "Main routes", "If this is a backup route, shows the routes I am backing up", null , d->d.getRoutesIAmBackup(), AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Route>(this , Collection.class, null , "Backup routes", "If this is a main route, shows the its back up routes", null , d->d.getBackupRoutes(), AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Route>(this , Double.class, null , "Carried traffic (" + layer.getDemandTrafficUnits() + ")", "Carried traffic by the route", (d,val)->d.setCarriedTraffic((Double) val , null), d->d.getCarriedTraffic() , AGTYPE.SUMDOUBLE , null));
      res.add(new AjtColumnInfo<Route>(this , Double.class, null , "Occupied capacity (" + layer.getLinkCapacityUnits() + ")", "Occupied capacity in the traversed links. If the occupied capacity is different in different links, no information is shown", null , d->d.isOccupyingDifferentCapacitiesInDifferentLinksInNoFailureState()? "--" : d.getSeqOccupiedCapacitiesIfNotFailing().get(0) , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Route>(this , Collection.class, null , "# links", "Number of traversed links", null , d->d.getSeqLinks() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Route>(this , Collection.class, null , "# resources", "Number of traversed resources", null , d->d.getSeqResourcesTraversed() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Route>(this , Double.class, null , "Length (km)", "Length of the route, considering also the length of the lower layer links traversed if any", null , d->d.getLengthInKm() , AGTYPE.MAXDOUBLE , null));
      res.add(new AjtColumnInfo<Route>(this , Double.class, null , "E2e latency (ms)", "End-to-end latency considering links and resources traversed, and propagation time in lower layer links if any", null , d->d.getPropagationDelayInMiliseconds() , AGTYPE.MAXDOUBLE , d->{ final double m = d.getDemand().getMaximumAcceptableE2EWorstCaseLatencyInMs(); if (m >= 0) return null; return d.getPropagationDelayInMiliseconds () > m? Color.RED : null; }));
      return res;
  }

    @Override
    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
    	final NetPlan np = callback.getDesign();
    	final NetworkLayer layer = this.getTableNetworkLayer();
        final List<AjtRcMenu> res = new ArrayList<> ();
        res.add(new AjtRcMenu("Add route", e->createRouteGUI(callback , layer), (a,b)->true, null));
        res.add(new AjtRcMenu("Remove selected routes", e->getSelectedElements().forEach(dd->((Route)dd).remove()) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Add one route per demand, shortest path (Service chain) in hops", e->new RouteSPFActionListener(true, false) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Add one route per demand, shortest path (Service chain) in km", e->new RouteSPFActionListener(false, false) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Add one route and 1+1 link disjoint protection per demand (minimize total num hops)", e->new RouteSPFActionListener(true, true) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Add one route and 1+1 link disjoint protection per demand (minimize total km)", e->new RouteSPFActionListener(false, true) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("View/edit backup routes", e->viewEditBackupRoutesGUI(callback, getSelectedElements().first()) , (a,b)->b==1, null));

        return res;
    }

    
    private static void createRouteGUI(final GUINetworkDesign callback , final NetworkLayer layer)
    {
        final NetPlan netPlan = callback.getDesign();
        final Collection<Long> demandIds = NetPlan.getIds(netPlan.getDemands(layer));
        final JComboBox<StringLabeller> demandSelector = new WiderJComboBox();
        if (netPlan.getNumberOfLinks() == 0)
            throw new Net2PlanException("The network has no links at this network layer");

        final JTextField txt_carriedTraffic = new JTextField();
        final JTextField txt_occupiedCapacity = new JTextField();

        final List<JComboBox> seqLinks_cmb = new LinkedList<JComboBox>();
        final JPanel seqLinks_pnl = new JPanel();
        seqLinks_pnl.setLayout(new BoxLayout(seqLinks_pnl, BoxLayout.Y_AXIS));

        demandSelector.addItemListener(e ->
        {
            StringLabeller item = (StringLabeller) e.getItem();
            long demandId = (Long) item.getObject();
            Demand demand = netPlan.getDemandFromId(demandId);

            double h_d = demand.getOfferedTraffic();
            double r_d = demand.getCarriedTraffic();
            txt_carriedTraffic.setText(Double.toString(Math.max(0, h_d - r_d)));
            txt_occupiedCapacity.setText(Double.toString(Math.max(0, h_d - r_d)));

            seqLinks_cmb.clear();
            seqLinks_pnl.removeAll();

            Node ingressNode = demand.getIngressNode();
            String ingressNodeName = ingressNode.getName();

            Collection<Link> outgoingLinks = ingressNode.getOutgoingLinks(layer);
            final JComboBox<StringLabeller> firstLink = new WiderJComboBox();
            for (Link link : outgoingLinks)
            {
                long destinationNodeId = link.getDestinationNode().getId();
                String destinationNodeName = link.getDestinationNode().getName();
                firstLink.addItem(StringLabeller.of(link.getId(), String.format("e%d: n%d (%s) => n%d (%s)", link.getId(), ingressNode.getId(), ingressNodeName, destinationNodeId, destinationNodeName)));
            }

            firstLink.addItemListener(e1 ->
            {
                JComboBox me = (JComboBox) e1.getSource();
                Iterator<JComboBox> it = seqLinks_cmb.iterator();
                while (it.hasNext()) if (it.next() == me) break;

                while (it.hasNext())
                {
                    JComboBox aux = it.next();
                    seqLinks_pnl.remove(aux);
                    it.remove();
                }

                seqLinks_pnl.revalidate();

                List<Link> seqLinks = new LinkedList<Link>();
                for (JComboBox link : seqLinks_cmb)
                    seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));
                callback.putTransientColorInElementTopologyCanvas(seqLinks, Color.BLUE);
            });

            setMaxSize(firstLink);

            seqLinks_cmb.add(firstLink);
            seqLinks_pnl.add(firstLink);

            JPanel pane = new JPanel(new FlowLayout());
            JButton addLink_btn = new JButton("Add new link");
            addLink_btn.addActionListener(e1 ->
            {
                long linkId = (Long) ((StringLabeller) seqLinks_cmb.get(seqLinks_cmb.size() - 1).getSelectedItem()).getObject();
                Link link = netPlan.getLinkFromId(linkId);
                long destinationNodeId = link.getDestinationNode().getId();
                String destinationNodeName = link.getDestinationNode().getName();

                Set<Link> outgoingLinks1 = link.getDestinationNode().getOutgoingLinks(layer);
                if (outgoingLinks1.isEmpty())
                {
                    ErrorHandling.showErrorDialog("Last node has no outgoing links", "Error");
                    return;
                }

                final JComboBox<StringLabeller> newLink = new WiderJComboBox();
                for (Link nextLink : outgoingLinks1)
                {
                    long nextDestinationNodeId = nextLink.getDestinationNode().getId();
                    String nextDestinationNodeName = nextLink.getDestinationNode().getName();
                    newLink.addItem(StringLabeller.of(nextLink.getId(), String.format("e%d: n%d (%s) => n%d (%s)", nextLink.getId(), destinationNodeId, destinationNodeName, nextDestinationNodeId, nextDestinationNodeName)));
                }

                newLink.addItemListener(e2 ->
                {
                    JComboBox me = (JComboBox) e2.getSource();
                    Iterator<JComboBox> it = seqLinks_cmb.iterator();
                    while (it.hasNext()) if (it.next() == me) break;

                    while (it.hasNext())
                    {
                        JComboBox aux = it.next();
                        seqLinks_pnl.remove(aux);
                        it.remove();
                    }

                    seqLinks_pnl.revalidate();

                    List<Link> seqLinks = new LinkedList<Link>();
                    for (JComboBox link1 : seqLinks_cmb)
                        seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link1.getSelectedItem()).getObject()));
                    callback.putTransientColorInElementTopologyCanvas(seqLinks, Color.BLUE);
                });

                setMaxSize(newLink);

                seqLinks_cmb.add(newLink);
                seqLinks_pnl.add(newLink, seqLinks_pnl.getComponentCount() - 1);
                seqLinks_pnl.revalidate();

                List<Link> seqLinks = new LinkedList<Link>();
                for (JComboBox auxLink : seqLinks_cmb)
                {
                    seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) auxLink.getSelectedItem()).getObject()));
                }
                callback.putTransientColorInElementTopologyCanvas(seqLinks, Color.BLUE);
            });

            pane.add(addLink_btn);

            JButton removeLink_btn = new JButton("Remove last link");
            removeLink_btn.addActionListener(e1 ->
            {
                if (seqLinks_cmb.size() < 2)
                {
                    ErrorHandling.showErrorDialog("Initial link cannot be removed", "Error");
                    return;
                }

                JComboBox cmb = seqLinks_cmb.get(seqLinks_cmb.size() - 1);
                seqLinks_cmb.remove(cmb);
                seqLinks_pnl.remove(cmb);
                seqLinks_pnl.revalidate();

                List<Link> seqLinks = new LinkedList<Link>();
                for (JComboBox link : seqLinks_cmb)
                    seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));
                callback.putTransientColorInElementTopologyCanvas(seqLinks, Color.BLUE);
            });

            pane.add(removeLink_btn);
            seqLinks_pnl.add(pane);

            seqLinks_pnl.revalidate();

            List<Link> seqLinks = new LinkedList<Link>();
            for (JComboBox link : seqLinks_cmb)
                seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));
            callback.putTransientColorInElementTopologyCanvas(seqLinks, Color.BLUE);
        });

        for (long demandId : demandIds)
        {
            Demand demand = netPlan.getDemandFromId(demandId);
            Node ingressNode = demand.getIngressNode();
            Node egressNode = demand.getEgressNode();
            long ingressNodeId = ingressNode.getId();
            long egressNodeId = egressNode.getId();

            final String ingressNodeName = ingressNode.getName();
            final String egressNodeName = egressNode.getName();

            final Set<Link> outgoingLinks = ingressNode.getOutgoingLinks(layer);
            if (outgoingLinks.isEmpty()) continue;

            String demandLabel = "Demand " + demandId;
            demandLabel += ": n" + ingressNodeId;
            if (!ingressNodeName.isEmpty()) demandLabel += " (" + ingressNodeName + ")";

            demandLabel += " => n" + egressNodeId;
            if (!egressNodeName.isEmpty()) demandLabel += " (" + egressNodeName + ")";

            double h_d = demand.getOfferedTraffic();
            double r_d = demand.getCarriedTraffic();

            demandLabel += ", offered traffic = " + h_d;
            demandLabel += ", carried traffic = " + r_d;

            demandSelector.addItem(StringLabeller.of(demandId, demandLabel));
        }

        if (demandSelector.getItemCount() == 0) throw new Net2PlanException("Bad - There are no demands in this layer");

        demandSelector.setSelectedIndex(0);

        final JScrollPane scrollPane = new JScrollPane(seqLinks_pnl, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Sequence of links"));
        scrollPane.setAlignmentY(JScrollPane.TOP_ALIGNMENT);

        final JPanel pane = new JPanel(new MigLayout("fill", "[][grow]", "[][grow][]"));
        pane.add(new JLabel("Demand"));
        pane.add(demandSelector, "growx, wrap, wmin 50");
        pane.add(scrollPane, "grow, spanx 2, wrap");
        pane.add(new JLabel("Carried traffic"));
        pane.add(txt_carriedTraffic, "grow, wrap");
        pane.add(new JLabel("Occupied capacity"));
        pane.add(txt_occupiedCapacity, "grow");
        pane.setPreferredSize(new Dimension(400, 400));

        while (true)
        {
            int result = JOptionPane.showConfirmDialog(null, pane, "Add new route", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) break;

            long demandId = (Long) ((StringLabeller) demandSelector.getSelectedItem()).getObject();

            double carriedTraffic;
            double occupiedCapacity = -1;

            try
            {
                carriedTraffic = Double.parseDouble(txt_carriedTraffic.getText());
                if (carriedTraffic < 0) throw new RuntimeException();
            } catch (Throwable e)
            {
                ErrorHandling.showErrorDialog("Carried traffic must be a non-negative number", "Error adding route");
                continue;
            }

            try
            {
                if (!txt_occupiedCapacity.getText().isEmpty())
                {
                    occupiedCapacity = Double.parseDouble(txt_occupiedCapacity.getText());
                    if (occupiedCapacity < 0 && occupiedCapacity != -1) throw new RuntimeException();
                }
            } catch (Throwable e)
            {
                ErrorHandling.showErrorDialog("Occupied capacity must be a non-negative number, -1 or empty", "Error adding route");
                continue;
            }

            List<Link> seqLinks = new LinkedList<Link>();
            for (JComboBox link : seqLinks_cmb)
                seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));

            try
            {
                netPlan.addRoute(netPlan.getDemandFromId(demandId), carriedTraffic, occupiedCapacity, seqLinks, null);
            } catch (Throwable e)
            {
                ErrorHandling.showErrorDialog(e.getMessage(), "Error adding route");
                continue;
            }

            break;
        }
        callback.resetPickedStateAndUpdateView();
    }

    private static void setMaxSize(JComponent c)
    {
        final Dimension max = c.getMaximumSize();
        final Dimension pref = c.getPreferredSize();

        max.height = pref.height;
        c.setMaximumSize(max);
    }


    private class RouteSPFActionListener implements ActionListener
    {
        final boolean isMinHops;
        final boolean add11LinkDisjointSegment;

        private RouteSPFActionListener(boolean isMinHops, boolean minCost)
        {
            this.isMinHops = isMinHops;
            this.add11LinkDisjointSegment = minCost;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            NetPlan netPlan = callback.getDesign();
            List<Link> links = netPlan.getLinks();
            final int E = links.size();
            Map<Link, Double> linkCostMap = new HashMap<Link, Double>();
            List<Route> addedRoutes = new LinkedList<Route>();

            // Ask for current element removal
            if (netPlan.hasRoutes(getTableNetworkLayer()))
            {
                final int answer = JOptionPane.showConfirmDialog(null, "Remove all existing routes?", "", JOptionPane.YES_NO_CANCEL_OPTION);
                if (answer == JOptionPane.OK_OPTION) netPlan.removeAllRoutes(getTableNetworkLayer());
                if (answer == JOptionPane.CANCEL_OPTION || answer == JOptionPane.CLOSED_OPTION) return;
            }

            for (Link link : netPlan.getLinks())
            {
                linkCostMap.put(link, isMinHops ? 1 : link.getLengthInKm());
            }
            DoubleMatrix1D linkCostVector = null;
            if (isMinHops)
                linkCostVector = DoubleFactory1D.dense.make(netPlan.getLinks().size(), 1.0);
            else
            {
                linkCostVector = netPlan.getVectorLinkLengthInKm();
            }

            for (Demand d : netPlan.getDemands())
            {
                if (add11LinkDisjointSegment)
                {
                    if (d.isServiceChainRequest())
                    {
                        List<NetworkElement> minCostServiceChain = GraphUtils.getMinimumCostServiceChain(netPlan.getLinks(), d.getIngressNode(), d.getEgressNode(), d.getServiceChainSequenceOfTraversedResourceTypes(), linkCostVector, null, -1, -1, -1).getFirst();
                        if (minCostServiceChain.isEmpty())
                            throw new Net2PlanException("Cannot find a route for demand of index " + d.getIndex() + ". No route is created");
                        DoubleMatrix1D linkCostModified = linkCostVector.copy();
                        Map<Resource, Double> resourceCostModified = new HashMap<Resource, Double>();
                        for (NetworkElement ee : minCostServiceChain)
                            if (ee instanceof Link) linkCostModified.set(ee.getIndex(), Double.MAX_VALUE);
                            else if (ee instanceof Resource)
                                resourceCostModified.put((Resource) ee, Double.MAX_VALUE);
                        List<NetworkElement> minCostServiceChain2 =
                                GraphUtils.getMinimumCostServiceChain(netPlan.getLinks(), d.getIngressNode(),
                                        d.getEgressNode(), d.getServiceChainSequenceOfTraversedResourceTypes(),
                                        linkCostModified, resourceCostModified, -1, -1, -1).getFirst();
                        if (minCostServiceChain2.isEmpty())
                            throw new Net2PlanException("Could not find two link and resource disjoint routes demand of index " + d.getIndex() + ". No route is created");
                        final Route r = netPlan.addServiceChain(d, d.getOfferedTraffic(),
                                Collections.nCopies(minCostServiceChain.size(), d.getOfferedTraffic()), minCostServiceChain, null);
                        final Route s = netPlan.addServiceChain(d, 0,
                                Collections.nCopies(minCostServiceChain2.size(), d.getOfferedTraffic()), minCostServiceChain2, null);
                        r.addBackupRoute(s);
                        addedRoutes.add(r);
                        addedRoutes.add(s);
                    } else
                    {
                        List<List<Link>> twoPaths = GraphUtils.getTwoLinkDisjointPaths(netPlan.getNodes(), netPlan.getLinks(), d.getIngressNode(), d.getEgressNode(), linkCostMap);
                        if (twoPaths.size() != 2)
                            throw new Net2PlanException("There are no two link disjoint paths for demand of index " + d.getIndex() + ". No route or protection segment is created");
                        final Route r = netPlan.addRoute(d, d.getOfferedTraffic(), d.getOfferedTraffic(), twoPaths.get(0), null);
                        final Route s = netPlan.addRoute(d, 0, d.getOfferedTraffic(), twoPaths.get(1), null);
                        r.addBackupRoute(s);
                        addedRoutes.add(r);
                        addedRoutes.add(s);
                    }
                } else
                {
                    List<NetworkElement> minCostServiceChain = GraphUtils.getMinimumCostServiceChain(netPlan.getLinks(), d.getIngressNode(), d.getEgressNode(), d.getServiceChainSequenceOfTraversedResourceTypes(), linkCostVector, null, -1, -1, -1).getFirst();
                    if (minCostServiceChain.isEmpty())
                        throw new Net2PlanException("Cannot find a route for demand of index " + d.getIndex() + ". No route is created");
                    Route r = netPlan.addServiceChain(d, d.getOfferedTraffic(),
                            Collections.nCopies(minCostServiceChain.size(), d.getOfferedTraffic()),
                            minCostServiceChain, null);
                    addedRoutes.add(r);
                }
            }
        }

    }


    private static void viewEditBackupRoutesGUI(final GUINetworkDesign callback, Route route)
    {
        final NetPlan netPlan = callback.getDesign();

        if (route.isBackupRoute()) throw new Net2PlanException("A backup route cannot have backup routes itself.");

        long routeId = route.getId();

        Set<Route> candidateBackupRoutes = route.getDemand().getRoutesAreNotBackup();
        List<Route> currentBackupRoutes = route.getBackupRoutes();

        final List<NetworkElement> seqLinksAndResources = route.getPath();

        if (candidateBackupRoutes.isEmpty())
            throw new Net2PlanException("No backup route can be applied to this route");

        candidateBackupRoutes.removeAll(currentBackupRoutes);

        final JComboBox<StringLabeller> backupRouteSelector = new WiderJComboBox();

        final DefaultTableModel model = new ClassAwareTableModel(new Object[1][6], new String[]{"Id", "Seq. links/resources", "Seq. nodes", "Seq. occupied capacities", "", ""})
        {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return columnIndex == 4 || columnIndex == 5;
            }
        };
        final JTable table = new AdvancedJTable(model);
        table.setEnabled(false);

        final JPanel addSegment_pnl = new JPanel(new MigLayout("", "[grow][][]", "[]"));
        JButton addSegment_btn = new JButton("Add");
        addSegment_btn.addActionListener(e ->
        {
            Object selectedItem = backupRouteSelector.getSelectedItem();
            long backupRouteId = (Long) ((StringLabeller) selectedItem).getObject();
            Route backupRoute = netPlan.getRouteFromId(backupRouteId);
            route.addBackupRoute(backupRoute);
            callback.getPickManager().reset();
            callback.updateVisualizationAfterChanges();
            callback.addNetPlanChange();

            backupRouteSelector.removeItem(selectedItem);
            if (backupRouteSelector.getItemCount() == 0) addSegment_pnl.setVisible(false);

            if (!table.isEnabled()) model.removeRow(0);
            model.addRow(new Object[]
                    {backupRouteId,
                            getSequenceLinkResourceIndexes(backupRoute),
                            getSequenceNodeIndexesWithResourceInfo(backupRoute),
                            getSequenceOccupiedCapacities(backupRoute), "Remove", "View"});
            table.setEnabled(true);
        });

        JButton viewSegment_btn1 = new JButton("View");
        viewSegment_btn1.addActionListener(e ->
        {
            Object selectedItem = backupRouteSelector.getSelectedItem();
            long backupRouteId = (Long) ((StringLabeller) selectedItem).getObject();
            List<NetworkElement> backupRoutePath = netPlan.getRouteFromId(backupRouteId).getPath();
            callback.putTransientColorInElementTopologyCanvas(seqLinksAndResources, Color.ORANGE);
            callback.putTransientColorInElementTopologyCanvas(backupRoutePath, Color.ORANGE);
        });

        addSegment_pnl.add(backupRouteSelector, "growx, wmin 50");
        addSegment_pnl.add(addSegment_btn);
        addSegment_pnl.add(viewSegment_btn1);

        Action delete = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    JTable table = (JTable) e.getSource();
                    int modelRow = Integer.parseInt(e.getActionCommand());

                    final long backupRouteId = (Long) table.getModel().getValueAt(modelRow, 0);
                    final Route backupRoute = netPlan.getRouteFromId(backupRouteId);
                    netPlan.getRouteFromId(routeId).removeBackupRoute(backupRoute);
                    callback.getPickManager().reset();
                    callback.updateVisualizationAfterChanges();
                    callback.addNetPlanChange();

                    String segmentLabel = "Backup route id " + backupRouteId +
                            ": path = " + getSequenceLinkResourceIndexes(backupRoute) +
                            ", seq. nodes = " + getSequenceNodeIndexesWithResourceInfo(backupRoute) +
                            ", occupied capacity = " + getSequenceOccupiedCapacities(backupRoute);

                    backupRouteSelector.addItem(StringLabeller.of(backupRouteId, segmentLabel));

                    ((DefaultTableModel) table.getModel()).removeRow(modelRow);

                    table.setEnabled(true);

                    if (table.getModel().getRowCount() == 0)
                    {
                        ((DefaultTableModel) table.getModel()).addRow(new Object[6]);
                        table.setEnabled(false);
                    }
                } catch (Throwable e1)
                {
                }
            }
        };

        Action view = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    JTable table = (JTable) e.getSource();
                    int modelRow = Integer.parseInt(e.getActionCommand());

                    final long backupRouteId = (Long) table.getModel().getValueAt(modelRow, 0);
                    final Route backupRoute = netPlan.getRouteFromId(backupRouteId);
                    callback.putTransientColorInElementTopologyCanvas(seqLinksAndResources, Color.ORANGE);
                    callback.putTransientColorInElementTopologyCanvas(backupRoute.getPath(), Color.ORANGE);
                } catch (Throwable ignored)
                {
                }
            }
        };

        new ButtonColumn(table, delete, 4);
        new ButtonColumn(table, view, 5);

        final JScrollPane scrollPane = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Current backup segment list"));
        scrollPane.setAlignmentY(JScrollPane.TOP_ALIGNMENT);

        final JDialog dialog = new JDialog();
        dialog.setLayout(new BorderLayout());
        dialog.add(addSegment_pnl, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);

        for (Route backupRoute : candidateBackupRoutes)
        {
            String segmentLabel = "Backup route id " + backupRoute.getId() +
                    ": path = " + getSequenceLinkResourceIndexes(backupRoute) +
                    ", seq. nodes = " + getSequenceNodeIndexesWithResourceInfo(backupRoute) +
                    ", occupied capacity = " + getSequenceOccupiedCapacities(backupRoute);
            backupRouteSelector.addItem(StringLabeller.of(backupRoute.getId(), segmentLabel));
        }

        if (backupRouteSelector.getItemCount() == 0)
        {
            addSegment_pnl.setVisible(false);
        } else
        {
            backupRouteSelector.setSelectedIndex(0);
        }

        if (!currentBackupRoutes.isEmpty())
        {
            model.removeRow(0);

            for (Route backupRoute : currentBackupRoutes)
            {
                model.addRow(new Object[]{backupRoute.getId(),
                        getSequenceLinkResourceIndexes(backupRoute), getSequenceNodeIndexesWithResourceInfo(backupRoute),
                        getSequenceOccupiedCapacities(backupRoute), "Remove", "View"});
            }

            table.setEnabled(true);
        }

//        table.setDefaultRenderer(Boolean.class, new CellRenderers.UnfocusableCellRenderer());
//        table.setDefaultRenderer(Double.class, new CellRenderers.UnfocusableCellRenderer());
//        table.setDefaultRenderer(Object.class, new CellRenderers.UnfocusableCellRenderer());
//        table.setDefaultRenderer(Float.class, new CellRenderers.UnfocusableCellRenderer());
//        table.setDefaultRenderer(Long.class, new CellRenderers.UnfocusableCellRenderer());
//        table.setDefaultRenderer(Integer.class, new CellRenderers.UnfocusableCellRenderer());
//        table.setDefaultRenderer(String.class, new CellRenderers.UnfocusableCellRenderer());

        double x_p = netPlan.getRouteFromId(routeId).getCarriedTraffic();
        dialog.setTitle("View/edit backup route list for route " + routeId + " (carried traffic = " + x_p + ", occupied capacity = " + getSequenceOccupiedCapacities(netPlan.getRouteFromId(routeId)) + ")");
        SwingUtils.configureCloseDialogOnEscape(dialog);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(new Dimension(500, 300));
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);

        callback.resetPickedStateAndUpdateView();
    }


    private static String getSequenceLinkResourceIndexes(Route r)
    {
        StringBuffer buf = new StringBuffer();
        for (NetworkElement e : r.getPath())
            if (e instanceof Link) buf.append("L" + e.getIndex() + ",");
            else if (e instanceof Resource) buf.append("R" + e.getIndex() + ",");
        buf.setLength(buf.length() - 1);
        return buf.toString();
    }

    private static String getSequenceNodeIndexesWithResourceInfo(Route r)
    {
        StringBuffer buf = new StringBuffer();
        buf.append("N" + r.getIngressNode().getIndex());
        for (NetworkElement e : r.getPath())
            if (e instanceof Link) buf.append(",N" + ((Link) e).getDestinationNode().getIndex());
            else if (e instanceof Resource) buf.append(",(R" + e.getIndex() + ")");
        return buf.toString();
    }

    private static String getSequenceOccupiedCapacities(Route r)
    {
        if (r.isDown()) return "0";
        if (r.getSeqOccupiedCapacitiesIfNotFailing().equals(Collections.nCopies(r.getPath().size(), r.getOccupiedCapacity())))
            return "" + r.getOccupiedCapacity();
        return CollectionUtils.join(r.getSeqOccupiedCapacitiesIfNotFailing(), ", ");
    }
}
