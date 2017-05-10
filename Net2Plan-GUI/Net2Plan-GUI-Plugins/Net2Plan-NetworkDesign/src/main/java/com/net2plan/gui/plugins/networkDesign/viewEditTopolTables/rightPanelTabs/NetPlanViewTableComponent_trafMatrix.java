package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.rightPanelTabs;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.collections15.BidiMap;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.CellRenderers;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.plugins.networkDesign.whatIfAnalysisPane.WhatIfAnalysisPane;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.TabIcon;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.Pair;

import net.miginfocom.swing.MigLayout;

public class NetPlanViewTableComponent_trafMatrix extends JPanel
{
    private final static String TITLE = "Traffic matrix design";
    private final static int DEFAULT_NUMBER_OF_NODES = 4;
    private final static TabIcon CLOSE_TAB_ICON = new TabIcon(TabIcon.IconType.TIMES_SIGN);

    private JTable trafficMatrix;

//    private final ArrayList<Node> filteredNodes;
//    private final Set<Demand> filteredDemands;
    private final JCheckBox filterOutNodesNotConnectedThisLayer;
    private final JComboBox<String> cmb_tagNodesSelector;
    private final JComboBox<String> cmb_tagDemandsSelector;
    private final JComboBox<String> cmb_trafficModelPattern;
    private final JComboBox<String> cmb_trafficNormalization;
    
    private Map<Pair<Node,Node>,Set<Demand>> filteredDemandView;
    private String demandTagFilter;
    private String nodeTagFilter;
    
    

    private final GUINetworkDesign networkViewer;

    public NetPlanViewTableComponent_trafMatrix(final GUINetworkDesign networkViewer)
    {
        super(new MigLayout("", "[grow]", "[][][][][][grow]"));
        this.networkViewer = networkViewer;
        final NetPlan np = networkViewer.getDesign();

//        this.filteredNodes = new ArrayList<> (np.getNodes());
//        this.filteredDemands = new HashSet<> (np.getDemands());
//        

        this.add(new JLabel ("Filter information for the traffic matrix"));
        this.filterOutNodesNotConnectedThisLayer = new JCheckBox("Filter out nodes without links at this layer");
        this.add(filterOutNodesNotConnectedThisLayer ,"wrap");
        this.cmb_tagNodesSelector = new WiderJComboBox();
        this.cmb_tagDemandsSelector = new WiderJComboBox();
        this.add(new JLabel ("Consider only demands between nodes tagged by...") ,"wrap");
        this.add(cmb_tagNodesSelector ,"wrap");
        this.add(new JLabel ("Consider only demands tagged by...") ,"wrap");
        this.add(cmb_tagDemandsSelector ,"wrap");
        final JButton applyFilterButton = new JButton("Apply filters");
        this.add(applyFilterButton ,"wrap");
        applyFilterButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { updateNetPlanView(); }
		});
        
        
        this.trafficMatrix = new AdvancedJTable();//createTrafficMatrix(np, filteredNodes, filteredDemands));
        //this.createDefaultColumnsFromModel();
        trafficMatrix.setDefaultRenderer(Object.class, new TotalRowColumnRenderer());
        trafficMatrix.setDefaultRenderer(Double.class, new TotalRowColumnRenderer());
        trafficMatrix.setDefaultRenderer(Number.class, new TotalRowColumnRenderer());
        trafficMatrix.setDefaultRenderer(Integer.class, new TotalRowColumnRenderer());
        trafficMatrix.setDefaultRenderer(String.class, new TotalRowColumnRenderer());
        JScrollPane pane = new JScrollPane(trafficMatrix);
        this.add(pane , "wrap");

        JPanel pnl_trafficModel = new JPanel ();
        pnl_trafficModel.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Traffic matrix synthesis"));
        cmb_trafficModelPattern = new WiderJComboBox();
        cmb_trafficModelPattern.addItem("Select a method for synthesizing a matrix");
        cmb_trafficModelPattern.addItem("1. Constant");
        cmb_trafficModelPattern.addItem("2. Uniform (0, 10)");
        cmb_trafficModelPattern.addItem("3. 50% Uniform (0, 100) & 50% Uniform(0, 10)");
        cmb_trafficModelPattern.addItem("4. 25% Uniform (0, 100) & 75% Uniform(0, 10)");
        cmb_trafficModelPattern.addItem("5. Gravity model");
        cmb_trafficModelPattern.addItem("6. Population-distance model");
        cmb_trafficModelPattern.addItem("7. Reset");
        pnl_trafficModel.add(cmb_trafficModelPattern);
        final JButton applyTrafficModelButton = new JButton("Apply");
        applyTrafficModelButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { /* updateNetPlanView(); */ }
		});
        pnl_trafficModel.setLayout(new MigLayout("insets 0 0 0 0", "[grow][][]", "[grow]"));
        pnl_trafficModel.add(cmb_trafficModelPattern, "grow, wmin 50");
        pnl_trafficModel.add(applyTrafficModelButton);
        this.add(pnl_trafficModel , "wrap");

        JPanel pnl_normalization = new JPanel ();
        pnl_normalization.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Traffic normalization and adjustments"));
        cmb_trafficNormalization = new WiderJComboBox();
        cmb_trafficNormalization.addItem("Select a method");
        cmb_trafficNormalization.addItem("1. Make symmetric");
        cmb_trafficNormalization.addItem("2. Scale by a factor");
        cmb_trafficNormalization.addItem("3. Apply random variation (truncated gaussian)");
        cmb_trafficNormalization.addItem("4. Normalization: fit to given total traffic");
        cmb_trafficNormalization.addItem("5. Normalization: fit to given out traffic per node");
        cmb_trafficNormalization.addItem("6. Normalization: fit to given in traffic per node");
        cmb_trafficNormalization.addItem("7. Normalization: scale to theoretical maximum traffic");
        pnl_normalization.add(cmb_trafficNormalization);
        final JButton applyTrafficNormalizationButton = new JButton("Apply");
        applyTrafficModelButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { /* updateNetPlanView(); */ }
		});
        pnl_normalization.setLayout(new MigLayout("insets 0 0 0 0", "[grow][][]", "[grow]"));
        pnl_normalization.add(cmb_trafficNormalization, "grow, wmin 50");
        pnl_normalization.add(applyTrafficNormalizationButton);
        this.add(pnl_normalization , "wrap");

        updateNetPlanView();
    }

    public void updateNetPlanView()
    {
    	final NetPlan np = networkViewer.getDesign();
    	final List<Node> filteredNodes = new ArrayList<> (np.getNumberOfNodes()); 
    	final String filteringNodeTag = this.cmb_tagNodesSelector.getSelectedIndex() == 0? null : (String) this.cmb_tagNodesSelector.getSelectedItem();
    	final String filteringDemandTag = this.cmb_tagDemandsSelector.getSelectedIndex() == 0? null : (String) this.cmb_tagDemandsSelector.getSelectedItem();
    	for (Node n : np.getNodes())
    	{
    		if (filteringNodeTag != null)
    			if (!n.hasTag(filteringNodeTag)) continue;
    		if (this.filterOutNodesNotConnectedThisLayer.isSelected())
    			if (!n.getOutgoingLinksAllLayers().stream().anyMatch(e->e.getLayer().isDefaultLayer())
    				&& (!n.getIncomingLinksAllLayers().stream().anyMatch(e->e.getLayer().isDefaultLayer())))
    				continue;
    		filteredNodes.add(n);
    	}
    	Set<Demand> filteredDemands;
    	if (filteringDemandTag == null) filteredDemands = new HashSet<> (np.getDemands());
    	else 
    	{ 
    		filteredDemands = new HashSet<> (); 
        	for (Demand d : np.getDemands())
        		if (d.hasTag(filteringDemandTag)) filteredDemands.add(d);
    	}
        this.trafficMatrix.setModel(createTrafficMatrix(filteredNodes, filteredDemands));
        
        cmb_tagNodesSelector.removeAllItems();
        cmb_tagNodesSelector.addItem("[NO FILTER]");
        final Set<String> allTagsNodes = np.getNodes().stream().map(n->n.getTags()).flatMap(e->e.stream()).collect(Collectors.toSet());
        final List<String> allTagsNodesOrdered = allTagsNodes.stream().sorted().collect(Collectors.toList());
        for (String tag : allTagsNodesOrdered) this.cmb_tagNodesSelector.addItem(tag);
        cmb_tagDemandsSelector.removeAllItems();
        cmb_tagDemandsSelector.addItem("[NO FILTER]");
        final Set<String> allTagsDemands = np.getDemands().stream().map(n->n.getTags()).flatMap(e->e.stream()).collect(Collectors.toSet());
        final List<String> allTagsDemandsOrdered = allTagsDemands.stream().sorted().collect(Collectors.toList());
        for (String tag : allTagsDemandsOrdered) this.cmb_tagDemandsSelector.addItem(tag);

        
    }

    private DefaultTableModel createTrafficMatrix(List<Node> filteredNodes , Set<Demand> filteredDemands) 
    {
    	final NetPlan np = this.networkViewer.getDesign();
    	final int N = filteredNodes.size();
    	final NetworkLayer layer = np.getNetworkLayerDefault();
        String[] columnHeaders = new String[N + 2];
        Object[][] data = new Object[N + 1][N + 2];
        final Map<Node,Integer> nodeToIndexInFilteredListMap = new HashMap<> ();
        for (int cont = 0; cont < filteredNodes.size() ; cont ++) nodeToIndexInFilteredListMap.put(filteredNodes.get(cont), cont);

        for (int inNodeListIndex = 0; inNodeListIndex < N; inNodeListIndex++) 
        {
        	final Node n = filteredNodes.get(inNodeListIndex);
            String aux = n.getName().equals("")? "Node " + n.getIndex() : n.getName();
            columnHeaders[1 + inNodeListIndex] = aux;
            Arrays.fill(data[inNodeListIndex], 0.0);
            data[inNodeListIndex][0] = aux;
        }

        Arrays.fill(data[data.length - 1], 0.0);
        final int totalsRowIndex = N;
        final int totalsColumnIndex = N+1;
        double totalTraffic = 0;
        for (Demand d : filteredDemands)
        {
        	final int row = nodeToIndexInFilteredListMap.get(d.getIngressNode());
        	final int column = nodeToIndexInFilteredListMap.get(d.getEgressNode()) + 1;
            totalTraffic += d.getOfferedTraffic();
        	data [row][column] = ((Double) data [row][column]) + d.getOfferedTraffic();
        	data [totalsRowIndex][column] = ((Double) data [totalsRowIndex][column]) + d.getOfferedTraffic();
        	data [row][totalsColumnIndex] = ((Double) data [row][totalsColumnIndex]) + d.getOfferedTraffic();
        }
    	data [totalsRowIndex][totalsColumnIndex] = totalTraffic;
        
        data[data.length - 1][0] = "";

        columnHeaders[0] = "";
    	data [totalsRowIndex][0] = "Total";
        columnHeaders[columnHeaders.length - 1] = "Total";

        final DefaultTableModel model = new ClassAwareTableModel(data, columnHeaders) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) 
            {
                final int columnCount = getColumnCount();
                final int rowCount = getRowCount();
                if (column == 0 || column == columnCount - 1 || row == rowCount - 1 || row == column - 1) return false;
                final Node n1 = filteredNodes.get(row);
                final Node n2 = filteredNodes.get(column-1);
                final Set<Demand> applicableDemands = Sets.intersection(
                		np.getNodePairDemands(n1, n2, false, layer) , filteredDemands);
                if (applicableDemands.isEmpty()) return false;
                if (applicableDemands.size() > 1) return false;
                if (applicableDemands.iterator().next().isCoupled()) return false;
                return true;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column) 
            {
                Object oldValue = getValueAt(row, column);

                try
                {
                    if (Math.abs((double) newValue - (double) oldValue) < 1e-10) return;
                    final double newOfferedTraffic = (Double) newValue;
                    if (newOfferedTraffic < 0) throw new Net2PlanException ("Wrong traffic value");
                    final Node n1 = filteredNodes.get(row);
                    final Node n2 = filteredNodes.get(column-1);
                    final Set<Demand> applicableDemands = Sets.intersection(
                    		np.getNodePairDemands(n1, n2, false, layer) , filteredDemands);
                    final Demand demand = applicableDemands.iterator().next(); 
                    
                    if (networkViewer.getVisualizationState().isWhatIfAnalysisActive())
                    {
                        final WhatIfAnalysisPane whatIfPane = networkViewer.getWhatIfAnalysisPane();
                        synchronized (whatIfPane)
                        {
                            whatIfPane.whatIfDemandOfferedTrafficModified(demand, newOfferedTraffic);
                            if (whatIfPane.getLastWhatIfExecutionException() != null)
                                throw whatIfPane.getLastWhatIfExecutionException();
                            whatIfPane.wait(); // wait until the simulation ends
                            if (whatIfPane.getLastWhatIfExecutionException() != null)
                                throw whatIfPane.getLastWhatIfExecutionException();
                            final VisualizationState vs = networkViewer.getVisualizationState();
                            Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> res =
                                    vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<>(networkViewer.getDesign().getNetworkLayers()));
                            vs.setCanvasLayerVisibilityAndOrder(networkViewer.getDesign(), res.getFirst(), res.getSecond());
                            networkViewer.updateVisualizationAfterNewTopology();
                        }
                    } else
                    {
                        demand.setOfferedTraffic(newOfferedTraffic);
                        networkViewer.updateVisualizationAfterChanges(Collections.singleton(NetworkElementType.DEMAND));
                        networkViewer.getVisualizationState().pickElement(demand);
                        networkViewer.updateVisualizationAfterPick();
                        networkViewer.addNetPlanChange();
                    }
                    super.setValueAt(newValue, row, column);
                } catch (Throwable e) { ErrorHandling.showErrorDialog("Wrong traffic value"); return; }
            }
        };
        return model;
    }

    private static class TotalRowColumnRenderer extends CellRenderers.NumberCellRenderer 
    {
        private final static Color BACKGROUND_COLOR = new Color(200, 200, 200);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (row == table.getRowCount() - 1 || column == table.getColumnCount() - 1) {
                c.setBackground(BACKGROUND_COLOR);
                if (isSelected) c.setForeground(Color.BLACK);
            }
            return c;
        }
    };
    
}


/*
Demands:
•	Only of the active layer
•	Filter nodes:
o	Filter out nodes without links at this layer
o	Nodes with tag: XXX
•	Filter demands:
o	Demands with tag XXX
•	Print the traffic matrix, between the given nodes
o	Cell with 0 demands => non editable and 0
o	Cell with > 1 demands => non editable and amount sum
o	Cell with 1 demand, but coupled => non editable
o	Cell with 1 demand and non-coupled => editable
•	Fill:
o	Constant: only to editable
o	Uniform random: only to editable
o	Uniform skewed: only to editable
o	Gravity model: only to editable
o	Population distance: only to editable
•	Same normalization options as in ALT-2
o	Clear
o	Scale
o	Make symmetric
o	Total: non-applicable if coupled demands in the traffic matrix
o	Row: non-applicable if coupled demands in the traffic matrix
o	Columns: non-applicable if coupled demands in the traffic matrix
o	Max for this: non-applicable if coupled demands in the traffic matrix
•	Variate current matrix:
o	Apply CAGR: not applied to coupled demands
o	Apply random variation: not applied to coupled demands
o	Apply Gaussian variation: not applied to coupled demands
•	set of matrices from a seminal one: each is a N2P 

*/
