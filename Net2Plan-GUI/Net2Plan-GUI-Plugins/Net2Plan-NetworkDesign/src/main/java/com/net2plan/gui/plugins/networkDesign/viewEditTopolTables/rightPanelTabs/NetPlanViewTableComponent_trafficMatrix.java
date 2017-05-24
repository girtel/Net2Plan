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
/* Pending
 * 1) GUI
 * 2) Make pending functionalities
 * 3) Add functionality of exporting to Excel the traffic matrix
 */

package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.rightPanelTabs;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.CellRenderers;
import com.net2plan.gui.plugins.networkDesign.io.excel.ExcelWriter;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.plugins.networkDesign.whatIfAnalysisPane.WhatIfAnalysisPane;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.JNumberField;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.TrafficMatrixGenerationModels;
import com.net2plan.utils.Pair;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.collections15.BidiMap;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class NetPlanViewTableComponent_trafficMatrix extends JPanel
{
    private static final String DEFAULT_TAG_FILTER = "[NO FILTER]";

    private static final int OPTIONINDEX_TRAFFICMODEL_CONSTANT = 1;
    private static final int OPTIONINDEX_TRAFFICMODEL_UNIFORM01 = 2;
    private static final int OPTIONINDEX_TRAFFICMODEL_UNIFORM5050 = 3;
    private static final int OPTIONINDEX_TRAFFICMODEL_UNIFORM2575 = 4;
    private static final int OPTIONINDEX_TRAFFICMODEL_GRAVITYMODEL = 5;
    private static final int OPTIONINDEX_TRAFFICMODEL_POPULATIONDISTANCE = 6;
    private static final int OPTIONINDEX_TRAFFICMODEL_RESET = 7;

    private static final int OPTIONINDEX_NORMALIZATION_MAKESYMMETRIC = 1;
    private static final int OPTIONINDEX_NORMALIZATION_SCALE = 2;
    private static final int OPTIONINDEX_NORMALIZATION_RANDOMVARIATION = 3;
    private static final int OPTIONINDEX_NORMALIZATION_TOTAL = 4;
    private static final int OPTIONINDEX_NORMALIZATION_PERNODETRAFOUT = 5;
    private static final int OPTIONINDEX_NORMALIZATION_PERNODETRAFIN = 6;
    private static final int OPTIONINDEX_NORMALIZATION_MAXIMUMSCALEDVERSION = 7;

    private final JTable trafficMatrixTable;

    private final JCheckBox cb_filterLinklessNodes;
    private final JComboBox<String> cmb_tagNodesSelector;
    private final JComboBox<String> cmb_tagDemandsSelector;
    private final JComboBox<String> cmb_trafficModelPattern;
    private final JComboBox<String> cmb_trafficNormalization;
    private final JButton applyTrafficNormalizationButton;
    private final JButton applyTrafficModelButton;
    private final GUINetworkDesign networkViewer;

    private final SwitchableItemListener itemListener = new SwitchableItemListener()
    {
        @Override
        protected void doAction()
        {
            NetPlanViewTableComponent_trafficMatrix.this.updateTable();
        }
    };

    public NetPlanViewTableComponent_trafficMatrix(GUINetworkDesign networkViewer)
    {
        super(new BorderLayout());
        this.networkViewer = networkViewer;

        this.trafficMatrixTable = new AdvancedJTable();
        trafficMatrixTable.setDefaultRenderer(Object.class, new TotalRowColumnRenderer());
        trafficMatrixTable.setDefaultRenderer(Double.class, new TotalRowColumnRenderer());
        trafficMatrixTable.setDefaultRenderer(Number.class, new TotalRowColumnRenderer());
        trafficMatrixTable.setDefaultRenderer(Integer.class, new TotalRowColumnRenderer());
        trafficMatrixTable.setDefaultRenderer(String.class, new TotalRowColumnRenderer());
        JScrollPane pane = new JScrollPane(trafficMatrixTable);
        this.add(pane, BorderLayout.CENTER);

        JPanel pnl_trafficModel = new JPanel();
        pnl_trafficModel.setBorder(BorderFactory.createTitledBorder("Traffic matrix synthesis"));

        // Will fail if not continuous.
        this.applyTrafficModelButton = new JButton("Apply");
        this.applyTrafficModelButton.setName("trafficModelApply");
        applyTrafficModelButton.addActionListener(new CommonActionPerformListenerModelAndNormalization());

        cmb_trafficModelPattern = new WiderJComboBox();
        cmb_trafficModelPattern.setName("trafficModelWheel");
        cmb_trafficModelPattern.insertItemAt("Select a method for synthesizing a matrix", 0);
        cmb_trafficModelPattern.insertItemAt("1. Constant", OPTIONINDEX_TRAFFICMODEL_CONSTANT);
        cmb_trafficModelPattern.insertItemAt("2. Uniform (0, 10)", OPTIONINDEX_TRAFFICMODEL_UNIFORM01);
        cmb_trafficModelPattern.insertItemAt("3. 50% Uniform (0, 100) & 50% Uniform(0, 10)", OPTIONINDEX_TRAFFICMODEL_UNIFORM5050);
        cmb_trafficModelPattern.insertItemAt("4. 25% Uniform (0, 100) & 75% Uniform(0, 10)", OPTIONINDEX_TRAFFICMODEL_UNIFORM2575);
        cmb_trafficModelPattern.insertItemAt("5. Gravity model", OPTIONINDEX_TRAFFICMODEL_GRAVITYMODEL);
        cmb_trafficModelPattern.insertItemAt("6. Population-distance model", OPTIONINDEX_TRAFFICMODEL_POPULATIONDISTANCE);
        cmb_trafficModelPattern.insertItemAt("7. Reset", OPTIONINDEX_TRAFFICMODEL_RESET);
        this.cmb_trafficModelPattern.addItemListener(e -> applyTrafficModelButton.setEnabled(!(cmb_trafficModelPattern.getSelectedIndex() == 0)));
        cmb_trafficModelPattern.setSelectedIndex(0);

        pnl_trafficModel.add(cmb_trafficModelPattern);
        pnl_trafficModel.setLayout(new MigLayout("insets 0 0 0 0", "[grow][][]", "[grow]"));
        pnl_trafficModel.add(cmb_trafficModelPattern, "grow, wmin 50");
        pnl_trafficModel.add(applyTrafficModelButton);

        JPanel pnl_normalization = new JPanel();
        pnl_normalization.setBorder(BorderFactory.createTitledBorder("Traffic normalization and adjustments"));

        this.applyTrafficNormalizationButton = new JButton("Apply");
        this.applyTrafficNormalizationButton.setName("normalizationApply");
        this.applyTrafficNormalizationButton.addActionListener(new CommonActionPerformListenerModelAndNormalization());

        cmb_trafficNormalization = new WiderJComboBox();
        cmb_trafficNormalization.setName("normalizationWheel");
        cmb_trafficNormalization.insertItemAt("Select a method", 0);
        cmb_trafficNormalization.insertItemAt("1. Make symmetric", OPTIONINDEX_NORMALIZATION_MAKESYMMETRIC);
        cmb_trafficNormalization.insertItemAt("2. Scale by a factor", OPTIONINDEX_NORMALIZATION_SCALE);
        cmb_trafficNormalization.insertItemAt("3. Apply random variation (truncated gaussian)", OPTIONINDEX_NORMALIZATION_RANDOMVARIATION);
        cmb_trafficNormalization.insertItemAt("4. Normalization: fit to given total traffic", OPTIONINDEX_NORMALIZATION_TOTAL);
        cmb_trafficNormalization.insertItemAt("5. Normalization: fit to given out traffic per node", OPTIONINDEX_NORMALIZATION_PERNODETRAFOUT);
        cmb_trafficNormalization.insertItemAt("6. Normalization: fit to given in traffic per node", OPTIONINDEX_NORMALIZATION_PERNODETRAFIN);
        cmb_trafficNormalization.insertItemAt("7. Normalization: scale to theoretical maximum traffic", OPTIONINDEX_NORMALIZATION_MAXIMUMSCALEDVERSION);
        this.cmb_trafficNormalization.addItemListener(e -> applyTrafficNormalizationButton.setEnabled(!(cmb_trafficNormalization.getSelectedIndex() == 0)));
        cmb_trafficNormalization.setSelectedIndex(0);

        pnl_normalization.add(cmb_trafficNormalization);
        pnl_normalization.setLayout(new MigLayout("insets 0 0 0 0", "[grow][][]", "[grow]"));
        pnl_normalization.add(cmb_trafficNormalization, "grow, wmin 50");
        pnl_normalization.add(applyTrafficNormalizationButton);

        final JPanel filterPanel = new JPanel(new MigLayout("wrap 2", "[][grow]"));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filters"));

        this.cb_filterLinklessNodes = new JCheckBox();
        filterPanel.add(new JLabel("Filter out nodes without links at this layer"), "align label");
        filterPanel.add(cb_filterLinklessNodes);

        this.cmb_tagNodesSelector = new WiderJComboBox();
        this.cmb_tagDemandsSelector = new WiderJComboBox();

        this.cmb_tagNodesSelector.addItemListener(itemListener);
        this.cmb_tagDemandsSelector.addItemListener(itemListener);
        this.cb_filterLinklessNodes.addItemListener(itemListener);


        filterPanel.add(new JLabel("Consider only demands between nodes tagged by..."), "align label");
        filterPanel.add(cmb_tagNodesSelector, "grow");

        filterPanel.add(new JLabel("Consider only demands tagged by..."), "align label");
        filterPanel.add(cmb_tagDemandsSelector, "grow");

        final JPanel bottomPanel = new JPanel(new MigLayout("fill, wrap 1"));
        bottomPanel.add(filterPanel, "grow");
        bottomPanel.add(pnl_trafficModel, "grow");
        bottomPanel.add(pnl_normalization, "grow");

        this.add(bottomPanel, BorderLayout.SOUTH);

        updateNetPlanView();
    }

    public JTable getTable()
    {
        return trafficMatrixTable;
    }

    public double[][] getTrafficMatrix()
    {
        TableModel dtm = trafficMatrixTable.getModel();
        int nRow = dtm.getRowCount();
        int nCol = dtm.getColumnCount();

        double[][] tableData = new double[nRow - 1][nCol - 2];
        for (int i = 0; i < nRow - 1; i++)
            for (int j = 1; j < nCol - 1; j++)
                tableData[i][j - 1] = (double) dtm.getValueAt(i, j);

        return tableData;
    }

    public void filterLinklessNodes(boolean doFilter)
    {
        cb_filterLinklessNodes.setSelected(doFilter);

        updateTable();
    }

    public void filterByNodeTag(String tag)
    {
        if (tag == null) return;

        updateComboBox();

        this.cmb_tagNodesSelector.setSelectedItem(tag);
        if (cmb_tagNodesSelector.getSelectedItem() != tag) return;

        updateTable();
    }

    public void filterByDemandTag(String tag)
    {
        if (tag == null) return;

        updateComboBox();

        this.cmb_tagDemandsSelector.setSelectedItem(tag);
        if (cmb_tagDemandsSelector.getSelectedItem() != tag) return;

        updateTable();
    }

    private Pair<List<Node>, Set<Demand>> computeFilteringNodesAndDemands()
    {
        final NetPlan np = networkViewer.getDesign();
        final List<Node> filteredNodes = new ArrayList<>(np.getNumberOfNodes());

        final String filteringNodeTag = this.cmb_tagNodesSelector.getSelectedItem() == DEFAULT_TAG_FILTER ? null : (String) this.cmb_tagNodesSelector.getSelectedItem();
        final String filteringDemandTag = this.cmb_tagDemandsSelector.getSelectedItem() == DEFAULT_TAG_FILTER ? null : (String) this.cmb_tagDemandsSelector.getSelectedItem();

        for (Node n : np.getNodes())
        {
            if (filteringNodeTag != null)
                if (!n.hasTag(filteringNodeTag)) continue;
            if (this.cb_filterLinklessNodes.isSelected())
                if (!n.getOutgoingLinksAllLayers().stream().anyMatch(e -> e.getLayer().isDefaultLayer())
                        && (!n.getIncomingLinksAllLayers().stream().anyMatch(e -> e.getLayer().isDefaultLayer())))
                    continue;
            filteredNodes.add(n);
        }

        Set<Demand> filteredDemands;

        if (filteringDemandTag == null)
        {
            filteredDemands = new HashSet<>(np.getDemands());
        } else
        {
            filteredDemands = new HashSet<>();
            for (Demand d : np.getDemands())
                if (d.hasTag(filteringDemandTag)) filteredDemands.add(d);
        }

        return Pair.of(filteredNodes, filteredDemands);
    }

    public void updateNetPlanView()
    {
        updateTable();
        updateComboBox();
    }

    private void updateComboBox()
    {
        final NetPlan np = networkViewer.getDesign();
        itemListener.setEnabled(false);

        cmb_tagNodesSelector.removeAllItems();
        cmb_tagNodesSelector.addItem(DEFAULT_TAG_FILTER);
        final Set<String> allTagsNodes = np.getNodes().stream().map(n -> n.getTags()).flatMap(e -> e.stream()).collect(Collectors.toSet());
        final List<String> allTagsNodesOrdered = allTagsNodes.stream().sorted().collect(Collectors.toList());
        for (String tag : allTagsNodesOrdered) this.cmb_tagNodesSelector.addItem(tag);

        cmb_tagDemandsSelector.removeAllItems();
        cmb_tagDemandsSelector.addItem(DEFAULT_TAG_FILTER);
        final Set<String> allTagsDemands = np.getDemands().stream().map(n -> n.getTags()).flatMap(e -> e.stream()).collect(Collectors.toSet());
        final List<String> allTagsDemandsOrdered = allTagsDemands.stream().sorted().collect(Collectors.toList());
        for (String tag : allTagsDemandsOrdered) this.cmb_tagDemandsSelector.addItem(tag);

        itemListener.setEnabled(true);
    }


    private void updateTable()
    {
        this.trafficMatrixTable.setModel(createTrafficMatrix());
    }

    private DefaultTableModel createTrafficMatrix()
    {
        final NetPlan np = this.networkViewer.getDesign();

        final Pair<List<Node>, Set<Demand>> filterInfo = computeFilteringNodesAndDemands();
        final List<Node> filteredNodes = filterInfo.getFirst();
        final Set<Demand> filteredDemands = filterInfo.getSecond();

        final int N = filteredNodes.size();
        final NetworkLayer layer = np.getNetworkLayerDefault();
        String[] columnHeaders = new String[N + 2];
        Object[][] data = new Object[N + 1][N + 2];
        final Map<Node, Integer> nodeToIndexInFilteredListMap = new HashMap<>();
        for (int cont = 0; cont < filteredNodes.size(); cont++)
            nodeToIndexInFilteredListMap.put(filteredNodes.get(cont), cont);

        for (int inNodeListIndex = 0; inNodeListIndex < N; inNodeListIndex++)
        {
            final Node n = filteredNodes.get(inNodeListIndex);
            String aux = n.getName().equals("") ? "Node " + n.getIndex() : n.getName();
            columnHeaders[1 + inNodeListIndex] = aux;
            Arrays.fill(data[inNodeListIndex], 0.0);
            data[inNodeListIndex][0] = aux;
        }

        Arrays.fill(data[data.length - 1], 0.0);
        final int totalsRowIndex = N;
        final int totalsColumnIndex = N + 1;
        double totalTraffic = 0;
        for (Demand d : filteredDemands)
        {
            if (!(nodeToIndexInFilteredListMap.containsKey(d.getIngressNode()))) continue;
            if (!(nodeToIndexInFilteredListMap.containsKey(d.getEgressNode()))) continue;

            final int row = nodeToIndexInFilteredListMap.get(d.getIngressNode());
            final int column = nodeToIndexInFilteredListMap.get(d.getEgressNode()) + 1;
            totalTraffic += d.getOfferedTraffic();
            data[row][column] = ((Double) data[row][column]) + d.getOfferedTraffic();
            data[totalsRowIndex][column] = ((Double) data[totalsRowIndex][column]) + d.getOfferedTraffic();
            data[row][totalsColumnIndex] = ((Double) data[row][totalsColumnIndex]) + d.getOfferedTraffic();
        }
        data[totalsRowIndex][totalsColumnIndex] = totalTraffic;

        data[data.length - 1][0] = "";

        columnHeaders[0] = "";
        data[totalsRowIndex][0] = "Total";
        columnHeaders[columnHeaders.length - 1] = "Total";

        final DefaultTableModel model = new ClassAwareTableModel(data, columnHeaders)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column)
            {
                final int columnCount = getColumnCount();
                final int rowCount = getRowCount();
                if (column == 0 || column == columnCount - 1 || row == rowCount - 1 || row == column - 1) return false;
                final Node n1 = filteredNodes.get(row);
                final Node n2 = filteredNodes.get(column - 1);
                final Set<Demand> applicableDemands = Sets.intersection(
                        np.getNodePairDemands(n1, n2, false, layer), filteredDemands);
                if (applicableDemands.isEmpty()) return false;
                if (applicableDemands.size() > 1) return false;
                if (applicableDemands.iterator().next().isCoupled()) return false;
                return true;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column)
            {
                Object oldValue = getValueAt(row, column);

                try
                {
                    if (Math.abs((double) newValue - (double) oldValue) < 1e-10) return;
                    final double newOfferedTraffic = (Double) newValue;
                    if (newOfferedTraffic < 0) throw new Net2PlanException("Wrong traffic value");

                    final Node n1 = filteredNodes.get(row);
                    final Node n2 = filteredNodes.get(column - 1);

                    final Set<Demand> applicableDemands = Sets.intersection(
                            np.getNodePairDemands(n1, n2, false, layer), filteredDemands);
                    final Demand demand = applicableDemands.iterator().next();
                    if (networkViewer.getVisualizationState().isWhatIfAnalysisActive())
                    {
                        final WhatIfAnalysisPane whatIfPane = networkViewer.getWhatIfAnalysisPane();
                        whatIfPane.whatIfDemandOfferedTrafficModified(demand, newOfferedTraffic);
                        super.setValueAt(newValue, row, column);
                        final VisualizationState vs = networkViewer.getVisualizationState();
                        Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> res =
                                vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<>(networkViewer.getDesign().getNetworkLayers()));
                        vs.setCanvasLayerVisibilityAndOrder(networkViewer.getDesign(), res.getFirst(), res.getSecond());
                        networkViewer.updateVisualizationAfterNewTopology();
                    } else
                    {
                        demand.setOfferedTraffic(newOfferedTraffic);
                        super.setValueAt(newValue, row, column);
                        networkViewer.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                        networkViewer.getVisualizationState().pickElement(demand);
                        networkViewer.updateVisualizationAfterPick();
                        networkViewer.addNetPlanChange();
                    }
                } catch (Throwable e)
                {
                    ErrorHandling.showErrorDialog("Wrong traffic value");
                    return;
                }
            }
        };
        return model;
    }

    private static class TotalRowColumnRenderer extends CellRenderers.NumberCellRenderer
    {
        private final static Color BACKGROUND_COLOR = new Color(200, 200, 200);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (row == table.getRowCount() - 1 || column == table.getColumnCount() - 1)
            {
                c.setBackground(BACKGROUND_COLOR);
                if (isSelected) c.setForeground(Color.BLACK);
            }
            return c;
        }
    }

    public void writeTableToFile(File file)
    {
        TableModel dtm = trafficMatrixTable.getModel();
        int nRow = dtm.getRowCount();
        int nCol = dtm.getColumnCount();

        Object[][] tableData;
        tableData = new Object[nRow + 1][nCol];

        for (int i = 0; i < trafficMatrixTable.getColumnCount(); i++)
            tableData[0][i] = trafficMatrixTable.getColumnName(i);

        for (int i = 1; i < nRow + 1; i++)
            for (int j = 0; j < nCol; j++)
                tableData[i][j] = dtm.getValueAt(i - 1, j);

        ExcelWriter.writeToFile(file, "Traffic matrix", tableData);
    }

    private class ApplyTrafficModels
    {
        private final List<Node> filteredNodes;

        ApplyTrafficModels(List<Node> filteredNodes)
        {
            this.filteredNodes = filteredNodes;
        }

        DoubleMatrix2D applyOption(int selectedOptionIndex)
        {
            if (selectedOptionIndex == 0)
                return null;

            final NetPlan netPlan = networkViewer.getDesign();

            final int N = filteredNodes.size();
            switch (selectedOptionIndex)
            {
                case OPTIONINDEX_TRAFFICMODEL_CONSTANT:
                    final JNumberField txt_constantValue = new JNumberField();
                    txt_constantValue.setToolTipText("x >= 0");

                    final JPanel pane = new JPanel(new MigLayout("fill, wrap 2"));
                    pane.add(new JLabel("Traffic per cell: "), "align label");
                    pane.add(txt_constantValue, "growx");
                    while (true)
                    {
                        int result = JOptionPane.showConfirmDialog(null, pane, "Please enter the traffic per cell", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (result != JOptionPane.OK_OPTION) return null;
                        try
                        {
                            final double constantValue = txt_constantValue.getValue();

                            if (constantValue < 0)
                                throw new IllegalArgumentException("Constant value must be greater or equal than zero");

                            return TrafficMatrixGenerationModels.constantTrafficMatrix(N, constantValue);
                        } catch (NumberFormatException e)
                        {
                            ErrorHandling.showWarningDialog("Traffic per cell must be a number.", "Invalid value");
                        }
                    }
                case OPTIONINDEX_TRAFFICMODEL_RESET:
                    return DoubleFactory2D.sparse.make(N, N);

                case OPTIONINDEX_TRAFFICMODEL_UNIFORM01:
                    return TrafficMatrixGenerationModels.uniformRandom(N, 0, 10);

                case OPTIONINDEX_TRAFFICMODEL_UNIFORM5050:
                    return TrafficMatrixGenerationModels.bimodalUniformRandom(N, 0.5, 0, 100, 0, 10);

                case OPTIONINDEX_TRAFFICMODEL_UNIFORM2575:
                    return TrafficMatrixGenerationModels.bimodalUniformRandom(N, 0.25, 0, 100, 0, 10);

                case OPTIONINDEX_TRAFFICMODEL_GRAVITYMODEL:
                    DefaultTableModel gravityModelTableModel = new ClassAwareTableModel()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public boolean isCellEditable(int row, int col)
                        {
                            if (col == 0) return false;
                            if (row == N) return false;
                            return true;
                        }

                        @Override
                        public void setValueAt(Object newValue, int row, int column)
                        {
                            Object oldValue = getValueAt(row, column);

							/* If value doesn't change, exit from function */
                            if (Math.abs((double) newValue - (double) oldValue) < 1e-10) return;
                            double trafficAmount = (Double) newValue;
                            if (trafficAmount < 0)
                            {
                                ErrorHandling.showErrorDialog("Traffic amount must be greater or equal than zero", "Error introducing traffic amount");
                                return;
                            }

                            if (row != N)
                            {
                                double sum = trafficAmount;
                                for (int i = 0; i < N; i++)
                                    if (i != row)
                                        sum += (Double) this.getValueAt(i, column);

                                this.setValueAt(sum, N, column);
                            }

                            super.setValueAt(newValue, row, column);
                        }
                    };

                    Object[][] gravityModelData = new Object[N + 1][3];
                    for (int n = 0; n < N; n++)
                    {
                        gravityModelData[n][0] = filteredNodes.get(n);
                        gravityModelData[n][1] = 0.0;
                        gravityModelData[n][2] = 0.0;
                    }

                    gravityModelData[N][0] = "Total";
                    gravityModelData[N][1] = 0d;
                    gravityModelData[N][2] = 0d;

                    String[] gravityModelHeader = new String[]{"Node", "Total ingress traffic per node", "Total egress traffic per node"};
                    gravityModelTableModel.setDataVector(gravityModelData, gravityModelHeader);

                    JTable gravityModelTable = new AdvancedJTable(gravityModelTableModel);
                    gravityModelTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
                    {
                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
                        {
                            final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                            if (row == gravityModelTable.getRowCount() - 1)
                            {
                                c.setBackground(new Color(200, 200, 200));
                                c.setForeground(Color.BLACK);
                            } else
                            {
                                c.setBackground(gravityModelTable.getBackground());
                                c.setForeground(gravityModelTable.getForeground());
                            }

                            if (column == 0)
                            {
                                c.setBackground(new Color(200, 200, 200));
                                c.setForeground(Color.BLACK);
                            }

                            return c;
                        }
                    });

                    JPanel gravityModelPanel = new JPanel();
                    JScrollPane gPane = new JScrollPane(gravityModelTable);
                    gravityModelPanel.add(gPane);

                    double[] ingressTrafficPerNode = new double[N];
                    double[] egressTrafficPerNode = new double[N];
                    while (true)
                    {
                        try
                        {
                            int gravityModelResult = JOptionPane.showConfirmDialog(null, gravityModelPanel, "Please enter total ingress/egress traffic per node (one value per row)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (gravityModelResult != JOptionPane.OK_OPTION) return null;

                            for (int n = 0; n < N; n++)
                            {
                                ingressTrafficPerNode[n] = (Double) gravityModelTableModel.getValueAt(n, 1);
                                egressTrafficPerNode[n] = (Double) gravityModelTableModel.getValueAt(n, 2);
                            }

                            return TrafficMatrixGenerationModels.gravityModel(ingressTrafficPerNode, egressTrafficPerNode);
                        } catch (Throwable ex)
                        {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error applying gravity model");
                        }
                    }

                case OPTIONINDEX_TRAFFICMODEL_POPULATIONDISTANCE:
                {
                    final JPanel popUpPanel = new JPanel(new MigLayout("fill"));

                    final JRadioButton euclideanDistance = new JRadioButton("Euclidean distance (X, Y)");
                    final JRadioButton haversineDistance = new JRadioButton("Haversine distance (lon, lat)");

                    final JNumberField txt_randomFactor = new JNumberField(0d, 0, 1, 0.1);
                    txt_randomFactor.setToolTipText("0 <= x <= 1");
                    final JNumberField txt_distanceOffset = new JNumberField();
                    txt_distanceOffset.setToolTipText("x >= 0");
                    final JNumberField txt_distancePower = new JNumberField();
                    txt_distancePower.setToolTipText("x >= 0");
                    final JNumberField txt_populationOffset = new JNumberField();
                    txt_populationOffset.setToolTipText("x >= 0");
                    final JNumberField txt_populationPower = new JNumberField();
                    txt_populationPower.setToolTipText("x >= 0");

                    final JCheckBox chk_populationDistanceModelNormalizePopulation = new JCheckBox();
                    chk_populationDistanceModelNormalizePopulation.setSelected(true);

                    final JCheckBox chk_populationDistanceModelNormalizeDistance = new JCheckBox();
                    chk_populationDistanceModelNormalizeDistance.setSelected(true);

                    popUpPanel.add(new JLabel("Random factor"));
                    popUpPanel.add(txt_randomFactor, "align right, wrap");
                    popUpPanel.add(new JLabel("Population offset"));
                    popUpPanel.add(txt_populationOffset, "align right, wrap");
                    popUpPanel.add(new JLabel("Population power"));
                    popUpPanel.add(txt_populationPower, "align right, wrap");
                    popUpPanel.add(new JLabel("Distance offset"));
                    popUpPanel.add(txt_distanceOffset, "align right, wrap");
                    popUpPanel.add(new JLabel("Distance power"));
                    popUpPanel.add(txt_distancePower, "align right, wrap");
                    popUpPanel.add(new JLabel("Normalize by max. population?"));
                    popUpPanel.add(chk_populationDistanceModelNormalizePopulation, "align center, wrap");
                    popUpPanel.add(new JLabel("Normalize by max. distance?"));
                    popUpPanel.add(chk_populationDistanceModelNormalizeDistance, "align center, wrap");

                    ButtonGroup bg = new ButtonGroup();
                    bg.add(euclideanDistance);
                    bg.add(haversineDistance);

                    final JPanel radioButtonPanel = new JPanel(new GridLayout(2, 1));
                    radioButtonPanel.add(euclideanDistance);
                    radioButtonPanel.add(haversineDistance);
                    euclideanDistance.setSelected(true);

                    radioButtonPanel.setBorder(BorderFactory.createTitledBorder("Distance type"));

                    popUpPanel.add(radioButtonPanel, "growx, spanx 2");

                    while (true)
                    {

                        final int result = JOptionPane.showConfirmDialog(NetPlanViewTableComponent_trafficMatrix.this, popUpPanel, "Model parameters", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                        if (result != JOptionPane.OK_OPTION)
                            return null;

                        try
                        {
                            double randomFactor;
                            try
                            {
                                randomFactor = txt_randomFactor.getValue();
                                if ((randomFactor > 1) || (randomFactor < 0)) throw new Exception();
                            } catch (Throwable e1)
                            {
                                throw new IllegalArgumentException("Random factor should be a number between 0 and 1 (both included)");
                            }

                            double distanceOffset;
                            try
                            {
                                distanceOffset = txt_distanceOffset.getValue();
                                if (distanceOffset < 0) throw new Exception();
                            } catch (Throwable e1)
                            {
                                throw new IllegalArgumentException("Distance offset should be a non-negative number");
                            }

                            double distancePower;
                            try
                            {
                                distancePower = txt_distancePower.getValue();
                            } catch (Throwable e1)
                            {
                                throw new IllegalArgumentException("Distance power is not a valid number");
                            }

                            double populationOffset;
                            try
                            {
                                populationOffset = txt_populationOffset.getValue();
                                if (populationOffset < 0) throw new Exception();
                            } catch (Throwable e1)
                            {
                                throw new IllegalArgumentException("Population offset should be a non-negative number");
                            }

                            double populationPower;
                            try
                            {
                                populationPower = txt_populationPower.getValue();
                            } catch (Throwable e1)
                            {
                                throw new IllegalArgumentException("Population power is not a valid number");
                            }

                            DoubleMatrix2D levelMatrix = DoubleFactory2D.dense.make(1, 1);
                            levelMatrix.set(0, 0, 1);

                            int[] levelVector = new int[N];
                            Arrays.fill(levelVector, 1);

                            double[] populationVector = new double[N];
                            for (int i = 0; i < populationVector.length; i++)
                                populationVector[i] = filteredNodes.get(i).getPopulation();


                            DoubleMatrix2D distanceMatrix = DoubleFactory2D.dense.make(N, N);
                            for (int i = 0; i < filteredNodes.size(); i++)
                            {
                                for (int j = 0; j < filteredNodes.size(); j++)
                                {
                                    final double distance = euclideanDistance.isSelected() ? netPlan.getNodePairEuclideanDistance(filteredNodes.get(i), filteredNodes.get(j)) : netPlan.getNodePairHaversineDistanceInKm(filteredNodes.get(i), filteredNodes.get(j));
                                    distanceMatrix.set(i, j, distance);
                                }
                            }

                            return TrafficMatrixGenerationModels.populationDistanceModel(distanceMatrix, populationVector, levelVector, levelMatrix
                                    , randomFactor, populationOffset, populationPower, distanceOffset, distancePower
                                    , chk_populationDistanceModelNormalizePopulation.isSelected(), chk_populationDistanceModelNormalizeDistance.isSelected());
                        } catch (Throwable ex)
                        {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error applying population-distance model");
                        }
                    }
                }

                default:
                    throw new RuntimeException("Bad");
            }
        }
    }

    private class ApplyTrafficNormalizationsAndAdjustments
    {
        private final List<Node> filteredNodes;
        private final Set<Demand> filteredDemands;

        ApplyTrafficNormalizationsAndAdjustments(List<Node> filteredNodes, Set<Demand> filteredDemands)
        {
            this.filteredDemands = filteredDemands;
            this.filteredNodes = filteredNodes;
        }

        DoubleMatrix2D applyOption(int selectedOptionIndex)
        {
            if (selectedOptionIndex == 0)
            {
                ErrorHandling.showWarningDialog("Please, select a valid traffic normalization/adjustment method", "Error applying method");
                return null;
            }
            final int N = filteredNodes.size();
            switch (selectedOptionIndex)
            {
                case OPTIONINDEX_NORMALIZATION_SCALE:
                {
                    while (true)
                    {
                        final JNumberField txt_scalingValue = new JNumberField();
                        txt_scalingValue.setToolTipText("x >= 0");

                        final JPanel pane = new JPanel(new MigLayout("fill"));
                        pane.add(new JLabel("Multiply cells by factor: "), "align label");
                        pane.add(txt_scalingValue, "growx");

                        final int result = JOptionPane.showConfirmDialog(null, pane, "Please enter the traffic scaling factor", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (result != JOptionPane.OK_OPTION) return null;

                        try
                        {
                            final double constantValue = txt_scalingValue.getValue();
                            if (constantValue < 0)
                                throw new IllegalArgumentException("Scaling value must be greater or equal than zero");

                            final DoubleMatrix2D res = DoubleFactory2D.sparse.make(N, N);
                            for (int n1 = 0; n1 < N; n1++)
                                for (int n2 = 0; n2 < N; n2++)
                                    if (n1 != n2)
                                        res.set(n1, n2, ((Double) trafficMatrixTable.getValueAt(n1, n2 + 1)) * constantValue);

                            return res;
                        } catch (NumberFormatException e)
                        {
                            ErrorHandling.showWarningDialog("The factor must be a number.", "Invalid value");
                        }
                    }
                }
                case OPTIONINDEX_NORMALIZATION_MAKESYMMETRIC:
                {
                    final DoubleMatrix2D res = DoubleFactory2D.sparse.make(N, N);
                    for (int n1 = 0; n1 < N; n1++)
                        for (int n2 = 0; n2 < N; n2++)
                            if (n1 != n2)
                                res.set(n1, n2, ((Double) trafficMatrixTable.getValueAt(n1, n2 + 1) + (Double) trafficMatrixTable.getValueAt(n2, n1 + 1)) / 2.0);
                    return res;
                }
                case OPTIONINDEX_NORMALIZATION_TOTAL:
                {
                    final JNumberField txt_scalingValue = new JNumberField();
                    txt_scalingValue.setToolTipText("x >= 0");
                    final JPanel pane = new JPanel(new MigLayout("fill"));
                    pane.add(new JLabel("Total sum of matrix cells should be: "), "align label");
                    pane.add(txt_scalingValue, "growx, wrap");
                    while (true)
                    {
                        try
                        {
                            final int result = JOptionPane.showConfirmDialog(null, pane, "Please enter the total traffic", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return null;

                            final double constantValue = txt_scalingValue.getValue();
                            if (constantValue < 0)
                                throw new IllegalArgumentException("Traffic value must be greater or equal than zero");

                            final DoubleMatrix2D res = DoubleFactory2D.sparse.make(N, N);
                            final double currentTotalTraffic = filteredDemands.stream().mapToDouble(d -> d.getOfferedTraffic()).sum();
                            for (int n1 = 0; n1 < N; n1++)
                                for (int n2 = 0; n2 < N; n2++)
                                    if (n1 != n2)
                                        res.set(n1, n2, ((Double) trafficMatrixTable.getValueAt(n1, n2 + 1)) * constantValue / currentTotalTraffic);

                            return res;
                        } catch (NumberFormatException e)
                        {
                            ErrorHandling.showWarningDialog("Total sum must be a number.", "Invalid value");
                        }
                    }
                }

                case OPTIONINDEX_NORMALIZATION_RANDOMVARIATION: // Uniform/Gaussian random normalization
                {
                    double cv;
                    double maxRelativeVariation;

                    JNumberField txt_cv = new JNumberField(0.1d, 0, Double.MAX_VALUE, 0.1);
                    JNumberField txt_maxRelativeVariation = new JNumberField(0.2d, 0, Double.MAX_VALUE, 0.1);

                    JRadioButton rb_uniform = new JRadioButton("Uniform normalization");
                    JRadioButton rb_gaussian = new JRadioButton("Gaussian normalization");

                    ButtonGroup radioGroup = new ButtonGroup();
                    radioGroup.add(rb_uniform);
                    radioGroup.add(rb_gaussian);

                    rb_uniform.setSelected(true);

                    JPanel pane = new JPanel(new MigLayout("fill"));

                    pane.add(new JLabel("Coefficient of variation (quotient between standard deviation and mean value):"), "align label");
                    pane.add(txt_cv, "growx, wrap");
                    pane.add(new JLabel("Maximum relative variation (i.e. 0.5 means a maximum relative deviation from seminal value equal 50%):"), "align label");
                    pane.add(txt_maxRelativeVariation, "growx, wrap");

                    final JPanel radioPanel = new JPanel(new GridLayout(2, 1));
                    radioPanel.setBorder(BorderFactory.createTitledBorder("Random Type"));
                    radioPanel.add(rb_uniform);
                    radioPanel.add(rb_gaussian);

                    pane.add(radioPanel, "growx, spanx 2");

                    while (true)
                    {
                        int result = JOptionPane.showConfirmDialog(NetPlanViewTableComponent_trafficMatrix.this, pane, "Please define the following data", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (result != JOptionPane.OK_OPTION) return null;

                        try
                        {
                            cv = txt_cv.getValue();
                            maxRelativeVariation = txt_maxRelativeVariation.getValue();

                            if (cv <= 0)
                                throw new Net2PlanException("Coefficient of variation must be greater than zero");
                            if (maxRelativeVariation <= 0)
                                throw new Net2PlanException("Maximum relative variation must be greater than zero");

                            break;
                        } catch (NumberFormatException | Net2PlanException ex)
                        {
                            if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(ex);
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error generating new matrices");
                        }
                    }

                    final DoubleMatrix2D trafficMatrix = DoubleFactory2D.dense.make(getTrafficMatrix());

                    if (rb_uniform.isSelected())
                        return TrafficMatrixGenerationModels.computeMatricesRandomUniformVariation(trafficMatrix, maxRelativeVariation, 1).get(0);
                    else if (rb_gaussian.isSelected())
                        return TrafficMatrixGenerationModels.computeMatricesRandomGaussianVariation(trafficMatrix, cv, maxRelativeVariation, 1).get(0);
                    else
                        return null;
                }

                case OPTIONINDEX_NORMALIZATION_PERNODETRAFIN: // Column normalization
                case OPTIONINDEX_NORMALIZATION_PERNODETRAFOUT: // Row normalization
                {
                    boolean isOutTraffic = selectedOptionIndex == OPTIONINDEX_NORMALIZATION_PERNODETRAFOUT;

                    DefaultTableModel model = new ClassAwareTableModel()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public boolean isCellEditable(int row, int col)
                        {
                            if (col == 0) return false;
                            return true;
                        }

                        @Override
                        public void setValueAt(Object newValue, int row, int column)
                        {
                            Object oldValue = getValueAt(row, column);

							/* If value doesn't change, exit from function */
                            if (newValue.equals(oldValue)) return;

                            double trafficAmount = (Double) newValue;

                            if (trafficAmount < 0)
                            {
                                ErrorHandling.showErrorDialog("Traffic amount must be greater or equal than zero", "Error introducing traffic amount");
                                return;
                            }

                            super.setValueAt(newValue, row, column);
                        }
                    };

                    String[] header = new String[]{"Node", isOutTraffic ? "Total ingress traffic per node" : "Total egress traffic per node"};

                    Object[][] data = new Object[N][header.length];
                    for (int n = 0; n < N; n++)
                    {
                        data[n][0] = filteredNodes.get(n);
                        data[n][1] = 0.0;
                    }
                    model.setDataVector(data, header);

                    JTable table = new AdvancedJTable(model);
                    table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
                    {
                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
                        {
                            final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                            if (column == 0)
                            {
                                c.setBackground(new Color(200, 200, 200));
                                c.setForeground(Color.BLACK);
                            } else
                            {
                                c.setBackground(table.getBackground());
                                c.setForeground(table.getForeground());
                            }

                            return c;
                        }
                    });

                    JScrollPane sPane = new JScrollPane(table);

                    JPanel pane = new JPanel();
                    pane.add(sPane);

                    int result = JOptionPane.showConfirmDialog(NetPlanViewTableComponent_trafficMatrix.this, pane, isOutTraffic ? "Please enter total ingress traffic per node (one value per row)" : "Please enter total egress traffic per node (one value per row)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return null;

                    double[] newValue = new double[N];
                    for (int n = 0; n < N; n++) newValue[n] = (Double) model.getValueAt(n, 1);

                    // Get traffic data
                    final double[][] trafficMatrix = getTrafficMatrix();

                    if (isOutTraffic)
                        return TrafficMatrixGenerationModels.normalizationPattern_outgoingTraffic(DoubleFactory2D.dense.make(trafficMatrix), newValue);
                    else
                        return TrafficMatrixGenerationModels.normalizationPattern_incomingTraffic(DoubleFactory2D.dense.make(trafficMatrix), newValue);
                }

                case OPTIONINDEX_NORMALIZATION_MAXIMUMSCALEDVERSION: // Maximum traffic that can be carried
                {
                    final NetPlan netPlan = networkViewer.getDesign();

                    String[] options = new String[]
                            {
                                    "Upper bound", "Exact (using JOM)"
                            };

                    String instructions = "<html><body>These methods multiply the reference matrix by a factor <i>alpha</i>, "
                            + "so that the resulting matrix represents the maximum traffic matrix that can be carried "
                            + "by the network.<br>"
                            + "<br>Two methods are available:<ul>"
                            + "<li>Estimated: A method is used that produces a traffic matrix that is equal or larger than the maximum possible matrix.</li>"
                            + "<li>Exact (JOM is used, may take a while): Finds the normalized matrix solving a formulation.</li>"
                            + "</ul></body></html>";

                    final JPanel container = new JPanel(new BorderLayout());
                    final JLabel lb_instructions = new JLabel(instructions);

                    container.add(lb_instructions, BorderLayout.CENTER);

                    int out = JOptionPane.showOptionDialog(NetPlanViewTableComponent_trafficMatrix.this, container, "Select a computation method",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

                    if (out < 0 && out > options.length)
                        return null;

                    DoubleMatrix1D h_d;
                    switch (out)
                    {
                        case 0:
                            h_d = TrafficMatrixGenerationModels.normalizeTraffic_networkCapacity(netPlan); // hops and km are the same
                            break;

                        case 1:
                            String solverName = Configuration.getOption("defaultILPSolver");
                            String solverLibraryName = Configuration.getDefaultSolverLibraryName(solverName);
                            h_d = TrafficMatrixGenerationModels.normalizeTraffic_linkCapacity_xde(netPlan, solverName, solverLibraryName);
                            break;

                        default:
                            throw new RuntimeException("Bad");
                    }

                    netPlan.setVectorDemandOfferedTraffic(h_d);
                    return netPlan.getMatrixNode2NodeOfferedTraffic();
                }
                default:
                    throw new RuntimeException("Bad");
            }
        }
    }

    private class CommonActionPerformListenerModelAndNormalization implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            try
            {
                final Pair<List<Node>, Set<Demand>> filtInfo = computeFilteringNodesAndDemands();
                final List<Node> filteredNodes = filtInfo.getFirst();
                final Set<Demand> filteredDemands = filtInfo.getSecond();
                if (filteredNodes.size() <= 1) throw new Net2PlanException("No demands are selected");
                if (filteredDemands.isEmpty()) throw new Net2PlanException("No demands are selected");
                boolean allCellsEditable = true;
                for (int row = 0; row < trafficMatrixTable.getRowCount() - 1; row++)
                {
                    for (int col = 1; col < trafficMatrixTable.getColumnCount() - 1; col++)
                    {
                        if (row != col - 1 && !trafficMatrixTable.isCellEditable(row, col))
                        {
                            allCellsEditable = false;
                            break;
                        }
                    }
                }

                if (!allCellsEditable)
                    throw new Net2PlanException("Traffic matrix modification is only possible when all the cells are editable");

                DoubleMatrix2D newTraffic2D = null;

                if (e.getSource() == applyTrafficModelButton)
                    newTraffic2D = new ApplyTrafficModels(filteredNodes).applyOption(cmb_trafficModelPattern.getSelectedIndex());
                else if (e.getSource() == applyTrafficNormalizationButton)
                    newTraffic2D = new ApplyTrafficNormalizationsAndAdjustments(filteredNodes, filteredDemands).applyOption(cmb_trafficNormalization.getSelectedIndex());
                else throw new RuntimeException();

                if (newTraffic2D == null) return;

                final int result = JOptionPane.showConfirmDialog(NetPlanViewTableComponent_trafficMatrix.this, "Overwrite current matrix?", "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (result != JOptionPane.OK_OPTION) return;

                final Map<Node, Integer> node2IndexInFilteredListMap = new HashMap<>();
                for (int cont = 0; cont < filteredNodes.size(); cont++)
                    node2IndexInFilteredListMap.put(filteredNodes.get(cont), cont);
                final List<Demand> filteredDemandList = new ArrayList<>(filteredDemands);
                final List<Double> demandOfferedTrafficsList = new ArrayList<>(filteredDemands.size());

                for (Demand d : filteredDemandList)
                    demandOfferedTrafficsList.add(newTraffic2D.get(node2IndexInFilteredListMap.get(d.getIngressNode()), node2IndexInFilteredListMap.get(d.getEgressNode())));

                if (networkViewer.getVisualizationState().isWhatIfAnalysisActive())
                {
                    final WhatIfAnalysisPane whatIfPane = networkViewer.getWhatIfAnalysisPane();
                    whatIfPane.whatIfDemandOfferedTrafficModified(filteredDemandList, demandOfferedTrafficsList);
                    final VisualizationState vs = networkViewer.getVisualizationState();
                    Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> res =
                            vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<>(networkViewer.getDesign().getNetworkLayers()));
                    vs.setCanvasLayerVisibilityAndOrder(networkViewer.getDesign(), res.getFirst(), res.getSecond());
                    networkViewer.updateVisualizationAfterNewTopology();
                } else
                {
                    for (int cont = 0; cont < filteredDemandList.size(); cont++)
                        filteredDemandList.get(cont).setOfferedTraffic(demandOfferedTrafficsList.get(cont));
                    networkViewer.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                    networkViewer.addNetPlanChange();
                }

            } catch (Net2PlanException ee)
            {
                ErrorHandling.showErrorDialog(ee.getMessage(), "Error");
            } catch (Throwable eee)
            {
                eee.printStackTrace();
                throw new Net2PlanException("Impossible to complete this action: " + eee.getMessage());
            }
        }
    }

    private abstract static class SwitchableItemListener implements ItemListener
    {
        private boolean isEnabled;

        SwitchableItemListener()
        {
            isEnabled = true;
        }


        @Override
        public void itemStateChanged(ItemEvent itemEvent)
        {
            if (isEnabled)
                doAction();
        }

        public void setEnabled(boolean enable)
        {
            isEnabled = enable;
        }

        protected abstract void doAction();
    }
}



