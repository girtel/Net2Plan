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
import com.net2plan.gui.utils.CellRenderers.UnfocusableCellRenderer;
import com.net2plan.gui.utils.topology.INetworkCallback;
import com.net2plan.gui.utils.topology.TopologyPanel;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.SRGUtils.SharedRiskModel;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 */
public class AdvancedJTable_srg extends AdvancedJTableNetworkElement {
    private static final String netPlanViewTabName = "Shared-risk groups";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "MTTF (hours)", "MTTR (hours)", "Availability",
            "Nodes", "Links", "Links (other layers)", "# Affected routes", "# Affected protection segments", "# Affected multicast trees", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)", "Index (consecutive integer starting in zero)", "Mean time to fail", "Mean time to repair", "Expected availability", "Nodes included into the shared-risk group", "Links (in this layer) included into the shared-risk group", "Links (in other layers) included into the shared-risk group", "# Affected routes", "# Affected protection segments", "# Affected multicast trees", "Attributes");
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_INDEX = 1;
    private static final int COLUMN_MTTF = 2;
    private static final int COLUMN_MTTR = 3;
    private static final int COLUMN_AVAILABILITY = 4;
    private static final int COLUMN_NODES = 5;
    private static final int COLUMN_LINKS = 6;
    private static final int COLUMN_LINKSOTHERLAYERS = 7;
    private static final int COLUMN_AFFECTEDROUTES = 8;
    private static final int COLUMN_AFFECTEDSEGMENTS = 9;
    private static final int COLUMN_AFFECTEDTREES = 10;
    private static final int COLUMN_ATTRIBUTES = 11;
    private static int MAXNUMDECIMALSINAVAILABILITY = 7;

    public AdvancedJTable_srg(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        super(createTableModel(networkViewer, topologyPanel), networkViewer, NetworkElementType.SRG);
        setDefaultCellRenderers(networkViewer);
        setSpecificCellRenderers();
        setColumnRowSorting(networkViewer.inOnlineSimulationMode());

    }

    public List<Object[]> getAllData(NetPlan currentState, TopologyPanel topologyPanel, NetPlan initialState) {
        NetworkLayer layer = currentState.getNetworkLayerDefault();
        List<Object[]> allSRGData = new LinkedList<Object[]>();
        for (SharedRiskGroup srg : currentState.getSRGs()) {
            Set<Route> routeIds_thisSRG = currentState.getRoutingType() == RoutingType.SOURCE_ROUTING ? srg.getAffectedRoutes(layer) : new LinkedHashSet<Route>();
            Set<ProtectionSegment> segmentIds_thisSRG = currentState.getRoutingType() == RoutingType.SOURCE_ROUTING ? srg.getAffectedProtectionSegments(layer) : new LinkedHashSet<ProtectionSegment>();
            Set<MulticastTree> treeIds_thisSRG = currentState.getRoutingType() == RoutingType.SOURCE_ROUTING ? srg.getAffectedMulticastTrees(layer) : new LinkedHashSet<MulticastTree>();
            int numRoutes = routeIds_thisSRG.size();
            int numSegments = segmentIds_thisSRG.size();
            int numMulticastTrees = treeIds_thisSRG.size();
            Set<Node> nodeIds_thisSRG = srg.getNodes();
            Set<Link> linkIds_thisSRG = srg.getLinks(layer);

            Object[] srgData = new Object[netPlanViewTableHeader.length];
            srgData[0] = srg.getId();
            srgData[1] = srg.getIndex();
            srgData[2] = srg.getMeanTimeToFailInHours();
            srgData[3] = srg.getMeanTimeToRepairInHours();
            srgData[4] = srg.getAvailability();
            srgData[5] = nodeIds_thisSRG.isEmpty() ? "none" : CollectionUtils.join(NetPlan.getIndexes(nodeIds_thisSRG), ", ");
            srgData[6] = linkIds_thisSRG.isEmpty() ? "none" : CollectionUtils.join(NetPlan.getIndexes(srg.getLinks()), ", ");
            srgData[7] = srg.getLinks(layer).isEmpty() ? "none" : CollectionUtils.join(NetPlan.getIndexes(srg.getLinks(layer)), ", ");
            srgData[8] = numRoutes == 0 ? "none" : numRoutes + " (" + CollectionUtils.join(NetPlan.getIndexes(routeIds_thisSRG), ", ") + ")";
            srgData[9] = numSegments == 0 ? "none" : numSegments + " (" + CollectionUtils.join(NetPlan.getIndexes(segmentIds_thisSRG), ", ") + ")";
            srgData[10] = numMulticastTrees == 0 ? "none" : numMulticastTrees + " (" + CollectionUtils.join(NetPlan.getIndexes(treeIds_thisSRG), ", ") + ")";
            srgData[11] = StringUtils.mapToString(srg.getAttributes());
            allSRGData.add(srgData);

            if (initialState != null && initialState.getSRGFromId(srg.getId()) != null) {
                layer = initialState.getNetworkLayerDefault();
                srg = initialState.getSRGFromId(srg.getId());
                routeIds_thisSRG = initialState.getRoutingType() == RoutingType.SOURCE_ROUTING ? srg.getAffectedRoutes(layer) : new LinkedHashSet<Route>();
                segmentIds_thisSRG = initialState.getRoutingType() == RoutingType.SOURCE_ROUTING ? srg.getAffectedProtectionSegments(layer) : new LinkedHashSet<ProtectionSegment>();
                treeIds_thisSRG = initialState.getRoutingType() == RoutingType.SOURCE_ROUTING ? srg.getAffectedMulticastTrees(layer) : new LinkedHashSet<MulticastTree>();
                numRoutes = routeIds_thisSRG.size();
                numSegments = segmentIds_thisSRG.size();
                numMulticastTrees = treeIds_thisSRG.size();
                nodeIds_thisSRG = srg.getNodes();
                linkIds_thisSRG = srg.getLinks(layer);


                Object[] srgData_initialNetPlan = new Object[netPlanViewTableHeader.length];
                srgData_initialNetPlan[0] = null;
                srgData_initialNetPlan[1] = null;
                srgData_initialNetPlan[2] = srg.getMeanTimeToFailInHours();
                srgData_initialNetPlan[3] = srg.getMeanTimeToRepairInHours();
                srgData_initialNetPlan[4] = srg.getAvailability();
                srgData_initialNetPlan[5] = nodeIds_thisSRG.isEmpty() ? "none" : CollectionUtils.join(NetPlan.getIndexes(nodeIds_thisSRG), ", ");
                srgData_initialNetPlan[6] = linkIds_thisSRG.isEmpty() ? "none" : CollectionUtils.join(NetPlan.getIndexes(srg.getLinks()), ", ");
                srgData_initialNetPlan[7] = srg.getLinks(layer).isEmpty() ? "none" : CollectionUtils.join(NetPlan.getIndexes(srg.getLinks(layer)), ", ");
                srgData_initialNetPlan[8] = numRoutes == 0 ? "none" : numRoutes + " (" + CollectionUtils.join(NetPlan.getIndexes(routeIds_thisSRG), ", ") + ")";
                srgData_initialNetPlan[9] = numSegments == 0 ? "none" : numSegments + " (" + CollectionUtils.join(NetPlan.getIndexes(segmentIds_thisSRG), ", ") + ")";
                srgData_initialNetPlan[10] = numMulticastTrees == 0 ? "none" : numMulticastTrees + " (" + CollectionUtils.join(NetPlan.getIndexes(treeIds_thisSRG), ", ") + ")";
                srgData_initialNetPlan[11] = StringUtils.mapToString(srg.getAttributes());
                allSRGData.add(srgData_initialNetPlan);
            }
        }

        return allSRGData;
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
        return np.hasSRGs();
    }

    public int[] getColumnsOfSpecialComparatorForSorting() {
        return new int[]{8, 9};
    }

    private static TableModel createTableModel(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        TableModel srgTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (getValueAt(rowIndex, columnIndex) == null) return false;
                if (!networkViewer.isEditable()) return false;

                return columnIndex == COLUMN_MTTF || columnIndex == COLUMN_MTTR;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column) {
                Object oldValue = getValueAt(row, column);

                if (newValue.equals(oldValue)) return;

                NetPlan netPlan = networkViewer.getDesign();
                NetPlan aux_netPlan = netPlan.copy();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long srgId = (Long) getValueAt(row, 0);
                final SharedRiskGroup srg = netPlan.getSRGFromId(srgId);

				/* Perform checks, if needed */
                try {
                    switch (column) {
                        case COLUMN_MTTF:
                            srg.setMeanTimeToFailInHours(Double.parseDouble(newValue.toString()));
                            super.setValueAt(srg.getAvailability(), row, COLUMN_AVAILABILITY);
                            break;

                        case COLUMN_MTTR:
                            srg.setMeanTimeToRepairInHours(Double.parseDouble(newValue.toString()));
                            super.setValueAt(srg.getAvailability(), row, COLUMN_AVAILABILITY);
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex) {
                    networkViewer.getDesign().assignFrom(aux_netPlan);
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying SRG");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };
        return srgTableModel;
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
        getColumnModel().getColumn(convertColumnIndexToView(COLUMN_AVAILABILITY)).setCellRenderer(new CellRenderers.NumberCellRenderer(MAXNUMDECIMALSINAVAILABILITY));
    }

    public void setColumnRowSorting(boolean allowShowInitialNetPlan) {
        if (allowShowInitialNetPlan) setRowSorter(new CurrentAndPlannedStateTableSorter(getModel()));
        else setAutoCreateRowSorter(true);
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_AFFECTEDROUTES, new IGUINetworkViewer.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_AFFECTEDSEGMENTS, new IGUINetworkViewer.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_AFFECTEDTREES, new IGUINetworkViewer.ColumnComparator());
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
                                netPlan.getSRGFromId((long) itemId).remove();
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
                            netPlan.removeAllSRGs();
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
                networkViewer.showSRG((long) itemId);
                break;

            default:
        }
    }

    private boolean isTableEmpty() {
        return !networkViewer.getDesign().hasSRGs();
    }

    private JMenuItem getAddOption() {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);

        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = networkViewer.getDesign();

                try {
                    netPlan.addSRG(8748, 12, null);
                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });
        return addItem;
    }

    private List<JComponent> getExtraAddOptions() {
        List<JComponent> options = new LinkedList<JComponent>();
        NetPlan netPlan = networkViewer.getDesign();
        JMenu submenuSRGs = new JMenu("Add SRGs from model");
        options.add(submenuSRGs);

        final JMenuItem onePerNode = new JMenuItem("One SRG per node");
        submenuSRGs.add(onePerNode);
        final JMenuItem onePerLink = new JMenuItem("One SRG per unidirectional link");
        submenuSRGs.add(onePerLink);
        final JMenuItem onePerLinkBundle = new JMenuItem("One SRG per unidirectional bundle of links");
        submenuSRGs.add(onePerLinkBundle);
        final JMenuItem onePerBidiLinkBundle = new JMenuItem("One SRG per bidirectional bundle of links");
        submenuSRGs.add(onePerBidiLinkBundle);

        if (!netPlan.hasNodes()) {
            onePerNode.setEnabled(false);
        }

        if (!netPlan.hasLinks()) {
            onePerLink.setEnabled(false);
            onePerLinkBundle.setEnabled(false);
            onePerBidiLinkBundle.setEnabled(false);
        }

        ActionListener srgModel = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double mttf;
                double mttr;

                JTextField txt_mttf = new JTextField(5);
                JTextField txt_mttr = new JTextField(5);
                JCheckBox chk_removeExistingSRGs = new JCheckBox();
                chk_removeExistingSRGs.setSelected(true);

                JPanel pane = new JPanel(new GridLayout(0, 2));
                pane.add(new JLabel("MTTF (in hours, zero or negative value means no failure): "));
                pane.add(txt_mttf);
                pane.add(new JLabel("MTTR (in hours): "));
                pane.add(txt_mttr);
                pane.add(new JLabel("Remove existing SRGs: "));
                pane.add(chk_removeExistingSRGs);

                while (true) {
                    int result = JOptionPane.showConfirmDialog(null, pane, "Add SRGs from model", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return;

                    try {
                        mttf = Double.parseDouble(txt_mttf.getText());
                        mttr = Double.parseDouble(txt_mttr.getText());
                        if (mttr <= 0) throw new RuntimeException();

                        break;
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog("MTTF/MTTR must be greater than zero", "Error adding SRGs from model");
                    }
                }

                boolean removeExistingSRGs = chk_removeExistingSRGs.isSelected();

                NetPlan netPlan = networkViewer.getDesign();

                try {
                    SharedRiskModel srgModel = null;
                    if (e.getSource() == onePerNode) srgModel = SRGUtils.SharedRiskModel.PER_NODE;
                    else if (e.getSource() == onePerLink) srgModel = SRGUtils.SharedRiskModel.PER_LINK;
                    else if (e.getSource() == onePerLinkBundle)
                        srgModel = SRGUtils.SharedRiskModel.PER_DIRECTIONAL_LINK_BUNDLE;
                    else if (e.getSource() == onePerBidiLinkBundle)
                        srgModel = SRGUtils.SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE;

                    if (srgModel == null) throw new RuntimeException("Bad");
                    SRGUtils.configureSRGs(netPlan, mttf, mttr, srgModel, removeExistingSRGs);

                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add SRGs from model");
                }

            }
        };

        onePerNode.addActionListener(srgModel);
        onePerLink.addActionListener(srgModel);
        onePerLinkBundle.addActionListener(srgModel);
        onePerBidiLinkBundle.addActionListener(srgModel);


        return options;
    }

    private List<JComponent> getExtraOptions(final int row, final Object itemId) {
        List<JComponent> options = new LinkedList<JComponent>();

        final int numRows = model.getRowCount();
        final NetPlan netPlan = networkViewer.getDesign();

        if (itemId != null) {
            JMenuItem editSRG = new JMenuItem("View/edit SRG");
            editSRG.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        viewEditSRGGUI(networkViewer, networkViewer.getTopologyPanel(), (long) itemId);
                        networkViewer.updateNetPlanView();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error viewing/editing SRG");
                    }
                }
            });

            options.add(editSRG);
        }

        if (numRows > 1) {
            if (!options.isEmpty()) options.add(new JPopupMenu.Separator());

            JMenuItem mttfValue = new JMenuItem("Set MTTF to all");
            mttfValue.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double mttf;

                    while (true) {
                        String str = JOptionPane.showInputDialog(null, "MTTF (in hours, zero or negative value means no failure)", "Set MTTF to all SRGs", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try {
                            mttf = Double.parseDouble(str);
                            break;
                        } catch (NumberFormatException ex) {
                            ErrorHandling.showErrorDialog("Non-valid MTTF value", "Error setting MTTF value");
                        } catch (Throwable ex) {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error setting MTTF");
                        }
                    }

                    NetPlan netPlan = networkViewer.getDesign();

                    try {
                        for (SharedRiskGroup srg : netPlan.getSRGs()) srg.setMeanTimeToFailInHours(mttf);

                        networkViewer.updateNetPlanView();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set MTTF to all SRGs");
                    }
                }
            });

            options.add(mttfValue);

            JMenuItem mttrValue = new JMenuItem("Set MTTR to all");
            mttrValue.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double mttr;

                    while (true) {
                        String str = JOptionPane.showInputDialog(null, "MTTR (in hours)", "Set MTTR to all SRGs", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try {
                            mttr = Double.parseDouble(str);
                            if (mttr <= 0) throw new NumberFormatException();

                            break;
                        } catch (NumberFormatException ex) {
                            ErrorHandling.showErrorDialog("Non-valid MTTR value. Please, introduce a non-zero non-negative number", "Error setting MTTR value");
                        } catch (Throwable ex) {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error setting MTTR");
                        }
                    }

                    NetPlan netPlan = networkViewer.getDesign();

                    try {
                        for (SharedRiskGroup srg : netPlan.getSRGs()) srg.setMeanTimeToRepairInHours(mttr);

                        networkViewer.updateNetPlanView();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set MTTR to all SRGs");
                    }
                }
            });

            options.add(mttrValue);
        }


        return options;
    }


    private static void viewEditSRGGUI(final INetworkCallback callback, final TopologyPanel topologyPanel, final long srgId) {
        final NetPlan netPlan = callback.getDesign();
        final Collection<Long> nodeIds = netPlan.getNodeIds();
        final Collection<Long> linkIds = netPlan.getLinkIds();
        final Collection<Node> nodeIds_thisSRG = netPlan.getSRGFromId(srgId).getNodes();
        final Collection<Link> linkIds_thisSRG = netPlan.getSRGFromId(srgId).getLinks();

        Map<Node, Color> coloredNodes = new HashMap<Node, Color>();
        for (Node n : nodeIds_thisSRG) coloredNodes.put(n, Color.ORANGE);
        Map<Link, Pair<Color, Boolean>> coloredLinks = new HashMap<Link, Pair<Color, Boolean>>();
        for (Link e : linkIds_thisSRG) coloredLinks.put(e, Pair.of(Color.ORANGE, false));
        topologyPanel.getCanvas().showAndPickNodesAndLinks(coloredNodes, coloredLinks);

        final int N = nodeIds.size();
        final int E = linkIds.size();
        final Object[][] nodeData = new Object[N == 0 ? 1 : N][3];
        final Object[][] linkData = new Object[E == 0 ? 1 : E][4];

        if (N > 0) {
            int n = 0;
            Iterator<Long> nodeIt = nodeIds.iterator();
            while (nodeIt.hasNext()) {
                long nodeId = nodeIt.next();
                nodeData[n] = new Object[3];
                nodeData[n][0] = nodeId;
                nodeData[n][1] = netPlan.getNodeFromId(nodeId).getName();
                nodeData[n][2] = nodeIds_thisSRG.contains(nodeId);

                n++;
            }
        }

        if (E > 0) {
            int e = 0;
            Iterator<Long> linkIt = linkIds.iterator();
            while (linkIt.hasNext()) {
                long linkId = linkIt.next();
                Link link = netPlan.getLinkFromId(linkId);
                long originNodeId = link.getOriginNode().getId();
                long destinationNodeId = link.getDestinationNode().getId();
                String originNodeName = link.getOriginNode().getName();
                String destinationNodeName = link.getDestinationNode().getName();

                linkData[e] = new Object[4];
                linkData[e][0] = linkId;
                linkData[e][1] = originNodeId + (originNodeName.isEmpty() ? "" : " (" + originNodeName + ")");
                linkData[e][2] = destinationNodeId + (destinationNodeName.isEmpty() ? "" : " (" + destinationNodeName + ")");
                linkData[e][3] = linkIds_thisSRG.contains(linkId);

                e++;
            }
        }

        final DefaultTableModel nodeModel = new ClassAwareTableModel(nodeData, new String[]{"Id", "Name", "Included in the SRG"}) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 2;
            }

            @Override
            public void setValueAt(Object aValue, int row, int column) {
                if (column == 2) {
                    boolean value = (boolean) aValue;
                    long nodeId = (long) getValueAt(row, 0);
                    if (value && !nodeIds_thisSRG.contains(nodeId)) {
                        netPlan.getSRGFromId(srgId).addNode(netPlan.getNodeFromId(nodeId));
                        Map<Node, Color> coloredNodes = new HashMap<Node, Color>();
                        for (Node n : nodeIds_thisSRG) coloredNodes.put(n, Color.ORANGE);
                        Map<Link, Pair<Color, Boolean>> coloredLinks = new HashMap<Link, Pair<Color, Boolean>>();
                        for (Link e : linkIds_thisSRG) coloredLinks.put(e, Pair.of(Color.ORANGE, false));
                        topologyPanel.getCanvas().showAndPickNodesAndLinks(coloredNodes, coloredLinks);
                    } else if (!value && nodeIds_thisSRG.contains(nodeId)) {
                        netPlan.getSRGFromId(srgId).removeNode(netPlan.getNodeFromId(nodeId));
                        Map<Node, Color> coloredNodes = new HashMap<Node, Color>();
                        for (Node n : nodeIds_thisSRG) coloredNodes.put(n, Color.ORANGE);
                        Map<Link, Pair<Color, Boolean>> coloredLinks = new HashMap<Link, Pair<Color, Boolean>>();
                        for (Link e : linkIds_thisSRG) coloredLinks.put(e, Pair.of(Color.ORANGE, false));
                        topologyPanel.getCanvas().showAndPickNodesAndLinks(coloredNodes, coloredLinks);
                    }
                }

                super.setValueAt(aValue, row, column);
            }
        };

        final DefaultTableModel linkModel = new ClassAwareTableModel(linkData, new String[]{"Id", "Origin node", "Destination node", "Included in the SRG"}) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 3;
            }

            @Override
            public void setValueAt(Object aValue, int row, int column) {
                if (column == 3) {
                    boolean value = (boolean) aValue;
                    long linkId = (long) getValueAt(row, 0);
                    if (value && !linkIds_thisSRG.contains(linkId)) {
                        netPlan.getSRGFromId(srgId).addLink(netPlan.getLinkFromId(linkId));
                        Map<Node, Color> coloredNodes = new HashMap<Node, Color>();
                        for (Node n : nodeIds_thisSRG) coloredNodes.put(n, Color.ORANGE);
                        Map<Link, Pair<Color, Boolean>> coloredLinks = new HashMap<Link, Pair<Color, Boolean>>();
                        for (Link e : linkIds_thisSRG) coloredLinks.put(e, Pair.of(Color.ORANGE, false));
                        topologyPanel.getCanvas().showAndPickNodesAndLinks(coloredNodes, coloredLinks);
                    } else if (!value && linkIds_thisSRG.contains(linkId)) {
                        netPlan.getSRGFromId(srgId).removeLink(netPlan.getLinkFromId(linkId));
                        Map<Node, Color> coloredNodes = new HashMap<Node, Color>();
                        for (Node n : nodeIds_thisSRG) coloredNodes.put(n, Color.ORANGE);
                        Map<Link, Pair<Color, Boolean>> coloredLinks = new HashMap<Link, Pair<Color, Boolean>>();
                        for (Link e : linkIds_thisSRG) coloredLinks.put(e, Pair.of(Color.ORANGE, false));
                        topologyPanel.getCanvas().showAndPickNodesAndLinks(coloredNodes, coloredLinks);
                    }
                }

                super.setValueAt(aValue, row, column);
            }
        };

        final JTable nodeTable = new AdvancedJTable(nodeModel);
        final JTable linkTable = new AdvancedJTable(linkModel);

        nodeTable.setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        nodeTable.setDefaultRenderer(Double.class, new UnfocusableCellRenderer());
        nodeTable.setDefaultRenderer(Object.class, new UnfocusableCellRenderer());
        nodeTable.setDefaultRenderer(Float.class, new UnfocusableCellRenderer());
        nodeTable.setDefaultRenderer(Long.class, new UnfocusableCellRenderer());
        nodeTable.setDefaultRenderer(Integer.class, new UnfocusableCellRenderer());
        nodeTable.setDefaultRenderer(String.class, new UnfocusableCellRenderer());

        linkTable.setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        linkTable.setDefaultRenderer(Double.class, new UnfocusableCellRenderer());
        linkTable.setDefaultRenderer(Object.class, new UnfocusableCellRenderer());
        linkTable.setDefaultRenderer(Float.class, new UnfocusableCellRenderer());
        linkTable.setDefaultRenderer(Long.class, new UnfocusableCellRenderer());
        linkTable.setDefaultRenderer(Integer.class, new UnfocusableCellRenderer());
        linkTable.setDefaultRenderer(String.class, new UnfocusableCellRenderer());

        JScrollPane nodeScrollPane = new JScrollPane(nodeTable);
        JScrollPane linkScrollPane = new JScrollPane(linkTable);

        FixedColumnDecorator nodeDecorator = new FixedColumnDecorator(nodeScrollPane, 1);
        FixedColumnDecorator linkDecorator = new FixedColumnDecorator(linkScrollPane, 1);
        nodeDecorator.getFixedTable().getColumnModel().getColumn(0).setMinWidth(50);
        linkDecorator.getFixedTable().getColumnModel().getColumn(0).setMinWidth(50);

        final JDialog dialog = new JDialog();
        dialog.setLayout(new MigLayout("", "[grow]", "[][grow][][grow]"));
        dialog.add(new JLabel("Nodes"), "growx, wrap");
        dialog.add(nodeScrollPane, "grow, wrap");
        dialog.add(new JLabel("Links"), "growx, wrap");
        dialog.add(linkScrollPane, "grow");

        dialog.setTitle("View/edit SRG " + srgId);
        SwingUtils.configureCloseDialogOnEscape(dialog);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(new Dimension(500, 300));
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }

    private List<JComponent> getForcedOptions() {
        return new LinkedList<JComponent>();
    }
}
