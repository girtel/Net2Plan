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

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Warning: This menu is meant for the Layer and Network special tables, do not use with the common tables.
 * Created by Jorge San Emeterio on 16/03/17.
 */
public class SingleElementTagEditor extends MouseAdapter
{
    private final GUINetworkDesign networkDesign;
    private final NetworkElementType type;

    public SingleElementTagEditor(final GUINetworkDesign networkDesign, final NetworkElementType type)
    {
        this.networkDesign = networkDesign;
        this.type = type;
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if (SwingUtilities.isRightMouseButton(e))
        {
            final JPopupMenu popupMenu = new JPopupMenu();

            JMenuItem addTag = new JMenuItem("Add tag");
            addTag.addActionListener(e1 ->
            {
                JTextField txt_name = new JTextField(20);

                JPanel pane = new JPanel();
                pane.add(new JLabel("Tag: "));
                pane.add(txt_name);

                NetPlan netPlan = networkDesign.getDesign();

                while (true)
                {
                    int result = JOptionPane.showConfirmDialog(null, pane, "Please enter tag name", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return;
                    try
                    {
                        if (txt_name.getText().isEmpty()) continue;

                        String tag = txt_name.getText();

                        switch (type)
                        {
                            case NETWORK:
                                netPlan.addTag(tag);
                                break;
                            case LAYER:
                                netPlan.getNetworkLayerDefault().addTag(tag);
                                break;
                            default:
                                throw new RuntimeException("Bad");
                        }

                        networkDesign.updateVisualizationJustTables();
                    } catch (Throwable ex)
                    {
                        ErrorHandling.addErrorOrException(ex, getClass());
                        ErrorHandling.showErrorDialog("Error adding/editing tag");
                    }
                    break;
                }
            });
            popupMenu.add(addTag);

            JMenuItem removeTag = new JMenuItem("Remove tag");
            removeTag.addActionListener(e2 ->
            {
                NetPlan netPlan = networkDesign.getDesign();

                try
                {
                    Set<String> tagList;

                    switch (type)
                    {
                        case NETWORK:
                            tagList = netPlan.getTags();
                            break;
                        case LAYER:
                            tagList = netPlan.getNetworkLayerDefault().getTags();
                            break;
                        default:
                            throw new RuntimeException("Bad");
                    }

                    final String[] tagArray = tagList.toArray(new String[tagList.size()]);
                    if (tagArray.length == 0) throw new Exception("No tag to remove");

                    Object out = JOptionPane.showInputDialog(null, "Please, select a tag to remove", "Remove tag", JOptionPane.QUESTION_MESSAGE, null, tagArray, tagArray[0]);
                    if (out == null) return;

                    String tagToRemove = out.toString();

                    switch (type)
                    {
                        case NETWORK:
                            netPlan.removeTag(tagToRemove);
                            break;
                        case LAYER:
                            netPlan.getNetworkLayerDefault().removeTag(tagToRemove);
                            break;
                        default:
                            throw new RuntimeException("Bad");
                    }

                    networkDesign.updateVisualizationJustTables();
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing tag");
                }
            });

            popupMenu.add(removeTag);

            JMenuItem removeAllTag = new JMenuItem("Remove all tags");
            removeAllTag.addActionListener(e2 ->
            {
                NetPlan netPlan = networkDesign.getDesign();

                try
                {
                    Set<String> tagList;

                    switch (type)
                    {
                        case NETWORK:
                            tagList = netPlan.getTags();
                            break;
                        case LAYER:
                            tagList = netPlan.getNetworkLayerDefault().getTags();
                            break;
                        default:
                            throw new RuntimeException("Bad");
                    }

                    if (tagList.size() == 0) throw new Exception("No tag to remove");

                    final HashSet<String> auxList = new HashSet<>(tagList);
                    switch (type)
                    {
                        case NETWORK:
                            for (String tag : auxList)
                                netPlan.removeTag(tag);
                            break;
                        case LAYER:
                            for (String tag : auxList)
                                netPlan.getNetworkLayerDefault().removeTag(tag);
                            break;
                        default:
                            throw new RuntimeException("Bad");
                    }

                    networkDesign.updateVisualizationJustTables();
                } catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing tag");
                }
            });
            popupMenu.addSeparator();
            popupMenu.add(removeAllTag);

            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
