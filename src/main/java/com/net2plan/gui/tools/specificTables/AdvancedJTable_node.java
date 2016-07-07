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
import com.net2plan.gui.utils.CellRenderers;
import com.net2plan.gui.utils.CellRenderers.NumberCellRenderer;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.CurrentAndPlannedStateTableSorter;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.gui.utils.topology.TopologyPanel;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.IntUtils;
import com.net2plan.utils.StringUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.*;

/**
 */
public class AdvancedJTable_node extends AdvancedJTableNetworkElement {
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_INDEX = 1;
    public static final int COLUMN_SHOWHIDE = 2;
    public static final int COLUMN_NAME = 3;
    public static final int COLUMN_STATE = 4;
    public static final int COLUMN_XCOORD = 5;
    public static final int COLUMN_YCOORD = 6;
    public static final int COLUMN_OUTLINKS = 7;
    public static final int COLUMN_INLINKS = 8;
    public static final int COLUMN_INGRESSTRAFFIC = 9;
    public static final int COLUMN_EGRESSTRAFFIC = 10;
    public static final int COLUMN_INGRESSMULTICASTTRAFFIC = 11;
    public static final int COLUMN_EGRESSMULTICASTTRAFFIC = 12;
    public static final int COLUMN_SRGS = 13;
    public static final int COLUMN_ATTRIBUTES = 14;
    private static final String netPlanViewTabName = "Nodes";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "Show/Hide", "Name", "State", "xCoord", "yCoord", "Outgoing links", "Incoming links", "Ingress traffic", "Egress traffic", "Ingress traffic (multicast)", "Egress traffic (multicast)", "SRGs", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)", "Index (consecutive integer starting in zero)", "Indicates whether or not the node is visible in the topology canvas", "Node name", "Indicates whether the node is in up/down state", "Coordinate along x-axis (i.e. longitude)", "Coordinate along y-axis (i.e. latitude)", "Outgoing links", "Incoming links", "Total UNICAST traffic entering to the network from this node", "Total UNICAST traffic leaving the network from this node", "UNICAST traffic entering the node, but not targeted to it", "Total MULTICAST traffic entering to the network from this node", "Total MULTICAST traffic leaving the network from this node", "SRGs including this node", "Node-specific attributes");


    /**
     * Default constructor.
     *
     * @since 0.2.0
     */
    public AdvancedJTable_node(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        super(createTableModel(networkViewer, topologyPanel), networkViewer, NetworkElementType.NODE);
        setDefaultCellRenderers(networkViewer);
        setSpecificCellRenderers();
        setColumnRowSorting(networkViewer.inOnlineSimulationMode());

    }

    public List<Object[]> getAllData(NetPlan currentState, TopologyPanel topologyPanel, NetPlan initialState) {
        List<Object[]> allNodeData = new LinkedList<Object[]>();

        for (Node node : currentState.getNodes()) {
            Set<Link> outgoingLinks = node.getOutgoingLinks();
            Set<Link> incomingLinks = node.getIncomingLinks();

            Object[] nodeData = new Object[netPlanViewTableHeader.length];
            nodeData[0] = node.getId();
            nodeData[1] = node.getIndex();
            nodeData[2] = topologyPanel.getCanvas().isNodeVisible(node);
            nodeData[3] = node.getName();
            nodeData[4] = node.isUp();
            nodeData[5] = node.getXYPositionMap().getX();
            nodeData[6] = node.getXYPositionMap().getY();
            nodeData[7] = outgoingLinks.isEmpty() ? "none" : outgoingLinks.size() + " (" + CollectionUtils.join(outgoingLinks, ", ") + ")";
            nodeData[8] = incomingLinks.isEmpty() ? "none" : incomingLinks.size() + " (" + CollectionUtils.join(incomingLinks, ", ") + ")";
            nodeData[9] = node.getIngressOfferedTraffic();
            nodeData[10] = node.getEgressOfferedTraffic();
            nodeData[11] = node.getIngressOfferedMulticastTraffic();
            nodeData[12] = node.getEgressOfferedMulticastTraffic();
            nodeData[13] = node.getSRGs().isEmpty() ? "none" : node.getSRGs().size() + " (" + CollectionUtils.join(currentState.getIndexes(node.getSRGs()), ", ") + ")";
            nodeData[14] = StringUtils.mapToString(node.getAttributes());
            allNodeData.add(nodeData);

            if (initialState != null && initialState.getNodeFromId(node.getId()) != null) {
                node = initialState.getNodeFromId(node.getId());
                outgoingLinks = node.getOutgoingLinks();
                incomingLinks = node.getIncomingLinks();

                Object[] nodeData_initialNetPlan = new Object[netPlanViewTableHeader.length];
                nodeData_initialNetPlan[0] = null;
                nodeData_initialNetPlan[1] = null;
                nodeData_initialNetPlan[2] = null;
                nodeData_initialNetPlan[3] = node.getName();
                nodeData_initialNetPlan[4] = node.isUp();
                nodeData_initialNetPlan[5] = node.getXYPositionMap().getX();
                nodeData_initialNetPlan[6] = node.getXYPositionMap().getY();
                nodeData_initialNetPlan[7] = outgoingLinks.isEmpty() ? "none" : outgoingLinks.size() + " (" + CollectionUtils.join(outgoingLinks, ", ") + ")";
                nodeData_initialNetPlan[8] = incomingLinks.isEmpty() ? "none" : incomingLinks.size() + " (" + CollectionUtils.join(incomingLinks, ", ") + ")";
                nodeData_initialNetPlan[9] = node.getIngressOfferedTraffic();
                nodeData_initialNetPlan[10] = node.getEgressOfferedTraffic();
                nodeData_initialNetPlan[11] = node.getIngressOfferedMulticastTraffic();
                nodeData_initialNetPlan[12] = node.getEgressOfferedMulticastTraffic();
                nodeData_initialNetPlan[13] = node.getSRGs().isEmpty() ? "none" : node.getSRGs().size() + " (" + CollectionUtils.join(currentState.getIndexes(node.getSRGs()), ", ") + ")";
                nodeData_initialNetPlan[14] = StringUtils.mapToString(node.getAttributes());
                allNodeData.add(nodeData_initialNetPlan);
            }
        }
        return allNodeData;
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
        return np.hasNodes();
    }

    public int[] getColumnsOfSpecialComparatorForSorting() {
        return new int[]{7, 8};
    }

    private static TableModel createTableModel(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        TableModel nodeTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (getValueAt(rowIndex, columnIndex) == null) return false;
                if (columnIndex == COLUMN_SHOWHIDE) return true;
                if (!networkViewer.isEditable()) return false;
                return IntUtils.contains(new int[]{COLUMN_NAME, COLUMN_STATE, COLUMN_XCOORD, COLUMN_YCOORD}, columnIndex);
            }

            @Override
            public void setValueAt(Object newValue, int row, int column) {
//				System.out.println ("set Value node, newValue: " + newValue + ", row: " + row + ", col: " + column);
                Object oldValue = getValueAt(row, column);

				/* If value doesn't change, exit from function */
                if (newValue != null && newValue.equals(oldValue)) return;

                NetPlan netPlan = networkViewer.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long nodeId = (Long) getValueAt(row, 0);
                final Node node = netPlan.getNodeFromId(nodeId);
                                /* Perform checks, if needed */
//				System.out.println ("set Value node: " + node + ", newValue: " + newValue + ", row: " + row + ", col: " + column);
                try {
                    switch (column) {
                        case COLUMN_SHOWHIDE:
                            if (newValue == null) return;
                            boolean visible = (Boolean) newValue;
                            topologyPanel.getCanvas().setNodeVisible(node, visible);
                            topologyPanel.getCanvas().refresh();
                            break;

                        case COLUMN_NAME:
                            netPlan.getNodeFromId(nodeId).setName(newValue.toString());
                            topologyPanel.getCanvas().refresh();
                            break;

                        case COLUMN_STATE:
                            boolean isNodeUp = (Boolean) newValue;
                            node.setFailureState(isNodeUp);
                            topologyPanel.getCanvas().refresh();
                            networkViewer.updateNetPlanView();
                            break;

                        case COLUMN_XCOORD:
                        case COLUMN_YCOORD:
                            Point2D newPosition = column == COLUMN_XCOORD ? new Point2D.Double(Double.parseDouble(newValue.toString()), node.getXYPositionMap().getY()) : new Point2D.Double(node.getXYPositionMap().getX(), Double.parseDouble(newValue.toString()));
                            node.setXYPositionMap(newPosition);
                            topologyPanel.getCanvas().updateNodeXYPosition(node);
                            topologyPanel.getCanvas().refresh();
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying node");
//					ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying node, Object newValue: " + newValue + ", int row: " + row + ", int column: " + column);
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };

        return nodeTableModel;
    }

    private void setDefaultCellRenderers(final IGUINetworkViewer networkViewer) {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());

        setDefaultRenderer(Boolean.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Boolean.class), networkViewer, NetworkElementType.NODE));
        setDefaultRenderer(Double.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Double.class), networkViewer, NetworkElementType.NODE));
        setDefaultRenderer(Object.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Object.class), networkViewer, NetworkElementType.NODE));
        setDefaultRenderer(Float.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Float.class), networkViewer, NetworkElementType.NODE));
        setDefaultRenderer(Long.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Long.class), networkViewer, NetworkElementType.NODE));
        setDefaultRenderer(Integer.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Integer.class), networkViewer, NetworkElementType.NODE));
        setDefaultRenderer(String.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(String.class), networkViewer, NetworkElementType.NODE));
    }

    private void setSpecificCellRenderers() {
    }

    public void setColumnRowSorting(boolean allowShowInitialNetPlan) {
        if (allowShowInitialNetPlan) setRowSorter(new CurrentAndPlannedStateTableSorter(getModel()));
        else setAutoCreateRowSorter(true);
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_OUTLINKS, new IGUINetworkViewer.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_INLINKS, new IGUINetworkViewer.ColumnComparator());
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
                                networkViewer.removeNode((long) itemId);
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
                            netPlan.removeAllNodes();
                            networkViewer.getTopologyPanel().getCanvas().updateTopology(netPlan);
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
                networkViewer.showNode((long) itemId);
                break;
            default:
                break;
        }
    }

    private boolean isTableEmpty() {
        return !networkViewer.getDesign().hasNodes();
    }

    private JMenuItem getAddOption() {
        JMenuItem addItem = addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = networkViewer.getDesign();

                try {
                    Node node = netPlan.addNode(0, 0, null, null);
                    node.setName("Node " + node.getIndex());
                    networkViewer.getTopologyPanel().getCanvas().addNode(node);
                    networkViewer.getTopologyPanel().getCanvas().refresh();
                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });
        return addItem;
    }

    private List<JComponent> getExtraAddOptions() {
        return new LinkedList<JComponent>();
    }

    private List<JComponent> getExtraOptions(final int row, final Object itemId) {
        List<JComponent> options = new LinkedList<JComponent>();

        final int numRows = model.getRowCount();
        final NetPlan netPlan = networkViewer.getDesign();

        if (itemId != null) {
            JMenuItem switchCoordinates_thisNode = new JMenuItem("Switch node coordinates from (x,y) to (y,x)");

            switchCoordinates_thisNode.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NetPlan netPlan = networkViewer.getDesign();
                    long nodeId = (long) itemId;
                    Point2D currentPosition = netPlan.getNodeFromId(nodeId).getXYPositionMap();
                    netPlan.getNodeFromId(nodeId).setXYPositionMap(new Point2D.Double(currentPosition.getY(), currentPosition.getX()));
                    networkViewer.getTopologyPanel().getCanvas().refresh();
                    networkViewer.updateNetPlanView();
                }
            });

            options.add(switchCoordinates_thisNode);

            JMenuItem xyPositionFromAttributes_thisNode = new JMenuItem("Set node coordinates from attributes");

            xyPositionFromAttributes_thisNode.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NetPlan netPlan = networkViewer.getDesign();

                    Set<String> attributeSet = new LinkedHashSet<String>();
                    long nodeId = (long) itemId;
                    attributeSet.addAll(netPlan.getNodeFromId(nodeId).getAttributes().keySet());

                    try {
                        if (attributeSet.isEmpty()) throw new Exception("No attribute to select");

                        final JComboBox latSelector = new WiderJComboBox();
                        final JComboBox lonSelector = new WiderJComboBox();
                        for (String attribute : attributeSet) {
                            latSelector.addItem(attribute);
                            lonSelector.addItem(attribute);
                        }

                        JPanel pane = new JPanel(new MigLayout("", "[][grow]", "[][]"));
                        pane.add(new JLabel("X-coordinate / Longitude: "));
                        pane.add(lonSelector, "growx, wrap");
                        pane.add(new JLabel("Y-coordinate / Latitude: "));
                        pane.add(latSelector, "growx, wrap");

                        while (true) {
                            int result = JOptionPane.showConfirmDialog(null, pane, "Please select the attributes for coordinates", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return;

                            try {
                                String latAttribute = latSelector.getSelectedItem().toString();
                                String lonAttribute = lonSelector.getSelectedItem().toString();

                                netPlan.getNodeFromId(nodeId).setXYPositionMap(new Point2D.Double(Double.parseDouble(netPlan.getNodeFromId(nodeId).getAttribute(lonAttribute)), Double.parseDouble(netPlan.getNodeFromId(nodeId).getAttribute(latAttribute))));
                                networkViewer.getTopologyPanel().getCanvas().refresh();
                                networkViewer.updateNetPlanView();
                                break;
                            } catch (Throwable ex) {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error retrieving coordinates from attributes");
                                break;
                            }
                        }
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error retrieving coordinates from attributes");
                    }
                }
            });

            options.add(xyPositionFromAttributes_thisNode);

            JMenuItem nameFromAttribute_thisNode = new JMenuItem("Set node name from attribute");
            nameFromAttribute_thisNode.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NetPlan netPlan = networkViewer.getDesign();

                    Set<String> attributeSet = new LinkedHashSet<String>();
                    long nodeId = (long) itemId;
                    attributeSet.addAll(netPlan.getNodeFromId(nodeId).getAttributes().keySet());

                    try {
                        if (attributeSet.isEmpty()) throw new Exception("No attribute to select");

                        final JComboBox selector = new WiderJComboBox();
                        for (String attribute : attributeSet)
                            selector.addItem(attribute);

                        JPanel pane = new JPanel(new MigLayout("", "[][grow]", "[]"));
                        pane.add(new JLabel("Name: "));
                        pane.add(selector, "growx, wrap");

                        while (true) {
                            int result = JOptionPane.showConfirmDialog(null, pane, "Please select the attribute for name", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return;

                            try {
                                String name = selector.getSelectedItem().toString();
                                netPlan.getNodeFromId(nodeId).setName(netPlan.getNodeFromId(nodeId).getAttribute(name));
                                networkViewer.getTopologyPanel().getCanvas().refresh();
                                networkViewer.updateNetPlanView();

                                break;
                            } catch (Throwable ex) {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error retrieving name from attribute");
                                break;
                            }
                        }
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error retrieving name from attribute");
                    }
                }
            });

            options.add(nameFromAttribute_thisNode);
        }

        if (numRows > 1) {
            if (!options.isEmpty()) options.add(new JPopupMenu.Separator());

            JMenuItem switchCoordinates_allNodes = new JMenuItem("Switch all node coordinates from (x,y) to (y,x)");

            switchCoordinates_allNodes.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NetPlan netPlan = networkViewer.getDesign();
                    Collection<Long> nodeIds = netPlan.getNodeIds();
                    for (long nodeId : nodeIds) {
                        Point2D currentPosition = netPlan.getNodeFromId(nodeId).getXYPositionMap();
                        netPlan.getNodeFromId(nodeId).setXYPositionMap(new Point2D.Double(currentPosition.getY(), currentPosition.getX()));
                        networkViewer.getTopologyPanel().getCanvas().refresh();
                    }

                    networkViewer.getTopologyPanel().getCanvas().refresh();
                    networkViewer.updateNetPlanView();
                }
            });

            options.add(switchCoordinates_allNodes);

            JMenuItem xyPositionFromAttributes_allNodes = new JMenuItem("Set all node coordinates from attributes");

            xyPositionFromAttributes_allNodes.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NetPlan netPlan = networkViewer.getDesign();

                    Set<String> attributeSet = new LinkedHashSet<String>();
                    Collection<Long> nodeIds = netPlan.getNodeIds();
                    for (long nodeId : nodeIds)
                        attributeSet.addAll(netPlan.getNodeFromId(nodeId).getAttributes().keySet());

                    try {
                        if (attributeSet.isEmpty()) throw new Exception("No attribute to select");

                        final JComboBox latSelector = new WiderJComboBox();
                        final JComboBox lonSelector = new WiderJComboBox();
                        for (String attribute : attributeSet) {
                            latSelector.addItem(attribute);
                            lonSelector.addItem(attribute);
                        }

                        JPanel pane = new JPanel(new MigLayout("", "[][grow]", "[][]"));
                        pane.add(new JLabel("X-coordinate / Longitude: "));
                        pane.add(lonSelector, "growx, wrap");
                        pane.add(new JLabel("Y-coordinate / Latitude: "));
                        pane.add(latSelector, "growx, wrap");

                        while (true) {
                            int result = JOptionPane.showConfirmDialog(null, pane, "Please select the attributes for coordinates", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return;

                            try {
                                String latAttribute = latSelector.getSelectedItem().toString();
                                String lonAttribute = lonSelector.getSelectedItem().toString();

                                for (long nodeId : nodeIds) {
                                    try {
                                        netPlan.getNodeFromId(nodeId).setXYPositionMap(new Point2D.Double(Double.parseDouble(netPlan.getNodeFromId(nodeId).getAttribute(lonAttribute)), Double.parseDouble(netPlan.getNodeFromId(nodeId).getAttribute(latAttribute))));
                                    } catch (Throwable e1) {
                                    }
                                }

                                networkViewer.getTopologyPanel().getCanvas().refresh();
                                networkViewer.updateNetPlanView();
                                break;
                            } catch (Throwable ex) {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error retrieving coordinates from attributes");
                                break;
                            }
                        }
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error retrieving coordinates from attributes");
                    }
                }
            });

            options.add(xyPositionFromAttributes_allNodes);

            JMenuItem nameFromAttribute_allNodes = new JMenuItem("Set all node names from attribute");
            nameFromAttribute_allNodes.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NetPlan netPlan = networkViewer.getDesign();

                    Set<String> attributeSet = new LinkedHashSet<String>();
                    Collection<Long> nodeIds = netPlan.getNodeIds();
                    for (long nodeId : nodeIds)
                        attributeSet.addAll(netPlan.getNodeFromId(nodeId).getAttributes().keySet());

                    try {
                        if (attributeSet.isEmpty()) throw new Exception("No attribute to select");

                        final JComboBox selector = new WiderJComboBox();
                        for (String attribute : attributeSet)
                            selector.addItem(attribute);

                        JPanel pane = new JPanel(new MigLayout("", "[][grow]", "[]"));
                        pane.add(new JLabel("Name: "));
                        pane.add(selector, "growx, wrap");

                        while (true) {
                            int result = JOptionPane.showConfirmDialog(null, pane, "Please select the attribute for name", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return;

                            try {
                                String name = selector.getSelectedItem().toString();

                                for (long nodeId : nodeIds) {
                                    try {
                                        netPlan.getNodeFromId(nodeId).setName(netPlan.getNodeFromId(nodeId).getAttribute(name));
                                    } catch (Throwable e1) {
                                    }
                                }
                                networkViewer.getTopologyPanel().getCanvas().refresh();
                                networkViewer.updateNetPlanView();
                                break;
                            } catch (Throwable ex) {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error retrieving name from attribute");
                                break;
                            }
                        }
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error retrieving name from attribute");
                    }
                }
            });

            options.add(nameFromAttribute_allNodes);
        }


        return options;
    }

    private List<JComponent> getForcedOptions() {
        List<JComponent> options = new LinkedList<JComponent>();

        final int numRows = model.getRowCount();
        if (numRows > 1) {
            JMenuItem showAllNodes = new JMenuItem("Show all nodes");
            showAllNodes.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (int row = 0; row < numRows; row++)
                        if (model.getValueAt(row, COLUMN_SHOWHIDE) != null)
                            model.setValueAt(true, row, COLUMN_SHOWHIDE);
                }
            });

            options.add(showAllNodes);

            JMenuItem hideAllNodes = new JMenuItem("Hide all nodes");
            hideAllNodes.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int numRows = model.getRowCount();
                    for (int row = 0; row < numRows; row++)
                        if (model.getValueAt(row, COLUMN_SHOWHIDE) != null)
                            model.setValueAt(false, row, COLUMN_SHOWHIDE);
                }
            });

            options.add(hideAllNodes);
        }

        return options;
    }
}
