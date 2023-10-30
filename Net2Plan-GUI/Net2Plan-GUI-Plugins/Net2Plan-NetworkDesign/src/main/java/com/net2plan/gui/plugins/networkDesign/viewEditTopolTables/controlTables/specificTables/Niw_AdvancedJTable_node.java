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

import java.awt.Color;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUINetworkDesignConstants.AJTableType;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AdvancedJTable_networkElement;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtColumnInfo;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables.AjtRcMenu;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.DialogBuilder;
import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs.InputForDialog;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.niw.IOadmArchitecture;
import com.net2plan.niw.OadmArchitecture_generic;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WLightpathRequest;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNetConstants;
import com.net2plan.niw.WNode;
import org.apache.poi.hssf.eventusermodel.AbortableHSSFListener;

/**
 *
 */
@SuppressWarnings("unchecked")
public class Niw_AdvancedJTable_node extends AdvancedJTable_networkElement<Node>
{
    private static final DecimalFormat df = new DecimalFormat("#.##");
    private static Function<Double, String> df2 = d -> d <= -Double.MAX_VALUE ? "-\u221E" : d >= Double.MAX_VALUE ? "\u221E" : df.format(d);

    public Niw_AdvancedJTable_node(GUINetworkDesign callback, NetworkLayer layerThisTable)
    {
        super(callback, AJTableType.NODES, layerThisTable.getName().equals(WNetConstants.ipLayerName) ? "IP routers" : "OADMs", layerThisTable, true, n -> n.isDown() ? Color.RED : null);
    }

    @Override
    public List<AjtColumnInfo<Node>> getNonBasicUserDefinedColumnsVisibleOrNot()
    {
        final List<AjtColumnInfo<Node>> res = new LinkedList<>();

        assert callback.getNiwInfo().getFirst();
        final WNet wNet = callback.getNiwInfo().getSecond();
        final Function<Node, WNode> toWNode = n -> {
            final WNode nn = new WNode(n);
            assert !nn.isVirtualNode();
            return nn;
        };
        final Function<Node, Boolean> isOadmGeneric = n -> {
            return toWNode.apply(n).getOpticalSwitchingArchitecture() instanceof OadmArchitecture_generic;
        };
        final Function<Node, OadmArchitecture_generic> toOadmGeneric = n -> {
            return (OadmArchitecture_generic) new WNode(n).getOpticalSwitchingArchitecture();
        };
        final boolean isIpLayer = getTableNetworkLayer().getName().equals(WNetConstants.ipLayerName);
        final boolean isWdmLayer = getTableNetworkLayer().getName().equals(WNetConstants.wdmLayerName);
        assert isIpLayer || isWdmLayer;
        assert !(isIpLayer && isWdmLayer);

        final String currentLauyout = wNet.getNe().getPlotNodeLayoutCurrentlyActive();

        res.add(new AjtColumnInfo<Node>(this, Boolean.class, null, "Show/hide", "Indicates whether or not the node is visible in the topology canvas", (n, s) -> {
            if ((Boolean) s) callback.getVisualizationState().showOnCanvas(n);
            else callback.getVisualizationState().hideOnCanvas(n);
        }, n -> !callback.getVisualizationState().isHiddenOnCanvas(n), AGTYPE.COUNTTRUE, null));

        res.add(new AjtColumnInfo<Node>(this, String.class, null, "Name", "Node name", (d, val) -> toWNode.apply(d).setName((String) val), d -> toWNode.apply(d).getName(), AGTYPE.NOAGGREGATION, null));
        res.add(new AjtColumnInfo<Node>(this, Boolean.class, null, "Up?", "", (d, val) -> {
            final boolean isNodeUp = (Boolean) val;
            if (isNodeUp) new WNode(d).setAsUp();
            else new WNode(d).setAsDown();
        }, d -> new WNode(d).isUp(), AGTYPE.COUNTTRUE, n -> new WNode(n).isUp() ? null : Color.RED));
        res.add(new AjtColumnInfo<Node>(this, Double.class, null, "X-coord", "The X coordinate of the node in he current layout. Interpreted as geographical longitude in the map view", (d, val) -> d.setXYPositionMap(new Point2D.Double((Double) val, d.getXYPositionMap(currentLauyout).getY()), currentLauyout), d -> d.getXYPositionMap(currentLauyout).getX(), AGTYPE.NOAGGREGATION, null));
        res.add(new AjtColumnInfo<Node>(this, Double.class, null, "Y-coord", "The Y coordinate of the node in he current layout. Interpreted as geographical latitude in the map view", (d, val) -> d.setXYPositionMap(new Point2D.Double(d.getXYPositionMap(currentLauyout).getX(), (Double) val), currentLauyout), d -> d.getXYPositionMap(currentLauyout).getY(), AGTYPE.NOAGGREGATION, null));
        res.add(new AjtColumnInfo<Node>(this, Double.class, null, "Population", "The node population", (d, val) -> toWNode.apply(d).setPoputlation((Double) val), d -> toWNode.apply(d).getPopulation(), AGTYPE.SUMDOUBLE, null));
        res.add(new AjtColumnInfo<Node>(this, String.class, null, "Site", "The site this node belongs to", (d, val) -> d.setSiteName((String) val), d -> d.getSiteName(), AGTYPE.NOAGGREGATION, null));
        res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "SRGs", "The SRGs that this node belongs to", null, d -> toWNode.apply(d).getSrgsThisElementIsAssociatedTo().stream().map(n -> n.getNe()).collect(Collectors.toList()), AGTYPE.NOAGGREGATION, null));
        if (isIpLayer)
        {
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "VNFs", "The VNF instances in this node", null, d -> toWNode.apply(d).getVnfInstances().stream().map(v -> v.getNe()).collect(Collectors.toList()), AGTYPE.SUMCOLLECTIONCOUNT, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "Out IP links", "The outgoing IP links of the node", null, d -> toWNode.apply(d).getOutgoingIpLinks().stream().map(n -> n.getNe()).collect(Collectors.toList()), AGTYPE.SUMCOLLECTIONCOUNT, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "In IP links", "The incoming IP links of the node", null, d -> toWNode.apply(d).getIncomingIpLinks().stream().map(n -> n.getNe()).collect(Collectors.toList()), AGTYPE.SUMCOLLECTIONCOUNT, null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, null, "Out capacity (Gbps)", "The sum of the current capacity of the outgoing IP links", null, d -> toWNode.apply(d).getOutgoingIpLinks().stream().mapToDouble(n -> n.getCurrentCapacityGbps()).sum(), AGTYPE.SUMDOUBLE, null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, null, "In capacity (Gbps)", "The sum of the current capacity of the incoming IP links", null, d -> toWNode.apply(d).getIncomingIpLinks().stream().mapToDouble(n -> n.getCurrentCapacityGbps()).sum(), AGTYPE.SUMDOUBLE, null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, null, "Out IP traffic (Gbps)", "The sum of the traffic carried in the outgoing IP links", null, d -> toWNode.apply(d).getOutgoingIpLinks().stream().mapToDouble(n -> n.getCarriedTrafficGbps()).sum(), AGTYPE.SUMDOUBLE, null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, null, "In IP traffic (Gbps)", "The sum of the traffic carried in the incoming IP links", null, d -> toWNode.apply(d).getIncomingIpLinks().stream().mapToDouble(n -> n.getCarriedTrafficGbps()).sum(), AGTYPE.SUMDOUBLE, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "Out IP demands", "The unicast IP demands that have this node as origin", null, d -> toWNode.apply(d).getOutgoingIpUnicastDemands().stream().map(n -> n.getNe()).collect(Collectors.toList()), AGTYPE.SUMCOLLECTIONCOUNT, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "In IP demands", "The unicast IP demands that have this node as origin", null, d -> toWNode.apply(d).getIncomingIpUnicastDemands().stream().map(n -> n.getNe()).collect(Collectors.toList()), AGTYPE.SUMCOLLECTIONCOUNT, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "Out SCReqs", "The service chain requests that have this node as potential origin", null, d -> toWNode.apply(d).getOutgoingServiceChainRequests().stream().map(n -> n.getNe()).collect(Collectors.toList()), AGTYPE.SUMCOLLECTIONCOUNT, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "In SCReqs", "The service chain requests that have this node as potential destination", null, d -> toWNode.apply(d).getIncomingServiceChainRequests().stream().map(n -> n.getNe()).collect(Collectors.toList()), AGTYPE.SUMCOLLECTIONCOUNT, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "Out SCs", "The service chains initiated in this node", null, d -> toWNode.apply(d).getOutgoingServiceChains().stream().map(n -> n.getNe()).collect(Collectors.toList()), AGTYPE.SUMCOLLECTIONCOUNT, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "In SCs", "The service chains ending in this node", null, d -> toWNode.apply(d).getIncomingServiceChains().stream().map(n -> n.getNe()).collect(Collectors.toList()), AGTYPE.SUMCOLLECTIONCOUNT, null));

            /* Pending information of HD/RAM etc capacicites */
            res.add(new AjtColumnInfo<Node>(this, Double.class, Arrays.asList("CPU/RAM/HD"), "Node CPUs", "The CPUs available in the node", (d, val) -> toWNode.apply(d).setTotalNumCpus((Double) val), d -> toWNode.apply(d).getTotalNumCpus(), AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, Arrays.asList("CPU/RAM/HD"), "Node RAM (GB)", "The amount of RAM in the node,in GB", (d, val) -> toWNode.apply(d).setTotalRamGB((Double) val), d -> toWNode.apply(d).getTotalRamGB(), AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, Arrays.asList("CPU/RAM/HD"), "Node HD (TB)", "The amount of hard-disk storage in TeraBytes", (d, val) -> toWNode.apply(d).setTotalHdGB(0.001 * (Double) val), d -> toWNode.apply(d).getTotalHdGB() * 1000.0, AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, Arrays.asList("CPU/RAM/HD"), "Occupied CPUs", "The CPUs occupied in the node", null, d -> toWNode.apply(d).getOccupiedCpus(), AGTYPE.NOAGGREGATION, e -> toWNode.apply(e).getOccupiedCpus() > toWNode.apply(e).getTotalNumCpus() ? Color.red : null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, Arrays.asList("CPU/RAM/HD"), "Occupied RAM (GB)", "The amount of RAM occupied in the node, in GB", null, d -> toWNode.apply(d).getOccupiedRamGB(), AGTYPE.NOAGGREGATION, e -> toWNode.apply(e).getOccupiedRamGB() > toWNode.apply(e).getTotalRamGB() ? Color.red : null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, Arrays.asList("CPU/RAM/HD"), "Occupied HD (TB)", "The amount of storage occupied in the node, in TeraBytes", null, d -> toWNode.apply(d).getOccupiedHdGB() * 1000, AGTYPE.NOAGGREGATION, e -> toWNode.apply(e).getOccupiedHdGB() > toWNode.apply(e).getTotalHdGB() ? Color.red : null));

        } else if (isWdmLayer)
        {
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "OADM type", "The switching type of the architecture. At this moment e.g. filterless (drop-and-waste), or non-blocking OADM", null, d -> toWNode.apply(d).getOpticalSwitchingArchitecture().getShortName(), AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Node>(this, Integer.class, null, "# Dirless Add/Drop", "Number of directionless add/drop modules. These modules are implemented as regular degrees by the switching architecture.", (d, val) -> toWNode.apply(d).setOadmNumAddDropDirectionlessModules((Integer) val), d -> toWNode.apply(d).getOadmNumAddDropDirectionlessModules(), AGTYPE.SUMINT, null));
            res.add(new AjtColumnInfo<Node>(this, Boolean.class, null, "Has dirful. AD modules?", "Indicates if this node, aside of the potential directionless add/drop modules, also has directed modules directly attached to the in/out degrees.", (d, val) -> toWNode.apply(d).setIsOadmWithDirectedAddDropModulesInTheDegrees((Boolean) val), d -> toWNode.apply(d).isOadmWithDirectedAddDropModulesInTheDegrees(), AGTYPE.COUNTTRUE, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "Out LP requests", "The outgoing lightpath requests of the node", null, d -> toWNode.apply(d).getOutgoingLigtpathRequests().stream().map(n -> n.getNe()).collect(Collectors.toList()), AGTYPE.SUMCOLLECTIONCOUNT, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "In LP requests", "The incoming lightpath requests of the node", null, d -> toWNode.apply(d).getIncomingLigtpathRequests().stream().map(n -> n.getNe()).collect(Collectors.toList()), AGTYPE.SUMCOLLECTIONCOUNT, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "ADD LPs", "The outgoing lightpaths of the node", null, d -> toWNode.apply(d).getAddedLigtpaths().stream().map(n -> n.getNe()).collect(Collectors.toList()), AGTYPE.SUMCOLLECTIONCOUNT, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "DROP LPs", "The incoming lightpaths of the node", null, d -> toWNode.apply(d).getDroppedLigtpaths().stream().map(n -> n.getNe()).collect(Collectors.toList()), AGTYPE.SUMCOLLECTIONCOUNT, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "EXPRESS LPs", "The lightpaths that are express to this node", null, d -> toWNode.apply(d).getExpressSwitchedLightpaths().stream().map(n -> n.getNe()).collect(Collectors.toList()), AGTYPE.SUMCOLLECTIONCOUNT, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "ADD optical rate (Gbps)", "The sum of the line rates of the lightpaths added in this node", null, d -> toWNode.apply(d).getAddedLigtpaths().stream().mapToDouble(n -> n.getLightpathRequest().getLineRateGbps()).sum(), AGTYPE.SUMDOUBLE, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "DROP optical rate (Gbps)", "The sum of the line rates of the lightpaths dropped in this node", null, d -> toWNode.apply(d).getDroppedLigtpaths().stream().mapToDouble(n -> n.getLightpathRequest().getLineRateGbps()).sum(), AGTYPE.SUMDOUBLE, null));
            res.add(new AjtColumnInfo<Node>(this, Collection.class, null, "EXPRESS optical rate (Gbps)", "The sum of the line rates of the lightpaths express in this node", null, d -> toWNode.apply(d).getExpressSwitchedLightpaths().stream().mapToDouble(n -> n.getLightpathRequest().getLineRateGbps()).sum(), AGTYPE.SUMDOUBLE, null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, Arrays.asList("Generic OADM model params"), "Architecture type", "The type of optical switching architecture", null, d -> toOadmGeneric.apply(d).getParameters().getArchitectureType(), AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, Arrays.asList("Generic OADM model params"), "Add/drop module type", "The type of add/drop module: based on mux/demux or on WSS", null, d -> toOadmGeneric.apply(d).getParameters().getAddDropModuleType(), AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, Arrays.asList("Generic OADM model params"), "Mux/demux attenuation (dB)", "The attenuation to apply by the multiplexer/demultiplexer of the add/drop module, if of this type", null, d -> toOadmGeneric.apply(d).getParameters().getMuxDemuxLoss_dB(), AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, Arrays.asList("Generic OADM model params"), "Mux/demux PMD (ps)", "The Polarization Mode Dierpsion (PMD) to apply, caused by the multiplexer/demultiplexer of the add/drop module, if of this type", null, d -> toOadmGeneric.apply(d).getParameters().getMuxDemuxPmd_ps(), AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, Arrays.asList("Generic OADM model params"), "WSS attenuation (dB)", "The attenuation to apply by the WSS of the add/drop module, if of this type", null, d -> toOadmGeneric.apply(d).getParameters().getWssLoss_dB(), AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, Arrays.asList("Generic OADM model params"), "WSS PMD (ps)", "The Polarization Mode Dierpsion (PMD) to apply, caused by the WSS of the add/drop module, if of this type", null, d -> toOadmGeneric.apply(d).getParameters().getWssPmd_ps(), AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, Arrays.asList("Generic OADM model params"), "[Optional] Degree splitter/coupler loss (dB)", "Loss of each splitter/combiner used in the in/out degrees. If not present, or a negative value, the loss is assumed to be the one of an ideal 1xN or Nx1 splitter/combiner (10 Log N)", null, d -> {
                final double val = toOadmGeneric.apply(d).getParameters().getManuallySettledDegreeSplitterCombinerLoss_dB().orElse(-1.0);
                return val < 0 ? "--" : val;
            }, AGTYPE.NOAGGREGATION, null));
            res.add(new AjtColumnInfo<Node>(this, Double.class, Arrays.asList("Generic OADM model params"), "[Optional] Directionless Add/drop module splitter/coupler loss (dB)", "Loss of each splitter/combiner used in the add/drop directionless modules, if any. If not present, or a negative value, the loss is assumed to be the one of an ideal 1xN or Nx1 splitter/combiner (10 Log N)", null, d -> {
                final double val = toOadmGeneric.apply(d).getParameters().getDirlessAddDropSplitterCombinerLoss_dB().orElse(-1.0);
                return val < 0 ? "--" : val;
            }, AGTYPE.NOAGGREGATION, null));
        }
        return res;
    }


    public List<AjtRcMenu> getNonBasicRightClickMenusInfo()
    {
        final List<AjtRcMenu> res = new ArrayList<>();
        assert callback.getNiwInfo().getFirst();
        final WNet wNet = callback.getNiwInfo().getSecond();
        final boolean isIpLayer = getTableNetworkLayer().getName().equals(WNetConstants.ipLayerName);
        final boolean isWdmLayer = getTableNetworkLayer().getName().equals(WNetConstants.wdmLayerName);
        assert isIpLayer || isWdmLayer;
        assert !(isIpLayer && isWdmLayer);
        final Function<Node, WNode> toWNode = d -> (WNode) wNet.getWElement(d).get();
        res.add(new AjtRcMenu("Add node", e -> wNet.addNode(0, 0, wNet.getUnusedValidNodeName(), null), (a, b) -> true, null));
        res.add(new AjtRcMenu("Remove selected nodes", e -> getSelectedElements().forEach(dd -> toWNode.apply(dd).remove()), (a, b) -> b > 0, null));
        res.add(new AjtRcMenu("Show selected nodes", e -> getSelectedElements().forEach(ee -> callback.getVisualizationState().showOnCanvas(ee)), (a, b) -> b > 0, null));
        res.add(new AjtRcMenu("Hide selected nodes", e -> getSelectedElements().forEach(ee -> callback.getVisualizationState().hideOnCanvas(ee)), (a, b) -> b > 0, null));
        res.add(new AjtRcMenu("Switch selected nodes' coordinates from (x,y) to (y,x)", e -> getSelectedElements().forEach(node -> node.setXYPositionMap(new Point2D.Double(node.getXYPositionMap().getY(), node.getXYPositionMap().getX()))), (a, b) -> b > 0, null));
        res.add(new AjtRcMenu("Re-arrange selected nodes", null, (a, b) -> b > 0, Arrays.asList(new AjtRcMenu("Equispaced in a circunference", e -> {
            DialogBuilder.launch("Indicate the coordinates of the circle center, and the radius", "Please introduce the requested data.", "", this, Arrays.asList(InputForDialog.inputTfDouble("X position of the center", "Introduce the X position of the circle center", 10, 0.0), InputForDialog.inputTfDouble("Y position of the center", "Introduce the Y position of the circle center", 10, 0.0), InputForDialog.inputTfDouble("Radius", "Introduce the radius", 10, 100.0)), (list) -> {
                final double x = ((Double) list.get(0).get());
                final double y = ((Double) list.get(1).get());
                final double radius = ((Double) list.get(2).get());
                if (radius <= 0) throw new Net2PlanException("The circle radius must e strictly positive");
                if (getSelectedElements().isEmpty()) throw new Net2PlanException("No nodes are selected");
                int contNode = 0;
                final double radQuantum = 2 * Math.PI / getSelectedElements().size();
                for (Node np_ee : getSelectedElements())
                {
                    final WNode ee = toWNode.apply(np_ee);
                    final double newX = x + radius * Math.cos(contNode * radQuantum);
                    final double newY = y + radius * Math.sin(contNode * radQuantum);
                    contNode++;
                    ee.setNodePositionXY(new Point2D.Double(newX, newY));
                }
            });
        }, (a, b) -> true, null))));



        if (isWdmLayer)
        {
            final List<AjtRcMenu> menusForEachOpticalSwitchType = new ArrayList<>();
            for (IOadmArchitecture arc : IOadmArchitecture.availableRepresentatives)
                menusForEachOpticalSwitchType.add(new AjtRcMenu(arc.getShortName(), e -> getSelectedElements().forEach(ee -> toWNode.apply(ee).setOpticalSwitchArchitecture(arc.getClass())), (a, b) -> b > 0, null));

            res.add(new AjtRcMenu("Add full-mesh of bidirectional lightpath requests among selected nodes", e -> {
                DialogBuilder.launch("Add full-mesh of lightpath requests", "Please introduce the required data", "", this, Arrays.asList(InputForDialog.inputTfDouble("Line rate (Gbps)", "Introduce the line rate of the lightpaths realizing this request", 20, 100.0), InputForDialog.inputCheckBox("1+1 protected?", "Indicate if this lighptath request has to be realized by a 1+1 arrangement of two lightpaths", false, null), InputForDialog.inputTfInt("Number of lightpath requests for each node pair", "Number of lightpath requests to create", 10, 1)), (list) -> {
                    final Double lineRateGbps = (Double) list.get(0).get();
                    final Boolean isToBe11Protected = (Boolean) list.get(1).get();
                    final Integer numRequests = (Integer) list.get(2).get();
                    for (WNode a : getSelectedElements().stream().map(n -> toWNode.apply(n)).collect(Collectors.toList()))
                    {
                        for (WNode b : getSelectedElements().stream().map(n -> toWNode.apply(n)).collect(Collectors.toList()))
                        {
                            if (a.getId() <= b.getId()) continue;
                            for (int cont = 0; cont < numRequests; cont++)
                            {
                                final WLightpathRequest lprAb = wNet.addLightpathRequest(a, b, lineRateGbps, isToBe11Protected);
                                final WLightpathRequest lprBa = wNet.addLightpathRequest(b, a, lineRateGbps, isToBe11Protected);
                                lprAb.setBidirectionalPair(lprBa);
                            }
                        }
                    }
                });
            }, (a, b) -> true, null));


            res.add(new AjtRcMenu("Set optical switching type of selected OADMs", null, (a, b) -> b > 0, menusForEachOpticalSwitchType));
            res.add(new AjtRcMenu("Set number of add/drop directionless modules to selected nodes", e -> {
                DialogBuilder.launch("Set number of add/drop directionless modules to selected nodes", "Please introduce the requested information.", "", this, Arrays.asList(InputForDialog.inputTfInt("Number of directionless Add/Drop modules in each node", "The number of directionless add/drop modules in each OADM", 10, 2)), (list) -> {
                    Integer numADs = (Integer) list.get(0).get();
                    if (numADs < 0) numADs = 0;
                    for (WNode node : getSelectedElements().stream().map(ee -> toWNode.apply(ee)).collect(Collectors.toList()))
                        node.setOadmNumAddDropDirectionlessModules(numADs);
                });
            }, (a, b) -> b > 0, null));
            res.add(new AjtRcMenu("Set existence of directionful ADD/DROP modules in the node", null, (a, b) -> b > 0, Arrays.asList(new AjtRcMenu("OADMs with directionful modules", ee -> getSelectedElements().stream().forEach(n -> toWNode.apply(n).setIsOadmWithDirectedAddDropModulesInTheDegrees(true)), (a, b) -> b > 0, null), new AjtRcMenu("OADMs without directionful modules", ee -> getSelectedElements().stream().forEach(n -> toWNode.apply(n).setIsOadmWithDirectedAddDropModulesInTheDegrees(false)), (a, b) -> b > 0, null))));


            res.add(new AjtRcMenu("Set parameters of selected OADMs", e -> {
                DialogBuilder.launch("Set parameters of selected OADMs", "Please introduce the requested information.", "", this, Arrays.asList(InputForDialog.inputTfCombo("Architecture type", "The type of optical switching architecture", 10, "Broadcast-and-select", Arrays.asList("Broadcast-and-select", "Route-and-select", "Filterless"), null, null), InputForDialog.inputTfCombo("Add/drop module type", "The type of add/drop module: based on mux/demux or on WSS", 10, "Mux/Demux-based", Arrays.asList("Mux/Demux-based", "Wss-based"), null, null), InputForDialog.inputTfDouble("Mux/demux attenuation (dB)", "The attenuation to apply by the multiplexer/demultiplexer of the add/drop module, if of this type", 10, 6.0), InputForDialog.inputTfDouble("WSS attenuation (dB)", "The attenuation to apply by the WSS of the add/drop module, if of this type", 10, 6.0), InputForDialog.inputTfDouble("Mux/demux PMD (ps)", "The Polarization Mode Dierpsion (PMD) to apply, caused by the multiplexer/demultiplexer of the add/drop module, if of this type", 10, 0.5), InputForDialog.inputTfDouble("WSS PMD (ps)", "The Polarization Mode Dierpsion (PMD) to apply, caused by the WSS of the add/drop module, if of this type", 10, 0.5), InputForDialog.inputTfDouble("[Optional] Degree splitter/coupler loss (dB)", "Loss of each splitter/combiner used in the in/out degrees. If not present, or a negative value, the loss is assumed to be the one of an ideal 1xN or Nx1 splitter/combiner (10 Log N)", 10, -1.0), InputForDialog.inputTfDouble("[Optional] Directionless Add/drop module splitter/coupler loss (dB)", "Loss of each splitter/combiner used in the add/drop directionless modules, if any. If not present, or a negative value, the loss is assumed to be the one of an ideal 1xN or Nx1 splitter/combiner (10 Log N)", 10, -1.0)), (list) -> {
                    final int indexArqType = Arrays.asList("Broadcast-and-select", "Route-and-select", "Filterless").indexOf((String) list.get(0).get());
                    final int indexAdType = Arrays.asList("Mux/Demux-based", "Wss-based").indexOf((String) list.get(1).get());
                    if (indexAdType < 0 || indexAdType < 0) throw new RuntimeException();
                    final String arqType = Arrays.asList("B&S", "R&S", "Filterless").get(indexArqType);
                    final String adType = Arrays.asList("Mux/Demux-based", "Wss-based").get(indexAdType);
                    final double muxDemuxAttDb = (Double) list.get(2).get();
                    final double wssAttDb = (Double) list.get(3).get();
                    final double muxDemuxPmdPs = (Double) list.get(4).get();
                    final double wssPmdPs = (Double) list.get(5).get();
                    final double degreeSplitterCombinerLoss_dB = (Double) list.get(6).get();
                    final double dirlessAdSplitterCombinerLoss_dB = (Double) list.get(7).get();
                    for (WNode node : getSelectedElements().stream().map(ee -> toWNode.apply(ee)).collect(Collectors.toList()))
                    {
                        if (!(node.getOpticalSwitchingArchitecture() instanceof OadmArchitecture_generic)) continue;
                        final OadmArchitecture_generic oadm = (OadmArchitecture_generic) node.getOpticalSwitchingArchitecture();
                        final OadmArchitecture_generic.Parameters param = oadm.getParameters();
                        if (indexArqType == 0) param.setArchitectureTypeAsBroadcastAndSelect();
                        else if (indexArqType == 1) param.setArchitectureTypeAsRouteAndSelect();
                        else if (indexArqType == 2) param.setArchitectureTypeAsFilterless();
                        if (indexAdType == 0) param.setAddDropModuleTypeAsMuxBased();
                        else if (indexAdType == 1) param.setAddDropModuleTypeAsWssBased();
                        param.setMuxDemuxLoss_dB(muxDemuxAttDb);
                        param.setMuxDemuxPmd_ps(muxDemuxPmdPs);
                        param.setWssLoss_dB(wssAttDb);
                        param.setWssPmd_ps(wssPmdPs);
                        param.setDegreeSplitterCombinerLoss_dB(degreeSplitterCombinerLoss_dB < 0 ? Optional.empty() : Optional.of(degreeSplitterCombinerLoss_dB));
                        param.setDirlessAddDropSplitterCombinerLoss_dB(dirlessAdSplitterCombinerLoss_dB < 0 ? Optional.empty() : Optional.of(dirlessAdSplitterCombinerLoss_dB));
                        oadm.updateParameters(param);
                    }
                });
            }, (a, b) -> b > 0, null));
        }

        return res;
    }
}
