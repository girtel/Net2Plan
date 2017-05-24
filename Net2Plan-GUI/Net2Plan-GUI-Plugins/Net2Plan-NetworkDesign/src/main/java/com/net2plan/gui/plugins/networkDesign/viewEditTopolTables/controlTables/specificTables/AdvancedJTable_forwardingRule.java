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


package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.CellRenderers;
import com.net2plan.gui.plugins.networkDesign.ElementSelection;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AggregationUtils;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.LastRowAggregatedValue;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.JScrollPopupMenu;
import com.net2plan.gui.utils.StringLabeller;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.IPUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;

/**
 */
@SuppressWarnings("unchecked")
public class AdvancedJTable_forwardingRule extends AdvancedJTable_networkElement
{
    private static final String netPlanViewTabName = "Forwarding rules";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Node", "Demand", "Outgoing link", "Splitting ratio", "Carried traffic");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Node where the forwarding rule is installed", "Demand", "Outgoing link", "Percentage of the traffic entering the node going through the outgoing link", "Carried traffic in this link for the demand");
    private static final int COLUMN_NODE = 0;
    public static final int COLUMN_DEMAND = 1;
    public static final int COLUMN_OUTGOINGLINK = 2;
    private static final int COLUMN_SPLITTINGRATIO = 3;
    private static final int COLUMN_CARRIEDTRAFFIC = 4;

    public AdvancedJTable_forwardingRule(final GUINetworkDesign callback)
    {
        super(createTableModel(callback), callback, NetworkElementType.FORWARDING_RULE);
        setDefaultCellRenderers(callback);
        setSpecificCellRenderers();
        setColumnRowSorting();
        fixedTable.setDefaultRenderer(Boolean.class, this.getDefaultRenderer(Boolean.class));
        fixedTable.setDefaultRenderer(Double.class, this.getDefaultRenderer(Double.class));
        fixedTable.setDefaultRenderer(Object.class, this.getDefaultRenderer(Object.class));
        fixedTable.setDefaultRenderer(Float.class, this.getDefaultRenderer(Float.class));
        fixedTable.setDefaultRenderer(Long.class, this.getDefaultRenderer(Long.class));
        fixedTable.setDefaultRenderer(Integer.class, this.getDefaultRenderer(Integer.class));
        fixedTable.setDefaultRenderer(String.class, this.getDefaultRenderer(String.class));
        fixedTable.getTableHeader().setDefaultRenderer(new CellRenderers.FixedTableHeaderRenderer());
    }

    public List<Object[]> getAllData(NetPlan currentState, ArrayList<String> attributesColumns)
    {
        final List<Pair<Demand, Link>> rowVisibleFRs = getVisibleElementsInTable();
        List<Object[]> allForwardingRuleData = new LinkedList<Object[]>();
        final double[] dataAggregator = new double[netPlanViewTableHeader.length];

        for (Pair<Demand, Link> demandLinkPair : rowVisibleFRs)
        {
            Demand demand = demandLinkPair.getFirst();
            Node ingressNode = demand.getIngressNode();
            Node egressNode = demand.getEgressNode();
            String ingressNodeName = ingressNode.getName();
            String egressNodeName = egressNode.getName();

            Link link = demandLinkPair.getSecond();
            Node originNode = link.getOriginNode();
            Node destinationNode = link.getDestinationNode();
            String originNodeName = originNode.getName();
            String destinationNodeName = destinationNode.getName();

            Object[] forwardingRuleData = new Object[netPlanViewTableHeader.length];
            forwardingRuleData[COLUMN_NODE] = originNode.getIndex() + (originNodeName.isEmpty() ? "" : " (" + originNodeName + ")");
            forwardingRuleData[COLUMN_DEMAND] = demand.getIndex() + " (" + ingressNode.getIndex() + (ingressNodeName.isEmpty() ? "" : " (" + ingressNodeName + ")") + " -> " + egressNode.getIndex() + (egressNodeName.isEmpty() ? "" : " (" + egressNodeName + ")") + ")";
            forwardingRuleData[COLUMN_OUTGOINGLINK] = link.getIndex() + " (" + originNode.getIndex() + (originNodeName.isEmpty() ? "" : " (" + originNodeName + ")") + " -> " + destinationNode.getIndex() + (destinationNodeName.isEmpty() ? "" : " (" + destinationNodeName + ")") + ")";
            forwardingRuleData[COLUMN_SPLITTINGRATIO] = currentState.getForwardingRuleSplittingFactor(demand, link);
            forwardingRuleData[COLUMN_CARRIEDTRAFFIC] = currentState.getForwardingRuleCarriedTraffic(demand, link);

            AggregationUtils.updateRowSum(dataAggregator, COLUMN_CARRIEDTRAFFIC, forwardingRuleData[COLUMN_CARRIEDTRAFFIC]);

            allForwardingRuleData.add(forwardingRuleData);
        }

        /* Add the aggregation row with the aggregated statistics */
        final LastRowAggregatedValue[] aggregatedData = new LastRowAggregatedValue[netPlanViewTableHeader.length + attributesColumns.size()];
        Arrays.fill(aggregatedData, new LastRowAggregatedValue());
        aggregatedData[COLUMN_CARRIEDTRAFFIC] = new LastRowAggregatedValue(dataAggregator[COLUMN_CARRIEDTRAFFIC]); // sum
        allForwardingRuleData.add(aggregatedData);

        return allForwardingRuleData;
    }

    public String getTabName()
    {
        return netPlanViewTabName;
    }

    public String[] getTableHeaders()
    {
        return netPlanViewTableHeader;
    }

    public String[] getCurrentTableHeaders()
    {
        ArrayList<String> attColumnsHeaders = getAttributesColumnsHeaders();
        String[] headers = new String[netPlanViewTableHeader.length + attColumnsHeaders.size()];
        for (int i = 0; i < headers.length; i++)
        {
            if (i < netPlanViewTableHeader.length)
            {
                headers[i] = netPlanViewTableHeader[i];
            } else
            {
                headers[i] = "Att: " + attColumnsHeaders.get(i - netPlanViewTableHeader.length);
            }
        }


        return headers;
    }

    public String[] getTableTips()
    {
        return netPlanViewTableTips;
    }

    public boolean hasElements()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
        return rf == null ? callback.getDesign().hasForwardingRules(layer) : rf.hasForwardingRules(layer);
    }

    @Override
    public int getAttributesColumnIndex()
    {
        return 0;
    }

    private static TableModel createTableModel(final GUINetworkDesign callback)
    {
        TableModel forwardingRuleTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                if (!callback.getVisualizationState().isNetPlanEditable()) return false;
                if (getValueAt(rowIndex, columnIndex) == null) return false;
                if (rowIndex == getRowCount() - 1) return false; // the last row is for the aggregated info

                return columnIndex == COLUMN_SPLITTINGRATIO || columnIndex >= netPlanViewTableHeader.length;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column)
            {
                Object oldValue = getValueAt(row, column);

                if (newValue.equals(oldValue)) return;

                NetPlan netPlan = callback.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final Pair<Long, Long> forwardingRule = Pair.of((Long) getValueAt(row, 1), (Long) getValueAt(row, 2));
                final Demand demand = netPlan.getDemandFromId(forwardingRule.getFirst());
                final Link link = netPlan.getLinkFromId(forwardingRule.getSecond());

				/* Perform checks, if needed */
                try
                {
                    switch (column)
                    {
                        case COLUMN_SPLITTINGRATIO:
                            netPlan.setForwardingRule(demand, link, Double.parseDouble(newValue.toString()));
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.FORWARDING_RULE));
                            callback.getVisualizationState().pickForwardingRule(Pair.of(demand, link));
                            callback.updateVisualizationAfterPick();
                            callback.addNetPlanChange();
                            break;

                        default:
                            break;
                    }

                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying forwarding rule");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };
        return forwardingRuleTableModel;
    }

    private void setDefaultCellRenderers(final GUINetworkDesign callback)
    {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());

        setDefaultRenderer(Boolean.class, new CellRenderers.ForwardingRuleRenderer(getDefaultRenderer(Boolean.class), callback));
        setDefaultRenderer(Double.class, new CellRenderers.ForwardingRuleRenderer(getDefaultRenderer(Double.class), callback));
        setDefaultRenderer(Object.class, new CellRenderers.ForwardingRuleRenderer(getDefaultRenderer(Object.class), callback));
        setDefaultRenderer(Float.class, new CellRenderers.ForwardingRuleRenderer(getDefaultRenderer(Float.class), callback));
        setDefaultRenderer(Long.class, new CellRenderers.ForwardingRuleRenderer(getDefaultRenderer(Long.class), callback));
        setDefaultRenderer(Integer.class, new CellRenderers.ForwardingRuleRenderer(getDefaultRenderer(Integer.class), callback));
        setDefaultRenderer(String.class, new CellRenderers.ForwardingRuleRenderer(getDefaultRenderer(String.class), callback));
    }

    private void setSpecificCellRenderers()
    {
    }

    @Override
    public void setColumnRowSorting()
    {
        setAutoCreateRowSorter(true);
        final Set<Integer> columnsWithDoubleAndThenParenthesis = Sets.newHashSet(COLUMN_NODE, COLUMN_DEMAND, COLUMN_OUTGOINGLINK);
        DefaultRowSorter rowSorter = ((DefaultRowSorter) getRowSorter());
        for (int col = 0; col <= COLUMN_CARRIEDTRAFFIC; col++)
            rowSorter.setComparator(col, new AdvancedJTable_networkElement.ColumnComparator(rowSorter, columnsWithDoubleAndThenParenthesis.contains(col)));
        fixedTable.setAutoCreateRowSorter(true);
        fixedTable.setRowSorter(this.getRowSorter());
        rowSorter = ((DefaultRowSorter) fixedTable.getRowSorter());
        for (int col = 0; col <= COLUMN_CARRIEDTRAFFIC; col++)
            rowSorter.setComparator(col, new AdvancedJTable_networkElement.ColumnComparator(rowSorter, columnsWithDoubleAndThenParenthesis.contains(col)));
    }

    public int getNumberOfDecoratorColumns()
    {
        return 2;
    }

    @Override
    public ArrayList<String> getAttributesColumnsHeaders()
    {
        return new ArrayList<String>();
    }


    public void pickSelection(ElementSelection selection)
    {
        if (selection.getElementType() != NetworkElementType.FORWARDING_RULE)
            throw new RuntimeException("Unmatched items with table, selected items are of type: " + selection.getElementType());

        callback.getVisualizationState().pickForwardingRule(selection.getForwardingRules());
        callback.updateVisualizationAfterPick();
    }

    @Override
    protected boolean hasAttributes()
    {
        return false;
    }

    public JPopupMenu getPopup(final ElementSelection selection)
    {
        assert selection != null;

        final JScrollPopupMenu popup = new JScrollPopupMenu(20);
        final List<Pair<Demand, Link>> frRowsInTheTable = getVisibleElementsInTable();

        if (!selection.isEmpty())
            if (selection.getElementType() != NetworkElementType.FORWARDING_RULE)
                throw new RuntimeException("Unmatched items with table, selected items are of type: " + selection.getElementType());

        /* Add the popup menu option of the filters */
        final List<Pair<Demand, Link>> selectedFRs = selection.getForwardingRules();
        if (!frRowsInTheTable.isEmpty())
        {
        	addPickOption(selection, popup);
            addFilterOptions(selection, popup);
            popup.addSeparator();
        }

        if (callback.getVisualizationState().isNetPlanEditable())
        {
            popup.add(getAddOption());

            if (!frRowsInTheTable.isEmpty())
            {
                if (!selectedFRs.isEmpty())
                {
                    if (popup.getSubElements().length > 0) popup.addSeparator();

                    JMenuItem removeItem = new JMenuItem("Remove selected forwarding rules");
                    removeItem.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            NetPlan netPlan = callback.getDesign();

                            try
                            {
                                for (Pair<Demand, Link> selectedFR : selectedFRs)
                                    netPlan.setForwardingRule(selectedFR.getFirst(), selectedFR.getSecond(), 0);

                                callback.getVisualizationState().resetPickedState();
                                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.FORWARDING_RULE));
                                callback.addNetPlanChange();
                            } catch (Throwable ex)
                            {
                                ErrorHandling.addErrorOrException(ex, getClass());
                                ErrorHandling.showErrorDialog("Unable to remove " + networkElementType);
                            }
                        }
                    });

                    popup.add(removeItem);
                }
            }

            for (JComponent item : getExtraAddOptions())
                popup.add(item);
        }

        return popup;
    }


    @Override
    protected JMenuItem getAddOption()
    {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(e ->
        {
            try
            {
                createForwardingRuleGUI(callback);
                callback.getVisualizationState().resetPickedState();
                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.FORWARDING_RULE));
                callback.addNetPlanChange();
            } catch (Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
            }
        });

        NetPlan netPlan = callback.getDesign();
        if (!netPlan.hasLinks() || !netPlan.hasDemands()) addItem.setEnabled(false);

        return addItem;
    }

    private static void createForwardingRuleGUI(final GUINetworkDesign callback)
    {
        final NetPlan netPlan = callback.getDesign();
        final JComboBox nodeSelector = new WiderJComboBox();
        final JComboBox linkSelector = new WiderJComboBox();
        final JComboBox demandSelector = new WiderJComboBox();
        final JTextField txt_splittingRatio = new JTextField(5);

        ItemListener nodeListener = new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                JComboBox me = (JComboBox) e.getSource();
                linkSelector.removeAllItems();
                long nodeId = (long) ((StringLabeller) me.getSelectedItem()).getObject();
                Set<Link> links = netPlan.getNodeFromId(nodeId).getOutgoingLinksAllLayers();
                for (Link link : links)
                {
                    String originNodeLabel = "Node " + link.getOriginNode().getId();
                    if (!link.getOriginNode().getName().isEmpty())
                        originNodeLabel += " (" + link.getOriginNode().getName() + ")";
                    String destinationNodeLabel = "Node " + link.getDestinationNode().getId();
                    if (!link.getDestinationNode().getName().isEmpty())
                        destinationNodeLabel += " (" + link.getDestinationNode().getName() + ")";
                    String linkLabel = "e" + link.getId() + ": " + originNodeLabel + " -> " + destinationNodeLabel;
                    linkSelector.addItem(StringLabeller.of(link.getId(), linkLabel));
                }

                linkSelector.setSelectedIndex(0);
            }
        };

        ItemListener linkDemandListener = new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                Demand demand = netPlan.getDemandFromId((long) ((StringLabeller) demandSelector.getSelectedItem()).getObject());
                Link link = netPlan.getLinkFromId((long) ((StringLabeller) linkSelector.getSelectedItem()).getObject());
                double splittingRatio;
                if (netPlan.getForwardingRuleSplittingFactor(demand, link) > 0)
                {
                    splittingRatio = netPlan.getForwardingRuleSplittingFactor(demand, link);
                } else
                {
                    Node node = link.getOriginNode();
                    Map<Pair<Demand, Link>, Double> forwardingRules_thisNode = node.getForwardingRules(demand);
                    double totalSplittingRatio = 0;
                    for (Double value : forwardingRules_thisNode.values()) totalSplittingRatio += value;

                    splittingRatio = Math.max(0, 1 - totalSplittingRatio);
                }

                txt_splittingRatio.setText(Double.toString(splittingRatio));
            }
        };

        nodeSelector.addItemListener(nodeListener);

        for (Node node : netPlan.getNodes())
        {
            if (node.getOutgoingLinks().isEmpty()) continue;

            final String nodeName = node.getName();
            String nodeLabel = "Node " + node.getId();
            if (!nodeName.isEmpty()) nodeLabel += " (" + nodeName + ")";

            nodeSelector.addItem(StringLabeller.of(node.getId(), nodeLabel));
        }

        linkSelector.addItemListener(linkDemandListener);
        demandSelector.addItemListener(linkDemandListener);

        for (Demand demand : netPlan.getDemands())
        {
            String ingressNodeLabel = "Node " + demand.getIngressNode().getId();
            if (!demand.getIngressNode().getName().isEmpty())
                ingressNodeLabel += " (" + demand.getIngressNode().getName() + ")";
            String egressNodeLabel = "Node " + demand.getEgressNode().getId();
            if (!demand.getEgressNode().getName().isEmpty())
                egressNodeLabel += " (" + demand.getEgressNode().getName() + ")";
            String demandLabel = "d" + demand.getId() + ": " + ingressNodeLabel + " -> " + egressNodeLabel;
            demandSelector.addItem(StringLabeller.of(demand.getId(), demandLabel));
        }

        nodeSelector.setSelectedIndex(0);
        demandSelector.setSelectedIndex(0);

        JPanel pane = new JPanel(new MigLayout("fill", "[][grow]", "[][][][][]"));
        pane.add(new JLabel("Node where to install the rule: "));
        pane.add(nodeSelector, "wrap");
        pane.add(new JLabel("Outgoing link: "));
        pane.add(linkSelector, "wrap");
        pane.add(new JLabel("Demand: "));
        pane.add(demandSelector, "wrap");
        pane.add(new JLabel("Splitting ratio: "));
        pane.add(txt_splittingRatio, "wrap");

        while (true)
        {
            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter information for the new forwarding rule", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            try
            {
                long demandId = (long) ((StringLabeller) demandSelector.getSelectedItem()).getObject();
                long linkId = (long) ((StringLabeller) linkSelector.getSelectedItem()).getObject();

                double splittingRatio;
                try
                {
                    splittingRatio = Double.parseDouble(txt_splittingRatio.getText());
                    if (splittingRatio <= 0) throw new RuntimeException();
                } catch (Throwable e)
                {
                    ErrorHandling.showErrorDialog("Splitting ratio must be a non-negative non-zero number", "Error adding forwarding rule");
                    continue;
                }

                netPlan.setForwardingRule(netPlan.getDemandFromId(demandId), netPlan.getLinkFromId(linkId), splittingRatio);
                break;
            } catch (Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Error adding forwarding rule");
            }
        }
    }


    @Override
    protected List<JComponent> getExtraAddOptions()
    {
        List<JComponent> options = new LinkedList<JComponent>();
        NetPlan netPlan = callback.getDesign();

        final JMenuItem ecmpRouting = new JMenuItem("Generate ECMP forwarding rules from link IGP weights");
        options.add(ecmpRouting);

        ecmpRouting.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                NetPlan netPlan = callback.getDesign();
                DoubleMatrix1D linkWeightMap = IPUtils.getLinkWeightVector(netPlan);
                IPUtils.setECMPForwardingRulesFromLinkWeights(netPlan, linkWeightMap);
                callback.getVisualizationState().resetPickedState();
                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.FORWARDING_RULE));
                callback.addNetPlanChange();
            }
        });

        if (!netPlan.hasLinks() || !netPlan.hasDemands()) ecmpRouting.setEnabled(false);

        return options;
    }


    @Override
    protected List<JComponent> getForcedOptions(ElementSelection selection)
    {
        return new LinkedList<>();
    }


    @Override
    protected List<JComponent> getExtraOptions(ElementSelection selection)
    {
        return new LinkedList<>();
    }

    private List<Pair<Demand, Link>> getVisibleElementsInTable()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
        return rf == null ? new ArrayList<>(callback.getDesign().getForwardingRules(layer).keySet()) : rf.getVisibleForwardingRules(layer);
    }
}
