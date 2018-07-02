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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

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
import com.net2plan.gui.utils.StringLabeller;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.utils.StringUtils;

/**
 * Created by César on 13/12/2016.
 */
@SuppressWarnings({ "unchecked", "serial" })
public class AdvancedJTable_resource extends AdvancedJTable_networkElement<Resource>
{

    public AdvancedJTable_resource(GUINetworkDesign callback , NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.RESOURCES , layerThisTable , true , r->!r.iAttachedToANode()? null : (r.getHostNode().get().isDown()? Color.RED : null));
    }

    @Override
  public List<AjtColumnInfo<Resource>> getNonBasicUserDefinedColumnsVisibleOrNot()
  {
		final NetworkLayer layer = this.getTableNetworkLayer();
		final List<AjtColumnInfo<Resource>> res = new LinkedList<> ();
      res.add(new AjtColumnInfo<Resource>(this , String.class, null , "Name", "Resource name", (d,val)->d.setName((String) val), d->d.getName() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Resource>(this , String.class, null , "Type", "Resource type", null , d->d.getType() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Resource>(this , Boolean.class, null , "Up?", "If the resource is up or down (just the state of its hosting node)", null , d->d.iAttachedToANode()? d.getHostNode().get().isUp() : true , AGTYPE.COUNTTRUE , r->!r.iAttachedToANode()? null : r.getHostNode().get().isDown()? Color.RED : null));
      res.add(new AjtColumnInfo<Resource>(this , Node.class, null , "Host node", "The node hosting this resource", null , d->d.getHostNode() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Resource>(this , Double.class, null , "Capacity", "The current capacity of the resource", null , d->d.getCapacity() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Resource>(this , Double.class, null , "Occupied capacity", "The current occupied capacity of the resource", null , d->d.getOccupiedCapacity() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Resource>(this , Double.class, null , "Utilization", "The current utilization of the resource", null , d->d.getUtilization() , AGTYPE.NOAGGREGATION , d-> { final double u = d.getUtilization(); if (u == 1) return Color.YELLOW; return u > 1? Color.RED : null;  }  ));
      res.add(new AjtColumnInfo<Resource>(this , String.class, null , "Cap. Units", "The units in which the resource capacity is measured", (d,val)->d.setCapacityMeasurementUnits((String)val) , d->d.getCapacityMeasurementUnits() , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Resource>(this , String.class, null , "Base resources", "The base resources that this resource relies on, and the amount of capacity consumed in each base resource", null , d->d.getCapacityOccupiedInBaseResourcesMap().entrySet().stream().map(ee->"(" + ee.getKey().getType() + "," + ee.getValue()).collect(Collectors.joining(",")) , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Resource>(this , String.class, null , "Upper resources", "The upper resources that rely on me, and the amount of capacity each one is occupying in me", null , d->d.getCapacityOccupiedByUpperResourcesMap().entrySet().stream().map(ee->"(" + ee.getKey().getType() + "," + ee.getValue()).collect(Collectors.joining(",")) , AGTYPE.NOAGGREGATION , null));
      res.add(new AjtColumnInfo<Resource>(this , Collection.class, null , "Trav. routes", "The routes in this layer that traverse this resource", null , d->d.getTraversingRoutes().stream().filter(r->r.getLayer().equals(layer)).collect(Collectors.toList()) , AGTYPE.SUMCOLLECTIONCOUNT , null));
      res.add(new AjtColumnInfo<Resource>(this , Double.class, null , "Processing time (ms)", "The processing time associated to added as delay to all the traffic units traversing this resource", (d,val)->d.setProcessingTimeToTraversingTrafficInMs((Double) val) , d->d.getProcessingTimeToTraversingTrafficInMs() , AGTYPE.MAXDOUBLE , null));

      return res;
  }

    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
    	final NetPlan np = callback.getDesign();
        final List<AjtRcMenu> res = new ArrayList<> ();
        res.add(new AjtRcMenu("Add resource", e->
        {
            JComboBox<StringLabeller> hostNodeSelector = new WiderJComboBox();
            JTextField capUnitsField = new JTextField(20);
            JTextField typeSelector = new JTextField(20);

            for (Node n : np.getNodes())
            {
                final String nodeName = n.getName();
                String nodeLabel = "Node " + n.getIndex();
                if (!nodeName.isEmpty()) nodeLabel += " (" + nodeName + ")";
                hostNodeSelector.addItem(StringLabeller.of(n.getId(), nodeLabel));
            }
            Node hostNode;
            String capacityUnits;
            String resType;
            JPanel pane = new JPanel();
            pane.add(new JLabel("Resource Type"));
            pane.add(typeSelector);
            pane.add(Box.createHorizontalStrut(30));
            pane.add(new JLabel("Host Node"));
            pane.add(hostNodeSelector);
            pane.add(Box.createHorizontalStrut(30));
            pane.add(new JLabel("Capacity Units"));
            pane.add(capUnitsField);

            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter parameters for the new resource" , JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
            hostNode = np.getNodeFromId((Long) ((StringLabeller) hostNodeSelector.getSelectedItem()).getObject());
            capacityUnits = capUnitsField.getText();
            resType = typeSelector.getText();

            JPanel panel = new JPanel();
            Object[][] data = {null, null, null};
            String[] headers = StringUtils.arrayOf("Base Resource", "Is Base Resource", "Capacity");
            TableModel tm = new ClassAwareTableModelImpl(data, headers, new HashSet<Integer>(Arrays.asList(1, 2)));
            AdvancedJTable table = new AdvancedJTable(tm);
            int baseResCounter = 0;
            for (Resource r : np.getResources())
            {
            	if (!r.iAttachedToANode()) continue;
                if (r.getHostNode().toString().equals(hostNode.toString()))
                    baseResCounter++;
            }
            Object[][] newData = new Object[baseResCounter][headers.length];
            int counter = 0;
            for (Resource r : np.getResources())
            {
                if (r.getHostNode().toString().equals(hostNode.toString()))
                {
                    newData[counter][0] = r.getName();
                    newData[counter][1] = false;
                    newData[counter][2] = 0;
                    addCheckboxCellEditor(false, counter, 1, table);
                    counter++;
                }
            }

            panel.setLayout(new BorderLayout());
            panel.add(new JLabel("Set new resource base resources"), BorderLayout.NORTH);
            panel.add(new JScrollPane(table), BorderLayout.CENTER);
            ((DefaultTableModel) table.getModel()).setDataVector(newData, headers);
            int option = JOptionPane.showConfirmDialog(null, panel, "Set base resources", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (option != JOptionPane.OK_OPTION) return;
            Map<Resource, Double> newBaseResources = new HashMap<>();
            for (int j = 0; j < table.getRowCount(); j++)
            {
                Resource r = np.getResource(j);
                String capacity = table.getModel().getValueAt(j, 2).toString();
                boolean isBaseResource = (boolean) table.getModel().getValueAt(j, 1);
                if (isBaseResource)
                    newBaseResources.put(r, Double.parseDouble(capacity));

            }
            np.addResource(resType, "Resource " + np.getResources().size(), Optional.of(hostNode),
                    0, capacityUnits, newBaseResources, 0, null);
        }, (a,b)->true, null));

        res.add(new AjtRcMenu("Add unatacched resource", e->
        {
            JTextField capUnitsField = new JTextField(20);
            JTextField typeSelector = new JTextField(20);
            String capacityUnits;
            String resType;
            JPanel pane = new JPanel();
            pane.add(new JLabel("Resource Type"));
            pane.add(typeSelector);
            pane.add(Box.createHorizontalStrut(30));
            pane.add(new JLabel("Capacity Units"));
            pane.add(capUnitsField);

            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter parameters for the new resource" , JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
            capacityUnits = capUnitsField.getText();
            resType = typeSelector.getText();
            np.addResource(resType, "Resource " + np.getResources().size(), Optional.empty(),0, capacityUnits, null, 0, null);
        } , (a,b)->b==1, null));

        res.add(new AjtRcMenu("Remove selected resources", e->getSelectedElements().forEach(dd->((Resource)dd).remove()) , (a,b)->b>0, null));
        res.add(new AjtRcMenu("Set capacity to selected resources", e->
        {
        	if (getSelectedElements().stream().anyMatch(r->r.isHavingMoreThanOneBaseResourceWithTheSameType ())) throw new Net2PlanException ("This option is not applicable if a resource has two base resources of the same type");
        	final List<String> baseResourcesTypes = new ArrayList<> (getSelectedElements().first().getBaseResources().stream().map(r->r.getType()).collect(Collectors.toCollection(TreeSet::new)));
        	final List<MtnInputForDialog<?>> dialogsCapacityAndBaseResources = new ArrayList<> ();
        	dialogsCapacityAndBaseResources.add(MtnInputForDialog.inputTfDouble("Resource capacity" , "Introduce the value to set as the resource capacity", 10, 0.0));
        	for (String baseResourceType : baseResourcesTypes)
            	dialogsCapacityAndBaseResources.add(MtnInputForDialog.inputTfDouble("Occupied capacity in resources of type '" + baseResourceType + "'" , "Introduce the value to set as the occupied capacity in the resource of the given type", 10, 0.0));
            MtnDialogBuilder.launch(
            		"Set capacity of selected resources", 
                    "Please introduce the capacity for the resources, and the base resources (if any).", 
                    "", 
                    this, 
                    dialogsCapacityAndBaseResources,
                    (list)->
                    	{
                    		final double resourceCapacity = (Double) list.get(0).get();
                    		for (Resource resource : getSelectedElements())
                    		{
                        		final Map<Resource,Double> newCapacityIOccupyInBaseResourcesMap = new HashMap<> ();
                        		for (int cont = 0; cont < baseResourcesTypes.size() ; cont ++)
                        		{
                        			final String baseResourceType = baseResourcesTypes.get(cont);
                        			final double capacityOccupied = (Double) list.get(1+cont).get();
                        			final Resource br = resource.getBaseResources().stream().filter(rr->rr.getType().equals(baseResourceType)).findFirst().orElse(null);
                        			if (br == null) throw new RuntimeException ("Unexpected error");
                        			newCapacityIOccupyInBaseResourcesMap.put(br, capacityOccupied);
                        		}
                        		resource.setCapacity(resourceCapacity, newCapacityIOccupyInBaseResourcesMap);
                    		}
                    	}
                    );
        } , (a,b)->b>1, null));

        res.add(new AjtRcMenu("Set capacity to selected resource", e->
        {
        	final Resource resource = getSelectedElements().first();
        	final List<Resource> baseResources = new ArrayList<> (resource.getBaseResources());
        	final List<MtnInputForDialog<?>> dialogsCapacityAndBaseResources = new ArrayList<> ();
        	dialogsCapacityAndBaseResources.add(MtnInputForDialog.inputTfDouble("Resource capacity" , "Introduce the value to set as the resource capacity", 10, 0.0));
        	for (Resource br : baseResources)
            	dialogsCapacityAndBaseResources.add(MtnInputForDialog.inputTfDouble("Occupied capacity in resources " + br.getName () + " of type '" + br.getType() + "'" , "Introduce the value to set as the occupied capacity in the resource", 10, 0.0));
            MtnDialogBuilder.launch(
            		"Set capacity of selected resource", 
                    "Please introduce the capacity for the resource, and the base resources (if any).", 
                    "", 
                    this, 
                    dialogsCapacityAndBaseResources,
                    (list)->
                    	{
                    		final double resourceCapacity = (Double) list.get(0).get();
                    		final Map<Resource,Double> newCapacityIOccupyInBaseResourcesMap = new HashMap<> ();
                    		for (int cont = 0; cont < baseResources.size() ; cont ++)
                    		{
                    			final Resource baseResource = baseResources.get(cont);
                    			final double capacityOccupied = (Double) list.get(1+cont).get();
                    			newCapacityIOccupyInBaseResourcesMap.put(baseResource, capacityOccupied);
                    		}
                    		resource.setCapacity(resourceCapacity, newCapacityIOccupyInBaseResourcesMap);
                    	}
                    );
        } , (a,b)->b==1, null));
        
        res.add(new AjtRcMenu("Set selected resources processing time", e->
        {
            MtnDialogBuilder.launch(
            		"Set processing time (ms) of selected resource", 
                    "Please introduce the processing time in miliseconds for the resources.", 
                    "", 
                    this, 
                    Arrays.asList(MtnInputForDialog.inputTfDouble("Processing time (ms)", "Introduce the processing time", 10, 0.0)),
                    (list)->
                    	{
                    		final double newProcTime = (Double) list.get(0).get();
                    		getSelectedElements().stream().forEach(ee->ee.setProcessingTimeToTraversingTrafficInMs(newProcTime));
                    	}
                    );
        } , (a,b)->b==1, null));
        
        return res;
    }

    
    private class ClassAwareTableModelImpl extends ClassAwareTableModel
    {
        private final Set<Integer> editableColumns;

        public  ClassAwareTableModelImpl (Object[][] dataVector, Object[] columnIdentifiers, Set<Integer> editableColumns)
        {
            super(dataVector, columnIdentifiers);
            this.editableColumns = editableColumns;
        }


        @Override
        public Class getColumnClass(int col)
        {
            return Object.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return (this.editableColumns.contains(columnIndex));
        }

        @Override
        public void setValueAt(Object value, int row, int column)
        {
            super.setValueAt(value, row, column);

        }
    }

    private void addCheckboxCellEditor(boolean defaultValue, int rowIndex, int columnIndex, AdvancedJTable table)
    {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setHorizontalAlignment(JLabel.CENTER);
        checkBox.setSelected(defaultValue);
        table.setCellEditor(rowIndex, columnIndex, new DefaultCellEditor(checkBox));
        table.setCellRenderer(rowIndex, columnIndex, new CheckBoxRenderer());
    }

    private static class CheckBoxRenderer extends JCheckBox implements TableCellRenderer
    {
        public CheckBoxRenderer()
        {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            if (isSelected)
            {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else
            {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }

            setSelected(value != null && Boolean.parseBoolean(value.toString()));
            return this;
        }

    }

    private List<Resource> getVisibleElementsInTable()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final NetworkLayer layer = getTableNetworkLayer();
        return rf == null ? callback.getDesign().getResources() : rf.getVisibleResources(layer);
    }
}
