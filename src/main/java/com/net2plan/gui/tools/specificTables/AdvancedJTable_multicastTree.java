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

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.net2plan.gui.tools.IGUINetworkViewer;
import com.net2plan.gui.utils.CellRenderers;
import com.net2plan.gui.utils.CellRenderers.NumberCellRenderer;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.CurrentAndPlannedStateTableSorter;
import com.net2plan.gui.utils.topology.INetworkCallback;
import com.net2plan.gui.utils.topology.TopologyPanel;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.GraphUtils;
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
public class AdvancedJTable_multicastTree extends AdvancedJTableNetworkElement {
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_INDEX = 1;
    private static final int COLUMN_MULTICASTDEMAND = 2;
    private static final int COLUMN_INGRESSNODE = 3;
    private static final int COLUMN_EGRESSNODES = 4;
    private static final int COLUMN_OFFEREDTRAFFIC = 5;
    private static final int COLUMN_CARRIEDTRAFFIC = 6;
    private static final int COLUMN_OCCUPIEDCAPACITY = 7;
    private static final int COLUMN_SETOFLINKS = 8;
    private static final int COLUMN_NUMLINKS = 9;
    private static final int COLUMN_SETOFNODES = 10;
    private static final int COLUMN_WORSECASENUMHOPS = 11;
    private static final int COLUMN_WORSECASELENGTH = 12;
    private static final int COLUMN_WORSECASEPROPDELAY = 13;
    private static final int COLUMN_BOTTLENECKUTILIZATION = 14;
    private static final int COLUMN_ATTRIBUTES = 15;
    private static final String netPlanViewTabName = "Multicast trees";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "Multicast demand", "Ingress node", "Egress nodes",
            "Demand offered traffic", "Carried traffic", "Occupied capacity", "Set of links", "Number of links", "Set of nodes", "Worse case number of hops",
            "Worse case length (km)", "Worse case propagation delay (ms)", "Bottleneck utilization", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)", "Index (consecutive integer starting in zero)", "Multicast demand", "Ingress node", "Egress nodes", "Multicast demand offered traffic", "This multicast tree carried traffic", "Capacity occupied in the links (typically same as the carried traffic)", "Set of links in the tree", "Number of links in the tree (equal to the number of traversed nodes minus one)", "Set of traversed nodes (including ingress and egress ndoes)", "Number of hops of the longest path (in number of hops) to any egress node", "Length (km) of the longest path (in km) to any egress node", "Propagation demay (ms) of the longest path (in ms) to any egress node", "Highest utilization among all traversed links", "Multicast tree specific attributes");

    public AdvancedJTable_multicastTree(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        super(createTableModel(networkViewer, topologyPanel), networkViewer, NetworkElementType.MULTICAST_TREE);
        setDefaultCellRenderers(networkViewer);
        setSpecificCellRenderers();
        setColumnRowSorting(networkViewer.inOnlineSimulationMode());

    }

    public List<Object[]> getAllData(NetPlan currentState, TopologyPanel topologyPanel, NetPlan initialState) {
        List<Object[]> allTreeData = new LinkedList<Object[]>();
        for (MulticastTree tree : currentState.getMulticastTrees()) {
            MulticastDemand demand = tree.getMulticastDemand();
            double maxUtilization = 0;
            for (Link e : tree.getLinkSet())
                maxUtilization = Math.max(maxUtilization, e.getOccupiedCapacityIncludingProtectionSegments() / e.getCapacity());
            Node ingressNode = tree.getIngressNode();
            Set<Node> egressNodes = tree.getEgressNodes();
            String ingressNodeName = ingressNode.getName();
            String egressNodesString = "";
            for (Node n : egressNodes) egressNodesString += n + "(" + (n.getName().isEmpty() ? "" : n.getName()) + ") ";

            Object[] treeData = new Object[netPlanViewTableHeader.length];
            treeData[0] = tree.getId();
            treeData[1] = tree.getIndex();
            treeData[2] = demand.getIndex();
            treeData[3] = ingressNode.getIndex() + (ingressNodeName.isEmpty() ? "" : " (" + ingressNodeName + ")");
            treeData[4] = egressNodesString;
            treeData[5] = demand.getOfferedTraffic();
            treeData[6] = demand.getCarriedTraffic();
            treeData[7] = tree.getOccupiedLinkCapacity();
            treeData[8] = CollectionUtils.join(NetPlan.getIndexes(tree.getLinkSet()), " ; ");
            treeData[9] = tree.getLinkSet().size();
            treeData[10] = CollectionUtils.join(NetPlan.getIndexes(tree.getNodeSet()), " ; ");
            treeData[11] = tree.getTreeMaximumPathLengthInHops();
            treeData[12] = tree.getTreeMaximumPathLengthInKm();
            treeData[13] = tree.getTreeMaximumPropagationDelayInMs();
            treeData[14] = maxUtilization;
            treeData[15] = StringUtils.mapToString(tree.getAttributes());
            allTreeData.add(treeData);

            if (initialState != null && initialState.getMulticastTreeFromId(tree.getId()) != null) {
                tree = initialState.getMulticastTreeFromId(tree.getId());
                demand = tree.getMulticastDemand();
                maxUtilization = 0;
                for (Link e : tree.getLinkSet())
                    maxUtilization = Math.max(maxUtilization, e.getOccupiedCapacityIncludingProtectionSegments() / e.getCapacity());
                ingressNode = tree.getIngressNode();
                egressNodes = tree.getEgressNodes();
                ingressNodeName = ingressNode.getName();
                egressNodesString = "";
                for (Node n : egressNodes)
                    egressNodesString += n + "(" + (n.getName().isEmpty() ? "" : n.getName()) + ") ";

                Object[] treeData_initialNetPlan = new Object[netPlanViewTableHeader.length];
                treeData_initialNetPlan[0] = null;
                treeData_initialNetPlan[1] = null;
                treeData_initialNetPlan[2] = null;
                treeData_initialNetPlan[3] = null;
                treeData_initialNetPlan[4] = null;
                treeData_initialNetPlan[5] = demand.getOfferedTraffic();
                treeData_initialNetPlan[6] = demand.getCarriedTraffic();
                treeData_initialNetPlan[7] = tree.getOccupiedLinkCapacity();
                treeData_initialNetPlan[8] = CollectionUtils.join(NetPlan.getIndexes(tree.getLinkSet()), " ; ");
                treeData_initialNetPlan[9] = tree.getLinkSet().size();
                treeData_initialNetPlan[11] = tree.getTreeMaximumPathLengthInHops();
                treeData_initialNetPlan[12] = tree.getTreeMaximumPathLengthInKm();
                treeData_initialNetPlan[13] = tree.getTreeMaximumPropagationDelayInMs();
                treeData_initialNetPlan[14] = maxUtilization;
                treeData_initialNetPlan[15] = StringUtils.mapToString(tree.getAttributes());
                allTreeData.add(treeData_initialNetPlan);
            }
        }
        return allTreeData;
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
        return np.hasMulticastTrees();
    }

    public int[] getColumnsOfSpecialComparatorForSorting() {
        return new int[]{};
    }

    private static TableModel createTableModel(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        TableModel treeTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (getValueAt(rowIndex, columnIndex) == null) return false;
                if (!networkViewer.isEditable()) return false;

                return columnIndex == COLUMN_CARRIEDTRAFFIC || columnIndex == COLUMN_OCCUPIEDCAPACITY;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column) {
                Object oldValue = getValueAt(row, column);

                if (newValue.equals(oldValue)) return;

                NetPlan netPlan = networkViewer.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long treeId = (Long) getValueAt(row, 0);
                final MulticastTree tree = netPlan.getMulticastTreeFromId(treeId);

				/* Perform checks, if needed */
                try {
                    switch (column) {
                        case COLUMN_CARRIEDTRAFFIC:
                            tree.setCarriedTraffic(Double.parseDouble(newValue.toString()), tree.getOccupiedLinkCapacity());
                            networkViewer.updateNetPlanView();
                            break;

                        case COLUMN_OCCUPIEDCAPACITY:
                            tree.setCarriedTraffic(tree.getCarriedTraffic(), Double.parseDouble(newValue.toString()));
                            networkViewer.updateNetPlanView();
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying route");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };
        return treeTableModel;
    }

    private void setDefaultCellRenderers(final IGUINetworkViewer networkViewer) {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());

        setDefaultRenderer(Boolean.class, new CellRenderers.MulticastTreeRenderer(getDefaultRenderer(Boolean.class), networkViewer));
        setDefaultRenderer(Double.class, new CellRenderers.MulticastTreeRenderer(getDefaultRenderer(Double.class), networkViewer));
        setDefaultRenderer(Object.class, new CellRenderers.MulticastTreeRenderer(getDefaultRenderer(Object.class), networkViewer));
        setDefaultRenderer(Float.class, new CellRenderers.MulticastTreeRenderer(getDefaultRenderer(Float.class), networkViewer));
        setDefaultRenderer(Long.class, new CellRenderers.MulticastTreeRenderer(getDefaultRenderer(Long.class), networkViewer));
        setDefaultRenderer(Integer.class, new CellRenderers.MulticastTreeRenderer(getDefaultRenderer(Integer.class), networkViewer));
        setDefaultRenderer(String.class, new CellRenderers.MulticastTreeRenderer(getDefaultRenderer(String.class), networkViewer));
    }

    private void setSpecificCellRenderers() {
    }

    public void setColumnRowSorting(boolean allowShowInitialNetPlan) {
        if (allowShowInitialNetPlan) setRowSorter(new CurrentAndPlannedStateTableSorter(getModel()));
        else setAutoCreateRowSorter(true);
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

                    JMenuItem removeItem = new JMenuItem("Remove " + networkElementType);
                    removeItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NetPlan netPlan = networkViewer.getDesign();
                            try {
                                netPlan.getMulticastTreeFromId((long) itemId).remove();
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
                            netPlan.removeAllMulticastTrees();
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

    @Override
    public void showInCanvas(MouseEvent e, Object itemId) {
        if (isTableEmpty()) return;

        int clickCount = e.getClickCount();
        switch (clickCount) {
            case 1:
                networkViewer.showMulticastTree((long) itemId);
                break;

            case 2:
                int col = convertColumnIndexToModel(columnAtPoint(e.getPoint()));
                if (col == -1 || col >= getColumnCount()) return;

                NetPlan netPlan = networkViewer.getDesign();

                MulticastDemand multicastDemand = netPlan.getMulticastTreeFromId((long) itemId).getMulticastDemand();

                switch (col) {
                    case COLUMN_MULTICASTDEMAND:
                        networkViewer.showMulticastDemand(multicastDemand.getId());
                        break;
                    case COLUMN_INGRESSNODE:
                        networkViewer.showNode(multicastDemand.getIngressNode().getId());
                        break;
                    case COLUMN_EGRESSNODES:
                        for (Node n : multicastDemand.getEgressNodes())
                            networkViewer.showNode(n.getId());
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    private boolean isTableEmpty() {
        return !networkViewer.getDesign().hasMulticastTrees();
    }

    private JMenuItem getAddOption() {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = networkViewer.getDesign();

                try {
                    createMulticastTreeGUI(networkViewer, networkViewer.getTopologyPanel());
                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });

        NetPlan netPlan = networkViewer.getDesign();
        if (!netPlan.hasMulticastDemands()) addItem.setEnabled(false);
        return addItem;
    }

    private static void createMulticastTreeGUI(final INetworkCallback networkViewer, final TopologyPanel topologyPanel) {
        final NetPlan netPlan = networkViewer.getDesign();

        JTextField textFieldDemandIndex = new JTextField(20);
        JTextField textFieldLinkIndexes = new JTextField(20);
        JPanel pane = new JPanel();
        pane.add(new JLabel("Multicast demand index: "));
        pane.add(textFieldDemandIndex);
        pane.add(Box.createHorizontalStrut(15));
        pane.add(new JLabel("Link indexes (space separated): "));
        pane.add(textFieldLinkIndexes);

        while (true) {
            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter multicast tree demand index and link indexes", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            try {
                if (textFieldDemandIndex.getText().isEmpty())
                    throw new Exception("Please, insert the multicast demand index");
                if (textFieldLinkIndexes.getText().isEmpty()) throw new Exception("Please, insert the link indexes");
                MulticastDemand demand = netPlan.getMulticastDemand(Integer.parseInt(textFieldDemandIndex.getText()));
                Set<Link> links = new HashSet<Link>();
                for (String linkString : StringUtils.split(textFieldLinkIndexes.getText()))
                    links.add(netPlan.getLink(Integer.parseInt(linkString)));
                netPlan.addMulticastTree(demand, 0, 0, links, null);
                break;
            } catch (Throwable ex) {
                ErrorHandling.addErrorOrException(ex, AdvancedJTable_multicastTree.class);
                ErrorHandling.showErrorDialog("Error adding the multicast tree");
            }
        }
    }

    private List<JComponent> getExtraAddOptions() {
        List<JComponent> options = new LinkedList<JComponent>();
        NetPlan netPlan = networkViewer.getDesign();

        if (netPlan.getNumberOfMulticastDemands() >= 1) {
            final JMenuItem oneTreePerDemandMinE2EHops = new JMenuItem("Add one tree per demand, minimizing end-to-end average number of traversed links");
            options.add(oneTreePerDemandMinE2EHops);
            oneTreePerDemandMinE2EHops.addActionListener(new MulticastTreeMinE2EActionListener(true, false));
            final JMenuItem oneTreePerDemandMinE2ELength = new JMenuItem("Add one tree per demand, minimizing end-to-end average traversed length in km");
            options.add(oneTreePerDemandMinE2ELength);
            oneTreePerDemandMinE2ELength.addActionListener(new MulticastTreeMinE2EActionListener(false, false));
            final JMenuItem oneTreePerDemandMinCostHops = new JMenuItem("Add one tree per demand, minimizing number of links in the tree (uses default ILP solver)");
            options.add(oneTreePerDemandMinCostHops);
            oneTreePerDemandMinCostHops.addActionListener(new MulticastTreeMinE2EActionListener(true, true));
            final JMenuItem oneTreePerDemandMinCostLength = new JMenuItem("Add one tree per demand, minimizing number of km of the links in the tree (uses default ILP solver)");
            options.add(oneTreePerDemandMinCostLength);
            oneTreePerDemandMinCostLength.addActionListener(new MulticastTreeMinE2EActionListener(true, false));
        }


        return options;
    }

    private class MulticastTreeMinE2EActionListener implements ActionListener {
        final boolean isMinHops;
        final boolean minCost;

        private MulticastTreeMinE2EActionListener(boolean isMinHops, boolean minCost) {
            this.isMinHops = isMinHops;
            this.minCost = minCost;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Random rng = new Random();
            NetPlan netPlan = networkViewer.getDesign();
            List<Link> links = netPlan.getLinks();
            final int E = links.size();
            List<MulticastTree> addedTrees = new LinkedList<MulticastTree>();
            try {
                if (minCost) {
                    DoubleMatrix1D linkCosts = isMinHops ? DoubleFactory1D.dense.make(E, 1) : netPlan.getVectorLinkLengthInKm();
                    String solverName = Configuration.getOption("defaultILPSolver");
                    String solverLibraryName = null;
                    if (solverName.equalsIgnoreCase("glpk")) solverLibraryName = Configuration.getOption("glpkSolverLibraryName");
                    else if (solverName.equalsIgnoreCase("ipopt")) solverLibraryName = Configuration.getOption("ipoptSolverLibraryName");
                    else if (solverName.equalsIgnoreCase("cplex")) solverLibraryName = Configuration.getOption("cplexSolverLibraryName");
                    else if (solverName.equalsIgnoreCase("xpress")) solverLibraryName = Configuration.getOption("xpressSolverLicenseFileName");
                    DoubleMatrix2D Aout_ne = netPlan.getMatrixNodeLinkOutgoingIncidence();
                    DoubleMatrix2D Ain_ne = netPlan.getMatrixNodeLinkIncomingIncidence();
                    for (MulticastDemand demand : netPlan.getMulticastDemands()) {
                        Set<Link> linkSet = GraphUtils.getMinimumCostMulticastTree(links, Aout_ne, Ain_ne, linkCosts, demand.getIngressNode(), demand.getEgressNodes(), E, -1, -1.0, -1.0, solverName, solverLibraryName, 5.0);
                        addedTrees.add(netPlan.addMulticastTree(demand, demand.getOfferedTraffic(), demand.getOfferedTraffic(), linkSet, null));
                    }
                } else {
                    Map<Link, Double> linkCostMap = new HashMap<Link, Double>();
                    for (Link link : netPlan.getLinks()) linkCostMap.put(link, isMinHops ? 1 : link.getLengthInKm());
                    for (MulticastDemand demand : netPlan.getMulticastDemands()) {
                        Set<Link> linkSet = new HashSet<Link>();
                        for (Node egressNode : demand.getEgressNodes()) {
                            List<Link> seqLinks = GraphUtils.getShortestPath(netPlan.getNodes(), netPlan.getLinks(), demand.getIngressNode(), egressNode, linkCostMap);
                            if (seqLinks.isEmpty())
                                throw new Net2PlanException("The multicast tree cannot be created since the network is not connected");
                            linkSet.addAll(seqLinks);
                        }
                        addedTrees.add(netPlan.addMulticastTree(demand, demand.getOfferedTraffic(), demand.getOfferedTraffic(), linkSet, null));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                for (MulticastTree t : addedTrees) t.remove();
                ErrorHandling.showErrorDialog(ex.getMessage(), "Error adding multicast trees. No tree was created.");
            }
            networkViewer.updateNetPlanView();
        }
    }

    private List<JComponent> getExtraOptions(final int row, final Object itemId) {
        return new LinkedList<JComponent>();
    }

    private List<JComponent> getForcedOptions() {
        return new LinkedList<JComponent>();
    }
}
