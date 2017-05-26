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
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Resource;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FigureResourcePanel extends FigureSequencePanel
{
	
	private Resource resource;
	private List<String> generalMessage; 
    private NetPlan np;
    private Dimension preferredSize;

    public FigureResourcePanel(GUINetworkDesign callback , Resource resource , String titleMessage)
    {
    	super(callback);
    	this.np = resource.getNetPlan();
   		this.generalMessage = Arrays.asList(titleMessage);
    	this.preferredSize = null;
    	this.resource = resource;
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
    	final int xOffset = initialXTitle * 3;
    	final int xSeparationDnCenters = maxHeightOrSizeIcon * 3;
    	final int thisResourceLeftPosition = xOffset + xSeparationDnCenters * (int) (Math.max(0.0 , Math.max(numBaseResources - 1 , numUpperResources - 1))/ 2.0);
    	final Point dnResourceTopLeftPosition = new Point (thisResourceLeftPosition , initialTopYOfThisResource);
    	final DrawNode dnResource = new DrawNode(resource , maxHeightOrSizeIcon , -1);
    	this.drawnNodes.add(dnResource);
    	DrawNode.addNodeToGraphics(g2d , dnResource , dnResourceTopLeftPosition , fontMetrics , regularInterlineSpacePixels , null);
    	
    	/* Now the upper resource and the links to the resource */
    	int xPositionResource = xOffset;
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
    	xPositionResource = xOffset;
        final Iterator<Resource> iterator = resource.getBaseResources().iterator();
        while (iterator.hasNext())
    	{
    	    final Resource baseResource = iterator.next();
    		/* Add the gn */
        	final Point dnTopLeftPosition = new Point (xPositionResource , initialTopYOfBaseResource);
        	final DrawNode dn = new DrawNode(baseResource , maxHeightOrSizeIcon , resource.getCapacityOccupiedInBaseResource(baseResource));
        	this.drawnNodes.add(dn);
        	final Dimension windowSize = DrawNode.addNodeToGraphics(g2d , dn , dnTopLeftPosition , fontMetrics , regularInterlineSpacePixels , null);
        	xPositionResource += xSeparationDnCenters;

    		/* Add the glink */
			final DrawLine dlNoURL = new DrawLine (dnResource , dn , dnResource.posSouth() , dn.posNorth());
			DrawLine.addLineToGraphics(g2d , dlNoURL , fontMetrics , regularInterlineSpacePixels);
			drawnLines.add(dlNoURL);

			if (!iterator.hasNext())
            {
                this.preferredSize = new Dimension(windowSize.width + XYMARGIN * 3, windowSize.height + XYMARGIN * 3);
            }
    	}
    }
}

