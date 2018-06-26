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


package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.MtnDialogBuilder;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.MtnInputForDialog;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

/**
 */
@SuppressWarnings({ "unchecked", "serial" })
public class AdvancedJTable_srg extends AdvancedJTable_networkElement<SharedRiskGroup>
{
    public AdvancedJTable_srg(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.SRGS , layerThisTable , true , null);
    }

    @Override
  public List<AjtColumnInfo<SharedRiskGroup>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
    	final NetPlan np = callback.getDesign();
    	final NetworkLayer layer = this.getTableNetworkLayer();
      final List<AjtColumnInfo<SharedRiskGroup>> res = new LinkedList<> ();
      res.add(new AjtColumnInfo<SharedRiskGroup>(this , Collection.class, null , "Nodes", "The nodes belonging to this SRG", null , d->d.getNodes() , AGTYPE.SUMCOLLECTIONCOUNT , null));
      res.add(new AjtColumnInfo<SharedRiskGroup>(this , Collection.class, null , "Links", "The links in this layer belonging to this SRG", null , d->d.getLinks(layer) , AGTYPE.SUMCOLLECTIONCOUNT , null));
      res.add(new AjtColumnInfo<SharedRiskGroup>(this , Collection.class, null , "Links all layers", "The links in this layer or other layers belonging to this SRG", null , d->d.getLinksAllLayers() , AGTYPE.SUMCOLLECTIONCOUNT , null));
      res.add(new AjtColumnInfo<SharedRiskGroup>(this , Double.class, null , "MTTF (hours)" , "The average Mean-Time-To-Fail value measued in hours (the time since the element is repaired until it fails again)", (d,val)->d.setMeanTimeToFailInHours((Double) val), d->d.getMeanTimeToFailInHours() , AGTYPE.MAXDOUBLE , null));
      res.add(new AjtColumnInfo<SharedRiskGroup>(this , Double.class, null , "MTTR (hours)" , "The average Mean-Time-To-Repair value measued in hours (the time betweem the element fails, and is up again since it is repaired)", (d,val)->d.setMeanTimeToRepairInHours((Double) val), d->d.getMeanTimeToRepairInHours() , AGTYPE.MAXDOUBLE , null));
      res.add(new AjtColumnInfo<SharedRiskGroup>(this , Double.class, null , "Availability" , "The probability of findig the element not failed (MTTF / (MTTF + MTTR)) ", (d,val)->d.setMeanTimeToRepairInHours((Double) val), d->d.getMeanTimeToRepairInHours() , AGTYPE.MAXDOUBLE , null));
      res.add(new AjtColumnInfo<SharedRiskGroup>(this , Boolean.class, null , "Is dynamic SRG?" , "Indicates if the SRG is dnyamic (and then its belonging nodes and links may change)", null , d->d.isDynamicSrg() , AGTYPE.NOAGGREGATION , null));
      return res;
  }

    @Override
    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
    	final NetPlan np = callback.getDesign();
        final List<AjtRcMenu> res = new ArrayList<> ();
        res.add(new AjtRcMenu("Add SRG", e->np.addSRG(8748, 12, null), (a,b)->true, null));
        res.add(new AjtRcMenu("Remove selected SRGs", e->getSelectedElements().forEach(dd->((SharedRiskGroup)dd).remove()) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Add SRGs from model", e->
        {
            MtnDialogBuilder.launch(
            		"Add SRG from model", 
                    "Please introduce the information below.", 
                    "", 
                    this, 
                    Arrays.asList(
                    		MtnInputForDialog.inputTfCombo ("SRG creation scheme" , "Please introduce the scheme to follow when generating the SRGs" , 20 , SRGUtils.SharedRiskModel.PER_NODE ,Arrays.asList (SRGUtils.SharedRiskModel.values()) , Arrays.asList (SRGUtils.SharedRiskModel.values()).stream().map(ee->ee.toString ()).collect (Collectors.toList ()) ,null),
                    		MtnInputForDialog.inputTfDouble("MTTF (hours)", "Mean-Time-To-Fail in hours, to set for all the SRGs", 10, 365*24.0),
                    		MtnInputForDialog.inputTfDouble("MTTR (hours)", "Mean-Time-To-Repair in hours, to set for all the SRGs", 10, 12.0),
                    		MtnInputForDialog.inputCheckBox ("Remove existing SRGs?" , "Indicates if the existing SRGs should be removed before creating the new ones" , false , null)
                    		),
                    (list)->
                    	{
                    		final SRGUtils.SharedRiskModel srgModel = (SRGUtils.SharedRiskModel) list.get(0).get();
                    		final double mttf = (Double) list.get(1).get();
                    		final double mttr = (Double) list.get(2).get();
                    		final boolean removeExistingSRGs = (Boolean) list.get(3).get();
                            SRGUtils.configureSRGs(np, mttf, mttr, srgModel, removeExistingSRGs);
                    	}
                    );
        }, (a,b)->true, null));

        res.add(new AjtRcMenu("View/edit SRG", e->viewEditSRGGUI(callback, getSelectedElements ().first()) , (a,b)->b==1, null));
        res.add(new AjtRcMenu("Set MTTF to selected SRGs", e->
        {
            MtnDialogBuilder.launch(
            		"Set MTTF to selected SRGs", 
                    "Please introduce the MTTF value.", 
                    "", 
                    this, 
                    Arrays.asList(
                    		MtnInputForDialog.inputTfDouble("MTTF (hours)", "Mean-Time-To-Fail in hours, to set for all the SRGs", 10, 365*24.0)
                    		),
                    (list)->
                    	{
                    		final double mttf = (Double) list.get(0).get();
                    		getSelectedElements ().stream ().forEach (s->s.setMeanTimeToFailInHours (mttf));
                    	}
                    );
        }, (a,b)->b>0, null));
        res.add(new AjtRcMenu("Set MTTR to selected SRGs", e->
        {
            MtnDialogBuilder.launch(
            		"Set MTTR to selected SRGs", 
                    "Please introduce the MTTR value.", 
                    "", 
                    this, 
                    Arrays.asList(
                    		MtnInputForDialog.inputTfDouble("MTTR (hours)", "Mean-Time-To-Repair in hours, to set for all the SRGs", 10, 12.0)
                    		),
                    (list)->
                    	{
                    		final double mttf = (Double) list.get(0).get();
                    		getSelectedElements ().stream ().forEach (s->s.setMeanTimeToRepairInHours (mttf));
                    	}
                    );
        }, (a,b)->b>0, null));
        
        
        
        return res;
    }
    
    private static void viewEditSRGGUI(final GUINetworkDesign callback, final SharedRiskGroup srg)
    {
        assert srg != null;

        final NetPlan netPlan = callback.getDesign();

        long srgId = srg.getId();

        callback.putTransientColorInElementTopologyCanvas(srg.getNodes(), Color.ORANGE);
        callback.putTransientColorInElementTopologyCanvas(srg.getLinksAllLayers(), Color.ORANGE);

        final int N = netPlan.getNumberOfNodes();
        final int E = netPlan.getNumberOfLinks();
        final Object[][] nodeData = new Object[N == 0 ? 1 : N][3];
        final Object[][] linkData = new Object[E == 0 ? 1 : E][4];

        if (N > 0)
        {
            int n = 0;
            for (Node node : netPlan.getNodes())
            {
                nodeData[n] = new Object[3];
                nodeData[n][0] = node.getId();
                nodeData[n][1] = node.getName();
                nodeData[n][2] = srg.getNodes().contains(node);

                n++;
            }
        }

        if (E > 0)
        {
            int e = 0;
            for (Link link : netPlan.getLinks())
            {
                linkData[e] = new Object[4];
                linkData[e][0] = link.getId();
                linkData[e][1] = link.getOriginNode().getId() + (link.getOriginNode().getName().isEmpty() ? "" : " (" + link.getOriginNode().getName() + ")");
                linkData[e][2] = link.getDestinationNode().getId() + (link.getDestinationNode().getName().isEmpty() ? "" : " (" + link.getDestinationNode().getName() + ")");
                linkData[e][3] = srg.getLinksAllLayers().contains(link);

                e++;
            }
        }

        final DefaultTableModel nodeModel = new ClassAwareTableModel(nodeData, new String[]{"Id", "Name", "Included in the SRG"})
        {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return columnIndex == 2;
            }

            @Override
            public void setValueAt(Object aValue, int row, int column)
            {
                if (column == 2)
                {
                    boolean value = (boolean) aValue;
                    Node node = netPlan.getNodeFromId((long) getValueAt(row, 0));
                    if (value && !srg.getNodes().contains(node))
                    {
                        netPlan.getSRGFromId(srgId).addNode(node);
                        callback.putTransientColorInElementTopologyCanvas(srg.getNodes(), Color.ORANGE);
                        callback.putTransientColorInElementTopologyCanvas(srg.getLinksAllLayers(), Color.ORANGE);
                    } else if (!value && srg.getNodes().contains(node))
                    {
                        netPlan.getSRGFromId(srgId).removeNode(node);
                        callback.putTransientColorInElementTopologyCanvas(srg.getNodes(), Color.ORANGE);
                        callback.putTransientColorInElementTopologyCanvas(srg.getLinksAllLayers(), Color.ORANGE);
                    }
                }

                super.setValueAt(aValue, row, column);
            }
        };

        final DefaultTableModel linkModel = new ClassAwareTableModel(linkData, new String[]{"Id", "Origin node", "Destination node", "Included in the SRG"})
        {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return columnIndex == 3;
            }

            @Override
            public void setValueAt(Object aValue, int row, int column)
            {
                if (column == 3)
                {
                    boolean value = (boolean) aValue;
                    Link link = netPlan.getLinkFromId((long) getValueAt(row, 0));
                    if (value && !srg.getLinksAllLayers().contains(link))
                    {
                        srg.addLink(link);
                        callback.putTransientColorInElementTopologyCanvas(srg.getNodes(), Color.ORANGE);
                        callback.putTransientColorInElementTopologyCanvas(srg.getLinksAllLayers(), Color.ORANGE);
                    } else if (!value && srg.getLinksAllLayers().contains(link))
                    {
                        srg.removeLink(link);
                        callback.putTransientColorInElementTopologyCanvas(srg.getNodes(), Color.ORANGE);
                        callback.putTransientColorInElementTopologyCanvas(srg.getLinksAllLayers(), Color.ORANGE);
                    }
                }

                super.setValueAt(aValue, row, column);
            }
        };

        final JTable nodeTable = new AdvancedJTable(nodeModel);
        final JTable linkTable = new AdvancedJTable(linkModel);

//        nodeTable.setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
//        nodeTable.setDefaultRenderer(Double.class, new CellRenderers.UnfocusableCellRenderer());
//        nodeTable.setDefaultRenderer(Object.class, new CellRenderers.UnfocusableCellRenderer());
//        nodeTable.setDefaultRenderer(Float.class, new CellRenderers.UnfocusableCellRenderer());
//        nodeTable.setDefaultRenderer(Long.class, new CellRenderers.UnfocusableCellRenderer());
//        nodeTable.setDefaultRenderer(Integer.class, new CellRenderers.UnfocusableCellRenderer());
//        nodeTable.setDefaultRenderer(String.class, new CellRenderers.UnfocusableCellRenderer());
//
//        linkTable.setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
//        linkTable.setDefaultRenderer(Double.class, new CellRenderers.UnfocusableCellRenderer());
//        linkTable.setDefaultRenderer(Object.class, new CellRenderers.UnfocusableCellRenderer());
//        linkTable.setDefaultRenderer(Float.class, new CellRenderers.UnfocusableCellRenderer());
//        linkTable.setDefaultRenderer(Long.class, new CellRenderers.UnfocusableCellRenderer());
//        linkTable.setDefaultRenderer(Integer.class, new CellRenderers.UnfocusableCellRenderer());
//        linkTable.setDefaultRenderer(String.class, new CellRenderers.UnfocusableCellRenderer());

        JScrollPane nodeScrollPane = new JScrollPane(nodeTable);
        JScrollPane linkScrollPane = new JScrollPane(linkTable);


        final JDialog dialog = new JDialog();
        dialog.setLayout(new MigLayout("", "[grow]", "[][grow][][grow]"));
        dialog.add(new JLabel("Nodes"), "growx, wrap");
        dialog.add(nodeScrollPane, "grow, wrap");
        dialog.add(new JLabel("Links"), "growx, wrap");
        dialog.add(linkScrollPane, "grow");

        dialog.setTitle("View/edit SRG " + srgId);
        SwingUtils.configureCloseDialogOnEscape(dialog);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(new Dimension(500, 300));
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }


    private List<SharedRiskGroup> getVisibleElementsInTable()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final NetworkLayer layer = getTableNetworkLayer();
        return rf == null ? callback.getDesign().getSRGs() : rf.getVisibleSRGs(layer);
    }
}
