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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.DefaultRowSorter;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.table.TableModel;

import com.google.common.collect.Sets;
import com.net2plan.gui.utils.CellRenderers;
import com.net2plan.gui.utils.CellRenderers.NumberCellRenderer;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.viewEditTopolTables.ITableRowFilter;
import com.net2plan.gui.utils.viewEditTopolTables.tableVisualizationFilters.TBFToFromCarriedTraffic;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.StringUtils;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

/**
 */
@SuppressWarnings("unchecked")
public class AdvancedJTable_multicastTree extends AdvancedJTable_NetworkElement
{
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_INDEX = 1;
    private static final int COLUMN_MULTICASTDEMAND = 2;
    private static final int COLUMN_INGRESSNODE = 3;
    private static final int COLUMN_EGRESSNODES = 4;
    private static final int COLUMN_OFFEREDTRAFFIC = 5;
    private static final int COLUMN_CARRIEDTRAFFIC = 6;
    private static final int COLUMN_OCCUPIEDCAPACITY = 7;
    private static final int COLUMN_SETOFLINKS = 8;
    private static final int COLUMN_NUMLINKS = 9;
    private static final int COLUMN_SETOFNODES = 10;
    private static final int COLUMN_WORSECASENUMHOPS = 11;
    private static final int COLUMN_WORSECASELENGTH = 12;
    private static final int COLUMN_WORSECASEPROPDELAY = 13;
    public static final int COLUMN_BOTTLENECKUTILIZATION = 14;
    private static final int COLUMN_ATTRIBUTES = 15;
    private static final String netPlanViewTabName = "Multicast trees";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "Multicast demand", "Ingress node", "Egress nodes",
            "Demand offered traffic", "Carried traffic", "Occupied capacity", "Set of links", "Number of links", "Set of nodes", "Worst case number of hops",
            "Worst case length (km)", "Worst case propagation delay (ms)", "Bottleneck utilization", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)", "Index (consecutive integer starting in zero)", "Multicast demand", "Ingress node", "Egress nodes", "Multicast demand offered traffic", "This multicast tree carried traffic", "Capacity occupied in the links (typically same as the carried traffic)", "Set of links in the tree", "Number of links in the tree (equal to the number of traversed nodes minus one)", "Set of traversed nodes (including ingress and egress ndoes)", "Number of hops of the longest path (in number of hops) to any egress node", "Length (km) of the longest path (in km) to any egress node", "Propagation demay (ms) of the longest path (in ms) to any egress node", "Highest utilization among all traversed links", "Multicast tree specific attributes");

    public AdvancedJTable_multicastTree(final IVisualizationCallback callback) {
        super(createTableModel(callback), callback, NetworkElementType.MULTICAST_TREE, true);
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
    	final List<MulticastTree> rowVisibleTrees = getVisibleElementsInTable ();
        List<Object[]> allTreeData = new LinkedList<Object[]>();
        for (MulticastTree tree : rowVisibleTrees)
        {
            final MulticastDemand demand = tree.getMulticastDemand();
            final double maxUtilization = tree.getLinkSet().stream().mapToDouble(e->e.getUtilization()).max().orElse(0);
            final Node ingressNode = tree.getIngressNode();
            final Set<Node> egressNodes = tree.getEgressNodes();
            final String ingressNodeName = ingressNode.getName();
            String egressNodesString = "";
            for (Node n : egressNodes) egressNodesString += n + "(" + (n.getName().isEmpty() ? "" : n.getName()) + ") ";

            Object[] treeData = new Object[netPlanViewTableHeader.length + attributesColumns.size()];
            treeData[COLUMN_ID] = tree.getId();
            treeData[COLUMN_INDEX] = tree.getIndex();
            treeData[COLUMN_MULTICASTDEMAND] = demand.getIndex();
            treeData[COLUMN_INGRESSNODE] = ingressNode.getIndex() + (ingressNodeName.isEmpty() ? "" : " (" + ingressNodeName + ")");
            treeData[COLUMN_EGRESSNODES] = egressNodesString;
            treeData[COLUMN_OFFEREDTRAFFIC] = demand.getOfferedTraffic();
            treeData[COLUMN_CARRIEDTRAFFIC] = demand.getCarriedTraffic();
            treeData[COLUMN_OCCUPIEDCAPACITY] = tree.getOccupiedLinkCapacity();
            treeData[COLUMN_SETOFLINKS] = CollectionUtils.join(NetPlan.getIndexes(tree.getLinkSet()), " ; ");
            treeData[COLUMN_NUMLINKS] = tree.getLinkSet().size();
            treeData[COLUMN_SETOFNODES] = CollectionUtils.join(NetPlan.getIndexes(tree.getNodeSet()), " ; ");
            treeData[COLUMN_WORSECASENUMHOPS] = tree.getTreeMaximumPathLengthInHops();
            treeData[COLUMN_WORSECASELENGTH] = tree.getTreeMaximumPathLengthInKm();
            treeData[COLUMN_WORSECASEPROPDELAY] = tree.getTreeMaximumPropagationDelayInMs();
            treeData[COLUMN_BOTTLENECKUTILIZATION] = maxUtilization;
            treeData[COLUMN_ATTRIBUTES] = StringUtils.mapToString(tree.getAttributes());

            for(int i = netPlanViewTableHeader.length; i < netPlanViewTableHeader.length + attributesColumns.size();i++)
            {
                if(demand.getAttributes().containsKey(attributesColumns.get(i-netPlanViewTableHeader.length)))
                {
                    treeData[i] = tree.getAttribute(attributesColumns.get(i-netPlanViewTableHeader.length));
                }
            }

            allTreeData.add(treeData);
        }
        
        /* Add the aggregation row with the aggregated statistics */
        final double aggOffered = rowVisibleTrees.stream().map(e->e.getMulticastDemand()).mapToDouble(e->e.getOfferedTraffic()).sum();
        final double aggCarried = rowVisibleTrees.stream().mapToDouble(e->e.getCarriedTraffic()).sum();
        final double aggOccupiedCap = rowVisibleTrees.stream().mapToDouble(e->e.getOccupiedLinkCapacity()).sum();
        final int aggWCNumHops = rowVisibleTrees.stream().mapToInt(e->e.getTreeMaximumPathLengthInHops()).max().orElse(0);
        final double aggWCLength = rowVisibleTrees.stream().mapToDouble(e->e.getTreeMaximumPathLengthInKm()).max().orElse(0);
        final double aggWCPropDelay = rowVisibleTrees.stream().mapToDouble(e->e.getTreeMaximumPropagationDelayInMs()).max().orElse(0);
        final LastRowAggregatedValue[] aggregatedData = new LastRowAggregatedValue [netPlanViewTableHeader.length + attributesColumns.size()];
        Arrays.fill(aggregatedData, new LastRowAggregatedValue());
        aggregatedData [COLUMN_OFFEREDTRAFFIC] = new LastRowAggregatedValue(aggOffered);
        aggregatedData [COLUMN_CARRIEDTRAFFIC] = new LastRowAggregatedValue(aggCarried);
        aggregatedData [COLUMN_OCCUPIEDCAPACITY] = new LastRowAggregatedValue(aggOccupiedCap);
        aggregatedData [COLUMN_WORSECASENUMHOPS] = new LastRowAggregatedValue(aggWCNumHops);
        aggregatedData [COLUMN_WORSECASELENGTH] = new LastRowAggregatedValue(aggWCLength);
        aggregatedData [COLUMN_WORSECASEPROPDELAY] = new LastRowAggregatedValue(aggWCPropDelay);
        allTreeData.add(aggregatedData);

        return allTreeData;
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
    	return rf == null? callback.getDesign().hasMulticastTrees(layer) : rf.hasMulticastTrees(layer);
}

    @Override
    public int getAttributesColumnIndex()
    {
        return COLUMN_ATTRIBUTES;
    }

//    public int[] getColumnsOfSpecialComparatorForSorting() {
//        return new int[]{};
//    }

    private static TableModel createTableModel(final IVisualizationCallback callback) {
        TableModel treeTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (!callback.getVisualizationState().isNetPlanEditable()) return false;
                if (columnIndex >= netPlanViewTableHeader.length) return true;
                if (getValueAt(rowIndex,columnIndex) == null) return false;

                return columnIndex == COLUMN_CARRIEDTRAFFIC || columnIndex == COLUMN_OCCUPIEDCAPACITY || columnIndex >= netPlanViewTableHeader.length;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column) {
                Object oldValue = getValueAt(row, column);

                if (newValue.equals(oldValue)) return;

                NetPlan netPlan = callback.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long treeId = (Long) getValueAt(row, 0);
                final MulticastTree tree = netPlan.getMulticastTreeFromId(treeId);

				/* Perform checks, if needed */
                try {
                    switch (column) {
                        case COLUMN_CARRIEDTRAFFIC:
                            tree.setCarriedTraffic(Double.parseDouble(newValue.toString()), tree.getOccupiedLinkCapacity());
                        	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.MULTICAST_TREE));
                        	callback.getVisualizationState ().pickMulticastTree(tree);
                            callback.updateVisualizationAfterPick();
                            callback.getUndoRedoNavigationManager().addNetPlanChange();
                            break;

                        case COLUMN_OCCUPIEDCAPACITY:
                            tree.setCarriedTraffic(tree.getCarriedTraffic(), Double.parseDouble(newValue.toString()));
                            callback.getVisualizationState ().pickMulticastTree(tree);
                        	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.MULTICAST_TREE));
                            callback.updateVisualizationAfterPick();
                            callback.getUndoRedoNavigationManager().addNetPlanChange();
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying route");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };
        return treeTableModel;
    }

    private void setDefaultCellRenderers(final IVisualizationCallback callback) {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());

        setDefaultRenderer(Boolean.class, new CellRenderers.MulticastTreeRenderer(getDefaultRenderer(Boolean.class), callback));
        setDefaultRenderer(Double.class, new CellRenderers.MulticastTreeRenderer(getDefaultRenderer(Double.class), callback));
        setDefaultRenderer(Object.class, new CellRenderers.MulticastTreeRenderer(getDefaultRenderer(Object.class), callback));
        setDefaultRenderer(Float.class, new CellRenderers.MulticastTreeRenderer(getDefaultRenderer(Float.class), callback));
        setDefaultRenderer(Long.class, new CellRenderers.MulticastTreeRenderer(getDefaultRenderer(Long.class), callback));
        setDefaultRenderer(Integer.class, new CellRenderers.MulticastTreeRenderer(getDefaultRenderer(Integer.class), callback));
        setDefaultRenderer(String.class, new CellRenderers.MulticastTreeRenderer(getDefaultRenderer(String.class), callback));
    }

    private void setSpecificCellRenderers() {
    }

    @Override
    public void setColumnRowSortingFixedAndNonFixedTable() 
    {
        setAutoCreateRowSorter(true);
        final Set<Integer> columnsWithDoubleAndThenParenthesis = Sets.newHashSet();
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
        for(MulticastTree mTree : getVisibleElementsInTable())
            for (Map.Entry<String, String> entry : mTree.getAttributes().entrySet())
                if(attColumnsHeaders.contains(entry.getKey()) == false)
                    attColumnsHeaders.add(entry.getKey());
        return attColumnsHeaders;
    }

    @Override
    public void doPopup(final MouseEvent e, final int row, final Object itemId) 
    {
        JPopupMenu popup = new JPopupMenu();
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final List<MulticastTree> rowsInTheTable = getVisibleElementsInTable();

        /* Add the popup menu option of the filters */
        final List<MulticastTree> selectedTrees = (List<MulticastTree>) (List<?>) getSelectedElements().getFirst();
        if (!selectedTrees.isEmpty()) 
        {
        	final JMenu submenuFilters = new JMenu ("Filters");
            final JMenuItem filterKeepElementsAffectedThisLayer = new JMenuItem("This layer: Keep elements associated to this tree traffic");
            final JMenuItem filterKeepElementsAffectedAllLayers = new JMenuItem("All layers: Keep elements associated to this tree traffic");
            submenuFilters.add(filterKeepElementsAffectedThisLayer);
            if (callback.getDesign().getNumberOfLayers() > 1) submenuFilters.add(filterKeepElementsAffectedAllLayers);
            filterKeepElementsAffectedThisLayer.addActionListener(new ActionListener() 
            {
				@Override
				public void actionPerformed(ActionEvent e) 
				{
					if (selectedTrees.size() > 1) throw new RuntimeException ();
					TBFToFromCarriedTraffic filter = new TBFToFromCarriedTraffic(selectedTrees.get(0), true);
					callback.getVisualizationState().updateTableRowFilter(filter);
					callback.updateVisualizationJustTables();
				}
			});
            filterKeepElementsAffectedAllLayers.addActionListener(new ActionListener() 
            {
				@Override
				public void actionPerformed(ActionEvent e) 
				{
					if (selectedTrees.size() > 1) throw new RuntimeException ();
					TBFToFromCarriedTraffic filter = new TBFToFromCarriedTraffic(selectedTrees.get(0), false);
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

        if (!rowsInTheTable.isEmpty()) {
            if (callback.getVisualizationState().isNetPlanEditable()) {
                if (row != -1) {
                    if (popup.getSubElements().length > 0) popup.addSeparator();

                    JMenuItem removeItem = new JMenuItem("Remove " + networkElementType);
                    removeItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NetPlan netPlan = callback.getDesign();
                            try {
                                netPlan.getMulticastTreeFromId((long) itemId).remove();
                                callback.getVisualizationState().resetPickedState();
                            	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.MULTICAST_TREE));
                            	callback.getUndoRedoNavigationManager().addNetPlanChange();
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

                        try {
                        	if (rf == null)
                        		netPlan.removeAllMulticastTrees();
                        	else
                        		for (MulticastTree t : rowsInTheTable) t.remove();
                            callback.getVisualizationState().resetPickedState();
                        	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.MULTICAST_TREE));
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
    public void showInCanvas(MouseEvent e, Object itemId) {
        if (getVisibleElementsInTable().isEmpty()) return;
        callback.getVisualizationState ().pickMulticastTree(callback.getDesign().getMulticastTreeFromId((long) itemId));
        callback.updateVisualizationAfterPick();
    }

    private JMenuItem getAddOption() {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    createMulticastTreeGUI(callback);
                    callback.getVisualizationState().resetPickedState();
                	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.MULTICAST_TREE));
                	callback.getUndoRedoNavigationManager().addNetPlanChange();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });

        NetPlan netPlan = callback.getDesign();
        if (!netPlan.hasMulticastDemands()) addItem.setEnabled(false);
        return addItem;
    }

    private static void createMulticastTreeGUI(final IVisualizationCallback callback) {
        final NetPlan netPlan = callback.getDesign();

        JTextField textFieldDemandIndex = new JTextField(20);
        JTextField textFieldLinkIndexes = new JTextField(20);
        JPanel pane = new JPanel();
        pane.add(new JLabel("Multicast demand index: "));
        pane.add(textFieldDemandIndex);
        pane.add(Box.createHorizontalStrut(15));
        pane.add(new JLabel("Link indexes (space separated): "));
        pane.add(textFieldLinkIndexes);

        while (true) {
            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter multicast tree demand index and link indexes", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            try {
                if (textFieldDemandIndex.getText().isEmpty())
                    throw new Exception("Please, insert the multicast demand index");
                if (textFieldLinkIndexes.getText().isEmpty()) throw new Exception("Please, insert the link indexes");
                MulticastDemand demand = netPlan.getMulticastDemand(Integer.parseInt(textFieldDemandIndex.getText()));
                Set<Link> links = new HashSet<Link>();
                for (String linkString : StringUtils.split(textFieldLinkIndexes.getText()))
                    links.add(netPlan.getLink(Integer.parseInt(linkString)));
                netPlan.addMulticastTree(demand, 0, 0, links, null);
                break;
            } catch (Throwable ex) {
                ErrorHandling.addErrorOrException(ex, AdvancedJTable_multicastTree.class);
                ErrorHandling.showErrorDialog("Error adding the multicast tree");
            }
        }
    }

    private List<JComponent> getExtraAddOptions() {
        List<JComponent> options = new LinkedList<JComponent>();
        NetPlan netPlan = callback.getDesign();

        if (netPlan.getNumberOfMulticastDemands() >= 1) {
            final JMenuItem oneTreePerDemandMinE2EHops = new JMenuItem("Add one tree per demand, minimizing end-to-end average number of traversed links");
            options.add(oneTreePerDemandMinE2EHops);
            oneTreePerDemandMinE2EHops.addActionListener(new MulticastTreeMinE2EActionListener(true, false));
            final JMenuItem oneTreePerDemandMinE2ELength = new JMenuItem("Add one tree per demand, minimizing end-to-end average traversed length in km");
            options.add(oneTreePerDemandMinE2ELength);
            oneTreePerDemandMinE2ELength.addActionListener(new MulticastTreeMinE2EActionListener(false, false));
            final JMenuItem oneTreePerDemandMinCostHops = new JMenuItem("Add one tree per demand, minimizing number of links in the tree (uses default ILP solver)");
            options.add(oneTreePerDemandMinCostHops);
            oneTreePerDemandMinCostHops.addActionListener(new MulticastTreeMinE2EActionListener(true, true));
            final JMenuItem oneTreePerDemandMinCostLength = new JMenuItem("Add one tree per demand, minimizing number of km of the links in the tree (uses default ILP solver)");
            options.add(oneTreePerDemandMinCostLength);
            oneTreePerDemandMinCostLength.addActionListener(new MulticastTreeMinE2EActionListener(true, false));
        }


        return options;
    }

    private class MulticastTreeMinE2EActionListener implements ActionListener {
        final boolean isMinHops;
        final boolean minCost;

        private MulticastTreeMinE2EActionListener(boolean isMinHops, boolean minCost) {
            this.isMinHops = isMinHops;
            this.minCost = minCost;
        }

        @Override
        public void actionPerformed(ActionEvent e) 
        {
            NetPlan netPlan = callback.getDesign();
            List<Link> links = netPlan.getLinks();
            final int E = links.size();
            List<MulticastTree> addedTrees = new LinkedList<MulticastTree>();

            // Ask for current element removal
            if (netPlan.hasMulticastTrees(netPlan.getNetworkLayerDefault()))
            {
                final int answer = JOptionPane.showConfirmDialog(null, "Remove all existing multicast trees?", "", JOptionPane.YES_NO_CANCEL_OPTION);
                if (answer == JOptionPane.OK_OPTION) netPlan.removeAllMulticastTrees(netPlan.getNetworkLayerDefault());
                if (answer == JOptionPane.CANCEL_OPTION || answer == JOptionPane.CLOSED_OPTION) return;
            }

            try {
                if (minCost) {
                    DoubleMatrix1D linkCosts = isMinHops ? DoubleFactory1D.dense.make(E, 1) : netPlan.getVectorLinkLengthInKm();
                    String solverName = Configuration.getOption("defaultILPSolver");
                    String solverLibraryName = null;
                    if (solverName.equalsIgnoreCase("glpk")) solverLibraryName = Configuration.getOption("glpkSolverLibraryName");
                    else if (solverName.equalsIgnoreCase("ipopt")) solverLibraryName = Configuration.getOption("ipoptSolverLibraryName");
                    else if (solverName.equalsIgnoreCase("cplex")) solverLibraryName = Configuration.getOption("cplexSolverLibraryName");
                    else if (solverName.equalsIgnoreCase("xpress")) solverLibraryName = Configuration.getOption("xpressSolverLicenseFileName");
                    DoubleMatrix2D Aout_ne = netPlan.getMatrixNodeLinkOutgoingIncidence();
                    DoubleMatrix2D Ain_ne = netPlan.getMatrixNodeLinkIncomingIncidence();
                    for (MulticastDemand demand : netPlan.getMulticastDemands()) {
                        Set<Link> linkSet = GraphUtils.getMinimumCostMulticastTree(links, Aout_ne, Ain_ne, linkCosts, demand.getIngressNode(), demand.getEgressNodes(), E, -1, -1.0, -1.0, solverName, solverLibraryName, 5.0);
                        addedTrees.add(netPlan.addMulticastTree(demand, demand.getOfferedTraffic(), demand.getOfferedTraffic(), linkSet, null));
                    }
                } else {
                    Map<Link, Double> linkCostMap = new HashMap<Link, Double>();
                    for (Link link : netPlan.getLinks()) linkCostMap.put(link, isMinHops ? 1 : link.getLengthInKm());
                    for (MulticastDemand demand : netPlan.getMulticastDemands()) {
                        Set<Link> linkSet = new HashSet<Link>();
                        for (Node egressNode : demand.getEgressNodes()) {
                            List<Link> seqLinks = GraphUtils.getShortestPath(netPlan.getNodes(), netPlan.getLinks(), demand.getIngressNode(), egressNode, linkCostMap);
                            if (seqLinks.isEmpty())
                                throw new Net2PlanException("The multicast tree cannot be created since the network is not connected");
                            linkSet.addAll(seqLinks);
                        }
                        addedTrees.add(netPlan.addMulticastTree(demand, demand.getOfferedTraffic(), demand.getOfferedTraffic(), linkSet, null));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                for (MulticastTree t : addedTrees) t.remove();
                ErrorHandling.showErrorDialog(ex.getMessage(), "Error adding multicast trees. No tree was created.");
            }
            callback.getVisualizationState().resetPickedState();
        	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.MULTICAST_TREE));
        	callback.getUndoRedoNavigationManager().addNetPlanChange();
        }
    }

    private List<JComponent> getExtraOptions(final int row, final Object itemId) {
        return new LinkedList<JComponent>();
    }

    private List<JComponent> getForcedOptions() {
        return new LinkedList<JComponent>();
    }
    private List<MulticastTree> getVisibleElementsInTable ()
    {
    	final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
    	final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
    	return rf == null? callback.getDesign().getMulticastTrees(layer) : rf.getVisibleMulticastTrees(layer);
    }
}
