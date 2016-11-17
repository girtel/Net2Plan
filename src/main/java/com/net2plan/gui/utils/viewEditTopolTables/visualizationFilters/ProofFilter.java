package com.net2plan.gui.utils.viewEditTopolTables.visualizationFilters;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.utils.Pair;

/**
 * @author César
 * @date 16/11/2016
 */
public class ProofFilter implements IVisualizationFilter
{
    String name;
    @Override
    public boolean isVisibleNetworkElement(NetworkElement element)
    {
        int index = element.getIndex();
        if(index % 2 == 0)
            return true;

        return false;
    }

    @Override
    public boolean isVisibleForwardingRules(Pair<Demand, Link> fRuleKey, Double fRuleValue)
    {
        return true;
    }

    @Override
    public String getDescription()
    {
        return "First Filter";
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }
}
