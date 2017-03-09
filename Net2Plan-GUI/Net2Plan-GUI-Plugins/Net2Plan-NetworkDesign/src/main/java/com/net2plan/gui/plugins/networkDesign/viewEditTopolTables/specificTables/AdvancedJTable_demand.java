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


package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.specificTables;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.networkDesign.CellRenderers;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.StringLabeller;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters.TBFToFromCarriedTraffic;
import com.net2plan.gui.plugins.networkDesign.whatIfAnalysisPane.WhatIfAnalysisPane;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
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
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
@SuppressWarnings("unchecked")
public class AdvancedJTable_demand extends AdvancedJTable_NetworkElement
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
    private static final int COLUMN_ATTRIBUTES = 14;
    private static final String netPlanViewTabName = "Demands";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index",
    		"Ingress node", "Egress node", "Coupled to link",
            "Offered traffic", "Carried traffic", "% Lost traffic",
            "Is Service Chain","Service types","Routing cycles", "Bifurcated",
            "# Routes (#BU)", "Max e2e latency (ms)", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf(
    		"Unique identifier (never repeated in the same netPlan object, never changes, long)",
    		"Index (consecutive integer starting in zero)",
    		"Ingress node",
    		"Egress node",
    		"Indicates the coupled upper layer link, if any, or empty",
    		"Offered traffic by the demand",
    		"Carried traffic by routes carrying traffic from the demand",
    		"Percentage of lost traffic from the offered",
    		"Is Service Chain","Service Types",
    		"Indicates whether there are routing cycles: loopless (no cycle in some route), open cycles (traffic reaches egress node after some cycles in some route), closed cycles (traffic does not reach the egress node in some route)",
    		"Indicates whether the demand has more than one associated route",
    		"Number of associated routes (in parenthesis, the number out of them that are designated as backup routes)", "Maximum end-to-end propagation time in miliseconds (accumulating any lower layer propagation times if any)", "Demand-specific attributes");

    private NetPlan currentTopology = null;
//    private final String[] resourceTypes = StringUtils.arrayOf("Firewall","NAT","CPU","RAM");
    /**
     * Default constructor.
     *
     * @param callback The network callback
     * @since 0.2.0
     */
    public AdvancedJTable_demand(final GUINetworkDesign callback) {
        super(createTableModel(callback), callback, NetworkElementType.DEMAND, true);
        setDefaultCellRenderers(callback);
        setSpecificCellRenderers();
        setColumnRowSortingFixedAndNonFixedTable();
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

    public String getTabName() {
        return netPlanViewTabName;
    }


    public List<Object[]> getAllData(NetPlan currentState, ArrayList<String> attributesColumns)
    {
    	final boolean isSourceRouting = currentState.getRoutingType() == RoutingType.SOURCE_ROUTING;
    	final List<Demand> rowVisibleDemands = getVisibleElementsInTable ();
        List<Object[]> allDemandData = new LinkedList<Object[]>();
        double accum_hd = 0;
        double accum_carriedTraffic = 0;
        double accum_lostTraffic = 0;
        int accum_numSCs = 0;
        int accum_numRoutes = 0; int accum_numBackupRoutes = 0;
        double accum_worstCasePropDelayMs = 0;
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
            demandData[COLUMN_OFFEREDTRAFFIC] = h_d; accum_hd += h_d;
            demandData[COLUMN_CARRIEDTRAFFIC] = demand.getCarriedTraffic(); accum_carriedTraffic += demand.getCarriedTraffic();
            demandData[COLUMN_LOSTTRAFFIC] = h_d == 0 ? 0 : 100 * lostTraffic_d / h_d; accum_lostTraffic += lostTraffic_d;
            demandData[COLUMN_ISSERVICECHAIN] = demand.isServiceChainRequest(); accum_numSCs += demand.isServiceChainRequest()? 1 : 0;
            demandData[COLUMN_TRAVERSEDRESOURCESTYPES] = isSourceRouting? joinTraversedResourcesTypes(demand) : "";
            demandData[COLUMN_ROUTINGCYCLES] = demand.getRoutingCycleType();
            demandData[COLUMN_BIFURCATED] = !isSourceRouting ? "-" : (demand.isBifurcated()) ? String.format("Yes (%d)", demand.getRoutes().size()) : "No";
            if (isSourceRouting) { accum_numRoutes += routes_thisDemand.size(); accum_numBackupRoutes += routes_thisDemand.stream().filter(e->e.isBackupRoute()).count(); }
            demandData[COLUMN_NUMROUTES] = routes_thisDemand.isEmpty() ? "none" : routes_thisDemand.size() + " (" + routes_thisDemand.stream().filter(e->e.isBackupRoute()).count() + ")";
            demandData[COLUMN_MAXE2ELATENCY] = demand.getWorstCasePropagationTimeInMs(); accum_worstCasePropDelayMs = Math.max(accum_worstCasePropDelayMs, demand.getWorstCasePropagationTimeInMs());
            demandData[COLUMN_ATTRIBUTES] = StringUtils.mapToString(demand.getAttributes());
            for(int i = netPlanViewTableHeader.length; i < netPlanViewTableHeader.length + attributesColumns.size();i++)
            {
                if(demand.getAttributes().containsKey(attributesColumns.get(i-netPlanViewTableHeader.length)))
                {
                    demandData[i] = demand.getAttribute(attributesColumns.get(i-netPlanViewTableHeader.length));
                }
            }
            allDemandData.add(demandData);
        }
        
        /* Add the aggregation row with the aggregated statistics */
        final LastRowAggregatedValue[] aggregatedData = new LastRowAggregatedValue [netPlanViewTableHeader.length + attributesColumns.size()];
        Arrays.fill(aggregatedData, new LastRowAggregatedValue());
        aggregatedData [COLUMN_OFFEREDTRAFFIC] = new LastRowAggregatedValue(accum_hd);
        aggregatedData [COLUMN_CARRIEDTRAFFIC] = new LastRowAggregatedValue(accum_carriedTraffic);
        aggregatedData [COLUMN_LOSTTRAFFIC] = new LastRowAggregatedValue(accum_hd == 0 ? 0 : 100 * accum_lostTraffic / accum_hd);
        aggregatedData [COLUMN_ISSERVICECHAIN] = new LastRowAggregatedValue(accum_numSCs);
        aggregatedData [COLUMN_NUMROUTES] = new LastRowAggregatedValue(accum_numRoutes + "(" + accum_numBackupRoutes + ")");
        aggregatedData [COLUMN_MAXE2ELATENCY] = new LastRowAggregatedValue(accum_worstCasePropDelayMs);
        allDemandData.add(aggregatedData);

        return allDemandData;
    }

    @Override
    public int getAttributesColumnIndex()
    {
        return COLUMN_ATTRIBUTES;
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
    	return rf == null? callback.getDesign().hasDemands(layer) : rf.hasDemands(layer);
    }

    private static TableModel createTableModel(final GUINetworkDesign callback)
    {
        TableModel demandTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                if (!callback.getVisualizationState().isNetPlanEditable()) return false;
                if (columnIndex >= netPlanViewTableHeader.length) return true;
                if (rowIndex == getRowCount() - 1) return false; // the last row is for the aggregated info
                if (getValueAt(rowIndex,columnIndex) == null) return false;

                return columnIndex == COLUMN_OFFEREDTRAFFIC;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column) {
                Object oldValue = getValueAt(row, column);

				/* If value doesn't change, exit from function */
                if (newValue.equals(oldValue)) return;

                NetPlan netPlan = callback.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long demandId = (Long) getValueAt(row, 0);
                final Demand demand = netPlan.getDemandFromId(demandId);

				/* Perform checks, if needed */
                try {
                    switch (column) {
                        case COLUMN_OFFEREDTRAFFIC:
                        	final double newOfferedTraffic = Double.parseDouble(newValue.toString());
                        	if (newOfferedTraffic < 0) throw new Net2PlanException ("The demand offered traffic cannot be negative");
                            if (callback.getVisualizationState().isWhatIfAnalysisActive())
                            {
                            	final WhatIfAnalysisPane whatIfPane = callback.getWhatIfAnalysisPane(); 
                            	synchronized (whatIfPane) 
                            	{
                            		whatIfPane.whatIfDemandOfferedTrafficModified(demand , newOfferedTraffic);
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
                            	demand.setOfferedTraffic(newOfferedTraffic);
                            	callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                            	callback.getVisualizationState().pickDemand(demand);
                                callback.updateVisualizationAfterPick();
                                callback.addNetPlanChange();
                            }
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex) {
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
    public void setColumnRowSortingFixedAndNonFixedTable()
    {
        setAutoCreateRowSorter(true);
        final Set<Integer> columnsWithDoubleAndThenParenthesis = Sets.newHashSet(COLUMN_INGRESSNODE , COLUMN_EGRESSNODE , COLUMN_NUMROUTES);
        DefaultRowSorter rowSorter = ((DefaultRowSorter) getRowSorter());
        for (int col = 0; col <= COLUMN_ATTRIBUTES ; col ++)
        	rowSorter.setComparator(col, new AdvancedJTable_NetworkElement.ColumnComparator(rowSorter , columnsWithDoubleAndThenParenthesis.contains(col)));
        fixedTable.setAutoCreateRowSorter(true);
        fixedTable.setRowSorter(this.getRowSorter());
        rowSorter = ((DefaultRowSorter) fixedTable.getRowSorter());
        for (int col = 0; col <= COLUMN_ATTRIBUTES ; col ++)
        	rowSorter.setComparator(col, new AdvancedJTable_NetworkElement.ColumnComparator(rowSorter , columnsWithDoubleAndThenParenthesis.contains(col)));
    }

    @Override
    public int getNumberOfDecoratorColumns() {
        return 2;
    }

    @Override
    public ArrayList<String> getAttributesColumnsHeaders()
    {
        ArrayList<String> attColumnsHeaders = new ArrayList<>();
        currentTopology = callback.getDesign();
        for(Demand demand : getVisibleElementsInTable())
            for (Map.Entry<String, String> entry : demand.getAttributes().entrySet())
                if(attColumnsHeaders.contains(entry.getKey()) == false)
                    attColumnsHeaders.add(entry.getKey());
        return attColumnsHeaders;
    }

    public void doPopup(final MouseEvent e, final int row, final Object itemId)
    {
        JPopupMenu popup = new JPopupMenu();

        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final List<Demand> demandRowsInTheTable = getVisibleElementsInTable();

        /* Add the popup menu option of the filters */
        final List<Demand> selectedDemands = (List<Demand>) (List<?>) getSelectedElements().getFirst();
        if (!selectedDemands.isEmpty())
        {
        	final JMenu submenuFilters = new JMenu ("Filters");
            final JMenuItem filterKeepElementsAffectedThisLayer = new JMenuItem("This layer: Keep elements associated to this demand traffic");
            final JMenuItem filterKeepElementsAffectedAllLayers = new JMenuItem("All layers: Keep elements associated to this demand traffic");
            submenuFilters.add(filterKeepElementsAffectedThisLayer);
            if (callback.getDesign().getNumberOfLayers() > 1) submenuFilters.add(filterKeepElementsAffectedAllLayers);
            filterKeepElementsAffectedThisLayer.addActionListener(new ActionListener()
            {
				@Override
				public void actionPerformed(ActionEvent e)
				{
					if (selectedDemands.size() > 1) throw new RuntimeException ();
					TBFToFromCarriedTraffic filter = new TBFToFromCarriedTraffic(selectedDemands.get(0), true);
					callback.getVisualizationState().updateTableRowFilter(filter);
					callback.updateVisualizationJustTables();
				}
			});
            filterKeepElementsAffectedAllLayers.addActionListener(new ActionListener()
            {
				@Override
				public void actionPerformed(ActionEvent e)
				{
					if (selectedDemands.size() > 1) throw new RuntimeException ();
					TBFToFromCarriedTraffic filter = new TBFToFromCarriedTraffic(selectedDemands.get(0), false);
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


        if (!demandRowsInTheTable.isEmpty())
        {
            if (callback.getVisualizationState().isNetPlanEditable()) {
                if (row != -1) {
                    if (popup.getSubElements().length > 0) popup.addSeparator();

                    JMenuItem removeItem = new JMenuItem("Remove " + networkElementType);

                    removeItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NetPlan netPlan = callback.getDesign();

                            try
                            {
                            	final Demand demand = netPlan.getDemandFromId((long) itemId);
                            	demand.remove();
                            	callback.getVisualizationState().resetPickedState();
                            	callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                            	callback.addNetPlanChange();
                            } catch (Throwable ex) {
                                ErrorHandling.addErrorOrException(ex, getClass());
                                ErrorHandling.showErrorDialog("Unable to remove " + networkElementType);
                            }
                        }
                    });

                    popup.add(removeItem);

                    addPopupMenuAttributeOptions(e, row, itemId, popup);
                }

                JMenuItem removeItems = new JMenuItem("Remove all " + networkElementType + "s in the table");
                removeItems.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        NetPlan netPlan = callback.getDesign();

                        try
                        {
                        	if (rf == null)
                        		netPlan.removeAllDemands();
                        	else
                        		for (Demand d : demandRowsInTheTable) d.remove();

                        	callback.getVisualizationState().resetPickedState();
                        	callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                        	callback.addNetPlanChange();
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

    public void showInCanvas(MouseEvent e, Object itemId)
    {
    	callback.getVisualizationState ().pickDemand(callback.getDesign().getDemandFromId((long)itemId));
        callback.updateVisualizationAfterPick();
    }

    private List<JComponent> getExtraAddOptions() {
        List<JComponent> options = new LinkedList<JComponent>();
        NetPlan netPlan = callback.getDesign();

        if (netPlan.getNumberOfNodes() >= 2) {
            final JMenuItem oneDemandPerNodePair = new JMenuItem("Add one demand per node pair");
            options.add(oneDemandPerNodePair);

            oneDemandPerNodePair.addActionListener(new FullMeshTrafficActionListener());
        }

        return options;
    }


    private JMenuItem getAddOption()
    {
        final NetworkElementType networkElementType = NetworkElementType.DEMAND;
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = callback.getDesign();

                try {
                    createLinkDemandGUI(networkElementType, callback);
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });

        if (callback.getDesign().getNumberOfNodes() < 2) addItem.setEnabled(false);

        return addItem;

    }

    public static void createLinkDemandGUI(final NetworkElementType networkElementType, final GUINetworkDesign callback) {
        final NetPlan netPlan = callback.getDesign();
        final JComboBox originNodeSelector = new WiderJComboBox();
        final JComboBox destinationNodeSelector = new WiderJComboBox();

        for (Node node : netPlan.getNodes()) {
            final String nodeName = node.getName();
            String nodeLabel = "Node " + node.getIndex();
            if (!nodeName.isEmpty()) nodeLabel += " (" + nodeName + ")";

            originNodeSelector.addItem(StringLabeller.of(node.getId(), nodeLabel));
            destinationNodeSelector.addItem(StringLabeller.of(node.getId(), nodeLabel));
        }

        ItemListener nodeListener = new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e) {
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

        while (true) {
            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter end nodes for the new " + networkElementType, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            try {
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
                	final Link e = netPlan.addLink(originNode , destinationNode , 0 , 0 , 200000 , null);
                	callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                	callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.LINK));
                	callback.getVisualizationState ().pickLink(e);
                    callback.updateVisualizationAfterPick();
                    callback.addNetPlanChange();

                } else
                {
                	final Demand d = netPlan.addDemand(originNode , destinationNode , 0 , null);
                	callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                	callback.getVisualizationState ().pickDemand(d);
                    callback.updateVisualizationAfterPick();
                    callback.addNetPlanChange();
                }

                break;
            } catch (Throwable ex) {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Error adding " + networkElementType);
            }
        }
    }

    private class FullMeshTrafficActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            NetPlan netPlan = callback.getDesign();
            final NetworkLayer layer = netPlan.getNetworkLayerDefault();

            if  (netPlan.hasDemands(netPlan.getNetworkLayerDefault()))
            {
                int result = JOptionPane.showConfirmDialog(null, "Remove all existing demands?", "", JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) return;
                if (result == JOptionPane.YES_OPTION) netPlan.removeAllDemands();
            }

            for (Node n1 : netPlan.getNodes()) {
                for (Node n2 : netPlan.getNodes()) {
                    if (n1.getIndex() >= n2.getIndex()) continue;
                    netPlan.addDemandBidirectional(n1,n2,0,null,layer);
                }
            }
        	callback.getVisualizationState().resetPickedState();
            callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
            callback.addNetPlanChange();
        }
    }


    private boolean isTableEmpty()
    {
        return !callback.getDesign().hasDemands();
    }

    private List<JComponent> getExtraOptions(final int row, final Object itemId) {
        List<JComponent> options = new LinkedList<JComponent>();
        final int numRows = model.getRowCount();
        final NetPlan netPlan = callback.getDesign();
        final List<Demand> tableVisibleDemands = getVisibleElementsInTable ();

        JMenuItem offeredTrafficToAll = new JMenuItem("Set offered traffic to all");
        offeredTrafficToAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double h_d;

                while (true) {
                    String str = JOptionPane.showInputDialog(null, "Offered traffic volume", "Set traffic value to all demands in the table", JOptionPane.QUESTION_MESSAGE);
                    if (str == null) return;

                    try {
                        h_d = Double.parseDouble(str);
                        if (h_d < 0) throw new RuntimeException();

                        break;
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog("Please, introduce a non-negative number", "Error setting offered traffic");
                    }
                }

                NetPlan netPlan = callback.getDesign();

                try
                {
                    for (Demand d : tableVisibleDemands) d.setOfferedTraffic(h_d);
                	callback.getVisualizationState().resetPickedState();
                    callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                    callback.addNetPlanChange();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set offered traffic to all demands in the table");
                }
            }
        });
        options.add(offeredTrafficToAll);

        JMenuItem scaleOfferedTrafficToAll = new JMenuItem("Scale offered traffic all demands in the table");
        scaleOfferedTrafficToAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double scalingFactor;

                while (true) {
                    String str = JOptionPane.showInputDialog(null, "Scaling factor to multiply to all offered traffics", "Scale offered traffic", JOptionPane.QUESTION_MESSAGE);
                    if (str == null) return;

                    try {
                        scalingFactor = Double.parseDouble(str);
                        if (scalingFactor < 0) throw new RuntimeException();

                        break;
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog("Please, introduce a non-negative number", "Error setting offered traffic");
                    }
                }

                try {
                    for (Demand d : tableVisibleDemands) d.setOfferedTraffic(d.getOfferedTraffic() * scalingFactor);
                    callback.getVisualizationState().resetPickedState();
                    callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                    callback.addNetPlanChange();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to scale demand offered traffics");
                }
            }
        });
        options.add(scaleOfferedTrafficToAll);

        JMenuItem setServiceTypes = new JMenuItem("Set traversed resource types (to one or all demands in the table)");
        setServiceTypes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                NetPlan netPlan = callback.getDesign();
                try {
                    Demand d = netPlan.getDemandFromId((Long)itemId);
                    String [] headers = StringUtils.arrayOf("Order","Type");
                    Object [][] data = {null, null};
                    DefaultTableModel model = new ClassAwareTableModelImpl(data, headers);
                    AdvancedJTable table = new AdvancedJTable(model);
                    JButton addRow = new JButton("Add new traversed resource type");
                    addRow.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Object [] newRow = {table.getRowCount(),""};
                            ((DefaultTableModel)table.getModel()).addRow(newRow);
                        }
                    });
                    JButton removeRow = new JButton("Remove selected");
                    removeRow.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            ((DefaultTableModel)table.getModel()).removeRow(table.getSelectedRow());
                            for (int t = 0; t < table.getRowCount() ; t ++)
                            	table.getModel().setValueAt(t , t , 0);
                        }
                    });
                    JButton removeAllRows = new JButton("Remove all");
                    removeAllRows.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            while(table.getRowCount() > 0)
                                ((DefaultTableModel)table.getModel()).removeRow(0);
                        }
                    });
                    List<String> oldTraversedResourceTypes = d.getServiceChainSequenceOfTraversedResourceTypes();
                    Object [][] newData = new Object[oldTraversedResourceTypes.size()][headers.length];
                    for(int i = 0; i < oldTraversedResourceTypes.size(); i++)
                    {
                        newData[i][0] = i;
                        newData[i][1] = oldTraversedResourceTypes.get(i);
                    }
                    ((DefaultTableModel)table.getModel()).setDataVector(newData, headers);
                    JPanel pane = new JPanel();
                    JPanel pane2 = new JPanel();
                    pane.setLayout(new BorderLayout());
                    pane2.setLayout(new BorderLayout());
                    pane.add(new JScrollPane(table),BorderLayout.CENTER);
                    pane2.add(addRow,BorderLayout.WEST);
                    pane2.add(removeRow,BorderLayout.EAST);
                    pane2.add(removeAllRows, BorderLayout.SOUTH);
                    pane.add(pane2,BorderLayout.SOUTH);
                    final String [] optionsArray = new String [] { "Set to selected demand" , "Set to all demands" ,  "Cancel" };
                    int result = JOptionPane.showOptionDialog(null , pane, "Set traversed resource types", JOptionPane.DEFAULT_OPTION , JOptionPane.PLAIN_MESSAGE , null , optionsArray , optionsArray[0]);
                    if ((result != 0) && (result != 1)) return;
                    final boolean setToAllDemands = (result == 1);
                    List<String> newTraversedResourcesTypes = new LinkedList<>();
                    for(int j = 0; j < table.getRowCount(); j++)
                    {
                        String travResourceType = table.getModel().getValueAt(j,1).toString();
                        newTraversedResourcesTypes.add(travResourceType);
                    }
                    if (setToAllDemands)
                    {
                    	for (Demand dd : tableVisibleDemands) if (!dd.getRoutes().isEmpty()) throw new Net2PlanException ("It is not possible to set the resource types traversed to demands with routes");
                    	for (Demand dd : tableVisibleDemands) dd.setServiceChainSequenceOfTraversedResourceTypes(newTraversedResourcesTypes);
                    }
                    else
                    {
                    	if (!d.getRoutes().isEmpty()) throw new Net2PlanException ("It is not possible to set the resource types traversed to demands with routes");
                    	d.setServiceChainSequenceOfTraversedResourceTypes(newTraversedResourcesTypes);
                    }
                    callback.getVisualizationState().resetPickedState();
                    callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                    callback.addNetPlanChange();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set traversed resource types");
                }
            }

        });
        options.add(setServiceTypes);

        if (itemId != null && netPlan.isMultilayer()) {
            final long demandId = (long) itemId;
            if (netPlan.getDemandFromId(demandId).isCoupled()) {
                JMenuItem decoupleDemandItem = new JMenuItem("Decouple demand");
                decoupleDemandItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        netPlan.getDemandFromId(demandId).decouple();
                        model.setValueAt("", row, 3);
                    	callback.getVisualizationState().resetPickedState();
                        callback.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                        callback.addNetPlanChange();
                    }
                });

                options.add(decoupleDemandItem);
            } else {
                JMenuItem createUpperLayerLinkFromDemandItem = new JMenuItem("Create upper layer link from demand");
                createUpperLayerLinkFromDemandItem.addActionListener(new ActionListener() {
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
                            int result = JOptionPane.showConfirmDialog(null, pane, "Please select the upper layer to create the link", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return;

                            try {
                                long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                                netPlan.getDemandFromId(demandId).coupleToNewLinkCreated(netPlan.getNetworkLayerFromId(layerId));
                            	callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.DEMAND , NetworkElementType.LINK));
                                callback.addNetPlanChange();
                                break;
                            } catch (Throwable ex) {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error creating upper layer link from demand");
                            }
                        }
                    }
                });

                options.add(createUpperLayerLinkFromDemandItem);

                JMenuItem coupleDemandToLink = new JMenuItem("Couple demand to upper layer link");
                coupleDemandToLink.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Collection<Long> layerIds = netPlan.getNetworkLayerIds();
                        final JComboBox layerSelector = new WiderJComboBox();
                        final JComboBox linkSelector = new WiderJComboBox();
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
                                    NetworkLayer selectedLayer = netPlan.getNetworkLayerFromId(selectedLayerId);

                                    linkSelector.removeAllItems();
                                    Collection<Link> links_thisLayer = netPlan.getLinks(selectedLayer);
                                    for (Link link : links_thisLayer) {
                                        if (link.isCoupled()) continue;

                                        String originNodeName = link.getOriginNode().getName();
                                        String destinationNodeName = link.getDestinationNode().getName();

                                        linkSelector.addItem(StringLabeller.unmodifiableOf(link.getId(), "e" + link.getIndex() + " [n" + link.getOriginNode().getIndex() + " (" + originNodeName + ") -> n" + link.getDestinationNode().getIndex() + " (" + destinationNodeName + ")]"));
                                    }
                                }

                                if (linkSelector.getItemCount() == 0) {
                                    linkSelector.setEnabled(false);
                                } else {
                                    linkSelector.setSelectedIndex(0);
                                    linkSelector.setEnabled(true);
                                }
                            }
                        });

                        layerSelector.setSelectedIndex(-1);
                        layerSelector.setSelectedIndex(0);

                        JPanel pane = new JPanel(new MigLayout("", "[][grow]", "[][]"));
                        pane.add(new JLabel("Select layer: "));
                        pane.add(layerSelector, "growx, wrap");
                        pane.add(new JLabel("Select link: "));
                        pane.add(linkSelector, "growx, wrap");

                        while (true) {
                            int result = JOptionPane.showConfirmDialog(null, pane, "Please select the upper layer link", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return;

                            try {
                                long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                                long linkId;
                                try {
                                    linkId = (long) ((StringLabeller) linkSelector.getSelectedItem()).getObject();
                                } catch (Throwable ex) {
                                    throw new RuntimeException("No link was selected");
                                }

                                netPlan.getDemandFromId(demandId).coupleToUpperLayerLink(netPlan.getLinkFromId(linkId));
                            	callback.getVisualizationState().resetPickedState();
                                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.DEMAND , NetworkElementType.LINK));
                                callback.addNetPlanChange();
                                break;
                            } catch (Throwable ex) {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error coupling upper layer link to demand");
                            }
                        }
                    }
                });

                options.add(coupleDemandToLink);
            }

            if (numRows > 1) {
                JMenuItem decoupleAllDemandsItem = null;
                JMenuItem createUpperLayerLinksFromDemandsItem = null;

                final Set<Demand> coupledDemands = tableVisibleDemands.stream().filter(d->d.isCoupled()).collect(Collectors.toSet());
                if (!coupledDemands.isEmpty()) {
                    decoupleAllDemandsItem = new JMenuItem("Decouple all demands");
                    decoupleAllDemandsItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            for (Demand d : new LinkedHashSet<Demand>(coupledDemands))
                                d.decouple();
                            int numRows = model.getRowCount();
                            for (int i = 0; i < numRows; i++) model.setValueAt("", i, 3);
                        	callback.getVisualizationState().resetPickedState();
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.DEMAND));
                            callback.addNetPlanChange();
                        }
                    });
                }

                if (coupledDemands.size() < tableVisibleDemands.size()) {
                    createUpperLayerLinksFromDemandsItem = new JMenuItem("Create upper layer links from uncoupled demands");
                    createUpperLayerLinksFromDemandsItem.addActionListener(new ActionListener() {
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
                                int result = JOptionPane.showConfirmDialog(null, pane, "Please select the upper layer to create links", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                                if (result != JOptionPane.OK_OPTION) return;

                                try {
                                    long layerId = (long) ((StringLabeller) layerSelector.getSelectedItem()).getObject();
                                    NetworkLayer layer = netPlan.getNetworkLayerFromId(layerId);
                                    for (Demand demand : tableVisibleDemands)
                                        if (!demand.isCoupled())
                                            demand.coupleToNewLinkCreated(layer);

                                    callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                                    callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.DEMAND , NetworkElementType.LINK));
                                    callback.addNetPlanChange();
                                    break;
                                } catch (Throwable ex) {
                                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error creating upper layer links");
                                }
                            }
                        }
                    });
                }

                if (!options.isEmpty() && (decoupleAllDemandsItem != null || createUpperLayerLinksFromDemandsItem != null)) {
                    options.add(new JPopupMenu.Separator());
                    if (decoupleAllDemandsItem != null) options.add(decoupleAllDemandsItem);
                    if (createUpperLayerLinksFromDemandsItem != null) options.add(createUpperLayerLinksFromDemandsItem);
                }

            }
        }

        return options;
    }

    private List<JComponent> getForcedOptions() {
        return new LinkedList<JComponent>();
    }

    private String joinTraversedResourcesTypes(Demand d)
    {
        List<String> trt = d.getServiceChainSequenceOfTraversedResourceTypes();
        String t = "";
        int counter = 0;
        for(String s : trt)
        {
            if(counter == trt.size() - 1)
                t = t + s;
            else
                t = t + s+", ";

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
            if(columnIndex == 1) return true;
            return false;
        }

        @Override
        public void setValueAt(Object value, int row, int column)
        {
            super.setValueAt(value, row, column);

        }
    }

    private List<Demand> getVisibleElementsInTable ()
    {
    	final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
    	final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
    	return rf == null? callback.getDesign().getDemands(layer) : rf.getVisibleDemands(layer);
    }

}
