package com.net2plan.niw;

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;

import java.util.Optional;


@Deprecated
public class WFlexAlgo extends WNet
{



    /**
     * Creates a WNet object from a NetPlan object. Does not check its consistency as a valid NIW design
     *
     * @param np see above
     */
    public WFlexAlgo(NetPlan np, int k, String calculationType, String weightType)
    {
        super(np);
        getNe().setAttribute("k", "" + k);
        getNe().setAttribute("calculationType", calculationType);
        getNe().setAttribute("weightType", weightType);
    }


    public void setLinkList()
    {

    }


    public void setSidList()
    {

    }


    public void getLinkList()
    {

    }

    public void getSidList()
    {

    }


    public boolean isMetricWeightTe()
    {
        return getNe().getAttribute("weighType").equals("te");
    }
    public boolean isMetricWeightBgp()
    {
        return getNe().getAttribute("weighType").equals("bgp");
    }
    public boolean isMetricWeightDelay()
    {
        return getNe().getAttribute("weighType").equals("delay");
    }

    public boolean isCalculationTypeSpf()
    {
        return getNe().getAttribute("calculationType").equals("spf");
    }
    public boolean isCalculationTypeHeuristic()
    {
        return getNe().getAttribute("calculationType").equals("heuristic");
    }




}
