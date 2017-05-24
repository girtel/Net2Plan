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
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.collections15.BidiMap;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
@SuppressWarnings("unchecked")
public class AdvancedJTable_link extends AdvancedJTable_networkElement
{
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_INDEX = 1;
    public static final int COLUMN_SHOWHIDE = 2;
    public static final int COLUMN_ORIGINNODE = 3;
    public static final int COLUMN_DESTNODE = 4;
    public static final int COLUMN_STATE = 5;
    public static final int COLUMN_CAPACITY = 6;
    public static final int COLUMN_CARRIEDTRAFFIC = 7;
    public static final int COLUMN_OCCUPIEDCAPACITY = 8;
    public static final int COLUMN_UTILIZATION = 9;
    public static final int COLUMN_ISBOTTLENECK = 10;
    public static final int COLUMN_LENGTH = 11;
    public static final int COLUMN_PROPSPEED = 12;
    public static final int COLUMN_PROPDELAYMS = 13;
    public static final int COLUMN_NUMROUTES = 14;
    public static final int COLUMN_NUMBACKUPROUTES = 15;
    public static final int COLUMN_NUMFORWRULES = 16;
    public static final int COLUMN_NUMTREES = 17;
    public static final int COLUMN_SRGS = 18;
    public static final int COLUMN_COUPLEDTODEMAND = 19;
    public static final int COLUMN_TAGS = 20;
    public static final int COLUMN_ATTRIBUTES = 21;
    private static final String netPlanViewTabName = "Links";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "Show/Hide", "Origin node", "Destination node", "State", "Capacity", "Carried traffic", "Occupation BU routes", "Utilization", "Is bottleneck?", "Length (km)", "Propagation speed (km/s)", "Propagation delay (ms)", "# Routes", "# Segments", "# Forwarding rules", "# Multicast trees", "# SRGs", "Coupled to demand", "Tags", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)", "Index (consecutive integer starting in zero)", "Indicates whether or not the link is visible in the topology canvas (if some of the end-nodes is hidden, this link will become hidden, even though the link is set as visible)", "Origin node", "Destination node", "Indicates whether the link is in up/down state", "Capacity", "Carried traffic (summing unicast and multicast)", "Capacity occupied by routes that are designated as backup routes", "Utilization (occupied capacity divided by link capacity)", "Indicates whether this link has the highest utilization in the network", "Length (km)", "Propagation speed (km/s)", "Propagation delay (ms)", "Number of routes traversing the link", "Number of protection segments traversing the link", "Number of forwarding rules for this link", "Number of multicast trees traversing the link", "Number of SRGs including this link", "Indicates the coupled lower layer demand, if any, or empty", "Link-specific tags", "Link-specific attributes");

    public AdvancedJTable_link(final GUINetworkDesign callback)
    {
        super(createTableModel(callback), callback, NetworkElementType.LINK);
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
        final List<Link> rowVisibleLinks = getVisibleElementsInTable();
        final double max_rho_e = currentState.getLinks().stream().mapToDouble(e -> e.getUtilization()).max().orElse(0);
        final List<Object[]> allLinkData = new LinkedList<Object[]>();
        final double[] dataAggregator = new double[netPlanViewTableHeader.length];

        for (Link link : rowVisibleLinks)
        {
            final Set<SharedRiskGroup> srgIds_thisLink = link.getSRGs();
            final Demand coupledDemand = link.getCoupledDemand();
            final MulticastDemand coupledMulticastDemand = link.getCoupledMulticastDemand();

            final Node originNode = link.getOriginNode();
            final Node destinationNode = link.getDestinationNode();
            final String originNodeName = originNode.getName();
            final String destinationNodeName = destinationNode.getName();

            final double rho_e = link.getUtilization();
            final Object[] linkData = new Object[netPlanViewTableHeader.length + attributesColumns.size()];
            linkData[COLUMN_ID] = link.getId();
            linkData[COLUMN_INDEX] = link.getIndex();
            linkData[COLUMN_SHOWHIDE] = !callback.getVisualizationState().isHiddenOnCanvas(link);
            linkData[COLUMN_ORIGINNODE] = originNode.getIndex() + (originNodeName.isEmpty() ? "" : " (" + originNodeName + ")");
            linkData[COLUMN_DESTNODE] = destinationNode.getIndex() + (destinationNodeName.isEmpty() ? "" : " (" + destinationNodeName + ")");
            linkData[COLUMN_STATE] = !link.isDown();
            linkData[COLUMN_CAPACITY] = link.getCapacity();
            linkData[COLUMN_CARRIEDTRAFFIC] = link.getCarriedTraffic();
            linkData[COLUMN_OCCUPIEDCAPACITY] = link.getOccupiedCapacity();
            linkData[COLUMN_UTILIZATION] = rho_e;
            linkData[COLUMN_ISBOTTLENECK] = DoubleUtils.isEqualWithinRelativeTolerance(max_rho_e, rho_e, Configuration.precisionFactor);
            linkData[COLUMN_LENGTH] = link.getLengthInKm();
            linkData[COLUMN_PROPSPEED] = link.getPropagationSpeedInKmPerSecond();
            linkData[COLUMN_PROPDELAYMS] = link.getPropagationDelayInMs();
            linkData[COLUMN_NUMROUTES] = !link.getLayer().isSourceRouting() ? 0 : link.getNumberOfTraversingRoutes();
            linkData[COLUMN_NUMBACKUPROUTES] = !link.getLayer().isSourceRouting() ? 0 : link.getNumberOfTraversingBackupRoutes();
            linkData[COLUMN_NUMFORWRULES] = link.getLayer().isSourceRouting() ? 0 : link.getNumberOfForwardingRules();
            linkData[COLUMN_NUMTREES] = link.getNumberOfTraversingTrees();
            linkData[COLUMN_SRGS] = srgIds_thisLink.size();
            linkData[COLUMN_COUPLEDTODEMAND] = coupledDemand != null ? "d" + coupledDemand.getIndex() + " (layer " + coupledDemand.getLayer() + ")" : (coupledMulticastDemand == null ? "" : "d" + coupledMulticastDemand.getIndex() + " (layer " + coupledMulticastDemand.getLayer() + ")");
            linkData[COLUMN_TAGS] = StringUtils.listToString(Lists.newArrayList(link.getTags()));
            linkData[COLUMN_ATTRIBUTES] = StringUtils.mapToString(link.getAttributes());

            for (int i = netPlanViewTableHeader.length; i < netPlanViewTableHeader.length + attributesColumns.size(); i++)
            {
                if (link.getAttributes().containsKey(attributesColumns.get(i - netPlanViewTableHeader.length)))
                {
                    linkData[i] = link.getAttribute(attributesColumns.get(i - netPlanViewTableHeader.length));
                }
            }

            AggregationUtils.updateRowSum(dataAggregator, COLUMN_CAPACITY, linkData[COLUMN_CAPACITY]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_CARRIEDTRAFFIC, linkData[COLUMN_CARRIEDTRAFFIC]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_OCCUPIEDCAPACITY, linkData[COLUMN_OCCUPIEDCAPACITY]);
            AggregationUtils.updateRowMax(dataAggregator, COLUMN_LENGTH, linkData[COLUMN_LENGTH]);
            AggregationUtils.updateRowMax(dataAggregator, COLUMN_PROPDELAYMS, linkData[COLUMN_PROPDELAYMS]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_NUMROUTES, linkData[COLUMN_NUMROUTES]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_NUMBACKUPROUTES, linkData[COLUMN_NUMBACKUPROUTES]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_NUMTREES, linkData[COLUMN_NUMTREES]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_SRGS, linkData[COLUMN_SRGS]);
            if (coupledDemand != null) AggregationUtils.updateRowCount(dataAggregator, COLUMN_COUPLEDTODEMAND, 1);

            allLinkData.add(linkData);
        }
        
        /* Add the aggregation row with the aggregated statistics */
        final LastRowAggregatedValue[] aggregatedData = new LastRowAggregatedValue[netPlanViewTableHeader.length + attributesColumns.size()];
        Arrays.fill(aggregatedData, new LastRowAggregatedValue());
        aggregatedData[COLUMN_CAPACITY] = new LastRowAggregatedValue(dataAggregator[COLUMN_CAPACITY]);
        aggregatedData[COLUMN_CARRIEDTRAFFIC] = new LastRowAggregatedValue(dataAggregator[COLUMN_CARRIEDTRAFFIC]);
        aggregatedData[COLUMN_OCCUPIEDCAPACITY] = new LastRowAggregatedValue(dataAggregator[COLUMN_OCCUPIEDCAPACITY]);
        aggregatedData[COLUMN_LENGTH] = new LastRowAggregatedValue(dataAggregator[COLUMN_LENGTH]);
        aggregatedData[COLUMN_PROPDELAYMS] = new LastRowAggregatedValue(dataAggregator[COLUMN_PROPDELAYMS]);
        aggregatedData[COLUMN_NUMROUTES] = new LastRowAggregatedValue(dataAggregator[COLUMN_NUMROUTES]);
        aggregatedData[COLUMN_NUMBACKUPROUTES] = new LastRowAggregatedValue(dataAggregator[COLUMN_NUMBACKUPROUTES]);
        aggregatedData[COLUMN_NUMTREES] = new LastRowAggregatedValue(dataAggregator[COLUMN_NUMTREES]);
        aggregatedData[COLUMN_SRGS] = new LastRowAggregatedValue(dataAggregator[COLUMN_SRGS]);
        aggregatedData[COLUMN_COUPLEDTODEMAND] = new LastRowAggregatedValue(dataAggregator[COLUMN_COUPLEDTODEMAND]);
        allLinkData.add(aggregatedData);


        return allLinkData;
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
        return rf == null ? callback.getDesign().hasLinks(layer) : rf.hasLinks(layer);
    }

    @Override
    public int getAttributesColumnIndex()
    {
        return COLUMN_ATTRIBUTES;
    }

//    public int[] getColumnsOfSpecialComparatorForSorting() {
//        return new int[]{3, 4, 16, 17, 18, 19};
//    } //{ return new int [] { 3,4,6,16,17,18,19 }; }

    private static TableModel createTableModel(final GUINetworkDesign callback)
    {
//    	final TopologyPanel topologyPanel = callback.getTopologyPanel();
        final VisualizationState vs = callback.getVisualizationState();
        TableModel linkTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                if (!vs.isNetPlanEditable()) return false;
                if (columnIndex >= netPlanViewTableHeader.length) return true;
                if (getValueAt(rowIndex, columnIndex) == null) return false;
                if (rowIndex == getRowCount() - 1) return false;
                switch (columnIndex)
                {
                    case COLUMN_ID:
                    case COLUMN_INDEX:
                    case COLUMN_ORIGINNODE:
                    case COLUMN_DESTNODE:
                    case COLUMN_CARRIEDTRAFFIC:
                    case COLUMN_OCCUPIEDCAPACITY:
                    case COLUMN_UTILIZATION:
                    case COLUMN_ISBOTTLENECK:
                    case COLUMN_PROPDELAYMS:
                    case COLUMN_NUMROUTES:
                    case COLUMN_NUMBACKUPROUTES:
                    case COLUMN_NUMFORWRULES:
                    case COLUMN_NUMTREES:
                    case COLUMN_SRGS:
                    case COLUMN_COUPLEDTODEMAND:
                    case COLUMN_ATTRIBUTES:
                    case COLUMN_TAGS:
                        return false;
                    case COLUMN_STATE:
                    case COLUMN_LENGTH:
                    case COLUMN_PROPSPEED:
                        return true;

                    case COLUMN_CAPACITY:
                        NetPlan netPlan = callback.getDesign();
                        if (getValueAt(rowIndex, 0) == null)
                            rowIndex = rowIndex - 1;
                        final long linkId = (Long) getValueAt(rowIndex, 0);
                        final Link link = netPlan.getLinkFromId(linkId);
                        if (link.isCoupled()) return false;
                        else return true;


                    default:
                        return true;
                }
            }

            @Override
            public void setValueAt(Object newValue, int row, int column)
            {
                Object oldValue = getValueAt(row, column);

				/* If value doesn't change, exit from function */
                if (newValue != null && newValue.equals(oldValue)) return;

                NetPlan netPlan = callback.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long linkId = (Long) getValueAt(row, 0);
                Link link = netPlan.getLinkFromId(linkId);

				/* Perform checks, if needed */
                try
                {
                    switch (column)
                    {
                        case COLUMN_SHOWHIDE:
                            if (newValue == null) return;
                            final boolean showOnCanvas = ((Boolean) newValue);
                            if (!showOnCanvas)
                            {
                                vs.hideOnCanvas(link);
                            } else
                            {
                                vs.showOnCanvas(link);
                            }
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                            callback.getVisualizationState().pickElement(link);
                            callback.updateVisualizationAfterPick();
                            callback.addNetPlanChange();
                            break;

                        case COLUMN_STATE:
                            final boolean isLinkUp = (Boolean) newValue;
                            if (vs.isWhatIfAnalysisActive())
                            {
                                final WhatIfAnalysisPane whatIfPane = callback.getWhatIfAnalysisPane();
                                whatIfPane.whatIfLinkNodesFailureStateChanged(null, null, isLinkUp ? Sets.newHashSet(link) : null, isLinkUp ? null : Sets.newHashSet(link));
                                final VisualizationState vs = callback.getVisualizationState();
                                Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> res =
                                        vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<>(callback.getDesign().getNetworkLayers()));
                                vs.setCanvasLayerVisibilityAndOrder(callback.getDesign(), res.getFirst(), res.getSecond());
                                callback.updateVisualizationAfterNewTopology();
                            } else
                            {
                                link.setFailureState(isLinkUp);
                                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                                callback.getVisualizationState().pickElement(link);
                                callback.updateVisualizationAfterPick();
                            }
                            break;

                        case COLUMN_CAPACITY:
                            String text = newValue.toString();
                            link.setCapacity(text.equalsIgnoreCase("inf") ? Double.MAX_VALUE : Double.parseDouble(text));
                            newValue = link.getCapacity();
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                            callback.getVisualizationState().pickElement(link);
                            callback.updateVisualizationAfterPick();
                            break;

                        case COLUMN_LENGTH:
                            link.setLengthInKm(Double.parseDouble(newValue.toString()));
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                            callback.getVisualizationState().pickElement(link);
                            callback.updateVisualizationAfterPick();
                            break;

                        case COLUMN_PROPSPEED:
                            link.setPropagationSpeedInKmPerSecond(Double.parseDouble(newValue.toString()));
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                            callback.getVisualizationState().pickElement(link);
                            callback.updateVisualizationAfterPick();
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex)
                {
                    ex.printStackTrace();
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying link");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };
        return linkTableModel;
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

        setDefaultRenderer(Boolean.class, new CellRenderers.LinkRenderer(getDefaultRenderer(Boolean.class), callback));
        setDefaultRenderer(Double.class, new CellRenderers.LinkRenderer(getDefaultRenderer(Double.class), callback));
        setDefaultRenderer(Object.class, new CellRenderers.LinkRenderer(getDefaultRenderer(Object.class), callback));
        setDefaultRenderer(Float.class, new CellRenderers.LinkRenderer(getDefaultRenderer(Float.class), callback));
        setDefaultRenderer(Long.class, new CellRenderers.LinkRenderer(getDefaultRenderer(Long.class), callback));
        setDefaultRenderer(Integer.class, new CellRenderers.LinkRenderer(getDefaultRenderer(Integer.class), callback));
        setDefaultRenderer(String.class, new CellRenderers.LinkRenderer(getDefaultRenderer(String.class), callback));
    }

    private void setSpecificCellRenderers()
    {
        getColumnModel().getColumn(convertColumnIndexToView(COLUMN_CAPACITY)).setCellEditor(new LinkCapacityCellEditor(new JTextField()));
    }

    private static class LinkCapacityCellEditor extends DefaultCellEditor
    {
        private static final Border black = new LineBorder(Color.black);
        private static final Border red = new LineBorder(Color.red);
        private final JTextField textField;

        public LinkCapacityCellEditor(JTextField textField)
        {
            super(textField);
            this.textField = textField;
            this.textField.setHorizontalAlignment(JTextField.RIGHT);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
        {
            textField.setBorder(black);
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }

        @Override
        public boolean stopCellEditing()
        {
            try
            {
                String text = textField.getText();
                if (!text.equalsIgnoreCase("inf"))
                {
                    double v = Double.valueOf(textField.getText());
                    if (v < 0) throw new NumberFormatException();
                }
            } catch (NumberFormatException e)
            {
                textField.setBorder(red);
                return false;
            }

            return super.stopCellEditing();
        }
    }

    @Override
    public void setColumnRowSorting()
    {
        setAutoCreateRowSorter(true);
        final Set<Integer> columnsWithDoubleAndThenParenthesis = Sets.newHashSet(COLUMN_ORIGINNODE, COLUMN_DESTNODE, COLUMN_NUMROUTES, COLUMN_NUMBACKUPROUTES, COLUMN_NUMFORWRULES, COLUMN_NUMTREES);
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
        for (Link link : getVisibleElementsInTable())
            for (Map.Entry<String, String> entry : link.getAttributes().entrySet())
                if (!attColumnsHeaders.contains(entry.getKey()))
                    attColumnsHeaders.add(entry.getKey());
        return attColumnsHeaders;
    }

    @Override
    public JPopupMenu getPopup(ElementSelection selection)
    {
        assert selection != null;

        /* Add the popup menu option of the filters */
        if (!selection.isEmpty())
        {
            if (selection.getElementType() != NetworkElementType.LINK)
                throw new RuntimeException("Unmatched items with table, selected items are of type: " + selection.getElementType());
        }

        final JScrollPopupMenu popup = new JScrollPopupMenu(20);
        final List<Link> linkRowsInTheTable = getVisibleElementsInTable();

        final List<Link> selectedLinks = (List<Link>) selection.getNetworkElements();

        if (!linkRowsInTheTable.isEmpty())
        {
            addPickOption(selection, popup);
            addFilterOptions(selection, popup);
            popup.addSeparator();
        }

        if (callback.getVisualizationState().isNetPlanEditable())
        {
            popup.add(getAddOption());

            if (!linkRowsInTheTable.isEmpty())
            {
                if (!selectedLinks.isEmpty())
                {
                    popup.add(new MenuItem_RemoveLinks(callback, selectedLinks));
                }
            }

            final List<JComponent> extraAddOptions = getExtraAddOptions();
            if (!extraAddOptions.isEmpty())
            {
                popup.addSeparator();
                for (JComponent item : extraAddOptions) popup.add(item);
            }

            if (!linkRowsInTheTable.isEmpty() && !selectedLinks.isEmpty())
            {
                List<JComponent> forcedOptions = getForcedOptions(selection);
                if (!forcedOptions.isEmpty()) popup.addSeparator();
                for (JComponent item : forcedOptions) popup.add(item);

                List<JComponent> extraOptions = getExtraOptions(selection);
                if (!extraOptions.isEmpty()) popup.addSeparator();
                for (JComponent item : extraOptions) popup.add(item);

                addPopupMenuAttributeOptions(selection, popup);
            }
        }

        return popup;
    }

    @Override
    public void pickSelection(ElementSelection selection)
    {
        if (getVisibleElementsInTable().isEmpty()) return;
        if (selection.getElementType() != NetworkElementType.LINK)
            throw new RuntimeException("Unmatched items with table, selected items are of type: " + selection.getElementType());

        callback.getVisualizationState().pickElement((List<Link>) selection.getNetworkElements());
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

        addItem.addActionListener(e ->
        {
            try
            {
                AdvancedJTable_demand.createLinkDemandGUI(networkElementType, callback);
            } catch (Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
            }
        });

        NetPlan netPlan = callback.getDesign();
        int N = netPlan.getNumberOfNodes();
        if (N < 2) addItem.setEnabled(false);

        return addItem;
    }


    @Override
    protected List<JComponent> getExtraAddOptions()
    {
        List<JComponent> options = new LinkedList<JComponent>();
        NetPlan netPlan = callback.getDesign();

        if (netPlan.getNumberOfNodes() >= 2)
        {
            final JMenuItem fullMeshEuclidean = new JMenuItem("Generate full-mesh (link length as Euclidean distance)");
            final JMenuItem fullMeshHaversine = new JMenuItem("Generate full-mesh (link length as Haversine distance)");
            options.add(fullMeshEuclidean);
            options.add(fullMeshHaversine);

            fullMeshEuclidean.addActionListener(new FullMeshTopologyActionListener(callback, true));
            fullMeshHaversine.addActionListener(new FullMeshTopologyActionListener(callback, false));
        }

        return options;
    }


    @Override
    protected List<JComponent> getExtraOptions(final ElementSelection selection)
    {
        assert selection != null;

        List<JComponent> options = new LinkedList<>();

        final List<Link> rowVisibleLinks = getVisibleElementsInTable();
        final List<Link> selectedLinks = (List<Link>) selection.getNetworkElements();
        final NetPlan netPlan = callback.getDesign();

        if (!selectedLinks.isEmpty())
        {
            if (netPlan.isMultilayer())
            {
                options.add(new MenuItem_DecoupleLinks(callback, selectedLinks));

                JMenuItem createLowerLayerDemandFromLinkItem = new JMenuItem("Create lower layer coupled demand from uncoupled links in selection");
                createLowerLayerDemandFromLinkItem.addActionListener(e ->
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
                        int result = JOptionPane.showConfirmDialog(null, pane, "Please select the lower layer to create the demand", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (result != JOptionPane.OK_OPTION) return;

                        try
                        {
                            final long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                            for (Link link : selectedLinks)
                                if (!link.isCoupled())
                                    link.coupleToNewDemandCreated(netPlan.getNetworkLayerFromId(layerId));
                            callback.getVisualizationState().resetPickedState();
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.DEMAND));
                            callback.addNetPlanChange();
                            break;
                        } catch (Throwable ex)
                        {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error creating lower layer demand from link");
                        }
                    }
                });

                options.add(createLowerLayerDemandFromLinkItem);

                final Set<Link> coupledLinks = rowVisibleLinks.stream().filter(Link::isCoupled).collect(Collectors.toSet());
                if (!coupledLinks.isEmpty())
                {
                    if (coupledLinks.size() < rowVisibleLinks.size())
                    {
                        JMenuItem createLowerLayerDemandsFromLinksItem = new JMenuItem("Create lower layer unicast demands from uncoupled links in selection");
                        createLowerLayerDemandsFromLinksItem.addActionListener(e ->
                        {
                            final JComboBox layerSelector = new WiderJComboBox();
                            for (NetworkLayer layer : netPlan.getNetworkLayers())
                            {
                                if (layer.getId() == netPlan.getNetworkLayerDefault().getId()) continue;

                                final String layerName = layer.getName();
                                String layerLabel = "Layer " + layer.getId();
                                if (!layerName.isEmpty()) layerLabel += " (" + layerName + ")";

                                layerSelector.addItem(StringLabeller.of(layer.getId(), layerLabel));
                            }

                            layerSelector.setSelectedIndex(0);

                            JPanel pane = new JPanel();
                            pane.add(new JLabel("Select layer: "));
                            pane.add(layerSelector);

                            while (true)
                            {
                                int result = JOptionPane.showConfirmDialog(null, pane, "Please select the lower layer to create demands", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                                if (result != JOptionPane.OK_OPTION) return;

                                try
                                {
                                    long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                                    NetworkLayer layer = netPlan.getNetworkLayerFromId(layerId);
                                    for (Link link : selectedLinks)
                                        if (!link.isCoupled())
                                            link.coupleToNewDemandCreated(layer);
                                    callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                                    callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK, NetworkElementType.DEMAND));
                                    callback.addNetPlanChange();
                                    break;
                                } catch (Throwable ex)
                                {
                                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error creating lower layer demands");
                                }
                            }
                        });

                        options.add(createLowerLayerDemandsFromLinksItem);
                    }
                }

                if (selectedLinks.size() == 1)
                {
                    JMenuItem coupleLinkToDemand = new JMenuItem("Couple link to lower layer demand");
                    coupleLinkToDemand.addActionListener(e ->
                    {
                        Collection<Long> layerIds = netPlan.getNetworkLayerIds();
                        final JComboBox layerSelector = new WiderJComboBox();
                        final JComboBox demandSelector = new WiderJComboBox();
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

                                demandSelector.removeAllItems();
                                for (Demand demand : netPlan.getDemands(netPlan.getNetworkLayerFromId(selectedLayerId)))
                                {
                                    if (demand.isCoupled()) continue;

                                    long ingressNodeId = demand.getIngressNode().getId();
                                    long egressNodeId = demand.getEgressNode().getId();
                                    String ingressNodeName = demand.getIngressNode().getName();
                                    String egressNodeName = demand.getEgressNode().getName();

                                    demandSelector.addItem(StringLabeller.unmodifiableOf(demand.getId(), "d" + demand.getId() + " [n" + ingressNodeId + " (" + ingressNodeName + ") -> n" + egressNodeId + " (" + egressNodeName + ")]"));
                                }
                            }

                            if (demandSelector.getItemCount() == 0)
                            {
                                demandSelector.setEnabled(false);
                            } else
                            {
                                demandSelector.setSelectedIndex(0);
                                demandSelector.setEnabled(true);
                            }
                        });

                        layerSelector.setSelectedIndex(-1);
                        layerSelector.setSelectedIndex(0);

                        JPanel pane = new JPanel(new MigLayout("", "[][grow]", "[][]"));
                        pane.add(new JLabel("Select layer: "));
                        pane.add(layerSelector, "growx, wrap");
                        pane.add(new JLabel("Select demand: "));
                        pane.add(demandSelector, "growx, wrap");

                        while (true)
                        {
                            int result = JOptionPane.showConfirmDialog(null, pane, "Please select the lower layer demand", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return;

                            try
                            {
                                try
                                {
                                    final Long demandId = (Long) ((StringLabeller) demandSelector.getSelectedItem()).getObject();
                                    if (demandId == null) throw new NullPointerException();
                                    final Demand demand = netPlan.getDemandFromId(demandId);
                                    if (demand == null) throw new NullPointerException();

                                    final Link link = selectedLinks.get(0);
                                    demand.coupleToUpperLayerLink(link);
                                } catch (Throwable ex)
                                {
                                    throw new RuntimeException("No demand was selected");
                                }

                                callback.getVisualizationState().resetPickedState();
                                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK, NetworkElementType.DEMAND));
                                callback.addNetPlanChange();
                                break;
                            } catch (Throwable ex)
                            {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error coupling lower layer demand to link");
                            }
                        }
                    });

                    options.add(coupleLinkToDemand);
                }
            }
            if (rowVisibleLinks.size() > 1)
            {
                if (!options.isEmpty()) options.add(new JPopupMenu.Separator());

                JMenuItem caFixValue = new JMenuItem("Set selected links capacity");
                caFixValue.addActionListener(e ->
                {
                    double u_e;

                    while (true)
                    {
                        String str = JOptionPane.showInputDialog(null, "Capacity value", "Set capacity to selected links", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try
                        {
                            u_e = Double.parseDouble(str);
                            if (u_e < 0) throw new NumberFormatException();

                            break;
                        } catch (NumberFormatException ex)
                        {
                            ErrorHandling.showErrorDialog("Non-valid capacity value. Please, introduce a non-negative number", "Error setting capacity value");
                        }
                    }

                    try
                    {
                        for (Link link : rowVisibleLinks) link.setCapacity(u_e);
                        callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                        callback.addNetPlanChange();
                    } catch (Throwable ex)
                    {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set capacity");
                    }
                });

                options.add(caFixValue);

                JMenuItem caFixValueUtilization = new JMenuItem("Set selected links capacity to match a given utilization");
                caFixValueUtilization.addActionListener(e ->
                {
                    double utilization;

                    while (true)
                    {
                        String str = JOptionPane.showInputDialog(null, "Link utilization value", "Set capacity to selected links to match a given utilization", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try
                        {
                            utilization = Double.parseDouble(str);
                            if (utilization <= 0) throw new NumberFormatException();

                            break;
                        } catch (NumberFormatException ex)
                        {
                            ErrorHandling.showErrorDialog("Non-valid link utilization value. Please, introduce a strictly positive number", "Error setting link utilization value");
                        }
                    }

                    try
                    {
                        for (Link link : rowVisibleLinks) link.setCapacity(link.getOccupiedCapacity() / utilization);
                        callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                        callback.addNetPlanChange();
                    } catch (Throwable ex)
                    {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set capacity to links according to a given link utilization");
                    }
                });

                options.add(caFixValueUtilization);

                JMenuItem setLength = new JMenuItem("Set selected links length");
                setLength.addActionListener(e ->
                {
                    double l_e;

                    while (true)
                    {
                        String str = JOptionPane.showInputDialog(null, "Link length value (in km)", "Set link length to selected table links", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try
                        {
                            l_e = Double.parseDouble(str);
                            if (l_e < 0) throw new RuntimeException();

                            break;
                        } catch (Throwable ex)
                        {
                            ErrorHandling.showErrorDialog("Non-valid link length value. Please, introduce a non-negative number", "Error setting link length");
                        }
                    }

                    try
                    {
                        for (Link link : rowVisibleLinks) link.setLengthInKm(l_e);
                        callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                        callback.addNetPlanChange();
                    } catch (Throwable ex)
                    {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set link length to selected links");
                    }
                });
                options.add(setLength);

                options.add(new MenuItem_LengthToEuclidean(callback, selectedLinks));

                options.add(new MenuItem_LengthToHaversine(callback, selectedLinks));

                JMenuItem scaleLength = new JMenuItem("Scale selected links length");
                scaleLength.addActionListener(e ->
                {
                    double scaleFactor;

                    while (true)
                    {
                        String str = JOptionPane.showInputDialog(null, "(Multiplicative) Scale factor", "Scale link length", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try
                        {
                            scaleFactor = Double.parseDouble(str);
                            if (scaleFactor < 0) throw new RuntimeException();

                            break;
                        } catch (Throwable ex)
                        {
                            ErrorHandling.showErrorDialog("Non-valid scale value. Please, introduce a non-negative number", "Error setting scale factor");
                        }
                    }

                    for (Link link : selectedLinks)
                        link.setLengthInKm(link.getLengthInKm() * scaleFactor);

                    callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.LINK));
                    callback.addNetPlanChange();
                });

                options.add(scaleLength);
            }
        }
        return options;
    }


    @Override
    protected List<JComponent> getForcedOptions(ElementSelection selection)
    {
        assert selection != null;

        final List<Link> links = (List<Link>) selection.getNetworkElements();

        List<JComponent> options = new LinkedList<>();
        if (!selection.isEmpty())
        {
            options.add(new MenuItem_ShowLinks(callback, links));
            options.add(new MenuItem_HideLinks(callback, links));
        }

        return options;
    }

    static class FullMeshTopologyActionListener implements ActionListener
    {
        private final GUINetworkDesign callback;
        private final boolean euclidean;

        public FullMeshTopologyActionListener(GUINetworkDesign callback, boolean euclidean)
        {
            this.callback = callback;
            this.euclidean = euclidean;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            assert callback != null;

            NetPlan netPlan = callback.getDesign();

            // Ask for current element removal
            if (netPlan.hasLinks(netPlan.getNetworkLayerDefault()))
            {
                final int answer = JOptionPane.showConfirmDialog(null, "Remove all existing links?", "", JOptionPane.YES_NO_CANCEL_OPTION);
                if (answer == JOptionPane.CANCEL_OPTION || answer == JOptionPane.CLOSED_OPTION) return;
                if (answer == JOptionPane.OK_OPTION) netPlan.removeAllLinks(netPlan.getNetworkLayerDefault());
            }

            for (long nodeId_1 : netPlan.getNodeIds())
            {
                for (long nodeId_2 : netPlan.getNodeIds())
                {
                    if (nodeId_1 >= nodeId_2) continue;
                    Node n1 = netPlan.getNodeFromId(nodeId_1);
                    Node n2 = netPlan.getNodeFromId(nodeId_2);

                    Pair<Link, Link> out = netPlan.addLinkBidirectional(n1, n2, 0, euclidean ? netPlan.getNodePairEuclideanDistance(n1, n2) : netPlan.getNodePairHaversineDistanceInKm(n1, n2), 200000, null);
                }
            }
            callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
            callback.addNetPlanChange();
        }

    }

    private List<Link> getVisibleElementsInTable()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
        return rf == null ? callback.getDesign().getLinks(layer) : rf.getVisibleLinks(layer);
    }

    static class MenuItem_RemoveLinks extends JMenuItem
    {
        MenuItem_RemoveLinks(GUINetworkDesign callback, List<Link> selectedLinks)
        {
            this.setText("Remove selected links");
            this.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    try
                    {
                        for (Link selectedLink : selectedLinks) selectedLink.remove();

                        callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                        callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                        callback.addNetPlanChange();
                    } catch (Throwable ex)
                    {
                        ErrorHandling.addErrorOrException(ex, getClass());
                        ErrorHandling.showErrorDialog("Unable to remove links");
                    }
                }
            });
        }
    }

    static class MenuItem_ShowLinks extends JMenuItem
    {
        MenuItem_ShowLinks(GUINetworkDesign callback, List<Link> links)
        {
            this.setText("Show selected links");
            this.addActionListener(e ->
            {
                for (Link link : links)
                    callback.getVisualizationState().showOnCanvas(link);

                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                callback.addNetPlanChange();
            });
        }
    }

    static class MenuItem_HideLinks extends JMenuItem
    {
        MenuItem_HideLinks(GUINetworkDesign callback, List<Link> links)
        {
            this.setText("Hide selected links");
            this.addActionListener(e ->
            {
                for (Link link : links)
                    callback.getVisualizationState().hideOnCanvas(link);

                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                callback.addNetPlanChange();
            });
        }
    }

    static class MenuItem_DecoupleLinks extends JMenuItem
    {
        MenuItem_DecoupleLinks(GUINetworkDesign callback, List<Link> selectedLinks)
        {
            this.setText("Decouple coupled links from selection");
            this.addActionListener(e ->
            {
                for (Link link : selectedLinks)
                    link.getCoupledDemand().decouple();

                callback.getVisualizationState().resetPickedState();
                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                callback.addNetPlanChange();
            });
        }
    }

    static class MenuItem_LengthToEuclidean extends JMenuItem
    {
        MenuItem_LengthToEuclidean(GUINetworkDesign callback, List<Link> selectedLinks)
        {
            this.setText("Set selected links length to node-pair Euclidean distance");
            this.addActionListener(e ->
            {
                final NetPlan netPlan = callback.getDesign();

                for (Link link : selectedLinks)
                {
                    Node originNode = link.getOriginNode();
                    Node destinationNode = link.getDestinationNode();
                    double euclideanDistance = netPlan.getNodePairEuclideanDistance(originNode, destinationNode);
                    link.setLengthInKm(euclideanDistance);
                }
                callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.LINK));
                callback.addNetPlanChange();
            });
        }
    }

    static class MenuItem_LengthToHaversine extends JMenuItem
    {
        MenuItem_LengthToHaversine(GUINetworkDesign callback, List<Link> selectedLinks)
        {
            this.setText("Set selected links length to node-pair Haversine distance (longitude-latitude) in km");
            this.addActionListener(e ->
            {
                final NetPlan netPlan = callback.getDesign();

                for (Link link : selectedLinks)
                {
                    Node originNode = link.getOriginNode();
                    Node destinationNode = link.getDestinationNode();
                    double haversineDistanceInKm = netPlan.getNodePairHaversineDistanceInKm(originNode, destinationNode);
                    link.setLengthInKm(haversineDistanceInKm);
                }
                callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.LINK));
                callback.addNetPlanChange();
            });
        }
    }
}
