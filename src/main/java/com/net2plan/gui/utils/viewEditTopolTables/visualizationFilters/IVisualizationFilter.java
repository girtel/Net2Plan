package com.net2plan.gui.utils.viewEditTopolTables.visualizationFilters;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.internal.IExternal;
import com.net2plan.utils.Pair;

/**
 * Created by César on 15/11/2016.
 */
   /*Forwarding Rules are defined in a Map
    //Key: Pair<Demand,Link>
    //Value: Double*/
public interface IVisualizationFilter
{
    public boolean isVisibleNetworkElement(NetworkElement element);
    public boolean isVisibleForwardingRules(Pair<Demand,Link> fRuleKey, Double fRuleValue);
    public String getDescription();
    public String getUniqueName();
}
