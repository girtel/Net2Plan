/* Pending
 * 1) GUI
 * 2) Make pending functionalities
 * 3) Add functionality of exporting to Excel the traffic matrix
 */

package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.rightPanelTabs;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.CellRenderers;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.plugins.networkDesign.whatIfAnalysisPane.WhatIfAnalysisPane;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.TrafficMatrixGenerationModels;
import com.net2plan.utils.Pair;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.collections15.BidiMap;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class NetPlanViewTableComponent_trafMatrix extends JPanel
{
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

    private final JCheckBox filterOutNodesNotConnectedThisLayer;
    private final JComboBox<String> cmb_tagNodesSelector;
    private final JComboBox<String> cmb_tagDemandsSelector;
    private final JComboBox<String> cmb_trafficModelPattern;
    private final JComboBox<String> cmb_trafficNormalization;
    private final JButton applyTrafficNormalizationButton;
    private final JButton applyTrafficModelButton;
    private final GUINetworkDesign networkViewer;

    public NetPlanViewTableComponent_trafMatrix(GUINetworkDesign networkViewer)
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
        cmb_trafficModelPattern = new WiderJComboBox();
        cmb_trafficModelPattern.addItem("Select a method for synthesizing a matrix");
        cmb_trafficModelPattern.addItem("1. Constant");
        cmb_trafficModelPattern.addItem("2. Uniform (0, 10)");
        cmb_trafficModelPattern.addItem("3. 50% Uniform (0, 100) & 50% Uniform(0, 10)");
        cmb_trafficModelPattern.addItem("4. 25% Uniform (0, 100) & 75% Uniform(0, 10)");
        cmb_trafficModelPattern.addItem("5. Gravity model");
        cmb_trafficModelPattern.addItem("6. Population-distance model");
        cmb_trafficModelPattern.addItem("7. Reset");
        pnl_trafficModel.add(cmb_trafficModelPattern);
        this.applyTrafficModelButton = new JButton("Apply");
        applyTrafficModelButton.addActionListener(new CommonActionPerformListenerModelAndNormalization());
        pnl_trafficModel.setLayout(new MigLayout("insets 0 0 0 0", "[grow][][]", "[grow]"));
        pnl_trafficModel.add(cmb_trafficModelPattern, "grow, wmin 50");
        pnl_trafficModel.add(applyTrafficModelButton);

        JPanel pnl_normalization = new JPanel();
        pnl_normalization.setBorder(BorderFactory.createTitledBorder("Traffic normalization and adjustments"));
        cmb_trafficNormalization = new WiderJComboBox();
        cmb_trafficNormalization.addItem("Select a method");
        cmb_trafficNormalization.addItem("1. Make symmetric");
        cmb_trafficNormalization.addItem("2. Scale by a factor");
        cmb_trafficNormalization.addItem("3. Apply random variation (truncated gaussian)");
        cmb_trafficNormalization.addItem("4. Normalization: fit to given total traffic");
        cmb_trafficNormalization.addItem("5. Normalization: fit to given out traffic per node");
        cmb_trafficNormalization.addItem("6. Normalization: fit to given in traffic per node");
        cmb_trafficNormalization.addItem("7. Normalization: scale to theoretical maximum traffic");
        pnl_normalization.add(cmb_trafficNormalization);
        this.applyTrafficNormalizationButton = new JButton("Apply");
        applyTrafficNormalizationButton.addActionListener(new CommonActionPerformListenerModelAndNormalization());
        pnl_normalization.setLayout(new MigLayout("insets 0 0 0 0", "[grow][][]", "[grow]"));
        pnl_normalization.add(cmb_trafficNormalization, "grow, wmin 50");
        pnl_normalization.add(applyTrafficNormalizationButton);

        final JPanel filterPanel = new JPanel(new MigLayout("wrap 2", "[][grow]"));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filters"));

        this.filterOutNodesNotConnectedThisLayer = new JCheckBox();
        filterPanel.add(new JLabel("Filter out linkless nodes at this layer"), "align label");
        filterPanel.add(filterOutNodesNotConnectedThisLayer);

        this.cmb_tagNodesSelector = new WiderJComboBox();
        this.cmb_tagDemandsSelector = new WiderJComboBox();

        cmb_tagDemandsSelector.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent itemEvent)
            {
                updateNetPlanView();
            }
        });

        cmb_tagDemandsSelector.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent itemEvent)
            {
                updateNetPlanView();
            }
        });

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

    private Pair<List<Node>, Set<Demand>> computeFilteringNodesAndDemands()
    {
        final NetPlan np = networkViewer.getDesign();
        final List<Node> filteredNodes = new ArrayList<>(np.getNumberOfNodes());
        final String filteringNodeTag = this.cmb_tagNodesSelector.getSelectedIndex() == 0 ? null : (String) this.cmb_tagNodesSelector.getSelectedItem();
        final String filteringDemandTag = this.cmb_tagDemandsSelector.getSelectedIndex() == 0 ? null : (String) this.cmb_tagDemandsSelector.getSelectedItem();
        for (Node n : np.getNodes())
        {
            if (filteringNodeTag != null)
                if (!n.hasTag(filteringNodeTag)) continue;
            if (this.filterOutNodesNotConnectedThisLayer.isSelected())
                if (!n.getOutgoingLinksAllLayers().stream().anyMatch(e -> e.getLayer().isDefaultLayer())
                        && (!n.getIncomingLinksAllLayers().stream().anyMatch(e -> e.getLayer().isDefaultLayer())))
                    continue;
            filteredNodes.add(n);
        }
        Set<Demand> filteredDemands;
        if (filteringDemandTag == null) filteredDemands = new HashSet<>(np.getDemands());
        else
        {
            filteredDemands = new HashSet<>();
            for (Demand d : np.getDemands())
                if (d.hasTag(filteringDemandTag)) filteredDemands.add(d);
        }
        return Pair.of(filteredNodes, filteredDemands);
    }

    public void updateNetPlanView()
    {
        final NetPlan np = networkViewer.getDesign();
        final Pair<List<Node>, Set<Demand>> filtInfo = computeFilteringNodesAndDemands();
        final List<Node> filteredNodes = filtInfo.getFirst();
        final Set<Demand> filteredDemands = filtInfo.getSecond();
        this.trafficMatrixTable.setModel(createTrafficMatrix(filteredNodes, filteredDemands));

        cmb_tagNodesSelector.removeAllItems();
        cmb_tagNodesSelector.addItem("[NO FILTER]");
        final Set<String> allTagsNodes = np.getNodes().stream().map(n -> n.getTags()).flatMap(e -> e.stream()).collect(Collectors.toSet());
        final List<String> allTagsNodesOrdered = allTagsNodes.stream().sorted().collect(Collectors.toList());
        for (String tag : allTagsNodesOrdered) this.cmb_tagNodesSelector.addItem(tag);

        cmb_tagDemandsSelector.removeAllItems();
        cmb_tagDemandsSelector.addItem("[NO FILTER]");
        final Set<String> allTagsDemands = np.getDemands().stream().map(n -> n.getTags()).flatMap(e -> e.stream()).collect(Collectors.toSet());
        final List<String> allTagsDemandsOrdered = allTagsDemands.stream().sorted().collect(Collectors.toList());
        for (String tag : allTagsDemandsOrdered) this.cmb_tagDemandsSelector.addItem(tag);
    }

    private DefaultTableModel createTrafficMatrix(List<Node> filteredNodes, Set<Demand> filteredDemands)
    {
        final NetPlan np = this.networkViewer.getDesign();
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

    private class ApplyTrafficModels
    {
        private final List<Node> filteredNodes;
        private final Set<Demand> filteredDemands;

        ApplyTrafficModels(List<Node> filteredNodes, Set<Demand> filteredDemands)
        {
            this.filteredDemands = filteredDemands;
            this.filteredNodes = filteredNodes;
        }

        DoubleMatrix2D applyOption(int selectedOptionIndex)
        {
            if (selectedOptionIndex == 0)
            {
                ErrorHandling.showWarningDialog("Please, select a traffic model", "Error applying traffic model");
                return null;
            }
            final NetPlan np = networkViewer.getDesign();
            final int N = filteredNodes.size();
            switch (selectedOptionIndex)
            {
                case OPTIONINDEX_TRAFFICMODEL_CONSTANT:
                    final JTextField txt_constantValue = new JTextField(5);
                    final JPanel pane = new JPanel(new GridLayout(0, 2));
                    pane.add(new JLabel("Traffic per cell: "));
                    pane.add(txt_constantValue);
                    while (true)
                    {
                        int result = JOptionPane.showConfirmDialog(null, pane, "Please enter the traffic per cell", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (result != JOptionPane.OK_OPTION) return null;
                        final double constantValue = Double.parseDouble(txt_constantValue.getText());
                        if (constantValue < 0)
                            throw new IllegalArgumentException("Constant value must be greater or equal than zero");
                        return TrafficMatrixGenerationModels.constantTrafficMatrix(N, constantValue);
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
                            super.setValueAt(newValue, row, column);
                        }
                    };

                    Object[][] gravityModelData = new Object[N][2];
                    for (int n = 0; n < N; n++)
                    {
                        gravityModelData[n][0] = 0.0;
                        gravityModelData[n][1] = 0.0;
                    }

                    String[] gravityModelHeader = new String[]{"Total ingress traffic per node", "Total egress traffic per node"};
                    gravityModelTableModel.setDataVector(gravityModelData, gravityModelHeader);

                    JTable gravityModelTable = new AdvancedJTable(gravityModelTableModel);

                    JPanel gravityModelPanel = new JPanel();
                    JScrollPane gPane = new JScrollPane(gravityModelTable);
                    gravityModelPanel.add(gPane);

                    double[] ingressTrafficPerNode = new double[N];
                    double[] egressTrafficPerNode = new double[N];
                    while (true)
                    {
                        int gravityModelResult = JOptionPane.showConfirmDialog(null, gravityModelPanel, "Please enter total ingress/egress traffic per node (one value per row)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (gravityModelResult != JOptionPane.OK_OPTION) return null;

                        for (int n = 0; n < N; n++)
                        {
                            ingressTrafficPerNode[n] = (Double) gravityModelTableModel.getValueAt(n, 0);
                            egressTrafficPerNode[n] = (Double) gravityModelTableModel.getValueAt(n, 1);
                        }
                        try
                        {
                            return TrafficMatrixGenerationModels.gravityModel(ingressTrafficPerNode, egressTrafficPerNode);
                        } catch (Throwable ex)
                        {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error applying gravity model");
                        }
                    }
                default:
                    throw new RuntimeException("Bad");
            }
        }
    }

    ;

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
            final NetPlan np = networkViewer.getDesign();
            final int N = filteredNodes.size();
            switch (selectedOptionIndex)
            {
                case OPTIONINDEX_NORMALIZATION_SCALE:
                {
                    final JTextField txt_scalingValue = new JTextField(10);
                    final JPanel pane = new JPanel(new GridLayout(0, 2));
                    pane.add(new JLabel("Multiply cells by factor: "));
                    pane.add(txt_scalingValue);
                    final int result = JOptionPane.showConfirmDialog(null, pane, "Please enter the traffic saling factor", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return null;
                    final double constantValue = Double.parseDouble(txt_scalingValue.getText());
                    if (constantValue < 0)
                        throw new IllegalArgumentException("Scaling value must be greater or equal than zero");
                    final DoubleMatrix2D res = DoubleFactory2D.sparse.make(N, N);
                    for (int n1 = 0; n1 < N; n1++)
                        for (int n2 = 0; n2 < N; n2++)
                            if (n1 != n2)
                                res.set(n1, n2, ((Double) trafficMatrixTable.getValueAt(n1, n2 + 1)) * constantValue);
                    return res;
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
                    final JTextField txt_scalingValue = new JTextField(10);
                    final JPanel pane = new JPanel(new GridLayout(0, 2));
                    pane.add(new JLabel("Total sum of matrix cells should be: "));
                    pane.add(txt_scalingValue);
                    final int result = JOptionPane.showConfirmDialog(null, pane, "Please enter the total traffic", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return null;
                    final double constantValue = Double.parseDouble(txt_scalingValue.getText());
                    if (constantValue < 0)
                        throw new IllegalArgumentException("Traffic value must be greater or equal than zero");
                    final DoubleMatrix2D res = DoubleFactory2D.sparse.make(N, N);
                    final double currentTotalTraffic = filteredDemands.stream().mapToDouble(d -> d.getOfferedTraffic()).sum();
                    for (int n1 = 0; n1 < N; n1++)
                        for (int n2 = 0; n2 < N; n2++)
                            if (n1 != n2)
                                res.set(n1, n2, ((Double) trafficMatrixTable.getValueAt(n1, n2 + 1)) * constantValue / currentTotalTraffic);
                    return res;
                }
                default:
                    throw new RuntimeException("Bad");
            }
        }
    }

    ;

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
                    for (int col = 1; col < trafficMatrixTable.getColumnCount() - 1; col++)
                        if (row != col - 1 && !trafficMatrixTable.isCellEditable(row, col))
                        {
                            allCellsEditable = false;
                            break;
                        }
                if (!allCellsEditable)
                    throw new Net2PlanException("Traffic matrix modification is only possible when all the cells are editable");

                DoubleMatrix2D newTraffic2D = null;
                if (e.getSource() == applyTrafficModelButton)
                    newTraffic2D = new ApplyTrafficModels(filteredNodes, filteredDemands).applyOption(cmb_trafficModelPattern.getSelectedIndex());
                else if (e.getSource() == applyTrafficNormalizationButton)
                    newTraffic2D = new ApplyTrafficNormalizationsAndAdjustments(filteredNodes, filteredDemands).applyOption(cmb_trafficNormalization.getSelectedIndex());
                else throw new RuntimeException();
                if (newTraffic2D == null) return;
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
                throw new Net2PlanException("Impossible to complete this action");
            }
        }
    }

}



