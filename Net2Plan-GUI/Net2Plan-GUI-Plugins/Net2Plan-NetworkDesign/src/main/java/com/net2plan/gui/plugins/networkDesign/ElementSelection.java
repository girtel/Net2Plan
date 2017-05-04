/*
 * ******************************************************************************
 *  * Copyright (c) 2017 Pablo Pavon-Marino.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the GNU Lesser Public License v3.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.gnu.org/licenses/lgpl.html
 *  *
 *  * Contributors:
 *  *     Pablo Pavon-Marino - Jose-Luis Izquierdo-Zaragoza, up to version 0.3.1
 *  *     Pablo Pavon-Marino - from version 0.4.0 onwards
 *  *     Pablo Pavon Marino - Jorge San Emeterio Villalain, from version 0.4.1 onwards
 *  *****************************************************************************
 */

package com.net2plan.gui.plugins.networkDesign;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.Pair;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Jorge San Emeterio Villalain
 * @date 28/03/17
 */
public class ElementSelection
{
    private NetworkElementType elementType;

    private final List<NetworkElement> networkElementList;
    private final List<Pair<Demand, Link>> forwardingRuleList;

    private final SelectionType selectionType;

    public enum SelectionType
    {
        EMPTY, NETWORK_ELEMENT, FORWARDING_RULE
    }

    public ElementSelection()
    {
        this.selectionType = SelectionType.EMPTY;

        this.elementType = null;
        this.networkElementList = Collections.unmodifiableList(Collections.emptyList());
        this.forwardingRuleList = Collections.unmodifiableList(Collections.emptyList());
    }

    public ElementSelection(final NetworkElementType elementType, final List<? extends NetworkElement> networkElements)
    {
        if (elementType == null) throw new NullPointerException();
        this.elementType = elementType;

        this.selectionType = SelectionType.NETWORK_ELEMENT;

        final NetworkElementType aux = NetworkElementType.getType(networkElements);
        if (aux == null) throw new RuntimeException("All elements in selection do not belong to the same type");
        if (aux != this.elementType) throw new RuntimeException("Given element type and list do not match up");

        this.networkElementList = new ArrayList<>(networkElements);
        this.forwardingRuleList = Collections.unmodifiableList(Collections.emptyList());
    }

    public ElementSelection(final List<Pair<Demand, Link>> forwardingRuleList)
    {
        this.elementType = NetworkElementType.FORWARDING_RULE;

        this.selectionType = SelectionType.FORWARDING_RULE;

        this.networkElementList = Collections.unmodifiableList(Collections.emptyList());
        this.forwardingRuleList = new ArrayList<>(forwardingRuleList);
    }

    public boolean isEmpty()
    {
        return selectionType == SelectionType.EMPTY || (networkElementList.isEmpty() && forwardingRuleList.isEmpty()) || elementType == null;
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

    public SelectionType getSelectionType()
    {
        return selectionType;
    }


    public ElementSelection invertSelection()
    {
        if (selectionType == SelectionType.EMPTY) return null;

        final ElementSelection elementSelection;
        if (selectionType == SelectionType.NETWORK_ELEMENT)
        {
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
                    break;
                case LINK:
                    invertedElements = new ArrayList<>(netPlan.getLinks());
                    invertedElements.removeAll(getNetworkElements());
                    break;
                case DEMAND:
                    invertedElements = new ArrayList<>(netPlan.getDemands());
                    invertedElements.removeAll(getNetworkElements());
                    break;
                case MULTICAST_DEMAND:
                    invertedElements = new ArrayList<>(netPlan.getMulticastDemands());
                    invertedElements.removeAll(getNetworkElements());
                    break;
                case ROUTE:
                    invertedElements = new ArrayList<>(netPlan.getRoutes());
                    invertedElements.removeAll(getNetworkElements());
                    break;
                case MULTICAST_TREE:
                    invertedElements = new ArrayList<>(netPlan.getMulticastTrees());
                    invertedElements.removeAll(getNetworkElements());
                    break;
                case RESOURCE:
                    invertedElements = new ArrayList<>(netPlan.getResources());
                    invertedElements.removeAll(getNetworkElements());
                    break;
                case SRG:
                    invertedElements = new ArrayList<>(netPlan.getSRGs());
                    invertedElements.removeAll(getNetworkElements());
                    break;
                default:
                    return null;
            }

            elementSelection = new ElementSelection(elementType, invertedElements);
        } else
        {
            NetPlan netPlan = null;
            for (Pair<Demand, Link> forwardingRule : forwardingRuleList)
            {
                if (netPlan == null) netPlan = forwardingRule.getFirst().getNetPlan();
                else if (netPlan != forwardingRule.getFirst().getNetPlan()) return null;
            }
            if (netPlan == null) return null;

            final List<Pair<Demand, Link>> forwardingRules = new ArrayList<>(netPlan.getForwardingRules().keySet());
            forwardingRules.removeAll(getForwardingRules());
            elementSelection = new ElementSelection(forwardingRules);
        }

        return elementSelection;
    }
}
