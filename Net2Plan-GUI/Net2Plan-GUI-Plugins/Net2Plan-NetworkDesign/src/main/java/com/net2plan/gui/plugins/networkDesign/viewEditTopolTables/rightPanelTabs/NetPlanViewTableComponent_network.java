/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/
package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.rightPanelTabs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneLayout;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_layer;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.ColumnHeaderToolTips;
import com.net2plan.gui.utils.FullScrollPaneLayout;
import com.net2plan.gui.utils.TableCursorNavigation;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.StringUtils;

import net.miginfocom.swing.MigLayout;

public class NetPlanViewTableComponent_network extends JPanel {
    private final static String[] attributeTableHeader = StringUtils.arrayOf("Attribute", "Value");
    private final static String[] attributeTableTips = attributeTableHeader;

    private final static String[] tagTableHeader = StringUtils.arrayOf("Tag");
    private final static String[] tagTableTip = StringUtils.arrayOf("Name of the tag");

    private JTextField txt_networkName, txt_numLayers, txt_numNodes, txt_numSRGs , txt_currentDate;
    private JButton updateTables;
    private JTextArea txt_networkDescription;
    private AdvancedJTable networkTagTable;
    private AdvancedJTable networkAttributeTable;
    private AdvancedJTable_layer layerTable;
    private final GUINetworkDesign networkViewer;

    public NetPlanViewTableComponent_network(final GUINetworkDesign networkViewer, AdvancedJTable_layer layerTable) {
        super(new MigLayout("", "[][grow]", "[][][grow][][][][][grow]"));

        this.layerTable = layerTable;
        this.networkViewer = networkViewer;
        updateTables = new JButton ("Update");
        updateTables.setEnabled(true);
        updateTables.addActionListener(new ActionListener() 
        {
			@Override
			public void actionPerformed(ActionEvent e) 
			{
        		final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
            	try
            	{
            		final Date date = df.parse(txt_currentDate.getText());
            		if (!date.equals(networkViewer.getDesign().getCurrentDate())) 
            		{
                        networkViewer.getDesign().setCurrentDate(date);
                        networkViewer.updateVisualizationJustTables();
            		}
            	} catch (Exception ee) { txt_currentDate.setText(df.format(networkViewer.getDesign().getCurrentDate()));}
			}
		});
        txt_networkName = new JTextField();
        txt_networkDescription = new JTextArea();
        txt_networkDescription.setFont(new JLabel().getFont());
        txt_networkDescription.setLineWrap(true);
        txt_networkDescription.setWrapStyleWord(true);
        txt_networkName.setEditable(networkViewer.getVisualizationState().isNetPlanEditable());
        txt_networkDescription.setEditable(networkViewer.getVisualizationState().isNetPlanEditable());
        txt_currentDate = new JTextField();
        txt_currentDate.setEditable(networkViewer.getVisualizationState().isNetPlanEditable());
        txt_numLayers = new JTextField();
        txt_numLayers.setEditable(false);
        txt_numNodes = new JTextField();
        txt_numNodes.setEditable(false);
        txt_numSRGs = new JTextField();
        txt_numSRGs.setEditable(false);

        if (networkViewer.getVisualizationState().isNetPlanEditable()) {
            txt_networkName.getDocument().addDocumentListener(new DocumentAdapter(networkViewer) {
                @Override
                protected void updateInfo(String text) {
                    networkViewer.getDesign().setName(text);
                }
            });

            txt_networkDescription.getDocument().addDocumentListener(new DocumentAdapter(networkViewer) {
                @Override
                protected void updateInfo(String text) {
                    networkViewer.getDesign().setDescription(text);
                }
            });
//            txt_currentDate.getDocument().addDocumentListener(new DocumentAdapter(networkViewer) 
//            {
//                @Override
//                protected void updateInfo(String text) 
//                {
//                	try
//                	{
//                		if (!inTheMiddleOfUpdateOfCurrentDate)
//                		{
//                			inTheMiddleOfUpdateOfCurrentDate = true;
//                    		final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
//                    		final Date date = df.parse(text);
//                    		if (!date.equals(networkViewer.getDesign().getCurrentDate())) 
//                    		{
//                                networkViewer.getDesign().setCurrentDate(date);
//                                networkViewer.updateVisualizationJustTables();
//                    		}
//                            inTheMiddleOfUpdateOfCurrentDate = false;
//                		}
//                	} catch (Exception ee) {}
//                }
//            });
        }

        networkTagTable = new AdvancedJTable(new ClassAwareTableModel(new Object[1][tagTableHeader.length], tagTableHeader));

        ColumnHeaderToolTips tagTips = new ColumnHeaderToolTips();
        for (int c = 0; c < tagTableHeader.length; c++) {
            TableColumn col = networkTagTable.getColumnModel().getColumn(c);
            tagTips.setToolTip(col, tagTableTip[c]);
        }

        networkTagTable.getTableHeader().addMouseMotionListener(tagTips);
        networkTagTable.setAutoCreateRowSorter(true);

        if (networkViewer.getVisualizationState().isNetPlanEditable())
        {
            networkTagTable.addMouseListener(new SingleElementTagEditor(networkViewer, NetworkElementType.NETWORK));
        }

        JScrollPane sp_tags = new JScrollPane(networkTagTable);
        ScrollPaneLayout tagLayout = new FullScrollPaneLayout();
        sp_tags.setLayout(tagLayout);
        sp_tags.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        networkAttributeTable = new AdvancedJTable(new ClassAwareTableModel(new Object[1][attributeTableHeader.length], attributeTableHeader));
        if (networkViewer.getVisualizationState().isNetPlanEditable()) {
            networkAttributeTable.addMouseListener(new SingleElementAttributeEditor(networkViewer, NetworkElementType.NETWORK));
        }

        ColumnHeaderToolTips tips = new ColumnHeaderToolTips();
        for (int c = 0; c < attributeTableHeader.length; c++) {
            TableColumn col = networkAttributeTable.getColumnModel().getColumn(c);
            tips.setToolTip(col, attributeTableTips[c]);
        }

        networkAttributeTable.getTableHeader().addMouseMotionListener(tips);
        networkAttributeTable.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(networkAttributeTable);
        ScrollPaneLayout layout = new FullScrollPaneLayout();
        scrollPane.setLayout(layout);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        this.add(new JLabel("Name"));
        this.add(txt_networkName, "grow, wrap");
        this.add(new JLabel("Description"), "aligny top");
        this.add(new JScrollPane(txt_networkDescription), "grow, wrap, height 100::");
        this.add(sp_tags, "grow, spanx, wrap");
        this.add(scrollPane, "grow, spanx 2, wrap");
        
        this.add(new JLabel("Current date (yyyy-MM-dd HH:mm:ss)"), "grow");
        final JPanel auxPanel = new JPanel (new BorderLayout()); 
        auxPanel.add(txt_currentDate , BorderLayout.CENTER); 
        auxPanel.add(updateTables, BorderLayout.EAST);
        this.add(auxPanel, "grow, wrap"); 
//        this.add(new JLabel("Click this button to update the tables"), "grow");
//        this.add(updateTables, "grow, wrap");
        this.add(new JLabel("Number of layers"), "grow");
        this.add(txt_numLayers, "grow, wrap");
        this.add(new JLabel("Number of nodes"), "grow");
        this.add(txt_numNodes, "grow, wrap");
        this.add(new JLabel("Number of SRGs"), "grow");
        this.add(txt_numSRGs, "grow, wrap");
        this.add(new JLabel("Layer information"), "grow, spanx2, wrap");
        this.add(layerTable.getTableScrollPane(), "grow, spanx 2");
        networkAttributeTable.addKeyListener(new TableCursorNavigation());
    }


    // GETDECORATOR

    public void updateNetPlanView(NetPlan currentState) {
        txt_numLayers.setText(Integer.toString(currentState.getNumberOfLayers()));
        txt_numNodes.setText(Integer.toString(currentState.getNumberOfNodes()));
        txt_numSRGs.setText(Integer.toString(currentState.getNumberOfSRGs()));

        networkAttributeTable.setEnabled(false);
        ((DefaultTableModel) networkAttributeTable.getModel()).setDataVector(new Object[1][attributeTableHeader.length], attributeTableHeader);
        networkTagTable.setEnabled(false);
        ((DefaultTableModel) networkTagTable.getModel()).setDataVector(new Object[1][tagTableHeader.length], tagTableHeader);

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

        // Tag data
        final Set<String> layerTags = currentState.getTags();
        final String[] tagArray = layerTags.toArray(new String[layerTags.size()]);

        if (!(tagArray.length == 0))
        {
            final Object[][] tagData = new Object[tagArray.length][1];
            for (int i = 0; i < tagData.length; i++)
            {
                tagData[i][0] = tagArray[i];
            }
            ((DefaultTableModel) networkTagTable.getModel()).setDataVector(tagData, tagTableHeader);
        }

        this.layerTable.updateView();
        
		final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		if (!txt_currentDate.getText().equals(df.format(currentState.getCurrentDate()))) 
			txt_currentDate.setText(df.format(currentState.getCurrentDate()));
        txt_networkName.setText(currentState.getName());
        txt_networkDescription.setText(currentState.getDescription());
        txt_networkDescription.setCaretPosition(0);
    }

    // GETTABLE
}
