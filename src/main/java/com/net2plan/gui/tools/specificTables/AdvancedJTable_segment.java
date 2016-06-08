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
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 */
public class AdvancedJTable_segment extends AdvancedJTableNetworkElement {
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_INDEX = 1;
    private static final int COLUMN_ORIGINNODE = 2;
    private static final int COLUMN_DESTNODE = 3;
    private static final int COLUMN_RESERVEDCAPACITY = 4;
    private static final int COLUMN_CARRIEDTRAFFIC = 5;
    private static final int COLUMN_SEQLINKS = 6;
    private static final int COLUMN_SEQNODES = 7;
    private static final int COLUMN_NUMHOPS = 8;
    private static final int COLUMN_LENGTH = 9;
    private static final int COLUMN_PROPDELAY = 10;
    private static final int COLUMN_DEDICATEDORSHARED = 11;
    private static final int COLUMN_NUMROUTES = 12;
    private static final int COLUMN_ATTRIBUTES = 13;
    private static final String netPlanViewTabName = "Protection segments";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "Origin node", "Destination node", "Reserved capacity",
            "Carried traffic", "Sequence of links", "Sequence of nodes", "Number of hops", "Length (km)", "Propagation delay (ms)", "Dedicated/Shared",
            "# Routes", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)", "Index (consecutive integer starting in zero)", "Origin node", "Destination node", "Reserved capacity for the segment", "Carried traffic by this segment", "Sequence of links", "Sequence of nodes", "Number of hops", "Length (km)", "Propagation delay (ms)", "Dedicated/Shared", "# Routes", "Attributes");

    public AdvancedJTable_segment(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        super(createTableModel(networkViewer, topologyPanel), networkViewer, NetworkElementType.PROTECTION_SEGMENT);
        setDefaultCellRenderers(networkViewer);
        setSpecificCellRenderers();
        setColumnRowSorting(networkViewer.inOnlineSimulationMode());

    }

    public List<Object[]> getAllData(NetPlan currentState, TopologyPanel topologyPanel, NetPlan initialState) {
        final boolean sameRoutingType = initialState != null && initialState.getRoutingType() == currentState.getRoutingType();
        List<Object[]> allSegmentData = new LinkedList<Object[]>();
        for (ProtectionSegment segment : currentState.getProtectionSegments()) {
            List<Link> seqLinks = segment.getSeqLinks();
            List<Node> seqNodes = segment.getSeqNodes();
            Set<Route> routeIds_thisSegment = segment.getAssociatedRoutesToWhichIsBackup();
            int numRoutes = routeIds_thisSegment.size();

            Node originNode = segment.getOriginNode();
            Node destinationNode = segment.getDestinationNode();
            String originNodeName = originNode.getName();
            String destinationNodeName = destinationNode.getName();

            Object[] segmentData = new Object[netPlanViewTableHeader.length];
            segmentData[0] = segment.getId();
            segmentData[1] = segment.getIndex();
            segmentData[2] = originNode.getIndex() + (originNodeName.isEmpty() ? "" : " (" + originNodeName + ")");
            segmentData[3] = destinationNode.getIndex() + (destinationNodeName.isEmpty() ? "" : " (" + destinationNodeName + ")");
            segmentData[4] = segment.getReservedCapacityForProtection();
            segmentData[5] = segment.getCarriedTraffic();
            segmentData[6] = CollectionUtils.join(NetPlan.getIndexes(seqLinks), " => ");
            segmentData[7] = CollectionUtils.join(NetPlan.getIndexes(seqNodes), " => ");
            segmentData[8] = segment.getNumberOfHops();
            segmentData[9] = segment.getLengthInKm();
            segmentData[10] = segment.getPropagationDelayInMs();
            segmentData[11] = numRoutes > 1 ? "Shared" : (numRoutes == 0 ? "Not used" : "Dedicated");
            segmentData[12] = numRoutes == 0 ? "none" : numRoutes + " (" + CollectionUtils.join(routeIds_thisSegment, ", ") + ")";
            segmentData[13] = StringUtils.mapToString(segment.getAttributes());
            allSegmentData.add(segmentData);

            if (initialState != null && sameRoutingType && initialState.getProtectionSegmentFromId(segment.getId()) != null) {
                segment = initialState.getProtectionSegmentFromId(segment.getId());
                seqLinks = segment.getSeqLinks();
                seqNodes = segment.getSeqNodes();
                routeIds_thisSegment = segment.getAssociatedRoutesToWhichIsBackup();
                numRoutes = routeIds_thisSegment.size();

                originNode = segment.getOriginNode();
                destinationNode = segment.getDestinationNode();
                originNodeName = originNode.getName();
                destinationNodeName = destinationNode.getName();

                Object[] segmentData_initialNetPlan = new Object[netPlanViewTableHeader.length];
                segmentData_initialNetPlan[0] = null;
                segmentData_initialNetPlan[1] = null;
                segmentData_initialNetPlan[2] = null;
                segmentData_initialNetPlan[3] = null;
                segmentData_initialNetPlan[4] = segment.getReservedCapacityForProtection();
                segmentData_initialNetPlan[5] = segment.getCarriedTraffic();
                segmentData_initialNetPlan[6] = CollectionUtils.join(NetPlan.getIndexes(seqLinks), " => ");
                segmentData_initialNetPlan[7] = CollectionUtils.join(NetPlan.getIndexes(seqNodes), " => ");
                segmentData_initialNetPlan[8] = segment.getNumberOfHops();
                segmentData_initialNetPlan[9] = segment.getLengthInKm();
                segmentData_initialNetPlan[10] = 1000 * segment.getPropagationDelayInMs();
                segmentData_initialNetPlan[11] = numRoutes > 1 ? "Shared" : (numRoutes == 0 ? "Not used" : "Dedicated");
                segmentData_initialNetPlan[12] = numRoutes == 0 ? "none" : numRoutes + " (" + CollectionUtils.join(routeIds_thisSegment, ", ") + ")";
                segmentData_initialNetPlan[13] = StringUtils.mapToString(segment.getAttributes());
                allSegmentData.add(segmentData_initialNetPlan);
            }

        }


        return allSegmentData;
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
        return np.hasProtectionSegments();
    }

    public int[] getColumnsOfSpecialComparatorForSorting() {
        return new int[]{12};
    }

    private static TableModel createTableModel(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        TableModel segmentTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (getValueAt(rowIndex, columnIndex) == null) return false;
                if (!networkViewer.isEditable()) return false;

                return columnIndex == COLUMN_RESERVEDCAPACITY;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column) {
                Object oldValue = getValueAt(row, column);

                if (newValue.equals(oldValue)) return;

                NetPlan netPlan = networkViewer.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long segmentId = (Long) getValueAt(row, 0);
                final ProtectionSegment segment = netPlan.getProtectionSegmentFromId(segmentId);
                /* Perform checks, if needed */
                try {
                    switch (column) {
                        case COLUMN_RESERVEDCAPACITY:
                            segment.setReservedCapacity(Double.parseDouble(newValue.toString()));
                            networkViewer.updateNetPlanView();
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying protection segment");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };
        return segmentTableModel;
    }

    private void setDefaultCellRenderers(final IGUINetworkViewer networkViewer) {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());

        setDefaultRenderer(Boolean.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Boolean.class), networkViewer, NetworkElementType.PROTECTION_SEGMENT));
        setDefaultRenderer(Double.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Double.class), networkViewer, NetworkElementType.PROTECTION_SEGMENT));
        setDefaultRenderer(Object.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Object.class), networkViewer, NetworkElementType.PROTECTION_SEGMENT));
        setDefaultRenderer(Float.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Float.class), networkViewer, NetworkElementType.PROTECTION_SEGMENT));
        setDefaultRenderer(Long.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Long.class), networkViewer, NetworkElementType.PROTECTION_SEGMENT));
        setDefaultRenderer(Integer.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Integer.class), networkViewer, NetworkElementType.PROTECTION_SEGMENT));
        setDefaultRenderer(String.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(String.class), networkViewer, NetworkElementType.PROTECTION_SEGMENT));
    }

    private void setSpecificCellRenderers() {
    }

    public void setColumnRowSorting(boolean allowShowInitialNetPlan) {
        if (allowShowInitialNetPlan) setRowSorter(new CurrentAndPlannedStateTableSorter(getModel()));
        else setAutoCreateRowSorter(true);
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_NUMROUTES, new IGUINetworkViewer.ColumnComparator());
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
                                netPlan.getProtectionSegmentFromId((long) itemId).remove();
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
                            netPlan.removeAllProtectionSegments();
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
                networkViewer.showProtectionSegment((long) itemId);
                break;
            default:
                break;
        }
    }

    private boolean isTableEmpty() {
        return !networkViewer.getDesign().hasProtectionSegments();
    }

    private JMenuItem getAddOption() {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = networkViewer.getDesign();

                try {
                    createProtectionSegmentGUI(networkViewer, networkViewer.getTopologyPanel());
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

    private static void createProtectionSegmentGUI(final INetworkCallback callback, final TopologyPanel topologyPanel) {
        final NetPlan netPlan = callback.getDesign();
        final Collection<Long> nodeIds = NetPlan.getIds(netPlan.getNodes());
        final JComboBox nodeSelector = new WiderJComboBox();

        final List<JComboBox> seqLinks_cmb = new LinkedList<JComboBox>();
        final JPanel seqLinks_pnl = new JPanel();
        seqLinks_pnl.setLayout(new BoxLayout(seqLinks_pnl, BoxLayout.Y_AXIS));

        nodeSelector.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                StringLabeller item = (StringLabeller) e.getItem();

                long nodeId = (Long) item.getObject();
                Node node = netPlan.getNodeFromId(nodeId);
                String nodeName = node.getName();

                seqLinks_cmb.clear();
                seqLinks_pnl.removeAll();

                Set<Link> outgoingLinks = node.getOutgoingLinks();
                final JComboBox firstLink = new WiderJComboBox();
                for (Link link : outgoingLinks) {
                    firstLink.addItem(StringLabeller.of(link.getId(), String.format("e%d: n%d (%s) => n%d (%s)", link.getId(), nodeId, nodeName, link.getDestinationNode().getId(), link.getDestinationNode().getName())));
                }

                firstLink.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        JComboBox me = (JComboBox) e.getSource();
                        Iterator<JComboBox> it = seqLinks_cmb.iterator();
                        while (it.hasNext()) {
                            if (it.next() == me) break;
                        }
                        while (it.hasNext()) {
                            JComboBox aux = it.next();
                            seqLinks_pnl.remove(aux);
                            it.remove();
                        }

                        seqLinks_pnl.revalidate();

                        List<Link> seqLinks = new LinkedList<Link>();
                        for (JComboBox link : seqLinks_cmb)
                            seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));
                        Map<Link, Pair<Color, Boolean>> res = new HashMap<Link, Pair<Color, Boolean>>();
                        for (Link link : seqLinks) res.put(link, Pair.of(Color.ORANGE, false));

                        topologyPanel.getCanvas().showAndPickNodesAndLinks(null, res);
                    }
                });

                setMaxSize(firstLink);

                seqLinks_cmb.add(firstLink);
                seqLinks_pnl.add(firstLink);

                JPanel pane = new JPanel(new FlowLayout());
                JButton addLink_btn = new JButton("Add new link");
                addLink_btn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        long linkId = (Long) ((StringLabeller) seqLinks_cmb.get(seqLinks_cmb.size() - 1).getSelectedItem()).getObject();
                        Node destinationNode = netPlan.getLinkFromId(linkId).getDestinationNode();
                        String destinationNodeName = destinationNode.getName();

                        Set<Link> outgoingLinks = destinationNode.getOutgoingLinks();
                        if (outgoingLinks.isEmpty()) {
                            ErrorHandling.showErrorDialog("Last node has no outgoing links", "Error");
                            return;
                        }

                        final JComboBox newLink = new WiderJComboBox();
                        for (Link nextLink : outgoingLinks)
                            newLink.addItem(StringLabeller.of(nextLink.getId(), String.format("e%d: n%d (%s) => n%d (%s)", nextLink.getId(), destinationNode.getId(), destinationNodeName, nextLink.getDestinationNode().getId(), nextLink.getDestinationNode().getName())));

                        newLink.addItemListener(new ItemListener() {
                            @Override
                            public void itemStateChanged(ItemEvent e) {
                                JComboBox me = (JComboBox) e.getSource();
                                Iterator<JComboBox> it = seqLinks_cmb.iterator();
                                while (it.hasNext()) if (it.next() == me) break;

                                while (it.hasNext()) {
                                    JComboBox aux = it.next();
                                    seqLinks_pnl.remove(aux);
                                    it.remove();
                                }

                                seqLinks_pnl.revalidate();

                                List<Link> seqLinks = new LinkedList<Link>();
                                for (JComboBox link : seqLinks_cmb)
                                    seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));

                                Map<Link, Pair<Color, Boolean>> res = new HashMap<Link, Pair<Color, Boolean>>();
                                for (Link link : seqLinks) res.put(link, Pair.of(Color.BLUE, false));
                                topologyPanel.getCanvas().showAndPickNodesAndLinks(null, res);
                            }
                        });

                        setMaxSize(newLink);

                        seqLinks_cmb.add(newLink);
                        seqLinks_pnl.add(newLink, seqLinks_pnl.getComponentCount() - 1);
                        seqLinks_pnl.revalidate();

                        List<Link> seqLinks = new LinkedList<Link>();
                        for (JComboBox link : seqLinks_cmb)
                            seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));

                        Map<Link, Pair<Color, Boolean>> res = new HashMap<Link, Pair<Color, Boolean>>();
                        for (Link link : seqLinks) res.put(link, Pair.of(Color.BLUE, false));

                        topologyPanel.getCanvas().showAndPickNodesAndLinks(null, res);
                    }
                });

                pane.add(addLink_btn);

                JButton removeLink_btn = new JButton("Remove last link");
                removeLink_btn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (seqLinks_cmb.size() < 2) {
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

                        Map<Link, Pair<Color, Boolean>> res = new HashMap<Link, Pair<Color, Boolean>>();
                        for (Link link : seqLinks) res.put(link, Pair.of(Color.BLUE, false));

                        topologyPanel.getCanvas().showAndPickNodesAndLinks(null, res);
                    }
                });

                pane.add(removeLink_btn);
                seqLinks_pnl.add(pane);

                seqLinks_pnl.revalidate();

                List<Link> seqLinks = new LinkedList<Link>();
                for (JComboBox link : seqLinks_cmb)
                    seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));
                Map<Link, Pair<Color, Boolean>> res = new HashMap<Link, Pair<Color, Boolean>>();
                for (Link link : seqLinks) res.put(link, Pair.of(Color.BLUE, false));
                topologyPanel.getCanvas().showAndPickNodesAndLinks(null, res);
            }
        });

        for (long nodeId : nodeIds) {
            Node node = netPlan.getNodeFromId(nodeId);
            final String nodeName = node.getName();
            final Set<Link> outgoingLinks = node.getOutgoingLinks();
            if (outgoingLinks.isEmpty()) continue;

            String nodeLabel = "Node " + nodeId;
            if (!nodeName.isEmpty()) nodeLabel += " (" + nodeName + ")";

            nodeSelector.addItem(StringLabeller.of(nodeId, nodeLabel));
        }

        if (nodeSelector.getItemCount() == 0) throw new Net2PlanException("Bad - No node has outgoing links");

        nodeSelector.setSelectedIndex(0);

        final JTextField txt_reservedCapacity = new JTextField();

        final JScrollPane scrollPane = new JScrollPane(seqLinks_pnl, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Sequence of links"));
        scrollPane.setAlignmentY(JScrollPane.TOP_ALIGNMENT);

        final JPanel pane = new JPanel(new MigLayout("fill", "[][grow]", "[][grow][]"));
        pane.add(new JLabel("Origin node"));
        pane.add(nodeSelector, "grow, wrap, wmin 50");
        pane.add(scrollPane, "grow, spanx 2, wrap");
        pane.add(new JLabel("Reserved capacity"));
        pane.add(txt_reservedCapacity, "grow");
        pane.setPreferredSize(new Dimension(400, 400));

        while (true) {
            int result = JOptionPane.showConfirmDialog(null, pane, "Add new protection segment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) break;

            double reservedCapacity;

            try {
                reservedCapacity = Double.parseDouble(txt_reservedCapacity.getText());
                if (reservedCapacity < 0) throw new RuntimeException();
            } catch (Throwable e) {
                ErrorHandling.showErrorDialog("Reserved capacity must be a non-negative number", "Error adding protection segment");
                continue;
            }

            List<Link> seqLinks = new LinkedList<Link>();
            for (JComboBox link : seqLinks_cmb)
                seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));

            try {
                netPlan.addProtectionSegment(seqLinks, reservedCapacity, null);
            } catch (Throwable e) {
                ErrorHandling.showErrorDialog(e.getMessage(), "Error adding protection segment");
                continue;
            }

            break;
        }

        topologyPanel.getCanvas().resetPickedAndUserDefinedColorState();
    }

    private static void setMaxSize(JComponent c) {
        final Dimension max = c.getMaximumSize();
        final Dimension pref = c.getPreferredSize();

        max.height = pref.height;
        c.setMaximumSize(max);
    }

    private List<JComponent> getExtraAddOptions() {
        return new LinkedList<JComponent>();
    }

    private List<JComponent> getExtraOptions(final int row, final Object itemId) {
        return new LinkedList<JComponent>();
    }

    private List<JComponent> getForcedOptions() {
        return new LinkedList<JComponent>();
    }
}
