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
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JPanel;

import com.google.common.collect.Sets;
import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.topologyPane.VisualizationState;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.Pair;

public class FigureResourcePanel extends JPanel 
{
	
	private List<DrawNode> drawnNodes;
	private List<DrawLine> drawnLines;
	private Resource resource;
	private List<String> generalMessage; 
    private NetPlan np;
    private Dimension preferredSize;
    private IVisualizationCallback callback;
    
    public FigureResourcePanel(IVisualizationCallback callback , Resource resource , String titleMessage) 
    {
    	this.callback = callback;
    	this.np = resource.getNetPlan();
   		this.generalMessage = Arrays.asList(titleMessage);
    	this.preferredSize = null;
    	this.resource = resource;
        addMouseListener(new MouseAdapterFocusPanel() );
    }

    @Override
    protected void paintComponent(Graphics grphcs) 
    {
        super.paintComponent(grphcs);
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
    	final int maxNumberOfTagsPerResource = 4;
        final int numBaseResources = resource.getBaseResources().size();
        final int numUpperResources = resource.getUpperResources().size();
        final int initialTopYOfUpperResources = initialYTitle + (generalMessage.size() * fontHeightTitle) + 20; 
        final int initialTopYOfThisResource = numUpperResources == 0? initialTopYOfUpperResources : initialTopYOfUpperResources + maxHeightOrSizeIcon + regularInterlineSpacePixels * maxNumberOfTagsPerResource + 20;
        final int initialTopYOfBaseResource = initialTopYOfThisResource + maxHeightOrSizeIcon + regularInterlineSpacePixels * maxNumberOfTagsPerResource + 20;

    	this.drawnNodes = new ArrayList<> ();
    	this.drawnLines = new ArrayList<> ();

    	/* First I draw the resource itself */
    	final int xSeparationDnCenters = maxHeightOrSizeIcon * 3;
    	final int thisResourceLeftPosition = initialXTitle + xSeparationDnCenters * (int) (Math.max(0.0 , Math.max(numBaseResources - 1 , numUpperResources - 1))/ 2.0);
    	final Point dnResourceTopLeftPosition = new Point (thisResourceLeftPosition , initialTopYOfThisResource);
    	final DrawNode dnResource = new DrawNode(resource , maxHeightOrSizeIcon , -1);
    	this.drawnNodes.add(dnResource);
    	DrawNode.addNodeToGraphics(g2d , dnResource , dnResourceTopLeftPosition , fontMetrics , regularInterlineSpacePixels , null);
    	
    	/* Now the upper resource and the links to the resource */
    	int xPositionResource = initialXTitle;
    	for (Resource upperResource : resource.getUpperResources())
    	{
    		/* Add the gn */
        	final Point dnTopLeftPosition = new Point (xPositionResource , initialTopYOfUpperResources);
        	final DrawNode dn = new DrawNode(upperResource , maxHeightOrSizeIcon , resource.getCapacityOccupiedByUpperResource(upperResource));
        	this.drawnNodes.add(dn);
        	DrawNode.addNodeToGraphics(g2d , dn , dnTopLeftPosition , fontMetrics , regularInterlineSpacePixels , null);
        	xPositionResource += xSeparationDnCenters;

    		/* Add the glink */
			final DrawLine dlNoURL = new DrawLine (dn , dnResource , dn.posSouth() , dnResource.posNorth());
			DrawLine.addLineToGraphics(g2d , dlNoURL , fontMetrics , regularInterlineSpacePixels);
			drawnLines.add(dlNoURL);
    	}

    	/* Now the base resource and the links from the resource */
    	xPositionResource = initialXTitle;
    	for (Resource baseResource : resource.getBaseResources())
    	{
    		/* Add the gn */
        	final Point dnTopLeftPosition = new Point (xPositionResource , initialTopYOfBaseResource);
        	final DrawNode dn = new DrawNode(baseResource , maxHeightOrSizeIcon , resource.getCapacityOccupiedInBaseResource(baseResource));
        	this.drawnNodes.add(dn);
        	DrawNode.addNodeToGraphics(g2d , dn , dnTopLeftPosition , fontMetrics , regularInterlineSpacePixels , null);
        	xPositionResource += xSeparationDnCenters;

    		/* Add the glink */
			final DrawLine dlNoURL = new DrawLine (dnResource , dn , dnResource.posSouth() , dn.posNorth());
			DrawLine.addLineToGraphics(g2d , dlNoURL , fontMetrics , regularInterlineSpacePixels);
			drawnLines.add(dlNoURL);
    	}
    }

    @Override
    public Dimension getPreferredSize() 
    {
        return new Dimension(600,600);
    }

    

    class MouseAdapterFocusPanel extends MouseAdapter
    {
        @Override
        public void mouseClicked(MouseEvent me) 
        {
            super.mouseClicked(me);
            for (DrawNode dn : drawnNodes)
            {
                if (dn.shapeIconToSetByPainter.contains(me.getPoint())) 
                	FocusPane.processMouseClickInternalLink ("node" + dn.associatedElement.getId() , callback);
                for (int labelIndex = 0; labelIndex < dn.labels.size() ; labelIndex ++)
                	if (dn.shapesLabelsToCreateByPainter.get(labelIndex).contains(me.getPoint())) 
                		FocusPane.processMouseClickInternalLink (dn.urlsLabels.get(labelIndex) , callback);
            }                
            for (DrawLine dl : drawnLines)
            {
                if (dl.shapeLineToCreateByPainter.contains(me.getPoint())) 
                	FocusPane.processMouseClickInternalLink ("link" + dl.associatedElement.getId() , callback);
                for (int labelIndex = 0; labelIndex < dl.labels.size() ; labelIndex ++)
                	if (dl.shapesLabelstoCreateByPainter.get(labelIndex).contains(me.getPoint())) 
                		FocusPane.processMouseClickInternalLink (dl.urlsLabels.get(labelIndex) , callback);
            }                
        }
    }

    
}

