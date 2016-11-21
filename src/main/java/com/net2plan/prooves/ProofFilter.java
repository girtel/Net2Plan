package com.net2plan.prooves;

import com.net2plan.gui.utils.visualizationFilters.IVisualizationFilter;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import java.util.List;

/**
 * @author César
 * @date 20/11/2016
 */
public class ProofFilter implements IVisualizationFilter
{
    boolean active = true;

    @Override
    public boolean isVisibleNetworkElement(NetworkElement element)
    {
        if(element.getIndex() % 2 == 0) return true;

        return false;
    }

    @Override
    public boolean isVisibleForwardingRules(Pair<Demand, Link> fRuleKey, Double fRuleValue)
    {
        return false;
    }

    @Override
    public String getDescription()
    {
        return "Visible if index is even";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        return null;
    }

    @Override
    public String getUniqueName()
    {
        return "Filter namber uan";
    }

    @Override
    public boolean isActive()
    {
        return active;
    }

    @Override
    public void setActive(boolean flag)
    {
        active = flag;
    }
}
