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
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.utils.Pair;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class FocusPanelHyperLinkListener implements HyperlinkListener
{
	public static final String SEPARATOR = "#";
	public static final String PREFIXNODE = "node";
	public static final String PREFIXLINK = "link";
	public static final String PREFIXDEMAND = "demand";
	public static final String PREFIXMULTICASTDEMAND = "mdemand";
	public static final String PREFIXROUTE = "route";
	public static final String PREFIXFR = "fr";
	public static final String PREFIXMULTICASTTREE = "tree";
	public static final String PREFIXRESOURCE = "resource";
	public static final String PREFIXSRG = "srg";
	public static final String PREFIXLAYER = "layer";
	public static final String PREFIXRESOURCETYPE = "resourceType";
	public static final String PREFIXINTERNALANCHOR = "internal";
	private GUINetworkDesign callback;
	private final JEditorPane ep;
	
	public FocusPanelHyperLinkListener (GUINetworkDesign callback , JEditorPane ep)
	{
		super ();
		this.callback = callback;
		this.ep = ep;
	}

    public void hyperlinkUpdate(HyperlinkEvent e) 
    {
    	final NetPlan np = callback.getDesign();
    	final VisualizationState vs = callback.getVisualizationState();

    	/* When clicked */
        if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
        {
        	final String hyperlinkURLFocusFormat = e.getDescription();
        	final String hlType = hyperlinkURLFocusFormat.substring(0 , hyperlinkURLFocusFormat.indexOf(SEPARATOR));
        	final String hlInfo = hyperlinkURLFocusFormat.substring(hyperlinkURLFocusFormat.indexOf(SEPARATOR) + 1);
        	System.out.println("hltype: " + hlType);
        	System.out.println("hlInfo: "+ hlInfo);
        	
        	if (hlType.equals(PREFIXINTERNALANCHOR))
        	{
        		ep.scrollToReference(hlInfo);
        	}
        	else if (hlType.equals(PREFIXNODE))
        		vs.pickElement (np.getNodeFromId(Long.parseLong(hlInfo)));
        	else if (hlType.equals(PREFIXLINK))
        		vs.pickElement(np.getLinkFromId(Long.parseLong(hlInfo)));
        	else if (hlType.equals(PREFIXDEMAND))
        		vs.pickElement(np.getDemandFromId(Long.parseLong(hlInfo)));
        	else if (hlType.equals(PREFIXMULTICASTDEMAND))
        		vs.pickElement(np.getMulticastDemandFromId(Long.parseLong(hlInfo)));
        	else if (hlType.equals(PREFIXROUTE))
        		vs.pickElement(np.getRouteFromId(Long.parseLong(hlInfo)));
        	else if (hlType.equals(PREFIXFR))
        	{
        		final Demand demand = np.getDemandFromId(Long.parseLong(hlInfo.substring(0 , hlInfo.indexOf(SEPARATOR))));
        		final Link link = np.getLinkFromId(Long.parseLong(hlInfo.substring(hlInfo.indexOf(SEPARATOR) + 1)));
        		vs.pickForwardingRule(Pair.of(demand,link));
        	}
        	else if (hlType.equals(PREFIXMULTICASTTREE))
        		vs.pickElement(np.getMulticastTreeFromId(Long.parseLong(hlInfo)));
        	else if (hlType.equals(PREFIXRESOURCE))
        		vs.pickElement(np.getResourceFromId(Long.parseLong(hlInfo)));
        	else if (hlType.equals(PREFIXSRG))
        		vs.pickElement(np.getSRGFromId(Long.parseLong(hlInfo)));
        	else if (hlType.equals(PREFIXLAYER))
        	{
        		final NetworkLayer layer = np.getNetworkLayerFromId(Long.parseLong(hlInfo));
        		np.setNetworkLayerDefault(layer);
        		vs.pickElement(layer);
        	} else if (hlType.equals(PREFIXRESOURCETYPE))
        	{
        	}
        	else throw new RuntimeException ();
        	
        	callback.updateVisualizationAfterPick();

        	
            System.out.println("Hyperlink clicked");
            System.out.println(e.getDescription()); // --> this is what I need to retrieve what is inside href, but may not be an URL
            System.out.println(e.getURL());
        }
    	/* When the mouse enters its area */
        else if(e.getEventType() == HyperlinkEvent.EventType.ENTERED)
        {
            System.out.println("Hyperlink ENTERED");
        }
    	/* When the mouse exits its area */
        else if(e.getEventType() == HyperlinkEvent.EventType.EXITED)
        {
            System.out.println("Hyperlink EXITED");
        }
    }

}
