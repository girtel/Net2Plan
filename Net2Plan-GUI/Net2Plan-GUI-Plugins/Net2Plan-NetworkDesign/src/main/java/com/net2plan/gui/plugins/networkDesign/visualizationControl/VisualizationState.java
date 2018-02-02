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
package com.net2plan.gui.plugins.networkDesign.visualizationControl;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter.FilterCombinationType;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUILink;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.ImageUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;
import edu.uci.ics.jung.visualization.FourPassImageShaper;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.MapUtils;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;
import java.util.List;

import static com.net2plan.internal.Constants.NetworkElementType;

public class VisualizationState
{
    private static Map<Triple<URL, Integer, Color>, Pair<ImageIcon, Shape>> databaseOfAlreadyReadIcons = new HashMap<>(); // for each url, height, and border color, an image


    private boolean showInCanvasNodeNames;
    private boolean showInCanvasLinkLabels;
    private boolean showInCanvasLinksInNonActiveLayer;
    private boolean showInCanvasInterLayerLinks;
    private boolean showInCanvasLowerLayerPropagation;
    private boolean showInCanvasUpperLayerPropagation;
    private boolean showInCanvasThisLayerPropagation;
    private boolean whatIfAnalysisActive;
    private ITableRowFilter tableRowFilter;

    private List<Double> linkUtilizationColorThresholdList;
    private List<Double> linkRunoutTimeColorThresholdList;
    private List<Double> linkCapacityThicknessThresholdList;
    private boolean isActiveLinkUtilizationColorThresholdList;
    private boolean isActiveLinkRunoutTimeColorThresholdList;
    private boolean isActiveLinkCapacityThicknessThresholdList;

    private float linkWidthIncreaseFactorRespectToDefault;
    private float nodeSizeFactorRespectToDefault;

    private boolean showInCanvasNonConnectedNodes;
    private int interLayerSpaceInPixels;
    private Set<Node> nodesToHideInCanvasAsMandatedByUserInTable;
    private Set<Link> linksToHideInCanvasAsMandatedByUserInTable;

    private final PickManager pickManager;
    private VisualizationSnapshot visualizationSnapshot;

    /* These need is recomputed inside a rebuild */
    private Map<Node, Set<GUILink>> cache_canvasIntraNodeGUILinks;
    private Map<Link, GUILink> cache_canvasRegularLinkMap;
    private BidiMap<NetworkLayer, Integer> cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible; // as many elements as visible layers
    private Map<Node, Map<Pair<Integer, Integer>, GUILink>> cache_mapNode2IntraNodeCanvasGUILinkMap; // integers are orders of REAL VISIBLE LAYERS
    private Map<Node, List<GUINode>> cache_mapNode2ListVerticallyStackedGUINodes;

    public NetPlan getNetPlan()
    {
        return visualizationSnapshot.getNetPlan();
    }

    public VisualizationState(NetPlan currentNp, BidiMap<NetworkLayer, Integer> mapLayer2VisualizationOrder, Map<NetworkLayer, Boolean> layerVisibilityMap, int maxSizePickUndoList)
    {
        this.visualizationSnapshot = new VisualizationSnapshot(currentNp);
        this.showInCanvasNodeNames = false;
        this.showInCanvasLinkLabels = false;
        this.showInCanvasLinksInNonActiveLayer = true;
        this.showInCanvasInterLayerLinks = true;
        this.showInCanvasNonConnectedNodes = true;
        this.showInCanvasLowerLayerPropagation = true;
        this.showInCanvasUpperLayerPropagation = true;
        this.showInCanvasThisLayerPropagation = true;
        this.whatIfAnalysisActive = false;
        this.nodesToHideInCanvasAsMandatedByUserInTable = new HashSet<>();
        this.linksToHideInCanvasAsMandatedByUserInTable = new HashSet<>();
        this.interLayerSpaceInPixels = 50;
        this.tableRowFilter = null;
        this.linkUtilizationColorThresholdList = new ArrayList<> (VisualizationConstants.DEFAULT_LINKCOLORINGUTILIZATIONTHRESHOLDS);
        this.linkCapacityThicknessThresholdList = new ArrayList<> (VisualizationConstants.DEFAULT_LINKTHICKNESSTHRESHPOLDS);
        this.linkRunoutTimeColorThresholdList = new ArrayList<> (VisualizationConstants.DEFAULT_LINKCOLORINGRUNOUTTHRESHOLDS);
        this.isActiveLinkUtilizationColorThresholdList = true;
        this.isActiveLinkRunoutTimeColorThresholdList = false;
        this.isActiveLinkCapacityThicknessThresholdList = true;

        this.linkWidthIncreaseFactorRespectToDefault = 1;
        this.nodeSizeFactorRespectToDefault = 1;

        this.pickManager = new PickManager(this);

        this.setCanvasLayerVisibilityAndOrder(currentNp, mapLayer2VisualizationOrder, layerVisibilityMap);
    }

    private void prepare()
    {

    }

    public boolean isWhatIfAnalysisActive()
    {
        return whatIfAnalysisActive;
    }

    public void setWhatIfAnalysisActive(boolean isWhatIfAnalysisActive)
    {
        this.whatIfAnalysisActive = isWhatIfAnalysisActive;
    }


    public ITableRowFilter getTableRowFilter()
    {
        return tableRowFilter;
    }

    public void cleanTableRowFilter()
    {
        this.tableRowFilter = null;
    }

    public void updateTableRowFilter(ITableRowFilter tableRowFilterToApply, FilterCombinationType filterCombinationType)
    {
        /* If clicked reset button, remove all existing filters */
        if (tableRowFilterToApply == null)
        {
            this.tableRowFilter = null;
            return;
        }

    	/* This is the first filter  */
        if (this.tableRowFilter == null) // when clicked reset button
        {
            this.tableRowFilter = tableRowFilterToApply;
            return;
        }

    	/* This is a concatenation of filters */
        if (filterCombinationType == null) filterCombinationType = FilterCombinationType.INCLUDEIF_AND;
        switch (filterCombinationType)
        {
            case INCLUDEIF_AND:
                this.tableRowFilter.recomputeApplyingShowIf_ThisAndThat(tableRowFilterToApply);
                break;
            case INCLUDEIF_OR:
                this.tableRowFilter.recomputeApplyingShowIf_ThisOrThat(tableRowFilterToApply);
                break;
            default:
                throw new RuntimeException();
        }
    }

    public boolean isVisibleInCanvas(GUINode gn)
    {
        final Node n = gn.getAssociatedNode();
        if (nodesToHideInCanvasAsMandatedByUserInTable.contains(n)) return false;
        if (!showInCanvasNonConnectedNodes)
        {
            final NetworkLayer layer = gn.getLayer();
            if (n.getOutgoingLinks(layer).isEmpty() && n.getIncomingLinks(layer).isEmpty()
                    && n.getOutgoingDemands(layer).isEmpty() && n.getIncomingDemands(layer).isEmpty()
                    && n.getOutgoingMulticastDemands(layer).isEmpty() && n.getIncomingMulticastDemands(layer).isEmpty())
                return false;
        }
        return true;
    }

    public boolean isVisibleInCanvas(GUILink gl)
    {
        if (gl.isIntraNodeLink())
        {
            final Node node = gl.getOriginNode().getAssociatedNode();
            final NetworkLayer originLayer = gl.getOriginNode().getLayer();
            final NetworkLayer destinationLayer = gl.getDestinationNode().getLayer();
            final int originIndexInVisualization = getCanvasVisualizationOrderRemovingNonVisible(originLayer);
            final int destinationIndexInVisualization = getCanvasVisualizationOrderRemovingNonVisible(destinationLayer);
            final int lowerVIndex = originIndexInVisualization < destinationIndexInVisualization ? originIndexInVisualization : destinationIndexInVisualization;
            final int upperVIndex = originIndexInVisualization > destinationIndexInVisualization ? originIndexInVisualization : destinationIndexInVisualization;
            cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(gl.getOriginNode());
            boolean atLeastOneLowerLayerVisible = false;
            for (int vIndex = 0; vIndex <= lowerVIndex; vIndex++)
                if (isVisibleInCanvas(getCanvasAssociatedGUINode(node, getCanvasNetworkLayerAtVisualizationOrderRemovingNonVisible(vIndex))))
                {
                    atLeastOneLowerLayerVisible = true;
                    break;
                }
            if (!atLeastOneLowerLayerVisible) return false;
            boolean atLeastOneUpperLayerVisible = false;
            for (int vIndex = upperVIndex; vIndex < getCanvasNumberOfVisibleLayers(); vIndex++)
                if (isVisibleInCanvas(getCanvasAssociatedGUINode(node, getCanvasNetworkLayerAtVisualizationOrderRemovingNonVisible(vIndex))))
                {
                    atLeastOneUpperLayerVisible = true;
                    break;
                }
            return atLeastOneUpperLayerVisible;
        } else
        {
            final Link e = gl.getAssociatedNetPlanLink();

            if (!visualizationSnapshot.getCanvasLinkVisibility(e.getLayer())) return false;
            if (linksToHideInCanvasAsMandatedByUserInTable.contains(e)) return false;
            final boolean inActiveLayer = e.getLayer() == this.getNetPlan().getNetworkLayerDefault();
            if (!showInCanvasLinksInNonActiveLayer && !inActiveLayer) return false;
            return true;
        }
    }

    /**
     * @return the interLayerSpaceInNetPlanCoordinates
     */
    public int getInterLayerSpaceInPixels()
    {
        return interLayerSpaceInPixels;
    }

    /**
     */
    public void setInterLayerSpaceInPixels(int interLayerSpaceInPixels)
    {
        this.interLayerSpaceInPixels = interLayerSpaceInPixels;
    }

    /**
     * @return the showInterLayerLinks
     */
    public boolean isShowInCanvasInterLayerLinks()
    {
        return showInCanvasInterLayerLinks;
    }

    /**
     * @param showInterLayerLinks the showInterLayerLinks to set
     */
    public void setShowInCanvasInterLayerLinks(boolean showInterLayerLinks)
    {
        this.showInCanvasInterLayerLinks = showInterLayerLinks;
    }

    /**
     * @return the isNetPlanEditable
     */
    public boolean isNetPlanEditable()
    {
        return this.getNetPlan().isModifiable();
    }

    public List<GUINode> getCanvasVerticallyStackedGUINodes(Node n)
    {
        return cache_mapNode2ListVerticallyStackedGUINodes.get(n);
    }

    public GUINode getCanvasAssociatedGUINode(Node n, NetworkLayer layer)
    {
        final Integer trueVisualizationIndex = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(layer);
        if (trueVisualizationIndex == null) return null;
        return getCanvasVerticallyStackedGUINodes(n).get(trueVisualizationIndex);
    }

    public GUILink getCanvasAssociatedGUILink(Link e)
    {
        return cache_canvasRegularLinkMap.get(e);
    }

    public Pair<Set<GUILink>, Set<GUILink>> getCanvasAssociatedGUILinksIncludingCoupling(Link e, boolean regularLinkIsPrimary)
    {
        Set<GUILink> resPrimary = new HashSet<>();
        Set<GUILink> resBackup = new HashSet<>();
        if (regularLinkIsPrimary) resPrimary.add(getCanvasAssociatedGUILink(e));
        else resBackup.add(getCanvasAssociatedGUILink(e));
        if (!e.isCoupled()) return Pair.of(resPrimary, resBackup);
        if (e.getCoupledDemand() != null)
        {
            /* add the intranode links */
            final NetworkLayer upperLayer = e.getLayer();
            final NetworkLayer downLayer = e.getCoupledDemand().getLayer();
            if (regularLinkIsPrimary)
            {
                resPrimary.addAll(getCanvasIntraNodeGUILinkSequence(e.getOriginNode(), upperLayer, downLayer));
                resPrimary.addAll(getCanvasIntraNodeGUILinkSequence(e.getDestinationNode(), downLayer, upperLayer));
            } else
            {
                resBackup.addAll(getCanvasIntraNodeGUILinkSequence(e.getOriginNode(), upperLayer, downLayer));
                resBackup.addAll(getCanvasIntraNodeGUILinkSequence(e.getDestinationNode(), downLayer, upperLayer));
            }

			/* add the regular links */
            Pair<Set<Link>, Set<Link>> traversedLinks = e.getCoupledDemand().getLinksThisLayerPotentiallyCarryingTraffic();
            for (Link ee : traversedLinks.getFirst())
            {
                Pair<Set<GUILink>, Set<GUILink>> pairGuiLinks = getCanvasAssociatedGUILinksIncludingCoupling(ee, true);
                if (regularLinkIsPrimary) resPrimary.addAll(pairGuiLinks.getFirst());
                else resBackup.addAll(pairGuiLinks.getFirst());
                resBackup.addAll(pairGuiLinks.getSecond());
            }
            for (Link ee : traversedLinks.getSecond())
            {
                Pair<Set<GUILink>, Set<GUILink>> pairGuiLinks = getCanvasAssociatedGUILinksIncludingCoupling(ee, false);
                resPrimary.addAll(pairGuiLinks.getFirst());
                resBackup.addAll(pairGuiLinks.getSecond());
            }
        } else if (e.getCoupledMulticastDemand() != null)
        {
            /* add the intranode links */
            final NetworkLayer upperLayer = e.getLayer();
            final MulticastDemand lowerLayerDemand = e.getCoupledMulticastDemand();
            final NetworkLayer downLayer = lowerLayerDemand.getLayer();
            if (regularLinkIsPrimary)
            {
                resPrimary.addAll(getCanvasIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode(), upperLayer, downLayer));
                resPrimary.addAll(getCanvasIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode(), downLayer, upperLayer));
                for (Node n : lowerLayerDemand.getEgressNodes())
                {
                    resPrimary.addAll(getCanvasIntraNodeGUILinkSequence(n, upperLayer, downLayer));
                    resPrimary.addAll(getCanvasIntraNodeGUILinkSequence(n, downLayer, upperLayer));
                }
            } else
            {
                resBackup.addAll(getCanvasIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode(), upperLayer, downLayer));
                resBackup.addAll(getCanvasIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode(), downLayer, upperLayer));
                for (Node n : lowerLayerDemand.getEgressNodes())
                {
                    resBackup.addAll(getCanvasIntraNodeGUILinkSequence(n, upperLayer, downLayer));
                    resBackup.addAll(getCanvasIntraNodeGUILinkSequence(n, downLayer, upperLayer));
                }
            }

            for (MulticastTree t : lowerLayerDemand.getMulticastTrees())
                for (Link ee : t.getLinkSet())
                {
                    Pair<Set<GUILink>, Set<GUILink>> pairGuiLinks = getCanvasAssociatedGUILinksIncludingCoupling(ee, true);
                    resPrimary.addAll(pairGuiLinks.getFirst());
                    resBackup.addAll(pairGuiLinks.getSecond());
                }
        }
        return Pair.of(resPrimary, resBackup);
    }

    public GUILink getCanvasIntraNodeGUILink(Node n, NetworkLayer from, NetworkLayer to)
    {
        final Integer fromRealVIndex = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(from);
        final Integer toRealVIndex = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(to);
        if ((fromRealVIndex == null) || (toRealVIndex == null)) return null;
        return cache_mapNode2IntraNodeCanvasGUILinkMap.get(n).get(Pair.of(fromRealVIndex, toRealVIndex));
    }

    public Set<GUILink> getCanvasIntraNodeGUILinks(Node n)
    {
        return cache_canvasIntraNodeGUILinks.get(n);
    }

    public List<GUILink> getCanvasIntraNodeGUILinkSequence(Node n, NetworkLayer from, NetworkLayer to)
    {
        if (from.getNetPlan() != this.getNetPlan()) throw new RuntimeException("Bad");
        if (to.getNetPlan() != this.getNetPlan()) throw new RuntimeException("Bad");
        final Integer fromRealVIndex = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(from);
        final Integer toRealVIndex = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(to);

        final List<GUILink> res = new LinkedList<>();
        if ((fromRealVIndex == null) || (toRealVIndex == null)) return res;
        if (fromRealVIndex == toRealVIndex) return res;
        final int increment = toRealVIndex > fromRealVIndex ? 1 : -1;
        int vLayerIndex = fromRealVIndex;
        do
        {
            final int origin = vLayerIndex;
            final int destination = vLayerIndex + increment;
            res.add(cache_mapNode2IntraNodeCanvasGUILinkMap.get(n).get(Pair.of(origin, destination)));
            vLayerIndex += increment;
        } while (vLayerIndex != toRealVIndex);

        return res;
    }

    VisualizationSnapshot getSnapshot()
    {
        return visualizationSnapshot;
    }

    /**
     * Implicitly it produces a reset of the picked state
     *
     * @param newCurrentNetPlan
     * @param newLayerOrderMap
     * @param newLayerVisibilityMap
     */
    public void setCanvasLayerVisibilityAndOrder(NetPlan newCurrentNetPlan, Map<NetworkLayer, Integer> newLayerOrderMap,
                                                 Map<NetworkLayer, Boolean> newLayerVisibilityMap)
    {
        if (newCurrentNetPlan == null) throw new RuntimeException("Trying to update an empty topology");

        final Map<NetworkLayer, Boolean> mapCanvasLinkVisibility = this.visualizationSnapshot.getMapCanvasLinkVisibility();

        this.visualizationSnapshot.resetSnapshot(newCurrentNetPlan);

        if (this.getNetPlan() != newCurrentNetPlan)
        {
            tableRowFilter = null;
            nodesToHideInCanvasAsMandatedByUserInTable = new HashSet<>();
            linksToHideInCanvasAsMandatedByUserInTable = new HashSet<>();
        }

        // Updating visualization snapshot
        if (newLayerOrderMap != null)
        {
            for (Map.Entry<NetworkLayer, Integer> entry : newLayerOrderMap.entrySet())
            {
                visualizationSnapshot.setLayerVisualizationOrder(entry.getKey(), entry.getValue());
            }
        }

        if (newLayerVisibilityMap != null)
        {
            for (Map.Entry<NetworkLayer, Boolean> entry : newLayerVisibilityMap.entrySet())
            {
                visualizationSnapshot.addLayerVisibility(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<NetworkLayer, Boolean> entry : mapCanvasLinkVisibility.entrySet())
        {
            if (!this.getNetPlan().getNetworkLayers().contains(entry.getKey())) continue;
            visualizationSnapshot.addLinkVisibility(entry.getKey(), entry.getValue());
        }

        /* implicitly we restart the picking state */
        pickManager.reset();

        this.cache_canvasIntraNodeGUILinks = new HashMap<>();
        this.cache_canvasRegularLinkMap = new HashMap<>();
        this.cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible = new DualHashBidiMap<>();
        this.cache_mapNode2IntraNodeCanvasGUILinkMap = new HashMap<>();
        this.cache_mapNode2ListVerticallyStackedGUINodes = new HashMap<>();
        for (int layerVisualizationOrderIncludingNonVisible = 0; layerVisualizationOrderIncludingNonVisible < this.getNetPlan().getNumberOfLayers(); layerVisualizationOrderIncludingNonVisible++)
        {
            final NetworkLayer layer = MapUtils.invertMap(visualizationSnapshot.getMapCanvasLayerVisualizationOrder()).get(layerVisualizationOrderIncludingNonVisible);
            if (isLayerVisibleInCanvas(layer))
                cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.put(layer, cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.size());
        }
        for (Node n : this.getNetPlan().getNodes())
        {
            List<GUINode> guiNodesThisNode = new ArrayList<>();
            cache_mapNode2ListVerticallyStackedGUINodes.put(n, guiNodesThisNode);
            Set<GUILink> intraNodeGUILinksThisNode = new HashSet<>();
            cache_canvasIntraNodeGUILinks.put(n, intraNodeGUILinksThisNode);
            Map<Pair<Integer, Integer>, GUILink> thisNodeInterLayerLinksInfoMap = new HashMap<>();
            cache_mapNode2IntraNodeCanvasGUILinkMap.put(n, thisNodeInterLayerLinksInfoMap);
            for (int trueVisualizationOrderIndex = 0; trueVisualizationOrderIndex < cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.size(); trueVisualizationOrderIndex++)
            {
                final NetworkLayer newLayer = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.inverseBidiMap().get(trueVisualizationOrderIndex);
                final double iconHeightIfNotActive = nodeSizeFactorRespectToDefault * (getNetPlan().getNumberOfNodes() > 100 ? VisualizationConstants.DEFAULT_GUINODE_SHAPESIZE_MORETHAN100NODES : VisualizationConstants.DEFAULT_GUINODE_SHAPESIZE);
                final GUINode gn = new GUINode(n, newLayer, iconHeightIfNotActive);
                guiNodesThisNode.add(gn);
                if (trueVisualizationOrderIndex > 0)
                {
                    final GUINode lowerLayerGNode = guiNodesThisNode.get(trueVisualizationOrderIndex - 1);
                    final GUINode upperLayerGNode = guiNodesThisNode.get(trueVisualizationOrderIndex);
                    if (upperLayerGNode != gn) throw new RuntimeException();
                    final GUILink glLowerToUpper = new GUILink(this , null, lowerLayerGNode, gn,
                            VisualizationUtils.resizedBasicStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE, linkWidthIncreaseFactorRespectToDefault),
                            VisualizationUtils.resizedBasicStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE, linkWidthIncreaseFactorRespectToDefault));
                    final GUILink glUpperToLower = new GUILink(this , null, gn, lowerLayerGNode,
                            VisualizationUtils.resizedBasicStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE, linkWidthIncreaseFactorRespectToDefault),
                            VisualizationUtils.resizedBasicStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE, linkWidthIncreaseFactorRespectToDefault));
                    intraNodeGUILinksThisNode.add(glLowerToUpper);
                    intraNodeGUILinksThisNode.add(glUpperToLower);
                    thisNodeInterLayerLinksInfoMap.put(Pair.of(trueVisualizationOrderIndex - 1, trueVisualizationOrderIndex), glLowerToUpper);
                    thisNodeInterLayerLinksInfoMap.put(Pair.of(trueVisualizationOrderIndex, trueVisualizationOrderIndex - 1), glUpperToLower);
                }
            }
        }
        for (int trueVisualizationOrderIndex = 0; trueVisualizationOrderIndex < cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.size(); trueVisualizationOrderIndex++)
        {
            final NetworkLayer layer = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.inverseBidiMap().get(trueVisualizationOrderIndex);
            for (Link e : this.getNetPlan().getLinks(layer))
            {
                final GUINode gn1 = cache_mapNode2ListVerticallyStackedGUINodes.get(e.getOriginNode()).get(trueVisualizationOrderIndex);
                final GUINode gn2 = cache_mapNode2ListVerticallyStackedGUINodes.get(e.getDestinationNode()).get(trueVisualizationOrderIndex);
                final GUILink gl1 = new GUILink(this , e, gn1, gn2,
                        VisualizationUtils.resizedBasicStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_ACTIVELAYER, linkWidthIncreaseFactorRespectToDefault),
                        VisualizationUtils.resizedBasicStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE, linkWidthIncreaseFactorRespectToDefault));
                cache_canvasRegularLinkMap.put(e, gl1);
            }
        }
    }

    public boolean decreaseCanvasFontSizeAll()
    {
        boolean changedSize = false;
        for (GUINode gn : getCanvasAllGUINodes())
            changedSize |= gn.decreaseFontSize();
        return changedSize;
    }

    public void increaseCanvasFontSizeAll()
    {
        for (GUINode gn : getCanvasAllGUINodes())
            gn.increaseFontSize();
    }

    public void decreaseCanvasNodeSizeAll()
    {
        nodeSizeFactorRespectToDefault *= VisualizationConstants.SCALE_OUT;
        for (GUINode gn : getCanvasAllGUINodes())
            gn.setIconHeightInNonActiveLayer(gn.getIconHeightInNotActiveLayer() * VisualizationConstants.SCALE_OUT);
    }

    public void increaseCanvasNodeSizeAll()
    {
        nodeSizeFactorRespectToDefault *= VisualizationConstants.SCALE_IN;
        for (GUINode gn : getCanvasAllGUINodes())
            gn.setIconHeightInNonActiveLayer(gn.getIconHeightInNotActiveLayer() * VisualizationConstants.SCALE_IN);
    }

    public void decreaseCanvasLinkSizeAll()
    {
        final float multFactor = VisualizationConstants.SCALE_OUT;
        linkWidthIncreaseFactorRespectToDefault *= multFactor;
        for (GUILink e : getCanvasAllGUILinks(true, true))
            e.setEdgeStroke(VisualizationUtils.resizedBasicStroke(e.getStrokeIfActiveLayer(), multFactor), VisualizationUtils.resizedBasicStroke(e.getStrokeIfNotActiveLayer(), multFactor));
    }

    public void increaseCanvasLinkSizeAll()
    {
        final float multFactor = VisualizationConstants.SCALE_IN;
        linkWidthIncreaseFactorRespectToDefault *= multFactor;
        for (GUILink e : getCanvasAllGUILinks(true, true))
            e.setEdgeStroke(VisualizationUtils.resizedBasicStroke(e.getStrokeIfActiveLayer(), multFactor), VisualizationUtils.resizedBasicStroke(e.getStrokeIfNotActiveLayer(), multFactor));
    }

    public int getCanvasNumberOfVisibleLayers()
    {
        return cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.size();
    }

    /**
     * @return the showNodeNames
     */
    public boolean isCanvasShowNodeNames()
    {
        return showInCanvasNodeNames;
    }

    /**
     * @param showNodeNames the showNodeNames to set
     */
    public void setCanvasShowNodeNames(boolean showNodeNames)
    {
        this.showInCanvasNodeNames = showNodeNames;
    }

    /**
     * @return the showLinkLabels
     */

    public boolean isCanvasShowLinkLabels()
    {
        return showInCanvasLinkLabels;
    }

    /**
     * @param showLinkLabels the showLinkLabels to set
     */
    public void setCanvasShowLinkLabels(boolean showLinkLabels)
    {
        this.showInCanvasLinkLabels = showLinkLabels;
    }

    /**
     * @return the showNonConnectedNodes
     */
    public boolean isCanvasShowNonConnectedNodes()
    {
        return showInCanvasNonConnectedNodes;
    }

    /**
     * @param showNonConnectedNodes the showNonConnectedNodes to set
     */
    public void setCanvasShowNonConnectedNodes(boolean showNonConnectedNodes)
    {
        this.showInCanvasNonConnectedNodes = showNonConnectedNodes;
    }

    public void hideOnCanvas(Node n)
    {
        nodesToHideInCanvasAsMandatedByUserInTable.add(n);
    }

    public void showOnCanvas(Node n)
    {
        if (nodesToHideInCanvasAsMandatedByUserInTable.contains(n))
            nodesToHideInCanvasAsMandatedByUserInTable.remove(n);
    }

    public boolean isHiddenOnCanvas(Node n)
    {
        return nodesToHideInCanvasAsMandatedByUserInTable.contains(n);
    }

    public void hideOnCanvas(Link e)
    {
        linksToHideInCanvasAsMandatedByUserInTable.add(e);
    }

    public void showOnCanvas(Link e)
    {
        if (linksToHideInCanvasAsMandatedByUserInTable.contains(e))
            linksToHideInCanvasAsMandatedByUserInTable.remove(e);
    }

    public boolean isHiddenOnCanvas(Link e)
    {
        return linksToHideInCanvasAsMandatedByUserInTable.contains(e);
    }

    public Set<GUILink> getCanvasAllGUILinks(boolean includeRegularLinks, boolean includeInterLayerLinks)
    {
        Set<GUILink> res = new HashSet<>();
        if (includeRegularLinks) res.addAll(cache_canvasRegularLinkMap.values());
        if (includeInterLayerLinks)
            for (Node n : this.getNetPlan().getNodes())
                res.addAll(this.cache_canvasIntraNodeGUILinks.get(n));
        return res;
    }

    public Set<GUINode> getCanvasAllGUINodes()
    {
        Set<GUINode> res = new HashSet<>();
        for (List<GUINode> list : this.cache_mapNode2ListVerticallyStackedGUINodes.values()) res.addAll(list);
        return res;
    }


    public double getCanvasDefaultVerticalDistanceForInterLayers()
    {
        if (this.getNetPlan().getNumberOfNodes() == 0) return 1.0;
        final int numVisibleLayers = getCanvasNumberOfVisibleLayers() == 0 ? this.getNetPlan().getNumberOfLayers() : getCanvasNumberOfVisibleLayers();
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (Node n : this.getNetPlan().getNodes())
        {
            final double y = n.getXYPositionMap().getY();
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        if ((maxY - minY < 1e-6)) return Math.abs(maxY) / (30 * numVisibleLayers);
        return (maxY - minY) / (30 * numVisibleLayers);
    }


    /**
     * To call when the topology has new/has removed any link or node, but keeping the same layers.
     * The topology is remade, which involves implicitly a reset of the view
     */
    public void recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals()
    {
        this.setCanvasLayerVisibilityAndOrder(this.getNetPlan(), null, null);
    }

    public void setCanvasLayerVisibility(final NetworkLayer layer, final boolean isVisible)
    {
        if (!this.getNetPlan().getNetworkLayers().contains(layer)) throw new RuntimeException();
        BidiMap<NetworkLayer, Integer> new_layerVisiblityOrderMap = new DualHashBidiMap<>(this.visualizationSnapshot.getMapCanvasLayerVisualizationOrder());
        Map<NetworkLayer, Boolean> new_layerVisibilityMap = new HashMap<>(this.visualizationSnapshot.getMapCanvasLayerVisibility());
        new_layerVisibilityMap.put(layer, isVisible);
        setCanvasLayerVisibilityAndOrder(this.getNetPlan(), new_layerVisiblityOrderMap, new_layerVisibilityMap);
    }

    public boolean isLayerVisibleInCanvas(final NetworkLayer layer)
    {
        return visualizationSnapshot.getCanvasLayerVisibility(layer);
    }

    public void setLayerLinksVisibilityInCanvas(final NetworkLayer layer, final boolean showLinks)
    {
        if (!this.getNetPlan().getNetworkLayers().contains(layer)) throw new RuntimeException();
        visualizationSnapshot.getMapCanvasLinkVisibility().put(layer, showLinks);
    }

    public boolean isCanvasLayerLinksShown(final NetworkLayer layer)
    {
        return visualizationSnapshot.getCanvasLinkVisibility(layer);
    }

    public NetworkLayer getCanvasNetworkLayerAtVisualizationOrderRemovingNonVisible(int trueVisualizationOrder)
    {
        if (trueVisualizationOrder < 0) throw new RuntimeException("");
        if (trueVisualizationOrder >= getCanvasNumberOfVisibleLayers()) throw new RuntimeException("");
        return cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.inverseBidiMap().get(trueVisualizationOrder);
    }

    public NetworkLayer getCanvasNetworkLayerAtVisualizationOrderNotRemovingNonVisible(int visualizationOrder)
    {
        if (visualizationOrder < 0) throw new RuntimeException("");
        if (visualizationOrder >= this.getNetPlan().getNumberOfLayers())
            throw new RuntimeException("");
        return MapUtils.invertMap(visualizationSnapshot.getMapCanvasLayerVisualizationOrder()).get(visualizationOrder);
    }

    public int getCanvasVisualizationOrderRemovingNonVisible(NetworkLayer layer)
    {
        Integer res = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(layer);
        if (res == null) throw new RuntimeException("");
        return res;
    }

    public int getCanvasVisualizationOrderNotRemovingNonVisible(NetworkLayer layer)
    {
        Integer res = visualizationSnapshot.getCanvasLayerVisualizationOrder(layer);
        return res;
    }

    public static Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> generateCanvasDefaultVisualizationLayerInfo(NetPlan np)
    {
        final BidiMap<NetworkLayer, Integer> res_1 = new DualHashBidiMap<>();
        final Map<NetworkLayer, Boolean> res_2 = new HashMap<>();

        for (NetworkLayer layer : np.getNetworkLayers())
        {
            res_1.put(layer, res_1.size());
            res_2.put(layer, true);
        }
        return Pair.of(res_1, res_2);
    }

    public Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(Set<NetworkLayer> newNetworkLayers)
    {
        final Map<NetworkLayer, Boolean> oldLayerVisibilityMap = getCanvasLayerVisibilityMap();
        final BidiMap<NetworkLayer, Integer> oldLayerOrderMap = new DualHashBidiMap<>(getCanvasLayerOrderIndexMap(true));
        final Map<NetworkLayer, Boolean> newLayerVisibilityMap = new HashMap<>();
        final BidiMap<NetworkLayer, Integer> newLayerOrderMap = new DualHashBidiMap<>();
        for (int oldVisibilityOrderIndex = 0; oldVisibilityOrderIndex < oldLayerOrderMap.size(); oldVisibilityOrderIndex++)
        {
            final NetworkLayer oldLayer = oldLayerOrderMap.inverseBidiMap().get(oldVisibilityOrderIndex);
            if (newNetworkLayers.contains(oldLayer))
            {
                newLayerOrderMap.put(oldLayer, newLayerVisibilityMap.size());
                newLayerVisibilityMap.put(oldLayer, oldLayerVisibilityMap.get(oldLayer));
            }
        }
        final Set<NetworkLayer> newLayersNotExistingBefore = Sets.difference(newNetworkLayers, oldLayerVisibilityMap.keySet());
        for (NetworkLayer newLayer : newLayersNotExistingBefore)
        {
            newLayerOrderMap.put(newLayer, newLayerVisibilityMap.size());
            newLayerVisibilityMap.put(newLayer, true); // new layers always visible
        }
        return Pair.of(newLayerOrderMap, newLayerVisibilityMap);
    }

    public List<NetworkLayer> getCanvasLayersInVisualizationOrder(boolean includeNonVisible)
    {
        Map<Integer, NetworkLayer> map = includeNonVisible ? MapUtils.invertMap(visualizationSnapshot.getMapCanvasLayerVisualizationOrder()) : cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.inverseBidiMap();
        List<NetworkLayer> res = new ArrayList<>();
        for (int vIndex = 0; vIndex < this.getNetPlan().getNumberOfLayers(); vIndex++)
            res.add(map.get(vIndex));
        return res;
    }

    public Map<NetworkLayer, Integer> getCanvasLayerOrderIndexMap(boolean includeNonVisible)
    {
        return includeNonVisible ? visualizationSnapshot.getMapCanvasLayerVisualizationOrder() : cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible;
    }

    /**
     * @return the showLowerLayerPropagation
     */
    public boolean isShowInCanvasLowerLayerPropagation()
    {
        return showInCanvasLowerLayerPropagation;
    }


    /**
     * @param showLowerLayerPropagation the showLowerLayerPropagation to set
     */
    public void setShowInCanvasLowerLayerPropagation(boolean showLowerLayerPropagation)
    {
        if (showLowerLayerPropagation == this.showInCanvasLowerLayerPropagation) return;
        this.showInCanvasLowerLayerPropagation = showLowerLayerPropagation;

        if (this.getPickedElementType() != null)
            if (this.getPickedNetworkElements() != null)
                this.pickElement(this.getPickedNetworkElements());
            else
                this.pickForwardingRule(this.getPickedForwardingRules());
    }

    /**
     * @return the showUpperLayerPropagation
     */
    public boolean isShowInCanvasUpperLayerPropagation()
    {
        return showInCanvasUpperLayerPropagation;
    }

    /**
     * @return the showThisLayerPropagation
     */
    public boolean isShowInCanvasThisLayerPropagation()
    {
        return showInCanvasThisLayerPropagation;
    }


    /* Changes */
    public List<Double> getLinkUtilizationColor()
    {
        return linkUtilizationColorThresholdList;
    }

    public List<Double> getLinkRunoutTimeColor()
    {
        return linkRunoutTimeColorThresholdList;
    }

    public List<Double> getLinkCapacityThickness()
    {
        return linkCapacityThicknessThresholdList;
    }

    public void setIsActiveLinkUtilizationColorThresholdList(boolean isActive) { this.isActiveLinkUtilizationColorThresholdList = isActive; }
    public void setIsActiveLinkRunoutTimeColorThresholdList(boolean isActive) { this.isActiveLinkRunoutTimeColorThresholdList = isActive; }
    public void setIsActiveLinkCapacityThicknessThresholdList(boolean isActive) { this.isActiveLinkCapacityThicknessThresholdList = isActive; }
    public boolean getIsActiveLinkUtilizationColorThresholdList() { return isActiveLinkUtilizationColorThresholdList; }
    public boolean getIsActiveLinkRunoutTimeColorThresholdList() { return isActiveLinkRunoutTimeColorThresholdList; }
    public boolean getIsActiveLinkCapacityThicknessThresholdList() { return isActiveLinkCapacityThicknessThresholdList; }

    public void setLinkUtilizationColor(List<Double> linkUtilizationColorList)
    {
        this.linkUtilizationColorThresholdList = linkUtilizationColorList;
    }

    public void setLinkRunoutTimeColor(List<Double> linkRunoutTimeColor)
    {
        this.linkRunoutTimeColorThresholdList = linkRunoutTimeColor;
    }
    public void setLinkCapacityThickness(List<Double> linkCapacityThickness)
    {
        this.linkCapacityThicknessThresholdList = linkCapacityThickness;
    }
    public Color getLinkColorAccordingToUtilization (double linkUtilization)
    {
        linkUtilization*=100;

        for(int i = 0; i < this.linkUtilizationColorThresholdList.size(); i++)
            if(linkUtilization < this.linkUtilizationColorThresholdList.get(i))
                return VisualizationConstants.DEFAULT_LINKCOLORSPERUTILIZATIONANDRUNOUT.get(i);

        return VisualizationConstants.DEFAULT_LINKCOLORSPERUTILIZATIONANDRUNOUT.get(VisualizationConstants.DEFAULT_LINKCOLORSPERUTILIZATIONANDRUNOUT.size() - 1);
    }
    public double getLinkRelativeThicknessAccordingToCapacity (double linkCapacity)
    {
        for(int i = 0; i < this.linkCapacityThicknessThresholdList.size(); i++)
            if(linkCapacity < this.linkCapacityThicknessThresholdList.get(i)) return VisualizationConstants.DEFAULT_LINKRELATIVETHICKNESSVALUES.get(i);
        return VisualizationConstants.DEFAULT_LINKRELATIVETHICKNESSVALUES.get(this.linkCapacityThicknessThresholdList.size());
    }

    /**
     * @param showUpperLayerPropagation the showUpperLayerPropagation to set
     */
    public void setShowInCanvasUpperLayerPropagation(boolean showUpperLayerPropagation)
    {
        if (showUpperLayerPropagation == this.showInCanvasUpperLayerPropagation) return;
        this.showInCanvasUpperLayerPropagation = showUpperLayerPropagation;

        if (this.getPickedElementType() != null)
            if (pickManager.getPickedNetworkElements() != null)
                this.pickElement(pickManager.getPickedNetworkElements());
            else
                pickManager.pickForwardingRule(pickManager.getPickedForwardingRules());
    }

    /**
     * @param showThisLayerPropagation the showThisLayerPropagation to set
     */
    public void setShowInCanvasThisLayerPropagation(boolean showThisLayerPropagation)
    {
        if (showThisLayerPropagation == this.showInCanvasThisLayerPropagation) return;
        this.showInCanvasThisLayerPropagation = showThisLayerPropagation;
        if (this.getPickedElementType() != null)
            if (this.getPickedNetworkElements() != null)
                this.pickElement(this.getPickedNetworkElements());
            else
                this.pickForwardingRule(this.getPickedForwardingRules());
    }

    public Map<NetworkLayer, Boolean> getCanvasLayerVisibilityMap()
    {
        return Collections.unmodifiableMap(this.visualizationSnapshot.getMapCanvasLayerVisibility());
    }

    public static Pair<ImageIcon, Shape> getIcon(URL url, int height, Color borderColor)
    {
        final Pair<ImageIcon, Shape> iconShapeInfo = databaseOfAlreadyReadIcons.get(Triple.of(url, height, borderColor));
        if (iconShapeInfo != null) return iconShapeInfo;
        if (url == null)
        {
            BufferedImage img = ImageUtils.createCircle(height, (Color) VisualizationConstants.DEFAULT_GUINODE_COLOR);
            if (img.getHeight() != height) throw new RuntimeException();
            final Shape shapeNoBorder = FourPassImageShaper.getShape(img);
            if (borderColor.getAlpha() != 0)
                img = ImageUtils.addBorder(img, VisualizationConstants.DEFAULT_ICONBORDERSIZEINPIXELS, borderColor);
            final ImageIcon icon = new ImageIcon(img);
            final Pair<ImageIcon, Shape> res = Pair.of(icon, shapeNoBorder);
            databaseOfAlreadyReadIcons.put(Triple.of(null, icon.getIconHeight(), borderColor), res);
            return res;
        }
        try
        {
            /* Read the base buffered image */
            BufferedImage img = ImageIO.read(url);
            if (img.getHeight() != height)
                img = ImageUtils.resize(img, (int) (img.getWidth() * height / (double) img.getHeight()), height);
            if (img.getHeight() != height) throw new RuntimeException();
            final Shape shapeNoBorder = FourPassImageShaper.getShape(img);
            if (borderColor.getAlpha() != 0)
                img = ImageUtils.addBorder(img, VisualizationConstants.DEFAULT_ICONBORDERSIZEINPIXELS, borderColor);
            final ImageIcon icon = new ImageIcon(img);
            final AffineTransform translateTransform = AffineTransform.getTranslateInstance(-icon.getIconWidth() / 2, -icon.getIconHeight() / 2);
            final Pair<ImageIcon, Shape> res = Pair.of(icon, translateTransform.createTransformedShape(shapeNoBorder));
            databaseOfAlreadyReadIcons.put(Triple.of(url, icon.getIconHeight(), borderColor), res);
            return res;
        } catch (Exception e)
        {
            System.out.println("URL: **" + url + "**");
            System.out.println(url);
            /* Use the default image, whose URL is the one given */
            e.printStackTrace();
            return getIcon(null, height, borderColor);
        }
    }

    public Object getPickNavigationBackElement()
    {
        return pickManager.getPickNavigationBackElement();
    }

    public Object getPickNavigationForwardElement()
    {
        return pickManager.getPickNavigationForwardElement();
    }

    public void pickForwardingRule(Pair<Demand, Link> fr)
    {
        pickForwardingRule(Collections.singletonList(fr));
    }

    public void pickForwardingRule(List<Pair<Demand, Link>> pickedFRs)
    {
        pickManager.pickForwardingRule(pickedFRs);
    }

    public NetworkElementType getPickedElementType()
    {
        final List<NetworkElement> pickedNetworkElements = getPickedNetworkElements();
        final List<Pair<Demand, Link>> pickedForwardingRules = getPickedForwardingRules();

        if (!pickedNetworkElements.isEmpty())
            return NetworkElementType.getType(pickedNetworkElements);
        else if (!pickedForwardingRules.isEmpty())
            return NetworkElementType.FORWARDING_RULE;
        else
            return null;
    }

    public List<NetworkElement> getPickedNetworkElements()
    {
        return pickManager.getPickedNetworkElements();
    }


    public List<Pair<Demand, Link>> getPickedForwardingRules()
    {
        return pickManager.getPickedForwardingRules();
    }

    public void pickElement(NetworkElement es)
    {
        if (es == null) return;

        try
        {
            if (es instanceof NetworkLayer) pickManager.pickLayer((NetworkLayer) es);
            else if (es instanceof Node) pickManager.pickNode(Collections.singletonList((Node) es));
            else if (es instanceof Link) pickManager.pickLink(Collections.singletonList((Link) es));
            else if (es instanceof Demand) pickManager.pickDemand(Collections.singletonList((Demand) es));
            else if (es instanceof Route) pickManager.pickRoute(Collections.singletonList((Route) es));
            else if (es instanceof MulticastDemand)
                pickManager.pickMulticastDemand(Collections.singletonList((MulticastDemand) es));
            else if (es instanceof MulticastTree)
                pickManager.pickMulticastTree(Collections.singletonList((MulticastTree) es));
            else if (es instanceof Resource) pickManager.pickResource(Collections.singletonList((Resource) es));
            else if (es instanceof SharedRiskGroup)
                pickManager.pickSRG(Collections.singletonList((SharedRiskGroup) es));
            else return;
        } catch (ClassCastException e)
        {
            ErrorHandling.showErrorDialog("Error");
            e.printStackTrace();
        }
    }

    public void pickElement(List<? extends NetworkElement> es)
    {
        if (es == null) return;
        if (es.isEmpty()) return;

        try
        {
            if (es.get(0) instanceof Node) pickManager.pickNode((List<Node>) es);
            else if (es.get(0) instanceof Link) pickManager.pickLink((List<Link>) es);
            else if (es.get(0) instanceof Demand) pickManager.pickDemand((List<Demand>) es);
            else if (es.get(0) instanceof Route) pickManager.pickRoute((List<Route>) es);
            else if (es.get(0) instanceof MulticastDemand) pickManager.pickMulticastDemand((List<MulticastDemand>) es);
            else if (es.get(0) instanceof MulticastTree) pickManager.pickMulticastTree((List<MulticastTree>) es);
            else if (es.get(0) instanceof Resource) pickManager.pickResource((List<Resource>) es);
            else if (es.get(0) instanceof SharedRiskGroup) pickManager.pickSRG((List<SharedRiskGroup>) es);
            else return;
        } catch (ClassCastException e)
        {
            ErrorHandling.showErrorDialog("Error");
            e.printStackTrace();
        }
    }

    public void resetPickedState()
    {
        pickManager.cleanPick();
    }

    float getLinkWidthFactor()
    {
        return linkWidthIncreaseFactorRespectToDefault;
    }

    float getNodeSizeFactorRespectToDefault()
    {
        return nodeSizeFactorRespectToDefault;
    }
}
