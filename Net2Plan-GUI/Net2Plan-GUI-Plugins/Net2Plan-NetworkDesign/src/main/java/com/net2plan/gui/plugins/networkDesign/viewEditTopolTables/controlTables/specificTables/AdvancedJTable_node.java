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
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.CanvasFunction;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AggregationUtils;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.LastRowAggregatedValue;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.plugins.networkDesign.whatIfAnalysisPane.WhatIfAnalysisPane;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.JScrollPopupMenu;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.collections15.BidiMap;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.*;

/**
 */
@SuppressWarnings("unchecked")
public class AdvancedJTable_node extends AdvancedJTable_networkElement
{
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_INDEX = 1;
    public static final int COLUMN_SHOWHIDE = 2;
    public static final int COLUMN_NAME = 3;
    public static final int COLUMN_STATE = 4;
    public static final int COLUMN_XCOORD = 5;
    public static final int COLUMN_YCOORD = 6;
    public static final int COLUMN_NUMOUTLINKS = 7;
    public static final int COLUMN_NUMINLINKS = 8;
    public static final int COLUMN_OUTGOINGLINKTRAFFIC = 9;
    public static final int COLUMN_INCOMINGLINKTRAFFIC = 10;
    public static final int COLUMN_OUTTRAFFICUNICAST = 11;
    public static final int COLUMN_INTRAFFICUNICAST = 12;
    public static final int COLUMN_OUTTRAFFICMULTICAST = 13;
    public static final int COLUMN_INTRAFFICMULTICAST = 14;
    public static final int COLUMN_SRGS = 15;
    public static final int COLUMN_POPULATION = 16;
    public static final int COLUMN_SITE = 17;
    public static final int COLUMN_TAGS = 18;
    public static final int COLUMN_ATTRIBUTES = 19;
    private static final String netPlanViewTabName = "Nodes";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "Show/Hide", "Name",
            "State", "xCoord / Longitude", "yCoord / Latitude", "# out links", "# in links",
            "Out-link cap", "In-link cap", "Out traffic", "In traffic", "Out multicast traffic)", "In multicast traffic",
            "# SRGs", "Population", "Site", "Tags", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)",
            "Index (consecutive integer starting in zero)",
            "Indicates whether or not the node is visible in the topology canvas",
            "Node name", "Indicates whether the node is in up/down state", "Coordinate along x-axis (i.e. longitude)",
            "Coordinate along y-axis (i.e. latitude)",
            "Number of outgoing links", "Number of incoming links",
            "Total capacity in outgoing links", "Total capacity in incoming links",
            "Total out UNICAST traffic (carried)", "Total in UNICAST traffic (carried)",
            "Total out MULTICAST traffic (carried)", "Total in MULTICAST traffic (carried)",
            "Number of SRGs including this node", "Total population in this node", "Site this node belongs to", "Node-specific tags", "Node-specific attributes");

    /**
     * Default constructor.
     *
     * @param callback The network callback
     * @since 0.2.0
     */
    public AdvancedJTable_node(final GUINetworkDesign callback)
    {
        super(createTableModel(callback), callback, NetworkElementType.NODE);
        setDefaultCellRenderers(callback);
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


    public List<Object[]> getAllData(NetPlan currentState, ArrayList<String> attributesTitles)
    {
        final List<Node> rowVisibleNodes = getVisibleElementsInTable();
        final List<Object[]> allNodeData = new LinkedList<Object[]>();
        final NetworkLayer layer = currentState.getNetworkLayerDefault();
        final double[] dataAggregator = new double[netPlanViewTableHeader.length];

        for (Node node : rowVisibleNodes)
        {
            final Set<NetworkLayer> workingLayers = node.getWorkingLayers();
            if (!workingLayers.isEmpty() && !workingLayers.contains(layer)) continue;
            Object[] nodeData = new Object[netPlanViewTableHeader.length + attributesTitles.size()];
            nodeData[COLUMN_ID] = node.getId();
            nodeData[COLUMN_INDEX] = node.getIndex();
            nodeData[COLUMN_SHOWHIDE] = !callback.getVisualizationState().isHiddenOnCanvas(node);
            nodeData[COLUMN_NAME] = node.getName();
            nodeData[COLUMN_STATE] = node.isUp();
            nodeData[COLUMN_XCOORD] = node.getXYPositionMap().getX();
            nodeData[COLUMN_YCOORD] = node.getXYPositionMap().getY();
            nodeData[COLUMN_NUMOUTLINKS] = node.getOutgoingLinks(layer).size();
            nodeData[COLUMN_NUMINLINKS] = node.getIncomingLinks(layer).size();
            nodeData[COLUMN_INCOMINGLINKTRAFFIC] = node.getIncomingLinksTraffic(layer);
            nodeData[COLUMN_OUTGOINGLINKTRAFFIC] = node.getOutgoingLinksTraffic(layer);
            nodeData[COLUMN_OUTTRAFFICUNICAST] = node.getIngressCarriedTraffic(layer);
            nodeData[COLUMN_INTRAFFICUNICAST] = node.getEgressCarriedTraffic(layer);
            nodeData[COLUMN_OUTTRAFFICMULTICAST] = node.getIngressCarriedMulticastTraffic(layer);
            nodeData[COLUMN_INTRAFFICMULTICAST] = node.getEgressCarriedMulticastTraffic(layer);
            nodeData[COLUMN_SRGS] = node.getSRGs().size();
            nodeData[COLUMN_POPULATION] = node.getPopulation();
            nodeData[COLUMN_SITE] = node.getSiteName() != null ? node.getSiteName() : "";
            nodeData[COLUMN_TAGS] = StringUtils.listToString(Lists.newArrayList(node.getTags()));
            nodeData[COLUMN_ATTRIBUTES] = StringUtils.mapToString(node.getAttributes());

            for (int i = netPlanViewTableHeader.length; i < netPlanViewTableHeader.length + attributesTitles.size(); i++)
                if (node.getAttributes().containsKey(attributesTitles.get(i - netPlanViewTableHeader.length)))
                    nodeData[i] = node.getAttribute(attributesTitles.get(i - netPlanViewTableHeader.length));

            AggregationUtils.updateRowSum(dataAggregator, COLUMN_NUMOUTLINKS, nodeData[COLUMN_NUMOUTLINKS]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_NUMINLINKS, nodeData[COLUMN_NUMINLINKS]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_INCOMINGLINKTRAFFIC, nodeData[COLUMN_INCOMINGLINKTRAFFIC]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_OUTGOINGLINKTRAFFIC, nodeData[COLUMN_OUTGOINGLINKTRAFFIC]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_OUTTRAFFICUNICAST, nodeData[COLUMN_OUTTRAFFICUNICAST]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_INTRAFFICUNICAST, nodeData[COLUMN_INTRAFFICUNICAST]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_OUTTRAFFICMULTICAST, nodeData[COLUMN_OUTTRAFFICMULTICAST]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_INTRAFFICMULTICAST, nodeData[COLUMN_INTRAFFICMULTICAST]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_POPULATION, nodeData[COLUMN_POPULATION]);

            allNodeData.add(nodeData);
        }
        
        /* Add the aggregation row with the aggregated statistics */
        final LastRowAggregatedValue[] aggregatedData = new LastRowAggregatedValue[netPlanViewTableHeader.length + attributesTitles.size()];
        Arrays.fill(aggregatedData, new LastRowAggregatedValue());
        aggregatedData[COLUMN_NUMOUTLINKS] = new LastRowAggregatedValue(dataAggregator[COLUMN_NUMOUTLINKS]);
        aggregatedData[COLUMN_NUMINLINKS] = new LastRowAggregatedValue(dataAggregator[COLUMN_NUMINLINKS]);
        aggregatedData[COLUMN_OUTTRAFFICUNICAST] = new LastRowAggregatedValue(dataAggregator[COLUMN_OUTTRAFFICUNICAST]);
        aggregatedData[COLUMN_INTRAFFICUNICAST] = new LastRowAggregatedValue(dataAggregator[COLUMN_INTRAFFICUNICAST]);
        aggregatedData[COLUMN_INCOMINGLINKTRAFFIC] = new LastRowAggregatedValue(dataAggregator[COLUMN_INCOMINGLINKTRAFFIC]);
        aggregatedData[COLUMN_OUTGOINGLINKTRAFFIC] = new LastRowAggregatedValue(dataAggregator[COLUMN_OUTGOINGLINKTRAFFIC]);
        aggregatedData[COLUMN_OUTTRAFFICMULTICAST] = new LastRowAggregatedValue(dataAggregator[COLUMN_OUTTRAFFICMULTICAST]);
        aggregatedData[COLUMN_INTRAFFICMULTICAST] = new LastRowAggregatedValue(dataAggregator[COLUMN_INTRAFFICMULTICAST]);
        aggregatedData[COLUMN_POPULATION] = new LastRowAggregatedValue(dataAggregator[COLUMN_POPULATION]);
        allNodeData.add(aggregatedData);

        return allNodeData;
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


    public String getTabName()
    {
        return netPlanViewTabName;
    }

    public String[] getTableHeaders()
    {
        return netPlanViewTableHeader;
    }

    public String[] getTableTips()
    {
        return netPlanViewTableTips;
    }

    public boolean hasElements()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
        return rf == null ? callback.getDesign().hasNodes() : rf.hasNodes(layer);
    }

    public int getNumberOfElements(boolean consideringFilters)
    {
        final NetPlan np = callback.getDesign();
        final NetworkLayer layer = np.getNetworkLayerDefault();
        if (!consideringFilters) return np.getNumberOfNodes();

        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        return rf.getNumberOfNodes(layer);
    }

    @Override
    public int getAttributesColumnIndex()
    {
        return COLUMN_ATTRIBUTES;
    }

    @Override
    public ArrayList<String> getAttributesColumnsHeaders()
    {
        ArrayList<String> attColumnsHeaders = new ArrayList<>();
        for (Node node : getVisibleElementsInTable())
            for (Map.Entry<String, String> entry : node.getAttributes().entrySet())
                if (attColumnsHeaders.contains(entry.getKey()) == false)
                    attColumnsHeaders.add(entry.getKey());
        return attColumnsHeaders;
    }

    private static TableModel createTableModel(final GUINetworkDesign callback)
    {
        TableModel nodeTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                if (!callback.getVisualizationState().isNetPlanEditable()) return false;
                if (columnIndex >= netPlanViewTableHeader.length) return true;
                if (rowIndex == getRowCount() - 1) return false;
                if (getValueAt(rowIndex, columnIndex) == null) return false;

                return columnIndex == COLUMN_SHOWHIDE || columnIndex == COLUMN_NAME || columnIndex == COLUMN_STATE || columnIndex == COLUMN_XCOORD
                        || columnIndex == COLUMN_YCOORD || columnIndex == COLUMN_POPULATION || columnIndex == COLUMN_SITE;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column)
            {
                Object oldValue = getValueAt(row, column);

				/* If value doesn't change, exit from function */
                if (newValue != null && newValue.equals(oldValue)) return;
                if (newValue == null) return;

                NetPlan netPlan = callback.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long nodeId = (Long) getValueAt(row, 0);
                final Node node = netPlan.getNodeFromId(nodeId);
                try
                {
                    switch (column)
                    {
                        case COLUMN_SHOWHIDE:
                        {
                            if (!(newValue instanceof Boolean)) return;
                            if (!(Boolean) newValue)
                                callback.getVisualizationState().hideOnCanvas(node);
                            else
                                callback.getVisualizationState().showOnCanvas(node);
                            break;
                        }
                        case COLUMN_NAME:
                        {
                            node.setName(newValue.toString());
                            break;
                        }
                        case COLUMN_STATE:
                        {
                            final boolean isNodeUp = (Boolean) newValue;
                            if (callback.getVisualizationState().isWhatIfAnalysisActive())
                            {
                                final WhatIfAnalysisPane whatIfPane = callback.getWhatIfAnalysisPane();
                                synchronized (whatIfPane)
                                {
                                    whatIfPane.whatIfLinkNodesFailureStateChanged(isNodeUp ? Sets.newHashSet(node) : null, isNodeUp ? null : Sets.newHashSet(node), null, null);
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

                                return;
                            } else
                            {
                                node.setFailureState(isNodeUp);
                            }
                            break;
                        }
                        case COLUMN_XCOORD:
                        case COLUMN_YCOORD:
                        {
                            Point2D newPosition = column == COLUMN_XCOORD ?
                                    new Point2D.Double(Double.parseDouble(newValue.toString()), node.getXYPositionMap().getY()) :
                                    new Point2D.Double(node.getXYPositionMap().getX(), Double.parseDouble(newValue.toString()));
                            node.setXYPositionMap(newPosition);
                            break;
                        }
                        case COLUMN_POPULATION:
                        {
                            String text = newValue.toString();
                            double value = Double.parseDouble(text);

                            node.setPopulation(value);
                            break;
                        }
                        case COLUMN_SITE:
                        {
                            final String text = newValue.toString();
                            node.setSiteName(text);
                            break;
                        }
                        default:
                            break;
                    }

                    callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.NODE));
                    callback.getVisualizationState().pickElement(node);
                    callback.updateVisualizationAfterPick();
                    callback.addNetPlanChange();
                } catch (Throwable ex)
                {
                    ex.printStackTrace();
                    ErrorHandling.showErrorDialog();
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };

        return nodeTableModel;
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

        setDefaultRenderer(Boolean.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Boolean.class), callback, NetworkElementType.NODE));
        setDefaultRenderer(Double.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Double.class), callback, NetworkElementType.NODE));
        setDefaultRenderer(Object.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Object.class), callback, NetworkElementType.NODE));
        setDefaultRenderer(Float.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Float.class), callback, NetworkElementType.NODE));
        setDefaultRenderer(Long.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Long.class), callback, NetworkElementType.NODE));
        setDefaultRenderer(Integer.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Integer.class), callback, NetworkElementType.NODE));
        setDefaultRenderer(String.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(String.class), callback, NetworkElementType.NODE));
    }

    @Override
    public void setColumnRowSorting()
    {
        setAutoCreateRowSorter(true);
        final Set<Integer> columnsWithDoubleAndThenParenthesis = Sets.newHashSet(COLUMN_NUMOUTLINKS, COLUMN_NUMINLINKS, COLUMN_OUTTRAFFICUNICAST, COLUMN_INTRAFFICUNICAST, COLUMN_OUTTRAFFICMULTICAST, COLUMN_INTRAFFICMULTICAST);
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
    protected JPopupMenu getPopup(ElementSelection selection)
    {
        assert selection != null;

        final JScrollPopupMenu popup = new JScrollPopupMenu(20);

        if (!selection.isEmpty())
        {
            if (selection.getElementType() != NetworkElementType.NODE)
                throw new RuntimeException("Unmatched selected items with table, selected items are of type: " + selection.getElementType());
        }

        final List<Node> rowsInTheTable = this.getVisibleElementsInTable(); // Only visible rows
        final List<Node> selectedNodes = (List<Node>) selection.getNetworkElements();

        /* Add the popup menu option of the filters */
        if (!rowsInTheTable.isEmpty())
        {
        	addPickOption(selection, popup);
            addFilterOptions(selection, popup);
            popup.addSeparator();
        }

        // Popup buttons
        if (callback.getVisualizationState().isNetPlanEditable())
        {
            popup.add(this.getAddOption());

            if (!rowsInTheTable.isEmpty())
            {
                if (!selectedNodes.isEmpty())
                {
                    popup.add(new MenuItem_RemoveNodes(callback, selectedNodes));
                    popup.addSeparator();
                }

                for (JComponent item : getExtraAddOptions()) popup.add(item);

                List<JComponent> forcedOptions = getForcedOptions(selection);
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
    protected void pickSelection(ElementSelection selection)
    {
        if (getVisibleElementsInTable().isEmpty()) return;
        if (selection.getElementType() != NetworkElementType.NODE)
            throw new RuntimeException("Unmatched selected items with table, selected items are of type: " + selection.getElementType());

        callback.getVisualizationState().pickElement(selection.getNetworkElements());
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
        return new MenuItem_AddNode(callback);
    }


    @Override
    protected List<JComponent> getExtraAddOptions()
    {
        return new LinkedList<>();
    }


    @Override
    protected List<JComponent> getExtraOptions(final ElementSelection selection)
    {
        assert selection != null;

        final List<Node> selectedNodes = (List<Node>) selection.getNetworkElements();

        final List<JComponent> options = new LinkedList<>();

        if (!selectedNodes.isEmpty())
        {
            options.add(new MenuItem_SwitchCoordinates(callback, selectedNodes));
            options.add(new MenuItem_NameFromAttribute(callback, selectedNodes));
            options.add(new MenuItem_CreatePlanningDomain(callback, selectedNodes));

        }

        return options;
    }


    @Override
    protected List<JComponent> getForcedOptions(ElementSelection selection)
    {
        assert selection != null;

        List<JComponent> options = new LinkedList<>();

        final int numRows = model.getRowCount();
        if (numRows > 1)
        {
            if (!selection.isEmpty())
            {
                final List<Node> nodes = (List<Node>) selection.getNetworkElements();

                options.add(new MenuItem_ShowSelection(callback, nodes));
                options.add(new MenuItem_HideSelection(callback, nodes));
            }
        }

        return options;
    }

    private List<Node> getVisibleElementsInTable()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
        return rf == null ? callback.getDesign().getNodes() : rf.getVisibleNodes(layer);
    }

    static class MenuItem_ShowSelection extends JMenuItem
    {
        MenuItem_ShowSelection(GUINetworkDesign callback, List<Node> nodes)
        {
            this.setText("Show selected nodes");

            this.addActionListener(e ->
            {
                for (Node node : nodes)
                {
                    callback.getVisualizationState().showOnCanvas(node);
                }

                callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.NODE));
                callback.addNetPlanChange();
            });
        }
    }

    static class MenuItem_HideSelection extends JMenuItem
    {
        MenuItem_HideSelection(GUINetworkDesign callback, List<Node> nodes)
        {
            this.setText("Hide selected nodes");
            this.addActionListener(e ->
            {
                for (Node node : nodes)
                    callback.getVisualizationState().hideOnCanvas(node);

                callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.NODE));
                callback.addNetPlanChange();
            });
        }
    }


    static class MenuItem_CreatePlanningDomain extends JMenuItem
    {
        MenuItem_CreatePlanningDomain(GUINetworkDesign callback, List<Node> selectedNodes)
        {
            this.setText("Create planning domain restricted to selected nodes");
            this.addActionListener(e ->
            {
                final NetPlan np = callback.getDesign();
                np.restrictDesign(new HashSet<>(selectedNodes));
                callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                callback.runCanvasOperation(CanvasFunction.ZOOM_ALL);
                callback.addNetPlanChange();
            });
        }
    }

    static class MenuItem_SwitchCoordinates extends JMenuItem
    {
        MenuItem_SwitchCoordinates(GUINetworkDesign callback, List<Node> nodes)
        {
            this.setText("Switch selected nodes' coordinates from (x,y) to (y,x)");
            this.addActionListener(e ->
            {
                for (Node node : nodes)
                {
                    Point2D currentPosition = node.getXYPositionMap();
                    node.setXYPositionMap(new Point2D.Double(currentPosition.getY(), currentPosition.getX()));
                }

                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                callback.runCanvasOperation(CanvasFunction.ZOOM_ALL);
                callback.addNetPlanChange();
            });
        }
    }

    static class MenuItem_NameFromAttribute extends JMenuItem
    {
        MenuItem_NameFromAttribute(GUINetworkDesign callback, List<Node> nodes)
        {
            this.setText("Set selected nodes' name from attribute");
            this.addActionListener(e ->
            {
                Set<String> attributeSet = new LinkedHashSet<String>();
                for (Node selectedNode : nodes)
                    attributeSet.addAll(selectedNode.getAttributes().keySet());

                try
                {
                    if (attributeSet.isEmpty()) throw new Exception("No attribute to select");

                    final JComboBox selector = new WiderJComboBox();
                    for (String attribute : attributeSet)
                        selector.addItem(attribute);

                    JPanel pane = new JPanel(new MigLayout("", "[][grow]", "[]"));
                    pane.add(new JLabel("Name: "));
                    pane.add(selector, "growx, wrap");

                    int result = JOptionPane.showConfirmDialog(null, pane, "Please select the attribute for name", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return;

                    String name = selector.getSelectedItem().toString();

                    for (Node node : nodes)
                        node.setName(node.getAttribute(name));

                    callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                    callback.addNetPlanChange();
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error retrieving name from attribute");
                }
            });
        }
    }

    static class MenuItem_RemoveNodes extends JMenuItem
    {
        MenuItem_RemoveNodes(GUINetworkDesign callback, List<Node> nodes)
        {
            this.setText("Remove selected nodes");
            this.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    try
                    {
                        for (Node selectedNode : nodes) selectedNode.remove();
                        callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                        callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                        callback.addNetPlanChange();
                    } catch (Throwable ex)
                    {
                        ErrorHandling.addErrorOrException(ex, getClass());
                        ErrorHandling.showErrorDialog("Unable to remove nodes");
                    }
                }
            });
        }
    }

    static class MenuItem_AddNode extends JMenuItem
    {
        MenuItem_AddNode(GUINetworkDesign callback)
        {
            this.setText("Add node");
            this.addActionListener(e ->
            {
                NetPlan netPlan = callback.getDesign();

                int index = netPlan.getNumberOfNodes();
                String nodeName = "Node " + index;

                // Check for name validity
                while (netPlan.getNodeByName(nodeName) != null)
                    nodeName = "Node " + (index++);

                try
                {
                    Node node = netPlan.addNode(0, 0, nodeName, null);
                    callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                    callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
                    callback.getVisualizationState().pickElement(node);
                    callback.addNetPlanChange();
                    callback.runCanvasOperation(CanvasFunction.ZOOM_ALL);
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add node");

                    final Node node = netPlan.getNodeByName(nodeName);
                    if (node != null) node.remove();
                }
            });
        }
    }
}
