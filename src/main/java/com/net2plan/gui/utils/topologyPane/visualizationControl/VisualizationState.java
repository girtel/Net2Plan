package com.net2plan.gui.utils.topologyPane.visualizationControl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.GUINode;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import com.google.common.collect.Sets;
import com.net2plan.gui.utils.viewEditTopolTables.ITableRowFilter;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.InterLayerPropagationGraph;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.ImageUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import edu.uci.ics.jung.visualization.FourPassImageShaper;

public class VisualizationState
{
	private static Map<Triple<URL,Integer,Color>,Pair<ImageIcon,Shape>> databaseOfAlreadyReadIcons = new HashMap<> (); // for each url, height, and border color, an image

    private PickTimeLineManager pickTimeLineManager;
	private boolean auxTemporalVariable_doNoAddResetPickInUndo;

    private NetworkElementType pickedElementType;
    private NetworkElement pickedElementNotFR;
    private Pair<Demand, Link> pickedElementFR;

    private boolean showInCanvasNodeNames;
    private boolean showInCanvasLinkLabels;
    private boolean showInCanvasLinksInNonActiveLayer;
    private boolean showInCanvasInterLayerLinks;
    private boolean showInCanvasLowerLayerPropagation;
    private boolean showInCanvasUpperLayerPropagation;
    private boolean showInCanvasThisLayerPropagation;
    private ITableRowFilter tableRowFilter;

    private boolean showInCanvasNonConnectedNodes;
    private int interLayerSpaceInPixels;
    private NetPlan currentNp;
    private Set<Node> nodesToHideInCanvasAsMandatedByUserInTable;
    private Set<Link> linksToHideInCanvasAsMandatedByUserInTable;

    /* This only can be changed calling to rebuild */
    private BidiMap<NetworkLayer, Integer> mapLayer2VisualizationOrderInCanvas; // as many entries as layers
    private Map<NetworkLayer, Boolean> layerVisibilityInCanvasMap;
    private Map<NetworkLayer, Boolean> mapShowInCanvasLayerLinks;

    /* These need is recomputed inside a rebuild */
    private Map<Node, Set<GUILink>> cache_canvasIntraNodeGUILinks;
    private Map<Link, GUILink> cache_canvasRegularLinkMap;
    private BidiMap<NetworkLayer, Integer> cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible; // as many elements as visible layers
    private Map<Node, Map<Pair<Integer, Integer>, GUILink>> cache_mapNode2IntraNodeCanvasGUILinkMap; // integers are orders of REAL VISIBLE LAYERS
    private Map<Node, List<GUINode>> cache_mapNode2ListVerticallyStackedGUINodes;

    public NetPlan getNetPlan() { return currentNp; }

	public VisualizationState(NetPlan currentNp, BidiMap<NetworkLayer, Integer> mapLayer2VisualizationOrder, Map<NetworkLayer,Boolean> layerVisibilityMap , int maxSizePickUndoList)
    {
        this.currentNp = currentNp;
        this.showInCanvasNodeNames = false;
        this.showInCanvasLinkLabels = false;
        this.showInCanvasLinksInNonActiveLayer = true;
        this.showInCanvasInterLayerLinks = true;
        this.showInCanvasNonConnectedNodes = true;
        this.showInCanvasLowerLayerPropagation = true;
        this.showInCanvasUpperLayerPropagation = true;
        this.showInCanvasThisLayerPropagation = true;
        this.nodesToHideInCanvasAsMandatedByUserInTable = new HashSet<>();
        this.linksToHideInCanvasAsMandatedByUserInTable = new HashSet<>();
        this.interLayerSpaceInPixels = 50;
        this.tableRowFilter = null;
        this.pickTimeLineManager = new PickTimeLineManager(maxSizePickUndoList);
        this.mapShowInCanvasLayerLinks = currentNp.getNetworkLayers().stream().collect(Collectors.toMap(layer -> layer, layer -> true));

    	pickTimeLineManager.updatePickUndoList_newPickOrPickReset (currentNp); // add a no pick, this is never removed
    	setCanvasLayerVisibilityAndOrder(currentNp ,mapLayer2VisualizationOrder , layerVisibilityMap);
    }

	public ITableRowFilter getTableRowFilter () { return tableRowFilter; }

	public void updateTableRowFilter (ITableRowFilter tableRowFilterToApply)
	{
		if (tableRowFilterToApply == null) { this.tableRowFilter = null; return; }
		if (this.tableRowFilter == null) { this.tableRowFilter = tableRowFilterToApply; return; }
		this.tableRowFilter.recomputeApplyingShowIf_ThisAndThat(tableRowFilterToApply);
	}

    public boolean isVisibleInCanvas(GUINode gn)
    {
        final Node n = gn.getAssociatedNetPlanNode();
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
        	final Node node = gl.getOriginNode().getAssociatedNetPlanNode();
        	final NetworkLayer originLayer = gl.getOriginNode().getLayer();
        	final NetworkLayer destinationLayer = gl.getDestinationNode().getLayer();
        	final int originIndexInVisualization = getCanvasVisualizationOrderRemovingNonVisible(originLayer);
        	final int destinationIndexInVisualization = getCanvasVisualizationOrderRemovingNonVisible(destinationLayer);
        	final int lowerVIndex = originIndexInVisualization < destinationIndexInVisualization? originIndexInVisualization  : destinationIndexInVisualization;
        	final int upperVIndex = originIndexInVisualization > destinationIndexInVisualization? originIndexInVisualization  : destinationIndexInVisualization;
        	cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(gl.getOriginNode());
        	boolean atLeastOneLowerLayerVisible = false;
        	for (int vIndex = 0 ; vIndex <= lowerVIndex ; vIndex ++)
        		if (isVisibleInCanvas(getCanvasAssociatedGUINode(node , getCanvasNetworkLayerAtVisualizationOrderRemovingNonVisible(vIndex))))
        		{
        			atLeastOneLowerLayerVisible = true;
        			break;
        		}
        	if (!atLeastOneLowerLayerVisible) return false;
        	boolean atLeastOneUpperLayerVisible = false;
        	for (int vIndex = upperVIndex ; vIndex < getCanvasNumberOfVisibleLayers() ; vIndex ++)
        		if (isVisibleInCanvas(getCanvasAssociatedGUINode(node , getCanvasNetworkLayerAtVisualizationOrderRemovingNonVisible(vIndex))))
        		{
        			atLeastOneUpperLayerVisible = true;
        			break;
        		}
        	return atLeastOneUpperLayerVisible;
        }
        else
        {
            final Link e = gl.getAssociatedNetPlanLink();

            if (!mapShowInCanvasLayerLinks.get(e.getLayer())) return false;
            if (linksToHideInCanvasAsMandatedByUserInTable.contains(e)) return false;
            final boolean inActiveLayer = e.getLayer() == currentNp.getNetworkLayerDefault();
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
        return currentNp.isModifiable();
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
        if (from.getNetPlan() != currentNp) throw new RuntimeException("Bad");
        if (to.getNetPlan() != currentNp) throw new RuntimeException("Bad");
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

    /** Implicitly it produces a reset of the picked state
     * @param newCurrentNetPlan
     * @param newLayerVisiblityOrderMap
     * @param newLayerVisibilityMap
     */
    public void setCanvasLayerVisibilityAndOrder (NetPlan newCurrentNetPlan , BidiMap<NetworkLayer,Integer> newLayerVisiblityOrderMap,
            Map<NetworkLayer,Boolean> newLayerVisibilityMap)
    {
        if (newCurrentNetPlan == null) throw new RuntimeException("Trying to update an empty topology");
        final boolean netPlanChanged = (this.currentNp != newCurrentNetPlan);

        this.currentNp = newCurrentNetPlan;

        if (netPlanChanged)
        {
        	tableRowFilter = null;
        }
        
        /* implicitly we restart the picking state */
        this.pickedElementType = null;
        this.pickedElementNotFR = null;
        this.pickedElementFR = null;

        if (newLayerVisiblityOrderMap != null)
        	this.mapLayer2VisualizationOrderInCanvas = new DualHashBidiMap<>(newLayerVisiblityOrderMap);
        if (newLayerVisibilityMap != null)
        	this.layerVisibilityInCanvasMap = new HashMap<> (newLayerVisibilityMap);

        if (!mapLayer2VisualizationOrderInCanvas.keySet().equals(new HashSet<>(currentNp.getNetworkLayers())))
            throw new RuntimeException();
        if (!this.layerVisibilityInCanvasMap.keySet().equals(new HashSet<>(currentNp.getNetworkLayers())))
            throw new RuntimeException();

		/* Update the interlayer space */
//        this.interLayerSpaceInPixels = 50; //getDefaultVerticalDistanceForInterLayers();

        if (netPlanChanged)
        {
            nodesToHideInCanvasAsMandatedByUserInTable = new HashSet<>();
            linksToHideInCanvasAsMandatedByUserInTable = new HashSet<>();

            // Set all layer links as visible when loading a new topology.
            this.mapShowInCanvasLayerLinks = currentNp.getNetworkLayers().stream().collect(Collectors.toMap(layer -> layer, layer -> true));
        }
        this.cache_canvasIntraNodeGUILinks = new HashMap<>();
        this.cache_canvasRegularLinkMap = new HashMap<>();
        this.cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible = new DualHashBidiMap<>();
        this.cache_mapNode2IntraNodeCanvasGUILinkMap = new HashMap<>();
        this.cache_mapNode2ListVerticallyStackedGUINodes = new HashMap<>();
        for (int layerVisualizationOrderIncludingNonVisible = 0; layerVisualizationOrderIncludingNonVisible < currentNp.getNumberOfLayers(); layerVisualizationOrderIncludingNonVisible++)
        {
            final NetworkLayer layer = mapLayer2VisualizationOrderInCanvas.inverseBidiMap().get(layerVisualizationOrderIncludingNonVisible);
            if (isLayerVisibleInCanvas(layer))
                cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.put(layer, cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.size());
        }
        for (Node n : currentNp.getNodes())
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
                final GUINode gn = new GUINode(n, newLayer);
                guiNodesThisNode.add(gn);
                if (trueVisualizationOrderIndex > 0)
                {
                    final GUINode lowerLayerGNode = guiNodesThisNode.get(trueVisualizationOrderIndex - 1);
                    final GUINode upperLayerGNode = guiNodesThisNode.get(trueVisualizationOrderIndex);
                    if (upperLayerGNode != gn) throw new RuntimeException();
                    final GUILink glLowerToUpper = new GUILink(null, lowerLayerGNode, gn);
                    final GUILink glUpperToLower = new GUILink(null, gn, lowerLayerGNode);
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
            for (Link e : currentNp.getLinks(layer))
            {
                final GUINode gn1 = cache_mapNode2ListVerticallyStackedGUINodes.get(e.getOriginNode()).get(trueVisualizationOrderIndex);
                final GUINode gn2 = cache_mapNode2ListVerticallyStackedGUINodes.get(e.getDestinationNode()).get(trueVisualizationOrderIndex);
                final GUILink gl1 = new GUILink(e, gn1, gn2);
                cache_canvasRegularLinkMap.put(e, gl1);
            }
        }
    }

    private void checkCacheConsistency()
    {
        for (Node n : currentNp.getNodes())
        {
            assertTrue(cache_canvasIntraNodeGUILinks.get(n) != null);
            assertTrue(cache_mapNode2IntraNodeCanvasGUILinkMap.get(n) != null);
            assertTrue(cache_mapNode2ListVerticallyStackedGUINodes.get(n) != null);
            for (Entry<Pair<Integer, Integer>, GUILink> entry : cache_mapNode2IntraNodeCanvasGUILinkMap.get(n).entrySet())
            {
                final int fromLayer = entry.getKey().getFirst();
                final int toLayer = entry.getKey().getSecond();
                final GUILink gl = entry.getValue();
                assertTrue(gl.isIntraNodeLink());
                assertTrue(gl.getOriginNode().getAssociatedNetPlanNode() == n);
                assertTrue(getCanvasVisualizationOrderRemovingNonVisible(gl.getOriginNode().getLayer()) == fromLayer);
                assertTrue(getCanvasVisualizationOrderRemovingNonVisible(gl.getDestinationNode().getLayer()) == toLayer);
            }
            assertEquals(new HashSet<>(cache_mapNode2IntraNodeCanvasGUILinkMap.get(n).values()), cache_canvasIntraNodeGUILinks.get(n));
            for (GUILink gl : cache_canvasIntraNodeGUILinks.get(n))
            {
                assertTrue(gl.isIntraNodeLink());
                assertEquals(gl.getOriginNode().getAssociatedNetPlanNode(), n);
                assertEquals(gl.getDestinationNode().getAssociatedNetPlanNode(), n);
            }
            assertEquals(cache_mapNode2ListVerticallyStackedGUINodes.get(n).size(), getCanvasNumberOfVisibleLayers());
            int indexLayer = 0;
            for (GUINode gn : cache_mapNode2ListVerticallyStackedGUINodes.get(n))
            {
                assertEquals(gn.getLayer(), cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.inverseBidiMap().get(indexLayer));
                assertEquals(getCanvasVisualizationOrderRemovingNonVisible(gn.getLayer()), indexLayer++);
                assertEquals(gn.getAssociatedNetPlanNode(), n);
            }
        }
//
//		for (NetworkLayer layer : currentNp.getNetworkLayers())
//		{
//			if (cache_layer2VLayerMap.get(layer) == null) 
//				for (VisualizationLayer vl : vLayers) 
//					assertTrue (!vl.npLayersToShow.contains(layer));
//			if (cache_layer2VLayerMap.get(layer) != null) 
//				for (VisualizationLayer vl : vLayers) 
//					if (vl == cache_layer2VLayerMap.get(layer))
//						assertTrue (vl.npLayersToShow.contains(layer));
//					else
//						assertTrue (!vl.npLayersToShow.contains(layer));
//			if (cache_layer2VLayerMap.get(layer) != null)
//				for (Link e : currentNp.getLinks(layer))
//				{
//					final GUILink gl = regularLinkMap.get(e);
//					assertTrue (gl != null);
//					assertEquals (gl.getAssociatedNetPlanLink() , e);
//					assertTrue (cache_layer2VLayerMap.get(layer).guiIntraLayerLinks.contains(gl));
//				}
//		}
//		
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
        for (GUINode gn : getCanvasAllGUINodes())
                gn.setIconHeightInNonActiveLayer(gn.getIconHeightInNotActiveLayer() * VisualizationConstants.SCALE_OUT);
    }

    public void increaseCanvasNodeSizeAll()
    {
        for (GUINode gn : getCanvasAllGUINodes())
            gn.setIconHeightInNonActiveLayer(gn.getIconHeightInNotActiveLayer() * VisualizationConstants.SCALE_IN);
    }

    public int getCanvasNumberOfVisibleLayers()
    {
        return cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.size();
    }

//	public void setVisualizationLayers (List<List<NetworkLayer>> listOfLayersPerVL , NetPlan netPlan)
//	{
//		if (listOfLayersPerVL.isEmpty()) throw new Net2PlanException ("At least one visualization layer is needed");
//		Set<NetworkLayer> alreadyAppearingLayer = new HashSet<> ();
//		for (List<NetworkLayer> layers : listOfLayersPerVL)
//		{	
//			if (layers.isEmpty()) throw new Net2PlanException ("A visualization layer cannot be empty");
//			for (NetworkLayer layer : layers)
//			{
//				if (layer.getNetPlan() != netPlan) throw new RuntimeException ("Bad");
//				if (alreadyAppearingLayer.contains(layer)) throw new Net2PlanException ("A layer cannot belong to more than one visualization layer");
//				alreadyAppearingLayer.add(layer);
//			}
//		}
//		this.currentNp = netPlan;
//		this.vLayers.clear();
//		for (List<NetworkLayer> layers : listOfLayersPerVL)
//			vLayers.add(new VisualizationLayer(layers, this , vLayers.size()));
//
//		this.cache_layer2VLayerMap = new HashMap<> (); 
//		for (VisualizationLayer visualizationLayer : vLayers) 
//			for (NetworkLayer layer : visualizationLayer.npLayersToShow) 
//				cache_layer2VLayerMap.put(layer , visualizationLayer);
////		for (VisualizationLayer visualizationLayer : vLayers) 
////			visualizationLayer.updateGUINodeAndGUILinks();
//	}

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

    public void setMandatedByTheUserToBeHiddenInCanvas(Node n, boolean shouldBeHidden)
    {
        if (shouldBeHidden) nodesToHideInCanvasAsMandatedByUserInTable.add(n);
        else nodesToHideInCanvasAsMandatedByUserInTable.remove(n);
    }

    public boolean isMandatedByTheUserToBeHiddenInCanvas(Node n)
    {
        return nodesToHideInCanvasAsMandatedByUserInTable.contains(n);
    }

    public void setMandatedByTheUserToBeHiddenInCanvas(Link e, boolean shouldBeHiden)
    {
        if (shouldBeHiden) linksToHideInCanvasAsMandatedByUserInTable.add(e);
        else linksToHideInCanvasAsMandatedByUserInTable.remove(e);
    }

    public boolean isMandatedByTheUserToBeHiddenInCanvas(Link e)
    {
        return linksToHideInCanvasAsMandatedByUserInTable.contains(e);
    }

    /* Everything to its default color, shape. Separated nodes, are set together again. Visibility state is unchanged */
//    public void resetColorAndShapeState()
//    {
//        for (GUINode n : getAllGUINodes())
//        {
//            n.setFont(DEFAULT_GUINODE_FONT);
//            n.setDrawPaint(DEFAULT_GUINODE_DRAWCOLOR);
//            n.setFillPaint(DEFAULT_GUINODE_FILLCOLOR);
//            n.setShape(DEFAULT_GUINODE_SHAPE);
//            n.setShapeSize(DEFAULT_GUINODE_SHAPESIZE);
//        }
//        for (GUILink e : getAllGUILinks(true, false))
//        {
//            e.setHasArrow(DEFAULT_REGGUILINK_HASARROW);
//            e.setArrowStroke(DEFAULT_REGGUILINK_EDGESTROKE_ACTIVELAYER , DEFAULT_REGGUILINK_EDGESTROKE);
//            e.setEdgeStroke(DEFAULT_REGGUILINK_EDGESTROKE_ACTIVELAYER , DEFAULT_REGGUILINK_EDGESTROKE);
//            e.setArrowDrawPaint(DEFAULT_REGGUILINK_EDGECOLOR);
//            e.setArrowFillPaint(DEFAULT_REGGUILINK_EDGECOLOR);
//            e.setEdgeDrawPaint(DEFAULT_REGGUILINK_EDGECOLOR);
//            e.setShownSeparated(false);
//        }
//        for (GUILink e : getAllGUILinks(false, true))
//        {
//            e.setHasArrow(DEFAULT_INTRANODEGUILINK_HASARROW);
//            e.setArrowStroke(DEFAULT_INTRANODEGUILINK_EDGESTROKE_ACTIVE , DEFAULT_INTRANODEGUILINK_EDGESTROKE);
//            e.setEdgeStroke(DEFAULT_INTRANODEGUILINK_EDGESTROKE_ACTIVE , DEFAULT_INTRANODEGUILINK_EDGESTROKE);
//            e.setArrowDrawPaint(DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR);
//            e.setArrowFillPaint(DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR);
//            e.setEdgeDrawPaint(DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR);
//            e.setShownSeparated(false);
//        }
//    }

    public Set<GUILink> getCanvasAllGUILinks(boolean includeRegularLinks, boolean includeInterLayerLinks)
    {
        Set<GUILink> res = new HashSet<>();
        if (includeRegularLinks) res.addAll(cache_canvasRegularLinkMap.values());
        if (includeInterLayerLinks) for (Node n : currentNp.getNodes()) res.addAll(this.cache_canvasIntraNodeGUILinks.get(n));
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
        if (currentNp.getNumberOfNodes() == 0) return 1.0;
        final int numVisibleLayers = getCanvasNumberOfVisibleLayers() == 0 ? currentNp.getNumberOfLayers() : getCanvasNumberOfVisibleLayers();
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (Node n : currentNp.getNodes())
        {
            final double y = n.getXYPositionMap().getY();
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        if ((maxY - minY < 1e-6)) return Math.abs(maxY) / (30 * numVisibleLayers);
        return (maxY - minY) / (30 * numVisibleLayers);
    }


    /** To call when the topology has new/has removed any link or node, but keeping the same layers.
     * The topology is remade, which involves implicitly a reset of the view
     */
    public void recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals ()
    {
    	this.setCanvasLayerVisibilityAndOrder(this.currentNp , null , null);
    }

    public void setCanvasLayerVisibility(final NetworkLayer layer, final boolean isVisible)
    {
    	if (!this.currentNp.getNetworkLayers().contains(layer)) throw new RuntimeException ();
    	BidiMap<NetworkLayer,Integer> new_layerVisiblityOrderMap = new DualHashBidiMap<> (this.mapLayer2VisualizationOrderInCanvas);
    	Map<NetworkLayer,Boolean> new_layerVisibilityMap = new HashMap<> (this.layerVisibilityInCanvasMap);
    	new_layerVisibilityMap.put(layer,isVisible);
    	setCanvasLayerVisibilityAndOrder(this.currentNp , new_layerVisiblityOrderMap , new_layerVisibilityMap);
    }

    public boolean isLayerVisibleInCanvas(final NetworkLayer layer)
    {
        return layerVisibilityInCanvasMap.get(layer);
    }

    public void setLayerLinksVisibilityInCanvas(final NetworkLayer layer, final boolean showLinks)
    {
    	if (!this.currentNp.getNetworkLayers().contains(layer)) throw new RuntimeException ();
        mapShowInCanvasLayerLinks.put(layer, showLinks);
    }

    public boolean isCanvasLayerLinksShown(final NetworkLayer layer)
    {
        return mapShowInCanvasLayerLinks.get(layer);
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
        if (visualizationOrder >= currentNp.getNumberOfLayers()) throw new RuntimeException("");
        return mapLayer2VisualizationOrderInCanvas.inverseBidiMap().get(visualizationOrder);
    }

    public int getCanvasVisualizationOrderRemovingNonVisible(NetworkLayer layer)
    {
        Integer res = cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.get(layer);
        if (res == null) throw new RuntimeException("");
        return res;
    }

    public int getCanvasVisualizationOrderNotRemovingNonVisible(NetworkLayer layer)
    {
        Integer res = mapLayer2VisualizationOrderInCanvas.get(layer);
        if (res == null) throw new RuntimeException("");
        return res;
    }

    public static Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer,Boolean>> generateCanvasDefaultVisualizationLayerInfo(NetPlan np)
    {
        final BidiMap<NetworkLayer, Integer> res_1 = new DualHashBidiMap<>();
        final Map<NetworkLayer,Boolean> res_2 = new HashMap<>();

        for (NetworkLayer layer : np.getNetworkLayers())
        {
        	res_1.put (layer,res_1.size());
        	res_2.put (layer , true);
        }
        return Pair.of(res_1, res_2);
    }

    public Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer,Boolean>> suggestCanvasUpdatedVisualizationLayerInfoForNewDesign (Set<NetworkLayer> newNetworkLayers)
    {
    	final Map<NetworkLayer,Boolean> oldLayerVisibilityMap = getCanvasLayerVisibilityMap();
    	final BidiMap<NetworkLayer,Integer> oldLayerOrderMap = getCanvasLayerOrderIndexMap(true);
    	final Map<NetworkLayer,Boolean> newLayerVisibilityMap = new HashMap<> ();
    	final BidiMap<NetworkLayer,Integer> newLayerOrderMap = new DualHashBidiMap<>();
    	for (int oldVisibilityOrderIndex = 0; oldVisibilityOrderIndex < oldLayerOrderMap.size() ; oldVisibilityOrderIndex ++)
    	{
    		final NetworkLayer oldLayer = oldLayerOrderMap.inverseBidiMap().get(oldVisibilityOrderIndex);
    		if (newNetworkLayers.contains(oldLayer))
    		{
    			newLayerOrderMap.put(oldLayer , newLayerVisibilityMap.size());
    			newLayerVisibilityMap.put(oldLayer , oldLayerVisibilityMap.get(oldLayer));
    		}
    	}
    	final Set<NetworkLayer> newLayersNotExistingBefore = Sets.difference(newNetworkLayers , oldLayerVisibilityMap.keySet());
    	for (NetworkLayer newLayer : newLayersNotExistingBefore)
    	{
			newLayerOrderMap.put(newLayer , newLayerVisibilityMap.size());
			newLayerVisibilityMap.put(newLayer , true); // new layers always visible
    	}
    	return Pair.of(newLayerOrderMap , newLayerVisibilityMap);
    }

    public boolean isElementPicked() { return pickedElementType != null; }

    public NetworkElementType getPickedElementType () { return pickedElementType; }

    public NetworkElement getPickedNetworkElement()
    {
        return pickedElementNotFR;
    }

    public Pair<Demand, Link> getPickedForwardingRule()
    {
        return pickedElementFR;
    }
    
    public void pickLayer (NetworkLayer pickedLayer)
    {
    	auxTemporalVariable_doNoAddResetPickInUndo = true;
   		resetPickedState();
    	auxTemporalVariable_doNoAddResetPickInUndo = false;
        this.pickedElementType = NetworkElementType.LAYER;
        this.pickedElementFR = null;
        this.pickedElementNotFR = pickedLayer;
    	pickTimeLineManager.pickLayer(currentNp, pickedLayer);
   	}
    
    public void pickDemand (Demand pickedDemand)
    {
    	auxTemporalVariable_doNoAddResetPickInUndo = true;
   		resetPickedState();
    	auxTemporalVariable_doNoAddResetPickInUndo = false;
        this.pickedElementType = NetworkElementType.DEMAND;
        this.pickedElementFR = null;
        this.pickedElementNotFR = pickedDemand;
        pickTimeLineManager.pickDemand(currentNp, pickedDemand);

		final boolean isDemandLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedDemand.getLayer());
    	final GUINode gnOrigin = getCanvasAssociatedGUINode(pickedDemand.getIngressNode() , pickedDemand.getLayer());
		final GUINode gnDestination = getCanvasAssociatedGUINode(pickedDemand.getEgressNode() , pickedDemand.getLayer());
		Pair<Set<Link>,Set<Link>> thisLayerPropagation = null;
		if (showInCanvasThisLayerPropagation && isDemandLayerVisibleInTheCanvas)
		{
    		thisLayerPropagation = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic(false);
    		final Set<Link> linksPrimary = thisLayerPropagation.getFirst();
    		final Set<Link> linksBackup = thisLayerPropagation.getSecond();
    		final Set<Link> linksPrimaryAndBackup = Sets.intersection(linksPrimary , linksBackup);
    		drawColateralLinks (Sets.difference(linksPrimary , linksPrimaryAndBackup) , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
    		drawColateralLinks (Sets.difference(linksBackup , linksPrimaryAndBackup) , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
    		drawColateralLinks (linksPrimaryAndBackup , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
		}
		if (showInCanvasLowerLayerPropagation && (currentNp.getNumberOfLayers() > 1))
		{
			if (thisLayerPropagation == null) thisLayerPropagation = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic(false);
			final Pair<Set<Demand>,Set<Pair<MulticastDemand,Node>>> downLayerInfoPrimary = getDownCoupling(thisLayerPropagation.getFirst()); 
			final Pair<Set<Demand>,Set<Pair<MulticastDemand,Node>>> downLayerInfoBackup = getDownCoupling(thisLayerPropagation.getSecond()); 
			final InterLayerPropagationGraph ipgPrimary = new InterLayerPropagationGraph (downLayerInfoPrimary.getFirst() , null , downLayerInfoPrimary.getSecond() , false , false);
			final InterLayerPropagationGraph ipgBackup = new InterLayerPropagationGraph (downLayerInfoBackup.getFirst() , null , downLayerInfoBackup.getSecond() , false , false);
			final Set<Link> linksPrimary = ipgPrimary.getLinksInGraph();
			final Set<Link> linksBackup = ipgBackup.getLinksInGraph();
			final Set<Link> linksPrimaryAndBackup = Sets.intersection(linksPrimary , linksBackup);
			final Set<Link> linksOnlyPrimary = Sets.difference(linksPrimary , linksPrimaryAndBackup);
			final Set<Link> linksOnlyBackup = Sets.difference(linksBackup , linksPrimaryAndBackup);
			drawColateralLinks (linksOnlyPrimary , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
			drawDownPropagationInterLayerLinks (linksOnlyPrimary , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
			drawColateralLinks (linksOnlyBackup , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
			drawDownPropagationInterLayerLinks (linksOnlyBackup , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP);
			drawColateralLinks (linksPrimaryAndBackup , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
			drawDownPropagationInterLayerLinks (linksPrimaryAndBackup , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY);
		}
		if (showInCanvasUpperLayerPropagation && (currentNp.getNumberOfLayers() > 1) && pickedDemand.isCoupled())
		{
			final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (null , Sets.newHashSet(pickedDemand.getCoupledLink()) , null , true , false);
			drawColateralLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
			drawDownPropagationInterLayerLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
		}
		/* Picked link the last, so overrides the rest */
		if (isDemandLayerVisibleInTheCanvas)
		{
	        gnOrigin.setDrawPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
	        gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
	        gnDestination.setDrawPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
	        gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
		}
    }
    
    public void pickSRG (SharedRiskGroup pickedSRG)
    {
    	auxTemporalVariable_doNoAddResetPickInUndo = true;
   		resetPickedState();
    	auxTemporalVariable_doNoAddResetPickInUndo = false;
        this.pickedElementType = NetworkElementType.SRG;
        this.pickedElementFR = null;
        this.pickedElementNotFR = pickedSRG;
    	pickTimeLineManager.pickSRG(currentNp, pickedSRG);

    	final Set<Link> allAffectedLinks = pickedSRG.getAffectedLinksAllLayers();
    	Map<Link,Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>>> thisLayerPropInfo = new HashMap<> (); 
		if (showInCanvasThisLayerPropagation)
		{
			for (Link link : allAffectedLinks)
			{
				thisLayerPropInfo.put(link , link.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false));
	    		final Set<Link> linksPrimary = thisLayerPropInfo.get(link).getFirst().values().stream().flatMap(set->set.stream()).collect (Collectors.toSet());
	    		final Set<Link> linksBackup = thisLayerPropInfo.get(link).getSecond().values().stream().flatMap(set->set.stream()).collect (Collectors.toSet());
	    		final Set<Link> linksMulticast = thisLayerPropInfo.get(link).getThird().values().stream().flatMap(set->set.stream()).collect (Collectors.toSet());
	    		drawColateralLinks (Sets.union(Sets.union(linksPrimary , linksBackup) , linksMulticast) , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
			}
		}
		if (showInCanvasLowerLayerPropagation && (currentNp.getNumberOfLayers() > 1))
		{
			final Set<Link> affectedCoupledLinks = allAffectedLinks.stream().filter(e->e.isCoupled()).collect(Collectors.toSet());
			final Pair<Set<Demand>,Set<Pair<MulticastDemand,Node>>> couplingInfo = getDownCoupling(affectedCoupledLinks);
			final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (couplingInfo.getFirst()  , null , couplingInfo.getSecond() , false , false);
			final Set<Link> lowerLayerLinks = ipg.getLinksInGraph(); 
			drawColateralLinks (lowerLayerLinks , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
			drawDownPropagationInterLayerLinks (lowerLayerLinks , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
		}
		if (showInCanvasUpperLayerPropagation && (currentNp.getNumberOfLayers() > 1))
		{
    		final Set<Demand> demandsPrimaryAndBackup = new HashSet<> ();
    		final Set<Pair<MulticastDemand,Node>> demandsMulticast = new HashSet<> ();
			for (Link link : allAffectedLinks)
			{
				final Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> thisLinkInfo = 
						showInCanvasThisLayerPropagation?  thisLayerPropInfo.get(link) : link.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false);
	    		demandsPrimaryAndBackup.addAll(Sets.union(thisLinkInfo.getFirst().keySet() , thisLinkInfo.getSecond().keySet()));
	    		demandsMulticast.addAll(thisLinkInfo.getThird().keySet());
			}
    		final Set<Link> coupledUpperLinks = getUpCoupling(demandsPrimaryAndBackup , demandsMulticast);
			final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (null , coupledUpperLinks , null , true , false);
			drawColateralLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
			drawDownPropagationInterLayerLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES);
		}
		/* Picked link the last, so overrides the rest */
		for (Link link : allAffectedLinks)
		{
			final GUILink gl = getCanvasAssociatedGUILink(link);
			if (gl == null) continue;
			gl.setHasArrow(true);
			gl.setArrowStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
			gl.setEdgeStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
			final Paint color = link.isDown()? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED;
			gl.setArrowDrawPaint(color);
			gl.setArrowFillPaint(color);
			gl.setEdgeDrawPaint(color);
			gl.setShownSeparated(true);
		}
		for (Node node : pickedSRG.getNodes())
		{
			for (GUINode gn : getCanvasVerticallyStackedGUINodes(node))
			{
				gn.setDrawPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_FAILED);
		        gn.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_FAILED);
			}
		}
    }

    public void pickMulticastDemand (MulticastDemand pickedDemand)
    {
    	auxTemporalVariable_doNoAddResetPickInUndo = true;
   		resetPickedState();
    	auxTemporalVariable_doNoAddResetPickInUndo = false;
        this.pickedElementType = NetworkElementType.MULTICAST_DEMAND;
        this.pickedElementFR = null;
        this.pickedElementNotFR = pickedDemand;
    	pickTimeLineManager.pickMulticastDemand(currentNp, pickedDemand);

    	final boolean isDemandLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedDemand.getLayer());
		final GUINode gnOrigin = getCanvasAssociatedGUINode(pickedDemand.getIngressNode() , pickedDemand.getLayer());
		Set<Link> linksThisLayer = null;
		for (Node egressNode : pickedDemand.getEgressNodes())
		{
			final GUINode gnDestination = getCanvasAssociatedGUINode(egressNode , pickedDemand.getLayer());
			if (showInCanvasThisLayerPropagation && isDemandLayerVisibleInTheCanvas)
			{
				linksThisLayer = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic(egressNode , false);
	    		drawColateralLinks (linksThisLayer , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
			}
			if (showInCanvasLowerLayerPropagation && (currentNp.getNumberOfLayers() > 1))
			{
				if (linksThisLayer == null) linksThisLayer = pickedDemand.getLinksThisLayerPotentiallyCarryingTraffic(egressNode , false);
				final Pair<Set<Demand>,Set<Pair<MulticastDemand,Node>>> downLayerInfo = getDownCoupling(linksThisLayer); 
				final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (downLayerInfo.getFirst() , null , downLayerInfo.getSecond() , false , false);
				final Set<Link> linksLowerLayers = ipg.getLinksInGraph();
				drawColateralLinks (linksLowerLayers , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
				drawDownPropagationInterLayerLinks (linksLowerLayers , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
			}
			if (showInCanvasUpperLayerPropagation && (currentNp.getNumberOfLayers() > 1) && pickedDemand.isCoupled())
			{
				final Set<Link> upCoupledLink = getUpCoupling(null , Sets.newHashSet(Pair.of(pickedDemand,egressNode)));
				final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (null , upCoupledLink , null , true , false);
				drawColateralLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
				drawDownPropagationInterLayerLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
			}
			/* Picked link the last, so overrides the rest */
			if (isDemandLayerVisibleInTheCanvas)
			{
				gnDestination.setDrawPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
				gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
			}
		}
		/* Picked link the last, so overrides the rest */
		if (isDemandLayerVisibleInTheCanvas)
		{
			gnOrigin.setDrawPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
			gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
		}
    }

    public void pickRoute (Route pickedRoute)
    {
    	auxTemporalVariable_doNoAddResetPickInUndo = true;
   		resetPickedState();
    	auxTemporalVariable_doNoAddResetPickInUndo = false;
        this.pickedElementType = NetworkElementType.ROUTE;
        this.pickedElementFR = null;
        this.pickedElementNotFR = pickedRoute;
    	pickTimeLineManager.pickRoute(currentNp, pickedRoute);

    	final boolean isRouteLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedRoute.getLayer());
		if (showInCanvasThisLayerPropagation && isRouteLayerVisibleInTheCanvas)
		{
    		final List<Link> linksPrimary = pickedRoute.getSeqLinks();
    		drawColateralLinks (linksPrimary , pickedRoute.isBackupRoute()? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
		}
		if (showInCanvasLowerLayerPropagation && (currentNp.getNumberOfLayers() > 1))
		{
			final Pair<Set<Demand>,Set<Pair<MulticastDemand,Node>>> downInfo = getDownCoupling (pickedRoute.getSeqLinks());
			final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (downInfo.getFirst() , null , downInfo.getSecond() , false , false);
			drawColateralLinks (ipg.getLinksInGraph() , pickedRoute.isBackupRoute()? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
			drawDownPropagationInterLayerLinks (ipg.getLinksInGraph() , pickedRoute.isBackupRoute()? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_BACKUP : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
		}
		if (showInCanvasUpperLayerPropagation && (currentNp.getNumberOfLayers() > 1) && pickedRoute.getDemand().isCoupled())
		{
			final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (null , Sets.newHashSet(pickedRoute.getDemand().getCoupledLink()) , null , true , false);
			drawColateralLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
			drawDownPropagationInterLayerLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
		}
		/* Picked link the last, so overrides the rest */
		if (isRouteLayerVisibleInTheCanvas)
		{
			final GUINode gnOrigin = getCanvasAssociatedGUINode(pickedRoute.getIngressNode() , pickedRoute.getLayer());
			final GUINode gnDestination = getCanvasAssociatedGUINode(pickedRoute.getEgressNode() , pickedRoute.getLayer());
	        gnOrigin.setDrawPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
	        gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
	        gnDestination.setDrawPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
	        gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
		}
    }

    public void pickMulticastTree (MulticastTree pickedTree)
    {
    	auxTemporalVariable_doNoAddResetPickInUndo = true;
   		resetPickedState();
    	auxTemporalVariable_doNoAddResetPickInUndo = false;
        this.pickedElementType = NetworkElementType.MULTICAST_TREE;
        this.pickedElementFR = null;
        this.pickedElementNotFR = pickedTree;
    	pickTimeLineManager.pickMulticastTree(currentNp, pickedTree);

		final boolean isTreeLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedTree.getLayer());
		final GUINode gnOrigin = getCanvasAssociatedGUINode(pickedTree.getIngressNode() , pickedTree.getLayer());
		for (Node egressNode : pickedTree.getEgressNodes())
		{
			final GUINode gnDestination = getCanvasAssociatedGUINode(egressNode , pickedTree.getLayer());
			if (showInCanvasThisLayerPropagation && isTreeLayerVisibleInTheCanvas)
			{
	    		final List<Link> linksPrimary = pickedTree.getSeqLinksToEgressNode(egressNode);
	    		drawColateralLinks (linksPrimary , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
			}
			if (showInCanvasLowerLayerPropagation && (currentNp.getNumberOfLayers() > 1))
			{
				final Pair<Set<Demand>,Set<Pair<MulticastDemand,Node>>> downInfo = getDownCoupling (pickedTree.getSeqLinksToEgressNode(egressNode));
				final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (downInfo.getFirst() , null , downInfo.getSecond() , false , false);
				drawColateralLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
				drawDownPropagationInterLayerLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
			}
			if (showInCanvasUpperLayerPropagation && (currentNp.getNumberOfLayers() > 1) && pickedTree.getMulticastDemand().isCoupled())
			{
				final Set<Link> upperCoupledLink = getUpCoupling(null , Arrays.asList(Pair.of(pickedTree.getMulticastDemand() , egressNode)));
				final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (null , upperCoupledLink , null , true , false);
				drawColateralLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
				drawDownPropagationInterLayerLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
			}
			if (isTreeLayerVisibleInTheCanvas)
			{
		        gnDestination.setDrawPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
		        gnDestination.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
			}
		}
		/* Picked link the last, so overrides the rest */
		if (isTreeLayerVisibleInTheCanvas)
		{
	        gnOrigin.setDrawPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
	        gnOrigin.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
		}
    }

    public void pickLink (Link pickedLink)
    {
    	auxTemporalVariable_doNoAddResetPickInUndo = true;
   		resetPickedState();
    	auxTemporalVariable_doNoAddResetPickInUndo = false;
        this.pickedElementType = NetworkElementType.LINK;
        this.pickedElementFR = null;
        this.pickedElementNotFR = pickedLink;
    	pickTimeLineManager.pickLink(currentNp, pickedLink);

		final boolean isLinkLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedLink.getLayer());
		Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> thisLayerTraversalInfo = null;
		if (showInCanvasThisLayerPropagation && isLinkLayerVisibleInTheCanvas)
		{
    		thisLayerTraversalInfo = pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false);
    		final Set<Link> linksPrimary = thisLayerTraversalInfo.getFirst().values().stream().flatMap(set->set.stream()).collect (Collectors.toSet());
    		final Set<Link> linksBackup = thisLayerTraversalInfo.getSecond().values().stream().flatMap(set->set.stream()).collect (Collectors.toSet());
    		final Set<Link> linksMulticast = thisLayerTraversalInfo.getThird().values().stream().flatMap(set->set.stream()).collect (Collectors.toSet());
    		drawColateralLinks (Sets.union(Sets.union(linksPrimary , linksBackup) , linksMulticast) , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
		}
		if (showInCanvasLowerLayerPropagation && (currentNp.getNumberOfLayers() > 1) && pickedLink.isCoupled())
		{
			final Pair<Set<Demand>,Set<Pair<MulticastDemand,Node>>> downLayerInfo = getDownCoupling (Arrays.asList(pickedLink));
			final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (downLayerInfo.getFirst() , null , downLayerInfo.getSecond() , false , false);
			drawColateralLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
			drawDownPropagationInterLayerLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
		}
		if (showInCanvasUpperLayerPropagation && (currentNp.getNumberOfLayers() > 1))
		{
			if (thisLayerTraversalInfo == null) thisLayerTraversalInfo = pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false);
    		final Set<Demand> demandsPrimaryAndBackup = Sets.union(thisLayerTraversalInfo.getFirst().keySet() , thisLayerTraversalInfo.getSecond().keySet());
    		final Set<Pair<MulticastDemand,Node>> mDemands = thisLayerTraversalInfo.getThird().keySet();
    		final Set<Link> initialUpperLinks = getUpCoupling(demandsPrimaryAndBackup , mDemands);
			final InterLayerPropagationGraph ipg = new InterLayerPropagationGraph (null , Sets.newHashSet(initialUpperLinks) , null , true , false);
			drawColateralLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
			drawDownPropagationInterLayerLinks (ipg.getLinksInGraph() , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
		}
		/* Picked link the last, so overrides the rest */
		if (isLinkLayerVisibleInTheCanvas)
		{
			final GUILink gl = getCanvasAssociatedGUILink(pickedLink);
			gl.setHasArrow(true);
			gl.setArrowStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
			gl.setEdgeStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
			final Paint color = pickedLink.isDown()? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED;
			gl.setArrowDrawPaint(color);
			gl.setArrowFillPaint(color);
			gl.setEdgeDrawPaint(color);
			gl.setShownSeparated(true);
		}
    }
    
    public void pickNode (Node pickedNode)
    {
    	auxTemporalVariable_doNoAddResetPickInUndo = true;
   		resetPickedState();
    	auxTemporalVariable_doNoAddResetPickInUndo = false;
        this.pickedElementType = NetworkElementType.NODE;
        this.pickedElementFR = null;
        this.pickedElementNotFR = pickedNode;
    	pickTimeLineManager.pickNode(currentNp, pickedNode);

		for (GUINode gn : getCanvasVerticallyStackedGUINodes(pickedNode))
		{
            gn.setDrawPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_PICK);
            gn.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_PICK);
		}
		for (Link e : Sets.union(pickedNode.getOutgoingLinks(currentNp.getNetworkLayerDefault()) , pickedNode.getIncomingLinks(currentNp.getNetworkLayerDefault())))
		{
			final GUILink gl = getCanvasAssociatedGUILink(e);
			gl.setShownSeparated(true);
			gl.setHasArrow(true);
		}
    }
    
    public void pickResource (Resource pickedResource)
    {
    	auxTemporalVariable_doNoAddResetPickInUndo = true;
   		resetPickedState();
    	auxTemporalVariable_doNoAddResetPickInUndo = false;
        this.pickedElementType = NetworkElementType.RESOURCE;
        this.pickedElementFR = null;
        this.pickedElementNotFR = pickedResource;
    	pickTimeLineManager.pickResource(currentNp, pickedResource);

    	for (GUINode gn : getCanvasVerticallyStackedGUINodes(pickedResource.getHostNode()))
		{
            gn.setDrawPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_RESOURCE);
            gn.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_RESOURCE);
		}
    }

    public void pickForwardingRule (Pair<Demand,Link> pickedFR)
    {
    	auxTemporalVariable_doNoAddResetPickInUndo = true;
   		resetPickedState();
    	auxTemporalVariable_doNoAddResetPickInUndo = false;
        this.pickedElementType = NetworkElementType.FORWARDING_RULE;
        this.pickedElementFR = pickedFR;
        this.pickedElementNotFR = null;
    	pickTimeLineManager.pickForwardingRule(currentNp, pickedFR);

    	final boolean isFRLayerVisibleInTheCanvas = isLayerVisibleInCanvas(pickedFR.getFirst().getLayer());
    	final Demand pickedDemand = pickedFR.getFirst();
    	final Link pickedLink = pickedFR.getSecond();
		if (showInCanvasThisLayerPropagation && isFRLayerVisibleInTheCanvas)
		{
    		final Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> triple = 
    				pickedLink.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink(false);
    		final Set<Link> linksPrimary = triple.getFirst().get(pickedDemand);
    		final Set<Link> linksBackup = triple.getSecond().get(pickedDemand);
    		drawColateralLinks (Sets.union(linksPrimary , linksBackup) , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
		}
		if (showInCanvasLowerLayerPropagation && (currentNp.getNumberOfLayers() > 1) && pickedLink.isCoupled())
		{
			final Pair<Set<Demand>,Set<Pair<MulticastDemand,Node>>> downLayerInfo = getDownCoupling (Arrays.asList(pickedLink));
			final InterLayerPropagationGraph ipgCausedByLink = new InterLayerPropagationGraph (downLayerInfo.getFirst() , null , downLayerInfo.getSecond() , false , false);
			final Set<Link> frPropagationLinks = ipgCausedByLink.getLinksInGraph(); 
			drawColateralLinks (frPropagationLinks , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
			drawDownPropagationInterLayerLinks (frPropagationLinks , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
		}
		if (showInCanvasUpperLayerPropagation && (currentNp.getNumberOfLayers() > 1) && pickedDemand.isCoupled())
		{
			final InterLayerPropagationGraph ipgCausedByDemand = new InterLayerPropagationGraph (null , Sets.newHashSet(pickedDemand.getCoupledLink()) , null , true , false);
			final Set<Link> frPropagationLinks = ipgCausedByDemand.getLinksInGraph(); 
			drawColateralLinks (frPropagationLinks , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
			drawDownPropagationInterLayerLinks (frPropagationLinks , VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED);
		}
		/* Picked link the last, so overrides the rest */
		if (isFRLayerVisibleInTheCanvas)
		{
			final GUILink gl = getCanvasAssociatedGUILink(pickedLink);
			gl.setHasArrow(true);
			gl.setArrowStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
			gl.setEdgeStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED);
			final Paint color = pickedLink.isDown()? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_PICKED;
			gl.setArrowDrawPaint(color);
			gl.setArrowFillPaint(color);
			gl.setEdgeDrawPaint(color);
			gl.setShownSeparated(true);
			gl.getOriginNode().setDrawPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
			gl.getOriginNode().setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ORIGINFLOW);
			gl.getDestinationNode().setDrawPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
			gl.getDestinationNode().setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR_ENDFLOW);
		}
    }

    
    public void pickElement (NetworkElement e)
    {
    	if (e instanceof Node) pickNode ((Node) e);
    	else if (e instanceof Node) pickNode ((Node) e);
    	else if (e instanceof Link) pickLink ((Link) e);
    	else if (e instanceof Demand) pickDemand ((Demand) e);
    	else if (e instanceof Route) pickRoute ((Route) e);
    	else if (e instanceof MulticastDemand) pickMulticastDemand ((MulticastDemand) e);
    	else if (e instanceof MulticastTree) pickMulticastTree ((MulticastTree) e);
    	else if (e instanceof Resource) pickResource ((Resource) e);
    	else if (e instanceof SharedRiskGroup) pickSRG ((SharedRiskGroup) e);
    	else throw new RuntimeException();
    }
    
    public void resetPickedState ()
    {
        this.pickedElementType = null;
        this.pickedElementNotFR = null;
        this.pickedElementFR = null;
    	if (!auxTemporalVariable_doNoAddResetPickInUndo) pickTimeLineManager.updatePickUndoList_newPickOrPickReset(currentNp);

        for (GUINode n : getCanvasAllGUINodes())
        {
            n.setDrawPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR);
            n.setFillPaint(VisualizationConstants.DEFAULT_GUINODE_COLOR);
        }
        for (GUILink e : getCanvasAllGUILinks(true, false))
        {
            e.setHasArrow(VisualizationConstants.DEFAULT_REGGUILINK_HASARROW);
            e.setArrowStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_ACTIVELAYER , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE);
            e.setEdgeStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_ACTIVELAYER , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE);
            final boolean isDown = e.getAssociatedNetPlanLink().isDown(); 
            final Paint color = isDown? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR;
            e.setArrowDrawPaint(color);
            e.setArrowFillPaint(color);
            e.setEdgeDrawPaint(color);
            e.setShownSeparated(isDown);
        }
        for (GUILink e : getCanvasAllGUILinks(false, true))
        {
            e.setHasArrow(VisualizationConstants.DEFAULT_INTRANODEGUILINK_HASARROW);
            e.setArrowStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE , VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE);
            e.setEdgeStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE , VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE);
            e.setArrowDrawPaint(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR);
            e.setArrowFillPaint(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR);
            e.setEdgeDrawPaint(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR);
            e.setShownSeparated(false);
        }
    }

    private void drawDownPropagationInterLayerLinks (Set<Link> links , Paint color)
    {
    	for (Link link : links)
    	{
    		final GUILink gl = getCanvasAssociatedGUILink(link);
    		if (gl == null) continue;
    		if (!link.isCoupled()) continue;
    		final boolean isCoupledToDemand = link.getCoupledDemand() != null;
    		final NetworkLayer upperLayer = link.getLayer(); 
    		final NetworkLayer lowerLayer = isCoupledToDemand? link.getCoupledDemand().getLayer() : link.getCoupledMulticastDemand().getLayer(); 
    		if (!isLayerVisibleInCanvas(lowerLayer)) continue;
    		for (GUILink interLayerLink : getCanvasIntraNodeGUILinkSequence(link.getOriginNode() , upperLayer , lowerLayer))
    		{
    			interLayerLink.setArrowStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED , VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED);
    			interLayerLink.setEdgeStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED , VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED);
    			interLayerLink.setArrowDrawPaint(color);
    			interLayerLink.setArrowFillPaint(color);
    			interLayerLink.setEdgeDrawPaint(color);
    			interLayerLink.setShownSeparated(false);
    			interLayerLink.setHasArrow(true);
    		}
    		for (GUILink interLayerLink : getCanvasIntraNodeGUILinkSequence(link.getDestinationNode() , lowerLayer , upperLayer))
    		{
    			interLayerLink.setArrowStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED , VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED);
    			interLayerLink.setEdgeStroke(VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED , VisualizationConstants.DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED);
    			interLayerLink.setArrowDrawPaint(color);
    			interLayerLink.setArrowFillPaint(color);
    			interLayerLink.setEdgeDrawPaint(color);
    			interLayerLink.setShownSeparated(false);
    			interLayerLink.setHasArrow(true);
    		}
    	}
    }
    private void drawColateralLinks (Collection<Link> links , Paint colorIfNotFailedLink)
    {
		for (Link link : links)
		{
			final GUILink glColateral = getCanvasAssociatedGUILink(link);
			if (glColateral == null) continue;
			glColateral.setArrowStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALACTVELAYER , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALNONACTIVELAYER);
			glColateral.setEdgeStroke(VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALACTVELAYER , VisualizationConstants.DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALNONACTIVELAYER);
			final Paint color = link.isDown()? VisualizationConstants.DEFAULT_REGGUILINK_EDGECOLOR_FAILED : colorIfNotFailedLink;
			glColateral.setArrowDrawPaint(color);
			glColateral.setArrowFillPaint(color);
			glColateral.setEdgeDrawPaint(color);
			glColateral.setShownSeparated(true);
    		glColateral.setHasArrow(true);
		}
    }

    private static Pair<Set<Demand>,Set<Pair<MulticastDemand,Node>>> getDownCoupling (Collection<Link> links)
    {
    	final Set<Demand> res_1 = new HashSet<> ();
    	final Set<Pair<MulticastDemand,Node>> res_2 = new HashSet<> ();
    	for (Link link : links)
    	{
    		if (link.getCoupledDemand() != null) res_1.add(link.getCoupledDemand());
    		else if (link.getCoupledMulticastDemand() != null) res_2.add(Pair.of(link.getCoupledMulticastDemand() , link.getDestinationNode()));
    	}
    	return Pair.of(res_1 , res_2);
    	
    }
    private static Set<Link> getUpCoupling (Collection<Demand> demands , Collection<Pair<MulticastDemand,Node>> mDemands)
    {
    	final Set<Link> res = new HashSet<> ();
    	if (demands != null)
    		for (Demand d : demands)
    			if (d.isCoupled()) res.add(d.getCoupledLink());
    	if (mDemands != null)
    		for (Pair<MulticastDemand,Node> md : mDemands)
    		{
    			if (md.getFirst().isCoupled())
        			res.add(md.getFirst().getCoupledLinks().stream().filter (e->e.getDestinationNode() == md.getSecond()).findFirst().get());
//    			System.out.println(md.getFirst().getCoupledLinks().stream().map(e->e.getDestinationNode()).collect(Collectors.toList()));
//    			System.out.println(md.getSecond());
    		}
    	return res;
    }

    public List<NetworkLayer> getCanvasLayersInVisualizationOrder (boolean considerNonVisible)
    {
    	BidiMap<Integer, NetworkLayer> map = considerNonVisible? mapLayer2VisualizationOrderInCanvas.inverseBidiMap() : cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.inverseBidiMap();
    	List<NetworkLayer> res = new ArrayList<> ();
    	for (int vIndex = 0; vIndex < currentNp.getNumberOfLayers() ; vIndex ++)
    		res.add(map.get(vIndex));
    	return res;
    }

    public BidiMap<NetworkLayer,Integer> getCanvasLayerOrderIndexMap(boolean considerNonVisible)
    {
    	return considerNonVisible? mapLayer2VisualizationOrderInCanvas : cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible;
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
			if (pickedElementNotFR != null)
				this.pickElement(pickedElementNotFR);
			else
				this.pickForwardingRule(pickedElementFR);
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
			if (pickedElementNotFR != null)
				this.pickElement(pickedElementNotFR);
			else
				this.pickForwardingRule(pickedElementFR);
	}

	/**
	 * @param showThisLayerPropagation the showThisLayerPropagation to set
	 */
	public void setShowInCanvasThisLayerPropagation(boolean showThisLayerPropagation)
	{
		if (showThisLayerPropagation == this.showInCanvasThisLayerPropagation) return;
		this.showInCanvasThisLayerPropagation = showThisLayerPropagation;
		if (pickedElementType != null)
			if (pickedElementNotFR != null)
				this.pickElement(pickedElementNotFR);
			else
				this.pickForwardingRule(pickedElementFR);
	}

	public Map<NetworkLayer,Boolean> getCanvasLayerVisibilityMap () { return Collections.unmodifiableMap(this.layerVisibilityInCanvasMap); }
    
    public static Pair<ImageIcon,Shape> getIcon (URL url , int height , Color borderColor)
    {
    	final Pair<ImageIcon,Shape> iconShapeInfo = databaseOfAlreadyReadIcons.get(Triple.of(url , height , borderColor));
    	if (iconShapeInfo != null) return iconShapeInfo;
		if (url == null)
		{
			BufferedImage img = ImageUtils.createCircle(height , (Color) VisualizationConstants.DEFAULT_GUINODE_COLOR);
			if (img.getHeight() != height) throw new RuntimeException();
			final Shape shapeNoBorder = FourPassImageShaper.getShape(img);
			if (borderColor.getAlpha() != 0)
				img = ImageUtils.addBorder(img , VisualizationConstants.DEFAULT_ICONBORDERSIZEINPIXELS , borderColor);
			final ImageIcon icon = new ImageIcon (img);
			final Pair<ImageIcon,Shape> res = Pair.of(icon , shapeNoBorder);
			databaseOfAlreadyReadIcons.put(Triple.of(null , icon.getIconHeight() , borderColor) , res);
			return res;
		}
		try
		{
    		/* Read the base buffered image */
			BufferedImage img = ImageIO.read(url);
			if (img.getHeight() != height)
				img = ImageUtils.resize(img , (int) (img.getWidth() * height / (double) img.getHeight()) , height);
			if (img.getHeight() != height) throw new RuntimeException();
			final Shape shapeNoBorder = FourPassImageShaper.getShape(img); 
			if (borderColor.getAlpha() != 0)
				img = ImageUtils.addBorder(img , VisualizationConstants.DEFAULT_ICONBORDERSIZEINPIXELS , borderColor);
			final ImageIcon icon = new ImageIcon (img);
            final AffineTransform translateTransform = AffineTransform.getTranslateInstance(-icon.getIconWidth()/2, -icon.getIconHeight()/2);
            final Pair<ImageIcon,Shape> res = Pair.of(icon , translateTransform.createTransformedShape(shapeNoBorder));
			databaseOfAlreadyReadIcons.put(Triple.of(url , icon.getIconHeight() , borderColor) , res);
			return res;
		} catch (Exception e)
		{
			System.out.println("URL: **" + url + "**");
			System.out.println(url);
			/* Use the default image, whose URL is the one given */
			e.printStackTrace();
			return getIcon (null , height , borderColor);
		}
    }

//    public VisualizationState copy (NetPlan translateToThisNp)
//    {
//        checkCacheConsistency();
//    	checkNpToVsConsistency (this , this.currentNp);
//    	final boolean translateGUINodesLinksAndNpElements = translateToThisNp != null;
//    	final VisualizationState copyVs = new VisualizationState();
//    	copyVs.showInCanvasNodeNames = this.showInCanvasNodeNames;
//    	copyVs.showInCanvasLinkLabels = this.showInCanvasLinkLabels;
//    	copyVs.showInCanvasLinksInNonActiveLayer = this.showInCanvasLinksInNonActiveLayer;
//    	copyVs.showInCanvasInterLayerLinks = this.showInCanvasInterLayerLinks;
//    	copyVs.showInCanvasLowerLayerPropagation = this.showInCanvasLowerLayerPropagation;
//    	copyVs.showInCanvasUpperLayerPropagation = this.showInCanvasUpperLayerPropagation;
//    	copyVs.showInCanvasThisLayerPropagation = this.showInCanvasThisLayerPropagation;
//    	copyVs.showInCanvasNonConnectedNodes = this.showInCanvasNonConnectedNodes;
//    	copyVs.interLayerSpaceInPixels = this.interLayerSpaceInPixels;
//    	copyVs.pickedElementType = this.pickedElementType;
//    	if (translateGUINodesLinksAndNpElements)
//    	{
//        	copyVs.currentNp = translateToThisNp;
//        	copyVs.tableRowFilter = null; // the row filter is not copied when the netPlan object changes
//    		copyVs.pickedElementNotFR = this.pickedElementNotFR == null? null : translateToThisNp.getNetworkElement(this.pickedElementNotFR.getId());
//    		copyVs.pickedElementFR = this.pickedElementFR == null? null : Pair.of(translateToThisNp.getDemandFromId(this.pickedElementFR.getFirst().getId()), translateToThisNp.getLinkFromId(this.pickedElementFR.getSecond().getId()));
//    		
//    		copyVs.nodesToHideInCanvasAsMandatedByUserInTable = getTranslatedNodeSet(translateToThisNp, this.nodesToHideInCanvasAsMandatedByUserInTable);
//    		copyVs.linksToHideInCanvasAsMandatedByUserInTable = getTranslatedLinkSet(translateToThisNp, this.linksToHideInCanvasAsMandatedByUserInTable);
//    		copyVs.mapLayer2VisualizationOrderInCanvas = new DualHashBidiMap<>(); 
//    		for (Entry<NetworkLayer,Integer> entry : this.mapLayer2VisualizationOrderInCanvas.entrySet()) 
//    			copyVs.mapLayer2VisualizationOrderInCanvas.put(translateToThisNp.getNetworkLayer(entry.getKey().getIndex()), entry.getValue());
//    		copyVs.layerVisibilityInCanvasMap = new HashMap<>(); 
//    		copyVs.cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible = new DualHashBidiMap<>();
//    		for (Entry<NetworkLayer,Integer> entry : this.cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.entrySet()) 
//    			copyVs.cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.put(translateToThisNp.getNetworkLayer(entry.getKey().getIndex()), entry.getValue());
//    		for (Entry<NetworkLayer,Boolean> entry : this.layerVisibilityInCanvasMap.entrySet()) 
//    			copyVs.layerVisibilityInCanvasMap.put(translateToThisNp.getNetworkLayer(entry.getKey().getIndex()), entry.getValue());
//    		copyVs.mapShowInCanvasLayerLinks = new HashMap<>(); 
//    		for (Entry<NetworkLayer,Boolean> entry : this.mapShowInCanvasLayerLinks.entrySet()) 
//    			copyVs.mapShowInCanvasLayerLinks.put(translateToThisNp.getNetworkLayer(entry.getKey().getIndex()), entry.getValue());
//
//    		/* translate the guinodes and its info */
//    		copyVs.cache_mapNode2ListVerticallyStackedGUINodes = new HashMap<> ();
//    		for (Entry<Node,List<GUINode>> entry : this.cache_mapNode2ListVerticallyStackedGUINodes.entrySet())
//    		{
//    			final Node tNode = translateToThisNp.getNode(entry.getKey().getIndex());
//    			final List<GUINode> tList = new ArrayList<> (entry.getValue().size());
//    			for (GUINode gn : entry.getValue())
//    				tList.add(gn.copy(translateToThisNp, copyVs));
//    			copyVs.cache_mapNode2ListVerticallyStackedGUINodes.put(tNode ,  tList);
//    		}
//    		copyVs.cache_canvasRegularLinkMap = new HashMap <> ();
//    		for (Entry<Link,GUILink> entry : this.cache_canvasRegularLinkMap.entrySet())
//    		{
//    			final Link tLink = translateToThisNp.getLinkFromId(entry.getKey().getId());
//    			final GUILink originalGl = entry.getValue();
//    			final Node tOriginNode = translateToThisNp.getNode(originalGl.getOriginNode().getAssociatedNetPlanNode().getIndex());
//    			final Node tDestinationNode = translateToThisNp.getNode(originalGl.getDestinationNode().getAssociatedNetPlanNode().getIndex());
//    			final NetworkLayer tLayer = translateToThisNp.getNetworkLayer(originalGl.getAssociatedNetPlanLink().getLayer().getIndex());
//    			final GUINode tOriginGn = copyVs.getCanvasAssociatedGUINode(tOriginNode, tLayer);
//    			final GUINode tDestinationGn = copyVs.getCanvasAssociatedGUINode(tDestinationNode, tLayer);
//    			copyVs.cache_canvasRegularLinkMap.put(tLink, entry.getValue().copy(tLink, tOriginGn, tDestinationGn));
//    		}
//    		copyVs.cache_canvasIntraNodeGUILinks = new HashMap<> ();
//    		copyVs.cache_mapNode2IntraNodeCanvasGUILinkMap = new HashMap<> ();
//    		for (Node thisNode : this.cache_canvasIntraNodeGUILinks.keySet())
//    		{
//    			final Node tNode = translateToThisNp.getNode(thisNode.getIndex());
//    			final Set<GUILink> tSetIntraLinks = new HashSet<> ();
//    			final Map<Pair<Integer,Integer>,GUILink> tMapIntraLinks = new HashMap<> ();
//    			for (Entry<Pair<Integer,Integer>,GUILink> entry : this.cache_mapNode2IntraNodeCanvasGUILinkMap.get(thisNode).entrySet())
//    			{
//    				final GUILink originalGl = entry.getValue();
//        			final Node tOriginNode = translateToThisNp.getNode(originalGl.getOriginNode().getAssociatedNetPlanNode().getIndex());
//        			final Node tDestinationNode = translateToThisNp.getNode(originalGl.getDestinationNode().getAssociatedNetPlanNode().getIndex());
//        			final NetworkLayer tOriginLayer = translateToThisNp.getNetworkLayer(originalGl.getOriginNode().getLayer().getIndex());
//        			final NetworkLayer tDestinationLayer = translateToThisNp.getNetworkLayer(originalGl.getDestinationNode().getLayer().getIndex());
//        			final GUINode tOriginGn = copyVs.getCanvasAssociatedGUINode(tOriginNode, tOriginLayer);
//        			final GUINode tDestinationGn = copyVs.getCanvasAssociatedGUINode(tDestinationNode, tDestinationLayer);
//    				final GUILink tGl = originalGl.copy(null, tOriginGn, tDestinationGn);
//    				tSetIntraLinks.add(tGl);
//    				tMapIntraLinks.put(entry.getKey(), tGl);
//    			}
//    			copyVs.cache_canvasIntraNodeGUILinks.put(tNode , tSetIntraLinks);
//    			copyVs.cache_mapNode2IntraNodeCanvasGUILinkMap.put(tNode, tMapIntraLinks);
//    		}
//    	}
//    	else
//    	{
//        	copyVs.currentNp = this.currentNp;
//        	copyVs.nodesToHideInCanvasAsMandatedByUserInTable = new HashSet<> (this.nodesToHideInCanvasAsMandatedByUserInTable);
//        	copyVs.linksToHideInCanvasAsMandatedByUserInTable = new HashSet<> (this.linksToHideInCanvasAsMandatedByUserInTable);
//        	copyVs.mapLayer2VisualizationOrderInCanvas = new DualHashBidiMap<>(this.mapLayer2VisualizationOrderInCanvas);
//        	copyVs.layerVisibilityInCanvasMap = new HashMap<> (this.layerVisibilityInCanvasMap);
//        	copyVs.mapShowInCanvasLayerLinks = new HashMap<> (this.mapShowInCanvasLayerLinks);
//        	copyVs.cache_canvasRegularLinkMap = new HashMap<> (this.cache_canvasRegularLinkMap);
//        	copyVs.pickedElementNotFR = this.pickedElementNotFR;
//        	copyVs.pickedElementFR = this.pickedElementFR;
//        	copyVs.tableRowFilter = this.tableRowFilter; // we keep the row filter since we have the same netPlan object
//        	copyVs.cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible = new DualHashBidiMap<>(this.cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible);
//
//        	copyVs.cache_canvasIntraNodeGUILinks = new HashMap<> ();
//        	for (Entry<Node, Set<GUILink>> entry : this.cache_canvasIntraNodeGUILinks.entrySet())
//        		copyVs.cache_canvasIntraNodeGUILinks.put(entry.getKey() , new HashSet<> (entry.getValue()));
//        	copyVs.cache_mapNode2ListVerticallyStackedGUINodes = new HashMap<> ();
//        	for (Entry<Node, List<GUINode>> entry : this.cache_mapNode2ListVerticallyStackedGUINodes.entrySet())
//        		copyVs.cache_mapNode2ListVerticallyStackedGUINodes.put(entry.getKey() , new ArrayList<> (entry.getValue()));
//        	copyVs.cache_mapNode2IntraNodeCanvasGUILinkMap = new HashMap<> ();
//        	for (Entry<Node, Map<Pair<Integer, Integer>, GUILink>> entry : this.cache_mapNode2IntraNodeCanvasGUILinkMap.entrySet())
//        		copyVs.cache_mapNode2IntraNodeCanvasGUILinkMap.put(entry.getKey() , new HashMap<> (entry.getValue()));
//    	}
//    	copyVs.checkCacheConsistency();
//    	if (translateToThisNp == null) System.out.println("translateToThisNp: null");
//    	
//        checkCacheConsistency();
//    	checkNpToVsConsistency (this , this.currentNp);
//
//    	checkNpToVsConsistency(copyVs , translateToThisNp == null? this.currentNp : translateToThisNp);
//    	copyVs.checkCacheConsistency();
//    	return copyVs;
//    }
//
//	private VisualizationState() 	{ }

    public Pair<NetworkElement, Pair<Demand, Link>> getPickNavigationBackElement()
    {
        return pickTimeLineManager.getPickNavigationBackElement();
    }

    public Pair<NetworkElement, Pair<Demand, Link>> getPickNavigationForwardElement()
    {
        return pickTimeLineManager.getPickNavigationForwardElement();
    }

    public static void checkNpToVsConsistency (VisualizationState vs , NetPlan np)
	{
		if (vs.currentNp != np) throw new RuntimeException ("inputVs.currentNp:" + vs.currentNp.hashCode() + ", inputNp: " + np.hashCode());
		for (Node n : vs.nodesToHideInCanvasAsMandatedByUserInTable) if (n.getNetPlan() != np) throw new RuntimeException (); 
		for (Link e : vs.linksToHideInCanvasAsMandatedByUserInTable) if (e.getNetPlan() != np) throw new RuntimeException (); 
		for (NetworkLayer e : vs.mapLayer2VisualizationOrderInCanvas.keySet()) if (e.getNetPlan() != np) throw new RuntimeException (); 
		for (NetworkLayer e : vs.layerVisibilityInCanvasMap.keySet()) if (e.getNetPlan() != np) throw new RuntimeException (); 
		for (NetworkLayer e : vs.mapShowInCanvasLayerLinks.keySet()) if (e.getNetPlan() != np) throw new RuntimeException (); 
		for (Node e : vs.cache_canvasIntraNodeGUILinks.keySet()) if (e.getNetPlan() != np) throw new RuntimeException (); 
		for (Set<GUILink> s : vs.cache_canvasIntraNodeGUILinks.values()) for (GUILink e : s) if (e.getOriginNode().getAssociatedNetPlanNode().getNetPlan() != np) throw new RuntimeException ();
		for (Set<GUILink> s : vs.cache_canvasIntraNodeGUILinks.values()) for (GUILink e : s) if (e.getDestinationNode().getAssociatedNetPlanNode().getNetPlan() != np) throw new RuntimeException ();
		for (Link e : vs.cache_canvasRegularLinkMap.keySet()) if (e.getNetPlan() != np) throw new RuntimeException (); 
		for (GUILink e : vs.cache_canvasRegularLinkMap.values()) if (e.getAssociatedNetPlanLink().getNetPlan() != np) throw new RuntimeException (); 
		for (NetworkLayer e : vs.cache_mapCanvasVisibleLayer2VisualizationOrderRemovingNonVisible.keySet()) if (e.getNetPlan() != np) throw new RuntimeException (); 
		for (Node e : vs.cache_mapNode2IntraNodeCanvasGUILinkMap.keySet()) if (e.getNetPlan() != np) throw new RuntimeException (); 
		for (Map<Pair<Integer, Integer>, GUILink> map : vs.cache_mapNode2IntraNodeCanvasGUILinkMap.values()) for (GUILink gl : map.values()) if (gl.getOriginNode().getAssociatedNetPlanNode().getNetPlan() != np) throw new RuntimeException (); 
		for (Map<Pair<Integer, Integer>, GUILink> map : vs.cache_mapNode2IntraNodeCanvasGUILinkMap.values()) for (GUILink gl : map.values()) if (gl.getDestinationNode().getAssociatedNetPlanNode().getNetPlan() != np) throw new RuntimeException (); 
		for (Node e : vs.cache_mapNode2ListVerticallyStackedGUINodes.keySet()) if (e.getNetPlan() != np) throw new RuntimeException (); 
		for (List<GUINode> list : vs.cache_mapNode2ListVerticallyStackedGUINodes.values()) for (GUINode gn : list) if (gn.getAssociatedNetPlanNode().getNetPlan() != np) throw new RuntimeException (); 
		if (vs.pickedElementNotFR != null) if (vs.pickedElementNotFR.getNetPlan() != np) throw new RuntimeException ();
		if (vs.pickedElementFR != null) if (vs.pickedElementFR.getFirst ().getNetPlan() != np) throw new RuntimeException ();
		if (vs.pickedElementFR != null) if (vs.pickedElementFR.getSecond ().getNetPlan() != np) throw new RuntimeException ();
	}
}
