package com.net2plan.gui.utils.viewEditTopolTables.specificTables;

import com.net2plan.gui.utils.*;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.Constants;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.StringUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * Created by César on 13/12/2016.
 */
public class AdvancedJTable_resource extends AdvancedJTableNetworkElement {

    public static final int COLUMN_ID = 0;
    public static final int COLUMN_INDEX = 1;
    public static final int COLUMN_NAME = 2;
    public static final int COLUMN_TYPE = 3;
    public static final int COLUMN_HOSTNODE = 4;
    public static final int COLUMN_CAPACITY = 5;
    public static final int COLUMN_CAPACITYMUNITS= 6;
    public static final int COLUMN_OCCUPIEDCAPACITY = 7;
    public static final int COLUMN_TRAVERSINGROUTES = 8;
    public static final int COLUMN_UPPERRESOURCES = 9;
    public static final int COLUMN_BASERESOURCES = 10;
    public static final int COLUMN_PROCESSINGTIME = 11;
    public static final int COLUMN_ATTRIBUTES = 12;
    private static final String netPlanViewTabName = "Resources";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique Identifier","Index","Name","Type","Host Node","Capacity","Cap. Units","Ocuppied capacity","Traversing Routes","Upper Resources","Base Resources","Processing Time","Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique Identifier","Index","Name","Type","Host Node","Capacity","Cap. Units","Ocuppied capacity","Traversing Routes","Upper Resources","Base Resources","Processing Time","Attributes");
    private List<Resource> currentResources = new LinkedList<>();
    private NetPlan currentTopology = null;
    private final String[] resourceTypes = StringUtils.arrayOf("Firewall","NAT","CPU","RAM");

    public AdvancedJTable_resource(final INetworkCallback networkViewer)
    {
        super(createTableModel(networkViewer), networkViewer, Constants.NetworkElementType.RESOURCE, true);
        setDefaultCellRenderers(networkViewer);
        setSpecificCellRenderers();
        setColumnRowSorting(networkViewer.inOnlineSimulationMode());
        fixedTable.setRowSorter(this.getRowSorter());
        fixedTable.setDefaultRenderer(Boolean.class, this.getDefaultRenderer(Boolean.class));
        fixedTable.setDefaultRenderer(Double.class, this.getDefaultRenderer(Double.class));
        fixedTable.setDefaultRenderer(Object.class, this.getDefaultRenderer(Object.class));
        fixedTable.setDefaultRenderer(Float.class, this.getDefaultRenderer(Float.class));
        fixedTable.setDefaultRenderer(Long.class, this.getDefaultRenderer(Long.class));
        fixedTable.setDefaultRenderer(Integer.class, this.getDefaultRenderer(Integer.class));
        fixedTable.setDefaultRenderer(String.class, this.getDefaultRenderer(String.class));
        fixedTable.getTableHeader().setDefaultRenderer(new CellRenderers.FixedTableHeaderRenderer());
        setEnabled(true);
    }


    @Override
    public List<Object[]> getAllData(NetPlan currentState, TopologyPanel topologyPanel, NetPlan initialState, ArrayList<String> attributesTitles) {
        List<Object[]> allResourceData = new LinkedList<Object[]>();
        for (Resource res : currentState.getResources()) {


            Object[] resData = new Object[netPlanViewTableHeader.length + attributesTitles.size()];
            resData[0] = res.getId();
            resData[1] = res.getIndex();
            resData[2] = res.getName();
            resData[3] = res.getType();
            resData[4] = res.getHostNode();
            resData[5] = res.getCapacity();
            resData[6] = res.getCapacityMeasurementUnits();
            resData[7] = res.getOccupiedCapacity();
            resData[8] = joinTraversingRoutesWithTheirCapacities(res);
            resData[9] = joinUpperResourcesWithTheirCapacities(res);
            resData[10] = joinBaseResourcesWithTheirCapacities(res);
            resData[11] = res.getProcessingTimeToTraversingTrafficInMs();
            resData[12] = StringUtils.mapToString(res.getAttributes());

            for(int i = netPlanViewTableHeader.length; i < netPlanViewTableHeader.length + attributesTitles.size();i++)
            {
                if(res.getAttributes().containsKey(attributesTitles.get(i-netPlanViewTableHeader.length)))
                {
                    resData[i] = res.getAttribute(attributesTitles.get(i-netPlanViewTableHeader.length));
                }
            }

            allResourceData.add(resData);

            if (initialState != null && initialState.getNodeFromId(res.getId()) != null) {
                res = initialState.getResourceFromId(res.getId());

                Object[] resData_initialNetPlan = new Object[netPlanViewTableHeader.length + attributesTitles.size()];
                resData_initialNetPlan[0] = null;
                resData_initialNetPlan[1] = null;
                resData_initialNetPlan[2] = null;
                resData_initialNetPlan[3] = res.getType();
                resData_initialNetPlan[4] = res.getHostNode();
                resData_initialNetPlan[5] = res.getCapacity();
                resData_initialNetPlan[6] = res.getCapacityMeasurementUnits();
                resData_initialNetPlan[7] = res.getOccupiedCapacity();
                resData_initialNetPlan[8] = joinTraversingRoutesWithTheirCapacities(res);
                resData_initialNetPlan[9] = joinUpperResourcesWithTheirCapacities(res);
                resData_initialNetPlan[10] = joinBaseResourcesWithTheirCapacities(res);
                resData_initialNetPlan[11] = res.getProcessingTimeToTraversingTrafficInMs();
                resData_initialNetPlan[12] = StringUtils.mapToString(res.getAttributes());

                for(int i = netPlanViewTableHeader.length; i < netPlanViewTableHeader.length + attributesTitles.size();i++)
                {
                    if(res.getAttributes().containsKey(attributesTitles.get(i-netPlanViewTableHeader.length)))
                    {
                        resData_initialNetPlan[i] = res.getAttribute(attributesTitles.get(i - netPlanViewTableHeader.length));
                    }

                }

                allResourceData.add(resData_initialNetPlan);
            }
        }
        return allResourceData;
    }

    @Override
    public String getTabName() {
        return netPlanViewTabName;
    }

    @Override
    public String[] getTableHeaders() {
        return netPlanViewTableHeader;
    }

    @Override
    public String[] getCurrentTableHeaders() {
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

    @Override
    public String[] getTableTips() {
        return netPlanViewTableTips;
    }

    @Override
    public boolean hasElements(NetPlan np) {
        return networkViewer.getDesign().hasResources();
    }

    public boolean isTableEmpty(){
        return !networkViewer.getDesign().hasResources();
    }

    @Override
    public int getAttributesColumnIndex() {
        return COLUMN_ATTRIBUTES;
    }

    @Override
    public int[] getColumnsOfSpecialComparatorForSorting() {
        return new int[]{5};
    }

    private static TableModel createTableModel(final INetworkCallback networkViewer) {
        final TopologyPanel topologyPanel = networkViewer.getTopologyPanel();
        TableModel resourceTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (!networkViewer.isEditable()) return false;
                if( columnIndex >= netPlanViewTableHeader.length) return true;
                if (getValueAt(rowIndex,columnIndex) == null) return false;

                return columnIndex == COLUMN_NAME || columnIndex == COLUMN_CAPACITY || columnIndex == COLUMN_PROCESSINGTIME;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column) {
                Object oldValue = getValueAt(row, column);

				/* If value doesn't change, exit from function */
                if (newValue != null && newValue.equals(oldValue)) return;

                NetPlan netPlan = networkViewer.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long resId = (Long) getValueAt(row, 0);
                final Resource res = netPlan.getResourceFromId(resId);

                try {
                    switch (column) {
                        case COLUMN_NAME:
                            netPlan.getResourceFromId(resId).setName(newValue.toString());
                            topologyPanel.getCanvas().refresh();
                            break;

                        case COLUMN_CAPACITY:
                            if (newValue == null) return;
                            netPlan.getResourceFromId(resId).setCapacity((Double)newValue,  netPlan.getResourceFromId(resId).getCapacityOccupiedInBaseResourcesMap());
                            networkViewer.updateNetPlanView();
                            topologyPanel.getCanvas().refresh();
                            break;

                        case COLUMN_PROCESSINGTIME:
                            if(newValue == null) return;
                            netPlan.getResourceFromId(resId).setProcessingTimeToTraversingTrafficInMs((Double)newValue);
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying resource");
//					ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying node, Object newValue: " + newValue + ", int row: " + row + ", int column: " + column);
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };

        return resourceTableModel;
    }
    private void setSpecificCellRenderers() {
    }

    private void setDefaultCellRenderers(final INetworkCallback networkViewer) {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());
    }
    @Override
    public void setColumnRowSorting(boolean allowShowInitialNetPlan) {
        if (allowShowInitialNetPlan) setRowSorter(new CurrentAndPlannedStateTableSorter(getModel()));
        else setAutoCreateRowSorter(true);
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_NAME, new AdvancedJTableNetworkElement.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_CAPACITY, new AdvancedJTableNetworkElement.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_INDEX, new AdvancedJTableNetworkElement.ColumnComparator());
    }

    @Override
    public int getNumFixedLeftColumnsInDecoration() {
        return 2;
    }

    @Override
    public ArrayList<String> getAttributesColumnsHeaders() {
        ArrayList<String> attColumnsHeaders = new ArrayList<>();
        currentTopology = networkViewer.getDesign();
        currentResources = currentTopology.getResources();
        for(Resource res : currentResources)
        {

            for (Map.Entry<String, String> entry : res.getAttributes().entrySet())
            {
                if(attColumnsHeaders.contains(entry.getKey()) == false)
                {
                    attColumnsHeaders.add(entry.getKey());
                }

            }

        }

        return attColumnsHeaders;
    }

    private String joinTraversingRoutesWithTheirCapacities(Resource res)
    {
        Map<Route, Double> routesCapacities = res.getTraversingRouteOccupiedCapacityMap();
        String t = "";
        int counter = 0;
        for(Map.Entry<Route, Double> entry : routesCapacities.entrySet())
        {
            if(counter == routesCapacities.size() - 1)
                t = t + "Route "+entry.getKey().toString()+" ("+entry.getValue()+") ";
            else
                t = t + "Route "+entry.getKey().toString()+" ("+entry.getValue()+"), ";

            counter++;
        }

        return t;
    }

    private String joinUpperResourcesWithTheirCapacities(Resource res)
    {
        Map<Resource, Double> upperResourcesCapacities = res.getCapacityOccupiedByUpperResourcesMap();
        String t = "";
        int counter = 0;
        for(Map.Entry<Resource, Double> entry : upperResourcesCapacities.entrySet())
        {
            if(counter == upperResourcesCapacities.size() - 1)
                t = t + "Resource "+entry.getKey().toString()+" ("+entry.getValue()+") ";
            else
                t = t + "Resource "+entry.getKey().toString()+" ("+entry.getValue()+"), ";

            counter++;
        }

        return t;
    }

    private String joinBaseResourcesWithTheirCapacities(Resource res)
    {
        Map<Resource, Double> baseResourcesCapacities = res.getCapacityOccupiedInBaseResourcesMap();
        String t = "";
        int counter = 0;
        for(Map.Entry<Resource, Double> entry : baseResourcesCapacities.entrySet())
        {
            if(counter == baseResourcesCapacities.size() - 1)
                t = t + "Resource "+entry.getKey().toString()+" ("+entry.getValue()+") ";
            else
                t = t + "Resource "+entry.getKey().toString()+" ("+entry.getValue()+"), ";

            counter++;
        }

        return t;

    }
    @Override
    public void doPopup(MouseEvent e, int row, Object itemId) {

        JPopupMenu popup = new JPopupMenu();

        if (networkViewer.isEditable()) {
            popup.add(getAddOption());
            for (JComponent item : getExtraAddOptions())
                popup.add(item);
        }

        if (!isTableEmpty()) {
            if (networkViewer.isEditable()) {
                if (row != -1) {
                    if (popup.getSubElements().length > 0) popup.addSeparator();

                    JMenuItem removeItem = new JMenuItem("Remove " + networkElementType);
                    removeItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NetPlan netPlan = networkViewer.getDesign();
                            try {
                                networkViewer.getDesign().getResourceFromId((Long)itemId).remove();
                                networkViewer.updateNetPlanView();
                            } catch (Throwable ex) {
                                ErrorHandling.addErrorOrException(ex, getClass());
                                ErrorHandling.showErrorDialog("Unable to remove " + networkElementType);
                            }
                        }
                    });

                    popup.add(removeItem);
                    addPopupMenuAttributeOptions(e, row, itemId, popup);
                }
                JMenuItem removeItemsOfAType = new JMenuItem("Remove all "+networkElementType+"s of a type");
                removeItemsOfAType.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        NetPlan netPlan = networkViewer.getDesign();
                        JComboBox typeSelector = new WiderJComboBox();
                        for(int i = 0; i < resourceTypes.length; i++)
                        {
                            typeSelector.addItem(resourceTypes[i]);
                        }
                        try{
                            JPanel pane = new JPanel();
                            pane.add(new JLabel("Resource Type"));
                            pane.add(typeSelector);
                            while (true) {
                                int result = JOptionPane.showConfirmDialog(null, pane, "Please choose the type of "+ networkElementType+" to remove", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                                if (result != JOptionPane.OK_OPTION) return;
                                    String typeToRemove = typeSelector.getSelectedItem().toString();
                                    List<Resource> resourcesToRemove = new LinkedList<Resource>();
                                    for(Resource res : netPlan.getResources())
                                    {
                                        if(res.getType().equals(typeToRemove))
                                            resourcesToRemove.add(res);
                                    }

                                    for(Resource res : resourcesToRemove)
                                    {
                                        res.remove();
                                    }
                                networkViewer.updateNetPlanView();
                                break;
                            }
                        }catch (Throwable ex) {
                            ErrorHandling.addErrorOrException(ex, getClass());
                            ErrorHandling.showErrorDialog("Unable to remove " + networkElementType+"s");
                        }

                    }
                });
                popup.add(removeItemsOfAType);
                JMenuItem removeItems = new JMenuItem("Remove all " + networkElementType + "s");

                removeItems.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        NetPlan netPlan = networkViewer.getDesign();

                        try {
                            netPlan.removeAllResources();
                            networkViewer.updateNetPlanView();
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

    private JMenuItem getAddOption() {
        JMenuItem addItem = addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = networkViewer.getDesign();

                try {

                    JComboBox hostNodeSelector = new WiderJComboBox();
                    JTextField capUnitsField = new JTextField(20);
                    JComboBox typeSelector = new WiderJComboBox();
                    for(int i = 0; i < resourceTypes.length; i++)
                        typeSelector.addItem(resourceTypes[i]);

                    for (Node n : netPlan.getNodes()) {
                        final String nodeName = n.getName();
                        String nodeLabel = "Node " + n.getIndex();
                        if (!nodeName.isEmpty()) nodeLabel += " (" + nodeName + ")";
                        hostNodeSelector.addItem(StringLabeller.of(n.getId(), nodeLabel));
                    }
                    Node hostNode = null;
                    String capacityUnits = "";
                    String resType = "";
                    JPanel pane = new JPanel();
                    pane.add(new JLabel("Resource Type"));
                    pane.add(typeSelector);
                    pane.add(Box.createHorizontalStrut(30));
                    pane.add(new JLabel("Host Node"));
                    pane.add(hostNodeSelector);
                    pane.add(Box.createHorizontalStrut(30));
                    pane.add(new JLabel("Capacity Units"));
                    pane.add(capUnitsField);
                    while (true) {
                        int result = JOptionPane.showConfirmDialog(null, pane, "Please enter parameters for the new " + networkElementType, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (result != JOptionPane.OK_OPTION) return;
                        hostNode = netPlan.getNodeFromId((Long) ((StringLabeller) hostNodeSelector.getSelectedItem()).getObject());
                        capacityUnits = capUnitsField.getText();
                        resType = typeSelector.getSelectedItem().toString();
                        break;
                    }
                    JPanel panel = new JPanel();
                    Object [][] data = {null,null,null};
                    String [] headers = StringUtils.arrayOf("Base Resource","Is Base Resource","Capacity");
                    TableModel tm = new ClassAwareTableModelImpl(data,headers);
                    AdvancedJTable table = new AdvancedJTable(tm);
                    Object[][] newData = new Object[netPlan.getResources().size() - 1][headers.length];
                    int counter = 0;
                    for(Resource r : netPlan.getResources())
                    {
                        newData[counter][0] = r.getName();
                        newData[counter][1] = false;
                        newData[counter][2] = 0;
                        addCheckboxCellEditor(false,counter,1,table);
                        counter++;
                    }
                    panel.setLayout(new BorderLayout());
                    panel.add(new JLabel("Set new resource base resources"),BorderLayout.NORTH);
                    panel.add(new JScrollPane(table),BorderLayout.CENTER);
                    ((DefaultTableModel)table.getModel()).setDataVector(newData, headers);
                    while (true) {
                        int result = JOptionPane.showConfirmDialog(null, panel, "Set base resources", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (result != JOptionPane.OK_OPTION) return;
                        Map<Resource, Double> newBaseResources = new HashMap<>();
                        for(int j = 0; j < table.getRowCount(); j++)
                        {
                            Resource r = netPlan.getResource(j);
                            String capacity = table.getModel().getValueAt(j,2).toString();
                            boolean isBaseResource = (boolean)table.getModel().getValueAt(j,1);
                            if(isBaseResource)
                                newBaseResources.put(r, Double.parseDouble(capacity));

                        }
                        netPlan.addResource(resType, "Resource n_" + netPlan.getResources().size(), hostNode,
                                0, capacityUnits, newBaseResources, 0, null);
                        networkViewer.updateNetPlanView();
                        break;
                    }
                }catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });
        return addItem;
    }

    private List<JComponent> getExtraAddOptions() {
        List<JComponent> options = new LinkedList<JComponent>();

        return options;
    }

    private List<JComponent> getExtraOptions(final int row, final Object itemId) {
        List<JComponent> options = new LinkedList<JComponent>();
        final int numRows = model.getRowCount();
        final NetPlan netPlan = networkViewer.getDesign();

        JMenuItem capacityInBaseResources = new JMenuItem("Set capacity to base resources");
        capacityInBaseResources.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                NetPlan netPlan = networkViewer.getDesign();

                    try {

                        Resource res = netPlan.getResourceFromId((Long) itemId);
                        Set<Resource> baseResources = res.getBaseResources();
                        if(baseResources.size() == 0)
                        {
                            JOptionPane.showMessageDialog(null,"This resource hasn't any base resource");
                            return;
                        }
                        JPanel pane = new JPanel();
                        Object [][] data = {null,null,null};
                        String [] headers = StringUtils.arrayOf("Base Resource","Index","Capacity");
                        TableModel tm = new ClassAwareTableModelImpl(data,headers);
                        AdvancedJTable table = new AdvancedJTable(tm);
                        Object[][] newData = new Object[baseResources.size()][headers.length];
                        int counter = 0;
                        for(Resource r : baseResources)
                        {
                            newData[counter][0] = r.getName();
                            newData[counter][1] = r.getIndex();
                            newData[counter][2] = res.getCapacityOccupiedInBaseResource(r);
                            counter++;
                        }
                        pane.add(new JLabel(res.toString()+" base resources"));
                        pane.add(new JScrollPane(table));
                        ((DefaultTableModel)table.getModel()).setDataVector(newData, headers);
                        while (true) {
                            int result = JOptionPane.showConfirmDialog(null, pane, "Set capacity to base resources", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return;


                            networkViewer.updateNetPlanView();
                            break;
                        }
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set capacity to base resources");
                    }
                }


        });
        options.add(capacityInBaseResources);

        JMenuItem capacityToAllBaseResources = new JMenuItem("Set equal capacity to base resources");
        capacityToAllBaseResources.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = networkViewer.getDesign();
                double cap;
                Resource res = netPlan.getResourceFromId((Long)itemId);
                Set<Resource> baseResources = res.getBaseResources();
                if(baseResources.size() == 0)
                {
                    JOptionPane.showMessageDialog(null,"This resource hasn't any base resource");
                    return;
                }
                while (true) {
                    String str = JOptionPane.showInputDialog(null, "Capacity value", "Set capacity to all base resources", JOptionPane.QUESTION_MESSAGE);
                    if (str == null) return;

                    try {
                        cap = Double.parseDouble(str);
                        if (cap < 0) throw new RuntimeException();

                        break;
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog("Please, introduce a non-negative number", "Error setting capacity");
                    }
                }

                try {
                    Map<Resource, Double> newBaseResourcesCapacities = new HashMap<>();
                    for(Resource r : baseResources)
                    {
                        newBaseResourcesCapacities.put(r,cap);
                    }
                    res.setCapacity(res.getCapacity(),newBaseResourcesCapacities);
                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set capacity to base resources");
                }
            }
        });
        options.add(capacityToAllBaseResources);

        JMenuItem editBaseResources = new JMenuItem("Edit base resources");
        editBaseResources.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                NetPlan netPlan = networkViewer.getDesign();

                try {


                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set capacity to base resources");
                }
                }
        });
        options.add(editBaseResources);
        return options;
    }

    private List<JComponent> getForcedOptions() {
        return new LinkedList<JComponent>();
    }


    @Override
    public void showInCanvas(MouseEvent e, Object itemId) {

    }

    private class ClassAwareTableModelImpl extends ClassAwareTableModel
    {
        public ClassAwareTableModelImpl(Object[][] dataVector, Object[] columnIdentifiers)
        {
            super(dataVector, columnIdentifiers);
        }

        @Override
        public Class getColumnClass(int col)
        {
            return Object.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            if(columnIndex == 2 || columnIndex == 3) return true;
            return false;
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

}
