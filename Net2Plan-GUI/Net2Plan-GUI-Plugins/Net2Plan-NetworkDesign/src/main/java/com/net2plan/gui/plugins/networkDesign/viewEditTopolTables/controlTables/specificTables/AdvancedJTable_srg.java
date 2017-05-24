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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.CellRenderers;
import com.net2plan.gui.plugins.networkDesign.ElementSelection;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AggregationUtils;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.LastRowAggregatedValue;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.JScrollPopupMenu;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.SRGUtils.SharedRiskModel;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Constants;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.SwingUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
@SuppressWarnings("unchecked")
public class AdvancedJTable_srg extends AdvancedJTable_networkElement
{
    private static final String netPlanViewTabName = "Shared-risk groups";
    private static final String[] netPlanViewTableHeader = StringUtils.arrayOf("Unique identifier", "Index", "MTTF (days)", "MTTR (days)", "Availability",
            "Nodes", "Links", "Links (other layers)", "# Affected routes", "# Affected backup routes", "# Affected multicast trees", "Tags", "Attributes");
    private static final String[] netPlanViewTableTips = StringUtils.arrayOf("Unique identifier (never repeated in the same netPlan object, never changes, long)",
            "Index (consecutive integer starting in zero)", "Mean time to fail", "Mean time to repair", "Expected availability", "Nodes included into the shared-risk group", "Links (in this layer) included into the shared-risk group", "Links (in other layers) included into the shared-risk group", "# Affected routes in this layer (primary or backup)", "# Affected routes in this layer that are designated as backup routes", "# Affected multicast trees in this layer ", "Tags", "Attributes");
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
    private static final int COLUMN_TAGS = 11;
    private static final int COLUMN_ATTRIBUTES = 12;
    private static int MAXNUMDECIMALSINAVAILABILITY = 7;

    public AdvancedJTable_srg(final GUINetworkDesign callback)
    {
        super(createTableModel(callback), callback, NetworkElementType.SRG);
        setDefaultCellRenderers(callback);
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
    }

    public List<Object[]> getAllData(NetPlan currentState, ArrayList<String> attributesColumns)
    {
        final NetworkLayer layer = currentState.getNetworkLayerDefault();
        final List<SharedRiskGroup> rowVisibleSRGs = getVisibleElementsInTable();
        List<Object[]> allSRGData = new LinkedList<Object[]>();
        final double[] dataAggregator = new double[netPlanViewTableHeader.length];

        for (SharedRiskGroup srg : rowVisibleSRGs)
        {
            final Set<Route> routeIds_thisSRG = currentState.getRoutingType() == Constants.RoutingType.SOURCE_ROUTING ? srg.getAffectedRoutes(layer) : new LinkedHashSet<Route>();
            final Set<Route> segmentIds_thisSRG = currentState.getRoutingType() == Constants.RoutingType.SOURCE_ROUTING ? srg.getAffectedRoutes(layer).stream().filter(e -> e.isBackupRoute()).collect(Collectors.toSet()) : new LinkedHashSet<Route>();
            final Set<MulticastTree> treeIds_thisSRG = currentState.getRoutingType() == Constants.RoutingType.SOURCE_ROUTING ? srg.getAffectedMulticastTrees(layer) : new LinkedHashSet<MulticastTree>();
            final int numRoutes = routeIds_thisSRG.size();
            final int numSegments = segmentIds_thisSRG.size();
            final int numMulticastTrees = treeIds_thisSRG.size();
            final Set<Node> nodeIds_thisSRG = srg.getNodes();
            final Set<Link> linkIds_thisSRG = srg.getLinks(layer);

            Object[] srgData = new Object[netPlanViewTableHeader.length + attributesColumns.size()];
            srgData[COLUMN_ID] = srg.getId();
            srgData[COLUMN_INDEX] = srg.getIndex();
            srgData[COLUMN_MTTF] = srg.getMeanTimeToFailInHours() / 24;
            srgData[COLUMN_MTTR] = srg.getMeanTimeToRepairInHours() / 24;
            srgData[COLUMN_AVAILABILITY] = srg.getAvailability();
            srgData[COLUMN_NODES] = nodeIds_thisSRG.isEmpty() ? "none" : CollectionUtils.join(NetPlan.getIndexes(nodeIds_thisSRG), ", ");
            srgData[COLUMN_LINKS] = linkIds_thisSRG.isEmpty() ? "none" : CollectionUtils.join(NetPlan.getIndexes(srg.getLinksAllLayers()), ", ");
            srgData[COLUMN_LINKSOTHERLAYERS] = srg.getLinks(layer).isEmpty() ? "none" : CollectionUtils.join(NetPlan.getIndexes(srg.getLinks(layer)), ", ");
            srgData[COLUMN_AFFECTEDROUTES] = numRoutes;
            srgData[COLUMN_AFFECTEDBACKUPROUTES] = numSegments;
            srgData[COLUMN_AFFECTEDTREES] = numMulticastTrees;
            srgData[COLUMN_TAGS] = StringUtils.listToString(Lists.newArrayList(srg.getTags()));
            srgData[COLUMN_ATTRIBUTES] = StringUtils.mapToString(srg.getAttributes());

            for (int i = netPlanViewTableHeader.length; i < netPlanViewTableHeader.length + attributesColumns.size(); i++)
            {
                if (srg.getAttributes().containsKey(attributesColumns.get(i - netPlanViewTableHeader.length)))
                {
                    srgData[i] = srg.getAttribute(attributesColumns.get(i - netPlanViewTableHeader.length));
                }
            }

            AggregationUtils.updateRowSum(dataAggregator, COLUMN_NODES, nodeIds_thisSRG.size());
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_LINKS, linkIds_thisSRG.size());
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_LINKSOTHERLAYERS, srg.getLinks(layer).size());
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_AFFECTEDROUTES, srgData[COLUMN_AFFECTEDROUTES]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_AFFECTEDBACKUPROUTES, srgData[COLUMN_AFFECTEDBACKUPROUTES]);
            AggregationUtils.updateRowSum(dataAggregator, COLUMN_AFFECTEDTREES, srgData[COLUMN_AFFECTEDTREES]);

            allSRGData.add(srgData);
        }

        /* Add the aggregation row with the aggregated statistics */
        final LastRowAggregatedValue[] aggregatedData = new LastRowAggregatedValue[netPlanViewTableHeader.length + attributesColumns.size()];
        Arrays.fill(aggregatedData, new LastRowAggregatedValue());
        aggregatedData[COLUMN_NODES] = new LastRowAggregatedValue(dataAggregator[COLUMN_NODES]);
        aggregatedData[COLUMN_LINKS] = new LastRowAggregatedValue(dataAggregator[COLUMN_LINKS]);
        aggregatedData[COLUMN_LINKSOTHERLAYERS] = new LastRowAggregatedValue(dataAggregator[COLUMN_LINKSOTHERLAYERS]);
        aggregatedData[COLUMN_AFFECTEDROUTES] = new LastRowAggregatedValue(dataAggregator[COLUMN_AFFECTEDROUTES]);
        aggregatedData[COLUMN_AFFECTEDBACKUPROUTES] = new LastRowAggregatedValue(dataAggregator[COLUMN_AFFECTEDBACKUPROUTES]);
        aggregatedData[COLUMN_AFFECTEDTREES] = new LastRowAggregatedValue(dataAggregator[COLUMN_AFFECTEDTREES]);
        allSRGData.add(aggregatedData);

        return allSRGData;
    }

    public String getTabName()
    {
        return netPlanViewTabName;
    }

    public String[] getTableHeaders()
    {
        return netPlanViewTableHeader;
    }

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


    public String[] getTableTips()
    {
        return netPlanViewTableTips;
    }

    public boolean hasElements()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
        return rf == null ? callback.getDesign().hasSRGs() : rf.hasSRGs(layer);
    }

    @Override
    public int getAttributesColumnIndex()
    {
        return COLUMN_ATTRIBUTES;
    }

    private static TableModel createTableModel(final GUINetworkDesign callback)
    {
        TableModel srgTableModel = new ClassAwareTableModel(new Object[1][netPlanViewTableHeader.length], netPlanViewTableHeader)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                if (!callback.getVisualizationState().isNetPlanEditable()) return false;
                if (columnIndex >= netPlanViewTableHeader.length) return true;
                if (getValueAt(rowIndex, columnIndex) == null) return false;
                if (rowIndex == getRowCount() - 1) return false;

                return columnIndex == COLUMN_MTTF || columnIndex == COLUMN_MTTR || columnIndex >= netPlanViewTableHeader.length;
            }

            @Override
            public void setValueAt(Object newValue, int row, int column)
            {
                Object oldValue = getValueAt(row, column);

                if (newValue.equals(oldValue)) return;

                NetPlan netPlan = callback.getDesign();

                if (getValueAt(row, 0) == null) row = row - 1;
                final long srgId = (Long) getValueAt(row, 0);
                final SharedRiskGroup srg = netPlan.getSRGFromId(srgId);

				/* Perform checks, if needed */
                try
                {
                    switch (column)
                    {
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
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying SRG");
                    return;
                }

				/* Set new value */
                super.setValueAt(newValue, row, column);
            }
        };
        return srgTableModel;
    }

    private void setDefaultCellRenderers(final GUINetworkDesign callback)
    {
        setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        setDefaultRenderer(Double.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer());
        setDefaultRenderer(Float.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Long.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer());
        setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer());
    }

    private void setSpecificCellRenderers()
    {
        getColumnModel().getColumn(convertColumnIndexToView(COLUMN_AVAILABILITY)).setCellRenderer(new CellRenderers.NumberCellRenderer(MAXNUMDECIMALSINAVAILABILITY));
    }

    @Override
    public void setColumnRowSorting()
    {
        setAutoCreateRowSorter(true);
        final Set<Integer> columnsWithDoubleAndThenParenthesis = Sets.newHashSet(COLUMN_AFFECTEDROUTES, COLUMN_AFFECTEDBACKUPROUTES, COLUMN_AFFECTEDTREES);
        DefaultRowSorter rowSorter = ((DefaultRowSorter) getRowSorter());
        for (int col = 0; col <= COLUMN_ATTRIBUTES; col++)
            rowSorter.setComparator(col, new AdvancedJTable_networkElement.ColumnComparator(rowSorter, columnsWithDoubleAndThenParenthesis.contains(col)));
        fixedTable.setAutoCreateRowSorter(true);
        fixedTable.setRowSorter(this.getRowSorter());
        rowSorter = ((DefaultRowSorter) fixedTable.getRowSorter());
        for (int col = 0; col <= COLUMN_ATTRIBUTES; col++)
            rowSorter.setComparator(col, new AdvancedJTable_networkElement.ColumnComparator(rowSorter, columnsWithDoubleAndThenParenthesis.contains(col)));
    }

    public int getNumberOfDecoratorColumns()
    {
        return 2;
    }

    @Override
    public ArrayList<String> getAttributesColumnsHeaders()
    {
        ArrayList<String> attColumnsHeaders = new ArrayList<>();
        for (SharedRiskGroup srg : getVisibleElementsInTable())
            for (Map.Entry<String, String> entry : srg.getAttributes().entrySet())
                if (attColumnsHeaders.contains(entry.getKey()) == false)
                    attColumnsHeaders.add(entry.getKey());
        return attColumnsHeaders;
    }

    @Override
    public JPopupMenu getPopup(final ElementSelection selection)
    {
        assert selection != null;

        final JScrollPopupMenu popup = new JScrollPopupMenu(20);
        final List<SharedRiskGroup> rowsInTheTable = getVisibleElementsInTable();

        if (!selection.isEmpty())
        {
            if (selection.getElementType() != NetworkElementType.SRG)
                throw new RuntimeException("Unmatched items with table, selected items are of type: " + selection.getElementType());
        }

        /* Add the popup menu option of the filters */
        final List<SharedRiskGroup> selectedSRGs = (List<SharedRiskGroup>) selection.getNetworkElements();

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
                if (!selectedSRGs.isEmpty())
                {
                    JMenuItem removeItem = new JMenuItem("Remove selected SRGs");
                    removeItem.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            try
                            {
                                for (SharedRiskGroup selectedSRG : selectedSRGs) selectedSRG.remove();
                                callback.getVisualizationState().resetPickedState();
                                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.SRG));
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

            final List<JComponent> extraAddOptions = getExtraAddOptions();
            if (!extraAddOptions.isEmpty())
            {
                popup.addSeparator();
                for (JComponent item : extraAddOptions) popup.add(item);
            }

            if (!rowsInTheTable.isEmpty() && !selectedSRGs.isEmpty())
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
    public void pickSelection(ElementSelection selection)
    {
        if (getVisibleElementsInTable().isEmpty()) return;
        if (selection.getElementType() != NetworkElementType.SRG)
            throw new RuntimeException("Unmatched items with table, selected items are of type: " + selection.getElementType());

        callback.getVisualizationState().pickElement((List<SharedRiskGroup>) selection.getNetworkElements());
        callback.updateVisualizationAfterPick();
    }

    @Override
    protected boolean hasAttributes()
    {
        return true;
    }


    @Override
    protected JMenuItem getAddOption()
    {
        JMenuItem addItem = new JMenuItem("Add " + networkElementType);

        addItem.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                NetPlan netPlan = callback.getDesign();

                try
                {
                    netPlan.addSRG(8748, 12, null);
                    callback.getVisualizationState().resetPickedState();
                    callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.SRG));
                    callback.addNetPlanChange();
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElementType);
                }
            }
        });
        return addItem;
    }


    @Override
    protected List<JComponent> getExtraAddOptions()
    {
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

        if (!netPlan.hasNodes())
        {
            onePerNode.setEnabled(false);
        }

        if (!netPlan.hasLinks())
        {
            onePerLink.setEnabled(false);
            onePerLinkBundle.setEnabled(false);
            onePerBidiLinkBundle.setEnabled(false);
        }

        ActionListener srgModel = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
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

                while (true)
                {
                    int result = JOptionPane.showConfirmDialog(null, pane, "Add SRGs from model", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return;

                    try
                    {
                        mttf = Double.parseDouble(txt_mttf.getText());
                        mttr = Double.parseDouble(txt_mttr.getText());
                        if (mttr <= 0) throw new RuntimeException();

                        break;
                    } catch (Throwable ex)
                    {
                        ErrorHandling.showErrorDialog("MTTF/MTTR must be greater than zero", "Error adding SRGs from model");
                    }
                }

                boolean removeExistingSRGs = chk_removeExistingSRGs.isSelected();

                NetPlan netPlan = callback.getDesign();

                try
                {
                    SharedRiskModel srgModel = null;
                    if (e.getSource() == onePerNode) srgModel = SRGUtils.SharedRiskModel.PER_NODE;
                    else if (e.getSource() == onePerLink) srgModel = SRGUtils.SharedRiskModel.PER_LINK;
                    else if (e.getSource() == onePerLinkBundle)
                        srgModel = SRGUtils.SharedRiskModel.PER_DIRECTIONAL_LINK_BUNDLE;
                    else if (e.getSource() == onePerBidiLinkBundle)
                        srgModel = SRGUtils.SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE;

                    if (srgModel == null) throw new RuntimeException("Bad");
                    SRGUtils.configureSRGs(netPlan, mttf, mttr, srgModel, removeExistingSRGs);
                    callback.getVisualizationState().resetPickedState();
                    callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.SRG));
                    callback.addNetPlanChange();
                } catch (Throwable ex)
                {
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


    @Override
    protected List<JComponent> getExtraOptions(final ElementSelection selection)
    {
        assert selection != null;

        final List<SharedRiskGroup> selectedSRGs = (List<SharedRiskGroup>) selection.getNetworkElements();
        final List<SharedRiskGroup> rowsInTheTable = getVisibleElementsInTable();
        final List<JComponent> options = new LinkedList<JComponent>();

        if (!selectedSRGs.isEmpty() && selectedSRGs.size() == 1)
        {
            JMenuItem editSRG = new JMenuItem("View/edit SRG");
            editSRG.addActionListener(e ->
            {
                try
                {
                    viewEditSRGGUI(callback, selectedSRGs.get(0));

                    callback.getVisualizationState().resetPickedState();
                    callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.SRG));
                    callback.addNetPlanChange();
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error viewing/editing SRG");
                }
            });

            options.add(editSRG);
        }

        if (!rowsInTheTable.isEmpty())
        {
            if (!options.isEmpty()) options.add(new JPopupMenu.Separator());

            JMenuItem mttfValue = new JMenuItem("Set selected SRGs MTTF");
            mttfValue.addActionListener(e ->
            {
                double mttf;

                while (true)
                {
                    String str = JOptionPane.showInputDialog(null, "MTTF (in hours, zero or negative value means no failure)", "Set MTTF to selected SRGs", JOptionPane.QUESTION_MESSAGE);
                    if (str == null) return;

                    try
                    {
                        mttf = Double.parseDouble(str);
                        break;
                    } catch (NumberFormatException ex)
                    {
                        ErrorHandling.showErrorDialog("Non-valid MTTF value", "Error setting MTTF value");
                    } catch (Throwable ex)
                    {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error setting MTTF");
                    }
                }

                try
                {
                    for (SharedRiskGroup srg : selectedSRGs) srg.setMeanTimeToFailInHours(mttf);

                    callback.getVisualizationState().resetPickedState();
                    callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.SRG));
                    callback.addNetPlanChange();
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set MTTF to selected SRGs");
                }
            });

            options.add(mttfValue);

            JMenuItem mttrValue = new JMenuItem("Set selected SRGs MTTR");
            mttrValue.addActionListener(e ->
            {
                double mttr;

                while (true)
                {
                    String str = JOptionPane.showInputDialog(null, "MTTR (in hours)", "Set MTTR to selected SRGs", JOptionPane.QUESTION_MESSAGE);
                    if (str == null) return;

                    try
                    {
                        mttr = Double.parseDouble(str);
                        if (mttr <= 0) throw new NumberFormatException();

                        break;
                    } catch (NumberFormatException ex)
                    {
                        ErrorHandling.showErrorDialog("Non-valid MTTR value. Please, introduce a non-zero non-negative number", "Error setting MTTR value");
                    } catch (Throwable ex)
                    {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error setting MTTR");
                    }
                }

                try
                {
                    for (SharedRiskGroup srg : selectedSRGs) srg.setMeanTimeToRepairInHours(mttr);
                    callback.getVisualizationState().resetPickedState();
                    callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.SRG));
                    callback.addNetPlanChange();
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set MTTR to selected SRGs");
                }
            });

            options.add(mttrValue);
        }


        return options;
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

        nodeTable.setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        nodeTable.setDefaultRenderer(Double.class, new CellRenderers.UnfocusableCellRenderer());
        nodeTable.setDefaultRenderer(Object.class, new CellRenderers.UnfocusableCellRenderer());
        nodeTable.setDefaultRenderer(Float.class, new CellRenderers.UnfocusableCellRenderer());
        nodeTable.setDefaultRenderer(Long.class, new CellRenderers.UnfocusableCellRenderer());
        nodeTable.setDefaultRenderer(Integer.class, new CellRenderers.UnfocusableCellRenderer());
        nodeTable.setDefaultRenderer(String.class, new CellRenderers.UnfocusableCellRenderer());

        linkTable.setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer());
        linkTable.setDefaultRenderer(Double.class, new CellRenderers.UnfocusableCellRenderer());
        linkTable.setDefaultRenderer(Object.class, new CellRenderers.UnfocusableCellRenderer());
        linkTable.setDefaultRenderer(Float.class, new CellRenderers.UnfocusableCellRenderer());
        linkTable.setDefaultRenderer(Long.class, new CellRenderers.UnfocusableCellRenderer());
        linkTable.setDefaultRenderer(Integer.class, new CellRenderers.UnfocusableCellRenderer());
        linkTable.setDefaultRenderer(String.class, new CellRenderers.UnfocusableCellRenderer());

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


    @Override
    protected List<JComponent> getForcedOptions(ElementSelection selection)
    {
        return new LinkedList<>();
    }

    private List<SharedRiskGroup> getVisibleElementsInTable()
    {
        final ITableRowFilter rf = callback.getVisualizationState().getTableRowFilter();
        final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
        return rf == null ? callback.getDesign().getSRGs() : rf.getVisibleSRGs(layer);
    }
}
