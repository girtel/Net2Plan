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
import com.net2plan.gui.tools.IGUINetworkViewer;
import com.net2plan.gui.utils.*;
import com.net2plan.gui.utils.CellRenderers.NumberCellRenderer;
import com.net2plan.gui.utils.topology.TopologyPanel;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 */
public class AdvancedJTable_link extends AdvancedJTableNetworkElement {
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_INDEX = 1;
    public static final int COLUMN_SHOWHIDE = 2;
    public static final int COLUMN_ORIGINNODE = 3;
    public static final int COLUMN_DESTNODE = 4;
    public static final int COLUMN_STATE = 5;
    public static final int COLUMN_CAPACITY = 6;
    public static final int COLUMN_CARRIEDTRAFFIC = 7;
    public static final int COLUMN_TRAFFICRESERVEDFORPROTECTION = 8;
    public static final int COLUMN_UTILIZATION = 9;
    public static final int COLUMN_UTILIZATIONWOPROTECTION = 10;
    public static final int COLUMN_ISBOTTLENECK = 11;
    public static final int COLUMN_LENGTH = 12;
    public static final int COLUMN_PROPSPEED = 13;
    public static final int COLUMN_PROPDELAYMS = 14;
    public static final int COLUMN_NUMROUTES = 15;
    public static final int COLUMN_NUMSEGMENTS = 16;
    public static final int COLUMN_NUMFORWRULES = 17;
    public static final int COLUMN_NUMTREES = 18;
    public static final int COLUMN_SRGS = 19;
    public static final int COLUMN_COUPLEDTODEMAND = 20;
    public static final int COLUMN_ATTRIBUTES = 21;
    private static final String netPlanViewTabName = "Links";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "Show/Hide", "Origin node", "Destination node", "State", "Capacity", "Carried traffic", "Reserved for protection", "Utilization", "Utilization (w.o. protection)", "Is bottleneck?", "Length (km)", "Propagation speed (km/s)", "Propagation delay (ms)", "# Routes", "# Segments", "# Forwarding rules", "# Multicast trees", "SRGs", "Coupled to demand", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)", "Index (consecutive integer starting in zero)", "Indicates whether or not the link is visible in the topology canvas (if some of the end-nodes is hidden, this link will become hidden, even though the link is set as visible)", "Origin node", "Destination node", "Indicates whether the link is in up/down state", "Capacity", "Carried traffic (summing unicast and multicast)", "Reserved for protection", "Utilization (carried traffic plus reserved bandwidth, divided by link capacity)", "Utilization excluding reserved capacity for protection (link carried traffic divided by link capacity)", "Indicates whether this link has the highest utilization in the network", "Length (km)", "Propagation speed (km/s)", "Propagation delay (ms)", "Number of routes traversing the link", "Number of protection segments traversing the link", "Number of forwarding rules for this link", "Number of multicast trees traversing the link", "SRGs including this link", "Indicates the coupled lower layer demand, if any, or empty", "Link-specific attributes");

    public AdvancedJTable_link(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        super(createTableModel(networkViewer, topologyPanel), networkViewer, NetworkElementType.LINK);
        setDefaultCellRenderers(networkViewer);
        setSpecificCellRenderers();
        setColumnRowSorting(networkViewer.inOnlineSimulationMode());
    }

    public List<Object[]> getAllData(NetPlan currentState, TopologyPanel topologyPanel, NetPlan initialState) {
        double max_rho_e = 0;
        for (Link link : currentState.getLinks())
            max_rho_e = Math.max(max_rho_e, link.getOccupiedCapacityIncludingProtectionSegments() / link.getCapacity());
        double max_rho_e_initialNetPlan = -1;
        if (initialState != null) for (Link link : initialState.getLinks())
            max_rho_e_initialNetPlan = Math.max(max_rho_e_initialNetPlan, link.getOccupiedCapacityIncludingProtectionSegments() / link.getCapacity());
        List<Object[]> allLinkData = new LinkedList<Object[]>();
        for (Link link : currentState.getLinks()) {
            Set<SharedRiskGroup> srgIds_thisLink = link.getSRGs();
            Set<Route> traversingRoutes = currentState.getRoutingType() == RoutingType.SOURCE_ROUTING ? link.getTraversingRoutes() : new LinkedHashSet<Route>();
            Set<ProtectionSegment> traversingSegments = currentState.getRoutingType() == RoutingType.SOURCE_ROUTING ? link.getTraversingProtectionSegments() : new LinkedHashSet<ProtectionSegment>();
            Set<MulticastTree> traversingMulticastTrees = link.getTraversingTrees();
            DoubleMatrix1D forwardingRules = currentState.getRoutingType() == RoutingType.HOP_BY_HOP_ROUTING ? currentState.getMatrixDemandBasedForwardingRules().viewColumn(link.getIndex()).copy() : DoubleFactory1D.sparse.make(currentState.getNumberOfDemands(), 0);
            int numRoutes = traversingRoutes.size();
            int numSegments = traversingSegments.size();
            int numForwardingRules = 0;
            for (int d = 0; d < forwardingRules.size(); d++) if (forwardingRules.get(d) != 0) numForwardingRules++;
            int numMulticastTrees = traversingMulticastTrees.size();

            String routesString = numRoutes + (numRoutes > 0 ? " (" + CollectionUtils.join(NetPlan.getIndexes(traversingRoutes), ", ") + ")" : "");
            String segmentsString = numSegments + (numSegments > 0 ? " (" + CollectionUtils.join(NetPlan.getIndexes(traversingSegments), ", ") + ")" : "");
            String multicastTreesString = numMulticastTrees + (numMulticastTrees > 0 ? " (" + CollectionUtils.join(NetPlan.getIndexes(traversingMulticastTrees), ", ") + ")" : "");
            StringBuilder forwardingRulesString = new StringBuilder(Integer.toString(numForwardingRules));

            Demand coupledDemand = link.getCoupledDemand();
            MulticastDemand coupledMulticastDemand = link.getCoupledMulticastDemand();

            Node originNode = link.getOriginNode();
            Node destinationNode = link.getDestinationNode();
            String originNodeName = originNode.getName();
            String destinationNodeName = destinationNode.getName();

            double rho_e = link.getOccupiedCapacityIncludingProtectionSegments() == 0 ? 0 : link.getCapacity() == 0 ? Double.MAX_VALUE : link.getOccupiedCapacityIncludingProtectionSegments() / link.getCapacity();
            Object[] linkData = new Object[netPlanViewTableHeader.length];
            linkData[COLUMN_ID] = link.getId();
            linkData[COLUMN_INDEX] = link.getIndex();
            linkData[COLUMN_SHOWHIDE] = topologyPanel.getCanvas().isLinkVisible(link);
            linkData[COLUMN_ORIGINNODE] = originNode.getIndex() + (originNodeName.isEmpty() ? "" : " (" + originNodeName + ")");
            linkData[COLUMN_DESTNODE] = destinationNode.getIndex() + (destinationNodeName.isEmpty() ? "" : " (" + destinationNodeName + ")");
            linkData[COLUMN_STATE] = !link.isDown();
            linkData[COLUMN_CAPACITY] = link.getCapacity();
            linkData[COLUMN_CARRIEDTRAFFIC] = link.getCarriedTrafficIncludingProtectionSegments();
            linkData[COLUMN_TRAFFICRESERVEDFORPROTECTION] = link.getReservedCapacityForProtection();
            linkData[COLUMN_UTILIZATION] = rho_e;
            linkData[COLUMN_UTILIZATIONWOPROTECTION] = link.getOccupiedCapacityNotIncludingProtectionSegments() == 0 ? 0 : (link.getCapacity() <= link.getReservedCapacityForProtection()) ? Double.MAX_VALUE : link.getOccupiedCapacityNotIncludingProtectionSegments() / (link.getCapacity() - link.getReservedCapacityForProtection());
            linkData[COLUMN_ISBOTTLENECK] = DoubleUtils.isEqualWithinRelativeTolerance(max_rho_e, rho_e, Configuration.precisionFactor);
            linkData[COLUMN_LENGTH] = link.getLengthInKm();
            linkData[COLUMN_PROPSPEED] = link.getPropagationSpeedInKmPerSecond();
            linkData[COLUMN_PROPDELAYMS] = link.getPropagationDelayInMs();
            linkData[COLUMN_NUMROUTES] = routesString;
            linkData[COLUMN_NUMSEGMENTS] = segmentsString;
            linkData[COLUMN_NUMFORWRULES] = forwardingRulesString.toString();
            linkData[COLUMN_NUMTREES] = multicastTreesString.toString();
            linkData[COLUMN_SRGS] = srgIds_thisLink.isEmpty() ? "none" : srgIds_thisLink.size() + " (" + CollectionUtils.join(NetPlan.getIndexes(srgIds_thisLink), ", ") + ")";
            linkData[COLUMN_COUPLEDTODEMAND] = coupledDemand != null ? "d" + coupledDemand.getIndex() + " (layer " + coupledDemand.getLayer() + ")" : (coupledMulticastDemand == null ? "" : "d" + coupledMulticastDemand.getIndex() + " (layer " + coupledMulticastDemand.getLayer() + ")");
            linkData[COLUMN_ATTRIBUTES] = StringUtils.mapToString(link.getAttributes());
            allLinkData.add(linkData);

            if (initialState != null && initialState.getLinkFromId(link.getId()) != null) {
                link = initialState.getLinkFromId(link.getId());
                srgIds_thisLink = link.getSRGs();
                traversingRoutes = initialState.getRoutingType() == RoutingType.SOURCE_ROUTING ? link.getTraversingRoutes() : new LinkedHashSet<Route>();
                traversingSegments = initialState.getRoutingType() == RoutingType.SOURCE_ROUTING ? link.getTraversingProtectionSegments() : new LinkedHashSet<ProtectionSegment>();
                traversingMulticastTrees = link.getTraversingTrees();
                forwardingRules = initialState.getRoutingType() == RoutingType.HOP_BY_HOP_ROUTING ? currentState.getMatrixDemandBasedForwardingRules().viewColumn(link.getIndex()).copy() : DoubleFactory1D.sparse.make(initialState.getNumberOfDemands(), 0);
                numRoutes = traversingRoutes.size();
                numSegments = traversingSegments.size();
                numForwardingRules = 0;
                for (int d = 0; d < forwardingRules.size(); d++) if (forwardingRules.get(d) != 0) numForwardingRules++;
                numMulticastTrees = traversingMulticastTrees.size();

                routesString = numRoutes + (numRoutes > 0 ? " (" + CollectionUtils.join(NetPlan.getIndexes(traversingRoutes), ", ") + ")" : "");
                segmentsString = numSegments + (numSegments > 0 ? " (" + CollectionUtils.join(NetPlan.getIndexes(traversingSegments), ", ") + ")" : "");
                multicastTreesString = numMulticastTrees + (numMulticastTrees > 0 ? " (" + CollectionUtils.join(NetPlan.getIndexes(traversingMulticastTrees), ", ") + ")" : "");
                forwardingRulesString = new StringBuilder(Integer.toString(numForwardingRules));

                coupledDemand = link.getCoupledDemand();
                coupledMulticastDemand = link.getCoupledMulticastDemand();

                originNode = link.getOriginNode();
                destinationNode = link.getDestinationNode();
                originNodeName = originNode.getName();
                destinationNodeName = destinationNode.getName();

                rho_e = link.getOccupiedCapacityIncludingProtectionSegments() == 0 ? 0 : link.getCapacity() == 0 ? Double.MAX_VALUE : link.getOccupiedCapacityIncludingProtectionSegments() / link.getCapacity();

                Object[] linkData_initialNetPlan = new Object[netPlanViewTableHeader.length];
                linkData_initialNetPlan[COLUMN_ID] = null;
                linkData_initialNetPlan[COLUMN_INDEX] = null;
                linkData_initialNetPlan[COLUMN_SHOWHIDE] = null;
                linkData_initialNetPlan[COLUMN_ORIGINNODE] = null;
                linkData_initialNetPlan[COLUMN_DESTNODE] = null;
                linkData_initialNetPlan[COLUMN_STATE] = !link.isDown();
                linkData_initialNetPlan[COLUMN_CAPACITY] = link.getCapacity();
                linkData_initialNetPlan[COLUMN_CARRIEDTRAFFIC] = link.getCarriedTrafficIncludingProtectionSegments();
                linkData_initialNetPlan[COLUMN_TRAFFICRESERVEDFORPROTECTION] = link.getReservedCapacityForProtection();
                linkData_initialNetPlan[COLUMN_UTILIZATION] = rho_e;
                linkData_initialNetPlan[COLUMN_UTILIZATIONWOPROTECTION] = link.getOccupiedCapacityNotIncludingProtectionSegments() == 0 ? 0 : (link.getCapacity() <= link.getReservedCapacityForProtection()) ? Double.MAX_VALUE : link.getOccupiedCapacityNotIncludingProtectionSegments() / (link.getCapacity() - link.getReservedCapacityForProtection());
                linkData_initialNetPlan[COLUMN_ISBOTTLENECK] = DoubleUtils.isEqualWithinRelativeTolerance(max_rho_e, rho_e, Configuration.precisionFactor);
                linkData_initialNetPlan[COLUMN_LENGTH] = link.getLengthInKm();
                linkData_initialNetPlan[COLUMN_PROPSPEED] = link.getPropagationSpeedInKmPerSecond();
                linkData_initialNetPlan[COLUMN_PROPDELAYMS] = 1000 * link.getPropagationDelayInMs();
                linkData_initialNetPlan[COLUMN_NUMROUTES] = routesString;
                linkData_initialNetPlan[COLUMN_NUMSEGMENTS] = segmentsString;
                linkData_initialNetPlan[COLUMN_NUMFORWRULES] = forwardingRulesString.toString();
                linkData_initialNetPlan[COLUMN_NUMTREES] = multicastTreesString.toString();
                linkData_initialNetPlan[COLUMN_SRGS] = srgIds_thisLink.isEmpty() ? "none" : srgIds_thisLink.size() + " (" + CollectionUtils.join(NetPlan.getIndexes(srgIds_thisLink), ", ") + ")";
                linkData_initialNetPlan[COLUMN_COUPLEDTODEMAND] = coupledDemand != null ? "d" + coupledDemand.getIndex() + " (layer " + coupledDemand.getLayer() + ")" : (coupledMulticastDemand == null ? "" : "d" + coupledMulticastDemand.getIndex() + " (layer " + coupledMulticastDemand.getLayer() + ")");
                linkData_initialNetPlan[COLUMN_ATTRIBUTES] = StringUtils.mapToString(link.getAttributes());
                allLinkData.add(linkData_initialNetPlan);
            }
        }

        return allLinkData;
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
        return np.hasLinks();
    }

    public int[] getColumnsOfSpecialComparatorForSorting() {
        return new int[]{3, 4, 16, 17, 18, 19};
    } //{ return new int [] { 3,4,6,16,17,18,19 }; }

    private static TableModel createTableModel(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        TableModel linkTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (getValueAt(rowIndex, columnIndex) == null) return false;
                if (columnIndex == COLUMN_SHOWHIDE) return true;
                if (!networkViewer.isEditable()) return false;
                switch (columnIndex) {
                    case COLUMN_STATE:
                    case COLUMN_LENGTH:
                    case COLUMN_PROPSPEED:
                        return true;

                    case COLUMN_CAPACITY:
                        NetPlan netPlan = networkViewer.getDesign();
                        if (getValueAt(rowIndex, 0) == null) rowIndex = rowIndex - 1;
                        final long linkId = (Long) getValueAt(rowIndex, 0);
                        final Link link = netPlan.getLinkFromId(linkId);
                        if (link.isCoupled()) return false;
                        else return true;
                    default:
                        return false;
                }
            }

            @Override
            public void setValueAt(Object newValue, int row, int column) {
                Object oldValue = getValueAt(row, column);

				/* If value doesn't change, exit from function */
                if (newValue != null && newValue.equals(oldValue)) return;

                NetPlan netPlan = networkViewer.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long linkId = (Long) getValueAt(row, 0);
                final Link link = netPlan.getLinkFromId(linkId);

				/* Perform checks, if needed */
                try {
                    switch (column) {
                        case COLUMN_SHOWHIDE:
                            if (newValue == null) return;
                            boolean visible = (Boolean) newValue;
                            topologyPanel.getCanvas().setLinkVisible(link, visible);
                            break;

                        case COLUMN_STATE:
                            boolean isLinkUp = (Boolean) newValue;
                            link.setFailureState(isLinkUp);
                            topologyPanel.getCanvas().refresh();
                            networkViewer.updateNetPlanView();
                            break;

                        case COLUMN_CAPACITY:
                            String text = newValue.toString();
                            link.setCapacity(text.equalsIgnoreCase("inf") ? Double.MAX_VALUE : Double.parseDouble(text));
                            newValue = link.getCapacity();
                            networkViewer.updateNetPlanView();
                            break;

                        case COLUMN_LENGTH:
                            link.setLengthInKm(Double.parseDouble(newValue.toString()));
                            networkViewer.updateNetPlanView();
                            break;

                        case COLUMN_PROPSPEED:
                            link.setPropagationSpeedInKmPerSecond(Double.parseDouble(newValue.toString()));
                            networkViewer.updateNetPlanView();
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying link");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };
        return linkTableModel;
    }

    private void setDefaultCellRenderers(final IGUINetworkViewer networkViewer) {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());

        setDefaultRenderer(Boolean.class, new CellRenderers.LinkRenderer(getDefaultRenderer(Boolean.class), networkViewer));
        setDefaultRenderer(Double.class, new CellRenderers.LinkRenderer(getDefaultRenderer(Double.class), networkViewer));
        setDefaultRenderer(Object.class, new CellRenderers.LinkRenderer(getDefaultRenderer(Object.class), networkViewer));
        setDefaultRenderer(Float.class, new CellRenderers.LinkRenderer(getDefaultRenderer(Float.class), networkViewer));
        setDefaultRenderer(Long.class, new CellRenderers.LinkRenderer(getDefaultRenderer(Long.class), networkViewer));
        setDefaultRenderer(Integer.class, new CellRenderers.LinkRenderer(getDefaultRenderer(Integer.class), networkViewer));
        setDefaultRenderer(String.class, new CellRenderers.LinkRenderer(getDefaultRenderer(String.class), networkViewer));
    }

    private void setSpecificCellRenderers() {
        getColumnModel().getColumn(convertColumnIndexToView(COLUMN_CAPACITY)).setCellEditor(new LinkCapacityCellEditor(new JTextField()));
    }

    private static class LinkCapacityCellEditor extends DefaultCellEditor {
        private static final Border black = new LineBorder(Color.black);
        private static final Border red = new LineBorder(Color.red);
        private final JTextField textField;

        public LinkCapacityCellEditor(JTextField textField) {
            super(textField);
            this.textField = textField;
            this.textField.setHorizontalAlignment(JTextField.RIGHT);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            textField.setBorder(black);
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }

        @Override
        public boolean stopCellEditing() {
            try {
                String text = textField.getText();
                if (!text.equalsIgnoreCase("inf")) {
                    double v = Double.valueOf(textField.getText());
                    if (v < 0) throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                textField.setBorder(red);
                return false;
            }

            return super.stopCellEditing();
        }
    }

    public void setColumnRowSorting(boolean allowShowInitialNetPlan) {
        if (allowShowInitialNetPlan) setRowSorter(new CurrentAndPlannedStateTableSorter(getModel()));
        else setAutoCreateRowSorter(true);
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_ORIGINNODE, new IGUINetworkViewer.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_DESTNODE, new IGUINetworkViewer.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_NUMROUTES, new IGUINetworkViewer.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_NUMSEGMENTS, new IGUINetworkViewer.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_NUMFORWRULES, new IGUINetworkViewer.ColumnComparator());
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

                    JMenuItem removeItem = new JMenuItem("Remove " + networkElementType);
                    removeItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NetPlan netPlan = networkViewer.getDesign();

                            try {
                                Link link = netPlan.getLinkFromId((long) itemId);
                                networkViewer.removeLink(link.getId());
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

                JMenuItem removeItems = new JMenuItem("Remove all " + networkElementType + "s");

                removeItems.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        NetPlan netPlan = networkViewer.getDesign();

                        try {
                            Collection<Long> linkIds = netPlan.getLinkIds();
                            for (long linkId : linkIds)
                                networkViewer.removeLink(linkId);
                            networkViewer.getTopologyPanel().getCanvas().refresh();
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
                networkViewer.showLink((long) itemId);
                break;

            case 2:
                int col = convertColumnIndexToModel(columnAtPoint(e.getPoint()));
                if (col == -1 || col >= getColumnCount()) return;

                NetPlan netPlan = networkViewer.getDesign();

                Link link = netPlan.getLinkFromId((long) itemId);
                switch (col) {
                    case COLUMN_ORIGINNODE:
                        networkViewer.showNode(link.getOriginNode().getId());
                        break;
                    case COLUMN_DESTNODE:
                        networkViewer.showNode(link.getDestinationNode().getId());
                        break;
                    case COLUMN_COUPLEDTODEMAND:
                        if (link.isCoupled()) {
                            if (link.getCoupledDemand() != null)
                                networkViewer.showDemand(link.getCoupledDemand().getId());
                            else if (link.getCoupledMulticastDemand() != null)
                                networkViewer.showMulticastDemand(link.getCoupledMulticastDemand().getId());
                        }
                        break;
                    default:
                        break;
                }

                break;
        }
    }

    private boolean isTableEmpty() {
        return !networkViewer.getDesign().hasLinks();
    }

    private JMenuItem getAddOption() {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        ;
        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = networkViewer.getDesign();
                try {
                    AdvancedJTable_demand.createLinkDemandGUI(networkElementType, networkViewer, networkViewer.getTopologyPanel());
                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });

        NetPlan netPlan = networkViewer.getDesign();
        int N = netPlan.getNumberOfNodes();
        if (N < 2) addItem.setEnabled(false);

        return addItem;
    }

    private List<JComponent> getExtraAddOptions() {
        List<JComponent> options = new LinkedList<JComponent>();
        NetPlan netPlan = networkViewer.getDesign();

        if (netPlan.getNumberOfNodes() >= 2) {
            final JMenuItem fullMeshEuclidean = new JMenuItem("Generate full-mesh (link length as Euclidean distance)");
            final JMenuItem fullMeshHaversine = new JMenuItem("Generate full-mesh (link length as Haversine distance)");
            options.add(fullMeshEuclidean);
            options.add(fullMeshHaversine);

            fullMeshEuclidean.addActionListener(new FullMeshTopologyActionListener(true));
            fullMeshHaversine.addActionListener(new FullMeshTopologyActionListener(false));
        }

        return options;
    }

    private List<JComponent> getExtraOptions(final int row, final Object itemId) {
        List<JComponent> options = new LinkedList<JComponent>();

        final int numRows = model.getRowCount();
        final NetPlan netPlan = networkViewer.getDesign();

        if (itemId != null) {
            final long linkId = (long) itemId;

            JMenuItem lengthToEuclidean_thisLink = new JMenuItem("Set link length to node-pair Euclidean distance");
            lengthToEuclidean_thisLink.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Link link = netPlan.getLinkFromId(linkId);
                    Node originNode = link.getOriginNode();
                    Node destinationNode = link.getDestinationNode();
                    double euclideanDistance = netPlan.getNodePairEuclideanDistance(originNode, destinationNode);
                    link.setLengthInKm(euclideanDistance);

                    networkViewer.updateNetPlanView();
                }
            });

            options.add(lengthToEuclidean_thisLink);

            JMenuItem lengthToHaversine_allNodes = new JMenuItem("Set link length to node-pair Haversine distance (longitude-latitude) in km");
            lengthToHaversine_allNodes.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Link link = netPlan.getLinkFromId(linkId);
                    Node originNode = link.getOriginNode();
                    Node destinationNode = link.getDestinationNode();
                    double haversineDistanceInKm = netPlan.getNodePairHaversineDistanceInKm(originNode, destinationNode);
                    link.setLengthInKm(haversineDistanceInKm);

                    networkViewer.updateNetPlanView();
                }
            });

            options.add(lengthToHaversine_allNodes);

            JMenuItem scaleLinkLength_thisLink = new JMenuItem("Scale link length");
            scaleLinkLength_thisLink.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double scaleFactor;

                    while (true) {
                        String str = JOptionPane.showInputDialog(null, "(Multiplicative) Scale factor", "Scale link length", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try {
                            scaleFactor = Double.parseDouble(str);
                            if (scaleFactor < 0) throw new RuntimeException();

                            break;
                        } catch (Throwable ex) {
                            ErrorHandling.showErrorDialog("Non-valid scale value. Please, introduce a non-negative number", "Error setting scale factor");
                        }
                    }

                    netPlan.getLinkFromId(linkId).setLengthInKm(netPlan.getLinkFromId(linkId).getLengthInKm() * scaleFactor);

                    networkViewer.updateNetPlanView();
                }
            });

            options.add(scaleLinkLength_thisLink);

            if (netPlan.isMultilayer()) {
                Link link = netPlan.getLinkFromId(linkId);
                if (link.getCoupledDemand() != null) {
                    JMenuItem decoupleLinkItem = new JMenuItem("Decouple link (if coupled to unicast demand)");
                    decoupleLinkItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            netPlan.getLinkFromId(linkId).getCoupledDemand().decouple();
                            model.setValueAt("", row, 20);
                            networkViewer.updateWarnings();
                        }
                    });

                    options.add(decoupleLinkItem);
                } else {
                    JMenuItem createLowerLayerDemandFromLinkItem = new JMenuItem("Create lower layer demand from link");
                    createLowerLayerDemandFromLinkItem.addActionListener(new ActionListener() {
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
                                int result = JOptionPane.showConfirmDialog(null, pane, "Please select the lower layer to create the demand", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                                if (result != JOptionPane.OK_OPTION) return;

                                try {
                                    long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                                    Link link = netPlan.getLinkFromId(linkId);
                                    netPlan.addDemand(link.getOriginNode(), link.getDestinationNode(), link.getCapacity(), link.getAttributes(), netPlan.getNetworkLayerFromId(layerId));
                                    networkViewer.updateNetPlanView();
                                    break;
                                } catch (Throwable ex) {
                                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error creating lower layer demand from link");
                                }
                            }
                        }
                    });

                    options.add(createLowerLayerDemandFromLinkItem);

                    JMenuItem coupleLinkToDemand = new JMenuItem("Couple link to lower layer demand");
                    coupleLinkToDemand.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Collection<Long> layerIds = netPlan.getNetworkLayerIds();
                            final JComboBox layerSelector = new WiderJComboBox();
                            final JComboBox demandSelector = new WiderJComboBox();
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

                                        demandSelector.removeAllItems();
                                        for (Demand demand : netPlan.getDemands(netPlan.getNetworkLayerFromId(selectedLayerId))) {
                                            if (demand.isCoupled()) continue;

                                            long ingressNodeId = demand.getIngressNode().getId();
                                            long egressNodeId = demand.getEgressNode().getId();
                                            String ingressNodeName = demand.getIngressNode().getName();
                                            String egressNodeName = demand.getEgressNode().getName();

                                            demandSelector.addItem(StringLabeller.unmodifiableOf(demand.getId(), "d" + demand.getId() + " [n" + ingressNodeId + " (" + ingressNodeName + ") -> n" + egressNodeId + " (" + egressNodeName + ")]"));
                                        }
                                    }

                                    if (demandSelector.getItemCount() == 0) {
                                        demandSelector.setEnabled(false);
                                    } else {
                                        demandSelector.setSelectedIndex(0);
                                        demandSelector.setEnabled(true);
                                    }
                                }
                            });

                            layerSelector.setSelectedIndex(-1);
                            layerSelector.setSelectedIndex(0);

                            JPanel pane = new JPanel(new MigLayout("", "[][grow]", "[][]"));
                            pane.add(new JLabel("Select layer: "));
                            pane.add(layerSelector, "growx, wrap");
                            pane.add(new JLabel("Select demand: "));
                            pane.add(demandSelector, "growx, wrap");

                            while (true) {
                                int result = JOptionPane.showConfirmDialog(null, pane, "Please select the lower layer demand", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                                if (result != JOptionPane.OK_OPTION) return;

                                try {
                                    long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                                    long demandId;
                                    try {
                                        demandId = (long) ((StringLabeller) demandSelector.getSelectedItem()).getObject();
                                    } catch (Throwable ex) {
                                        throw new RuntimeException("No demand was selected");
                                    }

                                    netPlan.getDemandFromId(demandId).coupleToUpperLayerLink(netPlan.getLinkFromId(linkId));

                                    networkViewer.updateNetPlanView();
                                    break;
                                } catch (Throwable ex) {
                                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error coupling lower layer demand to link");
                                }
                            }
                        }
                    });

                    options.add(coupleLinkToDemand);
                }
            }
        }

        if (numRows > 1) {
            if (!options.isEmpty()) options.add(new JPopupMenu.Separator());

            JMenuItem caFixValue = new JMenuItem("Set capacity to all");
            caFixValue.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double u_e;

                    while (true) {
                        String str = JOptionPane.showInputDialog(null, "Capacity value", "Set capacity to all links", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try {
                            u_e = Double.parseDouble(str);
                            if (u_e < 0) throw new NumberFormatException();

                            break;
                        } catch (NumberFormatException ex) {
                            ErrorHandling.showErrorDialog("Non-valid capacity value. Please, introduce a non-negative number", "Error setting capacity value");
                        }
                    }

                    NetPlan netPlan = networkViewer.getDesign();

                    try {
                        for (Link link : netPlan.getLinks()) link.setCapacity(u_e);

                        networkViewer.updateNetPlanView();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set capacity to all links");
                    }
                }
            });

            options.add(caFixValue);

            JMenuItem caFixValueUtilization = new JMenuItem("Set capacity to match a given utilization");
            caFixValueUtilization.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double utilization;

                    while (true) {
                        String str = JOptionPane.showInputDialog(null, "Link utilization value", "Set capacity to all links to match a given utilization", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try {
                            utilization = Double.parseDouble(str);
                            if (utilization <= 0) throw new NumberFormatException();

                            break;
                        } catch (NumberFormatException ex) {
                            ErrorHandling.showErrorDialog("Non-valid link utilization value. Please, introduce a strictly positive number", "Error setting link utilization value");
                        }
                    }

                    NetPlan netPlan = networkViewer.getDesign();

                    try {
                        for (Link link : netPlan.getLinks())
                            link.setCapacity(link.getOccupiedCapacityIncludingProtectionSegments() / utilization);
                        networkViewer.updateNetPlanView();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set capacity to all links according to a given link utilization");
                    }
                }
            });

            options.add(caFixValueUtilization);

            JMenuItem lengthToAll = new JMenuItem("Set link length to all");
            lengthToAll.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double l_e;

                    while (true) {
                        String str = JOptionPane.showInputDialog(null, "Link length value (in km)", "Set link length to all links", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try {
                            l_e = Double.parseDouble(str);
                            if (l_e < 0) throw new RuntimeException();

                            break;
                        } catch (Throwable ex) {
                            ErrorHandling.showErrorDialog("Non-valid link length value. Please, introduce a non-negative number", "Error setting link length");
                        }
                    }

                    NetPlan netPlan = networkViewer.getDesign();

                    try {
                        for (Link link : netPlan.getLinks()) link.setLengthInKm(l_e);

                        networkViewer.updateNetPlanView();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set link length to all links");
                    }
                }
            });

            options.add(lengthToAll);

            JMenuItem lengthToEuclidean_allLinks = new JMenuItem("Set all link lengths to node-pair Euclidean distance");
            lengthToEuclidean_allLinks.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NetPlan netPlan = networkViewer.getDesign();

                    try {
                        for (Link link : netPlan.getLinks()) {
                            link.setLengthInKm(netPlan.getNodePairEuclideanDistance(link.getOriginNode(), link.getDestinationNode()));
                        }

                        networkViewer.updateNetPlanView();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set link length value to all links");
                    }
                }
            });

            options.add(lengthToEuclidean_allLinks);

            JMenuItem lengthToHaversine_allLinks = new JMenuItem("Set all link lengths to node-pair Haversine distance (longitude-latitude) in km");
            lengthToHaversine_allLinks.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NetPlan netPlan = networkViewer.getDesign();

                    try {
                        for (Link link : netPlan.getLinks()) {
                            link.setLengthInKm(netPlan.getNodePairHaversineDistanceInKm(link.getOriginNode(), link.getDestinationNode()));
                        }

                        networkViewer.updateNetPlanView();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set link length value to all links");
                    }
                }
            });

            options.add(lengthToHaversine_allLinks);

            JMenuItem scaleLinkLength_allLinks = new JMenuItem("Scale all link lengths");
            scaleLinkLength_allLinks.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double scaleFactor;

                    while (true) {
                        String str = JOptionPane.showInputDialog(null, "(Multiplicative) Scale factor", "Scale (all) link length", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try {
                            scaleFactor = Double.parseDouble(str);
                            if (scaleFactor < 0) throw new RuntimeException();

                            break;
                        } catch (Throwable ex) {
                            ErrorHandling.showErrorDialog("Non-valid scale value. Please, introduce a non-negative number", "Error setting scale factor");
                        }
                    }

                    NetPlan netPlan = networkViewer.getDesign();

                    try {
                        for (Link link : netPlan.getLinks())
                            link.setLengthInKm(link.getLengthInKm() * scaleFactor);

                        networkViewer.updateNetPlanView();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to scale link length");
                    }
                }
            });

            options.add(scaleLinkLength_allLinks);

            if (netPlan.isMultilayer()) {
                final Set<Link> coupledLinksToUnicastDemands = netPlan.getLinksCoupledToUnicastDemands();
                if (!coupledLinksToUnicastDemands.isEmpty()) {
                    JMenuItem decoupleAllLinksItem = new JMenuItem("Decouple all links");
                    decoupleAllLinksItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            for (Link link : coupledLinksToUnicastDemands) link.getCoupledDemand().decouple();

                            int numRows = model.getRowCount();
                            for (int i = 0; i < numRows; i++) model.setValueAt("", i, 20);

                            networkViewer.updateWarnings();
                        }
                    });

                    options.add(decoupleAllLinksItem);
                }

                if (coupledLinksToUnicastDemands.size() < netPlan.getNumberOfLinks()) {
                    JMenuItem createLowerLayerDemandsFromLinksItem = new JMenuItem("Create lower layer unicast demands from uncoupled links");
                    createLowerLayerDemandsFromLinksItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            final JComboBox layerSelector = new WiderJComboBox();
                            for (NetworkLayer layer : netPlan.getNetworkLayers()) {
                                if (layer.getId() == netPlan.getNetworkLayerDefault().getId()) continue;

                                final String layerName = layer.getName();
                                String layerLabel = "Layer " + layer.getId();
                                if (!layerName.isEmpty()) layerLabel += " (" + layerName + ")";

                                layerSelector.addItem(StringLabeller.of(layer.getId(), layerLabel));
                            }

                            layerSelector.setSelectedIndex(0);

                            JPanel pane = new JPanel();
                            pane.add(new JLabel("Select layer: "));
                            pane.add(layerSelector);

                            while (true) {
                                int result = JOptionPane.showConfirmDialog(null, pane, "Please select the lower layer to create demands", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                                if (result != JOptionPane.OK_OPTION) return;

                                try {
                                    long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                                    NetworkLayer layer = netPlan.getNetworkLayerFromId(layerId);
                                    for (Link link : netPlan.getLinks())
                                        if (!link.isCoupled())
                                            link.coupleToNewDemandCreated(layer);

                                    networkViewer.updateNetPlanView();
                                    break;
                                } catch (Throwable ex) {
                                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error creating lower layer demands");
                                }
                            }
                        }
                    });

                    options.add(createLowerLayerDemandsFromLinksItem);
                }
            }
        }
        return options;
    }

    private List<JComponent> getForcedOptions() {
        List<JComponent> options = new LinkedList<JComponent>();

        final int numRows = model.getRowCount();
        if (numRows > 1) {
            JMenuItem showAllLinks = new JMenuItem("Show all links");
            showAllLinks.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int numRows = model.getRowCount();
                    for (int row = 0; row < numRows; row++)
                        if (model.getValueAt(row, COLUMN_SHOWHIDE) != null)
                            model.setValueAt(true, row, COLUMN_SHOWHIDE);
                }
            });

            options.add(showAllLinks);

            JMenuItem hideAllLinks = new JMenuItem("Hide all links");
            hideAllLinks.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int numRows = model.getRowCount();
                    for (int row = 0; row < numRows; row++)
                        if (model.getValueAt(row, COLUMN_SHOWHIDE) != null)
                            model.setValueAt(false, row, COLUMN_SHOWHIDE);
                }
            });

            options.add(hideAllLinks);
        }

        return options;
    }

    private class FullMeshTopologyActionListener implements ActionListener {
        private final boolean euclidean;

        public FullMeshTopologyActionListener(boolean euclidean) {
            this.euclidean = euclidean;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NetPlan netPlan = networkViewer.getDesign();
//			for (Link link : netPlan.getLinks()) topologyPanel.getCanvas().removeLink(link.getId ()); // PABLO
//			netPlan.removeAllLinks();
            for (long nodeId_1 : netPlan.getNodeIds()) {
                for (long nodeId_2 : netPlan.getNodeIds()) {
                    if (nodeId_1 >= nodeId_2) continue;
                    Node n1 = netPlan.getNodeFromId(nodeId_1);
                    Node n2 = netPlan.getNodeFromId(nodeId_2);

                    Pair<Link, Link> out = netPlan.addLinkBidirectional(n1, n2, 0, euclidean ? netPlan.getNodePairEuclideanDistance(n1, n2) : netPlan.getNodePairHaversineDistanceInKm(n1, n2), 200000, null);
                    networkViewer.getTopologyPanel().getCanvas().addLink(out.getFirst());
                    networkViewer.getTopologyPanel().getCanvas().addLink(out.getSecond());
                }
            }

            networkViewer.getTopologyPanel().getCanvas().refresh();
            networkViewer.updateNetPlanView();
        }
    }
}
