package com.net2plan.gui.plugins.networkDesign.visualizationControl;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITableRowFilter.FilterCombinationType;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUILink;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.ImageUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;
import edu.uci.ics.jung.visualization.FourPassImageShaper;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.MapUtils;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class VisualizationState
{
    private static Map<Triple<URL, Integer, Color>, Pair<ImageIcon, Shape>> databaseOfAlreadyReadIcons = new HashMap<>(); // for each url, height, and border color, an image

    private PickTimeLineManager pickTimeLineManager;

    private NetworkElementType pickedElementType;
    private List<? extends NetworkElement> pickedElement;
    private List<Pair<Demand, Link>> pickedForwardingRule;

    private boolean showInCanvasNodeNames;
    private boolean showInCanvasLinkLabels;
    private boolean showInCanvasLinksInNonActiveLayer;
    private boolean showInCanvasInterLayerLinks;
    private boolean showInCanvasLowerLayerPropagation;
    private boolean showInCanvasUpperLayerPropagation;
    private boolean showInCanvasThisLayerPropagation;
    private boolean whatIfAnalysisActive;
    private ITableRowFilter tableRowFilter;

    private boolean showInCanvasNonConnectedNodes;
    private int interLayerSpaceInPixels;
    private Set<Node> nodesToHideInCanvasAsMandatedByUserInTable;
    private Set<Link> linksToHideInCanvasAsMandatedByUserInTable;

    private VisualizationSnapshot visualizationSnapshot;
    private float linkWidthIncreaseFactorRespectToDefault;
    private float nodeSizeIncreaseFactorRespectToDefault;
    
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
        this.pickTimeLineManager = new PickTimeLineManager();
        this.pickedElement = null;
        this.pickedForwardingRule = null;
        this.linkWidthIncreaseFactorRespectToDefault = 1;
        this.nodeSizeIncreaseFactorRespectToDefault = 1;

        this.setCanvasLayerVisibilityAndOrder(currentNp, mapLayer2VisualizationOrder, layerVisibilityMap);
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

    public void updateTableRowFilter(ITableRowFilter tableRowFilterToApply , FilterCombinationType filterCombinationType)
    {
        if (tableRowFilterToApply == null)
        {
            this.tableRowFilter = null;
            return;
        }
        if (this.tableRowFilter == null)
        {
            this.tableRowFilter = tableRowFilterToApply;
            return;
        }
        switch (filterCombinationType)
        {
        	case INCLUDEIF_AND: 
        		this.tableRowFilter.recomputeApplyingShowIf_ThisAndThat(tableRowFilterToApply);
        		break;
        	case INCLUDEIF_OR: 
            	this.tableRowFilter.recomputeApplyingShowIf_ThisOrThat(tableRowFilterToApply);
            	break;
            default: throw new RuntimeException ();
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
            Pair<Set<Link>, Set<Link>> traversedLinks = e.getCoupledDemand().getLinksThisLayerPotentiallyCarryingTraffic(true);
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
        this.pickedElementType = null;
        this.pickedElement = null;
        this.pickedForwardingRule = null;

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
                final double iconHeightIfNotActive = nodeSizeIncreaseFactorRespectToDefault * (getNetPlan().getNumberOfNodes() > 100? VisualizationConstants.DEFAULT_GUINODE_SHAPESIZE_MORETHAN100NODES : VisualizationConstants.DEFAULT_GUINODE_SHAPESIZE);
                final GUINode gn = new GUINode(n, newLayer , iconHeightIfNotActive);
                guiNodesThisNode.add(gn);
                if (trueVisualizationOrderIndex > 0)
                {
                    final GUINode lowerLayerGNode = guiNodesThisNode.get(trueVisualizationOrderIndex - 1);
                    final GUINode upperLayerGNode = guiNodesThisNode.get(trueVisualizationOrderIndex);
                    if (upperLayerGNode != gn) throw new RuntimeException();
                    final GUILink glLowerToUpper = new GUILink(null, lowerLayerGNode, gn , 
                    		resizedBasicStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE, linkWidthIncreaseFactorRespectToDefault) , 
                    		resizedBasicStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE, linkWidthIncreaseFactorRespectToDefault));
                    final GUILink glUpperToLower = new GUILink(null, gn, lowerLayerGNode , 
                    		resizedBasicStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE, linkWidthIncreaseFactorRespectToDefault) , 
                    		resizedBasicStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE, linkWidthIncreaseFactorRespectToDefault));
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
                final GUILink gl1 = new GUILink(e, gn1, gn2 , 
                	resizedBasicStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_ACTIVELAYER, linkWidthIncreaseFactorRespectToDefault) , 
        			resizedBasicStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE, linkWidthIncreaseFactorRespectToDefault));
                cache_canvasRegularLinkMap.put(e, gl1);
            }
        }
    }

    // TODO: Test
//    private void checkCacheConsistency()
//    {
//        for (Node n : currentNp.getNodes())
//        {
//            assertTrue(cache_canvasIntraNodeGUILinks.get(n) != null);
//            assertTrue(cache_mapNode2IntraNodeCanvasGUILinkMap.get(n) != null);
//            assertTrue(cache_mapNode2ListVerticallyStackedGUINodes.get(n) != null);
//            for (Entry<Pair<Integer, Integer>, GUILink> entry : cache_mapNode2IntraNodeCanvasGUILinkMap.get(n).entrySet())
//            {
//                final int fromLayer = entry.getKey().getFirst();
//                final int toLayer = entry.getKey().getSecond();
//                final GUILink gl = entry.getValue();
//                assertTrue(gl.isIntraNodeLink());
//                assertTrue(gl.getOriginNode().getAssociatedNode() == n);
//                assertTrue(getCanvasVisualizationOrderRemovingNonVisible(gl.getOriginNode().getLayer()) == fromLayer);
//                assertTrue(getCanvasVisualizationOrderRemovingNonVisible(gl.getDestinationNode().getLayer()) == toLayer);
//            }
//            assertEquals(new HashSet<>(cache_mapNode2IntraNodeCanvasGUILinkMap.get(n).values()), cache_canvasIntraNodeGUILinks.get(n));
//            for (GUILink gl : cache_canvasIntraNodeGUILinks.get(n))
//            {
//                assertTrue(gl.isIntraNodeLink());
//                assertEquals(gl.getOriginNode().getAssociatedNode(), n);
//                assertEquals(gl.getDestinationNode().getAssociatedNode(), n);
//            }
//            assertEquals(cache_mapNode2ListVerticallyStackedGUINodes.get(n).size(), getCanvasNumberOfVisibleLayers());
//            int indexLayer = 0;
//            for (GUINode gn : cache_mapNode2ListVerticallyStackedGUINodes.get(n))
//            {
//                assertEquals(gn.getLayer(), cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.inverseBidiMap().get(indexLayer));
//                assertEquals(getCanvasVisualizationOrderRemovingNonVisible(gn.getLayer()), indexLayer++);
//                assertEquals(gn.getAssociatedNode(), n);
//            }
//        }
//    }

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
    	nodeSizeIncreaseFactorRespectToDefault *= VisualizationConstants.SCALE_OUT;
        for (GUINode gn : getCanvasAllGUINodes())
            gn.setIconHeightInNonActiveLayer(gn.getIconHeightInNotActiveLayer() * VisualizationConstants.SCALE_OUT);
    }

    public void increaseCanvasNodeSizeAll()
    {
    	nodeSizeIncreaseFactorRespectToDefault *= VisualizationConstants.SCALE_IN;
        for (GUINode gn : getCanvasAllGUINodes())
            gn.setIconHeightInNonActiveLayer(gn.getIconHeightInNotActiveLayer() * VisualizationConstants.SCALE_IN);
    }

    public void decreaseCanvasLinkSizeAll()
    {
    	final float multFactor = VisualizationConstants.SCALE_OUT;
    	linkWidthIncreaseFactorRespectToDefault *= multFactor;
        for (GUILink e : getCanvasAllGUILinks(true, true))
        	e.setEdgeStroke(resizedBasicStroke(e.getStrokeIfActiveLayer(), multFactor), resizedBasicStroke(e.getStrokeIfNotActiveLayer(), multFactor));
    }

    public void increaseCanvasLinkSizeAll()
    {
    	final float multFactor = VisualizationConstants.SCALE_IN;
    	linkWidthIncreaseFactorRespectToDefault *= multFactor;
        for (GUILink e : getCanvasAllGUILinks(true, true))
        	e.setEdgeStroke(resizedBasicStroke(e.getStrokeIfActiveLayer(), multFactor), resizedBasicStroke(e.getStrokeIfNotActiveLayer(), multFactor));
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
        if (nodesToHideInCanvasAsMandatedByUserInTable.contains(n)) nodesToHideInCanvasAsMandatedByUserInTable.remove(n);
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
        if (linksToHideInCanvasAsMandatedByUserInTable.contains(e)) linksToHideInCanvasAsMandatedByUserInTable.remove(e);
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
        if (res == null) throw new RuntimeException();
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

    public boolean isElementPicked()
    {
        return pickedElementType != null;
    }

    public NetworkElementType getPickedElementType()
    {
        return pickedElementType;
    }

    @Nullable
    public List<NetworkElement> getPickedNetworkElement()
    {
        return pickedElement == null ? null : Collections.unmodifiableList(pickedElement);
    }

    @Nullable
    public List<Pair<Demand, Link>> getPickedForwardingRule()
    {
        return pickedForwardingRule == null ? null : Collections.unmodifiableList(pickedForwardingRule);
    }

    public void pickNode(Node node)
    {
        pickNode(Collections.singletonList(node));
    }

    public void pickLink(Link link)
    {
        pickLink(Collections.singletonList(link));
    }

    public void pickDemand(Demand demand)
    {
        pickDemand(Collections.singletonList(demand));
    }

    public void pickSRG(SharedRiskGroup srg)
    {
        pickSRG(Collections.singletonList(srg));
    }

    public void pickMulticastDemand(MulticastDemand multicastDemand)
    {
        pickMulticastDemand(Collections.singletonList(multicastDemand));
    }

    public void pickRoute(Route route)
    {
        pickRoute(Collections.singletonList(route));
    }

    public void pickMulticastTree(MulticastTree tree)
    {
        pickMulticastTree(Collections.singletonList(tree));
    }

    public void pickResource(Resource resource)
    {
        pickResource(Collections.singletonList(resource));
    }

    public void pickForwardingRule(Pair<Demand, Link> fr)
    {
        pickForwardingRule(Collections.singletonList(fr));
    }

    public void pickLayer(NetworkLayer pickedLayer)
    {
        resetPickedState();
        this.pickedElementType = NetworkElementType.LAYER;
        this.pickedForwardingRule = null;
        this.pickedElement = Arrays.asList(pickedLayer);
        pickTimeLineManager.addElement(this.getNetPlan(), pickedLayer);
    }

    public void pickDemand(List<Demand> pickedDemands)
    {
        resetPickedState();
        this.pickedElementType = NetworkElementType.DEMAND;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedDemands;
        if (pickedDemands.size() == 1) pickTimeLineManager.addElement(this.getNetPlan(), pickedDemands.get(0));
        
        for (Demand pickedDemand : pickedDemands)
        {
            final boolean isDemandLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedDemand.getLayer());
            final GUINode gnOrigin = getCanvasAssociatedGUINode(pickedDemand.getIngressNode(), pickedDemand.getLayer());
            final GUINode gnDestination = getCanvasAssociatedGUINode(pickedDemand.getEgressNode(), pickedDemand.getLayer());
            Pair<Set<Link>, Set<Link>> thisLayerPropagation = null;
            if (showInCanvasThisLayerPropagation && isDemandLayerVisibleInTheCanvas)
            {
                thisLayerPropagation = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic(false);
                final Set<Link> linksPrimary = thisLayerPropagation.getFirst();
                final Set<Link> linksBackup = thisLayerPropagation.getSecond();
                final Set<Link> linksPrimaryAndBackup = Sets.intersection(linksPrimary, linksBackup);
                drawColateralLinks(Sets.difference(linksPrimary, linksPrimaryAndBackup), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawColateralLinks(Sets.difference(linksBackup, linksPrimaryAndBackup), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
                drawColateralLinks(linksPrimaryAndBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
            }
            if (showInCanvasLowerLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1))
            {
                if (thisLayerPropagation == null)
                    thisLayerPropagation = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic(false);
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfoPrimary = getDownCoupling(thisLayerPropagation.getFirst());
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfoBackup = getDownCoupling(thisLayerPropagation.getSecond());
                final InterLayerPropagationGraph ipgPrimary = new InterLayerPropagationGraph(downLayerInfoPrimary.getFirst(), null, downLayerInfoPrimary.getSecond(), false, false);
                final InterLayerPropagationGraph ipgBackup = new InterLayerPropagationGraph(downLayerInfoBackup.getFirst(), null, downLayerInfoBackup.getSecond(), false, false);
                final Set<Link> linksPrimary = ipgPrimary.getLinksInGraph();
                final Set<Link> linksBackup = ipgBackup.getLinksInGraph();
                final Set<Link> linksPrimaryAndBackup = Sets.intersection(linksPrimary, linksBackup);
                final Set<Link> linksOnlyPrimary = Sets.difference(linksPrimary, linksPrimaryAndBackup);
                final Set<Link> linksOnlyBackup = Sets.difference(linksBackup, linksPrimaryAndBackup);
                drawColateralLinks(linksOnlyPrimary, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(linksOnlyPrimary, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawColateralLinks(linksOnlyBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
                drawDownPropagationInterLayerLinks(linksOnlyBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
                drawColateralLinks(linksPrimaryAndBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
                drawDownPropagationInterLayerLinks(linksPrimaryAndBackup, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
            }
            if (showInCanvasUpperLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1) && pickedDemand.isCoupled())
            {
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, Sets.newHashSet(pickedDemand.getCoupledLink()), null, true, false);
                drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            /* Picked link the last, so overrides the rest */
            if (isDemandLayerVisibleInTheCanvas)
            {
                gnOrigin.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gnDestination.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
            }
        }
        
    }

    public void pickSRG(List<SharedRiskGroup> pickedSRGs)
    {
        resetPickedState();
        this.pickedElementType = NetworkElementType.SRG;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedSRGs;
        if (pickedSRGs.size() == 1)pickTimeLineManager.addElement(this.getNetPlan(), pickedSRGs.get(0));

        for (SharedRiskGroup pickedSRG : pickedSRGs)
        {
            final Set<Link> allAffectedLinks = pickedSRG.getAffectedLinksAllLayers();
            Map<Link, Triple<Map<Demand, Set<Link>>, Map<Demand, Set<Link>>, Map<Pair<MulticastDemand, Node>, Set<Link>>>> thisLayerPropInfo = new HashMap<>();
            if (showInCanvasThisLayerPropagation)
            {
                for (Link link : allAffectedLinks)
                {
                    thisLayerPropInfo.put(link, link.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false));
                    final Set<Link> linksPrimary = thisLayerPropInfo.get(link).getFirst().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
                    final Set<Link> linksBackup = thisLayerPropInfo.get(link).getSecond().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
                    final Set<Link> linksMulticast = thisLayerPropInfo.get(link).getThird().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
                    drawColateralLinks(Sets.union(Sets.union(linksPrimary, linksBackup), linksMulticast), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
                }
            }
            if (showInCanvasLowerLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1))
            {
                final Set<Link> affectedCoupledLinks = allAffectedLinks.stream().filter(e -> e.isCoupled()).collect(Collectors.toSet());
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> couplingInfo = getDownCoupling(affectedCoupledLinks);
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(couplingInfo.getFirst(), null, couplingInfo.getSecond(), false, false);
                final Set<Link> lowerLayerLinks = ipg.getLinksInGraph();
                drawColateralLinks(lowerLayerLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
                drawDownPropagationInterLayerLinks(lowerLayerLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
            }
            if (showInCanvasUpperLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1))
            {
                final Set<Demand> demandsPrimaryAndBackup = new HashSet<>();
                final Set<Pair<MulticastDemand, Node>> demandsMulticast = new HashSet<>();
                for (Link link : allAffectedLinks)
                {
                    final Triple<Map<Demand, Set<Link>>, Map<Demand, Set<Link>>, Map<Pair<MulticastDemand, Node>, Set<Link>>> thisLinkInfo =
                            showInCanvasThisLayerPropagation ? thisLayerPropInfo.get(link) : link.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false);
                    demandsPrimaryAndBackup.addAll(Sets.union(thisLinkInfo.getFirst().keySet(), thisLinkInfo.getSecond().keySet()));
                    demandsMulticast.addAll(thisLinkInfo.getThird().keySet());
                }
                final Set<Link> coupledUpperLinks = getUpCoupling(demandsPrimaryAndBackup, demandsMulticast);
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, coupledUpperLinks, null, true, false);
                drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
                drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
            }
            /* Picked link the last, so overrides the rest */
            for (Link link : allAffectedLinks)
            {
                final GUILink gl = getCanvasAssociatedGUILink(link);
                if (gl == null) continue;
                gl.setHasArrow(true);
                setCurrentDefaultEdgeStroke (gl , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
                final Paint color = link.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED;
                gl.setEdgeDrawPaint(color);
                gl.setShownSeparated(true);
            }
            for (Node node : pickedSRG.getNodes())
            {
                for (GUINode gn : getCanvasVerticallyStackedGUINodes(node))
                {
                    gn.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_FAILED);
                    gn.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_FAILED);
                }
            }
        }
    }

    public void pickMulticastDemand(List<MulticastDemand> pickedDemands)
    {
        resetPickedState();
        this.pickedElementType = NetworkElementType.MULTICAST_DEMAND;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedDemands;
        if (pickedDemands.size() == 1) pickTimeLineManager.addElement(this.getNetPlan(), pickedDemands.get(0));

        for (MulticastDemand pickedDemand : pickedDemands)
        {
            final boolean isDemandLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedDemand.getLayer());
            final GUINode gnOrigin = getCanvasAssociatedGUINode(pickedDemand.getIngressNode(), pickedDemand.getLayer());
            Set<Link> linksThisLayer = null;
            for (Node egressNode : pickedDemand.getEgressNodes())
            {
                final GUINode gnDestination = getCanvasAssociatedGUINode(egressNode, pickedDemand.getLayer());
                if (showInCanvasThisLayerPropagation && isDemandLayerVisibleInTheCanvas)
                {
                    linksThisLayer = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic(egressNode, false);
                    drawColateralLinks(linksThisLayer, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (showInCanvasLowerLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1))
                {
                    if (linksThisLayer == null)
                        linksThisLayer = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic(egressNode, false);
                    final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfo = getDownCoupling(linksThisLayer);
                    final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downLayerInfo.getFirst(), null, downLayerInfo.getSecond(), false, false);
                    final Set<Link> linksLowerLayers = ipg.getLinksInGraph();
                    drawColateralLinks(linksLowerLayers, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                    drawDownPropagationInterLayerLinks(linksLowerLayers, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (showInCanvasUpperLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1) && pickedDemand.isCoupled())
                {
                    final Set<Link> upCoupledLink = getUpCoupling(null, Collections.singleton(Pair.of(pickedDemand, egressNode)));
                    final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, upCoupledLink, null, true, false);
                    drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                    drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                /* Picked link the last, so overrides the rest */
                if (isDemandLayerVisibleInTheCanvas)
                {
                    gnDestination.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                    gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                }
            }
            /* Picked link the last, so overrides the rest */
            if (isDemandLayerVisibleInTheCanvas)
            {
                gnOrigin.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
            }
        }
        	
    }

    public void pickRoute(List<Route> pickedRoutes)
    {
        resetPickedState();
        this.pickedElementType = NetworkElementType.ROUTE;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedRoutes;
        if (pickedRoutes.size() == 1) pickTimeLineManager.addElement(this.getNetPlan(), pickedRoutes.get(0));

        for (Route pickedRoute : pickedRoutes)
        {
            final boolean isRouteLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedRoute.getLayer());
            if (showInCanvasThisLayerPropagation && isRouteLayerVisibleInTheCanvas)
            {
                final List<Link> linksPrimary = pickedRoute.getSeqLinks();
                drawColateralLinks(linksPrimary, pickedRoute.isBackupRoute() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (showInCanvasLowerLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1))
            {
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downInfo = getDownCoupling(pickedRoute.getSeqLinks());
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downInfo.getFirst(), null, downInfo.getSecond(), false, false);
                drawColateralLinks(ipg.getLinksInGraph(), pickedRoute.isBackupRoute() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), pickedRoute.isBackupRoute() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (showInCanvasUpperLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1) && pickedRoute.getDemand().isCoupled())
            {
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, Sets.newHashSet(pickedRoute.getDemand().getCoupledLink()), null, true, false);
                drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            /* Picked link the last, so overrides the rest */
            if (isRouteLayerVisibleInTheCanvas)
            {
                final GUINode gnOrigin = getCanvasAssociatedGUINode(pickedRoute.getIngressNode(), pickedRoute.getLayer());
                final GUINode gnDestination = getCanvasAssociatedGUINode(pickedRoute.getEgressNode(), pickedRoute.getLayer());
                gnOrigin.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gnDestination.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
            }
        }
    }

    public void pickMulticastTree(List<MulticastTree> pickedTrees)
    {
        resetPickedState();
        this.pickedElementType = NetworkElementType.MULTICAST_TREE;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedTrees;
        if (pickedTrees.size() == 1) pickTimeLineManager.addElement(this.getNetPlan(), pickedTrees.get(0));

        for (MulticastTree pickedTree : pickedTrees)
        {
            final boolean isTreeLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedTree.getLayer());
            final GUINode gnOrigin = getCanvasAssociatedGUINode(pickedTree.getIngressNode(), pickedTree.getLayer());
            for (Node egressNode : pickedTree.getEgressNodes())
            {
                final GUINode gnDestination = getCanvasAssociatedGUINode(egressNode, pickedTree.getLayer());
                if (showInCanvasThisLayerPropagation && isTreeLayerVisibleInTheCanvas)
                {
                    final List<Link> linksPrimary = pickedTree.getSeqLinksToEgressNode(egressNode);
                    drawColateralLinks(linksPrimary, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (showInCanvasLowerLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1))
                {
                    final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downInfo = getDownCoupling(pickedTree.getSeqLinksToEgressNode(egressNode));
                    final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downInfo.getFirst(), null, downInfo.getSecond(), false, false);
                    drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                    drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (showInCanvasUpperLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1) && pickedTree.getMulticastDemand().isCoupled())
                {
                    final Set<Link> upperCoupledLink = getUpCoupling(null, Arrays.asList(Pair.of(pickedTree.getMulticastDemand(), egressNode)));
                    final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, upperCoupledLink, null, true, false);
                    drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                    drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                }
                if (isTreeLayerVisibleInTheCanvas)
                {
                    gnDestination.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                    gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                }
            }
    		/* Picked link the last, so overrides the rest */
            if (isTreeLayerVisibleInTheCanvas)
            {
                gnOrigin.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
            }
        }
    }

    public void pickLink(List<Link> pickedLinks)
    {
        resetPickedState();
        this.pickedElementType = NetworkElementType.LINK;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedLinks;
        if (pickedLinks.size() == 1) pickTimeLineManager.addElement(this.getNetPlan(), pickedLinks.get(0));

        for (Link pickedLink : pickedLinks)
        {
            final boolean isLinkLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedLink.getLayer());
            Triple<Map<Demand, Set<Link>>, Map<Demand, Set<Link>>, Map<Pair<MulticastDemand, Node>, Set<Link>>> thisLayerTraversalInfo = null;
            if (showInCanvasThisLayerPropagation && isLinkLayerVisibleInTheCanvas)
            {
                thisLayerTraversalInfo = pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false);
                final Set<Link> linksPrimary = thisLayerTraversalInfo.getFirst().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
                final Set<Link> linksBackup = thisLayerTraversalInfo.getSecond().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
                final Set<Link> linksMulticast = thisLayerTraversalInfo.getThird().values().stream().flatMap(set -> set.stream()).collect(Collectors.toSet());
                drawColateralLinks(Sets.union(Sets.union(linksPrimary, linksBackup), linksMulticast), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (showInCanvasLowerLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1) && pickedLink.isCoupled())
            {
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfo = getDownCoupling(Arrays.asList(pickedLink));
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(downLayerInfo.getFirst(), null, downLayerInfo.getSecond(), false, false);
                drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (showInCanvasUpperLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1))
            {
                if (thisLayerTraversalInfo == null)
                    thisLayerTraversalInfo = pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false);
                final Set<Demand> demandsPrimaryAndBackup = Sets.union(thisLayerTraversalInfo.getFirst().keySet(), thisLayerTraversalInfo.getSecond().keySet());
                final Set<Pair<MulticastDemand, Node>> mDemands = thisLayerTraversalInfo.getThird().keySet();
                final Set<Link> initialUpperLinks = getUpCoupling(demandsPrimaryAndBackup, mDemands);
                final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph(null, Sets.newHashSet(initialUpperLinks), null, true, false);
                drawColateralLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(ipg.getLinksInGraph(), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
    		/* Picked link the last, so overrides the rest */
            if (isLinkLayerVisibleInTheCanvas)
            {
                final GUILink gl = getCanvasAssociatedGUILink(pickedLink);
                gl.setHasArrow(true);
                setCurrentDefaultEdgeStroke (gl , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
                final Paint color = pickedLink.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED;
                gl.setEdgeDrawPaint(color);
                gl.setShownSeparated(true);
            }
        }
    }

    public void pickNode(List<Node> pickedNodes)
    {
        resetPickedState();
        this.pickedElementType = NetworkElementType.NODE;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedNodes;
        if (pickedNodes.size() == 1) pickTimeLineManager.addElement(this.getNetPlan(), pickedNodes.get(0));

        for (Node pickedNode : pickedNodes)
        {
            for (GUINode gn : getCanvasVerticallyStackedGUINodes(pickedNode))
            {
                gn.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_PICK);
                gn.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_PICK);
            }
            for (Link e : Sets.union(pickedNode.getOutgoingLinks(this.getNetPlan().getNetworkLayerDefault()), pickedNode.getIncomingLinks(this.getNetPlan().getNetworkLayerDefault())))
            {
                final GUILink gl = getCanvasAssociatedGUILink(e);
                gl.setShownSeparated(true);
                gl.setHasArrow(true);
            }
        }
    }

    public void pickResource(List<Resource> pickedResources)
    {
        resetPickedState();
        this.pickedElementType = NetworkElementType.RESOURCE;
        this.pickedForwardingRule = null;
        this.pickedElement = pickedResources;
        if (pickedResources.size() == 1) pickTimeLineManager.addElement(this.getNetPlan(), pickedResources.get(0));

        for (Resource pickedResource : pickedResources)
        {
            for (GUINode gn : getCanvasVerticallyStackedGUINodes(pickedResource.getHostNode()))
            {
                gn.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_RESOURCE);
                gn.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_RESOURCE);
            }
        }
    }

    public void pickForwardingRule(List<Pair<Demand, Link>> pickedFRs)
    {
        resetPickedState();
        this.pickedElementType = NetworkElementType.FORWARDING_RULE;
        this.pickedForwardingRule = pickedFRs;
        this.pickedElement = null;
        if (pickedFRs.size() == 1) pickTimeLineManager.addElement(this.getNetPlan(), pickedFRs.get(0));

        for (Pair<Demand,Link> pickedFR : pickedFRs)
        {
            final boolean isFRLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedFR.getFirst().getLayer());
            final Demand pickedDemand = pickedFR.getFirst();
            final Link pickedLink = pickedFR.getSecond();
            if (showInCanvasThisLayerPropagation && isFRLayerVisibleInTheCanvas)
            {
                final Triple<Map<Demand, Set<Link>>, Map<Demand, Set<Link>>, Map<Pair<MulticastDemand, Node>, Set<Link>>> triple =
                        pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false);
                final Set<Link> linksPrimary = triple.getFirst().get(pickedDemand);
                final Set<Link> linksBackup = triple.getSecond().get(pickedDemand);
                drawColateralLinks(Sets.union(linksPrimary, linksBackup), VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (showInCanvasLowerLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1) && pickedLink.isCoupled())
            {
                final Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> downLayerInfo = getDownCoupling(Arrays.asList(pickedLink));
                final InterLayerPropagationGraph ipgCausedByLink = new InterLayerPropagationGraph(downLayerInfo.getFirst(), null, downLayerInfo.getSecond(), false, false);
                final Set<Link> frPropagationLinks = ipgCausedByLink.getLinksInGraph();
                drawColateralLinks(frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
            if (showInCanvasUpperLayerPropagation && (this.getNetPlan().getNumberOfLayers() > 1) && pickedDemand.isCoupled())
            {
                final InterLayerPropagationGraph ipgCausedByDemand = new InterLayerPropagationGraph(null, Sets.newHashSet(pickedDemand.getCoupledLink()), null, true, false);
                final Set<Link> frPropagationLinks = ipgCausedByDemand.getLinksInGraph();
                drawColateralLinks(frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
                drawDownPropagationInterLayerLinks(frPropagationLinks, VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
            }
    		/* Picked link the last, so overrides the rest */
            if (isFRLayerVisibleInTheCanvas)
            {
                final GUILink gl = getCanvasAssociatedGUILink(pickedLink);
                gl.setHasArrow(true);
                setCurrentDefaultEdgeStroke (gl , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
                final Paint color = pickedLink.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED;
                gl.setEdgeDrawPaint(color);
                gl.setShownSeparated(true);
                gl.getOriginNode().setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gl.getOriginNode().setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
                gl.getDestinationNode().setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
                gl.getDestinationNode().setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
            }
        }
    }

    public void pickElement(NetworkElement es)
    {
        if (es instanceof Node) pickNode((Node) es);
        else if (es instanceof Link) pickLink((Link) es);
        else if (es instanceof Demand) pickDemand((Demand) es);
        else if (es instanceof Route) pickRoute((Route) es);
        else if (es instanceof MulticastDemand) pickMulticastDemand((MulticastDemand) es);
        else if (es instanceof MulticastTree) pickMulticastTree((MulticastTree) es);
        else if (es instanceof Resource) pickResource((Resource) es);
        else if (es instanceof SharedRiskGroup) pickSRG((SharedRiskGroup) es);
        else throw new RuntimeException();
    }

    public void pickElement(List<? extends NetworkElement> es)
    {
        if (es.get(0) instanceof Node) pickNode((List<Node>) es);
        else if (es.get(0) instanceof Link) pickLink((List<Link>) es);
        else if (es.get(0) instanceof Demand) pickDemand((List<Demand>) es);
        else if (es.get(0) instanceof Route) pickRoute((List<Route>) es);
        else if (es.get(0) instanceof MulticastDemand) pickMulticastDemand((List<MulticastDemand>) es);
        else if (es.get(0) instanceof MulticastTree) pickMulticastTree((List<MulticastTree>) es);
        else if (es.get(0) instanceof Resource) pickResource((List<Resource>) es);
        else if (es.get(0) instanceof SharedRiskGroup) pickSRG((List<SharedRiskGroup>) es);
        else throw new RuntimeException();
    }

    public void resetPickedState()
    {
        this.pickedElementType = null;
        this.pickedElement = null;
        this.pickedForwardingRule = null;

        for (GUINode n : getCanvasAllGUINodes())
        {
            n.setBorderPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR);
            n.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR);
        }
        for (GUILink e : getCanvasAllGUILinks(true, false))
        {
            e.setHasArrow(VisualizationConstants.DEFAULT_REGGUILINK_HASARROW);
            setCurrentDefaultEdgeStroke (e , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_ACTIVELAYER , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE);
            final boolean isDown = e.getAssociatedNetPlanLink().isDown();
            final Paint color = isDown ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR;
            e.setEdgeDrawPaint(color);
            e.setShownSeparated(isDown);
        }
        for (GUILink e : getCanvasAllGUILinks(false, true))
        {
            e.setHasArrow(VisualizationConstants.DEFAULT_INTRANODEGUILINK_HASARROW);
            setCurrentDefaultEdgeStroke (e , VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE , VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE);
            e.setEdgeDrawPaint(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR);
            e.setShownSeparated(false);
        }
    }

    private void drawDownPropagationInterLayerLinks(Set<Link> links, Paint color)
    {
        for (Link link : links)
        {
            final GUILink gl = getCanvasAssociatedGUILink(link);
            if (gl == null) continue;
            if (!link.isCoupled()) continue;
            final boolean isCoupledToDemand = link.getCoupledDemand() != null;
            final NetworkLayer upperLayer = link.getLayer();
            final NetworkLayer lowerLayer = isCoupledToDemand ? link.getCoupledDemand().getLayer() : link.getCoupledMulticastDemand().getLayer();
            if (!isLayerVisibleInCanvas(lowerLayer)) continue;
            for (GUILink interLayerLink : getCanvasIntraNodeGUILinkSequence(link.getOriginNode(), upperLayer, lowerLayer))
            {
                setCurrentDefaultEdgeStroke (interLayerLink , VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED , VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED);
                interLayerLink.setEdgeDrawPaint(color);
                interLayerLink.setShownSeparated(false);
                interLayerLink.setHasArrow(true);
            }
            for (GUILink interLayerLink : getCanvasIntraNodeGUILinkSequence(link.getDestinationNode(), lowerLayer, upperLayer))
            {
                setCurrentDefaultEdgeStroke (interLayerLink , VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED , VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED);
                interLayerLink.setEdgeDrawPaint(color);
                interLayerLink.setShownSeparated(false);
                interLayerLink.setHasArrow(true);
            }
        }
    }

    private void drawColateralLinks(Collection<Link> links, Paint colorIfNotFailedLink)
    {
        for (Link link : links)
        {
            final GUILink glColateral = getCanvasAssociatedGUILink(link);
            if (glColateral == null) continue;
            setCurrentDefaultEdgeStroke (glColateral , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALACTVELAYER , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALNONACTIVELAYER);
            final Paint color = link.isDown() ? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : colorIfNotFailedLink;
            glColateral.setEdgeDrawPaint(color);
            glColateral.setShownSeparated(true);
            glColateral.setHasArrow(true);
        }
    }

    private static Pair<Set<Demand>, Set<Pair<MulticastDemand, Node>>> getDownCoupling(Collection<Link> links)
    {
        final Set<Demand> res_1 = new HashSet<>();
        final Set<Pair<MulticastDemand, Node>> res_2 = new HashSet<>();
        for (Link link : links)
        {
            if (link.getCoupledDemand() != null) res_1.add(link.getCoupledDemand());
            else if (link.getCoupledMulticastDemand() != null)
                res_2.add(Pair.of(link.getCoupledMulticastDemand(), link.getDestinationNode()));
        }
        return Pair.of(res_1, res_2);

    }

    private static Set<Link> getUpCoupling(Collection<Demand> demands, Collection<Pair<MulticastDemand, Node>> mDemands)
    {
        final Set<Link> res = new HashSet<>();
        if (demands != null)
            for (Demand d : demands)
                if (d.isCoupled()) res.add(d.getCoupledLink());
        if (mDemands != null)
            for (Pair<MulticastDemand, Node> md : mDemands)
            {
                if (md.getFirst().isCoupled())
                    res.add(md.getFirst().getCoupledLinks().stream().filter(e -> e.getDestinationNode() == md.getSecond()).findFirst().get());
//    			System.out.println(md.getFirst().getCoupledLinks().stream().map(e->e.getDestinationNode()).collect(Collectors.toList()));
//    			System.out.println(md.getSecond());
            }
        return res;
    }

    public List<NetworkLayer> getCanvasLayersInVisualizationOrder(boolean includeNonVisible)
    {
        BidiMap<Integer, NetworkLayer> map = includeNonVisible ? new DualHashBidiMap<>(MapUtils.invertMap(visualizationSnapshot.getMapCanvasLayerVisualizationOrder())) : cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.inverseBidiMap();
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
        if (pickedElementType != null)
            if (pickedElement != null)
                this.pickElement(pickedElement);
            else
                this.pickForwardingRule(pickedForwardingRule);
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


    /**
     * @param showUpperLayerPropagation the showUpperLayerPropagation to set
     */
    public void setShowInCanvasUpperLayerPropagation(boolean showUpperLayerPropagation)
    {
        if (showUpperLayerPropagation == this.showInCanvasUpperLayerPropagation) return;
        this.showInCanvasUpperLayerPropagation = showUpperLayerPropagation;
        if (pickedElementType != null)
            if (pickedElement != null)
                this.pickElement(pickedElement);
            else
                this.pickForwardingRule(pickedForwardingRule);
    }

    /**
     * @param showThisLayerPropagation the showThisLayerPropagation to set
     */
    public void setShowInCanvasThisLayerPropagation(boolean showThisLayerPropagation)
    {
        if (showThisLayerPropagation == this.showInCanvasThisLayerPropagation) return;
        this.showInCanvasThisLayerPropagation = showThisLayerPropagation;
        if (pickedElementType != null)
            if (pickedElement != null)
                this.pickElement(pickedElement);
            else
                this.pickForwardingRule(pickedForwardingRule);
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
        return pickTimeLineManager.getPickNavigationBackElement();
    }

    public Object getPickNavigationForwardElement()
    {
        return pickTimeLineManager.getPickNavigationForwardElement();
    }

    public static void checkNpToVsConsistency(VisualizationState vs, NetPlan np)
    {
        if (vs.getNetPlan() != np)
            throw new RuntimeException("inputVs.currentNp:" + vs.getNetPlan().hashCode() + ", inputNp: " + np.hashCode());
        for (Node n : vs.nodesToHideInCanvasAsMandatedByUserInTable)
            if (n.getNetPlan() != np) throw new RuntimeException();
        for (Link e : vs.linksToHideInCanvasAsMandatedByUserInTable)
            if (e.getNetPlan() != np) throw new RuntimeException();
        for (NetworkLayer e : vs.visualizationSnapshot.getMapCanvasLayerVisualizationOrder().keySet())
            if (e.getNetPlan() != np) throw new RuntimeException();
        for (NetworkLayer e : vs.visualizationSnapshot.getMapCanvasLayerVisibility().keySet())
            if (e.getNetPlan() != np) throw new RuntimeException();
        for (NetworkLayer e : vs.visualizationSnapshot.getMapCanvasLinkVisibility().keySet())
            if (e.getNetPlan() != np) throw new RuntimeException();
        for (Node e : vs.cache_canvasIntraNodeGUILinks.keySet()) if (e.getNetPlan() != np) throw new RuntimeException();
        for (Set<GUILink> s : vs.cache_canvasIntraNodeGUILinks.values())
            for (GUILink e : s)
                if (e.getOriginNode().getAssociatedNode().getNetPlan() != np) throw new RuntimeException();
        for (Set<GUILink> s : vs.cache_canvasIntraNodeGUILinks.values())
            for (GUILink e : s)
                if (e.getDestinationNode().getAssociatedNode().getNetPlan() != np) throw new RuntimeException();
        for (Link e : vs.cache_canvasRegularLinkMap.keySet()) if (e.getNetPlan() != np) throw new RuntimeException();
        for (GUILink e : vs.cache_canvasRegularLinkMap.values())
            if (e.getAssociatedNetPlanLink().getNetPlan() != np) throw new RuntimeException();
        for (NetworkLayer e : vs.cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.keySet())
            if (e.getNetPlan() != np) throw new RuntimeException();
        for (Node e : vs.cache_mapNode2IntraNodeCanvasGUILinkMap.keySet())
            if (e.getNetPlan() != np) throw new RuntimeException();
        for (Map<Pair<Integer, Integer>, GUILink> map : vs.cache_mapNode2IntraNodeCanvasGUILinkMap.values())
            for (GUILink gl : map.values())
                if (gl.getOriginNode().getAssociatedNode().getNetPlan() != np) throw new RuntimeException();
        for (Map<Pair<Integer, Integer>, GUILink> map : vs.cache_mapNode2IntraNodeCanvasGUILinkMap.values())
            for (GUILink gl : map.values())
                if (gl.getDestinationNode().getAssociatedNode().getNetPlan() != np) throw new RuntimeException();
        for (Node e : vs.cache_mapNode2ListVerticallyStackedGUINodes.keySet())
            if (e.getNetPlan() != np) throw new RuntimeException();
        for (List<GUINode> list : vs.cache_mapNode2ListVerticallyStackedGUINodes.values())
            for (GUINode gn : list) if (gn.getAssociatedNode().getNetPlan() != np) throw new RuntimeException();
        if (vs.pickedElement != null) if (!vs.pickedElement.isEmpty()) if (vs.pickedElement.get(0).getNetPlan() != np) throw new RuntimeException();
        if (vs.pickedForwardingRule != null)
        	if (!vs.pickedForwardingRule.isEmpty())
        	{
        		if (vs.pickedForwardingRule.get(0).getFirst().getNetPlan() != np) throw new RuntimeException();
                if (vs.pickedForwardingRule.get(0).getSecond().getNetPlan() != np) throw new RuntimeException();
        	}
    }
    
    private static BasicStroke resizedBasicStroke (BasicStroke a , float multFactorSize)
    {
    	if (multFactorSize == 1) return a;
    	return new BasicStroke(a.getLineWidth() * multFactorSize, a.getEndCap(), a.getLineJoin(), a.getMiterLimit(), a.getDashArray(), a.getDashPhase());
    }

    private void setCurrentDefaultEdgeStroke (GUILink e , BasicStroke a , BasicStroke na)
    {
    	e.setEdgeStroke(resizedBasicStroke(a, linkWidthIncreaseFactorRespectToDefault), resizedBasicStroke(na, linkWidthIncreaseFactorRespectToDefault));
    }
}
