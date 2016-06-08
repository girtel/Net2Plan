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
import com.net2plan.utils.StringUtils;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.*;

/**
 */
public class AdvancedJTable_multicastDemand extends AdvancedJTableNetworkElement {
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_INDEX = 1;
    private static final int COLUMN_INGRESSNODE = 2;
    private static final int COLUMN_EGRESSNODES = 3;
    private static final int COLUMN_COUPLEDTOLINKS = 4;
    private static final int COLUMN_OFFEREDTRAFFIC = 5;
    private static final int COLUMN_CARRIEDTRAFFIC = 6;
    private static final int COLUMN_LOSTTRAFFIC = 7;
    private static final int COLUMN_ROUTINGCYCLES = 8;
    private static final int COLUMN_BIFURCATED = 9;
    private static final int COLUMN_NUMTREES = 10;
    private static final int COLUMN_MAXE2ELATENCY = 11;
    private static final int COLUMN_ATTRIBUTES = 12;
    private static final String netPlanViewTabName = "Multicast demands";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "Ingress node", "Egress nodes", "Coupled to links",
            "Offered traffic", "Carried traffic", "% Lost traffic", "Routing cycles", "Bifurcated", "# Multicast trees", "Max e2e latency (ms)", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)", "Index (consecutive integer starting in zero)", "Ingress node", "Egress nodes", "Indicates the coupled upper layer links, if any, or empty", "Offered traffic by the multicast demand", "Carried traffic by multicast trees carrying demand traffic", "Percentage of lost traffic from the offered", "Indicates whether there are routing cycles: always loopless since we always deal with multicast trees", "Indicates whether the demand has more than one associated multicast tree carrying traffic", "Number of associated multicast trees", "Maximum end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any)", "Multicast demand-specific attributes");

    public AdvancedJTable_multicastDemand(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        super(createTableModel(networkViewer, topologyPanel), networkViewer, NetworkElementType.MULTICAST_DEMAND);
        setDefaultCellRenderers(networkViewer);
        setSpecificCellRenderers();
        setColumnRowSorting(networkViewer.inOnlineSimulationMode());

    }

    public List<Object[]> getAllData(NetPlan currentState, TopologyPanel topologyPanel, NetPlan initialState) {
        List<Object[]> allDemandData = new LinkedList<Object[]>();
        for (MulticastDemand demand : currentState.getMulticastDemands()) {
            Set<MulticastTree> multicastTreeIds_thisDemand = demand.getMulticastTrees();
            Set<Link> coupledLinks = demand.getCoupledLinks();
            Node ingressNode = demand.getIngressNode();
            Set<Node> egressNodes = demand.getEgressNodes();
            String ingressNodeName = ingressNode.getName();
            String egressNodesString = "";
            for (Node n : egressNodes) egressNodesString += n.getIndex() + "(" + n.getName() + ") ";

            double h_d = demand.getOfferedTraffic();
            double lostTraffic_d = demand.getBlockedTraffic();
            Object[] demandData = new Object[netPlanViewTableHeader.length];
            demandData[0] = demand.getId();
            demandData[1] = demand.getIndex();
            demandData[2] = ingressNode.getIndex() + (ingressNodeName.isEmpty() ? "" : " (" + ingressNodeName + ")");
            demandData[3] = egressNodesString;
            demandData[4] = coupledLinks.isEmpty() ? "" : "link ids " + CollectionUtils.join(NetPlan.getIndexes(coupledLinks), ",") + " layer " + coupledLinks.iterator().next().getLayer().getIndex() + "";
            demandData[5] = h_d;
            demandData[6] = demand.getCarriedTraffic();
            demandData[7] = h_d == 0 ? 0 : 100 * lostTraffic_d / h_d;
            demandData[8] = "Loopless by definition";
            demandData[9] = demand.isBifurcated() ? String.format("Yes (%d)", demand.getMulticastTrees().size()) : "No";
            demandData[10] = multicastTreeIds_thisDemand.isEmpty() ? "none" : multicastTreeIds_thisDemand.size() + " (" + CollectionUtils.join(NetPlan.getIndexes(multicastTreeIds_thisDemand), ",") + ")";
            demandData[11] = demand.getWorseCasePropagationTimeInMs();
            demandData[12] = StringUtils.mapToString(demand.getAttributes());
            allDemandData.add(demandData);

            if (initialState != null && initialState.getMulticastDemandFromId(demand.getId()) != null) {
                demand = initialState.getMulticastDemandFromId(demand.getId());
                multicastTreeIds_thisDemand = demand.getMulticastTrees();
                coupledLinks = demand.getCoupledLinks();
                ingressNode = demand.getIngressNode();
                egressNodes = demand.getEgressNodes();
                ingressNodeName = ingressNode.getName();
                egressNodesString = "";
                for (Node n : egressNodes) egressNodesString += n.getId() + "(" + n.getName() + ") ";

                h_d = demand.getOfferedTraffic();
                lostTraffic_d = demand.getBlockedTraffic();

                Object[] demandData_initialNetPlan = new Object[netPlanViewTableHeader.length];
                demandData_initialNetPlan[0] = null;
                demandData_initialNetPlan[1] = null;
                demandData_initialNetPlan[2] = null;
                demandData_initialNetPlan[3] = null;
                demandData_initialNetPlan[4] = coupledLinks.isEmpty() ? "" : "link ids " + CollectionUtils.join(NetPlan.getIndexes(coupledLinks), ",") + " layer " + coupledLinks.iterator().next().getLayer().getIndex() + "";
                demandData_initialNetPlan[5] = h_d;
                demandData_initialNetPlan[6] = demand.getCarriedTraffic();
                demandData_initialNetPlan[7] = h_d == 0 ? 0 : 100 * lostTraffic_d / h_d;
                demandData_initialNetPlan[8] = "Loopless by definition";
                demandData_initialNetPlan[9] = demand.isBifurcated() ? String.format("Yes (%d)", demand.getMulticastTrees().size()) : "No";
                demandData_initialNetPlan[10] = multicastTreeIds_thisDemand.isEmpty() ? "none" : multicastTreeIds_thisDemand.size() + " (" + CollectionUtils.join(NetPlan.getIndexes(multicastTreeIds_thisDemand), ",") + ")";
                demandData_initialNetPlan[11] = demand.getWorseCasePropagationTimeInMs();
                demandData_initialNetPlan[12] = StringUtils.mapToString(demand.getAttributes());
                allDemandData.add(demandData_initialNetPlan);
            }
        }

        return allDemandData;
    }

    public String getTabName() {
        return netPlanViewTabName;
    }

    public String[] getTableHeaders() {
        return netPlanViewTableHeader;
    }

    public String[] getTableTips() {
        return netPlanViewTableTips;
    }

    public boolean hasElements(NetPlan np) {
        return np.hasMulticastDemands();
    }

    public int[] getColumnsOfSpecialComparatorForSorting() {
        return new int[]{3, 4, 5, 9, 10};
    }

    private static TableModel createTableModel(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        TableModel multicastDemandTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
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
                final MulticastDemand demand = netPlan.getMulticastDemandFromId(demandId);

				/* Perform checks, if needed */
                try {
                    switch (column) {
                        case 5:
                            demand.setOfferedTraffic(Double.parseDouble(newValue.toString()));
                            networkViewer.updateNetPlanView();
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying multicast demand");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };
        return multicastDemandTableModel;
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
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_EGRESSNODES, new IGUINetworkViewer.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_NUMTREES, new IGUINetworkViewer.ColumnComparator());
    }

    public int getNumFixedLeftColumnsInDecoration() {
        return 2;
    }

    @Override
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

                    if (networkElementType == NetworkElementType.LAYER && networkViewer.getDesign().getNumberOfLayers() == 1) {

                    } else {
                        JMenuItem removeItem = new JMenuItem("Remove " + networkElementType);

                        removeItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                NetPlan netPlan = networkViewer.getDesign();

                                try {
                                    netPlan.getMulticastDemandFromId((long) itemId).remove();
                                    networkViewer.updateNetPlanView();
                                } catch (Throwable ex) {
                                    ErrorHandling.addErrorOrException(ex, getClass());
                                    ErrorHandling.showErrorDialog("Unable to remove " + networkElementType);
                                }
                            }
                        });

                        popup.add(removeItem);
                    }

                    addPopupMenuAttributeOptions(e, row, itemId, popup);
                }
                if (networkElementType != NetworkElementType.LAYER) {
                    JMenuItem removeItems = new JMenuItem("Remove all " + networkElementType + "s");

                    removeItems.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NetPlan netPlan = networkViewer.getDesign();
                            try {
                                netPlan.removeAllMulticastDemands();
                                networkViewer.updateNetPlanView();
                            } catch (Throwable ex) {
                                ex.printStackTrace();
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to remove all " + networkElementType + "s");
                            }
                        }
                    });
                    popup.add(removeItems);
                }

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

    @Override
    public void showInCanvas(MouseEvent e, Object itemId) {
        if (isTableEmpty()) return;

        int clickCount = e.getClickCount();
        switch (clickCount) {
            case 1:
                networkViewer.showMulticastDemand((long) itemId);
                break;

            case 2:
                int col = convertColumnIndexToModel(columnAtPoint(e.getPoint()));
                if (col == -1 || col >= getColumnCount()) return;

                NetPlan netPlan = networkViewer.getDesign();

                MulticastDemand demand = netPlan.getMulticastDemandFromId((long) itemId);
                Node ingressNode = demand.getIngressNode();
                Set<Node> egressNodes = demand.getEgressNodes();

                switch (col) {
                    case COLUMN_INGRESSNODE:
                        networkViewer.showNode(ingressNode.getId());
                        break;
                    case COLUMN_EGRESSNODES:
                        for (long n : NetPlan.getIds(egressNodes)) networkViewer.showNode(n);
                        break;
                    case COLUMN_COUPLEDTOLINKS:
//						if (demand.isCoupled())
//						{
//							Pair<Long, Long> coupledLink = netPlan.getDemandCoupledUpperLayerLink((long) itemId);
//							networkViewer.showLink(coupledLink.getFirst(), coupledLink.getSecond());
//						}
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    private boolean isTableEmpty() {
        return !networkViewer.getDesign().hasMulticastDemands();
    }

    private JMenuItem getAddOption() {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = networkViewer.getDesign();
                try {
                    createMulticastDemandGUI(networkElementType, networkViewer, networkViewer.getTopologyPanel());
                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });
        return addItem;
    }

    private static void createMulticastDemandGUI(final NetworkElementType networkElementType, final INetworkCallback networkViewer, final TopologyPanel topologyPanel) {
        final NetPlan netPlan = networkViewer.getDesign();

        JTextField textFieldIngressNodeId = new JTextField(20);
        JTextField textFieldEgressNodeIds = new JTextField(20);

        JPanel pane = new JPanel();
        pane.add(new JLabel("Ingress node id: "));
        pane.add(textFieldIngressNodeId);
        pane.add(Box.createHorizontalStrut(15));
        pane.add(new JLabel("Egress node ids (space separated): "));
        pane.add(textFieldEgressNodeIds);

        while (true) {
            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter multicast demand ingress node and set of egress nodes", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            try {
                if (textFieldIngressNodeId.getText().isEmpty())
                    throw new Exception("Please, insert the ingress node id");
                if (textFieldEgressNodeIds.getText().isEmpty())
                    throw new Exception("Please, insert the set of egress node ids");

                String ingressNodeId_st = textFieldIngressNodeId.getText();
                String egressNodeId_st = textFieldEgressNodeIds.getText();

                final long ingressNode = Long.parseLong(ingressNodeId_st);
                if (netPlan.getNodeFromId(ingressNode) == null)
                    throw new Exception("Not a valid ingress node id: " + ingressNodeId_st);
                Set<Node> egressNodes = new HashSet<Node>();
                for (String egressNodeIdString : StringUtils.split(egressNodeId_st)) {
                    final long nodeId = Long.parseLong(egressNodeIdString);
                    final Node node = netPlan.getNodeFromId(nodeId);
                    if (node == null) throw new Exception("Not a valid egress node id: " + egressNodeIdString);
                    egressNodes.add(node);
                }
                netPlan.addMulticastDemand(netPlan.getNodeFromId(ingressNode), egressNodes, 0, null);
                break;
            } catch (Throwable ex) {
                ErrorHandling.addErrorOrException(ex, AdvancedJTable_multicastDemand.class);
                ErrorHandling.showErrorDialog("Error adding the multicast demand");
            }
        }
    }

    private List<JComponent> getExtraAddOptions() {
        List<JComponent> options = new LinkedList<JComponent>();
        NetPlan netPlan = networkViewer.getDesign();
        if (netPlan.getNumberOfNodes() >= 2) {
            final JMenuItem oneBroadcastDemandPerNode = new JMenuItem("Add one broadcast demand per node");
            options.add(oneBroadcastDemandPerNode);
            oneBroadcastDemandPerNode.addActionListener(new BroadcastDemandPerNodeActionListener());
            final JMenuItem oneMulticastDemandPerNode = new JMenuItem("Add one multicast demand per node (ingress) with random egress nodes");
            options.add(oneMulticastDemandPerNode);
            oneMulticastDemandPerNode.addActionListener(new MulticastDemandPerNodeActionListener());
        }
        return options;
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
                    String str = JOptionPane.showInputDialog(null, "Offered traffic volume", "Set traffic value to all multicast demands", JOptionPane.QUESTION_MESSAGE);
                    if (str == null) return;

                    try {
                        h_d = Double.parseDouble(str);
                        if (h_d < 0) throw new RuntimeException();

                        break;
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog("Non-valid multicast demand offered traffic value. Please, introduce a non-negative number", "Error setting link length");
                    }
                }

                NetPlan netPlan = networkViewer.getDesign();

                try {
                    for (MulticastDemand demand : netPlan.getMulticastDemands()) demand.setOfferedTraffic(h_d);

                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set offered traffic to all multicast demands");
                }
            }
        });

        options.add(offeredTrafficToAll);

        if (itemId != null && netPlan.isMultilayer()) {
            final long demandId = (long) itemId;
            final MulticastDemand demand = netPlan.getMulticastDemandFromId(demandId);
            if (demand.isCoupled()) {
                JMenuItem decoupleDemandItem = new JMenuItem("Decouple multicast demand");
                decoupleDemandItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        demand.decouple();
                        model.setValueAt("", row, COLUMN_COUPLEDTOLINKS);
                        networkViewer.updateWarnings();
                    }
                });

                options.add(decoupleDemandItem);
            }
            if (numRows > 1) {
                JMenuItem decoupleAllDemandsItem = null;
                JMenuItem createUpperLayerLinksFromDemandsItem = null;

                final Set<MulticastDemand> coupledDemands = new HashSet<MulticastDemand>();
                for (MulticastDemand d : netPlan.getMulticastDemands()) if (d.isCoupled()) coupledDemands.add(d);
                if (!coupledDemands.isEmpty()) {
                    decoupleAllDemandsItem = new JMenuItem("Decouple all demands");
                    decoupleAllDemandsItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            for (MulticastDemand d : new HashSet<MulticastDemand>(coupledDemands)) d.decouple();
                            int numRows = model.getRowCount();
                            for (int i = 0; i < numRows; i++) model.setValueAt("", i, COLUMN_COUPLEDTOLINKS);
                            networkViewer.updateWarnings();
                        }
                    });
                }

                if (coupledDemands.size() < netPlan.getNumberOfDemands()) {
                    createUpperLayerLinksFromDemandsItem = new JMenuItem("Create upper layer links from uncoupled demands");
                    createUpperLayerLinksFromDemandsItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            List<Long> layerIds = netPlan.getNetworkLayerIds();
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
                                    for (MulticastDemand demand : netPlan.getMulticastDemands())
                                        if (!demand.isCoupled())
                                            demand.coupleToNewLinksCreated(layer);
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

    private class BroadcastDemandPerNodeActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            NetPlan netPlan = networkViewer.getDesign();

            int result = JOptionPane.showConfirmDialog(null, "Remove all existing multicast demands before?", "", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.CLOSED_OPTION) return;
            else if (result == JOptionPane.YES_OPTION) netPlan.removeAllMulticastDemands();

            if (netPlan.getNumberOfNodes() < 2) throw new Net2PlanException("At least two nodes are needed");

            for (Node ingressNode : netPlan.getNodes()) {
                Set<Node> egressNodes = new HashSet<Node>(netPlan.getNodes());
                egressNodes.remove(ingressNode);
                netPlan.addMulticastDemand(ingressNode, egressNodes, 0, null);
            }

            networkViewer.updateNetPlanView();
        }
    }

    private class MulticastDemandPerNodeActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Random rng = new Random();
            NetPlan netPlan = networkViewer.getDesign();

            int result = JOptionPane.showConfirmDialog(null, "Remove all existing multicast demands before?", "", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.CLOSED_OPTION) return;
            else if (result == JOptionPane.YES_OPTION) netPlan.removeAllMulticastDemands();

            if (netPlan.getNumberOfNodes() < 2) throw new Net2PlanException("At least two nodes are needed");

            for (Node ingressNode : netPlan.getNodes()) {
                Set<Node> egressNodes = new HashSet<Node>();
                for (Node n : netPlan.getNodes()) if ((n != ingressNode) && rng.nextBoolean()) egressNodes.add(n);
                if (egressNodes.isEmpty()) egressNodes.add(netPlan.getNode(ingressNode.getIndex() == 0 ? 1 : 0));
                netPlan.addMulticastDemand(ingressNode, egressNodes, 0, null);
            }

            networkViewer.updateNetPlanView();
        }
    }

}
