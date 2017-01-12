package com.net2plan.gui.utils.topologyPane;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;

public class VisualizationState
{
    private final static float SCALE_IN = 1.1f;
    private final static float SCALE_OUT = 1 / SCALE_IN;
	private boolean showNodeNames;
	private boolean showLinkLabels;
    private boolean showNonConnectedNodes;
    private NetPlan currentNp;
    private List<VisualizationLayer> vLayers;
    private Map<Node,List<GUILink>> intraNodeGUILinks;
    private Map<Node,List<GUINode>> cache_nodeGuiNodeMap;
    private int interLayerDistanceInPixels;
    
    public NetPlan getNetPlan () { return currentNp; }

	public VisualizationState (NetPlan currentNp)
	{
		this.currentNp = currentNp;
		this.showNodeNames = false;
		this.showLinkLabels = true;
		this.showNonConnectedNodes = false;
		this.vLayers = new ArrayList<> ();
		this.vLayers.add(new VisualizationLayer(currentNp.getNetworkLayerDefault() , this));
		this.intraNodeGUILinks = new HashMap<> ();
		this.cache_nodeGuiNodeMap = new HashMap<> ();
		this.interLayerDistanceInPixels = 50;
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


    public void addNode(Node npNode) 
    {
        if (cache_nodeGuiNodeMap.containsKey(npNode)) throw new RuntimeException("Bad - Node " + npNode + " already exists");
        List<GUINode> associatedGUINodes = new ArrayList<> ();
        for (VisualizationState.VisualizationLayer vLayer : vLayers)
        {
        	GUINode gn = new GUINode(npNode , vLayer);
        	associatedGUINodes.add(gn);
        	if (associatedGUINodes.size() > 1)
        	{
        		GUILink gl1 = new GUILink (null , associatedGUINodes.get(associatedGUINodes.size() - 2), gn);
        		GUILink gl2 = new GUILink (null , gn , associatedGUINodes.get(associatedGUINodes.size() - 2));
        		List<GUILink> existingList = intraNodeGUILinks.get(npNode);
        		if (existingList == null) { existingList = new ArrayList<GUILink> (); intraNodeGUILinks.put(npNode , existingList); } 
        		existingList.add(gl1); existingList.add(gl2);
        	}
        }
        cache_nodeGuiNodeMap.put(npNode, associatedGUINodes);
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
			vLayers.add(new VisualizationLayer(layers, this));
	}

	public List<VisualizationLayer> getVLList () { return Collections.unmodifiableList(vLayers); }
	
    public class VisualizationLayer
	{
    	private List<NetworkLayer> npLayersToShow;
    	private NetPlan currentNp;
    	private final VisualizationState vs;
    	private List<GUINode> guiNodes;
    	private List<GUILink> guiLink;
    	
    	
    	public VisualizationLayer(List<NetworkLayer> layers , VisualizationState vs)
		{
    		if (layers.isEmpty()) throw new Net2PlanException ("A visualization layer needs at least on layer");
    		this.npLayersToShow = layers;
    		this.currentNp = layers.get(0).getNetPlan();
    		for (NetworkLayer l : layers) if (l.getNetPlan() != currentNp) throw new RuntimeException("Bad");
    		this.vs = vs;
		}
    	public VisualizationLayer(NetworkLayer layer , VisualizationState vs)
		{
    		this.currentNp = layer.getNetPlan();
    		this.npLayersToShow = Arrays.asList(layer);
    		this.vs = vs;
		}
    	public VisualizationState getVisualizationState () { return vs; }

    	public List<GUINode> getGUINodes () { return Collections.unmodifiableList(guiNodes); }
    	public List<GUILink> getGUILinks () { return Collections.unmodifiableList(guiLinks); }
    	
    	
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
	
	
}
