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

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.DialogBuilder;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.InputForDialog;
import com.net2plan.gui.utils.StringLabeller;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.libraries.IPUtils;
import com.net2plan.niw.WFlexAlgo;
import com.net2plan.niw.WIpLink;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNode;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.collections4.iterators.EntrySetMapIterator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 */
public class Niw_AdvancedJTable_SegmentRouting extends AdvancedJTable_networkElement<WFlexAlgo.FlexAlgoProperties>
{
    public Niw_AdvancedJTable_SegmentRouting(GUINetworkDesign callback, NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.FLEXALGO, null, layerThisTable, false, null);
    }


    /* Helping functions */
    public static final Function<Node, WNode> toWNode = n -> {
        final WNode nn = new WNode(n);
        assert !nn.isVirtualNode();
        return nn;
    };
    public static final Function<Pair<Link, WNet>, WIpLink> toWIpLink = pair -> (WIpLink) pair.getSecond().getWElement(pair.getFirst()).get();


    @Override
    public List<AjtColumnInfo<WFlexAlgo.FlexAlgoProperties>> getNonBasicUserDefinedColumnsVisibleOrNot()
    {
        assert callback.getNiwInfo().getFirst();

        final NetPlan np = callback.getDesign();
        final NetworkLayer layer = this.getTableNetworkLayer();


        final List<AjtColumnInfo<WFlexAlgo.FlexAlgoProperties>> res = new LinkedList<>();
        // TODO buscar como seleccionar los enlaces incuidos en la lista. WNode -> outIpLinks

        res.add(new AjtColumnInfo<>(this, Integer.class, null, "ID (k)", "The flexible algo identifier", null, WFlexAlgo.FlexAlgoProperties::getK, AGTYPE.NOAGGREGATION, null));
        res.add(new AjtColumnInfo<>(this, String.class, null, "Calculation", "The flexible algo calculation type", null, WFlexAlgo.FlexAlgoProperties::getCalculationString, AGTYPE.NOAGGREGATION, null));
        res.add(new AjtColumnInfo<>(this, String.class, null, "Weight", "The flexible algo weight type", null, WFlexAlgo.FlexAlgoProperties::getWeightTypeString, AGTYPE.NOAGGREGATION, null));

        res.add(new AjtColumnInfo<>(this, Collection.class, null, "Links", "List of links that the flex algo can go through", null, f -> f.getLinksIncluded(np), AGTYPE.SUMCOLLECTIONCOUNT, null));
        res.add(new AjtColumnInfo<>(this, Collection.class, null, "Nodes", "List of nodes that are using this flex algo", null, f -> f.getNodesAssociated(np), AGTYPE.SUMCOLLECTIONCOUNT, null));


        return res;
    }

    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
        final NetPlan np = callback.getDesign();
        final NetworkLayer layer = this.getTableNetworkLayer();


//        assert callback.getNiwInfo().getFirst();
        final WNet wNet = callback.getNiwInfo().getSecond();


        final List<AjtRcMenu> res = new ArrayList<>();

        res.add(new AjtRcMenu("Add FlexAlgo", e -> createFlexAlgoFromGUI(callback, layer), (a, b) -> true, null));

        // TODO the remaining actions

        return res;
    }


    private static void createFlexAlgoFromGUI(final GUINetworkDesign callback, final NetworkLayer layer)
    {
        /* Check for some first steps */
        final NetPlan netPlan = callback.getDesign();
        final WNet wNet = callback.getNiwInfo().getSecond();
        if(!wNet.isSrInitialized()) wNet.initializeFlexAlgoAttributes();


        /* ID Selector */
        JTextField idText = new JTextField(6);
        JLabel idLabel = new JLabel("Identifier (k) of FlexAlgo. [128, 255]");

        /* Calculation selector */
        List<Pair<String, Integer>> calculationTypes = WFlexAlgo.FlexAlgoProperties.getCalculationOptions();
        JComboBox<String> calculationSelector = new WiderJComboBox<>();
        calculationTypes.forEach(pair -> calculationSelector.addItem(pair.getFirst()));
        JLabel calculationLabel = new JLabel("Calculation type");

        /* Weight selector */
        List<Pair<String, Integer>> weightTypes = WFlexAlgo.FlexAlgoProperties.getWeightOptions();
        JComboBox<String> weightSelector = new WiderJComboBox<>();
        weightTypes.forEach(pair -> weightSelector.addItem(pair.getFirst()));
        JLabel weightLabel = new JLabel("Calculation type");

        /* Link selector */
        JList<Link> linkList = new JList(wNet.getIpLinks().toArray());
        linkList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        linkList.setLayoutOrientation(JList.VERTICAL);
        linkList.setVisibleRowCount(-1);
        JScrollPane linkListScroller = new JScrollPane(linkList);
        linkListScroller.setPreferredSize(new Dimension(500, 160));

        /* Node selector */
//        JList<Node> nodeList = new JList(wNet.getNodes().toArray());
        List<Triple<String, Long, String>> candidateNodeTuples = new ArrayList<>();
        Map<String, Pair<Long, String>> candidateNodeMap = new TreeMap<>();
        wNet.getNodes().stream().forEach(node -> {
            node.getSidList().ifPresent( sidList -> sidList.forEach(sid -> candidateNodeMap.put(node.getName() + " (" + sid.trim() + ")", Pair.of(node.getId(), sid)) ) );
        });
        JList<String> nodeList = new JList(candidateNodeMap.keySet().toArray());

        nodeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        nodeList.setLayoutOrientation(JList.VERTICAL);
        nodeList.setVisibleRowCount(-1);
        JScrollPane nodeListScroller = new JScrollPane(nodeList);
        nodeListScroller.setPreferredSize(new Dimension(500, 160));


        /* Main panel construction */
        JPanel mainPanel = new JPanel(new MigLayout("fill", "[][grow]", "[][][][][]"));
        mainPanel.add(idLabel);
        mainPanel.add(idText, "wrap");
        mainPanel.add(calculationLabel);
        mainPanel.add(calculationSelector, "wrap");
        mainPanel.add(weightLabel);
        mainPanel.add(weightSelector, "wrap");
        mainPanel.add(new JLabel("SID-Nodes (optional)"));
        mainPanel.add(new JLabel("Links that will be added to the FlexAlgo."), "wrap");
        mainPanel.add(nodeListScroller);
        mainPanel.add(linkListScroller);
        mainPanel.add(new JLabel(""));
        mainPanel.add(new JLabel("Links are generated if two selected nodes have link between them"), "wrap");




        /* Events */

        // Listener for identifier checker. ID must belong to [128, 255] and not be already in use
        idText.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String text = idText.getText();
                try
                {
                    int id = Integer.parseInt(text);

                    // Check range
                    if (id < 128 || id > 255) throw new NumberFormatException();

                    // Check that it is not already in use
                    wNet.performOperationOnFlexAlgoRepository(repo -> {
                        if (repo.containsKey(id)) throw new NumberFormatException();
                    });

                    // If correct
                    idText.setBorder(BorderFactory.createLineBorder(Color.GREEN));
                } catch (NumberFormatException nfe) { idText.setBorder(BorderFactory.createLineBorder(Color.RED)); }
            }
        });


        // Selection panels
        ListSelectionModel nodeSelectionModel = new DefaultListSelectionModel()
        {
            boolean gestureStarted = false;
            @Override
            public void setSelectionInterval(int index0, int index1)
            {
                if (!gestureStarted)
                {
                    if (isSelectedIndex(index0)) super.removeSelectionInterval(index0, index1);
                    else super.addSelectionInterval(index0, index1);
                }
                gestureStarted = true;
            }

            @Override
            public void setValueIsAdjusting(boolean isAdjusting) { if (!isAdjusting) gestureStarted = false; }
        }; nodeList.setSelectionModel(nodeSelectionModel);

        ListSelectionModel linkSelectionModel = new DefaultListSelectionModel()
        {
            boolean gestureStarted = false;
            @Override
            public void setSelectionInterval(int index0, int index1)
            {
                if (!gestureStarted)
                {
                    if (isSelectedIndex(index0)) super.removeSelectionInterval(index0, index1);
                    else super.addSelectionInterval(index0, index1);
                }
                gestureStarted = true;
            }

            @Override
            public void setValueIsAdjusting(boolean isAdjusting) { if (!isAdjusting) gestureStarted = false; }
        }; linkList.setSelectionModel(linkSelectionModel);




        // Launch the confirm dialog and wait to fill information. When information recovered, do all the stuff
        while (true)
        {
            /* Launch the dialog */
            int result = JOptionPane.showConfirmDialog(null, mainPanel, "Please enter information for the new flex algo", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;


            try
            {
                /* Recover information from all tables */ //TODO
                int k = Integer.parseInt(idText.getText());
                int calculation = calculationTypes.stream().filter(pair -> pair.getFirst().equals(calculationSelector.getSelectedItem()) ).findFirst().get().getSecond();
                int weight = weightTypes.stream().filter(pair -> pair.getFirst().equals(weightSelector.getSelectedItem())).findFirst().get().getSecond();

                List<Link> links = linkList.getSelectedValuesList();
                Set<Long> linkIdList = links.stream().map(Link::getId).collect(Collectors.toSet());

                List<String> nodeRepresentatives = nodeList.getSelectedValuesList();
                List<Pair<Long, String>> selectedNodesInformation = nodeRepresentatives.stream().map(candidateNodeMap::get).collect(Collectors.toList());
                Set<String> flexNodes = selectedNodesInformation.stream().map(Pair::getSecond).collect(Collectors.toSet());



                // TODO check if k is already in use

                /* Create the FlexAlgoProperties */
                WFlexAlgo.FlexAlgoProperties wflex = new WFlexAlgo.FlexAlgoProperties(k, calculation, weight, Optional.of(linkIdList), Optional.of(flexNodes));
                wNet.performOperationOnFlexAlgoRepository(repo -> repo.mapFlexAlgoId2FlexAlgoProperties.put(k, wflex));

                /* Perform operation on the flex algo repository -> add the flex algo property */

                /* Perform operation on nodes (set this flex algo as used), links, etc */



            } catch (Exception e) { continue; }







            break;
        }


    }
}
