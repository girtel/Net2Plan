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

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.net2plan.gui.tools.IGUINetworkViewer;
import com.net2plan.gui.utils.*;
import com.net2plan.gui.utils.CellRenderers.NumberCellRenderer;
import com.net2plan.gui.utils.topology.INetworkCallback;
import com.net2plan.gui.utils.topology.TopologyPanel;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.IPUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.event.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public class AdvancedJTable_forwardingRule extends AdvancedJTableNetworkElement {
    private static final String netPlanViewTabName = "Forwarding rules";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Node", "Demand", "Outgoing link", "Splitting ratio", "Carried traffic");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Node where the forwarding rule is installed", "Demand", "Outgoing link", "Percentage of the traffic entering the node going through the outgoing link", "Carried traffic in this link for the demand");
    private static final int COLUMN_NODE = 0;
    private static final int COLUMN_DEMAND = 1;
    private static final int COLUMN_OUTGOINGLINK = 2;
    private static final int COLUMN_SPLITTINGRATIO = 3;
    private static final int COLUMN_CARRIEDTRAFFIC = 4;

    public AdvancedJTable_forwardingRule(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        super(createTableModel(networkViewer, topologyPanel), networkViewer, NetworkElementType.FORWARDING_RULE);
        setDefaultCellRenderers(networkViewer);
        setSpecificCellRenderers();
        setColumnRowSorting(networkViewer.inOnlineSimulationMode());
    }

    public List<Object[]> getAllData(NetPlan currentState, TopologyPanel topologyPanel, NetPlan initialState) {
        final boolean sameRoutingType = initialState != null && initialState.getRoutingType() == currentState.getRoutingType();
        Map<Pair<Demand, Link>, Double> forwardingRules = currentState.getForwardingRules();
        Set<Pair<Demand, Link>> demandLinkPairs = forwardingRules.keySet();
        List<Object[]> allForwardingRuleData = new LinkedList<Object[]>();
        for (Pair<Demand, Link> demandLinkPair : demandLinkPairs) {
            Demand demand = demandLinkPair.getFirst();
            Node ingressNode = demand.getIngressNode();
            Node egressNode = demand.getEgressNode();
            String ingressNodeName = ingressNode.getName();
            String egressNodeName = egressNode.getName();

            Link link = demandLinkPair.getSecond();
            Node originNode = link.getOriginNode();
            Node destinationNode = link.getDestinationNode();
            String originNodeName = originNode.getName();
            String destinationNodeName = destinationNode.getName();

            Object[] forwardingRuleData = new Object[netPlanViewTableHeader.length];
            forwardingRuleData[COLUMN_NODE] = originNode.getIndex() + (originNodeName.isEmpty() ? "" : " (" + originNodeName + ")");
            forwardingRuleData[COLUMN_DEMAND] = demand.getIndex() + " (" + ingressNode.getIndex() + (ingressNodeName.isEmpty() ? "" : " (" + ingressNodeName + ")") + " -> " + egressNode.getIndex() + (egressNodeName.isEmpty() ? "" : " (" + egressNodeName + ")") + ")";
            forwardingRuleData[COLUMN_OUTGOINGLINK] = link.getIndex() + " (" + originNode.getIndex() + (originNodeName.isEmpty() ? "" : " (" + originNodeName + ")") + " -> " + destinationNode.getIndex() + (destinationNodeName.isEmpty() ? "" : " (" + destinationNodeName + ")") + ")";
            forwardingRuleData[COLUMN_SPLITTINGRATIO] = currentState.getForwardingRuleSplittingFactor(demand, link);
            forwardingRuleData[COLUMN_CARRIEDTRAFFIC] = currentState.getForwardingRuleCarriedTraffic(demand, link);
            allForwardingRuleData.add(forwardingRuleData);

            if (initialState != null && sameRoutingType && initialState.getDemandFromId(demand.getId()) != null && initialState.getLinkFromId(link.getId()) != null) {
                demand = initialState.getDemandFromId(demand.getId());
                link = initialState.getLinkFromId(link.getId());
                ingressNode = demand.getIngressNode();
                egressNode = demand.getEgressNode();
                ingressNodeName = ingressNode.getName();
                egressNodeName = egressNode.getName();
                originNode = link.getOriginNode();
                destinationNode = link.getDestinationNode();
                originNodeName = originNode.getName();

                Object[] forwardingRuleData_initialNetPlan = new Object[netPlanViewTableHeader.length];
                forwardingRuleData_initialNetPlan[COLUMN_NODE] = null;
                forwardingRuleData_initialNetPlan[COLUMN_DEMAND] = null;
                forwardingRuleData_initialNetPlan[COLUMN_OUTGOINGLINK] = null;
                forwardingRuleData_initialNetPlan[COLUMN_SPLITTINGRATIO] = currentState.getForwardingRuleSplittingFactor(demand, link);
                forwardingRuleData_initialNetPlan[COLUMN_CARRIEDTRAFFIC] = currentState.getForwardingRuleCarriedTraffic(demand, link);
                allForwardingRuleData.add(forwardingRuleData_initialNetPlan);
            }
        }
        return allForwardingRuleData;
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
        return true;
    } //if (np.getRoutingType() != RoutingType.HOP_BY_HOP_ROUTING) return false; return (np.getMatrixDemandBasedForwardingRules().getMaxLocation() [0] > 0); }

    public int[] getColumnsOfSpecialComparatorForSorting() {
        return new int[]{0, 1, 2};
    }

    private static TableModel createTableModel(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        TableModel forwardingRuleTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (getValueAt(rowIndex, columnIndex) == null) return false;
                if (!networkViewer.isEditable()) return false;

                return columnIndex == COLUMN_SPLITTINGRATIO;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column) {
                Object oldValue = getValueAt(row, column);

                if (newValue.equals(oldValue)) return;

                NetPlan netPlan = networkViewer.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final Pair<Long, Long> forwardingRule = Pair.of((Long) getValueAt(row, 1), (Long) getValueAt(row, 2));
                final Demand demand = netPlan.getDemandFromId(forwardingRule.getFirst());
                final Link link = netPlan.getLinkFromId(forwardingRule.getSecond());

				/* Perform checks, if needed */
                try {
                    switch (column) {
                        case COLUMN_SPLITTINGRATIO:
                            netPlan.setForwardingRule(demand, link, Double.parseDouble(newValue.toString()));
                            networkViewer.updateNetPlanView();
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying forwarding rule");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };
        return forwardingRuleTableModel;
    }

    private void setDefaultCellRenderers(final IGUINetworkViewer networkViewer) {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());

        setDefaultRenderer(Boolean.class, new CellRenderers.ForwardingRuleRenderer(getDefaultRenderer(Boolean.class), networkViewer));
        setDefaultRenderer(Double.class, new CellRenderers.ForwardingRuleRenderer(getDefaultRenderer(Double.class), networkViewer));
        setDefaultRenderer(Object.class, new CellRenderers.ForwardingRuleRenderer(getDefaultRenderer(Object.class), networkViewer));
        setDefaultRenderer(Float.class, new CellRenderers.ForwardingRuleRenderer(getDefaultRenderer(Float.class), networkViewer));
        setDefaultRenderer(Long.class, new CellRenderers.ForwardingRuleRenderer(getDefaultRenderer(Long.class), networkViewer));
        setDefaultRenderer(Integer.class, new CellRenderers.ForwardingRuleRenderer(getDefaultRenderer(Integer.class), networkViewer));
        setDefaultRenderer(String.class, new CellRenderers.ForwardingRuleRenderer(getDefaultRenderer(String.class), networkViewer));
    }

    private void setSpecificCellRenderers() {
    }

    public void setColumnRowSorting(boolean allowShowInitialNetPlan) {
        if (allowShowInitialNetPlan) setRowSorter(new CurrentAndPlannedStateTableSorter(getModel()));
        else setAutoCreateRowSorter(true);
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_NODE, new IGUINetworkViewer.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_DEMAND, new IGUINetworkViewer.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_OUTGOINGLINK, new IGUINetworkViewer.ColumnComparator());
    }

    public int getNumFixedLeftColumnsInDecoration() {
        return 3;
    }


    public void showInCanvas(MouseEvent e, Object itemId) {
        if (e.getClickCount() == 1) {
            networkViewer.showForwardingRule((Pair<Integer, Integer>) itemId);
            //networkViewer.showForwardingRule((Pair<Long, Long>) itemId);
        }
        int col = convertColumnIndexToModel(columnAtPoint(e.getPoint()));
        if (col == -1 || col >= getColumnCount()) return;

        NetPlan netPlan = networkViewer.getDesign();
        Pair<Integer, Integer> forwardingRule = (Pair<Integer, Integer>) itemId;
        switch (col) {
            case COLUMN_NODE:
                networkViewer.showNode(netPlan.getLink(forwardingRule.getSecond()).getOriginNode().getId());
                break;
            case COLUMN_DEMAND:
                networkViewer.showDemand(netPlan.getDemand(forwardingRule.getFirst()).getId());
                break;
            case COLUMN_OUTGOINGLINK:
                networkViewer.showLink(netPlan.getLink(forwardingRule.getSecond()).getId());
                break;
            default:
                break;
        }
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
                                netPlan.setForwardingRule(netPlan.getDemandFromId(((Pair<Long, Long>) itemId).getFirst()), netPlan.getLinkFromId(((Pair<Long, Long>) itemId).getSecond()), 0);
                                networkViewer.updateNetPlanView();
                            } catch (Throwable ex) {
                                ErrorHandling.addErrorOrException(ex, getClass());
                                ErrorHandling.showErrorDialog("Unable to remove " + networkElementType);
                            }
                        }
                    });

                    popup.add(removeItem);
                }

                JMenuItem removeItems = new JMenuItem("Remove all " + networkElementType + "s");
                removeItems.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        NetPlan netPlan = networkViewer.getDesign();

                        try {
                            netPlan.removeAllForwardingRules();
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

    private boolean isTableEmpty() {
        return !networkViewer.getDesign().hasForwardingRules();
    }

    private JMenuItem getAddOption() {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = networkViewer.getDesign();

                try {
                    createForwardingRuleGUI(networkViewer);
                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });

        NetPlan netPlan = networkViewer.getDesign();
        if (!netPlan.hasLinks() || !netPlan.hasDemands()) addItem.setEnabled(false);

        return addItem;
    }

    private static void createForwardingRuleGUI(final INetworkCallback networkViewer) {
        final NetPlan netPlan = networkViewer.getDesign();
        final JComboBox nodeSelector = new WiderJComboBox();
        final JComboBox linkSelector = new WiderJComboBox();
        final JComboBox demandSelector = new WiderJComboBox();
        final JTextField txt_splittingRatio = new JTextField(5);

        ItemListener nodeListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                JComboBox me = (JComboBox) e.getSource();
                linkSelector.removeAllItems();
                long nodeId = (long) ((StringLabeller) me.getSelectedItem()).getObject();
                Set<Link> links = netPlan.getNodeFromId(nodeId).getOutgoingLinksAllLayers();
                for (Link link : links) {
                    String originNodeLabel = "Node " + link.getOriginNode().getId();
                    if (!link.getOriginNode().getName().isEmpty())
                        originNodeLabel += " (" + link.getOriginNode().getName() + ")";
                    String destinationNodeLabel = "Node " + link.getDestinationNode().getId();
                    if (!link.getDestinationNode().getName().isEmpty())
                        destinationNodeLabel += " (" + link.getDestinationNode().getName() + ")";
                    String linkLabel = "e" + link.getId() + ": " + originNodeLabel + " -> " + destinationNodeLabel;
                    linkSelector.addItem(StringLabeller.of(link.getId(), linkLabel));
                }

                linkSelector.setSelectedIndex(0);
            }
        };

        ItemListener linkDemandListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                Demand demand = netPlan.getDemandFromId((long) ((StringLabeller) demandSelector.getSelectedItem()).getObject());
                Link link = netPlan.getLinkFromId((long) ((StringLabeller) linkSelector.getSelectedItem()).getObject());
                double splittingRatio;
                if (netPlan.getForwardingRuleSplittingFactor(demand, link) > 0) {
                    splittingRatio = netPlan.getForwardingRuleSplittingFactor(demand, link);
                } else {
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

        for (Node node : netPlan.getNodes()) {
            if (node.getOutgoingLinks().isEmpty()) continue;

            final String nodeName = node.getName();
            String nodeLabel = "Node " + node.getId();
            if (!nodeName.isEmpty()) nodeLabel += " (" + nodeName + ")";

            nodeSelector.addItem(StringLabeller.of(node.getId(), nodeLabel));
        }

        linkSelector.addItemListener(linkDemandListener);
        demandSelector.addItemListener(linkDemandListener);

        for (Demand demand : netPlan.getDemands()) {
            String ingressNodeLabel = "Node " + demand.getIngressNode().getId();
            if (!demand.getIngressNode().getName().isEmpty())
                ingressNodeLabel += " (" + demand.getIngressNode().getName() + ")";
            String egressNodeLabel = "Node " + demand.getEgressNode().getId();
            if (!demand.getEgressNode().getName().isEmpty())
                egressNodeLabel += " (" + demand.getEgressNode().getName() + ")";
            String demandLabel = "d" + demand.getId() + ": " + ingressNodeLabel + " -> " + egressNodeLabel;
            demandSelector.addItem(StringLabeller.of(demand.getId(), demandLabel));
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

        while (true) {
            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter information for the new forwarding rule", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            try {
                long demandId = (long) ((StringLabeller) demandSelector.getSelectedItem()).getObject();
                long linkId = (long) ((StringLabeller) linkSelector.getSelectedItem()).getObject();

                double splittingRatio;
                try {
                    splittingRatio = Double.parseDouble(txt_splittingRatio.getText());
                    if (splittingRatio <= 0) throw new RuntimeException();
                } catch (Throwable e) {
                    ErrorHandling.showErrorDialog("Splitting ratio must be a non-negative non-zero number", "Error adding forwarding rule");
                    continue;
                }

                netPlan.setForwardingRule(netPlan.getDemandFromId(demandId), netPlan.getLinkFromId(linkId), splittingRatio);
                break;
            } catch (Throwable ex) {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Error adding forwarding rule");
            }
        }
    }

    private List<JComponent> getExtraAddOptions() {
        List<JComponent> options = new LinkedList<JComponent>();
        NetPlan netPlan = networkViewer.getDesign();

        final JMenuItem ecmpRouting = new JMenuItem("Generate ECMP forwarding rules from link IGP weights");
        options.add(ecmpRouting);

        ecmpRouting.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = networkViewer.getDesign();
                DoubleMatrix1D linkWeightMap = IPUtils.getLinkWeightVector(netPlan);
                IPUtils.setECMPForwardingRulesFromLinkWeights(netPlan, linkWeightMap);
                networkViewer.updateNetPlanView();
            }
        });

        if (!netPlan.hasLinks() || !netPlan.hasDemands()) ecmpRouting.setEnabled(false);

        return options;
    }

    private List<JComponent> getExtraOptions(final int row, final Object itemId) {
        return new LinkedList<JComponent>();
    }

    private List<JComponent> getForcedOptions() {
        return new LinkedList<JComponent>();
    }
}
