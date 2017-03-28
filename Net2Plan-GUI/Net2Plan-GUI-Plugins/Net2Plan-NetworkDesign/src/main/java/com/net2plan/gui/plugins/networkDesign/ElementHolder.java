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
import com.net2plan.utils.Pair;
import com.sun.istack.internal.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.net2plan.internal.Constants.NetworkElementType;

/**
 * @author Jorge San Emeterio Villalain
 * @date 28/03/17
 */
public class ElementHolder
{
    private NetworkElementType elementType;

    private final List<NetworkElement> networkElementList;
    private final List<Pair<Demand, Link>> forwardingRuleList;

    public ElementHolder(final NetworkElementType elementType)
    {
        this.elementType = elementType;

        if (elementType == NetworkElementType.FORWARDING_RULE)
        {
            this.networkElementList = Collections.unmodifiableList(Collections.emptyList());
            this.forwardingRuleList = new ArrayList<>();
        } else
        {
            this.networkElementList = new ArrayList<>();
            this.forwardingRuleList = Collections.unmodifiableList(Collections.emptyList());
        }
    }

    public ElementHolder(final NetworkElementType elementType, final List<NetworkElement> networkElements)
    {
        this.elementType = elementType;

        final NetworkElementType aux = getElementType(networkElements);
        if (aux != elementType) throw new RuntimeException("Given element type and list do not match up");

        this.networkElementList = new ArrayList<>(networkElements);
        this.forwardingRuleList = Collections.unmodifiableList(Collections.emptyList());
    }

    public ElementHolder(final List<Pair<Demand, Link>> forwardingRuleList)
    {
        this.elementType = NetworkElementType.FORWARDING_RULE;

        this.networkElementList = Collections.unmodifiableList(Collections.emptyList());
        this.forwardingRuleList = new ArrayList<>();
    }

    public boolean addElement(final NetworkElement element)
    {
        if (NetworkElementType.getType(element) != elementType) return false;
        networkElementList.add(element);
        return true;
    }

    public boolean addForwardingRule(final Pair<Demand, Link> forwardingRule)
    {
        if (elementType != NetworkElementType.FORWARDING_RULE) return false;
        forwardingRuleList.add(forwardingRule);
        return true;
    }

    public boolean addElements(final List<NetworkElement> elements)
    {
        boolean res = true;

        for (NetworkElement element : elements)
        {
            if (NetworkElementType.getType(element) != elementType)
            {
                res = false;
                continue;
            }

            networkElementList.add(element);
        }

        return res;
    }

    public boolean addForwardingRules(final List<Pair<Demand, Link>> forwardingRuleList)
    {
        if (elementType != NetworkElementType.FORWARDING_RULE) return false;
        forwardingRuleList.addAll(forwardingRuleList);
        return true;
    }

    public List<NetworkElement> getNetworkElements()
    {
        return Collections.unmodifiableList(networkElementList);
    }
    public List<Pair<Demand, Link>> getForwardingRules() { return Collections.unmodifiableList(forwardingRuleList); }
    public NetworkElementType getElementType()
    {
        return elementType;
    }

    @Nullable
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
