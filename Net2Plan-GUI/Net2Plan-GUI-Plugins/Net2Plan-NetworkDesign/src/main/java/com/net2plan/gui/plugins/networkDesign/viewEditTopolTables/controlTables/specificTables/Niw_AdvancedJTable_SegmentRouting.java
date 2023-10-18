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
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
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
        res.add(new AjtColumnInfo<>(this, Collection.class, null, "Nodes", "List of nodes that are using this flex algo", null, f -> f.getNodesIncluded(np), AGTYPE.SUMCOLLECTIONCOUNT, null));
        res.add(new AjtColumnInfo<>(this, String.class, null, "SID's", "List of SID's assigned to this node", null, f -> f.getAssociatedSidsAsNiceLookingString(), AGTYPE.NOAGGREGATION, null));


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
                new AjtRcMenu("As TE", e -> setWeightToSelectedFlex.accept(WFlexAlgo.WEIGHT_SSP), (a, b) -> true, null),
                new AjtRcMenu("As IGP", e -> setWeightToSelectedFlex.accept(WFlexAlgo.WEIGHT_IGP), (a, b) -> true, null),
                new AjtRcMenu("As Latency", e -> setWeightToSelectedFlex.accept(WFlexAlgo.WEIGHT_LATENCY), (a, b) -> true, null))));
        res.add(new AjtRcMenu("Set nodes and links to selected FlexAlgo", e -> createEditFlexAlgoFromGUI(callback, layer, Optional.of(getSelectedElements())), (a, b) -> true, null));
        res.add(new AjtRcMenu("Remove nodes and links to selected FlexAlgo", e -> WNet.performOperationOnRepository(np, repo ->
            repo.performBatchOperation(getSelectedKs(), WFlexAlgo.FlexAlgoProperties::removeNodesAndLinks)), (a, b) -> true, null));



        return res;
    }


    private static void createEditFlexAlgoFromGUI(GUINetworkDesign callback, NetworkLayer layer, Optional<SortedSet<WFlexAlgo.FlexAlgoProperties>> editingFlexAlgo)
    {
        /* Check for some first steps */
        final NetPlan netPlan = callback.getDesign();
        final WNet wNet = callback.getNiwInfo().getSecond();
        if (!wNet.isSrInitialized()) wNet.initializeFlexAlgoAttributes();

        final boolean isNew = !editingFlexAlgo.isPresent();
        ArrayList<Pair<JComponent, JComponent>> panelComponentsPairs = new ArrayList<>();




        /* ID Selector */
        JTextField idTextField = new JTextField(6);

        /* Calculation selector */
        List<Pair<String, Integer>> calculationTypes = WFlexAlgo.getCalculationOptions();
        JComboBox<String> calculationSelector = new WiderJComboBox<>();
        calculationTypes.forEach(pair -> calculationSelector.addItem(pair.getFirst()));

        /* Weight selector */
        List<Pair<String, Integer>> weightTypes = WFlexAlgo.getWeightOptions();
        JComboBox<String> weightSelector = new WiderJComboBox<>();
        weightTypes.forEach(pair -> weightSelector.addItem(pair.getFirst()));

        /* Link selector */
        DefaultListModel<WIpLink> linkListModel = new DefaultListModel<>();
        JList<WIpLink> linkList = new JList<>(linkListModel);
        linkList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); linkList.setLayoutOrientation(JList.VERTICAL); linkList.setVisibleRowCount(-1);
        JScrollPane linkListScroller = new JScrollPane(linkList);
        linkListScroller.setPreferredSize(new Dimension(500, 700));

        /* Node selector */
        Map<String, Pair<Long, String>> candidateNodeMap = new TreeMap<>();
        wNet.getNodes().forEach(node -> { node.getSidList().ifPresent(sidList -> sidList.forEach(sid -> candidateNodeMap.put(node.getName() + " (" + sid.trim() + ")", Pair.of(node.getId(), sid)))); });
        JList<String> nodeList = new JList(candidateNodeMap.keySet().toArray());
        nodeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); nodeList.setLayoutOrientation(JList.VERTICAL); nodeList.setVisibleRowCount(-1);
        JScrollPane nodeListScroller = new JScrollPane(nodeList);
        nodeListScroller.setPreferredSize(new Dimension(500, 700));


        /* Set the order to show the components */
        if(isNew) panelComponentsPairs.add(Pair.of(new JLabel("Identifier (k) of FlexAlgo. [128, 255]"), idTextField));
        if(isNew) panelComponentsPairs.add(Pair.of(new JLabel("Calculation type"), calculationSelector));
        if(isNew) panelComponentsPairs.add(Pair.of(new JLabel("Calculation type"), weightSelector));
        panelComponentsPairs.add(Pair.of( new JLabel("Nodes - (SID) -> (optional)"),new JLabel("Links that will be added to the FlexAlgo.")));
        panelComponentsPairs.add(Pair.of( nodeListScroller, linkListScroller));
        panelComponentsPairs.add(Pair.of( new JLabel(""), new JLabel("Links are added if two selected nodes have link between them")));


        /* Main panel construction */
        JPanel mainPanel = new JPanel(new MigLayout("fill", "[][grow]", "[][][][][]"));
        panelComponentsPairs.forEach(pair -> { mainPanel.add(pair.getFirst()); mainPanel.add(pair.getSecond(), "wrap"); });





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
//                WNet.performOperationOnRepository(netPlan, repo -> { if(!repo.isGoodK(id)) throw new NumberFormatException(); });

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
        linkList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.getA().getName() + " -> " + value.getB().getName());
            label.setOpaque(true);

            if (isSelected) {
                label.setBackground(Color.BLUE);
                label.setForeground(Color.WHITE);
            }
            else {
                label.setBackground(Color.WHITE);
                label.setForeground(Color.BLACK);
            }
            return label;
        });
        nodeList.setSelectionModel(new DefaultListSelectionModel()
        {
            boolean gestureStarted = false;

            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (!gestureStarted) {
                    if (isSelectedIndex(index0)) super.removeSelectionInterval(index0, index1);
                    else super.addSelectionInterval(index0, index1);
                }
                gestureStarted = true;
                recomputeLinkList();
            }
            @Override
            public void setValueIsAdjusting(boolean isAdjusting) {if (!isAdjusting) gestureStarted = false;}
            private void recomputeLinkList()
            {
                List<String> selectedNodesRepresentative = nodeList.getSelectedValuesList();
                List<Pair<Long, String>> selectedNodesInformation = selectedNodesRepresentative.stream().map(candidateNodeMap::get).collect(Collectors.toList());
                Set<Long> selectedNodesId = selectedNodesInformation.stream().map(Pair::getFirst).collect(Collectors.toSet());

                Set<WIpLink> links = recomputeVirtualLinkList(wNet, selectedNodesId);

                if (links.isEmpty()) return;
                linkListModel.clear();

                final Set<Long> containedLinks = new HashSet<>();
                final int N = netPlan.getNumberOfNodes();
                for(WIpLink l: links)
                {
                    WNode n1 = l.getA(), n2 = l.getB();
                    final long rAB = n1.getId() + N * n2.getId();
                    final long rBA = n2.getId() + N * n1.getId();

                    if(containedLinks.contains(rAB) || containedLinks.contains(rBA)) continue;

                    containedLinks.add(rAB);
                    linkListModel.addElement(l);
                }

            }
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


                List<String> nodeRepresentatives = nodeList.getSelectedValuesList();
                List<Pair<Long, String>> selectedNodesInformation = nodeRepresentatives.stream().map(candidateNodeMap::get).collect(Collectors.toList());
                Set<Long> selectedNodesId = selectedNodesInformation.stream().map(Pair::getFirst).collect(Collectors.toSet());
                Set<String> selectedNodesSid = selectedNodesInformation.stream().map(Pair::getSecond).collect(Collectors.toSet());


                Set<Long> selectedLinksId = new TreeSet<>();
                linkList.getSelectedValuesList().forEach(l -> {
                    selectedLinksId.add(l.getId());
                    selectedLinksId.add(wNet.getNodePairIpLinks(l.getB(), l.getA()).first().getId()); // selected links are unidirectional -> get the other pair
                });


                /* Create the FlexAlgoProperties */
                if(isNew)
                {
                    WFlexAlgo.FlexAlgoProperties wFlex = new WFlexAlgo.FlexAlgoProperties(k, calculation, weight, Optional.of(selectedLinksId), Optional.of(selectedNodesId), Optional.of(selectedNodesSid));
                    WNet.performOperationOnRepository(netPlan, repo -> repo.addFlexAlgo(k, Optional.of(wFlex)));
                }
                else
                {
                    List<Integer> kSet = editingFlexAlgo.get().stream().map(WFlexAlgo.FlexAlgoProperties::getK).collect(Collectors.toList());
                    WNet.performOperationOnFlexAlgo(netPlan, kSet, flexAlgo -> {
                        flexAlgo.setAssociatedSids(selectedNodesSid);
                        flexAlgo.setNodeIdsIncluded(selectedNodesId);
                        flexAlgo.setLinkIdsIncluded(selectedLinksId);
                    });
                }

                WNet.readFlexAlgoRepositoryInNetPlan(netPlan).ifPresent(repo -> System.out.println(Arrays.toString(repo.getAll().toArray())));


            } catch (Exception e) { continue; }


            break;
        }


    }


    private static Set<WIpLink> recomputeVirtualLinkList(WNet wNet, Set<Long> selectedNodes)
    {
        if (selectedNodes.size() <= 1) return new TreeSet<>();

        Set<WIpLink> links = new HashSet<>();
        for (long n1 : selectedNodes)
            for (long n2 : selectedNodes)
            {
                if (n1 <= n2) continue;

                final Optional<WNode> n1p = wNet.getNodeFromId(n1), n2p = wNet.getNodeFromId(n2);
                assert n1p.isPresent();
                assert n2p.isPresent();

                final Set<WIpLink> linkAB = wNet.getNodePairIpLinks(n1p.get(), n2p.get());
                final Set<WIpLink> linkBA = wNet.getNodePairIpLinks(n2p.get(), n1p.get());
                if (linkBA.isEmpty() || linkAB.size() != linkBA.size()) continue;

                links.addAll(linkAB);
                links.addAll(linkBA);
            }

        return links;
    }
}
