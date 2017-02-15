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


package com.net2plan.gui.utils.viewEditTopolTables.specificTables;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultRowSorter;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.TableModel;

import org.apache.commons.collections15.BidiMap;

import com.google.common.collect.Sets;
import com.net2plan.gui.utils.CellRenderers;
import com.net2plan.gui.utils.CellRenderers.NumberCellRenderer;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.StringLabeller;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.gui.utils.topologyPane.visualizationControl.VisualizationState;
import com.net2plan.gui.utils.viewEditTopolTables.ITableRowFilter;
import com.net2plan.gui.utils.viewEditTopolTables.tableVisualizationFilters.TBFToFromCarriedTraffic;
import com.net2plan.gui.utils.whatIfAnalysisPane.WhatIfAnalysisPane;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import net.miginfocom.swing.MigLayout;

/**
 */
@SuppressWarnings("unchecked")
public class AdvancedJTable_link extends AdvancedJTable_NetworkElement
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
    public static final int COLUMN_NUMSEGMENTS = 15;
    public static final int COLUMN_NUMFORWRULES = 16;
    public static final int COLUMN_NUMTREES = 17;
    public static final int COLUMN_SRGS = 18;
    public static final int COLUMN_COUPLEDTODEMAND = 19;
    public static final int COLUMN_ATTRIBUTES = 20;
    private static final String netPlanViewTabName = "Links";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "Show/Hide", "Origin node", "Destination node", "State", "Capacity", "Carried traffic", "Occupation BU routes", "Utilization", "Is bottleneck?", "Length (km)", "Propagation speed (km/s)", "Propagation delay (ms)", "# Routes", "# Segments", "# Forwarding rules", "# Multicast trees", "SRGs", "Coupled to demand", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)", "Index (consecutive integer starting in zero)", "Indicates whether or not the link is visible in the topology canvas (if some of the end-nodes is hidden, this link will become hidden, even though the link is set as visible)", "Origin node", "Destination node", "Indicates whether the link is in up/down state", "Capacity", "Carried traffic (summing unicast and multicast)", "Capacity occupied by routes that are designated as backup routes", "Utilization (occupied capacity divided by link capacity)", "Indicates whether this link has the highest utilization in the network", "Length (km)", "Propagation speed (km/s)", "Propagation delay (ms)", "Number of routes traversing the link", "Number of protection segments traversing the link", "Number of forwarding rules for this link", "Number of multicast trees traversing the link", "SRGs including this link", "Indicates the coupled lower layer demand, if any, or empty", "Link-specific attributes");

    public AdvancedJTable_link(final IVisualizationCallback callback) {
        super(createTableModel(callback), callback, NetworkElementType.LINK, true);
        setDefaultCellRenderers(callback);
        setSpecificCellRenderers();
        setColumnRowSortingFixedAndNonFixedTable();
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
    	final boolean isSourceRouting = currentState.getRoutingType() == RoutingType.SOURCE_ROUTING;
    	final List<Link> rowVisibleLinks = getVisibleElementsInTable ();
        final double max_rho_e = currentState.getLinks().stream().mapToDouble(e->e.getUtilization()).max().orElse(0);
        List<Object[]> allLinkData = new LinkedList<Object[]>();
        for (Link link : rowVisibleLinks) 
        {
            Set<SharedRiskGroup> srgIds_thisLink = link.getSRGs();
            Set<Route> traversingRoutes = isSourceRouting ? link.getTraversingRoutes() : new LinkedHashSet<Route>();
            Set<Route> traversingBURoutes = isSourceRouting ? link.getTraversingBackupRoutes() : new LinkedHashSet<Route>();
            Set<MulticastTree> traversingMulticastTrees = link.getTraversingTrees();
            DoubleMatrix1D forwardingRules = !isSourceRouting ? currentState.getMatrixDemandBasedForwardingRules().viewColumn(link.getIndex()).copy() : DoubleFactory1D.sparse.make(currentState.getNumberOfDemands(), 0);
            int numRoutes = traversingRoutes.size();
            int numSegments = traversingBURoutes.size();
            int numForwardingRules = 0;
            for (int d = 0; d < forwardingRules.size(); d++) if (forwardingRules.get(d) != 0) numForwardingRules++;
            int numMulticastTrees = traversingMulticastTrees.size();

            String routesString = numRoutes + (numRoutes > 0 ? " (" + CollectionUtils.join(NetPlan.getIndexes(traversingRoutes), ", ") + ")" : "");
            String segmentsString = numSegments + (numSegments > 0 ? " (" + CollectionUtils.join(NetPlan.getIndexes(traversingBURoutes), ", ") + ")" : "");
            String multicastTreesString = numMulticastTrees + (numMulticastTrees > 0 ? " (" + CollectionUtils.join(NetPlan.getIndexes(traversingMulticastTrees), ", ") + ")" : "");
            StringBuilder forwardingRulesString = new StringBuilder(Integer.toString(numForwardingRules));

            Demand coupledDemand = link.getCoupledDemand();
            MulticastDemand coupledMulticastDemand = link.getCoupledMulticastDemand();

            Node originNode = link.getOriginNode();
            Node destinationNode = link.getDestinationNode();
            String originNodeName = originNode.getName();
            String destinationNodeName = destinationNode.getName();

            double rho_e = link.getOccupiedCapacity() == 0 ? 0 : link.getCapacity() == 0 ? Double.MAX_VALUE : link.getOccupiedCapacity() / link.getCapacity();
            Object[] linkData = new Object[netPlanViewTableHeader.length + attributesColumns.size()];
            linkData[COLUMN_ID] = link.getId();
            linkData[COLUMN_INDEX] = link.getIndex();
            linkData[COLUMN_SHOWHIDE] = !callback.getVisualizationState().isMandatedByTheUserToBeHiddenInCanvas(link);
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
            linkData[COLUMN_NUMROUTES] = routesString;
            linkData[COLUMN_NUMSEGMENTS] = segmentsString;
            linkData[COLUMN_NUMFORWRULES] = forwardingRulesString.toString();
            linkData[COLUMN_NUMTREES] = multicastTreesString.toString();
            linkData[COLUMN_SRGS] = srgIds_thisLink.isEmpty() ? "none" : srgIds_thisLink.size() + " (" + CollectionUtils.join(NetPlan.getIndexes(srgIds_thisLink), ", ") + ")";
            linkData[COLUMN_COUPLEDTODEMAND] = coupledDemand != null ? "d" + coupledDemand.getIndex() + " (layer " + coupledDemand.getLayer() + ")" : (coupledMulticastDemand == null ? "" : "d" + coupledMulticastDemand.getIndex() + " (layer " + coupledMulticastDemand.getLayer() + ")");
            linkData[COLUMN_ATTRIBUTES] = StringUtils.mapToString(link.getAttributes());

            for(int i = netPlanViewTableHeader.length; i < netPlanViewTableHeader.length + attributesColumns.size();i++)
            {
                if(link.getAttributes().containsKey(attributesColumns.get(i-netPlanViewTableHeader.length)))
                {
                    linkData[i] = link.getAttribute(attributesColumns.get(i-netPlanViewTableHeader.length));
                }
            }
            allLinkData.add(linkData);
        }
        
        /* Add the aggregation row with the aggregated statistics */
        final double aggCapacity = rowVisibleLinks.stream().mapToDouble(e->e.getCapacity()).sum();
        final double aggOccupiedCapacity = rowVisibleLinks.stream().mapToDouble(e->e.getOccupiedCapacity()).sum();
        final double aggCarried = rowVisibleLinks.stream().mapToDouble(e->e.getCarriedTraffic()).sum();
        final double aggLengthKm = rowVisibleLinks.stream().mapToDouble(e->e.getLengthInKm()).sum();
        final double aggPropDelayMs = rowVisibleLinks.stream().mapToDouble(e->e.getPropagationDelayInMs()).max().orElse(0);
        final int aggNumRoutes = isSourceRouting? rowVisibleLinks.stream().mapToInt(e->e.getTraversingRoutes().size()).sum() : 0;
        final int aggNumBackupRoutes = isSourceRouting? rowVisibleLinks.stream().mapToInt(e->e.getTraversingBackupRoutes().size()).sum() : 0;
        final int aggNumTrees = rowVisibleLinks.stream().mapToInt(e->e.getTraversingTrees().size()).sum();
        final int aggNumSRGs = rowVisibleLinks.stream().mapToInt(e->e.getSRGs().size()).sum();
        final int aggNumCouplings = (int) rowVisibleLinks.stream().filter(e->e.isCoupled()).count();
        final LastRowAggregatedValue[] aggregatedData = new LastRowAggregatedValue [netPlanViewTableHeader.length + attributesColumns.size()];
        Arrays.fill(aggregatedData, new LastRowAggregatedValue());
        aggregatedData [COLUMN_CAPACITY] = new LastRowAggregatedValue(aggCapacity);
        aggregatedData [COLUMN_CARRIEDTRAFFIC] = new LastRowAggregatedValue(aggCarried);
        aggregatedData [COLUMN_OCCUPIEDCAPACITY] = new LastRowAggregatedValue(aggOccupiedCapacity);
        aggregatedData [COLUMN_LENGTH] = new LastRowAggregatedValue(aggLengthKm);
        aggregatedData [COLUMN_PROPDELAYMS] = new LastRowAggregatedValue(aggPropDelayMs);
        aggregatedData [COLUMN_NUMROUTES] = new LastRowAggregatedValue(aggNumRoutes);
        aggregatedData [COLUMN_NUMSEGMENTS] = new LastRowAggregatedValue(aggNumBackupRoutes);
        aggregatedData [COLUMN_NUMTREES] = new LastRowAggregatedValue(aggNumTrees);
        aggregatedData [COLUMN_SRGS] = new LastRowAggregatedValue(aggNumSRGs);
        aggregatedData [COLUMN_COUPLEDTODEMAND] = new LastRowAggregatedValue(aggNumCouplings);
        allLinkData.add(aggregatedData);

        
        return allLinkData;
    }

    public String getTabName() {
        return netPlanViewTabName;
    }

    public String[] getTableHeaders() {
        return netPlanViewTableHeader;
    }

    public String[] getCurrentTableHeaders(){
        ArrayList<String> attColumnsHeaders = getAttributesColumnsHeaders();
        String[] headers = new String[netPlanViewTableHeader.length + attColumnsHeaders.size()];
        for(int i = 0; i < headers.length ;i++)
        {
            if(i<netPlanViewTableHeader.length)
            {
                headers[i] = netPlanViewTableHeader[i];
            }
            else{
                headers[i] = "Att: "+attColumnsHeaders.get(i - netPlanViewTableHeader.length);
            }
        }


        return headers;
    }

    public String[] getTableTips() {
        return netPlanViewTableTips;
    }

    public boolean hasElements()
    {
    	final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
    	final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
    	return rf == null? callback.getDesign().hasLinks(layer) : rf.hasLinks(layer);
    }

    @Override
    public int getAttributesColumnIndex()
    {
        return COLUMN_ATTRIBUTES;
    }

//    public int[] getColumnsOfSpecialComparatorForSorting() {
//        return new int[]{3, 4, 16, 17, 18, 19};
//    } //{ return new int [] { 3,4,6,16,17,18,19 }; }

    private static TableModel createTableModel(final IVisualizationCallback callback) {
//    	final TopologyPanel topologyPanel = callback.getTopologyPanel();
    	final VisualizationState vs = callback.getVisualizationState();
        TableModel linkTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (!vs.isNetPlanEditable()) return false;
                if (columnIndex >= netPlanViewTableHeader.length) return true;
                if (getValueAt(rowIndex,columnIndex) == null) return false;
                switch (columnIndex) {
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
                    case COLUMN_NUMSEGMENTS:
                    case COLUMN_NUMFORWRULES:
                    case COLUMN_NUMTREES:
                    case COLUMN_SRGS:
                    case COLUMN_COUPLEDTODEMAND:
                    case COLUMN_ATTRIBUTES:
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
            public void setValueAt(Object newValue, int row, int column) {
                Object oldValue = getValueAt(row, column);

				/* If value doesn't change, exit from function */
                if (newValue != null && newValue.equals(oldValue)) return;

                NetPlan netPlan = callback.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long linkId = (Long) getValueAt(row, 0);
                Link link = netPlan.getLinkFromId(linkId);

				/* Perform checks, if needed */
                try {
                    switch (column) {
                        case COLUMN_SHOWHIDE:
                            if (newValue == null) return;
                            final boolean shouldBeHiden = ! ((Boolean) newValue);
                            vs.setMandatedByTheUserToBeHiddenInCanvas(link , !shouldBeHiden);
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                            callback.getVisualizationState ().pickLink(link);
                            callback.updateVisualizationAfterPick();
                            callback.getUndoRedoNavigationManager().addNetPlanChange();
                            break;

                        case COLUMN_STATE:
                            final boolean isLinkUp = (Boolean) newValue;
                            if (vs.isWhatIfAnalysisActive())
                            {
                            	final WhatIfAnalysisPane whatIfPane = callback.getWhatIfAnalysisPane(); 
                            	synchronized (whatIfPane) 
                            	{
                            		whatIfPane.whatIfLinkNodesFailureStateChanged(null, null, isLinkUp? Sets.newHashSet(link): null, isLinkUp? null : Sets.newHashSet(link));
                            		if (whatIfPane.getLastWhatIfExecutionException() != null) throw whatIfPane.getLastWhatIfExecutionException(); 
                            		whatIfPane.wait(); // wait until the simulation ends
                            		if (whatIfPane.getLastWhatIfExecutionException() != null) throw whatIfPane.getLastWhatIfExecutionException(); 

                                    final VisualizationState vs = callback.getVisualizationState();
                            		Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer,Boolean>> res = 
                            				vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<> (callback.getDesign().getNetworkLayers()));
                            		vs.setCanvasLayerVisibilityAndOrder(callback.getDesign() , res.getFirst() , res.getSecond());
                                    callback.updateVisualizationAfterNewTopology();
								}
                            }
                            else
                            {
                            	link.setFailureState(isLinkUp);
	                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
	                            callback.getVisualizationState ().pickLink(link);
	                            callback.updateVisualizationAfterPick();
                            }
                            break;

                        case COLUMN_CAPACITY:
                            String text = newValue.toString();
                            link.setCapacity(text.equalsIgnoreCase("inf") ? Double.MAX_VALUE : Double.parseDouble(text));
                            newValue = link.getCapacity();
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                            callback.getVisualizationState ().pickLink(link);
                            callback.updateVisualizationAfterPick();
                            break;

                        case COLUMN_LENGTH:
                            link.setLengthInKm(Double.parseDouble(newValue.toString()));
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                            callback.getVisualizationState ().pickLink(link);
                            callback.updateVisualizationAfterPick();
                            break;

                        case COLUMN_PROPSPEED:
                            link.setPropagationSpeedInKmPerSecond(Double.parseDouble(newValue.toString()));
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                            callback.getVisualizationState ().pickLink(link);
                            callback.updateVisualizationAfterPick();
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex) {
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

    private void setDefaultCellRenderers(final IVisualizationCallback callback) {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new NumberCellRenderer());
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

    private void setSpecificCellRenderers() {
        getColumnModel().getColumn(convertColumnIndexToView(COLUMN_CAPACITY)).setCellEditor(new LinkCapacityCellEditor(new JTextField()));
    }

    private static class LinkCapacityCellEditor extends DefaultCellEditor {
        private static final Border black = new LineBorder(Color.black);
        private static final Border red = new LineBorder(Color.red);
        private final JTextField textField;

        public LinkCapacityCellEditor(JTextField textField) {
            super(textField);
            this.textField = textField;
            this.textField.setHorizontalAlignment(JTextField.RIGHT);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            textField.setBorder(black);
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }

        @Override
        public boolean stopCellEditing() {
            try {
                String text = textField.getText();
                if (!text.equalsIgnoreCase("inf")) {
                    double v = Double.valueOf(textField.getText());
                    if (v < 0) throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                textField.setBorder(red);
                return false;
            }

            return super.stopCellEditing();
        }
    }

    @Override
    public void setColumnRowSortingFixedAndNonFixedTable() 
    {
        setAutoCreateRowSorter(true);
        final Set<Integer> columnsWithDoubleAndThenParenthesis = Sets.newHashSet(COLUMN_ORIGINNODE , COLUMN_DESTNODE , COLUMN_NUMROUTES , COLUMN_NUMSEGMENTS , COLUMN_NUMFORWRULES , COLUMN_NUMTREES);
        DefaultRowSorter rowSorter = ((DefaultRowSorter) getRowSorter());
        for (int col = 0; col <= COLUMN_ATTRIBUTES ; col ++)
        	rowSorter.setComparator(col, new AdvancedJTable_NetworkElement.ColumnComparator(rowSorter , columnsWithDoubleAndThenParenthesis.contains(col)));
        fixedTable.setAutoCreateRowSorter(true);
        fixedTable.setRowSorter(this.getRowSorter());
        rowSorter = ((DefaultRowSorter) fixedTable.getRowSorter());
        for (int col = 0; col <= COLUMN_ATTRIBUTES ; col ++)
        	rowSorter.setComparator(col, new AdvancedJTable_NetworkElement.ColumnComparator(rowSorter , columnsWithDoubleAndThenParenthesis.contains(col)));
    }

    public int getNumFixedLeftColumnsInDecoration() {
        return 2;
    }

    @Override
    public ArrayList<String> getAttributesColumnsHeaders()
    {
        ArrayList<String> attColumnsHeaders = new ArrayList<>();
        for(Link link : getVisibleElementsInTable())
            for (Map.Entry<String, String> entry : link.getAttributes().entrySet())
                if(attColumnsHeaders.contains(entry.getKey()) == false)
                    attColumnsHeaders.add(entry.getKey());
        return attColumnsHeaders;
    }

    @Override
    public void doPopup(final MouseEvent e, final int row, final Object itemId) 
    {
        JPopupMenu popup = new JPopupMenu();
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final List<Link> linkRowsInTheTable = getVisibleElementsInTable();

        /* Add the popup menu option of the filters */
        final List<Link> selectedLinks = (List<Link>) (List<?>) getSelectedElements().getFirst();
        if (!selectedLinks.isEmpty()) 
        {
        	final JMenu submenuFilters = new JMenu ("Filters");
            final JMenuItem filterKeepElementsAffectedThisLayer = new JMenuItem("This layer: Keep elements associated to this link traffic");
            final JMenuItem filterKeepElementsAffectedAllLayers = new JMenuItem("All layers: Keep elements associated to this link traffic");
            submenuFilters.add(filterKeepElementsAffectedThisLayer);
            if (callback.getDesign().getNumberOfLayers() > 1) submenuFilters.add(filterKeepElementsAffectedAllLayers);
            filterKeepElementsAffectedThisLayer.addActionListener(new ActionListener() 
            {
				@Override
				public void actionPerformed(ActionEvent e) 
				{
					if (selectedLinks.size() > 1) throw new RuntimeException ();
					TBFToFromCarriedTraffic filter = new TBFToFromCarriedTraffic(selectedLinks.get(0), true);
					callback.getVisualizationState().updateTableRowFilter(filter);
					callback.updateVisualizationJustTables();
				}
			});
            filterKeepElementsAffectedAllLayers.addActionListener(new ActionListener() 
            {
				@Override
				public void actionPerformed(ActionEvent e) 
				{
					if (selectedLinks.size() > 1) throw new RuntimeException ();
					TBFToFromCarriedTraffic filter = new TBFToFromCarriedTraffic(selectedLinks.get(0), false);
					callback.getVisualizationState().updateTableRowFilter(filter);
					callback.updateVisualizationJustTables();
				}
			});
            popup.add(submenuFilters);
            popup.addSeparator();
        }

        
        if (callback.getVisualizationState().isNetPlanEditable()) {
            popup.add(getAddOption());
            for (JComponent item : getExtraAddOptions())
                popup.add(item);
        }

        if (!linkRowsInTheTable.isEmpty()) {
            if (callback.getVisualizationState().isNetPlanEditable()) {
                if (row != -1) {
                    if (popup.getSubElements().length > 0) popup.addSeparator();

                    JMenuItem removeItem = new JMenuItem("Remove " + networkElementType);
                    removeItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NetPlan netPlan = callback.getDesign();

                            try {
                                Link link = netPlan.getLinkFromId((long) itemId);
                                link.remove();
                                callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                                callback.getUndoRedoNavigationManager().addNetPlanChange();
                            } catch (Throwable ex) {
                                ErrorHandling.addErrorOrException(ex, getClass());
                                ErrorHandling.showErrorDialog("Unable to remove " + networkElementType);
                            }
                        }
                    });

                    popup.add(removeItem);
                }

                addPopupMenuAttributeOptions(e, row, itemId, popup);

                JMenuItem removeItems = new JMenuItem("Remove all " + networkElementType + "s in table");

                removeItems.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        NetPlan netPlan = callback.getDesign();

                        try {
                        	if (rf == null)
                        		netPlan.removeAllLinks();
                        	else
                        		for (Link ee : linkRowsInTheTable) ee.remove();
                            callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                            callback.getUndoRedoNavigationManager().addNetPlanChange();
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
    public void showInCanvas(MouseEvent e, Object itemId) 
    {
        if (getVisibleElementsInTable().isEmpty()) return;
        final Link link = callback.getDesign().getLinkFromId((long) itemId);
        callback.getVisualizationState ().pickLink(link);
        callback.updateVisualizationAfterPick();
    }

    private JMenuItem getAddOption() {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        ;
        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    AdvancedJTable_demand.createLinkDemandGUI(networkElementType , callback);
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });

        NetPlan netPlan = callback.getDesign();
        int N = netPlan.getNumberOfNodes();
        if (N < 2) addItem.setEnabled(false);

        return addItem;
    }

    private List<JComponent> getExtraAddOptions() {
        List<JComponent> options = new LinkedList<JComponent>();
        NetPlan netPlan = callback.getDesign();

        if (netPlan.getNumberOfNodes() >= 2) {
            final JMenuItem fullMeshEuclidean = new JMenuItem("Generate full-mesh (link length as Euclidean distance)");
            final JMenuItem fullMeshHaversine = new JMenuItem("Generate full-mesh (link length as Haversine distance)");
            options.add(fullMeshEuclidean);
            options.add(fullMeshHaversine);

            fullMeshEuclidean.addActionListener(new FullMeshTopologyActionListener(true));
            fullMeshHaversine.addActionListener(new FullMeshTopologyActionListener(false));
        }

        return options;
    }

    private List<JComponent> getExtraOptions(final int row, final Object itemId) 
    {
        List<JComponent> options = new LinkedList<JComponent>();

        final List<Link> rowVisibleLinks = getVisibleElementsInTable();
        final NetPlan netPlan = callback.getDesign();

        if (itemId != null) {
            final long linkId = (long) itemId;

            JMenuItem lengthToEuclidean_thisLink = new JMenuItem("Set link length to node-pair Euclidean distance");
            lengthToEuclidean_thisLink.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Link link = netPlan.getLinkFromId(linkId);
                    Node originNode = link.getOriginNode();
                    Node destinationNode = link.getDestinationNode();
                    double euclideanDistance = netPlan.getNodePairEuclideanDistance(originNode, destinationNode);
                    link.setLengthInKm(euclideanDistance);
                	callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.LINK));
                	callback.getUndoRedoNavigationManager().addNetPlanChange();
                }
            });

            options.add(lengthToEuclidean_thisLink);

            JMenuItem lengthToHaversine_allNodes = new JMenuItem("Set link length to node-pair Haversine distance (longitude-latitude) in km");
            lengthToHaversine_allNodes.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Link link = netPlan.getLinkFromId(linkId);
                    Node originNode = link.getOriginNode();
                    Node destinationNode = link.getDestinationNode();
                    double haversineDistanceInKm = netPlan.getNodePairHaversineDistanceInKm(originNode, destinationNode);
                    link.setLengthInKm(haversineDistanceInKm);
                	callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.LINK));
                	callback.getUndoRedoNavigationManager().addNetPlanChange();
                }
            });

            options.add(lengthToHaversine_allNodes);

            JMenuItem scaleLinkLength_thisLink = new JMenuItem("Scale link length");
            scaleLinkLength_thisLink.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double scaleFactor;

                    while (true) {
                        String str = JOptionPane.showInputDialog(null, "(Multiplicative) Scale factor", "Scale link length", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try {
                            scaleFactor = Double.parseDouble(str);
                            if (scaleFactor < 0) throw new RuntimeException();

                            break;
                        } catch (Throwable ex) {
                            ErrorHandling.showErrorDialog("Non-valid scale value. Please, introduce a non-negative number", "Error setting scale factor");
                        }
                    }

                    netPlan.getLinkFromId(linkId).setLengthInKm(netPlan.getLinkFromId(linkId).getLengthInKm() * scaleFactor);
                	callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.LINK));
                	callback.getUndoRedoNavigationManager().addNetPlanChange();
                }
            });

            options.add(scaleLinkLength_thisLink);

            if (netPlan.isMultilayer()) {
                Link link = netPlan.getLinkFromId(linkId);
                if (link.getCoupledDemand() != null) {
                    JMenuItem decoupleLinkItem = new JMenuItem("Decouple link (if coupled to unicast demand)");
                    decoupleLinkItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            netPlan.getLinkFromId(linkId).getCoupledDemand().decouple();
                            model.setValueAt("", row, 20);
                            callback.getVisualizationState().resetPickedState();
                        	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK , NetworkElementType.DEMAND));
                        	callback.getUndoRedoNavigationManager().addNetPlanChange();
                        }
                    });

                    options.add(decoupleLinkItem);
                } else {
                    JMenuItem createLowerLayerDemandFromLinkItem = new JMenuItem("Create lower layer demand from link");
                    createLowerLayerDemandFromLinkItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Collection<Long> layerIds = netPlan.getNetworkLayerIds();
                            final JComboBox layerSelector = new WiderJComboBox();
                            for (long layerId : layerIds) {
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

                            while (true) {
                                int result = JOptionPane.showConfirmDialog(null, pane, "Please select the lower layer to create the demand", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                                if (result != JOptionPane.OK_OPTION) return;

                                try {
                                    long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                                    Link link = netPlan.getLinkFromId(linkId);
                                    netPlan.addDemand(link.getOriginNode(), link.getDestinationNode(), link.getCapacity(), link.getAttributes(), netPlan.getNetworkLayerFromId(layerId));
                                    callback.getVisualizationState().resetPickedState();
                                	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.DEMAND));
                                	callback.getUndoRedoNavigationManager().addNetPlanChange();
                                    break;
                                } catch (Throwable ex) {
                                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error creating lower layer demand from link");
                                }
                            }
                        }
                    });

                    options.add(createLowerLayerDemandFromLinkItem);

                    JMenuItem coupleLinkToDemand = new JMenuItem("Couple link to lower layer demand");
                    coupleLinkToDemand.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Collection<Long> layerIds = netPlan.getNetworkLayerIds();
                            final JComboBox layerSelector = new WiderJComboBox();
                            final JComboBox demandSelector = new WiderJComboBox();
                            for (long layerId : layerIds) {
                                if (layerId == netPlan.getNetworkLayerDefault().getId()) continue;

                                final String layerName = netPlan.getNetworkLayerFromId(layerId).getName();
                                String layerLabel = "Layer " + layerId;
                                if (!layerName.isEmpty()) layerLabel += " (" + layerName + ")";

                                layerSelector.addItem(StringLabeller.of(layerId, layerLabel));
                            }

                            layerSelector.addItemListener(new ItemListener() {
                                @Override
                                public void itemStateChanged(ItemEvent e) {
                                    if (layerSelector.getSelectedIndex() >= 0) {
                                        long selectedLayerId = (Long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();

                                        demandSelector.removeAllItems();
                                        for (Demand demand : netPlan.getDemands(netPlan.getNetworkLayerFromId(selectedLayerId))) {
                                            if (demand.isCoupled()) continue;

                                            long ingressNodeId = demand.getIngressNode().getId();
                                            long egressNodeId = demand.getEgressNode().getId();
                                            String ingressNodeName = demand.getIngressNode().getName();
                                            String egressNodeName = demand.getEgressNode().getName();

                                            demandSelector.addItem(StringLabeller.unmodifiableOf(demand.getId(), "d" + demand.getId() + " [n" + ingressNodeId + " (" + ingressNodeName + ") -> n" + egressNodeId + " (" + egressNodeName + ")]"));
                                        }
                                    }

                                    if (demandSelector.getItemCount() == 0) {
                                        demandSelector.setEnabled(false);
                                    } else {
                                        demandSelector.setSelectedIndex(0);
                                        demandSelector.setEnabled(true);
                                    }
                                }
                            });

                            layerSelector.setSelectedIndex(-1);
                            layerSelector.setSelectedIndex(0);

                            JPanel pane = new JPanel(new MigLayout("", "[][grow]", "[][]"));
                            pane.add(new JLabel("Select layer: "));
                            pane.add(layerSelector, "growx, wrap");
                            pane.add(new JLabel("Select demand: "));
                            pane.add(demandSelector, "growx, wrap");

                            while (true) {
                                int result = JOptionPane.showConfirmDialog(null, pane, "Please select the lower layer demand", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                                if (result != JOptionPane.OK_OPTION) return;

                                try {
                                    long demandId;
                                    try {
                                        demandId = (long) ((StringLabeller) demandSelector.getSelectedItem()).getObject();
                                    } catch (Throwable ex) {
                                        throw new RuntimeException("No demand was selected");
                                    }

                                    netPlan.getDemandFromId(demandId).coupleToUpperLayerLink(netPlan.getLinkFromId(linkId));
                                    callback.getVisualizationState().resetPickedState();
                                	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK , NetworkElementType.DEMAND));
                                	callback.getUndoRedoNavigationManager().addNetPlanChange();
                                    break;
                                } catch (Throwable ex) {
                                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error coupling lower layer demand to link");
                                }
                            }
                        }
                    });

                    options.add(coupleLinkToDemand);
                }
            }
        }

        if (rowVisibleLinks.size() > 1) {
            if (!options.isEmpty()) options.add(new JPopupMenu.Separator());

            JMenuItem caFixValue = new JMenuItem("Set capacity to all");
            caFixValue.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) 
                {
                    double u_e;

                    while (true) 
                    {
                        String str = JOptionPane.showInputDialog(null, "Capacity value", "Set capacity to all table links", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try 
                        {
                            u_e = Double.parseDouble(str);
                            if (u_e < 0) throw new NumberFormatException();

                            break;
                        } catch (NumberFormatException ex) {
                            ErrorHandling.showErrorDialog("Non-valid capacity value. Please, introduce a non-negative number", "Error setting capacity value");
                        }
                    }

                    try {
                        for (Link link : rowVisibleLinks) link.setCapacity(u_e);
                    	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                    	callback.getUndoRedoNavigationManager().addNetPlanChange();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set capacity to all links");
                    }
                }
            });

            options.add(caFixValue);

            JMenuItem caFixValueUtilization = new JMenuItem("Set capacity to match a given utilization");
            caFixValueUtilization.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double utilization;

                    while (true) {
                        String str = JOptionPane.showInputDialog(null, "Link utilization value", "Set capacity to all table links to match a given utilization", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try {
                            utilization = Double.parseDouble(str);
                            if (utilization <= 0) throw new NumberFormatException();

                            break;
                        } catch (NumberFormatException ex) {
                            ErrorHandling.showErrorDialog("Non-valid link utilization value. Please, introduce a strictly positive number", "Error setting link utilization value");
                        }
                    }

                    try {
                        for (Link link : rowVisibleLinks)
                            link.setCapacity(link.getOccupiedCapacity() / utilization);
                    	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                    	callback.getUndoRedoNavigationManager().addNetPlanChange();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set capacity to all links according to a given link utilization");
                    }
                }
            });

            options.add(caFixValueUtilization);

            JMenuItem lengthToAll = new JMenuItem("Set link length to all");
            lengthToAll.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double l_e;

                    while (true) {
                        String str = JOptionPane.showInputDialog(null, "Link length value (in km)", "Set link length to all table links", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try {
                            l_e = Double.parseDouble(str);
                            if (l_e < 0) throw new RuntimeException();

                            break;
                        } catch (Throwable ex) {
                            ErrorHandling.showErrorDialog("Non-valid link length value. Please, introduce a non-negative number", "Error setting link length");
                        }
                    }

                    NetPlan netPlan = callback.getDesign();

                    try {
                        for (Link link : rowVisibleLinks) link.setLengthInKm(l_e);
                    	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                    	callback.getUndoRedoNavigationManager().addNetPlanChange();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set link length to all links");
                    }
                }
            });

            options.add(lengthToAll);

            JMenuItem lengthToEuclidean_allLinks = new JMenuItem("Set all table link lengths to node-pair Euclidean distance");
            lengthToEuclidean_allLinks.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) 
                {
                    try 
                    {
                        for (Link link : rowVisibleLinks) 
                            link.setLengthInKm(netPlan.getNodePairEuclideanDistance(link.getOriginNode(), link.getDestinationNode()));
                    	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                    	callback.getUndoRedoNavigationManager().addNetPlanChange();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set link length value to all links");
                    }
                }
            });

            options.add(lengthToEuclidean_allLinks);

            JMenuItem lengthToHaversine_allLinks = new JMenuItem("Set all table link lengths to node-pair Haversine distance (longitude-latitude) in km");
            lengthToHaversine_allLinks.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NetPlan netPlan = callback.getDesign();

                    try {
                        for (Link link : rowVisibleLinks) {
                            link.setLengthInKm(netPlan.getNodePairHaversineDistanceInKm(link.getOriginNode(), link.getDestinationNode()));
                        }
                    	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                    	callback.getUndoRedoNavigationManager().addNetPlanChange();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set link length value to all links");
                    }
                }
            });

            options.add(lengthToHaversine_allLinks);

            JMenuItem scaleLinkLength_allLinks = new JMenuItem("Scale all table link lengths");
            scaleLinkLength_allLinks.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double scaleFactor;

                    while (true) {
                        String str = JOptionPane.showInputDialog(null, "(Multiplicative) Scale factor", "Scale (all) link length", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try {
                            scaleFactor = Double.parseDouble(str);
                            if (scaleFactor < 0) throw new RuntimeException();

                            break;
                        } catch (Throwable ex) {
                            ErrorHandling.showErrorDialog("Non-valid scale value. Please, introduce a non-negative number", "Error setting scale factor");
                        }
                    }

                    NetPlan netPlan = callback.getDesign();

                    try {
                        for (Link link : rowVisibleLinks)
                            link.setLengthInKm(link.getLengthInKm() * scaleFactor);
                    	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
                    	callback.getUndoRedoNavigationManager().addNetPlanChange();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to scale link length");
                    }
                }
            });

            options.add(scaleLinkLength_allLinks);

            if (netPlan.isMultilayer()) {
                final Set<Link> coupledLinks = rowVisibleLinks.stream().filter(e->e.isCoupled()).collect(Collectors.toSet());
                if (!coupledLinks.isEmpty()) {
                    JMenuItem decoupleAllLinksItem = new JMenuItem("Decouple all table links");
                    decoupleAllLinksItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) 
                        {
                            for (Link link : coupledLinks) 
                            	if (link.getCoupledDemand() == null) 
                            		link.getCoupledMulticastDemand().decouple();
                            	else
                            		link.getCoupledDemand().decouple();
                            int numRows = model.getRowCount();
                            for (int i = 0; i < numRows; i++) model.setValueAt("", i, 20);
                            callback.getVisualizationState().resetPickedState();
                        	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK , NetworkElementType.DEMAND));
                        	callback.getUndoRedoNavigationManager().addNetPlanChange();
                        }
                    });

                    options.add(decoupleAllLinksItem);
                }

                if (coupledLinks.size() < rowVisibleLinks.size()) {
                    JMenuItem createLowerLayerDemandsFromLinksItem = new JMenuItem("Create lower layer unicast demands from uncoupled links");
                    createLowerLayerDemandsFromLinksItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            final JComboBox layerSelector = new WiderJComboBox();
                            for (NetworkLayer layer : netPlan.getNetworkLayers()) {
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

                            while (true) {
                                int result = JOptionPane.showConfirmDialog(null, pane, "Please select the lower layer to create demands", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                                if (result != JOptionPane.OK_OPTION) return;

                                try {
                                    long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                                    NetworkLayer layer = netPlan.getNetworkLayerFromId(layerId);
                                    for (Link link : rowVisibleLinks)
                                        if (!link.isCoupled())
                                            link.coupleToNewDemandCreated(layer);
                                    callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                                	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK , NetworkElementType.DEMAND));
                                	callback.getUndoRedoNavigationManager().addNetPlanChange();
                                    break;
                                } catch (Throwable ex) {
                                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error creating lower layer demands");
                                }
                            }
                        }
                    });

                    options.add(createLowerLayerDemandsFromLinksItem);
                }
            }
        }
        return options;
    }

    private List<JComponent> getForcedOptions() 
    {
        List<JComponent> options = new LinkedList<JComponent>();
        final int numRows = model.getRowCount();
        if (numRows > 1) 
        {
            JMenuItem showAllLinks = new JMenuItem("Show all links");
            showAllLinks.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int numRows = model.getRowCount();
                    for (int row = 0; row < numRows; row++)
                        if (model.getValueAt(row, COLUMN_SHOWHIDE) != null)
                            model.setValueAt(true, row, COLUMN_SHOWHIDE);
                }
            });

            options.add(showAllLinks);

            JMenuItem hideAllLinks = new JMenuItem("Hide all links");
            hideAllLinks.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int numRows = model.getRowCount();
                    for (int row = 0; row < numRows; row++)
                        if (model.getValueAt(row, COLUMN_SHOWHIDE) != null)
                            model.setValueAt(false, row, COLUMN_SHOWHIDE);
                }
            });

            options.add(hideAllLinks);
        }

        return options;
    }

    private class FullMeshTopologyActionListener implements ActionListener 
    {
        private final boolean euclidean;

        public FullMeshTopologyActionListener(boolean euclidean) {
            this.euclidean = euclidean;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NetPlan netPlan = callback.getDesign();

            // Ask for current element removal
            if (netPlan.hasLinks(netPlan.getNetworkLayerDefault()))
            {
                final int answer = JOptionPane.showConfirmDialog(null, "Remove all existing links?", "", JOptionPane.YES_NO_CANCEL_OPTION);
                if (answer == JOptionPane.CANCEL_OPTION || answer == JOptionPane.CLOSED_OPTION) return;
                if (answer == JOptionPane.OK_OPTION) netPlan.removeAllLinks(netPlan.getNetworkLayerDefault());
            }

            for (long nodeId_1 : netPlan.getNodeIds()) {
                for (long nodeId_2 : netPlan.getNodeIds()) {
                    if (nodeId_1 >= nodeId_2) continue;
                    Node n1 = netPlan.getNodeFromId(nodeId_1);
                    Node n2 = netPlan.getNodeFromId(nodeId_2);

                    Pair<Link, Link> out = netPlan.addLinkBidirectional(n1, n2, 0, euclidean ? netPlan.getNodePairEuclideanDistance(n1, n2) : netPlan.getNodePairHaversineDistanceInKm(n1, n2), 200000, null);
                }
            }
            callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
        	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
        	callback.getUndoRedoNavigationManager().addNetPlanChange();
        }
    }


    private List<Link> getVisibleElementsInTable ()
    {
    	final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
    	final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
    	return rf == null? callback.getDesign().getLinks(layer) : rf.getVisibleLinks(layer);
    }
}
