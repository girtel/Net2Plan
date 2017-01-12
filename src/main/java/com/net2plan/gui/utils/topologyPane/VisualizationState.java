package com.net2plan.gui.utils.topologyPane;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Pair;

public class VisualizationState
{
    public final static float SCALE_IN = 1.1f;
    public final static float SCALE_OUT = 1 / SCALE_IN;
	private boolean showNodeNames;
	private boolean showLinkLabels;
    private boolean showNonConnectedNodes;
    private NetPlan currentNp;
    private List<VisualizationLayer> vLayers;
    private Map<Node,List<GUILink>> intraNodeGUILinks;
    private Map<Node,List<GUINode>> cache_nodeGuiNodeMap;
    private Map<Link,GUILink> regularLinkMap;
    private int interLayerDistanceInPixels;
    
    public NetPlan getNetPlan () { return currentNp; }

	public VisualizationState (NetPlan currentNp)
	{
		this.currentNp = currentNp;
		this.showNodeNames = false;
		this.showLinkLabels = true;
		this.showNonConnectedNodes = false;
		this.vLayers = new ArrayList<> ();
		this.vLayers.add(new VisualizationLayer(currentNp.getNetworkLayerDefault() , this , vLayers.size()));
		this.intraNodeGUILinks = new HashMap<> ();
		this.cache_nodeGuiNodeMap = new HashMap<> ();
		this.interLayerDistanceInPixels = 50;
		this.regularLinkMap = new HashMap<> ();
	}

	public List<GUINode> getVerticallyStackedNodes (Node n) { return cache_nodeGuiNodeMap.get(n); } 
	
	public void rebuildVisualizationState ()
	{
		for (Node n : currentNp.getNodes())
		{
	        List<GUINode> associatedGUINodes = new ArrayList<> ();
	        for (VisualizationState.VisualizationLayer vLayer : vLayers)
	        {
	        	GUINode gn = new GUINode(n , vLayer);
	        	associatedGUINodes.add(gn);
	        	if (associatedGUINodes.size() > 1)
	        	{
	        		GUILink gl1 = new GUILink (null , associatedGUINodes.get(associatedGUINodes.size() - 2), gn);
	        		GUILink gl2 = new GUILink (null , gn , associatedGUINodes.get(associatedGUINodes.size() - 2));
	        		List<GUILink> existingList = intraNodeGUILinks.get(n);
	        		if (existingList == null) { existingList = new ArrayList<GUILink> (); intraNodeGUILinks.put(n , existingList); } 
	        		existingList.add(gl1); existingList.add(gl2);
	        	}
	        }
	        cache_nodeGuiNodeMap.put(n, associatedGUINodes);
		}
		for (VisualizationLayer vl : vLayers)
		{
			for (int vLayerIndex = 0 ; vLayerIndex < vl.npLayersToShow.size() ; vLayerIndex ++)
			{
				final NetworkLayer layer  = vl.npLayersToShow.get(vLayerIndex);
				for (Link e : currentNp.getLinks(layer))
				{
					final GUINode gn1 = cache_nodeGuiNodeMap.get(e.getOriginNode()).get(vLayerIndex);
					final GUINode gn2 = cache_nodeGuiNodeMap.get(e.getDestinationNode()).get(vLayerIndex);
					final GUILink gl1 = new GUILink (e , gn1 , gn2);
					regularLinkMap.put(e , gl1);
				}
			}
		}
	}
	
	public int getInterLayerDistanceInPixels () { return interLayerDistanceInPixels; }

    public boolean decreaseFontSizeAll()
    {
        boolean changedSize = false;
        for (VisualizationLayer vl : vLayers)
        	for (GUINode gn : vl.guiNodes)
        		changedSize |= gn.decreaseFontSize();
        return changedSize;
    }

    public void increaseFontSizeAll()
    {
        for (VisualizationLayer vl : vLayers)
        	for (GUINode gn : vl.guiNodes)
        		gn.increaseFontSize();
    }

    public void decreaseNodeSizeAll()
    {
        for (VisualizationLayer vl : vLayers)
        	for (GUINode gn : vl.guiNodes)
        		gn.setShapeSize(gn.getShapeSize() * SCALE_OUT);
    }

    public void increaseNodeSizeAll()
    {
        for (VisualizationLayer vl : vLayers)
        	for (GUINode gn : vl.guiNodes)
        		gn.setShapeSize(gn.getShapeSize() * SCALE_IN);
    }


    
	public int getNumberOfVisualizationLayers () { return vLayers.size(); }
	
	public void setVisualizationLayers (List<List<NetworkLayer>> listOfLayersPerVL , NetPlan netPlan)
	{
		if (listOfLayersPerVL.isEmpty()) throw new Net2PlanException ("At least one visualization layer is needed");
		Set<NetworkLayer> alreadyAppearingLayer = new HashSet<> ();
		for (List<NetworkLayer> layers : listOfLayersPerVL)
		{	
			if (layers.isEmpty()) throw new Net2PlanException ("A visualization layer cannot be empty");
			for (NetworkLayer layer : layers)
			{
				if (layer.getNetPlan() != netPlan) throw new RuntimeException ("Bad");
				if (alreadyAppearingLayer.contains(layer)) throw new Net2PlanException ("A layer cannot belong to more than one visualization layer");
				alreadyAppearingLayer.add(layer);
			}
		}
		this.currentNp = netPlan;
		this.vLayers.clear();
		for (List<NetworkLayer> layers : listOfLayersPerVL)
			vLayers.add(new VisualizationLayer(layers, this , vLayers.size()));
	}

	public List<VisualizationLayer> getVLList () { return Collections.unmodifiableList(vLayers); }
	
    public class VisualizationLayer
	{
    	private List<NetworkLayer> npLayersToShow;
    	private NetPlan currentNp;
    	private final VisualizationState vs;
    	private List<GUINode> guiNodes;
    	private List<GUILink> guiLinks;
    	private final int index;
    	
    	public VisualizationLayer(List<NetworkLayer> layers , VisualizationState vs , int index)
		{
    		if (layers.isEmpty()) throw new Net2PlanException ("A visualization layer needs at least on layer");
    		this.npLayersToShow = layers;
    		this.currentNp = layers.get(0).getNetPlan();
    		for (NetworkLayer l : layers) if (l.getNetPlan() != currentNp) throw new RuntimeException("Bad");
    		this.vs = vs;
    		this.index = index;
		}
    	public VisualizationLayer(NetworkLayer layer , VisualizationState vs , int index)
		{
    		this.currentNp = layer.getNetPlan();
    		this.npLayersToShow = Arrays.asList(layer);
    		this.vs = vs;
    		this.index = index;
		}
    	public VisualizationState getVisualizationState () { return vs; }

    	public List<GUINode> getGUINodes () { return Collections.unmodifiableList(guiNodes); }
    	public List<GUILink> getGUILinks () { return Collections.unmodifiableList(guiLinks); }
    	public int getIndex () { return index; }
    	
	}

	/**
	 * @return the showNodeNames
	 */
	public boolean isShowNodeNames()
	{
		return showNodeNames;
	}

	/**
	 * @param showNodeNames the showNodeNames to set
	 */
	public void setShowNodeNames(boolean showNodeNames)
	{
		this.showNodeNames = showNodeNames;
	}

	/**
	 * @return the showLinkLabels
	 */
	public boolean isShowLinkLabels()
	{
		return showLinkLabels;
	}

	/**
	 * @param showLinkLabels the showLinkLabels to set
	 */
	public void setShowLinkLabels(boolean showLinkLabels)
	{
		this.showLinkLabels = showLinkLabels;
	}

	/**
	 * @return the showNonConnectedNodes
	 */
	public boolean isShowNonConnectedNodes()
	{
		return showNonConnectedNodes;
	}

	/**
	 * @param showNonConnectedNodes the showNonConnectedNodes to set
	 */
	public void setShowNonConnectedNodes(boolean showNonConnectedNodes)
	{
		this.showNonConnectedNodes = showNonConnectedNodes;
	}
	

    public void resetPickedAndUserDefinedColorState()
    {
        for (List<GUINode> list : cache_nodeGuiNodeMap.values()) for (GUINode n : list) n.setUserDefinedColorOverridesTheRest(null);
        for (GUILink e : regularLinkMap.values())
        {
            e.setUserDefinedColorOverridesTheRest(null);
            e.setUserDefinedStrokeOverridesTheRest(null);
        }
        for (List<GUILink> list : intraNodeGUILinks.values())
        {
        	for (GUILink e : list)
	        {
	            e.setUserDefinedColorOverridesTheRest(null);
	            e.setUserDefinedStrokeOverridesTheRest(null);
	        }
        }
    }
	

    public void setAllLinksVisibilityState(boolean regularLinksVisible , boolean intraNodeLinksVisible)
    {
        for (GUILink e : this.regularLinkMap.values()) e.setVisible(regularLinksVisible);
        for (List<GUILink> list : this.intraNodeGUILinks.values()) for (GUILink e : list) e.setVisible(intraNodeLinksVisible);
        
    }

    public void setAllNodesVisibilityState(boolean visible)
    {
        for (List<GUINode> list : this.cache_nodeGuiNodeMap.values()) for (GUINode n : list) n.setVisible(visible);
    }

    @Override
    public void showAndPickNodesAndLinks(Map<Node, Color> npNodes, Map<Link, Pair<Color, Boolean>> npLinks)
    {
        resetPickedAndUserDefinedColorState();

        if (npNodes != null)
        {
            for (Entry<Node, Color> npNode : npNodes.entrySet())
            {
                GUINode aux = nodeTable.get(npNode.getKey());
                aux.setUserDefinedColorOverridesTheRest(npNode.getValue());
                vv.getPickedVertexState().pick(aux, true);
            }
        }

        if (npLinks != null)
        {
            for (Entry<Link, Pair<Color, Boolean>> link : npLinks.entrySet())
            {
                GUILink aux = linkTable.get(link.getKey());
                aux.setUserDefinedColorOverridesTheRest(link.getValue().getFirst());
                vv.getPickedEdgeState().pick(aux, true);
                if (link.getValue().getSecond()) // if true, the edge is dashed
                    aux.setUserDefinedStrokeOverridesTheRest(new BasicStroke(vv.getPickedEdgeState().isPicked(aux) ? 2 : 1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10}, 0.0f));
                else
                    aux.setUserDefinedStrokeOverridesTheRest(null);
            }
        }
        refresh();
    }

    @Override
    public void showNonConnectedNodes(boolean show)
    {
        if (showHideNonConnectedNodes != show)
        {
            showHideNonConnectedNodes = show;
            refresh();
        }
    }

}
