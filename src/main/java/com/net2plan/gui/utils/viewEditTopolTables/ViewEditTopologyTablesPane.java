package com.net2plan.gui.utils.viewEditTopolTables;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.LayoutManager;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

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
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.internal.Constants;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;

@SuppressWarnings("unchecked")
public class ViewEditTopologyTablesPane extends JPanel
{
	private final IVisualizationCallback callback;
    private JTabbedPane netPlanView;
    private Map<NetworkElementType, AdvancedJTable_NetworkElement> netPlanViewTable;
    private Map<NetworkElementType, JComponent> netPlanViewTableComponent;
    private Map<NetworkElementType, JLabel> netPlanViewTableNumEntriesLabel;

	public ViewEditTopologyTablesPane (IVisualizationCallback callback , LayoutManager layout)
	{
		super (layout);
		
		this.callback = callback;

        netPlanViewTable = new EnumMap<NetworkElementType, AdvancedJTable_NetworkElement>(NetworkElementType.class);
        netPlanViewTableComponent = new EnumMap<NetworkElementType, JComponent>(NetworkElementType.class);
        netPlanViewTableNumEntriesLabel = new EnumMap<NetworkElementType, JLabel>(NetworkElementType.class);

//        mainWindow.allowDocumentUpdate = mainWindow.isEditable();
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
                panel.add(netPlanViewTableNumEntriesLabel.get(elementType), BorderLayout.NORTH);
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
        final RoutingType routingType = currentState.getRoutingType();
        final boolean isSourceRouting = routingType == RoutingType.SOURCE_ROUTING;
        currentState.checkCachesConsistency();

        Component selectedTab = netPlanView.getSelectedComponent();
        netPlanView.removeAll();
        for (NetworkElementType elementType : Constants.NetworkElementType.values()) 
        {
            if (isSourceRouting && elementType == NetworkElementType.FORWARDING_RULE)
                continue;
            if (!isSourceRouting && (elementType == NetworkElementType.ROUTE))
                continue;
            netPlanView.addTab(elementType == NetworkElementType.NETWORK ? "Network" : netPlanViewTable.get(elementType).getTabName(), netPlanViewTableComponent.get(elementType));
        }

        for (int tabId = 0; tabId < netPlanView.getTabCount(); tabId++) {
            if (netPlanView.getComponentAt(tabId).equals(selectedTab)) {
                netPlanView.setSelectedIndex(tabId);
                break;
            }
        }

        currentState.checkCachesConsistency();
        
        /* update the required tables */
        for (Entry<NetworkElementType,AdvancedJTable_NetworkElement> entry : netPlanViewTable.entrySet())
        {
            if (isSourceRouting && entry.getKey() == NetworkElementType.FORWARDING_RULE) continue;
            if (!isSourceRouting && (entry.getKey() == NetworkElementType.ROUTE)) continue;
            entry.getValue().updateView(currentState);
        }
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
