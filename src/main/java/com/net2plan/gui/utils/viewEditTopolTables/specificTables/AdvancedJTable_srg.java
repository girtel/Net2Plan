/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/


package com.net2plan.gui.utils.viewEditTopolTables.specificTables;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.DefaultRowSorter;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.google.common.collect.Sets;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.CellRenderers;
import com.net2plan.gui.utils.CellRenderers.NumberCellRenderer;
import com.net2plan.gui.utils.CellRenderers.UnfocusableCellRenderer;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.CurrentAndPlannedStateTableSorter;
import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.SwingUtils;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.SRGUtils.SharedRiskModel;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.StringUtils;

import net.miginfocom.swing.MigLayout;

/**
 */
@SuppressWarnings("unchecked")
public class AdvancedJTable_srg extends AdvancedJTableNetworkElement {
    private static final String netPlanViewTabName = "Shared-risk groups";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "MTTF (hours)", "MTTR (hours)", "Availability",
            "Nodes", "Links", "Links (other layers)", "# Affected routes", "# Affected backup routes", "# Affected multicast trees", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)", "Index (consecutive integer starting in zero)", "Mean time to fail", "Mean time to repair", "Expected availability", "Nodes included into the shared-risk group", "Links (in this layer) included into the shared-risk group", "Links (in other layers) included into the shared-risk group", "# Affected routes (primary or backup)", "# Affected routes that are designated as backup routes", "# Affected multicast trees", "Attributes");
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_INDEX = 1;
    private static final int COLUMN_MTTF = 2;
    private static final int COLUMN_MTTR = 3;
    private static final int COLUMN_AVAILABILITY = 4;
    private static final int COLUMN_NODES = 5;
    private static final int COLUMN_LINKS = 6;
    private static final int COLUMN_LINKSOTHERLAYERS = 7;
    private static final int COLUMN_AFFECTEDROUTES = 8;
    private static final int COLUMN_AFFECTEDBACKUPROUTES = 9;
    private static final int COLUMN_AFFECTEDTREES = 10;
    private static final int COLUMN_ATTRIBUTES = 11;
    private static int MAXNUMDECIMALSINAVAILABILITY = 7;

    private List<SharedRiskGroup> currentSRGs = new LinkedList<>();
    private NetPlan currentTopology = null;

    public AdvancedJTable_srg(final IVisualizationCallback callback) {
        super(createTableModel(callback), callback, NetworkElementType.SRG, true);
        setDefaultCellRenderers(callback);
        setSpecificCellRenderers();
        setColumnRowSorting(callback.inOnlineSimulationMode());
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

    public List<Object[]> getAllData(NetPlan currentState, NetPlan initialState, ArrayList<String> attributesColumns) {
        NetworkLayer layer = currentState.getNetworkLayerDefault();
        List<Object[]> allSRGData = new LinkedList<Object[]>();
        for (SharedRiskGroup srg : currentState.getSRGs()) {
            Set<Route> routeIds_thisSRG = currentState.getRoutingType() == RoutingType.SOURCE_ROUTING ? srg.getAffectedRoutes(layer) : new LinkedHashSet<Route>();
            Set<Route> segmentIds_thisSRG = currentState.getRoutingType() == RoutingType.SOURCE_ROUTING ? srg.getAffectedRoutes(layer).stream().filter(e->e.isBackupRoute()).collect(Collectors.toSet()) : new LinkedHashSet<Route>();
            Set<MulticastTree> treeIds_thisSRG = currentState.getRoutingType() == RoutingType.SOURCE_ROUTING ? srg.getAffectedMulticastTrees(layer) : new LinkedHashSet<MulticastTree>();
            int numRoutes = routeIds_thisSRG.size();
            int numSegments = segmentIds_thisSRG.size();
            int numMulticastTrees = treeIds_thisSRG.size();
            Set<Node> nodeIds_thisSRG = srg.getNodes();
            Set<Link> linkIds_thisSRG = srg.getLinks(layer);

            Object[] srgData = new Object[netPlanViewTableHeader.length + attributesColumns.size()];
            srgData[0] = srg.getId();
            srgData[1] = srg.getIndex();
            srgData[2] = srg.getMeanTimeToFailInHours();
            srgData[3] = srg.getMeanTimeToRepairInHours();
            srgData[4] = srg.getAvailability();
            srgData[5] = nodeIds_thisSRG.isEmpty() ? "none" : CollectionUtils.join(NetPlan.getIndexes(nodeIds_thisSRG), ", ");
            srgData[6] = linkIds_thisSRG.isEmpty() ? "none" : CollectionUtils.join(NetPlan.getIndexes(srg.getLinksAllLayers()), ", ");
            srgData[7] = srg.getLinks(layer).isEmpty() ? "none" : CollectionUtils.join(NetPlan.getIndexes(srg.getLinks(layer)), ", ");
            srgData[8] = numRoutes == 0 ? "none" : numRoutes + " (" + CollectionUtils.join(NetPlan.getIndexes(routeIds_thisSRG), ", ") + ")";
            srgData[9] = numSegments == 0 ? "none" : numSegments + " (" + CollectionUtils.join(NetPlan.getIndexes(segmentIds_thisSRG), ", ") + ")";
            srgData[10] = numMulticastTrees == 0 ? "none" : numMulticastTrees + " (" + CollectionUtils.join(NetPlan.getIndexes(treeIds_thisSRG), ", ") + ")";
            srgData[11] = StringUtils.mapToString(srg.getAttributes());

            for(int i = netPlanViewTableHeader.length; i < netPlanViewTableHeader.length + attributesColumns.size();i++)
            {
                if(srg.getAttributes().containsKey(attributesColumns.get(i-netPlanViewTableHeader.length)))
                {
                    srgData[i] = srg.getAttribute(attributesColumns.get(i-netPlanViewTableHeader.length));
                }
            }

            allSRGData.add(srgData);

            if (initialState != null && initialState.getSRGFromId(srg.getId()) != null) {
                layer = initialState.getNetworkLayerDefault();
                srg = initialState.getSRGFromId(srg.getId());
                routeIds_thisSRG = initialState.getRoutingType() == RoutingType.SOURCE_ROUTING ? srg.getAffectedRoutes(layer) : new LinkedHashSet<Route>();
                segmentIds_thisSRG = initialState.getRoutingType() == RoutingType.SOURCE_ROUTING ? srg.getAffectedRoutes(layer).stream().filter(e->e.isBackupRoute()).collect(Collectors.toSet()) : new LinkedHashSet<Route>();
                treeIds_thisSRG = initialState.getRoutingType() == RoutingType.SOURCE_ROUTING ? srg.getAffectedMulticastTrees(layer) : new LinkedHashSet<MulticastTree>();
                numRoutes = routeIds_thisSRG.size();
                numSegments = segmentIds_thisSRG.size();
                numMulticastTrees = treeIds_thisSRG.size();
                nodeIds_thisSRG = srg.getNodes();
                linkIds_thisSRG = srg.getLinks(layer);


                Object[] srgData_initialNetPlan = new Object[netPlanViewTableHeader.length + attributesColumns.size()];
                srgData_initialNetPlan[0] = null;
                srgData_initialNetPlan[1] = null;
                srgData_initialNetPlan[2] = srg.getMeanTimeToFailInHours();
                srgData_initialNetPlan[3] = srg.getMeanTimeToRepairInHours();
                srgData_initialNetPlan[4] = srg.getAvailability();
                srgData_initialNetPlan[5] = nodeIds_thisSRG.isEmpty() ? "none" : CollectionUtils.join(NetPlan.getIndexes(nodeIds_thisSRG), ", ");
                srgData_initialNetPlan[6] = linkIds_thisSRG.isEmpty() ? "none" : CollectionUtils.join(NetPlan.getIndexes(srg.getLinksAllLayers()), ", ");
                srgData_initialNetPlan[7] = srg.getLinks(layer).isEmpty() ? "none" : CollectionUtils.join(NetPlan.getIndexes(srg.getLinks(layer)), ", ");
                srgData_initialNetPlan[8] = numRoutes == 0 ? "none" : numRoutes + " (" + CollectionUtils.join(NetPlan.getIndexes(routeIds_thisSRG), ", ") + ")";
                srgData_initialNetPlan[9] = numSegments == 0 ? "none" : numSegments + " (" + CollectionUtils.join(NetPlan.getIndexes(segmentIds_thisSRG), ", ") + ")";
                srgData_initialNetPlan[10] = numMulticastTrees == 0 ? "none" : numMulticastTrees + " (" + CollectionUtils.join(NetPlan.getIndexes(treeIds_thisSRG), ", ") + ")";
                srgData_initialNetPlan[11] = StringUtils.mapToString(srg.getAttributes());

                for(int i = netPlanViewTableHeader.length; i < netPlanViewTableHeader.length + attributesColumns.size();i++)
                {
                    if(srg.getAttributes().containsKey(attributesColumns.get(i-netPlanViewTableHeader.length)))
                    {
                        srgData_initialNetPlan[i] = srg.getAttribute(attributesColumns.get(i-netPlanViewTableHeader.length));
                    }
                }

                allSRGData.add(srgData_initialNetPlan);
            }
        }

        return allSRGData;
    }

    public String getTabName() {
        return netPlanViewTabName;
    }

    public String[] getTableHeaders() {
        return netPlanViewTableHeader;
    }

    public String[] getCurrentTableHeaders(){
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




    public String[] getTableTips() {
        return netPlanViewTableTips;
    }

    public boolean hasElements(NetPlan np) {
        return np.hasSRGs();
    }

    @Override
    public int getAttributesColumnIndex()
    {
        return COLUMN_ATTRIBUTES;
    }

    public int[] getColumnsOfSpecialComparatorForSorting() {
        return new int[]{8, 9};
    }

    private static TableModel createTableModel(final IVisualizationCallback callback) {
        TableModel srgTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (!callback.getVisualizationState().isNetPlanEditable()) return false;
                if (columnIndex >= netPlanViewTableHeader.length) return true;
                if (getValueAt(rowIndex,columnIndex) == null) return false;

                return columnIndex == COLUMN_MTTF || columnIndex == COLUMN_MTTR || columnIndex >= netPlanViewTableHeader.length;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column) {
                Object oldValue = getValueAt(row, column);

                if (newValue.equals(oldValue)) return;

                NetPlan netPlan = callback.getDesign();
                NetPlan aux_netPlan = netPlan.copy();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long srgId = (Long) getValueAt(row, 0);
                final SharedRiskGroup srg = netPlan.getSRGFromId(srgId);

				/* Perform checks, if needed */
                try {
                    switch (column) {
                        case COLUMN_MTTF:
                            srg.setMeanTimeToFailInHours(Double.parseDouble(newValue.toString()));
                            super.setValueAt(srg.getAvailability(), row, COLUMN_AVAILABILITY);
                            break;

                        case COLUMN_MTTR:
                            srg.setMeanTimeToRepairInHours(Double.parseDouble(newValue.toString()));
                            super.setValueAt(srg.getAvailability(), row, COLUMN_AVAILABILITY);
                            break;

                        default:
                            break;
                    }
                } catch (Throwable ex) {
                    callback.getDesign().assignFrom(aux_netPlan);
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying SRG");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };
        return srgTableModel;
    }

    private void setDefaultCellRenderers(final IVisualizationCallback callback) {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());
    }

    private void setSpecificCellRenderers() {
        getColumnModel().getColumn(convertColumnIndexToView(COLUMN_AVAILABILITY)).setCellRenderer(new CellRenderers.NumberCellRenderer(MAXNUMDECIMALSINAVAILABILITY));
    }

    public void setColumnRowSorting(boolean allowShowInitialNetPlan) {
        if (allowShowInitialNetPlan) setRowSorter(new CurrentAndPlannedStateTableSorter(getModel()));
        else setAutoCreateRowSorter(true);
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_AFFECTEDROUTES, new AdvancedJTableNetworkElement.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_AFFECTEDBACKUPROUTES, new AdvancedJTableNetworkElement.ColumnComparator());
        ((DefaultRowSorter) getRowSorter()).setComparator(COLUMN_AFFECTEDTREES, new AdvancedJTableNetworkElement.ColumnComparator());
    }

    public int getNumFixedLeftColumnsInDecoration() {
        return 2;
    }

    @Override
    public ArrayList<String> getAttributesColumnsHeaders()
    {
        ArrayList<String> attColumnsHeaders = new ArrayList<>();
        currentTopology = callback.getDesign();
        currentSRGs = currentTopology.getSRGs();
        for(SharedRiskGroup srg : currentSRGs)
        {

            for (Map.Entry<String, String> entry : srg.getAttributes().entrySet())
            {
                if(attColumnsHeaders.contains(entry.getKey()) == false)
                {
                    attColumnsHeaders.add(entry.getKey());
                }

            }

        }

        return attColumnsHeaders;
    }

    @Override
    public void doPopup(final MouseEvent e, final int row, final Object itemId) {
        JPopupMenu popup = new JPopupMenu();
        if (callback.getVisualizationState().isNetPlanEditable()) {
            popup.add(getAddOption());
            for (JComponent item : getExtraAddOptions())
                popup.add(item);
        }

        if (!isTableEmpty()) {
            if (callback.getVisualizationState().isNetPlanEditable()) {
                if (row != -1) {
                    if (popup.getSubElements().length > 0) popup.addSeparator();

                    JMenuItem removeItem = new JMenuItem("Remove " + networkElementType);
                    removeItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NetPlan netPlan = callback.getDesign();
                            try {
                                netPlan.getSRGFromId((long) itemId).remove();
                            	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.SRG) , null , null);
                            } catch (Throwable ex) {
                                ErrorHandling.addErrorOrException(ex, getClass());
                                ErrorHandling.showErrorDialog("Unable to remove " + networkElementType);
                            }
                        }
                    });

                    popup.add(removeItem);
                    addPopupMenuAttributeOptions(e, row, itemId, popup);
                }
                JMenuItem removeItems = new JMenuItem("Remove all " + networkElementType + "s");

                removeItems.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        NetPlan netPlan = callback.getDesign();

                        try {
                            netPlan.removeAllSRGs();
                        	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.SRG) , null , null);
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

    @Override
    public void showInCanvas(MouseEvent e, Object itemId) 
    {
        if (isTableEmpty()) return;
        final SharedRiskGroup srg = callback.getDesign().getSRGFromId((long) itemId);
        callback.pickSRGAndUpdateView(callback.getDesign().getNetworkLayerDefault() , srg);
    }

    private boolean isTableEmpty() {
        return !callback.getDesign().hasSRGs();
    }

    private JMenuItem getAddOption() {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);

        addItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = callback.getDesign();

                try {
                    netPlan.addSRG(8748, 12, null);
                	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.SRG) , null , null);
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });
        return addItem;
    }

    private List<JComponent> getExtraAddOptions() {
        List<JComponent> options = new LinkedList<JComponent>();
        NetPlan netPlan = callback.getDesign();
        JMenu submenuSRGs = new JMenu("Add SRGs from model");
        options.add(submenuSRGs);

        final JMenuItem onePerNode = new JMenuItem("One SRG per node");
        submenuSRGs.add(onePerNode);
        final JMenuItem onePerLink = new JMenuItem("One SRG per unidirectional link");
        submenuSRGs.add(onePerLink);
        final JMenuItem onePerLinkBundle = new JMenuItem("One SRG per unidirectional bundle of links");
        submenuSRGs.add(onePerLinkBundle);
        final JMenuItem onePerBidiLinkBundle = new JMenuItem("One SRG per bidirectional bundle of links");
        submenuSRGs.add(onePerBidiLinkBundle);

        if (!netPlan.hasNodes()) {
            onePerNode.setEnabled(false);
        }

        if (!netPlan.hasLinks()) {
            onePerLink.setEnabled(false);
            onePerLinkBundle.setEnabled(false);
            onePerBidiLinkBundle.setEnabled(false);
        }

        ActionListener srgModel = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double mttf;
                double mttr;

                JTextField txt_mttf = new JTextField(5);
                JTextField txt_mttr = new JTextField(5);
                JCheckBox chk_removeExistingSRGs = new JCheckBox();
                chk_removeExistingSRGs.setSelected(true);

                JPanel pane = new JPanel(new GridLayout(0, 2));
                pane.add(new JLabel("MTTF (in hours, zero or negative value means no failure): "));
                pane.add(txt_mttf);
                pane.add(new JLabel("MTTR (in hours): "));
                pane.add(txt_mttr);
                pane.add(new JLabel("Remove existing SRGs: "));
                pane.add(chk_removeExistingSRGs);

                while (true) {
                    int result = JOptionPane.showConfirmDialog(null, pane, "Add SRGs from model", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return;

                    try {
                        mttf = Double.parseDouble(txt_mttf.getText());
                        mttr = Double.parseDouble(txt_mttr.getText());
                        if (mttr <= 0) throw new RuntimeException();

                        break;
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog("MTTF/MTTR must be greater than zero", "Error adding SRGs from model");
                    }
                }

                boolean removeExistingSRGs = chk_removeExistingSRGs.isSelected();

                NetPlan netPlan = callback.getDesign();

                try {
                    SharedRiskModel srgModel = null;
                    if (e.getSource() == onePerNode) srgModel = SRGUtils.SharedRiskModel.PER_NODE;
                    else if (e.getSource() == onePerLink) srgModel = SRGUtils.SharedRiskModel.PER_LINK;
                    else if (e.getSource() == onePerLinkBundle)
                        srgModel = SRGUtils.SharedRiskModel.PER_DIRECTIONAL_LINK_BUNDLE;
                    else if (e.getSource() == onePerBidiLinkBundle)
                        srgModel = SRGUtils.SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE;

                    if (srgModel == null) throw new RuntimeException("Bad");
                    SRGUtils.configureSRGs(netPlan, mttf, mttr, srgModel, removeExistingSRGs);
                	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.SRG) , null , null);
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add SRGs from model");
                }

            }
        };

        onePerNode.addActionListener(srgModel);
        onePerLink.addActionListener(srgModel);
        onePerLinkBundle.addActionListener(srgModel);
        onePerBidiLinkBundle.addActionListener(srgModel);


        return options;
    }

    private List<JComponent> getExtraOptions(final int row, final Object itemId) {
        List<JComponent> options = new LinkedList<JComponent>();

        final int numRows = model.getRowCount();
        final NetPlan netPlan = callback.getDesign();

        if (itemId != null) {
            JMenuItem editSRG = new JMenuItem("View/edit SRG");
            editSRG.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        viewEditSRGGUI(callback, (long) itemId);
                    	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.SRG) , null , null);
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error viewing/editing SRG");
                    }
                }
            });

            options.add(editSRG);
        }

        if (numRows > 1) {
            if (!options.isEmpty()) options.add(new JPopupMenu.Separator());

            JMenuItem mttfValue = new JMenuItem("Set MTTF to all");
            mttfValue.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double mttf;

                    while (true) {
                        String str = JOptionPane.showInputDialog(null, "MTTF (in hours, zero or negative value means no failure)", "Set MTTF to all SRGs", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try {
                            mttf = Double.parseDouble(str);
                            break;
                        } catch (NumberFormatException ex) {
                            ErrorHandling.showErrorDialog("Non-valid MTTF value", "Error setting MTTF value");
                        } catch (Throwable ex) {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error setting MTTF");
                        }
                    }

                    NetPlan netPlan = callback.getDesign();

                    try {
                        for (SharedRiskGroup srg : netPlan.getSRGs()) srg.setMeanTimeToFailInHours(mttf);
                    	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.SRG) , null , null);
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set MTTF to all SRGs");
                    }
                }
            });

            options.add(mttfValue);

            JMenuItem mttrValue = new JMenuItem("Set MTTR to all");
            mttrValue.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double mttr;

                    while (true) {
                        String str = JOptionPane.showInputDialog(null, "MTTR (in hours)", "Set MTTR to all SRGs", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try {
                            mttr = Double.parseDouble(str);
                            if (mttr <= 0) throw new NumberFormatException();

                            break;
                        } catch (NumberFormatException ex) {
                            ErrorHandling.showErrorDialog("Non-valid MTTR value. Please, introduce a non-zero non-negative number", "Error setting MTTR value");
                        } catch (Throwable ex) {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error setting MTTR");
                        }
                    }

                    NetPlan netPlan = callback.getDesign();

                    try {
                        for (SharedRiskGroup srg : netPlan.getSRGs()) srg.setMeanTimeToRepairInHours(mttr);
                    	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.SRG) , null , null);
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set MTTR to all SRGs");
                    }
                }
            });

            options.add(mttrValue);
        }


        return options;
    }


    private static void viewEditSRGGUI(final IVisualizationCallback callback, final long srgId) {
        final NetPlan netPlan = callback.getDesign();
        final SharedRiskGroup srg = netPlan.getSRGFromId(srgId);
        callback.putColorInElementTopologyCanvas(srg.getNodes() , Color.ORANGE);
        callback.putColorInElementTopologyCanvas(srg.getLinksAllLayers() , Color.ORANGE);

        final int N = netPlan.getNumberOfNodes();
        final int E = netPlan.getNumberOfLinks();
        final Object[][] nodeData = new Object[N == 0 ? 1 : N][3];
        final Object[][] linkData = new Object[E == 0 ? 1 : E][4];

        if (N > 0) {
            int n = 0;
            Iterator<Node> nodeIt = netPlan.getNodes().iterator();
            while (nodeIt.hasNext()) {
                Node node = nodeIt.next();
                nodeData[n] = new Object[3];
                nodeData[n][0] = node.getId();
                nodeData[n][1] = node.getName();
                nodeData[n][2] = srg.getNodes().contains(node);

                n++;
            }
        }

        if (E > 0) {
            int e = 0;
            Iterator<Link> linkIt = netPlan.getLinks().iterator();
            while (linkIt.hasNext()) {
                Link link = linkIt.next();

                linkData[e] = new Object[4];
                linkData[e][0] = link.getId();
                linkData[e][1] = link.getOriginNode().getId() + (link.getOriginNode().getName().isEmpty() ? "" : " (" + link.getOriginNode().getName() + ")");
                linkData[e][2] = link.getDestinationNode().getId() + (link.getDestinationNode().getName().isEmpty() ? "" : " (" + link.getDestinationNode().getName() + ")");
                linkData[e][3] = srg.getLinksAllLayers().contains(link);

                e++;
            }
        }

        final DefaultTableModel nodeModel = new ClassAwareTableModel(nodeData, new String[]{"Id", "Name", "Included in the SRG"}) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 2;
            }

            @Override
            public void setValueAt(Object aValue, int row, int column) {
                if (column == 2) {
                    boolean value = (boolean) aValue;
                    Node node = netPlan.getNodeFromId((long) getValueAt(row, 0));
                    if (value && !srg.getNodes().contains(node)) 
                    {
                        netPlan.getSRGFromId(srgId).addNode(node);
                        callback.putColorInElementTopologyCanvas(srg.getNodes() , Color.ORANGE);
                        callback.putColorInElementTopologyCanvas(srg.getLinksAllLayers(), Color.ORANGE);
                    } else if (!value && srg.getNodes().contains(node)) 
                    {
                        netPlan.getSRGFromId(srgId).removeNode(node);
                        callback.putColorInElementTopologyCanvas(srg.getNodes() , Color.ORANGE);
                        callback.putColorInElementTopologyCanvas(srg.getLinksAllLayers(), Color.ORANGE);
                    }
                }

                super.setValueAt(aValue, row, column);
            }
        };

        final DefaultTableModel linkModel = new ClassAwareTableModel(linkData, new String[]{"Id", "Origin node", "Destination node", "Included in the SRG"}) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 3;
            }

            @Override
            public void setValueAt(Object aValue, int row, int column) {
                if (column == 3) {
                    boolean value = (boolean) aValue;
                    Link link = netPlan.getLinkFromId((long) getValueAt(row, 0));
                    if (value && !srg.getLinksAllLayers().contains(link)) {
                        srg.addLink(link);
                        callback.putColorInElementTopologyCanvas(srg.getNodes() , Color.ORANGE);
                        callback.putColorInElementTopologyCanvas(srg.getLinksAllLayers(), Color.ORANGE);
                    } else if (!value && srg.getLinksAllLayers().contains(link)) {
                        srg.removeLink(link);
                        callback.putColorInElementTopologyCanvas(srg.getNodes() , Color.ORANGE);
                        callback.putColorInElementTopologyCanvas(srg.getLinksAllLayers(), Color.ORANGE);
                    }
                }

                super.setValueAt(aValue, row, column);
            }
        };

        final JTable nodeTable = new AdvancedJTable(nodeModel);
        final JTable linkTable = new AdvancedJTable(linkModel);

        nodeTable.setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        nodeTable.setDefaultRenderer(Double.class, new UnfocusableCellRenderer());
        nodeTable.setDefaultRenderer(Object.class, new UnfocusableCellRenderer());
        nodeTable.setDefaultRenderer(Float.class, new UnfocusableCellRenderer());
        nodeTable.setDefaultRenderer(Long.class, new UnfocusableCellRenderer());
        nodeTable.setDefaultRenderer(Integer.class, new UnfocusableCellRenderer());
        nodeTable.setDefaultRenderer(String.class, new UnfocusableCellRenderer());

        linkTable.setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        linkTable.setDefaultRenderer(Double.class, new UnfocusableCellRenderer());
        linkTable.setDefaultRenderer(Object.class, new UnfocusableCellRenderer());
        linkTable.setDefaultRenderer(Float.class, new UnfocusableCellRenderer());
        linkTable.setDefaultRenderer(Long.class, new UnfocusableCellRenderer());
        linkTable.setDefaultRenderer(Integer.class, new UnfocusableCellRenderer());
        linkTable.setDefaultRenderer(String.class, new UnfocusableCellRenderer());

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

    private List<JComponent> getForcedOptions() {
        return new LinkedList<JComponent>();
    }
}
