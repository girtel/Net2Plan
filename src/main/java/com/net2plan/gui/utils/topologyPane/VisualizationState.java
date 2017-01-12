package com.net2plan.gui.utils.topologyPane;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;

public class VisualizationState
{
	private boolean showNodeNames = false;
	private boolean showLinkLabels = true;
    private boolean showNonConnectedNodes = false;
    private NetPlan currentNp;
    private List<VisualizationLayer> vLayers;
    private Map<Node,Font> fonts;
    
    
    
    public NetPlan getNetPlan () { return currentNp; }

	public VisualizationState (NetPlan currentNp)
	{
		this.currentNp = currentNp;
		this.showNodeNames = false;
		this.showLinkLabels = true;
		this.showNonConnectedNodes = false;
		this.vLayers = new ArrayList<> ();
		this.vLayers.add(new VisualizationLayer(currentNp.getNetworkLayerDefault() , this));
	}

	
	
	/**
	 * @return the fontSize
	 */
	public Font getFont(Node node)
	{
		if (node.getNetPlan() != currentNp) throw new RuntimeException("Bad");
		return fonts.get(node);
	}

    public boolean decreaseFontSize()
    {
        boolean changedSize = false;
        for (Node n : fonts.keySet())
        {
        	final int currentSize = fonts.get(n).getSize(); 
        	if (currentSize > 1) { fonts.put(n , new Font("Helvetica" , Font.PLAIN , currentSize - 1)); changedSize = true; }
        }
        return changedSize;
    }

    public void increaseFontSize()
    {
        for (Node n : fonts.keySet())
        {
        	final int currentSize = fonts.get(n).getSize(); 
        	if (currentSize > 1) { fonts.put(n , new Font("Helvetica" , Font.PLAIN , currentSize + 1));  }
        }
    }

    
	/**
	 * @param fontSize the fontSize to set
	 */
	public void setFontSize(Node node , Font font)
	{
		if (node.getNetPlan() != currentNp) throw new RuntimeException("Bad");
		this.fonts.put(node, font);
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
