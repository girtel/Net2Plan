

package com.net2plan.gui.utils.viewEditTopolTables.specificTables;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultRowSorter;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

<<<<<<< HEAD
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
=======
import com.google.common.collect.Sets;
>>>>>>> develop
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ButtonColumn;
import com.net2plan.gui.utils.CellRenderers;
import com.net2plan.gui.utils.CellRenderers.NumberCellRenderer;
import com.net2plan.gui.utils.CellRenderers.UnfocusableCellRenderer;
import com.net2plan.gui.utils.viewEditTopolTables.ITableRowFilter;
import com.net2plan.gui.utils.viewEditTopolTables.tableVisualizationFilters.TBFToFromCarriedTraffic;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.StringLabeller;
import com.net2plan.gui.utils.SwingUtils;
import com.net2plan.gui.utils.WiderJComboBox;
<<<<<<< HEAD
import com.net2plan.interfaces.networkDesign.*;
=======
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
>>>>>>> develop
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.StringUtils;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import net.miginfocom.swing.MigLayout;

/**
 */
@SuppressWarnings("unchecked")
public class AdvancedJTable_route extends AdvancedJTable_NetworkElement
{
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_INDEX = 1;
    private static final int COLUMN_DEMAND = 2;
    private static final int COLUMN_INGRESSNODE = 3;
    private static final int COLUMN_EGRESSNODE = 4;
    private static final int COLUMN_DEMANDOFFEREDTRAFFIC = 5;
    private static final int COLUMN_CARRIEDTRAFFIC = 6;
    private static final int COLUMN_OCCUPIEDCAPACITY = 7;
    private static final int COLUMN_SEQUENCEOFLINKSANDRESOURCES = 8;
    private static final int COLUMN_SEQUENCEOFNODES = 9;
    private static final int COLUMN_NUMHOPS = 10;
    private static final int COLUMN_LENGTH = 11;
    private static final int COLUMN_PROPDELAY = 12;
    public static final int COLUMN_BOTTLENECKUTILIZATION = 13;
    private static final int COLUMN_ISBACKUP = 14;
    private static final int COLUMN_HASBACKUPROUTES = 15;
    private static final int COLUMN_ATTRIBUTES = 16;
    private static final String netPlanViewTabName = "Routes";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "Demand", "Ingress node", "Egress node",
            "Demand offered traffic", "Carried traffic", "Occupied capacity", "Sequence of links/resources", "Sequence of nodes", "Number of hops", "Length (km)",
            "Propagation delay (ms)", "Bottleneck utilization", "Is backup?", "Has backup?" , "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf(
    		"Unique identifier (never repeated in the same netPlan object, never changes, long)", "Index (consecutive integer starting in zero)", "Demand", "Ingress node", "Egress node", "Demand offered traffic", 
    		"Carried traffic", "Occupied capacity", "Sequence of indexes of the links (Lxx) and resources (Rxx) traversed", 
    		"Sequence of nodes traversed, in parenthesis the indexes of the resources in each node", 
    		"Number of hops", "Total route length", "Propagation delay (ms)", "Highest utilization among all traversed links", 
    		"Indicates if this route is designated as a backup route, of other route for the same demand. If so, shows the index of the primary route that this route is backing up", 
    		"Indicates if this route has backup routes. If so, their indexes are shown in parenthesis" , 
    		"Route-specific attributes");

    public AdvancedJTable_route(final IVisualizationCallback callback) {
        super(createTableModel(callback), callback, NetworkElementType.ROUTE, true);
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


    public List<Object[]> getAllData(NetPlan currentState,ArrayList<String> attributesColumns) 
    {
        List<Object[]> allRouteData = new LinkedList<Object[]>();
    	final List<Route> rowVisibleRoutes = getVisibleElementsInTable ();
        for (Route route : rowVisibleRoutes) {
            final Demand demand = route.getDemand();
            final double maxUtilization = route.getSeqLinks().stream().mapToDouble(e->e.getUtilization()).max().orElse(0);
            final Node ingressNode = route.getDemand().getIngressNode();
            final Node egressNode = route.getDemand().getEgressNode();
            final String ingressNodeName = ingressNode.getName();
            final String egressNodeName = egressNode.getName();

            final Object[] routeData = new Object[netPlanViewTableHeader.length + attributesColumns.size()];
            
            routeData[COLUMN_ID] = route.getId();
            routeData[COLUMN_INDEX] = route.getIndex();
            routeData[COLUMN_DEMAND] = demand.getIndex();
            routeData[COLUMN_INGRESSNODE] = ingressNode.getIndex() + (ingressNodeName.isEmpty() ? "" : " (" + ingressNodeName + ")");
            routeData[COLUMN_EGRESSNODE] = egressNode.getIndex() + (egressNodeName.isEmpty() ? "" : " (" + egressNodeName + ")");
            routeData[COLUMN_DEMANDOFFEREDTRAFFIC] = demand.getOfferedTraffic();
            routeData[COLUMN_CARRIEDTRAFFIC] = route.getCarriedTraffic();
            routeData[COLUMN_OCCUPIEDCAPACITY] = getSequenceOccupiedCapacities (route);
            routeData[COLUMN_SEQUENCEOFLINKSANDRESOURCES] = getSequenceLinkResourceIndexes (route);
            routeData[COLUMN_SEQUENCEOFNODES] = getSequenceNodeIndexesWithResourceInfo (route);
            routeData[COLUMN_NUMHOPS] = route.getNumberOfHops();
            routeData[COLUMN_LENGTH] = route.getLengthInKm();
            routeData[COLUMN_PROPDELAY] = route.getPropagationDelayInMiliseconds();
            routeData[COLUMN_BOTTLENECKUTILIZATION] = maxUtilization;
            routeData[COLUMN_ISBACKUP] = (route.isBackupRoute()? "yes (" + (CollectionUtils.join(NetPlan.getIndexes(route.getRoutesIAmBackup()) , ", ") ) +")" : "no");
            routeData[COLUMN_HASBACKUPROUTES] = (route.hasBackupRoutes()? "yes (" + (CollectionUtils.join(NetPlan.getIndexes(route.getBackupRoutes()) , ", ") ) +")" : "no");
            routeData[COLUMN_ATTRIBUTES] = StringUtils.mapToString(route.getAttributes());

            for(int i = netPlanViewTableHeader.length; i < netPlanViewTableHeader.length + attributesColumns.size();i++)
            {
                if(route.getAttributes().containsKey(attributesColumns.get(i-netPlanViewTableHeader.length)))
                {
                    routeData[i] = route.getAttribute(attributesColumns.get(i-netPlanViewTableHeader.length));
                }
            }

            allRouteData.add(routeData);
        }

        /* Add the aggregation row with the aggregated statistics */
        final double aggDemandOffered = rowVisibleRoutes.stream().mapToDouble(e->e.getDemand().getOfferedTraffic()).sum();
        final double aggCarried = rowVisibleRoutes.stream().mapToDouble(e->e.getCarriedTraffic()).sum();
        final double aggLinkOccupied = rowVisibleRoutes.stream().mapToDouble(e->e.isDown()? 0 : e.getSeqOccupiedCapacitiesIfNotFailing().stream().mapToDouble(ee->ee).sum()).sum();
        final int aggMaxNumHops = rowVisibleRoutes.stream().mapToInt(e->e.getNumberOfHops()).sum();
        final double aggMaxLength = rowVisibleRoutes.stream().mapToDouble(e->e.getLengthInKm()).sum();
        final double aggMaxPropDelay = rowVisibleRoutes.stream().mapToDouble(e->e.getPropagationDelayInMiliseconds()).sum();
        final int aggIsBackup = (int) rowVisibleRoutes.stream().filter(e->e.isBackupRoute()).count();
        final int aggHasBackup = (int) rowVisibleRoutes.stream().filter(e->e.hasBackupRoutes()).count();
        final LastRowAggregatedValue[] aggregatedData = new LastRowAggregatedValue [netPlanViewTableHeader.length + attributesColumns.size()];
        Arrays.fill(aggregatedData, new LastRowAggregatedValue());
        aggregatedData [COLUMN_DEMANDOFFEREDTRAFFIC] = new LastRowAggregatedValue(aggDemandOffered);
        aggregatedData [COLUMN_CARRIEDTRAFFIC] = new LastRowAggregatedValue(aggCarried);
        aggregatedData [COLUMN_OCCUPIEDCAPACITY] = new LastRowAggregatedValue(aggLinkOccupied);
        aggregatedData [COLUMN_NUMHOPS] = new LastRowAggregatedValue(aggMaxNumHops);
        aggregatedData [COLUMN_LENGTH] = new LastRowAggregatedValue(aggMaxLength);
        aggregatedData [COLUMN_PROPDELAY] = new LastRowAggregatedValue(aggMaxPropDelay);
        aggregatedData [COLUMN_ISBACKUP] = new LastRowAggregatedValue(aggIsBackup);
        aggregatedData [COLUMN_HASBACKUPROUTES] = new LastRowAggregatedValue(aggHasBackup);
        allRouteData.add(aggregatedData);
        
        return allRouteData;
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
    	return rf == null? callback.getDesign().hasRoutes(layer) : rf.hasRoutes(layer);
    }

    @Override
    public int getAttributesColumnIndex()
    {
        return COLUMN_ATTRIBUTES;
    }

//    public int[] getColumnsOfSpecialComparatorForSorting() {
//        return new int[]{};
//    }

    private static TableModel createTableModel(final IVisualizationCallback callback)
    {
        TableModel routeTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (!callback.getVisualizationState().isNetPlanEditable()) return false;
                if (columnIndex >= netPlanViewTableHeader.length) return true;
                if (getValueAt(rowIndex,columnIndex) == null) return false;

                return columnIndex == COLUMN_CARRIEDTRAFFIC || columnIndex == COLUMN_OCCUPIEDCAPACITY
                        || columnIndex >= netPlanViewTableHeader.length;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column) {
                Object oldValue = getValueAt(row, column);

                if (newValue.equals(oldValue)) return;

                NetPlan netPlan = callback.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long routeId = (Long) getValueAt(row, 0);
                final Route route = netPlan.getRouteFromId(routeId);
                /* Perform checks, if needed */
                try {
                    switch (column) {
                        case COLUMN_CARRIEDTRAFFIC:
                            route.setCarriedTraffic(Double.parseDouble(newValue.toString()), route.getOccupiedCapacity());
                        	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.ROUTE));
                        	callback.getVisualizationState ().pickRoute(route);
                            callback.updateVisualizationAfterPick();
                            callback.getUndoRedoNavigationManager().addNetPlanChange();
                            break;

                        case COLUMN_OCCUPIEDCAPACITY:
                            route.setCarriedTraffic(route.getCarriedTraffic(), Double.parseDouble(newValue.toString()));
                        	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.ROUTE));
                        	callback.getVisualizationState ().pickRoute(route);
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
        return routeTableModel;
    }

    private void setDefaultCellRenderers(final IVisualizationCallback callback) {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());

        setDefaultRenderer(Boolean.class, new CellRenderers.RouteRenderer(getDefaultRenderer(Boolean.class), callback));
        setDefaultRenderer(Double.class, new CellRenderers.RouteRenderer(getDefaultRenderer(Double.class), callback));
        setDefaultRenderer(Object.class, new CellRenderers.RouteRenderer(getDefaultRenderer(Object.class), callback));
        setDefaultRenderer(Float.class, new CellRenderers.RouteRenderer(getDefaultRenderer(Float.class), callback));
        setDefaultRenderer(Long.class, new CellRenderers.RouteRenderer(getDefaultRenderer(Long.class), callback));
        setDefaultRenderer(Integer.class, new CellRenderers.RouteRenderer(getDefaultRenderer(Integer.class), callback));
        setDefaultRenderer(String.class, new CellRenderers.RouteRenderer(getDefaultRenderer(String.class), callback));
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
        for(Route route : getVisibleElementsInTable())
            for (Map.Entry<String, String> entry : route.getAttributes().entrySet())
                if(attColumnsHeaders.contains(entry.getKey()) == false)
                    attColumnsHeaders.add(entry.getKey());
        return attColumnsHeaders;
    }

    @Override
    public void doPopup(final MouseEvent e, final int row, final Object itemId) 
    {
        JPopupMenu popup = new JPopupMenu();
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final List<Route> routeRowsInTheTable = getVisibleElementsInTable();

        /* Add the popup menu option of the filters */
        final List<Route> selectedRoutes = (List<Route>) (List<?>) getSelectedElements().getFirst();
        if (!selectedRoutes.isEmpty()) 
        {
        	final JMenu submenuFilters = new JMenu ("Filters");
            final JMenuItem filterKeepElementsAffectedThisLayer = new JMenuItem("This layer: Keep elements associated to this route traffic");
            final JMenuItem filterKeepElementsAffectedAllLayers = new JMenuItem("All layers: Keep elements associated to this route traffic");
            submenuFilters.add(filterKeepElementsAffectedThisLayer);
            if (callback.getDesign().getNumberOfLayers() > 1) submenuFilters.add(filterKeepElementsAffectedAllLayers);
            filterKeepElementsAffectedThisLayer.addActionListener(new ActionListener() 
            {
				@Override
				public void actionPerformed(ActionEvent e) 
				{
					if (selectedRoutes.size() > 1) throw new RuntimeException ();
					TBFToFromCarriedTraffic filter = new TBFToFromCarriedTraffic(selectedRoutes.get(0), true);
					callback.getVisualizationState().updateTableRowFilter(filter);
					callback.updateVisualizationJustTables();
				}
			});
            filterKeepElementsAffectedAllLayers.addActionListener(new ActionListener() 
            {
				@Override
				public void actionPerformed(ActionEvent e) 
				{
					if (selectedRoutes.size() > 1) throw new RuntimeException ();
					TBFToFromCarriedTraffic filter = new TBFToFromCarriedTraffic(selectedRoutes.get(0), false);
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

        if (!routeRowsInTheTable.isEmpty()) {
            if (callback.getVisualizationState().isNetPlanEditable()) {
                if (row != -1) {
                    if (popup.getSubElements().length > 0) popup.addSeparator();

                    JMenuItem removeItem = new JMenuItem("Remove " + networkElementType);

                    removeItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NetPlan netPlan = callback.getDesign();
                            try {
                                netPlan.getRouteFromId((long) itemId).remove();
                                callback.getVisualizationState().resetPickedState();
                            	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.ROUTE));
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

                JMenuItem removeItems = new JMenuItem("Remove all table " + networkElementType + "s");

                removeItems.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final NetPlan netPlan = callback.getDesign();

                        try {
                        	if (rf == null)
                        		netPlan.removeAllRoutes();
                        	else
                        		for (Route r : routeRowsInTheTable) r.remove();
                            callback.getVisualizationState().resetPickedState();
                        	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.ROUTE));
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
    	callback.getVisualizationState ().pickRoute(callback.getDesign().getRouteFromId((long) itemId));
        callback.updateVisualizationAfterPick();
    }

    private JMenuItem getAddOption() {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    createRouteGUI(callback);
                    callback.getVisualizationState().resetPickedState();
                	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.ROUTE));
                	callback.getUndoRedoNavigationManager().addNetPlanChange();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });

        NetPlan netPlan = callback.getDesign();
        if (!netPlan.hasDemands()) addItem.setEnabled(false);
        return addItem;
    }

    private static void createRouteGUI(final IVisualizationCallback callback) {
        final NetPlan netPlan = callback.getDesign();
        final Collection<Long> demandIds = NetPlan.getIds(netPlan.getDemands());
        final JComboBox demandSelector = new WiderJComboBox();
        if (netPlan.getNumberOfLinks() == 0)
            throw new Net2PlanException("The network has no links at this network layer");

        final JTextField txt_carriedTraffic = new JTextField();
        final JTextField txt_occupiedCapacity = new JTextField();

        final List<JComboBox> seqLinks_cmb = new LinkedList<JComboBox>();
        final JPanel seqLinks_pnl = new JPanel();
        seqLinks_pnl.setLayout(new BoxLayout(seqLinks_pnl, BoxLayout.Y_AXIS));

        demandSelector.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                StringLabeller item = (StringLabeller) e.getItem();
                long demandId = (Long) item.getObject();
                Demand demand = netPlan.getDemandFromId(demandId);

                double h_d = demand.getOfferedTraffic();
                double r_d = demand.getCarriedTraffic();
                txt_carriedTraffic.setText(Double.toString(Math.max(0, h_d - r_d)));
                txt_occupiedCapacity.setText(Double.toString(Math.max(0, h_d - r_d)));

                seqLinks_cmb.clear();
                seqLinks_pnl.removeAll();

                Node ingressNode = demand.getIngressNode();
                String ingressNodeName = ingressNode.getName();

                Collection<Link> outgoingLinks = ingressNode.getOutgoingLinks();
                final JComboBox firstLink = new WiderJComboBox();
                for (Link link : outgoingLinks) {
                    long destinationNodeId = link.getDestinationNode().getId();
                    String destinationNodeName = link.getDestinationNode().getName();
                    firstLink.addItem(StringLabeller.of(link.getId(), String.format("e%d: n%d (%s) => n%d (%s)", link.getId(), ingressNode.getId(), ingressNodeName, destinationNodeId, destinationNodeName)));
                }

                firstLink.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        JComboBox me = (JComboBox) e.getSource();
                        Iterator<JComboBox> it = seqLinks_cmb.iterator();
                        while (it.hasNext()) if (it.next() == me) break;

                        while (it.hasNext()) {
                            JComboBox aux = it.next();
                            seqLinks_pnl.remove(aux);
                            it.remove();
                        }

                        seqLinks_pnl.revalidate();

                        List<Link> seqLinks = new LinkedList<Link>();
                        for (JComboBox link : seqLinks_cmb) 
                            seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));
                        callback.putTransientColorInElementTopologyCanvas (seqLinks , Color.BLUE);
                    }
                });

                setMaxSize(firstLink);

                seqLinks_cmb.add(firstLink);
                seqLinks_pnl.add(firstLink);

                JPanel pane = new JPanel(new FlowLayout());
                JButton addLink_btn = new JButton("Add new link");
                addLink_btn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        long linkId = (Long) ((StringLabeller) seqLinks_cmb.get(seqLinks_cmb.size() - 1).getSelectedItem()).getObject();
                        Link link = netPlan.getLinkFromId(linkId);
                        long destinationNodeId = link.getDestinationNode().getId();
                        String destinationNodeName = link.getDestinationNode().getName();

                        Set<Link> outgoingLinks = link.getDestinationNode().getOutgoingLinks();
                        if (outgoingLinks.isEmpty()) {
                            ErrorHandling.showErrorDialog("Last node has no outgoing links", "Error");
                            return;
                        }

                        final JComboBox newLink = new WiderJComboBox();
                        for (Link nextLink : outgoingLinks) {
                            long nextDestinationNodeId = nextLink.getDestinationNode().getId();
                            String nextDestinationNodeName = nextLink.getDestinationNode().getName();
                            newLink.addItem(StringLabeller.of(nextLink.getId(), String.format("e%d: n%d (%s) => n%d (%s)", nextLink.getId(), destinationNodeId, destinationNodeName, nextDestinationNodeId, nextDestinationNodeName)));
                        }

                        newLink.addItemListener(new ItemListener() {
                            @Override
                            public void itemStateChanged(ItemEvent e) {
                                JComboBox me = (JComboBox) e.getSource();
                                Iterator<JComboBox> it = seqLinks_cmb.iterator();
                                while (it.hasNext()) if (it.next() == me) break;

                                while (it.hasNext()) {
                                    JComboBox aux = it.next();
                                    seqLinks_pnl.remove(aux);
                                    it.remove();
                                }

                                seqLinks_pnl.revalidate();

                                List<Link> seqLinks = new LinkedList<Link>();
                                for (JComboBox link : seqLinks_cmb)
                                    seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));
                                callback.putTransientColorInElementTopologyCanvas (seqLinks , Color.BLUE);
                            }
                        });

                        setMaxSize(newLink);

                        seqLinks_cmb.add(newLink);
                        seqLinks_pnl.add(newLink, seqLinks_pnl.getComponentCount() - 1);
                        seqLinks_pnl.revalidate();

                        List<Link> seqLinks = new LinkedList<Link>();
                        for (JComboBox auxLink : seqLinks_cmb) {
                            seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) auxLink.getSelectedItem()).getObject()));
                        }
                        callback.putTransientColorInElementTopologyCanvas (seqLinks , Color.BLUE);
                    }
                });

                pane.add(addLink_btn);

                JButton removeLink_btn = new JButton("Remove last link");
                removeLink_btn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (seqLinks_cmb.size() < 2) {
                            ErrorHandling.showErrorDialog("Initial link cannot be removed", "Error");
                            return;
                        }

                        JComboBox cmb = seqLinks_cmb.get(seqLinks_cmb.size() - 1);
                        seqLinks_cmb.remove(cmb);
                        seqLinks_pnl.remove(cmb);
                        seqLinks_pnl.revalidate();

                        List<Link> seqLinks = new LinkedList<Link>();
                        for (JComboBox link : seqLinks_cmb)
                            seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));
                        callback.putTransientColorInElementTopologyCanvas (seqLinks , Color.BLUE);
                    }
                });

                pane.add(removeLink_btn);
                seqLinks_pnl.add(pane);

                seqLinks_pnl.revalidate();

                List<Link> seqLinks = new LinkedList<Link>();
                for (JComboBox link : seqLinks_cmb)
                    seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));
                callback.putTransientColorInElementTopologyCanvas (seqLinks , Color.BLUE);
            }
        });

        for (long demandId : demandIds) {
            Demand demand = netPlan.getDemandFromId(demandId);
            Node ingressNode = demand.getIngressNode();
            Node egressNode = demand.getEgressNode();
            long ingressNodeId = ingressNode.getId();
            long egressNodeId = egressNode.getId();

            final String ingressNodeName = ingressNode.getName();
            final String egressNodeName = egressNode.getName();

            final Set<Link> outgoingLinks = ingressNode.getOutgoingLinks();
            if (outgoingLinks.isEmpty()) continue;

            String demandLabel = "Demand " + demandId;
            demandLabel += ": n" + ingressNodeId;
            if (!ingressNodeName.isEmpty()) demandLabel += " (" + ingressNodeName + ")";

            demandLabel += " => n" + egressNodeId;
            if (!egressNodeName.isEmpty()) demandLabel += " (" + egressNodeName + ")";

            double h_d = demand.getOfferedTraffic();
            double r_d = demand.getCarriedTraffic();

            demandLabel += ", offered traffic = " + h_d;
            demandLabel += ", carried traffic = " + r_d;

            demandSelector.addItem(StringLabeller.of(demandId, demandLabel));
        }

        if (demandSelector.getItemCount() == 0) throw new Net2PlanException("Bad - No node has outgoing links");

        demandSelector.setSelectedIndex(0);

        final JScrollPane scrollPane = new JScrollPane(seqLinks_pnl, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Sequence of links"));
        scrollPane.setAlignmentY(JScrollPane.TOP_ALIGNMENT);

        final JPanel pane = new JPanel(new MigLayout("fill", "[][grow]", "[][grow][]"));
        pane.add(new JLabel("Demand"));
        pane.add(demandSelector, "growx, wrap, wmin 50");
        pane.add(scrollPane, "grow, spanx 2, wrap");
        pane.add(new JLabel("Carried traffic"));
        pane.add(txt_carriedTraffic, "grow, wrap");
        pane.add(new JLabel("Occupied capacity"));
        pane.add(txt_occupiedCapacity, "grow");
        pane.setPreferredSize(new Dimension(400, 400));

        while (true) {
            int result = JOptionPane.showConfirmDialog(null, pane, "Add new route", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) break;

            long demandId = (Long) ((StringLabeller) demandSelector.getSelectedItem()).getObject();

            double carriedTraffic;
            double occupiedCapacity = -1;

            try {
                carriedTraffic = Double.parseDouble(txt_carriedTraffic.getText());
                if (carriedTraffic < 0) throw new RuntimeException();
            } catch (Throwable e) {
                ErrorHandling.showErrorDialog("Carried traffic must be a non-negative number", "Error adding route");
                continue;
            }

            try {
                if (!txt_occupiedCapacity.getText().isEmpty()) {
                    occupiedCapacity = Double.parseDouble(txt_occupiedCapacity.getText());
                    if (occupiedCapacity < 0 && occupiedCapacity != -1) throw new RuntimeException();
                }
            } catch (Throwable e) {
                ErrorHandling.showErrorDialog("Occupied capacity must be a non-negative number, -1 or empty", "Error adding route");
                continue;
            }

            List<Link> seqLinks = new LinkedList<Link>();
            for (JComboBox link : seqLinks_cmb)
                seqLinks.add(netPlan.getLinkFromId((Long) ((StringLabeller) link.getSelectedItem()).getObject()));

            try {
                netPlan.addRoute(netPlan.getDemandFromId(demandId), carriedTraffic, occupiedCapacity, seqLinks, null);
            } catch (Throwable e) {
                ErrorHandling.showErrorDialog(e.getMessage(), "Error adding route");
                continue;
            }

            break;
        }
        callback.resetPickedStateAndUpdateView();
    }

    private static void setMaxSize(JComponent c) {
        final Dimension max = c.getMaximumSize();
        final Dimension pref = c.getPreferredSize();

        max.height = pref.height;
        c.setMaximumSize(max);
    }

    private List<JComponent> getExtraAddOptions() {
        List<JComponent> options = new LinkedList<JComponent>();
        NetPlan netPlan = callback.getDesign();

        final JMenuItem oneRoutePerDemandSPFHops = new JMenuItem("Add one route per demand, shortest path (Service chain) in hops");
        options.add(oneRoutePerDemandSPFHops);
        oneRoutePerDemandSPFHops.addActionListener(new RouteSPFActionListener(true, false));
        final JMenuItem oneRoutePerDemandSPFKm = new JMenuItem("Add one route per demand, shortest path (Service chain) in km");
        options.add(oneRoutePerDemandSPFKm);
        oneRoutePerDemandSPFKm.addActionListener(new RouteSPFActionListener(false, false));
        final JMenuItem oneRouteAndLinkDisjointSegmentPerDemandSPFHops = new JMenuItem("Add one route and 1+1 link disjoint protection per demand (minimize total num hops)");
        options.add(oneRouteAndLinkDisjointSegmentPerDemandSPFHops);
        oneRouteAndLinkDisjointSegmentPerDemandSPFHops.addActionListener(new RouteSPFActionListener(true, true));
        final JMenuItem oneRouteAndLinkDisjointSegmentPerDemandSPFKm = new JMenuItem("Add one route and 1+1 link disjoint protection per demand (minimize total km)");
        options.add(oneRouteAndLinkDisjointSegmentPerDemandSPFKm);
        oneRouteAndLinkDisjointSegmentPerDemandSPFKm.addActionListener(new RouteSPFActionListener(false, true));


        return options;
    }

    private class RouteSPFActionListener implements ActionListener {
        final boolean isMinHops;
        final boolean add11LinkDisjointSegment;

        private RouteSPFActionListener(boolean isMinHops, boolean minCost) {
            this.isMinHops = isMinHops;
            this.add11LinkDisjointSegment = minCost;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NetPlan netPlan = callback.getDesign();
            List<Link> links = netPlan.getLinks();
            final int E = links.size();
            Map<Link, Double> linkCostMap = new HashMap<Link, Double>();
            List<Route> addedRoutes = new LinkedList<Route>();
<<<<<<< HEAD
            List<ProtectionSegment> addedProtectionSegments = new LinkedList<ProtectionSegment>();
=======

            // Ask for current element removal
            if (netPlan.hasRoutes(netPlan.getNetworkLayerDefault()))
            {
                final int answer = JOptionPane.showConfirmDialog(null, "Remove all existing routes?", "", JOptionPane.YES_NO_CANCEL_OPTION);
                if (answer == JOptionPane.OK_OPTION) netPlan.removeAllRoutes(netPlan.getNetworkLayerDefault());
                if (answer == JOptionPane.CANCEL_OPTION || answer == JOptionPane.CLOSED_OPTION) return;
            }

>>>>>>> develop
            for (Link link : netPlan.getLinks())
            {
                linkCostMap.put(link, isMinHops ? 1 : link.getLengthInKm());
            }
            DoubleMatrix1D linkCostVector = null;
            if(isMinHops)
<<<<<<< HEAD
                linkCostVector = cern.colt.matrix.tdouble.DoubleFactory1D.dense.make(netPlan.getLinks().size(),1.0);
=======
                linkCostVector = DoubleFactory1D.dense.make(netPlan.getLinks().size(),1.0);
>>>>>>> develop
            else{
                linkCostVector = netPlan.getVectorLinkLengthInKm();
            }

            try {
                for (Demand d : netPlan.getDemands()) {
<<<<<<< HEAD
                    if (add11LinkDisjointSegment) {
                        List<List<Link>> twoPaths = GraphUtils.getTwoLinkDisjointPaths(netPlan.getNodes(), netPlan.getLinks(), d.getIngressNode(), d.getEgressNode(), linkCostMap);
                        if (twoPaths.size() != 2)
                            throw new Net2PlanException("Cannot find two link disjoint paths for demand of index " + d.getIndex() + ". No route or protection segment is created");
                        Route r = netPlan.addRoute(d, d.getOfferedTraffic(), d.getOfferedTraffic(), twoPaths.get(0), null);
                        ProtectionSegment s = netPlan.addProtectionSegment(twoPaths.get(1), d.getOfferedTraffic(), null);
                        r.addProtectionSegment(s);
                        addedRoutes.add(r);
                        addedProtectionSegments.add(s);
                    } else {
                        List<Link> seqLinks = GraphUtils.getShortestPath(netPlan.getNodes(), netPlan.getLinks(), d.getIngressNode(), d.getEgressNode(), linkCostMap);
                        List<Pair<List<NetworkElement>,Double>> minCostServiceChain = GraphUtils.getMinimumCostServiceChain(netPlan.getLinks(), d.getIngressNode(), d.getEgressNode(), d.getServiceChainSequenceOfTraversedResourceTypes(), linkCostVector , null, -1,-1,-1 );
                        if (seqLinks.isEmpty())
=======
                    if (add11LinkDisjointSegment)
                    {
                    	if (d.isServiceChainRequest())
                    	{
                            List<NetworkElement> minCostServiceChain = GraphUtils.getMinimumCostServiceChain(netPlan.getLinks(), d.getIngressNode(), d.getEgressNode(), d.getServiceChainSequenceOfTraversedResourceTypes(), linkCostVector , null, -1,-1,-1 ).getFirst();
                            if (minCostServiceChain.isEmpty()) throw new Net2PlanException("Cannot find a route for demand of index " + d.getIndex() + ". No route is created");
                            DoubleMatrix1D linkCostModified = linkCostVector.copy();
                            Map<Resource,Double> resourceCostModified = new HashMap<Resource,Double> ();
                            for (NetworkElement ee : minCostServiceChain) 
                            	if (ee instanceof Link) linkCostModified.set(ee.getIndex() , Double.MAX_VALUE);
                            	else if (ee instanceof Resource) resourceCostModified.put((Resource) ee , Double.MAX_VALUE);
                            List<NetworkElement> minCostServiceChain2 = 
                            		GraphUtils.getMinimumCostServiceChain(netPlan.getLinks(), d.getIngressNode(), 
                            				d.getEgressNode(), d.getServiceChainSequenceOfTraversedResourceTypes(), 
                            				linkCostModified , resourceCostModified, -1,-1,-1 ).getFirst();
                            if (minCostServiceChain2.isEmpty()) throw new Net2PlanException("Could not find two link and resource disjoint routes demand of index " + d.getIndex() + ". No route is created");
                            final Route r = netPlan.addServiceChain(d, d.getOfferedTraffic(), 
                            		Collections.nCopies(minCostServiceChain.size() , d.getOfferedTraffic()), minCostServiceChain, null);
                            final Route s = netPlan.addServiceChain(d , 0 , 
                            		Collections.nCopies(minCostServiceChain2.size() , d.getOfferedTraffic()) , minCostServiceChain2, null);
                            r.addBackupRoute(s);
                            addedRoutes.add(r);
                            addedRoutes.add(s);
                    	}
                    	else
                    	{
                            List<List<Link>> twoPaths = GraphUtils.getTwoLinkDisjointPaths(netPlan.getNodes(), netPlan.getLinks(), d.getIngressNode(), d.getEgressNode(), linkCostMap);
                            if (twoPaths.size() != 2)
                                throw new Net2PlanException("There are no two link disjoint paths for demand of index " + d.getIndex() + ". No route or protection segment is created");
                            final Route r = netPlan.addRoute(d, d.getOfferedTraffic(), d.getOfferedTraffic(), twoPaths.get(0), null);
                            final Route s = netPlan.addRoute(d , 0 , d.getOfferedTraffic() , twoPaths.get(1), null);
                            r.addBackupRoute(s);
                            addedRoutes.add(r);
                            addedRoutes.add(s);
                    	}
                    } else 
                    {
                        List<NetworkElement> minCostServiceChain = GraphUtils.getMinimumCostServiceChain(netPlan.getLinks(), d.getIngressNode(), d.getEgressNode(), d.getServiceChainSequenceOfTraversedResourceTypes(), linkCostVector , null, -1,-1,-1 ).getFirst();
                        if (minCostServiceChain.isEmpty())
>>>>>>> develop
                            throw new Net2PlanException("Cannot find a route for demand of index " + d.getIndex() + ". No route is created");
                        Route r = netPlan.addServiceChain(d, d.getOfferedTraffic(), 
                        		Collections.nCopies(minCostServiceChain.size() , d.getOfferedTraffic()) , 
                        		minCostServiceChain, null);
                        addedRoutes.add(r);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                for (Route r : addedRoutes) r.remove();
                ErrorHandling.showErrorDialog(ex.getMessage(), "Error adding routes and/or protection segments");
            }
            callback.getVisualizationState().resetPickedState();
        	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.ROUTE));
        	callback.getUndoRedoNavigationManager().addNetPlanChange();
        }
    }

    private List<JComponent> getExtraOptions(final int row, final Object itemId) {
        List<JComponent> options = new LinkedList<JComponent>();

        final int numRows = model.getRowCount();
        final NetPlan netPlan = callback.getDesign();

        if (itemId != null) {
            JMenuItem viewEditBackupRoutes = new JMenuItem("View/edit backup routes");
            viewEditBackupRoutes.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        viewEditBackupRoutesGUI(callback, (long) itemId);
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error viewing/editing backup routes");
                    }
                }
            });

            options.add(viewEditBackupRoutes);
        }


        return options;
    }

    private static void viewEditBackupRoutesGUI(final IVisualizationCallback callback, final long routeId) {
        final NetPlan netPlan = callback.getDesign();
        final Route route = netPlan.getRouteFromId(routeId);
        if (route.isBackupRoute()) throw new Net2PlanException("A backup route cannot have backup routes itself.");
        
        Set<Route> candidateBackupRoutes = route.getDemand().getRoutesAreNotBackup();
        List<Route> currentBackupRoutes = route.getBackupRoutes();

        final List<NetworkElement> seqLinksAndResources = route.getPath();

        if (candidateBackupRoutes.isEmpty()) throw new Net2PlanException("No backup route can be applied to this route");

        candidateBackupRoutes.removeAll(currentBackupRoutes);

        final JComboBox backupRouteSelector = new WiderJComboBox();

        final DefaultTableModel model = new ClassAwareTableModel(new Object[1][6], new String[]{"Id", "Seq. links/resources", "Seq. nodes", "Seq. occupied capacities", "", ""}) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 4 || columnIndex == 5;
            }
        };
        final JTable table = new AdvancedJTable(model);
        table.setEnabled(false);

        final JPanel addSegment_pnl = new JPanel(new MigLayout("", "[grow][][]", "[]"));
        JButton addSegment_btn = new JButton("Add");
        addSegment_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object selectedItem = backupRouteSelector.getSelectedItem();
                long backupRouteId = (Long) ((StringLabeller) selectedItem).getObject();
                Route backupRoute = netPlan.getRouteFromId(backupRouteId);
                route.addBackupRoute(backupRoute);
                callback.getVisualizationState().resetPickedState();
            	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.ROUTE));
            	callback.getUndoRedoNavigationManager().addNetPlanChange();

                backupRouteSelector.removeItem(selectedItem);
                if (backupRouteSelector.getItemCount() == 0) addSegment_pnl.setVisible(false);

                if (!table.isEnabled()) model.removeRow(0);
                model.addRow(new Object[]
                		{backupRouteId, 
                			getSequenceLinkResourceIndexes (backupRoute) , 
                			getSequenceNodeIndexesWithResourceInfo(backupRoute), 
                			getSequenceOccupiedCapacities(backupRoute), "Remove", "View"});
                table.setEnabled(true);
            }
        });

        JButton viewSegment_btn1 = new JButton("View");
        viewSegment_btn1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object selectedItem = backupRouteSelector.getSelectedItem();
                long backupRouteId = (Long) ((StringLabeller) selectedItem).getObject();
                List<NetworkElement> backupRoutePath = netPlan.getRouteFromId(backupRouteId).getPath();
                callback.putTransientColorInElementTopologyCanvas(seqLinksAndResources , Color.ORANGE);
                callback.putTransientColorInElementTopologyCanvas(backupRoutePath , Color.ORANGE);
            }
        });

        addSegment_pnl.add(backupRouteSelector, "growx, wmin 50");
        addSegment_pnl.add(addSegment_btn);
        addSegment_pnl.add(viewSegment_btn1);

        Action delete = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    JTable table = (JTable) e.getSource();
                    int modelRow = Integer.parseInt(e.getActionCommand());

                    final long backupRouteId = (Long) table.getModel().getValueAt(modelRow, 0);
                    final Route backupRoute = netPlan.getRouteFromId(backupRouteId);
                    netPlan.getRouteFromId(routeId).removeBackupRoute(backupRoute);
                    callback.getVisualizationState().resetPickedState();
                	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.ROUTE));
                	callback.getUndoRedoNavigationManager().addNetPlanChange();

                    String segmentLabel = "Backup route id " + backupRouteId + 
                    		": path = " + getSequenceLinkResourceIndexes(backupRoute) + 
                    		", seq. nodes = " + getSequenceNodeIndexesWithResourceInfo(backupRoute) + 
                    		", occupied capacity = " + getSequenceOccupiedCapacities(backupRoute);

                    backupRouteSelector.addItem(StringLabeller.of(backupRouteId, segmentLabel));

                    ((DefaultTableModel) table.getModel()).removeRow(modelRow);

                    table.setEnabled(true);

                    if (table.getModel().getRowCount() == 0) {
                        ((DefaultTableModel) table.getModel()).addRow(new Object[6]);
                        table.setEnabled(false);
                    }
                } catch (Throwable e1) {
                }
            }
        };

        Action view = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    JTable table = (JTable) e.getSource();
                    int modelRow = Integer.parseInt(e.getActionCommand());

                    final long backupRouteId = (Long) table.getModel().getValueAt(modelRow, 0);
                    final Route backupRoute = netPlan.getRouteFromId(backupRouteId);
                    callback.putTransientColorInElementTopologyCanvas(seqLinksAndResources , Color.ORANGE);
                    callback.putTransientColorInElementTopologyCanvas(backupRoute.getPath() , Color.ORANGE);
                } catch (Throwable e1) {
                }
            }
        };

        new ButtonColumn(table, delete, 4);
        new ButtonColumn(table, view, 5);

        final JScrollPane scrollPane = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Current backup segment list"));
        scrollPane.setAlignmentY(JScrollPane.TOP_ALIGNMENT);

        final JDialog dialog = new JDialog();
        dialog.setLayout(new BorderLayout());
        dialog.add(addSegment_pnl, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);

        for (Route backupRoute : candidateBackupRoutes) 
        {
            String segmentLabel = "Backup route id " + backupRoute.getId() + 
            		": path = " + getSequenceLinkResourceIndexes(backupRoute) + 
            		", seq. nodes = " + getSequenceNodeIndexesWithResourceInfo(backupRoute) + 
            		", occupied capacity = " + getSequenceOccupiedCapacities(backupRoute);
            backupRouteSelector.addItem(StringLabeller.of(backupRoute, segmentLabel));
        }

        if (backupRouteSelector.getItemCount() == 0) {
            addSegment_pnl.setVisible(false);
        } else {
            backupRouteSelector.setSelectedIndex(0);
        }

        if (!currentBackupRoutes.isEmpty()) {
            model.removeRow(0);

            for (Route backupRoute : currentBackupRoutes)
            {
                model.addRow(new Object[]{backupRoute.getId(), 
                		getSequenceLinkResourceIndexes(backupRoute) , getSequenceNodeIndexesWithResourceInfo (backupRoute) ,  
                		getSequenceOccupiedCapacities (backupRoute) , "Remove", "View"});
            }

            table.setEnabled(true);
        }

        table.setDefaultRenderer(Boolean.class, new UnfocusableCellRenderer());
        table.setDefaultRenderer(Double.class, new UnfocusableCellRenderer());
        table.setDefaultRenderer(Object.class, new UnfocusableCellRenderer());
        table.setDefaultRenderer(Float.class, new UnfocusableCellRenderer());
        table.setDefaultRenderer(Long.class, new UnfocusableCellRenderer());
        table.setDefaultRenderer(Integer.class, new UnfocusableCellRenderer());
        table.setDefaultRenderer(String.class, new UnfocusableCellRenderer());

        double x_p = netPlan.getRouteFromId(routeId).getCarriedTraffic();
        dialog.setTitle("View/edit backup route list for route " + routeId + " (carried traffic = " + x_p + ", occupied capacity = " + getSequenceOccupiedCapacities(netPlan.getRouteFromId(routeId)) + ")");
        SwingUtils.configureCloseDialogOnEscape(dialog);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(new Dimension(500, 300));
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);

        callback.resetPickedStateAndUpdateView();
    }

    private List<JComponent> getForcedOptions() {
        return new LinkedList<JComponent>();
    }
    
    private static String getSequenceLinkResourceIndexes (Route r)
    {
    	StringBuffer buf = new StringBuffer ();
    	for (NetworkElement e : r.getPath())
    		if (e instanceof Link) buf.append("L" + e.getIndex() + ",");
    		else if (e instanceof Resource) buf.append("R" + e.getIndex() + ",");
    	buf.setLength(buf.length()-1);
    	return buf.toString();
    }
    private static String getSequenceNodeIndexesWithResourceInfo (Route r)
    {
    	StringBuffer buf = new StringBuffer ();
    	buf.append("N" + r.getIngressNode().getIndex());
    	for (NetworkElement e : r.getPath())
    		if (e instanceof Link) buf.append(",N" + ((Link) e).getDestinationNode().getIndex());
    		else if (e instanceof Resource) buf.append(",(R" + e.getIndex() + ")");
    	return buf.toString();
    }
    
    private static String getSequenceOccupiedCapacities (Route r)
    {
    	if (r.isDown()) return "0";
    	if (r.getSeqOccupiedCapacitiesIfNotFailing().equals(Collections.nCopies(r.getPath().size() , r.getOccupiedCapacity())))
    		return ""+r.getOccupiedCapacity();
    	return CollectionUtils.join(r.getSeqOccupiedCapacitiesIfNotFailing() , ", ");
    }

    private List<Route> getVisibleElementsInTable ()
    {
    	final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
    	final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
    	return rf == null? callback.getDesign().getRoutes(layer) : rf.getVisibleRoutes(layer);
    }
}
