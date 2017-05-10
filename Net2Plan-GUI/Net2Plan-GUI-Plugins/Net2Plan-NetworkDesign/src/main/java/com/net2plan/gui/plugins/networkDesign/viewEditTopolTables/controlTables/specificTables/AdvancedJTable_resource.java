package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.CellRenderers;
import com.net2plan.gui.plugins.networkDesign.ElementSelection;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AggregationUtils;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.LastRowAggregatedValue;
import com.net2plan.gui.utils.*;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * Created by César on 13/12/2016.
 */
@SuppressWarnings("unchecked")
public class AdvancedJTable_resource extends AdvancedJTable_networkElement
{

    public static final int COLUMN_ID = 0;
    public static final int COLUMN_INDEX = 1;
    public static final int COLUMN_NAME = 2;
    public static final int COLUMN_TYPE = 3;
    public static final int COLUMN_HOSTNODE = 4;
    public static final int COLUMN_CAPACITY = 5;
    public static final int COLUMN_CAPACITYMUNITS = 6;
    public static final int COLUMN_OCCUPIEDCAPACITY = 7;
    public static final int COLUMN_TRAVERSINGROUTES = 8;
    public static final int COLUMN_UPPERRESOURCES = 9;
    public static final int COLUMN_BASERESOURCES = 10;
    public static final int COLUMN_PROCESSINGTIME = 11;
    public static final int COLUMN_TAGS = 12;
    public static final int COLUMN_ATTRIBUTES = 13;
    private static final String netPlanViewTabName = "Resources";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique Identifier", "Index", "Name", "Type", "Host Node", "Capacity", "Cap. Units", "Ocuppied capacity", "# Trav. Routes", "# Upper Resources", "# Base Resources", "Processing Time", "Tags", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique Identifier", "Index", "Name", "Type", "Host Node", "Capacity", "Cap. Units", "Ocuppied capacity", "Number of service chains traversing this resource", "Number of resources in this node that has this resource as its bsae resource", "Number of resources in this node, that this resource has as its base resource", "Processing Time", "Tags", "Attributes");

    public AdvancedJTable_resource(final GUINetworkDesign callback)
    {
        super(createTableModel(callback), callback, Constants.NetworkElementType.RESOURCE);
        setDefaultCellRenderers();
        setSpecificCellRenderers();
        setColumnRowSorting();
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
    public List<Object[]> getAllData(NetPlan currentState, ArrayList<String> attributesTitles)
    {
        final List<Object[]> allResourceData = new LinkedList<Object[]>();
        final List<Resource> rowVisibleResources = getVisibleElementsInTable();
        final double[] dataAggregator = new double[netPlanViewTableHeader.length];

        for (Resource res : rowVisibleResources)
        {
            Object[] resData = new Object[netPlanViewTableHeader.length + attributesTitles.size()];
            resData[COLUMN_ID] = res.getId();
            resData[COLUMN_INDEX] = res.getIndex();
            resData[COLUMN_NAME] = res.getName();
            resData[COLUMN_TYPE] = res.getType();
            resData[COLUMN_HOSTNODE] = res.getHostNode();
            resData[COLUMN_CAPACITY] = res.getCapacity();
            resData[COLUMN_CAPACITYMUNITS] = res.getCapacityMeasurementUnits();
            resData[COLUMN_OCCUPIEDCAPACITY] = res.getOccupiedCapacity();
            resData[COLUMN_TRAVERSINGROUTES] = res.getTraversingRoutes().size();
            resData[COLUMN_UPPERRESOURCES] = res.getUpperResources().size();
            resData[COLUMN_BASERESOURCES] = res.getBaseResources().size();
            resData[COLUMN_PROCESSINGTIME] = res.getProcessingTimeToTraversingTrafficInMs();
            resData[COLUMN_TAGS] = StringUtils.listToString(Lists.newArrayList(res.getTags()));
            resData[COLUMN_ATTRIBUTES] = StringUtils.mapToString(res.getAttributes());

            for (int i = netPlanViewTableHeader.length; i < netPlanViewTableHeader.length + attributesTitles.size(); i++)
            {
                if (res.getAttributes().containsKey(attributesTitles.get(i - netPlanViewTableHeader.length)))
                {
                    resData[i] = res.getAttribute(attributesTitles.get(i - netPlanViewTableHeader.length));
                }
            }

            AggregationUtils.updateRowSum(dataAggregator, COLUMN_CAPACITY, resData[COLUMN_CAPACITY]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_OCCUPIEDCAPACITY, resData[COLUMN_OCCUPIEDCAPACITY]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_TRAVERSINGROUTES, resData[COLUMN_TRAVERSINGROUTES]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_PROCESSINGTIME, resData[COLUMN_PROCESSINGTIME]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_UPPERRESOURCES, resData[COLUMN_UPPERRESOURCES]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_BASERESOURCES, resData[COLUMN_BASERESOURCES]);

            allResourceData.add(resData);
        }

        /* Add the aggregation row with the aggregated statistics */
        final LastRowAggregatedValue[] aggregatedData = new LastRowAggregatedValue[netPlanViewTableHeader.length + attributesTitles.size()];
        Arrays.fill(aggregatedData, new LastRowAggregatedValue());
        aggregatedData[COLUMN_CAPACITY] = new LastRowAggregatedValue(dataAggregator[COLUMN_CAPACITY]);
        aggregatedData[COLUMN_OCCUPIEDCAPACITY] = new LastRowAggregatedValue(dataAggregator[COLUMN_OCCUPIEDCAPACITY]);
        aggregatedData[COLUMN_TRAVERSINGROUTES] = new LastRowAggregatedValue(dataAggregator[COLUMN_TRAVERSINGROUTES]);
        aggregatedData[COLUMN_PROCESSINGTIME] = new LastRowAggregatedValue(dataAggregator[COLUMN_PROCESSINGTIME]);
        aggregatedData[COLUMN_UPPERRESOURCES] = new LastRowAggregatedValue(dataAggregator[COLUMN_UPPERRESOURCES]);
        aggregatedData[COLUMN_BASERESOURCES] = new LastRowAggregatedValue(dataAggregator[COLUMN_BASERESOURCES]);

        allResourceData.add(aggregatedData);

        return allResourceData;
    }

    @Override
    public String getTabName()
    {
        return netPlanViewTabName;
    }

    @Override
    public String[] getTableHeaders()
    {
        return netPlanViewTableHeader;
    }

    @Override
    public String[] getCurrentTableHeaders()
    {
        ArrayList<String> attColumnsHeaders = getAttributesColumnsHeaders();
        String[] headers = new String[netPlanViewTableHeader.length + attColumnsHeaders.size()];
        for (int i = 0; i < headers.length; i++)
        {
            if (i < netPlanViewTableHeader.length)
            {
                headers[i] = netPlanViewTableHeader[i];
            } else
            {
                headers[i] = "Att: " + attColumnsHeaders.get(i - netPlanViewTableHeader.length);
            }
        }


        return headers;
    }

    @Override
    public String[] getTableTips()
    {
        return netPlanViewTableTips;
    }

    @Override
    public boolean hasElements()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
        return rf == null ? callback.getDesign().hasResources() : rf.hasResources(layer);
    }
    

    @Override
    public int getAttributesColumnIndex()
    {
        return COLUMN_ATTRIBUTES;
    }

//    @Override
//    public int[] getColumnsOfSpecialComparatorForSorting() {
//        return new int[]{5};
//    }

    private static TableModel createTableModel(final GUINetworkDesign callback)
    {
        TableModel resourceTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                if (!callback.getVisualizationState().isNetPlanEditable()) return false;
                if (columnIndex >= netPlanViewTableHeader.length) return true;
                if (getValueAt(rowIndex, columnIndex) == null) return false;
                if (rowIndex == getRowCount() - 1) return false;

                return columnIndex == COLUMN_NAME || columnIndex == COLUMN_CAPACITY || columnIndex == COLUMN_PROCESSINGTIME;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column)
            {
                Object oldValue = getValueAt(row, column);

				/* If value doesn't change, exit from function */
                if (newValue != null && newValue.equals(oldValue)) return;

                NetPlan netPlan = callback.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long resId = (Long) getValueAt(row, 0);
                final Resource res = netPlan.getResourceFromId(resId);

                try
                {
                    switch (column)
                    {
                        case COLUMN_NAME:
                            res.setName(newValue.toString());
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.RESOURCE));
                            callback.getVisualizationState().pickElement(res);
                            callback.updateVisualizationAfterPick();
                            callback.addNetPlanChange();
                            break;

                        case COLUMN_CAPACITY:
                            if (newValue == null) return;
                            res.setCapacity((Double) newValue, netPlan.getResourceFromId(resId).getCapacityOccupiedInBaseResourcesMap());
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.RESOURCE));
                            callback.getVisualizationState().pickElement(res);
                            callback.updateVisualizationAfterPick();
                            callback.addNetPlanChange();
                            break;

                        case COLUMN_PROCESSINGTIME:
                            if (newValue == null) return;
                            res.setProcessingTimeToTraversingTrafficInMs((Double) newValue);
                            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.RESOURCE));
                            callback.getVisualizationState().pickElement(res);
                            callback.updateVisualizationAfterPick();
                            callback.addNetPlanChange();
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex)
                {
                    ex.printStackTrace();
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying resource");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };

        return resourceTableModel;
    }

    private void setSpecificCellRenderers()
    {
    }

    private void setDefaultCellRenderers()
    {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());
    }

    @Override
    public void setColumnRowSorting()
    {
        setAutoCreateRowSorter(true);
        final Set<Integer> columnsWithDoubleAndThenParenthesis = Sets.newHashSet(COLUMN_CAPACITY);
        DefaultRowSorter rowSorter = ((DefaultRowSorter) getRowSorter());
        for (int col = 0; col <= COLUMN_ATTRIBUTES; col++)
            rowSorter.setComparator(col, new AdvancedJTable_networkElement.ColumnComparator(rowSorter, columnsWithDoubleAndThenParenthesis.contains(col)));
        fixedTable.setAutoCreateRowSorter(true);
        fixedTable.setRowSorter(this.getRowSorter());
        rowSorter = ((DefaultRowSorter) fixedTable.getRowSorter());
        for (int col = 0; col <= COLUMN_ATTRIBUTES; col++)
            rowSorter.setComparator(col, new AdvancedJTable_networkElement.ColumnComparator(rowSorter, columnsWithDoubleAndThenParenthesis.contains(col)));
    }

    @Override
    public int getNumberOfDecoratorColumns()
    {
        return 2;
    }

    @Override
    public ArrayList<String> getAttributesColumnsHeaders()
    {
        ArrayList<String> attColumnsHeaders = new ArrayList<>();
        for (Resource res : getVisibleElementsInTable())
        {

            for (Map.Entry<String, String> entry : res.getAttributes().entrySet())
            {
                if (attColumnsHeaders.contains(entry.getKey()) == false)
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
        for (Map.Entry<Route, Double> entry : routesCapacities.entrySet())
        {
            if (counter == routesCapacities.size() - 1)
                t = t + "R" + entry.getKey().getIndex() + " (" + entry.getValue() + ") ";
            else
                t = t + "R" + entry.getKey().getIndex() + " (" + entry.getValue() + "), ";

            counter++;
        }

        return t;
    }

    private String joinUpperResourcesWithTheirCapacities(Resource res)
    {
        Map<Resource, Double> upperResourcesCapacities = res.getCapacityOccupiedByUpperResourcesMap();
        String t = "";
        int counter = 0;
        for (Map.Entry<Resource, Double> entry : upperResourcesCapacities.entrySet())
        {
            if (counter == upperResourcesCapacities.size() - 1)
                t = t + "r" + entry.getKey().getIndex() + " (" + entry.getValue() + ") ";
            else
                t = t + "r" + entry.getKey().getIndex() + " (" + entry.getValue() + "), ";

            counter++;
        }

        return t;
    }

    private String joinBaseResourcesWithTheirCapacities(Resource res)
    {
        Map<Resource, Double> baseResourcesCapacities = res.getCapacityOccupiedInBaseResourcesMap();
        String t = "";
        int counter = 0;
        for (Map.Entry<Resource, Double> entry : baseResourcesCapacities.entrySet())
        {
            if (counter == baseResourcesCapacities.size() - 1)
                t = t + "r" + entry.getKey().getIndex() + " (" + entry.getValue() + ") ";
            else
                t = t + "r" + entry.getKey().getIndex() + " (" + entry.getValue() + "), ";

            counter++;
        }

        return t;

    }

    @Override
    public JPopupMenu getPopup(ElementSelection selection)
    {
        assert selection != null;

        final JScrollPopupMenu popup = new JScrollPopupMenu(20);
        final List<Resource> rowsInTheTable = getVisibleElementsInTable();

        if (!selection.isEmpty())
            if (selection.getElementType() != NetworkElementType.RESOURCE)
                throw new RuntimeException("Unmatched items with table, selected items are of type: " + selection.getElementType());

        /* Add the popup menu option of the filters */
        final List<Resource> selectedResources = (List<Resource>) selection.getNetworkElements();

        if (!rowsInTheTable.isEmpty())
        {
        	addPickOption(selection, popup);
            addFilterOptions(selection, popup);
            popup.addSeparator();
        }

        if (callback.getVisualizationState().isNetPlanEditable())
        {
            popup.add(getAddOption());

            if (!rowsInTheTable.isEmpty())
            {
                if (!selectedResources.isEmpty())
                {
                    JMenuItem removeItem = new JMenuItem("Remove selected resources");
                    removeItem.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            try
                            {
                                for (Resource selectedResource : selectedResources)
                                    selectedResource.remove();

                                callback.getVisualizationState().resetPickedState();
                                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.RESOURCE));
                                callback.addNetPlanChange();
                            } catch (Throwable ex)
                            {
                                ErrorHandling.addErrorOrException(ex, getClass());
                                ErrorHandling.showErrorDialog("Unable to remove " + networkElementType);
                            }
                        }
                    });

                    popup.add(removeItem);
                }
            }

            for (JComponent item : getExtraAddOptions())
                popup.add(item);

            if (!rowsInTheTable.isEmpty() && !selectedResources.isEmpty())
            {
                List<JComponent> forcedOptions = getForcedOptions(selection);
                if (!forcedOptions.isEmpty())
                {
                    if (popup.getSubElements().length > 0) popup.addSeparator();
                    for (JComponent item : forcedOptions) popup.add(item);
                }

                List<JComponent> extraOptions = getExtraOptions(selection);
                if (!extraOptions.isEmpty())
                {
                    if (popup.getSubElements().length > 0) popup.addSeparator();
                    for (JComponent item : extraOptions) popup.add(item);
                }

                addPopupMenuAttributeOptions(selection, popup);
            }
        }

        return popup;
    }


    @Override
    protected JMenuItem getAddOption()
    {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);
        addItem.addActionListener(e ->
        {
            NetPlan netPlan = callback.getDesign();

            try
            {

                JComboBox hostNodeSelector = new WiderJComboBox();
                JTextField capUnitsField = new JTextField(20);
                JTextField typeSelector = new JTextField(20);

                for (Node n : netPlan.getNodes())
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

                int result = JOptionPane.showConfirmDialog(null, pane, "Please enter parameters for the new " + networkElementType, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (result != JOptionPane.OK_OPTION) return;
                hostNode = netPlan.getNodeFromId((Long) ((StringLabeller) hostNodeSelector.getSelectedItem()).getObject());
                capacityUnits = capUnitsField.getText();
                resType = typeSelector.getText();

                JPanel panel = new JPanel();
                Object[][] data = {null, null, null};
                String[] headers = StringUtils.arrayOf("Base Resource", "Is Base Resource", "Capacity");
                TableModel tm = new ClassAwareTableModelImpl(data, headers, new HashSet<Integer>(Arrays.asList(1, 2)));
                AdvancedJTable table = new AdvancedJTable(tm);
                int baseResCounter = 0;
                for (Resource r : netPlan.getResources())
                {
                    if (r.getHostNode().toString().equals(hostNode.toString()))
                    {
                        baseResCounter++;
                    }
                }
                Object[][] newData = new Object[baseResCounter][headers.length];
                int counter = 0;
                for (Resource r : netPlan.getResources())
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
                    Resource r = netPlan.getResource(j);
                    String capacity = table.getModel().getValueAt(j, 2).toString();
                    boolean isBaseResource = (boolean) table.getModel().getValueAt(j, 1);
                    if (isBaseResource)
                        newBaseResources.put(r, Double.parseDouble(capacity));

                }
                netPlan.addResource(resType, "Resource n_" + netPlan.getResources().size(), hostNode,
                        0, capacityUnits, newBaseResources, 0, null);
                callback.getVisualizationState().resetPickedState();
                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.RESOURCE));
                callback.addNetPlanChange();
            } catch (Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
            }
        });
        return addItem;
    }


    @Override
    protected List<JComponent> getExtraAddOptions()
    {
        return new LinkedList<>();
    }


    @Override
    protected List<JComponent> getExtraOptions(final ElementSelection selection)
    {
        assert selection != null;

        List<JComponent> options = new LinkedList<>();
        final List<Resource> selectedResources = (List<Resource>) selection.getNetworkElements();

        JMenuItem capacityInBaseResources = new JMenuItem("Set capacity to base resources from selection");
        capacityInBaseResources.addActionListener(e ->
        {
            try
            {
                final Set<Resource> baseResources = new HashSet<>();
                for (Resource res : selectedResources)
                    baseResources.addAll(res.getBaseResources());

                if (baseResources.size() == 0)
                {
                    JOptionPane.showMessageDialog(null, "Selected resources does not have any base resources.");
                    return;
                }

                JPanel pane = new JPanel(new BorderLayout());
                Object[][] data = {null, null, null};
                String[] headers = StringUtils.arrayOf("Index", "Type", "Capacity");
                TableModel tm = new ClassAwareTableModelImpl(data, headers, Collections.singleton(2));
                AdvancedJTable table = new AdvancedJTable(tm);
                Object[][] newData = new Object[baseResources.size()][headers.length];

                final List<Resource> baseResourcesList = new ArrayList<>(baseResources);
                final StringBuilder indexBuilder = new StringBuilder();
                for (Resource res : selectedResources)
                {
                    indexBuilder.append(res.getIndex());
                    indexBuilder.append(" ");

                    for (int i = 0; i < baseResourcesList.size(); i++)
                    {
                        final Resource r = baseResourcesList.get(i);
                        newData[i][0] = r.getIndex();
                        newData[i][1] = r.getType();
                        newData[i][2] = res.getCapacityOccupiedInBaseResource(r);
                    }
                }
                pane.add(new JLabel("Setting the occupied capacity in base resource for index: " + indexBuilder.toString().trim()), BorderLayout.NORTH);
                pane.add(new JScrollPane(table), BorderLayout.CENTER);

                ((DefaultTableModel) table.getModel()).setDataVector(newData, headers);
                int result = JOptionPane.showConfirmDialog(null, pane, "Set capacity to base resources", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (result != JOptionPane.OK_OPTION) return;

                final Map<Resource, Map<Resource, Double>> valueMap = new HashMap<>();
                for (Resource res : selectedResources)
                {
                    final Set<Resource> resBaseResources = res.getBaseResources();

                    Map<Resource, Double> newCapMap = new HashMap<>();
                    for (int t = 0; t < table.getRowCount(); t++)
                    {
                        if (resBaseResources.contains(baseResourcesList.get(t)))
                        {
                            final String valueAt = table.getModel().getValueAt(t, 2).toString();
                            if (!NumberUtils.isNumber(valueAt))
                                throw new RuntimeException("Invalid value found for resource: " + baseResourcesList.get(t).getName());
                            newCapMap.put(baseResourcesList.get(t), Double.parseDouble(valueAt));
                        }
                    }
                    valueMap.put(res, newCapMap);
                }

                for (Resource res : selectedResources)
                    res.setCapacity(res.getCapacity(), valueMap.get(res));

                callback.getVisualizationState().resetPickedState();
                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.RESOURCE));
                callback.addNetPlanChange();
            } catch (Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set capacity to base resources");
            }
        });
        options.add(capacityInBaseResources);

        JMenuItem setCapacity = new JMenuItem("Set capacity to selected resources (base resources occupation unchanged)");
        setCapacity.addActionListener(e ->
        {
            double cap;
            while (true)
            {
                String str = JOptionPane.showInputDialog(null, "Capacity value", "Set capacity to selected table resources", JOptionPane.QUESTION_MESSAGE);
                if (str == null) continue;

                try
                {
                    cap = Double.parseDouble(str);
                    if (cap < 0) throw new RuntimeException("Please, introduce a non-negative number");
                    break;
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error setting capacity");
                }
            }

            try
            {
                for (Resource r : selectedResources)
                    r.setCapacity(cap, null);

                callback.getVisualizationState().resetPickedState();
                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.RESOURCE));
                callback.addNetPlanChange();
            } catch (Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set processing time to resources");
            }
        });
        options.add(setCapacity);

        JMenuItem setTraversingTime = new JMenuItem("Set selected resources processing time");
        setTraversingTime.addActionListener(e ->
        {
            double procTime;
            while (true)
            {
                String str = JOptionPane.showInputDialog(null, "Processing time in milliseconds", "Set processing time in milliseconds", JOptionPane.QUESTION_MESSAGE);
                if (str == null) return;

                try
                {
                    procTime = Double.parseDouble(str);
                    if (procTime < 0) throw new RuntimeException("Please, introduce a non-negative number");
                    break;
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error setting processing time");
                }
            }

            try
            {
                for (Resource r : selectedResources)
                    r.setProcessingTimeToTraversingTrafficInMs(procTime);

                callback.getVisualizationState().resetPickedState();
                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.RESOURCE));
                callback.addNetPlanChange();
            } catch (Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set processing time to resources");
            }
        });
        options.add(setTraversingTime);

        return options;
    }


    @Override
    protected List<JComponent> getForcedOptions(ElementSelection selection)
    {
        return new LinkedList<>();
    }

    @Override
    public void pickSelection(ElementSelection selection)
    {
        if (getVisibleElementsInTable().isEmpty()) return;
        if (selection.getElementType() != NetworkElementType.RESOURCE)
            throw new RuntimeException("Unmatched items with table, selected items are of type: " + selection.getElementType());

        callback.getVisualizationState().pickElement((List<Resource>) selection.getNetworkElements());
        callback.updateVisualizationAfterPick();
    }

    @Override
    protected boolean hasAttributes()
    {
        return true;
    }

    private class ClassAwareTableModelImpl extends ClassAwareTableModel
    {
        private final Set<Integer> editableColumns;

        public ClassAwareTableModelImpl(Object[][] dataVector, Object[] columnIdentifiers, Set<Integer> editableColumns)
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
        final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
        return rf == null ? callback.getDesign().getResources() : rf.getVisibleResources(layer);
    }
}
