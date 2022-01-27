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

package com.net2plan.gui.plugins.networkDesign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.Pair;

/**
 * @author Jorge San Emeterio Villalain
 * @date 28/03/17
 */
public class ElementSelection
{
    private NetworkElementType elementType;

    private final List<NetworkElement> networkElementList;
    private final List<Pair<Demand, Link>> forwardingRuleList;

    public ElementSelection()
    {
        this.elementType = null;
        this.networkElementList = Collections.unmodifiableList(Collections.emptyList());
        this.forwardingRuleList = Collections.unmodifiableList(Collections.emptyList());
    }

    public ElementSelection (final NetworkElementType elementType, final Collection<?> networkElementsOrForwRules)
    {
    	assert elementType != null;
    	this.elementType = elementType;
        this.networkElementList = new ArrayList<> ();
        this.forwardingRuleList = new ArrayList<> ();
    	for (Object ne : networkElementsOrForwRules)
    	{
    		if (ne instanceof NetworkElement)
    		{
    			final NetworkElement e = (NetworkElement) ne;
    			if (e.getNeType() == elementType) this.networkElementList.add(e);
    		}
    		else if (ne instanceof Pair)
    		{
    			if (elementType != NetworkElementType.FORWARDING_RULE) continue;
    			final Pair e = (Pair) ne;
    			if (e.getFirst() instanceof Demand && e.getSecond() instanceof Link)
    				this.forwardingRuleList.add(e);
    			else throw new RuntimeException ();
    		}
    		else throw new RuntimeException ();
    	}
    }
    
    public boolean isEmpty()
    {
        return (networkElementList.isEmpty() && forwardingRuleList.isEmpty()) || elementType == null;
    }

    public List<? extends NetworkElement> getNetworkElements()
    {
        return Collections.unmodifiableList(networkElementList);
    }

    public List<Pair<Demand, Link>> getForwardingRules()
    {
        return Collections.unmodifiableList(forwardingRuleList);
    }

    public NetworkElementType getElementType()
    {
        return elementType;
    }

    public ElementSelection invertSelection()
    {
        final ElementSelection elementSelection;
        // Check all elements belong to the same NetPlan
        NetPlan netPlan = null;
        for (NetworkElement networkElement : networkElementList)
        {
            if (netPlan == null) netPlan = networkElement.getNetPlan();
            else if (netPlan != networkElement.getNetPlan()) return null;
        }
        if (netPlan == null) return null;

        final List<NetworkElement> invertedElements;
        switch (elementType)
        {
            case NODE:
                invertedElements = new ArrayList<>(netPlan.getNodes());
                invertedElements.removeAll(getNetworkElements());
                elementSelection = new ElementSelection(elementType, invertedElements);
                break;
            case LINK:
                invertedElements = new ArrayList<>(netPlan.getLinks());
                invertedElements.removeAll(getNetworkElements());
                elementSelection = new ElementSelection(elementType, invertedElements);
                break;
            case DEMAND:
                invertedElements = new ArrayList<>(netPlan.getDemands());
                invertedElements.removeAll(getNetworkElements());
                elementSelection = new ElementSelection(elementType, invertedElements);
                break;
            case MULTICAST_DEMAND:
                invertedElements = new ArrayList<>(netPlan.getMulticastDemands());
                invertedElements.removeAll(getNetworkElements());
                elementSelection = new ElementSelection(elementType, invertedElements);
                break;
            case ROUTE:
                invertedElements = new ArrayList<>(netPlan.getRoutes());
                invertedElements.removeAll(getNetworkElements());
                elementSelection = new ElementSelection(elementType, invertedElements);
                break;
            case MULTICAST_TREE:
                invertedElements = new ArrayList<>(netPlan.getMulticastTrees());
                invertedElements.removeAll(getNetworkElements());
                elementSelection = new ElementSelection(elementType, invertedElements);
                break;
            case RESOURCE:
                invertedElements = new ArrayList<>(netPlan.getResources());
                invertedElements.removeAll(getNetworkElements());
                elementSelection = new ElementSelection(elementType, invertedElements);
                break;
            case SRG:
                invertedElements = new ArrayList<>(netPlan.getSRGs());
                invertedElements.removeAll(getNetworkElements());
                elementSelection = new ElementSelection(elementType, invertedElements);
                break;
            case FORWARDING_RULE:
                final List<Pair<Demand, Link>> forwardingRules = new ArrayList<>(netPlan.getForwardingRules().keySet());
                forwardingRules.removeAll(getForwardingRules());
                elementSelection = new ElementSelection(NetworkElementType.FORWARDING_RULE , forwardingRules);
                break;
            default:
                return null;
        }

        return elementSelection;
    }
}
