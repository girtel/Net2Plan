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

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.DialogBuilder;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.InputForDialog;
import com.net2plan.gui.utils.WiderJComboBox;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.niw.*;
import com.net2plan.utils.Pair;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
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

    public List<Integer> getSelectedKs() { return getSelectedElements().stream().map(WFlexAlgo.FlexAlgoProperties::getK).collect(Collectors.toList()); }


    /* Properties table */
    @Override
    public List<AjtColumnInfo<WFlexAlgo.FlexAlgoProperties>> getNonBasicUserDefinedColumnsVisibleOrNot()
    {
        assert callback.getNiwInfo().getFirst();

        final NetPlan np = callback.getDesign();
        final NetworkLayer layer = this.getTableNetworkLayer();


        final List<AjtColumnInfo<WFlexAlgo.FlexAlgoProperties>> res = new LinkedList<>();
        // TODO buscar como seleccionar los enlaces incuidos en la lista. WNode -> outIpLinks

        res.add(new AjtColumnInfo<>(this, Integer.class, null, "ID (k)", "The flexible algo identifier", null, WFlexAlgo.FlexAlgoProperties::getK, AGTYPE.NOAGGREGATION, null));
        res.add(new AjtColumnInfo<>(this, String.class, null, "--", "---", null, f -> "--", AGTYPE.NOAGGREGATION, null));
        res.add(new AjtColumnInfo<>(this, String.class, null, "Calculation", "The flexible algo calculation type", null, WFlexAlgo.FlexAlgoProperties::getCalculationString, AGTYPE.NOAGGREGATION, null));
        res.add(new AjtColumnInfo<>(this, String.class, null, "Weight", "The flexible algo weight type", null, WFlexAlgo.FlexAlgoProperties::getWeightTypeString, AGTYPE.NOAGGREGATION, null));
        res.add(new AjtColumnInfo<>(this, Collection.class, null, "Links", "List of links that the flex algo can go through", null, f -> f.getLinksIncluded(np), AGTYPE.SUMCOLLECTIONCOUNT, null));


        return res;
    }


    /* Action list */
    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
        final NetPlan np = callback.getDesign();
        final NetworkLayer layer = this.getTableNetworkLayer();


        assert callback.getNiwInfo().getFirst();
        final WNet wNet = callback.getNiwInfo().getSecond();

        Consumer<Integer> setWeightToSelectedFlex = weight -> {
            WNet.performOperationOnRepository(np, repo -> {
                repo.performBatchOperation(getSelectedKs(), flex -> flex.setWeightType(weight));
            });
        };
        Consumer<Integer> setCalculationToSelectedFlex = calculation -> {
            WNet.performOperationOnRepository(np, repo -> {
                repo.performBatchOperation(getSelectedKs(), flex -> flex.setCalculationType(calculation));
            });
        };


        final List<AjtRcMenu> res = new ArrayList<>();

        res.add(new AjtRcMenu("Add FlexAlgo", e -> createEditFlexAlgoFromGUI(callback, layer, Optional.empty()), (a, b) -> true, null));
        res.add(new AjtRcMenu("Remove selected FlexAlgo", e -> getSelectedElements().forEach(flexAlgo -> WNet.performOperationOnRepository(np, repo -> repo.removeFlexAlgoPropertiesFromID(flexAlgo.getK()))), (a, b) -> true, null));
        res.add(new AjtRcMenu("Change FlexAlgo identifier (k)", e ->
        {
            DialogBuilder.launch("Change identifier of last selected FlexAlgo", "Please introduce the new id.", "", this, Arrays.asList(InputForDialog.inputTfInt("FlexAlgo id", "Introduce the new id for the FlexAlgo" , 1, 0)),  list ->
                    {
                        final int newKId = (Integer) list.get(0).get();
                        final int selectedKId = getSelectedElements().last().getK();
                        WNet.performOperationOnRepository(np, repo -> repo.changeFlexAlgoK(selectedKId, newKId));
                    });
        }, (a, b) -> true, null));
        res.add(new AjtRcMenu("Set calculation type to selected FlexAlgo", null, (a, b) -> true, Arrays.asList(
                new AjtRcMenu("As ShortestPathFirst (SPF)", e -> setCalculationToSelectedFlex.accept(WFlexAlgo.CALCULATION_SPF), (a, b) -> true, null),
                new AjtRcMenu("As StrictShortestPath (SSP)", e -> setCalculationToSelectedFlex.accept(WFlexAlgo.CALCULATION_SSP), (a, b) -> true, null))));
        res.add(new AjtRcMenu("Set weight type to selected FlexAlgo", null, (a, b) -> true, Arrays.asList(
                new AjtRcMenu("As TE", e -> setWeightToSelectedFlex.accept(WFlexAlgo.WEIGHT_TE), (a, b) -> true, null),
                new AjtRcMenu("As IGP", e -> setWeightToSelectedFlex.accept(WFlexAlgo.WEIGHT_IGP), (a, b) -> true, null),
                new AjtRcMenu("As Latency", e -> setWeightToSelectedFlex.accept(WFlexAlgo.WEIGHT_LATENCY), (a, b) -> true, null))));
        res.add(new AjtRcMenu("Set nodes and links to selected FlexAlgo", e -> createEditFlexAlgoFromGUI(callback, layer, Optional.of(getSelectedElements())), (a, b) -> true, null));
        res.add(new AjtRcMenu("Remove nodes and links to selected FlexAlgo", e -> WNet.performOperationOnRepository(np, repo ->
            repo.performBatchOperation(getSelectedKs(), WFlexAlgo.FlexAlgoProperties::removeAllLinks)), (a, b) -> true, null));



        return res;
    }


    private static void createEditFlexAlgoFromGUI(GUINetworkDesign callback, NetworkLayer layer, Optional<SortedSet<WFlexAlgo.FlexAlgoProperties>> editingFlexAlgo)
    {
        /* Check for some first steps */
        final NetPlan netPlan = callback.getDesign();
        final WNet wNet = callback.getNiwInfo().getSecond();
        if (!wNet.isSrInitialized()) wNet.initializeFlexAlgoAttributes();

        final boolean isNew = !editingFlexAlgo.isPresent();



        /* ID Selector */
        JTextField idTextField = new JTextField(6);
        JPanel textPanel = new JPanel();
        textPanel.add(idTextField);
        textPanel.add(new JLabel("Identifier (k) of FlexAlgo. [128, 255]"));

        /* Calculation selector */
        List<Pair<String, Integer>> calculationTypes = WFlexAlgo.getCalculationOptions();
        JComboBox<String> calculationSelector = new WiderJComboBox<>();
        calculationTypes.forEach(pair -> calculationSelector.addItem(pair.getFirst()));
        JPanel calculationPanel = new JPanel();
        calculationPanel.add(calculationSelector);
        calculationPanel.add(new JLabel("Calculation type (no use yet)"));

        /* Weight selector */
        List<Pair<String, Integer>> weightTypes = WFlexAlgo.getWeightOptions();
        JComboBox<String> weightSelector = new WiderJComboBox<>();
        weightTypes.forEach(pair -> weightSelector.addItem(pair.getFirst()));
        JPanel weightPanel = new JPanel();
        weightPanel.add(weightSelector);
        weightPanel.add(new JLabel("Weight type"));

        /* Link selector */
        Map<String, WIpLink> stringRepresentative = new TreeMap<>();
        Set<WIpLink> uniLinks = computeUnidiLinks(wNet);
        uniLinks.forEach(l -> {
            String representative = l.getA().getName() + " <-> " + l.getB().getName();
            stringRepresentative.put(representative, l);
        });
        JList<String> linkList = new JList<>(stringRepresentative.keySet().toArray(new String[0]));
        linkList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); linkList.setLayoutOrientation(JList.VERTICAL); linkList.setVisibleRowCount(-1);
        JScrollPane linkListScroller = new JScrollPane(linkList);
        linkListScroller.setPreferredSize(new Dimension(500, 700));



        /* Main panel construction */
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        if(isNew)
        {
            mainPanel.add(textPanel);
            mainPanel.add(calculationPanel);
            mainPanel.add(weightPanel);
        }

        mainPanel.add(new JLabel("IpLinks included in the FlexAlgo (bidirectional links)"));
        mainPanel.add(linkListScroller);





        /* Event listeners */
        /* Listener for identifier checker. ID must belong to [128, 255] and not be already in use */
        idTextField.addActionListener(e -> {
            if(!isNew) return;

            String text = idTextField.getText();
            try
            {
                int id = Integer.parseInt(text);
                WFlexAlgo.FlexAlgoRepository repo = WNet.readFlexAlgoRepositoryInNetPlan(netPlan).get();
                if(!repo.isGoodK(id)) throw new NumberFormatException();
                // If correct
                idTextField.setBorder(BorderFactory.createLineBorder(Color.GREEN));
            } catch (NumberFormatException nfe) { idTextField.setBorder(BorderFactory.createLineBorder(Color.RED)); }
        });


        /* Selection panel */
        linkList.setSelectionModel(new DefaultListSelectionModel()
        {
            boolean gestureStarted = false;
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (!gestureStarted) {
                    if (isSelectedIndex(index0)) super.removeSelectionInterval(index0, index1);
                    else super.addSelectionInterval(index0, index1);
                }
                gestureStarted = true;
            }
            @Override
            public void setValueIsAdjusting(boolean isAdjusting) {if (!isAdjusting) gestureStarted = false;}

        });






        /* Launch the confirm dialog and wait to fill information. When information recovered, do all the stuff */
        while (true)
        {
            /* Launch the dialog */
            int result = JOptionPane.showConfirmDialog(null, mainPanel, "Please enter information for the new flex algo", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;


            try
            {
                /* Recover information from all tables */
                int k;
                int calculation = 0;
                int weight = 0;

                if(isNew)
                {
                    k = Integer.parseInt(idTextField.getText());
                    WFlexAlgo.FlexAlgoRepository repo = WNet.readFlexAlgoRepositoryInNetPlan(netPlan).get();
                    if(!repo.isGoodK(k)) continue;
                    calculation = calculationTypes.stream().filter(pair -> pair.getFirst().equals(calculationSelector.getSelectedItem())).findFirst().get().getSecond();
                    weight = weightTypes.stream().filter(pair -> pair.getFirst().equals(weightSelector.getSelectedItem())).findFirst().get().getSecond();
                } else { k = 0; }



                Set<Long> selectedLinksId = new TreeSet<>();
                linkList.getSelectedValuesList().forEach(s -> {
                    WIpLink l = stringRepresentative.get(s);
                    selectedLinksId.add(l.getId());
                    selectedLinksId.add(wNet.getNodePairIpLinks( l.getB(),l.getA() ).first().getId()); // selected links are unidirectional -> get the other pair
                });


                /* Create the FlexAlgoProperties */
                if(isNew)
                {
                    WFlexAlgo.FlexAlgoProperties wFlex = new WFlexAlgo.FlexAlgoProperties(k, calculation, weight, Optional.of(selectedLinksId));
                    WNet.performOperationOnRepository(netPlan, repo -> repo.addFlexAlgo(k, Optional.of(wFlex)));
                }
                else
                {
                    List<Integer> kSet = editingFlexAlgo.get().stream().map(WFlexAlgo.FlexAlgoProperties::getK).collect(Collectors.toList());
                    WNet.performOperationOnFlexAlgo(netPlan, kSet, flexAlgo -> {
                        flexAlgo.setLinkIdsIncluded(selectedLinksId);
                    });
                }

                WNet.readFlexAlgoRepositoryInNetPlan(netPlan).ifPresent(repo -> System.out.println(Arrays.toString(repo.getAll().toArray()))); // TODO remove


            } catch (Exception e) { continue; }
            break;
        }


    }

    private static Set<WIpLink> computeUnidiLinks(WNet net)
    {
        List<WIpLink> links = net.getIpLinks();
        Set<WIpLink> linksOneDirectional = new HashSet<>();

        for(WIpLink link : links)
        {
            if(linksOneDirectional.contains(link)) continue;
            linksOneDirectional.add(net.getNodePairIpLinks(link.getB(), link.getA()).first());
        }
        return linksOneDirectional;
    }
}
