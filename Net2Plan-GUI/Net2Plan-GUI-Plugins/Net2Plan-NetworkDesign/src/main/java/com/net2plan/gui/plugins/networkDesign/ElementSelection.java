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

        final NetworkElementType aux = getElementType(networkElements);
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
        return selectionType == SelectionType.EMPTY || (networkElementList.isEmpty() && forwardingRuleList.isEmpty());
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

    public static NetworkElementType getElementType(final List<? extends NetworkElement> networkElements)
    {
        NetworkElementType res = null, aux = null;
        for (NetworkElement networkElement : networkElements)
        {
            aux = NetworkElementType.getType(networkElement);

            if (aux == null) throw new RuntimeException();
            if (res == null) res = aux;

            if (res != aux) return null;
        }

        return res;
    }
}
