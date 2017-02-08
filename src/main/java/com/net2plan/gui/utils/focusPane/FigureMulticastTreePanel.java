package com.net2plan.gui.utils.focusPane;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JPanel;

import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;

public class FigureMulticastTreePanel extends FigureSequencePanel
{
	
	private MulticastTree tree;
	private List<String> generalMessage; 
    private NetPlan np;
    private Dimension preferredSize;

    public FigureMulticastTreePanel(IVisualizationCallback callback , MulticastTree tree , String titleMessage , double carriedTraffic) 
    {
    	super(callback);
    	this.np = tree.getNetPlan();
		this.generalMessage = Arrays.asList(titleMessage , "Carried trafffic: " + String.format("%.2f " , carriedTraffic) + " " + np.getDemandTrafficUnitsName(tree.getLayer()));
    	this.preferredSize = null;
    	this.tree = tree;
    }

    @Override
    protected void paintComponent(Graphics grphcs) 
    {
        final Graphics2D g2d = (Graphics2D) grphcs;
        g2d.setColor(Color.black);

        final int maxHeightOrSizeIcon = 40;
        final int initialXTitle = 20;
        final int initialYTitle = 20;
    	/* Initial messages */
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
    	final int fontHeightTitle = g2d.getFontMetrics().getHeight();
    	for (int indexMessage =0 ; indexMessage < generalMessage.size() ; indexMessage ++)
    	{
    		final String m = generalMessage.get(indexMessage);
        	g2d.drawString (m , initialXTitle , initialYTitle + (indexMessage * fontHeightTitle));
    	}

        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
    	final FontMetrics fontMetrics = g2d.getFontMetrics();
    	final int regularInterlineSpacePixels = fontMetrics.getHeight();
    	final int maxNumberOfTagsPerNode = 4;
        final int maxNumberOfHops = tree.getTreeMaximumPathLengthInHops();
        final int initialTopYOfFirstLineOfNodes = initialYTitle + (generalMessage.size() * fontHeightTitle) + 20;
        final int spaceBetweenVerticalNodes = maxHeightOrSizeIcon + regularInterlineSpacePixels * maxNumberOfTagsPerNode + 20;
    	final int xSeparationDnCenters = maxHeightOrSizeIcon * 4;

    	this.drawnNodes = new ArrayList<> ();
    	this.drawnLines = new ArrayList<> ();
    	final Point dnOriginTopLeftPosition = new Point (initialXTitle , initialTopYOfFirstLineOfNodes);
    	final DrawNode ingressDn = new DrawNode(tree.getIngressNode() , tree.getLayer() , maxHeightOrSizeIcon);
    	this.drawnNodes.add(ingressDn);
    	DrawNode.addNodeToGraphics(g2d , ingressDn , dnOriginTopLeftPosition , fontMetrics , regularInterlineSpacePixels , Color.GREEN);
        
    	BidiMap<Node,DrawNode> nodesToDnMap = new DualHashBidiMap<> ();
    	nodesToDnMap.put(tree.getIngressNode() , ingressDn);
        for (int numHops = 1 ; numHops <= maxNumberOfHops ; numHops ++)
        {
        	Set<Link> linksThisHopIndex = new HashSet<> ();
        	for (Node egressNode : tree.getEgressNodes())
        	{
        		final List<Link> seqLinks = tree.getSeqLinksToEgressNode(egressNode);
        		if (numHops-1 < seqLinks.size()) linksThisHopIndex.add(seqLinks.get(numHops-1));
        	}
        	final Set<Node> destinationNodesThisHopIndex = linksThisHopIndex.stream().map(e->e.getDestinationNode()).collect(Collectors.toSet());
        	if (destinationNodesThisHopIndex.size() != linksThisHopIndex.size()) throw new RuntimeException();
        	
        	int yPositionTopDn = initialTopYOfFirstLineOfNodes;
        	List<Link> sortedListUpToDown = linksThisHopIndex.stream().sorted((e1,e2)->
        		{ return Integer.compare(nodesToDnMap.get(e1.getOriginNode()).getPosTopLeftCornerToSetByPainter().y ,nodesToDnMap.get(e2.getOriginNode()).getPosTopLeftCornerToSetByPainter().y); }).collect(Collectors.toList());
        	for (Link e : sortedListUpToDown)
        	{
        		final Node n = e.getDestinationNode();
        		/* Add the node */
            	final Point dnTopLeftPosition = new Point (initialXTitle + numHops * xSeparationDnCenters , yPositionTopDn);
            	final DrawNode dn = new DrawNode(tree.getIngressNode() , tree.getLayer() , maxHeightOrSizeIcon);
            	this.drawnNodes.add(dn);
            	DrawNode.addNodeToGraphics(g2d , dn , dnTopLeftPosition , fontMetrics , regularInterlineSpacePixels , tree.getEgressNodes().contains(n)? Color.CYAN : null);
            	nodesToDnMap.put(n , dn);
            	yPositionTopDn += spaceBetweenVerticalNodes;

        		/* Add the link */
            	final DrawNode dnOrigin = nodesToDnMap.get(e.getOriginNode());
    			final DrawLine dlLink = new DrawLine (dnOrigin , dn , e , dnOrigin.posEast() , dn.posWest() , tree.getOccupiedLinkCapacity());
    			DrawLine.addLineToGraphics(g2d , dlLink , fontMetrics , regularInterlineSpacePixels);
    			drawnLines.add(dlLink);
        	}
        }
        
    }
}

