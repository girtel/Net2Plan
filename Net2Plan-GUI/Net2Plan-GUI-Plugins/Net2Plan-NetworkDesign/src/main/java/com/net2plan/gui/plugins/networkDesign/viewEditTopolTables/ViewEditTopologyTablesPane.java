/*******************************************************************************
 * 
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/
package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables;


// TODO: JAVIER. Differential coloring to see the differences
// TODO: JAVIER. Pick manager with ALT-LEFT/RIGHT
// TODO: JAVIER. Left layer selector, 7 buttons. 5 for layer, 2 for up and down (each double, one for scroll up (only visible if more than 5 layers), other for moving up visually the active layer), only visible if more than 5 layers.  
// TODO: PABLO: Do the multi traffic matrix thing for forecast, add the gravity model algorithm for traffic matrix prediction

// TODO: All the layers shown at the same time
// TODO: Simplify coloring exploiting we have id bidimap in the table
// TODO: Tips per cell (not per column) to report extra information (e.g. when QoS violation, who and where)

// pick, and everywhere FR or NE can come. 

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.utils.FilteredTablePanel;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_abstractElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_demand;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_forwardingRule;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_layer;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_link;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_multicastDemand;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_multicastTree;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_node;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_resource;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_route;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_srg;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.rightPanelTabs.NetPlanViewTableComponent_layer;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.rightPanelTabs.NetPlanViewTableComponent_network;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.rightPanelTabs.NetPlanViewTableComponent_trafficMatrix;
import com.net2plan.gui.utils.NetworkElementOrFr;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.Pair;
import com.net2plan.utils.SwingUtils;

@SuppressWarnings("unchecked")
public class ViewEditTopologyTablesPane extends JPanel
{
    private final GUINetworkDesign callback;
    private final JTabbedPane netPlanView;
    private final Map<NetworkLayer,Map<AJTableType, Pair<AdvancedJTable_abstractElement, FilteredTablePanel>>> netPlanViewTable = new HashMap<> (); //new EnumMap<>(AJTableType.class);
    private final Map<NetworkLayer,JTabbedPane> demandTabbedPaneListAndMatrix = new HashMap<> (); //JTabbedPane ();
    private final Map<NetworkLayer, JTabbedPane> layerSubTabbedPaneMap = new HashMap<>();
    private Map<NetworkLayer,NetPlanViewTableComponent_trafficMatrix> trafficMatrixComponent;
    private Map<NetworkLayer,NetPlanViewTableComponent_layer> highLevelTabComponent_layer;
    private NetPlanViewTableComponent_network highLevelTabComponent_network;

    private final JMenuBar menuBar;
    private final JMenu exportMenu;

    public ViewEditTopologyTablesPane(GUINetworkDesign callback)
    {
        super(new BorderLayout());
        this.callback = callback;
        this.netPlanView = new JTabbedPane();
        
        final JSplitPane splitPane = new JSplitPane();
        splitPane.setLeftComponent(netPlanView);
        splitPane.setRightComponent(new JPanel ());

        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.3);
        splitPane.setEnabled(true);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(0.4);

        this.add(splitPane, BorderLayout.CENTER);

        this.recomputNetPlanView ();
        
        this.add(netPlanView, BorderLayout.CENTER);

        final JMenuItem writeToExcel = new JMenuItem("To excel");
        writeToExcel.addActionListener((ActionEvent ev) ->
        {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);

            FileFilter xlsFilter = new FileNameExtensionFilter("Excel 2003 file (*.xls)", "xls");
            FileFilter xlsxFilter = new FileNameExtensionFilter("Excel 2007 file (*.xlsx)", "xlsx");
            fileChooser.addChoosableFileFilter(xlsFilter);
            fileChooser.addChoosableFileFilter(xlsxFilter);

            final int res = fileChooser.showSaveDialog(null);

            if (res == JFileChooser.APPROVE_OPTION)
            {
                final File file = SwingUtils.getSelectedFileWithExtension(fileChooser);

                if (file.exists())
                {
                    int option = JOptionPane.showConfirmDialog(null, "File already exists.\nOverwrite?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);

                    if (option == JOptionPane.YES_OPTION)
                        file.delete();
                    else
                        return;
                }

                try
                {
                    final NetPlan netPlan = callback.getDesign();
                    for (NetworkLayer layer : netPlan.getNetworkLayers())
                    {
                        for (Pair<AdvancedJTable_abstractElement, FilteredTablePanel> tableInfo : netPlanViewTable.get(layer).values())
                        {
                        	final AdvancedJTable_abstractElement table = tableInfo.getFirst();
                            table.writeTableToFile(file , layer);
                            if (table instanceof AdvancedJTable_demand)
                                trafficMatrixComponent.get(layer).writeTrafficMatrixTableToFile(file , layer);
                        }
                    }
                    ErrorHandling.showInformationDialog("Excel file successfully written", "Finished writing into file");
                } catch (Exception e)
                {
                    ErrorHandling.showErrorDialog("Error");
                    e.printStackTrace();
                }
            }
        });

        menuBar = new JMenuBar();

        exportMenu = new JMenu("Export tables...");
        exportMenu.add(writeToExcel);

        menuBar.add(exportMenu);

        this.add(menuBar, BorderLayout.SOUTH);
    }

    public Map<AJTableType, AdvancedJTable_abstractElement> getNetPlanViewTable(NetworkLayer layer)
    {
        return netPlanViewTable.get(layer).entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e->e.getValue().getFirst()));
    }

    public void updateView()
    {
        /* Load current network state */
        final NetPlan currentState = callback.getDesign();
        if (ErrorHandling.isDebugEnabled()) currentState.checkCachesConsistency();
        
        this.recomputNetPlanView();
        
//        final AdvancedJTable_abstractElement layerTable = this.netPlanViewTable.get(AJTableType.LAYERS).getFirst();
//    	System.out.println(layerTable.getTableScrollPane().getViewport());
//    	System.out.println("View is null? " + (layerTable.getTableScrollPane().getViewport().getView() == null));

        highLevelTabComponent_network.updateNetPlanView(currentState);
        for (NetworkLayer layer : currentState.getNetworkLayers())
        {
            netPlanViewTable.get(layer).values().stream().map(t -> t.getFirst()).forEach(t -> t.updateView());
            trafficMatrixComponent.get(layer).updateNetPlanView();
            highLevelTabComponent_layer.get(layer).updateNetPlanView(currentState);

            // Update filter header
            for (AJTableType type : AJTableType.values())
            	if (netPlanViewTable.get(layer).get(type).getSecond() != null)
            		netPlanViewTable.get(layer).get(type).getSecond().updateHeader();
        }


        if (ErrorHandling.isDebugEnabled()) currentState.checkCachesConsistency();
    }


    /**
     * Shows the tab corresponding associated to a network element.
     *
     * @param type   Network element type
     * @param itemId Item identifier (if null, it will just show the tab)
     */
    public void selectItemTab(NetworkElementType type , NetworkLayer layer)
    {
    	if (type == NetworkElementType.NETWORK) { netPlanView.setSelectedComponent(highLevelTabComponent_network); return; } 

    	final JTabbedPane subpaneThisLayer = layerSubTabbedPaneMap.get(layer);
		netPlanView.setSelectedComponent(subpaneThisLayer);
		final JTabbedPane subtabOfLayer = (JTabbedPane) netPlanView.getComponent(1+layer.getIndex());
		
    	if (type == NetworkElementType.LAYER) { subtabOfLayer.setSelectedComponent(highLevelTabComponent_layer.get(layer)); return; } 
    	if (type == NetworkElementType.DEMAND) 
    	{ 
    		subtabOfLayer.setSelectedComponent(demandTabbedPaneListAndMatrix.get(layer));
    		demandTabbedPaneListAndMatrix.get(layer).setSelectedComponent(netPlanViewTable.get(layer).get(AJTableType.DEMANDS).getSecond());
    		return;
    	} 
    	/* rest of the tables */
    	subpaneThisLayer.setSelectedComponent(netPlanViewTable.get(layer).get(AJTableType.getTypeOfElement(type)).getSecond());
    }

    public void selectTabAndGivenItems(NetworkElementType type, NetworkLayer layer , List<NetworkElementOrFr> elements)
    {
        final AdvancedJTable_abstractElement table = (AdvancedJTable_abstractElement) netPlanViewTable.get(layer).get(AJTableType.getTypeOfElement(type)).getFirst();
		table.clearSelection();
        final List<Integer> modelViewRows = elements.stream().map(ee->(Integer) table.getRowModelIndexOfElement(ee.getObject()).orElse(-1)).
        		filter(ee->ee != -1).
        		collect(Collectors.toList()); 
        for (int rowModelIndex : modelViewRows)
        {
            final int viewRow = table.convertRowIndexToView(rowModelIndex);
            table.addRowSelectionInterval(viewRow, viewRow);
        }
        selectItemTab(type , layer);
    }

    private Pair<AdvancedJTable_abstractElement, FilteredTablePanel> createPanelComponentInfo(AJTableType type , NetworkLayer layerThisTable)
    {
        AdvancedJTable_abstractElement table = null;
        switch(type)
        {
		case DEMANDS:
            table = new AdvancedJTable_demand(callback , layerThisTable);
			break;
		case FORWARDINGRULES:
            table = new AdvancedJTable_forwardingRule(callback , layerThisTable);
			break;
		case LAYERS:
            table = new AdvancedJTable_layer(callback , layerThisTable);
			break;
		case LINKS:
            table = new AdvancedJTable_link(callback , layerThisTable);
			break;
		case MULTICAST_DEMANDS:
            table = new AdvancedJTable_multicastDemand(callback , layerThisTable);
			break;
		case MULTICAST_TREES:
            table = new AdvancedJTable_multicastTree(callback , layerThisTable);
			break;
		case NODES:
            table = new AdvancedJTable_node(callback , layerThisTable);
			break;
		case RESOURCES:
            table = new AdvancedJTable_resource(callback , layerThisTable);
			break;
		case ROUTES:
            table = new AdvancedJTable_route(callback , layerThisTable);
			break;
		case SRGS:
            table = new AdvancedJTable_srg(callback , layerThisTable);
			break;
		default:
        	System.out.println(type);
            assert false;
        }

        return Pair.of(table, new FilteredTablePanel(callback, table.getTableScrollPane()));
    }


    public void resetPickedState()
    {
    	for (NetworkLayer layer : callback.getDesign().getNetworkLayers())
    		netPlanViewTable.get(layer).values().stream().filter(q -> q.getFirst() != null).forEach(q -> q.getFirst().clearSelection());
    }

    
    private void recomputNetPlanView ()
    {    	
    	/* Save current selected tab */
		final int selectedIndexFirstLevel = netPlanView.getSelectedIndex() == -1 ? 0 : netPlanView.getSelectedIndex();
		int selectedIndexSecondLevel = -1;
		if (netPlanView.getSelectedComponent() instanceof JTabbedPane)
			selectedIndexSecondLevel = ((JTabbedPane) netPlanView.getSelectedComponent()).getSelectedIndex();

    	
    	/* Save hide columns, decimal formats and attribute collapsed */
    	final Map<NetworkLayer, Map<AJTableType, Map<String, Boolean>>> hideColumnStatePerTable = new HashMap<>();
    	final Map<NetworkLayer, Map<AJTableType, Map<String, Integer>>> columnDoubleFormatStatePerTable = new HashMap<>();
    	final Map<NetworkLayer, Map<AJTableType, Boolean>> attributeCollapsedStatePerTable = new HashMap<>();
    	
    	for (Map.Entry<NetworkLayer, Map<AJTableType, Pair<AdvancedJTable_abstractElement, FilteredTablePanel>>> entry : netPlanViewTable.entrySet())
    	{
    		final NetworkLayer layer = entry.getKey();
    		final Map<AJTableType, Map<String, Boolean>> hideColumnStateThisTable = new HashMap<>();
    		final Map<AJTableType, Map<String, Integer>> columnDoubleFormatStateThisTable = new HashMap<>();
    		final Map<AJTableType, Boolean> attributeCollapsedStateThisTable = new HashMap<>();

    		for (AJTableType ajTableType : entry.getValue().keySet())
    		{
    			if (ajTableType == AJTableType.LAYERS) continue;
    			
    			final AdvancedJTable_abstractElement table = entry.getValue().get(ajTableType).getFirst();
    			hideColumnStateThisTable.put(ajTableType, table.getColumnShowHideValueByHeaderMap());
    			columnDoubleFormatStateThisTable.put(ajTableType, table.getColumnNumberOfDecimalsByHeaderMap());
    			
    			if (table instanceof AdvancedJTable_networkElement)
    				attributeCollapsedStateThisTable.put(ajTableType, ((AdvancedJTable_networkElement) table).isAttributesAreCollapsedInOneColumn());
    		}
    		hideColumnStatePerTable.put(layer, hideColumnStateThisTable);
    		columnDoubleFormatStatePerTable.put(layer, columnDoubleFormatStateThisTable);
    		attributeCollapsedStatePerTable.put(layer, attributeCollapsedStateThisTable);
    	}
    	
    	final NetPlan np = callback.getDesign();
    	netPlanViewTable.clear();
    	demandTabbedPaneListAndMatrix.clear();
    	trafficMatrixComponent = new HashMap<> ();
    	highLevelTabComponent_layer = new HashMap<> ();
    	layerSubTabbedPaneMap.clear();

    	netPlanView.removeAll();
    	
        final AdvancedJTable_layer layerTable = new AdvancedJTable_layer (callback , callback.getDesign().getNetworkLayerDefault());
    	highLevelTabComponent_network = new NetPlanViewTableComponent_network(callback, layerTable);
    	netPlanView.addTab("Network", highLevelTabComponent_network);
    	
        for (NetworkLayer layer : np.getNetworkLayers())
        {
        	layerSubTabbedPaneMap.put(layer, new JTabbedPane());
        	final JTabbedPane subpaneThisLayer = layerSubTabbedPaneMap.get(layer);        
        	netPlanViewTable.put (layer , new HashMap<> ());
        	netPlanViewTable.get(layer).put(AJTableType.LAYERS, Pair.of(layerTable, null));
        	demandTabbedPaneListAndMatrix.put(layer, new JTabbedPane ());
        	highLevelTabComponent_layer.put(layer, new NetPlanViewTableComponent_layer(callback , layer));
        	subpaneThisLayer.addTab("Layers", highLevelTabComponent_layer.get(layer));
            for (AJTableType ajType : AJTableType.values())
            {
            	if (ajType == ajType.LAYERS) continue;
            	if (ajType == ajType.DEMANDS)
            	{
                	final Pair<AdvancedJTable_abstractElement, FilteredTablePanel> component = createPanelComponentInfo(ajType , layer); 
                	netPlanViewTable.get(layer).put(ajType, component);
                	this.trafficMatrixComponent.put (layer, new NetPlanViewTableComponent_trafficMatrix(callback , layer));
                    this.demandTabbedPaneListAndMatrix.get(layer).addTab("List view", component.getSecond());
                    this.demandTabbedPaneListAndMatrix.get(layer).addTab("Traffic matrix view", trafficMatrixComponent.get(layer));
                    subpaneThisLayer.addTab(ajType.getTabName(), demandTabbedPaneListAndMatrix.get(layer));
            	}
            	else
            	{
                	final Pair<AdvancedJTable_abstractElement, FilteredTablePanel> component = createPanelComponentInfo(ajType , layer);//createPanelComponentInfo(ajType); 
                	netPlanViewTable.get(layer).put(ajType, component);
                	subpaneThisLayer.addTab(ajType.getTabName(), component.getSecond());
            	}
            }
        	netPlanView.addTab(layer.getName().equals("")? "Layer " + layer.getIndex() : layer.getName() , subpaneThisLayer);
        }
        
        /* Recover hide columns, decimal formats and attribute collapsed */
    	for (Map.Entry<NetworkLayer, Map<AJTableType, Pair<AdvancedJTable_abstractElement, FilteredTablePanel>>> entry : netPlanViewTable.entrySet())
    	{
    		final NetworkLayer layer = entry.getKey();
    		for (AJTableType ajTableType : entry.getValue().keySet())
    		{
    			if (ajTableType == AJTableType.LAYERS) continue;
    			
    			final AdvancedJTable_abstractElement table = entry.getValue().get(ajTableType).getFirst();
    			if (hideColumnStatePerTable.containsKey(layer)) 
    				table.setColumnShowHideValueByHeaderMap(hideColumnStatePerTable.get(layer).get(ajTableType));
    			if (columnDoubleFormatStatePerTable.containsKey(layer)) 
    				table.setColumnNumberOfDecimalsByHeaderMap(columnDoubleFormatStatePerTable.get(layer).get(ajTableType));
    			if (table instanceof AdvancedJTable_networkElement && attributeCollapsedStatePerTable.containsKey(layer))
    				((AdvancedJTable_networkElement) table).setAttributesAreCollapsedInOneColumn(attributeCollapsedStatePerTable.get(layer).get(ajTableType));
    		}
    	}
    	netPlanView.setSelectedIndex(selectedIndexFirstLevel);
    	if (netPlanView.getSelectedComponent() instanceof JTabbedPane && selectedIndexSecondLevel >= 0)
    		((JTabbedPane) netPlanView.getSelectedComponent()).setSelectedIndex(selectedIndexSecondLevel);
    	
    }
}
