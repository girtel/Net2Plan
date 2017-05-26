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
package com.net2plan.gui.plugins.networkDesign.focusPane;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FigureLinkSequencePanel extends FigureSequencePanel
{
    private List<? extends NetworkElement> path;
    private List<Double> occupationsPerElement;
    private List<Double> capacitiesPerElement;
    private List<String> generalMessage;
    private NetworkLayer layer;
    private NetPlan np;
    private Dimension preferredSize;

    public FigureLinkSequencePanel(GUINetworkDesign callback, List<? extends NetworkElement> path, NetworkLayer layer, List<Double> occupationsPerElement, double carriedTraffic, String... titleMessage)
    {
        super(callback);
        this.np = layer.getNetPlan();
        this.layer = layer;
        this.path = path;
        this.occupationsPerElement = occupationsPerElement;
        this.capacitiesPerElement = path.stream().map(e -> (e instanceof Link) ? ((Link) e).getCapacity() : ((Resource) e).getCapacity()).collect(Collectors.toList());
        if (carriedTraffic >= 0)
        {
            this.generalMessage = new ArrayList<>();
            this.generalMessage.addAll(Arrays.asList(titleMessage));
            this.generalMessage.add("Carried trafffic: " + String.format("%.2f ", carriedTraffic) + " " + np.getDemandTrafficUnitsName(layer));
        } else
        {
            this.generalMessage = Arrays.asList(titleMessage);
        }
        this.preferredSize = null;
    }

    @Override
    public Dimension getPreferredSize()
    {
        return preferredSize == null ? DEFAULT_DIMENSION : preferredSize;
    }

    @Override
    protected void paintComponent(Graphics grphcs)
    {
        final Graphics2D g2d = (Graphics2D) grphcs;
        g2d.setColor(Color.black);


        final int maxHeightOrSizeIcon = 40;
        final int maxNumberOfTagsPerNodeNorResource = 1;

    	/* Initial messages */
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        final int fontHeightTitle = g2d.getFontMetrics().getHeight();
        for (int indexMessage = 0; indexMessage < generalMessage.size(); indexMessage++)
        {
            final String m = generalMessage.get(indexMessage);
            g2d.drawString(m, maxHeightOrSizeIcon, maxHeightOrSizeIcon + (indexMessage * fontHeightTitle));
        }

        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final int regularInterlineSpacePixels = fontMetrics.getHeight();
        this.drawnNodes = new ArrayList<>();
        this.drawnLines = new ArrayList<>();

        final int topCoordinateLineNodes = maxHeightOrSizeIcon + (generalMessage.size() * fontHeightTitle) + (maxNumberOfTagsPerNodeNorResource * regularInterlineSpacePixels);
        final int topCoordinateLineResources = topCoordinateLineNodes + maxHeightOrSizeIcon * 4;
        final Point initialDnTopLeftPosition = new Point(maxHeightOrSizeIcon, topCoordinateLineNodes);
        final int xSeparationDnCenters = maxHeightOrSizeIcon * 3;

    	/* Initial dn */
    	final Node firstNode = path.get(0) instanceof Resource? ((Resource) path.get(0)).getHostNode() : ((Link) path.get(0)).getOriginNode();
    	this.drawnNodes.add(new DrawNode(firstNode , layer , maxHeightOrSizeIcon));
    	DrawNode.addNodeToGraphics(g2d , drawnNodes.get(0) , initialDnTopLeftPosition , fontMetrics , regularInterlineSpacePixels , null);

    	Dimension linkDimension = null, resourceDimension = null;
        for (int indexElementInPath = 0; indexElementInPath < path.size() ; indexElementInPath ++)
    	{
        	final NetworkElement e = path.get(indexElementInPath);
			final double occup = occupationsPerElement.get(indexElementInPath);

        	if (e instanceof Resource)
    		{
    			/* Draw the resource, there always are a previous node */
    			final Resource r = (Resource) e;
    			/* create resource node,with URL  */
    			final DrawNode dnResource = new DrawNode (r , maxHeightOrSizeIcon , occup);
                final Dimension windowDimension = DrawNode.addNodeToGraphics(g2d, dnResource, new Point(initialDnTopLeftPosition.x + (xSeparationDnCenters * drawnNodes.size()), topCoordinateLineResources), fontMetrics, regularInterlineSpacePixels, null);
                drawnNodes.add(dnResource);

    			/* create link from previous dn (resource of node) to here: no URL */
    			final DrawNode dnOrigin = drawnNodes.get(drawnNodes.size()-2);
    			final DrawNode dnDestination = drawnNodes.get(drawnNodes.size()-1);
    			final Point initialPoint = dnOrigin.getAssociatedElement() instanceof Resource? dnOrigin.posEast() : dnOrigin.posSouthSomeWest();
    			final Point endPoint = dnOrigin.getAssociatedElement() instanceof Resource? dnDestination.posWest() : dnDestination.posNorthSomeWest();
    			final DrawLine dlNoURL = new DrawLine (dnOrigin , dnDestination , initialPoint , endPoint);
    			DrawLine.addLineToGraphics(g2d , dlNoURL , fontMetrics , regularInterlineSpacePixels);
    			drawnLines.add(dlNoURL);

                resourceDimension = new Dimension(windowDimension.width + XYMARGIN, windowDimension.height + XYMARGIN);
    		}
    		else if (e instanceof Link)
    		{
    			final Link link = (Link) e;

    			DrawNode lastNodeElement = null;
    			for (int index = drawnNodes.size()-1 ; index >= 0 ; index --)
    				if (drawnNodes.get(index).getAssociatedElement() instanceof Node) { lastNodeElement = drawnNodes.get(index); break; }
    			if (lastNodeElement == null) throw new RuntimeException();

    			/* Get the previous node element added */
    			final DrawNode lastGn = drawnNodes.get(drawnNodes.size()-1);
    			if (lastGn.getAssociatedElement() instanceof Resource)
    			{
    				/* Add a link to the last resource to its host node */
    				final Point initialPoint = new Point (lastGn.posNorth().x + 5 , lastGn.posNorth().y);
    				final Point endPoint = lastNodeElement.posSouthSomeEast();
        			final DrawLine dlNoURL = new DrawLine (drawnNodes.get(drawnNodes.size()-1) , lastNodeElement , initialPoint , endPoint);
        			DrawLine.addLineToGraphics(g2d , dlNoURL , fontMetrics , regularInterlineSpacePixels);
        			drawnLines.add(dlNoURL);
    			}
    			
    			/* create node for link end node, with URL  */
    			final DrawNode dnNode = new DrawNode (link.getDestinationNode() , layer , maxHeightOrSizeIcon);
    			final Dimension windowDimension = DrawNode.addNodeToGraphics(g2d , dnNode , new Point (initialDnTopLeftPosition.x + (xSeparationDnCenters * drawnNodes.size()) , topCoordinateLineNodes) , fontMetrics , regularInterlineSpacePixels , null);
    			drawnNodes.add(dnNode);
    			
    			/* if the last element was a resource, add two links (res -> node [No URL], node->nextNode [URL]).
    			 * If not create just one link [URL] */
    			final DrawLine dlLink = new DrawLine (lastNodeElement , dnNode , link , lastNodeElement.posEast() , dnNode.posWest() , occup);
    			DrawLine.addLineToGraphics(g2d , dlLink , fontMetrics , regularInterlineSpacePixels);
    			drawnLines.add(dlLink);

                linkDimension = new Dimension (windowDimension.width + XYMARGIN , windowDimension.height + XYMARGIN);
            } else throw new RuntimeException();
    	}

    	if (resourceDimension == null)
        {
            if (linkDimension == null)
            {
                preferredSize = new Dimension(DEFAULT_DIMENSION.width + XYMARGIN, DEFAULT_HEIGHT + XYMARGIN);
            } else
            {
                preferredSize = new Dimension(linkDimension.width + XYMARGIN, linkDimension.height + XYMARGIN);
            }
        } else
        {
            if (linkDimension != null)
            {
    	        preferredSize = new Dimension(Math.max(linkDimension.width, resourceDimension.width), Math.max(linkDimension.height, resourceDimension.height));
            } else
            {
                preferredSize = new Dimension(resourceDimension.width + XYMARGIN, resourceDimension.height + XYMARGIN);
            }
        }
    }
}

