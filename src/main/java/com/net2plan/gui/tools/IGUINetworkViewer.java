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


package com.net2plan.gui.tools;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.net2plan.gui.tools.rightPanelTabs.NetPlanViewTableComponent_layer;
import com.net2plan.gui.tools.rightPanelTabs.NetPlanViewTableComponent_network;
import com.net2plan.gui.tools.specificTables.*;
import com.net2plan.gui.utils.CurrentAndPlannedStateTableSorter.CurrentAndPlannedStateTableCellValue;
import com.net2plan.gui.utils.*;
import com.net2plan.gui.utils.topology.*;
import com.net2plan.gui.utils.topology.jung.AddLinkGraphPlugin;
import com.net2plan.gui.utils.topology.jung.JUNGCanvas;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.SystemUtils;
import com.net2plan.internal.plugins.IGUIModule;
import com.net2plan.libraries.NetworkPerformanceMetrics;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.Closeable;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

/**
 * Template for any tool requiring network visualization.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
public abstract class IGUINetworkViewer extends IGUIModule implements INetworkCallback, ThreadExecutionController.IThreadExecutionHandler {
    public static Color COLOR_INITIALNODE = new Color(0, 153, 51);
    public static Color COLOR_ENDNODE = new Color(0, 162, 215);

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Reference to the topology panel.
     *
     * @since 0.3.0
     */
    protected TopologyPanel topologyPanel;

    /**
     * Reference to the popup menu in the topology panel.
     *
     * @since 0.3.0
     */
    protected ITopologyCanvasPlugin popupPlugin;

    private JPanel leftPane;
    private JSplitPane reportPane;
    private JTabbedPane rightPane, netPlanView, reportContainer;
    private RunnableSelector reportSelector;
    private ThreadExecutionController reportController;
    private int viewNetPlanTabIndex;
    private JButton closeAllReports;

    private Map<NetworkElementType, AdvancedJTableNetworkElement> netPlanViewTable;
    private Map<NetworkElementType, FixedColumnDecorator> netPlanViewTableDecorator;
    private Map<NetworkElementType, JComponent> netPlanViewTableComponent;

    protected JCheckBox showInitialPlan;
    private boolean allowDocumentUpdate;
    private NetPlan currentNetPlan, initialNetPlan;

    /**
     * Constructor that allows set a title for the tool in the top section of the panel.
     *
     * @param title Title of the tool (null or empty means no title)
     * @since 0.3.0
     */
    public IGUINetworkViewer(String title) {
        super(title);
    }

    @Override
    public long addLink(long originNode, long destinationNode) {
        long layer = getDesign().getNetworkLayerDefault().getId();
        return addLink(layer, originNode, destinationNode);
    }

    @Override
    public long addLink(long layer, long originNode, long destinationNode) {
        if (!isEditable()) throw new UnsupportedOperationException("Not supported");

        NetPlan netPlan = getDesign();
        Link link = netPlan.addLink(netPlan.getNodeFromId(originNode), netPlan.getNodeFromId(destinationNode), 0, 0, 200000, null, netPlan.getNetworkLayerFromId(layer));

        if (layer == netPlan.getNetworkLayerDefault().getId()) {
            getTopologyPanel().getCanvas().addLink(link);
            getTopologyPanel().getCanvas().refresh();
        }

        updateNetPlanView();
        return link.getId();
    }

    @Override
    public Pair<Long, Long> addLinkBidirectional(long originNode, long destinationNode) {
        return addLinkBidirectional(getDesign().getNetworkLayerDefault().getId(), originNode, destinationNode);
    }

    @Override
    public Pair<Long, Long> addLinkBidirectional(long layer, long originNode, long destinationNode) {
        if (!isEditable()) throw new UnsupportedOperationException("Not supported");

        NetPlan netPlan = getDesign();
        Pair<Link, Link> links = netPlan.addLinkBidirectional(netPlan.getNodeFromId(originNode), netPlan.getNodeFromId(destinationNode), 0, 0, 200000, null, netPlan.getNetworkLayerFromId(layer));
        if (layer == netPlan.getNetworkLayerDefault().getId()) {
            getTopologyPanel().getCanvas().addLink(links.getFirst());
            getTopologyPanel().getCanvas().addLink(links.getSecond());
            getTopologyPanel().getCanvas().refresh();
        }

        updateNetPlanView();
        return Pair.of(links.getFirst().getId(), links.getSecond().getId());
    }

    @Override
    public void addNode(Point2D pos) {
        if (!isEditable()) throw new UnsupportedOperationException("Not supported");

        NetPlan netPlan = getDesign();
        long nodeId = netPlan.getNetworkElementNextId();
        Node node = netPlan.addNode(pos.getX(), pos.getY(), "Node " + nodeId, null);
        getTopologyPanel().getCanvas().addNode(node);
        getTopologyPanel().getCanvas().refresh();
        updateNetPlanView();
    }

    @Override
    public void configure(JPanel contentPane) {
        topologyPanel = new TopologyPanel(this, JUNGCanvas.class);
        configureTopologyPanel();
        topologyPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Network topology"));
        topologyPanel.setAllowLoadTrafficDemand(allowLoadTrafficDemands());

        addKeyCombinationAction("Load design", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getTopologyPanel().loadDesign();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));

        addKeyCombinationAction("Save design", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getTopologyPanel().saveDesign();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));

        addKeyCombinationAction("Zoom in", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getTopologyPanel().zoomIn();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK));

        addKeyCombinationAction("Zoom out", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getTopologyPanel().zoomOut();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));

        addKeyCombinationAction("Zoom all", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getTopologyPanel().zoomAll();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY, InputEvent.CTRL_DOWN_MASK));

        addKeyCombinationAction("Take snapshot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getTopologyPanel().takeSnapshot();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.CTRL_DOWN_MASK));

        if (allowLoadTrafficDemands()) {
            addKeyCombinationAction("Load traffic demands", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    getTopologyPanel().loadTrafficDemands();
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
        }

        leftPane = new JPanel(new BorderLayout());
        JPanel logSection = configureLeftBottomPanel();
        if (logSection == null) {
            leftPane.add(topologyPanel, BorderLayout.CENTER);
        } else {
            JSplitPane splitPaneTopology = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            splitPaneTopology.setTopComponent(topologyPanel);
            splitPaneTopology.setBottomComponent(logSection);
            splitPaneTopology.setResizeWeight(0.8);
            splitPaneTopology.addPropertyChangeListener(new ProportionalResizeJSplitPaneListener());
            splitPaneTopology.setBorder(new LineBorder(contentPane.getBackground()));
            leftPane.add(splitPaneTopology, BorderLayout.CENTER);
        }

        rightPane = new JTabbedPane();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(leftPane);
        splitPane.setRightComponent(rightPane);
        splitPane.setResizeWeight(0.5);
        splitPane.addPropertyChangeListener(new ProportionalResizeJSplitPaneListener());

        splitPane.setBorder(BorderFactory.createEmptyBorder());
        contentPane.add(splitPane, "grow");

        configureNetPlanView();
        configureReportPane();
        loadDesign(new NetPlan());

        addKeyCombinationAction("Resets the tool", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));

        addKeyCombinationAction("Outputs current design to console", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println(getDesign().toString());
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_F11, InputEvent.CTRL_DOWN_MASK));

        for (int tabId = 0; tabId <= 8; tabId++) {
            final int key;
            switch (tabId) {
                case 0:
                    key = KeyEvent.VK_1;
                    break;

                case 1:
                    key = KeyEvent.VK_2;
                    break;

                case 2:
                    key = KeyEvent.VK_3;
                    break;

                case 3:
                    key = KeyEvent.VK_4;
                    break;

                case 4:
                    key = KeyEvent.VK_5;
                    break;

                case 5:
                    key = KeyEvent.VK_6;
                    break;

                case 6:
                    key = KeyEvent.VK_7;
                    break;

                case 7:
                    key = KeyEvent.VK_8;
                    break;

                case 8:
                    key = KeyEvent.VK_9;
                    break;

                default:
                    throw new RuntimeException("Bad");
            }

            addKeyCombinationAction("Open right tab " + tabId, new SwitchTabAction(tabId), KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK));
        }
    }

    @Override
    public Object execute(ThreadExecutionController controller) {
        if (controller == reportController) {
            Triple<File, String, Class> report = reportSelector.getRunnable();
            Map<String, String> reportParameters = reportSelector.getRunnableParameters();
            Map<String, String> net2planParameters = Configuration.getNet2PlanOptions();
            IReport instance = ClassLoaderUtils.getInstance(report.getFirst(), report.getSecond(), IReport.class);
            String title = null;
            try {
                title = instance.getTitle();
            } catch (UnsupportedOperationException ex) {
            }
            if (title == null) title = "Untitled";

            Pair<String, ? extends JPanel> aux = Pair.of(title, new ReportBrowser(instance.executeReport(getDesign().copy(), reportParameters, net2planParameters)));
            try {
                ((Closeable) instance.getClass().getClassLoader()).close();
            } catch (Throwable e) {
            }

            return aux;
        } else {
            throw new RuntimeException("Bad");
        }
    }

    @Override
    public void executionFailed(ThreadExecutionController controller) {
        if (controller == reportController) {
            ErrorHandling.showErrorDialog("Error executing report");
        } else {
            ErrorHandling.showErrorDialog("Bad");
        }
    }

    @Override
    public void executionFinished(ThreadExecutionController controller, Object out) {
        if (controller == reportController) {
            Pair<String, ? extends JPanel> aux = (Pair<String, ? extends JPanel>) out;
            reportContainer.addTab(aux.getFirst(), new TabIcon(TabIcon.IconType.TIMES_SIGN), aux.getSecond());
            reportContainer.setSelectedIndex(reportContainer.getTabCount() - 1);
        } else {
            ErrorHandling.showErrorDialog("Bad");
        }
    }

    @Override
    public NetPlan getDesign() {
        return currentNetPlan;
    }

    @Override
    public NetPlan getInitialDesign() {
        return initialNetPlan;
    }

    @Override
    public List<JComponent> getCanvasActions(Point2D pos) {
        List<JComponent> actions = new LinkedList<JComponent>();

        if (isEditable())
            actions.add(new JMenuItem(new AddNodeAction("Add node here", pos)));

        return actions;
    }

    @Override
    public List<JComponent> getLinkActions(long link, Point2D pos) {
        List<JComponent> actions = new LinkedList<JComponent>();

        if (isEditable())
            actions.add(new JMenuItem(new RemoveLinkAction("Remove link", link)));

        return actions;
    }

    @Override
    public List<JComponent> getNodeActions(long nodeId, Point2D pos) {
        List<JComponent> actions = new LinkedList<JComponent>();

        if (isEditable()) {
            actions.add(new JMenuItem(new RemoveNodeAction("Remove node", nodeId)));

            NetPlan netPlan = getDesign();
            Node node = netPlan.getNodeFromId(nodeId);
            if (netPlan.getNumberOfNodes() > 1) {
                actions.add(new JPopupMenu.Separator());
                JMenu unidirectionalMenu = new JMenu("Create unidirectional link");
                JMenu bidirectionalMenu = new JMenu("Create bidirectional link");

                String nodeName = node.getName() == null ? "" : node.getName();
                String nodeString = Long.toString(nodeId) + (nodeName.isEmpty() ? "" : " (" + nodeName + ")");

                long layer = netPlan.getNetworkLayerDefault().getId();
                for (Node auxNode : netPlan.getNodes()) {
                    if (auxNode.equals(nodeId)) continue;

                    String auxNodeName = auxNode.getName() == null ? "" : auxNode.getName();
                    String auxNodeString = Long.toString(auxNode.getId()) + (auxNodeName.isEmpty() ? "" : " (" + auxNodeName + ")");

                    AbstractAction unidirectionalAction = new AddLinkAction(nodeString + " => " + auxNodeString, layer, nodeId, auxNode.getId());
                    unidirectionalMenu.add(unidirectionalAction);

                    AbstractAction bidirectionalAction = new AddLinkBidirectionalAction(nodeString + " <=> " + auxNodeString, layer, nodeId, auxNode.getId());
                    bidirectionalMenu.add(bidirectionalAction);
                }

                actions.add(unidirectionalMenu);
                actions.add(bidirectionalMenu);
            }
        }

        return actions;
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public void layerChanged(long layer) {
    }

    @Override
    public void loadDesign(NetPlan netPlan) {
        netPlan.checkCachesConsistency();
        setNetPlan(netPlan);
        netPlan.checkCachesConsistency();
        topologyPanel.updateLayerChooser();
        topologyPanel.getCanvas().zoomAll();
        resetView();
    }

    @Override
    public void loadTrafficDemands(NetPlan demands) {
        if (!demands.hasDemands() && !demands.hasMulticastDemands())
            throw new Net2PlanException("Selected file doesn't contain a demand set");

        NetPlan netPlan = getDesign();
        if (netPlan.hasDemands() || netPlan.hasMulticastDemands()) {
            int result = JOptionPane.showConfirmDialog(null, "Current network structure contains a demand set. Overwrite?", "Loading demand set", JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) return;
        }

        NetPlan aux_netPlan = netPlan.copy();
        try {
            netPlan.removeAllDemands();
            for (Demand demand : demands.getDemands())
                netPlan.addDemand(netPlan.getNode(demand.getIngressNode().getIndex()), netPlan.getNode(demand.getEgressNode().getIndex()), demand.getOfferedTraffic(), demand.getAttributes());

            netPlan.removeAllMulticastDemands();
            for (MulticastDemand demand : demands.getMulticastDemands()) {
                Set<Node> egressNodesThisNetPlan = new HashSet<Node>();
                for (Node n : demand.getEgressNodes()) egressNodesThisNetPlan.add(netPlan.getNode(n.getIndex()));
                netPlan.addMulticastDemand(netPlan.getNode(demand.getIngressNode().getIndex()), egressNodesThisNetPlan, demand.getOfferedTraffic(), demand.getAttributes());
            }

            updateNetPlanView();
        } catch (Throwable ex) {
            getDesign().assignFrom(aux_netPlan);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void moveNode(long node, Point2D pos) {
        if (!isEditable()) throw new UnsupportedOperationException("Not supported");

        TableModel nodeTableModel = netPlanViewTable.get(NetworkElementType.NODE).getModel();
        int numRows = nodeTableModel.getRowCount();
        for (int row = 0; row < numRows; row++) {
            if ((long) nodeTableModel.getValueAt(row, 0) == node) {
                nodeTableModel.setValueAt(pos.getX(), row, AdvancedJTable_node.COLUMN_XCOORD);
                nodeTableModel.setValueAt(pos.getY(), row, AdvancedJTable_node.COLUMN_YCOORD);
            }
        }
    }

    @Override
    public void removeLink(long link) {
        if (!isEditable()) throw new UnsupportedOperationException("Not supported");

        NetPlan netPlan = getDesign();
        if (netPlan.getLinkFromId(link).getLayer().equals(getDesign().getNetworkLayerDefault())) {
            getTopologyPanel().getCanvas().removeLink(netPlan.getLinkFromId(link));
            getTopologyPanel().getCanvas().refresh();
        }
        netPlan.getLinkFromId(link).remove();

        updateNetPlanView();
    }

    @Override
    public void removeNode(long node) {
        if (!isEditable()) throw new UnsupportedOperationException("Not supported");

        NetPlan netPlan = getDesign();
        getTopologyPanel().getCanvas().removeNode(netPlan.getNodeFromId(node));
        getTopologyPanel().getCanvas().refresh();
        netPlan.getNodeFromId(node).remove();
        updateNetPlanView();
    }

    @Override
    public void reset() {
        try {
            boolean reset = askForReset();
            if (!reset) return;

            reset_internal();
//            reportSelector.reset();
//            reportContainer.removeAll();
        } catch (Throwable ex) {
            ErrorHandling.addErrorOrException(ex, IGUINetworkViewer.class);
            ErrorHandling.showErrorDialog("Unable to reset");
        }
    }

    @Override
    public void resetView() {
        topologyPanel.getCanvas().resetPickedAndUserDefinedColorState();
        for (Entry<NetworkElementType, AdvancedJTableNetworkElement> entry : netPlanViewTable.entrySet()) {
            switch (entry.getKey()) {
                case DEMAND:
                    clearDemandSelection();
                    break;

                case MULTICAST_DEMAND:
                    clearMulticastDemandSelection();
                    break;

                case FORWARDING_RULE:
                    clearForwardingRuleSelection();
                    break;

                case LINK:
                    clearLinkSelection();
                    break;

                case NODE:
                    clearNodeSelection();
                    break;

                case PROTECTION_SEGMENT:
                    clearProtectionSegmentSelection();
                    break;

                case ROUTE:
                    clearRouteSelection();
                    break;

                case MULTICAST_TREE:
                    clearMulticastTreeSelection();
                    break;

                case SRG:
                    clearSRGSelection();
                    break;

                default:
                    break;
            }
        }
    }

    public void showDemand(long demandId) {
        NetPlan netPlan = getDesign();
        NetworkLayer layer = netPlan.getDemandFromId(demandId).getLayer();
        selectNetPlanViewItem(layer.getId(), NetworkElementType.DEMAND, demandId);
        Demand demand = netPlan.getDemandFromId(demandId);

        Map<Node, Color> nodes = new HashMap<Node, Color>();
        nodes.put(demand.getIngressNode(), COLOR_INITIALNODE);
        nodes.put(demand.getEgressNode(), COLOR_ENDNODE);
        Map<Link, Pair<Color, Boolean>> links = new HashMap<Link, Pair<Color, Boolean>>();

        DoubleMatrix1D x_e = netPlan.getMatrixDemand2LinkTrafficCarried(layer).viewRow(demand.getIndex()).copy();
        for (int e = 0; e < x_e.size(); e++)
            if (x_e.get(e) > 0) {
                links.put(netPlan.getLink(e, layer), Pair.of(Color.BLUE, false));
            }
        topologyPanel.getCanvas().showAndPickNodesAndLinks(nodes, links);
        topologyPanel.getCanvas().refresh();
    }

    @Override
    public void showMulticastDemand(long demandId) {
        NetPlan netPlan = getDesign();
        MulticastDemand demand = netPlan.getMulticastDemandFromId(demandId);
        NetworkLayer layer = demand.getLayer();
        selectNetPlanViewItem(layer.getId(), NetworkElementType.MULTICAST_DEMAND, demandId);

        Map<Node, Color> nodes = new HashMap<Node, Color>();
        nodes.put(demand.getIngressNode(), COLOR_INITIALNODE);
        for (Node n : demand.getEgressNodes()) nodes.put(n, COLOR_ENDNODE);
        Map<Link, Pair<Color, Boolean>> links = new HashMap<Link, Pair<Color, Boolean>>();

        DoubleMatrix1D x_e = netPlan.getMatrixMulticastDemand2LinkTrafficCarried(layer).viewRow(demand.getIndex()).copy();
        for (int e = 0; e < x_e.size(); e++)
            if (x_e.get(e) > 0) links.put(netPlan.getLinkFromId(e), Pair.of(Color.BLUE, false));
        topologyPanel.getCanvas().showAndPickNodesAndLinks(nodes, links);
        topologyPanel.getCanvas().refresh();
    }

    @Override
    public void showForwardingRule(Pair<Integer, Integer> demandLink) {
        NetPlan netPlan = getDesign();
        Demand demand = netPlan.getDemand(demandLink.getFirst());
        Link link = netPlan.getLink(demandLink.getSecond());
        NetworkLayer layer = demand.getLayer();
        selectNetPlanViewItem(layer.getId(), NetworkElementType.FORWARDING_RULE, Pair.of(demand.getIndex(), link.getIndex()));

        Map<Node, Color> nodes = new HashMap<Node, Color>();
        nodes.put(demand.getIngressNode(), COLOR_INITIALNODE);
        nodes.put(demand.getEgressNode(), COLOR_ENDNODE);
        Map<Link, Pair<Color, Boolean>> links = new HashMap<Link, Pair<Color, Boolean>>();
        links.put(link, Pair.of(Color.BLUE, false));
        topologyPanel.getCanvas().showAndPickNodesAndLinks(nodes, links);
        topologyPanel.getCanvas().refresh();
    }

    @Override
    public void showLink(long linkId) {
        NetPlan netPlan = getDesign();
        Link link = netPlan.getLinkFromId(linkId);
        selectNetPlanViewItem(link.getLayer().getId(), NetworkElementType.LINK, linkId);

        topologyPanel.getCanvas().showNode(link.getOriginNode(), COLOR_INITIALNODE);
        topologyPanel.getCanvas().showNode(link.getDestinationNode(), COLOR_ENDNODE);

        topologyPanel.getCanvas().showLink(link, link.isUp() ? Color.BLUE : Color.RED, false);
        topologyPanel.getCanvas().refresh();
    }

    @Override
    public void showNode(long nodeId) {
        selectNetPlanViewItem(getDesign().getNetworkLayerDefault().getId(), NetworkElementType.NODE, nodeId);

        topologyPanel.getCanvas().showNode(getDesign().getNodeFromId(nodeId), Color.BLUE);
        topologyPanel.getCanvas().refresh();
    }

    @Override
    public void showProtectionSegment(long segmentId) {
        NetPlan netPlan = getDesign();
        ProtectionSegment segment = netPlan.getProtectionSegmentFromId(segmentId);
        selectNetPlanViewItem(segment.getLayer().getId(), NetworkElementType.PROTECTION_SEGMENT, segmentId);
        Map<Link, Pair<Color, Boolean>> res = new HashMap<Link, Pair<Color, Boolean>>();
        for (Link e : segment.getSeqLinks()) res.put(e, Pair.of(Color.YELLOW, false));
        topologyPanel.getCanvas().showAndPickNodesAndLinks(null, res);
        topologyPanel.getCanvas().refresh();
    }

    @Override
    public void showRoute(long routeId) // yellow segment link not used, orange segment link used, blue not segment link used. The same for initial state, in dashed
    {
        NetPlan netPlan = getDesign();
        Route route = netPlan.getRouteFromId(routeId);
        NetworkLayer layer = route.getLayer();
        selectNetPlanViewItem(layer.getId(), NetworkElementType.ROUTE, routeId);

        NetPlan initialState = getInitialDesign();
        Map<Link, Pair<Color, Boolean>> coloredLinks = new HashMap<Link, Pair<Color, Boolean>>();
        if (inOnlineSimulationMode() && (showInitialPlan != null) && showInitialPlan.isSelected()) {
            Route initialRoute = initialState.getRouteFromId(route.getId());
            if (initialRoute != null) {
                for (ProtectionSegment s : initialRoute.getPotentialBackupProtectionSegments())
                    for (Link e : s.getSeqLinks())
                        if (netPlan.getLinkFromId(e.getId()) != null)
                            coloredLinks.put(netPlan.getLinkFromId(e.getId()), Pair.of(Color.YELLOW, true));
                for (Link linkOrSegment : initialRoute.getSeqLinksAndProtectionSegments())
                    if (linkOrSegment instanceof ProtectionSegment) {
                        for (Link e : ((ProtectionSegment) linkOrSegment).getSeqLinks())
                            if (netPlan.getLinkFromId(e.getId()) != null)
                                coloredLinks.put(netPlan.getLinkFromId(e.getId()), Pair.of(Color.ORANGE, true));
                    } else if (netPlan.getLinkFromId(linkOrSegment.getId()) != null)
                        coloredLinks.put(netPlan.getLinkFromId(linkOrSegment.getId()), Pair.of(Color.BLUE, true));
            }
        }
        for (ProtectionSegment s : route.getPotentialBackupProtectionSegments())
            for (Link e : s.getSeqLinks())
                coloredLinks.put(e, Pair.of(Color.YELLOW, false));
        for (Link linkOrSegment : route.getSeqLinksAndProtectionSegments())
            if (linkOrSegment instanceof ProtectionSegment) {
                for (Link e : ((ProtectionSegment) linkOrSegment).getSeqLinks())
                    coloredLinks.put(netPlan.getLinkFromId(e.getId()), Pair.of(Color.ORANGE, false));
            } else coloredLinks.put(linkOrSegment, Pair.of(Color.BLUE, false));
        topologyPanel.getCanvas().showAndPickNodesAndLinks(null, coloredLinks);
        topologyPanel.getCanvas().refresh();
    }

    @Override
    public void showMulticastTree(long treeId) {
        NetPlan netPlan = getDesign();
        MulticastTree tree = netPlan.getMulticastTreeFromId(treeId);
        NetworkLayer layer = tree.getLayer();
        selectNetPlanViewItem(layer.getId(), NetworkElementType.MULTICAST_TREE, treeId);

        NetPlan currentState = getDesign();
        NetPlan initialState = getInitialDesign();
        Map<Node, Color> coloredNodes = new HashMap<Node, Color>();
        Map<Link, Pair<Color, Boolean>> coloredLinks = new HashMap<Link, Pair<Color, Boolean>>();
        if (inOnlineSimulationMode() && (showInitialPlan != null) && showInitialPlan.isSelected()) {
            MulticastTree initialTree = initialState.getMulticastTreeFromId(treeId);
            if (initialTree != null)
                for (Link e : initialTree.getLinkSet())
                    if (currentState.getLinkFromId(e.getId()) != null)
                        coloredLinks.put(currentState.getLinkFromId(e.getId()), Pair.of(Color.BLUE, true));
        }
        for (Link e : tree.getLinkSet()) coloredLinks.put(e, Pair.of(Color.BLUE, false));
        coloredNodes.put(tree.getIngressNode(), COLOR_INITIALNODE);
        for (Node n : tree.getEgressNodes()) coloredNodes.put(n, COLOR_ENDNODE);
        topologyPanel.getCanvas().showAndPickNodesAndLinks(coloredNodes, coloredLinks);
        topologyPanel.getCanvas().refresh();
    }

    @Override
    public void showSRG(long srg) {
        showSRG(getDesign().getNetworkLayerDefault().getId(), srg);
    }

    @Override
    public void showSRG(long layer, long srg) {
        NetPlan netPlan = getDesign();
        selectNetPlanViewItem(layer, NetworkElementType.SRG, srg);

        Set<Node> nodeIds_thisSRG = netPlan.getSRGFromId(srg).getNodes();
        Set<Link> linkIds_thisSRG_thisLayer = netPlan.getSRGFromId(srg).getLinks(netPlan.getNetworkLayerFromId(layer));
        Map<Node, Color> nodeColors = new HashMap<Node, Color>();
        Map<Link, Pair<Color, Boolean>> linkColors = new HashMap<Link, Pair<Color, Boolean>>();
        for (Node n : nodeIds_thisSRG) nodeColors.put(n, Color.ORANGE);
        for (Link e : linkIds_thisSRG_thisLayer) linkColors.put(e, Pair.of(Color.ORANGE, false));

        topologyPanel.getCanvas().showAndPickNodesAndLinks(nodeColors, linkColors);
        topologyPanel.getCanvas().refresh();
    }

    @Override
    public synchronized void updateNetPlanView() {
        updateWarnings();

		/* Load current network state */
        NetPlan currentState = getDesign();
        NetworkLayer layer = currentState.getNetworkLayerDefault();
        currentState.checkCachesConsistency();

        final RoutingType routingType = currentState.getRoutingType();
        Component selectedTab = netPlanView.getSelectedComponent();
        netPlanView.removeAll();
        for (NetworkElementType elementType : Constants.NetworkElementType.values()) {
            if (routingType == RoutingType.SOURCE_ROUTING && elementType == NetworkElementType.FORWARDING_RULE)
                continue;
            if (routingType == RoutingType.HOP_BY_HOP_ROUTING && (elementType == NetworkElementType.PROTECTION_SEGMENT || elementType == NetworkElementType.ROUTE))
                continue;
            netPlanView.addTab(elementType == NetworkElementType.NETWORK ? "Network" : netPlanViewTable.get(elementType).getTabName(), netPlanViewTableComponent.get(elementType));
        }

        for (int tabId = 0; tabId < netPlanView.getTabCount(); tabId++) {
            if (netPlanView.getComponentAt(tabId).equals(selectedTab)) {
                netPlanView.setSelectedIndex(tabId);
                break;
            }
        }

        NetPlan initialState = null;
        if (showInitialPlan != null && getInitialDesign().getNetworkLayerFromId(layer.getId()) != null)
            initialState = getInitialDesign();

        currentState.checkCachesConsistency();

        for (AdvancedJTableNetworkElement table : netPlanViewTable.values())
            table.updateView(currentState, initialState);

        ((NetPlanViewTableComponent_layer) netPlanViewTableComponent.get(NetworkElementType.LAYER)).updateNetPlanView(currentState);
        ((NetPlanViewTableComponent_network) netPlanViewTableComponent.get(NetworkElementType.NETWORK)).updateNetPlanView(currentState);
    }

    @Override
    public void updateWarnings() {
        Map<String, String> net2planParameters = Configuration.getNet2PlanOptions();
        List<String> warnings = NetworkPerformanceMetrics.checkNetworkState(getDesign(), net2planParameters);
        String warningMsg = warnings.isEmpty() ? "Design is successfully completed!" : StringUtils.join(warnings, StringUtils.getLineSeparator());
        updateLog(warningMsg);
    }

    /**
     * Adds a new tab in the right panel at the last position.
     *
     * @param name Tab name
     * @param tab  Tab component
     * @return Tab position
     * @since 0.3.0
     */
    protected final int addTab(String name, JComponent tab) {
        return addTab(name, tab, -1);
    }

    /**
     * Adds a new tab in the right panel at the given position.
     *
     * @param name     Tab name
     * @param tab      Tab component
     * @param tabIndex Tab position (-1 means last position)
     * @return Tab position
     * @since 0.3.0
     */
    protected final int addTab(String name, JComponent tab, int tabIndex) {
        int numTabs = rightPane.getTabCount();
        if (numTabs == 9) throw new RuntimeException("A maximum of 9 tabs are allowed");

        if (tabIndex == -1) tabIndex = numTabs;
        rightPane.insertTab(name, null, tab, null, tabIndex);

        if (tabIndex <= viewNetPlanTabIndex) viewNetPlanTabIndex++;
        return tabIndex;
    }

    /**
     * Indicates whether or not traffic demands can be added to the current design
     * from an external file.
     *
     * @return {@code true} if it is allowed to load traffic demands. Otherwise, {@code false}.
     * @since 0.3.0
     */
    protected boolean allowLoadTrafficDemands() {
        return false;
    }

    /**
     * Indicates whether or not the initial {@code NetPlan} object is stored to be
     * compared with the current one (i.e. after some simulation steps).
     *
     * @return {@code true} if the initial {@code NetPlan} object is stored. Otherwise, {@code false}.
     * @since 0.3.0
     */
    public boolean inOnlineSimulationMode() {
        return false;
    }

    /**
     * Asks user to confirm plugin reset.
     *
     * @return {@code true} if user confirms to reset the plugin, or {@code false} otherwise
     * @since 0.2.3
     */
    protected static boolean askForReset() {
        int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to reset? This will remove all unsaved data", "Reset", JOptionPane.YES_NO_OPTION);

        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Clears the current demand selection in the network state view.
     * <p>
     * <p><b>Important</b>: Always call the parent method.</p>
     *
     * @since 0.3.1
     */
    protected void clearDemandSelection() {
        JTable table = netPlanViewTable.get(NetworkElementType.DEMAND);
        table.clearSelection();
    }

    /**
     * Clears the current multicast demand selection in the network state view.
     * <p>
     * <p><b>Important</b>: Always call the parent method.</p>
     *
     * @since 0.3.1
     */
    protected void clearMulticastDemandSelection() {
        JTable table = netPlanViewTable.get(NetworkElementType.MULTICAST_DEMAND);
        table.clearSelection();
    }

    /**
     * Clears the current forwarding rule selection in the network state view.
     * <p>
     * <p><b>Important</b>: Always call the parent method.</p>
     *
     * @since 0.3.1
     */
    protected void clearForwardingRuleSelection() {
        JTable table = netPlanViewTable.get(NetworkElementType.FORWARDING_RULE);
        table.clearSelection();
    }

    /**
     * Clears the current link selection in the network state view.
     * <p>
     * <p><b>Important</b>: Always call the parent method.</p>
     *
     * @since 0.3.1
     */
    protected void clearLinkSelection() {
        JTable table = netPlanViewTable.get(NetworkElementType.LINK);
        table.clearSelection();
    }

    /**
     * Clears the current node selection in the network state view.
     * <p>
     * <p><b>Important</b>: Always call the parent method.</p>
     *
     * @since 0.3.1
     */
    protected void clearNodeSelection() {
        JTable table = netPlanViewTable.get(NetworkElementType.NODE);
        table.clearSelection();
    }

    /**
     * Clears the current proctection segment selection in the network state view.
     * <p>
     * <p><b>Important</b>: Always call the parent method.</p>
     *
     * @since 0.3.1
     */
    protected void clearProtectionSegmentSelection() {
        JTable table = netPlanViewTable.get(NetworkElementType.PROTECTION_SEGMENT);
        table.clearSelection();
    }

    /**
     * Clears the current route selection in the network state view.
     * <p>
     * <p><b>Important</b>: Always call the parent method.</p>
     *
     * @since 0.3.1
     */
    protected void clearRouteSelection() {
        JTable table = netPlanViewTable.get(NetworkElementType.ROUTE);
        table.clearSelection();
    }

    /**
     * Clears the current multicast tree selection in the network state view.
     * <p>
     * <p><b>Important</b>: Always call the parent method.</p>
     *
     * @since 0.3.1
     */
    protected void clearMulticastTreeSelection() {
        JTable table = netPlanViewTable.get(NetworkElementType.MULTICAST_TREE);
        table.clearSelection();
    }

    /**
     * Clears the current SRG selection in the network state view.
     * <p>
     * <p><b>Important</b>: Always call the parent method.</p>
     *
     * @since 0.3.1
     */
    protected void clearSRGSelection() {
        JTable table = netPlanViewTable.get(NetworkElementType.SRG);
        table.clearSelection();
    }

    /**
     * Allows customizing the 'demand' tab in the network state viewer.
     *
     * @param demandTableView Table with per-demand information
     * @return Component to be included in the 'demand' tab
     * @since 0.3.1
     */
    protected JComponent configureDemandTabView(JScrollPane demandTableView) {
        return demandTableView;
    }

    /**
     * Allows customizing the 'multicast demand' tab in the network state viewer.
     *
     * @param demandTableView Table with per-multicast demand information
     * @return Component to be included in the 'multicast demand' tab
     * @since 0.3.1
     */
    protected JComponent configureMulticastDemandTabView(JScrollPane multicastDemandTableView) {
        return multicastDemandTableView;
    }

    /**
     * Allows customizing the 'forwarding rule' tab in the network state viewer.
     *
     * @param forwadingRuleTableView Table with per-forwarding rule information
     * @return Component to be included in the 'forwarding rule' tab
     * @since 0.3.1
     */
    protected JComponent configureForwardingRuleTabView(JScrollPane forwadingRuleTableView) {
        return forwadingRuleTableView;
    }

    /**
     * Allows to include a custom panel in the left-bottom corner of the window,
     * just below the topologyPanel panel.
     *
     * @return A panel to be included in the left-bottom corner of the window
     * @since 0.3.0
     */
    protected JPanel configureLeftBottomPanel() {
        return null;
    }

    /**
     * Allows customizing the 'link' tab in the network state viewer.
     *
     * @param linkTableView Table with per-link information
     * @return Component to be included in the 'link' tab
     * @since 0.3.1
     */
    protected JComponent configureLinkTabView(JScrollPane linkTableView) {
        return linkTableView;
    }

    private void configureNetPlanView() {
        netPlanViewTable = new EnumMap<NetworkElementType, AdvancedJTableNetworkElement>(NetworkElementType.class);
        netPlanViewTableDecorator = new EnumMap<NetworkElementType, FixedColumnDecorator>(NetworkElementType.class);
        netPlanViewTableComponent = new EnumMap<NetworkElementType, JComponent>(NetworkElementType.class);


        allowDocumentUpdate = isEditable();
        netPlanViewTable.put(NetworkElementType.NODE, new AdvancedJTable_node(this, topologyPanel));
        netPlanViewTable.put(NetworkElementType.LINK, new AdvancedJTable_link(this, topologyPanel));
        netPlanViewTable.put(NetworkElementType.DEMAND, new AdvancedJTable_demand(this, topologyPanel));
        netPlanViewTable.put(NetworkElementType.ROUTE, new AdvancedJTable_route(this, topologyPanel));
        netPlanViewTable.put(NetworkElementType.PROTECTION_SEGMENT, new AdvancedJTable_segment(this, topologyPanel));
        netPlanViewTable.put(NetworkElementType.FORWARDING_RULE, new AdvancedJTable_forwardingRule(this, topologyPanel));
        netPlanViewTable.put(NetworkElementType.MULTICAST_DEMAND, new AdvancedJTable_multicastDemand(this, topologyPanel));
        netPlanViewTable.put(NetworkElementType.MULTICAST_TREE, new AdvancedJTable_multicastTree(this, topologyPanel));
        netPlanViewTable.put(NetworkElementType.SRG, new AdvancedJTable_srg(this, topologyPanel));
        netPlanViewTable.put(NetworkElementType.LAYER, new AdvancedJTable_layer(this, topologyPanel));

        netPlanView = new JTabbedPane();

        for (NetworkElementType elementType : Constants.NetworkElementType.values()) {
            if (elementType == NetworkElementType.NETWORK) {
                netPlanViewTableComponent.put(elementType, new NetPlanViewTableComponent_network(this, (AdvancedJTable_layer) netPlanViewTable.get(NetworkElementType.LAYER)));
            } else if (elementType == NetworkElementType.LAYER) {
                netPlanViewTableComponent.put(elementType, new NetPlanViewTableComponent_layer(this, (AdvancedJTable_layer) netPlanViewTable.get(NetworkElementType.LAYER)));
            } else {
                JScrollPane scrollPane = new JScrollPane(netPlanViewTable.get(elementType));
                ScrollPaneLayout layout = new FullScrollPaneLayout();
                scrollPane.setLayout(layout);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                FixedColumnDecorator decorator = new FixedColumnDecorator(scrollPane, ((AdvancedJTableNetworkElement) netPlanViewTable.get(elementType)).getNumFixedLeftColumnsInDecoration());
                decorator.getFixedTable().getColumnModel().getColumn(0).setMinWidth(50);
                netPlanViewTableDecorator.put(elementType, decorator);
                netPlanViewTableComponent.put(elementType, scrollPane);
            }
        }

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(netPlanView, BorderLayout.CENTER);

        if (inOnlineSimulationMode()) {
            showInitialPlan = new JCheckBox("Toggle show/hide planning information", true);
            showInitialPlan.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    RowFilter<TableModel, Integer> rowFilter = e.getStateChange() == ItemEvent.SELECTED ? null : new RowFilter<TableModel, Integer>() {
                        @Override
                        public boolean include(RowFilter.Entry<? extends TableModel, ? extends Integer> entry) {
                            if (entry.getIdentifier() == 0) return true;

                            if (entry.getValue(0) instanceof CurrentAndPlannedStateTableCellValue)
                                return ((CurrentAndPlannedStateTableCellValue) entry.getValue(0)).value != null;
                            else
                                return entry.getValue(0) != null;
                        }
                    };

                    for (NetworkElementType elementType : Constants.NetworkElementType.values()) {
                        if (elementType == NetworkElementType.NETWORK) continue;

                        ((TableRowSorter) netPlanViewTable.get(elementType).getRowSorter()).setRowFilter(rowFilter);
                    }
                    getTopologyPanel().getCanvas().refresh();
                }
            });

            showInitialPlan.setSelected(false);
            pane.add(showInitialPlan, BorderLayout.NORTH);
        }

        addTab(isEditable() ? "View/edit network state" : "View network state", pane);
        viewNetPlanTabIndex = 0;
    }

    /**
     * Allows customizing the 'node' tab in the network state viewer.
     *
     * @param nodeTableView Table with per-node information
     * @return Component to be included in the 'node' tab
     * @since 0.3.1
     */
    protected JComponent configureNodeTabView(JScrollPane nodeTableView) {
        return nodeTableView;
    }

    /**
     * Allows customizing the 'protection segment' tab in the network state viewer.
     *
     * @param segmentTableView Table with per-protection-segment information
     * @return Component to be included in the 'protection segment' tab
     * @since 0.3.1
     */
    protected JComponent configureProtectionSegmentTabView(JScrollPane segmentTableView) {
        return segmentTableView;
    }

    private void configureReportPane() {
        reportController = new ThreadExecutionController(this);

        File REPORTS_DIRECTORY = new File(CURRENT_DIR + SystemUtils.getDirectorySeparator() + "workspace");
        REPORTS_DIRECTORY = REPORTS_DIRECTORY.isDirectory() ? REPORTS_DIRECTORY : CURRENT_DIR;
        ParameterValueDescriptionPanel reportParameters = new ParameterValueDescriptionPanel();
        reportSelector = new RunnableSelector("Report", null, IReport.class, REPORTS_DIRECTORY, reportParameters);
        reportPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        reportContainer = new JTabbedPane();

        final JPanel pnl_buttons = new JPanel(new WrapLayout());

        reportContainer.setVisible(false);

        addKeyCombinationAction("Close selected report", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int tab = reportContainer.getSelectedIndex();
                if (tab == -1) return;

                reportContainer.remove(tab);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));

        addKeyCombinationAction("Close all reports", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reportContainer.removeAll();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));

        reportContainer.addContainerListener(new ContainerListener() {
            @Override
            public void componentAdded(ContainerEvent e) {
                reportContainer.setVisible(true);
                reportPane.setDividerLocation(0.5);

                for (Component component : pnl_buttons.getComponents())
                    if (component == closeAllReports)
                        return;

                pnl_buttons.add(closeAllReports);
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                if (reportContainer.getTabCount() == 0) {
                    reportContainer.setVisible(false);

                    for (Component component : pnl_buttons.getComponents())
                        if (component == closeAllReports)
                            pnl_buttons.remove(closeAllReports);
                }
            }
        });

        JButton btn_show = new JButton("Show");
        btn_show.setToolTipText("Show the report");
        btn_show.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reportController.execute();
            }
        });

        closeAllReports = new JButton("Close all");
        closeAllReports.setToolTipText("Close all reports");
        closeAllReports.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reportContainer.removeAll();
            }
        });

        reportContainer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int tabNumber = reportContainer.getUI().tabForCoordinate(reportContainer, e.getX(), e.getY());

                if (tabNumber >= 0) {
                    Rectangle rect = ((TabIcon) reportContainer.getIconAt(tabNumber)).getBounds();
                    if (rect.contains(e.getX(), e.getY())) reportContainer.removeTabAt(tabNumber);
                }
            }
        });

        pnl_buttons.add(btn_show);

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(reportSelector, BorderLayout.CENTER);
        pane.add(pnl_buttons, BorderLayout.SOUTH);
        reportPane.setTopComponent(pane);

        reportPane.setBottomComponent(reportContainer);
        reportPane.addPropertyChangeListener(new ProportionalResizeJSplitPaneListener());
        reportPane.setResizeWeight(0.5);

        addTab("View reports", reportPane);
    }

    /**
     * Allows customizing the 'route' tab in the network state viewer.
     *
     * @param routeTableView Table with per-route information
     * @return Component to be included in the 'route' tab
     * @since 0.3.1
     */
    protected JComponent configureRouteTabView(JScrollPane routeTableView) {
        return routeTableView;
    }

    /**
     * Allows customizing the 'multicast tree' tab in the network state viewer.
     *
     * @param multicastTreeTableView Table with per-route information
     * @return Component to be included in the 'multicast tree' tab
     * @since 0.3.1
     */
    protected JComponent configureMulticastTreeTabView(JScrollPane multicastTreeTableView) {
        return multicastTreeTableView;
    }

    /**
     * Allows customizing the 'srg' tab in the network state viewer.
     *
     * @param srgTableView Table with per-SRG information
     * @return Component to be included in the 'srg' tab
     * @since 0.3.1
     */
    protected JComponent configureSRGTabView(JScrollPane srgTableView) {
        return srgTableView;
    }

    /**
     * Allows to include custom code after initializing the topologyPanel panel (i.e. add new plugins).
     *
     * @since 0.3.0
     */
    protected void configureTopologyPanel() {
        popupPlugin = new PopupMenuPlugin(this);

        getTopologyPanel().addPlugin(new PanGraphPlugin(this, MouseEvent.BUTTON1_MASK));
        if (isEditable() && getTopologyPanel().getCanvas() instanceof JUNGCanvas)
            getTopologyPanel().addPlugin(new AddLinkGraphPlugin(this, MouseEvent.BUTTON1_MASK, MouseEvent.BUTTON1_MASK | MouseEvent.SHIFT_MASK));
        getTopologyPanel().addPlugin(popupPlugin);
        if (isEditable())
            getTopologyPanel().addPlugin(new MoveNodePlugin(this, MouseEvent.BUTTON1_MASK | MouseEvent.CTRL_MASK));
    }

    /**
     * Returns a reference to the topologyPanel panel.
     *
     * @return Reference to the topologyPanel panel
     * @since 0.3.0
     */
    public final TopologyPanel getTopologyPanel() {
        return topologyPanel;
    }

    /**
     * Allows to include custom code after resetting the topologyPanel panel.
     *
     * @since 0.3.0
     */
    protected void reset_internal() {
        loadDesign(new NetPlan());
    }

    /**
     * Shows the tab corresponding associated to a network element.
     *
     * @param type   Network element type
     * @param itemId Item identifier (if null, it will just show the tab)
     * @since 0.3.0
     */
    protected void selectNetPlanViewItem(NetworkElementType type, Object itemId) {
        selectNetPlanViewItem(getDesign().getNetworkLayerDefault().getId(), type, itemId);
    }

    /**
     * Shows the tab corresponding associated to a network element.
     *
     * @param layerId Layer identifier
     * @param type    Network element type
     * @param itemId  Item identifier (if null, it will just show the tab)
     * @since 0.3.0
     */
    protected void selectNetPlanViewItem(long layer, NetworkElementType type, Object itemId) {
        topologyPanel.selectLayer(layer);
        showTab(viewNetPlanTabIndex);

        AdvancedJTableNetworkElement table = netPlanViewTable.get(type);
        int tabIndex = netPlanView.getSelectedIndex();
        int col = 0;
        if (netPlanView.getTitleAt(tabIndex).equals(type == NetworkElementType.NETWORK ? "Network" : table.getTabName())) {
            col = table.getSelectedColumn();
            if (col == -1) col = 0;
        } else {
            netPlanView.setSelectedComponent(netPlanViewTableComponent.get(type));
        }

        if (itemId == null) {
            table.clearSelection();
            return;
        }

        TableModel model = table.getModel();
        int numRows = model.getRowCount();
        for (int row = 0; row < numRows; row++) {
            Object obj = model.getValueAt(row, 0);
            if (obj == null) continue;

            if (type == NetworkElementType.FORWARDING_RULE) {
                obj = Pair.of(Integer.parseInt(model.getValueAt(row, 1).toString().split(" ")[0]), Integer.parseInt(model.getValueAt(row, 2).toString().split(" ")[0]));
                if (!obj.equals(itemId)) continue;
            } else if ((long) obj != (long) itemId) {
                continue;
            }

            row = table.convertRowIndexToView(row);
            table.changeSelection(row, col, false, true);
            return;
        }

        throw new RuntimeException(type + " " + itemId + " does not exist");
    }

    /**
     * Allows to include actions when a {@code NetPlan} object is loaded.
     *
     * @param netPlan {@code NetPlan} object
     * @since 0.3.0
     */
    protected void setNetPlan(NetPlan netPlan) {
        currentNetPlan = netPlan;
        if (inOnlineSimulationMode()) initialNetPlan = currentNetPlan.copy();
    }

    /**
     * Shows the {@code NetPlan} view, moving to the corresponding tab.
     *
     * @since 0.3.0
     */
    protected final void showNetPlanView() {
        netPlanView.setSelectedIndex(0);
        showTab(viewNetPlanTabIndex);
    }

    /**
     * Shows the desired tab in {@code NetPlan} view.
     *
     * @param tabIndex Tab index
     * @since 0.3.0
     */
    protected final void showTab(int tabIndex) {
        if (tabIndex < rightPane.getTabCount() && rightPane.getSelectedIndex() != tabIndex) {
            rightPane.setSelectedIndex(tabIndex);
            rightPane.requestFocusInWindow();
        }
    }


    /**
     * Allows to show some custom log messages.
     *
     * @param text Log message
     * @since 0.3.0
     */
    protected void updateLog(String text) {
    }

    private class AddLinkAction extends AbstractAction {
        private final long layer;
        private final long originNode;
        private final long destinationNode;

        public AddLinkAction(String name, long layer, long originNode, long destinationNode) {
            super(name);
            this.layer = layer;
            this.originNode = originNode;
            this.destinationNode = destinationNode;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            addLink(layer, originNode, destinationNode);
        }
    }

    private class AddLinkBidirectionalAction extends AbstractAction {
        private final long layer;
        private final long originNode;
        private final long destinationNode;

        public AddLinkBidirectionalAction(String name, long layer, long originNode, long destinationNode) {
            super(name);
            this.layer = layer;
            this.originNode = originNode;
            this.destinationNode = destinationNode;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            addLinkBidirectional(layer, originNode, destinationNode);
        }
    }

    private class AddNodeAction extends AbstractAction {
        private final Point2D pos;

        public AddNodeAction(String name, Point2D pos) {
            super(name);
            this.pos = pos;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            addNode(pos);
        }
    }

    public static class ColumnComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            String oo1 = o1;
            String oo2 = o2;

            int pos1 = oo1.indexOf(" (");
            if (pos1 != -1) oo1 = oo1.substring(0, pos1);

            int pos2 = oo2.indexOf(" (");
            if (pos2 != -1) oo2 = oo2.substring(0, pos2);

            double d1 = Double.MAX_VALUE;
            try {
                d1 = Double.parseDouble(oo1);
            } catch (Throwable e) {
            }

            double d2 = Double.MAX_VALUE;
            try {
                d2 = Double.parseDouble(oo2);
            } catch (Throwable e) {
            }

            if (d1 != Double.MAX_VALUE && d2 != Double.MAX_VALUE) {
                int out = Double.compare(d1, d2);
                if (out != 0) return out;
            }

            return o1.compareTo(o2);
        }
    }

    public abstract class DocumentAdapter implements DocumentListener {
        @Override
        public void changedUpdate(DocumentEvent e) {
            processEvent(e);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            processEvent(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            processEvent(e);
        }

        private void processEvent(DocumentEvent e) {
            if (!allowDocumentUpdate) return;

            Document doc = e.getDocument();
            try {
                updateInfo(doc.getText(0, doc.getLength()));
            } catch (BadLocationException ex) {
            }
        }

        protected abstract void updateInfo(String text);
    }

    private class RemoveLinkAction extends AbstractAction {
        private final long link;

        public RemoveLinkAction(String name, long link) {
            super(name);
            this.link = link;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            removeLink(link);
        }
    }

    private class RemoveNodeAction extends AbstractAction {
        private final long node;

        public RemoveNodeAction(String name, long node) {
            super(name);
            this.node = node;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            removeNode(node);
        }
    }

    public static class SingleElementAttributeEditor extends MouseAdapter {
        private final INetworkCallback callback;
        private final NetworkElementType type;

        public SingleElementAttributeEditor(final INetworkCallback callback, final NetworkElementType type) {
            this.callback = callback;
            this.type = type;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                final JTable table = (JTable) e.getSource();
                final NetPlan netPlan = callback.getDesign();

                JPopupMenu popup = new JPopupMenu();

                JMenuItem addAttribute = new JMenuItem("Add/edit attribute");
                addAttribute.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JTextField txt_key = new JTextField(20);
                        JTextField txt_value = new JTextField(20);

                        JPanel pane = new JPanel();
                        pane.add(new JLabel("Attribute: "));
                        pane.add(txt_key);
                        pane.add(Box.createHorizontalStrut(15));
                        pane.add(new JLabel("Value: "));
                        pane.add(txt_value);

                        while (true) {
                            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter an attribute name and its value", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return;

                            try {
                                if (txt_key.getText().isEmpty())
                                    throw new Exception("Please, insert an attribute name");

                                switch (type) {
                                    case NETWORK:
                                        netPlan.setAttribute(txt_key.getText(), txt_value.getText());
                                        break;

                                    case LAYER:
                                        netPlan.getNetworkLayerDefault().setAttribute(txt_key.getText(), txt_value.getText());
                                        break;

                                    default:
                                        ErrorHandling.showErrorDialog("Bad", "Internal error");
                                        return;
                                }

                                callback.updateNetPlanView();
                                return;
                            } catch (Exception ex) {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error adding/editing attribute");
                            }
                        }
                    }
                });

                popup.add(addAttribute);

                int numAttributes;
                switch (type) {
                    case NETWORK:
                        numAttributes = netPlan.getAttributes().size();
                        break;

                    case LAYER:
                        numAttributes = netPlan.getNetworkLayerDefault().getAttributes().size();
                        break;

                    default:
                        ErrorHandling.showErrorDialog("Bad", "Internal error");
                        return;
                }

                if (numAttributes > 0) {
                    JMenuItem removeAttribute = new JMenuItem("Remove attribute");

                    removeAttribute.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                String[] attributeList;

                                switch (type) {
                                    case NETWORK:
                                        attributeList = StringUtils.toArray(netPlan.getAttributes().keySet());
                                        break;

                                    case LAYER:
                                        attributeList = StringUtils.toArray(netPlan.getNetworkLayerDefault().getAttributes().keySet());
                                        break;

                                    default:
                                        ErrorHandling.showErrorDialog("Bad", "Internal error");
                                        return;
                                }

                                if (attributeList.length == 0) throw new Exception("No attribute to remove");

                                Object out = JOptionPane.showInputDialog(null, "Please, select an attribute to remove", "Remove attribute", JOptionPane.QUESTION_MESSAGE, null, attributeList, attributeList[0]);
                                if (out == null) return;

                                String attributeToRemove = out.toString();

                                switch (type) {
                                    case NETWORK:
                                        netPlan.removeAttribute(attributeToRemove);
                                        break;

                                    case LAYER:
                                        netPlan.getNetworkLayerDefault().removeAttribute(attributeToRemove);
                                        break;

                                    default:
                                        ErrorHandling.showErrorDialog("Bad", "Internal error");
                                        return;
                                }

                                callback.updateNetPlanView();
                            } catch (Exception ex) {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing attribute");
                            }
                        }
                    });

                    popup.add(removeAttribute);

                    JMenuItem removeAttributes = new JMenuItem("Remove all attributes");

                    removeAttributes.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                switch (type) {
                                    case NETWORK:
                                        netPlan.setAttributeMap(new HashMap<String, String>());
                                        break;

                                    case LAYER:
                                        netPlan.getNetworkLayerDefault().setAttributeMap(new HashMap<String, String>());
                                        break;

                                    default:
                                        ErrorHandling.showErrorDialog("Bad", "Internal error");
                                        return;
                                }

                                callback.updateNetPlanView();
                            } catch (Exception ex) {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing attributes");
                            }
                        }
                    });

                    popup.add(removeAttributes);
                }

                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private class SwitchTabAction extends AbstractAction {
        private final int tabId;

        public SwitchTabAction(int tabId) {
            this.tabId = tabId;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showTab(tabId);
        }
    }
}
