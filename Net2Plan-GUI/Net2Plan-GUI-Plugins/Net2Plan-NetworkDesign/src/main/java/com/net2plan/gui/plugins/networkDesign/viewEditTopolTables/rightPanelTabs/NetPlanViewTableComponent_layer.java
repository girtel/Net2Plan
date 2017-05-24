/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/
package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.rightPanelTabs;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.ParamValueTable;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_layer;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.ColumnHeaderToolTips;
import com.net2plan.gui.utils.FullScrollPaneLayout;
import com.net2plan.gui.utils.ProportionalResizeJSplitPaneListener;
import com.net2plan.gui.utils.TableCursorNavigation;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.libraries.GraphTheoryMetrics;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.TrafficComputationEngine;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import net.miginfocom.swing.MigLayout;

public class NetPlanViewTableComponent_layer extends JPanel
{
    private final static String[] layerSummaryTableHeader = StringUtils.arrayOf("Metric", "Value");
    private final static String[] attributeTableHeader = StringUtils.arrayOf("Attribute", "Value");
    private final static String[] attributeTableTips = attributeTableHeader;
    private final static String[] tagTableHeader = StringUtils.arrayOf("Tag");
    private final static String[] tagTableTips = StringUtils.arrayOf("Name of the tag");
    private JTable layerAttributeTable;
    private JTable layerTagTable;
    private JTextField txt_layerName, txt_layerLinkCapacityUnits, txt_layerDemandTrafficUnits;
    private JTextArea txt_layerDescription;
    private JRadioButton sourceRoutingActivated, hopByHopRoutingActivated;
    private ButtonGroup routingSchemes;
    private JPanel layerMetricsInfo;
    private ParamValueTable[] layerSummaryTables;
    private JButton forceUpdate;
    private final AdvancedJTable_layer layerTable;
    private boolean insideUpdateView;

    private final GUINetworkDesign networkViewer;

    public NetPlanViewTableComponent_layer(final GUINetworkDesign networkViewer, final AdvancedJTable_layer layerTable)
    {
        super(new MigLayout("", "[grow]", "[][][][][][grow]"));
        this.layerTable = layerTable;
        this.networkViewer = networkViewer;

        txt_layerName = new JTextField();
        txt_layerDescription = new JTextArea();
        txt_layerDescription.setFont(new JLabel().getFont());
        txt_layerDescription.setLineWrap(true);
        txt_layerDescription.setWrapStyleWord(true);
        txt_layerDemandTrafficUnits = new JTextField();
        txt_layerLinkCapacityUnits = new JTextField();
        sourceRoutingActivated = new JRadioButton("Source routing", false);
        sourceRoutingActivated.setEnabled(networkViewer.getVisualizationState().isNetPlanEditable());
        hopByHopRoutingActivated = new JRadioButton("Hop-by-hop routing", false);
        hopByHopRoutingActivated.setEnabled(networkViewer.getVisualizationState().isNetPlanEditable());

        if (networkViewer.getVisualizationState().isNetPlanEditable())
        {
            ItemListener itemRoutingTypeListener = new ItemListener()
            {
                @Override
                public void itemStateChanged(ItemEvent event)
                {
                    JRadioButton button = (JRadioButton) event.getSource();
                    int state = event.getStateChange();

                    NetPlan netPlan = networkViewer.getDesign();
                    RoutingType previousRoutingType = netPlan.getRoutingType();

                    if (button == sourceRoutingActivated && state == ItemEvent.SELECTED)
                    {
                        netPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
                        if (previousRoutingType != RoutingType.SOURCE_ROUTING)
                        {
                            networkViewer.getVisualizationState().resetPickedState();
                            networkViewer.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LAYER));
                            networkViewer.addNetPlanChange();
                        }
                    }
                    else if (button == hopByHopRoutingActivated && state == ItemEvent.SELECTED)
                    {
                        netPlan.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
                        if (previousRoutingType != RoutingType.HOP_BY_HOP_ROUTING)
                        {
                            networkViewer.getVisualizationState().resetPickedState();
                            networkViewer.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LAYER));
                            networkViewer.addNetPlanChange();
                        }
                    }
                }
            };

            sourceRoutingActivated.addItemListener(itemRoutingTypeListener);
            hopByHopRoutingActivated.addItemListener(itemRoutingTypeListener);
        }
        routingSchemes = new ButtonGroup();
        routingSchemes.add(sourceRoutingActivated);
        routingSchemes.add(hopByHopRoutingActivated);

        txt_layerName.setEditable(networkViewer.getVisualizationState().isNetPlanEditable());
        txt_layerDescription.setEditable(networkViewer.getVisualizationState().isNetPlanEditable());
        txt_layerDemandTrafficUnits.setEditable(networkViewer.getVisualizationState().isNetPlanEditable());
        txt_layerLinkCapacityUnits.setEditable(networkViewer.getVisualizationState().isNetPlanEditable());
        if (networkViewer.getVisualizationState().isNetPlanEditable())
        {
            txt_layerName.getDocument().addDocumentListener(new DocumentAdapter(networkViewer)
            {
                @Override
                protected void updateInfo(String text)
                {
//					allowDocumentUpdate = false;
                    NetworkLayer layer = networkViewer.getDesign().getNetworkLayerDefault();
                    JTable table = (JTable) layerTable;
                    TableModel model = table.getModel();
                    int numRows = model.getRowCount();
                    for (int row = 0; row < numRows; row++)
                    {
                        if ((Long) model.getValueAt(row, AdvancedJTable_layer.COLUMN_ID) == layer.getId())
                        {
                            layer.setName(text);
                            model.setValueAt(text, row, AdvancedJTable_layer.COLUMN_NAME);
                        }
                    }
//					allowDocumentUpdate = isEditable();
                }
            });

            txt_layerName.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if (!insideUpdateView)
                    {
                        networkViewer.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LAYER));
                        networkViewer.addNetPlanChange();
                    }
                }
            });

            txt_layerLinkCapacityUnits.getDocument().addDocumentListener(new DocumentAdapter(networkViewer)
            {
                @Override
                protected void updateInfo(String text)
                {
//					allowDocumentUpdate = false;

                    NetworkLayer layer = networkViewer.getDesign().getNetworkLayerDefault();
                    JTable table = (JTable) layerTable;
                    TableModel model = table.getModel();
                    int numRows = model.getRowCount();
                    for (int row = 0; row < numRows; row++)
                    {
                        if ((Long) model.getValueAt(row, AdvancedJTable_layer.COLUMN_ID) == layer.getId())
                        {
                            final String previousValue = model.getValueAt(row, AdvancedJTable_layer.COLUMN_LINKCAPUNITS).toString();
                            model.setValueAt(text, row, AdvancedJTable_layer.COLUMN_LINKCAPUNITS);
                            if (!text.equals(model.getValueAt(row, AdvancedJTable_layer.COLUMN_LINKCAPUNITS).toString()))
                            {
                                final DocumentListener me = this;

                                SwingUtilities.invokeLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        txt_layerLinkCapacityUnits.getDocument().removeDocumentListener(me);
                                        txt_layerLinkCapacityUnits.setText(previousValue);
                                        txt_layerLinkCapacityUnits.getDocument().addDocumentListener(me);
                                    }
                                });
                            }
                        }
                    }

//					allowDocumentUpdate = isEditable();
                }
            });

            txt_layerDemandTrafficUnits.getDocument().addDocumentListener(new DocumentAdapter(networkViewer)
            {
                @Override
                protected void updateInfo(String text)
                {
//					allowDocumentUpdate = false;

                    NetworkLayer layer = networkViewer.getDesign().getNetworkLayerDefault();
                    JTable table = (JTable) layerTable;
                    TableModel model = table.getModel();
                    int numRows = model.getRowCount();
                    for (int row = 0; row < numRows; row++)
                    {
                        if ((Long) model.getValueAt(row, AdvancedJTable_layer.COLUMN_ID) == layer.getId())
                        {
                            final String previousValue = model.getValueAt(row, AdvancedJTable_layer.COLUMN_DEMANDTRAFUNITS).toString();
                            model.setValueAt(text, row, AdvancedJTable_layer.COLUMN_DEMANDTRAFUNITS);
                            if (!text.equals(model.getValueAt(row, AdvancedJTable_layer.COLUMN_DEMANDTRAFUNITS).toString()))
                            {
                                final DocumentListener me = this;

                                SwingUtilities.invokeLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        txt_layerDemandTrafficUnits.getDocument().removeDocumentListener(me);
                                        txt_layerDemandTrafficUnits.setText(previousValue);
                                        txt_layerDemandTrafficUnits.getDocument().addDocumentListener(me);
                                    }
                                });
                            }
                        }
                    }

//					allowDocumentUpdate = isEditable();
                }
            });

            txt_layerDescription.getDocument().addDocumentListener(new DocumentAdapter(networkViewer)
            {
                @Override
                protected void updateInfo(String text)
                {
//					allowDocumentUpdate = false;

                    NetworkLayer layer = networkViewer.getDesign().getNetworkLayerDefault();
                    JTable table = (JTable) layerTable;
                    TableModel model = table.getModel();
                    int numRows = model.getRowCount();
                    for (int row = 0; row < numRows; row++)
                        if ((Long) model.getValueAt(row, AdvancedJTable_layer.COLUMN_ID) == layer.getId())
                            model.setValueAt(text, row, AdvancedJTable_layer.COLUMN_DESCRIPTION);
//					allowDocumentUpdate = isEditable();
                }
            });
        }

        // Tag table
        layerTagTable = new AdvancedJTable(new ClassAwareTableModel(new Object[1][tagTableHeader.length], tagTableHeader));

        ColumnHeaderToolTips tagTips = new ColumnHeaderToolTips();
        for (int c = 0; c < tagTableHeader.length; c++)
        {
            TableColumn col = layerTagTable.getColumnModel().getColumn(c);
            tagTips.setToolTip(col, tagTableTips[c]);
        }

        if (networkViewer.getVisualizationState().isNetPlanEditable())
            layerTagTable.addMouseListener(new SingleElementTagEditor(networkViewer, NetworkElementType.LAYER));

        layerTagTable.getTableHeader().addMouseMotionListener(tagTips);

        JScrollPane sp_tags = new JScrollPane(layerTagTable);
        sp_tags.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        ScrollPaneLayout tagLayout = new FullScrollPaneLayout();
        sp_tags.setLayout(tagLayout);

        KeyListener tagKey = new TableCursorNavigation();
        layerTagTable.addKeyListener(tagKey);

        // Attribute table
        layerAttributeTable = new AdvancedJTable(new ClassAwareTableModel(new Object[1][attributeTableHeader.length], attributeTableHeader));
        if (networkViewer.getVisualizationState().isNetPlanEditable())
            layerAttributeTable.addMouseListener(new SingleElementAttributeEditor(networkViewer, NetworkElementType.LAYER));

        ColumnHeaderToolTips tips = new ColumnHeaderToolTips();
        for (int c = 0; c < attributeTableHeader.length; c++)
        {
            TableColumn col = layerAttributeTable.getColumnModel().getColumn(c);
            tips.setToolTip(col, attributeTableTips[c]);
        }

        layerAttributeTable.getTableHeader().addMouseMotionListener(tips);

        JScrollPane scrollPane = new JScrollPane(layerAttributeTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        ScrollPaneLayout layout = new FullScrollPaneLayout();
        scrollPane.setLayout(layout);

        KeyListener cursorNavigation = new TableCursorNavigation();
        layerAttributeTable.addKeyListener(cursorNavigation);

        layerSummaryTables = new ParamValueTable[4];
        for (int i = 0; i < layerSummaryTables.length; i++)
        {
            layerSummaryTables[i] = new ParamValueTable(layerSummaryTableHeader);
            layerSummaryTables[i].setAutoCreateRowSorter(true);
            layerSummaryTables[i].addKeyListener(cursorNavigation);
        }

        layerMetricsInfo = new JPanel();
        layerMetricsInfo.setLayout(new MigLayout("insets 0 0 0 0", "[grow]"));
        layerMetricsInfo.add(new JLabel("Topology and link capacities"), "growx, wrap");
        layerMetricsInfo.add(new JScrollPane(layerSummaryTables[0]), "growx, wrap");
        layerMetricsInfo.add(new JLabel("Traffic"), "growx, wrap");
        layerMetricsInfo.add(new JScrollPane(layerSummaryTables[1]), "growx, wrap");
        layerMetricsInfo.add(new JLabel("Routing"), "growx, wrap");
        layerMetricsInfo.add(new JScrollPane(layerSummaryTables[2]), "growx, wrap");
        layerMetricsInfo.add(new JLabel("Resilience information"), "growx, wrap");
        layerMetricsInfo.add(new JScrollPane(layerSummaryTables[3]), "growx");

        JPanel layerPane = new JPanel(new MigLayout("", "[][grow]", "[][][][grow]"));
        layerPane.add(new JLabel("Name"));
        layerPane.add(txt_layerName, "grow, wrap");
        layerPane.add(new JLabel("Description"), "aligny top");
        layerPane.add(new JScrollPane(txt_layerDescription), "grow, wrap, height 100::");
        layerPane.add(new JLabel("Link capacity units"));
        layerPane.add(txt_layerLinkCapacityUnits, "grow, wrap");
        layerPane.add(new JLabel("Demand traffic units"));
        layerPane.add(txt_layerDemandTrafficUnits, "grow, wrap");
        layerPane.add(new JLabel("Routing type"));

        JPanel radioPanel = new JPanel(new GridLayout(0, 1));
        radioPanel.add(sourceRoutingActivated);
        radioPanel.add(hopByHopRoutingActivated);
        layerPane.add(radioPanel, "grow, wrap");

        layerPane.add(sp_tags, "grow, spanx");

        layerPane.add(scrollPane, "grow, spanx 2");
        JScrollPane layerInfoScrollPane = new JScrollPane(layerMetricsInfo);
        layerInfoScrollPane.setBorder(BorderFactory.createEmptyBorder());

        forceUpdate = new JButton("Update all metrics");
        forceUpdate.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                updateLayerMetrics(networkViewer.getDesign(), true);
            }
        });

        forceUpdate.setToolTipText("Use this button to update those metrics which not automatically computed in large netwokrs, since they involve quite time-consuming computations");
        forceUpdate.setVisible(false);

        JPanel layerInfoPane = new JPanel(new BorderLayout());
        layerInfoPane.add(forceUpdate, BorderLayout.NORTH);
        layerInfoPane.add(layerInfoScrollPane, BorderLayout.CENTER);

        JSplitPane splitPaneTopology = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPaneTopology.setTopComponent(layerPane);
        splitPaneTopology.setBottomComponent(layerInfoPane);
        splitPaneTopology.setResizeWeight(0.3);
        splitPaneTopology.addPropertyChangeListener(new ProportionalResizeJSplitPaneListener());
        add(splitPaneTopology, "grow, wrap");
//		netPlanViewTableComponent.put(elementType, splitPaneTopology);

    }


    public void updateNetPlanView(NetPlan currentState)
    {
        this.insideUpdateView = true;
        layerAttributeTable.setEnabled(false);
        ((DefaultTableModel) layerAttributeTable.getModel()).setDataVector(new Object[1][attributeTableHeader.length], attributeTableHeader);
        layerTagTable.setEnabled(false);
        ((DefaultTableModel) layerTagTable.getModel()).setDataVector(new Object[1][tagTableHeader.length], tagTableHeader);

        if (currentState.getRoutingType() == RoutingType.SOURCE_ROUTING) sourceRoutingActivated.setSelected(true);
        else hopByHopRoutingActivated.setSelected(true);

        NetworkLayer layer = currentState.getNetworkLayerDefault();
        Map<String, String> layerAttributes = layer.getAttributes();
        if (!layerAttributes.isEmpty())
        {
            int layerAttributeId = 0;
            Object[][] layerData = new Object[layerAttributes.size()][2];
            for (Map.Entry<String, String> entry : layerAttributes.entrySet())
            {
                layerData[layerAttributeId][0] = entry.getKey();
                layerData[layerAttributeId][1] = entry.getValue();
                layerAttributeId++;
            }

            ((DefaultTableModel) layerAttributeTable.getModel()).setDataVector(layerData, attributeTableHeader);
        }

        // Tag data
        final Set<String> layerTags = layer.getTags();
        final String[] tagArray = layerTags.toArray(new String[layerTags.size()]);

        if (!(tagArray.length == 0))
        {
            final Object[][] tagData = new Object[tagArray.length][1];
            for (int i = 0; i < tagData.length; i++)
            {
                tagData[i][0] = tagArray[i];
            }
            ((DefaultTableModel) layerTagTable.getModel()).setDataVector(tagData, tagTableHeader);
        }

        if (!txt_layerName.getText().equals(layer.getName())) txt_layerName.setText(layer.getName());
        if (!txt_layerDescription.getText().equals(layer.getDescription()))
            txt_layerDescription.setText(layer.getDescription());
        if (!txt_layerLinkCapacityUnits.getText().equals(currentState.getLinkCapacityUnitsName()))
            txt_layerLinkCapacityUnits.setText(currentState.getLinkCapacityUnitsName());
        if (!txt_layerDemandTrafficUnits.getText().equals(currentState.getDemandTrafficUnitsName()))
            txt_layerDemandTrafficUnits.setText(currentState.getDemandTrafficUnitsName());

        boolean hardComputations = currentState.getNumberOfNodes() <= 100;
        updateLayerMetrics(currentState, hardComputations);
        this.insideUpdateView = false;
    }

    private void updateLayerMetrics(NetPlan netPlan, boolean applyHardComputations)
    {
        List<Node> nodes = netPlan.getNodes();
        List<Link> links = netPlan.getLinks();
        int N = netPlan.getNumberOfNodes();
        int E = netPlan.getNumberOfLinks();
        int D = netPlan.getNumberOfDemands();
        int MD = netPlan.getNumberOfMulticastDemands();
        int numSRGs = netPlan.getNumberOfSRGs();
        double U_e = netPlan.getVectorLinkCapacity().zSum();
        double H_d = netPlan.getVectorDemandOfferedTraffic().zSum();
        double u_e_avg = E == 0 ? 0 : U_e / E;

        int E_limitedCapacityLinks = 0;
        double totalCapacityInstalled_limitedCapacityLinks = 0;
        for (Link link : links)
        {
            double u_e = link.getCapacity();
            if (u_e < Double.MAX_VALUE)
            {
                E_limitedCapacityLinks++;
                totalCapacityInstalled_limitedCapacityLinks += u_e;
            }
        }

        double averageTotalCapacityInstalled_limitedCapacityLinks = E_limitedCapacityLinks == 0 ? 0 : totalCapacityInstalled_limitedCapacityLinks / E_limitedCapacityLinks;

        GraphTheoryMetrics metrics = new GraphTheoryMetrics(nodes, links, null);
        DoubleMatrix1D nodeOutDegree = metrics.getOutNodeDegree();
        int[] maxMinOutDegree = new int[2];
        maxMinOutDegree[0] = nodeOutDegree.size() == 0 ? 0 : (int) nodeOutDegree.getMaxLocation()[0];
        maxMinOutDegree[1] = nodeOutDegree.size() == 0 ? 0 : (int) nodeOutDegree.getMinLocation()[0];
        double avgOutDegree = metrics.getAverageOutNodeDegree();

        Map<String, Object> topologyData = new LinkedHashMap<String, Object>();
        topologyData.put("Number of nodes", N);
        topologyData.put("Number of links", E);
        topologyData.put("Node out-degree (max, min, avg)", String.format("%d, %d, %.3f", maxMinOutDegree[0], maxMinOutDegree[1], avgOutDegree));
        final int numConnectedComponents = GraphUtils.getConnectedComponents(nodes , links).size();
        topologyData.put("Is connected? (# connected components)", numConnectedComponents == 1? "yes" : "no (" + numConnectedComponents + ")");
        topologyData.put("All links are bidirectional (yes/no)", applyHardComputations? (GraphUtils.isBidirectional(nodes, links) ? "Yes" : "No") : "-");

        if (applyHardComputations)
        {
            int networkDiameter_hops = (int) metrics.getDiameter();
            metrics.configureLinkCostMap((Map<Link, Double>) CollectionUtils.toMap(links, netPlan.getVectorLinkLengthInKm()));
            double networkDiameter_km = metrics.getDiameter();
            metrics.configureLinkCostMap((Map<Link, Double>) CollectionUtils.toMap(links, netPlan.getVectorLinkPropagationDelayInMiliseconds()));
            double networkDiameter_ms = metrics.getDiameter();

            topologyData.put("Layer diameter (hops, km, ms)", String.format("%d, %.3f, %.3g", networkDiameter_hops, networkDiameter_km, networkDiameter_ms));
        } else
        {
            topologyData.put("Layer diameter (hops, km, ms)", "- (use 'Update all metrics' button)");
        }

        topologyData.put("Capacity installed: total", String.format("%.3f", U_e));
        topologyData.put("Capacity installed: average per link", String.format("%.3f", u_e_avg));
        topologyData.put("Capacity installed (limited capacity links): total", String.format("%.3f", totalCapacityInstalled_limitedCapacityLinks));
        topologyData.put("Capacity installed (limited capacity links): average per link", String.format("%.3f", averageTotalCapacityInstalled_limitedCapacityLinks));

        List<Map<String, Object>> layerSummaryInfo = new LinkedList<Map<String, Object>>();
        layerSummaryInfo.add(topologyData);

        boolean isTrafficSymmetric = GraphUtils.isWeightedBidirectional(netPlan.getNodes(), netPlan.getDemands(), netPlan.getVectorDemandOfferedTraffic());
        double averageNodePairOfferedTraffic = H_d == 0 ? 0 : H_d / (N * (N - 1));
        double blockedTrafficPercentage = H_d == 0 ? 0 : 100 * (netPlan.getVectorDemandBlockedTraffic().zSum() / H_d);

        Map<String, Object> trafficData = new LinkedHashMap<String, Object>();
        trafficData.put("Number of UNICAST demands", D);
        trafficData.put("Offered UNICAST traffic: total", String.format("%.3f", H_d));
        trafficData.put("Offered UNICAST traffic: average per node pair", String.format("%.3f", averageNodePairOfferedTraffic));
        trafficData.put("Blocked UNICAST traffic (%)", String.format("%.3f", blockedTrafficPercentage));
        trafficData.put("Symmetric offered UNICAST traffic?", isTrafficSymmetric ? "Yes" : "No");
        trafficData.put("Number of MULTICAST demands", MD);
        trafficData.put("Offered MULTICAST traffic: total", String.format("%.3f", netPlan.getVectorMulticastDemandOfferedTraffic().zSum()));
        trafficData.put("Blocked MULTICAST traffic (%)", String.format("%.3f", netPlan.getVectorMulticastDemandBlockedTraffic().zSum()));

        layerSummaryInfo.add(trafficData);

        DoubleMatrix1D vector_rhoe = netPlan.getVectorLinkUtilization();
        double max_rho_e = vector_rhoe.size() == 0 ? 0 : vector_rhoe.getMaxLocation()[0];
        RoutingType routingType = netPlan.getRoutingType();
        if (routingType == RoutingType.SOURCE_ROUTING)
        {
            int R = netPlan.getNumberOfRoutes();
            boolean isUnicastRoutingBifurcated = netPlan.isUnicastRoutingBifurcated();
            boolean hasUnicastRoutingLoops = netPlan.hasUnicastRoutingLoops();

            double averageRouteLength_hops = TrafficComputationEngine.getRouteAverageLength(netPlan.getRoutes(), null);
            double averageRouteLength_km = TrafficComputationEngine.getRouteAverageLength(netPlan.getRoutes(), netPlan.getVectorLinkLengthInKm());
            double averageRouteLength_ms = TrafficComputationEngine.getRouteAverageLength(netPlan.getRoutes(), netPlan.getVectorLinkPropagationDelayInMiliseconds());

            Map<String, Object> routingData = new LinkedHashMap<String, Object>();
            routingData.put("Number of routes", R);
            routingData.put("Unicast routing is bifurcated?", isUnicastRoutingBifurcated ? "Yes" : "No");
            routingData.put("Network congestion - bottleneck utilization", String.format("%.3f, %.3f", max_rho_e, max_rho_e));
            routingData.put("Average (unicast) route length (hops, km, ms)", String.format("%.3f, %.3f, %.3g", averageRouteLength_hops, averageRouteLength_km, averageRouteLength_ms));
            routingData.put("Unicast routing has loops?", hasUnicastRoutingLoops ? "Yes" : "No");
            routingData.put("Number of multicast trees", netPlan.getNumberOfMulticastTrees());
            routingData.put("Multicast routing is bifurcated?", netPlan.isMulticastRoutingBifurcated() ? "Yes" : "No");
            Pair<Double, Double> stats = TrafficComputationEngine.getAverageHopsAndLengthOfMulticastTrees(netPlan.getMulticastTrees());
            routingData.put("Average multicast tree size (hops, km)", String.format("%.3f, %.3f", stats.getFirst(), stats.getSecond()));

            layerSummaryInfo.add(routingData);

            final double protectionPercentage = TrafficComputationEngine.getTrafficProtectionDegree(netPlan);
            Pair<Double, Double> srgDisjointnessPercentage = SRGUtils.getSRGDisjointnessPercentage(netPlan);
            String srgModel = SRGUtils.getSRGModel(netPlan);

            Map<String, Object> protectionData = new LinkedHashMap<String, Object>();
            protectionData.put("% of carried traffic with at least one backup path", String.format("%.3f", protectionPercentage));
            protectionData.put("Number of SRGs in the network", numSRGs);
            protectionData.put("SRG definition characteristic", srgModel);
            protectionData.put("% routes protected with SRG disjoint backup paths (w. end nodes, w.o. end nodes)", String.format("%.3f, %.3f", srgDisjointnessPercentage.getFirst(), srgDisjointnessPercentage.getSecond()));

            layerSummaryInfo.add(protectionData);
        } else
        {
            Map<String, Object> routingData = new LinkedHashMap<String, Object>();
            routingData.put("Network congestion - bottleneck utilization", String.format("%.3f", max_rho_e));

            RoutingCycleType routingCycleType = RoutingCycleType.LOOPLESS;
            for (Demand demand : netPlan.getDemands())
            {
                if (demand.getRoutingCycleType() != RoutingCycleType.LOOPLESS)
                    routingCycleType = demand.getRoutingCycleType();
                if (routingCycleType == RoutingCycleType.CLOSED_CYCLES) break;
            }
            routingData.put("Routing has loops?", routingCycleType);
            layerSummaryInfo.add(routingData);
        }

        ListIterator<Map<String, Object>> it = layerSummaryInfo.listIterator();
        while (it.hasNext())
        {
            int tableId = it.nextIndex();
            layerSummaryTables[tableId].setData(it.next());

            AdvancedJTable.setVisibleRowCount(layerSummaryTables[tableId], layerSummaryTables[tableId].getRowCount());
            AdvancedJTable.setWidthAsPercentages(layerSummaryTables[tableId], 0.7, 0.3);
        }
        layerSummaryTables[0].setToolTipText(0, 0, "Indicates the number of defined nodes in the network");
        layerSummaryTables[0].setToolTipText(1, 0, "Indicates the number of defined links in this layer");
        layerSummaryTables[0].setToolTipText(2, 0, "Indicates the maximum/minimum/average value for the out-degree, that is, the number of outgoing links per node");
        layerSummaryTables[0].setToolTipText(3, 0, "Indicates if the topology is connected. If not, shows the number of connected components");
        layerSummaryTables[0].setToolTipText(4, 0, "Indicates whether all links are bidirectional, that is, if there are the same number of links between each node pair in both directions (irrespective of the respective capacities)");
        layerSummaryTables[0].setToolTipText(5, 0, "Indicates the layer diameter, that is, the length of the largest shortest-path in this layer");
        layerSummaryTables[0].setToolTipText(6, 0, "Indicates the total capacity installed in this layer");
        layerSummaryTables[0].setToolTipText(7, 0, "Indicates the average capacity installed per link");
        layerSummaryTables[0].setToolTipText(8, 0, "Indicates the total capacity installed in this layer for links whose capacity is not infinite");
        layerSummaryTables[0].setToolTipText(9, 0, "Indicates the average capacity installed in this layer for links whose capacity is not infinite");
        layerSummaryTables[1].setToolTipText(0, 0, "Indicates the number of defined demands in this layer");
        layerSummaryTables[1].setToolTipText(1, 0, "Indicates the total offered unicast traffic to the network in this layer");
        layerSummaryTables[1].setToolTipText(2, 0, "Indicates the total offered unicast traffic to the network per each node pair in this layer");
        layerSummaryTables[1].setToolTipText(3, 0, "Indicates the percentage of blocked unicast traffic from the total offered to the network in this layer");
        layerSummaryTables[1].setToolTipText(4, 0, "Indicates whether the unicast offered traffic is symmetric, that is, if there are the same number of demands with the same offered traffic between each node pair in both directions");
        layerSummaryTables[1].setToolTipText(5, 0, "Indicates the number of defined multicast demands in this layer");
        layerSummaryTables[1].setToolTipText(6, 0, "Indicates the total offered multicast traffic to the network in this layer");
        layerSummaryTables[1].setToolTipText(7, 0, "Indicates the percentage of blocked multicast traffic from the total offered to the network in this layer");

        layerMetricsInfo.removeAll();
        layerMetricsInfo.add(new JLabel("Topology and link capacities"), "growx, wrap");
        layerMetricsInfo.add(new JScrollPane(layerSummaryTables[0]), "growx, wrap");
        layerMetricsInfo.add(new JLabel("Traffic"), "growx, wrap");
        layerMetricsInfo.add(new JScrollPane(layerSummaryTables[1]), "growx, wrap");

        if (routingType == RoutingType.SOURCE_ROUTING)
        {
            layerSummaryTables[2].setToolTipText(0, 0, "Indicates the number of defined routes");
            layerSummaryTables[2].setToolTipText(1, 0, "<html>Indicates whether the unicast routing is bifurcated, that is, if at least there are more than one active (up) route <u>carrying</u> traffic from any demand</html>");
            layerSummaryTables[2].setToolTipText(2, 0, "Indicates the network congestion, that is, the utilization of the busiest link");
            layerSummaryTables[2].setToolTipText(3, 0, "Indicates the average route length (unicast traffic)");
            layerSummaryTables[2].setToolTipText(4, 0, "Indicates whether the unicast routing has loops, that is, if at least a route visits a node more than once");
            layerSummaryTables[2].setToolTipText(5, 0, "Indicates the number of defined multicast trees");
            layerSummaryTables[2].setToolTipText(6, 0, "<html>Indicates whether the multicast routing is bifurcated, that is, if for at least one multicast demand is carried by more than one up multicast trees</html>");
            layerSummaryTables[2].setToolTipText(7, 0, "Indicates the average number of links and average total length (summing all the links) of the multicast trees in the network");
            layerSummaryTables[3].setToolTipText(0, 0, "Indicates the number of defined protection segments");
            layerSummaryTables[3].setToolTipText(1, 0, "Indicates the average reserved bandwidth per protection segment");
            layerSummaryTables[3].setToolTipText(2, 0, "Indicates the percentage of traffic (from the total) which is not protected by any segment");
            layerSummaryTables[3].setToolTipText(3, 0, "Indicates the percentage of traffic (from the total) which is protected by dedicated segments reservating so the total traffic can be carried from origin to destination through currently unused and dedicated segments");
            layerSummaryTables[3].setToolTipText(4, 0, "Indicates the percentage of traffic (from the total) which is neither unprotected nor full-and-dedicated protected");
            layerSummaryTables[3].setToolTipText(5, 0, "Indicates the number of defined SRGs");
            layerSummaryTables[3].setToolTipText(6, 0, "Indicates whether SRG definition follows one of the predefined models (per node, per link...), or 'Mixed' otherwise (or 'None' if no SRGs are defined)");
            layerSummaryTables[3].setToolTipText(7, 0, "Indicates the percentage of routes from the total which have all protection segments SRG-disjoint, without taking into account the carried traffic per route");

            layerMetricsInfo.add(new JLabel("Routing"), "growx, wrap");
            layerMetricsInfo.add(new JScrollPane(layerSummaryTables[2]), "growx, wrap");
            layerMetricsInfo.add(new JLabel("Resilience information"), "growx, wrap");
            layerMetricsInfo.add(new JScrollPane(layerSummaryTables[3]), "growx");
        } else
        {
            layerSummaryTables[2].setToolTipText(0, 0, "Indicates the network congestion, that is, the utilization of the busiest link");
            layerSummaryTables[2].setToolTipText(1, 0, "Indicates whether the routing has loops, that is, if at least a route visits a node more than once");

            layerMetricsInfo.add(new JLabel("Routing"), "growx, wrap");
            layerMetricsInfo.add(new JScrollPane(layerSummaryTables[2]), "growx, wrap");
        }

        forceUpdate.setVisible(!applyHardComputations);
    }
}
