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


package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.CellRenderers;
import com.net2plan.gui.plugins.networkDesign.ElementSelection;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AggregationUtils;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.LastRowAggregatedValue;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.plugins.networkDesign.whatIfAnalysisPane.WhatIfAnalysisPane;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.JScrollPopupMenu;
import com.net2plan.gui.utils.StringLabeller;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import org.apache.commons.collections15.BidiMap;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.stream.Collectors;

/**
 */
@SuppressWarnings("unchecked")
public class AdvancedJTable_multicastDemand extends AdvancedJTable_networkElement
{
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_INDEX = 1;
    private static final int COLUMN_INGRESSNODE = 2;
    private static final int COLUMN_EGRESSNODES = 3;
    private static final int COLUMN_COUPLEDTOLINKS = 4;
    private static final int COLUMN_OFFEREDTRAFFIC = 5;
    private static final int COLUMN_CARRIEDTRAFFIC = 6;
    private static final int COLUMN_LOSTTRAFFIC = 7;
    private static final int COLUMN_BIFURCATED = 8;
    private static final int COLUMN_NUMTREES = 9;
    private static final int COLUMN_MAXE2ELATENCY = 10;
    private static final int COLUMN_TAGS = 11;
    private static final int COLUMN_ATTRIBUTES = 12;
    private static final String netPlanViewTabName = "Multicast demands";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "Ingress node", "Egress nodes", "Coupled to links",
            "Offered traffic", "Carried traffic", "% Lost traffic", "Bifurcated", "# Multicast trees", "Max e2e latency (ms)", "Tags", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)", "Index (consecutive integer starting in zero)", "Ingress node", "Egress nodes", "Indicates the coupled upper layer links, if any, or empty", "Offered traffic by the multicast demand", "Carried traffic by multicast trees carrying demand traffic", "Percentage of lost traffic from the offered", "Indicates whether the demand has more than one associated multicast tree carrying traffic", "Number of associated multicast trees", "Maximum end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any)", "Multicast demand-specific tags", "Multicast demand-specific attributes");

    public AdvancedJTable_multicastDemand(final GUINetworkDesign callback)
    {
        super(createTableModel(callback), callback, NetworkElementType.MULTICAST_DEMAND);
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
        List<Object[]> allDemandData = new LinkedList<Object[]>();
        final List<MulticastDemand> rowVisibleDemands = getVisibleElementsInTable();
        final double[] dataAggregator = new double[netPlanViewTableHeader.length];

        for (MulticastDemand demand : rowVisibleDemands)
        {
            final Set<Link> coupledLinks = demand.getCoupledLinks();
            final Node ingressNode = demand.getIngressNode();
            final Set<Node> egressNodes = demand.getEgressNodes();
            final String ingressNodeName = ingressNode.getName();
            String egressNodesString = "";
            for (Node n : egressNodes) egressNodesString += n.getIndex() + "(" + n.getName() + ") ";

            final double h_d = demand.getOfferedTraffic();
            final double lostTraffic_d = demand.getBlockedTraffic();
            Object[] demandData = new Object[netPlanViewTableHeader.length + attributesColumns.size()];
            demandData[COLUMN_ID] = demand.getId();
            demandData[COLUMN_INDEX] = demand.getIndex();
            demandData[COLUMN_INGRESSNODE] = ingressNode.getIndex() + (ingressNodeName.isEmpty() ? "" : " (" + ingressNodeName + ")");
            demandData[COLUMN_EGRESSNODES] = egressNodesString;
            demandData[COLUMN_COUPLEDTOLINKS] = coupledLinks.isEmpty() ? "" : "link ids " + CollectionUtils.join(NetPlan.getIndexes(coupledLinks), ",") + " layer " + coupledLinks.iterator().next().getLayer().getIndex() + "";
            demandData[COLUMN_OFFEREDTRAFFIC] = h_d;
            demandData[COLUMN_CARRIEDTRAFFIC] = demand.getCarriedTraffic();
            demandData[COLUMN_LOSTTRAFFIC] = h_d == 0 ? 0 : 100 * lostTraffic_d / h_d;
            demandData[COLUMN_BIFURCATED] = demand.isBifurcated() ? String.format("Yes (%d)", demand.getMulticastTrees().size()) : "No";
            demandData[COLUMN_NUMTREES] = demand.getMulticastTrees().size();
            demandData[COLUMN_MAXE2ELATENCY] = demand.getWorseCasePropagationTimeInMs();
            demandData[COLUMN_TAGS] = StringUtils.listToString(Lists.newArrayList(demand.getTags()));
            demandData[COLUMN_ATTRIBUTES] = StringUtils.mapToString(demand.getAttributes());
            for (int i = netPlanViewTableHeader.length; i < netPlanViewTableHeader.length + attributesColumns.size(); i++)
            {
                if (demand.getAttributes().containsKey(attributesColumns.get(i - netPlanViewTableHeader.length)))
                {
                    demandData[i] = demand.getAttribute(attributesColumns.get(i - netPlanViewTableHeader.length));
                }
            }

            if (demand.isCoupled()) AggregationUtils.updateRowCount(dataAggregator, COLUMN_COUPLEDTOLINKS, 1);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_OFFEREDTRAFFIC, demandData[COLUMN_OFFEREDTRAFFIC]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_CARRIEDTRAFFIC, demandData[COLUMN_CARRIEDTRAFFIC]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_LOSTTRAFFIC, demandData[COLUMN_LOSTTRAFFIC]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_NUMTREES, demandData[COLUMN_NUMTREES]);
            AggregationUtils.updateRowMax(dataAggregator, COLUMN_MAXE2ELATENCY, demandData[COLUMN_MAXE2ELATENCY]);

            allDemandData.add(demandData);
        }
        
        /* Add the aggregation row with the aggregated statistics */
        final LastRowAggregatedValue[] aggregatedData = new LastRowAggregatedValue[netPlanViewTableHeader.length + attributesColumns.size()];
        Arrays.fill(aggregatedData, new LastRowAggregatedValue());
        aggregatedData[COLUMN_COUPLEDTOLINKS] = new LastRowAggregatedValue(dataAggregator[COLUMN_COUPLEDTOLINKS]);
        aggregatedData[COLUMN_OFFEREDTRAFFIC] = new LastRowAggregatedValue(dataAggregator[COLUMN_OFFEREDTRAFFIC]);
        aggregatedData[COLUMN_CARRIEDTRAFFIC] = new LastRowAggregatedValue(dataAggregator[COLUMN_CARRIEDTRAFFIC]);
        aggregatedData[COLUMN_LOSTTRAFFIC] = new LastRowAggregatedValue(dataAggregator[COLUMN_LOSTTRAFFIC]);
        aggregatedData[COLUMN_NUMTREES] = new LastRowAggregatedValue(dataAggregator[COLUMN_NUMTREES]);
        aggregatedData[COLUMN_MAXE2ELATENCY] = new LastRowAggregatedValue(dataAggregator[COLUMN_MAXE2ELATENCY]);
        allDemandData.add(aggregatedData);


        return allDemandData;
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
        return rf == null ? callback.getDesign().hasMulticastDemands(layer) : rf.hasMulticastDemands(layer);
    }

    @Override
    public int getAttributesColumnIndex()
    {
        return COLUMN_ATTRIBUTES;
    }

//    public int[] getColumnsOfSpecialComparatorForSorting() {
//        return new int[]{3, 4, 5, 9, 10};
//    }

    private static TableModel createTableModel(final GUINetworkDesign callback)
    {
        TableModel multicastDemandTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                if (!callback.getVisualizationState().isNetPlanEditable()) return false;
                if (columnIndex >= netPlanViewTableHeader.length) return true;
                if (rowIndex == getRowCount() - 1) return false;
                if (getValueAt(rowIndex, columnIndex) == null) return false;

                return columnIndex == COLUMN_OFFEREDTRAFFIC || columnIndex >= netPlanViewTableHeader.length;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column)
            {
                Object oldValue = getValueAt(row, column);

				/* If value doesn't change, exit from function */
                if (newValue.equals(oldValue)) return;

                NetPlan netPlan = callback.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long demandId = (Long) getValueAt(row, 0);
                final MulticastDemand demand = netPlan.getMulticastDemandFromId(demandId);

				/* Perform checks, if needed */
                try
                {
                    switch (column)
                    {
                        case COLUMN_OFFEREDTRAFFIC:
                            final double newOfferedTraffic = Double.parseDouble(newValue.toString());
                            if (newOfferedTraffic < 0)
                                throw new Net2PlanException("The demand offered traffic cannot be negative");
                            if (callback.getVisualizationState().isWhatIfAnalysisActive())
                            {
                                final WhatIfAnalysisPane whatIfPane = callback.getWhatIfAnalysisPane();
                                synchronized (whatIfPane)
                                {
                                    whatIfPane.whatIfDemandOfferedTrafficModified(demand, newOfferedTraffic);
                                    if (whatIfPane.getLastWhatIfExecutionException() != null)
                                        throw whatIfPane.getLastWhatIfExecutionException();
                                    whatIfPane.wait(); // wait until the simulation ends
                                    if (whatIfPane.getLastWhatIfExecutionException() != null)
                                        throw whatIfPane.getLastWhatIfExecutionException();

                                    final VisualizationState vs = callback.getVisualizationState();
                                    Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> res =
                                            vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<>(callback.getDesign().getNetworkLayers()));
                                    vs.setCanvasLayerVisibilityAndOrder(callback.getDesign(), res.getFirst(), res.getSecond());
                                    callback.updateVisualizationAfterNewTopology();
                                }
                            } else
                            {
                                demand.setOfferedTraffic(newOfferedTraffic);
                                callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.MULTICAST_DEMAND));
                                callback.getVisualizationState().pickElement(demand);
                                callback.updateVisualizationAfterPick();
                                callback.addNetPlanChange();
                            }
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying multicast demand");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };
        return multicastDemandTableModel;
    }

    private void setDefaultCellRenderers(final GUINetworkDesign callback)
    {
        setDefaultRenderer(Boolean.class, new CellRenderers.LostTrafficCellRenderer(new CellRenderers.CheckBoxRenderer(), COLUMN_OFFEREDTRAFFIC, COLUMN_LOSTTRAFFIC));
        setDefaultRenderer(Double.class, new CellRenderers.LostTrafficCellRenderer(new CellRenderers.NumberCellRenderer(), COLUMN_OFFEREDTRAFFIC, COLUMN_LOSTTRAFFIC));
        setDefaultRenderer(Object.class, new CellRenderers.LostTrafficCellRenderer(new CellRenderers.NonEditableCellRenderer(), COLUMN_OFFEREDTRAFFIC, COLUMN_LOSTTRAFFIC));
        setDefaultRenderer(Float.class, new CellRenderers.LostTrafficCellRenderer(new CellRenderers.NumberCellRenderer(), COLUMN_OFFEREDTRAFFIC, COLUMN_LOSTTRAFFIC));
        setDefaultRenderer(Long.class, new CellRenderers.LostTrafficCellRenderer(new CellRenderers.NumberCellRenderer(), COLUMN_OFFEREDTRAFFIC, COLUMN_LOSTTRAFFIC));
        setDefaultRenderer(Integer.class, new CellRenderers.LostTrafficCellRenderer(new CellRenderers.NumberCellRenderer(), COLUMN_OFFEREDTRAFFIC, COLUMN_LOSTTRAFFIC));
        setDefaultRenderer(String.class, new CellRenderers.LostTrafficCellRenderer(new CellRenderers.NonEditableCellRenderer(), COLUMN_OFFEREDTRAFFIC, COLUMN_LOSTTRAFFIC));
    }

    private void setSpecificCellRenderers()
    {
        getColumnModel().getColumn(this.convertColumnIndexToView(COLUMN_LOSTTRAFFIC)).setCellRenderer(new CellRenderers.LostTrafficCellRenderer(getDefaultRenderer(Double.class), COLUMN_OFFEREDTRAFFIC, COLUMN_LOSTTRAFFIC));
    }

    @Override
    public void setColumnRowSorting()
    {
        setAutoCreateRowSorter(true);
        final Set<Integer> columnsWithDoubleAndThenParenthesis = Sets.newHashSet(COLUMN_INGRESSNODE, COLUMN_EGRESSNODES, COLUMN_NUMTREES);
        DefaultRowSorter rowSorter = ((DefaultRowSorter) getRowSorter());
        for (int col = 0; col <= COLUMN_ATTRIBUTES; col++)
            rowSorter.setComparator(col, new AdvancedJTable_networkElement.ColumnComparator(rowSorter, columnsWithDoubleAndThenParenthesis.contains(col)));
        fixedTable.setAutoCreateRowSorter(true);
        fixedTable.setRowSorter(this.getRowSorter());
        rowSorter = ((DefaultRowSorter) fixedTable.getRowSorter());
        for (int col = 0; col <= COLUMN_ATTRIBUTES; col++)
            rowSorter.setComparator(col, new AdvancedJTable_networkElement.ColumnComparator(rowSorter, columnsWithDoubleAndThenParenthesis.contains(col)));
    }

    public int getNumberOfDecoratorColumns()
    {
        return 2;
    }

    @Override
    public ArrayList<String> getAttributesColumnsHeaders()
    {
        ArrayList<String> attColumnsHeaders = new ArrayList<>();
        for (MulticastDemand mDemand : getVisibleElementsInTable())
            for (Map.Entry<String, String> entry : mDemand.getAttributes().entrySet())
                if (attColumnsHeaders.contains(entry.getKey()) == false)
                    attColumnsHeaders.add(entry.getKey());
        return attColumnsHeaders;
    }

    @Override
    public JPopupMenu getPopup(final ElementSelection selection)
    {
        assert selection != null;

        final JScrollPopupMenu popup = new JScrollPopupMenu(20);
        final List<MulticastDemand> demandRowsInTheTable = this.getVisibleElementsInTable();

        if (!selection.isEmpty())
        {
            if (selection.getElementType() != NetworkElementType.MULTICAST_DEMAND)
                throw new RuntimeException("Unmatched selected items with table, selected items are of type: " + selection.getElementType());
        }

        /* Add the popup menu option of the filters */
        final List<MulticastDemand> selectedDemands = (List<MulticastDemand>) selection.getNetworkElements();

        if (!demandRowsInTheTable.isEmpty())
        {
        	addPickOption(selection, popup);
            addFilterOptions(selection, popup);
            popup.addSeparator();
        }

        if (callback.getVisualizationState().isNetPlanEditable())
        {
            popup.add(getAddOption());

            if (!demandRowsInTheTable.isEmpty())
            {
                if (!selectedDemands.isEmpty())
                {
                    if (networkElementType != NetworkElementType.LAYER || callback.getDesign().getNumberOfLayers() != 1)
                    {
                        JMenuItem removeItem = new JMenuItem("Remove selected multicast demands");

                        removeItem.addActionListener(new ActionListener()
                        {
                            @Override
                            public void actionPerformed(ActionEvent e)
                            {
                                try
                                {
                                    for (MulticastDemand selectedDemand : selectedDemands)
                                        selectedDemand.remove();

                                    callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                                    callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.MULTICAST_DEMAND));
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
            }

            popup.addSeparator();
            for (JComponent item : getExtraAddOptions())
                popup.add(item);

            if (!demandRowsInTheTable.isEmpty() && !selectedDemands.isEmpty())
            {
                final List<JComponent> forcedOptions = getForcedOptions(selection);
                if (!forcedOptions.isEmpty())
                {
                    if (popup.getSubElements().length > 0) popup.addSeparator();
                    for (JComponent item : forcedOptions) popup.add(item);
                }

                List<JComponent> extraOptions = getExtraOptions(selection);
                if (!extraOptions.isEmpty())
                {
                    if (popup.getSubElements().length > 0) popup.addSeparator();
                    for (JComponent item : extraOptions) popup.add(item);
                }

                addPopupMenuAttributeOptions(selection, popup);
            }
        }

        return popup;
    }

    @Override
    public void pickSelection(ElementSelection selection)
    {
        if (getVisibleElementsInTable().isEmpty()) return;
        if (selection.getElementType() != NetworkElementType.MULTICAST_DEMAND)
            throw new RuntimeException("Unmatched items with table, selected items are of type: " + selection.getElementType());


        callback.getVisualizationState().pickElement((List<MulticastDemand>) selection.getNetworkElements());
        callback.updateVisualizationAfterPick();
    }

    @Override
    protected boolean hasAttributes()
    {
        return true;
    }


    @Override
    protected JMenuItem getAddOption()
    {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                NetPlan netPlan = callback.getDesign();
                try
                {
                    createMulticastDemandGUI(networkElementType, callback);
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });
        return addItem;
    }

    private static void createMulticastDemandGUI(final NetworkElementType networkElementType, final GUINetworkDesign callback)
    {
        final NetPlan netPlan = callback.getDesign();

        JTextField textFieldIngressNodeId = new JTextField(20);
        JTextField textFieldEgressNodeIds = new JTextField(20);

        JPanel pane = new JPanel();
        pane.add(new JLabel("Ingress node id: "));
        pane.add(textFieldIngressNodeId);
        pane.add(Box.createHorizontalStrut(15));
        pane.add(new JLabel("Egress node ids (space separated): "));
        pane.add(textFieldEgressNodeIds);

        while (true)
        {
            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter multicast demand ingress node and set of egress nodes", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            try
            {
                if (textFieldIngressNodeId.getText().isEmpty())
                    throw new Exception("Please, insert the ingress node id");
                if (textFieldEgressNodeIds.getText().isEmpty())
                    throw new Exception("Please, insert the set of egress node ids");

                String ingressNodeId_st = textFieldIngressNodeId.getText();
                String egressNodeId_st = textFieldEgressNodeIds.getText();

                final long ingressNode = Long.parseLong(ingressNodeId_st);
                if (netPlan.getNodeFromId(ingressNode) == null)
                    throw new Exception("Not a valid ingress node id: " + ingressNodeId_st);
                Set<Node> egressNodes = new HashSet<Node>();
                for (String egressNodeIdString : StringUtils.split(egressNodeId_st))
                {
                    final long nodeId = Long.parseLong(egressNodeIdString);
                    final Node node = netPlan.getNodeFromId(nodeId);
                    if (node == null) throw new Exception("Not a valid egress node id: " + egressNodeIdString);
                    egressNodes.add(node);
                }
                netPlan.addMulticastDemand(netPlan.getNodeFromId(ingressNode), egressNodes, 0, null);
                callback.getVisualizationState().resetPickedState();
                callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.MULTICAST_DEMAND));
                callback.addNetPlanChange();
                break;
            } catch (Throwable ex)
            {
                ErrorHandling.addErrorOrException(ex, AdvancedJTable_multicastDemand.class);
                ErrorHandling.showErrorDialog("Error adding the multicast demand");
            }
        }
    }


    @Override
    protected List<JComponent> getExtraAddOptions()
    {
        List<JComponent> options = new LinkedList<JComponent>();
        NetPlan netPlan = callback.getDesign();
        if (netPlan.getNumberOfNodes() >= 2)
        {
            final JMenuItem oneBroadcastDemandPerNode = new JMenuItem("Add one broadcast demand per node");
            options.add(oneBroadcastDemandPerNode);
            oneBroadcastDemandPerNode.addActionListener(new BroadcastDemandPerNodeActionListener());
            final JMenuItem oneMulticastDemandPerNode = new JMenuItem("Add one multicast demand per node (ingress) with random egress nodes");
            options.add(oneMulticastDemandPerNode);
            oneMulticastDemandPerNode.addActionListener(new MulticastDemandPerNodeActionListener());
        }
        return options;
    }


    @Override
    protected List<JComponent> getExtraOptions(final ElementSelection selection)
    {
        assert selection != null;

        List<JComponent> options = new LinkedList<>();

        final int numRows = model.getRowCount();
        final NetPlan netPlan = callback.getDesign();
        final List<MulticastDemand> visibleRows = getVisibleElementsInTable();

        final List<MulticastDemand> selectedDemands = (List<MulticastDemand>) selection.getNetworkElements();

        JMenuItem offeredTraffic = new JMenuItem("Set selected multicast demands offered traffic");
        offeredTraffic.addActionListener(e ->
        {
            double h_d;

            while (true)
            {
                String str = JOptionPane.showInputDialog(null, "Offered traffic volume", "Set traffic value to all table multicast demands", JOptionPane.QUESTION_MESSAGE);
                if (str == null) return;

                try
                {
                    h_d = Double.parseDouble(str);
                    if (h_d < 0) throw new RuntimeException();

                    break;
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog("Non-valid multicast demand offered traffic value. Please, introduce a non-negative number", "Error setting link length");
                }
            }

            try
            {
                for (MulticastDemand demand : selectedDemands) demand.setOfferedTraffic(h_d);
                callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.MULTICAST_DEMAND));
                callback.addNetPlanChange();
            } catch (Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set offered traffic to all multicast demands");
            }
        });
        options.add(offeredTraffic);

        if (netPlan.isMultilayer())
        {
            if (numRows > 1)
            {
                JMenuItem createUpperLayerLinksFromDemandsItem = null;

                final Set<MulticastDemand> coupledDemands = visibleRows.stream().filter(MulticastDemand::isCoupled).collect(Collectors.toSet());

                if (coupledDemands.size() < visibleRows.size())
                {
                    createUpperLayerLinksFromDemandsItem = new JMenuItem("Create and couple upper layer links from uncoupled multicast demands in selection");
                    createUpperLayerLinksFromDemandsItem.addActionListener(e ->
                    {
                        List<Long> layerIds = netPlan.getNetworkLayerIds();
                        final JComboBox layerSelector = new WiderJComboBox();
                        for (long layerId : layerIds)
                        {
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

                        while (true)
                        {
                            int result = JOptionPane.showConfirmDialog(null, pane, "Please select the upper layer to create links", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return;

                            try
                            {
                                long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                                NetworkLayer layer = netPlan.getNetworkLayerFromId(layerId);
                                for (MulticastDemand demand : selectedDemands)
                                    if (!demand.isCoupled())
                                        demand.coupleToNewLinksCreated(layer);
                                callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.MULTICAST_DEMAND, NetworkElementType.LINK));
                                callback.addNetPlanChange();
                                break;
                            } catch (Throwable ex)
                            {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error creating upper layer links");
                            }
                        }
                    });
                }

                if (!options.isEmpty() && createUpperLayerLinksFromDemandsItem != null)
                {
                    options.add(new JPopupMenu.Separator());
                    options.add(createUpperLayerLinksFromDemandsItem);
                }
            }

            JMenuItem decoupleDemandItem = new JMenuItem("Decouple selected multicast demands");
            decoupleDemandItem.addActionListener(e ->
            {
                for (MulticastDemand selectedDemand : selectedDemands)
                {
                    selectedDemand.decouple();
                    model.setValueAt("", selectedDemand.getIndex(), COLUMN_COUPLEDTOLINKS);
                }

                callback.getVisualizationState().resetPickedState();
                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.MULTICAST_DEMAND, NetworkElementType.LINK));
                callback.addNetPlanChange();
            });
            options.add(decoupleDemandItem);
        }


        return options;
    }


    @Override
    protected List<JComponent> getForcedOptions(ElementSelection selection)
    {
        return new LinkedList<>();
    }

    private class BroadcastDemandPerNodeActionListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            NetPlan netPlan = callback.getDesign();

            if (netPlan.hasMulticastDemands(netPlan.getNetworkLayerDefault()))
            {
                int result = JOptionPane.showConfirmDialog(null, "Remove all existing multicast demands before?", "", JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) return;
                else if (result == JOptionPane.YES_OPTION) netPlan.removeAllMulticastDemands();
            }

            if (netPlan.getNumberOfNodes() < 2) throw new Net2PlanException("At least two nodes are needed");

            for (Node ingressNode : netPlan.getNodes())
            {
                Set<Node> egressNodes = new HashSet<Node>(netPlan.getNodes());
                egressNodes.remove(ingressNode);
                netPlan.addMulticastDemand(ingressNode, egressNodes, 0, null);
            }
            callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.MULTICAST_DEMAND, NetworkElementType.LINK));
            callback.addNetPlanChange();
        }
    }

    private class MulticastDemandPerNodeActionListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            Random rng = new Random();
            NetPlan netPlan = callback.getDesign();

            if (netPlan.hasMulticastDemands(netPlan.getNetworkLayerDefault()))
            {
                int result = JOptionPane.showConfirmDialog(null, "Remove all existing multicast demands before?", "", JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) return;
                if (result == JOptionPane.YES_OPTION) netPlan.removeAllMulticastDemands();
            }

            if (netPlan.getNumberOfNodes() < 2) throw new Net2PlanException("At least two nodes are needed");

            for (Node ingressNode : netPlan.getNodes())
            {
                Set<Node> egressNodes = new HashSet<Node>();
                for (Node n : netPlan.getNodes()) if ((n != ingressNode) && rng.nextBoolean()) egressNodes.add(n);
                if (egressNodes.isEmpty()) egressNodes.add(netPlan.getNode(ingressNode.getIndex() == 0 ? 1 : 0));
                netPlan.addMulticastDemand(ingressNode, egressNodes, 0, null);
            }
            callback.getVisualizationState().resetPickedState();
            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.MULTICAST_DEMAND, NetworkElementType.LINK));
            callback.addNetPlanChange();
        }

    }

    private List<MulticastDemand> getVisibleElementsInTable()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
        return rf == null ? callback.getDesign().getMulticastDemands(layer) : rf.getVisibleMulticastDemands(layer);
    }
}
