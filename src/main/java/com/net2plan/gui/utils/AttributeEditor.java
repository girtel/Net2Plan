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


package com.net2plan.gui.utils;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.net2plan.gui.utils.topology.INetworkCallback;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.Pair;

/**
 * Utility to edit attributes from a set of elements (i.e. nodes, links, and so on).
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
public class AttributeEditor extends JDialog implements ActionListener {
    private final JTable table;
    private final ArrayList<Long> itemIds;

    /**
     * Default constructor.
     *
     * @param callback Reference to the handler of the network design
     * @param type     Type of element (i.e. layers, nodes, links, and so on)
     * @since 0.3.0
     */
    public AttributeEditor(final INetworkCallback callback, final NetworkElementType type) {
        NetPlan netPlan = callback.getDesign();

        Object[][] data;
        String[] columnNames;
        final Set<String> attributes = new HashSet<String>();
        String dialogHeader;

        switch (type) {
            case LAYER:
                ArrayList<Long> layerIds = netPlan.getNetworkLayerIds();
                itemIds = layerIds;
                int L = layerIds.size();

                columnNames = new String[L + 1];
                columnNames[0] = "Attribute";
                for (int l = 0; l < L; l++) {
                    long layerId = layerIds.get(l);
                    columnNames[l + 1] = generateColumnName(type, layerId);
                    attributes.addAll(netPlan.getNetworkLayerFromId(layerId).getAttributes().keySet());
                }

                if (attributes.isEmpty()) {
                    data = new Object[1][L + 1];
                } else {
                    data = new Object[attributes.size()][L + 1];

                    int itemId = 0;
                    Iterator<String> it = attributes.iterator();
                    while (it.hasNext()) {
                        String attribute = it.next();
                        data[itemId][0] = attribute;
                        for (int l = 0; l < L; l++) {
                            long layerId = layerIds.get(l);
                            data[itemId][l + 1] = netPlan.getNetworkLayerFromId(layerId).getAttribute(attribute);
                            if (data[itemId][l + 1] == null) {
                                data[itemId][l + 1] = "";
                            }
                        }

                        itemId++;
                    }
                }

                dialogHeader = "Edit layer attributes";

                break;

            case NODE:
                ArrayList<Long> nodeIds = netPlan.getNodeIds();
                itemIds = nodeIds;
                int N = nodeIds.size();

                columnNames = new String[N + 1];
                columnNames[0] = "Attribute";
                for (int n = 0; n < N; n++) {
                    long nodeId = nodeIds.get(n);
                    columnNames[n + 1] = generateColumnName(type, nodeId);
                    attributes.addAll(netPlan.getNodeFromId(nodeId).getAttributes().keySet());
                }

                if (attributes.isEmpty()) {
                    data = new Object[1][N + 1];
                } else {
                    data = new Object[attributes.size()][N + 1];

                    int itemId = 0;
                    Iterator<String> it = attributes.iterator();
                    while (it.hasNext()) {
                        String attribute = it.next();
                        data[itemId][0] = attribute;
                        for (int n = 0; n < N; n++) {
                            long nodeId = nodeIds.get(n);
                            data[itemId][n + 1] = netPlan.getNodeFromId(nodeId).getAttribute(attribute);
                            if (data[itemId][n + 1] == null) data[itemId][n + 1] = "";
                        }

                        itemId++;
                    }
                }

                dialogHeader = "Edit node attributes";

                break;

            case LINK:
                ArrayList<Long> linkIds = netPlan.getLinkIds();
                itemIds = linkIds;
                int E = linkIds.size();

                columnNames = new String[E + 1];
                columnNames[0] = "Attribute";
                for (int e = 0; e < E; e++) {
                    long linkId = linkIds.get(e);
                    columnNames[e + 1] = generateColumnName(type, linkId);
                    attributes.addAll(netPlan.getLinkFromId(linkId).getAttributes().keySet());
                }

                if (attributes.isEmpty()) {
                    data = new Object[1][E + 1];
                } else {
                    data = new Object[attributes.size()][E + 1];

                    int itemId = 0;
                    Iterator<String> it = attributes.iterator();
                    while (it.hasNext()) {
                        String attribute = it.next();
                        data[itemId][0] = attribute;
                        for (int e = 0; e < E; e++) {
                            long linkId = linkIds.get(e);
                            data[itemId][e + 1] = netPlan.getLinkFromId(linkId).getAttribute(attribute);
                            if (data[itemId][e + 1] == null) data[itemId][e + 1] = "";
                        }

                        itemId++;
                    }
                }

                dialogHeader = "Edit link attributes";
                break;

            case DEMAND: {
                ArrayList<Long> demandIds = netPlan.getDemandIds();
                itemIds = demandIds;
                int D = demandIds.size();

                columnNames = new String[D + 1];
                columnNames[0] = "Attribute";
                for (int d = 0; d < D; d++) {
                    long demandId = demandIds.get(d);
                    columnNames[d + 1] = generateColumnName(type, demandId);
                    attributes.addAll(netPlan.getDemandFromId(demandId).getAttributes().keySet());
                }

                if (attributes.isEmpty()) {
                    data = new Object[1][D + 1];
                } else {
                    data = new Object[attributes.size()][D + 1];

                    int itemId = 0;
                    Iterator<String> it = attributes.iterator();
                    while (it.hasNext()) {
                        String attribute = it.next();
                        data[itemId][0] = attribute;
                        for (int d = 0; d < D; d++) {
                            long demandId = demandIds.get(d);
                            data[itemId][d + 1] = netPlan.getDemandFromId(demandId).getAttribute(attribute);
                            if (data[itemId][d + 1] == null) data[itemId][d + 1] = "";
                        }

                        itemId++;
                    }
                }

                dialogHeader = "Edit demand attributes";
                break;
            }
            case MULTICAST_DEMAND:
                ArrayList<Long> demandIds = netPlan.getMulticastDemandIds();
                itemIds = demandIds;
                int D = demandIds.size();

                columnNames = new String[D + 1];
                columnNames[0] = "Attribute";
                for (int d = 0; d < D; d++) {
                    long demandId = demandIds.get(d);
                    columnNames[d + 1] = generateColumnName(type, demandId);
                    attributes.addAll(netPlan.getMulticastDemandFromId(demandId).getAttributes().keySet());
                }

                if (attributes.isEmpty()) {
                    data = new Object[1][D + 1];
                } else {
                    data = new Object[attributes.size()][D + 1];

                    int itemId = 0;
                    Iterator<String> it = attributes.iterator();
                    while (it.hasNext()) {
                        String attribute = it.next();
                        data[itemId][0] = attribute;
                        for (int d = 0; d < D; d++) {
                            long demandId = demandIds.get(d);
                            data[itemId][d + 1] = netPlan.getMulticastDemandFromId(demandId).getAttribute(attribute);
                            if (data[itemId][d + 1] == null) data[itemId][d + 1] = "";
                        }

                        itemId++;
                    }
                }

                dialogHeader = "Edit multicast demand attributes";
                break;

            case MULTICAST_TREE:
                ArrayList<Long> treeIds = netPlan.getMulticastTreeIds();
                itemIds = treeIds;
                int T = treeIds.size();

                columnNames = new String[T + 1];
                columnNames[0] = "Attribute";
                for (int t = 0; t < T; t++) {
                    long treeId = treeIds.get(t);
                    columnNames[t + 1] = generateColumnName(type, treeId);
                    attributes.addAll(netPlan.getMulticastTreeFromId(treeId).getAttributes().keySet());
                }

                if (attributes.isEmpty()) {
                    data = new Object[1][T + 1];
                } else {
                    data = new Object[attributes.size()][T + 1];

                    int itemId = 0;
                    Iterator<String> it = attributes.iterator();
                    while (it.hasNext()) {
                        String attribute = it.next();
                        data[itemId][0] = attribute;
                        for (int t = 0; t < T; t++) {
                            long treeId = treeIds.get(t);
                            data[itemId][t + 1] = netPlan.getMulticastTreeFromId(treeId).getAttribute(attribute);
                            if (data[itemId][t + 1] == null) data[itemId][t + 1] = "";
                        }

                        itemId++;
                    }
                }

                dialogHeader = "Edit multicast tree attributes";
                break;

            case ROUTE:
                ArrayList<Long> routeIds = netPlan.getRouteIds();
                itemIds = routeIds;
                int R = routeIds.size();

                columnNames = new String[R + 1];
                columnNames[0] = "Attribute";
                for (int p = 0; p < R; p++) {
                    long routeId = routeIds.get(p);
                    columnNames[p + 1] = generateColumnName(type, routeId);
                    attributes.addAll(netPlan.getRouteFromId(routeId).getAttributes().keySet());
                }

                if (attributes.isEmpty()) {
                    data = new Object[1][R + 1];
                } else {
                    data = new Object[attributes.size()][R + 1];

                    int itemId = 0;
                    Iterator<String> it = attributes.iterator();
                    while (it.hasNext()) {
                        String attribute = it.next();
                        data[itemId][0] = attribute;
                        for (int p = 0; p < R; p++) {
                            long routeId = routeIds.get(p);
                            data[itemId][p + 1] = netPlan.getRouteFromId(routeId).getAttribute(attribute);
                            if (data[itemId][p + 1] == null) data[itemId][p + 1] = "";
                        }

                        itemId++;
                    }
                }

                dialogHeader = "Edit route attributes";
                break;

            case PROTECTION_SEGMENT:
                ArrayList<Long> segmentIds = netPlan.getProtectionSegmentIds();
                itemIds = segmentIds;
                int S = segmentIds.size();

                columnNames = new String[S + 1];
                columnNames[0] = "Attribute";
                for (int s = 0; s < S; s++) {
                    long segmentId = segmentIds.get(s);
                    columnNames[s + 1] = generateColumnName(type, segmentId);
                    attributes.addAll(netPlan.getProtectionSegmentFromId(segmentId).getAttributes().keySet());
                }

                if (attributes.isEmpty()) {
                    data = new Object[1][S + 1];
                } else {
                    data = new Object[attributes.size()][S + 1];

                    int itemId = 0;
                    Iterator<String> it = attributes.iterator();
                    while (it.hasNext()) {
                        String attribute = it.next();
                        data[itemId][0] = attribute;
                        for (int segmentIndex = 0; segmentIndex < S; segmentIndex++) {
                            long segmentId = segmentIds.get(segmentIndex);
                            data[itemId][segmentIndex + 1] = netPlan.getProtectionSegmentFromId(segmentId).getAttribute(attribute);
                            if (data[itemId][segmentIndex + 1] == null) data[itemId][segmentIndex + 1] = "";
                        }

                        itemId++;
                    }
                }

                dialogHeader = "Edit protection segment attributes";
                break;

//			case FORWARDING_RULE:
//				List<Pair<Long, Long>> forwardingRules = new ArrayList<Pair<Long, Long>>(netPlan.getForwardingRules());
//				itemIds = forwardingRules;
//				int F = forwardingRules.size();
//
//				columnNames = new String[F + 1];
//				columnNames[0] = "Attribute";
//				for (int f = 0; f < F; f++)
//				{
//					Pair<Long, Long> demandLinkPair = forwardingRules.get(f);
//					columnNames[f + 1] = generateColumnName(type, demandLinkPair);
//					attributes.addAll(netPlan.getForwardingRuleAttributeMap(demandLinkPair).keySet());
//				}
//
//				if (attributes.isEmpty())
//				{
//					data = new Object[1][F + 1];
//				}
//				else
//				{
//					data = new Object[attributes.size()][F + 1];
//
//					int itemId = 0;
//					Iterator<String> it = attributes.iterator();
//					while (it.hasNext())
//					{
//						String attribute = it.next();
//						data[itemId][0] = attribute;
//						for (int f = 0; f < F; f++)
//						{
//							Pair<Long, Long> demandLinkPair = forwardingRules.get(f);
//							data[itemId][f + 1] = netPlan.getForwardingRuleAttribute(demandLinkPair, attribute);
//							if (data[itemId][f + 1] == null) data[itemId][f + 1] = "";
//						}
//
//						itemId++;
//					}
//				}
//
//				dialogHeader = "Edit forwarding rule attributes";
//				break;

            case SRG:
                ArrayList<Long> srgIds = netPlan.getSRGIds();
                itemIds = srgIds;
                int numSRGs = srgIds.size();

                columnNames = new String[numSRGs + 1];
                columnNames[0] = "Attribute";
                for (int srgIndex = 0; srgIndex < numSRGs; srgIndex++) {
                    long srgId = srgIds.get(srgIndex);
                    columnNames[srgIndex + 1] = generateColumnName(type, srgId);
                    attributes.addAll(netPlan.getSRGFromId(srgId).getAttributes().keySet());
                }

                if (attributes.isEmpty()) {
                    data = new Object[1][numSRGs + 1];
                } else {
                    data = new Object[attributes.size()][numSRGs + 1];

                    int itemId = 0;
                    Iterator<String> it = attributes.iterator();
                    while (it.hasNext()) {
                        String attribute = it.next();
                        data[itemId][0] = attribute;
                        for (int srgIndex = 0; srgIndex < numSRGs; srgIndex++) {
                            long srgId = srgIds.get(srgIndex);
                            data[itemId][srgIndex + 1] = netPlan.getSRGFromId(srgId).getAttribute(attribute);
                            if (data[itemId][srgIndex + 1] == null) data[itemId][srgIndex + 1] = "";
                        }

                        itemId++;
                    }
                }

                dialogHeader = "Edit SRG attributes";
                break;

            default:
                throw new IllegalArgumentException("Invalid network element type");
        }

        DefaultTableModel model = new ClassAwareTableModelImpl(data, columnNames, type, netPlan);

        table = new AdvancedJTable(model);
        table.addMouseListener(new PopupAttributeMenu(table, attributes));
        if (attributes.isEmpty()) table.setEnabled(false);

        JScrollPane pane = new JScrollPane(table);
        new FixedColumnDecorator(pane, 1);

        SwingUtils.configureCloseDialogOnEscape(this);
        setTitle(dialogHeader);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(pane, BorderLayout.CENTER);
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Constructor that allows to show just a single item.
     *
     * @param callback Reference to the handler of the network design
     * @param type     Type of element (i.e. layers, nodes, links, and so on)
     * @param itemId   Item identifier
     * @since 0.3.0
     */
    public AttributeEditor(final INetworkCallback callback, final NetworkElementType type, Object itemId) {
        this(callback, type);

        final TableColumnHider tch = new TableColumnHider(table);
        String columnName = generateColumnName(type, itemId);
        tch.maintainOnly(columnName);

        final JPanel pane = new JPanel();

        if (table.getModel().getColumnCount() > 2) {
            JButton viewAll = new JButton("View all " + type.toString() + "s");
            viewAll.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    getContentPane().remove(pane);
                    getContentPane().revalidate();

                    tch.showAll();
                }
            });

            pane.add(viewAll);
        }

        getContentPane().add(pane, BorderLayout.NORTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dispose();
    }

    private String generateColumnName(final NetworkElementType type, Object itemId) {
        switch (type) {
            case DEMAND:
                return "Demand " + (long) itemId;

            case MULTICAST_DEMAND:
                return "Multicast demand " + (long) itemId;

            case MULTICAST_TREE:
                return "Multicast tree " + (long) itemId;

            case FORWARDING_RULE:
                return "[d" + ((Pair<Integer, Integer>) itemId).getFirst() + ", e" + ((Pair<Integer, Integer>) itemId).getSecond() + "]";

            case LAYER:
                return "Layer " + (long) itemId;

            case LINK:
                return "Link " + (long) itemId;

            case NODE:
                return "Node " + (long) itemId;

            case PROTECTION_SEGMENT:
                return "Protection segment " + (long) itemId;

            case ROUTE:
                return "Route " + (long) itemId;

            case SRG:
                return "SRG " + (long) itemId;

            default:
                throw new RuntimeException("Bad");
        }
    }

    private static class PopupAttributeMenu extends MouseAdapter {
        private final JTable table;
        private final Set<String> attributes;

        public PopupAttributeMenu(JTable table, Set<String> attributes) {
            this.table = table;
            this.attributes = attributes;
        }

        private void doPopup(MouseEvent e) {
            final int row = table.rowAtPoint(e.getPoint());
            final DefaultTableModel model = (DefaultTableModel) table.getModel();

            JPopupMenu popup = new JPopupMenu();

            JMenuItem addAttribute = new JMenuItem("Add attribute");
            addAttribute.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String attributeName;

                    while (true) {
                        attributeName = JOptionPane.showInputDialog("Please enter new attribute name: ");
                        if (attributeName == null) return;

                        try {
                            if (attributes.contains(attributeName))
                                throw new RuntimeException("Attribute '" + attributeName + "' already exists");
                            if (attributeName.isEmpty())
                                throw new RuntimeException("Attribute cannot be null or empty");
                        } catch (Throwable ex) {
                            ErrorHandling.showWarningDialog(ex.getMessage(), "Error adding attribute");
                            continue;
                        }

                        break;
                    }

                    int numColumns = model.getColumnCount();
                    String[] newRow = new String[numColumns];
                    Arrays.fill(newRow, "");

                    newRow[0] = attributeName;
                    model.addRow(newRow);

                    if (!table.isEnabled()) {
                        model.removeRow(0);
                        table.setEnabled(true);
                    }
                }
            });

            popup.add(addAttribute);

            if (table.isEnabled() && row != -1) {
                JMenuItem removeAttribute = new JMenuItem("Remove attribute");
                removeAttribute.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int actualRow = table.convertRowIndexToModel(row);
                        String attributeName = (String) model.getValueAt(actualRow, 0);

                        int rc = JOptionPane.showConfirmDialog(null, "Are you sure?", "Removing attribute '" + attributeName + "'", JOptionPane.YES_NO_OPTION);
                        if (rc != JOptionPane.YES_OPTION) return;

                        int numColumns = model.getColumnCount();
                        for (int columnId = 1; columnId < numColumns; columnId++)
                            model.setValueAt("", actualRow, columnId);

                        model.removeRow(actualRow);
                        if (model.getRowCount() == 0) {
                            model.addRow(new Object[1][numColumns]);
                            table.setEnabled(false);
                        }
                    }
                });

                popup.add(removeAttribute);

                if (model.getColumnCount() > 2) {
                    JMenuItem setValueToAll = new JMenuItem("Set value attribute for all elements");
                    setValueToAll.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            int actualRow = table.convertRowIndexToModel(row);
                            String attributeName = (String) model.getValueAt(actualRow, 0);
                            String attributeValue;

                            while (true) {
                                attributeValue = JOptionPane.showInputDialog("Please enter new value for attribute '" + attributeName + "': ");
                                if (attributeValue == null) return;

                                break;
                            }

                            int numColumns = model.getColumnCount();
                            for (int columnId = 1; columnId < numColumns; columnId++)
                                model.setValueAt(attributeValue, actualRow, columnId);
                        }
                    });

                    popup.add(setValueToAll);
                }
            }

            popup.show(e.getComponent(), e.getX(), e.getY());
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            table.clearSelection();

            if (SwingUtilities.isRightMouseButton(e)) doPopup(e);
        }
    }

    private class ClassAwareTableModelImpl extends ClassAwareTableModel {
        private final NetworkElementType type;
        private final NetPlan netPlan;

        public ClassAwareTableModelImpl(Object[][] dataVector, Object[] columnIdentifiers, NetworkElementType type, NetPlan netPlan) {
            super(dataVector, columnIdentifiers);
            this.type = type;
            this.netPlan = netPlan;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != 0;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            String oldValue = getValueAt(row, col).toString();
            String newValue = value.toString();

            if (newValue.equals(oldValue)) return;

            if (col != 0) {
                String key = getValueAt(row, 0).toString();

                try {
                    switch (type) {
                        case LAYER:
                            long layerId = itemIds.get(col - 1);
                            if (newValue.isEmpty()) netPlan.getNetworkLayerFromId(layerId).removeAttribute(key);
                            else netPlan.getNetworkLayerFromId(layerId).setAttribute(key, newValue);
                            break;

                        case NODE:
                            long nodeId = itemIds.get(col - 1);
                            if (newValue.isEmpty()) netPlan.getNodeFromId(nodeId).removeAttribute(key);
                            else netPlan.getNodeFromId(nodeId).setAttribute(key, newValue);
                            break;

                        case LINK:
                            long linkId = itemIds.get(col - 1);
                            if (newValue.isEmpty()) netPlan.getLinkFromId(linkId).removeAttribute(key);
                            else netPlan.getLinkFromId(linkId).setAttribute(key, newValue);
                            break;

                        case DEMAND:
                            long demandId = itemIds.get(col - 1);
                            if (newValue.isEmpty()) netPlan.getDemandFromId(demandId).removeAttribute(key);
                            else netPlan.getDemandFromId(demandId).setAttribute(key, newValue);
                            break;

                        case MULTICAST_DEMAND:
                            long multicastDemandId = itemIds.get(col - 1);
                            if (newValue.isEmpty())
                                netPlan.getMulticastDemandFromId(multicastDemandId).removeAttribute(key);
                            else netPlan.getMulticastDemandFromId(multicastDemandId).setAttribute(key, newValue);
                            break;

                        case MULTICAST_TREE:
                            long multicastTreeId = itemIds.get(col - 1);
                            if (newValue.isEmpty())
                                netPlan.getMulticastTreeFromId(multicastTreeId).removeAttribute(key);
                            else netPlan.getMulticastTreeFromId(multicastTreeId).setAttribute(key, newValue);
                            break;

                        case ROUTE:
                            long routeId = itemIds.get(col - 1);
                            if (newValue.isEmpty()) netPlan.getRouteFromId(routeId).removeAttribute(key);
                            else netPlan.getRouteFromId(routeId).setAttribute(key, newValue);
                            break;

                        case PROTECTION_SEGMENT:
                            long segmentId = itemIds.get(col - 1);
                            if (newValue.isEmpty()) netPlan.getProtectionSegmentFromId(segmentId).removeAttribute(key);
                            else netPlan.getProtectionSegmentFromId(segmentId).setAttribute(key, newValue);
                            break;

//						case FORWARDING_RULE:
//							Pair<Long, Long> demandLinkPair = ((List<Pair<Long, Long>>) itemIds).get(col - 1);
//							if (newValue.isEmpty()) netPlan.removeForwardingRuleAttribute(demandLinkPair, key);
//							else netPlan.setForwardingRuleAttribute(demandLinkPair, key, newValue);
//							break;

                        case SRG:
                            long srgId = itemIds.get(col - 1);
                            if (newValue.isEmpty()) netPlan.getSRGFromId(srgId).removeAttribute(key);
                            else netPlan.getSRGFromId(srgId).setAttribute(key, newValue);
                            break;

                        default:
                            throw new IllegalArgumentException("Invalid network element type");
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    ErrorHandling.showErrorDialog(e.getMessage(), "Error modifying attributes");
                    return;
                }
            }

            super.setValueAt(newValue, row, col);
        }
    }
}
