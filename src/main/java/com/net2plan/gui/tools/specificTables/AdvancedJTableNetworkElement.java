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


package com.net2plan.gui.tools.specificTables;

import com.net2plan.gui.tools.IGUINetworkViewer;
import com.net2plan.gui.tools.IGUINetworkViewer.ColumnComparator;
import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.AttributeEditor;
import com.net2plan.gui.utils.ColumnHeaderToolTips;
import com.net2plan.gui.utils.topology.TopologyPanel;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * <p>Extended version of the {@code JTable} class. It presents the following
 * additional features:</p>
 * <p>
 * <ul>
 * <li>Reordering of table columns is not allowed</li>
 * <li>Auto-resize of columns is disabled</li>
 * <li>It allows to set per-cell editors</li>
 * <li>It allows to navigate the table with the cursor</li>
 * <li>It allows to configure if all cell contents are selected when editing or typing ('true' by default, using 'setSelectAllXXX()' methods to customize)</li>
 * </ul>
 * <p>
 * <p>Credits to Santhosh Kumar for his methods to solve partially visible cell
 * issues (<a href='http://www.jroller.com/santhosh/entry/partially_visible_tablecells'>Partially Visible TableCells</a>)</p>
 * <p>
 * <p>Credits to "Kah - The Developer" for his static method to set column widths
 * in proportion to each other (<a href='http://kahdev.wordpress.com/2011/10/30/java-specifying-the-column-widths-of-a-jtable-as-percentages/'>Specifying the column widths of a JTable as percentages</a>)
 * <p>
 * <p>Credits to Rob Camick for his 'select all' editing feature for {@code JTable}
 * (<a href='https://tips4java.wordpress.com/2008/10/20/table-select-all-editor/'>Table Select All Editor</a>)
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public abstract class AdvancedJTableNetworkElement extends AdvancedJTable {
    protected final TableModel model;
    protected final IGUINetworkViewer networkViewer;
    protected final NetworkElementType networkElementType;
    //	/**
//	 * Default constructor.
//	 * 
//	 * @since 0.2.0
//	 */
//	public AdvancedJTableNetworkElement() 
//	{
//		super();
//	}

    /**
     * Constructor that allows to set the table model.
     *
     * @param model Table model
     * @since 0.2.0
     */
    public AdvancedJTableNetworkElement(TableModel model, final IGUINetworkViewer networkViewer, NetworkElementType networkElementType) {
        super(model);
        this.model = model;
        this.networkViewer = networkViewer;
        this.networkElementType = networkElementType;

		/* configure the tips */
        String[] columnTips = getTableTips();
        String[] columnHeader = getTableHeaders();
        ColumnHeaderToolTips tips = new ColumnHeaderToolTips();
        for (int c = 0; c < columnHeader.length; c++) {
            TableColumn col = getColumnModel().getColumn(c);
            tips.setToolTip(col, columnTips[c]);
        }
        getTableHeader().addMouseMotionListener(tips);

		/* add the popup menu listener (this) */
        addMouseListener(new PopupMenuAdapter());
    }


    public abstract List<Object[]> getAllData(NetPlan currentState, TopologyPanel topologyPanel, NetPlan initialState);

    public abstract String getTabName();

    public abstract String[] getTableHeaders();

    public abstract String[] getTableTips();

    public abstract boolean hasElements(NetPlan np);

    public abstract int[] getColumnsOfSpecialComparatorForSorting();

    public abstract void setColumnRowSorting(boolean allowShowInitialNetPlan);

    public abstract int getNumFixedLeftColumnsInDecoration();

    public abstract void doPopup(final MouseEvent e, final int row, final Object itemId);

    public abstract void showInCanvas(MouseEvent e, Object itemId);

    public void updateView(NetPlan currentState, NetPlan initialState) {
        setEnabled(false);
        String[] header = getTableHeaders();
        ((DefaultTableModel) getModel()).setDataVector(new Object[1][getTableHeaders().length], header);

        if (currentState.getRoutingType() == RoutingType.SOURCE_ROUTING && networkElementType.equals(NetworkElementType.FORWARDING_RULE))
            return;
        if (currentState.getRoutingType() == RoutingType.HOP_BY_HOP_ROUTING && (networkElementType.equals(NetworkElementType.ROUTE) || networkElementType.equals(NetworkElementType.PROTECTION_SEGMENT)))
            return;
        if (hasElements(currentState)) {
            String[] tableHeaders = getTableHeaders();
            List<Object[]> allData = getAllData(currentState, networkViewer.getTopologyPanel(), initialState);
            setEnabled(true);
            ((DefaultTableModel) getModel()).setDataVector(allData.toArray(new Object[allData.size()][tableHeaders.length]), tableHeaders);

            for (int columnId : getColumnsOfSpecialComparatorForSorting())
                ((DefaultRowSorter) getRowSorter()).setComparator(columnId, new ColumnComparator());
        }
    }

    public class PopupMenuAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(final MouseEvent e) {
            Object auxItemId = null;
            int row = -1;
            if (hasElements(networkViewer.getDesign())) {
                JTable table = getTable(e);
                row = table.rowAtPoint(e.getPoint());
                if (row != -1) {
                    row = table.convertRowIndexToModel(row);
                    if (table.getModel().getValueAt(row, 0) == null)
                        row = row - 1;
                    if (networkElementType == NetworkElementType.FORWARDING_RULE)
                        auxItemId = Pair.of(Integer.parseInt(model.getValueAt(row, 1).toString().split(" ")[0]), Integer.parseInt(model.getValueAt(row, 2).toString().split(" ")[0]));
                    else
                        auxItemId = (Long) model.getValueAt(row, 0);
                }
            }

            final Object itemId = auxItemId;

            if (SwingUtilities.isRightMouseButton(e)) {
                doPopup(e, row, itemId);
                return;
            }

            if (itemId == null) {
                networkViewer.resetView();
                return;
            }

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    showInCanvas(e, itemId);
                }
            });
        }

        private JTable getTable(MouseEvent e) {
            Object src = e.getSource();
            if (src instanceof JTable) {
                JTable table = (JTable) src;
                if (table.getModel() != model) throw new RuntimeException("Table model is not valid");

                return table;
            }

            throw new RuntimeException("Bad - Event source is not a JTable");
        }
    }


    final protected void addPopupMenuAttributeOptions(final MouseEvent e, final int row, final Object itemId, JPopupMenu popup) {
        if (networkElementType == NetworkElementType.FORWARDING_RULE)
            throw new RuntimeException("Bad. Forwarding rules have no attributes");
        JMenuItem addAttribute = new JMenuItem("Add/edit attribute");
        addAttribute.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField txt_key = new JTextField(20);
                JTextField txt_value = new JTextField(20);

                JPanel pane = new JPanel();
                pane.add(new JLabel("Attribute: "));
                pane.add(txt_key);
                pane.add(Box.createHorizontalStrut(15));
                pane.add(new JLabel("Value: "));
                pane.add(txt_value);

                NetPlan netPlan = networkViewer.getDesign();

                while (true) {
                    int result = JOptionPane.showConfirmDialog(null, pane, "Please enter an attribute name and its value", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return;

                    try {
                        if (txt_key.getText().isEmpty()) throw new Exception("Please, insert an attribute name");

                        String attribute = txt_key.getText();
                        String value = txt_value.getText();
                        NetworkElement element = netPlan.getNetworkElement((long) itemId);
                        element.setAttribute(attribute, value);
                        break;
                    } catch (Throwable ex) {
                        ErrorHandling.addErrorOrException(ex, getClass());
                        ErrorHandling.showErrorDialog("Error adding/editing attribute");
                    }
                }

                try {
                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ErrorHandling.addErrorOrException(ex, getClass());
                    ErrorHandling.showErrorDialog("Unable to add attribute to " + networkElementType);
                }
            }
        });

        popup.add(addAttribute);

        JMenuItem viewAttributes = new JMenuItem("View/edit attributes");
        viewAttributes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    NetPlan netPlan = networkViewer.getDesign();
                    int itemIndex = convertRowIndexToModel(row);
                    Object itemId;

                    switch (networkElementType) {
                        case FORWARDING_RULE:
                            TableModel model = getModel();
                            itemId = Pair.of((Long) model.getValueAt(itemIndex, 1), (Long) model.getValueAt(itemIndex, 2));
                            break;

                        case LAYER:
                            itemId = netPlan.getNetworkLayers().get(itemIndex).getId();
                            break;

                        case NODE:
                            itemId = netPlan.getNodes().get(itemIndex).getId();
                            break;

                        case LINK:
                            itemId = netPlan.getLinks().get(itemIndex).getId();
                            break;

                        case DEMAND:
                            itemId = netPlan.getDemands().get(itemIndex).getId();
                            break;

                        case MULTICAST_DEMAND:
                            itemId = netPlan.getMulticastDemands().get(itemIndex).getId();
                            break;

                        case ROUTE:
                            itemId = netPlan.getRoutes().get(itemIndex).getId();
                            break;

                        case MULTICAST_TREE:
                            itemId = netPlan.getMulticastTrees().get(itemIndex).getId();
                            break;

                        case PROTECTION_SEGMENT:
                            itemId = netPlan.getProtectionSegments().get(itemIndex).getId();
                            break;

                        case SRG:
                            itemId = netPlan.getSRGs().get(itemIndex).getId();
                            break;

                        default:
                            throw new RuntimeException("Bad");
                    }

                    JDialog dialog = new AttributeEditor(networkViewer, networkElementType, itemId);
                    dialog.setVisible(true);
                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying attributes");
                }
            }
        });

        popup.add(viewAttributes);

        JMenuItem removeAttribute = new JMenuItem("Remove attribute");

        removeAttribute.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NetPlan netPlan = networkViewer.getDesign();

                try {
                    int itemIndex = convertRowIndexToModel(row);
                    Object itemId;

                    String[] attributeList;

                    switch (networkElementType) {
                        case LAYER: {
                            NetworkLayer element = netPlan.getNetworkLayers().get(itemIndex);
                            itemId = element.getId();
                            attributeList = StringUtils.toArray(element.getAttributes().keySet());
                        }
                        break;

                        case NODE: {
                            Node element = netPlan.getNodes().get(itemIndex);
                            itemId = element.getId();
                            attributeList = StringUtils.toArray(element.getAttributes().keySet());
                        }
                        break;

                        case LINK: {
                            Link element = netPlan.getLinks().get(itemIndex);
                            itemId = element.getId();
                            attributeList = StringUtils.toArray(element.getAttributes().keySet());
                        }
                        break;

                        case DEMAND: {
                            Demand element = netPlan.getDemands().get(itemIndex);
                            itemId = element.getId();
                            attributeList = StringUtils.toArray(element.getAttributes().keySet());
                        }
                        break;

                        case MULTICAST_DEMAND: {
                            MulticastDemand element = netPlan.getMulticastDemands().get(itemIndex);
                            itemId = element.getId();
                            attributeList = StringUtils.toArray(element.getAttributes().keySet());
                        }
                        break;

                        case ROUTE: {
                            Route element = netPlan.getRoutes().get(itemIndex);
                            itemId = element.getId();
                            attributeList = StringUtils.toArray(element.getAttributes().keySet());
                        }
                        break;

                        case MULTICAST_TREE: {
                            MulticastTree element = netPlan.getMulticastTrees().get(itemIndex);
                            itemId = element.getId();
                            attributeList = StringUtils.toArray(element.getAttributes().keySet());
                        }
                        break;

                        case PROTECTION_SEGMENT: {
                            ProtectionSegment element = netPlan.getProtectionSegments().get(itemIndex);
                            itemId = element.getId();
                            attributeList = StringUtils.toArray(element.getAttributes().keySet());
                        }
                        break;

                        case SRG: {
                            SharedRiskGroup element = netPlan.getSRGs().get(itemIndex);
                            itemId = element.getId();
                            attributeList = StringUtils.toArray(element.getAttributes().keySet());
                        }
                        break;

                        default:
                            throw new RuntimeException("Bad");
                    }

                    if (attributeList.length == 0) throw new Exception("No attribute to remove");

                    Object out = JOptionPane.showInputDialog(null, "Please, select an attribute to remove", "Remove attribute", JOptionPane.QUESTION_MESSAGE, null, attributeList, attributeList[0]);
                    if (out == null) return;

                    String attributeToRemove = out.toString();
                    NetworkElement element = netPlan.getNetworkElement((long) itemId);
                    if (element == null) throw new RuntimeException("Bad");
                    element.removeAttribute(attributeToRemove);

                    networkViewer.updateNetPlanView();
                } catch (Throwable ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing attribute");
                }
            }
        });

        popup.add(removeAttribute);


        int numItems = getModel().getRowCount();
        if (numItems > 1) {
            if (popup.getSubElements().length > 0) popup.addSeparator();

            JMenuItem addAttributeAll = new JMenuItem("Add/edit attribute to all");
            addAttributeAll.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JTextField txt_key = new JTextField(20);
                    JTextField txt_value = new JTextField(20);

                    JPanel pane = new JPanel();
                    pane.add(new JLabel("Attribute: "));
                    pane.add(txt_key);
                    pane.add(Box.createHorizontalStrut(15));
                    pane.add(new JLabel("Value: "));
                    pane.add(txt_value);

                    NetPlan netPlan = networkViewer.getDesign();

                    while (true) {
                        int result = JOptionPane.showConfirmDialog(null, pane, "Please enter an attribute name and its value", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (result != JOptionPane.OK_OPTION) return;

                        try {
                            if (txt_key.getText().isEmpty()) throw new Exception("Please, insert an attribute name");

                            String attribute = txt_key.getText();
                            String value = txt_value.getText();

                            switch (networkElementType) {
                                case LAYER:
                                    for (NetworkLayer element : netPlan.getNetworkLayers())
                                        element.setAttribute(attribute, value);
                                    break;

                                case NODE:
                                    for (Node element : netPlan.getNodes()) element.setAttribute(attribute, value);
                                    break;

                                case LINK:
                                    for (Link element : netPlan.getLinks()) element.setAttribute(attribute, value);
                                    break;

                                case DEMAND:
                                    for (Demand element : netPlan.getDemands()) element.setAttribute(attribute, value);
                                    break;

                                case MULTICAST_DEMAND:
                                    for (MulticastDemand element : netPlan.getMulticastDemands())
                                        element.setAttribute(attribute, value);
                                    break;

                                case ROUTE:
                                    for (Route element : netPlan.getRoutes()) element.setAttribute(attribute, value);
                                    break;

                                case MULTICAST_TREE:
                                    for (MulticastTree element : netPlan.getMulticastTrees())
                                        element.setAttribute(attribute, value);
                                    break;

                                case PROTECTION_SEGMENT:
                                    for (ProtectionSegment element : netPlan.getProtectionSegments())
                                        element.setAttribute(attribute, value);
                                    break;

                                case SRG:
                                    for (SharedRiskGroup element : netPlan.getSRGs())
                                        element.setAttribute(attribute, value);
                                    break;

                                default:
                                    throw new RuntimeException("Bad");
                            }

                            break;
                        } catch (Throwable ex) {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error adding/editing attribute to all " + networkElementType + "s");
                        }
                    }

                    try {
                        networkViewer.updateNetPlanView();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add attribute to all nodes");
                    }
                }
            });

            popup.add(addAttributeAll);

            JMenuItem viewAttributesAll = new JMenuItem("View/edit attributes from all");
            viewAttributesAll.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        JDialog dialog = new AttributeEditor(networkViewer, networkElementType);
                        dialog.setVisible(true);
                        networkViewer.updateNetPlanView();
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error modifying attributes");
                    }
                }
            });

            popup.add(viewAttributesAll);

            JMenuItem removeAttributeAll = new JMenuItem("Remove attribute from all " + networkElementType + "s");

            removeAttributeAll.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NetPlan netPlan = networkViewer.getDesign();

                    try {
                        Set<String> attributeSet = new LinkedHashSet<String>();
                        Collection<Long> itemIds;

                        switch (networkElementType) {
                            case LAYER:
                                itemIds = netPlan.getNetworkLayerIds();
                                for (long layerId : itemIds)
                                    attributeSet.addAll(netPlan.getNetworkLayerFromId(layerId).getAttributes().keySet());

                                break;

                            case NODE:
                                itemIds = netPlan.getNodeIds();
                                for (long nodeId : itemIds)
                                    attributeSet.addAll(netPlan.getNodeFromId(nodeId).getAttributes().keySet());

                                break;

                            case LINK:
                                itemIds = netPlan.getLinkIds();
                                for (long linkId : itemIds)
                                    attributeSet.addAll(netPlan.getLinkFromId(linkId).getAttributes().keySet());

                                break;

                            case DEMAND:
                                itemIds = netPlan.getDemandIds();
                                for (long demandId : itemIds)
                                    attributeSet.addAll(netPlan.getDemandFromId(demandId).getAttributes().keySet());

                                break;

                            case MULTICAST_DEMAND:
                                itemIds = netPlan.getMulticastDemandIds();
                                for (long demandId : itemIds)
                                    attributeSet.addAll(netPlan.getMulticastDemandFromId(demandId).getAttributes().keySet());

                                break;

                            case ROUTE:
                                itemIds = netPlan.getRouteIds();
                                for (long routeId : itemIds)
                                    attributeSet.addAll(netPlan.getRouteFromId(routeId).getAttributes().keySet());

                                break;

                            case MULTICAST_TREE:
                                itemIds = netPlan.getMulticastTreeIds();
                                for (long treeId : itemIds)
                                    attributeSet.addAll(netPlan.getMulticastTreeFromId(treeId).getAttributes().keySet());

                                break;

                            case PROTECTION_SEGMENT:
                                itemIds = netPlan.getProtectionSegmentIds();
                                for (long segmentId : itemIds)
                                    attributeSet.addAll(netPlan.getProtectionSegmentFromId(segmentId).getAttributes().keySet());

                                break;

                            case SRG:
                                itemIds = netPlan.getSRGIds();
                                for (long srgId : itemIds)
                                    attributeSet.addAll(netPlan.getSRGFromId(srgId).getAttributes().keySet());

                                break;

                            default:
                                throw new RuntimeException("Bad");
                        }

                        if (attributeSet.isEmpty()) throw new Exception("No attribute to remove");

                        Object out = JOptionPane.showInputDialog(null, "Please, select an attribute to remove", "Remove attribute from all nodes", JOptionPane.QUESTION_MESSAGE, null, attributeSet.toArray(new String[attributeSet.size()]), attributeSet.iterator().next());
                        if (out == null) return;

                        String attributeToRemove = out.toString();

                        switch (networkElementType) {
                            case LAYER:
                                for (long layerId : itemIds)
                                    netPlan.getNetworkLayerFromId(layerId).removeAttribute(attributeToRemove);
                                break;

                            case NODE:
                                for (long nodeId : itemIds)
                                    netPlan.getNodeFromId(nodeId).removeAttribute(attributeToRemove);
                                break;

                            case LINK:
                                for (long linkId : itemIds)
                                    netPlan.getLinkFromId(linkId).removeAttribute(attributeToRemove);
                                break;

                            case DEMAND:
                                for (long demandId : itemIds)
                                    netPlan.getDemandFromId(demandId).removeAttribute(attributeToRemove);
                                break;

                            case MULTICAST_DEMAND:
                                for (long demandId : itemIds)
                                    netPlan.getMulticastDemandFromId(demandId).removeAttribute(attributeToRemove);
                                break;

                            case ROUTE:
                                for (long routeId : itemIds)
                                    netPlan.getRouteFromId(routeId).removeAttribute(attributeToRemove);
                                break;

                            case MULTICAST_TREE:
                                for (long treeId : itemIds)
                                    netPlan.getMulticastTreeFromId(treeId).removeAttribute(attributeToRemove);
                                break;

                            case PROTECTION_SEGMENT:
                                for (long segmentId : itemIds)
                                    netPlan.getProtectionSegmentFromId(segmentId).removeAttribute(attributeToRemove);
                                break;

                            case SRG:
                                for (long srgId : itemIds)
                                    netPlan.getSRGFromId(srgId).removeAttribute(attributeToRemove);
                                break;

                            default:
                                throw new RuntimeException("Bad");
                        }

                        networkViewer.updateNetPlanView();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing attribute from all " + networkElementType + "s");
                    }
                }
            });

            popup.add(removeAttributeAll);

            JMenuItem removeAttributes = new JMenuItem("Remove all attributes from all " + networkElementType + "s");

            removeAttributes.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NetPlan netPlan = networkViewer.getDesign();

                    try {
                        switch (networkElementType) {
                            case LAYER:
                                Collection<Long> layerIds = netPlan.getNetworkLayerIds();
                                for (long layerId : layerIds)
                                    netPlan.getNetworkLayerFromId(layerId).removeAllAttributes();

                                break;

                            case NODE:
                                Collection<Long> nodeIds = netPlan.getNodeIds();
                                for (long nodeId : nodeIds) netPlan.getNodeFromId(nodeId).removeAllAttributes();

                                break;

                            case LINK:
                                Collection<Long> linkIds = netPlan.getLinkIds();
                                for (long linkId : linkIds) netPlan.getLinkFromId(linkId).removeAllAttributes();

                                break;

                            case DEMAND:
                                Collection<Long> demandIds = netPlan.getDemandIds();
                                for (long demandId : demandIds) netPlan.getDemandFromId(demandId).removeAllAttributes();

                                break;

                            case MULTICAST_DEMAND:
                                Collection<Long> multicastDemandIds = netPlan.getMulticastDemandIds();
                                for (long demandId : multicastDemandIds)
                                    netPlan.getMulticastDemandFromId(demandId).removeAllAttributes();

                                break;

                            case ROUTE:
                                Collection<Long> routeIds = netPlan.getRouteIds();
                                for (long routeId : routeIds) netPlan.getRouteFromId(routeId).removeAllAttributes();

                                break;

                            case MULTICAST_TREE:
                                Collection<Long> treeIds = netPlan.getMulticastTreeIds();
                                for (long treeId : treeIds)
                                    netPlan.getMulticastTreeFromId(treeId).removeAllAttributes();

                                break;

                            case PROTECTION_SEGMENT:
                                Collection<Long> segmentIds = netPlan.getProtectionSegmentIds();
                                for (long segmentId : segmentIds)
                                    netPlan.getProtectionSegmentFromId(segmentId).removeAllAttributes();

                                break;

                            case SRG:
                                Collection<Long> srgIds = netPlan.getSRGIds();
                                for (long srgId : srgIds) netPlan.getSRGFromId(srgId).removeAllAttributes();

                                break;

                            default:
                                throw new RuntimeException("Bad");
                        }

                        networkViewer.updateNetPlanView();
                    } catch (Throwable ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing attributes");
                    }
                }
            });

            popup.add(removeAttributes);
        }
    }

}
