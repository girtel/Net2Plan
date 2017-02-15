package com.net2plan.gui.utils.viewEditTopolTables;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableModel;

import com.net2plan.gui.utils.FullScrollPaneLayout;
import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.viewEditTopolTables.rightPanelTabs.NetPlanViewTableComponent_layer;
import com.net2plan.gui.utils.viewEditTopolTables.rightPanelTabs.NetPlanViewTableComponent_network;
<<<<<<< HEAD
import com.net2plan.gui.utils.viewEditTopolTables.specificTables.*;
import com.net2plan.gui.utils.FixedColumnDecorator;
import com.net2plan.gui.utils.FullScrollPaneLayout;
import com.net2plan.gui.utils.INetworkCallback;
=======
import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTable_NetworkElement;
import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTable_demand;
import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTable_forwardingRule;
import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTable_layer;
import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTable_link;
import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTable_multicastDemand;
import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTable_multicastTree;
import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTable_node;
import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTable_resource;
import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTable_route;
import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTable_srg;
import com.net2plan.gui.utils.viewEditTopolTables.tableVisualizationFilters.TBFToFromCarriedTraffic;
>>>>>>> develop
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.internal.Constants;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;

@SuppressWarnings("unchecked")
public class ViewEditTopologyTablesPane extends JPanel
{
	private final IVisualizationCallback callback;
    private final JTabbedPane netPlanView;
    private final Map<NetworkElementType, AdvancedJTable_NetworkElement> netPlanViewTable;
    private final Map<NetworkElementType, JComponent> netPlanViewTableComponent;
    private final Map<NetworkElementType, JLabel> netPlanViewTableNumEntriesLabel;

	public ViewEditTopologyTablesPane (IVisualizationCallback callback , LayoutManager layout)
	{
		super (layout);
		
		this.callback = callback;

        netPlanViewTable = new EnumMap<NetworkElementType, AdvancedJTable_NetworkElement>(NetworkElementType.class);
        netPlanViewTableComponent = new EnumMap<NetworkElementType, JComponent>(NetworkElementType.class);
        netPlanViewTableNumEntriesLabel = new EnumMap<NetworkElementType, JLabel>(NetworkElementType.class);

//        mainWindow.allowDocumentUpdate = mainWindow.isEditable();
<<<<<<< HEAD
        netPlanViewTable.put(NetworkElementType.NODE, new AdvancedJTable_node(mainWindow));
        netPlanViewTable.put(NetworkElementType.LINK, new AdvancedJTable_link(mainWindow));
        netPlanViewTable.put(NetworkElementType.DEMAND, new AdvancedJTable_demand(mainWindow));
        netPlanViewTable.put(NetworkElementType.ROUTE, new AdvancedJTable_route(mainWindow));
        netPlanViewTable.put(NetworkElementType.PROTECTION_SEGMENT, new AdvancedJTable_segment(mainWindow));
        netPlanViewTable.put(NetworkElementType.FORWARDING_RULE, new AdvancedJTable_forwardingRule(mainWindow));
        netPlanViewTable.put(NetworkElementType.MULTICAST_DEMAND, new AdvancedJTable_multicastDemand(mainWindow));
        netPlanViewTable.put(NetworkElementType.MULTICAST_TREE, new AdvancedJTable_multicastTree(mainWindow));
        netPlanViewTable.put(NetworkElementType.SRG, new AdvancedJTable_srg(mainWindow));
        netPlanViewTable.put(NetworkElementType.LAYER, new AdvancedJTable_layer(mainWindow));
        netPlanViewTable.put(NetworkElementType.RESOURCE, new AdvancedJTable_resource(mainWindow));

=======
        netPlanViewTable.put(NetworkElementType.NODE, new AdvancedJTable_node(callback));
        netPlanViewTable.put(NetworkElementType.LINK, new AdvancedJTable_link(callback));
        netPlanViewTable.put(NetworkElementType.DEMAND, new AdvancedJTable_demand(callback));
        netPlanViewTable.put(NetworkElementType.ROUTE, new AdvancedJTable_route(callback));
        netPlanViewTable.put(NetworkElementType.FORWARDING_RULE, new AdvancedJTable_forwardingRule(callback));
        netPlanViewTable.put(NetworkElementType.MULTICAST_DEMAND, new AdvancedJTable_multicastDemand(callback));
        netPlanViewTable.put(NetworkElementType.MULTICAST_TREE, new AdvancedJTable_multicastTree(callback));
        netPlanViewTable.put(NetworkElementType.SRG, new AdvancedJTable_srg(callback));
        netPlanViewTable.put(NetworkElementType.RESOURCE, new AdvancedJTable_resource(callback));
        netPlanViewTable.put(NetworkElementType.LAYER, new AdvancedJTable_layer(callback));

        netPlanViewTableNumEntriesLabel.put(NetworkElementType.NODE, new JLabel ("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.LINK, new JLabel ("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.DEMAND, new JLabel ("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.ROUTE, new JLabel ("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.FORWARDING_RULE, new JLabel ("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.MULTICAST_DEMAND, new JLabel ("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.MULTICAST_TREE, new JLabel ("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.SRG, new JLabel ("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.RESOURCE, new JLabel ("Number of entries: "));
        netPlanViewTableNumEntriesLabel.put(NetworkElementType.LAYER, new JLabel ("Number of entries: "));
        
>>>>>>> develop
        netPlanView = new JTabbedPane();

        for (NetworkElementType elementType : Constants.NetworkElementType.values()) {
            if (elementType == NetworkElementType.NETWORK) {
                netPlanViewTableComponent.put(elementType, new NetPlanViewTableComponent_network(callback, (AdvancedJTable_layer) netPlanViewTable.get(NetworkElementType.LAYER)));
            } else if (elementType == NetworkElementType.LAYER) {
                netPlanViewTableComponent.put(elementType, new NetPlanViewTableComponent_layer(callback, (AdvancedJTable_layer) netPlanViewTable.get(NetworkElementType.LAYER)));
            } else {
                JScrollPane scrollPane = netPlanViewTable.get(elementType).getScroll();
                scrollPane.setLayout(new FullScrollPaneLayout());
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                netPlanViewTable.get(elementType).getFixedTable().getColumnModel().getColumn(0).setMinWidth(50);
                final JPanel panel = new JPanel ();
                panel.setLayout(new BorderLayout());
                JPanel labelsPanel = new JPanel (); labelsPanel.setLayout(new BorderLayout());
                labelsPanel.setBackground(Color.YELLOW);
                labelsPanel.setForeground(Color.BLACK);
                labelsPanel.add(netPlanViewTableNumEntriesLabel.get(elementType), BorderLayout.CENTER);
                {
                	final JPanel buttonsPanel = new JPanel ();
                    final JButton resetTableRowFilters = new JButton ("Reset VFs");
                    buttonsPanel.add(resetTableRowFilters , BorderLayout.EAST);
                    resetTableRowFilters.addActionListener(new ActionListener()
    				{
    					@Override
    					public void actionPerformed(ActionEvent e)
    					{
    						callback.getVisualizationState().updateTableRowFilter(null);
    						callback.updateVisualizationJustTables();
    						callback.resetPickedStateAndUpdateView();
    					}
    				});
                    labelsPanel.add(buttonsPanel , BorderLayout.EAST);
                }
                
                labelsPanel.setBorder(BorderFactory.createEmptyBorder(3 , 0 , 3 , 0));
                panel.add(labelsPanel , BorderLayout.NORTH);
                panel.add(scrollPane, BorderLayout.CENTER);
                netPlanViewTableComponent.put(elementType, panel);
            }
        }

        this.add(netPlanView, BorderLayout.CENTER);

//        this.add(new JLabel ("Number of entries"), BorderLayout.NORTH);
        
        
	}

	public Map<NetworkElementType,AdvancedJTable_NetworkElement> currentTables(){

	    return netPlanViewTable;
    }

	public void resetTables()
    {
        netPlanViewTable.clear();
        netPlanViewTableComponent.clear();

<<<<<<< HEAD
        netPlanViewTable.put(NetworkElementType.NODE, new AdvancedJTable_node(mainWindow));
        netPlanViewTable.put(NetworkElementType.LINK, new AdvancedJTable_link(mainWindow));
        netPlanViewTable.put(NetworkElementType.DEMAND, new AdvancedJTable_demand(mainWindow));
        netPlanViewTable.put(NetworkElementType.ROUTE, new AdvancedJTable_route(mainWindow));
        netPlanViewTable.put(NetworkElementType.PROTECTION_SEGMENT, new AdvancedJTable_segment(mainWindow));
        netPlanViewTable.put(NetworkElementType.FORWARDING_RULE, new AdvancedJTable_forwardingRule(mainWindow));
        netPlanViewTable.put(NetworkElementType.MULTICAST_DEMAND, new AdvancedJTable_multicastDemand(mainWindow));
        netPlanViewTable.put(NetworkElementType.MULTICAST_TREE, new AdvancedJTable_multicastTree(mainWindow));
        netPlanViewTable.put(NetworkElementType.SRG, new AdvancedJTable_srg(mainWindow));
        netPlanViewTable.put(NetworkElementType.LAYER, new AdvancedJTable_layer(mainWindow));
        netPlanViewTable.put(NetworkElementType.RESOURCE, new AdvancedJTable_resource(mainWindow));
=======
        netPlanViewTable.put(NetworkElementType.NODE, new AdvancedJTable_node(callback));
        netPlanViewTable.put(NetworkElementType.LINK, new AdvancedJTable_link(callback));
        netPlanViewTable.put(NetworkElementType.DEMAND, new AdvancedJTable_demand(callback));
        netPlanViewTable.put(NetworkElementType.ROUTE, new AdvancedJTable_route(callback));
        netPlanViewTable.put(NetworkElementType.FORWARDING_RULE, new AdvancedJTable_forwardingRule(callback));
        netPlanViewTable.put(NetworkElementType.MULTICAST_DEMAND, new AdvancedJTable_multicastDemand(callback));
        netPlanViewTable.put(NetworkElementType.MULTICAST_TREE, new AdvancedJTable_multicastTree(callback));
        netPlanViewTable.put(NetworkElementType.SRG, new AdvancedJTable_srg(callback));
        netPlanViewTable.put(NetworkElementType.RESOURCE, new AdvancedJTable_resource(callback));
        netPlanViewTable.put(NetworkElementType.LAYER, new AdvancedJTable_layer(callback));
>>>>>>> develop

        for (NetworkElementType elementType : Constants.NetworkElementType.values()) {
            if (elementType == NetworkElementType.NETWORK) {
                netPlanViewTableComponent.put(elementType, new NetPlanViewTableComponent_network(callback, (AdvancedJTable_layer) netPlanViewTable.get(NetworkElementType.LAYER)));
            } else if (elementType == NetworkElementType.LAYER) {
                netPlanViewTableComponent.put(elementType, new NetPlanViewTableComponent_layer(callback, (AdvancedJTable_layer) netPlanViewTable.get(NetworkElementType.LAYER)));
            } else {
                JScrollPane scrollPane = new JScrollPane(netPlanViewTable.get(elementType));
                scrollPane.setLayout(new FullScrollPaneLayout());
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                if(netPlanViewTable.get(elementType) instanceof AdvancedJTable_NetworkElement)
                {
                    scrollPane.setRowHeaderView(((AdvancedJTable_NetworkElement) netPlanViewTable.get(elementType)).getFixedTable());
                    scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, ((AdvancedJTable_NetworkElement) netPlanViewTable.get(elementType)).getFixedTable().getTableHeader());
                    scrollPane.getRowHeader().addChangeListener(new ChangeListener(){

                        @Override
                        public void stateChanged(ChangeEvent e)
                        {
                            JViewport viewport = (JViewport) e.getSource();
                            scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
                        }
                    });
                }
                netPlanViewTableComponent.put(elementType, scrollPane);
            }
        }

    }

	public JTabbedPane getNetPlanView () { return netPlanView; }
	
    public Map<NetworkElementType, AdvancedJTable_NetworkElement> getNetPlanViewTable () { return netPlanViewTable; }

    public void updateView ()
    {
		/* Load current network state */
        final NetPlan currentState = callback.getDesign();
        final NetworkLayer layer = currentState.getNetworkLayerDefault();
        currentState.checkCachesConsistency();

        final int selectedTabIndex = netPlanView.getSelectedIndex();
        netPlanView.removeAll();
        for (NetworkElementType elementType : Constants.NetworkElementType.values()) 
        {
            if (layer.isSourceRouting() && elementType == NetworkElementType.FORWARDING_RULE)
                continue;
            if (!layer.isSourceRouting() && (elementType == NetworkElementType.ROUTE))
                continue;
            netPlanView.addTab(elementType == NetworkElementType.NETWORK ? "Network" : netPlanViewTable.get(elementType).getTabName(), netPlanViewTableComponent.get(elementType));
        }
        if ((selectedTabIndex < netPlanView.getTabCount()) && (selectedTabIndex >= 0))
            netPlanView.setSelectedIndex(selectedTabIndex);

        currentState.checkCachesConsistency();
        
        /* update the required tables */
        for (Entry<NetworkElementType,AdvancedJTable_NetworkElement> entry : netPlanViewTable.entrySet())
        {
            if (layer.isSourceRouting() && entry.getKey() == NetworkElementType.FORWARDING_RULE) continue;
            if (!layer.isSourceRouting() && (entry.getKey() == NetworkElementType.ROUTE)) continue;
            final AdvancedJTable_NetworkElement table = entry.getValue();
            table.updateView(currentState);
            final JLabel label = netPlanViewTableNumEntriesLabel.get(entry.getKey());
            if (label != null)
            {
            	final int numEntries = table.getModel().getRowCount() - 1; // last colums is for the aggregation
            	if (callback.getVisualizationState().getTableRowFilter() != null)
            		label.setText("Number of entries: " + numEntries + ", FILTERED VIEW: " + callback.getVisualizationState().getTableRowFilter().getDescription());
            	else
            		label.setText("Number of entries: " + numEntries);
            }
            
        }
<<<<<<< HEAD

        NetPlan initialState = null;
        if (showInitialPlan != null && mainWindow.getInitialDesign().getNetworkLayerFromId(layer.getId()) != null)
            initialState = mainWindow.getInitialDesign();

        currentState.checkCachesConsistency();

        for (AdvancedJTableNetworkElement table : netPlanViewTable.values())
        {
            table.updateView(currentState, initialState);
        }
=======
>>>>>>> develop
        ((NetPlanViewTableComponent_layer) netPlanViewTableComponent.get(NetworkElementType.LAYER)).updateNetPlanView(currentState);
        ((NetPlanViewTableComponent_network) netPlanViewTableComponent.get(NetworkElementType.NETWORK)).updateNetPlanView(currentState);
    }

    
    /**
     * Shows the tab corresponding associated to a network element.
     *
     * @param type    Network element type
     * @param itemId  Item identifier (if null, it will just show the tab)
     */
    public void selectViewItem (NetworkElementType type, Object itemId)
    {
        AdvancedJTable_NetworkElement table = netPlanViewTable.get(type);
        int tabIndex = netPlanView.getSelectedIndex();
        int col = 0;
        if (netPlanView.getTitleAt(tabIndex).equals(type == NetworkElementType.NETWORK ? "Network" : table.getTabName())) {
            col = table.getSelectedColumn();
            if (col == -1) col = 0;
        } else {
            netPlanView.setSelectedComponent(netPlanViewTableComponent.get(type));
        }

        if (itemId == null) {
            table.clearSelection();
            return;
        }

        TableModel model = table.getModel();
        int numRows = model.getRowCount();
        for (int row = 0; row < numRows; row++) 
        {
            Object obj = model.getValueAt(row, 0);
            if (obj == null) continue;

            if (type == NetworkElementType.FORWARDING_RULE) {
                obj = Pair.of(
                		Integer.parseInt(model.getValueAt(row, AdvancedJTable_forwardingRule.COLUMN_DEMAND).toString().split(" ")[0]), 
                		Integer.parseInt(model.getValueAt(row, AdvancedJTable_forwardingRule.COLUMN_OUTGOINGLINK).toString().split(" ")[0]));
                if (!obj.equals(itemId)) continue;
            } else if ((long) obj != (long) itemId) {
                continue;
            }

            row = table.convertRowIndexToView(row);
            table.changeSelection(row, col, false, true);
            return;
        }

        throw new RuntimeException(type + " " + itemId + " does not exist");
    }


    public void showMainTab ()
    {
    	getNetPlanView().setSelectedIndex(0);
    }

}


//final JButton applyIntersectFilter = new JButton ("AND filter");
//buttonsPanel.add(applyIntersectFilter , BorderLayout.CENTER);
//applyIntersectFilter.addActionListener(new ActionListener()
//{
//	@Override
//	public void actionPerformed(ActionEvent e)
//	{
//		final AdvancedJTable_NetworkElement table = netPlanViewTable.get(elementType);
//		final int  [] selectedElements = table.getSelectedRows();
//		if (selectedElements.length == 0) return;
//		if (selectedElements.length > 1) throw new RuntimeException("MULTIPLE SELECTIONS NOT IMPLEMENTED");
//		if (elementType != NetworkElementType.FORWARDING_RULE)
//		{
//			final List<NetworkElement> selectedNetElements = new LinkedList<NetworkElement> ();
//			for (int row : selectedElements) selectedNetElements.add(callback.getDesign().getNetworkElement((long) table.getValueAt(row , 0)));
//
//
//			PABLO: HACER EL FILTRO GENERICO QUE LO LLAMAS SIN SABER DE DONDE ES LA TABLA, CON EL UP DOWN Y TODA LA HISTORIA
//		}
//		
//		ITableRowFilter filter = callback.getVisualizationState().getTableRowFilter();
//		if (filter == null)
//			filter = new TBFToFromCarriedTraffic(pickedDemand , showInCanvasThisLayerPropagation , showInCanvasLowerLayerPropagation , showInCanvasUpperLayerPropagation);
//		
//		callback.getVisualizationState().setTableRowFilter(null);
//		callback.updateVisualizationJustTables();
//		callback.resetPickedStateAndUpdateView();
//	}
//});
