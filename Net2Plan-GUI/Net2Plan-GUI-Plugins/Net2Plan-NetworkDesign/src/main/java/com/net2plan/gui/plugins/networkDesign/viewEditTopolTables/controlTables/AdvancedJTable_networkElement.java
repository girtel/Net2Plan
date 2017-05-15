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


package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.AttributeEditor;
import com.net2plan.gui.plugins.networkDesign.ElementSelection;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.gui.plugins.networkDesign.io.excel.ExcelWriter;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.specificTables.AdvancedJTable_forwardingRule;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters.TBFSelectionBased;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters.TBFTagBased;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableVisualizationFilters.TBFToFromCarriedTraffic;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ColumnHeaderToolTips;
import com.net2plan.gui.utils.FixedColumnDecorator;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;

import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

import static com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter.FilterCombinationType;


/**
 * <p>Extended version of the {@code JTable} class. It presents the following
 * additional features:</p>
 * <ul>
 * <li>Reordering of table columns is not allowed</li>
 * <li>Auto-resize of columns is disabled</li>
 * <li>It allows to set per-cell editors</li>
 * <li>It allows to navigate the table with the cursor</li>
 * <li>It allows to configure if all cell contents are selected when editing or typing ('true' by default, using 'setSelectAllXXX()' methods to customize)</li>
 * </ul>
 * <p>Credits to Santhosh Kumar for his methods to solve partially visible cell
 * issues (<a href='http://www.jroller.com/santhosh/entry/partially_visible_tablecells'>Partially Visible TableCells</a>)</p>
 * <p>Credits to "Kah - The Developer" for his static method to set column widths
 * in proportion to each other (<a href='http://kahdev.wordpress.com/2011/10/30/java-specifying-the-column-widths-of-a-jtable-as-percentages/'>Specifying the column widths of a JTable as percentages</a>)
 * </p>
 * <p>Credits to Rob Camick for his 'select all' editing feature for {@code JTable}
 * (<a href='https://tips4java.wordpress.com/2008/10/20/table-select-all-editor/'>Table Select All Editor</a>)
 * </p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
@SuppressWarnings("unchecked")
public abstract class AdvancedJTable_networkElement extends AdvancedJTable
{
    protected final NetworkElementType networkElementType;
    protected final GUINetworkDesign callback;
    protected final TableModel model;

    protected final JTable mainTable;
    protected final JTable fixedTable;

    private final JScrollPane scrollPane;
    private final TableViewController tableController;
    private final FixedColumnDecorator decorator;

    /**
     * Constructor that allows to set the table model.
     *
     * @param model              Table model
     * @param networkViewer      Network callback
     * @param networkElementType Network element type
     * @since 0.2.0
     */
    public AdvancedJTable_networkElement(TableModel model, final GUINetworkDesign networkViewer, NetworkElementType networkElementType)
    {
        super(model);

        this.model = model;
        this.callback = networkViewer;
        this.networkElementType = networkElementType;

        this.setTips();
        this.addMouseListener(new PopupMenuMouseAdapter());
        this.addKeyboardActions();

        this.scrollPane = new JScrollPane(this);
        this.decorator = new FixedColumnDecorator(scrollPane, getNumberOfDecoratorColumns());
        this.mainTable = decorator.getMainTable();
        this.fixedTable = decorator.getFixedTable();

        this.setRowSelectionAllowed(true);
        this.getTableHeader().setReorderingAllowed(true);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        this.tableController = new TableViewController(callback, this);
    }

    private void addKeyboardActions()
    {
        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.VK_UNDEFINED), "pickElements");
        this.getActionMap().put("pickElements", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                final ElementSelection selectedElements = getSelectedElements();
                if (selectedElements.isEmpty()) return;
                SwingUtilities.invokeLater(() -> pickSelection(selectedElements));
            }
        });
    }

    public JScrollPane getScrollPane()
    {
        return scrollPane;
    }

    public NetworkElementType getNetworkElementType()
    {
        return networkElementType;
    }

    protected JTable getMainTable()
    {
        return mainTable;
    }

    protected JTable getFixedTable()
    {
        return fixedTable;
    }

    public void updateView(NetPlan currentState)
    {
        this.setEnabled(false);

        tableController.saveColumnsPositionsAndWidths();

        if (currentState.getRoutingType() == RoutingType.SOURCE_ROUTING && networkElementType.equals(NetworkElementType.FORWARDING_RULE))
            return;
        if (currentState.getRoutingType() == RoutingType.HOP_BY_HOP_ROUTING && (networkElementType.equals(NetworkElementType.ROUTE)))
            return;

        final List<? extends SortKey> sortKeys = this.getRowSorter().getSortKeys();
        final String[] tableHeaders = getCurrentTableHeaders();

        if (hasElements())
        {
            ArrayList<String> attColumnsHeaders = getAttributesColumnsHeaders();

            List<Object[]> allData = getAllData(currentState, attColumnsHeaders);
            ((DefaultTableModel) getModel()).setDataVector(allData.toArray(new Object[allData.size()][tableHeaders.length]), tableHeaders);
            this.createDefaultColumnsFromModel();

            if (attColumnsHeaders != null && networkElementType != NetworkElementType.FORWARDING_RULE)
            {

                this.setTips();

                if (tableController.isAttributeExpanded())
                    tableController.removeNewColumn("Attributes");
                else if (attColumnsHeaders.size() > 0)
                    for (String att : attColumnsHeaders)
                        tableController.removeNewColumn("Att: " + att);

                tableController.updateTables();

                tableController.restoreColumnsPositionsAndWidths();

                if (tableController.isAttributeExpanded())
                {
                    for (TableColumn col : tableController.getHiddenColumns())
                    {
                        String columnName = col.getHeaderValue().toString();

                        for (String att : getAttributesColumnsHeaders())
                        {
                            if (columnName.equals("Att: " + att))
                            {
                                tableController.showColumn("Att: " + att, 0, false);
                            }
                        }
                    }
                }
            }
            setColumnRowSorting();
        } else
        {
            ((DefaultTableModel) getModel()).setDataVector(new Object[1][tableHeaders.length], tableHeaders);
        }

        this.getRowSorter().setSortKeys(sortKeys);

        setEnabled(true);
    }

    protected final void addPopupMenuAttributeOptions(ElementSelection selection, JPopupMenu popup)
    {
        assert popup != null;
        assert selection != null;

        if (networkElementType == NetworkElementType.FORWARDING_RULE) return;

        final List<? extends NetworkElement> selectedElements = selection.getNetworkElements();

        if (!selectedElements.isEmpty())
        {
            popup.addSeparator();

            // Tags controls
            JMenuItem addTag = new JMenuItem("Add tag" + (networkElementType == NetworkElementType.LAYER ? "" : " to selected elements"));
            addTag.addActionListener(e1 ->
            {
                JTextField txt_name = new JTextField(20);

                JPanel pane = new JPanel();
                pane.add(new JLabel("Tag: "));
                pane.add(txt_name);

                while (true)
                {
                    int result = JOptionPane.showConfirmDialog(null, pane, "Please enter tag name", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return;
                    String tag;
                    try
                    {
                        if (txt_name.getText().isEmpty()) continue;

                        tag = txt_name.getText();

                        for (NetworkElement selectedElement : selectedElements)
                            selectedElement.addTag(tag);

                        callback.updateVisualizationJustTables();
                    } catch (Throwable ex)
                    {
                        ErrorHandling.addErrorOrException(ex, getClass());
                        ErrorHandling.showErrorDialog("Error adding/editing tag");
                    }
                    break;
                }
            });
            popup.add(addTag);

            JMenuItem removeTag = new JMenuItem("Remove tag" + (networkElementType == NetworkElementType.LAYER ? "" : " from selected elements"));

            removeTag.addActionListener(e1 ->
            {
                try
                {
                    final Set<String> tags = new HashSet<>();
                    for (NetworkElement selectedElement : selectedElements)
                    {
                        final Set<String> elementTags = selectedElement.getTags();
                        tags.addAll(elementTags);
                    }

                    String[] tagList = StringUtils.toArray(tags);

                    if (tagList.length == 0) throw new Exception("No tag to remove");

                    Object out = JOptionPane.showInputDialog(null, "Please, select a tag to remove", "Remove tag", JOptionPane.QUESTION_MESSAGE, null, tagList, tagList[0]);
                    if (out == null) return;

                    String tagToRemove = out.toString();

                    for (NetworkElement selectedElement : selectedElements)
                        selectedElement.removeTag(tagToRemove);

                    callback.updateVisualizationJustTables();

                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing tag");
                }
            });

            popup.add(removeTag);

            JMenuItem addAttribute = new JMenuItem("Add/Update attribute" + (networkElementType == NetworkElementType.LAYER ? "" : " to selected elements"));
            popup.add(new JPopupMenu.Separator());
            addAttribute.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    JTextField txt_key = new JTextField(20);
                    JTextField txt_value = new JTextField(20);

                    JPanel pane = new JPanel();
                    pane.add(new JLabel("Attribute: "));
                    pane.add(txt_key);
                    pane.add(Box.createHorizontalStrut(15));
                    pane.add(new JLabel("Value: "));
                    pane.add(txt_value);

                    while (true)
                    {
                        int result = JOptionPane.showConfirmDialog(null, pane, "Please enter an attribute name and its value", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (result != JOptionPane.OK_OPTION) return;
                        String attribute, value;
                        try
                        {
                            if (txt_key.getText().isEmpty())
                            {
                                ErrorHandling.showWarningDialog("Please, insert an attribute name.", "Message");
                                continue;
                            }

                            attribute = txt_key.getText();
                            value = txt_value.getText();

                            for (NetworkElement selectedElement : selectedElements)
                                selectedElement.setAttribute(attribute, value);

                            callback.updateVisualizationJustTables();
                        } catch (Throwable ex)
                        {
                            ErrorHandling.addErrorOrException(ex, getClass());
                            ErrorHandling.showErrorDialog("Error adding/editing attribute");
                        }
                        break;
                    }
                }
            });
            popup.add(addAttribute);

            JMenuItem removeAttribute = new JMenuItem("Remove attribute" + (networkElementType == NetworkElementType.LAYER ? "" : " from selected elements"));

            removeAttribute.addActionListener(e1 ->
            {
                try
                {
                    final Set<String> attributes = new HashSet<>();
                    for (NetworkElement selectedElement : selectedElements)
                    {
                        attributes.addAll(selectedElement.getAttributes().keySet());
                    }

                    String[] attributeList = StringUtils.toArray(attributes);

                    if (attributeList.length == 0) throw new Exception("No attribute to remove");

                    Object out = JOptionPane.showInputDialog(null, "Please, select an attribute to remove", "Remove attribute", JOptionPane.QUESTION_MESSAGE, null, attributeList, attributeList[0]);
                    if (out == null) return;

                    String attributeToRemove = out.toString();

                    for (NetworkElement selectedElement : selectedElements)
                        selectedElement.removeAttribute(attributeToRemove);

                    callback.updateVisualizationJustTables();
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing attribute");
                }
            });
            popup.add(removeAttribute);

            JMenuItem removeAttributes = new JMenuItem("Remove all attributes" + (networkElementType == NetworkElementType.LAYER ? "" : " from selected elements"));
            removeAttributes.addActionListener(e1 ->
            {
                try
                {
                    for (NetworkElement selectedElement : selectedElements)
                        selectedElement.removeAllAttributes();

                    if (tableController.isAttributeExpanded())
                    {
                        tableController.recoverRemovedColumn("Attributes");
                    }

                    callback.updateVisualizationJustTables();
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing attributes");
                }
            });

            popup.add(removeAttributes);
            popup.addSeparator();

            JMenuItem editAttributes = new JMenuItem("Edit attributes" + (networkElementType == NetworkElementType.LAYER ? "" : " from selected elements"));
            editAttributes.addActionListener(e1 ->
            {
                try
                {
                    JDialog dialog = new AttributeEditor(callback, selection);
                    dialog.setVisible(true);
                    callback.updateVisualizationJustTables();
                } catch (Throwable ex)
                {
                    ex.printStackTrace();
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying attributes");
                }
            });

            popup.add(editAttributes);
        }
    }

    protected static class ColumnComparator implements Comparator<Object>
    {
        private final boolean isDoubleWithParenthesis;
        private final RowSorter rs;

        public ColumnComparator(RowSorter rs, boolean isDoubleWithParenthesis)
        {
            this.rs = rs;
            this.isDoubleWithParenthesis = isDoubleWithParenthesis;
        }

        @Override
        public int compare(Object o1, Object o2)
        {

            if (o1 instanceof LastRowAggregatedValue)
            {
                final boolean ascending = ((List<? extends SortKey>) rs.getSortKeys()).get(0).getSortOrder() == SortOrder.ASCENDING;
                return ascending ? 1 : -1;
            }
            if (o2 instanceof LastRowAggregatedValue)
            {
                final boolean ascending = ((List<? extends SortKey>) rs.getSortKeys()).get(0).getSortOrder() == SortOrder.ASCENDING;
                return ascending ? -1 : 1;
            }
            if (o1 instanceof Boolean)
            {
                final Boolean oo1 = (Boolean) o1;
                final Boolean oo2 = (Boolean) o2;
                return oo1.compareTo(oo2);
            }
            if (o1 instanceof Number)
            {
                final Number oo1 = (Number) o1;
                final Number oo2 = (Number) o2;
                return new Double(oo1.doubleValue()).compareTo(new Double(oo2.doubleValue()));
            }
            String oo1 = (String) o1;
            String oo2 = (String) o2;
            if (!isDoubleWithParenthesis)
                return oo1.compareTo(oo2);

            int pos1 = oo1.indexOf(" (");
            if (pos1 != -1) oo1 = oo1.substring(0, pos1);

            int pos2 = oo2.indexOf(" (");
            if (pos2 != -1) oo2 = oo2.substring(0, pos2);

            try
            {
                Double d1, d2;
                d1 = Double.parseDouble(oo1);
                d2 = Double.parseDouble(oo2);
                return d1.compareTo(d2);
            } catch (Throwable e)
            {
                return oo1.compareTo(oo2);
            }
        }
    }

    /**
     * Gets the selected elements in this table.
     *
     * @return
     */
    private ElementSelection getSelectedElements()
    {
        final int[] rowViewIndexes = this.getSelectedRows();
        final NetPlan np = callback.getDesign();

        final List<NetworkElement> elementList = new ArrayList<>();
        final List<Pair<Demand, Link>> frList = new ArrayList<>();

        if (rowViewIndexes.length != 0)
        {
            final int maxValidRowIndex = model.getRowCount() - 1 - (hasAggregationRow() ? 1 : 0);
            final List<Integer> validRows = new ArrayList<Integer>();
            for (int a : rowViewIndexes) if ((a >= 0) && (a <= maxValidRowIndex)) validRows.add(a);

            if (networkElementType == NetworkElementType.FORWARDING_RULE)
            {
                for (int rowViewIndex : validRows)
                {
                    final int viewRowIndex = this.convertRowIndexToModel(rowViewIndex);
                    final String demandInfo = (String) getModel().getValueAt(viewRowIndex, AdvancedJTable_forwardingRule.COLUMN_DEMAND);
                    final String linkInfo = (String) getModel().getValueAt(viewRowIndex, AdvancedJTable_forwardingRule.COLUMN_OUTGOINGLINK);
                    final int demandIndex = Integer.parseInt(demandInfo.substring(0, demandInfo.indexOf("(")).trim());
                    final int linkIndex = Integer.parseInt(linkInfo.substring(0, linkInfo.indexOf("(")).trim());
                    frList.add(Pair.of(np.getDemand(demandIndex), np.getLink(linkIndex)));
                }
            } else
            {
                for (int rowViewIndex : validRows)
                {
                    final int viewRowIndex = this.convertRowIndexToModel(rowViewIndex);
                    final long id = (long) getModel().getValueAt(viewRowIndex, 0);
                    elementList.add(np.getNetworkElement(id));
                }
            }
        }

        // Parse into ElementSelection
        final Pair<List<NetworkElement>, List<Pair<Demand, Link>>> selection = Pair.of(elementList, frList);
        final boolean nothingSelected = selection.getFirst().isEmpty() && selection.getSecond().isEmpty();

        // Checking for selection type
        final ElementSelection elementHolder;

        if (!nothingSelected)
        {
            if (!selection.getFirst().isEmpty())
                elementHolder = new ElementSelection(NetworkElementType.getType(selection.getFirst()), selection.getFirst());
            else if (!selection.getSecond().isEmpty())
                elementHolder = new ElementSelection(selection.getSecond());
            else elementHolder = new ElementSelection();
        } else
        {
            elementHolder = new ElementSelection();
        }

        return elementHolder;
    }


    public boolean hasAggregationRow()
    {
        return !networkElementType.equals(NetworkElementType.LAYER) && !networkElementType.equals(NetworkElementType.NETWORK);
    }

    /* Dialog for filtering by tag */
    protected void dialogToFilterByTag(boolean onlyInActiveLayer, FilterCombinationType filterCombinationType)
    {
        JTextField txt_tagContains = new JTextField(30);
        JTextField txt_tagDoesNotContain = new JTextField(30);
        JPanel pane = new JPanel(new GridLayout(-1, 1));
        pane.add(new JLabel("Has tag (could be empty): "));
        pane.add(txt_tagContains);
        pane.add(new JLabel("AND does not have tag (could be empty): "));
        pane.add(txt_tagDoesNotContain);

        while (true)
        {
            int result = JOptionPane.showConfirmDialog(null, pane, "Filter elements by tag", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
            try
            {
                if (txt_tagContains.getText().isEmpty() && txt_tagDoesNotContain.getText().isEmpty())
                {
                    ErrorHandling.showErrorDialog("At least one input tag is required", "Invalid input");
                    continue;
                }

                final ITableRowFilter filter = new TBFTagBased(
                        callback.getDesign(), onlyInActiveLayer ? callback.getDesign().getNetworkLayerDefault() : null,
                        txt_tagContains.getText(), txt_tagDoesNotContain.getText());
                callback.getVisualizationState().updateTableRowFilter(filter, filterCombinationType);
                callback.updateVisualizationJustTables();
            } catch (Throwable ex)
            {
                ErrorHandling.addErrorOrException(ex, getClass());
                ErrorHandling.showErrorDialog("Error adding filter");
            }
            break;
        }
    }

    protected void addFilterOptions(ElementSelection selection, JPopupMenu popup)
    {
        final NetPlan netPlan = callback.getDesign();
        final VisualizationState vs = callback.getVisualizationState();
        final boolean isMultilayerDesign = netPlan.isMultilayer();

        for (boolean applyJustToThisLayer : isMultilayerDesign ? new boolean[]{true, false} : new boolean[]{true})
        {
            final JMenu submenuFilters;
            if (applyJustToThisLayer)
                submenuFilters = new JMenu("Filters: Apply to this layer");
            else
                submenuFilters = new JMenu("Filters: Apply to all layers");

            for (FilterCombinationType filterCombinationType : vs.getTableRowFilter() == null ? new FilterCombinationType[]{FilterCombinationType.INCLUDEIF_AND} : FilterCombinationType.values())
            {
                final JMenu filterCombinationSubMenu;
                switch (filterCombinationType)
                {
                    case INCLUDEIF_OR:
                        filterCombinationSubMenu = new JMenu("Add elements that...");
                        break;
                    case INCLUDEIF_AND:
                        filterCombinationSubMenu = new JMenu("Keep elements that...");
                        break;
                    default:
                        throw new RuntimeException();
                }

                final JMenuItem trafficBasedFilterMenu = new JMenuItem("Are affected by these " + networkElementType + "s");
                filterCombinationSubMenu.add(trafficBasedFilterMenu);
                trafficBasedFilterMenu.addActionListener(e1 ->
                {
                    if (selection.isEmpty()) return;

                    TBFToFromCarriedTraffic filter = null;
                    final List<? extends NetworkElement> selectedElements = selection.getNetworkElements();
                    final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
                    for (NetworkElement element : selectedElements)
                    {
                        final NetworkElementType type = NetworkElementType.getType(element);
                        if (type != null)
                        {

                            switch (type)
                            {
                                case NODE:
                                    if (filter == null)
                                        filter = new TBFToFromCarriedTraffic((Node) element, layer, applyJustToThisLayer);
                                    else
                                        filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((Node) element, layer, applyJustToThisLayer));
                                    break;
                                case LINK:
                                    if (filter == null)
                                        filter = new TBFToFromCarriedTraffic((Link) element, applyJustToThisLayer);
                                    else
                                        filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((Link) element, applyJustToThisLayer));
                                    break;
                                case DEMAND:
                                    if (filter == null)
                                        filter = new TBFToFromCarriedTraffic((Demand) element, applyJustToThisLayer);
                                    else
                                        filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((Demand) element, applyJustToThisLayer));
                                    break;
                                case MULTICAST_DEMAND:
                                    if (filter == null)
                                        filter = new TBFToFromCarriedTraffic((MulticastDemand) element, applyJustToThisLayer);
                                    else
                                        filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((MulticastDemand) element, applyJustToThisLayer));
                                    break;
                                case ROUTE:
                                    if (filter == null)
                                        filter = new TBFToFromCarriedTraffic((Route) element, applyJustToThisLayer);
                                    else
                                        filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((Route) element, applyJustToThisLayer));
                                    break;
                                case MULTICAST_TREE:
                                    if (filter == null)
                                        filter = new TBFToFromCarriedTraffic((MulticastTree) element, applyJustToThisLayer);
                                    else
                                        filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((MulticastTree) element, applyJustToThisLayer));
                                    break;
                                case RESOURCE:
                                    if (filter == null)
                                        filter = new TBFToFromCarriedTraffic((Resource) element, layer, applyJustToThisLayer);
                                    else
                                        filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((Resource) element, layer, applyJustToThisLayer));
                                    break;
                                case SRG:
                                    if (filter == null)
                                        filter = new TBFToFromCarriedTraffic((SharedRiskGroup) element);
                                    else
                                        filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic((SharedRiskGroup) element));
                                    break;
                                case FORWARDING_RULE:
                                    final List<Pair<Demand, Link>> forwardingRules = selection.getForwardingRules();

                                    for (Pair<Demand, Link> forwardingRule : forwardingRules)
                                    {
                                        if (filter == null)
                                            filter = new TBFToFromCarriedTraffic(forwardingRule, applyJustToThisLayer);
                                        else
                                            filter.recomputeApplyingShowIf_ThisOrThat(new TBFToFromCarriedTraffic(forwardingRule, applyJustToThisLayer));
                                    }
                                    break;
                                default:
                                    // TODO: Control exceptions on filters.
                                    throw new RuntimeException();
                            }
                        }
                    }

                    callback.getVisualizationState().updateTableRowFilter(filter, filterCombinationType);
                    callback.updateVisualizationJustTables();
                });
                final JMenuItem tagFilterMenu = new JMenuItem("Have tag...");
                filterCombinationSubMenu.add(tagFilterMenu);
                tagFilterMenu.addActionListener(e1 -> dialogToFilterByTag(applyJustToThisLayer, filterCombinationType));

                submenuFilters.add(filterCombinationSubMenu);
            }

            if (applyJustToThisLayer)
            {
                final JMenuItem submenuFilters_filterIn = new JMenuItem("Keep only selected elements in this table");
                submenuFilters_filterIn.addActionListener(e1 ->
                {
                    TBFSelectionBased filter = new TBFSelectionBased(callback.getDesign(), selection);
                    callback.getVisualizationState().updateTableRowFilter(filter, FilterCombinationType.INCLUDEIF_AND);
                    callback.updateVisualizationJustTables();
                });
                final JMenuItem submenuFilters_filterOut = new JMenuItem("Filter-out selected elements in this table");
                submenuFilters_filterOut.addActionListener(e1 ->
                {
                    final ElementSelection invertedSelection = selection.invertSelection();
                    if (invertedSelection == null)
                        throw new Net2PlanException("Could not invert selection for the given elements.");

                    TBFSelectionBased filter = new TBFSelectionBased(callback.getDesign(), invertedSelection);
                    callback.getVisualizationState().updateTableRowFilter(filter, FilterCombinationType.INCLUDEIF_AND);
                    callback.updateVisualizationJustTables();
                });

                submenuFilters.add(submenuFilters_filterIn);
                submenuFilters.add(submenuFilters_filterOut);
            }

            popup.add(submenuFilters);
        }

        if (networkElementType != NetworkElementType.FORWARDING_RULE && networkElementType != NetworkElementType.NETWORK && networkElementType != NetworkElementType.LAYER)
        {
            popup.addSeparator();
            popup.add(new MenuItem_RemovedFiltered(callback, networkElementType));

            if (networkElementType == NetworkElementType.NODE || networkElementType == NetworkElementType.LINK)
                popup.add(new MenuItem_HideFiltered(callback, networkElementType));
        }
    }

    protected void addPickOption(ElementSelection selection, JPopupMenu popup)
    {
        final JMenuItem menu = new JMenuItem("Pick");
        popup.add(menu);
        menu.setEnabled(!selection.isEmpty());
        menu.addActionListener(e ->
        {
            SwingUtilities.invokeLater(() -> pickSelection(selection));
        });
    }

    protected void pickSelection(ElementSelection selection)
    {
        assert selection != null;

        callback.getVisualizationState().pickElement(selection.getNetworkElements());
        callback.updateVisualizationAfterPick();
    }

    protected void setTips()
    {
        /* configure the tips */
        String[] columnTips = getTableTips();
        String[] columnHeader = getTableHeaders();
        ColumnHeaderToolTips tips = new ColumnHeaderToolTips();
        for (int c = 0; c < columnHeader.length; c++)
        {
            TableColumn col = getColumnModel().getColumn(c);
            tips.setToolTip(col, columnTips[c]);
        }
        getTableHeader().addMouseMotionListener(tips);
    }

    public void writeTableToFile(File file)
    {
        ExcelWriter.writeToFile(file, this.getTabName(), buildData());
    }

    private Object[][] buildData()
    {
        final int fixedColumnCount = fixedTable.getColumnCount();
        final int mainColumnCount = mainTable.getColumnCount();
        final int rowCount = this.hasElements() ? this.getRowCount() : 0;

        Object[][] data = new Object[rowCount + 1][fixedColumnCount + mainColumnCount];

        // Headers
        for (int i = 0; i < fixedColumnCount; i++)
            data[0][i] = fixedTable.getColumnName(i);

        for (int i = 0; i < mainColumnCount; i++)
            data[0][fixedColumnCount + i] = mainTable.getColumnName(i);

        // Values
        for (int i = 0; i < rowCount; i++)
        {
            for (int j = 0; j < fixedColumnCount; j++)
                data[i + 1][j] = fixedTable.getValueAt(i, j);

            for (int j = 0; j < mainColumnCount; j++)
                data[i + 1][fixedColumnCount + j] = mainTable.getValueAt(i, j);
        }

        return data;
    }

    public abstract List<Object[]> getAllData(NetPlan currentState, ArrayList<String> attributesTitles);

    public abstract String getTabName();

    public abstract String[] getTableHeaders();

    public abstract String[] getCurrentTableHeaders();

    public abstract String[] getTableTips();

    public abstract boolean hasElements();

    public abstract int getAttributesColumnIndex();

    public abstract int getNumberOfDecoratorColumns();

    public abstract ArrayList<String> getAttributesColumnsHeaders();

    protected abstract void setColumnRowSorting();

    protected abstract List<JComponent> getExtraAddOptions();

    protected abstract JMenuItem getAddOption();

    protected abstract List<JComponent> getForcedOptions(ElementSelection selection);

    protected abstract List<JComponent> getExtraOptions(ElementSelection selection);

    protected abstract JPopupMenu getPopup(ElementSelection selection);

    protected abstract boolean hasAttributes();

    private class PopupMenuMouseAdapter extends MouseAdapter
    {
        @Override
        public void mouseClicked(final MouseEvent e)
        {
            try
            {
                final ElementSelection selection = getSelectedElements();

                if (SwingUtilities.isRightMouseButton(e))
                {
                    getPopup(selection).show(e.getComponent(), e.getX(), e.getY());
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e))
                {
                    if (e.getClickCount() == 1)
                    {
                        if (selection.isEmpty())
                            callback.resetPickedStateAndUpdateView();
                    } else if (e.getClickCount() >= 2)
                    {
                        SwingUtilities.invokeLater(() -> pickSelection(selection));
                    }
                }
            } catch (Exception ex)
            {
                ErrorHandling.showErrorDialog("The GUI has suffered a problem.\nPlease see the console for more information.", "Error");
                ex.printStackTrace();
            }
        }
    }

    protected static class MenuItem_RemovedFiltered extends JMenuItem
    {
        MenuItem_RemovedFiltered(GUINetworkDesign callback, NetworkElementType networkElementType)
        {
            final NetPlan netPlan = callback.getDesign();

            this.setText("Remove all filtered out " + networkElementType + "s");
            this.addActionListener(e1 ->
            {
                try
                {
                    final ITableRowFilter tableRowFilter = callback.getVisualizationState().getTableRowFilter();
                    if (tableRowFilter != null)
                    {
                        switch (networkElementType)
                        {
                            case NODE:
                                final List<Node> visibleNodes = tableRowFilter.getVisibleNodes(netPlan.getNetworkLayerDefault());
                                for (Node node : new ArrayList<>(netPlan.getNodes()))
                                    if (!visibleNodes.contains(node)) node.remove();
                                break;
                            case LINK:
                                final List<Link> visibleLinks = tableRowFilter.getVisibleLinks(netPlan.getNetworkLayerDefault());
                                for (Link link : new ArrayList<>(netPlan.getLinks()))
                                    if (!visibleLinks.contains(link)) link.remove();
                                break;
                            case DEMAND:
                                final List<Demand> visibleDemands = tableRowFilter.getVisibleDemands(netPlan.getNetworkLayerDefault());
                                for (Demand demand : new ArrayList<>(netPlan.getDemands()))
                                    if (!visibleDemands.contains(demand)) demand.remove();
                                break;
                            case MULTICAST_DEMAND:
                                final List<MulticastDemand> visibleMulticastDemands = tableRowFilter.getVisibleMulticastDemands(netPlan.getNetworkLayerDefault());
                                for (MulticastDemand multicastDemand : new ArrayList<>(netPlan.getMulticastDemands()))
                                    if (!visibleMulticastDemands.contains(multicastDemand))
                                        multicastDemand.remove();
                                break;
                            case ROUTE:
                                final List<Route> visibleRoutes = tableRowFilter.getVisibleRoutes(netPlan.getNetworkLayerDefault());
                                for (Route route : new ArrayList<>(netPlan.getRoutes()))
                                    if (!visibleRoutes.contains(route)) route.remove();
                                break;
                            case MULTICAST_TREE:
                                final List<MulticastTree> visibleMulticastTrees = tableRowFilter.getVisibleMulticastTrees(netPlan.getNetworkLayerDefault());
                                for (MulticastTree tree : new ArrayList<>(netPlan.getMulticastTrees()))
                                    if (!visibleMulticastTrees.contains(tree)) tree.remove();
                                break;
                            case RESOURCE:
                                final List<Resource> visibleResources = tableRowFilter.getVisibleResources(netPlan.getNetworkLayerDefault());
                                for (Resource resource : new ArrayList<>(netPlan.getResources()))
                                    if (!visibleResources.contains(resource)) resource.remove();
                                break;
                            case SRG:
                                final List<SharedRiskGroup> visibleSRGs = tableRowFilter.getVisibleSRGs(netPlan.getNetworkLayerDefault());
                                for (SharedRiskGroup sharedRiskGroup : new ArrayList<>(netPlan.getSRGs()))
                                    if (!visibleSRGs.contains(sharedRiskGroup)) sharedRiskGroup.remove();
                                break;
                            default:
                                // TODO: Error message?
                                return;
                        }
                    }
                    callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
                    callback.updateVisualizationAfterChanges(Sets.newHashSet(networkElementType));
                    callback.addNetPlanChange();
                } catch (Throwable ex)
                {
                    ex.printStackTrace();
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to complete this action");
                }
            });
        }
    }

    protected static class MenuItem_HideFiltered extends JMenuItem
    {
        MenuItem_HideFiltered(GUINetworkDesign callback, NetworkElementType networkElementType)
        {
            final NetPlan netPlan = callback.getDesign();
            this.setText("Hide all filtered out " + networkElementType + "s");
            this.addActionListener(e1 ->
            {
                final ITableRowFilter tableRowFilter = callback.getVisualizationState().getTableRowFilter();
                if (tableRowFilter != null)
                {
                    switch (networkElementType)
                    {
                        case NODE:
                            final List<Node> visibleNodes = tableRowFilter.getVisibleNodes(netPlan.getNetworkLayerDefault());
                            for (Node node : netPlan.getNodes())
                                if (!visibleNodes.contains(node))
                                    callback.getVisualizationState().hideOnCanvas(node);
                            break;
                        case LINK:
                            final List<Link> visibleLinks = tableRowFilter.getVisibleLinks(netPlan.getNetworkLayerDefault());
                            for (Link link : netPlan.getLinks())
                                if (!visibleLinks.contains(link))
                                    callback.getVisualizationState().hideOnCanvas(link);
                            break;
                        default:
                            return;
                    }
                }

                callback.updateVisualizationAfterChanges(Sets.newHashSet(networkElementType));
                callback.addNetPlanChange();
            });
        }
    }
}
