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
import com.net2plan.gui.utils.*;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.collections15.BidiMap;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
@SuppressWarnings("unchecked")
public class AdvancedJTable_demand extends AdvancedJTable_networkElement
{
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_INDEX = 1;
    private static final int COLUMN_INGRESSNODE = 2;
    private static final int COLUMN_EGRESSNODE = 3;
    private static final int COLUMN_COUPLEDTOLINK = 4;
    private static final int COLUMN_OFFEREDTRAFFIC = 5;
    private static final int COLUMN_CARRIEDTRAFFIC = 6;
    private static final int COLUMN_LOSTTRAFFIC = 7;
    private static final int COLUMN_ISSERVICECHAIN = 8;
    private static final int COLUMN_TRAVERSEDRESOURCESTYPES = 9;
    private static final int COLUMN_ROUTINGCYCLES = 10;
    private static final int COLUMN_BIFURCATED = 11;
    private static final int COLUMN_NUMROUTES = 12;
    private static final int COLUMN_MAXE2ELATENCY = 13;
    private static final int COLUMN_TAGS = 14;
    private static final int COLUMN_ATTRIBUTES = 15;
    private static final String netPlanViewTabName = "Demands";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index",
            "Ingress node", "Egress node", "Coupled to link",
            "Offered traffic", "Carried traffic", "% Lost traffic",
            "Is Service Chain", "Service types", "Routing cycles", "Bifurcated",
            "# Routes (#BU)", "Max e2e latency (ms)", "Tags", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf(
            "Unique identifier (never repeated in the same netPlan object, never changes, long)",
            "Index (consecutive integer starting in zero)",
            "Ingress node",
            "Egress node",
            "Indicates the coupled upper layer link, if any, or empty",
            "Offered traffic by the demand",
            "Carried traffic by routes carrying traffic from the demand",
            "Percentage of lost traffic from the offered",
            "Is Service Chain", "Service Types",
            "Indicates whether there are routing cycles: loopless (no cycle in some route), open cycles (traffic reaches egress node after some cycles in some route), closed cycles (traffic does not reach the egress node in some route)",
            "Indicates whether the demand has more than one associated route",
            "Number of associated routes (in parenthesis, the number out of them that are designated as backup routes)", "Maximum end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any)", "Demand-specific tags", "Demand-specific attributes");

    /**
     * Default constructor.
     *
     * @param callback The network callback
     * @since 0.2.0
     */
    public AdvancedJTable_demand(final GUINetworkDesign callback)
    {
        super(createTableModel(callback), callback, NetworkElementType.DEMAND);
        setDefaultCellRenderers(callback);
        setSpecificCellRenderers();
        setColumnRowSorting();
        //fixedTable.setRowSorter(this.getRowSorter());
        fixedTable.setDefaultRenderer(Boolean.class, this.getDefaultRenderer(Boolean.class));
        fixedTable.setDefaultRenderer(Double.class, this.getDefaultRenderer(Double.class));
        fixedTable.setDefaultRenderer(Object.class, this.getDefaultRenderer(Object.class));
        fixedTable.setDefaultRenderer(Float.class, this.getDefaultRenderer(Float.class));
        fixedTable.setDefaultRenderer(Long.class, this.getDefaultRenderer(Long.class));
        fixedTable.setDefaultRenderer(Integer.class, this.getDefaultRenderer(Integer.class));
        fixedTable.setDefaultRenderer(String.class, this.getDefaultRenderer(String.class));
        fixedTable.setDefaultRenderer(LastRowAggregatedValue.class, new CellRenderers.LastRowAggregatingInfoCellRenderer());
        fixedTable.getTableHeader().setDefaultRenderer(new CellRenderers.FixedTableHeaderRenderer());
    }

    public String getTabName()
    {
        return netPlanViewTabName;
    }


    public List<Object[]> getAllData(NetPlan currentState, ArrayList<String> attributesColumns)
    {
        final boolean isSourceRouting = currentState.getRoutingType() == RoutingType.SOURCE_ROUTING;
        final List<Demand> rowVisibleDemands = getVisibleElementsInTable();
        List<Object[]> allDemandData = new LinkedList<Object[]>();

        double accum_numRoutes = 0;
        double accum_numBackupRoutes = 0;

        final double[] dataAggregator = new double[netPlanViewTableHeader.length];

        for (Demand demand : rowVisibleDemands)
        {
            final Set<Route> routes_thisDemand = isSourceRouting ? demand.getRoutes() : new LinkedHashSet<Route>();
            final Link coupledLink = demand.getCoupledLink();
            final Node ingressNode = demand.getIngressNode();
            final Node egressNode = demand.getEgressNode();
            final double h_d = demand.getOfferedTraffic();
            final double lostTraffic_d = demand.getBlockedTraffic();
            final Object[] demandData = new Object[netPlanViewTableHeader.length + attributesColumns.size()];
            demandData[COLUMN_ID] = demand.getId();
            demandData[COLUMN_INDEX] = demand.getIndex();
            demandData[COLUMN_INGRESSNODE] = ingressNode.getIndex() + (ingressNode.getName().isEmpty() ? "" : " (" + ingressNode.getName() + ")");
            demandData[COLUMN_EGRESSNODE] = egressNode.getIndex() + (egressNode.getName().isEmpty() ? "" : " (" + egressNode.getName() + ")");
            demandData[COLUMN_COUPLEDTOLINK] = coupledLink == null ? "" : "e" + coupledLink.getIndex() + " (layer " + coupledLink.getLayer() + ")";
            demandData[COLUMN_OFFEREDTRAFFIC] = h_d;
            demandData[COLUMN_CARRIEDTRAFFIC] = demand.getCarriedTraffic();
            demandData[COLUMN_LOSTTRAFFIC] = h_d == 0 ? 0 : 100 * lostTraffic_d / h_d;
            demandData[COLUMN_ISSERVICECHAIN] = demand.isServiceChainRequest();
            demandData[COLUMN_TRAVERSEDRESOURCESTYPES] = isSourceRouting ? joinTraversedResourcesTypes(demand) : "";
            demandData[COLUMN_ROUTINGCYCLES] = demand.getRoutingCycleType();
            demandData[COLUMN_BIFURCATED] = !isSourceRouting ? "-" : (demand.isBifurcated()) ? String.format("Yes (%d)", demand.getRoutes().size()) : "No";
            demandData[COLUMN_NUMROUTES] = routes_thisDemand.isEmpty() ? "none" : routes_thisDemand.size() + " (" + routes_thisDemand.stream().filter(e -> e.isBackupRoute()).count() + ")";
            demandData[COLUMN_MAXE2ELATENCY] = demand.getWorstCasePropagationTimeInMs();
            demandData[COLUMN_TAGS] = StringUtils.listToString(Lists.newArrayList(demand.getTags()));
            demandData[COLUMN_ATTRIBUTES] = StringUtils.mapToString(demand.getAttributes());
            for (int i = netPlanViewTableHeader.length; i < netPlanViewTableHeader.length + attributesColumns.size(); i++)
            {
                if (demand.getAttributes().containsKey(attributesColumns.get(i - netPlanViewTableHeader.length)))
                {
                    demandData[i] = demand.getAttribute(attributesColumns.get(i - netPlanViewTableHeader.length));
                }
            }

            AggregationUtils.updateRowSum(dataAggregator, COLUMN_OFFEREDTRAFFIC, demandData[COLUMN_OFFEREDTRAFFIC]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_CARRIEDTRAFFIC, demandData[COLUMN_CARRIEDTRAFFIC]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_LOSTTRAFFIC, demandData[COLUMN_LOSTTRAFFIC]);
            if (demand.isServiceChainRequest()) AggregationUtils.updateRowCount(dataAggregator, COLUMN_ISSERVICECHAIN, 1);
            if (isSourceRouting)
            {
                accum_numRoutes += routes_thisDemand.size();
                accum_numBackupRoutes += routes_thisDemand.stream().filter(e -> e.isBackupRoute()).count();
            }
            AggregationUtils.updateRowMax(dataAggregator, COLUMN_MAXE2ELATENCY, demandData[COLUMN_MAXE2ELATENCY]);

            allDemandData.add(demandData);
        }
        
        /* Add the aggregation row with the aggregated statistics */
        final LastRowAggregatedValue[] aggregatedData = new LastRowAggregatedValue[netPlanViewTableHeader.length + attributesColumns.size()];
        Arrays.fill(aggregatedData, new LastRowAggregatedValue());
        aggregatedData[COLUMN_OFFEREDTRAFFIC] = new LastRowAggregatedValue(dataAggregator[COLUMN_OFFEREDTRAFFIC]);
        aggregatedData[COLUMN_CARRIEDTRAFFIC] = new LastRowAggregatedValue(dataAggregator[COLUMN_CARRIEDTRAFFIC]);
        aggregatedData[COLUMN_LOSTTRAFFIC] = new LastRowAggregatedValue(dataAggregator[COLUMN_LOSTTRAFFIC]);
        aggregatedData[COLUMN_ISSERVICECHAIN] = new LastRowAggregatedValue(dataAggregator[COLUMN_ISSERVICECHAIN]);
        aggregatedData[COLUMN_NUMROUTES] = new LastRowAggregatedValue(accum_numRoutes + "(" + accum_numBackupRoutes + ")");
        aggregatedData[COLUMN_MAXE2ELATENCY] = new LastRowAggregatedValue(dataAggregator[COLUMN_MAXE2ELATENCY]);
        allDemandData.add(aggregatedData);

        return allDemandData;
    }

    @Override
    public int getAttributesColumnIndex()
    {
        return COLUMN_ATTRIBUTES;
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
        return rf == null ? callback.getDesign().hasDemands(layer) : rf.hasDemands(layer);
    }

    private static TableModel createTableModel(final GUINetworkDesign callback)
    {
        TableModel demandTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                if (!callback.getVisualizationState().isNetPlanEditable()) return false;
                if (columnIndex >= netPlanViewTableHeader.length) return true;
                if (rowIndex == getRowCount() - 1) return false; // the last row is for the aggregated info
                if (getValueAt(rowIndex, columnIndex) == null) return false;

                return columnIndex == COLUMN_OFFEREDTRAFFIC;
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
                final Demand demand = netPlan.getDemandFromId(demandId);

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
                                callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
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
                    ex.printStackTrace();
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying demand");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };
        return demandTableModel;
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
        //setDefaultRenderer(LastRowAggregatedValue.class, new CellRenderers.LostTrafficCellRenderer(new CellRenderers.LastRowAggregatingInfoCellRenderer(), COLUMN_OFFEREDTRAFFIC, COLUMN_LOSTTRAFFIC));
    }

    private void setSpecificCellRenderers()
    {
        getColumnModel().getColumn(this.convertColumnIndexToView(COLUMN_LOSTTRAFFIC)).setCellRenderer(new CellRenderers.LostTrafficCellRenderer(new CellRenderers.NumberCellRenderer(), COLUMN_OFFEREDTRAFFIC, COLUMN_LOSTTRAFFIC));
    }


    @Override
    public void setColumnRowSorting()
    {
        setAutoCreateRowSorter(true);
        final Set<Integer> columnsWithDoubleAndThenParenthesis = Sets.newHashSet(COLUMN_INGRESSNODE, COLUMN_EGRESSNODE, COLUMN_NUMROUTES);
        DefaultRowSorter rowSorter = ((DefaultRowSorter) getRowSorter());
        for (int col = 0; col <= COLUMN_ATTRIBUTES; col++)
            rowSorter.setComparator(col, new AdvancedJTable_networkElement.ColumnComparator(rowSorter, columnsWithDoubleAndThenParenthesis.contains(col)));
        fixedTable.setAutoCreateRowSorter(true);
        fixedTable.setRowSorter(this.getRowSorter());
        rowSorter = ((DefaultRowSorter) fixedTable.getRowSorter());
        for (int col = 0; col <= COLUMN_ATTRIBUTES; col++)
            rowSorter.setComparator(col, new AdvancedJTable_networkElement.ColumnComparator(rowSorter, columnsWithDoubleAndThenParenthesis.contains(col)));
    }

    @Override
    public int getNumberOfDecoratorColumns()
    {
        return 2;
    }

    @Override
    public ArrayList<String> getAttributesColumnsHeaders()
    {
        ArrayList<String> attColumnsHeaders = new ArrayList<>();
        NetPlan currentTopology = callback.getDesign();
        for (Demand demand : getVisibleElementsInTable())
            for (Map.Entry<String, String> entry : demand.getAttributes().entrySet())
                if (!attColumnsHeaders.contains(entry.getKey()))
                    attColumnsHeaders.add(entry.getKey());
        return attColumnsHeaders;
    }

    public JPopupMenu getPopup(final ElementSelection selection)
    {
        assert selection != null;

        final JScrollPopupMenu popup = new JScrollPopupMenu(20);

        final List<Demand> demandRowsInTheTable = this.getVisibleElementsInTable();

        if (!selection.isEmpty())
        {
            if (selection.getElementType() != NetworkElementType.DEMAND)
                throw new RuntimeException("Unmatched items with table, selected items are of type: " + selection.getElementType());
        }

        /* Add the popup menu option of the filters */
        final List<Demand> selectedDemands = (List<Demand>) selection.getNetworkElements();

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
                    JMenuItem removeItem = new JMenuItem("Remove selected demands");

                    removeItem.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            try
                            {
                                for (Demand selectedDemand : selectedDemands) selectedDemand.remove();

                                callback.getVisualizationState().resetPickedState();
                                callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
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

            if (!getExtraAddOptions().isEmpty()) popup.addSeparator();
            for (JComponent item : getExtraAddOptions()) popup.add(item);

            if (!demandRowsInTheTable.isEmpty() && !selectedDemands.isEmpty())
            {
                List<JComponent> forcedOptions = getForcedOptions(selection);
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

    public void pickSelection(ElementSelection selection)
    {
        if (selection.getElementType() != NetworkElementType.DEMAND)
            throw new RuntimeException("Unmatched items with table, selected items are of type: " + selection.getElementType());

        callback.getVisualizationState().pickElement((List<Demand>) selection.getNetworkElements());
        callback.updateVisualizationAfterPick();
    }

    @Override
    protected boolean hasAttributes()
    {
        return true;
    }


    @Override
    protected List<JComponent> getExtraAddOptions()
    {
        List<JComponent> options = new LinkedList<>();
        NetPlan netPlan = callback.getDesign();

        if (netPlan.getNumberOfNodes() >= 2)
        {
            final JMenuItem oneDemandPerNodePair = new JMenuItem("Add one demand per node pair");
            options.add(oneDemandPerNodePair);
            oneDemandPerNodePair.addActionListener(new FullMeshTrafficActionListener(false));
        }
        final JMenuItem oneDemandPerNodePairConnectedThisLayer = new JMenuItem("Add one demand per node pair linked at this layer");
        options.add(oneDemandPerNodePairConnectedThisLayer);
        oneDemandPerNodePairConnectedThisLayer.addActionListener(new FullMeshTrafficActionListener(true));
        return options;
    }


    @Override
    protected JMenuItem getAddOption()
    {
        final NetworkElementType networkElementType = NetworkElementType.DEMAND;
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    createLinkDemandGUI(networkElementType, callback);
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });

        if (callback.getDesign().getNumberOfNodes() < 2) addItem.setEnabled(false);

        return addItem;

    }


    @Override
    protected List<JComponent> getForcedOptions(ElementSelection selection)
    {
        return new LinkedList<>();
    }

    static void createLinkDemandGUI(final NetworkElementType networkElementType, final GUINetworkDesign callback)
    {
        final NetPlan netPlan = callback.getDesign();
        final JComboBox originNodeSelector = new WiderJComboBox();
        final JComboBox destinationNodeSelector = new WiderJComboBox();

        for (Node node : netPlan.getNodes())
        {
            final String nodeName = node.getName();
            String nodeLabel = "Node " + node.getIndex();
            if (!nodeName.isEmpty()) nodeLabel += " (" + nodeName + ")";

            originNodeSelector.addItem(StringLabeller.of(node.getId(), nodeLabel));
            destinationNodeSelector.addItem(StringLabeller.of(node.getId(), nodeLabel));
        }

        ItemListener nodeListener = new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                long originNodeId = (long) ((StringLabeller) originNodeSelector.getSelectedItem()).getObject();
                long destinationNodeId = (long) ((StringLabeller) destinationNodeSelector.getSelectedItem()).getObject();
                callback.putTransientColorInElementTopologyCanvas(Arrays.asList(netPlan.getNodeFromId(originNodeId)), Color.GREEN);
                callback.putTransientColorInElementTopologyCanvas(Arrays.asList(netPlan.getNodeFromId(destinationNodeId)), Color.CYAN);
            }
        };

        originNodeSelector.addItemListener(nodeListener);
        destinationNodeSelector.addItemListener(nodeListener);

        originNodeSelector.setSelectedIndex(0);
        destinationNodeSelector.setSelectedIndex(1);

        JPanel pane = new JPanel();
        pane.add(networkElementType == NetworkElementType.LINK ? new JLabel("Origin node: ") : new JLabel("Ingress node: "));
        pane.add(originNodeSelector);
        pane.add(Box.createHorizontalStrut(15));
        pane.add(networkElementType == NetworkElementType.LINK ? new JLabel("Destination node: ") : new JLabel("Egress node: "));
        pane.add(destinationNodeSelector);

        while (true)
        {
            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter end nodes for the new " + networkElementType, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            try
            {
                long originNodeId = (long) ((StringLabeller) originNodeSelector.getSelectedItem()).getObject();
                long destinationNodeId = (long) ((StringLabeller) destinationNodeSelector.getSelectedItem()).getObject();
                Node originNode = netPlan.getNodeFromId(originNodeId);
                Node destinationNode = netPlan.getNodeFromId(destinationNodeId);

                if (netPlan.getNodeFromId(originNodeId) == null)
                    throw new Net2PlanException("Node of id: " + originNodeId + " does not exist");
                if (netPlan.getNodeFromId(destinationNodeId) == null)
                    throw new Net2PlanException("Node of id: " + destinationNodeId + " does not exist");

                if (networkElementType == NetworkElementType.LINK)
                {
                    final Link e = netPlan.addLink(originNode, destinationNode, 0, 0, 200000, null);
                    callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                    callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.LINK));
                    callback.getVisualizationState().pickElement(e);
                    callback.updateVisualizationAfterPick();
                    callback.addNetPlanChange();

                } else
                {
                    final Demand d = netPlan.addDemand(originNode, destinationNode, 0, null);
                    callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                    callback.getVisualizationState().pickElement(d);
                    callback.updateVisualizationAfterPick();
                    callback.addNetPlanChange();
                }

                break;
            } catch (Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Error adding " + networkElementType);
            }
        }
    }

    private class FullMeshTrafficActionListener implements ActionListener
    {
    	private final boolean onlyConnectedAtThisLayer;
    	FullMeshTrafficActionListener (boolean onlyConnectedAtThisLayer) { this.onlyConnectedAtThisLayer = onlyConnectedAtThisLayer; }
        @Override
        public void actionPerformed(ActionEvent e)
        {
            NetPlan netPlan = callback.getDesign();
            final NetworkLayer layer = netPlan.getNetworkLayerDefault();

            if (netPlan.hasDemands(netPlan.getNetworkLayerDefault()))
            {
                int result = JOptionPane.showConfirmDialog(null, "Remove all existing demands?", "", JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) return;
                if (result == JOptionPane.YES_OPTION) netPlan.removeAllDemands();
            }
            final List<Node> nodes = onlyConnectedAtThisLayer? netPlan.getNodes().stream().filter(n->!n.getIncomingLinks(layer).isEmpty()).filter(n->!n.getOutgoingLinks(layer).isEmpty()).collect(Collectors.toList()): netPlan.getNodes();
            for (Node n1 : nodes)
                for (Node n2 : nodes)
                    if (n1.getIndex() < n2.getIndex()) 
                    	netPlan.addDemandBidirectional(n1, n2, 0, null, layer);
            callback.getVisualizationState().resetPickedState();
            callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
            callback.addNetPlanChange();
        }
    }


    @Override
    protected List<JComponent> getExtraOptions(final ElementSelection selection)
    {
        List<JComponent> options = new LinkedList<>();
        final int numRows = model.getRowCount();
        final NetPlan netPlan = callback.getDesign();
        final List<Demand> tableVisibleDemands = getVisibleElementsInTable();
        final List<Demand> selectedDemands = (List<Demand>) selection.getNetworkElements();

        JMenuItem offeredTraffic = new JMenuItem("Set selected demands offered traffic");
        offeredTraffic.addActionListener(e ->
        {
            double h_d;

            while (true)
            {
                String str = JOptionPane.showInputDialog(null, "Offered traffic volume", "Set traffic value to selected demands in the table", JOptionPane.QUESTION_MESSAGE);
                if (str == null) return;

                try
                {
                    h_d = Double.parseDouble(str);
                    if (h_d < 0) throw new RuntimeException();

                    break;
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog("Please, introduce a non-negative number", "Error setting offered traffic");
                }
            }

            try
            {
                for (Demand d : selectedDemands) d.setOfferedTraffic(h_d);
                callback.getVisualizationState().resetPickedState();
                callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                callback.addNetPlanChange();
            } catch (Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set offered traffic to selected demands in the table");
            }
        });
        options.add(offeredTraffic);

        JMenuItem scaleOfferedTraffic = new JMenuItem("Scale selected demands offered traffic");
        scaleOfferedTraffic.addActionListener(e ->
        {
            double scalingFactor;

            while (true)
            {
                String str = JOptionPane.showInputDialog(null, "Scaling factor to multiply to selected offered traffics", "Scale offered traffic", JOptionPane.QUESTION_MESSAGE);
                if (str == null) return;

                try
                {
                    scalingFactor = Double.parseDouble(str);
                    if (scalingFactor < 0) throw new RuntimeException();

                    break;
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog("Please, introduce a non-negative number", "Error setting offered traffic");
                }
            }

            try
            {
                for (Demand d : selectedDemands) d.setOfferedTraffic(d.getOfferedTraffic() * scalingFactor);
                callback.getVisualizationState().resetPickedState();
                callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                callback.addNetPlanChange();
            } catch (Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to scale demand offered traffics");
            }
        });
        options.add(scaleOfferedTraffic);

        JMenuItem setServiceTypes = new JMenuItem("Set traversed resource types (to selected or all demands in the table)");
        setServiceTypes.addActionListener(e ->
        {
            try
            {
                String[] headers = StringUtils.arrayOf("Order", "Type");
                Object[][] data = {null, null};
                DefaultTableModel model = new ClassAwareTableModelImpl(data, headers);
                AdvancedJTable table = new AdvancedJTable(model);
                JButton addRow = new JButton("Add new traversed resource type");
                addRow.addActionListener(e1 ->
                {
                    Object[] newRow = {table.getRowCount(), ""};
                    ((DefaultTableModel) table.getModel()).addRow(newRow);
                });
                JButton removeRow = new JButton("Remove selected");
                removeRow.addActionListener(e12 ->
                {
                    ((DefaultTableModel) table.getModel()).removeRow(table.getSelectedRow());
                    for (int t = 0; t < table.getRowCount(); t++)
                        table.getModel().setValueAt(t, t, 0);
                });
                JButton removeAllRows = new JButton("Remove all");
                removeAllRows.addActionListener(e13 ->
                {
                    while (table.getRowCount() > 0)
                        ((DefaultTableModel) table.getModel()).removeRow(0);
                });

                final Set<String> resourceTypes = netPlan.getResources().stream().map(Resource::getType).collect(Collectors.toSet());
                final List<String> sortedResourceTypes = resourceTypes.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
                Object[][] newData = new Object[sortedResourceTypes.size()][headers.length];
                for (int i = 0; i < sortedResourceTypes.size(); i++)
                {
                    newData[i][0] = i;
                    newData[i][1] = sortedResourceTypes.get(i);
                }

                ((DefaultTableModel) table.getModel()).setDataVector(newData, headers);
                JPanel pane = new JPanel();
                JPanel pane2 = new JPanel();
                pane.setLayout(new BorderLayout());
                pane2.setLayout(new BorderLayout());
                pane.add(new JScrollPane(table), BorderLayout.CENTER);
                pane2.add(addRow, BorderLayout.WEST);
                pane2.add(removeRow, BorderLayout.EAST);
                pane2.add(removeAllRows, BorderLayout.SOUTH);
                pane.add(pane2, BorderLayout.SOUTH);
                final String[] optionsArray = new String[]{"Set to selected demand", "Set to all demands", "Cancel"};
                int result = JOptionPane.showOptionDialog(null, pane, "Set traversed resource types", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, optionsArray, optionsArray[0]);
                if ((result != 0) && (result != 1)) return;
                List<String> newTraversedResourcesTypes = new LinkedList<>();
                for (int j = 0; j < table.getRowCount(); j++)
                {
                    String travResourceType = table.getModel().getValueAt(j, 1).toString();
                    newTraversedResourcesTypes.add(travResourceType);
                }

                for (Demand selectedDemand : selectedDemands)
                    if (!selectedDemand.getRoutes().isEmpty())
                        throw new Net2PlanException("It is not possible to set the resource types traversed to demands with routes");

                for (Demand selectedDemand : selectedDemands)
                    selectedDemand.setServiceChainSequenceOfTraversedResourceTypes(newTraversedResourcesTypes);

                callback.getVisualizationState().resetPickedState();
                callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                callback.addNetPlanChange();
            } catch (Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set traversed resource types");
            }
        });
        options.add(setServiceTypes);

        if (netPlan.isMultilayer())
        {
            options.add(new JPopupMenu.Separator());

            JMenuItem createUpperLayerLinkFromDemandItem = new JMenuItem("Create and couple upper layer links from uncoupled demands in selection");
            createUpperLayerLinkFromDemandItem.addActionListener(e ->
            {
                Collection<Long> layerIds = netPlan.getNetworkLayerIds();
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
                    int result = JOptionPane.showConfirmDialog(null, pane, "Please select the upper layer to create the link", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return;

                    try
                    {
                        long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                        for (Demand d : selectedDemands)
                            if (!d.isCoupled())
                                d.coupleToNewLinkCreated(netPlan.getNetworkLayerFromId(layerId));
                        callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                        callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.DEMAND, NetworkElementType.LINK));
                        callback.addNetPlanChange();
                        break;
                    } catch (Throwable ex)
                    {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error creating upper layer link from demand");
                    }
                }
            });

            options.add(createUpperLayerLinkFromDemandItem);

            if (selectedDemands.size() == 1)
            {
                JMenuItem coupleDemandToLink = new JMenuItem("Couple selected demands to upper layer link");
                coupleDemandToLink.addActionListener(e ->
                {
                    Collection<Long> layerIds = netPlan.getNetworkLayerIds();
                    final JComboBox layerSelector = new WiderJComboBox();
                    final JComboBox linkSelector = new WiderJComboBox();
                    for (long layerId : layerIds)
                    {
                        if (layerId == netPlan.getNetworkLayerDefault().getId()) continue;

                        final String layerName = netPlan.getNetworkLayerFromId(layerId).getName();
                        String layerLabel = "Layer " + layerId;
                        if (!layerName.isEmpty()) layerLabel += " (" + layerName + ")";

                        layerSelector.addItem(StringLabeller.of(layerId, layerLabel));
                    }

                    layerSelector.addItemListener(e1 ->
                    {
                        if (layerSelector.getSelectedIndex() >= 0)
                        {
                            long selectedLayerId = (Long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                            NetworkLayer selectedLayer = netPlan.getNetworkLayerFromId(selectedLayerId);

                            linkSelector.removeAllItems();
                            Collection<Link> links_thisLayer = netPlan.getLinks(selectedLayer);
                            for (Link link : links_thisLayer)
                            {
                                if (link.isCoupled()) continue;

                                String originNodeName = link.getOriginNode().getName();
                                String destinationNodeName = link.getDestinationNode().getName();

                                linkSelector.addItem(StringLabeller.unmodifiableOf(link.getId(), "e" + link.getIndex() + " [n" + link.getOriginNode().getIndex() + " (" + originNodeName + ") -> n" + link.getDestinationNode().getIndex() + " (" + destinationNodeName + ")]"));
                            }
                        }

                        if (linkSelector.getItemCount() == 0)
                        {
                            linkSelector.setEnabled(false);
                        } else
                        {
                            linkSelector.setSelectedIndex(0);
                            linkSelector.setEnabled(true);
                        }
                    });

                    layerSelector.setSelectedIndex(-1);
                    layerSelector.setSelectedIndex(0);

                    JPanel pane = new JPanel(new MigLayout("", "[][grow]", "[][]"));
                    pane.add(new JLabel("Select layer: "));
                    pane.add(layerSelector, "growx, wrap");
                    pane.add(new JLabel("Select link: "));
                    pane.add(linkSelector, "growx, wrap");

                    while (true)
                    {
                        int result = JOptionPane.showConfirmDialog(null, pane, "Please select the upper layer link", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (result != JOptionPane.OK_OPTION) return;

                        try
                        {
                            try
                            {
                                Long linkId = (Long) ((StringLabeller) linkSelector.getSelectedItem()).getObject();
                                if (linkId == null) throw new NullPointerException();

                                final Link link = netPlan.getLinkFromId(linkId);
                                if (link == null) throw new NullPointerException();

                                for (Demand selectedDemand : selectedDemands)
                                    selectedDemand.coupleToUpperLayerLink(link);
                            } catch (Throwable ex)
                            {
                                throw new RuntimeException("No link was selected");
                            }

                            callback.getVisualizationState().resetPickedState();
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.DEMAND, NetworkElementType.LINK));
                            callback.addNetPlanChange();
                            break;
                        } catch (Throwable ex)
                        {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error coupling upper layer link to demand");
                        }
                    }
                });

                options.add(coupleDemandToLink);
            }

            JMenuItem decoupleDemandItem = new JMenuItem("Decouple selected demands");
            decoupleDemandItem.addActionListener(e ->
            {
                for (Demand d : selectedDemands)
                {
                    if (d.isCoupled()) d.decouple();
                    model.setValueAt("", d.getIndex(), COLUMN_COUPLEDTOLINK);
                }
                callback.getVisualizationState().resetPickedState();
                callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                callback.addNetPlanChange();
            });
            options.add(decoupleDemandItem);

            if (numRows > 1)
            {
                JMenuItem createUpperLayerLinksFromDemandsItem = null;

                final Set<Demand> coupledDemands = tableVisibleDemands.stream().filter(Demand::isCoupled).collect(Collectors.toSet());

                if (coupledDemands.size() < tableVisibleDemands.size())
                {
                    createUpperLayerLinksFromDemandsItem = new JMenuItem("Create upper layer links from uncoupled demands in selection");
                    createUpperLayerLinksFromDemandsItem.addActionListener(e ->
                    {
                        Collection<Long> layerIds = netPlan.getNetworkLayerIds();
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
                                for (Demand demand : selectedDemands)
                                    if (!demand.isCoupled())
                                        demand.coupleToNewLinkCreated(layer);

                                callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.DEMAND, NetworkElementType.LINK));
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
        }

        return options;
    }

    private String joinTraversedResourcesTypes(Demand d)
    {
        List<String> trt = d.getServiceChainSequenceOfTraversedResourceTypes();
        String t = "";
        int counter = 0;
        for (String s : trt)
        {
            if (counter == trt.size() - 1)
                t = t + s;
            else
                t = t + s + ", ";

            counter++;

        }

        return t;
    }

    private class ClassAwareTableModelImpl extends ClassAwareTableModel
    {
        public ClassAwareTableModelImpl(Object[][] dataVector, Object[] columnIdentifiers)
        {
            super(dataVector, columnIdentifiers);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            if (columnIndex == 1) return true;
            return false;
        }

        @Override
        public void setValueAt(Object value, int row, int column)
        {
            super.setValueAt(value, row, column);

        }

    }

    private List<Demand> getVisibleElementsInTable()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
        return rf == null ? callback.getDesign().getDemands(layer) : rf.getVisibleDemands(layer);
    }

}
