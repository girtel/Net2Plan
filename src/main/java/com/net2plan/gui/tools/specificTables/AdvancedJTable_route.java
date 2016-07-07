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
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 */
public class AdvancedJTable_route extends AdvancedJTableNetworkElement {
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_INDEX = 1;
    private static final int COLUMN_DEMAND = 2;
    private static final int COLUMN_INGRESSNODE = 3;
    private static final int COLUMN_EGRESSNODE = 4;
    private static final int COLUMN_DEMANDOFFEREDTRAFFIC = 5;
    private static final int COLUMN_CARRIEDTRAFFIC = 6;
    private static final int COLUMN_OCCUPIEDCAPACITY = 7;
    private static final int COLUMN_SEQUENCEOFLINKS = 8;
    private static final int COLUMN_SEQUENCEOFNODES = 9;
    private static final int COLUMN_NUMHOPS = 10;
    private static final int COLUMN_LENGTH = 11;
    private static final int COLUMN_PROPDELAY = 12;
    public static final int COLUMN_BOTTLENECKUTILIZATION = 13;
    private static final int COLUMN_BACKUPSEGMENTS = 14;
    private static final int COLUMN_ATTRIBUTES = 15;
    private static final String netPlanViewTabName = "Routes";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "Demand", "Ingress node", "Egress node",
            "Demand offered traffic", "Carried traffic", "Occupied capacity", "Sequence of links", "Sequence of nodes", "Number of hops", "Length (km)",
            "Propagation delay (ms)", "Bottleneck utilization", "Backup segments", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)", "Index (consecutive integer starting in zero)", "Demand", "Ingress node", "Egress node", "Demand offered traffic", "Carried traffic", "Occupied capacity", "Sequence of links", "Sequence of nodes", "Number of hops", "Total route length", "Propagation delay (ms)", "Highest utilization among all traversed links", "Candidate protection segments for this route", "Route-specific attributes");

    public AdvancedJTable_route(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        super(createTableModel(networkViewer, topologyPanel), networkViewer, NetworkElementType.ROUTE);
        setDefaultCellRenderers(networkViewer);
        setSpecificCellRenderers();
        setColumnRowSorting(networkViewer.inOnlineSimulationMode());

    }

    public List<Object[]> getAllData(NetPlan currentState, TopologyPanel topologyPanel, NetPlan initialState) {
        final boolean sameRoutingType = initialState != null && initialState.getRoutingType() == currentState.getRoutingType();
        List<Object[]> allRouteData = new LinkedList<Object[]>();
        for (Route route : currentState.getRoutes()) {
            Demand demand = route.getDemand();
            double maxUtilization = 0;
            for (Link e : route.getSeqLinksRealPath())
                maxUtilization = Math.max(maxUtilization, e.getOccupiedCapacityIncludingProtectionSegments() / e.getCapacity());
            Node ingressNode = route.getDemand().getIngressNode();
            Node egressNode = route.getDemand().getEgressNode();
            String ingressNodeName = ingressNode.getName();
            String egressNodeName = egressNode.getName();

            Object[] routeData = new Object[netPlanViewTableHeader.length];
            routeData[0] = route.getId();
            routeData[1] = route.getIndex();
            routeData[2] = demand.getIndex();
            routeData[3] = ingressNode.getIndex() + (ingressNodeName.isEmpty() ? "" : " (" + ingressNodeName + ")");
            routeData[4] = egressNode.getIndex() + (egressNodeName.isEmpty() ? "" : " (" + egressNodeName + ")");
            routeData[5] = demand.getOfferedTraffic();
            routeData[6] = route.getCarriedTraffic();
            routeData[7] = route.getOccupiedCapacity();
            routeData[8] = CollectionUtils.join(NetPlan.getIndexes(route.getSeqLinksRealPath()), " => ");
            routeData[9] = CollectionUtils.join(NetPlan.getIndexes(route.getSeqNodesRealPath()), " => ");
            routeData[10] = route.getNumberOfHops();
            routeData[11] = route.getLengthInKm();
            routeData[12] = route.getPropagationDelayInMiliseconds();
            routeData[13] = maxUtilization;
            routeData[14] = CollectionUtils.join(NetPlan.getIndexes(route.getPotentialBackupProtectionSegments()), ", ");
            routeData[15] = StringUtils.mapToString(route.getAttributes());
            allRouteData.add(routeData);

            if (initialState != null && sameRoutingType && initialState.getRouteFromId(route.getId()) != null) {
                route = initialState.getRouteFromId(route.getId());
                demand = route.getDemand();
                maxUtilization = 0;
                for (Link e : route.getSeqLinksRealPath())
                    maxUtilization = Math.max(maxUtilization, e.getOccupiedCapacityIncludingProtectionSegments() / e.getCapacity());
                ingressNode = route.getDemand().getIngressNode();
                egressNode = route.getDemand().getEgressNode();
                ingressNodeName = ingressNode.getName();
                egressNodeName = egressNode.getName();

                Object[] routeData_initialNetPlan = new Object[netPlanViewTableHeader.length];
                routeData_initialNetPlan[0] = null;
                routeData_initialNetPlan[1] = null;
                routeData_initialNetPlan[2] = null;
                routeData_initialNetPlan[3] = null;
                routeData_initialNetPlan[4] = null;
                routeData_initialNetPlan[5] = demand.getOfferedTraffic();
                routeData_initialNetPlan[6] = route.getCarriedTraffic();
                routeData_initialNetPlan[7] = route.getOccupiedCapacity();
                routeData_initialNetPlan[8] = CollectionUtils.join(NetPlan.getIndexes(route.getSeqLinksRealPath()), " => ");
                routeData_initialNetPlan[9] = CollectionUtils.join(NetPlan.getIndexes(route.getSeqNodesRealPath()), " => ");
                routeData_initialNetPlan[10] = route.getNumberOfHops();
                routeData_initialNetPlan[11] = route.getLengthInKm();
                routeData_initialNetPlan[12] = route.getPropagationDelayInMiliseconds();
                routeData_initialNetPlan[13] = maxUtilization;
                routeData_initialNetPlan[14] = CollectionUtils.join(NetPlan.getIndexes(route.getPotentialBackupProtectionSegments()), ", ");
                routeData_initialNetPlan[15] = StringUtils.mapToString(route.getAttributes());
                allRouteData.add(routeData_initialNetPlan);
            }
        }

        return allRouteData;
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
        return np.hasRoutes();
    }

    public int[] getColumnsOfSpecialComparatorForSorting() {
        return new int[]{};
    }

    private static TableModel createTableModel(final IGUINetworkViewer networkViewer, final TopologyPanel topologyPanel) {
        TableModel routeTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
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
                final long routeId = (Long) getValueAt(row, 0);
                final Route route = netPlan.getRouteFromId(routeId);
                /* Perform checks, if needed */
                try {
                    switch (column) {
                        case COLUMN_CARRIEDTRAFFIC:
                            route.setCarriedTraffic(Double.parseDouble(newValue.toString()), route.getOccupiedCapacity());
                            networkViewer.updateNetPlanView();
                            break;

                        case COLUMN_OCCUPIEDCAPACITY:
                            route.setCarriedTraffic(route.getCarriedTraffic(), Double.parseDouble(newValue.toString()));
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
        return routeTableModel;
    }

    private void setDefaultCellRenderers(final IGUINetworkViewer networkViewer) {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());

        setDefaultRenderer(Boolean.class, new CellRenderers.RouteRenderer(getDefaultRenderer(Boolean.class), networkViewer));
        setDefaultRenderer(Double.class, new CellRenderers.RouteRenderer(getDefaultRenderer(Double.class), networkViewer));
        setDefaultRenderer(Object.class, new CellRenderers.RouteRenderer(getDefaultRenderer(Object.class), networkViewer));
        setDefaultRenderer(Float.class, new CellRenderers.RouteRenderer(getDefaultRenderer(Float.class), networkViewer));
        setDefaultRenderer(Long.class, new CellRenderers.RouteRenderer(getDefaultRenderer(Long.class), networkViewer));
        setDefaultRenderer(Integer.class, new CellRenderers.RouteRenderer(getDefaultRenderer(Integer.class), networkViewer));
        setDefaultRenderer(String.class, new CellRenderers.RouteRenderer(getDefaultRenderer(String.class), networkViewer));
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
                                netPlan.getRouteFromId((long) itemId).remove();
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
                            netPlan.removeAllRoutes();
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
                networkViewer.showRoute((long) itemId);
                break;

            case 2:
                int col = convertColumnIndexToModel(columnAtPoint(e.getPoint()));
                if (col == -1 || col >= getColumnCount()) return;

                NetPlan netPlan = networkViewer.getDesign();

                Demand demand = netPlan.getRouteFromId((long) itemId).getDemand();
                switch (col) {
                    case COLUMN_DEMAND:
                        networkViewer.showDemand(demand.getId());
                        break;
                    case COLUMN_INGRESSNODE:
                        networkViewer.showNode(demand.getIngressNode().getId());
                        break;
                    case COLUMN_EGRESSNODE:
                        networkViewer.showNode(demand.getEgressNode().getId());
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    private boolean isTableEmpty() {
        return !networkViewer.getDesign().hasRoutes();
    }

    private JMenuItem getAddOption() {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = networkViewer.getDesign();

                try {
                    createRouteGUI(networkViewer, networkViewer.getTopologyPanel());
                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });

        NetPlan netPlan = networkViewer.getDesign();
        if (!netPlan.hasDemands()) addItem.setEnabled(false);
        return addItem;
    }

    private static void createRouteGUI(final INetworkCallback networkViewer, final TopologyPanel topologyPanel) {
        final NetPlan netPlan = networkViewer.getDesign();
        final Collection<Long> demandIds = NetPlan.getIds(netPlan.getDemands());
        final JComboBox demandSelector = new WiderJComboBox();
        if (netPlan.getNumberOfLinks() == 0)
            throw new Net2PlanException("The network has no links at this network layer");

        final JTextField txt_carriedTraffic = new JTextField();
        final JTextField txt_occupiedCapacity = new JTextField();

        final List<JComboBox> seqLinks_cmb = new LinkedList<JComboBox>();
        final JPanel seqLinks_pnl = new JPanel();
        seqLinks_pnl.setLayout(new BoxLayout(seqLinks_pnl, BoxLayout.Y_AXIS));

        demandSelector.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
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

                Collection<Link> outgoingLinks = ingressNode.getOutgoingLinks();
                final JComboBox firstLink = new WiderJComboBox();
                for (Link link : outgoingLinks) {
                    long destinationNodeId = link.getDestinationNode().getId();
                    String destinationNodeName = link.getDestinationNode().getName();
                    firstLink.addItem(StringLabeller.of(link.getId(), String.format("e%d: n%d (%s) => n%d (%s)", link.getId(), ingressNode.getId(), ingressNodeName, destinationNodeId, destinationNodeName)));
                }

                firstLink.addItemListener(new ItemListener() {
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
                        for (JComboBox link : seqLinks_cmb) {
                            seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));
                        }
                        Map<Link, Pair<Color, Boolean>> res = new HashMap<Link, Pair<Color, Boolean>>();
                        for (Link link : seqLinks) res.put(link, Pair.of(Color.BLUE, false));

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
                        Link link = netPlan.getLinkFromId(linkId);
                        long destinationNodeId = link.getDestinationNode().getId();
                        String destinationNodeName = link.getDestinationNode().getName();

                        Set<Link> outgoingLinks = link.getDestinationNode().getOutgoingLinks();
                        if (outgoingLinks.isEmpty()) {
                            ErrorHandling.showErrorDialog("Last node has no outgoing links", "Error");
                            return;
                        }

                        final JComboBox newLink = new WiderJComboBox();
                        for (Link nextLink : outgoingLinks) {
                            long nextDestinationNodeId = nextLink.getDestinationNode().getId();
                            String nextDestinationNodeName = nextLink.getDestinationNode().getName();
                            newLink.addItem(StringLabeller.of(nextLink.getId(), String.format("e%d: n%d (%s) => n%d (%s)", nextLink.getId(), destinationNodeId, destinationNodeName, nextDestinationNodeId, nextDestinationNodeName)));
                        }

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
                        for (JComboBox auxLink : seqLinks_cmb) {
                            seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) auxLink.getSelectedItem()).getObject()));
                        }
                        Map<Link, Pair<Color, Boolean>> res = new HashMap<Link, Pair<Color, Boolean>>();
                        for (Link thisLink : seqLinks) res.put(thisLink, Pair.of(Color.BLUE, false));
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
                        for (Link thisLink : seqLinks) res.put(thisLink, Pair.of(Color.BLUE, false));
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
                for (Link thisLink : seqLinks) res.put(thisLink, Pair.of(Color.BLUE, false));
                topologyPanel.getCanvas().showAndPickNodesAndLinks(null, res);
            }
        });

        for (long demandId : demandIds) {
            Demand demand = netPlan.getDemandFromId(demandId);
            Node ingressNode = demand.getIngressNode();
            Node egressNode = demand.getEgressNode();
            long ingressNodeId = ingressNode.getId();
            long egressNodeId = egressNode.getId();

            final String ingressNodeName = ingressNode.getName();
            final String egressNodeName = egressNode.getName();

            final Set<Link> outgoingLinks = ingressNode.getOutgoingLinks();
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

        if (demandSelector.getItemCount() == 0) throw new Net2PlanException("Bad - No node has outgoing links");

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

        while (true) {
            int result = JOptionPane.showConfirmDialog(null, pane, "Add new route", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) break;

            long demandId = (Long) ((StringLabeller) demandSelector.getSelectedItem()).getObject();

            double carriedTraffic;
            double occupiedCapacity = -1;

            try {
                carriedTraffic = Double.parseDouble(txt_carriedTraffic.getText());
                if (carriedTraffic < 0) throw new RuntimeException();
            } catch (Throwable e) {
                ErrorHandling.showErrorDialog("Carried traffic must be a non-negative number", "Error adding route");
                continue;
            }

            try {
                if (!txt_occupiedCapacity.getText().isEmpty()) {
                    occupiedCapacity = Double.parseDouble(txt_occupiedCapacity.getText());
                    if (occupiedCapacity < 0 && occupiedCapacity != -1) throw new RuntimeException();
                }
            } catch (Throwable e) {
                ErrorHandling.showErrorDialog("Occupied capacity must be a non-negative number, -1 or empty", "Error adding route");
                continue;
            }

            List<Link> seqLinks = new LinkedList<Link>();
            for (JComboBox link : seqLinks_cmb)
                seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));

            try {
                netPlan.addRoute(netPlan.getDemandFromId(demandId), carriedTraffic, occupiedCapacity, seqLinks, null);
            } catch (Throwable e) {
                ErrorHandling.showErrorDialog(e.getMessage(), "Error adding route");
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
        List<JComponent> options = new LinkedList<JComponent>();
        NetPlan netPlan = networkViewer.getDesign();

        final JMenuItem oneRoutePerDemandSPFHops = new JMenuItem("Add one route per demand, shortest path in hops");
        options.add(oneRoutePerDemandSPFHops);
        oneRoutePerDemandSPFHops.addActionListener(new RouteSPFActionListener(true, false));
        final JMenuItem oneRoutePerDemandSPFKm = new JMenuItem("Add one route per demand, shortest path in km");
        options.add(oneRoutePerDemandSPFKm);
        oneRoutePerDemandSPFKm.addActionListener(new RouteSPFActionListener(false, false));
        final JMenuItem oneRouteAndLinkDisjointSegmentPerDemandSPFHops = new JMenuItem("Add one route and 1+1 link disjoint protection per demand (minimize total num hops)");
        options.add(oneRouteAndLinkDisjointSegmentPerDemandSPFHops);
        oneRouteAndLinkDisjointSegmentPerDemandSPFHops.addActionListener(new RouteSPFActionListener(true, true));
        final JMenuItem oneRouteAndLinkDisjointSegmentPerDemandSPFKm = new JMenuItem("Add one route and 1+1 link disjoint protection per demand (minimize total km)");
        options.add(oneRouteAndLinkDisjointSegmentPerDemandSPFKm);
        oneRouteAndLinkDisjointSegmentPerDemandSPFKm.addActionListener(new RouteSPFActionListener(false, true));


        return options;
    }

    private class RouteSPFActionListener implements ActionListener {
        final boolean isMinHops;
        final boolean add11LinkDisjointSegment;

        private RouteSPFActionListener(boolean isMinHops, boolean minCost) {
            this.isMinHops = isMinHops;
            this.add11LinkDisjointSegment = minCost;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NetPlan netPlan = networkViewer.getDesign();
            List<Link> links = netPlan.getLinks();
            final int E = links.size();
            Map<Link, Double> linkCostMap = new HashMap<Link, Double>();
            List<Route> addedRoutes = new LinkedList<Route>();
            List<ProtectionSegment> addedProtectionSegments = new LinkedList<ProtectionSegment>();
            for (Link link : netPlan.getLinks()) linkCostMap.put(link, isMinHops ? 1 : link.getLengthInKm());
            try {
                for (Demand d : netPlan.getDemands()) {
                    if (add11LinkDisjointSegment) {
                        List<List<Link>> twoPaths = GraphUtils.getTwoLinkDisjointPaths(netPlan.getNodes(), netPlan.getLinks(), d.getIngressNode(), d.getEgressNode(), linkCostMap);
                        if (twoPaths.size() != 2)
                            throw new Net2PlanException("Cannot find two link disjoint paths for demand of index " + d.getIndex() + ". No route or protection segment is created");
                        Route r = netPlan.addRoute(d, d.getOfferedTraffic(), d.getOfferedTraffic(), twoPaths.get(0), null);
                        ProtectionSegment s = netPlan.addProtectionSegment(twoPaths.get(1), d.getOfferedTraffic(), null);
                        r.addProtectionSegment(s);
                        addedRoutes.add(r);
                        addedProtectionSegments.add(s);
                    } else {
                        List<Link> seqLinks = GraphUtils.getShortestPath(netPlan.getNodes(), netPlan.getLinks(), d.getIngressNode(), d.getEgressNode(), linkCostMap);
                        if (seqLinks.isEmpty())
                            throw new Net2PlanException("Cannot find a route for demand of index " + d.getIndex() + ". No route is created");
                        Route r = netPlan.addRoute(d, d.getOfferedTraffic(), d.getOfferedTraffic(), seqLinks, null);
                        addedRoutes.add(r);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                for (Route r : addedRoutes) r.remove();
                for (ProtectionSegment s : addedProtectionSegments) s.remove();
                ErrorHandling.showErrorDialog(ex.getMessage(), "Error adding routes and/or protection segments");
            }
            networkViewer.updateNetPlanView();
        }
    }

    private List<JComponent> getExtraOptions(final int row, final Object itemId) {
        List<JComponent> options = new LinkedList<JComponent>();

        final int numRows = model.getRowCount();
        final NetPlan netPlan = networkViewer.getDesign();

        if (itemId != null) {
            JMenuItem viewEditProtectionSegments = new JMenuItem("View/edit backup segment list");
            viewEditProtectionSegments.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        viewEditBackupSegmentListGUI(networkViewer, networkViewer.getTopologyPanel(), (long) itemId);
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error viewing/editing backup segment list");
                    }
                }
            });

            options.add(viewEditProtectionSegments);
        }


        return options;
    }

    private static void viewEditBackupSegmentListGUI(final INetworkCallback callback, final TopologyPanel topologyPanel, final long routeId) {
        final NetPlan netPlan = callback.getDesign();
        final Route route = netPlan.getRouteFromId(routeId);

        Set<Long> candidateSegmentIds = new HashSet<Long>();
        Set<Long> currentSegmentIds = new HashSet<Long>();

        final Collection<Link> seqLinks = route.getSeqLinksAndProtectionSegments();

        for (ProtectionSegment segment : netPlan.getProtectionSegments()) {
            try {
                /* Only considered a segment as candidate if it is mergeable to the current route */
//				List<Long> seqLinks_thisSegment = netPlan.getProtectionSegmentSequenceOfLinks(segmentId);
//				List<Long> mergedRoute = netPlan.getMergedPath(seqLinks, seqLinks_thisSegment); /* Not remove! It's used to check mergeability */
                candidateSegmentIds.add(segment.getId());
            } catch (Throwable e) {
            }
        }

        if (candidateSegmentIds.isEmpty()) throw new Net2PlanException("No segment can be applied to this route");

        currentSegmentIds.addAll(NetPlan.getIds(route.getPotentialBackupProtectionSegments()));
        candidateSegmentIds.removeAll(currentSegmentIds);

        final JComboBox segmentSelector = new WiderJComboBox();

        final DefaultTableModel model = new ClassAwareTableModel(new Object[1][6], new String[]{"Id", "Seq. links", "Seq. nodes", "Reserved capacity", "", ""}) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 4 || columnIndex == 5;
            }
        };
        final JTable table = new AdvancedJTable(model);
        table.setEnabled(false);

        final JPanel addSegment_pnl = new JPanel(new MigLayout("", "[grow][][]", "[]"));
        JButton addSegment_btn = new JButton("Add");
        addSegment_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object selectedItem = segmentSelector.getSelectedItem();
                long segmentId = (Long) ((StringLabeller) selectedItem).getObject();
                ProtectionSegment segment = netPlan.getProtectionSegmentFromId(segmentId);
                route.addProtectionSegment(segment);
                callback.updateNetPlanView();

                segmentSelector.removeItem(selectedItem);
                if (segmentSelector.getItemCount() == 0) addSegment_pnl.setVisible(false);

                Collection<Long> segmentSeqLinks = NetPlan.getIds(segment.getSeqLinks());
                Collection<Long> segmentSeqNodes = NetPlan.getIds(segment.getSeqNodes());
                double reservedCapacity = segment.getReservedCapacityForProtection();

                if (!table.isEnabled()) model.removeRow(0);
                model.addRow(new Object[]{segmentId, CollectionUtils.join(segmentSeqLinks, " => "), CollectionUtils.join(segmentSeqNodes, " => "), reservedCapacity, "Remove", "View"});

                table.setEnabled(true);
            }
        });

        JButton viewSegment_btn1 = new JButton("View");
        viewSegment_btn1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object selectedItem = segmentSelector.getSelectedItem();
                long segmentId = (Long) ((StringLabeller) selectedItem).getObject();

                Collection<Link> segmentSeqLinks = netPlan.getProtectionSegmentFromId(segmentId).getSeqLinks();
                Map<Link, Pair<Color, Boolean>> coloredLinks = new HashMap<Link, Pair<Color, Boolean>>();
                for (Link thisLink : seqLinks) coloredLinks.put(thisLink, Pair.of(Color.BLUE, false));
                for (Link thisLink : segmentSeqLinks) coloredLinks.put(thisLink, Pair.of(Color.ORANGE, false));
                topologyPanel.getCanvas().showAndPickNodesAndLinks(null, coloredLinks);
            }
        });

        addSegment_pnl.add(segmentSelector, "growx, wmin 50");
        addSegment_pnl.add(addSegment_btn);
        addSegment_pnl.add(viewSegment_btn1);

        Action delete = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    JTable table = (JTable) e.getSource();
                    int modelRow = Integer.parseInt(e.getActionCommand());

                    long segmentId = (Long) table.getModel().getValueAt(modelRow, 0);
                    ProtectionSegment segment = netPlan.getProtectionSegmentFromId(segmentId);
                    netPlan.getRouteFromId(routeId).removeProtectionSegmentFromBackupSegmentList(segment);
                    callback.updateNetPlanView();

                    Collection<Long> segmentSeqLinks = NetPlan.getIds(segment.getSeqLinks());
                    Collection<Long> segmentSeqNodes = NetPlan.getIds(segment.getSeqNodes());
                    double reservedCapacity = segment.getReservedCapacityForProtection();

                    String segmentLabel = "Protection segment " + segmentId;
                    segmentLabel += ": seq. links = " + CollectionUtils.join(segmentSeqLinks, " => ");
                    segmentLabel += ", seq. nodes = " + CollectionUtils.join(segmentSeqNodes, " => ");
                    segmentLabel += ", reserved capacity = " + reservedCapacity;

                    segmentSelector.addItem(StringLabeller.of(segmentId, segmentLabel));

                    ((DefaultTableModel) table.getModel()).removeRow(modelRow);

                    table.setEnabled(true);

                    if (table.getModel().getRowCount() == 0) {
                        ((DefaultTableModel) table.getModel()).addRow(new Object[6]);
                        table.setEnabled(false);
                    }
                } catch (Throwable e1) {
                }
            }
        };

        Action view = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    JTable table = (JTable) e.getSource();
                    int modelRow = Integer.parseInt(e.getActionCommand());

                    long segmentId = (Long) table.getModel().getValueAt(modelRow, 0);
                    Collection<Link> segmentSeqLinks = netPlan.getProtectionSegmentFromId(segmentId).getSeqLinks();
                    Map<Link, Pair<Color, Boolean>> coloredLinks = new HashMap<Link, Pair<Color, Boolean>>();
                    for (Link thisLink : seqLinks) coloredLinks.put(thisLink, Pair.of(Color.BLUE, false));
                    for (Link thisLink : segmentSeqLinks) coloredLinks.put(thisLink, Pair.of(Color.ORANGE, false));
                    topologyPanel.getCanvas().showAndPickNodesAndLinks(null, coloredLinks);
                } catch (Throwable e1) {
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

        for (long segmentId : candidateSegmentIds) {
            ProtectionSegment segment = netPlan.getProtectionSegmentFromId(segmentId);
            Collection<Long> segmentSeqLinks = NetPlan.getIds(segment.getSeqLinks());
            Collection<Long> segmentSeqNodes = NetPlan.getIds(segment.getSeqNodes());
            double reservedCapacity = segment.getReservedCapacityForProtection();

            String segmentLabel = "Protection segment " + segmentId;
            segmentLabel += ": seq. links = " + CollectionUtils.join(segmentSeqLinks, " => ");
            segmentLabel += ", seq. nodes = " + CollectionUtils.join(segmentSeqNodes, " => ");
            segmentLabel += ", reserved capacity = " + reservedCapacity;

            segmentSelector.addItem(StringLabeller.of(segmentId, segmentLabel));
        }

        if (segmentSelector.getItemCount() == 0) {
            addSegment_pnl.setVisible(false);
        } else {
            segmentSelector.setSelectedIndex(0);
        }

        if (!currentSegmentIds.isEmpty()) {
            model.removeRow(0);

            for (long segmentId : currentSegmentIds) {
                ProtectionSegment segment = netPlan.getProtectionSegmentFromId(segmentId);
                Collection<Long> segmentSeqLinks = NetPlan.getIds(segment.getSeqLinks());
                Collection<Long> segmentSeqNodes = NetPlan.getIds(segment.getSeqNodes());
                double reservedCapacity = segment.getReservedCapacityForProtection();
                model.addRow(new Object[]{segmentId, CollectionUtils.join(segmentSeqLinks, " => "), CollectionUtils.join(segmentSeqNodes, " => "), reservedCapacity, "Remove", "View"});
            }

            table.setEnabled(true);
        }

        table.setDefaultRenderer(Boolean.class, new UnfocusableCellRenderer());
        table.setDefaultRenderer(Double.class, new UnfocusableCellRenderer());
        table.setDefaultRenderer(Object.class, new UnfocusableCellRenderer());
        table.setDefaultRenderer(Float.class, new UnfocusableCellRenderer());
        table.setDefaultRenderer(Long.class, new UnfocusableCellRenderer());
        table.setDefaultRenderer(Integer.class, new UnfocusableCellRenderer());
        table.setDefaultRenderer(String.class, new UnfocusableCellRenderer());

        double x_p = netPlan.getRouteFromId(routeId).getCarriedTraffic();
        double y_p = netPlan.getRouteFromId(routeId).getOccupiedCapacity();
        dialog.setTitle("View/edit backup segment list for route " + routeId + " (carried traffic = " + x_p + ", occupied capacity = " + y_p + ")");
        SwingUtils.configureCloseDialogOnEscape(dialog);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(new Dimension(500, 300));
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);

        topologyPanel.getCanvas().resetPickedAndUserDefinedColorState();
    }

    private List<JComponent> getForcedOptions() {
        return new LinkedList<JComponent>();
    }
}
