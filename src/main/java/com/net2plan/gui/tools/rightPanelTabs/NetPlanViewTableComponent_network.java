package com.net2plan.gui.tools.rightPanelTabs;

import com.net2plan.gui.tools.IGUINetworkViewer;
import com.net2plan.gui.tools.specificTables.AdvancedJTable_layer;
import com.net2plan.gui.utils.*;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.StringUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.util.Map;

public class NetPlanViewTableComponent_network extends JPanel {
    private final static String[] attributeTableHeader = StringUtils.arrayOf("Attribute", "Value");
    private final static String[] attributeTableTips = attributeTableHeader;

    private JTextField txt_networkName, txt_layerLinkCapacityUnits, txt_layerDemandTrafficUnits, txt_numLayers, txt_numNodes, txt_numSRGs;
    private JTextArea txt_networkDescription;
    private AdvancedJTable networkAttributeTable;
    private AdvancedJTable_layer layerTable;
    private JScrollPane scrollPane;
    private final IGUINetworkViewer networkViewer;

    public NetPlanViewTableComponent_network(final IGUINetworkViewer networkViewer, AdvancedJTable_layer layerTable) {
        super(new MigLayout("", "[][grow]", "[][][grow][][][][][grow]"));

        this.layerTable = layerTable;
        this.networkViewer = networkViewer;
        txt_networkName = new JTextField();
        txt_networkDescription = new JTextArea();
        txt_networkDescription.setFont(new JLabel().getFont());
        txt_networkDescription.setLineWrap(true);
        txt_networkDescription.setWrapStyleWord(true);
        txt_networkName.setEditable(networkViewer.isEditable());
        txt_networkDescription.setEditable(networkViewer.isEditable());
        txt_numLayers = new JTextField();
        txt_numLayers.setEditable(false);
        txt_numNodes = new JTextField();
        txt_numNodes.setEditable(false);
        txt_numSRGs = new JTextField();
        txt_numSRGs.setEditable(false);

        if (networkViewer.isEditable()) {
            txt_networkName.getDocument().addDocumentListener(networkViewer.new DocumentAdapter() {
                @Override
                protected void updateInfo(String text) {
                    networkViewer.getDesign().setNetworkName(text);
                }
            });

            txt_networkDescription.getDocument().addDocumentListener(networkViewer.new DocumentAdapter() {
                @Override
                protected void updateInfo(String text) {
                    networkViewer.getDesign().setNetworkDescription(text);
                }
            });
        }

        networkAttributeTable = new AdvancedJTable(new ClassAwareTableModel(new Object[1][attributeTableHeader.length], attributeTableHeader));
        if (networkViewer.isEditable()) {
            networkAttributeTable.addMouseListener(new IGUINetworkViewer.SingleElementAttributeEditor(networkViewer, NetworkElementType.NETWORK));
        }

        String[] columnTips = attributeTableTips;
        String[] columnHeader = attributeTableHeader;

        ColumnHeaderToolTips tips = new ColumnHeaderToolTips();
        for (int c = 0; c < columnHeader.length; c++) {
            TableColumn col = networkAttributeTable.getColumnModel().getColumn(c);
            tips.setToolTip(col, columnTips[c]);
        }

        networkAttributeTable.getTableHeader().addMouseMotionListener(tips);
        networkAttributeTable.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(networkAttributeTable);
        ScrollPaneLayout layout = new FullScrollPaneLayout();
        scrollPane.setLayout(layout);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

//		ColumnHeaderToolTips tips1 = new ColumnHeaderToolTips();
//		for (int c = 0; c < layerTable.getTableHeaders().length; c++)
//			tips1.setToolTip(layerTable.getColumnModel().getColumn(c), layerTable.getTableTips() [c]);
//		layerTable.getTableHeader().addMouseMotionListener(tips1);

//		if (networkViewer.allowShowInitialNetPlan()) layerTable.setRowSorter(new CurrentAndPlannedStateTableSorter(layerTable.getModel()));
//		else layerTable.setAutoCreateRowSorter(true);

        JScrollPane scrollPane1 = new JScrollPane(layerTable);
        ScrollPaneLayout layout1 = new FullScrollPaneLayout();
        scrollPane1.setLayout(layout1);
        scrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

//		layerTable.addMouseListener(new PopupMenuNetPlan(networkViewer, networkViewer.getTopologyPanel(), layerTable.getModel(), NetworkElementType.LAYER, networkViewer.isEditable()));
//
        FixedColumnDecorator decorator = new FixedColumnDecorator(scrollPane1, layerTable.getNumFixedLeftColumnsInDecoration());
        decorator.getFixedTable().getColumnModel().getColumn(0).setMinWidth(50);

//		netPlanViewTableComponent.put(NetworkElementType.LAYER, scrollPane1);

        this.add(new JLabel("Name"));
        this.add(txt_networkName, "grow, wrap");
        this.add(new JLabel("Description"), "aligny top");
        this.add(new JScrollPane(txt_networkDescription), "grow, wrap, height 100::");
        this.add(scrollPane, "grow, spanx 2, wrap");
        this.add(new JLabel("Number of layers"), "grow");
        this.add(txt_numLayers, "grow, wrap");
        this.add(new JLabel("Number of nodes"), "grow");
        this.add(txt_numNodes, "grow, wrap");
        this.add(new JLabel("Number of SRGs"), "grow");
        this.add(txt_numSRGs, "grow, wrap");
        this.add(new JLabel("Layer information"), "grow, spanx2, wrap");
        this.add(scrollPane1, "grow, spanx 2");
//		netPlanViewTableComponent.put(elementType, networkPane);
        networkAttributeTable.addKeyListener(new TableCursorNavigation());
    }


    // GETDECORATOR

    public void updateNetPlanView(NetPlan currentState) {
        txt_numLayers.setText(Integer.toString(currentState.getNumberOfLayers()));
        txt_numNodes.setText(Integer.toString(currentState.getNumberOfNodes()));
        txt_numSRGs.setText(Integer.toString(currentState.getNumberOfSRGs()));

        networkAttributeTable.setEnabled(false);
        ((DefaultTableModel) networkAttributeTable.getModel()).setDataVector(new Object[1][attributeTableHeader.length], attributeTableHeader);

        Map<String, String> networkAttributes = currentState.getAttributes();
        if (!networkAttributes.isEmpty()) {
            int networkAttributeId = 0;
            Object[][] networkData = new Object[networkAttributes.size()][2];
            for (Map.Entry<String, String> entry : networkAttributes.entrySet()) {
                networkData[networkAttributeId][0] = entry.getKey();
                networkData[networkAttributeId][1] = entry.getValue();
                networkAttributeId++;
            }

            ((DefaultTableModel) networkAttributeTable.getModel()).setDataVector(networkData, attributeTableHeader);
        }

        txt_networkName.setText(currentState.getNetworkName());
        txt_networkDescription.setText(currentState.getNetworkDescription());
        txt_networkDescription.setCaretPosition(0);
    }

    // GETTABLE
}
