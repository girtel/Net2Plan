package com.net2plan.gui.utils.topologyPane;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Pair;

public class VisualizationState
{
    public final static float SCALE_IN = 1.1f;
    public final static float SCALE_OUT = 1 / SCALE_IN;
    
    public final static Paint DEFAULT_GUINODE_DRAWCOLOR = java.awt.Color.BLACK;
    public final static Paint DEFAULT_GUINODE_FILLCOLOR = java.awt.Color.BLACK;
    public final static Paint DEFAULT_GUINODE_FILLCOLOR_PICKED = java.awt.Color.BLACK;
    public final static Font DEFAULT_GUINODE_FONT = new Font("Helvetica", Font.BOLD, 11);
    public final static int DEFAULT_GUINODE_SHAPESIZE = 30;
    public final static Shape DEFAULT_GUINODE_SHAPE = new Ellipse2D.Double(-1 * DEFAULT_GUINODE_SHAPESIZE / 2, -1 * DEFAULT_GUINODE_SHAPESIZE / 2, 1 * DEFAULT_GUINODE_SHAPESIZE, 1 * DEFAULT_GUINODE_SHAPESIZE);
    public final static Shape DEFAULT_GUINODE_SHAPE_PICKED = new Ellipse2D.Double(-1.2 * DEFAULT_GUINODE_SHAPESIZE / 2, -1.2 * DEFAULT_GUINODE_SHAPESIZE / 2, 1.2 * DEFAULT_GUINODE_SHAPESIZE, 1.2 * DEFAULT_GUINODE_SHAPESIZE);

    public final static boolean DEFAULT_REGGUILINK_HASARROW = false;
    public final static Stroke DEFAULT_REGGUILINK_ARROWSTROKE = new BasicStroke(1);
    public final static Stroke DEFAULT_REGGUILINK_ARROWSTROKE_PICKED = new BasicStroke(2);
    public final static Stroke DEFAULT_REGGUILINK_EDGETROKE = new BasicStroke(3);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_PICKED = new BasicStroke(5);
    public final static Paint DEFAULT_REGGUILINK_ARROWDRAWCOLOR = Color.BLACK;
    public final static Paint DEFAULT_REGGUILINK_ARROWDRAWCOLOR_PICKED = Color.BLUE;
    public final static Paint DEFAULT_REGGUILINK_ARROWFILLCOLOR = Color.BLACK;
    public final static Paint DEFAULT_REGGUILINK_EDGEDRAWCOLOR = Color.BLACK;
    public final static Paint DEFAULT_REGGUILINK_EDGEDRAWCOLOR_PICKED = Color.BLUE;
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_BACKUP = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_BACKUP_PICKED = new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
    
    public final static boolean DEFAULT_INTRANODEGUILINK_HASARROW = false;
    public final static Stroke DEFAULT_INTRANODEGUILINK_ARROWSTROKE = new BasicStroke(0.5f);
    public final static Stroke DEFAULT_INTRANODEGUILINK_ARROWSTROKE_PICKED = new BasicStroke(2);
    public final static Stroke DEFAULT_INTRANODEGUILINK_EDGETROKE = new BasicStroke(1.5f);
    public final static Stroke DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED = new BasicStroke(2.5f);
    public final static Paint DEFAULT_INTRANODEGUILINK_ARROWDRAWCOLOR = Color.BLACK;
    public final static Paint DEFAULT_INTRANODEGUILINK_ARROWDRAWCOLOR_PICKED = Color.BLUE;
    public final static Paint DEFAULT_INTRANODEGUILINK_ARROWFILLCOLOR = Color.BLACK;
    public final static Paint DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR = Color.BLACK;
    public final static Paint DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR_PICKED = Color.BLUE;
    public final static Stroke DEFAULT_REGGUILINK_INTRANODEGESTROKE_BACKUP = new BasicStroke(0.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
    public final static Stroke DEFAULT_REGGUILINK_INTRANODEGESTROKE_BACKUP_PICKED = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);

    private boolean showNodeNames;
	private boolean showLinkLabels;
    private boolean showNonConnectedNodes;
    private NetPlan currentNp;
    private List<VisualizationLayer> vLayers;
    private Map<Node,Map<Pair<VisualizationLayer,VisualizationLayer>,GUILink>> cache_perNodeIntraNodeGUILinkMap;
    private Map<Node,Set<GUILink>> intraNodeGUILinks;
    private Map<Node,List<GUINode>> cache_nodeGuiNodeMap;
    private Map<Link,GUILink> regularLinkMap;
    private int interLayerDistanceInPixels;
    private Map<NetworkLayer,VisualizationLayer> cache_layer2VLayerMap;
    private boolean isNetPlanEditable;
    
    
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
		this.cache_perNodeIntraNodeGUILinkMap = new HashMap <> ();
		this.cache_nodeGuiNodeMap = new HashMap<> ();
		this.interLayerDistanceInPixels = 50;
		this.regularLinkMap = new HashMap<> ();
		this.cache_layer2VLayerMap = new HashMap<> (); 
		for (VisualizationLayer visualizationLayer : vLayers) 
			for (NetworkLayer layer : visualizationLayer.npLayersToShow) 
				cache_layer2VLayerMap.put(layer , visualizationLayer);
		this.isNetPlanEditable = true;
	}

	
	/**
	 * @return the isNetPlanEditable
	 */
	public boolean isNetPlanEditable()
	{
		return isNetPlanEditable;
	}

	/**
	 * @param isNetPlanEditable the isNetPlanEditable to set
	 */
	public void setNetPlanEditable(boolean isNetPlanEditable)
	{
		this.isNetPlanEditable = isNetPlanEditable;
	}

	public List<GUINode> getVerticallyStackedGUINodes (Node n) { return cache_nodeGuiNodeMap.get(n); } 
	
	public GUINode getAssociatedGUINode (Node n , NetworkLayer layer) 
	{ 
		return getVerticallyStackedGUINodes(n).get(getAssociatedVisualizationLayer(layer).getIndex());
	} 

	public GUILink getAssociatedGUILink (Link e) { return regularLinkMap.get(e); } 

	public Pair<Set<GUILink>,Set<GUILink>> getAssociatedGUILinksIncludingCoupling (Link e , boolean regularLinkIsPrimary) 
	{
		Set<GUILink> resPrimary = new HashSet<> ();
		Set<GUILink> resBackup = new HashSet<> ();
		if (regularLinkIsPrimary) resPrimary.add (getAssociatedGUILink(e)); else resBackup.add(getAssociatedGUILink(e));
		if (!e.isCoupled()) return Pair.of(resPrimary , resBackup);
		if (e.getCoupledDemand() != null)
		{
			/* add the intranode links */
			final NetworkLayer upperLayer = e.getLayer();
			final NetworkLayer downLayer = e.getCoupledDemand().getLayer();
			if (regularLinkIsPrimary)
			{
				resPrimary.addAll(getIntraNodeGUILinkSequence(e.getOriginNode() , upperLayer , downLayer));
				resPrimary.addAll(getIntraNodeGUILinkSequence(e.getDestinationNode() , downLayer , upperLayer));
			}
			else 
			{
				resBackup.addAll(getIntraNodeGUILinkSequence(e.getOriginNode() , upperLayer , downLayer));
				resBackup.addAll(getIntraNodeGUILinkSequence(e.getDestinationNode() , downLayer , upperLayer));
			}

			/* add the regular links */
			Pair<Set<Link>,Set<Link>> traversedLinks = e.getCoupledDemand().getLinksThisLayerPotentiallyCarryingTraffic(true); 
			for (Link ee : traversedLinks.getFirst())
			{
				Pair<Set<GUILink>,Set<GUILink>> pairGuiLinks = getAssociatedGUILinksIncludingCoupling (ee , true); 
				if (regularLinkIsPrimary) resPrimary.addAll(pairGuiLinks.getFirst()); else resBackup.addAll(pairGuiLinks.getFirst());  
				resBackup.addAll(pairGuiLinks.getSecond());
			}
			for (Link ee : traversedLinks.getSecond())
			{
				Pair<Set<GUILink>,Set<GUILink>> pairGuiLinks = getAssociatedGUILinksIncludingCoupling (ee , false); 
				resPrimary.addAll(pairGuiLinks.getFirst());
				resBackup.addAll(pairGuiLinks.getSecond());
			}
		}
		else if (e.getCoupledMulticastDemand() != null)
		{
			/* add the intranode links */
			final NetworkLayer upperLayer = e.getLayer();
			final MulticastDemand lowerLayerDemand = e.getCoupledMulticastDemand(); 
			final NetworkLayer downLayer = lowerLayerDemand.getLayer();
			if (regularLinkIsPrimary)
			{
				resPrimary.addAll(getIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode() , upperLayer , downLayer));
				resPrimary.addAll(getIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode() , downLayer , upperLayer));
				for (Node n : lowerLayerDemand.getEgressNodes())
				{
					resPrimary.addAll(getIntraNodeGUILinkSequence(n , upperLayer , downLayer));
					resPrimary.addAll(getIntraNodeGUILinkSequence(n , downLayer , upperLayer));
				}
			}
			else 
			{
				resBackup.addAll(getIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode() , upperLayer , downLayer));
				resBackup.addAll(getIntraNodeGUILinkSequence(lowerLayerDemand.getIngressNode() , downLayer , upperLayer));
				for (Node n : lowerLayerDemand.getEgressNodes())
				{
					resBackup.addAll(getIntraNodeGUILinkSequence(n , upperLayer , downLayer));
					resBackup.addAll(getIntraNodeGUILinkSequence(n , downLayer , upperLayer));
				}
			}

			for (MulticastTree t : lowerLayerDemand.getMulticastTrees())
				for (Link ee : t.getLinkSet())
				{
					Pair<Set<GUILink>,Set<GUILink>> pairGuiLinks = getAssociatedGUILinksIncludingCoupling (ee , true); 
					resPrimary.addAll(pairGuiLinks.getFirst());
					resBackup.addAll(pairGuiLinks.getSecond());
				}
			}
		return Pair.of(resPrimary,resBackup);
	} 

	public GUILink getIntraNodeGUILink (Node n , VisualizationLayer from , VisualizationLayer to)
	{
		return cache_perNodeIntraNodeGUILinkMap.get(n).get(Pair.of(from,to));
	}
	
	public Set<GUILink> getIntraNodeGUILinks (Node n) { return intraNodeGUILinks.get(n); } 

	public VisualizationLayer getAssociatedVisualizationLayer (NetworkLayer layer) { return cache_layer2VLayerMap.get(layer); }
	
	public List<GUILink> getIntraNodeGUILinkSequence (Node n , NetworkLayer from , NetworkLayer to) 
	{
		if (from.getNetPlan() != currentNp) throw new RuntimeException ("Bad");
		if (to.getNetPlan() != currentNp) throw new RuntimeException ("Bad");
		final List<GUILink> res = new LinkedList<> ();
		final VisualizationLayer vLayerFrom = cache_layer2VLayerMap.get(from);
		final VisualizationLayer vLayerTo = cache_layer2VLayerMap.get(to);
		if (vLayerFrom == vLayerTo) return res;
		final int increment = vLayerTo.getIndex() > vLayerFrom.getIndex()? 1 : -1; 
		int vLayerIndex = vLayerFrom.getIndex();
		do
		{
			final VisualizationLayer origin = vLayers.get(vLayerIndex);
			final VisualizationLayer destination = vLayers.get(vLayerIndex+increment);
			res.add(cache_perNodeIntraNodeGUILinkMap.get(n).get(Pair.of(origin,destination)));
			vLayerIndex += increment;
		} while (vLayerIndex != vLayerTo.getIndex());
		
		return res; 
	} 

	
	public void rebuildVisualizationState (NetPlan newCurrentNetPlan)
	{
		if (newCurrentNetPlan == null) throw new RuntimeException("Trying to update an empty topology");
		this.currentNp = newCurrentNetPlan;
		
		this.vLayers = new ArrayList<> ();
		this.vLayers.add(new VisualizationLayer(currentNp.getNetworkLayerDefault() , this , vLayers.size()));
		this.intraNodeGUILinks = new HashMap<> ();
		this.cache_perNodeIntraNodeGUILinkMap = new HashMap <> ();
		this.cache_nodeGuiNodeMap = new HashMap<> ();
		this.regularLinkMap = new HashMap<> ();
		this.cache_layer2VLayerMap = new HashMap<> (); 
		for (VisualizationLayer visualizationLayer : vLayers) 
			for (NetworkLayer layer : visualizationLayer.npLayersToShow) 
				cache_layer2VLayerMap.put(layer , visualizationLayer);
		
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
	        		Set<GUILink> existingGUILinksSet = intraNodeGUILinks.get(n);
	        		if (existingGUILinksSet == null) { existingGUILinksSet = new HashSet<> (); intraNodeGUILinks.put(n , existingGUILinksSet); } 
	        		existingGUILinksSet.add(gl1); existingGUILinksSet.add(gl2);
	        		GUILink check = cache_perNodeIntraNodeGUILinkMap.get(n).put(Pair.of(gl1.getOriginNode().getVisualizationLayer() , gl1.getDestinationNode().getVisualizationLayer()) , gl1);
	        		if (check != null) throw new RuntimeException ("Bad");
	        		check = cache_perNodeIntraNodeGUILinkMap.get(n).put(Pair.of(gl2.getOriginNode().getVisualizationLayer() , gl2.getDestinationNode().getVisualizationLayer()) , gl2);
	        		if (check != null) throw new RuntimeException ("Bad");
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

		this.cache_layer2VLayerMap = new HashMap<> (); 
		for (VisualizationLayer visualizationLayer : vLayers) 
			for (NetworkLayer layer : visualizationLayer.npLayersToShow) 
				cache_layer2VLayerMap.put(layer , visualizationLayer);

	}

	public List<VisualizationLayer> getVLList () { return Collections.unmodifiableList(vLayers); }
	
    public class VisualizationLayer
	{
    	private List<NetworkLayer> npLayersToShow;
    	private NetPlan currentNp;
    	private final VisualizationState vs;
    	private List<GUINode> guiNodes;
    	private List<GUILink> guiIntraLayerLinks;
    	private final int index;
    	
    	public int getNumberOfNetPlanLayers () { return npLayersToShow.size(); }
    	
    	public List<NetworkLayer> getNetPlanLayers () { return Collections.unmodifiableList(npLayersToShow); }

    	public VisualizationLayer(List<NetworkLayer> layers , VisualizationState vs , int index)
		{
    		if (layers.isEmpty()) throw new Net2PlanException ("A visualization layer needs at least on layer");
    		this.npLayersToShow = layers;
    		this.currentNp = layers.get(0).getNetPlan();
    		for (NetworkLayer l : layers) if (l.getNetPlan() != currentNp) throw new RuntimeException("Bad");
    		this.vs = vs;
    		this.index = index;
    		this.updateGUINodeAndGUILinks();
		}

    	public VisualizationLayer(NetworkLayer layer , VisualizationState vs , int index)
		{
    		this.currentNp = layer.getNetPlan();
    		this.npLayersToShow = Collections.singletonList(layer);
    		this.vs = vs;
    		this.index = index;
    		this.updateGUINodeAndGUILinks();
		}
    	public VisualizationState getVisualizationState () { return vs; }

    	public List<GUINode> getGUINodes () { return Collections.unmodifiableList(guiNodes); }
    	public List<GUILink> getGUIIntraLayerLinks () { return Collections.unmodifiableList(guiIntraLayerLinks); }
    	public int getIndex () { return index; }
    	private void updateGUINodeAndGUILinks ()
    	{
			this.guiNodes = new ArrayList<>();
			this.guiIntraLayerLinks = new ArrayList<>();
			for (NetworkLayer layer : npLayersToShow)
			{
				for (Node n : currentNp.getNodes())
					guiNodes.add(vs.getAssociatedGUINode(n , layer));
				for (Link e : currentNp.getLinks(layer))
					guiIntraLayerLinks.add(vs.getAssociatedGUILink(e));
			}
    	}
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
	
	public void setVisibilityState (Node n , boolean isVisible)
	{
		for (GUINode gn : cache_nodeGuiNodeMap.get(n)) gn.setVisible(isVisible);
	}

	public void setVisibilityState (Link e , boolean isVisible)
	{
		GUILink gn = regularLinkMap.get(e); if (gn == null) throw new RuntimeException ("Bad");
		gn.setVisible(isVisible);
	}

	/* Everything to its default color, shape. Separated nodes, are set together again. Visibility state is unchanged */
	public void resetColorAndShapeState()
    {
		for (GUINode n : getAllGUINodes())
		{
		    //n.setVisible(true);
		    n.setFont(DEFAULT_GUINODE_FONT);
		    n.setDrawPaint(DEFAULT_GUINODE_DRAWCOLOR);
		    n.setFillPaint(DEFAULT_GUINODE_FILLCOLOR);
		    n.setShape(DEFAULT_GUINODE_SHAPE);
		    n.setShapeSize(DEFAULT_GUINODE_SHAPESIZE);
		}
        for (GUILink e : getAllGUILinks(true,false))
        {
        	//e.setVisible(true);
        	e.setHasArrow(DEFAULT_REGGUILINK_HASARROW);
        	e.setArrowStroke(DEFAULT_REGGUILINK_ARROWSTROKE);
        	e.setEdgeStroke(DEFAULT_REGGUILINK_EDGETROKE);
        	e.setArrowDrawPaint(DEFAULT_REGGUILINK_ARROWDRAWCOLOR);
        	e.setArrowFillPaint(DEFAULT_REGGUILINK_ARROWFILLCOLOR);
        	e.setEdgeDrawPaint(DEFAULT_REGGUILINK_EDGEDRAWCOLOR);
        	e.setShownSeparated(false);
        }
    	for (GUILink e : getAllGUILinks(false,true))
        {
        	e.setVisible(true);
        	e.setHasArrow(DEFAULT_INTRANODEGUILINK_HASARROW);
        	e.setArrowStroke(DEFAULT_INTRANODEGUILINK_ARROWSTROKE);
        	e.setEdgeStroke(DEFAULT_INTRANODEGUILINK_EDGETROKE);
        	e.setArrowDrawPaint(DEFAULT_INTRANODEGUILINK_ARROWDRAWCOLOR);
        	e.setArrowFillPaint(DEFAULT_INTRANODEGUILINK_ARROWFILLCOLOR);
        	e.setEdgeDrawPaint(DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR);
        	e.setShownSeparated(false);
        }
    }
	
	public Set<GUILink> getAllGUILinks (boolean includeRegularLinks , boolean includeInterLayerLinks)
	{
		Set<GUILink> res = new HashSet<> ();
		if (includeRegularLinks) res.addAll(regularLinkMap.values());
		if (includeInterLayerLinks) for (Node n : currentNp.getNodes()) res.addAll(this.intraNodeGUILinks.get(n));
		return res;
	}

	public Set<GUINode> getAllGUINodes () 
	{
		Set<GUINode> res = new HashSet<> ();
        for (List<GUINode> list : this.cache_nodeGuiNodeMap.values()) res.addAll(list);
		return res;
	}
	

    public void setNodeProperties (Collection<GUINode> nodes , Color color , Shape shape , double shapeSize)
    {
    	for (GUINode n : nodes)
    	{
    		if (color != null) { n.setDrawPaint(color); n.setFillPaint(color); }
    		if (shape != null) { n.setShape(shape); }
    		if (shapeSize > 0) { n.setShapeSize(shapeSize); }
    	}
    }

    public void setLinkProperties (Collection<GUILink> links , Color color , Stroke stroke , Boolean hasArrows , Boolean shownSeparated)
    {
    	for (GUILink e : links)
    	{
    		if (color != null) { e.setArrowDrawPaint(color); e.setArrowFillPaint(color); e.setEdgeDrawPaint(color);}
    		if (stroke != null) { e.setArrowStroke(stroke); e.setEdgeStroke(stroke);}
    		if (hasArrows != null) { e.setHasArrow(hasArrows);}
    		if (shownSeparated != null) { e.setShownSeparated(shownSeparated); }
    	}
    }

}
