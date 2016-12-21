package com.net2plan.gui.utils.viewEditTopolTables.specificTables;

import com.net2plan.gui.utils.CellRenderers;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.StringUtils;

import javax.swing.table.TableModel;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.*;

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
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique Identifier","Index","Name","Type","Host Node","Capacity","Capacity Measurement Units","Ocuppied capacity","Traversing Routes","Upper Resources","Base Resources","Processing Time","Attributes");
    private List<Resource> currentResources = new LinkedList<>();
    private NetPlan currentTopology = null;

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
    }


    @Override
    public List<Object[]> getAllData(NetPlan currentState, TopologyPanel topologyPanel, NetPlan initialState, ArrayList<String> attributesTitles) {
        List<Object[]> allNodeData = new LinkedList<Object[]>();
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
            }
        }
        return allNodeData;
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
        return !networkViewer.getDesign().hasResources();
    }

    @Override
    public int getAttributesColumnIndex() {
        return COLUMN_ATTRIBUTES;
    }

    @Override
    public int[] getColumnsOfSpecialComparatorForSorting() {
        return new int[0];
    }

    private static TableModel createTableModel(final INetworkCallback networkViewer) {
        final TopologyPanel topologyPanel = networkViewer.getTopologyPanel();
        TableModel nodeTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (!networkViewer.isEditable()) return false;
                if( columnIndex >= netPlanViewTableHeader.length) return true;
                if (getValueAt(rowIndex,columnIndex) == null) return false;

                return columnIndex == COLUMN_NAME || columnIndex == COLUMN_CAPACITY;
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

        return nodeTableModel;
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

        setDefaultRenderer(Boolean.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Boolean.class), networkViewer, Constants.NetworkElementType.NODE));
        setDefaultRenderer(Double.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Double.class), networkViewer, Constants.NetworkElementType.NODE));
        setDefaultRenderer(Object.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Object.class), networkViewer, Constants.NetworkElementType.NODE));
        setDefaultRenderer(Float.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Float.class), networkViewer, Constants.NetworkElementType.NODE));
        setDefaultRenderer(Long.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Long.class), networkViewer, Constants.NetworkElementType.NODE));
        setDefaultRenderer(Integer.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(Integer.class), networkViewer, Constants.NetworkElementType.NODE));
        setDefaultRenderer(String.class, new CellRenderers.UpDownRenderer(getDefaultRenderer(String.class), networkViewer, Constants.NetworkElementType.NODE));
    }
    @Override
    public void setColumnRowSorting(boolean allowShowInitialNetPlan) {

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
        for(Map.Entry<Route, Double> entry : routesCapacities.entrySet())
        {
            t = t + "Route "+entry.getKey().toString()+" ("+entry.getValue()+"), ";
        }

        return t;
    }

    private String joinUpperResourcesWithTheirCapacities(Resource res)
    {
        Map<Resource, Double> upperResourcesCapacities = res.getCapacityOccupiedByUpperResourcesMap();
        String t = "";
        for(Map.Entry<Resource, Double> entry : upperResourcesCapacities.entrySet())
        {
            t = t + "Resource "+entry.getKey().toString()+" ("+entry.getValue()+"), ";
        }

        return t;
    }

    private String joinBaseResourcesWithTheirCapacities(Resource res)
    {
        Map<Resource, Double> baseResourcesCapacities = res.getCapacityOccupiedInBaseResourcesMap();
        String t = "";
        for(Map.Entry<Resource, Double> entry : baseResourcesCapacities.entrySet())
        {
            t = t + "Resource "+entry.getKey().toString()+" ("+entry.getValue()+"), ";
        }

        return t;

    }
    @Override
    public void doPopup(MouseEvent e, int row, Object itemId) {

    }

    @Override
    public void showInCanvas(MouseEvent e, Object itemId) {

    }
}
