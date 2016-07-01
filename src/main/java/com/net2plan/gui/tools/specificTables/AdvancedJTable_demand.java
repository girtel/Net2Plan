/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/


package com.net2plan.gui.tools.specificTables;

import com.net2plan.gui.tools.IGUINetworkViewer;
import com.net2plan.gui.utils.*;
import com.net2plan.gui.utils.CellRenderers.NumberCellRenderer;
import com.net2plan.gui.utils.topology.INetworkCallback;
import com.net2plan.gui.utils.topology.TopologyPanel;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.StringUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 */
public class AdvancedJTable_demand extends AdvancedJTableNetworkElement {
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_INDEX = 1;
    private static final int COLUMN_INGRESSNODE = 2;
    private static final int COLUMN_EGRESSNODE = 3;
    private static final int COLUMN_COUPLEDTOLINK = 4;
    private static final int COLUMN_OFFEREDTRAFFIC = 5;
    private static final int COLUMN_CARRIEDTRAFFIC = 6;
    private static final int COLUMN_LOSTTRAFFIC = 7;
    private static final int COLUMN_ROUTINGCYCLES = 8;
    private static final int COLUMN_BIFURCATED = 9;
    private static final int COLUMN_NUMROUTES = 10;
    private static final int COLUMN_MAXE2ELATENCY = 11;
    private static final int COLUMN_ATTRIBUTES = 12;
    private static final String netPlanViewTabName = "Demands";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "Ingress node", "Egress node", "Coupled to link",
            "Offered traffic", "Carried traffic", "% Lost traffic", "Routing cycles", "Bifurcated", "# Routes", "Max e2e latency (ms)", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)", "Index (consecutive integer starting in zero)", "Ingress node", "Egress node", "Indicates the coupled upper layer link, if any, or empty", "Offered traffic by the demand", "Carried traffic by routes carrying traffic from the demand", "Percentage of lost traffic from the offered", "Indicates whether there are routing cycles: loopless (no cycle in some route), open cycles (traffic reaches egress node after some cycles in some route), closed cycles (traffic does not reach the egress node in some route)", "Indicates whether the demand has more than one associated route carrying traffic", "Number of associated routes", "Maximum end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any)", "Demand-specific attributes");

    /**
     * Default constructor.
     *
     * @since 0.2.0
     */
    public AdvancedJTable_demand(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        super(createTableModel(networkViewer, topologyPanel), networkViewer, NetworkElementType.DEMAND);
        setDefaultCellRenderers(networkViewer);
        setSpecificCellRenderers();
        setColumnRowSorting(networkViewer.inOnlineSimulationMode());
    }

    public String getTabName() {
        return netPlanViewTabName;
    }

    public List<Object[]> getAllData(NetPlan currentState, TopologyPanel topologyPanel, NetPlan initialState) {
        List<Object[]> allDemandData = new LinkedList<Object[]>();
        for (Demand demand : currentState.getDemands()) {
            Set<Route> routes_thisDemand = currentState.getRoutingType() == RoutingType.SOURCE_ROUTING ? demand.getRoutes() : new LinkedHashSet<Route>();
            Link coupledLink = demand.getCoupledLink();
            Node ingressNode = demand.getIngressNode();
            Node egressNode = demand.getEgressNode();
            double h_d = demand.getOfferedTraffic();
            double lostTraffic_d = demand.getBlockedTraffic();
            Object[] demandData = new Object[netPlanViewTableHeader.length];
            demandData[0] = demand.getId();
            demandData[1] = demand.getIndex();
            demandData[2] = ingressNode.getIndex() + (ingressNode.getName().isEmpty() ? "" : " (" + ingressNode.getName() + ")");
            demandData[3] = egressNode.getIndex() + (egressNode.getName().isEmpty() ? "" : " (" + egressNode.getName() + ")");
            demandData[4] = coupledLink == null ? "" : "e" + coupledLink.getIndex() + " (layer " + coupledLink.getLayer() + ")";
            demandData[5] = h_d;
            demandData[6] = demand.getCarriedTraffic();
            demandData[7] = h_d == 0 ? 0 : 100 * lostTraffic_d / h_d;
            demandData[8] = demand.getRoutingCycleType();
            demandData[9] = currentState.getRoutingType() == RoutingType.HOP_BY_HOP_ROUTING ? "-" : (demand.isBifurcated()) ? String.format("Yes (%d)", demand.getRoutes().size()) : "No";
            demandData[10] = routes_thisDemand.isEmpty() ? "none" : routes_thisDemand.size() + " (" + CollectionUtils.join(NetPlan.getIndexes(routes_thisDemand), ",") + ")";
            demandData[11] = demand.getWorseCasePropagationTimeInMs();
            demandData[12] = StringUtils.mapToString(demand.getAttributes());
            allDemandData.add(demandData);

            if (initialState != null && initialState.getDemandFromId(demand.getId()) != null) {
                demand = initialState.getDemandFromId(demand.getId());
                routes_thisDemand = initialState.getRoutingType() == RoutingType.SOURCE_ROUTING ? demand.getRoutes() : new LinkedHashSet<Route>();
                coupledLink = demand.getCoupledLink();
                ingressNode = demand.getIngressNode();
                egressNode = demand.getEgressNode();
                h_d = demand.getOfferedTraffic();

                Object[] demandData_initialNetPlan = new Object[netPlanViewTableHeader.length];
                demandData_initialNetPlan[0] = null;
                demandData_initialNetPlan[1] = null;
                demandData_initialNetPlan[2] = null;
                demandData_initialNetPlan[3] = null;
                demandData_initialNetPlan[5] = h_d;
                demandData_initialNetPlan[6] = demand.getCarriedTraffic();
                demandData_initialNetPlan[7] = h_d == 0 ? 0 : 100 * lostTraffic_d / h_d;
                demandData_initialNetPlan[8] = demand.getRoutingCycleType();
                demandData_initialNetPlan[9] = initialState.getRoutingType() == RoutingType.HOP_BY_HOP_ROUTING ? "-" : (demand.isBifurcated()) ? String.format("Yes (%d)", demand.getRoutes().size()) : "No";
                demandData_initialNetPlan[10] = routes_thisDemand.isEmpty() ? "none" : routes_thisDemand.size() + " (" + CollectionUtils.join(NetPlan.getIndexes(routes_thisDemand), ",") + ")";
                demandData_initialNetPlan[11] = demand.getWorseCasePropagationTimeInMs();
                demandData_initialNetPlan[12] = StringUtils.mapToString(demand.getAttributes());
                allDemandData.add(demandData_initialNetPlan);
            }
        }

        return allDemandData;
    }

    public String[] getTableHeaders() {
        return netPlanViewTableHeader;
    }

    public String[] getTableTips() {
        return netPlanViewTableTips;
    }

    public boolean hasElements(NetPlan np) {
        return np.hasDemands();
    }

    public int[] getColumnsOfSpecialComparatorForSorting() {
        return new int[]{COLUMN_INGRESSNODE, COLUMN_EGRESSNODE, COLUMN_COUPLEDTOLINK, COLUMN_BIFURCATED, COLUMN_NUMROUTES};
    } //{ return new int [] { 3,4,5,9,10 }; }

    private static TableModel createTableModel(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        TableModel demandTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (getValueAt(rowIndex, columnIndex) == null) return false;
                if (!networkViewer.isEditable()) return false;
                return columnIndex == COLUMN_OFFEREDTRAFFIC;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column) {
                Object oldValue = getValueAt(row, column);

				/* If value doesn't change, exit from function */
                if (newValue.equals(oldValue)) return;

                NetPlan netPlan = networkViewer.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long demandId = (Long) getValueAt(row, 0);
                final Demand demand = netPlan.getDemandFromId(demandId);

				/* Perform checks, if needed */
                try {
                    switch (column) {
                        case COLUMN_OFFEREDTRAFFIC:
                            demand.setOfferedTraffic(Double.parseDouble(newValue.toString()));
                            networkViewer.updateNetPlanView();
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying demand");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };
        return demandTableModel;
    }

    private void setDefaultCellRenderers(final IGUINetworkViewer networkViewer) {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());
    }

    private void setSpecificCellRenderers() {
        getColumnModel().getColumn(this.convertColumnIndexToView(COLUMN_LOSTTRAFFIC)).setCellRenderer(new CellRenderers.LostTrafficCellRenderer(COLUMN_OFFEREDTRAFFIC));
    }

    public void setColumnRowSorting(boolean allowShowInitialNetPlan) {
        if (allowShowInitialNetPlan) setRowSorter(new CurrentAndPlannedStateTableSorter(getModel()));
        else setAutoCreateRowSorter(true);
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_INGRESSNODE, new IGUINetworkViewer.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_EGRESSNODE, new IGUINetworkViewer.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_NUMROUTES, new IGUINetworkViewer.ColumnComparator());
    }

    public int getNumFixedLeftColumnsInDecoration() {
        return 2;
    }

    public void doPopup(final MouseEvent e, final int row, final Object itemId) {
        JPopupMenu popup = new JPopupMenu();

        if (networkViewer.isEditable()) {
            popup.add(getAddOption());
            for (JComponent item : getExtraAddOptions())
                popup.add(item);
        }

        if (!isTableEmpty()) {
            if (networkViewer.isEditable()) {
                if (row != -1) {
                    if (popup.getSubElements().length > 0) popup.addSeparator();

                    JMenuItem removeItem = new JMenuItem("Remove " + networkElementType);

                    removeItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NetPlan netPlan = networkViewer.getDesign();

                            try {
                                netPlan.getDemandFromId((long) itemId).remove();
                                networkViewer.updateNetPlanView();
                            } catch (Throwable ex) {
                                ErrorHandling.addErrorOrException(ex, getClass());
                                ErrorHandling.showErrorDialog("Unable to remove " + networkElementType);
                            }
                        }
                    });

                    popup.add(removeItem);

                    addPopupMenuAttributeOptions(e, row, itemId, popup);
                }

                JMenuItem removeItems = new JMenuItem("Remove all " + networkElementType + "s");
                removeItems.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        NetPlan netPlan = networkViewer.getDesign();

                        try {
                            netPlan.removeAllDemands();
                            networkViewer.updateNetPlanView();
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to remove all " + networkElementType + "s");
                        }
                    }
                });

                popup.add(removeItems);

                List<JComponent> extraOptions = getExtraOptions(row, itemId);
                if (!extraOptions.isEmpty()) {
                    if (popup.getSubElements().length > 0) popup.addSeparator();
                    for (JComponent item : extraOptions) popup.add(item);
                }
            }

            List<JComponent> forcedOptions = getForcedOptions();
            if (!forcedOptions.isEmpty()) {
                if (popup.getSubElements().length > 0) popup.addSeparator();
                for (JComponent item : forcedOptions) popup.add(item);
            }
        }

        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    public void showInCanvas(MouseEvent e, Object itemId) {
        if (e.getClickCount() == 1) {
            networkViewer.showDemand((long) itemId);
            return;
        }

        // Two clicks
        int col = convertColumnIndexToModel(columnAtPoint(e.getPoint()));
        if (col == -1 || col >= getColumnCount()) return;

        Demand demand = networkViewer.getDesign().getDemandFromId((long) itemId);
        Node ingressNode = demand.getIngressNode();
        Node egressNode = demand.getEgressNode();
        switch (col) {
            case COLUMN_INGRESSNODE:
                networkViewer.showNode(ingressNode.getId());
                break;
            case COLUMN_EGRESSNODE:
                networkViewer.showNode(egressNode.getId());
                break;
            case COLUMN_COUPLEDTOLINK:
                if (demand.isCoupled()) {
                    networkViewer.showLink(demand.getCoupledLink().getId());
                }
                break;
            default:
                break;
        }
    }

    private List<JComponent> getExtraAddOptions() {
        List<JComponent> options = new LinkedList<JComponent>();
        NetPlan netPlan = networkViewer.getDesign();

        if (netPlan.getNumberOfNodes() >= 2) {
            final JMenuItem oneDemandPerNodePair = new JMenuItem("Add one demand per node pair");
            options.add(oneDemandPerNodePair);

            oneDemandPerNodePair.addActionListener(new FullMeshTrafficActionListener());
        }

        return options;
    }


    private JMenuItem getAddOption() {
        final NetworkElementType networkElementType = NetworkElementType.DEMAND;
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = networkViewer.getDesign();

                try {
                    createLinkDemandGUI(networkElementType, networkViewer, networkViewer.getTopologyPanel());
                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });

        if (networkViewer.getDesign().getNumberOfNodes() < 2) addItem.setEnabled(false);

        return addItem;

    }

    public static void createLinkDemandGUI(final NetworkElementType networkElementType, final INetworkCallback networkViewer, final TopologyPanel topologyPanel) {
        final NetPlan netPlan = networkViewer.getDesign();
        final JComboBox originNodeSelector = new WiderJComboBox();
        final JComboBox destinationNodeSelector = new WiderJComboBox();

        for (Node node : netPlan.getNodes()) {
            final String nodeName = node.getName();
            String nodeLabel = "Node " + node.getIndex();
            if (!nodeName.isEmpty()) nodeLabel += " (" + nodeName + ")";

            originNodeSelector.addItem(StringLabeller.of(node.getId(), nodeLabel));
            destinationNodeSelector.addItem(StringLabeller.of(node.getId(), nodeLabel));
        }

        ItemListener nodeListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                long originNodeId = (long) ((StringLabeller) originNodeSelector.getSelectedItem()).getObject();
                long destinationNodeId = (long) ((StringLabeller) destinationNodeSelector.getSelectedItem()).getObject();
                Map<Node, Color> nodePair = new HashMap<Node, Color>();
                nodePair.put(netPlan.getNodeFromId(originNodeId), Color.GREEN);
                nodePair.put(netPlan.getNodeFromId(destinationNodeId), Color.CYAN);
                topologyPanel.getCanvas().showNodes(nodePair);
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

        while (true) {
            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter end nodes for the new " + networkElementType, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            try {
                long originNodeId = (long) ((StringLabeller) originNodeSelector.getSelectedItem()).getObject();
                long destinationNodeId = (long) ((StringLabeller) destinationNodeSelector.getSelectedItem()).getObject();
                Node originNode = netPlan.getNodeFromId(originNodeId);
                Node destinationNode = netPlan.getNodeFromId(destinationNodeId);

                if (netPlan.getNodeFromId(originNodeId) == null)
                    throw new Net2PlanException("Node of id: " + originNodeId + " does not exist");
                if (netPlan.getNodeFromId(destinationNodeId) == null)
                    throw new Net2PlanException("Node of id: " + destinationNodeId + " does not exist");

                if (networkElementType == NetworkElementType.LINK) {
                    Link link = netPlan.addLink(originNode, destinationNode, 0, 200000, netPlan.getNodePairEuclideanDistance(originNode, destinationNode), null);
                    topologyPanel.getCanvas().addLink(link);
                    topologyPanel.getCanvas().refresh();
                } else {
                    netPlan.addDemand(originNode, destinationNode, 0, null);
                }

                break;
            } catch (Throwable ex) {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Error adding " + networkElementType);
            }
        }
    }

    private class FullMeshTrafficActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            NetPlan netPlan = networkViewer.getDesign();

            int result = JOptionPane.showConfirmDialog(null, "Remove all existing demands?", "", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.CLOSED_OPTION) return;
            else if (result == JOptionPane.YES_OPTION) netPlan.removeAllDemands();

            for (long nodeId_1 : netPlan.getNodeIds()) {
                for (long nodeId_2 : netPlan.getNodeIds()) {
                    if (nodeId_1 >= nodeId_2) continue;
                    Node n1 = netPlan.getNodeFromId(nodeId_1);
                    Node n2 = netPlan.getNodeFromId(nodeId_2);

                    netPlan.addDemandBidirectional(n1, n2, 0, null);
                }
            }

            networkViewer.updateNetPlanView();
        }
    }


    private boolean isTableEmpty() {
        return !networkViewer.getDesign().hasDemands();
    }

    private List<JComponent> getExtraOptions(final int row, final Object itemId) {
        List<JComponent> options = new LinkedList<JComponent>();
        final int numRows = model.getRowCount();
        final NetPlan netPlan = networkViewer.getDesign();

        JMenuItem offeredTrafficToAll = new JMenuItem("Set offered traffic to all");
        offeredTrafficToAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double h_d;

                while (true) {
                    String str = JOptionPane.showInputDialog(null, "Offered traffic volume", "Set traffic value to all demands", JOptionPane.QUESTION_MESSAGE);
                    if (str == null) return;

                    try {
                        h_d = Double.parseDouble(str);
                        if (h_d < 0) throw new RuntimeException();

                        break;
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog("Please, introduce a non-negative number", "Error setting offered traffic");
                    }
                }

                NetPlan netPlan = networkViewer.getDesign();

                try {
                    Collection<Long> demandIds = netPlan.getDemandIds();
                    for (long demandId : demandIds) netPlan.getDemandFromId(demandId).setOfferedTraffic(h_d);

                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set offered traffic to all demands");
                }
            }
        });
        options.add(offeredTrafficToAll);

        JMenuItem scaleOfferedTrafficToAll = new JMenuItem("Scale offered traffic all demands");
        scaleOfferedTrafficToAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double scalingFactor;

                while (true) {
                    String str = JOptionPane.showInputDialog(null, "Scaling factor to multiply to all offered traffics", "Scale offered traffic", JOptionPane.QUESTION_MESSAGE);
                    if (str == null) return;

                    try {
                        scalingFactor = Double.parseDouble(str);
                        if (scalingFactor < 0) throw new RuntimeException();

                        break;
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog("Please, introduce a non-negative number", "Error setting offered traffic");
                    }
                }

                NetPlan netPlan = networkViewer.getDesign();

                try {
                    for (Demand d : netPlan.getDemands()) d.setOfferedTraffic(d.getOfferedTraffic() * scalingFactor);
                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to scale demand offered traffics");
                }
            }
        });
        options.add(scaleOfferedTrafficToAll);

        if (itemId != null && netPlan.isMultilayer()) {
            final long demandId = (long) itemId;
            if (netPlan.getDemandFromId(demandId).isCoupled()) {
                JMenuItem decoupleDemandItem = new JMenuItem("Decouple demand");
                decoupleDemandItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        netPlan.getDemandFromId(demandId).decouple();
                        model.setValueAt("", row, 3);
                        networkViewer.updateWarnings();
                    }
                });

                options.add(decoupleDemandItem);
            } else {
                JMenuItem createUpperLayerLinkFromDemandItem = new JMenuItem("Create upper layer link from demand");
                createUpperLayerLinkFromDemandItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Collection<Long> layerIds = netPlan.getNetworkLayerIds();
                        final JComboBox layerSelector = new WiderJComboBox();
                        for (long layerId : layerIds) {
                            if (layerId == netPlan.getNetworkLayerDefault().getId()) continue;

                            final String layerName = netPlan.getNetworkLayerFromId(layerId).getName();
                            String layerLabel = "Layer " + layerId;
                            if (!layerName.isEmpty()) layerLabel += " (" + layerName + ")";

                            layerSelector.addItem(StringLabeller.of(layerId, layerLabel));
                        }

                        layerSelector.setSelectedIndex(0);

                        JPanel pane = new JPanel();
                        pane.add(new JLabel("Select layer: "));
                        pane.add(layerSelector);

                        while (true) {
                            int result = JOptionPane.showConfirmDialog(null, pane, "Please select the upper layer to create the link", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return;

                            try {
                                long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                                netPlan.getDemandFromId(demandId).coupleToNewLinkCreated(netPlan.getNetworkLayerFromId(layerId));

                                networkViewer.updateNetPlanView();
                                break;
                            } catch (Throwable ex) {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error creating upper layer link from demand");
                            }
                        }
                    }
                });

                options.add(createUpperLayerLinkFromDemandItem);

                JMenuItem coupleDemandToLink = new JMenuItem("Couple demand to upper layer link");
                coupleDemandToLink.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Collection<Long> layerIds = netPlan.getNetworkLayerIds();
                        final JComboBox layerSelector = new WiderJComboBox();
                        final JComboBox linkSelector = new WiderJComboBox();
                        for (long layerId : layerIds) {
                            if (layerId == netPlan.getNetworkLayerDefault().getId()) continue;

                            final String layerName = netPlan.getNetworkLayerFromId(layerId).getName();
                            String layerLabel = "Layer " + layerId;
                            if (!layerName.isEmpty()) layerLabel += " (" + layerName + ")";

                            layerSelector.addItem(StringLabeller.of(layerId, layerLabel));
                        }

                        layerSelector.addItemListener(new ItemListener() {
                            @Override
                            public void itemStateChanged(ItemEvent e) {
                                if (layerSelector.getSelectedIndex() >= 0) {
                                    long selectedLayerId = (Long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                                    NetworkLayer selectedLayer = netPlan.getNetworkLayerFromId(selectedLayerId);

                                    linkSelector.removeAllItems();
                                    Collection<Link> links_thisLayer = netPlan.getLinks(selectedLayer);
                                    for (Link link : links_thisLayer) {
                                        if (link.isCoupled()) continue;

                                        String originNodeName = link.getOriginNode().getName();
                                        String destinationNodeName = link.getDestinationNode().getName();

                                        linkSelector.addItem(StringLabeller.unmodifiableOf(link.getId(), "e" + link.getIndex() + " [n" + link.getOriginNode().getIndex() + " (" + originNodeName + ") -> n" + link.getDestinationNode().getIndex() + " (" + destinationNodeName + ")]"));
                                    }
                                }

                                if (linkSelector.getItemCount() == 0) {
                                    linkSelector.setEnabled(false);
                                } else {
                                    linkSelector.setSelectedIndex(0);
                                    linkSelector.setEnabled(true);
                                }
                            }
                        });

                        layerSelector.setSelectedIndex(-1);
                        layerSelector.setSelectedIndex(0);

                        JPanel pane = new JPanel(new MigLayout("", "[][grow]", "[][]"));
                        pane.add(new JLabel("Select layer: "));
                        pane.add(layerSelector, "growx, wrap");
                        pane.add(new JLabel("Select link: "));
                        pane.add(linkSelector, "growx, wrap");

                        while (true) {
                            int result = JOptionPane.showConfirmDialog(null, pane, "Please select the upper layer link", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return;

                            try {
                                long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                                long linkId;
                                try {
                                    linkId = (long) ((StringLabeller) linkSelector.getSelectedItem()).getObject();
                                } catch (Throwable ex) {
                                    throw new RuntimeException("No link was selected");
                                }

                                netPlan.getDemandFromId(demandId).coupleToUpperLayerLink(netPlan.getLinkFromId(linkId));

                                networkViewer.updateNetPlanView();
                                break;
                            } catch (Throwable ex) {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error coupling upper layer link to demand");
                            }
                        }
                    }
                });

                options.add(coupleDemandToLink);
            }

            if (numRows > 1) {
                JMenuItem decoupleAllDemandsItem = null;
                JMenuItem createUpperLayerLinksFromDemandsItem = null;

                final Set<Long> coupledDemands = new HashSet<Long>();
                for (Demand d : netPlan.getDemands()) if (d.isCoupled()) coupledDemands.add(d.getId());
                if (!coupledDemands.isEmpty()) {
                    decoupleAllDemandsItem = new JMenuItem("Decouple all demands");
                    decoupleAllDemandsItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            for (long demandId : new LinkedHashSet<Long>(coupledDemands))
                                netPlan.getDemandFromId(demandId).decouple();

                            int numRows = model.getRowCount();
                            for (int i = 0; i < numRows; i++) model.setValueAt("", i, 3);

                            networkViewer.updateWarnings();
                        }
                    });
                }

                if (coupledDemands.size() < netPlan.getNumberOfDemands()) {
                    createUpperLayerLinksFromDemandsItem = new JMenuItem("Create upper layer links from uncoupled demands");
                    createUpperLayerLinksFromDemandsItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Collection<Long> layerIds = netPlan.getNetworkLayerIds();
                            final JComboBox layerSelector = new WiderJComboBox();
                            for (long layerId : layerIds) {
                                if (layerId == netPlan.getNetworkLayerDefault().getId()) continue;

                                final String layerName = netPlan.getNetworkLayerFromId(layerId).getName();
                                String layerLabel = "Layer " + layerId;
                                if (!layerName.isEmpty()) layerLabel += " (" + layerName + ")";

                                layerSelector.addItem(StringLabeller.of(layerId, layerLabel));
                            }

                            layerSelector.setSelectedIndex(0);

                            JPanel pane = new JPanel();
                            pane.add(new JLabel("Select layer: "));
                            pane.add(layerSelector);

                            while (true) {
                                int result = JOptionPane.showConfirmDialog(null, pane, "Please select the upper layer to create links", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                                if (result != JOptionPane.OK_OPTION) return;

                                try {
                                    long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                                    NetworkLayer layer = netPlan.getNetworkLayerFromId(layerId);
                                    for (Demand demand : netPlan.getDemands())
                                        if (!demand.isCoupled())
                                            demand.coupleToNewLinkCreated(layer);

                                    networkViewer.updateNetPlanView();
                                    break;
                                } catch (Throwable ex) {
                                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error creating upper layer links");
                                }
                            }
                        }
                    });
                }

                if (!options.isEmpty() && (decoupleAllDemandsItem != null || createUpperLayerLinksFromDemandsItem != null)) {
                    options.add(new JPopupMenu.Separator());
                    if (decoupleAllDemandsItem != null) options.add(decoupleAllDemandsItem);
                    if (createUpperLayerLinksFromDemandsItem != null) options.add(createUpperLayerLinksFromDemandsItem);
                }

            }
        }
        return options;
    }

    private List<JComponent> getForcedOptions() {
        return new LinkedList<JComponent>();
    }
}
