/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mari�o.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mari�o - initial API and implementation
 ******************************************************************************/


package com.net2plan.gui.utils;

import com.net2plan.gui.tools.specificTables.AdvancedJTable_link;
import com.net2plan.gui.tools.specificTables.AdvancedJTable_route;
import com.net2plan.gui.utils.topology.INetworkCallback;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Pair;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Set of several cell renderers used into the GUI.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class CellRenderers {
    /**
     * Renderer for cells containing boolean values.
     *
     * @since 0.2.0
     */
    public static class CheckBoxRenderer extends NonEditableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value == null) return c;

            Color oldColor = (Color) UIManager.get("CheckBox.interiorBackground");
            UIManager.put("CheckBox.interiorBackground", bgColorNonEditable);

            JCheckBox checkBox = new JCheckBox();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            checkBox.setBorderPainted(false);
            checkBox.setSelected((Boolean) value);
            checkBox.setForeground(c.getForeground());
            checkBox.setBackground(c.getBackground());
            setBorder(hasFocus ? UIManager.getBorder("Table.focusCellHighlightBorder") : noFocusBorder);

            UIManager.put("CheckBox.interiorBackground", oldColor);

            return checkBox;
        }
    }

    /**
     * Renderer that is able to determine if the row corresponds to the current state
     * or the initial state.
     *
     * @since 0.3.0
     */
    public abstract static class CurrentPlannedCellRenderer implements TableCellRenderer {
        private final TableCellRenderer cellRenderer;

        /**
         * Type of element (i.e. layers, nodes, links, and so on).
         *
         * @since 0.3.0
         */
        protected final NetworkElementType networkElementType;

        /**
         * Default constructor.
         *
         * @param cellRenderer       Reference to the original cell renderer
         * @param networkElementType Type of element (i.e. layers, nodes, links, and so on)
         * @since 0.3.0
         */
        public CurrentPlannedCellRenderer(TableCellRenderer cellRenderer, NetworkElementType networkElementType) {
            this.cellRenderer = cellRenderer;
            this.networkElementType = networkElementType;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = cellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected && row != -1 && column != -1) {
                row = table.convertRowIndexToModel(row);
                column = table.convertColumnIndexToModel(column);

                if (table.getModel().getValueAt(row, 0) == null) {
                    setPlannedState(c, table, row, column, isSelected);
                } else {
                    Object itemId;
                    if (networkElementType == NetworkElementType.FORWARDING_RULE) {
                        itemId = Pair.of(Integer.parseInt(table.getModel().getValueAt(row, 1).toString().split(" ")[0]), Integer.parseInt(table.getModel().getValueAt(row, 2).toString().split(" ")[0]));
                    } else
                        itemId = (Long) table.getModel().getValueAt(row, 0);

                    setCurrentState(c, table, itemId, row, column, isSelected);
                }
            }

            return c;
        }

        /**
         * Sets the cell properties for the current state.
         *
         * @param c                Component
         * @param table            Table
         * @param itemId           Item id (node identifier...)
         * @param rowIndexModel    Row index in model order
         * @param columnIndexModel Column index in model order
         * @param isSelected       Indicates whether or not the cell is selected
         * @since 0.2.0
         */
        public void setCurrentState(Component c, JTable table, Object itemId, int rowIndexModel, int columnIndexModel, boolean isSelected) {
        }

        /**
         * Sets the cell properties for the planned state.
         *
         * @param c                Component
         * @param table            Table
         * @param rowIndexModel    Row index in model order
         * @param columnIndexModel Column index in model order
         * @param isSelected       Indicates whether or not the cell is selected
         * @since 0.2.0
         */
        public void setPlannedState(Component c, JTable table, int rowIndexModel, int columnIndexModel, boolean isSelected) {
        }
    }

    /**
     * Renderer for links that shades cells referred to link utilization in
     * red or orange if value is greater or equal than one, respectively. In case
     * the link is down, the whole row is shaded in red.
     *
     * @since 0.3.0
     */
    public static class LinkRenderer extends UpDownRenderer {
        /**
         * Default constructor.
         *
         * @param cellRenderer Reference to the original cell renderer
         * @param callback     Reference to the handler of the network design
         * @since 0.3.0
         */
        public LinkRenderer(TableCellRenderer cellRenderer, INetworkCallback callback) {
            super(cellRenderer, callback, NetworkElementType.LINK);
        }

        @Override
        public void setCurrentStateUp(Component c, JTable table, Object itemId, int rowIndexModel, int columnIndexModel, boolean isSelected) {
            NetPlan currentNetPlan = callback.getDesign();

            long linkId = (long) itemId;
            long originNodeId = currentNetPlan.getLinkFromId(linkId).getOriginNode().getId();
            long destinationNodeId = currentNetPlan.getLinkFromId(linkId).getDestinationNode().getId();
            if (!currentNetPlan.getNodeFromId(originNodeId).isUp() || !currentNetPlan.getNodeFromId(destinationNodeId).isUp()) {
                c.setBackground(Color.RED);
                return;
            }

//			if (columnIndexModel == 9 || columnIndexModel == 10)
            if (columnIndexModel == AdvancedJTable_link.COLUMN_UTILIZATION || columnIndexModel == AdvancedJTable_link.COLUMN_UTILIZATIONWOPROTECTION) {
                double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
                double doubleValue = (Double) table.getModel().getValueAt(rowIndexModel, columnIndexModel);
                if (DoubleUtils.isEqualWithinAbsoluteTolerance(doubleValue, 1, PRECISION_FACTOR))
                    c.setBackground(Color.ORANGE);
                else if (doubleValue > 1) c.setBackground(Color.RED);
            }
        }
    }

    /**
     * Renderer that shades cell in red if value is greater than zero.
     *
     * @since 0.2.0
     */
    public static class LostTrafficCellRenderer extends NumberCellRenderer {
        private static final long serialVersionUID = 1L;
        private final int offeredTrafficColumnModelIndex;

        /**
         * Default constructor.
         *
         * @param offeredTrafficColumnModelIndex Column index (in model order) in which is found the offered traffic
         * @since 0.2.0
         */
        public LostTrafficCellRenderer(int offeredTrafficColumnModelIndex) {
            super();

            this.offeredTrafficColumnModelIndex = offeredTrafficColumnModelIndex;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected && value != null) {
                int demandId = table.convertRowIndexToModel(row);
                double lostTraffic = (Double) value;
                double h_d = (Double) table.getModel().getValueAt(demandId, offeredTrafficColumnModelIndex);
                if (h_d > 0) {
                    c.setBackground(lostTraffic > 1E-10 ? Color.RED : Color.GREEN);
                }
            }

            return c;
        }
    }

    /**
     * Renderer that shades in gray non-editable cells.
     *
     * @since 0.2.0
     */
    public static class NonEditableCellRenderer extends DefaultTableCellRenderer {
        /**
         * Background color for non-editable cell.
         *
         * @since 0.2.0
         */
        protected final static Color bgColorNonEditable = new Color(245, 245, 245);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			/*
             * When you call setForeground() or setBackground(), the selected colors stay
			 * in effect until subsequent calls are made to those methods. Without care,
			 * you might inadvertently color cells that should not be colored. To prevent
			 * that from happening, you should always include calls to the aforementioned
			 * methods that set a cell's colors to their look and feel–specific defaults,
			 * if that cell is not being colored
			 */
            c.setForeground(UIManager.getColor(isSelected ? "Table.selectionForeground" : "Table.foreground"));
            c.setBackground(UIManager.getColor(isSelected ? "Table.selectionBackground" : "Table.background"));

            if (!isSelected) {
                if (row != -1 && column != -1) {
                    row = table.convertRowIndexToModel(row);
                    column = table.convertColumnIndexToModel(column);

                    if (!table.getModel().isCellEditable(row, column)) {
                        c.setBackground(isSelected ? UIManager.getColor("Table.selectionBackground") : bgColorNonEditable);
                    }
                }
            }

            return c;
        }
    }

    /**
     * Gives the current number format to cells, and fixes the
     * {@code setForeground()}/{@code setBackground()} issue.
     *
     * @since 0.2.0
     */
    public static class NumberCellRenderer extends NonEditableCellRenderer {
        private final NumberFormat nf;

        /**
         * Default constructor.
         *
         * @since 0.2.0
         */
        public NumberCellRenderer() {
            nf = NumberFormat.getInstance();
            nf.setGroupingUsed(false);
        }

        /**
         * Constructor that allows configuring the maximum number of decimal digits.
         *
         * @param maxNumDecimals Maximum number of decimal digits
         * @since 0.3.1
         */
        public NumberCellRenderer(int maxNumDecimals) {
            this();
            nf.setMaximumFractionDigits(maxNumDecimals);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Number) {
                double doubleValue = ((Number) value).doubleValue();
                if (doubleValue == Double.MAX_VALUE) doubleValue = Double.POSITIVE_INFINITY;
                else if (doubleValue == -Double.MAX_VALUE) doubleValue = Double.NEGATIVE_INFINITY;
                value = nf.format(Math.abs(doubleValue) < 1e-10 ? 0.0 : doubleValue);
                setHorizontalAlignment(SwingConstants.RIGHT);
            }

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return c;
        }
    }

    /**
     * Renderer for forwarding rules that shades the cell in red if the node where
     * the rule is installed, or the outgoing link, is down.
     *
     * @since 0.3.0
     */
    public static class ForwardingRuleRenderer extends UpDownRenderer {
        /**
         * Default constructor.
         *
         * @param cellRenderer Reference to the original cell renderer
         * @param callback     Reference to the handler of the network design
         * @since 0.3.0
         */
        public ForwardingRuleRenderer(TableCellRenderer cellRenderer, INetworkCallback callback) {
            super(cellRenderer, callback, NetworkElementType.FORWARDING_RULE);
        }
    }

    /**
     * Renderer for routes that shades the cell referred to link utilization in
     * red or orange if value is greater or equal than one, respectively. In case
     * the route is down, the whole row is shaded in red.
     *
     * @since 0.3.0
     */
    public static class RouteRenderer extends UpDownRenderer {
        /**
         * Default constructor.
         *
         * @param cellRenderer Reference to the original cell renderer
         * @param callback     Reference to the handler of the network design
         * @since 0.3.0
         */
        public RouteRenderer(TableCellRenderer cellRenderer, INetworkCallback callback) {
            super(cellRenderer, callback, NetworkElementType.ROUTE);
        }

        @Override
        public void setCurrentStateUp(Component c, JTable table, Object itemId, int rowIndexModel, int columnIndexModel, boolean isSelected) {
            NetPlan currentNetPlan = callback.getDesign();
            NetPlan initialNetPlan = callback.getInitialDesign();

            long routeId = (long) itemId;
            if (columnIndexModel == AdvancedJTable_route.COLUMN_BOTTLENECKUTILIZATION) {
                double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
                double doubleValue = (Double) table.getModel().getValueAt(rowIndexModel, columnIndexModel);
                if (DoubleUtils.isEqualWithinAbsoluteTolerance(doubleValue, 1, PRECISION_FACTOR))
                    c.setBackground(Color.ORANGE);
                else if (doubleValue > 1) c.setBackground(Color.RED);
            }

            if (initialNetPlan != null && initialNetPlan.getRouteFromId(routeId) != null) {
                Route initialRoute = initialNetPlan.getRouteFromId(routeId);
                Route currentRoute = currentNetPlan.getRouteFromId(routeId);
                List<Long> currentSeqLinks = (List<Long>) NetPlan.getIds(currentRoute.getSeqLinksRealPath());
                List<Long> initialSeqLinks = (List<Long>) NetPlan.getIds(initialRoute.getSeqLinksRealPath());

                if (!currentSeqLinks.equals(initialSeqLinks))
                    c.setBackground(Color.YELLOW);

                if (currentRoute.getCarriedTraffic() < initialRoute.getCarriedTraffic())
                    c.setBackground(Color.ORANGE);
            }
        }
    }

    /**
     * Renderer for multicast trees that shades the cell referred to link utilization in
     * red or orange if value is greater or equal than one, respectively. In case
     * the tree is down, the whole row is shaded in red.
     *
     * @since 0.3.1
     */
    public static class MulticastTreeRenderer extends UpDownRenderer {
        /**
         * Default constructor.
         *
         * @param cellRenderer Reference to the original cell renderer
         * @param callback     Reference to the handler of the network design
         * @since 0.3.1
         */
        public MulticastTreeRenderer(TableCellRenderer cellRenderer, INetworkCallback callback) {
            super(cellRenderer, callback, NetworkElementType.MULTICAST_TREE);
        }

        @Override
        public void setCurrentStateUp(Component c, JTable table, Object itemId, int rowIndexModel, int columnIndexModel, boolean isSelected) {
            NetPlan currentNetPlan = callback.getDesign();
            NetPlan initialNetPlan = callback.getInitialDesign();

            long treeId = (long) itemId;
            if (columnIndexModel == 13) {
                double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
                double doubleValue = (Double) table.getModel().getValueAt(rowIndexModel, columnIndexModel);
                if (DoubleUtils.isEqualWithinAbsoluteTolerance(doubleValue, 1, PRECISION_FACTOR))
                    c.setBackground(Color.ORANGE);
                else if (doubleValue > 1) c.setBackground(Color.RED);
            }

            NetworkLayer layer = currentNetPlan.getNetworkLayerDefault();
            if (initialNetPlan != null && initialNetPlan.getMulticastTreeFromId(treeId) != null && initialNetPlan.getMulticastTreeFromId(treeId).getLayer().equals(layer)) {
                MulticastTree currentTree = currentNetPlan.getMulticastTreeFromId(treeId);
                MulticastTree initialTree = initialNetPlan.getMulticastTreeFromId(treeId);
                Set<Link> currentSeqLinks = currentTree.getLinkSet();
                Set<Link> initialSeqLinks = initialTree.getLinkSet();

                if (!currentSeqLinks.equals(initialSeqLinks)) {
                    c.setBackground(Color.YELLOW);
                } else {
                    Map<String, String> currentAttributes = currentTree.getAttributes();
                    Map<String, String> initialAttributes = initialTree.getAttributes();
                    if (!currentAttributes.equals(initialAttributes)) c.setBackground(Color.YELLOW);
                }

                double current_x_p = currentTree.getCarriedTraffic();
                double initial_x_p = initialTree.getCarriedTraffic();

                if (current_x_p < initial_x_p) {
                    c.setBackground(Color.ORANGE);
                } else {
                    double current_y_p = currentTree.getOccupiedLinkCapacity();
                    double initial_y_p = initialTree.getOccupiedLinkCapacity();
                    if (current_y_p < initial_y_p) c.setBackground(Color.ORANGE);
                }
            }
        }
    }

    /**
     * Renderer that is able to determine if current state is up or down.
     *
     * @since 0.3.0
     */
    public static class UpDownRenderer extends CurrentPlannedCellRenderer {
        /**
         * Reference to the handler of the network design.
         *
         * @since 0.3.0
         */
        protected final INetworkCallback callback;

        /**
         * Default constructor.
         *
         * @param cellRenderer       Reference to the original cell renderer
         * @param callback           Reference to the handler of the network design
         * @param networkElementType Type of element (i.e. layers, nodes, links, and so on)
         * @since 0.3.0
         */
        public UpDownRenderer(TableCellRenderer cellRenderer, INetworkCallback callback, NetworkElementType networkElementType) {
            super(cellRenderer, networkElementType);

            this.callback = callback;
        }

        @Override
        public void setCurrentState(Component c, JTable table, Object itemId, int rowIndexModel, int columnIndexModel, boolean isSelected) {
            NetPlan currentNetPlan = callback.getDesign();
            boolean isDown = false;

            switch (networkElementType) {
                case NODE:
                    isDown = currentNetPlan.getNodeFromId((long) itemId).isDown();
                    break;

                case LINK:
                    isDown = currentNetPlan.getLinkFromId((long) itemId).isDown();
                    break;

                case ROUTE:
                    isDown = currentNetPlan.getRouteFromId((long) itemId).isDown();
                    break;

                case MULTICAST_TREE:
                    isDown = currentNetPlan.getMulticastTreeFromId((long) itemId).isDown();
                    break;

                case PROTECTION_SEGMENT:
                    isDown = currentNetPlan.getProtectionSegmentFromId((long) itemId).isDown();
                    break;

                case FORWARDING_RULE:
                    final int linkIndex = ((Pair<Integer, Integer>) itemId).getSecond();
                    isDown = currentNetPlan.getLink(linkIndex).isDown() || currentNetPlan.getLink(linkIndex).getOriginNode().isDown();
                    break;

                default:
                    throw new RuntimeException("Bad");
            }

            if (isDown) {
                c.setBackground(Color.RED);
            } else {
                setCurrentStateUp(c, table, itemId, rowIndexModel, columnIndexModel, isSelected);
            }
        }

        /**
         * Sets the cell properties for the current state.
         *
         * @param c                Component
         * @param table            Table
         * @param itemId           Item id (node identifier...)
         * @param rowIndexModel    Row index in model order
         * @param columnIndexModel Column index in model order
         * @param isSelected       Indicates whether or not the cell is selected
         * @since 0.3.0
         */
        public void setCurrentStateUp(Component c, JTable table, Object itemId, int rowIndexModel, int columnIndexModel, boolean isSelected) {
        }
    }

    /**
     * Renderer that disables cell focusing.
     *
     * @since 0.3.0
     */
    public static class UnfocusableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
        }
    }
}
