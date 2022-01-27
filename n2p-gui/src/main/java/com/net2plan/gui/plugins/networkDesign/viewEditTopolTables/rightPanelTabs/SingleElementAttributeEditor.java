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
package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.rightPanelTabs;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.StringUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

class SingleElementAttributeEditor extends MouseAdapter {
    private final GUINetworkDesign callback;
    private final NetworkElementType type;

    public SingleElementAttributeEditor(final GUINetworkDesign callback, final NetworkElementType type) {
        this.callback = callback;
        this.type = type;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            final JTable table = (JTable) e.getSource();
            final NetPlan netPlan = callback.getDesign();

            JPopupMenu popup = new JPopupMenu();

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

                    while (true) {
                        int result = JOptionPane.showConfirmDialog(null, pane, "Please enter an attribute name and its value", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (result != JOptionPane.OK_OPTION) return;

                        try {
                            if (txt_key.getText().isEmpty())
                                throw new Exception("Please, insert an attribute name");

                            switch (type) {
                                case NETWORK:
                                    netPlan.setAttribute(txt_key.getText(), txt_value.getText());
                                    break;

                                case LAYER:
                                    netPlan.getNetworkLayerDefault().setAttribute(txt_key.getText(), txt_value.getText());
                                    break;

                                default:
                                    ErrorHandling.showErrorDialog("Bad", "Internal error");
                                    return;
                            }

                            callback.updateVisualizationJustTables();
                            return;
                        } catch (Exception ex) {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error adding/editing attribute");
                        }
                    }
                }
            });

            popup.add(addAttribute);

            int numAttributes;
            switch (type) {
                case NETWORK:
                    numAttributes = netPlan.getAttributes().size();
                    break;

                case LAYER:
                    numAttributes = netPlan.getNetworkLayerDefault().getAttributes().size();
                    break;

                default:
                    ErrorHandling.showErrorDialog("Bad", "Internal error");
                    return;
            }

            if (numAttributes > 0) {
                JMenuItem removeAttribute = new JMenuItem("Remove attribute");

                removeAttribute.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            String[] attributeList;

                            switch (type) {
                                case NETWORK:
                                    attributeList = StringUtils.toArray(netPlan.getAttributes().keySet());
                                    break;

                                case LAYER:
                                    attributeList = StringUtils.toArray(netPlan.getNetworkLayerDefault().getAttributes().keySet());
                                    break;

                                default:
                                    ErrorHandling.showErrorDialog("Bad", "Internal error");
                                    return;
                            }

                            if (attributeList.length == 0) throw new Exception("No attribute to remove");

                            Object out = JOptionPane.showInputDialog(null, "Please, select an attribute to remove", "Remove attribute", JOptionPane.QUESTION_MESSAGE, null, attributeList, attributeList[0]);
                            if (out == null) return;

                            String attributeToRemove = out.toString();

                            switch (type) {
                                case NETWORK:
                                    netPlan.removeAttribute(attributeToRemove);
                                    break;

                                case LAYER:
                                    netPlan.getNetworkLayerDefault().removeAttribute(attributeToRemove);
                                    break;

                                default:
                                    ErrorHandling.showErrorDialog("Bad", "Internal error");
                                    return;
                            }

                            callback.updateVisualizationJustTables();
                        } catch (Exception ex) {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing attribute");
                        }
                    }
                });

                popup.add(removeAttribute);

                JMenuItem removeAttributes = new JMenuItem("Remove all attributes");

                removeAttributes.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            switch (type) {
                                case NETWORK:
                                    netPlan.setAttributeMap(new HashMap<String, String>());
                                    break;

                                case LAYER:
                                    netPlan.getNetworkLayerDefault().setAttributeMap(new HashMap<String, String>());
                                    break;

                                default:
                                    ErrorHandling.showErrorDialog("Bad", "Internal error");
                                    return;
                            }

                            callback.updateVisualizationJustTables();
                        } catch (Exception ex) {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing attributes");
                        }
                    }
                });

                popup.add(removeAttributes);
            }

            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
