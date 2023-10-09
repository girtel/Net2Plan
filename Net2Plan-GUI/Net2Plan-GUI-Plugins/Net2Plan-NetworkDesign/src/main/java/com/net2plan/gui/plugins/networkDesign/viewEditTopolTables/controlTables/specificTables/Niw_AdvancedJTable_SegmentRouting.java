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
import net.miginfocom.swing.MigLayout;

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


        assert callback.getNiwInfo().getFirst();
        final WNet wNet = callback.getNiwInfo().getSecond();


        final List<AjtRcMenu> res = new ArrayList<>();

        res.add(new AjtRcMenu("Add FlexAlgo", e -> createFlexAlgoFromGUI(callback, layer) , (a, b) -> b>0, null));

        // TODO the remaining actions

        return res;
    }





    private static void createFlexAlgoFromGUI(final GUINetworkDesign callback, final NetworkLayer layer)
    {
        final NetPlan netPlan = callback.getDesign();
        final WNet wNet = callback.getNiwInfo().getSecond();



        /* ID Selector */
        JTextField idText = new JTextField();
        JLabel idLabel = new JLabel("k of FlexAlgo. [128, 255]");
        JPanel idPanel = new JPanel(new FlowLayout());
        idPanel.add(idText);
        idPanel.add(idLabel);

        /* Calculation selector */
        List<Pair<String, Integer>> calculationTypes = WFlexAlgo.FlexAlgoProperties.getCalculationOptions();
        JComboBox<String> calculationSelector = new WiderJComboBox<>((String[]) calculationTypes.stream().map(Pair::getFirst).toArray());
        JLabel calculationLabel = new JLabel("Calculation type");
        JPanel calcPanel = new JPanel(new FlowLayout());
        calcPanel.add(calculationSelector);
        calcPanel.add(calculationLabel);

        /* Weight selector */
        List<Pair<String, Integer>> weightTypes = WFlexAlgo.FlexAlgoProperties.getWeightOptions();
        JComboBox<String> weightSelector = new WiderJComboBox<>((String[]) weightTypes.stream().map(Pair::getFirst).toArray());
        JLabel weightLabel = new JLabel("Calculation type");
        JPanel weiPanel = new JPanel(new FlowLayout());
        weiPanel.add(weightSelector);
        weiPanel.add(weightLabel);


        /* Link selector */
        JList<WIpLink> linkList = new JList( callback.getDesign().getLinks().stream().map(link -> toWIpLink.apply(Pair.of(link, wNet))).toArray() );
        linkList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        linkList.setLayoutOrientation(JList.VERTICAL);
        linkList.setVisibleRowCount(-1);
        JScrollPane linkListScroller = new JScrollPane(linkList);
//        listScroller.setPreferredSize(new Dimension(250, 80));


        /* Node selector */
        JList<WNode> nodeList = new JList( callback.getDesign().getNodes().stream().map(toWNode).toArray() );
        nodeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        nodeList.setLayoutOrientation(JList.VERTICAL);
        nodeList.setVisibleRowCount(-1);
        JScrollPane nodeListScroller = new JScrollPane(nodeList);
//        nodeListScroller.setPreferredSize(new Dimension(250, 80));


        /* Scroll panels */
        final JPanel selectionPanel = new JPanel(new FlowLayout());
        selectionPanel.add(linkListScroller);
        selectionPanel.add(nodeListScroller);



        /* Create the layout to show all the elements */
        final JPanel mainPanel = new JPanel();
        mainPanel.add(idPanel);
        mainPanel.add(calcPanel);
        mainPanel.add(weiPanel);
        mainPanel.add(selectionPanel);





        /* Events */

        // Listener for identifier checker. ID must belong to [128, 255] and not be already in use
        idText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String text = idText.getText();
                try
                {
                    int id = Integer.parseInt(text);

                    // Check range
                    if(id < 128 || id > 255) throw new NumberFormatException();
                    // Check that it is not already in use
                    wNet.performOperationOnFlexAlgoRepository( repo -> {
                        if(repo.containsKey(id)) throw new NumberFormatException();
                    });

                    // If correct
                    idText.setBorder(BorderFactory.createLineBorder(Color.GREEN));
                } catch (NumberFormatException nfe)
                {
                    idText.setBorder(BorderFactory.createLineBorder(Color.RED));
                }



            }
        });



        // Launch the confirm dialog and wait to fill information. When information recovered, do all the stuff
        while (true)
        {
            int result = JOptionPane.showConfirmDialog(null, mainPanel, "Please enter information for the new flex algo", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            // Recover information from all tables TODO

            // Create the FlexAlgoProperties

            // Perform operation on the flex algo repository -> add the flex algo property



            break;
        }


    }
}
