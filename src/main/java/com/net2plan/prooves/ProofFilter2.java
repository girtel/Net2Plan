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
 * @date 21/11/2016
 */
public class ProofFilter2 implements IVisualizationFilter
{
    @Override
    public boolean isVisibleNetworkElement(NetworkElement element)
    {
        if(element.getIndex() > 5) return true;
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
        return "Visible if index > 5";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        return null;
    }

    @Override
    public String getUniqueName()
    {
        return "Filter namber chu";
    }

    @Override
    public boolean isActive()
    {
        return true;
    }

    @Override
    public void setActive(boolean flag)
    {

    }
}
